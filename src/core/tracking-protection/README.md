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

## Performance note

v1 compiles one regex per rule — correct and adequate for validation and
moderate lists. Production-scale lists (tens of thousands of rules) will get
an optimized combined matcher during the Phase 4/5 performance pass; the
public API (`decide`) stays unchanged.
