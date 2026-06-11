# AI Branding — Business Rules

**Last verified:** 2026-04-26 (v2 sync — GAP-229 Phase 1)
**Config prefix:** `ai.rate-limit`, `ai.provider`, `ai.queue`, `branding.routing`, `quality-gate` (see also [`kiteclass/ai-agent-workflow/rules.md`](../../kiteclass/ai-agent-workflow/rules.md) §Fair-queue scheduler for BR-QUEUE-001..018)
**v2 implementation:** Waves 2-4 shipped under `kiteclass/kiteclass-core/` (NOT `kitehub-branding/`); architecture per ADR-004 (state machine), ADR-005 (resource classification), ADR-006 (saga), ADR-009 (composite package + ETag)

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| AIB-01 | FREE/TRIAL daily limit | 3 requests/ngày | `ai.rate-limit.free-per-day` |
| AIB-02 | BASIC daily limit | 10 requests/ngày | `ai.rate-limit.basic-per-day` |
| AIB-03 | PREMIUM daily limit | 50 requests/ngày | `ai.rate-limit.premium-per-day` |
| AIB-04 | ENTERPRISE daily limit | Unlimited (-1) | `ai.rate-limit.enterprise-per-day` |
| AIB-05 | Rate limit reset | Daily (per calendar day) | AIUsageLog.usageDate = LocalDate |
| AIB-06 | Usage tracking | AIUsageLog (instanceId, usageDate, requestCount) | ai_usage_log table |
| AIB-07 | Usage increment | Upsert: increment nếu exists, create nếu new | recordUsage() |
| AIB-08 | Unlimited marker | -1 = không giới hạn | isRateLimited() check |
| AIB-09 | AI provider | Configurable: ollama hoặc openai | `ai.provider` |
| AIB-10 | Template gallery | Pre-built templates, không cần AI | TemplateGalleryService |
| AIB-11 | Template categories | education, business, general | category filter |
| AIB-12 | Template apply | Trả về themeConfig JSON | applyTemplate() |
| AIB-13 | Template active filter | Chỉ hiện active=true | findByActiveTrueOrderByNameAsc() |
| AIB-14 | Fair-queue dispatch | AI jobs tier-aware enqueue (Wave 3 Phase 1, GAP-005a) | xem BR-QUEUE-001..018 trong [`kiteclass/ai-agent-workflow/rules.md`](../../kiteclass/ai-agent-workflow/rules.md) — `AIQueueDispatcher` / `AIJobConsumer` / `AIQueueConfig` |

## Config

```yaml
ai:
  provider: ${AI_PROVIDER:openai}
  rate-limit:
    free-per-day: 3
    basic-per-day: 10
    premium-per-day: 50
    enterprise-per-day: -1

  ollama:
    base-url: ${OLLAMA_BASE_URL:http://kite-ollama:11434}
    text-model: ${OLLAMA_TEXT_MODEL:llama3.1:8b}
    vision-model: ${OLLAMA_VISION_MODEL:llava:13b}
    timeout-seconds: ${OLLAMA_TIMEOUT:120}

openai:
  api:
    key: ${OPENAI_API_KEY:sk-mock-key-for-local-testing}
    base-url: https://api.openai.com/v1
  models:
    vision: gpt-4-vision-preview
    dalle: dall-e-3
    text: gpt-4-turbo
  rate-limit:
    requests-per-minute: 10
  timeout:
    seconds: 60
```

---

## v2 Rules (Waves 2-4 implementation)

