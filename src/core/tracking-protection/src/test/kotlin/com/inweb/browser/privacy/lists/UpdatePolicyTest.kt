package com.inweb.browser.privacy.lists

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    private val metadata = FilterListMetadata(
        sourceId = "easylist",
        downloadedAtMillis = 1_000_000,
    )

    @Test
    fun neverCheckedListIsDue() {
        assertTrue(UpdatePolicy().isRefreshDue(metadata = null, nowMillis = 0))
    }

    @Test
    fun freshListIsNotDue() {
        val policy = UpdatePolicy(refreshIntervalMs = 10_000)
        assertFalse(policy.isRefreshDue(metadata, nowMillis = 1_000_000 + 9_999))
    }

    @Test
    fun staleListIsDue() {
        val policy = UpdatePolicy(refreshIntervalMs = 10_000)
        assertTrue(policy.isRefreshDue(metadata, nowMillis = 1_000_000 + 10_000))
    }

    @Test
    fun disabledPolicyIsNeverDue() {
        val policy = UpdatePolicy(enabled = false)
        assertFalse(policy.isRefreshDue(metadata = null, nowMillis = Long.MAX_VALUE / 2))
    }
}
