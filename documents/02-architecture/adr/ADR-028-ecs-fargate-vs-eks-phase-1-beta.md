# ADR-028: ECS Fargate vs EKS for Phase 1 BETA Container Orchestration

**Status:** ACCEPTED
**Date:** 2026-05-11
**Deciders:** @nguyenvankiet (solo-dev, acting CTO)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-464 (this ADR closes ADR-025 §5 commitment); GAP-415 (Phase 2 EKS Migration Plan — to be cross-noted)
**Related Rule(s):** `.claude/rules/release-deploy-standard.md` §3 (per-bump-type artifact checklist); ADR-025 §5 (commitment to file follow-up)

---

## Context

ADR-025 (AWS-only Deploy for Phase 1 BETA, 2026-05-07) §"Implementation Notes" §5 deferred the container orchestration decision:

> "Free tier favor **ECS Fargate** vì EKS control plane charge $73/tháng KHÔNG có free tier. **Decision tracked trong follow-up gap**; this ADR scope = Oracle→AWS switch."

GAP-464 (2026-05-11) surfaced that this follow-up commitment had not been formally closed. Phase 1 BETA infrastructure is already EC2-aligned (Architecture B per `aws-architecture-sizing-matrix.md`; see `infrastructure/terraform-aws/ec2.tf` shipped Phase 2.3 apply 2026-05-08), but the question of which orchestration path to take when Phase 1.5 PAID scaling demands containerized multi-AZ remains open.

This ADR closes ADR-025 §5 by formally selecting an orchestration target for the **Phase 1 BETA → Phase 1.5 PAID early** scope, and documents the trigger gates that would justify revisiting the decision before Phase 2.

### Forces at play

- **Phase 1 BETA scale:** ~5-10 beta tenants invite-only, persona P1+P2 (solo teacher + center owner). Single t4g.small EC2 sufficient per ADR-025 §3.
- **Phase 1.5 PAID scale:** ~50-200 tenants projected over 4-6 tuần post-Phase 1 trigger; multi-AZ requirement emerges; containerized orchestration mid-cycle.
- **AWS Free Tier 12-month constraint:** EKS control plane = $73/mo flat, NO Free Tier coverage. ECS Fargate = $0/mo control plane (managed); Fargate task pricing per-vCPU-hour + per-GB-RAM-hour, with 12-month Free Tier on small task sizes.
- **Solo-dev ops capacity:** No team to absorb K8s operational complexity (CNI debugging, IRSA, kubectl troubleshooting, cluster autoscaler tuning, Helm release management).
- **Team expertise asymmetry:** Java + Spring Boot deep (KiteHub 6 services + KiteClass core); Kubernetes shallow (concept-level, not operator-level). Helm charts exist (`infrastructure/helm/`) but were authored during Wave-pack planning — not battle-tested in production.
- **Existing Terraform module shape:** `infrastructure/terraform-aws/` ships `ec2.tf` (174 LOC), `rds.tf`, `s3.tf`, `iam.tf` (20K LOC), `cloudwatch-dashboard.tf`, etc. — all aligned to single-host EC2 + RDS Architecture B. No `ecs.tf` or EKS module currently in tree. Both paths require greenfield Terraform module work for Phase 1.5 transition.
- **Cost trajectory under Free Tier:** Phase 1 BETA targets $0/mo within Free Tier limits (ADR-025 §3). Phase 1.5 PAID full Architecture C estimates (per `aws-architecture-sizing-matrix.md` §5): EKS path ≈ $250/mo (≈$73 control plane + 2× t3.medium workers ≈ $60 + RDS multi-AZ + LB + traffic); ECS Fargate path ≈ $150-200/mo (no control plane + 2 services × 0.5 vCPU + 1 GB ≈ $30-50 + same RDS/LB/traffic baseline).

---

## Decision Drivers

Six drivers, weighted by Phase 1 BETA + Phase 1.5 PAID early scope (NOT Phase 2 or Phase 3 K-12):

1. **AWS Free Tier compatibility** — does the option preserve $0/mo orchestration cost during the 12-month Free Tier window?
2. **Operational overhead for solo-dev** — debuggability, patch surface, on-call cognitive load, time-to-restore on incident.
3. **Phase 1.5 PAID scale headroom** — can the option absorb the projected jump from ~10 BETA tenants to ~200 PAID tenants without re-architecting?
4. **Team expertise alignment** — does the option match the solo-dev's current skill profile (Java + Spring Boot strong; K8s shallow)?
5. **Existing Terraform module shape + Helm investment** — what work is preserved vs. wasted under each option?
6. **Vendor lock-in tolerance vs. future migration path** — what does each option cost in portability terms, and is there a credible escape hatch toward Phase 2+ EKS if needed?

---

## Considered Options

