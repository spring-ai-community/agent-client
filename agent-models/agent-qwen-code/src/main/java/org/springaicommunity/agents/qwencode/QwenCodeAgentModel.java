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

package org.springaicommunity.agents.qwencode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.qwen.code.cli.QwenCodeCli;
import com.alibaba.qwen.code.cli.protocol.data.PermissionMode;
import com.alibaba.qwen.code.cli.transport.TransportOptions;
import com.alibaba.qwen.code.cli.utils.Timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.StructuredOutputPromptHelper;

/**
 * Implementation of {@link AgentModel} for Qwen Code CLI-based agents.
 *
 * <p>
 * This adapter bridges Spring AI's agent abstraction with the Qwen Code CLI via the
 * qwencode-sdk. It uses {@code QwenCodeCli.simpleQuery()} for blocking execution.
 * </p>
 *
 * @author Spring AI Community
 * @since 0.12.0
 */
public class QwenCodeAgentModel implements AgentModel {

	private static final Logger logger = LoggerFactory.getLogger(QwenCodeAgentModel.class);

	private final QwenCodeAgentOptions defaultOptions;

	private final QwenCodeQueryFunction queryFunction;

	/**
	 * Create a new QwenCodeAgentModel with the given options.
	 * @param defaultOptions default execution options
	 */
	public QwenCodeAgentModel(QwenCodeAgentOptions defaultOptions) {
		this(defaultOptions, QwenCodeCli::simpleQuery);
	}

	/**
	 * Package-private constructor for testability.
	 * @param defaultOptions default execution options
	 * @param queryFunction function wrapping the CLI query
	 */
	QwenCodeAgentModel(QwenCodeAgentOptions defaultOptions, QwenCodeQueryFunction queryFunction) {
		this.defaultOptions = defaultOptions != null ? defaultOptions : QwenCodeAgentOptions.builder().build();
		this.queryFunction = queryFunction;
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		String goal = request.goal();

		// Tier 3: embed schema in prompt when jsonSchema is present
		if (request.options() != null && request.options().getJsonSchema() != null) {
			goal = StructuredOutputPromptHelper.wrapGoalWithSchema(goal, request.options().getJsonSchema());
		}

		logger.info("Executing Qwen Code agent with goal: {}", goal);

		QwenCodeAgentOptions options = mergeOptions(request);
		TransportOptions transportOptions = toTransportOptions(options, request);

		try {
			long startTime = System.currentTimeMillis();
			List<String> results = queryFunction.query(goal, transportOptions);
			long duration = System.currentTimeMillis() - startTime;

			String output = String.join("\n", results);
			return toAgentResponse(output, options.getModel(), java.time.Duration.ofMillis(duration));
		}
		catch (Exception ex) {
			logger.error("Qwen Code execution failed: {}", ex.getMessage(), ex);
			return toErrorResponse(ex);
		}
	}

	private QwenCodeAgentOptions mergeOptions(AgentTaskRequest request) {
		QwenCodeAgentOptions.Builder builder = QwenCodeAgentOptions.builder()
			.model(defaultOptions.getModel())
			.timeout(defaultOptions.getTimeout())
			.yolo(defaultOptions.isYolo())
			.executablePath(defaultOptions.getExecutablePath());

		if (defaultOptions.getPermissionMode() != null) {
			builder.permissionMode(defaultOptions.getPermissionMode());
		}

		if (request.options() != null && request.options() instanceof QwenCodeAgentOptions requestOptions) {
			if (requestOptions.getModel() != null) {
				builder.model(requestOptions.getModel());
			}
			if (requestOptions.getTimeout() != null) {
				builder.timeout(requestOptions.getTimeout());
			}
			if (requestOptions.getPermissionMode() != null) {
				builder.permissionMode(requestOptions.getPermissionMode());
			}
			if (requestOptions.getExecutablePath() != null) {
				builder.executablePath(requestOptions.getExecutablePath());
			}
			builder.yolo(requestOptions.isYolo());
		}

		return builder.build();
	}

	private TransportOptions toTransportOptions(QwenCodeAgentOptions options, AgentTaskRequest request) {
		TransportOptions transportOptions = new TransportOptions();

		if (options.getModel() != null) {
			transportOptions.setModel(options.getModel());
		}

		if (options.getTimeout() != null) {
			long seconds = options.getTimeout().toSeconds();
			transportOptions.setTurnTimeout(new Timeout(seconds, TimeUnit.SECONDS));
		}

		if (options.isYolo()) {
			transportOptions.setPermissionMode(PermissionMode.YOLO);
		}
		else if (options.getPermissionMode() != null) {
			transportOptions.setPermissionMode(options.getPermissionMode());
		}

		if (options.getExecutablePath() != null) {
			transportOptions.setPathToQwenExecutable(options.getExecutablePath());
		}

		String workingDir = options.getWorkingDirectory();
		if (workingDir == null && request.workingDirectory() != null) {
			workingDir = request.workingDirectory().toString();
		}
		if (workingDir != null) {
			transportOptions.setCwd(workingDir);
		}

		if (options.getEnvironmentVariables() != null && !options.getEnvironmentVariables().isEmpty()) {
			transportOptions.setEnv(options.getEnvironmentVariables());
		}

		return transportOptions;
	}

	private AgentResponse toAgentResponse(String output, String model, java.time.Duration duration) {
		AgentGeneration generation = new AgentGeneration(output,
				new AgentGenerationMetadata("SUCCESS", Map.of("model", model != null ? model : "")));

		AgentResponseMetadata metadata = AgentResponseMetadata.builder()
			.model(model != null ? model : "qwen3-coder")
			.duration(duration)
			.build();

		return new AgentResponse(List.of(generation), metadata);
	}

	private AgentResponse toErrorResponse(Exception ex) {
		AgentGeneration generation = new AgentGeneration(ex.getMessage(),
				new AgentGenerationMetadata("ERROR", Map.of("exception", ex.getClass().getSimpleName())));

		AgentResponseMetadata metadata = AgentResponseMetadata.builder().model("qwen3-coder").build();

		return new AgentResponse(List.of(generation), metadata);
	}

}
