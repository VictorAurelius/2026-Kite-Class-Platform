---
title: Post-Wave-80 Session Handoff — v1.0.0-rc gate cleared, Wave 81 DEPLOY+SMOKE queued
status: active
created: 2026-05-15
updated: 2026-05-15
waves: [80, 81]
gaps: [GAP-537c, GAP-562, GAP-562b]
---

# Post-Wave-80 Session Handoff — 2026-05-15

**Previous session:** Wave 80 v1.0.0-rc Blockers (4 buckets shipped) + retroactive Bucket D add per user-flagged inside-out completeness miss.
**Current state:** v1.0.0-rc gate cleared. Wave 81 DEPLOY+SMOKE unblocked (plan PR shipped Wave 80 closure rename).
**Next phase:** Phase 1 BETA production rollout — dev self-test gate.

---

## 1. Wave 80 Summary

**Theme:** v1.0.0-rc Blockers — close 3 P0 (GAP-561b/562b/564) + retroactive F2 manual.
**Buckets shipped (4 parallel):** A META audit v2 · B invite-staff · C RBAC role-guard · D F2 manual retrofit (added post-spawn per user nudge).

**Outcomes:**
- 3 gaps DONE 100% (GAP-561 / GAP-561b / GAP-564)
- 3 gaps PARTIAL (GAP-537→75 / GAP-562→90 / GAP-562b→85)
- 1 NEW: GAP-537c P1 follow-up filed (P2/P3 screenshots + Tier 2 annotation)
- v1.0.0-rc gate cleared

---

## 2. ⚠️ Open Items / Follow-up Wave 81

### 2.1 GAP-537c — P2/P3 screenshots + Tier 2 annotation (P1)

**Scope:** Run capture script against live dev server (OWNER + STAFF seeded users), then Sharp/Jimp annotation overlay (mũi tên đỏ + viền vàng + số bước) for all 20 screenshots. Replace placeholder PNGs.
**Bundle:** với Wave 81 DEPLOY+SMOKE Bucket F (dev self-test walkthrough) — natural fit because dev server live for smoke testing.
**Estimate:** ~2-3h.

### 2.2 kitehub-branding @PreAuthorize Wave 81 (P1 — sister GAP-562b carry-over)

**Scope:** Add `spring-boot-starter-security` dependency to `kitehub/kitehub-branding/pom.xml` + `@EnableMethodSecurity` to BrandingConfig + `@PreAuthorize("hasAuthority('OWNER')")` on all `BrandingController` endpoints + `BrandingControllerSecurityTest`. Currently defense = FE RoleGuard at `/branding/*` layout + gateway path-filter only.
**Estimate:** ~1-2h.

### 2.3 Wave 80 post-wave audit suite v2 format (≤3 ngày per `post-wave-audit-mandate.md` §2.2)

MUST use **v2 format per GAP-564** — per-control evidence template (Command run + Output + Verdict + Evidence artifact ID per SOC2/ISO27001/OWASP ASVS):

- UI /128 across admin staff (invite + list + accept) + customer (billing/branding/settings RoleGuard) + help/{p2,p3,admin}
- API Contract /100 across 5 invitation endpoints + 14 RBAC-protected billing/subscription endpoints
- Business Logic /100 (RBAC + invite flow + audit log)
- Security /100 **v2 format** (RBAC + HMAC token + RbacAccessDeniedHandler audit)
- Ops Readiness /100 (V49 + Puppeteer/Playwright dev deps)
- Quality /100 weekly refresh

### 2.4 TenantSettingsController dangerzone segmentation (P2 — kế thừa từ GAP-562b)

**Scope:** Khi BE controller `TenantSettingsController` được tạo, sub-resource split: general GET → OWNER+STAFF, dangerzone (delete tenant, reset data, transfer ownership) → OWNER only. Currently FE-only RoleGuard at `/settings/*`.
**Defer:** Wave 81+ khi BE controller lands.

---

## 3. Wave 81 — DEPLOY + SMOKE (existing draft renamed Wave 80→81)

**Status:** plan PR drafted `documents/03-planning/waves/wave-2026-05-14-81-deploy-smoke.md` (status:draft).
**Theme:** Phase 1 BETA prod rollout — dev self-test gate.
**Structure:** 7 sequential buckets ~6-10h (HIGH stake, outside-in SKIP per internal-ops scope).
**Trigger:** Wave 80 closure shipped → start Bucket A AWS stack up.

**Suggested bucket additions for Wave 81 (vs original Wave 80 draft):**
- Original 7 buckets (AWS up → deploy → smoke → verify → audit → dev self-test → beta invite)
- **NEW Bucket H:** GAP-537c (P2/P3 screenshots capture + Tier 2 annotation) — bundle with dev self-test live UI
- **NEW Bucket I:** kitehub-branding @PreAuthorize (deferred from Wave 80) — small BE delta

---

## 4. Quick Start Commands

