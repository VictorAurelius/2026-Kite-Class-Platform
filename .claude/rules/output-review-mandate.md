# Output Review Mandate

**Priority:** 🔴 CRITICAL — project-wide governance rule
**Version:** 1.25.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-06-11
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.23.0 MINOR self-approve per `rule-change-process.md` §5; adds §3 matrix row "Retention-policy completeness" tracking new rule `.claude/rules/retention-policy-completeness.md` v1.0.0 (paired same-PR #2288 ECR cost-cleanup 2026-06-10); no constraint loosening — codifies "CI artifact category vs retention policy coverage" class after GAP-402 cosign sig/att accumulated unbounded; META P1 force-multiplier per `meta-gap-priority.md` §3. v1.22.0 (kept): adds §3 matrix row "Multi-session concurrency coordination" tracking new rule `.claude/rules/multi-session-concurrency-coordination.md` v1.0.0 (paired same-PR GAP-1114 2026-06-10); no constraint loosening — codifies inter-session shared-resource reservation discipline after 2 independent coordinator sessions collided on GAP-1111 + V96 + gap-status.csv; META P1 force-multiplier per `meta-gap-priority.md` §3. v1.21.0 (kept): extends §3 matrix row "G2 handoff recipe MD" Standard+Process+Status cells để cite `g2-handoff-md-mandate.md` v1.0.2 §3.4 browser-walk-for-FE-wired mandate (curl chỉ BE-only affordance); paired same-PR 2026-06-09 G2-browser-fix (GAP-1092); no constraint loosening — tracks updated rule version. v1.17.0 (kept): adds §3 matrix row "Agent model selection" tracking new rule `.claude/rules/agent-model-opus-default.md` v1.0.0 (paired same-PR Wave beta-readiness-8 mid-wave META addition 2026-05-25); no constraint loosening — codifies Opus 4.7 default mandate cho mọi Agent tool spawn sau recurrence ≥2 waves Sonnet thrash (Wave br-4 4/4 audit agents + Wave beta-readiness-8 Đợt 1 2/3 bg-agents); META P1 force-multiplier per `meta-gap-priority.md` §3 — mọi agent spawn subsequent auto-comply prospectively. v1.16.0 (kept): adds §3 matrix row "Wave naming convention" tracking new rule `.claude/rules/wave-tag-numbering-convention.md` v1.0.0 (paired same-PR Wave thesis-1 Phase 1 META prereq); META P1 force-multiplier; Wave 01-107 grandfathered per §5 migration. v1.15.0 (kept): adds §3 matrix row "Thesis report / academic deliverable" tracking new rule `.claude/rules/thesis-content-standard.md` v1.0.0 (paired same-PR Wave 102 META 2026-05-19). v1.14.0 (kept): adds §3 matrix row "VN-localization audit checklist (cross-bucket)" tracking new rule `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 (paired same-PR Wave 100 Bucket D 2026-05-19, closes GAP-680 META P1). v1.13.0 (kept): adds §3 matrix row "Session-end context check". v1.12.3 (kept): see §11 Log for entries v1.12.3 .. v1.12.0 + [`_examples/output-review-mandate-log-history.md`](_examples/output-review-mandate-log-history.md) for entries v1.11.0 .. v1.0.0)
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
| **Design-source implementation parity** (port Claude Design / Figma / mockup → ui_kits / production: every affordance/control/state present OR documented dropped; interactive affordance runtime click-verified) | `design-source-implementation-parity.md` v1.1.0 §3 parity checklist (copied-but-unwired grep + §3.2 runtime click-verify) + reviewer-checklist | Pre-merge of PR porting design source into `documents/02-architecture/design-system/**` or frontend `src/**` | Author + reviewer | ⚠️ PARTIAL (2026-06-01 — rule v1.1.0 + worked self-test ThemeSwitcher dropped-wiring + round-1-inert + same-PR fixes; CI detector deferred per premature-rule guard) |
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
| **AWS stack lifecycle pre-flight** (start/stop EC2/RDS) | `.claude/rules/pre-flight-aws-lifecycle-check.md` §3 3-step sequence (cred check + state check + document evidence) | Pre-trigger reviewer-checklist per §7.1; memory auto-load + script-level extension deferred ≥7 ngày | Author + reviewer | ✅ DONE (2026-05-26 — Wave beta-prep-1 closure: rule v1.0.0 + worked self-test §6 on originating 2026-05-26 cred-rotate incident ~12min wall-clock save + paired same-PR rules-index.csv row) |
| **Acceptance test CSVs** (`documents/05-guides/operations/acceptance-tests/**`) per-release manual walkthrough matrix | `.claude/rules/test-artifact-format-standard.md` §1-§5 + `.claude/rules/dev-readable-doc-language.md` §2 | Pre-release-tag reviewer manual + `bash scripts/render-acceptance-test-xlsx.sh`; pre-commit BOM hook + CI detector deferred ≥7 ngày | Author self-review + reviewer | ✅ DONE (2026-05-14 — Wave 72b Bucket G) |
| **Dev-readable doc language** (gap files, runbooks, planning, audit, acceptance tests narrative) | `.claude/rules/dev-readable-doc-language.md` §1 Vietnamese narrative + §3 acceptable English + §4 mixed-language code-switch | Pre-merge reviewer-checklist per §7.1; CI grep + memory auto-load deferred ≥7 ngày | Author self-review + reviewer | ✅ DONE (2026-05-14 — Wave 72b Bucket G) |
| **Context budget** (auto-load size per session) | `.claude/rules/context-budget-mandate.md` §1 (<120k base; rules ≥1k tokens must `paths:` frontmatter OR `## Auto-load justification` OR hook-covered) | Pre-merge reviewer per §6.1; future `scripts/check-context-budget.sh` deferred ≥7 ngày | Reviewer + author | ✅ DONE (2026-05-14 — Wave 73 Bucket D; ~30 MANDATORY rules path-scoped via A1-A5) |
| **Session-end context check** (verify % before propose end-session / `/clear`) | `.claude/rules/session-end-context-check.md` §3 threshold table (<50% don't / 50-69% soft / 70-84% heads-up / ≥85% recommend / ≥95% force) | At-turn self-detection §4 sequence (run `statusline-kite.sh` với JSON stdin construct + auto-detect 200k vs 1M); CI transcript-scan detector deferred ≥7 ngày | Author self-review + memory auto-load + (future) transcript detector | ✅ DONE (2026-05-19 — rule v1.0.1 + memory + paired self-test PASS at 44% Opus 1M) |
| **VN-localization audit checklist (cross-bucket)** (tenant-facing artifact VND format / Vietnamese label / VN sample data / VN cultural awareness) | `.claude/rules/vn-localization-audit-checklist.md` §2 4-section checklist (VND `1.500.000đ` + Vietnamese label `Đăng nhập` + VN sample `Trần Thị Hồng` + VN culture Zalo/niên khóa/Mon-Sat) | Pre-merge reviewer-checklist per §4.1 + PR template row §4.2; CI grep detector deferred per `incident-to-rule-pipeline.md` §3.1 (heuristic FP risk inherently high cho English-narrative-in-VN-context detection — code-switching natural per `dev-readable-doc-language.md` §4) | Author + reviewer + (future) NLP language classifier | ✅ DONE (2026-05-19 — Wave 100 Bucket D: rule v1.0.0 + worked self-test 4 buckets × 4 sections = 16/16 PASS, closes GAP-680 META P1) |
| **Thesis report / academic deliverable** (`documents/08-thesis/**` thesis V1+ DOCX/PDF ship cho academic submission — khóa luận tốt nghiệp UTC convention) | `.claude/rules/thesis-content-standard.md` §2 9-category rubric /100 (C1 Format + C2 Content+page count + C3 Bibliography IEEE + C4 Academic tone + C5 Project-internal reference scrub + C6 Draft-marker scrub + C7 Diagram+figure rendering + C8 Examiner readiness + C9 Compliance+legal) grounded in UTC spec PDF + BAO_CAO sample + DE_CUONG sample + 43 persona-simulation findings | Pre-merge reviewer-checklist per §6.1 + path-scoped auto-load `documents/08-thesis/**`; CI detector + memory auto-load deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày | Author + reviewer + GVHD review pre-ship | ⚠️ PARTIAL (2026-05-19 — Wave 102 META: rule v1.0.0 shipped + retroactive audit annotation showing rubric v1 82/100 inflated vs rubric v2 42/100 baseline; thesis-v1.docx needs Wave 102.1 bundled fix PR achieve ≥75/100 target) |
| **User manual pages** (`documents/05-guides/user-manual/**` + `kitehub-frontend/src/app/help/**`) | `.claude/rules/user-manual-content-standard.md` §2 15-item checklist + §3 persona discoverability matrix (≥3 entry points per persona) | Pre-merge reviewer-checklist per §5.1; CI grep + memory auto-load deferred ≥7 ngày | Author + UI reviewer + 1 native VN reader | ⚠️ PARTIAL (2026-05-14 — Wave 79 Bucket F1: rule + anonymous-prospect 5-page sample DONE; P2/P3/Admin defer Wave 80+ Bucket F2 per GAP-537) |
| **Professional manual content** (`documents/05-guides/professional-manual/**` + `dev/**` + `integration/**` + `operations/**/*-runbook.md`) | `.claude/rules/professional-manual-content-standard.md` §2 15-item checklist + §3 audience discoverability matrix | Pre-merge reviewer-checklist per §5.1; CI grep + memory auto-load deferred ≥7 ngày | Author + technical reviewer + 1 native VN reader | ⚠️ PARTIAL (2026-05-18 — Wave 92 Bucket D: rule + 3 retroactive self-test samples 11/15 PASS; concrete content defer Wave 88+) |
| **Action scratchpad commit** (`documents/action-*.md` user inside content) | `.claude/rules/always-commit-action-scratchpad.md` §1 (commit ngay khi user edit, never stash/defer; §3 5-bước commit + push + sync wave/gap + session-handoff reference) | Pre-merge reviewer-checklist per §6.2 + path-scoped auto-load `documents/action-*.md`; detector deferred ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard | Author + reviewer + memory auto-load | ✅ DONE (2026-05-20 — rule v1.0.0 + memory + 2-recurrence self-test §5: 2026-05-18 stash@{3} Wave 95 era + 2026-05-20 Wave 102.7.3 scope miss action-2.md §4 inside items both fire correctly) |
| **Local fix production parity** (PR touching config-shape artifact local → must sweep prod-equivalent surface) | `.claude/rules/local-fix-production-parity-check.md` §2 5-row trigger-path matrix + §3 Production parity table + §3.2 follow-up gap exit ramp | Pre-merge reviewer-checklist per §5.1 + path-scoped auto-load on config-shape paths (application.yml / compose / Dockerfile / secrets.tf / iam*.tf / helm / fetch-secrets.sh); CI grep detector deferred per `incident-to-rule-pipeline.md` §3.1 (heuristic complexity moderate + recurrence count 1 + honest defer documented inline) | Author + reviewer + memory auto-load | ✅ DONE (2026-05-22 — Wave 105 Bucket E0: rule v1.0.0 + memory + worked self-test §6 retroactive Wave 81 + Wave 104.5 same incident class — closes GAP-718 META P0; concrete sister GAP-717 PARTIAL — terraform IaC declaration shipped, post-AWS-restore terraform import + live verify deferred GAP-612 unblock) |
| **Wave naming convention** (wave identifier + filename + branch + commit + frontmatter + history schema từ Wave thesis-1 forward) | `.claude/rules/wave-tag-numbering-convention.md` §2 format spec `wave-{tag_primary}-{counter}[-{descriptor}]` + §3 counter rules monotonic + §2.4 frontmatter `tag_primary` + `tags_secondary` + §2.5 history schema extension (legacy + new coexist, no backfill) | Pre-merge reviewer-checklist per §8.1 + path-scoped auto-load `documents/03-planning/waves/**` + `.claude/skills/quality/wave-pack-planner/**` + skill update `wave-pack-planner/SKILL.md` Section "Wave numbering"; CI grep detector deferred per `incident-to-rule-pipeline.md` §3.1 (moderate complexity + recurrence 0 + FP risk hybrid format handle) | Author + reviewer | ✅ DONE (2026-05-23 — Wave thesis-1 Phase 1: rule v1.0.0 + worked self-test §6 on Wave thesis-1 itself 8/8 expected artifacts match + skill update + rules-index.csv row; Wave 01-107 grandfathered per §5 migration policy) |
| **Agent model selection** (`Agent` tool spawn → `model: "opus"` default) | `.claude/rules/agent-model-opus-default.md` §1 (Opus 4.7 1M mandatory for non-trivial agents) + §3 exception list (single-lookup Explore / statusline-setup / init / user-explicit override) | Pre-spawn coordinator self-detection §8.2 (explicit `model: "opus"`); reviewer-checklist §8.3 (skill/agent template files); memory auto-load §8.1 paired same-PR; PreToolUse hook detector deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence count 0 post-rule; low FP risk; revisit when slip-through ≥1) | Coordinator self + reviewer + memory auto-load | ✅ DONE (2026-05-25 — rule v1.0.0 + memory + worked self-test §6 Wave beta-readiness-8 Đợt 1 2/3 Sonnet thrash + Wave br-4 4/4 Sonnet thrash recurrence ≥2 confirmed; META P1 force-multiplier prospective mọi agent spawn subsequent) |
| **E2E vs RST test layer boundary** (automated regression vs manual exploratory acceptance + RST→E2E spec promotion) | `.claude/rules/e2e-rst-test-layer-boundary.md` §1 (E2E + RST = 2 complementary layers; RST→E2E promotion mandate) + §2.2 owns table mapping bug class → layer | Reviewer-checklist §5.1 (PR fix bug from RST cycle paired E2E spec same PR); CI grep detector deferred §5.3; worked self-test Đợt 105 §6 retroactive | Reviewer + (future) CI grep detector | ✅ DONE (2026-05-25 — rule v1.0.0 + §2.2 owns matrix + §3 RST→E2E promotion mandate + worked self-test §6 Đợt 105 5-bug recurrence retroactive; META P0 force-multiplier prospective mọi test addition + RST cycle subsequent; Đợt 106/107 RST findings sẽ apply prospectively) |
| **API-contract change caller sweep** (method signature / call-site swap / `@Deprecated` / rename → sweep all callers prod+test + run tests not just compile) | `.claude/rules/api-contract-change-caller-sweep.md` §1 (sweep callers + migrate + `./mvnw test` before push) + §2 trigger table | Reviewer-checklist §5.1 (grep callers full-output + mock swap + run affected tests); CI grep detector deferred §5.3; worked self-test GAP-799 §6 двойн miss | Reviewer + (future) detector | ✅ DONE (2026-05-28 — rule v1.0.0 + worked self-test §6 GAP-799 stale-mock + deprecation-leftover двойн miss; META P0 force-multiplier prospective mọi method-contract change) |
| **FE production-build local-verify** (PR đụng `kitehub/kitehub-frontend/**` hoặc `kiteclass/kiteclass-frontend/**` source → `pnpm --filter <pkg> build` local trước push, không chỉ lint/tsc) | `.claude/rules/fe-build-local-verify.md` §1 (production `next build` local pre-push) + §3 build evidence | Pre-push reviewer-checklist + path-scoped auto-load; CI Docker build = canonical, rule = pre-push filter | Author self + reviewer | ✅ DONE (2026-05-28 — rule v1.0.0 + worked self-test §6 GAP-801 Suspense bailout; META force-multiplier prospective mọi FE change) |
| **BE↔FE contract drift detectors** (GAP-802: BE-built FE path ↔ Next.js route + email-link non-404/non-prod-on-local + env prod-domain default thiếu local override) | `scripts/check-be-fe-url-contract.sh` (#2 static CI) + `scripts/smoke-email-links.sh` (#1 MailHog smoke) + `scripts/audit-env-coverage.sh` CHECK B (#5 local-deadlink) | #2 CI WARN job `be-fe-url-contract` (quality-code.yml); #5 job "Production env-var coverage audit"; #1 offline test in Script tests + post-deploy smoke | CI + reviewer | ⚠️ PARTIAL (2026-05-28 — GAP-802 #1/#2/#4/#5 shipped; #3 E2E deferred; findings → GAP-803: `/reset-password` route + 3 local-deadlink vars) |
| **Discovery during non-audit work** (work session sinh discovery — docs writing / refactor / debug / cleanup / migration / code-read / design review — gap-worthy finding stuck trong narrative → KHÔNG vào CSV → fix pipeline broken) | `.claude/rules/discovery-to-gap-inline-filing.md` §1 (file gap inline + CSV row + PR body `## Discoveries filed` section) + §2 trigger table (đang làm × discover gì) + §3 minimal gap format | Pre-merge reviewer-checklist per §8.1 (count gaps filed inline match `## Discoveries filed` section) + §8.2 self-detection (STOP narrative + file gap when match §2 trigger); CI NLP detector deferred per `incident-to-rule-pipeline.md` §3.1 (FP risk high for narrative discovery classification — recurrence count 1 Wave 13) | Author + reviewer + memory auto-load | ✅ DONE (2026-06-03 — rule v1.0.0 + memory + worked self-test §6 on Wave 13 ~50 schema anomalies inline filings counterfactual; META P1 force-multiplier prospective mọi non-audit work session subsequent) |
| **G2 handoff recipe MD** (Flow Verification Campaign — khi flow G1 ✅ PASS flip campaign §4 row → `🔄 walk-pass-pending-human`, G1-passer must ship dedicated stepped MD recipe cho user G2 test cùng PR) | `.claude/rules/g2-handoff-md-mandate.md` §3 7 sections (frontmatter + goal + setup + stepped instructions {action/expected/sad-path/verify} + sad path checks + 4-outcome báo kết quả + troubleshooting+G3 preview) + **§3.4 (v1.0.2) browser-walk cho MỌI affordance FE-wired (`:3001` KH / `:3000` KC); curl CHỈ cho BE-only affordance (label rõ)** + §4 filename `YYYY-MM-DD-g2-recipe-<flow>.md` Tier 2 time-bound + §5 VN narrative + EN identifiers | Pre-merge reviewer-checklist §8.1 (incl. browser-walk-per-affordance row v1.0.2); detector deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence count 1) | G1-passer + reviewer | ✅ DONE (2026-06-04 — rule v1.0.0 + worked self-test §7 on Wave flow-kh1 G2 recipe MD shipped same PR; v1.0.2 2026-06-09 browser-walk-for-FE-wired mandate — KH-5/KH-6/KC-8 curl-only miss, GAP-1092; META P1 force-multiplier prospective mọi G1 PASS flow subsequent 22-flow campaign auto-comply) |
| **User-facing flow walk readiness** (Wave / PR ship user-facing flow — signup / auth / invite / payment / tenant-switch / upload / email-driven / async — PHẢI spawn Opus pre-walk persona simulation agent return ≥5 failure modes BEFORE coordinator/user walk local Docker stack) | `.claude/rules/pre-walk-persona-simulation-mandate.md` §1 The Rule + §2 trigger pattern + §3 required agent output (5-10 failure modes với (a) where + (b) symptom + (c) pre-walk check + saved artifact `documents/04-quality/audits/persona-review/YYYY-MM-DD-pre-walk-<flow>.md`) | Pre-walk before any session that includes user-facing flow walk per §6.1 reviewer-checklist + §6.2 Wave plan §3 Scope row + §6.3 memory auto-load; CI grep detector HONEST-deferred per §6.5 (recurrence 1) | Coordinator + spawned Opus agent + reviewer | ✅ DONE (2026-06-04 — Wave flow-kh1 post-incident: rule v1.0.0 + memory `feedback_pre_walk_persona_simulation.md` + `persona-based-business-review.md` v1.3 Pre-Walk Mode + `wave-pack-planner` SKILL.md Step 4.55 + worked self-test §5 on Wave flow-kh1 G2 walk session — 5/6 bugs would have surfaced pre-walk + ~1.5h wall-clock saved per walk; META P1 force-multiplier prospective Flow Verification Campaign §4 22-flow queue subsequent walks auto-comply) |
| **Beta signup chain E2E happy-path** (Wave flow-kh1 G2 walk surfaced 4 bugs — GAP-925 consumer wire-format / GAP-926 FE generic catch / GAP-927 BE rollback rotates token / GAP-928 gateway false 503 — that an automated chain E2E spec would catch upstream at PR time, not at G2 walk time) | `kitehub/kitehub-frontend/e2e/beta-funnel/full-chain-regression.spec.ts` — Playwright route-mocked spec walking request → approve → signup with positive assertion (201 + success surface) + sad path A (409 SUBDOMAIN_TAKEN → distinct message NOT generic token-expired per GAP-926) + sad path B (retry same token after rollback → succeeds per GAP-927) + sad path C (404 INVALID_TOKEN at submit time → distinct message per GAP-926 errorCode branch) + admin list state-flip guard | CI: existing `pnpm test:e2e:gates:ci` job `.github/workflows/kitehub-frontend-ci.yml` auto-picks up `e2e/beta-funnel/` (no workflow edit needed); pre-merge per PR | CI + reviewer | ✅ DONE (2026-06-04 — spec shipped Wave flow-kh1 G2 meta META Item C; assertions explicitly reference GAP-925/926/927/928 incident IDs as regression-guard rationale; META P1 force-multiplier prospective mọi future beta-funnel regression caught at PR time, not at G2 walk) |
| **User-facing flow G1 browser-walk readiness** (Flow Verification Campaign §4 — flip flow CÓ FE từ chưa-G1 → `🔄 walk-pass-pending-human` PHẢI có ≥1 browser-real walk qua FE :3000, không chỉ curl `:9000` gắn header tay — bắt FE↔gateway contract / tenant-resolution / routing drift mà curl-with-manual-header che mất) | `.claude/rules/g1-browser-walk-before-flip.md` v1.2.0 §3 6-row browser-walk evidence + §3.1 production-accurate Host (nip.io, cấm `?tenant=`) + **§3.2 (v1.2.0) nguyên tắc tổng quát: production access-mode PHẢI local-reproduce-được** (mọi access/entry mechanism — subdomain/custom-domain/JWT-routing/OAuth — local test bằng chính cơ chế đó) | Pre-flip reviewer-checklist §7.1 + path-scoped auto-load `wave-*-flow-*.md` + `flow-verification-campaign.md` + `*-g2-recipe-*.md`; campaign §1 **G2★-absorbs-G3-functional gate (2026-06-11)** cross-ref; CI detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 1) | G1-walker + reviewer + memory auto-load | ✅ DONE (2026-06-08 v1.0.0 KC-1 G2 3/3 bugs GAP-1067/1068/1069; v1.2.0 2026-06-11 §3.2 general access-mode parity + campaign §1 G2★ gate redefinition — paired landing-100 GAP-811/1077 nip.io-walk pending; META P1 force-multiplier prospective mọi access-mode-determined flow) |
| **Small-gap inline-fix during walk** (Flow Verification Campaign — khi 1 walk G2/RST/G1-browser/G3 bắt gap NHỎ (≤30 phút, in-scope, low-risk, verify-now-able) → fix inline + flip DONE cùng session thay vì file-and-defer "wave sau" tạo backlog tồn đọng; refine campaign §MODE defer-cosmetic-LỚN) | `.claude/rules/small-gap-inline-fix.md` §1 4 tiêu chí "nhỏ" (a) effort ≤30p + (b) scope-local + (c) low-risk + (d) verify-now → §3 decision matrix FIX-INLINE / DEFER / SPLIT | Pre-merge reviewer-checklist §7.1 + path-scoped auto-load `wave-*-flow-*.md` + `flow-verification-campaign.md` + `gaps/**` + `*-g2-recipe-*.md`; CI detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (size-classification FP risk high, recurrence 1) | Walk-fixer + reviewer + memory auto-load | ✅ DONE (2026-06-08 — Flow Verification Campaign KC-1 G2: rule v1.0.0 + memory + worked self-test §6 on KC-1 G2 — GAP-1067/1072 FIX-INLINE vs GAP-1071 root-fix SPLIT phân loại đúng; META P1 force-multiplier prospective mọi walk-found gap subsequent auto-comply) |
| **Investigation source order** (khi investigate behavior/architecture/"là gap không"/"nên làm gì" — check DESIGN architecture-docs/ADR TRƯỚC → gaps → documents → CODE cuối cùng; code = what IS, design = what SHOULD BE, cần intended design trước mới phán correct-vs-gap) | `.claude/rules/design-first-investigation-order.md` §1 4-tier order + §3 tầng table + §3.1 kết luận cite design source không suy diễn từ code | Self-detection §7.1 + reviewer-checklist §7.2 (cite design source not code-inference) + memory auto-load §7.3 (always-load §10 justified — cross-cuts mọi investigation moment); detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (NLP intent-classification, recurrence 1) | Investigator + reviewer + memory auto-load | ✅ DONE (2026-06-08 — KC-1 tenant-resolution code-first miss: rule v1.0.0 + memory + worked self-test §6 — grep code trước design → suy diễn ngược, user push 2 lần; design-first đọc `tenant-domain-landing-architecture.md` lập tức thấy GAP-811 middleware gap; META P1 force-multiplier) |
| **KiteHub vs KiteClass boundary** (mọi reference FE port/service/domain/flow/từ-trùng-tên cần phân biệt KH vs KC; KH=kitehub-frontend `:3001` SaaS lifecycle, KC=kiteclass-frontend `:3000` nghiệp vụ trường; "billing/branding/login/payment/dashboard/settings" tồn tại cả hai nghĩa khác nhau) | `.claude/rules/kitehub-kiteclass-boundary.md` §2 canonical matrix + §2.1 trùng-tên trap + §4.1 platform-side KC exception | Reviewer-checklist §6.1 (port/API/domain/flow khớp matrix) + cross-ref g2-handoff-md-mandate §8.1 + g1-browser-walk §7.1 + memory auto-load §6.3; detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 2, FP risk KC platform-side exception) | Author + reviewer + memory auto-load | ✅ DONE (2026-06-09 — KH-3/KC-8 recipe FE port drift: rule v1.0.0 + canonical matrix + memory + worked self-test §5 — KH-3 `:3000`→`:3001` + KC-8 `:3001`→`:3000` + KC-2 platform-side không false-positive; META P1 force-multiplier prospective mọi KH/KC reference) |
| **Worktree-only branch work** (mọi công việc trên nhánh ≠ nhánh hiện tại của main working tree PHẢI dùng `git worktree add`; KHÔNG `git checkout`/`git switch` switch main tree sang nhánh khác — main tree là shared filesystem state giữa nhiều phiên/agent đồng thời) | `.claude/rules/worktree-only-branch-work.md` §1 (worktree per nhánh) + §3 workflow + §4 banned checkout/switch | Self-detection §8.1 (trước `git checkout`/`switch`) + reviewer-checklist §8.2 (PR work từ worktree) + memory auto-load §8.3 (always-load §7 justified — git-decision-time); detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (arg-parse FP risk, recurrence 1) | Author + reviewer + memory auto-load | ✅ DONE (2026-06-09 — main-tree checkout-swap incident: phiên song song checkout `feature/wizard-redesign-gaps` vào shared main tree giữa session; rule v1.0.0 + memory + worked self-test §6 — worktree-per-branch counterfactual main tree ổn định; META P1 force-multiplier prospective mọi multi-branch work) |
| **Multi-session concurrency coordination** (≥2 session độc lập cùng lúc tranh shared monotonic/append resources — gap-ID `max+1`, migration-version `max(V)+1` Flyway, `gap-status.csv` EOF append, branch ownership; session-lock chỉ là HINT không govern allocation) | `.claude/rules/multi-session-concurrency-coordination.md` §1 (branch riêng off main qua worktree + reserve block gap-ID/migration-version trong lock file + check lock session khác TRƯỚC khi allocate + additive-resolve gap-status.csv) + §3.1 lock-file YAML schema extension (`reserved_gap_ids` + `reserved_migration_versions` + `owned_branches`) | Pre-merge reviewer-checklist §8.1 (PR body `## Multi-session coordination` + disjoint reserved block) + §8.2 self-detection (ls locks tại session-start + allocate-time) + path-scoped auto-load `.claude/session-locks/**` + `documents/04-quality/gaps/**` + 2 db/migration dirs; memory auto-load §8.3; `collect-state.sh` auto-reserve detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (non-trivial executable-script automation + recurrence 1) | Allocator session + reviewer + memory auto-load | ✅ DONE (2026-06-10 — GAP-1114: lần đầu 2 session độc lập cùng branch → GAP-1111 collision + V96 collision + gap-status.csv 3-way conflict; rule v1.0.0 + memory + worked self-test §6 — disjoint reserved-block → 0 collision; META P1 force-multiplier prospective mọi multi-session run subsequent) |
| **Retention-policy completeness** (CI sinh artifact category mới — ECR tag / S3 prefix / log stream — retention/lifecycle policy PHẢI cap nó HOẶC ghi keep-forever-có-chủ-đích; uncapped family = unbounded growth + silent cost creep) | `.claude/rules/retention-policy-completeness.md` §1 (lifecycle policy cap mọi CI-produced tag-family) + §3 required artifacts (cap rule OR keep-forever comment) | Static CI detector `scripts/check-ecr-lifecycle-coverage.sh` (job `ecr-lifecycle-coverage` trong `quality-infra.yml`, blocking + self-test fixtures) + reviewer-checklist §7.2 + path-scoped auto-load `ecr.tf`/`docker-build-push.yml`; detector scope-extension (S3/log/snapshot) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 1) | CI + reviewer | ✅ DONE (2026-06-10 — PR #2288 ECR cost-cleanup: GAP-402 cosign `sha256-*.sig/.att` rớt qua mọi lifecycle rule → ~2,780 image tích vô hạn / AWS vượt credit; rule v1.0.0 + static detector + collect-state ECR-count early-warning + worked self-test §6 detector FAIL on pre-fix / PASS on post-fix; META P1 force-multiplier prospective mọi lifecycle-policy / artifact-CI change auto-catch uncapped category) |
| **Agent concurrency budget + inline-hybrid** (khi giảm số agent spawn tránh server/rate limit — coordinator PHẢI lấp idle bằng inline bucket disjoint song song agent đang chạy; "ít agent" đi kèm "coordinator làm inline" KHÔNG phải "ngồi chờ") | `.claude/rules/agent-concurrency-budget-inline-hybrid.md` §1 (concurrency budget + inline compensation) + §2 decision model (heavy-cohesive→agent / small-disjoint→inline) + §2.3 lấp-idle nguyên tắc | Self-detection §7.1 (sau spawn → còn bucket inline-able? làm ngay) + reviewer-checklist §7.2 + memory auto-load §7.3 + path-scoped auto-load `wave-pack-planner/**` + `waves/**`; detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (NLP idle-detection, recurrence 1) | Coordinator + reviewer + memory auto-load | ✅ DONE (2026-06-11 — demo-seed wave: spawn 1 Opus agent đúng budget ≤1 tránh limit nhưng định để E/F "sau khi #1 xong" → coordinator idle; rule v1.0.0 + memory + worked self-test §6 — viết rule này inline song song agent #1 = pattern; META P1 force-multiplier prospective mọi wave limit-bound subsequent giữ wall-clock thấp) |

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
- **2026-06-11** (v1.25.0): MINOR — added §3 matrix row "Agent concurrency budget + inline-hybrid" tracking new rule `.claude/rules/agent-concurrency-budget-inline-hybrid.md` v1.0.0 (paired same-PR demo-seed wave 2026-06-11). Triggered by user direction 2026-06-11 "sửa meta, để tránh server limit sẽ spawn ít agent và làm inline bucket để bù vào, mục tiêu là tiết kiệm thời gian". Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule v1.0.0 + matrix row + rules-index.csv row + memory `feedback_agent_concurrency_budget_inline.md` + worked self-test §6 (demo-seed wave — spawn 1 Opus agent đúng budget ≤1 nhưng coordinator idle; counterfactual: làm Bucket F inline song song) all paired same PR. META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn hybrid → mọi wave limit-bound subsequent giữ wall-clock thấp mà không chạm limit. Sister cho `agent-model-opus-default` (model axis) + `agent-background-spawn-default` (sync axis) + `feedback_parallel_agent_strategy` (max-5 cap) tại axis mới: concurrency-budget-LOW + inline-compensation. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "agent concurrency orchestration"; no constraint loosening; existing wave executions grandfathered; rule applies prospectively từ this PR forward).
- **2026-06-11** (v1.24.0): MINOR — extended §3 matrix row "User-facing flow G1 browser-walk readiness" Standard+Process+Status cells để cite `g1-browser-walk-before-flip.md` v1.2.0 §3.2 (nguyên tắc tổng quát "production access-mode PHẢI local-reproduce-được" generalizing §3.1 subdomain case to any access/entry mechanism) + campaign `flow-verification-campaign.md` §1 G2★-absorbs-G3-functional gate redefinition (2026-06-11). Triggered by user direction 2026-06-11 4-part: (1) "đã có meta quy định local test = production (vd subdomain)?" → confirmed §3.1 covers subdomain + §3.2 generalizes; (2) landing-100 test bằng subdomain + production criteria → G2★ gate mandate subdomain/nip.io; (3) "gộp G2,G3" → user chốt AskUserQuestion "G2★ absorbs G3-functional" (G2 = human production-accurate browser walk hấp thụ functional-parity; G3-infra = AWS-only residual gated GAP-612); (4) state-check sync GAP-1077 OPEN→PARTIAL (code shipped 06-09, chờ nip.io walk). Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: g1-browser-walk v1.2.0 §3.2 + campaign §1 gate redefinition + rules-index.csv version bump + GAP-1077 status sync + this matrix row all paired same PR. META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn access-mode-parity + G2★ merge → mọi flow (host-based/OAuth/JWT-routing) G1/G2★ subsequent verify đúng production access path. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — tracks updated rule version + campaign gate for previously-covered output type "G1 browser-walk readiness"; no constraint loosening; existing walks grandfathered re-verify per campaign loop).
- **2026-06-10** (v1.23.0): MINOR — added §3 matrix row "Retention-policy completeness" tracking new rule `.claude/rules/retention-policy-completeness.md` v1.0.0 (paired same-PR #2288 ECR cost-cleanup). Triggered by user question 2026-06-10 "cần update meta để tránh lỗi không?" sau ECR cost-cleanup (4,210 → ~895 images, −79% prune). Root cause: GAP-402 cosign keyless signing sinh `sha256-*.sig/.att` nhưng `ecr.tf` lifecycle policy chỉ cap prefix `sha-`/`main`/`latest`/`pr-` → `sha256-` rớt qua mọi rule → ~2,780 image tích vô hạn / AWS vượt credit. Statically-detectable class per `cross-flow-bug-class-sweep.md` §4.1 → persistent CI detector `scripts/check-ecr-lifecycle-coverage.sh` (job `ecr-lifecycle-coverage` quality-infra.yml) shipped same PR + collect-state.sh ECR-count early-warning + worked self-test §6 (detector FAIL on pre-fix ecr.tf / PASS post-fix). Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate. META P1 force-multiplier per `meta-gap-priority.md` §3 — mọi lifecycle-policy / artifact-CI change subsequent auto-catch uncapped category. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "storage retention coverage"; no constraint loosening; existing storage policies grandfathered (ECR fixed same PR); rule applies prospectively).
- **2026-06-10** (v1.22.0): MINOR — added §3 matrix row "Multi-session concurrency coordination" tracking new rule `.claude/rules/multi-session-concurrency-coordination.md` v1.0.0 (paired same-PR GAP-1114 2026-06-10). Triggered by 2026-06-10 incident: lần đầu chạy 2 session độc lập cùng lúc trên cùng branch `feature/tier-ui-fix-g2-browser-2026-06-09` — mỗi session 1 coordinator riêng KHÔNG share state in-memory → va chạm THẬT ở shared monotonic/append resources (cả 2 tính `max(gap-id)+1` → cùng GAP-1111; cùng V96 migration; gap-status.csv 2+ EOF append merge conflict; branch ownership ambiguous do session-lock chỉ là HINT). Tự-tham-chiếu: author rule này spawn agent worktree thứ 3 → 3-way collision GAP-1111/1112/1113/1114. Per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate: new rule v1.0.0 + matrix row + rules-index.csv row + reviewer-checklist + memory `feedback_multi_session_concurrency.md` + worked self-test §6 + GAP-1114 closure all paired same PR. META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn reserve-block → mọi multi-session run subsequent allocate disjoint ID-space → eliminate collision class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds coverage row for previously-uncovered output type "multi-session shared-resource coordination"; no constraint loosening; existing single-session work grandfathered; rule applies prospectively từ this PR forward). Version v1.22.0 (not v1.21.0) — v1.21.0 taken by G2-handoff entry merged from main (#2279).
- **2026-06-09** (v1.21.0): MINOR — extended §3 matrix row "G2 handoff recipe MD" Standard+Process+Status cells để cite `g2-handoff-md-mandate.md` v1.0.2 §3.4 browser-walk-for-FE-wired mandate (curl chỉ cho BE-only affordance, label rõ). Triggered by 2026-06-09 user-flagged miss: KH-5/KH-6/KC-8 G2 recipe curl-only bỏ lọt affordance FE-wired (downgrade/cancel có FE) trong khi G2 = human walk UI thật. Paired same-PR với `g2-handoff-md-mandate.md` v1.0.2 §3.4 hard-requirement + `flow-verification-campaign.md` §1 G2 gate edit + GAP-1092 (META coverage DONE) + GAP-1093 (renew-no-FE OPEN) per `incident-to-rule-pipeline.md` 5-stage + `rule-change-process.md` §6.5 Enforcement Parity Mandate. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — tracks updated rule version for previously-covered output type "G2 handoff recipe MD"; no constraint loosening; existing recipes grandfathered).
- **2026-05-31** (v1.20.1): PATCH — Wave meta context-budget streamline. Moved §11 Log entries v1.18.0 → v1.12.0 (10 entries) to `_examples/output-review-mandate-log-history.md` to bring rule body under Anthropic 40k auto-load threshold (was 44.7k chars → triggered perf warning). Paired same batch with 11 sibling rules gaining `paths:` frontmatter per `context-budget-mandate.md` §3.2 — base session rule footprint cut ~49k tokens. No constraint change; canonical §3 matrix + §1-§10 body preserved. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — body-only streamline, no rule semantics affected; existing audits/refs grandfathered).



> **Entries v1.20.0 .. v1.0.0 moved to [`_examples/output-review-mandate-log-history.md`](_examples/output-review-mandate-log-history.md)** per Wave 99 streamline rounds 1+2+3 + 2026-06-11 context-budget trim (rule body was exceeding Anthropic 40k char auto-load threshold + always-load byte ceiling). Body retains v1.25.0 → v1.20.1.
