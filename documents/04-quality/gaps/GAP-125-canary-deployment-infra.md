# GAP-125: Canary Deployment Infrastructure

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Deployment
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Safe rollout cho risky changes, reduced blast radius

## Problem

`deploy-go-nogo-checklist.md` §2 mention canary deployment workflow:
```bash
helm upgrade kitehub ... --set gateway.canary.enabled=true --set gateway.canary.weight=10
```

Nhưng **Helm chart không có canary config** thực sự:
- `infrastructure/helm/kitehub/values.yaml` — không có `canary:` section
- `templates/deployment.yaml` — không có canary deployment separate
- Không có Argo Rollouts / Flagger deployed

Kết quả: canary doc là "theoretical" — không thực sự chạy được.

## Root Cause

Canary là advanced deployment pattern, MVP skip để get basic rolling deploy chạy trước.

## Proposed Fix

### Option A: Argo Rollouts (recommended)
1. Install Argo Rollouts operator vào cluster
2. Convert Deployments → Rollout CRD với canary strategy:
   ```yaml
   strategy:
     canary:
       steps:
         - setWeight: 10
         - pause: { duration: 10m }
         - setWeight: 50
         - pause: { duration: 10m }
         - setWeight: 100
       analysis:
         templates:
           - templateName: error-rate-analysis
   ```
3. Analysis templates query Prometheus (depends on GAP-111) để auto-abort nếu error rate spike

### Option B: Flagger + Linkerd/Istio
- Tương tự nhưng cần service mesh

### Minimum baseline (Option C — manual)
1. Add canary deployment template với fixed 10%/50%/100% steps
2. Weight qua Gateway (istio-style virtual service) hoặc ingress annotation
3. Manual promote sau 10 min soak

Scope của GAP này: **Option C minimum** (manual canary) — Option A upgrade sau.

## Acceptance Criteria

- [ ] Canary Helm template với configurable weight
- [ ] Values.yaml: `gateway.canary.enabled`, `gateway.canary.weight`
- [ ] Deploy workflow support canary flag (`--set gateway.canary.enabled=true`)
- [ ] Document workflow trong `deploy-go-nogo-checklist.md` (reconcile từ theoretical → real)
- [ ] Test: deploy canary với 10% traffic → verify via gateway logs
- [ ] Rollback: canary bad → delete canary revision, stable unaffected

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §8
- Depends: GAP-111 (monitoring for analysis metrics)
- Related: `rollback-procedure.md` (update with canary rollback steps)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
