# iNWEB Browser — Store-Readiness Data-Safety Draft (§41/§53)

**Status:** Phase 12 design-order item 5b (Step 42). DRAFT — the
working input for the Play Console "Data safety" declaration and the
privacy-policy content, filed for real with the shipped app (device
matrix row D12-5).

**Source authority:** every storage row below is derived from
`docs/STORAGE-INVENTORY.yaml` (23 surfaces: 14 seams + 9 adapters —
CI-enforced, ADR-033). If this draft and the inventory disagree, **the
inventory wins** and this draft is corrected (§59). The draft is
regenerated/reviewed whenever the inventory changes.

---

## 1. Headline answers (the declaration itself)

| Play "Data safety" question | iNWEB answer | Why (evidence) |
|---|---|---|
| Does your app collect or share any user data? | **No** | No telemetry, no analytics, no crash-reporting upload, no developer-operated servers (§41 "do not add hidden analytics"; ADR-026 sync = documented absence; ADR-025 VPN = bring-your-own-server) |
| Data collected (any type) | **None** | Every store in the inventory is LOCAL on-device data (§1 of the inventory); nothing is transmitted to iNWEB or a third party by the browser itself |
| Data shared with third parties | **None** | Filter lists are DOWNLOADED from public list sources (read-only fetch); browsing data never leaves the device (§29: no sync backend exists) |
| Encryption in transit / at rest (collected data) | Not applicable | No collected data; §1 answers stand on their own |
| Can users request data deletion? | Browsing data is deletable IN the app | §2 below: clear-browsing-data (5 items), per-entry deletions, app reset; uninstall removes everything (Android sandbox) |

## 2. What is stored on the device, and what clears it

All locations are inside the app's private sandbox (`filesDir/` or
app preferences) unless stated otherwise. "Leaves the device" is
**never** for any row below.

| Data | Where | Clears via | Leaves the device? |
|---|---|---|---|
| App settings (search engine, theme, onboarding flag) | app preferences `inweb_settings` | app reset only — settings are not browsing data | never |
| Toolbar configuration | app preferences `inweb_toolbar` | app reset | never |
| Notification per-channel toggles | app preferences `inweb_notifications` | app reset | never |
| Page-zoom default + per-site overrides | app preferences `inweb_zoom` | per-site overrides: clear-browsing-data (SITE_ZOOM_OVERRIDES); default factor: app reset | never |
| Download preferences (ask flag + folder) | app preferences `inweb_downloads` | app reset | never |
| Browsing history (private-mode excluded upstream) | `filesDir/history.tsv` | clear-browsing-data (HISTORY); per-entry deletion; range deletes in core | never |
| Bookmarks (explicit user action only) | `filesDir/bookmarks.tsv` | per-entry deletion; app reset (deliberate user data — NOT in clear-browsing-data, ADR-034) | never |
| Session snapshot (open tabs, crash-safe) | `filesDir/session.inweb` | clear-browsing-data (SESSION/TABS) | never |
| Downloaded filter lists (disposable cache) | `filesDir/filter-lists/` | clear-browsing-data (FILTER_LIST_CACHE) — re-downloads by design | downloaded from public list sources (read-only) |
| Offline library entries | in-memory until the offline patches (0017–0019) persist it | per-entry deletion + clear-browsing-data (OFFLINE_PAGES) — snapshots are user data, clear-cache never removes them (ADR-024) | never |
| Downloads catalog | in-memory until the downloads patch persists it | clear-browsing-data once persisted; app reset | never |
| Extension registry state | in-memory until the extension patches (0013–0016) persist it | extension removal; app reset | never |
| Profiles (registry + active id) | in-memory until patch 0022 binds per-profile directories | profile deletion (namespace retired); app reset | never |
| Sync queue | in-memory; **no sync backend exists** (§29 documented absence) | not applicable — no sync data exists | never |
| Backup bundles | exported files at a USER-CHOSEN location, encrypted at the Android layer (patch-pending) | user deletes the exported file; in-app restore preview gated | only by the user's own export action, encrypted |

## 3. Deletion story (what a user can delete, and how)

1. **In app:** clear-browsing-data surface — history, open tabs and
   session, downloaded filter lists (re-download), offline pages,
   per-site zoom overrides — each item optional, with real preview
   counts (ADR-034); per-entry history deletion, bookmark deletion,
   offline-entry deletion.
2. **App reset:** clears every preference store and resets
   customization (§2 rows).
3. **Uninstall:** removes the entire private sandbox — all files and
   preferences — leaving nothing behind on the device (standard
   Android behavior; no device-external copies exist).

## 4. Security practices (local data)

- All local data lives in the app's private storage (Android sandbox);
  no world-readable locations are used by any inventory adapter.
- VPN configuration secrets are opaque by construction (`SecretValue`,
  ADR-025); backups are integrity-checked and the plaintext-secret
  guard refuses known secret stores (ADR-026); backup archive
  encryption happens at the Android layer with platform primitives —
  **pending its patch** and stated as pending here.
- No custom cryptography (ADR-025); no secrets in the repository
  (CI-enforced secret grep).

## 5. Honest boundaries

- This draft is **source-level**: it describes the authored code and
  the CI-enforced inventory, not a built APK. The final Play
  declaration is made after the built app is audited (device-matrix
  D12-3: permissions, network egress, storage surfaces) and re-checked
  against D12-5.
- Rows whose persistence ships with future patches (offline library,
  downloads catalog, extension state, per-profile directories, backup
  encryption) are stated as **pending** — the declaration is updated
  when they land, never before.
- The VPN rows describe user-provided infrastructure: traffic goes to
  the user's own chosen endpoint; iNWEB operates no servers and shares
  nothing (ADR-025).
- §34 entertainment module is a non-goal; §33 promotional
  notifications never exist — neither adds a data flow.
