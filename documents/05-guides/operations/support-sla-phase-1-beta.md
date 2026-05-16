# Support SLA Phase 1 BETA (Wave 86 Bucket H H-AC14)

**Owner:** Solo dev / Support
**Created:** 2026-05-16 (Wave 86 Bucket H H-AC14)
**Last Updated:** 2026-05-16
**Related gap:** GAP-592 P2
**Scope:** Phase 1 BETA (5 cohort tenant). Phase 1.5+ SLA tightening per separate doc.

---

## 1. Mục đích

Phase 1 BETA cần cam kết SLA rõ ràng để:

- Tenant biết kỳ vọng response time → giảm friction
- Solo dev có deadline để prioritize ticket
- Industry benchmark VN edu SaaS = **4-6h business hours** (per Wave 86 Bucket A benchmark Q9 — Misa, Edmicro tier comparison)
- Ambiguous SLA = trust risk; tenant có thể kỳ vọng Intercom 1-min response

---

## 2. SLA commitment

### 2.1 First-response SLA

| Ticket type | First-response time (business hours) | Resolution time target |
|---|---|---|
| **P0 — Production blocker** (login fail, data loss, payment fail) | **< 2h** | < 24h |
| **P1 — Major functional** (cannot create class, attendance broken) | **< 4h** | < 48h |
| **P2 — Minor functional / UX** (typo, slow page, cosmetic) | **< 24h** | < 7 days |
| **P3 — Feature request** | **< 48h ack** | Roadmap consideration |

### 2.2 Business hours definition

**Phase 1 BETA business hours:**

- **Thứ 2 → Thứ 6:** 09:00 - 18:00 (GMT+7 Hanoi/HCMC)
- **Thứ 7:** 09:00 - 12:00 (sáng only)
- **Chủ nhật + ngày lễ VN:** off (best-effort response Phase 1.5+)

**Ticket nhận ngoài business hours:** clock starts at next business hour open.

Example:
- Ticket gửi Thứ 7 14:00 → clock starts Monday 09:00
- Ticket gửi Thứ 2 08:00 → clock starts 09:00 (cùng ngày)

### 2.3 Channels

| Channel | Use case | SLA priority |
|---|---|---|
| **Email support@kitehub.me** | Default — written record | Per §2.1 |
| **Zalo OA** (Phase 1.5+) | Quick question, FAQ | Best-effort < 2h |
| **In-app feedback widget** (Phase 2+) | Bug report with screenshot | Per §2.1 |
| **Phone hotline** | KHÔNG provide Phase 1 BETA — overhead lớn cho solo dev | — |

---

## 3. Ticket tracking (Phase 1 BETA — spreadsheet manual)

**Location:** `documents/04-quality/metrics/phase-1-beta-support-tickets.csv` (UTF-8 BOM)

Columns:

| Column | Type | Example |
|---|---|---|
| `ticket_id` | TKT-YYYYMMDD-NN | TKT-20260520-01 |
| `tenant_id` | UUID | abc... |
| `tenant_name` | string | "Trung tâm Sky" |
| `received_at` | ISO datetime | 2026-05-20T10:30:00+07 |
| `priority` | P0/P1/P2/P3 | P1 |
| `category` | enum | login / classes / payment / attendance / other |
| `subject` | string | "Không tạo được lớp" |
| `first_response_at` | ISO datetime | 2026-05-20T12:15:00+07 |
| `first_response_minutes_bh` | int | 105 (business-hours minutes) |
| `sla_met` | bool | true |
| `resolved_at` | ISO datetime / null | 2026-05-20T16:00:00+07 |
| `resolution_summary` | string | "User typo subdomain - reset" |
| `notes` | string | |

**Phase 1.5+ automation:** migrate to Helpdesk SaaS (Freshdesk free tier, Crisp free tier). Trigger when ticket volume > 10/week.

---

## 4. Workflow

### 4.1 Email arrival

