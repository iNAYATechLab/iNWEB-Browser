package com.inweb.browser.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupBundleTest {

    private fun build(entries: List<BackupEntry>, appVersion: String = "1.0.0"): BackupBundle {
        val result = BackupBundle.build(entries, appVersion, createdAtMillis = 1_000L)
        return (result as BackupParseResult.Ok).bundle
    }

    private fun roundTrip(bundle: BackupBundle): BackupParseResult =
        BackupBundle.parse(bundle.serialize())

    // --- build + round-trip ------------------------------------------------

    @Test
    fun buildComputesChecksumsAndRoundTrips() {
        val bundle = build(
            listOf(
                BackupEntry("bookmarks", "p-1", "iNWEB-BOOKMARKS v=1\nb-1|https://a.example|A\n"),
                BackupEntry("history", "p-1", "iNWEB-HISTORY v=1\nh-1|https://b.example\n"),
            ),
        )
        assertEquals(2, bundle.entries.size)
        val entry = bundle.entries[0]
        assertEquals(64, entry.sha256.length)
        assertEquals(entry.sha256, BackupBundle.sha256Hex(entry.payload))

        val parsed = roundTrip(bundle) as BackupParseResult.Ok
        assertEquals(bundle.entries, parsed.bundle.entries)
        assertTrue(parsed.warnings.isEmpty())
        assertEquals(1, parsed.bundle.manifest.formatVersion)
        assertEquals("1.0.0", parsed.bundle.manifest.appVersion)
        assertEquals(1_000L, parsed.bundle.manifest.createdAtMillis)
    }

    @Test
    fun multipleProfilesAndStoresRoundTrip() {
        val bundle = build(
            listOf(
                BackupEntry("bookmarks", "p-1", "payload-a\n"),
                BackupEntry("bookmarks", "p-2", "payload-b\n"),
                BackupEntry("settings", "p-2", "payload-c\n"),
            ),
        )
        val parsed = roundTrip(bundle) as BackupParseResult.Ok
        assertEquals(3, parsed.bundle.entries.size)
        assertEquals(setOf("p-1", "p-2"), parsed.bundle.entries.map { it.profileId }.toSet())
    }

    @Test
    fun emptyPayloadEntryRoundTrips() {
        val bundle = build(listOf(BackupEntry("settings", "p-1", "")))
        val parsed = roundTrip(bundle) as BackupParseResult.Ok
        assertEquals("", parsed.bundle.entries.single().payload)
    }

    @Test
    fun serializedFormStartsWithTheHeader() {
        val text = build(listOf(BackupEntry("settings", "p-1", "x\n"))).serialize()
        assertTrue(text.startsWith("iNWEB-BACKUP v=1\n"))
        assertTrue(text.endsWith("--end--\n"))
        assertTrue(text.contains("payload:"))
    }

    // --- secret-safety whitelist (§32) ---------------------------------------

    @Test
    fun buildRejectsUnknownStoreNames() {
        val result = BackupBundle.build(
            listOf(BackupEntry("vpn-config", "p-1", "PrivateKey = nope\n")),
            "1.0.0",
            1L,
        )
        val err = result as BackupParseResult.Err
        assertTrue(err.reason.contains("unknown store"))
        assertTrue(err.reason.contains("vpn-config"))
    }

    @Test
    fun parseSkipsEntriesNamingUnknownStores() {
        val bundle = build(listOf(BackupEntry("bookmarks", "p-1", "good\n")))
        val tampered = bundle.serialize().replace("store=bookmarks", "store=cookies")
        val parsed = BackupBundle.parse(tampered) as BackupParseResult.Ok
        assertEquals(0, parsed.bundle.entries.size)
        assertTrue(parsed.warnings.any { it.contains("unknown store") })
    }

    // --- version gating (§32) --------------------------------------------------

    @Test
    fun parseRefusesNewerFormats() {
        val bundle = build(listOf(BackupEntry("settings", "p-1", "x\n")))
        val newer = bundle.serialize()
            .replace("iNWEB-BACKUP v=1", "iNWEB-BACKUP v=2")
            .replace("format=1", "format=2")
        val err = BackupBundle.parse(newer) as BackupParseResult.Err
        assertTrue(err.reason.contains("newer"))
        assertTrue(err.reason.contains("upgrade"))
    }

    @Test
    fun parseRejectsBadHeadersAndManifests() {
        assertTrue(BackupBundle.parse("random text\n") is BackupParseResult.Err)
        val bundle = build(listOf(BackupEntry("settings", "p-1", "x\n")))
        val noVersion = bundle.serialize().replace("app-version=1.0.0\n", "")
        assertTrue(BackupBundle.parse(noVersion) is BackupParseResult.Err)
        val badCreated = bundle.serialize().replace("created-at=1000", "created-at=soon")
        assertTrue(BackupBundle.parse(badCreated) is BackupParseResult.Err)
        val formatDisagrees = bundle.serialize().replace("format=1", "format=9")
        assertTrue(BackupBundle.parse(formatDisagrees) is BackupParseResult.Err)
    }

    // --- corruption tolerance (§32) ---------------------------------------------

    @Test
    fun corruptEntryIsSkippedWhileOthersSurvive() {
        val bundle = build(
            listOf(
                BackupEntry("bookmarks", "p-1", "good payload one\n"),
                BackupEntry("history", "p-1", "good payload two\n"),
            ),
        )
        val tampered = bundle.serialize().replace("good payload two", "TAMPERED payload two")
        val parsed = BackupBundle.parse(tampered) as BackupParseResult.Ok
        assertEquals(1, parsed.bundle.entries.size)
        assertEquals("bookmarks", parsed.bundle.entries[0].storeName)
        assertTrue(parsed.warnings.any { it.contains("checksum") })
    }

    @Test
    fun entryCountMismatchBecomesAWarning() {
        val bundle = build(
            listOf(
                BackupEntry("bookmarks", "p-1", "a\n"),
                BackupEntry("history", "p-1", "b\n"),
            ),
        )
        val tampered = bundle.serialize().replace("entries=2", "entries=5")
        val parsed = BackupBundle.parse(tampered) as BackupParseResult.Ok
        assertEquals(2, parsed.bundle.entries.size)
        assertTrue(parsed.warnings.any { it.contains("declared 5 entries, 2 are intact") })
    }

    @Test
    fun missingEndMarkerIsAWarningNotAFailure() {
        val bundle = build(listOf(BackupEntry("settings", "p-1", "x\n")))
        val truncated = bundle.serialize().replace("--end--\n", "")
        val parsed = BackupBundle.parse(truncated) as BackupParseResult.Ok
        assertEquals(1, parsed.bundle.entries.size)
        assertTrue(parsed.warnings.any { it.contains("end marker missing") })
    }

    // --- restore preview (§32) -----------------------------------------------------

    @Test
    fun previewCountsPerProfileAndStoreAndCarriesWarnings() {
        val bundle = build(
            listOf(
                BackupEntry("bookmarks", "p-1", "a\n"),
                BackupEntry("history", "p-1", "b\n"),
                BackupEntry("bookmarks", "p-1", "c\n"),
                BackupEntry("settings", "p-2", "d\n"),
            ),
        )
        val preview = bundle.preview(warnings = listOf("w1"))
        assertEquals(1, preview.formatVersion)
        assertEquals("1.0.0", preview.appVersion)
        assertEquals(mapOf("bookmarks" to 2, "history" to 1), preview.perProfile["p-1"])
        assertEquals(mapOf("settings" to 1), preview.perProfile["p-2"])
        assertEquals(listOf("w1"), preview.warnings)
    }

    @Test
    fun parsedWarningsFlowIntoPreview() {
        val bundle = build(
            listOf(
                BackupEntry("bookmarks", "p-1", "a\n"),
                BackupEntry("history", "p-1", "b\n"),
            ),
        )
        val tampered = bundle.serialize().replace("entries=2", "entries=9")
        val parsed = BackupBundle.parse(tampered) as BackupParseResult.Ok
        assertTrue(parsed.bundle.preview(parsed.warnings).warnings.isNotEmpty())
    }
}
