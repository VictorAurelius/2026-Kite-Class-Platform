#!/usr/bin/env python3
"""Tests for stop-handoff-check.py — Wave 73 Bucket B Rule 8."""

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


if __name__ == "__main__":
    unittest.main()
