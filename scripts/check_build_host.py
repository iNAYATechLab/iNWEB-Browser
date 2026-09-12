#!/usr/bin/env python3
"""Pre-flight check for the B-001 build host (BUILD-INFRASTRUCTURE.md §2).

Run THIS on the machine being provisioned as the Chromium build host,
BEFORE scripts/fetch_chromium.sh:

    python3 scripts/check_build_host.py [--skip-network] [--workspace PATH]

Every check cites the spec row it verifies. Honest by design: a FAIL
means "do not start the multi-hour fetch yet — fix this first"; WARN
means "works, but below the recommended/spec software row"; INFO means
context, not a verdict. This script VERIFIES prerequisites — it is not
a build and claims no build result (§57, B-001).

Exit codes: 0 = all mandatory checks pass; 1 = mandatory failures;
2 = tool error.
"""
from __future__ import annotations

import argparse
import dataclasses
import os
import platform
import shutil
import sys
import urllib.request

SPEC = "BUILD-INFRASTRUCTURE §2"

# Spec rows (minimum / recommended).
MIN_CPU, REC_CPU = 16, 32
MIN_RAM_GB, REC_RAM_GB = 64.0, 128.0
MIN_DISK_GB, REC_DISK_GB = 300.0, 500.0

NETWORK_ENDPOINTS = (
    "https://chromium.googlesource.com",
    "https://storage.googleapis.com",
    "https://commondatastorage.googleapis.com",
    "https://github.com",
)

# Result statuses. FAIL = mandatory; WARN = proceed with caution; INFO = context.
PASS = "PASS"          # meets the recommended spec
MINIMUM = "MINIMUM"    # meets the minimum spec
WARN = "WARN"
FAIL = "FAIL"
INFO = "INFO"


@dataclasses.dataclass(frozen=True)
class CheckResult:
    name: str
    status: str
    detail: str
    spec: str = SPEC


def classify(value: float, minimum: float, recommended: float) -> str:
    """Map a measured value onto the spec's minimum/recommended bands."""
    if value < minimum:
        return FAIL
    if value < recommended:
        return MINIMUM
    return PASS


def meminfo_total_gb(meminfo_text: str) -> float:
    """Parse /proc/meminfo content and return MemTotal in GiB (1 decimal)."""
    for line in meminfo_text.splitlines():
        if line.startswith("MemTotal:"):
            fields = line.split()
            if len(fields) < 2:
                break
            try:
                total_kb = int(fields[1])
            except ValueError:
                break
            return round(total_kb / (1024 * 1024), 1)
    raise ValueError("MemTotal not found in meminfo")


def os_release(path: str = "/etc/os-release") -> tuple[str | None, tuple[int, ...] | None]:
    """Return (PRETTY_NAME, version tuple) from os-release, or (None, None)."""
    pretty: str | None = None
    version_id: str | None = None
    try:
        with open(path, "r", encoding="utf-8") as handle:
            for line in handle:
                if line.startswith("PRETTY_NAME="):
                    pretty = line.split("=", 1)[1].strip().strip('"')
                elif line.startswith("VERSION_ID="):
                    version_id = line.split("=", 1)[1].strip().strip('"')
    except OSError:
        return None, None
    version = None
    if version_id:
        try:
            version = tuple(int(part) for part in version_id.split(".") if part)
        except ValueError:
            version = None
    return pretty, version


def ubuntu_status(pretty: str | None, version: tuple[int, ...] | None) -> CheckResult:
    """The spec's OS row: Ubuntu 22.04 x86-64 minimum, 24.04 recommended."""
    if pretty is None or version is None:
        return CheckResult(
            "operating system",
            WARN,
            f"{pretty or 'unknown OS'} — cannot read /etc/os-release version info",
        )
    ubuntu = "ubuntu" in pretty.lower()
    if ubuntu and version >= (24, 4):
        return CheckResult("operating system", PASS, f"{pretty} (x86-64)" if _is_x86_64() else f"{pretty}")
    if ubuntu and version >= (22, 4):
        return CheckResult("operating system", MINIMUM, f"{pretty} — spec minimum is 22.04, recommended 24.04")
    return CheckResult(
        "operating system",
        WARN,
        f"{pretty} — the spec (and upstream build scripts) assume Ubuntu 22.04+ x86-64",
    )


