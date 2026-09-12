package com.inweb.browser.shell

/**
 * Page-zoom preferences (MASTER-SPEC §23): a validated default zoom
 * factor plus per-site overrides keyed by site host.
 *
 * Bounds match upstream Chromium's supported page-zoom range,
 * 25%–500%; the settings/ patch binds these preferences to
 * Chromium's OWN zoom mechanism — no custom renderer scaling
 * (Phase 11 design §3). Per-site keys are hosts, normalized by
 * [normalizeHost]; mapping an origin to its host is the engine
 * adapter's job.
 */
data class ZoomPreferences(
    val defaultFactor: Double = DEFAULT_FACTOR,
    val siteZooms: Map<String, Double> = emptyMap(),
) {

    /** The effective zoom for a site: its override, else the default. */
    fun zoomFor(host: String): Double = siteZooms[normalizeHost(host)] ?: defaultFactor

    fun hasSiteZoom(host: String): Boolean = normalizeHost(host) in siteZooms

    /** Sets (or replaces) a site's zoom — validated, immutable copy. */
    fun setSiteZoom(host: String, factor: Double): ZoomResult<ZoomPreferences> {
        val normalized = normalizeHost(host)
        if (normalized.isEmpty()) return ZoomResult.Err(ZoomError.BlankHost)
        if (!isValidFactor(factor)) return ZoomResult.Err(ZoomError.FactorOutOfRange)
        return ZoomResult.Ok(copy(siteZooms = siteZooms + (normalized to factor)))
    }

    /** Removes a site's override — that site falls back to the default. */
    fun removeSiteZoom(host: String): ZoomPreferences =
        copy(siteZooms = siteZooms - normalizeHost(host))

    /**
     * Removes EVERY site override at once (the clear-browsing-data
     * item); the default factor is user preference and stays.
     */
    fun clearSiteZooms(): ZoomPreferences = copy(siteZooms = emptyMap())

    fun setDefaultFactor(factor: Double): ZoomResult<ZoomPreferences> {
        if (!isValidFactor(factor)) return ZoomResult.Err(ZoomError.FactorOutOfRange)
        return ZoomResult.Ok(copy(defaultFactor = factor))
    }

    companion object {
        /** Upstream Chromium's supported page-zoom range: 25%–500%. */
        const val MIN_FACTOR = 0.25
        const val MAX_FACTOR = 5.0
        const val DEFAULT_FACTOR = 1.0

        /**
         * Upstream Chromium's preset page-zoom steps (as factors),
         * ascending — the table the settings surface (§23) offers for
         * the default factor. Every value is inside the supported
         * range, so core validation accepts each one; the engine
         * adapter reuses the same table for the page-zoom control.
         */
        val PRESET_FACTORS: List<Double> = listOf(
            0.25, 0.333, 0.5, 0.667, 0.75, 0.8, 0.9, 1.0,
            1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 5.0,
        )

        /** Bounds check; also rejects NaN and infinities. */
        fun isValidFactor(factor: Double): Boolean =
            factor.isFinite() && factor in MIN_FACTOR..MAX_FACTOR

        /** Site hosts are trimmed and lowercased; never otherwise rewritten. */
        fun normalizeHost(host: String): String = host.trim().lowercase()

        /**
         * Validates untrusted stored data. Checks run in a fixed order,
         * each reporting every offender:
         *
         *  1. the default factor's bounds
         *  2. blank host keys
         *  3. per-site factor bounds
         *  4. hosts that collide after normalization (ambiguous override)
         */
        fun parse(data: ZoomStoreData): ZoomResult<ZoomPreferences> {
            if (!isValidFactor(data.defaultFactor)) {
                return ZoomResult.Err(ZoomError.DefaultFactorOutOfRange(data.defaultFactor))
            }
            val blankHosts = data.siteZooms.keys.filter { normalizeHost(it).isEmpty() }
            if (blankHosts.isNotEmpty()) {
                return ZoomResult.Err(ZoomError.BlankHosts(blankHosts))
            }
            val badFactors = data.siteZooms.entries
                .filter { !isValidFactor(it.value) }
                .map { it.key }
            if (badFactors.isNotEmpty()) {
                return ZoomResult.Err(ZoomError.SiteFactorsOutOfRange(badFactors))
            }
            val normalized = LinkedHashMap<String, Double>()
            val ambiguous = mutableListOf<String>()
            for ((host, factor) in data.siteZooms) {
                val key = normalizeHost(host)
                if (normalized.containsKey(key)) {
                    ambiguous.add(key)
                } else {
                    normalized[key] = factor
                }
            }
            if (ambiguous.isNotEmpty()) {
                return ZoomResult.Err(ZoomError.AmbiguousHosts(ambiguous.distinct()))
            }
            return ZoomResult.Ok(ZoomPreferences(data.defaultFactor, normalized))
        }
    }
}

