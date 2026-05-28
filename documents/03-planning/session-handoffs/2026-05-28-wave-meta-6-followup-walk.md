---
audience: dev
date: 2026-05-28
session-theme: Wave meta-6 follow-up + Owner human walk RST + 8 PRs
prs_created: [1905, 1906, 1907, 1908, 1909, 1910, 1911, 1912, 1913, 1914, 1915, 1916, 1917]
prs_merged: [1900, 1902, 1903, 1904, 1901]
gaps_filed: [GAP-782, GAP-783, GAP-784, GAP-785]
gaps_updated: [GAP-772]
main_head_at_close: 57935a55
walk_status: BLOCKED at Bug #8 GAP-783 P0
---

# Session handoff — Wave meta-6 closure + RST walk findings — 2026-05-28

## Session scope shipped

**1. Wave meta-6 core merge** (Bucket A staff-invite BE + Bucket B closure rule v1.0.1 + Bucket C RST HTML + plan patch V64→V71):
- PR #1900 plan, #1902 patch, #1903 rule, #1904 BE, #1901 RST HTML → all merged to main HEAD `57935a55`

**2. Wave meta-6 follow-up Phase 1+2+3** (per `post-wave-audit-mandate.md` §3 3-day SLA):
- Phase 1 sync PR #1905 (GAP-772 PARTIAL + GAP-782 filed)
- Phase 2 wave-pack (2 Opus agents):
  - PR #1906 — 25 Mockito/MVC tests (Agent A)
  - PR #1907 — 3-layer business docs + api-contract audit 94/100 (Agent B)
- Phase 3 audit suite (5 Opus agents wave-pack):
  - PR #1908 UI 109.0/128 A ✅
  - PR #1909 Security 86/100 A- ✅ (4 P1)
  - PR #1910 Ops 76/100 C+ ⚠️ PARTIAL FAIL (3 P0 carry GAP-612/257/144)
  - PR #1911 Business 74/100 C ⚠️ PARTIAL FAIL (1 P0 + 6 P1)
  - PR #1912 Performance 85/100 B+ ✅

**3. F-002 gateway routing fix** (Security audit critical finding):
- PR #1913 — remove explicit `kitehub-staff-invitations` route, let catch-all route to kiteclass-core (Wave meta-6 canonical)

**4. Owner human walk RST cycle** (per `e2e-rst-test-layer-boundary.md` §2.2 manual exploratory):
- PR #1914 — duplicate BetaDisclaimerBanner fix `/onboarding`
- PR #1915 — login "Ghi nhớ đăng nhập" remember-me checkbox feature
- PR #1916 — AdminLayout allow OWNER access `/admin/staff/*` sub-routes
- PR #1917 — RST findings doc + 3 gaps (GAP-783 P0 + GAP-784 P1 + GAP-785 P1)

## Open PRs at close (12 — all need CI verify + merge)

| PR | Title | Mergeable | Recommended order |
|---|---|---|---|
| #1905 | sync GAP-772 PARTIAL + GAP-782 | docs-only auto-merge | 1 (foundation) |
| #1906 | Wave meta-6 25 tests | code, requires CI | 2 |
| #1907 | 3-layer business docs + api-contract audit | docs-only auto-merge | 3 |
| #1908-1912 | 5 audit reports | docs-only auto-merge | 4 (batch) |
| #1913 | gateway routing F-002 fix | config (yml), requires CI | 5 |
| #1914 | duplicate banner fix | code (.tsx), requires CI | 6 |
| #1915 | login remember-me | code, requires CI | 7 |
| #1916 | admin layout Owner /admin/staff | code, requires CI | 8 |
| #1917 | RST findings + 3 gaps | docs-only auto-merge | 9 |

## Walk status — BLOCKED

Walk reached **Bước 2.6** (Owner POST `/api/v1/staff-invitations`) → **HTTP 403 ACCESS_DENIED** at BE controller `@PreAuthorize`. Per RST findings:

