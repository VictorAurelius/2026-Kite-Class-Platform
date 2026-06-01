---
title: Wave tenant-domain-1 — Host→Tenant→Landing resolution cluster fix
status: draft
created: 2026-06-01
updated: 2026-06-01
tag_primary: tenant-domain
tags_secondary: [security, multi-tenant]
counter: 1
waves: []
gaps: [GAP-811, GAP-812, GAP-813, GAP-814]
---

# Wave tenant-domain-1 — Host→Tenant→Landing resolution cluster fix

**Goal:** Close 4-gap cluster (GAP-811/812/813/814) surfaced bởi Wave thesis-3 demo-trio RST walk + 3-agent outside-in audit 2026-05-29 — fix host-based tenant resolution end-to-end (gateway header strip + slug→UUID endpoint + FE middleware + custom domain scaffold).

**Trigger:** GAP-814 P0 security (cross-tenant IDOR via spoofed `X-Tenant-Id`) là single blocker; 3 sibling gaps (P1/P1/P2) cùng concern map độc lập theo layer → ideal cluster cho parallel wave-pack.

**Estimated wall-clock:** ~3-4h coordinator-inline; longest-bucket Bucket C FE middleware ~2h (Playwright host-header simulation breadth).

---

## 1. Brainstorm

**Q1 (alignment):** Phục vụ mọi tenant landing flow (P2 Center Owner + P3 Med Center + Anonymous visitor). Demo-trio (Sky Education + 2 sister tenants) đang block thesis evidence khi multi-tenant landing flaky. Cluster cũng phục vụ Phase 1 BETA invite cohort — mỗi tenant link `kitehub.me/?tenant=sky` phải resolve đúng.

**Q2 (trade-offs):**
- **Alternative A:** Sequential bucket order (B → C → D → A). Rejected — GAP-814 P0 security độc lập, không phụ thuộc nên parallel-first đúng hơn.
- **Alternative B:** Fold GAP-812 (P2 custom domain) sang wave sau. Rejected — gap đã filed PR #1967 với scope rõ, infra ACM cert scaffold parallel được, không block ai.
- **Alternative C:** Skip Bucket 0 Foundation (đẩy api-contract update vào Bucket B). Rejected — Bucket C FE middleware cần stub trên contract trước khi B endpoint xong; foundation-first cho cross-layer per `contract-first-for-cross-layer.md`.

**Q3 (risks):**
- Bucket A gateway strip có thể break existing flow nếu route nào đang trust client `X-Tenant-Id` (legitimate). Mitigation: state-check grep tất cả call sites + JWT-claim fallback giữ trust path.
- Bucket B endpoint cần authentication exempt (public lookup) — phải align gateway whitelist + Spring Security config + rate limit (anonymous brute-force enum slug). Mitigation: rate-limit 30/min/IP per slug-resolve endpoint.
- Bucket C middleware ảnh hưởng mọi request FE — performance budget < 10ms per resolve. Mitigation: in-memory cache + revalidate every 5min.
- Bucket D ACM cert apply chạm production AWS — defer apply (terraform plan only) per `release-deploy-standard.md` §9 dev-trigger.
- GAP-808 (sister DONE 2026-05-29) đã ship landing branding chain — Bucket B + C phải NOT regress public landing path.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | Foundation (api-contract.md + MSW handler) | bg-agent | est. 30min | ✅ docs + FE test fixture |
| A | GAP-814 | bg-agent Opus | est. 1.5h | ✅ kitehub-gateway only |
| B | GAP-813 | bg-agent Opus | est. 2h | ✅ kitehub-subscription module/tenant only |
| C | GAP-811 | bg-agent Opus | est. 2h | ✅ kitehub-frontend middleware + lib/api only |
| D | GAP-812 | bg-agent Opus | est. 2h | ✅ infrastructure/terraform-aws + V## migration only |

Disjoint check: 4 buckets touch 4 distinct service trees (gateway / subscription BE / kitehub-frontend / infra). Foundation Bucket 0 ships docs + test fixtures only (no service code). Zero file overlap predicted.

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **HIGH** (P0 security + customer-facing landing + cross-tenant IDOR risk) → model: **Opus 4.7 full** all buckets

