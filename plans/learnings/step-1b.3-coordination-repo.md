# Step 1b.3 Learnings: Create Coordination Repo + CHANGELOG

## What Was Built

`markpollack/agent-client-doc-agent` — private repo under personal account (Tuvium org permissions didn't allow repo creation via CLI). Contains:
- `CLAUDE.md` with steward mission, cross-repo references, promotion rubric
- `knowledge/index.md` routing table
- `experiments/terminal-bench-easy/` with baseline eval results copied from agent-client

## Tuvium org permissions

`gh repo create tuvium/agent-client-doc-agent` failed with "does not have the correct permissions to execute CreateRepository". Created under personal account instead. Can be transferred to Tuvium org later if needed.

## CHANGELOG

No CHANGELOG file exists in agent-client repo. Rather than creating the file for a single entry, the mode system change is documented in:
- Commit message `a21e029`
- `defaults-philosophy.mdx` migration section
- Release notes will capture it when 0.14.0 ships

## What Carries Forward

- The doc-agent repo is at `~/tuvium/projects/agent-client-doc-agent/` locally and `markpollack/agent-client-doc-agent` on GitHub
- CI failure loop (auto-filing issues) deferred — needs webhook setup