| Bug # | Bug | Severity | Fix status |
|---|---|---|---|
| 1 | Duplicate BetaDisclaimerBanner | P2 | ✅ PR #1914 |
| 2 | No remember-me checkbox | P2 | ✅ PR #1915 |
| 3 | Route guard mismatch `/admin/staff/invite` | P1 | ✅ PR #1916 |
| 4 | TenantResolver requires JWT claim | N/A architectural | docs only |
| 5 | Wave 80 + meta-6 multi-impl dead code | P2 | 🟡 GAP-782 Bucket C Wave-future |
| 6 | RabbitMQ `class.rescheduled.queue` not auto-declared | P1 | 🔵 GAP-785 — manual workaround applied |
| 7 | FE InviteStaffPage role param drift | P1 | 🔵 GAP-784 |
| 8 | **Owner JWT @PreAuthorize 403** | **P0** | 🔴 **GAP-783** (blocker) |

**Walk continuation blocked** until GAP-783 fixed (~10 phút Option A `@PreAuthorize hasAnyRole` → `hasAnyAuthority` syntax change).

## RabbitMQ workaround applied (local-only)

Queue declared manually on local stack so kiteclass-core could boot:

```bash
docker exec kite-rabbitmq rabbitmqadmin -u kitehub -p $RABBITMQ_PASS \
  declare queue name=class.rescheduled.queue durable=true
```

Production fix tracked GAP-785 (auto-declare via @RabbitListener config OR seed in up.sh).

## Recurring class identified

**JWT role → Spring Security authority mapping drift** (2nd occurrence):
- Wave 71b (2026-05-13) admin login 500 — BE seed `PLATFORM_ADMIN` vs FE guard `'ADMIN'`
- Wave meta-6 walk (2026-05-28) — JWT role `OWNER` vs Spring authority `ROLE_OWNER`

Per `incident-to-rule-pipeline.md` §3 recurrence threshold ≥2 met. Rule candidate trong follow-up:
- Pre-launch JWT-authority-mapping audit check trong api-contract-audit Cat 4 OR pre-launch-auth-hardening-checklist §2.X new sub-check

## Audit observations (meta-pattern)

Wave meta-6 Phase 2+3 had **6 audits PASS** (api-contract 94, UI 109, Security 86, Ops 76, Perf 85, Business 74) + 25 tests PASS, NHƯNG **8 bugs surfaced trong 1 RST walk**. Audits miss because:
- Code-level audits read code without execution
- Component tests (Mockito) bypass real Spring Security filter
- api-contract-audit BE-only — không verify FE call payload shape
- ops-readiness audit không probe broker queue existence

Per `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate — human RST walk irreplaceable cho these 5 classes.

## Next session priority

### P0 (walk unblock — ~15 phút)

1. Fix GAP-783 — `@PreAuthorize` syntax (Option A) HOẶC JWT filter add ROLE_ prefix (Option B)
2. Rebuild + restart kiteclass-core
3. Resume walk from Bước 2.6 → invite happy path 201 → DB row → email → accept flow

### P1 walk-related (~1-2h)

4. GAP-784 — FE InviteStaffPage add role dropdown + dispatch role param
5. GAP-785 — RabbitMQ queue auto-declare investigation + fix scope decision

### Phase 4 wave closure paperwork (task #10 still pending)

6. Merge 12 open PRs trong dependency order
7. Update ROADMAP §🎯 with Wave meta-6 final close + RST findings
8. wave-history.jsonl Wave meta-6 entry (done by this session-handoff PR)

### Wave-future scope

9. GAP-782 Bucket C — Wave 80 dead code cleanup (kitehub-subscription/staff/* + V45/V49/V57 migration rollback)
10. Meta rule candidate — JWT-authority-mapping audit (per incident-to-rule-pipeline.md recurrence ≥2)
11. api-contract-audit Cat 2 extension — verify FE call payload ↔ BE DTO

## Context state at handoff

- Context budget: **74%** (per `session-end-context-check.md` §3 heads-up zone)
- Main HEAD: `57935a55` (Wave meta-6 Bucket C RST HTML last merge)
- Local Docker stack: full kitehub+kiteclass stack UP healthy (RabbitMQ queue manually declared)
- Local feature branches active: fix/onboarding-duplicate-banner, feat/login-remember-me, fix/admin-layout-owner-staff-access, fix/wave-meta-6-followup-3-gateway-fix, docs/wave-meta-6-rst-walk-findings, docs/session-handoff-2026-05-28-wave-meta-6 (this PR)

## References

- Wave meta-6 plan: `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md`
- Wave meta-6 RST findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md`
- 4 new gaps: GAP-782, GAP-783, GAP-784, GAP-785 (all phase-1-beta)
- 12 open PRs: 1905-1917 (exclude already-merged 1900-1904)
