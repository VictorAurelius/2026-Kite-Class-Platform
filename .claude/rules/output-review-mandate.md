# Output Review Mandate

**Priority:** 🔴 CRITICAL — project-wide governance rule
**Version:** 1.15.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-05-19
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.15.0 MINOR self-approve per `rule-change-process.md` §5; adds §3 matrix row "Thesis report / academic deliverable" tracking new rule `.claude/rules/thesis-content-standard.md` v1.0.0 (paired same-PR Wave 102 META 2026-05-19); no constraint loosening — closes coverage gap exposed bởi Wave 102 GAP-688 closure audit 82/100 B- missing 7 user-flagged + 43 agent persona-simulation findings; META P0 force-multiplier. v1.14.0 (kept): adds §3 matrix row "VN-localization audit checklist (cross-bucket)" tracking new rule `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 (paired same-PR Wave 100 Bucket D 2026-05-19, closes GAP-680 META P1). v1.13.0 (kept): adds §3 matrix row "Session-end context check". v1.12.3 (kept): see §11 Log for entries v1.12.3 .. v1.12.0 + [`_examples/output-review-mandate-log-history.md`](_examples/output-review-mandate-log-history.md) for entries v1.11.0 .. v1.0.0)
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

**Code:** source code (Java, TypeScript), scripts (bash, Python, SQL), configuration (YAML, properties).

**Documentation:** business docs (rules, use-cases, api-contract), architecture docs, plans (wave, implementation, roadmap), gap reports, quality audit reports, skills + rules (meta — self-governance), user-facing guides.

**Generated artifacts:** database migrations, generated documents (invoices, certificates, transcripts, reports), AI-generated assets (banners, hero, images), templates (UI, image, contract, email), email content sent to users, API responses (contracts), screenshots, logs (format + retention).

**External-facing:** website content, marketing copy, legal documents, customer communications.

---

## 3. Review Standards Matrix

> **Volatile audit state** (scores, deltas, gap links per refresh) lives in [`documents/04-quality/audits/audits-index.csv`](../../documents/04-quality/audits/audits-index.csv) — canonical source. **Current Status column = terse audit-level verdict + date pointer.** Refresh post-wave = update CSV only (not this rule body). Per Wave 99 streamline round 3 lesson — eliminates ~80% rule churn from per-wave audit refresh cycles.

| Output Type | Review Standard | Process | Reviewer | Current Status |
|-------------|----------------|---------|----------|:--------------:|
| **Code** | two-stage-code-review (Stage 1+2+2.5) | Pre-merge | Peer + CI + pattern check | ✅ DONE |
| **UI screens** | ui-review /128 per-screen | After FE PR | Auditor | ✅ 110.6/128 A (2026-05-19, Wave 98 5-screen sample) — see `audits-index.csv` |
| **Quality audit reports** | quality-audit 11 categories /110→/100 | Periodic | Auditor | ✅ 90/110 B+ (2026-05-19, Wave 98; PASS Phase 1 BETA ≥80 +10 + PROD MAJOR ≥85 +5 buffer) — see `audits-index.csv` |
| **Ops readiness** | ops-readiness-audit skill /100 | Post-wave + quarterly | Auditor | ⚠️ 77/100 C+ (2026-05-18, Wave 94c) — see `audits-index.csv`; 3 P0 carry GAP-257/144/612 |
| **Performance baseline** | performance-audit skill /100 | Post-wave + quarterly | Auditor | ✅ 86/100 B+ (2026-05-15, Wave 85) — see `audits-index.csv` |
| **Security baseline** | security-audit skill /100 + **v2 audit format mandatory per GAP-564** (per-control evidence block — see `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md`) | Post-wave + quarterly | Auditor | ✅ 93/100 A (2026-05-18, Wave 94c v2 27/27 evidence blocks) — see `audits-index.csv` |
| **Business logic implementation** | business-logic-audit skill /100 | Post-wave + quarterly | Auditor | ⚠️ 73/100 C+ PARTIAL FAIL Cat 1 (2026-05-19, Wave 98) — see `audits-index.csv`; path 80 PASS via GAP-664/666 |
| **Business docs implementation match** (code ↔ rules.md sync) | Living Docs rule (3-layer) | Same PR as code change | PR reviewer | ✅ DONE |
| **Business logic CORRECTNESS** (giá trị rule đúng thị trường + law) | BRD + stakeholder sign-off + compliance | Before launch + quarterly | Product + Business + Legal | ⚠️ PARTIAL — rule shipped 2026-04-29 (`business-logic-review.md` Phase 1 of GAP-049); audit + sign-offs → GAP-156 |
| **PRs** | check-pr skill | Pre-merge | Reviewer | ✅ DONE |
| **Wave plans** | Wave review checklist | Before launch | Team lead + architect | ⚠️ PARTIAL (skill exists, no formal review) |
| **Wave closure scope completeness** | `wave-closure-scope-completeness.md` §3 Scope-Completeness Reconciliation table | At wave closure PR (draft → complete flip) | Author + reviewer + paired follow-up gap files | ✅ DONE (2026-05-18 — rule v1.0.0 + Wave 92 retroactive self-test) |
| **Docs archival cadence** | `docs-archival-cadence.md` §2 cadence table (audits 90d / session-handoffs 30d / wave plans 60d POST closure / pr-logs 180d) | Per artifact type cadence | Author + reviewer + future stale-check script | ✅ DONE (2026-05-18 — rule v1.0.0 Rule 1/4 docs scaling pack) |
| **Docs subfolder maturity** | `docs-subfolder-maturity.md` §2 threshold (≥5 files OR ≥2 contributors OR reviewer approval OR sister-pattern) | Pre-merge of PR creating new subdir | Author + reviewer | ✅ DONE (2026-05-18 — rule v1.0.0 Rule 2/4 docs scaling pack) |
| **Docs folder volume budget** | `docs-folder-volume-budget.md` §2 cap (50 time-bound / 100 static / 200 active gaps) | Per-folder when add file pushes count over cap | Author + reviewer + future monitor | ✅ DONE (2026-05-18 — rule v1.0.0 Rule 3/4 docs scaling pack) |
| **Docs filename prefix convention** | `docs-filename-prefix-convention.md` §2 5-tier taxonomy + §4 audience frontmatter recommended | Per file creation/rename | Author + reviewer + future grep detector | ✅ DONE (2026-05-18 — rule v1.0.0 Rule 4/4 docs scaling pack) |
| **Gap reports** | Gap review template | After creation | Peer | ✅ DONE (2026-04-20, GAP-170) |
| **Gap closure (Status flip → DONE)** | `gap-done-discipline.md` (AC checked, no banned phrases, follow-up filed for any deferral) | Pre-merge of closing PR | Author + reviewer + skill detection | ✅ DONE (2026-04-27 — rule + `session-docs-check` Rule 13 detector) |
| **Gap folder organization** (filesystem location mirrors CSV phase + per-phase DONE archive) | `gap-folder-organization.md` v2.0.0 §2 phase-only design + `scripts/check-gap-folder-location.sh` (3 modes) | Pre-merge of gap CRUD PR | Author + reviewer + CI script | ⚠️ PARTIAL (2026-05-18 — Wave 95 PR1.5 v2.0.0 phase-only design supersedes status-driven v1.0.0 after outside-in audit; mass migration PR2 queued) |
| **Diagram format selection** (Mermaid / PlantUML / ASCII per use case) | `diagram-format-selection.md` v1.0.0 §2 selection matrix + reviewer-checklist | Pre-merge of PR touching `documents/**/*.md`, `.claude/rules/**/*.md`, `.claude/skills/**/*.md` containing diagram | Author + reviewer | ⚠️ PARTIAL (2026-05-18 — rule + self-test email-architecture.md ASCII → Mermaid rewrite shipped Wave 96 PR2) |
| **Coverage gaps in rules/skills (incidents)** | `incident-to-rule-pipeline.md` (5-stage) | When user/reviewer flags a miss | Author + reviewer | ✅ DONE (2026-04-27 — rule paired with `rule-change-process.md` §6.5) |
| **Architecture docs** | ADR process | When written | Tech lead + team | ✅ DONE (2026-04-20, GAP-172) |
| **Skills (meta)** | skill-conventions.md rules + `scripts/check-skill-conventions.sh` | Pre-merge (CI) | Lead + CI | ✅ DONE (2026-04-28, GAP-251) |
| **Rules docs (meta)** | ADR-like | Pre-merge | Lead + team | ✅ DONE (2026-04-20, GAP-171 — `rule-change-process.md` + `quality/rule-review/`) |
| **Templates (UI/image)** | GAP-011 5 criteria | Before publish | Designer + lead | ⚠️ PLANNED (GAP-011) |
| **Email templates** | Brand + legal check | Before send | Marketing + legal | ✅ DONE (2026-04-20, GAP-173 — `quality/email-template-review/`) |
| **AI-generated assets** | Quality gate /100 + content safety + `ai-branding-quality-gate` skill | Auto + manual | Automated + admin | ⚠️ PARTIAL — governance scaffold DONE 2026-04-26 (GAP-223 Sub-PR 223.1); real WCAG/visual-regression/ML classifier → GAP-226/227/228 Wave 8+; systemic scaffold-as-DONE pattern → [GAP-225](../../documents/04-quality/gaps/closed/GAP-225-scaffolded-as-done-governance-closure-umbrella.md) Phase 1 DONE 2026-04-29; Phase 2-4 future scope |
| **Contracts (Word)** | Legal review | Before use | Lawyer | ❌ **VIOLATION** |
| **Generated PDFs/Excel** | QA checklist + visual regression | Before delivery | QA | ⚠️ PLANNED (GAP-047) |
| **Database migrations** | migration-review-checklist skill | Pre-merge | DBA + peer | ✅ DONE |
| **Scripts (bash/Python)** | script-review-checklist skill | Pre-merge | Peer | ✅ DONE |
| **API contracts** | api-contract-audit skill + schema validation | Pre-merge + runtime | Consumer/producer | 🔴 76/100 C FAIL (2026-05-19, Wave 98) — see `audits-index.csv`; path 82 PASS via GAP-662/663/664 cluster ~3.25h |
| **Screenshots** | Manual + automated audit | Capture time | Auditor | ⚠️ PARTIAL (ui-review skill) |
| **Logs format** | Log standard doc | Audit period | SRE | ✅ DONE (2026-04-20, GAP-175 — `logs-format-standard.md`; implementation GAP-114/115/116 Wave 7) |
| **README freshness** | `scripts/check-readme-freshness.sh` (`**Last Updated:**` date check, 30d WARN / 90d FAIL) | Pre-merge (CI) | CI + reviewer | ✅ DONE (2026-04-28, GAP-255) |
| **Meta CSV indexes** (rules / ADRs / gaps + future skills + audits) | `meta-csv-index-pattern.md` §3 (CSV + query helper + CI validator + CI wire) + 100% coverage parity | Pre-merge (CI `meta-csv-indexes` + `gap-status-csv`) | CI + reviewer | ✅ DONE (2026-05-12, GAP-485 Tier 1+2; Tier 3 skills + audits → GAP-490) |
| **Marketing copy** | Brand + legal | Before publish | Marketing + legal | ✅ DONE (2026-04-20, GAP-174) |
| **Legal docs sent to tenants** | Full legal review | Before issue | Lawyer | ✅ DONE (2026-04-20, GAP-174 — shared `marketing-legal-review` covers TOS/Privacy/DPA) |
| **HTML/JSX prototypes** (`documents/02-architecture/design-system/ui_kits/**`) | Per-screen `/128` rubric (extends `quality/ui-review/SKILL.md`) + WCAG AA self-measurement + 100-item AC checklist + integration smoke test + landing parity script `_shared/scripts/check-ui-kits-landing.sh` | Pre-merge per kit PR self-report + post-merge integration smoke test + user vibe-check + landing parity in CI (Tier 3 GAP-265) | Author self-review + reviewer integration check + user accepts | ⚠️ PARTIAL (Phase 1 standard + Tier 1 landing-parity script 2026-04-29 GAP-263; Phase 2 ui-review-prototype → GAP-264; Phase 3 hook/CI → GAP-265) |
| **Root README** (`README.md` at repo root) | `readme-content-discipline.md` §2 stable-only allowlist + §3 volatile-content denylist + §4 borderline decision rule + §11 self-test grep regex | Pre-merge reviewer manual; Phase 2 CI + Phase 3 hook deferred per rule §8 Open Items | Reviewer + author | ⚠️ PARTIAL (Phase 1 rule + same-PR README rewrite shipped 2026-04-29) |
| **UI/Design scope completeness (4-layer V-model)** | `.claude/rules/design-layer-coverage.md` §2 matrix per scope-unit — verify all 4 layers (要件定義 / 基本設計 / 詳細設計 / コンポーネント設計) have artifact pointers | Pre-merge reviewer checklist + PR template checkbox; reference: `dossier/16-design-layer-mapping.md` | Author self-review + reviewer 4-layer check | ⚠️ PARTIAL (rule + dossier shipped 2026-04-30; quarterly `quality-audit` 4-layer sample audit pending) |
| **AWS verification reports** (`documents/04-quality/audits/aws-verification/**`) | `.claude/rules/agent-aws-access.md` §2 Tier 1 read-only allowlist + §2.2 banned secret-reads + §5 mandatory artifact format | Pre-merge reviewer command-tier check; Phase 2 skill `aws-smoke-test` + smoke scripts deferred → GAP-438 | Author self-review + reviewer | ⚠️ PARTIAL (Phase 1 rule + Phase 3 first artifact 2026-05-08; Phase 2 skill + Phase 4 memory → GAP-438) |
| **Acceptance test CSVs** (`documents/05-guides/operations/acceptance-tests/**`) per-release manual walkthrough matrix | `.claude/rules/test-artifact-format-standard.md` §1-§5 + `.claude/rules/dev-readable-doc-language.md` §2 | Pre-release-tag reviewer manual + `bash scripts/render-acceptance-test-xlsx.sh`; pre-commit BOM hook + CI detector deferred ≥7 ngày | Author self-review + reviewer | ✅ DONE (2026-05-14 — Wave 72b Bucket G) |
| **Dev-readable doc language** (gap files, runbooks, planning, audit, acceptance tests narrative) | `.claude/rules/dev-readable-doc-language.md` §1 Vietnamese narrative + §3 acceptable English + §4 mixed-language code-switch | Pre-merge reviewer-checklist per §7.1; CI grep + memory auto-load deferred ≥7 ngày | Author self-review + reviewer | ✅ DONE (2026-05-14 — Wave 72b Bucket G) |
| **Context budget** (auto-load size per session) | `.claude/rules/context-budget-mandate.md` §1 (<120k base; rules ≥1k tokens must `paths:` frontmatter OR `## Auto-load justification` OR hook-covered) | Pre-merge reviewer per §6.1; future `scripts/check-context-budget.sh` deferred ≥7 ngày | Reviewer + author | ✅ DONE (2026-05-14 — Wave 73 Bucket D; ~30 MANDATORY rules path-scoped via A1-A5) |
| **Session-end context check** (verify % before propose end-session / `/clear`) | `.claude/rules/session-end-context-check.md` §3 threshold table (<50% don't / 50-69% soft / 70-84% heads-up / ≥85% recommend / ≥95% force) | At-turn self-detection §4 sequence (run `statusline-kite.sh` với JSON stdin construct + auto-detect 200k vs 1M); CI transcript-scan detector deferred ≥7 ngày | Author self-review + memory auto-load + (future) transcript detector | ✅ DONE (2026-05-19 — rule v1.0.1 + memory + paired self-test PASS at 44% Opus 1M) |
| **VN-localization audit checklist (cross-bucket)** (tenant-facing artifact VND format / Vietnamese label / VN sample data / VN cultural awareness) | `.claude/rules/vn-localization-audit-checklist.md` §2 4-section checklist (VND `1.500.000đ` + Vietnamese label `Đăng nhập` + VN sample `Trần Thị Hồng` + VN culture Zalo/niên khóa/Mon-Sat) | Pre-merge reviewer-checklist per §4.1 + PR template row §4.2; CI grep detector deferred per `incident-to-rule-pipeline.md` §3.1 (heuristic FP risk inherently high cho English-narrative-in-VN-context detection — code-switching natural per `dev-readable-doc-language.md` §4) | Author + reviewer + (future) NLP language classifier | ✅ DONE (2026-05-19 — Wave 100 Bucket D: rule v1.0.0 + worked self-test 4 buckets × 4 sections = 16/16 PASS, closes GAP-680 META P1) |
| **Thesis report / academic deliverable** (`documents/08-thesis/**` thesis V1+ DOCX/PDF ship cho academic submission — khóa luận tốt nghiệp UTC convention) | `.claude/rules/thesis-content-standard.md` §2 9-category rubric /100 (C1 Format + C2 Content+page count + C3 Bibliography IEEE + C4 Academic tone + C5 Project-internal reference scrub + C6 Draft-marker scrub + C7 Diagram+figure rendering + C8 Examiner readiness + C9 Compliance+legal) grounded in UTC spec PDF + BAO_CAO sample + DE_CUONG sample + 43 persona-simulation findings | Pre-merge reviewer-checklist per §6.1 + path-scoped auto-load `documents/08-thesis/**`; CI detector + memory auto-load deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày | Author + reviewer + GVHD review pre-ship | ⚠️ PARTIAL (2026-05-19 — Wave 102 META: rule v1.0.0 shipped + retroactive audit annotation showing rubric v1 82/100 inflated vs rubric v2 42/100 baseline; thesis-v1.docx needs Wave 102.1 bundled fix PR achieve ≥75/100 target) |
| **User manual pages** (`documents/05-guides/user-manual/**` + `kitehub-frontend/src/app/help/**`) | `.claude/rules/user-manual-content-standard.md` §2 15-item checklist + §3 persona discoverability matrix (≥3 entry points per persona) | Pre-merge reviewer-checklist per §5.1; CI grep + memory auto-load deferred ≥7 ngày | Author + UI reviewer + 1 native VN reader | ⚠️ PARTIAL (2026-05-14 — Wave 79 Bucket F1: rule + anonymous-prospect 5-page sample DONE; P2/P3/Admin defer Wave 80+ Bucket F2 per GAP-537) |
| **Professional manual content** (`documents/05-guides/professional-manual/**` + `dev/**` + `integration/**` + `operations/**/*-runbook.md`) | `.claude/rules/professional-manual-content-standard.md` §2 15-item checklist + §3 audience discoverability matrix | Pre-merge reviewer-checklist per §5.1; CI grep + memory auto-load deferred ≥7 ngày | Author + technical reviewer + 1 native VN reader | ⚠️ PARTIAL (2026-05-18 — Wave 92 Bucket D: rule + 3 retroactive self-test samples 11/15 PASS; concrete content defer Wave 88+) |
| **Action scratchpad commit** (`documents/action-*.md` user inside content) | `.claude/rules/always-commit-action-scratchpad.md` §1 (commit ngay khi user edit, never stash/defer; §3 5-bước commit + push + sync wave/gap + session-handoff reference) | Pre-merge reviewer-checklist per §6.2 + path-scoped auto-load `documents/action-*.md`; detector deferred ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard | Author + reviewer + memory auto-load | ✅ DONE (2026-05-20 — rule v1.0.0 + memory + 2-recurrence self-test §5: 2026-05-18 stash@{3} Wave 95 era + 2026-05-20 Wave 102.7.3 scope miss action-2.md §4 inside items both fire correctly) |

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

## 5. Remediation Plan (historical — Wave 8b closed)

See `_examples/output-review-mandate-examples.md` §Remediation Plan (original 2026-04-14 scoping artifact drove GAP-170..175 + GAP-048; all 6 CRITICAL violations closed Wave 8b 2026-04-20). Current matrix state lives in §3.

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

- **2026-05-19** (v1.15.0): MINOR — added §3 matrix row "Thesis report / academic deliverable" tracking new rule `.claude/rules/thesis-content-standard.md` v1.0.0 (paired same-PR Wave 102 META 2026-05-19). Triggered by user-flagged miss post Wave 102 GAP-688 closure: rubric v1 (6 categories) gave 82/100 B- nhưng MISSED 7 substantial content-quality issues (academic tone "đối thủ" / Claude refs / Mermaid as code / danh mục thuật ngữ vs viết tắt / logo UTC chưa chèn / TL;DR sections / repo jargon Wave/Phase BETA/GAP / 110 trang vs 60-70 target) plus 43 additional findings từ persona simulation agent (GVHD 13 + GVPB 15 + Defense committee 15) = total 50 missed dimensions. Plus state-check meta-miss: initial rule v1.0.0 draft thiếu explicit consult `documents/07-archived/academic/word-reports/` UTC samples (BAO_CAO_THUC_TAP.docx + DE_CUONG_DATN.docx) — reconciled BEFORE first push. META P0 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn rubric → eliminate retroactive content-quality rework cho mọi thesis V1+ V2+ subsequent. Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule v1.0.0 + matrix row + rules-index.csv row + retroactive audit annotation showing rubric v1 82 inflated vs rubric v2 42 baseline + path to 87/100 B+ documented all paired same PR. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "thesis academic deliverable"; no constraint loosening; existing thesis-v1.docx grandfathered with retroactive audit annotation; rule applies prospectively từ Wave 102.1 fix PR forward).
- **2026-05-19** (v1.14.0): MINOR — added §3 matrix row "VN-localization audit checklist (cross-bucket)" tracking new rule `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 (paired same-PR Wave 100 Bucket D 2026-05-19). Triggered by 3-audit consensus Wave 100 thesis push 2026-05-19 (persona simulation + failure-mode matrix + VN edu SaaS benchmark) — all 3 outside-in agents independently identified cross-bucket VN-localization concern Wave 100 4 buckets (A invoice VND + B income KPI VND + C email-only Zalo culture + D thesis VN narrative). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn cross-bucket → mọi PR subsequent (Wave 100+ Wave 101+) auto-comply. Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule + matrix row + rules-index.csv row + worked self-test (Wave 100 4 buckets × 4 sections = 16/16 applicable PASS) + GAP-680 closure all paired same PR. Closes coverage gap surfaced by 3-audit consensus — sister rules `dev-readable-doc-language.md` covers narrative-only; `user-manual-content-standard.md` §2 covers user manual narrow scope; cross-bucket VN-context previously uncovered. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type; no constraint loosening; existing artifacts grandfathered; rule applies prospectively từ Wave 100 forward).
- **2026-05-19** (v1.13.0): MINOR — added §3 matrix row "Session-end context check" tracking new rule `.claude/rules/session-end-context-check.md` v1.0.1 (paired same-PR Wave 100 prep 2026-05-19). Triggered by user-flagged 2026-05-19 "thêm rule là check % context thực tế bằng .claude/statusline-kite.sh trước khi đề xuất end session". Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule + matrix row + rules-index.csv row + memory `feedback_session_end_context_check.md` + worked self-test (PASS at 44% Opus 1M context, < 50% → don't propose end) all paired same PR. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "end-session decision moment"; no constraint loosening; existing session proposals grandfathered; rule applies prospectively từ next session forward).
- **2026-05-19** (v1.12.3): PATCH — Wave 99 streamline round 3. (1) Compress §3 matrix volatile "Current Status" cells (UI / Quality / Ops / Performance / Security / Business / API + a few other multi-line entries) to terse one-liners + pointer to canonical `audits-index.csv`. Removed redundant inline deltas, multi-line methodology, gap-link explosion already living in CSV row body — keeps audit-level verdict + score + date + CSV pointer. Eliminates ~80% rule churn going forward (audit refresh = CSV update only, no rule bump + Log entry per wave); (2) Compress §3 verbose Process column cells where Wave-specific implementation details exceeded standard scope; (3) Compress §2 "What Counts as Output" bullet-list layout to inline paragraph (factual content preserved); (4) Move §11 Log entries v1.11.0 → v1.8.4 (6 entries, 2026-05-15 → 2026-05-18 Wave 79..94c) sang `_examples/output-review-mandate-log-history.md`; (5) Bump v1.12.2 → v1.12.3 + sync Reviewer-Approver pointer (body now retains v1.12.3..v1.12.0; archive holds v1.11.0..v1.0.0). Pre-streamline 37.8k chars UTF-8 / ~53.5k Claude-metric → post-streamline target ~22-25k chars / well under Anthropic 40k auto-load threshold. No constraint change; canonical §3 matrix preserved in body with pointer for volatile state to CSV. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — body-only streamline, no rule semantics affected; volatile audit state now CSV-canonical per Wave 99 lesson; existing audits/refs grandfathered).
- **2026-05-19** (v1.12.2): PATCH — Wave 98 GAP-661 post-closure audit suite REFRESHED markers (4 §3 matrix rows): UI 104.7→**110.6/128 A** (+5.9 Cluster B persona-driven polish); Quality refreshed **90/110 B+** (+5 raw vs Wave 53 baseline 85/110 B+; PASS Phase 1 BETA ≥80 +10 buffer + PROD MAJOR ≥85 +5 buffer); API 79→**76/100 C FAIL** (-3, 2 P0 new GAP-662 EmailController URL drift + GAP-663 PreferencesController zero IT); Business 70→**73/100 C+ PARTIAL FAIL** (+3, 1 P1 new GAP-664 3-layer doc completeness shared BL+API). 4 new rows added to `audits-index.csv` (AUDIT-2026-05-19-wave-98-{ui-cluster-b-sample, quality-refresh, api-contract-new, business-logic-new}). 6 new gap files filed GAP-662..667 per `audit-to-gap-pipeline.md` Step 3. GAP-661 flipped DONE per `gap-done-discipline.md` §2; git mv to closed/. Cadence met T-2 from 2026-05-21 deadline. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — factual REFRESHED marker sync post-GAP-661 audit suite, no constraint loosening; existing audit standards preserved; status reflects audit-level verdicts per `audit-skill-rubric-*.md` §1 transparency mandate).
- **2026-05-19** (v1.12.1): PATCH — body streamline để giảm rule file size từ ~54k char xuống dưới Anthropic 40k auto-load threshold. (1) Compress frontmatter line 7 Reviewer-Approver field từ stacked history (~3k char inline) xuống one-line current pointer; (2) Move §11 Log entries v1.7.1 → v1.0.0 (2026-05-14 → 2026-04-14, 17 entries) sang `_examples/output-review-mandate-log-history.md` (deferred-load file per existing `_examples/` pattern); (3) Bump version v1.12.0 → v1.12.1 + sync Last-Reviewed. No constraint change; canonical §3 matrix + §1-§10 body content unchanged. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — body-only streamline, no rule semantics affected; existing audits/refs grandfathered).
- **2026-05-18** (v1.12.0): MINOR — added §3 matrix row "Diagram format selection" tracking new rule `.claude/rules/diagram-format-selection.md` v1.0.0 (paired same-PR Wave 96 PR2). Triggered by user-flagged miss recurrence #5 2026-05-18: `documents/02-architecture/email-architecture.md` (vừa ship Wave 95 PR1) dùng plain ASCII ~30 nodes thay vì Mermaid (GitHub native render). User direction "thêm rule tạo diagram thì phải dùng hợp lý trong 3 định dạng này, không phải text thuần như báo cáo kiến trúc email vừa rồi". Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule + §2 selection matrix + reviewer-checklist + self-test §6 (email-architecture.md ASCII → Mermaid flowchart rewrite same PR) + rules-index.csv row paired same PR. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "diagram format choice in markdown"; no constraint loosening; existing ASCII diagrams grandfathered until next refresh; rule applies prospectively from Wave 96 PR2 forward). Pattern recurrence logged in `feedback_outside_in_recurring_miss.md` memory entry.
> **Entries v1.11.0 .. v1.0.0 (2026-05-18 → 2026-04-14) moved to [`_examples/output-review-mandate-log-history.md`](_examples/output-review-mandate-log-history.md)** per Wave 99 streamline rounds 1+2+3 (rule body was exceeding Anthropic 40k char auto-load threshold). Body retains v1.12.3 → v1.12.0.
