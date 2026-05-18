#!/usr/bin/env python3
"""Migrate gap files to phase-X/[closed/] layout per rule v2.0.0.

Per `.claude/rules/gap-folder-organization.md` v2.0.0 §2.1:
  - DONE → phase-X/closed/GAP-NNN.md (where X = CSV phase)
  - Else → phase-X/GAP-NNN.md
  - phase=n/a → unclassified/ (POSIX folder name; no slash)
  - Legacy root closed/ orphans (no CSV row) UNTOUCHED

Run from repo root:
    python3 scripts/migrate-gaps-to-phase-subdirs.py            # dry-run
    python3 scripts/migrate-gaps-to-phase-subdirs.py --write    # execute

Wave 95 PR2 — Bucket B + C combined (mass file moves + CSV sync).
"""
from __future__ import annotations

import csv
import io
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
GAPS_DIR = REPO / "documents" / "04-quality" / "gaps"
CSV_PATH = GAPS_DIR / "gap-status.csv"

PHASE_TO_SUBDIR = {
    "phase-1-beta": "phase-1-beta",
    "phase-1.5-paid": "phase-1.5-paid",
    "phase-2": "phase-2",
    "phase-3": "phase-3",
    "n/a": "unclassified",
}


def expected_path(status: str, phase: str, filename_only: str) -> str:
    """Compute expected relative path per rule v2.0.0 §2.1."""
    subdir = PHASE_TO_SUBDIR.get(phase, "UNKNOWN-PHASE")
    if status == "DONE":
        return f"{subdir}/closed/{filename_only}"
    return f"{subdir}/{filename_only}"


def git_mv(old_rel: str, new_rel: str) -> None:
    """Run `git mv` from repo root."""
    old = GAPS_DIR / old_rel
    new = GAPS_DIR / new_rel
    new.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["git", "mv", str(old), str(new)],
        cwd=REPO,
        check=True,
    )


def main(write: bool = False) -> int:
    # Read CSV preserving comment lines + header
    with CSV_PATH.open("r", encoding="utf-8", newline="") as f:
        raw_lines = f.readlines()

    # Split into preamble (comments + header) and data rows
    preamble = []
    data_rows = []
    header = None
    for line in raw_lines:
        if line.startswith("#") or line.strip() == "":
            preamble.append(line)
        elif header is None and line.startswith("id,"):
            header = line
            preamble.append(line)
        else:
            data_rows.append(line)

    # Parse data rows via csv.reader (handles quoted fields with commas)
    reader = csv.reader(io.StringIO("".join(data_rows)))

    new_rows = []
    moves = []
    skipped = 0
    missing = []

    for row in reader:
        if len(row) < 11:
            # Pad short rows
            row = row + [""] * (11 - len(row))
        gap_id, filename, title, status, priority, domain, phase, completion, found, last_verified, *rest = row
        notes = ",".join(rest) if rest else ""

        if not gap_id.startswith("GAP-"):
            new_rows.append(row)
            continue

        # Extract bare filename (last path segment)
        filename_only = filename.rsplit("/", 1)[-1]

        new_filename = expected_path(status, phase, filename_only)

        if filename == new_filename:
            new_rows.append(row)
            skipped += 1
            continue

        # Check current file exists at CSV-claimed path
        current_path = GAPS_DIR / filename
        if not current_path.is_file():
            missing.append((gap_id, filename))
            new_rows.append(row)
            continue

        moves.append((gap_id, filename, new_filename))
        # Updated row with new filename
        new_rows.append([gap_id, new_filename, title, status, priority, domain, phase, completion, found, last_verified, notes])

    # Report
    print(f"=== Migration plan (rule v2.0.0 §2.1) ===")
    print(f"Total CSV rows: {len(new_rows)}")
    print(f"Moves required: {len(moves)}")
    print(f"Already at correct location: {skipped}")
    print(f"Missing files (CSV says present, FS doesn't): {len(missing)}")
    print()

    # Breakdown by target dir
    target_counts: dict[str, int] = {}
    for _, _, new in moves:
        target_dir = new.rsplit("/", 1)[0]
        target_counts[target_dir] = target_counts.get(target_dir, 0) + 1
    print("Move target breakdown:")
    for d, c in sorted(target_counts.items()):
        print(f"  {d}/ : {c}")
    print()

    if missing:
        print("MISSING files (first 5):")
        for gap_id, fn in missing[:5]:
            print(f"  {gap_id}: {fn}")
        print()

    if not write:
        print("=== DRY-RUN — re-run with --write to execute ===")
        print("Sample moves (first 10):")
        for gap_id, old, new in moves[:10]:
            print(f"  git mv {old} -> {new}")
        return 0

    # Execute moves
    print("=== Executing moves ===")
    for i, (gap_id, old, new) in enumerate(moves):
        try:
            git_mv(old, new)
            if (i + 1) % 50 == 0:
                print(f"  ... {i + 1}/{len(moves)} moves")
        except subprocess.CalledProcessError as e:
            print(f"FAIL: {gap_id} {old} -> {new}: {e}", file=sys.stderr)
            return 1
    print(f"Done: {len(moves)} moves.")

    # Write CSV
    print("Writing updated CSV...")
    with CSV_PATH.open("w", encoding="utf-8", newline="") as f:
        f.writelines(preamble)
        writer = csv.writer(f, lineterminator="\n")
        for row in new_rows:
            writer.writerow(row)
    print("CSV updated.")
    return 0


if __name__ == "__main__":
    write_mode = "--write" in sys.argv
    sys.exit(main(write=write_mode))
