# Execution Workflow

> Pointer: read this for day-to-day execution against the plan once it's approved. Parent skill: `../SKILL.md`.

## 🔄 Execution Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. ANALYZE ISSUE                                                 │
│    - Root cause identified                                       │
│    - Impact assessed                                             │
│    - Priority justified                                          │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. CREATE PRIORITY PLAN                                          │
│    ✅ Copy quality standards from master plan                    │
│    ✅ Reference .claude/skills/ for workflow                     │
│    ✅ Include all 7 required sections                            │
│    ✅ Complete acceptance criteria                               │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. VALIDATE PLAN                                                 │
│    ✅ Quality gate checklist (see validation-checklist.md)       │
│    ✅ Compliance matrix verified                                 │
│    ✅ All sections complete                                      │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. EXECUTE PLAN                                                  │
│    - Create branch                                               │
│    - Implement changes                                           │
│    - Write tests                                                 │
│    - Commit with proper message                                  │
│    - Push and create PR                                          │
│    - Monitor CI                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. POST-EXECUTION                                                │
│    ✅ Update STATUS-UPDATE-YYYY-MM-DD.md                         │
│    ✅ Update master implementation plan                          │
│    ✅ Update MEMORY.md if lessons learned                        │
│    ✅ Close ticket (KC-{id})                                     │
└─────────────────────────────────────────────────────────────────┘
```
