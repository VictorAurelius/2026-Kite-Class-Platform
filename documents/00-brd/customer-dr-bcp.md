# Customer-facing DR / BCP (Khôi phục thảm họa & Liên tục kinh doanh) — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** SRE + PM
**Reviewer:** Tech Lead + Legal counsel
**Legal basis:** **Nghị định 13/2023/NĐ-CP (PDPL — L1)** Art về bảo đảm an toàn dữ liệu cá nhân; **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (L3)** minh bạch khả năng phục hồi dịch vụ
**Related:** [`nfr-catalog.md`](nfr-catalog.md) §3 (RTO/RPO) · [`incident-response-breach-notification.md`](incident-response-breach-notification.md) · [`customer-sla-uptime.md`](customer-sla-uptime.md) · [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md)

---

## 1. Phạm vi & mục đích

Bản tóm tắt hướng-khách-hàng về năng lực **Disaster Recovery (DR)** và **Business Continuity Plan (BCP)** — backup cadence, RTO/RPO cam kết, kịch bản khôi phục. Chi tiết kỹ thuật runbook nằm ở [`nfr-catalog.md`](nfr-catalog.md) §3 + ops runbooks; doc này là tóm tắt minh bạch cho khách hàng + enterprise due-diligence.

Skeleton Phase 1: khung DR/BCP; **RTO/RPO cam kết + kết quả restore-drill cần SRE Phase 2** (restore drill carry GAP-257) — KHÔNG fabricate số liệu recovery.

---

## 2. RTO / RPO Commitment

| Chỉ số | Mục tiêu nội bộ (nfr §3) | Cam kết khách hàng |
|---|:---:|:---:|
| RTO (Recovery Time Objective) | TBD | TBD ≥ internal target |
| RPO (Recovery Point Objective) | TBD | TBD |

> TBD (Phase 2 — SRE, gated GAP-257 restore drill): RTO/RPO chỉ cam kết sau khi restore-drill thực tế đo lường + verify backup khôi phục được.

## 3. Backup strategy

- RDS automated snapshot + retention — TBD cadence (ref ops backup runbook).
- Cross-AZ / cross-region — Phase 1 single-region ap-southeast-1 per ADR-025; multi-region defer Phase 4 deploy.
- Backup integrity: restore-drill cadence — GAP-257 (P0 ops carry, gated).

## 4. Continuity scenarios

| Kịch bản | Phản ứng | Trạng thái |
|---|---|:---:|
| EC2/instance failure | dynamic re-launch (deploy-production) | code-ready |
| RDS failure | snapshot restore | TBD drill |
| Region outage | manual failover | Phase 4 |
| Data corruption | point-in-time restore | TBD drill |

## 5. Tuân thủ pháp lý

- **PDPL L1:** bảo đảm an toàn + khả năng phục hồi dữ liệu cá nhân (học sinh, phụ huynh).
- **L3:** minh bạch năng lực khôi phục cho khách hàng trả phí.

> TBD (Phase 2 — legal): nghĩa vụ thông báo khách hàng khi mất dữ liệu; cross-ref breach-notification.

## 6. Out of Scope (this skeleton)

- RTO/RPO numbers (Phase 2 — SRE post restore-drill GAP-257)
- Multi-region failover design (Phase 4 deploy)
- Technical restore runbook (lives in ops guides)

## 7. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P2 batch — I. Customer DR/BCP). RTO/RPO + backup + continuity-scenario structure; all recovery numbers TBD Phase 2 (gated GAP-257 restore drill). Cites PDPL L1 + Consumer Protection L3.
