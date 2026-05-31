---
paths:
  - "kitehub/kitehub-frontend/**"
  - "kiteclass/kiteclass-frontend/**"
  - "kitehub/*/src/main/resources/templates/email/**"
  - "kitehub/*/src/main/resources/i18n/**"
  - "documents/05-guides/user-manual/**"
---

# VN-Localization Audit Checklist — cross-bucket pre-merge gate

**Priority:** 🟠 MANDATORY — cross-bucket VN-context governance
**Version:** 1.1.2
**Created:** 2026-05-19
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; adds §5 "Data roundtrip preservation through sanitization layers" — input sanitization (XSS escape / HTML escape / SQL escape / Unicode normalization) áp dụng cho tenant-facing field PHẢI có VN diacritic roundtrip test; paired same-PR Wave 106 GAP-764 fix (BetaAccessService.sanitizeFreeText UTF-8 preserve) + Flyway V57 backfill migration per §6.5 Enforcement Parity Mandate; META P0 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi future input sanitization (audit log / admin form / course content) auto-comply. v1.0.0 (kept): new rule với built-in enforcement (4-section checklist + reviewer-checklist + worked self-test trên Wave 100 4 buckets retroactive) per §6.5; closes coverage gap GAP-680 từ 3-audit consensus 2026-05-19; META P1 force-multiplier)
**Applies to:** Mọi PR thêm/sửa artifact tenant-facing (UI component, email template, dashboard chart, FE label, invoice template, BE response narrative, marketing copy, user manual page, runbook narrative) thuộc scope `kitehub/kitehub-frontend/**`, `kiteclass/kiteclass-frontend/**`, `kitehub/*/src/main/resources/templates/email/**`, `kitehub/*/src/main/resources/i18n/**`, `documents/05-guides/user-manual/**`, `documents/04-quality/audits/**` chứa user-facing narrative. Out-of-scope: code internal (Java/TS source identifiers + comments per `dev-readable-doc-language.md` §3), config keys, technical CSV column names.

---

## 1. The Rule

> **Mọi artifact tenant-facing trong cross-bucket scope PHẢI satisfy 4-section checklist §2 trước khi merge.** Reviewer enforce per-PR; CI grep detector deferred per `incident-to-rule-pipeline.md` §3 premature-rule guard ≥7 ngày (heuristic FP risk cao do code-switching tự nhiên trong narrative tiếng Việt).

`dev-readable-doc-language.md` §2 covers narrative language broadly. `user-manual-content-standard.md` §2 covers user manual scope narrow (15-item checklist gồm VND/date row 8 + VN sample row 7). **Gap được close bằng rule này:** cross-bucket VN-context (format + label + sample data + cultural awareness) — scope rộng hơn user manual nhưng narrow hơn dev-readable language broadly.

Force-multiplier per `meta-gap-priority.md` §3: 1 checklist chuẩn → mọi bucket Wave 100+ auto-comply → eliminate retroactive rework cost khi user catch English label / USD format / John Doe sample / Zalo culture conflict trong PR review.

Triggered bởi 3 outside-in audits Wave 100 thesis push 2026-05-19 (per audit reports `documents/04-quality/audits/persona-review/2026-05-18-thesis-{persona-demo,vn-saas-benchmark,defense-failure-mode-matrix}.md`): cả 3 agents đồng thuận VN-localization concern cross-bucket — Bucket A invoice VND format + Bucket B income KPI VND label + Bucket C email-only Zalo culture conflict + Bucket D thesis VN narrative.

---

## 2. The 4-section checklist (mandatory per PR)

### Section 1 — VND currency + date format

| Pattern | ✅ Required | ❌ Banned |
|---|---|---|
| **Currency invoice / dashboard KPI** | `1.500.000đ` HOẶC `1.500.000 ₫` | `$60.00`, `60 USD`, `60.00` (no currency marker) |
| **Date long narrative** | `Thứ Hai, 14/05/2026` | `Mon May 14, 2026`, `2026-05-14` (trong narrative) |
| **Date short** | `14/05/2026` | `2026-05-14` (narrative), `05/14/2026` (US format) |
| **ISO date trong frontmatter / code** | `2026-05-14` (OK — code scope) | `14/05/2026` (frontmatter cần parseable) |
| **Time** | `09:30` 24h preferred; `9 giờ 30 sáng` narrative acceptable | `9:30 AM` (US format banned narrative) |
| **Number thousands separator** | `1.500.000` (VN convention dot) | `1,500,000` (US convention comma) trong narrative |
| **Decimal separator** | `1.500.000,50` (VN comma) | `1,500,000.50` (US dot) |

