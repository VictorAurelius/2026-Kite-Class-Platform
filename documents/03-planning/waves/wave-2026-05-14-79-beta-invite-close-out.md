---
title: Wave 79 — Beta Invite Close-Out (v1.0.0-rc gate + multi-user RBAC + PDPL compliance)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [79]
gaps: [GAP-040, GAP-537, GAP-544, GAP-545, GAP-547, GAP-548, GAP-551, GAP-552, GAP-553, GAP-554, GAP-555, GAP-556, GAP-557, GAP-558, GAP-559, GAP-560, GAP-561, GAP-562, GAP-563]
---

# Wave 79 — Beta Invite Close-Out

**Goal:** Đóng cổng `v1.0.0-rc` Phase 1 BETA — fix 6 P0 (3 inside-out audit findings + 3 outside-in persona findings) + 9 P1 (defense-in-depth security + UX retention + docs/test hygiene). Sau Wave 79, Beta invite có thể launch end-to-end với P3 Manager flow live + PDPL cookie consent + RBAC role separation.
**Trigger:** Wave 78 post-wave audit suite 2026-05-14 (5 audits parallel) surface 3 P0 v1.0.0-rc-gate blockers + 7 P1 follow-ups. Outside-in persona audit 4-persona walkthrough (P2 Owner / P3 Manager / Anonymous Prospect / Platform Admin) cùng ngày surface 3 P0 multi-user/compliance + 2 P1 UX/discoverability — class hoàn toàn khác inside-out (dev audit catch internal correctness; persona audit catch multi-user + VN compliance + UX discoverability).
**Estimated wall-clock:** ~8-12h agent work, longest bucket ~4-6h (Bucket B P0 outside-in — 3 cross-cutting flows: cookie consent + invite-staff + RBAC; split risk if effort >4h).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment — inside-out + outside-in scope coverage):**

Inside-out queue (13 items, Wave 78 audit suite + carry-forward):

| Source | Gap | Priority | Class |
|---|---|---|---|
| API Contract audit | GAP-547 | P0 | 2FA endpoints undocumented + unversioned |
| Security audit | GAP-551 | P0 | Feedback endpoint missing gateway route + null tenantId |
| Business Logic audit | GAP-555 | P0 | 15+ config keys documented không wired qua @Value |
| API Contract audit | GAP-548 | P1 | Password-reset BE controller missing (gateway forwards 404) |
| Security audit | GAP-552 | P1 | SecurityConfig default-allow fallback |
| Security audit | GAP-553 | P1 | TOTP cipher + JWT secret dev-default không fail-fast |
| Security audit | GAP-554 | P1 | Onboarding X-Tenant-Id không cross-check JWT claim |
| UI audit | GAP-545 | P1 | Dialog focus-trap + Escape (WCAG 2.1.1) |
| Carry-forward | GAP-544 | P1 | testcontainers/H2 migration |
| Carry-forward | GAP-537 | P1 | User manual screenshots per persona |
| Carry-forward | GAP-040 | P1 | Support impersonation |
| Business Logic audit | GAP-556 | P1 | Support rules.md misleading — BE not implemented |
| Business Logic audit | GAP-557 | P1 | use-cases.md thiếu BR-xxx refs |
| Outside-in user manual audit | **GAP-563** | **P1 META** | User manual content review standard rule file (force-multiplier; ship same PR với F1 sample) |

Outside-in additions (5 items, từ persona audit 2026-05-14 + 1 từ user manual audit pre-F1):

| Persona | Gap | Priority | Friction class |
|---|---|---|---|
| Anonymous | GAP-558 | **P0** | PDPL cookie consent (deadline 2026-07-01) |
| P3 Manager | GAP-561 | **P0** | invite-staff entire flow missing |
| P3 Manager | GAP-562 | **P0** | RBAC OWNER/STAFF role separation |
| P2 Owner | GAP-559 | P1 | /onboarding entry point invisible |
| P2 Owner | GAP-560 | P1 | Beta disclaimer specificity |

