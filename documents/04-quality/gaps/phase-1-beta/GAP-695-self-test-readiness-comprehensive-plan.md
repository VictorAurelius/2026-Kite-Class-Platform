# GAP-695: Self-test readiness — comprehensive gap catalog + dependency-ordered fix plan

**Status:** 🟡 PARTIAL 50% — Phase 0 catalog SHIPPED 2026-05-21; Tier 0 (Bucket A Docker preflight + .env) + Tier 1 (Bucket D admin login + gateway routing) shipped 2026-05-21 Wave 102.8; Tier 2-3 execution pending Wave 102.9+
**Priority:** 🔴 P0 (META — parent catalog cho mọi gap blocking actual self-test execution; force-multiplier per `meta-gap-priority.md` §3)
**Domain:** DevOps + Meta
**Detected:** 2026-05-21 (action-2.md line 73 user direction — "có thể self-test sớm nhất")
**Related PRs:** TBD
**Related Docs:** `documents/05-guides/local-dev/self-test-readiness-plan.md` (paired same-PR); GAP-694 Phase 0A audit; GAP-693 rebuild SOP; GAP-612 AWS suspension; `pre-handoff-self-test-completeness.md` §2.4

## Current State (verified 2026-05-21 via gap enumeration + CSV query)

> Per `audit-to-gap-pipeline.md` §2.5 state-check + §2.8 fix-time state-check (gap age 0d, fresh enumeration). Investigation method: (1) `bash scripts/query-gaps.sh` để liệt kê PARTIAL/OPEN phase-1-beta gaps; (2) `grep -liE "self.test|smoke|live.verify|admin.login|onboarding"` để filter self-test-related; (3) `Read documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md` để extract Phase 0A findings; (4) cross-reference 4 rules (`pre-handoff-self-test-completeness.md` §2.4, `release-deploy-standard.md` §3.1, `production-env-config-registry.md`, `user-manual-content-standard.md` §2).

### Tier 0 — Stack startup (Docker + .env + preflight)

| Gap | Status | Blocker for | Effort estimate |
|-----|--------|-------------|-----------------|
| GAP-694 | PARTIAL 15% | Docker Desktop process KHÔNG chạy trên Windows host → `docker version` command not found → mọi container ops bị block | Phase 0B fix #1: ~5min powershell launch; fix #2 .env 9 keys: ~10min; fix #3 preflight `check-docker.sh`: ~30-45min |
| GAP-694 sub-task (.env populate) | per Phase 0A finding #2 | Profile `branding-only` / `beta-funnel` / `full` start fail (thiếu OLLAMA endpoint + hCaptcha keys + tenant pattern) | ~10min append dev-safe defaults; KHÔNG commit `.env` (gitignored) |
| GAP-694 sub-task (preflight check) | per Phase 0A finding #3 META P2 | Recurrence: Windows reboot → Docker Desktop stopped → session lại block từ đầu | ~30-45min ship `kitehub/scripts/check-docker.sh` + integrate `up.sh` line 1 |
| GAP-408 | PARTIAL P0 phase-1-beta | JVM heap cap dev profile — Spring Boot OOM khi multiple service launch concurrent với WSL2 default cap | ~15min set `-XX:MaxRAMPercentage=60` trong dev profile per Wave 91 pattern |

**Tier 0 total: ~1-1.5h** (Docker launch fast; .env edit fast; preflight script medium effort).

### Tier 1 — Endpoint reachability + auth flow

| Gap | Status | Blocker for | Effort estimate |
|-----|--------|-------------|-----------------|
| GAP-518 | PARTIAL 97% | Code-side complete (BE RoleGuardMatrixIT 8/8 + FE 27/27 PASS local); admin login → `/admin` redirect chưa live verify | ~5min live walk khi stack up (per `pre-handoff-self-test-completeness.md` §2.4 (b)+(c)+(d)) |
| GAP-519 | PARTIAL P1 | Admin dashboard nav sidebar missing links — user phải gõ URL by memory | ~30min add Sidebar items `/admin/beta-requests` + verify post-login render |
| GAP-520 | PARTIAL P1 | JWT signing secret rotation runbook + dual-key — rotation testable trên local stack | ~20min execute existing runbook + verify dual-key accept |
| GAP-481 | OPEN | Gateway path routing 404 — `/api/v1/admin/*` routes có thể fail tại gateway level | ~15min verify routing config + curl smoke |
| GAP-502 | PARTIAL P0 | kh_backend production thrashing (RabbitMQ auth fail + OOM kills); local equivalent thrash risk | ~30min verify RMQ creds + memory cap config |
| GAP-684 | OPEN P0 | GAP-518 live admin-login walk gated GAP-612 — local stack bypasses AWS gate, unblocks local verify | n/a (resolved by Tier 0 unlock — local stack ≠ AWS prod) |

