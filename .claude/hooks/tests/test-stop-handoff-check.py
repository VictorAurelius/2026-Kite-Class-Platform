#!/usr/bin/env python3
"""Tests for stop-handoff-check.py - Wave 73 Bucket B Rule 8.

Wave 74 Bucket C extends coverage:
- DONE flip + §2 checklist verbatim -> silent
- DONE flip + user-facing keyword, no checklist -> WARN
- PARTIAL flip without checklist -> silent (only DONE flips need it)
- Multi-gap DONE flips -> checklist required (shared OR per-gap)
- Empty/missing transcript -> silent (fail-safe)
"""

import json
import subprocess
import tempfile
import unittest
from pathlib import Path

HOOK = Path(__file__).resolve().parent.parent / "stop-handoff-check.py"


def write_transcript(messages: list) -> str:
    """Write JSONL transcript with given assistant messages, return path."""
    with tempfile.NamedTemporaryFile(mode="w", suffix=".jsonl", delete=False) as fd:
        for msg in messages:
            fd.write(json.dumps({"role": "assistant", "content": [{"type": "text", "text": msg}]}) + "\n")
        return fd.name


def run_hook(transcript_path: str) -> dict:
    payload = {"transcript_path": transcript_path}
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


def is_warn(out: dict) -> bool:
    return "systemMessage" in out


# Existing 7 tests (preserved)


class TestStopHandoffCheck(unittest.TestCase):
    def test_done_with_user_facing_no_evidence_warns(self):
        path = write_transcript([
            "Admin dashboard URL works. Click approve button. GAP-518 flipped to 🟢 DONE today.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_warn(out), f"Expected warn, got {out}")
        self.assertIn("pre-handoff-self-test-completeness", out["systemMessage"])

    def test_done_with_evidence_silent(self):
        path = write_transcript([
            "Pre-handoff verify per §2.4: login flow works (admin@kitehub.me POST 200 + JWT), "
            "role-guard accepts PLATFORM_ADMIN, navigation path /admin/beta-requests visible. "
            "GAP-518 flipped DONE.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out), f"Expected silent, got {out}")

    def test_done_without_user_facing_silent(self):
        path = write_transcript([
            "Refactored backend service. Tests pass. GAP-XXX status DONE.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out))

    def test_no_done_claim_silent(self):
        path = write_transcript([
            "Investigating bug, login redirect issue. Will continue tomorrow.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out))

    def test_partial_override_silent(self):
        path = write_transcript([
            "Admin login dashboard verified live. PRE_HANDOFF_PARTIAL: browser walkthrough deferred to user.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out))

    def test_empty_transcript_silent(self):
        out = run_hook("/nonexistent/path.jsonl")
        self.assertTrue(is_silent(out))

    def test_empty_payload_silent(self):
        result = subprocess.run(
            ["python3", str(HOOK)],
            input="{}",
            capture_output=True, text=True, timeout=10,
        )
        out = json.loads(result.stdout) if result.stdout.strip() else {}
        self.assertTrue(is_silent(out))


# Wave 74 Bucket C: 5 new edge tests


class TestStopHandoffCheckEdge(unittest.TestCase):
    """Extended edge-case tests added Wave 74 Bucket C."""

    def test_done_verbatim_section_2_checklist_silent(self):
        """DONE flip + §2 explicit checklist verbatim -> silent."""
        path = write_transcript([
            "Closing GAP-600 with pre-handoff verify per §2.1 auth-gated flow:\n"
            "- credential available: aws secretsmanager get-secret-value --secret-id kite/test/admin\n"
            "- login flow: POST /api/v1/auth/login returns 200 + JWT\n"
            "- role-guard accepts admin → /admin URL\n"
            "- navigation path /admin/users in sidebar\n"
            "GAP-600 status: 🟢 DONE",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out), f"Expected silent (§2 evidence present), got: {out}")

    def test_done_with_user_facing_no_checklist_warns(self):
        """DONE flip + URL/dashboard keyword + NO checklist evidence -> WARN."""
        path = write_transcript([
            "Verified live admin dashboard at /admin/users URL. GAP-601 flipped to DONE this PR.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_warn(out), f"Expected warn, got: {out}")
        self.assertIn("pre-handoff-self-test-completeness", out["systemMessage"])

    def test_partial_flip_no_checklist_silent(self):
        """🟡 PARTIAL flip without §2 checklist -> silent (only DONE flips need checklist per gap-done-discipline.md)."""
        path = write_transcript([
            "GAP-602 status: 🟡 PARTIAL — admin button + login form work but dashboard data fetch deferred. "
            "Follow-up GAP-603 filed. Will revisit next wave.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out),
                        f"Expected silent (PARTIAL doesn't need pre-handoff checklist), got: {out}")

    def test_multi_gap_done_with_shared_checklist_silent(self):
        """Multiple gap DONE flips in single response + shared §2 checklist -> silent (shared evidence acceptable)."""
        path = write_transcript([
            "Pre-handoff verify per §2.2 anonymous flow (covers GAP-604, GAP-605, GAP-606 collectively):\n"
            "- URL entry: homepage link visible\n"
            "- form submit: POST /api/v1/signup returns 201\n"
            "- confirmation: success page renders + email arrives\n"
            "All three gaps share the signup form flow surface.\n"
            "GAP-604 flipped to DONE, GAP-605 flipped to DONE, GAP-606 flipped to DONE.",
        ])
        out = run_hook(path)
        Path(path).unlink()
        self.assertTrue(is_silent(out),
                        f"Expected silent (shared §2 checklist covers all 3), got: {out}")

    def test_missing_transcript_file_silent(self):
        """Path points to non-existent file -> silent (fail-safe degradation, exit 0)."""
        out = run_hook("/tmp/definitely-does-not-exist-12345.jsonl")
        self.assertTrue(is_silent(out),
                        f"Expected silent (missing file = fail-safe), got: {out}")

    def test_empty_jsonl_file_silent(self):
        """Empty JSONL file (0 bytes) -> silent (no text to scan)."""
        with tempfile.NamedTemporaryFile(mode="w", suffix=".jsonl", delete=False) as fd:
            empty_path = fd.name
        try:
            out = run_hook(empty_path)
            self.assertTrue(is_silent(out),
                            f"Expected silent (empty transcript), got: {out}")
        finally:
            Path(empty_path).unlink()


if __name__ == "__main__":
    unittest.main()
