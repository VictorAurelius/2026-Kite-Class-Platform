# GAP-153: Secondary Persona Acceptance Criteria (Student / Parent / Teacher / Admin)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — blocks GAP-152 persona review for tenants where end-users critical)
**Domain:** Business / Persona / Governance
**Found:** 2026-04-20 (user raised: "BRD đã có cho đối tượng học sinh không?")
**Affects:** Persona review completeness, Student/Parent UX quality, K-12 readiness (P5 persona)

## Problem

Personas catalog (`00-brd/personas-catalog.md`) phân 2 trục:

| Trục | Đã scope trong GAP-151 | Chưa scope |
|------|:---------------------:|:----------:|
| **Tenant personas** (tổ chức mua product) | ✅ P1/P2/P3/P5 AC | P4/P7/P8/P9/P10 |
| **Secondary personas** (users trong tenant) | ❌ 0/9 personas | Student, Parent, Teacher, Admin, Accountant, Receptionist, IT Staff, Parent Rep, Owner |

GAP-151 chỉ cover tenant-level AC (tổ chức). Users trong tenant (học sinh, phụ huynh, giáo viên, admin) chưa có AC → review "system đúng nghiệp vụ cho học sinh chưa?" không answer được.

### Concrete example — Student journey mismatch per tenant context

Student-within-P5 (K-12) journey:
- Nhận tài khoản via bulk import (không tự signup)
- Xem TKB theo tiết (multiple periods/day)
- Submit homework + xem điểm (bảng điểm VN format)
- Conduct tracking (hạnh kiểm)
- Liên lạc GVCN + phụ huynh (Zalo/SMS)
- Đóng học phí qua phụ huynh (không tự pay)

Student-within-P1 (Solo Teacher) journey:
- Tự signup (teacher send link)
- Xem lịch học đơn giản (1-2 classes/week)
- Homework cá nhân
- Tự trả tiền hoặc parent trả (direct, không qua tenant finance system)
- Không có conduct/report card formal

→ **4 AC docs khác nhau cho Student**, mỗi tenant context 1 doc. Tương tự Parent/Teacher/Admin.

### Coverage blind spot

Without secondary persona AC:
- K-12 review (GAP-152 P5) sẽ miss Student/Parent UX gaps
- Bulk import (GAP-051) chỉ validate tenant-side workflow, không validate "student nhận credentials thế nào?"
- Parent portal (GAP-052) scoped from tenant perspective, không có AC từ parent perspective

## Root Cause

Catalog v1 (2026-04-14) liệt kê Secondary Personas (Owner, Admin, Teacher, Student, Parent, Accountant, Receptionist, IT Staff, Parent Rep) trong 1 table với "Key Actions" bullets — không phải AC. GAP-151 scope không đọc hết table này.

Assumption at GAP-151 creation (2026-04-20): "tenant AC covers end-user workflows transitively". Reality: **users within tenant experience different journey per tenant type**. A student in P5 K-12 ≠ student in P1 solo teacher.

## Proposed Fix

### Deliverable: Matrix AC approach

Create `documents/00-brd/persona-criteria/secondary/` subdirectory với matrix-style AC docs.

**Priority matrix (9 secondary personas × 4 tenant contexts = 36 cells, NOT all needed):**

| Secondary → | P1 Solo Teacher | P2 Small Center | P3 Medium Center | P5 K-12 School |
|-------------|:---------------:|:---------------:|:----------------:|:--------------:|
| **Student** | 🟠 P1 | 🔴 P0 | 🔴 P0 | 🔴 **P0 critical** |
| **Parent** | N/A (tenant=teacher) | 🟠 P1 | 🟠 P1 | 🔴 **P0 critical** |
| **Teacher-employee** | N/A (tenant=teacher) | 🟠 P1 | 🔴 P0 | 🔴 P0 |
| **Admin** | N/A | N/A (owner=admin) | 🔴 P0 | 🔴 P0 |
| **Accountant** | N/A | N/A | 🟠 P1 | 🟠 P1 |
| **Receptionist** | N/A | N/A | 🟡 P2 | 🟡 P2 |
| **IT Staff** | N/A | N/A | 🟡 P2 | 🟡 P2 |
| **Parent Rep** | N/A | N/A | 🟡 P2 | 🟡 P2 |
| **Owner** | ✅ is P1 | ✅ is P2 | see admin | see admin |

