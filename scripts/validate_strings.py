#!/usr/bin/env python3
"""Validate Android string resources: locale parity and placeholder consistency.

For every values-*/strings.xml against the baseline values/strings.xml, checks:
  * every translatable baseline key exists in the locale file
  * no extra keys exist only in the locale file
  * no empty values
  * format placeholders (%s, %d, %1$s, ...) match the baseline exactly
  * at least one locale directory exists (expected: values-bn)

Also enforces the About-surface baseline mirror (§39): the
non-translatable `chromium_baseline` resource in values/strings.xml
must exist, be translatable="false", and equal the pinned
CHROMIUM_TAG in config/chromium/BASELINE — the app can never show a
baseline the build does not use.

Exit codes: 0 = valid, 1 = invalid, 2 = tool error.
"""
from __future__ import annotations

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_RES_DIR = os.path.join(REPO_ROOT, "src", "android-app", "src", "main", "res")
DEFAULT_BASELINE = os.path.join(REPO_ROOT, "config", "chromium", "BASELINE")

# Shared parser for the pinned-baseline file (same format contract as
# scripts/check_baseline.py and scripts/fetch_chromium.sh).
import check_baseline  # noqa: E402  (sibling script; repo scripts dir)

PLACEHOLDER_RE = re.compile(r"%\d+\$[sdf]|%[sdf]")


def load_strings(path: str) -> dict[str, tuple[str, bool]]:
    """Parse a strings.xml into {key: (value, translatable)}."""
    tree = ET.parse(path)
    entries: dict[str, tuple[str, bool]] = {}
    for el in tree.getroot().findall("string"):
        key = el.get("name")
        if key is None:
            continue
        value = "".join(el.itertext())
        translatable = (el.get("translatable", "true").lower() != "false")
        entries[key] = (value, translatable)
    return entries


def validate(res_dir: str) -> list[str]:
    """Return a list of validation error messages (empty list = valid)."""
    errors: list[str] = []
    base_path = os.path.join(res_dir, "values", "strings.xml")
    if not os.path.isfile(base_path):
        return [f"baseline strings file missing: {base_path}"]
    try:
        base = load_strings(base_path)
    except ET.ParseError as exc:
        return [f"baseline strings file not well-formed: {exc}"]

    translatable_keys = {k for k, (_, t) in base.items() if t}

    locales: list[str] = []
    if os.path.isdir(res_dir):
        for name in sorted(os.listdir(res_dir)):
            if name.startswith("values-") and os.path.isfile(
                os.path.join(res_dir, name, "strings.xml")
            ):
                locales.append(name)
    if not locales:
        errors.append("no localized values-*/strings.xml found (expected at least values-bn)")

    for locale in locales:
        path = os.path.join(res_dir, locale, "strings.xml")
        try:
            entries = load_strings(path)
        except ET.ParseError as exc:
            errors.append(f"{locale}: not well-formed: {exc}")
            continue
        locale_keys = set(entries)

        for key in sorted(translatable_keys - locale_keys):
            errors.append(f"{locale}: missing translation for '{key}'")
        for key in sorted(locale_keys - set(base)):
            errors.append(f"{locale}: extra key not present in baseline: '{key}'")

        for key in sorted(set(base) & locale_keys):
            if key not in translatable_keys:
                continue
            base_value, _ = base[key]
            locale_value, _ = entries[key]
            if not locale_value.strip():
                errors.append(f"{locale}: empty value for '{key}'")
                continue
            base_placeholders = sorted(PLACEHOLDER_RE.findall(base_value))
            locale_placeholders = sorted(PLACEHOLDER_RE.findall(locale_value))
            if base_placeholders != locale_placeholders:
                errors.append(
                    f"{locale}: placeholder mismatch for '{key}': "
                    f"baseline {base_placeholders} vs locale {locale_placeholders}"
                )
    return errors


def check_about_baseline(res_dir: str, baseline_path: str) -> list[str]:
    """The `chromium_baseline` resource must mirror the pinned CHROMIUM_TAG."""
    errors: list[str] = []
    try:
        tag, _ = check_baseline.read_baseline(baseline_path)
    except (OSError, ValueError) as exc:
        return [f"cannot read pinned baseline {baseline_path}: {exc}"]

    base_path = os.path.join(res_dir, "values", "strings.xml")
    try:
        base = load_strings(base_path)
    except (OSError, ET.ParseError) as exc:
        return [f"cannot read {base_path}: {exc}"]

    entry = base.get("chromium_baseline")
    if entry is None:
        return [
            "values/strings.xml must define the non-translatable 'chromium_baseline' "
            "resource (the About surface shows it; it mirrors config/chromium/BASELINE)"
        ]
    value, translatable = entry
    if translatable:
        errors.append("'chromium_baseline' must be translatable=\"false\" — a version tag is not prose")
    if value.strip() != tag:
        errors.append(
            f"'chromium_baseline' resource is {value.strip()!r} but the pinned "
            f"CHROMIUM_TAG is {tag!r} — update the resource with the baseline refresh"
        )
    return errors


def main(argv: list | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate localized string resources")
    parser.add_argument("--res-dir", default=DEFAULT_RES_DIR,
                        help="path to the Android res/ directory")
    parser.add_argument("--baseline", default=DEFAULT_BASELINE,
                        help="path to config/chromium/BASELINE")
    args = parser.parse_args(argv)

    if not os.path.isdir(args.res_dir):
        print(f"error: res directory not found: {args.res_dir}", file=sys.stderr)
        return 2

    errors = validate(args.res_dir)
    errors.extend(check_about_baseline(args.res_dir, args.baseline))
    if errors:
        for err in errors:
            print(f"INVALID: {err}")
        return 1
    print(f"string resources OK: {args.res_dir} (chromium_baseline mirror in sync)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
