---
title: "Wave 86 Bucket E quick-win 3 P0 BLOCKERS — Redis maxmemory-policy + MAX_ROWS=1000 + Dependabot docker"
status: complete
created: 2026-05-16
phase: Wave 86 Bucket E (pre-launch hardening verification)
wave: 86
gaps: []
related_prs:
  - "#1445 (Wave 86 Bucket E sweep — surface 3 quick-win P0)"
  - "current (Agent E quick-win fix sweep)"
rules_applied:
  - .claude/rules/audit-to-gap-pipeline.md §2.7 decision-doc code-sync
  - .claude/rules/pre-handoff-self-test-completeness.md §1 verify-flow
  - .claude/rules/pre-launch-infra-hardening-checklist.md §2.2 CORS / §2.4 Docker non-root (sister)
  - .claude/rules/pre-launch-dependency-hardening-checklist.md §2.7 Dependabot config docker
  - .claude/rules/dev-readable-doc-language.md Vietnamese narrative
---

# Wave 86 Bucket E Quick-Win — 3 P0 BLOCKERS Closed

## Scope

PR #1445 Bucket E sweep audit surface 3 P0 quick-win BLOCKERS chặn `v1.0.0-rc.1` tag:

| ID | Spec | Symptom | Fix |
|----|------|---------|-----|
| E-AC2 | Redis `maxmemory-policy=allkeys-lru` | `kite-redis` service không có `command:` directive → default `noeviction` → cache stampede risk khi memory đầy → RDS spike (sim cell 10) | Add `command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru` |
| E-AC5 | Bulk-import cap = 1000 rows/request + HTTP 413 | `StudentBulkImportService.MAX_ROWS=10_000` + HTTP 400 (BAD_REQUEST) → t3.micro RAM 1GB blow-up risk + wrong RFC 7231 semantic | `MAX_ROWS=1_000` + `HttpStatus.PAYLOAD_TOO_LARGE` (413) + extended test asserting HTTP 413 |
| (Cat 1 §2.7) | Dependabot `docker` ecosystem coverage | `.github/dependabot.yml` covers maven/npm/github-actions only — base images không monitored CVE | Add 10 docker ecosystem blocks (9 service dirs + frontend with dev variant) |

## Commands run (Tier 1 read-only + dedicated tools)

