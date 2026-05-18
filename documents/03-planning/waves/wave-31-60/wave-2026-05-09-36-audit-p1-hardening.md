---
title: Wave 36 — Audit P1 Hardening (5 P1 clusters from 2026-05-07 audit)
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [36]
gaps: [GAP-387, GAP-388, GAP-389, GAP-390, GAP-391, GAP-393]
---

# Wave 36 — Audit P1 Hardening

**Goal:** Resolve 5 P1 cluster gaps để push Quality 80→85+ và harden production stack post-Phase-1-launch.
**Trigger:** Wave 35 P0 sprint shipped (Quality 73→80); Phase 1 BETA deployed; remaining audit findings consolidated thành P1 hardening sprint.
**Estimated wall-clock:** ~20h dev parallel, longest-bucket Bucket C ops (~5h). With 4 background agents Opus medium effort → ~45-60min wall-clock.

**Prerequisite:** Wave 35 SHIPPED ✅ + audit re-run 2026-05-07 confirms Quality 80/100 ✅ (vừa đủ trigger gate) + 0 NEW P0 BLOCKERS as scope-active.

**P0 status post-audit-2026-05-07:** 3 P0 surfaced từ Ops Readiness audit (53/100 F):
- 🔴 NEW P0 — `BetaAccessService.recordHoneypotRejection()` dead-wire (0 callsite, Bean Validation rejects ConstraintViolation at MVC trước khi reach service) → **absorbed into Bucket A 388-A**, GAP-387 flipped 🟢→🟡 PARTIAL pending Bucket A re-close
- 🔴 CARRY P0 — GAP-144 AlertManager production receivers PARTIAL (pre-existing, không block Phase 1 BETA invite-only)
- 🔴 CARRY P0 — `scripts/backup-production.sh` missing → **already Bucket C scope (389-A)**

Effectively 0 P0 NEW blocker outside Wave 36 scope.

---

## 1. Brainstorm

**Q1 (alignment):** Phase 1.5 PAID readiness — production hardening cho beta tenants (security defense-in-depth + ops observability + API contract polish + UI quota UX + Performance scaling).

**Q2 (trade-offs):**
- **Reject** ship sequentially per-cluster — 5 disjoint domains, parallel agents tiết kiệm 70% wall-clock
- **Reject** combine với P0 (Wave 35) — context heavy, mix priority
- **Accept** Sonnet/Haiku cho LOW-stakes refactor buckets nếu spawn fresh; Opus medium cho cross-cutting (cluster GAPs touch multiple files)

**Q3 (risks):**
- **R1:** GAP-393 V31 collision risk — Wave 35 Bucket E ship V31 với organization_name + status indexes; Wave 36 Bucket E (perf cluster) cần V32+ cho subsequent migrations (quota cache key column nếu cần)
- **R2:** GAP-388 token plaintext fix có 2 options (claim code 2FA vs S/MIME) — Bucket A chọn 2FA (simpler, 2h)
- **R3:** GAP-389-C BR-LIFE/QUALITY compliance blocks docs-only nhưng yêu cầu cross-link với 5-attribute mandate; verify rule version `business-logic-review.md` v1.0.0 chưa drift

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-388 security cluster (honeypot + token 2FA + per-email rate limit) | bg-agent (Opus medium) | 5h | ✅ kitehub-subscription Java + email template + Redis rate-limit |
| B | GAP-390 API cluster (tenantId wire + SSE assertions + path param doc) | bg-agent (Opus medium) | 3h | ✅ kitehub-branding DTO + tests + docs |
| C | GAP-389 ops cluster (backup automation + email smoke + BR-LIFE compliance blocks) | bg-agent (Opus medium) | 5h | ✅ scripts/ + docs only |
| D | GAP-393 perf cluster (quota cache + SSE backpressure + idempotency cache) | bg-agent (Opus medium) | 5h | ✅ kitehub-branding service-layer (status index moved to Wave 35 Bucket E) |
| E | GAP-391 UI cluster (RegenerateCounter quota stale + i18n deferral doc) | bg-agent (Sonnet) | 1h | ✅ kitehub-frontend hook + docs |

Disjoint check: A=subscription, B+D=branding (different services trong module), C=infrastructure scripts + docs, E=frontend. No file collisions.

