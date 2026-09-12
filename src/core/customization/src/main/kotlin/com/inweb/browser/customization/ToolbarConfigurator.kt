package com.inweb.browser.customization

/**
 * One bottom-bar control (§23 toolbar configuration).
 *
 * The item universe mirrors the AUTHORED bottom bar exactly
 * (`BrowserBottomBar.kt`): back, forward, home, tabs, menu — nothing
 * else. A future item is a code change here that ships with its own
 * stored-config migration (see the module README); ids are stable
 * strings so a stored configuration never depends on enum ordinals.
 *
 * Mandatory items are the structural controls the bar cannot lose:
 * back (navigation escape), tabs (the only session surface), and
 * menu (the only entry to settings/downloads/history/bookmarks).
 * Forward and home are genuine user preferences and may be hidden.
 */
enum class ToolbarItem(
    /** Stable wire/persistence id. */
    val id: String,
    /** Mandatory items are always present and always visible. */
    val mandatory: Boolean,
) {
    BACK("back", true),
    FORWARD("forward", false),
    HOME("home", false),
    TABS("tabs", true),
    MENU("menu", true);

    companion object {
        /** The full item universe (every known item). */
        val ALL: List<ToolbarItem> = ToolbarItem.entries.toList()

        /** The authored default order (matches the shipped bottom bar). */
        val DEFAULT_ORDER: List<ToolbarItem> =
            listOf(BACK, FORWARD, HOME, TABS, MENU)

        fun fromId(id: String): ToolbarItem? =
            ToolbarItem.entries.firstOrNull { it.id == id }
    }
}

/** One slot in the configured toolbar: the item and its visibility. */
data class ToolbarEntry(
    val item: ToolbarItem,
    val visible: Boolean,
)

/** What the persistence seam round-trips (stable item ids — the wire form). */
data class ToolbarStoreData(
    /** ALL items, each exactly once, in display order. */
    val order: List<String>,
    /** The items the user hid (ids; a repeated id is idempotent, not corrupt). */
    val hidden: List<String>,
)

/** Why a configuration or operation failed — results, not exceptions. */
sealed class ToolbarError {
    /** The stored order lists an item id more than once. */
    data class DuplicateItems(val ids: List<String>) : ToolbarError()

    /** The stored order lists ids this build does not know. */
    data class UnknownItems(val ids: List<String>) : ToolbarError()

    /** Known items absent from the stored order entirely. */
    data class MissingItems(val ids: List<String>) : ToolbarError()

    /** Hidden ids that are not slots in the order at all. */
    data class UnknownHidden(val ids: List<String>) : ToolbarError()

    /** Mandatory items the stored configuration tries to hide. */
    data class MandatoryHidden(val items: List<ToolbarItem>) : ToolbarError()

    /** An operation named an item this build does not know. */
    object NotFound : ToolbarError()

    /** A move targeted a position outside the item list. */
    data class IndexOutOfRange(val index: Int, val size: Int) : ToolbarError()
}

sealed class ToolbarResult<out T> {
    data class Ok<T>(val value: T) : ToolbarResult<T>()
    data class Err(val error: ToolbarError) : ToolbarResult<Nothing>()
}

/**
 * The validated, typed toolbar configuration (§23).
 *
 * Invariant: every [ToolbarConfig] holds every known item exactly
 * once, and no mandatory item is hidden. [parse] enforces this for
 * untrusted stored data; the configurator's mutations preserve it.
 */
data class ToolbarConfig(val entries: List<ToolbarEntry>) {

    /** The items the bar renders, in display order. */
    fun visibleItems(): List<ToolbarItem> =
        entries.filter { it.visible }.map { it.item }

    /** The wire form the persistence seam stores. */
    fun toStoreData(): ToolbarStoreData = ToolbarStoreData(
        order = entries.map { it.item.id },
        hidden = entries.filter { !it.visible }.map { it.item.id },
    )

    companion object {
        /** The authored default: every item, shipped order, all visible. */
        fun default(): ToolbarConfig =
            ToolbarConfig(ToolbarItem.DEFAULT_ORDER.map { ToolbarEntry(it, visible = true) })

        /**
         * Validates untrusted stored data. Checks run in a fixed order
         * and each reports every offender, so a corrupt preference file
         * can be diagnosed from the error alone:
         *
         *  1. duplicate ids in the order
         *  2. unknown ids in the order
         *  3. known items missing from the order
         *  4. hidden ids that are not slots in the order
         *  5. mandatory items hidden
         *
         * A repeated id in [ToolbarStoreData.hidden] is idempotent
         * (a flag repeated, not an ambiguous layout) — only the order
         * must be duplicate-free.
         */
        fun parse(data: ToolbarStoreData): ToolbarResult<ToolbarConfig> {
            val duplicates = data.order.groupBy { it }
                .filterValues { it.size > 1 }
                .keys.toList()
            if (duplicates.isNotEmpty()) {
                return ToolbarResult.Err(ToolbarError.DuplicateItems(duplicates))
            }

            val known = data.order.mapNotNull { ToolbarItem.fromId(it) }
            val unknown = data.order.filter { ToolbarItem.fromId(it) == null }
            if (unknown.isNotEmpty()) {
                return ToolbarResult.Err(ToolbarError.UnknownItems(unknown))
            }

            val present = known.map { it.id }.toSet()
            val missing = ToolbarItem.ALL.filter { it.id !in present }.map { it.id }
            if (missing.isNotEmpty()) {
                return ToolbarResult.Err(ToolbarError.MissingItems(missing))
            }

            val hiddenNotInOrder = data.hidden.toSet().filter { it !in present }
            if (hiddenNotInOrder.isNotEmpty()) {
                return ToolbarResult.Err(ToolbarError.UnknownHidden(hiddenNotInOrder))
            }

            val hiddenSet = data.hidden.toSet()
            val mandatoryHidden = known.filter { it.mandatory && it.id in hiddenSet }
            if (mandatoryHidden.isNotEmpty()) {
                return ToolbarResult.Err(ToolbarError.MandatoryHidden(mandatoryHidden))
            }

            return ToolbarResult.Ok(
                ToolbarConfig(known.map { ToolbarEntry(it, visible = it.id !in hiddenSet) })
            )
        }
    }
}