```bash
# Find canonical compose file
find . -name "docker-compose.kitehub.yml" -not -path "*/node_modules/*"
# → kitehub/docker-compose.kitehub.yml

# Grep current Redis service config
grep -n "kite-redis" kitehub/docker-compose.kitehub.yml
# → line 36 (no command directive)

# Find bulk-import service
grep -rnE "StudentBulkImport|BulkImport|bulk.import" kitehub/ kiteclass/ --include="*.java" -l
# → kiteclass/kiteclass-core/.../bulkimport/service/StudentBulkImportService.java

# Find existing test
grep -rnE "MAX_ROWS|10_000|ROW_LIMIT" kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/student/bulkimport/
# → StudentBulkImportServiceTest:200-212 — test exists, no HTTP status assert

# Find api-contract.md for bulk-import
find documents/01-business -name "api-contract.md" | xargs grep -l "bulk.import|BULK_IMPORT"
# → documents/01-business/kiteclass/bulk-import/api-contract.md

# Find all Dockerfile locations
find . -name "Dockerfile*" -not -path "*/node_modules/*" -not -path "*/target/*"
# → 11 Dockerfile (9 service dirs + 1 frontend with Dockerfile.dev)

# Validate dependabot.yml YAML
python3 -c "import yaml; d=yaml.safe_load(open('.github/dependabot.yml')); print(len(d['updates']))"
# → 16 updates total (3 maven + 2 npm + 1 github-actions + 10 docker)

# Run targeted test
cd kiteclass/kiteclass-core && ./mvnw -B -ntp test -Dtest=StudentBulkImportServiceTest
# → Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

## Findings

### Real changes (intent verified)

| # | File | Action | Root cause | Risk |
|---|------|--------|-----------|------|
| 1 | `kitehub/docker-compose.kitehub.yml` | update kite-redis service | Add `command: redis-server --maxmemory ${REDIS_MAXMEMORY:-256mb} --maxmemory-policy ${REDIS_MAXMEMORY_POLICY:-allkeys-lru}` directive (configurable env override + sane default) | Low — backward compatible (default policy preserved if env set); restart `kite-redis` container picks up new policy |
| 2 | `kiteclass/kiteclass-core/.../StudentBulkImportService.java` | update constant + HTTP status | `MAX_ROWS: 10_000 → 1_000`; `HttpStatus.BAD_REQUEST → HttpStatus.PAYLOAD_TOO_LARGE` cho row-limit exception | Medium — existing client expecting 400 sẽ thấy 413 (semantic upgrade per RFC 7231 §6.5.11); FE phải implement client-side chunking nếu user upload > 1000 dòng |
| 3 | `kiteclass/kiteclass-core/.../StudentBulkImportServiceTest.java` | extend test | Assert `HttpStatus.PAYLOAD_TOO_LARGE` + `MAX_ROWS=1_000` invariant | Low — tests strengthened; 7/7 PASS local |
| 4 | `documents/01-business/kiteclass/bulk-import/api-contract.md` | update error table | HTTP 400 → 413; cite Wave 86 E-AC5 rationale | None — doc sync per Living Docs |
| 5 | `documents/01-business/kiteclass/bulk-import/rules.md` | update BR-BI-003 + BR-BI-005 | Row cap 10_000 → 1_000; HTTP 400 → 413 | None — doc sync |
| 6 | `documents/01-business/kiteclass/bulk-import/use-cases.md` | update UC-BI exception flow | Trigger threshold 10_000 → 1_000; FE narrative update; performance note 1000 rows × 500-row chunk = 2 round-trips (vs prior 20) | None — doc sync |
| 7 | `.github/dependabot.yml` | add 10 docker ecosystem blocks | 9 service dirs (kiteclass-core/frontend/gateway + kitehub-admin/base/branding/email/frontend/gateway/subscription) — patch-only update-types consistent với pre-MVP lock policy | Low — Dependabot will scan FROM directives weekly; 1 PR per service-dir max |

### Phantom updates (no real change)

None — all changes intentional.

### Verdict

All 3 P0 BLOCKERS resolved with paired Living Docs sync (api-contract.md + rules.md + use-cases.md) per `audit-to-gap-pipeline.md` §2.7 decision-doc code-sync. Test extended với HTTP status assertion per `pre-handoff-self-test-completeness.md` §1 verify-flow. Backward-compat risk minimized via env override (`REDIS_MAXMEMORY` / `REDIS_MAXMEMORY_POLICY`) + semantic HTTP status upgrade (400 → 413 đúng RFC 7231 hơn).

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|--------|------|----------------|
| PR #1445 Wave 86 Bucket E sweep audit | 2026-05-16 | Surface 3 P0 quick-win → this PR closes |
| `pre-launch-infra-hardening-checklist.md` v1.0.1 (Cat 5 rubric §2) | 2026-05-14 | Existing rule mandates infra hardening; this PR adds Redis as positive evidence |
| `pre-launch-dependency-hardening-checklist.md` v1.0.1 §2.7 | 2026-05-14 | Existing rule mandates Dependabot ecosystem coverage including `docker`; this PR closes coverage |
| `audit-env-coverage.sh` Wave 71 Bucket E (CORS / VERIFICATION_BASE_URL / EMAIL_PROVIDER overrides) | 2026-05-13 | Sister fix family — production env config audit; this PR extends infra side (Redis policy) |
| `production-env-config-registry.md` §11 (3 audit scripts gateway-routes/service-ports/spring-profiles) | 2026-05-13 | Existing class-of-bug audits; this PR's Redis fix is sim cell 10 follow-up |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Bucket E sweep findings flip 3 rows OPEN → ✅ | Coordinator | E-AC2 Redis + E-AC5 BulkImport + Cat 1 §2.7 docker — verify post-merge PR #1445 audit artifact |
| FE chunking implementation cho > 1000 rows | Future wave (Phase 1.5+ per use-cases.md UC-BI exception flow) | Tracked use-cases.md §"FE behavior" note + api-contract.md §413 row |
| Smoke test post-deploy: verify `redis-cli CONFIG GET maxmemory-policy` returns `allkeys-lru` on dev EC2 | Coordinator | Run `bash scripts/aws/start-stack.sh` then `docker exec kite-redis redis-cli CONFIG GET maxmemory-policy` |
| **Concurrent op check** | Agent verification | No active terraform/deploy workflows; PR scope = code + docs only (no infra mutation) — concurrent serialization N/A per `concurrent-production-mutation-ops.md` |

## Recommendations

1. **Merge sequence:** ship this PR after PR #1445 (audit artifact) merge để flip rows OPEN → ✅; OR coordinator updates Bucket E audit artifact same PR (chọn option 2 cho atomicity)
2. **Post-merge verification:**
   - Smoke test: `docker exec kite-redis redis-cli CONFIG GET maxmemory-policy` → expect `allkeys-lru`
   - Smoke test: `curl -X POST <bulk-import-endpoint> -F file=@1001row.xlsx` → expect HTTP 413 với JSON body `{"code":"BULK_IMPORT_ROW_LIMIT_EXCEEDED",...}`
   - Verify Dependabot Monday run produces first docker PRs (max 10, patch-only)
3. **Follow-up gap (Phase 1.5+):** FE auto-chunking implementation cho file > 1000 rows — file gap nếu user feedback xuất hiện
4. **Watch-for:** Redis `maxmemory 256mb` baseline với t3.micro (REDIS_MEM_LIMIT=512m default) — monitor `redis-cli INFO memory used_memory_human` để verify không bị evict prematurely khi traffic ramp; có thể nâng lên 384mb-512mb nếu cần (env override-able)

## Self-test per `pre-handoff-self-test-completeness.md` §1

### Fix 1 — Redis maxmemory-policy

- [x] **Config command directive present:** `python3 -c "import yaml; d=yaml.safe_load(open('kitehub/docker-compose.kitehub.yml')); print(d['services']['kite-redis']['command'])"` → `redis-server --maxmemory ${REDIS_MAXMEMORY:-256mb} --maxmemory-policy ${REDIS_MAXMEMORY_POLICY:-allkeys-lru}`
- [x] **Env override mechanism:** `REDIS_MAXMEMORY` + `REDIS_MAXMEMORY_POLICY` env vars defined với sane defaults (production có thể override qua compose env block)
- [x] **Backward-compat:** existing healthcheck `redis-cli ping` không thay đổi; ports 6380:6379 không thay đổi

### Fix 2 — BulkImport MAX_ROWS=1000 + HTTP 413

- [x] **Source code updated:** `MAX_ROWS = 1_000` (line 52) + `HttpStatus.PAYLOAD_TOO_LARGE` (line 192)
- [x] **Test asserts HTTP status:** `extracting("status").isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE)` (line 220-221)
- [x] **mvn verify PASS:** `cd kiteclass/kiteclass-core && ./mvnw test -Dtest=StudentBulkImportServiceTest` → Tests run: 7, Failures: 0
- [x] **Living Docs sync:** api-contract.md row + rules.md BR-BI-003/BR-BI-005 + use-cases.md UC-BI exception flow updated cùng PR

### Fix 3 — Dependabot docker

- [x] **YAML valid:** `python3 -c "import yaml; yaml.safe_load(open('.github/dependabot.yml'))"` → no exception
- [x] **10 docker entries:** ecosystem count `{'maven': 3, 'npm': 2, 'github-actions': 1, 'docker': 10}` — covers all 9 service Dockerfile dirs + 1 frontend dev variant
- [x] **Patch-only consistent với pre-MVP lock policy:** `update-types: ["patch"]` + ignore major/minor — matches existing maven/npm config style

## References

- PR #1445 — Wave 86 Bucket E sweep audit (surface 3 P0 quick-win)
- Wave 86 plan: `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md` §3 Bucket E
- Rule: `.claude/rules/pre-launch-infra-hardening-checklist.md` §2 Cat 5 (Redis policy sister to §2.2 CORS / §2.4 Docker non-root)
- Rule: `.claude/rules/pre-launch-dependency-hardening-checklist.md` §2.7 (Dependabot docker)
- Spec: RFC 7231 §6.5.11 — `413 Payload Too Large` semantic (row-cap exceeded đúng hơn 400 Bad Request)
- Living Docs trinity: `documents/01-business/kiteclass/bulk-import/{rules,use-cases,api-contract}.md`
