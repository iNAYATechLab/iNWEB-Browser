# `src/core/offline` — Offline Library Core (pure JVM)

The saved-page registry behind MASTER-SPEC §17 (Phase 7 design:
`docs/PHASE7-OFFLINE-DESIGN.md`): records with REAL byte sizes,
quota accounting, LRU eviction, and a persistence seam.

**Pure JVM, no Android dependency** — it compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit); the
`offline/` patches (0018+) bind it to the MHTML snapshot pipeline
(build infrastructure, B-001).

## Components

| Component | Purpose |
|---|---|
| `OfflinePageRecord` | One saved page: online URL (key), title, snapshot file, real byte size, created/last-accessed timestamps |
| `OfflineLibrary` | Insertion-order registry; `save` enforces the quota by evicting least-recently-accessed pages first (never pinned pages, never the record being saved; atomic on quota failure); same-URL saves REPLACE; `access` (monotonic), `delete`, real byte accounting |
| `OfflineStore` / `InMemoryOfflineStore` | Persistence seam (the file-backed store ships with the patch binding) |

## Design invariants (ADR-024)

- Snapshots are **user data** — this class never touches HTTP cache
  storage; `clear cache` cannot delete saved pages by construction.
- Sizes are real values from the snapshot pipeline — never estimated.
- Eviction returns the evicted records; the CALLER deletes their files
  (the core never performs I/O beyond the store seam).
- Expected failures are `LibraryResult.Err` values, never exceptions.