### Resource Classification (BR-RES) — ADR-005, GAP-007

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-RES-001 | Three resource categories | `STATIC` (uploaded/default, no compute), `TEMPLATE` (SVG + brand params, ~0 cost), `FULL_AI` (heavy, async, expensive) | `module/branding/entity/ResourceCategory` |
| BR-RES-002 | Classification chain (Chain of Responsibility) | Ordered classifiers walked until one returns a category; `DefaultTemplateClassifier` (order=100) is terminal | `module/branding/classifier/ResourceClassifier` + 7 implementations |
| BR-RES-003 | Handler dispatch (Strategy) | One `ResourceHandler` per category; `FALLBACK` status triggers `FallbackHandler.rescue` | `module/branding/handler/` |
| BR-RES-004 | Template-first routing flag | Default `true`; disabling reserved for debug/load-test only — startup logs WARN if disabled in non-dev | `branding.routing.template-first` |
| BR-RES-005 | Max AI ratio alert | ≥80% of requests should resolve to STATIC/TEMPLATE; alert threshold for `branding.routing.ai_ratio` Micrometer gauge | `branding.routing.max-ai-ratio` (default 0.20) |
| BR-RES-006 | Routing metric | Counter `branding.routing.classified` tagged by category — Prometheus alert when AI share exceeds BR-RES-005 | `ResourceRoutingService.recordClassification()` |

### Lifecycle State Machine (BR-LIFE) — ADR-004, GAP-009

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-LIFE-001 | Six-state lifecycle | `NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING; * → FAILED → INITIALIZING (retry)` | `module/instance/entity/FrontendInstanceStatus` |
| BR-LIFE-002 | Allowed transitions per state | Each enum value declares `Set<FrontendInstanceStatus> allowedTransitions()`; `canTransitionTo` enforces; invalid → `IllegalStateException` | `FrontendInstanceStatus.canTransitionTo` |
| BR-LIFE-003 | Single authority | `InstanceLifecycleService` is the only writer; controllers MUST delegate (semantic methods: `initiate`, `markInfrastructureReady`, `markBrandingCompleted`, `rebrand`, `markFailed`, `retry`) | `module/instance/service/InstanceLifecycleService` |
| BR-LIFE-004 | Outbox event per transition | Every state change writes outbox row in same JPA txn (`OutboxEventWriter`); event types: `instance.{initializing,generating,deployed,regenerating,failed}` | per ADR-007, design-patterns.md §3.5 |
| BR-LIFE-005 | Max retries | 3 consecutive failures before retry path is blocked; tracked in `FrontendInstance.retryCount` | `InstanceLifecycleService.MAX_RETRIES = 3` |
| BR-LIFE-006 | Branding version monotonic | Increments on every successful `markBrandingCompleted` (DEPLOYED transition) — drives ETag and FE cache invalidation | `FrontendInstance.brandingVersion` |

### Quality Gate (BR-QUALITY) — ai-branding-guidelines.md §5, GAP-012

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-QUALITY-001 | Pass threshold | Score ≥ 70/100 required before DEPLOY; below → `markFailed` + reason `"score X < threshold 70"` | `quality-gate.pass-threshold:70` |
| BR-QUALITY-002 | Five quality checks | `wcag-contrast`, `css-vars-applied`, `asset-urls-reachable`, `visual-regression`, `logo-placement` — averaged into single score | `module/quality/check/` (5 implementations) |
| BR-QUALITY-003 | Audit log on review | Every review writes `AuditLog` event (`quality.review.passed` or `quality.review.failed`) with score + reason | `InstanceQualityReviewer.review()` |
| BR-QUALITY-004 | Manual scaffold | Real WCAG/visual-regression/ML scoring deferred (GAP-226/227/228 Wave 8+); current implementation = scaffold returning fixed scores per check | per ai-branding-guidelines.md §11.4 |

