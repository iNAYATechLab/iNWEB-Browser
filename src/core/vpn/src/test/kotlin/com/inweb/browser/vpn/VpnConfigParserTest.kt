package com.inweb.browser.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnConfigParserTest {

    // a real 44-char base64 string encoding 32 zero bytes
    // (40 chars for ten 3-byte groups + "AAA=" for the final 2 bytes)
    private val key32 = "A".repeat(40) + "AAA="
    private val validConfig = """
        # iNWEB test tunnel
        [Interface]
        PrivateKey = $key32
        Address = 10.7.0.2/24, fd00:abcd::2/64
        DNS = 1.1.1.1, 2606:4700:4700::1111

        [Peer]
        PublicKey = $key32
        AllowedIPs = 0.0.0.0/0, ::/0
        Endpoint = vpn.example.com:51820
        PersistentKeepalive = 25
    """.trimIndent()

    private fun config(): VpnConfig =
        (VpnConfigParser.parse(validConfig) as VpnConfigParseResult.Ok).config

    // --- happy path ----------------------------------------------------------

    @Test
    fun parsesValidClientConfig() {
        val cfg = config()
        assertEquals(listOf("10.7.0.2/24", "fd00:abcd::2/64"), cfg.interfaceConfig.addresses)
        assertEquals(listOf("1.1.1.1", "2606:4700:4700::1111"), cfg.interfaceConfig.dns)
        assertEquals(null, cfg.interfaceConfig.listenPort)
        val peer = cfg.peers.single()
        assertEquals(key32, peer.publicKey)
        assertEquals("vpn.example.com:51820", peer.endpoint)
        assertEquals(listOf("0.0.0.0/0", "::/0"), peer.allowedIPs)
        assertEquals(25, peer.persistentKeepalive)
    }

    @Test
    fun listFieldsMaySpanSeveralLines() {
        val text = """
            [Interface]
            PrivateKey = $key32
            Address = 10.0.0.2/24
            Address = 10.0.1.2/24
            DNS = 1.1.1.1

            [Peer]
            PublicKey = $key32
            AllowedIPs = 10.0.0.0/8
            AllowedIPs = 192.168.0.0/16
            Endpoint = 203.0.113.7:51820
        """.trimIndent()
        val cfg = (VpnConfigParser.parse(text) as VpnConfigParseResult.Ok).config
        assertEquals(listOf("10.0.0.2/24", "10.0.1.2/24"), cfg.interfaceConfig.addresses)
        assertEquals(listOf("10.0.0.0/8", "192.168.0.0/16"), cfg.peers.single().allowedIPs)
    }

    @Test
    fun presharedKeyIsOptionalButValidatedWhenPresent() {
        val base = """
            [Interface]
            PrivateKey = $key32
            [Peer]
            PublicKey = $key32
            Endpoint = vpn.example.org:51820
        """.trimIndent()
        assertTrue(VpnConfigParser.parse(base) is VpnConfigParseResult.Ok)

        val withBadPsk = base.replace("Endpoint", "PresharedKey = not!base64\nEndpoint")
        assertTrue(VpnConfigParser.parse(withBadPsk) is VpnConfigParseResult.Err)
    }

    // --- structural errors ------------------------------------------------------

    @Test
    fun missingSectionsAreReported() {
        val result = VpnConfigParser.parse("# nothing here\n")
        val issues = (result as VpnConfigParseResult.Err).issues
        assertTrue(issues.any { it.contains("missing [Interface]") })
        assertTrue(issues.any { it.contains("missing [Peer]") })
    }

    @Test
    fun duplicateInterfaceSectionIsRejected() {
        val text = """
            [Interface]
            PrivateKey = $key32
            [Interface]
            PrivateKey = $key32
            [Peer]
            PublicKey = $key32
        """.trimIndent()
        assertTrue(VpnConfigParser.parse(text) is VpnConfigParseResult.Err)
    }

    @Test
    fun unknownSectionAndUnknownFieldsAreRejected() {
        val text = """
            [Interface]
            PrivateKey = $key32
            [wg-quick host settings]
            PostUp = echo hi
            [Peer]
            PublicKey = $key32
            Table = off
        """.trimIndent()
        val issues = (VpnConfigParser.parse(text) as VpnConfigParseResult.Err).issues
        assertTrue(issues.any { it.contains("unknown section") })
        assertTrue(issues.any { it.contains("unknown field 'Table'") })
    }

    @Test
    fun duplicateScalarFieldIsRejected() {
        val text = """
            [Interface]
            PrivateKey = $key32
            PrivateKey = $key32
            [Peer]
            PublicKey = $key32
        """.trimIndent()
        val issues = (VpnConfigParser.parse(text) as VpnConfigParseResult.Err).issues
        assertTrue(issues.any { it.contains("duplicate field 'PrivateKey'") })
    }

    @Test
    fun requiredKeysAreEnforced() {
        val text = """
            [Interface]
            Address = 10.0.0.2/24
            [Peer]
            Endpoint = vpn.example.com:51820
        """.trimIndent()
        val issues = (VpnConfigParser.parse(text) as VpnConfigParseResult.Err).issues
        assertTrue(issues.any { it.contains("PrivateKey is required") })
        assertTrue(issues.any { it.contains("PublicKey is required") })
    }

    @Test
    fun allIssuesAreCollectedNotFailFast() {
        val text = "garbage\n[Peer]\nEndpoint = bad\n"
        val issues = (VpnConfigParser.parse(text) as VpnConfigParseResult.Err).issues
        assertTrue(issues.size >= 4)
    }

    // --- value validation ---------------------------------------------------------

    @Test
    fun keysMustBeBase64OfExactly32Bytes() {
        fun cfg(privateKey: String) = """
            [Interface]
            PrivateKey = $privateKey
            [Peer]
            PublicKey = $privateKey
        """.trimIndent()
        assertTrue(VpnConfigParser.parse(cfg(key32)) is VpnConfigParseResult.Ok)
        // 31 bytes / 43 chars (wrong length)
        assertTrue(VpnConfigParser.parse(cfg("A".repeat(43))) is VpnConfigParseResult.Err)
        // invalid characters (44 chars, but '!' is not base64)
        assertTrue(VpnConfigParser.parse(cfg("A".repeat(42) + "!=")) is VpnConfigParseResult.Err)
    }

    @Test
    fun invalidCidrsAreRejected() {
        fun issueCount(addressLine: String): Int {
            val text = """
                [Interface]
                PrivateKey = $key32
                Address = $addressLine
                [Peer]
                PublicKey = $key32
            """.trimIndent()
            return when (val result = VpnConfigParser.parse(text)) {
                is VpnConfigParseResult.Err -> result.issues.size
                is VpnConfigParseResult.Ok -> 0
            }
        }
        assertTrue(issueCount("10.0.0.2") > 0)             // no prefix
        assertTrue(issueCount("10.0.0.2/33") > 0)          // v4 prefix too large
        assertTrue(issueCount("fd00::2/129") > 0)          // v6 prefix too large
        assertTrue(issueCount("300.1.1.1/8") > 0)          // bad octet
        assertTrue(issueCount("10.0.0.2/08") > 0)          // leading-zero prefix
        assertTrue(issueCount("10.0.0.2/24/16") > 0)       // two slashes
    }

    @Test
    fun validIpv6FormsAreAccepted() {
        fun ok(addressLine: String): Boolean {
            val text = """
                [Interface]
                PrivateKey = $key32
                Address = $addressLine
                [Peer]
                PublicKey = $key32
            """.trimIndent()
            return VpnConfigParser.parse(text) is VpnConfigParseResult.Ok
        }
        assertTrue(ok("fd00:abcd::2/64"))
        assertTrue(ok("2001:db8:1:2:3:4:5:6/64"))
        assertTrue(ok("::/0"))
        assertFalse(ok("1:2:3:4:5:6:7:8:9/64"))   // too many groups
        assertFalse(ok("1::2::3/64"))             // two compressions
    }

    @Test
    fun endpointsMustBeHostPort() {
        fun issueCount(endpoint: String): Int {
            val text = """
                [Interface]
                PrivateKey = $key32
                [Peer]
                PublicKey = $key32
                Endpoint = $endpoint
            """.trimIndent()
            return when (val result = VpnConfigParser.parse(text)) {
                is VpnConfigParseResult.Err -> result.issues.size
                is VpnConfigParseResult.Ok -> 0
            }
        }
        assertTrue(issueCount("vpn.example.com:51820") == 0)
        assertTrue(issueCount("203.0.113.7:51820") == 0)
        assertTrue(issueCount("[2001:db8::1]:51820") == 0)
        assertTrue(issueCount("vpn.example.com") > 0)        // no port
        assertTrue(issueCount("vpn.example.com:0") > 0)      // port 0
        assertTrue(issueCount("vpn.example.com:70000") > 0)  // port too large
        assertTrue(issueCount("bad host:51820") > 0)         // space in host
    }

    @Test
    fun listenPortMustBeInRange() {
        fun withPort(port: String) = """
            [Interface]
            PrivateKey = $key32
            ListenPort = $port
            [Peer]
            PublicKey = $key32
        """.trimIndent()
        assertTrue(VpnConfigParser.parse(withPort("51821")) is VpnConfigParseResult.Ok)
        assertTrue(VpnConfigParser.parse(withPort("0")) is VpnConfigParseResult.Err)
        assertTrue(VpnConfigParser.parse(withPort("70000")) is VpnConfigParseResult.Err)
    }

    @Test
    fun dnsMustBePlainIps() {
        val text = """
            [Interface]
            PrivateKey = $key32
            DNS = 1.1.1.1, example.com
            [Peer]
            PublicKey = $key32
        """.trimIndent()
        val issues = (VpnConfigParser.parse(text) as VpnConfigParseResult.Err).issues
        assertTrue(issues.any { it.contains("'example.com' is not a valid IP") })
    }

    // --- secret opacity (§15 / ADR-025) ----------------------------------------------

    @Test
    fun secretsNeverAppearInToString() {
        // a config whose PRIVATE key differs from the peer's PUBLIC key
        val privateKey = "B".repeat(40) + "AAA="
        val text = validConfig.replace(
            "PrivateKey = $key32",
            "PrivateKey = $privateKey",
        )
        val cfg = (VpnConfigParser.parse(text) as VpnConfigParseResult.Ok).config
        val rendered = cfg.toString() + cfg.interfaceConfig.toString() +
            cfg.peers.joinToString { it.toString() }
        assertFalse("private key leaked", rendered.contains(privateKey))
        // the peer's PUBLIC key is public information and may appear
        assertTrue(rendered.contains(key32))
        assertTrue(rendered.contains("<redacted>"))
    }

    @Test
    fun secretValueRedactsAlways() {
        assertEquals("<redacted>", SecretValue(key32).toString())
        // equality still works (usable in tests / dedupe logic)
        assertEquals(SecretValue(key32), SecretValue(key32))
    }
}
