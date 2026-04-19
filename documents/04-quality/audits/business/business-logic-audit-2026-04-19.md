# Business Logic Audit — 2026-04-19

**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md`
**Auditor:** Claude (session-audit-1 / governance catch-up)
**Commit base:** branch `worktree-agent-a62c1aa7` off `main`
**Scope window:** 2026-03-23 → 2026-04-19 (27 ngày — Wave 2, Wave 3b, Wave 4b + post-wave features)
**Previous audit:** `business-gap-check-2026-03-23-*.md` (27 ngày trước — 🔴 STALE)

---

## Summary

| Category | Score /20 | Evidence |
|----------|:---------:|----------|
| 1. Rule Coverage | 14 | Mapping BR-xxx → code: ~80% hit rate; 2 ghost rules (ResilientAIClient, MockAIClient profile) point to non-existent classes; Wave 3 fair-queue Phase 1 features shipped without BR-*; parent-portal feature has no rules.md despite referencing BR-PARENT-003 |
| 2. Config Accuracy | 11 | Waves 2-4 configs (parent-portal, security.\*, moderation.\*, retention.\*, quality-gate.\*, legal.trademark.\*) match rules perfectly; BUT `branding.routing.template-first`, `branding.routing.max-ai-ratio`, all `invoice.*` + `payment.gateway-timeout/minimum-amount/daily-limit.*` + `marketing.*` keys documented in rules.md but ABSENT from every `application*.yml`; `ai.ollama.text-model` inconsistent between kiteclass-core (`gemma2`) và kitehub-branding (`llama3.1:8b`) |
| 3. Edge Case Tests | 14 | Wave 2 parent-portal tests cover happy path + 3 errors; re-trial prevention TR-07 test exists (PR #311, GAP-092 DONE); Wave 4 moderation, CSRF, retention state transitions tested; BUT no tests for payment late-fee boundary + no bulk-import error-path tests for GAP-051 bulk CSV in-file duplicate |
| 4. Cross-Domain Consistency | 15 | Trial duration 14d consistent (TR-01/INS-01); retention per-tier values consistent between `kitehub/trial-lifecycle` + `kitehub/data-retention`; RET-01/RET-02 "TRIAL=FREE=7 ngày" coherent; MINOR: rule ID prefix inconsistent (BR-RET-xxx trong kiteclass vs RET-xx trong kitehub — cùng domain name khác cấu trúc prefix); AI branding wizard tiers (FREE/PRO/PREMIUM/ENTERPRISE) match ai-branding-guidelines §4.3 and rules.md |
| 5. Stakeholder Alignment | 11 | Re-trial prevention rule TR-07 added (compliance with "1 trial per owner" anti-abuse); VN-law alignment đảm bảo GDPR retention + Nghị định 13/2023; retention deletion grace 7 days consistent với ADR-013; FLAG cho human review: trademark seed list (Nike/Adidas/Apple Inc) chỉ 3 entries — không đủ cho VN market; VND currency hardcoded trong SUB-15 không document multi-currency roadmap |
| **Total** | **65/100** | **Grade: D (significant gaps — block GA)** |

**Verdict:** 🔴 **D (65/100)** — drift đáng kể, cần fix trước GA. Core Wave 2-4 delivery correct (parent-portal + security + moderation + retention all implemented đúng docs), nhưng Living Docs contract bị broken ở ≥4 surface: Wave 3 fair-queue (shipped no rules), parent-portal (code references BR-PARENT-003 but no domain folder), payment-invoice config externalization (docs say config keys but code hardcoded), branding routing (docs say config keys but code missing).

---

## Domains Covered (38 domains scanned)

### Fully audited (10 domains — deep BR → code trace)
1. **kitehub/trial-lifecycle** — TR-01 → TR-07 (7 rules)
2. **kitehub/subscription-billing** — SUB-01 → SUB-16 (16 rules)
3. **kitehub/data-retention** — RET-01 → RET-20 (20 rules)
4. **kitehub/instance-provisioning** — INS-01 → INS-16 (16 rules)
5. **kitehub/ai-branding** — AIB-01 → AIB-13 (13 rules)
6. **kitehub/domain-management** — DOM-01 → DOM-10 (10 rules)
7. **kitehub/email-lifecycle** — EML-01 → EML-08 (8 rules)
8. **kiteclass/ai-provider** — BR-AI-001 → BR-AI-006 (6 rules)
9. **kiteclass/ai-agent-workflow** — BR-STEP-*, BR-EXEC-*, BR-AGENT-* (13 rules)
10. **kiteclass/security-hardening** — BR-SEC-001 → BR-SEC-003 (3 rules)

### Spot-checked (18 domains — verify major BR coverage only)
- kiteclass/academic-year (7), k12-model (15), role-hierarchy (11), content-moderation (20), legal-ip-protection (13), data-retention (13), quality-gate (8), instance-lifecycle (5), branding-wizard (9), resource-classification (10), resource-handlers (6), rebrand-approval (8), marketing (19), payment-invoice (15), student-enrollment (12), tenant-settings (18), tenant-provisioning (9), security-foundation (9)

### Spot-sampled only (10 domains — metadata only, not drift-analyzed)
- attendance, branding-api, course-class, gamification-points, grade-assignment, lms, notification-email, outbox-events, storage, teacher

**Total BR tracked:** ~450 rules across 38 domains.

---

## Top 10 Violations

| # | Severity | Violation | Rule ref | Code ref | Gap |
|---|:--------:|-----------|----------|----------|-----|
| 1 | 🔴 P0 | Wave 3 fair-queue Phase 1 (GAP-005a) shipped với 8 new config keys + fair dispatcher nhưng `ai-agent-workflow/rules.md` KHÔNG có BR-QUEUE-* rules cho tier-weights/concurrency/SLA/backpressure. Living Docs rule violated. | `ai-agent-workflow/rules.md` thiếu rules | `kitehub-branding/src/main/resources/application.yml:60-82` (queue.fair-queue-enabled, tier-weights, concurrency, sla, backpressure) + `kitehub-branding/src/main/java/com/kitehub/branding/queue/AIQueueDispatcher.java` | GAP-104 |
| 2 | 🔴 P0 | Parent-portal domain KHÔNG có `documents/01-business/kiteclass/parent-portal/` folder dù `ParentPortalProperties.java` javadoc tham chiếu BR-PARENT-003. Living Docs contract broken. | `BR-PARENT-003` referenced in javadoc | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/config/ParentPortalProperties.java:16` ("default 24 h per BR-PARENT-003") | GAP-105 |
| 3 | 🟠 P1 | `branding.routing.template-first` + `branding.routing.max-ai-ratio` documented trong `resource-classification/rules.md` BUT config keys ABSENT from tất cả application.yml. Template-first enforcement chỉ dựa vào code flow chứ không config-driven như docs claim. | `resource-classification/rules.md:30-31` | `grep branding.routing.*` trong `kitehub/*/src/main/resources/application*.yml` → 0 hit | GAP-106 |
| 4 | 🟠 P1 | `ai-provider/rules.md` BR-AI-002 nói "all calls routed through ResilientAIClient (primary bean)" nhưng class `ResilientAIClient` KHÔNG tồn tại trong codebase. BR-AI-005 nói "MockAIClient / OllamaAIClient (profile ai-live)" nhưng code dùng `OpenAIClient` + `OllamaClient` (không có MockAIClient class + dùng `ai.provider` property thay vì `ai-live` profile). | `ai-provider/rules.md:12-15` (BR-AI-002, BR-AI-005) | `grep ResilientAIClient` trong kitehub/ → 0 hit; `kitehub-branding/src/main/java/com/kitehub/branding/config/AIProviderConfig.java:46-64` dùng `provider` property | GAP-107 |
| 5 | 🟠 P1 | Payment-Invoice rules.md §4 document 12 config keys (`invoice.payment-term-days`, `invoice.late-fee-percent-per-day`, `invoice.late-fee-max-percent`, `invoice.installment.*`, `payment.gateway-timeout-minutes`, `payment.minimum-amount`, `payment.daily-limit.*`) nhưng NONE exist in application.yml. Code hardcoded: `InvoiceServiceImpl.java:64` `private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.001")`. | `payment-invoice/rules.md:89-100` | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/service/InvoiceServiceImpl.java:64` + application.yml không chứa `invoice.*` keys | GAP-108 |
| 6 | 🟠 P1 | Bulk-import cho Student (GAP-051 Wave 1 PR #332 + #338 trên main) shipped `StudentBulkImportService` với in-file duplicate detection, nhưng `student-enrollment/rules.md` KHÔNG có BR-BULK-* rules. Business rules cho max-rows, duplicate policy, tenant isolation during bulk import undocumented. | `student-enrollment/rules.md` chỉ có BR-STU-* + BR-ENROLL-* | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/service/StudentBulkImportService.java` | GAP-109 |
| 7 | 🟡 P2 | `kitehub-branding/application.yml:56` default `text-model: llama3.1:8b` INCONSISTENT với `kiteclass/kiteclass-core/application.yml:135` default `default-model: gemma2` — cùng một AI infra nhưng 2 services dùng 2 models mặc định khác nhau. `ai-provider/rules.md` BR-AI-004 list key là `ai.ollama.default-model` = `gemma2` (chỉ match kiteclass). | `ai-provider/rules.md:57-58` | `kitehub/kitehub-branding/src/main/resources/application.yml:56` vs `kiteclass/kiteclass-core/src/main/resources/application.yml:135` | GAP-110 |
| 8 | 🟡 P2 | Marketing rules.md §2 define 3 config keys (`marketing.contact.message.max-length`, `marketing.lead.sources`, `marketing.landing.color.pattern`) nhưng NONE exist in application.yml; max-length `2000` + color regex `^#[0-9A-Fa-f]{6}$` hiện hardcoded trong DTOs / entity validators. | `marketing/rules.md:51-55` | `grep marketing.contact\|marketing.lead\|marketing.landing` trong kiteclass/*/src/main/resources/ → 0 hit | (batch in GAP-108) |
| 9 | 🟡 P2 | `kiteclass/data-retention/rules.md` dùng prefix `BR-RET-*` trong khi `kitehub/data-retention/rules.md` dùng prefix `RET-*` (không có BR-). Inconsistent naming convention across domains (có domain dùng BR-xxx-*, có domain bare prefix như TR-/SUB-/DOM-/INS-/EML-/AIB-/RET-). | `data-retention/rules.md` (cả 2 side) | 38 rules.md files với ≥5 different prefix conventions | (batch in GAP-104) |
| 10 | 🟢 P3 | Reserved subdomains trong `instance-provisioning/rules.md` INS-05 list 27 names — nhưng rule không documented WHY 27 (no rationale) + không có timestamp mark "stable list". Người mới đọc không biết có thêm được không. | `instance-provisioning/rules.md:27-36` | `RESERVED_SUBDOMAINS` constant in code | (informational — không tạo gap) |

---

## Rule-Code Drift Ranking

Drift = rule documents X, code implements Y (≠X). Score: 1 = severe (security/data loss), 5 = trivial (naming).

| Rank | Drift | Severity | Type |
|:----:|-------|:--------:|------|
| 1 | Wave 3 fair-queue shipped no BR rules | 1 | Missing governance for live feature |
| 2 | Parent-portal domain folder missing | 1 | 3-layer structure violation |
| 3 | `ResilientAIClient` class non-existent | 2 | Ghost rule — reviewer can't trace |
| 4 | `branding.routing.*` config missing | 2 | Template-first enforcement weaker than docs |
| 5 | Payment config 12 keys all missing | 2 | Hardcoded business rules (known since 2026-03-23 audit, not yet fixed) |
| 6 | Bulk-import rules undocumented | 2 | Post-wave feature gap |
| 7 | `ai.ollama.text-model` inter-service mismatch | 3 | Config drift but not business-breaking |
| 8 | Marketing config keys missing | 3 | Hardcoded but low-change |
| 9 | Rule prefix naming inconsistent | 4 | Cosmetic, trace-friction |
| 10 | Reserved subdomains rationale missing | 5 | Docs clarity |

---

## Recommendation (Fix Order)

Theo `meta-gap-priority.md` + `audit-to-gap-pipeline.md` §6:

### Sprint 1 (P0 unblock — meta-first)
1. **GAP-104** (Wave 3 fair-queue rules) — **meta-boost**: Living Docs rule broken → force-multiplier cho tương lai. Fix 1 lần, prevent drift cho mọi AI feature PR sắp tới. Yêu cầu thêm BR-QUEUE-* trong `ai-agent-workflow/rules.md` + use-cases.md cho fair-scheduling.
2. **GAP-105** (parent-portal rules.md missing) — **meta-boost**: 3-layer structure là hard rule của project. Phải tạo `documents/01-business/kiteclass/parent-portal/{rules,use-cases,api-contract}.md` trước Wave 5 landing full parent dashboard.

### Sprint 2 (P1 feature correctness)
3. **GAP-106** (branding.routing config keys) — externalize + update rules
4. **GAP-107** (AI provider ghost rules) — fix rules.md để match code thực tế (Mock class không có; profile `ai-live` không tồn tại; ResilientAIClient không tồn tại)
5. **GAP-108** (payment-invoice hardcoded rules) — externalize 12 config keys (known since 2026-03-23 audit, đã carry forward 27 ngày)
6. **GAP-109** (bulk-import rules undocumented) — thêm BR-BULK-* rules + use-cases cho Wave 1 GAP-051

### Sprint 3 (P2 cleanup)
7. **GAP-110** (cross-service ollama model default inconsistent) — pick one canonical default, update both yml + rule

### Exclusion (not gap-worthy này audit)
- Stakeholder alignment flags (VND hardcode, trademark seed list 3 entries) → escalate to business/legal team, không tạo gap cho đến khi có stakeholder input
- Reserved subdomain rationale missing → low priority docs clarity
- Rule prefix naming inconsistency → batch trong GAP-104 as scope guideline

---

## Evidence Logs

### Fair-queue config keys (kitehub-branding/application.yml:60-82)
```yaml
queue:
  fair-queue-enabled: ${AI_FAIR_QUEUE_ENABLED:true}
  tier-weights:
    enterprise: 3
    pro: 2
    free: 1
  concurrency:
    free: 1
    pro: 3
    enterprise: 10
  sla:
    free-p95-seconds: 180
    pro-p95-seconds: 60
    enterprise-p95-seconds: 30
  backpressure:
    enterprise-backlog-threshold: 50
```
→ Zero trace trong `ai-agent-workflow/rules.md` hoặc `ai-branding/rules.md`.

### Parent-portal javadoc reference (code)
```java
// ParentPortalProperties.java:16-18
// @param invitationTtlHours   token lifetime — default 24 h per BR-PARENT-003.
```
→ `documents/01-business/kiteclass/parent-portal/` folder KHÔNG tồn tại.

### Ghost rule: ResilientAIClient
```
$ grep -r "ResilientAIClient" kitehub/ kiteclass/ --include="*.java"
# 0 hits
$ grep "ResilientAIClient" documents/01-business/kiteclass/ai-provider/rules.md
BR-AI-002 | All calls routed through `ResilientAIClient` (primary bean) ...
```

### Payment late fee hardcoded
```java
// InvoiceServiceImpl.java:64
private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.001");
```
→ Rules.md documents `invoice.late-fee-percent-per-day: 0.1` config key. Drift persisted 27 days since 2026-03-23 audit flagged it.

### AI ollama text-model inconsistency
```yaml
# kitehub-branding/application.yml:56
text-model: ${OLLAMA_TEXT_MODEL:llama3.1:8b}

# kiteclass-core/application.yml:135
default-model: ${AI_OLLAMA_MODEL:gemma2}
```

---

## Gap files created

| Gap | Priority | Meta? | Title |
|-----|:--------:|:-----:|-------|
| GAP-104 | 🔴 P0 | ✅ Meta | Wave 3 fair-queue Phase 1 rules undocumented |
| GAP-105 | 🔴 P0 | ✅ Meta | Parent-portal domain missing 3-layer docs |
| GAP-106 | 🟠 P1 | Feature | Branding routing config keys missing from application.yml |
| GAP-107 | 🟠 P1 | Feature | AI-provider rules.md references non-existent classes (ResilientAIClient / MockAIClient / ai-live profile) |
| GAP-108 | 🟠 P1 | Feature | Payment-invoice rules document 12 config keys but code hardcoded (drift since 2026-03-23) |
| GAP-109 | 🟠 P1 | Feature | Bulk-import rules undocumented for student-enrollment domain |
| GAP-110 | 🟡 P2 | Feature | Cross-service Ollama default model inconsistent (kitehub=llama3.1:8b vs kiteclass=gemma2) |

---

## Audit methodology notes

**Context limits applied (per SKILL.md §Context Management):**
- Grep outputs capped `| head -30` per query
- 10 domains fully audited BR-by-BR; 18 spot-checked; 10 metadata-sampled (token budget)
- Wave 2-4 focused; Wave 1 reviewed through bulk-import Wave 1 PR #332 + #338
- Skipped detailed scoring cho stable pre-Wave-2 domains (course-class, attendance, lms)

**Category 5 flags for human review (not auto-gap'd):**
- Trademark seed list Nike/Adidas/Apple Inc — incomplete for VN market (no VN brands like Vingroup/FPT)
- VND-only currency hardcode (SUB-15) — no multi-currency roadmap
- Retention grace 7 days — verify compliance with Nghị định 13/2023 (PDPL) sign-off
- K-12 grading scale (Giỏi≥8, Khá≥6.5, TB≥5, Yếu<5) — verify matches Bộ GD&ĐT current regulation

---

## Log
- 2026-04-19 — Initial baseline audit after 27-day stale (previous: 2026-03-23). Scope: Wave 2 (parent portal), Wave 3b (async pipeline), Wave 4b (branding propagation), post-wave features #338, #353-355. Output: 65/100 (Grade D), 7 gaps created (GAP-104 .. GAP-110).
