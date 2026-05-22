---
title: Wave 105 — Persona Walk + P0 Security Cluster + META Local-Fix Prod-Parity (Beta-Readiness)
status: draft
created: 2026-05-22
updated: 2026-05-22
waves: [105]
gaps: [GAP-716, GAP-717, GAP-718]
audience: dev
phase: phase-1-beta
---

# Wave 105 — Persona Walk + P0 Security Cluster + META Local-Fix Prod-Parity (Beta-Readiness)

**Goal:** Ensure beta user (4 personas: Anonymous / Owner / Teacher / Parent) không gặp bug khi go live Phase 1 BETA. Combine inside-out persona walk (5 buckets) + outside-in audit findings (P0 security cluster + UX gaps) + GAP-716 audit obligations + **META rule GAP-718 local-fix production-parity check + concrete GAP-717 JWT_CHALLENGE_SECRET production parity (Bucket E0 priority FIRST per `meta-gap-priority.md` §3)**.

**Trigger:** Wave 104.5 close-loop user direction "test full để beta user không gặp lỗi" → spawned 3 outside-in audit agents per `outside-in-coverage-trigger.md` v1.1.0 §3 — surfaced 5 P0 real-code bugs + 6 outside-in UX gaps that inside-out endpoint-coverage approach would have missed. **Amendment 2026-05-22 same session**: user-flagged additional meta gap "fix local có check fix env production chưa?" → surfaced GAP-717 concrete (JWT_CHALLENGE_SECRET local-only) + GAP-718 meta rule (no existing rule covers code-fix → prod-env-sweep direction).

## 1. Brainstorm

### Q1 — Inside-out + Outside-in scope

**Inside-out source (dev brainstorm — Wave 104.5 close-loop):**

- Original 5-bucket Persona Walk plan (Anonymous / Owner / Teacher / Parent + GAP-716 audit obligations)
- Endpoint coverage approach — walk 4 persona journeys

**Outside-in source 1 — Persona Simulation audit** (`documents/04-quality/audits/persona-review/2026-05-22-wave-105-persona-simulation.md`):

- Verdict 4 buckets: A PARTIAL / B FAIL / C PARTIAL / D FAIL
- 5 cross-persona critical gaps: FE port quirk WSL2 + PDPL UX + Zalo OA absent + VN tone + VietQR/cash idempotency
- Bucket D Parent FAIL nặng nhất (6 critical gaps cộng dồn: Zalo culture + email-only + mobile-only + multi-child + parent-PDPL + VietQR-not-credit-card)
- Bucket B Owner FAIL do draft order ngược business reality (manual-then-bulk thay vì bulk-first)

**Outside-in source 2 — VN Edu SaaS Benchmark audit** (`documents/04-quality/audits/persona-review/2026-05-22-wave-105-vn-saas-benchmark.md`):

- Alignment 65-70% với VN edu SaaS norm
- Critical industry gap: **Zalo OA parent channel** — 3/3 VN competitors (DotB/EduSpace/CloudClass) prioritize; KiteHub email-only industry outlier
- Moderate gap: Zalo Mini App mobile
- KiteHub differentiators: explicit PDPL Art 11 consent (competitor implicit) + TOTP 2FA mandatory Teacher (competitor không enforce)

**Outside-in source 3 — Failure-Mode Matrix audit** (`documents/04-quality/audits/persona-review/2026-05-22-wave-105-failure-mode-matrix.md`):

- 18 failure scenarios audited across 4 buckets
- **5 P0 real-code bugs surfaced** (not theoretical):
  1. **B1/D1 PaymentController hardcoded `userId=1L`** — Beta-blocker, every payment ghi user_id=1, audit trail broken
  2. A4 stored XSS in beta-request name/orgName fields
  3. A1 beta-request no idempotency → double-click tạo 2 row
  4. B5 enrollment race on FULL class (TOCTOU)
  5. C3/D3 per-resource authz (OWASP A01) — teacher↔class scope + parent↔child scope
- 5 P1 candidates Wave 106
- 3 systemic patterns: no idempotency anywhere, JWT extraction inconsistent, per-resource authz untested

### Q2 — Scope decision

**7 buckets (was 5, then 6, now 7 — added META Bucket E0 FIRST per `meta-gap-priority.md` §3 force-multiplier):**

