package com.inweb.browser.notifications

/**
 * The v1 notification channel plan (§33, design §1 — ADR-028).
 *
 * Stable string ids are the Android channel ids. The set is EXACTLY
 * the design table: downloads, security, vpn, background — nothing
 * else. Sync and update channels are deliberately absent (no sync
 * exists, §29/ADR-026; no self-update infrastructure), and a
 * promotional channel is forbidden by policy (§41) — absences are
 * structural (no code path), not just undocumented.
 */
enum class NotificationChannel(val id: String) {
    DOWNLOADS("downloads"),
    SECURITY("security"),
    VPN("vpn"),
    BACKGROUND("background"),
}

/**
 * Every real, user-relevant event v1 may notify — exactly the design
 * §1 table:
 *
 *  - DOWNLOADS: download complete / failed (source real today — the
 *    Phase 2 downloads core)
 *  - SECURITY: backup failed, app-lock repeated failures (sources
 *    real when the Phase 8/9 patches land)
 *  - VPN: connected / disconnected / reconnecting (source real when
 *    patch 0022 lands)
 *  - BACKGROUND: offline page saved (source real when 0018 lands)
 *
 * Deliberately NOT events: filter-list update failure (silent by
 * design — a persistent failure surfaces in the Security Center, not
 * as a notification), and every sync / update / promotional event
 * (no infrastructure behind them; §57 forbids the claim).
 */
enum class NotificationEvent(val channel: NotificationChannel) {
    DOWNLOAD_COMPLETE(NotificationChannel.DOWNLOADS),
    DOWNLOAD_FAILED(NotificationChannel.DOWNLOADS),
    BACKUP_FAILED(NotificationChannel.SECURITY),
    APP_LOCK_REPEATED_FAILURES(NotificationChannel.SECURITY),
    VPN_CONNECTED(NotificationChannel.VPN),
    VPN_DISCONNECTED(NotificationChannel.VPN),
    VPN_RECONNECTING(NotificationChannel.VPN),
    OFFLINE_PAGE_SAVED(NotificationChannel.BACKGROUND),
}

/** POST_NOTIFICATIONS request phase (Android 13+; lazy per ADR-028). */
enum class PermissionPhase {
    /** Nothing shown yet — permission is never asked at startup. */
    NOT_REQUESTED,

    /** The ask is in flight, triggered by the first real event. */
    REQUESTED,

    GRANTED,
    DENIED,
}

/** Why a real event was not shown. */
enum class SuppressReason {
    /** The event's source is not real in this build — no stub events. */
    CHANNEL_NOT_AVAILABLE,

    /** The user turned the channel off. */
    CHANNEL_DISABLED_BY_USER,

    /** The permission ask is already in flight (never asked twice). */
    PERMISSION_PENDING,

    /** The user denied; the policy never re-asks (no nagging). */
    PERMISSION_DENIED,
}

sealed class NotificationDecision {
    data class Show(
        val event: NotificationEvent,
        val channel: NotificationChannel,
    ) : NotificationDecision()

    /** The FIRST show-worthy event triggers the lazy permission ask. */
    data class RequestPermission(val event: NotificationEvent) : NotificationDecision()

    data class Suppress(
        val event: NotificationEvent,
        val reason: SuppressReason,
    ) : NotificationDecision()
}

/** Why an operation or stored state failed — results, not exceptions. */
sealed class NotificationError {
    /** [NotificationPolicy.onPermissionResult] called outside REQUESTED. */
    object InvalidTransition : NotificationError()

    /**
     * Stored preferences failed validation; defaults were restored and
     * the repair persisted (never a crash, never a silent ignore).
     */
    data class CorruptStoredData(
        val unknownChannelIds: List<String>,
        val unknownPermissionPhase: String?,
    ) : NotificationError()
}

sealed class NotificationResult<out T> {
    data class Ok<T>(val value: T) : NotificationResult<T>()
    data class Err(val error: NotificationError) : NotificationResult<Nothing>()
}

/** What the persistence seam round-trips (stable ids — the wire form). */
data class NotificationStoreData(
    /** Channel ids the user turned OFF (everything else is on). */
    val disabledChannels: List<String>,
    /** [PermissionPhase] name. */
    val permissionPhase: String,
)

/** Persistence seam; the patch layer provides the real store (B-001). */
interface NotificationStore {
    fun load(): NotificationStoreData?
    fun save(data: NotificationStoreData)
}

/** In-memory store (tests, and until the preference-backed store ships). */
class InMemoryNotificationStore : NotificationStore {
    private var data: NotificationStoreData? = null
    override fun load(): NotificationStoreData? = data
    override fun save(data: NotificationStoreData) {
        this.data = data.copy(disabledChannels = data.disabledChannels.toList())
    }
}