**Tier 1 total: ~2h** (admin flow live verify cheap khi Tier 0 up; nav + routing checks fast).

### Tier 2 — Business flow execution

| Gap | Status | Blocker for | Effort estimate |
|-----|--------|-------------|-----------------|
| GAP-538 | PARTIAL 95% | Day-1 onboarding checklist + sample/demo data seed — Wave 78+98 shipped FE/BE/seed + Wave 101 Bucket D Playwright E2E (5-step VN checklist + IMPORT_DATA opt-in + no-English-placeholder); live walkthrough verify blocked GAP-612 (local OK) | ~30min execute Playwright E2E local + verify VN checklist render đúng |
| GAP-637 | PARTIAL P0 | Admin v1 controllers `@PreAuthorize` missing + 403 tests (OWASP A01 broken access control) | ~45min add `@PreAuthorize` + 403 ITs |
| GAP-620 | OPEN P0 | Wave 92 Bucket D live verify admin v1 controllers — paired GAP-637 | ~20min execute live walk per `pre-handoff-self-test-completeness.md` §2.4 |
| GAP-561 | DONE | invite-staff email + BE endpoint + FE UI — P3 Manager flow shipped Wave 79 | n/a (verify-only smoke trong Tier 1) |
| GAP-562 | DONE | RBAC role separation Customer vs Staff — kitehub-branding `@PreAuthorize` shipped | n/a (verify-only smoke) |
| GAP-516 | PARTIAL | 2FA Platform Admin TOTP — challenge flow blocks admin login khi enabled | ~30min verify TOTP code flow OR document disabled trong dev env |
| GAP-531 | PARTIAL | Tenant init handoff end-to-end — multi-step orchestration verify | ~45min walk POST /tenants → confirm subdomain → seed Day-1 data |

**Tier 2 total: ~3-4h** (live walk flows medium effort; @PreAuthorize backfill nhanh; tenant init end-to-end longest).

### Tier 3 — Data realism + polish

| Gap | Status | Blocker for | Effort estimate |
|-----|--------|-------------|-----------------|
| GAP-658 | PARTIAL P0 | VN sample seed worker — replace English placeholder data với Vietnamese-friendly content (per `user-manual-content-standard.md` §2 row 7) | ~1h replace seed fixtures (Trần Thị Hồng / Sky Education / Lớp 5A1) |
| GAP-659 | PARTIAL P0 | Staff-invite email + persona-tone split (formal owner vs informal teacher) | ~30min review email templates + adjust tone per persona |
| GAP-543 | PARTIAL P0 | Email content audit — 5 critical email types content/tone Vietnamese | ~45min audit + revise 5 templates |
| GAP-657 | PARTIAL P0 | Email layer hardening — plain-text fallback + List-Unsubscribe + Reply-To headers | ~30min header additions |
| GAP-269b | PARTIAL P2 | kc-student real REST endpoints (today/grades/payments/notifications) — beta cohort student-facing | ~2h endpoint scaffold |
| GAP-138 | OPEN P1 | KiteClass Landing Hero — duplicated "Chuyên nghiệp & Hiệu quả" text | ~5min content fix |
| GAP-139 | OPEN P1 | Parent Dashboard MVP placeholder-only (Wave 5 widgets missing) | ~2h widget scaffold |

**Tier 3 total: ~5-6h** (data realism polish; KHÔNG block critical path self-test — chỉ cần cho beta cohort UX quality).

## Problem

User direction 2026-05-21 (action-2.md line 73): "để dự án đạt được tiêu chí self-test thì cần có kế hoạch fix những gaps nào => điều tra và tạo kế hoạch, mục tiêu là có thể self-test sớm nhất".

