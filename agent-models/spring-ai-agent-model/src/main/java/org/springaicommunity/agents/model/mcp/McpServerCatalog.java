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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A named catalog of {@link McpServerDefinition} entries. Used by the client layer to
 * resolve server names to portable definitions before passing them to the model layer.
 *
 * <p>
 * Catalogs can be built programmatically via {@link #builder()} or loaded from JSON files
 * via {@link #fromJson(Path)}.
 *
 * @author Spring AI Community
 * @since 0.10.0
 */
public interface McpServerCatalog {

	/**
	 * Returns all server definitions in the catalog.
	 * @return unmodifiable map of server name to definition
	 */
	Map<String, McpServerDefinition> getAll();

	/**
	 * Resolves the given server names to their definitions.
	 * @param names the server names to resolve
	 * @return unmodifiable map of resolved name to definition
	 * @throws IllegalArgumentException if any name is not found in the catalog
	 */
	Map<String, McpServerDefinition> resolve(Collection<String> names);

	/**
	 * Checks whether a server name exists in the catalog.
	 * @param name the server name to check
	 * @return true if the catalog contains the name
	 */
	boolean contains(String name);

	/**
	 * Creates a catalog from an existing map of definitions.
	 * @param servers the server definitions
	 * @return an immutable catalog
	 */
	static McpServerCatalog of(Map<String, McpServerDefinition> servers) {
		return new DefaultMcpServerCatalog(servers);
	}

	/**
	 * Creates a new catalog builder.
	 * @return a new builder
	 */
	static Builder builder() {
		return new Builder();
	}

	/**
	 * Loads a catalog from a JSON file or directory of JSON files.
	 *
	 * <p>
	 * If the path is a regular file, it is parsed as a single JSON catalog. If the path
	 * is a directory, all {@code *.json} files in the directory are parsed and merged.
	 *
	 * <p>
	 * The expected JSON format follows the Claude CLI convention:
	 *
	 * <pre>{@code
	 * {
	 *   "mcpServers": {
	 *     "brave-search": {
	 *       "type": "stdio",
	 *       "command": "npx",
	 *       "args": ["-y", "@modelcontextprotocol/server-brave-search"],
	 *       "env": { "BRAVE_API_KEY": "${BRAVE_API_KEY}" }
	 *     },
	 *     "weather": {
	 *       "type": "sse",
	 *       "url": "http://localhost:8080/sse"
	 *     }
	 *   }
	 * }
	 * }</pre>
	 *
	 * Environment variable placeholders ({@code ${VAR_NAME}}) are resolved from
	 * {@link System#getenv()} at load time. Unresolvable placeholders are replaced with
	 * empty strings.
	 * @param path a JSON file or directory containing JSON files
	 * @return an immutable catalog
	 * @throws UncheckedIOException if reading fails
	 * @throws IllegalArgumentException if the path does not exist or the JSON is invalid
	 */
	static McpServerCatalog fromJson(Path path) {
		return McpServerCatalogLoader.load(path);
	}

	/**
	 * Mutable builder for constructing a catalog programmatically.
	 */
	class Builder {

		private final Map<String, McpServerDefinition> servers = new LinkedHashMap<>();

		Builder() {
		}

		/**
		 * Adds a named server definition to the catalog.
		 * @param name the server name
		 * @param definition the server definition
		 * @return this builder for chaining
		 */
		public Builder add(String name, McpServerDefinition definition) {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("name must not be null or blank");
			}
			if (definition == null) {
				throw new IllegalArgumentException("definition must not be null");
			}
			this.servers.put(name, definition);
			return this;
		}

		/**
		 * Builds an immutable catalog from the accumulated definitions.
		 * @return the catalog
		 */
		public McpServerCatalog build() {
			return new DefaultMcpServerCatalog(this.servers);
		}

	}

}
