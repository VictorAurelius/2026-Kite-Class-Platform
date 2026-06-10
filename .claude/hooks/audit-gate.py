#!/usr/bin/env python3
"""
PostToolUse hook: PR lifecycle tracker + audit gate.

Detects PR create/merge events, writes JSON log files for audit trail,
checks CI status and test coverage, warns/blocks on violations.

Log files: documents/03-planning/pr-logs/PR-{number}.json
"""
import contextlib
import json
import os
import re
import socket
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
    {
        # AI Branding behavior verification — model swap, prompt change, provider rewrite,
        # §5 Quality Reviewer logic, ContentModerationService logic.
        # Patterns target real kiteclass-core implementation paths (verified 2026-04-26 GAP-016).
        # NOT kitehub-branding/ — that module shipped v1 only; v2 redesign landed in kiteclass-core.
        # Frontend templates use ui-review skill (separate).
        # See `.claude/rules/ai-branding-guidelines.md` §11.4 + GAP-223 Sub-PR 223.1.
        "patterns": [
            # Module path — catches any v2 AI Branding Java change
            "kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/",
            "kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/branding/",
            "kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/instance/",
            "kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/quality/",
            "kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/moderation/",
            "kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/",
            # Specific real class names (real names — not architecture-doc names)
            "AIClient.java", "OllamaAIClient.java", "ResilientAIClient.java", "MockAIClient.java",
            "AnalyzerService.java", "PlannerService.java", "PlanExecutor.java",
            "InstanceQualityReviewer.java", "ContentModerationService.java",
            "TenantProvisioningSaga.java", "InstanceLifecycleService.java",
            # 5 quality checks (§5 Quality Gate Strategy pattern)
            "ContrastQualityCheck.java", "CssVarsQualityCheck.java",
            "AssetUrlsQualityCheck.java", "VisualRegressionQualityCheck.java",
            "LogoPlacementQualityCheck.java",
            # Resource handlers (Chain of Responsibility for STATIC/TEMPLATE/AI routing)
            "AIResourceHandler.java", "StaticResourceHandler.java",
        ],
        "audit": "ai-branding-quality-gate",
        "command": "/ai-branding-quality-gate",
        "label": "AI Branding Quality Gate (/100)",
    },
]

AUDIT_FRESHNESS_DAYS = 7

# ── UI Kits Integration Rule (GAP-265, Wave Review Process Improvement) ──
#
# Block merge of PRs touching `documents/02-architecture/design-system/ui_kits/**`
# unless PR body confirms the integration smoke test ran OR an override trailer
# is present. Closes Phase 3 of GAP-263 — Tier 3 enforcement layer.
#
# Why: 2026-04-29 incident — closure PR #678 updated kit READMEs but forgot
# `ui_kits/index.html` landing page; user caught "đã có UI của trang kitehub
# đâu nhỉ, tôi vẫn thấy 3 repo". Manual review missed it. Hook prevents
# recurrence by demanding explicit integration confirmation in PR body.
#
# Spec: GAP-265 §Proposed Fix — `INTEGRATION_OK_NO_LANDING_CHANGE: <reason>`
# trailer downgrades block → warn for genuine docs-only edits within ui_kits/.
UI_KITS_INTEGRATION_RULE = {
    "name": "ui-kits-integration-required",
    "trigger_paths": [
        "documents/02-architecture/design-system/ui_kits/",
    ],
    "trigger_exclude": [
        "documents/02-architecture/design-system/ui_kits/_v1-baseline/",
    ],
    # PR body must contain ONE of these phrases for non-overridden pass.
    "required_phrases": [
        "Integration smoke test:",  # explicit confirmation
    ],
    "override_trailer": "INTEGRATION_OK_NO_LANDING_CHANGE:",
    "severity": "block",
    "rationale_url": "documents/04-quality/gaps/GAP-265-ui-kits-hook-ci-enforcement.md",
    "incident_ref": "2026-04-29 landing-page parity miss (PR #678 closure)",
}

