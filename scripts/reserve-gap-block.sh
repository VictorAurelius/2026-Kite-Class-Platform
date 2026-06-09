#!/usr/bin/env bash
#
# reserve-gap-block.sh — atomic-ish GAP-ID block reservation for multi-session work.
#
# Enforces .claude/rules/multi-session-concurrency-coordination.md (GAP-1114).
# Root problem: a git-backed CSV has no atomic ID allocation across concurrent
# branches, so two independent coordinator sessions each compute max(GAP-NNN)+1
# and collide (2026-06-10 incident: both picked GAP-1111).
#
# Fix: each session reserves a DISJOINT block of GAP-IDs before allocating. This
# script computes the next free block by reading BOTH the canonical CSV max AND
# every active reservation, then records the new block in a per-session .reserved
# file. Sessions allocate gap-IDs FROM their block (never max+1). Single canonical
# gap-status.csv is preserved; the only residual CSV merge-conflict is additive
# (distinct rows) and trivially resolvable.
#
# Reservations auto-expire after 4h (matching session-lock staleness) so a block
# from an ended session is released automatically.
#
# Usage:
#   scripts/reserve-gap-block.sh [--size N] [--for <session-tag>]   # reserve a block
#   scripts/reserve-gap-block.sh --list                             # show state
#
# Env overrides (for testing): GAP_CSV, LOCK_DIR, NOW_EPOCH
set -euo pipefail

CSV="${GAP_CSV:-documents/04-quality/gaps/gap-status.csv}"
LOCK_DIR="${LOCK_DIR:-.claude/session-locks}"
SIZE=10
TAG=""
MODE="reserve"
STALE_SECONDS=14400  # 4h

while [ $# -gt 0 ]; do
  case "$1" in
    --size) SIZE="${2:?--size needs a number}"; shift 2 ;;
    --for)  TAG="${2:?--for needs a tag}"; shift 2 ;;
    --list) MODE="list"; shift ;;
    -h|--help)
      sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
      exit 0 ;;
    *) echo "reserve-gap-block: unknown arg '$1'" >&2; exit 2 ;;
  esac
done

case "$SIZE" in (*[!0-9]*|'') echo "reserve-gap-block: --size must be a positive integer" >&2; exit 2 ;; esac
mkdir -p "$LOCK_DIR"

now_epoch="${NOW_EPOCH:-$(date -u +%s 2>/dev/null || echo 0)}"

# Stale check: returns 0 (stale) when reservation timestamp is older than 4h.
# Unparseable / no-clock cases are treated as NOT stale (safe — never auto-release
# a block we cannot prove is old).
is_stale() {
  local ts="$1" tsec
  [ "$now_epoch" -eq 0 ] && return 1
  tsec="$(date -u -d "$ts" +%s 2>/dev/null || echo 0)"
  [ "$tsec" -eq 0 ] && return 1
  [ $(( now_epoch - tsec )) -gt "$STALE_SECONDS" ]
}

# Highest GAP-NNN in the canonical CSV.
csv_max=0
if [ -f "$CSV" ]; then
  csv_max="$(grep -oE '^GAP-[0-9]+' "$CSV" 2>/dev/null | grep -oE '[0-9]+' | sort -n | tail -1)"
  csv_max="${csv_max:-0}"
fi

# Highest reserved END across all non-stale reservation lines.
# Reservation line format: "START-END  <tag>  <iso-utc>"
# Compute res_max + build the listing in the CURRENT shell (no command
# substitution / no pipe) so res_max mutations persist — a for-loop plus a
# while-read fed by file redirect both run in this shell.
res_max=0
listing=""
for _f in "$LOCK_DIR"/*.reserved; do
  [ -e "$_f" ] || continue
  while IFS= read -r _line; do
    [ -z "$_line" ] && continue
    _range="$(printf '%s' "$_line" | grep -oE '^[0-9]+-[0-9]+' || true)"
    [ -z "$_range" ] && continue
    _start="${_range%-*}"; _end="${_range#*-}"
    _tag="$(printf '%s' "$_line" | awk '{print $2}')"
    _ts="$(printf '%s' "$_line" | awk '{print $3}')"
    if is_stale "${_ts:-unknown}"; then
      _state="stale-released"
    else
      _state="active"
      [ "$_end" -gt "$res_max" ] && res_max="$_end"
    fi
    listing+="    GAP-${_start}..GAP-${_end}  ${_tag:-?}  ${_ts:-?}  (${_state})
"
  done < "$_f"
done
base=$(( csv_max > res_max ? csv_max : res_max ))
next_start=$(( base + 1 ))

if [ "$MODE" = "list" ]; then
  echo "Canonical CSV:   $CSV"
  echo "Canonical max:   GAP-${csv_max}"
  echo "Reservations (${LOCK_DIR}/*.reserved):"
  if [ -n "$listing" ]; then printf '%s' "$listing"; else echo "    (none)"; fi
  echo "Next free block start: GAP-${next_start}"
  exit 0
fi

end=$(( next_start + SIZE - 1 ))
if [ -z "$TAG" ]; then
  TAG="$(whoami 2>/dev/null || echo session)-$$"
fi
ts="$(date -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo unknown)"
printf '%s-%s %s %s\n' "$next_start" "$end" "$TAG" "$ts" >> "$LOCK_DIR/${TAG}.reserved"

echo "Reserved block: GAP-${next_start} .. GAP-${end}  (size ${SIZE})"
echo "  session tag:  ${TAG}"
echo "  recorded in:  ${LOCK_DIR}/${TAG}.reserved"
echo "  canonical max GAP-${csv_max}; reserved-max GAP-${res_max}"
echo "Allocate new gaps FROM this block — do NOT use max+1 (per GAP-1114)."