**Zero overlap** giữa inside-out (internal correctness) ↔ outside-in (multi-user + PDPL + UX discoverability).

**Q2 (trade-offs):**
- **Cookie consent (GAP-558):** in-house lightweight component vs vendor Cookiebot/Osano → chọn **in-house** (Phase 1 BETA chưa cần vendor + tiết kiệm $30-50/mo).
- **RBAC (GAP-562):** full role matrix (Owner/Manager/Teacher/Accountant/Receptionist) vs MVP 2-role → chọn **MVP 2-role (OWNER/STAFF)** Phase 1; full matrix Wave 80+.
- **2FA versioning (GAP-547):** rollback PR #1301 vs add `/api/v1/` alias → chọn **add prefix với backward-compat alias 30 days** (tránh disrupt 2FA store đã setup).
- **15 config keys wiring (GAP-555):** bulk `@Value` annotation scan vs spring-cloud-config → chọn **bulk @Value** (~10 min effort vs infra overhead).
- **invite-staff (GAP-561):** new email pipeline vs re-use kitehub-email Resend infra → chọn **re-use** (chỉ thêm `invite-staff.ftl` template + BE endpoint + FE form).

**Q3 (risks):**
- **GAP-562 RBAC migration:** existing PLATFORM_ADMIN role + ADMIN FE guard (Wave 78 GAP-518 compat) cần migrate sang OWNER. Risk session invalidate; mitigation backward-compat alias 30 days + re-login prompt.
- **GAP-558 cookie consent:** analytics fire trên page load TRƯỚC banner consent → defer via `<Script strategy="afterInteractive">` + onConsent callback. Risk: GA/Mixpanel mất dữ liệu beta scale (acceptable).
- **GAP-561 multi-user impact:** Wave 79 ship LẦN ĐẦU multi-user flow live → test matrix Owner gửi invite → Staff nhận email → setup → first login → dashboard scoped (KHÔNG thấy billing/branding của Owner).
- **Bucket A `@Value` wiring miss key:** grep `kitehub\.(feedback|onboarding|beta-status|support)\.` cross-check rules.md vs Java để không miss key.
- **GAP-544 testcontainers Docker-in-Docker CI:** kiểm tra `.github/workflows/kitehub-ci.yml` đã có docker-in-docker.
- **V45 + V46 concurrent apply:** per `concurrent-production-mutation-ops.md` — serialize: V45 staff_invitations → V46 rbac_roles, KHÔNG cùng deploy slot.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|---|---|---|---|---|
| **0 Foundation** | GAP-547 + GAP-555 + GAP-562 + GAP-558 docs (contract + rules.md + cookie consent rules) | bg-agent | ~45 min | ✅ docs only |
| **A P0 v1.0.0-rc gate** | GAP-547 (2FA contract+version) + GAP-551 (feedback gateway+tenantId) + GAP-555 (15 config @Value) | bg-agent | ~3-4h | ✅ gateway YAML + feedback module + @Value scan |
| **B P0 outside-in invite** | GAP-558 (cookie consent) + GAP-561 (invite-staff E2E) + GAP-562 (RBAC OWNER/STAFF) | bg-agent | ~4-6h | ✅ public + dashboard staff + new staff-invitation module + V45/V46 |
| **C P1 cluster security** | GAP-548 (password-reset BE) + GAP-552 (SecurityConfig default-deny) + GAP-553 (TOTP/JWT fail-fast) + GAP-554 (X-Tenant-Id JWT cross-check) | bg-agent | ~2-3h | ✅ auth module + SecurityConfig + onboarding controller |
| **D P1 UX retention** | GAP-545 (Radix Dialog migrate) + GAP-559 (Sidebar nav + CTA) + GAP-560 (disclaimer specificity + data-reset doc) | bg-agent | ~2-3h | ✅ dashboard sidebar + disclaimer banner + new doc |
| **E P1 docs+tests** | GAP-544 (testcontainers 2 tests) + GAP-556 (support rules.md scope note) + GAP-557 (use-cases BR-refs) | bg-agent | ~2h | ✅ subscription tests + business docs |
| **F1 P1 user manual sample + meta** | GAP-563 (rule file `user-manual-content-standard.md`) + GAP-537 anonymous-prospect sample (5 pages) | bg-agent | ~2-3h | ✅ rule + anonymous folder + Next.js MDX route |
| **F2 P1 user manual rest** (DEFER Wave 80+) | GAP-537 rest (P2 Owner + P3 Manager + Platform Admin × 5-10 pages) | bg-agent | ~2-3h | gated on F1 dev review approval |
| **F-bis P1 support impersonation** | GAP-040 (admin "View as tenant" BE+FE+audit log) | bg-agent | ~2h | ✅ kitehub-admin impersonation module — independent F1/F2 |

