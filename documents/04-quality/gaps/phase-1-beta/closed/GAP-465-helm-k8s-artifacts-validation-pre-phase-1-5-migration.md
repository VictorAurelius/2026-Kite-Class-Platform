# GAP-465: Helm + k8s artifacts validation pre-Phase-1.5-migration

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Phase 1.5 mid-cycle prep; not Phase 1 BETA blocker)
**Domain:** Infrastructure / Helm / Kubernetes
**Found:** 2026-05-11 (user-flagged session — Helm/k8s artifacts staleness audit)
**Affects:** `infrastructure/helm/kitehub/` + `infrastructure/helm/kiteclass-instance/` + `infrastructure/k8s/kitehub/` + `infrastructure/k8s/kiteclass-template/`

## Problem

`infrastructure/helm/` + `infrastructure/k8s/` artifacts pre-existing trong repo nhưng KHÔNG được test/maintain regularly. GAP-415 (Phase 2 EKS migration plan) commits "Helm charts production-ready (already partial)" — nhưng "partial" chưa được audit.

**Risks:**
1. **Stale image references** — values.yaml có thể trỏ image tags không còn tồn tại (post-Wave-46 alpine→noble base bump)
2. **Deprecated K8s APIs** — manifests có thể dùng `apps/v1beta1` (removed) hoặc `extensions/v1beta1` (removed)
3. **Missing services** — manifests có thể chưa có ConfigMap/Secret/Deployment cho services shipped Wave 22+ (e.g., Wave 51 student-portal NEW package)
4. **Outdated env vars** — application.yml expanded đáng kể qua waves; Helm values có thể missing
5. **Multi-tenant logic divergence** — kiteclass-template assumes single-tenant per chart instance; conflict với Wave X+ shared-DB tenant_id pattern
6. **GAP-127 Wave 7-Perf code-splitting** không reflected trong frontend Deployment resources/limits

## Background

Helm/k8s shipped early waves cho future Phase 2 EKS cutover. Phase 1 BETA dùng EC2 docker-compose (per ADR-025) → Helm/k8s dormant. Without periodic validation, drift accumulates → Phase 1.5 PAID full migration cutover panic-refactor risk.

## Proposed Fix

Audit + remediate trong dedicated Wave (mid-Phase-1-BETA, ~3-5 ngày sau beta tenants live):

### Phase 1 — Audit (~4-6h Explore agent)

1. State-check actual services count: `find {kitehub,kiteclass}/{kitehub,kiteclass}-* -maxdepth 1 -name 'pom.xml' -o -name 'package.json'` → list all services
2. Cross-reference với Helm values.yaml + k8s Deployments → identify missing/stale services
3. Validate K8s API versions: `kubectl --validate-only --dry-run=client apply -f infrastructure/k8s/` (cần kubectl + K8s 1.30+ context)
4. Helm lint: `helm lint infrastructure/helm/kitehub/` + `helm lint infrastructure/helm/kiteclass-instance/`
5. Helm template render: `helm template kitehub infrastructure/helm/kitehub/ | kubectl apply --dry-run=client -f -`
6. Image tag freshness: cross-check values.yaml image tags với ECR `aws ecr describe-images` (Tier 1 read-only per agent-aws-access.md)
7. Env var coverage: diff `kitehub-frontend/.env.local.example` (or equivalent) vs Helm values

### Phase 2 — Remediate (~1-2 days)

For each finding from audit:
- Missing services → add Deployment + Service + ConfigMap manifests
- Deprecated APIs → upgrade to current K8s GA APIs
- Stale image tags → update values.yaml defaults (use `latest` wildcard cho dev, pinned tags for production overrides)
- Missing env vars → add to Helm `values.yaml` + ConfigMap templates
- Multi-tenant pattern review → reconcile kiteclass-instance template với shared-DB tenant_id approach (likely deprecate kiteclass-template/ in favor of single deployment với multi-tenant runtime config)

### Phase 3 — CI guard (~3-4h)

Add GitHub Actions workflow `helm-lint.yml`:
- Trigger: PRs touching `infrastructure/helm/**` OR `infrastructure/k8s/**`
- Runs: `helm lint` + `helm template | kubectl --dry-run=client apply -f -`
- Block merge nếu lint/template fail

## Acceptance Criteria

- [ ] Audit report `documents/04-quality/audits/infrastructure/2026-MM-DD-helm-k8s-artifacts-audit.md`
- [ ] All Helm `lint` clean (zero warnings/errors)
- [ ] All Helm `template` outputs valid against K8s 1.30+ schema
- [ ] All k8s manifests `kubectl --validate-only --dry-run=client` pass
- [ ] Service inventory match: |services_in_repo| == |services_in_helm_values|
- [ ] CI workflow `helm-lint.yml` shipped + green on first run
- [ ] GAP-415 Status updated với cross-link to this gap (Helm validation prerequisite for Phase 2 cutover)

## Related

- GAP-415 — Phase 2 EKS Migration Plan (parent obligation; this gap is "Helm audit" sub-task)
- ADR-025 — AWS-only Phase 1 Free Tier (defers EKS to Phase 2)
- GAP-464 — ECS Fargate vs EKS decision (may affect remediation scope if pivot ECS)
- `documents/02-architecture/kiteclass-architecture.md` — multi-tenant shared-DB pattern (reconcile with kiteclass-template)

## Log

- **2026-05-11**: Filed user-flagged via session question "tại sao có EKS và K8s trong infra?" — surfaced Helm/k8s staleness audit gap not yet filed. Trigger to start: Phase 1 BETA tenants live + 1-2 weeks stable. Defer Wave 55+ scope.
