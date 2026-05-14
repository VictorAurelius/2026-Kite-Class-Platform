---
title: Wave 79 — Beta Invite Close-Out (v1.0.0-rc gate + multi-user RBAC + PDPL compliance)
status: draft
created: 2026-05-14
updated: 2026-05-14
waves: [79]
gaps: [GAP-040, GAP-537, GAP-544, GAP-545, GAP-547, GAP-548, GAP-551, GAP-552, GAP-553, GAP-554, GAP-555, GAP-556, GAP-557, GAP-558, GAP-559, GAP-560, GAP-561, GAP-562]
---

# Wave 79 — Beta Invite Close-Out

**Goal:** Đóng cổng `v1.0.0-rc` Phase 1 BETA — fix 6 P0 (3 inside-out audit findings + 3 outside-in persona findings) + 9 P1 (defense-in-depth security + UX retention + docs/test hygiene). Sau Wave 79, Beta invite có thể launch end-to-end với P3 Manager flow live + PDPL cookie consent + RBAC role separation.

**Trigger:** Wave 78 post-wave audit suite 2026-05-14 (5 audits parallel) surface 3 P0 v1.0.0-rc-gate blockers + 7 P1 follow-ups. Outside-in persona audit 4-persona walkthrough (P2 Owner / P3 Manager / Anonymous Prospect / Platform Admin) cùng ngày surface 3 P0 multi-user/compliance + 2 P1 UX/discoverability — class hoàn toàn khác inside-out (dev audit catch internal correctness; persona audit catch multi-user + VN compliance + UX discoverability).

**Estimated wall-clock:** ~8-12h. Bucket 0 Foundation ~45 min sequential (4 contract files + RBAC role rules + cookie consent rules). 6 buckets parallel ~4-6h longest bucket (Bucket B P0 outside-in — 3 cross-cutting flows: cookie consent + invite-staff + RBAC).

---

## 1. Brainstorm

### Q1 (alignment — inside-out + outside-in scope coverage)

**Inside-out queue (13 items, from Wave 78 audit suite + carry-forward):**

| Source | Gap | Priority | Class |
|---|---|---|---|
| API Contract audit | GAP-547 | P0 | 2FA endpoints undocumented + unversioned |
| Security audit | GAP-551 | P0 | Feedback endpoint missing gateway route + null tenantId |
| Business Logic audit | GAP-555 | P0 | 15+ config keys documented không wired qua @Value |
| API Contract audit | GAP-548 | P1 | Password-reset BE controller missing (gateway forwards 404) |
| Security audit | GAP-552 | P1 | SecurityConfig default-allow fallback |
| Security audit | GAP-553 | P1 | TOTP cipher + JWT secret dev-default không fail-fast |
| Security audit | GAP-554 | P1 | Onboarding X-Tenant-Id không cross-check JWT claim |
| UI audit | GAP-545 | P1 | Dialog focus-trap + Escape (WCAG 2.1.1) trên FeedbackWidget + OnboardingChecklist |
| Carry-forward | GAP-544 | P1 | testcontainers/H2 migration cho subscription integration tests |
| Carry-forward | GAP-537 | P1 | User manual screenshots per persona |
| Carry-forward | GAP-040 | P1 | Support impersonation |
| Business Logic audit | GAP-556 | P1 | Support rules.md misleading — BE not implemented (Footer only) |
| Business Logic audit | GAP-557 | P1 | use-cases.md thiếu BR-xxx refs + admin role compat không vào BR-AUTH |

**Outside-in additions (5 items, from persona audit 2026-05-14 pre-Wave-79):**

| Persona | Gap | Priority | Friction class |
|---|---|---|---|
| Anonymous | GAP-558 | **P0** | Cookie consent banner thiếu trên public site (PDPL Art 11 + Decree 13/2023 Art 4 — hard deadline 2026-07-01) |
| P3 Manager | GAP-561 | **P0** | invite-staff email template + BE endpoint + FE UI **đều missing** — P3 persona flow KHÔNG ship được |
| P3 Manager | GAP-562 | **P0** | RBAC role separation Customer vs Staff missing — Manager invited thấy thẻ tín dụng + AI Branding của Owner (sensitive data leak) |
| P2 Owner | GAP-559 | P1 | /onboarding entry point invisible — dashboard CTA + Sidebar nav missing |
| P2 Owner | GAP-560 | P1 | Beta disclaimer banner thiếu specificity — data-reset policy doc missing |

