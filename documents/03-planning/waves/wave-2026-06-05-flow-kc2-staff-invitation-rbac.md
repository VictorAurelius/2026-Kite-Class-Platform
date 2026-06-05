---
title: Wave flow-kc2 — Staff invitation → accept → RBAC role
status: draft
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc2]
tag_primary: flow
tags_secondary: [kc2, staff-invitation, rbac, kitehub-subscription, campaign]
counter: 2
gaps: [GAP-886, GAP-893, GAP-784]
campaign: flow-verification-campaign
---

# Wave flow-kc2 — Staff invitation → accept → RBAC role

**Goal:** Walk end-to-end flow KC-2 (Owner mời nhân viên → email token → invitee accept + set password → tài khoản STAFF tạo → STAFF login → RBAC enforced) trên stack production-equivalent, đạt **G1 PASS**. Xác minh chuỗi mời-nhận-vai trò hoạt động thật cho mô hình 2-role MVP (OWNER + STAFF).

**Trigger:** KC-2 là **platform-side** (kitehub-subscription) + **decoupled khỏi KC-1** (campaign §3 revised 2026-06-05) — staff invite chỉ cần Owner tồn tại (KH-2c), KHÔNG cần tenant settings configured. → KC-2 KHÔNG block trên KC-1 G2/G3; có thể prep + walk song song. Owner cần mời được staff để vận hành tenant trước khi setup khóa học (KC-3). Flow này từng walk ở Wave meta-6 Bucket A (2026-05-28) — surface **17 bug** → một số đã fix, cần re-walk đầy đủ verify state hiện tại.

## 1. Brainstorm

**Bối cảnh thực tế (state-check 2026-06-05):** KC-2 là flow **platform-side** — implement ở `kitehub-subscription` (BE) + `kitehub-frontend` admin (FE), KHÔNG phải kiteclass-core (dù campaign label "KC-").

**Outside-in audit (per `outside-in-coverage-trigger.md`):** recent audit ≤30 ngày — Wave meta-6 Bucket A walk + 17-bug findings còn relevant. Pre-walk persona sim per `pre-walk-persona-simulation-mandate.md` BẮT BUỘC (invite + email-driven flow) — persona "Owner mời staff" + "invitee nhận email lần đầu".

**3 P1 blocker quanh RBAC id-model (cùng class với bug KC-1 UUID-vs-Long ref-id):**
- **GAP-886** 🔵 OPEN — `user_id/teacher_id` còn `BIGINT/Long` lệch actor UUID model.
- **GAP-893** 🟡 PARTIAL — `users.role` thiếu CHECK constraint + drift `ADMIN` vs `PLATFORM_ADMIN` seed.
- **GAP-784** 🟡 PARTIAL — FE InviteStaffPage role affordance lệch BE 2-role MVP (Owner gán role gì?).

**Phương án:**
- **A** Walk BE-direct (curl chuỗi invite→accept) — nhanh, verify backend logic + RBAC.
- **B** Walk full FE (admin/staff/invite UI → email → accept page) — cover UX nhưng chậm.
- **C** Cả 2 — BE-direct baseline G1 + FE walk baseline G2. Đắt nhưng đầy đủ 3-gate.

→ Chọn C: BE-direct làm G1 baseline; FE walk làm G2 human. Resolve/verify 3 P1 blocker TRƯỚC walk (đặc biệt GAP-784 role affordance ảnh hưởng trực tiếp bước Owner gán role).

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class | Effort |
|---|---|---|---|---|
| 0 (Pre-walk) | Spawn Opus pre-walk persona sim per `pre-walk-persona-simulation-mandate.md` §1 — persona "Owner mời staff" + "invitee accept lần đầu"; return ≥5 failure mode (role drift / email không gửi / token expired / accept tạo user sai role / RBAC không enforce). Save artifact persona-review. | Coordinator | n/a | ~5-10 phút agent + ~30 phút batch-fix |
| A | Verify/resolve 3 P1 blocker (GAP-886 id-model + GAP-893 role CHECK + GAP-784 FE role affordance) — state-check còn present không, scope-revise nếu self-corrected | claude | internal | ~30-60 phút |
| B | Loop walk + catalog: Owner login → invite (POST) → email token (MailHog) → accept (set password) → STAFF login → RBAC check | claude (session-pick) | user-facing flow ✅ pre-walk required | 30-60 phút |
| C | Batch-fix blocker → re-walk → G1 verdict + G2 handoff MD per `g2-handoff-md-mandate.md` | claude | n/a | 15-30 phút |

