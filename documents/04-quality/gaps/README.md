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
| [GAP-005](GAP-005-ai-queue-fair-scheduling.md) | AI queue fair scheduling (WFQ per tier) + horizontal scaling | AI/Backend/DevOps | 🔴 P0 | 🔵 OPEN |
| [GAP-006](GAP-006-upgrade-to-gemma-4.md) | Upgrade AI models from Llama 3.1 + LLaVA → Gemma 4 E4B | AI/Backend | 🟠 P1 | 🔵 OPEN |

## File Naming Convention

`GAP-XXX-short-kebab-title.md` where XXX is zero-padded sequential ID.

## Template

Dùng template `_TEMPLATE.md` khi tạo gap mới.

---

**Last updated:** 2026-04-14