| Bucket | Source | Scope | Priority |
|---|---|---|---|
| **🆕 E0 META local-fix prod-parity** | User-flagged meta gap Wave 104.5 close-loop amend | Ship `.claude/rules/local-fix-production-parity-check.md` v1.0.0 (GAP-718) + fix GAP-717 JWT_CHALLENGE_SECRET production parity (terraform secret + IAM + deploy script + env-vars-registry) | **HIGHEST (META P0)** — fix first |
| A Anonymous | Inside-out + 3 outside-in findings | Walk + PDPL UX redesign + Mobile fallback (ngrok) + XSS fix + idempotency fix | P0 |
| B Owner | Inside-out + 4 outside-in findings | Walk + bulk-import-first reorder + VietQR setup + multi-branch defer note + invoice delivery test | P0 |
| C Teacher | Inside-out + 3 outside-in findings | Walk + per-class authz + 2FA clock skew test | P0 |
| D Parent | Inside-out + 5 outside-in findings | Walk + VietQR idempotency + multi-child authz + parent-PDPL consent + Zalo OA stub | P0 |
| E Security P0 cluster | Outside-in failure-mode only | Fix PaymentController userId + XSS + idempotency + enrollment race + per-resource authz | P0 |
| F GAP-716 audit obligations | Inside-out (deadline 2026-05-25) | 3 audits + 2 doc syncs + 3 IT tests | P1 |

### Q3 — Acceptance criteria

- [ ] **Bucket E0 META ship FIRST** — `local-fix-production-parity-check.md` v1.0.0 + GAP-717 production parity (terraform + deploy script + registry)
- [ ] 4 persona walks completed với explicit fix cho 6 cross-persona gaps
- [ ] 5 P0 real-code bugs FIXED + verified live (PaymentController + XSS + idempotency + enrollment race + per-resource authz)
- [ ] GAP-716 audit obligations satisfied trước deadline 2026-05-25
- [ ] Zalo OA stub endpoint shipped (defer full integration Wave 106)
- [ ] Mobile fallback path documented (override trailer + ngrok smoke OR explicit defer with follow-up gap)
- [ ] No new P0 surfaced (P1 → file follow-up Wave 106)
- [ ] All outside-in audit gaps either fixed OR documented defer trong wave plan §Open Items
- [ ] GAP-717 + GAP-718 → DONE flip per `gap-done-discipline.md` §2 (E0 ship satisfies both)

### Q4 — Risks

1. **Scope creep risk** — 6 buckets vs original 5; effort ~32h vs ~20h. Mitigation: split nếu cần Wave 105 + Wave 105.5
2. **AWS still suspended (GAP-612)** — local-only verify; production cutover blocked. Mitigation: explicit override per `release-deploy-standard.md` §5
3. **FE port quirk WSL2** — mobile browser walk impossible from CLI. Mitigation: ngrok tunnel OR document defer + follow-up gap
4. **PaymentController fix scope creep** — may surface other `userId=1L` hardcoded sites. Mitigation: grep audit + fix scope cap (Wave 105 = PaymentController only, others Wave 106)
5. **Per-resource authz refactor** — `@PreAuthorize` annotations require `@authz` bean implementation. Mitigation: minimal `@authz.hasAccessTo*` helpers Wave 105, full RBAC Wave 107
6. **PDPL UX redesign legal review** — parent-on-behalf-of-child consent needs counsel sign-off. Mitigation: technical implementation Wave 105, legal sign-off deferred per `business-logic-review.md` GAP-156

## 2. Task Breakdown

