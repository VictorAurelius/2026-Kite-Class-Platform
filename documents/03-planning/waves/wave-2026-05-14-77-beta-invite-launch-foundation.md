---
title: Wave 77 — Beta Invite Launch Foundation (SEND)
status: draft
created: 2026-05-14
updated: 2026-05-14
waves: [77]
gaps: [GAP-370, GAP-502, GAP-525, GAP-530, GAP-533, GAP-534, GAP-535, GAP-536]
---

# Wave 77 — Beta Invite Launch Foundation (SEND)

**Goal:** Close 8 P0 items chặn "gửi invite an toàn" cho Phase 1 BETA — email infra deliverability (Resend/SES verify live), backend stability finalization, credential rotation execution, invite-token single-use enforcement, tenant-create safety (VN diacritics slug + idempotency).
**Trigger:** Wave 75/76 đóng meta-governance steady-state; ROADMAP §🚀 Next Action = đóng 8 P0 phase-1-beta PARTIAL còn lại trước khi mời beta tenants. User-flagged outside-in audit miss → outside-in (persona + benchmark + matrix) đã chạy parallel; **convergence findings = 8 P0 NEW items** trong đó 4 thuộc Sub-wave A SEND (GAP-533 deliverability, GAP-534 token single-use, GAP-535 slug normalize, GAP-536 idempotency); 4 còn lại UX/Trust → Sub-wave B (Wave 78).
**Estimated wall-clock:** ~3-4h agent parallel; longest-bucket ~70min (Bucket A email infra + DNS propagation wait).

---

## 1. Brainstorm

**Q1 (alignment):**
- **Inside-out (dev):** 4 P0 PARTIAL từ ROADMAP Phase 1 BETA gating beta invite ability — GAP-370 (email infra), GAP-502 (backend stability), GAP-525 (cred rotate), GAP-530 (email verify live).
- **Outside-in (Claude — 3 agents parallel 2026-05-14):**
  - **Persona walkthrough** (P1 Solo teacher Cô Hương, P2 Center owner Anh Tuấn): top miss = Resend deliverability warm-up (email rơi spam Gmail VN) → file GAP-533.
  - **External benchmark** (Linear/Notion/Vercel/Stripe/Cursor + ClassDojo): top miss (SEND-scope) = same deliverability warm-up + DKIM/DMARC/SPF baseline → reinforces GAP-533.
  - **Failure-mode matrix** (2 personas × 6 stages × 17 failure classes): top misses (SEND-scope) = F1 invite-token reuse share-link (GAP-534), F2 tenant slug smart-quotes+diacritics crash (GAP-535), F3 double-submit signup orphan tenants on slow 3G (GAP-536). All P0, none overlap với 8 P0 PARTIAL hiện tại.
- **Personas serve:** P1 + P2 (Tier 1 Phase 1 BETA). RETAIN-scope (UI kits GAP-428 + onboarding checklist + beta disclaimer + support channel + VN i18n) defer Wave 78.

**Q2 (trade-offs):**
- Reject "1 mega-wave A+B 17 P0" — quá tải coordinator, review kém, blast radius cao nếu rollback. Split 2 sub-waves để fault-isolate.
- Reject "fix GAP-370 chỉ riêng infra không deliverability" — GAP-370 PARTIAL 60% từ 2026-05-06, AWS SES denied (Wave 69 audit); email gửi được nhưng spam folder = invite bị bỏ qua. Cần ship deliverability cùng infra path (Resend pivot OR SES sandbox C1 + DKIM/DMARC/SPF + warm-up + spam-score test).
- Reject merge GAP-525 cred rotation vào Bucket A email — security concern độc lập, user-action pending, không cùng surface.
- Reject merge N4 + N5 + N6 thành 3 bucket riêng — cả 3 chạm domain "tenant signup security" (kitehub-subscription), bundle thành Bucket D để giữ 4 bucket disjoint.

