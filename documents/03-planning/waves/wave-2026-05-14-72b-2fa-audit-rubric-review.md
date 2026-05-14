---
title: Wave 72b — 2FA + Login Alert + Admin Verify + Audit Rubric Review + Pre-Handoff Ext
status: draft
created: 2026-05-14
updated: 2026-05-14
waves: [72b]
gaps: [GAP-516, GAP-517, GAP-523, GAP-524, GAP-526]
---

# Wave 72b — 2FA + Login Alert + Admin Verify + Audit Rubric Review + Pre-Handoff Ext

**Goal:** Close 5 remaining P1/P2 + META gaps from Wave 71c queue. Ship 2FA TOTP for PLATFORM_ADMIN, login-from-new-IP alert, verify 3 admin subpages reach correct backends, generalize per-check rubric pattern to 6 other audit skills, extend pre-handoff rule with 7 new flow classes.

**Trigger:** Wave 72a complete (1 DONE + 7 PARTIAL). Plan 1 self-test infrastructure ready (CSV 126 rows shipped); user-walk pending. Wave 72b advances OWASP A07 from 3/8 PARTIAL → 5/8 toward v0.9.0-beta-rc gate.

**Estimated wall-clock:** ~70 min longest bucket (Bucket A — 2FA TOTP BE with entity + enrollment endpoints + recovery codes), ~30 min coordinator wrap-up.

---

## 1. Brainstorm

**Q1 (alignment):** P2 admin persona (admin@kitehub.me ops); secondary security-audit credibility (Bucket E) preventing recurrence of "87/100 hid OWASP A07" pattern across remaining 6 audit skills. Serves Phase 1 BETA admin trust + Phase 1.5 PAID readiness gate.

**Q2 (trade-offs):**
- **2FA library choice:** considered `dev.samstevens.totp:1.7.1` vs hand-roll RFC 6238. Picked library — well-maintained, encrypted seed storage helper, recovery codes utility. Trade-off: +1 dep ~50KB transitive Maven.
- **Recovery codes UX:** considered SMS fallback. Rejected — VN Twilio not provisioned + adds compliance scope. Ship 10 single-use recovery codes shown ONCE at enrollment + AWS Secrets Manager backup runbook reference.
- **Login alert scope:** considered alerting tenant OWNER too. Rejected — Phase 1 BETA only PLATFORM_ADMIN privileged; widen later Phase 1.5+.
- **Audit rubric review scope (Bucket E):** could be its own wave. Accepted as bucket because pattern is mechanical (mirror `pre-launch-auth-hardening-checklist.md` v1.0.0 structure × 6 skills). Each rule ships worked self-test that surfaces ≥1 finding on current main — proves rubric isn't aspirational.
- **Pre-handoff extension scope (Bucket E):** 7 new flow classes (file-upload, payment, multi-tenant, SSE, async job, time-sensitive, i18n). Folded with audit rubric review because both are rule-only edits, mechanical.

**Q3 (risks):**
- **2FA lockout risk:** PLATFORM_ADMIN could lose access if recovery codes lost. Mitigation: codes printed ONCE at enrollment; AWS Secrets Manager backup recommended; emergency reset runbook in JWT rotation runbook §reset.
- **Cross-layer drift (FE vs BE):** Bucket A (BE) + Bucket B (FE) implement contract from Bucket 0. Mitigation: Bucket 0 ships api-contract.md FIRST + MSW handlers; FE consumes MSW until BE merges; both buckets reference same contract.
- **Admin subpages verify (Bucket D)** may discover bugs requiring fixes. Bucket D agent has authority to ship small fixes; if scope balloons → file follow-up gap + Bucket D ships PARTIAL.
- **Bucket E meta rules** could ship weak rubrics. Mitigation: each rule's worked self-test MUST surface ≥1 finding on current main; if not, rubric is aspirational → rewrite.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | (Foundation — no gap closure) | bg-agent | ~25min | ✅ docs + MSW infra |
| A | GAP-516 BE | bg-agent | ~60min | ✅ kitehub-subscription auth + new dep |
| B | GAP-516 FE | bg-agent | ~45min | ✅ kitehub-frontend 2FA enrollment + login challenge |
| C | GAP-517 | bg-agent | ~35min | ✅ kitehub-subscription LoginAuditService + email template ref |
| D | GAP-526 | bg-agent | ~30min | ✅ verify-only + small FE fixes if needed |
| E | GAP-523 + GAP-524 | bg-agent | ~55min | ✅ `.claude/rules/` + `.claude/skills/quality/*-audit/SKILL.md` |

