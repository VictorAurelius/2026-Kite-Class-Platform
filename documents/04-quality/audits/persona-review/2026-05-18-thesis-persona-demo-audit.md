---
title: Outside-in Audit — Thesis Demo Persona Walkthrough (P1 + P2)
status: complete
created: 2026-05-18
audit_type: persona-simulation
agent_model: sonnet-4-6
trigger: Release 1.5 thesis scope decision per outside-in-coverage-trigger.md
scope: 15-min thesis defense demo evaluation cho 2 personas (P1 Solo Teacher + P2 Center Owner)
---

# Outside-in Audit — Thesis Demo Persona Walkthrough

**Mục tiêu:** Đánh giá xem 2 personas P1 + P2 có thể demo end-to-end trong 15-min thesis defense không, surface BLOCKING vs NICE-TO-HAVE gaps.

## Persona 1: P1 Solo Teacher — Cô Linh (28t, dạy IELTS freelance)

| Bước demo | Demo-readiness | Visual | Failure recovery | Story arc | Thời gian |
|---|:---:|:---:|:---:|:---:|:---:|
| Signup (email + OTP) | 🔴 RED — GAP-286 OTP Zalo/SMS OPEN P0 | ⚠️ chưa styled | Pivot sang password-only | Multi-tenant SaaS hook | 1.5 phút |
| Onboarding + branding wizard | 🔴 RED — GAP-287 skip/default OPEN P0 | ⚠️ wizard blocker | Bỏ bước nếu blocked | Persona-based design | 1.5 phút |
| Tạo lớp học | 🟡 YELLOW — endpoint BE có, FE GAP-274/275 OPEN | ⚠️ form cơ bản | Chuẩn bị sẵn demo state | SaaS feature | 2 phút |
| Enroll học sinh | 🟡 YELLOW — GAP-233 API contract drift P0 | ⚠️ basic list | Seed data trước | Real product usage | 1.5 phút |
| Điểm danh | 🟡 YELLOW — GAP-294 NO_SHOW status OPEN | ⚠️ UI tồn tại | OK với PRESENT/ABSENT | Core education workflow | 2 phút |
| Xuất hoá đơn | 🔴 RED — GAP-297 batch invoice UX OPEN P0 | ❌ chưa có UI | Skip sang show DB record | Revenue model | 2 phút |
| Dashboard doanh thu | 🔴 RED — GAP-293 income dashboard OPEN | ❌ chưa có | Show số tĩnh | SaaS value proposition | 1.5 phút |

**Top 3 BLOCKING gaps** (phải fix trước demo):
1. **GAP-297** — Batch Monthly Invoice Generation UX: không có giao diện tạo hoá đơn → thesis mất đi điểm mạnh nhất (revenue management)
2. **GAP-287** — Branding wizard skip: demo kẹt ngay bước onboarding → ấn tượng đầu tiên thất bại
3. **GAP-293** — Income summary dashboard: không có UI → "SaaS revenue platform" không thể demo được

**Top 3 NICE-TO-HAVE:**
1. **GAP-288** — Onboarding tour 5-feature highlight
2. **GAP-294** — NO_SHOW attendance status
3. **GAP-292** — Per-session pricing (200K/buổi)

**Phân bổ thời gian:** 12 phút P1 journey + 3 phút buffer Q&A.

## Persona 2: P2 Center Owner — Anh Hùng (35t, 1 trung tâm, 3 NV, 80 HS)