```
[Email gửi support@kitehub.me]
         │
         ▼
[AWS SES inbound → S3 bucket kitehub-support-inbound]
         │
         ▼
[SES rule chain: forward to vannkite@outlook.com personal inbox]
         │
         ▼
[Solo dev mở email, log spreadsheet, classify priority]
         │
         ▼
[Reply trong SLA window — primary support@kitehub.me]
         │
         ▼
[Update spreadsheet first_response_at + sla_met]
```

**Note:** Phase 1 BETA chỉ inbound forwarding; outbound từ support@kitehub.me cần Resend domain verified (per Wave 84 deploy plan account-prep).

### 4.2 SLA breach process

Khi miss SLA (vd P1 ticket > 4h response):

1. **Apologize** trong reply: "Xin lỗi vì phản hồi muộn..."
2. **Document** trong spreadsheet `sla_met=false` + reason
3. **Root cause** trong notes: "(a) Off-hours nhận thứ 7 tối, (b) Volume burst 3 ticket cùng giờ, (c) Vacation/personal"
4. **Pattern check:** nếu > 2 miss/tuần → re-evaluate SLA realistic chưa, hoặc rotate support capacity

---

## 5. Cite trong invite email (per G-AC3)

Phase 1 BETA invite email template phải include:

```
## Hỗ trợ trong giai đoạn beta

KiteHub đang trong giai đoạn beta. Chúng tôi cam kết:

- **Email phản hồi < 4h** trong giờ hành chính (Mon-Fri 9-18h, Sat 9-12h GMT+7)
- **Sự cố nghiêm trọng phản hồi < 2h**
- **Channel:** support@kitehub.me (kèm chi tiết: trung tâm + mô tả + screenshot nếu có)

Lưu ý: KiteHub đang trong giai đoạn beta, một số tính năng còn đang được hoàn
thiện. Phản hồi của anh/chị giúp chúng tôi cải tiến nhanh hơn.

Chi tiết SLA: {link → support-sla-phase-1-beta.md public mirror}
```

---

## 6. Public mirror (FAQ surface)

Phase 1 BETA + Phase 1.5+: render SLA doc thành tenant-facing FAQ trên `kitehub.me/help/sla`. Translation:

- Vietnamese narrative (per `user-manual-content-standard.md`)
- Cập nhật lần cuối date visible
- Print-friendly CSS

**Source:** `documents/05-guides/user-manual/anonymous/sla.md` (deferred Wave 87+ — Phase 1 BETA scope dừng ở internal doc + invite email cite).

---

## 7. Metrics review

### 7.1 Weekly (cùng cadence cohort-retention-tracking.md §5.1)

- Tổng ticket nhận
- % SLA met theo priority
- Top 3 categories (login / classes / payment ?)
- Resolution time median

### 7.2 Monthly

- Trend SLA met rate vs tháng trước
- Identify systemic gap (vd 50% ticket "login fail" → triage UI/UX issue → wave plan gap)
- Re-evaluate SLA realistic không

---

## 8. Escalation matrix

| Situation | Action |
|---|---|
| > 5 ticket cùng category 1 tuần | File gap file Persona-Review category root cause |
| 1 tenant > 3 ticket P0 cùng tháng | 1-on-1 call (Zalo / Google Meet) — keep tenant |
| Vacation/illness > 2 ngày | Email auto-responder: "Mình đang off, sẽ phản hồi từ {date}" + Phase 1.5+ delegate to teammate |
| Solo dev capacity saturated | Phase 1.5+ trigger: hire CS support (tracked gap) |

---

## 9. Related

- Gap: GAP-592 P2 (Wave 86 Bucket H H-AC14)
- Wave 86 Bucket A audit: benchmark-vn-saas-edu Q9 (industry 4-6h email standard)
- Sister docs: `cohort-retention-tracking.md`, `beta-invite-flow.md`, `incident-comms-runbook.md`
- Rules: `dev-readable-doc-language.md`, `user-manual-content-standard.md` (future tenant-facing mirror)
- Future: helpdesk SaaS migration runbook (Phase 1.5+ scope)
