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

package org.springaicommunity.agents.codex;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springaicommunity.agents.codexsdk.CodexClient;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.tck.Provider;
import org.springaicommunity.agents.tck.ProviderParityTCK;
import org.springaicommunity.sandbox.LocalSandbox;

/**
 * Parity TCK wired for Codex provider.
 *
 * <p>
 * Uses LOOSE-mode defaults (skipGitCheck=true) so parity tests run in non-git
 * directories. Requires OPENAI_API_KEY.
 *
 * @author Spring AI Community
 * @since 0.14.0
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "Codex CLI not available in CI environment")
class CodexProviderParityIT extends ProviderParityTCK {

	@Override
	protected Provider getProvider() {
		return Provider.CODEX;
	}

	@BeforeEach
	void setUp() {
		String apiKey = System.getenv("OPENAI_API_KEY");
		assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY must be set");

		try {
			this.sandbox = new LocalSandbox(tempDir);

			ExecuteOptions executeOptions = ExecuteOptions.builder()
				.dangerouslyBypassSandbox(true)
				.timeout(Duration.ofMinutes(3))
				.skipGitCheck(true)
				.build();

			CodexClient codexClient = CodexClient.create(executeOptions, tempDir);

			CodexAgentOptions options = CodexAgentOptions.builder()
				.model("gpt-5-codex")
				.timeout(Duration.ofMinutes(3))
				.dangerouslyBypassSandbox(true)
				.skipGitCheck(true)
				.build();

			this.agentModel = new CodexAgentModel(codexClient, options, sandbox);

			assumeTrue(agentModel.isAvailable(), "Codex CLI must be available");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to initialize Codex CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return CodexAgentOptions.builder()
			.model("gpt-5-codex")
			.timeout(Duration.ofSeconds(10))
			.dangerouslyBypassSandbox(true)
			.skipGitCheck(true)
			.build();
	}

}
