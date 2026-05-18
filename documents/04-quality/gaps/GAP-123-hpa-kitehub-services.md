# GAP-123: HPA cho KiteHub Services

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Scaling
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** kitehub-subscription, kitehub-branding, kitehub-admin, kitehub-email, kitehub-gateway, kitehub-frontend

## Problem

Chỉ `kiteclass-core` có HorizontalPodAutoscaler (`infrastructure/k8s/kiteclass-template/core-deployment.yaml`). **6 kitehub services không có HPA** → scaling phải manual.

Evidence:
- `infrastructure/helm/kitehub/templates/deployment.yaml` — không có HPA resource
- `infrastructure/helm/kitehub/values.yaml` — chỉ có `replicas` fixed, không có `autoscaling:` section
- `infrastructure/k8s/kitehub/branding-deployment.yaml` — không có HPA

Risk: trong peak traffic (signup rush, bulk branding jobs) → services không autoscale → timeout/5xx cho users.

## Root Cause

Helm chart được thiết kế cho MVP fixed replicas. HPA thêm sau chưa được triển khai.

## Proposed Fix

1. Create `infrastructure/helm/kitehub/templates/hpa.yaml`:
   ```yaml
   {{- range $name, $svc := dict "gateway" .Values.gateway "subscription" .Values.subscription "branding" .Values.branding ... }}
   {{- if $svc.autoscaling.enabled }}
   apiVersion: autoscaling/v2
   kind: HorizontalPodAutoscaler
   metadata:
     name: kitehub-{{ $name }}
   spec:
     minReplicas: {{ $svc.autoscaling.minReplicas }}
     maxReplicas: {{ $svc.autoscaling.maxReplicas }}
     scaleTargetRef:
       apiVersion: apps/v1
       kind: Deployment
       name: kitehub-{{ $name }}
     metrics:
       - type: Resource
         resource: { name: cpu, target: { type: Utilization, averageUtilization: 70 } }
   {{- end }}
   {{- end }}
   ```
2. Add `autoscaling:` section vào `values.yaml` cho mỗi service
3. Special case: **kitehub-branding** scale theo queue depth (custom metric cho RabbitMQ) — depends on GAP-005 queue scheduling
4. Special case: **kitehub-email** scale theo `email.send` queue depth

## Acceptance Criteria

- [ ] HPA cho 6 kitehub services
- [ ] Values.yaml expose autoscaling config
- [ ] Load test: burst 100 req/s → verify scale-up
- [ ] Scale-down tested sau idle 5 min
- [ ] Custom metrics cho branding queue depth
- [ ] Cost model documented (min vs max replicas)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §8
- Related: GAP-005 (AI queue scheduling — branding custom metric source)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
- **2026-05-18 (Wave 93 §7.2 row 2 user decision)** — **Phase corrected: phase-1.5-paid → phase-2.** Per Wave 93 re-triage audit — HPA là K8s-specific; EC2 hiện tại không K8s; cần khi migrate EKS Phase 2 per GAP-415. Original phase-1.5-paid assignment incorrect (no payment dependency). CSV row updated.
