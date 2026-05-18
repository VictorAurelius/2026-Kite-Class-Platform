---
title: Wave 42 — AWS deploy follow-ups (post Phase 2.3)
status: complete
created: 2026-05-08
updated: 2026-05-07
waves: [42]
gaps: [GAP-438, GAP-436, GAP-117]
audit_cluster: release-deploy-artifacts
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 42 — AWS deploy follow-ups (post Phase 2.3)

**Mục tiêu:** Đóng follow-ups còn lại sau Phase 2.3 production apply. 5 buckets parallel disjoint, ~30-45min wall-clock cho longest path. Wave-pack methodology per `feedback_parallel_agent_strategy.md` rule #9 (max 5 agents) + `agent-background-spawn-default.md`.

**Trigger:** User-flagged session 2026-05-08 — "tại sao không tạo wave để spawn còn xử lý song song". Acknowledged miss của serial pattern (PR #989 → #990 → #991 → #992 → #993 → #994 → #995 = 7 serial PRs cho cùng cụm Phase 2.x). Wave 42 = pivot tới wave-pack cho remaining work.

**Wall-clock estimate:** ~45min longest path (Bucket B skill + script most work).

---

## 1. Brainstorm

**Q1 (alignment):**
- Persona: Solo dev tiếp tục Phase 1 BETA prep. Phase 2.3 prod apply DONE. Cần cleanup loose ends + setup Phase 3 image push.
- Domain: 5 mini-domains — DevOps governance (skill+script), Secrets management, GitHub config, Memory, Static-key cleanup.
- Wave: KHÔNG block Phase 3 — Phase 3 user-action sẽ chạy song song với wave này.

**Q2 (trade-offs):**
- 5 vs 6 buckets: chọn **5** vì Phase 2.4 secrets populate runbook đã ship trong PR #994 — chỉ cần helper script (1 ngăn).
- Bucket B (skill + script) = lớn nhất (~45min) → assign deep agent.
- Bucket E (remove static keys) cần verification từ first OIDC trigger trước → defer thực tế tới sau Phase 3 first push, BUT có thể prep PR description & checklist trong Bucket E giờ.

**Q3 (rủi ro):**
- Bucket B + Bucket C cả hai touch `scripts/` folder → nếu cùng tạo file similar name có thể conflict. Mitigate: B = `scripts/smoke-aws-phase-N.sh`; C = `scripts/populate-secrets.sh`. Disjoint.
- Bucket E phụ thuộc Phase 3 first OIDC trigger thành công — nếu Phase 3 chưa run, Bucket E ship PARTIAL.
- Bucket D (set `vars.AWS_CONFIGURED=true`) = effect immediate — sẽ trigger ECR push job trên next push to main. Mitigate: chỉ set sau khi Bucket B + C merge và ECR repos verify exists.

---

## 2. Phân chia công việc

| Ngăn | Gap(s) | Owner | Effort | Disjoint? |
|------|--------|-------|--------|-----------|
| A | **Wave 42 plan PR** (this file) — coordinator-only, no agent spawn | Coordinator | ~10min | ✅ docs only |
| B | **GAP-438 Phase 2** — skill `.claude/skills/devops/aws-smoke-test/SKILL.md` + `scripts/smoke-aws-phase-N.sh` | bg-agent Sonnet | ~45min | ✅ `.claude/skills/devops/aws-smoke-test/` + `scripts/smoke-aws.sh` |
| C | **Phase 2.4 helper** — `scripts/populate-secrets.sh` per runbook đã ship | bg-agent Sonnet | ~30min | ✅ `scripts/populate-secrets.sh` only |
| D | **Phase 3 prep** — set `vars.AWS_CONFIGURED=true` doc instructions + image push runbook + Phase 3 README | bg-agent Sonnet | ~20min | ✅ `documents/05-guides/deploy/phase-3-image-push.md` |
| E | **GAP-436 Phase 4** — checklist for removing static `AWS_ACCESS_KEY_ID`/`SECRET` after first OIDC trigger + GAP flip prep | bg-agent Sonnet | ~15min | ✅ Update GAP-436 file + commit checklist |
| F | **GAP-438 Phase 4** — memory entry `feedback_agent_aws_readonly_logging.md` + MEMORY.md index | bg-agent Sonnet | ~10min | ✅ memory folder only |

Disjoint check: 5 agent buckets touch 5 distinct path scopes. 0 file conflict expected.

**Quyết định:** ship **5 ngăn (B-F)** parallel. A = coordinator (this plan + closure PR).

---

## 3. Phạm vi (compact schema)

**Stake tier:** LOW (no AWS resource creation; all docs/skills/memory/scripts) → model: **Sonnet** cho tất cả 5 ngăn.
**Cross-layer? NO** — không có FE↔BE contract change. Bỏ qua Bucket 0 Foundation.

| # | Ngăn | Gap | Files | Spawn order |
|:-:|------|-----|-------|:-----------:|
| 1 | B | GAP-438 Phase 2 | `.claude/skills/devops/aws-smoke-test/SKILL.md` + `reference/`* + `scripts/smoke-aws-phase-N.sh` | parallel |
| 2 | C | Phase 2.4 helper | `scripts/populate-secrets.sh` + update `documents/05-guides/deploy/secrets-populate-phase-2-4.md` Phase 1 §"Helper script" | parallel |
| 3 | D | Phase 3 prep | `documents/05-guides/deploy/phase-3-image-push.md` (new) + reference từ `release-1-deploy-runbook.md` §3 | parallel |
| 4 | E | GAP-436 Phase 4 | Update `documents/04-quality/gaps/GAP-436-...md` Status header + add Phase 4 checklist | parallel |
| 5 | F | GAP-438 Phase 4 | `~/.claude/projects/.../memory/feedback_agent_aws_readonly_logging.md` + update `MEMORY.md` index | parallel |

---

## 4. State-Check Evidence (theo `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Evidence | Verdict |
|--------|------|----------|---------|
| `.claude/skills/devops/aws-smoke-test/` | skill folder | `ls .claude/skills/devops/aws-smoke-test/` | ❌ 🆕 to-be-created (Bucket B) |
| `scripts/smoke-aws-phase-N.sh` | script | `find scripts -name "smoke-aws*"` | ❌ 🆕 to-be-created (Bucket B) |
| `scripts/populate-secrets.sh` | script | `find scripts -name "populate-secrets*"` | ❌ 🆕 to-be-created (Bucket C) |
| `documents/05-guides/deploy/secrets-populate-phase-2-4.md` | runbook | `ls documents/05-guides/deploy/secrets-populate-phase-2-4.md` | ✅ exists (PR #994 pending merge) |
| `documents/05-guides/deploy/phase-3-image-push.md` | runbook | `ls documents/05-guides/deploy/phase-3-image-push.md` | ❌ 🆕 to-be-created (Bucket D) |
| `documents/04-quality/gaps/GAP-436-oidc-deploy-ecr-restore-roles.md` | gap | `ls documents/04-quality/gaps/GAP-436-*` | ✅ exists |
| `documents/04-quality/gaps/GAP-438-agent-aws-access-workflow.md` | gap | `ls documents/04-quality/gaps/GAP-438-*` | ✅ exists (PR #995 pending merge) |
| `~/.claude/projects/.../memory/feedback_agent_aws_readonly_logging.md` | memory | `ls ~/.claude/projects/.../memory/feedback_agent_aws_readonly_logging.md` | ❌ 🆕 to-be-created (Bucket F) |
| `.claude/rules/agent-aws-access.md` | rule | `ls .claude/rules/agent-aws-access.md` | ✅ exists (PR #995 pending merge) |

Forward-looking: 4 🆕 to-be-created (skill folder + 2 scripts + 1 runbook + 1 memory). All owned by exactly 1 bucket. No symbol referenced as existing while absent.

Banned shortcuts respected (no `| head` truncation; full grep/find).

---

## 5. Cổng kiểm tra (per ngăn)

| Ngăn | Local verify | CI gate |
|------|-------------|---------|
| B | `bash scripts/check-skill-conventions.sh aws-smoke-test` + `shellcheck scripts/smoke-aws-phase-N.sh` + dry-run smoke script | skill-conventions + ShellCheck |
| C | `shellcheck scripts/populate-secrets.sh` + `bash -n scripts/populate-secrets.sh` syntax | ShellCheck |
| D | `markdownlint documents/05-guides/deploy/phase-3-image-push.md` (if installed; else manual) | None |
| E | grep verify GAP-436 file has Phase 4 section + status header updated | Rule frontmatter (gap files do not require frontmatter) |
| F | grep verify memory file has frontmatter + MEMORY.md index has new entry | None (memory files outside repo CI) |

---

## 6. Pattern spawn agent

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 5 ngăn (B-F) cùng spawn `run_in_background: true`
- `isolation: worktree`
- RELATIVE paths theo `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge sequential B → C → D → E → F (alphabetical, không có dependency)
- 1 closure PR aggregate sau khi all 5 merge

---

## 7. Closure protocol

Per chuẩn rules:
- Mỗi ngăn ship 1 PR; flip status DONE per `gap-done-discipline.md` §2 cho gap-touching buckets
- Closure PR aggregate:
  - Wave plan frontmatter `status: complete` + Log entry §10
  - `wave-history.jsonl` append (entry #46)
  - ROADMAP §🚀 Next Action update
  - `prune-merged-worktrees.sh --yes`
- AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts — milestone Phase 1 BETA launch wave (cùng cluster với Wave 41)

---

## 8. Đường găng + thời gian

```
Đợt 1 (5 buckets parallel):    B ∥ C ∥ D ∥ E ∥ F  →  ~45min longest (B skill + script)
Coordinator merge sequential:  B→C→D→E→F          →  ~10min
Closure PR + cleanup:          aggregate          →  ~10min
─────────────────────────────────────────────────────────
Tổng wall-clock:               ~65min cho 1 wave
```

So với serial (5 PRs × ~25min mỗi = ~125min wall-clock + context-switch overhead): **wave-pack saves ~50% time + reduces parent context cost via background spawning.**

---

## 9. Lessons-learned (proactive — apply ở wave này)

Per session retro 2026-05-08:
- ✅ State-check evidence pre-flight (§4 above)
- ✅ Disjoint scope verified per file path
- ✅ AUDIT_DEFER tag noted (continuation of release-deploy-artifacts cluster)
- ✅ Pattern matches Wave 41 (which also had 6 disjoint buckets parallel, ~30min wall-clock)
- ✅ Stake tier LOW → all Sonnet (no Opus over-spend)
- ⚠️ Watch worktree absolute-path contamination (recurrence pattern Wave 41 — 4/6 agents) → agent prompt MUST emphasize relative paths

---

## 10. Log

- **2026-05-07** (closure): SHIPPED 5/5 buckets in ~6min wall-clock (vs ~65min plan estimate, ~11× faster than serial). Wave 42 closure PR includes Tier 1 meta updates from Phase 3 retro (runbook §F5 + ADR-025 addendum + memory `feedback_docker_multiarch_basemage_precheck.md`). PRs merged sequence: #1002 (B aws-smoke-test) → #1001 (C populate-secrets, includes 2-AI-keys trim per user 2026-05-07) → #1000 (D phase-3-image-push runbook) → #999 (E GAP-436 Phase 4 checklist PARTIAL) → #1003 (followup runbook Phase 4.2 sslip.io + 7.3.9 .vn). Bucket F (memory) shipped without PR (outside repo). All bucket-touched gaps flipped per `gap-done-discipline.md`: GAP-438 Phase 2 ✅ DONE; GAP-436 Phase 4 → 🟡 PARTIAL (deferred user-action after Phase 3 first push verified). Phase 3 first OIDC trigger fired immediately post-merge (tag `v0.9.0-beta-staging.1`, run #25527705091): OIDC AssumeRole verified ✅, Build Test 9/9 amd64 pass, but Push to ECR failed for 7 Java services due to multi-arch base-image mismatch. PR #1004 amd64-only fix shipped + tag `v0.9.0-beta-staging.2` (run #25528087813) re-trigger pending verification. Closure protocol per `post-wave-cleanup.md`: 4 worktree husks pruned + 4 wave/42-bucket-* branches deleted. Audit defer trailer: `AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts — milestone Phase 1 BETA launch wave (cluster với Wave 41)`.
- **2026-05-08** (draft): Plan tạo sau user-flagged retro về missed wave-pack opportunity. 5 ngăn parallel covering GAP-438 Phase 2+4, Phase 2.4 helper, Phase 3 prep, GAP-436 Phase 4. Stake tier LOW, all Sonnet bg-agents, ~65min wall-clock estimate.