**Inside-out ↔ outside-in overlap analysis:** Zero overlap. Inside-out dev audit catch internal correctness (auth/feedback/support hardening); outside-in persona audit catch multi-user + compliance + UX discoverability — class hoàn toàn khác. Confirms `outside-in-coverage-trigger.md` rationale: dev blind spot psychology + multi-user testing.

**Persona coverage Wave 79:**
- P2 Owner (primary): GAP-559 + GAP-560 + GAP-561 + GAP-562 (multi-user RBAC)
- P3 Manager (daily): GAP-561 + GAP-562 (entire persona flow currently broken)
- Anonymous (pre-conversion): GAP-558 (PDPL gate)
- Platform Admin (internal): GAP-551 (admin sees feedback; backend routing fix indirectly enables admin tooling)

### Q2 (trade-offs)

- **GAP-558 cookie consent — vendor SaaS (Cookiebot/Osano) vs in-house:** chọn **in-house lightweight** — vendor adds ~$30-50/mo overhead + extra script tag dependency Phase 1 BETA chưa cần. In-house: 1 component cookie banner + localStorage opt-in/opt-out + gate analytics fire. Risk: PDPL compliance audit defer to counsel-engaged Phase 3.
- **GAP-561 invite-staff — re-use beta-access email pipeline vs new path:** chọn **re-use** kitehub-email infra (template loader + Resend send) — chỉ add `invite-staff.ftl` template + BE `POST /api/v1/staff-invitations` endpoint + FE form trong dashboard "Quản lý nhân sự". 1 BE migration cho `staff_invitations` table.
- **GAP-562 RBAC — full role matrix vs MVP 2-role:** chọn **MVP 2-role (Owner / Staff)** Phase 1 BETA. Full role matrix (Owner / Manager / Teacher / Accountant / Receptionist) Phase 2 (Wave 80+). MVP đủ unblock P3 Manager flow + ngăn data leak; Manager-only role placeholder cho Phase 2.
- **GAP-555 config wiring approach — bulk @Value scan vs spring-cloud-config:** chọn **bulk @Value annotation scan** — 15 keys × ~30s mỗi key = <10 min. Spring Cloud Config infra overhead Phase 2.
- **GAP-547 2FA versioning — rename `/api/auth/2fa/*` → `/api/v1/auth/2fa/*`:** chọn **add `/api/v1/` prefix với backward-compat alias** (gateway double-route 30 days), break v0.9 callers Phase 2 cleanup. Saves rebase rollback của PR #1301.
- **Bucket parallelism:** 6 buckets parallel (+1 foundation). Bucket A + B both touch BE auth + RBAC area — split A=P0 contract+routing, B=P0 invite+RBAC để giảm conflict surface.

### Q3 (risks)

- **Bucket A GAP-551 gateway routing scope:** `currentTenantId()` hard-code `return null;` — depends on `XUserRolesHeaderFilter` propagating JWT claim. Verify filter chain order trước fix; risk regress nếu filter chưa attach `tenantId`.
- **Bucket B GAP-562 RBAC breaking change:** Existing PLATFORM_ADMIN role + ADMIN FE guard (Wave 78 GAP-518 compat) cần migrate sang OWNER. Mapping cũ giữ alias 30 days. Risk session invalidate trên existing dev tenants.
- **Bucket B GAP-558 cookie consent — analytics fire timing:** Existing `<script>` tags fire trên page load TRƯỚC banner consent. Cần defer all analytics tags qua `<Script strategy="afterInteractive">` + onConsent callback. Risk: GA / Mixpanel mất dữ liệu Phase 1 BETA (acceptable — beta scale nhỏ).
- **Bucket B GAP-561 multi-user impact:** Wave 79 ship sẽ là LẦN ĐẦU multi-user flow live. Test matrix:
  - Owner gửi invite → Staff nhận email → click link → setup password → first login → dashboard scoped
  - Staff không thấy thẻ tín dụng / AI Branding / billing
  - Owner thấy Staff trong "Quản lý nhân sự"
