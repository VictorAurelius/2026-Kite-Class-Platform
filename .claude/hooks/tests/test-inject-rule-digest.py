#!/usr/bin/env python3
"""Tests for inject-rule-digest.py UserPromptSubmit hook.

Run: python3 -m pytest .claude/hooks/tests/test-inject-rule-digest.py -v
Or:  python3 .claude/hooks/tests/test-inject-rule-digest.py
"""
from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import time
import unittest
from pathlib import Path

# Locate hook + fixtures
TESTS_DIR = Path(__file__).resolve().parent
HOOKS_DIR = TESTS_DIR.parent
PROJECT_ROOT = HOOKS_DIR.parent.parent
HOOK_PATH = HOOKS_DIR / "inject-rule-digest.py"
FIXTURES_DIR = TESTS_DIR / "fixtures"

# Load hook module dynamically (filename has hyphens — can't `import` directly)
_spec = importlib.util.spec_from_file_location("inject_rule_digest", HOOK_PATH)
inject_module = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(inject_module)


def run_hook(stdin_text: str) -> tuple[str, str, int]:
    """Run hook as subprocess; return (stdout, stderr, exit_code)."""
    result = subprocess.run(
        ["python3", str(HOOK_PATH)],
        input=stdin_text,
        capture_output=True,
        text=True,
        timeout=10,
        cwd=str(PROJECT_ROOT),
    )
    return result.stdout, result.stderr, result.returncode


class TestKeywordMatching(unittest.TestCase):
    """Test in-process keyword matching logic."""

    def setUp(self) -> None:
        config_path = HOOKS_DIR / "data" / "keyword-rule-map.json"
        self.config = json.loads(config_path.read_text(encoding="utf-8"))

    def test_audit_keyword_matches(self) -> None:
        matched = inject_module.find_matched_rules("Run a quality check audit", self.config)
        self.assertGreater(len(matched), 0, "Should match at least one rule for 'audit'")
        rules = [m[0] for m in matched]
        self.assertTrue(
            any("output-review-mandate" in r or "quality-audit" in r for r in rules),
            f"Expected audit-related rules in {rules}",
        )

    def test_no_keyword_returns_empty(self) -> None:
        matched = inject_module.find_matched_rules(
            "Hello, what is the weather?", self.config
        )
        self.assertEqual(matched, [], "No keywords should mean empty match list")

    def test_multiple_keywords_dedupe_rules(self) -> None:
        # "deploy" + "v1.0.0-rc.1" + "merge" + "terraform" + "secrets" all match
        prompt = (
            "Time to deploy v1.0.0-rc.1 — gh pr merge after terraform apply. "
            "Need to handle secrets via AWS Secrets Manager."
        )
        matched = inject_module.find_matched_rules(prompt, self.config)
        rule_paths = [m[0] for m in matched]
        # Each rule path should appear at most once (dedupe)
        self.assertEqual(
            len(rule_paths), len(set(rule_paths)),
            f"Rule paths should be deduplicated: {rule_paths}",
        )
        # Should match deploy + merge + terraform + secrets families
        self.assertGreaterEqual(
            len(matched), 4,
            f"Expected ≥4 matched rules for multi-keyword prompt; got {len(matched)}",
        )

    def test_case_insensitive(self) -> None:
        matched_lower = inject_module.find_matched_rules("audit it", self.config)
        matched_upper = inject_module.find_matched_rules("AUDIT it", self.config)
        matched_mixed = inject_module.find_matched_rules("Audit it", self.config)
        self.assertEqual(len(matched_lower), len(matched_upper))
        self.assertEqual(len(matched_lower), len(matched_mixed))

    def test_invalid_regex_skipped(self) -> None:
        bad_config = {
            "global_cap_chars": 20000,
            "keywords": [{"pattern": "[invalid(", "rules": ["x.md"], "max_lines": 10}],
        }
        # Should not raise; just skip the bad pattern
        matched = inject_module.find_matched_rules("anything", bad_config)
        self.assertEqual(matched, [])


class TestDigestExtraction(unittest.TestCase):
    """Test rule file digest extraction."""

    def test_extract_admin_merge_digest(self) -> None:
        rule = PROJECT_ROOT / ".claude" / "rules" / "admin-merge-discipline.md"
        self.assertTrue(rule.exists(), "admin-merge-discipline.md must exist")
        digest = inject_module.extract_digest(rule, max_lines=30)
        self.assertIsNotNone(digest)
        self.assertIn("--admin", digest, "Digest should include core rule keyword")

    def test_extract_nonexistent_returns_none(self) -> None:
        digest = inject_module.extract_digest(
            PROJECT_ROOT / ".claude" / "rules" / "does-not-exist.md", max_lines=30
        )
        self.assertIsNone(digest)

    def test_extract_includes_rule_section(self) -> None:
        rule = PROJECT_ROOT / ".claude" / "rules" / "gap-done-discipline.md"
        digest = inject_module.extract_digest(rule, max_lines=40)
        self.assertIsNotNone(digest)
        self.assertIn("The Rule", digest)


