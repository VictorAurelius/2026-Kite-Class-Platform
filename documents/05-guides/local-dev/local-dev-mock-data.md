# Local dev — AI Branding mock data

How to run the v2 AI Branding flow end-to-end on your laptop without invoking
real AI clients (Ollama / OpenAI / Bedrock). Pairs the FE MSW handlers with
the BE `BrandingDataSeeder` so the wizard demo reaches `DEPLOYED` in seconds.

**Tracking:** GAP-235 Sub-PR G — depends on Sub-PRs E1 (#588), F (#589), E2 (#590) all merged.
**Wave plan:** [`documents/03-planning/waves/wave-mock-data-local-dev.md`](../03-planning/waves/wave-mock-data-local-dev.md) §7

---

## What gets seeded

**Backend** (`kiteclass-core` with `--spring.profiles.active=dev`):

| Entity | Detail |
|--------|--------|
| `FrontendInstance` × 1 | `slug=thanglong`, status `DEPLOYED`, `brandingVersion=1` |
| `BrandingResource` × 3 | LOGO (STATIC), BANNER (TEMPLATE), HERO (FULL_AI) |
| `QualityReport` × 1 | `score=85`, `passed=true`, sub-scores populated |
| `OutboxEvent` × 1 | `branding.updated` event ready for downstream consumers |

Idempotent — boot the app twice, the seeder skips on the second run (slug existence check).

**Frontend** (MSW handlers in `kiteclass-frontend/src/mocks/`):

| Module | Provides |
|--------|----------|
| `ai-branding-state.ts` | In-memory `Map<id, MockFrontendInstance>` mirroring BE state machine, seeded with the same `slug=thanglong` row |
| `ai-branding-handlers.ts` | 11 endpoints (8 lifecycle + branding package + public + internal webhook), `canTransition()` enforcement, ETag/304, `NEXT_PUBLIC_MOCK_DELAY_MS` env override |

The two layers stay in sync: a wizard call hitting the FE mocks behaves the
same way the real BE would once it gets the equivalent request.

---

## Quick start

### 1. Backend on dev profile

```bash
# Boots postgres + redis + rabbitmq + kiteclass-core in Docker
./kiteclass/scripts/dev-up.sh

# OR run kiteclass-core directly with the dev profile (Postgres still required)
cd kiteclass/kiteclass-core
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Look for the seed log line:

```
INFO ... BrandingDataSeeder : Seeded dev branding: instance id=1, slug=thanglong, brandingVersion=1
```

If you see `Dev branding seed already present (slug=thanglong). Skipping.` instead, the seeder previously ran — that's idempotency working as intended. To re-seed, drop the row or reset the dev DB.

### 2. Frontend with mocks (test mode today, browser mode coming in PR B)

For unit + integration tests today:

```bash
cd kiteclass/kiteclass-frontend
pnpm exec vitest run src/mocks/ai-branding-handlers.test.ts
```

For local browser-mode demo (full wizard against MSW), see `kiteclass-frontend/src/mocks/server.ts` and the planned PR B that wires `setupWorker` + the `NEXT_PUBLIC_MOCK_API` toggle.

### 3. Smoke-test the seeded data

```bash
./kiteclass/scripts/smoke-ai-branding-dev.sh
```

Asserts the BE is on dev profile and returns the seeded `thanglong` row.

---

## Demo flow (wave plan §7.5)

Once both sides are up and a wizard page is wired:

1. Login as the seeded tenant admin.
2. Onboarding wizard → "Tạo thương hiệu AI".
3. Wizard steps 1–6 (constrained presets per `.claude/rules/ai-branding-guidelines.md` §2.1).
4. Click Deploy → `POST /api/v1/instances/{id}/infrastructure-ready`.
5. Watch the lifecycle animation: `INITIALIZING` (immediate) → `GENERATING` (1.5s delay, configurable) → `DEPLOYED`.
6. Tenant instance renders with mock theme (CSS variables from `BrandingPackage`).
7. Click Regenerate banner → `POST /api/v1/instances/{id}/rebrand` → `REGENERATING` → `DEPLOYED` (loop).
8. Quality panel shows `score=85` from the seeded `QualityReport`.

### Verifying 0 real AI calls

`MockAIClient` (`kiteclass-core/.../ai/client/MockAIClient.java`) is the Strategy adapter wired in dev. To assert no real Ollama traffic during a demo:

```bash
# In another terminal, watch backend logs
./kiteclass/scripts/dev-status.sh logs core | grep -E 'OllamaClient|llama|http.*11434' || echo "0 real-AI calls detected"
```

If that grep matches anything during the wizard run, real AI escaped the mock — file a regression.

---

## Screenshots (manual capture today; Playwright spec available)

A Playwright spec is in `kiteclass/kiteclass-frontend/e2e/ai-branding-demo.spec.ts` that walks the lifecycle and saves screenshots to `kiteclass/kiteclass-frontend/e2e/screenshots/ai-branding/`. Run manually after both BE and FE are up:

```bash
cd kiteclass/kiteclass-frontend
pnpm exec playwright test e2e/ai-branding-demo.spec.ts
```

Outputs:

| File | State |
|------|-------|
| `01-not-started.png` | Wizard landing, instance just created |
| `02-initializing.png` | After `infrastructure-ready` POST |
| `03-generating.png` | ~1.5s into lifecycle |
| `04-deployed.png` | After `branding-completed` |
| `05-regenerating.png` | After `rebrand` POST |
| `06-failed.png` | After `failed` POST (negative path) |

Capture is skipped in CI (`e2e` excluded from default vitest run).

---

## Endpoints exposed

Per wave plan §7.1, real `kiteclass-core` controllers (mirrored by FE mocks):

| Method | Path | Controller |
|:------:|------|------------|
| POST | `/api/v1/instances` | `InstanceController` (create) |
| GET | `/api/v1/instances/{id}` | `InstanceController` |
| GET | `/api/v1/instances` | `InstanceController` (list) |
| POST | `/api/v1/instances/{id}/infrastructure-ready` | `InstanceController` |
| POST | `/api/v1/instances/{id}/branding-completed` | `InstanceController` |
| POST | `/api/v1/instances/{id}/rebrand` | `InstanceController` |
| POST | `/api/v1/instances/{id}/failed` | `InstanceController` |
| POST | `/api/v1/instances/{id}/retry` | `InstanceController` |
| GET | `/api/v1/branding/{instanceId}/package` | `BrandingPackageController` |
| GET | `/api/v1/branding/public` | `PublicBrandingController` |
| POST | `/internal/notify/instance-deployed` | `InternalWebhookController` |

OpenAPI spec for the full list (134+ paths): regenerate via `kiteclass/shared/scripts/regenerate-openapi.sh` or pull the `kiteclass-core-openapi-spec` artifact from the latest `core-ci.yml` workflow run.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Seeder log doesn't appear | App not on dev profile | `--spring.profiles.active=dev` |
| `Dev branding seed already present` on every boot | Working as intended (idempotent) | Drop the row to re-seed: `DELETE FROM frontend_instances WHERE slug='thanglong';` |
| FE wizard never advances past `INITIALIZING` | Async transition not firing in test env | Set `NEXT_PUBLIC_MOCK_DELAY_MS=0` to fast-forward |
| 422 INVALID_TRANSITION on `infrastructure-ready` | Tenant already DEPLOYED — that's the seeded state | Use a freshly-created instance via `POST /api/v1/instances` |
| Smoke script fails with connection refused | BE not yet ready | Wait ~30s after `dev-up.sh`; check `dev-status.sh` |

---

## Related

- Rule: [`.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md)
- Wave plan: [`documents/03-planning/waves/wave-mock-data-local-dev.md`](../03-planning/waves/wave-mock-data-local-dev.md)
- Gap: [`documents/04-quality/gaps/closed/GAP-235-ai-branding-mock-implementation.md`](../04-quality/gaps/GAP-235-ai-branding-mock-implementation.md)
- Seeder: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/dev/seeder/BrandingDataSeeder.java`
- FE mocks: `kiteclass/kiteclass-frontend/src/mocks/ai-branding-{state,handlers}.ts`
- Smoke script: `kiteclass/scripts/smoke-ai-branding-dev.sh`
