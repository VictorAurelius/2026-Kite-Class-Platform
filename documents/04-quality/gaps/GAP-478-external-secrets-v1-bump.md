# GAP-478: External Secrets Operator `v1beta1 → v1` API bump

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-11 (Wave 64 Bucket D helm staleness audit)
**Affects:** `infrastructure/helm/**` templates using ExternalSecret CRD

## Problem

Wave 64 Bucket D audit (`documents/04-quality/audits/helm-k8s/2026-05-11-staleness-audit.md`) found helm templates using `external-secrets.io/v1beta1` API — **deprecated in ESO v0.10+**, current stable is `v1`.

Single mechanical bump across 2 helm templates + README example. Phase 1.5 prep work; will block Phase 2 EKS migration if ESO v0.10+ deployed.

## Proposed Fix

1. Grep `external-secrets.io/v1beta1` in `infrastructure/helm/` → identify all callsites
2. Replace `apiVersion: external-secrets.io/v1beta1` → `apiVersion: external-secrets.io/v1`
3. Verify CRD field compatibility (likely 1:1 for ExternalSecret/SecretStore)
4. Update README example if any
5. `helm template` to verify rendering

## Acceptance Criteria

- [ ] All `external-secrets.io/v1beta1` references bumped to `v1`
- [ ] `helm template infrastructure/helm/kitehub` exit 0 + no validation warnings
- [ ] README example consistent
- [ ] ESO chart version note if helm dep declared

## Related

- Audit: `documents/04-quality/audits/helm-k8s/2026-05-11-staleness-audit.md`
- Parent: GAP-465 (helm staleness audit, DONE Wave 64)
- Sibling: GAP-479 (Phase 2 EKS migration batch)

## Log

- **2026-05-11:** Filed as Wave 64 Bucket D audit-driven follow-up. P1 — straightforward mechanical bump.
