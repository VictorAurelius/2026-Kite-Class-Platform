# Core Principle — Master Plan Inheritance

> Pointer: read this when first using the skill or in doubt about authority of the priority plan vs master plan. Parent skill: `../SKILL.md`.

## 🎯 Core Principle

> **Priority plans PHẢI tuân thủ TẤT CẢ quality standards từ master plan**

```
┌─────────────────────────────────────────────────────────────────┐
│                   MASTER IMPLEMENTATION PLAN                     │
│                 (documents/.../kiteclass-implementation-plan.md) │
│                                                                  │
│  Quality Standards (NON-NEGOTIABLE):                            │
│  ✅ Backend: 80% coverage, JavaDoc, multi-tenant, soft delete  │
│  ✅ Frontend: TypeScript strict, React Testing Library         │
│  ✅ Security: Input validation, HMAC auth, tenant isolation    │
│  ✅ Testing: Unit + Integration + Multi-tenant tests           │
│                                                                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           │ INHERIT ALL STANDARDS
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PRIORITY PR PLAN                              │
│              (PRIORITY-PLAN-YYYY-MM-DD.md)                       │
│                                                                  │
│  Must include:                                                  │
│  ✅ All quality standards from master plan                      │
│  ✅ Workflow references to .claude/skills/                      │
│  ✅ Complete acceptance criteria                                │
│  ✅ Testing checklist (unit + integration + regression)         │
│  ✅ CI validation steps                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

The priority plan never invents new standards. It re-states or references the master plan + skills, then sequences the urgent PRs that justify deviation.
