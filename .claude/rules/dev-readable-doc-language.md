---
paths:
  - "documents/**/*.md"
  - "documents/05-guides/operations/acceptance-tests/**/*.csv"
---

# Dev-Readable Doc Language — Vietnamese for narrative, English for identifiers

**Priority:** 🟠 MANDATORY — communication language governance
**Version:** 1.0.3
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.0.2 PATCH self-approve per `rule-change-process.md` §5; Wave 99B post-closure 2026-05-19 — user-flagged §2 scope-list missing explicit "Architecture docs" row (`documents/02-architecture/**`). Coverage gap surfaced khi 14 arch files có `audience: mixed` (Claude + dev both consume via path-scoped auto-load) nhưng rule §2 không enumerate arch scope → "không chuẩn rule ngôn ngữ" cho dev reading. §2 extended với row "Architecture docs (`documents/02-architecture/**`)" + §11 Log v1.0.2 documenting per `incident-to-rule-pipeline.md` 5-stage. v1.0.1 (giữ): PATCH self-approve per §5; Wave 73 Bucket A3 thêm `paths:` frontmatter. v1.0.0 (giữ): MINOR self-approve per §5; new rule với built-in enforcement per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies CLAUDE.md §"CRITICAL: Communication Language" cho previously-uncovered class)
**Applies to:** Every dev-readable doc artifact a developer reads during work (gap files, runbooks, acceptance tests, end-user guides, planning docs body, audit reports, guide READMEs, business docs prose, architecture docs). Scope explicitly EXCLUDES code source, code comments, commit messages, PR titles, technical identifiers/enums/config keys (those follow standard English convention per CLAUDE.md "Code comments can be in English / Commit messages should be in English").

---

## 1. The Rule

> **Mọi document/artifact mà developer cần đọc trong khi làm việc PHẢI dùng tiếng Việt cho narrative content** (mô tả, giải thích, instruction, scope context). **Technical identifiers + enums + config keys + code-shaped tokens giữ English** (cross-locale stable, không dịch).

CLAUDE.md §"CRITICAL: Communication Language" mandate: "ALL responses, explanations, and documentation should be in Vietnamese". Rule này codify mandate đó cho dev-readable artifact scope — tách rõ "narrative để hiểu" (Việt) vs "identifier để parse/grep" (English).

Wave 72a Bucket F (2026-05-14) ship acceptance test CSV với narrative cells English (`Open KiteHub landing page`, `Click 'Pricing' link in nav`, `View Terms of Service`, `HTTP 201 confirmation banner`) → user flagged "Vietnamese devs không đọc fluidly được; vi phạm CLAUDE.md". Rule này close coverage gap.

---

## 2. What counts as dev-readable (mandatory Vietnamese)

| Artifact class | Examples | Scope |
|---|---|---|
| **Gap files** (`documents/04-quality/gaps/GAP-*.md`) | Problem / Root Cause / Proposed Fix / Acceptance Criteria / Log | Narrative + bullet content |
| **Runbooks** (`documents/05-guides/{deploy,operations,account-prep}/**.md`) | Steps, troubleshooting, when-to-run | Narrative + table content |
| **Acceptance test matrix narrative cells** (`*.csv`) | `step_title`, `action`, `expected_result`, `verify_via`, `notes` columns | Cell values (xem §4 cho column-vs-value split) |
| **Planning doc body** (`documents/03-planning/**.md`) | Wave plans, scope, brainstorm, task breakdown | Narrative outside frontmatter |
| **Audit reports** (`documents/04-quality/audits/**.md`) | Findings, recommendations, scope, methodology | Narrative |
| **Business docs** (`documents/01-business/**.md`) | Use cases, business rule rationale (per `business-logic-review.md` §2 attributes), error description | Narrative — config keys English |
| **Guide READMEs** (`documents/05-guides/**/README.md`) | Folder index, file placement rules, archive policy | Narrative |
| **Architecture docs** (`documents/02-architecture/**/*.md`) **— `audience: mixed`** | System architecture (service catalog, dependency graph, multi-tenant, compliance map, database map, C4 L1+L2, deployment strategy, SSL, domain, email, env-vars, retention policy). Narrative + Mermaid diagram captions + table column descriptions | Narrative tiếng Việt + English technical identifiers (service names, ports, env vars, HTTP tokens, Mermaid syntax) per §4 mixed-language. Note: ADR files trong `adr/` follow §8.3 ADR-title exception (English title acceptable, body Vietnamese narrative preferred). |
| **End-user docs / customer-facing** | TOS, Privacy, FAQ shown to tenant | Vietnamese (PDPL compliance angle + user-facing) |

