#!/usr/bin/env bash
# check-fe-be-api-contract.sh — static FE→BE method+path contract check
#
# Per GAP-1070 (cơ chế #3 của GAP-802 family). Chiều NGƯỢC của
# scripts/check-be-fe-url-contract.sh (cơ chế #2, chỉ check BE→FE email link).
#
# LỚP BUG (GAP-1069): FE gọi endpoint mà BE KHÔNG expose đúng (method + path).
# Ví dụ: FE dashboard gọi `GET /api/v1/classes` (collection) nhưng BE chỉ có
# `GET /api/v1/classes/{id}` → 404. Prefix-sweep thô không bắt được vì prefix
# `/api/v1/classes` match cả mapping `/{id}`. Cần granularity METHOD + EXACT PATH.
#
# Cách hoạt động (static, không cần running stack):
#   1. Extract FE call sites (METHOD, path-template) từ axios/apiClient/fetch
#      trong *-frontend/src. Normalize `${id}` / backtick interpolation → `{*}`.
#   2. Extract BE mappings (METHOD, full-path) từ @{Get,Post,Put,Delete,Patch}Mapping
#      + class-level @RequestMapping trong *-core / kitehub-*/src. Ghép full path.
#      Normalize `{id}` / `{classId}` → `{*}`.
#   3. Match: mỗi FE (METHOD, path) PHẢI có ≥1 BE (METHOD, path) khớp (wildcard
#      `{*}` match bất kỳ 1 segment, segment-count phải bằng nhau). FE call KHÔNG
#      khớp → WARN finding.
#   4. WARN-mode: exit 0 luôn (in WARN list + count). CI WARN job; coordinator
#      wire HARD-STOP sau khi giảm false-positive.
#
# False-positive được chấp nhận trong WARN-mode (dynamic path không resolve được
# → wildcard). Nhưng PHẢI bắt được case GAP-1069 (method-level collection-vs-{id}).
#
# Env override:
#   CONTRACT_ROOT — point at synthetic fixture repo layout (self-test).
#                   Default = real repo root.
#
# Usage:
#   bash scripts/check-fe-be-api-contract.sh           # human-readable, exit 0
#   bash scripts/check-fe-be-api-contract.sh --json     # {"checked":N,"drift":M}

set -uo pipefail

ROOT="${CONTRACT_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"

JSON_MODE=0
[[ "${1:-}" == "--json" ]] && JSON_MODE=1

# ---------------------------------------------------------------------------
# Roots
# ---------------------------------------------------------------------------
# Frontend source roots (FE call sites live here).
FE_ROOTS=()
for d in "$ROOT"/kiteclass/kiteclass-frontend/src "$ROOT"/kitehub/kitehub-frontend/src; do
  [[ -d "$d" ]] && FE_ROOTS+=("$d")
done

# Backend Java source roots (@*Mapping controllers live here).
BE_ROOTS=()
for d in "$ROOT"/kiteclass/*/src/main/java "$ROOT"/kitehub/*/src/main/java; do
  [[ -d "$d" ]] && BE_ROOTS+=("$d")
done

# ---------------------------------------------------------------------------
# Path normalization helper.
#   - Replace `${...}` template interpolation → `{*}` wildcard
#   - Replace `{id}` / `{classId}` path-var → `{*}` wildcard
#   - Strip query string `?...` + fragment `#...`
#   - Collapse duplicate slashes, strip trailing slash (except root "/")
# Echoes normalized path.
# ---------------------------------------------------------------------------
normalize_path() {
  local p="$1"
  # Strip query + fragment first (before wildcard so `?code=${x}` removed cleanly).
  p="${p%%\?*}"
  p="${p%%#*}"
  # `${...}` interpolation → {*}
  p="$(printf '%s' "$p" | sed -E 's/\$\{[^}]*\}/{*}/g')"
  # `{id}` / `{classId}` path-var → {*}
  p="$(printf '%s' "$p" | sed -E 's/\{[A-Za-z_][A-Za-z0-9_]*\}/{*}/g')"
  # Collapse `//` → `/`
  p="$(printf '%s' "$p" | sed -E 's#/+#/#g')"
  # Strip trailing slash except root
  [[ "$p" != "/" ]] && p="${p%/}"
  printf '%s' "$p"
}

