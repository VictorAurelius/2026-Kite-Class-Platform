---
audience: dev
topic: self-test-readiness-plan
last-updated: 2026-05-21
version: 1.0.0
effort_minutes: 10
---

# Self-Test Readiness Plan — Dependency-ordered fix sequence

> Goal: solo dev có thể execute end-to-end self-test (admin login → onboarding wizard → tenant init) **sớm nhất** trên local stack (WSL2 + Docker Desktop), KHÔNG depend on AWS production stack.

**Cross-link:** [`GAP-695`](../../04-quality/gaps/phase-1-beta/GAP-695-self-test-readiness-comprehensive-plan.md) — parent catalog với full gap enumeration per tier.

---

## 1. TL;DR

- **Current state:** 30+ phase-1-beta P0 gaps phân tán; KHÔNG có single sequenced plan → mỗi session restart mất 1-2h mental dependency-graph rebuild
- **Target state:** 4-tier ordered execution → self-test working end-to-end trong ~6-7h dev effort (critical path Tier 0-2; Tier 3 polish optional cho beta cohort)
- **Critical path:** Tier 0 (Docker + .env + JVM heap, ~1-1.5h) → Tier 1 (auth flow + gateway routing, ~2h) → Tier 2 (business flow walks, ~3-4h)
- **Sequencing logic:** Tier N+1 chỉ feasible khi Tier N exit gate PASS — không skip ahead
- **Force-multiplier:** 1 plan → mọi session subsequent reference, eliminate re-derivation cost

---

## 2. Dependency graph

```
Tier 0 — Stack startup (FOUNDATION)
   │
   │ exit gate: docker ps shows 4+ services UP + actuator/health 200
   ▼
Tier 1 — Endpoint reachability + auth flow
   │
   │ exit gate: admin@kitehub.me login → /admin redirect → /admin/beta-requests render
   ▼
Tier 2 — Business flow execution
   │
   │ exit gate: owner persona walks tenant init + onboarding 5-step + RBAC checks PASS
   ▼
Tier 3 — Data realism + polish (OPTIONAL cho self-test; MANDATORY cho beta cohort UX)
   │
   │ exit gate: VN content 100% + email tone audit PASS + zero English placeholder
   ▼
Self-test execution COMPLETE end-to-end
```

**Critical path (minimum subset cho "self-test working"):** Tier 0 → Tier 1 → Tier 2 = ~6-7h cumulative effort.

**Tier 3 trade-off:** không block self-test execution itself, nhưng block beta cohort velocity (per `user-manual-content-standard.md` §2 + Phase 1 BETA acceptance gate). Defer to Phase 4 sau khi Tier 0-2 working.

---

## 3. Ordered fix sequence

### Phase 1 — Tier 0 execution (~1-1.5h)

| # | Action | Gap | Effort | Verify command |
|---|--------|-----|--------|----------------|
| 1.1 | Launch Docker Desktop trên Windows host (user action per `agent-action-bias.md` §1 Part B) | GAP-694 fix #1 | ~5min | `wsl.exe -l -v` → `docker-desktop Running` + `docker version` returns Client+Server |
| 1.2 | Append 9 missing keys vào `kitehub/.env` với dev-safe defaults (hCaptcha test keys + Ollama endpoint + AI_PROVIDER + tenant pattern) | GAP-694 fix #2 | ~10min | `diff <(grep -oE "^[A-Z_]+" kitehub/.env.example \| sort -u) <(grep -oE "^[A-Z_]+" kitehub/.env \| sort -u)` → empty |
| 1.3 | (META P2 optional) Ship `kitehub/scripts/check-docker.sh` preflight + integrate `up.sh`/`setup.sh` line 1 | GAP-694 fix #3 | ~30-45min | `bash kitehub/scripts/check-docker.sh` → exit 0 khi daemon reachable; auto-launch khi không |
| 1.4 | Set JVM heap cap dev profile `-XX:MaxRAMPercentage=60` | GAP-408 | ~15min | `docker stats --no-stream` → backend services <500MB each |
| 1.5 | Cold-start infra-only profile | n/a (orchestration) | ~5min | `bash kitehub/scripts/up.sh --profile infra-only` → 4 services UP |

**Exit gate Phase 1:** `docker ps` shows ≥4 services UP (Postgres + Redis + RabbitMQ + MinIO healthy) + `/actuator/health` 200 cho core services.

### Phase 2 — Tier 1 execution (~2h)

