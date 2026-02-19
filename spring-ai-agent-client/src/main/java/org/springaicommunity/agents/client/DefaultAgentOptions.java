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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;

/**
 * Default implementation of {@link AgentOptions} for use in client layer.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class DefaultAgentOptions implements AgentOptions {

	private String workingDirectory;

	private Duration timeout = Duration.ofMinutes(10);

	private Map<String, String> environmentVariables = new HashMap<>();

	private String model;

	private Map<String, Object> extras = new HashMap<>();

	private Map<String, McpServerDefinition> mcpServerDefinitions = Map.of();

	public DefaultAgentOptions() {
	}

	public DefaultAgentOptions(String workingDirectory, Duration timeout, Map<String, String> environmentVariables,
			String model) {
		this.workingDirectory = workingDirectory;
		this.timeout = timeout != null ? timeout : Duration.ofMinutes(10);
		this.environmentVariables = environmentVariables != null ? new HashMap<>(environmentVariables)
				: new HashMap<>();
		this.model = model;
	}

	@Override
	public String getWorkingDirectory() {
		return this.workingDirectory;
	}

	@Override
	public Duration getTimeout() {
		return this.timeout;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return this.environmentVariables;
	}

	@Override
	public String getModel() {
		return this.model;
	}

	@Override
	public Map<String, Object> getExtras() {
		return this.extras;
	}

	@Override
	public Map<String, McpServerDefinition> getMcpServerDefinitions() {
		return this.mcpServerDefinitions;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public void setEnvironmentVariables(Map<String, String> environmentVariables) {
		this.environmentVariables = environmentVariables != null ? environmentVariables : new HashMap<>();
	}

	public void setModel(String model) {
		this.model = model;
	}

	public void setExtras(Map<String, Object> extras) {
		this.extras = extras != null ? extras : new HashMap<>();
	}

	public void setMcpServerDefinitions(Map<String, McpServerDefinition> mcpServerDefinitions) {
		this.mcpServerDefinitions = mcpServerDefinitions != null ? Map.copyOf(mcpServerDefinitions) : Map.of();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String workingDirectory;

		private Duration timeout = Duration.ofMinutes(10);

		private Map<String, String> environmentVariables = new HashMap<>();

		private String model;

		private Map<String, Object> extras = new HashMap<>();

		private Map<String, McpServerDefinition> mcpServerDefinitions = Map.of();

		public Builder workingDirectory(String workingDirectory) {
			this.workingDirectory = workingDirectory;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder environmentVariables(Map<String, String> environmentVariables) {
			this.environmentVariables = environmentVariables != null ? new HashMap<>(environmentVariables)
					: new HashMap<>();
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder extras(Map<String, Object> extras) {
			this.extras = extras != null ? new HashMap<>(extras) : new HashMap<>();
			return this;
		}

		public Builder mcpServerDefinitions(Map<String, McpServerDefinition> mcpServerDefinitions) {
			this.mcpServerDefinitions = mcpServerDefinitions != null ? Map.copyOf(mcpServerDefinitions) : Map.of();
			return this;
		}

		// IMPORTANT: When adding fields to DefaultAgentOptions, update this method.
		public Builder from(AgentOptions agentOptions) {
			if (agentOptions != null) {
				this.workingDirectory = agentOptions.getWorkingDirectory();
				this.timeout = agentOptions.getTimeout();
				this.environmentVariables = agentOptions.getEnvironmentVariables() != null
						? new HashMap<>(agentOptions.getEnvironmentVariables()) : new HashMap<>();
				this.model = agentOptions.getModel();
				this.extras = agentOptions.getExtras() != null ? new HashMap<>(agentOptions.getExtras())
						: new HashMap<>();
				this.mcpServerDefinitions = agentOptions.getMcpServerDefinitions() != null
						? agentOptions.getMcpServerDefinitions() : Map.of();
			}
			return this;
		}

		public DefaultAgentOptions build() {
			DefaultAgentOptions options = new DefaultAgentOptions();
			options.setWorkingDirectory(this.workingDirectory);
			options.setTimeout(this.timeout);
			options.setEnvironmentVariables(this.environmentVariables);
			options.setModel(this.model);
			options.setExtras(this.extras);
			options.setMcpServerDefinitions(this.mcpServerDefinitions);
			return options;
		}

	}

}