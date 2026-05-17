---
paths:
  - "documents/03-planning/**"
  - "documents/01-business/**"
  - "documents/02-architecture/adr/**"
  - "documents/05-guides/**"
  - "documents/04-quality/gaps/GAP-*.md"
  - ".claude/rules/**"
  - ".claude/skills/**"
  - "**/vercel.json"
  - ".github/workflows/*.yml"
  - "kitehub/kitehub-frontend/src/**"
  - "kiteclass/kiteclass-frontend/src/**"
---

# No-Vercel-References — Vercel deprecated, decommission in progress

**Priority:** 🟠 MANDATORY — vendor governance
**Version:** 1.0.0
**Created:** 2026-05-17
**Last-Reviewed:** 2026-05-17
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (path-scoped auto-load + reviewer-checklist + worked self-test on Wave 88 sweep scope) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies vendor decision Wave 82 self-host pivot + Wave 88 full decommission)
**Applies to:** Mọi PR / docs / planning / business / runbook / decision-doc / rule / skill / gap file / wave plan / commit message từ 2026-05-17 trở đi. Code references trong production fallback path grandfathered cho tới khi Wave 88 cutover execute (PR #1466 5-gate).

---

## 1. The Rule

> **Vercel KHÔNG còn là supported solution cho KiteHub FE hosting kể từ Wave 82 (2026-05-15 self-host pivot) và Wave 88 (2026-05-17 full decommission).** Mọi artifact mới (PR, docs, planning, decision-doc, code addition) KHÔNG được:
>
> 1. Đề xuất Vercel as new architecture choice
> 2. Add new code path consuming Vercel-specific APIs/SDKs
> 3. Document Vercel as production hosting solution
> 4. Reference `kitehub.vercel.app` URL trong end-user-facing docs hoặc external comms
>
> Existing Vercel references grandfathered tới khi Wave 88 sweep complete. Production cutover via PR #1466 5-gate sequence (dev-trigger required).

---

## 2. Why this rule exists

**Wave 82 pivot (2026-05-15):** Outside-in audit 2 agents (external benchmark + failure-mode matrix) đều recommend Vercel Pro $20/mo. User locked **AWS EC2 self-host** vì:
- Cost-priority (Free Tier exhausted; Pro $20/mo/user không scale)
- Vendor consistency (lock entire stack on AWS)
- Avoid Free Tier build cap (100/day hit 2026-05-13)
- Avoid double-vendor data residency complexity (VN PDPL angle)

**Wave 88 decommission (2026-05-17):** User explicit "cấm nhắc đến vercel nữa". Rule formalizes decision + paired same-wave với:
- Sweep planning + business + guides docs (Bucket B Wave 88)
- Sweep code production refs (Bucket C Wave 88)
- Execute CF apex cutover PR #1466 5-gate (Bucket D Wave 88, dev-trigger)
- Verify post-cutover DNS not Vercel (Bucket E Wave 88)

---

## 3. Forbidden patterns (new artifacts)

| ❌ Don't | ✅ Do |
|---|---|
| `documents/03-planning/...md` propose "deploy FE to Vercel" | Propose "deploy FE to AWS EC2 self-host (Wave 82 pattern)" |
| Gap file Proposed Fix mention Vercel as option | Recommend self-host AWS EC2 + nginx + PM2 (per Wave 82 Bucket B pattern) |
| ADR consider Vercel | ADR-XXX-fe-self-host.md là canonical hosting decision |
| Runbook reference `kitehub.vercel.app` cho dev/test | Reference `kitehub.me` apex (CF→EC2 cutover) hoặc EC2 direct IP cho pre-cutover smoke |
| Code: `import { ... } from '@vercel/...'` mới | Self-hosted alternatives (no Vercel SDK dependency) |
| Commit message "deploy via Vercel" | "deploy via EC2 self-host" |
| Email/marketing copy URL `https://kitehub.vercel.app` | `https://kitehub.me` post-cutover (defer khi pre-cutover — use neutral wording) |
| Plan Bucket "Vercel preview deploy" | "EC2 staging deploy" hoặc "self-host preview" |

---

## 4. Allowed exceptions (grandfathered + reality docs)

| Case | Why exempt |
|---|---|
| `documents/07-archived/**` historical refs | Preserve history per `docs-folder-structure.md` §archive policy |
| `documents/04-quality/audits/**` past audit findings | Audit artifacts immutable per `output-review-mandate.md` §3 |
| Wave plan Brainstorm Q1/Q2 mentioning Vercel pivot decision context | Decision rationale needs Vercel mention to explain pivot — historical context |
| Wave 82/87/88 specific cutover docs | Refs unavoidable in transition docs |
| Code path serving Vercel production traffic pre-cutover | Grandfathered until Wave 88 Bucket D cutover execute |
| Reality-state docs: "production currently serves via Vercel" | Documenting current state ≠ recommending Vercel; ok if neutral wording |
| Memory entries about past Vercel-related decisions | Historical record |

When invoking exception, document inline: "Vercel mention here is historical/transition; per `no-vercel-references.md` §4 exception <row>."

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active immediately)

