---
title: Outside-in audit — OCR auto-confirm receipt upload proposal (Phase 1.5+)
status: complete
created: 2026-05-18
audit_type: persona-review + benchmark + failure-mode (3-agent outside-in)
trigger: user inside-out proposal 2026-05-18 — "upload ảnh chuyển khoản → OCR auto-confirm"
parent_audit: 2026-05-18-phase-1-5-qr-payment-outside-in.md (Wave 93 base QR audit)
scope: phase-1.5-paid OCR feature evaluation (evolution beyond manual QR mark)
related_gaps: [GAP-630 evidence storage, GAP-636 Casso/SePay webhook investigation]
verdict: OCR REJECTED; pivot Casso/SePay webhook integration adopted
---

# Outside-in audit — OCR auto-confirm receipt upload

## 0. TL;DR

**Verdict:** 🔴 **OCR auto-confirm REJECTED** cho Phase 1.5+ — outside-in audit 3-agent surfaces industry pattern khác hẳn: **VietQR + Casso/SePay webhook** là dominant VN e-commerce 2026 pattern, ~0% fraud risk, eliminate manual reconcile entirely.

**Pivot adopted:** Wave 93 base QR manual mark Phase 1.5a (KEEP) → **Phase 1.5b Casso/SePay webhook integration** (replacing OCR proposal) → Phase 2 optional OCR fallback only cho banks không support Casso.

**Gap filed:** GAP-636 P1 "Casso/SePay webhook integration investigation Phase 1.5b" — chính canh path forward.

**Why OCR rejected:** 3 agents converge on 4 strong signals:
1. **Zero VN edu SaaS competitor dùng OCR** (MISA, DotB, VietQR EduPay, VnResource, Easy Edu, Mona) — not for lack of trying; better path exists
2. **2026 fraud landscape kill OCR:** 14% documents are AI-generated fakes, 75% human reviewer miss-rate
3. **VN bank screenshot không OCR-friendly:** 0 banks public template SDK; format drift mỗi app update
4. **Casso/SePay webhook pattern dominant VN e-commerce 2026** — match ecosystem, KiteHub vẫn KHÔNG broker tiền

---

## 1. Trigger + methodology

**Inside-out proposal (user, 2026-05-18):**
> "Phát triển feature upload ảnh chuyển khoản cho hệ thống nhận diện số tiền, tài khoản, ngày tháng để hệ thống tự xác nhận, thay vì user phải tự check. Nếu nộp bằng tiền mặt thì user mới tự tick → vẫn phải có flow manual."

**Outside-in trigger fired** per `.claude/rules/outside-in-coverage-trigger.md` §1 — đây là lần thứ 3 trong session, bucket-internal scope refinement Phase 1.5 payment. User confirm "Spawn 3 agents parallel ngay".

**3 agents spawned background:**
1. **Persona walkthrough** — P1 thầy Tâm + P2 chị Hằng × 3 upload-actor decisions × 6 scenarios
2. **External benchmark** — 7 VN edu SaaS + 10 VN banks + OCR providers (FPT.AI / VinAI / Google Vision / PaddleOCR)
3. **Failure-mode matrix** — 22 OCR-specific scenarios × 3 axes (Actor / Class / Phase)

---

## 2. Agent findings

### 2.1 Persona walkthrough (Agent P)

**6-cell decision matrix:**

| Decision | P1 Thầy Tâm (solo) | P2 Chị Hằng (~30 HS) |
|---|---|---|
| **A. PH self-upload** | 🟡 friction ↓ but anti-fraud ↑ risk | ✅ friction ↓↓↓ (75/90 click save) + manage fraud |
| **B. Owner self-upload** | ❌ friction ↑ (banking app + snap + upload) | ❌ friction ↑↑ (5-6× more work; not scalable) |
| **C. Hybrid (Owner default + PH appeal)** | ✅ safe trust + balanced fraud | ✅ works ≤50 HS; scale-cliff at 100 HS |

**TOP 3 sharpened decisions:**
1. **Upload actor:** Hybrid (Owner default + PH appeal) — **conditional** trên OCR ship; sửa lại nếu webhook path → moot
2. **OCR vs URL-signature:** OCR mandatory **conditional** trên OCR ship; webhook eliminates question
3. **Trust level:** Auto-suggest + 1-click approve (NOT fully auto)

**Persona net positive verdict:** P1 ✅ 30 min/tháng save; P2 ⚠️ partial (70-click save Phase 1.5; scale-cliff 100 HS) → mandatory Phase 2 pivot.

### 2.2 External benchmark (Agent B) — **OVERRIDE finding**

**Question 1 — Edu SaaS competitors có OCR?**

