# Customer-facing SLA + Uptime Commitment — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + SRE
**Reviewer:** Legal counsel + Tech Lead + Business Lead
**Legal basis:** **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (19/2023/QH15) (Consumer Protection — L3)** — minh bạch điều khoản dịch vụ, quyền của người tiêu dùng khi gián đoạn dịch vụ
**Related:** [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.3 VN-CON + PART 4 SLA clauses · [`compliance-scope.md`](compliance-scope.md) §6 (Consumer Protection/L3) · [`nfr-catalog.md`](nfr-catalog.md) §2 (Uptime) + §3 (RTO/RPO) + §10 (Tier-NFR matrix) · [`terms-of-service.md`](terms-of-service.md) · [`pricing-model.md`](pricing-model.md)

---

## 1. Phạm vi & mục đích

Tài liệu này là **SLA hướng khách hàng** (customer-facing) — cam kết uptime, cửa sổ hỗ trợ, lịch bảo trì, và chính sách credit khi vi phạm SLA. Khác với [`nfr-catalog.md`](nfr-catalog.md) (mục tiêu kỹ thuật nội bộ), SLA là **cam kết hợp đồng** có thể đính kèm TOS/MSA.

Skeleton Phase 1: khung cam kết + cấu trúc credit; **số liệu uptime cụ thể cần SRE baseline + Legal sign-off Phase 2** — KHÔNG fabricate uptime number trước khi có đo lường thực tế.

> TBD (Phase 1.5 — needs SRE input): SLA chỉ publish được sau khi có ≥30 ngày uptime baseline đo bằng synthetic monitor.

---

## 2. Uptime Commitment per Tier

Mục tiêu lấy từ [`nfr-catalog.md`](nfr-catalog.md) §2 (đó là target nội bộ; SLA cam kết = target trừ buffer). **Mọi giá trị dưới là placeholder.**

| Tier | Uptime cam kết | Downtime tối đa/tháng | Đo lường |
|---|:---:|:---:|---|
| FREE | best-effort (no SLA) | — | n/a |
| BASIC | TBD ~99.5% | TBD ~3.6h | synthetic monitor |
| PREMIUM | TBD ~99.9% | TBD ~43m | synthetic monitor |
| ENTERPRISE | TBD ~99.95% (negotiable MSA) | TBD ~22m | synthetic + custom |

> TBD (Phase 2 — needs SRE + legal input): cam kết SLA phải thấp hơn hoặc bằng NFR target có buffer; chốt sau baseline đo lường thực tế.

### 2.1 Phương pháp đo

- Synthetic monitor endpoint + grace period — TBD (ref `nfr-catalog.md` §2).
- Loại trừ scheduled maintenance (xem §4) khỏi tính toán downtime.
- Cửa sổ đo: rolling 30 ngày (calendar month).

> TBD (Phase 2): chọn monitoring vendor (Pingdom/Datadog/Statuspage — ref ADR-027 statuspage-vendor); định nghĩa "available" (HTTP 2xx trên health endpoint trong N giây).

---

## 3. Support Response Windows (cửa sổ hỗ trợ)

| Severity | Mô tả | First-response SLA | Tier áp dụng |
|:---:|---|:---:|---|
| **P1** | Service down / không truy cập được | TBD ≤1h | PREMIUM+ |
| **P2** | Tính năng chính lỗi, có workaround | TBD ≤4h | BASIC+ |
| **P3** | Lỗi nhỏ / câu hỏi | TBD ≤1 ngày làm việc | tất cả |
| **P4** | Feature request / cosmetic | best-effort | tất cả |

Kênh hỗ trợ: email + in-app ticket (Phase 1); hotline/Zalo OA — TBD Phase 2.

> TBD (Phase 2 — needs support-ops input): support hours (8x5 vs 24x7 per tier), escalation roster, kênh hotline.

---

## 4. Maintenance Windows (lịch bảo trì)

- **Scheduled maintenance:** thông báo trước ≥TBD 7 ngày, trong khung giờ thấp điểm (TBD — gợi ý 00:00–04:00 ICT).
- **Emergency maintenance:** thông báo sớm nhất có thể; loại trừ khỏi SLA nếu để vá lỗ hổng bảo mật khẩn (cross-ref [`incident-response-breach-notification.md`](incident-response-breach-notification.md)).
- Maintenance trong cửa sổ đã thông báo **không tính** vào downtime SLA.

> TBD (Phase 2): tần suất tối đa maintenance/quý; kênh thông báo (email + status page + in-app banner).

---

## 5. Service Credits Policy (chính sách bồi thường)

Khi uptime thực tế dưới cam kết, khách hàng đủ điều kiện nhận **service credit** (giảm trừ chu kỳ billing kế tiếp), KHÔNG hoàn tiền mặt (xem [`refund-dispute-resolution-policy.md`](refund-dispute-resolution-policy.md) cho hoàn tiền).

| Uptime thực tế (tháng) | Credit (% phí tháng) |
|---|:---:|
| Dưới cam kết nhưng ≥ TBD% | TBD 5% |
| Dưới TBD% | TBD 10% |
| Dưới TBD% | TBD 25% |

> TBD (Phase 2 — needs finance + legal input): bậc credit + cap tối đa; quy trình claim (khách hàng phải request trong N ngày); KHÔNG auto-credit để tránh lạm dụng.

### 5.1 Loại trừ (exclusions)

Credit KHÔNG áp dụng khi downtime do: force majeure, scheduled maintenance đã thông báo, lỗi từ phía khách hàng, tấn công DDoS ngoài tầm kiểm soát hợp lý, dịch vụ bên thứ ba (payment gateway, email provider).

---

## 6. Tuân thủ pháp lý (Compliance)

### 6.1 Consumer Protection Law (L3 — `compliance-scope.md` §6)

- **VN-CON-1:** thay đổi material SLA phải thông báo trước 30 ngày.
- **VN-CON-2:** khách hàng có quyền hủy đơn phương nếu không đồng ý thay đổi SLA.
- **VN-CON-4:** điều khoản SLA phải bằng tiếng Việt rõ ràng (không English-only cho user VN).
- **VN-CON-5:** tranh chấp SLA cho consumer-tenant (trung tâm nhỏ, giáo viên solo) thuộc thẩm quyền tòa án VN — arbitration clause không ràng buộc consumer.

### 6.2 SLA required clauses (compliance-checklist PART 4)

Khi publish SLA chính thức, bắt buộc có: uptime target, measurement methodology, exclusions, service-credit formula, claim process, reporting cadence.

> TBD (Phase 2 — needs legal input): xác nhận SLA cho enterprise (B2B lớn) có thể dùng arbitration; consumer-tier phải giữ tòa án VN.

---

## 7. Dependencies / References

- BRD: [`nfr-catalog.md`](nfr-catalog.md) §2/§3/§10, [`terms-of-service.md`](terms-of-service.md), [`pricing-model.md`](pricing-model.md), [`refund-dispute-resolution-policy.md`](refund-dispute-resolution-policy.md), [`incident-response-breach-notification.md`](incident-response-breach-notification.md)
- Compliance: [`compliance-scope.md`](compliance-scope.md) §6, [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.3 + PART 4 SLA
- ADR-027 (statuspage-vendor)

---

## 8. Out of Scope (this skeleton)

- Uptime baseline numbers (Phase 1.5 — SRE, cần ≥30d đo lường)
- SLA contract template per tier (Phase 2 — Legal)
- Monitoring vendor + endpoint config (Phase 2 — ADR)

---

## 9. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). Uptime/support/maintenance/credit structure complete; all numeric targets marked TBD (Phase 2, needs SRE baseline + finance + legal). Cites Consumer Protection L3 + SLA required clauses.
