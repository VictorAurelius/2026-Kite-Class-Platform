---
paths:
  - "documents/08-thesis/**"
---

# Thesis Binary Time Concept — chỉ "hiện tại đạt được" + "phát triển sau"

**Priority:** 🟠 MANDATORY — thesis content discipline
**Version:** 1.0.1
**Created:** 2026-05-29
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on Wave thesis-3 sweep) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies user direction 2026-05-29 mid-Wave-thesis-3)
**Applies to:** Mọi file dưới `documents/08-thesis/**` được render thành DOCX/PDF deliverable cho academic submission. Out-of-scope: source code, internal runbooks, non-academic project docs (per `professional-manual-content-standard.md` scope).

---

## 1. The Rule

> **Báo cáo thesis CHỈ có 2 khái niệm thời gian được phép tham chiếu:**
> 1. **"Hiện tại đạt được"** — những gì đã triển khai + chứng minh được trong phạm vi đồ án
> 2. **"Phát triển sau"** — định hướng/phương hướng phát triển kế tiếp (KHÔNG cam kết timeline cụ thể)

> **BANNED:** mọi đề cập đến "đợt thử nghiệm" / "đợt vận hành thật" / "giai đoạn thử nghiệm" / "giai đoạn vận hành chính thức" / "giai đoạn mở rộng" / "giai đoạn GA" / "giai đoạn thanh toán thử nghiệm" / "Phase 1.5" / "Phase 2" / "Phase 3" / "phiên bản đầu" / "phiên bản kế tiếp" / "release N" / "Wave N" trong narrative thesis.

Force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn binary time → mọi thesis V1+ V2+ subsequent auto-comply → eliminate "giai đoạn X" jargon causing committee confusion + premature commitment phrasing.

---

## 2. Why this rule exists

Academic thesis báo cáo về **những gì sinh viên đã làm và rút ra**, KHÔNG phải startup roadmap với pricing + go-to-market + release schedule. Khi narrative dùng "giai đoạn thử nghiệm / giai đoạn vận hành chính thức / giai đoạn GA":
- Committee không phải investor → không quan tâm phase timeline
- Phase nomenclature signals startup-mindset không phải academic
- "Phiên bản kế tiếp X sẽ có Y" = premature commitment, không có evidence
- Sinh viên không control được lifecycle → claim "Phase 2 sẽ có" = unsupported

Trigger: user direction 2026-05-29 mid-Wave-thesis-3:
> "thêm rules mới: không đề cập đến các khái niệm như đợt thử nghiệm, đợt vận hành thật hay các đợt nào khác. Báo cáo này chỉ có 2 khái niệm thời gian: hiện tại đạt được và sẽ phát triển sau"

> "báo cáo dừng lại ở kết quả đạt được là đã có thể thanh toán rồi, có kết nối zalo rồi => cần sửa lại 1 số chỗ nói chưa có tính năng này"

User explicit lock: thesis báo cáo state là **đã có thanh toán + đã có Zalo integration**. Mọi narrative "chưa có" / "phase X sẽ thêm" cho 2 tính năng này phải sửa lại thành "đã có".

---

## 3. Banned patterns reference

| ❌ BANNED phrasing | ✅ Required replacement |
|---|---|
| "giai đoạn thử nghiệm" | "hiện tại đạt được" / "kết quả triển khai" / strip if narrative redundant |
| "giai đoạn vận hành chính thức" | "phát triển sau" / "lộ trình phát triển" / "phương hướng phát triển" |
| "giai đoạn mở rộng" | "phát triển sau" / "lộ trình phát triển" |
| "giai đoạn GA" | "phát triển sau" / strip — "GA" academic không cần |
| "giai đoạn thanh toán thử nghiệm" | strip — payment đã active, không phải "thử nghiệm" |
| "giai đoạn K-12 expansion" | "phát triển sau (mở rộng sang khối K-12)" hoặc strip nếu redundant |
| "Phase 1 BETA" / "Phase 1.5" / "Phase 2" / "Phase 3" | strip — không đề cập phase indices |
| "phiên bản đầu" / "phiên bản kế tiếp" | "hiện tại" / "phát triển sau" |
| "release N" / "Wave N" | strip — internal repo jargon |
| "trong giai đoạn này không thu phí" | "hiện tại không thu phí" hoặc strip nếu context rõ |
| "Phụ huynh giai đoạn vận hành chính thức nhận thông báo Zalo" | "Phụ huynh nhận thông báo Zalo" — đã có Zalo |
| "VietQR giai đoạn thử nghiệm; mở rộng VNPay/MoMo giai đoạn vận hành chính thức" | "VietQR cho thanh toán hiện tại; lộ trình mở rộng VNPay/MoMo" |
| "Zalo OA sẽ tích hợp giai đoạn vận hành chính thức" | "Zalo OA đã tích hợp" hoặc "kênh Zalo đã kết nối" (per Task 3 status update) |
| "thanh toán chuyển khoản chưa có / defer phase 2" | "đã có thanh toán chuyển khoản qua VietQR" |
| "Cron tính phí bỏ qua khung Tết (sẽ implement phase 2)" | "cron tính phí có cấu hình bỏ qua khung Tết" |

