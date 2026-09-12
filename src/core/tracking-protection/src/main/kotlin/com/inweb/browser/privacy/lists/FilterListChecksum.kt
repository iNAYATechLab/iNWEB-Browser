package com.inweb.browser.privacy.lists

import java.security.MessageDigest

/**
 * Content-integrity pinning for cached filter lists (threat-review gap
 * G-07, closed in Step 44): the SHA-256 of a list body, computed when
 * the body is downloaded and re-verified whenever the cached copy is
 * loaded. A mismatch is treated as corruption or tampering — the copy
 * is never served and a re-download is attempted.
 *
 * This is integrity pinning, NOT a publisher signature: no upstream
 * EasyList-family source signs its content, and no such claim is made
 * (§57). Platform crypto only — MessageDigest SHA-256 (ADR-025: no
 * custom cryptography).
 */
object FilterListChecksum {

    /** Lowercase-hex SHA-256 of the body's UTF-8 bytes. */
    fun sha256(body: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(body.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
