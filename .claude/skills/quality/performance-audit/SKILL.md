---
name: performance-audit
description: "Dùng khi user nói 'perf audit', 'performance check', 'load test', 'kiểm tra hiệu năng', 'N+1', 'bundle size', hoặc trước production deploy. Baseline performance metrics /100."
user-invocable: true
---

# /performance-audit — Performance Baseline Assessment

Score /100. Identify bottlenecks across DB, API, frontend, caching, and resources.

## Process

### 1. Automated Checks

```bash
# DB: N+1 query detection
grep -rn "findAll\|findBy" --include="*.java" kiteclass/kiteclass-core/src/main/ | grep -v test | head -30
grep -rn "@Query" --include="*.java" kiteclass/kiteclass-core/src/main/ | head -20

# FE bundle: Next.js build analysis
cd kitehub/kitehub-frontend && npm run build 2>&1 | tail -30
cd kiteclass/kiteclass-frontend && npm run build 2>&1 | tail -30

# Redis caching (broad scope — catches all submodules)
grep -rn "RedisTemplate\|@Cacheable\|cache" --include="*.java" | grep -v test | grep -v target | head -20

# Resource config (broad scope)
grep -rn "pool-size\|max-connections\|timeout\|thread" --include="*.yml" | grep -v target | head -20
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface performance bombs (N+1, missing pagination, missing bulkhead) BEFORE prod. A `/100` score with hidden P0 bombs is WORSE than a low score listing every bomb honestly.** Per `.claude/rules/audit-skill-rubric-performance-audit.md` §4 (mirror of Wave 71c security-audit primacy pattern).

Rules for every audit run:
1. Enumerate ALL §3 sub-checks per category. NEVER skip "obviously fine."
2. Each sub-check returns: PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`. No partial credit.
3. Final output starts with **bug list** (every FAIL with `file:line` evidence) BEFORE the score.
4. Score is descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. If audit time-budget runs out, mark `❓ UNCHECKED` — do NOT default to PASS.

### 3. Score 5 Categories with per-check rubric

Per Wave 72b Bucket E (GAP-523 closure), every category binds to per-check pass/fail rule.

| # | Category (20pts) | Per-check rubric file |
|---|-----------------|-----------------------|
| 1 | **DB Query Efficiency** | **`.claude/rules/audit-skill-rubric-performance-audit.md` §2.1 (6 sub-checks)** |
| 2 | **API Response Time** | **`.claude/rules/audit-skill-rubric-performance-audit.md` §2.2 (6 sub-checks)** |
| 3 | **Frontend Bundle** | **`.claude/rules/audit-skill-rubric-performance-audit.md` §2.3 (6 sub-checks)** |
| 4 | **Caching Strategy** | **`.claude/rules/audit-skill-rubric-performance-audit.md` §2.4 (6 sub-checks)** |
| 5 | **Resource Utilization** | **`.claude/rules/audit-skill-rubric-performance-audit.md` §2.5 (6 sub-checks)** |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`.
3. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
4. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL.
5. Each FAIL surfaces in bug list per §2 primacy.

Legacy scoring narrative: `reference/scoring-guide.md` retained for backward-compat only.

### 4. Output

Save to `documents/04-quality/audits/performance/performance-audit-[date].md`

## Context Management

Token budget ~15-25K (nhỏ nhất trong audit suite). Chú ý:

1. **Build output** — `npm run build` output có thể 100+ lines. LUÔN `| tail -30` (chỉ cần route sizes table).
2. **Grep N+1** — `| head -30` per module. Đếm occurrences, không list tất cả.
3. **Không chạy load test trong audit** — chỉ static analysis + config review. Load test = separate task.

## Gotchas

- `findAll()` on JPA repo without `@Query` or `Pageable` = potential N+1
- Next.js `next build` output shows route sizes — flag anything >250KB
- Redis config is in `application.yml` under `spring.data.redis`
- KiteClass has Resilience4j bulkhead — check thread pool sizes match
- Docker `deploy.resources.limits` may not be set in dev compose — check k8s/Helm for prod
- Build output có thể rất dài — chỉ lấy summary table cuối cùng
- **Multi-module scope** — `kiteclass/ kitehub/` as grep dirs may miss submodules. Use broad `--include="*.java"` from root OR explicit `kiteclass/*/src/` `kitehub/*/src/` glob. Ref: GAP-149, memory `feedback_audit_grep_scope.md`.
- Always `grep -v target` to exclude compiled `target/classes/` duplicates

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