```bash
# Restart AWS stack
bash scripts/aws/start-stack.sh

# Run Wave 80 post-wave audit suite v2 format
# (use audit-report-template-v2.md skeleton)

# Verify invite-staff flow end-to-end
curl -X POST http://localhost:8080/api/v1/staff/invitations \
  -H "Authorization: Bearer $OWNER_JWT" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","role":"STAFF","fullName":"Test"}'

# Verify RBAC enforcement (STAFF role → 403)
curl -X POST http://localhost:8080/api/v1/billing/payment-method \
  -H "Authorization: Bearer $STAFF_JWT" \
  -d '{}' \
  # expect 403 + admin_audit_log row

# Regenerate user manual PDFs (verify Bucket D)
bash scripts/render-user-manual-pdf.sh anonymous
bash scripts/render-user-manual-pdf.sh platform-admin
```

---

## 5. Key Files Touched (Wave 80)

| Area | Files |
|------|-------|
| Migrations | `V49__create_staff_invitation_audit_log.sql` |
| BE invite | `InvitationController.java` (real 5 endpoints) · `InvitationTokenService.java` (HMAC) · `StaffInvitationAuditEntry.java` |
| BE RBAC | `PaymentController.java` + `SubscriptionController.java` (`@PreAuthorize`) · `RbacAccessDeniedHandler.java` |
| BE email | `invite-staff.html` + `invite-staff.txt` + `EmailType.INVITE_STAFF` + `EmailServiceClient.sendInviteStaffEmail()` |
| FE RBAC | `RoleGuard.tsx` + `useRole.ts` + `(customer)/{billing,branding,settings}/layout.tsx` + `Sidebar.tsx` (requiresRole filter) |
| FE invite | `/admin/staff/page.tsx` + `/admin/staff/invite/page.tsx` + `/staff/accept-invite/page.tsx` (Suspense wrapped) |
| FE help | `/help/{p2-owner,p3-manager,platform-admin}/[slug]/page.tsx` |
| Manual sources | `documents/05-guides/user-manual/{p2-owner,p3-manager,platform-admin}/*.md` (15 files) |
| Scripts | `scripts/render-user-manual-pdf.{sh,mjs}` · `scripts/capture-user-manual-screenshots.{sh,mjs}` |
| Skills | `.claude/skills/quality/security-audit/SKILL.md` v2 + `reference/audit-report-template-v2.md` (NEW) |
| Audit reports | 5 Wave 78 audit reports v1-format banner |
| Tests | `InvitationControllerIntegrationTest` (9) · `PaymentControllerSecurityTest` (7) · `SubscriptionControllerSecurityTest` (8) · `RoleGuard.test.tsx` (5) · `Sidebar.test.tsx` (13) · `e2e/role-guard.spec.ts` (8) · `e2e/staff-invite.spec.ts` (4) |

---

## 6. Notes for Next Session

1. **DO NOT** run Wave 81 Bucket A AWS start before Wave 80 closure PR merges (this PR has CSV + ROADMAP sync needed before deploy)
2. Worktree contamination noted 3/4 Bucket agents Wave 80 — `feedback_worktree_absolute_path_contamination.md` memory still active concern; Edit tool absolute path resolution bug when worktree branch active
3. Vercel rate-limit (GAP-495) inconsistent — sometimes "Canceled by Ignored Build Step" (whitelist working), sometimes "rate-limited" (intermittent). NOT required check on branch protection so doesn't block MCP merge
4. Wave 80 post-wave audit suite **MUST use v2 format** per GAP-564 — `documents/04-quality/audits/security/2026-05-XX-post-wave-80.md` template từ `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md`
5. **inside-out-completeness-trigger.md recurring miss** Wave 80 (Claude lock scope 3 buckets, user nudged manual missing → expand to 4) — 3rd time same session pattern caught. Update `feedback_outside_in_recurring_miss.md` memory with new occurrence

---

## 7. Phase 1 BETA Strict-Min Path to v0.9.0-beta

| Wave | Status | Description |
|------|:------:|-------------|
| 77 SEND foundation | ✅ DONE | email DNS + actuator + V39-V42 + invite token + slug normalize |
| 78 Launch Retain UX/trust | ✅ DONE | onboarding + feedback + beta-status + 5-email audit |
| 79 Close-Out v1.0.0-rc gate | ✅ DONE | 2FA versioning + RBAC + security cluster + user manual rule |
| **80 v1.0.0-rc Blockers** | ✅ DONE | invite flow + RBAC enforcement + audit v2 + F2 manual |
| **81 DEPLOY+SMOKE** | 🟡 DRAFT | AWS stack up + deploy + smoke + audit v2 + dev self-test + GAP-537c + kitehub-branding @PreAuthorize |
| **82 BETA invite trigger** | ⏳ PLANNED | 5 beta tenants seeded + tenant invite emails sent |

**v0.9.0-beta tag readiness:** after Wave 82 — all P0 BLOCKING cleared (GAP-558 cookie consent PDPL deadline 2026-07-01 still in window for Wave 82+ Bucket TBD).
