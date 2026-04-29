# Output Review Mandate

**Priority:** 🔴 CRITICAL — project-wide governance rule
**Version:** 1.1.3
**Created:** 2026-04-14
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (solo-dev — PATCH self-approve per `rule-change-process.md` §5)
**Applies to:** Every artifact (code, docs, gaps, audits, AI assets, contracts, generated reports, scripts, templates, logs) the project produces, plus every review process listed in §3 matrix

---

## 1. The Mandate

> **Mọi output (artifact) sinh ra trong dự án PHẢI có:**
> 1. **Review standard** documented (criteria to evaluate)
> 2. **Review process** executed (who, when, how)
> 3. **Review evidence** preserved (logs, reports, sign-offs)

Không có review = không được merge/deploy/publish.

---

## 2. What Counts as "Output"?

Mọi artifact tenant, user, dev, hay downstream system consume:

**Code:**
- Source code (Java, TypeScript)
- Scripts (bash, Python, SQL)
- Configuration (YAML, properties)

**Documentation:**
- Business docs (rules, use-cases, api-contract)
- Architecture docs
- Plans (wave, implementation, roadmap)
- Gap reports
- Quality audit reports
- Skills + rules (meta — self-governance)
- User-facing guides

**Generated Artifacts:**
- Database migrations
- Generated documents (invoices, certificates, transcripts, reports)
- AI-generated assets (banners, hero, images)
- Templates (UI, image, contract, email)
- Email content sent to users
- API responses (contracts)
- Screenshots
- Logs (format + retention)

**External-facing:**
- Website content
- Marketing copy
- Legal documents
- Customer communications

---

## 3. Review Standards Matrix

