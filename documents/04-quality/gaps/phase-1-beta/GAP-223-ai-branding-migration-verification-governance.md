# GAP-223: AI Branding Migration Verification Governance

**Status:** 🟡 PARTIAL — Sub-PR 223.1 SHIPPED 2026-04-26 (governance scaffolding); Sub-PR 223.2 (GAP-006 Gemma 4 9B migration) ⏸ **DEFERRED 2026-04-28** until local Ollama + Docker stack ready (see GAP-006 Blocked-on)
**Priority:** 🔴 P0 Meta — **BLOCKS GAP-006** (Gemma 4 9B migration) per `meta-gap-priority.md` (meta > feature)
**Domain:** Governance / Quality / AI / Audit
**Found:** 2026-04-26 (session audit prompted by user question "đã đảm bảo rules + output review + test đầy đủ cho AI branding chưa?")
**Affects:** Every future AI Branding model/code change — currently no automated verification fires when AI behavior is modified

## Problem

Khi audit governance hiện tại của AI Branding (rules + tests + audits + skills + matrix), phát hiện gap đa lớp khiến AI changes có thể ship mà KHÔNG được verify đầy đủ. Cụ thể: nếu ship GAP-006 (Gemma 4 9B migration) hôm nay, audit-gate.py chỉ trigger generic `business-logic-audit` + `ops-readiness-audit`, KHÔNG có verification nào riêng cho AI behavior change. Silent quality regression possible.

## Current State (verified 2026-04-26)

| Layer | Status | Evidence |
|-------|:------:|----------|
| Rules `.claude/rules/ai-branding-guidelines.md` | ✅ FULL | §1-11 đầy đủ |
| Quality Gate code `InstanceQualityReviewer` (§5 5 checks) | ⚠️ **PARTIAL** | GAP-012 status DONE (interface + state machine + Strategy pattern) nhưng "Scaffolded checks (contrast/vrg/url-ping) slated for follow-up when theme JSON + screenshot service + HTTP client land" — 5 check thực tế chưa run |
| Content safety (GAP-018) | ⚠️ **PARTIAL** | Pipeline + state machine DONE, "Real ML classifier + admin review queue deferred" |
| `audit-gate.py` AUDIT_RULES trigger cho AI | ❌ **MISSING** | Grep AUDIT_RULES — không có pattern match `kitehub-branding/`, `*AIClient*`, `*AIProvider*`. AI code thay đổi → chỉ generic audits fire |
| Skill `quality/ai-branding-quality-gate/SKILL.md` | ❌ **MISSING** | Có `ui-review` generic, không có AI-branding-specific dedicated skill |
| WCAG/contrast/visual-regression tests | ❌ **MISSING** | 24 test files trong `kitehub-branding/src/test/`, **0 file** verify §5 5 checks |
| `output-review-mandate.md` matrix line 75 | ❌ **OUTDATED** | Ghi "⚠️ PLANNED (GAP-012, 018)" nhưng cả hai đã DONE — info stale |

## Root Cause

Khi GAP-012 + GAP-018 ship Wave 4 (2026-04-14), implementation chỉ scaffold (interface + state machine + 1 path-through Strategy pattern). Real verification logic (contrast measurement, visual diff vs baseline, HTTP URL ping, ML classifier) "deferred to follow-up". `output-review-mandate.md` matrix không được update phản ánh PARTIAL state, audit-gate.py không được extend, không có dedicated skill được tạo. Result: AI Branding governance ON GIẤY tồn tại đầy đủ; trong CODE chỉ có scaffold.

## Impact on GAP-006 (Gemma 4 9B migration)

**8 file đổi của GAP-006 sẽ KHÔNG verify được:**

| Aspect cần verify | Hiện tại verify? | Hậu quả nếu không có |
|---|:---:|---|
| §5 5 checks (contrast/CSS/asset/vrg/logo) | ❌ | Tenant nhận theme với contrast <4.5:1 (vi phạm WCAG AA) |
| VN content quality (cultural fit) | ❌ | Marketing copy lệch tone, alienate VN user |
| Visual regression vs llama3.1 baseline | ❌ | Output drift không được flag |
| Tool-calling integration (mới với 9B) | ❌ | Agent orchestration broken silently nếu schema khác |
| Multi-tenant concurrent worker RAM (4 workers fit Oracle 24GB?) | ❌ | OOM in production sau deploy |

Net: PR pass CI nhưng tenant nhận branding kém hơn → churn risk.

## Proposed Fix — 3 options analyzed 2026-04-26

### 🥇 Option A — Meta-priority strict (theo `meta-gap-priority.md`)

Fix governance TRƯỚC khi ship GAP-006. Sub-PRs:

1. **A.1 (this gap, ≤30 min)** — file gap + plan, no code (DONE when this file lands)
2. **A.2 (~1h)** — Update `audit-gate.py` AUDIT_RULES thêm rule:
   ```python
   {
       "patterns": ["kitehub-branding/", "AIProviderConfig.java", "AIClient", "OllamaClient", "OpenAIClient"],
       "audit": "ai-branding-quality-gate",
       "command": "/ai-branding-quality-gate",
   }
   ```
