package com.inweb.browser.extensions

/**
 * Chrome-style extension version: 1–4 dot-separated integer components
 * (`1`, `1.2`, `1.2.3`, `1.2.3.4`), compared numerically with zero padding
 * (`1.0 == 1.0.0`, `1.2 < 1.10`). Trailing zero components are normalized
 * away at parse time so equality, ordering, and hashing all agree.
 */
data class ExtensionVersion private constructor(val parts: List<Int>) : Comparable<ExtensionVersion> {

    override fun compareTo(other: ExtensionVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val a = parts.getOrElse(i) { 0 }
            val b = other.parts.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    companion object {
        const val MAX_COMPONENTS = 4

        /** Returns null for anything that is not a valid version string. */
        fun parse(raw: String): ExtensionVersion? {
            if (raw.isEmpty()) return null
            val components = raw.split('.')
            if (components.size !in 1..MAX_COMPONENTS) return null
            val parts = mutableListOf<Int>()
            for (component in components) {
                if (component.isEmpty() || component.any { !it.isDigit() }) return null
                val value = component.toIntOrNull() ?: return null
                parts += value
            }
            // normalize: drop trailing zeros so 1.0 == 1.0.0 (keep at least one part)
            while (parts.size > 1 && parts.last() == 0) parts.removeAt(parts.size - 1)
            return ExtensionVersion(parts)
        }
    }
}

/** Manifest version — MV3 is primary; MV2 installs on a stated grace window (ADR-023). */
enum class ManifestVersion { V2, V3 }

/** A sideloaded extension's identity + permission set, as parsed by the installer. */
data class ExtensionRecord(
    val id: String,
    val name: String,
    val version: ExtensionVersion,
    val manifestVersion: ManifestVersion,
    val permissions: Set<String>,
)

/**
 * Lifecycle states:
 *  - [PENDING_REVIEW]     installed, warnings not yet reviewed — cannot be enabled
 *  - [DISABLED]           reviewed but not enabled (or user-disabled)
 *  - [ENABLED]            active
 *  - [DISABLED_UPDATE]    an update introduced NEW permissions — blocked until
 *                          they are reviewed (Chrome's upgrade-consent behavior)
 */
enum class ExtensionState { PENDING_REVIEW, DISABLED, ENABLED, DISABLED_UPDATE }

/** One managed extension: its current record, state, and reviewed permissions. */
data class InstalledExtension(
    val record: ExtensionRecord,
    val state: ExtensionState,
    /** Permissions the user has reviewed (warning surface shown). */
    val reviewedPermissions: Set<String>,
) {
    val reviewComplete: Boolean get() = record.permissions.all { it in reviewedPermissions }
}

/** Real counts only — no fabricated numbers (§24 discipline). */
data class RegistryCounts(
    val installed: Int,
    val pendingReview: Int,
    val enabled: Int,
    val disabled: Int,
    val awaitingPermissionReview: Int,
)

/** Persistence seam; the patch layer provides the real store (B-001). */
interface ExtensionStore {
    fun load(): List<InstalledExtension>
    fun save(entries: List<InstalledExtension>)
}

/** In-memory store (tests, and until the file-backed store ships with the patches). */
class InMemoryExtensionStore : ExtensionStore {
    private var entries: List<InstalledExtension> = emptyList()
    override fun load(): List<InstalledExtension> = entries
    override fun save(entries: List<InstalledExtension>) {
        this.entries = entries.toList()
    }
}

/** Why a registry operation failed — expected failures are results, not exceptions. */
sealed class RegistryError {
    object NotFound : RegistryError()
    object DuplicateInstall : RegistryError()
    data class IdMismatch(val expected: String, val provided: String) : RegistryError()
    data class VersionNotNewer(val current: String, val provided: String) : RegistryError()
    object ReviewRequired : RegistryError()
    object InvalidState : RegistryError()
}

sealed class RegistryResult<out T> {
    data class Ok<T>(val value: T) : RegistryResult<T>()
    data class Err(val error: RegistryError) : RegistryResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): RegistryResult<R> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }
}

/**
 * The extension management registry (§16 models): every state change is
 * explicit, reviewed, and persisted. No silent installs, no implicit
 * permission grants, no enable without review — ever.
 */
class ExtensionRegistry(private val store: ExtensionStore = InMemoryExtensionStore()) {

    private val entries = LinkedHashMap<String, InstalledExtension>()

    init {
        store.load().forEach { entries[it.record.id] = it }
    }

