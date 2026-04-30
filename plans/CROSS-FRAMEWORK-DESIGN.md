# Design: Cross-Framework Portability (Spring Boot / Quarkus / Micronaut)

## Goal

Make agent-client usable across the Java ecosystem — not just Spring Boot. Users should be able to use `AgentClient.create(model).run(goal)` from any framework or plain Java.

## Current State

### Jackson Leakage

Jackson (`ObjectMapper`, `@JsonProperty`, `JsonNode`) appears in:

| Module | Usage | Severity |
|--------|-------|----------|
| `agent-model` | `McpServerCatalog` JSON parsing, `StructuredOutputPromptHelper` | **High** — core abstraction depends on Jackson |
| `gemini-cli-sdk` | CLI output parsing, MCP settings writing, DTO annotations | Medium — SDK-specific |
| `codex-cli-sdk` | None (text-based CLI output) | Clean |
| `claude-code-sdk` | External dependency (claude-code-sdk 1.0.0) | External |
| `agent-client-core` | None | Clean |

### Spring Boot Leakage

| Module | Usage | Severity |
|--------|-------|----------|
| `agent-client-core` | `AgentClientAutoConfiguration` (1 file) | **High** — blocks Quarkus/Micronaut |
| `agent-*/autoconfigure/` | `@ConfigurationProperties`, `@AutoConfiguration` | Expected — these ARE Spring modules |

## Three-Layer Architecture (from MCP Java SDK precedent)

### Layer 1: Core (zero framework deps)

```
agent-model/          — AgentApi (renamed from AgentModel), AgentOptions, AgentTaskRequest, AgentResponse
agent-client-core/    — AgentClient, Goal, AgentClientResponse (remove autoconfigure)
agent-json-core/      — JsonCodec interface + TypeRef (NEW)
```

- No Jackson annotations on DTOs
- No Spring Boot dependency
- `JsonCodec` discovered via `ServiceLoader` (same pattern as MCP SDK's `McpJsonMapper`)

### Layer 2: JSON Adapters

```
agent-json-jackson/   — JacksonJsonCodec implements JsonCodec (NEW)
```

Future:
```
agent-json-micronaut/ — MicronautJsonCodec (when Micronaut support ships)
agent-json-jsonb/     — JsonBCodec for Quarkus JSON-B users
```

### Layer 3: Framework Integration

```
agent-starter-spring-boot/  — Spring Boot auto-config (existing starters, refactored)
```

Future:
```
agent-extension-quarkus/    — Quarkus CDI extension
agent-factory-micronaut/    — Micronaut bean factory
```

## Jackson Removal Plan for Core

### `agent-model` — McpServerCatalog

Currently uses `ObjectMapper` to parse `mcp-servers.json`. Replace with:
```java
McpServerCatalog.fromJson(String json, JsonCodec codec)
```

Default: `McpServerCatalog.fromJson(path)` discovers `JsonCodec` via ServiceLoader.

### `agent-model` — StructuredOutputPromptHelper

Uses `ObjectMapper.writeValueAsString()` to format JSON schema. Replace with `JsonCodec.toJson()`.

### `gemini-cli-sdk` — DTO Annotations

`@JsonProperty` annotations on `Metadata`, `Usage`, `Cost` records. Options:
1. Remove annotations, use naming convention (snake_case → camelCase in codec config)
2. Keep annotations but make them optional (Jackson adapter reads them, other codecs use conventions)

Recommendation: **Option 2** — Jackson annotations are a compile-time no-op if Jackson isn't on the classpath. They only activate when Jackson is the runtime codec. This avoids breaking the Gemini SDK while adding portability.

## MCP Java SDK Pattern Summary

The MCP SDK uses:
- `McpJsonMapper` interface in `mcp-core` (zero Jackson deps)
- `JacksonMcpJsonMapper` in `mcp-json-jackson2` (separate module)
- `McpJsonMapperSupplier` as a `Supplier<McpJsonMapper>` for ServiceLoader discovery
- `McpJsonDefaults.getMapper()` lazy-loads via ServiceLoader on first call
- Supports OSGi via SCR component injection as alternative to ServiceLoader

## Implementation Phases

### Phase 1: Extract Autoconfigure (Step 5.0 — existing roadmap)
- Move `AgentClientAutoConfiguration` out of `agent-client-core`
- `agent-client-core` compiles with zero Spring Boot deps
- No user-visible change (starters still work)

### Phase 2: JSON Abstraction
- Create `agent-json-core` with `JsonCodec` interface + `TypeRef`
- Create `agent-json-jackson` with `JacksonJsonCodec`
- Replace direct `ObjectMapper` usage in `agent-model` with `JsonCodec`
- ServiceLoader discovery (same as MCP SDK)
- **Breaking change**: users need `agent-json-jackson` on classpath (starters include it transitively)

### Phase 3: Quarkus/Micronaut Integration
- Create framework-specific modules with DI wiring
- Test with native image compilation
- Document getting-started for each framework

## Open Questions

1. **Should we align naming with MCP SDK?** `AgentJsonMapper` vs `JsonCodec` — consistency across community projects?
2. **Jackson annotation tolerance**: keep `@JsonProperty` on Gemini SDK DTOs or strip them?
3. **Scope of Phase 1**: should autoconfigure extraction happen before or after AgentModel→AgentApi rename?
4. **BOM**: do we need an `agent-client-bom` for managing versions across all these new modules?
