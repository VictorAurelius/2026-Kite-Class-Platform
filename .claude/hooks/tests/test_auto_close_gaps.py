#!/usr/bin/env python3
"""Unit tests for GAP-751 Option A — auto_close_referenced_gaps() trong audit-gate.py.

Strategy:
- Import audit-gate.py via importlib (dashed filename).
- Override module-level GAPS_DIR + GAP_STATUS_CSV to point at a temp scratch dir.
- Build synthetic gap file + CSV row fixtures.
- Exercise auto_close_referenced_gaps() across 5 scenarios:
  1. Closes: GAP-NNN → CSV flip OPEN→DONE + file moved to closed/ + Log appended
  2. Resolves: GAP-NNN → same as Closes
  3. Refs: GAP-NNN → last_verified bumped, status unchanged
  4. Closes: GAP-NNN already DONE → SKIP (idempotent)
  5. Closes: GAP-NNN no CSV row → WARN (soft-fail, no crash)
- Regex pattern unit test.

Run:
    python3 -m unittest .claude.hooks.tests.test_auto_close_gaps
    OR
    python3 .claude/hooks/tests/test_auto_close_gaps.py
"""

import importlib.util
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

# ── Load audit-gate.py via importlib (dashed filename) ──────────
HOOKS_DIR = Path(__file__).resolve().parent.parent
AUDIT_GATE_PATH = HOOKS_DIR / "audit-gate.py"

_spec = importlib.util.spec_from_file_location("audit_gate", AUDIT_GATE_PATH)
audit_gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(audit_gate)


CSV_HEADER = (
    "# Gap Status CSV — test fixture\n"
    "# Columns: id,filename,title_short,status,priority,domain,phase,"
    "completion_pct,found_date,last_verified,notes\n"
)


def _make_csv_line(
    gap_id: str,
    filename: str,
    status: str = "OPEN",
    priority: str = "P1",
    domain: str = "Meta",
    phase: str = "phase-1-beta",
    completion: str = "0",
    found: str = "2026-05-01",
    last_verified: str = "2026-05-01",
    notes: str = "synthetic fixture",
) -> str:
    return ",".join([gap_id, filename, "synthetic title", status, priority, domain, phase, completion, found, last_verified, notes])


def _make_gap_md(gap_id: str, title: str = "Synthetic gap fixture") -> str:
    return (
        f"---\n"
        f"title: {title}\n"
        f"status: 🔵 OPEN\n"
        f"priority: 🟠 P1\n"
        f"---\n\n"
        f"# {gap_id}: {title}\n\n"
        f"## Problem\nSynthetic.\n\n"
        f"## Acceptance Criteria\n- [ ] Synthetic AC\n\n"
        f"## Log\n\n"
        f"- **2026-05-01 (filed):** Test fixture.\n"
    )


