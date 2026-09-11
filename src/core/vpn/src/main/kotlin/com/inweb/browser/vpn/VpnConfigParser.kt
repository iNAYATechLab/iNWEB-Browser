package com.inweb.browser.vpn

/**
 * A secret value (private/preshared keys). Opaque by construction:
 * [toString] always redacts, so data-class rendering, logs, and crash
 * reports can never leak key material. Equality still works for tests.
 */
class SecretValue(val value: String) {
    override fun toString(): String = "<redacted>"
    override fun equals(other: Any?): Boolean = other is SecretValue && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

/** The `[Interface]` section of an imported client configuration. */
data class VpnInterfaceConfig(
    val privateKey: SecretValue,
    /** CIDR list (may span several Address lines). */
    val addresses: List<String>,
    /** Plain IP list (may span several DNS lines). */
    val dns: List<String>,
    val listenPort: Int?,
)

/** One `[Peer]` section. */
data class VpnPeer(
    val publicKey: String,
    val presharedKey: SecretValue?,
    /** host:port — the client endpoint of this peer (optional per peer). */
    val endpoint: String?,
    /** CIDR list (may span several AllowedIPs lines). */
    val allowedIPs: List<String>,
    val persistentKeepalive: Int?,
)

/** A structurally valid, parsed VPN client configuration. */
data class VpnConfig(
    val interfaceConfig: VpnInterfaceConfig,
    val peers: List<VpnPeer>,
)

/** Parse outcome: all issues are collected — never fail-fast. */
sealed class VpnConfigParseResult {
    data class Ok(val config: VpnConfig) : VpnConfigParseResult()
    data class Err(val issues: List<String>) : VpnConfigParseResult()
}

/**
 * WireGuard-style configuration parser with STRICT structural validation
 * (§15/ADR-025): the import UI accepts a configuration only when this
 * parser says Ok — a fake "imported" state for a broken file would be a
 * §15 violation waiting to happen.
 *
 * Supported sections/fields (exact names, case-sensitive):
 *  [Interface] PrivateKey, Address*, DNS*, ListenPort
 *  [Peer]     PublicKey, PresharedKey, AllowedIPs*, Endpoint, PersistentKeepalive
 *  (* list-valued fields may repeat on several lines; scalars may not)
 *
 * NOT supported (by design): `Table`, `PostUp/PostDown`, `SaveConfig` —
 * wg-quick host features with no meaning on Android; a config using them
 * still parses if the unknown fields are absent, otherwise it is rejected
 * with an explicit issue (no silent ignoring).
 */
object VpnConfigParser {

    private const val KEY_BYTES = 32
    private val BASE64_32 = Regex("^[A-Za-z0-9+/]{43}=$")

    fun parse(text: String): VpnConfigParseResult {
        val issues = mutableListOf<String>()
        var interfaceFields: MutableMap<String, MutableList<String>>? = null
        val peerFields = mutableListOf<MutableMap<String, MutableList<String>>>()
        var current: MutableMap<String, MutableList<String>>? = null
        var inPeer = false

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            val lineNumber = index + 1
            when {
                line.isEmpty() || line.startsWith("#") -> {}
                line.equals("[Interface]", ignoreCase = false) -> {
                    if (interfaceFields != null) issues += "line $lineNumber: duplicate [Interface] section"
                    interfaceFields = mutableMapOf()
                    current = interfaceFields
                    inPeer = false
                }
                line.equals("[Peer]", ignoreCase = false) -> {
                    val fields = mutableMapOf<String, MutableList<String>>()
                    peerFields += fields
                    current = fields
                    inPeer = true
                }
                line.startsWith("[") -> issues += "line $lineNumber: unknown section '$line'"
                else -> {
                    val target = current
                    if (target == null) {
                        issues += "line $lineNumber: field outside any section"
                        return@forEachIndexed
                    }
                    val eq = line.indexOf('=')
                    if (eq <= 0 || eq == line.length - 1) {
                        issues += "line $lineNumber: expected 'Field = value', got '$line'"
                        return@forEachIndexed
                    }
                    val name = line.substring(0, eq).trim()
                    val value = line.substring(eq + 1).trim()
                    val known = if (inPeer) PEER_FIELDS else INTERFACE_FIELDS
                    if (name !in known) {
                        issues += "line $lineNumber: unknown field '$name' in ${if (inPeer) "[Peer]" else "[Interface]"}"
                        return@forEachIndexed
                    }
                    val listType = name in LIST_FIELDS
                    if (!listType && target.containsKey(name)) {
                        issues += "line $lineNumber: duplicate field '$name'"
                        return@forEachIndexed
                    }
                    target.getOrPut(name) { mutableListOf() } += value
                }
            }
        }

