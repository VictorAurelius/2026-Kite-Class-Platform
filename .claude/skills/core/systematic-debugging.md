---
description: "Dùng khi gặp lỗi khó hiểu, user nói 'debug', 'investigate', 'root cause', 'tại sao fail', 'lỗi kỳ lạ', 'unexpected behavior'. Áp dụng 4-phase: Reproduce → Trace → Root Cause → Defensive Fix. Skip: typos/syntax errors rõ ràng, known issues trong troubleshooting.md."
---

# Systematic Debugging

## Khi nào dùng

- Bug investigations (any severity)
- Unexpected behavior in tests
- Integration failures
- Performance issues

## Khi nào skip

- Typos/syntax errors (obvious fix)
- Compiler errors (clear error message)
- Known issues — check `troubleshooting.md` first

## 4-Phase Process (High Level)

1. **Reproduce** (15-30 min) — Create failing test case, document exact steps, verify consistency
2. **Trace** (30-60 min) — Follow execution flow, identify divergence point (debugger/logs)
3. **Root Cause** (30-45 min) — 5 Whys technique, distinguish symptom vs cause
4. **Defensive Fix** (1-2 hrs) — Fix root cause, add regression test, update docs

## Gotchas

- **`findById()` bypasses Hibernate filter** — dùng custom query `findByIdAndDeletedFalse()` thay thế; JPA findById() dùng EntityManager.find() không qua interceptors
- **Redis cache stale sau rebuild** — clear Redis (`docker exec kiteclass-redis redis-cli FLUSHALL`) khi restart sau code change
- **Flyway checksum -1** — từ prior `flyway repair`; fix bằng `validate-on-migrate: false` trong application.yml
- **Testcontainers @SuppressWarnings("resource")** — lifecycle managed by JVM shutdown hook, KHÔNG manually close
- **Multi-tenant 401/403** — check `TenantContext.getTenantId()` trước khi debug service logic; filter chạy trước controller
- **MockMvc async endpoints** — `Mono<ResponseEntity>` cần `.andExpect(request().asyncStarted())` + `asyncDispatch()`

## Skill Contents

- `quick-reference/systematic-debugging-4phases.md` — Chi tiết 4 phases với examples (multi-tenant filter, Redis serialization)
- `quick-reference/systematic-debugging-checklist.md` — Pre-debugging sanity check

## Trigger Phrases

"debug", "investigate", "root cause", "tại sao fail", "lỗi kỳ lạ", "unexpected behavior", "failing test"

## Quick Checklist

- [ ] Phase 1: Reproduce consistently? (test case exists, fails 3+ times)
- [ ] Phase 2: Traced execution flow? (debugger or debug logs, divergence point found)
- [ ] Phase 3: Root cause identified? (5 Whys, not just symptom)
- [ ] Phase 4: Regression test added? (prevents recurrence)
- [ ] Phase 4: `troubleshooting.md` or `MEMORY.md` updated?

**If stuck:** Explain problem out loud (rubber duck), check similar issues in MEMORY.md
