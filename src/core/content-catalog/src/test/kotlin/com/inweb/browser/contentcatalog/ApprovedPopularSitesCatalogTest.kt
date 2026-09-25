package com.inweb.browser.contentcatalog

import com.inweb.browser.home.PopularSiteCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovedPopularSitesCatalogTest {

    // A fixture row, never a real destination: the shipped catalogue is
    // empty, so the admission path can only be exercised through the
    // module-internal test seam.
    private val reviewed = ApprovedCatalogRow(
        id = "popular_test_quran",
        category = PopularSiteCategory.QURAN,
        canonicalUrl = "https://example.invalid/quran",
        localeVariantUrls = mapOf("bn" to "https://example.invalid/bn/quran"),
        artworkId = "artwork_quran",
        destinationName = "Qur’an",
        accessibilityLabel = "Example Qur’an destination",
        reviewerRole = "unit-test fixture",
        approvedOn = "2026-09-25",
        scope = "exercises the admission path only",
        rationale = "no destination is approved yet; this row never ships",
    )

    @Test
    fun shippedCatalogueIsEmptySoTheRailStaysHidden() {
        assertTrue(ApprovedPopularSitesCatalog.rows.isEmpty())
        PopularSiteCategory.values().forEach { category ->
            assertTrue(
                "no category may admit anything while the catalogue is empty",
                ApprovedPopularSitesCatalog.admit(category, "bn-BD").isEmpty(),
            )
        }
    }

    @Test
    fun anUnlistedCandidateIsNotAdmitted() {
        assertFalse(ApprovedPopularSitesCatalog.isApproved("popular_unknown"))
        assertTrue(
            ApprovedPopularSitesCatalog.admit(PopularSiteCategory.QURAN, "en").isEmpty(),
        )
    }

    @Test
    fun anApprovedRowIsAdmittedWithItsReviewedNames() {
        val admitted = ApprovedPopularSitesCatalog.admitApprovedRows(
            PopularSiteCategory.QURAN,
            "en-US",
            listOf(reviewed),
        )
        assertEquals(1, admitted.size)
        val site = admitted.single()
        assertEquals("popular_test_quran", site.id)
        assertEquals("https://example.invalid/quran", site.url)
        assertEquals("Qur’an", site.destinationName)
        assertEquals("Example Qur’an destination", site.accessibilityLabel)
    }

    @Test
    fun approvedLocaleVariantUsesOnlyItsRecordedUrl() {
        val site = ApprovedPopularSitesCatalog.admitApprovedRows(
            PopularSiteCategory.QURAN,
            "bn-BD",
            listOf(reviewed),
        ).single()
        assertEquals("https://example.invalid/bn/quran", site.url)

        val fallback = ApprovedPopularSitesCatalog.admitApprovedRows(
            PopularSiteCategory.QURAN,
            "fr",
            listOf(reviewed),
        ).single()
        assertEquals("https://example.invalid/quran", fallback.url)
    }

    @Test
    fun admissionIsCategoryScoped() {
        assertTrue(
            ApprovedPopularSitesCatalog.admitApprovedRows(
                PopularSiteCategory.HADITH,
                "bn",
                listOf(reviewed),
            ).isEmpty(),
        )
    }

    @Test
    fun approvalMetadataNamesAndUrlsCannotBeBlankOrInsecure() {
        fun row(
            id: String = "row",
            canonicalUrl: String = "https://example.invalid",
            destinationName: String = "Example",
            accessibilityLabel: String = "Example destination",
            reviewerRole: String = "reviewer",
        ) = ApprovedCatalogRow(
            id = id,
            category = PopularSiteCategory.QURAN,
            canonicalUrl = canonicalUrl,
            artworkId = "art",
            destinationName = destinationName,
            accessibilityLabel = accessibilityLabel,
            reviewerRole = reviewerRole,
            approvedOn = "2026-09-25",
            scope = "test",
            rationale = "test",
        )

        assertThrows(IllegalArgumentException::class.java) { row(id = " ") }
        assertThrows(IllegalArgumentException::class.java) { row(id = "Unstable ID") }
        assertThrows(IllegalArgumentException::class.java) { row(canonicalUrl = "http://example.invalid") }
        assertThrows(IllegalArgumentException::class.java) { row(destinationName = "") }
        assertThrows(IllegalArgumentException::class.java) { row(accessibilityLabel = " ") }
        assertThrows(IllegalArgumentException::class.java) { row(reviewerRole = "") }
    }

    @Test
    fun duplicateStableIdsAreRejectedBeforeAdmission() {
        assertThrows(IllegalArgumentException::class.java) {
            ApprovedPopularSitesCatalog.admitApprovedRows(
                PopularSiteCategory.QURAN,
                "en",
                listOf(reviewed, reviewed.copy(canonicalUrl = "https://other.invalid")),
            )
        }
    }

    @Test
    fun localeVariantKeysAndUrlsMustBeExplicitAndValid() {
        assertThrows(IllegalArgumentException::class.java) {
            reviewed.copy(localeVariantUrls = mapOf("bn_BD" to "https://example.invalid/bn"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            reviewed.copy(localeVariantUrls = mapOf("" to "https://example.invalid/blank"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            reviewed.copy(localeVariantUrls = mapOf("bn" to "http://example.invalid/bn"))
        }
    }
}
