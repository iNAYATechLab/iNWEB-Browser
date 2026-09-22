# Phase 4 — Ad Blocker Patch-Series Design (`adblock/` area)

**Status:** authored design (Step 14). Patch generation, application, and
verification happen on build infrastructure against the pinned baseline
`154.0.8037.21` (blocker B-001).
**Governing requirements:** MASTER-SPEC §11 (genuine ad blocking — "Do not
implement a fake ad blocker that only changes a toggle"), §10, §24; the
pure-JVM engine and filter-list management already implemented in
`src/core/tracking-protection` (116 unit tests, CI-enforced).

---

## 1. What already exists (implemented and tested)

| Piece | Where | State |
|---|---|---|
| EasyList-family parser (documented subset) | `FilterListParser` | Implemented, tested |
| Request decision engine `decide(): BLOCK / ALLOW / PASS` | `TrackingProtectionEngine` | Implemented, tested |
| Per-site allowlist + policy model | `TrackingProtectionSettings` | Implemented, tested |
| Filter-list download / cache / update | `FilterListManager` + `HttpFilterListFetcher` + `FileFilterListCache` | Implemented, tested |
| Real decision statistics | `EngineStatistics` → `SecurityCenterModel` (§24) | Implemented, tested |

Everything below is the Chromium-side wiring that turns those decisions
into real request blocking. **No request is blocked until this series
builds and passes the verification checklist (§8) — §57.**

## 2. Interception point decision

**Chosen: `content::URLLoaderThrottle`**, registered from the iNWEB
`ContentBrowserClient` override (the override itself is introduced by the
`ui/` series, patch `0002`).

| Candidate | Verdict | Why |
|---|---|---|
| `URLLoaderThrottle` (browser process) | **chosen** | The integration point used by shipped Chromium-derived browsers for request-level blocking; sees every URL load (main + subresource) with initiator/frame context; can defer a request while a background thread decides, then resume or cancel; no network-service process-boundary complexity |
| Network-service `URLRequestInterceptor` | rejected for v1 | Lives inside the network service; harder to reach browser-process state (engine, settings) and to attribute requests to tabs |
| Extension WebRequest API plumbing | rejected | Ties blocking to the extension system; heavier and less controllable than a throttle |
| Render-process early blocking | rejected | Trivially bypassable (compromised renderer); wrong trust boundary |

Throttle lifecycle used by the design:

```text
WillStartRequest / WillRedirectRequest (browser process)
    ├─ build RequestContext (URL, initiator/document URL, mapped resource type)
    ├─ set_deferred(true)  … request pauses in the network stack
    ├─ PostTask(worker pool) → Kotlin TrackingProtectionEngine.decide()
    └─ PostTask(back) →
         BLOCK  → CancelWithError(net::ERR_BLOCKED_BY_CLIENT)
         ALLOW  → Resume()   (exception rule matched — pass-through is fine)
         PASS   → Resume()
```

Redirects re-run the decision (`WillRedirectRequest`) against the new URL —
a redirect chain must not launder an originally-blocked tracker.

Exact `RequestParams` field names (`destination`, `request_initiator`,
frame routing ids) are confirmed against the pinned tree when the patch is
generated; any drift is recorded in the registry `rebase_notes`.

## 3. Resource-type mapping

`network::mojom::RequestDestination` (and the frame context) map onto the
engine's `ResourceType`:

| Chromium destination | Engine `ResourceType` |
|---|---|
| `kScript` (incl. workers) | `SCRIPT` |
| `kImage` | `IMAGE` |
| `kStyle` | `STYLESHEET` |
| `kXhr` / fetch | `XHR` |
| `kDocument` from a sub-frame (iframe) | `SUBDOCUMENT` |
| `kObject` / `kEmbed` | `OBJECT` |
| `kMedia` / audio / video track | `MEDIA` |
| `kFont` | `FONT` |
| `kManifest`, `kPing`, `kWorker`, … | `OTHER` |
| Main-frame navigation `kDocument` | see §4 policy |

**Deferred types (honest scope):** `WEBSOCKET` rules parse and count today,
but WebSockets do not traverse the URL loader; WebSocket blocking needs the
network-delegate path and is scheduled with the Phase 5 performance pass.
`POPUP` rules belong to the `popup_protection/` area (§12), not this series.

## 4. Main-frame policy (v1)

Main-frame navigations are **not filtered in v1**: a blocked navigation can
only produce an error page, and the master spec's popup/abuse scenarios are
handled by `popup_protection/`. The engine is therefore not consulted for
top-level navigations; this is a documented policy, not a hidden gap. When
`$document` rules need real effect (rare, list-dependent), the policy is
revisited with the popup-protection design.

## 5. Component design

```text
chrome/android/inweb/adblock/
    inweb_adblock_service.h/.cc      browser-process singleton:
                                     - owns the JNI global refs to the Kotlin
                                       FilterListManager + current engine
                                       snapshot (ref-counted wrapper; atomic
                                       swap after a successful refresh)
                                     - startup(now): FilterListManager.startup
                                     - refresh(now): conditional; on Updated/
                                       NotModified → buildEngine() snapshot swap
    inweb_adblock_throttle.h/.cc     the URLLoaderThrottle (§2 flow)
    inweb_request_context.h/.cc      native struct → Kotlin RequestContext
                                     marshalling (requestUrl, documentUrl,
                                     resourceType only — minimal surface)
    inweb_adblock_jni.h/.cc          AttachCurrentThread + cached method ids:
                                     decide(RequestContext)String? no —
                                     decide returns a small int enum
                                     (BLOCK=0/ALLOW=1/PASS=2) for cheap JNI
```

Kotlin-side additions (authored later, in `src/`, pure JVM, tested in CI):
an `int`-returning `decideCode(request: RequestContext)` convenience on the
engine — semantics identical to `decide()`, mapping `FilterAction` to the
stable int code. No behavior moves into C++: the C++ side only marshals,
defers, and applies the decision.

Threading: throttles run on the browser-process UI thread; `decide()`
always executes on a worker-pool thread (never blocks the UI thread);
Resume/Cancel hop back to the owning sequence. v1 performs no decision
caching and keeps per-rule regexes (ADR-013) — the combined matcher and
decision cache are the Phase 5 performance deliverable, with measured
budgets from real EasyList traffic.

## 6. Provisioning, allowlist, and statistics

- **Provisioning:** the app start path (ui/ patch `0002` adapter) calls
  `InwebAdBlockService::Startup` → `FilterListManager.startup(now)`:
  cache first, download only missing lists. `HttpFilterListFetcher` is the
  transport; the host scheduler (WorkManager in the app layer) calls
  `Refresh` per `UpdatePolicy` — the policy never runs timers itself
  (ADR-014).
- **Per-site allowlist / global toggle:** already honored inside
  `decide()`; the C++ side needs no parallel logic — one decision path,
  no divergence.
- **Statistics:** `SecurityCenter.build(...)` (Step 13) reads
  `EngineStatistics` of the *current* engine snapshot. In-flight decisions
  during an engine swap complete against the old snapshot — counts stay
  real and attributable.

## 7. Planned patch series (registry entries added when the tree exists)

Ordered after the `ui/` series (0001–0004, PHASE2-INTEGRATION-PLAN):

| id | file | content | risk |
|---|---|---|---|
| `0005-adblock-engine-service` | `adblock/0005-adblock-engine-service.patch` | service + JNI binding + `inweb_adblock_jni`; new files only + `BUILD.gn` deps | medium |
| `0006-adblock-url-loader-throttle` | `adblock/0006-adblock-url-loader-throttle.patch` | throttle + registration in the iNWEB `ContentBrowserClient` override (one upstream hook point) + C++ unit test with a scriptable engine stub | medium |
| `0007-adblock-provisioning-statistics` | `adblock/0007-adblock-provisioning-statistics.patch` | startup/refresh wiring + Security Center read path + device-test filter list fixture | low |

Each entry carries `upstream_files` traceability, a `validation` procedure,
and `rebase_notes`. Registry stays empty until the patches are generated
against the real pinned tree (§57).

## 8. Verification strategy

1. **Core (already live):** 116 Kotlin tests + 77 Python tests in CI —
   parser, matcher, engine, lists, Security Center model.
2. **C++ unit tests (at build time):** throttle with a fake
   `Throttle::Callback` + stub engine: BLOCK cancels with
   `net::ERR_BLOCKED_BY_CLIENT`, ALLOW/PASS resume, redirect re-evaluates,
   main frame skipped.
3. **Patch level:** `apply_patches.py apply && verify` on the pristine
   pinned tag.
4. **On device:** a local verification filter list blocks a known test
   host; the blocked counter in the Security Center increments (real
   decision only — §24); allowlisting the page bypasses everything; the
   global toggle disables filtering; redirects to blocked hosts stay
   blocked.

## 9. Honest boundaries

- Nothing blocks a request until this series builds and passes §8 (B-001).
- Cosmetic filtering (`##` selectors) is **out of scope for this series**:
  the parser already counts cosmetic rules; the injection design
  (isolated-world script + stylesheet hiding) ships as its own later patch.
- `csp=`, `rewrite=`, `removeparam=` remain unsupported (ADR-013 subset).
- `WEBSOCKET` and `POPUP` handling deferred (§3) — stated, not hidden.

---

## 10. Native-port amendment (2026-09-22, ADR-040)

Verification against the pinned tree `154.0.8037.21` (Gitiles) settled the
engine-language question with tree facts:

- `build/android/gyp/` contains **no Kotlin compile step** (121 entries,
  zero Kotlin scripts);
- `third_party/kotlin_stdlib` is a **prebuilt runtime jar**
  (`java_prebuilt` of a CIPD-downloaded `kotlin-stdlib-jdk8.jar`) consumed
  by AAR dependencies — there is no vendored Kotlin compiler
  (`third_party/kotlin` does not exist);
- `chrome/android/BUILD.gn` (3,531 lines) references **zero `.kt`
  sources**.

The §5 JNI-into-Kotlin design is therefore not buildable on this
baseline. The engine is **ported to native C++** (patch 0005), with the
tested Kotlin implementation as the behavioral specification and its
test suite mirrored at build time. Everything else in this design is
unchanged — the `URLLoaderThrottle` interception point (§2; confirmed on
the pinned tree as `ContentBrowserClient::CreateURLLoaderThrottles`),
the resource-type mapping (§3), the main-frame policy (§4),
provisioning/statistics (§6) and the honest boundaries (§9).

Documented port deviations (see `src/native/adblock/README.md`): RE2
instead of `java.util.regex`; Chromium's full Public Suffix List via
`net::registry_controlled_domains` instead of the Kotlin suffix table
(the documented integration intent); regexes compiled once at parse
time; lock-guarded statistics (worker-thread decisions); v1 full-scan
decide (combined matcher = patch 0012); an RE2 compile failure counts
the rule invalid instead of throwing.

Authoritative C++ sources: `src/native/adblock/` (synced into the patch
at generation time; the patch series remains the single authority for
the tree).
