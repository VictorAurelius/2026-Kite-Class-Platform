#!/usr/bin/env python3
"""Bulk-extract gap markdown frontmatter → gap-status.csv rows.

Phase 2 migration tool per `.claude/rules/gap-architecture-v2.md` §4.
Preserves existing pilot rows (5 from PR #1159); appends new rows for
remaining active gap files. Idempotent — running twice is safe.

Run from repo root:
    python3 scripts/migrate-gaps-to-csv.py            # dry-run, prints diff
    python3 scripts/migrate-gaps-to-csv.py --write    # rewrite CSV
"""
from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
GAPS_DIR = REPO / "documents" / "04-quality" / "gaps"
CSV_PATH = GAPS_DIR / "gap-status.csv"

TODAY = "2026-05-11"

# PARTIAL beats DONE because a PARTIAL gap often cites "Phase X DONE" inline —
# the gap as a whole remains PARTIAL until ALL phases ship.
STATUS_PRIORITY = ["PARTIAL", "IN_PROGRESS", "PLANNED", "DONE", "WONTFIX", "OPEN"]
ID_RE = re.compile(r"^(GAP-\d+[a-z]?(?:-\d+)?)")
DATE_RE = re.compile(r"\b(20\d{2}-\d{2}-\d{2})\b")

DOMAIN_MAP = [
    # ordered: first match wins (Compliance/Legal beats Backend etc.)
    # value is the canonical CSV-enum casing
    ("Compliance", ["pdpl", "legal", "compliance", "consent", "moet", "retention", "data protection"]),
    ("AI", ["ai branding", "ai-branding", "ollama", "ai queue", "gemma", "llm"]),
    ("Meta", ["meta", "skill", "rule", "governance", "tooling", "workflow", "test infra"]),
    ("DevOps", ["devops", "infrastructure", "ci/cd", "helm", "terraform", "kubernetes", "k8s", "docker", "ops", "sre", "supply chain", "deploy"]),
    ("Frontend", ["frontend", "ui", "ux", "react", "next.js", "nextjs", "design system", "kit"]),
    ("Backend", ["backend", "api", "database", "data", "auth", "billing"]),
]


def normalize_status(text: str) -> str:
    """Pick highest-priority status keyword from a status block."""
    upper = text.upper().replace("IN PROGRESS", "IN_PROGRESS")
    for kw in STATUS_PRIORITY:
        if re.search(rf"\b{kw}\b", upper):
            return kw
    return "OPEN"


def normalize_priority(text: str) -> str:
    for p in ("P0", "P1", "P2", "P3"):
        if re.search(rf"\b{p}\b", text):
            return p
    return "P2"  # safe default


def normalize_domain(text: str) -> str:
    low = text.lower()
    slash_count = low.count("/")
    if slash_count >= 2:
        return "Mixed"
    for canonical, keywords in DOMAIN_MAP:
        for kw in keywords:
            if kw in low:
                return canonical
    return "Backend"


def infer_phase(content: str, priority: str, filename: str = "") -> str:
    """Conservative-but-richer heuristic: assign phase when content explicitly cites it.

    Scans full file content + filename for explicit phase signals.
    Order matters — most specific phase wins. Default n/a when zero signal.

    Filename convention `GAP-NNN-p3-*` indicates author-tagged Phase 3 K-12.
    """
    low = content.lower()
    fname_low = filename.lower()

    # filename author-tag: `GAP-NNN-p3-*` → phase-3 K-12 LEGAL
    if re.search(r"^gap-\d+[a-z]?-p3-", fname_low):
        return "phase-3"

    # phase-1.5-paid — strongest signal
    if any(k in low for k in ("phase 1.5", "phase-1.5", "phase 1.5 paid",
                              "p1.5", "phase 1.5 launch", "phase 1.5 paid launch")):
        return "phase-1.5-paid"

    # phase-3 K-12 minors LEGAL
    phase_3_signals = (
        "phase 3 k-12", "phase-3 k-12", "k-12 phase 3", "k12 phase 3",
        "phase 3 minor", "luật trẻ em", "luật giáo dục", "mandatory.reporting",
        "k-12 legal", "phocap escalation", "moet inter-school",
    )
    if any(k in low for k in phase_3_signals):
        return "phase-3"

    # phase-2 ramp / medium-center
    if any(k in low for k in ("phase 2 ramp", "phase-2 ramp",
                              "medium-center phase 2", "phase 2 medium-center")):
        return "phase-2"

    # phase-1-beta — broad signals; this is the current active phase per CLAUDE.md
    phase_1_beta_signals = (
        "phase 1 beta", "phase-1-beta", "p1 beta", "phase-1 beta",
        "blocking phase 1 beta", "phase 1 beta blocker", "phase 1 beta required",
        "phase 1 beta launch", "phase 1 beta hard deadline",
        "phase 1 beta deploy", "phase 1 beta milestone",
        "release 1 phase 1", "release lần 1 phase 1",
        "p1+p2 soft launch", "p1 + p2 soft launch",
    )
    if any(k in low for k in phase_1_beta_signals):
        return "phase-1-beta"

    return "n/a"


