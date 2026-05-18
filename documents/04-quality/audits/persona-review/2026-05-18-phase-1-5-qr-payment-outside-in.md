---
title: Outside-in audit — Phase 1.5 QR payment proposal (P1 solo teacher + P2 small center)
status: complete
created: 2026-05-18
audit_type: persona-review + benchmark + failure-mode (3-agent outside-in)
trigger: user inside-out proposal 2026-05-18 — "cho phép Owner edit QR thay payment processor"
scope: phase-1.5-paid
personas: [P1-solo-teacher, P2-small-center-owner-under-50-hs]
rules_applied: [outside-in-coverage-trigger.md, audit-to-gap-pipeline.md §2.5, incident-to-rule-pipeline.md]
related_gaps: [GAP-108, GAP-183, GAP-185, GAP-594, GAP-625-..-GAP-635]
---

# Outside-in audit — Phase 1.5 QR payment proposal

## 0. TL;DR

**Verdict:** ✅ **PROCEED with QR approach** cho cả P1 + P2 Phase 1.5 — KHÔNG phải shortcut mà là **mandatory** do compliance constraint VN (PSP license + KYC merchant onboarding). 3 outside-in agents độc lập đều converge cùng recommendation.

**Conditions:**
- 3 P0 gaps **MUST close trước Phase 1.5 launch** (KYC + multi-tenant QR binding + PDPL transaction PII)
- 5 P1 gaps close trong 3 tháng beta đầu
- Pivot Phase 2 sang **VietQR EduPay (NAPAS) partnership** + **MISA MeInvoice partnership** khi PH count > 100 — KiteHub vẫn KHÔNG broker tiền

**Revenue model implication:** Pure SaaS subscription. KHÔNG take % per-transaction. Match industry norm (Easy Edu, DotB, Mona, VnResource, Faceworks).

---

## 1. Trigger + methodology

**Inside-out proposal (user):** 2026-05-18 — "Cho phép owner có role hợp lý được phép chỉnh sửa mã QR nhận tiền học phí, thay vì thiết lập 1 hệ thống thanh toán phức tạp cho kiteclass — chỉ áp dụng cho đối tượng giáo viên đơn lẻ?"

**Outside-in trigger fired per `.claude/rules/outside-in-coverage-trigger.md` §1** — bucket-internal scope refinement cho Phase 1.5 payment. User confirmed "Tất cả 3 parallel".

**3 agents spawned background:**
1. **Persona walkthrough** (Explore agent) — role-play P1 thầy Tâm + P2 chị Hằng qua 6 scenarios
2. **External benchmark** (general-purpose + WebSearch) — 7 VN edu SaaS competitors
3. **Failure-mode matrix** (Explore agent) — 3-axis (Actor × Class × Phase) ≥20 scenarios

Audit completed 2026-05-18. Total wall-clock ~25 min.

---

## 2. Agent findings summary

### 2.1 Persona walkthrough verdict

**P1 — Thầy Tâm (solo teacher, 15-20 HS):** 🟡 CONDITIONAL OK
- 20-30 manual marks/tháng = borderline tolerable
- Single QR cá nhân = ownership clear, no multi-tenant ambiguity
- Friction tolerable IF: invoice template clear + SMS reminder hook + memo field tracking

**P2 — Chị Hằng (small center, ~30 HS × 3 GV):** 🔴 BLOCKER
- 90+ manual marks/tháng = 2-4h admin/tháng — NOT viable for growth
- Multiple QR (3 GV × accounts) = ownership ambiguity catastrophic khi GV nghỉ
- Zero batch reconcile, zero wrong-payment detection
- Conflict với "Enrollment + payment collection" key need

**TOP 3 UX friction:**
1. Zero reconciliation workflow (90+ clicks/tháng P2)
2. No payment-amount mismatch detection (PH chuyển 1.45M thay vì 1.5M)
3. Mid-cycle QR change conflict (Owner đổi ngân hàng → PH cũ chuyển vào account cũ)

**TOP 3 edge cases dev miss:**
1. **QR ownership ambiguity** — personal account vs tenant operating account; GV nghỉ → tiền stuck
2. **Proof-of-payment outside KiteHub** — Owner mark "đã thu" có thể fake; no banking API verify
3. **Double-payment + refund risk** — PH chuyển 2 lần → no idempotency safeguard

### 2.2 External benchmark verdict

7 VN edu SaaS competitors surveyed:

