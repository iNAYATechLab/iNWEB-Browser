package com.inweb.browser.privacy.lists

import com.inweb.browser.privacy.CosmeticFilterEngine
import com.inweb.browser.privacy.CosmeticFilterParser
import com.inweb.browser.privacy.FilterListParser
import com.inweb.browser.privacy.ParsedCosmeticList
import com.inweb.browser.privacy.ParsedFilterList
import com.inweb.browser.privacy.TrackingProtectionEngine
import com.inweb.browser.privacy.TrackingProtectionSettings

/** Per-source outcome of a startup load or refresh pass. */
sealed interface ListUpdateStatus {
    /** Downloaded a new body (initial or refresh) — parsed and cached. */
    data class Updated(val version: String?, val ruleCount: Int) : ListUpdateStatus

    /** Server confirmed the cached copy is current (304). */
    data class NotModified(val version: String?) : ListUpdateStatus

    /** Download failed; [servedFromCache] tells whether the engine kept content. */
    data class Failed(val reason: String, val servedFromCache: Boolean) : ListUpdateStatus

    /** Not attempted — e.g. source disabled, or refresh not yet due. */
    data class Skipped(val reason: String) : ListUpdateStatus

    data object LoadedFromCache : ListUpdateStatus
}

/**
 * Orchestrates the filter-list lifecycle: startup (cache first, download
 * only what is missing), conditional refresh (ETag / Last-Modified → 304),
 * and graceful degradation — a failed refresh always keeps the cached copy
 * in the engine.
 *
 * Pure JVM and fully unit-tested; it performs no background scheduling
 * itself ([UpdatePolicy] is the decision, the host layer runs the timer).
 */
