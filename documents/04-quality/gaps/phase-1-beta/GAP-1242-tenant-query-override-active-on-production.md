# GAP-1242: `?tenant=` query override hoạt động cả trên production — brand-confusion risk

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-12 (G2★ walk branding-100 — user hỏi "?tenant= là tính năng gì, production thì sao")
**Affects:** `kiteclass-frontend/src/middleware.ts` `extractSlug()` — ưu tiên `?tenant=` TRƯỚC Host mọi môi trường

## Problem

`?tenant=<slug>` là dev/preview override (local không có wildcard DNS). Middleware không gate
theo môi trường → trên production, `https://sky-education.kitehub.me/?tenant=tenant-khac`
render landing của tenant KHÁC dưới subdomain của Sky. Không lộ dữ liệu (landing public)
nhưng là brand-confusion / phishing-nhẹ risk (URL trường A hiển thị nội dung trường B).

## Proposed Fix

Production (`NODE_ENV=production`): chỉ honor `?tenant=` khi Host KHÔNG phải tenant subdomain
(apex/localhost/IP); Host đã resolve ra tenant → bỏ qua query override. Giữ nguyên local/dev.

## Acceptance Criteria

- [ ] Production: `sky.kitehub.me/?tenant=khac` render landing CỦA SKY (override bị bỏ qua)
- [ ] Production: apex/`localhost` + `?tenant=` vẫn hoạt động (preview path)
- [ ] Local dev: hành vi không đổi
- [ ] Middleware tests cover 3 case trên

## Related

- Discovered in: G2★ walk branding-100 2026-06-12 (user question)
- GAP-811 (host→tenant middleware) · `g1-browser-walk-before-flip.md` §3.1 (`?tenant=` banned as evidence)
