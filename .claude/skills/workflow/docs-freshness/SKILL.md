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
