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

package org.springaicommunity.agents.model.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link McpServerCatalog}, {@link DefaultMcpServerCatalog}, and JSON
 * loading.
 */
class McpServerCatalogTest {

	@Test
	void builderAddAndBuild() {
		McpServerCatalog catalog = McpServerCatalog.builder()
			.add("brave", new McpServerDefinition.StdioDefinition("npx", List.of("-y", "brave-search")))
			.add("weather", new McpServerDefinition.SseDefinition("http://localhost:8080/sse"))
			.build();

		assertThat(catalog.getAll()).hasSize(2);
		assertThat(catalog.contains("brave")).isTrue();
		assertThat(catalog.contains("weather")).isTrue();
		assertThat(catalog.contains("nonexistent")).isFalse();
	}

	@Test
	void ofFactoryMethod() {
		var servers = Map.<String, McpServerDefinition>of("s1", new McpServerDefinition.StdioDefinition("cmd"));
		McpServerCatalog catalog = McpServerCatalog.of(servers);

		assertThat(catalog.getAll()).hasSize(1);
		assertThat(catalog.contains("s1")).isTrue();
	}

	@Test
	void resolveByName() {
		McpServerCatalog catalog = McpServerCatalog.builder()
			.add("a", new McpServerDefinition.StdioDefinition("cmd-a"))
			.add("b", new McpServerDefinition.SseDefinition("http://b"))
			.add("c", new McpServerDefinition.HttpDefinition("http://c"))
			.build();

		Map<String, McpServerDefinition> resolved = catalog.resolve(List.of("a", "c"));
		assertThat(resolved).hasSize(2).containsOnlyKeys("a", "c");
		assertThat(resolved.get("a")).isInstanceOf(McpServerDefinition.StdioDefinition.class);
		assertThat(resolved.get("c")).isInstanceOf(McpServerDefinition.HttpDefinition.class);
	}

	@Test
	void resolveEmptyCollectionReturnsEmptyMap() {
		McpServerCatalog catalog = McpServerCatalog.builder()
			.add("a", new McpServerDefinition.StdioDefinition("cmd"))
			.build();

		assertThat(catalog.resolve(List.of())).isEmpty();
	}

	@Test
	void resolveNullCollectionReturnsEmptyMap() {
		McpServerCatalog catalog = McpServerCatalog.builder()
			.add("a", new McpServerDefinition.StdioDefinition("cmd"))
			.build();

		assertThat(catalog.resolve(null)).isEmpty();
	}

	@Test
	void resolveMissingNameThrows() {
		McpServerCatalog catalog = McpServerCatalog.builder()
			.add("a", new McpServerDefinition.StdioDefinition("cmd"))
			.build();

		assertThatThrownBy(() -> catalog.resolve(List.of("a", "missing"))).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("missing")
			.hasMessageContaining("not found in catalog");
	}

