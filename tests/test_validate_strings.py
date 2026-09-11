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


if __name__ == "__main__":
    unittest.main()