**Q3 (risks):**
- **Bucket A risk:** AWS SES C1 path fail / Resend onboarding chậm / DNS propagation chậm → recovery = ship sandbox-mode invite cho 5 user đầu, escalate AWS/Resend support qua follow-up gap.
- **Bucket B risk:** GAP-502 còn 20% deploy-prod debt → GAP-506 (separate). Wave 77 chỉ close 20% còn lại trong scope của 502 (email service healthcheck), không bao gồm GAP-506.
- **Bucket C risk:** Cred rotation = user-action, agent không tự rotate AWS Secrets Manager / GitHub Variables / Cloudflare. Per `agent-action-bias.md` §3 exception "destructive shared-state requires confirmation" — Bucket C ship checklist + automation script, user execute. Closure = both shipped + user confirms rotation done.
- **Bucket D risk:** DB migration cho invite_tokens (single-use flag + used_at column) + idempotency_keys table → 2 V-migrations cùng release. Flyway checksum precaution per GAP-493 retro; preflight script GAP-499 covers.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-370 + GAP-533 + GAP-530 (live verify after A ships) | bg-agent | ~70min | ✅ DNS + email vendor config + verify scripts |
| B | GAP-502 (close 20% remaining; GAP-506 deferred) | bg-agent | ~45min | ✅ kitehub-email module + JVM tuning + DLQ config |
| C | GAP-525 (rotate 3 creds; agent ships automation, user executes) | bg-agent | ~30min agent + user exec | ✅ AWS Secrets + GitHub Vars + CF API surfaces |
| D | GAP-534 + GAP-535 + GAP-536 | bg-agent | ~60min | ✅ kitehub-subscription tenant/invite domain + 2 V-migrations |

**Cross-layer check** (per `contract-first-for-cross-layer.md` §2):
- Bucket A = config + scripts (no FE)
- Bucket B = BE module + infra config (no FE)
- Bucket C = config rotation (no code/no FE)
- Bucket D = BE service + DB migrations (no FE)

→ NOT cross-layer. No Bucket 0 Foundation needed.

**Stake tier** (per `wave-pack-planner/SKILL.md` §Step 4.6): **HIGH** (production email + security + tenant data) → model: **Opus 4.7 full**.

---

## 3. Scope

