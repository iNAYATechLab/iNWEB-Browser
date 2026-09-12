"""Unit tests for scripts/validate_localization.py (§36 externalization gate)."""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))

import validate_localization as vl  # noqa: E402


class ValidateLocalizationTest(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.app_dir = os.path.join(self._tmp.name, "browser")
        self.ui_dir = os.path.join(self.app_dir, "ui")
        os.makedirs(self.ui_dir)

    def tearDown(self):
        self._tmp.cleanup()

    def write_ui(self, name: str, content: str):
        with open(os.path.join(self.ui_dir, name), "w", encoding="utf-8") as handle:
            handle.write(content)

    def test_externalized_ui_passes(self):
        self.write_ui(
            "Clean.kt",
            'Text(stringResource(R.string.home_title))\n'
            'Text(text = stringResource(R.string.subtitle))\n',
        )
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual([], errors)

    def test_positional_text_literal_is_flagged(self):
        self.write_ui("Bad.kt", 'Text("Hello world")\n')
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual(1, len(errors))
        self.assertIn("Hello world", errors[0])
        self.assertIn("externalize", errors[0])

    def test_named_parameter_literals_are_flagged(self):
        self.write_ui(
            "Bad.kt",
            'IconButton(contentDescription = "Open menu") { }\n'
            'Placeholder(label = "Search", title = "Find") { }\n',
        )
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual(3, len(errors))
        self.assertTrue(any("Open menu" in e for e in errors))
        self.assertTrue(any("Search" in e for e in errors))
        self.assertTrue(any("Find" in e for e in errors))

    def test_empty_digits_and_interpolation_are_allowed(self):
        self.write_ui(
            "Ok.kt",
            'Text("")\n'
            'Text("42")\n'
            'Text("${viewModel.tabCount}")\n'
            'Text(text = "${state.title}")\n',
        )
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual([], errors)

    def test_non_localized_marker_is_reviewed_escape_hatch(self):
        self.write_ui('Flagged.kt', 'Text("HTTP") // NON-LOCALIZED\n')
        errors, warnings = vl.validate(self.app_dir)
        self.assertEqual([], errors)
        self.assertEqual(1, len(warnings))
        self.assertIn("escape hatch", warnings[0])

    def test_entry_activity_is_scanned(self):
        with open(os.path.join(self.app_dir, "MainActivity.kt"), "w", encoding="utf-8") as handle:
            handle.write('Text("Welcome!")\n')
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual(1, len(errors))
        self.assertIn("MainActivity.kt", errors[0])

    def test_non_ui_sources_are_not_scanned(self):
        # a clean UI file so the scan has something to walk
        self.write_ui("Clean.kt", 'Text(stringResource(R.string.ok))\n')
        other = os.path.join(self.app_dir, "settings")
        os.makedirs(other)
        with open(os.path.join(other, "Store.kt"), "w", encoding="utf-8") as handle:
            handle.write('const val FORMAT = "iNWEB-STORE v=1"\n')
        # a violation in the NON-UI file must NOT be flagged
        with open(os.path.join(other, "Screen.kt"), "w", encoding="utf-8") as handle:
            handle.write('Text("hardcoded in non-ui source")\n')
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual([], errors)

    def test_missing_ui_directory_is_an_error(self):
        import shutil
        shutil.rmtree(self.ui_dir)
        errors, _ = vl.validate(self.app_dir)
        self.assertEqual(1, len(errors))
        self.assertIn("no authored UI sources", errors[0])


if __name__ == "__main__":
    unittest.main()
