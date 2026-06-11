# GAP-1225: Course card catalog thiếu cover image — placeholder icon (re-score delta #2)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-11 (re-score — visual polish CODE/DATA)
**Affects:** `DemoAcademicSeeder` courses (coverImageUrl null) + catalog card render

## Problem

Course entity CÓ `coverImageUrl` (update được cả khi PUBLISHED) nhưng seeder không set → catalog card render placeholder gradient/icon thay vì ảnh — kit có cover.

## Proposed Fix

Seeder set coverImageUrl = asset webp thật per-tenant (demo-banners) lúc create + reconcile pass (mirror GAP-1209 publish reconcile).

## Acceptance Criteria

- [x] Catalog demo-trio render cover ảnh thật

## Log

- **2026-06-11 (DONE):** coverImageUrl per-tenant webp (create + reconcile backfill) + catalog card wire next/image. DB verify 6 course + catalog render cover.
