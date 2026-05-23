---
title: Release 1 Beta Cohort Execution Plan — 4 GV recruit + 9 tuần signed review
status: draft
created: 2026-05-23
updated: 2026-05-23
gaps: [GAP-623, GAP-649, GAP-612]
audience: dev
---

# Release 1 Beta Cohort Execution Plan

**Goal:** Tuyển 4 giáo viên (2 GV trial path A + 2 GV VIP path B) → 9 tuần Phase 1 BETA active use → thu ≥4 nhận xét ký tay phân biệt thesis 8 điểm vs 9-10 điểm.

**Status:** Plan-only (Wave thesis-1 closure). Execution defer Wave thesis-2 hậu GAP-612 AWS restore + cluster live ≥7 ngày.

**Blocked by:** GAP-612 (AWS account suspended Day 5+). Invite gửi CHỈ khi cluster live ≥7 ngày + GAP-369/370 email infra verified + PDPL consent gate (cookies + Privacy Policy) signed.

---

## §1 Bối cảnh + mục tiêu (objective)

### Lý do tồn tại cohort plan

Thesis Chương 4 (Implementation + Results) và Chương 6 (Validation + Future Work) cần evidence beta user feedback ký tay từ ≥4 personas đại diện. Đây là baseline phân biệt:

| Thesis score | Evidence quality |
|---|---|
| 7-8 điểm | Demo working + screenshots + dev tests; KHÔNG có real user feedback |
| **9-10 điểm** | **≥4 signed reviews từ real user (multiple personas) + production-like UX + iteration evidence** |

Per thesis audit T3 §Top-5 P0 #1 (`documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-failure-mode-matrix.md`), beta cohort execution + ≥4 bản nhận xét ký tay là chặn TRƯỚC khi viết Chương 4 production-ready.

### Mục tiêu cụ thể

1. **Quantity:** ≥4 GV active use ≥4 tuần liên tiếp + ≥4 signed review forms (ký tay PDF scan)
2. **Quality:** Mỗi review trả lời 4 câu open-ended depth (per §5 template), không phải checkbox satisfaction survey
3. **Diversity:** 2 acquisition paths (path A organic trial + path B warm intro) → validate cả 2 entry channel
4. **Timeline:** 9 tuần từ invite-send → final signed review collect, align với Phase 1 BETA window per `documents/03-planning/roadmap/release-1-plan-2026.md` §3 Phase 1 detailed scope

### Cite chapter mapping

Per [`documents/08-thesis/chapter-mapping.md`](../../08-thesis/chapter-mapping.md) — beta cohort findings sẽ feed:
- Chương 4 §4.X (Implementation results) — real usage metrics + bug surface count
- Chương 6 §6.X (Validation + lessons learned) — signed review excerpts + persona-specific friction points

---

## §2 Persona target (4 GV, 2 acquisition path)

### Path A — Trial Anonymous Prospect (2 GV)

**Persona archetype:** P2 Center Owner (chủ trung tâm dạy thêm 50-200 học sinh, mid-tier market)

**Acquisition channel:**
- Landing page `kitehub.me` → CTA "Yêu cầu truy cập Beta" → form email submit
- KiteHub admin (dev) tuyển chọn từ pool beta-access-request → gửi email trial code
- Self-onboard qua wizard (no high-touch hand-holding)

**Profile mô phỏng:**

| Profile field | GV trial 1 (Sky Education) | GV trial 2 (Quang Minh) |
|---|---|---|
| Tên Owner | Trần Thị Hằng | Lê Văn Tâm |
| Tên trung tâm | Trung tâm Anh ngữ Sky Education | Trung tâm Toán Quang Minh |
| Quy mô | 80 học sinh, 5 GV, 8 lớp | 120 học sinh, 7 GV, 12 lớp |
| Địa chỉ | 123 Lê Lợi, Q.1, TP.HCM | 45 Hai Bà Trưng, Hà Nội |
| Giải pháp đang dùng | Excel + Zalo manual | Misa Edu cũ + sổ tay |
| Pain point chính | Tốn time tổng hợp điểm danh + báo cáo phụ huynh | Khó track doanh thu per-lớp + chi nhánh |

