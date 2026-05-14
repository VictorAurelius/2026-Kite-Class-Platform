#!/usr/bin/env python3
"""Tests for post-tool-guard.py - Wave 73 Bucket B Rules 6 + 7.

Subprocess-style tests smoke-check the entrypoint. In-process tests
monkey-patch _git so we can simulate arbitrary diff scenarios
deterministically without touching real git state.

Wave 74 Bucket C extended coverage:
- Rule 6 (check_status_csv_sync):
  * gap MD flipped Status, CSV NOT touched -> WARN
  * CSV touched but Status field NOT flipped -> silent (idempotent re-sync)
  * Multiple gap files flipped in one commit -> single combined WARN
- Rule 7 (check_release_retry_pattern):
  * 3 consecutive commits touching same workflow file -> WARN
  * Mixed gates across retries -> silent
"""

import importlib.util
import json
import subprocess
import sys
import unittest
from pathlib import Path

HOOK = Path(__file__).resolve().parent.parent / "post-tool-guard.py"


def _load_hook_module():
    """Import post-tool-guard.py as a module so we can monkey-patch _git."""
    spec = importlib.util.spec_from_file_location("post_tool_guard", HOOK)
    module = importlib.util.module_from_spec(spec)
    sys.modules["post_tool_guard"] = module
    spec.loader.exec_module(module)
    return module


def run_hook(payload: dict) -> dict:
    result = subprocess.run(
        ["python3", str(HOOK)],
        input=json.dumps(payload),
        capture_output=True, text=True, timeout=10,
    )
    if result.returncode != 0:
        return {"_error": result.stderr}
    try:
        return json.loads(result.stdout) if result.stdout.strip() else {}
    except json.JSONDecodeError:
        return {"_raw": result.stdout}


def is_silent(out: dict) -> bool:
    return "systemMessage" not in out


# Existing subprocess smoke tests (preserved)


class TestPostToolGuard(unittest.TestCase):
    def test_non_bash_silent(self):
        out = run_hook({"tool_name": "Read", "tool_input": {"file_path": "/foo"}})
        self.assertTrue(is_silent(out))

    def test_bash_unrelated_silent(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "ls -la"}})
        self.assertTrue(is_silent(out))

    def test_git_status_silent(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "git status"}})
        self.assertTrue(is_silent(out))

    def test_git_commit_smoke(self):
        # Smoke: git commit triggers code path; current HEAD may or may not have status flip,
        # but hook must not crash. Either silent OR systemMessage is valid.
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "git commit -m foo"}})
        self.assertTrue(isinstance(out, dict))
        self.assertNotIn("_error", out)

    def test_gh_pr_merge_smoke(self):
        out = run_hook({"tool_name": "Bash", "tool_input": {"command": "gh pr merge 1234 --squash"}})
        self.assertTrue(isinstance(out, dict))
        self.assertNotIn("_error", out)

    def test_empty_payload_silent(self):
        result = subprocess.run(
            ["python3", str(HOOK)],
            input="{}",
            capture_output=True, text=True, timeout=10,
        )
        out = json.loads(result.stdout) if result.stdout.strip() else {}
        self.assertTrue(is_silent(out))


# Wave 74 Bucket C: in-process tests with _git monkey-patch


class _GitStub:
    """Callable stub for post_tool_guard._git that returns canned responses by arg signature."""

    def __init__(self, responses: dict):
        self.responses = responses
        self.calls = []

    def __call__(self, *args, **kwargs):
        self.calls.append(args)
        if args in self.responses:
            return self.responses[args]
        for key, value in self.responses.items():
            if len(key) <= len(args) and args[: len(key)] == key:
                return value
        return ""


