"""Unit tests for scripts/check_baseline.py — baseline parsing and comparison.

Network calls are intentionally NOT tested here (CI keeps unit tests offline);
fetch_latest_stable() is exercised by the live Upstream Watch workflow.
"""
import os
import sys
import tempfile
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO_ROOT, "scripts"))

import check_baseline  # noqa: E402


class ReadBaselineTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-baseline-")

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmp, ignore_errors=True)

    def write_baseline(self, content):
        path = os.path.join(self.tmp, "BASELINE")
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(content)
        return path

    def test_parses_tag_and_date(self):
        path = self.write_baseline(
            "# comment\nCHROMIUM_TAG=154.0.8037.21\nPINNED_ON=2026-09-12\n"
        )
        tag, pinned_on = check_baseline.read_baseline(path)
        self.assertEqual("154.0.8037.21", tag)
        self.assertEqual("2026-09-12", pinned_on)

    def test_missing_tag_raises(self):
        path = self.write_baseline("# comment only\nPINNED_ON=2026-09-12\n")
        with self.assertRaises(ValueError):
            check_baseline.read_baseline(path)


class VersionTupleTestCase(unittest.TestCase):
    def test_higher_version_is_greater(self):
        self.assertGreater(
            check_baseline.version_tuple("154.0.8037.36"),
            check_baseline.version_tuple("154.0.8037.21"),
        )

    def test_major_version_dominates(self):
        self.assertGreater(
            check_baseline.version_tuple("155.0.0.0"),
            check_baseline.version_tuple("154.9.9.9"),
        )

    def test_equal_versions(self):
        self.assertEqual(
            check_baseline.version_tuple("154.0.8037.21"),
            check_baseline.version_tuple("154.0.8037.21"),
        )

    def test_lower_version_is_smaller(self):
        self.assertLess(
            check_baseline.version_tuple("153.0.8010.36"),
            check_baseline.version_tuple("154.0.8037.21"),
        )


if __name__ == "__main__":
    unittest.main()
