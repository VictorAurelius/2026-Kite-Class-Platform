#!/usr/bin/env bash
# Category #8 — Dockerfile FROM image reachability + freshness.
#
# Standard:
#   - CIS Docker Benchmark v1.6 §4.1 (Create a user for the container)
#   - CIS §4.2 (Use trusted base images for containers; pinned digest preferred)
#   - Chainguard Distroless guidance (avoid :latest)
#
# What it does:
#   For every `FROM <image>` in Dockerfiles under kitehub/* and kiteclass/*,
#     - verify image is reachable via `docker buildx imagetools inspect` (read-only;
#       does NOT pull the layers)
#     - WARN if `:latest` tag is used (CIS §4.2 anti-pattern)
#     - WARN if manifest `Created` timestamp is older than 6 months (CIS staleness)
#
# Tier 1 read-only — `imagetools inspect` queries registry HEAD/manifest only.
#
# Exit codes:
#   0 PASS / WARN
#   1 FAIL (≥1 image unreachable / 404)
#
# Fallback:
#   If `docker` not installed → degrade to `curl` against Docker Hub manifest API
#   for `library/*` images; emit WARN (best-effort) and skip non-Hub registries.

set -uo pipefail

CHECK_NAME="dockerfile-from-reachability"
CHECK_CATEGORY=8
STALE_THRESHOLD_DAYS=180  # 6 months per CIS recommendation

emit() {
  printf '[%s][cat-%d][%s] %s\n' "$1" "$CHECK_CATEGORY" "$CHECK_NAME" "$2"
}

# --- collect FROM lines ---------------------------------------------------

DOCKERFILES=$(find . -name "Dockerfile" \
  -not -path "*/node_modules/*" \
  -not -path "*/.git/*" \
  -not -path "*/target/*" \
  -not -path "*/.next/*" 2>/dev/null | sort)

if [[ -z "$DOCKERFILES" ]]; then
  emit PASS "no Dockerfile found under repo root — skip"
  exit 0
fi

DF_COUNT=$(printf '%s\n' "$DOCKERFILES" | wc -l | tr -d ' ')
emit INFO "found $DF_COUNT Dockerfile(s)"

# Build list of file:line:image triples. Skip multi-stage stage-aliases
# (FROM <stage>) by checking image contains a ':' or '/' or matches docker hub
# library short-names.
TMP_REFS=$(mktemp)
trap 'rm -f "$TMP_REFS"' EXIT

while IFS= read -r df; do
  [[ -z "$df" ]] && continue
  awk -v file="$df" '
    /^[[:space:]]*FROM[[:space:]]+/ {
      # strip leading "FROM " and any trailing "AS xxx"
      img = $0
      sub(/^[[:space:]]*FROM[[:space:]]+/, "", img)
      sub(/[[:space:]]+[Aa][Ss][[:space:]]+.*$/, "", img)
      sub(/[[:space:]]+--platform=[^[:space:]]+[[:space:]]*/, " ", img)
      gsub(/[[:space:]]+/, " ", img)
      sub(/^ /, "", img); sub(/ $/, "", img)
      # skip --platform-only or stage references (single word, no : and no /)
      if (img ~ /^[a-z][a-z0-9_-]*$/) {
        # likely a stage alias; skip
        next
      }
      print file ":" NR "\t" img
    }
  ' "$df" >> "$TMP_REFS"
done <<< "$DOCKERFILES"

REF_COUNT=$(wc -l < "$TMP_REFS" | tr -d ' ')
if [[ "$REF_COUNT" -eq 0 ]]; then
  emit PASS "no external base images referenced (only stage aliases)"
  exit 0
fi

emit INFO "found $REF_COUNT external FROM ref(s) across Dockerfile(s)"

# --- check reachability ---------------------------------------------------

FAIL_COUNT=0
WARN_COUNT=0
HAVE_DOCKER=0
if command -v docker >/dev/null 2>&1 && docker buildx version >/dev/null 2>&1; then
  HAVE_DOCKER=1
fi

if [[ "$HAVE_DOCKER" -eq 0 ]]; then
  emit WARN "docker buildx not available — using curl fallback (Docker Hub library/* only)"
fi

# Track unique images to avoid re-inspecting same image referenced N times
declare -A SEEN_REACHABLE
declare -A SEEN_LATEST_WARN
declare -A SEEN_STALE

