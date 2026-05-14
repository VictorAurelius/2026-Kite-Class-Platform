---
title: Wave 72a — Self-Test Unblock + OWASP A07 P0/P1 Hardening
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [72a]
gaps: [GAP-514, GAP-515, GAP-518, GAP-519, GAP-520, GAP-521, GAP-522, GAP-525]
---

# Wave 72a — Self-Test Unblock + OWASP A07 P0/P1 Hardening

**Goal:** Unblock Plan 1 self-test admin UI end-to-end AND close the P0/P1 slice of OWASP A07 pre-launch hardening checklist, in one parallel 5-bucket wave.

**Trigger:** User-directed 2026-05-14 — "tạo wave để tiếp tục fix hết gaps cho self-test". Wave 71b/71c surfaced 11 gaps (GAP-514..526) but only filed; none fixed. Plan 1 self-test currently blocked by GAP-518 (FE role-guard `'ADMIN'` vs BE seed `PLATFORM_ADMIN`).

**Estimated wall-clock:** ~45–60 min longest bucket (Bucket B — 3 BE features + migrations + tests), ~30 min coordinator wrap-up.

**Scope companion (deferred Wave 72b):** GAP-516 (2FA TOTP), GAP-517 (login alert P2), GAP-526 (admin subpages verify, depends Bucket C done), GAP-523 (audit skill rubric review — 6 skills), GAP-524 (pre-handoff rule extension P1 META). Stub file `wave-2026-05-14-72b-2fa-audit-rubric-review.md` ships same plan PR.

---

## 1. Brainstorm

**Q1 (alignment):** P2 Center Owner persona (admin@kitehub.me operates platform); secondary unblock Pa. Parent + Teacher who can't onboard until admin approves beta requests. Serves Phase 1 BETA launch (10/10 production push, currently held at "admin UI broken").

**Q2 (trade-offs):**
- Considered: ship GAP-518 alone as a tiny PR to unblock self-test fast, defer hardening. Rejected — leaves 6 OWASP A07 gaps open without forcing function, recurrence likely. User chose full P0+P1 slice (Option 2 of AskUserQuestion 2026-05-14).
- Considered: include GAP-516 2FA in this wave. Rejected — TOTP enrollment is a feature-sized scope (BE service + entity + FE enrollment UI + recovery codes); would push longest-bucket past 90 min and break 5-bucket parallelism limit per `feedback_parallel_agent_strategy.md` rule #9.
- Considered: fold GAP-525 credential rotation into Bucket D as runbook-only (agent prep) + later user-action. Accepted — agent ships runbook + rotation checklist + per-credential commands; user executes after wave merge.

**Q3 (risks):**
- Bucket B has 3 gaps with sequential touch-points in `AuthService` + `JwtService`. Mitigation: single-agent owns Bucket B; tests cover each gap separately.
- Bucket C is cross-layer (FE+BE) but uses **existing** endpoints — no Bucket 0 Foundation needed per `contract-first-for-cross-layer.md` §2 (existing auth endpoints, JWT payload shape unchanged; role string Java enum already returns `PLATFORM_ADMIN`). Verified §4 State-Check.
- GAP-525 credential rotation list could leak credentials AGAIN if agent reads/writes them. Mitigation: agent writes runbook **referencing** AWS Secrets Manager secret IDs only; never reads or echoes secret values.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-514 | bg-agent | ~25min | ✅ kitehub-gateway only |
| B | GAP-515 + 520 + 521 | bg-agent | ~50min | ✅ kitehub-subscription only (different packages within) |
| C | GAP-518 + 519 | bg-agent | ~30min | ✅ kitehub-frontend + 1-line auth-store/JWT-claim BE edit |
| D | GAP-525 | bg-agent | ~20min | ✅ runbook docs only (`documents/05-guides/operations/credential-rotation-runbook.md`) |
| E | GAP-522 | bg-agent | ~40min | ✅ `.claude/skills/quality/security-audit/` + 4 sister rules `.claude/rules/pre-launch-{dependency,secrets,owasp-rest,infra}-hardening-checklist.md` |