**Disjoint check:** 9 bucket (0 + A/B/C/D/E + F1/F2-defer/F-bis) touch disjoint package/file paths — chi tiết §3 Scope. F2 gated on F1 dev review = defer-eligible (Wave 80+); 8 actual buckets shipping Wave 79.

---

## 3. Scope (compact schema — Strategy B+C proven Wave 33)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH → model: Opus 4.7 full
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** YES → Bucket 0 Foundation required per `contract-first-for-cross-layer.md`

> Gap referencing convention per `.claude/rules/gap-architecture-v2.md`: query via `bash scripts/query-gaps.sh <prefix>` để confirm status/priority/phase.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|---|---|:-:|---|:-:|
| 0 | **Foundation** | GAP-547 (contract) + GAP-555 (rules.md config registry) + GAP-562 (RBAC roles rules.md+use-cases) + GAP-558 (cookie consent rules.md) | 🟠 P0/P1 mixed | `documents/01-business/kitehub/auth-2fa/api-contract.md` + `documents/01-business/{onboarding,feedback,beta-status,support}/rules.md` (15-key registry update) + `documents/01-business/roles/{rules.md,use-cases.md}` + `documents/01-business/cookie-consent/{rules.md,use-cases.md,api-contract.md}` + `kitehub-frontend/src/test/msw/handlers/{staff-invitations,cookie-consent}.ts` | MERGE FIRST |
| 1 | **A** | GAP-547 + GAP-551 + GAP-555 | 🔴 P0 | `kitehub-gateway/src/main/resources/application*.yml` + `kitehub-subscription/src/main/java/com/kitehub/subscription/feedback/**` + `@Value` scan across `kitehub-subscription/{feedback,onboarding,beta-status,support}/service/**` | parallel after 0 |
| 2 | **B** | GAP-558 + GAP-561 + GAP-562 | 🔴 P0 | `kitehub-frontend/src/components/CookieConsent.tsx` + `kitehub-frontend/src/app/(dashboard)/staff/**` + `kitehub-subscription/src/main/java/com/kitehub/subscription/staff/**` (new module) + `kitehub-subscription/src/main/resources/db/migration/V45__create_staff_invitations.sql` + `V46__create_rbac_roles.sql` | parallel after 0 |
| 3 | **C** | GAP-548 + GAP-552 + GAP-553 + GAP-554 | 🟠 P1 | `kitehub-subscription/src/main/java/com/kitehub/subscription/auth/{controller,config}/**` + `kitehub-subscription/.../onboarding/controller/OnboardingProgressController.java` | parallel after 0 |
| 4 | **D** | GAP-545 + GAP-559 + GAP-560 | 🟠 P1 | `kitehub-frontend/src/components/{Sidebar,BetaDisclaimerBanner,FeedbackWidget,OnboardingChecklist}/**` + `documents/05-guides/operations/data-reset-policy.md` (new doc) | parallel after 0 |
| 5 | **E** | GAP-544 + GAP-556 + GAP-557 | 🟠 P1 | `kitehub-subscription/src/test/java/.../{DatabaseBackupServiceTest,InstanceControllerIntegrationTest}.java` + `documents/01-business/support/rules.md` + `documents/01-business/kitehub/auth/use-cases.md` | parallel after 0 |
| 6a | **F1** | GAP-563 (META rule) + GAP-537 (anonymous sample) | 🟠 P1 META | `.claude/rules/user-manual-content-standard.md` (NEW) + `documents/05-guides/user-manual/anonymous/{index,pricing,beta-access,terms,faq}.md` (5 MDX) + `kitehub-frontend/src/app/help/anonymous/**` (Next.js route) + `scripts/render-user-manual-pdf.sh` (NEW) | parallel after 0 |
| 6b | **F2** (DEFER Wave 80+) | GAP-537 rest | 🟠 P1 | `documents/05-guides/user-manual/{p2-owner,p3-manager,platform-admin}/*.md` | gated on F1 dev review — DEFER eligible |
| 6c | **F-bis** | GAP-040 | 🟠 P1 | `kitehub-admin/src/main/java/com/kitehub/admin/impersonation/**` (new module) + `kitehub-admin/frontend/src/components/ImpersonateButton.tsx` | parallel after 0 — independent |

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.0:
- Files: `documents/01-business/kitehub/auth-2fa/api-contract.md` (CREATE — GAP-547) + `documents/01-business/{onboarding,feedback,beta-status,support}/rules.md` (UPDATE — GAP-555 mark 15 keys với `@Value` target) + `documents/01-business/roles/{rules.md,use-cases.md}` (CREATE — GAP-562 RBAC OWNER/STAFF) + `documents/01-business/cookie-consent/{rules.md,use-cases.md,api-contract.md}` (CREATE — GAP-558) + `kitehub-frontend/src/test/msw/handlers/{staff-invitations,cookie-consent}.ts` (NEW MSW handlers)
- Acceptance: api-contract.md cho 2FA + cookie-consent + roles list all endpoints consumed by Bucket A/B; MSW handlers consumable bởi Bucket B FE tests
- Spawn order: MERGE FIRST trước khi spawn 6 buckets parallel

