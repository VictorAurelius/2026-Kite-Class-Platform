# GAP-653: Thesis defense prep — slide deck + Q&A prep + examiner archetype anticipation

**Status:** 🟢 DONE 2026-05-23 — Wave thesis-1 Bucket C shipped 4 defense artifacts (deck 40 slide + Q&A 20 câu + demo script 15 phút 6 phase + practice schedule 2 buổi dry-run). Backup recording + 2 practice runs queued cho T-3/T-2 weeks pre-defense.
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

- [x] Reveal.js deck shipped 30-40 slides — 40 slides ship Wave thesis-1 Bucket C (`documents/08-thesis/defense/defense-deck.html`) với 4 Mermaid diagrams (AI pipeline + C4 architecture + AWS deployment + auth sequence) + speaker notes mọi content slide
- [x] Demo script 15-min consolidated với fallback steps — `documents/08-thesis/defense/defense-demo-script.md` 6 phase × tổng 14-15 phút (Anonymous → Admin onboard → Tenant wizard → Multi-tenant proof → Audit → Wrap) + fallback per step + backup recording note
- [x] Q&A response sheet 20 questions covered — `documents/08-thesis/defense/defense-qa-response-sheet.md` 4 archetype × 5 câu hỏi mỗi response ≤120 từ + evidence cite cụ thể
- [x] Examiner archetype playbook documented — 4 archetype (Architecture / NFR-DB-DevOps / Business-Compliance / Process-Methodology) trong Q&A response sheet + cheatsheet "Top 5 evidence cite mạnh nhất"
- [ ] Backup demo video recorded — DEFERRED to T-3 week dry-run cycle per `practice-schedule.md` § Pre-defense T-1 ngày; recording tool OBS Studio + retention strategy documented
- [ ] ≥2 practice runs completed before defense (T-3 + T-2) — SCHEDULED per `documents/08-thesis/defense/practice-schedule.md` (Buổi 1 T-3 tuần full deck + demo, Buổi 2 T-2 tuần Q&A drill) — practice runs là time-bound deferred work, không phải scope work; gap closure đúng cho artifact deliverables

## Out-of-scope (track separately)

| Item | Where |
|---|---|
| Backup demo recording MP4 actual capture | Practice cycle T-3 week per `practice-schedule.md` § Buổi 1 setup; not committed (file > 100MB Google Drive) |
| 2 practice runs execution | Time-bound activities scheduled per `practice-schedule.md`; gap tracks artifact deliverables, not the practice events themselves |

## Related

- GAP-651 thesis-image-curation (slide figures)
- GAP-652 multi-tenant-isolation-demo (5-phút secondary)
- GAP-649 thesis-beta-cohort-execution (beta testimonials slide content)
- Failure-mode audit (20 examiner questions reference)
- Persona audit P1 demo script (10-phút primary content)
- VN benchmark §4 BREADTH vs DEPTH recommendation

## Log

- **2026-05-23 (Wave thesis-1 Bucket C shipped — DONE flip):** 4 defense artifacts shipped per Acceptance Criteria. Reveal.js deck 40 slide tiếng Việt + 4 Mermaid diagrams + speaker notes. Q&A sheet 20 câu × 4 archetype × 5 câu/archetype. Demo script 15 phút × 6 phase. Practice schedule 2 buổi dry-run T-3/T-2 + pre-defense checklist + defense day checklist + contingency plans. README ship cho folder `documents/08-thesis/defense/`. Backup recording + 2 practice runs là time-bound deferred work tracked trong practice-schedule (§Out-of-scope) — gap closure đúng cho artifact deliverables. Content discipline verified per `thesis-content-standard.md`: zero repo jargon (Wave/GAP/bucket/BETA) trong slide content + Q&A response body; zero Claude/AI assistant attribution; Vietnamese narrative + English technical tokens preserved per `dev-readable-doc-language.md` §4 mixed-language pattern.

- **2026-05-18 (created):** Filed per Failure-mode P0 #10 "v2.0.0 stable + defense materials prep" + Persona/Benchmark convergence.