| Competitor | Payment model | Target size | Take-rate |
|---|---|---|---|
| **MISA EMIS** | Merchant integration FULL + eInvoice MeInvoice | K-12 + trung tâm trung-lớn >100 HS | Revenue share với MSB bank |
| **VietQR EduPay** (NAPAS) | QR-only + webhook auto-reconcile | Mọi quy mô | ~0% cost-plus, không broker tiền |
| **DotB EMS** | Hybrid (phiếu thu manual + QR gateway) | Trung tâm vừa-lớn | Pure SaaS, no transaction fee |
| **Easy Edu** | Manual + QR option | 1,400+ trung tâm | Pure SaaS |
| **Mona eLMS** | Custom integration (QR cho small, merchant cho large) | Đa quy mô | Pure SaaS |
| **VnResource** | Manual + QR upload + bank statement export | Vừa-lớn | Pure SaaS |
| **Faceworks** | Manual quản lý học phí (no gateway) | Nhỏ-vừa | Pure SaaS |

**Industry verdict:**
- **80%+ VN edu SaaS dùng QR upload/manual + reconcile cho <50 HS** — INDUSTRY NORM
- **Pure SaaS subscription** là norm tuyệt đối; KHÔNG charge % transaction
- **Cutoff merchant integration:** ~100-200 HS hoặc chuỗi nhiều cơ sở
- HOCMAI/Topica là B2C marketplace ≠ B2B SaaS class với KiteHub

**TOP 3 pitfalls surface:**
1. **Chọn payment processor sớm = compliance hell** — hộ kinh doanh dạy thêm KHÔNG có MST doanh nghiệp đầy đủ → KYC merchant onboarding fail; nếu broker tiền → KiteHub thành PSP cần giấy phép NHNN trung gian thanh toán
2. **QR-only KHÔNG có webhook reconcile = manual hell scale lớn** — competitors mất khách scale 50→200 HS vì thiếu auto-reconcile
3. **eInvoice integration miss = legal risk khi P2 có MST** — bắt buộc xuất hóa đơn 10% VAT cho học phí (trừ ngoại ngữ/kỹ năng nhất định)

**Industry recommendation:** Hybrid nghiêng heavily về QR upload — P1 QR pure, P2 QR + Phase 2 webhook reconcile, P3+ VietQR EduPay partnership + MISA MeInvoice.

### 2.3 Failure-mode matrix verdict

21 failure scenarios documented across 3 axes (Actor × Class × Phase).

**TOP 5 P0/P1 failures QR CANNOT mitigate (vs full processor):**
1. **Anti-fraud owner verification** — QR không validate recipient bank account ownership
2. **Transaction audit immutability** — Owner edit "mark paid" log sau-the-fact
3. **Multi-tenant QR binding leak** — QR có thể share across tenants; PH copy nhầm
4. **PII/PDPL Art 11** — KiteHub ngầm lưu PH_name + Owner_STK không consent
5. **Reconciliation at scale** — 100 PH × 1200 txn/year không real-time

**TOP 3 QR MITIGATE TỐT HƠN processor:**
1. **Owner payment control** — đổi QR instant; processor cần 5-7 ngày approval
2. **No subscription lock-in** — PH chủ động; processor auto-renewal = 30% churn dispute
3. **Zero credential storage** — không lưu card/bank credentials → không có hack surface PCI DSS

**Recommendation:** PROCEED with QR conditional — close 3 P0 trước launch + pivot Phase 2 partnership khi PH > 100.

---

## 3. 3-agent convergence highlights

Cross-cutting findings từ ≥2 agents (confidence cao):

| Finding | Persona | Benchmark | Failure-mode |
|---|:---:|:---:|:---:|
| **P2 scale reconciliation cliff ~50-100 HS** | ✅ blocker 90 clicks/tháng | ✅ industry cutoff ~100-200 HS | ✅ P1 reconciliation scale |
| **QR ownership ambiguity catastrophic** | ✅ GV nghỉ scenario | ✅ pitfall mid-cycle change | ✅ P0 anti-fraud owner verify + multi-tenant binding |
| **PSP license risk nếu broker tiền** | — | ✅ pitfall #1 | ✅ KiteHub stay non-PSP |
| **eInvoice partnership > self-build** | — | ✅ MISA MeInvoice norm | ✅ P1 VAT tenant self-issue |
| **No idempotency = double-payment risk** | ✅ edge case dev miss | — | ✅ P1 manual-mark-paid audit |
| **Audit trail weak vs processor** | ✅ refund 6-tháng-sau | ✅ pitfall audit gap | ✅ P0 immutable log + P1 evidence storage |

---

## 4. Updated proposal (inside-out + outside-in merged)

### 4.1 Original inside-out proposal

> "Cho phép Owner upload/edit QR code thay payment processor cho P1 (giáo viên đơn lẻ); chưa rõ áp cho P2 không."