Pre-merge review cho PR touching:
- `documents/03-planning/**`, `documents/01-business/**`, `documents/02-architecture/adr/**`, `documents/05-guides/**`
- `.claude/rules/**`, `.claude/skills/**`
- Any source code path (`*.ts`/`*.tsx`/`*.java`/`*.yml`/`*.tf`)

Reviewer asks:
- Diff có introduce mention Vercel mới không?
- Nếu CÓ → match §4 exception nào? Document inline?
- Nếu KHÔNG match exception → request rewrite trước merge

### 5.2 CI grep detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày)

Future enhancement — `scripts/check-no-vercel-references.sh`:

```bash
# Scan new lines in PR diff (git diff origin/main...HEAD)
git diff origin/main...HEAD --no-renames -G "[Vv]ercel" -- \
  ':(exclude)documents/07-archived/' \
  ':(exclude)documents/04-quality/audits/' \
  ':(exclude)documents/03-planning/waves/wave-*82*.md' \
  ':(exclude)documents/03-planning/waves/wave-*87*.md' \
  ':(exclude)documents/03-planning/waves/wave-*88*.md' \
  ':(exclude).claude/rules/no-vercel-references.md' \
  | grep -E "^\+.*[Vv]ercel" \
  && { echo "WARN: new Vercel reference detected — review §3/§4 of no-vercel-references.md"; exit 0; }
```

WARN-only initially; track follow-up gap stabilize. Defer wiring CI job đến Wave 89+ sau v1.0.0 paths đã pulled mọi Vercel ref trong production code.

### 5.3 Memory auto-load (optional, deferred)

Memory entry `feedback_no_vercel_references.md` could remind session start. Defer until 2nd reviewer-flagged miss; reviewer-checklist + worked self-test sufficient cho v1.0.0.

### 5.4 Path-scoped auto-load (active)

`paths:` frontmatter — rule load khi Claude touch active-docs paths above + code source. Per `context-budget-mandate.md` §3.1 path-scope justified.

### 5.5 Override mechanism

Genuine exception ngoài §4 list:

