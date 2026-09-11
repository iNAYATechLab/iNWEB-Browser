# iNWEB_PATCHES — Tracked Chromium Patch Series

This directory is the **single authority** for every iNWEB modification to upstream
Chromium (Master Specification §5). No untracked modification may ever exist in a
Chromium working tree; the tree must always be reproducible as:

```text
pristine upstream Chromium @ pinned tag (build/config/inweb/BASELINE)
        + this ordered, registered patch series
```

## Layout

| Directory | Scope |
|---|---|
| `privacy/` | tracking protection, storage/cookie controls, referrer policy, fingerprinting resistance |
| `security/` | hardening, secure defaults, abuse protections |
| `adblock/` | engine-level request filtering, cosmetic filtering hooks |
| `popup_protection/` | popup blocking, redirect-abuse protection |
| `extension/` | WebExtensions-on-Android scope (Phase 6) |
| `performance/` | startup / memory / network / battery optimizations |
| `ui/` | iNWEB Android application layer integration |
| `offline/` | offline pages / reader-mode plumbing |
| `settings/` | iNWEB settings model integration |
| `tests/` | validation executed after patch application (per-patch tests) |

## Registry

`MANIFEST.yaml` is the registry. Every patch must be registered there with its
metadata (id, file, area, description, upstream files, risk, validation, rebase
notes). `python3 scripts/lint_manifest.py` validates the registry; CI runs it on
every change.

## Naming

`NNNN-<area>-<slug>.patch` inside the area directory, where `NNNN` is the global
application order (zero-padded 4 digits). The registry `id` equals the file name
without the `.patch` extension.

## Patch lifecycle

1. **Author** — develop the change inside a Chromium checkout at the pinned tag,
   in a git working tree.
2. **Export** — `git diff > iNWEB_PATCHES/<area>/NNNN-<slug>.patch`
   (export only the intended change; keep patches small and orthogonal).
3. **Register** — add the entry to `MANIFEST.yaml` (keep order).
4. **Validate** — apply the full series on a pristine pinned checkout:
   `python3 scripts/apply_patches.py apply <chromium-src>`,
   then run the patch's validation and `verify`.
5. **Review** — patches touching process model, sandboxing, IPC, or permissions
   (risk `high`) require a security review note before entering the series.

## Tooling

```bash
# Validate the registry
python3 scripts/lint_manifest.py

# Converge a Chromium tree to the fully-applied series state
# (peels cleanly-applied patches and re-applies — idempotent and resumable;
#  aborts with a CONFLICT report on the first non-applicable patch)
python3 scripts/apply_patches.py apply /path/to/chromium/src

# Verify the full series was already applied (converges first; content-neutral)
python3 scripts/apply_patches.py verify /path/to/chromium/src

# Deterministic content hash of a tree (reproducibility invariant)
python3 scripts/apply_patches.py hash /path/to/chromium/src
```

## Rebase discipline

At every upstream rebase (`docs/PHASE0-CHROMIUM-BASELINE.md` §3), each patch is
recorded in the rebase report as: `clean` | `conflict-resolved (how)` |
`reworked (why)` | `dropped (why)`.