| Competitor | OCR receipt | Auto-reconcile | Approach |
|---|:---:|:---:|---|
| MISA EMIS | ❌ Không | ⚠️ Partial | Payment gateway integration, e-invoice |
| DotB EMS | ❌ Không | ⚠️ Partial | Pricing per active student |
| VnResource | ❌ Không | ❌ Không | HRM-focused, manual |
| MISA eMon / Mona / Easy Edu | ❌ Không | ❌ Không | Manual reconcile dominant |
| **VietQR EduPay (NAPAS)** | ❌ **Không cần** | ✅ **Có** | **Webhook real-time qua bank** |
| VCB CashUp | ❌ Không | ✅ Corporate | Virtual Account + webhook callback |

**Verdict Q1:** 0 VN edu SaaS dùng OCR primary. Họ dùng VietQR webhook.

**Question 2 — VN bank OCR-friendly?**

| Bank | Template consistent | Public retail API |
|---|:---:|:---:|
| Vietcombank | ⚠️ Khác app vs web | ❌ Không (CashUp corporate only) |
| Techcombank | ⚠️ Consistent F@st Mobile | ❌ Open Banking pilot only |
| MB Bank | ⚠️ MB Bizz consistent | ⚠️ Partial qua MB BIZ MCA |
| ACB, BIDV, VPBank, TPBank, VietinBank, Agribank, Sacombank | ⚠️ Format khác mỗi app | ❌ Không public |

**Verdict Q2:** 0 banks public OCR template SDK. Format drift mỗi app update.

**Question 3 — URL signature alternative?**

🔴 **NOT feasible VN 2026.** VietQR/NAPAS không generate signed URL post-transfer. Không TransactionRefId verifiable cross-bank cho retail.

**Question 4 — Fraud landscape 2026:**
- AI-generated fake receipts: **14% fraud documents** (jump từ 0% 2024)
- Human reviewer miss-rate: **75%** high-quality AI fakes
- Copy-move + splicing tools democratized
- Detection requires AI image forensic layer riêng (TruthScan / Klippa / ELA) → additional engineering cost

**Question 5 — OCR providers VN:**

| Provider | VN accuracy | Cost/1000 receipts | Phase 1.5 fit |
|---|---|---|---|
| FPT.AI Read | 98% (50+ bank templates) | $5-15/1000 | VN-native, best fit IF OCR pursued |
| VinAI OCR | Không public benchmark | Liên hệ | VN-trained |
| Google Cloud Vision | 85-90% VN | $1.50/1000 (free tier 1000/month) | Multinational best |
| PaddleOCR PP-OCRv5 | 92% VietOCR fork | $0 + infra | Open source, fine-tune cost |
| Tesseract + VietOCR | 85-90% | $0 | Yếu hơn PaddleOCR |

**Benchmark verdict:** OCR tech khả thi nhưng KHÔNG nên làm vì 4 lý do:
1. Fraud risk 2026 quá cao
2. VN bank screenshot format drift = ongoing maintenance cost
3. Pattern dominant đã là VietQR + webhook
4. Edu SaaS competitors skip OCR — không phải vì không nghĩ tới

