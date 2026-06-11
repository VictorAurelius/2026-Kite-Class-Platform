# GAP-1228: Tier-name drift ở CODE — "PRO" trong branding/FE-wizard vs canonical BASIC → BASIC tenant rơi nhầm quota FREE

**Status:** 🟢 DONE 2026-06-11 — Wave ui-kits-100 Bucket H (coordinator inline)
**Priority:** 🟠 P1
**Domain:** Mixed (Backend kitehub-branding + Frontend kiteclass wizard + DB CHECK + docs contract)
**Found:** 2026-06-11 (Wave ui-kits-100 Bucket E — sweep GAP-1098 cross-check code phát hiện drift KHÔNG chỉ docs)
**Affects:** kitehub-branding quota/queue, kiteclass-frontend branding wizard, V59 CHECK constraint, 4 docs contract

## Problem

Canonical tier = `PricingTier.java` (FREE/BASIC/PREMIUM/ENTERPRISE). JWT tier claim phát từ `TokenService.resolveTierForRole()` = `Instance.getTier().name()` → giá trị **"BASIC"** cho tenant BASIC. Nhưng chuỗi tiêu thụ còn dùng tên cũ **"PRO"**:

1. **`RegenerateQuotaService.limitFor()`** (`kitehub-branding/.../wizard/service/RegenerateQuotaService.java:78`): `case "PRO" -> proLimit` — KHÔNG có case "BASIC" → **BASIC tenant rơi `default -> freeLimit`** (quota 3 thay vì 10). Line 220 `case "PRO","PREMIUM","ENTERPRISE" -> upper` cùng lỗi.
2. **FE wizard** `kiteclass-frontend/src/components/branding/wizard/types.ts:22`: `export type Tier = 'FREE' | 'PRO' | 'PREMIUM' | 'ENTERPRISE'` — thiếu BASIC; tests dùng `initialState('PRO')`.
3. **`AIJobPriority.java:63`** đã dual-accept `case "PREMIUM","BASIC","PRO" -> PRO` (transitional shim) — chứng tỏ drift đã biết một phần nhưng chưa unify.
4. **DB CHECK** `chk_branding_regen_tier` (V59): `tier IN ('FREE','PRO','PREMIUM','ENTERPRISE')` — insert tier "BASIC" sẽ **vi phạm CHECK** nếu service snapshot tier từ header.
5. **Docs contract phản ánh code cũ** (EXEMPT khỏi docs-sweep GAP-1098, phải đổi CÙNG code per Living Docs): `01-business/kitehub/ai-branding/api-contract.md` (header `X-Subscription-Tier: FREE|PRO|...`), `01-business/kiteclass/branding-wizard/rules.md` (`wizard.tier PRO`, limits PRO=10), `01-business/kiteclass/ai-agent-workflow/rules.md` (config keys `ai.queue.*.pro` + `AIJobPriority.PRO`), `02-architecture/database/kitehub/03-branding.md` (V59 CHECK + tier enum rows).

## Root Cause

Tier rename PRO→BASIC chỉ đổi `PricingTier` enum (platform) — chuỗi branding service / FE wizard / DB CHECK / config keys viết trước rename chưa sweep. Same class GAP-1098 nhưng ở CODE layer (1098 scope docs/skills only).

## Proposed Fix

1 PR unify (doc + code cùng PR): `RegenerateQuotaService` case "BASIC" (giữ "PRO" alias 1 release nếu cần backward-compat JWT cũ) + FE `types.ts` Tier thêm 'BASIC' (migrate dần khỏi 'PRO') + migration mới mở rộng `chk_branding_regen_tier` thêm 'BASIC' + đổi 4 docs contract khớp + cân nhắc rename config keys `ai.queue.*.pro` → `.basic` (hoặc document alias). `AIJobPriority` giữ dual-accept đến khi JWT cũ hết hạn.

## Acceptance Criteria

- [x] BASIC tenant nhận đúng quota (limitFor BASIC=10 + canonicalTier PRO→BASIC; unit RegenerateQuotaServiceTest 22/22 PASS — IT full-stack header verify theo walk GAP-1020 chain sẵn có)
- [x] V72__regen_tier_check_basic_canonical.sql — migrate rows PRO→BASIC + CHECK canonical 4 tier
- [x] FE Tier type +BASIC (giữ 'PRO' alias); wizard tests 30/30 PASS + next build compiled
- [x] 4 docs contract sync cùng PR (api-contract header+tier samples / branding-wizard rules / ai-agent-workflow queue-group annotate / 03-branding V72 rows)
- [x] Cross-flow sweep: residual "PRO" = alias-compat tests (giữ chủ đích) + AIJobPriority queue-group nội bộ (EXEMPT, annotated)

## Related

- GAP-1098 (docs-sweep sibling — DONE Wave ui-kits-100 Bucket E; code-contract docs EXEMPT chuyển sang gap này)
- GAP-1020 (tier header trust — TokenService claim đã ship), GAP-1089 (tier entitlement core, phase-1.5), GAP-1078 (tier→provider routing)
- Discovered in: Wave ui-kits-100 Bucket E PR (sweep cross-check `RegenerateQuotaService` vs `PricingTier`)

## Log

- **2026-06-11 (DONE — Wave ui-kits-100 Bucket H, coordinator inline):** RegenerateQuotaService case "BASIC","PRO"→proLimit + canonicalTier PRO/BASIC→"BASIC" (persist canonical); V72 migrate data + CHECK canonical; FE Tier type +BASIC; 4 docs contract sync. Verify: BE 22/22 (RegenerateQuotaServiceTest+AIJobConsumerTest+BrandingWizardControllerTest) + FE wizard 30/30 + `next build` compiled. AIJobPriority dual-accept giữ nguyên (queue-group nội bộ).
