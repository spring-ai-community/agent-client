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

package org.springaicommunity.agents.gemini;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springaicommunity.agents.model.mcp.McpServerDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for portable McpServerDefinition → Gemini MCP config translation in
 * GeminiAgentModel.
 */
class GeminiAgentModelMcpTranslationTest {

	private GeminiAgentModel model;

	@BeforeEach
	void setUp() {
		this.model = new GeminiAgentModel(null, null, null);
	}

	@Test
	@SuppressWarnings("unchecked")
	void translateStdioDefinition() {
		McpServerDefinition def = new McpServerDefinition.StdioDefinition("npx",
				List.of("-y", "@modelcontextprotocol/server-brave-search"), Map.of("BRAVE_API_KEY", "key123"));

		Map<String, Object> config = this.model.toGeminiMcpConfig(def);

		assertThat(config.get("command")).isEqualTo("npx");
		assertThat((List<String>) config.get("args")).containsExactly("-y",
				"@modelcontextprotocol/server-brave-search");
		assertThat((Map<String, String>) config.get("env")).containsEntry("BRAVE_API_KEY", "key123");
	}

	@Test
	void translateStdioWithEmptyArgsAndEnv() {
		McpServerDefinition def = new McpServerDefinition.StdioDefinition("node");

		Map<String, Object> config = this.model.toGeminiMcpConfig(def);

		assertThat(config.get("command")).isEqualTo("node");
		assertThat(config).doesNotContainKey("args");
		assertThat(config).doesNotContainKey("env");
	}

	@Test
	void translateSseDefinition() {
		McpServerDefinition def = new McpServerDefinition.SseDefinition("http://localhost:8080/sse",
				Map.of("Authorization", "Bearer tok"));

		Map<String, Object> config = this.model.toGeminiMcpConfig(def);

		assertThat(config.get("url")).isEqualTo("http://localhost:8080/sse");
		@SuppressWarnings("unchecked")
		Map<String, String> headers = (Map<String, String>) config.get("headers");
		assertThat(headers).containsEntry("Authorization", "Bearer tok");
	}

	@Test
	void translateSseWithNoHeaders() {
		McpServerDefinition def = new McpServerDefinition.SseDefinition("http://localhost:8080/sse");

		Map<String, Object> config = this.model.toGeminiMcpConfig(def);

		assertThat(config.get("url")).isEqualTo("http://localhost:8080/sse");
		assertThat(config).doesNotContainKey("headers");
	}

	@Test
	void translateHttpDefinition() {
		McpServerDefinition def = new McpServerDefinition.HttpDefinition("http://localhost:3000/mcp",
				Map.of("X-Api-Key", "secret"));

		Map<String, Object> config = this.model.toGeminiMcpConfig(def);

		assertThat(config.get("url")).isEqualTo("http://localhost:3000/mcp");
		@SuppressWarnings("unchecked")
		Map<String, String> headers = (Map<String, String>) config.get("headers");
		assertThat(headers).containsEntry("X-Api-Key", "secret");
	}

	@Test
	void translateHttpWithNoHeaders() {
		McpServerDefinition def = new McpServerDefinition.HttpDefinition("http://localhost:3000/mcp");

		Map<String, Object> config = this.model.toGeminiMcpConfig(def);

		assertThat(config.get("url")).isEqualTo("http://localhost:3000/mcp");
		assertThat(config).doesNotContainKey("headers");
	}

}
