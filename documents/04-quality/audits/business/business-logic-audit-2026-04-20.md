# Business Logic Audit — 2026-04-20 (Refresh after Part B)

**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md`
**Auditor:** Claude (session-refresh / governance Part B verification)
**Commit base:** branch `worktree-refresh-biz-2026-04-20` off `main` (HEAD `d950da8a`)
**Scope window:** 2026-04-19 → 2026-04-20 (1 ngày — Part B 5 PRs)
**Previous audit:** [`business-logic-audit-2026-04-19.md`](./business-logic-audit-2026-04-19.md) (baseline 65/100 D)

---

## Summary

| Category | Score /20 | Δ vs 2026-04-19 | Evidence |
|----------|:---------:|:--------------:|----------|
| 1. Rule Coverage | 17 | **+3** | GAP-104 closed (18 BR-QUEUE rules + 4 UC-AGENT-08..11 documented); GAP-105 closed (30 BR-PARENT rules + 6 UC-PARENT); baseline #4 revealed FALSE POSITIVE (ResilientAIClient/MockAIClient/OllamaAIClient DO exist in `kiteclass-core/module/ai/client/` — baseline grep only covered `kitehub/`); BR-QUEUE-015..018 reference ResilientAIClient nhưng kitehub-branding KHÔNG có resilience annotation → 4 rules partially uncovered (new drift) |
| 2. Config Accuracy | 13 | **+2** | Parent-portal: 4 config keys `kiteclass.parent-portal.{enabled,invitation-ttl-hours,redeem-base-url,expire-sweep-ms}` match rules.md §7 exactly; ai-queue: 8 config keys `ai.queue.{fair-queue-enabled,tier-weights.*,concurrency.*,sla.*,backpressure.*}` + 4 resilience4j keys match application.yml; UNCHANGED drifts: GAP-106 (branding.routing.* keys vẫn thiếu), GAP-108 (12 invoice/payment keys vẫn hardcoded sau 28 ngày), GAP-110 (ollama default model mismatch llama3.1:8b vs gemma2), marketing keys vẫn thiếu |
| 3. Edge Case Tests | 15 | **+1** | Parent-portal: `ParentServiceTest.java` + `ParentInvitationServiceTest.java` cover UC-PARENT-01..06 happy + error paths (409 email exists, 404 invitation not found, 503 disabled, expired token); ai-queue: fair-queue tested upstream trong PR #341 fixtures (không trong Part B scope); UNCHANGED gaps: GAP-109 bulk-import error paths, payment late-fee boundary tests |
| 4. Cross-Domain Consistency | 15 | **0** | Parent-portal dùng `BR-PARENT-*`/`BR-PARENT-LINK-*`/`BR-PARENT-INV-*` — consistent pattern; BR-QUEUE-* prefix mới (phù hợp naming); ai-branding rules.md cross-link hợp lệ đến kiteclass/ai-agent-workflow §Fair-queue scheduler; NEW concern: BR-QUEUE-015..018 reference `ResilientAIClient` nhưng class này ở `kiteclass-core`, fair-queue code ở `kitehub-branding` — cross-module boundary cần clarification; rule prefix inconsistency cũ (BR-RET vs RET-) vẫn tồn tại |
| 5. Stakeholder Alignment | 12 | **+1** | Parent-portal rules §10 flag Wave 5 deferrals (PDPL consent capture, notification preferences, scheduler-lock); `PARENT_PORTAL_ENABLED=false` default cho đến khi PDPL wording hoàn tất — compliance-aware; Wave 3 fair-queue rules capture tier-weight reasoning + SLA targets (60/180/30s p95 per tier); UNCHANGED flags: VND currency hardcode, trademark seed list incomplete, K-12 grading scale regulation sign-off |
| **Total** | **72/100** | **+7** | **Grade: C (Acceptable — gaps need tracking, but no longer block GA on meta-contracts)** |

**Verdict:** 🟡 **C (72/100)** — Part B 2 meta-P0 gaps đóng thành công nâng score +7. Living Docs contract khôi phục cho Wave 3 fair-queue + parent-portal (hai surface P0 ở baseline). Drift còn lại thuộc nhóm P1 feature-level (GAP-106, GAP-108, GAP-109, GAP-110) — cùng danh sách baseline, chưa fix. 1 new partial drift phát hiện: BR-QUEUE-015..018 cite ResilientAIClient (lives kiteclass-core) nhưng fair-queue implementation nằm kitehub-branding không có @CircuitBreaker annotation → config CB dead trong service đó.

---

## Baseline Violation Tracker

| # | Baseline Violation | Gap | Status | Evidence |
|:-:|--------------------|-----|:------:|----------|
| 1 | Wave 3 fair-queue Phase 1 shipped no BR rules | GAP-104 | **🟢 CLOSED** | PR #371 (60da12d3) — 18 BR-QUEUE + UC-AGENT-08..11 + metrics catalogue in `ai-agent-workflow/rules.md` + `use-cases.md`; cross-link from `kitehub/ai-branding/rules.md` AIB-14 |
| 2 | Parent-portal folder missing (3-layer violation) | GAP-105 | **🟢 CLOSED** | PR #373 (72430b78) — `documents/01-business/kiteclass/parent-portal/{rules,use-cases,api-contract}.md` created; 30 BR-PARENT rules, 6 UC-PARENT, 5 endpoints documented |
| 3 | `branding.routing.*` config keys missing from yml | GAP-106 | **🔵 UNCHANGED** | Keys vẫn chỉ ở `resource-classification/rules.md:30-31`, zero hits in any `application*.yml` |
| 4 | AI-provider "ghost rules" (ResilientAIClient/MockAIClient/ai-live profile) | GAP-107 | **⚠️ FALSE POSITIVE** | All 3 classes EXIST: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/{ResilientAIClient,MockAIClient,OllamaAIClient}.java` + `@Profile("ai-live")` wired properly. Baseline grep covered `kitehub/` only — `kiteclass/` was the actual home. Domain `kiteclass/ai-provider` is correctly implemented. **GAP-107 recommend CLOSE as invalid.** |
| 5 | Payment-invoice 12 config keys documented but code hardcoded | GAP-108 | **🔵 UNCHANGED** | `InvoiceServiceImpl.java:64` vẫn `LATE_FEE_RATE = 0.001` hardcoded; zero `invoice.*`/`payment.gateway-timeout`/`payment.daily-limit` keys trong yml |
| 6 | Bulk-import rules undocumented (student-enrollment) | GAP-109 | **🔵 UNCHANGED** | `student-enrollment/rules.md` grep "BR-BULK" = 0 hits; `StudentBulkImportService` vẫn không có business rules |
| 7 | `ai.ollama.text-model` inter-service inconsistency | GAP-110 | **🔵 UNCHANGED** | kitehub-branding `text-model: llama3.1:8b`; kiteclass-core `default-model: gemma2` — 2 services 2 defaults |
| 8 | Marketing config keys 3 missing | batched GAP-108 | **🔵 UNCHANGED** | `marketing.contact.message.max-length`, `marketing.lead.sources`, `marketing.landing.color.pattern` vẫn chỉ trong docs |
| 9 | Rule prefix naming inconsistent (BR-RET vs RET- etc) | batched GAP-104 | **🔵 UNCHANGED** | 38 rules.md files vẫn dùng ≥5 convention khác nhau; BR-QUEUE mới thêm không làm tệ hơn |
| 10 | Reserved subdomains rationale missing | informational | **🔵 UNCHANGED** | INS-05 27 names không rationale — không gap-worthy, acceptable |

