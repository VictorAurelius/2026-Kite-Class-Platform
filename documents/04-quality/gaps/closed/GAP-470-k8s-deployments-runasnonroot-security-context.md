# GAP-470: K8s Deployments missing `runAsNonRoot` securityContext

**Status:** 🟢 DONE 2026-05-11 (Wave 61 Bucket E — securityContext + readOnlyRootFilesystem + drop:ALL applied across 4 k8s manifests + 2 Helm charts; kiteclass-core Dockerfile USER 1000 added; structural validation via `helm lint` + `python yaml` parse + `kubectl --dry-run=client`)
**Priority:** 🟠 P1 → promoted 🔴 P0 (Wave 61 Bucket E pre-cutover guard)
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

- [x] 4 K8s deployment manifests (kitehub-branding, kiteclass-core/gateway/frontend) có `securityContext.runAsNonRoot: true` + `runAsUser: 1000` + `fsGroup: 1000` + `seccompProfile: RuntimeDefault`
- [x] Helm pod templates (`infrastructure/helm/kitehub/templates/deployment.yaml` cho 6 services + `infrastructure/helm/kiteclass-instance/templates/deployment.yaml`) áp dụng cùng pod-level + container-level securityContext
- [x] Container-level `allowPrivilegeEscalation: false` + `capabilities.drop: [ALL]` + `readOnlyRootFilesystem: true` + tmp emptyDir mount (+ nextjs-cache cho frontend)
- [x] Dockerfiles verify `USER 1000` — kiteclass-core Dockerfile add USER 1000 lần đầu (7 Dockerfiles khác đã có USER spring trước đó)
- [x] Audit re-verify: `grep -rl runAsNonRoot infrastructure/k8s infrastructure/helm` returns 4 k8s + 2 helm files (verified post-edit)

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| CI lint via kube-score / polaris (Phase 1.5 optional) | Future Phase 1.5 enhancement — not blocking pre-cutover guard; file follow-up gap when Phase 1.5 starts |

## Related

- 2026-05-11 pen-test audit P1-B
- 2026-05-08 Wave 40 milestone P1-1 (carry-over)
- GAP-415 EKS cluster (DEFERRED Phase 2)
- ADR-025 Phase 1 BETA AWS Singapore Free Tier
- `release-deploy-standard.md` §3.4 cổng MAJOR

## Log

- **2026-05-11** Filed by Wave 60 Bucket A pen-test self-audit (GAP-406 follow-up). Carry-over từ Wave 40 baseline. Promote P0 khi v1.0.0 PRODUCTION cutover gate fires.
- **2026-05-11** Wave 61 Bucket E — securityContext block landed across 4 k8s deployment manifests (kitehub-branding + kiteclass-core/gateway/frontend) + 2 Helm chart templates (kitehub umbrella covering 6 services + kiteclass-instance per-tenant chart). Pod-level: `runAsNonRoot: true`, `runAsUser: 1000`, `fsGroup: 1000`, `seccompProfile: RuntimeDefault`. Container-level: `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, `capabilities.drop: [ALL]`. Writable scratch dirs (`/tmp` emptyDir + `/.next/cache` for Next.js frontend) mounted to satisfy read-only-rootFS for Spring Boot + Next.js runtime tmp writes. `kiteclass-core` Dockerfile gained `USER 1000` + group/user creation lines (other 7 Dockerfiles already had USER spring). Verification: `helm lint` 0 errors trên cả 2 charts; `helm template kiteclass-instance` rendered + structural YAML check (pod runAsNonRoot=True, container readOnlyRoot=True, drop=['ALL']) — see PR body for full output. CI lint (kube-score / polaris) moved to §Out-of-scope as optional Phase 1.5 enhancement.
