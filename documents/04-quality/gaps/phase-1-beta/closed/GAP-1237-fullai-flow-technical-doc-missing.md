# GAP-1237: Thiếu tài liệu kỹ thuật hợp nhất cho flow FULL_AI + prompt catalog

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Docs
**Found:** 2026-06-12 (user hỏi "có tài liệu kỹ thuật về flow full ai chưa, dùng các prompt gì?" — investigation cho thấy rải 4 chỗ, không chỗ nào đủ)
**Affects:** `documents/02-architecture/` — flow FULL_AI 2-mode

## Problem

Thông tin flow FULL_AI rải ở: ADR-037 (decision-level), `ai-branding-deploy-flow.md` (chỉ SSE deploy — 0 mention FULL_AI), `01-business/kitehub/ai-branding/api-contract.md` (endpoint shapes), `ai-branding-guidelines.md` §2.3 (nguyên tắc fixed-prompt). KHÔNG có doc nào: (a) flow 2-mode end-to-end với chuỗi gate mode-resolve, (b) prompt catalog thật đang dùng (4 prompts nằm cứng trong code), (c) quota/fallback matrix, (d) sequence preview→approve→deploy→landing đổi.

## Fix shipped (cùng PR)

`documents/02-architecture/ai-branding-generation-flow.md` — TL;DR + §1 chuỗi gate mode-resolve (5 hàng, fallbackReason × quota) + §2 Mermaid sequence end-to-end (gồm outbox `branding.deployed` → KC-core + SSE token) + §3 prompt catalog 4 prompts verbatim kèm nguồn + §4 config keys/secrets (gồm AWS SM source cho OPENAI_API_KEY) + §5 refs.

## Acceptance Criteria

- [x] 1 doc hợp nhất flow 2-mode + gate chain + prompt catalog + config — `ai-branding-generation-flow.md`
- [x] Mermaid sequence render được trên GitHub (per diagram-format-selection; không `<br/>` trong sequence)
- [x] Cross-ref 2 chiều: doc cite ADR-037/guidelines/gaps; không lặp nội dung `ai-branding-deploy-flow.md`

## Related

- ADR-037 · GAP-1213/1217/1218/1135/1147/1021/1108 (flow components) · GAP-1251 (api-contract endpoints — phần docs riêng)