    /** Registers a freshly sideloaded extension — always pending review first. */
    fun install(record: ExtensionRecord): RegistryResult<InstalledExtension> {
        if (entries.containsKey(record.id)) return RegistryResult.Err(RegistryError.DuplicateInstall)
        val entry = InstalledExtension(record, ExtensionState.PENDING_REVIEW, emptySet())
        entries[record.id] = entry
        persist()
        return RegistryResult.Ok(entry)
    }

    /**
     * Marks the CURRENT permission set as reviewed (the warning surface was
     * shown). PENDING_REVIEW → DISABLED; DISABLED_UPDATE → DISABLED.
     */
    fun reviewPermissions(id: String): RegistryResult<InstalledExtension> {
        val entry = entries[id] ?: return RegistryResult.Err(RegistryError.NotFound)
        val state = when (entry.state) {
            ExtensionState.PENDING_REVIEW, ExtensionState.DISABLED_UPDATE -> ExtensionState.DISABLED
            else -> entry.state
        }
        val updated = entry.copy(
            state = state,
            reviewedPermissions = entry.record.permissions,
        )
        entries[id] = updated
        persist()
        return RegistryResult.Ok(updated)
    }

    /** User action — allowed only after review is complete. */
    fun enable(id: String): RegistryResult<InstalledExtension> {
        val entry = entries[id] ?: return RegistryResult.Err(RegistryError.NotFound)
        if (!entry.reviewComplete) return RegistryResult.Err(RegistryError.ReviewRequired)
        if (entry.state == ExtensionState.ENABLED) return RegistryResult.Ok(entry)
        if (entry.state != ExtensionState.DISABLED) return RegistryResult.Err(RegistryError.InvalidState)
        val updated = entry.copy(state = ExtensionState.ENABLED)
        entries[id] = updated
        persist()
        return RegistryResult.Ok(updated)
    }

    /** User action — only a running extension can be disabled. */
    fun disable(id: String): RegistryResult<InstalledExtension> {
        val entry = entries[id] ?: return RegistryResult.Err(RegistryError.NotFound)
        if (entry.state != ExtensionState.ENABLED) return RegistryResult.Err(RegistryError.InvalidState)
        val updated = entry.copy(state = ExtensionState.DISABLED)
        entries[id] = updated
        persist()
        return RegistryResult.Ok(updated)
    }

    /**
     * Sideload update: requires a strictly newer version of the SAME id.
     * If the update introduces permissions the user has not reviewed, the
     * extension lands in DISABLED_UPDATE until they are reviewed — even if
     * it was enabled (upgrade consent). A never-reviewed extension stays
     * PENDING_REVIEW.
     */
    fun update(id: String, newRecord: ExtensionRecord): RegistryResult<InstalledExtension> {
        val entry = entries[id] ?: return RegistryResult.Err(RegistryError.NotFound)
        if (newRecord.id != id) {
            return RegistryResult.Err(RegistryError.IdMismatch(id, newRecord.id))
        }
        if (newRecord.version <= entry.record.version) {
            return RegistryResult.Err(
                RegistryError.VersionNotNewer(entry.record.version.toString(), newRecord.version.toString()),
            )
        }
        val newPermissions = newRecord.permissions - entry.reviewedPermissions
        val state = when {
            entry.state == ExtensionState.PENDING_REVIEW -> ExtensionState.PENDING_REVIEW
            newPermissions.isNotEmpty() -> ExtensionState.DISABLED_UPDATE
            else -> entry.state
        }
        val updated = entry.copy(record = newRecord, state = state)
        entries[id] = updated
        persist()
        return RegistryResult.Ok(updated)
    }

    fun remove(id: String): RegistryResult<Unit> {
        if (entries.remove(id) == null) return RegistryResult.Err(RegistryError.NotFound)
        persist()
        return RegistryResult.Ok(Unit)
    }

    fun get(id: String): InstalledExtension? = entries[id]

    /** Insertion order, oldest first (project-wide order contract). */
    fun all(): List<InstalledExtension> = entries.values.toList()

    fun counts(): RegistryCounts = RegistryCounts(
        installed = entries.size,
        pendingReview = entries.values.count { it.state == ExtensionState.PENDING_REVIEW },
        enabled = entries.values.count { it.state == ExtensionState.ENABLED },
        disabled = entries.values.count { it.state == ExtensionState.DISABLED },
        awaitingPermissionReview = entries.values.count { it.state == ExtensionState.DISABLED_UPDATE },
    )

    private fun persist() = store.save(entries.values.toList())
}
