---
name: docs-freshness
description: Nhac update living docs sau moi PR/wave
user-invocable: false
---

# Docs Freshness — Living Documents Tracking

## Living Docs (can update lien tuc)

| Doc | Update khi | Check |
|-----|-----------|-------|
| `README.md` (root) | Them/xoa folder, thay doi tech stack, them service | Folder structure khop reality |
| `CLAUDE.md` | Thay doi quy trinh, them skill, doi convention | Context du cho session moi |
| `documents/01-business/*.md` | Thay doi business logic, config, email trigger | Rules khop code |
| `documents/01-business/README.md` | Them/xoa business doc | Index day du |
| `documents/03-planning/quality/*.md` | Sau moi quality audit | Score phan anh thuc te |
| `.claude/skills/_README-skills-index.md` | Them/xoa/rename skill | Index khop files |

## Trigger Points

1. **Sau moi wave merge** → check tat ca living docs
2. **Sau moi PR thay doi structure** → check README, CLAUDE.md
3. **Sau moi PR thay doi business logic** → check 01-business/
4. **Sau moi skill them/sua** → check skills index

## Check Process

1. So sanh `git diff` cua wave voi living docs list
2. Neu code thay doi domain X nhung `01-business/X.md` khong update → flag
3. Neu folder structure thay doi nhung README khong update → flag
4. Output: danh sach docs can update

## Integration

- `/wave-completion-check` Level 5 goi docs-freshness check
- `/quality-audit` Category 8 dung docs-freshness criteria
- Pre-commit hook warning khi business logic thay doi

---

## Gotchas

- **Skill flags candidates, does NOT enforce** — output is an advisory list of docs that *might* need update; final decision still belongs to the PR author/reviewer. Pair with `audit-to-gap-pipeline.md` Step 2.5 state-check if you need the harder "code-doc divergence" check
- **Project root `README.md` is living, but service-level READMEs are NOT in this list** — only the 6 docs in §Living Docs are tracked; Wave Meta-Gov 1 introduced `scripts/check-readme-freshness.sh` (CI job `readme-freshness`, GAP-255) for the broader 46-README freshness window — they are complementary, not duplicates
- **Business doc drift detection is grep-based, not semantic** — this skill checks "did business code change without business doc change?" via path overlap; it cannot detect that the *content* drifted (e.g. config value changed but doc still cites old value). Use `quality/business-logic-audit/SKILL.md` for value-level verification
- **Pre-commit hook coverage is partial** — the hook in `.husky/` only fires on staged files; if a PR re-bases or rewrites history, drift between branches won't trigger a warning. Re-run check on the PR base SHA, not just HEAD
- **`documents/01-business/*/rules.md` 3-layer rule trumps this list** — when a domain folder has the 3-file structure (rules.md / use-cases.md / api-contract.md), all three are living together; do not flag only `rules.md` as drift while leaving the matching `api-contract.md` stale (CLAUDE.md §Business Logic Documents)
