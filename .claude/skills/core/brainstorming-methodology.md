---
description: "Dùng khi bắt đầu PR medium+ complexity, user nói 'brainstorm', 'plan trước', 'chưa rõ scope', 'nên chọn approach nào'. Bắt buộc: features mới, architecture changes, cross-service integrations. Skip: bug fixes rõ ràng, config/doc-only changes."
---

# Brainstorming Methodology

## Khi nào dùng (Mandatory)

- New features (Medium+ complexity)
- Architectural decisions (service boundaries, data models)
- Cross-service integrations
- Unclear or ambiguous requirements
- PRs marked "Complexity: Medium/High"

## Khi nào skip

- Simple bug fixes (well-defined problem)
- Typo corrections, documentation updates, config changes

**When in doubt:** 10 phút brainstorm. Better to over-clarify than rework.

## KiteClass Decision Log

Các quyết định đã được đưa ra — không debate lại:

- **Wave vs PR lẻ:** Wave = sprint nhiều task liên quan; PR lẻ = single purpose. Quyết định trong brainstorm trước khi code.
- **Multi-tenant storage:** Luôn PostgreSQL với `instance_id` — không Redis-only cho persistent data.
- **Service boundary:** Không tách service mới khi scale <10k students; KiteClass Core đảm nhận tất cả.

## Gotchas

- **Đọc business docs TRƯỚC** (`/pre-flight-check domain`) — tránh redecide điều đã documented trong `rules.md`
- **Wave/PR decision phải xong ở brainstorm** — thay đổi giữa chừng gây branch naming conflict
- **Multi-tenant**: mọi feature touching data isolation → explicit brainstorm decision, không assume safe

## Quick Process (20-40 phút)

1. **Question Assumptions** (10 min) — What problem? Why now? Who uses it? Success criteria? Constraints?
2. **Explore Trade-offs** (15 min) — ≥2 options, trade-off matrix, weighted scoring
3. **Document Decision** (10 min) — Chosen approach + rationale + why alternatives rejected

## Skill Contents

- `quick-reference/quick-brainstorm-template.md` — Copy-paste 5-min template
- `quick-reference/brainstorming-question-templates.md` — Question bank theo category
- `quick-reference/brainstorming-trade-off-matrix.md` — Matrix template + Attendance Storage example
- `quick-reference/design-decision-documentation.md` — Decision doc template + full worked example

## Trigger Phrases

"brainstorm", "plan trước", "chưa rõ scope", "nên dùng A hay B", "đánh giá approach", "Complexity: Medium/High"

## Quick Checklist

- [ ] Question assumptions? (what/why/who/success criteria/constraints)
- [ ] ≥2 alternatives explored? (trade-off matrix)
- [ ] Decision documented? (chosen + rationale + rejected alternatives)

**If rushed:** 10 min questioning + 1-line rationale = minimum viable brainstorm