Legend:
- 🔴 P0 = create AC doc now (12 cells)
- 🟠 P1 = create after P0 (6 cells)
- 🟡 P2 = defer to post-GA (6 cells)
- N/A = tenant persona IS this user, no separate AC needed

### Phase 1 (this gap): 12 P0 AC docs

Filenames: `documents/00-brd/persona-criteria/secondary/<role>-in-P<N>.md`:

1. `student-in-P2.md`
2. `student-in-P3.md`
3. `student-in-P5.md` ← **user's priority**
4. `parent-in-P5.md` ← **critical cho K-12**
5. `teacher-employee-in-P3.md`
6. `teacher-employee-in-P5.md`
7. `admin-in-P3.md`
8. `admin-in-P5.md`

Plus 4 deferred (student-in-P1, parent-in-P2/P3, teacher-employee-in-P2) khi scope cho phép.

Each file follows GAP-151 template with:
- Context: tenant + role
- Onboarding AC (receiving account, first login)
- Daily actions AC (core workflows)
- Communication AC (with other roles)
- Financial AC (if applicable)
- Edge cases AC (forgot password, transferred class, graduated)
- Exit AC (account deactivation, data retention per VN PDPL)

### Phase 2 (future): P1 + P2 cells

Track as GAP-155 (P1 cells) + GAP-156 (P2 cells) after Phase 1 stable.

## Acceptance Criteria

- [ ] `00-brd/persona-criteria/secondary/` directory + README created
- [ ] 8 P0 secondary persona AC docs populated (student × 3, parent × 1, teacher-employee × 2, admin × 2)
- [ ] Each doc follows GAP-151 template structure
- [ ] Gap linkage populated (cross-reference GAP-051..064 where applicable, especially GAP-052 parent portal, GAP-055 report card, GAP-063 SMS/Zalo)
- [ ] `personas-catalog.md` "Secondary Personas (Users within Tenant)" section updated to reference new AC docs
- [ ] `00-brd/persona-criteria/README.md` (created by GAP-151) extended with secondary/ navigation
- [ ] GAP-152 dependency updated — cannot execute P5 review without student-in-P5 + parent-in-P5 AC
- [ ] ROADMAP Epic 14 updated with GAP-153
- [ ] Follow-up gaps filed: GAP-155 (P1 cells deferred), GAP-156 (P2 cells deferred)

## Dependencies

- **Blocked by GAP-151** — template must exist before Phase 1 populated
- `personas-catalog.md` Secondary Personas table (already exists — source for role attributes)

## Blocks

- **GAP-152 P5 review** — cannot meaningfully execute K-12 review without student-in-P5 + parent-in-P5 AC (those are the most painful user touches in K-12)
- **GAP-152 P3 review** — needs student-in-P3 + admin-in-P3 + teacher-employee-in-P3

## Out of Scope

- **Execute the reviews** (GAP-152 handles all review execution after AC ready)
- **Tier 2/3 tenant contexts** (P4/P7/P8/P9/P10) — future gap
- **P1 + P2 cells** — deferred to GAP-155/156
- **Parent Rep / Receptionist / IT Staff / Accountant AC** — P1/P2 cells deferred

## Related

- GAP-151 — parent (template + tenant-level AC — this gap extends to secondary personas)
- GAP-152 — consumer (will use both tenant AC + secondary AC for review execution)
- GAP-150 — sibling (BRD skeletons — provide pricing/compliance values referenced by AC)
- GAP-051 bulk import — validated by student-in-P5 onboarding AC
- GAP-052 parent portal — validated by parent-in-P5 AC
- GAP-055 report card — validated by student-in-P5 + parent-in-P5 AC
- GAP-058 role hierarchy — validated by admin-in-P3/P5 AC
- Rule: `.claude/rules/meta-gap-priority.md` §3 — business-logic tier

## Log

- 2026-04-20 — Created. User caught scope gap: "BRD đã có cho đối tượng học sinh không?" — revealed GAP-151 only covered tenant personas, missing 9 secondary personas × 4 tenant contexts. Phase 1 scoped to 12 P0 cells; 10 cells deferred to P1/P2 follow-ups. Blocks GAP-152 K-12 review (P5 critical).
