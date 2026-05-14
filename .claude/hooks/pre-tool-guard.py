#!/usr/bin/env python3
"""
PreToolUse hook — deterministic enforcement of 5 rules per Wave 73 Bucket B.

Rules enforced (BLOCK on violation unless override trailer in HEAD commit body):
1. admin-merge-discipline — `gh pr merge.*--admin` requires ADMIN_MERGE_OVERRIDE: trailer
2. agent-aws-access Tier 3 — `aws (create-|delete-|put-|update-|modify-|terminate-|...)` requires AGENT_AWS_TIER3_OK: trailer
3. aws-sg-description-ascii — Edit/Write to infrastructure/**/*.tf with non-ASCII in `description = "..."` lines
4. terraform-apply-retry-reconfirm — 2nd `terraform apply` <5min after previous (state in .claude/hooks/data/)
5. concurrent-production-mutation-ops — BLOCK if `gh workflow run terraform-apply|deploy-production|rollback` while another in_progress

Fail-safe: any internal error → exit 0 (allow), don't break user workflow.
Hook contract: reads JSON from stdin {"tool_name": "...", "tool_input": {...}}, returns
JSON to stdout {} for allow OR {"hookSpecificOutput": {"permissionDecision": "deny",
"permissionDecisionReason": "..."}} for block.
"""

import contextlib
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
STATE_DIR = PROJECT_ROOT / ".claude" / "hooks" / "data"
STATE_DIR.mkdir(parents=True, exist_ok=True)


def _allow():
    print("{}")
    sys.exit(0)


def _deny(reason: str):
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }))
    sys.exit(0)


def _commit_body() -> str:
    """Get HEAD commit message body (for override trailer detection)."""
    try:
        result = subprocess.run(
            ["git", "log", "-1", "--format=%B"],
            cwd=PROJECT_ROOT, capture_output=True, text=True, timeout=2,
        )
        return result.stdout if result.returncode == 0 else ""
    except Exception:
        return ""


def _has_trailer(trailer: str, body: str = "") -> bool:
    if not body:
        body = _commit_body()
    return bool(re.search(rf"^{re.escape(trailer)}:\s+\S", body, re.MULTILINE))


# ── Rule 1: admin-merge-discipline ─────────────────────────────

ADMIN_MERGE_RE = re.compile(
    r"(?:^|[;&|(\n])\s*(?:[A-Z_][A-Z0-9_]*=\S+\s+)*gh\s+pr\s+merge\b[^\n;&|]*\s--admin\b"
)


def check_admin_merge(command: str):
    if not ADMIN_MERGE_RE.search(command):
        return
    if _has_trailer("ADMIN_MERGE_OVERRIDE"):
        return
    _deny(
        "admin-merge-discipline: `gh pr merge --admin` BANNED unless commit body has "
        "`ADMIN_MERGE_OVERRIDE: <reason>` trailer. See .claude/rules/admin-merge-discipline.md §4. "
        "Use `gh pr merge --squash` (no --admin) and wait for CI, OR add override trailer with reason + follow-up gap."
    )


# ── Rule 2: agent-aws-access Tier 3 ────────────────────────────

AWS_TIER3_VERBS = (
    "create-", "delete-", "put-", "update-", "modify-", "terminate-",
    "start-", "stop-", "reboot-", "restore-", "attach-", "detach-",
)
AWS_TIER3_RE = re.compile(
    r"(?:^|[;&|(\n])\s*(?:[A-Z_][A-Z0-9_]*=\S+\s+)*aws\s+(?:[a-z0-9-]+\s+)?(?:"
    + "|".join(re.escape(v) for v in AWS_TIER3_VERBS) + r")[a-z0-9-]+"
)


def check_aws_tier3(command: str):
    if not AWS_TIER3_RE.search(command):
        return
    if _has_trailer("AGENT_AWS_TIER3_OK"):
        return
    _deny(
        "agent-aws-access Tier 3: AWS mutation verb (create/delete/put/update/modify/terminate/start/stop/reboot/restore/attach/detach) "
        "BANNED for agent-initiated calls. See .claude/rules/agent-aws-access.md §4. "
        "User runs manually OR add `AGENT_AWS_TIER3_OK: <reason>` trailer if user-pre-authorized."
    )


# ── Rule 3: aws-sg-description-ascii ───────────────────────────

DESCRIPTION_LINE_RE = re.compile(r'description\s*=\s*"([^"]*)"')


def _is_ascii(s: str) -> bool:
    return all(ord(c) < 128 for c in s)


