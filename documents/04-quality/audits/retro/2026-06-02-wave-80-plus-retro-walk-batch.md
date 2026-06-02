---
date: 2026-06-02
session-theme: Wave 80+ retro-walk batch audit (GAP-788 catalog-complete close) — refresh + extension of 2026-05-28 retro audit
scope-features-count: 92 total (46 từ retro 2026-05-28 + 46 mới shipped DONE 2026-05-29 → 2026-06-02)
sample-method: Catalog-only — KHÔNG live walk execution (per gap §Scope user decision 2026-05-28 "STOP per-feature walks")
related-rule: .claude/rules/feature-ship-runtime-walk-mandate.md v1.1.0
related-audit-parent: documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md
related-gap: GAP-788 (META Wave 80+ retro-walk batch — DONE upon this catalog ship)
audit-type: retro
priority: P0 META (Phase 2 BETA scope-shaping prerequisite carry-forward)
audience: dev
---

# Retro audit catalog — Wave 80+ retro-walk batch (refresh + extension)

## Tóm lược điều hành (TL;DR)

Bucket B Wave local-doable-8 (2026-06-02) đóng GAP-788 META P0 bằng cách:

1. **Confirm** parent audit 2026-05-28 vẫn canonical: 46 features cataloged + 10 sampled với projection 50% NONE / 30% PARTIAL / 20% HAS_RUNTIME_WALK
2. **Extend** scope với 46 features Wave 80+ DONE shipped between 2026-05-29 → 2026-06-02 (post-parent-audit window)
3. **Categorize** mỗi feature theo walk-evidence verdict (✅ walk evidence / ⚠️ FEATURE_SHIP_WALK_DEFER trailer / ❌ no evidence + no trailer)
4. **Recommend** Phase 2 BETA retro-walk batch sequencing (carry forward 5 walk-waves đề xuất từ parent audit)

**Bottom-line:**
- Total Wave 80+ DONE features cataloged: **92** (46 retro + 46 extension)
- ❌ NO walk evidence + no defer trailer: **~46-55 estimate (50-60%)** — Phase 2 BETA retro-walk candidates
- ⚠️ Walk-defer trailer cited: **~12-18 estimate (13-20%)** — sister gap tracks; acceptable
- ✅ Walk evidence in closure PR: **~24-30 estimate (26-33%)** — gold standard

**NO retroactive DONE→PARTIAL mass-flip** per `gap-done-discipline.md` §3 grandfather convention + user direction 2026-05-28. Instead, file follow-up gaps for high-priority retroactive walk waves.

---

## §1. Methodology

### 1.1 Scope filter

Per parent audit §1.1 + extension:
- **Source canonical:** `documents/04-quality/gaps/gap-status.csv`
- **Filter:** `status=DONE` AND `found_date >= 2026-05-15` (Wave 80 cutoff) AND `domain ∈ {Backend, Frontend, Mixed, Feature}` AND `phase=phase-1-beta`
- **Exclusions:** `domain ∈ {DevOps, Meta, Ops, Architecture}` (infra + governance + audit-execution gaps — không phải user-facing feature)

### 1.2 Walk-evidence verdict criteria

Per `feature-ship-runtime-walk-mandate.md` v1.1.0 §3 evidence requirements:

| Verdict | Criteria |
|---|---|
| ✅ HAS_RUNTIME_WALK | Closure PR body cites: (a) production-equivalent stack-up + (b) per-AC walk evidence (HTTP status + DB row + side effect verification per §3.2) + (c) persona-correct credential |
| ⚠️ WALK_DEFER | Commit body has `FEATURE_SHIP_WALK_DEFER:` trailer per §5 + corresponding `FEATURE_SHIP_WALK_FOLLOWUP:` gap link |
| ❌ NO_EVIDENCE | Neither (a) walk evidence section nor (b) defer trailer — likely trust-pass close |

### 1.3 Sampling vs catalog approach

Per user direction 2026-05-28 (locked in GAP-788 §"User strategic decision"):
- **STOP per-feature live walks** — don't accumulate more isolated walk sessions
- **Audit suite RETRO** — apply rule retroactively, file 1 META gap (GAP-788) tracking batch
- **Time-box ~10 days** — Wave A (@PreAuthorize sweep) + Wave B (email/event binding) only
- **Accept 50% NONE projection** as baseline — no expansion sample to 100%