Disjoint check: A touches `kitehub-subscription/.../auth/2fa/**` (new package); B touches `kitehub-frontend/src/app/(auth)/2fa-setup/**` (new) + small login page edit; C touches `kitehub-subscription/.../audit/login/**` (new sub-package, NOT admin audit which Wave 72a Bucket B already created at `audit/admin/**`); D verifies + maybe small `kitehub-frontend/src/app/(admin)/admin/*/page.tsx` edits; E touches `.claude/rules/pre-handoff-self-test-completeness.md` + 6 `.claude/rules/audit-skill-rubric-*.md` (NEW) + 6 `.claude/skills/quality/{quality,ops-readiness,performance,api-contract,business-logic,ui-review}-audit/SKILL.md` edits.

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM-HIGH (P1+P2+META, no production blocker but OWASP A07 advancement + meta governance hardening) → model: Opus 4.7 full for Bucket A (2FA BE complex), Opus medium for B/C/D/E.

**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5 + `contract-first-for-cross-layer.md`):** YES — Bucket A (BE) creates NEW endpoints `/api/auth/2fa/enroll`, `/api/auth/2fa/verify`, `/api/auth/2fa/recovery-codes` consumed by Bucket B (FE). **Bucket 0 Foundation REQUIRED** — must MERGE FIRST.

> **Gap referencing convention** (per `gap-architecture-v2.md`): IDs verified via `bash scripts/query-gaps.sh <id>` 2026-05-14 — all 5 target gaps OPEN at 0% completion.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | (enabler) | 🟠 P1 | `documents/01-business/kitehub/auth/{rules.md,use-cases.md,api-contract.md}` (NEW domain) + `kitehub/kitehub-frontend/src/test/msw/handlers/auth.ts` (extend or NEW) | **MERGE FIRST** |
| 1 | **A** | GAP-516 BE | 🟠 P1 | `kitehub/kitehub-subscription/src/main/{java/com/kitehub/subscription/auth/twofactor,resources/db/migration}/**` + `pom.xml` add `dev.samstevens.totp:1.7.1` | parallel after 0 |
| 2 | **B** | GAP-516 FE | 🟠 P1 | `kitehub/kitehub-frontend/src/app/(auth)/2fa-{setup,challenge}/page.tsx` (NEW) + `src/app/(auth)/login/page.tsx` (extend post-login conditional 2FA challenge) + new components | parallel after 0 |
| 3 | **C** | GAP-517 | 🟡 P2 | `kitehub/kitehub-subscription/src/main/{java/com/kitehub/subscription/audit/login,resources/db/migration}/**` (V37 login_audit_log) + email template ref | parallel after 0 |
| 4 | **D** | GAP-526 | 🟠 P1 | `kitehub/kitehub-frontend/src/app/(admin)/admin/{instances,payments,revenue}/page.tsx` (verify + small FE fixes) — backend routing already verified Wave 71b/72a; this is **end-to-end click-through** | parallel after 0 |
| 5 | **E** | GAP-523 + GAP-524 | 🔴 P0 META + 🟠 P1 META | `.claude/rules/audit-skill-rubric-{quality,ops-readiness,performance,api-contract,business-logic,ui-review}.md` (6 NEW) + extend `pre-handoff-self-test-completeness.md` + 6 `.claude/skills/quality/*-audit/SKILL.md` edits | parallel after 0 |

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.0:

- **Files (NEW 3-file domain):**
  - `documents/01-business/kitehub/auth/rules.md` — BR-AUTH-001..010 business rules covering: login attempts (cross-ref GAP-515 lockout), JWT TTL (cross-ref GAP-520), 2FA enforcement (GAP-516 mandates PLATFORM_ADMIN; OWNER+ optional), recovery codes (10 single-use), login alert cadence (GAP-517 24h cooldown per fingerprint), session/refresh rotation
  - `documents/01-business/kitehub/auth/use-cases.md` — UC-AUTH-001 login + 2FA challenge; UC-AUTH-002 first-time 2FA enrollment; UC-AUTH-003 TOTP verify; UC-AUTH-004 recovery code use; UC-AUTH-005 admin login alert delivery
  - `documents/01-business/kitehub/auth/api-contract.md` — endpoints `/api/auth/2fa/enroll-init` (POST returns QR + secret + recovery codes), `/api/auth/2fa/enroll-confirm` (POST verifies first TOTP code, enrolls), `/api/auth/2fa/verify` (POST during login flow), `/api/auth/2fa/recovery-codes/regenerate` (POST authenticated), `/api/auth/2fa/disable` (POST admin-self-service with confirmation)
