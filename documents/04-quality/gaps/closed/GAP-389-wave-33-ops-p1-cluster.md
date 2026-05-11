# GAP-389: Wave 33 Ops P1 cluster — pre-deploy backup + beta email smoke test + br-life compliance blocks

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟠 P1 cluster (3 sub-issues — ops hardening + business doc compliance, ship after P0 GAP-384/385/386/387)
**Domain:** DevOps + Business Logic docs
**Found:** 2026-05-07 (Ops /100 + Business /100 audits Wave 33/34 — agents a0737b3f + ad3b6e89)
**Affects:** Production deploy pipeline + `documents/01-business/kitehub/ai-branding/rules.md`

## Problem (3 sub-issues)

### 389-A: Pre-deploy DB backup automation missing
- `release-1-deploy-plan.md` checklist say "Backup snapshot taken pre-deploy" nhưng KHÔNG có script
- Manual: coordinator phải chạy `aws rds create-db-snapshot ...` trong maintenance window
- Risk: forget hoặc race với migration → no rollback point
- `kite_backup_last_success_timestamp_seconds` alert defined nhưng backup cron job KHÔNG wired

### 389-B: Beta-invite email delivery smoke test missing
- `scripts/smoke-test.sh` (GAP-377) checks `/actuator/health` + basic legal pages
- KHÔNG validate beta-signup form submit
- KHÔNG validate token generation
- KHÔNG validate invite email actually delivered (mock SES staging, live prod)
- Operational blind spot: deploy pass smoke nhưng beta flow broken end-to-end

### 389-C: BR-LIFE-001..006 + BR-QUALITY-001 thiếu 5-attribute compliance blocks
- `documents/01-business/kitehub/ai-branding/rules.md` §Lifecycle (L72-81) + §Quality Gate (L90)
- Missing per `business-logic-review.md` v1.0.0 §2: Reviewer + Compliance check + Review cadence
- Audit recorded `BR-WIZARD-001..006` đã COMPLIANT (Wave 34 Bucket A baseline) — chỉ LIFE + QUALITY gaps

## Proposed Fix

### 389-A
- Create `scripts/backup-production.sh`:
  ```bash
  #!/bin/bash
  set -euo pipefail
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)
  aws rds create-db-snapshot \
    --db-instance-identifier kite-rds-prod \
    --db-snapshot-identifier "kite-prod-pre-deploy-${TIMESTAMP}"
  # Wait for snapshot available + emit metric
  ```
- Wire as pre-deploy CI gate trong `.github/workflows/deploy-production.yml` (chạy trước Helm upgrade)
- Add Prometheus counter `kite_backup_snapshots_total{type="pre_deploy"}`
- Update `release-1-deploy-plan.md` checkbox với script reference

### 389-B
- Extend `scripts/smoke-test.sh`:
  ```bash
  # Beta-signup flow smoke
  TEST_EMAIL="smoke+$(date +%s)@kite.test"
  curl -X POST "${BASE}/api/v1/auth/request-beta-access" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"name\":\"Smoke\",\"orgName\":\"Smoke Inc\",\"persona\":\"P1_SOLO_TEACHER\"}"
  # Verify SES delivery: poll `kite_email_sent_total` counter delta or use SES API
  ```
- Mock SES sandbox → live SES post-graduation
- Cleanup: DELETE PENDING row post-smoke (avoid pollution)

### 389-C
- Append 5-attribute blocks trong rules.md:

```markdown
### BR-LIFE-001..006: Lifecycle State Machine

- **Source:** ai-branding-guidelines.md §6
- **Rationale:** State machine NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING; FAILED retry-path. State graph derived from operational AI provisioning patterns; 6 states minimal sufficient (no over-engineering).
- **Reviewer:** @nguyenvankiet (acting Tech Lead solo-dev, 2026-05-07). Pure domain logic, no legal/PDPL trigger.
- **Compliance check:** N/A — internal state machine, no PII/financial/regulated data.
- **Review cadence:** Quarterly OR on state-add PR. **Next review:** 2026-08-07.
- **Code reference:** `kitehub-branding/.../lifecycle/InstanceLifecycleService.java`

### BR-QUALITY-001: Quality Gate Pass Threshold

- **Source:** ai-branding-guidelines.md §5 + empirical observation Wave 4 baseline
- **Rationale:** ≥70/100 score required before DEPLOY. Threshold tunable per tenant tier post-launch (FREE may use 65, ENT 80). Default 70 = midpoint of A/B test range Q3 2026.
- **Reviewer:** @nguyenvankiet (acting Product Owner solo-dev, 2026-05-07). A/B test scheduled Q3 2026.
- **Compliance check:** N/A — internal quality gate, no legal trigger.
- **Review cadence:** Quarterly OR on threshold change PR. **Next review:** 2026-08-07.
- **Config key:** `quality-gate.pass-threshold` (per GAP-386 externalization)
- **Code reference:** `kitehub-branding/.../wizard/quality/QualityScoreAggregator.java`
```

