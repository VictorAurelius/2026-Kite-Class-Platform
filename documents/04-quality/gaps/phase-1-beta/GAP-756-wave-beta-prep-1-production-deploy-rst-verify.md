# GAP-756 — Wave beta-prep-1 production deploy + RST verify

**Status:** 🟡 PARTIAL 25% (Phase 1 local RST PASS 2026-05-27 — Phase 2+3 deploy + Bucket C alarms defer next session user-trigger per `release-deploy-standard.md` §9)
**Priority:** 🔴 P0
**Domain:** DevOps
**Detected:** 2026-05-26
**Related PRs:** Wave beta-prep-1 cluster (#1871-#1877 + #1872) — all merged on main HEAD `a64bcef2`
**Related Docs:** `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md` §Phase β + §9 Scope-Completeness Reconciliation
**Blocker:** GAP-612 RST policy gate (2026-05-25 "No AWS push until local RST verified" policy)

## Current State (verified 2026-05-26)

| Piece | State |
|---|---|
| Main HEAD | `a64bcef2` (7 wave PRs merged) |
| EC2 production image | PRE-wave-merge Docker image (kh-backend, kc-app, kc-app-fe) |
| Production endpoints | `api.kitehub.me/actuator/health` 200 ✅; `kitehub.me/` landing 200 ✅; `/privacy` 404 ❌; `/terms` 404 ❌; `/waitlist` 404 ❌ (new FE routes from Bucket F+G NOT deployed) |
| ECR repo `kitehub-platform` | NoSuchEntity (provisioning gap — separate sub-issue) |
| docker-build-push.yml push triggers | DISABLED since 2026-05-25 per GAP-612 RST policy comment |

**Grep commands run:**
```bash
curl -sSI https://kitehub.me/waitlist → HTTP 404 (Bucket F+G FE not deployed)
curl -sSI https://api.kitehub.me/actuator/health → HTTP 200 (BE infra UP)
aws ecr describe-images --repository-name kitehub-platform → RepositoryNotFoundException
gh workflow view docker-build-push.yml --yaml | grep -A5 'push:' → triggers commented out per GAP-612
```

## Problem

Wave beta-prep-1 (7 buckets) shipped 2026-05-26 ~6h coordinator-inline. All 7 PRs merged main. Phase β AWS infrastructure smoke PASS (3 EC2 + RDS started cleanly, apex healthy). NHƯNG Wave code NOT live on production:

- New FE routes (`/privacy`, `/terms`, `/waitlist`) → 404 because EC2 runs pre-wave Docker image
- New BE migrations (V62 consent if added) → not applied
- New legal docs FE routes → defer (Bucket A only shipped `.md` documents)
- New IT tests + concurrency fixes → only validated CI Testcontainers, not production runtime

Full production deploy pipeline blocked by GAP-612 RST (Restore + Smoke Test) policy gate:
- Per `docker-build-push.yml` header comment (2026-05-25): "No AWS push (ECR image upload) until local RST verified successfully. Re-enable condition: GAP-612 RST local verification PASS"
- Local RST = `bash kitehub/scripts/up.sh --profile full` + admin-login smoke + endpoint walk

## Proposed Fix

### Phase 1 — Local RST verify (~30 min)

1. Start local Docker stack với Wave beta-prep-1 code:
   ```bash
   cd kitehub && bash scripts/up.sh --profile full
   ```
2. Wait 8/8 services healthy
3. Smoke admin-login flow per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g)
4. Smoke wave endpoints:
   - POST /api/auth/beta-signup with consent payload → verify HTTP 201 + (current) silent drop OR (post GAP-755 fix) DB row
   - GET /privacy / /terms → 200 + Vietnamese content render
   - GET /waitlist → 200 + Phase 2 messaging render
   - POST /api/admin/beta-requests/bulk-invite (if F.6 shipped) → defer per F.6 NOT-IMPLEMENTED
5. Update GAP-612 Log: "RST PASS Wave beta-prep-1 code Q3 2026-05-26 — re-enable ECR push"
6. Re-enable docker-build-push.yml push:main + tags triggers (revert 2026-05-25 disable comment)

### Phase 2 — Pre-deploy audit + tag (~30 min)

7. Write pre-mutation audit artifact `documents/04-quality/audits/aws-verification/2026-05-XX-wave-beta-prep-1-pre-apply.md` per `pre-mutation-state-check.md` v1.2.0 §3 + §3.5 plan-vs-predicted reconciliation
8. Verify ECR repo `kitehub-platform` exists OR provision via terraform (sub-issue if missing)
9. Tag release `v0.9.0-beta-staging.22` from main HEAD a64bcef2
10. Push tag → triggers `docker-build-push.yml` (post-Phase-1 re-enable) → builds 10 services + push ECR

### Phase 3 — Production deploy (~30 min)

11. `gh workflow run deploy-production.yml -f version=v0.9.0-beta-staging.22 -f confirm=DEPLOY`
12. Wait SSM send-command complete on 2 EC2 (~10-15 min)
13. Smoke post-deploy:
    - `POST /api/auth/login` with seeded `PLATFORM_ADMIN` → HTTP 200 + JWT (per `release-deploy-standard.md` v1.2.0 §3.1 Smoke admin-login mandate)
    - Wave endpoints all return expected status
    - DB migrations applied (Flyway version check)
14. Audit trail: workflow output → CloudWatch metric `KiteHub/Deploy/TimeToHealthy`
15. Update wave plan Log: "Phase β AWS smoke complete + production deploy live 2026-05-XX"

