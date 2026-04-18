#!/usr/bin/env python3
"""
Backfill PR-{N}.json log files for PRs merged without the audit-gate hook firing.

Uses GitHub API (via gh CLI) to reconstruct the same JSON shape the
audit-gate hook produces at merge time. Does NOT fail on partial data —
backfilled logs mark the event type as PR_MERGED_BACKFILLED so later
tooling can tell backfilled from live-captured logs.

Usage:
  python3 scripts/backfill-pr-logs.py 339-357
  python3 scripts/backfill-pr-logs.py 342       # single PR
  python3 scripts/backfill-pr-logs.py --missing # auto-detect gaps since last logged PR
"""
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
LOG_DIR = PROJECT_ROOT / "documents" / "03-planning" / "pr-logs"

AUDIT_PATTERNS = [
    (["kiteclass-frontend/", "kitehub-frontend/src/"], "ui-review"),
    (["rules.md", "use-cases.md", "application.yml", "application.yaml"], "business-logic-audit"),
    (["Controller.java", "api-contract.md", "Dto.java", "Request.java", "Response.java"], "api-contract-audit"),
    (["pom.xml", "package.json", "pnpm-lock.yaml"], "security-audit"),
    (["infrastructure/", "docker-compose", "Dockerfile", "helm/", "k8s/", "terraform"], "ops-readiness-audit"),
]


def gh(args):
    r = subprocess.run(["gh"] + args, capture_output=True, text=True, timeout=30)
    return r.stdout.strip() if r.returncode == 0 else ""


def fetch_pr(pr):
    raw = gh(["pr", "view", str(pr), "--json",
              "number,title,headRefName,createdAt,mergedAt,mergeCommit,state,files"])
    if not raw:
        return None
    try:
        return json.loads(raw)
    except Exception:
        return None


def fetch_ci_conclusion(sha):
    if not sha:
        return "unknown"
    raw = gh(["run", "list", "--commit", sha, "--json", "workflowName,conclusion"])
    try:
        runs = json.loads(raw)
        for r in runs:
            wf = r.get("workflowName", "")
            if any(k in wf for k in ("KiteHub", "Frontend", "KiteClass")):
                return r.get("conclusion") or "unknown"
        return "unknown"
    except Exception:
        return "unknown"


def classify_audits(file_paths):
    required = []
    for patterns, name in AUDIT_PATTERNS:
        if any(any(p in f for f in file_paths) for p in patterns):
            required.append(name)
    return required


def compute_score(checklist):
    scored = ["tests_written", "ci_green_before_merge", "business_docs_updated"]
    passed = sum(1 for k in scored if checklist.get(k) is True)
    total = sum(1 for k in scored if checklist.get(k) is not None)
    req = checklist.get("audits_required", [])
    run = checklist.get("audits_run", [])
    if req:
        total += 1
        if all(a in run for a in req):
            passed += 1
    wc = checklist.get("wave_completion_check")
    if wc is not None:
        total += 1
        if wc:
            passed += 1
    return f"{passed}/{total}" if total > 0 else "N/A"


def build_log(pr_num, pr_data, ci_status):
    files = [f.get("path", "") for f in pr_data.get("files") or []]
    java_prod = [f for f in files if f.endswith(".java") and "/test/" not in f]
    test_files = [f for f in files if "Test.java" in f or "IT.java" in f]
    script_files = [f for f in files if f.endswith((".sh", ".py"))]
    code_changes = any(f.endswith(("Controller.java", "Service.java")) for f in files)
    doc_changes = any("01-business/" in f for f in files)
    branch = pr_data.get("headRefName", "") or ""
    is_wave = branch.startswith("wave/")

    required_audits = classify_audits(files)

    has_code = len(java_prod) > 0 or len(script_files) > 0
    # Same heuristic as hook: if there's code, we need test files; if no code, it's N/A
    tests_written = (len(test_files) > 0) if has_code else None
    # business_docs_updated is only scored if Controller/Service changed
    business_docs_updated = doc_changes if code_changes else None

    checklist = {
        "tests_written": tests_written,
        "ci_green_before_merge": ci_status == "success",
        "business_docs_updated": business_docs_updated,
        "audits_required": required_audits,
        "audits_run": [],  # Backfill cannot reliably check 7-day freshness retroactively
        "wave_completion_check": False if is_wave else None,
    }

    log = {
        "pr": int(pr_num),
        "title": pr_data.get("title", ""),
        "branch": branch,
        "created_at": pr_data.get("createdAt"),
        "merged_at": pr_data.get("mergedAt"),
        "checklist": checklist,
        "events": [
            {
                "time": datetime.now().isoformat(),
                "type": "PR_MERGED_BACKFILLED",
                "data": {
                    "ci_status": ci_status,
                    "files_changed": len(files),
                    "java_files": len(java_prod),
                    "test_files": len(test_files),
                    "script_files": len(script_files),
                    "business_docs": business_docs_updated,
                    "note": "Reconstructed via backfill-pr-logs.py — audits_run not verified",
                },
            }
        ],
        "compliance_score": compute_score(checklist),
    }
    return log


def parse_range(arg):
    if "-" in arg:
        start, end = arg.split("-", 1)
        return list(range(int(start), int(end) + 1))
    return [int(arg)]


def find_missing():
    """Find PRs merged after the last logged PR but without a log file."""
    existing = sorted(int(f.stem.replace("PR-", ""))
                      for f in LOG_DIR.glob("PR-*.json"))
    if not existing:
        return []
    latest_logged = existing[-1]
    raw = gh(["pr", "list", "--state", "merged", "--limit", "50",
              "--json", "number"])
    try:
        merged_numbers = sorted(int(p["number"]) for p in json.loads(raw))
    except Exception:
        return []
    return [n for n in merged_numbers if n > latest_logged]


def main(argv):
    if len(argv) < 2:
        print("Usage: backfill-pr-logs.py <range|N|--missing>", file=sys.stderr)
        sys.exit(1)

    if argv[1] == "--missing":
        prs = find_missing()
        if not prs:
            print("No missing PR logs detected.")
            return
        print(f"Missing logs for {len(prs)} PRs: {prs}")
    else:
        prs = parse_range(argv[1])

    LOG_DIR.mkdir(parents=True, exist_ok=True)
    written = 0
    skipped = 0
    for pr in prs:
        out = LOG_DIR / f"PR-{pr}.json"
        if out.exists():
            skipped += 1
            continue
        pr_data = fetch_pr(pr)
        if not pr_data:
            print(f"  PR #{pr}: not found or API error, skipping")
            continue
        if pr_data.get("state") != "MERGED":
            print(f"  PR #{pr}: state={pr_data.get('state')}, skipping")
            continue
        sha = (pr_data.get("mergeCommit") or {}).get("oid", "")
        ci = fetch_ci_conclusion(sha)
        log = build_log(pr, pr_data, ci)
        out.write_text(json.dumps(log, indent=2, default=str), encoding="utf-8")
        print(f"  PR #{pr}: written (ci={ci}, score={log['compliance_score']})")
        written += 1

    print(f"\nDone. Written: {written}, already-present: {skipped}")


if __name__ == "__main__":
    main(sys.argv)
