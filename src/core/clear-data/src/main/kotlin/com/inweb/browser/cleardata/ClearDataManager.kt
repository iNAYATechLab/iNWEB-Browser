package com.inweb.browser.cleardata

import com.inweb.browser.offline.OfflineLibrary
import com.inweb.browser.privacy.lists.FilterListCache
import com.inweb.browser.shell.HistoryStore
import com.inweb.browser.shell.SessionPersistence
import com.inweb.browser.shell.SessionStore
import com.inweb.browser.shell.ZoomSettings

/**
 * One clearable browsing-data category — EXACTLY the STORAGE-INVENTORY
 * rows whose clear column says clear-browsing-data (Phase 12 design
 * item 2):
 *
 *  - HISTORY: browsing history visits (HistoryStore)
 *  - SESSION: open tabs / session snapshot (SessionPersistence)
 *  - FILTER_LIST_CACHE: downloaded filter-list bodies + metadata
 *    (disposable cache)
 *  - OFFLINE_PAGES: saved offline pages (user data, cleared by choice)
 *  - SITE_ZOOM_OVERRIDES: per-site zoom choices (site data; the
 *    default factor is user preference and stays)
 *
 * Deliberately NOT items (STORAGE-INVENTORY clear column): bookmarks
 * (deliberate user data), settings/toolbar/notifications/download
 * preferences (app reset only), downloads catalog (nothing is
 * persisted yet — the item joins when the patch lands).
 */
enum class ClearDataItem(val id: String) {
    HISTORY("history"),
    SESSION("session"),
    FILTER_LIST_CACHE("filter_list_cache"),
    OFFLINE_PAGES("offline_pages"),
    SITE_ZOOM_OVERRIDES("site_zoom_overrides"),
}

/** Dry-run answer for one item: how many things clearing would remove. */
data class ClearDataPreview(
    /** null = not countable (e.g. a disposable cache with no enumeration API). */
    val count: Int?,
)

/** Why a clear request failed — results, not exceptions. */
sealed class ClearDataError {
    /** The selection names items this manager has no binding for. */
    data class UnboundItems(val items: List<ClearDataItem>) : ClearDataError()

    /** Nothing was selected; the surface must prevent this, not the stores. */
    object EmptySelection : ClearDataError()
}

sealed class ClearDataResult<out T> {
    data class Ok<T>(val value: T) : ClearDataResult<T>()
    data class Err(val error: ClearDataError) : ClearDataResult<Nothing>()
}

/**
 * One item's real clearing behavior. The default bindings below each
 * delegate to an EXISTING store API — no new clearing paths are
 * invented here; this module orchestrates real ones.
 */
interface ClearDataBinding {
    val item: ClearDataItem

    /** Dry-run: what clearing would remove now; NEVER mutates anything. */
    fun preview(): ClearDataPreview

    /** Clears; returns how many things were removed (null = not countable). */
    fun clear(): Int?
}

/** Browsing history (§13/§14): clears every recorded visit. */
class HistoryClearBinding(private val store: HistoryStore) : ClearDataBinding {
    override val item = ClearDataItem.HISTORY
    override fun preview(): ClearDataPreview = ClearDataPreview(count = store.allVisits().size)
    override fun clear(): Int = store.clearAll()
}

/**
 * Open tabs / the session snapshot (§51): clears the persisted
 * snapshot. Preview counts the tabs a restore would reopen; a corrupt
 * snapshot previews as 0 (it would have fallen back to a fresh
 * session anyway).
 */
class SessionClearBinding(private val persistence: SessionPersistence) : ClearDataBinding {
    override val item = ClearDataItem.SESSION

    private fun tabCount(): Int = persistence.load()?.let { text ->
        try {
            SessionStore.deserialize(text).tabs.size
        } catch (_: Exception) {
            0
        }
    } ?: 0

    override fun preview(): ClearDataPreview = ClearDataPreview(count = tabCount())
    override fun clear(): Int {
        val removed = tabCount()
        persistence.clear()
        return removed
    }
}

/**
 * Downloaded filter lists (§10): a disposable cache — the cache has no
 * enumeration API and needs none; lists re-download on next refresh.
 */
class FilterListCacheClearBinding(private val cache: FilterListCache) : ClearDataBinding {
    override val item = ClearDataItem.FILTER_LIST_CACHE
    override fun preview(): ClearDataPreview = ClearDataPreview(count = null)
    override fun clear(): Int? {
        cache.clear()
        return null
    }
}

/** Saved offline pages (§17): deletes every page and persists the empty library. */
class OfflinePagesClearBinding(private val library: OfflineLibrary) : ClearDataBinding {
    override val item = ClearDataItem.OFFLINE_PAGES
    override fun preview(): ClearDataPreview = ClearDataPreview(count = library.all().size)
    override fun clear(): Int = library.clearAll()
}

/** Per-site zoom overrides (§23): site data — the default factor stays. */
class SiteZoomOverridesClearBinding(private val zoom: ZoomSettings) : ClearDataBinding {
    override val item = ClearDataItem.SITE_ZOOM_OVERRIDES
    override fun preview(): ClearDataPreview =
        ClearDataPreview(count = zoom.current().siteZooms.size)

    override fun clear(): Int {
        val removed = zoom.current().siteZooms.size
        zoom.clearSiteZooms()
        return removed
    }
}

/**
 * The clear-browsing-data center (Phase 12 design item 2): dry-run
 * preview and execution through REAL store APIs, driven by the item
 * universe above (the STORAGE-INVENTORY clear column).
 *
 * Selection is transient dialog state, not a persisted preference —
 * nothing is remembered between dialogs. A selection naming an
 * unbound item is REJECTED (never silently skipped): the surface must
 * bind or disable it, not drop it.
 */
class ClearDataManager(private val bindings: List<ClearDataBinding>) {

    init {
        val duplicates = bindings.groupBy { it.item }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "duplicate clear-data bindings: $duplicates" }
    }

    /** The bound items, in canonical order. */
    fun availableItems(): List<ClearDataItem> =
        ClearDataItem.entries.filter { item -> bindings.any { it.item == item } }

    /** Dry-run: per-item preview of what would be cleared; never mutates. */
    fun preview(items: Set<ClearDataItem>): ClearDataResult<Map<ClearDataItem, ClearDataPreview>> {
        validate(items)?.let { return ClearDataResult.Err(it) }
        return ClearDataResult.Ok(items.associateWith { requireBinding(it).preview() })
    }

    /** Executes the chosen items through their real store APIs. */
    fun clear(items: Set<ClearDataItem>): ClearDataResult<Map<ClearDataItem, Int?>> {
        validate(items)?.let { return ClearDataResult.Err(it) }
        return ClearDataResult.Ok(items.associateWith { requireBinding(it).clear() })
    }

    private fun validate(items: Set<ClearDataItem>): ClearDataError? {
        if (items.isEmpty()) return ClearDataError.EmptySelection
        val unbound = items.filter { bindings.none { b -> b.item == it } }
        if (unbound.isNotEmpty()) return ClearDataError.UnboundItems(unbound)
        return null
    }

    /** Only reachable after [validate]; a guard against future regressions. */
    private fun requireBinding(item: ClearDataItem): ClearDataBinding =
        bindings.firstOrNull { it.item == item }
            ?: throw IllegalStateException("unbound item passed validation: $item")
}
