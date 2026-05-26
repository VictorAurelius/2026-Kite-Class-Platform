---
title: Inside-out queue — user-flagged items beyond ROADMAP canonical
status: active
created: 2026-05-14
updated: 2026-05-26
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

### 2026-05-18 — QR upload approach Phase 1.5 payment

- **Source:** user direct (in-chat 2026-05-18 during Phase 1.5 brainstorm)
- **Phase relevance:** phase-1.5-paid
- **Status:** consumed (Wave 93)
- **Wave (if consumed):** wave-93 — 11 new gaps GAP-625..635 + 4 re-scope GAP-108/183/185/594 + base audit `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`
- **Description:** "Cho phép Owner có role hợp lý được phép chỉnh sửa mã QR nhận tiền học phí, thay vì thiết lập 1 hệ thống thanh toán phức tạp cho kiteclass — chỉ áp dụng cho đối tượng giáo viên đơn lẻ?" Outside-in audit (3-agent: persona + benchmark + failure-mode) reveals QR approach mandatory cho cả P1+P2 do compliance VN (PSP license + KYC merchant onboarding barrier). Industry norm 80%+ VN edu SaaS dùng QR. Phase 2 pivot VietQR EduPay partnership khi PH > 100. Pure SaaS subscription model preserved.

### 2026-05-18 — OCR auto-confirm receipt upload evolution

- **Source:** user direct (in-chat 2026-05-18 — evolution proposal sau QR base chốt)
- **Phase relevance:** phase-1.5-paid (initial); pivot phase-1.5b webhook
- **Status:** consumed (Wave 93 — REJECTED via outside-in audit; pivoted Casso/SePay webhook)
- **Wave (if consumed):** wave-93 — OCR audit `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md` + GAP-636 P1 Casso/SePay webhook investigation (replaces OCR proposal)
- **Description:** "Phát triển feature upload ảnh chuyển khoản cho hệ thống nhận diện số tiền, tài khoản, ngày tháng để hệ thống tự xác nhận, thay vì user phải tự check. Tiền mặt vẫn manual tick." Outside-in audit (3-agent OCR-specific) — Persona + Failure-mode evaluated OCR conditional Phase 1.5b/2; **Benchmark agent OVERRIDES** — VN edu SaaS 0/7 dùng OCR; Casso/SePay webhook là dominant pattern 2026 với ~0% fraud risk. OCR REJECTED primary; deferred Phase 2 fallback optional. Pivot Casso/SePay webhook Phase 1.5b.

---

## Log

- **2026-05-18 (later)** — Wave 93 consumed 2 items: (a) "QR upload approach Phase 1.5 payment" (canonical Phase 1.5 path); (b) "OCR auto-confirm receipt upload" (REJECTED via 3-agent outside-in audit benchmark OVERRIDE; pivoted Casso/SePay webhook GAP-636). Queue now 4 queued + 3 consumed. Both items demonstrate `outside-in-coverage-trigger.md` §3 5-Bước flow applied lần thứ 2+3 trong cùng session — closes `feedback_outside_in_recurring_miss.md` recurrence pattern.
- **2026-05-18** — Wave 92 Bucket D consumed "Manual split: professional vs end-user" item via sister rule path (option b): `professional-manual-content-standard.md` v1.0.0 shipped paired same-PR với `output-review-mandate.md` §3 row + `rules-index.csv` row + 3 retroactive self-test samples. End-user scope `user-manual-content-standard.md` v1.0.0 đã exist từ Wave 79. Queue now 4 queued + 1 consumed.
- **2026-05-17** — Appended item "Manual split: professional vs end-user" surfaced in-chat during Wave 87 planning session. Queue now 5 items (4 prior + 1 new). Wave 87 không consume; defer Wave 88+.
- **2026-05-14** — File created. Codified user inside-out queue per [`outside-in-coverage-trigger.md`](../../.claude/rules/outside-in-coverage-trigger.md) sister rule `inside-out-completeness-trigger.md`. Triggered by 2026-05-14 audit hole — Claude missed Premium plan / Feedback channel / Email content audit / user manual when planning Wave 78 because only pulled inside-out from ROADMAP §🚀.

### 2026-05-19 — Thesis V1 DOCX format MUST match báo cáo thực tập UTC spec

**Source:** User post-Wave-101 retro 2026-05-19 — "có inside yêu cầu bản docx phải có format tương đương như báo cáo thực tập chưa nhỉ, sao format của nó lại dở như vậy?"
**Status:** consumed (GAP-688 filed same session)

**Codified requirement:**
- Mọi DOCX academic deliverable trong dự án (báo cáo thực tập / khảo sát / đề cương / **khóa luận tốt nghiệp** / báo cáo defense) PHẢI match UTC official spec
- Reference path canonical: `documents/07-archived/academic/word-reports/templates/*.pdf` (3 official UTC PDF templates)
- Existing production pipeline: `create_bao_cao_thuc_tap.py` + `create_de_cuong_datn_v4.py` (Python python-docx)
- Format: Times New Roman 13pt body / 14pt heading / 18pt cover title; margins per artifact type (TTTN 25-20-25-30mm / DATN 20-20-20-25mm — verify per PDF template)
- Components: bìa chính + bìa phụ + nhận xét (nếu applicable) + lời cảm ơn + mục lục + danh mục hình/bảng/từ viết tắt + 4 chương + IEEE refs + phụ lục

