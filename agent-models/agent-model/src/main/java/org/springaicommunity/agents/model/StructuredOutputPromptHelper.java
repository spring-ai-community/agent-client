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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared helper for embedding JSON schema instructions into agent prompts. Used by Tier 2
 * (prompt + JSON output mode) and Tier 3 (prompt-only) agent models that lack native
 * constrained decoding support.
 *
 * @author Mark Pollack
 * @since 0.12.0
 */
public final class StructuredOutputPromptHelper {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private StructuredOutputPromptHelper() {
	}

	/**
	 * Wrap a goal with full schema guardrails for Tier 3 models (prompt-only, no JSON
	 * output mode). Includes instruction to not produce any text outside the JSON object.
	 * @param goal the original goal text
	 * @param jsonSchema the JSON schema to embed
	 * @return the goal with schema instructions prepended
	 */
	public static String wrapGoalWithSchema(String goal, Map<String, Object> jsonSchema) {
		return wrapGoalWithSchema(goal, jsonSchema, false);
	}

	/**
	 * Wrap a goal with schema instructions.
	 * @param goal the original goal text
	 * @param jsonSchema the JSON schema to embed
	 * @param jsonOutputModeEnabled when {@code true}, omits the "no text outside JSON"
	 * instruction since the CLI's output mode flag already constrains format (Tier 2)
	 * @return the goal with schema instructions prepended
	 */
	public static String wrapGoalWithSchema(String goal, Map<String, Object> jsonSchema,
			boolean jsonOutputModeEnabled) {
		String serializedSchema;
		try {
			serializedSchema = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to serialize JSON schema", e);
		}

		StringBuilder sb = new StringBuilder();
		if (jsonOutputModeEnabled) {
			sb.append("IMPORTANT: Your response MUST conform to this JSON schema:\n");
		}
		else {
			sb.append("IMPORTANT: You MUST respond with valid JSON conforming to this schema:\n");
		}
		sb.append("<json_schema>\n");
		sb.append(serializedSchema).append('\n');
		sb.append("</json_schema>\n");
		if (!jsonOutputModeEnabled) {
			sb.append("Do not include any text outside the JSON object.\n");
		}
		sb.append('\n');
		sb.append(goal);
		return sb.toString();
	}

}