class AutoCloseReferencedGapsTest(unittest.TestCase):
    """Test GAP-751 Option A auto-close mechanism end-to-end."""

    def setUp(self):
        # Scratch dir mirroring documents/04-quality/gaps/ layout
        self.scratch = Path(tempfile.mkdtemp(prefix="gap751-test-"))
        self.gaps_dir = self.scratch / "gaps"
        (self.gaps_dir / "phase-1-beta").mkdir(parents=True)
        (self.gaps_dir / "phase-1-beta" / "closed").mkdir(parents=True)
        self.csv = self.gaps_dir / "gap-status.csv"

        # Init scratch as a git repo so `git mv` works
        subprocess.run(["git", "init", "-q"], cwd=self.scratch, check=True)
        subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=self.scratch, check=True)
        subprocess.run(["git", "config", "user.name", "Test"], cwd=self.scratch, check=True)

        # Override module-level paths
        self._orig_gaps_dir = audit_gate.GAPS_DIR
        self._orig_csv = audit_gate.GAP_STATUS_CSV
        self._orig_project_root = audit_gate.PROJECT_ROOT
        audit_gate.GAPS_DIR = self.gaps_dir
        audit_gate.GAP_STATUS_CSV = self.csv
        audit_gate.PROJECT_ROOT = self.scratch

    def tearDown(self):
        audit_gate.GAPS_DIR = self._orig_gaps_dir
        audit_gate.GAP_STATUS_CSV = self._orig_csv
        audit_gate.PROJECT_ROOT = self._orig_project_root
        shutil.rmtree(self.scratch, ignore_errors=True)

    def _write_fixture_gap(
        self,
        gap_id: str,
        slug: str,
        status: str = "OPEN",
        completion: str = "0",
        sub: str = "phase-1-beta",
    ) -> Path:
        """Write a synthetic gap markdown file + CSV row. Returns gap file path."""
        rel_path = f"{sub}/{gap_id}-{slug}.md"
        gap_file = self.gaps_dir / rel_path
        gap_file.parent.mkdir(parents=True, exist_ok=True)
        gap_file.write_text(_make_gap_md(gap_id), encoding="utf-8")
        # Write/append CSV
        line = _make_csv_line(gap_id, rel_path, status=status, completion=completion)
        if not self.csv.exists():
            self.csv.write_text(CSV_HEADER + line + "\n", encoding="utf-8")
        else:
            existing = self.csv.read_text(encoding="utf-8").rstrip()
            self.csv.write_text(existing + "\n" + line + "\n", encoding="utf-8")
        # git add so `git mv` doesn't fail with "not under version control"
        subprocess.run(["git", "add", str(gap_file), str(self.csv)], cwd=self.scratch, check=True)
        subprocess.run(["git", "commit", "-q", "-m", f"fixture {gap_id}"], cwd=self.scratch, check=True)
        return gap_file

    # ── Regex pattern test ────────────────────────────────────────

    def test_regex_matches_all_three_marker_types(self):
        body = (
            "Summary line.\n\n"
            "Closes: GAP-997\n"
            "Resolves: GAP-998\n"
            "Refs: GAP-999\n"
            "Closes:GAP-1000\n"  # no space after colon — still match
            "closes:gap-1001\n"  # lowercase variant
        )
        matches = audit_gate.GAP_MARKER_RE.findall(body)
        # Lowercase variant won't match GAP- (case-insensitive on verb but GAP-NNN
        # capitalised in regex). Verify Closes/Resolves/Refs all caught.
        marker_types = {m[0].lower() for m in matches}
        gap_ids = {m[1].upper() for m in matches}
        self.assertIn("closes", marker_types)
        self.assertIn("resolves", marker_types)
        self.assertIn("refs", marker_types)
        self.assertIn("GAP-997", gap_ids)
        self.assertIn("GAP-998", gap_ids)
        self.assertIn("GAP-999", gap_ids)

    # ── Scenario 1: Closes: GAP-NNN OPEN→DONE ─────────────────────

    def test_closes_marker_flips_open_to_done_and_moves_file(self):
        gap_file = self._write_fixture_gap("GAP-997", "synthetic-open")
        self.assertTrue(gap_file.exists())

        results = audit_gate.auto_close_referenced_gaps("9999", "Closes: GAP-997")

        # Verify result line
        self.assertEqual(len(results), 1)
        self.assertIn("FLIPPED", results[0])
        self.assertIn("GAP-997", results[0])
        self.assertIn("OPEN→DONE", results[0])

        # Verify CSV row updated
        row = audit_gate._find_csv_row("GAP-997")
        self.assertIsNotNone(row)
        _, fields = row
        self.assertEqual(fields[3], "DONE")
        self.assertEqual(fields[7], "100")
        # Filename column should now point at phase-1-beta/closed/
        self.assertTrue(fields[1].startswith("phase-1-beta/closed/"))

        # Verify file moved to closed/
        moved = self.gaps_dir / "phase-1-beta" / "closed" / "GAP-997-synthetic-open.md"
        self.assertTrue(moved.exists())
        self.assertFalse(gap_file.exists())

        # Verify Log entry appended
        content = moved.read_text(encoding="utf-8")
        self.assertIn("PR #9999 auto-close", content)
        self.assertIn("Flipped DONE 100%", content)

    # ── Scenario 2: Resolves: GAP-NNN ─────────────────────────────

    def test_resolves_marker_same_as_closes(self):
        self._write_fixture_gap("GAP-998", "synthetic-resolves")
        results = audit_gate.auto_close_referenced_gaps("9998", "Resolves: GAP-998")
        self.assertEqual(len(results), 1)
        self.assertIn("FLIPPED", results[0])
        row = audit_gate._find_csv_row("GAP-998")
        self.assertEqual(row[1][3], "DONE")

    # ── Scenario 3: Refs: GAP-NNN ────────────────────────────────

    def test_refs_marker_bumps_last_verified_only_no_status_flip(self):
        self._write_fixture_gap("GAP-996", "synthetic-refs", status="OPEN")
        results = audit_gate.auto_close_referenced_gaps("9996", "Refs: GAP-996")
        self.assertEqual(len(results), 1)
        self.assertIn("VERIFIED", results[0])
        row = audit_gate._find_csv_row("GAP-996")
        self.assertIsNotNone(row)
        _, fields = row
        # Status MUST remain OPEN
        self.assertEqual(fields[3], "OPEN")
        # File MUST stay at original location (not moved to closed/)
        self.assertTrue(fields[1].startswith("phase-1-beta/GAP-996"))
        # File should still be at active path, not closed/
        self.assertTrue((self.gaps_dir / fields[1]).exists())

    # ── Scenario 4: Already DONE — idempotent skip ─────────────────

    def test_closes_marker_on_already_done_gap_is_skipped(self):
        # Pre-create gap directly under closed/ with status=DONE
        gap_file = self.gaps_dir / "phase-1-beta" / "closed" / "GAP-995-already-done.md"
        gap_file.write_text(_make_gap_md("GAP-995"), encoding="utf-8")
        line = _make_csv_line("GAP-995", "phase-1-beta/closed/GAP-995-already-done.md", status="DONE", completion="100")
        if not self.csv.exists():
            self.csv.write_text(CSV_HEADER + line + "\n", encoding="utf-8")
        else:
            self.csv.write_text(self.csv.read_text() + line + "\n", encoding="utf-8")

        results = audit_gate.auto_close_referenced_gaps("9995", "Closes: GAP-995")
        self.assertEqual(len(results), 1)
        self.assertIn("SKIP", results[0])
        self.assertIn("already DONE", results[0])

    # ── Scenario 5: No CSV row — soft-fail WARN ───────────────────

    def test_closes_marker_no_csv_row_soft_fails_with_warn(self):
        # CSV is empty (no row for GAP-994)
        if not self.csv.exists():
            self.csv.write_text(CSV_HEADER, encoding="utf-8")
        results = audit_gate.auto_close_referenced_gaps("9994", "Closes: GAP-994")
        self.assertEqual(len(results), 1)
        self.assertIn("WARN", results[0])
        self.assertIn("no CSV row", results[0])

    # ── Scenario 6: Empty / no-marker PR body ─────────────────────

    def test_empty_pr_body_returns_empty_list(self):
        self.assertEqual(audit_gate.auto_close_referenced_gaps("9993", ""), [])

    def test_pr_body_without_markers_returns_empty_list(self):
        body = "## Summary\n\nThis PR adds a new feature.\n\nNo gap references."
        self.assertEqual(audit_gate.auto_close_referenced_gaps("9992", body), [])

    # ── Scenario 7: Idempotency — re-run no double-append ─────────

    def test_re_running_closes_does_not_double_append_log(self):
        self._write_fixture_gap("GAP-993", "synthetic-idempotent")
        body = "Closes: GAP-993"
        results1 = audit_gate.auto_close_referenced_gaps("9991", body)
        self.assertIn("FLIPPED", results1[0])
        # Second run on same PR — file already DONE per CSV, expect SKIP
        results2 = audit_gate.auto_close_referenced_gaps("9991", body)
        self.assertIn("SKIP", results2[0])
        # Verify Log entry only appears once in file
        moved = self.gaps_dir / "phase-1-beta" / "closed" / "GAP-993-synthetic-idempotent.md"
        content = moved.read_text(encoding="utf-8")
        self.assertEqual(content.count("PR #9991 auto-close"), 1)

    # ── Scenario 8: Multiple markers — deduped + processed ───────

    def test_multiple_markers_processed_in_order(self):
        self._write_fixture_gap("GAP-990", "multi-a")
        self._write_fixture_gap("GAP-991", "multi-b")
        body = (
            "## Summary\n\n"
            "Closes: GAP-990\n"
            "Closes: GAP-990\n"  # duplicate — deduped
            "Refs: GAP-991\n"
        )
        results = audit_gate.auto_close_referenced_gaps("9990", body)
        # Expect 2 distinct results (dedupe removes duplicate Closes:GAP-990)
        self.assertEqual(len(results), 2)
        flipped = [r for r in results if "FLIPPED" in r]
        verified = [r for r in results if "VERIFIED" in r]
        self.assertEqual(len(flipped), 1)
        self.assertEqual(len(verified), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