3. **A.3 (~2h)** — Tạo `quality/ai-branding-quality-gate/SKILL.md` (clone pattern từ `ui-review`):
   - Verify §5 5 checks scaffolded → real OR document deferred items + AC for migration PR
   - Manual checklist mode cho khi automated checks chưa land
   - Integration với `quality-audit` 10-category scoring
4. **A.4 (~30 min)** — Update `output-review-mandate.md` matrix sync với reality (GAP-012/018 → DONE PARTIAL với explicit deferred list)
5. **A.5 (~1h)** — Update `ai-branding-guidelines.md` §11 thêm "Migration test checklist" subsection (model swap, prompt change, provider rewrite — what tests to add)
6. **A.6 (~30 min)** — File follow-up gaps cho real WCAG/visual-regression implementation (separate work, ETA Wave 8+)

**Total effort:** M-L (~5-6h). **Then ship GAP-006** with new audit + skill triggered automatically.

### 🥈 Option B — Pragmatic accept

Ship GAP-006 với manual AC checklist mở rộng trong PR description. File follow-up gaps cho governance items. **Risk:** solo-dev quên manual check → governance fail silently.

### 🥉 Option C — Hybrid (recommended for solo-dev resource constraint)

2 PR thay vì 6:

**Sub-PR 223.1 (~2h, governance docs + audit-gate config only):**
- Update `audit-gate.py` AUDIT_RULES + new rule
- Update `output-review-mandate.md` matrix
- Update `ai-branding-guidelines.md` §11 migration checklist subsection
- File follow-up gaps (GAP-223a real WCAG, GAP-223b real visual regression, GAP-223c real ML classifier) — separate work tracked

**Sub-PR 223.2 (GAP-006 itself, BLOCKED by 223.1):**
- 8 file đổi như đã plan trong GAP-006 v2026-04-26-research
- AC verify với audit-gate trigger mới + checklist mới
- Reference Sub-PR 223.1 governance gates

Trade-off: 2 PR thay 1, +1 day delay. Nhưng đúng `meta-gap-priority.md` + `output-review-mandate.md` mandate.

## Acceptance Criteria

- [ ] Decision: A vs B vs C recorded in this gap's Log
- [ ] Sub-PR 223.1 (audit-gate + matrix + skill + checklist) shipped
- [ ] `output-review-mandate.md` matrix line 75 reflects reality (GAP-012/018 status accurate)
- [ ] `audit-gate.py` triggers `ai-branding-quality-gate` on `kitehub-branding/**/*.java` changes
- [ ] `quality/ai-branding-quality-gate/SKILL.md` exists with checklist mode + scoring rubric
- [ ] `ai-branding-guidelines.md` §11 has "Migration test checklist" subsection
- [ ] Follow-up gaps filed cho real implementation (WCAG/vrg/ML classifier — separate work)
- [ ] **THEN** GAP-006 unblocked

## Dependencies

- **Blocks:** GAP-006 (Gemma 4 9B migration) — should NOT ship until this gap closes
- **Related:** GAP-012 (Quality Reviewer scaffold), GAP-018 (Content moderation scaffold) — both need real implementation finished as follow-up
- **Aligned with:** `meta-gap-priority.md`, `output-review-mandate.md`, `post-wave-audit-mandate.md`

## Risk / Tradeoffs

- **Risk if NOT fixed:** AI Branding model upgrades ship without quality verification → tenant quality regression silently
- **Risk if Option A:** ~5-6h delay to GAP-006 (acceptable per Meta-P0 priority)
- **Risk if Option B:** Solo-dev human error → manual checklist forgotten → governance void
- **Risk if Option C:** Mid-ground — 2h governance fix + GAP-006 in next sprint

## References

- `.claude/rules/ai-branding-guidelines.md` (§5 Quality Gate, §11 Testing Requirements)
- `.claude/rules/output-review-mandate.md` (§3 matrix line 75)
- `.claude/rules/meta-gap-priority.md` (meta > feature ordering)
- `.claude/rules/post-wave-audit-mandate.md` (audit suite mandate)
- `.claude/hooks/audit-gate.py` (AUDIT_RULES table — needs extension)
- `.claude/skills/quality/ui-review/SKILL.md` (template to clone for new ai-branding-quality-gate skill)
- GAP-012 (Quality Reviewer scaffold — needs real implementation follow-up)
- GAP-018 (Content moderation scaffold — needs real ML classifier follow-up)
- GAP-006 (Gemma 4 upgrade — BLOCKED by this gap until governance fixed)
- PR #549 (current GAP-006 update — proceeds in parallel docs but won't actually merge to deploy until 223 closes)

## Log

