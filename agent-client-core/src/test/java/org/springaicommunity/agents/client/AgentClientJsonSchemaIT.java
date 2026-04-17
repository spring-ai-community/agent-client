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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.amp.AmpAgentModel;
import org.springaicommunity.agents.amp.AmpAgentOptions;
import org.springaicommunity.agents.ampsdk.AmpClient;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.codex.CodexAgentModel;
import org.springaicommunity.agents.codex.CodexAgentOptions;
import org.springaicommunity.agents.codexsdk.CodexClient;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.sandbox.LocalSandbox;
import org.zeroturnaround.exec.ProcessExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@code AgentClient.goal(prompt).jsonSchema(schema).run()} across
 * all supported CLIs.
 *
 * <p>
 * Each nested class tests a specific CLI provider. Tests skip gracefully if the CLI is
 * not installed or the required API key is missing.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> Claude and Codex tests cannot run from within a Claude Code
 * session (nesting guard). Use {@code ~/scripts/claude-run.sh} or run from terminal/CI.
 * </p>
 *
 * @author Mark Pollack
 */
class AgentClientJsonSchemaIT {

	private static final Logger logger = LoggerFactory.getLogger(AgentClientJsonSchemaIT.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	// Simple schema: { "name": string, "color": string }
	private static final Map<String, Object> FRUIT_SCHEMA = Map.of("type", "object", "properties",
			Map.of("name", Map.of("type", "string", "description", "Name of a fruit"), "color",
					Map.of("type", "string", "description", "Color of the fruit")),
			"required", new String[] { "name", "color" }, "additionalProperties", false);

	private static final String FRUIT_PROMPT = "Return a JSON object describing an apple. Do not read or write any files.";

	private static void assertValidFruitResponse(AgentClientResponse response, String provider) throws Exception {
		assertThat(response).as("%s response", provider).isNotNull();
		assertThat(response.getResult()).as("%s result not blank", provider).isNotBlank();

		String json = response.getResult().strip();
		logger.info("{} raw response: {}", provider, json);

		// Extract JSON object: handle markdown fences, prose prefixes, etc.
		json = extractJsonObject(json, provider);

		Map<String, Object> parsed = MAPPER.readValue(json, new TypeReference<>() {
		});
		assertThat(parsed).as("%s parsed JSON", provider).containsKey("name").containsKey("color");
		assertThat(parsed.get("name").toString()).as("%s name", provider).isNotBlank();
		assertThat(parsed.get("color").toString()).as("%s color", provider).isNotBlank();

		logger.info("{} structured output: name={}, color={}", provider, parsed.get("name"), parsed.get("color"));
	}

	// ── Claude ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Claude — jsonSchema via --json-schema (Tier 1)")
	class Claude {

		@TempDir
		Path tempDir;

		private AgentClient client;

		private ClaudeAgentModel model;

		@BeforeEach
		void setUp() throws IOException {
			assumeTrue(isCliAvailable("claude"), "Claude CLI must be available");

			Files.writeString(tempDir.resolve("README.md"), "# Test\n");

			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model("claude-haiku-4-5-20251001")
				.maxTurns(1)
				.yolo(true)
				.timeout(Duration.ofSeconds(60))
				.build();

			this.model = ClaudeAgentModel.builder()
				.workingDirectory(tempDir)
				.timeout(Duration.ofSeconds(60))
				.defaultOptions(options)
				.build();

			assumeTrue(this.model.isAvailable(), "Claude agent must be available");

			this.client = AgentClient.builder(this.model)
				.defaultWorkingDirectory(tempDir)
				.defaultTimeout(Duration.ofMinutes(2))
				.build();
		}

		@Test
		@DisplayName("jsonSchema produces valid structured JSON")
		void jsonSchemaReturnsStructuredOutput() throws Exception {
			AgentClientResponse response = this.client.goal(FRUIT_PROMPT).jsonSchema(FRUIT_SCHEMA).run();

			assertValidFruitResponse(response, "Claude");
		}

	}

	// ── Codex ───────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Codex — jsonSchema via --output-schema temp file (Tier 1)")
	class Codex {

		@TempDir
		Path tempDir;

		private AgentClient client;

		@BeforeEach
		void setUp() throws Exception {
			assumeTrue(isCliAvailable("codex"), "Codex CLI must be available");

			String apiKey = System.getenv("OPENAI_API_KEY");
			assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY must be set");

			Files.writeString(tempDir.resolve("README.md"), "# Test\n");
			new ProcessExecutor().command("git", "init").directory(tempDir.toFile()).execute();

			org.springaicommunity.agents.codexsdk.types.ExecuteOptions execOpts = org.springaicommunity.agents.codexsdk.types.ExecuteOptions
				.builder()
				.dangerouslyBypassSandbox(true)
				.timeout(Duration.ofMinutes(3))
				.skipGitCheck(false)
				.build();

			CodexClient codexClient = CodexClient.create(execOpts, tempDir);

			CodexAgentOptions options = CodexAgentOptions.builder()
				.model(null)
				.timeout(Duration.ofMinutes(3))
				.dangerouslyBypassSandbox(true)
				.skipGitCheck(false)
				.build();

			LocalSandbox sandbox = new LocalSandbox(tempDir);
			CodexAgentModel model = new CodexAgentModel(codexClient, options, sandbox);
			assumeTrue(model.isAvailable(), "Codex agent must be available");

			this.client = AgentClient.builder(model)
				.defaultWorkingDirectory(tempDir)
				.defaultTimeout(Duration.ofMinutes(3))
				.build();
		}

		@Test
		@DisplayName("jsonSchema produces valid structured JSON")
		void jsonSchemaReturnsStructuredOutput() throws Exception {
			AgentClientResponse response = this.client.goal(FRUIT_PROMPT).jsonSchema(FRUIT_SCHEMA).run();

			assertValidFruitResponse(response, "Codex");
		}

	}

	// ── Gemini ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Gemini — jsonSchema via prompt embedding + -o json (Tier 2)")
	class Gemini {

		@TempDir
		Path tempDir;

		private AgentClient client;

		@BeforeEach
		void setUp() throws IOException {
			assumeTrue(isCliAvailable("gemini"), "Gemini CLI must be available");

			String apiKey = System.getenv("GEMINI_API_KEY");
			assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY must be set");

			Files.writeString(tempDir.resolve("README.md"), "# Test\n");

			GeminiAgentOptions options = GeminiAgentOptions.builder()
				.model("gemini-2.5-flash")
				.yolo(true)
				.timeout(Duration.ofMinutes(2))
				.build();

			GeminiClient geminiClient = GeminiClient.create();
			LocalSandbox sandbox = new LocalSandbox(tempDir);
			GeminiAgentModel model = new GeminiAgentModel(geminiClient, options, sandbox);
			assumeTrue(model.isAvailable(), "Gemini agent must be available");

			this.client = AgentClient.builder(model)
				.defaultWorkingDirectory(tempDir)
				.defaultTimeout(Duration.ofMinutes(2))
				.build();
		}

		@Test
		@DisplayName("jsonSchema produces valid structured JSON")
		void jsonSchemaReturnsStructuredOutput() throws Exception {
			AgentClientResponse response = this.client.goal(FRUIT_PROMPT).jsonSchema(FRUIT_SCHEMA).run();

			assertValidFruitResponse(response, "Gemini");
		}

	}

	// ── Amp ─────────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Amp — jsonSchema via prompt embedding (Tier 3)")
	class Amp {

		@TempDir
		Path tempDir;

		private AgentClient client;

		@BeforeEach
		void setUp() throws IOException {
			assumeTrue(isCliAvailable("amp"), "Amp CLI must be available");
			// Amp requires authentication — skip if no API key configured
			assumeTrue(System.getenv("AMP_API_KEY") != null || isAmpAuthenticated(),
					"Amp must be authenticated (set AMP_API_KEY or run 'amp login')");

			Files.writeString(tempDir.resolve("README.md"), "# Test\n");

			org.springaicommunity.agents.ampsdk.types.ExecuteOptions execOpts = org.springaicommunity.agents.ampsdk.types.ExecuteOptions
				.builder()
				.dangerouslyAllowAll(true)
				.timeout(Duration.ofMinutes(3))
				.build();

			AmpClient ampClient = AmpClient.create(execOpts, tempDir);

			AmpAgentOptions options = AmpAgentOptions.builder()
				.timeout(Duration.ofMinutes(3))
				.dangerouslyAllowAll(true)
				.build();

			LocalSandbox sandbox = new LocalSandbox(tempDir);
			AmpAgentModel model = new AmpAgentModel(ampClient, options, sandbox);
			assumeTrue(model.isAvailable(), "Amp agent must be available");

			this.client = AgentClient.builder(model)
				.defaultWorkingDirectory(tempDir)
				.defaultTimeout(Duration.ofMinutes(3))
				.build();
		}

		@Test
		@DisplayName("jsonSchema produces valid structured JSON")
		void jsonSchemaReturnsStructuredOutput() throws Exception {
			AgentClientResponse response = this.client.goal(FRUIT_PROMPT).jsonSchema(FRUIT_SCHEMA).run();

			assertValidFruitResponse(response, "Amp");
		}

	}

	// ── Helpers ─────────────────────────────────────────────────────────────

	/**
	 * Extract a JSON object from a response that may contain markdown fences and/or
	 * prose.
	 */
	private static String extractJsonObject(String text, String provider) {
		// Try to find JSON between markdown fences first
		java.util.regex.Matcher fenceMatcher = java.util.regex.Pattern
			.compile("```(?:json)?\\s*\\n?(\\{.*?\\})\\s*\\n?```", java.util.regex.Pattern.DOTALL)
			.matcher(text);
		if (fenceMatcher.find()) {
			return fenceMatcher.group(1).strip();
		}

		// Otherwise find the first JSON object by matching braces
		int braceStart = text.indexOf('{');
		int braceEnd = text.lastIndexOf('}');
		assertThat(braceStart).as("%s response must contain a JSON object, got: %s", provider, text)
			.isGreaterThanOrEqualTo(0);
		return text.substring(braceStart, braceEnd + 1).strip();
	}

	private static boolean isCliAvailable(String command) {
		try {
			Process process = new ProcessBuilder("which", command).start();
			return process.waitFor() == 0;
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check if Amp CLI has cached credentials (avoids interactive login timeout).
	 */
	private static boolean isAmpAuthenticated() {
		try {
			// amp stores tokens in ~/.ampcode/ — check for token file existence
			Path tokenPath = Path.of(System.getProperty("user.home"), ".ampcode", "tokens.json");
			return Files.exists(tokenPath) && Files.size(tokenPath) > 2;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