**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** **YES** (Bucket B BE endpoint consumed by Bucket C FE middleware) → **Bucket 0 Foundation required** per `contract-first-for-cross-layer.md`

> **Gap referencing convention:** Status verified via `bash scripts/query-gaps.sh GAP-81N` — 4 gaps OPEN as of 2026-06-01.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | (contract + MSW infra) | 🟠 P1 | `documents/01-business/marketing/api-contract.md` + `kitehub/kitehub-frontend/src/test/msw/handlers/tenant.ts` | MERGE FIRST |
| 1 | **A** | GAP-814 | 🔴 P0 | `kitehub/kitehub-gateway/src/main/{java,resources}/` | parallel after Bucket 0 |
| 2 | **B** | GAP-813 | 🟠 P1 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/{module/tenant,api/controller}/` + `kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql` | parallel after Bucket 0 |
| 3 | **C** | GAP-811 | 🟠 P1 | `kitehub/kitehub-frontend/src/middleware.ts` (NEW) + `kitehub/kitehub-frontend/src/lib/tenant/` (NEW) + `kitehub/kitehub-frontend/src/test/e2e/` | parallel after Bucket 0 |
| 4 | **D** | GAP-812 | 🟡 P2 | `infrastructure/terraform-aws/acm-*.tf` (NEW) + `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/module/domain/` (NEW) + `kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql` | parallel after Bucket 0 |

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.0:
- Files: `documents/01-business/marketing/api-contract.md` (UPDATE — add §Public Tenant Resolve endpoint contract) + `kitehub/kitehub-frontend/src/test/msw/handlers/tenant.ts` (NEW — MSW handler stub returning `{id, subdomain, name, status}` shape)
- Acceptance: api-contract.md lists `GET /api/v1/public/tenants/by-subdomain/{slug}` with request/response/error schemas; MSW handler consumable by Bucket C FE tests
- Spawn order: MERGE FIRST trước khi spawn A/B/C/D parallel

### Bucket A — GAP-814 Gateway header strip + JWT verify

- Files: `kitehub/kitehub-gateway/src/main/resources/application.yml` (`default-filters` add `RemoveRequestHeader X-Tenant-Id`, `X-User-Id`) + `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/` (TenantHeaderGuardFilter NEW — verify JWT signature trước khi inject X-Tenant-Id từ claim) + `kitehub/kitehub-gateway/src/test/java/com/kitehub/gateway/filter/` (integration test: spoofed header → 403; JWT-claim path → 200)
- Tests: TenantHeaderGuardFilterTest + IT spoof rejection
- Acceptance: Per GAP-814 §AC — (1) gateway strip client header before route; (2) JWT signature verified before claim trust; (3) IT spoof scenario returns 403 not cross-tenant data

### Bucket B — GAP-813 Public slug→UUID endpoint + base-domain config

- Files: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/api/controller/PublicTenantController.java` (NEW) + `module/tenant/service/TenantLookupService.java` (NEW) + `InstanceRepository` add `findBySubdomainAndStatus` query + `application.yml` add `kite.platform.base-domain` config + `documents/02-architecture/domain-strategy.md` UPDATE base-domain section + V## migration add subdomain index nếu chưa có (state-check confirm `idx_instances_subdomain` đã có → skip migration)
- Tests: PublicTenantControllerTest (Mockito unit) + PublicTenantIT (Testcontainers Postgres + 4-row tenant fixture verify slug→UUID resolve + 404 unknown slug + 410 suspended)
- Cross-layer BE bucket: Controller signature + DTO match `documents/01-business/marketing/api-contract.md` schema (Bucket 0 output)
- Acceptance: Per GAP-813 §AC — endpoint returns `{id, subdomain, name, status}` for active tenant; 404 for unknown; 410 for suspended; rate-limited 30/min/IP

### Bucket C — GAP-811 FE middleware host→tenant resolver