        val iface = interfaceFields
        if (iface == null) issues += "missing [Interface] section"
        if (peerFields.isEmpty()) issues += "missing [Peer] section (at least one required)"

        // --- [Interface] validation ---------------------------------------
        var privateKey: SecretValue? = null
        var listenPort: Int? = null
        val addresses = mutableListOf<String>()
        val dns = mutableListOf<String>()
        if (iface != null) {
            val pk = iface["PrivateKey"]?.singleOrNull()
            if (pk == null) {
                issues += "[Interface] PrivateKey is required"
            } else if (!isValidKey(pk)) {
                issues += "[Interface] PrivateKey must be base64 encoding exactly $KEY_BYTES bytes"
            } else {
                privateKey = SecretValue(pk)
            }
            iface["Address"]?.forEach { addresses += splitList(it, "Address", issues) }
            addresses.forEach { if (!isValidCidr(it)) issues += "[Interface] Address '$it' is not a valid CIDR" }
            iface["DNS"]?.forEach { dns += splitList(it, "DNS", issues) }
            dns.forEach { if (!isValidIp(it)) issues += "[Interface] DNS '$it' is not a valid IP address" }
            listenPort = parsePort(iface["ListenPort"]?.singleOrNull(), "[Interface] ListenPort", issues)
        }

        // --- [Peer] validation ----------------------------------------------
        val peers = mutableListOf<VpnPeer>()
        peerFields.forEachIndexed { peerIndex, fields ->
            val label = "[Peer] #${peerIndex + 1}"
            val pub = fields["PublicKey"]?.singleOrNull()
            if (pub == null) {
                issues += "$label PublicKey is required"
            } else if (!isValidKey(pub)) {
                issues += "$label PublicKey must be base64 encoding exactly $KEY_BYTES bytes"
            }
            val psk = fields["PresharedKey"]?.singleOrNull()
            if (psk != null && !isValidKey(psk)) {
                issues += "$label PresharedKey must be base64 encoding exactly $KEY_BYTES bytes"
            }
            val endpoint = fields["Endpoint"]?.singleOrNull()
            if (endpoint != null && !isValidEndpoint(endpoint)) {
                issues += "$label Endpoint '$endpoint' is not a valid host:port"
            }
            val allowed = mutableListOf<String>()
            fields["AllowedIPs"]?.forEach { allowed += splitList(it, "AllowedIPs", issues) }
            allowed.forEach { if (!isValidCidr(it)) issues += "$label AllowedIPs '$it' is not a valid CIDR" }
            val keepaliveRaw = fields["PersistentKeepalive"]?.singleOrNull()
            var keepalive: Int? = null
            if (keepaliveRaw != null) {
                val n = keepaliveRaw.toIntOrNull()
                if (n == null || n !in 0..65535) {
                    issues += "$label PersistentKeepalive must be an integer in 0..65535"
                } else {
                    keepalive = n
                }
            }
            if (pub != null && isValidKey(pub)) {
                peers += VpnPeer(
                    publicKey = pub,
                    presharedKey = if (psk != null && isValidKey(psk)) SecretValue(psk) else null,
                    endpoint = endpoint,
                    allowedIPs = allowed,
                    persistentKeepalive = keepalive,
                )
            }
        }

        if (issues.isNotEmpty()) return VpnConfigParseResult.Err(issues)
        return VpnConfigParseResult.Ok(
            VpnConfig(
                interfaceConfig = VpnInterfaceConfig(
                    privateKey = privateKey!!,
                    addresses = addresses,
                    dns = dns,
                    listenPort = listenPort,
                ),
                peers = peers,
            ),
        )
    }