## Acceptance Criteria

- [x] **389-A**: `scripts/backup-production.sh` created + wired pre-deploy CI + Prometheus counter + runbook (`documents/05-guides/deploy/backup-runbook.md` instead of inline checklist update — equivalent artifact per `release-deploy-standard.md` §3)
- [x] **389-B**: smoke-test.sh extends with beta-signup flow + SES delivery validation (live + stub modes) + cleanup (idempotent admin endpoint or WARN if env unset)
- [x] **389-C**: rules.md gets 5-attribute blocks for BR-LIFE-001..006 + BR-QUALITY-001 verification (next review 2026-08-07 / 2026-08-08)
- [x] All 3 sub-issues local-verified: `bash scripts/backup-production.sh --dry-run` exits 0; `bash -n scripts/smoke-test.sh` parses clean; rules.md 5-attribute count = 7 blocks
- [ ] Re-run Ops /100 audit delta: 50/100 → ≥60/100 — deferred to Wave 36 closure audit suite (per `post-wave-audit-mandate.md` §2.4 — Bucket C contributes; aggregate audit run at wave milestone, not per-bucket)

## Related

- Source audits: `documents/04-quality/audits/ops/2026-05-07-wave-33-beta-deploy-ops-readiness.md` (Findings #3, #5) + `documents/04-quality/audits/business/2026-05-07-wave-34-ai-branding-business-logic.md` (Finding #2)
- Parent gaps: GAP-372 (beta invite), GAP-377 (smoke test), GAP-378 (rollback), GAP-115 (logs/metrics)
- Rule: `.claude/rules/business-logic-review.md` v1.0.0 (5-attribute mandate)

## Log

- **2026-05-07** Filed from Ops + Business audits. State-check: 0 existing gaps cover backup automation (grep `backup-production` 0 matches), 0 cover beta email smoke (smoke-test.sh exists but no beta integration), 0 cover BR-LIFE compliance blocks (grep `BR-LIFE.*compliance|BR-LIFE.*5-attribute` 0 matches).
- **2026-05-07** Wave 36 Bucket C SHIPPED (closing PR see commit). Deliverables:
  - 389-A: `scripts/backup-production.sh` (200 LOC, --dry-run support, AWS CLI wrapper, Prometheus pushgateway optional). Wired into `.github/workflows/deploy-production.yml` as pre-deploy step (`Pre-deploy RDS snapshot (GAP-389-A)`). Runbook at `documents/05-guides/deploy/backup-runbook.md`. Verify: `bash scripts/backup-production.sh --dry-run` → exit 0 (pre-flight skipped, simulated AWS invocation logged, snapshot id `kite-prod-pre-deploy-<ts>` returned on stdout, counter line `kite_backup_snapshots_total{type="pre_deploy",region="ap-southeast-1",instance="kite-rds-prod"} 1` emitted).
  - 389-B: `scripts/smoke-test.sh` extended with `check_beta_signup_flow()` (POST /api/v1/auth/request-beta-access + SES delivery validation 2 modes [live via `aws ses get-send-statistics`, stub via `kite.email.sent` actuator metric] + cleanup via `BETA_SMOKE_CLEANUP_URL`/`BETA_SMOKE_ADMIN_TOKEN` or WARN). Idempotent — synthetic email `smoke+<ts>@kite.test`. Verify: `bash -n scripts/smoke-test.sh` parses clean (no live staging available in dev environment).
  - 389-C: `documents/01-business/kitehub/ai-branding/rules.md` appended 6 BR-LIFE-001..006 5-attribute compliance blocks per `business-logic-review.md` v1.0.0 §2 + verification note for BR-QUALITY-001 (already shipped GAP-386 2026-05-08). Total = 7 blocks (`grep -cE "^### BR-LIFE-00[1-6] —|^## BR-QUALITY-001 — Compliance block status|^### BR-QUALITY-001 —" rules.md` returns 7). All blocks document Source, Rationale, Reviewer (acting role declared), Compliance check (verdict + reasoning), Review cadence (next-review date set: 2026-08-07 quarterly OR 2027-05-07 annual for stable patterns BR-LIFE-003 + BR-LIFE-006).
  - Verification artifacts: `bash scripts/backup-production.sh --dry-run` output captured in PR description; YAML parse `python3 -c "import yaml; yaml.safe_load(...)"` on `deploy-production.yml` returns OK; `bash -n scripts/smoke-test.sh` exits 0; rules.md compliance block count grep returns 7.
  - Out-of-scope (tracked separately): re-run Ops /100 audit at Wave 36 milestone closure per `post-wave-audit-mandate.md` §2.4 (cluster covers `backend-domain-kitehub-branding` + `release-deploy-artifacts` domains).
