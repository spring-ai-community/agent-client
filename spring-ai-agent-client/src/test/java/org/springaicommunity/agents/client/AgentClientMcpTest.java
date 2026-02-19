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

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springaicommunity.agents.model.mcp.McpServerCatalog;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MCP server catalog integration in AgentClient. Verifies the "client
 * resolves, model translates" pattern: the client layer resolves MCP server names to
 * portable definitions before the request reaches the model.
 */
class AgentClientMcpTest {

	private MockAgentModel mockModel;

	private McpServerCatalog catalog;

	@TempDir
	Path testWorkspace;

	@BeforeEach
	void setUp() {
		this.mockModel = new MockAgentModel();
		this.catalog = McpServerCatalog.builder()
			.add("brave-search", new McpServerDefinition.StdioDefinition("npx",
					List.of("-y", "@modelcontextprotocol/server-brave-search"), Map.of("BRAVE_API_KEY", "test-key")))
			.add("weather", new McpServerDefinition.SseDefinition("http://localhost:8080/sse"))
			.add("api",
					new McpServerDefinition.HttpDefinition("http://localhost:3000/mcp", Map.of("X-Api-Key", "secret")))
			.build();
	}

	@Test
	void requestSpecMcpServersResolvesViaClientCatalog() {
		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(this.catalog).build();

		client.goal("Search the web").workingDirectory(this.testWorkspace).mcpServers("brave-search").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(1).containsKey("brave-search");
		assertThat(resolved.get("brave-search")).isInstanceOf(McpServerDefinition.StdioDefinition.class);
	}

	@Test
	void defaultMcpServersAppliedToEveryRequest() {
		AgentClient client = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(this.catalog)
			.defaultMcpServers("weather")
			.build();

		client.goal("What's the forecast?").workingDirectory(this.testWorkspace).run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(1).containsKey("weather");
		assertThat(resolved.get("weather")).isInstanceOf(McpServerDefinition.SseDefinition.class);
	}

	@Test
	void requestSpecAndDefaultMcpServersAreUnioned() {
		AgentClient client = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(this.catalog)
			.defaultMcpServers("weather")
			.build();

		client.goal("Search and check weather").workingDirectory(this.testWorkspace).mcpServers("brave-search").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("weather", "brave-search");
	}

	@Test
	void duplicateMcpServerNamesAreDeduplicated() {
		AgentClient client = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(this.catalog)
			.defaultMcpServers("weather")
			.build();

		client.goal("Duplicate test").workingDirectory(this.testWorkspace).mcpServers("weather", "brave-search").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("weather", "brave-search");
	}

	@Test
	void noMcpServersResultsInEmptyDefinitions() {
		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(this.catalog).build();

		client.goal("No MCP needed").workingDirectory(this.testWorkspace).run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).isEmpty();
	}

	@Test
	void noCatalogAndNoNamesIsNoOp() {
		// No catalog, no MCP names — should work fine
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.goal("Simple task").workingDirectory(this.testWorkspace).run();

		assertThat(response).isNotNull();
		assertThat(this.mockModel.lastRequest.options().getMcpServerDefinitions()).isEmpty();
	}

	@Test
	void mcpServersWithoutCatalogThrowsIllegalState() {
		// Names present but no catalog → fail-fast
		AgentClient client = AgentClient.builder(this.mockModel).build();

		assertThatThrownBy(
				() -> client.goal("Will fail").workingDirectory(this.testWorkspace).mcpServers("brave-search").run())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("McpServerCatalog")
			.hasMessageContaining("brave-search");
	}

	@Test
	void defaultMcpServersWithoutCatalogThrowsIllegalState() {
		AgentClient client = AgentClient.builder(this.mockModel).defaultMcpServers("weather").build();

		assertThatThrownBy(() -> client.goal("Will fail").workingDirectory(this.testWorkspace).run())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("McpServerCatalog");
	}

	@Test
	void missingServerNameInCatalogThrowsAtRunTime() {
		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(this.catalog).build();

		assertThatThrownBy(
				() -> client.goal("Will fail").workingDirectory(this.testWorkspace).mcpServers("nonexistent").run())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("nonexistent")
			.hasMessageContaining("not found in catalog");
	}

	@Test
	void mcpServersVarargAccumulates() {
		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(this.catalog).build();

		client.goal("Multi add")
			.workingDirectory(this.testWorkspace)
			.mcpServers("brave-search")
			.mcpServers("weather")
			.run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("brave-search", "weather");
	}

	@Test
	void mcpServersListForm() {
		AgentClient client = AgentClient.builder(this.mockModel).mcpServerCatalog(this.catalog).build();

		client.goal("List form").workingDirectory(this.testWorkspace).mcpServers(List.of("brave-search", "api")).run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("brave-search", "api");
		assertThat(resolved.get("api")).isInstanceOf(McpServerDefinition.HttpDefinition.class);
	}

	@Test
	void defaultMcpServersListForm() {
		AgentClient client = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(this.catalog)
			.defaultMcpServers(List.of("weather", "api"))
			.build();

		client.goal("List default form").workingDirectory(this.testWorkspace).run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("weather", "api");
	}

	@Test
	void builderFromRoundTripsAllFieldsIncludingMcpDefinitions() {
		McpServerDefinition braveDef = new McpServerDefinition.StdioDefinition("npx", List.of("-y", "brave"));
		DefaultAgentOptions original = DefaultAgentOptions.builder()
			.workingDirectory("/tmp/test")
			.timeout(Duration.ofMinutes(3))
			.model("test-model")
			.environmentVariables(Map.of("KEY", "val"))
			.extras(Map.of("extra", "data"))
			.mcpServerDefinitions(Map.of("brave-search", braveDef))
			.build();

		DefaultAgentOptions copy = DefaultAgentOptions.builder().from(original).build();

		assertThat(copy.getWorkingDirectory()).isEqualTo(original.getWorkingDirectory());
		assertThat(copy.getTimeout()).isEqualTo(original.getTimeout());
		assertThat(copy.getModel()).isEqualTo(original.getModel());
		assertThat(copy.getEnvironmentVariables()).isEqualTo(original.getEnvironmentVariables());
		assertThat(copy.getExtras()).isEqualTo(original.getExtras());
		assertThat(copy.getMcpServerDefinitions()).isEqualTo(original.getMcpServerDefinitions());
		assertThat(copy.getMcpServerDefinitions()).containsKey("brave-search");
	}

	@Test
	void mutatePreservesMcpCatalogAndDefaults() {
		AgentClient original = AgentClient.builder(this.mockModel)
			.mcpServerCatalog(this.catalog)
			.defaultMcpServers("weather")
			.build();

		AgentClient mutated = original.mutate().build();

		// Mutated client should still resolve MCP servers through the catalog
		mutated.goal("From mutated client").workingDirectory(this.testWorkspace).mcpServers("brave-search").run();

		Map<String, McpServerDefinition> resolved = this.mockModel.lastRequest.options().getMcpServerDefinitions();
		assertThat(resolved).hasSize(2).containsOnlyKeys("weather", "brave-search");
	}

}