Disjoint check:
- A touches `kitehub-gateway/src/main/resources/application.yml` only
- B touches `kitehub-subscription/src/main/{java,resources}/**` only (no gateway, no FE)
- C touches `kitehub-frontend/src/**` (3 files) + ONE Java edit in `JwtService.java` adding `ADMIN` to acceptable-role list (BE-side option B per GAP-518 §Proposed Fix)
- D touches `documents/05-guides/operations/` + `documents/04-quality/audits/credential-rotation/` only
- E touches `.claude/skills/quality/security-audit/SKILL.md` + 4 NEW rules under `.claude/rules/`

**Bucket B vs C overlap risk:** Bucket B adds `jwt.secret.previous` config slot in `application.yml`; Bucket C reads `JwtService.java` to find role-claim logic. Solution: Bucket B owns ALL `JwtService.java` edits including the GAP-518 role-list extension (folded). Bucket C → FE only. Re-check: GAP-518 §Proposed Fix Option B = FE-only accept-both pattern → fold INTO C as FE-only. Final answer: **Bucket C is FE-only; the BE side of GAP-518 needs NO change** because BE already returns `PLATFORM_ADMIN` correctly; FE is wrong. Confirmed below in §4 State-Check.

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH (blocks Plan 1 self-test + OWASP A07 P0) → model: Opus 4.7 full

**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** Bucket C touches FE+BE BUT consumes existing endpoints with unchanged contract (role claim already in JWT; FE just needs to accept both values). No new endpoint, no DTO change → **NO Bucket 0 Foundation required**. Other buckets are single-layer.

> **Gap referencing convention:** all gap IDs verified via `bash scripts/query-gaps.sh <id>` 2026-05-14; status OPEN P0 (except GAP-520/521 P1).

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-514 | 🔴 P0 | `kitehub/kitehub-gateway/src/main/resources/application.yml` + gateway tests | parallel |
| 2 | **B** | GAP-515 + 520 + 521 | 🔴 P0 + 🟠 P1 + 🟠 P1 | `kitehub/kitehub-subscription/src/main/{java/com/kitehub/subscription/{service,security,audit,entity},resources/db/migration}/**` | parallel |
| 3 | **C** | GAP-518 + 519 | 🔴 P0 + 🟠 P1 | `kitehub/kitehub-frontend/src/{components/layout/AdminLayout.tsx,app/(auth)/login/page.tsx,stores/auth-store.ts,components/layout/Sidebar.tsx}` | parallel |
| 4 | **D** | GAP-525 | 🔴 P0 | `documents/05-guides/operations/credential-rotation-runbook.md` (NEW) + `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md` (NEW) | parallel |
| 5 | **E** | GAP-522 | 🔴 P0 META | `.claude/skills/quality/security-audit/SKILL.md` + `.claude/rules/pre-launch-{dependency,secrets,owasp-rest,infra}-hardening-checklist.md` (4 NEW) | parallel |

### Bucket A — Gateway rate limit (GAP-514)

- **Files:** `kitehub/kitehub-gateway/src/main/resources/application.yml` (extend), `kitehub/kitehub-gateway/src/test/java/com/kitehub/gateway/**/*RateLimitTest.java` (new IT)
- **Acceptance subset:** 7 auth routes (`/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/verify-email`, `/api/auth/resend-verification`, `/api/auth/password-reset-request` (if exists, else N/A), `/api/v1/auth/request-beta-access`) have `RequestRateLimiter` per `pre-launch-auth-hardening-checklist.md` §2.1 table. Add missing `emailKeyResolver` + `userKeyResolver` beans in `KeyResolverConfig.java` if not present.
- **Tests:** at minimum an integration test for `/api/auth/login` rapid-fire 11 requests → 11th returns 429.

### Bucket B — Backend security hardening (GAP-515 + 520 + 521)

- **Files:**
  - Migrations: `V35__add_account_lockout_columns.sql`, `V36__create_admin_audit_log.sql` (numbering verified §4)
  - Java: `service/AuthService.java` (lockout increment + check), `security/JwtService.java` (dual-key verify), `security/JwtKeyManager.java` (NEW), `audit/AdminAuditLog.java` (entity NEW), `audit/AdminAuditLogRepository.java` (NEW), `audit/AdminAuditAspect.java` (AOP interceptor NEW), `audit/Auditable.java` (annotation NEW)
  - Config: `application.yml` jwt section `secret-current` + `secret-previous` slots
  - Tests: unit + IT for lockout, dual-key, audit log
