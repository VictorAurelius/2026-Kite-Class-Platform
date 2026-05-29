---
audience: dev
title: Session handoff 2026-05-29 — demo landing banner + tenant→domain→landing initiative
created: 2026-05-29
---

# Session handoff 2026-05-29 — demo landing + domain→landing initiative

## Scope shipped

3 PR + 1 còn CI:

| PR | Trạng thái | Nội dung |
|---|---|---|
| #1965 | MERGED | demo-trio CI fixes (checkstyle + Grade triad + LandingPageServiceTest mock) |
| #1966 | MERGED | **GAP-810 Phase A** — banner-style hero landing (slots wiring fix + HeroSection 2-col + composer code `compose-sky-demo-banner.mjs` + BE `seedSkyLanding`). RST walk PASS 3-layer. |
| #1967 | MERGED | **4 gap initiative** GAP-811/812/813/814 + doc kiến trúc mới `documents/02-architecture/tenant-domain-landing-architecture.md` |
| #1968 | **OPEN — chờ CI** | sync 5 arch docs (README + kiteclass-architecture + multi-tenant + ssl-automation + domain-management) → merge khi xanh |

## Open items (next session)

1. **Merge #1968** (docs-only) khi CI xanh — `gh pr merge 1968 --squash --delete-branch`.
2. **Implement initiative tenant→domain→landing** theo thứ tự: **GAP-814** (P0 security host-spoofing) → GAP-813 (base-domain reconcile + by-subdomain endpoint) → GAP-811 (FE middleware) → GAP-812 (custom domain DNS/SSL).
3. **GAP-810 Phase B** (defer): teachers[] DB table cho Teachers section + OG per-tenant.

## Findings cần nhớ (đã ghi trong gaps/docs, không lặp lại chi tiết)

- **GAP-814 P0:** gateway chưa strip client `X-Tenant-Id` → cross-tenant IDOR (failure-mode audit bắt; inside-out designs bỏ sót). Ưu tiên cao nhất.
- **Redis `@Cacheable("landingPages")`:** update landing KHÔNG phản ánh tới khi `redis-cli DEL landingPages::{tenantId}` (restart core không clear).
- **FE 1-tenant-per-deploy:** demo dùng `?tenant=` dev-path; production-host multi-tenant chưa wire (GAP-811).
- **Custom domain:** entity + ssl-automation.md design đúng; code `checkDnsTxtRecord` stub + no SSL provisioning (GAP-812).

## Asset state (local-only, gitignored per rule)

- `kiteclass-frontend/public/demo/sky/`: `hero-banner.webp` (composer output) + `teacher-do-lan-khanh.webp`. Regen banner: `node kiteclass/kiteclass-frontend/scripts/compose-sky-demo-banner.mjs`.
- Demo seed cần `redis-cli DEL landingPages::e8ff87e1...` + DB landing_pages row (seeder `seedSkyLanding` set khi core dev restart).

## AI Branding re-scope (Phase 2 idea — chưa file gap)

Banner composer chứng minh hướng **template-composer** (form → parameterized HTML → headless render → PNG) tốt hơn AI image-gen cho asset có chữ Việt. Cân nhắc re-scope GAP-003 + ADR Phase 2.