GAP-694 (Local self-test investigation fix) chỉ cover **Tier 0 root cause** (Docker Desktop not running + .env keys missing + preflight). Comprehensive self-test execution cần unblock **4 tiers** sequenced theo dependency:

1. **Tier 0 — Stack startup**: Docker daemon reachable + .env complete + JVM heap tuned. Block 100% self-test nếu fail.
2. **Tier 1 — Endpoint reachability + auth**: services healthy (`/actuator/health` 200) + gateway routing OK + admin login → redirect → dashboard render. Block tất cả business flow walks.
3. **Tier 2 — Business flow execution**: admin/owner/staff persona walk per `pre-handoff-self-test-completeness.md` §2.4 — onboarding wizard, RBAC checks, tenant init. Block beta cohort velocity.
4. **Tier 3 — Data realism + polish**: VN sample data, email content tone, student endpoints. Block UX quality cho beta cohort (NOT block self-test execution itself).

Hiện trạng: 30+ gaps phân tán across phase-1-beta folder; KHÔNG có single catalog enumerate ALL self-test blockers + dependency order + effort estimate. Solo dev mất 1-2h cross-reference 30+ gaps để build mental dependency graph mỗi session restart.

Catalog GAP-695 này = single source-of-truth cho "self-test readiness" — fix `meta-gap-priority.md` §3 force-multiplier (1 catalog → mọi session subsequent reuse plan).

## Context

Per outside-in synthesis 2026-05-21 (3 parallel agents: failure-mode + external benchmark + persona simulation per `outside-in-coverage-trigger.md` §3), user sequencing "Phase 0 local self-test fix → Item 2 refactor → rebuild" makes Phase 0 the PREREQUISITE. GAP-694 covers Docker root cause but không cover broader self-test execution path.

Cost compounding without catalog:
- Solo dev fatigue: 5+ min/session cross-reference 30 gaps → mental map → forgot 1 critical Tier 2 gap → execution-time pivot
- Beta tenant velocity: mỗi rebuild/deploy cycle thiếu local gate → role-guard mismatch (Wave 71b pattern) + CORS misconfig (Wave 82) + env wire bug (Wave 81 JWT) escape to prod
- Per `feedback_audit_of_trust_pass.md` (memory) — recurrence #4 "AC [x] ≠ production-verified" pattern; catalog forces explicit verify-via reference per tier

Impact:
- 🔴 **Self-test execution velocity** — catalog reduces mental overhead từ 30+ gaps lookup → 1 sequenced plan reference
- 🔴 **Beta cohort onboarding velocity** — Tier 2 unlock = walkthrough feasible local trước AWS deploy
- 🔴 **Force-multiplier** — Wave 92+ planning auto-references this catalog instead of re-deriving each session

## Evidence

- Phase 0A audit `documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md` — 16 read-only commands run; 3 ranked root causes; 6 phantoms ruled out; pending follow-ups enumerate Tier 0-Tier 1 unlock path
- GAP-694 §Proposed Fix Phase 0A → Phase 0B → Phase 0C — single-tier focus (Docker)
- 30 phase-1-beta P0 gaps OPEN/PARTIAL per CSV query 2026-05-21 — không có dependency graph
- `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist 7 items — mandates verification chain user-facing, không có execution plan
- `release-deploy-standard.md` §3.1 PRE-RELEASE checklist "Smoke admin-login" — mandates post-deploy gate, không có local equivalent
- Wave 71b admin-login 500 incident (per rule `pre-handoff-self-test-completeness.md` §6) — counterfactual: catalog Tier 1 + Tier 2 verify would have caught role-guard mismatch + nav missing locally trước deploy

## Proposed Fix

### Phase 0 (this PR — investigation + catalog ship) ✅ ~25% — DONE

- Ship GAP-695 file (this artifact) — 4-tier catalog với gap enumeration + effort estimate + dependency notes
- Ship `documents/05-guides/local-dev/self-test-readiness-plan.md` paired same-PR — TL;DR + dependency graph + ordered fix sequence + critical path
- Add CSV row GAP-695 (PARTIAL P0 Meta phase-1-beta completion_pct=25)
- Update ROADMAP §🚀 Next Action mentioning GAP-695 as parent catalog
- Cross-link from GAP-694 + GAP-693 to GAP-695

### Phase 1 — Tier 0 execution (~1-1.5h) — pending

Per GAP-694 Phase 0B:
- User action: launch Docker Desktop trên Windows host (`powershell.exe Start-Process` per `agent-action-bias.md` §1 Part B)
- Append 9 missing keys vào `.env` với dev-safe defaults
- (Optional META) Ship `kitehub/scripts/check-docker.sh` preflight + integrate `up.sh`/`setup.sh`
- Verify: `bash kitehub/scripts/up.sh --profile infra-only` → 4 services (Postgres/Redis/RabbitMQ/MinIO) healthy

**Exit gate Phase 1:** `docker ps` shows 4+ services UP + `/actuator/health` 200 cho core services.

### Phase 2 — Tier 1 execution (~2h) — pending

- Verify gateway routing (`curl http://localhost:9000/actuator/health`)
- Live walk admin login per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g)
- Close out GAP-518 PARTIAL 97% → DONE 100% với live verify evidence (eliminate AWS dependency for code-side closure)
- Ship GAP-519 sidebar nav links
- Verify GAP-481 gateway path routing 404 fixed

