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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, map-backed implementation of {@link McpServerCatalog}.
 *
 * @author Spring AI Community
 * @since 0.10.0
 */
final class DefaultMcpServerCatalog implements McpServerCatalog {

	private final Map<String, McpServerDefinition> servers;

	DefaultMcpServerCatalog(Map<String, McpServerDefinition> servers) {
		this.servers = Map.copyOf(servers != null ? servers : Map.of());
	}

	@Override
	public Map<String, McpServerDefinition> getAll() {
		return this.servers;
	}

	@Override
	public Map<String, McpServerDefinition> resolve(Collection<String> names) {
		if (names == null || names.isEmpty()) {
			return Map.of();
		}
		Map<String, McpServerDefinition> resolved = new LinkedHashMap<>();
		for (String name : names) {
			McpServerDefinition definition = this.servers.get(name);
			if (definition == null) {
				throw new IllegalArgumentException(
						"MCP server '" + name + "' not found in catalog. Available: " + this.servers.keySet());
			}
			resolved.put(name, definition);
		}
		return Map.copyOf(resolved);
	}

	@Override
	public boolean contains(String name) {
		return this.servers.containsKey(name);
	}

}
