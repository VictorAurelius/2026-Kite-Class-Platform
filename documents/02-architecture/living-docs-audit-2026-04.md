# Living Docs Impact Audit — AI Branding v2 Redesign

**Date:** 2026-04-14
**Context:** AI Branding v2 redesign creates 15 implementation gaps (GAP-007..015). Per CLAUDE.md Living Docs rule, business docs must sync with code changes. This audit maps ALL docs needing update per implementation PR.

**Related:** GAP-016 (this audit fulfills the gap)

---

## Scope Matrix

### 🔴 MUST UPDATE per PR (business-critical)

Business docs 3-layer structure requires ALL 3 files updated when logic changes:

| Gap Implementing | Business Docs to Update | Approver |
|------------------|------------------------|----------|
| GAP-007 (Classification) | `01-business/kitehub/ai-branding/{rules,use-cases,api-contract}.md` | PM + Tech Lead |
| GAP-008 (Agent workflow) | Same + UC-AIB-07..10 new | PM |
| GAP-009 (Lifecycle) | `01-business/kitehub/instance-provisioning/{rules,use-cases}.md` | PM + Architect |
| GAP-010 (Package API) | `01-business/kitehub/ai-branding/api-contract.md` (new endpoints) | API owner |
| GAP-011 (Templates) | Branding rules + new template review rules | Design lead |
| GAP-012 (Quality gate) | Quality rules section + lifecycle integration | QA lead |
| GAP-013 (Wizard UX) | Use-cases for 10-step wizard | UX + PM |
| GAP-014 (Mock) | Mock plan doc (this PR updates) | Dev |
| GAP-015 (Auto-trigger) | Instance-provisioning rules + subscription integration | Architect |

---

### 🟠 SHOULD UPDATE (technical architecture)

| Doc | Updated by | When |
|-----|-----------|------|
| `02-architecture/docker-platform-architecture.md` | GAP-005 (queue topology), GAP-006 (Gemma 4) | Infrastructure changes |
| `03-planning/database/database-design.md` | GAP-007, 009, 012 (new entities) | DB schema additions |
| `03-planning/database/database-migration-plan.md` | Every migration PR | V28, V29... |
| `06-diagrams/plantuml/03-erd.puml` | GAP-007, 009 | New relationships |
| `06-diagrams/plantuml/04-architecture-full.puml` | GAP-008 (new components) | Architecture changes |
| `.claude/skills/api-design.md` | GAP-010 (new endpoints) | API changes |

---

### 🟢 TESTS TO UPDATE

| Test Area | Action | Trigger |
|-----------|--------|---------|
| `kitehub-branding/src/test/java/.../AIBrandingServiceTest.java` | Refactor to test Agent workflow | GAP-008 done |
| `kitehub-branding/src/test/java/.../OllamaClientTest.java` | Update model refs to Gemma 4 | GAP-006 done |
| New: `BrandingE2EIntegrationTest.java` | Create | GAP-010 done |
| New: `InstanceQualityReviewerTest.java` | Create | GAP-012 done |
| New: `InstanceLifecycleServiceTest.java` | Create | GAP-009 done |

---

### 🔵 NEW DOCS TO CREATE

| Path | Purpose | Created by |
|------|---------|-----------|
| `05-guides/branding/branding-integration.md` | KiteClass consume branding package | GAP-010 PR |
| `05-guides/branding/ai-branding-wizard-flow.md` | Wizard UX user guide | GAP-013 PR |
| `05-guides/contributing/template-contribution-guide.md` | How to add templates | GAP-011 PR |
| `.claude/skills/quality/instance-quality-review.md` | Skill for automated review | GAP-012 PR |
| `02-architecture/adr/ADR-001-ai-branding-v2.md` | Architecture decision record | GAP-046 rollout |

---

## PR Checklist (to embed in PR template)

Copy into every AI Branding implementation PR:

```markdown
## Living Docs Compliance (per GAP-016)

If PR changes business logic, check relevant docs updated:

- [ ] `01-business/kitehub/{domain}/rules.md` — rules changed?
- [ ] `01-business/kitehub/{domain}/use-cases.md` — UC added/modified?
- [ ] `01-business/kitehub/{domain}/api-contract.md` — endpoints new/changed?
- [ ] `03-planning/database/database-design.md` — entity added?
- [ ] `03-planning/database/database-migration-plan.md` — migration added?
- [ ] `06-diagrams/plantuml/03-erd.puml` — relationships changed?
- [ ] `06-diagrams/plantuml/04-architecture-full.puml` — component added?
- [ ] `.claude/skills/api-design.md` — API spec updated?
- [ ] Tests added/updated for new logic?
- [ ] New user-facing feature? → `05-guides/` guide created?
```

---

## Verification Chain (per GAP-016)

Every business rule must have traceable chain:

```
BR-AIB-01 (rules.md)
  ↓ implements
UC-AIB-07 (use-cases.md)
  ↓ exposed via
POST /api/v1/branding/plan (api-contract.md)
  ↓ mapped by
@PostMapping in PlannerController.java
  ↓ validated by
PlannerControllerTest.java
```

Break chain = broken Living Docs rule = PR rejected.

---

## Implementation Progress Tracker

Update khi implement gaps:

| Gap | Impl PR | Business docs updated? | ERD updated? | Tests added? | Status |
|-----|---------|:----------------------:|:------------:|:------------:|:------:|
| GAP-007 | — | — | — | — | PENDING |
| GAP-008 | — | — | — | — | PENDING |
| GAP-009 | — | — | — | — | PENDING |
| GAP-010 | — | — | — | — | PENDING |
| GAP-011 | — | — | — | — | PENDING |
| GAP-012 | — | — | — | — | PENDING |
| GAP-013 | — | — | — | — | PENDING |
| GAP-014 | PR-wave-01-B1 | ✓ (this audit) | N/A | N/A | 🟡 IN PROGRESS |
| GAP-015 | — | — | — | — | PENDING |

---

## Governance

- **Review this audit:** Quarterly (or per AI Branding impl PR)
- **Owner:** Tech Lead
- **Enforcement:** PR template checklist + code review gate

---

## Log

- 2026-04-14 — Initial audit created as part of Wave 1 Foundation (GAP-016)