# ---------------------------------------------------------------------------
# Step 1 — extract FE call sites as "METHOD<TAB>path<TAB>file:line".
#
# Two builder shapes:
#   (a) axios/apiClient/api .<verb>( '<path>' | "<path>" | `<path>` )
#       verb ∈ get|post|put|delete|patch
#   (b) fetch( '<path>' | "<path>" | `<path>` [, { ... method: 'VERB' ... }] )
#       default GET; method extracted if present on the same line.
#
# Only paths starting with /api/ are considered (excludes asset/router paths).
# ---------------------------------------------------------------------------
extract_fe_calls() {
  local root
  for root in "${FE_ROOTS[@]}"; do
    # Shape (a): .get( / .post( / ... with first string arg = /api/ path.
    grep -rnE "\.(get|post|put|delete|patch)\(\s*[\`'\"]/api/" \
      "$root" --include='*.ts' --include='*.tsx' 2>/dev/null \
      | grep -vE '/(node_modules|\.next|__tests__|__mocks__|msw|mocks)/|/src/test/|\.(test|spec)\.tsx?:' \
      | while IFS= read -r hit; do
          parse_fe_axios_hit "$hit"
        done

    # Shape (b): fetch( with first string arg = /api/ path.
    grep -rnE "fetch\(\s*[\`'\"]/api/" \
      "$root" --include='*.ts' --include='*.tsx' 2>/dev/null \
      | grep -vE '/(node_modules|\.next|__tests__|__mocks__|msw|mocks)/|/src/test/|\.(test|spec)\.tsx?:' \
      | while IFS= read -r hit; do
          parse_fe_fetch_hit "$hit"
        done
  done | sort -u
}

# grep hit = "file:line:content" — careful: content may contain ':' (URLs etc).
# Split only the FIRST two colons (file then line).
split_hit() {
  # sets globals HIT_FILE HIT_LINE HIT_CONTENT
  local hit="$1"
  HIT_FILE="${hit%%:*}"
  local rest="${hit#*:}"
  HIT_LINE="${rest%%:*}"
  HIT_CONTENT="${rest#*:}"
}

