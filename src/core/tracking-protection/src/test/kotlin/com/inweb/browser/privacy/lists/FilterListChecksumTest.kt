package com.inweb.browser.privacy.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Content-integrity pinning (G-07, Step 44): known SHA-256 vectors and
 * the properties the update path relies on.
 */
class FilterListChecksumTest {

    @Test
    fun knownVectorsMatch() {
        // FIPS 180-4 test vectors, UTF-8 bytes.
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            FilterListChecksum.sha256("abc"),
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            FilterListChecksum.sha256(""),
        )
    }

    @Test
    fun outputIsDeterministicLowercaseHexOfLength64() {
        val body = "! Title: Unit\n||ads.example.com^\n"
        val digest = FilterListChecksum.sha256(body)
        assertEquals(digest, FilterListChecksum.sha256(body))
        assertEquals(64, digest.length)
        assertTrue(digest.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun anyBodyChangeChangesTheDigest() {
        val body = "! Version: v1\n||ads.example.com^\n"
        val tampered = "! Version: v1\n||ads.example.org^\n"
        assertNotEquals(FilterListChecksum.sha256(body), FilterListChecksum.sha256(tampered))
        // UTF-8 semantics: multibyte content hashes differently.
        assertNotEquals(FilterListChecksum.sha256("café"), FilterListChecksum.sha256("cafe"))
    }
}