**Why path A:** validate hypothesis "anonymous prospect có thể tìm + onboard tự organic" — critical cho post-thesis go-to-market.

### Path B — VIP warm intro (2 GV)

**Persona archetype:** P2 Center Owner + P3 Center Manager mixed (network warm intro)

**Acquisition channel:**
- Dev liên hệ trực tiếp qua warm intro (đồng nghiệp UTC / network GVHD / personal connection)
- Hand-holding onboarding qua Zoom call 60 phút mỗi GV
- Pre-seed sample data (lớp + GV + học sinh) trước khi GV thử

**Profile mô phỏng (placeholder Wave thesis-2 fill cụ thể):**

| Profile field | GV VIP 1 (warm intro network 1) | GV VIP 2 (warm intro network 2) |
|---|---|---|
| Tên Owner | (placeholder) | (placeholder) |
| Tên trung tâm | (placeholder — VN edu sample) | (placeholder — VN edu sample) |
| Quy mô | (placeholder) | (placeholder) |
| Quan hệ với dev | Warm intro qua mạng lưới GVHD UTC / personal connection | Warm intro qua đồng nghiệp dev |
| Tone hỗ trợ | Cao (Zoom call, ưu tiên xử lý feedback ≤24h) | Cao (Zoom call, ưu tiên xử lý feedback ≤24h) |

**Why path B:** validate "có support hand-holding for high-touch onboarding" — capture friction points không xuất hiện trong path A (self-onboard skip steps).

---

## §3 Timeline gantt 9 tuần

Tham chiếu T-0 = ngày defense thesis. Timeline backward planning:

| Tuần | Mốc | Ai chủ trách | Hoạt động chính |
|---|---|---|---|
| **T-9 (Tuần 1)** | Invite gửi | Dev | Gửi 4 invite email (2 path A + 2 path B) + Schedule onboard call qua Calendar 30-60 phút mỗi GV |
| **T-8 (Tuần 2)** | Onboard calls | Dev + GV | 4 buổi onboard call (path A 30 phút, path B 60 phút) — record Zoom/Google Meet; gửi credentials |
| **T-7 (Tuần 3)** | Setup support | Dev | Hỗ trợ wizard onboarding + seed sample data + first class create + first attendance entry |
| **T-6 đến T-3 (Tuần 4-7)** | Active use | GV chủ động + dev on-call | 4 tuần active classroom usage: attendance entry, grade entry, payment tracking, parent communication (Zalo + email) |
| **T-2 (Tuần 8)** | Mid-cohort feedback | Dev + GV | Mid-cohort survey + interview 30 phút mỗi GV — collect mid-trial friction + iterate priority fix list |
| **T-1 (Tuần 9)** | Final review collect | Dev + GV | Signed review form PDF (ký tay) + final interview 30 phút + thank-you call |
| **T-0 (Tuần 10)** | Defense | Dev solo | Defense Q&A reference cohort findings — quote signed review excerpts trong slide |

### Critical path dependencies

- **T-9 invite-send** chặn bởi: GAP-612 AWS restore (cluster live) + GAP-369 DNS production + GAP-370 email transactional (Resend / AWS SES verified) + GAP-353 PDPL consent gate
- **T-7 setup support** chặn bởi: KiteHub onboarding wizard end-to-end smoke pass + sample data seed script
- **T-2 mid-cohort survey** chặn bởi: Survey tool ready (Google Forms simple OR Tally OAuth)
- **T-1 signed review** chặn bởi: Signed review form template ready (PDF print-ready)

---

## §4 Invite flow narrative (template defer Wave thesis-2)

### Email persona tone

Per [`.claude/rules/vn-localization-audit-checklist.md`](../../../.claude/rules/vn-localization-audit-checklist.md) §2 Email tone matrix, P2 Center Owner formal-respectful:

- **Subject:** `Em chào chị/anh, mời tham gia beta KiteHub — quản lý trung tâm dạy thêm 9 tuần`
- **Greeting:** `Em chào chị Hằng,` / `Em chào anh Tâm,`
- **Closing:** `Trân trọng,\nNguyễn Vạn Kiệt\nKiteHub Team`

### Body 3-paragraph structure