**Recommend approach 🟢 Path A — VietQR + Casso/SePay webhook:**
- Tenant connect bank account qua Casso (https://casso.vn) hoặc SePay (https://sepay.vn)
- KiteHub generate VietQR per invoice với unique `addInfo=KH-INV-12345`
- PH chuyển khoản → bank → Casso webhook → KiteHub auto-confirm
- Cost: SePay free tier; Casso $10-30/month per tenant
- Fraud risk: ~0% (tiền thực sự vào tài khoản, không phải fake screenshot)
- Match dominant VN e-commerce pattern

### 2.3 Failure-mode matrix (Agent F)

**22 OCR-specific failure scenarios** documented (separate from Wave 93 base 21 manual-mark scenarios). Categories: OCR accuracy / Fake-screenshot fraud / PDPL PII / Wrong-recipient / Multi-bank format drift / Operational.

**TOP 5 P0 failures OCR CANNOT mitigate:**
1. **Fake screenshot detection** (F1-F4) — Photoshop + AI-generated; OCR blind to image forensics
2. **Cross-tenant isolation exploit** (F15) — PH copy STK tenant khác; OCR extract correctly nhưng system không validate tenant scope
3. **Multi-bank template drift** (F10-F11) — Techcombank 2025 UI overhaul + HSBC VN international format → OCR retrain weeks
4. **PDPL retention policy void** (F12-F13) — auto-capture full STK + balance + name; Art 11 audit trail not auto-built
5. **Fraud cost > false-reject cost** (F18) — confidence threshold tuning requires fraud-rate data hậu launch; pre-launch tune risk

**TOP 3 mitigates BETTER than manual:**
1. Timestamp + image-hash replay detection
2. Installment payment tracking (auto-detect 1/N paid)
3. STK mismatch vs current Owner record

**5 P0 conditions MUST close trước enable auto-confirm:**
1. Image forensics baseline (hash DB + metadata + screen-vs-native check)
2. Multi-bank template versioning SLA (<48h retrain on UI change)
3. PDPL audit trail integration (Art 11 audit log + DSAR queryable)
4. Cross-tenant validation at match phase (STK lookup tenant-scoped per GAP-625)
5. Confidence threshold calibration (90% recommended; post-BETA retune)

**Confidence threshold analysis (cost-benefit 1000 receipts/week):**

| Threshold | False-accept | False-reject | Loss | Review cost | Total |
|---|---|---|---|---|---|
| 80% | 1.5% | 8% | 0.56M | 20k | 0.58M |
| **90%** | **0.5%** | **15%** | **0.19M** | **37.5k** | **0.227M** ← minimum |
| 95% | 0.1% | 25% | 0.038M | 62.5k | 0.1M (UX unsustainable) |

**Failure-mode verdict:**
- Phase 1.5a: ❌ BLOCK (5 P0 conditions not ready)
- Phase 1.5b: ⚠️ CONDITIONAL only if all 5 P0 conditions close + BETA fraud-rate data tune threshold
- Phase 2: ✅ Feasible after BETA cycle + post-launch retro

---

## 3. 3-agent convergence + override pattern

| Finding | Persona | Benchmark | Failure-mode | Verdict |
|---|:---:|:---:|:---:|:---:|
| OCR mitigates P2 reconciliation pain | ✅ 78% click save | ⚠️ Casso webhook also solves | ⚠️ but with new fraud surface | OCR partial yes, webhook better |
| Industry uses OCR for VN edu SaaS | — | ❌ **0 competitors** | — | ❌ NO |
| Fraud risk acceptable 2026 | ⚠️ "Owner final say" | ❌ 14% AI fakes + 75% miss | ❌ TOP 5 P0 cannot mitigate | ❌ HIGH RISK |
| OCR ship-ready Phase 1.5 | ⚠️ Yes if HYBRID + 1-click | ❌ Pivot Casso instead | ❌ 5 P0 conditions blocker | ❌ NO |
| Better path exists than OCR | — | ✅ Casso/SePay webhook | — | ✅ YES — pivot |

**Override pattern:** Persona + Failure-mode agents evaluated OCR as proposed (conditional path forward); **Benchmark agent surfaces fundamentally better alternative** (Casso/SePay webhook) outside original problem framing. This is exactly the value of external benchmark in outside-in audit per `outside-in-coverage-trigger.md` §3 Bước 2.

---

## 4. Decision adopted

**🔴 OCR auto-confirm REJECTED Phase 1.5+** (NOT Phase 1.5a / NOT Phase 1.5b / NOT Phase 2 as primary). Optional fallback Phase 2 only cho banks không support Casso webhook (defer to future scope).

**🟢 PIVOT — Casso/SePay webhook integration Phase 1.5b:**

| Phase | Approach |
|---|---|
| Phase 1.5a (Wave 31-32) | QR manual mark Wave 93 chốt — **KEEP** |
| **Phase 1.5b (Wave 33-34)** | **Casso/SePay webhook investigation + integration** — GAP-636 P1 |
| Phase 2 | Optional OCR fallback only cho edge cases (banks không Casso) — defer scope |
| **Never (primary)** | OCR auto-confirm pure path |

**KiteHub revenue model:** Unchanged — pure SaaS subscription. Casso/SePay webhook = notification only, KiteHub still non-broker (no PSP license risk). Tenant pays Casso/SePay direct.

**Cost implication tenant Phase 1.5b:**
- SePay free tier khả dụng cho most tenants
- Casso $10-30/month per tenant (>= 1000 txn/month)
- KiteHub absorb hoặc pass-through cost (Phase 1.5 plan §4.4 deliverable update)

**Webhook flow architecture (high-level):**
```
PH chuyển khoản
   ↓
Bank (Vietcombank/Techcombank/...)
   ↓
Casso/SePay aggregator (subscribed to tenant's bank)
   ↓ (webhook HTTPS POST)
KiteHub webhook endpoint
   ↓ parse addInfo=KH-INV-12345
   ↓ match invoice → mark paid
   ↓ update audit log + immutable record
Tenant Owner dashboard auto-update
```

---

## 5. Gap recommendations

### 5.1 NEW gap filed Wave 93

| Gap ID | Title | Priority | Phase | Source |
|---|---|---|---|---|
| **GAP-636** | Casso/SePay webhook integration investigation Phase 1.5b | P1 | phase-1.5-paid | This audit §4 pivot decision |

GAP-636 scope:
- (a) Casso vs SePay vendor evaluation (cost, coverage, webhook reliability, support, OpenAPI)
- (b) Webhook receiver implementation (HTTPS endpoint + signature verify + idempotency)
- (c) `addInfo` unique ID generation per invoice + match algorithm fuzzy fallback
- (d) Tenant onboarding flow (Owner connect bank account via Casso/SePay)
- (e) Manual reconcile fallback retained (cash payments + webhook failure)
- (f) Pilot 3-5 P2 tenants Phase 1.5b sub-bucket

### 5.2 NEW gap deferred Phase 2 (no separate filing this PR)

| Gap candidate | Reason defer |
|---|---|
| OCR fallback Phase 2 cho banks không Casso | Defer — only if Casso/SePay coverage incomplete; investigate during GAP-636 vendor eval |

### 5.3 Existing gap cross-references

| Existing | Cross-ref scope |
|---|---|
| GAP-630 (evidence storage) | Phase 1.5a manual QR mark scope unchanged; OCR-specific storage requirements obsoleted (no OCR ship) |
| GAP-628 (batch reconcile API) | Phase 1.5a manual path scope unchanged; webhook obviates batch reconcile for many invoices Phase 1.5b |
| GAP-632 (mark-paid audit trail) | Both manual + webhook path use same audit trail infra; no scope change |

---

## 6. Audit trail

### 6.1 3 agent transcripts (verifiable)

- OCR Persona walkthrough — id `abf511c663e8d5f80` — completed 2026-05-18 (80s wall-clock, 102k tokens)
- OCR External benchmark — id `affe6d9360f7036bf` — completed 2026-05-18 (129s wall-clock, 155k tokens, 13 citations)
- OCR Failure-mode matrix — id `a3d8f115c69bbaf43` — completed 2026-05-18 (74s wall-clock, 111k tokens, 22 scenarios)

### 6.2 Rules applied

- `.claude/rules/outside-in-coverage-trigger.md` v1.0.0 — fire trigger lần 3 trong session per memory `feedback_outside_in_recurring_miss.md`
- `.claude/rules/agent-action-bias.md` Part A — propose action không offload user investigation
- `.claude/rules/audit-to-gap-pipeline.md` §2.5 — state-check (greenfield OCR/webhook scope)
- `.claude/rules/meta-gap-priority.md` §3 — Business-Logic tier (payment compliance + UX strategic decision)

### 6.3 External sources (Benchmark agent citations)

- MISA EMIS Platform — https://emis.misa.vn/emisconglap/en/
- DotB EMS — https://dotb.vn/ + https://dotb.vn/pricing/
- VietQR API — https://api.vietqr.vn/en + https://www.vietqr.io/en/danh-sach-api/payment-confirmation/
- **Casso** — https://casso.vn/api-ngan-hang/ + https://developer.casso.vn/webhook/thiet-lap-webhook-thu-cong
- **SePay** — https://sepay.vn/bang-gia.html + https://sepay.vn/tai-khoan-ao-theo-don-hang.html
- FPT.AI Read OCR — https://fpt.ai/products/fpt-ai-read/
- Google Cloud Vision — https://cloud.google.com/vision/pricing
- PaddleOCR vs Tesseract — https://www.codesota.com/ocr/paddleocr-vs-tesseract
- Klippa Image Tampering Detection — https://www.klippa.com/en/blog/information/image-tampering-detection/
- TruthScan Image Fraud — https://truthscan.com/blog/types-of-image-based-fraud/
- Instant Payments Vietnam Rails 2026 — https://www.lightspark.com/knowledge/instant-payments-vietnam

### 6.4 Related project artifacts

- Parent audit: `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md` (Wave 93 base)
- Re-triage audit: `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-26-gaps-re-triage.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- Inside-out queue: `documents/03-planning/inside-out-queue.md` — entry status: consumed Wave 93

---

## 7. Log

- **2026-05-18:** Audit completed. 3 outside-in agents spawned per `outside-in-coverage-trigger.md` §1 (lần thứ 3 session — closes `feedback_outside_in_recurring_miss.md` recurrence). Persona + Failure-mode agents evaluated OCR conditionally; **External benchmark agent OVERRIDES với industry pivot signal: Casso/SePay webhook is VN e-commerce 2026 dominant pattern**. User accepted benchmark recommendation 2026-05-18. GAP-636 filed Wave 93 P1 cho webhook investigation Phase 1.5b. OCR rejected primary path; optional fallback Phase 2 deferred. Audit demonstrates value of external benchmark catching alternative path outside original problem framing — protects against "execute inside-out scope blindly" anti-pattern.
