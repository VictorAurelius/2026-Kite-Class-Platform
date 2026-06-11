---
title: Pre-Wave UI-Kits-100 — Outside-In Refresh (staleness + class-sweep + persona + bucket verdict)
audit_date: 2026-06-11
auditor: Outside-in audit agent (pre-wave lock refresh cho wave-ui-kits-100)
scope: 12 UI kit folders under documents/02-architecture/design-system/ui_kits/ + 8 gap files (GAP-363b/364b/428/274/366/367 + parents 363/364)
trigger: outside-in-coverage-trigger.md §2 (pre-lock wave plan gom toàn bộ gap UI kits) + audit-to-gap-pipeline.md §2.8 staleness check (2 review report >30 ngày)
related_reports:
  - documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md (avg 100.4/128)
  - documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md (avg 101.1/128)
verdict_summary: 2 review report PARTIAL-stale (Wave 22 đã fix 2 màn lowest-score); 3 cross-flow class bug surfaced (font-drift root token + tier-name drift + domain/fabrication); 4 bucket verdict bên dưới
---

# Pre-Wave UI-Kits-100 — Outside-In Refresh

> Outside-in refresh TRƯỚC khi lock wave plan `wave-ui-kits-100`. 4 nhiệm vụ: (1) staleness 2 review report, (2) class-bug sweep 12 kit, (3) persona refresh 3 deliverable, (4) bucket verdict A/B/C/F.

---

## 1. Staleness check — 2 review report (per audit-to-gap-pipeline.md §2.8)

**Kết luận: PARTIAL-stale. 2 report = state TRƯỚC Wave 22; Wave 22 (2026-05-06, NGAY SAU report 2026-05-05) đã fix đúng 2 màn lowest-score.**

`git log` 2 kit từ 2026-05-05:

| Commit (2026-05-06) | Kit | Thay đổi |
|---|---|---|
| `96ce3ffd8` Wave 22-A (GAP-363) | kiteclass-student | `payments.html` rebuild Option C (parent-trigger child-protection) +4 polish (my-classes / assignments / grade-detail / profile) |
| `f325394d8` Wave 22-B (GAP-364) | kitehub-admin | `school-profile.html` rebuild (1078 dòng đổi) |
| `a99b6fbe1` Wave 22-C (GAP-365) | kiteclass-student | persona AC doc S-student.md (README +3 dòng) |

**Findings nào ĐÃ fix vs CÒN valid:**

| Kit | Màn đã lift (report stale) | Màn còn nguyên (report valid) |
|---|---|---|
| kiteclass-student | `payments.html` 92 → ~108 (self) — P0 child-protection cleared; `my-classes`/`assignments`/`grade-detail`/`profile` minor +1-2 | 8/14 màn cluster 99-104 KHÔNG đổi từ review → findings còn valid nguyên vẹn (today/class-detail/assignment-detail/grades/attendance/notifications/login/empty-states) |
| kitehub-admin | `school-profile.html` 91 → 107 — lowest-screen cleared | 10/12 màn 99-104 KHÔNG đổi → cross-screen findings (skeleton/empty-state/dark-mode parity) còn valid |

→ **Không cần re-audit toàn bộ; chỉ cần external re-audit để xác nhận avg sau Wave 22** (self-rescore student ~102.5, admin ~école chưa rescore). 2 report vẫn dùng được làm baseline cho ~80% màn chưa đổi. Đây CHÍNH là scope GAP-363b Step 1 (external re-audit) + GAP-364b re-score — không lãng phí.

---

## 2. Class-bug sweep — 12 kit (per cross-flow-bug-class-sweep.md §4.1)

### 2.1 🔴 Font drift — ROOT TOKEN dùng Inter (GAP-1223 class lặp, cross-flow)

**Đây là finding lớn nhất.** `_shared/colors_and_type.css` (token canonical mà mọi kit import) VẪN định nghĩa Inter:

```
line 8:  @import url('...family=Inter:wght@400;500;600;700;800...');
line 62: --font-sans: 'Inter', system-ui, ...;
```

Chỉ **2/12 kit** dùng `Be Vietnam Pro` (per grep): `components` + `marketing-site` (marketing-site vừa fix GAP-1223). **10 kit còn lại + chính root `_shared` token = Inter** (kiteclass-pro-v2, kiteclass-parent, kiteclass-student, kiteclass-teacher, kitehub-admin, kitehub-pro-v2, kitehub-story-v2, ai-branding-wizard-v2, components-states, index.html).

