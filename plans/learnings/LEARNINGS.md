# Learnings: AgentClient Multi-Provider Hardening

> **Last compacted**: 2026-04-29T21:15-04:00
> **Covers through**: Stage 4 complete

This is the **Tier 1 compacted summary**. Read this first for the current state of project knowledge. For details on specific steps, see the per-step files (Tier 2).

---

## Key Discoveries

1. **Codex is the only provider with a non-git directory gate** — `skipGitCheck=false` blocks execution even when `fullAuto=true`. Claude and Gemini work in any directory with default settings.
   - *Source*: Step 0.1
   - *Impact*: Validated Track 1 need. Fixed via AgentClientMode.LOOSE defaulting skipGitCheck=true.

2. **AgentClientMode belongs in agent-model, not agent-client-core** — provider auto-config modules depend on `agent-model`, not `agent-client-core`. Mode enum placed alongside `AgentOptions`.
   - *Source*: Step 1a.0
   - *Impact*: No new cross-module dependencies. Each provider's `*Properties` reads mode directly.

3. **Don't wire speculative no-op fields** — plan called for mode fields in Claude/Gemini properties. Rejected — add fields when there's actual behavior, not before.
   - *Source*: Step 1a.5
   - *Impact*: Only Codex has mode-dependent behavior today.

4. **@ProviderCapability with JUnit Assumptions works cleanly** — annotation on TCK methods + `Assumptions.assumeTrue(false)` produces "skipped" in surefire XML, distinct from pass/fail.
   - *Source*: Step 1a.1
   - *Impact*: CI summary can show PASS/FAIL/NOT_APPLICABLE per provider.

5. **Codex IT `@DisabledIfEnvironmentVariable(CI=true)` will skip in parity CI** — the existing annotation prevents Codex parity tests from running in CI. Needs attention before the parity workflow is useful for Codex.
   - *Source*: Step 1a.3
   - *Impact*: Track for resolution in Stage 1b or next session.

6. **QDox is the right tool for extracting Javadoc from source** — reflection cannot access Javadoc (stripped at compile time). QDox parses source files directly, extracting field names, types, initializers, and Javadoc comments in one pass.
   - *Source*: Step 2.1
   - *Impact*: Docgen tool in `tools/agent-options-docgen/` uses QDox to generate reference tables.

7. **Claude has 18 options — split standard vs advanced** — too many for one reference table. Standard options (13) cover common use. Advanced options (7) provide Python SDK parity for power users.
   - *Source*: Step 2.2
   - *Impact*: Reference page structure: standard table + advanced table with clear labeling.

8. **Tutorial should not require Spring Boot** — `AgentClient` has static factory methods (`create()`, `builder()`) that work standalone. The hello world is plain Java: build a model, create a client, run a goal. Spring Boot is a convenience layer, not a requirement.
   - *Source*: Step 3.1
   - *Impact*: Getting-started and tutorial pages show plain Java as primary path. Spring Boot is secondary.

9. **`agent-client-core` has a misplaced Spring Boot dependency** — depends on `spring-boot-autoconfigure` for one file (`AgentClientAutoConfiguration`). Should be extracted to a separate module. Added as Step 5.0.
   - *Source*: Step 3.1
   - *Impact*: Architecture fix planned. Core API should be plain Java.

10. **`AgentModel` is a misleading name** — it wraps a CLI agent runtime, not a model. Rename to `AgentApi` planned for Step 5.1 with deprecation shims (old class extends new, `@Deprecated(forRemoval=true)`).
    - *Source*: Step 3.1
    - *Impact*: One-release deprecation cycle. `ClaudeAgentModel` → `ClaudeAgentApi`, etc.

11. **Codex `workspace-write` sandbox fails via bwrap** — `--full-auto` sets `workspace-write` which uses bubblewrap. Fails with `bwrap: loopback: Failed RTM_NEWADDR` in many environments. Terminal-bench uses `--sandbox danger-full-access` instead.
    - *Source*: Step 4.1
    - *Impact*: LOOSE now derives `dangerouslyBypassSandbox=true` for Codex.

12. **Codex model `gpt-5-codex` removed in CLI 0.125.0** — old default caused CLI to hang on stdin. Updated to `gpt-5.4-mini`.
    - *Source*: Step 4.1
    - *Impact*: Default model updated across SDK, Properties, and all tests.

13. **Gemini working directory not propagated** — `GeminiAgentModel` only passed working dir in prompt text, not to the actual `ProcessExecutor.directory()`. Files created in wrong location.
    - *Source*: Step 4.1
    - *Impact*: Added `workingDirOverride` param to `CLITransport.executeQuery()` and `GeminiClient.query()`.

14. **Global `spring.ai.agents.mode` not implemented** — only per-provider `spring.ai.agents.codex.mode` works. Documented in `defaults-philosophy.mdx` but no central binding exists.
    - *Source*: Step 4.1
    - *Impact*: Added as Step 5.5 in roadmap.

15. **No CLI arg validation tests** — SDK flag mappings drift as CLIs update. Model names, flag names, and stdin behavior change between versions. No automated check exists.
    - *Source*: Step 4.1
    - *Impact*: Added as Steps 5.3 (validation test) and 5.4 (daily CI) in roadmap.

## Patterns Established

