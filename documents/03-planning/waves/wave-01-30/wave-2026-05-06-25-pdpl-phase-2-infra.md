---
title: Wave 25 — PDPL Phase 2 + Critical Infra (consent API + structured logging + rollback runbook)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [25]
gaps: [GAP-353b, GAP-114, GAP-116, GAP-378]
---

# Wave 25 — PDPL Phase 2 + Critical Infra

**Goal:** Ship 3 disjoint Phase 1 BETA-readiness deliverables: server-side consent API (PDPL Phase 2), structured JSON logging stack with PII scrubbing (ops baseline), and detailed rollback runbook (recovery confidence).

**Trigger:** Wave 24 closure recommended Wave 25 BE wave-pack per Release Lần 1 Plan §9. State-check on the 6 ROADMAP-recommended gaps (2026-05-06): GAP-117 already PARTIAL (Phase 1+2 shipped PR #632, Phase 3 → GAP-257 infra-blocked) and GAP-204 already PARTIAL (only 6 medium Dependabot auto-PRs pending) → eliminated. GAP-115 needs deployed monitoring stack (depends GAP-111) → deferred. Picked GAP-378 (rollback runbook, P1 STRONGLY recommend per Release Lần 1 deploy plan §5) as Bucket C — disjoint from BE Java work, force-multiplier for incident MTTR.

**Estimated wall-clock:** ~45-60 phút parallel (longest bucket A ~16h raw → background agent ~25-30 phút). Buckets B + C smaller (~8-12h raw each).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- **All Phase 1 BETA personas** (P1 Owner / P2 Trial / P3 Paid) — consent banner shipped Wave 23 BC needs server-side persistence so consent records survive cross-device + flow into DR-03 36-month retention pipeline.
- **Solo-dev coordinator** (incident response) — rollback runbook closes the §5 §6 §7 dependency on `release-1-deploy-plan.md`.
- **Dev + SRE persona** (debuggability) — structured JSON logging unlocks per-tenant `tenantId` filter + cross-service `traceId` correlation, prerequisite for Wave 26+ Loki/Grafana integration.
- **4-layer V-model coverage** (per `design-layer-coverage.md` §2):
  - 要件定義: BR-PDPL-CONSENT-003 (consent retention 36mo) + DR-03 (data retention) already shipped Wave 23 + earlier; Bucket A wires the implementation.
  - 基本設計: 3 REST endpoints `POST /api/v1/consent/record` + `GET /api/v1/consent/{visitorId}` + `POST /api/v1/consent/{visitorId}/revoke` (Bucket A) + JSON log schema + MDC propagation (Bucket B) + rollback decision flow + per-component specifics (Bucket C).
  - 詳細設計: idempotent upsert by visitor_id + audit-log hash-chain reuse (Bucket A) + LogstashEncoder + MDC filter ordering (Bucket B) + rollback trigger thresholds + step ordering (Bucket C).
  - コンポーネント設計: NEW `ConsentController` + `ConsentRecord` entity + `ConsentService` + `ConsentRetentionCron` (Bucket A) + `PIIScrubber` converter + `@Redact` annotation + `TenantContextFilter` + `RabbitMQTenantInterceptor` (Bucket B) + rollback-runbook.md sections (Bucket C).

**Q2 (trade-offs):**
- **Reject:** include GAP-115 (Loki/Promtail Helm) — needs deployed monitoring infra (GAP-111) + real K8s cluster; would ship as PARTIAL with empty AC checks. Defer to post-deploy wave.
- **Reject:** include GAP-117/204 — already PARTIAL per state-check; re-touching wastes scope.
- **Reject:** combine GAP-114 + GAP-116 into separate buckets — both touch `logback-spring.xml` registration → merge conflict. Combine into single Bucket B "logging stack" (covers both gaps' scope; PARTIAL exit-ramp possible if PIIScrubber audit-of-existing-code defers).
- **Reject:** new `kitehub-consent` module for GAP-353b — adds module-creation overhead. Place consent table + service in existing `kitehub-subscription` (V25 next migration; module already has DB infra).
- **Accept:** ship 3 disjoint buckets totalling ~32-36h raw. Buckets B + C explicitly modest scope (~8-12h each); Bucket A largest (~16h) but file-disjoint from B + C.

**Q3 (risks):**
- **Risk: Bucket A consent-record schema collides with future GDPR/CCPA tables** — mitigation: schema uses `visitor_id` UUID v4 as pseudonymous key (PDPL-aligned); future expansion adds columns or sibling tables, not table-rename.
- **Risk: Bucket A FE `useConsent` hook extension breaks Wave 23 LocalStorage MVP** — mitigation: API call layered ON TOP of LocalStorage (LocalStorage stays primary, server-side authoritative for cross-device); no behavior change for offline-first path.
- **Risk: Bucket B logback-spring.xml config errors silently break logging across all services** — mitigation: ship per-service `logback-spring.xml` (not single shared file) so per-module override possible; verify via `mvn test` log capture in each service test suite.
- **Risk: Bucket B `LogstashEncoder` dependency conflicts with Spring Boot 3.5.14 BOM** — mitigation: use `logstash-logback-encoder` 8.x (Spring Boot 3.x compatible per release notes); add to parent `dependencyManagement` only, child modules opt-in.
- **Risk: Bucket B PIIScrubber regex false-positives mask legitimate log content (e.g., emails in audit logs)** — mitigation: scrubber applied at `<encoder><pattern>` level via `%pii(msg)` conversion word, NOT global on all fields; `@Redact` annotation explicit on DTO fields requiring redaction.
- **Risk: Bucket C rollback-runbook.md commands drift from actual deploy infra (Helm vs Docker-compose)** — mitigation: runbook documents both Helm (future K8s) + Docker-compose (current Oracle Cloud) paths; explicit "verify version before running" step at top.
- **Risk: parent `kitehub/pom.xml` modifications conflict between Bucket A (subscription deps) and Bucket B (logstash dep)** — mitigation: Bucket A adds deps to `kitehub-subscription/pom.xml` only (child); Bucket B adds to parent `dependencyManagement` only. No overlap.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort (raw) | Disjoint? |
|--------|--------|-------|--------------|-----------|
| A | GAP-353b | bg-agent | ~16h | ✅ kitehub-subscription module + packages/shared-ui FE hook |
| B | GAP-114 + GAP-116 | bg-agent | ~10-12h | ✅ all-services resources/logback-spring.xml + new shared logging classes in kitehub-platform |
| C | GAP-378 | bg-agent | ~8-10h | ✅ documents/05-guides/operations/runbooks/ docs only |

**Disjoint check:**
- A touches: `kitehub/kitehub-subscription/src/{main,test}/**`, `packages/shared-ui/src/components/ConsentBanner/**`, new Flyway `V25__create_consent_record.sql`, `documents/01-business/kitehub/marketing/{rules,api-contract}.md` cross-link.
- B touches: every Java service's `src/main/resources/logback-spring.xml` (kitehub-platform, kitehub-subscription, kitehub-branding, kitehub-email, kitehub-admin, kitehub-gateway, kiteclass-core, kiteclass-gateway), parent `kitehub/pom.xml` `dependencyManagement` block ONLY, new shared logging package `kitehub-platform/src/main/java/com/kitehub/shared/logging/`, new `documents/05-guides/operations/logging-standard.md`.
- C touches: new `documents/05-guides/operations/runbooks/rollback-runbook.md` only.

**Pom.xml conflict guard:** Bucket A edits `kitehub-subscription/pom.xml` (child); Bucket B edits `kitehub/pom.xml` parent `<dependencyManagement>` only. No file overlap.

---

## 3. Scope (per bucket)

### Bucket A — GAP-353b server consent API + audit-log link

- **Files (Java):**
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/entity/ConsentRecord.java` 🆕
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/repository/ConsentRecordRepository.java` 🆕
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/service/ConsentService.java` 🆕
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/service/ConsentServiceImpl.java` 🆕
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/controller/ConsentController.java` 🆕
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/dto/ConsentRequest.java` + `ConsentResponse.java` 🆕
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/cron/ConsentRetentionCron.java` 🆕 (DR-03 36mo cleanup, daily 3am)
- **Migration:** `kitehub/kitehub-subscription/src/main/resources/db/migration/V25__create_consent_record.sql` 🆕
- **Files (FE TS):**
  - `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` extended (visitor_id generation + API sync)
  - `packages/shared-ui/src/components/ConsentBanner/storage.ts` extended (visitor_id LocalStorage key)
  - `packages/shared-ui/src/components/ConsentBanner/api.ts` 🆕 (fetch wrapper)
- **Docs:**
  - `documents/01-business/kitehub/marketing/api-contract.md` 3 endpoints documented
  - `documents/01-business/kitehub/marketing/rules.md` BR-PDPL-CONSENT-003 implementation footer cross-link
- **Tests:**
  - Unit: `ConsentServiceImplTest`, `ConsentControllerTest`
  - IT: `ConsentControllerIT` (TestContainers Postgres, V25 applied)
  - FE: `useConsent.test.ts` extended (API mock + multi-device flow)
- **Acceptance (subset of GAP-353b AC):** all 11 AC items checked; `gap-done-discipline.md` §2 compliant; PARTIAL exit-ramp if multi-device sync test deferred (file follow-up).

### Bucket B — GAP-114 + GAP-116 logging stack

- **Files (parent pom):**
  - `kitehub/pom.xml` add `logstash-logback-encoder` 8.x to `<dependencyManagement>` (no child uses yet — opt-in per service)
  - `kiteclass/pom.xml` same dependency-management add (kiteclass parent — verify exists)
- **Files (per-service logback):**
  - `kitehub/kitehub-platform/src/main/resources/logback-spring.xml` 🆕
  - `kitehub/kitehub-subscription/src/main/resources/logback-spring.xml` 🆕
  - `kitehub/kitehub-branding/src/main/resources/logback-spring.xml` 🆕
  - `kitehub/kitehub-email/src/main/resources/logback-spring.xml` 🆕
  - `kitehub/kitehub-admin/src/main/resources/logback-spring.xml` 🆕
  - `kitehub/kitehub-gateway/src/main/resources/logback-spring.xml` 🆕
  - `kiteclass/kiteclass-core/src/main/resources/logback-spring.xml` 🆕
  - `kiteclass/kiteclass-gateway/src/main/resources/logback-spring.xml` 🆕 (verify path)
- **Files (shared logging classes):**
  - `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/PIIScrubber.java` 🆕 (logback `ReplacingCompositeConverter`)
  - `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/Redact.java` 🆕 (`@Redact` annotation)
  - `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/TenantContextFilter.java` 🆕 (HTTP MDC populator)
  - `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/RabbitMQTenantInterceptor.java` 🆕 (AMQP MDC propagator)
- **Docs:**
  - `documents/05-guides/operations/logging-standard.md` 🆕 (required fields + log levels + PII rules + query examples)
- **Tests:**
  - Unit: `PIIScrubberTest` (email/phone/Vietnamese ID regex coverage)
  - Unit: `TenantContextFilterTest` (MDC populated from JWT subject + tenantId claim)
  - Verify: `mvn test` per service confirms JSON output via test appender capture
- **Acceptance (subset of GAP-114 + GAP-116 AC):** all services emit JSON logs with required MDC fields; PIIScrubber unit-tested for email/phone redaction; logging-standard.md cross-linked from `output-review-mandate.md` §3 row "Logs format"; PARTIAL exit-ramp if existing-code PII audit (GAP-116 AC #4 "Existing code audit → fix 100% PII log leaks") defers to follow-up gap.

### Bucket C — GAP-378 rollback runbook

- **Files:**
  - `documents/05-guides/operations/runbooks/rollback-runbook.md` 🆕 (~7-step procedure + per-component specifics + comm templates)
- **Cross-link updates:**
  - `documents/03-planning/roadmap/release-1-deploy-plan.md` §5 link to runbook
- **Acceptance (full GAP-378 AC):** all 8 AC items checked; documents both Helm (K8s future) + Docker-compose (Oracle Cloud current) paths; communication templates included for status page + email; smoke test integration (cross-link GAP-377 even though not yet shipped — forward-flag).

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kitehub-subscription` module | Maven module | `ls kitehub/kitehub-subscription/pom.xml` | 1 file | ✅ exists |
| `kitehub-platform` module | Maven module | `ls kitehub/kitehub-platform/` (declared in parent `<modules>`) | declared in `kitehub/pom.xml:42` | ✅ exists |
| `V24__add_instance_vertical_type.sql` | Latest existing migration | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V24*` | 1 file | ✅ exists (V25 next) |
| `V25__create_consent_record.sql` | Migration | `ls .../db/migration/V25*` | 0 matches | 🆕 to-be-created (Bucket A) |
| `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` | TS hook | `ls packages/shared-ui/src/components/ConsentBanner/useConsent.ts` | 1 file | ✅ exists (Wave 23 BC) |
| `packages/shared-ui/src/components/ConsentBanner/storage.ts` | TS module | `ls .../ConsentBanner/storage.ts` | 1 file | ✅ exists (Wave 23 BC) |
| `BR-PDPL-CONSENT-003` | Business rule | `grep -rn "BR-PDPL-CONSENT-00[1-4]" documents/01-business` | 3 files (kitehub/marketing + kiteclass/marketing + README) | ✅ exists (Wave 23 A) |
| `DR-03` | Data retention rule | `grep -rn "DR-03" documents/01-business` | 1 file (kitehub/marketing/rules.md) | ✅ exists |
| `documents/01-business/kitehub/marketing/api-contract.md` | API contract doc | file present | 2.9K | ✅ exists (extend in Bucket A) |
| `ConsentController` Java class | BE controller | `grep -rn "class ConsentController" kitehub/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `ConsentRecord` entity | JPA entity | `grep -rn "class ConsentRecord" kitehub/` | 0 matches | 🆕 to-be-created (Bucket A) |
| `ConsentRequest.java` | DTO file | `find kitehub/ -name "ConsentRequest.java"` | 0 matches | 🆕 to-be-created (Bucket A) |
| `ConsentResponse.java` | DTO file | `find kitehub/ -name "ConsentResponse.java"` | 0 matches | 🆕 to-be-created (Bucket A) |
| `ChildProtectionAuditServiceImpl` | Hash-chain pattern source | `grep -rn "ChildProtectionAuditService" kiteclass/` | 12 files | ✅ exists (reuse pattern in Bucket A) |
| `logback-spring.xml` (any module) | Existing config | `find . -name "logback-spring.xml"` (no head) | 0 matches | 🆕 to-be-created (Bucket B, 8 files) |
| `logstash-logback-encoder` dependency | Maven dep | `grep -rn "logstash-logback-encoder\|net.logstash" kitehub/ kiteclass/ --include="pom.xml"` | 0 matches | 🆕 to-be-created (Bucket B, parent pom) |
| `LogstashEncoder` reference | Java class import | `grep -rn "net\.logstash" --include="*.java" --include="*.xml" .` | 0 matches in production code (only in `.claude/rules/logs-format-standard.md` + GAP file + planning doc) | 🆕 to-be-created (Bucket B) |
| `PIIScrubber` Java class | Logback converter | `grep -rn "class PIIScrubber" kitehub/ kiteclass/` | 0 matches | 🆕 to-be-created (Bucket B) |
| `@Redact` annotation | Java annotation | `grep -rn "@Redact\|public.*Redact" kitehub/ kiteclass/ --include="*.java"` | 0 matches | 🆕 to-be-created (Bucket B) |
| `TenantContextFilter` | HTTP filter | `grep -rn "class TenantContextFilter" kitehub/ kiteclass/` | 0 matches | 🆕 to-be-created (Bucket B) |
| `documents/05-guides/operations/` folder | Guides folder | `ls documents/05-guides/operations/` | 4+ files (DR plan, runbook, matrix, README) | ✅ exists |
| `documents/05-guides/operations/runbooks/` folder | Runbooks subfolder | `ls documents/05-guides/operations/runbooks/` | exists | ✅ exists |
| `documents/05-guides/operations/runbooks/rollback-runbook.md` | Runbook | `ls .../runbooks/rollback-runbook.md` | 0 matches | 🆕 to-be-created (Bucket C) |
| `documents/03-planning/roadmap/release-1-deploy-plan.md` | Cross-link target | `ls documents/03-planning/roadmap/release-1-deploy-plan.md` | exists | ✅ exists (Bucket C cross-link) |
| `documents/05-guides/operations/logging-standard.md` | Standard doc | `ls .../operations/logging-standard.md` | 0 matches | 🆕 to-be-created (Bucket B) |

**Banned shortcuts honored:** no `| head` truncation; full grep/find output read; alternative class-name searches performed (e.g., `LogstashEncoder` checked across `*.java` AND `*.xml`); cross-folder searches for symbol absence (PIIScrubber checked in both `kitehub/` and `kiteclass/`).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify` + `pnpm -F @kite/shared-ui test:unit` | kitehub-ci + frontend-ci |
| B | `cd kitehub && ./mvnw clean verify -DskipITs` + `cd kiteclass && ./mvnw -pl kiteclass-core clean verify` (per-service test confirms JSON appender output) | kitehub-ci + core-ci |
| C | `bash scripts/check-docs.sh` (markdown lint + frontmatter check) + manual cross-link verify | doc-quality CI |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` v1.0.0:
- All 3 buckets spawn with `run_in_background: true` (default per project rule)
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions: A first (largest, sets schema baseline), B second (logging stack), C last (docs)
- Agent briefing includes both BE + FE local verify commands per `feedback_agent_local_verify_both_layers.md` (Bucket A)
- Wave plan PR merges to main BEFORE agent spawn per `feedback_wave_plan_through_pr.md`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md`:
- Each bucket PR updates affected GAP file Log + status (PARTIAL exit-ramp if any AC defers per §3 Bucket scope notes)
- ROADMAP §🚀 Next Action updated in closure PR with Wave 26 candidate (likely PDPL Phase 2 remainder GAP-353c DSAR + GAP-353d DPIA, OR Track 2 FE wave-pack)
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement per `feedback_wave_history_append_required.md`)
- Sub-gaps filed for any deferral (anticipated: GAP-114 existing-code PII audit follow-up if Bucket B B-side defers; GAP-353b multi-device sync verification if cross-browser test infra unavailable)
- Counts update in ROADMAP §🚀 Next Action

---

## 8. Log

- **2026-05-06** (complete): Wave 25 SHIPPED — 3 buckets + plan + closure (5 PRs). PR #835 plan, PR #836 Bucket C GAP-378 🟢 DONE (rollback runbook 11 sections), PR #838 Bucket A GAP-353b 🟡 PARTIAL (consent API + V25 + useConsent hook; 8/11 AC; +follow-up GAP-353b-followup-multi-device-and-audit-chain), PR #837 Bucket B GAP-114 + GAP-116 🟡 PARTIAL (logging stack 8 logback configs + PIIScrubber + MDC; 5/6 + 5/6 AC; +follow-up GAP-116-followup-existing-code-pii-audit). Coordinator-applied 2 fixes to Bucket B before merge: (1) pinned logstash-logback-encoder version=8.0 inline in kitehub-platform/pom.xml (installed pom needs self-sufficient version, parent dependencyManagement doesn't transit through consumer modules at out-of-context resolution); (2) added @ConditionalOnClass(SecurityContextHolder) to LoggingAutoConfiguration ServletConfig so services without spring-security-core skip filter wiring (subscription has only spring-security-crypto); (3) added logstash dep to kiteclass-gateway/pom.xml (standalone module, no platform transitive). 1 merge conflict (B vs main ROADMAP §🚀 Next Action additive — coordinator resolved). 60th→61st 0-clarification streak. **Counts: 167 → 167 OPEN** (-GAP-378 closed; +GAP-353b-followup +GAP-116-followup; GAP-353b/114/116 PARTIAL pool unchanged). Total wall-clock ~120 min (plan 5min + 3 parallel agents longest 16min A + 12min B + 6min C + B CI fix iterations 2× ~25min + closure 15min). Token cost ~1.5M for 3 wave agents.
- **2026-05-06** (draft): Plan created. State-check completed per `audit-to-gap-pipeline.md` §2.6 — eliminated GAP-117/204/115 from ROADMAP recommendation; substituted GAP-378 as Bucket C. 3 buckets file-disjoint verified. Ready for plan PR per `feedback_wave_plan_through_pr.md`.
