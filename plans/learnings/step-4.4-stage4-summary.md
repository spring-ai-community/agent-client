# Step 4.4 Learnings: Stage 4 Summary

## What Stage 4 Delivered

### LOOSE Permission Discoveries (4 bugs fixed)
1. **Codex sandbox bypass** — `dangerouslyBypassSandbox=true` for LOOSE (bwrap fails in many environments)
2. **Codex model default** — `gpt-5-codex` → `gpt-5.4-mini` (old name removed in CLI 0.125.0)
3. **Codex CLI transport** — `--` separator + stdin redirect for reliable prompt delivery
4. **Gemini working directory** — `workingDirOverride` param added to propagate request dir to CLI process

### LOOSE Derivation Table
| Provider | Option | LOOSE | STRICT |
|----------|--------|-------|--------|
| Codex | `skipGitCheck` | `true` | `false` |
| Codex | `dangerouslyBypassSandbox` | `true` | `false` |
| Claude | `yolo` | `true` (default) | — |
| Gemini | `yolo` | `true` (default) | — |

### Integration Test
`LoosePermissionIT` in `agent-client-core` validates hello-world across all 3 providers via AgentClient.

### Tutorial Examples 02-03
- `02-read-and-transform/` — log file processing
- `03-git-operations/` — git task with STRICT mode

### 0.15.0 Release
Published to Maven Central during this stage. Tutorial repo updated to reference 0.15.0.

## Known Gaps (tracked in Stage 5)
- Global `spring.ai.agents.mode` not implemented (only per-provider works)
- No CLI arg validation tests
- No daily CI against latest CLI versions
- Terminal-bench tasks not yet permanent TCK resources
