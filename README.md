# Agent Client

> **Note**: This project was renamed from `spring-ai-agents` to `agent-client` in version 0.9.0.
> Update your git remote: `git remote set-url origin git@github.com:spring-ai-community/agent-client.git`
>
> Additionally, shared infrastructure has been extracted to separate repositories:
> - **[agent-sandbox](https://github.com/spring-ai-community/agent-sandbox)** - Sandbox execution infrastructure (Docker/Local)
> - **[agent-judge](https://github.com/spring-ai-community/agent-judge)** - Agent evaluation and judging framework

> **⚠️ Important Notice for Fork Owners**: We cleaned up git history on Sept 28, 2025. If you have a fork, please see [Issue #2](https://github.com/spring-ai-community/agent-client/issues/2) for update instructions.

## 📊 SpringOne 2025 Presentation
This project was featured in a talk at SpringOne 2025 by Mark Pollack. View the presentation: [springone-2025-presentation.html](springone-2025-presentation.html)

[![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity.agents/agent-starter-claude.svg)](https://search.maven.org/search?q=g:org.springaicommunity.agents)

📖 **[Documentation](https://spring-ai-community.github.io/agent-client/)** | [Getting Started](https://spring-ai-community.github.io/agent-client/getting-started.html) | [API Reference](https://spring-ai-community.github.io/agent-client/api/agentclient.html) | [Agent Bench](https://github.com/spring-ai-community/agent-bench)

> **Note**: This project is currently in development. The repository structure and APIs are subject to change.

Agent Client provides autonomous CLI agent integrations for the Spring AI ecosystem. This project brings Claude Code, Gemini CLI, and SWE-bench agents to Spring applications as first-class citizens with Spring Boot auto-configuration support and secure sandbox isolation.

## Overview

Transform autonomous CLI agents into pluggable Spring components:
- **Claude Code CLI** - Production-ready code assistance and autonomous development tasks
- **Gemini CLI** - Google's Gemini models through command-line interface
- **Amp CLI** - Sourcegraph's AI coding assistant
- **Codex CLI** - OpenAI's code generation agent
- **Amazon Q CLI** - AWS developer assistant
- **Secure Sandbox Execution** - Docker container isolation with local fallback

## Quick Start

### Try with JBang (Zero Setup Required)

The fastest way to try Agent Client - no cloning, no building, just run:

```bash
# One-time setup: Add the catalog
jbang catalog add --name=springai https://raw.githubusercontent.com/spring-ai-community/agent-client/main/jbang-catalog.json

# Static content example
jbang agents@springai hello-world \
  path=greeting.txt \
  content="Hello Agent Client!"

# AI-powered examples (requires API keys)
export ANTHROPIC_API_KEY="your-key-here"

jbang agents@springai hello-world-agent-ai \
  path=ai-greeting.txt \
  content="a creative message about AI agents" \
  provider=claude

jbang agents@springai hello-world-agent-ai \
  path=ai-future.txt \
  content="a vision of the future of AI" \
  provider=gemini
```

> **Note**: Gemini CLI can only write files within your current project directory due to workspace restrictions. Use relative paths like `myfile.txt` instead of absolute paths like `/tmp/myfile.txt`.

### Maven Dependencies

```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>agent-starter-claude</artifactId>
    <version>0.10.0</version>
</dependency>
```

### Basic Usage

```java
@Autowired
private AgentClient agentClient;

// Simple goal execution
String result = agentClient.run("Fix the failing test in UserServiceTest");

// Advanced goal configuration
AgentClientResponse response = agentClient
    .goal("Generate comprehensive API documentation")
    .workingDirectory(projectRoot)
    .run();
```

## Code Coverage Agent

An autonomous agent that increased test coverage from **0% to 71.4%** on Spring's [gs-rest-service](https://spring.io/guides/gs/rest-service) tutorial.

**Key Finding**: Both Claude and Gemini achieved the same coverage percentage, but **only Claude followed all Spring WebMVC best practices** (@WebMvcTest, jsonPath(), BDD naming).

> **Model quality matters**: Same coverage, different code quality. Claude generated production-ready tests while Gemini used slower patterns (@SpringBootTest).

📖 **[Read the complete analysis with test code examples →](https://spring-ai-community.github.io/agent-client/getting-started/code-coverage-agent.html)**

### Agent Advisors

Agent Client implements the same advisor pattern as Spring AI's ChatClient, providing powerful interception points for execution flows:

```java
// Create an advisor to inject workspace context
public class WorkspaceContextAdvisor implements AgentCallAdvisor {

    @Override
    public AgentClientResponse adviseCall(AgentClientRequest request,
                                          AgentCallAdvisorChain chain) {
        // Inject context before execution
        String workspaceInfo = analyzeWorkspace(request.workingDirectory());
        request.context().put("workspace_info", workspaceInfo);

        // Execute agent
        AgentClientResponse response = chain.nextCall(request);

        // Add post-execution metrics
        response.context().put("files_modified", countModifiedFiles());
        return response;
    }

    @Override
    public String getName() {
        return "WorkspaceContext";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}

// Register advisors with AgentClient builder
AgentClient client = AgentClient.builder(agentModel)
    .defaultAdvisor(new WorkspaceContextAdvisor())
    .defaultAdvisor(new TestExecutionAdvisor())
    .build();
```

**Common Advisor Use Cases**:
- **Context Engineering**: Git cloning, dependency sync, workspace preparation
- **Evaluation (Judges)**: Post-execution test running, file verification, quality checks
- **Security**: Goal validation, dangerous operation blocking
- **Observability**: Metrics collection, execution logging, performance tracking

See the [Agent Advisors documentation](https://spring-ai-community.github.io/agent-client/api/advisors.html) for complete details.

### Configuration

```yaml
spring:
  ai:
    agent:
      provider: claude-code  # or gemini, swebench
      max-steps: 6
      timeout: 300s
    agents:
      sandbox:
        docker:
          enabled: true
          image-tag: ghcr.io/spring-ai-community/agents-runtime:latest
        local:
          working-directory: /tmp
    claude-code:
      model: claude-sonnet-4-20250514
      bin: /usr/local/bin/claude
      yolo: true
    gemini:
      model: gemini-2.0-flash
      bin: /usr/local/bin/gemini
```

## Architecture

### Mono-repo Structure

```
agent-client/
├── advisors/                        # Agent advisors
├── agent-client-core/               # Agent client fluent API
├── agent-launcher/                  # Agent launcher framework
├── agent-models/                    # Agent implementations
│   ├── agent-amazon-q/              # Amazon Q agent
│   ├── agent-amp/                   # Amp agent
│   ├── agent-claude/                # Claude Code agent
│   ├── agent-codex/                 # Codex agent
│   ├── agent-gemini/                # Gemini CLI agent
│   ├── agent-judge-bridge/          # Judge Bridge agent
│   ├── agent-model/                 # Core abstractions
│   ├── agent-swe/                   # SWE agent
│   └── agent-tck/                   # Technology Compatibility Kit
├── agent-starters/                  # Auto-configuration starters
├── agents/                          # JBang-compatible agents
│   ├── code-coverage-agent/         # Test coverage improvement agent
│   ├── hello-world-agent/           # Static file creation
│   └── hello-world-agent-ai/        # AI-powered file creation
├── provider-sdks/                   # CLI/SDK integrations
│   ├── amazon-q-cli-sdk/            # Amazon Q CLI client
│   ├── amp-cli-sdk/                 # Amp CLI client
│   ├── codex-cli-sdk/               # Codex CLI client
│   ├── gemini-cli-sdk/              # Gemini CLI client
│   └── swe-agent-sdk/               # SWE-bench agent SDK
└── samples/                         # Example applications
```

### Key Components

- **`AgentModel`** - Core interface for autonomous agents
- **`AgentClient`** - ChatClient-inspired fluent API
- **`Sandbox`** - Secure execution environment (Docker/Local)
- **Provider SDKs** - CLI integrations with resilience features
- **Spring Boot Starter** - Auto-configuration and metrics

## Modules

| Module | Description                                         | Maven Coordinates                                      |
|--------|-----------------------------------------------------|--------------------------------------------------------|
| Core Abstractions | `AgentModel`, `AgentTaskRequest`, `AgentCallResult` | `org.springaicommunity.agents:agent-model`             |
| Amazon Q CLI SDK | Amazon Q command-line interface client              | `org.springaicommunity.agents:amazon-q-cli-sdk`        |
| Amp CLI SDK | Amp command-line interface client                   | `org.springaicommunity.agents:amp-cli-sdk`             |
| Codex CLI SDK | Codex command-line interface client                 | `org.springaicommunity.agents:codex-cli-sdk`           |
| Gemini CLI SDK | Gemini command-line interface client                | `org.springaicommunity.agents:gemini-cli-sdk`          |
| SWE Agent SDK | SWE-bench agent SDK                                 | `org.springaicommunity.agents:swe-agent-sdk`           |
| Amazon Q Agent | Adapter for Amazon Q                                | `org.springaicommunity.agents:agent-amazon-q`          |
| Amp Agent | Adapter for Amp                                     | `org.springaicommunity.agents:agent-amp`               |
| Claude Code Agent | Adapter for Claude Code                             | `org.springaicommunity.agents:agent-claude`            |
| Codex Agent | Adapter for Codex                                   | `org.springaicommunity.agents:agent-codex`             |
| Gemini Agent | Adapter for Gemini CLI                              | `org.springaicommunity.agents:agent-gemini`            |
| Judge Bridge Agent | Adapter for Judge Bridge                            | `org.springaicommunity.agents:agent-judge-bridge`      |
| SWE Agent | Software engineering benchmarking agent             | `org.springaicommunity.agents:agent-swe`               |
| Agent TCK | Technology Compatibility Kit for Agents             | `org.springaicommunity.agents:agent-tck`               |
| Agent Client | Unified fluent API                                  | `org.springaicommunity.agents:agent-client-core`       |
| Agents Core | Agent launcher framework                            | `org.springaicommunity.agents:agent-launcher`          |
| Hello World Agent | Static file creation agent                          | `org.springaicommunity.agents:hello-world-agent`       |
| Hello World AI Agent | AI-powered file creation agent                      | `org.springaicommunity.agents:hello-world-agent-ai`    |
| Code Coverage Agent | Test coverage improvement agent                     | `org.springaicommunity.agents:code-coverage-agent`     |
| Agent Starters | Auto-configuration for providers                    | `org.springaicommunity.agents:agent-starter-*`         |

## Features

- **Production Ready**: Circuit breakers, retries, timeouts, and comprehensive error handling
- **Secure by Default**: Docker container isolation with automatic fallback to local execution
- **Spring Boot Integration**: Auto-configuration, externalized configuration, and actuator support
- **Observability**: Micrometer metrics and structured logging
- **Type Safe**: Full Java type safety with comprehensive JavaDoc
- **Flexible**: Provider-agnostic `AgentClient` with pluggable implementations
- **Advisor Pattern**: Powerful interception points for context engineering, validation, and evaluation

## Examples

See the [`samples/`](samples/) directory for complete examples:
- [`hello-world/`](samples/hello-world/) - Simple Spring Boot application demonstrating AgentClient basics
- [`context-engineering/`](samples/context-engineering/) - Advanced context engineering with VendirContextAdvisor

## Documentation

- [Getting Started Guide](docs/quickstart.md)
- [Architecture Overview](docs/architecture.md)
- [API Reference](docs/api-reference.md)

## Building and Testing

### Prerequisites

- Java 17 or higher
- Maven 3.8+ or Gradle 7+
- Claude CLI (for Claude Code agent)
- Gemini CLI (for Gemini agent)
- Docker (recommended for secure sandbox execution)

### Build Commands

#### Basic Build
```bash
# Compile all modules
./mvnw clean compile

# Build and run unit tests only
./mvnw clean test

# Full build with unit tests, no integration tests
./mvnw clean install
```

#### Integration Tests
```bash
# Run integration tests (requires live APIs and Docker)
./mvnw clean verify -Pfailsafe

# Run all tests including integration tests
./mvnw clean verify

# Run specific integration test (Failsafe - proper way)
./mvnw failsafe:integration-test -pl agent-models/agent-claude -Dit.test=ClaudeCodeLocalSandboxIT

# Run Docker infrastructure tests (Failsafe - proper way)
./mvnw failsafe:integration-test -pl agent-models/agent-claude -Dit.test=ClaudeDockerInfraIT

# Alternative: Surefire can run IT tests when explicitly specified
./mvnw test -pl agent-models/agent-claude -Dtest=ClaudeDockerInfraIT
```

#### Authentication for Tests
```bash
# Option 1: Claude CLI session authentication (recommended)
claude auth login
./mvnw test

# Option 2: Environment variables (may conflict with session)
export ANTHROPIC_API_KEY="your-key"
export GEMINI_API_KEY="your-key"
./mvnw test
```

### Test Categories

- **Unit Tests** (`*Test.java`): Fast, mocked dependencies
- **Integration Tests** (`*IT.java`): Real CLI execution, requires authentication
- **Docker Tests** (`*DockerInfraIT.java`): Container infrastructure testing

### Performance Expectations

- **Unit tests**: < 10 seconds total
- **Integration tests**: 20-60 seconds per test (depends on complexity)
- **Docker tests**: 10-15 seconds per test (container overhead)

## Contributing

This project follows the [Spring AI Community Guidelines](https://github.com/spring-ai-community). 

## License

Agent Client is Open Source software released under the [Apache 2.0 license](LICENSE).

## Status

**Current Status**: **0.10.0 Released** on Maven Central

This project is actively being developed. APIs may evolve between minor releases.

## Migration Path

This project is designed to eventually integrate with the main [Spring AI](https://github.com/spring-projects/spring-ai) project. The package structure and module organization are designed to make this transition seamless when ready.