package com.inweb.browser.shell

/** Parsed omnibox input: either a navigable URL or a search. */
sealed class OmniboxInput {
    data class Url(val url: String) : OmniboxInput()
    data class Search(val query: String, val searchUrl: String) : OmniboxInput()
}

/**
 * Decides whether raw omnibox text is a URL or a search query — the same
 * decision real browsers make before navigation:
 *
 *  - input with an explicit `scheme://` authority is used as-is;
 *  - input with a known scheme that takes no authority (mailto:, about:,
 *    data:, inweb:, ...) is used as-is;
 *  - bare domains, IPv4 literals and localhost get an https:// prefix
 *    (so `host:port` is never mistaken for a scheme);
 *  - everything else becomes a search against the configured engine.
 */
object OmniboxParser {

    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")
    private val hostRegex = Regex(
        "^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" +
            "(:\\d+)?(/.*)?(\\?.*)?(#.*)?$"
    )
    private val ipv4Regex = Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?(/.*)?$")

    /** Schemes that legitimately take no `//` authority part. */
    private val knownNoAuthoritySchemes = setOf(
        "about", "data", "chrome", "chrome-extension", "inweb", "mailto", "tel", "sms",
    )

    fun parse(rawInput: String, engine: SearchEngine): OmniboxInput {
        val input = rawInput.trim()
        require(input.isNotEmpty()) { "input must not be blank" }
        val hasScheme = schemeRegex.matches(input)
        return when {
            hasScheme && input.contains("://") -> OmniboxInput.Url(input)
            hasScheme && input.substringBefore(':').lowercase() in knownNoAuthoritySchemes ->
                OmniboxInput.Url(input)
            input == "localhost" || input.startsWith("localhost:") ->
                OmniboxInput.Url("https://$input")
            ipv4Regex.matches(input) || hostRegex.matches(input) ->
                OmniboxInput.Url("https://$input")
            else -> OmniboxInput.Search(query = input, searchUrl = engine.buildSearchUrl(input))
        }
    }
}
