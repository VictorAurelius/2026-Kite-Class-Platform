# GAP-754: Multi-branch foundation — Phase 2 scope

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend schema + Frontend dashboard + DevOps RLS + Compliance)
**Detected:** 2026-05-26
**Related PRs:** [Wave beta-prep-1 Bucket H PR (TBD)]
**Related Docs:** `documents/02-architecture/adr/ADR-036-multi-branch-defer-phase-2.md`, `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md` §3 Bucket H + Decision D3
**Related ADR:** ADR-036

## Current State (verified 2026-05-26)

> Per `audit-to-gap-pipeline.md` §2.5 state-check mandate.

| Piece | File / Path | Status |
|---|---|---|
| Database `branches` table | Not declared anywhere | ❌ missing |
| Domain `branch_id` discriminator | Mọi service entity (`Class`, `Enrollment`, `Invoice`, `Attendance`, `Student`) scope theo `tenant_id` đơn lẻ | ❌ missing |
| `BRANCH_MANAGER` role | ADR-003 role hierarchy stops at P3 Center Manager (tenant-wide scope) | ❌ missing |
| Cross-branch dashboard | Owner dashboard scope theo tenant_id only | ❌ missing |
| Branch-scoped RLS policy | V60 RLS migration scope `tenant_id` only | ❌ missing |
| Multi-branch FAQ q1.4 | `documents/05-guides/user-manual/anonymous/faq.md` line ~58 | 🟡 honest defer text (Bucket H this PR) |
| Waitlist mechanism cho ≥2 branches signup | Filter logic Bucket F Agent 6 (Wave beta-prep-1) | 🟡 ship parallel với GAP này |
| Phase 1 BETA cohort filter | Bucket F invite tooling shortlist P2 1-branch only | 🟡 ship parallel |

**Grep commands run:**

```bash
# Verify no branches schema exists
find kitehub kiteclass -path '*/migrations/*.sql' | xargs grep -l "CREATE TABLE.*branch" 2>/dev/null
# (empty output expected — no branches table)

grep -rn "branch_id" kitehub/*/src/main/java kiteclass/*/src/main/java 2>/dev/null | head -5
# (empty output expected — no branch_id field)

grep -n "BRANCH_MANAGER\|branch_manager" kitehub/*/src/main/java/.../Role*.java 2>/dev/null
# (empty output expected — role doesn't exist)
```

## Problem

Outside-in audit Wave beta-prep-1 3-agent consensus 2026-05-26 (persona simulation + VN edu SaaS benchmark + failure-mode matrix) surfaced finding **P0 Multi-branch missing**: ~50% P2 Center Owner cohort (anh Tâm Sky Education 3 chi nhánh + chị Hằng Quang Minh 2 chi nhánh + tương tự) cần quản lý đa chi nhánh từ ngày đầu. Hệ thống hiện tại mặc định `tenant_id = 1 branch implicit` → P2 multi-branch cohort blocked signup hoặc force tự maintain Excel ngoài system.

Per ADR-036 (2026-05-26) — multi-branch foundation defer Phase 2 vì:
- PDPL deadline 2026-07-01 (~5 tuần countdown) chiếm priority
- Multi-branch schema + cross-branch dashboard + branch-scoped RLS = ~3-4 tuần engineering
- Schema design cần học từ Phase 1 BETA actual usage (~5 P2 1-branch tenants) trước khi commit hierarchical design

Phase 1 BETA Bucket F (Wave beta-prep-1 Agent 6) ship signup form filter "Số chi nhánh > 1 → waitlist redirect" + Bucket H ship ADR-036 + FAQ honest defer + GAP này (Phase 2 follow-up).

## Proposed Fix

Wave multi-branch-1 trong Phase 2 (post Phase 1 BETA gate met):

### Phase 2.1 — Schema + Domain (3-5d)

1. **Migration V70 `branches` table:**
   - `id` UUID PK
   - `tenant_id` FK (parent tenant)
   - `name`, `code`, `address`, `phone`, `manager_user_id`
   - `created_at`, `updated_at`, `deleted_at`
   - Indexes: `(tenant_id, deleted_at)`, `(tenant_id, code) UNIQUE`

2. **Migration V71 add `branch_id` to multi-branch-scoped tables:**
   - `classes`, `enrollments`, `students`, `attendance_records`, `invoices`
   - Default `branch_id` = first branch của tenant (data migration)
   - NOT NULL constraint sau backfill

3. **Domain layer:**
   - `Branch` entity + repository + service
   - `BranchScopedService` base class với `@PreAuthorize("hasAccessToBranch(#branchId)")`
   - Extend `hasAccessToClass` / `hasAccessToEnrollment` etc. với branch boundary check

### Phase 2.2 — RLS (2-3d)

4. **Migration V72 branch-scoped RLS policies:**
   - Extend V60 RLS với `branch_id` discriminator
   - Policy: tenant-wide roles (Owner, Manager) thấy all branches; branch-scoped role (BRANCH_MANAGER, Teacher) chỉ thấy own branch