> **Gap referencing convention** (per `.claude/rules/gap-architecture-v2.md`): canonical IDs from `gap-status.csv`. New gaps GAP-533..536 created in this wave plan PR (stub-only — full content updated during execution).

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A — Email SEND foundation** | GAP-370 (close PARTIAL→DONE), GAP-533 (NEW), GAP-530 (live verify post-A) | 🔴 P0 | `infrastructure/terraform-aws/ses.tf`, `infrastructure/terraform-cloudflare/dns.tf`, `documents/05-guides/deploy/email-deliverability-runbook.md`, `scripts/verify-email-deliverability.sh`, `scripts/smoke-resend.sh` | parallel |
| 2 | **B — Backend stability close** | GAP-502 (close 20% remaining for kitehub-email scope; GAP-506 defer) | 🔴 P0 | `kitehub/kitehub-email/src/main/resources/application*.yml`, `kitehub/kitehub-email/src/main/java/**/HealthCheck*.java`, `infrastructure/helm/kitehub-email/values.yaml` | parallel |
| 3 | **C — Credential rotation execution** | GAP-525 (ship rotation automation + user-action checklist) | 🔴 P0 | `scripts/rotate-leaked-credentials.sh`, `documents/05-guides/operations/credential-rotation-2026-05-13.md`, `.env.production.template` | parallel |
| 4 | **D — Tenant signup security** | GAP-534 invite-token single-use, GAP-535 slug normalize, GAP-536 idempotency | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/**/invite/*`, `kitehub/kitehub-subscription/src/main/java/**/tenant/*`, `kitehub/kitehub-subscription/src/main/resources/db/migration/V*__invite_token_single_use.sql`, `kitehub/kitehub-subscription/src/main/resources/db/migration/V*__idempotency_keys.sql` | parallel |

### Bucket A — Email SEND foundation

- Files: `infrastructure/terraform-aws/ses.tf`, `infrastructure/terraform-cloudflare/dns.tf` (DKIM/DMARC/SPF records), `documents/05-guides/deploy/email-deliverability-runbook.md` (new), `scripts/verify-email-deliverability.sh` (mail-tester.com smoke), `scripts/smoke-resend.sh` if Resend pivot.
- Tests: smoke test send + spam-score ≥8/10 (mail-tester.com)
- Acceptance:
  - GAP-370 AC met: production email sends successfully cho 5 beta recipients (sandbox C1 OR Resend prod)
  - GAP-533 AC met (NEW): DKIM/DMARC/SPF active in Cloudflare DNS; spam-score ≥8/10; warm-up plan documented + executed (first 50 emails over 7 days)
  - GAP-530 AC met (live verify per `pre-handoff-self-test-completeness.md` §2.3 email flow): real email sent → click link → token valid → flow advance

### Bucket B — Backend stability close

- Files: `kitehub-email/application*.yml`, `HealthCheck*.java`, `helm/kitehub-email/values.yaml`
- Tests: `./mvnw -pl kitehub-email verify`; integration test for email service healthcheck endpoint
- Acceptance:
  - GAP-502 close-out: kitehub-email healthcheck active + reports OK; thrashing zero in last 24h post-deploy
  - GAP-506 deferral documented in GAP-502 closure Log per `gap-done-discipline.md` §3 PARTIAL exit-ramp

### Bucket C — Credential rotation execution

- Files: `scripts/rotate-leaked-credentials.sh` (new wrapper), `documents/05-guides/operations/credential-rotation-2026-05-13.md` (new from runbook), `.env.production.template` (placeholder updates)
- Tests: dry-run mode verifies AWS/GitHub/CF API reachability; idempotency
- Acceptance:
  - GAP-525 ships agent-side automation + user-action checklist
  - User confirms 3 creds rotated (logged in commit trailer); GAP-525 closes only after user confirmation (per `agent-action-bias.md` §3 row 5)

### Bucket D — Tenant signup security

- Files: `kitehub-subscription/src/main/java/**/invite/InviteTokenService.java` (single-use enforcement), `tenant/TenantService.java` (slug normalize + idempotency), `V{N}__invite_token_single_use.sql`, `V{N+1}__idempotency_keys.sql`
- Tests: integration test (a) reuse token → 409, (b) slug VN diacritics → normalized + stored, (c) double-submit POST /tenants with same idempotency-key → returns existing 200
- Acceptance:
  - GAP-534 AC: invite token rejects 2nd use with 409 + audit log entry; device-binding optional (defer Phase 2 if not trivial)
  - GAP-535 AC: tenant slug accepts "Trường Mầm Non "Hoa Mai"" → normalized to `truong-mam-non-hoa-mai` (smart quotes stripped, diacritics flattened, unique collision suffix `-1`/`-2`)
  - GAP-536 AC: POST /tenants with `Idempotency-Key` header → 2nd call returns same response (200) + only 1 tenant row created

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-370` | Gap | `grep -E "^GAP-370," documents/04-quality/gaps/gap-status.csv` | PARTIAL 60%, AWS SES denied Wave 69 audit | ✅ exists |
| `GAP-502` | Gap | `grep -E "^GAP-502," documents/04-quality/gaps/gap-status.csv` | PARTIAL 80%, Wave 70 RC1+RC2 fixed | ✅ exists |
| `GAP-525` | Gap | `grep -E "^GAP-525," documents/04-quality/gaps/gap-status.csv` | PARTIAL 50%, runbook shipped 2026-05-14 | ✅ exists |
| `GAP-530` | Gap | `grep -E "^GAP-530," documents/04-quality/gaps/gap-status.csv` | OPEN 0%, Wave 76 persona audit NEW-001 | ✅ exists |
| `GAP-533` | Gap (NEW) | `bash scripts/query-gaps.sh GAP-533` | not in CSV yet | 🆕 to-be-created (Bucket A) |
| `GAP-534` | Gap (NEW) | `bash scripts/query-gaps.sh GAP-534` | not in CSV yet | 🆕 to-be-created (Bucket D) |
| `GAP-535` | Gap (NEW) | `bash scripts/query-gaps.sh GAP-535` | not in CSV yet | 🆕 to-be-created (Bucket D) |
| `GAP-536` | Gap (NEW) | `bash scripts/query-gaps.sh GAP-536` | not in CSV yet | 🆕 to-be-created (Bucket D) |
| `GAP-506` | Gap (referenced — defer) | `grep -E "^GAP-506," documents/04-quality/gaps/gap-status.csv` | (verified existing per GAP-502 Log) | ✅ exists (out of scope this wave) |
| `kitehub-email module` | Maven module | `ls kitehub/kitehub-email/pom.xml` | exists | ✅ exists |
| `kitehub-subscription module` | Maven module | `ls kitehub/kitehub-subscription/pom.xml` | exists | ✅ exists |
| `InviteTokenService` | Java class | `grep -rln "class InviteTokenService" kitehub/kitehub-subscription/src/main/java/` | TBD by Bucket D agent (likely exists with multi-use semantics; will extend) | ⚠️ verify-at-spawn (Bucket D first action) |
| `TenantService` | Java class | `grep -rln "class TenantService" kitehub/kitehub-subscription/src/main/java/` | TBD by Bucket D agent | ⚠️ verify-at-spawn (Bucket D first action) |
| `infrastructure/terraform-aws/ses.tf` | Terraform | `ls infrastructure/terraform-aws/ses.tf` | TBD | ⚠️ verify-at-spawn (Bucket A — may need create) |
| `infrastructure/terraform-cloudflare/dns.tf` | Terraform | `ls infrastructure/terraform-cloudflare/dns.tf 2>/dev/null` | TBD | ⚠️ verify-at-spawn (Bucket A — likely exists from earlier waves) |