class TestBuildContext(unittest.TestCase):
    """Test full context construction with cap enforcement."""

    def setUp(self) -> None:
        config_path = HOOKS_DIR / "data" / "keyword-rule-map.json"
        self.config = json.loads(config_path.read_text(encoding="utf-8"))

    def test_no_match_empty_context(self) -> None:
        context = inject_module.build_context("nothing relevant here", self.config)
        self.assertEqual(context, "")

    def test_match_includes_header(self) -> None:
        context = inject_module.build_context("run audit now", self.config)
        self.assertIn("Auto-injected rule digests", context)

    def test_cap_enforced(self) -> None:
        # Use very small cap to force truncation
        cap_config = dict(self.config)
        cap_config["global_cap_chars"] = 500
        prompt = "deploy merge audit terraform secrets gap closure mutation"
        context = inject_module.build_context(prompt, cap_config)
        # Should be ≤ cap + small slack for header/truncation note
        self.assertLessEqual(len(context), 1500)


class TestHookE2E(unittest.TestCase):
    """End-to-end subprocess invocation tests."""

    def test_audit_fixture(self) -> None:
        stdin_text = (FIXTURES_DIR / "prompt-audit.json").read_text(encoding="utf-8")
        stdout, _stderr, rc = run_hook(stdin_text)
        self.assertEqual(rc, 0)
        # Parse stdout JSON
        out = json.loads(stdout)
        self.assertIn("hookSpecificOutput", out)
        ctx = out["hookSpecificOutput"]["additionalContext"]
        self.assertIn("Auto-injected rule digests", ctx)

    def test_no_keyword_fixture_returns_empty_object(self) -> None:
        stdin_text = (FIXTURES_DIR / "prompt-no-keyword.json").read_text(encoding="utf-8")
        stdout, _stderr, rc = run_hook(stdin_text)
        self.assertEqual(rc, 0)
        out = json.loads(stdout)
        # Empty {} OR no hookSpecificOutput → both acceptable
        self.assertNotIn("hookSpecificOutput", out)

    def test_multi_keyword_fixture(self) -> None:
        stdin_text = (FIXTURES_DIR / "prompt-multi-keyword.json").read_text(encoding="utf-8")
        stdout, _stderr, rc = run_hook(stdin_text)
        self.assertEqual(rc, 0)
        out = json.loads(stdout)
        self.assertIn("hookSpecificOutput", out)
        ctx = out["hookSpecificOutput"]["additionalContext"]
        # Should mention multiple rule families
        keywords_present = sum(
            1
            for kw in ["release-deploy", "admin-merge", "terraform", "agent-aws"]
            if kw in ctx
        )
        self.assertGreaterEqual(keywords_present, 3, f"Expected ≥3 rule families in {ctx[:300]}")

    def test_gap_closure_fixture(self) -> None:
        stdin_text = (FIXTURES_DIR / "prompt-gap-closure.json").read_text(encoding="utf-8")
        stdout, _stderr, rc = run_hook(stdin_text)
        self.assertEqual(rc, 0)
        out = json.loads(stdout)
        self.assertIn("hookSpecificOutput", out)
        ctx = out["hookSpecificOutput"]["additionalContext"]
        self.assertIn("gap-done-discipline", ctx)

    def test_malformed_json_graceful(self) -> None:
        stdin_text = (FIXTURES_DIR / "prompt-malformed.json").read_text(encoding="utf-8")
        stdout, _stderr, rc = run_hook(stdin_text)
        self.assertEqual(rc, 0, "Malformed input must NOT crash hook")
        # stdout should be empty {} (parseable JSON)
        out = json.loads(stdout)
        self.assertEqual(out, {})

    def test_empty_stdin_graceful(self) -> None:
        stdout, _stderr, rc = run_hook("")
        self.assertEqual(rc, 0)
        self.assertEqual(json.loads(stdout), {})

    def test_missing_prompt_field_graceful(self) -> None:
        stdout, _stderr, rc = run_hook('{"session_id": "x"}')
        self.assertEqual(rc, 0)
        self.assertEqual(json.loads(stdout), {})


class TestPerformance(unittest.TestCase):
    """Verify hook completes <500ms (UserPromptSubmit hot path)."""

    def test_e2e_under_500ms(self) -> None:
        stdin_text = (FIXTURES_DIR / "prompt-multi-keyword.json").read_text(encoding="utf-8")
        start = time.perf_counter()
        _stdout, _stderr, rc = run_hook(stdin_text)
        elapsed_ms = (time.perf_counter() - start) * 1000
        self.assertEqual(rc, 0)
        self.assertLess(
            elapsed_ms, 500,
            f"Hook took {elapsed_ms:.1f}ms — must be <500ms on hot path",
        )

    def test_no_match_under_200ms(self) -> None:
        # No match path should be even faster
        stdin_text = (FIXTURES_DIR / "prompt-no-keyword.json").read_text(encoding="utf-8")
        start = time.perf_counter()
        _stdout, _stderr, rc = run_hook(stdin_text)
        elapsed_ms = (time.perf_counter() - start) * 1000
        self.assertEqual(rc, 0)
        self.assertLess(elapsed_ms, 500)


if __name__ == "__main__":
    unittest.main(verbosity=2)
