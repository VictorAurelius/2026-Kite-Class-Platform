#!/usr/bin/env bash
# check-stale-domain-references.sh — guard against re-introducing the STALE
# tenant-platform domain `kiteclass.vn` / `kiteclass.com`.
#
# Per GAP-1241 (domain canonical sweep). Canonical platform domain = `kitehub.me`
# (GAP-458/459 Path C; env-reference.yaml domain_root = kitehub.me). Tenant landing
# production = `{slug}.kitehub.me`. Any literal `kiteclass.vn` / `kiteclass.com`
# in active code/docs makes Claude (and readers) believe the wrong domain is the
# product surface — exactly the "nhận thức sai" this guard prevents.
#
# Statically-detectable bug class per cross-flow-bug-class-sweep.md §4.1 →
# persistent CI detector (not one-time manual grep). WARN-mode in CI initially.
#
# Intentional keeps must carry an inline `# stale-domain-ok` (or `// stale-domain-ok`)
# marker on the SAME line — e.g. the DomainService RESERVED_DOMAINS claim-denylist,
# custom-domain test fixtures, and the env-reference supersession note.
#
# Usage:
#   bash scripts/check-stale-domain-references.sh          # strict — exit 1 on any finding
#   bash scripts/check-stale-domain-references.sh --warn    # WARN — report but exit 0
#   bash scripts/check-stale-domain-references.sh --self-test  # run built-in fixtures
#
# Env:
#   SCAN_ROOT — repo root to scan (default: git toplevel, fallback PWD)

set -uo pipefail

MODE="strict"
case "${1:-}" in
  --warn) MODE="warn" ;;
  --self-test) MODE="self-test" ;;
  "") ;;
  *) echo "Unknown arg: $1" >&2; exit 2 ;;
esac

# `\b` after the TLD avoids false positives on reverse-DNS namespaces like the
# Docker LABEL `com.kiteclass.commit-hash` (kiteclass.com is a prefix of
# kiteclass.commit) — match only when `.com`/`.vn` ends a domain token.
PATTERN='kiteclass\.(vn|com)\b'
MARKER='stale-domain-ok'

# Scan scope: product code + active docs + skills. Rules (.claude/rules) excluded —
# their append-only Log sections legitimately cite the stale value as historical
# decision context.
SCAN_DIRS=(kitehub kiteclass infrastructure scripts .github documents .claude/skills)

# Path exclusions (historical / immutable / data logs / forbidden). Anchors use
# `:` because grep -rn output is `path:line:content` (a trailing `$` never matches
# the filename). `/closed/` covers both flat `gaps/closed/` + per-phase
# `gaps/<phase>/closed/`. Flyway migrations excluded — editing an applied
# migration breaks its checksum (the stale ref there is a one-off column COMMENT).
EXCLUDE_RE='/07-archived/|/audits/|/pr-logs/|/session-handoffs/|/closed/|/waves/|/node_modules/|/\.next/|/db/migration/|\.jsonl:|documents/action-[^/]*\.md:|scripts/check-stale-domain-references\.sh:|scripts/tests/test-check-stale-domain-references\.sh:'

scan() {
  local root="$1"
  local -a present=()
  local d
  for d in "${SCAN_DIRS[@]}"; do
    [ -e "$root/$d" ] && present+=("$root/$d")
  done
  [ ${#present[@]} -eq 0 ] && return 0
  # -I skip binary; grep extended; print file:line:content
  grep -rInE "$PATTERN" "${present[@]}" 2>/dev/null \
    | grep -vE "$EXCLUDE_RE" \
    | grep -v "$MARKER"
}

run_repo_scan() {
  local root
  root="${SCAN_ROOT:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
  local findings
  findings="$(scan "$root")"
  local count
  count="$(printf '%s' "$findings" | grep -c . || true)"

  if [ "$count" -eq 0 ]; then
    echo "✅ check-stale-domain-references: 0 stale kiteclass.vn/.com references (canonical = kitehub.me)"
    return 0
  fi

  echo "🔴 check-stale-domain-references: $count stale reference(s) to kiteclass.vn / kiteclass.com"
  echo "   Canonical platform domain = kitehub.me ; tenant landing = {slug}.kitehub.me (GAP-1241)."
  echo "   Fix to kitehub.me, OR add inline '# stale-domain-ok' marker if intentionally historical."
  echo
  printf '%s\n' "$findings"
  echo

  if [ "$MODE" = "warn" ]; then
    echo "WARN mode — not failing build (continue-on-error)."
    return 0
  fi
  return 1
}

self_test() {
  local tmp rc
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  # FAIL fixture — a stale tenant URL in code
  mkdir -p "$tmp/kitehub/app/src"
  printf 'String url = "https://%%s.kiteclass.vn/dashboard";\n' >"$tmp/kitehub/app/src/Foo.java"

  # KEEP fixture — marker-exempted line (must NOT be flagged)
  mkdir -p "$tmp/kitehub/sub/src"
  printf 'Set.of("kiteclass.com","kiteclass.vn") // stale-domain-ok: reserved-claim denylist\n' \
    >"$tmp/kitehub/sub/src/Reserved.java"

  # CLEAN fixture — canonical
  printf 'String url = "https://%%s.kitehub.me/dashboard";\n' >"$tmp/kitehub/app/src/Bar.java"

  # EXCLUDED fixture — archived path (must NOT be flagged)
  mkdir -p "$tmp/documents/07-archived"
  printf 'old tenant url tenant.kiteclass.com\n' >"$tmp/documents/07-archived/old.md"

  local out
  out="$(SCAN_ROOT="$tmp" scan "$tmp")"

  rc=0
  if printf '%s' "$out" | grep -q "Foo.java"; then
    echo "self-test: FAIL fixture flagged          ✅"
  else echo "self-test: FAIL fixture NOT flagged       ❌"; rc=1; fi

  if printf '%s' "$out" | grep -q "Reserved.java"; then
    echo "self-test: marker-keep WRONGLY flagged    ❌"; rc=1
  else echo "self-test: marker-keep exempted          ✅"; fi

  if printf '%s' "$out" | grep -q "Bar.java"; then
    echo "self-test: canonical WRONGLY flagged      ❌"; rc=1
  else echo "self-test: canonical kitehub.me clean    ✅"; fi

  if printf '%s' "$out" | grep -q "07-archived"; then
    echo "self-test: archived WRONGLY flagged       ❌"; rc=1
  else echo "self-test: archived path excluded        ✅"; fi

  [ $rc -eq 0 ] && echo "self-test: PASS ✅" || echo "self-test: FAIL ❌"
  return $rc
}

if [ "$MODE" = "self-test" ]; then
  self_test
  exit $?
fi
run_repo_scan
exit $?
