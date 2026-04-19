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

import java.time.Duration;
import java.util.Map;

import org.springaicommunity.agents.model.AgentOptions;

/**
 * Portable {@link AgentOptions} implementation for integration testing. Carries the
 * cross-cutting options ({@code maxTurns}, {@code autoApprove},
 * {@code systemInstructions}) that every provider should honor, without importing any
 * provider-specific classes.
 *
 * <p>
 * Use this in provider test modules to verify portable option fallback paths. The TCK
 * module is available as a test-scoped dependency in all provider modules.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.14.0
 */
public final class PortableAgentOptions implements AgentOptions {

	private final Integer maxTurns;

	private final boolean autoApprove;

	private final String systemInstructions;

	private final Map<String, Object> jsonSchema;

	public PortableAgentOptions(Integer maxTurns, boolean autoApprove, String systemInstructions) {
		this(maxTurns, autoApprove, systemInstructions, null);
	}

	public PortableAgentOptions(Integer maxTurns, boolean autoApprove, String systemInstructions,
			Map<String, Object> jsonSchema) {
		this.maxTurns = maxTurns;
		this.autoApprove = autoApprove;
		this.systemInstructions = systemInstructions;
		this.jsonSchema = jsonSchema;
	}

	@Override
	public Integer getMaxTurns() {
		return this.maxTurns;
	}

	@Override
	public boolean isAutoApprove() {
		return this.autoApprove;
	}

	@Override
	public String getSystemInstructions() {
		return this.systemInstructions;
	}

	@Override
	public Map<String, Object> getJsonSchema() {
		return this.jsonSchema;
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

}
