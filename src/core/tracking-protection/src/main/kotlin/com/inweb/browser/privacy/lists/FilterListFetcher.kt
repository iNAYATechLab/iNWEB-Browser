package com.inweb.browser.privacy.lists

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Outcome of one filter-list download attempt. */
sealed interface FetchResult {
    /** 200 OK — full body plus revalidation validators (may be null). */
    data class Success(val body: String, val etag: String?, val lastModified: String?) : FetchResult

    /** 304 Not Modified — cached content is still current. */
    data class NotModified(val etag: String?, val lastModified: String?) : FetchResult

    /** Transport or HTTP failure — the caller keeps serving cached content. */
    data class Failure(val reason: String) : FetchResult
}

/**
 * Port: how filter lists are downloaded. The pure-JVM [HttpFilterListFetcher]
 * is the default implementation; the engine patches (B-001) and the Android
 * layer may provide their own transport.
 */
interface FilterListFetcher {
    /**
     * Fetches `url`. When [etag] / [lastModified] are supplied they are sent
     * as conditional validators (`If-None-Match` / `If-Modified-Since`) so an
     * unchanged list costs one cheap 304 round-trip.
     */
    fun fetch(url: String, etag: String? = null, lastModified: String? = null): FetchResult
}

/**
 * Real JVM download implementation over `HttpURLConnection`:
 * timeouts, same-protocol redirects, conditional revalidation, and a hard
 * response-size cap so a misbehaving mirror cannot exhaust memory.
 */
class HttpFilterListFetcher(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 30_000,
    private val maxBodyBytes: Long = 64L * 1024 * 1024,
) : FilterListFetcher {

    override fun fetch(url: String, etag: String?, lastModified: String?): FetchResult {
        var connection: HttpURLConnection? = null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            connection = conn
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.instanceFollowRedirects = true
            if (etag != null) conn.setRequestProperty("If-None-Match", etag)
            if (lastModified != null) conn.setRequestProperty("If-Modified-Since", lastModified)

            when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_OK -> FetchResult.Success(
                    body = readBodyBounded(conn),
                    etag = conn.getHeaderField("ETag"),
                    lastModified = conn.getHeaderField("Last-Modified"),
                )
                HttpURLConnection.HTTP_NOT_MODIFIED -> FetchResult.NotModified(
                    etag = conn.getHeaderField("ETag"),
                    lastModified = conn.getHeaderField("Last-Modified"),
                )
                else -> FetchResult.Failure("HTTP $code")
            }
        } catch (e: Exception) {
            FetchResult.Failure(e.message ?: e.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBodyBounded(conn: HttpURLConnection): String {
        val declared = conn.contentLengthLong
        if (declared > maxBodyBytes) {
            throw IOException("response body exceeds limit ($declared bytes)")
        }
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        conn.inputStream.use { input ->
            while (true) {
                val read = input.read(chunk)
                if (read < 0) break
                output.write(chunk, 0, read)
                if (output.size().toLong() > maxBodyBytes) {
                    throw IOException("response body exceeds limit ($maxBodyBytes bytes)")
                }
            }
        }
        return output.toString("UTF-8")
    }
}