def check_sg_ascii(tool_name: str, tool_input: dict):
    if tool_name not in ("Edit", "Write"):
        return
    file_path = tool_input.get("file_path", "")
    if not file_path or "infrastructure/" not in file_path or not file_path.endswith(".tf"):
        return
    content = tool_input.get("new_string") or tool_input.get("content") or ""
    for match in DESCRIPTION_LINE_RE.finditer(content):
        value = match.group(1)
        if not _is_ascii(value):
            offending = "".join(c if ord(c) < 128 else f"<U+{ord(c):04X}>" for c in value)
            _deny(
                f"aws-sg-description-ascii: AWS rejects non-ASCII in security group description fields. "
                f'Offending value: "{offending}" in {file_path}. '
                f"See .claude/rules/aws-sg-description-ascii.md. Use ASCII only (replace em-dash with hyphen, etc.)."
            )


# ── Rule 4: terraform-apply-retry-reconfirm ────────────────────

TERRAFORM_APPLY_RE = re.compile(
    r"(?:^|[;&|(\n])\s*(?:[A-Z_][A-Z0-9_]*=\S+\s+)*terraform\s+apply\b"
)
TF_APPLY_STATE = STATE_DIR / "terraform-apply-last-ts.txt"
TF_APPLY_WINDOW_SEC = 300  # 5 minutes


def check_terraform_retry(command: str):
    if not TERRAFORM_APPLY_RE.search(command):
        return
    if any(flag in command for flag in ("--help", "-help", "--version", "-version")):
        return
    now = int(time.time())
    last_ts = 0
    try:
        if TF_APPLY_STATE.exists():
            last_ts = int(TF_APPLY_STATE.read_text().strip() or 0)
    except Exception:
        last_ts = 0
    with contextlib.suppress(Exception):
        TF_APPLY_STATE.write_text(str(now))
    if last_ts > 0 and (now - last_ts) < TF_APPLY_WINDOW_SEC:
        if _has_trailer("TERRAFORM_RETRY_PREAPPROVED"):
            return
        elapsed = now - last_ts
        _deny(
            f"terraform-apply-retry-reconfirm: 2nd `terraform apply` within {elapsed}s of previous (<5min). "
            "Per .claude/rules/terraform-apply-retry-reconfirm.md, retries require explicit user re-confirmation "
            "(AskUserQuestion) showing diff + impact. Add `TERRAFORM_RETRY_PREAPPROVED: <reason>` trailer if user pre-authorized blanket retry."
        )


# ── Rule 5: concurrent-production-mutation-ops ─────────────────

PROD_MUTATION_TRIGGER_RE = re.compile(
    r"(?:^|[;&|(\n])\s*(?:[A-Z_][A-Z0-9_]*=\S+\s+)*gh\s+workflow\s+run\s+(terraform-apply|deploy-production|rollback)\.yml\b"
)


def check_concurrent_mutation(command: str):
    match = PROD_MUTATION_TRIGGER_RE.search(command)
    if not match:
        return
    triggered = match.group(1) + ".yml"
    try:
        result = subprocess.run(
            ["gh", "run", "list", "--status", "in_progress",
             "--limit", "10", "--json", "workflowName,databaseId,name"],
            cwd=PROJECT_ROOT, capture_output=True, text=True, timeout=4,
        )
        if result.returncode != 0:
            return
        all_runs = json.loads(result.stdout or "[]")
    except Exception:
        return
    mutation_workflows = {"terraform apply", "deploy production", "rollback"}
    runs = [
        r for r in all_runs
        if any(mw in (r.get("workflowName", "") or r.get("name", "")).lower() for mw in mutation_workflows)
    ]
    if not runs:
        return
    if _has_trailer("CONCURRENT_OPS_OK"):
        return
    active = ", ".join(f"{r.get('workflowName', r.get('name', '?'))}#{r.get('databaseId', '?')}" for r in runs)
    _deny(
        f"concurrent-production-mutation-ops: Triggering {triggered} while {len(runs)} mutation workflow(s) "
        f"in progress: {active}. Per .claude/rules/concurrent-production-mutation-ops.md, serialize mutations "
        f"on shared production resources. Wait for active workflow `completed`, verify resource healthy, then trigger. "
        "Add `CONCURRENT_OPS_OK: <reason>` trailer for genuine independent ops on disjoint resources."
    )


# ── Main ───────────────────────────────────────────────────────


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        _allow()
        return
    tool_name = payload.get("tool_name", "")
    tool_input = payload.get("tool_input", {}) or {}
    command = tool_input.get("command", "") or ""

    if tool_name == "Bash" and command:
        check_admin_merge(command)
        check_aws_tier3(command)
        check_terraform_retry(command)
        check_concurrent_mutation(command)
    if tool_name in ("Edit", "Write"):
        check_sg_ascii(tool_name, tool_input)

    _allow()


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception:
        os.write(2, b"pre-tool-guard: internal error, allowing tool call\n")
        print("{}")
        sys.exit(0)
