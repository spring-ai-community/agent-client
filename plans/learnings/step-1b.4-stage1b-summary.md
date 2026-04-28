# Step 1b.4 Learnings: Stage 1b Summary

## What Stage 1b Delivered

### Track 1b: Multi-Provider Sample
- `samples/create-file-multi-provider/` — Maven profiles for Claude/Codex/Gemini
- Portable Java code (no provider imports), profile-specific YAML
- Commit: `a21e029` (bundled with Stage 1a mode work)

### Track 2: Tutorial Repository
- `spring-ai-community/agent-client-tutorial` — public GitHub repo
- `01-create-file/` example with all 3 provider profiles
- `CLAUDE.md` documenting structure convention for contributors
- MWF CI workflow (compile-only until secrets configured)
- References `agent-client.version=0.13.0` (latest release)

### Coordination Repo
- `tuvium/agent-client-doc-agent` — private Tuvium repo
- Steward CLAUDE.md, knowledge index, baseline eval results
- Required `gh auth switch --user mark-tuvium` (documented in memory)

## Open Issues

1. **Codex `@DisabledIfEnvironmentVariable(CI=true)`** — still prevents parity tests in CI (carried from Stage 1a)
2. **OPENAI_API_KEY secret** — needs adding to agent-client and tutorial repos
3. **Tutorial CI runtime** — compile-only until secrets configured
4. **Personal markpollack/agent-client-doc-agent** — leftover repo from failed Tuvium creation attempt. Can be deleted when delete_repo scope is available.

## What Carries Forward to Stage 2

- Reference docs should link to tutorial repo and samples
- The `defaults-philosophy.mdx` page is already published — reference pages link to it for precedence rules
- Tutorial repo structure convention in CLAUDE.md is the template for future examples
