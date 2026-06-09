#!/usr/bin/env bash
# Static guard: the ECR lifecycle policy (ecr.tf) MUST cap every unbounded-growth
# tag family the CI (docker-build-push.yml) produces. Catches the class where a
# new CI artifact category is added but no lifecycle expire rule covers it →
# unbounded ECR growth + silent cost creep.
#
# Origin: 2026-06-10 incident — GAP-402 added cosign keyless signing producing
# `sha256-<digest>.sig` + `.att` tags, but the lifecycle policy only capped
# `sha-`/`main`/`latest`/`pr-` prefixes → ~2,780 signature images accumulated
# (4,210 total images / ~$2/mo + clutter). See
# documents/04-quality/audits/aws-verification/2026-06-10-cost-optimization-ecr-secrets-eip.md
#
# Per cross-flow-bug-class-sweep.md §4.1 (statically-detectable class → persistent
# CI detector) + retention-policy-completeness.md.
#
# Usage:
#   bash scripts/check-ecr-lifecycle-coverage.sh              # check real files
#   bash scripts/check-ecr-lifecycle-coverage.sh --self-test  # run fixtures
#
# Exit 0 = PASS, 1 = FAIL. WARN does not fail.
set -uo pipefail

ECR_TF_DEFAULT="infrastructure/terraform-aws/ecr.tf"
BUILD_WF_DEFAULT=".github/workflows/docker-build-push.yml"

# check <ecr_tf> <build_wf> -> echoes findings, returns 0 PASS / 1 FAIL
check() {
  local ecr_tf="$1" build_wf="$2" fail=0 covered

  if [ ! -f "$ecr_tf" ]; then echo "❌ FAIL: ecr.tf not found: $ecr_tf"; return 1; fi
  if [ ! -f "$build_wf" ]; then echo "⚠️  WARN: build workflow not found: $build_wf (skip CI-family checks)"; return 0; fi

  # Prefixes covered by any lifecycle expire rule (tagPrefixList entries)
  covered=$(grep -oE 'tagPrefixList[[:space:]]*=[[:space:]]*\[[^]]*\]' "$ecr_tf" \
            | grep -oE '"[^"]+"' | tr -d '"' | sort -u || true)

  # 1. untagged-expire rule must exist (untagged images grow unbounded otherwise)
  if ! grep -qE 'tagStatus[[:space:]]*=[[:space:]]*"untagged"' "$ecr_tf"; then
    echo "❌ FAIL: no 'untagged' expire rule in $ecr_tf"; fail=1
  fi

  # 2. cosign/SBOM/attestation present in CI → sha256-*.sig/.att produced → require sha256- rule
  if grep -qiE 'cosign|sigstore|sbom-action|provenance:[[:space:]]*true|sbom:[[:space:]]*true' "$build_wf"; then
    if printf '%s\n' "$covered" | grep -qx 'sha256-'; then
      echo "✅ cosign sig/att (sha256-) capped by lifecycle rule"
    else
      echo "❌ FAIL: $build_wf produces cosign sha256-*.sig/.att but $ecr_tf has NO 'sha256-' expire rule"
      echo "        → signatures accumulate unbounded (the 2026-06-10 incident class)"
      fail=1
    fi
  fi

  # 3. always-expected ephemeral families covered? (WARN only — version tags intentionally kept-forever)
  local fam
  for fam in 'sha-' 'main' 'latest'; do
    printf '%s\n' "$covered" | grep -qx "$fam" || echo "⚠️  WARN: ephemeral tag family '$fam' has no expire rule in $ecr_tf"
  done

  return $fail
}

self_test() {
  local tmp rc=0; tmp=$(mktemp -d)
  cat > "$tmp/wf-cosign.yml" <<'EOF'
      - name: Sign image with Cosign (keyless OIDC)
        run: cosign sign $IMAGE
EOF
  cat > "$tmp/ecr-good.tf" <<'EOF'
    rules = [
      { tagStatus = "untagged" }
      { tagPrefixList = ["sha-"] }
      { tagPrefixList = ["main", "test", "latest", "pr-"] }
      { tagPrefixList = ["sha256-"] }
    ]
EOF
  cat > "$tmp/ecr-bad.tf" <<'EOF'
    rules = [
      { tagStatus = "untagged" }
      { tagPrefixList = ["sha-"] }
      { tagPrefixList = ["main", "test", "latest", "pr-"] }
    ]
EOF
  echo "--- Fixture A: cosign + sha256- rule present (expect PASS) ---"
  if check "$tmp/ecr-good.tf" "$tmp/wf-cosign.yml" >/dev/null 2>&1; then echo "✅ A PASS"; else echo "❌ A unexpectedly FAILED"; rc=1; fi
  echo "--- Fixture B: cosign present, NO sha256- rule = pre-fix state (expect FAIL) ---"
  if check "$tmp/ecr-bad.tf" "$tmp/wf-cosign.yml" >/dev/null 2>&1; then echo "❌ B unexpectedly PASSED"; rc=1; else echo "✅ B correctly FAILED"; fi
  rm -rf "$tmp"
  return $rc
}

if [ "${1:-}" = "--self-test" ]; then
  if self_test; then echo "🟢 self-test PASS"; exit 0; else echo "🔴 self-test FAIL"; exit 1; fi
fi

if check "${ECR_TF:-$ECR_TF_DEFAULT}" "${BUILD_WF:-$BUILD_WF_DEFAULT}"; then
  echo "🟢 ECR lifecycle coverage OK"
  exit 0
else
  echo "🔴 ECR lifecycle coverage FAIL — see .claude/rules/retention-policy-completeness.md"
  exit 1
fi