/** Persistence seam; the patch layer provides the real store (B-001). */
interface ToolbarStore {
    /** The stored configuration, or null when nothing was ever saved. */
    fun load(): ToolbarStoreData?
    fun save(data: ToolbarStoreData)
}

/** In-memory store (tests, and until the file-backed store ships with the patches). */
class InMemoryToolbarStore : ToolbarStore {
    private var data: ToolbarStoreData? = null
    override fun load(): ToolbarStoreData? = data
    override fun save(data: ToolbarStoreData) {
        this.data = ToolbarStoreData(order = data.order.toList(), hidden = data.hidden.toList())
    }
}

/**
 * Holds the current toolbar configuration (§23): reordering, hiding,
 * showing, and reset — every mutation validated and persisted through
 * the seam.
 *
 * Recovery rule (ADR-029, same spirit as the extension registry's
 * snapshot restore): a stored configuration that fails [ToolbarConfig.parse]
 * is corrupt. The toolbar falls back to the authored default, the
 * repair is PERSISTED, and [lastRecovery] reports what was rejected —
 * a corrupt preference file is never a crash and never a silent
 * ignore.
 */
class ToolbarConfigurator(private val store: ToolbarStore = InMemoryToolbarStore()) {

    private var config: ToolbarConfig
    private var recovery: ToolbarError? = null

    init {
        when (val stored = store.load()) {
            null -> {
                config = ToolbarConfig.default()
                persist()
            }
            else -> when (val parsed = ToolbarConfig.parse(stored)) {
                is ToolbarResult.Ok -> config = parsed.value
                is ToolbarResult.Err -> {
                    recovery = parsed.error
                    config = ToolbarConfig.default()
                    persist()
                }
            }
        }
    }

    /** The current validated configuration. */
    fun current(): ToolbarConfig = config

    /** The items the bar renders, in display order. */
    fun visibleItems(): List<ToolbarItem> = config.visibleItems()

    /** What a corrupt stored configuration was rejected for, or null. */
    fun lastRecovery(): ToolbarError? = recovery

    /**
     * Moves [itemId] to [newIndex] of the FULL item list (visible and
     * hidden — the customization surface shows both). Semantics: the
     * item is removed, then inserted at [newIndex] of the resulting
     * list. The move is rejected (state unchanged) for an unknown item
     * or an out-of-range index.
     */
    fun move(itemId: String, newIndex: Int): ToolbarResult<ToolbarConfig> {
        val item = ToolbarItem.fromId(itemId)
            ?: return ToolbarResult.Err(ToolbarError.NotFound)
        val entries = config.entries.toMutableList()
        if (newIndex !in entries.indices) {
            return ToolbarResult.Err(ToolbarError.IndexOutOfRange(newIndex, entries.size))
        }
        val from = entries.indexOfFirst { it.item == item }
        if (from == -1) {
            // Unreachable while the validity invariant holds; kept defensive.
            return ToolbarResult.Err(ToolbarError.NotFound)
        }
        val entry = entries.removeAt(from)
        entries.add(newIndex, entry)
        return commit(ToolbarConfig(entries))
    }

    /**
     * Shows or hides [itemId]. Hiding a mandatory item is rejected
     * (state unchanged); a hidden item keeps its stored position, so
     * showing it restores it where the user left it.
     */
    fun setVisible(itemId: String, visible: Boolean): ToolbarResult<ToolbarConfig> {
        val item = ToolbarItem.fromId(itemId)
            ?: return ToolbarResult.Err(ToolbarError.NotFound)
        if (!visible && item.mandatory) {
            return ToolbarResult.Err(ToolbarError.MandatoryHidden(listOf(item)))
        }
        val index = config.entries.indexOfFirst { it.item == item }
        if (index == -1) {
            // Unreachable while the validity invariant holds; kept defensive.
            return ToolbarResult.Err(ToolbarError.NotFound)
        }
        val entries = config.entries.toMutableList()
        entries[index] = entries[index].copy(visible = visible)
        return commit(ToolbarConfig(entries))
    }

    /** Restores the authored default configuration and persists it. */
    fun reset(): ToolbarConfig {
        config = ToolbarConfig.default()
        persist()
        return config
    }

    /**
     * Re-validates defensively (mutations of a valid config are valid
     * by construction; this keeps the "everything persisted passed
     * validation" invariant airtight for future mutation paths) and
     * persists.
     */
    private fun commit(candidate: ToolbarConfig): ToolbarResult<ToolbarConfig> {
        val parsed = ToolbarConfig.parse(candidate.toStoreData())
        if (parsed is ToolbarResult.Err) return parsed
        config = (parsed as ToolbarResult.Ok).value
        persist()
        return ToolbarResult.Ok(config)
    }

    private fun persist() = store.save(config.toStoreData())
}
