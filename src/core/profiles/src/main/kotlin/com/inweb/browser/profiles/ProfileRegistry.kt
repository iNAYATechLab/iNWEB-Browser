package com.inweb.browser.profiles

/** One browser profile (§28). */
data class ProfileRecord(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
)

/** What the persistence seam round-trips. */
data class ProfileStoreData(
    val profiles: List<ProfileRecord>,
    val activeProfileId: String?,
)

/** Persistence seam; the patch layer provides the real store (B-001). */
interface ProfileStore {
    fun load(): ProfileStoreData
    fun save(data: ProfileStoreData)
}

/** In-memory store (tests, and until the file-backed store ships with the patches). */
class InMemoryProfileStore : ProfileStore {
    private var data = ProfileStoreData(emptyList(), null)
    override fun load(): ProfileStoreData = data
    override fun save(data: ProfileStoreData) {
        this.data = data.copy(profiles = data.profiles.toList())
    }
}

/** Why a registry operation failed — expected failures are results, not exceptions. */
sealed class ProfileError {
    object NotFound : ProfileError()
    object BlankName : ProfileError()
    /** The last remaining profile cannot be deleted. */
    object LastProfile : ProfileError()
}

sealed class ProfileResult<out T> {
    data class Ok<T>(val value: T) : ProfileResult<T>()
    data class Err(val error: ProfileError) : ProfileResult<Nothing>()
}

/** A deleted profile: its record (for teardown) and the new active profile id. */
data class DeletedProfile(
    val record: ProfileRecord,
    /** The profile that becomes active after this deletion (never null here). */
    val newActiveProfileId: String,
)

/**
 * The profile registry (§28): profile records, active-profile selection,
 * and per-profile store routing.
 *
 * Isolation contract: every iNWEB-owned store is created against
 * [namespaceOf] of ONE profile; a namespace is the profile id itself, so
 * the app layer maps it to a per-profile data directory. Profile ids are
 * never reused — a deleted profile's namespace can never silently
 * resurrect stale data.
 */
class ProfileRegistry(private val store: ProfileStore = InMemoryProfileStore()) {

    private val profiles = LinkedHashMap<String, ProfileRecord>()
    private var activeId: String? = null

    init {
        val loaded = store.load()
        loaded.profiles.forEach { profiles[it.id] = it }
        activeId = loaded.activeProfileId?.takeIf { it in profiles }
        if (profiles.isEmpty()) {
            // a real browser always has exactly one profile to start with
            val default = ProfileRecord(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME, 0L)
            profiles[default.id] = default
            activeId = default.id
            persist()
        }
    }

    /** Creates a NEW profile (inactive); ids are monotonic and never reused. */
    fun create(name: String, nowMillis: Long): ProfileResult<ProfileRecord> {
        if (name.isBlank()) return ProfileResult.Err(ProfileError.BlankName)
        val id = nextId()
        val record = ProfileRecord(id, name.trim(), nowMillis)
        profiles[id] = record
        persist()
        return ProfileResult.Ok(record)
    }

    fun rename(id: String, newName: String): ProfileResult<ProfileRecord> {
        if (newName.isBlank()) return ProfileResult.Err(ProfileError.BlankName)
        val current = profiles[id] ?: return ProfileResult.Err(ProfileError.NotFound)
        val updated = current.copy(name = newName.trim())
        profiles[id] = updated
        persist()
        return ProfileResult.Ok(updated)
    }

    fun setActive(id: String): ProfileResult<ProfileRecord> {
        val record = profiles[id] ?: return ProfileResult.Err(ProfileError.NotFound)
        activeId = id
        persist()
        return ProfileResult.Ok(record)
    }

    fun activeProfile(): ProfileRecord? = activeId?.let { profiles[it] }

    fun get(id: String): ProfileRecord? = profiles[id]

    /** Insertion order, oldest first (project-wide order contract). */
    fun all(): List<ProfileRecord> = profiles.values.toList()

    /**
     * The store namespace for a profile (its id). The app layer prefixes
     * its data root: `<dataDir>/profiles/<namespace>/`. Stores built for
     * one namespace must never receive another's path — that is the §28
     * isolation rule, enforced at store construction.
     */
    fun namespaceOf(id: String): ProfileResult<String> {
        if (id !in profiles) return ProfileResult.Err(ProfileError.NotFound)
        return ProfileResult.Ok(id)
    }

    /**
     * Deletes a profile and retires its namespace (the caller deletes the
     * profile's data directory). Deleting the ACTIVE profile is allowed —
     * active falls back to the oldest remaining profile (the caller
     * tears down and reloads the browsing surface). The last remaining
     * profile cannot be deleted.
     */
    fun delete(id: String): ProfileResult<DeletedProfile> {
        val record = profiles[id] ?: return ProfileResult.Err(ProfileError.NotFound)
        if (profiles.size == 1) return ProfileResult.Err(ProfileError.LastProfile)
        profiles.remove(id)
        if (activeId == id) {
            activeId = profiles.values.first().id
        }
        persist()
        return ProfileResult.Ok(DeletedProfile(record = record, newActiveProfileId = activeId!!))
    }

    private fun nextId(): String {
        var max = 0
        for (key in profiles.keys) {
            val n = key.removePrefix(ID_PREFIX).toIntOrNull() ?: continue
            if (n > max) max = n
        }
        return "$ID_PREFIX${max + 1}"
    }

    private fun persist() =
        store.save(ProfileStoreData(profiles = profiles.values.toList(), activeProfileId = activeId))

    companion object {
        const val ID_PREFIX = "p-"
        const val DEFAULT_PROFILE_ID = "p-1"
        const val DEFAULT_PROFILE_NAME = "Default"
    }
}
