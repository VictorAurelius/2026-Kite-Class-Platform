# GAP-1423: class create with blank start_date → 500 (DB NOT NULL) instead of 400 — 3-layer optional mismatch

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed (FE+BE)
**Found:** 2026-06-15 (KC-3 re-walk — POST /api/v1/courses/{id}/classes 500)
**Affects:** `kiteclass-frontend` class-form + `kiteclass-core` CreateClassRequest

## Problem

Creating a class with the "Ngày bắt đầu" field blank returned HTTP **500**: `null value in column "start_date" of relation "classes" violates not-null constraint`. `classes.start_date` is **NOT NULL** in the DB, but `startDate` was **optional at every app layer** — FE zod `z.string().optional()` + BE `CreateClassRequest.startDate` had no `@NotNull` → a blank date passed FE + BE validation and only failed at the DB → 500 (should be a 400 field error). `end_date` is genuinely nullable (no change).

## Fix (this PR)

- FE `class-form.tsx`: `startDate` → `z.string().min(1, 'Ngày bắt đầu không được để trống')` + `required` on the field.
- BE `CreateClassRequest`: `@NotNull(message="Ngày bắt đầu không được để trống")` on `startDate` → 400 with a clear message instead of 500.

## Acceptance Criteria

- [x] FE: tsc + lint clean; `class-form.test` 9/9; integration tests (`.skip`, unaffected).
- [x] BE: `ClassControllerTest` 16/16 + `CourseClassCrudOwnerIT` 5/5 (builder supplies startDate).
- [x] Live (post FE+BE rebuild): create class without date → 400 field error; with date → 201.

## Related

- Found in: KC-3 academic re-walk 2026-06-15
- Class: app-layer optional vs DB NOT NULL contract mismatch (sibling of GAP-1422 landing template_type)