def completion_for(status: str) -> int:
    return {"OPEN": 0, "DONE": 100, "PARTIAL": 50, "IN_PROGRESS": 40, "PLANNED": 10, "WONTFIX": 0}.get(status, 0)


def extract_title(lines: list[str], gap_id: str) -> str:
    for line in lines[:5]:
        if line.startswith("# "):
            t = line[2:].strip()
            t = re.sub(rf"^{re.escape(gap_id)}\s*:\s*", "", t)
            if len(t) > 80:
                t = t[:77].rstrip() + "..."
            # CSV-safe: collapse commas to semicolons (CSV uses comma delim)
            return t.replace(",", ";")
    return gap_id


def extract_field_block(lines: list[str], label: str) -> str:
    """Return the **Label:** value line plus any indented continuation lines."""
    pattern = re.compile(rf"^\*\*{label}:\*\*\s*(.*)")
    capturing = False
    collected: list[str] = []
    for _i, line in enumerate(lines[:40]):
        m = pattern.match(line)
        if m:
            collected.append(m.group(1))
            capturing = True
            continue
        if capturing:
            # continuation if line begins with "- " or is indented or is a bullet — until next **Field:**
            if line.startswith("**") and ":**" in line[:30]:
                break
            if line.strip() == "" and len(collected) >= 1:
                # blank line ends block UNLESS preceded by a "-" bullet continuation
                if not (collected and collected[-1].strip().endswith(",")):
                    break
                continue
            collected.append(line.strip())
    return " ".join(collected).strip()


def extract_found_date(lines: list[str]) -> str | None:
    for label in ("Found", "Detected", "Created"):
        block = extract_field_block(lines, label)
        if block:
            m = DATE_RE.search(block)
            if m:
                return m.group(1)
    # fallback: scan first 25 lines
    for line in lines[:25]:
        m = DATE_RE.search(line)
        if m:
            return m.group(1)
    return "2026-04-14"  # earliest known gap date as safe floor


def extract_gap_row(path: Path, gap_id: str | None = None) -> dict | None:
    name = path.name
    if gap_id is None:
        m = ID_RE.match(name)
        if not m:
            return None
        gap_id = m.group(1)

    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()

    status_block = extract_field_block(lines, "Status")
    priority_block = extract_field_block(lines, "Priority")
    domain_block = extract_field_block(lines, "Domain")

    status = normalize_status(status_block)
    priority = normalize_priority(priority_block)
    domain = normalize_domain(domain_block)
    phase = infer_phase(text, priority, filename=name)
    completion = completion_for(status)
    found = extract_found_date(lines)
    title = extract_title(lines, gap_id)

    return {
        "id": gap_id,
        "filename": name,
        "title_short": title,
        "status": status,
        "priority": priority,
        "domain": domain,
        "phase": phase,
        "completion_pct": str(completion),
        "found_date": found,
        "last_verified": TODAY,
        "notes": "",
    }


def load_existing_csv() -> tuple[list[str], dict[str, dict], list[str]]:
    """Return (header_comments, existing_rows_by_id, header_line)."""
    if not CSV_PATH.exists():
        return [], {}, ""
    header_comments: list[str] = []
    header_line = ""
    rows: dict[str, dict] = {}
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
            # parse via csv module on this single line
            for row in csv.reader([line]):
                if row and row[0].startswith("GAP-"):
                    rows[row[0]] = dict(zip(
                        ["id", "filename", "title_short", "status", "priority", "domain",
                         "phase", "completion_pct", "found_date", "last_verified", "notes"],
                        row, strict=False,
                    ))
    return header_comments, rows, header_line


def gap_id_sort_key(gid: str) -> tuple:
    """Sort GAP-005 < GAP-005a < GAP-321b < GAP-321b-1 < GAP-353b."""
    m = re.match(r"GAP-(\d+)([a-z]?)(?:-(\d+))?", gid)
    if not m:
        return (99999, "", 0)
    return (int(m.group(1)), m.group(2) or "", int(m.group(3) or 0))