- **Acceptance subset:** see each GAP's AC; merge separately if needed.

### Bucket C — Admin role unification + sidebar nav (GAP-518 + 519)

- **Files (FE-only, no BE touch):**
  - `kitehub/kitehub-frontend/src/stores/auth-store.ts:8` — extend role union: `role: 'OWNER' | 'ADMIN' | 'PLATFORM_ADMIN'`
  - `kitehub/kitehub-frontend/src/components/layout/AdminLayout.tsx:20,33` — `!['ADMIN','PLATFORM_ADMIN'].includes(user?.role ?? '')`
  - `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx:38` — `['ADMIN','PLATFORM_ADMIN'].includes(user.role) ? '/admin' : '/dashboard'`
  - `kitehub/kitehub-frontend/src/components/layout/AdminLayout.tsx` OR `AdminSidebar.tsx` (NEW per GAP-519) — sidebar with 4 nav links (Beta Requests / Instances / Payments / Revenue) + `data-testid="admin-nav-{slug}"`
  - Tests: `kitehub/kitehub-frontend/src/stores/__tests__/auth-store.test.ts` add `PLATFORM_ADMIN` case; new `AdminLayout.test.tsx` for role check; nav-link presence test
- **Acceptance subset:** admin@kitehub.me login → lands `/admin` → sees sidebar → 4 links clickable.
- **Cross-layer note:** no api-contract.md edit needed (JWT response shape unchanged; this is FE-side validation widening).

### Bucket D — Credential rotation runbook (GAP-525)

- **Files (docs only):**
  - NEW `documents/05-guides/operations/credential-rotation-runbook.md` — generic runbook covering admin password / Cloudflare API token / Resend API key rotation procedures
  - NEW `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md` — incident artifact listing 3 specific credentials with rotation status checklist (user-action)
- **Agent constraint:** Agent MUST NOT read/echo secret values; references AWS Secrets Manager secret IDs + AWS CLI commands only. User executes rotation after wave merge.
- **Acceptance subset:** runbook covers 3 credential types + per-credential rotation flow + verification step + audit-log entry pointer.

### Bucket E — Meta security-audit rubric extension (GAP-522)

