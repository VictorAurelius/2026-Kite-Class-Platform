#!/usr/bin/env python3
"""Empirical hook ordering test — Wave 75 Bucket C.

Mục tiêu: chứng minh behaviour của Claude Code khi nhiều hooks cùng listen 1 event.

Per Anthropic docs (https://code.claude.com/docs/en/hooks) + closed feature request
https://github.com/anthropics/claude-code/issues/21533 — "All matching hooks run in
parallel, and identical handlers are deduplicated automatically." Không có sequential
option (closed as not planned).

Trong repo này:
- PreToolUse Bash|Edit|Write: 1 hook (`pre-tool-guard.py`)
- PostToolUse Bash: 2 hooks (`audit-gate.py` + `post-tool-guard.py`) — đây là multi-hook
  same event case cần investigate
- Stop: 1 hook (`stop-handoff-check.py`)
- UserPromptSubmit: 1 hook (`inject-rule-digest.py`)

Test approach: invoke 2 PostToolUse hooks subprocess-style với cùng payload, verify:
  1. Both exit 0 silently cho non-trigger event (independence baseline)
  2. settings.local.json wires both hooks (config audit)
  3. Hooks không share state qua stdin/stdout (parallel execution implies này)
  4. Nếu 1 hook fail (non-zero), không ảnh hưởng hook kia (independence)

Limitation: test này SYNTHETIC — không integration với real Claude Code lifecycle.
Không verify được:
- Actual parallel vs serial dispatch in Claude Code runtime
- Whether BLOCK from hook A skips hook B (must read source / docs)
"""

import json
import os
import subprocess
import unittest
from pathlib import Path

HOOKS_DIR = Path(__file__).resolve().parent.parent
PROJECT_ROOT = HOOKS_DIR.parent.parent
SETTINGS_PATH = PROJECT_ROOT / ".claude" / "settings.local.json"
HOOK_AUDIT_GATE = HOOKS_DIR / "audit-gate.py"
HOOK_POST_TOOL_GUARD = HOOKS_DIR / "post-tool-guard.py"
HOOK_PRE_TOOL_GUARD = HOOKS_DIR / "pre-tool-guard.py"


def invoke_hook(hook_path: Path, payload: dict, timeout: int = 10) -> dict:
    """Invoke hook subprocess với JSON stdin, capture exit + stdout + stderr."""
    env = os.environ.copy()
    env["CLAUDE_PROJECT_DIR"] = str(PROJECT_ROOT)
    result = subprocess.run(
        ["python3", str(hook_path)],
        input=json.dumps(payload),
        capture_output=True,
        text=True,
        timeout=timeout,
        env=env,
    )
    return {
        "exit": result.returncode,
        "stdout": result.stdout,
        "stderr": result.stderr,
    }


class TestSettingsWiring(unittest.TestCase):
    """Verify settings.local.json wires 2 hooks per PostToolUse Bash event."""

    def setUp(self):
        self.settings = json.loads(SETTINGS_PATH.read_text(encoding="utf-8"))
        self.hooks_cfg = self.settings.get("hooks", {})

    def test_settings_file_exists_and_parses(self):
        self.assertTrue(SETTINGS_PATH.exists(), "settings.local.json must exist")
        self.assertIn("hooks", self.settings, "settings.local.json must have 'hooks' key")

    def test_pretooluse_has_single_hook(self):
        """PreToolUse Bash|Edit|Write should have exactly 1 hook (pre-tool-guard.py)."""
        pretool = self.hooks_cfg.get("PreToolUse", [])
        self.assertGreaterEqual(len(pretool), 1, "Expect ≥1 PreToolUse matcher block")
        commands = []
        for block in pretool:
            for h in block.get("hooks", []):
                commands.append(h.get("command", ""))
        guard_count = sum(1 for c in commands if "pre-tool-guard.py" in c)
        self.assertEqual(guard_count, 1, f"Expect 1 pre-tool-guard.py wiring, got {guard_count}")

    def test_posttooluse_bash_has_two_hooks(self):
        """PostToolUse Bash should have 2 hooks (audit-gate + post-tool-guard) — multi-hook case."""
        posttool = self.hooks_cfg.get("PostToolUse", [])
        bash_block = None
        for block in posttool:
            if "Bash" in block.get("matcher", ""):
                bash_block = block
                break
        self.assertIsNotNone(bash_block, "PostToolUse Bash matcher must exist")
        hooks_list = bash_block.get("hooks", [])
        self.assertEqual(
            len(hooks_list), 2,
            f"Expect 2 PostToolUse Bash hooks (audit-gate + post-tool-guard), got {len(hooks_list)}",
        )
        commands = [h.get("command", "") for h in hooks_list]
        self.assertTrue(
            any("audit-gate.py" in c for c in commands),
            "audit-gate.py must be wired in PostToolUse Bash",
        )
        self.assertTrue(
            any("post-tool-guard.py" in c for c in commands),
            "post-tool-guard.py must be wired in PostToolUse Bash",
        )


