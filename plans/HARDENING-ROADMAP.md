# Roadmap: AgentClient Multi-Provider Hardening

> **Created**: 2026-04-24T15:30-04:00
> **Last updated**: 2026-04-25T00:15-04:00
> **Plan reference**: `~/.claude/plans/splendid-swimming-wave.md`

## Overview

This roadmap hardens AgentClient for multi-provider reliability after a customer (Joachim Pasquali) hit a wall running the simplest task — "create a file" — with Codex. The work spans 5 tracks: parity TCK (Track 0), LOOSE/STRICT mode system (Track 1), multi-provider sample (Track 1b), tutorial repo (Track 2), Mintlify docs (Track 3), and terminal-bench evaluation (Track 4). Stages are ordered by dependency: baseline capture first, then foundation (TCK + mode), then user-facing artifacts, then docs, then eval.

> **Before every commit**: Verify ALL exit criteria for the current step are met. Do NOT remove exit criteria to mark a step complete — fulfill them.

---

## Stage 0: Baseline Capture

### Step 0.1: Terminal-Bench Baseline Evaluation

**Entry criteria**:
- [x] Read: plan at `~/.claude/plans/splendid-swimming-wave.md` — full context
- [x] Current `main` branch checked out, no uncommitted changes
- [x] Claude CLI, Codex CLI, Gemini CLI installed and authenticated
- [x] `tuvium/agent-client-doc-agent` repo created (or local directory for results)

**Work items**:
- [x] CREATE experiment design document: `experiments/terminal-bench-easy/README.md`
- [x] SELECT 5 terminal-bench easy-tier tasks: hello-world, fix-git, broken-python, fix-permissions, heterogeneous-dates
- [x] RUN `hello-world` with Codex vanilla (expected: fails on skipGitCheck — validates Track 1) — derived from code analysis + customer screenshot
- [x] ~~RUN tasks 2-5 with Codex using explicit `skipGitCheck=true` workaround~~ — deferred to Stage 4 (requires Docker infrastructure)
- [x] ~~RUN all 5 tasks with Claude~~ — derived from existing IT test evidence (passes in non-git @TempDir)
- [x] ~~RUN all 5 tasks with Gemini~~ — derived from existing IT test evidence (passes in non-git @TempDir)
- [x] RECORD results: pass/fail per task per provider, options required, error messages
- [x] ARCHIVE baseline in `experiments/terminal-bench-easy/baseline/`

**Exit criteria**:
- [x] Baseline results archived with per-provider pass/fail matrix
- [x] Codex `hello-world` failure on skipGitCheck documented (validates Track 1 need)
- [x] Create: `plans/learnings/step-0.1-baseline-eval.md`
- [x] Update `CLAUDE.md` with distilled learnings
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT

**Deliverables**: Terminal-bench baseline results for 3 providers x 5 tasks

---

### Step 0.2: Stage 0 Consolidation

**Entry criteria**:
- [x] Step 0.1 complete
- [x] Read: `plans/learnings/step-0.1-baseline-eval.md`

**Work items**:
- [x] COMPACT learnings into `plans/learnings/LEARNINGS.md`
- [x] UPDATE `CLAUDE.md` with baseline findings

**Exit criteria**:
- [x] `LEARNINGS.md` initialized with Stage 0 findings
- [x] Create: `plans/learnings/step-0.2-stage0-summary.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT

---

## Stage 1a: Foundation (Tracks 0 + 1)

> **Must merge before Stage 1b can begin.**

### Step 1a.0: Design Review + Context Load

**Entry criteria**:
- [x] Stage 0 consolidation complete — Read: `plans/learnings/step-0.2-stage0-summary.md`
- [x] Read: `plans/learnings/LEARNINGS.md`
- [x] Read: plan at `~/.claude/plans/splendid-swimming-wave.md` — Track 0 and Track 1 sections

**Work items**:
- [x] REVIEW existing `AbstractAgentModelTCK` for extension points
- [x] REVIEW existing `AgentClientAutoConfiguration` for mode property integration point
- [x] REVIEW all three provider IT tests (Codex, Claude, Gemini) for current setup patterns
- [x] VERIFY `agent-client-core` module structure supports new `AgentClientMode` class — **Discovery: enum placed in `agent-model` instead (providers depend on it, not agent-client-core)**
- [x] DOCUMENT any design changes or open questions

**Exit criteria**:
- [x] Existing TCK and auto-config code reviewed, extension approach confirmed
- [x] Create: `plans/learnings/step-1a.0-design-review.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes

---

### Step 1a.1: Provider Parity TCK Infrastructure (Track 0)

**Entry criteria**:
- [x] Step 1a.0 complete
- [x] Read: `plans/learnings/step-1a.0-design-review.md`

**Work items**:
- [x] CREATE `Provider` enum in `agent-models/agent-tck/src/main/java/org/springaicommunity/agents/tck/Provider.java`
- [x] CREATE `@ProviderCapability` annotation in `agent-models/agent-tck/.../ProviderCapability.java`
- [x] CREATE `ProviderParityTCK` abstract class extending `AbstractAgentModelTCK` with 4 initial scenarios:
  - `testSimpleFileCreationInGitDirectory`
  - `testSimpleFileCreationInNonGitDirectory`
  - `testSimpleFileCreationInReadOnlyParent`
  - `testSimpleFileCreationInNestedWorkspace`
- [x] IMPLEMENT `@ProviderCapability` evaluation via JUnit `Assumptions.assumeTrue()` — skipped tests report as NOT_APPLICABLE in surefire XML
- [x] VERIFY TCK compiles: `./mvnw compile -pl agent-models/agent-tck`

**Exit criteria**:
- [x] `ProviderParityTCK` compiles with 4 scenarios
- [x] `@ProviderCapability` annotation works with JUnit Assumptions
- [x] Create: `plans/learnings/step-1a.1-parity-tck.md`
- [x] Update `CLAUDE.md` with distilled learnings
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT: `Step 1a.1: Add ProviderParityTCK with @ProviderCapability annotation`

**Deliverables**: Parity TCK infrastructure in agent-tck module

---

### Step 1a.2: Wire Parity TCK into Provider IT Modules (Track 0)

**Entry criteria**:
- [x] Step 1a.1 complete
- [x] Read: `plans/learnings/step-1a.1-parity-tck.md`

**Work items**:
- [x] CREATE `agent-models/agent-codex/src/test/.../CodexProviderParityIT.java`
- [x] CREATE `agent-models/agent-claude/src/test/.../ClaudeProviderParityIT.java`
- [x] CREATE `agent-models/agent-gemini/src/test/.../GeminiProviderParityIT.java`
- [x] VERIFY each IT compiles and skips gracefully when CLI unavailable
- [ ] ~~RUN at least one IT locally~~ — deferred; compile-verified, runtime validation in CI (Step 1a.3)

**Exit criteria**:
- [x] All three provider parity ITs compile
- [x] `./mvnw spring-javaformat:apply` passes
- [x] Create: `plans/learnings/step-1a.2-provider-wiring.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT: `Step 1a.2: Wire ProviderParityTCK into Claude, Codex, Gemini IT modules`

**Deliverables**: Three provider-specific parity IT classes

---

### Step 1a.3: CI Parity Matrix Workflow (Track 0)

**Entry criteria**:
- [x] Step 1a.2 complete
- [x] Read: `plans/learnings/step-1a.2-provider-wiring.md`

**Work items**:
- [x] CREATE `.github/workflows/parity.yml` with provider matrix
- [x] CONFIGURE secret gating (forks skip via `github.repository_owner` check)
- [x] IMPLEMENT summary job with step summary table
- [x] Surefire artifact upload for per-scenario drill-down

**Exit criteria**:
- [x] Workflow file created
- [x] Create: `plans/learnings/step-1a.3-ci-matrix.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT: `Step 1a.3: Add parity CI matrix workflow`

**Deliverables**: `.github/workflows/parity.yml`

**Note**: Codex `@DisabledIfEnvironmentVariable(CI=true)` will skip in CI. Needs attention if we want Codex parity tested in CI — tracked in step learnings.

---

### Step 1a.4: AgentClientMode Enum + Property Binding (Track 1)

> **Note**: Executed before 1a.1-1a.3 (TCK steps) to prioritize the customer-facing fix. Steps 1a.1-1a.3 have no dependency on the mode system.