**Rationale:** VN edu SaaS user (chị Hằng / anh Tâm) đọc số `60.00` không hiểu là USD; đọc `Mon May 14` không trực giác. Format đúng VN convention = trust signal + reduce support burden.

### Section 2 — Vietnamese label (UI + email + error message)

| Surface | ✅ Required | ❌ Banned |
|---|---|---|
| **Button / menu / nav label** | `Đăng nhập`, `Lưu`, `Hủy`, `Xác nhận`, `Xem trang chủ` | `Login`, `Save`, `Cancel`, `Confirm`, `View Home` |
| **Email subject line** | `Chào mừng bạn đến KiteHub!`, `Hóa đơn tháng 5/2026 từ Trung tâm Sky Education` | `Welcome to KiteHub!`, `May 2026 Invoice from Sky Education Center` |
| **Email body greeting** | `Em chào anh/chị,` (Owner formal), `Chào bạn,` (Solo casual), `Kính gửi quý phụ huynh,` (Parent) | `Hi`, `Hello`, `Dear` (translate cứng English habits) |
| **Error message** | `Email không hợp lệ`, `Mật khẩu phải ≥ 8 ký tự`, `Số tiền không đủ` | `Invalid email`, `Password must be ≥ 8 chars`, `Insufficient amount` |
| **Form field placeholder** | `Nhập email`, `Nhập số điện thoại (10 chữ số)` | `Enter email`, `Enter phone number (10 digits)` |
| **Form validation message** | `Vui lòng nhập đầy đủ thông tin`, `Email đã được đăng ký` | `Please fill in all fields`, `Email already registered` |
| **Toast / banner notification** | `Đã lưu thành công`, `Lỗi kết nối, vui lòng thử lại` | `Saved successfully`, `Connection error, please try again` |

**Acceptable English context (per `dev-readable-doc-language.md` §3):**
- Persona slug identifier: `P2_CENTER_OWNER`, `anonymous-prospect` (code-shaped, internal)
- Technical token natural: `JWT hết hạn, vui lòng đăng nhập lại` (JWT giữ English)
- Brand name proper noun: `KiteHub`, `KiteClass`, `Sky Education` (không dịch)
- Code-switching trong narrative: `Click nút 'Gửi' để submit` (English action verb trong VN context)

**Email tone matrix per persona:**

| Persona | Greeting | Tone | Example subject |
|---|---|---|---|
| **P1 Solo Teacher** | `Chào em,` / `Chào bạn,` | Casual, friendly | `Lớp học của em đã mở thành công!` |
| **P2 Center Owner** | `Em chào chị/anh,` | Formal-respectful | `Hóa đơn tháng 5/2026 cho Trung tâm Sky Education` |
| **P3 Center Manager** | `Em chào chị/anh,` | Formal | `Báo cáo doanh thu tháng 5 — Trung tâm Sky Education` |
| **Parent (P4)** | `Kính gửi quý phụ huynh,` | Very formal | `Thông báo điểm danh tuần — em Trần Thị Hồng lớp 5A1` |
| **Student** | `Chào em,` | Friendly | `Lịch học tuần này của em` |

### Section 3 — VN sample data