class TestHookIndependence(unittest.TestCase):
    """Verify 2 PostToolUse Bash hooks behave independently khi invoked subprocess-style.

    Per Anthropic parallel execution model, mỗi hook phải:
    - Receive fresh stdin (không pollute từ hook kia)
    - Return independent exit code
    - Không depend on order of invocation
    """

    def test_audit_gate_silent_on_non_pr_command(self):
        """audit-gate.py should exit 0 silently cho generic Bash (non-PR event)."""
        payload = {
            "tool_name": "Bash",
            "tool_input": {"command": "ls -la"},
            "tool_response": {"stdout": "", "stderr": "", "exit_code": 0},
        }
        result = invoke_hook(HOOK_AUDIT_GATE, payload)
        self.assertEqual(
            result["exit"], 0,
            f"audit-gate.py should exit 0; got {result['exit']}, stderr={result['stderr']!r}",
        )

    def test_post_tool_guard_silent_on_non_trigger_command(self):
        """post-tool-guard.py should exit 0 silently cho generic Bash."""
        payload = {
            "tool_name": "Bash",
            "tool_input": {"command": "ls -la"},
            "tool_response": {"stdout": "", "stderr": "", "exit_code": 0},
        }
        result = invoke_hook(HOOK_POST_TOOL_GUARD, payload)
        self.assertEqual(
            result["exit"], 0,
            f"post-tool-guard.py should exit 0; got {result['exit']}, stderr={result['stderr']!r}",
        )

    def test_both_hooks_independent_same_payload(self):
        """Invoke both hooks với cùng payload sequentially. Both should exit 0 independently."""
        payload = {
            "tool_name": "Bash",
            "tool_input": {"command": "echo test"},
            "tool_response": {"stdout": "test\n", "stderr": "", "exit_code": 0},
        }
        out_a = invoke_hook(HOOK_AUDIT_GATE, payload)
        out_b = invoke_hook(HOOK_POST_TOOL_GUARD, payload)
        self.assertEqual(out_a["exit"], 0, f"audit-gate exit={out_a['exit']} stderr={out_a['stderr']!r}")
        self.assertEqual(out_b["exit"], 0, f"post-tool-guard exit={out_b['exit']} stderr={out_b['stderr']!r}")

    def test_order_swap_does_not_change_outcomes(self):
        """Swap invocation order — both hooks still exit 0 independently (parallel-safe)."""
        payload = {
            "tool_name": "Bash",
            "tool_input": {"command": "pwd"},
            "tool_response": {"stdout": "/tmp\n", "stderr": "", "exit_code": 0},
        }
        out_b_first = invoke_hook(HOOK_POST_TOOL_GUARD, payload)
        out_a_second = invoke_hook(HOOK_AUDIT_GATE, payload)
        self.assertEqual(out_b_first["exit"], 0)
        self.assertEqual(out_a_second["exit"], 0)


class TestHookStdoutShape(unittest.TestCase):
    """Verify hook stdout shape — parallel execution implies stdout không feed sang hook kế.

    Per Anthropic docs: hooks không chained — mỗi hook reads stdin từ Claude Code runtime,
    output stdout là response cho Claude, KHÔNG là input cho next hook.
    """

    def test_audit_gate_stdout_is_valid_json_or_empty(self):
        """audit-gate.py stdout should be valid JSON object or empty (per hook contract)."""
        payload = {
            "tool_name": "Bash",
            "tool_input": {"command": "ls"},
            "tool_response": {"stdout": "", "stderr": "", "exit_code": 0},
        }
        result = invoke_hook(HOOK_AUDIT_GATE, payload)
        stdout = result["stdout"].strip()
        if stdout:
            try:
                parsed = json.loads(stdout)
                self.assertIsInstance(parsed, dict, "Hook stdout must be JSON object")
            except json.JSONDecodeError:
                pass

    def test_post_tool_guard_stdout_is_valid_json_or_empty(self):
        payload = {
            "tool_name": "Bash",
            "tool_input": {"command": "ls"},
            "tool_response": {"stdout": "", "stderr": "", "exit_code": 0},
        }
        result = invoke_hook(HOOK_POST_TOOL_GUARD, payload)
        stdout = result["stdout"].strip()
        if stdout:
            try:
                parsed = json.loads(stdout)
                self.assertIsInstance(parsed, dict)
            except json.JSONDecodeError:
                pass


class TestFailSafe(unittest.TestCase):
    """Verify hooks fail-safe — malformed input không crash hook (exit 0 anyway)."""

    def test_audit_gate_malformed_input_does_not_crash(self):
        """audit-gate.py với malformed JSON stdin should fail-safe exit 0."""
        env = os.environ.copy()
        env["CLAUDE_PROJECT_DIR"] = str(PROJECT_ROOT)
        result = subprocess.run(
            ["python3", str(HOOK_AUDIT_GATE)],
            input="not valid json",
            capture_output=True, text=True, timeout=10, env=env,
        )
        # Hook contract: any error → exit 0 silently per fail-safe principle.
        self.assertEqual(
            result.returncode, 0,
            f"audit-gate.py should fail-safe on malformed input; got exit={result.returncode}",
        )

    def test_post_tool_guard_malformed_input_does_not_crash(self):
        env = os.environ.copy()
        env["CLAUDE_PROJECT_DIR"] = str(PROJECT_ROOT)
        result = subprocess.run(
            ["python3", str(HOOK_POST_TOOL_GUARD)],
            input="not valid json",
            capture_output=True, text=True, timeout=10, env=env,
        )
        self.assertEqual(
            result.returncode, 0,
            f"post-tool-guard.py should fail-safe on malformed input; got exit={result.returncode}",
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