### Bucket A — P0 v1.0.0-rc gate (GAP-547+551+555)

- Files: `kitehub-gateway/src/main/resources/application*.yml` (RELATIVE), `kitehub-subscription/src/main/java/com/kitehub/subscription/feedback/**`, `kitehub-subscription/src/main/java/com/kitehub/subscription/{onboarding,feedback,beta-status,support}/service/**` (15-key @Value)
- Tests: `FeedbackServiceTest.java` (existing — add tenantId test); `kitehub-gateway/src/test/.../GatewayRoutesIntegrationTest.java` (new — verify feedback route + rate limit)
- Acceptance: 2FA endpoints documented in api-contract.md với version `/api/v1/auth/2fa/*` + backward-compat alias `/api/auth/2fa/*`; feedback endpoint route gateway + tenantId fix; 15 config keys wired qua @Value
- Cross-layer BE bucket: Controller signature + DTO match `documents/01-business/kitehub/auth-2fa/api-contract.md` schema

### Bucket B — P0 outside-in invite enablement (GAP-558+561+562)

- Files: `kitehub-frontend/src/components/CookieConsent.tsx`, `kitehub-frontend/src/app/(public)/layout.tsx` (mount banner), `kitehub-frontend/src/app/(dashboard)/staff/**` (list + invite form), `kitehub-subscription/src/main/java/com/kitehub/subscription/staff/**` (new staff-invitation module: controller/service/repository/dto), `kitehub-subscription/src/main/resources/db/migration/V45__create_staff_invitations.sql`, `V46__create_rbac_roles.sql`, `kitehub-email/src/main/resources/templates/invite-staff.ftl` (new template)
- Tests: `StaffInvitationServiceTest.java`, `StaffInvitationControllerTest.java`, `CookieConsent.test.tsx`, `RoleGuardTest.java`
- Acceptance: cookie consent banner live trên public site + analytics fire gated bằng consent state; P3 Manager E2E flow live (Owner invite → Staff nhận email → click link → setup password → first login → dashboard scoped); RBAC OWNER/STAFF role enforced (Staff KHÔNG thấy billing/branding)
- Cross-layer FE+BE bucket: Endpoint consumption tuân thủ schema trong `documents/01-business/{roles,cookie-consent}/api-contract.md`

