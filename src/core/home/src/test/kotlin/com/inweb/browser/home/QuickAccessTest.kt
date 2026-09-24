package com.inweb.browser.home

import com.inweb.browser.shell.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAccessTest {

    @Test
    fun publicItemRequiresValidIdentityAndWebDestination() {
        assertThrows(IllegalArgumentException::class.java) {
            QuickAccessItem("", "Example", "https://example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QuickAccessItem("id", "", "https://example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QuickAccessItem("id", "Example", "data:text/plain,test")
        }
    }

    @Test
    fun addTrimsNameAndUsesBrowserNormalization() {
        val manager = manager(ids = listOf("id-1"))

        val result = manager.add("  Example  ", " example.com/ ") as QuickAccessResult.Ok

        assertEquals(
            QuickAccessItem("id-1", "Example", "https://example.com"),
            result.value,
        )
        assertEquals(listOf(result.value), manager.items())
    }

    @Test
    fun blankNameAndAddressAreRejectedWithoutMutation() {
        val manager = manager()
        assertTrue(manager.add(" ", "example.com") is QuickAccessResult.Err)
        assertEquals(
            QuickAccessError.AddressRequired,
            (manager.add("Example", " ") as QuickAccessResult.Err).error,
        )
        assertTrue(manager.items().isEmpty())
    }

    @Test
    fun searchTextIsNotAcceptedAsSiteAddress() {
        val result = manager().add("Search", "two words") as QuickAccessResult.Err
        assertEquals(QuickAccessError.InvalidAddress, result.error)
    }

    @Test
    fun nonWebSchemesAreRejected() {
        val result = manager().add("Mail", "mailto:test@example.org") as QuickAccessResult.Err
        assertEquals(QuickAccessError.UnsupportedScheme("mailto"), result.error)
    }

    @Test
    fun duplicateNormalizedUrlIsRejected() {
        val manager = manager(ids = listOf("one", "two"))
        manager.add("One", "https://example.com/")

        val duplicate = manager.add("Two", "https://example.com") as QuickAccessResult.Err

        assertEquals(QuickAccessError.DuplicateUrl("one"), duplicate.error)
        assertEquals(1, manager.items().size)
    }

    @Test
    fun capacityIsEnforcedBeforeGeneratingAnotherId() {
        val ids = CountingIds(listOf("one", "two"))
        val manager = QuickAccessManager(
            idGenerator = ids,
            maxItems = 1,
        )
        manager.add("One", "one.example")

        assertEquals(
            QuickAccessError.AtCapacity,
            (manager.add("Two", "two.example") as QuickAccessResult.Err).error,
        )
        assertEquals(1, ids.calls)
    }

    @Test
    fun successfulMutationReturnsCompleteCanonicalStoreData() {
        val manager = manager(ids = listOf("one"))

        val result = manager.add("One", "one.example") as QuickAccessResult.Ok

        assertEquals(
            listOf(StoredQuickAccessItem("one", "One", "https://one.example")),
            result.storeData,
        )
        assertEquals(result.storeData, manager.storeData())
    }

    @Test
    fun removeAndRestoreRoundTripOriginalPosition() {
        val manager = manager(ids = listOf("one", "two", "three"))
        manager.add("One", "one.example")
        manager.add("Two", "two.example")
        manager.add("Three", "three.example")

        val removed = (manager.remove("two") as QuickAccessResult.Ok).value
        assertEquals(listOf("one", "three"), manager.items().map { it.id })
        val restored = manager.restore(removed) as QuickAccessResult.Ok

        assertEquals(listOf("one", "two", "three"), manager.items().map { it.id })
        assertEquals(listOf("one", "two", "three"), restored.storeData.map { it.id })
    }

    @Test
    fun restoreRejectsDuplicateWithoutChangingState() {
        val manager = manager(ids = listOf("one"))
        val item = (manager.add("One", "one.example") as QuickAccessResult.Ok).value
        val before = manager.items()

        val result = manager.restore(RemovedQuickAccessItem(item, 0)) as QuickAccessResult.Err

        assertEquals(QuickAccessError.DuplicateId("one"), result.error)
        assertEquals(before, manager.items())
    }

    @Test
    fun moveReturnsStoreDataForAccessibleReorder() {
        val manager = manager(ids = listOf("one", "two", "three"))
        manager.add("One", "one.example")
        manager.add("Two", "two.example")
        manager.add("Three", "three.example")

        val result = manager.move("three", 0) as QuickAccessResult.Ok

        assertEquals(listOf("three", "one", "two"), result.value.map { it.id })
        assertEquals(listOf("three", "one", "two"), result.storeData.map { it.id })
    }

    @Test
    fun invalidMoveLeavesStateAndStoreDataUntouched() {
        val manager = manager(ids = listOf("one", "two"))
        manager.add("One", "one.example")
        manager.add("Two", "two.example")
        val before = manager.items()
        val storeData = manager.storeData()

        val result = manager.move("one", 5) as QuickAccessResult.Err

        assertEquals(QuickAccessError.IndexOutOfRange(5, 2), result.error)
        assertEquals(before, manager.items())
        assertEquals(storeData, manager.storeData())
    }

    @Test
    fun startupRepairsInvalidAndDuplicateStoredRows() {
        val manager = manager(
            storedItems = listOf(
                StoredQuickAccessItem("one", " One ", "https://one.example/"),
                StoredQuickAccessItem("one", "Duplicate ID", "https://two.example"),
                StoredQuickAccessItem("three", "Duplicate URL", "https://one.example"),
                StoredQuickAccessItem("four", "Bad", "not an address"),
            ),
        )

        assertEquals(listOf(QuickAccessItem("one", "One", "https://one.example")), manager.items())
        assertEquals(
            listOf(
                QuickAccessStoredIssueReason.DUPLICATE_ID,
                QuickAccessStoredIssueReason.DUPLICATE_URL,
                QuickAccessStoredIssueReason.INVALID_ADDRESS,
            ),
            manager.lastRecovery().map { it.reason },
        )
        assertEquals(
            listOf(StoredQuickAccessItem("one", "One", "https://one.example")),
            manager.storeData(),
        )
    }

    @Test
    fun startupDropsRowsOverCapacity() {
        val manager = QuickAccessManager(
            storedItems = listOf(
                StoredQuickAccessItem("one", "One", "one.example"),
                StoredQuickAccessItem("two", "Two", "two.example"),
            ),
            maxItems = 1,
        )

        assertEquals(listOf("one"), manager.items().map { it.id })
        assertEquals(QuickAccessStoredIssueReason.OVER_CAPACITY, manager.lastRecovery().single().reason)
    }

    @Test
    fun invalidOrDuplicateGeneratedIdIsRejected() {
        val blank = manager(ids = listOf(" "))
        assertEquals(
            QuickAccessError.InvalidGeneratedId,
            (blank.add("One", "one.example") as QuickAccessResult.Err).error,
        )

        val duplicate = manager(ids = listOf("same", "same"))
        duplicate.add("One", "one.example")
        assertEquals(
            QuickAccessError.DuplicateId("same"),
            (duplicate.add("Two", "two.example") as QuickAccessResult.Err).error,
        )
    }

    private fun manager(
        storedItems: List<StoredQuickAccessItem> = emptyList(),
        ids: List<String> = emptyList(),
    ) = QuickAccessManager(
        storedItems = storedItems,
        searchEngine = SearchEngine.DUCK_DUCK_GO,
        idGenerator = CountingIds(ids),
    )

    private class CountingIds(private val ids: List<String>) : QuickAccessIdGenerator {
        var calls = 0
        override fun nextId(): String = ids.getOrElse(calls++) { "generated-$calls" }
    }
}