	@Test
	void getAllReturnsUnmodifiableMap() {
		McpServerCatalog catalog = McpServerCatalog.builder()
			.add("a", new McpServerDefinition.StdioDefinition("cmd"))
			.build();

		assertThatThrownBy(() -> catalog.getAll().put("b", new McpServerDefinition.StdioDefinition("other")))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void builderRejectsNullName() {
		assertThatThrownBy(() -> McpServerCatalog.builder().add(null, new McpServerDefinition.StdioDefinition("cmd")))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void builderRejectsNullDefinition() {
		assertThatThrownBy(() -> McpServerCatalog.builder().add("name", null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void emptyCatalog() {
		McpServerCatalog catalog = McpServerCatalog.builder().build();
		assertThat(catalog.getAll()).isEmpty();
		assertThat(catalog.contains("anything")).isFalse();
	}

	// ── JSON Loading ──────────────────────────────────────────────────

	@Test
	void fromJsonSingleFile(@TempDir Path tempDir) throws IOException {
		Path jsonFile = tempDir.resolve("servers.json");
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
				      "url": "http://localhost:3000/mcp",
				      "headers": { "X-Api-Key": "secret" }
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);

		assertThat(catalog.getAll()).hasSize(3);

		var brave = (McpServerDefinition.StdioDefinition) catalog.resolve(Set.of("brave-search")).get("brave-search");
		assertThat(brave.command()).isEqualTo("npx");
		assertThat(brave.args()).containsExactly("-y", "@modelcontextprotocol/server-brave-search");

		var weather = (McpServerDefinition.SseDefinition) catalog.resolve(Set.of("weather")).get("weather");
		assertThat(weather.url()).isEqualTo("http://localhost:8080/sse");

		var api = (McpServerDefinition.HttpDefinition) catalog.resolve(Set.of("api")).get("api");
		assertThat(api.url()).isEqualTo("http://localhost:3000/mcp");
		assertThat(api.headers()).containsEntry("X-Api-Key", "secret");
	}

	@Test
	void fromJsonDirectory(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("search.json"), """
				{
				  "mcpServers": {
				    "brave": { "type": "stdio", "command": "npx", "args": ["-y", "brave"] }
				  }
				}
				""");
		Files.writeString(tempDir.resolve("weather.json"), """
				{
				  "mcpServers": {
				    "weather": { "type": "sse", "url": "http://localhost:8080/sse" }
				  }
				}
				""");
		// Non-JSON file should be ignored
		Files.writeString(tempDir.resolve("readme.txt"), "not a json file");

		McpServerCatalog catalog = McpServerCatalog.fromJson(tempDir);

		assertThat(catalog.getAll()).hasSize(2);
		assertThat(catalog.contains("brave")).isTrue();
		assertThat(catalog.contains("weather")).isTrue();
	}

	@Test
	void fromJsonDefaultsToStdioWhenTypeOmitted(@TempDir Path tempDir) throws IOException {
		Path jsonFile = tempDir.resolve("servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "myserver": { "command": "node", "args": ["server.js"] }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);
		assertThat(catalog.resolve(Set.of("myserver")).get("myserver"))
			.isInstanceOf(McpServerDefinition.StdioDefinition.class);
	}

	@Test
	void fromJsonEnvironmentVariableSubstitution(@TempDir Path tempDir) throws IOException {
		// Use a well-known env var that should always exist
		String homeValue = System.getenv("HOME");
		org.junit.jupiter.api.Assumptions.assumeTrue(homeValue != null, "HOME env var required");

		Path jsonFile = tempDir.resolve("servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "test": {
				      "type": "stdio",
				      "command": "cmd",
				      "env": { "MY_HOME": "${HOME}" }
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);
		var def = (McpServerDefinition.StdioDefinition) catalog.resolve(Set.of("test")).get("test");
		assertThat(def.env().get("MY_HOME")).isEqualTo(homeValue);
	}

	@Test
	void fromJsonUnresolvableEnvVarReplacedWithEmpty(@TempDir Path tempDir) throws IOException {
		Path jsonFile = tempDir.resolve("servers.json");
		Files.writeString(jsonFile, """
				{
				  "mcpServers": {
				    "test": {
				      "type": "stdio",
				      "command": "cmd",
				      "env": { "VAL": "${DEFINITELY_NOT_A_REAL_ENV_VAR_XYZ}" }
				    }
				  }
				}
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);
		var def = (McpServerDefinition.StdioDefinition) catalog.resolve(Set.of("test")).get("test");
		assertThat(def.env().get("VAL")).isEmpty();
	}

	@Test
	void fromJsonNonexistentPathThrows() {
		assertThatThrownBy(() -> McpServerCatalog.fromJson(Path.of("/nonexistent/path.json")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("does not exist");
	}

	@Test
	void fromJsonNullPathThrows() {
		assertThatThrownBy(() -> McpServerCatalog.fromJson(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fromJsonEmptyMcpServersNode(@TempDir Path tempDir) throws IOException {
		Path jsonFile = tempDir.resolve("empty.json");
		Files.writeString(jsonFile, """
				{ "mcpServers": {} }
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);
		assertThat(catalog.getAll()).isEmpty();
	}

	@Test
	void fromJsonMissingMcpServersNode(@TempDir Path tempDir) throws IOException {
		Path jsonFile = tempDir.resolve("other.json");
		Files.writeString(jsonFile, """
				{ "someOtherKey": "value" }
				""");

		McpServerCatalog catalog = McpServerCatalog.fromJson(jsonFile);
		assertThat(catalog.getAll()).isEmpty();
	}

}