### Option 1 — ECS Fargate (serverless containers, AWS-managed)

**Description:** Container orchestration via AWS ECS with Fargate launch type. Task definitions replace Helm charts. Service auto-scaling via CloudWatch metrics. No nodes to manage.

**Trade-offs:**
- ✅ $0/mo control plane (managed) — preserves Free Tier posture
- ✅ Free Tier 12-month coverage on small task sizes
- ✅ Serverless — zero node patching, zero CNI debugging, zero kubelet incidents
- ✅ Low operational complexity — fits solo-dev cognitive budget
- ✅ Native integration with ALB, IAM (task roles), Secrets Manager, CloudWatch
- ✅ AWS Service Auto Scaling native — no Cluster Autoscaler tuning
- ❌ AWS-locked — task definitions are ECS-specific; portability to GCP/Azure lost
- ❌ Helm chart investment in `infrastructure/helm/` becomes reference-only (artifacts NOT wasted — see "future migration path" below: charts remain a valid Phase 2 EKS migration starting point)
- ❌ Fargate Spot pricing unsuitable for stateful Spring Boot sessions (Phase 1.5 sticky-session workloads must use on-demand Fargate)
- ❌ Less mature multi-region tooling vs. EKS for eventual Phase 3 K-12 cross-region

### Option 2 — EKS (managed Kubernetes)

**Description:** AWS EKS with managed control plane + 2-3 worker nodes (t3.medium or Graviton). Helm charts in `infrastructure/helm/` deploy directly. Industry-standard K8s primitives.

**Trade-offs:**
- ✅ Industry-standard K8s — broad ecosystem, vast community knowledge
- ✅ Portable — same Helm charts deploy to GCP GKE / Azure AKS / on-prem if Phase 3 demands VN cloud (per ADR-025 §3 compliance migration trigger)
- ✅ Helm charts already authored (`infrastructure/helm/`) — investment preserved
- ✅ HPA + Cluster Autoscaler + IRSA mature
- ✅ Multi-region patterns standard at Phase 3 K-12 scale
- ❌ **$73/mo control plane — NO Free Tier coverage.** This single line item exceeds ADR-025 §3 "Free tier only" constraint immediately when EKS comes online.
- ❌ ~30% of projected Phase 1.5 full Architecture C baseline cost is the control plane alone (zero-traffic baseline = $73 + $60 worker minimum)
- ❌ Operational complexity HIGH for solo-dev: CNI plugin debugging, IAM IRSA misconfigurations, kubectl/Helm release management, node upgrade discipline, cluster autoscaler tuning
- ❌ Time-to-restore on incident higher — more layers between symptom and root cause (Pod → Node → CNI → SG → ALB → Target Group)

### Option 3 — EC2 + Docker Compose (lightweight, status quo extended)

**Description:** Keep Phase 1 BETA single-host EC2 + Docker Compose model. Scale vertically via instance-type bumps (t4g.small → t4g.medium → t4g.large). Add second instance + LB only when single-host saturates.

**Trade-offs:**
- ✅ Lowest cost — Free Tier 12-month covers t3.medium / t4g.small fully
- ✅ Zero new tooling — Phase 1 BETA team workflow unchanged
- ✅ Fastest to-launch (already shipped)
- ❌ Vertical-only scaling — caps at ~t3.xlarge / t4g.xlarge before forcing horizontal pivot
- ❌ No automatic recovery on instance failure — Phase 1.5 PAID multi-tenant uptime SLA cannot be met
- ❌ Multi-AZ requires manual orchestration via Compose-on-each-host pattern — not production-grade
- ❌ Phase 1.5 PAID full (~200 tenants) cannot run on this architecture safely

---

## Decision Outcome

**Selected: Option 1 — ECS Fargate** for Phase 1 BETA → Phase 1.5 PAID early/full container orchestration target.

### Rationale

