# GAP-323: MOET TT 32/2018 GDPT Subject Taxonomy Auto-Seed per Cấp Học

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (MoET-mandate but not LEGAL-block — admin can manually seed as workaround)
**Domain:** Backend (KiteClass Core) + Reference Data
**Detected:** 2026-05-04 (P5 K-12 persona review Round 1)
**Related Docs:** `documents/00-brd/persona-criteria/P5-k12-school.md` AC-ONBOARD-006

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Curriculum entity | `module/k12/entity/Curriculum.java` (JSONB subjects field) | ✅ shipped |
| Curriculum seed data per cấp học | `db/migration/V##*.sql` | ❌ missing |
| TT 32/2018 GDPT 2018 reference | nowhere | ❌ missing |

**Grep commands run:**
```bash
grep -rli "TT 32/2018\|GDPT 2018\|chương trình giáo dục phổ thông" kiteclass/ documents/
# returns: nothing in code; AC docs reference it
```

## Problem

MOET Thông tư 32/2018 (chương trình GDPT 2018) defines 13 môn THCS / 12 môn cấp 1 / 14 môn THPT with số tiết/tuần per môn. K-12 onboarding wizard should auto-populate Curriculum.subjects when admin chooses cấp học (THCS/THPT) — not require admin to type 13 môn manually.

## Proposed Fix

1. Reference data seed migration `V##__seed_moet_curriculum_taxonomy.sql`:
   - Cấp 1 (lớp 1-5): 11 môn (Toán, TV, Tự nhiên & Xã hội, Đạo đức, Tiếng Anh, Mỹ thuật, Âm nhạc, Thể dục, Tin học, Lịch sử & Địa lý, HĐTN)
   - Cấp 2 (lớp 6-9): 13 môn (Toán, Ngữ Văn, Tiếng Anh, KHTN, KHXH, Lịch Sử, Địa Lý, GDCD, Tin học, Công Nghệ, Thể Dục, Âm Nhạc, Mỹ Thuật)
   - Cấp 3 (lớp 10-12): 14 môn (theo phân ban)
2. Seed `subjects` JSONB with `{name, weekly_hours, is_optional}` per môn
3. Onboarding wizard offers "Use MoET TT 32/2018 default" button
4. Admin can edit (add CLB / môn tự chọn / liên cấp combinations)

## Acceptance Criteria

- [ ] V## migration seeds 3 curriculum templates (cấp 1/2/3) per `instance_id` template
- [ ] Subject names + weekly_hours match TT 32/2018 (citation in migration comment)
- [ ] Wizard wires "use default" button
- [ ] Admin edit endpoint preserved
- [ ] Unit test verifies seed counts match reference (11/13/14)

## Related

- AC-ONBOARD-006 P5 tenant
- AC-ONBOARD-001 teacher (qualification metadata may reference subjects)
- GAP-054 (multi-subject)

## Log

- 2026-05-04 — Filed by Wave 17 Bucket D. State-check: Curriculum entity exists but no MoET seed.
