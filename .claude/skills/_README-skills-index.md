# Skills Index — Khi nào dùng skill nào?

**Updated:** 2026-04-28 | Rebuilt during Wave Meta-Gov 1 Move 2 (closes GAP-252)

This index lists every skill under `.claude/skills/` so Claude (and humans)
can scan trigger conditions in one place. Source of truth for each row is
the skill file's own frontmatter `description`. When you add a new skill,
add a row here in the same PR — `scripts/check-skill-conventions.sh` warns
on drift between disk and this index.

---

## Quy trình phát triển (theo thứ tự cho mỗi PR)

1. `quality/pre-flight-check.md` — TRƯỚC khi bắt đầu module mới
2. `core/brainstorming-methodology.md` — Brainstorm cho mọi PR
3. `core/task-breakdown-guide.md` — Chia tasks
4. `quality/pre-flight-check.md pr` — Check trước khi code
5. `core/tdd-enforcement.md` — Viết test trước
6. `backend/` hoặc `frontend/` — Standards khi code
7. `core/two-stage-code-review.md` — Self-review trước PR (now incl. 5-tier severity rubric — GAP-254)
8. `testing/testing-standards.md` — Verify test coverage

---

## Core Skills (Superpowers)

**Folder:** `.claude/skills/core/`

| File | Description (trigger phrases) |
|------|-------------------------------|
| `core/brainstorming-methodology.md` | Dùng khi bắt đầu PR medium+ complexity, brainstorm scope/risks/edge cases |
| `core/systematic-debugging.md` | Dùng khi gặp lỗi khó hiểu — 4-phase: Reproduce → Trace → Root Cause → Defensive Fix |
| `core/task-breakdown-guide.md` | Dùng khi bắt đầu implement PR medium+ — chia thành tasks 2-5 phút có exact path |
| `core/tdd-enforcement.md` | Dùng trước khi viết code — test-first, RED → GREEN → REFACTOR |
| `core/two-stage-code-review.md` | Dùng trước khi merge PR — Stage 1 spec compliance + Stage 2 code quality + 5-tier severity rubric |

---

## Quality & Audit Skills

**Folder:** `.claude/skills/quality/` + `.claude/skills/quality-audit/`

### Master scoring + audits (output /100)

| File | Description (trigger phrases) |
|------|-------------------------------|
| `quality-audit/SKILL.md` | Dùng khi 'audit', 'quality check', 'kiểm tra chất lượng', 'ready to merge?' — 10 categories /100 |
| `quality/business-logic-audit/SKILL.md` | Dùng khi 'business audit', 'code đúng rules chưa' — code ↔ rules.md verification /100 (now with 3 eval-fixtures — GAP-253) |
| `quality/security-audit/SKILL.md` | Dùng khi 'security audit', 'pentest', 'dependency scan' — deep security /100 (now with 3 eval-fixtures — GAP-253) |
| `quality/performance-audit/SKILL.md` | Dùng khi 'perf audit', 'load test', 'kiểm tra hiệu năng' — performance baseline /100 |
| `quality/api-contract-audit/SKILL.md` | Dùng khi 'api audit', 'contract check', 'breaking change?' — API ↔ docs sync /100 |
| `quality/ops-readiness-audit/SKILL.md` | Dùng khi 'ops audit', 'production ready?', 'deploy checklist' — ops readiness /100 |
| `quality/ui-review/SKILL.md` | Dùng khi 'review UI', 'audit screenshots' — auto sau frontend PR; per-screen /128 (KC port 4700, KH 4701) |
| `quality/ui-review-prototype/SKILL.md` | Dùng khi 'audit prototype', 'review ui kits', 'check landing parity', 'kiểm tra prototype HTML' — HTML kit review (link-checker + landing-parity stricter than Tier 1 + state-coverage); closes GAP-264 |
| `quality/kit-production-parity/SKILL.md` | Dùng khi 'kit parity', 'review Track 2 port', 'kit vs production', 'back-port kit' — 4-layer V-model parity (要件定義/基本設計/詳細設計/コンポーネント設計) kit ↔ production, bidirectional (kit→prod port + prod→kit back-port); closes GAP-367 |
| `quality/design-pattern-audit/SKILL.md` | Dùng khi 'design pattern audit', 'God service đâu', 'anti-pattern hotspot' — score /100 vs `design-patterns.md` §3 |
| `quality/ai-branding-quality-gate/SKILL.md` | Dùng khi PR thay đổi AI Branding behavior (model swap, prompt change, §5 logic) — manual checklist /100; auto-trigger qua audit-gate |
| `quality/rework-audit/SKILL.md` | Dùng khi 'rework audit', 'context-degraded PRs', 'audit lại wave X' — re-audit PRs từ high-context-pressure sessions (GAP-199) |

