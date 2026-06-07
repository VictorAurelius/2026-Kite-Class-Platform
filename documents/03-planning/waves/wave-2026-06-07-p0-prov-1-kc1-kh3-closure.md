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
status: complete
gaps: [GAP-942, GAP-945, GAP-946, GAP-948, GAP-949, GAP-1053, GAP-1054, GAP-1055]
---

# Wave p0-prov-1 — KC-1 provisioning + KH-3 subscription P0 closure

**Goal:** Đóng 5 P0 flow-walk blocker cuối còn chặn G2 human-walk trên local stack (KC-1 provisioning GAP-945/946/948/949 + KH-3 GAP-942) qua 2 code-prep nhỏ (GAP-1053 template + GAP-1054 IT) + 2 coordinator-driven walk. Không gap nào cần SePay key / AWS. Phase 1 BETA P0 28 → 23.

## 1. Brainstorm

**Trigger:** User directive 2026-06-07 "tối ưu wave và làm cả 2" sau thống kê P0-block-G2 — 5 P0 local-walkable còn lại ([[project_p0_priority_over_g2]] + flow-verification-campaign §4 KC-1/KH-3 rows).

**Q1 alignment:** Phục vụ Flow Verification Campaign §4. Personas: Owner (KH-3 subscription, KC-1 tenant), Platform Admin.

**Outside-in skip:** Per `outside-in-coverage-trigger.md` §4 exception — gap fix cụ thể đã có root cause (mỗi gap có pre-walk persona-sim 2026-06-04 + documented failure modes). Đây là **post-fix re-walk closure** (`feature-ship-runtime-walk-mandate.md` §3 + `pre-handoff-self-test-completeness.md` §3), không phải fresh flow walk → existing pre-walk artifacts đủ.

**Tối ưu — walk-convergent, KHÔNG fan-out:** 5 P0 hội tụ về **2 luồng walk trên 1 stack chung** + overlap nặng module (kitehub-subscription touched by 942/945/948/949). Fan-out 5 agent → collide (cùng flow, cùng DB/MailHog/RabbitMQ state, cùng module branch). → 2-phase: prep song song (module rời nhau) + coordinator-driven sequential walk.

## 2. Task Breakdown

**Phase 1 — Code-prep song song (2 agent Opus, module rời nhau):**
- Bucket A → GAP-1053 (`kitehub-email`): tenant-ready Thymeleaf template (HTML+txt) + graceful `renderHtmlWithFallback`.
- Bucket B → GAP-1054 (`kitehub-subscription` test): seed parent `instances` row trong `SubscriptionPendingNullableColumnsIT` FK-fix.

**Phase 2 — Coordinator-driven walk (tuần tự, shared stack, catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4):**
1. Merge prep → rebuild kitehub-email.
2. KH-3 walk: POST `/api/platform/subscriptions` BASIC → 201 + PENDING → GAP-942.
3. KC-1 walk: publish `tenant.deployed` → MailHog render + audit row + Instance.status → GAP-945/946/948/949.
4. Catalog-then-batch walk-surfaced bug → rebuild → re-walk.

## 3. Scope

| Bucket | Gap | Module | Scope |
|---|---|---|---|
| A | GAP-1053 | `kitehub-email` | tenant-ready template + graceful fallback — unblock GAP-948 AC#2 |
| B | GAP-1054 | `kitehub-subscription` (test) | seed parent instances FK-fix — unblock GAP-942 regression guard |
| Walk | GAP-942 | `kitehub-subscription` | POST /subscriptions PENDING live verify |
| Walk | GAP-945/946/948/949 | `kitehub-subscription` + `kiteclass-core` | KC-1 provisioning chain live verify |

**Scope boundary (chốt — không re-litigate):** GAP-946 `provisionInfrastructure` real-impl (DNS/MinIO/DB-schema) DEFER Phase 1.5 → GAP-1055. Gap tự ghi "Saga stub acceptable Phase 1 BETA (tenant DB do subscription-side `provisionDatabase` provision thật)".

## 4. State-Check Evidence

Per `audit-to-gap-pipeline.md` §2.6 — đọc 7 gap file + stack state trước khi plan:
- Stack 13/13 services UP healthy (gateway :9000, postgres, rabbitmq, mailhog, kiteclass-core, kitehub-subscription, kitehub-email).
- GAP-942: V62 + PENDING semantics đã ship PR #2157 (AC#1 verified live trước wave này); residual = runtime POST walk + IT.
- GAP-945: saga wiring shipped + live re-confirmed Wave provisioning-1 (kiteclass-core FrontendInstance DEPLOYED).
- GAP-946: defensive `assertDatabaseProvisioned` shipped Wave p0-1; real-impl stub remains.
- GAP-948: wiring G3-verified Wave p0-1; MailHog residual = template render error (GAP-1053).
- GAP-949: subscription-side audit wired Wave provisioning-1; live verify + FK-timing risk pending.
- GAP-1053/1054: OPEN, code-prep targets.

## 5. Verification Gates

Live walk trên local prod-parity stack 2026-06-07 (catalog-then-batch — 0 bug lòi ra):
- **GAP-942:** POST `/api/platform/subscriptions` BASIC (gateway :9000) → HTTP 201 + `status=PENDING, startedAt=null, expiresAt=null, pendingPaymentId` set (trước 409); IT 2/2 green.
- **GAP-948/1053:** publish `tenant.deployed` (nhi-hoathcs) → `template: tenant-ready` → `[SMTP] Email sent` → MailHog 25→26 rendered.
- **GAP-945:** `markProvisioned` PENDING→TRIAL wired + executes (TRIAL no-op đúng).
- **GAP-949:** `admin_audit_log` 3 `TENANT_PROVISIONED` rows — FK-timing bug không materialize.
- **GAP-946:** 0/9 instances half-provisioned (no `database_url` null/pending).
- **GAP-1054:** module BUILD SUCCESS 879 tests.

## 6. Agent Spawn Pattern

- 2 prep agent Opus song song (worktree-isolated, module rời nhau) — Bucket A + B per `agent-model-opus-default.md`.
- Coordinator drive 2 walk tuần tự (không delegate — shared-stack walk không parallelize được).
- 1 closure agent Opus (governance paperwork: gap flips + CSV + campaign + GAP-1055).

## 7. Closure Protocol

- 7 gap → 🟢 DONE + git-mv `phase-1-beta/closed/` per `gap-folder-organization.md`.
- gap-status.csv 7 rows DONE + GAP-1055 row (per `meta-csv-index-pattern.md`).
- GAP-1055 filed (P2, phase-1.5-paid): `provisionInfrastructure` real-impl scope-split từ GAP-946.
- flow-verification-campaign §4 KC-1 + KH-3 rows synced.
- CI guards: `check-gap-folder-location.sh` + `check-gap-status-csv.sh` PASS.

## 8. Log

- **2026-06-07:** Wave tạo + executed + closed cùng session. Walk-convergent structure (2 prep agent + coordinator walks). 5 P0 + 2 P1/P2 DONE; GAP-1055 follow-up (provisionInfrastructure Phase 1.5). 0 walk-surfaced bug. Phase 1 BETA P0 28 → 23.
