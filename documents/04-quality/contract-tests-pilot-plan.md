# FE↔BE Contract Tests — Phase 1 Pilot Plan

> Last updated: 2026-04-20 (GAP-198 Phase 1) | Owner: FE Lead + QA Lead
> Governing ADR: [`../02-architecture/adr/ADR-016-fe-be-contract-strategy.md`](../02-architecture/adr/ADR-016-fe-be-contract-strategy.md)
> Producer-side reference: `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/contract/InstanceApiContractTest.java` (GAP-090 DONE)

Deliverable: 3 endpoints covered by consumer-side contract tests; CI fails on breaking schema drift; `api-contract-audit` skill reads report. Expected effort: 1 sprint (2 weeks).

---

## 1. Scope

3 pilot endpoints chosen for:
- **High traffic** (exercised by most tenant sessions)
- **Representative shape** (simple GET, paginated GET, POST with array payload)
- **Two different services** (core + gateway)

| # | Endpoint | Service | Owner handler (FE) | Shape sample |
|:-:|----------|---------|---------------------|--------------|
| 1 | `POST /api/v1/auth/login` | kiteclass-core via gateway | `kiteclass-frontend/src/mocks/handlers.ts` | `{accessToken, refreshToken, user}` envelope |
| 2 | `GET /api/v1/classes?page=&size=` | kiteclass-core | `kiteclass-frontend/src/mocks/handlers.ts` | Paginated `{content[], page, size, totalElements}` |
| 3 | `POST /api/v1/attendance/submit` | kiteclass-core | `kiteclass-frontend/src/mocks/handlers.ts` | `{classId, date, records[]}` request + `{success, savedCount}` response |

Out of scope for Phase 1:
- kitehub-frontend handlers (Phase 2)
- Websocket/SSE contracts (Phase 2)
- Semantic constraints (value-ranges, required-if) — Phase 2 via Pact if needed

---

## 2. Architecture

```
┌──────────────────────┐     ┌──────────────────────┐
│ kiteclass-frontend   │     │  kitehub services    │
│                      │     │                      │
│  MSW handlers.ts     │     │  Springdoc           │
│        │             │     │  /v3/api-docs        │
│        ▼             │     │        │             │
│  extract fixtures    │     │        ▼             │
│  __contracts__/*.json│     │  openapi-{svc}.json  │
└──────────┬───────────┘     └──────────┬───────────┘
           │                            │
           └─────────┬──────────────────┘
                     ▼
              ┌──────────────┐
              │  oasdiff     │   ← CI job
              │  schema diff │
              └──────┬───────┘
                     │
                     ▼
          ┌──────────────────────┐
          │ .ci/contract-diff-   │
          │ report.md            │
          └──────────┬───────────┘
                     │
                     ├───► GitHub Actions status (fail on breaking)
                     │
                     └───► api-contract-audit skill /100 subcategory 5
```

---

## 3. Implementation Steps

### 3.1 MSW fixture extraction (FE side)

**File:** `kiteclass-frontend/src/mocks/__contracts__/extract.ts` (new)

```typescript
// Pseudo-code — actual impl can use a Vitest plugin or script
import { handlers } from '../handlers';

/**
 * Walk each handler, POST a dummy request, capture the shape of the response body.
 * Emits one JSON file per endpoint under __contracts__/
 */
async function extractAll() {
  for (const h of handlers) {
    const shape = await probeResponseShape(h);
    writeFixture(h.info.path, h.info.method, shape);
  }
}
```

Output:
```
kiteclass-frontend/src/mocks/__contracts__/
├── POST__api_v1_auth_login.json
├── GET__api_v1_classes.json
└── POST__api_v1_attendance_submit.json
```

Each fixture contains JSON Schema inferred from the MSW mock response (types + required fields, no values).

### 3.2 BE OpenAPI spec download (CI)

**File:** `.github/workflows/contract-check.yml` (new step)

```yaml
- name: Extract BE OpenAPI
  run: |
    mkdir -p .ci/openapi
    docker compose -f kitehub/docker-compose.kitehub.yml up -d kitehub-gateway kiteclass-core
    until curl -sf http://localhost:8080/v3/api-docs -o .ci/openapi/gateway.json; do sleep 2; done
    curl -sf http://localhost:8081/v3/api-docs -o .ci/openapi/kiteclass-core.json
```

### 3.3 Schema diff (CI)

**File:** `.github/workflows/contract-check.yml`

```yaml
- name: Install oasdiff
  run: go install github.com/oasdiff/oasdiff@latest

- name: Run contract diff
  run: |
    oasdiff breaking \
      --base .ci/openapi/kiteclass-core.json \
      --revision kiteclass/kiteclass-frontend/src/mocks/__contracts__/merged.json \
      --format markdown > .ci/contract-diff-report.md
    if grep -q "breaking" .ci/contract-diff-report.md; then
      echo "BREAKING schema drift detected"
      exit 1
    fi
```

