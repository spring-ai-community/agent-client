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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@link McpServerCatalog} from JSON files. Package-private — accessed via
 * {@link McpServerCatalog#fromJson(Path)}.
 *
 * @author Spring AI Community
 * @since 0.10.0
 */
final class McpServerCatalogLoader {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

	private McpServerCatalogLoader() {
	}

	static McpServerCatalog load(Path path) {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Path does not exist: " + path);
		}
		Map<String, McpServerDefinition> servers = new LinkedHashMap<>();
		if (Files.isDirectory(path)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.json")) {
				for (Path file : stream) {
					servers.putAll(parseFile(file));
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to scan directory: " + path, e);
			}
		}
		else {
			servers.putAll(parseFile(path));
		}
		return new DefaultMcpServerCatalog(servers);
	}

	private static Map<String, McpServerDefinition> parseFile(Path file) {
		try {
			JsonNode root = MAPPER.readTree(Files.readString(file));
			JsonNode serversNode = root.path("mcpServers");
			if (serversNode.isMissingNode()) {
				return Map.of();
			}
			Map<String, McpServerDefinition> result = new LinkedHashMap<>();
			Iterator<Map.Entry<String, JsonNode>> fields = serversNode.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				result.put(entry.getKey(), parseDefinition(entry.getValue(), entry.getKey(), file));
			}
			return result;
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to parse JSON file: " + file, e);
		}
	}

	private static McpServerDefinition parseDefinition(JsonNode node, String name, Path file) {
		String type = resolveEnvVars(textOrNull(node, "type"));
		if (type == null) {
			type = "stdio";
		}
		return switch (type.toLowerCase()) {
			case "stdio" -> parseStdio(node);
			case "sse" -> parseSse(node);
			case "http" -> parseHttp(node);
			default -> throw new IllegalArgumentException(
					"Unknown MCP server type '" + type + "' for server '" + name + "' in " + file);
		};
	}

	private static McpServerDefinition.StdioDefinition parseStdio(JsonNode node) {
		String command = resolveEnvVars(textOrNull(node, "command"));
		List<String> args = parseStringList(node.path("args"));
		Map<String, String> env = parseStringMap(node.path("env"));
		return new McpServerDefinition.StdioDefinition(command, args, env);
	}

	private static McpServerDefinition.SseDefinition parseSse(JsonNode node) {
		String url = resolveEnvVars(textOrNull(node, "url"));
		Map<String, String> headers = parseStringMap(node.path("headers"));
		return new McpServerDefinition.SseDefinition(url, headers);
	}

	private static McpServerDefinition.HttpDefinition parseHttp(JsonNode node) {
		String url = resolveEnvVars(textOrNull(node, "url"));
		Map<String, String> headers = parseStringMap(node.path("headers"));
		return new McpServerDefinition.HttpDefinition(url, headers);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode child = node.path(field);
		return child.isMissingNode() || child.isNull() ? null : child.asText();
	}

	private static List<String> parseStringList(JsonNode node) {
		if (node.isMissingNode() || !node.isArray()) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (JsonNode element : node) {
			result.add(resolveEnvVars(element.asText()));
		}
		return result;
	}

	private static Map<String, String> parseStringMap(JsonNode node) {
		if (node.isMissingNode() || !node.isObject()) {
			return Map.of();
		}
		Map<String, String> result = new LinkedHashMap<>();
		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			result.put(entry.getKey(), resolveEnvVars(entry.getValue().asText()));
		}
		return result;
	}

	static String resolveEnvVars(String value) {
		if (value == null) {
			return null;
		}
		Matcher matcher = ENV_VAR_PATTERN.matcher(value);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			String envName = matcher.group(1);
			String envValue = System.getenv(envName);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(envValue != null ? envValue : ""));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

}
