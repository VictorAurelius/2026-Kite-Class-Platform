# Support Escalation Runbook — decision tree + SLA per tier

**Audience:** Solo dev / Beta coordinator (Mai) + on-call support
**Created:** 2026-05-26 (Wave beta-prep-1 Bucket G4)
**Status:** Phase 1 BETA — `[v1 chờ tư vấn pháp lý]`
**Owner:** @nguyenvankiet
**References:**
- Sister: [`tenant-support-channels-runbook.md`](tenant-support-channels-runbook.md) — 3 kênh setup
- Sister: [`support-sla-phase-1-beta.md`](support-sla-phase-1-beta.md) — SLA cam kết
- Sister: [`incident-comms-runbook.md`](incident-comms-runbook.md) — comms cho incident scope
- Sister: [`incident-response-runbook.md`](incident-response-runbook.md) — production incident response
- Rule: [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md)

---

## 1. Mục đích

Decision tree quyết định **kênh hỗ trợ + tier SLA** dựa trên severity của vấn đề tenant báo cáo. Khác `tenant-support-channels-runbook.md` (setup từng kênh), runbook này tập trung **routing quyết định**.

---

## 2. Severity classification (4 tier)

| Tier | Mô tả | Ví dụ | Channel | SLA phản hồi đầu |
|---|---|---|---|---|
| **P0 (Critical)** | System unavailable trong giờ học; data leak; security breach | Lớp 18:00 đang dạy mà GV không login được; 1 tenant thấy data tenant khác | Phone | 30 phút (24/7 cho 5 cohort) |
| **P1 (High)** | Feature down ảnh hưởng workflow; payment/invoice sai | "Chuyển khoản rồi mà invoice vẫn UNPAID 24 giờ"; "Báo cáo doanh thu lỗi" | Zalo OA → email follow-up | 1 giờ (giờ làm việc) |
| **P2 (Medium)** | UX confusing; minor bug không block; "làm sao" question | "Em không biết tạo class schedule recurring"; "Nút 'Lưu' hơi mờ" | Zalo OA → docs link | 4 giờ (giờ làm việc) |
| **P3 (Low)** | Feature request; nice-to-have; cosmetic | "Em muốn export Excel với cột bonus"; "Logo có thể to hơn không" | Email → backlog | 24 giờ |

---

## 3. Decision tree

```
Tenant báo cáo vấn đề
        │
        ▼
┌─────────────────────────────────────────────────┐
│  Q1: Lớp đang dạy bị ảnh hưởng ngay?              │
│       (real-time impact: GV/HS không vào được)   │
└─────────────────────────────────────────────────┘
        │
   YES ─┴─ NO
    │       │
    ▼       ▼
[P0]    ┌──────────────────────────────────────┐
Phone   │  Q2: Workflow chính hỏng (payment,    │
30 phút │       invoice, attendance, grade)?    │
        └──────────────────────────────────────┘
                │
           YES ─┴─ NO
            │       │
            ▼       ▼
        [P1]    ┌──────────────────────────────┐
        Zalo    │  Q3: Là "làm sao để..." hay   │
        1 giờ   │       "có cách nào..." question? │
                └──────────────────────────────┘
                        │
                   YES ─┴─ NO
                    │       │
                    ▼       ▼
                [P2]    [P3]
                Zalo    Email
                4 giờ   24 giờ
```

### 3.1 Branching chi tiết Q1 (lớp đang dạy?)

Coordinator hỏi tenant qua chat:

```
Em xác nhận giúp 1 câu thôi nha:
"Hiện tại có lớp nào đang dạy/sắp dạy trong 30 phút tới bị ảnh hưởng không?"

Trả lời CÓ → mình gọi điện ngay
Trả lời KHÔNG → mình ưu tiên trong vòng 1 giờ qua chat
```

### 3.2 Branching chi tiết Q2 (workflow chính hỏng?)

Workflow chính = bất kỳ feature sau:

- Đăng nhập / signup
- Tạo lớp / quản lý lịch
- Điểm danh
- Tính học phí / invoice / payment
- Báo cáo doanh thu / chi tiêu
- Email/Zalo thông báo phụ huynh
- Export CSV/Excel cho audit

→ Nếu ≥ 1 workflow hỏng hoàn toàn (không workaround) = P1.

