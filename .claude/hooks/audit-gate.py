#!/usr/bin/env python
"""
PostToolUse hook: PR lifecycle tracker + audit gate.

Detects PR create/merge events, writes JSON log files for audit trail,
checks CI status and test coverage, warns/blocks on violations.

Log files: documents/03-planning/pr-logs/PR-{number}.json
"""
import contextlib
import json
import re
import subprocess
import sys
from datetime import datetime, timedelta
from pathlib import Path

# ── Audit Rules (file pattern → required audit) ─────────────────

AUDIT_RULES = [
    {
        "patterns": ["kiteclass-frontend/", "kitehub-frontend/src/"],
        "audit": "ui-review",
        "command": "/ui-review",
        "label": "UI audit (/128)",
    },
    {
        "patterns": ["rules.md", "use-cases.md", "application.yml", "application.yaml"],
        "audit": "business-logic-audit",
        "command": "/business-logic-audit",
        "label": "Business Logic audit (/100)",
    },
    {
        "patterns": ["Controller.java", "api-contract.md", "Dto.java", "Request.java", "Response.java"],
        "audit": "api-contract-audit",
        "command": "/api-contract-audit",
        "label": "API Contract audit (/100)",
    },
    {
        "patterns": ["pom.xml", "package.json", "pnpm-lock.yaml"],
        "audit": "security-audit",
        "command": "/security-audit",
        "label": "Security audit (/100)",
    },
    {
        "patterns": ["infrastructure/", "docker-compose", "Dockerfile", "helm/", "k8s/", "terraform"],
        "audit": "ops-readiness-audit",
        "command": "/ops-readiness-audit",
        "label": "Ops Readiness audit (/100)",
    },
]

AUDIT_FRESHNESS_DAYS = 7

AUDIT_DIRS = {
    "ui-review": "documents/04-quality/audits/ui",
    "business-logic-audit": "documents/04-quality/audits/business",
    "api-contract-audit": "documents/04-quality/audits/api",
    "security-audit": "documents/04-quality/audits/security",
    "ops-readiness-audit": "documents/04-quality/audits/ops",
    "performance-audit": "documents/04-quality/audits/performance",
}

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
LOG_DIR = PROJECT_ROOT / "documents" / "03-planning" / "pr-logs"


# ── Event Detection ──────────────────────────────────────────────

def detect_pr_merge(cmd: str) -> str | None:
    """Detect: gh pr merge <number> — only if it's the primary command, not a substring."""
    # Skip if command is echo, python, cat, or piped test (avoids false positives)
    stripped = cmd.strip()
    if any(stripped.startswith(p) for p in ("echo ", "python ", "cat ", "printf ", "#")):
        return None
    match = re.match(r"^\s*gh\s+pr\s+merge\s+(\d+)", stripped)
    return match.group(1) if match else None


def detect_pr_create(cmd: str, output: str) -> str | None:
    """Detect: gh pr create → parse PR number from output URL."""
    stripped = cmd.strip()
    if any(stripped.startswith(p) for p in ("echo ", "python ", "cat ", "printf ", "#")):
        return None
    if not re.match(r"^\s*gh\s+pr\s+create", stripped):
        return None
    match = re.search(r"/pull/(\d+)", output or "")
    return match.group(1) if match else None


def detect_wave_merge(cmd: str) -> bool:
    return bool(re.search(r"wave/", cmd))


# ── GitHub Data Fetching ─────────────────────────────────────────

def gh_run(args: list[str], timeout: int = 15) -> str:
    try:
        r = subprocess.run(["gh"] + args, capture_output=True, text=True, timeout=timeout)
        return r.stdout.strip()
    except Exception:
        return ""


def get_pr_files(pr: str) -> list[str]:
    raw = gh_run(["pr", "view", pr, "--json", "files", "--jq", ".files[].path"])
    return [f for f in raw.splitlines() if f.strip()]


def get_pr_info(pr: str) -> dict:
    raw = gh_run(["pr", "view", pr, "--json", "title,headRefName,mergedAt,mergeCommit,state"])
    try:
        return json.loads(raw)
    except Exception:
        return {}


