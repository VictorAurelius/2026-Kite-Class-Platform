# GAP-104: Wave 3 Fair-Queue Phase 1 — Business Rules Undocumented

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (meta — Living Docs contract broken)
**Domain:** KiteHub / AI Branding / Business Docs
**Found:** 2026-04-19 (business-logic audit)
**Fixed:** 2026-04-19 — Part B audit catch-up (PR pending merge)
**Affects:** kitehub-branding service, future AI feature PRs, audit traceability

## Problem

Wave 3 Phase 1 shipped qua PR #341 (GAP-005a) đã introduce **fair-queue scheduler** với 8+ config keys mới trong `kitehub-branding/application.yml` và mới `AIQueueDispatcher` + `AIJobPriority` + `AIJobConsumer` classes. Nhưng `documents/01-business/kiteclass/ai-agent-workflow/rules.md` (hoặc bất kỳ rules.md nào) **KHÔNG có** rule nào về:

- Fair-queue feature flag (`queue.fair-queue-enabled`)
- Tier-weighted round-robin (enterprise=3, pro=2, free=1)
- Per-tier concurrency caps (free=1, pro=3, enterprise=10)
- SLA p95 targets (free=180s, pro=60s, enterprise=30s)
- Backpressure threshold (enterprise-backlog-threshold=50)
- Resilience4j circuit breaker around AI provider (separate `ai-provider` CB instance)

Evidence:
```
$ grep -r "fair-queue\|tier-weights\|sla.*p95" documents/01-business/
# 0 hits
$ grep -r "fair-queue\|tier-weights" kitehub/kitehub-branding/src/main/resources/application.yml
60:    fair-queue-enabled: ${AI_FAIR_QUEUE_ENABLED:true}
65:      enterprise: 3
74:      free-p95-seconds: 180
```

## Root Cause

Living Docs rule (CLAUDE.md §Business Logic Documents 3-Layer) requires: "đổi logic = đổi doc trong cùng commit". PR #341 đã ship code + config nhưng không update `ai-agent-workflow/rules.md`. Root cause đoán: Wave 3 Phase 1 ưu tiên delivery async pipeline, docs update slip qua post-merge review nhưng không bao giờ được catch-up.

Meta impact: every subsequent AI feature PR không có BR-QUEUE-* để reference → reviewer không thể trace "tier-weights enterprise:pro:free=3:2:1 có phải business rule chính thức?" → reviews drift sâu hơn.

## Proposed Fix

Tạo hoặc update rules + use-cases cho fair-queue:

**Option A (simpler):** thêm section mới trong `documents/01-business/kiteclass/ai-agent-workflow/rules.md`:
```markdown
### Fair-queue scheduler (Wave 3 Phase 1, GAP-005a)
| ID | Rule |
|----|------|
| BR-QUEUE-001 | Feature flag `queue.fair-queue-enabled` để rollback tới single-queue |
| BR-QUEUE-002 | Weighted round-robin: enterprise=3, pro=2, free=1 |
| BR-QUEUE-003 | Per-tier concurrency caps: free=1, pro=3, enterprise=10 |
| BR-QUEUE-004 | SLA p95 target (informational): free=180s, pro=60s, enterprise=30s |
| BR-QUEUE-005 | Backpressure: free tier degrade khi enterprise backlog ≥50 jobs |
| BR-QUEUE-006 | CB: failure-rate-threshold 50%, wait-duration-open 30s, slidingWindow 20 |
```

**Option B (cleaner):** tạo domain mới `documents/01-business/kitehub/ai-queue/` với rules + use-cases + api-contract (nếu có endpoint exposed).

Choose based on coupling: nếu queue logic sẽ expand (Phase 2 = GAP-005 still open), Option B; nếu stable, Option A.

## Acceptance Criteria
- [x] Mỗi config key trong `kitehub-branding/application.yml:60-82` có BR-*-xxx đối ứng trong rules.md (BR-QUEUE-001..014 cho `ai.queue.*`, BR-QUEUE-015..018 cho `resilience4j.circuitbreaker.ai-provider.*`)
- [x] Mỗi BR-QUEUE-* có code reference pointer tới dispatcher/consumer class (`AIQueueProperties`, `AIQueueConfig`, `AIQueueDispatcher`, `AIJobConsumer`, `AIJobPriority`, `BacklogInspector`, `DistributedRateLimiter`, `ResilientAIClient`)
- [x] Use-case cho "AI job dispatch fair across tiers" thêm vào `use-cases.md` (UC-AGENT-08 fair dispatch, UC-AGENT-09 concurrency NACK, UC-AGENT-10 backpressure degrade, UC-AGENT-11 circuit breaker open)
- [x] Option A chosen — extended `kiteclass/ai-agent-workflow/rules.md` thay vì tạo domain mới (Phase 2 fair-queue mới deferred per GAP-005, không cần dedicated domain ngay)
- [x] Post-fix: rules.md có 18 BR-QUEUE-* entries; cross-reference từ `kitehub/ai-branding/rules.md` (AIB-14)

## Resolution
- Fix scope: docs-only (no code changes)
- Files changed:
  - `documents/01-business/kiteclass/ai-agent-workflow/rules.md` — added BR-QUEUE-001..018, metrics catalogue, config keys table
  - `documents/01-business/kiteclass/ai-agent-workflow/use-cases.md` — added UC-AGENT-08..11
  - `documents/01-business/kitehub/ai-branding/rules.md` — added AIB-14 cross-reference + config prefix update
- Decision: Option A (single rules.md extension) over Option B (new `kitehub/ai-queue/` domain) — fair-queue chỉ active trong 1 service hiện tại; Phase 2 (GAP-005) sẽ revisit nếu queue logic expand cross-service.
- Living Docs contract restored cho Wave 3 Phase 1.

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Original feature PR: #341 (wave 3 AI async pipeline + fair queue Phase 1)
- Related gap (Phase 2 feature): GAP-005 (still OPEN per ROADMAP)
- Rules referenced: `.claude/rules/audit-to-gap-pipeline.md` §6 meta-boost, CLAUDE.md §Living Documents
