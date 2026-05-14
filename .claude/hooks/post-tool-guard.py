#!/usr/bin/env python3
"""
PostToolUse hook — Wave 73 Bucket B Rules 6 + 7.

Rule 6: post-merge-sync-completeness
  After `git commit` or `gh pr merge`, scan the latest diff for `+**Status:**` lines on
  any documents/04-quality/gaps/GAP-*.md. If detected, verify gap-status.csv was also
  touched in the same diff. If not → emit WARN (systemMessage), don't block.

Rule 7: release-fix-retry-budget
  After `git commit` or `gh pr merge`, scan last 4 commits. If most-recent + prior 2
  fix-PRs all touch SAME workflow file or .trivyignore → WARN to apply §3 decision flow
  before next retry. WARN-only.

Coexists with audit-gate.py (the existing PostToolUse hook). Anthropic supports
multiple hooks per event — both run on PostToolUse. This file scoped narrow, audit-gate.py
keeps existing logic.

Fail-safe: any error → exit 0 silently.
"""

import json
import re
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
GIT_COMMIT_RE = re.compile(r"\bgit\s+commit\b")
GH_MERGE_RE = re.compile(r"\bgh\s+pr\s+merge\b")


def _silent():
    print("{}")
    sys.exit(0)


def _warn(msg: str):
    print(json.dumps({"systemMessage": msg}))
    sys.exit(0)


def _git(*args, timeout=4) -> str:
    try:
        result = subprocess.run(
            ["git", *args], cwd=PROJECT_ROOT,
            capture_output=True, text=True, timeout=timeout,
        )
        return result.stdout if result.returncode == 0 else ""
    except Exception:
        return ""


# ── Rule 6: post-merge-sync-completeness ───────────────────────


def check_status_csv_sync() -> str:
    """Return warning message if HEAD has gap status flip without CSV update."""
    diff = _git("show", "--name-only", "--pretty=format:", "HEAD")
    if not diff:
        return ""
    files = [f for f in diff.strip().split("\n") if f]
    gap_files = [f for f in files if f.startswith("documents/04-quality/gaps/GAP-") and f.endswith(".md")]
    if not gap_files:
        return ""
    csv_touched = "documents/04-quality/gaps/gap-status.csv" in files
    if csv_touched:
        return ""
    # Check if any gap file actually flipped Status (not just edited)
    diff_content = _git("show", "--unified=0", "HEAD", "--", *gap_files)
    if not re.search(r"^\+\s*\*\*Status:\*\*", diff_content, re.MULTILINE):
        return ""
    # Check override trailer
    body = _git("log", "-1", "--format=%B")
    if re.search(r"^POST_MERGE_SYNC_OVERRIDE:\s+\S", body, re.MULTILINE):
        return ""
    return (
        "⚠️  post-merge-sync-completeness: HEAD commit flipped gap Status field but "
        "gap-status.csv NOT updated in same diff. Per .claude/rules/post-merge-sync-completeness.md §2 "
        "target 1, CSV is canonical. Update CSV row in follow-up PR OR add `POST_MERGE_SYNC_OVERRIDE:` trailer."
    )


# ── Rule 7: release-fix-retry-budget ───────────────────────────

WORKFLOW_FILE_RE = re.compile(r"^\.github/workflows/[\w.-]+\.ya?ml$|^\.trivyignore$")


def check_release_retry_pattern() -> str:
    """Return warning if last 3 commits all touch same workflow file or .trivyignore."""
    # Get last 3 commit shas + names
    log = _git("log", "-3", "--format=%H")
    shas = [s for s in log.strip().split("\n") if s]
    if len(shas) < 3:
        return ""
    # For each, get touched files matching workflow / trivyignore
    per_commit_files = []
    for sha in shas:
        diff = _git("show", "--name-only", "--pretty=format:", sha)
        files = [f for f in diff.strip().split("\n") if f and WORKFLOW_FILE_RE.match(f)]
        per_commit_files.append(set(files))
    # Find intersection — same file touched in all 3
    if not per_commit_files[0]:
        return ""
    common = per_commit_files[0]
    for fs in per_commit_files[1:]:
        common &= fs
    if not common:
        return ""
    # Override trailer in HEAD?
    body = _git("log", "-1", "--format=%B")
    override_pat = r"^RELEASE_RETRY_(?:EXTERNAL_FIX|DEADLINE_OVERRIDE|TRANSIENT|TOOLING_FIXED):\s+\S"
    if re.search(override_pat, body, re.MULTILINE):
        return ""
    files_str = ", ".join(sorted(common))
    return (
        f"⚠️  release-fix-retry-budget: Last 3 commits all touch same file(s): {files_str}. "
        "Per .claude/rules/release-fix-retry-budget.md §3, at retry #2 STOP patching + apply pivot matrix "
        "(redesign/relax gate). Add `RELEASE_RETRY_*_OVERRIDE:` trailer if genuine retry-#3+ exempt per §5."
    )


# ── Main ───────────────────────────────────────────────────────


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        _silent()
        return
    tool_name = payload.get("tool_name", "")
    tool_input = payload.get("tool_input", {}) or {}
    command = tool_input.get("command", "") or ""

    if tool_name != "Bash" or not command:
        _silent()
        return

    if not (GIT_COMMIT_RE.search(command) or GH_MERGE_RE.search(command)):
        _silent()
        return

    warnings = []
    msg = check_status_csv_sync()
    if msg:
        warnings.append(msg)
    msg = check_release_retry_pattern()
    if msg:
        warnings.append(msg)

    if warnings:
        _warn("\n\n".join(warnings))
    _silent()


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception:
        print("{}")
        sys.exit(0)
