# Phase 4 — Cosmetic Filtering Patch-Series Design

**Status:** authored design (Step 16); the **engine half is already
implemented and tested in pure JVM** (`CosmeticFilterParser` /
`CosmeticFilterEngine`, 13 new tests). The Chromium injection half ships as
a tracked patch generated on build infrastructure against baseline
`154.0.8037.21` (blocker B-001).
**Governing requirements:** MASTER-SPEC §11 "cosmetic filtering where
technically feasible"; sister documents: `PHASE4-ADBLOCK-DESIGN.md`
(network filtering), `PHASE4-POPUP-PROTECTION-DESIGN.md`.

---

## 1. What exists (implemented and tested today)

| Piece | Where | State |
|---|---|---|
| Cosmetic rule parsing: `##` hide, `#@#` exception, domain includes/excludes | `CosmeticFilterParser` | Implemented, tested |
| Procedural `#?#` rules: recognized and counted, **not matched** | `CosmeticFilterParser` | Implemented, tested |
| `#$#` / `#%#` (style rewrite / scriptlets): counted unsupported | `CosmeticFilterParser` | Implemented, tested |
| Per-host CSS computation (grouped selectors, exception cancellation) | `CosmeticFilterEngine.hideCssFor(host)` | Implemented, tested |
| List lifecycle: cosmetic lists parsed beside network lists | `FilterListManager.buildCosmeticEngine()` | Implemented, tested |

The v1 subset (ADR-021): **stylesheet hiding only** — `##` with `#@#`
exceptions. Procedural and scriptlet filtering is deferred, explicitly
counted, never silently applied.

## 2. Injection architecture (the patch's half)

```text
frame commits a document (main frame or same-process iframe)
    └─ DidFinishNavigation / document-available notification (browser process)
         ├─ host = frame's last committed origin host
         ├─ query CosmeticFilterEngine.hideCssFor(host)
         │    (worker thread via the 0005 engine service; never the UI thread)
         └─ PostTask back → insert the stylesheet into that frame
              (content-layer CSS-insertion API; exact symbol confirmed
               against the pinned tree when the patch is generated)
```

- **Per-frame, not per-tab:** iframes get their own host-matched CSS;
  same-process frames only in v1 (cross-process iframes get CSS at their
  own commit).
- **Timing:** injection happens at document-committed time, as early as
  the API allows. A brief flash of unhidden elements is possible before
  injection completes — documented, not hidden (this is why network
  blocking stays the primary defense; cosmetic is secondary).
- **Re-injection:** same-document navigations and dynamic DOM are NOT
  re-scanned in v1 (pure stylesheet hiding is set-and-forget); procedural
  filtering (v2 candidate) would introduce a MutationObserver in an
  isolated world.
- **Selectors pass through verbatim** — the browser CSS parser is the
  validator; the engine never rewrites selectors.

## 3. Policy integration (one path, no divergence)

- Global toggle and per-site allowlist are honored **before** the engine
  is consulted — the same `TrackingProtectionSettings` the network engine
  uses. One shields list, one decision path (ADR-020 pattern).
- No new §24 counters in v1: cosmetic injection is a page-decoration
  concern; the Security Center keeps counting real request decisions only.

## 4. Planned patch series (registry entry added when the tree exists)

Continuing the global order after `ui/` (0001–0004), `adblock/`
(0005–0007), `popup_protection/` (0008–0011):

| id | file | content | risk |
|---|---|---|---|
| `0012-cosmetic-injection` | `adblock/0012-cosmetic-injection.patch` | per-frame stylesheet injection wired to the cosmetic engine + C++ unit test with a scriptable engine stub | medium |

(The cosmetic engine itself lives in `src/` — pure JVM, CI-tested — the
patch only wires the Chromium side, consistent with ADR-009/ADR-012.)

## 5. Verification strategy

1. **Core (live today):** 116 Kotlin tests in `tracking-protection`,
   including the 13 cosmetic tests (parsing, domain semantics, exception
   cancellation, grouped CSS output, manager integration).
2. **C++ unit tests (at build time):** injection observer with a stub
   engine — CSS inserted once per committed frame, empty CSS = no
   insertion, allowlisted host = no query.
3. **Patch level:** `apply_patches.py apply && verify` on the pristine
   pinned tag.
4. **On device:** a local test list with `##` + `#@#` rules: element
   hidden on matching host, subdomain matching, exception honored,
   allowlisted site unfiltered, toggle disables everything.

## 6. Honest boundaries

- Nothing is hidden anywhere until `0012` builds and passes §5 (B-001).
- Procedural (`#?#`), style-rewrite (`#$#`), and scriptlet (`#%#`)
  filtering is deferred — counted, never silently applied.
- Cosmetic filtering never claims to *block* anything: it hides page
  elements; ad/tracker requests remain the network engine's job.
