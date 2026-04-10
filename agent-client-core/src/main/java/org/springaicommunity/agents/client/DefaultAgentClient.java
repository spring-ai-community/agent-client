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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springaicommunity.agents.client.advisor.AgentModelCallAdvisor;
import org.springaicommunity.agents.client.advisor.DefaultAgentCallAdvisorChain;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisor;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentOptionsUtils;
import org.springaicommunity.agents.model.mcp.McpServerCatalog;
import org.springaicommunity.agents.model.mcp.McpServerDefinition;

/**
 * Default implementation of AgentClient following Spring AI patterns.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class DefaultAgentClient implements AgentClient {

	private final AgentModel agentModel;

	private final AgentOptions defaultOptions;

	private final List<AgentCallAdvisor> defaultAdvisors;

	private final McpServerCatalog mcpServerCatalog;

	private final List<String> defaultMcpServerNames;

	/**
	 * Create a new DefaultAgentClient with the given agent model.
	 * @param agentModel the underlying agent model
	 */
	public DefaultAgentClient(AgentModel agentModel) {
		this(agentModel, new DefaultAgentOptions(), new ArrayList<>(), null, List.of());
	}

	/**
	 * Create a new DefaultAgentClient with the given agent model and default options.
	 * @param agentModel the underlying agent model
	 * @param defaultOptions default options for all requests
	 */
	public DefaultAgentClient(AgentModel agentModel, AgentOptions defaultOptions) {
		this(agentModel, defaultOptions, new ArrayList<>(), null, List.of());
	}

	/**
	 * Create a new DefaultAgentClient with the given agent model, default options, and
	 * advisors.
	 * @param agentModel the underlying agent model
	 * @param defaultOptions default options for all requests
	 * @param defaultAdvisors default advisors for all requests
	 */
	public DefaultAgentClient(AgentModel agentModel, AgentOptions defaultOptions,
			List<AgentCallAdvisor> defaultAdvisors) {
		this(agentModel, defaultOptions, defaultAdvisors, null, List.of());
	}

	/**
	 * Create a new DefaultAgentClient with the given agent model, default options,
	 * advisors, and MCP catalog.
	 * @param agentModel the underlying agent model
	 * @param defaultOptions default options for all requests (may be null, defaults to
	 * DefaultAgentOptions)
	 * @param defaultAdvisors default advisors for all requests
	 * @param mcpServerCatalog the MCP server catalog, may be null
	 * @param defaultMcpServerNames default MCP server names for all requests
	 */
	public DefaultAgentClient(AgentModel agentModel, AgentOptions defaultOptions,
			List<AgentCallAdvisor> defaultAdvisors, McpServerCatalog mcpServerCatalog,
			List<String> defaultMcpServerNames) {
		this.agentModel = Objects.requireNonNull(agentModel, "AgentModel cannot be null");
		this.defaultOptions = defaultOptions != null ? defaultOptions : new DefaultAgentOptions();
		this.defaultAdvisors = defaultAdvisors != null ? new ArrayList<>(defaultAdvisors) : new ArrayList<>();
		this.mcpServerCatalog = mcpServerCatalog;
		this.defaultMcpServerNames = defaultMcpServerNames != null ? List.copyOf(defaultMcpServerNames) : List.of();
	}

	@Override
	public AgentClientRequestSpec goal() {
		return new DefaultAgentClientRequestSpec(null);
	}

	@Override
	public AgentClientRequestSpec goal(String goal) {
		return goal(new Goal(goal));
	}

	@Override
	public AgentClientRequestSpec goal(Goal goal) {
		return new DefaultAgentClientRequestSpec(goal);
	}

	@Override
	public AgentClientResponse run(String goalText) {
		return goal(goalText).run();
	}

	@Override
	public AgentClientResponse run(String goalText, AgentOptions agentOptions) {
		Goal goal = new Goal(goalText, null, agentOptions);
		return goal(goal).run();
	}

	@Override
	public AgentClient.Builder mutate() {
		return new DefaultAgentClientBuilder(this.agentModel).defaultOptions(this.defaultOptions)
			.defaultAdvisors(this.defaultAdvisors)
			.mcpServerCatalog(this.mcpServerCatalog)
			.defaultMcpServers(this.defaultMcpServerNames);
	}

	/**
	 * Default implementation of AgentClientRequestSpec.
	 */
	private class DefaultAgentClientRequestSpec implements AgentClientRequestSpec {

		private Goal goal;

		private Path workingDirectory;

		private List<AgentCallAdvisor> requestAdvisors = new ArrayList<>();

		private List<String> mcpServerNames = new ArrayList<>();

		private AgentOptions options;

		public DefaultAgentClientRequestSpec(Goal goal) {
			this.goal = goal; // Can be null for goal() method
			this.workingDirectory = goal != null ? goal.getWorkingDirectory() : null;
			this.options = goal != null ? goal.getOptions() : null;
		}

		@Override
		public AgentClientRequestSpec goal(String goalContent) {
			this.goal = new Goal(goalContent);
			return this;
		}

		@Override
		public AgentClientRequestSpec workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
			return this;
		}

		@Override
		public AgentClientRequestSpec advisors(AgentCallAdvisor... advisors) {
			this.requestAdvisors.addAll(Arrays.asList(advisors));
			return this;
		}

		@Override
		public AgentClientRequestSpec advisors(List<AgentCallAdvisor> advisors) {
			this.requestAdvisors.addAll(advisors);
			return this;
		}

		@Override
		public AgentClientRequestSpec mcpServers(String... serverNames) {
			if (serverNames != null) {
				this.mcpServerNames.addAll(Arrays.asList(serverNames));
			}
			return this;
		}

		@Override
		public AgentClientRequestSpec mcpServers(List<String> serverNames) {
			if (serverNames != null) {
				this.mcpServerNames.addAll(serverNames);
			}
			return this;
		}

		@Override
		public AgentClientRequestSpec options(AgentOptions options) {
			this.options = options;
			return this;
		}

		@Override
		public AgentClientResponse run() {
			// Ensure we have a goal before proceeding
			if (this.goal == null) {
				throw new IllegalStateException(
						"Goal must be set before running. Use goal(String) or goal(Goal) first.");
			}

			// Determine effective working directory
			Path effectiveWorkingDirectory = determineWorkingDirectory();

			// Determine effective options with priority: request > goal > default
			AgentOptions effectiveOptions = determineEffectiveOptions();

			// Resolve MCP server names to definitions via the catalog
			effectiveOptions = resolveMcpServers(effectiveOptions);

			// Create client-layer request
			AgentClientRequest request = new AgentClientRequest(this.goal, effectiveWorkingDirectory, effectiveOptions,
					new HashMap<>());

			// Build advisor chain with terminal advisor
			List<AgentCallAdvisor> advisors = new ArrayList<>(DefaultAgentClient.this.defaultAdvisors);
			advisors.addAll(this.requestAdvisors);
			advisors.add(new AgentModelCallAdvisor(DefaultAgentClient.this.agentModel));

			var chain = DefaultAgentCallAdvisorChain.builder().pushAll(advisors).build();

			// Execute through advisor chain
			return chain.nextCall(request);
		}

		private Path determineWorkingDirectory() {
			// Use working directory priority: explicit > goal > builder default > current
			// directory
			if (this.workingDirectory != null) {
				// Explicit working directory was set on this request
				return this.workingDirectory;
			}
			else if (this.goal.getWorkingDirectory() != null) {
				// Use working directory from goal
				return this.goal.getWorkingDirectory();
			}
			else if (DefaultAgentClient.this.defaultOptions.getWorkingDirectory() != null) {
				// Use default working directory from builder
				return Path.of(DefaultAgentClient.this.defaultOptions.getWorkingDirectory());
			}
			else {
				// Fall back to current working directory
				return Path.of(System.getProperty("user.dir"));
			}
		}

		private AgentOptions determineEffectiveOptions() {
			if (this.options != null) {
				// Request-level options take highest priority
				return AgentOptionsUtils.merge(this.options, DefaultAgentClient.this.defaultOptions,
						this.options.getClass());
			}
			else if (this.goal.getOptions() != null) {
				AgentOptions goalOptions = this.goal.getOptions();
				// Goal options take second priority
				return AgentOptionsUtils.merge(goalOptions, DefaultAgentClient.this.defaultOptions,
						goalOptions.getClass());
			}
			else {
				// Fall back to default options
				return DefaultAgentClient.this.defaultOptions;
			}
		}

		private AgentOptions resolveMcpServers(AgentOptions options) {
			// Union builder defaults + request spec names, deduplicated
			Set<String> allNames = new LinkedHashSet<>(DefaultAgentClient.this.defaultMcpServerNames);
			allNames.addAll(this.mcpServerNames);

			if (allNames.isEmpty()) {
				return options;
			}

			// Fail-fast: names present but no catalog
			if (DefaultAgentClient.this.mcpServerCatalog == null) {
				throw new IllegalStateException("MCP server names were requested " + allNames
						+ " but no McpServerCatalog is configured on the AgentClient builder. "
						+ "Use AgentClient.builder(model).mcpServerCatalog(catalog) to set one.");
			}

			// Resolve names to definitions
			Map<String, McpServerDefinition> resolved = DefaultAgentClient.this.mcpServerCatalog.resolve(allNames);

			// Build new options carrying the resolved definitions
			// Use AgentOptionsUtils.merge() to preserve the original options type
			DefaultAgentOptions mcpOptions = DefaultAgentOptions.builder().mcpServerDefinitions(resolved).build();
			return AgentOptionsUtils.merge(mcpOptions, options, options.getClass());
		}

	}

}