# Phase 11 — Notifications & Advanced Features Design

**Status:** authored design (Step 30). Runtime pieces land as pure-JVM
cores (CI-tested) plus `ui/`/`settings/` patch entries generated against
the pinned baseline `154.0.8037.21` on build infrastructure (blocker
B-001).
**Governing requirements:** MASTER-SPEC §33 (notifications), §34
(offline games / entertainment — optional), §23 (customization);
§41's privacy-first rule binds the notification policy; §7 (base APK
size) binds the entertainment decision.

---

## 1. §33 Notifications — privacy-first, minimal by default

**Policy (ADR-028):** notifications exist to report REAL events the user
acted on — never promotion, never engagement bait, never telemetry in
disguise (§41). Every channel is off-able; Android 13+
`POST_NOTIFICATIONS` is requested lazily at the first real notification,
not at startup.

### v1 channel plan (each backed by a real event source)

| Channel | Events | Source today? |
|---|---|---|
| Downloads | download complete / failed | real — `DownloadsStore` state machine (Phase 2 core) |
| Security | backup failed (integrity/encryption), app-lock repeated failures | real when Phase 8/9 patches land |
| VPN | connected / disconnected / reconnecting | real when `0022` lands — absent until then, NO stub channel |
| Background | offline page saved, filter-list update failed | offline: real with `0019`; list-update failures are silent-by-design EXCEPT a persistent failure surfaced in the Security Center |

### Explicitly absent (honest)

- **Sync notifications:** no sync exists (§29, ADR-026) — no channel, no
  stub.
- **Update notifications:** iNWEB has no self-update infrastructure;
  updates come from wherever the user obtained the build — claiming an
  in-app update service would be false.
- **Any promotional/usage nudge:** forbidden by policy.

**Core (implemented — Step 32):** the pure-JVM
`NotificationPolicy` model in `src/core/notifications` — event →
channel registry (this table, audited by its unit test), per-channel
user enabled/disabled, lazy permission-requested state machine; the
Android binding (channels, permissions) is patch-side.

## 2. §34 Offline games / entertainment — documented non-goal for v1

The entertainment module is OPTIONAL by the spec's own wording. iNWEB
v1 ships **none of it**: the base APK must stay lean (§7), and nothing
here may pretend otherwise. If ever built, the binding constraints are
already fixed: a **separate, independently installable component**
(dynamic-feature-module style where the distribution channel supports
it; a separate artifact otherwise), zero core-browser coupling, and its
own design step with APK-size accounting. Nothing in the browser core
references or depends on it.

## 3. §23 Customization — real levers, mapped honestly

| Lever | Status / mechanism |
|---|---|
| Toolbar configuration | authored bottom bar today; **user-reorderable/hideable item set = Step 31 core — implemented `src/core/customization` (25 tests, ADR-029)** |
| Navigation controls | back/forward/reload in the bottom bar (live in authored UI) |
| Icons / theme | Material 3 tokens; light/dark/system theme (authored UI) |
| Font / text size | system font scale + in-app text-size setting; §49 contracts apply |
| Page zoom | **core implemented — `ZoomPreferences`/`ZoomSettings` (Step 34, ADR-032)**; bound to Chromium's Android page-zoom setting via a `settings/` patch (real upstream mechanism, not a custom renderer hack) |
| Homepage / new-tab layout | the Phase 2 home surface — top sites + shortcuts (ADR-017) |
| Search engine | real — `SearchEngine` model (Phase 2 core), user-selectable |
| Privacy defaults | real — tracking-protection settings, per-site allowlist (Phase 3 core) |
| Download preferences | **core implemented — `DownloadPreferences`/`DownloadSettings` (Step 34, ADR-032)**; ask-before-download / default folder via `settings/` patch |

Everything above is an existing Chromium/iNWEB mechanism or a designed
core — no lever is listed that has no mechanism behind it.

## 4. Patch-plan impact

- `settings/` future entries: page-zoom binding, download preferences,
  notification channels + permission flow
- `ui/` future entry: toolbar configuration surface (bound to the
  Step 31 core)
- No entertainment entry exists — there is nothing to patch (§34)

## 5. Verification strategy

1. **Core (live):** toolbar-configuration and notification-policy unit tests (Steps 31–32).
2. **Build-time:** notification channel registration matches §1 exactly
   (a scriptable audit against the channel table); no extra channels.
3. **On device (B-001):** no notification without a real event; every
   channel toggle works; permission requested lazily; airplane-mode
   download failure notifies honestly; zero promotional notifications
   ever.

## 6. Honest boundaries

- No notification, zoom binding, or toolbar setting exists on a device
  until the patches build (B-001); cores are real and tested as soon as
  they land.
- No sync/update/promotional notifications — stated as absent, not
  stubbed.
- The entertainment module is a documented non-goal for v1; any future
  version needs its own design with APK-size accounting first.
