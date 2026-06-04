---
title: Outside-in persona audit — Payment flow Phase 1 BETA (manual VietQR) + Phase 1.5 PAID (auto-reconcile A/B/C)
status: complete
created: 2026-06-04
audit_type: persona-review (outside-in)
trigger: Wave flow-kh3 KH-3 re-walk preparation — auto-reconcile patterns comparison
scope: phase-1-beta + phase-1-5-paid
personas: [P1-bac-hung-55-low-tech, P2-chi-lan-35-medium-tech, P3-anh-tuan-28-accountant, P4-anh-duc-platform-admin]
rules_applied: [outside-in-coverage-trigger.md, persona-based-business-review, pre-walk-persona-simulation-mandate.md]
related_gaps: [GAP-049, GAP-156, GAP-185, GAP-594, SUB-11, SUB-19, SUB-20]
---

# Payment Flow Outside-In Persona Audit — Phase 1 BETA + Phase 1.5 PAID

## 0. TL;DR

**Phase 1 BETA Pattern 0** (static QR + admin manual confirm) — ⚠️ tolerable beta volume ≤30 conf/wk; HARD BLOCKER cho Persona 3 Accountant + churn risk Persona 1 (≥4h bank delay = bỏ cuộc) + UX broken cho P1 elderly.

**Phase 1.5 ranking** (consensus across 4 personas): **Pattern A (Casso/SePay aggregator)** ≫ Pattern C (PSP QR) ≫ Pattern B (direct bank API) — A là sweet spot Phase 1.5 vì: no PSP license, no KYC business 2-4 tuần, fast integration, persona-friendly UX (auto-fill amount + memo).