## 3. Scope

Full §3 scope + bucket expansion happens tại session start khi pick wave này (per stub convention). Skeleton scope:

- **BE (kitehub-subscription):** `StaffInvitationController` @ `/api/v1/staff-invitations` — `POST` invite / `GET` list / `POST /{id}/resend` / `GET /by-token/{token}` / `POST /{token}/accept`. `InvitationTokenService`, V49 audit-log migration.
- **FE (kitehub-frontend admin):** `(admin)/admin/staff/invite/page.tsx` + `(admin)/admin/staff/page.tsx`.
- **RBAC:** mô hình 2-role MVP (OWNER + STAFF) — resolve GAP-784/886/893 role drift.
- **Walk target:** tenant `sky-education` (Owner `owner@skyedu.vn` / `SkyEdu@2026`) — reuse từ KC-1.

## 4. State-Check Evidence

Verified present 2026-06-05 (coordinator state-check):

| Symbol | Type | Verify command | Verdict |
|---|---|---|---|
| `StaffInvitationController` | BE controller @ `/api/v1/staff-invitations` | `grep -rn "StaffInvitationController" kitehub/kitehub-subscription/src/main/java` | ✅ exists |
| `POST /{token}/accept` + `GET /by-token/{token}` | BE endpoints | grep `@PostMapping`/`@GetMapping` in controller | ✅ exists |
| `V49__create_staff_invitation_audit_log.sql` | Flyway migration | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V49*` | ✅ exists |
| `(admin)/admin/staff/invite/page.tsx` | FE page | `find kitehub/kitehub-frontend/src -ipath "*staff/invite*"` | ✅ exists |
| `documents/01-business/kiteclass/staff-invitation/{rules,use-cases,api-contract}.md` | 3-layer business docs | `ls documents/01-business/kiteclass/staff-invitation/` | ✅ exists (api-contract UC-STAFF-INV-01..04) |

**Cross-layer (per `contract-first-for-cross-layer.md`):** api-contract.md ✅ đã tồn tại đầy đủ — KHÔNG cần Bucket 0 Foundation; FE/BE bucket reference contract hiện có.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — agent runtime walk | Claude | (a) Owner login → `POST /api/v1/staff-invitations` (email + role STAFF) trả 201 + DB row + token; (b) email "mời nhân viên" arrive MailHog với token link; (c) `GET /by-token/{token}` → 200 + invitation detail; (d) `POST /{token}/accept` (set password) → 201 + STAFF user tạo với role STAFF (không phải ADMIN/OWNER); (e) STAFF login → RBAC enforced (STAFF không truy cập Owner-only endpoint) | ✅ **PASS** — xem §5.1 |
| G2 — human local test | User | Login Owner → admin/staff/invite UI → mời staff → check email → click accept → set password → login STAFF → verify quyền | ⬜ |
| G3 — production parity | Claude + User | Production: staff-invitation schema migrate sạch (RDS) + email gửi thật (SES) + gateway JWT→header + RBAC role enforce đúng tenant scope | ⬜ |

### 5.1 G1 verdict (2026-06-05, coordinator walk)

**Walk target:** tenant `sky-education` (Owner `owner@skyedu.vn`). Stack production-equivalent (kitehub-subscription rebuilt 2026-06-05).

| Gate criteria | Walk result |
|---|---|
| (a) Owner invite | ✅ HTTP 201, `invitedBy` populated (gateway X-User-Id forward OK — FM-3 không manifest) |
| (b) Email sent | ✅ MailHog "Bạn được mời..." (FM-5 không manifest) |
| (c) GET by-token | ✅ 200 + invitation detail |
| (d) Accept | ✅ 200 + STAFF user tạo, role STAFF |
| (e) STAFF login + RBAC | ✅ JWT `tenantId=0edaee10` (FM-1 fixed) + STAFF→owner-only **403** Access denied |

**Verdict: G1 ✅ PASS** (sau fix FM-1).

**Catalog:** pre-walk dự đoán 3 P0; walk thực tế chỉ **FM-1 manifest** (STAFF JWT tenantId null → cross-tenant hole). FM-2 (role vocab) = tech-debt không block (Owner OWNER khớp OWNER_AUTHZ; STAFF→403 đúng). FM-3 (invited_by null) không manifest (gateway forward X-User-Id). FM-5 (email silent) không manifest.

**Fix shipped (GAP-981 DONE):** `resolveTenantIdForRole` thêm STAFF branch (tenant từ `staff_invitations.tenant_id WHERE accepted_user_id=userId ACCEPTED`) + repo method + field-inject (zero ctor ripple). Re-walk verified + 3 AuthService test pass.

**Byproduct:** GAP-784 → DONE (FE read-only STAFF + live 201 walk confirmed).

**2 minor finding (defer P3):** email link dùng prod domain `kitehub.me` (local test friction) + subject "Trung tâm KiteHub" thay tên tenant org.

G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 — ship same PR as G1 PASS flip.

## 6. Agent Spawn Pattern

- KHÔNG parallel-spawn cho walk (state-continuous flow).
- Pre-walk Opus agent BACKGROUND per `agent-background-spawn-default.md` §1 + `agent-model-opus-default.md` §1 — coordinator prep walk runbook parallel.
- Walk + batch-fix + re-walk execute sequential trong session.
- 3 P1 blocker resolve có thể spawn 1 Opus agent (id-model UUID-vs-Long = recurring class, đáng meta-fix một lần) — quyết định tại session start.

## 7. Closure Protocol

1. Flip `gap-status.csv` rows DONE cho gaps closed wave này (GAP-784/886/893 nếu resolved) + git mv → `phase-1-beta/closed/`.
2. ROADMAP §🎯 Current Status Snapshot — thêm Wave flow-kc2 closure entry.
3. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`.
4. Wave plan frontmatter `status: draft → complete` flip + Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3.
5. Campaign §4 row KC-2 flip → `🔄 walk-pass-pending-human` (G1 ✅) + ship G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 (`documents/05-guides/operations/YYYY-MM-DD-g2-recipe-kc2-staff-invitation.md`).
6. wave-history.jsonl append (new tag-based format) tại full closure.

