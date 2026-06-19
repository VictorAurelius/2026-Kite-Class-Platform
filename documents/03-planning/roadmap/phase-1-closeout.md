# Phase 1 BETA Close-Out — campaign tracker (lean)

**Created:** 2026-06-19 · **Mode:** gate-driven, fast/lean, docs xúc tích (no per-gap brainstorm/TDD ceremony; batch walks per `feature-ship-runtime-walk-mandate` §3.4).
**Goal:** đạt cổng Phase 1→2 (Quality audit ≥80 + 5 beta tenant live + 0 P0 incident 2 tuần) nhanh nhất — KHÔNG fix cả 298 gap.

## Landscape (sau reclassification wave-gap-audit-p1-1, #2502)
- 298 active phase-1-beta. **Gate-critical = 126** (26 P0 + 100 P1-launch-critical). 75 P1 non-critical + 88 P2 + 9 P3 → ride/defer.
- Gate-critical chia: **97 LOCAL-closable** (no cost) · **20 AWS-gated** · **9 vendor-gated**.

## 3 tracks
| Track | Scope | Trạng thái |
|---|---|---|
| 🟢 LOCAL | 97 (code/local-walk/docs) | **ACTIVE** — không tốn tiền |
| 🔴 AWS-gated | 20 (terraform/EC2/RDS/IAM/SES/deploy/restore + "5 beta tenant live" gate cond) | BLOCKED — chờ quyết restore AWS (cost) |
| 🟡 Vendor-gated | 9 (Resend/SePay/Zalo sandbox) | BLOCKED — chờ sandbox/tunnel |

> **Sự thật:** cổng "5 beta tenant live" + 20 P0/P1 ops **bắt buộc AWS stack** (đã teardown). Không thể *đóng thật* Phase 1 mà không redeploy AWS.

## Wave 1 — 5 P0 local-walk (ACTIVE)
Code đã ship 80-95%, cần runtime walk verify để flip DONE (per `feature-ship-runtime-walk-mandate` + `g1-browser-walk-before-flip`):
- GAP-1066 — attendance status normalize (V87 chk uppercase)
- GAP-1115 — LMS paywall bypass (getCourseStructureForStudent)
- GAP-1139 — KC OWNER tenant-admin 403
- GAP-1213 — wizard deploy mock→KC theme propagation
- GAP-1308 — gateway strip X-User-Roles (role-spoof)

Steps: stack up (`up.sh`) → seed (`seed-walk-tenant.sh`) → pre-walk persona sim (done) → browser walk (catalog-then-batch) → flip DONE.

## Wave 2+ (queued, LOCAL)
- 2 P0 doc-umbrella: GAP-049 (business-logic correctness), GAP-154 (22 BRD docs).
- 89 P1-launch-critical (code agents + walks), batched by domain.

## Blocked-track notes
- GAP-502 (kh_backend prod thrashing) → AWS-gated (moved out of Wave 1).
- AWS/vendor tracks resume on redeploy decision.

## Restructuring executed (2026-06-19) — phase-4-deploy split
- **phase-4-deploy created** (sau phase-3): 40 gap moved (16 P0 + 16 P1 + 8 P2). Classify agent confirmed deploy-gated (31) + hybrid-move (9); 26 candidate giữ lại phase-1-beta (21 local-doable + 5 hybrid-keep — substantive local code còn lại; split AWS-child sau).
- **unclassified/ cleaned**: 16 n/a → phase (2 active P0 → phase-1-beta; 11 DONE → closed; 3 WONTFIX → phase-1-beta/phase-2). n/a = 0.
- **Phase 1 P0 burden: 26 → 10** (local-closable). Gate redefined (CLAUDE.md): "5 beta tenants live" → phase-4-deploy.
- Infra: 2 validators + gap-architecture enum v1.0.6 + folder + README.
- **Phase 1 active now: 258** (was 298). Còn 10 P0 + P1-critical local → đẩy nốt để đạt gate redefined.

## Wave-pack sequencing outline (2026-06-19, context-warm draft — LEAN)

**Scope:** 119 gate-critical local gaps (phase-1-beta active: 9 P0 + 110 P1-launch-critical). Readiness: 72 CODE (agent-spawnable) + 47 WALK/verify (G2★ human browser). Full per-wave 8-section plan drafted just-in-time với state-check tươi (per audit-to-gap-pipeline §2.6.1). Đây CHỈ là bản đồ thứ tự, không phải plan đầy đủ.

### Two parallel tracks
- **Code track** (72 gaps OPEN + PARTIAL<70%): agent-spawnable fix waves (Opus, worktree), batch per domain.
- **G2★ walk track** (47 gaps PARTIAL≥70%, code shipped): human browser walk-batch (stack-up + seed), grouped by flow. Tôi làm G1 (BE/API/DB verify); human làm G2★.

### Wave sequence (P0-first → user-trust → residual)

| Wave | Domain | #gaps | P0 | Readiness profile | Lead gaps | Depends |
|---|---|---:|---:|---|---|---|
| **close-1** | P0 walk batch (Wave 1) | 5 | 5 | 4 G1✅ + 1 agent | 1308 DONE · 1066/1139/1213 G1 · 1115 agent | — (in progress) |
| **close-2** | SECURITY (isolation/RLS/IDOR/auth/spoof) | 29 | 2 | code-fresh + walk mix | **GAP-1413** nil-UUID tenant-resolver (P0) · 1130 IDOR · 1414 EmailClient URLs · 1428 attendance leak | — (highest: security gates launch) |
| **close-3** | FE mock-data + onboarding | 15 | 1 | mostly code-fresh | **GAP-286** OTP signup (P0) · 1410/1411/1412/1430 fabricated-data-to-real-users · 955 provisioning-abandon | close-2 (auth) |
| **close-4** | BRANDING wizard | 17 | 1 | mostly G2★ walk | 1213 (propagation✅, G2★) · 1021/1082/1105/1107/1108/1112/1146 walk-batch | stack-up |
| **close-5** | PAYMENT-local (non-PSP) | 10 | 0 | code + walk | 943/896 VietQR config · 1431 RecordPayment · 1004/1005 clamp/authz | — |
| **close-6** | BE-OTHER (subscription/RBAC/email/attendance/grade) | 44 | 1 | code + walk mix | 063 Zalo-stub(P0) · 1119 RBAC-shell · 1166/1167 attendance-contract · split 6a/6b | — (largest; sub-split) |
| **close-7** | BRD-DOC | 4 | 2 | doc | **GAP-049** business-correctness · **GAP-154** 22 BRD docs · 678/685 audit obligations | — (doc, parallelizable) |

### Execution notes
- **P0-first:** close-2 (GAP-1413 tenant-isolation) + close-3 (GAP-286 OTP) + close-7 (049/154 BRD) hold the remaining 9 - (5 in close-1) = 4 local P0. Clear these → Phase 1 P0 ≈ 0.
- **G2★ walk-marathon:** 47 WALK gaps batch into 1-2 human browser sessions (stack-up + seed-walk-tenant) rather than per-domain — coordinator pre-walk-sim per `pre-walk-persona-simulation-mandate`, human walks, coordinator flips DONE.
- **Agent code waves:** close-2/3/5/6 code portions → Opus parallel agents (worktree-isolated, max 5/wave per `feedback_parallel_agent_strategy`), each gap = state-check residual first (§2.6.1) before fix.
- **Defer (not gate-critical):** 75 P1-non-critical + 88 P2 + 9 P3 phase-1-beta → ride post-gate or wave-fix-later.
- **Phase-4-deploy (48):** separate redeploy-gated track, resume on AWS restore decision.
