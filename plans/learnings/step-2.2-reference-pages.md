# Step 2.2 Learnings: Reference Pages

## What Was Built

4 reference pages in `~/community/mintlify-docs/agent-client/`:

| Page | Content |
|------|---------|
| `portable-options.mdx` | AgentOptions interface, precedence rules, mode system, promotion rubric, provider-specific vs portable classification |
| `claude-reference.mdx` | 18 properties (split into standard + advanced), permission modes, tool filtering, budget controls, structured output, auth |
| `codex-reference.mdx` | 6 properties, skipGitCheck/mode interaction table, migration note, full-auto explanation, auth |
| `gemini-reference.mdx` | 6 properties, yolo mode, temperature guidance, mode no-op explanation, auth |

Updated `mint.json` to add a **Reference** subgroup under Agent Client with all 4 pages.

## Design Decisions

### Separate standard and advanced tables for Claude
Claude has 18 properties — too many for one table. Split into "Configuration Properties" (13 common options) and "Advanced Options" (7 Python SDK parity options). Advanced options are clearly labeled for power users.

### skipGitCheck interaction table for Codex
The skipGitCheck/mode interaction is the most confusing part of Codex config. Dedicated 4-row truth table showing every combination of explicit vs mode-derived values.

### No Snippet includes
Generated content pasted inline rather than using Mintlify `<Snippet>` components. Simpler — one place to edit, no cross-file dependencies. The docgen tool serves as a development aid to regenerate when properties change.

## Navigation Structure

```
Agent Client
├── Overview (projects/incubating/agent-client)
├── Defaults Philosophy
├── Sessions
└── Reference
    ├── Portable Options
    ├── Claude Reference
    ├── Codex Reference
    └── Gemini Reference
```
