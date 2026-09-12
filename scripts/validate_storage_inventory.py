#!/usr/bin/env python3
"""Validate the storage inventory (Phase 12 hardening; MASTER-SPEC §50/§51).

"Never silently lose user data" (§50) and §51's corrupted-data detection
are enforced structurally: EVERY persistence surface in the codebase must
have a complete, honest entry in docs/STORAGE-INVENTORY.yaml — what it
stores, where it lives, what happens on corruption, and what "clear"
means for it.

Discovery (source-level, mechanical):
  * interface declarations whose name ends in Store / Persistence / Cache
  * class declarations named File* / SharedPreferences* (test sources and
    *Test classes excluded; InMemory* doubles are covered by their seam's
    entry)
  * getSharedPreferences("...") preference names in the app layer

Checks:
  1. catalog well-formedness: required fields non-empty, unique surfaces,
     kind in {seam, adapter}, adapters declare `implements` (a known
     seam), SharedPreferences* adapters declare `prefs_name`
  2. every discovered surface has a catalog entry
  3. every discoverable catalog entry is discovered (no stale rows);
     `discoverable: false` is the reviewed manual-extra hatch
  4. discovered preference names match the catalog's prefs_name set
     (bidirectional)

This is a source-level gate: it proves the inventory covers the code as
written; runtime clear/corruption behavior on a device is verified at
B-001 per the Phase 12 plan.

Exit codes: 0 = valid, 1 = violations found, 2 = tool error.
"""
from __future__ import annotations

import argparse
import os
import re
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    print("validate_storage_inventory: pyyaml is required", file=sys.stderr)
    sys.exit(2)

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_SRC = os.path.join(REPO_ROOT, "src")
DEFAULT_CATALOG = os.path.join(REPO_ROOT, "docs", "STORAGE-INVENTORY.yaml")

REQUIRED_FIELDS = ("surface", "kind", "module", "stores", "location", "corruption", "clear")
KINDS = ("seam", "adapter")
SEAM_SUFFIXES = ("Store", "Persistence", "Cache")
ADAPTER_PREFIXES = ("File", "SharedPreferences")

INTERFACE_RE = re.compile(
    r"^\s*(?:public\s+|internal\s+|private\s+)?interface\s+([A-Za-z0-9_]+)", re.MULTILINE
)
CLASS_RE = re.compile(
    r"^\s*(?:public\s+|internal\s+|private\s+)?(?:final\s+)?class\s+([A-Za-z0-9_]+)", re.MULTILINE
)
PREFS_RE = re.compile(r"getSharedPreferences\(\s*\"([A-Za-z0-9_.]+)\"")


def _is_test_path(path: str) -> bool:
    parts = os.path.normpath(path).split(os.sep)
    return "test" in parts


def discover_surfaces(src_dir: str) -> tuple[set[str], set[str], set[str]]:
    """Return (interfaces, adapter classes, prefs names) under src_dir."""
    interfaces: set[str] = set()
    adapters: set[str] = set()
    prefs: set[str] = set()
    if not os.path.isdir(src_dir):
        return interfaces, adapters, prefs
    for root, _, names in os.walk(src_dir):
        for name in sorted(names):
            if not name.endswith(".kt"):
                continue
            path = os.path.join(root, name)
            if _is_test_path(path):
                continue
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            for match in INTERFACE_RE.finditer(text):
                candidate = match.group(1)
                if candidate.endswith(SEAM_SUFFIXES):
                    interfaces.add(candidate)
            for match in CLASS_RE.finditer(text):
                candidate = match.group(1)
                if candidate.endswith("Test"):
                    continue
                if candidate.startswith(ADAPTER_PREFIXES):
                    adapters.add(candidate)
            for match in PREFS_RE.finditer(text):
                prefs.add(match.group(1))
    return interfaces, adapters, prefs


