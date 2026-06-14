---
title: Wave P0-Closeout 1 — autonomous-fixable Phase 1 BETA P0 cluster
status: in-progress
created: 2026-06-14
updated: 2026-06-14
tag_primary: p0-closeout
tags_secondary: [lms, authz, attendance, sso, security]
waves: [p0-closeout-1]
---

# Wave P0-Closeout 1 — autonomous-fixable Phase 1 BETA P0 cluster

Đóng cụm P0 **tự-sửa-được** (code-only, không phụ thuộc AWS/deploy/người) còn tồn đọng sau wave kitehub-biz-100 + SSO closeout. Ưu tiên security + business-correctness theo `meta-gap-priority.md` (Business-Logic-P0 dẫn trước Feature-P0).

## 1. Brainstorm

**Bối cảnh:** Sau khi đóng kitehub-biz-100 (subscription lifecycle) + SSO (GAP-1138/1305/1306), rà `gap-status.csv`: Phase 1 BETA có **14 P0 OPEN + 31 P0 PARTIAL**. Phần lớn P0 PARTIAL ở % cao là **AWS/human-gated** (phụ thuộc GAP-612 AWS restore + deploy thật) → KHÔNG đóng được autonomous trong session local.

**Phân loại P0 theo khả năng tự-đóng:**
- **Gated (loại khỏi wave):** GAP-502/533/566/567/572/608/117/756/380 (ops deploy), 063/286 (Zalo/SMS vendor), 530/793 (email live verify), 648 (thesis NFR live), **952** (CloudWatch live-apply, code IaC đã shipped), **610** (code đã fix local, chờ production deploy verify), 975/976 (payment — có thể cần live webhook verify).
- **Autonomous code-only (vào wave):** GAP-1115/1116 (LMS paywall), 1139 (KC owner authz), 1066 (attendance normalize), + GAP-1306 (SSO determinism — đã in-flight từ SSO closeout, fold vào wave).

**Risks / edge cases:**
- A/B/C đều `kiteclass-core` → rủi ro chạm file chung (SecurityConfig, Flyway version). Mitigation: chỉ C đụng migration; A=service-LMS, B=authz-controllers → disjoint file.
- Một số AC yêu cầu **human G2 browser walk** (GAP-1139 re-walk; GAP-1115/1116 mutation walk) → agent fix code + test, để AC walk unchecked + giữ PARTIAL (không flip DONE giả) per `pre-handoff-self-test-completeness.md` + `feature-ship-runtime-walk-mandate.md`.
- Rate-limit (bài học wave biz-100 Đợt 1): giữ **≤2 concurrent agent**.

## 2. Task Breakdown

| Bucket | Gaps | Effort | Mô tả |
|---|---|---|---|
| 0 (in-flight) | GAP-1306 | M | SSO determinism — repo-level `ORDER BY` + cross-flow sweep 6 site + test |
| A | GAP-1115 + GAP-1116 | M | LMS paywall — enforce enrollment-gate trên read (`getCourseStructureForStudent`) + write (`completeLesson`) paths; shared guard helper + sweep sibling LMS endpoints |
| B | GAP-1139 | S-M | KC OWNER không được nhận tenant-admin → 403 reports/enrollment; sửa role→authority mapping / `@PreAuthorize` hasRole('ADMIN') gồm OWNER |
| C | GAP-1066 | S | Normalize 450 row attendance status lowercase → UPPERCASE trước/cùng V87 chk constraint; migration idempotent (V99 nếu cần) + boot healthy |

## 3. Scope

**In scope:** GAP-1306 (Bucket 0), GAP-1115+1116 (A), GAP-1139 (B), GAP-1066 (C). Code + test + gap-doc closure. Mỗi bucket = 1 PR riêng base `main` (disjoint branch), human duyệt merge.

**Out of scope:** mọi P0 AWS/deploy/vendor-gated (xem §1 danh sách gated) — chờ GAP-612 AWS restore + production deploy. Branding-100 cluster (GAP-1213/1214/1215/1216/1217…) — bỏ qua theo chỉ thị dev. Human G2★ browser walks — việc của dev.

**Outside-in audit:** N/A cho wave này per `outside-in-coverage-trigger.md` §4 — đây là **gap-fix cụ thể đã có root cause** (không phải inside-out feature brainstorm). Scope = đóng gap đã filed + triaged, không discover net-new feature.

## 4. State-Check Evidence

- `gap-status.csv` trên `origin/main` (cd89a3ddc) rà 2026-06-14: P0 phase-1-beta = 14 OPEN + 31 PARTIAL.
- Per-gap scope extract xác nhận disjoint module: GAP-1115/1116 = `kiteclass-core` LMS service; GAP-1139 = `kiteclass-core` authz (report/enrollment controllers + `hasRole('ADMIN')`); GAP-1066 = `kiteclass-core` Flyway (max version trên main = **V98** → C reserve V99); GAP-1306 = `kitehub-subscription` AuthService/InstanceRepository (repo khác).
- Gated gaps xác nhận qua gap file: GAP-952 "live-apply deferred … GAP-612"; GAP-610 "Production deploy verify ⏳ defer (AWS … GAP-612)".

## 5. Verification Gates

- Mỗi bucket: local `./mvnw verify -P strict-warnings` (module liên quan) PASS trước push.
- CI là cổng canon (bài học #2381): chờ toàn bộ check xanh + `mergeStateStatus: CLEAN` trước khi đề xuất merge.
- Test bắt buộc per bucket chứng minh AC (paywall test / authz 200-not-403 / migration boot-healthy / determinism stable).
- Gap DONE chỉ flip khi AC verified; AC yêu cầu human walk → giữ PARTIAL.

## 6. Agent Spawn Pattern

- **Model:** Opus 4.x mọi agent (per `agent-model-opus-default.md`).
- **Isolation:** worktree sibling off `origin/main` (`kite-wt-p0-{a,b,c}` + `kite-wt-gap1306`).
- **Concurrency ≤2** (rate-limit né): Đợt 1 = GAP-1306 (in-flight) + Bucket A. Đợt 2 = Bucket B khi 1 slot free. Đợt 3 = Bucket C.
- **Inline-hybrid** (`agent-concurrency-budget-inline-hybrid.md`): coordinator viết wave-plan doc này inline song song 2 agent.
- Mỗi agent: fix + sweep (cross-flow) + test + flip gap + PR base main (KHÔNG merge — human authorizes).

## 7. Closure Protocol

- Wave `status: complete` khi: cả 4 bucket PR merged (human-authorized) + mọi gap thuộc wave flip DONE (hoặc PARTIAL có lý do human-walk-gated ghi rõ) + CSV/ROADMAP/wave-history sync.
- Per `wave-closure-scope-completeness.md`: reconcile bucket↔gap table; gap còn PARTIAL do human-walk → liệt kê rõ trong closure note, không tính là "incomplete wave".
- Post-wave audit cadence: filed follow-up audit gap nếu wave touch business-logic/security surface (≤3 ngày per `post-wave-audit-mandate.md`).

## 8. Log

- **2026-06-14:** Wave tạo. Membership = autonomous-fixable P0 cluster (1306/1115/1116/1139/1066) tách từ Phase 1 BETA P0 backlog sau rà `gap-status.csv`. Bucket 0 (GAP-1306) đã in-flight từ SSO closeout. Đợt 1 spawn Bucket A song song (concurrency ≤2). Outside-in N/A (gap-fix có root cause). Gated P0s (AWS/deploy/vendor — GAP-612 dependency) loại khỏi scope, chờ AWS restore. Plan ship qua PR docs-only per `feedback_wave_plan_through_pr.md`.
