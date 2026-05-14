---
title: Wave 58 — Cleanup follow-up wave-pack (helm CI guard + infrastructure README + ECS ADR)
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [58]
gaps: [GAP-467, GAP-463, GAP-464]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 58 — Cleanup follow-up wave-pack

**Goal:** Đóng 3 disjoint follow-up gaps cuối Phase 1 BETA pre-launch cleanup queue: helm CI guard (Wave 57 AC #5 deferred) + infrastructure README sync Phase 1 BETA reality + ECS Fargate vs EKS ADR (ADR-025 §5 commitment unfulfilled).

**Trigger:** Post-Wave-57 cleanup follow-up — 1 deferred AC + 2 P2 gaps tồn đọng từ user 4-question deployment review (GAP-463/464) + ADR-025 §5 commitment chưa thực hiện.

**Estimated wall-clock:** ~1 ngày total với 3 background agents parallel; longest-bucket (A helm CI guard với self-test fixture) ~3h sequential equivalent.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** which personas / domains / waves does this serve?
- **DevOps / SRE** — Bucket A prevents helm-lint regression (Wave 57 GAP-467 chỉ chữa root cause; CI guard chặn recurrence).
- **Phase 1 BETA architecture closure** — Bucket C ADR-028 đóng ADR-025 §5 commitment ("EKS vs ECS Fargate evaluation as Phase 1 BETA closure trigger").
- **Pre-Phase-1.5 infrastructure audit prep** — Bucket B `infrastructure/README.md` sync giúp future readers không bị mislead bởi `terraform-oracle` archived + EKS hints + helm/k8s dormant references.
- **Wave 57 follow-up housekeeping** — Bucket A là deferred AC #5 từ GAP-467 Wave 57 Bucket A.

**Q2 (trade-offs):**
- **3 parallel buckets vs sequential** → 3 parallel per `feedback_parallel_agent_strategy.md` rule #1. Zero overlap (scripts+CI / infrastructure docs / adr docs). Rejected: sequential — chậm 3x.
- **Bucket A scope** → script-based check (`scripts/check-helm-lint.sh`) + workflow job entry. Rejected: Husky pre-commit hook — helm binary install requirement quá nặng cho local dev; CI-only đủ.
- **Bucket C scope** → ADR-028 draft với decision (recommend ECS Fargate per Phase 1 BETA Free Tier constraints) + ACCEPTED status sau review. Rejected: PROPOSED-only — ADR-025 §5 explicit commit là full evaluation kèm decision.

**Q3 (risks):**
- **Risk A: helm CI guard trip trên existing helm chart edge cases** — `helm lint` strict mode có thể flag pre-existing warnings không liên quan. Mitigation: agent run `helm lint` trước khi viết check để baseline; check script chỉ fail trên ERROR không phải WARNING.
- **Risk B: Bucket C ADR-028 decision conflict với existing infrastructure** — agent có thể recommend EKS nhưng existing helm charts là target ECS Fargate. Mitigation: agent đọc ADR-025 + `infrastructure/terraform-aws/` first; decision phải align với existing Terraform module shape.
- **Risk C: Bucket B `infrastructure/README.md` content stale beyond fix scope** — README có thể lag toàn diện. Mitigation: scope ngắn — sync chỉ 4 vấn đề user-flagged (terraform-oracle archived, EKS misleading, helm dormant, k8s dormant); broader audit defer follow-up.
- **Risk D: zero overlap assumption** — A touches `scripts/` + `.github/workflows/script-quality.yml`; B touches `infrastructure/README.md`; C touches `documents/02-architecture/adr/ADR-028-*.md`. Verified disjoint via file glob.
- **Risk E: token-quota mid-stream** — coordinator context ~22% post-Wave-57; 3 agent spawns cuối session có risk. Mitigation: Opus medium effort thay Opus full (LOW-MEDIUM stake docs/scripts work); agents brief ngắn gọn.

---

## 2. Task Breakdown

| Bucket | Gap | Owner | Effort | Disjoint? |
|--------|-----|-------|--------|-----------|
| A | GAP-467 AC #5 | bg-agent | ~3h | ✅ scripts+CI only |
| B | GAP-463 | bg-agent | ~1-2h | ✅ infrastructure/README only |
| C | GAP-464 | bg-agent | ~2-3h | ✅ ADR docs only |

Disjoint check: A `scripts/check-helm-lint.sh` + `.github/workflows/script-quality.yml`; B `infrastructure/README.md` only; C `documents/02-architecture/adr/ADR-028-*.md` only. Fully disjoint, zero rebase expected.

---

## 4. State-Check Evidence

| Symbol | Type | grep command | Result | Verdict |
|---|---|---|---|---|
| `scripts/check-helm-lint.sh` | Script file | `ls scripts/check-helm-lint.sh` | absent | 🆕 to-be-created (Bucket A) |
| `.github/workflows/script-quality.yml` | Workflow | `ls .github/workflows/script-quality.yml` | exists 12.4K | ✅ exists (Bucket A appends job) |
| `infrastructure/README.md` | Doc | `ls infrastructure/README.md` | exists 1.6K | ✅ exists (Bucket B edits) |
| `documents/02-architecture/adr/ADR-028-*.md` | ADR | `ls documents/02-architecture/adr/ADR-028-*.md` | absent | 🆕 to-be-created (Bucket C) |
| `documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md` | ADR | `ls documents/02-architecture/adr/ADR-025-*.md` | exists | ✅ exists (Bucket C cites §5) |
| `infrastructure/terraform-aws/` | Module | `ls infrastructure/terraform-aws/` | exists | ✅ exists (Bucket C reviews shape) |
| `infrastructure/helm/kitehub/` | Chart | `ls infrastructure/helm/kitehub/` | exists | ✅ exists (Bucket A target) |

No `| head` truncation. All 7 symbols verified.

---

## 3. Scope (compact schema)

**Stake tier:** LOW-MEDIUM → model: Opus medium (3 buckets all docs/scripts; not security or business-logic; per `feedback_sonnet_baseline_context_thrash.md` Opus medium fallback for LOW-stakes wave-pack).
**Cross-layer?:** NO → skip Bucket 0 Foundation. Pure infra/docs.

| # | Bucket | Gap | Priority | Files (glob) | Spawn order |
|:-:|--------|-----|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-467 AC #5 | 🟠 P1 | `scripts/check-helm-lint.sh` (NEW) + `.github/workflows/script-quality.yml` (append job) | parallel |
| 2 | **B** | GAP-463 | 🟡 P2 | `infrastructure/README.md` | parallel |
| 3 | **C** | GAP-464 | 🟡 P2 | `documents/02-architecture/adr/ADR-028-ecs-fargate-vs-eks-phase-1-beta.md` (NEW) | parallel |

### Bucket A — Helm CI guard

- Files (RELATIVE):
  - **NEW** `scripts/check-helm-lint.sh` — runs `helm lint infrastructure/helm/kitehub` + `helm template infrastructure/helm/kitehub`; exits 0 nếu cả 2 pass, exits 1 nếu fail. Handles "helm not installed" gracefully (1-liner install instruction).
  - **EDIT** `.github/workflows/script-quality.yml` — append new job `helm-lint` mirror pattern của existing `readme-freshness` / `alert-runbook-url` jobs; trigger trên `paths: [infrastructure/helm/**, scripts/check-helm-lint.sh, .github/workflows/script-quality.yml]`.
- Tests:
  - Self-test 1: `bash scripts/check-helm-lint.sh` clean trên current main (chart đã fixed Wave 57)
  - Self-test 2: `bash scripts/check-helm-lint.sh` baseline check workflow YAML valid (`python3 -c "import yaml; yaml.safe_load(...)"`)
- Acceptance (closes GAP-467 AC #5):
  - [ ] `scripts/check-helm-lint.sh` exists + executable + chmod +x
  - [ ] Script handles helm-missing case gracefully (exits với clear install instruction, không crash)
  - [ ] New `helm-lint` job added to `script-quality.yml` với `paths:` filter
  - [ ] Workflow YAML validates (`python3 yaml.safe_load`)
  - [ ] Self-test passes (exit 0) trên current main
  - [ ] GAP-467 Status flip 🟡 PARTIAL → 🟢 DONE if all AC checked

### Bucket B — Infrastructure README sync

- Files (RELATIVE):
  - **EDIT** `infrastructure/README.md` — sync 4 user-flagged issues:
    - `terraform-oracle/` archived (folder đã move sang `documents/07-archived/`?) — clarify hoặc remove reference
    - EKS hint misleading — Phase 1 BETA dùng ECS Fargate per Architecture B, NOT EKS
    - `helm/` dormant — chart exists nhưng chỉ deploy via Phase 1.5+ scope; clarify "future use"
    - `k8s/` dormant — same status; clarify
- Tests:
  - Read current `infrastructure/README.md` + `documents/04-quality/gaps/GAP-463*.md` for problem statement
  - Edit để reflect Phase 1 BETA reality (ECS Fargate active, helm+k8s dormant, oracle archived)
  - Cross-link to ADR-025 (AWS-only Free Tier) + ADR-028 (ECS vs EKS, ship same wave)
- Acceptance (closes GAP-463):
  - [ ] `terraform-oracle` reference correctly framed (archived or removed)
  - [ ] EKS misleading hint removed/corrected → ECS Fargate per ADR-025
  - [ ] `helm/` + `k8s/` framed as "Phase 1.5+ future" với clear status
  - [ ] Cross-link ADR-025 + ADR-028
  - [ ] GAP-463 Status flip 🔵 OPEN → 🟢 DONE

### Bucket C — ADR-028 ECS Fargate vs EKS decision

- Files (RELATIVE):
  - **NEW** `documents/02-architecture/adr/ADR-028-ecs-fargate-vs-eks-phase-1-beta.md` — follow MADR template (xem `documents/02-architecture/adr/_TEMPLATE.md`)
- Content:
  - Context: ADR-025 §5 commit ("EKS vs ECS Fargate evaluation as Phase 1 BETA closure trigger")
  - Decision drivers: Free Tier compatibility, ops overhead, Phase 1 BETA traffic scale (~5-10 beta tenants), Phase 1.5 PAID scale, team expertise
  - Considered options: ECS Fargate / EKS / EC2 + Docker Compose
  - Decision outcome: **ECS Fargate** (recommend per Free Tier compat + ops simplicity + existing Terraform module shape)
  - Consequences: positive (simpler ops, Free Tier eligible) + negative (vendor lock-in AWS, limited k8s ecosystem)
  - Status: ACCEPTED (with user review at closure)
- Tests:
  - Read `documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md` §5 verbatim
  - Read `documents/02-architecture/adr/_TEMPLATE.md` MADR pattern
  - Read `infrastructure/terraform-aws/` top-level để xác nhận module shape (ECS-aligned hay EKS-aligned)
- Acceptance (closes GAP-464):
  - [ ] ADR-028 file created với MADR frontmatter
  - [ ] Context cites ADR-025 §5 commitment
  - [ ] All decision drivers explicitly evaluated
  - [ ] 3 considered options (ECS Fargate / EKS / EC2+Compose) with trade-offs
  - [ ] Decision = ECS Fargate (per existing Terraform module shape)
  - [ ] Consequences section: positive + negative
  - [ ] Status: ACCEPTED 2026-05-11
  - [ ] ADR-025 §5 cross-link updated to reference ADR-028 (closes commitment)
  - [ ] GAP-464 Status flip 🔵 OPEN → 🟢 DONE

---

## 7. Closure Protocol

After all 3 buckets merge:
1. Wave plan status: draft → complete
2. wave-history.jsonl append Wave 58 entry
3. ROADMAP §Next Action signpost update
4. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`
5. GAP-467 final 🟢 DONE flip nếu Bucket A AC all checked (last deferred AC closed)
6. GAP-463 + GAP-464 flip 🟢 DONE
