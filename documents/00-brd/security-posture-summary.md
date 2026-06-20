# Security Posture Summary — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** Security Lead + Tech Lead
**Reviewer:** Legal counsel + SRE
**Legal basis:** **Luật An ninh mạng 2018** · **Nghị định 13/2023/NĐ-CP (PDPL — L1)** an toàn dữ liệu cá nhân · **Nghị định 53/2022/NĐ-CP** (localization/an ninh mạng)
**Related:** [`data-classification-policy.md`](data-classification-policy.md) · [`incident-response-breach-notification.md`](incident-response-breach-notification.md) · [`compliance-scope.md`](compliance-scope.md) · audits: `04-quality/audits/security/`

---

## 1. Phạm vi & mục đích

Bản tóm tắt **hướng-khách-hàng / due-diligence** về tư thế bảo mật — dùng cho enterprise sales, vendor assessment, trust page. KHÔNG lộ chi tiết kiến trúc khai thác được; tóm tắt control + chứng nhận. Chi tiết kỹ thuật ở audit reports nội bộ (`04-quality/audits/security/`).

Skeleton Phase 1: khung control summary; **số liệu + chứng nhận cụ thể tham chiếu security-audit gần nhất** (2026-06 baseline ~85+/100; verdict refresh đang chạy).

---

## 2. Security controls (tóm tắt)

| Lĩnh vực | Control | Trạng thái Phase 1 |
|---|---|:---:|
| Authentication | JWT HS512, account lockout (GAP-515), rate-limit (GAP-514) | ✅ shipped |
| Authorization | gateway header-strip (GAP-1308/1310), RLS multi-tenant, role-guard | ✅ shipped |
| Transport | TLS — Phase 4 deploy (AWS ACM) | gated |
| Secrets | AWS Secrets Manager, 90d rotation (IaC) | ✅ IaC |
| Audit logging | immutable admin_audit_logs (PDPL Art 11) | ✅ shipped |
| Dependency scan | Trivy SARIF gate | ✅ CI |
| Secret scan | CI secret-scanning | ✅ CI |

> TBD (Phase 4 — AWS live): TLS cert, CloudTrail verify, IAM least-privilege apply (stack stopped).

## 3. Compliance alignment

- **PDPL L1:** consent, data-subject rights, breach notification (cross-ref `incident-response-breach-notification.md`).
- **Cybersecurity Law 2018 + NĐ 53/2022:** data localization VN, an toàn hệ thống.

> TBD (Phase 2+ — legal): xác định nghĩa vụ localization (lưu trữ dữ liệu người dùng VN trong nước).

## 4. Vulnerability management

- Pen-test light (OWASP Top 10) — GAP-406 DONE.
- Audit cadence: post-wave security-audit /100 (per `output-review-mandate.md`).
- Disclosure: security@ contact — TBD responsible-disclosure policy Phase 2.

## 5. Out of Scope (this skeleton)

- Architecture detail (kept internal — exploit risk)
- Third-party certification (SOC2/ISO27001 — Phase 3+ maturity)
- Penetration test full report (internal `04-quality/audits/security/`)

## 6. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P2 batch — Q. Security Posture Summary). Control summary + compliance + vuln-mgmt structure; references security-audit baseline (refresh in progress). Cites Cybersecurity Law + PDPL L1 + NĐ 53/2022.
