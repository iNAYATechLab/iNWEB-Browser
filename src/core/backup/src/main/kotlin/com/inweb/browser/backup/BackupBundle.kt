package com.inweb.browser.backup

import java.security.MessageDigest

/**
 * One store export inside a backup bundle (§32). Payloads are the texts
 * the corresponding file stores write, kept in CANONICAL form (no
 * trailing newline) so the wire format round-trips exactly — never
 * re-encoded, never reinterpreted.
 */
data class BackupEntry(
    val storeName: String,
    val profileId: String,
    val payload: String,
)

/** A stored entry plus its integrity checksum (SHA-256 of the payload). */
data class StoredBackupEntry(
    val storeName: String,
    val profileId: String,
    val payload: String,
    val sha256: String,
)

/** The bundle manifest — corrupt manifests abort the whole restore (§32). */
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtMillis: Long,
    val entryCount: Int,
)

/** What the restore flow shows BEFORE importing anything (§32 preview). */
data class BackupPreview(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtMillis: Long,
    /** profileId -> (storeName -> entry count) */
    val perProfile: Map<String, Map<String, Int>>,
    val warnings: List<String>,
)

sealed class BackupParseResult {
    /** Parsed bundle; corrupt entries were skipped and are named in [warnings]. */
    data class Ok(val bundle: BackupBundle, val warnings: List<String> = emptyList()) : BackupParseResult()

    /** Unusable bundle: corrupt/unknown header or manifest, or a newer format. */
    data class Err(val reason: String) : BackupParseResult()
}

/**
 * The versioned, checksummed backup bundle (§32; ADR-026).
 *
 * Format (`iNWEB-BACKUP v=1`, line-based; the payload sentinel lines are
 * reserved and never appear in store formats):
 *
 * ```
 * iNWEB-BACKUP v=1
 * format=1
 * app-version=1.2.3
 * created-at=1234567890
 * entries=2
 * --entry--
 * store=bookmarks
 * profile=p-1
 * sha256=<64 hex chars>
 * payload:
 * <payload lines...>
 * --end-entry--
 * --end--
 * ```
 *
 * Version gating: this build reads exactly format 1. NEWER formats are
 * refused with an explicit upgrade message (no guessing); older formats
 * migrate forward only via explicit migration functions (none exist yet —
 * there is nothing older than v1).
 *
 * Secret safety: only [KNOWN_STORES] may be bundled — store names outside
 * the whitelist (e.g. anything carrying credentials or VPN keys) are
 * rejected at build time; no plaintext secrets can enter a bundle through
 * this core (§32).
 */
