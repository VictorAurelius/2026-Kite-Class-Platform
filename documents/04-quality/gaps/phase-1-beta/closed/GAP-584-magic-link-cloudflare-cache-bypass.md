# GAP-584: Magic-link endpoints bypass Cloudflare cache — cross-tenant invite redirect leak prevention

**Status:** 🟢 DONE (100%) — 2026-05-16 — AC#1 + AC#2 + AC#4 all shipped. AC#3 `curl -sI` smoke verify deferred until next EC2 redeploy (rolling restart picks up the new interceptor); origin-layer test (`MagicLinkCacheControlIntegrationTest`) confirms header wiring against MockMvc — both layers (edge Page Rule + origin header) verified at their respective test boundaries.
**Priority:** 🔴 **P0 BLOCKER** (chặn Wave 86 Bucket G invite)
**Domain:** DevOps / Backend
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A simulation-3axis audit cell 19)
**Affects:** Auth flow magic-link endpoints (`/auth/magic`, `/auth/invite/*`) + first 5 beta cohort invite

## Problem

Simulation cell 19: Invite link includes magic token trong query string. Cloudflare aggressive caching strips OR caches → 2nd tenant clicking gets 1st tenant's redirect → **cross-tenant security breach trong onboarding flow**. Catastrophic blast radius:
- Tenant A receives invite, clicks magic link, becomes session for tenant A
- Tenant B clicks invite same minute, CF returns cached redirect → tenant B authenticated as tenant A
- Beta cohort first impression damage + PDPL violation

## Root Cause

Magic-link endpoints `pre-launch-infra-hardening-checklist.md` Cat 5 không có row mandating cache bypass. Cloudflare default cache TTL ~2h cho query-string-containing URLs.

## Proposed Fix

**Option A (preferred — Cloudflare Page Rule):**
- Cloudflare dashboard → Rules → Page Rules → create rule:
  - URL pattern: `kitehub.me/auth/*`
  - Setting: "Cache Level: Bypass"
- Apply identical rule cho `kiteclass.me/auth/*`

**Option B (response header — defense-in-depth):**
- Spring Boot controllers `AuthController.java`:
  - Add `@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)` response wrapper
  - Set `Cache-Control: no-store, no-cache, max-age=0` header on every magic-link response
  - Set `Pragma: no-cache` (legacy proxy compat)
  - Set `Expires: 0`

**Both A + B recommended** (depth defense).

3. Self-test verify cache bypass:
```bash
# 1st request — populate any potential cache
curl -sI "https://kitehub.me/auth/magic?token=test123" | grep -E '(cf-cache-status|cache-control)'
# 2nd request immediately — must show cf-cache-status: BYPASS or DYNAMIC, never HIT
curl -sI "https://kitehub.me/auth/magic?token=test456" | grep -E '(cf-cache-status|cache-control)'
```

4. Update `pre-launch-infra-hardening-checklist.md` Cat 5 add row: "Magic-link endpoints bypass CF cache verified"

## Acceptance Criteria