def get_ci_status(pr: str) -> str:
    """Check CI conclusion for PR's merge commit."""
    info = get_pr_info(pr)
    sha = (info.get("mergeCommit") or {}).get("oid", "")
    if not sha:
        return "unknown"
    raw = gh_run(["run", "list", "--commit", sha, "--json", "workflowName,conclusion"])
    try:
        runs = json.loads(raw)
        for r in runs:
            wf = r.get("workflowName", "")
            if any(k in wf for k in ("KiteHub", "Frontend", "KiteClass")):
                return r.get("conclusion", "unknown")
        return "unknown"
    except Exception:
        return "unknown"


def has_recent_audit(audit_name: str) -> bool:
    audit_dir = PROJECT_ROOT / AUDIT_DIRS.get(audit_name, "")
    if not audit_dir.exists():
        return False
    cutoff = datetime.now() - timedelta(days=AUDIT_FRESHNESS_DAYS)
    for f in audit_dir.iterdir():
        if f.suffix == ".md" and f.stat().st_mtime > cutoff.timestamp():
            return True
    return False


# ── PR Log File Management ───────────────────────────────────────

def read_pr_log(pr: str) -> dict:
    log_file = LOG_DIR / f"PR-{pr}.json"
    if log_file.exists():
        try:
            return json.loads(log_file.read_text(encoding="utf-8"))
        except Exception:
            pass
    return {
        "pr": int(pr),
        "title": "",
        "branch": "",
        "created_at": None,
        "merged_at": None,
        "checklist": {
            "tests_written": None,
            "ci_green_before_merge": None,
            "business_docs_updated": None,
            "audits_required": [],
            "audits_run": [],
            "wave_completion_check": None,
        },
        "events": [],
        "compliance_score": None,
    }


def write_pr_log(pr: str, log: dict) -> None:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_file = LOG_DIR / f"PR-{pr}.json"
    log_file.write_text(json.dumps(log, indent=2, default=str), encoding="utf-8")
    # Auto-stage so file gets included in next commit (avoids forgetting to commit).
    # Non-critical — worst case, file stays untracked.
    with contextlib.suppress(Exception):
        subprocess.run(["git", "add", str(log_file)], capture_output=True, timeout=5, cwd=PROJECT_ROOT)


def add_event(log: dict, event_type: str, data: dict) -> dict:
    log["events"].append({
        "time": datetime.now().isoformat(),
        "type": event_type,
        "data": data,
    })
    return log


def compute_score(checklist: dict) -> str:
    scored = ["tests_written", "ci_green_before_merge", "business_docs_updated"]
    passed = sum(1 for k in scored if checklist.get(k) is True)
    total = sum(1 for k in scored if checklist.get(k) is not None)
    # Add audits check
    req = checklist.get("audits_required", [])
    run = checklist.get("audits_run", [])
    if req:
        total += 1
        if all(a in run for a in req):
            passed += 1
    # Add wave check
    wc = checklist.get("wave_completion_check")
    if wc is not None:
        total += 1
        if wc:
            passed += 1
    return f"{passed}/{total}" if total > 0 else "N/A"


# ── Event Handlers ───────────────────────────────────────────────

def on_pr_create(pr: str, output: str) -> dict | None:
    """Handle PR creation — initialize log file."""
    try:
        info = get_pr_info(pr)
        log = read_pr_log(pr)
        log["title"] = info.get("title", "")
        log["branch"] = info.get("headRefName", "")
        log["created_at"] = info.get("createdAt") or datetime.now().isoformat()
        log = add_event(log, "PR_CREATED", {"number": int(pr)})
        write_pr_log(pr, log)
    except Exception:
        pass  # Non-critical — don't crash hook on log failure
    return None  # No message needed on create


def on_pr_merge(pr: str) -> dict | None:
    """Handle PR merge — collect compliance data, log, warn/block."""
    try:
        return _on_pr_merge_impl(pr)
    except Exception as e:
        # Non-critical — don't crash hook, but warn
        return {"systemMessage": f"PR #{pr} — audit-gate error: {e}"}