class BackupBundle private constructor(
    val manifest: BackupManifest,
    val entries: List<StoredBackupEntry>,
) {

    fun preview(warnings: List<String> = emptyList()): BackupPreview {
        val perProfile = entries.groupBy({ it.profileId }, { it.storeName })
            .mapValues { (_, stores) -> stores.groupingBy { it }.eachCount() }
        return BackupPreview(
            formatVersion = manifest.formatVersion,
            appVersion = manifest.appVersion,
            createdAtMillis = manifest.createdAtMillis,
            perProfile = perProfile,
            warnings = warnings,
        )
    }

    fun serialize(): String = buildString {
        appendLine(HEADER_LINE)
        appendLine("format=${manifest.formatVersion}")
        appendLine("app-version=${manifest.appVersion}")
        appendLine("created-at=${manifest.createdAtMillis}")
        appendLine("entries=${entries.size}")
        for (entry in entries) {
            appendLine(ENTRY_BEGIN)
            appendLine("store=${entry.storeName}")
            appendLine("profile=${entry.profileId}")
            appendLine("sha256=${entry.sha256}")
            appendLine("payload:")
            if (entry.payload.isNotEmpty()) appendLine(entry.payload.trimEnd('\n'))
            appendLine(ENTRY_END)
        }
        appendLine(BUNDLE_END)
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val HEADER = "iNWEB-BACKUP"
        const val HEADER_LINE = "$HEADER v=$FORMAT_VERSION"

        const val ENTRY_BEGIN = "--entry--"
        const val ENTRY_END = "--end-entry--"
        const val BUNDLE_END = "--end--"

        /** iNWEB-owned stores that may be bundled. Nothing else, ever. */
        val KNOWN_STORES = setOf(
            "bookmarks", "history", "downloads", "settings",
            "top-sites", "offline-pages", "extension-registry",
        )

        /**
         * Builds a bundle from store exports, computing per-entry SHA-256
         * checksums. Unknown store names are rejected — the whitelist is
         * the no-plaintext-secrets guard (§32).
         */
        fun build(
            entries: List<BackupEntry>,
            appVersion: String,
            createdAtMillis: Long,
        ): BackupParseResult {
            if (appVersion.isBlank()) return BackupParseResult.Err("app version is blank")
            val unknown = entries.map { it.storeName }.filter { it !in KNOWN_STORES }.distinct()
            if (unknown.isNotEmpty()) {
                return BackupParseResult.Err(
                    "refusing to bundle unknown store(s): ${unknown.joinToString(", ")} " +
                        "(only known iNWEB-owned stores may be backed up — §32 no-plaintext-secrets)",
                )
            }
            val stored = entries.map { entry ->
                val canonical = entry.payload.trimEnd('\n')
                StoredBackupEntry(
                    storeName = entry.storeName,
                    profileId = entry.profileId,
                    payload = canonical,
                    sha256 = sha256Hex(canonical),
                )
            }
            return BackupParseResult.Ok(
                BackupBundle(
                    manifest = BackupManifest(
                        formatVersion = FORMAT_VERSION,
                        appVersion = appVersion,
                        createdAtMillis = createdAtMillis,
                        entryCount = stored.size,
                    ),
                    entries = stored,
                ),
            )
        }

        /** Parses and validates a serialized bundle (corruption-tolerant per entry). */
        fun parse(text: String): BackupParseResult {
            val lines = text.lines()
            if (lines.isEmpty()) return BackupParseResult.Err("empty bundle")

            // --- header + version gating -----------------------------------
            val header = lines[0].trim()
            if (!header.startsWith("$HEADER v=")) {
                return BackupParseResult.Err("not an iNWEB backup (bad header)")
            }
            val version = header.removePrefix("$HEADER v=").trim().toIntOrNull()
                ?: return BackupParseResult.Err("unreadable format version")
            if (version > FORMAT_VERSION) {
                return BackupParseResult.Err(
                    "backup format v$version is newer than this build supports (v$FORMAT_VERSION) — " +
                        "upgrade the app before restoring",
                )
            }
            if (version < FORMAT_VERSION) {
                return BackupParseResult.Err("legacy format v$version has no migration path in this build")
            }

            // --- manifest (corruption here aborts everything) ----------------
            var appVersion: String? = null
            var createdAt: Long? = null
            var declaredCount: Int? = null
            var index = 1
            val warnings = mutableListOf<String>()
            while (index < lines.size && lines[index] != ENTRY_BEGIN) {
                val line = lines[index]
                if (line.isEmpty()) { index++; continue }
                val eq = line.indexOf('=')
                if (eq <= 0) {
                    warnings += "manifest line $index ignored: '$line'"
                } else {
                    when (val key = line.substring(0, eq)) {
                        "format" -> if (line.substring(eq + 1).trim() != "$FORMAT_VERSION") {
                            return BackupParseResult.Err("manifest format field disagrees with header")
                        }
                        "app-version" -> appVersion = line.substring(eq + 1).trim()
                        "created-at" -> createdAt = line.substring(eq + 1).trim().toLongOrNull()
                            ?: return BackupParseResult.Err("created-at is not a number")
                        "entries" -> declaredCount = line.substring(eq + 1).trim().toIntOrNull()
                            ?: return BackupParseResult.Err("entries is not a number")
                        else -> warnings += "unknown manifest key '$key' ignored"
                    }
                }
                index++
            }
            if (appVersion.isNullOrBlank()) return BackupParseResult.Err("manifest is missing app-version")
            if (createdAt == null) return BackupParseResult.Err("manifest is missing created-at")
            if (declaredCount == null) return BackupParseResult.Err("manifest is missing entries")

            // --- entries (corruption skips the entry, not the bundle) ----------
            val entries = mutableListOf<StoredBackupEntry>()
            var sawEnd = false
            while (index < lines.size) {
                if (lines[index] == BUNDLE_END) { sawEnd = true; break }
                if (lines[index] != ENTRY_BEGIN) {
                    warnings += "line $index ignored: '${lines[index]}'"
                    index++
                    continue
                }
                var store: String? = null
                var profile: String? = null
                var checksum: String? = null
                index++
                // field lines until "payload:" (or a broken entry end)
                while (index < lines.size) {
                    val line = lines[index]
                    when {
                        line.startsWith("store=") -> store = line.removePrefix("store=").trim()
                        line.startsWith("profile=") -> profile = line.removePrefix("profile=").trim()
                        line.startsWith("sha256=") -> checksum = line.removePrefix("sha256=").trim()
                        line == "payload:" -> { index++; break }
                        line == ENTRY_END || line == BUNDLE_END -> break
                        else -> warnings += "entry line $index ignored: '$line'"
                    }
                    index++
                }
                val payloadBuilder = StringBuilder()
                var closed = false
                while (index < lines.size) {
                    if (lines[index] == ENTRY_END) { closed = true; break }
                    if (lines[index] == BUNDLE_END) break
                    if (payloadBuilder.isNotEmpty()) payloadBuilder.append('\n')
                    payloadBuilder.append(lines[index])
                    index++
                }
                if (!closed) warnings += "unterminated entry (store=${store ?: "?"}) — skipped"
                val payload = payloadBuilder.toString()
                val label = "${store ?: "?"}/${profile ?: "?"}"
                if (store == null || profile == null || checksum == null) {
                    warnings += "entry $label has missing fields — skipped"
                } else if (store !in KNOWN_STORES) {
                    warnings += "entry $label names an unknown store — skipped (§32)"
                } else if (sha256Hex(payload) != checksum) {
                    warnings += "entry $label failed its checksum — skipped (corrupt payload)"
                } else {
                    entries += StoredBackupEntry(store, profile, payload, checksum)
                }
                index++ // past ENTRY_END or BUNDLE_END
            }
            if (!sawEnd) warnings += "bundle end marker missing (read to end of file)"
            if (entries.size != declaredCount) {
                warnings += "manifest declared $declaredCount entries, ${entries.size} are intact"
            }
            return BackupParseResult.Ok(
                BackupBundle(
                    manifest = BackupManifest(FORMAT_VERSION, appVersion, createdAt!!, declaredCount),
                    entries = entries,
                ),
                warnings,
            )
        }

        /** Platform SHA-256 (integrity primitive — no custom crypto, ADR-025). */
        internal fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