- **Bucket C GAP-548 password-reset BE controller — orphan endpoint risk:** Gateway rate-limit (PR #1354) đã wired cho route nhưng BE controller missing. Add `POST /api/auth/password-reset-request` + `POST /api/auth/password-reset-confirm` + email pipeline.
- **Bucket D GAP-559 onboarding entry — Sidebar nav scope:** Add nav item "Bắt đầu" / "Hướng dẫn nhanh" trong Sidebar; dashboard top CTA "Hoàn tất setup (3/5)" — cần i18n key + persona check (Owner thấy; Staff không thấy).
- **Bucket E test infra GAP-544 testcontainers:** `DatabaseBackupServiceTest` + `InstanceControllerIntegrationTest` hardcode `:5433`. Migrate qua testcontainers ALPINE postgres image. Risk: CI runner Docker availability — kiểm tra `.github/workflows/kitehub-ci.yml` đã có docker-in-docker.
- **Cross-cutting concurrent migration risk:** Bucket B GAP-561 + GAP-562 cần V45+V46 migrations. Per `concurrent-production-mutation-ops.md`: serialize Flyway runs; KHÔNG run V45 + V46 trong cùng deploy slot.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Class | Owner | Effort | Disjoint? |
|---|---|---|---|---|---|
| **0 Foundation** | GAP-547 + GAP-555 docs only (contract + config registry rules.md); GAP-562 RBAC role rules.md + use-cases.md; GAP-558 cookie consent rules.md | Contract docs + business rules | bg-agent | ~45 min | ✅ documents/01-business/* + MSW handlers; no BE/FE code |
| **A P0 v1.0.0-rc gate** | GAP-547 (2FA contract + version) + GAP-551 (feedback gateway route + tenantId fix) + GAP-555 (15 config keys @Value wiring) | Inside-out audit P0 | bg-agent | ~3-4h | ✅ documents/01-business/kitehub/auth-2fa + kitehub-gateway/application.yml + kitehub-subscription/feedback + 15-key @Value scan |
| **B P0 outside-in invite enablement** | GAP-558 (cookie consent component + analytics gate) + GAP-561 (invite-staff template + BE endpoint + FE form) + GAP-562 (RBAC OWNER/STAFF role + role-guard FE + scope filter BE) | Outside-in persona P0 | bg-agent | ~4-6h | ✅ kitehub-frontend public + dashboard staff + kitehub-subscription new module staff-invitation + V45 + V46 migrations |
| **C P1 cluster security defense-in-depth** | GAP-548 (password-reset BE controllers) + GAP-552 (SecurityConfig default-deny) + GAP-553 (TOTP/JWT fail-fast guard) + GAP-554 (X-Tenant-Id JWT cross-check) | Inside-out audit P1 | bg-agent | ~2-3h | ✅ kitehub-subscription auth module + SecurityConfig + onboarding controller |
| **D P1 UX retention** | GAP-545 (dialog focus-trap Radix migrate) + GAP-559 (Sidebar nav + dashboard CTA "Hoàn tất setup") + GAP-560 (disclaimer specificity + data-reset policy doc) | Inside-out + outside-in P1 | bg-agent | ~2-3h | ✅ kitehub-frontend dashboard sidebar + onboarding card + beta disclaimer banner + new doc data-reset-policy.md |
| **E P1 docs+test hygiene** | GAP-544 (testcontainers migration cho 2 tests) + GAP-556 (support rules.md scope note Wave 78 DISCOVERABILITY ONLY) + GAP-557 (use-cases.md BR-refs + BR-AUTH-009 role alias) | Test infra + docs sync | bg-agent | ~2h | ✅ kitehub-subscription tests + documents/01-business/* |
| **F P1 user manual + support** | GAP-537 (user manual screenshots per persona — 4 personas × 5-10 screens) + GAP-040 (support impersonation BE + FE admin "View as tenant" mode) | Internal tooling + docs | bg-agent | ~3-4h | ✅ documents/05-guides/user-manual + kitehub-admin impersonation module |

**Disjoint check:**
- Bucket 0: only `documents/01-business/{auth-2fa,roles,cookie-consent}/`, `kitehub-frontend/src/test/msw/handlers/` (3 NEW domain folders + 1 update)
- Bucket A: only `documents/01-business/kitehub/auth-2fa/api-contract.md` + `kitehub-gateway/src/main/resources/application*.yml` + `kitehub-subscription/feedback/**` + `@Value` annotation scan across 5 BE modules
- Bucket B: only `kitehub-frontend/src/app/(public)/components/CookieConsent*` + `(dashboard)/staff/**` + `kitehub-subscription/src/main/java/.../staff/**` (new module) + V45 (staff_invitations) + V46 (rbac_roles)
- Bucket C: only `kitehub-subscription/src/main/java/.../auth/{controller,config}/**` (no overlap với Bucket B staff module)
- Bucket D: only `kitehub-frontend/src/components/{Sidebar,BetaDisclaimer,FeedbackWidget,OnboardingChecklist}/**` + `documents/05-guides/operations/data-reset-policy.md` (new doc)
- Bucket E: only `kitehub-subscription/src/test/**` (2 test files) + `documents/01-business/support/rules.md` + `kitehub/auth/use-cases.md`
- Bucket F: only `documents/05-guides/user-manual/**` (new screenshots) + `kitehub-admin/src/main/java/.../impersonation/**` + admin FE "View as tenant" button

**Wave-pack parallelism:** 6 buckets + 1 foundation = 7 worktrees. Foundation MERGE FIRST (~45min) → 6 buckets spawn parallel. Per `wave-pack-planner` skill: optimal 4-5 parallel for solo-dev mode; 6 buckets edge-case nhưng disjoint check pass → acceptable.

---

## 3. Dependencies + sequencing

```
Bucket 0 Foundation (P0 docs)
   │
   ├──► Bucket A (P0 v1.0.0-rc gate: 547+551+555) ──┐
   │                                                  │
   ├──► Bucket B (P0 outside-in: 558+561+562) ─────┐ │
   │                                                │ │
   ├──► Bucket C (P1 cluster security: 548+552+553+554)
   │                                                │ │
   ├──► Bucket D (P1 UX: 545+559+560) ─────────────┘ │
   │                                                  │
   ├──► Bucket E (P1 docs+tests: 544+556+557) ────── ┘
   │
   └──► Bucket F (P1 user manual: 537+040)

   All 6 buckets ──► Wave 79 closure PR (status:complete + ROADMAP §🚀 + wave-history.jsonl)
                       │
                       └──► Post-wave audit suite (5 audits parallel within 3 days)
```

**Parallel track (user-action deploys — không gate Wave 79 merge):**
- Wave 77 Resend dashboard verify + DKIM CNAME + terraform apply + Day 1-7 warm-up
- kitehub-email deploy + actuator healthcheck SSM verify
- 3× credential rotations (`bash scripts/rotate-leaked-credentials.sh --cred=...`)
- kitehub-subscription deploy → V39-V44 auto-apply
- Wave 79 V45 + V46 sẽ deploy sau Wave 79 merge

---

## 4. Acceptance Criteria (wave-level)

- [ ] **Audit P0 close-out (3):** GAP-547 + GAP-551 + GAP-555 status=DONE; v1.0.0-rc gate unblocked per API Contract + Security + Business Logic audit reports
- [ ] **Outside-in P0 close-out (3):** GAP-558 (cookie consent live + analytics gated) + GAP-561 (P3 Manager E2E flow: invite → email → setup password → first login → dashboard scoped) + GAP-562 (RBAC OWNER/STAFF role separation — Staff KHÔNG thấy billing/branding của Owner)
- [ ] **P1 cluster security (4):** GAP-548 + GAP-552 + GAP-553 + GAP-554 status=DONE
- [ ] **P1 UX retention (3):** GAP-545 + GAP-559 + GAP-560 status=DONE
- [ ] **P1 docs/tests hygiene (3):** GAP-544 + GAP-556 + GAP-557 status=DONE
- [ ] **P1 internal tooling (2):** GAP-537 + GAP-040 status=DONE or PARTIAL với follow-up gap
- [ ] **CI green:** all 16 required checks SUCCESS trên Wave 79 closure PR
- [ ] **Post-wave audit suite kicked off:** 5 audits scheduled within 3 days per `post-wave-audit-mandate.md` §2.4.1
- [ ] **ROADMAP §🚀 updated:** Wave 79 row + Phase 1 BETA P0 count decremented (17 → 11 PARTIAL OR less)
- [ ] **wave-history.jsonl Rule 15:** Wave 79 entry appended

---

## 5. Risk register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| GAP-562 RBAC migration session-invalidate Owner sessions | Medium | Medium | Backward-compat alias 30 days; user re-login prompt với clear msg |
| GAP-561 invite-staff email Resend rate-limit (Day 0-7 warm-up) | Low | Low | Per Wave 77 Resend runbook — warm-up cap 100/day Day 0-3 |
| GAP-558 cookie consent analytics-fire break GA dashboards | Medium | Low (beta scale) | Document acceptable trade-off; Phase 1 BETA scale insignificant |
| Bucket A `@Value` wiring miss key (15 keys × 4 domain) | Medium | Medium | Grep `kitehub\.(feedback\|onboarding\|beta-status\|support)\.` cross-check rules.md vs Java |
| GAP-544 testcontainers Docker-in-Docker CI | Low | Low | If CI Docker missing → add to `.github/workflows/kitehub-ci.yml` setup-buildx step |
| Bucket B 3 P0 cùng bucket → effort overflow >6h | Medium | Medium | Split sub-bucket B1/B2/B3 nếu effort >4h after Foundation |
| V45 + V46 concurrent apply (per `concurrent-production-mutation-ops.md`) | Medium | High | Serialize: V45 staff_invitations DONE → V46 rbac_roles. Document trong deploy plan |

---

## 6. Post-wave audit suite plan (per `post-wave-audit-mandate.md` §2.1)

Wave 79 touches multi-domain (FE + BE + content + infra Docker test config) → NOT eligible §2.4 milestone deferral. Required:

| Audit | Trigger |
|---|---|
| UI /128 | kitehub-frontend changes (Bucket A B D F) |
| API Contract /100 | NEW endpoints (2FA contract, password-reset, staff-invitations, impersonation) + version policy enforcement |
| Business Logic /100 | rules.md changes (Bucket 0 + E) + 15-key @Value wiring (Bucket A) |
| Security /100 | RBAC migration (Bucket B) + auth controllers (Bucket C) + cookie consent (Bucket B) |
| Quality /110 | Weekly refresh post-wave |
| Ops Readiness /100 | Testcontainers infra change (Bucket E) + V45+V46 migrations |

**Cadence:** scheduled ≤3 days after Wave 79 closure merge. Each bucket PR commits `AUDIT_OVERRIDE:` trailer citing this closure-audit obligation.

---

## 7. Definition of Done (Wave 79 closure)

- ✅ 18 gaps status=DONE OR PARTIAL với follow-up gap filed
- ✅ ROADMAP §🚀 updated; Phase 1 BETA P0 count ≤ 11 PARTIAL
- ✅ wave-history.jsonl entry appended (Rule 15)
- ✅ 6 audit reports filed within 3 days
- ✅ v1.0.0-rc gate UNBLOCKED (3 P0 audit findings closed)
- ✅ P3 Manager E2E flow live (GAP-561 + GAP-562 cross-verify)
- ✅ PDPL cookie consent live on public site
- ✅ Wave 80 candidates queued: full RBAC matrix (Manager/Teacher/Accountant/Receptionist) + advanced premium plan + UI kit polish remaining

---

## 8. References

- **Audit reports driving Wave 79:**
  - `documents/04-quality/audits/api-contract/2026-05-14-post-wave-78.md` (76/100 B-, FAIL P0)
  - `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` (89/100 B+, 1 P0 BLOCKING)
  - `documents/04-quality/audits/business-logic/2026-05-14-post-wave-78.md` (71/100 C+, FAIL ≥3 P0)
  - `documents/04-quality/audits/ui/2026-05-14-post-wave-78.md` (112/128 A+, PASS)
  - `documents/04-quality/audits/quality/2026-05-14-post-wave-78-quality-refresh.md` (87/110 B+, PASS)
  - `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (4-persona walkthrough)
- **Rules applied:**
  - `.claude/rules/outside-in-coverage-trigger.md` — outside-in audit BEFORE plan PR
  - `.claude/rules/post-wave-audit-mandate.md` — audit suite within 3 days
  - `.claude/rules/contract-first-for-cross-layer.md` — Bucket 0 Foundation merge first
  - `.claude/rules/concurrent-production-mutation-ops.md` — V45 + V46 serialize
  - `.claude/rules/release-fix-retry-budget.md` — Bucket B 3 P0 split if effort >4h
- **Cross-wave queue:** `documents/03-planning/inside-out-queue.md` (Premium plan deferred Wave 80+)
- **Previous wave:** `wave-2026-05-14-78-beta-invite-launch-retain.md` (status:complete)
