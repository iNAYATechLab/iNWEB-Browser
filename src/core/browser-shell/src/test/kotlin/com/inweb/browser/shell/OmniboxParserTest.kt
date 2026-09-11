package com.inweb.browser.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniboxParserTest {

    private val engine = SearchEngine.DUCK_DUCK_GO

    @Test
    fun bareDomainBecomesHttpsUrl() {
        assertEquals(
            OmniboxInput.Url("https://example.com"),
            OmniboxParser.parse("example.com", engine),
        )
    }

    @Test
    fun domainWithPortPathQueryAndFragmentBecomesUrl() {
        assertEquals(
            OmniboxInput.Url("https://example.com:8080/a/b?q=1#top"),
            OmniboxParser.parse("example.com:8080/a/b?q=1#top", engine),
        )
    }

    @Test
    fun explicitSchemeUsedVerbatim() {
        assertEquals(
            OmniboxInput.Url("http://example.com"),
            OmniboxParser.parse("http://example.com", engine),
        )
    }

    @Test
    fun noAuthoritySchemesUsedVerbatim() {
        assertEquals(
            OmniboxInput.Url("mailto:user@example.com"),
            OmniboxParser.parse("mailto:user@example.com", engine),
        )
        assertEquals(
            OmniboxInput.Url("inweb://home"),
            OmniboxParser.parse("inweb://home", engine),
        )
    }

    @Test
    fun unknownSchemeBecomesSearchNotNavigation() {
        // javascript: and unknown schemes must never navigate from the omnibox
        assertTrue(OmniboxParser.parse("javascript:alert(1)", engine) is OmniboxInput.Search)
        assertTrue(OmniboxParser.parse("weird-thing:1234", engine) is OmniboxInput.Search)
    }

    @Test
    fun localhostAndIpv4BecomeUrls() {
        assertEquals(OmniboxInput.Url("https://localhost"), OmniboxParser.parse("localhost", engine))
        assertEquals(OmniboxInput.Url("https://localhost:3000"), OmniboxParser.parse("localhost:3000", engine))
        assertEquals(OmniboxInput.Url("https://192.168.0.1"), OmniboxParser.parse("192.168.0.1", engine))
    }

    @Test
    fun plainWordsBecomeSearch() {
        assertEquals(
            OmniboxInput.Search("how to code", "https://duckduckgo.com/?q=how+to+code"),
            OmniboxParser.parse("how to code", engine),
        )
    }

    @Test
    fun numbersWithoutFullDotsBecomeSearch() {
        assertTrue(OmniboxParser.parse("123 456", engine) is OmniboxInput.Search)
        assertTrue(OmniboxParser.parse("how are you", engine) is OmniboxInput.Search)
    }

    @Test
    fun bengaliQueryBecomesEncodedSearch() {
        val result = OmniboxParser.parse("ব্রাউজার", engine)
        assertTrue(result is OmniboxInput.Search)
        val search = result as OmniboxInput.Search
        assertEquals("ব্রাউজার", search.query)
        assertTrue(search.searchUrl.startsWith("https://duckduckgo.com/?q="))
        // first character 'ব' (U+09AC) encodes to E0 A6 AC in UTF-8 percent-encoding
        assertTrue(search.searchUrl.contains("%E0%A6%AC"))
    }

    @Test
    fun inputIsTrimmed() {
        assertEquals(
            OmniboxInput.Url("https://example.com"),
            OmniboxParser.parse("  example.com  ", engine),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankInputRejected() {
        OmniboxParser.parse("   ", engine)
    }
}