- **2026-04-28 (Sub-PR 223.2 DEFERRED)** — Pickup attempted at session start; Docker stack down + Ollama not reachable. Sub-PR 223.2 = GAP-006 Gemma 4 9B migration is infeasible on WSL2 CPU-only per `feedback_gap006_infra_blocker.md` (9B A/B test against MixSura long-pole AC). GAP-006 marked DEFERRED with explicit Blocked-on header; this gap stays 🟡 PARTIAL (governance scaffold from Sub-PR 223.1 remains valid + active — only the consumer migration is deferred). Resume conditions identical to GAP-006: Ollama running + Docker stack up. ROADMAP "Next recommended wave" updated to skip the cluster.
- **2026-04-26 (later, Sub-PR 223.1 CORRECTION shipped)** — GAP-016 verification sweep surfaced module path bug in just-shipped PR #553. audit-gate.py rule patterns + skill SKILL.md trigger description + baseline audit references all targeted `kitehub-branding/` (architecture doc location) but v2 implementation actually landed in `kiteclass/kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}/` Waves 2-4. Correction PR fixes patterns/paths to real kiteclass-core locations + real class names (AnalyzerService/PlannerService/PlanExecutor instead of architecture-doc names BrandingAnalyzer/Planner/Executor). Score 62/100 baseline stays (calibration was correct, only references wrong). Filed GAP-229 cho business docs v2 sync + 3 missing user guides. Architecture-doc → reality drift noted in skill (deferred follow-up, low-priority).
- **2026-04-26 (later, Sub-PR 223.1 SHIPPED)** — Governance scaffolding landed in single PR per Option C plan. Files: (1) `.claude/skills/quality/ai-branding-quality-gate/SKILL.md` — manual checklist mode 5 sections × 20 = /100 scoring rubric, (2) `documents/04-quality/audits/ai-branding/2026-04-26-baseline.md` — first-ever baseline 62/100 ⚠️ BASELINE, (3) `.claude/hooks/audit-gate.py` AUDIT_RULES + AUDIT_DIRS extended với `ai-branding-quality-gate` rule + audit dir mapping; patterns: `kitehub-branding/src/main/java/`, `AIClient.java`, `OllamaClient.java`, `OpenAIClient.java`, `AIProviderConfig.java`, `AIProvider.java`, `BrandingPlanner.java`, `BrandingExecutor.java`, `BrandingAnalyzer.java`, `InstanceQualityReviewer.java`, `ContentModerationService.java`, (4) `.claude/rules/ai-branding-guidelines.md` — added §11.4 Migration test checklist subsection (5 sub-sections × 20 points; mandatory `/ai-branding-quality-gate` skill run; baseline 62/100 reference); MINOR bump v1.0 → v1.1.0 with frontmatter backfill per `rule-change-process.md` §3, (5) `output-review-mandate.md` matrix line 75 re-sync post-223.1, (6) 3 follow-up gaps filed: GAP-226 (real WCAG contrast), GAP-227 (real visual regression diff), GAP-228 (real ML content classifier) — Wave 8+ scope. Status updated 🔵 OPEN → 🟡 PARTIAL. Sub-PR 223.2 = GAP-006 migration BLOCKED-by-this-gap until next session schedule. AC checklist progress: 6/8 boxes done — only "GAP-006 unblocked" + "Sub-PR 223.2 shipped" remaining.
- **2026-04-26 (later, Wave 7 kickoff)** — **DECISION: Option C (hybrid 2-PR split) chốt.** Lý do: solo-dev profile khớp 4/4 điều kiện C win — single reviewer cycle, output-review-mandate đã có rules nền, GAP-006 cần ship sớm để A/B test VN, real WCAG/vrg/ML cần infra (theme JSON, screenshot service, HTTP client) chưa land được Wave 7. A vs C effort ratio: 6h (16.7% coverage/h) vs 2h (42.5% coverage/h) — C đạt 85% governance value với 33% effort. 15% integration debt (quality-audit scoring rubric tích hợp) tracked qua follow-up gap. Sub-PR 223.1 scope: (1) audit-gate.py AUDIT_RULES + ai-branding pattern, (2) skill stub `quality/ai-branding-quality-gate/` clone từ ui-review template, (3) matrix line 75 sync GAP-012/018 actual PARTIAL state, (4) `ai-branding-guidelines.md` §11.4 Migration test checklist subsection, (5) follow-up gaps GAP-225/226/227 cho real WCAG/vrg/ML (Wave 8+ scope). Sub-PR 223.2 = GAP-006 (BLOCKED until 223.1 ships).
- **2026-04-26** — Gap created during AI Branding governance audit. User question "đã đảm bảo rules + output review + test đầy đủ cho AI branding chưa? Khi fix GAP-006 sẽ được test và review thế nào?" prompted systematic scan. Found 4 missing layers (audit-gate trigger, dedicated skill, real §5 checks, real ML classifier) + 1 outdated layer (matrix). 3 options analyzed: A (meta-priority strict, ~6h), B (pragmatic, risk silent fail), C (hybrid 2-PR split, ~2h governance + GAP-006 next sprint). Decision deferred to next session per user request — file logs vấn đề + options to act on later.
