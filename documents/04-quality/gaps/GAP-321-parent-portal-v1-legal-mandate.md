# GAP-321: Parent Portal v1 — LEGAL MANDATE (Luật Giáo dục 2019 Đ.83)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend + Frontend + Compliance
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 persona review)
**Related PRs:** —
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 1
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-COMM-001..005, AC-OPS-009
- `documents/00-brd/persona-criteria/secondary/parent-in-P5.md` (84 legal citations)
- Existing GAP-052 (parent portal stub)

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Parent role distinguished from generic user | `kiteclass-core/.../user/Role.java` (assumed) | ❌ missing — flat student/teacher/admin model |
| Parent-student relationship entity | `kiteclass-core/.../student` | ❌ missing |
| Parent-facing UI in kiteclass-frontend | `kiteclass/kiteclass-frontend/src/app` | ❌ missing — no `/parent` routes |
| Multi-children selector | — | ❌ missing |
| View học bạ (transcript) per child | — | ❌ missing |
| View điểm danh per child | — | ❌ missing |
| View học phí + payment per child | — | ❌ missing |
| View hạnh kiểm per child | — | ❌ missing |
| Multi-channel notification (Zalo + SMS + email + push) | — | ❌ missing (GAP-063 scaffold only) |
| GAP-052 status | `documents/04-quality/gaps/GAP-052-parent-portal.md` | 🔵 OPEN — original gap, this gap consolidates K-12 specific scope |

**Grep commands run:**
```bash
grep -rl "parent" kiteclass/kiteclass-core/src/main/java --include="*.java" | head
find kiteclass/kiteclass-frontend/src/app -type d -name "parent*"
grep -rl "ParentPortal\|guardian" kiteclass/ --include="*.java" --include="*.tsx"
```
Result: no parent-portal scaffolding in code; GAP-052 file exists but no implementation.

## Problem

K-12 schools (P5 persona) cannot deploy without a parent portal. Luật Giáo dục 2019 Điều 83 Khoản 2 grants parents the **legal right** to view full information about their child's learning + behavior. Without this:

1. Schools deploying KiteClass for K-12 violate Vietnamese education law.
2. Parents can sue schools (and platform as data processor).
3. 6 P5 tenant ACs (AC-COMM-001..005, AC-OPS-009) + ~26 secondary parent ACs are unsatisfiable.
4. Daily operations break: GVCN cannot publish điểm số, conduct, attendance to parents.
5. Emergency communication (school closure, child safety) cannot reach 1500+ parents.

This gap **consolidates** the K-12-specific parent portal scope on top of the original GAP-052 (which was generic).

## Context

P5 K-12 review (Round 1) scored 0/6 ACs in Communication category — every AC depends on parent portal existing. Persona simulation: Trường THCS 1200 HS / 1800 PH — without portal, kế toán + GVCN + Văn thư manually phone-call PH for any update. Scale (1800 × multi-events) makes this infeasible.

**Cross-cuts:**
- Child protection (GAP-322) needs parent portal as report intake channel
- MOET financial reporting (GAP-336) for transparency — parent should see học phí breakdown
- Phổ cập escalation (GAP-341) needs parent contact channel before Phòng GD escalation
- Complaint workflow (GAP-339) needs parent submission entry point

## Evidence

- Luật Giáo dục 2019 Điều 83 Khoản 2: "Cha mẹ học sinh có quyền yêu cầu nhà trường, cơ sở giáo dục cung cấp đầy đủ thông tin về quá trình học tập, rèn luyện của con."
- Decree 13/2023 Điều 16: special protection of children's personal data — parent has consent rights
- P5 review report Finding 1: 0% communication coverage
- AC-COMM-001 marked LEGAL MANDATE in P5-k12-school.md

## Proposed Fix

### Phase 1 — Read-only portal v1 (Stage 1, Q3 2026)