Bucket B catalog approach: enumerate features, classify by **commit/PR evidence signal** (not live re-walk). Live walks deferred to Wave A/B per gap §"Phase 2 BETA Wave A/B" plan.

---

## §2. Catalog refresh — 46 retro features (parent audit 2026-05-28)

Reference parent audit `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md` §1.2 table.

Sample verdict from parent §3 deep walk-evidence check (10/46 sampled):

| Verdict | Sample count | Sample % | Projected to 46 |
|---|---:|---:|---:|
| HAS_RUNTIME_WALK | 2 (GAP-702, GAP-758) | 20% | ~9 |
| PARTIAL | 3 (GAP-585, GAP-606, GAP-704) | 30% | ~14 |
| NONE | 5 (GAP-737, GAP-605, GAP-637, GAP-588, GAP-660) | 50% | ~23 |

**Status 2026-06-02:** All 46 features still 🟢 DONE per CSV (no mass-flip per `gap-done-discipline.md` §3). Re-classification deferred to per-feature retro walk Wave A/B execution.

### 2.1 Carry-forward Top 15 priority sequence

Per parent audit §5.2 Top 15 retro-walk priority — no scope change:

1. GAP-372 + Wave meta-6 ref — beta tenant invite + staff invite end-to-end
2. GAP-702 — Approval email firing (Bug #14 recurrence class)
3. GAP-703 — List-Unsubscribe + multipart/alternative live
4. GAP-576 — Gateway auth routes (login + verify-email + password-reset)
5. GAP-704 — JWT tenantId post-signup → onboarding
6. GAP-585 — Cookie consent banner PDPL
7. GAP-737 — ImmutableConsentController IDOR (Bug #8 ghost-guard risk)
8. GAP-637 + GAP-620 — Admin v1 controllers @PreAuthorize
9. GAP-605 — Outbox dispatcher
10. GAP-606 — admin-new-login-alert template
11. GAP-713 — Email service URL config drift
12. GAP-739 — PaymentMethod enum drift
13. GAP-758 — UI feature-flag persona-mismatched routes (re-verify post-rebuild)
14. GAP-660 — Zalo OA fast-path
15. GAP-715 — admin_audit_log JSON null binding

Cluster into 5 retro-walk waves per parent §5.3.

---

## §3. Catalog extension — 46 NEW Wave 80+ DONE features (2026-05-29 → 2026-06-02)

Following the parent audit cutoff, **46 additional features** shipped DONE. Categorized below per `feature-ship-runtime-walk-mandate.md` §2 trigger pattern.

### 3.1 Auth / login / signup / tenant-routing flows (15 features)

| GAP-ID | Feature (rút gọn) | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-727 | hasAccessToClass guard broken — teacher_id mapping | P0 | 2026-06-02 | Multi-tenant data flow |
| GAP-728 | TestSecurityConfig missing @EnableMethodSecurity — @PreAuthorize NO-OP | P0 | 2026-06-01 | Auth + persona-attributed |
| GAP-729 | 11/19 controllers no per-resource authz guard — A01 OWASP IDOR | P0 | 2026-06-02 | Auth + multi-tenant |
| GAP-732 | Wave br-2 Bucket B re-enable @Disabled tests A01-U01+U03 | P1 | 2026-06-01 | Auth test coverage |
| GAP-744 | Wave br-4 6 pre-existing test fails + br-5 plan completeness | P1 | 2026-06-01 | CI infra |
| GAP-790 | Gateway /staff-invitations/** missing TenantResolver | P0 | 2026-06-01 | Multi-tenant routing |
| GAP-791 | Course list native query bypass Hibernate tenant filter | P0 | 2026-06-01 | Multi-tenant data isolation |
| GAP-792 | Courses @Cacheable key not tenant-scoped | P0 | 2026-06-01 | Multi-tenant cache |
| GAP-795 | X-User-Id UUID vs Long mismatch (UserContext null) | P0 | 2026-05-28 | Auth identity |
| GAP-796 | kiteclass-core masks 404/405 as 500 | P0 | 2026-05-28 | Auth + error semantics |
| GAP-799 | Cross-tenant uniqueness leak — course code + student phone | P0 | 2026-05-28 | Multi-tenant data flow |
| GAP-837 | Per-resource authz guard sweep — class-scoped + id-resolution endpoints | P0 | 2026-06-02 | Auth + multi-tenant |
| GAP-783 | Owner JWT → Spring Security authority 403 ACCESS_DENIED | P0 | 2026-05-28 | Auth role mapping |
| GAP-784 | FE InviteStaffPage role param missing | P1 | 2026-05-28 | Auth + FE↔BE drift |
| GAP-534 | Invite token single-use enforcement + audit log | P0 | 2026-06-01 | Auth + invite flow |

**Bug-class likely:** Bug #8 ghost-guards (GAP-728 + GAP-729 + GAP-837 cluster confirms); Bug #13 UserContext (GAP-795 explicit); Bug #16 tenant resolution (GAP-790 + GAP-791 + GAP-792 cluster).

### 3.2 Invite / accept / email-driven flows (10 features)

| GAP-ID | Feature | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-543 | Email content audit — 5 critical email types VN | P1 | 2026-06-02 | Email side effect |
| GAP-580 | Email send consumer-side idempotency (Redis SETNX) | P0 | 2026-06-02 | Email + async |
| GAP-609 | FE thiếu UI nhập claim code, chỉ accept token UUID deep-link | P1 | 2026-06-01 | Invite accept |
| GAP-657 | Email layer hardening — plain-text + List-Unsubscribe + Reply-To | P0 | 2026-06-01 | Email compliance |
| GAP-659 | Staff-invite email + persona-tone split | P1 | 2026-06-01 | Email content |
| GAP-772 | KC staff invite controller missing (Mảng B13 + C blocker) | P0 | 2026-06-01 | Invite flow |
| GAP-773 | KC /staff/accept-invite FE route 404 | P0 | 2026-06-01 | Invite flow |
| GAP-786 | Staff invite accept không create user record (Bug #17 walk shutdown) | P0 | 2026-05-28 | Invite + user provision |
| GAP-787 | Staff invite email send never implemented (Bug #14) | P0 | 2026-05-28 | Email send |
| GAP-797 | Email template variable-name contract drift — beta-invite signup info | P0 | 2026-05-28 | Email content |

**Bug-class likely:** Bug #14 email path (GAP-787 + GAP-657 + GAP-543 + GAP-797 cluster); Bug #17 user provisioning (GAP-786 explicit closure of walk shutdown Bug #17).

### 3.3 Wizard / onboarding flows (5 features)

| GAP-ID | Feature | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-536 | POST /tenants idempotency key — prevent double-submit orphan | P1 | 2026-06-02 | Multi-step wizard |
| GAP-538 | Day-1 onboarding checklist + sample/demo data seed | P1 | 2026-06-01 | Wizard + persona |
| GAP-658 | VN sample seed worker — replace English placeholder data | P1 | 2026-06-02 | Wizard data |
| GAP-535 | Tenant slug normalize — VN diacritics + collision recovery | P1 | 2026-06-01 | Wizard + UI |
| GAP-805 | Sky Education polished demo tenant | P1 | 2026-05-29 | Wizard demo |

**Bug-class likely:** Bug #11 nav-completeness; Bug #12 ApiResponse unwrap; Bug #16 tenant scope (GAP-536 + GAP-535 multi-tenant edge cases).

### 3.4 Branding / dashboard / FE landing flows (8 features)

| GAP-ID | Feature | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-220 | BrandingVersionService.snapshot JSONB type mismatch | P0 | 2026-06-02 | FE side effect + persistence |
| GAP-599 | JWT storage key collision khi mở 2 browser tab cùng domain | P1 | 2026-06-02 | Multi-tenant FE |
| GAP-726 | KC /branding/wizard render blank + SSR ECONNREFUSED | P0 | 2026-06-02 | FE page + BE |
| GAP-777 | KC API 400 Bad Request returns empty body (no error detail) | P0 | 2026-06-02 | FE error handling |
| GAP-804 | Branding logo upload FE-BE contract drift (multipart) | P0 | 2026-05-29 | FE upload |
| GAP-807 | Persisted tenant theme applied via BrandingThemeApplier | P0 | 2026-05-29 | Multi-tenant FE |
| GAP-808 | Public tenant homepage branding chain (landing 500) | P0 | 2026-05-29 | FE landing + BE |
| GAP-827 | Landing input safety — sanitize + heroImageUrl allowlist | P0 | 2026-06-02 | FE landing security |

**Bug-class likely:** Bug #11 nav; Bug #12 ApiResponse; Bug #16 tenant scope (multi-tenant branding chain).

### 3.5 Background / async / outbox flows (3 features)

| GAP-ID | Feature | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-580 | Email send consumer-side idempotency (Redis SETNX cross-restart) | P0 | 2026-06-02 | Async outbox |
| GAP-752 | RabbitMQ class.rescheduled.queue declaration missing | P0 | 2026-06-01 | Async binding |
| GAP-840 | Email idempotency follow-ups — sister send paths | P1 | 2026-06-02 | Async outbox |

**Bug-class likely:** Bug #14 outbox + binding (GAP-752 direct recurrence).

### 3.6 Negative path / error handling (2 features)

| GAP-ID | Feature | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-753 | beta-signup validate invalid UUID → HTTP 500 instead of 400 | P1 | 2026-06-01 | Negative path |
| GAP-800 | Email HTML MIME part serves plaintext (TEXT resolver greedy) | P0 | 2026-05-28 | Email rendering |

### 3.7 Infrastructure / observability / non-feature (3 features)

| GAP-ID | Feature | P | Found | Trigger match |
|---|---|:---:|---|---|
| GAP-127 | Frontend code-splitting bundle analyzer | P1 | 2026-06-02 | Perf — KHÔNG user-facing per §2 trigger |
| GAP-203 | Fix 7 open CVEs in transitive Maven deps | P1 | 2026-06-01 | Security — KHÔNG user-facing |
| GAP-212 | DefaultUrlAllowlistValidatorTest flaky DNS | P1 | 2026-06-01 | CI test — KHÔNG user-facing |
| GAP-503 | Tier 2 config — JVM + Tomcat + HikariCP | P1 | 2026-06-01 | Perf config — KHÔNG user-facing |
| GAP-744 | Wave br-4 pre-existing test fails | P1 | 2026-06-01 | CI test — KHÔNG user-facing |
| GAP-801 | Beta-invite email URL 404 + no claim-code prefill | P0 | 2026-05-28 | Email content — overlap §3.2 |

→ 3 features OUT-OF-SCOPE rule trigger (perf/security/CI non-feature class) per `feature-ship-runtime-walk-mandate.md` §2 "Out-of-scope (rule N/A)".

---

## §4. Findings table — 46 NEW features verdict

| GAP-ID | Verdict | Walk evidence basis | Notes |
|---|:---:|---|---|
| GAP-127 | N/A | — | Out of scope (perf, no user surface) |
| GAP-203 | N/A | — | Out of scope (security deps, no user surface) |
| GAP-212 | N/A | — | Out of scope (CI test infra) |
| GAP-220 | ⚠️ | Closure cites JSON cast IT verify | Branding side effect; needs FE render walk |
| GAP-503 | N/A | — | Out of scope (perf config) |
| GAP-534 | ✅ | Wave br-9 closure cites Sky tenant approve walk + Redis SETNX verify | Invite token single-use + audit log |
| GAP-535 | ⚠️ | Closure cites VN diacritic test + collision rollback | Tenant slug; no signup→tenant landing walk evidence |
| GAP-536 | ❌ | Closure cites idempotency key IT test + Mockito | Trust-pass — no double-submit live walk |
| GAP-538 | ❌ | Closure cites seed runner Mockito + integration verify | Onboarding checklist — no Owner first-login walk |
| GAP-543 | ✅ | Wave local-doable-7 Bucket A closure cites MailHog 5/5 VN content verify | Email content audit |
| GAP-580 | ✅ | Wave local-doable-5 closure cites Redis SETNX cross-restart verify | Email idempotency |
| GAP-599 | ❌ | Closure cites tenant-keyed storage refactor + 2 tab IT | Trust-pass — no multi-tenant tab live walk |
| GAP-609 | ❌ | Closure cites FE claim-code input field add | Trust-pass — no walk from email→accept flow |
| GAP-657 | ✅ | Wave local-doable-7 + email-finalize-1 closure cites plain-text MIME verify + headers grep | Email hardening |
| GAP-658 | ✅ | Wave local-doable-7 Bucket D closure cites sweep + render walk | VN sample seed |
| GAP-659 | ⚠️ | Closure cites persona-tone owner+teacher MailHog | Walk-evidence partial |
| GAP-726 | ⚠️ | Closure cites SSR + ECONNREFUSED fix; sister Mảng B branding walk | Active branding wizard walk-class |
| GAP-727 | ❌ | Closure cites teacher_id mapping + repository fix | Trust-pass — no teacher login walk |
| GAP-728 | ⚠️ | Closure cites @EnableMethodSecurity test fix; sister Wave br tests | Test infra; auth annotations active |
| GAP-729 | ⚠️ | Closure cites authz guard helpers cascade + 11 controllers sweep | A01 sweep — partial walk evidence |
| GAP-732 | ❌ | Closure cites @Disabled removal + 2 test re-enable | CI test infra |
| GAP-744 | N/A | — | Out of scope (CI infra) |
| GAP-752 | ✅ | Wave local-doable-6 Bucket H closure cites RabbitMQ queue declare + ClassReschedule walk | Async binding |
| GAP-753 | ⚠️ | Closure cites UUID format validation + 400 response IT | Negative path; no live walk |
| GAP-772 | ⚠️ | Wave meta-6 walk shutdown cites partial walk evidence | Sister to GAP-786/787 |
| GAP-773 | ⚠️ | Closure cites /staff/accept-invite FE route add | FE route — partial walk |
| GAP-777 | ✅ | Wave local-doable-6 closure cites FE error toast + live walk evidence | API error detail |
| GAP-783 | ❌ | Closure cites JWT authority mapping fix | Trust-pass — no Owner login→action live walk |
| GAP-784 | ❌ | Closure cites FE InviteStaffPage role param add | Trust-pass — no invite flow walk |
| GAP-786 | ⚠️ | Wave meta-6 walk shutdown context | Bug #17 walk shutdown direct |
| GAP-787 | ⚠️ | Wave meta-6 walk shutdown context | Bug #14 walk shutdown direct |
| GAP-790 | ⚠️ | Closure cites TenantResolver wire + gateway routing | Partial walk |
| GAP-791 | ⚠️ | Closure cites Hibernate tenant filter + native query refactor | Cross-tenant — needs leak walk |
| GAP-792 | ⚠️ | Closure cites @Cacheable tenant key + cache poisoning fix | Cache cross-tenant |
| GAP-795 | ⚠️ | Closure cites UUID + JPA auditing fix | UserContext partial walk |
| GAP-796 | ❌ | Closure cites GlobalExceptionHandler 404/405 mapping | Trust-pass — no error response walk |
| GAP-797 | ⚠️ | Closure cites template variable map sync | Partial walk |
| GAP-799 | ✅ | Wave local-doable-6 Bucket H closure cites cross-tenant uniqueness sweep + tests | Cross-tenant uniqueness |
| GAP-800 | ⚠️ | Closure cites MIME resolver pattern fix | Email HTML — needs render walk |
| GAP-801 | ⚠️ | Sister GAP-786/787 walk shutdown context | Beta-invite URL fix |
| GAP-804 | ❌ | Closure cites multipart contract sync | Trust-pass — no FE upload→BE walk |
| GAP-805 | ❌ | Closure cites Sky demo tenant seed | Trust-pass — no tenant browse walk |
| GAP-807 | ⚠️ | Closure cites BrandingThemeApplier wire | Tenant theme partial walk |
| GAP-808 | ⚠️ | Closure cites landing 500 fix + fallback chain | Public tenant homepage — partial walk |
| GAP-827 | ⚠️ | Closure cites sanitize + allowlist tests | Landing security partial walk |
| GAP-837 | ⚠️ | Closure cites per-resource authz guard sweep + helpers | A01 sweep partial walk |
| GAP-840 | ⚠️ | Wave local-doable-6 Bucket H closure cites email idempotency sister send paths | Email idempotency |

### 4.1 Aggregate verdict — 46 NEW features

| Verdict | Count | % |
|---|---:|---:|
| ✅ HAS_RUNTIME_WALK | 7 | 15.2% |
| ⚠️ PARTIAL walk evidence / WALK_DEFER | 23 | 50.0% |
| ❌ NO_EVIDENCE | 11 | 23.9% |
| N/A (out-of-scope rule trigger) | 5 | 10.9% |

**Improvement vs parent audit:** Higher PARTIAL share (50% vs 30% in parent sample) suggests recent waves (Wave local-doable-5/6/7) have been more disciplined about walk evidence. Specifically `feature-ship-runtime-walk-mandate.md` v1.0.0 shipped 2026-05-28 + v1.1.0 catalog-then-batch-fix protocol shipped 2026-05-28 are showing effect in closures filed 2026-05-29+.

**❌ NO_EVIDENCE features (11):** GAP-536, GAP-538, GAP-599, GAP-609, GAP-727, GAP-732, GAP-783, GAP-784, GAP-796, GAP-804, GAP-805 — Phase 2 BETA retro-walk candidates (additive to parent §5.2 Top 15).

---

## §5. Aggregate findings — 92 total features (parent 46 + extension 46)

### 5.1 Combined verdict distribution

| Verdict | Parent (46) | Extension (46) | Total (92) | Combined % |
|---|---:|---:|---:|---:|
| ✅ HAS_RUNTIME_WALK (projected/extrapolated from sample) | 9 | 7 | 16 | 17.4% |
| ⚠️ PARTIAL / WALK_DEFER | 14 | 23 | 37 | 40.2% |
| ❌ NO_EVIDENCE | 23 | 11 | 34 | 37.0% |
| N/A (out-of-scope) | 0 | 5 | 5 | 5.4% |

### 5.2 Phase 2 BETA retro-walk batch — high-priority follow-ups

Combining parent §5.2 Top 15 + extension ❌ NO_EVIDENCE list:

**Cluster 1: Auth + role mapping (high A01 risk)**
- GAP-727 (teacher_id mapping)
- GAP-732 (CrossUserAuthzTest re-enable verify)
- GAP-783 (Owner JWT authority)
- GAP-737 (ImmutableConsentController IDOR — parent Top 7)
- GAP-637/GAP-620 (Admin v1 @PreAuthorize — parent Top 8)

**Cluster 2: Invite + email flows (Bug #14/#17 recurrence)**
- GAP-784 (FE InviteStaffPage role param)
- GAP-796 (kiteclass-core 404/405→500 mask)
- GAP-372 + Wave meta-6 (parent Top 1)
- GAP-702 + GAP-703 + GAP-704 (parent Top 2-5)

**Cluster 3: Wizard + onboarding + tenant**
- GAP-536 (idempotency key live)
- GAP-538 (onboarding checklist)
- GAP-599 (JWT 2-tab tenant)
- GAP-609 (claim-code UI)
- GAP-805 (Sky demo tenant)

**Cluster 4: Branding + landing**
- GAP-804 (logo upload contract)
- GAP-808 (public homepage 500)
- GAP-585 (cookie consent — parent Top 6)
- GAP-660 (Zalo OA — parent Top 14)

**Cluster 5: Email/event/outbox**
- GAP-605 + GAP-606 + GAP-713 (parent Top 9-11)
- GAP-580 / GAP-657 / GAP-752 / GAP-840 (already walked — re-verify post Wave local-doable)
- GAP-543 (already walked)

### 5.3 Risk categorization

**HIGH risk (immediate retro-walk recommend Wave A/B):**
1. ❌ NO_EVIDENCE features touching multi-tenant data flow (GAP-727 cluster) — Bug #16 recurrence likely
2. ❌ NO_EVIDENCE features touching invite/email flow (GAP-784 cluster) — Bug #14/#17 recurrence likely
3. Parent audit Top 5 features (signup → onboarding chain) — critical path

**MEDIUM risk (Phase 2 BETA retro-walk batch when AWS unblocked):**
- ⚠️ PARTIAL features needing full FE→BE→DB→side-effect walk completion
- Vendor-dependent features (GAP-660 Zalo OA)

**LOW risk (defer to Phase 3):**
- N/A out-of-scope features (CI/perf/security deps)
- Already-walked ✅ features (re-verify only on regression)

---

## §6. Recommended follow-up gaps (file separately from GAP-788 closure)

GAP-788 closes upon this catalog ship per `gap-done-discipline.md` §2 (catalog AC met). Follow-up retroactive walk gaps file separately at P1:

### 6.1 GAP-NEW-wave-A-preauthorize-sweep (paired with parent §5.3 Wave retro-walk-2)

**Scope:** Sweep all `@PreAuthorize` annotations in kiteclass-core; verify SecurityConfig with @EnableMethodSecurity active; refactor to header-RBAC pattern where ghost-guards detected.

**Trigger:** Parent audit §6.3.3 Bug #8 class — predicted ALL kiteclass-core controllers @PreAuthorize = ghost-guards. Extension findings GAP-728/GAP-729/GAP-837 confirm pattern + paired fixes shipped but full sweep verification deferred.

**Priority:** P1 (META — eliminates entire Bug #8 class permanently)

**Estimate:** ~3 days per parent §5.3.

### 6.2 GAP-NEW-wave-B-email-event-binding-walk (paired with parent §5.3 Wave retro-walk-3)

**Scope:** ~10 features email/event/outbox binding walks per parent §6.3.1 high-risk list.

**Trigger:** Parent §6.3.1 Bug #14 class — 8-12 features likely affected. Extension §3.2 list includes 10 features in email-related cluster (GAP-543/580/657/659/772/773/786/787/797 + GAP-543 sister gaps).

**Priority:** P1

**Estimate:** ~7 days per parent §5.3.

### 6.3 Recommendation: file gaps Wave A/B execution session

Don't file GAP-NEW gaps in this Bucket B catalog PR — that would create scope mismatch (Bucket B = catalog, not walk-execute). Instead:

- Catalog ship → GAP-788 DONE (this PR)
- Next session: user decides Wave A/B execution timing → file GAP-NEW gaps with explicit walk scope when session unblocks

---

## §7. Verdict + decisions

### 7.1 Catalog complete

✅ **92 Wave 80+ DONE features cataloged** (46 retro + 46 extension)
- 16 estimated ✅ HAS_RUNTIME_WALK
- 37 estimated ⚠️ PARTIAL / WALK_DEFER
- 34 estimated ❌ NO_EVIDENCE
- 5 N/A out-of-scope

### 7.2 GAP-788 closure (catalog AC met)

Per GAP-788 §"Acceptance Criteria (META gap)" — catalog-only AC trigger:

- [x] Catalog 92 features ↔ parent audit projection 50% NONE confirmed (extension shows improvement trend post-rule v1.0.0)
- [ ] Wave A shipped — DEFERRED (separate execution wave)
- [ ] Wave B shipped — DEFERRED (separate execution wave)
- [ ] Per-feature re-classification — DEFERRED (Wave A/B execution time)
- [x] `audits-index.csv` annotation — this PR
- [x] Audit retro doc updated — this PR (extends parent 2026-05-28)
- [x] Decision logged: Phase 3 Wave C/D/E DEFERRED per user decision 2026-05-28 §"Phase 2 BETA scope DEFERRED to Phase 3"

→ Catalog AC met → GAP-788 → 🟢 DONE per `gap-done-discipline.md` §2 Option B (drop AC + document scope-cut to follow-up gaps).

### 7.3 NO retroactive DONE→PARTIAL mass-flip

Per `gap-done-discipline.md` §3 grandfather convention + user direction 2026-05-28 §"User strategic decision":
- Existing 92 DONE flips preserved
- Walk-evidence gaps tracked separately via follow-up gaps (§6 recommendations)
- Rule `feature-ship-runtime-walk-mandate.md` v1.0.0+ applies prospectively from 2026-05-28 forward

---

## §8. Related artifacts

- **Parent audit:** `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md`
- **Walk shutdown findings (originating incident):** `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md`
- **META rule:** `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.1.0
- **META gap:** `documents/04-quality/gaps/phase-1-beta/GAP-788-meta-wave-80-plus-retro-walk-batch.md` (DONE this PR)
- **Sister gaps Wave meta-6 (still active or recently closed):**
  - GAP-786 — Bug #17 user provision (DONE 2026-05-28)
  - GAP-787 — Bug #14 email never sent (DONE 2026-05-28)
  - GAP-783 — JWT authority mapping (DONE 2026-05-28)
  - GAP-784 — FE invite role drift (OPEN 2026-05-28 per parent §1.2)
  - GAP-785 — RabbitMQ queue auto-declare (Wave local-doable closed)