### Rebrand Approval (BR-APRV) — GAP-070, Wave 3 Sub-PR 3.5

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-APRV-001 | Approval state machine | `PENDING → APPROVED \| REJECTED \| EXPIRED` (terminal states) | `module/instance/approval/ApprovalStatus` |
| BR-APRV-002 | Two-person rule | Approver MUST differ from initiator; same user → `ConcurrentRebrandException` | `RebrandApprovalService.approve()` |
| BR-APRV-003 | TTL (auto-expire) | Default 24h from `requestedAt`; scheduler expires PENDING approvals | `RebrandApprovalService.DEFAULT_TTL = Duration.ofHours(24)` |
| BR-APRV-004 | Tier gating | Caller (controller) decides whether to require approval — Enterprise: required; lower tiers: skip and call `lifecycle.rebrand` directly | per BR-APRV javadoc note |
| BR-APRV-005 | Optimistic locking | Caller supplies `expectedVersion`; mismatch → 409 (form was opened before concurrent mutation) | `RebrandApprovalService.request(expectedVersion)` |
| BR-APRV-006 | Single in-flight | Only one PENDING approval per instance; second request → `ConcurrentRebrandException` | `RebrandApprovalRepository.findFirstByTargetInstanceIdAndStatus...` |

### Wizard Provisioning (BR-WIZARD) — Saga, ADR-006

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-WIZARD-001 | Wizard input contract | `TenantCreatedEvent { tenantId, slug, audience, tone }` — wizard UI must populate audience + tone before saga starts | `module/provisioning/TenantCreatedEvent` |
| BR-WIZARD-002 | Audience values | `"K-12"`, `"center"`, `"university"` (free-text but UI constrains to dropdown) | `AnalysisRequest.audience` javadoc |
| BR-WIZARD-003 | Tone values | `"friendly"`, `"professional"`, `"energetic"` | `AnalysisRequest.tone` javadoc |
| BR-WIZARD-004 | Saga compensation | Any Step failure → `lifecycle.markFailed(reason)` runs as compensation; failed-stage transitions are persisted (no rollback of INITIALIZING/GENERATING) | `TenantProvisioningSaga.compensate()` |
| BR-WIZARD-005 | Pipeline order | `Analyzer → Planner → PlanExecutor`; PlanExecutor's last Step transitions DEPLOYED via `markBrandingCompleted` | `TenantProvisioningSaga.runBrandingPlan()` |
| BR-WIZARD-006 | Free-form prompt BAN | Per ai-branding-guidelines.md §2.1, free-form text prompts are BANNED for FREE/BASIC/PREMIUM tiers; ENTERPRISE may opt-in via `ai.enterprise.advancedModeEnabled` flag (not yet wired) | per ai-branding-guidelines.md §2.4 |

### Content Moderation (BR-MOD) — GAP-018, Wave 4

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-MOD-001 | Three-stage pipeline | Pre-prompt screen → output check → admin review queue if uncertain | `module/moderation/ContentModerationService` |
| BR-MOD-002 | Status enum | `APPROVED`, `REJECTED`, `NEEDS_REVIEW`, `ESCALATED` | `module/moderation/ModerationStatus` |
| BR-MOD-003 | Manual queue | Failures route to `ModerationQueue` for admin review | `module/moderation/ModerationQueue` |

### Composite Package (BR-PKG) — ADR-009

| ID | Rule | Value | Code reference |
|----|------|-------|----------------|
| BR-PKG-001 | Single composite endpoint | `GET /api/v1/branding/{instanceId}/package` — returns theme + assets + metadata in one round-trip | `BrandingPackageController` |
| BR-PKG-002 | ETag scheme | `W/"v{brandingVersion}-{hashHex}"` — FE sends `If-None-Match`, server returns 304 on match | `BrandingPackageController.buildEtag()` |
| BR-PKG-003 | Server-side cache | `CachingBrandingPackageProxy` (Spring Cache + Redis); evict on `instance.deployed` / `instance.regenerating` outbox events | `module/branding/service/CachingBrandingPackageProxy` |
| BR-PKG-004 | Cache evict webhook | Internal `POST /internal/notify/instance-deployed` (filtered by gateway to internal network); manual ops invalidation path | `InternalWebhookController` |
| BR-PKG-005 | Public endpoint | `GET /api/v1/branding/public?tenantId={uuid\|slug}` — minimal payload (logo + name + 3 colors + tagline) for unauthenticated login/register/reset pages; never leaks admin config | `PublicBrandingController` |

## v2 Config (additions)

