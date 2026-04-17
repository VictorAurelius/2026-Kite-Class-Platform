---
name: pr-health
description: "Dùng khi user nói 'check PR', 'PR health', 'PR compliance', 'PR có ổn không', 'kiểm tra PR', 'PR nào vi phạm', 'scan PRs'. Scan 1 hoặc nhiều merged PRs xem compliance với workflow rules."
user-invocable: true
argument-hint: "<PR#> hoặc <from>-<to>"
---

# /pr-health — PR Compliance Scanner

Scan bất kỳ PR nào (đã merge hoặc chưa) và output bảng compliance.

## Usage

```
/pr-health 314         # Single PR
/pr-health 310-315     # Range → matrix table
```

## Process

1. Run: `./scripts/pr-compliance-check.sh <argument>`
2. Format output cho user
3. Nếu có violations → suggest fix commands

## 5 Compliance Checks

| Check | Pass khi | Fail khi |
|-------|---------|---------|
| CI green | CI success at merge commit | CI failure/unknown |
| Tests | Test files exist for java changes | 0 test files |
| Business docs | 01-business/ updated khi code logic changed | Code changed, no docs |
| Audits | Required audit reports ≤7 days old | Missing audit report |
| Wave check | Wave completion report exists | Wave PR without report |

## PR Log Files

Hook `audit-gate.py` auto-creates JSON log: `documents/03-planning/pr-logs/PR-{N}.json`

Logs track: events (create, merge), checklist status, compliance score.

## Gotchas

- Script cần `gh` CLI authenticated
- Retroactive scan dùng GitHub API (slower cho range queries)
- Wave check chỉ apply cho branches matching `wave/*`
- Audits check dựa trên file modification time, không phải git commit date
