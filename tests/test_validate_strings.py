"""Unit tests for scripts/validate_strings.py — localization parity rules."""
import os
import sys
import tempfile
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO_ROOT, "scripts"))

import validate_strings  # noqa: E402


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(content)


BASELINE = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name" translatable="false">iNWEB Browser</string>
    <string name="action_back">Back</string>
    <string name="tabs_count">%1$d open tabs</string>
</resources>
"""


class ValidateStringsTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-strings-")
        self.res = os.path.join(self.tmp, "res")
        write(os.path.join(self.res, "values", "strings.xml"), BASELINE)

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmp, ignore_errors=True)

    def add_locale(self, body, locale="values-bn"):
        write(os.path.join(self.res, locale, "strings.xml"), body)

    def test_valid_locale_passes(self):
        self.add_locale(
            '<resources>\n'
            '    <string name="action_back">পেছনে</string>\n'
            '    <string name="tabs_count">%1$d টি ট্যাব খোলা</string>\n'
            '</resources>\n'
        )
        self.assertEqual([], validate_strings.validate(self.res))

    def test_translatable_false_key_not_required(self):
        # app_name is translatable="false": its absence from the locale is fine
        self.add_locale(
            '<resources>\n'
            '    <string name="action_back">পেছনে</string>\n'
            '    <string name="tabs_count">%1$d টি ট্যাব খোলা</string>\n'
            '</resources>\n'
        )
        errors = validate_strings.validate(self.res)
        self.assertEqual([], errors)

    def test_missing_translation_detected(self):
        self.add_locale(
            '<resources>\n'
            '    <string name="tabs_count">%1$d টি ট্যাব খোলা</string>\n'
            '</resources>\n'
        )
        errors = validate_strings.validate(self.res)
        self.assertTrue(any("missing translation" in e and "action_back" in e for e in errors))

    def test_placeholder_mismatch_detected(self):
        self.add_locale(
            '<resources>\n'
            '    <string name="action_back">পেছনে</string>\n'
            '    <string name="tabs_count">%2$d টি ট্যাব খোলা</string>\n'
            '</resources>\n'
        )
        errors = validate_strings.validate(self.res)
        self.assertTrue(any("placeholder mismatch" in e and "tabs_count" in e for e in errors))

    def test_extra_locale_key_detected(self):
        self.add_locale(
            '<resources>\n'
            '    <string name="action_back">পেছনে</string>\n'
            '    <string name="tabs_count">%1$d টি ট্যাব খোলা</string>\n'
            '    <string name="mystery">??</string>\n'
            '</resources>\n'
        )
        errors = validate_strings.validate(self.res)
        self.assertTrue(any("extra key" in e and "mystery" in e for e in errors))

    def test_empty_locale_value_detected(self):
        self.add_locale(
            '<resources>\n'
            '    <string name="action_back"></string>\n'
            '    <string name="tabs_count">%1$d টি ট্যাব খোলা</string>\n'
            '</resources>\n'
        )
        errors = validate_strings.validate(self.res)
        self.assertTrue(any("empty value" in e and "action_back" in e for e in errors))

    def test_malformed_xml_detected(self):
        self.add_locale("<resources><string name='x'>broken\n")
        errors = validate_strings.validate(self.res)
        self.assertTrue(any("not well-formed" in e for e in errors))

    def test_missing_locale_directory_detected(self):
        errors = validate_strings.validate(self.res)
        self.assertTrue(any("no localized" in e for e in errors))

    def test_missing_baseline_detected(self):
        empty_res = os.path.join(self.tmp, "empty-res")
        os.makedirs(empty_res, exist_ok=True)
        errors = validate_strings.validate(empty_res)
        self.assertTrue(any("baseline strings file missing" in e for e in errors))


class AboutBaselineMirrorTestCase(unittest.TestCase):
    """The §39 About surface's chromium_baseline resource must mirror the pinned tag."""

    def setUp(self):
        import shutil

        self.tmp = tempfile.mkdtemp(prefix="inweb-mirror-")
        self.res = os.path.join(self.tmp, "res")
        self.baseline = os.path.join(self.tmp, "BASELINE")
        self._shutil = shutil

    def tearDown(self):
        self._shutil.rmtree(self.tmp, ignore_errors=True)

    def make(self, resource_line, tag="154.0.8037.21"):
        write(
            os.path.join(self.res, "values", "strings.xml"),
            '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            + resource_line
            + "\n</resources>\n",
        )
        write(self.baseline, f"# comment\nCHROMIUM_TAG={tag}\nPINNED_ON=2026-09-12\n")

    def test_synced_mirror_passes(self):
        self.make('<string name="chromium_baseline" translatable="false">154.0.8037.21</string>')
        self.assertEqual([], validate_strings.check_about_baseline(self.res, self.baseline))

    def test_missing_resource_is_an_error(self):
        self.make('<string name="other">x</string>')
        errors = validate_strings.check_about_baseline(self.res, self.baseline)
        self.assertEqual(1, len(errors))
        self.assertIn("must define", errors[0])

    def test_drifted_value_reports_both_sides(self):
        self.make('<string name="chromium_baseline" translatable="false">153.0.0.1</string>')
        errors = validate_strings.check_about_baseline(self.res, self.baseline)
        self.assertEqual(1, len(errors))
        self.assertIn("153.0.0.1", errors[0])
        self.assertIn("154.0.8037.21", errors[0])

    def test_translatable_mirror_is_rejected(self):
        self.make('<string name="chromium_baseline">154.0.8037.21</string>')
        errors = validate_strings.check_about_baseline(self.res, self.baseline)
        self.assertTrue(any("translatable" in error for error in errors))

    def test_unreadable_baseline_is_an_error(self):
        self.make('<string name="chromium_baseline" translatable="false">154.0.8037.21</string>')
        errors = validate_strings.check_about_baseline(self.res, os.path.join(self.tmp, "nope"))
        self.assertEqual(1, len(errors))
        self.assertIn("cannot read", errors[0])


if __name__ == "__main__":
    unittest.main()