| Field | ✅ Required samples | ❌ Banned samples |
|---|---|---|
| **Tên người** | `Trần Thị Hồng`, `Nguyễn Văn An`, `Phạm Thị Mai`, `Lê Văn Quang` | `John Doe`, `Jane Doe`, `Alice Smith`, `Bob Jones` |
| **Tên trung tâm** | `Trung tâm Anh ngữ Sky Education`, `Trung tâm Toán Quang Minh`, `Trung tâm Tin học Bách Khoa` | `Example Center`, `Acme Inc.`, `Foo Bar Center`, `Test Center` |
| **Tên lớp** | `Lớp Anh ngữ 5A1`, `Lớp Toán 9B`, `Lớp IELTS 7.0 Buổi tối`, `Lớp Lập trình Python K12` | `Class A1`, `Class 5A1`, `Class 101`, `English Class A` |
| **Địa chỉ** | `123 Lê Lợi, Q.1, TP.HCM`, `45 Hai Bà Trưng, Hà Nội`, `Số 78 Nguyễn Trãi, Thanh Xuân, Hà Nội` | `123 Main St, Anytown`, `456 Example Rd`, `1234 Test Avenue` |
| **Số điện thoại** | `0901 234 567`, `0987 654 321`, `(024) 3826 0000` | `+1 555-0100`, `123-456-7890`, `1-800-EXAMPLE` |
| **Email** | `hong.tran@skyedu.vn`, `tam.nguyen@gmail.com`, `info@quangminh.edu.vn` | `john@example.com`, `test@test.com`, `user@foo.bar` |
| **Mã số thuế (MST)** | `0312345678` (10 chữ số tổ chức), `0312345678-001` (chi nhánh) | `123-45-6789` (US SSN format) |
| **Tài khoản ngân hàng** | `1234 5678 9012 3456` (Vietcombank), `0001 234 567 890` (Techcombank) | `4111-1111-1111-1111` (US test card pattern) |
| **Số tiền invoice/payment** | `1.500.000đ`, `15.000.000 VNĐ`, `850.000đ/tháng` | `$60.00`, `60.00 USD`, `€500.00` |

**Rationale:** Sample data VN-friendly = trust signal + tránh confusion (`John Doe` reader VN không biết là placeholder hay tên thật) + giảm onboarding friction khi user thấy ví dụ match context VN edu.

### Section 4 — VN cultural awareness

| Aspect | ✅ Required pattern | ❌ Anti-pattern |
|---|---|---|
| **Phone signup habit (Zalo culture)** | Email signup mặc định; SMS/Zalo path optional Phase 2+. Nếu remove SMS path → MUST document rationale + migration FAQ ("Vì sao chỉ email?") | Remove SMS/Zalo path không document → user confusion (VN edu user dùng phone OTP nhiều) |
| **Niên khóa Việt Nam** | Niên khóa 9-5 (`2025-2026` = Sep 2025 → May 2026); kỳ học 4 kỳ HK1/HK2/HK3/HK_Hè | Calendar year (`Jan-Dec 2026`) — VN edu không dùng |
| **Working day convention** | Trung tâm dạy thêm: tuần 6 ngày Mon-Sat (school weekday 5 + Sat overflow); Sun nghỉ | Mon-Fri only = US/EU office convention, không match VN edu |
| **Greeting trong email** | `Em chào chị/anh` (Owner formal Vietnamese), `Kính gửi quý phụ huynh` (Parent very formal) | `Hi Hằng,` (direct first-name English habit) — disrespectful trong VN context |
| **GVCN / Hiệu trưởng terminology** | `GVCN` (Giáo viên chủ nhiệm) cho lớp leader, `Hiệu trưởng` cho center principal, `Quản lý` cho center manager | `Class teacher`, `Principal`, `Manager` — Anglicized terminology |
| **Phong tục thanh toán** | Bank transfer dominant (Vietcombank/Techcombank/MB), QR code (VietQR/Momo) growing; cash receipt vẫn common | Credit card "default" — VN edu user ít dùng credit card cho fee học |
| **Phụ huynh thoại chính** | Mother (mẹ) thường là primary contact cho child education; father (bố) backup; ông bà occasional | Single "parent" generic — miss mother-primary VN convention |
| **Tết / Lễ holidays** | Tết Nguyên Đán 7-10 ngày off (late Jan/early Feb); 30/4 + 1/5 (5 ngày off); Giải phóng MN; nhiều lớp pause Tết | Western holidays (Christmas, Thanksgiving) — không relevant VN K-12 |
| **Học thêm vs. trường công** | Trung tâm = học thêm sau giờ school + cuối tuần; trường công ≠ trung tâm | Treat all "school" generic — miss distinction học thêm market (KiteHub primary persona) |
| **Phụ huynh communication style** | Zalo group chat dominant cho parent ↔ center; SMS backup; email secondary cho formal docs (invoice/report) | Email-only "modern" → miss Zalo group chat reality |

**Rationale:** VN edu market có cultural conventions khác US/EU SaaS reference (Stripe / Slack / Notion). Inside-out brainstorm dev không tự surface được do đã quá quen US convention. Outside-in audit (per `outside-in-coverage-trigger.md` v1.1.0) là cách primary để catch culture conflict.

