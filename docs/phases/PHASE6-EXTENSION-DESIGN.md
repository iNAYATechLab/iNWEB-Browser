# Phase 6 — Extension System Design (WebExtensions on Android)

**Status:** patches `0014`–`0017` prepared against pinned baseline
`154.0.8037.21`; build, artifact audit, and device verification still pending
(blocker B-001). Preparation is not a capability claim.
**Governing requirements:** MASTER-SPEC §16 (the eight documentation items
below), roadmap Phase 6; honesty rule: *"Do not pretend that opening an
extension website equals extension support."*

---

## 1. Investigation summary (to verify against the pinned tree)

What upstream Chromium provides at this baseline — the design's factual
basis, each point re-verified against the real tree when patches are
generated:

1. The WebExtensions runtime (`extensions/`, `chrome/browser/extensions`,
   renderer-side bindings, IPC, isolated worlds) **exists in the tree**. The
   pinned baseline already has a separate, explicitly unstable
   `enable_desktop_android_extensions` build path. iNWEB uses a narrow custom
   opt-in to that path rather than setting the full desktop
   `enable_extensions` flag.
2. `declarativeNetRequest` (MV3 network filtering) and the MV3 service
   worker model are core subsystems; their Android availability must be
   audited from the built artifact and on device (§7 step 2).
3. The pinned tree already contains Android management/developer bridges,
   app-menu entries, toolbar actions, popup contents and options routing.
   iNWEB reuses them and adds no parallel fake model. Because the upstream
   backend reports enabled whenever compiled, `0016` closes those surfaces by
   default behind an explicit device-verification switch.
4. The Chrome Web Store serves install payloads to recognized clients;
   depending on it for the primary flow couples us to Google behavior —
   the v1 installation model is sideload-first (§4).

## 2. Architecture decision

**Enable and complete the upstream extension subsystem on Android** via the
`extension/` patch area. Rejected alternatives:

| Alternative | Why rejected |
|---|---|
| Custom/proprietary extension format | No ecosystem, §16 explicitly demands WebExtensions compatibility |
| WebView-based shim for extension UI | Violates the Chromium-architecture requirement (§2) and cannot host real extension logic |
| "Open the Web Store in a tab" and stop | Explicitly forbidden by §16 — no pretense |

## 3. Supported / partial / unsupported APIs (v1 target)

Honesty note: the per-API table below is the **design target**; final
status is audited by a build-time script (§7 step 2) that greps the
built binary's API registrations and produces the REAL table for the
release notes. The table is never hand-edited to look better.

| API | v1 status | Notes |
|---|---|---|
| `runtime`, `i18n`, `storage` | **supported** | core plumbing; local + sync areas (sync = local until Phase 9) |
| `tabs` | **supported** (events + query/create/update limited to real tabs) | maps onto the tabs controller surface |
| `scripting` (MV3) | **supported** | isolated-world injection, shared with cosmetic-filter machinery |
| `declarativeNetRequest` | **supported** | the MV3 filtering path; coexists with the built-in filter engine (both run; built-in cannot be uninstalled) |
| `action`/`browserAction` | **partial** | button + badge + popup; popup renders in an iNWEB dialog surface (§5) |
| `windows` | **partial** | single-window semantics on Android; `create` opens a tab |
| `cookies`, `webNavigation` | **partial** | subject to build-time audit |
| `webRequest` (blocking) | **MV2 only** | MV3 gets observational webRequest; policy in §4 |
| `webstore`, desktop-only APIs (`identity`-class, `nativeMessaging`) | **unsupported** | native messaging needs host binaries — deferred with an explicit note; no silent stubs — calling an unsupported API returns the standard `chrome.runtime.lastError`, never a fake success |

## 4. The four models (§16 requirement)

These are release acceptance targets. The prepared patches contain package
inspection and policy decisions but do not yet prove or claim runtime install
wiring.

- **Permission model:** upstream install-time permission warnings,
  unchanged; runtime/optional permissions (MV3) prompt in an iNWEB dialog.
  No permission is ever granted implicitly by installation.
- **Installation model:** v1 = **sideload a CRX/ZIP** from the device via
  the extensions management screen, with full permission-warning review
  before enable. Browsing the Chrome Web Store is allowed for DISCOVERY;
  installing from it works only as far as Google's client detection
  permits at the time — never claimed as a guarantee. No silent/remote
  installs, ever.
- **Update model:** manual re-sideload with version comparison against the
  installed copy. A future iNWEB-hosted catalog is a separate, explicitly
  designed step (privacy policy applies, §41). No Chrome-Web-Store
  auto-update claim.
- **Security model:** CRX3 signature verification where applicable;
  permission warning surface; per-extension disable/remove; extension
  errors surface in the management screen; the built-in tracker/ad
  protection cannot be disabled BY an extension; extensions run only in
  their isolated worlds and the browser's own decision paths (§ ADR-019/20
  one-path rule) take precedence over extension-observed state.

**MV2/MV3 policy (decision):** MV3 is the primary target. MV2 extensions
install on a documented grace basis while the upstream runtime remains in
the tree at our baseline — with a visible deprecation notice — because
several major Bengali-relevant blockers (ad blockers) still ship MV2.
When upstream removes the MV2 runtime at a future rebase, MV2 installs
stop; that limitation is stated up front, not discovered later.

## 5. Component design (patch plan)

iNWEB adds only package-inspection and review-policy components under
`chrome/android/inweb/extensions/`. Management, toolbar, popup and options
surfaces are the pinned upstream Android implementations; the patch series
must not duplicate them.

Generated registry entries, continuing after the Home/app-layer probe `0013`:

| id | file | content | risk |
|---|---|---|---|
| `0014-extension-enable-android` | `extension/0014-extension-enable-android.patch` | default-false iNWEB opt-in to the experimental Android core + build-time API audit script | high (requires security review note) |
| `0015-extension-management-ui` | `extension/0015-extension-management-ui.patch` | CRX3/ZIP inspector + Chromium build/test hooks; retain upstream management surfaces; suppress unsupported Web Store menu route | medium |
| `0016-extension-action-surfaces` | `extension/0016-extension-action-surfaces.patch` | default-closed runtime/test gate for upstream Android extension surfaces | medium |
| `0017-extension-policy-gate` | `extension/0017-extension-policy-gate.patch` | deterministic stage/review/new-permission/MV2 policy model + tests; no runtime support claim | medium |

## 6. Verification strategy

1. **Core (live today):** the extension management state machine this
   design calls for (install/enable/disable/remove/version checks) is
   implemented in pure JVM — `src/core/extensions` (17 unit tests,
   CI-enforced, ADR-023) — exactly like the tracking-protection core.
2. **Build-time audit:** a script that extracts the actually-registered
   extension APIs from the built APK and regenerates the §3 table —
   the release notes show reality, not intent.
3. **C++ unit tests:** archive/installer validation (valid/invalid/corrupt
   packages and version comparison) plus stage/review/permission/MV2 policy.
4. **On device:** install a known test extension (uBlock-class) — DNR rules
   fire, popup opens and interacts, storage persists across restart,
   disable/enable/remove work, permission warnings shown pre-install, and
   the built-in protections remain active alongside.

## 7. Honest boundaries

- Until `0014–0017` build and pass §6, **iNWEB has zero extension
  support** — browsing the Web Store is just browsing (§16).
- Every API status in §3 is a target until the build-time audit confirms
  it; the audit output is authoritative.
- No Web-Store auto-update, no silent installs, no pretending an
  unsupported API works (standard `lastError`, never fake success).
