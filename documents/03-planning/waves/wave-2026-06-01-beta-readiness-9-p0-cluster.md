---
title: Wave beta-readiness-9 — P0 cluster (security IDOR + staff-invite + code-splitting + VN-seed)
status: planning
created: 2026-06-01
waves: [9]
wave: 9
tag_primary: beta-readiness
tags_secondary: [security-idor, staff-invite, code-splitting, vn-seed, phase-1-beta-gate]
counter: 9
date_launch: 2026-06-01
gaps: [GAP-814, GAP-772, GAP-773, GAP-127, GAP-658]
---

# Wave beta-readiness-9 — P0 cluster (4 parallel buckets)

**Trigger:** User direction 2026-06-01 "lập wave tiếp, tối ưu agents" sau khi đóng GAP-727/732. Pick 4 P0 AWS-free, file-disjoint gaps từ Phase 1 BETA backlog → 4 parallel Opus background worktree agents.

**Goal:** Đóng/đẩy 4 P0 blocker AWS-free song song: cross-tenant IDOR (gateway header-strip) + KC staff-invite full-stack + FE code-splitting + VN sample seed. Live AWS verify defer post-restore (GAP-612).

## 1. Brainstorm

**Inside-out (3-source pull per `inside-out-completeness-trigger.md`):**
- ROADMAP §🎯 / gap-status.csv P0 phase-1-beta non-DONE: 32 active → lọc AWS-free + file-disjoint.
- Candidates: GAP-814 (security), GAP-772/773 (staff-invite chain), GAP-127 (perf), GAP-658 (VN seed).
- User AskUserQuestion 2026-06-01: chốt **4 bucket** + Bucket A **chỉ P0 header-strip** (defer JWT-sig-verify + network-isolation).

**Outside-in:** Skipped per `outside-in-coverage-trigger.md` §4 — (a) các gap đã qua brainstorm phase (execution, không phải scope mới); (b) recent audit ≤30 ngày (Wave 100 3-agent 2026-05-19 + Wave 105 persona walk). Security/perf/seed = internal scope; staff-invite covered by prior persona audits.

## 2. Task Breakdown

- **4 parallel agents**, Opus 4.7 (per `agent-model-opus-default.md`), `run_in_background: true` (per `agent-background-spawn-default.md`), worktree isolation (per `feedback_parallel_agent_strategy.md` — max 5 concurrent).
- **Cross-layer Bucket B** (GAP-772 BE + GAP-773 FE): single full-stack bucket — 1 agent owns the staff-invite API contract end-to-end (per `contract-first-for-cross-layer.md` — contract defined within bucket, no separate Bucket 0 needed since one agent ships both sides cohesively).
- **AWS-gated live verify** deferred all buckets (stack stopped) — `FEATURE_SHIP_WALK_DEFER` per `feature-ship-runtime-walk-mandate.md` §5; gated GAP-612.
- **Merge order** (minimize conflict on shared projects): A (kitehub-gateway, isolated) → D (kiteclass-core seeder) → B (kiteclass-core controller + FE route) → C (FE next.config + components). Worktree + sequential squash-merge.

## 3. Scope