### Section 5 — Data roundtrip preservation through sanitization layers (added v1.1.0)

Mọi input sanitization (XSS escape / HTML escape / SQL escape / Unicode normalization) áp dụng cho tenant-facing field PHẢI preserve Vietnamese diacritic roundtrip — input → DB write → DB read → response serialize → display → ALL preserved unchanged.

| Sanitization scope | ❌ Banned | ✅ Required |
|---|---|---|
| **HTML escape (Spring `HtmlUtils.htmlEscape`)** | `HtmlUtils.htmlEscape(input)` single-arg — escapes ALL non-ASCII as numeric entity (corrupts `â → &acirc;`) | `HtmlUtils.htmlEscape(input, "UTF-8")` two-arg — escapes ONLY 5 XSS chars `<>&"'`, preserves VN diacritic raw |
| **JSON serialize (Jackson)** | `jackson.escape-non-ascii: true` in config (escapes VN diacritic as `\uXXXX`) | Default Jackson UTF-8 — raw VN chars in JSON response body |
| **SQL parameterization** | Manual string concat với HTML-escaped values | PreparedStatement bind UTF-8 raw |
| **Unicode normalization** | `Normalizer.normalize(input, Form.NFKD)` strips combining marks | `Normalizer.normalize(input, Form.NFC)` preserves precomposed VN chars |
| **HTML tag strip regex** | Regex matching `[^a-zA-Z0-9 ]` (strips all VN diacritic) | Regex matching `<[^>]*>` only (XSS tags), preserves text content |

**Mandatory roundtrip test** — every PR adding/modifying input sanitization touching tenant-facing field MUST include integration test (per `postgres-specific-type-testcontainers.md` v1.0.0 mandate):

```java
// EXAMPLE — kitehub-subscription BetaAccessServicePostgresIT
@Test void vn_diacritic_roundtrip_preserved() {
    BetaAccessRequest req = service.requestBetaAccess(new BetaSignupRequest(
        "Trần Thị Hồng",                          // diacritic: ầ ị ồ
        "hong@test.vn",
        "0901234567",
        "Trung tâm Anh ngữ Sky Education",        // diacritic: â ữ
        "P2_CENTER_OWNER",
        null, true, ""
    ));
    repository.flush();
    BetaAccessRequest reloaded = repository.findById(req.getId()).orElseThrow();
    assertThat(reloaded.getName()).isEqualTo("Trần Thị Hồng");         // raw, NOT "Tr&agrave;n..."
    assertThat(reloaded.getOrgName()).isEqualTo("Trung tâm Anh ngữ Sky Education");
    // Validate JSON response shape too
    ResponseEntity<BetaRequestResponse> resp = restTemplate.postForEntity(...);
    assertThat(resp.getBody().orgName()).isEqualTo("Trung tâm Anh ngữ Sky Education");
}
```

**Test data MUST include** all 7 VN-frequent diacritics: `â ê ô ữ ồ ằ ấ` (covers ~95% VN names). Edge cases: precomposed vs combining form, lowercase + uppercase.

**Rationale:** Wave 106 GAP-764 (2026-05-27) — Wave 105 Bucket E0 Bug 2 added `HtmlUtils.htmlEscape(input)` defense-in-depth XSS sanitization. Single-arg variant escapes ALL non-ASCII chars → Vietnamese `â/ê/ô` got corrupted to `&acirc;/&ecirc;/&ocirc;` BEFORE DB write. Cost: 2 production rows corrupted (id=11, 12), retroactive Flyway backfill migration V57, RST walk to discover. Counterfactual với rule §5 at Wave 105 Bucket E0 design: reviewer-checklist + Testcontainers IT would catch ngay tại PR review → 0 production corruption.

---

