# GAP-695: Self-test readiness — comprehensive gap catalog + dependency-ordered fix plan

**Status:** 🟡 PARTIAL 85% — Phase 0 catalog SHIPPED 2026-05-21; Tier 0 (Docker preflight + .env) + Tier 1 (admin login + gateway routing) shipped Wave 102.8; catalog REFRESHED 2026-06-02 against current CSV (Tier 0/1 substantially DONE; new RST-blocker tier added). Remaining 15% = Tier 1.5/2/3 LOCAL-DOABLE execution (campaign-tracked) + Tier 4 AWS-BLOCKED live-verify subset (gated GAP-612)
**Priority:** 🔴 P0 (META — parent catalog cho mọi gap blocking actual self-test execution; force-multiplier per `meta-gap-priority.md` §3)
**Domain:** DevOps + Meta
**Detected:** 2026-05-21 (action-2.md line 73 user direction — "có thể self-test sớm nhất")
**Related PRs:** TBD
**Related Docs:** `documents/05-guides/local-dev/self-test-readiness-plan.md` (paired); `documents/03-planning/plans/plan-autonomous-gap-campaign-local-doable.md` (campaign steered by this catalog); GAP-694 Docker fix; GAP-693 rebuild SOP; GAP-612 AWS suspension; `pre-handoff-self-test-completeness.md` §2.4; `feature-ship-runtime-walk-mandate.md`

## Current State (REFRESHED 2026-06-02 via CSV state-check)

> Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check + `gap-architecture-v2.md` §3 (CSV canonical). Refresh method: `bash scripts/query-gaps.sh` enumerate active phase-1-beta P0/P1; cross-verify mỗi catalog gap row chống CSV (0 phantom — tất cả 22 original gaps tồn tại trong CSV); apply campaign scope filter §1 (`plan-autonomous-gap-campaign-local-doable.md`) tách LOCAL-DOABLE vs AWS-BLOCKED.

**Drift caught:** Catalog gốc (2026-05-21) đã stale nặng — phần lớn Tier 0/1/2 gaps đã DONE qua Wave 102.8→meta-6, và ~30+ RST-blocker gaps mới (Wave 1xx series) surfaced sau khi viết catalog. Refresh này đồng bộ trạng thái thật + thêm tầng RST-blocker.

### Legend
- ✅ DONE (CSV status=DONE) — không còn block self-test
- 🟡 PARTIAL (CSV completion 1-99%) — còn delta
- 🔵 OPEN (CSV completion 0%) — chưa bắt đầu
- 🟢 LOCAL-DOABLE — verify được trên local Docker stack (campaign in-scope)
- 🔴 AWS-BLOCKED — cần AWS deploy / DNS / live SES / vendor (campaign skip, giữ PARTIAL)

### Tier 0 — Stack startup (Docker + .env + preflight) — ✅ ESSENTIALLY DONE

| Gap | CSV status | Self-test relevance | Campaign |
|-----|-----------|---------------------|----------|
| GAP-694 | ✅ DONE 100% | Docker preflight `check-docker.sh` + `.env` populate shipped Wave 102.8 Bucket A | n/a (done) |
| GAP-408 | 🟡 PARTIAL 50% (P2) | JVM heap cap dev profile — Spring Boot OOM khi multi-service concurrent (WSL2) | 🟢 LOCAL — set `-XX:MaxRAMPercentage=60` dev profile, verify `docker stats` |

**Tier 0 verdict:** Stack khởi động được trên local. Chỉ còn GAP-408 heap tuning (P2, nice-to-have chống OOM khi chạy full profile).

### Tier 1 — Endpoint reachability + auth flow — ✅ MOSTLY DONE

| Gap | CSV status | Self-test relevance | Campaign |
|-----|-----------|---------------------|----------|
| GAP-518 | ✅ DONE 100% | admin login → role=ADMIN JWT — curl + code-side verified | n/a (done) |
| GAP-519 | ✅ DONE 100% | Admin sidebar nav links | n/a (done) |
| GAP-481 | ✅ DONE 100% | Gateway `/api/v1/admin/*` routing — curl smoke PASS (400 not 404) | n/a (done) |
| GAP-684 | ✅ DONE 100% | GAP-518 live admin-login walk (local bypasses AWS gate) | n/a (done) |
| GAP-520 | 🟡 PARTIAL 70% (P1) | JWT signing secret rotation runbook + dual-key | 🔴 AWS-BLOCKED — rotation runbook references prod Secrets Manager; local dual-key spec OK nhưng live rotation cần AWS |
| GAP-502 | 🟡 PARTIAL 90% (P0) | kh_backend thrashing (RabbitMQ auth + OOM) | 🔴 AWS-BLOCKED — production thrash symptom; local RMQ creds verify OK nhưng prod tuning cần AWS |
| GAP-599 | 🟡 PARTIAL 95% (P0) | JWT storage key collision 2 browser tab cùng domain | 🟢 LOCAL — FE storage namespacing, verify browser DevTools 2-tab |