AUDIT_DIRS = {
    "ui-review": "documents/04-quality/audits/ui",
    "business-logic-audit": "documents/04-quality/audits/business",
    "api-contract-audit": "documents/04-quality/audits/api",
    "security-audit": "documents/04-quality/audits/security",
    "ops-readiness-audit": "documents/04-quality/audits/ops",
    "performance-audit": "documents/04-quality/audits/performance",
    "ai-branding-quality-gate": "documents/04-quality/audits/ai-branding",
}

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
LOG_DIR = PROJECT_ROOT / "documents" / "03-planning" / "pr-logs"
SESSION_LOCKS_DIR = PROJECT_ROOT / ".claude" / "session-locks"
SESSION_LOCK_GUARD = PROJECT_ROOT / ".claude" / "hooks" / "session-lock-guard.py"


# ── Session Telemetry (GAP-193 Phase 2) ──────────────────────────


def get_session_id() -> str:
    """Same resolution rules as session-lock-guard.py — keep in sync."""
    sid = os.environ.get("CLAUDE_SESSION_ID")
    if sid:
        return sid.strip()
    user = os.environ.get("USER") or "unknown"
    host = socket.gethostname()
    return f"{user}@{host}:ppid-{os.getppid()}"


def get_session_started_at(session_id: str) -> str | None:
    """Best-effort: read earliest matching lock file's `started:` field, else
    fall back to env var $CLAUDE_SESSION_START.
    """
    if SESSION_LOCKS_DIR.is_dir():
        candidates: list[tuple[float, Path]] = []
        for entry in SESSION_LOCKS_DIR.iterdir():
            if entry.suffix != ".lock":
                continue
            try:
                content = entry.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            if f"session_id: {session_id}" in content or f'session_id: "{session_id}"' in content:
                try:
                    candidates.append((entry.stat().st_mtime, entry))
                except OSError:
                    continue
        if candidates:
            candidates.sort()
            content = candidates[0][1].read_text(encoding="utf-8", errors="replace")
            for line in content.splitlines():
                m = re.match(r"^started\s*:\s*(.+?)\s*$", line)
                if m:
                    return m.group(1).strip().strip('"').strip("'")
    return os.environ.get("CLAUDE_SESSION_START")


def get_turn_count() -> int | None:
    """Best-effort turn count.

    Prefer $CLAUDE_TURN_COUNT (if harness exposes it). Otherwise return None
    so the field can be omitted gracefully — never fabricate.
    """
    val = os.environ.get("CLAUDE_TURN_COUNT")
    if val and val.isdigit():
        return int(val)
    return None


def build_session_telemetry() -> dict:
    """Assemble session_telemetry block for PR-{N}.json. Always returns a dict;
    optional fields are omitted (not None) so JSON stays clean.
    """
    telemetry: dict = {"session_id": get_session_id()}
    started = get_session_started_at(telemetry["session_id"])
    if started:
        telemetry["session_started_at"] = started
    turns = get_turn_count()
    if turns is not None:
        telemetry["turn_count"] = turns
    return telemetry


def run_session_lock_guard() -> tuple[bool, str]:
    """Invoke session-lock-guard.py. Returns (ok, message).

    ok=False indicates a foreign-session lock conflict (caller may surface as
    a warning — we do NOT block PR-merge events on this signal alone, since
    the hook fires post-action; the guard is more useful at commit/edit time).
    """
    if not SESSION_LOCK_GUARD.is_file():
        return True, ""
    try:
        result = subprocess.run(
            ["python3", str(SESSION_LOCK_GUARD)],
            capture_output=True,
            text=True,
            timeout=5,
            cwd=PROJECT_ROOT,
            check=False,
        )
    except (subprocess.SubprocessError, FileNotFoundError) as exc:
        return True, f"(session-lock-guard could not run: {exc})"
    if result.returncode == 0:
        return True, ""
    return False, (result.stderr or "session-lock-guard: foreign lock detected").strip()


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
        # session_telemetry populated by build_session_telemetry() on PR_CREATED /
        # PR_MERGED events. Keys: session_id (always), session_started_at +
        # turn_count when detectable. GAP-193 Phase 2.
        "session_telemetry": {},
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
        # GAP-193 Phase 2 — record session telemetry on every PR event so retro
        # analysis can correlate PR quality vs session length / turn count.
        log["session_telemetry"] = build_session_telemetry()
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


