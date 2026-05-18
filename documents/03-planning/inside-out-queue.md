---
title: Inside-out queue — user-flagged items beyond ROADMAP canonical
status: active
created: 2026-05-14
updated: 2026-05-18
---

# Inside-out queue

**Purpose:** Single canonical append-only file để user (and Claude) log inside-out items (dev-proposed features / gaps / concerns) ngoài ROADMAP §🚀 Next Action. Mục đích = không miss user-proposed scope khi plan wave mới.

**Rule:** [`.claude/rules/inside-out-completeness-trigger.md`](../../.claude/rules/inside-out-completeness-trigger.md) — Claude PHẢI đọc file này trước khi lock scope wave plan (sister rule với `outside-in-coverage-trigger.md`).

**Mirror memory:** `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/project_phase_1_beta_inside_out_queue.md`

---

## How to add an item

| Method | Steps |
|--------|-------|
| **User direct dump** | Edit this file → append entry below per §Format. Commit khi convenient. |
| **In-chat flag** | Nói "Claude, ghi item X vào inside-out queue" → Claude append + commit same session. |
| **Wave plan miss** | Sau audit completeness, items found append với note `source: audit-YYYY-MM-DD`. |
| **Closure consumption** | Khi item lên wave → status `consumed` + reference wave/gap. KHÔNG delete (lưu lịch sử). |

## Format

```markdown
### YYYY-MM-DD — <Item title>

- **Source:** user-direct / in-chat / audit-YYYY-MM-DD / outside-in-agent
- **Phase relevance:** phase-1-beta / phase-1.5-paid / phase-2 / phase-3 / n/a
- **Status:** queued / consumed / dropped
- **Wave (if consumed):** wave-NN — GAP-NNN
- **Description:** ≤3 câu mô tả ý tưởng / lý do.
```

---

## Active queue

### 2026-05-14 — Premium plan / pricing surface Phase 1 BETA

- **Source:** user direct (confirm 2026-05-14 audit completeness AskUserQuestion)
- **Phase relevance:** phase-1-beta (disclaimer surface) / phase-1.5-paid (actual pricing model)
- **Status:** queued (defer Wave 79+)
- **Wave (if consumed):** —
- **Description:** "Tenant BETA pay gì?" — beta disclaimer + lifetime discount post-convert + TOS checkbox + pricing page "Free during beta". GAP-292 P0 (per-session pricing 200K/buổi) phase n/a tồn tại nhưng không cover Phase 1 BETA disclaimer scope. Audit Wave 78 = defer Wave 79 (Phase 1.5 trigger).

### 2026-05-14 — Feedback channel / post-onboarding survey

- **Source:** user direct (confirm 2026-05-14 audit completeness AskUserQuestion)
- **Phase relevance:** phase-1-beta
- **Status:** queued (Wave 78 candidate)
- **Wave (if consumed):** wave-78 (gap-stub GAP-542 by Wave 78 plan agent)
- **Description:** Structured feedback channel beyond N7 support channel (reactive) — in-app widget + email survey day-7/day-14 (Userpilot/Sequenzy benchmark). Beta tenants 3x feedback structured khi có automated workflow per Linear playbook.

### 2026-05-14 — Email content audit (5 email types content/tone VN)

- **Source:** user direct (confirm 2026-05-14 audit completeness AskUserQuestion)
- **Phase relevance:** phase-1-beta
- **Status:** queued (Wave 78 candidate)
- **Wave (if consumed):** wave-78 (gap-stub GAP-543 by Wave 78 plan agent)
- **Description:** Audit content + tone toàn bộ 5 email types (welcome / approval / invite / verify / password-reset) trước beta send. Extends Wave 77 Bucket A (GAP-370 infra + GAP-533 deliverability) sang content/tone Vietnamese-first audit per `dev-readable-doc-language.md`.

### 2026-05-14 — User manual Vietnamese (screenshots-based, per-persona)