| # | Task | Owner | Bucket | Est. effort |
|---|---|---|---|---|
| 1 | **Bucket E0 (FIRST — META P0)** — ship meta rule `local-fix-production-parity-check.md` v1.0.0 + memory pair + fix GAP-717 (terraform secret + IAM + deploy script + env-vars-registry) | Coordinator inline (Opus full) | E0 | 4h |
| 2 | Spawn 5 background agents (A/B/C/D/E) + coordinator inline Bucket F | Coordinator | Pre-spawn | 0.5h |
| 3 | Bucket A — Anonymous walk + PDPL UX + mobile fallback + XSS fix + idempotency | Agent A (Opus medium) | A | 5h |
| 4 | Bucket B — Owner walk + bulk-import-first reorder + VietQR + multi-branch defer | Agent B (Opus full) | B | 7h |
| 5 | Bucket C — Teacher walk + per-class authz + 2FA clock skew | Agent C (Opus full) | C | 5h |
| 6 | Bucket D — Parent walk + VietQR idempotency + multi-child authz + parent-PDPL + Zalo OA stub | Agent D (Opus full) | D | 8h |
| 7 | Bucket E — Security P0 cluster (5 real-code bug fixes + IT tests) | Agent E (Opus full) | E | 8h |
| 8 | Bucket F — GAP-716 audit obligations (3 audits + 2 doc syncs + 3 IT tests) | Coordinator inline (Sonnet) | F | 6h |
| 9 | Coordinator cherry-pick all 7 buckets to wave/105 branch | Coordinator | Closure | 1h |
| 10 | Wave closure PR + CI wait + squash merge | Coordinator | Closure | 1h |
| 11 | Post-merge sync (CSV + ROADMAP + wave-history + MEMORY) | Coordinator | Post-merge | 0.5h |
| 12 | Post-wave audit suite ≤3 ngày | Coordinator | Post-wave | 4h (deadline +3d) |

**Total: ~50h (36h agent + coordinator work + ~5h coord + 4h post-wave + ~5h misc)**

**Order rationale per `meta-gap-priority.md` §3:** Bucket E0 META P0 ship FIRST → prevents future iteration of same gap class. Bucket E0 doesn't block A/B/C/D agent spawn — coordinator inline Bucket E0 SAME WAVE PR ngay khi plan amend merge, parallel agents A-E spawn sau khi E0 commit lands.

### Out-of-scope (Wave 106+)

- File upload magic-byte validation + ClamAV
- 2FA clock skew ±60s/±90s boundary IT test
- Concurrent admin approve race optimistic lock
- Multipart upload size cap + VN error i18n
- Webhook idempotency + signature TTL
- Bulk-import Excel UX Misa-style polish
- Offline-resilient attendance service-worker queue
- Multi-branch routing full UX
- Zalo OA + Zalo Mini App full integration (Phase 2 per GAP-286)
- Production AWS cutover (GAP-612 unblock dependency)
- 4 KH services endpoint-level test (admin/branding/platform/frontend)
- ~190 KC endpoints NOT in 4 persona journeys (defer per `quality-audit` statistical sampling)

## 3. Scope

### Bucket E0 — META local-fix prod-parity (SHIP FIRST per `meta-gap-priority.md` §3)

- Files:
  - `.claude/rules/local-fix-production-parity-check.md` v1.0.0 (NEW per GAP-718 §Proposed body)
  - `infrastructure/terraform-aws/secrets.tf` + `iam-deploy.tf` (NEW resources per GAP-717 §Proposed Fix)
  - `scripts/deploy-prod.sh` (env injection)
  - `documents/02-architecture/env-vars-registry.md` (row addition)
  - `documents/04-quality/rules-index.csv` + `output-review-mandate.md` §3 (cross-link sync)
- Acceptance:
  - [ ] `.claude/rules/local-fix-production-parity-check.md` v1.0.0 ship với 9 sections + reviewer-checklist + override mechanism
  - [ ] Memory entry `feedback_local_fix_production_parity.md` paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity
  - [ ] §6 worked self-test retroactive Wave 104.5 incident
  - [ ] `output-review-mandate.md` §3 matrix row added cho "Local fix production parity"
  - [ ] `rules-index.csv` + `documents/02-architecture/env-vars-registry.md` updates
  - [ ] GAP-717 fix: terraform secret + IAM grant + deploy script env injection per §Proposed Fix
  - [ ] Live verify deferred per GAP-612 AWS unblock dependency (runbook documents expected curl smoke)
- Spawn: coordinator inline (Opus full, security + IaC), ~4h

### Bucket A — Anonymous Prospect (Em Vy)

- Files: `kitehub-frontend/src/app/signup` + `kitehub-frontend/src/app/page.tsx` + sanitization utils
- Acceptance:
  - [ ] Curl walk landing → beta-request → approval email → invite link → signup PASS
  - [ ] Mobile fallback: ngrok tunnel + real iPhone Safari smoke OR explicit override trailer + follow-up gap
  - [ ] PDPL consent checkbox explicit với VN summary; tick-all-skip-read invalid
  - [ ] XSS protection: name/orgName sanitize on input + verify admin panel render escapes
  - [ ] Idempotency: partial unique index `(email) WHERE status=PENDING` + FE button debounce 1s
