# iNWEB core — browser-shell (pure JVM)

The engine-independent heart of the browser application layer: tabs,
omnibox input handling, sessions, settings, history, bookmarks, downloads,
and top-site computation. Everything here runs and is tested without the
Android SDK (ADR-009) — the same sources are compiled into the product by
the Chromium build via the `ui/` patch series.

## Components

| Component | Purpose |
|---|---|
| `TabState` / `TabsController` | Immutable tab state with real navigation-stack semantics; controller owns the tab set, selection, and crash-safe session restore (`allTabs()` feeds the tab switcher and the engine adapter) |
| `OmniboxParser` / `SearchEngine` | URL-vs-search classification (incl. Bengali queries and scheme edge cases) and `%s`-template search engines; DuckDuckGo default (ADR-010) |
| `SessionStore` / `SessionManager` | Session snapshot/restore with corruption fallback to a fresh session (§51) |
| `Settings` (`AppSettings`, `SettingsStore`) | Settings model that only grows with real backing behavior (§10); includes the persisted onboarding flag (§35) |
| `HistoryStore` / `InMemoryHistoryStore` / `FileHistoryStore` | History port + persistent TSV store (write-through atomic, corruption-tolerant); `PrivacyFilterHistory` is the single privacy enforcement point (§13/§14, ADR-011/ADR-015) |
| `BookmarkStore` / `InMemoryBookmarkStore` / `FileBookmarkStore` | Bookmarks port + persistent store; unique URLs, folders-lite, explicit user action only (ADR-016) |
| `TopSites` | Home "shortcuts" computed from real history visits — never fabricated (ADR-017) |
| `Downloads` (`DownloadRecord`) / `DownloadsStore` | Download state machine (QUEUED→RUNNING→{PAUSED, COMPLETED, FAILED, CANCELLED}) + catalog |
| `BrowserEngine` | The engine port the Chromium adapter implements (`ui/` patch 0002) |

## Testing

115 unit tests run by `bash scripts/validate_kotlin_core.sh` (pinned
kotlinc 2.4.20 + JUnit 4.13.2) in the authoring sandbox and in CI.

## Honesty scope (§57)

This module is real logic with real persistence — but no page rendering,
page titles, or network stack exist until the engine patches land (B-001).
History records omnibox navigations with the URL as the title until the
adapter refines recording to page-load events (see
`docs/PHASE2-INTEGRATION-PLAN.md` §3).