```yaml
branding:
  routing:
    template-first: true        # BR-RES-004 — never disable in prod
    max-ai-ratio: 0.20          # BR-RES-005 — Prometheus alert threshold

quality-gate:
  pass-threshold: ${QUALITY_GATE_PASS_THRESHOLD:70}   # BR-QUALITY-001 (GAP-386 — externalized 2026-05-08)
```

---

## BR-QUALITY-001 — Quality Gate Pass Threshold (5-attribute compliance block)

Per [`.claude/rules/business-logic-review.md`](../../../.claude/rules/business-logic-review.md) v1.0.0 §2 — every business rule MUST document Source, Rationale, Reviewer, Compliance check, Review cadence.

- **Value:** `70/100` (config key: `quality-gate.pass-threshold`; env var `QUALITY_GATE_PASS_THRESHOLD`; Helm path `branding.qualityGate.passThreshold`)
- **Source:** `ai-branding-guidelines.md` §5 (Quality Gate spec) + GAP-386 audit finding 2026-05-07 (Wave 34 Business Logic /100 audit). Empirical observation — no internal A/B yet; baseline value chosen during AI Branding v2 design (Wave 2-4).
- **Rationale:** 70/100 = WCAG AA contrast floor (4.5:1 maps to ~70 sub-score in placeholder scoring per ai-branding-guidelines.md §5) + buffer for 4 other sub-checks. Below 70 = at least 1 sub-check failing materially. Above 80 = blocks borderline-acceptable instances during scaffold v0 (until GAP-226/227/228 land real measurement). 70 chosen because (a) caps obviously-bad output, (b) does NOT block placeholder v0 spread (60..100), (c) tunable per tier post-launch (FREE 65 = lenient, ENTERPRISE 80 = stricter) without recompile.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal counsel review N/A — quality threshold is product-tuning value, not regulated. Full Product/Designer review queued after GAP-226/227/228 land real WCAG measurement (estimated Wave 8+ post-Phase-1 launch).
- **Compliance check:** **N/A** — pure quality-tuning value. No PDPL trigger (no PII), no Consumer Protection trigger (no advertised SLA tied to threshold), no MoET education trigger (not student-facing). Re-evaluate if marketing copy ever advertises specific quality score guarantee to tenants.
- **Review cadence:** **Quarterly** (default per `business-logic-review.md` §2.5) + event-driven on: (a) GAP-226/227/228 landing (real measurement → re-baseline threshold), (b) tier-specific override request from Sales/PM, (c) ≥10% deploy-rejection rate observed in production. **Next review:** 2026-08-08 (Q3 2026) OR upon any of above triggers.

### Externalization (GAP-386 — landed 2026-05-08)

- Code: `kitehub/kitehub-branding/.../wizard/quality/QualityScoreAggregator.java` field `@Value("${quality-gate.pass-threshold:70}") private int threshold;` (was `private static final int THRESHOLD = 70;` pre-GAP-386)
- App config: `kitehub/kitehub-branding/src/main/resources/application.yml` → `quality-gate.pass-threshold: ${QUALITY_GATE_PASS_THRESHOLD:70}`
- Helm: `infrastructure/helm/kitehub/values.yaml` → `branding.qualityGate.passThreshold: 70`
- Deployment: `infrastructure/helm/kitehub/templates/deployment.yaml` → env `QUALITY_GATE_PASS_THRESHOLD`
- Tests: `kitehub-branding/src/test/.../QualityScoreAggregatorThresholdTest.java` covers (a) custom threshold 80 → score 75 = FAIL / score 85 = PASS; (b) default fallback 70 when no override.

---

## BR-LIFE-001..006 — Lifecycle State Machine (5-attribute compliance blocks)

Per [`.claude/rules/business-logic-review.md`](../../../../.claude/rules/business-logic-review.md) v1.0.0 §2 — every business rule MUST document Source, Rationale, Reviewer, Compliance check, Review cadence. The 6 BR-LIFE rules share a single state-machine concern; each is documented below with the 5 attributes. Closes Wave 34 Business Logic /100 audit Finding #2 (GAP-389-C, Wave 36 Bucket C).

