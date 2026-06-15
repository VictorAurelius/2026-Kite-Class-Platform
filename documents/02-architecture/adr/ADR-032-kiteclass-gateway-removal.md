# ADR-032: Remove `kiteclass-gateway` service

**Status:** ACCEPTED
**Date:** 2026-05-18
**Author:** @nguyenvankiet (solo-dev)
**Supersedes:** N/A
**Superseded by:** N/A
**Related:** ADR-023 (Gateway key resolver strategy), ADR-025 (Phase 1 BETA AWS architecture), GAP-001

---

## Context

KiteClass module có 3 services khi khởi tạo: `kiteclass-core`, `kiteclass-frontend`, `kiteclass-gateway`.

Tuy nhiên kiến trúc đã evolve:
- **Production (integrated mode):** `kitehub/docker-compose.kitehub.yml` chỉ start 2 service đầu (core + frontend). Routing được xử lý bởi shared `kite-gateway` (per ADR-023). `kiteclass-gateway` redundant.
- **Standalone mode** (dev-only): `kiteclass/docker-compose.dev.yml` vẫn dùng `kiteclass-gateway` — KHÔNG có active use case ngoài dev sandbox.

GAP-001 filed 2026-04-14 đề nghị quyết định:
- Option A: Xóa `kiteclass-gateway` (eliminate dead code)
- Option B: Giữ cho standalone mode (formalize via ADR + 2-mode docs)

Decision deferred >1 tháng. Recent maintenance (Wave 85 Tier 2 config, dependabot bumps Wave 96) tốn effort cho dead path.

## Decision

**Option A — Remove `kiteclass-gateway` service entirely.**

Wave 96 PR2 thực hiện:
1. Delete folder `kiteclass/kiteclass-gateway/` (154 git-tracked file)
2. Update `kiteclass/docker-compose.dev.yml` — remove gateway service block
3. Delete `.github/workflows/gateway-ci.yml` (dedicated CI workflow)
4. Update `.github/workflows/docker-build-push.yml` — remove 4 build entries
5. Update `.github/dependabot.yml` — remove 2 monitoring entries
6. Update `infrastructure/terraform-aws/ecr.tf` — remove ECR repo
7. Update `infrastructure/terraform-aws/iam.tf` — update IAM comment
8. Delete `infrastructure/k8s/kiteclass-template/gateway-deployment.yaml` + update sister manifests
9. Update 4 scripts (`test-local.sh`, `dev-docker.sh`, `qa-collect.sh`, `sweep-be-cors-origins.sh`)
10. Update active docs (`kiteclass-architecture.md`, parent-portal use-cases, ADR-020, ADR-031, PlantUML CI/CD diagram)
11. Close GAP-001 → DONE

## Rationale

### Why Option A

1. **No active use case for standalone mode** — Phase 1 BETA scope = solo-dev + invite-only beta tenants on shared infrastructure. Standalone single-tenant deployment not in roadmap Phase 1-2.
2. **Shared `kite-gateway` already covers routing** (per ADR-023). `kiteclass-gateway` duplicates effort with zero benefit khi integrated mode is the only production path.
3. **Maintenance cost** — recent commits show active dependabot bumps + Wave 85 Tier 2 config applied to dead-path service. Cost compounds với every Spring Boot / CVE update.
4. **Solo-dev simplification** — eliminating 154 file + 1 CI workflow + ECR repo + IAM perms + 3 k8s manifests = real cognitive load reduction.
5. **Future scenario flexibility preserved via git history** — nếu cần standalone mode later, `git revert` hoặc `git checkout <commit>~ kiteclass/kiteclass-gateway/` để khôi phục.

### Why NOT Option B

1. Writing ADR + maintaining 2-mode docs requires ongoing effort even though standalone path không có active user.
2. ADR-only doesn't reduce maintenance cost (deps bumps still required).
3. "Just in case" preservation pattern accumulates dead code — anti-pattern per `design-patterns.md` YAGNI principle.

## Consequences

### Positive

- ~154 git-tracked file removed (~956KB source, plus build artifacts)
- 1 CI workflow deleted (`gateway-ci.yml`)
- 1 ECR repo terraform-managed → removed
- 2 dependabot monitoring entries removed → less Dependabot noise
- 4 scripts simplified
- Cognitive load for dev onboarding reduced (1 fewer service to understand)
- Future deps bumps + CVE fixes don't apply to dead path

### Negative

