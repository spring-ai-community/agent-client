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

package org.springaicommunity.agents.codex;

import org.springaicommunity.agents.codexsdk.types.ApprovalPolicy;
import org.springaicommunity.agents.codexsdk.types.SandboxMode;
import org.springaicommunity.agents.model.AgentOptions;

import java.nio.file.Path;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration options for Codex Agent Model implementations.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CodexAgentOptions implements AgentOptions {

	@JsonProperty("model")
	private String model = "gpt-5-codex";

	@JsonProperty("timeout")
	private Duration timeout = Duration.ofMinutes(10);

	@JsonProperty("sandboxMode")
	private SandboxMode sandboxMode = SandboxMode.WORKSPACE_WRITE;

	@JsonProperty("approvalPolicy")
	private ApprovalPolicy approvalPolicy = ApprovalPolicy.NEVER;

	@JsonProperty("fullAuto")
	private boolean fullAuto = true;

	@JsonProperty("skipGitCheck")
	private boolean skipGitCheck = false;

	@JsonProperty("dangerouslyBypassSandbox")
	private boolean dangerouslyBypassSandbox = false;

	@JsonProperty("executablePath")
	private String executablePath;

	@JsonProperty("outputSchema")
	private Path outputSchema;

	@JsonProperty("workingDirectory")
	private String workingDirectory;

	@JsonProperty("environmentVariables")
	private Map<String, String> environmentVariables = Map.of();

	@JsonProperty("extras")
	private Map<String, Object> extras = Map.of();

	private CodexAgentOptions() {
	}

	public String getModel() {
		return model;
	}

	public Duration getTimeout() {
		return timeout;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public SandboxMode getSandboxMode() {
		return sandboxMode;
	}

	public ApprovalPolicy getApprovalPolicy() {
		return approvalPolicy;
	}

	public boolean isFullAuto() {
		return fullAuto;
	}

	public boolean isSkipGitCheck() {
		return skipGitCheck;
	}

	public boolean isDangerouslyBypassSandbox() {
		return dangerouslyBypassSandbox;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public Path getOutputSchema() {
		return outputSchema;
	}

	@Override
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	@Override
	public Map<String, Object> getExtras() {
		return extras;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private final CodexAgentOptions options = new CodexAgentOptions();

		private Builder() {
		}

		public Builder model(String model) {
			options.model = model;
			return this;
		}

		public Builder timeout(Duration timeout) {
			options.timeout = timeout;
			return this;
		}

		public Builder sandboxMode(SandboxMode sandboxMode) {
			options.sandboxMode = sandboxMode;
			return this;
		}

		public Builder approvalPolicy(ApprovalPolicy approvalPolicy) {
			options.approvalPolicy = approvalPolicy;
			return this;
		}

		public Builder fullAuto(boolean fullAuto) {
			options.fullAuto = fullAuto;
			if (fullAuto) {
				// Full-auto implies workspace-write and never approval
				options.sandboxMode = SandboxMode.WORKSPACE_WRITE;
				options.approvalPolicy = ApprovalPolicy.NEVER;
			}
			return this;
		}

		public Builder skipGitCheck(boolean skipGitCheck) {
			options.skipGitCheck = skipGitCheck;
			return this;
		}

		public Builder dangerouslyBypassSandbox(boolean dangerouslyBypassSandbox) {
			options.dangerouslyBypassSandbox = dangerouslyBypassSandbox;
			return this;
		}

		public Builder executablePath(String executablePath) {
			options.executablePath = executablePath;
			return this;
		}

		public Builder outputSchema(Path outputSchema) {
			options.outputSchema = outputSchema;
			return this;
		}

		public Builder extras(Map<String, Object> extras) {
			options.extras = extras != null ? extras : Map.of();
			return this;
		}

		public CodexAgentOptions build() {
			return options;
		}

	}

}