---

## 4. Status update mandate (Task 3 paired)

Per user direction 2026-05-29:
> "báo cáo dừng lại ở kết quả đạt được là đã có thể thanh toán rồi, có kết nối zalo rồi => cần sửa lại 1 số chỗ nói chưa có tính năng này"

Locked state cho thesis:
- ✅ Thanh toán: VietQR đã tích hợp + chuyển khoản ngân hàng + tự động đối soát
- ✅ Zalo: kênh thông báo Zalo OA đã kết nối + group chat parent communication
- ✅ Mọi tính năng narrative-claimed "đã có" phải reflect trong thesis state

Mọi narrative nói "chưa có / sẽ có / defer / phase N" cho 2 tính năng này → REPHRASE thành "đã có".

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Tạo subsection mô tả "lộ trình các giai đoạn 1-5 chi tiết với timeline" | "Phương hướng phát triển" mô tả định hướng chung — KHÔNG enumerate phase indices |
| "Phase 2 sẽ thêm chatbot AI" | "Lộ trình phát triển sau bao gồm chatbot AI" |
| "Trong giai đoạn vận hành chính thức, hệ thống sẽ tích hợp đa cổng thanh toán" | "Lộ trình phát triển sau gồm tích hợp thêm đa cổng thanh toán (VNPay, MoMo)" |
| "Beta tenant cohort 30-50 trung tâm phase 2 sẽ mở" | strip phase nomenclature — chỉ nêu "mở rộng cohort khi sản phẩm trưởng thành" |
| Bao gồm pricing model với "giai đoạn thanh toán thử nghiệm" prefix | "Mô hình giá hiện tại" — không phase prefix |
| "K-12 expansion sẽ mở giai đoạn GA" | "Phát triển sau bao gồm mở rộng sang khối K-12" |
| Chia §4.X thành "kết quả giai đoạn 1" + "kết quả giai đoạn 2" | Đơn giản "kết quả đạt được" — không phase split |

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/08-thesis/**`:

- [ ] grep `giai đoạn (thử nghiệm|vận hành chính thức|mở rộng|GA|thanh toán thử nghiệm)` returns 0 in body
- [ ] grep `Phase [1-9]|Wave [0-9]|release [0-9]` returns 0 in body (per `thesis-content-standard.md` §10 S3 + sister rule)
- [ ] grep `phiên bản (đầu|kế tiếp|sau)` returns minimal — only narrative "lộ trình phát triển sau" context
- [ ] Status check: Zalo + payment → narrative says "đã có" / "đã tích hợp" / "đã kết nối"
- [ ] Lộ trình phát triển sau (Phương hướng phát triển) section dùng "phát triển sau" / "lộ trình" — KHÔNG enumerate phase indices

### 6.2 CI grep detector (deferred per `incident-to-rule-pipeline.md` §3.1)

Heuristic grep detector simple:

```bash
grep -rnE 'giai đoạn (thử nghiệm|vận hành chính thức|mở rộng|GA)' documents/08-thesis/chapter-*.md \
  && echo "WARN: violates thesis-binary-time-concept.md §1 — use 'hiện tại đạt được' / 'phát triển sau'"
```

Defer per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions:
- Recurrence count 0 post-rule
- Reviewer-checklist §6.1 sufficient cho v1.0.0
- Revisit when recurrence ≥2 post-rule

### 6.3 Memory auto-load (optional, deferred)

Memory entry `feedback_thesis_binary_time_concept.md` could remind tại session start trước khi edit thesis content. Defer ≥7 ngày per premature-rule guard.

### 6.4 Override mechanism

Genuine exception (vd narrative MUST explain prior project phases for historical context):

```
git commit -m "...
THESIS_BINARY_TIME_OVERRIDE: <file> — <reason — e.g., explaining migration history pre-design>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 7. Worked self-test — Wave thesis-3 sweep 2026-05-29

**Sweep scope** verified via grep at rule-creation time:
- ch1-competitor: 5 instances `giai đoạn X`
- ch1-vn-law: 2 instances (in §1.3.3 Phạm vi đề tài lộ trình)
- ch2-system: 30 instances (heavy "giai đoạn thử nghiệm" / "giai đoạn vận hành chính thức" pattern)
- ch3-implementation: 5 instances
- ch4-deployment: 2 instances
- **Total: 44 instances**

**Apply rule retroactively across 44 instances:**

| Original phrasing | Required replacement |
|---|---|
| "Trong giai đoạn thử nghiệm, hai thẻ Học viên và Khóa học đã hiển thị..." | "Hiện tại, hai thẻ Học viên và Khóa học đã hiển thị..." |
| "VietQR giai đoạn thử nghiệm; mở rộng VNPay/MoMo giai đoạn vận hành chính thức" | "VietQR cho thanh toán hiện tại; lộ trình phát triển sau gồm tích hợp thêm VNPay/MoMo" |
| "tích hợp Zalo OA cho phụ huynh là yêu cầu giai đoạn vận hành chính thức" | "tích hợp Zalo OA cho phụ huynh đã hoàn thành" (per Task 3 status lock) |
| "Cron tính phí bỏ qua khung Tết (giai đoạn vận hành chính thức)" | "Cron tính phí có cấu hình bỏ qua khung Tết" |
| "Giai đoạn thử nghiệm tenant gồm vận hành thử với hai giáo viên độc lập" | "Phạm vi triển khai hiện tại bao gồm vận hành thử với hai giáo viên độc lập" |
| "Giai đoạn K-12 expansion mở rộng sang khối phổ thông K-12 phục vụ trường công + tư" | "Lộ trình phát triển sau bao gồm mở rộng sang khối phổ thông K-12 với yêu cầu bổ nhiệm DPO chính thức..." |

**Verdict:** rule fires correctly trên 44 instances; sweep generates clean narrative aligned với "hiện tại" + "phát triển sau" binary. Self-test PASS ✅

**Counterfactual without rule:** thesis ship với 44 phase nomenclature instances → committee confusion + premature commitment language + startup-mindset signals → defense criticisms.

---

## 8. Relationship to other rules

- **`thesis-content-standard.md`** v2.0.0 §10 S3 — Vietnamese narrative strict; this rule adds time-concept narrowing
- **`thesis-content-standard.md`** §3 banned patterns — sister table, this rule extends with "phase nomenclature" banned class
- **`output-review-mandate.md`** §3 — paired same-PR with new matrix row "Thesis binary time concept"
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 chuẩn binary time → mọi thesis V1+ V2+ subsequent auto-comply)
- **`incident-to-rule-pipeline.md`** — this rule = direct output user direction 2026-05-29 mid-Wave-thesis-3 sweep
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §7 + paired same-PR with 44-instance sweep all ship same commit batch
- **`dev-readable-doc-language.md`** §2 row "Thesis report" — sister rule covering narrative language; binary time concept extends

---

## 9. Log

- **2026-05-31** (v1.0.1): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~13k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-29 (v1.0.0):** Rule created in response to user direction mid-Wave-thesis-3 (2 directions same message):
  1. "thêm rules mới: không đề cập đến các khái niệm như đợt thử nghiệm, đợt vận hành thật hay các đợt nào khác. Báo cáo này chỉ có 2 khái niệm thời gian: hiện tại đạt được và sẽ phát triển sau"
  2. "báo cáo dừng lại ở kết quả đạt được là đã có thể thanh toán rồi, có kết nối zalo rồi => cần sửa lại 1 số chỗ nói chưa có tính năng này"
  
  Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user direction explicit) → Classify ✓ (no existing rule codifies binary time concept; `thesis-content-standard.md` v2.0.0 §10 S3 covers narrative language Vietnamese only, không cover phase nomenclature ban; §3 banned patterns covers project-internal scrub Wave/GAP/Phase but not narrative "giai đoạn X" academic-VN phrasing) → Rule+Enforce ✓ (this file + reviewer-checklist §6.1 + worked self-test §7 on 44-instance Wave thesis-3 sweep + paired same-commit-batch with status update + heading audit + pipeline page_break fix per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example trên 44 instances — rule fires correctly + counterfactual eliminates startup-mindset signals trong academic deliverable) → Retro Log ✓ (this entry). 
  
  META P0 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn binary time → mọi thesis V1+ V2+ subsequent auto-comply prospectively.
  
  Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered phase nomenclature ban + status update lock; no constraint loosening for prior thesis content; existing 44 instances grandfathered until Wave thesis-3 sweep ships same commit batch; rule applies prospectively từ Wave thesis-4+ forward).
  
  Atomic-unique-bar §5.1 check passed:
  - ✅ atomic (single concept: 2 khái niệm thời gian only)
  - ✅ unique (sister rules cover narrative language + project-internal scrub, neither covers phase nomenclature specifically)
  - ✅ widely applicable (every thesis content edit)
  - ✅ body discipline §1 has ≤2 "and" conjunctions
  
  CI grep detector (§6.2) + memory auto-load (§6.3) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + worked self-test §7 sufficient cho v1.0.0.
