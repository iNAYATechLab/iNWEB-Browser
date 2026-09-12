package com.inweb.browser.notifications

import com.inweb.browser.notifications.NotificationChannel.BACKGROUND
import com.inweb.browser.notifications.NotificationChannel.DOWNLOADS
import com.inweb.browser.notifications.NotificationChannel.SECURITY
import com.inweb.browser.notifications.NotificationChannel.VPN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {

    /** All four sources real — the fully-patched build. */
    private fun fullyPatched(): NotificationPolicy =
        NotificationPolicy(NotificationChannel.entries.toSet())

    private fun suppressed(decision: NotificationDecision): SuppressReason =
        (decision as NotificationDecision.Suppress).reason

    // --- the registry IS the §1 design table (auditable) ----------------------

    @Test
    fun channelRegistryMatchesTheDesignTableExactly() {
        assertEquals(
            listOf("downloads", "security", "vpn", "background"),
            NotificationChannel.entries.map { it.id },
        )
    }

    @Test
    fun everyEventMapsToItsDesignTableChannel() {
        assertEquals(DOWNLOADS, NotificationEvent.DOWNLOAD_COMPLETE.channel)
        assertEquals(DOWNLOADS, NotificationEvent.DOWNLOAD_FAILED.channel)
        assertEquals(SECURITY, NotificationEvent.BACKUP_FAILED.channel)
        assertEquals(SECURITY, NotificationEvent.APP_LOCK_REPEATED_FAILURES.channel)
        assertEquals(VPN, NotificationEvent.VPN_CONNECTED.channel)
        assertEquals(VPN, NotificationEvent.VPN_DISCONNECTED.channel)
        assertEquals(VPN, NotificationEvent.VPN_RECONNECTING.channel)
        assertEquals(BACKGROUND, NotificationEvent.OFFLINE_PAGE_SAVED.channel)
        assertEquals(8, NotificationEvent.entries.size)
    }

    @Test
    fun deliberateAbsencesAreStructuralNotJustUndocumented() {
        // sync: no backend (§29, ADR-026)
        assertThrows(IllegalArgumentException::class.java) {
            NotificationEvent.valueOf("SYNC_COMPLETE")
        }
        // self-update: no infrastructure
        assertThrows(IllegalArgumentException::class.java) {
            NotificationEvent.valueOf("UPDATE_AVAILABLE")
        }
        // promotional / engagement: forbidden by policy (§41)
        assertThrows(IllegalArgumentException::class.java) {
            NotificationEvent.valueOf("NEW_FEATURE_TIP")
        }
        // filter-list update failure: silent by design — Security Center, not a notification
        assertThrows(IllegalArgumentException::class.java) {
            NotificationEvent.valueOf("FILTER_LIST_UPDATE_FAILED")
        }
    }

    // --- defaults, seeding, persistence ---------------------------------------

    @Test
    fun defaultPolicyEnablesEveryKnownChannel() {
        val policy = fullyPatched()
        for (channel in NotificationChannel.entries) {
            assertTrue(policy.isChannelEnabled(channel))
        }
        assertEquals(PermissionPhase.NOT_REQUESTED, policy.permissionPhase())
        assertNull(policy.lastRecovery())
    }

    @Test
    fun emptyStoreIsSeededAndPersisted() {
        val store = InMemoryNotificationStore()
        NotificationPolicy(NotificationChannel.entries.toSet(), store)
        assertEquals(
            NotificationStoreData(disabledChannels = emptyList(), permissionPhase = "NOT_REQUESTED"),
            store.load(),
        )
    }

    @Test
    fun validStoredStateLoadsAsIs() {
        val store = InMemoryNotificationStore()
        store.save(
            NotificationStoreData(
                disabledChannels = listOf("vpn", "background"),
                permissionPhase = "GRANTED",
            )
        )
        val policy = NotificationPolicy(setOf(DOWNLOADS, SECURITY, VPN, BACKGROUND), store)
        assertFalse(policy.isChannelEnabled(VPN))
        assertFalse(policy.isChannelEnabled(BACKGROUND))
        assertTrue(policy.isChannelEnabled(DOWNLOADS))
        assertEquals(PermissionPhase.GRANTED, policy.permissionPhase())
        assertNull(policy.lastRecovery())
    }

    @Test
    fun corruptUnknownChannelIdsRecoverToDefaultsAndPersistTheRepair() {
        val store = InMemoryNotificationStore()
        store.save(
            NotificationStoreData(
                disabledChannels = listOf("downloads", "promos"),
                permissionPhase = "GRANTED",
            )
        )
        val policy = NotificationPolicy(NotificationChannel.entries.toSet(), store)
        val recovery = policy.lastRecovery() as NotificationError.CorruptStoredData
        assertEquals(listOf("promos"), recovery.unknownChannelIds)
        assertNull(recovery.unknownPermissionPhase)
        for (channel in NotificationChannel.entries) {
            assertTrue(policy.isChannelEnabled(channel)) // defaults restored
        }
        assertEquals(PermissionPhase.NOT_REQUESTED, policy.permissionPhase())
        assertEquals(
            NotificationStoreData(disabledChannels = emptyList(), permissionPhase = "NOT_REQUESTED"),
            store.load(),
        )
    }

    @Test
    fun corruptPermissionPhaseRecoversToDefaults() {
        val store = InMemoryNotificationStore()
        store.save(NotificationStoreData(disabledChannels = listOf("vpn"), permissionPhase = "maybe"))
        val policy = NotificationPolicy(NotificationChannel.entries.toSet(), store)
        val recovery = policy.lastRecovery() as NotificationError.CorruptStoredData
        assertTrue(recovery.unknownChannelIds.isEmpty())
        assertEquals("maybe", recovery.unknownPermissionPhase)
        assertTrue(policy.isChannelEnabled(VPN))
        assertEquals(PermissionPhase.NOT_REQUESTED, policy.permissionPhase())
    }

    @Test
    fun corruptIdsAndPhaseAreBothReported() {
        val store = InMemoryNotificationStore()
        store.save(NotificationStoreData(disabledChannels = listOf("ads"), permissionPhase = "nope"))
        val policy = NotificationPolicy(NotificationChannel.entries.toSet(), store)
        val recovery = policy.lastRecovery() as NotificationError.CorruptStoredData
        assertEquals(listOf("ads"), recovery.unknownChannelIds)
        assertEquals("nope", recovery.unknownPermissionPhase)
    }

    // --- availability: no stub channels, no stub events ------------------------

    @Test
    fun registeredChannelsAreExactlyTheAvailableOnesInPlanOrder() {
        val policy = NotificationPolicy(setOf(DOWNLOADS))
        assertEquals(listOf(DOWNLOADS), policy.registeredChannels())
        assertFalse(policy.isChannelAvailable(VPN))

        val patched = NotificationPolicy(setOf(DOWNLOADS, VPN))
        assertEquals(listOf(DOWNLOADS, VPN), patched.registeredChannels())
    }

    @Test
    fun unavailableChannelEventsAreSuppressedEvenWhenEnabledAndGranted() {
        // a build without patch 0021: the VPN channel is absent, NOT stubbed
        val policy = NotificationPolicy(setOf(DOWNLOADS))
        policy.onSystemPermissionChanged(true)
        val decision = policy.decide(NotificationEvent.VPN_CONNECTED)
        assertEquals(SuppressReason.CHANNEL_NOT_AVAILABLE, suppressed(decision))
    }

    // --- per-channel toggles (every channel off-able, §33) ---------------------

    @Test
    fun everyChannelIsToggleable() {
        val policy = fullyPatched()
        policy.onSystemPermissionChanged(true)
        for (channel in NotificationChannel.entries) {
            policy.setChannelEnabled(channel, false)
            assertFalse(policy.isChannelEnabled(channel))
        }
        for (channel in NotificationChannel.entries) {
            policy.setChannelEnabled(channel, true)
            assertTrue(policy.isChannelEnabled(channel))
        }
    }

    @Test
    fun disabledChannelEventsAreSuppressedAndReEnabledOnesShowAgain() {
        val policy = fullyPatched()
        policy.onSystemPermissionChanged(true)
        policy.setChannelEnabled(DOWNLOADS, false)
        assertEquals(
            SuppressReason.CHANNEL_DISABLED_BY_USER,
            suppressed(policy.decide(NotificationEvent.DOWNLOAD_COMPLETE)),
        )
        policy.setChannelEnabled(DOWNLOADS, true)
        assertTrue(policy.decide(NotificationEvent.DOWNLOAD_COMPLETE) is NotificationDecision.Show)
    }

    @Test
    fun togglesArePersistedInTheWireForm() {
        val store = InMemoryNotificationStore()
        val policy = NotificationPolicy(NotificationChannel.entries.toSet(), store)
        policy.setChannelEnabled(BACKGROUND, false)
        policy.setChannelEnabled(VPN, false)
        assertEquals(listOf("background", "vpn"), store.load()?.disabledChannels)
    }

    // --- the lazy permission ask (§41-aligned, ADR-028) ------------------------

    @Test
    fun firstShowWorthyEventRequestsThePermissionLazily() {
        val store = InMemoryNotificationStore()
        val policy = NotificationPolicy(setOf(DOWNLOADS), store)
        val decision = policy.decide(NotificationEvent.DOWNLOAD_FAILED)
        assertTrue(decision is NotificationDecision.RequestPermission)
        assertEquals(NotificationEvent.DOWNLOAD_FAILED, (decision as NotificationDecision.RequestPermission).event)
        assertEquals(PermissionPhase.REQUESTED, policy.permissionPhase())
        assertEquals("REQUESTED", store.load()?.permissionPhase)
    }

    @Test
    fun suppressedEventsNeverTriggerThePermissionAsk() {
        val policy = NotificationPolicy(setOf(DOWNLOADS, VPN))
        policy.setChannelEnabled(DOWNLOADS, false)
        val decision = policy.decide(NotificationEvent.DOWNLOAD_COMPLETE)
        assertEquals(SuppressReason.CHANNEL_DISABLED_BY_USER, suppressed(decision))
        assertEquals(PermissionPhase.NOT_REQUESTED, policy.permissionPhase())
    }

    @Test
    fun secondEventWhileTheAskIsInFlightIsSuppressedWithoutASecondAsk() {
        val policy = NotificationPolicy(setOf(DOWNLOADS))
        policy.decide(NotificationEvent.DOWNLOAD_COMPLETE)
        assertEquals(
            SuppressReason.PERMISSION_PENDING,
            suppressed(policy.decide(NotificationEvent.DOWNLOAD_FAILED)),
        )
        assertEquals(PermissionPhase.REQUESTED, policy.permissionPhase())
    }

    @Test
    fun grantUnlocksTheShowAndThePendingEventCanBeReDecided() {
        val policy = NotificationPolicy(setOf(DOWNLOADS))
        val trigger = policy.decide(NotificationEvent.DOWNLOAD_COMPLETE)
        assertTrue(trigger is NotificationDecision.RequestPermission)
        assertTrue(policy.onPermissionResult(true) is NotificationResult.Ok)
        assertEquals(PermissionPhase.GRANTED, policy.permissionPhase())
        assertTrue(policy.decide(NotificationEvent.DOWNLOAD_COMPLETE) is NotificationDecision.Show)
    }

    @Test
    fun denialSuppressesForeverAndIsNeverReAsked() {
        val policy = NotificationPolicy(setOf(DOWNLOADS))
        policy.decide(NotificationEvent.DOWNLOAD_COMPLETE)
        assertTrue(policy.onPermissionResult(false) is NotificationResult.Ok)
        assertEquals(
            SuppressReason.PERMISSION_DENIED,
            suppressed(policy.decide(NotificationEvent.DOWNLOAD_FAILED)),
        )
        // later events never produce another ask
        assertTrue(policy.decide(NotificationEvent.DOWNLOAD_COMPLETE) !is NotificationDecision.RequestPermission)
    }

    @Test
    fun dialogResultOutsideTheRequestedPhaseIsRejected() {
        val policy = NotificationPolicy(setOf(DOWNLOADS))
        assertTrue(policy.onPermissionResult(true) is NotificationResult.Err)
        assertEquals(PermissionPhase.NOT_REQUESTED, policy.permissionPhase())
        policy.decide(NotificationEvent.DOWNLOAD_COMPLETE)
        assertTrue(policy.onPermissionResult(true) is NotificationResult.Ok)
        assertTrue(policy.onPermissionResult(false) is NotificationResult.Err) // GRANTED: no re-ask
        assertEquals(PermissionPhase.GRANTED, policy.permissionPhase())
    }

    @Test
    fun systemPermissionObservationIsAuthoritativeFromAnyPhase() {
        // below Android 13 permission is auto-granted at install: observed once
        val pre13 = NotificationPolicy(setOf(DOWNLOADS))
        pre13.onSystemPermissionChanged(true)
        assertTrue(pre13.decide(NotificationEvent.DOWNLOAD_COMPLETE) is NotificationDecision.Show)

        // the user later disables notifications in system settings
        pre13.onSystemPermissionChanged(false)
        assertEquals(
            SuppressReason.PERMISSION_DENIED,
            suppressed(pre13.decide(NotificationEvent.DOWNLOAD_COMPLETE)),
        )

        // and can grant again outside the app, even after a denial
        val denied = NotificationPolicy(setOf(DOWNLOADS))
        denied.decide(NotificationEvent.DOWNLOAD_COMPLETE)
        denied.onPermissionResult(false)
        denied.onSystemPermissionChanged(true)
        assertTrue(denied.decide(NotificationEvent.DOWNLOAD_COMPLETE) is NotificationDecision.Show)
    }

    // --- show decisions --------------------------------------------------------

    @Test
    fun decidedShowCarriesTheEventAndItsChannel() {
        val policy = fullyPatched()
        policy.onSystemPermissionChanged(true)
        val show = policy.decide(NotificationEvent.BACKUP_FAILED) as NotificationDecision.Show
        assertEquals(NotificationEvent.BACKUP_FAILED, show.event)
        assertEquals(SECURITY, show.channel)
    }

    // --- store seam ------------------------------------------------------------

    @Test
    fun inMemoryStoreRoundTripsTheWireForm() {
        val store = InMemoryNotificationStore()
        assertNull(store.load())
        val data = NotificationStoreData(disabledChannels = listOf("security"), permissionPhase = "DENIED")
        store.save(data)
        assertEquals(data, store.load())
    }
}