---

## 3. Scope

**Stake tier:** MEDIUM (P1 hardening, không block deploy nhưng improve audit scores) → model: **Opus medium effort** cho 4 cluster buckets, **Sonnet** cho UI bucket
**Cross-layer?** NO (pure-BE clusters except E pure-FE; api-contract changes are doc-only) → skip Bucket 0 Foundation

| # | Bucket | Gap(s) | Priority | Files | Spawn order |
|:-:|--------|--------|:--------:|-------|:-----------:|
| 1 | A | GAP-388 security | 🟠 P1 | `kitehub-subscription/.../beta/{controller,service,repository}/` + email template Thymeleaf + Redis rate-limit | parallel |
| 2 | B | GAP-390 API | 🟠 P1 | `kitehub-branding/.../dto/BrandingJobResponse.java` + `DeployStreamControllerTest` + `documents/01-business/kitehub/ai-branding/api-contract.md` | parallel |
| 3 | C | GAP-389 ops | 🟠 P1 | `scripts/backup-production.sh` (NEW) + `scripts/smoke-test.sh` (extend) + `documents/01-business/kitehub/ai-branding/rules.md` (BR-LIFE/QUALITY blocks) | parallel |
| 4 | D | GAP-393 perf | 🟠 P1 | `kitehub-branding/.../service/RegenerateQuotaService.java` + `DeployStreamController.java` + Caffeine config | parallel |
| 5 | E | GAP-391 UI | 🟠 P1 | `kitehub-frontend/.../RegenerateCounter.tsx` + `useRegenerateQuota` hook + `documents/00-brd/i18n-strategy.md` (NEW doc deferral) | parallel |

### Bucket A — Security P1 cluster (GAP-388 + GAP-387 wire-up)

3 sub-issues bundled (388-A absorbs GAP-387 honeypot dead-wire fix):

- **388-A Honeypot logging + GAP-387 wire-up:** explicit controller check + Micrometer counter `beta.honeypot.rejections.total` + audit log entry với `email`+`IP`. **Context:** 2026-05-07 ops audit found `BetaAccessService.recordHoneypotRejection()` line 181 có 0 callsite — Bean Validation reject `ConstraintViolation` ở MVC layer trước khi reach service, làm `BetaHoneypotSpike` alert là dead rule. **Fix:** thêm controller-level `@ExceptionHandler(ConstraintViolationException.class)` kiểm honeypot field name → invoke `recordHoneypotRejection()` trước khi return 4xx. Closes GAP-387 PARTIAL (flipped 🟢→🟡 sau audit, sẽ re-flip 🟢 DONE trong PR Bucket A)
- **388-B Token 2FA (preferred over S/MIME, 2h):** email contains 6-digit claim code; signup page exchanges claim code for full UUID server-side
- **388-C Per-email rate limit:** Redis key `beta:request:rate:{email_hash}` TTL 24h; idempotent dedupe + 429 + audit log on 2nd attempt different IP

Files (RELATIVE):
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java`
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java`
- `kitehub/kitehub-subscription/src/main/resources/templates/beta-invite.html` (Thymeleaf — replace `${inviteUrl}` plaintext UUID với 6-digit claim code logic)
- New endpoint `POST /api/v1/auth/beta-signup/exchange-claim-code` để swap claim code → UUID
- Tests: 7 new (honeypot logged + counter increments via synthetic ConstraintViolation trigger [GAP-387 regression guard], 2FA happy/wrong code, rate-limit 1st OK / 2nd 429)

### Bucket B — API P1 cluster (GAP-390)

3 sub-issues:

- **390-A:** `BrandingJobResponse.from()` line 54 wire `tenantId` từ `FrontendInstance` (Bucket C InstanceLifecycleService có available)
- **390-B:** `DeployStreamControllerTest` extend với SSE event payload assertions (`state-change`/`progress`/`complete`/`log`)
- **390-C:** `documents/01-business/kitehub/ai-branding/api-contract.md` — replace numeric `12345` examples với UUID `550e8400-e29b-41d4-a716-446655440000`

