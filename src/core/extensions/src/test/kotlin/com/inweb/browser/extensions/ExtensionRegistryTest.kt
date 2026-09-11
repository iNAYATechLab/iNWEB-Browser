package com.inweb.browser.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionRegistryTest {

    private fun record(
        id: String = "ext-a",
        version: String = "1.0.0",
        permissions: Set<String> = setOf("storage"),
        manifestVersion: ManifestVersion = ManifestVersion.V3,
        name: String = "Test Extension",
    ) = ExtensionRecord(
        id = id,
        name = name,
        version = ExtensionVersion.parse(version)!!,
        manifestVersion = manifestVersion,
        permissions = permissions,
    )

    private fun ok(result: RegistryResult<InstalledExtension>): InstalledExtension =
        (result as RegistryResult.Ok).value

    // --- install / review / enable ---------------------------------------

    @Test
    fun installCreatesPendingReviewEntry() {
        val registry = ExtensionRegistry()
        val entry = ok(registry.install(record()))
        assertEquals(ExtensionState.PENDING_REVIEW, entry.state)
        assertTrue(!entry.reviewComplete)
    }

    @Test
    fun enableWithoutReviewIsRejected() {
        val registry = ExtensionRegistry()
        registry.install(record())
        val result = registry.enable("ext-a")
        assertEquals(RegistryError.ReviewRequired, (result as RegistryResult.Err).error)
    }

    @Test
    fun reviewThenEnableThenDisable() {
        val registry = ExtensionRegistry()
        registry.install(record())

        val reviewed = ok(registry.reviewPermissions("ext-a"))
        assertEquals(ExtensionState.DISABLED, reviewed.state)
        assertTrue(reviewed.reviewComplete)

        val enabled = ok(registry.enable("ext-a"))
        assertEquals(ExtensionState.ENABLED, enabled.state)

        val disabled = ok(registry.disable("ext-a"))
        assertEquals(ExtensionState.DISABLED, disabled.state)

        assertEquals(ExtensionState.ENABLED, ok(registry.enable("ext-a")).state)
    }

    @Test
    fun duplicateInstallIsRejected() {
        val registry = ExtensionRegistry()
        registry.install(record())
        val result = registry.install(record())
        assertEquals(RegistryError.DuplicateInstall, (result as RegistryResult.Err).error)
    }

    @Test
    fun reviewIsIdempotentForAlreadyReviewedExtension() {
        val registry = ExtensionRegistry()
        registry.install(record())
        ok(registry.reviewPermissions("ext-a"))
        val again = ok(registry.reviewPermissions("ext-a"))
        assertEquals(ExtensionState.DISABLED, again.state)
        assertTrue(again.reviewComplete)
    }

    // --- updates ----------------------------------------------------------

    @Test
    fun updateRequiresStrictlyNewerVersion() {
        val registry = ExtensionRegistry()
        registry.install(record(version = "1.2.0"))

        val same = registry.update("ext-a", record(version = "1.2.0"))
        assertTrue(same is RegistryResult.Err)

        val downgrade = registry.update("ext-a", record(version = "1.1.9"))
        assertTrue(downgrade is RegistryResult.Err)
    }

    @Test
    fun updateWithoutNewPermissionsKeepsEnabledState() {
        val registry = ExtensionRegistry()
        registry.install(record(version = "1.0.0", permissions = setOf("storage", "tabs")))
        ok(registry.reviewPermissions("ext-a"))
        ok(registry.enable("ext-a"))

        val updated = ok(registry.update("ext-a", record(version = "1.1.0", permissions = setOf("storage"))))
        assertEquals(ExtensionState.ENABLED, updated.state)
        // trailing zeros are normalized away (1.1.0 == 1.1)
        assertEquals("1.1", updated.record.version.toString())
    }

    @Test
    fun updateWithNewPermissionsRequiresReviewEvenWhenEnabled() {
        val registry = ExtensionRegistry()
        registry.install(record(version = "1.0.0", permissions = setOf("storage")))
        ok(registry.reviewPermissions("ext-a"))
        ok(registry.enable("ext-a"))

        val updated = ok(
            registry.update("ext-a", record(version = "2.0.0", permissions = setOf("storage", "tabs"))),
        )
        assertEquals(ExtensionState.DISABLED_UPDATE, updated.state)
        assertEquals(RegistryError.ReviewRequired, (registry.enable("ext-a") as RegistryResult.Err).error)

        val reviewed = ok(registry.reviewPermissions("ext-a"))
        assertEquals(ExtensionState.DISABLED, reviewed.state)
        assertEquals(ExtensionState.ENABLED, ok(registry.enable("ext-a")).state)
    }

    @Test
    fun updateOnNeverReviewedExtensionStaysPendingReview() {
        val registry = ExtensionRegistry()
        registry.install(record(version = "1.0.0"))
        val updated = ok(registry.update("ext-a", record(version = "1.1.0")))
        assertEquals(ExtensionState.PENDING_REVIEW, updated.state)
        assertTrue(!updated.reviewComplete)
    }

    @Test
    fun updateOnDisabledExtensionWithoutNewPermissionsStaysDisabled() {
        val registry = ExtensionRegistry()
        registry.install(record(version = "1.0.0", permissions = setOf("storage")))
        ok(registry.reviewPermissions("ext-a"))
        // reviewed but never enabled -> DISABLED
        val updated = ok(registry.update("ext-a", record(version = "1.5.0")))
        assertEquals(ExtensionState.DISABLED, updated.state)
    }

    @Test
    fun updateWithMismatchedIdIsRejected() {
        val registry = ExtensionRegistry()
        registry.install(record(id = "ext-a", version = "1.0.0"))
        val result = registry.update("ext-a", record(id = "ext-b", version = "2.0.0"))
        assertTrue(result is RegistryResult.Err)
    }

    // --- remove / lookup ----------------------------------------------------

    @Test
    fun removeDeletesAndSecondRemoveFails() {
        val registry = ExtensionRegistry()
        registry.install(record())
        assertTrue(registry.remove("ext-a") is RegistryResult.Ok)
        assertNull(registry.get("ext-a"))
        assertEquals(RegistryError.NotFound, (registry.remove("ext-a") as RegistryResult.Err).error)
        assertEquals(RegistryError.NotFound, (registry.enable("ext-a") as RegistryResult.Err).error)
    }

    @Test
    fun allPreservesInsertionOrder() {
        val registry = ExtensionRegistry()
        registry.install(record(id = "z-ext"))
        registry.install(record(id = "a-ext"))
        registry.install(record(id = "m-ext"))
        assertEquals(listOf("z-ext", "a-ext", "m-ext"), registry.all().map { it.record.id })
    }

    @Test
    fun countsReflectRealStateOnly() {
        val registry = ExtensionRegistry()
        registry.install(record(id = "one", permissions = setOf("storage")))
        registry.install(record(id = "two", permissions = setOf("storage")))
        registry.install(record(id = "three", permissions = setOf("storage")))
        ok(registry.reviewPermissions("one"))
        ok(registry.enable("one"))
        // two is reviewed (but not enabled), then gets an update that adds
        // permissions -> DISABLED_UPDATE (upgrade consent)
        ok(registry.reviewPermissions("two"))
        ok(registry.update("two", record(id = "two", version = "2.0.0", permissions = setOf("storage", "tabs"))))

        val counts = registry.counts()
        assertEquals(3, counts.installed)
        assertEquals(1, counts.pendingReview)            // three
        assertEquals(1, counts.enabled)                  // one
        assertEquals(0, counts.disabled)
        assertEquals(1, counts.awaitingPermissionReview) // two
    }

    // --- version model --------------------------------------------------------

    @Test
    fun versionsCompareNumericallyWithPadding() {
        val v = { raw: String -> ExtensionVersion.parse(raw)!! }
        assertTrue(v("1.2") < v("1.10"))
        assertTrue(v("1.0") == v("1.0.0"))
        assertTrue(v("1.2.3.4") < v("1.2.4"))
        assertTrue(v("2.10.0") > v("2.9.9"))
        assertEquals(0, v("1.2.3").compareTo(v("1.2.3")))
    }

    @Test
    fun invalidVersionsAreRejected() {
        val invalid = listOf("", "1..2", "a.b", "1.2.3.4.5", "1.-2", " 1.2", "1.x")
        for (raw in invalid) {
            assertNull("expected rejection of '$raw'", ExtensionVersion.parse(raw))
        }
    }

    // --- persistence seam ------------------------------------------------------

    @Test
    fun registryStateSurvivesStoreRoundTrip() {
        val store = InMemoryExtensionStore()
        val first = ExtensionRegistry(store)
        first.install(record(id = "kept", version = "1.0.0"))
        ok(first.reviewPermissions("kept"))
        ok(first.enable("kept"))

        val second = ExtensionRegistry(store)
        val restored = second.get("kept")
        assertEquals(ExtensionState.ENABLED, restored?.state)
        assertTrue(restored?.reviewComplete == true)
        assertEquals("kept", restored?.record?.id)
    }
}
