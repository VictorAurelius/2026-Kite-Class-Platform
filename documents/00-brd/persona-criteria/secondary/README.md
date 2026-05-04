# Secondary Persona Acceptance Criteria — Users-Within-Tenant

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md)

Per-role × tenant-context Acceptance Criteria for **secondary personas** — users **within** a tenant, not the tenant organization itself. Different from `../P<N>-*.md` (tenant-level AC).

**Audience:** Product Manager (drives review with secondary lens), Domain expert (acts as Student/Parent/Teacher/Admin), Engineering (consumes gap-linkage to plan UX fixes).

---

## Why this folder exists

User caught scope gap (2026-04-20):
> "BRD đã có cho đối tượng học sinh không?"

GAP-151 (Wave 15, 2026-04-30) shipped **tenant-level** AC for P1/P2/P3/P5 (organization-as-customer perspective). But users **within** a tenant — Student/Parent/Teacher/Admin — experience different journeys per tenant context:

- Student-in-P5 (K-12) ≠ Student-in-P1 (Solo Tutor): bulk import vs self signup, period attendance vs simple schedule, formal report card vs informal progress, parent-mediated payment vs direct
- Parent-in-P5 (K-12) is **legal mandate** (Luật Giáo dục Đ.83) — parent has legal right to monitor child's data
- Teacher-in-P3 (Medium Center, employee) ≠ Teacher-in-P1 (Solo, owner): commission tracking, multi-class grading, peer collaboration

Each cell needs its own AC doc. Without this, GAP-152 P5 review missed Student/Parent/GVCN UX gaps entirely.

---

## Priority matrix (9 secondary roles × 4 tenant contexts = 36 cells, NOT all needed)

| Secondary → | P1 Solo | P2 Small | P3 Medium | P5 K-12 |
|-------------|:-------:|:--------:|:---------:|:-------:|
| **Student** | 🟠 P1 (deferred) | 🔴 **P0** | 🔴 **P0** | 🔴 **P0 critical** |
| **Parent** | N/A (tenant=teacher) | 🟠 P1 (deferred) | 🟠 P1 (deferred) | 🔴 **P0 critical** |
| **Teacher-employee** | N/A (tenant=teacher) | 🟠 P1 (deferred) | 🔴 **P0** | 🔴 **P0** |
| **Admin** | N/A (solo) | N/A (owner=admin) | 🔴 **P0** | 🔴 **P0** |
| **Accountant** | N/A | N/A | 🟡 P2 (deferred) | 🟡 P2 (deferred) |
| **Receptionist** | N/A | N/A | 🟡 P2 (deferred) | 🟡 P2 (deferred) |
| **IT Staff** | N/A | N/A | 🟡 P2 (deferred) | 🟡 P2 (deferred) |
| **Parent Rep** | N/A | N/A | 🟡 P2 (deferred) | 🟡 P2 (deferred) |
| **Owner** | ✅ tenant=P1 | ✅ tenant=P2 | see admin | see admin |

**Legend:**
- 🔴 **P0** = Phase 1 (this folder, GAP-153) — 8 cells
- 🟠 P1 = Phase 2 (deferred to GAP-281 P1 cells follow-up — 4 cells)
- 🟡 P2 = Phase 3 (deferred to GAP-282 P2 cells follow-up — 4 cells)
- N/A = tenant persona IS this user (no separate AC needed)

---

## Directory Map (Phase 1 — 8 P0 cells)

