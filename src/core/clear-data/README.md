# `src/core/clear-data` — Clear-Browsing-Data Core (pure JVM)

The clear-browsing-data plan model (Phase 12 hardening design item 2):
which stores a clear action touches, per-item dry-run previews, and
execution through REAL store APIs. The item universe is driven by the
**clear column of `docs/STORAGE-INVENTORY.yaml`** — no item exists
without a store behind it, and no store with a clear-browsing-data
contract is missing from the item set.

**Pure JVM, no Android dependency** — validated by
`bash scripts/validate_kotlin_core.sh`, which compiles this module
with `browser-shell`, `tracking-protection`, and `offline` on the
classpath (first core with cross-module dependencies — the script's
`--deps` mechanism). The clear-data surface + engine bindings ship
with the patches (B-001).

## Components

| Component | Purpose |
|---|---|
| `ClearDataItem` | The item universe — exactly the inventory's clear-browsing-data rows: history, session, filter-list cache, offline pages, per-site zoom overrides |
| `ClearDataBinding` | One item's real behavior: `preview()` (dry-run, never mutates) + `clear()` (through the store's existing API) |
| Five default bindings | `HistoryClearBinding` → `HistoryStore.clearAll()`; `SessionClearBinding` → `SessionPersistence.clear()`; `FilterListCacheClearBinding` → `FilterListCache.clear()`; `OfflinePagesClearBinding` → `OfflineLibrary.clearAll()`; `SiteZoomOverridesClearBinding` → `ZoomSettings.clearSiteZooms()` |
| `ClearDataManager` | Duplicate-binding guard (fail-fast), canonical `availableItems()`, `preview(items)` and `clear(items)` with validation |

## Contract

- **No invented clearing paths:** every binding delegates to an API
  that already existed on its store (plus `OfflineLibrary.clearAll()`
  and `ZoomSettings.clearSiteZooms()`, added with this step for the
  same purpose).
- **Nothing silently skipped:** a selection naming an unbound item is
  rejected (`UnboundItems`) — the surface must bind or disable it.
- **Dry-run honesty:** `preview()` reports real counts from the stores
  (null = not countable, e.g. the disposable filter-list cache); it
  never mutates anything.
- **Selection is transient dialog state**, not a persisted preference.
- **Deliberately not items** (per the inventory's clear column):
  bookmarks (deliberate user data), settings/toolbar/notification/
  download preferences (app reset only), the downloads catalog
  (nothing persisted yet — it joins when the patch lands).
