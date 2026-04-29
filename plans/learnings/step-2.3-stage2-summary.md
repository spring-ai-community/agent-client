# Step 2.3 Learnings: Stage 2 Summary

## What Stage 2 Delivered

### Docgen Tool (`tools/agent-options-docgen/`)
- Standalone Maven module using QDox to parse `*Properties.java` source files
- Extracts field names, types, defaults, Javadoc → Markdown reference tables
- Run: `./mvnw -f tools/agent-options-docgen/pom.xml compile exec:java -Dexec.args="<project-root> [output-dir]"`
- Not in parent POM modules list — development tool only

### Reference Pages (4 pages in mintlify-docs)
| Page | Properties Covered |
|------|--------------------|
| `portable-options.mdx` | AgentOptions interface, precedence rules, mode system, promotion rubric |
| `claude-reference.mdx` | 18 properties (13 standard + 5 advanced), permission modes, tool filtering, budget, auth |
| `codex-reference.mdx` | 6 properties, skipGitCheck/mode interaction truth table, migration note |
| `gemini-reference.mdx` | 6 properties, temperature guidance, mode no-op explanation |

### Navigation Update
Added `Reference` subgroup under Agent Client in `mint.json` with all 4 pages.

## Design Decisions

1. **QDox over reflection** — Javadoc comments are stripped at compile time; QDox parses source directly.
2. **Inline tables, not Snippet includes** — Simpler maintenance. One file to update when properties change.
3. **Claude standard/advanced split** — 18 properties is too many for one table. Split at the Python SDK parity boundary.
4. **Standalone Maven module** — Not added to parent POM. Avoids polluting the build and Central publishing.

## Open Issues (carried forward)

1. Codex `@DisabledIfEnvironmentVariable(CI=true)` — still active
2. OPENAI_API_KEY secret — not yet added to CI repos
3. Tutorial CI — still compile-only
4. `mintlify dev` verification — pages not tested with dev server (no Mintlify CLI available)

## What Carries Forward to Stage 3

- Reference pages exist and can be cross-linked from how-to and getting-started pages
- The `portable-options.mdx` promotion rubric is the canonical reference for option promotion decisions
- Docgen tool can regenerate tables when properties change (run before docs PRs)
