# `src/core/vpn` — VPN Configuration Validation Core (pure JVM)

Structural validation of imported VPN configurations (Phase 8 design:
`docs/PHASE8-VPN-SECURITY-DESIGN.md` §5). The import UI accepts a
configuration **only** when this parser returns `Ok` — no fake
"imported" state for broken files (§15).

**Pure JVM, no Android dependency** — compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit); the
`security/` patches (0020–0021) bind it to the real `VpnService` tunnel
module (build infrastructure, B-001).

## Components

| Component | Purpose |
|---|---|
| `VpnConfigParser` | WireGuard-style INI parsing with STRICT validation: exact section/field names, required keys, base64 keys of exactly 32 bytes, CIDR syntax (IPv4/IPv6, correct prefix ranges), `host:port` endpoints (domain / IPv4 / `[IPv6]`), port/keepalive ranges; all issues collected — never fail-fast; list fields (`Address`, `DNS`, `AllowedIPs`) may span lines, scalars may not repeat |
| `VpnConfig` / `VpnInterfaceConfig` / `VpnPeer` | The parsed model |
| `SecretValue` | Private/preshared keys are opaque by construction — `toString` always redacts, so logs and crash reports cannot leak key material |

## Design invariants (ADR-025)

- **Secrets are opaque to the core**: no crypto, no key derivation, no
  echo of key material — structural validation only; the Android layer
  owns actual key handling.
- Unknown sections/fields (e.g. wg-quick host features `PostUp`,
  `Table`) are REJECTED with explicit issues — never silently ignored.
- Expected failures are `Err(issues)` values, never exceptions.
- Android note: `java.util.Base64` requires API 26+ or core-library
  desugaring on device; the binding patch selects the platform decoder.
