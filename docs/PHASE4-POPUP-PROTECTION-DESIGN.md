# Phase 4 — Popup & Abuse Protection Patch-Series Design (`popup_protection/` area)

**Status:** authored design (Step 15). Patch generation, application, and
verification happen on build infrastructure against the pinned baseline
`154.0.8037.21` (blocker B-001).
**Governing requirements:** MASTER-SPEC §12 — popup blocking, unwanted
redirect protection, abusive notification controls, malicious download
protection *where supported*, deceptive interaction protection *where
feasible*. Sister document: `PHASE4-ADBLOCK-DESIGN.md` (network filtering).

---

## 1. What already exists (implemented and tested)

| Piece | Where | State |
|---|---|---|
| `$popup` rule type + `ResourceType.POPUP` | `FilterListParser` / `ResourceType` | Parses, counts, matches — **this series gives it a real call site** |
| Per-site allowlist (single exemption list) | `TrackingProtectionSettings.isSiteAllowlisted` | Implemented, tested — reused for popup exemptions (one shields list, not three) |
| Request-decision API | `TrackingProtectionEngine.decide` | Implemented, tested — consulted for popup attempts |

## 2. Protection surfaces and integration points

Exact upstream symbols are confirmed against the pinned tree when each
patch is generated; candidates below are the designed points, with drift
recorded in registry `rebase_notes`.

### A. Popup blocking (window-open abuse)

**Policy:** a new window/tab creation without a user activation on the
initiating frame is a popup; it is blocked unless (1) the site is
allowlisted, (2) an `@@` exception matches, or (3) the user explicitly
re-opens it from the blocked-popup indicator.

**Hook:** the window-creation consent path in the browser process —
`WebContentsDelegate::ShouldCreateWebContents`-class guard (or the
equivalent `RenderFrameHost` window-open consent at this baseline).
Decision inputs: user-activation state of the initiating frame, the popup
URL, the opener document URL → engine `decide()` with
`ResourceType.POPUP`.

**Behavior:** blocked popups are counted (real counter, feeds §24) and
surfaced as a UI chip with "open once" / "always allow this site"
(writes the existing allowlist — no new settings surface).

### B. Unwanted redirect protection (tab-takeover / tab-under)

The abuse pattern: a hidden or ad **subframe** navigates the **top-level
page** without user activation, hijacking the tab.

**Hook:** a NavigationThrottle on top-level navigations that checks
(a) initiator = subframe of the current tab, (b) no user activation →
block with a "redirect blocked" message and keep the original page.
Same-tab JS redirects with genuine user gestures are never touched.
Redirects toward hosts already BLOCKed by the adblock engine are caught
by `0006` (`WillRedirectRequest` re-evaluation) — this throttle adds the
frame-context rule the engine cannot see.

### C. Abusive notification controls

**Policy:** notification permission prompts are **quieted** (no modal) on
sites without meaningful engagement, exactly once explained, never
auto-granted. Uses Chromium's existing quiet-messaging /
permission-autoblocking infrastructure (engagement-based selector) driven
to our stricter default.

**Honest boundary:** iNWEB makes **no Safe-Browsing-class reputation
claim** — that requires Google API keys and services this build does not
bundle. Abuse detection is policy + list based only; §12's "where
supported" is satisfied by quieting + autoblocking, stated plainly.

### D. Malicious download protection (where supported)

1. **Automatic-download limiting:** multiple automatic (non-gesture)
   downloads trigger an explicit user confirmation
   (DownloadRequestLimiter-class behavior), enabled by default.
2. **Engine-backed host checks:** download URLs from hosts blocked by
   the filter engine (BLOCK decisions on `OBJECT`/`OTHER` downloads) are
   declined; blocked-download counters feed §24.
3. No remote reputation/scan service is claimed (same boundary as C).

### E. Deceptive interaction protection (where feasible)

Clickjacking-style guards need renderer-side heuristics beyond v1's
scope; **deferred** with an explicit registry note — stated, not hidden.

## 3. Component design

```text
chrome/android/inweb/popup/
    inweb_popup_guard.h/.cc          window-creation guard + engine call
    inweb_redirect_throttle.h/.cc    tab-takeover detection
    inweb_notification_policy.h/.cc  quiet-prompt policy wiring
    inweb_download_guard.h/.cc       auto-download confirmation +
                                     engine-backed host check
```

All components live in the browser process, call the same JNI engine
service as the adblock series (`0005`), and record into a shared
protection-statistics bundle the Security Center model will expose when
the wiring lands (the Kotlin model extension ships with the patches —
never before the real counters exist, §24).

## 4. Planned patch series (registry entries added when the tree exists)

Continuing the global order after the `ui/` (0001–0004) and `adblock/`
(0005–0007) series:

| id | file | content | risk |
|---|---|---|---|
| `0008-popup-window-guard` | `popup_protection/0008-popup-window-guard.patch` | popup guard + `$popup` engine call site + blocked-popup chip signal | medium |
| `0009-popup-redirect-throttle` | `popup_protection/0009-popup-redirect-throttle.patch` | tab-takeover throttle + blocked-redirect message | medium |
| `0010-popup-notification-policy` | `popup_protection/0010-popup-notification-policy.patch` | quiet prompts + autoblock wiring | low |
| `0011-popup-download-guard` | `popup_protection/0011-popup-download-guard.patch` | automatic-download confirmation + host checks | medium |

## 5. Verification strategy

1. **Core (live today):** `$popup` parsing/matching already covered by the
   79 Kotlin engine tests; allowlist semantics covered.
2. **C++ unit tests (at build time):** guard with scripted
   activation/frame states; throttle with synthetic initiators;
   notification policy table tests.
3. **Patch level:** `apply_patches.py apply && verify` on the pristine
   pinned tag.
4. **On device:** test page opens `window.open` without gesture → blocked
   + chip visible + "open once" works; subframe top-level navigation →
   blocked, page intact; notification prompt on a fresh site → quiet;
   second automatic download → confirmation required; all counters
   visible in the Security Center and all zero before any real event
   (§24 — no fabricated numbers).

## 6. Honest boundaries

- Nothing above blocks anything until this series builds and passes §5
  (B-001).
- No Safe-Browsing/remote-reputation claims (no bundled service).
- Deceptive-interaction guards deferred (§2E) — explicit, not hidden.
- Same-tab user-driven redirects are never blocked (web-compatibility).