/**
 * The §33 notification policy: which real events may notify, on which
 * channel, given the user's per-channel toggles and the permission
 * phase — minimal by default, permission asked lazily at the first
 * REAL show-worthy event, zero promotional paths (ADR-028).
 *
 * [availableChannels] is REQUIRED with no default: the build must
 * state explicitly which event sources are real in it (the design §1
 * table gates VPN on patch 0022, offline-page events on 0019, and so
 * on). A channel that is not available is never registered and its
 * events are never shown — no stub channels, no stub events.
 *
 * The ONLY entry point to a notification is [decide] with a typed
 * [NotificationEvent] — there is no generic "notify(channel, text)"
 * API, so promotional or unlisted content has no way in.
 *
 * Check order in [decide] (documented, fixed):
 *  1. availability — a structural fact, not a preference
 *  2. the user's channel toggle
 *  3. the permission phase — the lazy ask fires ONLY for an event
 *     that would otherwise be shown (never for a suppressed one).
 */
class NotificationPolicy(
    private val availableChannels: Set<NotificationChannel>,
    private val store: NotificationStore = InMemoryNotificationStore(),
) {

    private val disabled = mutableSetOf<NotificationChannel>()
    private var phase = PermissionPhase.NOT_REQUESTED
    private var recovery: NotificationError? = null

    init {
        when (val stored = store.load()) {
            null -> persist()
            else -> {
                val unknownIds = stored.disabledChannels
                    .filter { NotificationChannel.entries.firstOrNull { c -> c.id == it } == null }
                val phaseFromName = PermissionPhase.entries.firstOrNull { it.name == stored.permissionPhase }
                if (unknownIds.isNotEmpty() || phaseFromName == null) {
                    recovery = NotificationError.CorruptStoredData(
                        unknownChannelIds = unknownIds,
                        unknownPermissionPhase = if (phaseFromName == null) stored.permissionPhase else null,
                    )
                    disabled.clear()
                    phase = PermissionPhase.NOT_REQUESTED
                    persist()
                } else {
                    stored.disabledChannels.forEach { id ->
                        NotificationChannel.entries.firstOrNull { c -> c.id == id }
                            ?.let { disabled.add(it) }
                    }
                    phase = phaseFromName
                }
            }
        }
    }

    /** Whether the build's event source for [channel] is real. */
    fun isChannelAvailable(channel: NotificationChannel): Boolean =
        channel in availableChannels

    /** The user toggle — every channel is off-able (§33). */
    fun isChannelEnabled(channel: NotificationChannel): Boolean =
        channel !in disabled

    /** What a corrupt stored state was rejected for, or null. */
    fun lastRecovery(): NotificationError? = recovery

    /** The channels this build registers (the available set, in plan order). */
    fun registeredChannels(): List<NotificationChannel> =
        NotificationChannel.entries.filter { it in availableChannels }

    fun permissionPhase(): PermissionPhase = phase

    /** Turns a channel off/on; every known channel is toggleable. */
    fun setChannelEnabled(channel: NotificationChannel, enabled: Boolean) {
        if (enabled) disabled.remove(channel) else disabled.add(channel)
        persist()
    }

    /**
     * Decides what a real [event] produces. The decision is one of:
     * show it, ask the permission (first show-worthy event only), or
     * suppress it with an honest reason.
     */
    fun decide(event: NotificationEvent): NotificationDecision {
        val channel = event.channel
        if (channel !in availableChannels) {
            return NotificationDecision.Suppress(event, SuppressReason.CHANNEL_NOT_AVAILABLE)
        }
        if (channel in disabled) {
            return NotificationDecision.Suppress(event, SuppressReason.CHANNEL_DISABLED_BY_USER)
        }
        return when (phase) {
            PermissionPhase.NOT_REQUESTED -> {
                phase = PermissionPhase.REQUESTED
                persist()
                NotificationDecision.RequestPermission(event)
            }
            PermissionPhase.REQUESTED ->
                NotificationDecision.Suppress(event, SuppressReason.PERMISSION_PENDING)
            PermissionPhase.GRANTED ->
                NotificationDecision.Show(event, channel)
            PermissionPhase.DENIED ->
                NotificationDecision.Suppress(event, SuppressReason.PERMISSION_DENIED)
        }
    }

    /**
     * The result of the system permission dialog. Valid only in
     * REQUESTED (the ask happened because of a real event). After a
     * grant, the caller re-decides the event that triggered the ask.
     */
    fun onPermissionResult(granted: Boolean): NotificationResult<Unit> {
        if (phase != PermissionPhase.REQUESTED) {
            return NotificationResult.Err(NotificationError.InvalidTransition)
        }
        phase = if (granted) PermissionPhase.GRANTED else PermissionPhase.DENIED
        persist()
        return NotificationResult.Ok(Unit)
    }

    /**
     * An OBSERVED system fact (e.g. the user changed the notification
     * setting outside the app, or the platform grants automatically
     * below Android 13). Authoritative from any phase — unlike the
     * dialog result, this is not a request but an observation.
     */
    fun onSystemPermissionChanged(granted: Boolean) {
        phase = if (granted) PermissionPhase.GRANTED else PermissionPhase.DENIED
        persist()
    }

    private fun persist() = store.save(
        NotificationStoreData(
            disabledChannels = disabled.map { it.id }.sorted(),
            permissionPhase = phase.name,
        )
    )
}