Note: `oasdiff` expects two OpenAPI specs. If MSW fixtures are JSON Schema (not full OpenAPI), wrap them first into a synthetic OpenAPI doc. See §3.5.

### 3.4 Merge fixtures → synthetic OpenAPI

Small Node script `kiteclass-frontend/scripts/fixtures-to-openapi.ts`:

```typescript
// Read __contracts__/*.json → emit merged.json conforming to OpenAPI 3.0
// Uses the MSW URL + method as paths entry; response JSON Schema as content schema
```

### 3.5 `api-contract-audit` skill hook

Skill updated to read `.ci/contract-diff-report.md` when scoring:
- File absent → subcategory 5 scored 0/20 (not measured)
- File present, 0 breaking → 20/20
- Breaking changes:
  - 1-2 → 10/20
  - 3-5 → 5/20
  - >5 → 0/20

Skill SKILL.md update is part of Phase 1 deliverable (reference section).

---

## 4. CI Integration

### 4.1 Workflow trigger
- On PR touching `kiteclass-frontend/src/mocks/**` OR `kiteclass-core/src/main/**Controller.java`
- On main merge (post-merge baseline)
- Nightly job for drift detection

### 4.2 Failure modes
- **Breaking change** → PR red; blocked until either FE mock or BE schema updated
- **Additive only** → PR green with warning comment
- **Tool failure** (network, build) → PR red with "contract check infra failure — rerun"; retry once

### 4.3 Report location
`.ci/contract-diff-report.md` — uploaded as CI artifact for 30d

---

## 5. Acceptance Criteria (Phase 1)

- [ ] 3 pilot endpoints have `__contracts__/*.json` fixtures auto-generated
- [ ] CI workflow runs schema diff on every FE-touching PR
- [ ] Breaking change in any of the 3 endpoints fails CI
- [ ] `api-contract-audit` skill reads `.ci/contract-diff-report.md` and scores subcategory 5
- [ ] `output-review-mandate.md` §3 retains ⚠️ PARTIAL until Phase 2 full coverage, but gains link to Phase 1 evidence
- [ ] ADR-016 linked from api-contract-audit skill

---

## 6. Timeline

| Week | Task | Owner |
|:----:|------|-------|
| 1 | Fixture extraction script (FE) | FE Lead |
| 1 | OpenAPI download workflow step | DevOps |
| 2 | `oasdiff` integration + synthetic OpenAPI merge | FE Lead |
| 2 | `api-contract-audit` skill update | QA Lead |
| 2 | Pilot 3 endpoints end-to-end; red-green test | FE + QA |

Gate to Phase 2 launch: 2 sprints (4 weeks) of stable CI signal.

---

## 7. Phase 2 Expansion (deferred)

After Phase 1 stable for ≥1 quarter:
- All `kiteclass-frontend` routes (est. ~40 endpoints)
- All `kitehub-frontend` routes (est. ~25 endpoints)
- Decide: stay on schema diff OR escalate to Pact (ADR-016 revisit)
- `output-review-mandate.md` §3 promotion from ⚠️ PARTIAL → ✅ DONE

---

## 8. Risks + Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|:----------:|:------:|------------|
| Fixture extraction misinfers type (e.g. number vs string) | Medium | Medium | Manual override via `@contract-type` JSDoc comment |
| BE `/v3/api-docs` endpoint lags schema changes | Low | High | Add CI step to verify Springdoc freshness (`lastModified` header) |
| `oasdiff` false positives on cosmetic changes | Medium | Low | Configure `--ignore-title`, `--ignore-description` flags |
| Gateway composite endpoints (aggregation) not in single BE spec | High | Medium | Document aggregation layer separately; Phase 2 scope |
| FE mock for paginated endpoint differs from real pagination | Medium | Medium | Pilot endpoint #2 (`GET /classes`) explicitly covers this |

---

## 9. Related

- [`../02-architecture/adr/ADR-016-fe-be-contract-strategy.md`](../02-architecture/adr/ADR-016-fe-be-contract-strategy.md)
- [`gaps/GAP-198-fe-be-mock-contract-tests.md`](gaps/GAP-198-fe-be-mock-contract-tests.md)
- [`gaps/GAP-090-api-contract-tests.md`](gaps/GAP-090-api-contract-tests.md) (DONE — producer-side)
- `.claude/skills/quality/api-contract-audit/SKILL.md`
- `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/contract/InstanceApiContractTest.java`
- External: [oasdiff](https://github.com/oasdiff/oasdiff), [OpenAPI](https://spec.openapis.org/)

---

## 10. Log

- **2026-04-20:** Created (GAP-198 Phase 1 — ADR + pilot plan). Phase 2 implementation deferred.
