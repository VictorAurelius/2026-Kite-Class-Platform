# Skills Index — Khi nao dung skill nao?

**Updated:** 2026-04-16 | Reorganized root skills into subfolders

---

## Quy trinh phat trien (theo thu tu)

1. `quality/pre-flight-check.md` — TRUOC khi bat dau module moi
2. `core/brainstorming-methodology.md` — Brainstorm cho moi PR
3. `core/task-breakdown-guide.md` — Chia tasks
4. `quality/pre-flight-check.md pr` — Check truoc khi code
5. `core/tdd-enforcement.md` — Viet test truoc
6. `backend/` hoac `frontend/` — Standards khi code
7. `core/two-stage-code-review.md` — Self-review truoc PR
8. `testing/testing-standards.md` — Verify test coverage

---

## Tat ca Skills

### Core Skills (Superpowers)
**Thu muc:** `core/`

| File | Mo ta | Dung khi |
|------|-------|----------|
| `brainstorming-methodology.md` | Quick brainstorm process | Bat dau moi PR |
| `task-breakdown-guide.md` | Task decomposition | Chia nho cong viec |
| `tdd-enforcement.md` | Test-first development | Truoc khi viet code |
| `two-stage-code-review.md` | Self-review checklist | Truoc khi tao PR |
| `systematic-debugging.md` | 4-phase debugging | Khi gap loi kho hieu |

### Quality & Audit Skills
**Thu muc:** `quality/`

| File | Mo ta | Dung khi |
|------|-------|----------|
| `quality/pre-flight-check.md` | 3-layer check: PR / Domain / Project | Truoc moi PR, milestone |
| `quality/business-gap-check.md` | Business logic gap analysis | Kiem tra business coverage |
| `quality/persona-based-business-review.md` | Role-play 10 tenant types → find gaps | Quarterly; before GA |
| `quality/simulation-gap-finder.md` | 3-axis matrix (personas × stages × categories) | Tim gaps toan dien sau design |
| `quality-audit/SKILL.md` | Master quality scoring /100 (10 categories) | Danh gia chat luong |
| `quality/ui-review/SKILL.md` | UI audit per-screen /128 | Sau moi frontend PR |
| `quality/business-logic-audit/SKILL.md` | Code ↔ rules.md mapping /100 | Truoc GA, sau wave |
| `quality/security-audit/SKILL.md` | Deep security /100 (deps, OWASP, auth) | Truoc production deploy |
| `quality/performance-audit/SKILL.md` | Performance baseline /100 (DB, API, bundle) | Truoc production deploy |
| `quality/api-contract-audit/SKILL.md` | API ↔ docs sync /100 | Sau thay doi endpoints |
| `quality/ops-readiness-audit/SKILL.md` | Production ops readiness /100 | Truoc GA deploy |
| `quality/design-pattern-audit/SKILL.md` | Anti-pattern hotspot scan /100 (5 categories from design-patterns.md §3) | Wave 7+ refactor planning, post-Wave 6 |
| `quality/script-review-checklist.md` | Script review checklist (bash/python) | PR co .sh/.py files |
| `quality/migration-review-checklist.md` | Flyway migration DBA checklist | PR co V*.sql files |
| `quality/gap-review/SKILL.md` | Peer-review checklist cho gap files | PR touch documents/04-quality/gaps/GAP-*.md |
| `quality/rule-review/SKILL.md` | ADR-like review cho rules docs | PR touch .claude/rules/*.md |
| `quality/rework-audit/SKILL.md` | Retroactive audit cho context-degraded PRs (GAP-199) | Sau wave, hoac khi nghi quality drift |
| `quality/cross-app-consistency.md` | Cross-app KiteHub↔KiteClass check | PR touch shared infra |

### Technical Standards

| File | Mo ta |
|------|-------|
| `backend/backend-standards.md` | Code style, API design, DB, enums, errors, Maven |
| `frontend/frontend-standards.md` | TypeScript, React, Shadcn, theme, i18n, a11y |
| `testing/testing-standards.md` | Spring Boot tests, frontend tests, E2E, security |
| `devops/devops-standards.md` | Docker scripts, CI/CD, deployment, cloud |
| `devops/terraform-cloud-deploy/SKILL.md` | Terraform review + AWS/OCI deploy strategy |

### Workflow Skills
**Thu muc:** `workflow/`

| File/Folder | Mo ta |
|-------------|-------|
| `workflow/start-session/SKILL.md` | /start-session — load context + session-lock check (GAP-193) |
| `workflow/continue/SKILL.md` | /continue — resume PR uu tien nhat |
| `workflow/check-pr/SKILL.md` | /check-pr — monitor CI + verify PR |
| `workflow/fix-pr/SKILL.md` | /fix-pr — fix PR issues |
| `workflow/start-pr/SKILL.md` | /start-pr — start new PR |
| `workflow/quality-plan/SKILL.md` | /quality-plan — generate PR plan tu audit gaps |
| `workflow/repo-status/SKILL.md` | /repo-status — remote health (GREEN→BLACK) |
| `workflow/pr-health.md` | /pr-health — PR compliance scanner (CI, tests, docs, audits) |
| `workflow/gap-triage.md` | /gap-triage — triage gaps, xep uu tien, assign sprint |
| `workflow/ci-failure-triage.md` | /ci-failure-triage — CI fail classification + fix guide |
| `workflow/wave-completion-check.md` | Wave completion gate (Level 7: audit suite) |
| `workflow/gap-to-pr-converter.md` | Convert gap → PR/wave voi template |
| `workflow/docs-freshness/SKILL.md` | Nhac update living docs (auto) |
| `workflow/development-workflow.md` | Day-to-day workflow |
| `workflow/priority-pr-planning.md` | PR prioritization |

### Reference (doc khi can)
**Thu muc:** `reference/`

| File | Mo ta |
|------|-------|
| `reference/architecture-overview.md` | System architecture overview |
| `reference/business-docs-3-layer.md` | 3-layer business docs |
| `reference/cross-service-data-strategy.md` | Data sharing between services |
| `reference/design-pattern-advisor.md` | Choose + apply design patterns |
| `reference/diagrams.md` | PlantUML/Mermaid setup |
| `reference/email-service.md` | Email service integration |
| `reference/ide-setup.md` | VS Code settings |
| `reference/plantuml-diagrams.md` | PlantUML diagram patterns |
| `reference/project-structure.md` | Folder structure best practice |
| `reference/service-docs-standard.md` | Service README/QUICK-START |
| `reference/ui-template-guide.md` | Figma→code, page checklist |

### Rules (conventions noi bo)
**Thu muc:** `.claude/rules/`

| File | Mo ta |
|------|-------|
| `skill-conventions.md` | Cach viet skill dung chuan |
| `design-patterns.md` | Mandatory design patterns + anti-patterns |
| `ai-branding-guidelines.md` | AI Branding feature rules |
| `output-review-mandate.md` | Master rule: moi output phai co review |

---

## Kiem tra chat luong

```bash
/quality-audit [target]        # Score ky thuat /100
/business-gap-check [target]   # Business coverage gaps
/pre-flight-check project      # Milestone check
/ui-review                     # UI per-screen /128
```