## 3. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Hardcode `$60.00` trong invoice template "vì copy từ Stripe sample" | `1.500.000đ` VND format per Section 1 |
| `Welcome to KiteHub!` email subject English | `Chào mừng bạn đến KiteHub!` Vietnamese per Section 2 |
| `John Doe` sample data trong unit test fixture / docs | `Trần Thị Hồng` VN-friendly per Section 3 |
| Remove SMS signup path không document rationale | Document email-only rationale + migration FAQ per Section 4 |
| `Hi Hằng,` greeting trong email Owner | `Em chào chị Hằng,` formal-respectful per Section 2 |
| `Class A1` trong sample data | `Lớp Anh ngữ 5A1` VN convention per Section 3 |
| `Mon May 14, 2026` date format trong narrative | `Thứ Hai, 14/05/2026` per Section 1 |
| Treat parent generic "user" trong email template | Differentiate mother-primary + father-backup per Section 4 |
| US business hours Mon-Fri 9-5 trong scheduling | VN edu Mon-Sat 9-21 (evening classes common) per Section 4 |
| Email-only invoice delivery (no Zalo backup) | Zalo group chat reminder + email formal doc per Section 4 |
| `Dear Customer,` impersonal greeting | Persona-specific greeting per Section 2 tone matrix |
| Hardcode `+1 555-0100` test phone | `0901 234 567` VN mobile format per Section 3 |

---

## 4. Enforcement (per `rule-change-process.md` §6.5)

### 4.1 Reviewer-checklist (active now — primary enforcement)

Pre-merge review cho PR touching tenant-facing scope:

- [ ] **Section 1 — VND + date format:** mọi số tiền `1.500.000đ`? Date `Thứ Hai, 14/05/2026`?
- [ ] **Section 2 — Vietnamese label:** button/email/error tiếng Việt? Greeting đúng persona tone?
- [ ] **Section 3 — VN sample data:** `Trần Thị Hồng` không `John Doe`? `Lớp 5A1` không `Class A1`?
- [ ] **Section 4 — VN cultural awareness:** Zalo culture documented nếu touch signup? Niên khóa 9-5? Mon-Sat?
- [ ] Cross-reference `dev-readable-doc-language.md` §4 mixed-language pattern OK (English technical token trong VN narrative)
- [ ] Sample data cross-reference `user-manual-content-standard.md` §2 row 7 (mở rộng scope user manual narrow → cross-bucket)

### 4.2 PR template (active — extend existing checklist)

Thêm row vào `.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist:

```markdown
- [ ] **VN-localization audit** — nếu PR touching tenant-facing scope (UI label / email template / invoice / dashboard / user manual narrative), 4-section checklist (VND format / Vietnamese label / VN sample data / VN cultural awareness) satisfied per `.claude/rules/vn-localization-audit-checklist.md` §2
```

### 4.3 CI grep detector (HONEST DEFER — heuristic FP risk inherently high)

Per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions:

- **Detector complexity:** English-narrative-in-VN-context detection inherently ambiguous — code-switching natural per `dev-readable-doc-language.md` §4 (technical tokens `HTTP`, `JWT`, `JSON`, `JWT` valid English trong Vietnamese sentences)
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-19; recurrence count starts từ Wave 100+ ship date)
- **FP risk:** Very high — every acceptable English token (per `dev-readable-doc-language.md` §3) trigger false positive
- **Decision:** Reviewer-checklist §4.1 + worked self-test §6 (Wave 100 4 buckets retroactive) sufficient cho v1.0.0; revisit detector when recurrence-count ≥2 OR proven NLP language classifier available

Future heuristic regex (when implemented, WARN-mode):

```bash
# Detect USD currency trong narrative (skip code blocks)
grep -rnE '\$[0-9]+\.[0-9]+|\b[0-9]+ USD\b' \
  kitehub/kitehub-frontend/src/ kitehub/*/src/main/resources/templates/ \
  documents/05-guides/user-manual/ 2>/dev/null \
  && { echo "WARN: USD currency in tenant-facing artifact — use VND per vn-localization-audit-checklist.md §1"; exit 0; }

# Detect English placeholder names
grep -rnE "John Doe|Jane Doe|Alice Smith|Bob Jones|Example Center|Class [A-Z][0-9]+" \
  kitehub/kitehub-frontend/src/ kitehub/*/src/main/resources/templates/ \
  documents/05-guides/user-manual/ 2>/dev/null \
  && { echo "WARN: English placeholder data — use VN sample per vn-localization-audit-checklist.md §3"; exit 0; }
```

WARN-only (false positives expected — code internal strings legitimate English). Track follow-up gap khi rule stabilize ≥7 ngày sau Wave 100.

### 4.4 Memory auto-load (deferred)

Memory entry `feedback_vn_localization_audit_checklist.md` could remind tại session start trước khi touch tenant-facing scope. Defer per `incident-to-rule-pipeline.md` §3.1 premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test §6 đủ cho v1.0.0.

### 4.5 Override mechanism

Genuine exception (vd vendor template chỉ support English locale Phase 1, regulator template English-only):

```
git commit -m "...
VN_LOCALIZATION_OVERRIDE: <artifact-path> — <reason — e.g., 'Stripe checkout vendor template English-only Phase 1.5, defer Phase 2 i18n integration'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review.