→ GAP-1223 chỉ fix marketing-site **cục bộ**, KHÔNG fix root `_shared/colors_and_type.css` → font drift vẫn systemic. Nếu Be Vietnam Pro thật sự là canonical (cần confirm qua design-system doc per design-first-investigation-order.md — tôi KHÔNG suy diễn từ code), thì đây là cross-flow bug-class statically-detectable → **nên ship persistent detector** `grep "Inter" trong ui_kits/ ngoài _shared archived` + fix root token 1 lần (cascade mọi kit). Đây là leverage cao nhất của cả wave: sửa 1 file token → 10 kit auto-comply.

⚠️ Caveat: cần đọc design-system source-of-truth doc xác nhận Be Vietnam Pro vs Inter là canonical TRƯỚC khi fix root (tránh fix sai chiều).

### 2.2 🟡 Tier-name drift — "PRO" dùng làm tier label

Canonical (per prompt) = FREE/BASIC/PREMIUM/ENTERPRISE. Sweep thấy "PRO" dùng làm **tier label** (KHÔNG phải folder proper-noun `*-pro-v2`):

| File | Dòng |
|---|---|
| `kitehub-story-v2/sections/pricing-cta.html:34` + `index.html:452` | `<div class="kh-tier__name">PRO</div>` |
| `kitehub-story-v2/README.md:40,112` | "3 tiers (FREE / PRO / PREMIUM)" |
| `ai-branding-wizard-v2/index.html:56` + 4 lifecycle/step screens | `tier-badge tier-pro">PRO` |

→ "PRO" có thể nên là "BASIC" (cần confirm canonical tier names qua pricing rules.md). 2 kit KiteHub-side affected. Low-effort fix (text swap) nhưng cần confirm mapping PRO→BASIC vs PRO=hợp lệ.

### 2.3 🟡 Domain + fabrication placeholder (landing-100 anti-fabrication class)

| File | Nội dung | Verdict |
|---|---|---|
| `marketing-site/index.html:36` (JSON-LD) | `"email": "info@kiteclass.vn", "telephone": "1900 6868"` | ⚠️ Domain `kiteclass.vn` mâu thuẫn GAP-458 (canonical = `kitehub.me`); `1900 6868` = hotline bịa (chưa có hotline Phase 1 BETA, per GAP-428 đã sửa `1900-xxxx`→email ở production). marketing-site vừa fix GAP-1223/1227 nhưng JSON-LD này lọt. |
| `kitehub-admin/screens/login.html:28` | "Nhập 1.000+ học sinh... 3.000 phụ huynh" | ⚠️ Marketing copy con số cụ thể — nếu là static landing copy thì OK (aspiration); nếu hiển thị như stat thật → fabrication. Cần review context. |
| `components/G11-theme-preview/states/*.html` | "500+ học viên", "25 khóa" | ✅ ACCEPTABLE — đây là **theme PREVIEW demo** (sample tenant content để preview theme), không phải claim của KiteClass. Exempt. |

→ marketing-site JSON-LD là item cụ thể nhất (domain stale + hotline bịa) — nên fold vào GAP-1227 class sweep ("mislabel/stale trong design docs").

### 2.4 Surface mislabel KH/KC (sweep nhẹ — recommend dedicated sweep)

Sweep README cho thấy surface-label chủ yếu đúng (kitehub-* ghi "KH brand sky blue + orange", kiteclass-* ghi blue). KHÔNG tìm thấy mislabel rõ ràng ngoài marketing-site (đã fix GAP-1227). `kitehub-pro-v2/README:186` reference subdomain `*.kiteclass.app` — đây là tenant-instance subdomain KH quản lý (hợp lệ, không phải mislabel). **Tuy nhiên sweep này nhẹ** (chỉ README, chưa quét port `:3001`/`:3000` trong từng index card + screen). Recommend: 1 bucket nhỏ chạy dedicated surface-label sweep 11 kit (GAP-1227 mới chỉ fix marketing-site → class chưa close ở 11 kit kia).

---

## 3. Persona refresh — 3 deliverable chính (rủi ro outside-in mà inside-out miss)

