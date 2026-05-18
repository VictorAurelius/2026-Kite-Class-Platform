# GAP-653: Thesis defense prep — slide deck + Q&A prep + examiner archetype anticipation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Demo + Content)
**Phase:** phase-1-beta
**Found:** 2026-05-18
**Related Audits:** [thesis-defense-failure-mode-matrix](../../audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md)

## Current State (verified 2026-05-18)

| Piece | Status |
|---|---|
| Defense slide deck | ❌ missing |
| 15-min demo script (full) | ❌ partial — Persona audit có draft, không consolidated |
| Q&A prep doc | ❌ missing — Failure-mode 20 questions chưa có response sheet |
| Examiner archetype playbook | ❌ missing |
| Backup demo recording | ❌ missing |
| Defense practice runs | ❌ none planned |

## Problem

Defense window 2026-08-15 → 2026-10-15. 15-phút thesis defense typical: 5 phút slides giới thiệu + 10 phút demo + 15-20 phút Q&A. Currently zero defense materials prepared. Failure-mode audit identified 20 specific examiner challenges (4 archetypes × 5 questions) — cần response sheet cho từng question để confident defense.

## Proposed Fix

### Step 1: Slide deck (30-40 slides)

Tool: Reveal.js HTML deck (per `agent-action-bias.md` Part B — HTML/CSS over PowerPoint UI). Hosted local + exportable PDF.

Structure (Reveal.js):
- 3 slides Intro (problem statement + scope + outline)
- 5 slides Theoretical (multi-tenant + microservices + AI literature, cite GAP-650)
- 5 slides Requirements (use cases + ERD diagram từ `06-diagrams/plantuml/`)
- 5 slides Architecture (high-level + sequence + deployment, cite ADR-025)
- 5 slides Implementation (wave-pack methodology + audit-driven dev — RENAMED per Failure-mode "drop Wave/GAP terminology")
- 5 slides Testing & Evaluation (audit scores 6 dimensions, NFR results từ GAP-648)
- 3 slides Conclusion (achievements + limitations honest + future scope incl K-12)
- 2 slides Demo intro/transition
- 2 slides Q&A buffer (backup screenshots)

`documents/08-thesis/defense-deck/`:
- `index.html` Reveal.js master
- `slides/*.md` content per chapter
- `assets/` figures (link to thesis figures per GAP-651)
- `scripts/export-pdf.sh` (reveal-md export)

### Step 2: 15-min demo script consolidated

`documents/08-thesis/defense-deck/demo-script-15min.md`:
- Combine Persona audit P1 (10 phút primary) + GAP-652 multi-tenant isolation (5 phút secondary)
- Per minute breakdown
- Browser tab setup pre-demo (3 tabs pre-loaded with seeded data)
- Failure fallback per step (per Persona audit failure recovery column)
- "Cliffhanger" closing: tease Phase 2 K-12 future scope

### Step 3: Q&A response sheet

`documents/08-thesis/defense-deck/qa-response-sheet.md`:
- 20 Failure-mode questions × prepared response (per archetype + severity)
- Each response: ≤30s spoken + slide reference + evidence pointer
- Per archetype playbook:
  - Examiner A (Architecture Hawk): emphasize ADR docs + benchmark artifacts
  - Examiner B (NFR Auditor): emphasize audit scores + score evolution chart
  - Examiner C (Business): emphasize 5 beta tenants + market positioning
  - Examiner D (AI/Modern): emphasize honest limitations + Phase 2 roadmap

### Step 4: Backup demo recording

Pre-record full 15-min demo (slides + screen demo + voiceover):
- Tool: OBS Studio (CLI configurable per `agent-action-bias.md`)
- Output `documents/08-thesis/defense-deck/backup-demo.mp4`
- Fallback nếu live demo bug hoặc network issue defense room

### Step 5: Practice runs

- T-3 weeks: dry run 1 với advisor
- T-2 weeks: dry run 2 với 2 alumni examiner role-play
- T-1 week: final polish + rest

## Acceptance Criteria

- [ ] Reveal.js deck shipped 30-40 slides
- [ ] Demo script 15-min consolidated với fallback steps
- [ ] Q&A response sheet 20 questions covered
- [ ] Examiner archetype playbook documented
- [ ] Backup demo video recorded
- [ ] ≥2 practice runs completed before defense (T-3 + T-2)

## Related

- GAP-651 thesis-image-curation (slide figures)
- GAP-652 multi-tenant-isolation-demo (5-phút secondary)
- GAP-649 thesis-beta-cohort-execution (beta testimonials slide content)
- Failure-mode audit (20 examiner questions reference)
- Persona audit P1 demo script (10-phút primary content)
- VN benchmark §4 BREADTH vs DEPTH recommendation

## Log

- **2026-05-18 (created):** Filed per Failure-mode P0 #10 "v2.0.0 stable + defense materials prep" + Persona/Benchmark convergence.