**Đoạn 1 — Introduce (giới thiệu bản thân + dự án):**
> Em là Nguyễn Vạn Kiệt, sinh viên năm cuối ngành CNTT — UTC. Dự án khoá luận tốt nghiệp của em là KiteHub — nền tảng SaaS quản lý trung tâm dạy thêm cho thị trường Việt Nam.

**Đoạn 2 — Value prop (lý do beta + benefit):**
> Phiên bản Beta hiện đang mở mời 4 chủ trung tâm trải nghiệm miễn phí 9 tuần. KiteHub giúp chị/anh:
> - Quản lý điểm danh + điểm số tự động (không cần Excel)
> - Báo cáo doanh thu tháng tự động (tính per-lớp, per-chi nhánh)
> - Giao tiếp phụ huynh qua email + Zalo group chat
>
> Đổi lại, em mong nhận được ý kiến phản hồi sau 9 tuần dùng thử (bản nhận xét ký tay) phục vụ khoá luận.

**Đoạn 3 — Next step (CTA + lịch hẹn):**
> Nếu chị/anh quan tâm, em xin phép gửi Calendar link để hẹn buổi onboard call 30-60 phút giới thiệu hệ thống + setup tài khoản beta:
>
> [Link Calendly / Google Meet schedule]
>
> Hoặc trả lời email này em sẽ liên hệ trực tiếp.

### Template defer Wave thesis-2

Draft email template chi tiết (4 variants per persona tone matrix) defer Wave thesis-2 khi:
1. GAP-612 unblock (AWS restore confirmed)
2. Persona profile final (path B 2 GV warm intro confirm tên cụ thể)
3. Email infra ready (Resend domain verified)

---

## §5 Signed review template (PDF defer Wave thesis-2)

### Form structure (A4 PDF, 1-2 trang)

**Header:**
- Logo KiteHub + UTC (logo placeholder Wave thesis-2)
- Tiêu đề: `BẢN NHẬN XÉT — Beta KiteHub Cohort Phase 1 BETA 2026`
- Thông tin GV: Họ tên, chức danh, tên trung tâm, ngày ký

**4 câu open-ended depth:**

#### Câu 1 — Trải nghiệm onboard wizard (1-2 đoạn)

> Chị/anh mô tả trải nghiệm đăng ký tài khoản + onboard wizard KiteHub. Có bước nào confusing không? Bao lâu mới hoàn thành setup tài khoản đầu tiên?

**Probe:** signup flow, email verification, sample data seed, first class create, first attendance entry.

#### Câu 2 — Daily use friction (3-5 friction points cụ thể)

> Trong 4-7 tuần active use, chị/anh gặp những điểm khó chịu/bug/UX kém nào? Liệt kê 3-5 friction points cụ thể (vd: "Khó tìm nút xóa attendance entry sai", "Báo cáo doanh thu không export PDF được").

**Probe:** attendance UX, grade entry, payment tracking, parent communication, mobile responsive, error message clarity.

#### Câu 3 — So sánh với giải pháp đang dùng (1-2 đoạn)

> Trước khi dùng KiteHub, chị/anh quản lý trung tâm bằng giải pháp gì (Misa / Schoolnet / Excel manual / sổ tay)? KiteHub khác biệt thế nào? Có gì KiteHub làm tốt hơn? Có gì giải pháp cũ tốt hơn?

**Probe:** competitive landscape VN edu SaaS, switch cost, gain/loss vs current.

#### Câu 4 — Khuyến nghị cho future tenants (1 đoạn)

> Nếu một đồng nghiệp chủ trung tâm khác hỏi chị/anh "Có nên dùng KiteHub không?", chị/anh trả lời thế nào? Recommend / không recommend / điều kiện gì?

**Probe:** willingness-to-recommend (NPS-style), use case fit, future feature ask.

**Footer:**
- Chỗ ký + ghi rõ họ tên + chức danh + ngày
- Note: "Bản nhận xét này được sử dụng cho mục đích nghiên cứu khoá luận tốt nghiệp. Cảm ơn chị/anh đã đồng hành."

### Format + execution defer