1. **Data model:** Add `Parent`, `ParentStudentRelationship` entities; many-to-many (parent can have multiple children, child can have 1-2 parents); sibling dedup at import time.
2. **Auth:** Distinguish `parent` role from `student`/`teacher`/`admin`; Zalo OTP + email/password login (Zalo dominant in VN K-12).
3. **API:** `GET /api/v1/parent/children` (list with consent flags), `GET /api/v1/parent/children/{id}/transcript`, `/attendance`, `/fees`, `/conduct`, `/notifications` — all scoped to relationship.
4. **Frontend:** New `/parent` route in `kiteclass-frontend` with multi-children selector + dashboard cards per child + drill-down per facet.
5. **i18n:** Vietnamese-only Phase 1; future EN/zh-CN for international schools.
6. **Audit log:** Every parent-side data view logged for legal compliance evidence.

### Phase 2 — Write actions (Stage 2, Q4 2026)

- File complaint (GAP-339)
- Confirm receipt of monthly conduct report
- RSVP parent-teacher meeting (GAP-338)
- Submit absence excuse with evidence upload

### Phase 3 — Multi-channel notification (Stage 3, Q1 2027)

- Bulk notify integration (GAP-063 Zalo + SMS + email + push)
- Read-receipt analytics
- Emergency broadcast (GAP-337) leverages this

## Acceptance Criteria

- [ ] `Parent` + `ParentStudentRelationship` entities migrated (V<N>__parent_portal.sql)
- [ ] Bulk import xlsx supports `Tên Cha, SĐT Cha, Email Cha, Tên Mẹ, SĐT Mẹ, Email Mẹ` columns with sibling dedup (links to GAP-325)
- [ ] Zalo OTP login working (test tenant + real Zalo OA sandbox)
- [ ] Parent dashboard renders 1 child / multi-children with cards: học bạ, điểm danh tháng hiện tại, học phí pending, hạnh kiểm HK hiện tại
- [ ] All 6 facet drill-down pages render (transcript / attendance / fees / conduct / notifications / kỷ luật history)
- [ ] All parent-side reads emit audit log entry (entity, parent ID, timestamp, IP)
- [ ] Test: real PH login → see 2 children → drill into HS A 7A → see 12 môn điểm + 32/35 buổi điểm danh + học phí tháng 10 paid + hạnh kiểm "Tốt"
- [ ] PDPL Decree 13/2023 Art 16 children-data special protection: parental consent flag tracked + viewable; data minimization (no fields beyond Đ.83 list)
- [ ] Documentation: 3-layer (rules.md + use-cases.md + api-contract.md) per `documents/01-business/kiteclass/parent-portal/`
- [ ] business-logic-review.md 5-attribute frontmatter on rules.md (Source = Luật GD Đ.83 + Decree 13/2023; Compliance = Compliant; Reviewer = solo-dev acting Legal scout, queue formal counsel review)

## Related

- **Supersedes scope of:** GAP-052 (original parent portal stub) — close GAP-052 once this lands
- **Blocks:** GAP-322 (child protection — needs parent intake), GAP-337 (emergency broadcast), GAP-338 (parent meeting), GAP-339 (complaint), GAP-321 secondary ACs
- **Depends on:** GAP-325 (parent-student bulk import linking)
- **Cross-cuts:** GAP-063 (Zalo channel), GAP-184 (data retention 5y), GAP-186 (child protection policy)
- **Audit-to-gap-pipeline.md** Step 2.5 state-check: complete (no pre-existing implementation; greenfield)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-persona-review-round-1.md` Bucket D
- **business-logic-review.md** Source: Luật Giáo dục 2019 Đ.83 Khoản 2; Compliance: Compliant per Đ.83 + Decree 13/2023 Art 16; Cadence: Annual + event-driven on Đ.83 amendment

## Log

- **2026-05-04** — Filed during Wave 17 Bucket D P5 K-12 persona review. State-check confirms greenfield (no pre-existing parent portal in kiteclass-core/frontend). Consolidates K-12-specific scope on top of generic GAP-052.
