# Data Classification + Handling Policy — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** Tech Lead + DPO
**Reviewer:** Legal counsel + Security Lead + DPO
**Legal basis:** **Nghị định 13/2023/NĐ-CP (PDPL — L1)** Điều 2 (phân loại dữ liệu cá nhân + dữ liệu nhạy cảm), Điều 16 (dữ liệu trẻ em); **Luật An ninh mạng 2018 + Nghị định 53/2022/NĐ-CP (Cybersecurity — L6)** (data localization + bảo vệ hệ thống thông tin)
**Related:** [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.1 VN-PDPL-8/9 · [`compliance-scope.md`](compliance-scope.md) §2 (PDPL/L1) + §4 (Cybersecurity/L6) · ADR-013 (data-retention-classification) · ADR-011 (defense-in-depth-security) · [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) · [`child-protection-policy.md`](child-protection-policy.md) · [`logs-format-standard.md`](../../.claude/rules/logs-format-standard.md) §3 PII scrubbing

---

## 1. Phạm vi & nguyên tắc

Tài liệu này định nghĩa **các tier phân loại dữ liệu** và **quy tắc xử lý (handling rules) per tier** cho toàn bộ nền tảng. Mục tiêu: mọi dữ liệu trong hệ thống đều có 1 classification rõ ràng, từ đó suy ra quy tắc lưu trữ, mã hóa, access control, logging, retention.

Nguyên tắc cốt lõi:
- **Classification-driven handling:** quy tắc xử lý quyết định bởi tier, không ad-hoc per field.
- **Highest-tier-wins:** record chứa nhiều loại dữ liệu áp dụng tier cao nhất.
- **Minor data = stricter:** dữ liệu trẻ em (<16) luôn nâng tier theo PDPL Art 16 (xem [`child-protection-policy.md`](child-protection-policy.md)).

Đây là **skeleton Phase 1** — khung tier + handling matrix; ngưỡng cụ thể (encryption algo, key rotation cadence, access role) cần Security + Legal sign-off Phase 2.

---

## 2. Data Classification Tiers

5 tier, từ thấp đến cao về độ nhạy cảm:

| Tier | Tên | Định nghĩa | Ví dụ |
|:---:|---|---|---|
| **T0** | **Public** | Công khai, không gây hại nếu lộ | Tên trung tâm public, landing copy, pricing tier names |
| **T1** | **Internal** | Nội bộ, không PII, lộ gây bất tiện nhẹ | Config không-secret, feature flags, aggregated analytics đã anonymize |
| **T2** | **Confidential** | Bí mật business, lộ gây tổn hại business | Tenant subscription terms, internal pricing, AI prompt templates, secrets (xử lý riêng) |
| **T3** | **PII** | Dữ liệu cá nhân (PDPL Art 2) | Tên, email, SĐT, CCCD, địa chỉ, DOB của user/teacher/parent |
| **T4** | **Sensitive / Child** | Dữ liệu nhạy cảm (PDPL Art 2.4) + dữ liệu trẻ em (Art 16) | Health/conduct của học sinh, dữ liệu sinh trắc, dữ liệu minor <16, financial transaction detail |

> TBD (Phase 2 — needs legal input): xác nhận ranh giới T3↔T4 cho từng field cụ thể; PDPL Art 2.4 liệt kê 10 nhóm sensitive — map đầy đủ vào schema.

---

## 3. Handling Rules per Tier

Bảng quy tắc xử lý. Giá trị TBD cần Security workshop Phase 2.

| Handling dimension | T0 Public | T1 Internal | T2 Confidential | T3 PII | T4 Sensitive/Child |
|---|:---:|:---:|:---:|:---:|:---:|
| Encryption at rest | ❌ | optional | ✅ AES-256 | ✅ AES-256 | ✅ AES-256 + key tách biệt |
| Encryption in transit | TLS 1.3 | TLS 1.3 | TLS 1.3 | TLS 1.3 | TLS 1.3 |
| Access control | none | role-based | RBAC chặt | RBAC + audit log đọc | RBAC tối thiểu + audit mọi access |
| Logging (giá trị raw) | OK | OK | masked | **PII scrubber bắt buộc** | **tuyệt đối không log raw** |
| Data localization (NĐ53/L6) | n/a | n/a | TBD | ✅ store tại VN | ✅ store tại VN |
| Retention | n/a | n/a | per contract | per matrix | **stricter** (minor ≤6mo) |
| Cross-border transfer | OK | OK | TBD | DPIA bắt buộc | **cấm trừ DPIA + approval** |

> TBD (Phase 2 — needs security/legal input): key rotation cadence, access role enumeration, masking format per field, data-localization residency decision (AWS Singapore vs VN — ref ADR-025 + `compliance-scope.md` §4.1).

### 3.1 Mapping sang code

- PII scrubbing: implement theo [`logs-format-standard.md`](../../.claude/rules/logs-format-standard.md) §3.
- Encryption + access: ref ADR-011 (defense-in-depth-security).
- Retention: ref ADR-013 + [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) §2 matrix.

> TBD (Phase 2): mỗi entity field trong `01-business/*` cần annotation classification tier (`@DataClass(T3)` hoặc tương đương) — externalize, không hardcode.

---

## 4. Tuân thủ pháp lý (Compliance)

### 4.1 PDPL (L1 — `compliance-scope.md` §2)

- **Art 2** — classification dữ liệu cá nhân vs nhạy cảm: T3/T4 tiers map trực tiếp. (VN-PDPL-8: quyền access/correct/delete/portability áp dụng T3/T4.)
- **Art 16** — dữ liệu trẻ em: nâng T3→T4 cho minor (VN-PDPL-9 parental consent). Xem [`child-protection-policy.md`](child-protection-policy.md) §3 Minor Data Handling Matrix.
- **Art 6** — minimization: chỉ thu thập + giữ tier cao khi cần; ưu tiên anonymize (T3/T4 → T1) khi hết mục đích.

### 4.2 Cybersecurity Law (L6 — `compliance-scope.md` §4)

- **NĐ 53/2022 data localization:** T3/T4 của user VN PHẢI store tại VN ≥24 tháng. Quyết định server placement — TBD (ref `compliance-scope.md` §4.1).
- **Bảo vệ hệ thống thông tin:** encryption + access control + audit trail theo cấp độ.

> TBD (Phase 2 — needs legal input): xác nhận khung phân loại "hệ thống thông tin quan trọng" theo Luật ANM có áp dụng cho nền tảng ở quy mô nào.

---

## 5. Dependencies / References

- ADR-013 (data-retention-classification), ADR-011 (defense-in-depth-security)
- BRD: [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md), [`child-protection-policy.md`](child-protection-policy.md), [`privacy-policy.md`](privacy-policy.md), [`compliance-scope.md`](compliance-scope.md), [`nfr-catalog.md`](nfr-catalog.md) §6 Security NFRs
- Rule: [`logs-format-standard.md`](../../.claude/rules/logs-format-standard.md) §3 PII scrubbing
- Checklist: [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.1

---

## 6. Out of Scope (this skeleton)

- Per-field classification annotation trong code (Phase 2 — engineering)
- Data localization residency final decision (Phase 2 — ADR + legal)
- Key management + rotation runbook (Phase 2 — security)

---

## 7. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). 5-tier classification + handling matrix structure complete; encryption/access/localization specifics marked TBD (Phase 2, needs security + legal input). Maps PDPL L1 (Art 2/16/6) + Cybersecurity L6 (NĐ 53/2022).
