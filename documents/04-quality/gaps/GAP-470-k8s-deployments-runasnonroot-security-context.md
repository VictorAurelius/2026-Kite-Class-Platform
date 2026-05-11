# GAP-470: K8s Deployments missing `runAsNonRoot` securityContext

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (🔴 P0 cho v1.0.0 PRODUCTION cutover)
**Domain:** Infrastructure / Security
**Found:** 2026-05-11 (Wave 60 Bucket A pen-test self-audit; carry-over từ 2026-05-08 Wave 40 milestone audit P1-1)
**Affects:** K8s cutover Phase 1.5+ (Phase 1 BETA chạy ECS không tác động ngay, nhưng manifest cần production-ready)

## Problem

Tất cả K8s deployment manifests trong `infrastructure/k8s/kiteclass-template/` + `infrastructure/k8s/kitehub/` đều thiếu `securityContext.runAsNonRoot`. Nếu container compromised, attacker chạy ngay với UID 0 → potential host escape qua kernel vulnerability hoặc volume mount privilege.

## Evidence

```bash
$ grep -rl "runAsNonRoot" infrastructure/k8s/ infrastructure/helm/
# (no output — 0 matches)

$ find infrastructure/k8s -name "*deployment*.yaml" | wc -l
4
```

4 deployment files đều không có securityContext block per pod template.

## Root Cause

- ADR-025 defer EKS Phase 2 → K8s manifests treated as "deferred concern"
- Helm chart conversion (Wave 40 placeholder GAP-415 DEFERRED) chưa thực hiện
- Không có lint job (kube-score / polaris) trong CI

## Proposed Fix

Add per-pod template:

```yaml
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
      - name: app
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
              - ALL
```

Verify mỗi Dockerfile có `USER 1000` line OR equivalent non-root user setup.

## Acceptance Criteria

- [ ] 4 K8s deployment manifests + tất cả Helm pod templates có `securityContext.runAsNonRoot: true`
- [ ] Container-level `allowPrivilegeEscalation: false` + `capabilities.drop: [ALL]`
- [ ] Dockerfiles verify `USER 1000` (kitehub services + kiteclass-core)
- [ ] CI lint via kube-score hoặc polaris (optional Phase 1.5)
- [ ] Audit re-verify: `grep -rl runAsNonRoot infrastructure/k8s` returns all manifests

## Related

- 2026-05-11 pen-test audit P1-B
- 2026-05-08 Wave 40 milestone P1-1 (carry-over)
- GAP-415 EKS cluster (DEFERRED Phase 2)
- ADR-025 Phase 1 BETA AWS Singapore Free Tier
- `release-deploy-standard.md` §3.4 cổng MAJOR

## Log

- **2026-05-11** Filed by Wave 60 Bucket A pen-test self-audit (GAP-406 follow-up). Carry-over từ Wave 40 baseline. Promote P0 khi v1.0.0 PRODUCTION cutover gate fires.
