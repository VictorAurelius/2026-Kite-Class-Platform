# GAP-1235: Landing production — avatar GV là vòng tròn chữ cái thay vì chân dung thật (kit dùng ảnh gốc)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-12 (visual check production vs kit — screenshot 3 tenant demo-trio)
**Affects:** Section "Giáo viên đồng hành / Đội ngũ giáo viên" — cả 3 tenant (NTH / NĐN / ĐLK initials)

## Problem

Screenshot stack local 2026-06-12 (co-ha-toan / thay-nhi-hoa / sky-education): section giáo viên render vòng tròn initials thay vì ảnh chân dung. Kit `landing-personal` (113/128) dùng chân dung ảnh gốc 800w (GAP-1232). Đây là delta parity lộ nhất còn lại sau fix-pack #2326.

Hạ tầng ĐÃ sẵn: FE `page.tsx` map `teachers[].photoUrl?` → TeachersSection; ảnh thật có tại `ui_kits/_shared/assets/portraits/{ha,nhi,khanh}.webp`; pattern upload/serve ảnh đã có từ heroImages (V96 + GAP-826). CHỈ thiếu: seed `photoUrl` cho teachers JSONB của demo-trio.

## Proposed Fix

Extend seeder demo-trio (Bucket G `seed-landing-content` path): upload 3 chân dung thật → set `teachers[].photoUrl` per tenant (Hà/Nhì/Khánh — khớp persona). Ảnh thật từ thesis portraits, KHÔNG bịa (anti-fabrication OK — ảnh người thật của chính GV demo).

## Acceptance Criteria

- [ ] 3 tenant demo-trio: section giáo viên hiện chân dung thật (không initials)
- [ ] Ước +1-2 điểm parity (rescore addendum)

## Related

- GAP-1232 (banner + portrait thật vào kit — nguồn ảnh) · GAP-826 (heroImages 3 lớp — pattern serve) · rescore 2026-06-11 §4 delta table · landing-100 path-to-90