# Domain registry per post-wave-audit-mandate.md §2.4.1
DOMAIN_REGISTRY = {
    "track-2-shared-ui": ["packages/shared-ui/"],
    "phase-4-kit-ports": ["kiteclass/kiteclass-frontend/", "kitehub/kitehub-frontend/"],
    "release-deploy-artifacts": ["infrastructure/", "helm/", "terraform-aws/", "terraform-oracle/"],
    "meta-governance": [".claude/rules/", ".claude/skills/"],
    # backend-domain-* keys are dynamic — accepted with prefix match
}


def has_domain_milestone_defer(pr: str, files: list[str]) -> tuple[bool, str, str]:
    """Check AUDIT_DEFER_DOMAIN_MILESTONE trailer per post-wave-audit-mandate.md §2.4.

    Returns (deferred, domain_key, error_msg). If trailer present but domain
    invalid OR diff touches outside domain → returns (False, key, error).
    Else (True, key, "") = silent pass (audit deferred to milestone).
    """
    body = gh_run(["pr", "view", pr, "--json", "body", "--jq", ".body"]) or ""
    match = re.search(r"AUDIT_DEFER_DOMAIN_MILESTONE:\s*([\w-]+)\s*(?:—|--)?\s*(.+?)(?:\n|$)", body)
    if not match:
        return False, "", ""
    domain_key = match.group(1).strip()
    # Accept backend-domain-* prefix
    valid_keys = list(DOMAIN_REGISTRY.keys()) + [k for k in [domain_key] if k.startswith("backend-domain-")]
    if domain_key not in valid_keys:
        return False, domain_key, f"AUDIT_DEFER_DOMAIN_MILESTONE: unknown domain key '{domain_key}'. Valid: {sorted(DOMAIN_REGISTRY.keys())} or backend-domain-*"
    # Validate diff stays within domain path scope
    if domain_key.startswith("backend-domain-"):
        # Backend domain accepts kiteclass-core/kitehub/* — broad
        path_scopes = ["kiteclass/", "kitehub/"]
    else:
        path_scopes = DOMAIN_REGISTRY[domain_key]
    # Allowlist: docs/governance changes always OK alongside domain work
    docs_allow = ("documents/", ".claude/", "README", "CLAUDE.md", "MEMORY.md", ".github/")
    out_of_scope = []
    for f in files:
        if any(f.startswith(s) for s in path_scopes):
            continue
        if any(f.startswith(s) for s in docs_allow):
            continue
        out_of_scope.append(f)
    if out_of_scope:
        return False, domain_key, f"AUDIT_DEFER_DOMAIN_MILESTONE: '{domain_key}' but diff touches {len(out_of_scope)} file(s) outside domain scope (first 3: {out_of_scope[:3]}). Either remove out-of-scope changes or run audit per §2.1."
    return True, domain_key, ""


