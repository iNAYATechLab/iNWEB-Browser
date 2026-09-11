# iNWEB Browser — System Architecture

**Status:** Phase 0 baseline architecture; living document, updated each phase.
**Governing constraints:** `MASTER-SPEC.md` §2, §5–§39, §71.

---

## 1. Architectural identity

```text
UPSTREAM CHROMIUM (pinned Android stable tag, pristine)
        ↓  gclient sync @ tag
iNWEB TRACKED PATCH SERIES (iNWEB_PATCHES/, ordered + registered)
        ↓  privacy / security / adblock / popup / extension / performance / ui / offline / settings
PATCHED CHROMIUM TREE
        ↓  GN build target: inweb_public_apk (derived from upstream chrome_public_apk)
CUSTOM iNWEB ANDROID BROWSER (Kotlin + Material 3 application layer)
        ↓
iNWEB BROWSER (development → beta → stable channels)
```

- **Engine:** upstream Chromium — Blink, V8, the Chromium network stack (HTTP/2,
  HTTP/3/QUIC, TLS 1.3), GPU compositing, and site-isolated multi-process sandboxing.
- **Application layer:** the iNWEB Android application is integrated as a first-class
  Android build target inside the Chromium build (the integration model used by shipped
  Chromium-derived browsers), written in Kotlin with a Material 3 design system.
- **Never** an Android WebView host (§2, §71).

## 2. Process model

- Inherited upstream Chromium multi-process architecture: browser process, per-site
  sandboxed renderer processes, GPU process, and utility/network service processes.
- Android lifecycle integration: renderer reclamation under memory pressure, tab
  freezing, foreground/background scheduling policies (feeds §21 Energy Saver and §52).
- iNWEB patches must not weaken sandboxing or site isolation; security review is a gate
  for any patch touching the process model, IPC, or permissions.

## 3. Patch areas → feature map

| Patch area | Carries (master spec) |
|---|---|
| `privacy/` | tracking protection, third-party cookie/storage controls, referrer policy, fingerprinting-resistance scope (§10) |
| `security/` | secure defaults, abuse protections, security-center backing state (§12, §24, §42) |
| `adblock/` | engine-level network request filtering, cosmetic filtering hooks, filter-list update mechanism (§11) |
| `popup_protection/` | popup blocking, unwanted redirects, abusive notifications/downloads (§12) |
| `extension/` | Android WebExtensions support scope (§16; Phase 6 investigation) |
| `performance/` | startup, memory, network, battery optimizations (§9, §21, §52) |
| `ui/` | iNWEB Android application layer and `chrome/android` adaptations (§37–§39) |
| `offline/` | save-page / reader-mode plumbing (§17) |
| `settings/` | iNWEB settings model (§39) |

## 4. iNWEB application modules (§6) — honest scoping

| Module | Mechanism | Phase |
|---|---|---|
| Privacy Engine | Chromium patch-level controls + iNWEB policy layer; no setting without behavior (§10) | 3 |
| Ad Blocker | Engine-level request filtering in the network stack + filter-list subscriptions, per-site allowlist, statistics from real blocked-request counters (§11) | 4 |
| Popup Protection | Popup policy patches + abusive-notification and suspicious-download warnings (§12) | 4 |
| Private Browsing | Upstream off-the-record profile architecture; in-memory per-profile state; clear user communication of scope (§13) | 3 |
| History / Search history | iNWEB data layer with exclusion in private mode, time-range deletion (§14) | 2–3 |
| Extensions | Upstream has been extending WebExtensions support on Android; the exact usable scope at our baseline is a **Phase 6 investigation deliverable** (supported / partial / unsupported API matrix). No support will be claimed beyond what runs (§16) | 6 |
| VPN | Real `android.net.VpnService` tunnel with a WireGuard-class protocol candidate; **requires server infrastructure before any protection claim or UI** (§15) | 8 |
| Offline Reading | Save-page (MHTML-class) + reader mode; iNWEB local store; offline manager (§17) | 7 |
| Data Saver | Resource blocking, prefetch control, cache policy; remote-proxy compression stays **out of scope** until infrastructure exists (§19–§20) | 7 |
| Energy Saver | Background tab freezing, timer throttling, media and background-activity controls (§21) | 5 |
| Security Center | Dashboard backed strictly by real browser state (§24) | 3+ |
| Biometrics / Encryption | Android BiometricPrompt + Keystore for protected profiles, vaults, backup keys (§25, §27) | 8–9 |
| Profiles | Multiple Chromium profile data directories; iNWEB profile management UI; strict isolation (§28) | 9 |
| Cloud Sync | E2E-encrypted bookmarks/settings/tabs sync; **backend required** — designed first, claimed only when real (§29–§30) | 9 |
| Backup / Restore | Encrypted export/import of iNWEB-owned data with integrity validation (§32) | 9 |
| Notifications | Download/sync/security/VPN-state notifications with permission hygiene (§33) | 11 |
| Entertainment | Optional offline games as a dynamic feature module, outside base APK (§34) | 11 |
| Localization | Android resources: `bn` (Bangladesh) + `en`; all strings externalized (§36) | 10 |

## 5. Data architecture (§31)

- **Chromium-owned data** (cookies, cache, site storage, session state, per-profile
  internals) stays inside Chromium's own storage systems — never duplicated.
- **iNWEB-owned data** (bookmarks, settings, filter-list cache, offline page index, sync
  queue and metadata, download metadata, privacy preferences) lives in an iNWEB Android
  data layer (Room/SQLite), isolated per profile, with Keystore-backed encryption for
  sensitive tables (§27).

## 6. APK size strategy (§7)

- Levers: GN build configuration, unused component removal, resource optimization,
  arm64-v8a as primary ABI (others evaluated), Android App Bundle delivery, and dynamic
  feature modules for optional capabilities (e.g., entertainment, §34).
- Size is **never** reduced by replacing Chromium with WebView (§71).
- A size budget gate is defined in `BUILD-INFRASTRUCTURE.md` §3.

## 7. Honesty constraints carried into design

No setting without behavior (§10); no fake VPN (§15); no fake extension support (§16);
no client-only HTTPS compression claims (§20); telemetry minimal, documented, and
opt-in-first (§41); every completed feature must be implemented → built → tested →
validated (§44).
