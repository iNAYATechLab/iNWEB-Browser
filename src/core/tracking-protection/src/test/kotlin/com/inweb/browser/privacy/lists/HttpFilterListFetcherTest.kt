package com.inweb.browser.privacy.lists

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Real HTTP round-trip tests against the JDK's built-in HttpServer — no
 * mocks: actual sockets, actual conditional headers, actual status codes.
 */
class HttpFilterListFetcherTest {

    private lateinit var server: HttpServer
    private lateinit var listUrl: String

    private val receivedValidators = CopyOnWriteArrayList<Pair<String?, String?>>()

    private fun startServer(handler: (HttpExchange) -> Unit) {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            receivedValidators += exchange.requestHeaders.getFirst("If-None-Match") to
                exchange.requestHeaders.getFirst("If-Modified-Since")
            handler(exchange)
            exchange.close()
        }
        server.start()
        listUrl = "http://127.0.0.1:${server.address.port}/list.txt"
    }

    @After
    fun stopServer() {
        if (::server.isInitialized) server.stop(0)
    }

    private fun serveOk(body: String, etag: String? = null, lastModified: String? = null): (HttpExchange) -> Unit =
        { exchange ->
            if (etag != null) exchange.responseHeaders.add("ETag", etag)
            if (lastModified != null) exchange.responseHeaders.add("Last-Modified", lastModified)
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }

    @Test
    fun fetch200ReadsBodyAndValidators() {
        startServer(
            serveOk(
                body = "! Version: 1\n||ads.example.com^\n",
                etag = "\"v1\"",
                lastModified = "Fri, 11 Sep 2026 00:00:00 GMT",
            ),
        )
        val result = HttpFilterListFetcher().fetch(listUrl)
        assertTrue("expected Success but was $result", result is FetchResult.Success)
        result as FetchResult.Success
        assertEquals("! Version: 1\n||ads.example.com^\n", result.body)
        assertEquals("\"v1\"", result.etag)
        assertEquals("Fri, 11 Sep 2026 00:00:00 GMT", result.lastModified)
    }

    @Test
    fun conditionalValidatorsAreSentToServer() {
        startServer { exchange -> exchange.sendResponseHeaders(304, -1) }
        val result = HttpFilterListFetcher().fetch(
            listUrl,
            etag = "\"cached-etag\"",
            lastModified = "Wed, 10 Sep 2026 00:00:00 GMT",
        )
        assertTrue(result is FetchResult.NotModified)
        assertEquals("\"cached-etag\"" to "Wed, 10 Sep 2026 00:00:00 GMT", receivedValidators.first())
    }

    @Test
    fun http304WithoutValidatorsStillNotModified() {
        startServer { exchange -> exchange.sendResponseHeaders(304, -1) }
        val result = HttpFilterListFetcher().fetch(listUrl)
        assertTrue(result is FetchResult.NotModified)
    }

    @Test
    fun httpErrorYieldsFailure() {
        startServer { exchange -> exchange.sendResponseHeaders(404, -1) }
        val result = HttpFilterListFetcher().fetch(listUrl)
        assertTrue("expected Failure but was $result", result is FetchResult.Failure)
        assertEquals("HTTP 404", (result as FetchResult.Failure).reason)
    }

    @Test
    fun unreachableServerYieldsFailure() {
        // grab a port and close it again — nothing is listening there
        val socket = ServerSocket(0)
        val deadPort = socket.localPort
        socket.close()
        val result = HttpFilterListFetcher().fetch("http://127.0.0.1:$deadPort/list.txt")
        assertTrue("expected Failure but was $result", result is FetchResult.Failure)
    }

    @Test
    fun bodyOverSizeCapYieldsFailure() {
        startServer { exchange ->
            val bytes = ByteArray(64) { 'x'.code.toByte() }
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        val fetcher = HttpFilterListFetcher(maxBodyBytes = 16)
        val result = fetcher.fetch(listUrl)
        assertTrue("expected Failure but was $result", result is FetchResult.Failure)
        assertTrue((result as FetchResult.Failure).reason.contains("exceeds"))
    }
}