---

## 5. Worked self-test — Wave 100 4 buckets retroactive

Apply 4-section checklist trên scope dự kiến mỗi bucket Wave 100 (per wave plan §3 Scope), validate rule fires correctly:

### 5.1 Bucket D — GAP-650 Part 1 thesis chapter 1

| Section | Concern | Required check |
|---|---|---|
| **§1 VND/date** | Citation date format trong bibliography IEEE | `[Accessed 2026-05-19]` ISO trong code/citation OK; narrative date "ngày 19/05/2026" |
| **§2 Vietnamese label** | Chapter heading + section titles | `Chương 1: Tổng quan` (NOT `Chapter 1: Introduction`) — Vietnamese narrative mandate per `dev-readable-doc-language.md` |
| **§3 VN sample data** | Competitor analysis sample case studies | `Trung tâm Anh ngữ Sky Education` (NOT `Acme Education Inc.`) |
| **§4 VN cultural awareness** | AI techniques discussion in VN edu context | Cite VN edu use cases (auto-grading bài tập tiếng Anh, sinh nội dung học cho học sinh THCS) NOT generic US K-12 |

**Verdict Bucket D:** all 4 sections applicable; expected PASS post-rule landing (this PR demonstrates Part 1 ship với rule compliance).

### 5.2 Bucket C — GAP-286 email-only signup

| Section | Concern | Required check |
|---|---|---|
| **§1 VND/date** | Migration timeline "phased 2 tuần" | `Trong vòng 14 ngày (từ 19/05 đến 02/06)` VN date format trong narrative |
| **§2 Vietnamese label** | FAQ "Vì sao chỉ email?" + landing copy | Vietnamese narrative `Vì sao chỉ email?` (NOT `Why email-only?`); error message `Vui lòng nhập email hợp lệ` |
| **§3 VN sample data** | Migration FAQ examples | `Sample: chị Hằng (Owner) đăng nhập email hong@skyedu.vn` (NOT `John Doe with john@example.com`) |
| **§4 VN cultural awareness** | **CRITICAL** — Zalo culture conflict | MUST document rationale removing SMS path (Phase 1 cost saving + Zalo culture override) + migration FAQ + Phase 2 mobile OTP roadmap |

**Verdict Bucket C:** §4 Zalo culture awareness = critical risk per 3-audit consensus failure-mode matrix C5; rule fires correctly cho concern này.

### 5.3 Bucket A — GAP-297 batch invoice generator

| Section | Concern | Required check |
|---|---|---|
| **§1 VND/date** | Invoice template số tiền + due date | `Tổng cộng: 1.500.000đ` VND format; due date `Hạn thanh toán: 25/05/2026` |
| **§2 Vietnamese label** | Email subject + invoice header | Subject `Hóa đơn tháng 5/2026 — Trung tâm Sky Education`; header `HÓA ĐƠN ĐIỆN TỬ` per VN convention |
| **§3 VN sample data** | Test fixture invoice | Tenant `Trung tâm Anh ngữ Sky Education`, customer `Trần Thị Hồng`, MST `0312345678` |
| **§4 VN cultural awareness** | Payment method preference + Tết pause | Bank transfer + VietQR primary; cash receipt option; defer billing run Tết window (cron skip Jan 25-Feb 5) |

**Verdict Bucket A:** all 4 sections applicable; eInvoice VAT integration prep stub MUST follow VN Thông tư 78/2021/TT-BTC format (per §1 VND mandatory + §4 cultural payment preference).

### 5.4 Bucket B — GAP-293 income dashboard

