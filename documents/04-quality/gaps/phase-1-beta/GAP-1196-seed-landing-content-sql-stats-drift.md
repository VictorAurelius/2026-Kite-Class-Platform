---
id: GAP-1196
title: seed-landing-content.sql teachers/pricing/stats drift vs BrandingDataSeeder canonical (marketing stats ≠ academic-accurate)
status: OPEN
priority: P3
domain: Backend
phase: phase-1-beta
created: 2026-06-11
last_verified: 2026-06-11
completion_pct: 0
---

# GAP-1196 — seed-landing-content.sql landing-sections drift vs BrandingDataSeeder

## Problem

Discovered khi implement GAP-1194 (Bucket E landing sections): `kitehub/scripts/seed-landing-content.sql` đã seed CÙNG teachers/pricing/stats cho Hà/Nhì/Khánh nhưng:
1. Là **manual one-off SQL** (chạy tay), trong khi `BrandingDataSeeder.java` (dev `@Profile`) auto-seed mỗi boot → giờ là canonical dev path.
2. Stats trong SQL dùng số **marketing** (`500+` học viên, `95%`) **KHÁC** giá trị academic-accurate của BrandingDataSeeder + DemoAcademicSeeder (Hà 12 HV/85%, Nhì 35 HV/94% — khớp data lớp/HV/điểm danh thật).

→ Drift risk: 2 nguồn seed cùng landing sections với giá trị mâu thuẫn; người chạy SQL sau seeder sẽ overwrite bằng số marketing bịa (vi phạm anti-fabrication GAP-958).

## Proposed Fix

Chốt 1 nguồn canonical (BrandingDataSeeder auto-path) cho landing sections dev. Hoặc: (a) gỡ teachers/pricing/stats section khỏi seed-landing-content.sql (để BrandingDataSeeder lo), HOẶC (b) sync giá trị SQL khớp academic-accurate. SQL giữ phần không trùng (hero/branding/template_type).

## Acceptance Criteria

- [ ] Chốt canonical source cho landing sections dev (seeder vs SQL); document.
- [ ] seed-landing-content.sql không còn stats marketing mâu thuẫn academic-accurate.
- [ ] Chạy SQL sau seeder không overwrite sections bằng giá trị bịa.

## Related

- Discovered in: GAP-1194 Bucket E implementation 2026-06-11 (cross-layer contract investigate)
- [[GAP-1194]] landing sections seed (BrandingDataSeeder canonical)
- [[GAP-958]] anti-fabrication (no fake marketing stats)
- Code: `kitehub/scripts/seed-landing-content.sql` (teachers/pricing/stats UPDATE) vs `BrandingDataSeeder.java` (seedTrioTenant sections)
