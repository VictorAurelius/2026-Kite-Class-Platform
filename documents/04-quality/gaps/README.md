# Gaps Queue — Design & Implementation Issues

Hàng đợi các design gaps / implementation gaps được phát hiện. Mỗi gap = 1 file, được fix theo priority. Không fix ngay trong session phát hiện, tránh scope creep.

## Workflow

1. **Phát hiện** — gap được tạo file `GAP-XXX-title.md` với status `OPEN`
2. **Prioritize** — user review, assign priority (P0 blocker → P3 nice-to-have)
3. **Plan** — gap có thể trở thành: PR riêng, wave, hoặc task trong wave có sẵn
4. **Track** — update status trong file khi tiến triển
5. **Close** — khi merged, đổi status `DONE` + link PR

## Status Legend

| Status | Ý nghĩa |
|--------|---------|
| 🔵 OPEN | Đã document, chưa có plan |
| 🟡 PLANNED | Có PR/wave nhận xử lý |
| 🟠 IN_PROGRESS | Đang implement |
| 🟢 DONE | Đã merged |
| ⚫ WONTFIX | Quyết định không fix (lý do ghi trong file) |

## Priority Legend

| Priority | Ý nghĩa |
|----------|---------|
| 🔴 P0 | Blocker — phải fix trước khi ship |
| 🟠 P1 | High — fix trong sprint tới |
| 🟡 P2 | Medium — fix khi có resource |
| 🟢 P3 | Low — nice-to-have |

## Active Queue

| ID | Title | Domain | Priority | Status |
|----|-------|--------|:--------:|:------:|
| [GAP-001](GAP-001-kiteclass-gateway-decision.md) | Quyết định giữ/xóa kiteclass-gateway service | Architecture | 🟡 P2 | 🔵 OPEN |
| [GAP-002](GAP-002-ai-async-pipeline.md) | Async pipeline cho heavy AI tasks (image gen) | AI/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-003](GAP-003-ai-multi-tier-image-generation.md) | Multi-tier image generation strategy | AI/Backend | 🟡 P2 | 🔵 OPEN |
| [GAP-004](GAP-004-template-based-image-composition.md) | Template-based image composition (Canva-like) | AI/Frontend | 🟡 P2 | 🔵 OPEN |
| [GAP-005](GAP-005-ai-queue-fair-scheduling.md) | AI queue fair scheduling + capacity plan (100 users feasible on Oracle Free) | AI/Backend/DevOps | 🔴 P0 | 🔵 OPEN |
| [GAP-006](GAP-006-upgrade-to-gemma-4.md) | Upgrade AI models Llama 3.1 + LLaVA → Gemma 4 E4B | AI/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-007](GAP-007-resource-classification-pipeline.md) | Resource classification pipeline (static/template/full-AI) | AI/Backend | 🔴 P0 | 🔵 OPEN |
| [GAP-008](GAP-008-ai-agent-workflow.md) | AI Agent workflow (analyzer→planner→executor) thay direct gen | AI/Backend | 🟠 P1 | 🔵 OPEN |
| [GAP-009](GAP-009-instance-provisioning-lifecycle.md) | Frontend instance provisioning lifecycle (6 states) | Backend/DevOps | 🟠 P1 | 🔵 OPEN |
| [GAP-010](GAP-010-branding-package-api-integration.md) | Branding package API + KiteClass integration + E2E test | Backend/Frontend | 🟠 P1 | 🔵 OPEN |
| [GAP-011](GAP-011-template-library-curation-plan.md) | Template library curation plan + review standards | Design/Product | 🔴 P0 | 🔵 OPEN |
| [GAP-012](GAP-012-frontend-instance-quality-review.md) | Automated frontend instance quality review (post-deploy) | Quality/AI/FE | 🟠 P1 | 🔵 OPEN |
| [GAP-013](GAP-013-guided-branding-wizard-ux.md) | Guided branding wizard UX (closed-loop with flexibility) | FE/UX/Product | 🟠 P1 | 🔵 OPEN |
| [GAP-014](GAP-014-wave-mock-include-ai-branding.md) | Wave mock plan missing AI branding workflow | Mock Data/Wave | 🔴 P0 | 🔵 OPEN |
| [GAP-015](GAP-015-tenant-provisioning-auto-trigger-branding.md) | Tenant provisioning thiếu auto-trigger AI branding (event-driven) | Backend/FE/Integration | 🔴 P0 | 🔵 OPEN |
| [GAP-016](GAP-016-ai-branding-v2-living-docs-impact.md) | AI Branding v2 living docs impact scope (business/API/ERD/tests) | Docs/Governance | 🔴 P0 | 🔵 OPEN |

## Developer Rules

- **[`.claude/rules/ai-branding-guidelines.md`](../../../.claude/rules/ai-branding-guidelines.md)** — MANDATORY rules cho developers làm AI branding feature

## AI Branding Master Design

Key feature redesign doc: **[`documents/02-architecture/ai-branding-v2-redesign.md`](../../02-architecture/ai-branding-v2-redesign.md)** (supersedes old `ai-local-implementation-plan.md` §architecture)

GAP-005, 007, 008, 009, 010 đều derive từ redesign doc này.

## File Naming Convention

`GAP-XXX-short-kebab-title.md` where XXX is zero-padded sequential ID.

## Template

Dùng template `_TEMPLATE.md` khi tạo gap mới.

---

**Last updated:** 2026-04-14
