# Step 4.3 Learnings: Tutorial Examples 02-03

## What Was Built

In `spring-ai-community/agent-client-tutorial`:

### 02-read-and-transform
- Reads log files (auth.log, api.log, worker.log) and counts ERROR/WARNING/INFO lines
- Writes summary.csv with severity counts
- Creates sample log files if they don't exist
- Derived from terminal-bench `log-summary` task

### 03-git-operations  
- Finds lost git changes on a detached HEAD and merges them into master
- `setup.sh` creates the git repo with "lost" commit
- Uses `mode: strict` in application.yml — demonstrates when STRICT mode is appropriate
- Derived from terminal-bench `fix-git` task

### Version Update
- `agent-client.version` updated from `0.13.0` to `0.15.0`
- Codex profile config updated to `gpt-5.4-mini`

## Design Decisions

- Tutorial examples use Spring Boot (CommandLineRunner pattern) since that's the tutorial repo's convention
- The getting-started docs show plain Java; the tutorial repo shows Spring Boot
- 03-git-operations demonstrates STRICT mode with `spring.ai.agents.mode: strict` in config
