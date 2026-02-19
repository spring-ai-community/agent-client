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

package org.springaicommunity.agents.model;

import org.springaicommunity.agents.model.mcp.McpServerDefinition;
import org.springframework.ai.model.ModelOptions;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration options for autonomous development agents. Extends ModelOptions to
 * provide agent-specific settings such as execution timeouts, working directories, and
 * environment variables.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public interface AgentOptions extends ModelOptions {

	/**
	 * Get the working directory for agent execution.
	 * @return the working directory path
	 */
	String getWorkingDirectory();

	/**
	 * Get the timeout for agent operations.
	 * @return the timeout duration
	 */
	Duration getTimeout();

	/**
	 * Get environment variables for agent execution.
	 * @return map of environment variables
	 */
	Map<String, String> getEnvironmentVariables();

	/**
	 * Get the agent model identifier.
	 * @return the model identifier
	 */
	String getModel();

	/**
	 * Get provider-specific extra options for forward compatibility.
	 * @return map of extra options
	 */
	default Map<String, Object> getExtras() {
		return Map.of();
	}

	/**
	 * Get the resolved MCP server definitions. This map contains definitions that have
	 * been resolved by the client layer (e.g., from a catalog) and are ready for
	 * translation by the model layer. This is not a user declaration — it is populated by
	 * the {@code AgentClient} at request time.
	 * @return unmodifiable map of server name to resolved definition, empty by default
	 */
	default Map<String, McpServerDefinition> getMcpServerDefinitions() {
		return Map.of();
	}

}