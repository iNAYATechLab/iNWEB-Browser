# `src/core/backup` — Backup Bundle Core (pure JVM)

The versioned, checksummed backup bundle behind MASTER-SPEC §32
(Phase 9 design: `docs/PHASE9-PROFILES-SYNC-DESIGN.md` §5).

**Pure JVM, no Android dependency** — compiles and tests with
`bash scripts/validate_kotlin_core.sh` (pinned kotlinc + JUnit); the
`settings/` patch (0023) binds it to the backup/restore UI and the
Keystore-envelope encryption of the on-disk file (build infrastructure,
B-001).

## Components

| Component | Purpose |
|---|---|
| `BackupBundle.build / parse / serialize` | `iNWEB-BACKUP v=1` line format: header + manifest (`format`, `app-version`, `created-at`, `entries`) + per-store entries with SHA-256 payload checksums (platform primitive — no custom crypto, ADR-025) |
| `BackupPreview` | What restore shows BEFORE importing anything: counts per profile and store, plus warnings |

## §32 rules enforced in code (ADR-026)

- **No plaintext secrets:** only `KNOWN_STORES` (iNWEB-owned store
  exports) may be bundled — unknown store names are rejected at build
  time and skipped at parse time; credentials/VPN keys can never enter
  a bundle through this core.
- **Version compatibility:** this build reads exactly v1; NEWER formats
  are refused with an explicit upgrade message (no guessing); older
  formats migrate forward only via explicit migration functions (none
  exist yet — nothing is older than v1).
- **Integrity:** per-entry SHA-256; a corrupt entry is SKIPPED and
  reported; a corrupt manifest aborts the whole restore.
- **Corruption handling:** warnings (never silent) for skipped entries,
  count mismatches, unknown lines, missing end marker.
- Encryption of the serialized bundle happens at the Android layer
  (Keystore envelope) — the core handles structure and integrity only.