Narrative content = ngôn ngữ tự nhiên để dev hiểu context, không phải code/data.

---

## 3. What is ACCEPTABLE English (no Vietnamese required)

| Artifact class | Examples | Why |
|---|---|---|
| **Code source** | `*.java`, `*.ts`, `*.tsx`, `*.py`, `*.sql` | CLAUDE.md mandate — standard practice |
| **Code comments** | `// inline comment` trong source | CLAUDE.md mandate |
| **Commit messages** | `feat(scope): description` | CLAUDE.md mandate — git convention |
| **PR titles** | conventional commits | git convention |
| **Technical identifiers** | `flow_id`, `persona`, `step_num`, `tier`, `status`, `phase` (column names) | Cross-locale stable; downstream tooling parses |
| **Enum values** | `P2_CENTER_OWNER`, `PENDING`, `APPROVED`, `BETA_APPROVE` | Code-shaped tokens |
| **Config keys** | `kitehub.trial.duration-days`, `ai.input.cap.free` | Reference từ code |
| **HTTP / protocol tokens** | `HTTP 201`, `POST /api/v1/...`, `JWT`, `CORS`, `OIDC`, `TLS` | Technical lexicon |
| **Brand/product names** | `KiteHub`, `KiteClass`, `Resend`, `AWS SES`, `Cloudflare` | Proper nouns |
| **CLI flags / commands** | `--dry-run`, `gh pr merge`, `terraform apply` | Verbatim from tool |
| **File paths** | `documents/05-guides/operations/` | Verbatim filesystem |
| **License headers** | SPDX / Apache 2.0 | Legal convention |
| **ADR title prefix** | `ADR-NNN: Decision title` | Optional English title acceptable; body Vietnamese narrative preferred |

---

## 4. Mixed-language rule (most common case)

Vietnamese narrative containing English technical terms = natural code-switching trong Vietnamese tech writing. Vd:

- ✅ "Mở browser → click link 'Pricing' trên nav"
- ✅ "Submit form → kỳ vọng HTTP 201 + JWT trong response body"
- ✅ "Verify row DB tồn tại với status=PENDING"
- ✅ "Counter quota hiển thị: 'Gói FREE: còn 3/3 lần regenerate hôm nay'"
- ✅ "SSM: docker exec kite-postgres psql -U kite -d kitehub"

Vietnamese sentences với English technical tokens trong code-style backticks hoặc câu naturally = ACCEPTABLE — không cần dịch HTTP/JWT/CORS/POST/DB/SSM sang Việt (gây buồn cười + mất context).

---

## 5. Banned patterns

| ❌ Banned | ✅ Required |
|---|---|
| English narrative trong gap file `## Problem` section | Vietnamese narrative + technical token English inline |
| `View Terms of Service` trong acceptance test step_title cell | `Xem trang Điều khoản dịch vụ` |
| `Login as admin` trong action cell | `Đăng nhập với tài khoản admin` |
| `HTTP 201 confirmation banner` trong expected_result cell | `Banner xác nhận HTTP 201` |
| `Click 'Submit' button` (English narrative) | `Click nút 'Gửi yêu cầu' / 'Submit'` (Vietnamese narrative + English label as alt) |
| `Browser DevTools no console error` | `DevTools không có console error` |
| Translate `flow_id` column name sang `ma_buoc` | Keep English column names — technical identifier cross-locale stable |
| Translate `P2_CENTER_OWNER` enum sang `P2_CHU_TRUNG_TAM` | Keep enum English — code-shaped token |
| Translate config key `kitehub.trial.duration-days` | Keep English — references code |

