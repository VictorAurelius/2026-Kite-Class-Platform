#!/usr/bin/env python
"""
PostToolUse hook: audit gate — detect PR merges and check if required audits exist.

Maps changed file patterns to required audit types. If a required audit report
is missing for the current date/wave, prints a warning with the audit command.

Replaces the simpler fe-pr-merge-reminder.py with a comprehensive audit gate.
"""
import json
import re
import subprocess
import sys
from datetime import datetime, timedelta
from pathlib import Path


# File pattern → required audit(s)
AUDIT_RULES = [
    # FE changes → ui-review
    {
        "patterns": ["kiteclass-frontend/", "kitehub-frontend/src/"],
        "audit": "ui-review",
        "command": "/ui-review",
        "label": "UI audit (capture screenshots + score /128)",
    },
    # Business rules changes → business-logic-audit
    {
        "patterns": ["rules.md", "use-cases.md", "application.yml", "application.yaml"],
        "audit": "business-logic-audit",
        "command": "/business-logic-audit",
        "label": "Business Logic audit (code ↔ rules.md /100)",
    },
    # API changes → api-contract-audit
    {
        "patterns": ["Controller.java", "api-contract.md", "Dto.java", "Request.java", "Response.java"],
        "audit": "api-contract-audit",
        "command": "/api-contract-audit",
        "label": "API Contract audit (endpoints ↔ docs /100)",
    },
    # Dependency changes → security-audit
    {
        "patterns": ["pom.xml", "package.json", "pnpm-lock.yaml"],
        "audit": "security-audit",
        "command": "/security-audit",
        "label": "Security audit (deps + OWASP /100)",
    },
    # Infra changes → ops-readiness-audit
    {
        "patterns": ["infrastructure/", "docker-compose", "Dockerfile", "helm/", "k8s/", "terraform"],
        "audit": "ops-readiness-audit",
        "command": "/ops-readiness-audit",
        "label": "Ops Readiness audit (monitoring + deploy /100)",
    },
]

# How recent an audit report must be (days)
AUDIT_FRESHNESS_DAYS = 7

# Audit report directories
AUDIT_DIRS = {
    "ui-review": "documents/04-quality/audits/ui",
    "business-logic-audit": "documents/04-quality/audits/business",
    "api-contract-audit": "documents/04-quality/audits/api",
    "security-audit": "documents/04-quality/audits/security",
    "ops-readiness-audit": "documents/04-quality/audits/ops",
    "performance-audit": "documents/04-quality/audits/performance",
}

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent


def get_pr_number(cmd: str) -> str | None:
    match = re.search(r"gh\s+pr\s+merge\s+(\d+)", cmd)
    return match.group(1) if match else None


def get_pr_files(pr: str) -> str:
    try:
        result = subprocess.run(
            ["gh", "pr", "view", pr, "--json", "files", "--jq", ".files[].path"],
            capture_output=True, text=True, timeout=15,
        )
        return result.stdout or ""
    except Exception:
        return ""


def has_recent_audit(audit_name: str) -> bool:
    """Check if an audit report exists within AUDIT_FRESHNESS_DAYS."""
    audit_dir = PROJECT_ROOT / AUDIT_DIRS.get(audit_name, "")
    if not audit_dir.exists():
        return False

    cutoff = datetime.now() - timedelta(days=AUDIT_FRESHNESS_DAYS)
    for f in audit_dir.iterdir():
        if f.suffix == ".md" and f.stat().st_mtime > cutoff.timestamp():
            return True
    return False


def check_wave_merge(cmd: str) -> bool:
    """Detect if this is a wave → main merge."""
    return bool(re.search(r"wave/\d+", cmd))


def main() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0

    cmd = (data.get("tool_input") or {}).get("command", "") or ""
    pr = get_pr_number(cmd)
    if not pr:
        return 0

    files = get_pr_files(pr)
    if not files:
        return 0

    # Determine required audits based on changed files
    required = []
    for rule in AUDIT_RULES:
        for pattern in rule["patterns"]:
            if pattern in files:
                if not has_recent_audit(rule["audit"]):
                    required.append(rule)
                break

    # Wave merge → ALL audits required
    is_wave = check_wave_merge(cmd)
    if is_wave:
        for audit_name, audit_dir in AUDIT_DIRS.items():
            if not has_recent_audit(audit_name):
                # Find the matching rule or create generic
                matching = [r for r in AUDIT_RULES if r["audit"] == audit_name]
                if matching and matching[0] not in required:
                    required.append(matching[0])

    if not required:
        return 0

    # Build warning message
    lines = [f"PR #{pr} merged — {len(required)} audit(s) recommended:"]
    for r in required:
        lines.append(f"  - {r['label']} → run {r['command']}")

    if is_wave:
        lines.append("")
        lines.append("Wave merge detected — ALL audits should pass before next wave.")

    print(json.dumps({"systemMessage": "\n".join(lines)}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
