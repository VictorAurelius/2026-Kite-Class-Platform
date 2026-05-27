---
audience: dev
---

# GAP-765 — Beta request POST 201 nhưng không có confirmation email

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (cần verify design intent — có thể downgrade P2 nếu by-design)
**Domain:** Backend (subscription + email service)
**Found:** 2026-05-27 (Wave 106 RST Mảng A2)
**Affects:** Beta access request UX — user submits form nhưng không nhận confirmation
**Phase:** phase-1-beta

## Problem

Plan Đợt 106 §3 row A2 expect: "Biểu mẫu yêu cầu beta — **gửi thành công + nhận thư xác nhận**".

Reality:
- POST `/api/v1/auth/request-beta-access` returns HTTP 201 + `status: PENDING` row trong DB ✅
- MailHog inbox (port 8025) check sau POST: **0 messages** cho recipient email ❌

```bash
curl -s http://localhost:8025/api/v2/messages | jq '.items[].To[].Mailbox'
# Returns: 'test+smoke', 'admin', 'admin' — NO message cho hong.tran-...@skyedu.vn
```

## Possible interpretations

**Interpretation A: By-design (downgrade P2)**
- Confirmation email chỉ gửi khi admin approve PENDING → APPROVED
- Acceptable nếu UX flow expected là "submitted, we'll review and email you"
- Cần landing copy explicit ("Chúng tôi sẽ phản hồi qua email trong vòng X giờ")

**Interpretation B: Bug (P1)**
- Plan expect immediate "request received" confirmation email
- User submits form → không nhận email → uncertain whether submission successful → support burden
- Industry standard: SaaS signup → immediate confirmation email (Stripe / Linear / Notion / Airtable all send)

## Proposed Fix (depending on interpretation)

**Path A (downgrade P2):** Update FE UI to show toast/banner "Yêu cầu đã gửi! Chúng tôi sẽ phản hồi qua email hong@example.com trong vòng 24-48h." sau POST 201. Update plan Đợt 106 §3 A2 expectation.

**Path B (P1 fix):** Wire `kitehub-subscription` AuthService.requestBetaAccess() to trigger `kitehub-email` send confirmation email với template "request-received.html" (tiếng Việt, persona greeting Owner formal `Em chào chị/anh`). Send synchronously OR via outbox.

## Acceptance Criteria

- [ ] Decision: A (by-design) hay B (bug)
- [ ] Nếu A: FE post-submit UX updated + plan Đợt 106 §3 expectation reframe
- [ ] Nếu B: confirmation email shipped + MailHog smoke test pass + E2E spec paired

## Related

- Wave 106 plan §3 row A2
- `kitehub-email` template scope: `kitehub/kitehub-email/src/main/resources/templates/emails/`
- Rule: `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate (nếu Path B fix)
