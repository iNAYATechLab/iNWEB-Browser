#!/usr/bin/env python3
"""Validate the iNWEB patch registry (iNWEB_PATCHES/MANIFEST.yaml).

Checks:
  * schema_version is 1; 'patches' is a list
  * required fields present (id, file, area, description)
  * id matches NNNN-slug; file name matches id; file lives under its area dir
  * areas restricted to the architectural patch areas
  * numeric prefixes strictly increasing; ids unique
  * every registered patch file exists on disk

Exit codes: 0 = valid, 1 = invalid, 2 = tool error.
"""
from __future__ import annotations

import argparse
import os
import re
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    print("error: PyYAML is required (pip install pyyaml)", file=sys.stderr)
    sys.exit(2)

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_MANIFEST = os.path.join(REPO_ROOT, "iNWEB_PATCHES", "MANIFEST.yaml")

ALLOWED_AREAS = {
    "privacy", "security", "adblock", "popup_protection", "extension",
    "performance", "ui", "offline", "settings",
}
REQUIRED_FIELDS = ("id", "file", "area", "description")
ID_RE = re.compile(r"^(\d{4})-[a-z0-9][a-z0-9-]*$")


def validate(manifest_path: str) -> list[str]:
    """Return a list of validation error messages (empty list = valid)."""
    errors: list[str] = []
    base = os.path.dirname(os.path.abspath(manifest_path))
    try:
        with open(manifest_path, "r", encoding="utf-8") as fh:
            data = yaml.safe_load(fh) or {}
    except (OSError, yaml.YAMLError) as exc:
        return [f"cannot read manifest: {exc}"]

    if data.get("schema_version") != 1:
        errors.append(f"schema_version must be 1 (got {data.get('schema_version')!r})")

    patches = data.get("patches")
    if patches is None:
        errors.append("missing 'patches' list")
        return errors
    if not isinstance(patches, list):
        errors.append("'patches' must be a list")
        return errors

    seen_ids: set[str] = set()
    prev_prefix = -1
    for i, entry in enumerate(patches):
        where = f"patches[{i}]"
        if not isinstance(entry, dict):
            errors.append(f"{where}: entry must be a mapping")
            continue
        for field in REQUIRED_FIELDS:
            if not entry.get(field):
                errors.append(f"{where}: missing required field '{field}'")

        pid = entry.get("id") or ""
        match = ID_RE.match(pid)
        if not match:
            errors.append(f"{where}: id must match NNNN-slug (got {pid!r})")
        else:
            prefix = int(match.group(1))
            if prefix <= prev_prefix:
                errors.append(
                    f"{where}: numeric prefix {prefix:04d} breaks strictly increasing order"
                )
            else:
                prev_prefix = prefix
            if pid in seen_ids:
                errors.append(f"{where}: duplicate id {pid!r}")
            seen_ids.add(pid)

        area = entry.get("area")
        if area and area not in ALLOWED_AREAS:
            errors.append(
                f"{where}: unknown area {area!r} (allowed: {sorted(ALLOWED_AREAS)})"
            )

        file_path = entry.get("file") or ""
        if file_path:
            if not file_path.endswith(".patch"):
                errors.append(f"{where}: file must end with .patch (got {file_path!r})")
            if area and not file_path.startswith(f"{area}/"):
                errors.append(
                    f"{where}: file {file_path!r} must live under its area directory '{area}/'"
                )
            if area and match and file_path != f"{area}/{pid}.patch":
                errors.append(
                    f"{where}: file must be '{area}/{pid}.patch' (got {file_path!r})"
                )
            if not os.path.isfile(os.path.join(base, file_path)):
                errors.append(f"{where}: patch file missing on disk: {file_path}")
    return errors


def main(argv: list | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate the iNWEB patch registry")
    parser.add_argument("manifest", nargs="?", default=DEFAULT_MANIFEST,
                        help="path to MANIFEST.yaml")
    args = parser.parse_args(argv)

    errors = validate(args.manifest)
    if errors:
        for err in errors:
            print(f"INVALID: {err}")
        return 1
    print(f"registry OK: {args.manifest}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
