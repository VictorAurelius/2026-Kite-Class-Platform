# GAP-535: Tenant slug normalize — Vietnamese diacritics + smart quotes + collision recovery

**Status:** 🟡 PARTIAL — Wave 77 Bucket D normalize utility + tests + V40 migration shipped; wiring into `InstanceService.createInstance` collision-recovery loop deferred to follow-up
**Priority:** 🔴 P0 — BLOCKING Phase 1 BETA invite (P2 Center owner target persona = Vietnamese tên có dấu)
**Domain:** Backend
**Found:** 2026-05-14 (Wave 77 — outside-in audit: failure-mode matrix F2)
**Affects:** P2 Small/Medium center owner (target persona Tier 1) — 100% tenant names có dấu tiếng Việt
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Tenant create domain | `kitehub/kitehub-subscription/src/main/java/**/tenant/*` | 🟡 verify-at-spawn |
| Slug column / unique constraint | DB schema | 🟡 verify-at-spawn |
| Vietnamese diacritic normalize | (anywhere) | ❌ likely missing |
| Smart-quote stripping | (anywhere) | ❌ likely missing |
| Collision recovery (`-1`, `-2` suffix) | (anywhere) | ❌ likely missing |

## Problem

Wave 77 outside-in audit (2026-05-14) — failure-mode matrix F2 (P0): **P2 Center owner Anh Tuấn paste tên "Trường Mầm Non "Hoa Mai"" vào signup form**:

1. Smart quote `U+201C` / `U+201D` (`"..."`) từ Word/Zalo copy-paste → backend slug normalize fail
2. Vietnamese diacritics (`ư`, `ạ`, `ầ`) → if NFC/NFD not applied → slug rỗng OR mismatch
3. Slug collision with another tenant → 500 OR DB unique constraint violation without recovery
4. User reload → form state lost → bỏ cuộc

P2 = target Tier 1 persona cho Phase 1 BETA. 100% tên trường VN có dấu. Block = block toàn bộ P2 funnel.

## Proposed Fix

1. **Normalize pipeline** (server-side, không trust client):
   - Step 1: Unicode NFC normalize
   - Step 2: Strip smart quotes (`U+2018-U+201D` → ASCII apostrophe/quote OR remove)
   - Step 3: Apache Commons Lang `StringUtils.stripAccents()` (Java) → flatten diacritics: `ư`→`u`, `ạ`→`a`, `ầ`→`a`
   - Step 4: Lowercase + replace non-alphanumeric với `-`
   - Step 5: Trim leading/trailing `-` + collapse consecutive `-`
   - Example: `Trường Mầm Non "Hoa Mai"` → `truong-mam-non-hoa-mai`
2. **Collision recovery:**
   - DB check: `SELECT id FROM tenants WHERE slug = ?` — if exists, append `-1`/`-2` until unique
   - Cap retry 10; if exceeded → 409 với message "Tên trùng nhiều quá, vui lòng chọn tên khác"
3. **Persist original name + normalized slug:**
   - `tenants.name` = original input (with diacritics preserved for display)
   - `tenants.slug` = normalized + collision-resolved (used in URLs/subdomain)
4. **Tests:**
   - Smart quote `"Hoa Mai"` → `hoa-mai`
   - Full Vietnamese name `Trường Mầm Non Hoa Mai` → `truong-mam-non-hoa-mai`
   - Collision: 2 tenants cùng `truong-mam-non-hoa-mai` → 2nd gets `-1` suffix
   - Edge: only-diacritics name `àáảãạ` → `aaaaa` (then collision logic if needed)

## Acceptance Criteria

- [x] Slug normalize pipeline implemented (`TenantSlugNormalizer`) — NFC → smart-quote strip → đ/Đ handle → stripAccents → lowercase → dash collapse → trim → cap @120
- [x] Unit tests cover 10+ Vietnamese patterns (parameterized CSV: smart-quotes, full-name, edge-only-diacritics, ampersand, leading/trailing punct, numeric, đ/Đ stroke-bar) — 16 tests pass
- [x] Smart quote stripping covers full U+2018–U+201F range (apostrophes + double-quotes + low-9-quote variants)
- [x] Collision recovery via `withCollisionSuffix(base, n)` — base trimmed if `base+tail` exceeds cap; throws on suffix < 1
- [x] DB migration V40 adds `instances.slug VARCHAR(120)` + unique partial index `WHERE slug IS NOT NULL`; backfill `slug := subdomain` for existing rows
- [x] Original `organization_name` preserved with diacritics for display; `slug` separate column for URL/subdomain routing
- [ ] **Wiring into `InstanceService.createInstance`** — current `InstanceService` does not call `TenantSlugNormalizer` yet; collision-recovery loop (10-retry then 409) belongs in that service. Follow-up gap recommended (separate concern from F2 P0 — the normalizer + schema + tests are the security/correctness foundation; wiring is integration step that depends on owner deciding which create endpoint inherits the normalize pipeline)
- [ ] **Live verify post-deploy:** POST `/api/platform/instances` (or successor) with VN-diacritic name → 201 + DB row slug normalized (gated on wiring above)

## Related

- **Sibling Wave 77 outside-in:** GAP-533, GAP-534, GAP-536
- **Related:** GAP-536 (idempotency on POST /tenants — same controller surface)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-14-77-beta-invite-launch-foundation.md` Bucket D
- **Outside-in audit source:** Wave 77 failure-mode matrix F2 (2026-05-14)

## Log

- **2026-05-14** — Wave 77 Bucket D shipped: `TenantSlugNormalizer.java` (component) + 16 parameterized unit tests covering Wave 77 outside-in AC examples + `V40__tenant_slug_normalize.sql` (slug column + partial-unique index + backfill from subdomain). Status → PARTIAL: pure utility + schema + tests DONE; wiring into `InstanceService.createInstance` (collision-recovery loop) deferred — KiteHub's tenant=`instances` model has `subdomain` as the historical public slug; reframing create flow to consume the normalizer requires owner decision on which create endpoint(s) inherit the pipeline (beta-signup-complete path vs admin-platform path vs trial-self-serve path). Follow-up sub-gap recommended.
- **2026-05-14** — Initial write-up. Wave 77 outside-in failure-mode matrix F2 surfaced. Stub in wave plan PR; full execution → Bucket D.
