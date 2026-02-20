/*
 * Copyright 2024 Spring AI Community
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

package org.springaicommunity.agents.geminisdk.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MCP settings file writing and cleanup in CLITransport.
 */
class CLITransportMcpTest {

	@TempDir
	Path tempDir;

	@Test
	void writeMcpSettingsCreatesDirectoryAndFile() throws IOException {
		Map<String, Object> mcpServers = Map.of("weather",
				Map.of("command", "npx", "args", List.of("-y", "@mcp/weather-server")));

		Path settingsFile = CLITransport.writeMcpSettings(this.tempDir, mcpServers);

		assertThat(settingsFile).exists();
		assertThat(settingsFile.getParent().getFileName().toString()).isEqualTo(".gemini");

		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		Map<String, Object> written = mapper.readValue(settingsFile.toFile(), Map.class);
		assertThat(written).containsKey("mcpServers");

		@SuppressWarnings("unchecked")
		Map<String, Object> servers = (Map<String, Object>) written.get("mcpServers");
		assertThat(servers).containsKey("weather");
	}

	@Test
	void writeMcpSettingsWithMultipleServers() throws IOException {
		Map<String, Object> mcpServers = Map.of("server-a", Map.of("command", "echo", "args", List.of("hello")),
				"server-b", Map.of("url", "http://localhost:8080/sse"));

		Path settingsFile = CLITransport.writeMcpSettings(this.tempDir, mcpServers);

		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		Map<String, Object> written = mapper.readValue(settingsFile.toFile(), Map.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> servers = (Map<String, Object>) written.get("mcpServers");
		assertThat(servers).hasSize(2);
		assertThat(servers).containsKeys("server-a", "server-b");
	}

	@Test
	void cleanupMcpSettingsDeletesFileAndEmptyDirectory() throws IOException {
		Map<String, Object> mcpServers = Map.of("test", Map.of("command", "echo"));

		Path settingsFile = CLITransport.writeMcpSettings(this.tempDir, mcpServers);
		assertThat(settingsFile).exists();

		CLITransport.cleanupMcpSettings(settingsFile);

		assertThat(settingsFile).doesNotExist();
		assertThat(settingsFile.getParent()).doesNotExist();
	}

	@Test
	void cleanupMcpSettingsPreservesNonEmptyDirectory() throws IOException {
		Map<String, Object> mcpServers = Map.of("test", Map.of("command", "echo"));

		Path settingsFile = CLITransport.writeMcpSettings(this.tempDir, mcpServers);
		// Create another file in .gemini/ so it's not empty after settings.json removal
		Files.writeString(settingsFile.getParent().resolve("other.json"), "{}");

		CLITransport.cleanupMcpSettings(settingsFile);

		assertThat(settingsFile).doesNotExist();
		assertThat(settingsFile.getParent()).exists();
	}

	@Test
	void cleanupMcpSettingsHandlesNullGracefully() {
		CLITransport.cleanupMcpSettings(null);
		// Should not throw
	}

	@Test
	void cleanupMcpSettingsHandlesNonexistentFileGracefully() {
		CLITransport.cleanupMcpSettings(this.tempDir.resolve(".gemini/settings.json"));
		// Should not throw
	}

	@Test
	void buildCommandIncludesAllowedMcpServerNames() {
		CLIOptions options = CLIOptions.builder()
			.mcpServers(Map.of("weather", Map.of("command", "echo"), "brave-search", Map.of("command", "npx")))
			.build();

		CLITransport transport = new CLITransport(this.tempDir, options.getTimeout());
		List<String> command = transport.buildCommand("test prompt", options);

		int flagIndex = command.indexOf("--allowed-mcp-server-names");
		assertThat(flagIndex).isGreaterThan(-1);

		String serverNames = command.get(flagIndex + 1);
		assertThat(serverNames).contains("weather");
		assertThat(serverNames).contains("brave-search");
	}

	@Test
	void buildCommandOmitsAllowedMcpServerNamesWhenNoServers() {
		CLIOptions options = CLIOptions.builder().build();

		CLITransport transport = new CLITransport(this.tempDir, options.getTimeout());
		List<String> command = transport.buildCommand("test prompt", options);

		assertThat(command).doesNotContain("--allowed-mcp-server-names");
	}

}