def is_docs_only(files: list[str]) -> bool:
    """Docs-only PR: all changed files are docs/skills/rules/READMEs. No audit required."""
    if not files:
        return False
    DOC_PREFIXES = ("documents/", ".claude/rules/", ".claude/skills/", "docs/")
    DOC_SUFFIXES = (".md",)
    DOC_BASENAMES = ("README.md", "CHANGELOG.md", "CLAUDE.md")
    for f in files:
        base = f.rsplit("/", 1)[-1]
        if base in DOC_BASENAMES:
            continue
        if f.endswith(DOC_SUFFIXES):
            continue
        if any(f.startswith(p) for p in DOC_PREFIXES):
            continue
        # Anything else = not docs-only
        return False
    return True


def has_audit_override(pr: str, info: dict) -> tuple[bool, str]:
    """Check if PR body or merge commit has AUDIT_OVERRIDE: marker. Returns (overridden, reason)."""
    body = gh_run(["pr", "view", pr, "--json", "body", "--jq", ".body"]) or ""
    match = re.search(r"AUDIT_OVERRIDE:\s*(.+?)(?:\n|$)", body)
    if match:
        return True, match.group(1).strip()
    return False, ""


def check_gap_doc_drift(pr: str, info: dict, files: list[str]) -> list[str]:
    """Detect PRs that reference GAP-XXX in title/body without touching the gap file.

    Returns list of "GAP-XXX (no log entry referencing this PR)" warnings.
    Governance check per memory `feedback_post_merge_doc_sync.md` and
    scripts/gap-drift-check.sh — Wave 5 (2026-04-24) shipped 4 sub-PRs
    closing partial GAP-047 progress without updating the gap file or
    ROADMAP, requiring a follow-up sync PR (#527).
    """
    title = info.get("title", "")
    body = info.get("body", "") or ""
    combined = f"{title} {body}"
    gap_ids = sorted(set(re.findall(r"GAP-\d{3}", combined)))
    if not gap_ids:
        return []
    repo_root = Path(__file__).resolve().parents[2]
    gaps_dir = repo_root / "documents" / "04-quality" / "gaps"
    warnings = []
    for gap_id in gap_ids:
        # Check 1: did the PR touch the gap file at all?
        touched = any(f.startswith("documents/04-quality/gaps/") and gap_id in f for f in files)
        # Check 2: even if touched, does the gap Log reference this PR? (catches
        # touch-without-log mistakes — e.g. status bump but no log entry).
        gap_files = list(gaps_dir.glob(f"{gap_id}-*.md"))
        log_has_pr = False
        if gap_files:
            content = gap_files[0].read_text(encoding="utf-8", errors="replace")
            # Crude but effective: search for "#<pr>" in the file.
            if re.search(rf"#{pr}\b", content):
                log_has_pr = True
        if not touched and not log_has_pr:
            warnings.append(f"{gap_id} (PR refs gap but doesn't update gap file Log)")
        elif touched and not log_has_pr:
            warnings.append(f"{gap_id} (gap file touched but Log doesn't mention #{pr})")
    return warnings