### Bucket C — P1 cluster security defense-in-depth (GAP-548+552+553+554)

- Files: `kitehub-subscription/src/main/java/com/kitehub/subscription/auth/controller/PasswordResetController.java` (new), `kitehub-subscription/.../auth/config/SecurityConfig.java` (default-deny migration), `kitehub-subscription/.../auth/totp/TotpSecretCipher.java` (fail-fast guard), `kitehub-subscription/.../onboarding/controller/OnboardingProgressController.java` (JWT cross-check)
- Tests: `PasswordResetControllerTest`, `SecurityConfigTest`, `TotpSecretCipherTest`, `OnboardingProgressControllerTest` (extend với JWT tampering test)
- Acceptance: GAP-548/552/553/554 status=DONE

### Bucket D — P1 UX retention (GAP-545+559+560)

- Files: `kitehub-frontend/src/components/{FeedbackWidget,OnboardingChecklist}/*` (Radix Dialog migrate), `kitehub-frontend/src/components/Sidebar/SidebarNav.tsx` (add "Bắt đầu" nav), `kitehub-frontend/src/app/(dashboard)/layout.tsx` (top CTA "Hoàn tất setup (N/5)"), `kitehub-frontend/src/components/BetaDisclaimerBanner/BetaDisclaimerBanner.tsx` (specificity), `documents/05-guides/operations/data-reset-policy.md` (new doc)
- Tests: `FeedbackWidget.test.tsx` (Escape key + focus-trap), `Sidebar.test.tsx` (persona role display)
- Acceptance: GAP-545 WCAG 2.1.1 + 2.4.3 PASS; GAP-559 onboarding nav visible cho Owner persona; GAP-560 disclaimer link tới data-reset-policy.md

### Bucket E — P1 docs+tests hygiene (GAP-544+556+557)

- Files: `kitehub-subscription/src/test/java/.../DatabaseBackupServiceTest.java` + `InstanceControllerIntegrationTest.java` (testcontainers migration), `kitehub-subscription/pom.xml` (testcontainers dep), `documents/01-business/support/rules.md` (header scope note), `documents/01-business/kitehub/auth/use-cases.md` (BR-AUTH-009 alias)
- Acceptance: 2 tests pass on CI runner via testcontainers; support rules.md scope note explicit "Wave 78 DISCOVERABILITY ONLY"; BR-AUTH-009 documented

### Bucket F1 — User manual sample + meta standard (GAP-563 + GAP-537 anonymous sample)

- Files: `.claude/rules/user-manual-content-standard.md` (NEW rule v1.0.0 với 15-item checklist từ outside-in audit findings), `documents/05-guides/user-manual/anonymous/{index,pricing,beta-access,terms,faq}.md` (5 MDX pages annotated screenshots 1440×900 + 375×812 vi-VN), `kitehub-frontend/src/app/help/anonymous/**` (Next.js MDX route), `scripts/render-user-manual-pdf.sh` (NEW reusable từ `render-acceptance-test-xlsx.sh` pattern), `output-review-mandate.md` §3 row mới "User manual pages", `rules-index.csv` row mới
- Tests: 15-item self-test pass trên 5 pages prototype (auto-script verify); reviewer manual screenshot annotation review
- Acceptance: GAP-563 status=DONE (rule + 15-item checklist + 5-page worked example); GAP-537 status=PARTIAL (anonymous sample done; P2/P3/Admin defer F2)
- Outside-in findings integrated: Intercom-style TOC sidebar + PDF auto-gen + annotated screenshots + Fuse.js search + ≥3 discoverability entry points (header nav + footer + search)
- Cross-layer: rule file + content + render script — Bucket 0 Foundation prereq cho rule scaffold

### Bucket F2 — User manual rest (DEFER Wave 80+ post-F1 dev review)