def has_domain_milestone_audit(pr: str) -> tuple[bool, str]:
    """Check DOMAIN_MILESTONE_AUDIT trailer (milestone wave audit reports).
    Returns (present_and_valid, reason)."""
    body = gh_run(["pr", "view", pr, "--json", "body", "--jq", ".body"]) or ""
    match = re.search(r"DOMAIN_MILESTONE_AUDIT:\s*([\w-]+)\s+(.+?)(?:\n|$)", body)
    if not match:
        return False, ""
    domain_key = match.group(1).strip()
    reports = [p.strip() for p in match.group(2).split(",") if p.strip()]
    if not reports:
        return False, f"DOMAIN_MILESTONE_AUDIT '{domain_key}' but no audit report paths listed"
    return True, f"{domain_key}: {len(reports)} reports"


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
    # GAP IDs are 3+ digits; \d{3,} (not \d{3}) so "GAP-1122" matches as one
    # token, not substring "GAP-112". Bug fixed 2026-06-10 (GAP-1129): unanchored
    # \d{3} collapsed every 4-digit gap to its 3-digit prefix → false drift on the
    # prefix gap (e.g. GAP-1122/1127/1128 all reported as GAP-112).
    gap_ids = sorted(set(re.findall(r"GAP-\d{3,}", combined)))
    if not gap_ids:
        return []
    repo_root = Path(__file__).resolve().parents[2]
    gaps_dir = repo_root / "documents" / "04-quality" / "gaps"
    warnings = []
    for gap_id in gap_ids:
        # Check 1: did the PR touch the gap file at all?
        touched = any(
            f.startswith("documents/04-quality/gaps/")
            and Path(f).name.startswith(f"{gap_id}-")
            for f in files
        )
        # Check 2: even if touched, does the gap Log reference this PR? (catches
        # touch-without-log mistakes — e.g. status bump but no log entry).
        # Recursive glob: gap files live in phase subdirs + closed/ archives per
        # gap-folder-organization.md v2.0.0 (not flat in gaps_dir). Non-recursive
        # glob missed them → log_has_pr never set → false "Log doesn't mention" warns.
        gap_files = list(gaps_dir.glob(f"**/{gap_id}-*.md"))
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


def check_ui_kits_integration(pr: str, info: dict, files: list[str]) -> dict | None:
    """Check UI Kits integration smoke test confirmation per GAP-265.

    Returns dict with 'level' ('block' | 'warn' | 'pass') and 'message'.
    Returns None if rule does not apply (no ui_kits/ files touched).

    Logic:
    1. Filter files to those under trigger_paths excluding trigger_exclude.
    2. If none match → rule N/A (return None).
    3. Read PR body. If override trailer present → warn (downgraded).
    4. If required phrase present → pass.
    5. Otherwise → block.
    """
    rule = UI_KITS_INTEGRATION_RULE

    # Step 1+2: filter files by trigger paths
    triggered_files = []
    for f in files:
        if not any(f.startswith(p) for p in rule["trigger_paths"]):
            continue
        if any(f.startswith(p) for p in rule["trigger_exclude"]):
            continue
        triggered_files.append(f)

    if not triggered_files:
        return None  # Rule does not apply

    # Step 3: read PR body
    body = info.get("body", "") or gh_run(["pr", "view", pr, "--json", "body", "--jq", ".body"]) or ""

    # Override trailer check
    override_match = re.search(rf"{re.escape(rule['override_trailer'])}\s*(.+?)(?:\n|$)", body)
    if override_match:
        reason = override_match.group(1).strip()
        return {
            "level": "warn",
            "message": (
                f"UI kits integration override: {reason} "
                f"({len(triggered_files)} file(s) under ui_kits/, "
                f"{rule['override_trailer']} trailer present)"
            ),
        }

    # Required phrase check
    if any(phrase in body for phrase in rule["required_phrases"]):
        return {"level": "pass", "message": ""}

    # Block
    return {
        "level": "block",
        "message": (
            f"UI kits PR missing integration smoke test confirmation "
            f"({len(triggered_files)} file(s) under ui_kits/). "
            f"Add 'Integration smoke test:' line to PR body OR add "
            f"'{rule['override_trailer']} <reason>' trailer for docs-only edits. "
            f"See {rule['rationale_url']} (incident: {rule['incident_ref']})."
        ),
    }


