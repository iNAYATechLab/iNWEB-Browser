# `src/core/home` — Home / New Tab Core

Pure-JVM decision and state module for iNWEB Browser's Home / New Tab surface.
It has no Android, Chromium, network, or UI-resource dependency.

## Responsibilities

- route Home search without duplicating `browser-shell` URL/search parsing;
- define the seven curated utility shortcuts (with no Add item);
- manage user-owned Quick Access and emit canonical persistence snapshots independently from browsing history;
- validate reviewed HTTPS popular-site configuration;
- model truthful loading, first-use, content, unavailable, and error card states;
- enforce the approved Home section order and optional widget visibility;
- adapt real privacy, history, bookmark, download, Top Sites, and offline state.

## Existing-core mapping

| Home state | Source |
|---|---|
| Web search / URL | `browser-shell` `OmniboxParser` + selected `SearchEngine` |
| History suggestions / recents | `HistoryStore`; ranking delegates to `TopSites.compute` |
| Bookmarks | `BookmarkStore` |
| Downloads | `DownloadsStore` |
| Privacy | `tracking-protection` `SecurityCenterModel` |
| Saved pages | `offline` `OfflineLibrary` |
| Widget visibility | Home validates wire snapshots; Android/Chromium supplies durable storage |
| Quick Access | Home returns canonical wire snapshots; durable storage never infers entries from history |

Qur'an content/search, voice recognition, reading/audio progress, daily wisdom,
prayer calculations, and curated destination lists require real providers in
the Lead-owned integration. Until then the typed unavailable/first-use states
are rendered or the relevant surface is hidden; this module contains no demo
content, sample times, fake counters, or unreviewed URLs.

## Validation

The canonical gate is:

```sh
bash scripts/validate_kotlin_core.sh
```

That script is Lead-owned and has an explicit module list. Until the Lead adds
`home` with dependencies on `browser-shell`, `tracking-protection`, and
`offline`, compile this module with the same pinned Kotlin/JUnit toolchain as a
separate pre-integration check. See the branch delivery report for the exact
command and result.
