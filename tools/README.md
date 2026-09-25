# tools/ — workspace helpers (not product code)

Nothing here ships, is built, or is part of any gate. These two scripts
live outside `scripts/` because `scripts/` holds project tooling that CI
and the build depend on, while these support the agent workspace that
produces the patches — and would otherwise exist only on one machine.

| Script | What it is for |
|---|---|
| `bootstrap-agent-env.sh` | Prepares the sandbox: puts a pinned `gh` on `PATH` and configures the git identity used for commits. Environment setup, nothing else. |
| `build-e2e-fixture-tree.sh` | Builds the small pristine pinned-tree fixture set that `scripts/apply_patches.py` is validated against locally. Resumable (non-empty files are skipped) and parallel (6 fetches), so a partial run can just be re-run — it was written that way after a serial version timed out at 15 minutes. |

If the workspace is rebuilt, these are what makes it reproducible
without rediscovering the setup.