**Exit gate Phase 2:** admin@kitehub.me logs in → `/admin` redirect → `/admin/beta-requests` page renders với data.

### Phase 3 — Tier 2 execution (~3-4h) — pending

- Execute Wave 101 Bucket D Playwright E2E spec cho GAP-538 (Day-1 onboarding checklist 5-step VN)
- Backfill `@PreAuthorize` cho admin v1 controllers (GAP-637 + GAP-620 paired)
- Walk POST /tenants → confirm subdomain → seed Day-1 (GAP-531)
- 2FA TOTP verify OR document disable trong dev env (GAP-516)

**Exit gate Phase 3:** owner persona walks tenant init → onboarding wizard 5 steps → seed data import — zero blockers.

### Phase 4 — Tier 3 polish (~5-6h, optional cho self-test, mandatory cho beta cohort) — pending

- VN sample seed worker (GAP-658)
- Email tone audit + persona split (GAP-543 + GAP-659)
- Email layer hardening headers (GAP-657)
- Student endpoints scaffold (GAP-269b)
- Landing hero duplicate fix (GAP-138)
- Parent Dashboard widgets (GAP-139)

**Exit gate Phase 4:** beta cohort onboarding walkthrough render với 100% Vietnamese content + zero placeholder English data.

## Acceptance Criteria

- [x] GAP-695 file created với 4-tier catalog + effort estimate + dependency notes — this artifact
- [x] Plan doc `documents/05-guides/local-dev/self-test-readiness-plan.md` paired same-PR shipped
- [x] CSV row GAP-695 added (PARTIAL P0 Meta phase-1-beta completion_pct=25)
- [x] ROADMAP §🚀 Next Action references GAP-695 as parent catalog
- [x] Cross-link from GAP-694 + GAP-693 to GAP-695
- [ ] Phase 1 (Tier 0 execution) — Docker launch + .env populate + infra-only profile UP (deferred next session)
- [ ] Phase 2 (Tier 1 execution) — admin login live walk + GAP-518 closure local (deferred Phase 1 unlock)
- [ ] Phase 3 (Tier 2 execution) — owner persona walks tenant init + onboarding (deferred Phase 2 unlock)
- [ ] Phase 4 (Tier 3 polish) — VN data realism + email content quality (optional, beta cohort polish)
- [ ] Status PARTIAL 25% → 100% DONE khi tất cả 4 phases shipped + self-test execution chứng minh end-to-end

## Related