- **MSW handler:** `kitehub/kitehub-frontend/src/test/msw/handlers/auth.ts` — extend existing OR create; ship handlers for 5 above endpoints returning mock responses for FE dev/test
- **Acceptance subset:** 3 files exist with proper section headers (Endpoints + Request/Response shapes + Error codes); MSW handlers compile-clean

### Bucket A — 2FA TOTP BE (GAP-516 BE half)

- **Files:**
  - NEW migration `V37__add_user_2fa_columns.sql`: add to `users`:
    - `totp_secret VARCHAR(64)` (encrypted at rest via Spring `@Convert` or app-level AES)
    - `totp_enrolled_at TIMESTAMPTZ`
    - `totp_required BOOLEAN DEFAULT FALSE` (PLATFORM_ADMIN seed sets TRUE)
    - `recovery_codes_hash TEXT` (hashed array — single-use, mark used)
  - NEW Java package `com.kitehub.subscription.auth.twofactor`:
    - `TwoFactorAuthService.java` — wraps `dev.samstevens.totp` lib
    - `RecoveryCodeService.java` — generates 10 single-use codes, bcrypt-hashes, marks used
    - `TwoFactorController.java` — 5 endpoints per Bucket 0 api-contract
    - `EnrollInitResponse.java` / `EnrollConfirmRequest.java` / `VerifyRequest.java` DTOs
  - Modify `AuthService.login` flow:
    - On successful password verify: if `user.totp_enrolled` → return `{requires2fa: true, challenge_token: <one-time>}` instead of JWT
    - Add `/api/auth/login/2fa-complete` endpoint that takes `challenge_token` + `totp_code`, verifies, issues JWT
  - Add dependency to `kitehub/kitehub-subscription/pom.xml`: `dev.samstevens.totp:totp:1.7.1`
  - Update PLATFORM_ADMIN seed (likely `scripts/seed-direct-sql.sh` OR `ProductionSeedRunner`): set `totp_required=TRUE` (admin must enroll on first login)
  - Tests: unit for TwoFactorAuthService (verify happy + wrong code + expired + recovery code use); IT for 5 endpoints (enroll-init → returns QR; enroll-confirm valid code → enrolled; verify valid → 200; verify invalid → 401; recovery code single-use)

### Bucket B — 2FA TOTP FE (GAP-516 FE half)

- **Files:**
  - NEW `kitehub/kitehub-frontend/src/app/(auth)/2fa-setup/page.tsx` — first-time enrollment: fetch QR + recovery codes from `/api/auth/2fa/enroll-init`, show QR + "Print recovery codes" UI, submit first TOTP code to `enroll-confirm`
  - NEW `kitehub/kitehub-frontend/src/app/(auth)/2fa-challenge/page.tsx` — login challenge step: shown after password if `requires2fa: true`, input 6-digit code OR "Use recovery code" toggle
  - Modify `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx`: post-password response handler — if `requires2fa: true` → router.push(`/2fa-challenge?token=...`); else proceed with JWT as today
  - NEW components: `kitehub/kitehub-frontend/src/components/auth/TotpInput.tsx` (6-digit code input + autofocus) + `RecoveryCodesDisplay.tsx` (print + copy + download)
  - Test consume from MSW handlers (Bucket 0 shipped); add `__tests__/2fa-setup.test.tsx` + `2fa-challenge.test.tsx`
- **Cross-layer AC:** Endpoint consumption tuân thủ schema trong `documents/01-business/kitehub/auth/api-contract.md` (Bucket 0 ship trước)

### Bucket C — Login Alert (GAP-517)