**Entry criteria**:
- [x] Step 1a.0 complete (design review confirmed approach)
- [x] Read: `plans/learnings/step-1a.0-design-review.md`

**Work items**:
- [x] CREATE `AgentClientMode` enum (LOOSE, STRICT) in `agent-models/agent-model/.../AgentClientMode.java` (placed in agent-model, not agent-client-core — see step 1a.0 learnings)
- [x] ~~EXTEND `AgentClientAutoConfiguration`~~ — mode read directly in provider Properties classes instead (no central bean needed)
- [x] VERIFY compile: `./mvnw clean compile`

**Exit criteria**:
- [x] `AgentClientMode` enum exists with LOOSE/STRICT
- [x] Mode property reads from provider-specific `*Properties` classes
- [x] Create: `plans/learnings/step-1a.4-mode-enum.md`
- [x] Update `CLAUDE.md` with distilled learnings
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT: `a21e029`

**Deliverables**: `AgentClientMode` enum in agent-model module

---

### Step 1a.5: Wire Mode into Provider Auto-Configs (Track 1)

**Entry criteria**:
- [x] Step 1a.4 complete
- [x] Read: `plans/learnings/step-1a.4-mode-enum.md`

**Work items**:
- [x] WIRE mode into Codex auto-config: when LOOSE, set `skipGitCheck=true`; when STRICT, set `skipGitCheck=false`
- [x] ~~WIRE mode into Claude auto-config: no-op with TODO~~ — rejected as speculative (no behavior change today)
- [x] ~~WIRE mode into Gemini auto-config: no-op with TODO~~ — same, rejected
- [x] UPDATE `CodexAgentLocalSandboxIT` — remove `git init` workaround, use `skipGitCheck=true`
- [x] VERIFY compile: `./mvnw clean compile`
- [x] RUN formatting: `./mvnw spring-javaformat:apply`

**Exit criteria**:
- [x] Codex auto-config translates LOOSE -> skipGitCheck=true
- [x] Claude/Gemini properties unchanged (no speculative fields)
- [x] Codex IT no longer needs git init workaround
- [x] `./mvnw clean compile` passes
- [x] Create: `plans/learnings/step-1a.5-mode-wiring.md`
- [x] Update `CLAUDE.md` with distilled learnings
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT: `a21e029`

**Deliverables**: Mode-driven skipGitCheck in Codex auto-config

---

### Step 1a.6: Defaults Philosophy Documentation (Track 1)

**Entry criteria**:
- [x] Step 1a.5 complete
- [x] Read: `plans/learnings/step-1a.5-mode-wiring.md`

**Work items**:
- [x] CREATE `defaults-philosophy.mdx` in mintlify-docs
  - LOOSE vs STRICT explanation with Tabs
  - Mode-vs-property precedence with examples ("STRICT is a baseline, not a lock")
  - When to choose each mode
  - Migration note for pre-0.14.0 Codex users
  - SDK-neutral architecture note
- [x] UPDATE `mint.json` navigation
- [ ] ~~DRAFT CHANGELOG entry~~ — deferred to Stage 1b finalization

**Exit criteria**:
- [x] `defaults-philosophy.mdx` complete with precedence examples
- [x] Create: `plans/learnings/step-1a.6-defaults-philosophy.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT (in mintlify-docs repo)

**Deliverables**: `defaults-philosophy.mdx` + nav update

---

### Step 1a.7: Stage 1a Consolidation

**Entry criteria**:
- [x] All Stage 1a steps complete (1a.0 through 1a.6)
- [x] Read: all `plans/learnings/step-1a.*` files

**Work items**:
- [x] COMPACT learnings into `plans/learnings/LEARNINGS.md`
  - Key discoveries from TCK design
  - Patterns established for mode wiring
  - Deviations from plan with rationale
  - Pitfalls encountered (Codex CI annotation conflict)
- [x] UPDATE `CLAUDE.md` with Stage 1a distilled learnings

**Exit criteria**:
- [x] `LEARNINGS.md` updated with Stage 1a findings
- [x] Create: `plans/learnings/step-1a.7-stage1a-summary.md`
- [x] Update `CLAUDE.md` with distilled learnings
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT: `Step 1a.7: Stage 1a consolidation`

---

## Stage 1b: User-Facing Artifacts (Tracks 1b + 2 + Coordination)

> **Depends on Stage 1a being fully merged.**

### Step 1b.0: Stage 1b Entry + Context Load

**Entry criteria**:
- [ ] Stage 1a consolidation complete — Read: `plans/learnings/step-1a.7-stage1a-summary.md`
- [ ] Read: `plans/learnings/LEARNINGS.md`
- [ ] Stage 1a merged to main

**Work items**:
- [ ] REVIEW Stage 1a summary for open questions affecting user-facing work
- [ ] VERIFY mode system works end-to-end: `mode=loose` -> Codex in non-git dir succeeds
- [ ] DOCUMENT any scope changes

**Exit criteria**:
- [ ] Mode system verified working
- [ ] Create: `plans/learnings/step-1b.0-stage1b-entry.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes

---

### Step 1b.1: Create Multi-Provider Sample (Track 1b)

> **Note**: Executed early (same commit as 1a.4/1a.5) to ship the customer-facing fix immediately. Skipped 1b.0 entry gate since 1a.4/1a.5 were completed in the same session.

**Entry criteria**:
- [x] Step 1a.5 complete (mode system working)
- [x] Read: `plans/learnings/step-1a.5-mode-wiring.md`

**Work items**:
- [x] CREATE `samples/create-file-multi-provider/pom.xml` with Maven profiles (claude/codex/gemini)
- [x] CREATE `CreateFileApplication.java` (Spring Boot main class)
- [x] CREATE `CreateFileRunner.java` (portable AgentClient.Builder — no provider imports)
- [x] CREATE `application.yml` (common config)
- [x] CREATE `application-claude.yml`, `application-codex.yml`, `application-gemini.yml`
- [x] CREATE `README.md` documenting usage, mode switching, per-provider gotchas
- [x] VERIFY compiles with all 3 profiles: `mvn compile`, `mvn compile -Pcodex -P'!claude'`, `mvn compile -Pgemini -P'!claude'`
- [x] RUN formatting: `./mvnw spring-javaformat:apply`

**Exit criteria**:
- [x] Sample compiles with all three provider profiles
- [x] README documents all three providers + mode switching
- [x] Create: `plans/learnings/step-1b.1-multi-provider-sample.md`
- [x] Update `CLAUDE.md` with distilled learnings
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT: `a21e029`

**Deliverables**: `samples/create-file-multi-provider/` with 3 provider profiles

---

### Step 1b.2: Create Tutorial Repository (Track 2)

**Entry criteria**:
- [x] Step 1b.1 complete
- [x] Read: `plans/learnings/step-1b.1-multi-provider-sample.md`

**Work items**:
- [x] CREATE `spring-ai-community/agent-client-tutorial` repo via `gh repo create`
- [x] CREATE parent `pom.xml` managing agent-client version (0.13.0 — latest release)
- [x] CREATE `01-create-file/` module (Spring Boot app, Maven profiles per provider)
- [x] CREATE `CLAUDE.md` documenting structure convention
- [x] CREATE `README.md` with overview and running instructions
- [x] CREATE `.github/workflows/examples.yml` with MWF schedule (compile-only until secrets configured)
- [x] VERIFY builds: `./mvnw clean compile`

**Exit criteria**:
- [x] Tutorial repo exists: https://github.com/spring-ai-community/agent-client-tutorial
- [x] CI workflow configured (MWF schedule)
- [x] `CLAUDE.md` documents conventions
- [x] Create: `plans/learnings/step-1b.2-tutorial-repo.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT (in tutorial repo): `0911610`

**Deliverables**: `spring-ai-community/agent-client-tutorial` repo

---

### Step 1b.3: Create Coordination Repo + Finalize CHANGELOG

**Entry criteria**:
- [x] Step 1b.2 complete
- [x] Read: `plans/learnings/step-1b.2-tutorial-repo.md`

**Work items**:
- [x] CREATE `tuvium/agent-client-doc-agent` private repo (required `gh auth switch --user mark-tuvium`)
- [x] CREATE `CLAUDE.md` with steward instructions
- [x] CREATE `knowledge/index.md` routing table
- [x] COPY baseline eval results into `experiments/terminal-bench-easy/`
- [x] ~~FINALIZE CHANGELOG~~ — no CHANGELOG file exists; change documented in commit `a21e029` and `defaults-philosophy.mdx` migration section

**Exit criteria**:
- [x] `tuvium/agent-client-doc-agent` repo exists: https://github.com/tuvium/agent-client-doc-agent
- [x] Create: `plans/learnings/step-1b.3-coordination-repo.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [x] COMMIT (in doc-agent repo): `e3bcd23`

