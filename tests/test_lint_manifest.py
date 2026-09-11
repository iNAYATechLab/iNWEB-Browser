"""Unit tests for scripts/lint_manifest.py — registry validation rules."""
import os
import sys
import tempfile
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO_ROOT, "scripts"))

import lint_manifest  # noqa: E402


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(content)


class LintManifestTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-lint-")
        self.patches = os.path.join(self.tmp, "patches")
        self.manifest = os.path.join(self.patches, "MANIFEST.yaml")

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmp, ignore_errors=True)

    def write_manifest(self, body):
        write(self.manifest, body)
        return self.manifest

    def create_patch_file(self, relpath):
        path = os.path.join(self.patches, relpath)
        write(path, "# placeholder diff\n")
        return relpath

    def test_empty_series_is_valid(self):
        m = self.write_manifest("schema_version: 1\npatches: []\n")
        self.assertEqual([], lint_manifest.validate(m))

    def test_valid_series(self):
        self.create_patch_file("privacy/0001-alpha.patch")
        self.create_patch_file("security/0002-beta.patch")
        m = self.write_manifest(
            "schema_version: 1\n"
            "patches:\n"
            '  - id: "0001-alpha"\n'
            '    file: "privacy/0001-alpha.patch"\n'
            "    area: privacy\n"
            '    description: "first"\n'
            '  - id: "0002-beta"\n'
            '    file: "security/0002-beta.patch"\n'
            "    area: security\n"
            '    description: "second"\n'
        )
        self.assertEqual([], lint_manifest.validate(m))

    def test_missing_patch_file_is_invalid(self):
        m = self.write_manifest(
            "schema_version: 1\n"
            "patches:\n"
            '  - id: "0001-alpha"\n'
            '    file: "privacy/0001-alpha.patch"\n'
            "    area: privacy\n"
            '    description: "first"\n'
        )
        errors = lint_manifest.validate(m)
        self.assertTrue(any("missing on disk" in e for e in errors))

    def test_unknown_area_is_invalid(self):
        self.create_patch_file("privacy/0001-alpha.patch")
        m = self.write_manifest(
            "schema_version: 1\n"
            "patches:\n"
            '  - id: "0001-alpha"\n'
            '    file: "privacy/0001-alpha.patch"\n'
            "    area: nonsense\n"
            '    description: "first"\n'
        )
        errors = lint_manifest.validate(m)
        self.assertTrue(any("unknown area" in e for e in errors))

    def test_duplicate_id_is_invalid(self):
        self.create_patch_file("privacy/0001-alpha.patch")
        self.create_patch_file("security/0002-alpha.patch")
        m = self.write_manifest(
            "schema_version: 1\n"
            "patches:\n"
            '  - id: "0001-alpha"\n'
            '    file: "privacy/0001-alpha.patch"\n'
            "    area: privacy\n"
            '    description: "first"\n'
            '  - id: "0001-alpha"\n'
            '    file: "security/0002-alpha.patch"\n'
            "    area: security\n"
            '    description: "duplicate id, wrong file too"\n'
        )
        errors = lint_manifest.validate(m)
        self.assertTrue(any("duplicate id" in e for e in errors))

    def test_out_of_order_prefixes_is_invalid(self):
        self.create_patch_file("security/0003-alpha.patch")
        self.create_patch_file("privacy/0002-beta.patch")
        m = self.write_manifest(
            "schema_version: 1\n"
            "patches:\n"
            '  - id: "0003-alpha"\n'
            '    file: "security/0003-alpha.patch"\n'
            "    area: security\n"
            '    description: "listed first with a higher prefix"\n'
            '  - id: "0002-beta"\n'
            '    file: "privacy/0002-beta.patch"\n'
            "    area: privacy\n"
            '    description: "lower prefix listed after a higher one"\n'
        )
        errors = lint_manifest.validate(m)
        self.assertTrue(any("strictly increasing" in e for e in errors))

    def test_missing_required_field_is_invalid(self):
        self.create_patch_file("privacy/0001-alpha.patch")
        m = self.write_manifest(
            "schema_version: 1\n"
            "patches:\n"
            '  - id: "0001-alpha"\n'
            '    file: "privacy/0001-alpha.patch"\n'
            "    area: privacy\n"
        )
        errors = lint_manifest.validate(m)
        self.assertTrue(any("missing required field 'description'" in e for e in errors))

    def test_bad_schema_version_is_invalid(self):
        m = self.write_manifest("schema_version: 2\npatches: []\n")
        errors = lint_manifest.validate(m)
        self.assertTrue(any("schema_version" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
