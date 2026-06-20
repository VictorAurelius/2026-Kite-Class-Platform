# Product Scope / Market Requirements (MRD) — KiteHub + KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + Business Lead
**Reviewer:** Tech Lead + Stakeholders
**Related:** [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.5 VN-EDU (sector scope) · [`compliance-scope.md`](compliance-scope.md) §3 Education Law (L5) · [`personas-catalog.md`](personas-catalog.md) · [`business-objectives.md`](business-objectives.md) · [`pricing-model.md`](pricing-model.md) · [`go-to-market.md`](go-to-market.md) · `release-1-plan-2026.md` (Phase 1/2/3 roadmap)

---

## 1. Phạm vi & mục đích tài liệu

Tài liệu MRD (Market Requirements Document) định nghĩa **thị trường mục tiêu**, **phạm vi sản phẩm theo từng phase**, và **success metrics** cho 2 sản phẩm chia sẻ hạ tầng:

- **KiteHub** — SaaS platform quản lý vòng đời education instances (trial, subscription, billing, domain provisioning, branding).
- **KiteClass** — Multi-tenant education platform, mỗi tenant là 1 trường học / trung tâm.

MRD là cầu nối giữa `personas-catalog.md` (ai dùng), `business-objectives.md` (đo gì), và `01-business/` (rules chi tiết per domain). Đây là **skeleton Phase 1** — khung scope + market framing; số liệu thị trường + success metrics cần product/finance input ở Phase 2.

---

## 2. Thị trường mục tiêu (Target Market)

### 2.1 Phân khúc chính

Thị trường sơ cấp: **trung tâm giáo dục tại Việt Nam** (education centers) — trung tâm dạy thêm, trung tâm ngoại ngữ, trung tâm kỹ năng, tiến tới trường K-12 ở Phase 3.

| Phân khúc | Persona ref | Phase ưu tiên |
|---|---|:---:|
| Trung tâm dạy thêm / luyện thi (tutoring center) | P3 | Phase 1-2 |
| Trung tâm ngoại ngữ (language center) | P2 | Phase 1-2 |
| Giáo viên solo / lớp học cá nhân (solo teacher) | P1 | Phase 1 |
| Trung tâm kỹ năng người lớn (adult skills) | P1 | Phase 2 |
| Trường K-12 (school) | P5 | Phase 3 (sau counsel) |

> TBD (Phase 2 — needs product/market input): market sizing (TAM/SAM/SOM), số lượng trung tâm tại VN theo phân khúc, tốc độ digitalization, willingness-to-pay per phân khúc.

### 2.2 Đặc điểm thị trường VN

- Văn hóa Zalo/email-first cho parent communication (xem `i18n-strategy.md` + `compliance-scope.md`).
- Niên khóa / học kỳ theo khung VN (ref ADR-002, xem [`academic-year-curriculum-structure-policy.md`](academic-year-curriculum-structure-policy.md)).
- Định giá VND, hóa đơn VAT/TCT (ref [`billing-terms.md`](billing-terms.md)).

> TBD (Phase 2): phân tích đối thủ (chỉ benchmark định tính — KHÔNG comparative advertising naming competitors per VN-ADV-6).

---

## 3. Phạm vi sản phẩm theo Phase (In-scope / Out-of-scope)

Aligns với `release-1-plan-2026.md` 3-phase rollout.

### 3.1 Phase 1 — P1+P2 Soft Launch (in-scope)

| Capability | In-scope Phase 1? |
|---|:---:|
| Tenant provisioning + lifecycle (KiteHub) | ✅ |
| Student / course / class / attendance / grade (KiteClass core) | ✅ |
| Subscription + billing + invoice (VAT/TCT skeleton) | ✅ |
| Email transactional (signup, invite, notification) | ✅ |
| AI branding (TEMPLATE-first per ADR-037/026) | ✅ (template mode) |
| Parent / teacher auth (KC-native) | ✅ |

### 3.2 Out-of-scope Phase 1 (defer)

| Capability | Defer to |
|---|:---:|
| Payroll / HR (Labor Code domain) | Phase 2-3 |
| K-12 MoET reporting + học bạ điện tử | Phase 3 |
| Full AI generation (FULL_AI mode) | Phase 2 (ref ADR-026 Ollama defer) |
| Payment gateway live (PSP) | Phase 4 deploy / partnership |
| Mobile native app | TBD |

> TBD (Phase 2 — needs product input): scope matrix đầy đủ per feature × phase; MoSCoW prioritization (Must/Should/Could/Won't) per persona.

### 3.3 Phase 2 / Phase 3 outline

- **Phase 2:** P3 medium-center (payroll, advanced reporting, full AI) — trigger = counsel engaged.
- **Phase 3:** K-12 P5 (MoET alignment, child protection, học bạ điện tử) — xem [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md) + [`child-protection-policy.md`](child-protection-policy.md).

---

## 4. Personas (reference)

Canonical list: [`personas-catalog.md`](personas-catalog.md) (10 personas, Tier 1 = P1/P2/P3/P5). MRD KHÔNG định nghĩa lại personas — chỉ map scope → persona priority. AC chi tiết per persona: `persona-criteria/`.

> TBD (Phase 2): per-persona value proposition + jobs-to-be-done summary trỏ về `persona-criteria/`.

---

## 5. Success Metrics (MRD-level)

MRD-level metrics khác OKRs ở `business-objectives.md` — đây là **market-fit signals**, không phải operating KPIs.

| Signal | Mục tiêu | Nguồn |
|---|:---:|---|
| Number of active tenants per phân khúc | TBD | `business-objectives.md` §4 |
| Trial → Paid conversion % | TBD | [`trial-to-paid-conversion.md`](trial-to-paid-conversion.md) |
| NPS từ pilot tenants | TBD (≥40 candidate) | persona reviews |
| Time-to-first-value (signup → first class created) | TBD | product analytics |

> TBD (Phase 2 — needs product/finance input): tất cả target numbers cần stakeholder workshop; KHÔNG fabricate metric. Mọi marketing claim phải substantiated (VN-ADV-5).

---

## 6. Compliance scope (sản phẩm)

MRD bản thân là tài liệu nội bộ, KHÔNG customer-facing → compliance gián tiếp. Tuy nhiên scope quyết định obligation:

- **Education Law / MoET (L5 — `compliance-scope.md` §3):** scope giáo dục kích hoạt VN-EDU-1 (tuition transparency), VN-EDU-2 (student data minimization), VN-EDU-3 (student records retention) — chi tiết [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md).
- **PDPL (L1 — `compliance-scope.md` §2):** mọi scope chứa student/parent PII → kích hoạt nghĩa vụ data protection (xem [`data-classification-policy.md`](data-classification-policy.md)).
- **K-12 (Phase 3):** scope expansion sang P5 kích hoạt [`child-protection-policy.md`](child-protection-policy.md) như điều kiện tiên quyết.

> TBD (Phase 3): scope-to-obligation traceability matrix khi K-12 vào scope.

---

## 7. Dependencies / References

- BRD: [`personas-catalog.md`](personas-catalog.md), [`business-objectives.md`](business-objectives.md), [`pricing-model.md`](pricing-model.md), [`go-to-market.md`](go-to-market.md), [`nfr-catalog.md`](nfr-catalog.md)
- Compliance: [`compliance-scope.md`](compliance-scope.md), [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md)
- Roadmap: `release-1-plan-2026.md` (Phase 1/2/3/4)
- Consumer: `01-business/*` — rules.md per domain implement scope này

---

## 8. Out of Scope (this skeleton)

- Market sizing numbers (Phase 2 — finance + market research)
- Final feature × phase matrix sign-off (Phase 2 — product)
- Competitive analysis artifact (Phase 2 — định tính, không naming per VN-ADV-6)

---

## 9. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). Section structure + phase scope framing complete; market numbers + success metrics marked TBD (Phase 2, needs product/finance input).