class TestStatusCsvSync(unittest.TestCase):
    """Rule 6: check_status_csv_sync()."""

    def setUp(self):
        self.mod = _load_hook_module()
        self._orig_git = self.mod._git

    def tearDown(self):
        self.mod._git = self._orig_git

    def test_gap_md_flipped_csv_not_touched_warns(self):
        """Gap MD has Status flip but CSV not in same diff -> WARN."""
        responses = {
            ("show", "--name-only", "--pretty=format:", "HEAD"):
                "documents/04-quality/gaps/GAP-999-test.md\n",
            ("show", "--unified=0", "HEAD", "--",
             "documents/04-quality/gaps/GAP-999-test.md"):
                "+**Status:** DONE\n",
            ("log", "-1", "--format=%B"): "feat: close GAP-999\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_status_csv_sync()
        self.assertTrue(msg, f"Expected warning, got empty: {msg!r}")
        self.assertIn("post-merge-sync-completeness", msg)
        self.assertIn("gap-status.csv", msg)

    def test_csv_touched_status_not_flipped_silent(self):
        """CSV touched but no '+**Status:**' line in diff -> silent (idempotent re-sync)."""
        responses = {
            ("show", "--name-only", "--pretty=format:", "HEAD"):
                "documents/04-quality/gaps/GAP-999-test.md\n",
            ("show", "--unified=0", "HEAD", "--",
             "documents/04-quality/gaps/GAP-999-test.md"):
                "+Some other line change\n-Old line\n",
            ("log", "-1", "--format=%B"): "docs: update GAP-999 narrative\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_status_csv_sync()
        self.assertFalse(msg, f"Expected silent, got warning: {msg!r}")

    def test_csv_co_touched_silent(self):
        """Gap MD + CSV touched in same commit -> silent."""
        responses = {
            ("show", "--name-only", "--pretty=format:", "HEAD"):
                "documents/04-quality/gaps/GAP-999-test.md\n"
                "documents/04-quality/gaps/gap-status.csv\n",
            ("log", "-1", "--format=%B"): "feat: close GAP-999\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_status_csv_sync()
        self.assertFalse(msg, f"Expected silent (CSV co-touched), got: {msg!r}")

    def test_multiple_gap_files_one_warning(self):
        """3 gap MDs flipped in one commit, no CSV -> single combined WARN."""
        responses = {
            ("show", "--name-only", "--pretty=format:", "HEAD"):
                "documents/04-quality/gaps/GAP-001-foo.md\n"
                "documents/04-quality/gaps/GAP-002-bar.md\n"
                "documents/04-quality/gaps/GAP-003-baz.md\n",
            ("show", "--unified=0", "HEAD", "--",
             "documents/04-quality/gaps/GAP-001-foo.md",
             "documents/04-quality/gaps/GAP-002-bar.md",
             "documents/04-quality/gaps/GAP-003-baz.md"):
                "+**Status:** DONE (GAP-001)\n"
                "+**Status:** DONE (GAP-002)\n"
                "+**Status:** DONE (GAP-003)\n",
            ("log", "-1", "--format=%B"): "feat: close 3 gaps\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_status_csv_sync()
        self.assertTrue(msg, "Expected single combined warning for 3 gaps")
        # check_status_csv_sync returns ONE string (not a list); 3 gap files share one consolidated message
        self.assertIsInstance(msg, str, "Multiple gap flips should produce ONE consolidated warning string")
        self.assertIn("gap-status.csv", msg)
        # Single warning template mentions rule name twice (header + .md path reference)
        self.assertEqual(msg.count("post-merge-sync-completeness"), 2,
                         "Warning template should mention rule name twice (header + path); presence proves single-call consolidated msg")

    def test_override_trailer_silent(self):
        """Status flip + no CSV but POST_MERGE_SYNC_OVERRIDE trailer -> silent."""
        responses = {
            ("show", "--name-only", "--pretty=format:", "HEAD"):
                "documents/04-quality/gaps/GAP-999-test.md\n",
            ("show", "--unified=0", "HEAD", "--",
             "documents/04-quality/gaps/GAP-999-test.md"):
                "+**Status:** DONE\n",
            ("log", "-1", "--format=%B"):
                "feat: close GAP-999\n\nPOST_MERGE_SYNC_OVERRIDE: CSV updated in PR #999\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_status_csv_sync()
        self.assertFalse(msg, f"Expected silent (override trailer), got: {msg!r}")


class TestReleaseRetryPattern(unittest.TestCase):
    """Rule 7: check_release_retry_pattern()."""

    def setUp(self):
        self.mod = _load_hook_module()
        self._orig_git = self.mod._git

    def tearDown(self):
        self.mod._git = self._orig_git

    def test_three_consecutive_same_workflow_warns(self):
        """3 commits all touching same workflow file -> WARN."""
        responses = {
            ("log", "-3", "--format=%H"): "sha3\nsha2\nsha1\n",
            ("show", "--name-only", "--pretty=format:", "sha3"):
                ".github/workflows/release.yml\nsome/other.txt\n",
            ("show", "--name-only", "--pretty=format:", "sha2"):
                ".github/workflows/release.yml\n",
            ("show", "--name-only", "--pretty=format:", "sha1"):
                ".github/workflows/release.yml\n",
            ("log", "-1", "--format=%B"): "fix: tweak release workflow\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_release_retry_pattern()
        self.assertTrue(msg, f"Expected warning, got: {msg!r}")
        self.assertIn("release-fix-retry-budget", msg)
        self.assertIn(".github/workflows/release.yml", msg)

    def test_three_consecutive_trivyignore_warns(self):
        """3 commits all touching .trivyignore -> WARN."""
        responses = {
            ("log", "-3", "--format=%H"): "sha3\nsha2\nsha1\n",
            ("show", "--name-only", "--pretty=format:", "sha3"): ".trivyignore\n",
            ("show", "--name-only", "--pretty=format:", "sha2"): ".trivyignore\nsrc/main.py\n",
            ("show", "--name-only", "--pretty=format:", "sha1"): ".trivyignore\n",
            ("log", "-1", "--format=%B"): "fix: add CVE-2026-XXX to trivyignore\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_release_retry_pattern()
        self.assertTrue(msg, f"Expected warning, got: {msg!r}")
        self.assertIn(".trivyignore", msg)

    def test_mixed_workflows_silent(self):
        """3 commits touching DIFFERENT workflow files -> no pattern, silent."""
        responses = {
            ("log", "-3", "--format=%H"): "sha3\nsha2\nsha1\n",
            ("show", "--name-only", "--pretty=format:", "sha3"):
                ".github/workflows/release.yml\n",
            ("show", "--name-only", "--pretty=format:", "sha2"):
                ".github/workflows/lighthouse.yml\n",
            ("show", "--name-only", "--pretty=format:", "sha1"):
                ".github/workflows/trivy.yml\n",
            ("log", "-1", "--format=%B"): "fix: tweak workflows\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_release_retry_pattern()
        self.assertFalse(msg, f"Expected silent (mixed gates), got: {msg!r}")

    def test_override_trailer_silent(self):
        """3 consecutive same-workflow commits BUT RELEASE_RETRY_TOOLING_FIXED trailer -> silent."""
        responses = {
            ("log", "-3", "--format=%H"): "sha3\nsha2\nsha1\n",
            ("show", "--name-only", "--pretty=format:", "sha3"):
                ".github/workflows/release.yml\n",
            ("show", "--name-only", "--pretty=format:", "sha2"):
                ".github/workflows/release.yml\n",
            ("show", "--name-only", "--pretty=format:", "sha1"):
                ".github/workflows/release.yml\n",
            ("log", "-1", "--format=%B"):
                "fix: retry after CloudWatch streaming added\n\n"
                "RELEASE_RETRY_TOOLING_FIXED: GAP-491 closed, observability now live\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_release_retry_pattern()
        self.assertFalse(msg, f"Expected silent (override trailer), got: {msg!r}")

    def test_fewer_than_three_commits_silent(self):
        """Repo with <3 commits -> cannot establish retry pattern -> silent."""
        responses = {
            ("log", "-3", "--format=%H"): "sha1\n",
            ("show", "--name-only", "--pretty=format:", "sha1"):
                ".github/workflows/release.yml\n",
        }
        self.mod._git = _GitStub(responses)
        msg = self.mod.check_release_retry_pattern()
        self.assertFalse(msg, f"Expected silent (<3 commits), got: {msg!r}")


if __name__ == "__main__":
    unittest.main()
