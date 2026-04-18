# GAP-103: Deploy Philosophy Consolidation + AWS Agent Plugins ADR

**Status:** 🟢 DONE (2026-04-18, PR #351)
**Priority:** 🟢 P3
**Domain:** Architecture — Deployment strategy + AI tooling evaluation
**Found:** 2026-04-18 (session Q&A về deploy philosophy + AWS Agent Plugins)
**Affects:** `documents/02-architecture/deployment-strategy.md` (new), `documents/02-architecture/adr/ADR-002-*`

## Problem

**Part 1 — Deploy docs scattered, no single source of truth:**

Deploy-related documentation hiện rải rác 6 vị trí:
- `documents/03-planning/infrastructure/kitehub-oracle-cloud-deployment.md`
- `documents/03-planning/implementation/kiteclass-docker-deployment.md`
- `documents/05-guides/operations/runbooks/deployment-procedures.md`
- `documents/05-guides/deploy-go-nogo-checklist.md`
- `documents/05-guides/vietnamese/huong-dan-deploy-oracle-cloud.md`
- `documents/08-thesis/references/deployment-guide.md`

Không có doc nào trình bày **philosophy tổng thể** (dual-cloud + Helm-first + cost-conscious) để developers/reviewers/thesis readers hiểu quyết định.

**Part 2 — AWS Agent Plugins evaluation missing:**

AWS ra mắt [awslabs/agent-plugins](https://github.com/awslabs/agent-plugins) — bộ plugin chính thức cho Claude Code/Cline/Amazon Q, cung cấp AI agents cho AWS operations (EC2, Lambda, CloudFormation, IAM, S3, CodePipeline, cost explorer).

Dự án chưa đánh giá:
- Có nên adopt? (trade-off: productivity vs cloud-agnostic)
- Impact đến dual-cloud philosophy (AWS + Oracle Cloud)?
- Impact đến thesis narrative (biện luận chọn tool độc quyền AWS)?
- Chi phí per-invocation khi Claude Code chạy nhiều?

## Impact

**Cao nếu không fix:**
- Reviewer/contributor không biết philosophy → propose changes lệch direction (vd. lock-in AWS service)
- Thesis defense khó biện luận deployment choice
- Team không có framework đánh giá AI tooling khác trong tương lai (Anthropic MCP, Azure AI agents, GCP)

**Thấp nếu defer:**
- Engineering không block (deploy đang chạy Oracle + Docker)
- Decision có thể defer đến khi có paying customer #1 (commit cloud)

## Proposed Fix

### Part 1: `deployment-strategy.md` single source

Tạo `documents/02-architecture/deployment-strategy.md` tổng hợp:

1. **Philosophy (5 nguyên tắc):**
   - Cloud-agnostic Helm
   - Cost-conscious (Oracle Free Tier staging, AWS prod)
   - Reproducible local = prod
   - GitOps ready
   - Thesis-friendly (biện luận được trong context K-12 VN)

2. **Matrix environments:**

   | Layer | Tech | Location | Why |
   |-------|------|----------|-----|
   | Local dev | Docker Compose | Developer machine | Reproducibility |
   | Staging | Oracle Cloud Free (ARM Ampere A1) | Always-free VM | Zero cost |
   | Prod (target) | AWS EKS hoặc Oracle OKE | Helm chart portable | Options open |
   | IaC | Terraform (aws + oracle) | `infrastructure/terraform-*` | Multi-cloud |
   | K8s | Helm charts | `infrastructure/helm` | Rollback, versioning |
   | CI/CD | GitHub Actions | `.github/workflows` | Free tier |
   | Observability | Prometheus + Grafana self-hosted | Cluster | No vendor lock |

3. **Cross-references:** link tới 6 deploy docs hiện có, explain cái nào operator-facing, cái nào planning.

### Part 2: ADR-002 AWS Agent Plugins Evaluation

`documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md` theo existing Nygard template (`adr/_TEMPLATE.md`):

- **Context:** AWS Agent Plugins released 2026; Claude Code dùng mạnh trong dự án
- **Decision Drivers:** productivity, cost, cloud-agnostic commitment, thesis defense, least-privilege
- **Options considered:**
  1. Full adopt (all plugins)
  2. Selective adopt (chỉ cost explorer + IAM advisor)
  3. Reject (stay CLI-only)
  4. Defer evaluation to Q3 2026
- **Decision outcome (recommended):** Option 4 — Defer. Revisit sau Wave 6 khi Prometheus/Grafana ổn, sau pilot tenant #1.
- **Consequences:**
  - ✅ Giữ cloud-agnostic philosophy ngắn hạn
  - ✅ Thesis narrative không phức tạp thêm
  - ❌ Miss potential productivity gain
  - ⚠️ Risk: plugins phát triển nhanh, lỗi tương lai nếu defer quá lâu

**Red flags nêu trong ADR:**
- Plugin pricing per-agent-invocation?
- Plugin yêu cầu AWS admin full access?
- Plugin lock-in proprietary workflows?

## Acceptance Criteria

### Part 1 — DONE
- [x] `02-architecture/deployment-strategy.md` tạo với 5 nguyên tắc + matrix
- [x] Link từ `02-architecture/README.md` (Key Documents + Directory Map)
- [x] Cross-reference 6 deploy docs hiện có (Section 6)
- [x] Section "Future: migration paths" (Oracle → AWS + AWS → Oracle contingency)

### Part 2 — DONE
- [x] ADR-015 written theo existing Nygard template (`adr/_TEMPLATE.md`)
- [x] 4 options đánh giá với pros/cons (Full adopt | Selective | Reject | Defer)
- [x] Decision outcome = Defer Q3 2026 + 5 revisit triggers clear
- [x] Related link tới AWS Agent Plugins repo
- [x] Index `adr/README.md` updated (14 → 15 ADRs)

## Dependencies

- **GAP-101** — `02-architecture/README.md` tồn tại để link deployment-strategy
- **GAP-102 Part 2** — ADR template + README cần có trước khi viết ADR-002
- **Soft: Wave 6 (AI Billing + Observability)** — nếu Wave 6 dùng AWS CloudWatch thay Prometheus, decision matrix thay đổi

## Related

- [awslabs/agent-plugins](https://github.com/awslabs/agent-plugins) — AWS repo
- `infrastructure/terraform-aws`, `infrastructure/terraform-oracle` — current IaC
- `infrastructure/helm` — current K8s packaging
- Thesis chapter on deployment (`08-thesis/chapter-mapping.md`)
- GAP-102 ADR kickoff