def _on_pr_merge_impl(pr: str) -> dict | None:
    files = get_pr_files(pr)
    info = get_pr_info(pr)
    ci_status = get_ci_status(pr)

    # Analyze files
    java_files = [f for f in files if f.endswith(".java") and "/test/" not in f]
    test_files = [f for f in files if "Test.java" in f or "IT.java" in f]
    script_files = [f for f in files if f.endswith((".sh", ".py"))]
    code_changes = any(f.endswith(("Controller.java", "Service.java")) for f in files)
    doc_changes = any("01-business/" in f for f in files)
    is_wave = any(f.startswith("wave/") for f in [info.get("headRefName", "")])
    docs_only = is_docs_only(files)

    # Required audits
    required_audits = []
    missing_audits = []
    for rule in AUDIT_RULES:
        for pattern in rule["patterns"]:
            if any(pattern in f for f in files):
                required_audits.append(rule["audit"])
                if not has_recent_audit(rule["audit"]):
                    missing_audits.append(rule)
                break

    # Build log
    log = read_pr_log(pr)
    log["title"] = info.get("title", log.get("title", ""))
    log["branch"] = info.get("headRefName", log.get("branch", ""))
    log["merged_at"] = info.get("mergedAt") or datetime.now().isoformat()

    has_code = len(java_files) > 0 or len(script_files) > 0
    has_tests = len(test_files) > 0 or (not java_files and not script_files)
    docs_ok = not code_changes or doc_changes

    log["checklist"]["ci_green_before_merge"] = ci_status == "success"
    log["checklist"]["tests_written"] = has_tests if has_code else None
    log["checklist"]["business_docs_updated"] = docs_ok if code_changes else None
    log["checklist"]["audits_required"] = required_audits
    log["checklist"]["audits_run"] = [a for a in required_audits if has_recent_audit(a)]
    log["checklist"]["wave_completion_check"] = False if is_wave else None

    log["compliance_score"] = compute_score(log["checklist"])

    log = add_event(log, "PR_MERGED", {
        "ci_status": ci_status,
        "files_changed": len(files),
        "java_files": len(java_files),
        "test_files": len(test_files),
        "business_docs": docs_ok,
        "missing_audits": [a["audit"] for a in missing_audits],
    })

    write_pr_log(pr, log)

    # Build response
    violations = []
    if ci_status != "success" and not docs_only:
        violations.append(f"CI status: {ci_status}")
    if java_files and not test_files:
        violations.append(f"{len(java_files)} java files, 0 test files")
    if script_files and not java_files and not test_files:
        violations.append(f"{len(script_files)} script(s) — verify syntax + review per output-review-mandate")
    if code_changes and not doc_changes:
        violations.append("Business logic changed but no 01-business/ docs updated")
    if missing_audits and not docs_only:
        violations.append(f"Missing audits: {', '.join(a['audit'] for a in missing_audits)}")
    gap_drift = check_gap_doc_drift(pr, info, files)
    if gap_drift:
        violations.append(
            "Gap doc drift — " + "; ".join(gap_drift)
            + " (per memory feedback_post_merge_doc_sync.md — file a docs-only sync PR)"
        )
    if is_wave:
        violations.append("Wave merge — run /wave-completion-check + audit suite within 3 days (post-wave-audit-mandate.md)")

    if not violations:
        return {"systemMessage": f"PR #{pr} merged — compliance {log['compliance_score']}. Log: pr-logs/PR-{pr}.json"}

    # Check AUDIT_OVERRIDE
    overridden, override_reason = (False, "")
    if missing_audits and not docs_only:
        overridden, override_reason = has_audit_override(pr, info)

    lines = [f"PR #{pr} merged — {len(violations)} violation(s) detected:"]
    for v in violations:
        lines.append(f"  - {v}")
    if docs_only:
        lines.append("  (docs-only PR — audit/CI checks skipped)")
    if overridden:
        lines.append(f"  ⚠️  AUDIT_OVERRIDE present: {override_reason}")
    lines.append(f"\nCompliance: {log['compliance_score']}. Log: documents/03-planning/pr-logs/PR-{pr}.json")
    lines.append("Run: ./scripts/pr-compliance-check.sh " + pr)

    # BLOCK conditions (per post-wave-audit-mandate.md §3):
    # 1. CI RED (hardest gate) — except docs-only
    # 2. Missing audits on non-docs-only PR — unless AUDIT_OVERRIDE present
    should_block = False
    block_reasons = []
    if ci_status == "failure" and not docs_only:
        should_block = True
        block_reasons.append("CI failure")
    if missing_audits and not docs_only and not overridden:
        should_block = True
        block_reasons.append(f"missing audits: {', '.join(a['audit'] for a in missing_audits)}")

    if should_block:
        lines.append(f"\n🛑 BLOCKED: {'; '.join(block_reasons)}")
        lines.append("Run required audits, then retry. Or add 'AUDIT_OVERRIDE: <reason> <gap-link>' to PR body to bypass.")
        return {
            "decision": "block",
            "reason": "\n".join(lines),
        }

    return {"systemMessage": "\n".join(lines)}


# ── Main ─────────────────────────────────────────────────────────

def main() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0

    cmd = (data.get("tool_input") or {}).get("command", "") or ""
    output = (data.get("tool_output") or {}).get("stdout", "") or str(data.get("tool_output", ""))

    # Detect PR create
    pr_created = detect_pr_create(cmd, output)
    if pr_created:
        result = on_pr_create(pr_created, output)
        if result:
            print(json.dumps(result))
        return 0

    # Detect PR merge
    pr_merged = detect_pr_merge(cmd)
    if pr_merged:
        result = on_pr_merge(pr_merged)
        if result:
            print(json.dumps(result))
            if result.get("decision") == "block":
                return 1
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())
