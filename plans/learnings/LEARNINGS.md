# Learnings: AgentClient Multi-Provider Hardening

> **Last compacted**: 2026-04-26T01:00-04:00
> **Covers through**: Stage 1b complete

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

## Patterns Established

- **SDK neutral, agent-models translates**: `ExecuteOptions.skipGitCheck` stays at CLI-native default (`false`). `CodexAgentProperties.isSkipGitCheck()` derives the effective value from mode.
- **Nullable for explicit-vs-derived**: Use `Boolean` (nullable) instead of `boolean` (primitive) to distinguish "user explicitly set" from "derive from mode".
- **Evidence-based baselines**: Derive baselines from code structure when evidence is clear. Save API spend for post-change eval.
- **Provider-specific setUp, portable tests**: Parity IT subclasses differ only in `@BeforeEach` setup and `getProvider()`. All scenarios inherited from TCK.
- **ProcessBuilder OK in tests**: zt-exec mandate applies to production code. Tests use `ProcessBuilder` for simple one-liners.

## Deviations from Design

| Design says | Implementation does | Why |
|-------------|-------------------|-----|
| AgentClientMode in `agent-client-core` | Placed in `agent-model` | Provider auto-configs depend on agent-model |
| Central mode bean | Mode in each provider's Properties | Simpler, no cross-module injection |
| Wire no-op mode into Claude/Gemini | Properties unchanged | Speculative — add when behavior exists |
| Steps 1a.1-1a.3 before 1a.4-1a.5 | 1a.4/1a.5/1b.1 first | Customer fix (Joachim) prioritized |

## Common Pitfalls

1. **Codex CI annotation conflict** — `@DisabledIfEnvironmentVariable(CI=true)` on existing Codex ITs prevents parity tests from running in CI. Need to either remove the annotation from parity IT or use a different CI env var approach.

## Stage Review Summaries

| Stage | Status |
|-------|--------|
| Stage 0 | Complete |
| Stage 1a | Complete |
| Stage 1b | Complete |
| Stage 2 | Not started |

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

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-04-24T15:30-04:00 | Initial draft | Roadmap created |
| 2026-04-25T00:15-04:00 | Added Stage 0 + partial 1a findings | Steps 0.1-1a.5 complete |
| 2026-04-26T00:00-04:00 | Stage 1a consolidation | All Stage 1a steps complete |