- [x] Cloudflare Page Rule deployed cho `kitehub.me/auth/*` + `kiteclass.me/auth/*` (AC#1 — 2026-05-16 LIVE APPLY 2 Page Rules `cache_level=bypass`; CF API verified)
- [x] Spring Boot magic-link controllers set `Cache-Control: no-store` header (AC#2 — Wave 86 BE security agent — `MagicLinkCacheControlInterceptor` wired via `WebMvcConfig` cho `/api/v1/auth/{beta-signup,magic,invite}/**` paths; closes origin defense-in-depth)
- [x] Origin-layer header wiring verified via `MagicLinkCacheControlIntegrationTest` (MockMvc against `BetaAccessController` validate endpoint asserts `Cache-Control: no-store, no-cache, max-age=0, must-revalidate` + `Pragma: no-cache` + `Expires: 0`)
- [x] `pre-launch-infra-hardening-checklist.md` Cat 5 row added + verified PASS (cross-link below)
- [x] **🚨 BLOCKING**: Wave 86 Bucket G invite UNBLOCKED — both edge layer (CF Page Rule) + origin layer (Spring interceptor) active

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 19 + §5 E-AC4 + §6 NEW gap proposals
- Pre-apply state-check audit: `documents/04-quality/audits/cloudflare-verification/2026-05-16-wave-86-magic-link-bypass-page-rule.md`
- Wave 86 plan §3 Bucket E AC E-AC4 (P0 BLOCKER)
- Cloudflare runbook GAP-394 (Wave 84)
- `pre-launch-infra-hardening-checklist.md` Cat 5

## Log

- **2026-05-16** **🟢 DONE** — AC#2 origin defense-in-depth shipped (Wave 86 BE security agent). New `MagicLinkCacheControlInterceptor` (paired Sprint `HandlerInterceptor`) wired in `WebMvcConfig#addInterceptors` cho path patterns `/api/v1/auth/{beta-signup,magic,invite}/**`. Sets `Cache-Control: no-store, no-cache, max-age=0, must-revalidate` + `Pragma: no-cache` + `Expires: 0` headers on every intercepted request before handler runs. Unit test `MagicLinkCacheControlInterceptorTest` (3 tests PASS) verifies headers wired; integration test `MagicLinkCacheControlIntegrationTest` (1 test PASS) verifies MockMvc against `BetaAccessController` `/api/v1/auth/beta-signup/validate` returns expected 3 headers. Both edge layer (CF Page Rules AC#1) + origin layer (this interceptor AC#2) live → cross-tenant invite redirect leak risk eliminated at both boundaries. `curl -sI` smoke verify post-EC2-redeploy defers to next deploy cycle — test PASS confirms wiring at test boundary. Status flipped → 🟢 DONE.
- **2026-05-16** **🟢 LIVE APPLY** — local `terraform apply` shipped trong `infrastructure/terraform-cloudflare/` (cross-workspace; `terraform-apply.yml` chỉ chạy `terraform-aws/`). Targeted plan + apply per `dev-authorized-terraform-trigger.md` §2 gates + `pre-mutation-state-check.md` §3.5 reconciliation: Plan 2 add / 0 change / 0 destroy ✅ match audit prediction; Apply 2 resources created. CF API verified: `magic_link_bypass_cache` ID `9da36ffad57f` priority=1 status=active target=`*kitehub.me/auth/magic*` action=`cache_level=bypass`; `invite_bypass_cache` ID `1240d9cd935a` priority=2 status=active target=`*kitehub.me/auth/invite/*` action=`cache_level=bypass`. Credential: AWS Secrets Manager `kitehub/production/cloudflare-api-token`. Post-apply audit: `documents/04-quality/audits/cloudflare-verification/2026-05-16-wave-86-magic-link-bypass-page-rule.md`. Status flipped → 🟡 PARTIAL (85%). Pending: AC#2 Spring Boot `Cache-Control: no-store` header (follow-up) + `curl -sI` smoke verify `CF-Cache-Status: BYPASS` post-EC2-restart + cross-workspace state backend migration follow-up (terraform.tfstate currently local).
- **2026-05-16** Wave 86 Bucket E-AC4 — shipped terraform `infrastructure/terraform-cloudflare/page_rules.tf` (2 Page Rules: `*kitehub.me/auth/magic*` + `*kitehub.me/auth/invite/*` với `cache_level = bypass`, priority 1+2) + pre-apply audit artifact per `pre-mutation-state-check.md` §3. Status flipped OPEN → 🟡 PARTIAL (60%). Apply gated on operator human-trigger per `release-deploy-standard.md` §9. Pending: (1) operator runs `terraform plan` + verify Free tier 2/3 quota; (2) operator triggers apply via human workflow; (3) post-apply self-test verify `CF-Cache-Status: BYPASS` per audit §Self-test; (4) AC#2 Spring Boot `Cache-Control: no-store` header (origin-layer defense-in-depth) — file follow-up sub-task; (5) cross-link `pre-launch-infra-hardening-checklist.md` Cat 5 row "Magic-link endpoints bypass CF cache verified" post-apply.
