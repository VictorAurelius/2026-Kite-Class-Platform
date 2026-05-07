#!/usr/bin/env bash
# gap-drift-check — detect PRs that referenced GAP-XXX without updating the gap file.
#
# Scans recent merged PRs (default last 14 days). For each commit whose subject
# or body mentions GAP-XXX (or GAP-XX), checks whether that gap file's
# `## Log` section has a line referencing the PR number. Reports drift cases
# that need a doc-sync follow-up.
#
# Used by:
#   - /start-session (collect-state.sh) — surface staleness on session entry
#   - /repo-status — health-check signal
#   - manual run pre wave-completion PR
#
# Why this exists: 2026-04-24 Wave 5 sub-PRs 5.0/5.1/5.2/5.3 (PRs #474/#476/
# #477/#478) all closed PARTIAL progress on GAP-047 but none updated the gap
# file. Result: /start-session next session showed GAP-047 still 🔵 OPEN.
# See memory `feedback_post_merge_doc_sync.md`.

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
GAPS_DIR="$REPO_ROOT/documents/04-quality/gaps"
SINCE="${1:-14 days ago}"

cd "$REPO_ROOT"

if [[ ! -d "$GAPS_DIR" ]]; then
  echo "ERROR: $GAPS_DIR not found — are you in the right repo?" >&2
  exit 2
fi

# Collect PRs from git log on main since the window. Each line: SHA SUBJECT (#PRN).
# Capture both gap-id and PR-number per commit.
declare -A pr_to_gaps   # PR# → space-separated GAP-IDs from commit message
declare -A pr_subject   # PR# → first 80 chars of subject

while IFS=$'\t' read -r sha subj body; do
  pr_num="$(printf "%s" "$subj" | grep -oE '#[0-9]+' | head -1 | tr -d '#' || true)"
  [[ -z "$pr_num" ]] && continue
  combined="$subj $body"
  gaps="$(printf "%s" "$combined" | grep -oE 'GAP-[0-9]{3}' | sort -u | tr '\n' ' ' || true)"
  [[ -z "$gaps" ]] && continue
  pr_to_gaps[$pr_num]="$gaps"
  pr_subject[$pr_num]="${subj:0:80}"
done < <(git log main --since="$SINCE" --pretty=format:'%H	%s	%b' --no-merges)

if [[ ${#pr_to_gaps[@]} -eq 0 ]]; then
  echo "✓ No gap-touching PRs in last '$SINCE' — nothing to drift-check."
  exit 0
fi

drift_count=0
ok_count=0
declare -a drift_lines

for pr_num in "${!pr_to_gaps[@]}"; do
  for gap_id in ${pr_to_gaps[$pr_num]}; do
    gap_file="$(find "$GAPS_DIR" -maxdepth 1 -name "${gap_id}-*.md" -type f | head -1)"
    if [[ -z "$gap_file" ]]; then
      drift_lines+=("⚠ ${gap_id} referenced by #${pr_num} but no gap file found in $GAPS_DIR")
      ((drift_count++)) || true
      continue
    fi
    # Look in the ## Log section for a line mentioning this PR number.
    log_section="$(awk '/^## .*[Ll]og/{flag=1; next} /^## /{flag=0} flag' "$gap_file")"
    if printf "%s" "$log_section" | grep -qE "#${pr_num}\b"; then
      ((ok_count++)) || true
    else
      drift_lines+=("✗ ${gap_id}  ←  #${pr_num}  ${pr_subject[$pr_num]}")
      ((drift_count++)) || true
    fi
  done
done

echo "Gap drift check (since: $SINCE)"
echo "─────────────────────────────────"
echo "  PRs scanned:        ${#pr_to_gaps[@]}"
echo "  Gap refs verified:  $ok_count ✓"
echo "  Drift cases:        $drift_count ✗"
echo

if (( drift_count > 0 )); then
  echo "Drift detected — these PRs reference a GAP-XXX but the gap file's Log"
  echo "section does NOT mention the PR. Consider a docs-only sync PR to update"
  echo "the gap file Log + status (per memory feedback_post_merge_doc_sync.md):"
  echo
  printf "  %s\n" "${drift_lines[@]}"
  echo
  exit 1
fi

echo "✓ All gap references in recent PRs are mirrored in their gap files."
