#!/usr/bin/env python
"""PostToolUse hook: remind to run UI audit when an FE PR is merged via gh pr merge."""
import json
import re
import subprocess
import sys


def main() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0

    cmd = (data.get("tool_input") or {}).get("command", "") or ""
    match = re.search(r"gh\s+pr\s+merge\s+(\d+)", cmd)
    if not match:
        return 0

    pr = match.group(1)
    try:
        result = subprocess.run(
            ["gh", "pr", "view", pr, "--json", "files", "--jq", ".files[].path"],
            capture_output=True,
            text=True,
            timeout=15,
        )
    except Exception:
        return 0

    files = result.stdout or ""
    if "kiteclass-frontend" in files or "kitehub-frontend" in files:
        print(
            json.dumps(
                {
                    "systemMessage": (
                        f"FE PR #{pr} merged — run UI audit "
                        "(capture screenshots + score per-page /128, update latest/, "
                        "save report to documents/04-quality/audits/ui/)"
                    )
                }
            )
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