1. **Free Tier alignment is decisive at Phase 1 BETA scope.** EKS $73/mo control plane violates ADR-025 §3 "Free tier only" constraint the moment it comes online — even at zero traffic. ECS Fargate $0/mo control plane preserves the entire Free Tier 12-month window for compute + RDS + storage + email — the budget primitives that matter for solo-dev mode.
2. **Solo-dev cognitive budget is decisive at Phase 1.5 scope.** K8s operational surface (CNI, IRSA, kubectl, Helm releases, cluster autoscaler, node upgrades) is well-documented as a multi-person discipline. Solo-dev cannot absorb the on-call surface area without compromising Phase 1.5 PAID launch velocity. ECS Fargate's "task definition + service" abstraction matches the team's Java + Spring Boot mental model directly.
3. **Architecture B Terraform module shape already aligns ECS-friendly.** `infrastructure/terraform-aws/` ships EC2 + RDS + ALB + IAM + Secrets Manager + CloudWatch — all directly reusable under ECS Fargate (swap `ec2.tf` for `ecs.tf` task definitions + service modules; ALB / RDS / IAM trust policies / Secrets / dashboards remain). Greenfield Terraform work for ECS adoption ≈ replacing `ec2.tf` (174 LOC) with `ecs.tf` + 2-3 task definitions. EKS adoption would require introducing an entirely new `eks.tf` module + worker node groups + add-ons + ALB Controller — net more new tree.
4. **Future migration path to EKS exists and is concrete.** Helm charts in `infrastructure/helm/` remain a valid Phase 2+ migration starting point. If Phase 2 scaling demands (K8s ecosystem features: operators, CRDs, GitOps via ArgoCD) emerge, the project pivots EKS using the existing Helm investment as the deployment manifest layer. The ECS Fargate stint Phase 1 BETA + 1.5 is not a dead-end — it's a deferral of K8s complexity until traffic + revenue justify the operational cost.
5. **Phase 3 K-12 cross-region multi-AZ concerns are real but Phase 3-scoped.** ECS Fargate's less-mature multi-region story is a Phase 3 K-12 trigger gate concern, not Phase 1.5 PAID. By Phase 3, counsel review + VN-cloud compliance migration (per ADR-025 §3 trigger) may pivot the entire orchestration question — possibly to non-AWS infrastructure. Locking EKS now to hedge a Phase 3 contingency that may not even be on AWS is premature optimization.

### Trade-offs explicitly accepted

- **AWS lock-in via ECS-specific task definitions.** Mitigated by Helm charts remaining authoritative for future Phase 2+ EKS migration; ECS task definitions are deployment-time artifacts that can be regenerated from the same container images.
- **Less mature multi-region tooling vs. EKS.** Acceptable Phase 1 BETA + 1.5 (single-region Singapore `ap-southeast-1` per ADR-025); revisit at Phase 2 if multi-region demand surfaces before counsel-driven Phase 3 cloud migration.
- **Fargate cost scaling Phase 2+.** Per `aws-architecture-sizing-matrix.md` §5 + GAP-464 estimates, Phase 2 P3 Fargate ≈ $250-350/mo vs. EKS Architecture C ≈ $400/mo — ECS Fargate continues to undercut at Phase 2 P3 scope. Phase 3 K-12 scope (~$700-900 Fargate vs. $1000+ EKS) flips closer to parity but remains within Fargate favor unless K8s ecosystem features become essential.

### Trigger gates to revisit this decision before Phase 2

The decision is firm for Phase 1 BETA → Phase 1.5 PAID. It SHOULD be re-opened (file ADR-029 or supersede this) IF any of:

1. **Phase 1.5 PAID full cost exceeds projection by >25%** in dress-rehearsal cost-modeling — ECS Fargate per-task pricing may surprise at higher tenant density; pivot back to EKS becomes economical at scale.
2. **K8s ecosystem features become essential** (operators, CRDs, GitOps via ArgoCD/Flux, service mesh, advanced HPA scenarios) before Phase 2.
3. **Team grows beyond solo-dev** and absorbs K8s operational surface — ECS Fargate's primary justification (cognitive load) inverts when ops capacity expands.
4. **Counsel-driven Phase 3 VN-cloud migration** (per ADR-025 §3 compliance trigger) — at that point, orchestration choice may follow VN cloud provider's managed K8s offering (Viettel/VNG/FPT typically ship K8s, not ECS-equivalent).

---

## Consequences

### Positive

- **Phase 1 BETA + Phase 1.5 stay within Free Tier $0/mo orchestration cost** — preserves ADR-025 §3 budget posture.
- **Operational simplicity matches solo-dev capacity** — no K8s on-call surface, no cluster maintenance windows, no CNI/IRSA debugging.
- **Architecture B Terraform module shape preserved** — `infrastructure/terraform-aws/` evolves additively (add `ecs.tf`) rather than re-architects (would have needed full `eks.tf` + worker groups + add-ons).
- **Fast Phase 1.5 cutover path** — task definitions + service modules are smaller surface than Helm charts + EKS cluster + addons.
- **Native AWS integration** — Task IAM roles, Secrets Manager, CloudWatch, ALB target groups, X-Ray all wire in via Fargate without custom CRDs.

### Negative

- **AWS lock-in deepens** — ECS task definitions are AWS-specific (mitigation: container images + Helm charts remain portable; only the orchestration manifest layer is AWS-bound).
- **Phase 2+ K8s ecosystem features unavailable** without migrating to EKS (mitigation: Helm charts kept current as Phase 2+ migration prep; trigger gate above flags when migration justified).
- **Fargate Spot pricing unavailable for stateful workloads** — Spring Boot sticky-session services must use on-demand Fargate pricing (mitigation: stateless refactor opportunities surface naturally during Phase 2 scaling work).
- **Multi-region story less mature than EKS** — Phase 3 K-12 cross-region requirements may force EKS pivot or VN cloud migration; not a Phase 1.5 concern.