- Files: `documents/05-guides/user-manual/{p2-owner,p3-manager,platform-admin}/*.md` (5-10 pages each)
- Gate: F1 sample approved by dev review → template + format validated → F2 apply same standard
- DEFER candidate: Wave 80 hoặc Wave 81 — KHÔNG gate Wave 79 closure
- Acceptance (Wave 79 trigger): GAP-537 status=PARTIAL completion_pct ~25% (1/4 personas); DONE flip khi cả 4 personas shipped

### Bucket F-bis — Support impersonation (GAP-040, independent F1/F2)

- Files: `kitehub-admin/src/main/java/com/kitehub/admin/impersonation/**` (new module: controller/service/repository), `kitehub-admin/frontend/src/components/ImpersonateButton.tsx` ("View as tenant" button), audit log row schema
- Tests: `ImpersonationServiceTest.java` (30s session timeout + audit log entry), `ImpersonateButton.test.tsx`
- Acceptance: GAP-040 status=DONE (admin impersonation E2E flow + audit trail; 30s session timeout enforced)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|---|---|---|---|---|
| `TwoFactorController.java` | Java class (Wave 72b ship) | `find kitehub/kitehub-subscription/src/main -name TwoFactorController.java` | 1 match `.../auth/twofactor/` | ✅ exists |
| `documents/01-business/kitehub/auth-2fa/api-contract.md` | Contract doc (target GAP-547) | `ls documents/01-business/kitehub/auth-2fa/` | folder absent | 🆕 to-be-created (Bucket 0 Foundation) |
| `FeedbackController.java` | Java class | `find kitehub -name FeedbackController.java` | 1 match `.../feedback/controller/` | ✅ exists |
| `kitehub-gateway/src/main/resources/application*.yml` | Gateway config | `ls kitehub/kitehub-gateway/src/main/resources/application*.yml` | 2 files | ✅ exists |
| `kitehub.feedback.*` / `kitehub.onboarding.*` / `kitehub.beta-status.*` / `kitehub.support.*` config keys | rules.md keys (GAP-555 target) | `grep -rn "^- \`kitehub\." documents/01-business/{feedback,onboarding,beta-status,support}/rules.md` | 15+ keys documented | ✅ documented |
| `@Value("\${kitehub.feedback...")` | Java @Value wiring | `grep -rn "kitehub.feedback" kitehub/kitehub-subscription/src/main/java` | 1 match (survey-cron only) | 🆕 to-be-created (Bucket A — wire 14 remaining) |
| `CookieConsent.tsx` | FE component (target GAP-558) | `find kitehub/kitehub-frontend/src -name "CookieConsent*"` | 0 matches | 🆕 to-be-created (Bucket B) |
| `documents/01-business/cookie-consent/` | Business domain folder | `ls documents/01-business/cookie-consent/ 2>&1` | "No such directory" | 🆕 to-be-created (Bucket 0 Foundation) |
| `kitehub-frontend/src/app/(dashboard)/staff/` | FE route (target GAP-561) | `find kitehub/kitehub-frontend/src -path '*dashboard/staff*'` | 0 matches | 🆕 to-be-created (Bucket B) |
| `kitehub-subscription/.../staff/` | BE module (target GAP-561) | `find kitehub/kitehub-subscription -path '*subscription/staff*'` | 0 matches | 🆕 to-be-created (Bucket B) |
| `V45__create_staff_invitations.sql` | Flyway migration (target GAP-561) | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V45*` | 0 matches | 🆕 to-be-created (Bucket B) |
| `V46__create_rbac_roles.sql` | Flyway migration (target GAP-562) | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V46*` | 0 matches | 🆕 to-be-created (Bucket B) |
| `documents/01-business/roles/` | Business domain folder (target GAP-562) | `ls documents/01-business/roles/ 2>&1` | "No such directory" | 🆕 to-be-created (Bucket 0 Foundation) |
| `PasswordResetController.java` | Java class (target GAP-548) | `find kitehub -name PasswordResetController.java` | 0 matches | 🆕 to-be-created (Bucket C) |
| `SecurityConfig.java` | Java class | `find kitehub/kitehub-subscription -name SecurityConfig.java` | 1 match `.../auth/config/` | ✅ exists (target GAP-552 modify) |
| `TotpSecretCipher.java` | Java class (Wave 72b ship) | `find kitehub/kitehub-subscription -name TotpSecretCipher.java` | 1 match `.../totp/` | ✅ exists (target GAP-553 modify) |
| `OnboardingProgressController.java` | Java class (Wave 78 ship) | `find kitehub/kitehub-subscription -name OnboardingProgressController.java` | 1 match `.../onboarding/controller/` | ✅ exists (target GAP-554 modify) |
| `FeedbackWidget.tsx` + `OnboardingChecklist.tsx` | FE components (target GAP-545) | `find kitehub/kitehub-frontend/src -name 'FeedbackWidget*'` | 1 match each | ✅ exists |
| `BetaDisclaimerBanner.tsx` | FE component | `find kitehub/kitehub-frontend/src -name 'BetaDisclaimerBanner*'` | 1 match | ✅ exists |
| `DatabaseBackupServiceTest.java` + `InstanceControllerIntegrationTest.java` | Test classes (target GAP-544) | `find kitehub -name 'DatabaseBackupServiceTest*'` | 1 match each | ✅ exists |
| `documents/05-guides/user-manual/anonymous/` | Doc folder F1 sample (target GAP-563 + GAP-537 anonymous) | `ls documents/05-guides/user-manual/ 2>&1` | "No such directory" | 🆕 to-be-created (Bucket F1) |
| `.claude/rules/user-manual-content-standard.md` | Meta rule file (target GAP-563) | `ls .claude/rules/user-manual-content-standard.md 2>&1` | "No such file" | 🆕 to-be-created (Bucket F1) |
| `scripts/render-user-manual-pdf.sh` | Render script (target Bucket F1) | `ls scripts/render-user-manual-pdf.sh 2>&1` | "No such file" | 🆕 to-be-created (Bucket F1) |
| `kitehub-frontend/src/app/help/anonymous/` | Next.js MDX route (target Bucket F1) | `find kitehub/kitehub-frontend/src/app/help 2>&1` | "No such directory" | 🆕 to-be-created (Bucket F1) |
| `kitehub-admin/.../impersonation/` | BE module (target GAP-040) | `find kitehub/kitehub-admin -path '*impersonation*'` | 0 matches | 🆕 to-be-created (Bucket F-bis) |

