# Parallel Execution Strategy

**Ngày:** 2026-03-23
**Vấn đề:** 39 PRs + 3 refactor = 42 PRs, ~20 ngày tuần tự
**Mục tiêu:** Giảm xuống ~8-10 ngày bằng parallel execution

---

## Dependency Analysis

### PRs KHÔNG phụ thuộc nhau (song song được)

```
Group A — Backend Config (không đụng nhau):
  SAAS-1  (subscription config)     ← chỉ sửa subscription service
  SAAS-14 (reserved subdomain)      ← chỉ sửa InstanceService
  SAAS-15 (configurable BASE_DOMAIN) ← chỉ sửa gateway filter
  KC-12   (secure internal secret)  ← chỉ sửa kiteclass-core config

Group B — Email (không đụng Group A):
  SAAS-2  (missing templates)       ← chỉ sửa kitehub-email templates
  SAAS-5  (email sent log)          ← chỉ thêm table + repository

Group C — Frontend (không đụng backend):
  SAAS-6  (SEO foundation)          ← chỉ sửa kitehub-frontend
  KC-5    (fix FE TODOs)            ← chỉ sửa kiteclass-frontend
  KC-13   (FE SEO basics)           ← chỉ sửa kiteclass-frontend

Group D — Docs only (không đụng code):
  REFACTOR-1 (docs restructure)
  REFACTOR-3 (business docs migration)
  SAAS-13 (architecture docs)
  KC-6    (README + deploy guide)

Group E — Tests only:
  KC-1    (fix @Disabled)           ← chỉ sửa test files
  KC-2    (integration tests)       ← chỉ thêm test files
  KC-15   (tenant isolation test)   ← chỉ thêm test files
```

### PRs CÓ dependency (phải tuần tự)

```
SAAS-1 (config) ──→ SAAS-4 (trial limit, dùng config)
                ──→ SAAS-3 (data retention, dùng config)
                ──→ SAAS-7 (email lifecycle, dùng config)

SAAS-2 (templates) ──→ SAAS-7 (thêm templates, cùng folder)

SAAS-3 (retention) ──→ SAAS-7 (retention emails)

REFACTOR-2 (skills) ──→ phải sau REFACTOR-1 (docs) vì cross-reference
```

---

## Parallel Execution Plan

### Wave 1 — Foundation (Day 1-2) — 4 agents song song

```
Agent 1 (worktree): SAAS-1 + SAAS-4 (config + trial limit)
  Files: subscription/config/, InstanceService, Instance.java, TrialExpirationChecker

Agent 2 (worktree): SAAS-14 + SAAS-15 + KC-12 (security fixes)
  Files: InstanceService validation, TenantResolverFilter, InternalRequestFilter

Agent 3 (worktree): SAAS-2 (missing email templates)
  Files: kitehub-email/templates/ only

Agent 4 (worktree): REFACTOR-1 + REFACTOR-3 (docs restructure + business docs)
  Files: documents/ only — KHÔNG đụng code
```

**Conflict risk:** LOW — mỗi agent sửa files khác nhau.
**Merge order:** Agent 4 → Agent 3 → Agent 2 → Agent 1 (ít conflict nhất trước)

### Wave 2 — SEO + Tests (Day 3-4) — 4 agents song song

```
Agent 1 (worktree): SAAS-6 (SEO foundation — kitehub-frontend)
  Files: kitehub-frontend/src/app/ only

Agent 2 (worktree): KC-5 + KC-13 (FE TODOs + SEO — kiteclass-frontend)
  Files: kiteclass-frontend/src/ only

Agent 3 (worktree): KC-1 + KC-2 + KC-15 (tests — kiteclass-core/test/)
  Files: kiteclass-core/src/test/ only

Agent 4 (worktree): SAAS-5 + SAAS-3 (email log + data retention)
  Files: subscription service + Flyway migration
  Depends on: Wave 1 Agent 1 (config) merged
```

### Wave 3 — Email + Domain (Day 5-6) — 3 agents song song

```
Agent 1 (worktree): SAAS-7 (complete email lifecycle — 12 templates)
  Files: email templates + subscription schedulers
  Depends on: SAAS-1 (config), SAAS-2 (base templates)

Agent 2 (worktree): SAAS-16 (custom domain UI + DNS verify)
  Files: subscription/service + kitehub-frontend

Agent 3 (worktree): REFACTOR-2 (skills consolidation — 49→20)
  Files: .claude/skills/ only
```

### Wave 4 — Features (Day 7-8) — 3 agents song song

```
Agent 1 (worktree): SAAS-8 (template gallery — Canva-like)
  Files: branding service + kitehub-frontend

Agent 2 (worktree): KC-3 + KC-4 (E2E + multi-tenant verify)
  Files: scripts + test files

Agent 3 (worktree): V3-3 + V3-4 + V3-5 + V3-6 (quality polish)
  Files: mixed — nhưng nhỏ, ít conflict
```

### Wave 5 — Content + Final (Day 9-10) — 2 agents song song

```
Agent 1 (worktree): SAAS-11 (blog MDX)
  Files: kitehub-frontend content/

Agent 2 (worktree): SAAS-9 + SAAS-10 + SAAS-12 + SAAS-17 + remaining KC PRs
  Files: mixed small changes
```

---

## Timeline Comparison

| Approach | Days | PRs/day |
|----------|------|---------|
| Tuần tự (1 agent) | ~20 ngày | ~2 PRs/day |
| **Song song (3-4 agents)** | **~10 ngày** | **~4 PRs/day** |
| Tối đa song song | ~7 ngày | ~6 PRs/day (rủi ro merge conflict cao) |

**Recommend: 3-4 agents/wave** — balance giữa tốc độ và conflict risk.

---

## Agent Worktree Strategy

```bash
# Mỗi agent chạy trong git worktree riêng biệt
# → Không conflict working directory
# → Merge tuần tự sau khi review

# Ví dụ Wave 1:
Agent 1: isolation=worktree → branch feature/saas-1-config
Agent 2: isolation=worktree → branch feature/saas-14-15-security
Agent 3: isolation=worktree → branch feature/saas-2-templates
Agent 4: isolation=worktree → branch feature/refactor-docs
```

### Merge Protocol

```
1. Tất cả agents hoàn thành → review song song
2. Merge theo thứ tự conflict-risk (thấp → cao):
   - Docs-only PRs first
   - Test-only PRs
   - Single-service PRs
   - Cross-service PRs last
3. Nếu conflict → agent cuối resolve
4. CI check sau mỗi merge
```

---

## Limitations

| Constraint | Impact | Mitigation |
|-----------|--------|-----------|
| Git worktree = snapshot tại thời điểm tạo | Agent không thấy changes từ agent khác | Merge theo wave, không cross-wave |
| Pre-commit hooks chạy trên mỗi worktree | Mỗi agent phải pass hooks riêng | Hooks đã LF-safe |
| CI chạy trên main sau merge | Không validate cross-PR trước merge | Merge 1 PR → chờ CI → merge tiếp |
| Context window | Agent có giới hạn context | Prompt rõ ràng, scope nhỏ |

---

## Khi nào KHÔNG nên song song

- 2 PRs sửa CÙNG file → tuần tự
- PR B phụ thuộc output PR A → tuần tự
- PR phức tạp cần nhiều iteration → dedicated agent
- Lần đầu làm pattern mới → tuần tự, đúc rút, rồi mới parallel