### Neutral

- **Helm charts under `infrastructure/helm/`** become reference artifacts rather than active deployment manifests Phase 1 BETA + 1.5. They remain valuable as the Phase 2+ EKS migration starting point. No deletion; status reframed from "active deployment target" to "Phase 2+ migration prep."
- **GAP-415 (Phase 2 EKS Migration Plan)** status updated from "current default path" to "Phase 2+ trigger-gated path" — the migration remains a credible future direction, but is no longer the default Phase 1.5 target. Cross-link noted in `aws-architecture-sizing-matrix.md` §5 footnote (per GAP-464 AC).
- **`aws-architecture-sizing-matrix.md` §5 Architecture C** stays as the EKS reference architecture for documentation completeness, but Phase 1.5 active sizing follows ECS Fargate task definitions instead. Same-PR footnote pending GAP-464 AC closure.

---

## Implementation Notes

### Migration sequence (when Phase 1.5 trigger fires)

| Step | Owner | Phase |
|---|---|---|
| Author `infrastructure/terraform-aws/ecs.tf` (cluster + service modules) | dev | Phase 1.5 prep |
| Write per-service ECS task definitions (8 services: 6 KH backend + 1 frontend + 1 KiteClass core) | dev | Phase 1.5 prep |
| Wire IAM task roles + Secrets Manager bindings | dev | Phase 1.5 prep |
| Configure ALB target groups + listener rules per service | dev | Phase 1.5 prep |
| Configure Service Auto Scaling policies per service | dev | Phase 1.5 prep |
| Dress-rehearsal cost modeling on staging-parity (per GAP-380) | dev | Phase 1.5 prep |
| Cutover EC2 → ECS Fargate (blue-green via ALB weighted routing) | dev | Phase 1.5 trigger |
| Decommission `ec2.tf` (move to archive under `documents/07-archived/`) | dev | Phase 1.5 cutover |
| Update `aws-architecture-sizing-matrix.md` §5 with ECS Fargate as primary path | dev | Phase 1.5 cutover |

### Rollback plan

If ECS Fargate cutover fails Phase 1.5 dress-rehearsal OR cost projection exceeds Architecture C estimate by >25%:
- Revert to EC2 single-host (Architecture B) and continue vertical scaling (t4g.small → t4g.medium → t4g.large) until decision can be re-opened
- File ADR superseding this one with revised reasoning
- Consider EKS path via existing Helm charts as next candidate

### Monitoring / success criteria

- ECS Fargate cutover: 0 P0 incidents 2 weeks post-cutover
- Per-service task CPU sustain <60%, memory <70% under Phase 1.5 PAID full load
- Service auto-scaling triggers within 60s of metric threshold
- Phase 1.5 monthly cost ≤ Architecture C projection + 25% tolerance

---

## References

- ADR-025: AWS-only Deploy for Phase 1 BETA (Free Tier) — §5 commitment closed by this ADR
- GAP-464: ECS Fargate vs EKS architecture decision (ADR-025 §5 follow-up) — this ADR's parent gap
- GAP-415: Phase 2 EKS Migration Plan — status reframed to "Phase 2+ trigger-gated" (cross-link pending GAP-464 AC closure)
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` §5 Architecture C — EKS reference architecture retained; Phase 1.5 active path is ECS Fargate (footnote pending)
- `.claude/rules/release-deploy-standard.md` §3 (per-bump-type artifact checklist)
- `documents/02-architecture/adr/_TEMPLATE.md` (MADR pattern)
- ADR-018: DNS / Cloudflare (orchestration-neutral)
- AWS Free Tier reference: https://aws.amazon.com/free/
- AWS ECS Fargate pricing: https://aws.amazon.com/fargate/pricing/
- AWS EKS pricing: https://aws.amazon.com/eks/pricing/

---

## Log

- **2026-05-11** — ACCEPTED. Closes ADR-025 §5 commitment per GAP-464. Decision: ECS Fargate selected for Phase 1 BETA → Phase 1.5 PAID early/full. Trigger gates documented for Phase 2+ revisit (cost surprise / K8s ecosystem demand / team growth / counsel-driven Phase 3 VN-cloud migration). Helm charts under `infrastructure/helm/` reframed from "active deployment target" to "Phase 2+ EKS migration prep" — no deletion. GAP-415 cross-noting + `aws-architecture-sizing-matrix.md` §5 footnote pending under GAP-464 Acceptance Criteria (other Wave 58 Bucket C scope). Solo-dev acting CTO sign-off per CLAUDE.md decision context locked 2026-05-06.
