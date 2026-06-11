---
audience: mixed
date: 2026-06-11
session: landing-100 G2★ fix-pack (3 vòng) + branding-100 plan
---

# Session handoff 2026-06-11 — landing-100 fix-pack + branding-100

## Shipped (PR #2326 — OPEN, CI green các vòng, CHƯA merge)

Branch `wave/landing-100-g2-walk-2026-06-11` (worktree `../kite-wt-landing-g2`), ~25 commits:
- **Code:** middleware suspended-loop + not-found page + F-sections hide-when-empty + hero khung phải + multi-banner 3 lớp (V96 heroImages + HeroBannerCarousel + settings UI + upload endpoint) + personal voice headings + port 4 trang public theo kit + same-host public client (GAP-1207) + gateway BASE_DOMAIN nip.io + seeder upsert/publish/cover/FAQ/testimonials + zebra skip-empty + container 1180px + drift-gate sibling-install.
- **Design:** kit `landing-personal` 113/128 + kit `kiteclass-public` 4 screens 110-115/128 (đều Be Vietnam Pro sau GAP-1223) + marketing-site README sửa surface KITEHUB (GAP-1227).
- **META:** `wave-closure-scope-completeness` v1.1.0→**v1.2.0** (quality-target wave: fix hết gap KHÔNG KỂ PHASE, chỉ trừ PENDING external-blocked + creation-time gate) + wave-pack-planner section + matrix row.
- **Gaps:** 25 xử lý hôm nay — DONE: 138, 826, 1083, 1194-1196, 1199-1206 (1201/1202 scripts), 1207, 1209, 1210, 1220, 1223-1227. PARTIAL pending-human: 1077 (90%), 1082, 1211. OPEN: 1221 (BE email optional — user chưa chốt fix/defer), 1222 (gradeLevel — đề xuất defer), 1213-1219 (branding-100 scope).
- **Re-score landing:** 81 → **~89-90/100** post-fix (addendum trong `audits/ui-review/2026-06-11-landing-100-production-rescore.md`); sky ~85+ (stats cố tình không seed — anti-fabrication).

## Wave states

- **landing-100**: `in-progress` (re-open theo v1.2.0). Flip `complete` cần: (1) user chốt GAP-1221 fix / GAP-1222 defer-trailer, (2) **human G2★ walk** per recipe `documents/05-guides/operations/2026-06-11-g2-recipe-landing-100-nipio-subdomain.md` (stack local đang chạy đúng bản branch; 12 container healthy).
- **branding-100**: plan `draft` (`waves/wave-2026-06-11-branding-100.md`) — 3 audit artifacts (persona 12 findings / benchmark 7 sp / failure-mode 23/42 — **deploy wizard = MOCK**, GAP-1213 P0) + 14 gaps scope + gate v1.2.0 declared-at-creation. Execute sau landing-100.

## Pickup next session

1. User trả lời (a)/(b) GAP-1221/1222 → fix hoặc trailer → human walk → flip landing-100 + GAP-1077 DONE + wave-history sync + merge PR #2326 (squash) → Pages tự deploy kits.
2. Sau merge: rebuild từ main, prune worktree (`bash scripts/prune-merged-worktrees.sh --yes`).
3. branding-100 execute (bucket A kit v3 trước — design đi trước).
4. Server preview kits còn chạy: `localhost:8090` (kill: `pkill -f "http.server 8090"`).

## Notes

- ECR docker-build-push main: 2 fail sáng = transient GH cache, rerun success. Self-hosted runner hôm nay flaky mạng (Maven DNS / docker registry / psql socket) — rerun là đủ; GAP-1220 đã vá class stale-.m2.
- Session locks: lock phiên này đã gỡ; reserved IDs dùng hết tới GAP-1227.
