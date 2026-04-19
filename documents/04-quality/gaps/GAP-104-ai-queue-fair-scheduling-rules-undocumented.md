# GAP-104: Wave 3 Fair-Queue Phase 1 — Business Rules Undocumented

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (meta — Living Docs contract broken)
**Domain:** KiteHub / AI Branding / Business Docs
**Found:** 2026-04-19 (business-logic audit)
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
- [ ] Mỗi config key trong `kitehub-branding/application.yml:60-82` có BR-*-xxx đối ứng trong rules.md
- [ ] Mỗi BR-QUEUE-* có code reference pointer tới dispatcher/consumer class
- [ ] Use-case cho "AI job dispatch fair across tiers" thêm vào `use-cases.md`
- [ ] Pre-commit hook (`scripts/verify-business-docs.sh`) pass cho ai-queue domain nếu Option B
- [ ] Post-fix, `grep -c "BR-QUEUE\|fair-queue" documents/01-business/` ≥ 8

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Original feature PR: #341 (wave 3 AI async pipeline + fair queue Phase 1)
- Related gap (Phase 2 feature): GAP-005 (still OPEN per ROADMAP)
- Rules referenced: `.claude/rules/audit-to-gap-pipeline.md` §6 meta-boost, CLAUDE.md §Living Documents