    // --- field tables ---------------------------------------------------------

    private val INTERFACE_FIELDS = setOf("PrivateKey", "Address", "DNS", "ListenPort")
    private val PEER_FIELDS = setOf("PublicKey", "PresharedKey", "AllowedIPs", "Endpoint", "PersistentKeepalive")
    private val LIST_FIELDS = setOf("Address", "DNS", "AllowedIPs")

    // --- validators -----------------------------------------------------------

    private fun splitList(value: String, field: String, issues: MutableList<String>): List<String> =
        value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.also {
            if (it.isEmpty()) issues += "$field value '$value' contains no usable entries"
        }

    /** base64 with exactly [KEY_BYTES] decoded bytes (44 chars, one '='). */
    internal fun isValidKey(value: String): Boolean =
        BASE64_32.matches(value) && runCatching { java.util.Base64.getDecoder().decode(value).size == KEY_BYTES }.getOrDefault(false)

    internal fun isValidIp(addr: String): Boolean = isValidIpv4(addr) || isValidIpv6(addr)

    internal fun isValidIpv4(addr: String): Boolean {
        val octets = addr.split('.')
        if (octets.size != 4) return false
        for (o in octets) {
            if (o.isEmpty() || o.length > 3 || !o.all { it in '0'..'9' }) return false
            if (o.length > 1 && o[0] == '0') return false
            if (o.toInt() > 255) return false
        }
        return true
    }

    internal fun isValidIpv6(addr: String): Boolean {
        if (addr.isEmpty()) return false
        val parts = addr.split("::")
        if (parts.size > 2) return false
        fun groups(s: String): List<String>? {
            if (s.isEmpty()) return emptyList()
            val gs = s.split(':')
            if (gs.any { it.isEmpty() || it.length > 4 || !it.all { c -> c.isHex() } }) return null
            return gs
        }
        val left = groups(parts[0]) ?: return false
        val right = if (parts.size == 2) groups(parts[1]) ?: return false else emptyList()
        return if (parts.size == 1) left.size == 8 else left.size + right.size <= 7
    }

    internal fun isValidCidr(cidr: String): Boolean {
        val slash = cidr.lastIndexOf('/')
        if (slash <= 0 || slash == cidr.length - 1 || cidr.indexOf('/') != slash) return false
        val addr = cidr.substring(0, slash)
        val prefix = cidr.substring(slash + 1)
        if (prefix.isEmpty() || prefix.length > 3 || !prefix.all { it in '0'..'9' }) return false
        if (prefix.length > 1 && prefix[0] == '0') return false
        val p = prefix.toInt()
        return when {
            isValidIpv4(addr) -> p in 0..32
            isValidIpv6(addr) -> p in 0..128
            else -> false
        }
    }

    internal fun isValidEndpoint(endpoint: String): Boolean {
        val colon = endpoint.lastIndexOf(':')
        if (colon <= 0 || colon == endpoint.length - 1) return false
        val host = endpoint.substring(0, colon)
        val port = endpoint.substring(colon + 1)
        if (port.isEmpty() || port.length > 5 || !port.all { it in '0'..'9' }) return false
        if (port.toInt() !in 1..65535) return false
        return when {
            host.startsWith("[") && host.endsWith("]") && host.length > 2 ->
                isValidIpv6(host.substring(1, host.length - 1))
            else -> isValidIpv4(host) || isValidDomain(host)
        }
    }

    internal fun isValidDomain(host: String): Boolean {
        if (host.isEmpty() || host.length > 253) return false
        val labels = host.lowercase().split('.')
        if (labels.any { it.isEmpty() || it.length > 63 }) return false
        return labels.all { label ->
            label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' } &&
                !label.startsWith("-") && !label.endsWith("-")
        }
    }

    private fun parsePort(raw: String?, label: String, issues: MutableList<String>): Int? {
        if (raw == null) return null
        val n = raw.toIntOrNull()
        if (n == null || n !in 1..65535) {
            issues += "$label must be an integer in 1..65535"
            return null
        }
        return n
    }

    private fun Char.isHex(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
