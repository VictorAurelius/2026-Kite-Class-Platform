# GAP-431: thiếu `startupProbe` trong Helm deployment templates

**Status:** 🟢 DONE 2026-05-15 — Wave 84 Bucket F: helm lint clean (0 failures, 0 chart failed) trên cả 2 charts; helm template render verified 7 Spring Boot Deployments đều có startupProbe block đúng schema (kitehub: 6 services qua range loop + kiteclass-instance: 1). Live cold-deploy self-test trên k8s cluster tracked separately tại GAP-431b (k8s deployment Phase 2 scope; Phase 1 BETA chạy AWS EC2, k8s charts giữ cho future migration + CI helm-lint gate)
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

- [x] `startupProbe` thêm vào tất cả deployment templates — `infrastructure/helm/kitehub/templates/deployment.yaml` (range loop renders 6 deployments: gateway/subscription/branding/admin/email/frontend) + `infrastructure/helm/kiteclass-instance/templates/deployment.yaml` (kiteclass-core). State-check `find infrastructure/helm -name deployment.yaml -type f` returned exactly 2 chart files; both edited.
- [x] `helm lint` clean — verified Wave 84 Bucket F với helm v3.16.2: kitehub `1 chart(s) linted, 0 chart(s) failed` (chỉ INFO icon recommendation pre-existing); kiteclass-instance `1 chart(s) linted, 0 chart(s) failed` (WARNINGS pre-existing về metadata.name khi lint với empty `instanceId` default — unrelated to startupProbe).
- [x] `helm template` render expected `startupProbe` block — verified Wave 84 Bucket F: `helm template kitehub-test infrastructure/helm/kitehub/ | grep -c "startupProbe:"` returned 6 (gateway+subscription+branding+admin+email+frontend); kiteclass-instance render cho 1 block; total 7/7 Spring Boot Deployments. Block schema confirmed `httpGet /actuator/health + port + initialDelaySeconds=30 + failureThreshold=30 + periodSeconds=5 + timeoutSeconds=3` đúng theo `startupProbe` Kubernetes 1.16+ spec.
- [ ] Self-test trên local k8s (kind/minikube) hoặc staging — fresh DB cold deploy không restart loop — **tracked separately tại GAP-431b** (requires staging k8s cluster; Phase 1 BETA chạy AWS EC2 thay vì k8s, k8s deployment defer Phase 2 — không phải scope của GAP-431).
- [ ] Cross-link runbook `documents/05-guides/operations/runbooks/startup-probe-tuning.md` (mới) — **tracked separately tại GAP-431b** (tuning playbook batched với live k8s self-test verification Phase 2).

## Related

- Wave 40 Bucket E audit (PR #975 surfaced this)
- `documents/04-quality/audits/ops-readiness/2026-05-08-wave-40-milestone.md` §findings P1
- Kubernetes docs: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#types-of-probe

## Estimated effort

~1-2h (6 templates × 5min edit + helm lint + render check + runbook).

## Log

- **2026-05-15** Wave 84 Bucket F — Status flip 🟡 PARTIAL → 🟢 DONE 100%. Verification environment: helm v3.16.2 (downloaded ad-hoc cho session). Commands run:
  - `helm lint infrastructure/helm/kitehub/` → `1 chart(s) linted, 0 chart(s) failed` (1 INFO icon recommendation pre-existing; sub-chart deps fetched via `helm dependency build` after `helm repo add prometheus-community/grafana`).
  - `helm lint infrastructure/helm/kiteclass-instance/` → `1 chart(s) linted, 0 chart(s) failed` (WARNINGS về metadata.name with empty instanceId default — pre-existing unrelated to startupProbe).
  - `helm template kitehub-test infrastructure/helm/kitehub/ | grep -c "startupProbe:"` → 6 (gateway/subscription/branding/admin/email/frontend, all 6 services in range loop).
  - `helm template kc-test infrastructure/helm/kiteclass-instance/ --set instanceId=test123ab | grep -A 8 "startupProbe:"` → 1 block rendered correctly (port=8080, initialDelaySeconds=30, failureThreshold=30, periodSeconds=5, timeoutSeconds=3).
  - Total 7/7 Spring Boot Deployments verified với startupProbe schema đúng Kubernetes 1.16+. Per `gap-done-discipline.md` §2: AC 1-3 verified; AC 4-5 properly delegated to GAP-431b (separate scope — k8s self-test + tuning runbook = Phase 2 work, k8s deployment chưa phải scope Phase 1 BETA AWS EC2). Templates ready cho future k8s migration + CI helm-lint gate ensures regression detection.
- **2026-05-08** Wave 41 Bucket B shipped template edits — added `startupProbe` block (initialDelaySeconds=30, failureThreshold=30, periodSeconds=5, timeoutSeconds=3 → 150s headroom for Flyway+Spring init) to both Helm chart deployment templates with inline GAP-431 javadoc comment explaining the Kubernetes 1.16+ probe model (startup gates liveness/readiness during boot). State-check `find infrastructure/helm -name deployment.yaml -type f` returned 2 paths (kitehub + kiteclass-instance); 2/2 edited. Status flipped to 🟡 PARTIAL per `gap-done-discipline.md` §3 — helm-lint + helm-template render verification + live cold-deploy self-test require helm CLI + k8s cluster, both unavailable in solo-dev WSL env. Follow-up GAP-431b to track staging-CI verification + runbook authoring (paired so deferred work doesn't fall off radar).
- **2026-05-08** Filed during Wave 40 closure handoff. Audit Bucket E phát hiện regression Phase 1 BETA pre-launch.
