"""Unit tests for scripts/check_build_host.py — the B-001 pre-flight checker.

System probes are exercised structurally (the CI runner is far below the
build-host spec — only the presence/shape of results is asserted, never
their PASS/FAIL verdicts). Network calls are mocked, matching the other
tool tests (CI keeps unit tests offline).
"""
import os
import sys
import tempfile
import unittest
from unittest import mock

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO_ROOT, "scripts"))

import check_build_host  # noqa: E402


class MeminfoTestCase(unittest.TestCase):
    def test_parses_mem_total_to_gib(self):
        self.assertEqual(64.0, check_build_host.meminfo_total_gb("MemTotal:        67108864 kB\nMemFree: 1 kB\n"))

    def test_rounds_to_one_decimal(self):
        self.assertEqual(1.9, check_build_host.meminfo_total_gb("MemTotal:        2032608 kB\n"))

    def test_missing_mem_total_raises(self):
        with self.assertRaises(ValueError):
            check_build_host.meminfo_total_gb("MemFree: 1 kB\n")


class ClassifyTestCase(unittest.TestCase):
    def test_cpu_bands(self):
        self.assertEqual(check_build_host.FAIL, check_build_host.classify(15, 16, 32))
        self.assertEqual(check_build_host.MINIMUM, check_build_host.classify(16, 16, 32))
        self.assertEqual(check_build_host.MINIMUM, check_build_host.classify(31, 16, 32))
        self.assertEqual(check_build_host.PASS, check_build_host.classify(32, 16, 32))

    def test_ram_bands(self):
        self.assertEqual(check_build_host.FAIL, check_build_host.classify(63.9, 64.0, 128.0))
        self.assertEqual(check_build_host.MINIMUM, check_build_host.classify(64.0, 64.0, 128.0))
        self.assertEqual(check_build_host.PASS, check_build_host.classify(128.0, 64.0, 128.0))

    def test_disk_bands(self):
        self.assertEqual(check_build_host.FAIL, check_build_host.classify(299.9, 300.0, 500.0))
        self.assertEqual(check_build_host.MINIMUM, check_build_host.classify(300.0, 300.0, 500.0))
        self.assertEqual(check_build_host.PASS, check_build_host.classify(500.0, 300.0, 500.0))


class DiskFreeTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-disk-")

    def tearDown(self):
        import shutil

        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_reports_positive_free_space(self):
        free = check_build_host.disk_free_gb(self.tmp)
        self.assertGreater(free, 0.0)

    def test_walks_up_to_an_existing_ancestor(self):
        free = check_build_host.disk_free_gb(os.path.join(self.tmp, "chromium", "src"))
        self.assertGreater(free, 0.0)


class OsReleaseTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-osrel-")

    def tearDown(self):
        import shutil

        shutil.rmtree(self.tmp, ignore_errors=True)

    def write(self, content):
        path = os.path.join(self.tmp, "os-release")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(content)
        return path

    def test_parses_pretty_name_and_version(self):
        pretty, version = check_build_host.os_release(
            self.write('NAME="Ubuntu"\nVERSION_ID="22.04"\nPRETTY_NAME="Ubuntu 22.04.4 LTS"\n')
        )
        self.assertEqual("Ubuntu 22.04.4 LTS", pretty)
        self.assertEqual((22, 4), version)

    def test_missing_file_yields_none(self):
        self.assertEqual((None, None), check_build_host.os_release(os.path.join(self.tmp, "nope")))


class UbuntuStatusTestCase(unittest.TestCase):
    def test_unparseable_is_a_warning(self):
        result = check_build_host.ubuntu_status(None, None)
        self.assertEqual(check_build_host.WARN, result.status)

    def test_ubuntu_2404_is_recommended(self):
        result = check_build_host.ubuntu_status("Ubuntu 24.04.1 LTS", (24, 4))
        self.assertEqual(check_build_host.PASS, result.status)

    def test_ubuntu_2204_is_minimum(self):
        result = check_build_host.ubuntu_status("Ubuntu 22.04.4 LTS", (22, 4))
        self.assertEqual(check_build_host.MINIMUM, result.status)

    def test_other_distro_is_a_warning(self):
        result = check_build_host.ubuntu_status("Debian GNU/Linux 12 (bookworm)", (12,))
        self.assertEqual(check_build_host.WARN, result.status)


class RepoCheckTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-repo-")

    def tearDown(self):
        import shutil

        shutil.rmtree(self.tmp, ignore_errors=True)

    def make_repo(self, baseline="CHROMIUM_TAG=154.0.8037.21\n"):
        for rel in (
            "config/chromium/args-development.gn",
            "config/chromium/args-release.gn",
            "scripts/fetch_chromium.sh",
            "scripts/build_android.sh",
            "scripts/apply_patches.py",
            "iNWEB_PATCHES/MANIFEST.yaml",
        ):
            path = os.path.join(self.tmp, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as handle:
                handle.write("stub\n")
        path = os.path.join(self.tmp, "config/chromium/BASELINE")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(baseline)

    def test_complete_repo_has_no_failures_and_pins_the_tag(self):
        self.make_repo()
        results = check_build_host.check_repo(self.tmp)
        self.assertEqual([], check_build_host.failures(results))
        baseline = [r for r in results if r.name == "pinned baseline"]
        self.assertEqual(1, len(baseline))
        self.assertIn("154.0.8037.21", baseline[0].detail)

    def test_missing_baseline_fails(self):
        self.make_repo(baseline="# no tag here\n")
        results = check_build_host.check_repo(self.tmp)
        self.assertTrue(any(r.name == "pinned baseline" and r.status == check_build_host.FAIL for r in results))

    def test_missing_build_script_fails(self):
        self.make_repo()
        os.remove(os.path.join(self.tmp, "scripts/build_android.sh"))
        results = check_build_host.check_repo(self.tmp)
        self.assertTrue(any(r.name == "repo file: scripts/build_android.sh" and r.status == check_build_host.FAIL for r in results))


class NetworkCheckTestCase(unittest.TestCase):
    def test_unreachable_endpoint_fails(self):
        with mock.patch("urllib.request.urlopen", side_effect=OSError("no route to host")):
            results = check_build_host.check_network(("https://chromium.googlesource.com",), timeout=0.1)
        self.assertEqual(check_build_host.FAIL, results[0].status)

    def test_reachable_endpoint_passes(self):
        with mock.patch("urllib.request.urlopen", return_value=mock.MagicMock()):
            results = check_build_host.check_network(("https://chromium.googlesource.com",), timeout=0.1)
        self.assertEqual(check_build_host.PASS, results[0].status)


class ReportTestCase(unittest.TestCase):
    def test_report_counts_and_verdict(self):
        results = [
            check_build_host.CheckResult("a", check_build_host.PASS, "ok"),
            check_build_host.CheckResult("b", check_build_host.FAIL, "bad"),
        ]
        report = check_build_host.format_report(results, skipped_network=False)
        self.assertIn("1 recommended-pass", report)
        self.assertIn("1 failures", report)
        self.assertIn("fix the FAIL rows", report)

    def test_clean_report_points_at_the_runbook(self):
        results = [check_build_host.CheckResult("a", check_build_host.MINIMUM, "ok")]
        report = check_build_host.format_report(results, skipped_network=True)
        self.assertIn("proceed with docs/BUILD-HOST-RUNBOOK.md step 2", report)
        self.assertIn("network checks skipped", report)


class RunChecksSmokeTestCase(unittest.TestCase):
    """Structural smoke test on the real repository (network skipped)."""

    def test_all_results_are_well_formed_and_core_probes_present(self):
        results = check_build_host.run_checks(REPO_ROOT, os.path.join(REPO_ROOT, "chromium"), skip_network=True)
        names = {result.name for result in results}
        for expected in ("cpu cores", "memory", "operating system", "pinned baseline"):
            self.assertIn(expected, names)
        for result in results:
            self.assertTrue(result.name and result.detail and result.spec)
            self.assertIn(result.status, (check_build_host.PASS, check_build_host.MINIMUM, check_build_host.WARN, check_build_host.FAIL, check_build_host.INFO))


if __name__ == "__main__":
    unittest.main()
