# GAP-416: Ollama Defer Phase 2 ADR (AI Branding Template-Only Phase 1)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Architecture / AI / Strategy
**Found:** 2026-05-07 (Wave 37 — user-confirmed defer Ollama scope)
**Affects:** Phase 1 BETA scope reduction + GAP-006 cluster reschedule

## Problem

Original Phase 1 BETA included Ollama AI inference (GAP-006 Gemma 4 9B). Analysis 2026-05-07 confirmed:
- Local Ollama hybrid (cloud → tunnel → home) cover ~70% — KHÔNG đủ production-ready
- Cloud GPU EC2 g4dn.xlarge $379/mo — vượt Architecture B budget
- OpenAI cloud API $30-60/mo — alternative nhưng cost recurring

User decision (2026-05-07): **Defer Ollama Phase 2 — Phase 1 BETA = template-only**.

## Proposed Fix

NEW ADR `documents/02-architecture/adr/ADR-026-ollama-defer-phase-2.md`:

**Decision:** AI Branding Phase 1 BETA = TEMPLATE-only (per `ai-branding-guidelines.md` §1.1 STATIC/TEMPLATE/FULL_AI taxonomy default route ~80% requests). Defer FULL_AI route Phase 2.

**Rationale:**
- Free Tier $0 cost (no Ollama compute, no OpenAI API)
- 6 templates pre-designed đã ship Wave 30/31 (sufficient for invite-only beta)
- Marketing rebrand: "AI Branding" → "Smart Brand Templates" + "AI generation Phase 2"
- Compliance: PDPL data localization simpler (no AI provider data flow)

**Impact on existing gaps:**
- GAP-006 Gemma 4 9B migration → defer Phase 2
- GAP-225 cluster (scaffold-as-DONE umbrella) → tracked but không block Release 1
- GAP-228 ML classifier scoring → Phase 2

**Phase 2 trigger gate:** ≥30 paying tenants + revenue covers $379/mo GPU OR $60/mo OpenAI cloud API.

**Marketing copy update:**
- README.md: "AI Branding" → "Smart Brand Templates" + "AI generation coming Phase 2"
- Landing pages, beta invite emails

## Acceptance Criteria

- [ ] ADR-026 file created với 5 sections (Status / Context / Decision / Consequences / Alternatives)
- [ ] GAP-006 status flipped: 🔵 OPEN → 🟡 PARTIAL (deferred Phase 2) hoặc explicit DEFERRED tag
- [ ] GAP-225 cluster updated (Phase 2-4 future scope explicit)
- [ ] Marketing copy review queued (Wave 38+ candidate)
- [ ] `release-deploy-standard.md` checklist Phase 1 BETA — bỏ AI inference items

## Related

- ADR-025 AWS Singapore (parallel Phase 1 BETA decision)
- `ai-branding-guidelines.md` §1.1 STATIC/TEMPLATE/FULL_AI
- GAP-006 / GAP-225 / GAP-228 (deferred consequences)
- `feedback_release_1_first_session_priority.md` MVP-first philosophy
