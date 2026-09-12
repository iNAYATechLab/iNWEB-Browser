"""Unit tests for scripts/validate_storage_inventory.py (§50/§51 gate)."""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))

import validate_storage_inventory as vsi  # noqa: E402


class StorageInventoryTest(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.src = os.path.join(self._tmp.name, "src")
        self.catalog = os.path.join(self._tmp.name, "STORAGE-INVENTORY.yaml")
        os.makedirs(os.path.join(self.src, "core", "demo"))

    def tearDown(self):
        self._tmp.cleanup()

    # --- helpers --------------------------------------------------------------

    def write_kt(self, name: str, content: str, subdir: str = "core/demo"):
        path = os.path.join(self.src, subdir)
        os.makedirs(path, exist_ok=True)
        with open(os.path.join(path, name), "w", encoding="utf-8") as handle:
            handle.write(content)

    def write_catalog(self, surfaces_yaml: str):
        body = surfaces_yaml if surfaces_yaml.strip() else "    []\n"
        with open(self.catalog, "w", encoding="utf-8") as handle:
            handle.write("schema_version: 1\nsurfaces:\n" + body)

    def full_entry(self, surface: str, **overrides) -> str:
        fields = {
            "surface": surface,
            "kind": "seam",
            "module": "demo",
            "stores": "'demo data'",
            "location": "'demo location'",
            "corruption": "'demo recovery'",
            "clear": "'app reset'",
        }
        fields.update(overrides)
        items = list(fields.items())
        lines = [f"    - {items[0][0]}: {items[0][1]}"]
        lines += [f"      {key}: {value}" for key, value in items[1:]]
        return "\n".join(lines) + "\n"

    # --- the live gate ----------------------------------------------------------

    def test_real_repo_catalog_is_valid(self):
        repo_root = os.path.join(os.path.dirname(__file__), "..")
        errors, _ = vsi.validate(
            os.path.join(repo_root, "src"),
            os.path.join(repo_root, "docs", "STORAGE-INVENTORY.yaml"),
        )
        self.assertEqual([], errors)

    # --- discovery ----------------------------------------------------------------

    def test_discovery_finds_interfaces_adapters_and_prefs_names(self):
        self.write_kt(
            "Demo.kt",
            "interface DemoStore { fun load(): Int }\n"
            "interface DemoCache { fun get(): Int }\n"
            "interface EnginePort { fun go() }\n"
            "class FileDemoStore : DemoStore { override fun load() = 1 }\n"
            "class InMemoryDemoStore : DemoStore { override fun load() = 1 }\n"
            "class FileDemoStoreTest { fun t() {} }\n",
        )
        self.write_kt(
            "Prefs.kt",
            'class SharedPreferencesDemoStore(context: Context) {\n'
            '    val p = context.getSharedPreferences("inweb_demo", 1)\n'
            "}\n",
            subdir="android-app",
        )
        interfaces, adapters, prefs = vsi.discover_surfaces(self.src)
        self.assertEqual({"DemoStore", "DemoCache"}, interfaces)
        self.assertEqual({"FileDemoStore", "SharedPreferencesDemoStore"}, adapters)
        self.assertEqual({"inweb_demo"}, prefs)

    def test_test_sources_are_not_discovered(self):
        test_dir = os.path.join(self.src, "core", "demo", "test")
        os.makedirs(test_dir, exist_ok=True)
        with open(os.path.join(test_dir, "Bad.kt"), "w", encoding="utf-8") as handle:
            handle.write("class FileUntrackedStore\ninterface UntrackedStore\n")
        interfaces, adapters, _ = vsi.discover_surfaces(self.src)
        self.assertEqual(set(), interfaces)
        self.assertEqual(set(), adapters)

    # --- gate rules ------------------------------------------------------------------

    def test_missing_entry_for_discovered_surface_fails(self):
        self.write_kt("Demo.kt", "interface DemoStore { fun load(): Int }\n")
        self.write_catalog("")
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(1, len(errors))
        self.assertIn("DemoStore", errors[0])
        self.assertIn("no STORAGE-INVENTORY entry", errors[0])

    def test_stale_catalog_entry_fails(self):
        self.write_kt("Demo.kt", "interface DemoStore { fun load(): Int }\n")
        self.write_catalog(self.full_entry("DemoStore") + self.full_entry("GhostStore"))
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(1, len(errors))
        self.assertIn("GhostStore", errors[0])
        self.assertIn("stale row", errors[0])

    def test_discoverable_false_exempts_stale_but_not_blank_fields(self):
        self.write_kt("Demo.kt", "interface DemoStore { fun load(): Int }\n")
        self.write_catalog(
            self.full_entry("DemoStore")
            + self.full_entry("ManualExtra", discoverable="false")
        )
        errors, warnings = vsi.validate(self.src, self.catalog)
        self.assertEqual([], errors)
        # manual extras stay visible in the gate report
        self.assertTrue(any("ManualExtra" in w for w in warnings))
        self.assertTrue(any("discoverable: false" in w for w in warnings))

    def test_blank_required_field_fails(self):
        self.write_kt("Demo.kt", "interface DemoStore { fun load(): Int }\n")
        self.write_catalog(self.full_entry("DemoStore", corruption="''"))
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(1, len(errors))
        self.assertIn("corruption", errors[0])

    def test_adapter_requires_known_implements(self):
        self.write_kt(
            "Demo.kt",
            "interface DemoStore { fun load(): Int }\n"
            "class FileDemoStore : DemoStore { override fun load() = 1 }\n",
        )
        self.write_catalog(
            self.full_entry("DemoStore")
            + self.full_entry("FileDemoStore", kind="adapter", implements="NoSuchSeam")
        )
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(1, len(errors))
        self.assertIn("NoSuchSeam", errors[0])

    def test_prefs_adapter_requires_prefs_name(self):
        self.write_kt(
            "Demo.kt",
            "interface DemoStore { fun load(): Int }\n"
            'class SharedPreferencesDemoStore : DemoStore { override fun load() = 1 }\n',
        )
        self.write_catalog(
            self.full_entry("DemoStore")
            + self.full_entry("SharedPreferencesDemoStore", kind="adapter", implements="DemoStore")
        )
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(1, len(errors))
        self.assertIn("prefs_name", errors[0])

    def test_prefs_name_parity_is_bidirectional(self):
        self.write_kt(
            "Demo.kt",
            "interface DemoStore { fun load(): Int }\n"
            'class SharedPreferencesDemoStore : DemoStore {\n'
            '    val p = getSharedPreferences("inweb_demo", 1)\n'
            "    override fun load() = 1\n"
            "}\n",
        )
        # catalog declares a DIFFERENT prefs name than the code uses
        self.write_catalog(
            self.full_entry("DemoStore")
            + self.full_entry(
                "SharedPreferencesDemoStore",
                kind="adapter",
                implements="DemoStore",
                prefs_name="inweb_other",
            )
        )
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(2, len(errors))
        self.assertTrue(any("inweb_demo" in e for e in errors))
        self.assertTrue(any("inweb_other" in e for e in errors))

    def test_duplicate_surface_fails(self):
        self.write_kt("Demo.kt", "interface DemoStore { fun load(): Int }\n")
        self.write_catalog(self.full_entry("DemoStore") + self.full_entry("DemoStore"))
        errors, _ = vsi.validate(self.src, self.catalog)
        self.assertEqual(1, len(errors))
        self.assertIn("duplicate surface", errors[0])

    def test_missing_catalog_file_fails(self):
        errors, _ = vsi.validate(self.src, os.path.join(self._tmp.name, "missing.yaml"))
        self.assertEqual(1, len(errors))
        self.assertIn("catalog not found", errors[0])


if __name__ == "__main__":
    unittest.main()
