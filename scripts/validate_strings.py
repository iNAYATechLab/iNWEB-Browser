#!/usr/bin/env python3
"""Validate Android string resources: locale parity and placeholder consistency.

For every values-*/strings.xml against the baseline values/strings.xml, checks:
  * every translatable baseline key exists in the locale file
  * no extra keys exist only in the locale file
  * no empty values
  * format placeholders (%s, %d, %1$s, ...) match the baseline exactly
  * at least one locale directory exists (expected: values-bn)

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


def main(argv: list | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate localized string resources")
    parser.add_argument("--res-dir", default=DEFAULT_RES_DIR,
                        help="path to the Android res/ directory")
    args = parser.parse_args(argv)

    if not os.path.isdir(args.res_dir):
        print(f"error: res directory not found: {args.res_dir}", file=sys.stderr)
        return 2

    errors = validate(args.res_dir)
    if errors:
        for err in errors:
            print(f"INVALID: {err}")
        return 1
    print(f"string resources OK: {args.res_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
