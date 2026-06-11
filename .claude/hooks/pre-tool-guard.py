#!/usr/bin/env python3
"""
PreToolUse hook — deterministic enforcement of 5 rules per Wave 73 Bucket B.

Rules enforced (BLOCK on violation unless override trailer in PR body — per-PR-scoped):
1. admin-merge-discipline — `gh pr merge.*--admin` requires ADMIN_MERGE_OVERRIDE: trailer in PR body
2. agent-aws-access Tier 3 — `aws (create-|delete-|put-|update-|modify-|terminate-|...)` requires AGENT_AWS_TIER3_OK: trailer
3. aws-sg-description-ascii — Edit/Write to infrastructure/**/*.tf with non-ASCII in `description = "..."` lines
4. terraform-apply-retry-reconfirm — 2nd `terraform apply` <5min after previous (state in .claude/hooks/data/)
5. concurrent-production-mutation-ops — BLOCK if `gh workflow run terraform-apply|deploy-production|rollback` while another in_progress

Trailer scoping (Wave 75 GAP-529 fix): for `gh pr merge <N> --admin`, read the SPECIFIC PR's
body via `gh pr view <N> --json body` instead of the current branch HEAD body — prevents stale
trailer leak across PRs / branches / sessions. Falls back to HEAD body only when PR number
not extractable from command (terraform retry, AWS Tier 3, concurrent ops).

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
    """Get HEAD commit message body (legacy fallback for override trailer detection).

    Wave 75 GAP-529: prefer `_pr_body(pr_num)` for per-PR scoping when PR number is
    extractable from the command. This function remains as fallback for commands
    that don't carry a PR number (terraform apply, AWS CLI, gh workflow run).
    """
    try:
        result = subprocess.run(
            ["git", "log", "-1", "--format=%B"],
            cwd=PROJECT_ROOT, capture_output=True, text=True, timeout=2,
        )
        return result.stdout if result.returncode == 0 else ""
    except Exception:
        return ""


def _pr_body(pr_num: str) -> str:
    """Read PR body via `gh pr view <N> --json body`. Empty on any error (fail-safe).

    Per Wave 75 GAP-529 fix — replaces HEAD-scoped trailer reads with per-PR scoping
    for `gh pr merge <N>` style commands. See .claude/rules/admin-merge-discipline.md §4.
    """
    try:
        result = subprocess.run(
            ["gh", "pr", "view", pr_num, "--json", "body"],
            cwd=PROJECT_ROOT, capture_output=True, text=True, timeout=5,
        )
        if result.returncode != 0:
            return ""
        data = json.loads(result.stdout or "{}")
        return data.get("body", "") or ""
    except Exception:
        return ""


# Match `gh pr merge <N>` with optional flags interleaved before/after N
# (covers `gh pr merge 1234 --admin`, `gh pr merge --squash 5678 --admin`, etc.)
PR_NUM_RE = re.compile(
    r"gh\s+pr\s+merge\b(?:\s+-{1,2}\S+(?:=\S+)?)*\s+(\d+)\b"
)


def _extract_pr_num(command: str) -> str | None:
    """Extract PR number from `gh pr merge <N>` command. Returns None if not found."""
    m = PR_NUM_RE.search(command)
    return m.group(1) if m else None


def _has_trailer_in_pr(pr_num, trailer: str) -> bool:
    """Check trailer in PR body (per-PR scope) with HEAD commit body fallback.

    Per Wave 75 GAP-529 fix — primary path queries the specific PR being merged
    via `gh pr view`. Falls back to HEAD commit body only when pr_num is None
    (commands that don't carry a PR number — terraform retry, AWS Tier 3, etc.).
    """
    if pr_num:
        body = _pr_body(pr_num)
    else:
        body = _commit_body()
    return bool(re.search(rf"^{re.escape(trailer)}:\s+\S", body, re.MULTILINE))


def _has_trailer(trailer: str, body: str = "") -> bool:
    """Legacy helper — HEAD-scoped trailer detection.

    DEPRECATED for PR-context rules per Wave 75 GAP-529. Retained for backward
    compatibility with any external callers; new code should use `_has_trailer_in_pr`.
    """
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
    pr_num = _extract_pr_num(command)
    if _has_trailer_in_pr(pr_num, "ADMIN_MERGE_OVERRIDE"):
        return
    _deny(
        "admin-merge-discipline: `gh pr merge --admin` BANNED unless PR body has "
        "`ADMIN_MERGE_OVERRIDE: <reason>` trailer. See .claude/rules/admin-merge-discipline.md §4. "
        "Use `gh pr merge --squash` (no --admin) and wait for CI, OR add override trailer to PR body with reason + follow-up gap."
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
    # AWS Tier 3 commands don't carry a PR number — HEAD-scoped trailer per current
    # rule contract (user pre-authorizes via commit body trailer). Migrating AWS
    # rule to PR-scope would require a different override mechanism (e.g.,
    # session-scoped state file) — out of scope for GAP-529.
    if _has_trailer_in_pr(None, "AGENT_AWS_TIER3_OK"):
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
        # terraform apply doesn't carry a PR number; keep HEAD-scoped trailer check
        # via _has_trailer_in_pr(None, ...) which falls back to _commit_body().
        if _has_trailer_in_pr(None, "TERRAFORM_RETRY_PREAPPROVED"):
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
    # `gh workflow run` doesn't have a PR number — keep HEAD-scoped trailer check.
    if _has_trailer_in_pr(None, "CONCURRENT_OPS_OK"):
        return
    active = ", ".join(f"{r.get('workflowName', r.get('name', '?'))}#{r.get('databaseId', '?')}" for r in runs)
    _deny(
        f"concurrent-production-mutation-ops: Triggering {triggered} while {len(runs)} mutation workflow(s) "
        f"in progress: {active}. Per .claude/rules/concurrent-production-mutation-ops.md, serialize mutations "
        f"on shared production resources. Wait for active workflow `completed`, verify resource healthy, then trigger. "
        "Add `CONCURRENT_OPS_OK: <reason>` trailer for genuine independent ops on disjoint resources."
    )


# ── Rule 6: worktree-only-branch-work — in-repo worktree path ──

WORKTREE_ADD_RE = re.compile(
    r"(?:^|[;&|(\n])\s*(?:[A-Z_][A-Z0-9_]*=\S+\s+)*git\s+worktree\s+add\b([^\n;&|]*)"
)
_WT_VALUE_FLAGS = {"-b", "-B", "--reason"}


def _worktree_path_arg(args: str):
    """First positional after `git worktree add` = destination path.
    Skips flags (-b/-B/--reason take a value; other -flags standalone)."""
    toks = args.split()
    i = 0
    while i < len(toks):
        t = toks[i]
        if t in _WT_VALUE_FLAGS:
            i += 2
            continue
        if t.startswith("-"):
            i += 1
            continue
        return t.strip("'\"")
    return None


def check_worktree_in_repo(command: str):
    m = WORKTREE_ADD_RE.search(command)
    if not m:
        return
    path = _worktree_path_arg(m.group(1))
    if not path:
        return
    in_repo = False
    if ".claude/worktrees/" in path:
        in_repo = True
    elif path.startswith("../"):
        in_repo = False  # sibling/parent — outside repo root
    elif path.startswith("/"):
        try:
            in_repo = str(Path(path).resolve()).startswith(str(PROJECT_ROOT) + os.sep)
        except Exception:
            in_repo = False
    else:
        in_repo = True  # relative path not escaping cwd (repo root)
    if not in_repo:
        return
    if _has_trailer_in_pr(None, "WORKTREE_ONLY_OVERRIDE"):
        return
    _deny(
        "worktree-only-branch-work §3: `git worktree add` IN-REPO path "
        f"('{path}') makes the worktree's .claude/ a nested project-config → harness "
        "auto-loads DUPLICATE CLAUDE.md + rules (context ~2x). Use a SIBLING path OUTSIDE "
        "the repo: `git worktree add ../kite-wt-<slug> <branch>`. "
        "See .claude/rules/worktree-only-branch-work.md §3.1. Add "
        "`WORKTREE_ONLY_OVERRIDE: <reason>` trailer to HEAD commit if genuinely single-session/recovery."
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
        check_worktree_in_repo(command)
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
