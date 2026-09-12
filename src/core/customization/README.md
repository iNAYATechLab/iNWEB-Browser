# `src/core/customization` — Customization Core (pure JVM)

The toolbar-configuration model behind MASTER-SPEC §23 (Phase 11
design: `docs/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md` §3): the
user-reorderable / hideable bottom-bar item set.

**Pure JVM, no Android dependency** — compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit); the
`ui/` patch binds the model to the real bar and persists the
configuration in preferences (build infrastructure, B-001).

## Components

| Component | Purpose |
|---|---|
| `ToolbarItem` | The item universe — mirrors the authored bottom bar EXACTLY (back, forward, home, tabs, menu); stable string ids; `mandatory` marks the structural controls (back / tabs / menu) that can never be hidden |
| `ToolbarEntry` / `ToolbarConfig` | One slot (item + visibility) and the validated configuration (every item exactly once, no mandatory item hidden) |
| `ToolbarConfig.parse` | Validates untrusted stored data — fixed check order (duplicates → unknown → missing → unknown-hidden → mandatory-hidden), every offender reported |
| `ToolbarStore` / `InMemoryToolbarStore` | Persistence seam (wire form = stable ids; the preference-backed store ships with the ui/ patch binding) |
| `ToolbarConfigurator` | `move` (remove-then-insert, full-list index), `setVisible` (hide optional / show at stored position), `reset`; every mutation validated + persisted |

## Contract (ADR-029)

- **No invented items:** the universe is the authored bar as shipped;
  adding an item is a code change that ships with a stored-config
  migration.
- **Strict validation:** duplicate / unknown / missing / unknown-hidden
  / mandatory-hidden are hard errors — nothing is silently dropped or
  invented to "fix" a corrupt file.
- **Corrupt stored config → authored default:** the fallback is
  persisted and reported via `lastRecovery()` — never a crash, never a
  silent ignore.
- **Mandatory items stay visible:** back (navigation escape), tabs
  (the only session surface), and menu (the only entry to
  settings/downloads/history/bookmarks) cannot be hidden by the user
  or by stored data.
- **Hidden keeps its slot:** a hidden item keeps its position in the
  full order, so showing it restores it where the user left it.
