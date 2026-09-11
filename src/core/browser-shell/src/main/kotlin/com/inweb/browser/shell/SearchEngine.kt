package com.inweb.browser.shell

import java.net.URLEncoder

/** A search engine definition with a `%s` query template. */
data class SearchEngine(val id: String, val name: String, val queryTemplate: String) {
    init {
        require(queryTemplate.contains("%s")) { "query template must contain %s" }
    }

    /** Builds the search URL for a query, percent-encoding the query. */
    fun buildSearchUrl(query: String): String =
        queryTemplate.replace("%s", URLEncoder.encode(query, Charsets.UTF_8.name()))

    companion object {
        /**
         * Privacy-preserving default engine (MASTER-SPEC §10:
         * privacy-preserving defaults). iNWEB ships with this selected.
         */
        val DUCK_DUCK_GO = SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s")
        val GOOGLE = SearchEngine("google", "Google", "https://www.google.com/search?q=%s")
        val BING = SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s")

        /** Engines offered in settings, in presentation order. */
        val DEFAULTS: List<SearchEngine> = listOf(DUCK_DUCK_GO, GOOGLE, BING)

        fun byId(id: String): SearchEngine? = DEFAULTS.firstOrNull { it.id == id }
    }
}