- Standalone single-instance KiteClass deployment requires re-implementation if ever needed (recoverable via git history)
- 17+ references in historical planning docs (`documents/03-planning/`) become stale (acceptable — historical record, not active source of truth per `docs-archival-cadence.md` archival policy)
- Existing screenshots / diagrams reference 3-service kiteclass topology — gradual sweep over time

### Risks

| # | Risk | Mitigation |
|---|---|---|
| 1 | Hidden production dependency on kiteclass-gateway | State-checked: NOT in `kitehub/docker-compose.kitehub.yml` (production compose); verified via grep |
| 2 | CI workflow refs break | All 4 docker-build-push.yml entries + entire gateway-ci.yml + 2 dependabot entries removed atomic in same PR |
| 3 | Dev workflow scripts break | 4 scripts updated to skip kiteclass-gateway code paths |
| 4 | Future need standalone mode | Git history preserves; can revert or re-create from commit |
| 5 | k8s template breakage | `infrastructure/k8s/kiteclass-template/` is Phase 2 EKS scope per GAP-415/479; removal aligned with Phase 2 redesign |

## Alternatives considered

- **Option B (keep + ADR)** — rejected per Rationale §2 (dead path with maintenance burden).
- **Phased removal (decide now, remove later)** — rejected per `gap-done-discipline.md` §3 PARTIAL exit ramp guard — premature PARTIAL creates noise without commitment value. Single-PR atomic removal cleaner.
- **Keep folder but disable CI** — half-measure; folder still in repo creating onboarding confusion. All-or-nothing is cleaner.

## Backup + recovery path

Pre-removal state preserved via:

| Layer | Mechanism | Recovery command |
|---|---|---|
| 1. Git history | Commit history immutable | `git show <pre-removal-commit-sha>:kiteclass/kiteclass-gateway/<path>` |
| 2. Archive branch (local) | `archive/kiteclass-gateway-pre-removal-2026-05-18` | `git checkout archive/kiteclass-gateway-pre-removal-2026-05-18 -- kiteclass/kiteclass-gateway/` |
| 3. Archive branch (remote) | Pushed to origin (GitHub + GitLab) | `git fetch origin archive/kiteclass-gateway-pre-removal-2026-05-18:archive/kiteclass-gateway-pre-removal-2026-05-18` |
| 4. PR revert | `git revert <PR-sha>` undoes entire removal | Standard git workflow |

Branch retention: **permanent** (no expiry policy). Per `docs-archival-cadence.md`, `archive/*` branches are read-only history; never deleted.

## References

- GAP-001 — `documents/04-quality/gaps/unclassified/closed/GAP-001-kiteclass-gateway-decision.md` (DONE this PR)
- Wave 96 sweep — `documents/04-quality/audits/meta/2026-05-18-wave-96-gap-retriage-full-sweep.md`
- ADR-023 — Gateway key resolver strategy (shared `kite-gateway` design)
- ADR-025 — Phase 1 BETA AWS Singapore Free Tier architecture
- Rule — `.claude/rules/design-patterns.md` §1.1 YAGNI

## Log

- **2026-05-18:** ADR shipped same PR as Wave 96 PR2 actual removal — atomic decision + implementation. User explicit Option A per GAP-001 triage. Recurrence #3 of outside-in pattern (user picked re-triage over PR3 sweep, surfaced GAP-001 as example of deferred decision). Decision validity Phase 1-2 BETA scope; revisit if Phase 3+ K-12 introduces standalone single-tenant deployment requirement.
- **2026-06-15 (cleanup completion — GAP-1408):** Deploy-parity investigation found Wave 96 PR2 cleanup INCOMPLETE — ~19 active files still referenced `kiteclass-gateway` (source folder `kiteclass/kiteclass-gateway/` 3 tracked files, `ecr.tf` repo, `iam.tf` comment, `docker-compose.dev.yml` + `docker-compose.kc.yml` blocks, k8s `ingress.yaml`/`frontend-deployment.yaml`, 8 scripts, `prometheus.yml`, `dependabot.yml`, `.codecov.yml`). Completed source scrub this session (grep `kiteclass-gateway` over `*.yml/*.tf/*.sh` excl docs/adr = 0 active reference; only ADR-removal annotation comments remain). DEFERRED: `terraform apply` to destroy live ECR repo `kite/kiteclass-gateway` (AWS account stopped — cost posture). Discovery GAP-1409 filed (dev-stack residual gateway-era assumptions). Per `incident-to-rule-pipeline.md` decision-doc→code-sync class.