### 3.3 Branching Q3 ("làm sao" question)

P2 nếu là:
- "Làm sao để X?" (how-to → docs link)
- "Có cách nào X?" (feature exists question)
- "Tính năng X có không?" (discovery)

P3 nếu là:
- "Mong muốn thêm X" (feature request → backlog)
- "Có thể đổi màu/font/icon?" (cosmetic)

---

## 4. Per-tier procedure

### 4.1 P0 procedure (phone, 30 phút SLA)

1. Coordinator nhận signal (Zalo emoji 🚨 / phone direct call)
2. **Trong 5 phút:** acknowledge "Em nhận rồi, em đang vào hệ thống check ngay"
3. **Trong 30 phút:** root cause identified + workaround OR ETA fix
4. **Parallel:** mở incident chat (Discord/Slack cá nhân) → log từng step
5. **Sau resolve:** post-mortem mini (15 phút) → file gap nếu cần per `audit-to-gap-pipeline.md`
6. **Follow-up:** Zalo follow-up 24 giờ sau confirm tenant OK + thank-you

**Banned shortcut:** "Em sẽ check rồi báo lại" sau đó im lặng > 30 phút. Mỗi 15 phút silent = update status (kể cả "vẫn đang investigate").

### 4.2 P1 procedure (Zalo OA, 1 giờ SLA)

1. Coordinator nhận Zalo message
2. **Trong 5 phút:** auto-acknowledge template
3. **Trong 30 phút:** read context + reproduce nếu có thể
4. **Trong 1 giờ:** workaround OR ETA fix (P0 escalate nếu không workaround được)
5. **Parallel:** email follow-up với formal ticket ID nếu liên quan invoice/payment
6. **Sau resolve:** verify với tenant + thank-you

### 4.3 P2 procedure (Zalo + docs link, 4 giờ SLA)

1. Coordinator nhận Zalo message
2. **Trong 30 phút:** check docs có sẵn → reply với link
3. **Nếu docs không có:** offer screen-share 15 phút (tự deescalate P2 → P1 nếu tenant stressed)
4. **Trong 4 giờ:** confirm tenant đã giải quyết được hay chưa
5. **Sau resolve:** file gap user-manual P2 (per `user-manual-content-standard.md`) nếu docs gap

### 4.4 P3 procedure (email, 24 giờ SLA)

1. Coordinator nhận email
2. **Trong 4 giờ:** auto-acknowledge với ticket ID
3. **Trong 24 giờ:** evaluate request + add to backlog (gap file P3/P2) OR reject với explanation
4. **Quarterly:** review backlog P3 với cohort prioritization

---

## 5. Escalation ladder (dev → external)

```
┌─────────────────────────────────────────┐
│  L1: Coordinator (Mai / solo-dev)         │
│       — own most P0-P3                    │
└─────────────────────────────────────────┘
              │
              ▼ (escalate khi)
┌─────────────────────────────────────────┐
│  L2: Internal devs (Phase 2+ team)       │
│       — Phase 1 BETA: N/A solo            │
│       — Phase 2: 1 backend + 1 FE dev    │
└─────────────────────────────────────────┘
              │
              ▼ (escalate khi)
┌─────────────────────────────────────────┐
│  L3: Vendor support                       │
│       — AWS support (CloudWatch / RDS /  │
│         SES bounces)                      │
│       — Cloudflare support (DNS / DDoS)  │
│       — Resend support (email deliverab.) │
└─────────────────────────────────────────┘
              │
              ▼ (escalate khi)
┌─────────────────────────────────────────┐
│  L4: External counsel / regulator         │
│       — PDPL violation / breach           │
│       — K-12 MPS A05 (Phase 3)            │
└─────────────────────────────────────────┘
```

### 5.1 Khi nào escalate L1 → L2 (Phase 2+)

- Coordinator stuck > 1 giờ với P0/P1
- > 3 tickets cùng vấn đề trong 24 giờ
- Coordinator hết giờ làm (nightshift cần ngừoi khác)

### 5.2 Khi nào escalate L2 → L3 (vendor)

- AWS service issue (CloudWatch confirms vendor-side)
- DNS misroute (CF dashboard confirms)
- Email bounce > 5% (Resend/SES dashboard)