---

## 6. Worked self-test — Wave 72a Bucket F incident (2026-05-14)

**Scenario:** Wave 72a Bucket F ship 126-row acceptance test CSV với narrative cells English. User flagged Q3 violation.

Sample original rows (vi phạm rule §2 + §5):

```
PUB-LAND-001,Anonymous,Setup,1,Open KiteHub landing page,none,Mở browser → navigate to https://kitehub.me/,...,"HTTP 200; hero section renders Vietnamese tone; CTA 'Request Beta Access' button visible","curl -sI https://kitehub.me/ → 200; browser DevTools no console error",,,
```

| Cell | Original (English) | Rule §2 verdict | Required Vietnamese |
|---|---|---|---|
| `step_title` | `Open KiteHub landing page` | FAIL — narrative | `Mở trang chủ KiteHub` |
| `action` | `Mở browser → navigate to https://kitehub.me/` | partial — đã code-switch | `Mở browser → truy cập https://kitehub.me/` |
| `expected_result` | `HTTP 200; hero section renders Vietnamese tone; CTA 'Request Beta Access' button visible` | FAIL — narrative | `HTTP 200; hero section render đúng tone tiếng Việt; nút CTA 'Yêu cầu truy cập Beta' hiển thị` |
| `verify_via` | `curl -sI https://kitehub.me/ → 200; browser DevTools no console error` | partial — mostly code | `curl -sI https://kitehub.me/ → 200; DevTools không có console error` |
| `flow_id` | `PUB-LAND-001` | ✅ English required (§3) | (unchanged) |
| `persona` | `Anonymous` | ✅ English required (§3 enum) | (unchanged) |
| `phase` | `Setup` | ✅ English required (§3 enum) | (unchanged) |

→ Rule fires correctly: 3 narrative cells FAIL English; 2 mixed-content cells need slight polish (English residue); identifier/enum cells PASS untouched. Counterfactual: nếu rule landed trước Bucket F, agent ship Vietnamese narrative từ đầu → eliminate user Q3 flag.

**Verdict:** rule fires correctly trên originating incident. Self-test PASS ✅

**Same-PR fix:** all 126 rows translated to Vietnamese for narrative cells; English column names + enums + identifiers + sample VN-friendly data preserved.

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist

Pre-merge review cho PR touching dev-readable artifacts (per §2 scope):

- [ ] Narrative content (Problem / Action / Expected / Verify / Notes / body text) tiếng Việt?
- [ ] Technical identifiers (column names / enums / config keys / file paths / HTTP tokens / brand names) tiếng Anh giữ nguyên?
- [ ] Mixed-language sentences tự nhiên (Vietnamese narrative + inline English token)?
- [ ] Acceptable English class (per §3) đúng scope (code source / commit / identifier)?

### 7.2 CI grep detector (HONEST DEFER — heuristic FP risk inherently high)

Per Wave 99C META-META GAP-675 audit (2026-05-19): detector HONEST-deferred per `incident-to-rule-pipeline.md` §3 tightened legitimate-deferral conditions:
- **Detector complexity:** English narrative detection in mixed Vietnamese+English context inherently ambiguous — code-switching natural per §4 mixed-language rule (`HTTP 201`, `JWT`, `CORS` valid English tokens trong Vietnamese sentences)
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-14, ~5 days at audit time; no recurrence of English-narrative-only artifact yet)
- **FP risk:** Very high — any acceptable English token (per §3 acceptable English class) trigger false positive
- **Decision:** Reviewer-checklist §7.1 + worked self-test §6 (Wave 72a Bucket F CSV translation) sufficient cho v1.0.0; revisit detector when recurrence-count ≥2 OR proven NLP language classifier available

Heuristic regex (when eventually implemented):