| Section | Concern | Required check |
|---|---|---|
| **§1 VND/date** | 3 KPI cards + 12-month chart | KPI values `1.500.000đ` format; chart x-axis `T1/2026, T2/2026, ..., T5/2026` (NOT `Jan 2026, Feb 2026`) |
| **§2 Vietnamese label** | KPI titles + chart labels + MoM/YoY annotation | `Doanh thu tháng` (NOT `Monthly Revenue`); `Tăng/giảm so với tháng trước` (NOT `MoM Change`); `+12.5% so với cùng kỳ năm trước` (NOT `+12.5% YoY`) |
| **§3 VN sample data** | Per-class per-branch breakdown samples | `Lớp Anh ngữ 5A1: 12.000.000đ`, `Chi nhánh Quận 1: 45.000.000đ` |
| **§4 VN cultural awareness** | Solo simplified variant tone + persona expectation | Solo persona: friendly 1 KPI + sparkline (NOT enterprise full dashboard); Owner persona: formal 3 KPI + breakdown |

**Verdict Bucket B:** §2 VN label critical cho mọi chart annotation; §1 VND mandatory cho mọi giá trị; rule fires correctly.

### 5.5 META cross-cut summary

| Bucket | §1 VND/date | §2 VN label | §3 VN sample | §4 VN culture | Critical concern |
|---|:---:|:---:|:---:|:---:|---|
| **D** | ✅ | ✅ | ✅ | ✅ | AI techniques VN edu use case |
| **C** | ✅ | ✅ | ✅ | 🔴 critical | Zalo culture migration rationale |
| **A** | ✅ | ✅ | ✅ | ✅ | Tết cron skip + bank transfer primary |
| **B** | ✅ | ✅ | ✅ | ✅ | Solo vs Owner persona tone diff |

**Verdict 4-buckets:** 16/16 section × bucket combinations applicable; rule fires correctly on all Wave 100 buckets. Self-test PASS ✅

**Counterfactual without rule:** 4 buckets ship với potential English label / USD format / John Doe sample / Zalo culture conflict — user catch trong PR review round-trip (recurrence Wave 72a Bucket F CSV English narrative precedent 2026-05-14) → ~30 min retro round-trip per violation × 4 buckets = ~2 hours total session friction eliminated by rule.

---

## 6. Relationship to other rules

- **`dev-readable-doc-language.md`** §2-§4 — sister rule cover narrative language (Vietnamese for dev-readable docs); rule này extend với format + cultural awareness layers cho tenant-facing scope
- **`user-manual-content-standard.md`** §2 row 7-8 — covers VND format + VN sample data CHỈ cho user manual scope; rule này mở rộng cross-bucket (UI / email / invoice / dashboard)
- **`outside-in-coverage-trigger.md`** v1.1.0 §3 — triggered rule này filing per 3-audit consensus Wave 100 (META-audit cross-cut pattern)
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn cross-bucket → mọi PR subsequent auto-comply)
- **`output-review-mandate.md`** §3 — paired same-PR với new matrix row "VN-localization audit checklist (cross-bucket)" tracking this rule's review standard
- **`incident-to-rule-pipeline.md`** — applied 5-stage: Detect ✓ (3-audit consensus Wave 100) → Classify ✓ (no existing rule cover cross-bucket VN-context; sister rules cover narrative-only OR user-manual-narrow) → Rule+Enforce ✓ (this file + §6.5 paired §2 matrix row + reviewer-checklist + worked self-test §5 trên Wave 100 4 buckets) → Self-Test ✓ (§5 worked example 4 buckets × 4 sections = 16/16 PASS) → Retro Log ✓ (§7 below + GAP-680 closure)
- **`rule-change-process.md`** §5.1 atomic-unique-bar — passed: ✅ atomic concept (cross-bucket VN-context audit checklist) / ✅ unique scope (sister rules narrative-only + user-manual-narrow đều khác scope) / ✅ widely applicable (mọi PR tenant-facing scope) / ✅ body discipline (§1 ≤2 "and" conjunctions)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist §4.1 + PR template row §4.2 + worked self-test §5 + paired GAP-680 closure all ship same PR (this Wave 100 Bucket D PR)
- **`dev-readable-doc-language.md`** §2 row "Architecture docs" — adjacent scope cho `documents/02-architecture/**`; rule này focuses tenant-facing scope; sister rule covers technical narrative
- **`docs-folder-structure.md`** + **`docs-filename-prefix-convention.md`** — adjacent governance cho `documents/05-guides/user-manual/**` placement; rule này focuses content discipline within those folders

---

## 7. Log