### 4.2 Refined proposal (post-audit)

**Phase 1.5 PAID payment scope:**

| Component | P1 Solo Teacher | P2 Small Center (<50 HS) |
|---|---|---|
| **Payment collection** | QR upload cá nhân (VietQR / Momo cá nhân / ZaloPay cá nhân) — manual mark "đã thu" | QR upload cá nhân/doanh nghiệp — manual mark + **batch reconcile sub-bucket** (P1 GAP) |
| **eInvoice** | N/A (hộ kinh doanh dạy thêm thường miễn VAT) | **Partnership MISA MeInvoice** — replace self-build (re-scope GAP-185) |
| **KYC** | Phone + email verify | **CMND/CCCD upload + bank account ownership verify** (P0 GAP) |
| **Audit log** | Immutable "mark paid" action log | Same + receipt screenshot hash storage (P1 GAP) |
| **Refund** | Manual transfer back (out-of-band) — KiteHub track SOP (re-scope GAP-183) | Same SOP + dispute escalation flow |
| **Multi-tenant isolation** | QR embed `tenant_id` (P0 GAP) | Same |
| **Phase 2 path** | Stay QR (industry norm cho solo teacher) | **Pivot sang VietQR EduPay (NAPAS) partnership** khi PH > 100 — webhook auto-reconcile, KiteHub stay non-PSP |

### 4.3 Revenue model implication

- ❌ **KHÔNG take % per-transaction** (Phase 1.5 + Phase 2) — vì KiteHub stay non-broker
- ✅ **Pure SaaS subscription** — match industry norm 80%+ competitors
- ✅ **Competitive edge** vs MISA EMIS: **take-rate 0%** (MISA chia revenue với MSB bank → cost cao hơn cho tenant)

### 4.4 Answer 2 câu user

| Câu hỏi user | Verdict |
|---|---|
| **QR approach hợp lý không?** | ✅ Hợp lý + mandatory do compliance VN (PSP license + KYC barrier). Industry norm 80%+. |
| **Chỉ áp cho giáo viên đơn lẻ?** | ❌ KHÔNG — áp cho CẢ P1 + P2 Phase 1.5. P3 (Phase 2) sang VietQR EduPay partnership. P1+P2 chia conditions khác nhau (P2 cần batch reconcile + KYC chặt hơn). |

---

## 5. Gap recommendations (consolidated)

### 5.1 NEW gaps — 11 items

#### 🔴 P0 — MUST close TRƯỚC Phase 1.5 launch (3)

| Gap ID | Title | Surface from |
|---|---|---|
| **GAP-625** | QR payment foundation: KYC + multi-tenant binding + immutable audit | 3 agents convergence |
| **GAP-626** | QR payment PDPL transaction PII handling + consent collection | Failure-mode #4 |
| **GAP-627** | Payment-amount mismatch detection + UI alert workflow | Persona TOP 3 friction |

#### 🟠 P1 — Close trong 3 tháng beta (5)

| Gap ID | Title | Surface from |
|---|---|---|
| **GAP-628** | QR batch reconcile API for P2 monthly closing | Persona + Benchmark |
| **GAP-629** | QR refund workflow SOP (manual out-of-band transfer tracked) | Persona + Failure-mode |
| **GAP-630** | QR evidence storage (receipt screenshot hash + metadata) | Failure-mode dispute |
| **GAP-631** | QR account-verification quarterly refresh + notification | Failure-mode owner lifecycle |
| **GAP-632** | Manual mark-paid audit trail + override approval flow | Failure-mode idempotency |

#### 🟡 P2 — Wave 35+ hoặc Phase 2 (3)

| Gap ID | Title | Surface from |
|---|---|---|
| **GAP-633** | VietQR EduPay (NAPAS) partnership investigation Phase 2 | Benchmark recommendation |
| **GAP-634** | MISA MeInvoice partnership integration cho P2 VAT | Benchmark recommendation |
| **GAP-635** | QR installment payment support (Phase 2 P3 scope) | Failure-mode |

### 5.2 Re-scope existing gaps (4)

| Gap ID | Original scope | Refined scope |
|---|---|---|
| **GAP-108** | Payment-Invoice Rules 12 config keys hardcoded | Re-scope: config cho QR display + reconcile metadata (NOT processor integration) |
| **GAP-183** | Refund + Dispute Resolution Policy | Re-scope: manual refund SOP + dispute escalation (paired GAP-629) |
| **GAP-185** | Build VAT/TCT Invoice engine self-build | Re-scope: MISA MeInvoice partnership (paired GAP-634) |
| **GAP-594** | 30-day money-back doc | Keep — align với manual refund SOP (paired GAP-629) |