5. **Migration V73 add `BRANCH_MANAGER` role:**
   - Extend ADR-003 role hierarchy
   - Permissions: read/write classes + enrollments + invoices + attendance scoped to assigned branch_id only
   - Cannot create/delete branches (Owner-only)

### Phase 2.3 — Frontend (3-5d)

6. **Branch CRUD UI:** Owner-only `/admin/branches` page
7. **Branch switcher:** Header dropdown (cho users với multi-branch access)
8. **Cross-branch dashboard:** Owner dashboard với per-branch breakdown + tổng hợp
9. **Cross-branch transfer:** Move student/teacher between branches (Owner-only)
10. **Branch-scoped role assignment:** Owner gán BRANCH_MANAGER cho user X với branch_id Y

### Phase 2.4 — Migration + Live (2-3d)

11. **Migrate Phase 1 tenants:**
    - Mỗi existing tenant tự động tạo 1 branch mặc định ("Chi nhánh chính")
    - Backfill `branch_id` cho mọi rows (classes/enrollments/etc.)
    - Verify zero data loss

12. **Waitlist conversion:**
    - Email waitlist signups: "Đa chi nhánh sẵn sàng — đăng ký onboarding"
    - Onboarding playbook update với multi-branch setup steps

13. **Cross-branch report types:**
    - Doanh thu cross-branch
    - Sĩ số học sinh cross-branch
    - GV utilization cross-branch
    - Branch P&L

## Acceptance Criteria

- [ ] V70 `branches` table migration shipped + tested
- [ ] V71 `branch_id` discriminator backfill migration shipped + tested
- [ ] V72 branch-scoped RLS migration shipped + tested
- [ ] V73 `BRANCH_MANAGER` role + permissions shipped
- [ ] Domain layer `Branch` entity + service + branch-scoped access guards
- [ ] FE Branch CRUD UI shipped
- [ ] FE Branch switcher header dropdown shipped
- [ ] FE Owner cross-branch dashboard shipped
- [ ] Cross-branch transfer workflow shipped (student + teacher)
- [ ] BRANCH_MANAGER role assignment UI shipped
- [ ] Phase 1 tenant data backfill verified (zero loss)
- [ ] Waitlist email conversion campaign sent
- [ ] FAQ q1.4 updated từ "defer Q3 2026" → "now available" link manual
- [ ] Onboarding playbook updated với multi-branch setup
- [ ] Audit suite refresh: business-logic + security + performance audits

## Dependencies + Blockers

- **Phase 1 BETA gate met** — required per Phase progression (CLAUDE.md):
  - Quality audit ≥80
  - 5 beta tenants live
  - 0 P0 incidents 2 tuần
- **Counsel review** — not strict prerequisite cho multi-branch (no PDPL change); but better cover liability
- **Actual usage data Phase 1** — schema design informed by ≥2 weeks production usage Phase 1 BETA cohort

## Effort estimate

**Total: ~10-15d engineering** (Wave multi-branch-1 trong Phase 2):
- Phase 2.1 schema + domain: 3-5d
- Phase 2.2 RLS + role: 2-3d
- Phase 2.3 frontend: 3-5d
- Phase 2.4 migration + live: 2-3d

Sized cho 3-4 parallel Opus bg-agents per `agent-model-opus-default.md` + `agent-background-spawn-default.md`.

## Risk

- **Schema drift Phase 1 → Phase 2:** Phase 1 tenants tạo data mà không biết multi-branch coming → migration risk. Mitigation: V71 backfill cẩn thận + test trên staging copy production data first.
- **RLS performance regression:** Adding branch_id filter to existing RLS policies → query plan changes. Mitigation: EXPLAIN ANALYZE + benchmark before/after.
- **UX complexity creep:** Branch switcher + cross-branch ops thêm UI surface → confusion cho 1-branch tenants. Mitigation: progressive disclosure (1-branch tenants không thấy switcher).
- **Counsel review delay:** Nếu Phase 2 trigger gates counsel engagement, multi-branch có thể defer thêm. Mitigation: ADR-036 không phụ thuộc counsel scope.

## Log

- **2026-05-26 (Filed P1 OPEN):** GAP-754 created paired với ADR-036 + Wave beta-prep-1 Bucket H. Triggered by outside-in audit 3-agent consensus C-4 P0 Multi-branch missing (Wave beta-prep-1 plan §1 Brainstorm Q1). Decision D3 locked: defer Phase 2 + Phase 1 BETA cohort filter P2 1-branch only. Phase 1 ship paired artifacts: ADR-036 (Bucket H this PR) + signup filter (Bucket F Agent 6 parallel) + FAQ q1.4 update (Bucket H this PR) + waitlist mechanism (Bucket F Agent 6 parallel) + landing/pricing honest copy (Bucket L coordinator inline). Phase 2 trigger evaluation per CLAUDE.md gates.
