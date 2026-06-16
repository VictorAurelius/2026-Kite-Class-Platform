# GAP-1456: Remediate ~34 G2-recipe KC tenant-URL access-mode violations (localhost:3000 → nip.io subdomain)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Docs
**Found:** 2026-06-16 (detector ship — `g1-browser-walk-before-flip.md` §3.1/§3.2 recurrence #2)
**Affects:** ~10 G2-recipe MD `documents/05-guides/operations/*g2-recipe*.md` (KC-1, KC-8, KC-enroll, LMS, RBAC, SSO, branding-100) + possibly flow wave plans

## Problem

Detector `scripts/check-walk-recipe-access-mode.sh` (shipped 2026-06-16) surfaced **~34 violations** trong G2-recipe corpus: KC tenant-flow URL dùng bare `http://localhost:3000/...` (1 cái `?tenant=sky-education`) thay vì production-accurate subdomain `http://<slug>.127.0.0.1.nip.io:3000/...` per `g1-browser-walk-before-flip.md` §3.1/§3.2.

KC là multi-tenant resolve qua Host subdomain → walk tại bare `localhost:3000` đi nhánh dev pass-through (NEXT_PUBLIC_TENANT_ID default tenant), **BYPASS** Host-resolution path → G2 PASS tại đó KHÔNG chứng minh production access-mode hoạt động (chính là gap GAP-811 / GAP-1067/1068 từng gặp).

Đây là systemic — meta rule tồn tại (§3.1 từ 2026-06-08) nhưng recipe vẫn default localhost:3000 vì detector bị defer. Detector giờ đã ship (WARN) → grandfather corpus cũ, remediate qua gap này.

## Proposed Fix

Per recipe (theo flow KC): thay bare `localhost:3000` tenant-URL → `<slug>.127.0.0.1.nip.io:3000` với slug tenant đúng của recipe đó (vd `sky-education` / `g2-test-center-5`); xóa `?tenant=` (branding-100 line 41). Một số dùng localhost cho mục đích KHÔNG-tenant-resolution (vd session-isolation observe localStorage per-origin) → đánh giá từng cái, giữ + note exempt nếu hợp lệ.

## Acceptance Criteria
- [ ] `bash scripts/check-walk-recipe-access-mode.sh` → 0 violation (hoặc còn lại đều documented-exempt inline)
- [ ] Mỗi recipe KC dùng nip.io subdomain đúng slug; không còn `?tenant=` làm walk evidence
- [ ] (sau remediate) cân nhắc flip detector CI WARN → `--strict` (blocking)

## Related
- Rule: `g1-browser-walk-before-flip.md` §3.1/§3.2 (detector recurrence #2)
- Detector: `scripts/check-walk-recipe-access-mode.sh` + `quality-docs.yml` job `walk-recipe-access-mode`
- Sibling incidents: GAP-811 (`?tenant=` slip) + GAP-1067/1068 (KC-1 G2 host-resolution)
- Found while: Phase-3 consolidated recipe localhost:3000 slip (this session)
