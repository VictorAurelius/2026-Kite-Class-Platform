# Session handoff — 2026-06-04 Flow Verification Campaign 2 waves G1 PASS

**Date:** 2026-06-04
**Session scope:** Flow Verification Campaign loop — KH-2 wave (split per topology revision) + KH-1 wave (full beta funnel chain) + GAP-916 P0 production-blocking fix
**Outcome:** 2 wave G1 PASS shipped (PR #2146 merged, PR #2147 open), 5 gaps filed, 1 P0 fix shipped, campaign §3 topology revised

---

## TL;DR

- ✅ **Wave flow-kh2** ([PR #2146 merged](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/2146)): KH-2a admin auth + KH-2c owner login + onboarding wizard G1 ✅; **GAP-916 P0 fix shipped** (JwtAuthenticationGatewayFilter Order `LOWEST_PRECEDENCE-2` để header inject sau default-filter `RemoveRequestHeader` strip + trước NettyRoutingFilter forward)
- ✅ **Wave flow-kh1** ([PR #2147 open](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/2147)): Beta funnel full chain S1-S5 + KH-2c chain G1 ✅ (anonymous request → admin approve via gateway → MailHog email → exchange-claim-code → validate token → complete signup → owner+tenant provisioned → onboarding wizard)
- 🔄 **Topology revision** (Wave flow-kh2 §6 closing per GAP-919): KH-2 split → KH-2a (admin auth prereq cho KH-1.S2) + KH-2b (register-via-invite = KH-1.S5 sub-step) + KH-2c (owner login + wizard post-register). Campaign §3 dependency graph Mermaid re-state-checked, thứ tự loop revised
- 📋 **5 gaps filed today**: GAP-916 P0 DONE (gateway header race) + GAP-917 P2 (login sad path 400 vs 401) + GAP-918 P2 (gateway authCircuitBreaker startup transient) + GAP-919 P2 (KH-2 topology fix) + GAP-920 P2 (api-contract.md beta-signup body drift)

---

## State at session end

### Stack
- All KH services healthy (gateway / subscription / admin / branding / email / FE)
- Postgres / Redis / RabbitMQ / MinIO / MailHog healthy
- kiteclass-* healthy
- Test users created: Owner `prospect+kh1walk-1780540178@example.com / Walk@KH1Test123` (created via KH-1 walk), Admin `admin@kitehub.com / Admin@KiteHub123` (TOTP re-enrolled this session, secret in DB but encrypted)
- **Stop hint** nếu idle: `bash kitehub/scripts/down.sh` để save Free Tier hours

### Campaign progression (per `documents/03-planning/roadmap/flow-verification-campaign.md`)
- ✅ **KH-2a** Admin auth (login + 2FA enroll) — G1 ✅ from wave-flow-kh2 S4
- ✅ **KH-2c** Owner login + onboarding wizard — G1 ✅ via gateway (chain verified Wave flow-kh1)
- 🔄 **KH-1** Beta funnel full chain — G1 ✅ pending G2 user FE test + G3 production parity
- ⬜ **KH-3** Subscription create + trial→paid migration — **NEXT LOOP**
- ⬜ KC-1, KC-2, KC-3, KC-4, KC-5/6/7, KC-8/9 — chờ topology order

### PRs
- ✅ [#2146](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/2146) Wave flow-kh2 — MERGED
- 🔵 [#2147](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/2147) Wave flow-kh1 — OPEN, needs CI verify + merge

---

## Pickup state cho next session

### G2 user FE test recipe (KH-1 + KH-2c chain) — PENDING USER

1. Browser http://localhost:3001 (kitehub-frontend)
2. Click CTA "Dùng thử miễn phí 14 ngày" → redirect `/request-beta-access`
3. Điền form (email + name + organizationName + persona + consent checkbox) → submit
4. Mở tab khác, admin@kitehub.com login (TOTP code re-compute từ DB encrypted secret OR reset enrollment via SQL)
5. Vào admin dashboard → "Beta Requests" → approve request vừa tạo
6. Mở MailHog http://localhost:8025 → click latest message → copy invite link
7. Paste link incognito browser → form prefilled → submit password
8. Auto-login → dashboard → onboarding wizard 5 step → click 1 step → state update

### G3 production parity verify — PENDING POST-G2

- `EMAIL_VERIFICATION_ENABLED=true` production (local=false bypass)
- AWS SES email signing (vs MailHog local)
- Cloudflare DNS invite-link URL reachable (`kitehub.me` prefix)
- Captcha enabled production (local `captcha.enabled=false`)
- Re-walk full chain trên production-equivalent env

### Next loop = KH-3 Subscription create + trial→paid migration

- Stack vẫn UP (nếu chưa down)
- Owner test user vẫn live: `prospect+kh1walk-1780540178@example.com / Walk@KH1Test123` (tier FREE, status TRIAL, 14 ngày)
- Wave plan KH-3 lazy-create khi start loop per campaign §5 convention
- Likely sub-steps: GET subscription state → POST /api/auth/upgrade-tier (KH-4 ✅ verified) → admin confirm payment → tier=PRO transition

### Residual blockers/drift defer Phase 1 BETA cleanup batch

- GAP-917 P2: login sad path returns 400 thay vì spec 401 INVALID_CREDENTIALS (UC-AUTH-001 spec drift)
- GAP-918 P2: gateway authCircuitBreaker mở state lúc startup → 503 fallback HTML cho /api/auth/register + /api/v1/auth/beta-signup (cold-start race, ~30s self-heal)
- GAP-920 P2: api-contract.md beta-signup body schema `{token, password, acceptTos}` vs code `{token, ownerPassword, subdomain}` — docs drift from code
- GAP-919 P2 (informational): KH-2b register-via-invite belongs to KH-1 wave per topology

---

## Investigation insights (per `release-fix-retry-budget.md` §3.5 documented)

### GAP-916 root cause + fix (5 rebuild cycles → final --no-cache)

- **Root cause:** Spring Cloud Gateway filter ordering race. Default-filter `RemoveRequestHeader=X-User-Id` (Order ~0) was stripping headers AFTER `JwtAuthenticationGatewayFilter` (Order=-100) injected them → subscription received empty `X-User-*` headers → Spring Security `@PreAuthorize` rejected with 401
- **Diagnostic process:** Added log statements progressively (catch block → success path → post-mutate header → doFinally response). Earlier rebuilds (5 cycles) had stale Docker layer cache hiding Order change. Final `--no-cache` rebuild applied actual Order change → fix verified GET+PUT onboarding HTTP 200
- **Fix:** `JwtAuthenticationGatewayFilter.ORDER = Ordered.LOWEST_PRECEDENCE - 2` + `TenantHeaderGuardFilter.ORDER = Ordered.LOWEST_PRECEDENCE - 1` → filters chạy SAU default-filter strip + TRƯỚC NettyRoutingFilter (LOWEST_PRECEDENCE) forward
- **Sister test files updated:** `JwtAuthenticationGatewayFilterTest.filterOrderIsLowestPrecedenceMinus2` + `TenantHeaderGuardFilterTest.filterOrderIsLowestPrecedenceMinus1`

### Topology revision (GAP-919) — Phase 1 BETA gate self-service register

- KiteHub landing CTA = "Dùng thử miễn phí 14 ngày" (NOT "Đăng ký")
- `/register` route → HTTP 307 redirect → `/request-beta-access` (KH-1 funnel entry)
- BE `AuthService.register()` line 123 = Phase 2 self-service path (BE-reachable trực tiếp); `AuthService.registerFromBetaInvite()` line 218 = Phase 1 BETA actual path (auto-verifies email, tenant provisioned)
- FE chỉ expose registerFromBetaInvite via `/beta-signup/code/<token>` từ invite email
- Campaign §3 dependency graph initially had KH-2 → KH-1 (CODE dependency: KH-1 admin approve calls KH-2 admin auth). User-facing FE flow ngược lại: KH-1 root, KH-2b register là consequence của KH-1.S5

---

## Cross-link

- Wave flow-kh2 plan: `documents/03-planning/waves/wave-2026-06-03-flow-kh2-auth-onboarding.md`
- Wave flow-kh1 plan: `documents/03-planning/waves/wave-2026-06-04-flow-kh1-beta-funnel.md`
- Campaign: `documents/03-planning/roadmap/flow-verification-campaign.md`
- 5 gap files: `documents/04-quality/gaps/phase-1-beta/{GAP-917,918,919,920}.md` + `phase-1-beta/closed/GAP-916*.md`
- ROADMAP §🎯: `documents/04-quality/gaps/ROADMAP.md` (updated this session)
- wave-history: `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` (2 new entries `flow-kh2` + `flow-kh1`)

---

## Session lock cleanup

- Session lock created at start: `.claude/session-locks/session-20260603-194356-NguyenVanKiet.lock`
- Auto-purge after 4h staleness OR manual delete khi confirmed done

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
