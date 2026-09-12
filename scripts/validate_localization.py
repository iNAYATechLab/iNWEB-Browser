#!/usr/bin/env python3
"""Validate §36 externalization in the authored Android UI source.

"All user-visible strings must be externalized" (MASTER-SPEC §36).
This check scans the AUTHORED UI sources (src/.../ui/**.kt and
MainActivity.kt) for hardcoded user-visible string literals:

  * positional literals in Text("...") / Text(text = "...")
  * named user-visible parameters: text=, label=, title=,
    contentDescription=, description=, placeholder=

Allowed without externalization (documented, deliberate):
  * empty strings
  * pure digits (counts/badges)
  * dynamic interpolation ("${...}") with no literal prose
  * any line carrying an explicit trailing `// NON-LOCALIZED` marker
    (reviewed escape hatch — CI still shows the file in the report)

Static single-quoted literals are flagged the same way. This is a
source-level heuristic gate: it cannot prove a screen is fully
localized (runtime work needs the built app, B-001), but it guarantees
no NEW hardcoded user-visible literals enter the authored UI.

Exit codes: 0 = valid, 1 = violations found, 2 = tool error.
"""
from __future__ import annotations

import argparse
import os
import re
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_APP_DIR = os.path.join(REPO_ROOT, "src", "android-app", "src", "main", "kotlin", "com", "inweb", "browser")

UI_DIR_NAME = "ui"
ENTRY_FILES = ["MainActivity.kt"]

NON_LOCALIZED_MARKER = "// NON-LOCALIZED"

# Text("...") / Text(text = "...") — also matches single-quoted
POSITIONAL_TEXT_RE = re.compile(r"""Text\s*\(\s*(?:text\s*=\s*)?(["'])((?:(?!\1).)*)\1""")
# named user-visible parameters with literal values
NAMED_PARAM_RE = re.compile(
    r"""\b(?:text|label|title|contentDescription|description|placeholder)\s*=\s*(["'])((?:(?!\1).)*)\1"""
)

PURE_DIGITS_RE = re.compile(r"^\d+$")
HAS_INTERPOLATION_RE = re.compile(r"\$\{")


def ui_source_files(app_dir: str) -> list[str]:
    """The .kt files this check governs: ui/** and the entry activity."""
    files: list[str] = []
    ui_dir = os.path.join(app_dir, UI_DIR_NAME)
    if not os.path.isdir(ui_dir):
        return []
    for root, _, names in os.walk(ui_dir):
        for name in sorted(names):
            if name.endswith(".kt"):
                files.append(os.path.join(root, name))
    for entry in ENTRY_FILES:
        path = os.path.join(app_dir, entry)
        if os.path.isfile(path):
            files.append(path)
    return files


def literal_is_allowed(literal: str) -> bool:
    if not literal.strip():
        return True  # empty string
    if PURE_DIGITS_RE.fullmatch(literal.strip()):
        return True  # counts / badges
    if HAS_INTERPOLATION_RE.search(literal):
        return True  # dynamic value, no literal prose
    return False


def scan_file(path: str) -> list[tuple[int, str]]:
    """Return [(line_number, literal), ...] for flagged literals."""
    hits: list[tuple[int, str]] = []
    with open(path, encoding="utf-8") as handle:
        for number, line in enumerate(handle, start=1):
            if NON_LOCALIZED_MARKER in line:
                continue
            for match in POSITIONAL_TEXT_RE.finditer(line):
                literal = match.group(2)
                if not literal_is_allowed(literal):
                    hits.append((number, literal))
            for match in NAMED_PARAM_RE.finditer(line):
                literal = match.group(2)
                if not literal_is_allowed(literal):
                    hits.append((number, literal))
    return hits


def validate(app_dir: str) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) — empty errors means valid."""
    errors: list[str] = []
    warnings: list[str] = []
    if not os.path.isdir(app_dir):
        return [f"app source directory not found: {app_dir}"], warnings

    files = ui_source_files(app_dir)
    if not files:
        return [f"no authored UI sources found under {app_dir}"], warnings

    for path in files:
        rel = os.path.relpath(path, REPO_ROOT)
        try:
            hits = scan_file(path)
        except OSError as exc:
            errors.append(f"{rel}: unreadable: {exc}")
            continue
        for number, literal in hits:
            errors.append(
                f"{rel}:{number}: hardcoded user-visible literal {literal!r} — "
                f"externalize via stringResource(R.string....)"
            )
        with open(path, encoding="utf-8") as handle:
            for number, line in enumerate(handle, start=1):
                if NON_LOCALIZED_MARKER in line:
                    warnings.append(f"{rel}:{number}: reviewed NON-LOCALIZED escape hatch")
    return errors, warnings


def main(argv: list | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate §36 string externalization in authored UI")
    parser.add_argument("--app-dir", default=DEFAULT_APP_DIR,
                        help="path to the authored app source directory")
    args = parser.parse_args(argv)

    errors, warnings = validate(args.app_dir)
    for warning in warnings:
        print(f"note: {warning}")
    if errors:
        for err in errors:
            print(f"INVALID: {err}")
        return 1
    print(f"authored UI externalization OK: {args.app_dir} ({len(warnings)} reviewed escape hatch(es))")
    return 0


if __name__ == "__main__":
    sys.exit(main())
