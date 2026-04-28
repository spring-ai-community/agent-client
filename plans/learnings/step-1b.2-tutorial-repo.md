# Step 1b.2 Learnings: Create Tutorial Repository

## What Was Built

`spring-ai-community/agent-client-tutorial` — public GitHub repo with:
- Parent POM referencing `agent-client.version=0.13.0` (latest release)
- `01-create-file/` module with Maven profiles (claude/codex/gemini)
- `CLAUDE.md` documenting structure convention for contributors
- `.github/workflows/examples.yml` (MWF schedule, compile-only for now)
- Maven wrapper installed

## Design Decisions

### Version alignment
Tutorial references `0.13.0` (latest released) not `0.14.0-SNAPSHOT`. Users clone the tutorial and it should work against published artifacts, not local snapshots.

### CI runtime tests commented out
The workflow compiles all examples across all provider profiles but doesn't run them yet. Runtime execution is commented out until repo secrets are configured. This is intentional — compile verification catches dependency and API-change regressions without incurring API costs on every PR.

### Maven wrapper
Added via `mvn wrapper:wrapper -Dmaven=3.9.11`. Tutorial repo is standalone — users shouldn't need Maven pre-installed.

## What Carries Forward

- When 0.14.0 releases, update `agent-client.version` in the parent POM
- When CI secrets are added, uncomment the `Run example` step in the workflow
- Future examples (02-07) follow the pattern documented in CLAUDE.md