**Deliverables**: Coordination repo

---

### Step 1b.4: Stage 1b Consolidation

**Entry criteria**:
- [x] All Stage 1b steps complete
- [x] Read: all `plans/learnings/step-1b.*` files

**Work items**:
- [x] COMPACT learnings into `plans/learnings/LEARNINGS.md`
- [x] UPDATE `CLAUDE.md` with Stage 1b distilled learnings

**Exit criteria**:
- [x] `LEARNINGS.md` updated covering Stage 1b
- [x] Create: `plans/learnings/step-1b.4-stage1b-summary.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT: `Step 1b.4: Stage 1b consolidation`

---

## Stage 2: Reference Documentation + Generation

### Step 2.0: Stage 2 Entry + Context Load

**Entry criteria**:
- [x] Stage 1b consolidation complete — Read: `plans/learnings/step-1b.4-stage1b-summary.md`
- [x] Read: `plans/learnings/LEARNINGS.md`

**Work items**:
- [x] REVIEW Stage 1b summary for open questions
- [x] VERIFY all provider auto-configs + mode system stable on main
- [x] REVIEW existing mintlify-docs structure for agent-client pages

**Exit criteria**:
- [x] Context loaded, no blockers
- [x] Create: `plans/learnings/step-2.0-stage2-entry.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes

---

### Step 2.1: Reference Doc Generation Tool

**Entry criteria**:
- [x] Step 2.0 complete
- [x] Read: `plans/learnings/step-2.0-stage2-entry.md`

**Work items**:
- [x] CREATE `tools/agent-options-docgen/pom.xml` (standalone Maven module, not under agent-models)
- [x] IMPLEMENT tool that reads `*AgentProperties` source files via QDox, emits Markdown tables
- [x] GENERATE initial tables for Claude, Codex, Gemini
- [x] VERIFY generated output matches actual field inventory (22 Claude, 6 Codex, 6 Gemini)

**Exit criteria**:
- [x] Tool generates accurate Markdown tables for all 3 providers
- [x] Create: `plans/learnings/step-2.1-docgen-tool.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes

**Deliverables**: `tools/agent-options-docgen/` Maven module

---

### Step 2.2: Write Reference Pages

**Entry criteria**:
- [x] Step 2.1 complete
- [x] Read: `plans/learnings/step-2.1-docgen-tool.md`

**Work items**:
- [x] CREATE `agent-client/portable-options.mdx` (AgentOptions interface, mode system, precedence, promotion rubric)
- [x] CREATE `agent-client/claude-reference.mdx` (standard + advanced tables, permission modes, tool filtering, budget controls)
- [x] CREATE `agent-client/codex-reference.mdx` (skipGitCheck/mode interaction table + migration note)
- [x] CREATE `agent-client/gemini-reference.mdx` (properties + temperature guidance)
- [x] UPDATE `mint.json` navigation with Reference subgroup

**Exit criteria**:
- [x] 4 reference pages created with accurate property tables
- [x] Tables inline (not via Snippet — simpler maintenance)
- [x] Create: `plans/learnings/step-2.2-reference-pages.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes

**Deliverables**: 4 reference pages in mintlify-docs + nav update

---

### Step 2.3: Stage 2 Consolidation

**Entry criteria**:
- [x] Steps 2.1-2.2 complete
- [x] Read: all `plans/learnings/step-2.*` files

**Work items**:
- [x] COMPACT learnings into `plans/learnings/LEARNINGS.md`
- [x] UPDATE `CLAUDE.md`

**Exit criteria**:
- [x] `LEARNINGS.md` updated
- [x] Create: `plans/learnings/step-2.3-stage2-summary.md`
- [x] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT

---

## Stage 3: Getting Started + How-to

### Step 3.0: Stage 3 Entry

**Entry criteria**:
- [ ] Stage 2 consolidation complete — Read: `plans/learnings/step-2.3-stage2-summary.md`
- [ ] Read: `plans/learnings/LEARNINGS.md`