# ── GAP-751 Option A: Auto-close referenced gaps on PR merge ────
#
# When a PR body contains markers `Closes: GAP-NNN` / `Resolves: GAP-NNN` /
# `Refs: GAP-NNN`, this function auto-flips the matching CSV row + appends a
# Log entry to the gap markdown file + git mv to phase-X/closed/.
#
# Rationale (per GAP-751): Wave br-7 closure surfaced 4/5 buckets stale-gap
# pattern — code shipped Wave 5 Sub-PR 5.6b era ~30 ngày trước nhưng CSV vẫn
# OPEN P0. Hook eliminates manual CSV sync by linking PR body marker → CSV +
# gap file + git mv in a single PostToolUse event on merge.
#
# Soft-fail behavior: hook never raises; CSV/file/git-mv failures only emit
# WARN to stdout (caller surfaces via systemMessage), letting the merge stand.

GAP_MARKER_RE = re.compile(
    r"\b(Closes|Resolves|Refs)\s*:\s*(GAP-\d+[a-z]*(?:-\d+)?)",
    re.IGNORECASE,
)

GAPS_DIR = PROJECT_ROOT / "documents" / "04-quality" / "gaps"
GAP_STATUS_CSV = GAPS_DIR / "gap-status.csv"

# Status values eligible for auto-flip per gap-architecture-v2.md §2
AUTO_FLIPPABLE_STATUSES = {"OPEN", "PARTIAL", "IN_PROGRESS", "PENDING", "PLANNED"}


def _parse_csv_row(line: str) -> list[str]:
    """Parse a single CSV row. Project CSV uses simple comma-separation (no
    embedded commas with quotes in canonical rows per audit). Best-effort csv
    module fallback if needed."""
    import csv
    import io
    reader = csv.reader(io.StringIO(line))
    try:
        return next(reader)
    except StopIteration:
        return []


def _format_csv_row(fields: list[str]) -> str:
    """Format a list of fields back into a single CSV row line. Matches the
    project's simple-comma convention; if any field contains a comma, quote it."""
    import csv
    import io
    buf = io.StringIO()
    writer = csv.writer(buf, lineterminator="")
    writer.writerow(fields)
    return buf.getvalue()


def _find_csv_row(gap_id: str) -> tuple[int, list[str]] | None:
    """Return (line_index_0based, fields_list) for the gap_id row, or None."""
    if not GAP_STATUS_CSV.is_file():
        return None
    try:
        lines = GAP_STATUS_CSV.read_text(encoding="utf-8").splitlines()
    except OSError:
        return None
    for i, line in enumerate(lines):
        if not line or line.startswith("#"):
            continue
        # Cheap prefix check before parsing
        if not line.startswith(gap_id + ","):
            continue
        fields = _parse_csv_row(line)
        if fields and fields[0] == gap_id:
            return i, fields
    return None


def _find_gap_file(gap_id: str, csv_filename: str | None = None) -> Path | None:
    """Resolve gap markdown file path. Prefer CSV filename column; fallback
    to glob across known subdirs."""
    if csv_filename:
        candidate = GAPS_DIR / csv_filename
        if candidate.is_file():
            return candidate
    # Fallback: glob phase-X/ + phase-X/closed/ + unclassified/ + root
    search_dirs = [
        GAPS_DIR / "phase-1-beta",
        GAPS_DIR / "phase-1.5-paid",
        GAPS_DIR / "phase-2",
        GAPS_DIR / "phase-3",
        GAPS_DIR / "unclassified",
        GAPS_DIR / "phase-1-beta" / "closed",
        GAPS_DIR / "phase-1.5-paid" / "closed",
        GAPS_DIR / "phase-2" / "closed",
        GAPS_DIR / "phase-3" / "closed",
        GAPS_DIR / "unclassified" / "closed",
        GAPS_DIR / "closed",
        GAPS_DIR,
    ]
    for d in search_dirs:
        if not d.is_dir():
            continue
        for match in d.glob(f"{gap_id}-*.md"):
            return match
    return None


