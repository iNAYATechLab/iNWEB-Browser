# iNWEB core — tracking-protection (pure JVM)

The engine-independent heart of the **Privacy Engine** (MASTER-SPEC §10) and
the **Ad Blocker** (§11): filter-list parsing, network-request matching,
per-site allowlisting, and real decision statistics.

## Components

| Component | Purpose |
|---|---|
| `FilterListParser` | Parses EasyList-family filter lists (subset below) |
| `NetworkFilterRule` / `FilterOptions` | Parsed rule + option constraints |
| `PatternCompiler` | Pattern → regex translation (`||`, `|`, `*`, `^`) |
| `RuleMatcher` | Option-aware matching of a rule against a request |
| `TrackingProtectionEngine` | Decision order: disable → site allowlist → exception → block → pass |
| `EngineStatistics` | Real block/allow/pass counters + per-domain blocks (feeds the Security Center, §24) |
| `DomainClassifier` | Simplified registrable-domain classification (built-in multi-part suffix table; replaced by Chromium's full PSL at engine integration) |
| `TrackingProtectionSettings` / `CookiePolicy` | Policy model consumed by the engine patches (not yet user-facing UI — §57) |

### Filter-list management layer (`lists` subpackage, Step 6)

| Component | Purpose |
|---|---|
| `FilterListSource` | Subscribable list metadata (EasyList + EasyPrivacy defaults); ids restricted to `[a-z0-9-]` |
| `FilterListFetcher` / `HttpFilterListFetcher` | Download port + real `HttpURLConnection` implementation: timeouts, conditional revalidation (`If-None-Match` / `If-Modified-Since` → 304), hard response-size cap |
| `FileFilterListCache` / `FilterListCache` | Atomic (temp + rename) file cache — `<id>.txt` body + `<id>.meta` metadata; corrupt metadata degrades to "missing", never crashes |
| `FilterListVersion` | `! Version:` / `! Last modified:` header extraction (first occurrence, never invented) |
| `UpdatePolicy` | Refresh-due decision (interval, startup fetch, enabled); no timers of its own |
| `FilterListManager` | Lifecycle orchestration: startup (cache first), conditional refresh, graceful cached fallback on failure; per-source `ListUpdateStatus` report |
| `SecurityCenter` / `SecurityCenterModel` | §24 dashboard contract built strictly from real decision statistics, real policy inputs, and real filter-list state (top blocked domains, per-list version/rules); `enforcementActive` stays false until the engine patches wire `decide()` (B-001) |

## Supported syntax subset

| Syntax | Meaning |
|---|---|
| `! ...`, `[Adblock Plus 2.0]` | comment / header (counted) |
| `##`, `#@#`, `#?#` rules | cosmetic rules — recognized and counted; cosmetic filtering ships in a later phase |
| `@@rule` | exception rule (allow) |
| `||domain^path` | domain anchor + separator |
| `\|start`, `end\|` | left / right URL anchors |
| `*`, `^` | wildcard; separator (any non `[a-zA-Z0-9_.%-]` char or end of URL) |
| `$script,image,...` | positive resource-type constraints |
| `$~script` | negated type constraints |
| `$third-party`, `$first-party` (and `~` forms) | party constraints |
| `$domain=a.com\|~b.com` | document-domain includes/excludes |
| `$match-case` | accepted and ignored (matching is case-insensitive) |

**Unknown options** (e.g. `csp=`, `rewrite=`, `removeparam=`) mark the rule
*unsupported*: it is excluded from matching and counted in
`unsupportedRuleCount` — never silently treated as matching.

## Honesty scope (§57)

This module makes **decisions**; no request is actually blocked until the
Chromium `adblock/` / `privacy/` patches wire `decide()` into the network
stack on build infrastructure (B-001). Statistics count real decisions made
by this engine only.

The management layer is transport-real (HTTP round-trips, conditional
requests, atomic disk cache) but runs no background scheduler: `UpdatePolicy`
decides *when a refresh is due*; the host layer (Android app or engine patch)
owns the timer and invokes `startup()` / `refresh()`. No list is downloaded
on a device until that wiring exists.

## Performance note

v1 compiles one regex per rule — correct and adequate for validation and
moderate lists. Production-scale lists (tens of thousands of rules) will get
an optimized combined matcher during the Phase 4/5 performance pass; the
public API (`decide`) stays unchanged.