**Summary:** 2 CLOSED, 7 UNCHANGED, 1 FALSE POSITIVE (recommend invalidate GAP-107).

---

## New Violations (post-baseline)

| # | Severity | Violation | Rule ref | Code ref | Gap |
|:-:|:--------:|-----------|----------|----------|-----|
| 1 | 🟡 P2 | BR-QUEUE-015..018 (circuit breaker around AI provider) reference `ResilientAIClient` nhưng class này CHỈ tồn tại ở `kiteclass/kiteclass-core`, TRONG KHI fair-queue code (AIQueueDispatcher, AIJobConsumer) lives trong `kitehub-branding`. `kitehub-branding` pom có resilience4j-spring-boot3 dependency + application.yml §resilience4j block, NHƯNG không có `@CircuitBreaker`/`@Bulkhead`/`@Retry` annotation nào trong Java code. Config block hiện là dead code trong kitehub-branding scope. UC-AGENT-11 describes CB transition logic but no Java enforcement exists in the service hosting the fair-queue. | `ai-agent-workflow/rules.md:54-57` (BR-QUEUE-015..018) + `use-cases.md:93-102` (UC-AGENT-11) | `kitehub-branding/src/main/resources/application.yml:92-99` (config block) vs `grep -r "@CircuitBreaker" kitehub-branding/src/main/java` = 0 hits; `ResilientAIClient` only in `kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/ResilientAIClient.java` | GAP-148 |