Banned shortcuts: không `| head` truncation, không skip verification "because agents will check", không aspirational ref thiếu 🆕 flag.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|---|---|---|
| 0 | `bash scripts/check-business-docs-3-layer.sh documents/01-business/{auth-2fa,roles,cookie-consent}/` + `pnpm -F kitehub-frontend lint` | wave-plan-completeness + audit-gate.py |
| A | `cd kitehub && ./mvnw -pl kitehub-gateway,kitehub-subscription clean verify -Dcheckstyle.skip=true` + `grep -c "@Value(.\${kitehub" kitehub/kitehub-subscription/src/main/java -r` → ≥15 | kitehub-ci + gateway-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify` + `pnpm -F kitehub-frontend test --run -- staff cookie-consent` + `pnpm -F kitehub-frontend build` | kitehub-ci + frontend-ci |
| C | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -Dtest='*Security*,*Totp*,*PasswordReset*,*Onboarding*'` | kitehub-ci |
| D | `pnpm -F kitehub-frontend test --run -- FeedbackWidget Sidebar BetaDisclaimer` + `pnpm -F kitehub-frontend build` + `bash scripts/check-readme-freshness.sh` | frontend-ci |
| E | `cd kitehub && ./mvnw -pl kitehub-subscription test -Dtest='DatabaseBackupServiceTest,InstanceControllerIntegrationTest'` (CI Docker available) + `bash scripts/check-business-docs-3-layer.sh documents/01-business/{support,kitehub}` | kitehub-ci |
| F1 | `bash scripts/check-rule-frontmatter.sh .claude/rules/user-manual-content-standard.md` + `bash scripts/render-user-manual-pdf.sh anonymous` (generates PDF; verify exit 0) + `pnpm -F kitehub-frontend build` (verify MDX route) + reviewer manual 15-item checklist on 5 sample pages | rule-frontmatter + frontend-ci + reviewer |
| F2 (DEFER) | (gated on F1 dev review approval; F2 spawn in Wave 80+ when template validated) | — |
| F-bis | `cd kitehub && ./mvnw -pl kitehub-admin clean verify -Dtest='*Impersonation*'` + `pnpm -F kitehub-admin-frontend test` + audit log row presence verify | kitehub-ci + reviewer |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- Bucket 0 Foundation spawn FIRST, sequential merge (~45min)
- 7 buckets (A, B, C, D, E, F1, F-bis) spawn parallel với `run_in_background: true` sau khi Bucket 0 merge — F2 DEFER Wave 80+ gated trên F1 dev review
- Worktree isolation per bucket (`isolation: worktree`) cho parallel safety
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge sequentially (rebase from main mỗi bucket trước merge) sau all background completions
- Bucket B split risk: nếu effort >4h sau Foundation, split B1 (cookie consent) + B2 (invite-staff + RBAC) per `release-fix-retry-budget.md` pivot
- 7 buckets parallel là edge-case cho solo-dev (per `wave-pack-planner` optimal 4-5); disjoint check pass → acceptable. F1 + F-bis low-risk add-on (docs + isolated module) — không tăng coordination cost meaningful

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Mỗi bucket PR update affected GAP file Log + status
- Mỗi bucket PR commit body include `AUDIT_OVERRIDE:` trailer cite Wave 79 closure audit suite obligation (post-wave-audit-mandate §2.4.1)
- ROADMAP §🚀 Next Action update trong closure PR
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Rule 15 enforcement)
- Sub-gaps file cho any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- `bash scripts/prune-merged-worktrees.sh --yes` prune worktree husks + merged branches
- **`## Release Plan Progress` section trong closure PR body** — per `feedback_wave_closure_release_progress_report.md` rules #1-6: current Phase + milestone progress + wave contribution + trigger gates + estimated remaining wall-clock + **Waves Remaining table** (3 rows: strict-min v0.9.0-beta / practical v0.9.0-beta / v1.0.0 PROD với explicit wave numbers + GAP IDs + PR #s)
- Post-wave audit suite trigger trong vòng 3 ngày sau Wave 79 closure merge per `post-wave-audit-mandate.md` §2.4.1 (multi-domain — NOT eligible §2.4 milestone deferral)

