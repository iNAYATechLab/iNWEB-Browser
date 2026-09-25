# Repository transfer runbook — user account → organization

**Decision (2026-09-25, project owner):** move `iNWEB-Browser` from the
user account `iNWEB-Management` to the organization `iNAYATechLab`,
**after the in-flight workflow runs finish** — transferring during a run
can break CI and would waste hours of build time.

## Current state (measured, not assumed)

| Item | Value |
|---|---|
| Repository | `iNWEB-Management/iNWEB-Browser` — owner type **User** ("InayaTechLab Pvt. Ltd.") |
| Target | `iNAYATechLab` — type **Organization**, plan free, token role **admin** |
| Token permissions on the repo | `admin=true`, `push=true`, `maintain=true` |
| Token scopes | `gist, read:org, repo, workflow` — **no `admin:org`, no `delete_repo`** |
| Releases | `v1.0.0-alpha.2` with one asset, ~686 MB |
| `iNAYATechLab/iNWEB-Browser` | **HTTP 301 Moved Permanently** — a redirect stub from the earlier transfer out |

## Pre-conditions — all must be true before the transfer

1. **No workflow run in progress.** Check:
   `gh run list --repo iNWEB-Management/iNWEB-Browser --limit 5`
   A hop costs 40–95 minutes plus up to ~1h of tree fetch; killing one
   mid-flight is the single most expensive mistake available here.
2. **All local work pushed** — every branch, tag and the working tree
   clean (`git status -sb`, `git log --oneline origin/main..main`).
3. **The remote URL recorded**, so the redirect can be replaced
   deliberately rather than discovered later.

## The command

```bash
gh api -X POST repos/iNWEB-Management/iNWEB-Browser/transfer \
  -f new_owner=iNAYATechLab
```

Success returns HTTP 202. If the token lacks a required scope, or the
name is unavailable, nothing changes — the call is safe to attempt and to
report honestly if refused.

## Known risk: the redirect stub

`iNAYATechLab/iNWEB-Browser` answers 301 because the repository was
moved *out* of that name earlier. GitHub may refuse to transfer into a
name that is currently a redirect ("name temporarily unavailable").

Fallbacks, in order of preference:

1. **Transfer under a new name in the org** —
   `iNAYATechLab/iNWEB-Browser-Android`. Clean, immediate, no waiting on
   anyone; the only cost is the name.
2. **Ask GitHub Support to clear the redirect**, then transfer to the
   preferred name. Slow, but keeps the canonical name.
3. **Create a fresh repository in the org and push the full history**,
   recreating the release asset. Loses stars, watchers, forks and issue
   numbering continuity — last resort only.

## What moves with the repository

Issues, pull requests and their comments, releases and their assets
(including the ~686 MB APK), tags, all branches and their history,
Actions run history, stars, watchers, forks, and branch protection that
lives on the repo.

## What does NOT move, or needs re-checking

- **Webhooks** configured at the user level do not become org hooks.
- **Deploy keys and environments** — verify after the move.
- **Actions permissions** — a free organization can run Actions on public
  repositories, but the org setting must allow it; check after transfer.
- **Repository secrets** move with the repo, but confirm by dispatching a
  run rather than assuming.

## Post-transfer checklist

1. Update the local remote:
   `git remote set-url origin https://github.com/iNAYATechLab/iNWEB-Browser.git`
2. `gh repo view iNAYATechLab/iNWEB-Browser` — confirm owner type is
   Organization and the default branch is `main`.
3. Confirm the release and its asset are intact:
   `gh release view v1.0.0-alpha.2 --repo iNAYATechLab/iNWEB-Browser`
4. Confirm Actions still run: dispatch `b001-verify` (cheap, no build)
   and watch it complete.
5. Confirm both tags (`v1.0.0-alpha.1`, `v1.0.0-alpha.2`) are present.
6. Record the transfer here with the resulting owner, time and run
   evidence.

## Rollback

Transfer back with the same endpoint and `new_owner=iNWEB-Management`.
Prefer not to bounce the repository repeatedly: every transfer leaves
another redirect behind, and redirects are what blocked the name in the
first place.

## MEASURED 2026-09-26 — the target name is RETIRED, not merely occupied

Attempt (hop-36 terminal, so the transfer window was open):

    gh api -X POST repos/iNWEB-Management/iNWEB-Browser/transfer -f new_owner=iNAYATechLab

Result — HTTP 422, hard failure:

    {"message":"Validation Failed","errors":[{"resource":"Repository",
     "code":"unprocessable","field":"data",
     "message":"Repository name iNAYATechLab/iNWEB-Browser has been retired
      and cannot be reused"}],"status":"422"}

Corroborating evidence (independent of the transfer call): pushing to
`https://github.com/iNAYATechLab/iNWEB-Browser.git` returns

    remote: This repository moved. Please use the new location:
    remote:   https://github.com/iNWEB-Management/iNWEB-Browser.git

So a repository previously lived at `iNAYATechLab/iNWEB-Browser`, was moved
out to `iNWEB-Management`, and GitHub has since **retired** the vacated name.

Consequences (measured, not speculated):
- The exact name `iNAYATechLab/iNWEB-Browser` cannot be reclaimed by transfer.
- Retrying the transfer call is pointless; it is not a transient error.
- Verified free: `GET /repos/iNAYATechLab/iNWEB-Browser-Android` -> **404**,
  so that fallback name is available today.

Open decision for the project owner (not a Lead decision):
  A. transfer as `iNAYATechLab/iNWEB-Browser-Android` (works now, name differs)
  B. ask GitHub Support to un-retire `iNAYATechLab/iNWEB-Browser` (days, may refuse)
  C. create a fresh repo in the org and push history (loses issues/PRs/releases)
