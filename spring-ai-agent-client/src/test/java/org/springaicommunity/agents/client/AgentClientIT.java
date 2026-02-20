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

package org.springaicommunity.agents.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.claude.agent.sdk.config.ClaudeCliDiscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for the full AgentClient stack: AgentClient -> ClaudeAgentModel ->
 * Claude Agent SDK -> Claude CLI.
 *
 * <p>
 * Requires Claude CLI installed and authenticated. Skips gracefully if unavailable.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> Cannot run from within a Claude Code session (nesting guard
 * added in CLI 2.1.39). Run from terminal or CI.
 * </p>
 *
 * @author Mark Pollack
 * @see AgentClientChatClientStyleTest for fast mocked API surface testing
 */
class AgentClientIT {

	private static final Logger logger = LoggerFactory.getLogger(AgentClientIT.class);

	@TempDir
	Path testWorkspace;

	private ClaudeAgentModel claudeAgentModel;

	private AgentClient agentClient;

	@BeforeEach
	void setUp() throws IOException {
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available for integration tests");

		Files.writeString(this.testWorkspace.resolve("README.md"), "# Test Project\n");
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

			this.agentClient = AgentClient.builder(this.claudeAgentModel)
				.defaultWorkingDirectory(this.testWorkspace)
				.defaultTimeout(Duration.ofMinutes(2))
				.build();

			logger.info("AgentClient created and available");
		}
		catch (Exception ex) {
			logger.error("Failed to setup AgentClient", ex);
			assumeTrue(false, "Failed to setup AgentClient: " + ex.getMessage());
		}
	}

	@AfterEach
	void tearDown() {
		if (this.claudeAgentModel != null) {
			this.claudeAgentModel.close();
		}
	}

	@Test
	@DisplayName("AgentClient.run() executes a simple task via Claude CLI")
	void simpleRunExecution() {
		AgentClientResponse response = this.agentClient.run("Say hello. Keep your response to one sentence.");

		logger.info("Response: {}", response.getResult());

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isNotBlank();
	}

	@Test
	@DisplayName("AgentClient fluent goal().run() executes via Claude CLI")
	void fluentGoalExecution() {
		AgentClientResponse response = this.agentClient.goal("Say hello. Keep your response to one sentence.").run();

		logger.info("Response: {}", response.getResult());

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isNotBlank();
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
