package com.inweb.browser.home

import com.inweb.browser.shell.SearchEngine
import java.util.UUID

/** Validated item rendered in the user-owned Quick Access section. */
data class QuickAccessItem(
    val id: String,
    val title: String,
    val url: String,
) {
    init {
        require(id.isNotBlank()) { "Quick Access id must not be blank" }
        require(title.isNotBlank()) { "Quick Access title must not be blank" }
        require(HomeWebAddresses.isWebDestination(url)) {
            "Quick Access URL must be a valid HTTP(S) destination"
        }
    }
}

/** Untrusted wire form loaded from and committed by Lead-owned durable storage. */
data class StoredQuickAccessItem(
    val id: String,
    val title: String,
    val url: String,
)

fun interface QuickAccessIdGenerator {
    fun nextId(): String
}

enum class QuickAccessStoredIssueReason {
    BLANK_ID,
    BLANK_TITLE,
    INVALID_ADDRESS,
    UNSUPPORTED_SCHEME,
    DUPLICATE_ID,
    DUPLICATE_URL,
    OVER_CAPACITY,
}

data class QuickAccessStoredIssue(
    val index: Int,
    val reason: QuickAccessStoredIssueReason,
)

sealed class QuickAccessError {
    object NameRequired : QuickAccessError()
    object AddressRequired : QuickAccessError()
    object InvalidAddress : QuickAccessError()
    data class UnsupportedScheme(val scheme: String?) : QuickAccessError()
    data class DuplicateUrl(val existingId: String) : QuickAccessError()
    object AtCapacity : QuickAccessError()
    object InvalidGeneratedId : QuickAccessError()
    data class DuplicateId(val id: String) : QuickAccessError()
    data class NotFound(val id: String) : QuickAccessError()
    data class IndexOutOfRange(val index: Int, val size: Int) : QuickAccessError()
}

sealed class QuickAccessResult<out T> {
    /**
     * [storeData] is the complete canonical snapshot the integration must
     * durably commit before reporting user-visible save success.
     */
    data class Ok<T>(
        val value: T,
        val storeData: List<StoredQuickAccessItem>,
    ) : QuickAccessResult<T>()

    data class Err(val error: QuickAccessError) : QuickAccessResult<Nothing>()
}

/** Enough information to provide a real undo after removal. */
data class RemovedQuickAccessItem(
    val item: QuickAccessItem,
    val index: Int,
)

/**
 * Quick Access state and mutation rules.
 *
 * The list is explicit user data and is never seeded or ranked from history.
 * This pure core validates untrusted loaded data and returns a full canonical
 * wire snapshot with every successful mutation. The Lead-owned integration
 * owns durable I/O and its required storage-inventory entry.
 */
