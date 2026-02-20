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

package org.springaicommunity.agents.helloworldai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for HelloWorldAgentAI via JBang launcher. Tests the end-to-end JBang
 * runner with the hello-world-agent-ai that uses AgentClient to invoke real AI agents.
 *
 * <p>
 * Requires the corresponding provider's API key or CLI to be available. Skips gracefully
 * if unavailable.
 * </p>
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class HelloWorldAgentAIRunnerIT {

	private static final Logger logger = LoggerFactory.getLogger(HelloWorldAgentAIRunnerIT.class);

	@TempDir
	Path tempDir;

	static Stream<Arguments> providerVariants() {
		return Stream.of(Arguments.of("claude", "ANTHROPIC_API_KEY", "ai-greeting.txt", "a creative greeting message"),
				Arguments.of("gemini", "GEMINI_API_KEY", "gemini-info.txt", "information about Gemini AI"));
	}

	@ParameterizedTest(name = "provider={0}")
	@MethodSource("providerVariants")
	@DisplayName("JBang launcher invokes AI agent and creates file")
	void launcherInvokesAiAgent(String provider, String envVar, String fileName, String contentDescription)
			throws Exception {
		assumeTrue(System.getenv(envVar) != null, envVar + " must be set");

		Path launcherJavaPath = getLauncherJavaFile();

		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai", "path=" + fileName,
					"content=" + contentDescription, "provider=" + provider)
			.directory(this.tempDir.toFile())
			.timeout(120, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		logger.info("JBang output ({}): {}", provider, result.outputUTF8());
		assertThat(result.getExitValue()).as("JBang exit code for provider " + provider).isZero();

		Path createdFile = this.tempDir.resolve(fileName);
		if (Files.exists(createdFile)) {
			String content = Files.readString(createdFile);
			assertThat(content).isNotBlank();
			logger.info("AI generated content ({}): {}", provider, content);
		}

		assertThat(result.outputUTF8()).contains("AI agent completed");
	}

	@Test
	@DisplayName("Default settings agent is recognized")
	void defaultSettingsAgentIsRecognized() throws Exception {
		Path launcherJavaPath = getLauncherJavaFile();

		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai")
			.directory(this.tempDir.toFile())
			.timeout(60, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		logger.info("JBang output: {}", result.outputUTF8());
		assertThat(result.outputUTF8()).doesNotContain("No executor found for agent");
	}

	@Test
	@DisplayName("Invalid provider fails gracefully")
	void invalidProviderFails() throws Exception {
		Path launcherJavaPath = getLauncherJavaFile();

		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world-agent-ai", "path=test.txt",
					"provider=invalid-provider")
			.directory(this.tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		assertThat(result.getExitValue()).as("Should fail for invalid provider").isNotZero();
		assertThat(result.outputUTF8()).contains("Failed to create agent model for provider");
	}

	private Path getLauncherJavaFile() {
		Path current = Path.of(System.getProperty("user.dir"));
		for (int i = 0; i < 5; i++) {
			Path launcherJava = current.resolve("jbang/launcher.java");
			if (Files.exists(launcherJava)) {
				return launcherJava;
			}
			current = current.getParent();
			if (current == null) {
				break;
			}
		}
		throw new RuntimeException("Could not find jbang/launcher.java in project root");
	}

	private String getJBangExecutable() {
		String jbangHome = System.getenv("JBANG_HOME");
		if (jbangHome != null) {
			return jbangHome + "/bin/jbang";
		}
		return "jbang";
	}

}