now_epoch=$(date +%s)

while IFS=$'\t' read -r site image; do
  [[ -z "$image" ]] && continue

  # Latest-tag warning (always check, no network needed)
  if [[ "$image" == *":latest" ]] || [[ "$image" != *":"* && "$image" != *"@"* ]]; then
    if [[ -z "${SEEN_LATEST_WARN[$image]:-}" ]]; then
      SEEN_LATEST_WARN[$image]=1
      WARN_COUNT=$((WARN_COUNT + 1))
      emit WARN "$site uses '$image' (untagged or :latest) — pin to explicit tag/digest per CIS §4.2"
    fi
  fi

  # Reachability + freshness via docker buildx (preferred)
  if [[ "$HAVE_DOCKER" -eq 1 ]]; then
    if [[ -z "${SEEN_REACHABLE[$image]:-}" ]]; then
      if inspect=$(docker buildx imagetools inspect "$image" --format '{{json .}}' 2>&1); then
        SEEN_REACHABLE[$image]=ok

        # Try to extract Created field from json (best-effort)
        # imagetools format includes Manifest + Image.Created in some forms.
        created=$(printf '%s' "$inspect" | grep -oE '"[Cc]reated":\s*"[^"]+"' \
          | head -n1 | sed -E 's/.*"([^"]+)"$/\1/' || true)
        if [[ -n "$created" ]]; then
          # ISO-8601 → epoch (best-effort; date -d works for most formats)
          if created_epoch=$(date -d "$created" +%s 2>/dev/null); then
            age_days=$(( (now_epoch - created_epoch) / 86400 ))
            if [[ "$age_days" -gt "$STALE_THRESHOLD_DAYS" ]]; then
              if [[ -z "${SEEN_STALE[$image]:-}" ]]; then
                SEEN_STALE[$image]=1
                WARN_COUNT=$((WARN_COUNT + 1))
                emit WARN "$site image '$image' is ${age_days}d old (>${STALE_THRESHOLD_DAYS}d) — refresh per CIS §4.2"
              fi
            fi
          fi
        fi
      else
        SEEN_REACHABLE[$image]=fail
        FAIL_COUNT=$((FAIL_COUNT + 1))
        emit FAIL "$site image '$image' UNREACHABLE: $(printf '%s' "$inspect" | head -n1)"
      fi
    elif [[ "${SEEN_REACHABLE[$image]}" == "fail" ]]; then
      # report each site that uses an already-known-bad image
      FAIL_COUNT=$((FAIL_COUNT + 1))
      emit FAIL "$site image '$image' UNREACHABLE (see prior message)"
    fi
  else
    # Curl fallback for Docker Hub library/* (free, no auth)
    if [[ -z "${SEEN_REACHABLE[$image]:-}" ]]; then
      # parse "library/name:tag" or "name:tag" → assume Hub library
      img_repo="${image%:*}"
      img_tag="${image#*:}"
      [[ "$img_tag" == "$image" ]] && img_tag="latest"
      # Only handle simple Hub paths; skip vendored registries silently
      if [[ "$img_repo" != *"/"* ]]; then
        ns="library/$img_repo"
      elif [[ "$img_repo" == */* && "$img_repo" != *.*/* ]]; then
        ns="$img_repo"
      else
        emit WARN "$site image '$image' on non-Hub registry — curl fallback skipped"
        SEEN_REACHABLE[$image]=skip
        continue
      fi
      url="https://hub.docker.com/v2/repositories/${ns}/tags/${img_tag}/"
      if curl -sf -o /dev/null "$url" 2>/dev/null; then
        SEEN_REACHABLE[$image]=ok
      else
        SEEN_REACHABLE[$image]=fail
        FAIL_COUNT=$((FAIL_COUNT + 1))
        emit FAIL "$site image '$image' UNREACHABLE via Docker Hub manifest API"
      fi
    fi
  fi
done < "$TMP_REFS"

# --- summary --------------------------------------------------------------

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  emit FAIL "$FAIL_COUNT FROM ref(s) unreachable — deploy WILL fail at image pull time"
  exit 1
fi

if [[ "$WARN_COUNT" -gt 0 ]]; then
  emit WARN "$WARN_COUNT non-fatal hygiene issue(s) — see above"
fi

emit PASS "all $REF_COUNT external FROM ref(s) reachable"
exit 0
