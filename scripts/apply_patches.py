#!/usr/bin/env python3
"""iNWEB Browser — tracked patch-series tool (apply / verify / hash).

Converges a Chromium source tree to the deterministic state:

    pristine upstream @ pinned tag + registered patch series (in order)

Commands:
    apply  <tree>   converge the tree to the fully-applied series state
    verify <tree>   exit 0 iff the full series was already applied
                    (converges first — content-neutral on success)
    hash   <tree>   print the deterministic content digest of a tree
                    (excludes VCS metadata such as .git)

Convergence algorithm (`apply`):
    1. PEEL   — scan the series from last to first; every patch whose
                reverse applies cleanly is reversed out of the tree and
                recorded as "previously applied". This makes the operation
                idempotent and resumable after partial application.
    2. FORWARD— apply the entire series in registry order; abort on the
                first patch that does not apply cleanly (conflict).

Exit codes: 0 = success, 1 = conflict / missing patch / verify failure,
2 = usage or tool error.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import subprocess
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    print("error: PyYAML is required (pip install pyyaml)", file=sys.stderr)
    sys.exit(2)

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_MANIFEST = os.path.join(REPO_ROOT, "iNWEB_PATCHES", "MANIFEST.yaml")


def load_manifest(path: str) -> list[dict]:
    """Load and structurally validate the patch registry."""
    with open(path, "r", encoding="utf-8") as fh:
        data = yaml.safe_load(fh) or {}
    if data.get("schema_version") != 1:
        raise ValueError(f"unsupported manifest schema_version: {data.get('schema_version')!r}")
    patches = data.get("patches")
    if patches is None:
        patches = []
    if not isinstance(patches, list):
        raise ValueError("manifest 'patches' must be a list")
    for entry in patches:
        for field in ("id", "file"):
            if not entry.get(field):
                raise ValueError(f"patch entry missing required field {field!r}: {entry!r}")
    return patches


def _patch_path(manifest_path: str, entry: dict) -> str:
    return os.path.join(os.path.dirname(os.path.abspath(manifest_path)), entry["file"])


def _git(args: list, cwd: str) -> int:
    return subprocess.run(
        ["git", *args], cwd=cwd,
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    ).returncode


def apply_pending(tree: str, manifest_path: str) -> tuple[list[str], dict | None]:
    """Converge the tree to the fully-applied state of the registered series.

    Returns (was_applied_ids, failed_entry). failed_entry is None on success.
    was_applied_ids lists the patches that were already applied before the
    call (they are peeled and re-applied to reach the converged state).
    """
    entries = load_manifest(manifest_path)
    for entry in entries:
        if not os.path.isfile(_patch_path(manifest_path, entry)):
            return [], entry
    if not entries:
        return [], None

    # Phase 1 — PEEL (last to first; skip non-reversible, peel reversible).
    was_applied: list[str] = []
    for entry in reversed(entries):
        patch_file = _patch_path(manifest_path, entry)
        if _git(["apply", "--check", "--reverse", patch_file], tree) == 0:
            if _git(["apply", "--reverse", patch_file], tree) != 0:
                return was_applied, entry
            was_applied.append(entry["id"])
    was_applied.reverse()

    # Phase 2 — FORWARD (registry order; abort on first conflict).
    for entry in entries:
        patch_file = _patch_path(manifest_path, entry)
        if _git(["apply", "--check", patch_file], tree) != 0:
            return was_applied, entry
        if _git(["apply", patch_file], tree) != 0:
            return was_applied, entry
    return was_applied, None


def verify_series(tree: str, manifest_path: str) -> bool:
    """True iff the full series was already applied on the tree.

    Note: verification converges the tree first (peel + re-apply of the same
    series), which is content-neutral on success.
    """
    entries = load_manifest(manifest_path)
    was_applied, failed = apply_pending(tree, manifest_path)
    return failed is None and len(was_applied) == len(entries)


def hash_tree(tree: str) -> str:
    """Deterministic content digest over all files, excluding .git metadata.

    Symlinks contribute their LINK TARGET TEXT ("link:<target>"), never the
    referenced content: the digest does not follow links and therefore cannot
    fail on the dangling ones the Android NDK ships (libc++.so et al.), and
    a re-pointed link changes the hash exactly like an edit would.
    """
    digest = hashlib.sha256()
    for root, dirs, files in os.walk(tree):
        dirs[:] = sorted(d for d in dirs if d != ".git")
        for name in sorted(files):
            path = os.path.join(root, name)
            rel = os.path.relpath(path, tree).replace(os.sep, "/")
            if os.path.islink(path):
                content = ("link:" + os.readlink(path)).encode("utf-8")
            else:
                with open(path, "rb") as fh:
                    content = fh.read()
            digest.update(rel.encode("utf-8"))
            digest.update(b"\0")
            digest.update(hashlib.sha256(content).hexdigest().encode("ascii"))
            digest.update(b"\0")
    return digest.hexdigest()


def main(argv: list | None = None) -> int:
    parser = argparse.ArgumentParser(description="iNWEB tracked patch-series tool")
    sub = parser.add_subparsers(dest="command", required=True)

    for name in ("apply", "verify"):
        p = sub.add_parser(name, help=f"{name} the registered patch series on a tree")
        p.add_argument("tree", help="path to the Chromium source tree")
        p.add_argument("--manifest", default=DEFAULT_MANIFEST,
                       help="path to MANIFEST.yaml (default: iNWEB_PATCHES/MANIFEST.yaml)")

    p = sub.add_parser("hash", help="print the deterministic content hash of a tree")
    p.add_argument("tree", help="path to the tree")

    args = parser.parse_args(argv)
    tree = os.path.abspath(args.tree)
    if not os.path.isdir(tree):
        print(f"error: tree directory not found: {tree}", file=sys.stderr)
        return 2

    try:
        if args.command == "hash":
            print(hash_tree(tree))
            return 0

        if args.command == "apply":
            was_applied, failed = apply_pending(tree, args.manifest)
            if failed is not None:
                print(
                    f"CONFLICT  {failed.get('id')} ({failed.get('file')}) — manual rebase required",
                    file=sys.stderr,
                )
                return 1
            total = len(load_manifest(args.manifest))
            print(f"series OK ({len(was_applied)} already applied, "
                  f"{total - len(was_applied)} applied now)")
            return 0

        if args.command == "verify":
            if verify_series(tree, args.manifest):
                print("verify OK: full series applied")
                return 0
            print("verify FAILED: series not fully applied", file=sys.stderr)
            return 1
    except (OSError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
