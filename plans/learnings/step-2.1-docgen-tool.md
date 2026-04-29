# Step 2.1 Learnings: Reference Doc Generation Tool

## What Was Built

`tools/agent-options-docgen/` — standalone Maven module (not in parent POM) that reads `*Properties.java` source files using QDox and emits Markdown reference tables.

### Usage
```bash
# From project root:
./mvnw -f tools/agent-options-docgen/pom.xml compile exec:java \
  -Dexec.args="/path/to/agent-client [output-dir]"
```

### Output
Generates one `.md` file per provider with property tables:
- `claude-reference.md` — 18 properties under `spring.ai.agents.claude-code`
- `codex-reference.md` — 6 properties under `spring.ai.agents.codex`
- `gemini-reference.md` — 6 properties under `spring.ai.agents.gemini`

## Design Decisions

### QDox over reflection
Reflection cannot access Javadoc comments (stripped at compile time). QDox parses source files directly, extracting field names, types, initializers, and Javadoc in one pass. Lightweight — single dependency.

### Standalone module
Not added to parent POM `<modules>` list. Development tool only, not published to Central. Run via `./mvnw -f tools/agent-options-docgen/pom.xml`.

### Inline tables over Snippet includes
Generated Markdown goes directly into reference .mdx pages rather than using Mintlify `<Snippet>` includes. Simpler workflow — regenerate and update pages when properties change.

## What Carries Forward

- Run the tool after adding/changing properties to update reference docs
- The tool auto-discovers: field name → kebab-case property name, field type, default value from initializer, first sentence of Javadoc
- `tools/agent-options-docgen/output/` contains the latest generated tables (gitignored via tool output)