Files (RELATIVE):
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/dto/BrandingJobResponse.java`
- `kitehub/kitehub-branding/src/test/java/.../DeployStreamControllerTest.java`
- `documents/01-business/kitehub/ai-branding/api-contract.md`

### Bucket C — Ops P1 cluster (GAP-389)

3 sub-issues:

- **389-A:** `scripts/backup-production.sh` (NEW) wrap `aws rds create-db-snapshot`; pre-deploy CI gate trong `.github/workflows/deploy-production.yml`; counter `kite_backup_snapshots_total{type="pre_deploy"}`
- **389-B:** Extend `scripts/smoke-test.sh` — POST beta request → retrieve token → SES delivery validation; cleanup PENDING row post-smoke
- **389-C:** `rules.md` 5-attribute compliance blocks cho BR-LIFE-001..006 + BR-QUALITY-001 per `business-logic-review.md` v1.0.0

Files (RELATIVE):
- `scripts/backup-production.sh` (NEW)
- `scripts/smoke-test.sh` (extend)
- `.github/workflows/deploy-production.yml` (or equivalent — pre-deploy gate)
- `documents/01-business/kitehub/ai-branding/rules.md` (append 2 compliance blocks)

### Bucket D — Performance P1 cluster (GAP-393)

3 sub-issues (status index portion moved to Wave 35 Bucket E):

- **393-A:** `RegenerateQuotaService.getQuota()` add `@Cacheable(value="regenerateQuota", ...)` + `@CacheEvict` on usage record
- **393-B:** `DeployStreamController` SSE emitter timeout + IOException cleanup + bounded `ThreadPoolTaskExecutor`
- **393-D:** Idempotency hash local Caffeine 10min cache trong `RegenerateQuotaService.regenerate()`

Files (RELATIVE):
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/service/RegenerateQuotaService.java`
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/controller/DeployStreamController.java`
- Caffeine config trong `application.yml`

### Bucket E — UI P1 (GAP-391)

2 sub-issues:

- **391-A:** `RegenerateCounter` re-fetch quota post-regenerate via `queryClient.invalidateQueries(['regenerate-quota', jobId])`
- **391-B:** Defer i18n migration — file `documents/00-brd/i18n-strategy.md` (NEW) documenting "Phase 1 = VN-only acceptable"

Files (RELATIVE):
- `kitehub/kitehub-frontend/src/components/branding/wizard/hooks/useRegenerateQuota.ts`
- `kitehub/kitehub-frontend/src/components/branding/wizard/RegenerateCounter.tsx`
- `documents/00-brd/i18n-strategy.md` (NEW doc)

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Verdict |
|--------|------|----------------------|---------|
| `beta-invite.html` Thymeleaf | Template | `find kitehub/kitehub-subscription/src/main/resources -name "beta-invite*"` | ✅ verify-at-spawn (Wave 33 ship) |
| `BrandingJobResponse.from()` | Method | `grep -rn "BrandingJobResponse.from" kitehub/kitehub-branding/src/main/java` | ✅ verify-at-spawn |
| `DeployStreamControllerTest` | Test class | `ls kitehub/kitehub-branding/src/test/.../DeployStreamControllerTest.java` | ✅ verify-at-spawn |
| `scripts/backup-production.sh` | Bash script | `ls scripts/backup-production.sh` | 🆕 to-be-created (Bucket C) |
| `scripts/smoke-test.sh` | Bash script | `ls scripts/smoke-test.sh` | ✅ exists (extend Bucket C) |
| `RegenerateQuotaService.getQuota` | Method | `grep -rn "getQuota" kitehub/kitehub-branding/src/main/java` | ✅ verify-at-spawn |
| `RegenerateCounter.tsx` | FE component | `find kitehub/kitehub-frontend/src -name "RegenerateCounter*"` | ✅ verify-at-spawn |
| `useRegenerateQuota` | FE hook | `grep -rn "useRegenerateQuota" kitehub/kitehub-frontend/src` | ✅ verify-at-spawn (Wave 34 Bucket D) |
| `documents/00-brd/i18n-strategy.md` | Doc | `ls documents/00-brd/i18n-strategy.md` | 🆕 to-be-created (Bucket E) |
| `business-logic-review.md` | Rule | `ls .claude/rules/business-logic-review.md` | ✅ exists v1.0.0 |

---

## 5. Verification Gates

| Bucket | Local verify | CI gate |
|--------|---------------|---------|
| A | `mvn -pl kitehub-subscription verify` | core-ci |
| B | `mvn -pl kitehub-branding test -Dtest='*DeployStream*,*BrandingJobResponse*'` | core-ci |
| C | `bash scripts/backup-production.sh --dry-run` + `bash scripts/smoke-test.sh --staging` | (no CI; manual) |
| D | `mvn -pl kitehub-branding test -Dtest='*RegenerateQuota*,*DeployStream*'` | core-ci |
| E | `pnpm -F kitehub-frontend test:unit -- RegenerateCounter` + `pnpm -F kitehub-frontend build` | frontend-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md`:
- 5 buckets parallel (no Bucket 0 — non-cross-layer)
- Opus medium cho 4 P1 clusters; Sonnet cho UI bucket
- `run_in_background: true` + `isolation: worktree`
- RELATIVE paths trong prompts
- Coordinator merges sequentially A→B→C→D→E