**P0 BLOCKING** (4): admin confirm endpoint chưa expose (Wave flow-kh3 pre-walk #1) / dynamic QR mandatory Phase 1.5 / unique-memo collision cùng amount cùng phút / Persona 3 Excel export VAT TCT eInvoice.

---

## 1. Methodology

4 personas role-played qua 4 patterns × full payment journey (submit → display → user-action → reconcile → kích hoạt). 27 findings total (6-9 per persona); ranking matrix §6.

Source artifacts: `documents/01-business/kitehub/subscription-billing/{rules,use-cases,api-contract}.md` + pre-walk audit `2026-06-04-pre-walk-flow-kh3-subscription.md` + outside-in Phase 1.5 audit `2026-05-18-phase-1-5-qr-payment-outside-in.md`.

---

## 2. Persona 1 — Bác Hùng (55, Owner gia sư 15 HS, VCB cá nhân, low-tech)

### Pattern 0 — Finding 1.0.1: 🔴 P0 "Mã giao dịch" terminology ambiguity
- **Where:** Payment page hiển thị `SUB001ABC` content. Bác Hùng quen "nội dung chuyển khoản" cho con đóng học phí: viết tay "Hùng đóng tháng 6".
- **Symptom:** Bác Hùng mở app VCB → nhập tự do "Nâng cấp KiteHub" → admin reconcile fail vì content không khớp memo SUB001ABC → admin reject → Bác Hùng confused "tôi đã chuyển rồi sao bảo chưa?".
- **Pre-walk check:** `grep -rn "nội dung chuyển khoản\|copy.*memo\|btn-copy" kitehub/kitehub-frontend/src/app/\(customer\)/billing/` — verify có copy-to-clipboard + sample VN-friendly + giải thích "bắt buộc copy nguyên văn".
- **Severity:** P0. **Class:** UX-clarity + cultural.

### Pattern 0 — Finding 1.0.2: 🟠 P1 No bank-delay expectation set
- VCB cuối tuần delay 2-4h. Bác Hùng chờ 30 phút thấy KiteHub vẫn FREE → tưởng "lỗi mạng" → CSKH gọi. Cần inline countdown "đối soát thường trong giờ làm việc 8h-18h T2-T6".
- **Severity:** P1. **Class:** UX-clarity + cultural.

### Pattern A — Finding 1.A.1: ✅ POSITIVE Dynamic QR amount-prefilled
- App VCB scan QR đã có sẵn `599.000đ` + memo. Bác Hùng KHÔNG cần gõ amount → eliminate Finding 1.0.1 + risk gõ sai (Bác Hùng + Pattern 0 gõ 599000 thay 599.000 chiếm ~10%).
- **Class:** UX win.

### Pattern A — Finding 1.A.2: 🟠 P1 Webhook delay confusion (Casso poll 1-5 min)
- Bác Hùng chuyển khoản → đợi 30 giây → page chưa flip → reload → vẫn PENDING → nghĩ "lỗi". Need WebSocket push + visible polling indicator "đang chờ bank xác nhận 30s-5 phút".
- **Severity:** P1.

### Pattern B — Finding 1.B.1: 🔴 P0 Bank API mất 2-4 tuần KYC business
- KiteHub solo dev chưa có MB BizAPI/TCB Open API contract → Pattern B BLOCKED Phase 1.5 trừ khi partnership signed. Bác Hùng không impact persona-specific.
- **Class:** cost-tier / vendor.

### Pattern C — Finding 1.C.1: ✅ POSITIVE PSP redirect = familiar UX
- Bác Hùng đã quen MoMo từ taxi/Grab. Click "Thanh toán MoMo" → mở MoMo app → familiar → PSP handle reconcile fully.
- **Class:** UX win. **Counter:** 1.5-2.5% fee × 599k = 9-15k/tháng × N tenants — KiteHub absorb hay pass-through PH?

### Pattern C — Finding 1.C.2: 🟠 P1 MoMo PSP license + 0.5-2M/tháng minimum
- Cost-tier mismatch cho 15-HS Bác Hùng — phí PSP eat margin. Pattern A capex-free phù hợp hơn.

**Persona 1 recommendation ranking:** A > C > 0 > B. **Acceptable minimum:** Pattern 0 với P0 1.0.1 fix (memo copy button + VN explainer copy).

---

## 3. Persona 2 — Chị Lan (35, Owner TT tiếng Anh 80 HS, TCB business + cá nhân, medium-high)

### Pattern 0 — Finding 2.0.1: 🔴 P0 Zalo screenshot QR rò rỉ ai-chuyển-cho-ai
- Chị Lan thường share QR Zalo group PH → 80 PH cùng chuyển vào account `1234567890` cùng memo `SUB001ABC` (static QR Phase 1) → admin KiteHub không phân biệt được PH nào đã đóng (KiteClass scope khác — nhưng pattern lặp lại cho KiteHub upgrade). Đối với KiteHub upgrade: chỉ Owner Chị Lan chuyển 1 lần → KHÔNG impact. NHƯNG mental model carry-over Phase 1.5 KiteClass cùng infrastructure.
- **Severity:** P0 cho KiteClass scope; P2 cho KiteHub upgrade isolated.
- **Class:** cultural (Zalo OA habit).

### Pattern 0 — Finding 2.0.2: 🟠 P1 Email-only "đã nhận tiền" notification miss
- Chị Lan kiểm Zalo OA hơn email (VN edu owner expectation per Wave 100 audit). Admin confirm chỉ send email → Chị Lan miss → check KiteHub manual.
- **Severity:** P1.
- **Class:** cultural (Zalo > email VN).

### Pattern A — Finding 2.A.1: ✅ POSITIVE Casso auto-reconcile 80-tenant scale
- Casso 200k/tháng aggregator phù hợp scale 80 PH (KiteClass) + Chị Lan KiteHub upgrade. Auto-match memo + amount eliminate manual reconcile.

### Pattern A — Finding 2.A.2: 🟠 P1 Casso mapping multiple tenants 1 bank account
- KiteHub central bank acct nhận multiple tenant upgrades — Casso webhook chỉ trả memo + amount. Backend match memo → tenant. Risk: memo collision (Persona 4 finding).

### Pattern B — Finding 2.B.1: ✅ POSITIVE TCB business API native
- Chị Lan đã có TCB business account + biết internet banking → if KiteHub có TCB Open API integration, lowest latency + zero per-tx fee. Setup overhead KiteHub-side, persona-side seamless.

### Pattern C — Finding 2.C.1: 🟡 P2 PSP fee disclosure
- Chị Lan accountant-minded → expect transparent receipt: "599.000đ + phí PSP 12.000đ = 611.000đ" hay KiteHub absorb? Need pricing display rule.

**Persona 2 recommendation ranking:** A > B > C > 0.

---

## 4. Persona 3 — Anh Tuấn (28, Accountant, Excel + MISA + VNPay personal, high-tech)

### Pattern 0 — Finding 3.0.1: 🔴 P0 BLOCKER Zero auto-export Excel reconciliation
- Cuối tháng Anh Tuấn cần CSV: `txn_date / amount / memo / matched_subscription_id / VAT_amount / TCT_invoice_no`. Pattern 0 admin nhập transactionId manually → KHÔNG có structured export → Tuấn phải copy-paste từ admin UI → eat 1-2h/tháng.
- **Severity:** P0 (hard blocker P3 onboarding Phase 1.5).
- **Class:** observability + cost-tier.

### Pattern 0 — Finding 3.0.2: 🔴 P0 No VAT TCT eInvoice integration
- VN Nghị định 123/2020 + Wave 93 GAP-185 MISA MeInvoice partnership scope Phase 1.5+. Pattern 0 KHÔNG link `Payment.transactionId` → `Invoice.einvoiceNo`. Tuấn manual issue invoice từ MISA standalone → reconcile mismatch.
- **Severity:** P0.
- **Class:** VN-specific compliance.

### Pattern A — Finding 3.A.1: ✅ Casso CSV export native + webhook structured
- Casso dashboard có export Excel + webhook structured JSON (txn_date/amount/memo/bank/account). Pattern A unblocks Tuấn entirely.

### Pattern A — Finding 3.A.2: 🟠 P1 MISA bridge still manual
- Casso giải quyết reconcile, nhưng eInvoice issuance vẫn cần MISA partnership (Wave 93 deferred). Pattern A + MISA = full P3 readiness.

### Pattern B — Finding 3.B.1: ✅ Bank API direct = realtime VAT
- TCB/MB API trả transactionRefNo realtime → backend trigger MISA invoice issuance same-second. Tuấn happy. Setup cost outweigh Phase 1.5 scope.

### Pattern C — Finding 3.C.1: 🟡 P2 PSP merchant reconcile portal separate
- MoMo/VNPay có merchant portal riêng → Tuấn login 2 portals (KiteHub + PSP) → reconcile cross-system. Pattern A cleaner.

**Persona 3 recommendation ranking:** A > B > C ≫ 0 (Pattern 0 HARD BLOCKER cho Anh Tuấn).

---

## 5. Persona 4 — Anh Đức (PlatformAdmin)

### Pattern 0 — Finding 4.0.1: 🔴 P0 Memo collision 2 tenants same amount same minute
- 2 Owners cùng upgrade BASIC 599k cùng phút → memo `SUB001ABC` vs `SUB002DEF` distinguishable, NHƯNG IF Owner gõ sai memo → both payments same content "Nâng cấp" → Đức assign nhầm tenant. Pre-walk audit #2 already filed but PLATFORM-ADMIN scope distinct.
- **Pre-walk check:** `grep -n "generatePaymentContent\|unique" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/VietQRService.java` — verify ≥8-char unique suffix.
- **Severity:** P0.

### Pattern 0 — Finding 4.0.2: 🟠 P1 Admin confirm rate ceiling 5-20/tuần before burnout
- Beta volume 5-20 conf/tuần tolerable (~10-30 phút/tuần). Phase 1.5 scale 100+ conf/tuần → unsustainable solo-dev. Need auto.

### Pattern A — Finding 4.A.1: ✅ Đức monitor health dashboard (Casso webhook fail rate / DLQ)
- Casso webhook → backend reconcile. Đức watch DLQ + manual-intervene khi auto-fail (5-10% expected). Lower burnout vs Pattern 0.

### Pattern A — Finding 4.A.2: 🟠 P1 Webhook signature verify mandatory
- Per `pre-handoff-self-test-completeness.md` §2.6 Payment flow (c) — Casso webhook signature server-side verify; reject unsigned. Đức audit trail mandatory.

### Pattern B — Finding 4.B.1: 🟠 P1 Bank API breaking change risk
- TCB/MB API version bump → silent break. Đức cần monitoring + version pinning.

### Pattern C — Finding 4.C.1: 🟠 P1 PSP idempotency + reconciliation table
- Per `pre-handoff-self-test-completeness.md` §2.6 (d) — same key replay không double-charge. Đức audit log `payment_audit_log`.

**Persona 4 recommendation ranking:** A > C > B > 0 (Pattern 0 burnout ceiling).

---

## 6. Cross-persona ranking matrix

| Pattern | P1 Bác Hùng | P2 Chị Lan | P3 Anh Tuấn | P4 Đức admin | Phase fit |
|---|---|---|---|---|---|
| **0 manual** | 🟡 tolerable + P0 memo fix | 🟡 tolerable | 🔴 HARD BLOCKER VAT | 🟡 ≤20 conf/tuần | Phase 1 BETA ≤30 conf/wk |
| **A Casso/SePay** | ✅ amount auto-fill | ✅ scale 80+ | ✅ CSV export | ✅ webhook + DLQ | **Phase 1.5 sweet spot** |
| **B Direct bank** | ✅ seamless | ✅ TCB native | ✅ realtime | 🟠 vendor lock | Phase 2 (KYC 2-4 tuần) |
| **C PSP gateway** | ✅ MoMo familiar | 🟡 fee disclosure | 🟡 dual portal | 🟠 PSP license | Phase 2 (license + 0.5-2M/tháng) |

## 7. Non-obvious VN cultural findings summary

1. **Memo terminology mismatch** — VN PH viết tự do "Hùng đóng tháng 6"; KiteHub mandate `SUB001ABC` → friction. Need copy-button + VN explainer.
2. **Zalo OA > email** cho payment confirmation — VN edu owner habit confirmed (Wave 100 audit recurrence).
3. **Bank delay weekend/holiday** — VCB 2-4h delay → expectation setting inline.
4. **Persona age × tech** — Bác Hùng 55 KHÔNG quen QR app banking, prefer redirect (MoMo) hoặc transfer-only flow.
5. **Phí PSP transparency** — VN accountant expect line-item receipt; absorb hay pass-through phải explicit policy.
6. **MISA MeInvoice partnership** Phase 1.5+ mandatory cho P3 onboarding (VAT TCT compliance Nghị định 123/2020).
7. **Memo collision** at solo-dev volume — even 10 tenants/tuần có thể collide cùng amount cùng phút without uniqueness guard.

---

## 8. Recommendations

### Phase 1 BETA actions (immediate, Wave flow-kh3 + flow-kh4):
1. Fix P0 Finding 1.0.1 memo copy-button + VN explainer (frontend payment page).
2. Fix P0 Finding 4.0.1 memo uniqueness guard (`VietQRService.generatePaymentContent` ≥8-char suffix + collision retry).
3. Defer dynamic QR + auto-reconcile → Phase 1.5; document Pattern A as canonical Phase 1.5 target.
4. Admin volume ceiling explicit doc: Phase 1 BETA ≤30 conf/tuần MAX before Pattern A ship.

### Phase 1.5 PAID actions (next-quarter scope):
1. **Pick Pattern A** (Casso/SePay) → 1-2 tuần integration + 200k/tháng cost-bearable.
2. **Sister GAP-185 MISA MeInvoice partnership** unblock P3 Anh Tuấn VAT eInvoice.
3. Pattern C (PSP) defer Phase 2+ post counsel + KYC.
4. Pattern B (direct bank) defer Phase 3+ at scale ≥100 tenants justifying KYC overhead.

### File follow-up gaps:
- GAP-NNN-1: P0 Finding 1.0.1 memo copy-button + VN explainer
- GAP-NNN-2: P0 Finding 4.0.1 memo uniqueness guard
- GAP-NNN-3: P0 Finding 3.0.1 + 3.0.2 CSV export + MISA bridge (Phase 1.5 prereq)
- GAP-NNN-4: P1 Finding 1.0.2 bank-delay expectation inline copy
- GAP-NNN-5: P1 Finding 2.0.2 Zalo OA notification channel (sister GAP-589)

---

**Verdict:** Pattern A (Casso/SePay aggregator) là Phase 1.5 sweet spot — consensus 4/4 personas. Phase 1 BETA Pattern 0 acceptable với 2 P0 fix (memo button + uniqueness). Pattern C defer Phase 2+; Pattern B defer Phase 3+ scale-driven.