**Anti-pattern this codifies:**
- ❌ Scoping new DOCX pipeline from scratch without checking `documents/07-archived/academic/word-reports/` existing tooling first
- ❌ Choosing pandoc default OR Java POI Create-from-scratch when production-quality Python pipeline exists
- ❌ Audit a generated DOCX without referencing the canonical UTC PDF template spec as scoring baseline

**Force-multiplier:** GAP-646 + GAP-687 + PR #1606 wasted scope choosing wrong pipeline path. 1 inside-out item codified → all future academic DOCX generation auto-reuses existing pattern.

### 2026-05-26 — PDPL beta scope-cut decision (3 sub-questions DEFER to Wave compliance-1 plan time)

**Source:** User question 2026-05-26 mid-session Wave rst-cascade-1 Phase 0 prep — "PDPL có thực sự cần thiết cho beta user không?"
**Status:** queued — defer scope-cut decision đến khi Wave compliance-1 plan time (post Wave rst-cascade-1 ship). User direction: "note đủ và đưa vào pending, giờ chỉ tập trung để đưa beta lên thôi" → focus shift Wave rst-cascade-1 + path-to-beta-launch.

**Context locked 2026-05-26:**
- PDPL hard deadline 2026-07-01 = ~5 tuần countdown
- CLAUDE.md Risk tolerance Moderate ("v1 pending counsel review" disclaimer OK cho non-K-12)
- Solo dev mode, no legal counsel engaged
- Audit log immutable (PDPL Art 11) đã ship Wave 92 V61 admin_audit_logs

**Codified analysis — PDPL CẦN cho beta nhưng SCOPE-CUT acceptable:**

MUST-HAVE Day 1 beta (~10-14 ngày Wave compliance-1 minimum scope):
1. Privacy notice tiếng Việt + ToS public (Art 13) — 2-3 ngày, "v1 pending counsel" disclaimer OK
2. Consent checkbox tại signup + granular per data category (Art 11) — 1-2 ngày FE+BE
3. Audit log immutable cho data access/modify (Art 11) — ✅ ĐÃ SHIP Wave 92
4. Data retention policy document (Art 9) — 1 ngày doc-only
5. Breach notification SOP 72h timeline (Art 23) — 1-2 ngày runbook
6. Right-to-access / right-to-erasure endpoint (Art 14-15) — 2-3 ngày BE+FE

DEFER Phase 2 / post-counsel:
- DPO formal appointment (Art 39 — beta <10k subjects threshold)
- Full DPIA report (Art 24 — K-12 mandate; beta P1/P2 adult/Owner acceptable risk acceptance)
- MPS cross-border transfer registration (Art 21 + 25 — beta <10k Art 25 lighter-touch)
- K-12 specific (parental consent + age gate) — Phase 3 K-12 trigger
- Cookie consent enterprise-grade — Phase 2

**3 sub-questions OPEN khi Wave compliance-1 plan time:**

1. **Risk position confirm**: Moderate scope-cut (5 items, ~2 tuần) HAY full 8 obligations (~4-5 tuần)?
2. **Counsel engagement timing**: Ship beta trước counsel HAY engage counsel-light review (~$500-1000 1-shot) trước beta?
3. **K-12 scope trong beta**: Exclude K-12 tenants khỏi 5-beta cohort (chỉ P1 Solo Teacher adult learner + P2 Center adult/teen non-K-12) để defer full K-12 obligations?

**Anti-pattern this codifies:**
- ❌ Lock Wave compliance-1 scope = full 8 obligations without checking acceptable risk position per CLAUDE.md Moderate
- ❌ Skip PDPL entirely cho beta vì "pilot scope" — KHÔNG có exemption trong NĐ 13/2023; MPS Art 22 NĐ 27/2023 fines 15-50M VND/violation
- ❌ Decide scope-cut without user explicit confirm 3 sub-questions trên

**Force-multiplier:** 1 scope-cut decision → Wave compliance-1 scope ~2 tuần thay vì ~4-5 tuần → Phase 1 BETA gate path khả thi trong 5-tuần PDPL window.

---

## Focus signal — 2026-05-26 active session

**User direction 2026-05-26:** focus chỉ trên path-to-beta-launch. Wave rst-cascade-1 = current. Hard-blocker waves (security-1 / ops-1 / compliance-1 / perf-1) + functional fix waves (class-teacher-fix-1 / idempotency-finish-1) + foundational (aws-rebuild-sop-1) defer scope-detail decisions đến khi plan time tương ứng.

Roadmap post Wave rst-cascade-1 đã note đầy đủ ở session message 2026-05-26 — KHÔNG re-confirm scope từng wave trước khi rst-cascade-1 ship.