def _is_x86_64() -> bool:
    return platform.machine() in ("x86_64", "AMD64") and sys.platform.startswith("linux")


def disk_free_gb(path: str) -> float:
    """Free space in GiB on the filesystem containing [path]."""
    probe = path
    while not os.path.exists(probe):
        parent = os.path.dirname(probe)
        if parent == probe:
            raise ValueError(f"no existing ancestor for {path}")
        probe = parent
    return round(shutil.disk_usage(probe).free / (1024 ** 3), 1)


def check_repo(root: str) -> list[CheckResult]:
    """The repository must be complete: pinned baseline, GN args, build scripts."""
    results: list[CheckResult] = []

    baseline_path = os.path.join(root, "config", "chromium", "BASELINE")
    tag = None
    try:
        with open(baseline_path, "r", encoding="utf-8") as handle:
            for line in handle:
                if line.startswith("CHROMIUM_TAG="):
                    tag = line.split("=", 1)[1].strip()
                    break
    except OSError:
        pass
    if tag:
        results.append(CheckResult("pinned baseline", PASS, f"CHROMIUM_TAG={tag}"))
    else:
        results.append(CheckResult("pinned baseline", FAIL, f"missing/unreadable CHROMIUM_TAG in {baseline_path}"))

    for rel in (
        "config/chromium/args-development.gn",
        "config/chromium/args-release.gn",
        "scripts/fetch_chromium.sh",
        "scripts/build_android.sh",
        "scripts/apply_patches.py",
        "iNWEB_PATCHES/MANIFEST.yaml",
    ):
        path = os.path.join(root, rel)
        if os.path.isfile(path):
            results.append(CheckResult(f"repo file: {rel}", PASS, "present"))
        else:
            results.append(CheckResult(f"repo file: {rel}", FAIL, "missing — clone the repository fully"))

    return results


def check_network(endpoints: tuple[str, ...] = NETWORK_ENDPOINTS, timeout: float = 10.0) -> list[CheckResult]:
    """Outbound HTTPS reachability of the endpoints the fetch needs."""
    results: list[CheckResult] = []
    for url in endpoints:
        try:
            request = urllib.request.Request(url, method="HEAD")
            urllib.request.urlopen(request, timeout=timeout)
            results.append(CheckResult(f"network: {url}", PASS, "reachable"))
        except Exception as exc:  # noqa: BLE001 — any failure means unreachable
            results.append(CheckResult(f"network: {url}", FAIL, f"unreachable: {exc}"))
    return results


def failures(results: list[CheckResult]) -> list[CheckResult]:
    return [result for result in results if result.status == FAIL]


def format_report(results: list[CheckResult], skipped_network: bool) -> str:
    lines = []
    for result in results:
        lines.append(f"[{result.status:^7}] {result.name}: {result.detail}  ({result.spec})")
    counts = {status: sum(1 for r in results if r.status == status) for status in (PASS, MINIMUM, WARN, FAIL, INFO)}
    lines.append("")
    lines.append(
        f"Summary: {counts[PASS]} recommended-pass, {counts[MINIMUM]} minimum-pass, "
        f"{counts[WARN]} warnings, {counts[FAIL]} failures"
        + (" (network checks skipped)" if skipped_network else "")
    )
    if failures(results):
        lines.append("Verdict: fix the FAIL rows before running scripts/fetch_chromium.sh")
    else:
        lines.append("Verdict: prerequisites met — proceed with docs/BUILD-HOST-RUNBOOK.md step 2")
    return "\n".join(lines)


