#!/usr/bin/env python3
"""
UserPromptSubmit hook: inject relevant rule digest based on keyword matching.

Wave 73 Bucket C — Meta Context Optimization.

Reads user prompt from stdin (Anthropic UserPromptSubmit contract), scans for
keywords from `data/keyword-rule-map.json`, extracts §1 The Rule + earliest
Anti-patterns section from each matched rule (~30 lines), and returns JSON
with `additionalContext` field containing concatenated digest.

Constraints:
- Hook completes <500ms (UserPromptSubmit is on hot path)
- Total injection cap: 5000 tokens (~20000 chars)
- Dedupe per-prompt (same rule matched by multiple keywords → injected once)
- Graceful fallback: any exception → empty additionalContext (no crash)

Hook contract (Anthropic):
- stdin: JSON with `prompt` (str), `session_id`, `transcript_path`, etc.
- stdout: JSON; if `additionalContext` present, prepended to model context
- exit 0: success; non-zero: error (but hook still degrades gracefully)
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

# ── Paths ────────────────────────────────────────────────────────

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
CONFIG_PATH = PROJECT_ROOT / ".claude" / "hooks" / "data" / "keyword-rule-map.json"


# ── Digest extraction ───────────────────────────────────────────


def extract_digest(rule_path: Path, max_lines: int) -> str | None:
    """Extract §1 The Rule + earliest Anti-patterns/banned section from rule file.

    Returns concatenated digest string capped at ~max_lines lines, or None if
    the file cannot be read.
    """
    if not rule_path.exists():
        return None

    try:
        text = rule_path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return None

    lines = text.splitlines()

    # Find §1 The Rule section (first `## 1.` heading or `## The Rule`)
    rule_section = _extract_section(
        lines,
        start_pattern=re.compile(r"^##\s*1\.\s|^##\s+The Rule\b", re.IGNORECASE),
        max_lines=max_lines // 2 if max_lines >= 20 else max_lines,
    )

    # Find earliest Anti-patterns (or "Banned" / "Anti-pattern") section
    anti_section = _extract_section(
        lines,
        start_pattern=re.compile(
            r"^##\s*\d*\.?\s*(Anti-?patterns?|Banned\b|Banned shortcuts)",
            re.IGNORECASE,
        ),
        max_lines=max_lines // 2 if max_lines >= 20 else max_lines,
    )

    parts: list[str] = []
    if rule_section:
        parts.append(rule_section)
    if anti_section:
        parts.append(anti_section)

    if not parts:
        # Fallback: first max_lines after frontmatter
        parts.append(_extract_head(lines, max_lines=max_lines))

    return "\n\n".join(parts).strip()


def _extract_section(
    lines: list[str], start_pattern: re.Pattern, max_lines: int
) -> str | None:
    """Extract a markdown section starting at start_pattern, until next `## ` or EOF."""
    for i, line in enumerate(lines):
        if start_pattern.search(line):
            collected = [line]
            for j in range(i + 1, min(i + 1 + max_lines, len(lines))):
                next_line = lines[j]
                # Stop at next top-level heading
                if next_line.startswith("## ") and j > i:
                    break
                collected.append(next_line)
            return "\n".join(collected).rstrip()
    return None


def _extract_head(lines: list[str], max_lines: int) -> str:
    """Skip frontmatter (lines starting with **) + return first max_lines content."""
    skipped = 0
    for i, line in enumerate(lines):
        if (
            line.startswith("**")
            or line.startswith("---")
            or not line.strip()
            or line.startswith("# ")
        ):
            skipped = i + 1
        else:
            break
    end = min(skipped + max_lines, len(lines))
    return "\n".join(lines[skipped:end]).rstrip()


# ── Keyword matching ────────────────────────────────────────────


def find_matched_rules(prompt: str, config: dict) -> list[tuple[str, int, str]]:
    """Scan prompt for keyword patterns; return list of (rule_path, max_lines, label).

    Dedupes: each rule path appears at most once (first match wins for max_lines).
    """
    seen: set[str] = set()
    matched: list[tuple[str, int, str]] = []

    for entry in config.get("keywords", []):
        pattern_str = entry.get("pattern", "")
        if not pattern_str:
            continue

        try:
            regex = re.compile(pattern_str, re.IGNORECASE)
        except re.error:
            continue

        if not regex.search(prompt):
            continue

        max_lines = int(entry.get("max_lines", 30))
        label = entry.get("label", "")

        for rule in entry.get("rules", []):
            if rule in seen:
                continue
            seen.add(rule)
            matched.append((rule, max_lines, label))

    return matched


# ── Build additionalContext ─────────────────────────────────────


def build_context(prompt: str, config: dict) -> str:
    """Match keywords + extract digests + concat under global cap."""
    matched = find_matched_rules(prompt, config)
    if not matched:
        return ""

    cap_chars = int(config.get("global_cap_chars", 20000))

    parts: list[str] = []
    used_chars = 0

    header = (
        "# Auto-injected rule digests (Wave 73 Bucket C)\n"
        "Matched keyword(s) in your prompt → relevant rule snippets prepended for "
        "in-context guidance. Read full rule via `Read` tool if needed.\n"
    )
    parts.append(header)
    used_chars += len(header)

    for rule_path_str, max_lines, label in matched:
        rule_path = PROJECT_ROOT / rule_path_str
        digest = extract_digest(rule_path, max_lines)
        if not digest:
            continue

        snippet = (
            f"\n---\n"
            f"## Rule: `{rule_path_str}` ({label})\n\n"
            f"{digest}\n"
        )

        if used_chars + len(snippet) > cap_chars:
            # Cap reached; append truncation note
            parts.append(
                f"\n---\n_(Cap {cap_chars} chars reached; remaining matched rules "
                f"omitted. Read directly if needed.)_\n"
            )
            break

        parts.append(snippet)
        used_chars += len(snippet)

    return "".join(parts)


# ── Main hook entry ─────────────────────────────────────────────


def main() -> int:
    """Read stdin JSON, build additionalContext, write JSON to stdout.

    Always returns 0; any error → empty additionalContext (silent degradation).
    """
    try:
        raw = sys.stdin.read()
        if not raw.strip():
            print("{}")
            return 0

        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            print("{}")
            return 0

        prompt = payload.get("prompt", "")
        if not isinstance(prompt, str) or not prompt.strip():
            print("{}")
            return 0

        if not CONFIG_PATH.exists():
            print("{}")
            return 0

        try:
            config = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError, UnicodeDecodeError):
            print("{}")
            return 0

        context = build_context(prompt, config)

        if context:
            response = {
                "hookSpecificOutput": {
                    "hookEventName": "UserPromptSubmit",
                    "additionalContext": context,
                }
            }
            print(json.dumps(response))
        else:
            print("{}")

        return 0

    except Exception:  # noqa: BLE001 — graceful fallback for ANY error
        # Silent degradation: never break the user's prompt flow
        print("{}")
        return 0


if __name__ == "__main__":
    sys.exit(main())
