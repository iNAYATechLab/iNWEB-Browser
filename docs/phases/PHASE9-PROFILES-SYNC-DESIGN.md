# Phase 9 — Profiles, Sync & Backup Design

**Status:** authored design (Step 25). Code lands as pure-JVM cores
(CI-tested) plus `ui/`/`settings/` patch entries generated against the
pinned baseline `154.0.8037.21` on build infrastructure (blocker B-001).
**Governing requirements:** MASTER-SPEC §28 (profiles), §29 (cloud sync),
§30 (offline synchronization), §31 (database), §32 (backup/restore).

Honesty anchors: §29 *"never pretend cloud sync exists without real
infrastructure"*; §32 *"no plaintext secrets"*; §31 *"do not
unnecessarily duplicate existing Chromium storage systems."*

---

## 1. §28 Profiles — real isolation, not cosmetic names

- **Model:** first-class profiles with per-profile namespaces for ALL
  iNWEB-owned data: bookmarks, history, downloads, saved sessions, top
  sites, settings, privacy preferences (allowlists), offline pages,
  extension registry state. Each store instance is created against a
  profile-scoped directory — the existing pure-JVM stores already accept
  injected storage, so isolation is enforced at construction (ADR pattern
  of seams).
- **Chromium-owned data** (cookies, HTTP cache, credentials): per-profile
  user-data directories at the Android layer — the patch wires the
  profile id into the browser-process data-dir selection. Cookies are
  NEVER shared across profiles; incognito remains the existing
  in-memory model (Phase 3).
- **Switching:** profile switcher in settings; active profile is a real
  persisted root setting; switching closes the browsing surface and
  reloads every store against the new namespace. A "profile" that shares
  storage with another is a §28 violation and cannot occur here.
- **Pure-JVM core (Step 26):** `src/core/profiles` — profile records
  (id, name, createdAt), active-profile selection, per-profile store
  routing (a store-factory binding each profile to its namespace),
  rename, delete (with the namespace retired), all persisted via a seam.

## 2. §29 Cloud sync — documented absence

There is **no sync backend** (none operated, none bundled). Therefore
iNWEB v1 ships **no cloud sync and no sync UI** — §29 forbids pretending
otherwise. The moment a backend is seriously considered, its design must
cover: account model, 2FA (§26 — TOTP per the Phase 8 design), transport
security, privacy implications under §41 (no silent telemetry), data
retention, and the §30 queue/conflict model below. Until then, every
sync-shaped surface stays absent, not stubbed.

## 3. §30 Offline synchronization — the backend-agnostic core, designed now

The §30 requirements (queued changes, retry, conflict resolution,
integrity, secure transport, failure recovery) are designed as a
**transport-agnostic queue model** so that no backend dependency leaks
into the data model:

- Change log per data type (bookmarks/history/settings/tabs): append-only
  operations with monotonic revision counters per store
- Retry with backoff; failure recovery = replay of the durable queue
- Conflict resolution: last-writer-wins per item with tombstones for
  deletes (recorded, auditable) — the simplest truthful model; CRDTs are
  NOT claimed
- Integrity: per-batch checksums (platform SHA-256 at the Android layer)
- This model is exercised by pure-JVM tests against a fake transport —
  real sync ships only when a real backend exists (§2)

Scope honesty: the queue core is infrastructure for a future backend —
it is not announced as a feature anywhere in the UI.

## 4. §31 Database — one rule, already followed

App-owned structured data (bookmarks, history, downloads, profiles,
offline-page metadata, extension registry, sync queue) uses SQLite at the
Android layer (versioned schema, migrations) behind the existing store
seams. **Chromium-owned data stays in Chromium's storage** — no shadow
copies of cookies/cache/passwords (§31). The pure-JVM cores keep their
file/in-memory seams so CI can test logic without a device.

## 5. §32 Backup & restore

- **Format:** a versioned bundle (`iNWEB-BACKUP v=1`) containing
  per-store exports (the same formats the file stores already write) +
  manifest (format version, app version, profile ids, creation time).
- **Encryption:** optional-but-default ON at the Android layer using the
  ADR-025 Keystore envelope model — the BACKUP FILE carries only
  ciphertext; passphrase-derived key unwraps via Keystore. **No
  plaintext secrets in the bundle, ever** (§32); keys themselves are
  never exported.
- **Integrity:** per-entry SHA-256 checksums (platform primitive) +
  bundle-level checksum; restore validates before importing anything.
- **Version compatibility & migration:** the manifest's format version
  gates restore; forward migration only, with explicit refusal of newer
  formats on older builds (no guessing).
- **Restore preview:** the restore flow shows what the bundle contains
  (counts per store, profile names) BEFORE anything is imported; the
  user confirms per-profile import.
- **Corruption handling:** corrupt entries are skipped and REPORTED
  (list), never silently imported; a corrupt manifest aborts the whole
  restore.
- **Pure-JVM core (Step 27):** `src/core/backup` — bundle assembly,
  manifest, checksums, version gating, per-entry corruption tolerance,
  preview model; encryption stays at the Android layer (no crypto in
  cores — ADR-025).

## 6. Patch plan (registry entries added when the tree exists)

Continuing the global order after `security/` (0021–0022):

| id | file | content | risk |
|---|---|---|---|
| `0023-ui-multi-profile` | `ui/0023-ui-multi-profile.patch` | profile switcher UI, per-profile data dirs at the browser-process level, store-namespace binding, incognito unchanged | high (isolation review required) |
| `0024-settings-backup-restore` | `settings/0024-settings-backup-restore.patch` | backup/restore UI, Keystore-envelope encryption of the bundle, restore preview flow | medium |

No sync patch exists — there is nothing to patch (§2).

## 7. Verification strategy

1. **Core (live after Steps 26–27):** profiles + backup unit tests —
   isolation (no store can see another namespace), bundle round-trips,
   checksums, version gates, corruption tolerance.
2. **C++/Android unit tests (at build time):** data-dir selection per
   profile; envelope encryption round-trip.
3. **Patch level:** `apply_patches.py apply && verify` on the pristine
   tag.
4. **On device:** two profiles keep separate bookmarks/history/cookies;
   deleting a profile removes its namespace; an encrypted backup restores
   on a clean install with preview; a tampered bundle is refused; an
   old-format bundle migrates forward; a newer-format bundle is refused
   with a clear message.

## 8. Honest boundaries

- No cloud sync exists or is claimed (§29); the queue model is future
  infrastructure, not a feature.
- Profiles and backup/restore work on a device only after the patches
  build and pass §7 (B-001); the pure-JVM cores are real and tested as
  soon as they land.
- Encryption happens at the Android layer with platform primitives;
  cores contain no cryptography and no secrets.
- Conflict resolution is last-writer-wins with tombstones — stated
  plainly, no CRDT claims.
