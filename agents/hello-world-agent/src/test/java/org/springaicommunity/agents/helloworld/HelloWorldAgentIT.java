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

package org.springaicommunity.agents.helloworld;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Integration test for HelloWorldAgent via JBang launcher. Tests end-to-end JBang runner
 * functionality with the hello-world agent.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class HelloWorldAgentIT {

	private static final Logger logger = LoggerFactory.getLogger(HelloWorldAgentIT.class);

	@TempDir
	Path tempDir;

	static Stream<Arguments> contentVariants() {
		return Stream.of(
				Arguments.of("custom-test.txt", "Hello from integration test!", "Hello from integration test!"),
				Arguments.of("default-test.txt", null, "HelloWorld"));
	}

	@ParameterizedTest(name = "content={1}")
	@MethodSource("contentVariants")
	@DisplayName("JBang launcher creates file with expected content")
	void launcherCreatesFileWithContent(String fileName, String content, String expectedContent) throws Exception {
		Path launcherJavaPath = getLauncherJavaFile();
		Path expectedFile = this.tempDir.resolve(fileName);

		List<String> command = new ArrayList<>();
		command.add(getJBangExecutable());
		command.add(launcherJavaPath.toString());
		command.add("hello-world");
		command.add("path=" + fileName);
		if (content != null) {
			command.add("content=" + content);
		}

		ProcessResult result = new ProcessExecutor().command(command)
			.directory(this.tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		logger.info("JBang output: {}", result.outputUTF8());
		assertThat(result.getExitValue()).as("JBang exit code").isZero();
		assertThat(expectedFile).exists();
		assertThat(Files.readString(expectedFile)).isEqualTo(expectedContent);
	}

	@Test
	@DisplayName("Missing required input fails gracefully")
	void missingRequiredInputFails() throws Exception {
		Path launcherJavaPath = getLauncherJavaFile();

		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "hello-world", "content=test content")
			.directory(this.tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		assertThat(result.getExitValue()).as("Should fail for missing required input").isNotZero();
	}

	@Test
	@DisplayName("Unknown agent fails gracefully")
	void unknownAgentFails() throws Exception {
		Path launcherJavaPath = getLauncherJavaFile();

		ProcessResult result = new ProcessExecutor()
			.command(getJBangExecutable(), launcherJavaPath.toString(), "unknown-agent")
			.directory(this.tempDir.toFile())
			.timeout(30, TimeUnit.SECONDS)
			.readOutput(true)
			.execute();

		assertThat(result.getExitValue()).as("Should fail for unknown agent").isNotZero();
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
