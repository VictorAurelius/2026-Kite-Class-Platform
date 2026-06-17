# GAP-1481: Wildcard tenant subdomain `*.kitehub.me` routing chưa hoạt động

**Status:** 🟠 PARTIAL
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-18 (Bước 2 deploy smoke — `co-ha-toan.kitehub.me` → 000)
**Affects:** Tenant landing per-subdomain (kiến trúc `tenant-domain-landing-architecture.md` §2)

## Problem

Kiến trúc tenant landing (`documents/02-architecture/tenant-domain-landing-architecture.md` §2) ghi DNS `*.kitehub.me` wildcard = ✅, nhưng smoke 2026-06-18 cho thấy `https://co-ha-toan.kitehub.me/` → **000** (không có DNS). FE middleware Host→tenant (GAP-811/1077) + gateway TenantResolver đã ship; thiếu 2 mảnh ở tầng ingress:

1. **nginx (`infrastructure/fe-host/nginx-fe.conf`):** chỉ có server block exact cho apex/www/app/api → tenant subdomain rơi vào `default_server _` → trả **444 (reject)**. Không có block cho `*.kitehub.me`.
2. **Cloudflare DNS:** không có record `*.kitehub.me` (apex + app quản lý manual qua CF dashboard per `dns.tf` line 114). Subdomain tuỳ ý không resolve.

Cert wildcard `*.kitehub.me` (Let's Encrypt DNS-01, GAP-567) đã có sẵn trên nginx → không cần cấp cert mới.

## Proposed Fix

1. nginx: thêm server block regex `server_name ~^(?<tenant>[a-z0-9][a-z0-9-]*)\.kitehub\.me$` → proxy `kiteclass_frontend` (mirror block 3 app.kitehub.me). nginx precedence: exact (apex/www/app/api) match trước → regex chỉ bắt tenant slug. (✅ shipped this PR)
2. Cloudflare DNS: thêm record `*.kitehub.me` → kc_app_fe (proxied, mirror app/api). (deploy via CF API this session)
3. Deploy nginx reload trên kc_app_fe + verify `co-ha-toan.kitehub.me` → 200 + render landing tenant Hà.

## Acceptance Criteria

- [ ] `https://{slug}.kitehub.me/` (tenant thật, vd co-ha-toan) → 200 + landing tenant đó
- [ ] Host lạ (non-kitehub.me) vẫn → 444 (default_server giữ nguyên)
- [ ] `app`/`api`/`www`/apex routing không đổi (exact precedence)

## Related

- Discovered in: PR #2490 deploy session 2026-06-18
- Design: `documents/02-architecture/tenant-domain-landing-architecture.md` §2-§4
- Sister: GAP-811 / GAP-1077 (FE middleware Host resolution, shipped), GAP-812 (custom domain)
