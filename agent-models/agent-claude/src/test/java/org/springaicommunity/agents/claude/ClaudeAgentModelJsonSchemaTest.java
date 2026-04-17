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

package org.springaicommunity.agents.claude;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.claude.agent.sdk.transport.CLIOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jsonSchema propagation in ClaudeAgentModel.buildCLIOptions().
 *
 * <p>
 * Verifies that portable jsonSchema (set via AgentClient.goal().jsonSchema()) flows
 * through to the CLI --json-schema flag, and that Claude-specific options take
 * precedence.
 * </p>
 */
class ClaudeAgentModelJsonSchemaTest {

	private static final Map<String, Object> FRUIT_SCHEMA = Map.of("type", "object", "properties",
			Map.of("name", Map.of("type", "string"), "color", Map.of("type", "string")), "required",
			new String[] { "name", "color" }, "additionalProperties", false);

	private static final Map<String, Object> ALT_SCHEMA = Map.of("type", "object", "properties",
			Map.of("title", Map.of("type", "string")), "required", new String[] { "title" });

	private ClaudeAgentModel model;

	@BeforeEach
	void setUp() {
		this.model = ClaudeAgentModel.builder().build();
	}

	@AfterEach
	void tearDown() {
		this.model.close();
	}

	@Test
	@DisplayName("Portable jsonSchema from AgentOptions is passed to CLI")
	void portableJsonSchemaPassedToCli() {
		AgentOptions portableOptions = new SimpleAgentOptions(FRUIT_SCHEMA);

		AgentTaskRequest request = new AgentTaskRequest("describe a fruit", Path.of("/tmp"), portableOptions);

		CLIOptions cliOptions = this.model.buildCLIOptions(request);

		assertThat(cliOptions.getJsonSchema()).as("CLI options should contain the portable jsonSchema")
			.isNotNull()
			.isNotEmpty();
		assertThat(cliOptions.getJsonSchema()).containsKey("type");
		assertThat(cliOptions.getJsonSchema()).containsKey("properties");
	}

	@Test
	@DisplayName("Claude-specific jsonSchema takes precedence over portable")
	void claudeSpecificTakesPrecedence() {
		ClaudeAgentOptions claudeOptions = ClaudeAgentOptions.builder().jsonSchema(ALT_SCHEMA).build();

		AgentTaskRequest request = new AgentTaskRequest("describe a fruit", Path.of("/tmp"), claudeOptions);

		ClaudeAgentModel modelWithDefaults = ClaudeAgentModel.builder().defaultOptions(claudeOptions).build();
		try {
			CLIOptions cliOptions = modelWithDefaults.buildCLIOptions(request);

			assertThat(cliOptions.getJsonSchema()).containsKey("properties");
			@SuppressWarnings("unchecked")
			Map<String, Object> props = (Map<String, Object>) cliOptions.getJsonSchema().get("properties");
			assertThat(props).containsKey("title").doesNotContainKey("name");
		}
		finally {
			modelWithDefaults.close();
		}
	}

	@Test
	@DisplayName("No jsonSchema when neither Claude-specific nor portable sets it")
	void noJsonSchemaWhenNotSet() {
		AgentOptions portableOptions = new SimpleAgentOptions(null);

		AgentTaskRequest request = new AgentTaskRequest("describe a fruit", Path.of("/tmp"), portableOptions);

		CLIOptions cliOptions = this.model.buildCLIOptions(request);

		assertThat(cliOptions.getJsonSchema()).as("CLI options should have no jsonSchema").isNull();
	}

	@Test
	@DisplayName("Null request options does not cause NPE")
	void nullRequestOptionsDoesNotCauseNpe() {
		AgentTaskRequest request = new AgentTaskRequest("describe a fruit", Path.of("/tmp"), null);

		CLIOptions cliOptions = this.model.buildCLIOptions(request);

		assertThat(cliOptions.getJsonSchema()).isNull();
	}

	@Test
	@DisplayName("Empty portable jsonSchema is not passed to CLI")
	void emptyPortableSchemaNotPassedToCli() {
		AgentOptions portableOptions = new SimpleAgentOptions(Map.of());

		AgentTaskRequest request = new AgentTaskRequest("describe a fruit", Path.of("/tmp"), portableOptions);

		CLIOptions cliOptions = this.model.buildCLIOptions(request);

		assertThat(cliOptions.getJsonSchema()).as("Empty schema should not be passed").isNull();
	}

	/**
	 * Minimal AgentOptions implementation for testing portable jsonSchema propagation.
	 */
	private static class SimpleAgentOptions implements AgentOptions {

		private final Map<String, Object> jsonSchema;

		SimpleAgentOptions(Map<String, Object> jsonSchema) {
			this.jsonSchema = jsonSchema;
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

}
