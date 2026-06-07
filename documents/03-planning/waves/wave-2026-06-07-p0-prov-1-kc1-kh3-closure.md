---
title: Wave p0-prov-1 — KC-1 provisioning + KH-3 subscription P0 closure (walk-convergent)
wave: 1
tag_primary: p0-prov
tags_secondary: [flow-campaign, p0, beta-prep, walk-convergent]
counter: 1
date_launch: 2026-06-07
created: 2026-06-07
updated: 2026-06-07
waves: [p0-prov-1]
status: draft
gaps: [GAP-942, GAP-945, GAP-946, GAP-948, GAP-949, GAP-1053, GAP-1054]
---

# Wave p0-prov-1 — KC-1 provisioning + KH-3 subscription P0 closure

**Goal:** Đóng 5 P0 flow-walk blocker cuối còn chặn G2 human-walk trên local stack (KC-1 provisioning cluster GAP-945/946/948/949 + KH-3 subscription GAP-942), qua 2 code-prep nhỏ (GAP-1053 template + GAP-1054 IT) + 2 coordinator-driven walk. Không gap nào cần SePay key / AWS.

**Trigger:** User directive 2026-06-07 "tối ưu wave và làm cả 2" sau thống kê P0-block-G2 — 5 P0 local-walkable còn lại (xem [[project_p0_priority_over_g2]] + flow-verification-campaign §4 KC-1/KH-3 rows).

## Q1 (alignment) — outside-in skip rationale

Per `outside-in-coverage-trigger.md` §4 exception: gap fix cụ thể đã có root cause (mỗi gap có pre-walk persona-sim 2026-06-04 + documented failure modes). Đây là **post-fix re-walk closure** (`feature-ship-runtime-walk-mandate.md` §3 + `pre-handoff-self-test-completeness.md` §3), không phải fresh flow walk → existing pre-walk artifacts đủ.

## Tối ưu wave — walk-convergent, KHÔNG fan-out

**Nhận định:** 5 P0 hội tụ về **2 luồng walk trên 1 stack chung** + overlap nặng module (kitehub-subscription touched by 942/945/948/949). Fan-out 5 agent → collide (cùng flow, cùng DB/MailHog/RabbitMQ state, cùng module branch). → Cấu trúc 2-phase:

### Phase 1 — Code-prep song song (2 agent Opus, module rời nhau)

| Bucket | Gap | Module | Scope |
|---|---|---|---|
| A | GAP-1053 | `kitehub-email` | tenant-ready Thymeleaf template (HTML+txt) + graceful `renderHtmlWithFallback` — unblock GAP-948 AC#2 |
| B | GAP-1054 | `kitehub-subscription` (test) | seed parent `instances` row trong `SubscriptionPendingNullableColumnsIT` FK-fix — unblock GAP-942 regression guard |

### Phase 2 — Coordinator-driven walk (tuần tự, shared stack, catalog-then-batch per §3.4)

1. Merge prep → rebuild `kitehub-email` + `kitehub-subscription`.
2. **KH-3 walk** ([recipe](../../05-guides/operations/2026-06-04-g2-recipe-kh3-subscription.md)): POST `/api/platform/subscriptions` BASIC (Owner FREE/TRIAL) → 201 + PENDING persist + payment row + confirmPayment activation → **GAP-942 DONE**.
3. **KC-1 walk** ([recipe](../../05-guides/operations/2026-06-05-g2-recipe-kc1-tenant-settings.md)): beta signup E2E → tenant provisioned → MailHog tenant-ready render (GAP-948 AC#2) + `admin_audit_log` TENANT_PROVISIONED row (GAP-949) + Instance.status decision (GAP-945 AC#3) → **GAP-945/946/948/949 DONE**.
4. Catalog-then-batch walk-surfaced bug (e.g. GAP-949 FK-timing AFTER_COMMIT, GAP-945 status-callback) → single rebuild → re-walk.

## Scope boundary (chốt — không re-litigate)

- **GAP-946 `provisionInfrastructure` real-impl (DNS/MinIO/DB-schema) DEFER Phase 1.5** — gap tự ghi "Saga stub acceptable Phase 1 BETA (tenant DB do subscription-side `provisionDatabase` provision thật)". Phase 1 BETA closure = defensive hardening (đã ship) + walk confirm 0 `pending` rows. Real-impl → file follow-up gap Phase 1.5.

## Expected outcome

- 5 P0 phase-1-beta → DONE (Phase 1 BETA P0 28 → 23) + 2 P1/P2 (GAP-1053/1054) → DONE.
- 1 follow-up gap filed: `provisionInfrastructure` real-impl (Phase 1.5).
- KC-1 + KH-3 flow rows → G2-ready (no OPEN P0 residual).

## Log

- **2026-06-07:** Wave tạo. Walk-convergent structure chốt (2 prep agent + coordinator walks). provisionInfrastructure defer Phase 1.5 per gap scope note.
