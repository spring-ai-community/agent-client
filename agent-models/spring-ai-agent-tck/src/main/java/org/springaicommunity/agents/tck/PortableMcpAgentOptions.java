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

package org.springaicommunity.agents.tck;

import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;

import java.time.Duration;
import java.util.Map;

/**
 * Minimal {@link AgentOptions} implementation that carries portable MCP definitions for
 * integration testing. All other options return defaults/nulls since the model under test
 * supplies its own configuration.
 *
 * @author Spring AI Community
 * @since 0.10.0
 */
public final class PortableMcpAgentOptions implements AgentOptions {

	private final Map<String, McpServerDefinition> mcpDefinitions;

	public PortableMcpAgentOptions(Map<String, McpServerDefinition> mcpDefinitions) {
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