- Files: `kitehub/kitehub-frontend/src/middleware.ts` (NEW per Next.js 15 convention) + `kitehub/kitehub-frontend/src/lib/tenant/resolveTenant.ts` (NEW — caller for Bucket B endpoint) + `kitehub/kitehub-frontend/src/lib/tenant/tenantCache.ts` (NEW — 5min in-memory) + `kitehub/kitehub-frontend/src/test/e2e/host-tenant-resolution.spec.ts` (NEW — Playwright host header simulation 5 scenarios)
- Tests: Vitest unit cho resolveTenant + tenantCache + Playwright E2E 5 scenarios (valid host / unknown host / suspended / preview mode / BE-down graceful 404)
- Cross-layer FE bucket: Endpoint consumption tuân thủ schema trong `documents/01-business/marketing/api-contract.md` (Bucket 0 output)
- Acceptance: Per GAP-811 §AC — middleware reads Host header → slug-resolve via Bucket B endpoint → inject `x-tenant-id` header → server component receives + renders per-tenant landing; graceful fallback 404/suspended/BE-down

### Bucket D — GAP-812 Custom domain DNS verify + ACM cert scaffold

- Files: `infrastructure/terraform-aws/acm-tenant-domains.tf` (NEW — ACM cert module với DNS validation) + `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/module/domain/` (NEW — DomainVerifyController + DomainVerifyService + DomainStatus enum) + `kitehub/kitehub-subscription/src/main/resources/db/migration/V##__custom_domain_verify_columns.sql` (note: columns đã có trong Instance entity — state-check confirm migration thực sự cần backfill index/constraint thêm gì không) + `documents/05-guides/operations/custom-domain-verify-runbook.md` (NEW)
- Tests: DomainVerifyServiceTest + integration test verify flow (issue token → DNS TXT mock → poll status → ACTIVE)
- Acceptance: Per GAP-812 §AC — owner submits custom domain → verify TXT token issued → background poll DNS → cert provisioned via ACM (terraform plan only, apply deferred Wave next per `release-deploy-standard.md` §9 dev-trigger)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-811` / `GAP-812` / `GAP-813` / `GAP-814` | Gap files | `ls documents/04-quality/gaps/phase-1-beta/GAP-81{1,2,3,4}-*.md` | 4 files | ✅ exists |
| `Instance.subdomain` | Java field | `grep "private String subdomain" kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/Instance.java` | 1 match (UNIQUE NOT NULL length=50, `idx_instances_subdomain`) | ✅ exists |
| `Instance.customDomain` + `Instance.domainVerifyToken` + `Instance.domainVerifiedAt` + `Instance.domainStatus` | Java fields | `grep -E "custom_domain\|domain_verify_token\|domain_verified_at\|domain_status" kitehub/kitehub-platform/.../Instance.java` | 4 matches | ✅ exists |
| `InstanceRepository` | Spring Data interface | `find . -name "InstanceRepository.java" -not -path "*/test/*"` | 2 files (gateway + subscription) | ✅ exists |
| `SecurityHeadersFilter` + `KeyResolverConfig` + `RateLimitConfig` | Gateway filter/config | `ls kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/{filter,config}/` | All 3 present | ✅ exists |
| Public route precedent `/api/v1/tenants/*/landing` | Gateway whitelist | `grep "public-tenant-landing\|Path=/api/v1/tenants/\*/landing" kitehub/kitehub-gateway/src/main/resources/application.yml` | 2 matches (id + Path predicate) | ✅ exists (precedent cho `/api/v1/public/tenants/by-subdomain/{slug}` Bucket B) |
| `kitehub-frontend/src/middleware.ts` | Next.js middleware | `ls kitehub/kitehub-frontend/src/middleware.ts kitehub/kitehub-frontend/middleware.ts` | 0 files | 🆕 to-be-created (Bucket C) |
| `kitehub-frontend/src/lib/tenant/` | FE module dir | `ls kitehub/kitehub-frontend/src/lib/tenant/` | not exists | 🆕 to-be-created (Bucket C) |
| `infrastructure/terraform-aws/acm-tenant-domains.tf` | Terraform ACM module | `ls infrastructure/terraform-aws/acm-*.tf` | 0 files matching | 🆕 to-be-created (Bucket D) |
| `kitehub-subscription/module/tenant/service/TenantLookupService` | Backend service class | `find . -name "TenantLookupService.java"` | 0 files | 🆕 to-be-created (Bucket B) |
| `documents/01-business/marketing/api-contract.md` | API contract doc | `ls documents/01-business/marketing/api-contract.md` | exists (Wave thesis-4 LandingPage scope) | ✅ exists (Bucket 0 UPDATE add §Public Tenant Resolve section) |
| `TenantHeaderGuardFilter` | Gateway filter class | `find . -name "TenantHeaderGuardFilter.java"` | 0 files | 🆕 to-be-created (Bucket A) |
| `GAP-808` (DONE sister) | Gap file | `bash scripts/query-gaps.sh GAP-808` | DONE 2026-05-29 (landing branding chain) | ✅ DONE — Bucket B+C MUST NOT regress public landing path |

