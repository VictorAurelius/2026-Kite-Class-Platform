# Plan: UI/UX Design System Integration

**Created:** 2026-04-18
**Trigger:** Post Wave 4 review — current ui-review skill scores subjectively (Nielsen + WCAG), missing industry-benchmark design-system reasoning.
**Goal:** Adopt best patterns from [nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) (community-vetted, multi-star) to make UI audit objective + improve tenant-facing UI quality.

---

## Context

Session 2026-04-17 đến 2026-04-18 shipped 37 PRs, 4 waves. UI work đã có:
- Wave 4 branding propagation (email + error pages + auth flows)
- Existing ui-review skill: /128 per-screen (Technical /20 + Heuristics /40 + Visual /28 + Friendliness /20 + WCAG /20)

**Problem:** ui-review scoring là **output validator**, không phải **design advisor**. Reviewer nói "Visual Aesthetics: 18/28" không có framework lý giải **tại sao** hoặc **nên làm gì** để nâng điểm.

**Reference repo strengths (ui-ux-pro-max):**
- 161 industry-specific reasoning rules (education, healthcare, finance...)
- 67 UI styles catalog (Glassmorphism, Brutalism, Bento, AI-Native...)
- 161 color palettes mapped 1:1 với product types
- 57 font pairings + Google Fonts imports
- 99 UX guidelines + anti-patterns (e.g., "banking → avoid AI purple/pink")
- Pre-delivery checklist (7 items: emoji icons, cursor-pointer, hover states, contrast, focus, reduced-motion, responsive breakpoints)

---

## Approach: Hybrid (install external + internal upgrades)

### Why hybrid

- **Không rebuild 161 rules** — uipro-cli distribution đủ tốt, cộng đồng vetted
- **Không 100% external** — project cần Vietnamese education context (MOET colors, VN typography, K-12 patterns)
- **Internal upgrade** của ui-review để reference external skill khi scoring

---

## 3-PR Execution

### PR 1 — Install + integrate (1-2 giờ)

**Branch:** `feat/design-system-advisor-integration`

**Deliverables:**

1. Install uipro-cli globally:
   ```bash
   npm install -g uipro-cli
   uipro init --ai claude --global  # → ~/.claude/skills/ui-ux-pro-max/
   ```
   (Document this in new playbook `documents/05-guides/ui-design-system-setup.md`)

2. New skill `.claude/skills/quality/design-system-advisor.md` (≤50 lines):
   - Description: when to invoke uipro vs ui-review
   - Example: "design landing page for school" → uipro generates design system → ui-review audits output
   - Reference: external skill path + CLI commands

3. Upgrade `.claude/skills/quality/ui-review/SKILL.md`:
   - Add 6th scoring dimension: **Design System Fit /20**
   - New total: /148 (thay vì /128)
   - Sub-scores:
     - Industry match (5): palette/style align với education context
     - Anti-pattern absence (5): không có AI purple on K-12 app, no neon on wellness
     - Typography pairing (4): heading+body combination in curated list
     - Pre-delivery checklist (6): 7-item list from ui-ux-pro-max
   - Reference uipro rules khi scoring

4. Update `.claude/skills/_README-skills-index.md` — add design-system-advisor

**Testing:** Run ui-review on sample screen, verify new dimension scores output.

---

### PR 2 — Re-audit với framework mới (half-day)

**Branch:** `docs/audit/ui-review-post-wave4-objective`

**Deliverables:**

1. Capture screenshots (both KiteHub + KiteClass):
   ```bash
   ./scripts/capture-ui-all.sh --label post-wave4-framework
   ```

2. Run /ui-review với new framework /148 — scope:
   - KiteClass 30 pages (public, auth, dashboard)
   - KiteHub 19 pages (public, auth, customer, admin)

3. Output: `documents/04-quality/audits/ui/ui-review-2026-04-18-framework-upgrade.md`
   - Per-screen /148 score
   - Gap list prioritized (P0/P1/P2 with specific fix recommendations from uipro catalog)
   - Before/after comparison table with prior `ui-review-latest.md`

4. Auto-generate gaps:
   - Any screen < 100/148 → P1 gap
   - Any screen < 80/148 → P0 gap
   - Files: `GAP-101+` onwards

---

### PR 3+ — Waves to execute top gaps (ongoing)

Sau PR 2 có concrete gap list. Typical priorities:

**Expected top gaps based on current state:**

| Priority | Gap type | Example |
|:-:|---|---|
| P0 | Auth flow polish | Typography pairing, CTA button hierarchy |
| P1 | Dashboard data-density | Bento grid pattern for stat cards |
| P1 | Error pages | Wave 4 shipped basic — uipro suggests illustration + recovery flow |
| P2 | Landing marketing | Hero-centric + social proof pattern |
| P2 | Wizard UX | Progressive disclosure, step indicator style |

Each gap → wave của 2-3 tuần theo roadmap.

**Integration with existing roadmap:**

- Wave 5 (Parent Dashboard — đang next): interleave P0 dashboard gaps
- Wave 6 (AI Billing observability): interleave admin cost explorer gaps
- Wave 7 (Moderation admin): fresh UI opportunity cho admin table style
- Dedicated "Wave UI-Uplift" (optional) để batch P1/P2 gaps across screens

---

## Success Criteria

- [ ] PR 1 merged — uipro skill available globally, ui-review upgraded to /148
- [ ] PR 2 merged — audit report with /148 scores + concrete gap list
- [ ] ≥ 10 UI gaps created with objective reasoning
- [ ] Each new gap references ui-ux-pro-max rule ID (traceable)
- [ ] Average UI score improves ≥ 10 points after interleaved wave execution

---

## Risks

| Risk | Mitigation |
|------|-----------|
| uipro distribution changes break integration | Install pinned version, document in playbook |
| 161 rules overwhelm — reviewers cherry-pick | Start with top 10 rules per page type, expand later |
| Subjectivity creeps back | Always cite uipro rule ID in gap file, never "I think X looks bad" |
| Python 3.x dependency | WSL env already has it (verified 2026-04-18) |

---

## Related

- Analysis: See Wave 4 retrospective in conversation on 2026-04-18
- External skill: [github.com/nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)
- Current ui-review: `.claude/skills/quality/ui-review/SKILL.md`

---

## Log

- **2026-04-18:** Plan created after external skill analysis. PR 1 to start next.
