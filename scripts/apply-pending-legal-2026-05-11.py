#!/usr/bin/env python3
"""Apply 2026-05-11 legal/compliance gap deferral to gap-status.csv.

29 gaps moved from `documents/04-quality/gaps/` → `pending/` subfolder.
Solo-dev decision: KiteClass chưa cần compliance toàn diện trong Phase 1 BETA.
See `documents/04-quality/gaps/pending/README.md` for rationale + risks +
re-evaluation triggers.

CSV updates:
- filename: GAP-XXX-*.md → pending/GAP-XXX-*.md
- status: any → PENDING
- completion_pct: → 0
- last_verified: → 2026-05-11
- notes: appended "pending-legal-2026-05-11: <category>"

Run: python3 scripts/apply-pending-legal-2026-05-11.py [--check]
"""
from __future__ import annotations

import csv
import io
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CSV_PATH = REPO / "documents" / "04-quality" / "gaps" / "gap-status.csv"
TODAY = "2026-05-11"
NOTE_PREFIX = "pending-legal-2026-05-11"

# Gap → (category, law)
PENDING_LEGAL: dict[str, tuple[str, str]] = {
    # PDPL — Nghị định 13/2023
    "GAP-182": ("PDPL", "Privacy Policy"),
    "GAP-184": ("PDPL", "Data Retention + Deletion"),
    "GAP-301": ("PDPL", "Tenant Data Export DSAR"),
    "GAP-321c": ("PDPL", "Parent Portal Phase 1C consent"),
    "GAP-353": ("PDPL", "Cookie Consent Banner"),
    "GAP-353b-server-consent-api-audit-log": ("PDPL", "Server Consent API"),
    "GAP-353b-followup-multi-device-and-audit-chain": ("PDPL", "Multi-device + Audit Chain"),
    # Luật Trẻ em 2016
    "GAP-186": ("Luật Trẻ em", "Child Protection Policy"),
    "GAP-322": ("Luật Trẻ em", "Child Protection Workflow"),
    "GAP-322b": ("Luật Trẻ em", "Vetting + MinIO RBAC"),
    "GAP-322c": ("Luật Trẻ em", "Mandatory Reporting + Hash Audit"),
    "GAP-359": ("Luật Trẻ em", "Phase 1C Remainder"),
    # Luật Giáo dục 2019 + MOET
    "GAP-321": ("Luật Giáo dục", "Parent Portal v1 LEGAL MANDATE"),
    "GAP-326": ("MOET", "School License Verification"),
    "GAP-327": ("MOET", "Subject Taxonomy Seed"),
    "GAP-336": ("MOET", "Financial Report TT 107/200"),
    "GAP-340": ("MOET", "Inter-school Transfer API"),
    "GAP-341": ("Luật Giáo dục", "Phổ cập Escalation"),
    "GAP-343": ("Luật Giáo dục", "Học bạ + Bằng tốt nghiệp Sealed PDF"),
    "GAP-361": ("Luật Giáo dục", "Parent Portal Phase 1C Remainder"),
    # Tax + Lao động + Lưu trữ
    "GAP-185": ("Tax", "VAT/TCT Billing Terms"),
    "GAP-306": ("Tax + Lao động", "Teacher Commission BHXH/BHYT/TNCN"),
    "GAP-317": ("Lao động", "Staff Offboard Wizard"),
    "GAP-319": ("Tax", "WORM Audit Log 10-Year"),
    "GAP-344": ("Luật Lưu trữ", "School Closure 30y Archive"),
    # Legal audit + Sổ đầu bài
    "GAP-156": ("Legal audit", "Business Rules Compliance Audit (Phase 2 counsel)"),
    "GAP-333": ("Luật Giáo dục", "Sổ đầu bài Digital"),
    "GAP-345": ("Legal audit", "K-12 LEGAL Trio State-Check"),
    "GAP-335": ("Luật Giáo dục", "Public/Private School Fee Compliance"),
}


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

    changes = 0
    seen_ids = {r["id"] for r in rows}
    not_found = [gid for gid in PENDING_LEGAL if gid not in seen_ids]

    for r in rows:
        gid = r["id"]
        if gid not in PENDING_LEGAL:
            continue
        category, label = PENDING_LEGAL[gid]
        # Update filename to pending/ subpath
        if not r["filename"].startswith("pending/"):
            r["filename"] = f"pending/{r['filename']}"
        r["status"] = "PENDING"
        r["completion_pct"] = "0"
        note = f"{NOTE_PREFIX}: {category} — {label}"
        r["notes"] = note if not r["notes"] else f"{r['notes']}; {note}"
        r["last_verified"] = TODAY
        changes += 1

    print("=== pending-legal-2026-05-11 dry-run ===", file=sys.stderr)
    print(f"  CSV rows updated:    {changes}", file=sys.stderr)
    print(f"  Expected (PENDING_LEGAL count): {len(PENDING_LEGAL)}", file=sys.stderr)
    if not_found:
        print(f"  WARNING — ids referenced but not in CSV: {not_found}", file=sys.stderr)

    if check_only:
        print("\nDry-run only. Drop --check to write.", file=sys.stderr)
        return 0

    # Bump header comment
    for i, line in enumerate(header_comments):
        if line.startswith("# Rows:"):
            header_comments[i] = f"# Rows: {len(rows)} (Phase 2 bulk 2026-05-11; audit reclass + pending-legal-2026-05-11 {changes} legal gaps moved to pending/)."

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
