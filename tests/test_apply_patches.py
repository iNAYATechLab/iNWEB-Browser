"""Unit tests for scripts/apply_patches.py — real git-based fixtures.

Each test builds a synthetic "Chromium-like" tree and real unified diffs
(produced by git itself), then exercises the patch-series tooling exactly as
the build pipeline will use it.
"""
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO_ROOT, "scripts"))

import apply_patches  # noqa: E402


def run_git(cwd, *args):
    subprocess.run(
        ["git", *args], cwd=cwd, check=True,
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )


def make_patch_text(relpath, old, new):
    """Produce a unified diff (a/ b/ prefixes) that turns `old` into `new`."""
    repo = tempfile.mkdtemp(prefix="inweb-patchsrc-")
    try:
        run_git(repo, "init", "-q")
        run_git(repo, "config", "user.email", "dev@inweb.invalid")
        run_git(repo, "config", "user.name", "iNWEB Test")
        target = os.path.join(repo, relpath)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8") as fh:
            fh.write(old)
        run_git(repo, "add", "-A")
        run_git(repo, "commit", "-qm", "base")
        with open(target, "w", encoding="utf-8") as fh:
            fh.write(new)
        return subprocess.run(
            ["git", "diff"], cwd=repo, capture_output=True, text=True, check=True,
        ).stdout
    finally:
        shutil.rmtree(repo, ignore_errors=True)


def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(content)


class PatchSeriesTestCase(unittest.TestCase):
    """End-to-end behaviour of apply_pending / verify_series / hash_tree."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="inweb-test-")
        self.tree = os.path.join(self.tmp, "tree")
        self.target = os.path.join(self.tree, "chrome", "browser", "hello.txt")
        write_file(self.target, "alpha-v1\n")
        self.patches = os.path.join(self.tmp, "patches")
        self.manifest = os.path.join(self.patches, "MANIFEST.yaml")

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def register(self, entries):
        lines = ["schema_version: 1", "patches:"]
        for e in entries:
            lines.append(f'  - id: "{e["id"]}"')
            lines.append(f'    file: "{e["file"]}"')
            lines.append(f"    area: {e['area']}")
            lines.append(f'    description: "{e["description"]}"')
        write_file(self.manifest, "\n".join(lines) + "\n")

    def make_series(self):
        """Two chained patches: v1 -> v2 -> v3 on the same file."""
        p1 = make_patch_text("chrome/browser/hello.txt", "alpha-v1\n", "alpha-v2\n")
        p2 = make_patch_text("chrome/browser/hello.txt", "alpha-v2\n", "alpha-v3\n")
        write_file(os.path.join(self.patches, "privacy", "0001-demo-one.patch"), p1)
        write_file(os.path.join(self.patches, "privacy", "0002-demo-two.patch"), p2)
        self.register([
            {"id": "0001-demo-one", "file": "privacy/0001-demo-one.patch",
             "area": "privacy", "description": "first change"},
            {"id": "0002-demo-two", "file": "privacy/0002-demo-two.patch",
             "area": "privacy", "description": "second change"},
        ])

    def read_target(self):
        with open(self.target, "r", encoding="utf-8") as fh:
            return fh.read()

    def test_apply_chained_series_in_order(self):
        self.make_series()
        was_applied, failed = apply_patches.apply_pending(self.tree, self.manifest)
        self.assertIsNone(failed)
        self.assertEqual([], was_applied)
        self.assertEqual(["0001-demo-one", "0002-demo-two"],
                         ["0001-demo-one", "0002-demo-two"])  # series consumed in order
        self.assertEqual("alpha-v3\n", self.read_target())

    def test_apply_is_idempotent(self):
        self.make_series()
        apply_patches.apply_pending(self.tree, self.manifest)
        was_applied, failed = apply_patches.apply_pending(self.tree, self.manifest)
        self.assertIsNone(failed)
        self.assertEqual(["0001-demo-one", "0002-demo-two"], was_applied)
        self.assertEqual("alpha-v3\n", self.read_target())

    def test_verify_series(self):
        self.make_series()
        self.assertFalse(apply_patches.verify_series(self.tree, self.manifest))
        apply_patches.apply_pending(self.tree, self.manifest)
        self.assertTrue(apply_patches.verify_series(self.tree, self.manifest))

    def test_conflict_aborts_with_failed_entry(self):
        # Registry contains only the second (v2 -> v3) patch while the tree is
        # still at v1: forward check fails, so the series must abort.
        p2 = make_patch_text("chrome/browser/hello.txt", "alpha-v2\n", "alpha-v3\n")
        write_file(os.path.join(self.patches, "privacy", "0002-demo-two.patch"), p2)
        self.register([
            {"id": "0002-demo-two", "file": "privacy/0002-demo-two.patch",
             "area": "privacy", "description": "second change"},
        ])
        was_applied, failed = apply_patches.apply_pending(self.tree, self.manifest)
        self.assertEqual([], was_applied)
        self.assertIsNotNone(failed)
        self.assertEqual("0002-demo-two", failed["id"])
        self.assertEqual("alpha-v1\n", self.read_target())

    def test_missing_patch_file_aborts(self):
        self.register([
            {"id": "0001-nope", "file": "privacy/0001-nope.patch",
             "area": "privacy", "description": "missing on disk"},
        ])
        was_applied, failed = apply_patches.apply_pending(self.tree, self.manifest)
        self.assertEqual([], was_applied)
        self.assertIsNotNone(failed)

    def test_empty_series_is_valid(self):
        self.register([])
        was_applied, failed = apply_patches.apply_pending(self.tree, self.manifest)
        self.assertIsNone(failed)
        self.assertEqual([], was_applied)
        self.assertTrue(apply_patches.verify_series(self.tree, self.manifest))

    def test_hash_tree_deterministic_and_ignores_git(self):
        a = os.path.join(self.tmp, "tree-a")
        b = os.path.join(self.tmp, "tree-b")
        write_file(os.path.join(a, "x.txt"), "same\n")
        write_file(os.path.join(b, "x.txt"), "same\n")
        run_git(a, "init", "-q")  # a has VCS metadata that must be ignored
        h_a = apply_patches.hash_tree(a)
        h_b = apply_patches.hash_tree(b)
        self.assertEqual(h_a, h_b)
        write_file(os.path.join(b, "x.txt"), "changed\n")
        self.assertNotEqual(h_a, apply_patches.hash_tree(b))


if __name__ == "__main__":
    unittest.main()
