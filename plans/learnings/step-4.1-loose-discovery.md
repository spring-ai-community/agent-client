# Step 4.1 Learnings: LOOSE Permission Discovery

## Discoveries

### 1. Codex bwrap sandbox fails in many environments
`--full-auto` sets `workspace-write` sandbox which uses bubblewrap (`bwrap`). Fails with `bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted` in environments without network namespace privileges. Terminal-bench independently solved this by using `--sandbox danger-full-access`.

**Fix**: LOOSE mode now derives `dangerouslyBypassSandbox=true` for Codex.

### 2. Codex model `gpt-5-codex` no longer valid
Codex CLI 0.125.0 doesn't recognize `gpt-5-codex`. The `--model gpt-5-codex` flag caused the CLI to print "Reading additional input from stdin..." and exit without executing.

**Fix**: Default model updated to `gpt-5.4-mini`.

### 3. Codex CLI needs `--` separator and stdin redirect
Without `--` before the prompt, complex prompts can be misinterpreted as flags. Without stdin redirect (`ByteArrayInputStream(new byte[0])`), Codex blocks waiting for stdin when run from Maven surefire.

**Fix**: Added both to `CLITransport.buildCommand()` and `executeCommand()`.

### 4. Gemini working directory not propagated to CLI process
`GeminiAgentModel.call()` passed the request working directory in the prompt text ("You are working in directory: ...") but not to the actual `ProcessExecutor.directory()` call. The transport always used the directory set at construction time (`System.getProperty("user.dir")`).

**Fix**: Added `workingDirOverride` parameter to `CLITransport.executeQuery()` and `GeminiClient.query()`.

## Terminal-Bench Easy Tier Results

### Direct CLI (before AgentClient fixes)

| Task | Claude | Codex (full-auto) | Codex (bypass) | Gemini |
|------|--------|-------------------|----------------|--------|
| hello-world | PASS | FAIL (bwrap) | PASS | PASS |
| fix-git | PASS | — | PASS | PASS |
| log-summary | PASS | — | FAIL (wrong counts) | PASS |
| fix-permissions | PASS | — | PASS | PASS |
| heterogeneous-dates | PASS | — | PASS | PASS |

### Via AgentClient (after fixes)

| Task | Claude | Codex | Gemini |
|------|--------|-------|--------|
| hello-world | PASS | PASS | PASS |

## Terminal-Bench Reference

Terminal-bench uses these flags per provider:
- **Claude**: `--allowedTools Bash,Edit,Write,...` (explicit tool list)
- **Codex**: `--sandbox danger-full-access --skip-git-repo-check`
- **Gemini**: `-y` (yolo)

## LOOSE Derivation Table (Final)

| Provider | Option | LOOSE Value | STRICT Value | Rationale |
|----------|--------|-------------|--------------|-----------|
| Codex | `skipGitCheck` | `true` | `false` | Non-git directory gate |
| Codex | `dangerouslyBypassSandbox` | `true` | `false` | bwrap sandbox fails in many environments |
| Claude | `yolo` | `true` (already default) | — | No new derivation needed |
| Gemini | `yolo` | `true` (already default) | — | No new derivation needed |