**Delta on rule-code drift ranking:** 1 new P2 drift (CB dead config trong kitehub-branding). Severity cấp 3 (config drift but not business-breaking — fair-queue operational mà không cần CB ở Phase 1; Phase 2 sẽ cần).

---

## Rule-Code Drift Ranking (refreshed)

Drift severity: 1 = severe (security/data loss), 5 = trivial (naming).

| Rank | Drift | Severity | Type | Status |
|:----:|-------|:--------:|------|:------:|
| 1 | ~~Wave 3 fair-queue shipped no BR rules~~ | ~~1~~ | Missing governance | ✅ GAP-104 CLOSED |
| 2 | ~~Parent-portal domain folder missing~~ | ~~1~~ | 3-layer violation | ✅ GAP-105 CLOSED |
| 3 | Payment config 12 keys all missing | 2 | Hardcoded business rules (28d aged) | 🔵 GAP-108 open |
| 4 | `branding.routing.*` config missing | 2 | Template-first enforcement weak | 🔵 GAP-106 open |
| 5 | Bulk-import rules undocumented | 2 | Post-wave feature gap | 🔵 GAP-109 open |
| 6 | BR-QUEUE-015..018 dead CB config trong kitehub-branding | 3 | New drift from GAP-104 scope gap | 🆕 GAP-148 |
| 7 | `ai.ollama.text-model` inter-service mismatch | 3 | Config drift | 🔵 GAP-110 open |
| 8 | Marketing config keys missing | 3 | Hardcoded | 🔵 batched GAP-108 |
| 9 | Rule prefix naming inconsistent | 4 | Cosmetic | 🔵 batched GAP-104 |
| 10 | ~~ResilientAIClient "ghost rule"~~ | — | FALSE POSITIVE | ⚠️ GAP-107 invalidate |

---

## Delta Narrative

### Gains (+7 points)

1. **Meta-P0 → meta-fixed (GAP-104 + GAP-105):** Hai Living Docs violations P0 đóng trong Part B. Fix 1 lần, ngăn drift cho mọi AI feature PR + mọi parent-portal enhancement trong Wave 5. Force multiplier hoàn toàn đạt được.
2. **Cross-link pattern (AIB-14):** `kitehub/ai-branding/rules.md` thêm cross-link đến `kiteclass/ai-agent-workflow/rules.md` — tốt cho cross-service contract navigation.
3. **FE behavior documentation:** UC-AGENT-10 (backpressure) + UC-PARENT-01..06 đều capture FE behavior explicitly — tuân thủ 3-layer template.
4. **Compliance awareness:** Parent-portal rules.md §10 flag PDPL + scheduler-lock deferrals rõ ràng; PARENT_PORTAL_ENABLED default false thể hiện gate PDPL.

### Remaining drift (blocks A-grade)

