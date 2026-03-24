# Wave 12 — Verification: Code ↔ Docs ↔ Tests Consistency

**Date:** 2026-03-24
**Prerequisite:** Wave 10 + Wave 11 complete
**Target:** 100% consistency across 3 layers + code + tests

## Approach: Audit First, Fix Later

Wave 12 chia 2 phase:
- **Phase A (12a):** AUDIT only — scan mismatches, tạo report, KHÔNG sửa code
- **Phase B (12b):** FIX — từng PR lẻ, TDD, CI verify, review trước merge

Code changes tốn thời gian test → phải audit trước để biết scope chính xác.

---

## Verification Chain

```
rules.md ──→ use-cases.md ──→ api-contract.md ──→ Controller.java ──→ *Test.java
  BR-xxx       UC-xxx          endpoint            @Mapping            @Test
```

**Mỗi link phải verifiable:**

| Link | Check method | Pass criteria |
|------|-------------|---------------|
| Rule → Use Case | Grep BR-xxx trong use-cases.md | Mỗi BR- xuất hiện ít nhất 1 lần |
| Use Case → API | Grep UC-xxx trong api-contract.md | Mỗi UC- map đến ≥1 endpoint |
| API → Controller | Grep endpoint path trong `@*Mapping` | Mỗi endpoint tồn tại trong code |
| Controller → Test | Grep method name trong *Test.java | Mỗi public method có test |
| Use Case Error → Test | Grep error code trong test assertions | Mỗi error path có test case |

---

## PR List

### Phase A: AUDIT (read-only, không sửa code)

### PR-A1: Verification Script + KiteClass Audit [CRITICAL]

**Scope:** Tạo script + scan 9 KC domains

- [ ] Tạo `scripts/verify-business-docs.sh`
  ```bash
  # For each domain:
  # 1. Extract BR-xxx from rules.md
  # 2. Check each BR-xxx exists in use-cases.md
  # 3. Extract UC-xxx from use-cases.md
  # 4. Check each UC-xxx exists in api-contract.md
  # 5. Extract endpoints from api-contract.md
  # 6. Check each endpoint exists in Controller
  ```
- [ ] Run verification → fix mismatches
- [ ] Fix code nếu không match docs (docs là source of truth)
- [ ] Fix docs nếu code đã implement khác (code proved better approach)
- [ ] Add missing tests cho untested use case error paths

### PR-A2: KiteHub Audit [CRITICAL]

**Scope:** 7 domains × 5 checks = 35 verification points

- [ ] Run verification script cho KiteHub
- [ ] Output: mismatch report (KHÔNG sửa code)

### Phase B: FIX (từng PR lẻ, TDD, CI verify)

### PR-B1..Bn: Fix Mismatches (1 PR per issue)

**Mỗi PR gồm:**
- [ ] TDD: viết/update test trước
- [ ] Fix code hoặc update docs
- [ ] CI green
- [ ] Review trước merge

**Ví dụ PRs có thể phát sinh:**
- PR-B1: Fix endpoint X không match api-contract.md
- PR-B2: Add missing test cho UC-xxx error path
- PR-B3: Update rules.md — code đã implement khác
- ...số lượng tùy thuộc kết quả audit

### PR-C1: Create Verification Skill [HIGH]

- [ ] Tạo `.claude/skills/workflow/verify-business-docs/SKILL.md`
  - Auto-run sau Wave complete
  - Checks: rules→UC→API→code→tests chain
  - Output: mismatch report
- [ ] Update `/wave-completion-check` Level 5 — integrate verification
- [ ] Update `/quality-audit` Category 8 — include chain verification score

### PR-4: Final Quality Audit [HIGH]

- [ ] Run `/quality-audit kitehub` → target 100/100
- [ ] Run `/quality-audit kiteclass` → target 100/100
- [ ] Run `/business-gap-check` → target 100%
- [ ] Run verification script → target 0 mismatches
- [ ] Save final reports

---

## Execution

| Agent | PR | Scope |
|-------|-----|-------|
| 1 | PR-1 | KiteClass verification (9 domains) |
| 2 | PR-2 | KiteHub verification (7 domains) |
| 3 | PR-3 | Verification skill + integration |
| 4 | PR-4 | Final audit reports |

---

## Success Criteria

- [ ] 0 orphan rules (BR- not referenced in UC)
- [ ] 0 orphan use cases (UC- not mapped to API)
- [ ] 0 phantom endpoints (in docs but not in code)
- [ ] 0 untested error paths
- [ ] Quality audit: KiteHub 100/100, KiteClass 100/100
- [ ] Business gap: KiteHub 100%, KiteClass 100%
- [ ] Verification script passes with 0 warnings
