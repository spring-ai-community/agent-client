# Roadmap: MCP Tool Catalog

> **Created**: 2026-02-19T15:30-05:00
> **Last updated**: 2026-02-19T22:55-05:00
> **Design version**: 2.1

## Context

The AgentClient portable API has no way to specify MCP servers. MCP config exists only at the Claude-specific layer (`ClaudeAgentOptions.mcpServers`). Every CLI agent tool (Claude, Gemini, etc.) supports MCP but with different config formats. Users need a portable catalog of named MCP servers at the `AgentClient` level, with automatic translation to provider-specific formats. This also addresses community requests (issues #3, #8, #11).

## Overview

Three stages: portable types in agent-model, client-layer resolution and fluent API, then Claude provider translation. Each stage is independently testable. Start narrow — Claude only — and extend to Gemini afterward.

The central architectural principle: **AgentClient resolves, AgentModel translates.** This follows the ChatClient/ChatModel pattern — the client is the developer-facing configuration surface that assembles fully-resolved requests; the model is a connectivity detail that mechanically translates portable types to its native format. The model never resolves names, never holds a catalog, never does application-level work.

> **Before every commit**: Verify ALL exit criteria for the current step are met — especially the standard items (see [Step Exit Criteria Convention](#step-exit-criteria-convention)). Do NOT remove exit criteria to mark a step complete — fulfill them.

---

## Stage 1: Portable MCP Types

### Step 1.1: McpServerDefinition and McpServerCatalog

**Entry criteria**:
- [x] Read: `AgentOptions.java` — understand existing interface contract
- [x] Read: `ClaudeAgentMcpProperties.java` — naming pattern reference for MCP server types

**Work items**:
- [x] CREATE `McpServerDefinition` sealed interface in `org.springaicommunity.agents.model.mcp`
  - `StdioDefinition(String command, List<String> args, Map<String, String> env)`
  - `SseDefinition(String url, Map<String, String> headers)`
  - `HttpDefinition(String url, Map<String, String> headers)`
  - All records, all immutable (defensive copies in compact constructors)
- [x] CREATE `McpServerCatalog` interface in same package
  - `Map<String, McpServerDefinition> getAll()`
  - `Map<String, McpServerDefinition> resolve(Collection<String> names)` — throws if name missing
  - `boolean contains(String name)`
  - `static McpServerCatalog of(Map<String, McpServerDefinition> servers)`
  - `static Builder builder()` with `.add(name, definition)` and `.build()`
  - `static McpServerCatalog fromJson(Path file)` — load from a single JSON file
  - `static McpServerCatalog fromJson(Path directory)` — scan directory for `*.json` files, merge all
- [x] CREATE `DefaultMcpServerCatalog` — immutable map-backed implementation
- [x] IMPLEMENT JSON loading in `McpServerCatalogLoader` (package-private)
  - JSON format mirrors the structure from Claude's `mcp-servers-config.json`
  - Uses Jackson (transitive dependency) for parsing
  - Environment variable placeholders (`${VAR}`) resolved from `System.getenv()` at load time
- [x] ADD default method to `AgentOptions`:
  - `default Map<String, McpServerDefinition> getMcpServerDefinitions() { return Map.of(); }`
  - Javadoc: documents that the map contains *resolved* definitions placed by the client layer
- [x] WRITE unit tests: 13 definition tests + 19 catalog tests (32 total)
- [x] VERIFY: `./mvnw test -pl agent-models/spring-ai-agent-model` — 36 tests pass

**Exit criteria**:
- [x] Portable types compile with zero new external dependencies (Jackson is already transitive)
- [x] Catalog loadable from programmatic builder, `Map`, single JSON file, or directory scan
- [x] All tests pass: `./mvnw test -pl agent-models/spring-ai-agent-model`
- [x] `./mvnw spring-javaformat:apply`
- [x] Update `CLAUDE.md` with MCP types in package structure section
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 1.1: Add portable MCP server definitions and catalog` (0e0c105)

**Deliverables**: `McpServerDefinition`, `McpServerCatalog`, `DefaultMcpServerCatalog`, JSON file loading

**Files**:
- NEW: `agent-models/spring-ai-agent-model/src/main/java/org/springaicommunity/agents/model/mcp/McpServerDefinition.java`
- NEW: `agent-models/spring-ai-agent-model/src/main/java/org/springaicommunity/agents/model/mcp/McpServerCatalog.java`
- NEW: `agent-models/spring-ai-agent-model/src/main/java/org/springaicommunity/agents/model/mcp/DefaultMcpServerCatalog.java`
- MODIFY: `agent-models/spring-ai-agent-model/src/main/java/org/springaicommunity/agents/model/AgentOptions.java`

---

## Stage 2: Client-Layer Resolution and Fluent API

### Step 2.1: AgentClient Fluent API with Client-Side Resolution

**Entry criteria**:
- [x] Step 1.1 complete
- [x] Read: `AgentClient.java`, `DefaultAgentClient.java`, `DefaultAgentClientBuilder.java`
- [x] Read: `DefaultAgentOptions.java` — understand existing builder and `from()` method

**Work items**:
- [x] ADD to `AgentClient.AgentClientRequestSpec`:
  - `mcpServers(String... serverNames)` — accumulates names for resolution at `run()` time
  - `mcpServers(List<String> serverNames)`
- [x] ADD to `AgentClient.Builder`:
  - `mcpServerCatalog(McpServerCatalog catalog)` — the catalog lives here, nowhere else
  - `defaultMcpServers(String... serverNames)` — default server names applied to every request
  - `defaultMcpServers(List<String> serverNames)`
- [x] IMPLEMENT in `DefaultAgentClientBuilder`: store catalog and default MCP server names
- [x] IMPLEMENT client-side resolution in `DefaultAgentClient.DefaultAgentClientRequestSpec.run()`
- [x] ADD `mcpServerDefinitions` field to `DefaultAgentOptions`
  - Builder method: `mcpServerDefinitions(Map<String, McpServerDefinition>)`
  - UPDATE `DefaultAgentOptions.Builder.from()` to copy `getMcpServerDefinitions()`
  - ADD comment in `from()`: `// IMPORTANT: When adding fields to DefaultAgentOptions, update this method.`
- [x] WRITE unit tests: 14 tests covering all scenarios
- [x] VERIFY: `./mvnw test -pl spring-ai-agent-client` — 79 tests pass (1 pre-existing skip)

**Exit criteria**:
- [x] Fluent API compiles and tests pass
- [x] Resolution happens in client layer — model receives only resolved definitions
- [x] Fail-fast: clear errors for missing catalog or missing server names
- [x] All tests pass: `./mvnw test -pl spring-ai-agent-client`
- [x] `./mvnw spring-javaformat:apply`
- [x] Update `CLAUDE.md` with AgentClient MCP usage example
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 2.1: Add MCP server selection to AgentClient with client-side resolution` (232181e)

**Deliverables**: Working fluent API — `client.goal("task").mcpServers("brave-search", "weather").run()` — with client resolving names to definitions before calling the model.

**Files**:
- MODIFY: `spring-ai-agent-client/src/main/java/org/springaicommunity/agents/client/AgentClient.java`
- MODIFY: `spring-ai-agent-client/src/main/java/org/springaicommunity/agents/client/DefaultAgentClient.java`
- MODIFY: `spring-ai-agent-client/src/main/java/org/springaicommunity/agents/client/DefaultAgentClientBuilder.java`
- MODIFY: `spring-ai-agent-client/src/main/java/org/springaicommunity/agents/client/DefaultAgentOptions.java`

---

## Stage 3: Claude Provider Translation

### Step 3.1: Portable Definition Translation in ClaudeAgentModel

**Entry criteria**:
- [x] Step 2.1 complete
- [x] Read: `ClaudeAgentModel.java` — `buildCLIOptions()` and `getEffectiveOptions()` methods
- [x] Read: `McpServerConfig` sealed interface in claude-code-sdk (external dep)

**Work items**:
- [x] IMPLEMENT `toClaudeMcpServerConfig(McpServerDefinition)` method in `ClaudeAgentModel`
- [x] EXTEND `buildCLIOptions()` with dual-read pattern and merge logic
- [x] WRITE unit tests: 5 tests for all 3 translation types + edge cases
- [x] VERIFY: `./mvnw test -pl agent-models/spring-ai-claude-agent` — 75 tests pass

**Exit criteria**:
- [x] Model reads portable definitions from `AgentOptions` interface
- [x] Explicit `ClaudeAgentOptions.mcpServers` takes precedence over portable definitions with same name
- [x] Dual-read pattern documented with comment in `buildCLIOptions()`
- [x] All tests pass: `./mvnw test -pl agent-models/spring-ai-claude-agent`
- [x] `./mvnw spring-javaformat:apply`
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 3.1: Add portable MCP definition translation in ClaudeAgentModel` (5e0da78)

**Deliverables**: Complete end-to-end: catalog → AgentClient resolves → AgentModel translates → CLI `--mcp-config`

**Files**:
- MODIFY: `agent-models/spring-ai-claude-agent/src/main/java/org/springaicommunity/agents/claude/ClaudeAgentModel.java`

---

### Step 3.2: Full Build Verification

**Entry criteria**:
- [x] Step 3.1 complete

**Work items**:
- [x] VERIFY full build: `./mvnw clean test` — 30 modules, all pass
- [x] VERIFY no regressions in existing tests
- [x] UPDATE `CLAUDE.md` with MCP catalog architecture and usage examples
- [ ] UPDATE `plans/knowledge/` with MCP design decisions

**Exit criteria**:
- [x] Full build passes
- [x] `CLAUDE.md` updated with MCP documentation
- [x] `./mvnw spring-javaformat:apply`
- [x] Update `ROADMAP.md` checkboxes
- [ ] COMMIT: `Step 3.2: MCP catalog verification and documentation`

**Deliverables**: Verified, documented feature

---

## Stage 4: Integration Tests

### Step 4.1: Full-Pipeline Integration Test (No CLI)

**Entry criteria**:
- [x] Steps 1.1–3.1 complete

**Work items**:
- [x] CREATE `AgentClientMcpPipelineIT` in `spring-ai-agent-client/src/test/java/.../client/`
  - Tests the complete chain: JSON file on disk → `McpServerCatalog.fromJson()` → `AgentClient.builder().mcpServerCatalog()` → `.mcpServers()` → `.run()` → advisor chain → `AgentModelCallAdvisor` → `MockAgentModel` captures `AgentTaskRequest` with resolved definitions
  - Why IT not unit: exercises real file I/O, full advisor chain wiring, and complete catalog → client → model pipeline together (unit tests tested each layer in isolation)
  - No skip conditions — uses MockAgentModel, no external CLI
  - Tests:
    - JSON file → catalog → client resolves → model receives resolved `StdioDefinition` and `SseDefinition`
    - Directory scan (multiple JSON files) → merged catalog → client resolves correctly
    - Builder defaults + per-request servers unioned through full advisor chain
    - `${ENV_VAR}` substitution works end-to-end with real `System.getenv()`
    - Mutated client preserves catalog across full pipeline
- [x] VERIFY: `./mvnw test -pl spring-ai-agent-client -Dtest="AgentClientMcpPipelineIT"`

**Exit criteria**:
- [x] All pipeline tests pass: `./mvnw test -pl spring-ai-agent-client`
- [x] `./mvnw spring-javaformat:apply`
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 4.1: Add MCP catalog full-pipeline integration test` (9a7512a)

**Files**:
- NEW: `spring-ai-agent-client/src/test/java/org/springaicommunity/agents/client/AgentClientMcpPipelineIT.java`

---

### Step 4.2: Real Claude CLI Integration Test

**Entry criteria**:
- [x] Step 4.1 complete

**Work items**:
- [x] CREATE `ClaudeAgentMcpIT` in `agent-models/spring-ai-claude-agent/src/test/java/.../claude/`
  - Tests portable definitions flow through real `ClaudeAgentModel` and Claude CLI accepts MCP configuration
  - MCP server won't actually connect (no running brave-search process) but Claude CLI should run and produce a response
  - Skip conditions: `@Disabled` (same as `AgentClientIT`), `@EnabledIfEnvironmentVariable(ANTHROPIC_API_KEY)`, `assumeTrue(isClaudeCliAvailable())`
  - Tests:
    - `ClaudeAgentModel.call()` with portable MCP definitions → real CLI executes with `--mcp-config` → returns non-null response
    - Verify response text is present (Claude may mention MCP connection issues, that's fine)
  - Note: Uses `AgentTaskRequest` directly (not `AgentClient`) since `spring-ai-claude-agent` does not depend on `spring-ai-agent-client`
- [x] VERIFY: `./mvnw test -pl agent-models/spring-ai-claude-agent -Dtest="ClaudeAgentMcpIT"` — skips gracefully (requires CLI + API key)

**Exit criteria**:
- [x] Claude CLI IT compiles and skips gracefully when `@Disabled`
- [x] Test skips gracefully when prerequisites missing
- [x] `./mvnw spring-javaformat:apply`
- [x] Full build passes: `./mvnw clean test`
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 4.2: Add Claude CLI MCP integration test` (38c166b)

**Files**:
- NEW: `agent-models/spring-ai-claude-agent/src/test/java/org/springaicommunity/agents/claude/ClaudeAgentMcpIT.java`

---

## Key Design Decisions

1. **Client resolves, model translates.** Following the ChatClient/ChatModel pattern: `AgentClient` is the developer-facing configuration surface that holds the catalog, resolves names to portable definitions, and assembles fully-resolved requests. `AgentModel` is a connectivity detail that mechanically translates portable `McpServerDefinition` types to its native format (e.g., `McpServerConfig` for Claude). The model never sees names, never holds a catalog, never does resolution.

2. **Resolved definitions on `AgentOptions`, not names.** `AgentOptions.getMcpServerDefinitions()` carries the already-resolved `Map<String, McpServerDefinition>`. Names are a client-layer convenience — they exist on the fluent API (`mcpServers("brave-search", "weather")`) and the builder (`defaultMcpServers(...)`) but are resolved to definitions before the request reaches the model. This means models can read portable definitions from any `AgentOptions` implementation via the interface, sidestepping the `getEffectiveOptions()` `instanceof` gate.

3. **`McpServerDefinition`** — sealed interface with `Stdio/Sse/Http` record variants. Lives in `agent-model`, zero new external deps. Mirrors the structure of the Claude SDK's `McpServerConfig` without coupling to it.

4. **Multiple catalog sources.** The catalog can be built programmatically (builder/`of()`), loaded from a single JSON file, or loaded by scanning a directory. This accommodates both code-first and config-first workflows. JSON format follows the same structure as Claude's `mcp-servers-config.json` for familiarity.

5. **No Spring Boot dependency** — the catalog is plain Java. Users build it programmatically or load from files. Spring Boot users who want properties-driven config can write their own `@Bean` method. The existing Claude-specific `ClaudeAgentMcpProperties` remains unchanged for Claude-only users.

6. **`McpSdkServerConfig` (in-process) excluded** — cannot be expressed portably since it carries a live server instance. Users needing this use `ClaudeAgentOptions` directly.

7. **Merge precedence for Claude** — when both portable definitions (from client catalog resolution) and Claude-specific `ClaudeAgentOptions.mcpServers` exist with the same name, the Claude-specific entry wins. This lets users set a portable catalog and override individual servers with Claude-specific config (e.g., adding in-process SDK servers).

8. **Dual-read pattern in `buildCLIOptions()`** — portable fields (MCP definitions) are read from `request.options()` via the `AgentOptions` interface; Claude-specific fields go through `getEffectiveOptions()`. This is documented with an inline comment to prevent confusion.

---

## User Experience (end state)

### Programmatic catalog

```java
McpServerCatalog catalog = McpServerCatalog.builder()
    .add("brave-search", new McpServerDefinition.StdioDefinition(
        "npx", List.of("-y", "@modelcontextprotocol/server-brave-search"),
        Map.of("BRAVE_API_KEY", System.getenv("BRAVE_API_KEY"))))
    .add("weather", new McpServerDefinition.SseDefinition(
        "http://localhost:8080/sse", Map.of()))
    .build();
```

### File-based catalog

```java
// Single file
McpServerCatalog catalog = McpServerCatalog.fromJson(
    Path.of("config/mcp-servers.json"));

// Directory scan — merges all *.json files
McpServerCatalog catalog = McpServerCatalog.fromJson(
    Path.of("config/mcp-servers/"));
```

Where `config/mcp-servers.json` contains:
```json
{
  "mcpServers": {
    "brave-search": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": { "BRAVE_API_KEY": "${BRAVE_API_KEY}" }
    },
    "weather": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

### Using the catalog

```java
AgentClient client = AgentClient.builder(claudeModel)
    .mcpServerCatalog(catalog)
    .defaultMcpServers("weather")
    .build();

// Uses default "weather" server
client.goal("What's the forecast for Atlanta?")
    .run();

// Adds brave-search for this request
client.goal("Research Spring AI MCP support and summarize")
    .mcpServers("brave-search")
    .run();
```

### Data Flow

```
User: .mcpServers("brave-search")
  ↓
AgentClient: unions defaults ["weather"] + request ["brave-search"]
  ↓
AgentClient: resolves via catalog → Map<String, McpServerDefinition>
  ↓
AgentOptions.getMcpServerDefinitions() → resolved portable definitions
  ↓
AgentTaskRequest → advisor chain → AgentModel.call()
  ↓
ClaudeAgentModel: reads portable definitions, translates to McpServerConfig
  ↓
CLI: --mcp-config '{"weather":{"type":"sse",...},"brave-search":{"type":"stdio",...}}'
```

---

## Not In Scope (Future Enhancements)

- **Tool-level filtering within MCP servers** — e.g., "from brave-search server, only expose `brave_web_search`". Claude already supports `allowedTools`/`disallowedTools` with `mcp__<server>__<tool>` naming. Portable tool filtering is a natural next step once the catalog is working and we can test with real MCP tool names.
- Gemini MCP translation (future step, same pattern as Claude)
- Spring Boot auto-configuration for the catalog (users wire their own `@Bean` if needed)
- Deep `AgentOptions` merge for all fields (separate concern)
- In-process SDK MCP servers in the portable catalog
- MCP server health checks or connection validation
- YAML catalog format (JSON first, YAML can be added later if demand exists)

---

## Step Exit Criteria Convention

Every step's exit criteria must include these items (in addition to step-specific criteria):

```
- [ ] All tests pass: `./mvnw test` (or full build for final step)
- [ ] `./mvnw spring-javaformat:apply`
- [ ] Update `CLAUDE.md` with distilled learnings (when applicable)
- [ ] Update `ROADMAP.md` checkboxes
- [ ] COMMIT
```

Commit format: `Step X.Y: Brief description`

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-02-19T15:30-05:00 | Initial draft | Community triage revealed MCP gap |
| 2026-02-19T16:00-05:00 | Finalized forge-methodology format | User review |
| 2026-02-19T17:00-05:00 | Dropped Step 2.2 (Spring Boot auto-config). Catalog is plain Java — no Boot dependency. | User feedback |
| 2026-02-19T18:00-05:00 | v2.0: Redesigned to "client resolves, model translates". Catalog lives only on AgentClient. AgentOptions carries resolved definitions, not names. Model does mechanical translation only. | Design review + ChatClient/ChatModel layering principle |
| 2026-02-19T19:00-05:00 | v2.1: Incorporated review findings as explicit work items. Added file-based catalog loading (JSON files, directory scan). Updated examples to use brave-search and weather servers (not filesystem — CLI agents have built-in file search). | Design review v2.0 results + user feedback |