### (a) Student kit polish ≥105 (GAP-363b)
1. **Calibration trap:** self-rescore ~102.5 nhưng `feedback_audit_calibration.md` cảnh báo self overstate 15-20pt → external thật có thể 85-95, KHÔNG phải 102. Risk: ship "polish" tưởng đạt 105 nhưng external vẫn <100. → external re-audit (Step 1) BẮT BUỘC trước khi scope polish.
2. **Child-protection AC-FIN-001 (Option C):** payments.html fix parent-trigger, nhưng còn `notifications.html` parent-kép visualization + `login.html` parent-reset workflow ĐỀU deferred → child-protection flow chưa khép kín ở 2 màn này. Lọt nếu chỉ nhìn payments.
3. **Mobile 320px:** student kit là mobile-first PWA — cần verify breakpoint 320px (iPhone SE/máy rẻ phổ biến HS VN), report 2026-05-05 không nêu rõ.
4. **11/14 màn cluster 99-104:** muốn avg ≥105 phải lift 4-6 màn ~3-5pt mỗi màn — risk effort underestimate nếu external < self.

### (b) Admin kit cross-screen polish (GAP-364b)
1. **Dark-mode parity:** chỉ dashboard + report-cards có dark vars; 10 màn default-light. Inside-out dễ skip vì "light đủ rồi" — nhưng dark-mode là expectation 2026.
2. **Staff-vetting (AC-ONBOARD-005):** màn MỚI chưa tồn tại — child-protection LLTP vetting workflow. Outside-in: đây là compliance-critical (K-12 child protection) chứ không phải "polish", priority có thể cao hơn P2.
3. **Skeleton/empty-state per-screen:** 10 màn thiếu — ảnh hưởng perceived-performance khi tenant data rỗng (beta tenant mới = nhiều empty state).
4. **Zalo OA reusable:** hardcoded trong parent-comms.html — VN culture-critical (Zalo là kênh chính), nhưng chỉ 1 màn.

### (c) kiteclass-public kit MỚI ≥110 (GAP-428 + GAP-274)
1. **🔴 PORT-FROM-PRODUCTION, KHÔNG design lại:** production landing (`LandingClient.tsx` 1015 LOC) + pricing (`PricingContent.tsx`) ĐÃ VN-polish + Shadcn + WCAG AA tại Wave 78. Nếu wave design kit fresh → kit drift khỏi production đã polish (ngược chiều — production giờ là source of truth). Kit phải PORT TỪ production để làm baseline doc, KHÔNG vẽ mới. Đây là risk lớn nhất inside-out scope (GAP-428/274 AC viết "create kit" như greenfield, nhưng reality production đã đi trước).
2. **ConsentBanner PDPL:** GAP-274 AC đòi ConsentBanner mount trên landing (BR-PDPL-CONSENT-001..004, effective 2026-07-01) — kit phải reflect, dễ miss.
3. **Domain consistency:** kit public PHẢI dùng `kitehub.me` (GAP-458), KHÔNG `kiteclass.vn`/`kiteclass.com` — chính lỗi §2.3 marketing-site JSON-LD lặp lại.
4. **Anti-fabrication:** landing không được bịa "1900 hotline"/"500+ trường" — production đã sửa thành "Hỗ trợ qua email (Beta giai đoạn 1)". Kit phải kế thừa honesty đó.
5. **Tier table parity:** pricing kit phải dùng canonical tier names (xem §2.2) + khớp pricing rules.md, không "PRO".

---

## 4. Bucket verdict (per audit-to-gap-pipeline.md §2.6.1 completion check)

