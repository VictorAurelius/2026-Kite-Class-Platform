# Simulation Gap Finder Report — Wave 3 AI Branding Core

**Date:** 2026-04-14
**Method:** `.claude/skills/simulation-gap-finder.md` — 3-axis matrix (5 personas × 8 stages × 10 categories)
**Feature scope:** Wave 3 AI Branding Core Pipeline
**Existing gap baseline:** 68 gaps (after Wave 2 completion + GAP-065/066/067/068 merged)

---

## Matrix Coverage Summary

| Persona \ Stage | 1 Discovery | 2 Signup | 3 Config | 4 Provisioning | 5 Daily | 6 Edge | 7 Evolution | 8 Termination |
|-----------------|:-----------:|:--------:|:--------:|:--------------:|:-------:|:------:|:-----------:|:-------------:|
| Owner | ✓ | ✓ | **⚠️ GAP-069** | ✓ | ✓ | **⚠️ GAP-070** | **⚠️ GAP-071, 072** | **⚠️ GAP-073** |
| End User | n/a | ✓ | n/a | n/a | **⚠️ GAP-074** | ✓ | ✓ | ✓ |
| Platform Admin | n/a | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Developer | ✓ | ✓ | **⚠️ GAP-075** | ✓ | ✓ | ✓ | ✓ | ✓ |
| Support | n/a | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

Legend: ✓ covered by existing gaps / code, ⚠️ NEW gap

---

## New Gaps Discovered (7)

| ID | Title | Matrix cell | Category | Priority | Target wave |
|----|-------|-------------|:--------:|:--------:|:-----------:|
| GAP-069 | Industry-specific branding presets (K-12 / center / univ VN) | Owner × Config | C9 + C2 | 🟠 P1 | **Wave 3** Sub-PR 3.7 |
| GAP-070 | Concurrent rebrand race + approval workflow | Owner × Edge × Provisioning | C3 + C5 | 🟠 P1 | **Wave 3** Sub-PR 3.5 |
| GAP-071 | Branding migration on tier up/downgrade | Owner × Evolution | C10 | 🟡 P2 | Wave 7 |
| GAP-072 | Scheduled rebrand + academic-year refresh | Owner × Evolution | C10 | 🟡 P2 | Wave 8 |
| GAP-073 | GDPR/VN deletion policy for AI assets | Owner × Termination | C6 | 🟠 P1 | Wave 4 Security |
| GAP-074 | AI-generated alt-text for accessibility | End User × Daily | C2 a11y | 🟡 P2 | Wave 5/7 |
| GAP-075 | Developer sandbox tenant environment | Developer × Config | C8 | 🟡 P2 | Wave 9 DX |

---

## Cross-Check Against Existing Gaps (Duplicate Prevention)

For each new gap, verified no overlap với 68 existing gaps:

| New | Closest existing | Overlap? | Decision |
|-----|------------------|:--------:|----------|
| GAP-069 | GAP-011 (template curation) | Partial — templates need segment tags | Separate: GAP-069 is preset/wizard, GAP-011 is library content |
| GAP-070 | GAP-023 (moderation) | No — GAP-023 post-generation, GAP-070 pre-generation approval | Distinct |
| GAP-070 | GAP-035 (team collaboration) | No — GAP-035 is collaborative editing, GAP-070 is serialization | Distinct |
| GAP-071 | GAP-036 (tier upgrade UX) | No — GAP-036 is reveal UX, GAP-071 is migration logic | Distinct |
| GAP-072 | — | None | New |
| GAP-073 | GAP-024 (asset lifecycle) + GAP-042 (legal/IP) | Partial — GAP-024 is ops hygiene, GAP-042 is IP; GAP-073 is user-facing GDPR | Distinct |
| GAP-074 | — | None | New |
| GAP-075 | GAP-038 (dev docs/SDK) | Partial — GAP-038 docs, GAP-075 runtime sandbox | Distinct |

---

## Wave 3 Plan Impact

**Decision: patch, don't rewrite.**

Changes to `03-planning/wave-03-ai-branding-core.md`:
- Add GAP-069 to Sub-PR 3.7 scope (+1 day: 8d → 9d)
- Add GAP-070 to Sub-PR 3.5 scope (+1 day: 10d → 11d)
- Wave total: 44d → **46d** (+4.5%)
- GAP-073 (GDPR) targets Wave 4 Security parallel workstream — must land by GA
- Other 4 gaps slot into later waves (5-9), do not block Wave 3

---

## Methodology Notes

### Axis coverage achieved
- **Personas:** 5/5 walked through
- **Stages:** 8/8 covered per applicable persona
- **Categories:** 10/10 cross-checked (C1-C10 per cell)

### Stress tests applied
- ✅ **Failure:** AI provider down (covered by Wave 3 Sub-PR 3.2 Circuit Breaker)
- ✅ **Concurrency:** 2 admins rebrand same instance simultaneously → NEW GAP-070
- ✅ **Scale:** queue fair scheduling (covered by GAP-005)
- ✅ **Malice:** prompt injection via logo metadata (covered by GAP-041 + rule §9)

### Cells deliberately skipped
- End User × Discovery (marketing) — not a branding core concern
- Platform Admin × Signup — admins don't sign up as tenants
- Developer × Termination — developer not a deletable tenant type

---

## Follow-ups

- [ ] This report committed in PR alongside 7 gap files
- [ ] Re-run simulation after Wave 3 Sub-PR 3.4 (REST API) lands to check Developer × Integration stage more thoroughly
- [ ] Run simulation on Wave 4 Security scope before kickoff
- [ ] Consider expanding `reference/category-checklists.md` for C10 Evolution (currently sparse)

---

## Log

- 2026-04-14 — First simulation run on Wave 3 scope; 7 new gaps, 2 integrated into Wave 3 plan