- Spawn: ~5h, Opus medium

### Bucket B — Owner (Chị Hằng)

- Files: `kitehub-frontend/src/app/(dashboard)/onboarding` + `kiteclass-frontend/src/app/(dashboard)/students` + `BulkImportController.java`
- Acceptance:
  - [ ] Walk full Owner journey: login → onboarding wizard → branding setup → bulk-import 50 students FIRST → create class → invite teacher → enroll students → invoice
  - [ ] Reorder draft: bulk-import-first (Step 5 thành Step 4) per Hằng business reality
  - [ ] VietQR billing account setup verified (mock locally OK)
  - [ ] Invoice delivery: PDF generated + Zalo OA stub log "would send invoice" + email backup OK
  - [ ] Multi-branch routing: documented defer Wave 106 với explicit FAQ "Phase 1 BETA single-branch only"
- Spawn: ~7h, Opus full

### Bucket C — Teacher (Anh Tâm)

- Files: `kiteclass-frontend/src/app/(teacher)/attendance` + `AttendanceClassBatchController.java` + `@PreAuthorize` annotations
- Acceptance:
  - [ ] Walk Teacher journey: accept invite → 2FA enroll → view assigned class → record attendance → record grade → upload assignment
  - [ ] Per-class authz: teacher KHÔNG record attendance/grade cho class NOT assigned (test cross-class spoof)
  - [ ] 2FA clock skew ±60s test PASS (boundary)
  - [ ] Dual-role (Teacher + Manager) RBAC scope switch verified
- Spawn: ~5h, Opus full (security-sensitive)

### Bucket D — Parent (Chị Linh)

- Files: `kiteclass-frontend/src/app/(parent)/**` + `ParentController.java` + `ParentTranscriptController.java` + `PaymentController.java`
- Acceptance:
  - [ ] Walk Parent journey: receive invite → PDPL consent → setup password → view child (1 of 2 children) attendance + grade → pay invoice via VietQR
  - [ ] Parent-on-behalf-of-child PDPL consent variant (data trẻ <16t khác data bản thân)
  - [ ] Multi-child authz: Linh có 2 con, chỉ xem được con A khi click vào con A; spoof childId=B → 403
  - [ ] VietQR idempotency: pay 2× → 1 payment row + 1 QR code (Idempotency-Key header)
  - [ ] Zalo OA notification stub: log "would send Zalo OA" cho 3 events (invite + payment confirm + attendance alert) — full integration Wave 106
- Spawn: ~8h, Opus full

### Bucket E — Security P0 cluster (5 real-code bug fixes)

- Files: `PaymentController.java` (line 49, 69) + `BetaAccessController.java` + `EnrollmentController.java` + new `@authz` bean
- Acceptance:
  - [ ] `PaymentController.createPayment` inject Authentication principal, extract userId từ JWT — KHÔNG hardcoded `1L`
  - [ ] Beta-request `name`/`orgName` sanitize on input (Jsoup OR @SafeHtml validator)
  - [ ] Beta-request DB unique partial index `(email) WHERE status=PENDING`
  - [ ] Enrollment `@Version` optimistic lock on class.currentCount OR `SELECT FOR UPDATE`
  - [ ] Per-resource authz `@authz.hasAccessToClass(classId)` + `@authz.hasAccessToChild(childId)` helpers + applied to 8 critical endpoints (teacher attendance + grade + parent transcript + parent fees)
  - [ ] IT tests covering each fix per `pre-handoff-self-test-completeness.md` §2 admin-flow checklist
- Spawn: ~8h, Opus full (security-critical)

### Bucket F — GAP-716 audit obligations (deadline 2026-05-25)

