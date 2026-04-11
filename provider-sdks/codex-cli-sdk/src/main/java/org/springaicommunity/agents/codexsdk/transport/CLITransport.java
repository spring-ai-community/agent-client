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

package org.springaicommunity.agents.codexsdk.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.codexsdk.exceptions.CodexSDKException;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.codexsdk.types.ExecuteResult;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Low-level transport layer for Codex CLI communication using zt-exec for robust process
 * management.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CLITransport {

	private static final Logger logger = LoggerFactory.getLogger(CLITransport.class);

	private final Path workingDirectory;

	private final String codexCliPath;

	public CLITransport(Path workingDirectory) {
		this(workingDirectory, null);
	}

	public CLITransport(Path workingDirectory, String codexCliPath) {
		this.workingDirectory = workingDirectory;
		this.codexCliPath = codexCliPath != null ? codexCliPath : CodexCliDiscovery.discoverCodexCli();

		// Validate the CLI is functional
		if (!CodexCliDiscovery.validateCodexCli(this.codexCliPath)) {
			throw new CodexSDKException("Codex CLI at " + this.codexCliPath + " is not functional");
		}

		logger.info("Codex CLI initialized at: {}", this.codexCliPath);
	}

	/**
	 * Execute a prompt via Codex CLI in execute mode.
	 * @param prompt the user prompt/goal to execute
	 * @param options execution options
	 * @return the execution result
	 * @throws CodexSDKException if execution fails
	 */
	public ExecuteResult execute(String prompt, ExecuteOptions options) {
		if (prompt == null || prompt.isEmpty()) {
			throw new IllegalArgumentException("Prompt cannot be null or empty");
		}
		if (options == null) {
			options = ExecuteOptions.defaultOptions();
		}

		List<String> command = buildCommand(prompt, options, null);
		return executeCommand(command, options);
	}

	/**
	 * Resume a previous session with a new prompt.
	 * @param sessionId the session ID to resume
	 * @param prompt the new prompt/goal to execute
	 * @param options execution options
	 * @return the execution result
	 * @throws CodexSDKException if execution fails
	 */
	public ExecuteResult resume(String sessionId, String prompt, ExecuteOptions options) {
		if (sessionId == null || sessionId.isEmpty()) {
			throw new IllegalArgumentException("Session ID cannot be null or empty");
		}
		if (prompt == null || prompt.isEmpty()) {
			throw new IllegalArgumentException("Prompt cannot be null or empty");
		}
		if (options == null) {
			options = ExecuteOptions.defaultOptions();
		}

		List<String> command = buildCommand(prompt, options, sessionId);
		return executeCommand(command, options);
	}

	private ExecuteResult executeCommand(List<String> command, ExecuteOptions options) {
		Instant startTime = Instant.now();
		logger.debug("Executing Codex CLI command: {}", command);

		try {
			ProcessResult result = new ProcessExecutor().command(command)
				.directory(workingDirectory.toFile())
				.timeout(options.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
				.readOutput(true)
				.destroyOnExit()
				.execute();

			Duration duration = Duration.between(startTime, Instant.now());

			String combinedOutput = result.outputUTF8();
			int exitCode = result.getExitValue();

			logger.debug("Codex CLI execution completed. Exit code: {}, Duration: {}ms", exitCode, duration.toMillis());

			// Codex outputs activity to stderr and final message to stdout
			// Since zt-exec combines them by default, we parse from combined output
			// The response appears between the "codex" marker and "tokens used" marker
			String[] lines = combinedOutput.split("\n");

			// Find the last "codex" marker and "tokens used" marker
			int codexMarker = -1;
			int tokensMarker = -1;
			for (int i = lines.length - 1; i >= 0; i--) {
				if (tokensMarker < 0 && lines[i].trim().equals("tokens used")) {
					tokensMarker = i;
				}
				if (codexMarker < 0 && lines[i].trim().equals("codex")) {
					codexMarker = i;
				}
				if (codexMarker >= 0 && tokensMarker >= 0) {
					break;
				}
			}

			String activityLog = combinedOutput;
			String finalMessage = "";
			if (codexMarker >= 0) {
				// Extract text between "codex" marker and "tokens used" (or end of
				// output)
				int end = tokensMarker > codexMarker ? tokensMarker : lines.length;
				if (codexMarker + 1 < end) {
					finalMessage = String.join("\n", java.util.Arrays.copyOfRange(lines, codexMarker + 1, end));
				}
			}
			else if (tokensMarker >= 0 && tokensMarker > 0) {
				// Fallback: no "codex" marker, take everything before "tokens used"
				finalMessage = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, tokensMarker));
			}
			else {
				// No markers found — treat entire output as the message
				finalMessage = combinedOutput;
			}

			// Extract model from activity log
			String model = extractModel(activityLog);

			return new ExecuteResult(finalMessage, activityLog, exitCode, duration, model);
		}
		catch (IOException e) {
			Duration duration = Duration.between(startTime, Instant.now());
			logger.error("Codex CLI execution failed after {}ms: {}", duration.toMillis(), e.getMessage());
			throw new CodexSDKException("Failed to execute Codex CLI command", e);
		}
		catch (InterruptedException e) {
			Duration duration = Duration.between(startTime, Instant.now());
			logger.error("Codex CLI execution interrupted after {}ms", duration.toMillis());
			Thread.currentThread().interrupt();
			throw new CodexSDKException("Codex CLI execution interrupted", e);
		}
		catch (Exception e) {
			Duration duration = Duration.between(startTime, Instant.now());
			logger.error("Codex CLI execution failed after {}ms: {}", duration.toMillis(), e.getMessage());
			throw new CodexSDKException("Failed to execute Codex CLI command", e);
		}
	}

	private List<String> buildCommand(String prompt, ExecuteOptions options, String sessionId) {
		List<String> command = new ArrayList<>();

		// Base command + exec subcommand
		command.add(codexCliPath);
		command.add("exec");

		// Model is an exec-specific option (not a global flag)
		if (options.getModel() != null && !options.getModel().isEmpty()) {
			command.add("--model");
			command.add(options.getModel());
		}

		// Execution mode
		if (options.isDangerouslyBypassSandbox()) {
			command.add("--dangerously-bypass-approvals-and-sandbox");
		}
		else if (options.isFullAuto()) {
			command.add("--full-auto");
		}
		else {
			command.add("--sandbox");
			command.add(options.getSandboxMode().getValue());
		}

		// Working directory via -C flag
		if (options.getWorkingDirectory() != null) {
			command.add("-C");
			command.add(options.getWorkingDirectory().toString());
		}

		// Git check skip
		if (options.isSkipGitCheck()) {
			command.add("--skip-git-repo-check");
		}

		// JSON output mode
		if (options.isJsonOutput()) {
			command.add("--json");
		}

		// Output schema for structured output
		if (options.getOutputSchema() != null) {
			command.add("--output-schema");
			command.add(options.getOutputSchema().toString());
		}

		// Session resume
		if (sessionId != null && !sessionId.isEmpty()) {
			command.add("resume");
			command.add(sessionId);
		}

		// Prompt as final argument
		command.add(prompt);

		return command;
	}

	/**
	 * Extracts model name from Codex stderr output. Model appears in stderr like: "model:
	 * gpt-5-codex"
	 * @param stderr the stderr output
	 * @return extracted model name or "codex-default" if not found
	 */
	private String extractModel(String stderr) {
		if (stderr == null || stderr.isEmpty()) {
			return "codex-default";
		}

		// Look for "model: <model-name>" in stderr
		String[] lines = stderr.split("\n");
		for (String line : lines) {
			if (line.startsWith("model:")) {
				return line.substring("model:".length()).trim();
			}
		}

		return "codex-default";
	}

	/**
	 * Checks if the Codex CLI is available and functional.
	 * @return true if available
	 */
	public boolean checkAvailability() {
		return CodexCliDiscovery.validateCodexCli(codexCliPath);
	}

	public String getCodexCliPath() {
		return codexCliPath;
	}

}
