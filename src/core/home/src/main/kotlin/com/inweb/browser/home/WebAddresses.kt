package com.inweb.browser.home

import com.inweb.browser.shell.OmniboxInput
import com.inweb.browser.shell.OmniboxParser
import com.inweb.browser.shell.SearchEngine
import java.net.URI

/** Internal result used by Quick Access and curated-site validation. */
internal sealed class WebAddressResult {
    data class Valid(val url: String) : WebAddressResult()
    object Invalid : WebAddressResult()
    data class UnsupportedScheme(val scheme: String?) : WebAddressResult()
}

internal object HomeWebAddresses {

    /**
     * Applies the browser's existing omnibox policy first, then accepts only a
     * real HTTP(S) web destination. Search results and non-web schemes are not
     * valid Quick Access addresses.
     */
    fun normalize(rawInput: String, searchEngine: SearchEngine): WebAddressResult {
        val input = rawInput.trim()
        if (input.isEmpty()) return WebAddressResult.Invalid

        val parsed = try {
            OmniboxParser.parse(input, searchEngine)
        } catch (_: IllegalArgumentException) {
            return WebAddressResult.Invalid
        }
        if (parsed !is OmniboxInput.Url) return WebAddressResult.Invalid
        return normalizeParsedUrl(parsed.url)
    }

    fun isWebDestination(url: String): Boolean =
        normalizeParsedUrl(url) is WebAddressResult.Valid

    fun isReviewedHttps(url: String): Boolean =
        when (val result = normalizeParsedUrl(url)) {
            is WebAddressResult.Valid -> {
                val scheme = runCatching { URI(result.url).scheme }.getOrNull()
                scheme.equals("https", ignoreCase = true)
            }
            WebAddressResult.Invalid,
            is WebAddressResult.UnsupportedScheme,
            -> false
        }

    private fun normalizeParsedUrl(value: String): WebAddressResult {
        val uri = try {
            URI(value).normalize()
        } catch (_: Exception) {
            return WebAddressResult.Invalid
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("http", "https")) {
            return WebAddressResult.UnsupportedScheme(scheme)
        }
        val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() }
            ?: return WebAddressResult.Invalid
        if (uri.userInfo != null) return WebAddressResult.Invalid

        val port = when {
            uri.port == -1 -> ""
            scheme == "http" && uri.port == 80 -> ""
            scheme == "https" && uri.port == 443 -> ""
            else -> ":${uri.port}"
        }
        val displayHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        val path = when {
            uri.rawPath == null || uri.rawPath == "/" -> ""
            else -> uri.rawPath
        }
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        val fragment = uri.rawFragment?.let { "#$it" }.orEmpty()
        return WebAddressResult.Valid("$scheme://$displayHost$port$path$query$fragment")
    }
}
