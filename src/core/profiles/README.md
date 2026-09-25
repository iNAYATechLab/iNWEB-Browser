# `src/core/profiles` — Profiles Core (pure JVM)

The profile model behind MASTER-SPEC §28 (Phase 9 design:
`docs/PHASE9-PROFILES-SYNC-DESIGN.md` §1): profile records,
active-profile selection, and per-profile store routing.

**Pure JVM, no Android dependency** — compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit); the
`ui/` patch (0023) binds namespaces to real per-profile Chromium
user-data directories (build infrastructure, B-001).

## Components

| Component | Purpose |
|---|---|
| `ProfileRecord` | id (`p-N`, monotonic, NEVER reused), name, creation time |
| `ProfileRegistry` | Seeding (a real browser always starts with one default profile); create (inactive) / rename / setActive / delete; `namespaceOf(id)` — the store-routing contract |
| `ProfileStore` / `InMemoryProfileStore` | Persistence seam (the file-backed store ships with the patch binding) |

## Isolation contract (ADR-026)

- A store namespace is the profile id; the app layer maps it to
  `<dataDir>/profiles/<id>/`. Every iNWEB-owned store is constructed
  against exactly ONE namespace — a profile sharing storage with
  another cannot occur (§28).
- Deleting the ACTIVE profile is allowed: active falls back to the
  oldest remaining profile and the caller tears down the browsing
  surface; the deleted profile's data directory is retired by the
  caller (the registry reports it).
- The last remaining profile cannot be deleted.
- Expected failures are `ProfileResult.Err` values, never exceptions.
