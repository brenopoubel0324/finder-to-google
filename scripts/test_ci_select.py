import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from ci_select import classify, select


class SelectionTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = Path(self.tmp.name) / "repo"
        self.repo.mkdir()
        self.git("init", "-b", "main")
        self.git("config", "user.name", "CI test")
        self.git("config", "user.email", "ci@example.invalid")
        self.write("README.md", "base")
        self.write("app/src/main/java/br/com/findertogoogle/routing/LinkRouter.kt", "base")
        self.base = self.commit()

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.repo), *args], stderr=subprocess.DEVNULL).decode().strip()

    def write(self, name, text):
        path = self.repo / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)

    def commit(self):
        self.git("add", ".")
        self.git("commit", "-m", "fixture")
        return self.git("rev-parse", "HEAD")

    def push(self, base=None):
        return select(self.repo, "push", {"before": base or self.base, "ref": "refs/heads/main"}, self.git("rev-parse", "HEAD"))

    def test_docs_and_daily(self):
        self.write("README.md", "edit")
        self.commit()
        self.assertEqual(self.push()[0], "docs")
        self.write("app/src/main/res/layout/page_home.xml", "layout")
        self.commit()
        self.assertEqual(self.push()[0], "daily")

    def test_whole_push_not_just_last_commit(self):
        self.write("app/src/main/java/br/com/findertogoogle/routing/LinkRouter.kt", "critical")
        self.commit()
        self.write("README.md", "only last commit is docs")
        self.commit()
        self.assertEqual(self.push()[0], "complete")

    def test_deletion_and_rename_include_original_path(self):
        old = "app/src/main/java/br/com/findertogoogle/routing/LinkRouter.kt"
        (self.repo / old).rename(self.repo / "moved.md")
        self.commit()
        self.assertEqual(self.push()[0], "complete")
        self.assertEqual(self.push()[2], 2)

    def test_deleted_docs_and_filename_with_newline(self):
        (self.repo / "README.md").unlink()
        self.write("docs/a\nb.md", "doc")
        self.commit()
        self.assertEqual(self.push(), ("docs", "documentation_only", 2))

    def test_unknown_paths_assets_and_security_always_complete(self):
        for path in ("app/src/main/AndroidManifest.xml", "app/src/main/assets/data.json", "docs/data.json",
                     "app/src/main/res/xml/backup_rules.xml", "app/build.gradle.kts", "app/proguard-rules.pro",
                     "scripts/ci_select.py", ".github/workflows/android.yml", "app/src/androidTest/Test.kt",
                     "app/src/main/java/br/com/findertogoogle/ui/SettingsTransferUi.kt", "downloads/file.apk", "new.file"):
            with self.subTest(path=path):
                self.assertEqual(classify([("M", path)])[0], "complete")
        self.assertEqual(classify([("T", "README.md")])[0], "complete")

    def test_missing_base_new_branch_and_head_mismatch(self):
        self.assertEqual(self.push("0" * 40)[0], "complete")
        self.assertEqual(self.push("f" * 40)[:2], ("complete", "unavailable_diff"))
        self.assertEqual(select(self.repo, "push", {"before": self.base}, "f" * 40)[:2], ("complete", "head_mismatch"))
        self.assertEqual(self.push()[0], "complete")  # Empty diff is not docs-only.

    def test_manual_and_tag_always_complete(self):
        for name, event in (("workflow_dispatch", {}), ("push", {"ref": "refs/tags/v1.2.0"})):
            self.assertEqual(select(self.repo, name, event, self.base)[0], "complete")

    def test_pr_uses_checked_out_merge_and_base(self):
        self.git("checkout", "-b", "feature")
        self.write("app/src/main/res/layout/page_home.xml", "feature")
        feature = self.commit()
        self.git("checkout", "main")
        self.write("README.md", "new integration base")
        base = self.commit()
        self.git("merge", "--no-ff", "feature", "-m", "merge fixture")
        event = {"pull_request": {"base": {"sha": base}, "head": {"sha": feature}}}
        self.assertEqual(select(self.repo, "pull_request", event, self.git("rev-parse", "HEAD")), ("daily", "application_change", 1))

    def test_shallow_checkout_fetches_only_needed_base(self):
        self.write("README.md", "edit")
        head = self.commit()
        clone = Path(self.tmp.name) / "shallow"
        subprocess.run(["git", "clone", "--depth=1", self.repo.as_uri(), str(clone)], check=True, capture_output=True)
        self.assertEqual(select(clone, "push", {"before": self.base}, head)[0], "docs")

    def test_cli_outputs_and_bad_payload(self):
        self.write("README.md", "edit")
        head = self.commit()
        event = Path(self.tmp.name) / "event.json"
        event.write_text(json.dumps({"before": self.base}))
        output = Path(self.tmp.name) / "out"
        summary = Path(self.tmp.name) / "summary"
        env = dict(os.environ, GITHUB_OUTPUT=str(output), GITHUB_STEP_SUMMARY=str(summary))
        command = [sys.executable, str(Path(__file__).with_name("ci_select.py")), "--repo", str(self.repo),
                   "--event-name", "push", "--event-path", str(event), "--head", head]
        subprocess.run(command, check=True, capture_output=True, env=env)
        self.assertIn("mode=docs\n", output.read_text())
        self.assertIn("sem build Android", summary.read_text())
        event.write_text("invalid json")
        result = subprocess.run(command, check=True, capture_output=True, env=env)
        self.assertIn(b"mode=complete", result.stdout)


if __name__ == "__main__":
    unittest.main()