```bash
# Trong .github/workflows/quality-docs.yml hoặc .husky/pre-commit
grep -rnE "^[^|`'\"#]*\b(Click|Open|View|Submit|Enter|Navigate|Verify|Check) (the |a |to )?\b" \
  documents/05-guides/operations/acceptance-tests/*.csv \
  documents/04-quality/gaps/*.md \
  documents/05-guides/**/*.md \
  2>/dev/null | grep -v ':[A-Z]*-[A-Z]*-[0-9]+,' \
  && { echo "WARN: possible English narrative in dev-readable doc — review per dev-readable-doc-language.md"; exit 0; }
```

False positives expected (some English phrases legitimate inside code blocks); WARN only, not BLOCK. Track follow-up gap khi rule stabilize.

### 7.3 Memory auto-load (per-session)

Memory entry `feedback_dev_readable_doc_language.md` (optional, paired same-PR or follow-up) reminds at session start to translate narrative content for dev-readable artifacts.

### 7.4 Override mechanism

Genuine exception (vd: external compliance template English-only, regulator-required English):

```
git commit -m "...
DEV_READABLE_LANG_OVERRIDE: <reason — explain why English narrative warranted, link to standard if applicable>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 8. Edge cases + clarifications

### 8.1 Doc đa-ngôn-ngữ (rare)

Nếu doc designed cho cross-locale audience (vd public marketing copy targeting both VN + international users), ship 2 versions: `<stem>.vi.md` + `<stem>.en.md`. Default version (no suffix) = Vietnamese theo CLAUDE.md.

### 8.2 Quote external English source

Quoting public docs/spec/error message English KHÔNG cần dịch:

```markdown
Per AWS docs: "CloudTrail management events (first copy) FREE."
```

Quote markers (`>`, `"..."`) signal verbatim source — translation losses meaning.

### 8.3 ADR titles

ADR title English acceptable (vd `ADR-025: AWS Singapore Free Tier for Phase 1 BETA`); body narrative SHOULD be Vietnamese for dev-readable scope. Existing ADRs grandfathered until next major revision.

### 8.4 ROADMAP entries

ROADMAP narrative Vietnamese; gap IDs / wave IDs / PR refs English (identifier scope per §3).

### 8.5 Test fixture data

Test data in code (Java/TS) tự do dùng English variable names + Vietnamese sample values (vd `String fullName = "Nguyễn Thị Hương"`). Acceptable — code scope per §3.

---

## 9. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| "Doc này nhỏ, English fast enough" | Per CLAUDE.md mandate — Vietnamese for ALL dev-readable docs regardless size |
| Translate technical token (HTTP → "giao thức HTTP") in mid-sentence | Code-switch natural: "kỳ vọng HTTP 201" not "kỳ vọng giao thức HTTP mã 201" |
| Translate column name (`status` → `trang_thai`) | Keep English column name — downstream parse stable |
| Translate enum (`PENDING` → `DANG_CHO`) | Keep English enum — code-shaped token |
| Ship English narrative "because copy-paste from prior PR" | Translate during copy — costs minutes, saves user retro round-trip |
| English narrative inside backticks "because it looks code-like" | Backticks for actual code/identifier only; narrative outside backticks Vietnamese |
| Bilingual narrative everywhere "for safety" | One language per cell — Vietnamese for narrative, English for identifier. Mixed sentence OK; mixed paragraph confusing |

---

## 10. Relationship to other rules

- **`test-artifact-format-standard.md`** (sister rule, same PR) — §3.2 cites this rule cho column-name vs value language split trong CSV
- **CLAUDE.md §"CRITICAL: Communication Language"** — this rule codifies + specializes mandate cho dev-readable doc scope
- **`output-review-mandate.md`** §3 — adds row "Dev-readable doc language" tracking review standard
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + §6 self-test + §7 reviewer-checklist + meta-CSV-index row all ship same PR
- **`incident-to-rule-pipeline.md`** — this rule = direct output of Wave 72a Bucket F user-flagged Q3 (English content) applied through 5-stage pipeline
- **`business-logic-review.md`** — business rule narrative Vietnamese; config keys English (consistent với §2/§3 split)
- **`audit-to-gap-pipeline.md`** §3 — gap template Problem/Root Cause/Proposed Fix in Vietnamese
- **`pre-handoff-self-test-completeness.md`** — checklist narrative Vietnamese
- **`readme-content-discipline.md`** — root README Vietnamese narrative + English code/version (overlap với §3)