| # | Action | Gap | Effort | Verify command |
|---|--------|-----|--------|----------------|
| 2.1 | Scale up to `branding-only` profile (add kitehub-branding + frontend) | n/a | ~5min | `bash kitehub/scripts/up.sh --profile branding-only` → branding service health |
| 2.2 | Verify gateway routing config (no 404 cho admin v1 routes) | GAP-481 | ~15min | `curl http://localhost:9000/api/v1/auth/login -X POST -d '{}'` → 400 (validation error, NOT 404) |
| 2.3 | Live walk admin login per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) | GAP-518 closure | ~15min | Browser: login admin@kitehub.me → redirect `/admin` → `/admin/beta-requests` renders với data |
| 2.4 | Ship GAP-519 sidebar nav links (`/admin/beta-requests` + `/admin/onboarding` etc.) | GAP-519 | ~30min | Browser: post-login → sidebar visible với 3+ admin links |
| 2.5 | Verify GAP-520 JWT secret rotation dual-key local | GAP-520 | ~20min | Rotate secret → old JWT vẫn accept trong overlap window → new JWT minted với new key |
| 2.6 | Close GAP-518 PARTIAL 97% → DONE 100% với live verify evidence (eliminate AWS dependency cho code-side closure) | GAP-518 closure | ~5min | Update gap file Status + CSV row per `post-merge-sync-completeness.md` §4 |

**Exit gate Phase 2:** admin@kitehub.me logs in → `/admin` redirect → `/admin/beta-requests` page renders với data → sidebar nav visible.

### Phase 3 — Tier 2 execution (~3-4h)

| # | Action | Gap | Effort | Verify command |
|---|--------|-----|--------|----------------|
| 3.1 | Scale up to `full` profile (all services) | n/a | ~5min | `bash kitehub/scripts/up.sh --profile full` → all 14+ services UP |
| 3.2 | Backfill `@PreAuthorize` cho admin v1 controllers + 403 ITs | GAP-637 | ~45min | `cd kitehub && ./mvnw -pl kitehub-subscription verify` → 403 tests PASS |
| 3.3 | Execute Wave 101 Bucket D Playwright E2E spec cho Day-1 onboarding | GAP-538 closure | ~30min | `cd kitehub-frontend && pnpm test:e2e onboarding-day1.spec.ts` → 5-step VN checklist PASS + IMPORT_DATA opt-in PASS + no-English-placeholder PASS |
| 3.4 | Walk POST /tenants → confirm subdomain → seed Day-1 data | GAP-531 | ~45min | Browser: signup tenant → confirm subdomain `{slug}.localhost:8088` → onboarding wizard 5 steps complete → sample data import OK |
| 3.5 | 2FA TOTP verify flow OR document disabled trong dev env | GAP-516 | ~30min | Login với TOTP code → /admin OR `.env` SECURITY_2FA_ENABLED=false documented |
| 3.6 | Execute Wave 92 Bucket D live verify admin v1 controllers (paired GAP-637) | GAP-620 | ~20min | Live walk per `pre-handoff-self-test-completeness.md` §2.4 trên admin v1 endpoints |

**Exit gate Phase 3:** owner persona walks tenant init → onboarding wizard 5 steps complete → sample data import OK + RBAC check PASS (staff cannot access admin routes).

### Phase 4 — Tier 3 polish (~5-6h, optional cho self-test)

| # | Action | Gap | Effort |
|---|--------|-----|--------|
| 4.1 | VN sample seed worker — replace English placeholder | GAP-658 | ~1h |
| 4.2 | Staff-invite email persona-tone split | GAP-659 | ~30min |
| 4.3 | Email content audit — 5 critical templates VN tone | GAP-543 | ~45min |
| 4.4 | Email layer hardening (plain-text + List-Unsubscribe + Reply-To) | GAP-657 | ~30min |
| 4.5 | kc-student real REST endpoints (today/grades/payments/notifications) | GAP-269b | ~2h |
| 4.6 | KiteClass Landing Hero duplicate fix | GAP-138 | ~5min |
| 4.7 | Parent Dashboard widgets scaffold | GAP-139 | ~2h |

**Exit gate Phase 4:** beta cohort walkthrough render với 100% Vietnamese content + zero placeholder data + email tone audit PASS per `user-manual-content-standard.md` §2 row 7.

---

## 4. Critical path — minimum subset cho "self-test working"

**Critical path:** Phase 1 (Tier 0) + Phase 2 (Tier 1) + Phase 3 (Tier 2) = **~6-7h dev effort** cho:

- ✅ Stack starts clean từ cold state
- ✅ Admin login works end-to-end với browser walkthrough
- ✅ Owner persona walks tenant init + onboarding 5-step VN
- ✅ RBAC checks PASS (admin/owner/staff persona separation)
- ✅ GAP-518 + GAP-538 + GAP-637 + GAP-620 closure local (bypass AWS dependency)

Skip Phase 4 (Tier 3) khi self-test execution = target; revisit cho beta cohort acceptance gate.

---

## 5. Optional enhancements

### 5.1 Beta tenant onboarding readiness (Tier 3 priority)

- VN data realism (GAP-658) → beta tenant không thấy "John Doe" placeholder
- Email tone audit (GAP-543 + GAP-659) → invite emails đọc tự nhiên Vietnamese
- Email layer hardening (GAP-657) → deliverability tăng + List-Unsubscribe compliance