- **Files:**
  - NEW migration `V38__create_login_audit_log.sql`:
    ```sql
    CREATE TABLE login_audit_log (
      id BIGSERIAL PRIMARY KEY,
      user_id BIGINT NOT NULL REFERENCES users(id),
      login_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      ip INET,
      user_agent VARCHAR(512),
      geo_country VARCHAR(8),
      fingerprint_hash CHAR(64),  -- sha256(ip||ua)
      alert_sent BOOLEAN DEFAULT FALSE
    );
    CREATE INDEX idx_login_audit_user_time ON login_audit_log(user_id, login_at DESC);
    CREATE INDEX idx_login_audit_fingerprint ON login_audit_log(user_id, fingerprint_hash);
    ```
  - NEW Java sub-package `com.kitehub.subscription.audit.login` (separate from `audit.admin` which Wave 72a Bucket B created):
    - `LoginAudit.java` entity + repository
    - `LoginAuditService.java` — on successful login: compute fingerprint hash, write row, check if first time fingerprint seen for this user (lookback 24h), if NEW + user is PLATFORM_ADMIN → emit `admin.login.new-fingerprint` event
  - Modify `AuthService.login` (post-Bucket-A merge): inject LoginAuditService; emit on every successful login
  - Email template reference: add row in `documents/01-business/kitehub/email-lifecycle/api-contract.md` for new template `admin-new-login-alert` (real template body deferred — Bucket C ships event + service; Resend template config = ops follow-up gap)
  - Tests: LoginAuditServiceTest (known fingerprint = no event; new fingerprint = event; non-PLATFORM_ADMIN = no event; 24h cooldown per fingerprint)

### Bucket D — Admin Subpages Verify + Small Fixes (GAP-526)

