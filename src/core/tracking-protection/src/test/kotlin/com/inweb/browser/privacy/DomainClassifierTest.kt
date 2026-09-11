package com.inweb.browser.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainClassifierTest {

    @Test
    fun simpleDomainsReduceToRegistrableDomain() {
        assertEquals("example.com", DomainClassifier.registrableDomain("example.com"))
        assertEquals("example.com", DomainClassifier.registrableDomain("a.example.com"))
        assertEquals("example.com", DomainClassifier.registrableDomain("a.b.c.example.com"))
    }

    @Test
    fun multiPartSuffixesNeedThreeLabels() {
        assertEquals("bbc.co.uk", DomainClassifier.registrableDomain("www.bbc.co.uk"))
        assertEquals("bbc.co.uk", DomainClassifier.registrableDomain("news.bbc.co.uk"))
        assertEquals("example.com.bd", DomainClassifier.registrableDomain("www.example.com.bd"))
        assertEquals("shop.co.jp", DomainClassifier.registrableDomain("www.shop.co.jp"))
    }

    @Test
    fun ipAddressHostsStayWhole() {
        assertEquals("192.168.0.1", DomainClassifier.registrableDomain("192.168.0.1"))
    }

    @Test
    fun singleLabelHostsStayWhole() {
        assertEquals("localhost", DomainClassifier.registrableDomain("localhost"))
    }

    @Test
    fun thirdPartyClassification() {
        // same registrable domain → first-party
        assertFalse(
            DomainClassifier.isThirdParty("news.example.com", "cdn.example.com"),
        )
        // different registrable domains → third-party
        assertTrue(
            DomainClassifier.isThirdParty("news.example.com", "ads.other.net"),
        )
        // multi-part suffix: different sites under co.uk are third-party
        assertTrue(
            DomainClassifier.isThirdParty("bbc.co.uk", "itv.co.uk"),
        )
        // same multi-part-suffix site
        assertFalse(
            DomainClassifier.isThirdParty("www.bbc.co.uk", "news.bbc.co.uk"),
        )
    }
}