### BR-LIFE-001 — Six-state lifecycle

- **Value:** `NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING; * → FAILED → INITIALIZING (retry)`
- **Source:** `ai-branding-guidelines.md` §6 + ADR-004 (Lifecycle State Machine). State graph derived from operational AI provisioning patterns (provisioning → generating → live → re-generating, with explicit failed branch).
- **Rationale:** 6 states is the minimal sufficient set — fewer collapses (e.g. merging INITIALIZING + GENERATING) loses the ability to distinguish infrastructure-prepared vs AI-output-ready, which different downstream consumers (cache warmer vs FE polling) need. More states (e.g. separate FAILED_QUOTA / FAILED_AI / FAILED_INFRA) would be over-engineering at v1 — failure reason is captured in the FAILED state's `reason` field.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-07). Pure domain logic, no legal/PDPL/MoET trigger; review when adding/removing states.
- **Compliance check:** **N/A** — internal state machine, no PII/financial/regulated data exposed.
- **Review cadence:** **Quarterly** + event-driven on state-add PR (any change to `FrontendInstanceStatus` enum). **Next review:** 2026-08-07.
- **Code reference:** `kitehub/kitehub-branding/src/main/java/.../module/instance/entity/FrontendInstanceStatus.java`

### BR-LIFE-002 — Allowed transitions per state

- **Value:** Each enum value declares `Set<FrontendInstanceStatus> allowedTransitions()`; `canTransitionTo` enforces; invalid → `IllegalStateException`.
- **Source:** `ai-branding-guidelines.md` §6 + design-patterns.md §3.3 (status switch cascade BANNED — must use State Pattern enforcement).
- **Rationale:** Encoding allowed transitions ON the enum value (not in caller code) prevents the "switch statement scattered across services" anti-pattern. Throwing `IllegalStateException` (not silent skip) catches integration bugs at first observable failure rather than producing inconsistent state silently.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-07). Pure domain enforcement; no compliance/PDPL impact.
- **Compliance check:** **N/A** — internal contract; failures surface as 5xx not user-data exposure.
- **Review cadence:** **Quarterly** + event-driven on transition matrix change. **Next review:** 2026-08-07.
- **Code reference:** `FrontendInstanceStatus.canTransitionTo()`

### BR-LIFE-003 — Single authority

- **Value:** `InstanceLifecycleService` is the only writer; controllers MUST delegate via semantic methods (`initiate`, `markInfrastructureReady`, `markBrandingCompleted`, `rebrand`, `markFailed`, `retry`).
- **Source:** `ai-branding-guidelines.md` §6 + ADR-004 (single-authority pattern for state machines).
- **Rationale:** A single writer is the precondition for outbox-event integrity (BR-LIFE-004) and audit-log completeness. If 2+ writers exist, ordering becomes implementation-dependent and outbox events can be missed. Semantic method names (vs raw `setStatus`) document intent and prevent invalid transitions at compile time (callers can't construct a `markBrandingCompleted` from `NOT_STARTED`).
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-07). Internal pattern; no regulated impact.
- **Compliance check:** **N/A** — architectural rule.
- **Review cadence:** **Annual** (stable pattern, no expected drift). **Next review:** 2027-05-07.
- **Code reference:** `kitehub/kitehub-branding/src/main/java/.../module/instance/service/InstanceLifecycleService.java`

### BR-LIFE-004 — Outbox event per transition