## 8. Log

- **2026-06-05 (prep complete — 2 agent song song):** Pre-walk persona sim + RBAC blocker investigation chạy parallel.
  - **Blocker verdict (3 known gaps NOT walk-blocking):** GAP-784 ✅ self-corrected FE (role read-only display, body `{email,fullName}`) → DONE candidate on walk. GAP-893 ✅ self-corrected (CHECK V46 + data migration V61) → scope-close users.role + tách 3-table → P2. GAP-886 ⚠️ present nhưng chỉ kiteclass-core (kitehub-subscription staff đã all-UUID) → fold vào GAP-877 P0 actor-sweep (KHÔNG gộp với GAP-979 — khác fix surface).
  - **Persona sim REVISE "walkable now" → 3 P0 MỚI verified empirically:** FM-1 STAFF user accept KHÔNG link tenant (`User` entity no `tenant_id` → cross-tenant RBAC hole); FM-2 role-literal mismatch (`.role("STAFF")` seed vs `hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')` @PreAuthorize); FM-3 `invited_by NOT NULL` (V45:19) vs gateway `RemoveRequestHeader=X-User-Id` strip risk → Owner invite 500. + FM-4/5 P1 (email best-effort silent). Artifact `persona-review/2026-06-05-pre-walk-kc2-staff-invitation.md`.
  - **Walk-readiness:** KC-2 KHÔNG walk sạch — 3 P0 cần verify+fix trong walk (catalog-then-batch; pre-walk đã cho map). kitehub-subscription đã fix 2 P0 prior-walk (email + accept-creates-user); 3 P0 mới là regression-class kế cận.
- **2026-06-05 (plan stub ship):** Filed sau KC-1 G1 PASS merge (PR #2169). KC-2 = next-in-chain per campaign §3 (KC-1 → KC-2 → KC-3..9). State-check confirmed BE+FE+docs đã tồn tại (platform-side kitehub-subscription, not kiteclass-core). 3 P1 blocker GAP-784/886/893 quanh RBAC id-model. Prior walk Wave meta-6 Bucket A surface 17 bug — re-walk verify state hiện tại. Plan stub thỏa `check-wave-plan-completeness.sh` (8 sections + 4 frontmatter). Full §3 Scope + bucket expansion + pre-walk persona sim happens tại session start khi pick wave này.
