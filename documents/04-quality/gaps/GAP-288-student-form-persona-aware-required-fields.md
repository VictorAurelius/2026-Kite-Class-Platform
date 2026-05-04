# GAP-288: Student form persona-aware required-field gating

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend) + Backend (validation)
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (5-15 students, kid age 8-15 typically without email), broader minor-student tenants

## Problem

`kiteclass/kiteclass-frontend/src/components/forms/student-form.tsx:21` declares `email: z.string().email('Email không hợp lệ')` as REQUIRED for every student created. P1 AC-ONBOARD-003 fail signal explicitly bans "email + ngày sinh + lớp + parent phone đều required" for solo persona.

Solo teacher's typical use-case: 5 student names + phone added in 5 minutes via mobile. Forcing email per student means either (a) teacher generates fake emails (data quality issue) or (b) collects from parents (delayed onboarding).

Backend likely mirrors this constraint via `@NotBlank` on `Student.email` field — needs verification.

## Root Cause

Form schema designed for center/school personas where student records flow from official enrollment data with email collected at registration. Solo persona's informal context (per-session tutor, kid student, no school admin) wasn't accommodated.

## Proposed Fix

1. Frontend: `studentSchema` change `email` from required to `.optional()` matching `phone` (line 22).
2. Frontend: at least ONE of (email, phone, parent phone) must be provided — refine via `.refine()` with custom error.
3. Backend: relax `@NotBlank` on Student.email; add cross-field constraint "at least 1 contact" via custom validator.
4. UX: persona-aware default — solo persona form shows phone field FIRST + emphasizes "tên + phone đủ rồi"; center persona shows email FIRST.
5. Migration: existing student records with email — no change. Schema allows null going forward.

## Acceptance Criteria

- [ ] `studentSchema` email is optional with cross-field "at least 1 contact" validation
- [ ] Backend Student validation relaxed; cross-field validator added
- [ ] DB migration NOT NULL → NULL on email (with default empty handling for existing rows if needed)
- [ ] Persona-aware form ordering (phone first for solo)
- [ ] Form tests: solo persona can save student with name+phone only
- [ ] AC-ONBOARD-003 passes when re-tested

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §1
- AC: AC-ONBOARD-003, AC-OPS-008
- Sibling: GAP-293 (persona-aware feature gating — foundation)
- Sibling: GAP-051 (xlsx import — orthogonal but shares student-shape concerns)

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check: `student-form.tsx:21` confirmed email required; phone already optional. Backend validation needs verification at gap-fix time.