parse_fe_axios_hit() {
  local hit="$1" verb path
  split_hit "$hit"
  # Capture verb + first string-literal path after the verb.
  # Matches  .get(`/api/...`  | .post('/api/...'  | .put("/api/...
  if [[ "$HIT_CONTENT" =~ \.(get|post|put|delete|patch)\(\ *[\`\'\"](/api/[^\`\'\"]*) ]]; then
    verb="${BASH_REMATCH[1]}"
    path="${BASH_REMATCH[2]}"
  else
    return 0
  fi
  path="$(normalize_path "$path")"
  [[ -z "$path" ]] && return 0
  printf '%s\t%s\t%s:%s\n' "$(upper "$verb")" "$path" "${HIT_FILE#"$ROOT"/}" "$HIT_LINE"
}

parse_fe_fetch_hit() {
  local hit="$1" path verb="GET"
  split_hit "$hit"
  if [[ "$HIT_CONTENT" =~ fetch\(\ *[\`\'\"](/api/[^\`\'\"]*) ]]; then
    path="${BASH_REMATCH[1]}"
  else
    return 0
  fi
  # Method option on the same line? `method: 'POST'` / method:"PUT"
  if [[ "$HIT_CONTENT" =~ method:\ *[\'\"]([A-Za-z]+)[\'\"] ]]; then
    verb="$(upper "${BASH_REMATCH[1]}")"
  fi
  path="$(normalize_path "$path")"
  [[ -z "$path" ]] && return 0
  printf '%s\t%s\t%s:%s\n' "$verb" "$path" "${HIT_FILE#"$ROOT"/}" "$HIT_LINE"
}

upper() { printf '%s' "$1" | tr '[:lower:]' '[:upper:]'; }

# ---------------------------------------------------------------------------
# Step 2 — extract BE mappings as "METHOD<TAB>full-path".
#
# Per controller file:
#   base  = class-level @RequestMapping("...") path (before `class X` decl); "" if none
#   for each method-level @{Get,Post,Put,Delete,Patch}Mapping("/sub") OR
#       @RequestMapping(value="/sub", method=RequestMethod.VERB):
#         full = normalize(base + sub)
#         method from annotation type (or RequestMethod attr; ANY if unspecified)
#
# `@RequestMapping` without an explicit method maps ALL verbs → registered as
# method "ANY" (matches any FE verb during step 3).
# ---------------------------------------------------------------------------
extract_be_mappings() {
  local root file
  for root in "${BE_ROOTS[@]}"; do
    while IFS= read -r file; do
      parse_be_controller "$file"
    done < <(grep -rlE '@(Rest)?Controller' "$root" --include='*.java' 2>/dev/null)
  done | sort -u
}

# Pull a path literal out of an annotation argument string.
# Handles:  ("/x")  (value = "/x", ...)  (path = "/x")  ()  (method = ...) → ""
# Echoes the path (may be empty).
extract_mapping_path() {
  local arg="$1" p=""
  if [[ "$arg" =~ (value|path)\ *=\ *\"([^\"]*)\" ]]; then
    p="${BASH_REMATCH[2]}"
  elif [[ "$arg" =~ ^\(\ *\"([^\"]*)\" ]]; then
    p="${BASH_REMATCH[1]}"
  fi
  printf '%s' "$p"
}

# Pull HTTP method out of a @RequestMapping arg (method = RequestMethod.VERB).
# Echoes VERB or "ANY".
extract_requestmapping_method() {
  local arg="$1"
  if [[ "$arg" =~ RequestMethod\.([A-Za-z]+) ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
  else
    printf 'ANY'
  fi
}

parse_be_controller() {
  local file="$1"
  local base="" class_seen=0 verb sub full method line arg

  while IFS= read -r line; do
    # Class declaration boundary — anything @RequestMapping before this is base.
    if [[ "$class_seen" -eq 0 && "$line" =~ (^|[[:space:]])class[[:space:]] ]]; then
      class_seen=1
    fi

    # Class-level @RequestMapping (before class decl) → base path.
    if [[ "$class_seen" -eq 0 && "$line" =~ @RequestMapping ]]; then
      arg="${line#*@RequestMapping}"
      base="$(extract_mapping_path "$arg")"
      continue
    fi

    # Method-level typed mappings.
    if [[ "$line" =~ @(Get|Post|Put|Delete|Patch)Mapping ]]; then
      verb="${BASH_REMATCH[1]}"
      method="$(upper "$verb")"
      [[ "$method" == "GET" || "$method" == "POST" || "$method" == "PUT" \
        || "$method" == "DELETE" || "$method" == "PATCH" ]] || continue
      arg="${line#*Mapping}"
      sub="$(extract_mapping_path "$arg")"
      full="$(normalize_path "${base}${sub:+$sub}")"
      [[ "$full" == /api/* ]] || continue
      printf '%s\t%s\n' "$method" "$full"
      continue
    fi

    # Method-level @RequestMapping (after class decl) with method= attr.
    if [[ "$class_seen" -eq 1 && "$line" =~ @RequestMapping ]]; then
      arg="${line#*@RequestMapping}"
      sub="$(extract_mapping_path "$arg")"
      method="$(extract_requestmapping_method "$arg")"
      full="$(normalize_path "${base}${sub:+$sub}")"
      [[ "$full" == /api/* ]] || continue
      printf '%s\t%s\n' "$method" "$full"
    fi
  done < "$file"
}

# ---------------------------------------------------------------------------
# Step 3 — segment-wise wildcard match.
#   `{*}` segment (either side) matches any single segment.
#   Segment counts must be equal.
# ---------------------------------------------------------------------------
path_matches() {
  local fe="$1" be="$2"
  [[ "$fe" == "$be" ]] && return 0
  local -a f b
  IFS='/' read -ra f <<< "$fe"
  IFS='/' read -ra b <<< "$be"
  [[ "${#f[@]}" -ne "${#b[@]}" ]] && return 1
  local i
  for i in "${!f[@]}"; do
    [[ "${f[$i]}" == "{*}" || "${b[$i]}" == "{*}" ]] && continue
    [[ "${f[$i]}" != "${b[$i]}" ]] && return 1
  done
  return 0
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
BE_MAPPINGS="$(extract_be_mappings)"
FE_CALLS="$(extract_fe_calls)"

declare -a DRIFTS=()
CHECKED=0
DRIFT=0

while IFS=$'\t' read -r fe_method fe_path fe_loc; do
  [[ -z "${fe_method:-}" ]] && continue
  CHECKED=$((CHECKED + 1))
  matched=0
  while IFS=$'\t' read -r be_method be_path; do
    [[ -z "${be_method:-}" ]] && continue
    if [[ "$be_method" == "$fe_method" || "$be_method" == "ANY" ]]; then
      if path_matches "$fe_path" "$be_path"; then
        matched=1
        break
      fi
    fi
  done <<< "$BE_MAPPINGS"
  if [[ "$matched" -eq 0 ]]; then
    DRIFTS+=("$fe_method $fe_path ($fe_loc)")
    DRIFT=$((DRIFT + 1))
  fi
done <<< "$FE_CALLS"

if [[ "$JSON_MODE" -eq 1 ]]; then
  printf '{"checked":%d,"drift":%d}\n' "$CHECKED" "$DRIFT"
  exit 0
fi

echo "=== FE → BE API contract check (GAP-1070 cơ chế #3) — WARN mode ==="
echo
if [[ "$CHECKED" -eq 0 ]]; then
  echo "Không phát hiện FE call site /api/ nào (nothing to check)."
  echo "PASS."
  exit 0
fi
if [[ "$DRIFT" -eq 0 ]]; then
  echo "PASS: tất cả $CHECKED FE call site khớp BE mapping (method + path)."
  exit 0
fi
echo "WARN — $DRIFT FE call site KHÔNG khớp BE mapping nào (method + path):"
echo
printf '  ⚠️  %s\n' "${DRIFTS[@]}"
echo
echo "Checked: $CHECKED · Drift findings: $DRIFT"
echo
echo "Lưu ý: WARN-mode — false-positive có thể có (dynamic path không resolve,"
echo "       BE ở module/service khác, path build qua biến). Review từng finding:"
echo "       drift thật = FE gọi endpoint BE chưa expose (lớp GAP-1069 → 404)."
exit 0