| Path | Tenant context | Role | Status | Tracking |
|------|:--------------:|------|:------:|----------|
| `README.md` | — | (this index) | — | — |
| [`student-in-P2.md`](student-in-P2.md) | Small Tutoring Center | Student | 🟡 DRAFT v1 (PR #726) | GAP-153 SHIPPED |
| [`student-in-P3.md`](student-in-P3.md) | Medium Education Center | Student | 🟡 DRAFT v1 | GAP-153 |
| [`student-in-P5.md`](student-in-P5.md) | **K-12 School (USER PRIORITY)** | Student | 🟡 DRAFT v1 | GAP-153 |
| [`parent-in-P5.md`](parent-in-P5.md) | **K-12 School (LEGAL MANDATE)** | Parent | 🟡 DRAFT v1 | GAP-153 |
| [`teacher-employee-in-P3.md`](teacher-employee-in-P3.md) | Medium Education Center | Teacher (employee) | 🟡 DRAFT v1 | GAP-153 |
| [`teacher-employee-in-P5.md`](teacher-employee-in-P5.md) | K-12 School | Teacher (GVCN + bộ môn) | 🟡 DRAFT v1 | GAP-153 |
| [`admin-in-P3.md`](admin-in-P3.md) | Medium Education Center | Admin (lễ tân/kế toán/ops) | 🟡 DRAFT v1 | GAP-153 |
| [`admin-in-P5.md`](admin-in-P5.md) | K-12 School | Admin (văn phòng/giáo vụ) | 🟡 DRAFT v1 | GAP-153 |

**Total Phase 1:** 8 secondary persona AC docs.

---

## Deferred (Phase 2 + 3)

### Phase 2 — P1 cells (4 cells, tracked GAP-281)

- `student-in-P1.md` (Solo Teacher tenant — student receives signup link from teacher directly)
- `parent-in-P2.md` (Small Center — parent engagement at this scale)
- `parent-in-P3.md` (Medium Center — parent engagement at this scale)
- `teacher-employee-in-P2.md` (Small Center — 1-2 hired teachers)

### Phase 3 — P2 cells (4 cells, tracked GAP-282)

- `accountant-in-P3.md` / `accountant-in-P5.md` (financial role — payroll, BHXH/BHYT/TNCN, VAT invoicing)
- `receptionist-in-P3.md` / `receptionist-in-P5.md` (front-desk: enrollment, parent contact)
- `it-staff-in-P3.md` / `it-staff-in-P5.md` (system admin: bulk import, troubleshooting, integrations)
- `parent-rep-in-P3.md` / `parent-rep-in-P5.md` (Hội phụ huynh — communication coordination)

---

## File Placement Rules

- ✅ **Belongs here:**
  - One AC doc per (role, tenant context) cell that is P0 priority
  - README index (this file)

- ❌ **Does NOT belong here:**
  - Tenant-level AC → [`../P<N>-*.md`](..) (GAP-151 deliverable)
  - Generic role descriptions → [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas"
  - Review reports / scored ACs → [`../../persona-reviews/`](../../persona-reviews/) (GAP-152 onwards)

- **Naming:** `<role-slug>-in-P<N>.md` (lowercase kebab + tenant ID — e.g. `student-in-P5.md`, `teacher-employee-in-P3.md`)

---

## How to use

### For AC author

1. **Read** [`../_TEMPLATE.md`](../_TEMPLATE.md) — same template as tenant-level AC, 6 categories
2. **Read** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas" — find role's "Key Actions" + responsibilities
3. **Read** sibling tenant AC doc (e.g. for `student-in-P5.md`, read [`../P5-k12-school.md`](../P5-k12-school.md)) — context + scale
4. **Derive** 10-20 ACs per (role, tenant) cell focused on user-perspective journey:
   - Onboarding: how role receives account + first login
   - Daily actions: core workflow this role does (different from tenant admin)
   - Communication: who role talks to (peer, supervisor, family)
   - Financial: if role touches money (mostly admin/accountant; student/parent = payment receiver/payer)
   - Edge: forgot password, transferred class, graduation, role change
   - Exit: account deactivation, data retention per VN PDPL Art 16 (minor)
5. **Cross-link** existing gaps (GAP-051 import, GAP-052 parent portal, GAP-055 report card, GAP-058 hierarchy, GAP-063 SMS/Zalo, GAP-186 child protection)
6. **Status remains blank** — filled at GAP-152 review time

### For reviewer (GAP-152 Round 1 onwards)

1. **Load BOTH** tenant AC doc (e.g. `P5-k12-school.md`) + relevant secondary AC docs (e.g. `student-in-P5.md`, `parent-in-P5.md`, `teacher-employee-in-P5.md`, `admin-in-P5.md`)
2. **Role-play** for each persona — mark PASS/PARTIAL/FAIL with evidence
3. **Output** combined review report covering tenant + all secondary personas in that tenant context

---

## Cross-References

- **Parent index:** [`../README.md`](../README.md) — tenant-level AC
- **Template:** [`../_TEMPLATE.md`](../_TEMPLATE.md) — reusable across both tenant + secondary
- **Catalog:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas"
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md) v1.2+
- **Parent gaps:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (tenant AC + template), [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md) (review execution — UNBLOCKED after Phase 1 ships), [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md) (this Phase 1)
- **Follow-up gaps:** GAP-281 Phase 2 P1 cells (tracked in closure PR), GAP-282 Phase 3 P2 cells
