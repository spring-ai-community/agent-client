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

package org.springaicommunity.agents.client;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jsonSchema propagation through AgentClient to AgentModel.
 */
class AgentClientJsonSchemaTest {

	private MockAgentModel mockModel;

	@TempDir
	Path testWorkspace;

	private static final Map<String, Object> SCHEMA = Map.of("type", "object", "properties",
			Map.of("name", Map.of("type", "string")), "required", java.util.List.of("name"));

	@BeforeEach
	void setUp() {
		this.mockModel = new MockAgentModel();
	}

	@Test
	void jsonSchemaPropagatedToModelRequestOptions() {
		AgentClient client = AgentClient.create(this.mockModel);

		client.goal("Classify this").workingDirectory(this.testWorkspace).jsonSchema(SCHEMA).run();

		assertThat(this.mockModel.lastRequest.options().getJsonSchema()).isEqualTo(SCHEMA);
	}

	@Test
	void nullJsonSchemaWhenNotSet() {
		AgentClient client = AgentClient.create(this.mockModel);

		client.goal("Simple task").workingDirectory(this.testWorkspace).run();

		assertThat(this.mockModel.lastRequest.options().getJsonSchema()).isNull();
	}

	@Test
	void perRequestOverridesDefaultOptions() {
		Map<String, Object> defaultSchema = Map.of("type", "object", "properties",
				Map.of("x", Map.of("type", "integer")));
		DefaultAgentOptions defaults = DefaultAgentOptions.builder().jsonSchema(defaultSchema).build();
		AgentClient client = AgentClient.builder(this.mockModel).defaultOptions(defaults).build();

		client.goal("Override test").workingDirectory(this.testWorkspace).jsonSchema(SCHEMA).run();

		assertThat(this.mockModel.lastRequest.options().getJsonSchema()).isEqualTo(SCHEMA);
	}

	@Test
	void nullPerRequestDoesNotClobberDefault() {
		Map<String, Object> defaultSchema = Map.of("type", "object", "properties",
				Map.of("x", Map.of("type", "integer")));
		DefaultAgentOptions defaults = DefaultAgentOptions.builder().jsonSchema(defaultSchema).build();
		AgentClient client = AgentClient.builder(this.mockModel).defaultOptions(defaults).build();

		// Do NOT call jsonSchema() on request spec — should inherit from defaults
		client.goal("Inherit test").workingDirectory(this.testWorkspace).run();

		assertThat(this.mockModel.lastRequest.options().getJsonSchema()).isEqualTo(defaultSchema);
	}

}
