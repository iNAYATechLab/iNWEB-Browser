# Phase 8 — VPN & Security Design

**Status:** authored design (Step 23). Code lands as pure-JVM cores
(CI-tested) plus `security/` patch entries generated against the pinned
baseline `154.0.8037.21` on build infrastructure (blocker B-001).
**Governing requirements:** MASTER-SPEC §15 (secure VPN), §25 (biometric
security), §26 (2FA), §27 (encryption). Scope note: §28 profiles belongs
to Phase 9 and is out of this document.

Honesty rules that bind every line below (verbatim intent):
§15 *"Never implement a fake VPN interface that claims traffic is
protected when it is not"*; §25 *"Do not claim biometric security if the
underlying data is not actually protected"*; §26 *"Do not add
meaningless 2FA UI without an authentication backend"*; §27 *"Never
hard-code secrets."*

---

## 1. §15 Secure VPN — bring-your-own-server model

**What ships:** a REAL Android `VpnService`-based tunnel module speaking
the WireGuard protocol, with user-imported configuration. iNWEB operates
**no VPN servers** — the user (or their organization) provisions the
endpoint. This is the honest architecture: a browser vendor without
server infrastructure cannot truthfully sell "protected traffic"; a
real protocol client with user-owned endpoints can.

- **Tunnel:** `VpnService` + WireGuard-protocol client (the proven
  Android implementation model); config import from file/QR.
- **Routing & DNS:** full-tunnel default (`0.0.0.0/0, ::/0` allowed-IPs
  from the imported config); DNS forced through the tunnel — no
  plaintext system-DNS fallback while connected.
- **Kill-switch / leak protection:** Android's always-on VPN +
  "block connections without VPN" settings (presented during setup);
  connection-state surface shows the REAL tunnel state from
  `VpnService` callbacks — never a cosmetic toggle.
- **Credential security:** private keys live in Android Keystore-backed
  storage (§3 envelope), never in plain preferences; imported config is
  validated before acceptance (pure-JVM core, §5).
- **Server infrastructure requirements (§15's own demand):** for the
  bring-your-own model — one WireGuard endpoint the user controls;
  bandwidth and uptime are the user's. If an iNWEB-hosted service is
  ever considered, §15/§20 require a separate, explicit design
  (server architecture, encryption model, privacy and security
  implications, bandwidth model, operations) BEFORE any UI claims it —
  this document deliberately claims none.

**Boundary:** VPN protection covers DEVICE traffic (that is what
`VpnService` can do); it is not a browser-internal feature pretending to
be more. A "VPN" toggle that only changes settings is a §15 violation
and does not exist in this design.

## 2. §25 Biometric security — actually-protected data only

- **Mechanism:** `BiometricPrompt` + Android Keystore key with
  `setUserAuthenticationRequired` — the key unlocks only on successful
  biometric/device-credential auth, and the protected data is encrypted
  WITH that key (AES/GCM). No biometric flag guarding plaintext, ever.
- **v1 protected operations (each real):**
  1. **App lock** — browser launch and returning from recents requires
     auth; the unlock key decrypts the app's sensitive settings blob.
  2. **Incognito/private-profile gate** — entering private browsing
     requires auth (the private-browsing data itself is Phase 3's
     in-memory model; the GATE is real, storage encryption for
     profiles arrives with Phase 9 profiles).
- **Explicitly not claimed in v1:** "protected bookmarks"/"encrypted
  vault" — these ship only when the encrypted stores themselves exist
  (Phase 9+); a biometric lock on unencrypted data would be a §25
  violation.

## 3. §27 Encryption — platform primitives only

- Android Keystore for key material (hardware-backed where the device
  supports it); AES/GCM envelope encryption for every stored secret,
  token, or protected blob; per-purpose data keys wrapped by the
  Keystore key.
- **No hand-rolled cryptography anywhere** — core modules do not
  implement primitives; they delegate to platform APIs at the Android
  layer (pure-JVM cores handle structure and validation, never crypto).
- **No hard-coded secrets:** repo-auditable today — the codebase
  contains zero secrets; CI can grep-enforce this going forward.

## 4. §26 2FA — documented absence

There is **no authentication backend** in iNWEB v1 (no accounts, no
cloud sync — Phase 9 scope, and conditional on real infrastructure).
Therefore there is **no 2FA UI** — adding one would be §26's forbidden
"meaningless 2FA". When (and only when) an account/sync backend is
designed, TOTP (RFC 6238) is the intended mechanism for login, sync
authorization, and recovery flows; that design will be its own step.

## 5. Pure-JVM core scoping (next step's deliverable)

`src/core/vpn` — **VPN configuration validation** (proposed Step 24),
testable without a device:

- WireGuard-style config parsing: `[Interface]` (PrivateKey, Address,
  DNS) and `[Peer]` (PublicKey, Endpoint, AllowedIPs) sections
- Validation: required fields present; keys are valid base64 with
  correct decoded length (32 bytes); endpoints host:port; CIDR notation
  for addresses/allowed-IPs; duplicate-section handling
- The core validates STRUCTURE only — keys are opaque values to it
  (never logged, never echoed); the Android layer owns actual key
  handling

## 6. Patch plan (registry entries added when the tree exists)

Continuing the global order after `extension/` (0013–0016) and
`offline/` (0017–0019):

| id | file | content | risk |
|---|---|---|---|
| `0020-security-app-lock` | `security/0020-security-app-lock.patch` | BiometricPrompt + Keystore envelope app lock, incognito gate, settings surface | medium |
| `0021-security-vpn-tunnel` | `security/0021-security-vpn-tunnel.patch` | VpnService tunnel module + config import (bound to the validated core) + connection-state surface + always-on/kill-switch setup flow | **high** — security review note required |

## 7. Verification strategy

1. **Core (live after Step 24):** config-validation unit tests — valid
   configs, every malformed variant, secret-free behavior.
2. **C++/Android unit tests (at build time):** envelope encryption
   round-trip through Keystore fakes; app-lock gating logic.
3. **Patch level:** `apply_patches.py apply && verify` on the pristine
   tag; repo secret-grep in CI stays green.
4. **On device:** app lock actually blocks launch and re-entry; wrong
   biometric does not decrypt; VPN with a real test endpoint passes
   traffic AND leaks none (DNS + kill-switch tests with always-on +
   block-without-VPN enabled); disconnect state is truthfully shown;
   invalid config files are rejected at import.

## 8. Honest boundaries

- No VPN, app lock, or biometric feature exists on a device until the
  patches build and pass §7 (B-001); the pure-JVM core is real and
  tested as soon as it lands.
- No iNWEB-operated VPN servers; no traffic-protection claims beyond
  what the user's own endpoint provides.
- No 2FA (§26) and no "protected bookmarks" claims (§25) until their
  backends/stores actually exist.
- No custom cryptography; no secrets in the repository — enforceable
  and enforced.