| Bước demo | Demo-readiness | Visual | Failure recovery | Story arc | Thời gian |
|---|:---:|:---:|:---:|:---:|:---:|
| Signup + AI branding | 🔴 RED — GAP-287 wizard block, AI gen chưa polished | ⚠️ | Pre-generate logo | Multi-tenant differentiation | 2 phút |
| Invite staff / Manager | 🟡 YELLOW — invite flow có, GAP-562 RBAC P0 PARTIAL | ⚠️ role mismatch | Show email invite only | SaaS multi-role | 1.5 phút |
| Tạo nhiều lớp + phân công | 🟡 YELLOW — GAP-137 bulk import DONE | ✅ upload CSV đẹp | Fallback manual | Scale narrative | 2 phút |
| Monthly billing cycle | 🔴 RED — GAP-297 batch invoice OPEN, GAP-292 pricing OPEN | ❌ | Show concept only | Business model | 2 phút |
| AI Branding generation | 🟡 YELLOW — logo upload + color picker có; full AI gen PARTIAL | ⚠️ basic UI | Demo logo upload | AI integration thesis hook | 1.5 phút |

**Top 3 BLOCKING gaps:**
1. **GAP-287** — Branding wizard skip/default (P2 USP)
2. **GAP-562** — RBAC Manager visibility leak (live demo bug risk)
3. **GAP-297** — Batch invoice (revenue model proof)

**Top 3 NICE-TO-HAVE:**
1. **GAP-293** — Income dashboard
2. **GAP-636** — Casso/SePay webhook (VN-market fit narrative)
3. **GAP-288** — Onboarding tour Manager variant

## Cross-Cutting Scope — Release 1.5 Thesis

**P0 BLOCKING (7 items):**
1. GAP-287 — Branding wizard skip/default — ảnh hưởng CẢ HAI persona
2. GAP-297 — Batch monthly invoice UX — core demo hook
3. GAP-293 — Monthly income summary dashboard — upgrade thesis priority
4. GAP-562 — RBAC role separation PARTIAL — bug visible trước hội đồng
5. GAP-518 — BE seed PLATFORM_ADMIN vs FE guard ADMIN mismatch PARTIAL — admin demo broken
6. GAP-286 — Mobile OTP Zalo/SMS OR switch email-only flow — signup gate
7. GAP-538 — Day-1 onboarding checklist + demo data seed PARTIAL — empty state risk

**P1 IF TIME (4 items):**
1. GAP-288 — Onboarding tour
2. GAP-292 — Per-session pricing model
3. GAP-294 — NO_SHOW attendance status
4. GAP-636 — Casso/SePay webhook investigation

**Items NÊN DROP khỏi thesis scope:**
- GAP-182/184/185/186 — Legal docs (counsel-pending, không demo)
- GAP-117 — Restore drill (ops infra, no visual impact)
- GAP-216/217/218 — PDF benchmark / alert rules / font runbook (invisible)
- GAP-203 — CVE deps fix (security hygiene, không thesis narrative)
- GAP-634 — MISA MeInvoice partnership (Phase 1.5b, too complex)

## Recommendation

**Scope estimate:** 7 P0 BLOCKING + 2-3 P1 IF TIME = ~9-10 gaps cần đóng cho thesis-ready state. Wave-pack methodology (3-5 agents parallel) = ~2-3 waves (~2-3 tuần active dev).

**Wall-clock đến thesis-ready:** Nếu bắt đầu 2026-05-18, ước tính **3-4 tuần** cho demo-ready milestone. Thesis demo không cần production-grade deploy — chỉ local hoặc staging instance với demo data seeded, OTP bypass cho demo env.

**2 rủi ro chiến lược:**
1. **P2 onboarding wizard không styled = ấn tượng đầu tiên thất bại** — Hội đồng xem demo 15 phút, ấn tượng trực quan quyết định 40% đánh giá. GAP-287 (wizard blocked) + GAP-538 (empty state) = "cold start problem" phút đầu.
2. **Billing demo không có UI = thesis narrative về "SaaS revenue platform" mất điểm mạnh nhất** — Toàn bộ thesis argument xoay quanh multi-tenant + AI branding + revenue management. Nếu GAP-297 + GAP-293 vẫn OPEN, demo chỉ show class management + attendance — mất differentiator SaaS. Hội đồng sẽ hỏi "payment ở đâu?" và "phase 1.5" không thuyết phục ở bảo vệ.