### 5.3 Cancelled (planned but not yet filed)

| Gap | Reason |
|---|---|
| `GAP-NEW-payment-processor-init` | CANCEL — PSP license risk + KYC fail; defer Phase 2 partnership |

---

## 6. Phase 1.5 timeline implication

Original Phase 1.5 §4.4 deliverables targeted **5-7 BLOCKING + 4-5 STRONGLY-recommend = 9-12 gaps in 4-6 tuần**. Updated post-audit:

| Phase 1.5 sub-phase | Scope | Gaps | Trigger |
|---|---|---|---|
| **1.5a (Wave 31-32)** | P0 payment foundation | GAP-625/626/627 + existing legal gaps (180-186 lock) | Foundation block |
| **1.5b (Wave 33-34)** | P1 payment ops | GAP-628/629/630/631/632 + re-scope GAP-108/183/185/594 | Beta runtime |
| **1.5c (Wave 35)** | PUBLIC PAID LAUNCH `v1.0.0` | Final validation + Pen test + onboarding | Trigger Phase 2 |
| **Phase 2 (post-launch)** | Pivot Phase 2 partnership | GAP-633 VietQR EduPay + GAP-634 MISA MeInvoice + GAP-635 installment | PH > 100 trigger |

Quality bar Phase 1.5 unchanged: /100 ≥85 + 0 P0 incidents 4 tuần. Bar updated với 3 P0 payment gaps phải close trước launch.

---

## 7. References + audit trail

### 7.1 3 agent transcripts (verifiable)

- Persona walkthrough agent — id `a22e8469ba8bceef5` — completed 2026-05-18 (64s wall-clock, 103k tokens)
- External benchmark agent — id `a1ee5d6e141e07b42` — completed 2026-05-18 (149s wall-clock, 155k tokens)
- Failure-mode matrix agent — id `a2615874804195b90` — completed 2026-05-18 (100s wall-clock, 119k tokens)

### 7.2 Rules applied

- `.claude/rules/outside-in-coverage-trigger.md` v1.0.0 — primary trigger
- `.claude/rules/audit-to-gap-pipeline.md` v1.4.2 §2.5 — state-check before file gaps (this audit IS the state-check)
- `.claude/rules/incident-to-rule-pipeline.md` — outside-in finding documentation pattern
- `.claude/rules/meta-gap-priority.md` §3 — Business-Logic tier 2nd after Meta (payment compliance = business-logic-P0)

### 7.3 External sources (benchmark agent citations)

- MISA EMIS — https://emis.misa.vn/bao-gia/ + https://emishelp.misa.vn/kb/23745/
- DotB EMS — https://dotb.vn/pricing/ + https://dotb.vn/news/phan-mem-quan-ly-trung-tam-day-them/
- Easy Edu — https://easyedu.vn/
- Mona eLMS — https://mona.software/phan-mem-quan-ly-giao-duc
- VietQR EduPay — https://vietqr.com/edu/
- Faceworks — https://faceworks.vn/chi-tiet/thu-hoc-phi-bang-phan-mem-quan-ly-trung-tam-dao-tao/
- VnResource — https://blog.vnresource.vn/phan-mem-quan-ly-trung-tam-day-them-bi-quyet-tang-gap-doi-doanh-thu/
- eInvoice giáo dục — https://www.meinvoice.vn/tin-tuc/4543/hoa-don-hoc-phi-nganh-giao-duc/
- Hộ kinh doanh dạy thêm thuế 2025 — https://einvoice.vn/tin-tuc/nam-2025-ho-kinh-doanh-day-them-phai-nop-nhung-loai-thue-nao
- PayOS Top 15 cổng thanh toán VN — https://payos.vn/top-15-cong-thanh-toan-tot-nhat-hien-nay/

### 7.4 Related project docs

- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID
- `documents/03-planning/inside-out-queue.md` — proposal consumed Wave 93
- `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action — 11 new gaps queued Wave 93+

---

## 8. Log

- **2026-05-18:** Audit completed. 3 outside-in agents spawned per `outside-in-coverage-trigger.md` §1; all 3 converged on QR PROCEED conditional. 11 new gaps filed (GAP-625..635). 4 existing gaps re-scoped (GAP-108/183/185/594). Wave 93 plan paired same-PR for atomic landing. User triggered audit via inside-out proposal "QR thay payment processor" — outside-in audit reveals compliance constraint (PSP license + KYC) makes QR mandatory, not optional. Reviewer: @nguyenvankiet (solo-dev audit author + reviewer per `output-review-mandate.md` §3 row "Persona-based business review").