def check_catalog(catalog_path: str) -> tuple[list[dict], list[str]]:
    """Load + structurally validate the catalog; return (entries, errors)."""
    errors: list[str] = []
    if not os.path.isfile(catalog_path):
        return [], [f"catalog not found: {catalog_path}"]
    with open(catalog_path, encoding="utf-8") as handle:
        try:
            data = yaml.safe_load(handle)
        except yaml.YAMLError as exc:
            return [], [f"catalog is not valid YAML: {exc}"]
    if not isinstance(data, dict) or not isinstance(data.get("surfaces"), list):
        return [], ["catalog must be a mapping with a 'surfaces' list"]

    entries: list[dict] = []
    seen: set[str] = set()
    for index, entry in enumerate(data["surfaces"]):
        label = f"surfaces[{index}]"
        if not isinstance(entry, dict):
            errors.append(f"{label}: entry must be a mapping")
            continue
        surface = str(entry.get("surface") or "").strip()
        if not surface:
            errors.append(f"{label}: 'surface' is required and must be non-empty")
            continue
        label = f"surfaces[{surface}]"
        if surface in seen:
            errors.append(f"{label}: duplicate surface")
        seen.add(surface)
        for field in REQUIRED_FIELDS:
            value = entry.get(field)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"{label}: '{field}' is required and must be non-empty")
        kind = entry.get("kind")
        if kind not in KINDS:
            errors.append(f"{label}: 'kind' must be one of {KINDS}, got {kind!r}")
        if kind == "adapter":
            implements = entry.get("implements")
            if not isinstance(implements, str) or not implements.strip():
                errors.append(f"{label}: adapters must declare 'implements' (the seam)")
        if surface.startswith("SharedPreferences"):
            if not isinstance(entry.get("prefs_name"), str) or not entry["prefs_name"].strip():
                errors.append(f"{label}: SharedPreferences* adapters must declare 'prefs_name'")
        entries.append(entry)
    return entries, errors


def validate(src_dir: str, catalog_path: str) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) — empty errors means valid."""
    errors: list[str] = []
    warnings: list[str] = []

    entries, catalog_errors = check_catalog(catalog_path)
    errors.extend(catalog_errors)
    if catalog_errors:
        return errors, warnings

    seam_names = {e["surface"] for e in entries if e.get("kind") == "seam"}
    for entry in entries:
        if entry.get("kind") != "adapter":
            continue
        implements = entry.get("implements")
        if isinstance(implements, str) and implements not in seam_names:
            errors.append(
                f"surfaces[{entry['surface']}]: 'implements' references unknown seam {implements!r}"
            )

    interfaces, adapters, prefs = discover_surfaces(src_dir)
    catalog_by_surface = {e["surface"]: e for e in entries}

    for surface in sorted(interfaces | adapters):
        entry = catalog_by_surface.get(surface)
        if entry is None:
            errors.append(
                f"discovered persistence surface {surface!r} has no STORAGE-INVENTORY entry — "
                f"every store must document location, corruption recovery, and clear semantics"
            )
        elif entry.get("discoverable") is False:
            warnings.append(
                f"surfaces[{surface}]: marked discoverable: false (reviewed manual extra)"
            )

    for entry in entries:
        surface = entry["surface"]
        if entry.get("discoverable") is False:
            warnings.append(
                f"surfaces[{surface}]: discoverable: false (reviewed manual extra)"
            )
        elif surface not in (interfaces | adapters):
            errors.append(
                f"surfaces[{surface}]: catalog entry not found in code (stale row) — "
                f"remove it or mark discoverable: false with justification"
            )

    catalog_prefs = {
        e["prefs_name"] for e in entries if isinstance(e.get("prefs_name"), str) and e["prefs_name"]
    }
    for name in sorted(prefs - catalog_prefs):
        errors.append(
            f"getSharedPreferences(\"{name}\") found in code but no catalog entry declares "
            f"prefs_name: {name}"
        )
    for name in sorted(catalog_prefs - prefs):
        errors.append(
            f"catalog declares prefs_name {name!r} but no getSharedPreferences(\"{name}\") "
            f"exists in code"
        )
    return errors, warnings


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--src", default=DEFAULT_SRC, help="source root to scan")
    parser.add_argument("--catalog", default=DEFAULT_CATALOG, help="inventory YAML")
    args = parser.parse_args(argv)

    try:
        errors, warnings = validate(args.src, args.catalog)
    except OSError as exc:
        print(f"storage inventory: tool error: {exc}", file=sys.stderr)
        return 2

    for warning in warnings:
        print(f"storage inventory: WARNING: {warning}")
    if errors:
        for error in errors:
            print(f"storage inventory: ERROR: {error}")
        print(f"storage inventory: {len(errors)} error(s) — {args.catalog}")
        return 1
    count = len(check_catalog(args.catalog)[0])
    print(f"storage inventory OK: {count} surfaces documented ({args.catalog})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
