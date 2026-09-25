# Phase 7 — Offline Reading & Data-Saving Design

**Status:** authored design (Step 21). Code lands as pure-JVM cores
(CI-tested) plus `offline/` patch-series entries generated against the
pinned baseline `154.0.8037.21` on build infrastructure (blocker B-001).
**Governing requirements:** MASTER-SPEC §17 (offline reading), §18 (cache),
§19 (data saver); §20's compression-truthfulness rules bind every claim:
*no client-only compression of arbitrary HTTPS traffic, ever.*

---

## 1. Strategy: reuse the real upstream machinery

| Need | Upstream subsystem at baseline | iNWEB action |
|---|---|---|
| Reader-mode content extraction | DOM distiller + readability (article heuristics) | enable + expose iNWEB reader surface |
| Save page / offline snapshots | Android offline pages (MHTML snapshot pipeline) | enable + bind to the offline library core (§2) |
| Cache & site-data clearing | browsing-data remover (per-type, per-range) | bind iNWEB "clear data" UI to it |
| Data saver | — (no honest local-only proxy exists) | OUR filter engine, rule-class mode (§4) |

Rebuilding any of these would be worse engineering and a §57 risk; the
patch work is enablement, UI surfaces, and binding to iNWEB cores.

## 2. §17 Offline reading

- **Reader mode:** offered when the distiller's heuristics judge the page
  distillable; the extracted view opens in the iNWEB reader surface.
  Quality depends on the readability heuristics — stated, not oversold.
- **Save page:** snapshot via the upstream MHTML pipeline; the snapshot is
  REGISTERED in the iNWEB offline library (pure-JVM core, §5): online URL,
  title, file, byte size, created/last-accessed timestamps.
- **Offline reopening:** offline library lists saved pages; opening one
  loads the MHTML snapshot and marks last-accessed (feeds eviction).
- **Offline management + storage controls + delete:** the library core
  enforces a quota with LRU eviction (least-recently-accessed first,
  never the currently-open page); explicit delete always available; the
  user is shown REAL byte sizes from the actual files.
- **Cache vs offline storage (§18's "do not confuse" rule, enforced):**
  HTTP cache is re-fetchable and clearable wholesale; saved snapshots are
  USER DATA — `clear cache` never deletes them, `clear saved pages` is a
  separate explicit action. The separation lives in the data model, not
  just the UI.

## 3. §18 Cache

- Cache controls bind to the upstream browsing-data remover: clear cache /
  site data / cookies, per time range, with per-site site-data management
  in settings.
- Storage inspection reports real numbers from the quota/disk accounting
  APIs (cache bytes, site-data bytes, saved-pages bytes) — never estimated
  placeholders (§24 discipline applies to storage too).

## 4. §19 Data saver (honest mechanisms only)

Data-saver mode is a **rule-class mode over the iNWEB filter engine** —
the same engine, decision path, and statistics (ADR-019/020):

1. **Resource blocking:** data-saver rules classify heavyweight,
   non-essential resource classes (e.g. large media, third-party images)
   which are blocked when the mode is on — visible in the normal blocked
   counters.
2. **Prefetch control:** speculative preconnect/prefetch is reduced while
   the mode is on.
3. **Efficient caching:** unchanged — HTTP cache already avoids re-fetches;
   no double-counting of "saved" bytes from normal caching.
4. **Accounting:** the counter reports **requests avoided (real, counted
   BLOCK decisions under data-saver rules)**. "Bytes saved" is reported
   only as a clearly-labeled ESTIMATE (sum of known content-lengths of
   blocked responses where observed), never as a measured fact.
5. **Proxy / compression (§20): explicitly OUT of scope.** Any remote
   proxy would require documented server architecture, encryption and
   privacy models, and operations (§20's own list) — iNWEB ships none and
   claims none. No client-only HTTPS compression claim exists anywhere.

## 5. Pure-JVM core scoping (next step's deliverable)

`src/core/offline` — the offline library registry, testable without a
device (proposed Step 22):

- `OfflinePageRecord` (onlineUrl, title, snapshotFileName, sizeBytes,
  createdAtMillis, lastAccessedAtMillis)
- `OfflineLibrary` store: insertion-order listing, real quota accounting
  (bytes), LRU eviction policy returning the evicted pages, access-time
  updates, explicit delete; persistence seam (in-memory first, file store
  with the patch binding — the established pattern)

## 6. Patch plan (registry entries added when the tree exists)

Continuing the global order after `extension/` (0014–0017):

| id | file | content | risk |
|---|---|---|---|
| `0018-offline-reader-mode` | `offline/0018-offline-reader-mode.patch` | distiller enablement + iNWEB reader surface | medium |
| `0019-offline-page-snapshots` | `offline/0019-offline-page-snapshots.patch` | MHTML save path + offline library UI + quota/eviction binding to the core | medium |
| `0020-offline-data-saver` | `offline/0020-offline-data-saver.patch` | data-saver rule-class wiring into the engine service + requests-avoided accounting | medium |

Cache controls (§18) bind through the settings surfaces — future
`settings/` entries, cross-referenced here.

## 7. Verification strategy

1. **Core (live today after Step 22):** offline library unit tests — quota
   accounting, LRU eviction order, access updates, persistence seam.
2. **C++ unit tests (at build time):** snapshot save/load lifecycle;
   data-saver rule-class activation flags.
3. **Patch level:** `apply_patches.py apply && verify` on the pristine tag.
4. **On device:** save a page → airplane mode → it reopens from the
   library; reader mode offered on an article page and not on a web app;
   delete frees the shown bytes; **clear cache does NOT remove saved
   pages**; data-saver mode blocks an image request and the counter
   increments by exactly one real request.

## 8. Honest boundaries

- Nothing above exists on a device until the patches build and pass §7
  (B-001); the pure-JVM core is real and tested as soon as it lands.
- No proxy, no compression claims (§19/§20) — deferred indefinitely and
  documented as a non-goal unless separately provisioned infrastructure
  is designed first.
- Reader-mode quality inherits the readability heuristics' limits; some
  pages will not be distillable — the offer simply does not appear.
