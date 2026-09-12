# `src/core/notifications` — Notification Policy Core (pure JVM)

The §33 notification policy model (Phase 11 design:
`docs/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md` §1, ADR-028):
the channel/event registry, per-channel user toggles, real-event-only
decisions, and the lazy `POST_NOTIFICATIONS` permission state machine.

**Pure JVM, no Android dependency** — compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit); the
`settings/`+`ui/` patch bindings create the real Android channels,
fire real events, and persist preferences (build infrastructure,
B-001).

## Components

| Component | Purpose |
|---|---|
| `NotificationChannel` / `NotificationEvent` | The v1 registry — EXACTLY the design §1 table (4 channels, 8 events); the unit test is the scriptable audit ("registration matches §1, no extra channels") |
| `NotificationPolicy.decide` | The ONLY notify path — a typed real event in, one of Show / RequestPermission / Suppress(reason) out; there is no generic `notify(channel, text)` API, so promotional content has no way in |
| `PermissionPhase` | NOT_REQUESTED → REQUESTED → GRANTED/DENIED; the ask fires lazily at the first show-worthy event, never at startup, never twice, never after denial |
| `NotificationStore` / `InMemoryNotificationStore` | Persistence seam (per-channel toggles + permission phase) |

## Contract (ADR-028/029 lineage)

- **Real events only:** the event set is the design §1 table; sync,
  self-update, and promotional events are structurally absent (no
  enum value, no code path) — stated, never stubbed. Filter-list
  update failures are silent by design and surface in the Security
  Center, not as notifications.
- **Availability is explicit:** the build states which event sources
  are real (`availableChannels`, required — no default). An
  unavailable channel is never registered and its events are always
  suppressed (`CHANNEL_NOT_AVAILABLE`) — e.g. the VPN channel is
  absent until patch `0021` lands.
- **Check order (fixed):** availability → user toggle → permission.
  The lazy ask fires ONLY for an event that would otherwise be shown.
- **Every channel is off-able** (§33); defaults are all-on for the
  tiny real-event set.
- **No nagging:** a denial is terminal in-app; only an observed
  system-setting change (`onSystemPermissionChanged`) can reverse it.
- **Corrupt stored preferences** recover to defaults with the repair
  persisted and reported via `lastRecovery()` — never a crash, never
  a silent ignore.
