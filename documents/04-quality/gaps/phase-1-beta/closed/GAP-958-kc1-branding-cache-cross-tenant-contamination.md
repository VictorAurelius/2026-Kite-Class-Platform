# GAP-958: Branding cache contamination across tenants — cross-tenant data leak risk

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Multi-tenant branding) — Owner trust + cross-tenant isolation
**Defer-to:** After Wave flow-kh3 finish

## Problem

BR-SET-13 `theme_config_json`, BR-INST-004 `brandingVersion` increments on DEPLOY. Nếu FE cache (Next.js ISR / SWR) keyed bằng `branding-key` mà KHÔNG include tenant slug → tenant B mượn theme tenant A. Linh tạo tenant B với primary-color `#FF0000` (đỏ — tone tiếng Anh trẻ em). Refresh tenant A → A bị thay theme đỏ vì cache hit. Owner mất uy tín với staff. Surfaced: persona Finding 2.2.

## Proposed Fix

Audit FE cache keys + BE `@Cacheable.*branding` annotations — verify mọi key include `tenantSlug` HOẶC `instanceId`. CacheKeyGenerator override nếu cần. Test fixture: provision 2 tenants với khác theme → load A → load B → reload A → A theme intact.

## Acceptance Criteria

- [ ] `grep -rn "@Cacheable.*branding\|CacheKeyGenerator" kiteclass/kiteclass-core/src/main/java` keys include tenantSlug/instanceId
- [ ] `grep -rn "branding.*cache\|themeConfig.*ISR\|revalidate.*branding" kiteclass/kiteclass-frontend/src` FE cache scoped per tenant
- [ ] E2E test 2-tenant cache isolation PASS

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 2.2
- Sister: GAP-944 (cross-module payment cache invalidation pattern)
- Flow Verification Campaign §4 row KC-1

## Log
- **2026-06-09 DONE:** Wave landing-100 shipped (bucket 958) — G1-headless verified (FE build green + curl render 200 + ?tenant= data-binding proven). Full browser-G2 + subdomain resolution gated GAP-811/1077; BE per-tenant fields GAP-1083.