### Pre-flight + business gap analysis

| File | Description (trigger phrases) |
|------|-------------------------------|
| `quality/pre-flight-check.md` | Dùng trước khi bắt đầu PR/domain mới — 3-layer check: PR / Domain / Project |
| `quality/business-gap-check.md` | Dùng khi 'gap check', 'business logic gaps', 'missing features' — phát hiện gaps |
| `quality/persona-based-business-review.md` | Dùng khi 'persona review', 'role-play review' — nhập vai persona → walk nghiệp vụ → tìm missing features |
| `quality/simulation-gap-finder.md` | Dùng khi 'simulate gaps', 'persona simulation' — 3-axis matrix simulation |
| `quality/cross-app-consistency.md` | Dùng khi 'cross-app', 'KiteHub vs KiteClass', 'shared infra' — verify shared conventions |

### Wave orchestration (multi-gap parallel execution)

| File | Description (trigger phrases) |
|------|-------------------------------|
| `quality/wave-pack-planner/SKILL.md` | Dùng khi 'plan wave', 'cluster gaps', 'wave-pack', 'next wave' — group ≥3 disjoint gaps thành wave-pack rồi spawn 3-5 parallel agents (codify Wave Obs 2026-04-28 5x speedup) |

### Review checklists (per PR file types)

| File | Description (trigger phrases) |
|------|-------------------------------|
| `quality/script-review-checklist.md` | Dùng khi PR có .sh/.py files — bash/python checklist |
| `quality/migration-review-checklist.md` | Dùng khi PR có Flyway V*.sql — DBA checklist |
| `quality/email-template-review/SKILL.md` | Use when PR touches email templates (kitehub-email, customer emails) — brand/legal/i18n/mobile gate |
| `quality/marketing-legal-review/SKILL.md` | Use when PR touches marketing copy or legal docs (TOS, Privacy, DPA) — VN PDPL + GDPR/CAN-SPAM compliance |
| `quality/gap-review/SKILL.md` | Dùng khi PR thêm/sửa GAP files — peer-review checklist trước status flip |
| `quality/rule-review/SKILL.md` | Dùng khi PR thêm/sửa `.claude/rules/*.md` — ADR-like checklist |
| `quality/hook-review/SKILL.md` | Dùng khi PR thêm/sửa `.claude/hooks/*.py` — 8-point rubric (matcher correctness + BLOCK/WARN gradient + override trailer + fail-safe + wiring + false-positive + idempotency + perf budget) |
| `quality/release-deploy/SKILL.md` | Dùng khi 'deploy version', 'release v1.0', 'go-live', 'tag release', 'production cutover', 'release checklist' — release checklist per `release-deploy-standard.md` |
| `quality/thesis-citation-extract/SKILL.md` | Dùng khi 'extract citations', 'trích dẫn luận văn', 'audit thesis cite', 'verify bibliography', 'orphan citation' — citation/bibliography audit cho luận văn |
| `quality/thesis-figure-curation/SKILL.md` | Dùng khi 'figure curation', 'thesis figures', 'caption', 'đánh số hình', 'INDEX figure' — curate caption + numbering hình cho chapter luận văn |

---

## Workflow Skills

**Folder:** `.claude/skills/workflow/`

### User-facing slash commands