**Tier 1 verdict:** Auth flow + gateway routing PASS local. Còn GAP-599 (FE storage collision — LOCAL) + 2 AWS-blocked tuning gaps.

### Tier 1.5 — RST-blocker functional bugs (NEW — surfaced post-catalog via RST walks Wave 1xx) — 🔵 BLOCK SELF-TEST

> Đây là tầng MỚI không có trong catalog 2026-05-21. RST (manual exploratory) walks Đợt 1xx bắt nhiều functional bugs chặn persona end-to-end walk. Per `feature-ship-runtime-walk-mandate.md` các bug này MUST fix trước khi self-test claim PASS. ĐÂY là trọng tâm self-test readiness hiện tại — cluster ưu tiên #1 cho campaign.

| Gap | CSV status | Self-test blocker | Campaign |
|-----|-----------|-------------------|----------|
| GAP-727 | 🟡 PARTIAL 95% (P0) | `hasAccessToClass` guard broken — Class thiếu teacher_id mapping → teacher lock-out hoàn toàn | 🟢 LOCAL — IT + RST walk teacher persona local |
| GAP-610 | 🟡 PARTIAL 85% (P0) | GET beta-signup validate trả TOKEN_NOT_FOUND cho valid token (lifecycle collapse H4) | 🟢 LOCAL — lifecycle state fix + IT |
| GAP-794 | 🔵 IN_PROGRESS 80% (P1) | Anonymous PDPL consent endpoints 401 (SecurityConfig path drift) | 🟢 LOCAL — SecurityConfig matcher fix + IT |
| GAP-777 | 🔵 OPEN (P1) | KC API 400 Bad Request trả empty body (no error detail) → user/dev không debug được | 🟢 LOCAL — error handler RFC 7807 body, verify curl |
| GAP-776 | 🔵 OPEN (P1) | Gateway circuit-breaker 503 fallback cold-start (auth + admin) | 🟢 LOCAL — verify gateway resilience config local |
| GAP-726 | 🔵 OPEN (P1) | KC `/branding/wizard` render blank + SSR ECONNREFUSED localhost:8080 | 🟢 LOCAL — SSR fetch fix, verify browser local |
| GAP-774 | 🔵 OPEN (P1) | KH admin audit-log controller missing (Mảng D4 blocker) | 🟢 LOCAL — scaffold controller + IT |
| GAP-775 | 🔵 OPEN (P1) | KC ReportController missing (Mảng B11 blocker) | 🟢 LOCAL — scaffold controller + IT |
| GAP-729 | 🔵 OPEN (P1) | 11/19 controllers no per-resource authz guard (A01 OWASP IDOR wide) | 🟢 LOCAL — add guards + 403 IT |
| GAP-784 | 🔵 OPEN (P1) | FE InviteStaffPage role param missing — Wave 80 FE vs meta-6 BE drift | 🟢 LOCAL — FE param fix + RST walk |
| GAP-765 | 🔵 OPEN (P1) | Beta request POST 201 nhưng không gửi confirmation email | 🟢 LOCAL — verify MailHog local |
| GAP-825 | 🔵 OPEN (P1) | Tenant-isolation hardening — JWT-sig-verify TenantResolver fallback | 🟢 LOCAL — IT cross-tenant |

**Tier 1.5 verdict:** 12 RST-blocker gaps, 12/12 LOCAL-DOABLE. Cluster ưu tiên cao nhất cho campaign vì chặn persona walk thực sự.

### Tier 2 — Business flow execution — ✅ MOSTLY DONE

| Gap | CSV status | Self-test relevance | Campaign |
|-----|-----------|---------------------|----------|
| GAP-538 | ✅ DONE 100% | Day-1 onboarding 5-step VN checklist + Playwright E2E | n/a (done) |
| GAP-637 | ✅ DONE 100% | Admin v1 `@PreAuthorize` + 403 ITs | n/a (done) |
| GAP-620 | ✅ DONE 100% | Admin v1 live verify | n/a (done) |
| GAP-561 | ✅ DONE 100% | invite-staff email + endpoint + UI | n/a (done) |
| GAP-562 | ✅ DONE 100% | RBAC Customer vs Staff | n/a (done) |
| GAP-516 | 🟡 PARTIAL 75% (P1) | 2FA TOTP mandatory PLATFORM_ADMIN | 🟢 LOCAL — verify TOTP flow OR document disabled dev env |
| GAP-531 | 🟡 PARTIAL 45% (P1) | Tenant init handoff post admin-approve end-to-end | 🟢 LOCAL — walk POST /tenants → confirm → seed local |
| GAP-536 | 🟡 PARTIAL 80% (P0) | POST /tenants idempotency key (chống double-submit orphan) | 🟢 LOCAL — UNIQUE constraint + 409 IT |
| GAP-532 | 🟡 PARTIAL (P0) | Multi-tenant tenant-switch flow §2.7 coverage | 🟢 LOCAL — walk tenant switch local |