- **Files:**
  - `.claude/skills/quality/security-audit/SKILL.md` — extend Category 1, 2, 3, 5 rubrics per per-check pattern established for Category 4 (PR #1278)
  - NEW `.claude/rules/pre-launch-dependency-hardening-checklist.md` — N checks for Cat 1
  - NEW `.claude/rules/pre-launch-secrets-hardening-checklist.md` — N checks for Cat 2
  - NEW `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` — N checks for Cat 3 (A01/A02/A05/A08/A09/A10)
  - NEW `.claude/rules/pre-launch-infra-hardening-checklist.md` — N checks for Cat 5
  - Each rule follows `rule-change-process.md` §3 frontmatter + §6.5 Enforcement Parity (reviewer-checklist + worked self-test minimum; full automation deferred per `incident-to-rule-pipeline.md` ≥7-day guard)
- **Acceptance subset:** 4 new rules ship + security-audit SKILL.md cites them + self-test on current main state surfaces ≥1 finding per category (proving rubric fires).

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `RequestRateLimiter` (gateway) | YAML filter | `grep -n "RequestRateLimiter" kitehub/kitehub-gateway/src/main/resources/application.yml` | matches lines 32, 118 only (2 of 7 auth routes) | ✅ exists; needs extension |
| `ipKeyResolver` | Java bean | `grep -n "ipKeyResolver" kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/config/KeyResolverConfig.java` | line 56 `@Primary public KeyResolver ipKeyResolver()` | ✅ exists |
| `emailKeyResolver` | Java bean | `grep -rn "emailKeyResolver" kitehub/kitehub-gateway/src` | 0 matches | 🆕 to-be-created (Bucket A) |
| `userKeyResolver` | Java bean | `grep -rn "userKeyResolver" kitehub/kitehub-gateway/src` | 0 matches | 🆕 to-be-created (Bucket A) |
| `user?.role !== 'ADMIN'` (FE guard) | TSX condition | `grep -n "user?.role" kitehub/kitehub-frontend/src/components/layout/AdminLayout.tsx` | lines 20, 33 hard-block | ✅ exists (= bug GAP-518) |
| `user.role === 'ADMIN'` (redirect) | TSX condition | `grep -n "user.role" kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx` | line 38 | ✅ exists (= bug GAP-518) |
| `role: 'OWNER' \| 'ADMIN'` (auth-store) | TS type | `grep -n "role:" kitehub/kitehub-frontend/src/stores/auth-store.ts` | line 8 union | ✅ exists; needs widening |
| `AuthService.java` | Java class | `grep -n "class AuthService" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/AuthService.java` | line 38 | ✅ exists |
| `failedLoginAttempts` (BE) | Java field | `grep -rn "failedLoginAttempts\|accountLocked" kitehub/kitehub-subscription/src/main/java` | 0 matches | 🆕 to-be-created (Bucket B) |
| `AdminAuditLog` | Java entity | `grep -rn "class AdminAuditLog\|admin_audit_log" kitehub/kitehub-subscription/src/main` | 0 matches | 🆕 to-be-created (Bucket B) |
| `jwt.secret.previous` | Config key | `grep -rn "jwt.secret.previous\|secret-previous" kitehub/kitehub-subscription/src/main/resources` | 0 matches | 🆕 to-be-created (Bucket B) |
| Latest Flyway version | Migration filename | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql \| sort -V \| tail -1` | `V34__enable_rls_tenant_scoped_tables.sql` | ✅ exists; next free = V35, V36 |
| `pre-launch-auth-hardening-checklist.md` | Rule file (parent reference) | `ls .claude/rules/pre-launch-auth-hardening-checklist.md` | exists v1.0.0 (2026-05-13) | ✅ exists |
| `pre-launch-dependency-hardening-checklist.md` | Sister rule | `ls .claude/rules/pre-launch-dependency-hardening-checklist.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `pre-launch-secrets-hardening-checklist.md` | Sister rule | `ls .claude/rules/pre-launch-secrets-hardening-checklist.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `pre-launch-owasp-rest-hardening-checklist.md` | Sister rule | `ls .claude/rules/pre-launch-owasp-rest-hardening-checklist.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `pre-launch-infra-hardening-checklist.md` | Sister rule | `ls .claude/rules/pre-launch-infra-hardening-checklist.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `credential-rotation-runbook.md` | Runbook | `ls documents/05-guides/operations/credential-rotation-runbook.md` | 0 matches | 🆕 to-be-created (Bucket D) |
| `AdminSidebar.tsx` | FE component | `grep -rn "AdminSidebar" kitehub/kitehub-frontend/src` | 0 matches | 🆕 to-be-created (Bucket C — may inline into AdminLayout instead) |
| `V35__add_account_lockout_columns.sql` | Flyway migration | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V35*.sql` | 0 matches; next free per V34 latest | 🆕 to-be-created (Bucket B) |
| `V36__create_admin_audit_log.sql` | Flyway migration | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V36*.sql` | 0 matches; next free after V35 | 🆕 to-be-created (Bucket B) |
| AWS Secret `kitehub/production/admin-seed-password` | AWS resource | (Tier 1 `aws secretsmanager describe-secret`) | known per Wave 71b seed | ✅ exists (do not read value) |

Banned shortcuts respected: full `grep -n` outputs (no `| head` truncation); all 🆕 references have explicit bucket owner.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-gateway verify -P strict-warnings` | kitehub-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` | kitehub-ci |
| C | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend lint && pnpm -F kitehub-frontend build` | kitehub-frontend-ci |
| D | `bash scripts/check-docs.sh` + manual reviewer check | script-quality |
| E | `bash scripts/check-rule-frontmatter.sh` + `bash scripts/check-skill-conventions.sh` + `bash scripts/check-rules-index-csv.sh` | script-quality + meta-csv-indexes |

Per `admin-merge-discipline.md`: each bucket merger MUST run the corresponding local-verify command on the rebased HEAD before `--admin` merge. No `--admin` post-rebase without verify per §2 rule.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 5 buckets spawned with `run_in_background: true` in a SINGLE message
- `isolation: worktree` for parallel safety (creates `.claude/worktrees/agent-{A,B,C,D,E}/`)
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md` (no leading `/home/nguyenvankiet/...`)
- Coordinator waits for completion notifications, merges sequentially in dependency-safe order: A → B → C (FE depends on B's role-claim semantics being stable) → D → E

Post-merge each bucket PR per `admin-merge-discipline.md`:
- Re-base on current main, local verify (per §5), then `gh pr merge --squash` (NOT `--admin` unless local verify just passed on rebased HEAD)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md` + `feedback_wave_closure_release_progress_report.md`:

1. Each bucket PR updates affected GAP file `## Log` + flips `**Status:**` field + closing PR # reference
2. `documents/04-quality/gaps/gap-status.csv` row updated for each gap closed (Status → DONE / PARTIAL, completion_pct → 100 / N) — per `gap-architecture-v2.md` canonical
3. `ROADMAP.md` §🚀 Next Action — Phase 1 BETA P0 count drops by 5 (GAP-514/515/518/522/525); P1 advances on 520/521/519 if DONE
4. Wave plan frontmatter `status: complete` flip in closure PR
5. `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` append (Rule 15)
6. `bash scripts/prune-merged-worktrees.sh --yes` before drafting closure PR
7. Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3 (Bucket D credential rotation likely PARTIAL pending user-action; Bucket E sister-rules may be PARTIAL on detector wiring per ≥7-day guard)
8. **`## Release Plan Progress` section in closure PR body** per `feedback_wave_closure_release_progress_report.md`:
   - Current phase: Phase 1 BETA (Soft Launch chốt 2026-05-06)
   - Wave 72a contribution: 5 P0 closed + 3 P1 advanced
   - Trigger gates after Wave 72a: Plan 1 self-test PASS; OWASP A07 progress 3/8 → 5+/8
   - Estimated remaining wall-clock to v0.9.0-beta strict-min: ~Wave 72b + 73 (2FA + audit rubric review + admin subpages verify)
   - Waves Remaining table (strict-min v0.9.0-beta / practical v0.9.0-beta / v1.0.0 PROD)

### Post-handoff verify per `pre-handoff-self-test-completeness.md` §2.4

Coordinator MUST run admin-flow checklist post-wave per §2.4 rows (a-g):
- (a) admin@kitehub.me credential retrievable: ✅ AWS Secrets Manager
- (b) Login API: curl POST `/api/auth/login` → JWT 200
- (c) Login UI: browser submit → redirect to `/admin` (verified post-Bucket-C)
- (d) Role-guard: `/admin/*` accessible (post-Bucket-C)
- (e) Navigation: sidebar visible with 4 links (post-Bucket-C GAP-519)
- (f) Target page renders: `/admin/beta-requests` loads data without spinner-forever
- (g) Target action: approve test beta request → success

If any FAIL → wave PARTIAL, file follow-up gap, stay at 🟡 PARTIAL not 🟢 DONE.

---

## 8. Log

- **2026-05-14** (draft): Wave plan created. State-check evidence collected directly on main (commit 00fbf2d6). Bucket layout = 5 disjoint buckets. Cross-layer Bucket C confirmed NO Bucket 0 Foundation needed (existing JWT response shape; FE-only fix). Sister stub `wave-2026-05-14-72b-2fa-audit-rubric-review.md` ships same PR for follow-up scope visibility.
- **2026-05-14** (in-progress): Plan PR #1282 merged main `78b530db`. 5 buckets spawned background+worktree-isolated. Bucket F (self-test CSV matrix) added in-flight per user request "tạo thêm self-test để đi qua flow thông thường" — 6th bucket on top of original 5.
- **2026-05-14** (complete): Wave SHIPPED. 6 bucket PRs all merged: #1283 A (GAP-514 PARTIAL 66%), #1287 B (GAP-515/520/521 PARTIAL 80/90/70%), #1285 C (GAP-518/519 PARTIAL 80%), #1284 D (GAP-525 PARTIAL 50%), #1286 E (GAP-522 DONE), #1288 F (new self-test CSV 126 rows). Gaps: 1 DONE + 7 PARTIAL + 0 newly-filed in scope (Wave 72b stub references GAP-516/517/523/524/526 for follow-up). Side-track: #1289 statusline sync + #1290 starter-kit v2.4.0 retro-sync (17 new + 3 updated rules) + remote v2.4.0 PR #11 merged on canonical repo.
