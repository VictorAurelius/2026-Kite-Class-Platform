# GAP-479: Phase 2 EKS migration helm batch (kitehub-platform + ingress + subchart bumps)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-05-11 (Wave 64 Bucket D helm staleness audit)
**Affects:** `infrastructure/helm/kitehub/**` Phase 2 EKS migration prep

## Problem

Wave 64 Bucket D audit identified 3 helm gaps that don't block Phase 1 BETA (docker-compose deploy per ADR-025) but **surface at Phase 2 EKS migration**:

1. **`kitehub-platform` missing from helm deployment** — service exists in code (`kitehub/kitehub-platform/`) but `infrastructure/helm/kitehub/templates/deployment.yaml:1` `dict` literal only renders 6 services (gateway/subscription/branding/admin/email/frontend). No Deployment/Service/ServiceMonitor for kitehub-platform.

2. **Ingress declared but not rendered** — `values.yaml:103-114` defines `ingress.enabled=true` + hosts `kitehub.me` + ALB annotations. No `templates/ingress.yaml` exists → silent no-op. Phase 1 BETA bypasses helm Ingress (ALB managed by terraform), but Phase 2 EKS migration needs decision.

3. **Subchart version drift:**
   - `loki-stack 2.10.2` — deprecated upstream (Grafana migrated to `loki` v6.x + standalone `promtail`)
   - `kube-prometheus-stack 58.7.2` — ~17 minor versions stale (current ~75.x)

## Proposed Fix

Batch into Phase 2 EKS migration prep wave (gated by Phase 1 BETA stabilization):

1. Extend `infrastructure/helm/kitehub/templates/deployment.yaml` `dict` to include `platform:` + add `platform:` block to `values.yaml`
2. Decide ingress direction:
   - Option A: create `templates/ingress.yaml` rendering ALB Ingress from values
   - Option B: remove `ingress.*` from values.yaml (terraform-only ALB)
3. Subchart bumps:
   - Replace `loki-stack` → `loki` v6.x + `promtail` standalone (separate `Chart.yaml` deps)
   - Bump `kube-prometheus-stack 58.7.2 → ~75.x` (check breaking changes in CHANGELOG)
4. `helm dependency update` + `helm template` validate

## Acceptance Criteria

- [ ] `kitehub-platform` renders Deployment + Service in `helm template` output
- [ ] Ingress decision made + executed (template added OR values cleaned)
- [ ] `loki-stack` migrated to modern `loki` + `promtail`
- [ ] `kube-prometheus-stack` bumped to current stable
- [ ] `helm template` clean; no validation warnings
- [ ] Migration notes in audit follow-up report

## Related

- Audit: `documents/04-quality/audits/helm-k8s/2026-05-11-staleness-audit.md`
- Parent: GAP-465 (helm staleness audit, DONE Wave 64)
- Sibling: GAP-478 (ESO v1 bump, P1)
- Bundled into: GAP-415 (Phase 2 EKS migration plan)

## Log

- **2026-05-11:** Filed as Wave 64 Bucket D audit-driven follow-up. P2 — batched into Phase 2 EKS migration; not blocking Phase 1 BETA.