def _append_log_entry(gap_file: Path, pr: str, marker_type: str, today: str) -> bool:
    """Append a closure Log entry to the gap markdown file. Returns True on
    success, False on failure. Idempotent: if the file's tail already cites
    `PR #<pr> auto-close`, skip."""
    try:
        content = gap_file.read_text(encoding="utf-8")
    except OSError:
        return False
    # Idempotency check — don't double-append
    if f"PR #{pr} auto-close" in content:
        return True
    verb = "Flipped DONE 100%" if marker_type.lower() in ("closes", "resolves") else "Auto-verified"
    entry = (
        f"\n- **{today} (PR #{pr} auto-close):** {verb} — closed by PR #{pr} "
        f"via \"{marker_type}: {gap_file.stem.split('-')[0]}-{gap_file.stem.split('-')[1]}\" "
        f"marker in PR body. CSV row updated + file moved to phase-X/closed/ "
        f"per `gap-folder-organization.md` v2.0.0 §3.3 + `gap-done-discipline.md` §2.\n"
    )
    # Append at end of file (ensures Log section catches new entry if last section)
    new_content = content.rstrip() + "\n" + entry
    try:
        gap_file.write_text(new_content, encoding="utf-8")
        return True
    except OSError:
        return False


def _move_to_closed(gap_file: Path) -> Path | None:
    """git mv gap_file → <parent>/closed/<basename>. Returns new path or None.
    If file already under closed/, no-op return same path."""
    if gap_file.parent.name == "closed":
        return gap_file
    target_dir = gap_file.parent / "closed"
    target = target_dir / gap_file.name
    try:
        target_dir.mkdir(parents=True, exist_ok=True)
        # Prefer git mv to preserve history; fallback to plain rename
        result = subprocess.run(
            ["git", "mv", str(gap_file), str(target)],
            capture_output=True,
            text=True,
            timeout=10,
            cwd=PROJECT_ROOT,
            check=False,
        )
        if result.returncode != 0:
            # Fallback: plain rename + git add (handles untracked / new files)
            gap_file.rename(target)
            subprocess.run(
                ["git", "add", str(target)],
                capture_output=True,
                timeout=5,
                cwd=PROJECT_ROOT,
                check=False,
            )
        return target
    except (OSError, subprocess.SubprocessError):
        return None


def _update_csv_row(
    line_index: int,
    fields: list[str],
    new_status: str,
    new_completion: str,
    today: str,
    new_filename: str | None = None,
) -> bool:
    """Mutate fields in-place + rewrite CSV. Returns True on success."""
    # Column layout per gap-architecture-v2.md §2:
    # 1:id 2:filename 3:title_short 4:status 5:priority 6:domain 7:phase
    # 8:completion_pct 9:found_date 10:last_verified 11:notes
    if len(fields) < 10:
        return False
    fields[3] = new_status        # status
    fields[7] = new_completion    # completion_pct
    fields[9] = today              # last_verified
    if new_filename is not None and len(fields) >= 2:
        fields[1] = new_filename  # filename
    try:
        lines = GAP_STATUS_CSV.read_text(encoding="utf-8").splitlines()
    except OSError:
        return False
    if line_index >= len(lines):
        return False
    lines[line_index] = _format_csv_row(fields)
    try:
        GAP_STATUS_CSV.write_text("\n".join(lines) + "\n", encoding="utf-8")
        # Auto-stage for inclusion in next commit (consistent with PR-log staging).
        with contextlib.suppress(Exception):
            subprocess.run(
                ["git", "add", str(GAP_STATUS_CSV)],
                capture_output=True,
                timeout=5,
                cwd=PROJECT_ROOT,
                check=False,
            )
        return True
    except OSError:
        return False


