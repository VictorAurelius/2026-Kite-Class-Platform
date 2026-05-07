# GAP-431: thiếu `startupProbe` trong Helm deployment templates

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Phase 1 BETA — restart loop risk on cold deploy với Flyway migration startup ~15-45s)
**Domain:** DevOps / Helm
**Found:** 2026-05-08 Wave 40 audit milestone (Bucket E Ops Readiness, PR #975)
**Affects:** `infrastructure/helm/kitehub/templates/deployment.yaml` + tất cả service deployment templates

## Problem

Liveness + readiness probes đã có nhưng KHÔNG có `startupProbe`. Service Spring Boot có Flyway migration startup delay 15-45s (tuỳ migration count), liveness probe có thể kill pod trước khi Flyway xong → restart loop trên first cold deploy.

## Root Cause

Helm chart Wave 33-37 setup probes copy từ template chuẩn nhưng Kubernetes 1.16+ recommendation `startupProbe` chưa được follow. Wave 40 Bucket E phát hiện regression.

## Proposed Fix

Thêm `startupProbe` vào tất cả service deployment templates:

```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  failureThreshold: 30
  periodSeconds: 5
  timeoutSeconds: 3
```

Tổng `30 × 5s = 150s` window cho Flyway + Spring init — đủ headroom (Wave 33 V27 + Wave 35 V31..V33 + Wave 33 V28 = ~5 migrations max).

## Acceptance Criteria

- [ ] `startupProbe` thêm vào 6 Helm templates (kitehub-{admin,branding,email,gateway,subscription} + kiteclass-core)
- [ ] `helm lint` clean
- [ ] `helm template` render expected `startupProbe` block
- [ ] Self-test trên local k8s (kind/minikube) hoặc staging — fresh DB cold deploy không restart loop
- [ ] Cross-link runbook `documents/05-guides/operations/runbooks/startup-probe-tuning.md` (mới)

## Related

- Wave 40 Bucket E audit (PR #975 surfaced this)
- `documents/04-quality/audits/ops-readiness/2026-05-08-wave-40-milestone.md` §findings P1
- Kubernetes docs: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#types-of-probe

## Estimated effort

~1-2h (6 templates × 5min edit + helm lint + render check + runbook).

## Log

- **2026-05-08** Filed during Wave 40 closure handoff. Audit Bucket E phát hiện regression Phase 1 BETA pre-launch.
