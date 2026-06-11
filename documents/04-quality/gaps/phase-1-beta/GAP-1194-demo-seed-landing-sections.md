---
id: GAP-1194
title: Seed landing sections (teachers[] / pricing / stats) lấp empty-state khi đã có data thật
status: PARTIAL
priority: P2
domain: Mixed
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 70
---

# GAP-1194 — Seed landing sections (teachers / pricing / stats)

## Problem

Landing 2 tenant demo còn empty-state cho sections teachers[] / pricing / stats vì phụ thuộc onboarding manual. Khi academic core đã seed (GAP-1190..1193), nên lấp landing sections bằng data thật thay vì empty-state. Wave plan Bucket E (chưa làm).

## Proposed Fix

Seed `teachers[]` + `pricing` + `stats` vào LandingPage JSONB (BrandingDataSeeder kiteclass-side) + FE section render bỏ empty-state khi đã có data. Cross-layer (BE JSONB shape ↔ FE section) — per `contract-first-for-cross-layer` cần api-contract/shape thống nhất.

## Acceptance Criteria

- [x] BrandingDataSeeder seed teachers[]/pricing/stats vào LandingPage JSONB 2 tenant
- [x] FE section render data thật, ẩn empty-state khi có data (PERSONAL_TEMPLATE enable `teachers`)
- [ ] G2 walk: landing Hà + Nhì hiện sections data thật (pending — human render walk trên Docker)

## Current State (verified 2026-06-11)

**CONTRACT (FE `(public)/page.tsx` = consumer canonical, JsonNode passthrough qua API):**
- `teachers` JSONB `[{name, subject, credentials[], photoUrl?}]` → SlotItem (title/description/items/image)
- `pricingTiers` JSONB `[{name, price, period, features[], highlighted?}]` → plans (description = `price / period`; FE auto-highlight giữa khi ≥3)
- `stats` JSONB `[{value, label}]` → (title=value, description=label)

**Shipped (BE):** `BrandingDataSeeder.seedTrioTenant` seed teachers/pricingTiers/stats cho Hà (a1100000) + Nhì (b1100000). Stats khớp academic core `DemoAcademicSeeder` (Hà 2 lớp/12 HV; Nhì 4 lớp/35 HV; chuyên cần = present%+late%). Idempotent (overwrite cột trên single landing row). Hà = 1 gói FREE; Nhì = 3 gói PAID (thesis §4.4). Avatar omit → TeachersSection fallback initials (no remote 404). `kiteclass-core compile` PASS.

**Shipped (FE):** PERSONAL_TEMPLATE (template Hà/Nhì) THIẾU `teachers` section (chỉ org template có) → seed teachers không render. Fix: enable `teachers` order 4 trong PERSONAL_TEMPLATE (backward-compat: null teachers → section tự ẩn). `stats`+`pricing` đã có sẵn trong personal template (không sửa).

**Pending (G2):** human render walk Docker — landing 2 tenant hiện 3 section data thật, không empty-state.

## Related

- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket E (🔨 Delta)
- Depends: [[GAP-1190]] (academic core data nguồn cho stats)
- Cross-layer: `contract-first-for-cross-layer` (LandingPage JSONB shape ↔ FE section)
