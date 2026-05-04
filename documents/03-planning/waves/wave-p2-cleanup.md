---
title: Wave P2-Cleanup — close 3 follow-up gaps from session 2026-04-26/27
status: in_progress
created: 2026-04-27
updated: 2026-04-27
gaps: [GAP-234, GAP-236, GAP-237]
parent_session: 20260426-164325
---

# Wave P2-Cleanup — 3 parallel agents

**Status:** 🟠 IN_PROGRESS
**Trigger:** Session 2026-04-26/27 closed 8 P0/P1 gaps + filed 7 P2 follow-ups. Cluster 3 of those P2s into a parallel-agent cleanup wave.
**Strategy:** 3 `isolation: worktree` agents; lead owns ROADMAP consolidation.

## Wave-eligibility (Step 0)

| Q | Answer |
|---|--------|
| ≥3 sub-tasks? | ✅ YES — 3 gaps, distinct domains |
| Disjoint files? | ✅ YES — docs (234) / FE+frontend-ci (236) / kitehub-admin AMQP (237) |
| Self-contained TDD/build? | ✅ YES — each agent runs own module pipeline |

**Excluded from this wave:**
- GAP-239 P2 (API SLO coverage completion) — scope creep risk; touches 13 controllers across 5 modules + PR template + skill enforcement; defer to dedicated follow-up
- GAP-006 — infra-blocked
- GAP-005, GAP-011, GAP-144 — infra/designer-blocked

## Agent assignments

### Agent A — GAP-234 architecture/diagram drift sync
**Branch:** `feature/wave-p2-A-gap-234-arch-diagram-drift`
**Effort:** M (1-2h)
**Files (exclusive):**
- `documents/02-architecture/ai-branding-v2-redesign.md` (module location + class renames)
- `documents/02-architecture/docker-platform-architecture.md` (append §AI Branding v2 section)
- `documents/03-planning/database/database-design.md` (add 6+ v2 entities to ERD section)
- `documents/06-diagrams/plantuml/03-erd.puml` (add v2 KiteClass-side AI Branding tables)
- `documents/06-diagrams/plantuml/04-architecture-full.puml` (add Analyzer/Planner/Executor + Quality components)
- `documents/06-diagrams/plantuml/14-ai-branding-pipeline.puml` (rewrite — replace GPT-4/DALL-E with Ollama llama3.1+llava)
- `documents/06-diagrams/plantuml/16-database-schema-full.puml` (add v2 tables)
- Generated `architecture-full.png/.svg` + `erd.png/.svg` (regenerate from PUML)
- `documents/04-quality/gaps/GAP-234-*.md` (Status + Log only)

**Out-of-bounds:** any code file, any other gap files, ROADMAP.md, MEMORY.md
**Acceptance:** §AC of GAP-234; PUML files validate via `plantuml -checkonly` (or `python -c` YAML alternative)

### Agent B — GAP-236 FE bundle budget CI guardrail (foundation only)
**Branch:** `feature/wave-p2-B-gap-236-fe-bundle-budget`
**Effort:** S-M (1h) — **CI guardrail piece ONLY**, NOT page conversions
**Files (exclusive):**
- `.github/workflows/frontend-ci.yml` (add bundle-size step)
- `.github/workflows/kitehub-frontend-ci.yml` (add bundle-size step)
- NEW: `kiteclass/kiteclass-frontend/scripts/check-bundle-budget.mjs` (or similar; reads `.next/build-manifest.json`, fails if route > threshold)
- NEW: `kitehub/kitehub-frontend/scripts/check-bundle-budget.mjs` (mirror)
- `kiteclass/kiteclass-frontend/package.json` (add `analyze` script if missing)
- `kitehub/kitehub-frontend/package.json` (mirror)
- `documents/04-quality/gaps/GAP-236-*.md` (Status — likely flips to 🟡 PARTIAL since 44 page conversions deferred)

**Out-of-bounds:**
- Any backend file
- Any docker-compose or helm file
- Any actual page conversion (those are scope of leftover-44 work, NOT this wave)
- pnpm-lock.yaml beyond what `pnpm install` regenerates

**Scope cap:** CI guardrail + threshold defaults (250KB First Load JS) only. Bundle analyzer baseline reports also out-of-scope (separate discipline).

**Acceptance:**
- CI fails if any route's First Load JS > 250KB threshold (configurable via env var)
- Threshold tunable per route via config file
- Test PR (intentionally bloat one page) → CI red as expected
- Document threshold + override pattern in `documents/05-guides/monitoring/frontend-bundle-budget.md` (NEW)

### Agent C — GAP-237 admin Outbox-based cache invalidation
**Branch:** `feature/wave-p2-C-gap-237-admin-outbox-amqp`
**Effort:** M (1-2h)
**Files (exclusive):**
- `kitehub/kitehub-admin/pom.xml` (add `spring-boot-starter-amqp` dep)
- NEW: `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/event/RabbitListenerConfig.java` (queue/exchange/binding declarations)
- NEW: `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/event/CrossServiceCacheInvalidationListener.java` (`@RabbitListener` adapter calling existing `AdminCacheInvalidationListener.handle()`)
- `kitehub/kitehub-admin/src/main/resources/application.yml` (RabbitMQ host/queue config)
- `kitehub/kitehub-admin/src/main/resources/application-test.yml` (RabbitMQ disabled in test profile)
- NEW: `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/event/CrossServiceCacheInvalidationListenerTest.java`
- `documents/04-quality/gaps/GAP-237-*.md` (Status + Log only)

**Out-of-bounds:**
- kitehub-subscription's RabbitConfig (parent owns; agent must NOT modify)
- Any docker-compose file
- Any frontend file
- Any other module's application.yml
- ROADMAP.md, MEMORY.md (parent owns)

**Acceptance:**
- AMQP dep present
- `@RabbitListener` on `kitehub.events.exchange` for keys `instance.*`, `subscription.*`
- Admin caches evict within 1s of cross-service Outbox event (verified via integration test using Testcontainers RabbitMQ OR `@MockBean` — agent's choice based on existing test patterns in subscription module)
- No regression on existing in-process Spring ApplicationEvent path
- Subscription full suite 355/355 + admin full suite 23/23 still pass

## Hard rules (per `feedback_parallel_agent_strategy.md`)

1. **Worktree path discipline** — agents work in `.claude/worktrees/agent-*`; do NOT write to main repo working copy
2. **Lead owns shared files** — ROADMAP.md, MEMORY.md, .claude/rules/output-review-mandate.md
3. **Each agent updates only own GAP file** Log + Status
4. **No new gaps filed by agents** — return findings; parent files follow-ups
5. **Migration version slots** — none required (no DB changes)
6. **Test-profile escape hatch** — Agent C must ensure `application-test.yml` excludes RabbitAutoConfiguration so admin's existing tests don't break

## Consolidation (parent post-merge)

After all 3 PRs merged:
1. Update ROADMAP.md Current Status Snapshot
2. Mark GAP-234/237 → DONE; GAP-236 → PARTIAL (CI guardrail only; page conversions remain)
3. Update Block GA tier counts
4. File optional follow-up gaps if agents return findings
