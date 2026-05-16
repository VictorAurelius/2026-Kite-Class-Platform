# GAP-584: Magic-link endpoints bypass Cloudflare cache — cross-tenant invite redirect leak prevention

**Status:** 🟡 PARTIAL (60%) — terraform Page Rules + pre-apply audit artifact shipped Wave 86 Bucket E-AC4; apply gated on operator human-trigger per `release-deploy-standard.md` §9; AC#2 (Spring Boot `Cache-Control: no-store` header) + post-apply self-test pending
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

- [ ] Cloudflare Page Rule deployed cho `kitehub.me/auth/*` + `kiteclass.me/auth/*`
- [ ] Spring Boot magic-link controllers set `Cache-Control: no-store` header
- [ ] Self-test 2 sequential curl requests show `cf-cache-status: BYPASS` hoặc `DYNAMIC`, never HIT
- [ ] `pre-launch-infra-hardening-checklist.md` Cat 5 row added + verified PASS
- [ ] **🚨 BLOCKING**: Wave 86 Bucket G invite KHÔNG ship cho đến khi AC này verified

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 19 + §5 E-AC4 + §6 NEW gap proposals
- Pre-apply state-check audit: `documents/04-quality/audits/cloudflare-verification/2026-05-16-wave-86-magic-link-bypass-page-rule.md`
- Wave 86 plan §3 Bucket E AC E-AC4 (P0 BLOCKER)
- Cloudflare runbook GAP-394 (Wave 84)
- `pre-launch-infra-hardening-checklist.md` Cat 5

## Log

- **2026-05-16** Wave 86 Bucket E-AC4 — shipped terraform `infrastructure/terraform-cloudflare/page_rules.tf` (2 Page Rules: `*kitehub.me/auth/magic*` + `*kitehub.me/auth/invite/*` với `cache_level = bypass`, priority 1+2) + pre-apply audit artifact per `pre-mutation-state-check.md` §3. Status flipped OPEN → 🟡 PARTIAL (60%). Apply gated on operator human-trigger per `release-deploy-standard.md` §9. Pending: (1) operator runs `terraform plan` + verify Free tier 2/3 quota; (2) operator triggers apply via human workflow; (3) post-apply self-test verify `CF-Cache-Status: BYPASS` per audit §Self-test; (4) AC#2 Spring Boot `Cache-Control: no-store` header (origin-layer defense-in-depth) — file follow-up sub-task; (5) cross-link `pre-launch-infra-hardening-checklist.md` Cat 5 row "Magic-link endpoints bypass CF cache verified" post-apply.
