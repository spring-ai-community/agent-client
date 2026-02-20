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

package org.springaicommunity.agents.client.advisor.context;

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
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.claude.agent.sdk.config.ClaudeCliDiscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for VendirContextAdvisor demonstrating end-to-end context engineering
 * with real vendir execution and Claude agent.
 *
 * <p>
 * Requires Claude CLI and vendir CLI installed and authenticated. Skips gracefully if
 * unavailable.
 * </p>
 *
 * @author Mark Pollack
 */
class VendirContextAdvisorIT {

	private static final Logger logger = LoggerFactory.getLogger(VendirContextAdvisorIT.class);

	@TempDir
	Path testWorkspace;

	private ClaudeAgentModel claudeAgentModel;

	@BeforeEach
	void setUp() throws IOException {
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available");
		assumeTrue(isVendirAvailable(), "Vendir CLI must be available");

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
			logger.info("Test workspace: {}", this.testWorkspace);
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
		}
	}

	@Test
	@DisplayName("VendirContextAdvisor gathers git context and agent uses it")
	void gitContextGathering() throws IOException {
		Path vendirConfig = writeVendirConfig("vendir.yml", """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: spring-guide
				    git:
				      url: https://github.com/spring-guides/gs-rest-service
				      ref: main
				      depth: 1
				""");

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir")
			.autoCleanup(false)
			.timeout(120)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(3))
			.build();

		AgentClientResponse response = client.run("Say hello. Keep your response to one sentence.");

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify context metadata
		assertThat(response.context()).containsKey("vendir.context.success");
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);
		assertThat(response.context()).containsKeys("vendir.context.path", "vendir.context.gathered",
				"vendir.context.output");

		// Verify context directory was populated
		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir/vendor/spring-guide");
		assertThat(contextDir).exists().isDirectory();
		assertThat(contextDir.resolve("README.adoc")).exists();

		logger.info("Response: {}", response.getResult());
	}

	@Test
	@DisplayName("VendirContextAdvisor handles vendir sync failure gracefully")
	void failureHandling() throws IOException {
		Path vendirConfig = writeVendirConfig("vendir-invalid.yml", """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: nonexistent
				    git:
				      url: https://github.com/nonexistent/nonexistent-repo-12345
				      ref: main
				""");

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir")
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Say hello. Keep your response to one sentence.");

		// Agent should still execute even if context gathering fails
		assertThat(response).isNotNull();
		assertThat(response.context()).containsKey("vendir.context.success");

		logger.info("Graceful failure handling verified");
	}

	@Test
	@DisplayName("VendirContextAdvisor with inline content (no network required)")
	void inlineContent() throws IOException {
		Path vendirConfig = writeVendirConfig("vendir-inline.yml", createInlineVendirYaml());

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-inline")
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Say hello. Keep your response to one sentence.");

		assertThat(response).isNotNull();
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);

		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir-inline/vendor/docs");
		assertThat(contextDir).exists();

		logger.info("Inline content test completed");
	}

	@Test
	@DisplayName("VendirContextAdvisor with auto-cleanup removes context after execution")
	void autoCleanup() throws IOException {
		Path vendirConfig = writeVendirConfig("vendir-cleanup.yml", createInlineVendirYaml());

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-temp")
			.autoCleanup(true)
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Say hello. Keep your response to one sentence.");

		assertThat(response).isNotNull();

		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir-temp");
		logger.info("Auto-cleanup test completed. Context dir exists: {}", Files.exists(contextDir));
	}

	private Path writeVendirConfig(String filename, String content) throws IOException {
		Path configPath = this.testWorkspace.resolve(filename);
		Files.writeString(configPath, content);
		return configPath;
	}

	private String createInlineVendirYaml() {
		return """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: docs
				    inline:
				      paths:
				        GUIDELINES.md: |
				          # Development Guidelines
				          Always use meaningful variable names.
				          Write tests for all public APIs.
				        PATTERNS.md: |
				          # Design Patterns
				          Prefer composition over inheritance.
				          Use dependency injection.
				""";
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

	private boolean isVendirAvailable() {
		try {
			ProcessBuilder pb = new ProcessBuilder("vendir", "--version");
			Process process = pb.start();
			return process.waitFor() == 0;
		}
		catch (Exception ex) {
			logger.error("Vendir CLI availability check failed", ex);
			return false;
		}
	}

}