| Output Type | Review Standard | Process | Reviewer | Current Status |
|-------------|----------------|---------|----------|:--------------:|
| **Code** | two-stage-code-review (Stage 1+2+2.5) | Pre-merge | Peer + CI + pattern check | ✅ DONE |
| **UI screens** | ui-review /128 per-screen | After FE PR | Auditor | ✅ DONE |
| **Quality audit reports** | quality-audit 10 categories /100 | Periodic | Auditor | ✅ DONE |
| **Ops readiness** | ops-readiness-audit skill /100 | Post-wave + quarterly | Auditor | ✅ REFRESHED (2026-04-25, 52/100 — Sub-PR 5.6a #530; baseline 2026-04-19 was 49/100) |
| **Performance baseline** | performance-audit skill /100 | Post-wave + quarterly | Auditor | ✅ REFRESHED (2026-04-25, 63/100 — Sub-PR 5.6a #530; baseline 2026-04-19 was 58/100) |
| **Business docs implementation match** (code ↔ rules.md sync) | Living Docs rule (3-layer) | Same PR as code change | PR reviewer | ✅ DONE |
| **Business logic CORRECTNESS** (giá trị rule đúng thị trường + law) | BRD + stakeholder sign-off + compliance | Before launch + quarterly | Product + Business + Legal | ⚠️ PARTIAL — rule shipped 2026-04-29 (`.claude/rules/business-logic-review.md` Phase 1 of GAP-049, Wave Business Correctness Agent B); audit + stakeholder sign-offs → GAP-156 |
| **PRs** | check-pr skill | Pre-merge | Reviewer | ✅ DONE |
| **Wave plans** | Wave review checklist | Before launch | Team lead + architect | ⚠️ PARTIAL (skill exists, no formal review) |
| **Gap reports** | Gap review template | After creation | Peer | ✅ DONE (2026-04-20, GAP-170 — `.claude/skills/quality/gap-review/` + `_REVIEW-TEMPLATE.md`) |
| **Gap closure (Status flip → DONE)** | `gap-done-discipline.md` (AC checked, no banned phrases, follow-up filed for any deferral) | Pre-merge of closing PR | Author + reviewer + skill detection | ✅ DONE (2026-04-27 — `.claude/rules/gap-done-discipline.md` + `session-docs-check` Rule 13 detector + 3-fixture self-test in this PR; closes GAP-235 Sub-PR G silent-deferral incident) |
| **Coverage gaps in rules/skills (incidents)** | `incident-to-rule-pipeline.md` (5-stage: Detect → Classify → Rule+Enforce → Self-Test → Retro Log) | When user/reviewer flags a miss | Author + reviewer | ✅ DONE (2026-04-27 — `.claude/rules/incident-to-rule-pipeline.md` paired with `rule-change-process.md` §6.5 Enforcement Parity Mandate in this PR) |
| **Architecture docs** | ADR process | When written | Tech lead + team | ✅ DONE (2026-04-20, GAP-172 — `documents/02-architecture/adr/README.md` + `_TEMPLATE.md`) |
| **Skills (meta)** | skill-conventions.md rules + `scripts/check-skill-conventions.sh` | Pre-merge (CI) | Lead + CI | ✅ DONE (2026-04-28, GAP-251 — script + 3 fixtures + CI job `skill-conventions`; baseline 44 PASS / 38 WARN / 0 FAIL; 21 grandfathered skills tracked for Wave 9 cleanup) |
| **Rules docs (meta)** | ADR-like | Pre-merge | Lead + team | ✅ DONE (2026-04-20, GAP-171 — `.claude/rules/rule-change-process.md` + `.claude/skills/quality/rule-review/`) |
| **Templates (UI/image)** | GAP-011 5 criteria | Before publish | Designer + lead | ⚠️ PLANNED (GAP-011) |
| **Email templates** | Brand + legal check | Before send | Marketing + legal | ✅ DONE (2026-04-20, GAP-173 — `.claude/skills/quality/email-template-review/`) |
| **AI-generated assets** | Quality gate /100 + content safety + `ai-branding-quality-gate` skill | Auto + manual | Automated + admin | ⚠️ PARTIAL — governance scaffold DONE 2026-04-26 (GAP-223 Sub-PR 223.1: skill `quality/ai-branding-quality-gate/` + audit-gate rule + `ai-branding-guidelines.md` §11.4 Migration test checklist + baseline audit 62/100); GAP-012 §5 5 Strategy-pattern checks + GAP-018 3-stage moderation pipeline DONE Wave 4 scaffold-only; real WCAG/visual-regression/ML classifier tracked GAP-226/227/228 Wave 8+; umbrella GAP-225 |
| **Contracts (Word)** | Legal review | Before use | Lawyer | ❌ **VIOLATION** |
| **Generated PDFs/Excel** | QA checklist + visual regression | Before delivery | QA | ⚠️ PLANNED (GAP-047) |
| **Database migrations** | migration-review-checklist skill | Pre-merge | DBA + peer | ✅ DONE |
| **Scripts (bash/Python)** | script-review-checklist skill | Pre-merge | Peer | ✅ DONE |
| **API contracts** | api-contract-audit skill + schema validation | Pre-merge + runtime | Consumer/producer | ⚠️ PARTIAL (audit skill exists, no consumer-driven contract tests yet) |
| **Screenshots** | Manual + automated audit | Capture time | Auditor | ⚠️ PARTIAL (ui-review skill) |
| **Logs format** | Log standard doc | Audit period | SRE | ✅ DONE (2026-04-20, GAP-175 — `.claude/rules/logs-format-standard.md`; implementation tracked GAP-114/115/116 Wave 7) |
| **README freshness** | `scripts/check-readme-freshness.sh` (`**Last Updated:**` date check, 30d WARN / 90d FAIL, exempt via `<!-- readme-freshness-exempt: <reason> -->`) | Pre-merge (CI) | CI + reviewer | ✅ DONE (2026-04-28, GAP-255 — script + 5 self-test fixtures + CI job `readme-freshness`; baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs) |
| **Marketing copy** | Brand + legal | Before publish | Marketing + legal | ✅ DONE (2026-04-20, GAP-174 — `.claude/skills/quality/marketing-legal-review/`) |
| **Legal docs sent to tenants** | Full legal review | Before issue | Lawyer | ✅ DONE (2026-04-20, GAP-174 — shared `marketing-legal-review` skill covers TOS/Privacy/DPA) |

**Legend:**
- ✅ DONE — standard exists, process runs
- ⚠️ PARTIAL — standard partial or process informal
- ❌ VIOLATION — no standard/process, remediation needed
- ⚠️ PLANNED — remediation tracked in gap

---

## 4. Current Violations Summary

### 🔴 CRITICAL violations (ALL RESOLVED 2026-04-20 — Wave 8b)

| # | Output | Status | Closed by |
|---|--------|--------|-----------|
| 1 | Gap reports | ✅ DONE | GAP-170 (PR #402) |
| 2 | Rules docs | ✅ DONE | GAP-171 (PR #402) |
| 3 | Architecture docs | ✅ DONE | GAP-172 (PR #401) |
| 4 | Email templates | ✅ DONE | GAP-173 (PR #403) |
| 5 | Marketing copy / legal docs | ✅ DONE | GAP-174 (PR #403) |
| 6 | Logs format | ✅ DONE | GAP-175 (PR #405; implementation Wave 7) |

**Wave 8b outcome:** VIOLATION count 6 → 0 across original §4 critical list. Six meta-P1 additions (GAP-193/194/195/198/199/201) also shipped Phase 1 in same wave.

### ⚠️ PARTIAL (exists but informal)

- Wave plans
- Skills self-review
- Screenshots scoring
- Templates (planned in GAP-011)
- API contracts (audit skill exists, no consumer-driven contract tests)

### ⚠️ PLANNED (tracked in gaps)

- AI asset quality gate (GAP-012, GAP-018)
- Generated document QA (GAP-047)

---

## 5. Remediation Plan

Create **GAP-048: Output Review Standards Coverage** to track closing all violations.

Each violation → dedicated action:

### 5.1 Gap Reports (meta-level)
- Add peer review step trong `gap-to-pr-converter.md`
- Template cho gap review: validates Problem clear, AC measurable, dependencies identified
- Gap không được status 🟡 PLANNED cho đến khi peer-reviewed

### 5.2 Rules + Skills (meta-governance)
- ADR template cho rules changes
- Lead + 1 dev review trước merge
- Changelog per rule file
- Version + last-reviewed date trong front-matter

### 5.3 Architecture Docs (ADR)
- `documents/02-architecture/adr/` folder
- ADR template (context, decision, consequences)
- Link ADR từ docs referencing decisions
- Reviewed in architecture meeting

### 5.4 Database Migrations
- Migration review checklist:
  - [ ] Backward compatible?
  - [ ] Rollback script provided?
  - [ ] Index impact assessed?
  - [ ] Data migration safe (no lock holds)?
  - [ ] Tested on staging with production-like data?
- DBA approval required for V-migrations

### 5.5 Scripts
- Script linting (shellcheck for bash, ruff for Python)
- Security review (no `eval`, no hardcoded secrets)
- Test coverage or at least `--dry-run` mode
- Documentation: purpose, usage, edge cases

### 5.6 API Contracts
- OpenAPI spec updates in same PR as controller changes
- Contract tests (Pact or similar)
- Backward compat check automated
- Breaking change requires version bump + deprecation notice

### 5.7 Email Templates
- Review checklist:
  - [ ] Brand colors + logo applied
  - [ ] Legal footer included (unsubscribe, address)
  - [ ] i18n (Vietnamese + English if needed)
  - [ ] Variables work (preview with sample data)
  - [ ] Mobile-responsive
- Marketing + legal sign-off for customer-facing

### 5.8 Marketing & Legal Docs
- Legal counsel review
- Compliance checklist
- Version control + dated signatures
- Archive previous versions

### 5.9 Logs Standard
- Structured logging (JSON)
- Required fields: timestamp, service, level, tenantId, traceId
- Retention policy documented
- PII scrubbing rules

---

## 6. Enforcement

### 6.1 Pre-commit hooks
```bash
# .husky/pre-commit
- Check PR touches any output type → verify review doc exists
- Example: migration added → require DBA checklist in PR
```

### 6.2 PR template additions

```markdown
## Output Review Checklist

Check all output types modified trong PR:
- [ ] Code — two-stage-code-review completed
- [ ] Business docs — updated if logic changed
- [ ] API contract — OpenAPI spec updated
- [ ] DB migration — DBA checklist
- [ ] Scripts — linted + tested
- [ ] Email templates — brand + legal check
- [ ] Architecture — ADR created if significant decision
- [ ] Gap files — peer reviewed
- [ ] Skills/Rules — lead approved
- [ ] Templates — designer reviewed (if UI/image)
```

### 6.3 Automated detection

```bash
# CI check
- git diff detect output types
- fail if review evidence missing
```

### 6.4 Quarterly audit

```
/quality-audit — add category "Review Standards Coverage"
Measure % of outputs with documented standard + process
Target: 100% ✅ by end of Q2 2026
```

---

## 7. Responsibility Matrix (RACI)

| Output Type | Responsible | Accountable | Consulted | Informed |
|-------------|------------|-------------|-----------|----------|
| Code | Dev | Tech lead | Peer | Team |
| Business docs | Dev | PM | Stakeholders | Team |
| Architecture | Architect | Tech lead | Team | Stakeholders |
| Migrations | DBA | Tech lead | Dev | SRE |
| Scripts | Dev | Security lead | Peer | Ops |
| API contracts | Dev | API owner | Consumers | Clients |
| Email templates | Marketing | Brand lead | Legal | Customers |
| AI assets | System | AI lead | Admin | Tenant |
| Templates | Designer | Creative lead | PM | Tenants |
| Generated docs | System | QA lead | Legal (contracts) | Tenant |
| Legal docs | Legal | CEO | Tech lead | All tenants |
| Logs | SRE | SRE lead | Security | Ops |

---

## 8. Exceptions

Cases khi review có thể lighter:

| Case | Lighter process |
|------|-----------------|
| Typo fix (single char) | Commit with message, CI passes |
| Comment-only change | Same as typo |
| Dev-only config | Reduced review (sanity check) |
| Emergency hotfix | Fast-track review + post-merge audit |

**Never skipped:** security, migrations, legal, customer-facing.

---

## 9. Integration với Existing

- `CLAUDE.md` Living Docs rule → subset of this mandate
- `.claude/rules/skill-conventions.md` → applies to skills output
- `.claude/rules/design-patterns.md` → applies to code output
- `.claude/rules/ai-branding-guidelines.md` → applies to AI asset output
- Extends all trên với universal mandate

---

## 10. Related

- Gap: GAP-048 (new — tracks remediation)
- Skill: `two-stage-code-review.md`
- Skill: `ui-review/SKILL.md`
- Skill: `quality-audit/SKILL.md`
- Skill: `quality/business-gap-check.md`
- Rules: `skill-conventions.md`, `design-patterns.md`, `ai-branding-guidelines.md`

---

## 11. Log

- **2026-04-29** (v1.1.3): PATCH — flipped §3 matrix row "Business logic CORRECTNESS" from ❌ VIOLATION (GAP-049) → ⚠️ PARTIAL — rule shipped 2026-04-29 (`.claude/rules/business-logic-review.md`); audit + stakeholder sign-offs → GAP-156. Phase 1 of GAP-049 scope split (Wave Business Correctness Agent B) — review standard shipped, audit-execution + sign-off sub-tasks tracked in GAP-156 follow-up. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — matrix-row state sync, no constraint loosening; new business-logic-review rule itself is MAJOR-scope but self-contained per its own §10 Log entry). §4 VIOLATION list narrows further (this was the last unaddressed CRITICAL row).
- **2026-04-28** (v1.1.2): PATCH — added §3 matrix row "README freshness" (CI script + workflow job + 5 self-test fixtures, baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs); flipped "Skills (meta)" row from ⚠️ PARTIAL to ✅ DONE post-Wave Meta-Gov 1 Sub-PR C (#610). Closes GAP-255 row addition + GAP-251 status sync. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — factual coverage update, no constraint loosening).
- **2026-04-28** (v1.1.1): PATCH — added missing `**Applies to:**` frontmatter field flagged by `scripts/check-rule-frontmatter.sh` (GAP-250 self-test). No content change; promotes Priority field to first line per project convention. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — frontmatter sync, no constraint loosening).
- **2026-04-27** (v1.1.0): MINOR — added §3 matrix rows: "Gap closure (Status flip → DONE)" (closes GAP-235 silent-deferral incident; enforced by `gap-done-discipline.md` + `session-docs-check` Rule 13) and "Coverage gaps in rules/skills (incidents)" (closes the meta-process gap user surfaced; enforced by `incident-to-rule-pipeline.md` paired with `rule-change-process.md` §6.5). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5; no constraint loosening — only adds coverage rows for previously-uncovered output types). Motivation: user "có quy trình khi thêm 1 skill, 1 rules vào dự án chưa, mà vẫn miss kiểu này" — matrix had no row for gap closure or for coverage-gap discovery, so silent misses had no review standard.
- **2026-04-26 (v1.0.2, later):** PATCH — re-sync §3 matrix line 75 post-Sub-PR 223.1 shipping. Row now states governance scaffold DONE (skill + audit-gate rule + §11.4 + baseline 62/100); real WCAG/vrg/ML tracked GAP-226/227/228 Wave 8+. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve). Motivation: prior v1.0.1 row referenced GAP-223 Sub-PR 223.1 as future scope; now landed.
- **2026-04-26 (v1.0.1):** PATCH — sync §3 matrix line 75 "AI-generated assets" row from "⚠️ PLANNED (GAP-012, 018)" → "⚠️ PARTIAL — scaffolded only" with explicit deferred items + cluster-fix references (GAP-225 umbrella + GAP-223 Sub-PR 223.1). Also backfilled frontmatter Version + Last-Reviewed + Reviewer-Approver per `rule-change-process.md` §3 backfill-on-next-edit policy. Reviewer: @nguyenvankiet (solo-dev self-approve per §5 matrix for PATCH — factual correctness fix, no constraint loosening). Motivation: cross-gap audit (GAP-225) found GAP-012 + GAP-018 both shipped Wave 4 with scaffold-only Status DONE despite explicit deferred items; matrix row claiming "PLANNED" was 12 days stale.
- **2026-04-20 — Wave 8b shipped: 6 CRITICAL §4 VIOLATIONS closed** in one wave. 6 parallel worktree-isolated agents (8b-A..F) merged PRs #401/402/403/404/405/406. Closed: GAP-170 gap reports (review template + checklist + skill), GAP-171 rules docs (rule-change-process + rule-review skill), GAP-172 architecture ADRs (README + MADR template), GAP-173 email templates (review skill + 40-point checklist), GAP-174 marketing + legal (VN PDPL/Advertising/Consumer Protection-primary compliance checklist), GAP-175 logs format (structured JSON rule with PII scrubbing). Also shipped Phase 1 of 6 meta-P1/P2 from action-1: GAP-193 (start-session skill), GAP-194 (shellcheck+ruff CI — blocking; 35 warnings non-blocking), GAP-195 (starter-kit diff tooling + retro-sync runbook), GAP-198 (FE↔BE contract ADR-016 oasdiff strategy), GAP-199 (rework-audit skill), GAP-201 (tenant off-boarding runbook + 3-layer docs). §4 VIOLATIONS: 6 → **0**. 12 gap files updated with status + log entries; matrix rows moved from ❌ to ✅ DONE.
- **2026-04-25 — Wave 5 post-wave audit suite refresh shipped (Sub-PR 5.6a #530, closes GAP-214):** all 5 audits ran in parallel (4 Explore agents + parent quality refresh). Scores: API contract 95/100 (A), Security 85/100 (B, +9 vs 2026-04-17), Performance 63/100 (D, +5 vs baseline), Ops Readiness 52/100 (F, +3 vs baseline), Quality refresh 78/100 (C+, +1 honest baseline). 4 P0 + 5 P1 + 8 P2/P3 gaps filed (GAP-215..219). §3 matrix rows for Ops + Performance flipped from ⚠️ BASELINE to ✅ REFRESHED. Sub-PR 5.6b (#X) shipped P0 fixes (GAP-215 cache, GAP-216 soft-cap canary, GAP-218 font runbook + Dockerfile assertion) + Wave 5 closure (sample gallery, ADR-019 ACCEPTED, MiniMax ADOPTED). GAP-217 PARTIAL — alert rules filed in helm + docker prometheus configs, routing depends on GAP-120 Alertmanager.
- 2026-04-19 — Audit catch-up Part A (3/5) shipped: resolved 2 first-ever VIOLATIONS via baseline capture — ops-readiness 49/100 (PR #365, 15 gaps, GAP-111 → GAP-125) and performance 58/100 (PR #364, 10 gaps, GAP-126 → GAP-135). Business-logic refresh after 27-day drift caught 7 gaps (PR #366, 65/100, GAP-104 → GAP-110). Status in §3 matrix: ops-readiness + performance now BASELINE — subsequent audits measure delta against this. Remaining Part A: ui-review /128 refresh (8d stale) + quality-audit /100 refresh.
- 2026-04-16 — Resolved 2 violations: Scripts (script-review-checklist skill), DB migrations (migration-review-checklist skill). API contracts moved to PARTIAL (audit skill exists). Remaining: 6 critical violations.
- 2026-04-14 — Rule established; 9 critical violations identified; remediation via GAP-048
