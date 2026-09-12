# iNWEB Browser — Threat Model

**Version:** 0.2 (Phase 12 re-validation — see `THREAT-MODEL-REVIEW.md` for the
evidence walk and gap register)
**Policy:** living document; re-validated at every phase gate and whenever architecture
changes (§42).

---

## 1. Assets

- User browsing data: history, cookies, site data, credentials, form data.
- iNWEB-owned data: bookmarks, settings, offline pages, download metadata.
- Sync payloads and account credentials (Phase 9).
- Backup archives (Phase 9).
- VPN credentials and tunnel state (Phase 8).
- Browser integrity: the patched engine itself.
- Device integrity and battery/data resources.

## 2. Adversaries

Malicious websites and scripts; abusive trackers and ad networks; network attackers
(open Wi-Fi, on-path MITM); malicious or compromised extensions (Phase 6); phishers and
social engineers; local device attackers (locked and unlocked scenarios); compromised
sync/backup storage; supply-chain attacks (upstream code, third-party deps, filter
lists).

## 3. Threats → planned mitigations

| Threat | Vector | Planned mitigation | Phase |
|---|---|---|---|
| Malicious site code | Renderer | Upstream site isolation + sandboxing preserved across all rebases; security gate on any patch touching process model/IPC | all |
| Phishing / deceptive sites | User deception | Upstream deceptive-site warnings class of protections (licensing noted in `LICENSING.md`); Security Center status (§24) | 3 |
| Malicious downloads | Downloads | Suspicious-download protections where supported (§12) | 4 |
| Network attacker | TLS MITM | Upstream TLS stack, HSTS/certificate transparency preserved; no convenience cert-override additions | all |
| Tracking / fingerprinting | Web requests & APIs | Privacy Engine (§10): tracker blocking, third-party storage controls, fingerprinting-resistance scope | 3–4 |
| Popup / redirect abuse | JS, navigation | Popup protection patches (§12) | 4 |
| Compromised extension | Extension APIs | Permission model, scoped API surface, kill-switch, management UI (§16) | 6 |
| Credential theft on device | Local storage | Android Keystore encryption for secrets; BiometricPrompt gating for sensitive profiles/vaults (§25, §27) | 8 |
| Sync compromise | Backend / payload | End-to-end encryption, scoped tokens, 2FA where accounts exist (§26, §29) | 9 |
| Backup leakage | Exported file | Encrypted backup format, integrity validation, no plaintext secrets (§32) | 9 |
| Profile leakage | Local data | Strict per-profile isolation (§28) | 9 |
| Supply chain (upstream/deps) | Rebase, dependencies | Patch-series review at every rebase; CI reproducibility hash; no untracked edits; SBOM at release | all |
| Data loss from crashes/corruption | Local state | Crash-safe state handling, session recovery, database recovery, migration rollback (§50–§51); every persisted surface carries a CI-audited corruption contract (`STORAGE-INVENTORY.yaml`, ADR-033) | 2+ |
| Notification abuse (impersonation, promotional pressure) | Notification surface | Real-events-only policy with no generic notify path; promotional/sync/update events have no code path; per-channel toggles; lazily-requested permission (ADR-028/030) | 11 |

## 4. Residual risks (current, Phase 12 re-validation)

- **B-001:** no built binary exists yet; therefore zero runtime security claims are made.
  First reproducible build on real infrastructure is the earliest point any binary-level
  security statement can be validated.
- **Extensions on Android:** scope DECIDED in the Phase 6 design (upstream
  WebExtensions enabled on Android, sideload-first, ADR-023); enforcement surface
  ships with patches 0013–0016.
- **VPN / Sync:** no protection exists until real infrastructure exists; UI will not
  claim otherwise (§57, §65).
- **Source-level contracts:** all corruption/recovery contracts are CI-audited at the
  source level; on-device verification of each is a B-001 checklist item
  (gap G-09 in the review).

## 5. Review cadence

Re-validated at each phase gate. Every architecture decision that touches a row above
must reference it in the patch registry entry (see `PHASE0-CHROMIUM-BASELINE.md` §4,
rule 5).
