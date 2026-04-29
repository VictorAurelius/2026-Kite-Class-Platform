---
title: Wave Legal-BRD Phase 1.5 — 3 sister policy docs (Refund / Billing-VAT / Child Protection) + meta codify skeleton-agent template
status: active
created: 2026-04-29
updated: 2026-04-29
gaps: [GAP-183, GAP-185, GAP-186]
deferred_to_next_wave: []
deferred_separate_track: []
umbrella: GAP-154
sister_wave: wave-2026-04-29-legal-brd-phase1
---

# Wave Legal-BRD Phase 1.5 — Cluster Pack 14 (10th wave-pack, 2nd legal-BRD slice)

**Wave date:** 2026-04-29 (kicked off this session, ~1h after Wave 13 closure)
**Cluster theme:** Closes 7/7 P0 BL legal mandate skeletons trong GAP-154 umbrella. Sister wave của `wave-2026-04-29-legal-brd-phase1.md` (4-doc TOS/AUP/Privacy/Retention shipped earlier today). Plus 1 meta-track agent codifies recurring `docs-only-skeleton-agent.md` template variant.
**Strategy reference:** Re-use proven pattern (Wave 13 wall-clock 35 min vs 85 estimate). 4-agent max-cap per `feedback_parallel_agent_strategy.md` rule #9 — 3 BRD docs + 1 meta to fill cap.

## Why this wave

- 3 OPEN P0 BL legal mandate gaps siblings của 4 vừa ship (GAP-180/181/182/184)
- Phase 1 = skeleton-only pattern proven, deterministic ~5 min/agent
- 4th agent meta-track codifies template variant (2nd recurrence threshold = early codify để avoid 3rd-time re-derivation)
- After this wave: 7/7 BRD legal skeletons DONE → GAP-154 umbrella có thể flip 🟡 → 🟢 sau Phase 2 legal counsel content
- VN Consumer Protection Law 2023 (GAP-183), TCT e-invoice mandate (GAP-185), VN Law on Children 2016 + PDPL Art 16 (GAP-186) — all P0 legal mandates

## Scope

| # | Gap / Track | Title | Priority | Agent | Disjoint files |
|:-:|-----|-------|:--------:|:-----:|----------------|
| 1 | **GAP-183** | Refund + Dispute Resolution Policy (8 sections + eligibility matrix) | 🔴 P0 BL VN Consumer Protection | A | `documents/00-brd/refund-dispute-resolution-policy.md` (NEW) |
| 2 | **GAP-185** | Billing Terms + VAT/TCT Compliance (9 sections + payment matrix + tax calc) | 🔴 P0 BL VN Tax + TCT e-invoice | B | `documents/00-brd/billing-terms.md` (NEW) |
| 3 | **GAP-186** | Child Protection Policy K-12 (8 sections + safeguarding) | 🔴 P0 BL Law on Children 2016 + PDPL Art 16 | C | `documents/00-brd/child-protection-policy.md` (NEW) |
| 4 | **META** | Codify `docs-only-skeleton-agent.md` template variant (2nd recurrence) | 🟠 Meta-P1 force multiplier | D | `.claude/skills/quality/wave-pack-planner/assets/agents/docs-only-skeleton-agent.md` (NEW) + `.claude/skills/quality/wave-pack-planner/reference/retrospective-checklist.md` (modify) |

## File overlap analysis