def run_checks(root: str, workspace: str, skip_network: bool) -> list[CheckResult]:
    """Collect all check results (kept separate from main() for testability)."""
    results: list[CheckResult] = []

    # OS / architecture — Linux x86-64 is mandatory for the Android build.
    if _is_x86_64():
        pretty, version = os_release()
        results.append(ubuntu_status(pretty, version))
    else:
        results.append(
            CheckResult(
                "operating system",
                FAIL,
                f"{platform.system()} {platform.machine()} — Chromium Android builds require Linux x86-64",
            )
        )

    # CPU / RAM / disk against the spec bands.
    cpus = os.cpu_count() or 0
    results.append(
        CheckResult(
            "cpu cores",
            classify(cpus, MIN_CPU, REC_CPU),
            f"{cpus} cores (minimum {MIN_CPU}, recommended {REC_CPU})",
        )
    )

    try:
        with open("/proc/meminfo", "r", encoding="utf-8") as handle:
            ram_gb = meminfo_total_gb(handle.read())
        results.append(
            CheckResult(
                "memory",
                classify(ram_gb, MIN_RAM_GB, REC_RAM_GB),
                f"{ram_gb} GiB (minimum {MIN_RAM_GB:.0f}, recommended {REC_RAM_GB:.0f})",
            )
        )
    except (OSError, ValueError) as exc:
        results.append(CheckResult("memory", FAIL, f"cannot determine RAM: {exc}"))

    try:
        free_gb = disk_free_gb(workspace)
        results.append(
            CheckResult(
                f"disk free ({workspace})",
                classify(free_gb, MIN_DISK_GB, REC_DISK_GB),
                f"{free_gb} GiB free (minimum {MIN_DISK_GB:.0f}, recommended {REC_DISK_GB:.0f})",
            )
        )
    except (OSError, ValueError) as exc:
        results.append(CheckResult("disk free", FAIL, f"cannot determine free space: {exc}"))

    # Tools the scripts actually invoke.
    for tool, mandatory in (("git", True), ("python3", True), ("curl", False)):
        present = shutil.which(tool) is not None
        results.append(
            CheckResult(
                f"tool: {tool}",
                PASS if present else (FAIL if mandatory else WARN),
                "present" if present else "missing",
            )
        )

    # depot_tools: pre-installed is fine; otherwise fetch_chromium.sh clones it.
    if shutil.which("gclient") or os.path.isdir(os.path.join(workspace, "depot_tools")):
        results.append(CheckResult("depot_tools", PASS, "found (gclient)"))
    else:
        results.append(
            CheckResult("depot_tools", INFO, "not installed — scripts/fetch_chromium.sh clones it at the pinned revision")
        )

    # Docker: the spec's software row (container path / runner images).
    if shutil.which("docker"):
        results.append(CheckResult("docker", PASS, "present (spec software row)"))
    else:
        results.append(
            CheckResult("docker", WARN, "missing — required by the spec software row; the direct script path works without it")
        )

    # Self-hosted runner: reminder, not a detectable fact from here.
    results.append(
        CheckResult(
            "github actions self-hosted runner",
            INFO,
            "verify registration if CI-driven builds are intended (runbook §6); direct script builds need none",
        )
    )

    results.extend(check_repo(root))

    if skip_network:
        results.append(CheckResult("network", INFO, "skipped (--skip-network)"))
    else:
        results.extend(check_network())

    return results


def main(argv: list[str] | None = None) -> int:
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--skip-network", action="store_true", help="skip outbound reachability checks")
    parser.add_argument(
        "--workspace",
        default=os.environ.get("INWEB_WORKSPACE", os.path.join(repo_root, "chromium")),
        help="Chromium workspace path (default: $INWEB_WORKSPACE or <repo>/chromium)",
    )
    args = parser.parse_args(argv)

    try:
        results = run_checks(repo_root, args.workspace, args.skip_network)
    except Exception as exc:  # noqa: BLE001 — tool error
        print(f"check_build_host: tool error: {exc}", file=sys.stderr)
        return 2

    print(format_report(results, args.skip_network))
    return 1 if failures(results) else 0


if __name__ == "__main__":
    sys.exit(main())
