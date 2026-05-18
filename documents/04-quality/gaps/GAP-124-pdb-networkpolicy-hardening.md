# GAP-124: PodDisruptionBudget + NetworkPolicy Hardening

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Reliability + Security
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Cluster reliability during node drain + security isolation

## Problem

1. **No PodDisruptionBudget (PDB):** khi k8s node drain (upgrade, autoscaler), kubelet có thể evict all replicas của 1 deployment cùng lúc → service downtime.
2. **No NetworkPolicy:** pod-to-pod traffic unrestricted. Compromise 1 pod → access mọi services trong cluster. Multi-tenant isolation violation.

Evidence:
- `infrastructure/helm/kitehub/templates/` — không có `pdb.yaml` hoặc `networkpolicy.yaml`
- `infrastructure/k8s/kitehub/` + `infrastructure/k8s/kiteclass-template/` — chỉ có deployment + service + configmap + secrets + namespace + ingress; không có PDB/NetworkPolicy

## Root Cause

Security hardening + reliability features chưa được add vào Helm chart baseline. MVP focus on functional.

## Proposed Fix

### PDB
1. Create `infrastructure/helm/kitehub/templates/pdb.yaml`:
   ```yaml
   {{- range $name, $svc := dict "gateway" .Values.gateway "subscription" .Values.subscription ... }}
   apiVersion: policy/v1
   kind: PodDisruptionBudget
   metadata:
     name: kitehub-{{ $name }}-pdb
   spec:
     minAvailable: 1  # cho service có 2+ replicas
     selector:
       matchLabels:
         app: kitehub-{{ $name }}
   {{- end }}
   ```

### NetworkPolicy
1. Default deny all inter-pod traffic
2. Whitelist:
   - Gateway → kitehub-* services
   - kitehub-* services → postgres, redis, rabbitmq, minio
   - kiteclass-core → postgres, redis, rabbitmq, minio
   - kitehub-branding → Ollama (if in-cluster)
   - All services → DNS (kube-system)
3. Explicit deny: kiteclass-* ↔ kitehub-* direct pod talk (must go through gateway)

### Security Context
1. Add `securityContext` vào deployments:
   ```yaml
   securityContext:
     runAsNonRoot: true
     runAsUser: 1000
     readOnlyRootFilesystem: true
     allowPrivilegeEscalation: false
     capabilities:
       drop: ["ALL"]
   ```

## Acceptance Criteria

- [ ] PDB cho 6 kitehub services + kiteclass-core (minAvailable: 1)
- [ ] NetworkPolicy default-deny + explicit allow rules
- [ ] Test: drain node → service vẫn serve requests
- [ ] Test: curl từ 1 pod sang pod khác (không whitelisted) → denied
- [ ] SecurityContext set trên tất cả deployments
- [ ] Pod Security Standards (Baseline/Restricted) enforcement

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §8
- Related: security audit (2026-04-17) — network hardening scope

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
- **2026-05-18 (Wave 93 §7.2 row 2 user decision)** — **Phase corrected: phase-1.5-paid → phase-2.** Per Wave 93 re-triage audit — PDB + NetworkPolicy là K8s-specific; cần khi migrate EKS Phase 2 per GAP-415. Same family GAP-123. Original phase-1.5-paid assignment incorrect. CSV row updated.