- **Format:** A4 PDF print-ready, font Times New Roman 12pt body, sans-serif heading
- **Print:** GV in giấy → ký tay → scan/photo → gửi qua email/Zalo
- **Backup:** Google Forms digital fallback nếu GV không tiện in (less ideal — ký tay PDF stronger evidence cho thesis)
- **Execution defer Wave thesis-2:** PDF template + mailing/print flow defer khi cohort vào tuần 9

---

## §6 Risk + mitigation

| # | Risk | Probability | Impact | Mitigation |
|---|---|---|---|---|
| 1 | GAP-612 AWS restore timeline unknown (Day 5+ no AWS response) | Cao | Toàn bộ plan stall — invite không gửi được | Wave thesis-1 ship plan doc only (không stall scope); execution defer Wave thesis-2 hậu GAP-612 unblock; daily AWS support follow-up |
| 2 | Recruit không đủ 4 GV path A (anonymous prospect signup low) | Trung bình | Thiếu acquisition validation cho thesis | Backfill path B network (recruit 6 GV ban đầu thay 4 — 2 backup standby); GVHD UTC network giới thiệu thêm |
| 3 | GV drop mid-cohort (tuần 3-7) — bận / thấy không hữu ích / bug critical | Trung bình | <4 signed review final | Pre-recruit 6 GV ban đầu, target 4 signed final; mid-cohort feedback tuần 8 sớm phát hiện churn risk |
| 4 | Signed review thiếu depth (1-line answer thay vì 1-2 đoạn) | Thấp | Thesis 8 điểm thay 9-10 điểm | Mid-cohort interview pre-review (tuần 8) để iterate form clarity; show form template cho GV xem trước khi điền |
| 5 | Production incident block GV use (P0 bug stop classroom workflow) | Trung bình | Cohort UX broken, GV bỏ midway | Phase 1 BETA P0 fix gates pre-invite (per `release-1-plan-2026.md` Phase 1 gates) + on-call dev support 9 tuần |
| 6 | PDPL consent gate chưa pass → invite vi phạm law | Thấp (post GAP-353 cluster) | Legal risk + delay 2-4 tuần | GAP-353 cluster PDPL implementation phải DONE trước T-9 invite-send; pre-invite checklist verify consent gate live |
| 7 | Email transactional fail (Resend domain verify chậm) | Thấp | Invite không deliver | GAP-370 email infra verify ≥7 ngày trước T-9; backup Zalo direct outreach |
| 8 | Path B warm intro network không có 2 GV phù hợp | Trung bình | Path B thiếu data | GVHD UTC network expand (thêm GV mentor khác); accept 3 path A + 1 path B nếu cần |

### Backup recruit pool size

Pre-recruit **6 GV** (3 path A trial + 3 path B VIP) để defend lại risk #3 drop midway. Target final 4 signed review.

---

## §7 Acceptance criteria (plan-doc-only, Wave thesis-1 closure)

Per gap-done-discipline.md §3 PARTIAL exit ramp interpretation — plan-only mode acceptable cho Wave thesis-1 closure; concrete execution defer Wave thesis-2:

- [x] Plan doc shipped covering §1-§6 + executable timeline 9 tuần
- [x] 4 GV persona target defined (2 path A + 2 path B) với profile mô phỏng VN-friendly
- [x] Timeline gantt 9 tuần với mốc T-9 → T-0 + ai chủ trách + activities + dependencies
- [x] Risk matrix với 8 risks + mitigation per row
- [x] Signed review form template structure documented (4 câu open-ended depth)
- [x] Invite email persona tone documented (P2 Owner formal greeting + 3-paragraph body structure)
- [x] Critical path dependencies cross-linked (GAP-612 + GAP-369/370 + GAP-353 cluster + thesis chapter-mapping)
- [x] VN-localization satisfied per `vn-localization-audit-checklist.md` §2 (VND format dùng natural, persona greeting `Em chào chị/anh`, VN sample data `Trần Thị Hằng` / `Lê Văn Tâm` / `Trung tâm Anh ngữ Sky Education`)

---

## §8 Out-of-scope (Wave thesis-1; defer Wave thesis-2 post GAP-612 unblock)