- Files: `documents/04-quality/audits/{business-logic,api-contract,ops-readiness}/2026-05-22-wave-104.5-*.md` + `documents/01-business/auth/api-contract.md` + `documents/01-business/onboarding/api-contract.md` + `AdminAuditLogPostgresIT.java` + 2 unit tests
- Acceptance per GAP-716:
  - [ ] 3 audits run + reports filed
  - [ ] `audits-index.csv` rows added
  - [ ] `01-business/auth/api-contract.md` + `01-business/onboarding/api-contract.md` synced với Wave 104.5 semantic changes
  - [ ] `AdminAuditLogPostgresIT` Testcontainers IT shipped + CI green
  - [ ] `TenantResolverGatewayFilterFactoryTest` + `OnboardingProgressControllerResolveTenantTest` unit tests
  - [ ] audit-gate.py compliance returns 4/4
- Spawn: ~6h, Sonnet (mechanical scope)

## 4. State-Check Evidence

| Item | Verified | Source |
|---|---|---|
| Wave 104.5 PRs merged | ✅ | PRs #1715 + #1716 + #1717 |
| GAP-716 follow-up exists + deadline 2026-05-25 | ✅ | `documents/04-quality/gaps/phase-1-beta/closed/GAP-716-*.md` (DONE filed; obligations open) |
| 3 outside-in audit reports filed | ✅ | `documents/04-quality/audits/persona-review/2026-05-22-wave-105-{persona-simulation,vn-saas-benchmark,failure-mode-matrix}.md` (this PR) |
| Local stack healthy | ✅ | 13/13 services, Wave 104.5 Bucket E verify confirmed |
| AWS suspended (GAP-612) | ⚠️ | Status unchanged, local-only verify per `agent-aws-access.md` |
| KH backend self-test passed (Wave 104.5) | ✅ | Bucket E live verify audit doc |
| KC core CRUD verified | ✅ | `documents/04-quality/audits/local-stack/2026-05-22-wave-104.5-kc-multi-tenant-walk.md` |
| Multi-tenant isolation verified gateway-level | ✅ | KC walk audit §Multi-tenant isolation matrix |
| 5 P0 real-code bugs identified | ✅ | Failure-mode matrix audit §Top 5 P0 |
| GAP-717 concrete prod-parity bug filed | ✅ | `documents/04-quality/gaps/phase-1-beta/GAP-717-jwt-challenge-secret-production-parity.md` (this PR) |
| GAP-718 META rule proposed | ✅ | `documents/04-quality/gaps/phase-1-beta/GAP-718-local-fix-production-parity-meta-rule.md` (this PR) |

## 5. Verification Gates

| Gate | Check | When |
|---|---|---|
| Pre-spawn | 3 audit reports filed + Wave 105 plan PR merged | Before agent spawn |
| Per-bucket | Each agent commits to own branch, no shared push | During execution |
| Pre-merge | Each bucket PR `./mvnw verify -P strict-warnings` PASS + IT tests green | Before bucket squash |
| **Bucket E special gate** | `pre-handoff-self-test-completeness.md` §2 admin/security checklist 7/7 PASS | Before Bucket E squash |
| Pre-closure | All 6 buckets DONE + GAP-716 obligations met | Before wave closure PR |
| Post-closure | `audit-gate.py` post-merge compliance 4/4 (no new audit obligations) | Post-merge |

## 6. Agent Spawn Pattern

- **Coordinator inline**: GAP-716 Bucket F (mechanical scope, low context cost)
- **Parallel agents**: 5 background (A/B/C/D/E)
- **Stake tier**: HIGH (P0 security cluster + persona walks user-facing)
- **Model**: Bucket E Opus full (security-sensitive); Buckets B/D Opus full (cross-controller); Buckets A/C Opus medium; Bucket F Sonnet
- **Max 5 parallel** per `feedback_parallel_agent_strategy.md` rule #9
- **All `run_in_background: true`** per `agent-background-spawn-default.md`

## 7. Closure Protocol

1. Each bucket PR squash-merged to `wave/105-persona-walk-beta-readiness`
2. PR `wave/105 → main` với:
   - 6 bucket fix summary
   - 5 P0 bug fix evidence (before/after curl walks)
   - 4 persona walk audit doc per bucket
   - Wave 104.5 audit obligations GAP-716 satisfied evidence
3. Wait CI green (no `--admin`)
4. Post-merge sync per `post-merge-sync-completeness.md` §2 (4 targets: CSV + ROADMAP + wave-history.jsonl + MEMORY)
5. Post-wave audit suite ≤3 ngày per `post-wave-audit-mandate.md` §2.2
6. Update PR template + audit gate to track this Wave's scope

