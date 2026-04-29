# Agent Client

[![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity.agents/agent-starter-claude.svg)](https://search.maven.org/search?q=g:org.springaicommunity.agents)

**What ChatClient did for completion endpoints, AgentClient does for agent CLIs.**

Agent Client provides a unified Java API for autonomous CLI agents — Claude Code, Codex, Gemini, Amazon Q, and Amp — with Spring Boot auto-configuration support.

📖 **[Documentation](https://springaicommunity.mintlify.app/projects/incubating/agent-client)** | [Getting Started](https://springaicommunity.mintlify.app/agent-client/howto/getting-started) | [Reference](https://springaicommunity.mintlify.app/agent-client/reference/portable-options) | [Tutorial](https://springaicommunity.mintlify.app/agent-client/tutorial/index)

## Quick Start

Add the dependency for your provider:

```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>agent-claude</artifactId>
    <version>0.15.0</version>
</dependency>
```

Build a model, create a client, run a goal — no Spring Boot required:

```java
ClaudeAgentModel model = ClaudeAgentModel.builder()
    .defaultOptions(ClaudeAgentOptions.builder()
        .model("claude-sonnet-4-5")
        .yolo(true)
        .build())
    .build();

AgentClient client = AgentClient.create(model);
AgentClientResponse response = client.run("Create hello.txt with 'Hello from Agent Client!'");
```

### With Spring Boot

Use a starter for auto-configuration:

```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>agent-starter-claude</artifactId>
    <version>0.15.0</version>
</dependency>
```

```java
@Component
public class MyAgent implements CommandLineRunner {
    private final AgentClient.Builder agentClientBuilder;

    public MyAgent(AgentClient.Builder agentClientBuilder) {
        this.agentClientBuilder = agentClientBuilder;
    }

    @Override
    public void run(String... args) {
        AgentClient client = agentClientBuilder.build();
        AgentClientResponse response = client.run("Fix the failing test");
    }
}
```

## Supported Providers

| Provider | Starter | Status |
|----------|---------|--------|
| [Claude Code](https://docs.anthropic.com/en/docs/claude-code) | `agent-starter-claude` | Production |
| [Codex](https://github.com/openai/codex) | `agent-starter-codex` | Production |
| [Gemini CLI](https://github.com/google-gemini/gemini-cli) | `agent-starter-gemini` | Production |
| [Amazon Q](https://aws.amazon.com/q/developer/) | `agent-starter-amazon-q` | Beta |
| [Amp](https://ampcode.com/) | `agent-starter-amp` | Beta |

## Multi-Provider Support

Switch providers without changing code — use Maven profiles or swap the starter:

```java
// This code works with ANY provider
AgentClient client = AgentClient.create(model);
AgentClientResponse response = client.run("Create hello.txt");
```

See [Switching Providers](https://springaicommunity.mintlify.app/agent-client/howto/switching-providers) for the Maven profile pattern.

## Configuration

```yaml
spring:
  ai:
    agents:
      mode: loose  # or strict
      claude-code:
        model: claude-sonnet-4-5
        timeout: PT5M
        yolo: true
      codex:
        model: gpt-5-codex
        full-auto: true
      gemini:
        model: gemini-2.5-flash
        yolo: true
```

See the [Reference](https://springaicommunity.mintlify.app/agent-client/reference/portable-options) pages for all configuration options.

## Architecture

```
agent-client/
├── agent-client-core/               # AgentClient fluent API
├── agent-models/                    # Provider adapters
│   ├── agent-model/                 # Core abstractions (AgentModel, AgentOptions)
│   ├── agent-tck/                   # Provider parity test kit
│   ├── agent-claude/                # Claude Code adapter
│   ├── agent-codex/                 # Codex adapter
│   ├── agent-gemini/                # Gemini CLI adapter
│   ├── agent-amazon-q/              # Amazon Q adapter
│   └── agent-amp/                   # Amp adapter
├── provider-sdks/                   # CLI client libraries
├── agent-starters/                  # Spring Boot auto-configuration
└── agents/                          # JBang-compatible agents
```

### Two-Layer Design

- **`AgentClient`** — High-level fluent API (like `ChatClient`)
- **`AgentModel`** — Low-level provider interface (like `ChatModel`)

Provider selection happens at construction time. Everything after `AgentClient.create(model)` is portable.

## Documentation

| Type | Link |
|------|------|
| Getting Started | [Quick start guide](https://springaicommunity.mintlify.app/agent-client/howto/getting-started) |
| Tutorial | [Step-by-step lessons](https://springaicommunity.mintlify.app/agent-client/tutorial/index) |
| Reference | [Configuration options](https://springaicommunity.mintlify.app/agent-client/reference/portable-options) |
| Provider Reference | [Claude](https://springaicommunity.mintlify.app/agent-client/reference/claude-reference) · [Codex](https://springaicommunity.mintlify.app/agent-client/reference/codex-reference) · [Gemini](https://springaicommunity.mintlify.app/agent-client/reference/gemini-reference) |
| Defaults Philosophy | [LOOSE vs STRICT modes](https://springaicommunity.mintlify.app/agent-client/explanation/defaults-philosophy) |
| Sessions | [Multi-turn conversations](https://springaicommunity.mintlify.app/agent-client/reference/sessions) |

## Building

```bash
./mvnw clean compile          # Compile
./mvnw clean test             # Unit tests
./mvnw clean verify -Pfailsafe  # Integration tests (requires CLIs + API keys)
```

## License

Apache 2.0 — see [LICENSE](LICENSE).
