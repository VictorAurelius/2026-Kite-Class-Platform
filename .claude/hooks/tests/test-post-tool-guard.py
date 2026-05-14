#!/usr/bin/env python3
"""Tests for post-tool-guard.py — Wave 73 Bucket B Rules 6 + 7.

These tests focus on the regex/logic pure paths. The git-state-dependent paths
(check_status_csv_sync against actual HEAD diff) are smoke-tested for non-crash;
real validation happens in CI when the rules fire on synthetic gap diffs.
"""

import json
import subprocess
import unittest
from pathlib import Path

HOOK = Path(__file__).resolve().parent.parent / "post-tool-guard.py"


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
        # Must not be an error response
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


if __name__ == "__main__":
    unittest.main()
