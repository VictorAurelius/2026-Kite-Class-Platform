# GAP-818: Wave tenant-domain-1 live RST walk all 4 buckets

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-01 (Wave tenant-domain-1 closure)
**Affects:** Live verify gated GAP-612 AWS restore + Bucket D ACM apply

## Problem

Wave tenant-domain-1 shipped 4 PARTIAL gaps (GAP-811/812/813/814) — code merged + unit/IT tests PASS local + Mockito stack PASS, BUT live RST walk on production-equivalent stack DEFERRED per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g). 4 gaps DONE flip blocked until live walk evidence pasted.

Blockers:
- GAP-612 AWS restore (RDS unreachable) — blocks live stack walk
- Bucket D ACM cert apply (terraform deferred per `release-deploy-standard.md` §9) — blocks custom-domain HTTPS walk

## Proposed Fix

Once GAP-612 + ACM apply unblock:

1. **GAP-814 walk:** Owner login → curl `X-Tenant-Id` spoofed → expect 403 + gateway log strip; verify TenantHeaderGuardFilter re-injects from JWT
2. **GAP-813 walk:** GET `/api/v1/public/tenants/resolve?slug=test-tenant` from anonymous browser → expect 200 + `{id, slug, status}`; verify 30/min/IP rate-limit kicks at 31st
3. **GAP-811 walk:** Browser visit `tenant-a.kitehub.me` → FE middleware resolves tenant → SSR render correct landing; visit suspended tenant → suspended page
4. **GAP-812 walk:** Owner adds custom domain → DNS TXT challenge issued → user adds TXT → background verify polls → status flips CERT_PROVISIONING → ACM cert issued → custom domain serves HTTPS

5. **Flip 4 gaps DONE** per `gap-done-discipline.md` §2 (paste walk evidence in gap closure block; git mv to `phase-1-beta/closed/`)

## Acceptance Criteria

- [ ] GAP-612 AWS restore unblocked
- [ ] Bucket D ACM cert provisioned (terraform apply)
- [ ] GAP-814 walk evidence: HTTP 403 spoofed header + JWT re-inject confirmed
- [ ] GAP-813 walk evidence: 200 resolve + rate-limit at 31
- [ ] GAP-811 walk evidence: multi-host SSR + suspended state
- [ ] GAP-812 walk evidence: full DNS→cert→HTTPS lifecycle (or PARTIAL flag if SSL Phần B defer to GAP-816)
- [ ] GAP-811/812/813/814 status flipped DONE in CSV + file git mv to `phase-1-beta/closed/`

## Related

- Wave plan: `documents/03-planning/waves/wave-tenant-domain-1.md`
- Rules: `.claude/rules/pre-handoff-self-test-completeness.md` §2.4, `.claude/rules/feature-ship-runtime-walk-mandate.md`, `.claude/rules/gap-done-discipline.md` §2
- Blockers: GAP-612 (AWS restore), Bucket D ACM apply deferred
- Sister gaps: GAP-811/812/813/814/816/817