---

## 8. Log

- **2026-05-14** (draft): Plan created. Inside-out queue (13 items) từ Wave 78 post-wave audit suite (5 audits parallel). Outside-in persona audit (4 personas: P2 Owner/P3 Manager/Anonymous/Platform Admin) surface 5 new gaps (GAP-558..562) — applied `outside-in-coverage-trigger.md` Bước 4 BEFORE plan PR opens. Zero overlap inside-out ↔ outside-in confirms rule rationale.
- **2026-05-14** (draft restructure): Bucket F split into F1 (sample + meta rule, ship Wave 79) + F2 (rest 3 personas, DEFER Wave 80+) + F-bis (impersonation, independent) sau outside-in user manual audit (4 personas × 5 questions: discovery/format/cognitive-load/VN-edu/trust-gates). Added GAP-563 (META P1 — user manual content review standard rule file) per `outside-in-coverage-trigger.md` Bước 5 + `meta-gap-priority.md` §3 force-multiplier. Recurring outside-in miss pattern logged in memory `feedback_outside_in_recurring_miss.md` — Wave 73 + Wave 79 Bucket F user-caught both. Outside-in findings integrated: Intercom-style web manual + PDF auto-gen + annotated screenshots 1440×900 + 375×812 vi-VN + Fuse.js search + ≥3 discoverability entry points. F1 spawn ONLY anonymous-prospect persona (lowest-risk validate template).
