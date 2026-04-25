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

package org.springaicommunity.agents.claude;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.tck.Provider;
import org.springaicommunity.agents.tck.ProviderParityTCK;
import org.springaicommunity.claude.agent.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.sandbox.LocalSandbox;

/**
 * Parity TCK wired for Claude Code provider.
 *
 * <p>
 * Requires Claude CLI installed and authenticated. Skips gracefully if unavailable.
 *
 * @author Spring AI Community
 * @since 0.14.0
 */
class ClaudeProviderParityIT extends ProviderParityTCK {

	@Override
	protected Provider getProvider() {
		return Provider.CLAUDE;
	}

	@BeforeEach
	void setUp() {
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available");

		try {
			this.sandbox = new LocalSandbox(tempDir);

			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model("claude-haiku-4-5-20251001")
				.timeout(Duration.ofMinutes(2))
				.yolo(true)
				.build();

			this.agentModel = ClaudeAgentModel.builder().workingDirectory(tempDir).defaultOptions(options).build();

			assumeTrue(agentModel.isAvailable(), "Claude agent must be available");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to initialize Claude CLI: " + e.getMessage());
		}
	}

	@Override
	protected AgentOptions createShortTimeoutOptions() {
		return ClaudeAgentOptions.builder()
			.model("claude-haiku-4-5-20251001")
			.timeout(Duration.ofSeconds(10))
			.yolo(true)
			.build();
	}

	private static boolean isClaudeCliAvailable() {
		try {
			return ClaudeCliDiscovery.discoverClaudeCli() != null;
		}
		catch (Exception e) {
			return false;
		}
	}

}
