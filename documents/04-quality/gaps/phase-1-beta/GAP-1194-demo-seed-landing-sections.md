---
id: GAP-1194
title: Seed landing sections (teachers[] / pricing / stats) lấp empty-state khi đã có data thật
status: OPEN
priority: P2
domain: Mixed
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 0
---

# GAP-1194 — Seed landing sections (teachers / pricing / stats)

## Problem

Landing 2 tenant demo còn empty-state cho sections teachers[] / pricing / stats vì phụ thuộc onboarding manual. Khi academic core đã seed (GAP-1190..1193), nên lấp landing sections bằng data thật thay vì empty-state. Wave plan Bucket E (chưa làm).

## Proposed Fix

Seed `teachers[]` + `pricing` + `stats` vào LandingPage JSONB (BrandingDataSeeder kiteclass-side) + FE section render bỏ empty-state khi đã có data. Cross-layer (BE JSONB shape ↔ FE section) — per `contract-first-for-cross-layer` cần api-contract/shape thống nhất.

## Acceptance Criteria

- [ ] BrandingDataSeeder seed teachers[]/pricing/stats vào LandingPage JSONB 2 tenant
- [ ] FE section render data thật, ẩn empty-state khi có data
- [ ] G2 walk: landing Hà + Nhì hiện sections data thật

## Related

- Wave: `wave-2026-06-11-demo-seed-1-2tenant-full.md` Bucket E (🔨 Delta)
- Depends: [[GAP-1190]] (academic core data nguồn cho stats)
- Cross-layer: `contract-first-for-cross-layer` (LandingPage JSONB shape ↔ FE section)
