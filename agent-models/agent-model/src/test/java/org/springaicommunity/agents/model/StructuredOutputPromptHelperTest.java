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

package org.springaicommunity.agents.model;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredOutputPromptHelperTest {

	private static final Map<String, Object> SCHEMA = Map.of("type", "object", "properties",
			Map.of("name", Map.of("type", "string")));

	@Test
	void tier3WrapIncludesNoTextOutsideJsonInstruction() {
		String result = StructuredOutputPromptHelper.wrapGoalWithSchema("Classify this", SCHEMA);

		assertThat(result).contains("You MUST respond with valid JSON");
		assertThat(result).contains("<json_schema>");
		assertThat(result).contains("</json_schema>");
		assertThat(result).contains("Do not include any text outside the JSON object.");
		assertThat(result).endsWith("Classify this");
	}

	@Test
	void tier2WrapOmitsNoTextOutsideJsonInstruction() {
		String result = StructuredOutputPromptHelper.wrapGoalWithSchema("Classify this", SCHEMA, true);

		assertThat(result).contains("MUST conform to this JSON schema");
		assertThat(result).contains("<json_schema>");
		assertThat(result).doesNotContain("Do not include any text outside the JSON object.");
		assertThat(result).endsWith("Classify this");
	}

	@Test
	void schemaIsSerializedAsJson() {
		String result = StructuredOutputPromptHelper.wrapGoalWithSchema("Goal", SCHEMA);

		assertThat(result).contains("\"type\" : \"object\"");
		assertThat(result).contains("\"properties\"");
	}

	@Test
	void goalPreservedAtEnd() {
		String goal = "Do something complex with multiple lines\nLine two";
		String result = StructuredOutputPromptHelper.wrapGoalWithSchema(goal, SCHEMA);

		assertThat(result).endsWith(goal);
	}

}
