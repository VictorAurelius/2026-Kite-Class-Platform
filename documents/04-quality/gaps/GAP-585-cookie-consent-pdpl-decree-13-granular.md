# GAP-585: Cookie consent banner PDPL Decree 13 — granular consent + no dark pattern + retention log

**Status:** 🔵 OPEN
**Priority:** 🔴 **P0 BLOCKER** (chặn Wave 86 Bucket E pass + tag rc1)
**Domain:** Frontend / Backend / Compliance
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A benchmark-vn-saas-edu audit Q7)
**Affects:** Landing kitehub.me + kiteclass.me + first 5 beta cohort + PDPL compliance posture

## Problem

VN Decree 13/2023/NĐ-CP (PDPL) effective 2023-07-01 mandates:
- **Granular consent** required cho every distinct purpose (analytics ≠ marketing ≠ functional)
- **No dark patterns** (no pre-checked boxes, no asymmetric Accept/Reject button styling, no consent walls)
- **No-action = no consent** (strict opt-in, không default-accept)
- **Withdraw consent mechanism** available trong footer/settings
- **Consent log retained ≥3 năm** per Decree 13 retention rule

Wave 86 plan §3 Bucket E nói "CSP report-only acceptable v1" nhưng KHÔNG mention cookie consent banner. Tag `v1.0.0-rc.1` ship → invite 5 beta tenants → compliance risk + first-impression damage (beta tenants có thể flag immediately).

## Root Cause

Wave 83 PDPL Art 11 opt-in shipped covers data-collection consent nhưng KHÔNG cover cookie consent UX banner. Industry benchmark CookieYes/CookieHub Vietnam PDPL coverage requires distinct cookie consent layer trên top of data-collection consent.

## Proposed Fix

1. **FE component** `kitehub-frontend/src/components/cookie-consent-banner.tsx`:
   - 3 checkbox split: Analytics (Google Analytics / Plausible) / Marketing (Resend tracking pixels) / Functional (always-on essential)
   - 2 buttons equal weight: "Chấp nhận tất cả" + "Chỉ chấp nhận thiết yếu" (NO Accept-bigger pattern)
   - Banner persistent đến khi user choose (no auto-dismiss)
   - Footer link "Cài đặt cookie" → re-open banner cho withdraw
2. **BE consent log table** Flyway `V54__consent_log.sql`:
   - Columns: `id`, `user_id` nullable cho anonymous, `tenant_id` nullable, `consent_categories` JSON, `consent_version` (Decree 13 reference), `ip_hash`, `user_agent`, `created_at`, `withdrawn_at` nullable
   - Retention: 3 năm hot OR archive to S3 lifecycle
3. **No-cookie-before-consent verify**:
   - All analytics/marketing scripts load conditional on consent state
   - Self-test: `curl -sI https://kitehub.me/ | grep -i 'set-cookie'` không có analytics cookie trước user accept
4. **Update `pre-launch-infra-hardening-checklist.md`** Cat 5 add row "Cookie consent banner PDPL Decree 13 compliance"

## Acceptance Criteria

- [ ] Cookie consent banner shipped với 3-checkbox granular consent
- [ ] No dark pattern: equal-weight Accept/Reject buttons + no pre-checked boxes
- [ ] Withdraw mechanism trong footer settings working
- [ ] Consent log table V54 shipped + populated correctly per user action
- [ ] Consent log retention ≥3 năm policy documented + cron archive setup
- [ ] Self-test `curl -sI` confirms zero analytics cookie SET trước explicit accept
- [ ] `pre-launch-infra-hardening-checklist.md` Cat 5 row verified PASS
- [ ] **🚨 BLOCKING**: Bucket E gate gated on AC này; tag rc1 không ship trước khi PASS

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q7 + §5 E.6 NEW + §6 GAP-NEW-1
- Wave 86 plan §3 Bucket E AC E-AC6 (P0 BLOCKER)
- Wave 83 PDPL Art 11 opt-in (data-collection consent, separate layer)
- VN Decree 13/2023/NĐ-CP
- External refs: CookieYes PDPL coverage, CookieHub PDPL Vietnam