### Phase 4 — Retry budget (per `release-fix-retry-budget.md` v1.2.0)

- Retry budget: ≤2 fix attempts on same CI gate; retry #2 → STOP + pivot
- Investigation phase MANDATORY per §3.5 between retries
- Override trailer: `RELEASE_RETRY_*_OVERRIDE:` if genuine exception

## Acceptance Criteria

- [x] Local RST PASS Wave beta-prep-1 code (Phase 1) — verified 2026-05-27 02:55 UTC (13/13 services healthy + admin-login JWT `PLATFORM_ADMIN` HTTP 200 + 3 wave FE routes 200 + beta-request HTTP 201 với consent + VN sample)
- [x] GAP-612 Log entry: "RST PASS 2026-05-27 — re-enable ECR push" added cùng PR
- [x] docker-build-push.yml push triggers re-enabled (revert 2026-05-25 disable) — cùng PR Phase 1
- [ ] Pre-mutation audit artifact shipped per `pre-mutation-state-check.md` §3 + §3.5
- [ ] ECR repo `kitehub-platform` exists (provision if missing — sub-issue)
- [ ] Release tag `v0.9.0-beta-staging.22` from main `a64bcef2`
- [ ] docker-build-push CI completes (10 images pushed)
- [ ] deploy-production.yml workflow_dispatch PASS (SSM send-command success)
- [ ] Smoke admin-login PASS (HTTP 200 + JWT)
- [ ] Smoke wave endpoints all PASS (/privacy 200, /terms 200, /waitlist 200, consent payload reaches BE)
- [ ] Wave plan Log entry: "Phase β AWS smoke complete + production deploy live"
- [ ] Stop AWS stack post-verify to save Free Tier hours
- [ ] Bucket C cloudwatch-p0-alarms.tf terraform apply triggered (8 SNS alarms wired)
- [ ] Update GAP-727 / GAP-755 / Wave plan §9 reconciliation table with live-verify evidence

## Dependencies + Blockers

- **GAP-612** AWS production restoration verification (currently PARTIAL post-2026-05-25 restore)
- **GAP-755** PDPL consent BE persistence (independent — can ship post-deploy as separate PR)
- **Sub-issue:** ECR repo `kitehub-platform` provisioning gap (may block Phase 2 step 8)

## Effort estimate

**Total: ~1.5-2h coordinator-inline** (~30 min local RST + ~30 min audit/tag/build + ~30 min deploy/smoke + ~30 min retry buffer)

## Risk

- **GAP-612 RST may surface new bugs** — Wave code 7 buckets parallel-shipped, local RST is first integration test
- **ECR provisioning gap** — separate sub-issue blocks Phase 2
- **docker-build-push retry budget** — if image build fails 2x on same gate, redesign trigger per `release-fix-retry-budget.md` v1.2.0 §3
- **Production smoke admin-login regression** — per 2026-05-16 admin login 500 incident class; new V62 migration (if shipped) may trigger H2/Mockito divergence

## Log

- **2026-05-27 (Phase 1 PASS — PARTIAL 25%):** User scope-lock 2026-05-27 02:43 UTC "Phase 1 only (local RST + admin-login smoke)". Executed:
  1. `bash kitehub/scripts/up.sh --profile full` → 13/13 services healthy (~3 min)
  2. Initial smoke: `/legal/privacy` + `/legal/terms` 200; `/waitlist` 404 (image stale)
  3. `bash kitehub/scripts/rebuild.sh frontend` rebuild kitehub-frontend → re-smoke `/waitlist` 200
  4. Admin login: `POST /api/auth/login` với `admin@kitehub.com / Admin@KiteHub123` (V9 seed) → HTTP 200 + JWT `role=PLATFORM_ADMIN`
  5. Public beta-request: `POST /api/v1/auth/request-beta-access` payload `{persona: P2_CENTER_OWNER, honeypot: "", consentGiven: true}` + VN sample (Trần Thị Smoke / Trung tâm Smoke Test) → HTTP 201 row id=11 PENDING
  6. GAP-612 AC "Local RST PASS" + "docker-build-push re-enable" flipped to checked + Log entry appended
  7. `.github/workflows/docker-build-push.yml` push:main + tags triggers uncommented (revert 2026-05-25 disable)
  
  **Phase 2+3 defer next session user-trigger per `release-deploy-standard.md` §9** — agent autonomy banned cho workflow_dispatch deploy. Caveats logged: (a) image rebuild required mid-flow because Wave Bucket F+G commit landed after last cached image build — deploy pipeline ECR push must build from main HEAD `a64bcef2`; (b) `kitehub-subscription` container logs show EmailEvent deserialization error loop trên admin-new-login-alert poisoned messages từ prior session — non-blocking on login path, file follow-up nếu recurrence > 1 session.
- **2026-05-26 (Filed P0 OPEN):** GAP-756 created as Wave beta-prep-1 Phase β follow-up. Triggered by Phase β AWS smoke verify session 2026-05-26: infrastructure UP (3 EC2 + RDS started cleanly + apex healthy) BUT Wave code NOT deployed (FE 404 on new routes confirms pre-wave Docker image on EC2). Full deploy pipeline blocked by GAP-612 RST policy gate (2026-05-25 docker-build-push.yml push triggers DISABLED). User direction "defer deploy + Phase E closure now" per `release-fix-retry-budget.md` v1.2.0 §5 tooling-fix-then-retry exception class. Wave beta-prep-1 ships as PARTIAL pending live verify per `gap-done-discipline.md` §3 exit ramp.