**Tier 2 verdict:** Core business flows DONE. Còn 4 PARTIAL (tenant init/switch/idempotency/2FA) — tất cả LOCAL-DOABLE.

### Tier 3 — Data realism + email + polish — 🟡 MIXED

| Gap | CSV status | Self-test relevance | Campaign |
|-----|-----------|---------------------|----------|
| GAP-658 | 🟡 PARTIAL 90% (P0) | VN sample seed worker — replace English placeholder (Trần Thị Hồng / Sky Education / Lớp 5A1) | 🟢 LOCAL — seed fixture replace + verify |
| GAP-659 | ✅ DONE 100% | Staff-invite email persona-tone split | n/a (done) |
| GAP-543 | 🟡 PARTIAL 95% (P0) | Email content audit 5 critical types Vietnamese | 🟢 LOCAL — revise templates, verify MailHog |
| GAP-657 | ✅ DONE 100% | Email layer hardening headers | n/a (done) |
| GAP-269b | 🟡 PARTIAL 50% (P2) | kc-student REST endpoints (today/grades/payments/notifications) | 🟢 LOCAL — endpoint scaffold + IT |
| GAP-138 | 🔵 OPEN (P1) | KC Landing Hero duplicated "Chuyên nghiệp & Hiệu quả" text | 🟢 LOCAL — content fix + visual verify |
| GAP-139 | 🟡 PARTIAL 40% (P1) | Parent Dashboard MVP placeholder-only (Wave 5 widgets) | 🟢 LOCAL — widget scaffold + RST walk |
| GAP-586 | 🟡 PARTIAL (P1) | Beta invite email content audit tone + sender + feedback CTA | 🟢 LOCAL — template audit MailHog |
| GAP-587 | 🟡 PARTIAL (P1) | P3 invite email content (owner name + center context) | 🟢 LOCAL — template audit MailHog |
| GAP-590 | 🟡 PARTIAL (P1) | Email/reset link expiry policy spec (24h/15min/10min) | 🟢 LOCAL — config + IT |

**Tier 3 verdict:** Data realism + email polish — tất cả LOCAL-DOABLE (MailHog cho email local verify). KHÔNG block critical-path self-test (chỉ beta cohort UX quality), nhưng nâng chất lượng walk.

### Tier 4 — AWS-BLOCKED live-verify (self-test cần AWS — campaign SKIP) — 🔴

> Các gap này cần AWS deploy / DNS / live SES / Resend live send. Catalog liệt kê để self-test biết phần nào KHÔNG thể verify local. Campaign giữ PARTIAL, mark blocked.

| Gap | CSV status | Cần gì | Campaign note |
|-----|-----------|--------|---------------|
| GAP-530 | 🟡 PARTIAL 10% (P0) | Email-driven flow end-to-end live verify §2.3 | 🔴 cần live SES/Resend (MailHog local là partial substitute) |
| GAP-793 | 🟡 PARTIAL 95% (P0) | Production Resend send branch never reached | 🔴 cần live Resend send |
| GAP-608 | 🟡 PARTIAL 90% (P0) | EC2 IAM role thiếu ses:SendEmail | 🔴 cần AWS IAM apply |
| GAP-533 | 🟡 PARTIAL 80% (P0) | Resend deliverability DKIM/DMARC/SPF + spam-score | 🔴 cần live domain + Resend |
| GAP-756 | 🟡 PARTIAL 35% (P0) | Wave beta-prep-1 production deploy + RST verify | 🔴 cần AWS deploy (GAP-612 gate) |
| GAP-818 | 🔴 BLOCKED (P1) | Wave tenant-domain-1 live RST walk 4 buckets | 🔴 cần AWS restore + ACM |
| GAP-747 | 🔴 BLOCKED (P1) | Live verify SES ses:SendEmail post AWS restore | 🔴 cần AWS account restore |