```
git commit -m "...
NO_VERCEL_OVERRIDE: <reason — explain why Vercel mention warranted in new artifact>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review (likely §4 list mis-defined hoặc decommission incomplete).

---

## 6. Worked self-test — Wave 88 sweep scope (2026-05-17)

**Scenario:** Wave 88 Bucket B/C agents sẽ sweep planning + code docs cho Vercel refs.

**Apply rule to sample artifacts đang exist:**

| Artifact | Rule verdict | Action |
|---|---|---|
| `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` (mentions Vercel decision context) | §4 exception "Wave 82 specific cutover doc" + "Brainstorm Q1/Q2 decision context" | ✅ KEEP — historical rationale |
| `documents/07-archived/**` Vercel refs | §4 exception "historical archive" | ✅ KEEP — immutable history |
| `documents/04-quality/audits/acceptance-tests/2026-05-16-wave-86-pretag-self-test-results.md` (mentions Vercel) | §4 exception "audit artifact" | ✅ KEEP |
| `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` `verify_via` column `kitehub.vercel.app` (Wave 87 Bucket C patch) | §4 exception "transition reality" — pre-cutover smoke fallback | ✅ KEEP với plan revert post-cutover (already documented Wave 87 Bucket C README) |
| `kitehub/kitehub-frontend/src/lib/api-client.ts` `https://kitehub.vercel.app` hardcoded | NO exception — production code | ❌ SWEEP — replace với `kitehub.me` apex (env-var-driven) |
| New gap file 2026-05-17+ proposing "deploy to Vercel" | NO exception | ❌ BLOCK — rewrite proposing self-host |
| Marketing email URL pointing `kitehub.vercel.app` | NO exception | ❌ SWEEP — `kitehub.me` post-cutover (or neutral pre-cutover) |

**Verdict:** Rule fires correctly:
- Grandfathered scope (historical / audit / transition reality) explicitly exempt
- Forward-looking new mentions BLOCKED unless §4 exception cited
- Production code SWEEP candidate identified

Self-test PASS ✅

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Add Vercel as fallback "just in case" | Self-host EC2 = single canonical hosting; fallback path = different EC2 instance hoặc CF Pages future eval (NOT Vercel) |
| Document Vercel deploy step trong runbook mới | Document EC2 self-host deploy via SSM/CI workflow |
| Cite `kitehub.vercel.app` URL in email/marketing | Use `kitehub.me` apex post-cutover; neutral wording pre-cutover ("Try our beta at https://[apex]") |
| Sweep historical Wave 82 docs để remove Vercel refs | §4 exception preserves history; rule prospective only |
| File gap "Vercel rate-limit blocking deploy" | Self-host eliminates rate-limit; gap N/A post-Wave-88 |
| Code: install new `@vercel/*` SDK | No new Vercel SDK dependencies |
| Block PR Wave 82 closure-history docs that mention Vercel | Rule applies forward-only; historical rationale preserve |

---

## 8. Relationship to other rules

- **`agent-aws-access.md`** — production AWS operations governance; this rule extends to vendor decision discipline
- **`audit-to-gap-pipeline.md`** §2.7 Decision-Doc Code-Sync — Wave 88 decision lands → code sweep mandatory same wave (Bucket C)
- **`output-review-mandate.md`** §3 — adds row "Vendor decision discipline" tracking standard
- **`incident-to-rule-pipeline.md`** — this rule = direct output 2026-05-17 user-flagged decision "cấm nhắc đến vercel nữa" applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + worked self-test + reviewer-checklist ship same Wave 88 PR
- **`release-deploy-standard.md`** — deploy standard applies to self-host; Vercel not in scope
- **Wave 82 plan** `wave-2026-05-15-82-fe-self-host.md` — original pivot decision context
- **Wave 88 plan** `wave-2026-05-17-88-vercel-decommission.md` (paired same PR) — sweep + cutover execution
- **PR #1466** Wave 86 EIP + CF apex cutover workflow — Wave 88 Bucket D dev-trigger

---

## 9. Log

- **2026-05-17 (v1.0.0):** Rule created in response to user-flagged decision 2026-05-17 "thêm rule là cấm nhắc đến vercel nữa" sau Wave 82 self-host pivot (2026-05-15) + Wave 87 closure walkthrough audit (PR #1473) phát hiện production DNS vẫn proxy Vercel. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged) → Classify ✓ (no existing rule codifies Vercel decommission; Wave 82 pivot decision rationale ở wave plan nhưng không enforce prospectively) → Rule+Enforce ✓ (this file + Wave 88 plan paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên Wave 88 sweep scope — rule fires correctly trên 7 sample artifacts, exception ranges hợp lý) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying vendor decommission decision; no constraint loosening for prior work; existing Vercel references grandfathered per §4 cho tới Wave 88 sweep/cutover; rule applies prospectively từ 2026-05-17 forward). CI grep detector + memory auto-load deferred per premature-rule guard ≥7 ngày; v1.0.0 enforcement = path-scoped auto-load + reviewer-checklist + worked self-test sufficient.
