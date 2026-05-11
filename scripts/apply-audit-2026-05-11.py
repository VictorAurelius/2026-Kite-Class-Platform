#!/usr/bin/env python3
"""Apply 2026-05-11 gap audit findings to gap-status.csv.

3 parallel Explore-agent audit reports consolidated:
- Cluster A (.vn domain): 7 KEEP, 2 UPDATE-CONTENT (deferred to follow-up gap)
- Cluster B (49 OPEN n/a ≥22d): 34 phase reclassifications + 3 WONTFIX
- Cluster C (30 PARTIAL ≥21d): 7 completion_pct refinements (still PARTIAL)

This script is the canonical record of the audit-2026-05-11 reclassification
decisions. Re-running is safe — sets last_verified=2026-05-11 on every changed
row. Pass --check to dry-run.

Run: python3 scripts/apply-audit-2026-05-11.py [--check]
"""
from __future__ import annotations

import csv
import io
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CSV_PATH = REPO / "documents" / "04-quality" / "gaps" / "gap-status.csv"
TODAY = "2026-05-11"

# (id, new_phase) — 34 phase reclassifications per agent B
PHASE_RECLASS = {
    # phase-1-beta (+1)
    "GAP-080": "phase-1-beta",
    # phase-1.5-paid (+5)
    "GAP-023": "phase-1.5-paid",
    "GAP-110": "phase-1.5-paid",
    "GAP-123": "phase-1.5-paid",
    "GAP-124": "phase-1.5-paid",
    "GAP-125": "phase-1.5-paid",
    # phase-2 (+22)
    "GAP-003": "phase-2", "GAP-004": "phase-2", "GAP-017": "phase-2",
    "GAP-019": "phase-2", "GAP-022": "phase-2", "GAP-024": "phase-2",
    "GAP-025": "phase-2", "GAP-026": "phase-2", "GAP-027": "phase-2",
    "GAP-028": "phase-2", "GAP-029": "phase-2", "GAP-030": "phase-2",
    "GAP-034": "phase-2", "GAP-035": "phase-2", "GAP-036": "phase-2",
    "GAP-038": "phase-2", "GAP-039": "phase-2", "GAP-044": "phase-2",
    "GAP-071": "phase-2", "GAP-072": "phase-2", "GAP-074": "phase-2",
    "GAP-075": "phase-2",
    # phase-3 K-12 MOET (+6)
    "GAP-055": "phase-3", "GAP-056": "phase-3", "GAP-059": "phase-3",
    "GAP-060": "phase-3", "GAP-061": "phase-3", "GAP-062": "phase-3",
}

# (id, new_status) — 3 WONTFIX flips per agent B
WONTFIX = {
    "GAP-020": "AI Wizard advanced UX deferred — Phase 1 AI Branding = minimal logo+color only (per release-1-plan §6.1)",
    "GAP-045": "Template marketplace forever-defer (community feature, post-GA per release-1-plan §7.2)",
    "GAP-064": "SCORM/xAPI forever-defer (enterprise LMS feature, not SMB center MVP per release-1-plan §7.2)",
}

# (id, new_completion_pct) — 7 PARTIAL near-DONE per agent C
# Kept as PARTIAL (not flipped DONE) — user can verify per-gap §2 discipline.
COMPLETION_REFINE = {
    "GAP-033": 95,
    "GAP-049": 95,
    "GAP-050": 95,
    "GAP-102": 95,
    "GAP-112": 90,
    "GAP-114": 90,
    "GAP-116-pii-scrubbing-logs": 90,
}

AUDIT_NOTE = "audit-2026-05-11"


def main() -> int:
    check_only = "--check" in sys.argv

    header_comments: list[str] = []
    header_line = ""
    rows: list[dict] = []
    fieldnames = ["id", "filename", "title_short", "status", "priority", "domain",
                  "phase", "completion_pct", "found_date", "last_verified", "notes"]

    with CSV_PATH.open() as f:
        for line in f:
            if line.startswith("#"):
                header_comments.append(line.rstrip("\n"))
                continue
            if line.startswith("id,"):
                header_line = line.rstrip("\n")
                continue
            if not line.strip():
                continue
            for r in csv.reader([line]):
                if r and r[0].startswith("GAP-"):
                    rows.append(dict(zip(fieldnames, r, strict=False)))

    changes_phase = 0
    changes_wontfix = 0
    changes_completion = 0
    not_found = []

    seen_ids = {r["id"] for r in rows}

    for r in rows:
        gid = r["id"]
        changed = False

        if gid in PHASE_RECLASS:
            new_phase = PHASE_RECLASS[gid]
            if r["phase"] != new_phase:
                r["phase"] = new_phase
                changes_phase += 1
                changed = True

        if gid in WONTFIX and r["status"] != "WONTFIX":
            r["status"] = "WONTFIX"
            r["completion_pct"] = "0"
            note = WONTFIX[gid]
            r["notes"] = f"{AUDIT_NOTE}: {note}" if not r["notes"] else f"{r['notes']}; {AUDIT_NOTE}: {note}"
            changes_wontfix += 1
            changed = True

        if gid in COMPLETION_REFINE:
            new_pct = str(COMPLETION_REFINE[gid])
            if r["completion_pct"] != new_pct and r["status"] == "PARTIAL":
                r["completion_pct"] = new_pct
                changes_completion += 1
                changed = True

        if changed:
            r["last_verified"] = TODAY

    # Identify referenced ids not in CSV
    referenced = set(PHASE_RECLASS) | set(WONTFIX) | set(COMPLETION_REFINE)
    not_found = [gid for gid in referenced if gid not in seen_ids]

    print("=== audit-2026-05-11 reclassification dry-run ===", file=sys.stderr)
    print(f"  Phase reclassifications:    {changes_phase}", file=sys.stderr)
    print(f"  WONTFIX flips:              {changes_wontfix}", file=sys.stderr)
    print(f"  completion_pct refinements: {changes_completion}", file=sys.stderr)
    if not_found:
        print(f"  WARNING — ids referenced but not in CSV: {not_found}", file=sys.stderr)

    if check_only:
        print("\nDry-run only. Drop --check to write.", file=sys.stderr)
        return 0

    # Update header comments — bump Last-Verified note
    for i, line in enumerate(header_comments):
        if line.startswith("# Rows:"):
            header_comments[i] = f"# Rows: {len(rows)} (Phase 2 bulk migration 2026-05-11; audit-2026-05-11 reclassification {changes_phase}+{changes_wontfix}+{changes_completion} changes)."

    # Write
    buf = io.StringIO()
    writer = csv.writer(buf, lineterminator="\n")
    for r in rows:
        writer.writerow([r[k] for k in fieldnames])

    output = "\n".join(header_comments) + "\n" + header_line + "\n" + buf.getvalue()
    CSV_PATH.write_text(output, encoding="utf-8")
    print(f"\nWrote {CSV_PATH}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