| Bucket | Gap | Scope | Files (disjoint) | Effort | AWS? |
|---|---|---|---|---|---|
| **A** | GAP-814 | Gateway `RemoveRequestHeader=X-Tenant-Id, X-User-Id` global `default-filters` (strip-then-set) + audit route coverage (every tenant-scoped route has TenantResolver OR path-UUID). State-check existing `TenantHeaderGuardFilter` first. Defer JWT-sig-verify + core network-isolation → follow-up gap. | `kitehub/kitehub-gateway/**` | ~1.5h | ❌ |
| **B** | GAP-772 + GAP-773 | Full-stack staff-invite: BE `StaffInvitationController` + service + DTO + endpoint (`/api/v1/staff-invitations` issue + list + accept) in kiteclass-core; FE `/staff/accept-invite/[token]` route in `(auth)` group (mirror existing `parent-invite` pattern). | `kiteclass/kiteclass-core/**` (new staff module) + `kiteclass/kiteclass-frontend/src/app/(auth)/staff-accept-invite/**` | ~2h | ❌ |
| **C** | GAP-127 | FE code-splitting: `next.config.js` both frontends (`@next/bundle-analyzer` + `experimental.optimizePackageImports` + `images.formats`) + `next/dynamic` lazy-wrap heavy deps (framer-motion/recharts/react-table) below-the-fold. | `kiteclass/kiteclass-frontend/next.config.js` + `kitehub/kitehub-frontend/next.config.js` + lazy-wrapped `*.tsx` | ~1.5h | ❌ |
| **D** | GAP-658 | VN sample seed worker: replace English placeholder (`John Doe`/`Class A1`) với VN-friendly (`Trần Thị Hương`/`Lớp 5A1`) in `BrandingDataSeeder` + production sample-data path. State-check dev-only vs production seed scope first. | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/dev/seeder/**` + sample-data service | ~1h | ❌ |

**Parallelizable:** ✅ all 4 — worktree isolation + sequential merge. Shared-project overlaps (B+D in kiteclass-core; B+C in kiteclass-frontend) are disjoint files (new modules vs config/components); sequential squash-merge resolves any residual.

**Out-of-scope (defer):**
- GAP-814 JWT signature-verify (HMAC/JWKS) + core network-isolation/shared-secret → follow-up gap (architectural, bigger).
- All live browser/AWS verify → GAP-612 unblock.
- GAP-823 META instances-triad (needs ADR ownership decision) → separate META wave.

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol / file | Bucket | Command | Verdict |
|---|---|---|---|
| `kitehub/kitehub-gateway/src/main/resources/application.yml` | A | `ls` | ✅ exists |
| `TenantResolverGatewayFilterFactory` + `TenantHeaderGuardFilter` (existing partial guard) | A | `grep -rln` | ✅ exists (agent must reconcile existing guard) |
| `RemoveRequestHeader` in gateway yml | A | `grep -rln RemoveRequestHeader kitehub --include=*.yml` | 🆕 to-be-added (Bucket A owns) |
| `StaffInvitationController` in kiteclass-core | B | `grep -rln staff-invitations --include=*Controller.java` | 🆕 to-be-created (Bucket B owns) |
| `(auth)` FE group (login/register/parent-invite) | B | `ls kiteclass-frontend/src/app/(auth)/` | ✅ exists (mirror parent-invite) |
| `kiteclass-frontend/next.config.js` + `kitehub-frontend/next.config.js` | C | `ls` | ✅ exists (both 12-line baseline) |
| `framer-motion`/`recharts` heavy deps | C | `grep -lE` package.json | ✅ present (kitehub-frontend confirmed; agent verifies kiteclass) |
| `BrandingDataSeeder.java` | D | `find` | ✅ exists (`dev/seeder/`; agent verifies production seed path) |

## 5. Verification Gates

Per-bucket acceptance (wave-level):
- [ ] **A** GAP-814: gateway strips client `X-Tenant-Id`/`X-User-Id` on all routes (strip-then-set); route-coverage audit documented; existing `TenantHeaderGuardFilter` reconciled; gateway tests PASS (`./mvnw -pl kitehub-gateway test`).
- [ ] **B** GAP-772/773: `StaffInvitationController` issue/list/accept endpoints + FE accept-invite route; BE tests PASS (`./mvnw test` affected); FE `pnpm --filter kiteclass-frontend build` clean (per `fe-build-local-verify.md`).
- [ ] **C** GAP-127: bundle analyzer wired both frontends + heavy deps lazy-loaded; `pnpm build` clean both frontends.
- [ ] **D** GAP-658: VN sample names in seed; zero `John Doe`/`Class A1` grep in seed scope; BE tests PASS.
- [ ] Live verify (all buckets) → **deferred GAP-612 AWS restore** (`FEATURE_SHIP_WALK_DEFER`).
- Each agent runs local verify before declaring done (BE: `./mvnw test` affected; FE: `pnpm build`). API-contract change caller-sweep per `api-contract-change-caller-sweep.md` if method signatures touched.

## 6. Agent Spawn Pattern

4 agents, `model: "opus"`, `run_in_background: true`, `isolation: "worktree"`. Each agent prompt: RELATIVE paths only (per `feedback_worktree_absolute_path_contamination.md`); commit on own branch; coordinator merges sequentially per §2 order. Spawn all 4 in a single message (true parallelism). Coordinator stays responsive; merges on completion notifications.

## 7. Closure Protocol
- Run audit suite if file-pattern triggers (security for A, UI for C) per `post-wave-audit-mandate.md` §2.4 domain-milestone (or defer with trailer).
- Scope-Completeness Reconciliation table (per `wave-closure-scope-completeness.md` §3) in closure PR.
- 4-target sync (gap-status.csv + ROADMAP §🎯 + wave-history.jsonl append counter=9 + MEMORY if new entry) per `post-merge-sync-completeness.md`.
- `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`.
- Flip frontmatter `status: planning → complete`.

## 8. Log
- **2026-06-01 (planning):** Plan drafted + State-Check Evidence verified (gateway = `kitehub/kitehub-gateway` located repo-wide; staff-invite controller absent → to-be-created; next.config both exist; BrandingDataSeeder exists). User chose 4-bucket scope + Bucket A P0-header-strip-only via AskUserQuestion. Plan PR merges before agent spawn per `feedback_wave_plan_through_pr.md`.