### 5.3 Khi nào escalate L3 → L4 (counsel)

- PDPL data leak (Article 11)
- Cybersecurity incident (Decree 53/2022)
- Regulator inquiry / subpoena

---

## 6. SLA tracking + breach handling

### 6.1 Tracking

Coordinator log mỗi ticket vào support sheet:

| Ticket | Severity | Channel | Received | First reply | Resolved | SLA met (Y/N) |
|---|---|---|---|---|---|---|
| TKT-001 | P1 | Zalo | 14:30 | 14:42 | 15:25 | Y |
| TKT-002 | P0 | Phone | 18:05 | 18:08 | 18:55 | Y (within 30min ack) |

### 6.2 SLA breach (missed)

- **P0 breach:** post-mortem same-day, file P0 retrospective gap
- **P1 breach:** retrospective vào weekly review
- **P2-P3 breach:** monthly review

### 6.3 Quarterly retro

Coordinator review SLA stats:
- % ticket meet SLA (target ≥ 95%)
- Top 3 ticket categories (input cho user-manual gaps)
- Channel mix (% Zalo / email / phone)
- Tenant satisfaction (kèm NPS từ `beta-cohort-onboarding-playbook.md` §7.1)

---

## 7. Handoff templates

### 7.1 P0 acknowledge

```
🚨 EM NHẬN RỒI 🚨
Em đang vào hệ thống check ngay cho chị/anh.
Em sẽ update mỗi 15 phút, kể cả đang investigate.
Thời gian dự kiến biết root cause: 15 phút nữa.
— Mai
```

### 7.2 P1 acknowledge

```
Em nhận rồi nha,
Em đang check log cho chị/anh, ưu tiên trong vòng 1 giờ.
Trong lúc đó chị/anh có thể workaround bằng cách [tạm thời X].
— Mai
```

### 7.3 P2 acknowledge

```
Cảm ơn chị/anh đã báo. Em check rồi gửi lại trong vòng 4 giờ làm việc.
Tham khảo trước: [docs link]
— Mai
```

### 7.4 P3 acknowledge

```
Em đã ghi nhận yêu cầu của chị/anh (mã TKT-XXXX).
Em sẽ đánh giá trong vòng 24 giờ và phản hồi qua email.
— Mai
```

---

## 8. Phase 1 BETA constraints (5-tenant cohort)

Với 5 tenant cohort solo-dev:

- Tổng tickets/tuần dự kiến: 15-30 (3-6/tenant/tuần)
- P0 expected: ≤ 1/tuần (post-cohort onboarding)
- P1 expected: 3-5/tuần (Phase 1 BETA bugs phổ biến)
- P2 expected: 5-15/tuần (docs/UX confusion phổ biến)
- P3 expected: 2-5/tuần (feature requests)

Coordinator workload estimate:
- P0 × 1 × 1h = 1h
- P1 × 4 × 0.5h = 2h
- P2 × 10 × 0.25h = 2.5h
- P3 × 3 × 0.5h = 1.5h
- **Total: ~7h/tuần** trong Phase 1 BETA (solo-dev sustainable)

Phase 1.5 (10-25 tenant) → ~20h/tuần → thuê thêm 1 support agent.

---

## 9. Standards reference

- `pre-handoff-self-test-completeness.md` §2.3 + §2.4 — flow verify mỗi resolve
- `vn-localization-audit-checklist.md` §2 + §4 — VN tone trong template + Zalo culture
- `professional-manual-content-standard.md` — runbook narrative discipline
- `dev-readable-doc-language.md` §2 — Vietnamese narrative
- `output-review-mandate.md` §3 row "Support escalation"
- `audit-to-gap-pipeline.md` — escalation → gap chain

---

## 10. Log

- **2026-05-26 (v1.0.0):** Runbook created cho Wave beta-prep-1 Bucket G4. Covers 4-tier severity classification (P0/P1/P2/P3) + decision tree 3-Q branching + per-tier procedure + 4-level escalation ladder (L1 coord → L2 dev → L3 vendor → L4 counsel) + SLA tracking + handoff templates VN-localized. Reviewer: @nguyenvankiet (solo-dev). Phase 1 BETA workload estimate ~7h/tuần với 5 cohort tenant.
