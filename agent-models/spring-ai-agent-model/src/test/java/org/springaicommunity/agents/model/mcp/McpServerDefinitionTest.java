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

package org.springaicommunity.agents.model.mcp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link McpServerDefinition} record variants.
 */
class McpServerDefinitionTest {

	@Test
	void stdioDefinitionWithAllFields() {
		var def = new McpServerDefinition.StdioDefinition("npx", List.of("-y", "server"), Map.of("KEY", "val"));

		assertThat(def.command()).isEqualTo("npx");
		assertThat(def.args()).containsExactly("-y", "server");
		assertThat(def.env()).containsEntry("KEY", "val");
	}

	@Test
	void stdioDefinitionConvenienceConstructors() {
		var withArgs = new McpServerDefinition.StdioDefinition("npx", List.of("-y"));
		assertThat(withArgs.env()).isEmpty();

		var commandOnly = new McpServerDefinition.StdioDefinition("npx");
		assertThat(commandOnly.args()).isEmpty();
		assertThat(commandOnly.env()).isEmpty();
	}

	@Test
	void stdioDefinitionRejectsNullCommand() {
		assertThatThrownBy(() -> new McpServerDefinition.StdioDefinition(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void stdioDefinitionRejectsBlankCommand() {
		assertThatThrownBy(() -> new McpServerDefinition.StdioDefinition("  "))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void stdioDefinitionDefensiveCopies() {
		var mutableArgs = new ArrayList<>(List.of("a", "b"));
		var mutableEnv = new HashMap<>(Map.of("K", "V"));
		var def = new McpServerDefinition.StdioDefinition("cmd", mutableArgs, mutableEnv);

		mutableArgs.add("c");
		mutableEnv.put("K2", "V2");

		assertThat(def.args()).containsExactly("a", "b");
		assertThat(def.env()).containsOnlyKeys("K");
	}

	@Test
	void stdioDefinitionArgsAreImmutable() {
		var def = new McpServerDefinition.StdioDefinition("cmd", List.of("a"), Map.of());
		assertThatThrownBy(() -> def.args().add("b")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void sseDefinitionWithAllFields() {
		var def = new McpServerDefinition.SseDefinition("http://localhost:8080/sse",
				Map.of("Authorization", "Bearer tok"));

		assertThat(def.url()).isEqualTo("http://localhost:8080/sse");
		assertThat(def.headers()).containsEntry("Authorization", "Bearer tok");
	}

	@Test
	void sseDefinitionConvenienceConstructor() {
		var def = new McpServerDefinition.SseDefinition("http://localhost:8080/sse");
		assertThat(def.headers()).isEmpty();
	}

	@Test
	void sseDefinitionRejectsNullUrl() {
		assertThatThrownBy(() -> new McpServerDefinition.SseDefinition(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void httpDefinitionWithAllFields() {
		var def = new McpServerDefinition.HttpDefinition("http://localhost:3000", Map.of("X-Api-Key", "key123"));

		assertThat(def.url()).isEqualTo("http://localhost:3000");
		assertThat(def.headers()).containsEntry("X-Api-Key", "key123");
	}

	@Test
	void httpDefinitionConvenienceConstructor() {
		var def = new McpServerDefinition.HttpDefinition("http://localhost:3000");
		assertThat(def.headers()).isEmpty();
	}

	@Test
	void httpDefinitionRejectsNullUrl() {
		assertThatThrownBy(() -> new McpServerDefinition.HttpDefinition(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void sealedInterfacePermittedSubtypes() {
		McpServerDefinition stdio = new McpServerDefinition.StdioDefinition("cmd");
		McpServerDefinition sse = new McpServerDefinition.SseDefinition("http://localhost");
		McpServerDefinition http = new McpServerDefinition.HttpDefinition("http://localhost");

		assertThat(stdio).isInstanceOf(McpServerDefinition.StdioDefinition.class);
		assertThat(sse).isInstanceOf(McpServerDefinition.SseDefinition.class);
		assertThat(http).isInstanceOf(McpServerDefinition.HttpDefinition.class);
	}

}