Per [`.claude/rules/gap-done-discipline.md`](../../../.claude/rules/gap-done-discipline.md) §3 PARTIAL exit ramp — plan-only mode acceptable cho Wave thesis-1 closure. Items below defer Wave thesis-2 hoặc later phase:

### Draft templates

- Email invite template 4 variants (per persona tone matrix — path A trial vs path B VIP × 2 GV each) — defer Wave thesis-2 hậu persona profile final
- Calendar event template (.ics file) cho onboard call schedule — defer Wave thesis-2
- Signed review PDF print-ready format (A4, font, header logo) — defer Wave thesis-2 khi cohort vào tuần 9
- Mid-cohort survey form (Google Forms / Tally) — defer Wave thesis-2 tuần 8

### Production execution

- Invite send (4 emails T-9 tuần 1) — defer Wave thesis-2
- Onboard call schedule + execute (tuần 2) — defer Wave thesis-2
- Cohort run 9 tuần (tuần 3-9 active use) — defer Wave thesis-2
- Mid-cohort feedback collect + iterate (tuần 8) — defer Wave thesis-2
- Final signed review collect (tuần 9) — defer Wave thesis-2

### Trigger condition Wave thesis-2 unlock

- GAP-612 DONE (AWS account restored + cluster live ≥7 ngày stable)
- GAP-369 (DNS production) + GAP-370 (email transactional) + GAP-353 cluster (PDPL consent) verified
- KiteHub onboarding wizard end-to-end smoke pass + sample data seed script ready
- Path B warm intro 2 GV confirmed (tên + trung tâm cụ thể)

### Related follow-up gaps

- GAP-649 (beta cohort plan tracking) — này gap covers detail execution phase
- GAP-612 — AWS restoration P0 blocker
- GAP-369/370/353 — PDPL + email + DNS pre-requisites
- GAP-561b — invite-staff flow (potential auto invite mechanism, defer Wave thesis-3)

---

## Related

- User direction `documents/action-2.md` 2026-05-18 thesis brief — "2 giáo viên đơn lẻ + 2 business trial/vip"
- Roadmap: [`documents/03-planning/roadmap/release-1-plan-2026.md`](../roadmap/release-1-plan-2026.md) §3 Phase 1 detailed scope
- Thesis chapter mapping: [`documents/08-thesis/chapter-mapping.md`](../../08-thesis/chapter-mapping.md) — Chương 4 + 6 cite beta evidence
- Wave 100 outside-in audits:
  - [`documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-persona-simulation.md`](../../04-quality/audits/persona-review/2026-05-18-thesis-plan-persona-simulation.md) §Persona advisor concerns
  - [`documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-failure-mode-matrix.md`](../../04-quality/audits/persona-review/2026-05-18-thesis-plan-failure-mode-matrix.md) §Top-5 P0 #1
  - [`documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-vn-saas-benchmark.md`](../../04-quality/audits/persona-review/2026-05-18-thesis-plan-vn-saas-benchmark.md) — VN edu SaaS reference
- Gap files:
  - GAP-623 — beta cohort execution plan (this doc closes plan-only scope)
  - GAP-612 — AWS account suspension (blocker)
  - GAP-369/370 — DNS + email transactional
  - GAP-353 cluster — PDPL implementation
  - GAP-561b — invite-staff flow (Wave thesis-3 scope)
- Rules:
  - [`.claude/rules/vn-localization-audit-checklist.md`](../../../.claude/rules/vn-localization-audit-checklist.md) — VND format + persona tone matrix
  - [`.claude/rules/gap-done-discipline.md`](../../../.claude/rules/gap-done-discipline.md) §3 PARTIAL exit ramp — plan-only mode rationale
  - [`.claude/rules/dev-readable-doc-language.md`](../../../.claude/rules/dev-readable-doc-language.md) — Vietnamese narrative + English technical token
  - [`.claude/rules/outside-in-coverage-trigger.md`](../../../.claude/rules/outside-in-coverage-trigger.md) — audit findings re-use
  - [`.claude/rules/wave-closure-scope-completeness.md`](../../../.claude/rules/wave-closure-scope-completeness.md) — parallel scope discipline

**Last Updated:** 2026-05-23
