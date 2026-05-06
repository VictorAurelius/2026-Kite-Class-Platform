# GAP-377: Smoke Test Post-Deploy Automation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — confidence in deploy success)
**Domain:** DevOps / QA
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Deploy verification, regression detection

## Problem

KHÔNG có automated smoke test chạy ngay sau production deploy. Hiện tại verify thủ công (checklist trong runbook). Risk:
- Deploy "succeed" theo CI nhưng broken trên production
- No early detection của broken auth, broken endpoints, broken UI
- Manual smoke test có thể skip steps under time pressure

## Proposed Fix

`scripts/smoke-test.sh <kitehub-url> <kiteclass-url>`:

```bash
#!/usr/bin/env bash
set -euo pipefail
KH_URL="${1:-https://kitehub.vn}"
KC_URL="${2:-https://kiteclass.vn}"
PASS=0
FAIL=0

assert() {
  local name="$1"
  local actual="$2"
  local expected="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "✅ $name"
    PASS=$((PASS+1))
  else
    echo "❌ $name (got '$actual', expected '$expected')"
    FAIL=$((FAIL+1))
  fi
}

# Health checks
assert "KH health" "$(curl -s "${KH_URL}/actuator/health" | jq -r .status)" "UP"
assert "KC health" "$(curl -s "${KC_URL}/actuator/health" | jq -r .status)" "UP"

# Public marketing
assert "KH home" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/")" "200"
assert "KH legal/privacy" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/legal/privacy")" "200"
assert "KH legal/terms" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/legal/terms")" "200"
assert "KH legal/cookies" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/legal/cookies")" "200"
assert "KC home" "$(curl -s -o /dev/null -w '%{http_code}' "${KC_URL}/")" "200"

# Auth endpoints
assert "Auth signup form" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/auth/signup")" "200"
assert "Beta access form" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/auth/request-beta-access")" "200"

# API health
assert "KH /api/v1/health" "$(curl -s -o /dev/null -w '%{http_code}' "${KH_URL}/api/v1/health")" "200"

# ConsentBanner present (per GAP-353)
assert "Consent banner present" "$(curl -s "${KH_URL}/" | grep -c 'ConsentBanner\|consent-banner')" "1"

# Build info
BUILD_INFO=$(curl -s "${KH_URL}/actuator/info" | jq -r '.build.version // "unknown"')
echo "Build version: $BUILD_INFO"

# Summary
echo "─────────────────────"
echo "Smoke test: ${PASS} passed, ${FAIL} failed"
if [[ $FAIL -gt 0 ]]; then
  exit 1
fi
```

### Extend with E2E (optional)

Playwright E2E suite chạy critical user journey:
- Signup flow
- Login flow
- Owner dashboard load
- Add student
- Add lesson
- Logout

Trigger từ GitHub Action sau deploy:
```yaml
post-deploy-smoke:
  needs: deploy
  steps:
    - run: ./scripts/smoke-test.sh https://kitehub.vn https://kiteclass.vn
    - if: failure()
      run: ./scripts/rollback.sh  # auto-rollback on smoke fail
```

## Acceptance Criteria

- [ ] `scripts/smoke-test.sh` script created
- [ ] 15+ assertions covering: health, public pages, auth, API, ConsentBanner, build info
- [ ] Exit code: 0 = pass, 1 = fail
- [ ] CI integration: post-deploy smoke test step
- [ ] Auto-rollback on smoke fail (optional)
- [ ] Documentation: how to extend với new tests
- [ ] Run on staging trước first prod use

## Effort estimate

~1 ngày script + ~1 ngày CI integration. Optional E2E ~2-3 ngày extra.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` §2.4 + §3.3
- Sister: GAP-378 (rollback procedure — auto-trigger on smoke fail)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. STRONGLY recommend cho confidence + early detection.
