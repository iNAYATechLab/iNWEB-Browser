# `src/core/sync` — Sync Queue Core (pure JVM)

The transport-agnostic sync queue model behind MASTER-SPEC §30 (Phase 9
design: `docs/PHASE9-PROFILES-SYNC-DESIGN.md` §3).

> **Future infrastructure — NOT a feature (§29).** No sync backend
> exists. Nothing in the product may present this module as cloud sync;
> no UI claims synchronization. The model exists so a future backend can
> be added without redesigning the data layer, and so its correctness is
> testable today against a fake transport.

**Pure JVM, no Android dependency** — compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit).

## Components

| Component | Purpose |
|---|---|
| `SyncEngine` | Durable append-only change log with monotonic revisions (`record`), batch flush with retry/backoff gating (`attemptFlush`), and explicit failure reporting (`failedChanges`) — nothing is silently dropped |
| `SyncTransport` | The backend seam; tests use a scriptable fake |
| `RetryPolicy` | Exponential backoff (capped) with a maximum attempt count; exhausted retries move the batch to FAILED with a reason |
| `ConflictResolver` | Last-writer-wins with tombstones: later timestamp wins (a newer upsert legitimately beats an older tombstone and vice versa); exact ties resolve to the LOCAL entry — deterministic, no ping-pong; **no CRDTs are claimed** |
| `SyncQueueStore` / `InMemorySyncQueueStore` | Persistence seam (§30 durable queue survives restarts at the app layer) |

## §30 rules enforced in code (ADR-026)

- Local changes and queued changes are one append-only log; revisions
  are monotonic and never reused.
- Retry with backoff; failure recovery = replay of the durable queue.
- Deletes are recorded tombstones (auditable), never silent drops.
- Integrity and secure transport belong to the future real backend
  (§29); this core makes no claims about either.