- **2026-05-31** (v1.1.2): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~31k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-28 (v1.1.1):** PATCH — fixed §8 Applies-to scope path `kitehub/kiteclass-frontend/**` → `kiteclass/kiteclass-frontend/**` (KiteClass FE thật ở top-level `kiteclass/`, không phải dưới `kitehub/`). Same path-bug class surfaced bởi GAP-802 cross-flow sweep (PR #1958) — agent đã copy path sai từ rule này. Sync `rules-index.csv` path_trigger. No constraint change; rule scope giữ nguyên, chỉ sửa glob trỏ đúng dir để auto-load fire đúng. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — path correction, no constraint loosening). Cross-ref GAP-803 Finding 0.
- **2026-05-27 (v1.1.0):** MINOR — added §5 "Data roundtrip preservation through sanitization layers". Triggered by Wave 106 GAP-764 (P0 escalation) — Wave 105 Bucket E0 Bug 2 introduced `HtmlUtils.htmlEscape(input)` single-arg variant defense-in-depth XSS sanitization that corrupts Vietnamese diacritic `â/ê/ô` to HTML entities `&acirc;/&ecirc;/&ocirc;` BEFORE DB write. RST walk Mảng A2 caught 2 production rows corrupted (id=11, 12 trong `beta_access_request` table). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (Wave 106 RST A2 walk POST probe + DB row inspection 2026-05-27) → Classify ✓ (existing §1-§4 cover format + label + sample data + cultural awareness BUT không cover data preservation through sanitization layers; sister rules `postgres-specific-type-testcontainers.md` covers DB binding type only, `audit-service-isolation.md` covers transaction propagation — none cover sanitization-vs-i18n conflict) → Rule+Enforce ✓ (this §5 + paired same-PR with code fix BetaAccessService.sanitizeFreeText UTF-8 mode + Flyway V57 backfill migration + Testcontainers IT VN diacritic roundtrip + GAP-764 closure per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (Wave 106 GAP-764 originating incident — counterfactual: rule §5 at Wave 105 Bucket E0 design would catch via reviewer-checklist + IT test) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi future input sanitization (audit log / admin form / course content / student name / class name) auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds previously-uncovered scope "data preservation through sanitization"; no constraint loosening; existing sanitization code grandfathered until next refresh; rule applies prospectively từ Wave 106+ forward).

- **2026-05-19 (v1.0.0):** Rule created in response to 3-audit consensus Wave 100 thesis push 2026-05-19 (per `documents/04-quality/audits/persona-review/2026-05-18-thesis-{persona-demo,vn-saas-benchmark,defense-failure-mode-matrix}.md`). 3 outside-in agents (persona simulation + failure-mode matrix + VN edu SaaS benchmark) independently identified cross-bucket VN-localization concern Wave 100 4 buckets (A invoice VND + B income KPI VND + C email-only Zalo culture + D thesis VN narrative). Filed GAP-680 P1 META 2026-05-19 same session; rule body shipped Wave 100 Bucket D PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (3-audit consensus) → Classify ✓ (no existing rule covers cross-bucket VN-context; `dev-readable-doc-language.md` covers narrative-only; `user-manual-content-standard.md` §2 row 7-8 covers VND/date format CHỈ user manual scope narrow) → Rule+Enforce ✓ (this file 4-section checklist + reviewer-checklist §4.1 + PR template row §4.2 + worked self-test §5 on Wave 100 4 buckets + paired same-PR output-review-mandate §3 matrix row + rules-index.csv row + GAP-680 closure per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example 4 buckets × 4 sections = 16/16 applicable cells PASS; rule fires correctly; counterfactual ~2h session friction eliminated) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix checklist 1 lần → mọi bucket subsequent (Wave 100+ Wave 101+) auto-comply. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying cross-bucket VN-context governance previously-uncovered scope; no constraint loosening; existing artifacts grandfathered per `rule-change-process.md` convention; rule applies prospectively từ Wave 100 forward). CI grep detector (§4.3) + memory auto-load (§4.4) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (heuristic FP risk inherently high cho English-narrative-in-VN-context detection; reviewer-checklist + worked self-test §5 sufficient v1.0.0; revisit when recurrence-count ≥2 OR proven NLP classifier available). Atomic-unique-bar §5.1 check passed: atomic concept (cross-bucket VN-context audit) / unique scope (sister rules narrative-only OR user-manual-narrow đều khác) / widely applicable (mọi tenant-facing PR) / body discipline §1 has ≤2 "and" conjunctions.