class QuickAccessManager(
    storedItems: List<StoredQuickAccessItem> = emptyList(),
    private val searchEngine: SearchEngine = SearchEngine.DUCK_DUCK_GO,
    private val idGenerator: QuickAccessIdGenerator = QuickAccessIdGenerator {
        "qa-${UUID.randomUUID()}"
    },
    private val maxItems: Int = DEFAULT_MAX_ITEMS,
) {

    private val entries = mutableListOf<QuickAccessItem>()
    private val recoveryIssues = mutableListOf<QuickAccessStoredIssue>()

    init {
        require(maxItems > 0) { "maxItems must be positive" }
        restoreStoredItems(storedItems)
    }

    fun items(): List<QuickAccessItem> = entries.toList()

    /** Canonical wire snapshot for initial repair or explicit integration save. */
    fun storeData(): List<StoredQuickAccessItem> = entries.map {
        StoredQuickAccessItem(it.id, it.title, it.url)
    }

    /** Invalid stored rows rejected during startup; empty means no recovery. */
    fun lastRecovery(): List<QuickAccessStoredIssue> = recoveryIssues.toList()

    fun add(name: String, rawAddress: String): QuickAccessResult<QuickAccessItem> {
        val title = name.trim()
        if (title.isEmpty()) return QuickAccessResult.Err(QuickAccessError.NameRequired)
        if (rawAddress.isBlank()) return QuickAccessResult.Err(QuickAccessError.AddressRequired)
        if (entries.size >= maxItems) return QuickAccessResult.Err(QuickAccessError.AtCapacity)

        val url = when (val address = HomeWebAddresses.normalize(rawAddress, searchEngine)) {
            is WebAddressResult.Valid -> address.url
            WebAddressResult.Invalid -> return QuickAccessResult.Err(QuickAccessError.InvalidAddress)
            is WebAddressResult.UnsupportedScheme -> {
                return QuickAccessResult.Err(QuickAccessError.UnsupportedScheme(address.scheme))
            }
        }
        entries.firstOrNull { it.url == url }?.let { existing ->
            return QuickAccessResult.Err(QuickAccessError.DuplicateUrl(existing.id))
        }

        val id = idGenerator.nextId().trim()
        if (id.isEmpty()) return QuickAccessResult.Err(QuickAccessError.InvalidGeneratedId)
        if (entries.any { it.id == id }) {
            return QuickAccessResult.Err(QuickAccessError.DuplicateId(id))
        }

        val item = QuickAccessItem(id = id, title = title, url = url)
        entries += item
        return ok(item)
    }

    fun remove(id: String): QuickAccessResult<RemovedQuickAccessItem> {
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return QuickAccessResult.Err(QuickAccessError.NotFound(id))
        val removed = entries.removeAt(index)
        return ok(RemovedQuickAccessItem(removed, index))
    }

    /** Restores a prior removal at its original position (or the nearest end). */
    fun restore(removed: RemovedQuickAccessItem): QuickAccessResult<QuickAccessItem> {
        if (entries.size >= maxItems) return QuickAccessResult.Err(QuickAccessError.AtCapacity)
        if (entries.any { it.id == removed.item.id }) {
            return QuickAccessResult.Err(QuickAccessError.DuplicateId(removed.item.id))
        }
        entries.firstOrNull { it.url == removed.item.url }?.let { existing ->
            return QuickAccessResult.Err(QuickAccessError.DuplicateUrl(existing.id))
        }
        val index = removed.index.coerceIn(0, entries.size)
        entries.add(index, removed.item)
        return ok(removed.item)
    }

    /**
     * Removes the item, then inserts it at [newIndex] of the resulting list.
     * Invalid requests leave state unchanged.
     */
    fun move(id: String, newIndex: Int): QuickAccessResult<List<QuickAccessItem>> {
        val oldIndex = entries.indexOfFirst { it.id == id }
        if (oldIndex < 0) return QuickAccessResult.Err(QuickAccessError.NotFound(id))
        if (newIndex !in entries.indices) {
            return QuickAccessResult.Err(
                QuickAccessError.IndexOutOfRange(newIndex, entries.size),
            )
        }
        val item = entries.removeAt(oldIndex)
        entries.add(newIndex, item)
        return ok(items())
    }

    private fun restoreStoredItems(stored: List<StoredQuickAccessItem>) {
        stored.forEachIndexed { index, item ->
            val id = item.id.trim()
            val title = item.title.trim()
            val issue = when {
                id.isEmpty() -> QuickAccessStoredIssueReason.BLANK_ID
                title.isEmpty() -> QuickAccessStoredIssueReason.BLANK_TITLE
                entries.size >= maxItems -> QuickAccessStoredIssueReason.OVER_CAPACITY
                entries.any { it.id == id } -> QuickAccessStoredIssueReason.DUPLICATE_ID
                else -> null
            }
            if (issue != null) {
                recoveryIssues += QuickAccessStoredIssue(index, issue)
                return@forEachIndexed
            }

            val url = when (val address = HomeWebAddresses.normalize(item.url, searchEngine)) {
                is WebAddressResult.Valid -> address.url
                WebAddressResult.Invalid -> {
                    recoveryIssues += QuickAccessStoredIssue(
                        index,
                        QuickAccessStoredIssueReason.INVALID_ADDRESS,
                    )
                    return@forEachIndexed
                }
                is WebAddressResult.UnsupportedScheme -> {
                    recoveryIssues += QuickAccessStoredIssue(
                        index,
                        QuickAccessStoredIssueReason.UNSUPPORTED_SCHEME,
                    )
                    return@forEachIndexed
                }
            }
            if (entries.any { it.url == url }) {
                recoveryIssues += QuickAccessStoredIssue(
                    index,
                    QuickAccessStoredIssueReason.DUPLICATE_URL,
                )
                return@forEachIndexed
            }

            entries += QuickAccessItem(id = id, title = title, url = url)
        }
    }

    private fun <T> ok(value: T): QuickAccessResult.Ok<T> =
        QuickAccessResult.Ok(value = value, storeData = storeData())

    companion object {
        const val DEFAULT_MAX_ITEMS = 8
    }
}
