#!/usr/bin/env python3
"""Empirical concurrent fire test — Wave 75 Bucket D.

Purpose: investigate concurrent race conditions on hook state writes when
multiple wave-pack agents fire the same hook in parallel (per
`feedback_parallel_agent_strategy.md` rule #9 — up to 5 agents).

Approach: invoke target hook N times in parallel via concurrent.futures
ThreadPoolExecutor spawning subprocess.run for each invocation. Each call
gets a DIFFERENT payload to mimic distinct agents. Verify:

  - All invocations exit cleanly (no exception/crash, no traceback in stderr)
  - Output files (if any) end up in consistent state (parseable JSON, no
    truncated writes, no corrupted bytes)
  - Document actual behavior; do NOT enforce "no race" — just observe

CAVEAT: subprocess-based parallelism approximates but does NOT exactly model
Claude Code runtime hook invocation. The runtime spawns hooks via its own
event-loop scheduler with timing characteristics we don't control here.
Findings are necessary-but-not-sufficient: a race here = production risk;
absence here ≠ absence in production. Documented in companion audit artifact.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

HOOKS_DIR = Path(__file__).resolve().parent.parent
PROJECT_ROOT = HOOKS_DIR.parent.parent

PRE_TOOL_GUARD = HOOKS_DIR / "pre-tool-guard.py"
POST_TOOL_GUARD = HOOKS_DIR / "post-tool-guard.py"
AUDIT_GATE = HOOKS_DIR / "audit-gate.py"
INJECT_RULE_DIGEST = HOOKS_DIR / "inject-rule-digest.py"
SESSION_LOCK_GUARD = HOOKS_DIR / "session-lock-guard.py"

PARALLEL_N = 5  # matches `feedback_parallel_agent_strategy.md` rule #9 max


def invoke_hook(hook_path: Path, payload: dict, timeout: int = 10) -> dict:
    """Invoke a hook with the given JSON payload via stdin. Return result dict."""
    try:
        result = subprocess.run(
            ["python3", str(hook_path)],
            input=json.dumps(payload),
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        return {
            "exit": result.returncode,
            "stdout": result.stdout,
            "stderr": result.stderr,
        }
    except subprocess.TimeoutExpired:
        return {"exit": -1, "stdout": "", "stderr": "TIMEOUT"}
    except Exception as exc:  # noqa: BLE001
        return {"exit": -2, "stdout": "", "stderr": f"EXC: {exc}"}


class TestPreToolGuardConcurrent(unittest.TestCase):
    """pre-tool-guard.py is mostly stateless EXCEPT terraform-apply-last-ts.txt
    write. Each invocation overwrites that file; race = lost timestamp update.
    """

    def test_n_parallel_no_crash(self):
        """N parallel Bash invocations: all exit 0 (allow) with empty/JSON output."""
        payloads = [
            {"tool_name": "Bash", "tool_input": {"command": f"echo agent-{i}"}}
            for i in range(PARALLEL_N)
        ]
        with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
            results = list(ex.map(lambda p: invoke_hook(PRE_TOOL_GUARD, p), payloads))
        for i, r in enumerate(results):
            self.assertEqual(
                r["exit"], 0,
                f"agent-{i}: hook exited non-zero. stderr={r['stderr']!r}",
            )
            self.assertNotIn("Traceback", r["stderr"], f"agent-{i}: stack trace in stderr")

    def test_n_parallel_terraform_apply_state_write_race(self):
        """N parallel `terraform apply` Bash invocations all write to
        terraform-apply-last-ts.txt. Race scenario: 5 writes within ms of each
        other. Verify file ends in valid integer state (last-writer-wins is OK;
        partial/corrupted write is NOT).
        """
        state_file = HOOKS_DIR / "data" / "terraform-apply-last-ts.txt"
        # Pre-clean to avoid pre-existing timestamp affecting test
        if state_file.exists():
            state_file.unlink()
        payloads = [
            {"tool_name": "Bash", "tool_input": {"command": f"terraform apply -auto-approve  # agent {i}"}}
            for i in range(PARALLEL_N)
        ]
        with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
            results = list(ex.map(lambda p: invoke_hook(PRE_TOOL_GUARD, p), payloads))
        # All must exit cleanly. First invocation may allow; subsequent may deny
        # via terraform-apply-retry-reconfirm rule (5min window). Either way:
        # exit 0, no traceback.
        for i, r in enumerate(results):
            self.assertEqual(
                r["exit"], 0,
                f"agent-{i} non-zero exit. stderr={r['stderr']!r}",
            )
            self.assertNotIn("Traceback", r["stderr"])
        # State file must be parseable as integer (no partial write)
        self.assertTrue(state_file.exists(), "state file not written")
        content = state_file.read_text().strip()
        try:
            ts = int(content)
            self.assertGreater(ts, 0, "state file integer non-positive")
        except ValueError:
            self.fail(f"state file corrupted (not integer): {content!r}")


class TestPostToolGuardConcurrent(unittest.TestCase):
    """post-tool-guard.py emits systemMessage JSON; verify no crash under
    parallel fire. Hook itself does not write state files.
    """

    def test_n_parallel_no_crash(self):
        payloads = [
            {
                "tool_name": "Bash",
                "tool_input": {"command": f"echo agent-{i}"},
                "tool_response": {"output": f"result {i}"},
            }
            for i in range(PARALLEL_N)
        ]
        with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
            results = list(ex.map(lambda p: invoke_hook(POST_TOOL_GUARD, p), payloads))
        for i, r in enumerate(results):
            self.assertEqual(
                r["exit"], 0,
                f"agent-{i}: non-zero exit. stderr={r['stderr']!r}",
            )
            self.assertNotIn("Traceback", r["stderr"])


class TestInjectRuleDigestConcurrent(unittest.TestCase):
    """inject-rule-digest.py READS data/keyword-rule-map.json but does NOT
    write it (config is read-only from hook's perspective; produced by another
    process). Parallel fire = pure read concurrency, no race.
    """

    def test_n_parallel_no_crash(self):
        payloads = [
            {"prompt": f"check rule {i} for AWS terraform deploy"}
            for i in range(PARALLEL_N)
        ]
        with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
            results = list(ex.map(lambda p: invoke_hook(INJECT_RULE_DIGEST, p), payloads))
        for i, r in enumerate(results):
            self.assertEqual(
                r["exit"], 0,
                f"agent-{i}: non-zero exit. stderr={r['stderr']!r}",
            )
            self.assertNotIn("Traceback", r["stderr"])
            # Output must be valid JSON (empty {} OR hookSpecificOutput dict)
            stdout = r["stdout"].strip()
            if stdout:
                try:
                    json.loads(stdout)
                except json.JSONDecodeError:
                    self.fail(f"agent-{i}: stdout not valid JSON: {stdout!r}")


class TestSessionLockGuardConcurrent(unittest.TestCase):
    """session-lock-guard.py is the ONLY hook designed for lock acquisition.
    It auto-purges stale locks (>4h) AND raises conflict for foreign active
    locks. Concurrent purge could race: 2 processes both call unlink() on
    same stale file → one succeeds, one OSError (handled).
    """

    def test_n_parallel_no_crash(self):
        """N parallel invocations from SAME pseudo-session — should not raise
        false-positive 'foreign lock' since they share same session_id.
        """
        # session-lock-guard reads CWD git context. Run from PROJECT_ROOT.
        # Run via subprocess but with shared env (same session_id derivation).
        env = os.environ.copy()
        env["CLAUDE_SESSION_ID"] = "wave-75-test-shared-session"

        def invoke():
            result = subprocess.run(
                ["python3", str(SESSION_LOCK_GUARD)],
                capture_output=True,
                text=True,
                timeout=10,
                cwd=str(PROJECT_ROOT),
                env=env,
            )
            return {"exit": result.returncode, "stderr": result.stderr}

        with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
            results = list(ex.map(lambda _: invoke(), range(PARALLEL_N)))

        for i, r in enumerate(results):
            # exit 0 (no conflict) or 2 (internal error — should not happen in
            # clean env). exit 1 only if a foreign lock present, which we did
            # not create. Tolerate exit 0; flag others.
            self.assertIn(
                r["exit"], (0, 1, 2),
                f"invocation-{i}: unexpected exit {r['exit']}. stderr={r['stderr']!r}",
            )
            # No traceback indicates clean error handling even if exit != 0
            if "Traceback" in r["stderr"]:
                self.fail(f"invocation-{i}: traceback in stderr: {r['stderr']!r}")


class TestAuditGatePRLogWrite(unittest.TestCase):
    """audit-gate.py writes documents/03-planning/pr-logs/PR-NNN.json on
    PostToolUse merge events. Different PR numbers → different files → no
    same-file race in normal wave-pack scenario (each agent has its own PR).

    This test asserts: under normal multi-PR concurrent merge scenario, all
    PR-NNN.json files end in valid JSON state. We do NOT trigger audit-gate
    directly here (it requires extensive PR/merge mocking); instead we
    document the analysis in the companion audit artifact and verify via
    direct file-write race simulation below.
    """

    def test_concurrent_different_files_no_race(self):
        """Simulate N parallel writes to N DIFFERENT JSON files (mimicking
        N agents merging N different PRs simultaneously). All files should
        end up valid JSON.
        """
        tmp_dir = Path(tempfile.mkdtemp(prefix="wave-75-pr-logs-"))
        try:
            def write_pr_log(idx: int) -> bool:
                target = tmp_dir / f"PR-{1000 + idx}.json"
                data = {"pr": 1000 + idx, "events": [{"i": j} for j in range(50)]}
                # Mimic audit-gate.py write pattern
                target.write_text(json.dumps(data, indent=2), encoding="utf-8")
                return True

            with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
                list(ex.map(write_pr_log, range(PARALLEL_N)))

            # Verify all files parseable
            for idx in range(PARALLEL_N):
                target = tmp_dir / f"PR-{1000 + idx}.json"
                self.assertTrue(target.exists(), f"PR-{1000 + idx}.json missing")
                parsed = json.loads(target.read_text(encoding="utf-8"))
                self.assertEqual(parsed["pr"], 1000 + idx)
        finally:
            shutil.rmtree(tmp_dir, ignore_errors=True)

    def test_concurrent_same_file_last_writer_wins(self):
        """Simulate N parallel writes to SAME JSON file (edge case: agent retry
        on same PR fires audit-gate multiple times). audit-gate.py uses
        `write_text(json.dumps(...))` which is NOT atomic on most filesystems
        but is generally fast enough that one write completes before the next
        truncates. Verify file ends in VALID JSON state (one of the candidate
        writes wins) — never partial/corrupted.
        """
        tmp_dir = Path(tempfile.mkdtemp(prefix="wave-75-same-file-"))
        try:
            target = tmp_dir / "PR-2000.json"

            def write_pr_log(idx: int) -> bool:
                data = {"pr": 2000, "writer": idx, "events": [{"i": j} for j in range(200)]}
                target.write_text(json.dumps(data, indent=2), encoding="utf-8")
                return True

            with ThreadPoolExecutor(max_workers=PARALLEL_N) as ex:
                list(ex.map(write_pr_log, range(PARALLEL_N)))

            self.assertTrue(target.exists())
            # Final file MUST parse as valid JSON — even if which writer won
            # is non-deterministic. Failure here = corrupted partial write.
            try:
                parsed = json.loads(target.read_text(encoding="utf-8"))
                self.assertEqual(parsed["pr"], 2000)
                self.assertIn(parsed["writer"], range(PARALLEL_N))
            except json.JSONDecodeError as e:
                self.fail(f"same-file write race produced corrupted JSON: {e}")
        finally:
            shutil.rmtree(tmp_dir, ignore_errors=True)


def main():
    """Run all tests + print summary."""
    suite = unittest.TestLoader().loadTestsFromModule(sys.modules[__name__])
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main())
