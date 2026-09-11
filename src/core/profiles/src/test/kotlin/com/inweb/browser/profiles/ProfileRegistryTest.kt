package com.inweb.browser.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRegistryTest {

    private fun ok(result: ProfileResult<ProfileRecord>): ProfileRecord = (result as ProfileResult.Ok).value

    // --- seeding ---------------------------------------------------------------

    @Test
    fun emptyStoreIsSeededWithDefaultActiveProfile() {
        val registry = ProfileRegistry()
        val all = registry.all()
        assertEquals(1, all.size)
        assertEquals("p-1", all[0].id)
        assertEquals("Default", all[0].name)
        assertEquals("p-1", registry.activeProfile()?.id)
    }

    @Test
    fun loadedStoreIsNotReseeded() {
        val store = InMemoryProfileStore()
        val record = ProfileRecord("p-7", "Work", 123L)
        store.save(ProfileStoreData(listOf(record), "p-7"))
        val registry = ProfileRegistry(store)
        assertEquals(listOf("p-7"), registry.all().map { it.id })
        assertEquals("p-7", registry.activeProfile()?.id)
    }

    // --- create / rename / activate ----------------------------------------------

    @Test
    fun createAddsInactiveProfilesWithMonotonicIds() {
        val registry = ProfileRegistry()
        val work = ok(registry.create("Work", 1_000L))
        val personal = ok(registry.create("Personal", 2_000L))
        assertEquals("p-2", work.id)
        assertEquals("p-3", personal.id)
        // active profile unchanged by creation
        assertEquals("p-1", registry.activeProfile()?.id)
        assertEquals(listOf("p-1", "p-2", "p-3"), registry.all().map { it.id })
    }

    @Test
    fun createRejectsBlankNames() {
        val registry = ProfileRegistry()
        assertTrue(registry.create("   ", 1L) is ProfileResult.Err)
        assertEquals(1, registry.all().size)
    }

    @Test
    fun renameUpdatesAndTrims() {
        val registry = ProfileRegistry()
        val renamed = ok(registry.rename("p-1", "  Main  "))
        assertEquals("Main", renamed.name)
        assertEquals("Main", registry.get("p-1")?.name)
        assertTrue(registry.rename("p-1", "") is ProfileResult.Err)
        assertTrue(registry.rename("p-99", "X") is ProfileResult.Err)
    }

    @Test
    fun setActiveSwitchesTheActiveProfile() {
        val registry = ProfileRegistry()
        ok(registry.create("Work", 1_000L))
        val active = ok(registry.setActive("p-2"))
        assertEquals("p-2", active.id)
        assertEquals("p-2", registry.activeProfile()?.id)
        assertTrue(registry.setActive("p-99") is ProfileResult.Err)
    }

    // --- namespaces (§28 isolation contract) -----------------------------------------

    @Test
    fun namespaceOfExistingProfileIsItsId() {
        val registry = ProfileRegistry()
        ok(registry.create("Work", 1_000L))
        assertEquals("p-2", (registry.namespaceOf("p-2") as ProfileResult.Ok).value)
        assertTrue(registry.namespaceOf("p-99") is ProfileResult.Err)
    }

    // --- delete -------------------------------------------------------------------

    @Test
    fun deleteRemovesProfileAndReportsNewActive() {
        val registry = ProfileRegistry()
        ok(registry.create("Work", 1_000L))
        ok(registry.create("Personal", 2_000L))

        val deleted = (registry.delete("p-3") as ProfileResult.Ok).value
        assertEquals("p-3", deleted.record.id)
        // active was p-1 and stays p-1
        assertEquals("p-1", deleted.newActiveProfileId)
        assertNull(registry.get("p-3"))
    }

    @Test
    fun deletingActiveProfileFallsBackToOldestRemaining() {
        val registry = ProfileRegistry()
        ok(registry.create("Work", 1_000L))
        ok(registry.create("Personal", 2_000L))
        ok(registry.setActive("p-3"))

        val deleted = (registry.delete("p-3") as ProfileResult.Ok).value
        assertEquals("p-1", deleted.newActiveProfileId)
        assertEquals("p-1", registry.activeProfile()?.id)
    }

    @Test
    fun lastRemainingProfileCannotBeDeleted() {
        val registry = ProfileRegistry()
        assertTrue(registry.delete("p-1") is ProfileResult.Err)
        assertEquals(1, registry.all().size)
        assertEquals("p-1", registry.activeProfile()?.id)
    }

    @Test
    fun idsAreNeverReusedAfterDelete() {
        val registry = ProfileRegistry()
        ok(registry.create("Work", 1_000L))       // p-2
        ok(registry.create("Personal", 2_000L))   // p-3
        registry.delete("p-2")
        val next = ok(registry.create("Study", 3_000L))
        assertEquals("p-4", next.id)              // p-2's namespace is never resurrected
    }

    // --- persistence seam --------------------------------------------------------------

    @Test
    fun stateSurvivesStoreRoundTrip() {
        val store = InMemoryProfileStore()
        val first = ProfileRegistry(store)
        ok(first.create("Work", 1_000L))
        ok(first.create("Personal", 2_000L))
        ok(first.setActive("p-2"))
        first.rename("p-2", "Office")

        val second = ProfileRegistry(store)
        assertEquals(listOf("p-1", "p-2", "p-3"), second.all().map { it.id })
        assertEquals("Office", second.get("p-2")?.name)
        assertEquals("p-2", second.activeProfile()?.id)
    }
}