| File | Description (trigger phrases) |
|------|-------------------------------|
| `workflow/start-session/SKILL.md` | Dùng khi 'start new session', '/start-session', 'tình trạng hiện tại' — load CLAUDE.md digest + open PRs + lock check |
| `workflow/end-session/SKILL.md` | Dùng khi 'end session', 'đóng session', 'kết thúc session', '/end-session' — archive session-lock vào `.claude/session-locks/archived/YYYY-MM-DD/` + 1-line summary (PRs/gaps/turns/elapsed). Phase 2 GAP-193. |
| `workflow/start-pr/SKILL.md` | /start-pr — Start a new PR with Superpowers methodology |
| `workflow/continue/SKILL.md` | /continue — Resume action ưu tiên nhất từ project plans |
| `workflow/check-pr/SKILL.md` | /check-pr — Monitor CI và verify chất lượng PR (dùng scripts) |
| `workflow/fix-pr/SKILL.md` | /fix-pr — Fix quality issues identified by /check-pr |
| `workflow/repo-status/SKILL.md` | Dùng khi 'status', 'health check', 'security' — checks CI/PRs/gaps/Dependabot/code-scanning → GREEN→BLACK level |
| `workflow/quality-plan/SKILL.md` | /quality-plan — Auto-generate PR plan + wave strategy sau audit |
| `workflow/session-docs-check/SKILL.md` | Dùng khi 'check docs', 'pre-merge check', '/session-docs-check' — 13-rule Living Docs matrix detection (Rule 13 = gap-DONE discipline) |

### Background + meta workflow

| File | Description |
|------|-------------|
| `workflow/docs-freshness/SKILL.md` | Nhắc update living docs sau mỗi PR/wave (auto, not user-invocable) |
| `workflow/wave-completion-check.md` | Wave completion gate — Level 7 audit suite gate |
| `workflow/pr-health.md` | Dùng khi 'check PR', 'PR health', 'PR compliance' — scan merged PRs vs workflow rules |
| `workflow/gap-triage.md` | Dùng khi 'triage gaps', 'gap nào ưu tiên', 'sprint assignment' — phân loại + assign |
| `workflow/gap-to-pr-converter.md` | Dùng khi 'convert gap → PR', 'fix gaps' — gap ID → branch name + template |
| `workflow/ci-failure-triage.md` | Dùng khi 'CI fail', 'tests broken' — systematic triage: identify → classify → fix |
| `workflow/development-workflow/SKILL.md` | Use when user says 'development workflow', 'PR workflow', 'branching strategy', 'how to ship'. End-to-end workflow planning → release. (Folder skill — Wave 9 split from 1221-line monolith) |
| `workflow/priority-pr-planning/SKILL.md` | Use when user says 'priority PR plan', 'urgent PR queue', 'plan PRs by priority'. Standards for temporary priority PR plans. (Folder skill — Wave 9 split from 800-line monolith) |

---

## Document Generation

**Folder:** `.claude/skills/document-generation/`

| File | Description (trigger phrases) |
|------|-------------------------------|
| `document-generation/excel/SKILL.md` | Use when generate Excel/xlsx — attendance, roster, financial, grade sheet, 'báo cáo điểm danh' |
| `document-generation/pdf/SKILL.md` | Use when generate PDF — invoice, certificate, transcript, 'hóa đơn', 'xuất PDF' |
| `document-generation/word/SKILL.md` | Use when generate Word/docx — contract, letter, policy, 'hợp đồng', 'công văn' |

---

## Backend / Frontend / Testing / DevOps Standards

**Folders:** `.claude/skills/{backend,frontend,testing,devops}/`

| File | Description |
|------|-------------|
| `backend/backend-standards.md` | Code style, API design, DB, enums, errors, Maven |
| `frontend/frontend-standards.md` | TypeScript, React, Shadcn, theme, i18n, a11y |
| `frontend/ui-template-guide.md` | Figma → code, page checklist, FE anti-patterns, gotchas |
| `testing/testing-standards.md` | Spring Boot tests, frontend tests, E2E, security |
| `devops/devops-standards.md` | Docker scripts, CI/CD, deployment, cloud |
| `devops/terraform-cloud-deploy/SKILL.md` | Dùng khi review Terraform / IaC, AWS/OCI deploy strategy |
| `devops/aws-smoke-test/SKILL.md` | Dùng khi 'smoke test AWS', 'verify Phase 2.3', 'kiểm tra AWS sau apply', 'AWS health check' — post-apply AWS resource verification |
| `devops/deploy-preflight-simulator/SKILL.md` | Dùng khi sắp tag release / push image / merge PR đổi terraform-aws hoặc workflow CI — preflight dependency + deploy simulation |

---

## Reference (load khi cần)

**Folder:** `.claude/skills/reference/`