class FilterListManager(
    private val sources: List<FilterListSource> = FilterListSource.DEFAULTS,
    private val fetcher: FilterListFetcher,
    private val cache: FilterListCache,
    private val policy: UpdatePolicy = UpdatePolicy(),
) {
    private class ManagedList(
        val source: FilterListSource,
        var body: String,
        var metadata: FilterListMetadata,
        var parsed: ParsedFilterList,
        val parsedCosmetic: ParsedCosmeticList,
    )

    private val lists = LinkedHashMap<String, ManagedList>()
    private val lastStatus = LinkedHashMap<String, ListUpdateStatus>()

    /**
     * Initial load: serve every enabled source from cache; download only the
     * ones with no cached copy (when [UpdatePolicy.fetchOnStartup] allows).
     */
    fun startup(nowMillis: Long): Map<String, ListUpdateStatus> {
        lastStatus.clear()
        for (source in sources) {
            if (!source.enabled) {
                record(source.id, ListUpdateStatus.Skipped("disabled"))
                continue
            }
            val cachedBody = cache.loadBody(source)
            if (cachedBody != null) {
                val metadata = cache.loadMetadata(source)
                    ?: FilterListMetadata(sourceId = source.id, downloadedAtMillis = 0L)
                put(source, cachedBody, metadata)
                record(source.id, ListUpdateStatus.LoadedFromCache)
            } else if (policy.fetchOnStartup) {
                when (val result = fetcher.fetch(source.downloadUrl)) {
                    is FetchResult.Success -> storeAndRecord(source, result, nowMillis)
                    is FetchResult.NotModified ->
                        record(source.id, ListUpdateStatus.Failed("not-modified without a cached copy", servedFromCache = false))
                    is FetchResult.Failure ->
                        record(source.id, ListUpdateStatus.Failed(result.reason, servedFromCache = false))
                }
            } else {
                record(source.id, ListUpdateStatus.Skipped("no cached copy; startup fetch disabled"))
            }
        }
        return lastStatus.toMap()
    }

    /**
     * Refresh pass for all enabled sources whose content is due. Conditional
     * validators make unchanged lists a cheap 304; failures never evict the
     * cached copy.
     */
    fun refresh(nowMillis: Long): Map<String, ListUpdateStatus> {
        lastStatus.clear()
        for (source in sources) {
            if (!source.enabled) {
                record(source.id, ListUpdateStatus.Skipped("disabled"))
                continue
            }
            val current = lists[source.id] ?: loadFromCacheOrNull(source)
            if (!policy.isRefreshDue(current?.metadata, nowMillis)) {
                record(source.id, ListUpdateStatus.Skipped("not due"))
                continue
            }
            when (val result = fetcher.fetch(source.downloadUrl, current?.metadata?.etag, current?.metadata?.lastModified)) {
                is FetchResult.Success -> storeAndRecord(source, result, nowMillis)
                is FetchResult.NotModified -> {
                    val cached = current ?: loadFromCacheOrNull(source)
                    if (cached == null) {
                        record(source.id, ListUpdateStatus.Failed("not-modified but no cached copy", servedFromCache = false))
                    } else {
                        cached.metadata = cached.metadata.copy(
                            downloadedAtMillis = nowMillis,
                            etag = result.etag ?: cached.metadata.etag,
                            lastModified = result.lastModified ?: cached.metadata.lastModified,
                        )
                        cache.store(cached.source, cached.body, cached.metadata)
                        record(source.id, ListUpdateStatus.NotModified(cached.metadata.version))
                    }
                }
                is FetchResult.Failure -> {
                    val served = lists[source.id] != null || cache.loadBody(source) != null
                    record(source.id, ListUpdateStatus.Failed(result.reason, servedFromCache = served))
                }
            }
        }
        return lastStatus.toMap()
    }

    /** Current parsed lists — snapshot for [TrackingProtectionEngine]. */
    fun parsedLists(): List<ParsedFilterList> = lists.values.map { it.parsed }

    /**
     * Builds a fresh engine from the current lists. Statistics are per engine
     * instance — hold the returned engine while serving requests.
     */
    fun buildEngine(
        settings: TrackingProtectionSettings = TrackingProtectionSettings(),
    ): TrackingProtectionEngine = TrackingProtectionEngine(parsedLists(), settings)

    fun totalNetworkRules(): Int = lists.values.sumOf { it.parsed.networkRules.size }

    /** Current parsed cosmetic lists — snapshot for [CosmeticFilterEngine]. */
    fun parsedCosmeticLists(): List<ParsedCosmeticList> = lists.values.map { it.parsedCosmetic }

    /**
     * Builds a fresh cosmetic-filter engine from the current lists
     * (stylesheet hiding; see ADR-021 for the v1 subset).
     */
    fun buildCosmeticEngine(): CosmeticFilterEngine = CosmeticFilterEngine(parsedCosmeticLists())

    fun totalCosmeticRules(): Int = lists.values.sumOf { it.parsedCosmetic.rules.size }

    fun metadataOf(sourceId: String): FilterListMetadata? = lists[sourceId]?.metadata

    fun lastReport(): Map<String, ListUpdateStatus> = lastStatus.toMap()

    // --- internals ---------------------------------------------------------

    private fun storeAndRecord(source: FilterListSource, result: FetchResult.Success, nowMillis: Long) {
        val parsed = FilterListParser.parse(result.body, source.id)
        val parsedCosmetic = CosmeticFilterParser.parse(result.body, source.id)
        val metadata = FilterListMetadata(
            sourceId = source.id,
            downloadedAtMillis = nowMillis,
            etag = result.etag,
            lastModified = result.lastModified,
            version = FilterListVersion.parse(result.body).version,
            ruleCount = parsed.networkRules.size,
        )
        cache.store(source, result.body, metadata)
        put(source, result.body, metadata, parsed, parsedCosmetic)
        record(source.id, ListUpdateStatus.Updated(metadata.version, parsed.networkRules.size))
    }

    private fun put(
        source: FilterListSource,
        body: String,
        metadata: FilterListMetadata,
        parsed: ParsedFilterList = FilterListParser.parse(body, source.id),
        parsedCosmetic: ParsedCosmeticList = CosmeticFilterParser.parse(body, source.id),
    ) {
        lists[source.id] = ManagedList(source, body, metadata, parsed, parsedCosmetic)
    }

    private fun loadFromCacheOrNull(source: FilterListSource): ManagedList? {
        val body = cache.loadBody(source) ?: return null
        val metadata = cache.loadMetadata(source)
            ?: FilterListMetadata(sourceId = source.id, downloadedAtMillis = 0L)
        put(source, body, metadata)
        return lists[source.id]
    }

    private fun record(sourceId: String, status: ListUpdateStatus) {
        lastStatus[sourceId] = status
    }
}
