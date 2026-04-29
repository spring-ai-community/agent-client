# Step 3.0 Learnings: Stage 3 Entry

## Context Loaded

- Stage 2 complete — docgen tool + 4 reference pages committed
- Tutorial repo at `~/community/agent-client-tutorial/` references `agent-client.version=0.13.0`
- Tutorial `01-create-file/` has the portable pattern: inject `AgentClient.Builder`, call `.run(goal)`
- Sample `create-file-multi-provider/` is identical pattern

## API Surface for Documentation

Key APIs to document in getting-started and how-to:

- `AgentClient.builder(agentModel).build()` — programmatic
- `AgentClient.Builder` auto-configured bean — Spring Boot way
- `.run(goalText)` — simple convenience method
- `.goal(text).workingDirectory(path).run()` — fluent spec
- `.goal(text).mcpServers("name").run()` — MCP selection
- Maven profiles for provider switching (claude/codex/gemini)

## No Blockers

Reference pages accurate. Tutorial repo compiles against 0.13.0.