| File | Description |
|------|-------------|
| `reference/architecture-overview.md` | System architecture overview |
| `reference/business-docs-3-layer.md` | 3-layer business docs (rules.md / use-cases.md / api-contract.md) |
| `reference/cross-service-data-strategy.md` | Data sharing between services |
| `reference/design-pattern-advisor.md` | Choose + apply design patterns |
| `reference/diagrams.md` | PlantUML/Mermaid setup |
| `reference/email-service.md` | Email service integration |
| `reference/ide-setup.md` | VS Code settings |
| `reference/plantuml-diagrams.md` | PlantUML diagram patterns |
| `reference/project-structure.md` | Folder structure best practice |
| `reference/service-docs-standard.md` | Service README/QUICK-START |
| `reference/ui-template-guide.md` | Figma→code, page checklist (mirror; canonical in frontend/) |

---

## Quick-Reference (in-skill helpers)

**Folder:** `.claude/skills/quick-reference/`

Helper docs co-located with the core skills they support. Loaded only when
a core skill points to them.

| File | Supports |
|------|----------|
| `quick-reference/brainstorming-question-templates.md` | `core/brainstorming-methodology.md` |
| `quick-reference/brainstorming-trade-off-matrix.md` | `core/brainstorming-methodology.md` |
| `quick-reference/design-decision-documentation.md` | `core/brainstorming-methodology.md` |
| `quick-reference/quick-brainstorm-template.md` | `core/brainstorming-methodology.md` |
| `quick-reference/review-checklists.md` | `core/two-stage-code-review.md` |
| `quick-reference/review-stage-decision-tree.md` | `core/two-stage-code-review.md` |
| `quick-reference/review-template.md` | `core/two-stage-code-review.md` |
| `quick-reference/systematic-debugging-4phases.md` | `core/systematic-debugging.md` |
| `quick-reference/systematic-debugging-checklist.md` | `core/systematic-debugging.md` |
| `quick-reference/task-breakdown-examples.md` | `core/task-breakdown-guide.md` |
| `quick-reference/task-breakdown-formula.md` | `core/task-breakdown-guide.md` |
| `quick-reference/tdd-git-hook.md` | `core/tdd-enforcement.md` |
| `quick-reference/tdd-phases.md` | `core/tdd-enforcement.md` |
| `quick-reference/tdd-workflow-diagram.md` | `core/tdd-enforcement.md` |

---

## Rules (project conventions, not skills)

**Folder:** `.claude/rules/` — read when authoring skills/rules or reviewing related PRs.

| File | Purpose |
|------|---------|
| `skill-conventions.md` | How to write a skill (read when creating a new skill) |
| `rule-change-process.md` | ADR-like governance for `.claude/rules/**` (semver + reviewer matrix) |
| `gap-done-discipline.md` | Gap → DONE discipline (AC checks, banned phrases) — paired with `session-docs-check` Rule 13 |
| `incident-to-rule-pipeline.md` | 5-stage pipeline: Detect → Classify → Rule+Enforce → Self-Test → Retro Log |
| `audit-to-gap-pipeline.md` | Audit issue → Gap → PR pipeline |
| `output-review-mandate.md` | 🔴 MASTER: every output has review standard + process |
| `meta-gap-priority.md` | 🔴 MASTER: meta gaps (skills/rules/workflow) > business-logic > feature gaps |
| `post-wave-audit-mandate.md` | 🔴 MASTER: audit suite ≤3 days post-wave; hook blocks non-compliant PRs |
| `design-patterns.md` | Mandatory design patterns + §3 BANNED anti-patterns |
| `ai-branding-guidelines.md` | AI Branding feature rules (incl. §11.4 migration test checklist) |
| `mcp-first-with-fallback.md` | MCP-first tool selection (GitHub, Postgres) with CLI fallback |
| `planning-docs-structure.md` | Layout + frontmatter rules for `documents/03-planning/` |
| `docs-folder-structure.md` | Generic README rule for top-level `documents/` folders |
| `logs-format-standard.md` | Structured JSON logs spec (implementation: GAP-114/115/116 Wave 7) |

---

## Drift detection

`scripts/check-skill-conventions.sh` (GAP-251 + GAP-252) emits a WARN when
the on-disk skill-file count differs from references in this index.
Re-run after every skill add / rename / delete. Cap WARN: 0.

---

## Quick-pick by task

```bash
/quality-audit [target]        # 10-category /100 score
/business-gap-check [target]   # business coverage gaps
/pre-flight-check project      # milestone check
/ui-review                     # UI per-screen /128
/start-session                 # load context after /clear
/repo-status                   # remote health snapshot
/session-docs-check            # pre-merge Living Docs gate (incl. Rule 13 gap-DONE)
```