Banned shortcuts:
- ✅ No `| head` truncation
- ✅ Full grep output verified
- ✅ Aspirational refs marked 🆕

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 | docs-only — markdown lint via reviewer | quality-docs (3-layer completeness + cross-layer drift detector) |
| A | `cd kitehub && ./mvnw -pl kitehub-gateway clean verify -P strict-warnings` | kitehub-ci Test Gateway Service |
| B | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -P strict-warnings` + `bash scripts/check-postgres-types-testcontainers.sh` | kitehub-ci Test Subscription Service |
| C | `pnpm --filter kitehub-frontend test --run && pnpm --filter kitehub-frontend build` + `pnpm --filter kitehub-frontend e2e:headless` | kitehub-frontend-ci |
| D | `cd infrastructure/terraform-aws && terraform validate && terraform plan -out=tfplan` (apply DEFERRED per `release-deploy-standard.md` §9) + `cd kitehub && ./mvnw -pl kitehub-subscription clean verify` | kitehub-ci Test Subscription Service + terraform-validate workflow |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `agent-model-opus-default.md`:

- **Sequence 1 (single):** Bucket 0 Foundation — Opus 4.7, foreground spawn (~30min), MERGE FIRST
- **Sequence 2 (parallel ×4):** Buckets A + B + C + D — Opus 4.7, `run_in_background: true`, `isolation: worktree`, all spawn in 1 message
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge sequentially after all background completions per `feedback_coordinator_ci_fix_pattern.md`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected GAP file Log + status (CSV canonical per `gap-architecture-v2.md`)
- Bucket A → GAP-814 🔵 → 🟢 DONE
- Bucket B → GAP-813 🔵 → 🟢 DONE (or 🟡 PARTIAL nếu defer follow-up)
- Bucket C → GAP-811 🔵 → 🟢 DONE
- Bucket D → GAP-812 🔵 → 🟡 PARTIAL (terraform apply deferred per `release-deploy-standard.md` §9 dev-trigger; follow-up file Wave tenant-domain-2 apply phase)
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append entry với agent metrics + lessons
- Files moved to `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0
- Run `bash scripts/prune-merged-worktrees.sh --yes`
- Closure PR body MUST include `## Release Plan Progress` section per `feedback_wave_closure_release_progress_report.md`

---

## 8. Log

- **2026-06-01** (draft): Plan created. Cluster surfaced từ Wave thesis-3 demo-trio RST walk + 3-agent outside-in audit 2026-05-29 (persona simulation + external benchmark + failure-mode matrix). 4 gaps filed PR #1967 (GAP-811/812/813/814) đã state-checked. State-Check Evidence §4 verified — 11 symbols ✅ exists / 5 🆕 to-be-created with explicit Bucket owner. Cross-layer YES → Bucket 0 Foundation required. Wave-pack-planner skill applied: max 5 parallel respected (4 Opus parallel + 1 Foundation sequential). Outside-in audit exception applied per `outside-in-coverage-trigger.md` §4 (gaps from 3-agent audit ≤30 ngày trước → skip refresh).
