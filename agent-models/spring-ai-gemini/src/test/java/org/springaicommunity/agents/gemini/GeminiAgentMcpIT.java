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

package org.springaicommunity.agents.gemini;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.geminisdk.util.GeminiCliDiscovery;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;
import org.springaicommunity.sandbox.LocalSandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test verifying that portable MCP definitions flow through real
 * {@link GeminiAgentModel} and Gemini CLI accepts the {@code .gemini/settings.json}
 * configuration.
 *
 * <p>
 * Uses a dummy stdio MCP server (echo command) — the server won't function but Gemini CLI
 * should accept the configuration and produce a response.
 * </p>
 *
 * <p>
 * Requires Gemini CLI installed and GEMINI_API_KEY or GOOGLE_API_KEY set. Skips
 * gracefully via {@code assumeTrue} if CLI or API key is unavailable.
 * </p>
 */
class GeminiAgentMcpIT {

	private static final Logger logger = LoggerFactory.getLogger(GeminiAgentMcpIT.class);

	@TempDir
	Path testWorkspace;

	private GeminiAgentModel geminiAgentModel;

	@BeforeEach
	void setUp() throws IOException {
		assumeTrue(GeminiCliDiscovery.isGeminiCliAvailable(), "Gemini CLI must be available for integration tests");
		assumeTrue(hasApiKey(), "GEMINI_API_KEY or GOOGLE_API_KEY must be set");

		Files.writeString(this.testWorkspace.resolve("README.md"), "# MCP Integration Test\n");
		logger.info("Test workspace: {}", this.testWorkspace);

		try {
			GeminiAgentOptions options = GeminiAgentOptions.builder()
				.model("gemini-2.5-flash")
				.yolo(true)
				.timeout(Duration.ofMinutes(3))
				.build();

			GeminiClient client = GeminiClient.create(
					CLIOptions.builder().model("gemini-2.5-flash").timeout(Duration.ofMinutes(3)).build(),
					this.testWorkspace);

			LocalSandbox sandbox = new LocalSandbox(this.testWorkspace);

			this.geminiAgentModel = new GeminiAgentModel(client, options, sandbox);
			assumeTrue(this.geminiAgentModel.isAvailable(), "Gemini agent must be available");
			logger.info("GeminiAgentModel created and available");
		}
		catch (Exception ex) {
			logger.error("Failed to setup Gemini client", ex);
			assumeTrue(false, "Failed to setup Gemini client: " + ex.getMessage());
		}
	}

	@Test
	@DisplayName("GeminiAgentModel executes with portable MCP definitions via settings.json")
	void portableMcpDefinitionsFlowToRealCli() {
		// Use 'echo' as a dummy MCP server command — it exits immediately but
		// Gemini CLI should accept the settings.json and produce a response.
		Map<String, McpServerDefinition> mcpDefinitions = Map.of("dummy-server",
				new McpServerDefinition.StdioDefinition("echo", List.of("hello"), Map.of()));

		logger.info("MCP definitions: {}", mcpDefinitions.keySet());

		AgentOptions optionsWithMcp = new PortableMcpAgentOptions(mcpDefinitions);

		AgentTaskRequest request = AgentTaskRequest
			.builder("Say hello. Keep your response to one sentence.", this.testWorkspace)
			.options(optionsWithMcp)
			.build();

		logger.info("Calling GeminiAgentModel.call() — this may take 15-60s...");
		AgentResponse response = this.geminiAgentModel.call(request);
		logger.info("Got response from Gemini CLI");

		assertThat(response).isNotNull();
		assertThat(response.getResults()).isNotEmpty();
		String output = response.getResults().get(0).getOutput();
		logger.info("Response text: {}", output);
		assertThat(output).isNotBlank();

		// Verify settings file was cleaned up
		assertThat(this.testWorkspace.resolve(".gemini/settings.json")).doesNotExist();
	}

	private static boolean hasApiKey() {
		return System.getenv("GEMINI_API_KEY") != null || System.getenv("GOOGLE_API_KEY") != null;
	}

	/**
	 * Minimal AgentOptions that carries portable MCP definitions for testing.
	 */
	private static final class PortableMcpAgentOptions implements AgentOptions {

		private final Map<String, McpServerDefinition> mcpDefinitions;

		PortableMcpAgentOptions(Map<String, McpServerDefinition> mcpDefinitions) {
			this.mcpDefinitions = mcpDefinitions;
		}

		@Override
		public String getWorkingDirectory() {
			return null;
		}

		@Override
		public Duration getTimeout() {
			return null;
		}

		@Override
		public Map<String, String> getEnvironmentVariables() {
			return Map.of();
		}

		@Override
		public String getModel() {
			return null;
		}

		@Override
		public Map<String, Object> getExtras() {
			return Map.of();
		}

		@Override
		public Map<String, McpServerDefinition> getMcpServerDefinitions() {
			return this.mcpDefinitions;
		}

	}

}