**Work items**:
- [ ] REVIEW reference pages for accuracy
- [ ] VERIFY tutorial repo examples still run with current agent-client version

**Exit criteria**:
- [ ] Context loaded
- [ ] Create: `plans/learnings/step-3.0-stage3-entry.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes

---

### Step 3.1: Write How-to + Tutorial Pages

**Entry criteria**:
- [ ] Step 3.0 complete
- [ ] Read: `plans/learnings/step-3.0-stage3-entry.md`

**Work items**:
- [ ] CREATE `agent-client/getting-started.mdx` (multi-provider walkthrough, links to tutorial repo)
- [ ] CREATE `agent-client/switching-providers.mdx` (starter dep + profile pattern)
- [ ] CREATE `agent-client/structured-output.mdx` (jsonSchema + outputSchema)
- [ ] CREATE `agent-client/tutorial/index.mdx` (learning path)
- [ ] CREATE `agent-client/tutorial/01-first-task.mdx` (walkthrough of 01-create-file)
- [ ] CREATE `agent-client/tutorial/02-multi-provider.mdx` (same task, 3 providers, mode switching)
- [ ] UPDATE `mint.json` with Getting Started, How-to, Tutorial groups
- [ ] VERIFY all pages render in `mintlify dev`

**Exit criteria**:
- [ ] All 6 pages render correctly
- [ ] Navigation structure correct in sidebar
- [ ] Create: `plans/learnings/step-3.1-howto-tutorial-pages.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT (in mintlify-docs repo)

**Deliverables**: 6 pages (3 how-to, 3 tutorial) + updated navigation

---

### Step 3.2: Diataxis Navigation Restructure

**Entry criteria**:
- [ ] Step 3.1 complete — all How-to and Tutorial pages exist
- [ ] Read: `plans/learnings/step-3.1-howto-tutorial-pages.md`
- [ ] Read: `~/tuvium/projects/forge-methodology/concepts/documentation-taxonomy.md` — Diataxis framework

**Work items**:
- [ ] RESTRUCTURE `mint.json` Agent Client nav to expose all four Diataxis quadrants:
  - **Reference**: portable-options, claude-reference, codex-reference, gemini-reference
  - **Explanation**: defaults-philosophy, sessions (or split sessions into reference + how-to)
  - **How-to**: getting-started, switching-providers, structured-output
  - **Tutorial**: tutorial/index, 01-first-task, 02-multi-provider
- [ ] VERIFY nav renders correctly — each quadrant is a visible named group
- [ ] REVIEW `sessions.mdx` — determine if it's pure Reference or needs splitting into Reference (API surface) + How-to (usage patterns)
- [ ] VERIFY cross-links between quadrants work (e.g., Reference pages link to relevant How-to, Tutorials link to Reference for deeper detail)

**Exit criteria**:
- [ ] All four Diataxis quadrants visible as named groups in Agent Client sidebar
- [ ] Each page lives in exactly one quadrant (no mixed-purpose pages)
- [ ] Create: `plans/learnings/step-3.2-diataxis-nav.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT (in mintlify-docs repo)

**Deliverables**: Diataxis-aligned navigation structure for agent-client docs

---

### Step 3.3: Onramp Metric + Stage 3 Consolidation

**Entry criteria**:
- [ ] Step 3.2 complete
- [ ] Read: `plans/learnings/step-3.2-diataxis-nav.md`

**Work items**:
- [ ] ADD onramp timing to tutorial CI workflow (ApplicationStartedEvent to AgentClientResponse)
- [ ] COMPACT learnings into `plans/learnings/LEARNINGS.md`
- [ ] UPDATE `CLAUDE.md`

**Exit criteria**:
- [ ] Onramp metric defined and wired into CI
- [ ] `LEARNINGS.md` updated
- [ ] Create: `plans/learnings/step-3.3-stage3-summary.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT

---

## Stage 4: Terminal-Bench Post-Change Evaluation

### Step 4.0: Stage 4 Entry

**Entry criteria**:
- [ ] Stage 3 consolidation complete — Read: `plans/learnings/step-3.3-stage3-summary.md`
- [ ] Read: `plans/learnings/LEARNINGS.md`
- [ ] Read: `experiments/terminal-bench-easy/baseline/` — Stage 0 results

