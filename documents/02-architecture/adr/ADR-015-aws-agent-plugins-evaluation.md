# ADR-015: AWS Agent Plugins Evaluation — Defer Adoption

**Status:** ACCEPTED
**Date:** 2026-04-18
**Deciders:** Tech Lead + Architect
**Reviewers:** DevOps
**Related Gap(s):** GAP-103 (Deploy Philosophy Consolidation)

## Context

AWS đã release [awslabs/agent-plugins](https://github.com/awslabs/agent-plugins) — bộ plugin chính thức cho AI coding agents (Claude Code, Cline, Amazon Q Developer) cung cấp specialized skills/agents cho AWS operations: EC2 management, Lambda deploy, CloudFormation, IAM advisor, S3, CodePipeline, cost explorer.

Dự án hiện:
- Dùng Claude Code heavy (40+ PRs shipped via AI agents)
- Dual-cloud strategy (Oracle Cloud staging + AWS prod target)
- Chưa commit AWS production (đang chạy Oracle Free Tier staging)
- Thesis defense cần justify deployment choices

Question: **Có nên adopt AWS Agent Plugins không?**

Forces at play:
- ✅ Productivity gain tiềm năng (agent tự deploy, tune, debug AWS resources)
- ❌ Vendor lock-in tăng (plugins chỉ work với AWS, đối nghịch cloud-agnostic philosophy — xem `deployment-strategy.md` §1.1)
- ❌ Thesis narrative phức tạp thêm (phải biện luận "tại sao chọn AWS-only tool")
- ⚠️ Pricing model plugin chưa rõ (per-invocation? free?)
- ⚠️ Permission scope — plugins có yêu cầu AWS admin full không?

## Decision

**We will DEFER adoption of AWS Agent Plugins until Q3 2026 review.** Not rejection — không adopt tại thời điểm này, revisit khi 3 conditions satisfied:

1. Pilot tenant #1 đã ký hợp đồng → commit production cloud (likely AWS)
2. Wave 6 (AI Billing + Observability) shipped → đã có Prometheus/Grafana baseline, có thể đánh giá replacement value
3. AWS Agent Plugins pricing model công khai và predictable

Trong lúc chờ, **KHÔNG dùng plugins trong any workflow** (CI/CD, agents, dev scripts).

## Consequences

### Positive
- ✅ Giữ cloud-agnostic philosophy ngắn hạn — option để migrate Oracle ↔ AWS vẫn mở
- ✅ Thesis narrative không phức tạp thêm — không cần biện luận AWS-only tooling
- ✅ Avoid pricing surprise — Claude Code chạy ~50 invocations/day, nếu plugin tính per-invocation có thể spike cost
- ✅ Team giữ manual-mastery AWS concepts (quan trọng cho thesis defense)
- ✅ Kernel AWS CLI + Terraform + Helm workflow đã đủ cho hiện tại

### Negative
- ❌ Miss potential productivity gain (ước tính 10-20% faster cho AWS ops nếu plugin work as advertised)
- ❌ Team không build familiarity sớm với ecosystem → nếu Q3 review decides adopt, learning curve
- ❌ Competitor đang adopt có thể ship nhanh hơn

### Neutral
- 🟡 Decision reversible — adopt ngay khi conditions fire
- 🟡 Plugin ecosystem còn non non mới (< 6 tháng) → đợi mature là prudent

### Confirmation

**Review triggers (any fires → revisit):**
- Pilot tenant #1 ký hợp đồng + commit AWS production
- Wave 6 observability deployed + stable 2 tuần
- AWS Agent Plugins ra v1.0 stable với clear pricing
- Competitor adoption + measurable advantage (qua conference talks, blog posts)
- Claude Code team recommend (official statement)

**Review cadence:** End Q3 2026 (July-Sep). Earlier nếu trigger fires.

**Metrics để đánh giá khi review:**
- AWS ops time per week (baseline hiện tại ~2h/week)
- Cost per Claude Code invocation (baseline: ~$0.01/invocation via API)
- Incident MTTD/MTTR on AWS resources
- Team comfort level với plugins (qua survey nếu adopt pilot)

## Alternatives Considered

### Alternative A: Full Adopt (all plugins)

**Pros:**
- Maximum productivity gain
- Team builds ecosystem familiarity early
- Potential CI/CD acceleration

**Cons:**
- Commit AWS-only trước khi revenue lock-in
- Đối nghịch cloud-agnostic philosophy (deployment-strategy.md §1.1)
- Thesis defense khó biện luận selective tool adoption
- Unknown pricing risk khi Claude Code usage scale

**Rejected because:** Premature. Chưa có paying customer commit AWS, adopt plugins = lock-in preemptive. Philosophy §1.1 yêu cầu flexibility vendor.

### Alternative B: Selective Adopt (cost explorer + IAM advisor only)

**Pros:**
- Limited vendor lock (chỉ 2 plugins, không full ecosystem)
- Cost explorer thực sự valuable (visibility into AWS spend)
- IAM advisor tăng security posture (least-privilege check)

**Cons:**
- Still adds AWS-specific tool dependency
- Plugins có thể interact với nhau — "selective" có thể không clean
- Maintenance burden — phải track updates cho 2 plugins
- Cost explorer có thể replicate bằng AWS Cost Explorer native (không cần plugin)

**Rejected because:** Value add marginal vs added complexity. Cost explorer native UI đủ cho hiện tại. Revisit Q3 2026.

### Alternative C: Reject entirely (stay CLI-only)

**Pros:**
- Maximum cloud-agnostic commitment
- Zero new vendor dependency
- Team giữ manual-mastery cho thesis

**Cons:**
- Miss permanent productivity opportunity
- Nếu ecosystem mature + team migrate late, catch-up cost cao
- Đối thủ có advantage nếu họ adopt

**Rejected because:** Too absolute. "Never adopt" rules out future option. Defer (Alternative D) mềm hơn và giữ option alive.

### Alternative D: Defer Q3 2026 Review (CHOSEN)

**Pros:**
- Keep option open
- Ecosystem có 6+ tháng để mature
- Có Wave 6 baseline để compare value
- Align với pilot tenant timeline

**Cons:**
- Risk: nếu Q3 review vẫn defer, decision fatigue
- Team không build familiarity → learning curve nếu adopt muộn

**Chosen because:** Balance giữa "don't commit prematurely" và "don't foreclose future". Explicit revisit cadence ngăn indefinite defer.

## Implementation Notes

### Migration strategy
- Hiện tại: no-op. Không dùng plugins, không add dependency.
- Q3 2026 review: follow ADR-015 amendment process (tạo ADR-NNN superseding 015 nếu adopt).

### Rollback plan
- N/A — decision là "không adopt", không có rollback.

### Feature flags
- N/A — không code change.

### Revisit trigger sources
- This doc Section "Confirmation"
- `.claude/rules/` future update nếu adopt
- Team quarterly retrospective agenda item

## References

- **External repo:** [awslabs/agent-plugins](https://github.com/awslabs/agent-plugins)
- **Related ADRs:**
  - ADR-008 (Resilience) — already applies cloud-agnostic pattern via Circuit Breaker
  - ADR-014 (Async Jobs Queue) — RabbitMQ chosen over cloud-specific messaging
- **Related docs:**
  - `deployment-strategy.md` §1.1 Cloud-agnostic Helm-first (philosophical foundation)
  - `infrastructure/terraform-aws/` — current AWS IaC (managed manually)
- **External references:**
  - [Claude Code Plugin Directory](https://docs.claude.com/en/docs/claude-code/plugins) (general plugin docs)
  - [AWS Agent Plugins README](https://github.com/awslabs/agent-plugins/blob/main/README.md)

## Log

- **2026-04-18:** Created (GAP-103). Decision = defer to Q3 2026 review. Reason: premature commit AWS-only tooling trước khi paying customer commit production cloud.
