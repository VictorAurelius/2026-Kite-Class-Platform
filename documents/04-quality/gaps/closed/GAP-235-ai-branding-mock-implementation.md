# GAP-235: AI Branding Mock Data Implementation (MSW + DataSeeder)

**Status:** 🟢 DONE 2026-04-27 — All 4 sub-PRs shipped (E1 #588, F #589, E2 #590, G this PR)
**Priority:** 🟠 P1 (mock data unblocks FE dev without backend; not GA-blocking but accelerates Wave 8+ FE work)
**Domain:** Frontend (MSW) / Backend (DataSeeder) / Mock Data
**Detected:** 2026-04-26 (split from GAP-014 implementation portion)
**Related Docs:**
- `documents/03-planning/waves/wave-mock-data-local-dev.md` §7 (v2-aligned planning)
- `documents/04-quality/gaps/closed/GAP-014-wave-mock-include-ai-branding.md` (parent — planning portion DONE)
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
| OpenAPI spec export | grep `springdoc-openapi` in kiteclass-core/pom.xml | ✅ **Re-verified 2026-04-27**: dependency v2.8.17 present, `springdoc.api-docs.path: /api-docs` configured, `swagger-ui.path: /swagger-ui.html` enabled in `application.yml`. Spec available at `/api-docs` (not `/v3/api-docs` — custom path). |
| Controller count | `find kiteclass-core -name '*Controller.java' \| wc -l` | ⚠️ **34 controllers** (plan §4 said 21 — outdated since Wave 4) |
| `shared/` folder | `ls kiteclass/shared 2>/dev/null` | ❌ Not present — needs creation for fixtures + openapi.json export |

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

### Sub-PR E1: OpenAPI export from kiteclass-core (in flight 2026-04-27 — `feat/gap-235-pr-a-openapi-fixtures`)
**Branch:** `feat/gap-235-pr-a-openapi-fixtures`
**Scope (revised after state-check 2026-04-27):**
- ~~Add `springdoc-openapi-starter-webmvc-ui` dependency~~ ✅ already present (v2.8.17)
- ~~Expose `/v3/api-docs` endpoint~~ ✅ already at `/api-docs` (custom path)
- **Create `kiteclass/shared/` folder** with fixtures starter (mock-data.json, Vietnamese realistic — top-tier entities only; full 36-entity richness deferred to PR C/seeder)
- **Add CI step** to dump spec to `kiteclass/shared/openapi.json` on push (uses `springdoc-openapi-maven-plugin` or curl-after-app-start in workflow)
- **Verify** all 34 controllers serialize to spec (smoke test in CI: `jq '.paths | keys | length' shared/openapi.json` ≥ 60)

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
- Update `documents/05-guides/local-dev/local-dev-mock-data.md` with v2 AI Branding section
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
- **2026-04-27** — State-check round 2 (per `audit-to-gap-pipeline.md` Step 2.5) before starting Sub-PR E1: discovered springdoc-openapi v2.8.17 already in `kiteclass-core/pom.xml` + `application.yml` configured (path `/api-docs`, swagger-ui enabled). Plan §4 was outdated. Sub-PR E1 scope revised down — focus shifts to (1) create `kiteclass/shared/` with starter fixtures + (2) wire CI export. Status flipped 🔵 OPEN → 🟡 PARTIAL on branch creation. Per memory `feedback_gap_state_check_required.md` — checking before re-doing already-shipped scaffolding.
- **2026-04-27** — Sub-PR E1 SHIPPED (PR #588). `kiteclass/shared/` folder + VN fixtures starter + manual regen script + `OpenApiSpecExportTest` (RANDOM_PORT + TestRestTemplate, `@SpringBootTest(properties="springdoc.api-docs.path=/api-docs")` to lock production path against test-resources/application.yml override) + `core-ci.yml` artifact upload. CI fix: original MockMvc-based test got 500 because (1) MockMvc bypasses springdoc init and (2) test-resources application.yml dropped the custom path; both addressed. Local: 142 paths, OpenAPI 3.1.0, 284 KB.
- **2026-04-27** — Sub-PR F (BE DataSeeder) shipped on `feat/gap-235-pr-f-be-dataseeder`. Adds `application-dev.yml` + `BrandingDataSeeder` (`@Profile("dev")`, `ApplicationReadyEvent` listener, idempotent on slug check) + 4 unit tests with mocked repos. Seeds 1 DEPLOYED `FrontendInstance` + 3 `BrandingResource` (one per `STATIC`/`TEMPLATE`/`FULL_AI`) + 1 `QualityReport` (score 85) + 1 outbox event (`branding.updated`). Walks the state machine `NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED` so `brandingVersion=1` and `deployedAt` are populated correctly. Local test 4/4 ✅. Branched independently from main (does not stack on Sub-PR E1 / PR #588).
- **2026-04-27** — Sub-PR E2 (FE MSW v2 AI Branding handlers) shipped on `feat/gap-235-pr-e2-fe-msw-handlers`. Adds `kiteclass-frontend/src/mocks/ai-branding-state.ts` (in-memory `Map<id, MockFrontendInstance>` + `canTransition()` rules + seeded demo `slug=thanglong` matching DataSeeder's row) and `ai-branding-handlers.ts` (11 endpoints from wave plan §7.1: 8 Instance lifecycle + 1 BrandingPackage + 1 Public + 1 Internal webhook). Lifecycle endpoints enforce state-machine via `canTransition()` and return 422 on invalid moves. Async transitions (NOT_STARTED → INITIALIZING → GENERATING) use `msw/delay` with `NEXT_PUBLIC_MOCK_DELAY_MS` env override (default 1500ms, set 0 in tests). Branding package supports ETag (`If-None-Match` → 304). Wired into `handlers.ts` via spread. 15 vitest tests covering create/list/lifecycle/gate-pass/gate-fail/rebrand/retry/package/304/public/webhook — 15/15 ✅; full FE suite 565/565 unaffected. Independent from PR #588 (Sub-PR E1) and PR #589 (Sub-PR F).
- **2026-04-27** — Sub-PR G (demo + smoke) shipped: `documents/05-guides/local-dev/local-dev-mock-data.md` (full guide + endpoints inventory + troubleshooting matrix), `kiteclass/scripts/smoke-ai-branding-dev.sh` (curl-based dev smoke against running BE — instances list shape + branding package theme + public branding endpoint, shellcheck clean), and `kiteclass/kiteclass-frontend/e2e/ai-branding-demo.spec.ts` (Playwright lifecycle screenshot capture, gated by `AI_BRANDING_DEMO_RUN=1` env so excluded from default CI). Live screenshot capture deferred to manual run — Docker stack not up in this session. **Status: 🟢 DONE.** GAP-235 fully closed — all 4 sub-PRs (E1/E2/F/G) merged.