- **P1 feature drift từ baseline CHƯA fix** (GAP-106, GAP-108, GAP-109, GAP-110) — 4 gaps vẫn open sau 28 ngày (với GAP-108 carried forward từ 2026-03-23).
- **Cat 2 còn 13/20** chủ yếu do 15+ config keys vẫn hardcoded hoặc missing across invoice / payment / marketing / branding routing.
- **1 new partial drift** (GAP-148) từ scope gap của GAP-104 — không critical.

### What would get 80+ (B grade)?

1. GAP-108 externalize 12 payment/invoice config keys (+3 Cat 2)
2. GAP-109 document BR-BULK-* rules (+2 Cat 1)
3. GAP-106 add `branding.routing.*` keys to yml (+1 Cat 2)
4. Tests for UC-AGENT-09/10 + payment late-fee boundary (+2 Cat 3)

Total potential: +8 → 80/100 B grade khả thi trong 1 sprint.

---

## Recommendation (Fix Order)

Theo `meta-gap-priority.md` + `audit-to-gap-pipeline.md` §6:

### Sprint 1 (P1 carry-forward — pay down 28-day debt)
1. **GAP-108** (payment-invoice 12 config keys) — aged 28 days, externalize + unit test boundaries
2. **GAP-109** (bulk-import rules) — feature đã ship Wave 1 (PR #332) nhưng 0 BR-BULK → drift lớn nhất về governance

### Sprint 2 (P1 cleanup)
3. **GAP-106** (branding.routing config) — externalize + update `resource-classification/rules.md`
4. **GAP-110** (ollama default model) — pick canonical default, update both yml + rule
5. **GAP-148** (new P2 — CB dead config) — hoặc wrap AIQueueDispatcher/AIJobConsumer in `@CircuitBreaker("ai-provider")`, hoặc remove resilience4j block khỏi kitehub-branding nếu không dùng

### Administrative
6. **GAP-107** — recommend CLOSE as invalid (false positive baseline; domain verified correctly implemented)

### Exclusion (không gap-worthy refresh này)
- Stakeholder alignment flags — escalate business/legal, không tạo gap
- Rule prefix naming — batch theo GAP-104 scope guideline (không block)
- Reserved subdomain rationale — informational

---

## Evidence Logs

### GAP-104 closure verification (BR-QUEUE in rules.md + yml)

```yaml
# kitehub-branding/application.yml:69-89
queue:
  fair-queue-enabled: ${AI_FAIR_QUEUE_ENABLED:true}  # BR-QUEUE-001
  tier-weights:
    enterprise: 3   # BR-QUEUE-002
    pro: 2          # BR-QUEUE-003
    free: 1         # BR-QUEUE-004
  concurrency:
    free: 1         # BR-QUEUE-005
    pro: 3          # BR-QUEUE-006
    enterprise: 10  # BR-QUEUE-007
  sla:
    free-p95-seconds: 180       # BR-QUEUE-008
    pro-p95-seconds: 60         # BR-QUEUE-009
    enterprise-p95-seconds: 30  # BR-QUEUE-010
  backpressure:
    enterprise-backlog-threshold: 50  # BR-QUEUE-011

resilience4j:
  circuitbreaker:
    instances:
      ai-provider:
        failureRateThreshold: 50       # BR-QUEUE-015
        waitDurationInOpenState: 30s   # BR-QUEUE-016
        slidingWindowSize: 20          # BR-QUEUE-017
        minimumNumberOfCalls: 10       # BR-QUEUE-018
```
→ 16/18 BR-QUEUE fully traceable config → yml → Java class (dispatcher/consumer). 4 (BR-QUEUE-015..018) trace to config-only; Java wiring absent in kitehub-branding (new GAP-148).

### GAP-105 closure verification (parent-portal 3-layer)

```
documents/01-business/kiteclass/parent-portal/
├── api-contract.md   (251 lines)
├── rules.md          (150 lines — 30 BR-PARENT rules)
└── use-cases.md      (204 lines — 6 UC-PARENT)
```
Code ref: `ParentPortalProperties.java:15` javadoc now traceable to BR-PARENT-003 (invitation-ttl-hours); `ParentInvitationServiceImpl.java:115` cũng align.

### Baseline false positive #4 (ResilientAIClient EXISTS)

```
$ grep -l "class ResilientAIClient" kiteclass/ --include="*.java" -r
kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/ResilientAIClient.java

$ grep "@Profile\|@Primary" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/{ResilientAIClient,MockAIClient,OllamaAIClient}.java
MockAIClient.java: @Profile("!ai-live")
OllamaAIClient.java: @Profile("ai-live")
ResilientAIClient.java: @Primary
```
→ Domain correctly implemented. Baseline audit scope error (grep kitehub/ cho kiteclass/ rule).

### New drift: BR-QUEUE-015..018 dead CB config

```
$ grep -rn "@CircuitBreaker\|@Bulkhead\|@Retry" kitehub/kitehub-branding/src/main/java/
# 0 hits

$ grep "ResilientAIClient" kitehub/kitehub-branding/src/main/java/ -r
# 0 hits
```
→ kitehub-branding pom có `resilience4j-spring-boot3` dep + application.yml §resilience4j block, nhưng không có Java wiring → config dead. BR-QUEUE-015..018 + UC-AGENT-11 describe CB trong service context này nhưng enforcement mismatch.

### UNCHANGED baseline gaps (sample spot-check)

```
$ grep -c "LATE_FEE_RATE" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/service/InvoiceServiceImpl.java
2   # still hardcoded at line 64

$ grep -c "branding.routing" kitehub/*/src/main/resources/application*.yml kiteclass/*/src/main/resources/application*.yml
0   # still absent

$ grep -c "BR-BULK" documents/01-business/kiteclass/student-enrollment/rules.md
0   # still undocumented
```

---

## Gap files created

| Gap | Priority | Meta? | Title |
|-----|:--------:|:-----:|-------|
| GAP-148 | 🟡 P2 | Feature | BR-QUEUE-015..018 circuit breaker config dead in kitehub-branding (no Java @CircuitBreaker wiring) |

(No other new gaps — refresh scope limited to Part B delta verification.)

---

## Audit methodology notes

**Context limits applied (per SKILL.md §Context Management):**
- Re-used baseline 2026-04-19 fully-audited 10 domains + 18 spot-checked — carried forward scores where unchanged
- Focused re-verification only on 2 Part B closure targets (ai-agent-workflow + parent-portal)
- Delta scan: `git log --since=2026-04-19 --oneline --name-only -- "*.java" "*.yml"` + `documents/01-business/` change tracking
- Grep capped `| head -N` per query per skill guidance

**False positive root cause:** Baseline audit's BR-AI-002 violation (GAP-107) stemmed from grep scope mismatch — checked `kitehub/` for classes documented in `kiteclass/ai-provider/rules.md`. Refresh confirmed all 3 classes (ResilientAIClient/MockAIClient/OllamaAIClient) exist in `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/` with correct `@Profile`/`@Primary` wiring per rule. **Recommend:** update `business-logic-audit/SKILL.md` §Gotchas to note "rule domain path (kiteclass/ vs kitehub/) determines which service to grep — don't default to kitehub/". Not blocking refresh; note for skill maintainers.

**Category 5 flags for human review (carry forward — not auto-gap'd):**
- Trademark seed list Nike/Adidas/Apple Inc — incomplete for VN market
- VND-only currency hardcode (SUB-15) — no multi-currency roadmap
- Retention grace 7 days — verify Nghị định 13/2023 (PDPL) sign-off
- K-12 grading scale — verify match Bộ GD&ĐT current regulation
- Parent-portal PDPL wording — required trước khi flip PARENT_PORTAL_ENABLED=true

---

## Log

- 2026-04-20 — Refresh sau Part B merge: score 72/100 (C, +7 delta vs 65 baseline). GAP-104 + GAP-105 closed (meta-P0). GAP-107 revealed FALSE POSITIVE. 1 new P2 drift GAP-148 (dead CB config). Remaining P1 debt (GAP-106/108/109/110) unchanged — recommend Sprint 1 pay down.