- **GAP-694** Local self-test investigation fix (Tier 0 sub-scope — Docker + .env + preflight)
- **GAP-693** AWS rebuild SOP playbook (downstream — depends on self-test execution working)
- **GAP-612** AWS account suspension recovery (parallel — local self-test bypasses AWS dependency)
- **GAP-518** BE seed PLATFORM_ADMIN vs FE ADMIN mismatch (Tier 1 — live verify path)
- **GAP-538** Day-1 onboarding checklist + sample data seed (Tier 2 — Playwright E2E)
- **GAP-637** Admin v1 controllers `@PreAuthorize` missing (Tier 2 — RBAC)
- **GAP-658** VN sample seed worker (Tier 3 — data realism)
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (verification chain mandate)
- `.claude/rules/release-deploy-standard.md` §3.1 PRE-RELEASE "Smoke admin-login" (post-deploy gate)
- `.claude/rules/production-env-config-registry.md` v1.1.0 (runtime env coverage)
- `.claude/rules/user-manual-content-standard.md` §2 (Tier 3 VN data + email quality criteria)
- `.claude/rules/meta-gap-priority.md` §3 META P0 force-multiplier (catalog scope justification)
- `.claude/rules/outside-in-coverage-trigger.md` v1.1.0 (Phase 0 outside-in synthesis 2026-05-21)
- Outside-in audit synthesis 2026-05-21 (3 parallel agents output GAP-693/694 + this catalog gap)

## Log

- **2026-05-21 (Wave 102.8 Bucket D)** — PARTIAL 25 → 50%. Tier 1 endpoint+auth verify SHIPPED via Bucket D execution.
  - Consumed Bucket A Docker preflight + `.env` populate (Wave 102.8 Bucket A merged PR #1691): `bash kitehub/scripts/check-docker.sh` exit 0; `bash kitehub/scripts/setup.sh` generated `.env` với dev-safe secrets.
  - Stack startup via `docker-compose -f docker-compose.kitehub.yml --profile beta-funnel up -d`: kite-postgres + kite-redis + kite-rabbitmq + kite-minio + kitehub-subscription + kitehub-admin + kitehub-email + kite-gateway + kitehub-frontend all UP healthy (after volume reset to clear stale postgres password).
  - GAP-518 admin login PARTIAL 97 → 99% — curl-level (b)(d) PASS: `POST /api/auth/login admin@kitehub.com/Admin@KiteHub123` → 200 + JWT role=`ADMIN`.
  - GAP-519 sidebar nav PARTIAL 80 → 90% — code-side Sidebar.tsx 4 testid'd links confirmed via direct Read.
  - GAP-481 gateway routing OPEN → 🟢 DONE 100% — `for path in /api/v1/admin/beta-requests /api/v1/admin/instances /api/v1/admin/impersonate/start; do curl ... ; done` → all 400 (NOT 404) = routing PASS.
  - Tier 1 (e)(f) browser walk PARTIAL — running FE image `gap-284-test` predates Wave 79+ `(admin)/admin/beta-requests` route group → 404 from runtime; rebuild needed to unlock. Code-side complete.
  - Tier 2-3 execution defer Wave 102.9+ per scope (per `wave-closure-scope-completeness.md` §3 — explicitly tracked, not orphan).
  - CSV row: completion_pct 25 → 50, last_verified 2026-05-21.

- **2026-05-21** — Initial write-up (state-check completed per `audit-to-gap-pipeline.md` §2.5 + §2.8). Investigation method: (1) CSV query PARTIAL/OPEN phase-1-beta P0 gaps (30 found); (2) grep self-test/smoke/admin-login keywords (30 file matches); (3) Read Phase 0A audit `local-stack/2026-05-21-local-self-test-investigation.md` (16 commands + 3 root causes + 6 phantoms); (4) cross-reference 4 rules. **4-tier catalog** assembled: Tier 0 (Docker + .env + preflight ~1-1.5h) → Tier 1 (endpoint + auth ~2h) → Tier 2 (business flow ~3-4h) → Tier 3 (data realism ~5-6h optional). Critical path = Tier 0 → Tier 1 → Tier 2 = ~6-7h dev effort cho self-test execution working end-to-end (Tier 3 beta cohort polish, không block self-test itself). Per `gap-done-discipline.md` §3 PARTIAL exit ramp: GAP-695 stays PARTIAL 25% (catalog ship ≠ execution); Tier 0-3 execution tracked Phase 1-4 dependent on next sessions. Per `meta-gap-priority.md` §3 META P0 force-multiplier: 1 catalog → eliminate 30+ gap cross-reference overhead mỗi session restart. Reviewer self-approve solo-dev mode. CSV row added paired same-PR per `gap-architecture-v2.md` canonical store. ROADMAP §🚀 Next Action updated reference GAP-695 as parent catalog. Cross-link GAP-694 + GAP-693 updated. Plan doc `documents/05-guides/local-dev/self-test-readiness-plan.md` paired same-PR.
