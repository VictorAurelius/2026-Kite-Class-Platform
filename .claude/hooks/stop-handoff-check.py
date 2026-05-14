#!/usr/bin/env python3
"""
Stop hook — pre-handoff-self-test-completeness enforcement.

When session stops, scan recent assistant messages for "DONE" claims on user-facing
acceptance criteria WITHOUT corresponding §2 self-test checklist evidence.

If detected, emit `systemMessage` (visible to user) WARNING — does NOT block stop
since Stop hook block semantics differ from PreToolUse. WARN-only is safe v1.0
behavior; future enhancement could bump to BLOCK after pattern verification.

Hook contract: reads JSON from stdin (transcript_path field per Anthropic docs),
returns JSON `{}` for silent OR `{"systemMessage": "..."}` for warn.

Fail-safe: any error → exit 0 silently.
"""

import json
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent

# Phrases indicating "user-facing" AC verified
DONE_PHRASES = [
    r"\bgap\b.*\b(flipped to|set to|marked|status:?)\s*🟢?\s*DONE",
    r"\b(✅\s*)?Merged\b.*#\d+",
    r"\bDONE\s+(today|now|this PR|this session)",
    r"\bverified\s+live\b",
    r"\bself-test\s+PASS\b",
    r"\bproduction\s+verified\b",
]
DONE_RE = re.compile("|".join(DONE_PHRASES), re.IGNORECASE)

# Phrases indicating §2 self-test checklist WAS performed
EVIDENCE_PHRASES = [
    r"§2\.\d",                     # rule §2.x reference
    r"login\s+flow\b",             # auth-gated AC
    r"role-?guard\b",              # role mismatch check
    r"navigation\s+path\b",        # nav existence check
    r"credential\s+available\b",
    r"PRE_HANDOFF_PARTIAL:",       # explicit override trailer
    r"AC\s+verified",
    r"smoke\s+test\s+(passed|run)",
    r"browser\s+test",
    r"end-to-end\s+verify",
]
EVIDENCE_RE = re.compile("|".join(EVIDENCE_PHRASES), re.IGNORECASE)

# User-facing AC keywords that trigger the rule
USER_FACING_KEYWORDS = [
    "login", "redirect", "dashboard", "button", "URL", "form",
    "admin UI", "sidebar", "nav link", "click", "approve",
]
USER_FACING_RE = re.compile("|".join(re.escape(k) for k in USER_FACING_KEYWORDS), re.IGNORECASE)


def _silent():
    print("{}")
    sys.exit(0)


def _warn(msg: str):
    print(json.dumps({"systemMessage": msg}))
    sys.exit(0)


def _read_transcript(path_str: str) -> str:
    """Read last ~20 assistant messages from transcript JSONL."""
    if not path_str:
        return ""
    try:
        path = Path(path_str)
        if not path.exists():
            return ""
        text_chunks = []
        with path.open() as f:
            for line in f:
                try:
                    entry = json.loads(line)
                except json.JSONDecodeError:
                    continue
                # Look for assistant role + text content
                role = entry.get("role") or entry.get("type", "")
                if role not in ("assistant", "model"):
                    continue
                content = entry.get("content") or entry.get("message", {}).get("content", "")
                if isinstance(content, list):
                    for block in content:
                        if isinstance(block, dict) and block.get("type") == "text":
                            text_chunks.append(block.get("text", ""))
                elif isinstance(content, str):
                    text_chunks.append(content)
        # Return last ~20 chunks
        return "\n".join(text_chunks[-20:])
    except Exception:
        return ""


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        _silent()
        return
    transcript_path = payload.get("transcript_path", "")
    text = _read_transcript(transcript_path)
    if not text:
        _silent()
        return

    has_done = bool(DONE_RE.search(text))
    if not has_done:
        _silent()
        return
    has_user_facing = bool(USER_FACING_RE.search(text))
    if not has_user_facing:
        _silent()
        return
    has_evidence = bool(EVIDENCE_RE.search(text))
    if has_evidence:
        _silent()
        return
    # DONE claim + user-facing keyword + no evidence → WARN
    _warn(
        "⚠️  pre-handoff-self-test-completeness: DONE claim detected on user-facing AC "
        "(login/dashboard/button/URL etc.) without §2 checklist evidence. "
        "Per .claude/rules/pre-handoff-self-test-completeness.md §2/§5.1, walk login flow + "
        "role-guard + navigation + target action before flipping DONE. Add `PRE_HANDOFF_PARTIAL: <reason>` "
        "trailer if browser test deferred."
    )


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception:
        print("{}")
        sys.exit(0)
