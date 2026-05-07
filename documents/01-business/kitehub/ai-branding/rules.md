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
| BR-WIZARD-006 | Free-form prompt BAN | Per ai-branding-guidelines.md §2.1, free-form text prompts are BANNED for FREE/PRO/PREMIUM tiers; ENTERPRISE may opt-in via `ai.enterprise.advancedModeEnabled` flag (not yet wired) | per ai-branding-guidelines.md §2.4 |

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