### Open Items / Follow-ups

Per `gap-done-discipline.md` §3 PARTIAL exit ramp:

- [ ] **Multi-branch routing Wave 106** — Hằng (Owner B) requires; Phase 1 BETA single-branch only documented
- [ ] **Zalo OA full integration Wave 106** — extends GAP-286 stub from this wave
- [ ] **Mobile UI walk Wave 106** — Docker host port quirk WSL2 fix OR ngrok-only smoke
- [ ] **File upload validation Wave 106** — magic-byte + ClamAV stub per `pre-handoff-self-test-completeness.md` §2.5
- [ ] **2FA clock skew boundary IT Wave 106** — ±60s/±90s/±120s test cases
- [ ] **AWS production cutover** — blocked GAP-612; deferred until account restored
- [ ] **KH 4 services endpoint-level test Wave 107** — admin/branding/platform/frontend
- [ ] **KC 190 endpoint coverage Wave 107+** — statistical sampling per `quality-audit` skill
- [ ] **RLS DB-layer explicit cross-tenant test Wave 106+** — currently gateway-level only

### Related

- Wave 104.5 close-loop: PRs #1715 + #1716 + #1717
- GAP-716 (obligations Wave 104.5 audit): `documents/04-quality/gaps/phase-1-beta/closed/GAP-716-wave-104.5-post-merge-audit-suite-deadline.md`
- Audit reports (3 outside-in, this PR):
  - `documents/04-quality/audits/persona-review/2026-05-22-wave-105-persona-simulation.md`
  - `documents/04-quality/audits/persona-review/2026-05-22-wave-105-vn-saas-benchmark.md`
  - `documents/04-quality/audits/persona-review/2026-05-22-wave-105-failure-mode-matrix.md`
- Rules applied:
  - `outside-in-coverage-trigger.md` v1.1.0 §3 (3-agent audit BEFORE lock scope)
  - `release-fix-retry-budget.md` (Bucket E P0 cluster gate)
  - `pre-handoff-self-test-completeness.md` §2 (per-bucket admin/security walk)
  - `gap-done-discipline.md` §3 (PARTIAL exit ramp for deferred items)
  - `post-merge-sync-completeness.md` §2 (4 sync targets post-merge)
- Draft scope source: `/tmp/wave-105-draft-scope.md` (in-flight, not committed)

## 8. Log

- **2026-05-22 (amend):** Plan amended to add **Bucket E0 META local-fix prod-parity (SHIP FIRST per `meta-gap-priority.md` §3)**. Triggered by user-flagged meta gap same session after initial plan lock PR #1718: "fix local có check fix env production chưa?" Audit existing rules → no rule covers code-fix → prod-env-sweep direction (sister rule precedent `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync covers inverse direction only). Filed GAP-717 (concrete bug — JWT_CHALLENGE_SECRET local-only in Wave 104.5 PR #1715 commit `b45f9b28`, missing AWS Secrets Manager + Terraform IAM + deploy script env injection) + GAP-718 (META rule `local-fix-production-parity-check.md` v1.0.0 proposal). Wave 105 scope expanded 6 → 7 buckets; E0 first per meta priority. Effort estimate increased ~46h → ~50h. Status: draft pending amend PR merge.

- **2026-05-22 (draft):** Wave 105 plan created. Triggered by Wave 104.5 close-loop user direction "test full để beta user không gặp lỗi" → 3 outside-in audit agents spawned per `outside-in-coverage-trigger.md` v1.1.0 §3 before lock scope. 3 audit reports filed (`documents/04-quality/audits/persona-review/2026-05-22-wave-105-{persona-simulation,vn-saas-benchmark,failure-mode-matrix}.md`). Refined scope from original 5-bucket inside-out persona walk to 6-bucket (added Bucket E Security P0 cluster) after audit surfaced 5 P0 real-code bugs + 6 outside-in UX gaps. Effort estimate increased ~20h → ~32h agent work (~46h total including coordination + post-wave audit). Inside-out + outside-in sources documented §1 Brainstorm Q1 per `outside-in-coverage-trigger.md` v1.1.0 Bước 5 mandate. Status: draft pending plan PR merge → agent spawn next session.
