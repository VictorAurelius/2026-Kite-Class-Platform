#!/usr/bin/env bash
# Check #1 — Multi-arch base image manifest
# Standard: OCI Image Spec §5.1 + CIS Docker Benchmark §4.1
set -uo pipefail

WORKFLOW=".github/workflows/docker-build-push.yml"
[[ -f $WORKFLOW ]] || { echo "  └─ workflow not found: $WORKFLOW"; exit 0; }

# Extract declared platforms from PUSH stage (line context indicates push-step block)
PUSH_PLATFORMS=$(awk '/Build and push to ECR/{found=1} found && /platforms:/{print; exit}' "$WORKFLOW" | grep -oE 'linux/[a-z0-9]+(/[a-z0-9]+)?' | sort -u)

if [[ -z "$PUSH_PLATFORMS" ]]; then
  echo "  └─ no Push to ECR platforms: declared (skip)"
  exit 0
fi

# Single-arch case = no risk
PLATFORM_COUNT=$(echo "$PUSH_PLATFORMS" | wc -l)
if [[ $PLATFORM_COUNT -le 1 ]]; then
  echo "  └─ single-arch declared ($PUSH_PLATFORMS) — no multi-arch verification needed"
  exit 0
fi

# Extract base images from matrix Dockerfiles
DOCKERFILES=$(grep -oE '[a-z]+/[a-z][a-z0-9-]+/Dockerfile' "$WORKFLOW" | sort -u)
ISSUES=0
for df in $DOCKERFILES; do
  [[ -f $df ]] || continue
  while IFS= read -r base; do
    # Skip if already verified (cache via filename)
    if command -v docker &>/dev/null; then
      MANIFEST=$(docker buildx imagetools inspect "$base" 2>/dev/null | grep -oE 'linux/[a-z0-9]+(/[a-z0-9]+)?' | sort -u || echo "")
      if [[ -z "$MANIFEST" ]]; then
        echo "  ⚠️  $df: cannot fetch manifest for $base (network or auth issue)"
        ISSUES=$((ISSUES + 1))
        continue
      fi
      for plat in $PUSH_PLATFORMS; do
        if ! grep -qx "$plat" <<< "$MANIFEST"; then
          echo "  ❌ FAIL $df: base \`$base\` missing platform \`$plat\`"
          echo "       declared platforms: $(echo $PUSH_PLATFORMS | tr '\n' ' ')"
          echo "       available platforms: $(echo $MANIFEST | tr '\n' ' ')"
          echo "       fix: drop \`$plat\` from workflow OR switch base image to multi-arch variant"
          echo "       standard: OCI Image Spec §5.1 (manifest list)"
          ISSUES=$((ISSUES + 1))
        fi
      done
    else
      echo "  ⚠️  docker not installed — fallback static check ($df: $base)"
      # Static fallback: known-bad list
      if [[ "$base" =~ maven:.*-eclipse-temurin-.*-alpine ]] && grep -q "linux/arm64" <<< "$PUSH_PLATFORMS"; then
        echo "  ❌ FAIL $df: maven:*-eclipse-temurin-*-alpine known amd64-only"
        ISSUES=$((ISSUES + 1))
      fi
    fi
  done < <(grep -oE '^FROM[[:space:]]+[^[:space:]]+' "$df" | awk '{print $2}' | grep -v '^scratch$' | sort -u)
done

if [[ $ISSUES -gt 0 ]]; then
  echo "  └─ $ISSUES issue(s) — see above"
  exit 2
fi
echo "  └─ all base images support all declared platforms"
exit 0
