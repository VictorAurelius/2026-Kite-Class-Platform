# GAP-235: AI Branding Mock Data Implementation (MSW + DataSeeder)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (mock data unblocks FE dev without backend; not GA-blocking but accelerates Wave 8+ FE work)
**Domain:** Frontend (MSW) / Backend (DataSeeder) / Mock Data
**Detected:** 2026-04-26 (split from GAP-014 implementation portion)
**Related Docs:**
- `documents/03-planning/waves/wave-mock-data-local-dev.md` §7 (v2-aligned planning)
- `documents/04-quality/gaps/GAP-014-wave-mock-include-ai-branding.md` (parent — planning portion DONE)
- `.claude/rules/ai-branding-guidelines.md`

## Current State (verified 2026-04-26)

| Piece | File / Path | Status |
|-------|-------------|--------|
| MSW worker | `kiteclass/kiteclass-frontend/src/mocks/handlers.ts` (501 LOC, 27 handlers) | ✅ Exists for KiteClass core (students/teachers/classes/etc); ❌ 0 AI Branding handlers |
| MSW package | `kiteclass-frontend/package.json` (msw ^2.13.6) | ✅ Installed |
| `NEXT_PUBLIC_MOCK_API` toggle | grep returns 0 hits in kiteclass-frontend/src | ❌ Not implemented |
| `DataSeeder.java` (kiteclass-core) | find returns 0 results | ❌ Not implemented |
| `application-dev.yml` (kiteclass-core) | not present | ❌ Not implemented |
| `MockAIClient.java` | `kiteclass-core/.../ai/client/MockAIClient.java` (1.7K) | ✅ Wave 4 Strategy pattern adapter |
| OpenAPI spec export | grep `springdoc-openapi` in kiteclass-core/pom.xml | ❌ Need verify; currently no `/v3/api-docs` confirmed |

**Grep commands run:**
```bash
find kiteclass kitehub -name "DataSeeder*.java" -o -name "*Seeder*.java" 2>/dev/null | grep -v "/target/\|/node_modules/"
ls kiteclass/kiteclass-core/src/main/resources/application-dev.yml
grep -rln "NEXT_PUBLIC_MOCK_API" kiteclass/kiteclass-frontend/src
grep -nE "branding|wizard|analyze|plan|package|frontend.instance|quality" kiteclass/kiteclass-frontend/src/mocks/handlers.ts
# all return 0 results
```

## Problem

Wave plan `wave-mock-data-local-dev.md` §7 (updated 2026-04-26 GAP-014) describes a v2-aligned mock target (10 endpoints + lifecycle simulation + DataSeeder for 3 entities). None of it shipped yet. FE dev currently has no way to demo full AI Branding flow locally without running real `kiteclass-core` + Postgres + MockAIClient profile.

This is wave-eligible per `feedback_wave_plan_before_serial_prs.md` — ≥3 disjoint sub-tasks (MSW handlers, DataSeeder, OpenAPI export, demo capture).

## Proposed Fix — wave-eligible (4 sub-PRs)

### Sub-PR E1: OpenAPI export from kiteclass-core
**Branch:** `feat/kiteclass-core-openapi-export`
**Scope:**
- Add `springdoc-openapi-starter-webmvc-ui` dependency
- Expose `/v3/api-docs` endpoint
- CI step: dump spec to `kiteclass/shared/openapi-v2.json`
- Verify all 11 v2 controllers (InstanceController + 3 branding controllers) appear with schemas

### Sub-PR E2: FE MSW handlers — v2 AI Branding (10 endpoints)
**Branch:** `feat/fe-mock-ai-branding-v2`
**Dependencies:** E1
**Scope:**
- Add 10 mock handlers to `kiteclass-frontend/src/mocks/handlers.ts`
- In-memory state machine for FrontendInstance lifecycle (6 states)
- Simulated 1-2s delays for transitions
- Mock branding package returns theme + 6 placeholder asset URLs
- Mock QualityReport: score=85 deterministic; toggle flag for <70 fail scenario

### Sub-PR F: BE DataSeeder for v2 entities
**Branch:** `feat/be-dataseeder-branding-v2`
**Scope:**
- Create `kiteclass-core/src/main/resources/application-dev.yml`
- `@Profile("dev") @Component DataSeeder implements CommandLineRunner`
- Seed: 1 FrontendInstance (DEPLOYED, brandingVersion=1) + 3 BrandingResources (one per category) + 1 QualityReport (score=85, 5 mock issues) + 1 OutboxEvent (`branding.updated`)
- Skip `branding_templates` until GAP-011 lands ImageTemplate entity
- Idempotent: check `count()` before seed
- Respect FK order: Instance → Resource → Report → OutboxEvent

### Sub-PR G: Demo flow capture + integration smoke
**Branch:** `docs/ai-branding-demo-mock`
**Dependencies:** E2 + F
**Scope:**
- Run wave plan §7.5 demo flow end-to-end with mocks
- Capture screenshots of each lifecycle state (NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED)
- Verify 0 Ollama calls via log assertion
- Update `documents/05-guides/local-dev-mock-data.md` with v2 AI Branding section
- Smoke test script: start kiteclass-core `--spring.profiles.active=dev`, verify seed data loads, hit /api/v1/instances → returns seeded instance

## Acceptance Criteria

- [ ] OpenAPI spec exported from kiteclass-core covers v2 endpoints
- [ ] 10 v2 endpoints mocked in MSW (per wave plan §7.1 inventory)
- [ ] Lifecycle 6-state transitions simulated với realistic delays
- [ ] DataSeeder seeds 1 sample DEPLOYED instance + 3 BrandingResources + 1 QualityReport + 1 OutboxEvent
- [ ] Demo flow runs end-to-end without `OllamaClient` invocation (log assertion: 0 Ollama calls)
- [ ] Screenshots captured all 6 lifecycle states
- [ ] Smoke test: `--spring.profiles.active=dev` → seed loads → API returns seeded data
- [ ] No regressions in existing 27 KiteClass core MSW handlers

## Out-of-scope (track separately)

| Item | Linked gap |
|------|-----------|
| `ImageTemplate` entity + 30 template seeds | GAP-011 |
| Regenerate counter UI + tier limits | GAP-005 Phase 2 |
| Real wizard draft persistence (server-side) | GAP-020 |
| `quality-reports/{id}` REST endpoint | GAP-012 follow-up |
| Approval per resource REST | GAP-070 placeholder |
| Real model swap (Gemma 4 9B) | GAP-006 |
| Full 71→100% MSW coverage for KiteClass core | original wave PR B (separate gap if needed) |

## Related

- **Parent:** GAP-014 (planning portion DONE 2026-04-26)
- **Wave plan:** `documents/03-planning/waves/wave-mock-data-local-dev.md` §7
- **Architecture drift context:** GAP-016 (closed) + GAP-234 (filed) — module-location notes
- Memory: `feedback_wave_plan_before_serial_prs.md` (≥3 disjoint sub-tasks → wave + parallel agents)
- Rule: `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 (state-check completed above)

## Log

- **2026-04-26** — Filed during GAP-014 planning portion closure. State-check confirmed 0 v2 mock handlers + 0 DataSeeder + 0 dev profile config. Wave-eligible (4 sub-PRs disjoint files). P1 not P0 because doesn't block GA — just accelerates FE dev. Defer execution until next FE-focused wave.
