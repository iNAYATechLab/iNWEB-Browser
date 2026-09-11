package com.inweb.browser.shell

/** A most-visited page computed from real browsing history (§38 shortcuts). */
data class TopSite(
    val url: String,
    val title: String,
    val visitCount: Int,
)

/**
 * Computes the "shortcuts" section of the home / new-tab page from real
 * raw history visits — never fabricated, never pinned (v1).
 *
 * Ranking: visit count descending; ties broken by the most recent visit.
 * The title shown is the one recorded with the most recent visit of that
 * URL.
 */
object TopSites {

    fun compute(visits: List<HistoryEntry>, limit: Int): List<TopSite> {
        require(limit > 0) { "limit must be > 0" }

        class Accumulator(var title: String, var lastVisitedAtMillis: Long, var visitCount: Int)

        val byUrl = LinkedHashMap<String, Accumulator>()
        for (visit in visits) {
            val accumulator = byUrl.getOrPut(visit.url) {
                Accumulator(visit.title, visit.visitedAtMillis, 0)
            }
            accumulator.visitCount++
            if (visit.visitedAtMillis >= accumulator.lastVisitedAtMillis) {
                accumulator.lastVisitedAtMillis = visit.visitedAtMillis
                accumulator.title = visit.title
            }
        }

        return byUrl.entries
            .map { (url, accumulator) ->
                Triple(url, accumulator.title, accumulator.visitCount to accumulator.lastVisitedAtMillis)
            }
            .sortedWith(
                compareByDescending<Triple<String, String, Pair<Int, Long>>> { it.third.first }
                    .thenByDescending { it.third.second },
            )
            .take(limit)
            .map { (url, title, counts) -> TopSite(url, title, counts.first) }
    }
}