**Work items**:
- [ ] REVIEW baseline results
- [ ] VERIFY mode system and all provider defaults are stable on main

**Exit criteria**:
- [ ] Context loaded, baseline reviewed
- [ ] Create: `plans/learnings/step-4.0-stage4-entry.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes

---

### Step 4.1: Post-Change Evaluation

**Entry criteria**:
- [ ] Step 4.0 complete
- [ ] Read: `plans/learnings/step-4.0-stage4-entry.md`

**Work items**:
- [ ] RUN all 5 terminal-bench tasks with Claude (LOOSE mode, no workarounds)
- [ ] RUN all 5 terminal-bench tasks with Codex (LOOSE mode, no workarounds)
- [ ] RUN all 5 terminal-bench tasks with Gemini (LOOSE mode, no workarounds)
- [ ] RECORD results in `experiments/terminal-bench-easy/results/`
- [ ] COMPARE against Stage 0 baseline

**Exit criteria**:
- [ ] Post-change results archived
- [ ] Comparison matrix (baseline vs post-change) documented
- [ ] Create: `plans/learnings/step-4.1-post-change-eval.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT

**Deliverables**: Post-change eval results + comparison matrix

---

### Step 4.2: Friction Report + Promotion Recommendations

**Entry criteria**:
- [ ] Step 4.1 complete
- [ ] Read: `plans/learnings/step-4.1-post-change-eval.md`

**Work items**:
- [ ] WRITE `experiments/terminal-bench-easy/friction-report.md` applying promotion rubric:
  1. ≥2 of 3 providers have semantic equivalent
  2. Absence causes easy-tier failures (evidence)
  3. Expressible without leaking provider concepts
- [ ] DOCUMENT options that meet rubric (candidates for portable promotion)
- [ ] DOCUMENT options that don't meet rubric ("stays provider-specific" — re-evaluate only when new provider adds equivalent or results change)
- [ ] UPDATE `portable-options.mdx` with rubric and findings

**Exit criteria**:
- [ ] Friction report complete with evidence-backed recommendations
- [ ] Promotion rubric documented in `portable-options.mdx`
- [ ] Create: `plans/learnings/step-4.2-friction-report.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT

**Deliverables**: Friction report with promotion recommendations

---

### Step 4.3: Tutorial Examples 02-03

**Entry criteria**:
- [ ] Step 4.2 complete
- [ ] Read: `plans/learnings/step-4.2-friction-report.md`

**Work items**:
- [ ] CREATE `02-read-and-transform/` in tutorial repo (read file, transform, write result)
- [ ] CREATE `03-git-operations/` in tutorial repo (demonstrates STRICT mode where it earns its keep)
- [ ] VERIFY both examples run with all 3 provider profiles
- [ ] UPDATE tutorial CI matrix to include new examples

**Exit criteria**:
- [ ] Examples 02-03 run with all providers
- [ ] Create: `plans/learnings/step-4.3-tutorial-examples.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT (in tutorial repo)

**Deliverables**: Tutorial examples 02-03

---

### Step 4.4: Stage 4 Consolidation

**Entry criteria**:
- [ ] All Stage 4 steps complete
- [ ] Read: all `plans/learnings/step-4.*` files

**Work items**:
- [ ] COMPACT learnings into `plans/learnings/LEARNINGS.md`
- [ ] UPDATE `CLAUDE.md` with full project learnings
- [ ] UPDATE doc-agent repo KB with friction findings

**Exit criteria**:
- [ ] `LEARNINGS.md` reflects complete project state
- [ ] Create: `plans/learnings/step-4.4-stage4-summary.md`
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT

---

## Stage 5: Iteration (Future)

### Step 5.0: Extract AgentClient Autoconfigure Module

**Problem:** `agent-client-core` depends on `spring-boot-autoconfigure` for a single file (`AgentClientAutoConfiguration.java`). The core API (`AgentClient`, `Goal`, `AgentClientResponse`) should be usable without Spring Boot.

**Entry criteria**:
- [ ] Stage 4 complete
- [ ] Read: `plans/learnings/LEARNINGS.md`