def auto_close_referenced_gaps(pr: str, pr_body: str) -> list[str]:
    """Scan PR body for marker patterns and auto-flip referenced gaps.

    Returns a list of human-readable result lines (one per gap processed).
    Soft-fails: never raises; missing CSV row OR missing gap file emit WARN
    in the result list but do not abort the merge.
    """
    if not pr_body:
        return []
    matches = GAP_MARKER_RE.findall(pr_body)
    if not matches:
        return []
    # Deduplicate while preserving order; key on (marker_type_normalised, gap_id)
    seen: set[tuple[str, str]] = set()
    ordered: list[tuple[str, str]] = []
    for marker_type, gap_id in matches:
        key = (marker_type.lower(), gap_id)
        if key in seen:
            continue
        seen.add(key)
        ordered.append((marker_type, gap_id))

    today = datetime.now().strftime("%Y-%m-%d")
    results: list[str] = []

    for marker_type, gap_id in ordered:
        is_closing = marker_type.lower() in ("closes", "resolves")

        row = _find_csv_row(gap_id)
        if row is None:
            results.append(f"  WARN [{gap_id}]: no CSV row found — manual sync needed")
            continue
        line_index, fields = row
        current_status = fields[3] if len(fields) > 3 else ""
        csv_filename = fields[1] if len(fields) > 1 else None
        gap_file = _find_gap_file(gap_id, csv_filename)

        if is_closing:
            # Only flip if status is in flippable set; skip if already DONE
            if current_status == "DONE":
                results.append(f"  SKIP [{gap_id}]: already DONE")
                continue
            if current_status not in AUTO_FLIPPABLE_STATUSES:
                results.append(
                    f"  SKIP [{gap_id}]: status={current_status} not in flippable set "
                    f"({sorted(AUTO_FLIPPABLE_STATUSES)})"
                )
                continue

            # Move file to closed/
            new_path: Path | None = None
            new_csv_filename: str | None = None
            if gap_file is not None:
                new_path = _move_to_closed(gap_file)
                if new_path is not None:
                    try:
                        new_csv_filename = str(new_path.relative_to(GAPS_DIR))
                    except ValueError:
                        new_csv_filename = None
                # Append closure Log entry (use the new path if moved, else original)
                target_for_log = new_path if new_path is not None else gap_file
                _append_log_entry(target_for_log, pr, marker_type, today)
            else:
                results.append(f"  WARN [{gap_id}]: gap file not found — CSV-only flip")

            # CSV row flip
            ok = _update_csv_row(
                line_index, fields,
                new_status="DONE",
                new_completion="100",
                today=today,
                new_filename=new_csv_filename,
            )
            if ok:
                results.append(
                    f"  FLIPPED [{gap_id}] {current_status}→DONE 100% "
                    f"(file→{new_csv_filename or 'unchanged'})"
                )
            else:
                results.append(f"  ERROR [{gap_id}]: CSV update failed — manual sync needed")
        else:
            # Refs:GAP-NNN — bump last_verified only; do NOT flip status
            ok = _update_csv_row(
                line_index, fields,
                new_status=current_status,
                new_completion=fields[7] if len(fields) > 7 else "0",
                today=today,
                new_filename=None,
            )
            if ok:
                results.append(f"  VERIFIED [{gap_id}] last_verified={today} (status unchanged)")
            else:
                results.append(f"  ERROR [{gap_id}]: last_verified update failed")

    return results


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
    # Refresh session telemetry on merge — captures end-of-session state.
    log["session_telemetry"] = build_session_telemetry()

    # GAP-193 Phase 2 — non-blocking session-lock check. We only surface as a
    # systemMessage line; merge-blocking lives in the commit/edit hooks.
    lock_ok, lock_msg = run_session_lock_guard()

    log = add_event(log, "PR_MERGED", {
        "ci_status": ci_status,
        "files_changed": len(files),
        "java_files": len(java_files),
        "test_files": len(test_files),
        "business_docs": docs_ok,
        "missing_audits": [a["audit"] for a in missing_audits],
    })

    write_pr_log(pr, log)

    # GAP-751 Option A — auto-close referenced gaps via PR body markers
    # (`Closes:`/`Resolves:`/`Refs:` GAP-NNN). Soft-fail: errors emit WARN
    # in the systemMessage but never block the merge.
    autoclose_results: list[str] = []
    try:
        pr_body = info.get("body", "") or gh_run(["pr", "view", pr, "--json", "body", "--jq", ".body"]) or ""
        autoclose_results = auto_close_referenced_gaps(pr, pr_body)
    except Exception as exc:
        autoclose_results = [f"  ERROR auto-close failed: {exc}"]

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
    # GAP-265 — UI kits integration smoke test enforcement.
    ui_kits_result = check_ui_kits_integration(pr, info, files)
    ui_kits_blocked = False
    if ui_kits_result and ui_kits_result["level"] == "block":
        violations.append(f"UI kits integration: {ui_kits_result['message']}")
        ui_kits_blocked = True
    elif ui_kits_result and ui_kits_result["level"] == "warn":
        violations.append(f"UI kits integration: {ui_kits_result['message']}")
    if is_wave:
        violations.append("Wave merge — run /wave-completion-check + audit suite within 3 days (post-wave-audit-mandate.md)")
    if not lock_ok and lock_msg:
        # Informational — same branch claimed by another session at merge time.
        violations.append(f"Session-lock notice: {lock_msg.splitlines()[0] if lock_msg else 'foreign session lock detected'}")

    if not violations:
        msg = f"PR #{pr} merged — compliance {log['compliance_score']}. Log: pr-logs/PR-{pr}.json"
        if autoclose_results:
            msg += (
                f"\n[audit-gate] Auto-closed {len(autoclose_results)} gap reference(s) per PR #{pr} markers:\n"
                + "\n".join(autoclose_results)
            )
        return {"systemMessage": msg}

    # Check AUDIT_OVERRIDE
    overridden, override_reason = (False, "")
    if missing_audits and not docs_only:
        overridden, override_reason = has_audit_override(pr, info)

    # Check AUDIT_DEFER_DOMAIN_MILESTONE per post-wave-audit-mandate.md §2.4 (v1.1.0)
    domain_deferred, domain_key, domain_err = (False, "", "")
    if missing_audits and not docs_only and not overridden:
        domain_deferred, domain_key, domain_err = has_domain_milestone_defer(pr, files)
    # Check DOMAIN_MILESTONE_AUDIT (milestone wave running deferred audit)
    milestone_audit_ok, milestone_reason = has_domain_milestone_audit(pr)

    lines = [f"PR #{pr} merged — {len(violations)} violation(s) detected:"]
    for v in violations:
        lines.append(f"  - {v}")
    if docs_only:
        lines.append("  (docs-only PR — audit/CI checks skipped)")
    if overridden:
        lines.append(f"  ⚠️  AUDIT_OVERRIDE present: {override_reason}")
    if domain_deferred:
        lines.append(f"  ℹ️  AUDIT_DEFER_DOMAIN_MILESTONE: '{domain_key}' — audit deferred to milestone wave (per post-wave-audit-mandate.md §2.4)")
    if domain_err:
        lines.append(f"  ❌ AUDIT_DEFER_DOMAIN_MILESTONE invalid: {domain_err}")
    if milestone_audit_ok:
        lines.append(f"  ✅ DOMAIN_MILESTONE_AUDIT present: {milestone_reason}")
    if autoclose_results:
        lines.append(f"\n[audit-gate] Auto-closed {len(autoclose_results)} gap reference(s) per PR #{pr} markers:")
        lines.extend(autoclose_results)
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
    if missing_audits and not docs_only and not overridden and not domain_deferred:
        should_block = True
        block_reasons.append(f"missing audits: {', '.join(a['audit'] for a in missing_audits)}")
    # Domain-deferral with INVALID trailer (typo or out-of-scope diff) → BLOCK
    if domain_err:
        should_block = True
        block_reasons.append(domain_err)
    if ui_kits_blocked:
        should_block = True
        block_reasons.append("UI kits integration smoke test missing (GAP-265)")

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
