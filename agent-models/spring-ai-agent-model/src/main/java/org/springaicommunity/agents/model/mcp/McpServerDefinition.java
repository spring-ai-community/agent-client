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

import java.util.List;
import java.util.Map;

/**
 * Portable MCP server definition. Sealed interface with record variants for each
 * transport type: stdio, SSE, and HTTP.
 *
 * <p>
 * These definitions are provider-agnostic and live in the agent-model layer. Provider
 * implementations (e.g., Claude, Gemini) translate them to their native MCP config
 * format.
 *
 * <p>
 * In-process SDK servers are intentionally excluded — they carry live server instances
 * and cannot be expressed portably. Use provider-specific options directly for those.
 *
 * @author Spring AI Community
 * @since 0.10.0
 */
public sealed interface McpServerDefinition permits McpServerDefinition.StdioDefinition,
		McpServerDefinition.SseDefinition, McpServerDefinition.HttpDefinition {

	/**
	 * Stdio-based MCP server. The server is started as a subprocess with the specified
	 * command and arguments.
	 *
	 * @param command the command to execute (e.g., "npx", "node", "python")
	 * @param args command arguments
	 * @param env environment variables to set for the process
	 */
	record StdioDefinition(String command, List<String> args, Map<String, String> env) implements McpServerDefinition {

		public StdioDefinition {
			if (command == null || command.isBlank()) {
				throw new IllegalArgumentException("command must not be null or blank");
			}
			args = args != null ? List.copyOf(args) : List.of();
			env = env != null ? Map.copyOf(env) : Map.of();
		}

		public StdioDefinition(String command, List<String> args) {
			this(command, args, Map.of());
		}

		public StdioDefinition(String command) {
			this(command, List.of(), Map.of());
		}

	}

	/**
	 * Server-Sent Events (SSE) based MCP server. Connects to a remote server via HTTP SSE
	 * transport.
	 *
	 * @param url the SSE endpoint URL
	 * @param headers HTTP headers to include in requests
	 */
	record SseDefinition(String url, Map<String, String> headers) implements McpServerDefinition {

		public SseDefinition {
			if (url == null || url.isBlank()) {
				throw new IllegalArgumentException("url must not be null or blank");
			}
			headers = headers != null ? Map.copyOf(headers) : Map.of();
		}

		public SseDefinition(String url) {
			this(url, Map.of());
		}

	}

	/**
	 * HTTP-based MCP server. Connects to a remote server via HTTP transport.
	 *
	 * @param url the HTTP endpoint URL
	 * @param headers HTTP headers to include in requests
	 */
	record HttpDefinition(String url, Map<String, String> headers) implements McpServerDefinition {

		public HttpDefinition {
			if (url == null || url.isBlank()) {
				throw new IllegalArgumentException("url must not be null or blank");
			}
			headers = headers != null ? Map.copyOf(headers) : Map.of();
		}

		public HttpDefinition(String url) {
			this(url, Map.of());
		}

	}

}
