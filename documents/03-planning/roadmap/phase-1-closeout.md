# Phase 1 BETA Close-Out — campaign tracker (lean)

**Created:** 2026-06-19 · **Mode:** gate-driven, fast/lean, docs xúc tích (no per-gap brainstorm/TDD ceremony; batch walks per `feature-ship-runtime-walk-mandate` §3.4).
**Goal:** đạt cổng Phase 1→2 (Quality audit ≥80 + 5 beta tenant live + 0 P0 incident 2 tuần) nhanh nhất — KHÔNG fix cả 298 gap.

## Landscape (sau reclassification wave-gap-audit-p1-1, #2502)
- 298 active phase-1-beta. **Gate-critical = 126** (26 P0 + 100 P1-launch-critical). 75 P1 non-critical + 88 P2 + 9 P3 → ride/defer.
- Gate-critical chia: **97 LOCAL-closable** (no cost) · **20 AWS-gated** · **9 vendor-gated**.

## 3 tracks
| Track | Scope | Trạng thái |
|---|---|---|
| 🟢 LOCAL | 97 (code/local-walk/docs) | **ACTIVE** — không tốn tiền |
| 🔴 AWS-gated | 20 (terraform/EC2/RDS/IAM/SES/deploy/restore + "5 beta tenant live" gate cond) | BLOCKED — chờ quyết restore AWS (cost) |
| 🟡 Vendor-gated | 9 (Resend/SePay/Zalo sandbox) | BLOCKED — chờ sandbox/tunnel |

> **Sự thật:** cổng "5 beta tenant live" + 20 P0/P1 ops **bắt buộc AWS stack** (đã teardown). Không thể *đóng thật* Phase 1 mà không redeploy AWS.

## Wave 1 — 5 P0 local-walk (ACTIVE)
Code đã ship 80-95%, cần runtime walk verify để flip DONE (per `feature-ship-runtime-walk-mandate` + `g1-browser-walk-before-flip`):
- GAP-1066 — attendance status normalize (V87 chk uppercase)
- GAP-1115 — LMS paywall bypass (getCourseStructureForStudent)
- GAP-1139 — KC OWNER tenant-admin 403
- GAP-1213 — wizard deploy mock→KC theme propagation
- GAP-1308 — gateway strip X-User-Roles (role-spoof)

Steps: stack up (`up.sh`) → seed (`seed-walk-tenant.sh`) → pre-walk persona sim (done) → browser walk (catalog-then-batch) → flip DONE.

## Wave 2+ (queued, LOCAL)
- 2 P0 doc-umbrella: GAP-049 (business-logic correctness), GAP-154 (22 BRD docs).
- 89 P1-launch-critical (code agents + walks), batched by domain.

## Blocked-track notes
- GAP-502 (kh_backend prod thrashing) → AWS-gated (moved out of Wave 1).
- AWS/vendor tracks resume on redeploy decision.
