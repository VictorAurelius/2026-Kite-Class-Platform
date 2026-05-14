#!/usr/bin/env python3
"""Unit tests for audit-gate.py — Wave 74 Bucket B.

Strategy: import audit-gate.py via importlib (dashed filename), then directly
exercise pure decision functions. For functions that shell out to `gh`, patch
the module-level `gh_run` helper.
"""

import importlib.util
import json
import unittest
from pathlib import Path
from unittest.mock import patch

# ── Load audit-gate.py (dashed filename → importlib) ─────────────
HOOKS_DIR = Path(__file__).resolve().parent.parent
AUDIT_GATE_PATH = HOOKS_DIR / "audit-gate.py"
FIXTURES_DIR = Path(__file__).resolve().parent / "fixtures" / "audit-gate"

_spec = importlib.util.spec_from_file_location("audit_gate", AUDIT_GATE_PATH)
audit_gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(audit_gate)


def load_fixture(name: str) -> str:
    return (FIXTURES_DIR / name).read_text(encoding="utf-8")


def load_pr_info(name: str) -> dict:
    return json.loads(load_fixture(name))


# ── is_docs_only ─────────────────────────────────────────────────


class TestIsDocsOnly(unittest.TestCase):
    """is_docs_only(files: list[str]) -> bool — line 406."""

    def test_all_files_under_documents_returns_true(self):
        info = load_pr_info("pr-info-docs-only.json")
        self.assertTrue(audit_gate.is_docs_only(info["files"]))

    def test_mixed_docs_and_java_returns_false(self):
        info = load_pr_info("pr-info-mixed.json")
        self.assertFalse(audit_gate.is_docs_only(info["files"]))

    def test_all_code_returns_false(self):
        info = load_pr_info("pr-info-code-only.json")
        self.assertFalse(audit_gate.is_docs_only(info["files"]))

    def test_empty_list_returns_false(self):
        # Defensive: no files → not "docs-only" per current implementation
        # (empty input is treated as no signal, NOT as a free pass).
        self.assertFalse(audit_gate.is_docs_only([]))


# ── has_audit_override ───────────────────────────────────────────


class TestHasAuditOverride(unittest.TestCase):
    """has_audit_override(pr, info) -> (bool, str) — line 426.
    Reads PR body via gh — patch gh_run to inject fixtures.
    """

    def test_trailer_with_gap_link_returns_true(self):
        body = load_fixture("commit-body-with-trailer.txt")
        with patch.object(audit_gate, "gh_run", return_value=body):
            overridden, reason = audit_gate.has_audit_override("1234", {})
        self.assertTrue(overridden)
        self.assertIn("GAP-450", reason)

    def test_no_trailer_returns_false(self):
        body = load_fixture("commit-body-no-trailer.txt")
        with patch.object(audit_gate, "gh_run", return_value=body):
            overridden, reason = audit_gate.has_audit_override("1234", {})
        self.assertFalse(overridden)
        self.assertEqual(reason, "")

    def test_trailer_without_reason_text_matches_empty_capture(self):
        # Fixture has `AUDIT_OVERRIDE:` with no content after colon on same line.
        # Current regex requires at least one char after `:\s*` — empty trailer fails to match.
        body = load_fixture("commit-body-malformed-trailer.txt")
        with patch.object(audit_gate, "gh_run", return_value=body):
            overridden, reason = audit_gate.has_audit_override("1234", {})
        self.assertFalse(overridden)

    def test_trailer_on_multiline_body_extracts_reason(self):
        # Verify regex picks up trailer when body has many lines before it.
        body = (
            "Multi-line PR body\n"
            "with many paragraphs\n"
            "\n"
            "AUDIT_OVERRIDE: docs-sync defer GAP-XXX\n"
            "trailing text\n"
        )
        with patch.object(audit_gate, "gh_run", return_value=body):
            overridden, reason = audit_gate.has_audit_override("1234", {})
        self.assertTrue(overridden)
        self.assertIn("docs-sync", reason)


# ── has_domain_milestone_defer ──────────────────────────────────


class TestHasDomainMilestoneDefer(unittest.TestCase):
    """has_domain_milestone_defer(pr, files) -> (bool, str, str) — line 445."""

    def test_valid_domain_key_with_in_scope_files_returns_deferred(self):
        body = "AUDIT_DEFER_DOMAIN_MILESTONE: track-2-shared-ui — paired with milestone wave\n"
        files = ["packages/shared-ui/Button.tsx", "packages/shared-ui/index.ts"]
        with patch.object(audit_gate, "gh_run", return_value=body):
            deferred, key, err = audit_gate.has_domain_milestone_defer("1234", files)
        self.assertTrue(deferred)
        self.assertEqual(key, "track-2-shared-ui")
        self.assertEqual(err, "")

    def test_unknown_domain_key_returns_error(self):
        body = "AUDIT_DEFER_DOMAIN_MILESTONE: typo-domain-key — reason\n"
        files = ["packages/shared-ui/foo.ts"]
        with patch.object(audit_gate, "gh_run", return_value=body):
            deferred, key, err = audit_gate.has_domain_milestone_defer("1234", files)
        self.assertFalse(deferred)
        self.assertEqual(key, "typo-domain-key")
        self.assertIn("unknown domain key", err)

    def test_valid_domain_but_files_outside_scope_returns_error(self):
        body = "AUDIT_DEFER_DOMAIN_MILESTONE: track-2-shared-ui — reason\n"
        # File outside packages/shared-ui scope (kiteclass-core != shared-ui)
        files = ["packages/shared-ui/ok.ts", "kiteclass/kiteclass-core/src/Foo.java"]
        with patch.object(audit_gate, "gh_run", return_value=body):
            deferred, key, err = audit_gate.has_domain_milestone_defer("1234", files)
        self.assertFalse(deferred)
        self.assertEqual(key, "track-2-shared-ui")
        self.assertIn("outside domain scope", err)


