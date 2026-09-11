package com.inweb.browser.session

import android.content.Context
import com.inweb.browser.shell.SessionPersistence
import java.io.File

/**
 * File-backed session persistence.
 *
 * Writes atomically (temp file + rename) so a crash mid-write can never
 * corrupt the previous snapshot (MASTER-SPEC §51).
 */
class FileSessionPersistence(context: Context) : SessionPersistence {

    private val file: File = File(context.filesDir, "session.inweb")
    private val temp: File = File(context.filesDir, "session.inweb.tmp")

    override fun save(text: String) {
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            // Some filesystems refuse rename-over-existing: replace explicitly.
            file.delete()
            check(temp.renameTo(file)) { "atomic session write failed" }
        }
    }

    override fun load(): String? = if (file.exists()) file.readText() else null

    override fun clear() {
        file.delete()
        temp.delete()
    }
}
