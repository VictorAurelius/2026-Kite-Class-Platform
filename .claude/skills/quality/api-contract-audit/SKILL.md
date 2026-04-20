---
name: api-contract-audit
description: "Dùng khi user nói 'api audit', 'contract check', 'kiểm tra API', 'endpoint match docs?', 'breaking change?', hoặc trước release. Verify API endpoints match api-contract.md /100."
user-invocable: true
---

# /api-contract-audit — API ↔ Documentation Sync

Score /100. Verify every controller endpoint is documented and every documented endpoint exists in code.

## Process

### 1. Extract Endpoints from Code

```bash
# KiteClass controllers
grep -rn "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping\|@PatchMapping\|@RequestMapping" \
  --include="*.java" kiteclass/kiteclass-core/src/main/ | grep -v test

# KiteHub controllers (all services)
grep -rn "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" \
  --include="*.java" kitehub/*/src/main/ | grep -v test
```

### 2. Extract Endpoints from Docs

```bash
# API contract docs
grep -rn "^### \|^## \|GET \|POST \|PUT \|DELETE \|PATCH " \
  documents/01-business/*/api-contract.md | head -50
```

### 3. Score 5 Categories

| # | Category (20pts) | Key Checks |
|---|-----------------|------------|
| 1 | **Endpoint Coverage** | Every @Mapping has api-contract.md entry |
| 2 | **Request/Response Match** | DTO fields match documented schema |
| 3 | **Error Code Consistency** | Error codes in code match docs |
| 4 | **Versioning & Deprecation** | No undocumented breaking changes |
| 5 | **Integration Test Coverage** | Each documented endpoint has IT |

Scoring details: `reference/scoring-guide.md`

### 4. Output

Save to `documents/04-quality/audits/api/api-contract-audit-[date].md`

Format: 2-column table per domain — Code endpoints | Doc endpoints — highlight mismatches.

## Context Management

Token budget ~30-45K. KiteHub 6 microservices có thể tạo output rất lớn. Kiểm soát:

1. **Grep output limiting** — LUÔN `| head -30` per service. Tổng controller grep cho 6 services nếu không limit = 200+ lines.
2. **Per-service delegation** — Nếu >4 microservices:
   - Agent 1: KiteClass core (1 service)
   - Agent 2: KiteHub services (6 services)
   - Mỗi agent: extract endpoints → compare với api-contract.md → trả mismatch list
3. **Diff-based audit** — Sau baseline, chỉ check endpoints trong files changed since last audit:
   ```bash
   git diff main --name-only | grep -E 'Controller\.java|api-contract\.md'
   ```
4. **Mismatch-first output** — KHÔNG list tất cả endpoints khớp. Chỉ output mismatches + summary count.

## Gotchas

- Gateway routes (`/api/v1/**`) proxy to core — check gateway config for actual public paths
- KiteHub has 6 microservices — check ALL service controllers, not just one
- Public endpoints (no auth): `/public/**` — documented differently from authenticated ones
- Websocket/SSE endpoints may not follow REST pattern — document separately
- Wave 4 added `PublicDmcaController` — verify it's in legal-ip-protection api-contract.md
- Controller grep output cho 6 services rất lớn — LUÔN limit per service
- **Multi-module scope** — `kiteclass/kiteclass-core/src/main/` + `kitehub/*/src/main/` cover known controllers. Nếu thêm service mới (e.g. kiteclass-analytics), update grep paths OR switch to broad `--include="*.java"` from root. Ref: GAP-149.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
