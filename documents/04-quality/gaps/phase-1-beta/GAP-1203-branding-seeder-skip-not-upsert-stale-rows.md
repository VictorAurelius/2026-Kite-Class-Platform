# GAP-1203: BrandingDataSeeder idempotent-skip để lại rows cũ khi seed data đổi (hero .png → .webp)

**Status:** 🟡 PARTIAL
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-11 (landing-100 G2★ nip.io walk — hero bg render gradient thay vì AI-scene)
**Affects:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/dev/seeder/BrandingDataSeeder.java` + mọi env đã seed trước khi seed-constants đổi

## Problem

Walk 2026-06-11: hero 2 tenant demo-trio render gradient fallback thay vì ảnh AI-scene. DB `landing_pages.hero_image_url` = `/demo-banners/co-ha-toan.png` (404) trong khi asset thật + seeder code hiện tại đều là `.webp`. Nguyên nhân: seeder idempotent theo kiểu **skip-if-exists** ("already exists") — khi constants trong seeder đổi (.png → .webp ở commit trước), rows đã seed KHÔNG được reconcile → stale data sống mãi ở mọi env từng chạy seeder bản cũ. Redis cache `landingPages::{id}` còn che thêm 1 lớp (per `tenant-domain-landing-architecture.md` §6).

## Workaround applied (local, 2026-06-11)

```sql
UPDATE landing_pages SET hero_image_url = REPLACE(hero_image_url, '.png', '.webp')
WHERE hero_image_url LIKE '/demo-banners/%.png';
-- + redis-cli DEL landingPages::a1100000-... landingPages::b1100000-...
```

Verify: banner asset 200 image/webp + screenshot hero AI-scene render đúng.

## Proposed Fix (root)

Seeder demo-trio chuyển skip-if-exists → **upsert có chủ đích cho demo rows** (match theo instance_id cố định, update các cột content nếu khác constants) + evict Redis key sau update. Chỉ áp dụng cho demo-trio fixed UUIDs — không đụng rows user tạo.

## Acceptance Criteria

- [ ] Seeder re-run trên DB có rows cũ → rows phản ánh constants hiện tại
- [ ] Redis landingPages keys evicted sau upsert
- [x] Local DB đã fix tay: hero_image_url .png→.webp + logo_url co-ha/nhi (rỗng → /demo-banners/*-logo.webp) + redis DEL (workaround — walk unblocked)

## Related

- Discovered in: landing-100 G2★ walk (PR #2326 session)
- Sister: GAP-1036 (ensure-bucket already-exists pattern), design doc §6 Redis cache ops note
