# Step 2.0 Learnings: Stage 2 Entry + Context Load

## Context Loaded

- Stage 1b complete and pushed (commits through `c76cc21`)
- Project version now `0.15.0-SNAPSHOT`, Spring Boot 4.0.1, Spring AI 2.0.0-M2
- Build compiles clean on main, git is clean

## Properties Inventory (source of truth for docgen)

| Provider | Prefix | Fields | Key Options |
|----------|--------|--------|-------------|
| Claude | `spring.ai.agents.claude-code` | 18 | model, timeout, yolo, maxThinkingTokens, systemPrompt, allowedTools, disallowedTools, permissionMode, jsonSchema, maxTokens, maxTurns, maxBudgetUsd, fallbackModel, appendSystemPrompt, addDirs, settings, permissionPromptToolName, extraArgs, env, maxBufferSize, user |
| Codex | `spring.ai.agents.codex` | 6 | mode, model, timeout, fullAuto, skipGitCheck, executablePath |
| Gemini | `spring.ai.agents.gemini` | 6 | model, timeout, yolo, executablePath, temperature, maxTokens |

Total: ~30 configurable properties across 3 providers.

## Mintlify-docs Current State

- Agent Client nav group: `projects/incubating/agent-client`, `agent-client/defaults-philosophy`, `agent-client/sessions`
- Reference pages needed: portable-options, claude-reference, codex-reference, gemini-reference
- Pattern to follow: Claude Agent SDK has a `reference/java` page — similar structure

## Open Issues from Stage 1b (still open)

1. Codex `@DisabledIfEnvironmentVariable(CI=true)` — prevents parity tests in CI
2. OPENAI_API_KEY secret — needs adding to agent-client and tutorial repos
3. Tutorial CI — compile-only until secrets configured
4. Personal `markpollack/agent-client-doc-agent` — leftover repo

## Design Decision: Docgen Approach

The tool will use QDox (lightweight Java source parser) to read `*Properties.java` files and extract:
- Field names → Spring property names (camelCase → kebab-case)
- Field types
- Default values (from field initializers)
- Descriptions (from Javadoc comments)
- @ConfigurationProperties prefix

Output: Markdown tables per provider. These get included inline in the reference .mdx pages.

The tool lives in `tools/agent-options-docgen/` — standalone Maven module, not in parent POM modules list, not published to Central.
