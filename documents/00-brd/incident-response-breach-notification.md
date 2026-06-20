# Incident Response + Breach Notification Policy — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** SRE + DPO
**Reviewer:** Legal counsel + Tech Lead + Security Lead
**Legal basis:** **Nghị định 13/2023/NĐ-CP (PDPL — L1)** Điều 23 (nghĩa vụ thông báo vi phạm dữ liệu cá nhân ≤72h tới Bộ Công an); **Luật An ninh mạng 2018 (L6)** (xử lý sự cố an ninh mạng); cross-ref GDPR Art 33 (72h breach notification, extraterritorial)
**Related:** [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.1 VN-PDPL-5 + PART 2 EU-GDPR-7 · [`compliance-scope.md`](compliance-scope.md) §2.5 (PDPL breach) + §4 (Cybersecurity/L6) · `05-guides/operations/incident-response-runbook.md` · `05-guides/operations/breach-notification-sop.md` · `05-guides/operations/incident-comms-runbook.md` · [`child-protection-policy.md`](child-protection-policy.md) §7 · [`data-classification-policy.md`](data-classification-policy.md)

---

## 1. Phạm vi & mục đích

Tài liệu này là **policy cấp BRD** định nghĩa khung phản ứng sự cố (incident response) và **nghĩa vụ thông báo vi phạm dữ liệu** (breach notification). Đây là tầng policy — các SOP vận hành chi tiết đã tồn tại tại `05-guides/operations/` (incident-response-runbook, breach-notification-sop, incident-comms-runbook); tài liệu này hợp nhất khung + nghĩa vụ pháp lý làm nguồn cho các runbook đó.

Skeleton Phase 1: severity model + timeline + nghĩa vụ PDPL ≤72h + comms plan structure. Chi tiết liên hệ cơ quan + on-call roster cần ops + legal sign-off Phase 2.

> Lưu ý: child-safety incident có khung riêng nghiêm hơn — xem [`child-protection-policy.md`](child-protection-policy.md) §7 (mandatory reporting tới MOLISA/cảnh sát). Tài liệu này KHÔNG override khung đó.

---

## 2. Severity Levels (phân loại mức độ)

| Severity | Định nghĩa | Ví dụ |
|:---:|---|---|
| **SEV1** | Service down toàn bộ HOẶC data breach xác nhận chứa PII/sensitive (T3/T4) | RDS mất, mass PII leak, ransomware |
| **SEV2** | Tính năng chính lỗi HOẶC nghi ngờ breach chưa xác nhận phạm vi | Auth lỗi 1 service, suspicious exfiltration signal |
| **SEV3** | Lỗi cục bộ, có workaround, không có dấu hiệu data exposure | 1 endpoint 500, degraded performance |
| **SEV4** | Cosmetic / minor, không ảnh hưởng data hoặc uptime | UI glitch, typo |

Phân loại dữ liệu trong scope sự cố theo [`data-classification-policy.md`](data-classification-policy.md) — T3/T4 trong scope = nâng severity tối thiểu SEV2.

> TBD (Phase 2 — needs security input): tiêu chí định lượng phân biệt SEV1/SEV2 (số record, loại tier, xác nhận exfiltration).

---

## 3. Response Timeline (khung thời gian phản ứng)

| Giai đoạn | SEV1 | SEV2 | Mô tả |
|---|:---:|:---:|---|
| Detect → Acknowledge | TBD ≤15m | TBD ≤1h | on-call nhận + xác nhận |
| Triage + containment | TBD ≤1h | TBD ≤4h | cô lập, ngăn lan rộng |
| Mitigation | TBD ≤4h | TBD ≤1 ngày | khôi phục dịch vụ |
| **Breach assessment** | ngay khi nghi ngờ PII | ngay khi nghi ngờ | xác định có phải data breach không (§4) |
| Post-mortem | ≤7 ngày | ≤7 ngày | RCA + preventive actions |

> TBD (Phase 2 — needs SRE input): SLA timeline chốt theo on-call capacity; align với [`customer-sla-uptime.md`](customer-sla-uptime.md) §3 support windows.

### 3.1 Roles

Incident Commander (IC) + DPO (cho breach) + Comms lead. On-call roster — TBD Phase 2 (ref incident-response-runbook).

---

## 4. Breach Notification Obligation (nghĩa vụ thông báo vi phạm)

### 4.1 PDPL ≤72h (L1 — bắt buộc)

Theo **Nghị định 13/2023/NĐ-CP Điều 23** (VN-PDPL-5): khi xảy ra **vi phạm dữ liệu cá nhân** (personal data breach), nền tảng PHẢI thông báo tới **Bộ Công an (A05/Cục An ninh mạng)** trong vòng **72 giờ** kể từ khi phát hiện.

Quy trình (skeleton — chi tiết tại `breach-notification-sop.md`):

1. **Xác nhận breach:** DPO + Security xác định sự cố có cấu thành "vi phạm dữ liệu cá nhân" (lộ/mất/truy cập trái phép T3/T4) không.
2. **Đồng hồ 72h bắt đầu** từ thời điểm phát hiện (detect), không phải thời điểm xác nhận đầy đủ phạm vi.
3. **Thông báo cơ quan:** nội dung gồm bản chất breach, loại + số lượng dữ liệu, hậu quả khả dĩ, biện pháp khắc phục.
4. **Thông báo chủ thể dữ liệu** (data subject) khi breach có rủi ro cao tới quyền lợi — timeline TBD.
5. **Lưu hồ sơ breach** (immutable audit) cho thanh tra.

> TBD (Phase 2 — needs legal input): mẫu form thông báo A05; ngưỡng "rủi ro cao" kích hoạt thông báo cá nhân; thời hạn thông báo data subject (likely "không chậm trễ bất hợp lý").

### 4.2 GDPR (extraterritorial, nếu có user EU)

EU-GDPR-7: 72h tới supervisory authority. Áp dụng nếu có user truy cập từ EU. Default conservative: chuẩn bị quy trình tương thích cả PDPL + GDPR.

### 4.3 Child-data breach

Breach chứa minor data (T4) → áp dụng **đồng thời** [`child-protection-policy.md`](child-protection-policy.md) §4.4 (thông báo MOLISA + cảnh sát A05 + phụ huynh ≤72h) — nghiêm hơn breach thường.

---

## 5. Communications Plan (kế hoạch truyền thông)

| Đối tượng | Khi nào | Kênh | Owner |
|---|---|---|---|
| Cơ quan (A05) | breach ≤72h | văn bản chính thức | DPO |
| Data subject bị ảnh hưởng | breach rủi ro cao | email + in-app | DPO + Comms |
| Tenant (Owner/Admin) | SEV1/SEV2 | email + status page | Comms |
| Public / status page | service-impacting | status page (ADR-027) | Comms |
| Phụ huynh (child breach) | minor breach | call + email | T&S + DPO |

Chi tiết wording + template: `incident-comms-runbook.md`.

> TBD (Phase 2 — needs comms input): pre-approved comms template per scenario; tránh over-disclosure (VN-ADV substantiation, không gây hoảng loạn).

---

## 6. Post-mortem (hậu sự cố)

- Mọi SEV1/SEV2 có **post-incident review ≤7 ngày**: timeline, root cause (RCA), preventive actions, follow-up gaps.
- Blameless culture — tập trung process, không cá nhân.
- Output feeds back vào roadmap (preventive gap filing).

> TBD (Phase 2): post-mortem template + tracking; quarterly aggregate report cho compliance audit (ref GAP-156).

---

## 7. Tuân thủ pháp lý (Compliance)

- **PDPL L1 (`compliance-scope.md` §2.5):** breach notification ≤72h tới Bộ Công an — **🔴 BLOCKING** obligation. VN-PDPL-5.
- **Cybersecurity L6 (`compliance-scope.md` §4.2):** hợp tác với cơ quan khi có yêu cầu; xử lý sự cố an ninh mạng.
- **GDPR (extraterritorial):** EU-GDPR-7 nếu áp dụng.

---

## 8. Dependencies / References

- Runbooks: `05-guides/operations/incident-response-runbook.md`, `breach-notification-sop.md`, `incident-comms-runbook.md`
- BRD: [`compliance-scope.md`](compliance-scope.md) §2.5/§4, [`data-classification-policy.md`](data-classification-policy.md), [`child-protection-policy.md`](child-protection-policy.md) §7, [`customer-sla-uptime.md`](customer-sla-uptime.md)
- Checklist: [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.1 VN-PDPL-5, PART 2 EU-GDPR-7
- ADR-027 (statuspage-vendor)

---

## 9. Out of Scope (this skeleton)

- A05 notification form template (Phase 2 — Legal)
- On-call roster + escalation contacts (Phase 2 — SRE ops)
- Comms templates per scenario (Phase 2 — Comms + Legal)

---

## 10. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). Severity model + response timeline + PDPL ≤72h breach obligation + comms plan structure complete; timeline numbers + agency contacts + templates marked TBD (Phase 2, needs ops + legal). Cites PDPL L1 Art 23 (VN-PDPL-5) + Cybersecurity L6 + GDPR Art 33; references existing operational runbooks as policy companion.
