/*
 * Copyright 2025 Spring AI Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agents.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springaicommunity.agents.model.mcp.McpServerCatalog;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-pipeline integration test for MCP catalog flow.
 *
 * <p>
 * Tests the complete chain: JSON file on disk &rarr; {@link McpServerCatalog#fromJson}
 * &rarr; {@link AgentClient} builder &rarr; advisor chain &rarr; {@link MockAgentModel}
 * captures {@code AgentTaskRequest} with resolved definitions.
 * </p>
 *
 * <p>
 * Why IT not unit: exercises real file I/O, full advisor chain wiring, and the complete
 * catalog &rarr; client &rarr; model pipeline together. Unit tests in
 * {@link AgentClientMcpTest} tested each layer in isolation.
 * </p>
 *
 * <p>
 * No skip conditions &mdash; uses {@link MockAgentModel}, no external CLI required.
 * </p>
 */
class AgentClientMcpPipelineIT {

	private MockAgentModel mockModel;

	@TempDir
	Path testWorkspace;

	@TempDir
	Path catalogDir;

	@BeforeEach
	void setUp() {
		this.mockModel = new MockAgentModel();
	}

	@Test
	@DisplayName("JSON file -> catalog -> client resolves -> model receives StdioDefinition and SseDefinition")
	void jsonFileToCatalogToClientToModel() throws IOException {
		Path jsonFile = this.catalogDir.resolve("mcp-servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "brave-search": {
				      "type": "stdio",
				      "command": "npx",
				      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
				      "env": { "BRAVE_API_KEY": "test-key-123" }
				    },
				    "weather": {
				      "type": "sse",
				      "url": "http://localhost:8080/sse"
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);

		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(catalog).build();

		client.goal("Search and check weather")
			.workingDirectory(this.testWorkspace)
			.mcpServers("brave-search", "weather")
			.run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("brave-search", "weather");

		McpServerDefinition.StdioDefinition stdio = (McpServerDefinition.StdioDefinition) resolved.get("brave-search");
		assertThat(stdio.command()).isEqualTo("npx");
		assertThat(stdio.args()).containsExactly("-y", "@modelcontextprotocol/server-brave-search");
		assertThat(stdio.env()).containsEntry("BRAVE_API_KEY", "test-key-123");

		McpServerDefinition.SseDefinition sse = (McpServerDefinition.SseDefinition) resolved.get("weather");
		assertThat(sse.url()).isEqualTo("http://localhost:8080/sse");
	}

	@Test
	@DisplayName("Directory scan with multiple JSON files -> merged catalog -> client resolves correctly")
	void directoryScanMergesCatalog() throws IOException {
		Files.writeString(this.catalogDir.resolve("search-servers.json"), """
				{
				  "mcpServers": {
				    "brave-search": {
				      "type": "stdio",
				      "command": "npx",
				      "args": ["-y", "@modelcontextprotocol/server-brave-search"]
				    }
				  }
				}
				""");

		Files.writeString(this.catalogDir.resolve("api-servers.json"), """
				{
				  "mcpServers": {
				    "weather": {
				      "type": "sse",
				      "url": "http://localhost:8080/sse"
				    },
				    "api": {
				      "type": "http",
				      "url": "http://localhost:3000/mcp",
				      "headers": { "X-Api-Key": "secret" }
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(this.catalogDir);

		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(catalog).build();

		client.goal("Use all servers")
			.workingDirectory(this.testWorkspace)
			.mcpServers("brave-search", "weather", "api")
			.run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(3).containsOnlyKeys("brave-search", "weather", "api");
		assertThat(resolved.get("brave-search")).isInstanceOf(McpServerDefinition.StdioDefinition.class);
		assertThat(resolved.get("weather")).isInstanceOf(McpServerDefinition.SseDefinition.class);
		assertThat(resolved.get("api")).isInstanceOf(McpServerDefinition.HttpDefinition.class);
	}

	@Test
	@DisplayName("Builder defaults + per-request servers unioned through full advisor chain")
	void builderDefaultsAndRequestServersUnioned() throws IOException {
		Path jsonFile = this.catalogDir.resolve("servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "brave-search": {
				      "type": "stdio",
				      "command": "npx",
				      "args": ["-y", "@modelcontextprotocol/server-brave-search"]
				    },
				    "weather": {
				      "type": "sse",
				      "url": "http://localhost:8080/sse"
				    },
				    "api": {
				      "type": "http",
				      "url": "http://localhost:3000/mcp"
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);

		AgentClient client = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(catalog)
			.defaultMcpServers("weather")
			.build();

		// Request adds brave-search; default weather should also be present
		client.goal("Search with weather").workingDirectory(this.testWorkspace).mcpServers("brave-search").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("weather", "brave-search");
	}

	@Test
	@DisplayName("${ENV_VAR} substitution works end-to-end with real System.getenv()")
	void envVarSubstitutionEndToEnd() throws IOException {
		// Use a well-known environment variable that is always set
		Path jsonFile = this.catalogDir.resolve("env-servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "env-test": {
				      "type": "stdio",
				      "command": "test-cmd",
				      "env": { "HOME_DIR": "${HOME}", "MISSING_VAR": "${DEFINITELY_NOT_SET_XYZ_123}" }
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);

		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(catalog).build();

		client.goal("Test env vars").workingDirectory(this.testWorkspace).mcpServers("env-test").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		McpServerDefinition.StdioDefinition stdio = (McpServerDefinition.StdioDefinition) resolved.get("env-test");

		// HOME should be resolved to an actual value
		String homeValue = System.getenv("HOME");
		assertThat(stdio.env().get("HOME_DIR")).isEqualTo(homeValue != null ? homeValue : "");

		// Missing env var should resolve to empty string
		assertThat(stdio.env().get("MISSING_VAR")).isEmpty();
	}

	@Test
	@DisplayName("Mutated client preserves catalog across full pipeline")
	void mutatedClientPreservesCatalog() throws IOException {
		Path jsonFile = this.catalogDir.resolve("servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "brave-search": {
				      "type": "stdio",
				      "command": "npx",
				      "args": ["-y", "@modelcontextprotocol/server-brave-search"]
				    },
				    "weather": {
				      "type": "sse",
				      "url": "http://localhost:8080/sse"
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);

		AgentClient original = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(catalog)
			.defaultMcpServers("weather")
			.build();

		AgentClient mutated = original.mutate().build();

		mutated.goal("From mutated client").workingDirectory(this.testWorkspace).mcpServers("brave-search").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("weather", "brave-search");
		assertThat(resolved.get("brave-search")).isInstanceOf(McpServerDefinition.StdioDefinition.class);
		assertThat(resolved.get("weather")).isInstanceOf(McpServerDefinition.SseDefinition.class);
	}

}