**Tier 4 verdict:** 7 gaps gated AWS (GAP-612 account restore là master dependency). Self-test FULL (incl. live email + DNS) blocked until AWS up. Self-test LOCAL (functional + UI + email-via-MailHog) KHÔNG bị block — đó là điểm campaign tận dụng.

### Campaign steering summary (LOCAL-DOABLE vs AWS-BLOCKED)

| Tier | LOCAL-DOABLE | AWS-BLOCKED | DONE |
|------|:------------:|:-----------:|:----:|
| Tier 0 (stack) | 1 (GAP-408) | 0 | 1 |
| Tier 1 (auth/routing) | 1 (GAP-599) | 2 (GAP-520/502) | 4 |
| Tier 1.5 (RST-blocker) | **12** | 0 | 0 |
| Tier 2 (business flow) | 4 | 0 | 5 |
| Tier 3 (data/email/polish) | 8 | 0 | 2 |
| Tier 4 (live-verify) | 0 | 7 | 0 |
| **Total catalogued** | **26 LOCAL** | **11 BLOCKED** | **12 DONE** |

**Self-test verdict:** LOCAL self-test (functional persona walk + UI + email-via-MailHog) là khả thi NGAY — chỉ cần đóng 12 Tier 1.5 RST-blocker (cluster ưu tiên #1). FULL self-test (incl. live SES/DNS) chờ AWS GAP-612 restore.

### Top-10 priority order cho campaign coordinator (next /loop iterations)

Per `meta-gap-priority.md` (Meta → P0 → P1) + dependency (RST-blocker chặn persona walk trước):

1. **GAP-727** (P0, 95%) — teacher lock-out fix; chặn toàn bộ teacher persona walk. Gần xong.
2. **GAP-610** (P0, 85%) — beta-signup token lifecycle; chặn signup→onboarding flow đầu tiên.
3. **GAP-794** (P1, 80% IN_PROGRESS) — PDPL consent 401; gần xong, finish trước.
4. **GAP-536** (P0, 80%) — POST /tenants idempotency; chặn tenant-init reliability.
5. **GAP-543** (P0, 95%) — email content audit 5 types; gần xong, MailHog-verifiable.
6. **GAP-658** (P0, 90%) — VN sample seed; nâng chất lượng walk + thesis VN-data.
7. **GAP-777** (P1, 0%) — KC API empty error body; affects mọi error-path debug.
8. **GAP-729** (P1, 0%) — 11/19 controllers IDOR guards; OWASP A01 wide, batch-fixable.
9. **GAP-726** (P1, 0%) — KC wizard blank SSR; chặn branding wizard persona walk.
10. **GAP-774 + GAP-775** (P1, 0%) — missing controllers (audit-log + report); scaffold cùng cluster.

**Cluster gợi ý cho wave-pack-planner (≥3 disjoint):**
- Cluster A (auth/authz): GAP-727 + GAP-729 + GAP-825 (kiteclass-core authz layer)
- Cluster B (signup/consent): GAP-610 + GAP-794 + GAP-765 (kitehub-subscription signup flow)
- Cluster C (missing controllers): GAP-774 + GAP-775 + GAP-777 (KC/KH controller scaffold)
- Cluster D (email/data polish): GAP-543 + GAP-658 + GAP-586/587 (MailHog-verifiable)

## Problem

User direction 2026-05-21 (action-2.md line 73): "để dự án đạt được tiêu chí self-test thì cần có kế hoạch fix những gaps nào => điều tra và tạo kế hoạch, mục tiêu là có thể self-test sớm nhất".

GAP-694 (Local self-test investigation fix) chỉ cover **Tier 0 root cause** (Docker Desktop not running + .env keys missing + preflight). Comprehensive self-test execution cần unblock **4 tiers** sequenced theo dependency:

1. **Tier 0 — Stack startup**: Docker daemon reachable + .env complete + JVM heap tuned. Block 100% self-test nếu fail.
2. **Tier 1 — Endpoint reachability + auth**: services healthy (`/actuator/health` 200) + gateway routing OK + admin login → redirect → dashboard render. Block tất cả business flow walks.
3. **Tier 2 — Business flow execution**: admin/owner/staff persona walk per `pre-handoff-self-test-completeness.md` §2.4 — onboarding wizard, RBAC checks, tenant init. Block beta cohort velocity.
4. **Tier 3 — Data realism + polish**: VN sample data, email content tone, student endpoints. Block UX quality cho beta cohort (NOT block self-test execution itself).

Hiện trạng: 30+ gaps phân tán across phase-1-beta folder; KHÔNG có single catalog enumerate ALL self-test blockers + dependency order + effort estimate. Solo dev mất 1-2h cross-reference 30+ gaps để build mental dependency graph mỗi session restart.

**Cập nhật 2026-06-02:** Sau Wave 102.8→meta-6, bức tranh self-test readiness đã shift căn bản — Tier 0/1/2 phần lớn DONE; trọng tâm chuyển sang **Tier 1.5 RST-blocker** (12 functional bug surfaced từ RST walks Wave 1xx chặn persona end-to-end walk). Catalog refresh §Current State đồng bộ trạng thái thật + tách LOCAL-DOABLE (26) vs AWS-BLOCKED (11) để steer autonomous gap campaign (`plan-autonomous-gap-campaign-local-doable.md`). LOCAL self-test khả thi NGAY khi đóng 12 Tier 1.5; FULL self-test chờ AWS GAP-612 restore.

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

- [x] GAP-695 file created với catalog + effort estimate + dependency notes — this artifact
- [x] Plan doc `documents/05-guides/local-dev/self-test-readiness-plan.md` paired same-PR shipped
- [x] CSV row GAP-695 added
- [x] ROADMAP §🚀 Next Action references GAP-695 as parent catalog
- [x] Cross-link from GAP-694 + GAP-693 to GAP-695
- [x] Catalog REFRESHED 2026-06-02 against current CSV (0 phantom; Tier 0/1/2 DONE-status synced; Tier 1.5 RST-blocker tier added)
- [x] LOCAL-DOABLE vs AWS-BLOCKED split computed (26 LOCAL / 11 BLOCKED / 12 DONE) — steers campaign per `plan-autonomous-gap-campaign-local-doable.md` §1 filter
- [x] Top-10 priority order + 4 cluster suggestions cho campaign coordinator
- [ ] Tier 1.5 RST-blocker execution (12 LOCAL-DOABLE gaps) — campaign-tracked, chặn LOCAL persona walk
- [ ] Tier 2/3 PARTIAL execution (tenant init/switch/idempotency/2FA + email/data polish) — campaign-tracked LOCAL
- [ ] Tier 4 AWS-BLOCKED live-verify (7 gaps) — gated GAP-612 AWS restore
- [ ] Status PARTIAL 85% → 100% DONE khi LOCAL self-test execution chứng minh end-to-end (Tier 1.5 đóng) + FULL self-test post-AWS-restore (Tier 4)

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

- **2026-06-02 (autonomous gap campaign — catalog refresh)** — PARTIAL giữ 85% (CSV-canonical). Catalog REFRESHED against current `gap-status.csv` state-check per `audit-to-gap-pipeline.md` §2.8. Findings:
  - **Drift caught:** catalog 2026-05-21 đã stale — Tier 0 GAP-694 DONE; Tier 1 GAP-518/519/481/684 DONE; Tier 2 GAP-538/637/620/561/562/659/657 DONE. Catalog gốc list chúng PARTIAL/OPEN.
  - **0 phantom:** verify tất cả 22 original gaps + 30+ active gaps chống CSV — mọi entry map real row.
  - **Tier 1.5 RST-blocker tier ADDED** (12 gaps, không có trong catalog gốc): functional bugs surfaced từ RST walks Wave 1xx (GAP-727 teacher lock-out / GAP-610 token lifecycle / GAP-794 PDPL 401 / GAP-777 empty error body / GAP-776 circuit-breaker / GAP-726 wizard blank / GAP-774+775 missing controllers / GAP-729 IDOR guards / GAP-784 FE param / GAP-765 confirm email / GAP-825 tenant isolation). ĐÂY là trọng tâm self-test readiness hiện tại — chặn persona end-to-end walk.
  - **LOCAL-DOABLE vs AWS-BLOCKED split computed** per `plan-autonomous-gap-campaign-local-doable.md` §1 filter: 26 LOCAL / 11 BLOCKED / 12 DONE. Tier 4 (7 gaps) gated GAP-612 AWS restore (live SES/Resend/DNS). LOCAL self-test khả thi NGAY khi đóng 12 Tier 1.5.
  - **Campaign steering:** Top-10 priority order + 4 disjoint cluster suggestions (A auth/authz, B signup/consent, C missing controllers, D email/data) cho wave-pack-planner.
  - **Honest PARTIAL:** catalog refresh là doc-work hoàn thành; AC cuối (LOCAL self-test execution end-to-end) cần Tier 1.5 fix execution (campaign work, không phải catalog work); FULL self-test cần AWS GAP-612. Per `gap-done-discipline.md` §3 — không flip DONE vì execution AC chưa met.
  - CSV row last_verified: 2026-05-26 → 2026-06-02; completion_pct giữ 85.

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
