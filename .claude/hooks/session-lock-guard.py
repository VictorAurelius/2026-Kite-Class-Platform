#!/usr/bin/env python3
"""
session-lock-guard.py — block commits on a branch locked by a different session.

Phase 2 enforcement layer for GAP-193 (paired with /start-session skill, Phase 1).

Behavior:
  1. Determine current session ID (env CLAUDE_SESSION_ID, fallback whoami+timestamp).
  2. Determine current branch (`git branch --show-current`).
  3. Scan `.claude/session-locks/*.lock` for active locks matching the branch.
  4. Auto-purge stale locks (>4h old) per start-session/SKILL.md §Gotchas.
  5. If a fresh lock from a DIFFERENT session_id claims the branch → exit 1 with
     a clear error message; otherwise exit 0.

Invocation:
  - Called by `audit-gate.py` at the start of merge-event processing.
  - Can also run standalone for testing.

Exit codes:
  0 — no foreign lock conflict (or only own/stale locks)
  1 — conflict detected; commit/merge should be blocked
  2 — internal error (treated as soft-pass to avoid breaking workflow)
"""
from __future__ import annotations

import os
import re
import socket
import subprocess
import sys
import time
from pathlib import Path

# Stale lock threshold — matches start-session/SKILL.md §Gotchas "Stale locks (>4h)
# auto-purged". Keep value in sync with that doc.
STALE_LOCK_SECONDS = 4 * 60 * 60  # 4 hours


def get_session_id() -> str:
    """Resolve current session id.

    Priority:
      1. $CLAUDE_SESSION_ID — set by Claude Code harness when available
      2. fallback: whoami + boot-relative epoch (best-effort, stable for one session)
    """
    sid = os.environ.get("CLAUDE_SESSION_ID")
    if sid:
        return sid.strip()
    # Fallback — derive a stable per-process-tree id; not perfect but better than
    # `time.time()` (which would drift each invocation). Use hostname + uid + parent
    # pid so two scripts in the same shell session match.
    user = os.environ.get("USER") or "unknown"
    host = socket.gethostname()
    ppid = os.getppid()
    return f"{user}@{host}:ppid-{ppid}"


def get_current_branch(repo_root: Path) -> str | None:
    try:
        result = subprocess.run(
            ["git", "branch", "--show-current"],
            capture_output=True,
            text=True,
            timeout=5,
            cwd=repo_root,
            check=False,
        )
        branch = result.stdout.strip()
        return branch or None
    except (subprocess.SubprocessError, FileNotFoundError):
        return None


def parse_lock_file(path: Path) -> dict[str, str]:
    """Parse a YAML-ish lock file (we keep it lightweight — see schema in
    .claude/skills/workflow/start-session/reference/context-template.md).

    Returns a dict with at least: session_id, branch (best-effort).
    """
    fields: dict[str, str] = {}
    try:
        content = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return fields
    for line in content.splitlines():
        m = re.match(r"^([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(.+?)\s*$", line)
        if m:
            key, val = m.group(1), m.group(2)
            # Strip surrounding quotes
            val = val.strip().strip('"').strip("'")
            fields[key] = val
    return fields


def find_repo_root() -> Path:
    """Locate repo root from this script's path. The hook lives at
    `<repo>/.claude/hooks/session-lock-guard.py`, so root is two parents up.
    """
    return Path(__file__).resolve().parent.parent.parent


def scan_locks(
    locks_dir: Path,
    current_branch: str,
    current_session: str,
    now: float,
) -> tuple[list[Path], list[dict[str, str]]]:
    """Return (purged_stale_paths, foreign_active_locks).

    `foreign_active_locks` is a list of parsed lock dicts that:
      - claim the same branch as current_branch
      - have a session_id different from current_session
      - are NOT stale (mtime within STALE_LOCK_SECONDS)
    """
    purged: list[Path] = []
    foreign: list[dict[str, str]] = []
    if not locks_dir.is_dir():
        return purged, foreign

    for entry in sorted(locks_dir.iterdir()):
        if entry.is_dir():
            continue
        if entry.suffix != ".lock":
            continue
        try:
            mtime = entry.stat().st_mtime
        except OSError:
            continue
        age = now - mtime
        if age > STALE_LOCK_SECONDS:
            # Auto-purge stale lock — keeps the directory tidy and avoids false
            # blocks from crashed sessions.
            try:
                entry.unlink()
                purged.append(entry)
            except OSError:
                # Permission / race — ignore; treating as ignored stale lock.
                pass
            continue
        fields = parse_lock_file(entry)
        lock_branch = fields.get("branch", "")
        lock_session = fields.get("session_id", "")
        if lock_branch == current_branch and lock_session and lock_session != current_session:
            fields["_path"] = str(entry)
            foreign.append(fields)
    return purged, foreign


def main() -> int:
    repo_root = find_repo_root()
    branch = get_current_branch(repo_root)
    if not branch:
        # No git context — nothing to guard. Soft-pass.
        return 0

    session_id = get_session_id()
    locks_dir = repo_root / ".claude" / "session-locks"
    purged, foreign = scan_locks(locks_dir, branch, session_id, time.time())

    if purged:
        # Informational only — don't block on cleanup.
        print(
            f"session-lock-guard: purged {len(purged)} stale lock(s) (>4h)",
            file=sys.stderr,
        )

    if not foreign:
        return 0

    # Conflict — print enough detail for the user to identify the other session.
    lines = [
        "🛑 session-lock-guard: branch is locked by a different session.",
        f"   branch       : {branch}",
        f"   this session : {session_id}",
    ]
    for fl in foreign:
        lines.append(
            f"   foreign lock : session={fl.get('session_id', '?')}"
            f"  started={fl.get('started', '?')}"
            f"  file={fl.get('_path', '?')}"
        )
    lines.append(
        "   To proceed: either coordinate with the other session, "
        "or run `/end-session` there to release the lock."
    )
    print("\n".join(lines), file=sys.stderr)
    return 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001 — last-resort guard, must not break workflow
        print(f"session-lock-guard: internal error: {exc}", file=sys.stderr)
        sys.exit(2)