Banned shortcuts: per `audit-to-gap-pipeline.md` §2.6 — no `| head` truncation; verify-at-spawn rows MUST be cleared as first action by each bucket agent (per `pre-mutation-state-check.md` §1.5 for Bucket D Flyway migrations + Bucket A Terraform).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash scripts/verify-email-deliverability.sh` (post-DNS propagation); mail-tester.com spam score ≥8/10 | `terraform-plan` (DNS diff) + manual smoke |
| B | `cd kitehub && ./mvnw -pl kitehub-email clean verify -P strict-warnings` | `kitehub-ci` (kitehub-email module) |
| C | `bash scripts/rotate-leaked-credentials.sh --dry-run` (verify AWS/GH/CF reachability) | manual user-action; closure commit trailer |
| D | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -P strict-warnings` + integration tests | `kitehub-ci` (kitehub-subscription module) |

Per `admin-merge-discipline.md`: each bucket PR requires local verify clean on EXACT rebased HEAD before `gh pr merge` (no `--admin` post-rebase).

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 4 buckets spawned with `run_in_background: true`
- `isolation: worktree` for parallel safety
- RELATIVE paths in agent prompts (per `feedback_worktree_absolute_path_contamination.md`)
- Coordinator merges sequentially after all background completions:
  - Merge order: B (lowest risk) → C (user-action gated) → D (DB migration sequenced) → A (longest, DNS propagation)
  - OR by completion order if no migration conflicts surface

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates affected GAP file Log + status + AC checkboxes
- GAP-533/534/535/536 stub files created in THIS wave plan PR; full content (Problem / Root Cause / Proposed Fix / AC) populated by execution buckets
- ROADMAP §🚀 Next Action updated in closure PR
- Wave 77 plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement)
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged
- `## Release Plan Progress` section in closure PR body — per `feedback_wave_closure_release_progress_report.md` rules #1-6
- Sub-wave B (Wave 78) queued: GAP-508, GAP-514, GAP-515, GAP-518, GAP-428, + 4 outside-in P0 (N1 onboarding+sample, N2 disclaimer+status, N7 support channel, N8 VN i18n)

## 8. Log

- **2026-05-14** (draft): Plan created. Inside-out (4 P0 PARTIAL from ROADMAP) + outside-in (3 parallel agents: persona, benchmark, failure-matrix → 4 new gaps GAP-533/534/535/536 SEND-scope) merged via `outside-in-coverage-trigger.md` §3 5-step flow. Sub-wave A scope locked at 4 buckets / 8 P0 items (Sub-wave B Wave 78 queued for RETAIN scope). Coordinator: @nguyenvankiet.

---

## 9. Post-Wave Audit Mandate

Per `.claude/rules/post-wave-audit-mandate.md` §2.1 — Bucket A touches `infrastructure/`, Bucket B touches BE module, Bucket D touches Java + Flyway migrations. Required audits within 3 days post-merge:
- **Security /100** (deps + secret-rotation surface — Bucket C)
- **Ops Readiness /100** (infra changes — Bucket A + B)
- **API Contract /100** (Bucket D may add new endpoints OR header semantics)
- **Quality /100** refresh (post-wave mandatory checkpoint)

No domain-milestone deferral — buckets touch multiple domains.
