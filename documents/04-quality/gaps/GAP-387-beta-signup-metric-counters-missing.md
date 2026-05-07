# GAP-387: Beta-signup/approval/rejection metric counters missing

**Status:** 🟢 DONE 2026-05-07 (Wave 35 Bucket D)
**Priority:** 🔴 P0 — operator blind to beta flow health post-launch (no funnel observability)
**Domain:** Backend / Observability
**Found:** 2026-05-07 (Ops Readiness /100 audit Wave 33 — agent a0737b3f)
**Affects:** `kitehub-subscription` BetaAccessService — beta tenant invite flow

## Problem

Wave 33 Bucket C ship BetaAccessService với `submitRequest()` / `approveRequest()` / `rejectRequest()` / `completeBetaSignup()` — **0 Micrometer counters**. Hậu quả production:

- Operator KHÔNG đo được signup attempt rate
- KHÔNG biết approval backlog (pending count growing?)
- KHÔNG biết rejection rate per coordinator
- KHÔNG có alert threshold (e.g., backlog > 50 = ops issue)
- Honeypot rejection silent → bot detection metric vô hình

`/actuator/prometheus` endpoint exposed nhưng KHÔNG có custom metrics cho beta domain.

## Root Cause

Wave 33 plan §3 Scope không enumerate observability bucket; assumption "Prometheus auto-instruments controller endpoints" — true cho HTTP timing/error rate, sai cho domain-level funnel metrics. GAP-372 acceptance criteria không yêu cầu custom counters.

## Proposed Fix

Add 3 Micrometer counters trong `BetaAccessService`:

```java
@Component
public class BetaAccessService {
    private final Counter signupRequestsTotal;
    private final Counter approvalsTotal;
    private final Counter rejectionsTotal;
    private final Counter honeypotRejectionsTotal;  // optional but recommended
    
    public BetaAccessService(MeterRegistry registry, ...) {
        this.signupRequestsTotal = Counter.builder("beta.signup.requests.total")
            .description("Total beta access requests submitted")
            .tag("persona", /* dynamic */)
            .register(registry);
        // ...
    }
    
    public BetaAccessRequest submitRequest(BetaRequestDto dto) {
        signupRequestsTotal.increment();
        // ...
    }
}
```

Tag dimension: `persona` (P1_SOLO_TEACHER / P2_CENTER_OWNER) cho funnel breakdown.

Alert rules trong `infrastructure/helm/.../alerts.yaml` (or wherever defined):
- `beta_signup_requests_total` rate > 100/min sustained → potential abuse alert
- `beta_pending_approvals_count` > 50 → coordinator backlog alert (gauge vs counter)
- `beta_rejection_ratio` > 50% over 1h → quality issue alert

## Acceptance Criteria

- [x] 3-4 Micrometer counters in BetaAccessService (4 added: signup_requests / approvals / rejections / honeypot_rejections)
- [x] `persona` tag dimension trên signup_requests (P1_SOLO_TEACHER / P2_CENTER_OWNER + "unknown" fallback)
- [x] `/actuator/prometheus` exposes new metrics (Counter registered against Spring's `MeterRegistry`; auto-exposed via Actuator)
- [x] Unit test: counters increment correctly per service call (4 new tests in `BetaAccessServiceTest` using `SimpleMeterRegistry`)
- [x] Add 2-3 alert rules trong helm chart (with runbook URL placeholders) — `BetaSignupRateAnomaly` + `BetaHoneypotSpike` mirrored in `kitehub/docker/prometheus/alert-rules.yml` + `infrastructure/helm/kitehub/templates/prometheusrule.yaml`; runbook stubs created at `documents/05-guides/operations/runbooks/beta-signup-rate-anomaly.md` + `beta-honeypot-spike.md`
- [x] Update `documents/01-business/kitehub/beta-access/rules.md` (nếu tồn tại) với observability section — N/A, file does not yet exist (Bucket 0 created `api-contract.md` only); observability documented in service javadoc + runbook references instead

## Related

- Source audit: `documents/04-quality/audits/ops/2026-05-07-wave-33-beta-deploy-ops-readiness.md` (Finding #1)
- Parent gap: GAP-372 (beta tenant invite — Wave 33)
- Related observability: GAP-115 (logs aggregation), GAP-144 (AlertManager receivers PARTIAL)
- Rule: `.claude/rules/logs-format-standard.md` §2.3 — observable counters

## Log

- **2026-05-07** Filed from Ops Readiness /100 audit Wave 33. State-check: 0 existing gaps cover beta metric counters (grep `beta_signup|beta.*metric|metric.*beta` returned 0 matches). Verified absence via `BetaAccessService` code read — chỉ có `log.info(...)` cho approve/reject/submit, no Counter.
- **2026-05-07** Wave 35 Bucket D shipped (this PR). 4 counters added — `beta_signup_requests_total{persona}`, `beta_signup_approvals_total{persona}`, `beta_signup_rejections_total{persona}`, `beta_honeypot_rejections_total` (no persona tag — pre-validation). 2 alerts mirrored across docker prometheus + helm prometheusrule (`BetaSignupRateAnomaly` + `BetaHoneypotSpike`) with paired runbook stubs. 4 new unit tests added; full module test suite 432/432 PASS. Verification artifact: `python3 scripts/check-alert-runbook-url.py` → all 58 alerts have runbook_url. Honeypot counter wire-up from controller @ExceptionHandler is Bucket A scope (out of bucket D); `recordHoneypotRejection()` exposed as public service method ready for caller integration.
