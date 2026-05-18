# GAP-650: Thesis Chapter 1 literature review — competitor + AI theory + VN law

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed (Business + AI + Compliance)
**Phase:** phase-1-beta
**Found:** 2026-05-18
**Related Audits:** [thesis-defense-failure-mode-matrix](../../audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md), [thesis-vn-saas-benchmark](../../audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md)

## Current State (verified 2026-05-18)

| Piece | Status |
|---|---|
| Competitor analysis Chapter 1 | ❌ missing (Failure-mode C2 + D3) |
| AI technique comparison (RAG/fine-tuning/multi-modal vs prompt-eng) | ❌ missing (Failure-mode D3) |
| VN law section (PDPL 2023 + Cybersecurity Law) | ⚠️ partial — PDPL mention scattered, không structured chapter |
| Existing Chapter 1 sources | per `documents/08-thesis/chapter-mapping.md` Ch1 maps `01-business/` + `07-archived/research/competitive/` |
| Competitive research raw | `documents/07-archived/research/competitive/` | ⚠️ archived nhưng không synthesized |

## Problem

Failure-mode aggregate P0 #4 + #8 + #9: "Chapter 1 literature review hoàn toàn thiếu — viết competitor table + AI theory + VN law section". Hội đồng VN CS thesis 2026 đặc biệt check Chapter 1 quality vì:
- Examiner C (Business) hỏi "So với BeeClass, Misa, EduFit — superior ở đâu?" (Q C2)
- Examiner D (AI) hỏi "RAG, fine-tuning, multi-modal LLaVA so sánh thế nào?" (Q D3)
- Examiner A (Architecture) cite ADR-025 nhưng cần literature review backing decisions

Chapter 1 thiếu = thesis weak foundation. 80-120 trang report mà foundation chapter 5-10 trang thin = examiner drill sâu tìm weakness.

## Proposed Fix

### Step 1: Competitor analysis section

`documents/08-thesis/references/chapter-1-competitor-analysis.md`:
- Bảng 5 đối thủ VN edu SaaS: EasyEdu, MISA EMIS, Mona LMS, Faceworks, BeeClass
- Per đối thủ: tính năng matrix, multi-tenant support, AI integration, pricing tier, target persona
- Positioning Kite Platform: unique differentiator (multi-tenant + AI + DevOps + security audit)

Cite per VN benchmark §2 industry research (EasyEdu 1,400+ trung tâm, MISA EMIS gov-facing, Faceworks 390k-1.1M/tháng).

### Step 2: AI technique comparison section

`documents/08-thesis/references/chapter-1-ai-techniques.md`:
- 4-approach comparison: prompt-engineering / fine-tuning / RAG / multi-modal LLaVA
- Per approach: cost ($/inference), latency (ms), quality (qualitative), maintenance overhead, vendor lock-in
- Rationale chọn GPT-4 + SD-XL prompt-engineering cho AI Branding feature

Academic refs (IEEE format per GAP-647):
- Brown et al. "Language Models are Few-Shot Learners" (GPT-3 paper)
- Lewis et al. "Retrieval-Augmented Generation for Knowledge-Intensive NLP" (RAG)
- Rombach et al. "High-Resolution Image Synthesis with Latent Diffusion Models" (Stable Diffusion)
- Liu et al. "Visual Instruction Tuning" (LLaVA)

### Step 3: VN law section

`documents/08-thesis/references/chapter-1-vn-law-compliance.md`:
- PDPL 2023 (Luật BVDLCN) — applicable articles cho edu SaaS: Art.9 (data subject rights), Art.11 (audit log), Art.14 (DSAR)
- Decree 13/2023/NĐ-CP — implementation details
- Luật An ninh mạng 2018 + Decree 53/2022/NĐ-CP — data localization
- TT 78/2021 + NĐ 123/2020 — VAT eInvoice (referenced Phase 1.5 scope)
- Mapping table: Law article → Feature implementation in Kite Platform → Evidence (V54 migration / audit log / consent flow)

### Step 4: Methodology section (audit-driven development)

Extend existing `documents/08-thesis/references/methodology.md`:
- Audit-driven development methodology (65+ rules, 200+ audits, 8 audit categories)
- Outside-in + inside-out trigger rules
- Wave-pack parallel agent strategy
- Trace academic refs: Continuous Improvement (Deming), Test-Driven Development (Beck), Domain-Driven Design (Evans)

### Step 5: Integration với chapter-mapping.md

Update `documents/08-thesis/chapter-mapping.md` Chapter 1 row:
- Primary sources: new 4 files above + existing `01-business/README.md`
- Supplementary: `07-archived/research/competitive/`, citations per GAP-647 bibliography

## Acceptance Criteria

- [ ] `chapter-1-competitor-analysis.md` shipped — 5-đối-thủ table + Kite positioning
- [ ] `chapter-1-ai-techniques.md` shipped — 4-approach comparison + rationale
- [ ] `chapter-1-vn-law-compliance.md` shipped — PDPL/Cybersecurity/VAT mapping table
- [ ] `methodology.md` extended với audit-driven development section
- [ ] All 4 docs có IEEE citations per GAP-647 format
- [ ] `chapter-mapping.md` Chapter 1 row updated với new sources
- [ ] Sample injection into thesis-docx (GAP-646 dependency) verified render đúng

## Related

- GAP-646 thesis-docx-pipeline (Chapter 1 injection)
- GAP-647 thesis-bibliography-ieee (citation backend)
- `documents/07-archived/research/competitive/` (raw research, synthesize)
- VN benchmark audit §2 industry references

## Log

- **2026-05-18 (created):** Filed per outside-in audit. Failure-mode P0 #4/#8/#9 convergence on Chapter 1 literature review missing.