- **Source:** user mention in meta-comment 2026-05-14 (initial attribution ambiguous; user confirmed via file gap)
- **Phase relevance:** phase-1-beta
- **Status:** queued (Wave 79 — depends UI kit chốt)
- **Wave (if consumed):** GAP-537 filed 2026-05-14; consumed Wave 79
- **Description:** Phase 1 BETA invite-only tenants cần tài liệu hướng dẫn Vietnamese screenshots-based. Depends FE stable → UI kits đóng (GAP-348/364/428). Phase 1 capture + draft + publish across Wave 79+.

### 2026-05-17 — Manual split: professional vs end-user (2 doc tracks)

- **Source:** user direct (in-chat 2026-05-17 during Wave 87 planning session)
- **Phase relevance:** phase-1-beta (end-user track cho beta cohort) + phase-1.5+ (professional track ongoing)
- **Status:** consumed (Wave 92 Bucket D)
- **Wave (if consumed):** wave-92 — sister rule path chosen (option b): `professional-manual-content-standard.md` v1.0.0 shipped 2026-05-18 paired same-PR với `output-review-mandate.md` §3 row + `rules-index.csv` row + 3 retroactive self-test samples. End-user scope (`user-manual-content-standard.md` v1.0.0) đã exist từ Wave 79. Phase 1 BETA professional manual concrete content (architecture diagrams, dev integration guides, ops runbooks polish per 15-item checklist) defer Wave 88+ audience-by-audience cadence.
- **Description:** Manual hiện tại text-only không đủ cho 2 audience. Cần tách 2 track: (1) **Professional system manual** — cho founder/dev/tester, text + visual explanation (architecture diagram, data-flow, troubleshooting), ngôn ngữ kỹ thuật ok; (2) **End-user manual** — cho tenant (P2 Owner / Teacher / Parent / Anonymous prospect), heavy screenshots + annotations trực tiếp trên hình ảnh (arrow + callout + step number), minimal text, task-oriented. Reference: `user-manual-content-standard.md` v1.0.0 đã codify end-user scope (§2 15-item checklist + annotated screenshots); cần (a) extend rule với professional sister-scope HOẶC (b) create sister rule `professional-manual-content-standard.md`. Bổ sung cho GAP-537 (Vietnamese user manual đang in-flight Wave 79+). Wave 87 KHÔNG include; surface để Wave 88+ planner consume.

---

## Consumed / Historical

(empty — items move here khi wave merge với reference)

---

## Audit-surfaced items (2026-05-14 audit)

5 inside-out BLOCKING phase-1-beta items audit found beyond ROADMAP canonical — added to Wave 78 scope by Wave 78 plan agent (separate gap files):

- GAP-480 Beta invitation flow doc (existing OPEN P1 — consumed Wave 78)
- GAP-527 kitehub-email actuator + E2E smoke (existing OPEN P1 — consumed Wave 78)
- GAP-531 Tenant init handoff post admin-approve (existing OPEN P1 — consumed Wave 78)
- GAP-040 Support impersonation tools (existing OPEN P1 — defer Wave 79 pairs N7)
- PDPL DSAR + DPO verify (existing scope per Wave 26/48 — defer Wave 79 verify status)

---

## Log

- **2026-05-18** — Wave 92 Bucket D consumed "Manual split: professional vs end-user" item via sister rule path (option b): `professional-manual-content-standard.md` v1.0.0 shipped paired same-PR với `output-review-mandate.md` §3 row + `rules-index.csv` row + 3 retroactive self-test samples. End-user scope `user-manual-content-standard.md` v1.0.0 đã exist từ Wave 79. Queue now 4 queued + 1 consumed.
- **2026-05-17** — Appended item "Manual split: professional vs end-user" surfaced in-chat during Wave 87 planning session. Queue now 5 items (4 prior + 1 new). Wave 87 không consume; defer Wave 88+.
- **2026-05-14** — File created. Codified user inside-out queue per [`outside-in-coverage-trigger.md`](../../.claude/rules/outside-in-coverage-trigger.md) sister rule `inside-out-completeness-trigger.md`. Triggered by 2026-05-14 audit hole — Claude missed Premium plan / Feedback channel / Email content audit / user manual when planning Wave 78 because only pulled inside-out from ROADMAP §🚀.
