---
title: G3 Production-Parity Plan — Flow Verification Campaign (G1→G3→G2 reorder)
status: active
created: 2026-06-07
updated: 2026-06-07
supersedes_sequencing: g1-all-first-then-g2 (memory flow_campaign_g1_first_then_g2)
---

# G3 Production-Parity Plan

**Quyết định (user 2026-06-07):** reorder campaign từ **G1→G2→G3** sang **G1→G3→G2**. G3 (production-parity, Claude-drivable) làm TRƯỚC G2 (human test) để: (1) tận dụng Claude thay vì tốn thời gian người trên stack non-parity; (2) bắt nhóm P0 đáng sợ nhất (gateway routing / IDOR / email-security) trước; (3) đảm bảo cái human G2 test = cái chạy production. Tiền lệ: KC-7 + KC-8 đã G3-trước-G2.

## Gate G3 — định nghĩa (campaign §1)
Walk trên production-equivalent: cùng Docker image tag, **Postgres+Flyway+RLS thật (KHÔNG H2)**, **gateway JWT→header auth (qua :9000, không BE-direct curl)**, prod-profile config, env-var đủ. Verify cross-tenant isolation (IDOR defended) + env-var completeness.

## Vấn đề: đa số G1 walk = BE-direct curl → CHƯA chạm gateway+RLS
17/22 flow chưa-G3. Nhiều P0 OPEN chính là G3-surface (production-parity fails).

## Phases

### P0 — Finish KC-1 (đang chạy, gần xong)
- ✅ GAP-1045 (RabbitMQ converter) + GAP-1047 (saga TenantContext) — fixed + walk#3 verified (PR #2226).
- ⬜ Remaining KC-1 sub-walks: GAP-953 retry() path (force FAILED instance → admin retry → verify retry không slug-collision), GAP-954 delete cascade (PDPL), GAP-947 TenantSettings GET/PUT. Sau đó flip GAP-945/946/947/948/952/953/954 DONE.

### P1 — META gateway route-predicate audit (GAP-1042)
1 agent audit TOÀN BỘ gateway route config → map mọi routing collision 1 lần (recurrence #3 systemic per `meta-gap-priority.md`). Output: bảng route → controller, flag collision (vd KC-10 GAP-1034, KC-12 GAP-1041). Fix-class-once thay vì per-flow.

### P2 — P0 security/parity fix wave (parallel Opus agents, wave-eligible)
Cluster G3-blocking P0, disjoint theo service/flow → 4-5 agent song song:
| Cluster | Gaps | Note |
|---|---|---|
| A. Gateway routing collision | GAP-1034 (KC-10), GAP-1041 (KC-12) + P1 META findings | shadow controllers; fix route predicate |
| B. Cross-tenant IDOR | GAP-1015 (KH-5), GAP-1019 (KH-6), GAP-1023 (KH-7), GAP-1025 (KH-8), GAP-1035 (KC-10) | tìm shared-fix: missing `instance_id` predicate / `@PreAuthorize` — recurrence ≥4 |
| C. Email zero-security | GAP-1031 (KH-10) | arbitrary unauthenticated email send (gateway pass-through × email zero-auth) |
Thứ tự ưu tiên: A (systemic, multi-flow) → B (security P0) → C (isolated).

### P3 — G3 re-walk per flow (coordinator-driven, batch)
17 flow chưa-G3: walk qua gateway :9000 (JWT→header thật) + Postgres+RLS + prod-profile + verify cross-tenant isolation + env-var đủ. Per flow: flip campaign §4 status → G3 PASS (chờ G2). Batch theo readiness (sau P2 fix).

### P4 — Hand G2 (human)
Stack G3-verified → bàn cho dev G2 human walk per-flow (việc của dev). Flow ✅ THÔNG khi G2 PASS.

## Thứ tự thực thi
P0 (finish KC-1) → P1 (META audit) → P2 (parallel fix wave) → P3 (G3 re-walk batch) → P4 (hand G2).

## Cross-link
- Campaign: `flow-verification-campaign.md` §1 gates + §4 flow inventory (reorder sequencing note added)
- `meta-gap-priority.md` §3 — systemic P0 (routing/IDOR) ưu tiên
- `local-fix-production-parity-check.md` — G3 parity definition
- `agent-model-opus-default.md` — P2 parallel agents = Opus