### 5.2 Self-test loop automation (Wave subsequent META)

- Ship `scripts/local/smoke-e2e.sh` Playwright walkthrough automation (per GAP-694 §Proposed Fix Phase 0B sub-item)
- Wire smoke vào `kitehub/scripts/up.sh --smoke` flag — runtime ≤5min target
- Codify rule `local-self-test-before-aws-deploy.md` v1.0.0 mandate local smoke PASS trước AWS deploy trigger

### 5.3 Persistence pattern cho Tier 0 fix

- Docker Desktop auto-start on Windows login (Settings → General → "Start Docker Desktop when you sign in") — eliminate Tier 0 fix #1 recurrence
- Document trong `wsl2-fresh-setup.md` Phase 0 prerequisites

---

## 6. Time estimate total

| Phase | Tier | Effort | Cumulative |
|-------|------|--------|------------|
| 1 | Tier 0 — Stack startup | ~1-1.5h | ~1-1.5h |
| 2 | Tier 1 — Endpoint + auth | ~2h | ~3-3.5h |
| 3 | Tier 2 — Business flow | ~3-4h | ~6-7.5h |
| **Critical path total** | — | **~6-7.5h** | **~1-2 dev days** |
| 4 | Tier 3 — Data realism (optional) | ~5-6h | ~11-13.5h |
| **Full readiness total** | — | **~11-13.5h** | **~2-3 dev days** |

**Realistic estimate cho critical path:** **1-2 dev days** (assuming sequential focus + no Tier 0 blocker recurrence).

**Realistic estimate cho full readiness (beta cohort quality):** **2-3 dev days**.

---

## 7. Recommended next session action

1. **User action first** (~5min): Launch Docker Desktop trên Windows host per Phase 1 step 1.1
2. **Verify Phase 1 exit gate:** `bash kitehub/scripts/up.sh --profile infra-only` → 4 services UP
3. **Spawn session-pack:** parallel agents Phase 2 (admin login walk + GAP-519 sidebar + GAP-481 routing verify) + Phase 3 (GAP-637 @PreAuthorize backfill) per `wave-pack-planner` SKILL.md pattern
4. **Close gap chain:** GAP-518 → GAP-519 → GAP-538 → GAP-637 → GAP-620 trong cùng wave window
5. **Defer Tier 3** cho follow-up wave sau khi self-test execution working

---

## 8. Cross-references

- **Parent gap:** [`GAP-695`](../../04-quality/gaps/phase-1-beta/GAP-695-self-test-readiness-comprehensive-plan.md) — comprehensive catalog
- **Tier 0 sub-gap:** [`GAP-694`](../../04-quality/gaps/phase-1-beta/GAP-694-local-self-test-investigation-fix.md) — Docker + .env + preflight
- **Tier 0 audit:** [`local-stack/2026-05-21-local-self-test-investigation.md`](../../04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md) — Phase 0A findings
- **Downstream:** [`GAP-693`](../../04-quality/gaps/phase-1-beta/GAP-693-aws-rebuild-sop-playbook.md) — AWS rebuild SOP (depends on local self-test working)
- **Parallel:** [`GAP-612`](../../04-quality/gaps/phase-1-beta/GAP-612-aws-account-suspension-recovery.md) — AWS account suspension (local stack bypasses dependency)
- **Existing setup:** [`wsl2-fresh-setup.md`](wsl2-fresh-setup.md) — 60-90 min full WSL2 setup
- **Existing playbook:** [`wsl-migration-playbook.md`](wsl-migration-playbook.md) — existing Windows install migration
- **Existing non-WSL:** [`local-dev-setup-non-wsl.md`](local-dev-setup-non-wsl.md) — Mac/Linux native alternative

## 9. Rules invoked

- `.claude/rules/pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (verification chain mandate Phase 2)
- `.claude/rules/release-deploy-standard.md` §3.1 PRE-RELEASE "Smoke admin-login" (post-deploy gate parallel)
- `.claude/rules/production-env-config-registry.md` v1.1.0 (runtime env coverage Phase 1 step 1.2)
- `.claude/rules/user-manual-content-standard.md` §2 (Tier 3 VN data + email quality criteria)
- `.claude/rules/meta-gap-priority.md` §3 META P0 force-multiplier (catalog scope justification)
- `.claude/rules/outside-in-coverage-trigger.md` v1.1.0 (Phase 0 outside-in synthesis 2026-05-21 origin)
- `.claude/rules/agent-action-bias.md` §1 Part B (command-over-UI per Phase 1 step 1.1)
- `.claude/rules/post-merge-sync-completeness.md` §4 (gap closure sync per Phase 2 step 2.6)
- `.claude/rules/gap-done-discipline.md` §3 PARTIAL exit ramp (GAP-695 stays PARTIAL until 4 phases shipped)