def main() -> int:
    write_mode = "--write" in sys.argv

    header_comments, existing, header_line = load_existing_csv()
    if not header_line:
        header_line = "id,filename,title_short,status,priority,domain,phase,completion_pct,found_date,last_verified,notes"

    active_files = sorted(GAPS_DIR.glob("GAP-*.md"))
    print(f"Scanning {len(active_files)} active gap files...", file=sys.stderr)

    # First pass: group by numeric/letter prefix to detect collisions
    from collections import defaultdict
    by_prefix: dict[str, list[Path]] = defaultdict(list)
    for path in active_files:
        m = ID_RE.match(path.name)
        if m:
            by_prefix[m.group(1)].append(path)

    # Compute final id per file: short prefix when unique, full stem when collision
    file_ids: dict[Path, str] = {}
    for prefix, paths in by_prefix.items():
        if len(paths) == 1:
            file_ids[paths[0]] = prefix
        else:
            for p in paths:
                file_ids[p] = p.stem  # full filename minus .md

    new_rows: dict[str, dict] = {}
    skipped: list[str] = []
    for path in active_files:
        gid = file_ids.get(path)
        if not gid:
            skipped.append(path.name)
            continue
        row = extract_gap_row(path, gap_id=gid)
        if not row:
            skipped.append(path.name)
            continue
        new_rows[row["id"]] = row

    # Phase 2.1: re-infer phase for existing rows whose current phase is "n/a"
    # if new inference yields a non-n/a result. Preserves hand-curated phases
    # (e.g. pilot rows GAP-006 phase-2, GAP-353b phase-1-beta) verbatim.
    reinferred_phase = 0
    for gid, row in list(existing.items()):
        if row.get("phase") != "n/a":
            continue
        # Need filename to read the file
        target_file = GAPS_DIR / row["filename"]
        if not target_file.exists():
            continue
        text = target_file.read_text(encoding="utf-8")
        new_phase = infer_phase(text, row.get("priority", ""), filename=row["filename"])
        if new_phase != "n/a":
            existing[gid] = {**row, "phase": new_phase, "last_verified": TODAY}
            reinferred_phase += 1
    if reinferred_phase:
        print(f"  Re-inferred phase (n/a → specific): {reinferred_phase} rows", file=sys.stderr)

    # Reconcile pilot rows with collision-renamed ids.
    # If a pilot row's id matches a collision-prefix that now expands to full
    # stems, re-key the pilot row to its full-stem id (matching its filename).
    pilot_remapped = 0
    reconciled: dict[str, dict] = {}
    for pilot_id, row in existing.items():
        target_id = pilot_id
        if pilot_id in by_prefix and len(by_prefix[pilot_id]) > 1:
            stem = Path(row["filename"]).stem
            target_id = stem
            row = {**row, "id": target_id}
            pilot_remapped += 1
        reconciled[target_id] = row

    # Preserve reconciled pilot rows verbatim; fill new rows where missing.
    final: dict[str, dict] = dict(reconciled)
    added = 0
    for gid, row in new_rows.items():
        if gid not in final:
            final[gid] = row
            added += 1
    if pilot_remapped:
        print(f"  Pilot rows remapped to collision-stem ids: {pilot_remapped}", file=sys.stderr)

    # Sort
    ordered_ids = sorted(final.keys(), key=gap_id_sort_key)

    # Write or preview
    out_lines: list[str] = list(header_comments)
    # Update comment block: bump from "Rows: 5 pilot" → "Rows: N (Phase 2 bulk)"
    for i, line in enumerate(out_lines):
        if line.startswith("# Rows:"):
            out_lines[i] = f"# Rows: {len(final)} (Phase 2 bulk migration 2026-05-11; preserves Phase 1 pilot rows)."
        if line.startswith("# Bulk migration tracked"):
            out_lines[i] = "# Bulk migration: this file. Phase 3 (strip markdown Status/Priority) tracked in follow-up."
    out_lines.append(header_line)

    import io
    buf = io.StringIO()
    writer = csv.writer(buf, lineterminator="\n")
    for gid in ordered_ids:
        r = final[gid]
        writer.writerow([
            r["id"], r["filename"], r["title_short"], r["status"], r["priority"],
            r["domain"], r["phase"], r["completion_pct"], r["found_date"],
            r["last_verified"], r["notes"],
        ])
    out_lines.append(buf.getvalue().rstrip("\n"))

    output = "\n".join(out_lines) + "\n"

    print("\n=== Summary ===", file=sys.stderr)
    print(f"  Existing CSV rows: {len(existing)}", file=sys.stderr)
    print(f"  Active gap files:  {len(active_files)}", file=sys.stderr)
    print(f"  New rows added:    {added}", file=sys.stderr)
    print(f"  Final CSV rows:    {len(final)}", file=sys.stderr)
    if skipped:
        print(f"  Skipped (no ID match): {skipped}", file=sys.stderr)

    # Status / priority / phase distribution
    from collections import Counter
    sc = Counter(r["status"] for r in final.values())
    pc = Counter(r["priority"] for r in final.values())
    phc = Counter(r["phase"] for r in final.values())
    dc = Counter(r["domain"] for r in final.values())
    print(f"  Status:   {dict(sc)}", file=sys.stderr)
    print(f"  Priority: {dict(pc)}", file=sys.stderr)
    print(f"  Phase:    {dict(phc)}", file=sys.stderr)
    print(f"  Domain:   {dict(dc)}", file=sys.stderr)

    if write_mode:
        CSV_PATH.write_text(output, encoding="utf-8")
        print(f"\nWrote {CSV_PATH}", file=sys.stderr)
    else:
        print("\nDry-run only. Pass --write to commit changes.", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