---

## 11. Log

- **2026-05-31** (v1.0.3): PATCH — fixed 1 stale CI reference(s) `script-quality.yml` → `quality-docs.yml` (workflow was split into quality-{code,docs,rules-skills,infra}.yml 2026-05-22; this rule's §Enforcement still pointed to the removed file). Historical Log entries left unchanged per `rule-change-process.md` §7 append-only. No constraint change. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — broken-link fix).

- **2026-05-19 (v1.0.2):** PATCH — Wave 99B post-closure 2026-05-19 user-flagged coverage gap: §2 "What counts as dev-readable" scope-list MISSING explicit "Architecture docs" row (`documents/02-architecture/**`). Issue surfaced khi 14 arch files có `audience: mixed` (Claude + dev both consume — path-scoped auto-load) nhưng rule §2 enumerate scope (gap files / runbooks / acceptance tests / planning / audit / business / guide READMEs / end-user docs) KHÔNG cover architecture docs — "không chuẩn rule ngôn ngữ" cho dev đọc. Architecture docs ALREADY use correct content pattern (Vietnamese narrative + English identifiers per §4) — rule §2 scope-list cần extend explicit cover. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "cho dev đọc thì không chuẩn rule ngôn ngữ" 2026-05-19) → Classify ✓ (rule §2 scope coverage gap; arch docs implicit covered bởi CLAUDE.md broad mandate "ALL documentation in Vietnamese" nhưng §2 enumerate scope list không list architecture → explicit row needed) → Rule+Enforce ✓ (§2 +1 row "Architecture docs documents/02-architecture/**/*.md — audience: mixed" + paired same-PR with: 12 arch files audience: dev → mixed flip + `docs-filename-prefix-convention.md` §4 audience field clarified — all in PR #1579 per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (existing 14 arch files content pattern already Vietnamese narrative + English identifiers → rule §2 extension validates current state without content edit; no migration debt) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — additive scope-list row + clarification; no constraint loosening; existing arch docs grandfathered with no content edit required since content already follows §4 mixed-language pattern; rule applies prospectively cho new architecture docs).
- **2026-05-14 (v1.0.1):** Wave 73 Bucket A3 — thêm `paths:` frontmatter (`documents/**/*.md`, `documents/05-guides/operations/acceptance-tests/**/*.csv`) cho Anthropic native deferred-loading. Path-scope MANDATORY rule giúp tiết kiệm token context khi session không động chạm dev-readable docs hoặc acceptance test CSV. Không đổi constraint, scope rule giữ nguyên. Sync `rules-index.csv` path_trigger column. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-05-14 (v1.0.0):** Rule created. Triggered by Wave 72a Bucket F user-flagged Q3 (English content trong acceptance test CSV) — user wrote "ALL responses, explanations, and documentation should be in Vietnamese (theo CLAUDE.md); CSV content English (`Open KiteHub landing page`, `View pricing page`, `View Terms of Service`, etc.) — Vietnamese devs cannot read fluidly". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged specific English narrative violations) → Classify ✓ (no existing rule codifies CLAUDE.md mandate cho dev-readable doc scope; mandate sits in top-level CLAUDE.md but no enforcement parity, no scope clarification, no column-vs-value split rule for test artifacts) → Rule+Enforce ✓ (this file + sister rule `test-artifact-format-standard.md` cross-references §3.2 + paired same-PR with: 126-row CSV translation + folder relocation + `output-review-mandate.md` §3 row + rules-index.csv 2 new rows per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 worked example on Wave 72a CSV row PUB-LAND-001 — 3 cells FAIL, 2 partial, 3 identifier-cells PASS) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint codifying previously-implicit CLAUDE.md mandate for dev-readable scope; no constraint loosening; existing English-narrative docs grandfathered until next refresh; rule applies prospectively to new artifacts từ session sau). CI grep detector + memory auto-load deferred per premature-rule guard ≥7 ngày; enforcement = reviewer-checklist + self-detection + worked self-test sufficient cho v1.0.0.
