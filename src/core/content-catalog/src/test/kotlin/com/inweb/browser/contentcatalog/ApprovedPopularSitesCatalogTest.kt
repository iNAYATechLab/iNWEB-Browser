package com.inweb.browser.contentcatalog

import com.inweb.browser.home.PopularSiteCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovedPopularSitesCatalogTest {

    // A fixture row, never a real destination: the shipped catalogue is
    // empty, so the admission path can only be exercised with a test row.
    private val reviewed = ApprovedCatalogRow(
        id = "popular_test_quran",
        category = PopularSiteCategory.QURAN,
        url = "https://example.invalid/",
        artworkId = "artwork_quran",
        destinationName = "Qur’an",
        accessibilityLabel = "Quran.com",
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
                ApprovedPopularSitesCatalog.admit(category).isEmpty(),
            )
        }
    }

    @Test
    fun anUnlistedCandidateIsNotAdmitted() {
        assertFalse(ApprovedPopularSitesCatalog.isApproved("popular_unknown"))
        assertTrue(
            ApprovedPopularSitesCatalog.admit(PopularSiteCategory.QURAN)
                .none { it.id == "popular_unknown" },
        )
    }

    @Test
    fun anApprovedRowIsAdmittedWithItsReviewedNames() {
        val admitted = ApprovedPopularSitesCatalog.admit(
            PopularSiteCategory.QURAN,
            listOf(reviewed),
        )
        assertEquals(1, admitted.size)
        val site = admitted.single()
        assertEquals("popular_test_quran", site.id)
        assertEquals("Qur’an", site.destinationName)
        assertEquals("Quran.com", site.accessibilityLabel)
    }

    @Test
    fun admissionIsCategoryScoped() {
        assertTrue(
            ApprovedPopularSitesCatalog.admit(PopularSiteCategory.HADITH, listOf(reviewed)).isEmpty(),
        )
    }
}