```bash
bash .claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-183 GAP-185 GAP-186
```

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/00-brd/refund-dispute-resolution-policy.md` (NEW) | A only | None |
| `documents/00-brd/billing-terms.md` (NEW) | B only | None |
| `documents/00-brd/child-protection-policy.md` (NEW) | C only | None |
| `.claude/skills/quality/wave-pack-planner/assets/agents/docs-only-skeleton-agent.md` (NEW) | D only | None |
| `.claude/skills/quality/wave-pack-planner/reference/retrospective-checklist.md` | D only | None |
| `documents/00-brd/README.md` | foundation only | None — coordinator owns |
| `.claude/rules/meta-gap-priority.md` | A,B,C (read-only) | **SOFT** — read-only |
| `.claude/rules/business-logic-review.md` | A,B,C (read-only) | **SOFT** — read-only |

Net: **0 HARD, 2 SOFT (read-only citations only)**. 3 BRD agents fully disjoint from meta agent (different folder trees).

**Mitigation:** foundation PR ships `00-brd/README.md` directory map updates centrally → BRD agents KHÔNG touch README.

## Agent workflow

Per `feedback_parallel_agent_strategy.md` + Wave 13 lessons:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off main (after this foundation PR merges)
3. Agent verify cwd: `pwd | grep -q "\.claude/worktrees/" || abort` — applies to ALL 4
4. Commits + creates own PR — branch naming: `feat/wave-legal-brd-1-5-{topic}-skeleton`
5. Coordinator merges sequentially: A → B → C → D
6. **NEW for 4+-agent waves (Wave 13 lesson):** prune worktrees BEFORE final merge of last PR OR accept local stale state; recovery via `git fetch && git reset --hard origin/main`
7. **NEW (Wave 13 contamination lesson):** coordinator verify `pwd` before branch ops — closure branch creation can land inside worktree if cd happened invisibly
8. Status flip 🔵 OPEN → 🟡 PARTIAL on closure PR for 3 BRD gaps (Phase 2 deferred to GAP-154)
9. Agent D meta-track: NO gap to flip, just SHIPPED in closure PR ROADMAP entry

## Acceptance criteria (wave-level)

- [ ] 4 PRs merged (3 BRD + 1 meta) with green CI
- [ ] 3 BRD gaps transitioned 🔵 OPEN → 🟡 PARTIAL với Log entry citing Phase 1 done + Phase 2 blocked-on legal counsel + GAP-154 umbrella
- [ ] Meta deliverable: `docs-only-skeleton-agent.md` template variant exists + `retrospective-checklist.md` extended với 4+-agent local-state hazard pattern
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry
- [ ] `documents/00-brd/README.md` directory map shows 3 new rows với status `skeleton`
- [ ] No conflicts left unresolved on main
- [ ] Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `data/wave-history.jsonl` entry appended với wall-clock + lessons
- [ ] **Milestone:** 7/7 BRD legal mandate skeletons DONE (Phase 1 of GAP-154 umbrella complete)

## Wall-clock target

Per Wave 13 calibration: docs-only-skeleton ~5 min/agent + sequential merge ~3 min/PR + closure ~10 min.

- Foundation PR (this doc + ROADMAP entry + README directory map): ~12 min
- 4 parallel agents: ~6-8 min wall (each ~5-7 min agent-time, parallel; meta agent slightly longer)
- Sequential merge (no conflicts expected): ~12 min
- Closure (ROADMAP + cleanup + retrospective): ~10 min
- **Total wave: ~40-45 min** (vs Wave 13 actual 35 min — slight upward adjustment for meta agent)

## Per-agent skeleton requirements

### Agent A — GAP-183 Refund + Dispute Resolution Policy

Sections required (per gap §Scope):
1. Refund Eligibility Matrix (Free/Pro/Premium/Enterprise × Trial/Mid-cycle/Feature failure/Goodwill)
2. Refund Process (request channel, info required, SLA 5 business days per VN law, refund timing 7-14 days)
3. Non-Refundable Items (used services, AI generation delivered, custom branding approved)
4. Service Credits (alternative to refund — eligibility, calculation linked GAP-189 SLA, validity)
5. Dispute Resolution Process (informal L1→L2→Lead, formal 30-day, mediation, VIAC arbitration, court TAND)
6. Consumer vs Commercial Customers (Consumer Law stronger protections vs contract-based)
7. Chargeback Handling (response procedure, evidence collection)
8. Force Majeure (service interruptions categorization)

Required tables: Refund eligibility matrix (tier × scenario), Dispute escalation ladder.

Frontmatter: `Trạng thái: 🔵 SKELETON`, Owner Legal+PM, Reviewer Legal counsel + Finance + Support Lead (Phase 2), Tracking GAP-183→GAP-154, Legal basis cite Law No. 19/2023/QH15 + Commercial Law 2005.

### Agent B — GAP-185 Billing Terms + VAT/TCT Compliance

Sections required (9):
1. Payment Terms (billing cycle, due date, currency, methods per tier, auto-renewal)
2. Tax Treatment (VAT 10% std + 5% education exemption analysis, e-invoice TT 78/2021, foreign tenant export-of-services, tax incentives)
3. Invoicing (e-invoice XML schema TCT, digital signature CA, issuance timing, MST tenant tax info, credit note process)
4. Late Payment (grace period, late fee linked GAP-108 externalization, suspension trigger, reactivation, write-off)
5. Pricing Changes (notice 30-60d, grandfathering, mid-term prohibition)
6. Promotions + Discounts (types, referral credits, education discounts, terms)
7. Refunds → link GAP-183
8. Enterprise Payment (PO process, annual prepay discount, multi-year, net-30/60)
9. Currency Conversion (international tenants)

Required tables: Payment method matrix (tier × methods × fees), Tax calculation examples (VAT-inclusive vs exclusive), Late fee examples linked GAP-108 config keys.

Frontmatter: Owner Legal+Finance, Reviewer + Tax advisor + TCT e-invoice provider, Tracking GAP-185→GAP-154→GAP-108, Legal basis Circular 78/2021/TT-BTC + Decree 123/2020/NĐ-CP + VAT Law 2008.

### Agent C — GAP-186 Child Protection Policy

Sections required (8):
1. Scope (minor definition <16 VN per PDPL/Civil Code Art 21; persona triggers — Student in P5, Student <16 in P3, Parent in P5)
2. Parental Consent (when required, format written/digital sig/verifiable, scope, withdrawal, age verification)
3. Minor Data Protection (stricter retention link GAP-184 sensitive-minor 6mo max, no marketing/profiling/3rd-party-sharing beyond educational, enhanced encryption)
4. Safeguarding Rules (teacher-student platform-mediated only, 1-to-1 calls recording option + parent visibility, suspicious behavior reporting, mandatory reporting grooming/abuse)
5. Platform Safety Features (content filtering stricter for minors, time-of-day restrictions, screen time reporting to parents, emergency contact)
6. Staff Safeguarding (teacher vetting at tenant level, background check docs, code of conduct)
7. Incident Response (24/7 hotline, escalation police + MOLISA, evidence preservation, parent notification)
8. Training + Awareness (annual safeguarding training, age-appropriate student materials, parent resources)

Required tables/diagrams: Age verification + consent flow description, Minor data handling matrix (data category × standard rules × minor-specific rules), Safeguarding incident classification (severity × response).

Frontmatter: Owner Legal + Trust&Safety + DPO, Reviewer + MOLISA consultation, Tracking GAP-186→GAP-154, Legal basis cite **Law on Children 2016** + Decree 56/2017/NĐ-CP + PDPL 2023 Art 16 + Decree 13/2023 + MOLISA circulars; International ref COPPA + UK Children's Code + SG PDPA.

### Agent D — META: Codify `docs-only-skeleton-agent.md` template + extend retrospective-checklist

**Deliverable 1:** New file `.claude/skills/quality/wave-pack-planner/assets/agents/docs-only-skeleton-agent.md` — variant của `docs-only-agent.md` specialized for skeleton-only Phase-1 cluster pattern.

Differences từ base template:
- Explicit "Phase 1 skeleton scope" framing (sections + structure + TODO markers, NOT content)
- 5-attribute pattern reference (`business-logic-review.md` §2 informed-gut Source category)
- Phase 2 TODO marker template inline: `<!-- Phase 2: <topic> — informed gut <date>, <umbrella-gap> -->`
- Cross-link verification: relative paths cho sibling skeletons, "(planned — see GAP-XXX)" cho deferred
- Frontmatter style explicit: markdown-header (mimic `personas-catalog.md`) NOT YAML — adjust if folder convention differs
- 5-min wall-clock budget per agent (Wave 13 calibration)
- "DO NOT touch README" constraint default-on (foundation PR owns)
- Worktree verify boilerplate inline (Wave 13 Agent C contamination lesson)

Cite worked examples: Wave 13 (4 BRD skeletons) + Wave 14 (this wave's 3 BRD skeletons).

**Deliverable 2:** Modify `.claude/skills/quality/wave-pack-planner/reference/retrospective-checklist.md` — add new section §"4+-agent local-state hazards":
- Worktrees holding refs of merged branches → `gh pr merge --delete-branch` post-checkout fails
- Coordinator-side cd contamination: branch ops can land inside worktree
- Recovery: `git fetch && git reset --hard origin/main` (BUT warn: nukes uncommitted dirty files — stash first)
- Mitigation: prune worktrees BEFORE final merge OR accept local stale state until cleanup task

Cite Wave 13 incident as worked example.

**Deliverable 3:** Bump `Last-Reviewed` date on `retrospective-checklist.md` if frontmatter exists; otherwise add modification timestamp inline.

## Per-agent constraints (enforced)

All 4 agents MUST:
- Path constraint: only their assigned file(s) — KHÔNG touch README, KHÔNG touch sibling files
- Frontmatter standard: copy from existing `documents/00-brd/personas-catalog.md` style (markdown-header) for BRD; for meta agent follow skill folder conventions
- Cross-link verify: every link resolves; use relative paths
- Frontmatter required fields: Trạng thái, Owner, Reviewer, Last-Updated, Tracking, Legal basis (where applicable)
- Vietnamese prose default; English cho legal/technical terms
- KHÔNG flip GAP-XXX Status — coordinator handles per `gap-done-discipline.md`
- Worktree verify: `pwd | grep -q "\.claude/worktrees/"` AND `git branch --show-current | grep -q "^feat/wave-legal-brd-1-5"` trước Write/Edit
- RELATIVE paths only trong commands

## Lessons-learned (filled AFTER wave merges)

(Filled per `reference/retrospective-checklist.md`)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold for all 4 (BRD + meta)?

### File overlap accuracy
- [ ] Did predicted overlap (0 HARD, 2 SOFT) match actual?

### Wall-clock variance
- [ ] Estimate ~40-45 min vs actual?

### Pattern reuse
- [ ] Sister-wave reuse pattern (same-day cluster sibling) successful?
- [ ] Meta-codify in same wave as 2nd recurrence — early codify pay off?

### Agent template effectiveness
- [ ] `docs-only-agent.md` held for Wave 14 BRD agents?
- [ ] Meta agent template (general-purpose for skill scaffold) effective?

### 4+-agent hazards (Wave 13 lessons applied)
- [ ] Did coordinator prune worktrees before final merge?
- [ ] Any coordinator cd contamination?
- [ ] Local main glitch recurrence?
