#!/usr/bin/env python3
"""Check the pinned Chromium baseline against the current upstream Android stable.

Data source: the public Chrome Version History API.

Exit codes: 0 = pinned baseline is current; 1 = a newer upstream Android stable
exists (baseline drift — rebase required per docs/PHASE0-CHROMIUM-BASELINE.md);
2 = tool error.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.request
from datetime import datetime, timezone

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_BASELINE = os.path.join(REPO_ROOT, "config", "chromium", "BASELINE")
API_URL = (
    "https://versionhistory.googleapis.com/v1/chrome/platforms/"
    "android/channels/stable/versions?pageSize=1"
)


def read_baseline(path: str) -> tuple[str, str | None]:
    """Parse CHROMIUM_TAG (required) and PINNED_ON (optional) from the BASELINE file."""
    tag = None
    pinned_on = None
    with open(path, "r", encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line.startswith("CHROMIUM_TAG="):
                tag = line.split("=", 1)[1].strip()
            elif line.startswith("PINNED_ON="):
                pinned_on = line.split("=", 1)[1].strip()
    if not tag:
        raise ValueError(f"CHROMIUM_TAG not found in {path}")
    return tag, pinned_on


def version_tuple(version: str) -> tuple[int, ...]:
    """Convert a dotted version string to a comparable tuple of ints."""
    return tuple(int(part) for part in version.split("."))


def fetch_latest_stable(timeout: int = 20) -> str:
    """Return the newest upstream Android stable version string."""
    with urllib.request.urlopen(API_URL, timeout=timeout) as resp:
        data = json.load(resp)
    return data["versions"][0]["version"]


def main(argv: list | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--baseline", default=DEFAULT_BASELINE,
                        help="path to the BASELINE file")
    parser.add_argument("--json", action="store_true", help="emit a JSON report")
    args = parser.parse_args(argv)

    try:
        pinned, pinned_on = read_baseline(args.baseline)
        latest = fetch_latest_stable()
    except (OSError, ValueError, KeyError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2

    drift = version_tuple(latest) > version_tuple(pinned)
    payload = {
        "pinned": pinned,
        "pinned_on": pinned_on,
        "latest": latest,
        "drift": drift,
        "checked_at": datetime.now(timezone.utc).isoformat(),
    }
    if args.json:
        print(json.dumps(payload, indent=2))
    else:
        status = "UPDATE AVAILABLE — rebase required" if drift else "CURRENT"
        print(f"pinned : {pinned} (pinned on {pinned_on})")
        print(f"latest : {latest}")
        print(f"status : {status}")
    return 1 if drift else 0


if __name__ == "__main__":
    sys.exit(main())
