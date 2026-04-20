---
name: gap-triage
description: "Dùng khi user nói 'triage gaps', 'gap nào ưu tiên', 'quá nhiều gaps', 'sprint assignment', 'xếp ưu tiên gap'. Phân loại và assign gaps vào sprints."
user-invocable: true
---

# /gap-triage — Gap Triage & Sprint Assignment

## Process

1. **Scan gaps:** đọc `documents/04-quality/gaps/` — count theo status (OPEN/PLANNED/IN_PROGRESS/DONE)
2. **State-check scan (BẮT BUỘC):** for any OPEN/PLANNED gap older than 14 days, re-verify codebase state per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5. Common outcomes:
   - Code shipped elsewhere → close gap as DONE, do not triage
   - Partial now exists → convert to 🟡 PARTIAL with `Current State` section before assigning sprint
   - Still valid → continue
3. **Group by priority:** P0 (blockers) → P1 (important) → P2 (nice-to-have) → P3 (opportunistic)
4. **Group by domain:** Frontend / Backend / DevOps / Docs / Architecture
5. **Dependency check:** gap nào block gap khác? (tìm "Related" links, shared domain)
6. **Score:** Impact (5=blocks prod, 3=blocks feature, 1=inconvenience) ÷ Effort (1=<2h, 3=half-day, 5=multi-day)
7. **Suggest sprint assignment** — gom P0 vào sprint hiện tại, P1 independent có thể parallel, P2 batch theo domain
8. **Update** `documents/04-quality/gaps/ROADMAP.md` với assignments

## Output Format

```
| Gap | Priority | Domain | Impact/Effort | Sprint | Notes |
|-----|----------|--------|---------------|--------|-------|
```

## Gotchas

- ROADMAP.md dùng epic headings — assign gap vào đúng epic, tạo epic mới nếu cần
- P0 mà block P1 chain → fix P0 TRƯỚC dù effort cao
- Gaps cùng domain nên gom vào 1 PR (max 3-5 gaps/PR theo `audit-to-gap-pipeline.md`)
- Check duplicate trước khi triage — 2 gaps có thể describe cùng issue
- Infrastructure gaps fix trước feature gaps (dependency rule từ pipeline)
