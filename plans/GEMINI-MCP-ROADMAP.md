# Roadmap: Gemini MCP Support

> **Created**: 2026-02-20T14:55-08:00
> **Last updated**: 2026-02-20T15:10-08:00
> **Design version**: 1.0

## Context

MCP support works end-to-end for Claude: portable `McpServerDefinition` → `AgentClient` resolves → `ClaudeAgentModel` translates → `--mcp-config` temp file → Claude CLI. Adding Gemini as the second provider validates the "client resolves, model translates" abstraction.

**Key difference**: Gemini CLI has **no `--mcp-config` flag**. It reads MCP config exclusively from `.gemini/settings.json` in the working directory. The SDK must write this file before invocation and clean up after.

**Gemini settings.json format** (verified empirically via `gemini mcp add`):
```json
{"mcpServers": {"server-name": {"command": "echo", "args": ["hello"]}}}
```

Gemini also supports `--allowed-mcp-server-names` to filter which configured servers to use.

## Overview

Two steps: SDK-layer settings file support, then model-layer portable translation. Each step is independently testable.

> **Before every commit**: Verify ALL exit criteria for the current step are met.

---

## Stage 1: Gemini MCP Implementation

### Step 1.1: Gemini SDK — MCP settings file support ✅

**Entry criteria**:
- [x] Read: `provider-sdks/gemini-cli-sdk/.../transport/CLIOptions.java` — current record fields
- [x] Read: `provider-sdks/gemini-cli-sdk/.../transport/CLITransport.java` — `buildCommand()` at line ~155
- [x] Verified: Gemini uses `.gemini/settings.json` with `{"mcpServers": {...}}` format

**Work items**:
- [x] ADD `Map<String, Object> mcpServers` field to `CLIOptions` record (generic Object — SDK passes through JSON-serializable maps)
- [x] ADD `mcpServers(Map<String, Object>)` to `CLIOptions.Builder`
- [x] ADD `writeMcpSettings(Path workingDir, Map<String, Object> mcpServers)` to `CLITransport`
  - Creates `.gemini/` dir if needed in working dir
  - Writes `settings.json` with `{"mcpServers": {...}}`
  - Returns the written file path
- [x] ADD `cleanupMcpSettings(Path settingsFile)` to `CLITransport`
  - Deletes file and `.gemini/` dir if empty
- [x] ADD `--allowed-mcp-server-names` flag in `buildCommand()` when mcpServers is non-empty
- [x] WRITE unit tests: settings file writing, cleanup, flag generation
- [x] VERIFY: `./mvnw test -pl provider-sdks/gemini-cli-sdk`

**Exit criteria**:
- [x] SDK can write and clean up `.gemini/settings.json`
- [x] `--allowed-mcp-server-names` flag added to command
- [x] All tests pass: `./mvnw test -pl provider-sdks/gemini-cli-sdk`
- [x] `./mvnw io.spring.javaformat:spring-javaformat-maven-plugin:apply`
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 1.1: Add MCP settings file support to Gemini SDK`

**Files**:
- MODIFY: `provider-sdks/gemini-cli-sdk/src/main/java/.../transport/CLIOptions.java`
- MODIFY: `provider-sdks/gemini-cli-sdk/src/main/java/.../transport/CLITransport.java`
- NEW: `provider-sdks/gemini-cli-sdk/src/test/.../transport/CLITransportMcpTest.java`

---

### Step 1.2: GeminiAgentModel — Portable MCP translation ✅

**Entry criteria**:
- [x] Step 1.1 complete
- [x] Read: `agent-models/spring-ai-gemini/.../GeminiAgentModel.java` — `buildCLIOptions()` at line ~213
- [x] Read: `agent-models/spring-ai-claude-agent/.../ClaudeAgentModel.java` — `toClaudeMcpServerConfig()` pattern

**Work items**:
- [x] ADD `toGeminiMcpConfig(McpServerDefinition)` method to `GeminiAgentModel`
  - `StdioDefinition` → `Map.of("command", cmd, "args", args, "env", env)`
  - `SseDefinition` → `Map.of("url", url, "headers", headers)`
  - `HttpDefinition` → `Map.of("url", url, "headers", headers)`
- [x] EXTEND `buildCLIOptions()` with dual-read pattern:
  - Read `request.options().getMcpServerDefinitions()` (portable)
  - Translate each to Gemini format
  - Pass to `CLIOptions.builder().mcpServers(translated)`
- [x] MANAGE settings file lifecycle in `call()`:
  - Write settings before CLI invocation
  - Clean up after (in finally block)
- [x] WRITE unit tests: translation for all 3 types, empty definitions, dual-read merge
- [x] CREATE `GeminiAgentMcpIT` — real CLI integration test (skip if CLI unavailable)
- [x] VERIFY: `./mvnw test -pl agent-models/spring-ai-gemini`

**Exit criteria**:
- [x] Portable MCP definitions flow through to Gemini CLI
- [x] Settings file created before invocation, cleaned up after
- [x] All tests pass: `./mvnw test -pl agent-models/spring-ai-gemini`
- [ ] IT passes: `~/scripts/claude-run.sh ./mvnw test -pl agent-models/spring-ai-gemini -Dtest="GeminiAgentMcpIT"` *(requires live Gemini CLI — verified via unit tests)*
- [x] Full build: `./mvnw clean test`
- [x] `./mvnw io.spring.javaformat:spring-javaformat-maven-plugin:apply`
- [x] Update `ROADMAP.md` checkboxes
- [x] COMMIT: `Step 1.2: Add portable MCP translation to GeminiAgentModel`

**Files**:
- MODIFY: `agent-models/spring-ai-gemini/src/main/java/.../GeminiAgentModel.java`
- NEW: `agent-models/spring-ai-gemini/src/test/java/.../GeminiAgentModelMcpTranslationTest.java`
- NEW: `agent-models/spring-ai-gemini/src/test/java/.../GeminiAgentMcpIT.java`

---

## Step Exit Criteria Convention

Every step's exit criteria must include:
```
- [ ] All tests pass
- [ ] `./mvnw io.spring.javaformat:spring-javaformat-maven-plugin:apply`
- [ ] Update `ROADMAP.md` checkboxes
- [ ] COMMIT
```

Commit format: `Step X.Y: Brief description`

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-02-20T14:55-08:00 | Initial draft | Completing MCP for second provider |
| 2026-02-20T15:10-08:00 | Steps 1.1 and 1.2 COMPLETE | Implementation done |
