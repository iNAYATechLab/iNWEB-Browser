package com.inweb.browser.privacy

/**
 * Simplified registrable-domain (eTLD+1) classification.
 *
 * Uses a built-in table of common multi-part public suffixes (including
 * Bangladeshi and other regional suffixes) with a last-two-labels fallback.
 * The engine-side integration replaces this with Chromium's full Public
 * Suffix List; the interface stays identical.
 */
object DomainClassifier {

    private val multiPartSuffixes = setOf(
        // United Kingdom
        "co.uk", "org.uk", "ac.uk", "gov.uk", "me.uk", "net.uk",
        // Bangladesh
        "com.bd", "net.bd", "org.bd", "ac.bd", "gov.bd", "edu.bd", "mil.bd",
        // Other common multi-part suffixes
        "co.jp", "or.jp", "ne.jp", "ac.jp", "go.jp",
        "com.au", "net.au", "org.au", "edu.au",
        "co.in", "net.in", "org.in", "ac.in",
        "co.nz", "com.br", "com.mx", "com.tr", "com.cn", "com.tw",
        "co.kr", "co.id", "com.my", "com.ph", "com.vn", "com.ar",
        "com.co", "com.pe", "com.eg", "co.za", "com.ng", "com.pk", "com.sa",
    )

    private val ipv4Regex = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    /** The registrable domain of a host (e.g. `a.b.example.com` → `example.com`). */
    fun registrableDomain(host: String): String {
        val h = host.trim().lowercase().removeSuffix(".")
        if (h.isEmpty()) return h
        if (ipv4Regex.matches(h)) return h
        if (!h.contains('.')) return h

        val labels = h.split('.').filter { it.isNotEmpty() }
        if (labels.size <= 2) return labels.joinToString(".")

        val lastTwo = labels.takeLast(2).joinToString(".")
        return if (lastTwo in multiPartSuffixes) {
            labels.takeLast(3).joinToString(".")
        } else {
            lastTwo
        }
    }

    /** True when the two hosts belong to different registrable domains. */
    fun isThirdParty(documentHost: String, requestHost: String): Boolean =
        registrableDomain(documentHost) != registrableDomain(requestHost)
}