- **Value:** Every state change writes outbox row in same JPA txn (`OutboxEventWriter`); event types: `instance.{initializing,generating,deployed,regenerating,failed}`.
- **Source:** ADR-007 (Outbox Pattern) + design-patterns.md §3.5 (Outbox bypass policy).
- **Rationale:** Outbox + same-txn write guarantees that lifecycle changes published to consumers (cache invalidation, FE notifications, downstream analytics) match the persisted state — no state-event drift even on broker outage. Per design-patterns.md §3.5 Exception list, lifecycle is NOT eligible for direct-publish exception A (no fast-path requirement); outbox is mandatory.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-07). Internal reliability pattern.
- **Compliance check:** **Considered** — event payloads include `instanceId` (UUID) and `tenantId` (UUID); no PII (no email/name/phone in lifecycle events). Reviewed against PDPL Art 23: tenantId pseudonym does not constitute personal data per Decree 13/2023/NĐ-CP Art 2.
- **Review cadence:** **Quarterly** + event-driven on payload schema change (new field or sensitive-attribute addition). **Next review:** 2026-08-07.
- **Code reference:** Outbox writes at every `InstanceLifecycleService.mark*()` callsite; consumer side per ADR-007.

### BR-LIFE-005 — Max retries

- **Value:** `MAX_RETRIES = 3` consecutive failures before retry path is blocked; tracked in `FrontendInstance.retryCount`.
- **Source:** `ai-branding-guidelines.md` §6 + empirical observation Wave 4 baseline (no production data — informed gut). To be re-baselined post-Phase-1 launch with real failure-mode data.
- **Rationale:** 3 retries balances (a) tolerance for transient AI provider failures (Ollama/OpenAI 5xx, network blips — typically 1-2 retries succeed) vs (b) preventing runaway compute cost when a tenant request is permanently broken (e.g. impossible logo + brand combination triggers repeated AI rejection). 3 chosen as 2× expected transient-failure window; tunable per tier post-launch.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-07). Operational threshold; full Product review queued post-Phase-1 launch with real failure-rate data (Q3 2026).
- **Compliance check:** **N/A** — operational tuning, no PDPL/Consumer-Protection trigger (tenant sees "provisioning failed, please retry" — no advertised SLA tied to retry count).
- **Review cadence:** **Quarterly** + event-driven on (a) retry-rate >5% MoM, (b) tier-specific override request. **Next review:** 2026-08-07.
- **Code reference:** `InstanceLifecycleService.MAX_RETRIES` (static final; consider externalization to config in future PR).

### BR-LIFE-006 — Branding version monotonic

- **Value:** Increments on every successful `markBrandingCompleted` (DEPLOYED transition); drives ETag (BR-PKG-002) and FE cache invalidation.
- **Source:** ADR-009 (Composite Branding Package) + ai-branding-guidelines.md §7.1 (composite API + ETag scheme).
- **Rationale:** Monotonic version (vs timestamp or random hash) enables three downstream guarantees: (1) ETag comparison is strict-ordering — FE knows v5 is newer than v4 without parsing; (2) cache eviction is idempotent — out-of-order outbox event delivery still converges to latest version; (3) audit trail preserves rebranding history. Increments only on successful DEPLOYED — failed regenerations don't bump version (FE cache stays valid).
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-07). Internal cache contract.
- **Compliance check:** **N/A** — version number is opaque integer, not personal data.
- **Review cadence:** **Annual** (stable — change requires ETag scheme rev). **Next review:** 2027-05-07.
- **Code reference:** `FrontendInstance.brandingVersion` field; bumped in `InstanceLifecycleService.markBrandingCompleted()`.

---

## BR-QUALITY-001 — Compliance block status

The `BR-QUALITY-001` 5-attribute compliance block is documented above (see "BR-QUALITY-001 — Quality Gate Pass Threshold (5-attribute compliance block)" section, landed via GAP-386 on 2026-05-08). GAP-389-C (Wave 36 Bucket C, 2026-05-07) re-verifies the block matches `business-logic-review.md` v1.0.0 §2 — verdict: **COMPLIANT** (all 5 attributes present: Source, Rationale, Reviewer, Compliance check, Review cadence including next-review date 2026-08-08). No new block needed; entry counted toward Wave 36 Bucket C compliance tally as 7th of 7 (BR-LIFE-001..006 + BR-QUALITY-001).
