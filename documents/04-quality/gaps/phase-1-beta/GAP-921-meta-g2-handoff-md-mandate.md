# GAP-921: Meta — Mandate G2 handoff MD recipe khi G1 PASS (Flow Verification Campaign)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META P1 force-multiplier)
**Domain:** Meta (governance)
**Found:** 2026-06-04 (Wave flow-kh1 G2 handoff session)
**Affects:**
- `.claude/rules/` — new rule file
- `documents/03-planning/roadmap/flow-verification-campaign.md` §2 loop protocol bước 7
- Possibly `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch extension

## Problem

Wave flow-kh1 G2 handoff session 2026-06-04: user explicit asked "Hướng dẫn G2 luôn, bằng file md các bước rõ ràng" + "có meta chưa". State-check existing rules:

- `feature-ship-runtime-walk-mandate.md` v1.1.0 — covers G1 agent walk + catalog-then-batch + walk evidence requirement, NHƯNG KHÔNG mandate G2 handoff format
- `pre-handoff-self-test-completeness.md` v1.2.0 — covers self-test BEFORE handoff (per-flow checklist §2.1-2.11) for agent G1 evidence, NHƯNG handoff content for human G2 không explicit
- `flow-verification-campaign.md` §2 step 7 "Hand cho human (G2)" — KHÔNG specify format hoặc level of detail
- `output-review-mandate.md` §3 — không có matrix row cho G2 handoff format

**Coverage gap:** Khi G1 PASS chuyển sang G2 (human test), KHÔNG có rule mandate:
1. G1-passer (Claude) PHẢI tạo dedicated MD file với stepped instructions cho user
2. MD file PHẢI có specific sections (setup, bước-by-bước, expected, sad path, verify DB, troubleshooting, báo kết quả)
3. MD file location convention (`documents/05-guides/operations/YYYY-MM-DD-g2-recipe-<flow>.md`)

Without rule → G2 handoff drift: có khi inline ngắn trong chat, có khi dài trong wave plan section, có khi không có gì cả → user phải tự decode → friction + risk skip G2.

User-flagged session 2026-06-04: I gave G2 recipe inline trong chat lần đầu (cho KH-2 wave), KHÔNG MD file → user push back asking for MD. Lặp lại cho KH-1 wave → user surface meta question.

## Proposed Fix

### New rule: `.claude/rules/g2-handoff-md-mandate.md`

Scope: Mọi G1 PASS → flow status transition `🔄 walk-pass-pending-human` trong campaign §4.

Mandate sections:
1. **Trigger:** G1 PASS flip (campaign §4 row update) → MUST tạo G2 MD file cùng PR
2. **Required content:**
   - Frontmatter (`audience: dev`, `created`, `scope`, `references`)
   - Setup section (prerequisites, browser, stack state)
   - Stepped instructions (numbered, mỗi step có Hành động + Kỳ vọng PASS + Sad path + Verify DB optional)
   - Troubleshooting quick table
   - Báo kết quả section (4 outcomes: full PASS / mostly PASS / blocking / unclear)
   - Production parity preview cho G3
3. **Filename convention:** `documents/05-guides/operations/YYYY-MM-DD-g2-recipe-<flow-id>.md` (Tier 2 time-bound per `docs-filename-prefix-convention`)
4. **Language:** Vietnamese narrative + English technical identifiers per `dev-readable-doc-language`
5. **Override mechanism:** trailer `G2_HANDOFF_INLINE_OVERRIDE: <reason>` nếu MD overkill cho flow trivial scope

### Sister enforcement
- `feature-ship-runtime-walk-mandate` §3.4 extension — add §3.5 "G2 Handoff MD Mandate when G1 PASS for campaign flow"
- `output-review-mandate.md` §3 — new matrix row "G2 handoff recipe MD"
- Reviewer-checklist Wave plan PR closure: verify G2 MD file present nếu G1 PASS flip

### Worked self-test

Apply retroactively to Wave flow-kh2 G1 PASS:
- Initial handoff (inline chat) → user push back "Hướng dẫn G2 luôn, bằng file md các bước rõ ràng" → MUST create MD
- → Counterfactual với rule: MD file shipped same PR as G1 PASS flip → 0 user round-trip

Apply Wave flow-kh1:
- This gap session 2026-06-04 — MD `2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md` shipped same PR như evidence rule fires correctly

## Acceptance Criteria

- [ ] Rule file `.claude/rules/g2-handoff-md-mandate.md` v1.0.0 shipped với §6.5 Enforcement Parity Mandate (rule + reviewer-checklist + worked self-test + output-review-mandate §3 matrix row + rules-index.csv row paired same PR)
- [ ] `feature-ship-runtime-walk-mandate.md` extended với §3.5 cross-reference
- [ ] `flow-verification-campaign.md` §2 step 7 updated với MD mandate
- [ ] Memory entry `feedback_g2_handoff_md_mandate.md` paired same PR
- [ ] Future G1 PASS waves auto-comply (KH-3 next loop verify)

## Related

- Discovered in: Wave flow-kh1 G2 handoff session 2026-06-04 (user-flagged "có meta chưa")
- Sister rules: `feature-ship-runtime-walk-mandate.md` + `pre-handoff-self-test-completeness.md` + `flow-verification-campaign.md`
- Concrete evidence: `documents/05-guides/operations/2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md` (first MD shipped per proposed rule)
- META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi G1 PASS flow subsequent auto-comply (eliminate user push-back round-trip class)
