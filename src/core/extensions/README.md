# `src/core/extensions` — Extension Management Core (pure JVM)

The management model behind MASTER-SPEC §16 (Phase 6 design:
`docs/PHASE6-EXTENSION-DESIGN.md` §4): the install / review / enable /
disable / update / remove state machine, Chrome-style version
comparison for sideload updates, and permission-review records.

**Pure JVM, no Android dependency** — it compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit), and the
Chromium `extension/` patches bind this model to the real WebExtensions
runtime (registry entries 0014–0017, blocked on build infrastructure
B-001).

## Components

| Component | Purpose |
|---|---|
| `ExtensionVersion` | Chrome version syntax (1–4 dot-separated integers), numeric comparison with zero padding (`1.0 == 1.0.0`, `1.2 < 1.10`) |
| `ExtensionRecord` / `ManifestVersion` | Identity + permission set of a sideloaded extension; MV3 primary, MV2 on the stated grace window (ADR-023) |
| `ExtensionState` | `PENDING_REVIEW` → (review) → `DISABLED` ⇄ `ENABLED`; updates adding unreviewed permissions force `DISABLED_UPDATE` |
| `ExtensionRegistry` | The state machine — no silent installs, no implicit permission grants, no enable without review; every change persisted through the store seam |
| `ExtensionStore` / `InMemoryExtensionStore` | Persistence seam (the file-backed store ships with the patch binding) |

## Honest boundaries

- Nothing here installs or runs a real extension — the Chromium-side
  installer, management UI, and runtime are the `extension/` patch
  series (B-001).
- Expected failures are `RegistryResult.Err` values, never exceptions
  and never fake success (§16: no pretending an unsupported operation
  worked).
