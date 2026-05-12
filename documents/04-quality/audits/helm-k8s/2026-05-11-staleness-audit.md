---
title: Helm + K8s Artifacts Staleness Audit (Wave 64 Bucket D)
date: 2026-05-11
auditor: bg-agent (Wave 64 GAP-465)
scope: infrastructure/helm/**
gap: GAP-465
---

# Helm + K8s Staleness Audit

Read-only audit per `agent-aws-access.md` Tier 1 scope — file reads only, no apply,
no cluster access. Goal: surface drift trước Phase 1.5 / Phase 2 EKS migration prep.

## Summary

| Metric | Value |
|--------|-------|
| Charts audited | 2 (`kitehub`, `kiteclass-instance`) |
| Material drift findings | 4 |
| Follow-up gaps filed | 2 (GAP-478 P1, GAP-479 P2) |
| Overall verdict | 🟡 **YELLOW** — no production blockers but Phase 2 readiness gaps |

**Top-line:**
- ✅ Chart API versions clean (`v2`)
- ✅ No deprecated K8s core APIs (`extensions/v1beta1` etc.) in any template
- ✅ Pod/container security context aligned with Wave 61 GAP-470 hardening baseline
- ✅ startupProbe / liveness / readiness probes wired correctly (GAP-431)
- ⚠️ `external-secrets.io/v1beta1` API used — deprecated in ESO v0.10+ (current = `v1`)
- ⚠️ Service inventory drift: `kitehub-platform` exists trong code nhưng KHÔNG có trong helm
- ⚠️ Ingress declared via `values.yaml` (host: `kiteclass.com`) nhưng KHÔNG có template render → silent no-op
- ⚠️ Subchart `loki-stack 2.10.2` deprecated upstream (Grafana đã chuyển sang `loki` v6.x + standalone `promtail`)
- ⚠️ Subchart `kube-prometheus-stack 58.7.2` ~17 minor versions stale (upstream hiện tại ~75.x)

---

## Per-chart findings

### Chart 1: `infrastructure/helm/kitehub/`

| Dimension | Current | Latest / Expected | Severity | Action |
|-----------|---------|-------------------|----------|--------|
| Chart `apiVersion` | `v2` | `v2` | none | ✅ |
| Chart `version` | `1.2.0` | n/a (project-internal) | none | ✅ |
| Chart `appVersion` | `"1.0.0"` | aligns với Phase 1 BETA target | none | ✅ |
| Deployment apiVersion | `apps/v1` | `apps/v1` | none | ✅ |
| Service apiVersion | `v1` | `v1` | none | ✅ |
| PrometheusRule apiVersion | `monitoring.coreos.com/v1` | `v1` | none | ✅ |
| ServiceMonitor apiVersion | `monitoring.coreos.com/v1` | `v1` | none | ✅ |
| ExternalSecret apiVersion | **`external-secrets.io/v1beta1`** (2 templates) | `external-secrets.io/v1` (ESO ≥0.10) | **P1** | follow-up **GAP-478** |
| Service inventory match | 6 BE services in helm (gateway/subscription/branding/admin/email/frontend) | 7 BE services exist (`kitehub-platform` missing) | **P2** | follow-up **GAP-479** |
| Ingress template | values.yaml has `ingress.enabled=true` + 2 hosts, NO matching `templates/ingress.yaml` | Either render template OR disable default | P2 | follow-up GAP-479 (bundled) |
| Subchart `kube-prometheus-stack` | `58.7.2` (Apr 2024) | `~75.x` (current) | P2 | bundled GAP-479 review |
| Subchart `loki-stack` | `2.10.2` (deprecated; last release of bundled chart) | `loki` v6.x + `promtail` v6.x separate | P2 | bundled GAP-479 review |
| Deprecated K8s core APIs (`extensions/v1beta1`, `apps/v1beta*`, `batch/v1beta1`, `networking.k8s.io/v1beta1`, `policy/v1beta1`, `autoscaling/v2beta*`) | none | n/a | none | ✅ |
| Pod `securityContext` (runAsNonRoot/seccomp) | ✅ Wave 61 GAP-470 baseline applied | meets baseline | none | ✅ |
| Container `securityContext` (readOnlyRootFilesystem/drop ALL caps/no privesc) | ✅ applied | meets baseline | none | ✅ |
| Resource limits/requests | ✅ explicit on all 6 services | meets baseline | none | ✅ |
| Probe coverage | ✅ startupProbe + liveness + readiness on all services (GAP-431) | meets baseline | none | ✅ |
| Image tag drift | values.yaml default `tag: "latest"` cho global | Production override required (override-prod.yaml) — not enforced bằng schema | P3 | document trong follow-up; chưa file gap (low risk Phase 1 BETA) |

#### Detail: ExternalSecret API deprecation

Files affected:
- `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml:35`
- `infrastructure/helm/README.md:132` (doc example)

`external-secrets.io/v1beta1` was deprecated in External Secrets Operator v0.10
(2024-Q3). Stable served version `v1` lands in ESO v0.11+. Both API versions are
served by current ESO releases (backwards compat maintained), nhưng new installs
should land on `v1` per upstream guidance:
https://external-secrets.io/latest/api/externalsecret/

**Impact:** P1 because deferring drift to Phase 2 EKS cutover (where we'll likely
adopt newer ESO version) means migration PR bị surprise. Fix now = 1-file edit.

**Fix path:** Bump apiVersion `v1beta1 → v1`; schema is backward-compat (no
spec changes between versions). Test với `helm template` clean. File: GAP-478.

#### Detail: Service inventory drift

```
$ ls kitehub/ | grep ^kitehub-
kitehub-admin
kitehub-base       <- shared lib (not deployable)
kitehub-branding
kitehub-email
kitehub-frontend
kitehub-gateway
kitehub-platform   <- ❌ NOT in helm deployment template
kitehub-subscription
```

helm `templates/deployment.yaml:1` only renders for 6 services
(`gateway` / `subscription` / `branding` / `admin` / `email` / `frontend`).
`kitehub-platform` (separate Spring Boot service) — chưa có
Deployment / Service / ServiceMonitor.

**Impact:** P2 — Phase 1 BETA dùng docker-compose (per ADR-025), nên helm gap không
block. Phase 2 EKS migration sẽ surface gap này. Fix path: extend the
`dict` literal at line 1 of `deployment.yaml` + add `platform:` block trong values.yaml.

#### Detail: Ingress declared but not rendered

`values.yaml:103-114` defines `ingress.enabled=true` với hosts `kiteclass.com`
+ `api.kiteclass.com` và ALB annotations. NHƯNG không có
`templates/ingress.yaml` file. Result: `helm template` không render Ingress
resource — silent no-op cho operators expect ALB ingress.

**Impact:** P2 — Phase 1 BETA dùng nginx → ALB direct (terraform-aws-managed
ALB không qua K8s Ingress controller per Architecture B). Drift surfaces Phase 2.
Bundled fix path trong GAP-479: hoặc tạo `templates/ingress.yaml`, hoặc remove
ingress config (move to terraform-only).

### Chart 2: `infrastructure/helm/kiteclass-instance/`

| Dimension | Current | Latest / Expected | Severity | Action |
|-----------|---------|-------------------|----------|--------|
| Chart `apiVersion` | `v2` | `v2` | none | ✅ |
| Chart `version` | `1.0.0` | n/a | none | ✅ |
| Deployment apiVersion | `apps/v1` | `apps/v1` | none | ✅ |
| Service apiVersion | `v1` | `v1` | none | ✅ |
| Deprecated K8s APIs | none | n/a | none | ✅ |
| Pod `securityContext` | ✅ GAP-470 baseline | meets | none | ✅ |
| Container hardening | ✅ readOnlyRootFS + drop caps + no privesc | meets | none | ✅ |
| Resource quotas per tier | ✅ FREE/BASIC/PREMIUM/ENTERPRISE explicit | meets | none | ✅ |
| Probe coverage | ✅ startupProbe + liveness + readiness | meets | none | ✅ |
| Image tag default | `tag: "latest"` | per-install override | P3 | document only |
| Multi-tenant pattern review | per-instance Deployment via `instanceId | trunc 8` naming | conflict với Wave X+ shared-DB multi-tenant pattern | P3 | tracked in parent GAP-465 §6 (deprecate kiteclass-template/ direction); chưa file separate gap |
| Service inventory | 1 service `core` only | per-instance scope correct | none | ✅ |

---

## Recommendations

### Immediate (this PR — none, audit-only scope per Bucket D)

Audit report shipped. No code changes per Wave 64 Bucket D constraints.

### Follow-up gaps filed (this PR)

1. **GAP-478 (P1)** — Bump `external-secrets.io/v1beta1 → v1` trong 2 helm templates + README example. Single-file mostly mechanical; Phase 1.5 prep.
2. **GAP-479 (P2)** — Phase 2 EKS migration prep batch: (a) add `kitehub-platform` to deployment.yaml, (b) decide ingress: render-template-or-remove, (c) bump subchart versions (kube-prometheus-stack 58.7.2 → ~75.x, loki-stack → modern loki+promtail). Batch into Phase 2 migration prep wave.

### Deferred (no separate gap — tracked in parent GAP-465)

- Multi-tenant pattern reconciliation (kiteclass-instance per-DB vs shared-DB
  pattern) — gap §6 already cites this; revisit at Phase 2 cutover.
- Image tag pinning enforcement (Pod Spec `tag: "latest"` default) — P3
  hygiene; addressed by override-prod.yaml convention; chưa critical.

---

## State-check evidence

Read-only commands run (per `agent-aws-access.md` §2 Tier 1):

```bash
ls infrastructure/helm/
find infrastructure/helm -type f \( -name "*.yaml" -o -name "*.yml" \)
wc -l infrastructure/helm/kitehub/values.yaml infrastructure/helm/kiteclass-instance/values.yaml ...
# Grep apiVersion across all helm templates
# Read Chart.yaml + values.yaml + templates/deployment.yaml + ExternalSecret
# Verify service inventory: ls kitehub/ | grep ^kitehub-
```

No `helm lint` / `helm template` / `kubectl --dry-run` executed (agent has no
cluster access; CI not yet wired per GAP-465 Phase 3 plan).

---

## Cross-references

- Parent: [GAP-465](../../gaps/GAP-465-helm-k8s-artifacts-validation-pre-phase-1-5-migration.md) (P2 audit-driven validation)
- Bundled into: GAP-415 (Phase 2 EKS migration plan)
- Wave 64 plan: `documents/03-planning/waves/wave-2026-05-11-64-cleanup-cluster.md` (Bucket D)
- Related rules:
  - `agent-aws-access.md` §2 (Tier 1 read-only scope used here)
  - `release-deploy-standard.md` §3.4 (helm prep MAJOR-release artifact)
- New follow-ups: GAP-478, GAP-479