| Bucket | Gap | Scope còn đúng? | Residual delta chính xác | Effort verdict |
|---|---|---|---|---|
| **A** | GAP-363b | ✅ Đúng — 🔨 Delta | Wave 22 đã clear P0 (payments) + floor ≥95. Residual = external re-audit (calibration!) + lift 4-6 màn lên ≥105 + 2 deferred (notifications parent-kép / login parent-reset). | ~10-15h **vẫn đúng** NHƯNG phụ thuộc external re-audit Step 1: nếu external 85-95 (calibration) → effort cao hơn; nếu 102-104 → nhẹ hơn. Re-audit TRƯỚC khi commit polish effort. |
| **B** | GAP-364b | ✅ Đúng — 🔨 Delta | school-profile đã lift (107). Residual = skeleton(11)/empty-state(11)/dark-mode(10)/staff-vetting(NEW)/zalo-extract. | ~23h **vẫn đúng** NHƯNG: staff-vetting (AC-ONBOARD-005 child-protection) nên tách priority cao hơn P2 — compliance không phải polish. Cân nhắc split staff-vetting thành sub-bucket riêng. |
| **C** | GAP-428 + GAP-274 | ⚠️ Scope cần RE-FRAME — 🔨 Delta, KHÔNG greenfield | GAP-428 production pages ĐÃ PASS (Wave 78 VN-polish); chỉ kit prototype defer. GAP-274 AC viết "create kit ≥105" như greenfield nhưng production đã đi trước. **Reality: kit = PORT-FROM-production doc baseline, không design mới.** | Effort GAP-274 cũ "~1-2 weeks" **KHÔNG còn đúng** — port từ production polished thấp hơn nhiều design-fresh. Re-frame AC: "port production landing/pricing/catalog → kit ≥110" + ConsentBanner + domain `kitehub.me` + anti-fabrication. GAP-428 phần production = ✅ Already-shipped (chỉ giữ kit-prototype AC). |
| **F** | GAP-366 + GAP-367 | ⏳ Chưa đọc đầy đủ trong refresh này (unclassified/) — scope = "kit as source-of-truth" (366) + "kit production-parity skill" (367) = META/process gaps | Đây là META gaps (skill/process) per meta-gap-priority.md → priority cao hơn feature buckets A/B/C **nếu** chúng govern cách port kit→production. GAP-367 (parity skill) đặc biệt liên quan: nếu ship skill này TRƯỚC, Bucket C port sẽ dùng được skill. | Recommend đọc kỹ 366/367 khi lock plan; xét đưa lên đầu (META force-multiplier) vì govern A/B/C. |

---

## 5. Khuyến nghị cho wave plan `wave-ui-kits-100`

- **🔴 Ưu tiên #1 (leverage cao nhất): fix root font token.** Confirm Be Vietnam Pro vs Inter là canonical qua design-system doc (design-first), rồi fix `_shared/colors_and_type.css` 1 lần → cascade 10 kit + ship persistent detector `grep Inter` (cross-flow-bug-class-sweep §4.1). 1 file → 10 kit comply. Tách thành Bucket riêng (hoặc prepend) vì độc lập + force-multiplier.
- **Bucket C phải RE-FRAME** từ "create kit greenfield" → "port-from-production polished pages" (production Wave 78 giờ là source of truth). Cập nhật GAP-274 AC + effort estimate TRƯỚC khi spawn agent. GAP-428 production-part đánh ⚠️ Already-shipped, chỉ giữ kit-prototype AC.
- **Bucket A: chèn external re-audit (GAP-363b Step 1) làm GATE** trước polish effort — calibration trap (self 102.5 có thể = external 85-95). Đừng scope polish-to-105 trước khi biết external baseline thật.
- **Bucket B: tách `staff-vetting` (AC-ONBOARD-005)** ra khỏi "cosmetic polish" — child-protection compliance, priority P1 không phải P2.
- **Đọc kỹ GAP-366/367 (META) khi lock** — xét prepend vì govern cách port kit→production (force-multiplier per meta-gap-priority §3). GAP-367 parity skill nên ship trước Bucket C port.
- **Thêm 1 bucket nhỏ: surface-label + tier-name + domain sweep cho 11 kit** (GAP-1227 + GAP-1223 mới chỉ fix marketing-site cục bộ; class chưa close ở: tier "PRO" trong story-v2 + ai-branding, domain `kiteclass.vn` JSON-LD). Statically-detectable → ship detector luôn.
- **Persona AC bổ sung cho wave plan §1 Brainstorm:** mobile 320px (student), dark-mode parity (admin), ConsentBanner + anti-fabrication + domain `kitehub.me` (public kit) — 3 dimension inside-out scope dễ miss.

---

## Cross-link
- Staleness method: `audit-to-gap-pipeline.md` §2.8 + §2.6.1 bucket-completion
- Class-sweep: `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable → persistent detector)
- Canonical-confirm-before-fix: `design-first-investigation-order.md` (font token canonical phải đọc design doc trước)
- Boundary: `kitehub-kiteclass-boundary.md` §2.1
- Trigger: `outside-in-coverage-trigger.md` §2 (pre-lock wave scope)