---

## 7. Closure Protocol

- Each bucket PR updates GAP file Log + status (PARTIAL hoặc DONE per `gap-done-discipline.md`)
- ROADMAP §🚀 Next Action updated
- Wave plan `status: complete`
- `wave-history.jsonl` append
- Worktree prune
- **Re-run audit cluster** post-Wave-36 → target Quality ≥85, Security ≥85, Ops ≥70

---

## 8. Log

- **2026-05-07** (draft): Plan created post-audit-cluster (PR #913). Wave 36 prerequisite Wave 35 complete. Pairs trong cùng plan PR với Wave 35.
- **2026-05-07 (complete) — SHIPPED:** 5 PRs A→B→C→D→E sequential merged: #933 Bucket A (GAP-387 + GAP-388 DONE, 7 tests, mvn 444+33/444+33), #930 Bucket B (GAP-390 DONE, 4 SSE assertions + 2 tenantId tests, mvn 231/231), #929 Bucket C (GAP-389 DONE, backup-production.sh NEW + smoke extend + 7 BR-LIFE/QUALITY 5-attr blocks, shellcheck clean), #932 Bucket D (GAP-393 DONE, Caffeine quota cache + SSE backpressure cap=20 + idempotency cache, 4 tests + 1 expanded, mvn 229/229; coordinator rebase conflict resolved on `DeployStreamControllerTest.java` additive merge — kept HEAD's 4 SSE tests + appended Bucket D's 2 backpressure tests; force-push + CI rerun green per `admin-merge-discipline.md` §2), #931 Bucket E (GAP-391 DONE, RegenerateCounter quota refresh regression test + i18n-strategy.md NEW 120 lines VN, pnpm test 4/4 + build clean). All 6 gap files flipped per `gap-done-discipline.md` §2 (no banned phrases, AC checked, verification artifacts cited). Worktree prune executed 5 husks + 5 branches. 72nd consecutive 0-clarification streak (5 agents + 1 retry agent E Opus after Sonnet thrashed autocompact). Wall-clock ~50min parallel (longest Bucket A 14min) + ~10min coordinator merge cycle + Bucket D conflict resolve ~5min + closure ~10min ≈ 75min total.

- **2026-05-07** (active, post-audit re-run): Audit suite 7 specialists chạy parallel sau Wave 35 SHIPPED. Aggregate scores: Quality 80/100 B (+7), Security 84/100 B (+12), Performance 71/100 C (+13), Business Logic 82/100 B− (+4), API Contract 71/100 C (−1 inventory expand), Ops Readiness 53/100 F (+3, 1 NEW P0), UI 99/128 A+ (+2). Phase 1 BETA trigger gate Quality ≥80 ✅ vừa đủ. NEW P0 GAP-387 honeypot dead-wire absorbed vào Bucket A 388-A (controller `@ExceptionHandler` + GAP-387 status flip 🟢→🟡→🟢 trong cùng PR). Carry-over P0 GAP-144 AlertManager + backup script — backup script đã trong Bucket C scope; AlertManager không block Phase 1 BETA invite-only. Plan flipped status: draft → active. Reports: `documents/04-quality/audits/{quality,security,performance,business-logic,api-contract,ops-readiness,ui}/2026-05-07-post-wave-35.md`. Spawning 5 background agents.