- **SDK neutral, agent-models translates**: `ExecuteOptions.skipGitCheck` stays at CLI-native default (`false`). `CodexAgentProperties.isSkipGitCheck()` derives the effective value from mode.
- **Nullable for explicit-vs-derived**: Use `Boolean` (nullable) instead of `boolean` (primitive) to distinguish "user explicitly set" from "derive from mode".
- **Evidence-based baselines**: Derive baselines from code structure when evidence is clear. Save API spend for post-change eval.
- **Provider-specific setUp, portable tests**: Parity IT subclasses differ only in `@BeforeEach` setup and `getProvider()`. All scenarios inherited from TCK.
- **ProcessBuilder OK in tests**: zt-exec mandate applies to production code. Tests use `ProcessBuilder` for simple one-liners.
- **Inline tables over Snippet includes**: Generated reference content goes directly into .mdx pages rather than using Mintlify `<Snippet>` cross-file includes. Simpler maintenance, one place to edit.
- **Standalone docgen tool**: `tools/agent-options-docgen/` is a standalone Maven module (not in parent POM). Run via `./mvnw -f tools/agent-options-docgen/pom.xml compile exec:java`.
- **Diataxis subdirectories**: Docs organized into `tutorial/`, `howto/`, `reference/`, `explanation/` subdirectories — not just nav groups but physical directory structure matching the quadrants.

## Deviations from Design

| Design says | Implementation does | Why |
|-------------|-------------------|-----|
| AgentClientMode in `agent-client-core` | Placed in `agent-model` | Provider auto-configs depend on agent-model |
| Central mode bean | Mode in each provider's Properties | Simpler, no cross-module injection |
| Wire no-op mode into Claude/Gemini | Properties unchanged | Speculative — add when behavior exists |
| Steps 1a.1-1a.3 before 1a.4-1a.5 | 1a.4/1a.5/1b.1 first | Customer fix (Joachim) prioritized |
| Tutorial uses Spring Boot | Plain Java primary | AgentClient doesn't require Spring Boot |
| Snippet includes for generated tables | Inline tables | Simpler — one file to edit |
| `AgentModel` naming | Keep for now, rename in 5.1 | Deprecation shim needed for backwards compat |

## Common Pitfalls

1. **Codex CI annotation conflict** — `@DisabledIfEnvironmentVariable(CI=true)` on existing Codex ITs prevents parity tests from running in CI. Need to either remove the annotation from parity IT or use a different CI env var approach.

## Stage Review Summaries

| Stage | Status |
|-------|--------|
| Stage 0 | Complete |
| Stage 1a | Complete |
| Stage 1b | Complete |
| Stage 2 | Complete |
| Stage 3 | Complete (onramp metric deferred) |
| Stage 4 | Complete |

---

## Per-Step Detail Files (Tier 2)

| File | Step | Topic |
|------|------|-------|
| `step-0.1-baseline-eval.md` | 0.1 | Terminal-bench baseline — Codex skipGitCheck blocks non-git dirs |
| `step-0.2-stage0-summary.md` | 0.2 | Stage 0 consolidation |
| `step-1a.0-design-review.md` | 1a.0 | Design review — AgentClientMode location, extension points |
| `step-1a.1-parity-tck.md` | 1a.1 | Parity TCK — @ProviderCapability, 4 scenarios, JUnit Assumptions |
| `step-1a.2-provider-wiring.md` | 1a.2 | Provider IT wiring — Claude/Codex/Gemini parity ITs |
| `step-1a.3-ci-matrix.md` | 1a.3 | CI matrix workflow — provider matrix, summary job |
| `step-1a.4-mode-enum.md` | 1a.4 | AgentClientMode enum — nullable skipGitCheck, SDK-neutral |
| `step-1a.5-mode-wiring.md` | 1a.5 | Mode wiring — Codex only, no speculative fields |
| `step-1a.6-defaults-philosophy.md` | 1a.6 | Defaults philosophy doc — LOOSE/STRICT, precedence, migration |
| `step-1a.7-stage1a-summary.md` | 1a.7 | Stage 1a consolidation |
| `step-1b.1-multi-provider-sample.md` | 1b.1 | Multi-provider sample — Maven profiles, portable API |
| `step-1b.2-tutorial-repo.md` | 1b.2 | Tutorial repo — agent-client-tutorial on GitHub |
| `step-1b.3-coordination-repo.md` | 1b.3 | Coordination repo — tuvium/agent-client-doc-agent |
| `step-1b.4-stage1b-summary.md` | 1b.4 | Stage 1b consolidation |
| `step-2.0-stage2-entry.md` | 2.0 | Stage 2 entry — properties inventory, mintlify-docs state |
| `step-2.1-docgen-tool.md` | 2.1 | Docgen tool — QDox source parser, Markdown table generator |
| `step-2.2-reference-pages.md` | 2.2 | Reference pages — 4 .mdx pages, mint.json nav update |
| `step-2.3-stage2-summary.md` | 2.3 | Stage 2 consolidation |
| `step-3.0-stage3-entry.md` | 3.0 | Stage 3 entry — API surface, tutorial repo state |
| `step-3.1-howto-tutorial-pages.md` | 3.1 | How-to + tutorial pages — plain Java primary, no Spring Boot |
| `step-3.2-diataxis-nav.md` | 3.2 | Diataxis nav restructure — 4 quadrant subdirectories |
| `step-3.3-stage3-summary.md` | 3.3 | Stage 3 consolidation |

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-04-24T15:30-04:00 | Initial draft | Roadmap created |
| 2026-04-25T00:15-04:00 | Added Stage 0 + partial 1a findings | Steps 0.1-1a.5 complete |
| 2026-04-26T00:00-04:00 | Stage 1a consolidation | All Stage 1a steps complete |
| 2026-04-29T15:15-04:00 | Stage 2 consolidation | Docgen tool + reference pages complete |
| 2026-04-29T16:00-04:00 | Stage 3 consolidation | How-to, tutorials, Diataxis restructure, architecture issues identified |