- **Files (verify-only first; small FE fixes if bugs found):**
  - Verify 3 admin subpages click-through using `phase-1-beta-acceptance-self-test.csv` rows ADM-INST-* / ADM-PAY-* / ADM-REV-*
  - For each: open DevTools Network → click page → note API calls fired → confirm each call hits correct backend service (vs blanket /api/v1/** which Wave 71b GAP-512 fixed)
  - Likely scope: small selector fixes / data-shape mismatches / empty-state UI tweaks in `kitehub/kitehub-frontend/src/app/(admin)/admin/{instances,payments,revenue}/page.tsx`
  - Tests: ensure existing tests pass; add 1 smoke test per subpage if absent
- **PARTIAL exit:** if Bucket D finds blockers requiring deeper fix → ship verification report + file follow-up gaps + GAP-526 stays PARTIAL

### Bucket E — Audit Skill Rubric Review (GAP-523) + Pre-Handoff Extension (GAP-524)

- **Files (GAP-523 — 6 sister rules + 6 skill edits):**
  - NEW `.claude/rules/audit-skill-rubric-quality.md` v1.0.0 — apply per-check + primacy pattern to `quality-audit` SKILL (currently /110 11 categories)
  - NEW `.claude/rules/audit-skill-rubric-ops-readiness.md` v1.0.0 — `ops-readiness-audit` /100
  - NEW `.claude/rules/audit-skill-rubric-performance.md` v1.0.0 — `performance-audit` /100
  - NEW `.claude/rules/audit-skill-rubric-api-contract.md` v1.0.0 — `api-contract-audit` /100
  - NEW `.claude/rules/audit-skill-rubric-business-logic.md` v1.0.0 — `business-logic-audit` /100
  - NEW `.claude/rules/audit-skill-rubric-ui-review.md` v1.0.0 — `ui-review` /128
  - Each rule: per-check enumeration replacing vague averaging + primacy "bug-finding > scoring" section + worked self-test surfacing ≥1 finding on current main
  - Update each `.claude/skills/quality/{quality,ops-readiness,performance,api-contract,business-logic,ui-review}-audit/SKILL.md` (or `quality/ui-review/SKILL.md`) to cite the binding rule per `pre-launch-auth-hardening-checklist.md` v1.0.0 precedent
  - Add 6 rows to `.claude/rules/rules-index.csv`
- **Files (GAP-524 — pre-handoff extension):**
  - Edit `.claude/rules/pre-handoff-self-test-completeness.md` v1.0.0 → v1.1.0
  - Add §2.5 File-upload flow, §2.6 Payment flow, §2.7 Multi-tenant tenant-switch, §2.8 SSE/WS, §2.9 Background job/async, §2.10 Time-sensitive (token expiry / refresh / clock skew), §2.11 i18n flow
  - Each new §2.X: 4-row checklist (a-d minimum) similar to existing §2.1-2.4 pattern
  - Worked self-test §4 extension — apply 1 new class retroactively to a prior incident if findable

### Bucket G — Self-Test CSV Rework + 2 Meta Rules (post-hoc addition after Wave 72a Bucket F user-flagged 4 issues)

Added post-Wave-72a closure 2026-05-14 per user retro on PR #1288 self-test CSV. Wave plan PR #1292 already shipped — this Bucket G note documents the in-flight addition. Bucket G ships from this same wave to keep meta-rule rollout cluster (Bucket E rubric review + G test artifact format) co-located.

- **Files:**
  - Relocate `documents/05-guides/operations/phase-1-beta-acceptance-self-test.csv` → `documents/05-guides/operations/acceptance-tests/` + companion README
  - Translate 126 CSV rows narrative content to Vietnamese (column names + identifiers + enums stay English)
  - Prepend UTF-8 BOM to CSV
  - NEW `scripts/render-acceptance-test-xlsx.sh` — CSV → XLSX render (openpyxl preferred, libreoffice fallback)
  - NEW folder `documents/05-guides/operations/acceptance-tests/{README.md, .gitignore}` (XLSX gitignored)
  - Archive old `documents/03-planning/end-user/plan-1-self-test-e2e.md` → `documents/07-archived/planning-2026/plan-1-self-test-e2e-superseded.md`; delete now-empty `end-user/` folder
  - NEW `.claude/rules/test-artifact-format-standard.md` v1.0.0 — CSV canonical + XLSX generated + per-artifact-type matrix
  - NEW `.claude/rules/dev-readable-doc-language.md` v1.0.0 — Vietnamese narrative + English identifier split
  - Update `.claude/rules/rules-index.csv` — 2 new rows
  - Update `.claude/rules/output-review-mandate.md` §3 — add "Acceptance test CSVs" row
  - Update ROADMAP + Wave 72b plan with new paths
- **Acceptance:**
  - [ ] CSV at `acceptance-tests/` with UTF-8 BOM verified
  - [ ] 126 rows narrative translated to Vietnamese; column names + enums English
  - [ ] Old Plan 1 archived; `end-user/` folder deleted
  - [ ] Render script + folder README + .gitignore shipped
  - [ ] 2 meta rules shipped per `rule-change-process.md` §6.5 (worked self-test + cross-refs + paired enforcement)
  - [ ] Rules-index CSV validator PASS (47 rows)
  - [ ] `output-review-mandate.md` §3 row added for acceptance test CSVs
  - [ ] `bash .claude/skills/workflow/session-docs-check/scripts/check-docs.sh` PASS

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `documents/01-business/kitehub/auth/` | Domain folder | `ls documents/01-business/kitehub/auth/ 2>&1` | folder not found | 🆕 to-be-created (Bucket 0) |
| `kitehub/kitehub-frontend/src/test/msw/handlers/` | MSW infra | `find kitehub/kitehub-frontend/src -path "*msw*" -type d` | 3 dirs present including `handlers/__tests__` | ✅ exists |
| `TwoFactorAuthService` | Java class | `grep -rln "TwoFactorAuthService\|samstevens.totp" kitehub/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `LoginAuditService` | Java class | `grep -rln "LoginAuditService\|login_audit_log" kitehub/` | 0 matches | 🆕 to-be-created (Bucket C) |
| `V37__add_user_2fa_columns.sql` | Flyway migration | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V37*.sql` | 0 matches; latest = V36 | 🆕 to-be-created (Bucket A) |
| `V38__create_login_audit_log.sql` | Flyway migration | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V38*.sql` | 0 matches; next free after V37 | 🆕 to-be-created (Bucket C) |
| `/admin/instances/page.tsx` | FE admin subpage | `ls kitehub/kitehub-frontend/src/app/(admin)/admin/instances/page.tsx` | exists (+ `[id]/page.tsx`) | ✅ exists |
| `/admin/payments/page.tsx` | FE admin subpage | `ls kitehub/kitehub-frontend/src/app/(admin)/admin/payments/page.tsx` | exists | ✅ exists |
| `/admin/revenue/page.tsx` | FE admin subpage | `ls kitehub/kitehub-frontend/src/app/(admin)/admin/revenue/page.tsx` | exists | ✅ exists |
| `pre-handoff-self-test-completeness.md` | Rule file (extend) | `ls .claude/rules/pre-handoff-self-test-completeness.md` | v1.0.0 (2026-05-13) | ✅ exists; extend to v1.1.0 |
| `audit-skill-rubric-quality.md` | Sister rule | `ls .claude/rules/audit-skill-rubric-quality.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `audit-skill-rubric-ops-readiness.md` | Sister rule | `ls .claude/rules/audit-skill-rubric-ops-readiness.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `audit-skill-rubric-performance.md` | Sister rule | `ls .claude/rules/audit-skill-rubric-performance.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `audit-skill-rubric-api-contract.md` | Sister rule | `ls .claude/rules/audit-skill-rubric-api-contract.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `audit-skill-rubric-business-logic.md` | Sister rule | `ls .claude/rules/audit-skill-rubric-business-logic.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `audit-skill-rubric-ui-review.md` | Sister rule | `ls .claude/rules/audit-skill-rubric-ui-review.md` | 0 matches | 🆕 to-be-created (Bucket E) |
| `documents/01-business/kitehub/auth/api-contract.md` | API contract doc (cross-layer) | `ls documents/01-business/kitehub/auth/api-contract.md` | 0 matches | 🆕 to-be-created (Bucket 0 Foundation) |
| `dev.samstevens.totp:totp:1.7.1` | Maven dep | `grep -rln "samstevens.totp" kitehub/kitehub-subscription/pom.xml` | 0 matches | 🆕 to-be-added (Bucket A) |

Banned shortcuts respected: full `grep -rln` outputs (no `\| head`), all 🆕 symbols explicitly owned by a bucket, Bucket 0 row present per `contract-first-for-cross-layer.md` §3.2.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 | `bash .claude/skills/workflow/session-docs-check/scripts/check-docs.sh` + manual reviewer check on 3 docs | script-quality |
| A | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -P strict-warnings` | kitehub-ci |
| B | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend lint && pnpm -F kitehub-frontend build` | kitehub-frontend-ci |
| C | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -P strict-warnings` | kitehub-ci |
| D | Manual browser walkthrough using `phase-1-beta-acceptance-self-test.csv` rows ADM-INST-* / ADM-PAY-* / ADM-REV-* + `pnpm -F kitehub-frontend test --run` | kitehub-frontend-ci |
| E | `bash scripts/check-rule-frontmatter.sh` + `bash scripts/check-skill-conventions.sh` + `bash scripts/check-rules-index-csv.sh` | script-quality + meta-csv-indexes |

Per `admin-merge-discipline.md`: each bucket merger runs corresponding local-verify on rebased HEAD before merge. No `--admin` post-rebase without verify.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `contract-first-for-cross-layer.md`:

**Phase 1 — Foundation merge (sequential prerequisite):**
- Spawn Bucket 0 in single foreground or single background agent
- Wait for PR merged to main
- Then **Phase 2** spawn

**Phase 2 — 5 parallel buckets (after Bucket 0 merged):**
- A + B + C + D + E spawned in SINGLE message via 5 Agent tool calls
- All `run_in_background: true` + `isolation: worktree`
- RELATIVE paths in prompts (per `feedback_worktree_absolute_path_contamination.md`)
- 5 concurrent = at cap

**Sequential merge order post-bucket-completion:** A → B (B references A's endpoints — but MSW from Bucket 0 covers test path, so order flexible) → C → D → E.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md` + `feedback_wave_closure_release_progress_report.md`:

1. Each bucket PR updates affected gap file `## Log` + flips `**Status:**` + closing PR # reference
2. `gap-status.csv` row updated per gap closed (canonical per `gap-architecture-v2.md`)
3. `ROADMAP.md` §🚀 Next Action — Phase 1 BETA P1 count drops by 4 (GAP-516/517/523 P0-meta/524/526); P0 META advances on GAP-523
4. Wave plan frontmatter `status: complete` flip
5. `wave-history.jsonl` append (Rule 15)
6. `bash scripts/prune-merged-worktrees.sh --yes` before drafting closure PR
7. Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
8. **`## Release Plan Progress` section in closure PR body** — OWASP A07 advancement (3/8 PARTIAL → 5/8 minimum after Wave 72b) + remaining waves to v0.9.0-beta-rc + v1.0.0 PROD

### Post-handoff verify per `pre-handoff-self-test-completeness.md` §2.4 (admin flow) + new §2.X classes from Bucket E

Coordinator runs admin-flow checklist post-wave; user-action items: enroll 2FA on admin@kitehub.me + verify login with TOTP + verify recovery code single-use + verify new-IP alert email received on test login from secondary device.

---

## 8. Log

- **2026-05-14** (draft → full): Wave plan replaces v0 stub (drafted 2026-05-14 as deferred companion to Wave 72a plan PR #1282) with full §3-§5 elaboration. State-check evidence collected directly on main commit `9802a388` post Wave 72a closure merge. Bucket layout = 1 Foundation + 5 parallel disjoint buckets. Cross-layer Bucket A+B → Bucket 0 Foundation REQUIRED + ships first. Bucket E folds GAP-523 + GAP-524 (both meta rule edits, mechanical, single agent).
- **2026-05-14** (TBD in-progress): Plan PR merged; Bucket 0 Foundation spawned; merged; Phase 2 5 parallel buckets spawned.
- **2026-05-14** (TBD complete): Wave SHIPPED. Outcomes: ...