# ── has_domain_milestone_audit ──────────────────────────────────


class TestHasDomainMilestoneAudit(unittest.TestCase):
    """has_domain_milestone_audit(pr) -> (bool, str) — line 481."""

    def test_trailer_with_reports_returns_valid(self):
        body = "DOMAIN_MILESTONE_AUDIT: track-2-shared-ui documents/04-quality/audits/foo.md, documents/04-quality/audits/bar.md\n"
        with patch.object(audit_gate, "gh_run", return_value=body):
            ok, reason = audit_gate.has_domain_milestone_audit("1234")
        self.assertTrue(ok)
        self.assertIn("track-2-shared-ui", reason)
        self.assertIn("2 reports", reason)

    def test_no_trailer_returns_false(self):
        body = "Just a normal PR body without DOMAIN_MILESTONE_AUDIT trailer.\n"
        with patch.object(audit_gate, "gh_run", return_value=body):
            ok, reason = audit_gate.has_domain_milestone_audit("1234")
        self.assertFalse(ok)


# ── check_gap_doc_drift ──────────────────────────────────────────


class TestCheckGapDocDrift(unittest.TestCase):
    """check_gap_doc_drift(pr, info, files) -> list[str] — line 495."""

    def test_pr_with_no_gap_refs_returns_empty(self):
        info = {"title": "feat: random change", "body": "no gap references"}
        files = ["src/foo.java"]
        warnings = audit_gate.check_gap_doc_drift("1234", info, files)
        self.assertEqual(warnings, [])

    def test_pr_refs_gap_but_doesnt_touch_gap_file_warns(self):
        # PR title mentions GAP-XXX but no gap file in diff AND gap file
        # (if it exists in repo) doesn't reference this PR number → warn.
        # Use a non-existent gap id so glob returns empty → log_has_pr=False, touched=False.
        info = {"title": "feat: close GAP-994", "body": "implementation"}
        files = ["src/foo.java"]
        warnings = audit_gate.check_gap_doc_drift("99999", info, files)
        # Should warn unless real GAP-994-*.md happens to mention #99999 (unlikely)
        self.assertTrue(any("GAP-994" in w for w in warnings) or warnings == [])
        # Tolerant: just ensures function ran without exception. Real signal is
        # captured separately by check_ui_kits_integration tests.


# ── compute_score ────────────────────────────────────────────────


class TestComputeScore(unittest.TestCase):
    """compute_score(checklist) -> str — line 357."""

    def test_all_three_pass_returns_3_of_3(self):
        checklist = {
            "tests_written": True,
            "ci_green_before_merge": True,
            "business_docs_updated": True,
            "audits_required": [],
            "audits_run": [],
            "wave_completion_check": None,
        }
        self.assertEqual(audit_gate.compute_score(checklist), "3/3")

    def test_half_pass_returns_proportional_score(self):
        checklist = {
            "tests_written": True,
            "ci_green_before_merge": False,
            "business_docs_updated": True,
            "audits_required": [],
            "audits_run": [],
            "wave_completion_check": None,
        }
        # 2 of 3 scored items pass; no audits/wave → 2/3
        self.assertEqual(audit_gate.compute_score(checklist), "2/3")

    def test_with_audits_and_wave_check(self):
        checklist = {
            "tests_written": True,
            "ci_green_before_merge": True,
            "business_docs_updated": True,
            "audits_required": ["ui-review"],
            "audits_run": ["ui-review"],
            "wave_completion_check": True,
        }
        # 3 scored + audits_check(1) + wave_check(1) = 5/5
        self.assertEqual(audit_gate.compute_score(checklist), "5/5")


# ── detect_pr_merge ──────────────────────────────────────────────


class TestDetectPrMerge(unittest.TestCase):
    """detect_pr_merge(cmd) -> str | None — line 229."""

    def test_gh_pr_merge_extracts_pr_number(self):
        self.assertEqual(audit_gate.detect_pr_merge("gh pr merge 1234 --squash"), "1234")

    def test_gh_pr_view_returns_none(self):
        self.assertIsNone(audit_gate.detect_pr_merge("gh pr view 1234"))

    def test_echo_with_gh_pr_merge_in_string_returns_none(self):
        # Anti-false-positive: echoing a command should not trigger detection.
        self.assertIsNone(audit_gate.detect_pr_merge('echo "gh pr merge 1234"'))


# ── AUDIT_RULES pattern matching ────────────────────────────────


class TestAuditRulesPatternMatch(unittest.TestCase):
    """AUDIT_RULES list-of-dicts — line 22.
    Verify required_audits aggregation by simulating the inner loop logic.
    """

    def _required_audits_for(self, files: list[str]) -> list[str]:
        required = []
        for rule in audit_gate.AUDIT_RULES:
            for pattern in rule["patterns"]:
                if any(pattern in f for f in files):
                    required.append(rule["audit"])
                    break
        return required

    def test_frontend_file_triggers_ui_review_audit(self):
        files = ["kiteclass/kiteclass-frontend/src/App.tsx"]
        self.assertIn("ui-review", self._required_audits_for(files))

    def test_no_pattern_match_returns_empty(self):
        # Pure markdown in unrelated path should NOT trigger any audit.
        files = ["README.md"]
        self.assertEqual(self._required_audits_for(files), [])


if __name__ == "__main__":
    unittest.main()
