# Output Review Mandate

**Priority:** 🔴 CRITICAL — project-wide governance rule
**Version:** 1.9.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-05-18
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.9.0 MINOR self-approve per `rule-change-process.md` §5; Wave 92 Bucket D — added §3 matrix row "Professional manual content" tracking new sister rule `professional-manual-content-standard.md` v1.0.0 per Manual split queue item 2026-05-17; sister to `user-manual-content-standard.md` covering professional/technical audience scope; no constraint loosening, additive coverage row. v1.8.4 (kept): PATCH self-approve per `rule-change-process.md` §5; Wave 85 Bucket H — §3 matrix 2 rows updated (Performance baseline 81→86/100 B+ +5; Security baseline 90→93/100 A v2 format +3); 2 new rows added to `audits-index.csv`; no constraint change, factual REFRESHED marker sync post-Wave-85-Bucket-H audit. v1.8.3 (kept): Wave 84 Bucket H — §3 matrix row "Ops readiness" updated (60/100 D Wave 40 → 78/100 C+ Wave 84 post-apply; +18; CloudTrail/startupProbe/secrets rotation/cost monitoring deltas); 1 new row added to `audits-index.csv`; no constraint change, factual REFRESHED marker sync. v1.8.2 (kept): Wave 83 post-wave audit suite refresh — 4 §3 matrix rows updated (UI screens, Security baseline, Business logic, API contracts) với new scores + delta annotations; no constraint change, factual REFRESHED marker sync. v1.8.1 (kept): Wave 80 Bucket A — extends §3 row "Security baseline" Process column với reference tới v2 audit format mandate (GAP-564) + worked self-test reference; no constraint change for prior audits (Wave 78 grandfathered v1 banner); v2 applies prospectively Wave 80+. v1.8.0 (giữ): MINOR self-approve per `rule-change-process.md` §5; v1.2.0 → v1.3.0 extends §3 row "HTML/JSX prototypes" Process column with integration smoke test + landing parity script per `incident-to-rule-pipeline.md` Stage 3 paired with same-PR enforcement: Tier 1 script `_shared/scripts/check-ui-kits-landing.sh` ships in same wave foundation; Tier 2 ui-review-prototype skill GAP-264 + Tier 3 hook/CI GAP-265 ship same wave by parallel agents)
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
| **UI screens** | ui-review /128 per-screen | After FE PR | Auditor | ✅ REFRESHED (2026-05-15, **112.0/128 A+** — Wave 83 post-deploy 3-screen sample landing/pricing/legal-cookies; +0.3 vs Wave 53 baseline 111.7; +2 discoverability legal/cookies via Footer link (GAP-558); banner UI live verify deferred post-Vercel-deploy; full kit /128 refresh defer Wave 84+) |
| **Quality audit reports** | quality-audit 11 categories /110→/100 | Periodic | Auditor | ✅ REFRESHED (2026-05-11, **85/110 (87/100 / 80 tech-only) B+** — Wave 53 milestone, PR #1107; +1 vs Wave 40 baseline 86; +7 buffer above Phase 1 BETA threshold 80; Cat 4 FE Tests +2 (Wave 51 209 component + 28 E2E); Cat 8 Docs +1; persona Cat 11 = 5/10 placeholder GAP-152 carry-forward) |
| **Ops readiness** | ops-readiness-audit skill /100 | Post-wave + quarterly | Auditor | ⚠️ REFRESHED (2026-05-18, **75/100 C** — Wave 91 post-batch1, closes GAP-601; **-3 vs Wave 84 baseline 78/100 C+**; code-level positive deltas (Wave 89 gateway JWT + PM2 systemd / Wave 91 outbox dispatcher + DLQ + admin email template + smoke scripts + Trivy SARIF guard ≈+8) offset by operational regression (≈-11) do **GAP-612 AWS account suspension** blocking Wave 91 Bucket F live verify + CloudWatch SNS fire path + IAM apply; 3 P0 FAILs (restore drill carry GAP-257 / alertmanager + AWS SNS regression GAP-144 / rollback drill blocked); 1 NEW gap GAP-614 filed (Wave 91 Bucket D V60 RLS migration verify); path to Phase 1 BETA gate 80 = GAP-612 restoration + Bucket F live verify = ≥80 trong 24-72h) |
| **Performance baseline** | performance-audit skill /100 | Post-wave + quarterly | Auditor | ✅ REFRESHED (2026-05-15, **86/100 B+** — Wave 85 Bucket H post-apply; +5 vs Wave 54 baseline 81; Cat 1 +1 (Bucket B RLS NULL force-fail + HikariCP GUC reset), Cat 2 +2 (Bucket D cursor pagination 2 endpoints + 3 findAll Pageable), Cat 5 +2 (Bucket E Tier 2 JVM 60% + 7 services prod profile + 3 CloudWatch alarms); zero new P0/P1; 4 P2 carry-forward; PASS Phase 1 BETA ≥80 + v1.0.0-rc ≥85 trajectory) |
| **Security baseline** | security-audit skill /100 + **v2 audit format mandatory per GAP-564** (per-control evidence block: Command run + Output + Verdict + Evidence artifact ID — see `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md`) | Post-wave + quarterly | Auditor | ✅ REFRESHED (2026-05-15, **93/100 A** — Wave 85 Bucket H post-apply, v2 format; +3 vs Wave 83 baseline 90; Cat 3 +2 (A01 RLS NULL force-fail eliminate silent cross-tenant leak + A09 V60 immutable admin_audit_logs PDPL Art 11 tamper-proof), Cat 4 +1 (admin-bypass paired aspect + immutable log multi-layer defense); v2 evidence 5/5 blocks; PASS Phase 1 BETA ≥80 + v1.0.0-rc ≥85; 3 P1 Wave 78 carry-forward unchanged) |
| **Business logic implementation** | business-logic-audit skill /100 | Post-wave + quarterly | Auditor | ✅ REFRESHED (2026-05-15, **71/100 C** — Wave 83 post-deploy; +3 vs Wave 40 baseline 68; Cat 2 error semantic mapping +15 (RFC 7807 6 handlers); Cat 3 PDPL Art 11 opt-in +10; Cat 4 Living Docs -8 (rules.md sync gap P1); 1 P1 recommendation) |
| **Business docs implementation match** (code ↔ rules.md sync) | Living Docs rule (3-layer) | Same PR as code change | PR reviewer | ✅ DONE |
| **Business logic CORRECTNESS** (giá trị rule đúng thị trường + law) | BRD + stakeholder sign-off + compliance | Before launch + quarterly | Product + Business + Legal | ⚠️ PARTIAL — rule shipped 2026-04-29 (`.claude/rules/business-logic-review.md` Phase 1 of GAP-049, Wave Business Correctness Agent B); audit + stakeholder sign-offs → GAP-156 |
| **PRs** | check-pr skill | Pre-merge | Reviewer | ✅ DONE |
| **Wave plans** | Wave review checklist | Before launch | Team lead + architect | ⚠️ PARTIAL (skill exists, no formal review) |
| **Wave closure scope completeness** | `wave-closure-scope-completeness.md` §3 Scope-Completeness Reconciliation table (every plan §3 item categorized ✅ DONE / 🟡 PARTIAL with gap link / ❌ NOT-IMPLEMENTED with follow-up gap link OR out-of-scope rationale) | At wave closure PR (status: draft → complete flip) | Author + reviewer + paired follow-up gap files | ✅ DONE (2026-05-18 — `.claude/rules/wave-closure-scope-completeness.md` v1.0.0 + 3 follow-up gap files GAP-619/620/621 for Wave 92 orphan items + worked self-test on Wave 87/88 DNS + Wave 92 retroactive per `incident-to-rule-pipeline.md` 5-stage) |
| **Gap reports** | Gap review template | After creation | Peer | ✅ DONE (2026-04-20, GAP-170 — `.claude/skills/quality/gap-review/` + `_REVIEW-TEMPLATE.md`) |
| **Gap closure (Status flip → DONE)** | `gap-done-discipline.md` (AC checked, no banned phrases, follow-up filed for any deferral) | Pre-merge of closing PR | Author + reviewer + skill detection | ✅ DONE (2026-04-27 — `.claude/rules/gap-done-discipline.md` + `session-docs-check` Rule 13 detector + 3-fixture self-test in this PR; closes GAP-235 Sub-PR G silent-deferral incident) |
| **Coverage gaps in rules/skills (incidents)** | `incident-to-rule-pipeline.md` (5-stage: Detect → Classify → Rule+Enforce → Self-Test → Retro Log) | When user/reviewer flags a miss | Author + reviewer | ✅ DONE (2026-04-27 — `.claude/rules/incident-to-rule-pipeline.md` paired with `rule-change-process.md` §6.5 Enforcement Parity Mandate in this PR) |
| **Architecture docs** | ADR process | When written | Tech lead + team | ✅ DONE (2026-04-20, GAP-172 — `documents/02-architecture/adr/README.md` + `_TEMPLATE.md`) |
| **Skills (meta)** | skill-conventions.md rules + `scripts/check-skill-conventions.sh` | Pre-merge (CI) | Lead + CI | ✅ DONE (2026-04-28, GAP-251 — script + 3 fixtures + CI job `skill-conventions`; baseline 44 PASS / 38 WARN / 0 FAIL; 21 grandfathered skills tracked for Wave 9 cleanup) |
| **Rules docs (meta)** | ADR-like | Pre-merge | Lead + team | ✅ DONE (2026-04-20, GAP-171 — `.claude/rules/rule-change-process.md` + `.claude/skills/quality/rule-review/`) |
| **Templates (UI/image)** | GAP-011 5 criteria | Before publish | Designer + lead | ⚠️ PLANNED (GAP-011) |
| **Email templates** | Brand + legal check | Before send | Marketing + legal | ✅ DONE (2026-04-20, GAP-173 — `.claude/skills/quality/email-template-review/`) |
| **AI-generated assets** | Quality gate /100 + content safety + `ai-branding-quality-gate` skill | Auto + manual | Automated + admin | ⚠️ PARTIAL — governance scaffold DONE 2026-04-26 (GAP-223 Sub-PR 223.1: skill `quality/ai-branding-quality-gate/` + audit-gate rule + `ai-branding-guidelines.md` §11.4 Migration test checklist + baseline audit 62/100); GAP-012 §5 5 Strategy-pattern checks + GAP-018 3-stage moderation pipeline DONE Wave 4 scaffold-only; real WCAG/visual-regression/ML classifier tracked GAP-226/227/228 Wave 8+; **systemic scaffold-as-DONE pattern (GAP-008/009/012/015/018) tracked under umbrella [GAP-225](../../documents/04-quality/gaps/closed/GAP-225-scaffolded-as-done-governance-closure-umbrella.md) — Phase 1 docs truth-up DONE 2026-04-29; Phase 2-4 (saga-pattern-review skill, ai-agent-review skill, scaffold-governance.md rule) future scope** |
| **Contracts (Word)** | Legal review | Before use | Lawyer | ❌ **VIOLATION** |
| **Generated PDFs/Excel** | QA checklist + visual regression | Before delivery | QA | ⚠️ PLANNED (GAP-047) |
| **Database migrations** | migration-review-checklist skill | Pre-merge | DBA + peer | ✅ DONE |
| **Scripts (bash/Python)** | script-review-checklist skill | Pre-merge | Peer | ✅ DONE |
| **API contracts** | api-contract-audit skill + schema validation | Pre-merge + runtime | Consumer/producer | ✅ REFRESHED (2026-05-15, **82/100 B** — Wave 83 post-deploy; +6 vs Wave 78 baseline 76; Cat 3 Error Code Consistency +5 (RFC 7807 surface 11 handlers); 1 P2 follow-up sync extension props vào api-contract.md; CDC tests Pact still missing carry-forward) |
| **Screenshots** | Manual + automated audit | Capture time | Auditor | ⚠️ PARTIAL (ui-review skill) |
| **Logs format** | Log standard doc | Audit period | SRE | ✅ DONE (2026-04-20, GAP-175 — `.claude/rules/logs-format-standard.md`; implementation tracked GAP-114/115/116 Wave 7) |
| **README freshness** | `scripts/check-readme-freshness.sh` (`**Last Updated:**` date check, 30d WARN / 90d FAIL, exempt via `<!-- readme-freshness-exempt: <reason> -->`) | Pre-merge (CI) | CI + reviewer | ✅ DONE (2026-04-28, GAP-255 — script + 5 self-test fixtures + CI job `readme-freshness`; baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs) |
| **Meta CSV indexes** (rules / ADRs / gaps + future skills + audits) | `.claude/rules/meta-csv-index-pattern.md` §3 (CSV + query helper + CI validator + CI wire) + 100% coverage parity (every enumerated file has CSV row) | Pre-merge (CI `meta-csv-indexes` + `gap-status-csv`) | CI + reviewer | ✅ DONE (2026-05-12, GAP-485 Tier 1+2 — rule shipped + `adrs-index.csv` 28 rows + `rules-index.csv` 36 rows + 2 query helpers + 2 validators + CI job; Tier 3 skills + audits → GAP-490 follow-up) |
| **Marketing copy** | Brand + legal | Before publish | Marketing + legal | ✅ DONE (2026-04-20, GAP-174 — `.claude/skills/quality/marketing-legal-review/`) |
| **Legal docs sent to tenants** | Full legal review | Before issue | Lawyer | ✅ DONE (2026-04-20, GAP-174 — shared `marketing-legal-review` skill covers TOS/Privacy/DPA) |
| **HTML/JSX prototypes** (`documents/02-architecture/design-system/ui_kits/**`) | Per-screen `/128` rubric (extends `quality/ui-review/SKILL.md` for static HTML path) + WCAG AA self-measurement in HTML comments + 100-item AC checklist (`design-system/dossier/10-acceptance-criteria.md`) + **integration smoke test** (post-merge: open `http://127.0.0.1:PORT/ui_kits/` landing → click each kit card → verify page loads + sample 3 screens per kit) + **landing parity** (`_shared/scripts/check-ui-kits-landing.sh` exit 0 — every kit folder has matching card AND vice versa) | Pre-merge per kit PR self-report + post-merge integration smoke test by reviewer + user vibe-check + landing parity script in CI (Tier 3 GAP-265) | Author self-review + reviewer integration check + user accepts | ⚠️ PARTIAL (Phase 1 standard + Tier 1 landing-parity script + review template documented 2026-04-29 GAP-263; Phase 2 ui-review-prototype skill → GAP-264; Phase 3 hook/CI enforcement → GAP-265) |
| **Root README** (`README.md` at repo root) | `readme-content-discipline.md` §2 stable-only allowlist + §3 volatile-content denylist + §4 borderline decision rule + §11 self-test grep regex | Pre-merge reviewer manual; Phase 2 CI script + Phase 3 hook deferred per rule §8 Open Items | Reviewer + author | ⚠️ PARTIAL (Phase 1 rule + same-PR README rewrite shipped 2026-04-29; CI script + hook tracked Open Items in rule §8) |
| **UI/Design scope completeness (4-layer V-model)** | `.claude/rules/design-layer-coverage.md` §2 matrix per scope-unit (gap / kit / wave / Track 2 port) — verify all 4 Japanese layers (要件定義 / 基本設計 / 詳細設計 / コンポーネント設計) have artifact pointers; ❌ at any layer = scope incomplete | Pre-merge reviewer checklist + PR template checkbox; reference: `dossier/16-design-layer-mapping.md` for per-context lookup | Author self-review + reviewer 4-layer check | ⚠️ PARTIAL (rule + dossier mapping shipped 2026-04-30 via Wave Coverage Audit follow-up; PR template checkbox added; quarterly `quality-audit` 4-layer sample audit pending) |
| **AWS verification reports** (`documents/04-quality/audits/aws-verification/**`) | `.claude/rules/agent-aws-access.md` §2 Tier 1 read-only allowlist (`describe-*`/`list-*`/safe `get-*`) + §2.2 banned secret-revealing reads + §5 mandatory artifact format (scope/commands/results/findings/next steps) | Pre-merge reviewer command-tier check; Phase 2 skill `aws-smoke-test` + `scripts/smoke-aws-phase-N.sh` deferred GAP-438 follow-up | Author self-review + reviewer | ⚠️ PARTIAL (Phase 1 rule + Phase 3 first artifact shipped 2026-05-08; Phase 2 skill + Phase 4 memory → GAP-438 Wave 42 follow-up) |
| **Acceptance test CSVs** (`documents/05-guides/operations/acceptance-tests/**`) per-release manual walkthrough matrix | `.claude/rules/test-artifact-format-standard.md` §1-§5 (CSV canonical + UTF-8 BOM + XLSX generated + companion README + .gitignore) + `.claude/rules/dev-readable-doc-language.md` §2 (Vietnamese narrative + English column names/enums/identifiers) | Pre-release-tag reviewer manual checklist + `bash scripts/render-acceptance-test-xlsx.sh` for Excel UX; pre-commit BOM-check hook + CI grep detector deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard | Author self-review + reviewer | ✅ DONE (2026-05-14 — Wave 72b Bucket G: 2 rules + 126-row Vietnamese translation + UTF-8 BOM + dedicated folder + render script; closes Wave 72a Bucket F user-flagged 4-issue incident) |
| **Dev-readable doc language** (gap files, runbooks, planning docs, audit reports, acceptance tests narrative content) | `.claude/rules/dev-readable-doc-language.md` §1 Vietnamese narrative + §3 acceptable English (code/commit/identifier/enum/protocol-token) + §4 mixed-language code-switch rule | Pre-merge reviewer-checklist per §7.1; CI grep detector + memory auto-load deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard | Author self-review + reviewer | ✅ DONE (2026-05-14 — Wave 72b Bucket G — codifies CLAUDE.md §"CRITICAL: Communication Language" for dev-readable scope; existing English-narrative docs grandfathered until next refresh) |
| **Context budget** (auto-load size per session) | `.claude/rules/context-budget-mandate.md` §1 (<120k base; rules ≥1k tokens must `paths:` frontmatter OR `## Auto-load justification` section OR hook-covered) | Pre-merge reviewer manual checklist per §6.1; future detector `scripts/check-context-budget.sh` deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard | Reviewer + author | ✅ DONE (2026-05-14 — Wave 73 Bucket D — codifies meta-context-optimization mandate; ~30 MANDATORY rules path-scoped via Wave 73 A1-A5; baseline measurement Bucket E) |
| **User manual pages** (`documents/05-guides/user-manual/**` + `kitehub-frontend/src/app/help/**` tenant-facing help content per persona) | `.claude/rules/user-manual-content-standard.md` §2 (15-item checklist: 5 foundation + 3 visual + 4 trust + 3 format) + §3 persona discoverability matrix (≥3 entry points per persona) | Pre-merge reviewer-checklist per §5.1; CI grep detector + memory auto-load deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard | Author + UI reviewer + 1 native VN reader | ⚠️ PARTIAL (2026-05-14 — Wave 79 Bucket F1: rule + anonymous-prospect 5-page sample DONE; P2 Owner + P3 Manager + Platform Admin defer Wave 80+ Bucket F2 gated F1 dev review per GAP-537) |
| **Professional manual content** (`documents/05-guides/professional-manual/**` + `documents/05-guides/dev/**` + `documents/05-guides/integration/**` + `documents/05-guides/operations/**/*-runbook.md` — technical audience nội bộ dev/ops/architect/integrator/founder/tester) | `.claude/rules/professional-manual-content-standard.md` §2 (15-item checklist adapted professional audience: 5 foundation + 3 visual+technical + 4 trust + 3 format) + §3 audience discoverability matrix (≥3 entry points per audience) | Pre-merge reviewer-checklist per §5.1; CI grep detector + memory auto-load deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard | Author + technical reviewer + 1 native VN reader | ⚠️ PARTIAL (2026-05-18 — Wave 92 Bucket D: rule shipped với 15-item checklist + audience matrix + 3 retroactive self-test samples (architecture.md + 2 runbooks) 11/15 PASS + 4/15 partial polish; concrete Phase 1 BETA professional manual content defer Wave 88+ sister scope per Manual split queue item 2026-05-17) |

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

- **2026-05-18** (v1.9.0): MINOR — added §3 matrix row "Professional manual content" tracking new rule `.claude/rules/professional-manual-content-standard.md` v1.0.0 (paired same-PR Wave 92 Bucket D — sister rule split). Triggered by Manual split queue item 2026-05-17 (inside-out-queue.md) — user direct surface during Wave 87 planning: "manual hiện tại text-only không đủ cho 2 audience; cần tách 2 track: Professional system manual vs End-user manual". Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule + matrix row + rules-index.csv row + queue file `status: consumed (Wave 92 Bucket D)` + 3 retroactive self-test samples all paired same PR. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "professional manual content"; no constraint loosening; existing professional docs grandfathered; rule applies prospectively from Wave 92 forward).
- **2026-05-18** (v1.8.5): PATCH — Wave 91 post-batch1 ops-readiness audit refresh (closes GAP-601 deadline 2026-05-20). §3 matrix row "Ops readiness" updated: 78/100 C+ Wave 84 → **75/100 C** Wave 91 post-batch1 (-3 delta). Deltas: code-level positive (Wave 89 gateway JWT + PM2 systemd / Wave 91 outbox dispatcher + DLQ + admin email template + smoke scripts + Trivy SARIF guard ≈+8) offset by operational regression (≈-11) do GAP-612 AWS account suspension (2026-05-17 16:50 UTC) blocking Wave 91 Bucket F live verify + CloudWatch SNS fire path + IAM apply. 3 P0 FAILs surfaced (restore drill carry GAP-257 / alertmanager + AWS SNS regression GAP-144 / rollback drill blocked). 1 NEW gap GAP-614 filed (Wave 91 Bucket D V60 RLS migration verify — Wave 92 queue). 1 new row added to `audits-index.csv` (AUDIT-2026-05-18-wave-91-post-batch1-ops-readiness). Path to Phase 1 BETA gate 80: GAP-612 AWS restoration + Wave 91 Bucket F live verify = ≥80 trong 24-72h. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — factual REFRESHED marker sync; no constraint loosening; existing audit standard preserved; status `⚠️ REFRESHED` reflects PARTIAL FAIL audit-level verdict per `audit-skill-rubric-ops-readiness-audit.md` §1 transparency mandate).
- **2026-05-15** (v1.8.4): PATCH — Wave 85 Bucket H post-wave audit suite refresh (Performance + Security). §3 matrix 2 rows updated: "Performance baseline" 81/100 B Wave 54 → **86/100 B+** Wave 85 post-apply (+5 delta — Cat 1 +1 Bucket B RLS NULL force-fail + HikariCP GUC reset; Cat 2 +2 Bucket D cursor pagination 2 endpoints + 3 findAll Pageable; Cat 5 +2 Bucket E Tier 2 JVM 60% + 7 services prod profile + 3 CloudWatch alarms). "Security baseline" 90/100 A- Wave 83 → **93/100 A** Wave 85 post-apply v2 format (+3 delta — Cat 3 +2 A01 RLS NULL force-fail eliminate silent cross-tenant leak + A09 V60 immutable admin_audit_logs PDPL Art 11 tamper-proof; Cat 4 +1 admin-bypass paired aspect + immutable log multi-layer defense). 2 new rows added to `audits-index.csv` (AUDIT-2026-05-15-wave-85-performance + AUDIT-2026-05-15-wave-85-security-v2). 3 P1 carry-forward Wave 78 unchanged (TOTP KMS + SecurityConfig default-allow + tenant header JWT). PASS Phase 1 BETA gate ≥80 + v1.0.0-rc gate ≥85 cho cả Performance + Security. No new P0/P1 filed Wave 85 audit scope. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — factual REFRESHED marker sync post-Wave-85-Bucket-H audit suite, no constraint loosening).
- **2026-05-15** (v1.8.3): PATCH — Wave 84 Bucket H post-apply ops-readiness audit refresh. §3 matrix row "Ops readiness" updated: 60/100 D Wave 40 milestone → 78/100 C+ Wave 84 post-apply (+18 delta). Deltas: CloudTrail observability baseline (GAP-437 DONE — 4 metric filters + dashboard + 4 security alarms + SNS topic), startupProbe wired Helm 7/7 (GAP-431 DONE — fixes Wave 40 regression), secrets rotation 90-day cadence (GAP-379 95% — Lambda Active + EventBridge wirings), EC2 cost monitoring (GAP-414 DONE — Lambda + 3 low-CPU alarms + monthly cron + SNS), 4 new account-prep runbooks (GAP-394 DONE Cloudflare/Resend/Vercel), SES + Statuspage VN overlays (GAP-423/424 DONE). 1 new row added to `audits-index.csv` (AUDIT-2026-05-15-wave-84-ops-readiness). 1 P0 carry (GAP-257 restore drill) + 1 P1 carry (GAP-144 AlertManager receivers) chặn Phase 1 BETA gate 80; path +2 pts trong 2-3 tuần. No new P0 filed (Wave 84 chỉ surface existing carries). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — factual REFRESHED marker sync post-Wave-84-Bucket-H audit, no constraint loosening).
- **2026-05-15** (v1.8.2): PATCH — Wave 83 post-wave audit suite refresh shipped 4 audit reports (api-contract 82/100, business-logic 71/100, security 90/100 v2 format, UI sample 112.0/128). §3 matrix 4 rows updated với new scores + delta annotations: UI screens 111.7→112.0 A+ (+0.3 sample-level), Security 89→90 A- v2 format (+1, A05+A09 hardening), Business logic 68→71 C (+3, error semantic mapping + PDPL Art 11), API contracts 76→82 B (+6, RFC 7807 surface 11 handlers). 4 new rows added to `audits-index.csv`. No P0/P1 new gaps filed in audit scope (Wave 78 P1 carry-forward unchanged). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — factual REFRESHED marker sync post-audit suite, no constraint loosening).
- **2026-05-15** (v1.8.1): PATCH — Wave 80 Bucket A — §3 matrix row "Security baseline" Process column extended để cite v2 audit format mandate per GAP-564 (per-control evidence block: Command run + Output + Verdict + Evidence artifact ID). Reference tới `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md`. v2 áp dụng prospectively Wave 80+ (Wave 78 5 audit reports retroactively annotated "v1 format" cost-benefit không re-run). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — reference addition only, no constraint loosening; existing audits grandfathered with v1 banner).
- **2026-05-14** (v1.8.0): MINOR — added §3 matrix row "User manual pages" tracking new rule `.claude/rules/user-manual-content-standard.md` v1.0.0 (paired same-PR Wave 79 Bucket F1). Triggered by Wave 79 Bucket F1 outside-in audit `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-bucket-f1-user-manual-outside-in.md` — 4 personas × 5 questions (Discovery/Format/Cognitive/VN edu/Trust) surfaced format + discoverability blind spot vague trong GAP-537 inside-out scope. Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity: new rule + matrix row + rules-index.csv row + anonymous-prospect 5-page worked self-test all paired same PR. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "user manual pages"; no constraint loosening; existing user manual scope = empty folder verify-at-spawn confirmed; rule applies prospectively from Wave 79 Bucket F1 forward).
- **2026-05-14** (v1.7.1): PATCH — Wave 76 Bucket E body streamline. §5 Remediation Plan (historical Wave 8b scoping artifact, all 6 violations closed 2026-04-20) moved to `_examples/output-review-mandate-examples.md`; body replaced with 1-line stub pointer. No constraint change; canonical §3 matrix retained in body. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-05-14** (v1.7.0): MINOR — added §3 matrix row "Context budget" tracking `.claude/rules/context-budget-mandate.md` v1.0.0 (new rule, paired same-PR Wave 73 Bucket D). Triggered by user-flagged 2026-05-14 miss "/start-session tốn ~34% context" — Wave 73 Meta Context Optimization. Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity: new rule + matrix row + rules-index.csv row + memory entry text in PR body all paired same PR. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "context budget"; no constraint loosening; existing rules grandfathered until path-scope refresh).
- **2026-05-14** (v1.6.0): MINOR — added §3 matrix rows: "Acceptance test CSVs" (per-release manual walkthrough matrix; references new rule `.claude/rules/test-artifact-format-standard.md`) and "Dev-readable doc language" (Vietnamese narrative + English identifier split; references new rule `.claude/rules/dev-readable-doc-language.md`). Triggered by Wave 72a Bucket F PR #1288 user-flagged 4 issues on `phase-1-beta-acceptance-self-test.csv` (folder placement, old Plan 1 not archived, English content, no UTF-8 BOM). Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: 2 new rules + same-PR CSV translation + folder relocation + render script + Plan 1 archive + rules-index.csv 2 new rows. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage rows for previously-uncovered output types; no constraint loosening; existing English-narrative test artifacts grandfathered until next refresh).
- **2026-04-30** (v1.5.0): MINOR — added §3 matrix row "UI/Design scope completeness (4-layer V-model)" referencing new rule `.claude/rules/design-layer-coverage.md` v1.0.0 + paired-PR `dossier/16-design-layer-mapping.md` reference doc + PR template checkbox. Triggered by user request "tôi mong muốn sử dụng 4 layer này để tránh miss docs như vừa rồi" — direct response to 2026-04-29 UI Coverage Audit incident (32% missing coverage caught at audit, not at gap-filing). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ Classify ✓ Rule+Enforce ✓ (this matrix row + design-layer-coverage rule + dossier mapping doc + PR template paired same PR per §6.5 Enforcement Parity Mandate) Self-test ✓ (worked example in design-layer-coverage.md §6 applied to 2026-04-29 incident — surfaces ⚠️ flags at 2 of 3 contexts checked) Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds completeness guard for previously-uncovered scope-completeness gap, no constraint loosening for prior work; existing artifacts grandfathered).
- **2026-04-29** (v1.4.0): MINOR — added §3 matrix row "Root README" referencing new rule `.claude/rules/readme-content-discipline.md`. Triggered by user-flagged miss "readme vẫn quá xấu" with 4 specific complaints (volatile metrics, ugly UI table, pixel art logo, project-specific clutter). Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity: rule + same-PR README rewrite + Phase 2/3 deferred per rule §8 Open Items. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage for previously-uncovered output type, no constraint loosening).
- **2026-04-29** (v1.3.0): MINOR — extended §3 matrix row "HTML/JSX prototypes" Process column with **integration smoke test** (open landing → click each card → sample 3 screens per kit) + **landing parity script** (`_shared/scripts/check-ui-kits-landing.sh` exit 0 — every kit folder has matching card AND vice versa). Triggered by user-flagged miss in PR #678 closure: landing `index.html` not synced với 6 kits → user catch "đã có UI của trang kitehub đâu nhỉ, tôi vẫn thấy 3 repo". Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ Classify ✓ Rule+Enforce ✓ (this entry + Tier 1 script same PR per §6.5 Enforcement Parity Mandate) Self-test ✓ (script ran on current 6-kit `ui_kits/` → exit 0 PASS) Retro Log ✓ (this entry + memory `feedback_post_merge_doc_sync.md` extended). Tier 2 ui-review-prototype skill (GAP-264) + Tier 3 hook/CI/lefthook (GAP-265) ship same wave by parallel agents per Wave Review Process Improvement plan. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds enforcement, no constraint loosening; tightens existing standard with concrete script + post-merge gate).
- **2026-04-29** (v1.2.0): MINOR — added §3 matrix row "HTML/JSX prototypes" (`documents/02-architecture/design-system/ui_kits/**`) covering Round 2+ design prototypes. Standard: per-screen `/128` rubric (extends `quality/ui-review/SKILL.md` for static HTML path) + WCAG AA self-measurement in HTML comments + 100-item AC checklist (`dossier/10-acceptance-criteria.md`). Phase 1 (matrix-row + version bump) lands this PR paired with Wave UI Kits Round 2 foundation — first kit set applies the standard immediately. Phase 2 (ui-review-prototype skill extension) tracked GAP-264; Phase 3 (hook/CI enforcement) tracked GAP-265. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5; no constraint loosening — adds coverage for previously-uncovered output type). Closes Phase 1 of GAP-263. Motivation: Phase 0 of Wave UI Kits Round 2 surfaced this gap when scaffold work started without a defined review standard for prototypes — user (Option A) flagged Superpowers compliance violation; rule extension corrects the matrix.
- **2026-04-29** (v1.1.4): PATCH — extended §3 matrix row "AI-generated assets" line 80 to cite umbrella [GAP-225](../../documents/04-quality/gaps/closed/GAP-225-scaffolded-as-done-governance-closure-umbrella.md) for systemic scaffold-as-DONE pattern across 5 affected gaps (GAP-008/009/012/015/018) shipped Wave 2-4. Phase 1 (docs truth-up) DONE this PR — Phase 2-4 (saga-pattern-review skill, ai-agent-review skill, scaffold-governance.md meta-rule) explicitly future scope per gap §"Future scope". Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — matrix-row reference extension, no constraint loosening; coordinated with Wave Meta-Gov 2 Cluster 6 Phase-1 Agent B). Motivation: prior matrix entries cited GAP-225 only as "umbrella" tail-reference; expanded to surface the systemic pattern + Phase 1/Phase 2-4 split for future readers.
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