/** Why a zoom preference failed — results, not exceptions. */
sealed class ZoomError {
    /** An operation named a blank host. */
    object BlankHost : ZoomError()

    /** An operation supplied a factor outside 25%–500% (or NaN/∞). */
    object FactorOutOfRange : ZoomError()

    data class DefaultFactorOutOfRange(val factor: Double) : ZoomError()

    data class BlankHosts(val hosts: List<String>) : ZoomError()

    data class SiteFactorsOutOfRange(val hosts: List<String>) : ZoomError()

    /** Stored hosts that collide after normalization — ambiguous. */
    data class AmbiguousHosts(val hosts: List<String>) : ZoomError()
}

sealed class ZoomResult<out T> {
    data class Ok<T>(val value: T) : ZoomResult<T>()
    data class Err(val error: ZoomError) : ZoomResult<Nothing>()
}

/** What the persistence seam round-trips (the wire form). */
data class ZoomStoreData(
    val defaultFactor: Double,
    val siteZooms: Map<String, Double>,
)

/** Persistence seam; the patch layer provides the real store (B-001). */
interface ZoomPreferencesStore {
    fun load(): ZoomStoreData?
    fun save(data: ZoomStoreData)
}

/** In-memory store (tests, and until the preference-backed store ships). */
class InMemoryZoomPreferencesStore : ZoomPreferencesStore {
    private var data: ZoomStoreData? = null
    override fun load(): ZoomStoreData? = data
    override fun save(data: ZoomStoreData) {
        this.data = data.copy(siteZooms = data.siteZooms.toMap())
    }
}

/**
 * Holds the zoom preferences (§23): every mutation validated and
 * persisted through the seam. Corrupt stored data follows the
 * ADR-029/030 recovery rule: fall back to defaults, PERSIST the
 * repair, and report what was rejected via [lastRecovery] — never a
 * crash, never a silent ignore.
 */
class ZoomSettings(private val store: ZoomPreferencesStore = InMemoryZoomPreferencesStore()) {

    private var prefs: ZoomPreferences
    private var recovery: ZoomError? = null

    init {
        when (val stored = store.load()) {
            null -> {
                prefs = ZoomPreferences()
                persist()
            }
            else -> when (val parsed = ZoomPreferences.parse(stored)) {
                is ZoomResult.Ok -> prefs = parsed.value
                is ZoomResult.Err -> {
                    recovery = parsed.error
                    prefs = ZoomPreferences()
                    persist()
                }
            }
        }
    }

    fun current(): ZoomPreferences = prefs

    fun lastRecovery(): ZoomError? = recovery

    fun zoomFor(host: String): Double = prefs.zoomFor(host)

    fun setDefaultFactor(factor: Double): ZoomResult<ZoomPreferences> {
        val next = prefs.setDefaultFactor(factor)
        if (next is ZoomResult.Err) return next
        prefs = (next as ZoomResult.Ok).value
        persist()
        return ZoomResult.Ok(prefs)
    }

    fun setSiteZoom(host: String, factor: Double): ZoomResult<ZoomPreferences> {
        val next = prefs.setSiteZoom(host, factor)
        if (next is ZoomResult.Err) return next
        prefs = (next as ZoomResult.Ok).value
        persist()
        return ZoomResult.Ok(prefs)
    }

    fun removeSiteZoom(host: String): ZoomPreferences {
        prefs = prefs.removeSiteZoom(host)
        persist()
        return prefs
    }

    /** Clears every per-site override (clear-browsing-data); the default stays. */
    fun clearSiteZooms(): ZoomPreferences {
        prefs = prefs.clearSiteZooms()
        persist()
        return prefs
    }

    /** Restores the identity default (100%, no site overrides). */
    fun reset(): ZoomPreferences {
        prefs = ZoomPreferences()
        persist()
        return prefs
    }

    private fun persist() =
        store.save(ZoomStoreData(defaultFactor = prefs.defaultFactor, siteZooms = prefs.siteZooms))
}
