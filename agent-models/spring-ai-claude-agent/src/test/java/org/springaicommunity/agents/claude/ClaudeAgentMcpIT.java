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

package org.springaicommunity.agents.claude;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;
import org.springaicommunity.agents.tck.PortableMcpAgentOptions;
import org.springaicommunity.claude.agent.sdk.config.ClaudeCliDiscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test verifying that portable MCP definitions flow through real
 * {@link ClaudeAgentModel} and Claude CLI accepts the {@code --mcp-config} configuration.
 *
 * <p>
 * Uses a dummy stdio MCP server (echo command) — the server won't function but Claude CLI
 * should accept the configuration and produce a response.
 * </p>
 *
 * <p>
 * Requires Claude CLI installed and authenticated (session auth or API key). Skips
 * gracefully via {@code assumeTrue} if CLI is unavailable.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> Cannot run from within a Claude Code session (nesting guard
 * added in CLI 2.1.39). Run from terminal or CI. The SDK's env whitelist in
 * {@code StreamingTransport} prevents {@code CLAUDECODE} leaking to the subprocess.
 * </p>
 */
class ClaudeAgentMcpIT {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeAgentMcpIT.class);

	@TempDir
	Path testWorkspace;

	private ClaudeAgentModel claudeAgentModel;

	@BeforeEach
	void setUp() throws IOException {
		logger.info("Checking Claude CLI availability...");
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available for integration tests");
		logger.info("Claude CLI is available");

		Files.writeString(this.testWorkspace.resolve("README.md"), "# MCP Integration Test\n");
		logger.info("Test workspace: {}", this.testWorkspace);

		try {
			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model("claude-haiku-4-5-20251001")
				.yolo(true)
				.build();

			this.claudeAgentModel = ClaudeAgentModel.builder()
				.workingDirectory(this.testWorkspace)
				.defaultOptions(options)
				.build();

			assumeTrue(this.claudeAgentModel.isAvailable(), "Claude agent must be available");
			logger.info("ClaudeAgentModel created and available");
		}
		catch (Exception ex) {
			logger.error("Failed to setup Claude client", ex);
			assumeTrue(false, "Failed to setup Claude client: " + ex.getMessage());
		}
	}

	@AfterEach
	void tearDown() {
		if (this.claudeAgentModel != null) {
			this.claudeAgentModel.close();
			logger.info("ClaudeAgentModel closed");
		}
	}

	@Test
	@DisplayName("ClaudeAgentModel executes with portable MCP definitions via --mcp-config")
	void portableMcpDefinitionsFlowToRealCli() {
		// Use 'echo' as a dummy MCP server command — it exits immediately but
		// Claude CLI still accepts the --mcp-config and produces a response.
		Map<String, McpServerDefinition> mcpDefinitions = Map.of("dummy-server",
				new McpServerDefinition.StdioDefinition("echo", List.of("hello"), Map.of()));

		logger.info("MCP definitions: {}", mcpDefinitions.keySet());

		PortableMcpAgentOptions optionsWithMcp = new PortableMcpAgentOptions(mcpDefinitions);

		AgentTaskRequest request = AgentTaskRequest
			.builder("Say hello. Keep your response to one sentence.", this.testWorkspace)
			.options(optionsWithMcp)
			.build();

		logger.info("Calling ClaudeAgentModel.call() — this may take 15-60s...");
		AgentResponse response = this.claudeAgentModel.call(request);
		logger.info("Got response from Claude CLI");

		assertThat(response).isNotNull();
		assertThat(response.getResults()).isNotEmpty();
		String output = response.getResults().get(0).getOutput();
		logger.info("Response text: {}", output);
		assertThat(output).isNotBlank();
	}

	private boolean isClaudeCliAvailable() {
		try {
			return ClaudeCliDiscovery.isClaudeCliAvailable();
		}
		catch (Exception ex) {
			logger.error("Claude CLI availability check failed", ex);
			return false;
		}
	}

}
