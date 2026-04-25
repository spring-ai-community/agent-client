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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.exceptions.GeminiSDKException;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.tck.Provider;
import org.springaicommunity.agents.tck.ProviderParityTCK;
import org.springaicommunity.sandbox.LocalSandbox;

/**
 * Parity TCK wired for Gemini CLI provider.
 *
 * <p>
 * Requires GEMINI_API_KEY or GOOGLE_API_KEY. Skips gracefully if unavailable.
 *
 * @author Spring AI Community
 * @since 0.14.0
 */
@EnabledIf("hasGeminiApiKey")
class GeminiProviderParityIT extends ProviderParityTCK {

	@Override
	protected Provider getProvider() {
		return Provider.GEMINI;
	}

	static boolean hasGeminiApiKey() {
		String geminiKey = System.getenv("GEMINI_API_KEY");
		String googleKey = System.getenv("GOOGLE_API_KEY");
		return (geminiKey != null && !geminiKey.trim().isEmpty()) || (googleKey != null && !googleKey.trim().isEmpty());
	}

	@BeforeEach
	void setUp() {
		try {
			this.sandbox = new LocalSandbox(tempDir);

			CLIOptions cliOptions = CLIOptions.builder().debug(true).yoloMode(true).build();

			GeminiClient geminiClient = GeminiClient.create(cliOptions, tempDir);

			GeminiAgentOptions options = GeminiAgentOptions.builder()
				.model("gemini-2.5-flash")
				.timeout(Duration.ofMinutes(3))
				.yolo(true)
				.build();

			this.agentModel = new GeminiAgentModel(geminiClient, options, sandbox);

			assumeTrue(agentModel.isAvailable(), "Gemini CLI must be available");
		}
		catch (GeminiSDKException e) {
			assumeTrue(false, "Failed to initialize Gemini CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return GeminiAgentOptions.builder()
			.model("gemini-2.5-flash")
			.timeout(Duration.ofSeconds(10))
			.yolo(true)
			.build();
	}

}