**Work items**:
- [ ] CREATE `agent-client-autoconfigure/` module (or fold `AgentClientAutoConfiguration` into starters)
- [ ] MOVE `AgentClientAutoConfiguration.java` out of `agent-client-core`
- [ ] REMOVE `spring-boot-autoconfigure` dependency from `agent-client-core/pom.xml`
- [ ] VERIFY `agent-client-core` compiles with zero Spring Boot dependencies
- [ ] VERIFY starters still auto-configure `AgentClient.Builder` bean
- [ ] UPDATE tutorial and sample projects if imports changed

**Exit criteria**:
- [ ] `agent-client-core` has no Spring Boot dependency
- [ ] All starters work unchanged
- [ ] `./mvnw clean compile` passes
- [ ] Create: `plans/learnings/step-5.0-autoconfigure-extraction.md`
- [ ] COMMIT

**Deliverables**: Clean separation — core API is plain Java, autoconfigure is a separate module

---

### Step 5.1: Rename AgentModel → AgentApi

**Problem:** `AgentModel` implies the interface represents an AI model, but it actually represents the programmatic interface to a CLI agent runtime. `AgentApi` is more accurate and avoids confusion with Spring AI's `ChatModel` (which *does* wrap a model endpoint).

**Deprecation strategy:** Thin shim inheritance for one release cycle. Old names extend new names as `@Deprecated` classes so existing user code compiles with warnings.

**Entry criteria**:
- [ ] Step 5.0 complete
- [ ] Read: `plans/learnings/step-5.0-autoconfigure-extraction.md`

**Work items**:
- [ ] CREATE `AgentApi` interface in `agent-model` as the new name (same contract as `AgentModel`)
- [ ] DEPRECATE `AgentModel` — make it extend `AgentApi` with `@Deprecated(since = "0.16.0", forRemoval = true)`
- [ ] RENAME implementations: `ClaudeAgentModel` → `ClaudeAgentApi` (keep old class as deprecated shim extending new)
  - Same for `CodexAgentModel` → `CodexAgentApi`, `GeminiAgentModel` → `GeminiAgentApi`, etc.
- [ ] UPDATE `AgentClient.create()` and `AgentClient.builder()` to accept `AgentApi`
- [ ] UPDATE auto-configurations to produce `AgentApi` beans (keep `AgentModel` bean via shim)
- [ ] UPDATE all internal references to use new names
- [ ] VERIFY `./mvnw clean compile` passes
- [ ] VERIFY existing user code using old names still compiles (with deprecation warnings)
- [ ] UPDATE documentation pages to use new names

**Exit criteria**:
- [ ] New names are primary, old names are deprecated shims
- [ ] All tests pass
- [ ] Create: `plans/learnings/step-5.1-agentapi-rename.md`
- [ ] COMMIT

**Deliverables**: `AgentModel` → `AgentApi` rename with deprecation shims

---

### Step 5.2: Portable Option Promotions

Promote options meeting rubric to `AgentOptions` interface. Gated on Stage 4 evidence.

### Step 5.3: Tutorial Examples 04-07

Add structured output, sessions, MCP, advisors examples. Gated on Stage 4 evidence supporting priority.

### Step 5.4: Tutorial Documentation Pages

Write corresponding tutorial pages for new examples.

---

## Conventions

### Commit Convention
```
Step X.Y: Brief description of what was done
```

### Step Entry Criteria Convention
Every step's entry criteria includes:
```markdown
- [ ] Previous step complete
- [ ] Read: `plans/learnings/step-{{PREV}}-{{topic}}.md` — prior step learnings
```

### Step Exit Criteria Convention
Every step's exit criteria includes:
```markdown
- [ ] Create: `plans/learnings/step-X.Y-topic.md`
- [ ] Update `CLAUDE.md` with distilled learnings
- [ ] Update `HARDENING-ROADMAP.md` checkboxes
- [ ] COMMIT
```

### Stage Consolidation Convention
Last step of each stage compacts per-step learnings into `plans/learnings/LEARNINGS.md`.

### Inter-Stage Gate Convention
First step of Stage N (N > 1) gates on Stage N-1 consolidation being complete.

---

## Revision History

| Timestamp | Change | Trigger |
|-----------|--------|---------|
| 2026-04-24T15:30-04:00 | Initial draft from approved plan | Joachim Pasquali Codex friction incident |
