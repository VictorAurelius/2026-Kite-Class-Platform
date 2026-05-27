---
title: RST findings — Wave meta-6 human walk 2026-05-28
status: complete
created: 2026-05-28
phase: wave-meta-6
wave: meta-6
audit-type: rst-human-walk
findings_count: 8
gaps_filed: [GAP-783, GAP-784, GAP-785]
gaps_extended: [GAP-782]
related_prs: [#1914, #1915, #1916]
audience: dev
---

# RST findings — Wave meta-6 human walk 2026-05-28

## Scope

Real-Story-Testing (RST) cycle session 2026-05-28 — Owner persona (`owner.test@test.vn`) walk Wave meta-6 staff-invite happy path qua kitehub-frontend production-equivalent stack (post `kitehub/scripts/build-all.sh` + `up.sh` rebuild). Per `e2e-rst-test-layer-boundary.md` §2.2 RST scope = "manual exploratory walk surfacing bugs E2E specs missed".

Walk **blocked at Bước 2.6** (Owner POST /api/v1/staff-invitations → 403 ACCESS_DENIED). 8 bugs surfaced trên path từ Bước 1 (login) → Bước 2.5 (gateway routing). Walk continuation defer tới session sau khi P0 bug #8 fixed.

## 8 findings

| # | Bug | Severity | Class | Discovery context | Fix status |
|---|---|---|---|---|---|
| 1 | Duplicate BetaDisclaimerBanner trên `/onboarding` (page mounts explicitly + DashboardLayout already mounts) | P2 | **Component duplicate mount** | Browser screenshot post-Owner-login redirect | ✅ PR #1914 |
| 2 | Login page thiếu "Ghi nhớ đăng nhập" (remember-me) checkbox | P2 | **UX feature gap** | User flagged "khá bất tiện" sau login fresh | ✅ PR #1915 |
| 3 | Route guard mismatch: `/admin/staff/invite` AdminLayout enforces `isPlatformAdmin`, page comment says "Owner-only" | P1 | **Role-route alignment** | Owner login → navigate `/admin/staff/invite` → bounce to /login | ✅ PR #1916 |
| 4 | Gateway TenantResolver fail cho PLATFORM_ADMIN (no `tenantId` claim) | N/A (by-design) | **Architectural — admin vs tenant scope** | PLATFORM_ADMIN walk attempt → "Could not resolve tenant from request" gateway log | docs-only — note in RST |
| 5 | Wave 80 parallel impl `kitehub-subscription/staff/*` (36 files) + Wave meta-6 `kiteclass-core/module/staff/*` duplicate same URL `/api/v1/staff-invitations` | P2 | **Architecture conflict — multi-impl** | Security audit F-002 + audit-gateway-routes.sh script flag 5 wrong-service findings | 🟡 GAP-782 Bucket C (Wave-future cleanup) |
| 6 | RabbitMQ queue `class.rescheduled.queue` không auto-declared at kiteclass-core startup → `/actuator/health` 503 → circuit breaker stuck open | P1 | **Dev-stack provisioning gap** | kiteclass-core log: `NOT_FOUND - no queue 'class.rescheduled.queue' in vhost '/'`. Workaround: manual `rabbitmqadmin declare queue` | 🔵 GAP-785 |
| 7 | FE `(admin)/admin/staff/invite/page.tsx` (Wave 80 era) sends `{email, fullName}`, Wave meta-6 BE requires `role` → 400 VALIDATION_ERROR | P1 | **API contract drift — FE-BE schema** | Manual curl with `{email, fullName}` → 400 "role: must not be blank" | 🔵 GAP-784 |
| 8 | Owner JWT role `OWNER` → `@PreAuthorize("hasAnyRole('OWNER',...)")` returns 403 ACCESS_DENIED. Spring Security `hasRole('OWNER')` requires authority `ROLE_OWNER` prefix; JWT filter likely không add `ROLE_` prefix when converting claim → authority | **P0** | **Recurrent: JWT role → Spring authority mapping drift** (2nd occurrence — Wave 71b admin login 500 was 1st) | curl với role=TEACHER → 403 | 🔴 GAP-783 |

## Findings by class

### Class A — Recurrent governance pattern (file rule consideration)

**Bug #8 — JWT role claim → Spring Security authority mapping** (2nd occurrence):
- Wave 71b incident (2026-05-13): admin login 500 → `PLATFORM_ADMIN` JWT vs FE `'ADMIN'` guard mismatch
- 2026-05-28 walk: `OWNER` JWT → BE `hasAnyRole('OWNER')` 403 fail
- Pattern: role string flows through layers (JWT issuance → JWT filter → Spring Security → @PreAuthorize) without consistent normalization
- Existing rule covers symptom only (`pre-handoff-self-test-completeness.md` §2.4 admin-flow (a)→(g)) but doesn't enforce JWT-authority-mapping audit at controller addition time

Per `incident-to-rule-pipeline.md` 5-stage Stage 1 (Detect) ✓ + Stage 2 (Classify) ✓ (no existing rule mandates JWT role authority mapping verify in api-contract-audit) → recurrence threshold ≥2 met → **rule candidate** trong follow-up.

### Class B — API contract FE-BE drift detector gap

**Bug #7 — FE InviteStaffPage role param missing**:
- BE Wave meta-6 `InviteStaffRequest.role` required field
- FE Wave 80 page sends `{email, fullName}` only
- api-contract-audit (PR #1907) scored 94/100 nhưng audited BE-only — doesn't verify FE call site matches BE DTO

Per `audit-skill-rubric-api-contract-audit.md` extension needed: Cat 2 Request Schema audit should grep FE call sites for endpoint + verify payload shape ↔ BE @Valid DTO.

### Class C — Dev-stack provisioning gap detector

**Bug #6 — RabbitMQ queue auto-declare missing**:
- kiteclass-core declares queue via Spring AMQP `@RabbitListener` annotation
- User account in RabbitMQ may not have `configure` permission on the queue → fails silently
- Service `/actuator/health` returns 503 because broker binding unhealthy
- Manual workaround: `rabbitmqadmin declare queue name=class.rescheduled.queue durable=true`

Existing ops-readiness audit (PR #1910 Wave 92 76/100) didn't catch — audit doesn't probe broker queue existence vs service `@RabbitListener` declarations.

### Class D — Component duplicate mount detector (Bug #1)

`BetaDisclaimerBanner` mounted at 3 sites: `DashboardLayout`, `AdminLayout`, `onboarding/page.tsx` explicit. When customer layout uses DashboardLayout + page mounts again → duplicate. Per `pre-launch-owasp-rest-hardening-checklist.md` deferred detector class — could add grep audit for components mounted in both layout + page.

### Class E — Role-route alignment audit (Bug #3)

Wave 80 page comment `// Owner-only` vs Wave meta-6 AdminLayout `isPlatformAdmin` gate. **No audit detects comment ↔ route guard divergence**. Potential rule: "page route comment role claim ↔ layout role guard MUST match OR be explicitly justified".

## Walk session log (chronological)

```
17:50  Stack: down → build-all.sh → up.sh (Docker rebuild Wave meta-6 latest)
17:52  Gateway routing fix F-002 applied (PR #1916 base)
18:01  PR #1914 duplicate banner fix shipped local rebuild
18:08  Login owner.test@test.vn → bounced to /onboarding
18:09  Bug #1 discovered (duplicate banner) → fix shipped PR #1914
18:15  Login attempt blocked by 503 transient — gateway warm-up
18:18  Bug #2 discovered (no remember-me) → fix shipped PR #1915
18:21  Navigate /admin/staff/invite → bounced to /login
18:22  Bug #3 discovered (role-route mismatch) → fix shipped PR #1916
18:23  admin.test login → invite fails — TenantResolver no tenantId claim
18:24  Bug #4 discovered (TenantResolver per-tenant scope architectural)
18:25  Owner login retry → still 503 — Bug #5 (Wave 80 dead code + multi-impl)
18:26  curl direct test → connection refused kiteclass-core:8080
18:29  Bug #6 discovered (RabbitMQ class.rescheduled.queue missing) → manual declare
18:32  kiteclass-core healthy after queue declare + restart
18:33  Owner POST {email,fullName} → 400 "role: must not be blank" → Bug #7 (API drift)
18:33  Retry với role=TEACHER → 403 ACCESS_DENIED → Bug #8 (P0 JWT authority mapping)
18:35  Walk blocked. Codify session begins.
```

## Recommendations (per `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion)

| Finding | E2E spec promotion candidate | Owner |
|---|---|---|
| #1 Duplicate banner | Visual regression test: `/onboarding` page renders ≤1 BetaDisclaimerBanner element | FE team next wave |
| #2 Remember-me | E2E spec: login với checkbox → close browser → reopen → still logged in | FE team |
| #3 Route guard mismatch | E2E spec: Owner login → /admin/staff/invite → renders form (not bounce) | FE team |
| #6 RabbitMQ queue | Ops health smoke: post-stack-up verify `actuator/health` returns 200 not 503 | Ops |
| #7 API drift | api-contract IT extension: assert InviteStaffRequest schema ↔ FE call payload | BE team — GAP-784 |
| #8 OWNER 403 | Controller IT extension: Mock JWT với role=OWNER → @PreAuthorize pass | BE team — GAP-783 |

## Cross-references

- Wave meta-6 plan: `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md`
- Wave meta-6 audit suite (5 audits): PR #1908 UI / #1909 Security / #1910 Ops / #1911 Business / #1912 Performance
- GAP-782 Wave meta-6 follow-ups (consolidated audit-gate hook violations)
- `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (Bug #8 falls under (a) role match BE seed vs FE guard scope — extending to JWT-authority-mapping)
- `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate

## Meta-pattern observation

**1 walk session surfaced 8 bugs across 5 distinct classes** trong Wave meta-6 mà 5 Phase 3 audits + Phase 2 25 tests + Phase 2 api-contract audit 94/100 PASS đều **không catch**. Audit scope-fit reality:

- Code-level audits read code without executing → miss runtime/config/network drift
- Component tests (Mockito) bypass real Spring Security filter → JWT-authority mapping bug invisible
- api-contract audit BE-only → FE-BE drift bug invisible
- ops-readiness audit didn't probe broker queue existence → provisioning gap invisible

→ **Human RST walk irreplaceable** cho catching the 5 classes above. Per `e2e-rst-test-layer-boundary.md` `META P0 force-multiplier`:  RST cycle là canonical E2E coverage source — promotion to automated specs codifies the boundary.
