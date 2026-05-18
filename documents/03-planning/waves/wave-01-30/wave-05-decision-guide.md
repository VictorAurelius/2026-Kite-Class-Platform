---
title: Wave 5 Decision Guide — 6 câu hỏi mở + best-practice defaults
status: approved
created: 2026-04-19
updated: 2026-04-24
waves: [5]
gaps: [GAP-047]
approved_by: nguyenvankiet
approved_at: 2026-04-24
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 5 — Decision Guide (tiếng Việt)

**Mục đích:** Giúp reviewer (hoặc user) trả lời 6 câu hỏi mở trong `wave-05-document-generation.md` §9 trước khi Sub-PR 5.0 start. Mỗi câu hỏi có: bối cảnh, kiến thức technical, so sánh lựa chọn, và **default khuyến nghị** theo best practice cho SaaS giáo dục.

**Cách dùng:**
- Đọc từng câu hỏi, hiểu trade-off
- Nếu OK với default → tick "approved", chuyển sang câu tiếp
- Nếu muốn khác → note lý do + impact trong §Log của wave plan

---

## Câu 1: iText 7 vs Apache PDFBox (PDF library)

### Bối cảnh
Wave 5 cần 1 thư viện tạo PDF ở backend (Java). Có 2 ứng viên phổ biến cùng với 1 option bridge.

### Kiến thức technical

**iText 7**
- License: **AGPL v3** OR commercial (~$2,000+/năm per server)
- AGPL §13 (Network clause): nếu iText chạy trên server và output tới user qua network = "conveyed" → phải offer source dưới AGPL. Với SaaS = phải open-source toàn bộ codebase hoặc mua commercial.
- Ưu: high-level layout engine, HTML→PDF built-in, digital signatures, PDF/A archival, rich CJK/Vietnamese support
- Nhược: license tốn kém cho closed-source SaaS

**Apache PDFBox**
- License: **Apache 2.0** (permissive, không copyleft)
- Ưu: miễn phí, không ràng buộc; core library ổn định
- Nhược: API low-level, không có HTML parser → phải viết layout code tay cho từng block

**OpenHTMLtoPDF** (bridge, nên xem như option 3)
- License: **LGPL 3 / MIT** (permissive)
- Dùng PDFBox làm backend, nhận HTML + CSS (~CSS 2.1 + một phần CSS 3) → xuất PDF
- Ưu: viết Thymeleaf template → HTML → PDF, cùng pattern với Gateway đang dùng cho email (đã có trong dự án)
- Nhược: không support HTML5 mới nhất, cần test CSS cho Vietnamese fonts

### So sánh nhanh

| Tiêu chí | iText 7 | PDFBox | OpenHTMLtoPDF |
|----------|:-------:|:------:|:-------------:|
| License OK cho SaaS closed-source | ❌ (cần commercial) | ✅ | ✅ |
| HTML→PDF out-of-box | ✅ | ❌ | ✅ |
| Vietnamese Unicode | ✅ | ✅ (cần load TTF) | ✅ (cần load TTF) |
| Dev speed (invoice template) | Nhanh nhất | Chậm (layout tay) | Nhanh (tái dùng Thymeleaf) |
| Chi phí license | $2-5k/năm | $0 | $0 |
| Maturity | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### 🎯 Default: **OpenHTMLtoPDF + PDFBox backend**

**Lý do:**
1. License Apache/LGPL hoàn toàn compatible với SaaS closed-source
2. Reuse Thymeleaf skill đã có (Gateway đang dùng cho email templates)
3. Invoice/certificate/transcript = perfect fit cho HTML→PDF pattern
4. Nếu sau này cần PDF phức tạp (digital signature, PDF/A), có thể thêm iText như library phụ cho use case đó và mua commercial license cho module tách biệt

**Khi nên chọn khác:**
- Chọn **iText 7 AGPL** nếu KiteClass sẽ open-source toàn bộ codebase (hiện tại không phải kế hoạch)
- Chọn **iText 7 commercial** nếu cần PDF/A-3 archival hoặc digital signature ngay từ Wave 5 (chưa có use case)
- Chọn **PDFBox pure** nếu muốn kiểm soát layout 100% tay (overkill cho invoice đơn giản)

### References
- iText AGPL FAQ: https://itextpdf.com/resources/faq
- OpenHTMLtoPDF GitHub: https://github.com/danfickle/openhtmltopdf
- Thymeleaf: đã có trong `kiteclass-gateway/pom.xml`

---

## Câu 2: Maven module split vs inline trong `kiteclass-core`

### Bối cảnh
Wave 5 thêm ~5k LoC (4 generators + templates + tests) + dependencies (PDFBox ~3MB, POI ~15MB). Cần quyết định: tạo Maven module riêng `kiteclass-document-gen` hay để trong `kiteclass-core`?

### Kiến thức technical

**Maven multi-module project**
- Pros: dependency boundary rõ ràng, reusable cross-service, test isolation
- Cons: build time tăng (~15-30%), IDE navigation phức tạp hơn, thêm boilerplate pom.xml

**Inline trong kiteclass-core**
- Pros: đơn giản, 1 JAR deploy, IDE click-through dễ
- Cons: core JAR phình ra (~900MB → ~920MB), khó reuse nếu kitehub cần sau

### Tiêu chí khi nào nên split (design heuristics)

Split khi:
- Module >10k LoC
- ≥3 services depend vào module
- Module có release cycle riêng
- Team size đủ lớn để own module tách biệt

Không split khi:
- MVP, YAGNI (You Aren't Gonna Need It)
- Chỉ 1 service consume
- Team nhỏ

### So sánh cho Wave 5

| Tiêu chí | Inline (core) | Split (kiteclass-document-gen) |
|----------|:-------------:|:------------------------------:|
| LoC hiện tại | <5k | <5k |
| Services consume | 1 (core) | 1 (core) |
| Future: kitehub dùng? | Có thể (brand export) | Có thể |
| Build time impact | Thấp | Cao hơn ~20% |
| YAGNI score | ✅ Pass | ❌ Fail (premature) |

### 🎯 Default: **Inline trong `kiteclass-core` cho Wave 5. Extract sau khi kitehub bắt đầu consume.**

**Lý do:**
1. YAGNI — chưa có >1 consumer, split bây giờ là premature abstraction
2. CLAUDE.md rule: "Don't add features, refactor, or introduce abstractions beyond what the task requires"
3. Extract sau = refactor dễ (1 lần pháp/gitflow), không risk lost work
4. 20MB dependency bloat trong core JAR không đáng quan ngại (~2%)

**Khi nên extract sớm:**
- Nếu Wave 5 scope mở rộng thành 10+ generators (không phải kế hoạch hiện tại)
- Nếu biết chắc kitehub-branding Wave 6 sẽ consume (chưa confirmed)

**Follow-up gap** nếu default được approve:
- Tạo gap "Extract kiteclass-document-gen module" với trigger: khi kitehub đầu tiên add dependency

---

## Câu 3: 4 template stubs đã đủ cho launch chưa?

### Bối cảnh
Wave 5 plan §2.4 ship 4 template stubs: invoice PDF, attendance Excel, teacher contract Word, marketing pitch PPT. User hỏi: đủ cho launch SaaS chưa?

### Kiến thức — templates cần thiết cho SaaS giáo dục

**MUST-HAVE cho MVP launch:**
| Template | Format | Frequency | Ai dùng |
|----------|:------:|:---------:|---------|
| Hóa đơn học phí | PDF | Hàng tháng | Phụ huynh |
| Biên lai thu tiền | PDF | Theo lần | Phụ huynh |
| Báo cáo điểm danh | Excel | Tuần/tháng | Giáo viên + admin |
| Bảng điểm học kỳ | PDF | Cuối kỳ | Phụ huynh + học sinh |

**SHOULD-HAVE (trong 3 tháng sau launch):**
| Template | Format | Frequency |
|----------|:------:|:---------:|
| Chứng chỉ hoàn thành khóa | PDF | Cuối khóa |
| Hợp đồng giáo viên | Word | Khi tuyển |
| Đơn đăng ký học | PDF/Word | Khi đăng ký |
| Báo cáo tài chính tháng | Excel | Cuối tháng |

**NICE-TO-HAVE:**
| Template | Format |
|----------|:------:|
| Slide giới thiệu trung tâm | PPT |
| Tài liệu đào tạo giáo viên | PPT/PDF |
| Thư mời sự kiện | Word/PDF |

### Phân tích 4 stubs của Wave 5

| Stub | Category | Đánh giá |
|------|:--------:|----------|
| Invoice PDF | MUST | ✅ đúng |
| Attendance Excel | MUST | ✅ đúng |
| Teacher contract Word | SHOULD | 🟡 OK placeholder; legal review phải defer |
| Marketing pitch PPT | NICE | 🟡 có thể defer |

### 🎯 Default: **4 stubs ĐỦ cho Wave 5 acceptance criteria nhưng KHÔNG đủ cho launch.**

**Quan trọng: tách biệt 2 milestone**
- **Wave 5 goal** = "chứng minh skills infrastructure hoạt động" → 4 stubs đủ
- **Launch-ready** = đủ template cho tenant dùng ngay → cần ~8-10 templates

**Lý do không mở rộng Wave 5:**
1. Wave XL rồi (~14h parallel wall-clock), thêm template = scope creep
2. Templates cần legal/brand review riêng (per `output-review-mandate.md`)
3. Tạo Wave 7 sau riêng cho template library expansion (5-7 templates nữa)

**Action:**
- Giữ nguyên 4 stubs trong Wave 5
- Sau Wave 5 merge → tạo **GAP-104: Launch template library expansion** (MUST: certificate, transcript, biên lai, báo cáo tài chính tháng)
- Wave 7 thực hiện GAP-104 (~1 tuần)

**Khi nên expand Wave 5:**
- Nếu user có deadline launch trong 2 tuần và không có thời gian làm Wave 7 (escalate scope)

---

## Câu 4: FE preview vs direct download

### Bối cảnh
Khi user click "Download invoice" hay "Generate report", backend tạo file rồi trả về. Hỏi: có cần preview trên browser trước khi download không, hay cứ download luôn?

### Kiến thức — UX patterns

**Pattern A: Direct download**
```
User click "Download" → BE generate → return bytes + Content-Disposition: attachment
→ Browser lưu file → user mở Acrobat/Excel
```
- Đơn giản nhất, 1 API call
- UX: user không biết file trông thế nào đến khi mở app ngoài
- Mobile: hoạt động (browser download)

**Pattern B: Preview in-browser, then download**
```
User click "View" → BE generate → return URL hoặc bytes
→ FE hiển thị trong iframe/PDF.js
→ User click "Download" → file lưu xuống
```
- Trải nghiệm tốt hơn cho invoice/contract (user muốn xem trước)
- Cần work thêm cho mỗi format

**Công nghệ cho preview theo format:**
| Format | Preview method | Complexity |
|--------|----------------|:----------:|
| PDF | `<iframe>` + browser's built-in PDF viewer HOẶC PDF.js | 🟢 Thấp (browser native) |
| Excel | Convert to HTML table **hoặc** Google Docs viewer iframe | 🟠 Medium |
| Word | Convert to PDF first (thêm 1 step) | 🔴 Cao |
| PPT | Convert to images hoặc PDF | 🔴 Cao |

### So sánh

| Format | Direct download | Preview | Khuyến nghị Wave 5 |
|--------|:---------------:|:-------:|:-------------------:|
| PDF | Simple | Browser built-in PDF viewer miễn phí | **Preview** (gần như free) |
| Excel | Simple | Phức tạp | **Download only** |
| Word | Simple | Rất phức tạp | **Download only** |
| PPT | Simple | Rất phức tạp | **Download only** |

### 🎯 Default: **PDF preview in-browser; Excel/Word/PPT download-only cho Wave 5.**

**Chi tiết implementation:**
- BE trả PDF bytes kèm `Content-Disposition: inline` (không attachment) cho endpoint `/preview`
- BE trả PDF bytes kèm `Content-Disposition: attachment; filename="..."` cho endpoint `/download`
- FE invoice page: `<iframe src="/api/invoice/{id}/preview">` + button "Tải về" → `/download`
- Excel/Word/PPT: chỉ có `/download`, không preview

**Lý do:**
1. PDF preview free (browser handle native) → UX tốt với cost 0
2. Excel/Word/PPT preview costly → ROI thấp cho Wave 5
3. Mobile-safe (PDF inline iframe hoạt động trên iOS/Android)

**Khi nên đầu tư preview cho Excel/Word:**
- Sau launch, nếu user feedback cần "xem trước Excel report khi chưa tải"
- Tạo gap riêng, dùng library như SheetJS (FE) hoặc LibreOffice headless (BE)

---

## Câu 5: Sync vs queue-first

### Bối cảnh
Tạo PDF/Excel/Word/PPT mất từ 200ms (invoice đơn giản) đến 30s+ (Excel 10k rows). Hỏi: dùng sync HTTP (đợi tạo xong) hay async queue (enqueue → process → notify)?

### Kiến thức — async patterns

**Sync (blocking HTTP)**
```
Client POST /invoices/123/pdf
→ BE generate (2-5s)
→ BE return bytes
Client receive + download
```
- Đơn giản, không infra
- OK với latency <5s
- Fail khi: request timeout, generator crash mid-way, load balancer timeout

**Queue-first (async với RabbitMQ)**
```
Client POST /invoices/123/generate
← BE return jobId (200 OK)
BE enqueue message → RabbitMQ

[Worker]
Pull message → generate → upload MinIO → publish event

Client poll /jobs/{id} hoặc subscribe WebSocket
← BE return {status: "DONE", downloadUrl: "..."}
```
- Scale tốt (worker horizontal)
- Handle retry, backoff
- Handle large generation jobs
- Infra phức tạp hơn (RabbitMQ, MinIO, WebSocket/polling)

### Kiến thức — dự án KiteClass hiện tại

- **RabbitMQ: đã có** (Wave 3b GAP-002 shipped async pipeline cho AI image gen)
- **MinIO: đã có** (branding assets storage)
- **Infra sẵn sàng reuse**

### Benchmark ước tính cho 4 formats

| Format | Size | Time (sync) | Nguy cơ sync fail |
|--------|:----:|:-----------:|:-----------------:|
| Invoice PDF (1 page) | ~50KB | 100-500ms | 🟢 Không |
| Certificate PDF | ~100KB | 200ms | 🟢 Không |
| Attendance Excel (1 class, 1 tuần) | ~30KB | 300ms | 🟢 Không |
| Financial Excel (cả năm) | ~2MB | 3-10s | 🟡 Borderline |
| Monthly report PDF (20 page) | ~500KB | 2-5s | 🟡 Borderline |
| Bulk invoice 500 students | ~25MB | 60-120s | 🔴 Fail sync |

### 🎯 Default: **Sync trong Wave 5. Queue hóa sau khi có use case real.**

**Lý do:**
1. 80% use case (single invoice/certificate) hoàn tất <2s — sync hợp lý
2. Queue infrastructure đã có từ Wave 3b → add khi cần, không tốn thời gian build trước
3. YAGNI — chưa có use case bulk/large report
4. Simpler code → ít bug, ít infra touch points

**Trigger để queue hóa:**
- Use case "Bulk generate invoices for 500 students" xuất hiện
- Use case "Monthly financial report" vượt 5s
- Error rate sync request timeout >0.5%

**Follow-up gap** nếu default approve:
- "GAP-XXX: Queue-backed document generation cho bulk + large reports" — chỉ mở khi trigger xuất hiện

**Endpoint naming convention (future-proof):**
- Wave 5: `POST /documents/generate` (sync, trả bytes)
- Future: thêm `POST /documents/generate-async` (trả jobId) — không break existing API

---

## Câu 6: Priority của Sub-PR 5.3 (Word) + 5.4 (PPT)

### Bối cảnh
Wave 5 plan có 5.3 Word P1 + 5.4 PPT P2. Hỏi: cả 2 làm trong Wave 5 hay defer một số sang Wave 6?

### Kiến thức — use case per format

**Word (contracts, policies, forms):**
- User: HR, admin
- Frequency: low (1-5 contracts/tháng cho 1 trung tâm trung bình)
- Critical path: tuyển giáo viên cần contract → blocking HR workflow
- Legal review: PHẢI có trước khi production use → không thể ship "finished" trong Wave 5 bất kể gì

**PPT (marketing, training):**
- User: center owner, trainer
- Frequency: rất thấp (1-2/quý)
- Critical path: không blocking (owner có thể dùng Canva/Google Slides ngoài)
- Legal review: không cần

### Cân nhắc scope Wave 5

**Với 5.3 + 5.4 (hiện tại):**
- 4 parallel agents — đúng mode parallel
- Wall-clock ~14h
- Risk: 1 agent delay ảnh hưởng merge order

**Chỉ 5.3, defer 5.4:**
- 3 parallel agents — vẫn parallel
- Wall-clock ~12h
- Scope tight hơn
- PPT vào Wave 6 như standalone feature

**Defer cả 5.3 + 5.4:**
- Chỉ 2 parallel (5.1 + 5.2) — ít parallelism nhưng scope rất tight
- Wall-clock ~10h
- Wave 5 chỉ prove PDF + Excel infrastructure
- Word + PPT vào Wave 6

### 🎯 Default: **Giữ Sub-PR 5.3 Word trong Wave 5 (P1). Defer Sub-PR 5.4 PPT sang Wave 6.**

**Lý do:**
1. Word contract là SHOULD-HAVE (HR workflow unblocks), legal review có thể parallel với dev
2. PPT là NICE-HAVE, defer an toàn, Canva là alternative cho user
3. Scope Wave 5 gọn hơn, rủi ro agent coordination thấp hơn
4. 3 generators trong Wave 5 (PDF, Excel, Word) = đủ cover 90% doc gen use case

**Sub-PR 5.4 PPT → Wave 6:**
- Tạo Wave 6 plan riêng sau khi Wave 5 ship
- Scope Wave 6: PPT generator + marketing pitch template + training slides template + FE preview cho docs (enhancement)

**Khi nên giữ 5.4 PPT trong Wave 5:**
- Nếu có event/deadline cần marketing pitch trong 1 tuần
- Nếu agent thứ 4 sẵn sàng và không có blocker khác

---

## Tóm tắt default answers

| # | Câu hỏi | Default |
|:-:|---------|---------|
| 1 | PDF library? | **OpenHTMLtoPDF + PDFBox** (Apache/LGPL, reuse Thymeleaf) |
| 2 | Maven module split? | **Inline trong kiteclass-core** (YAGNI, extract khi kitehub consume) |
| 3 | 4 template stubs đủ cho launch? | **Đủ cho Wave 5 acceptance; KHÔNG đủ launch → tạo Wave 7 với template library expansion** |
| 4 | FE preview? | **PDF preview in-browser (free); Excel/Word/PPT download-only** |
| 5 | Sync vs queue? | **Sync trong Wave 5. Queue khi có bulk/large use case** |
| 6 | Sub-PR 5.3 + 5.4 priority? | **Giữ 5.3 Word trong Wave 5. Defer 5.4 PPT sang Wave 6** |

---

## Tác động khi adopt default answers

### Scope Wave 5 sau defaults
- 3 formats: PDF (OpenHTMLtoPDF), Excel (POI), Word (POI) — không PPT
- Inline trong kiteclass-core — không module mới
- 3 stub templates: invoice, attendance, teacher contract (placeholder)
- Sync endpoints only
- PDF preview endpoint + download endpoint; Excel/Word download-only
- 5 sub-PR (bỏ 5.4): 5.0 foundation → 5.1 PDF → 5.2 Excel → 5.3 Word → 5.5 integration → 5.6 completion
- Wall-clock ước tính: ~12h với 3 parallel agents (vs ~14h với 4 parallel)

### New follow-up gaps (gap numbers renumbered 2026-04-24 — original 104-108 already consumed by Part A audit)
- **GAP-208** Launch template library expansion (certificates, transcripts, financial reports, receipts) — P0 for launch (Wave 7)
- **GAP-209** Extract `kiteclass-document-gen` Maven module — P2, trigger khi kitehub consume
- **GAP-210** Queue-backed document generation — P2, trigger khi bulk/large use case xuất hiện
- **GAP-211** FE preview cho Excel/Word — P2, trigger khi user request
- **GAP-212** PPT marketing/training templates (Wave 6) — P2 (deferred from Wave 5)

Gap files will be created during Wave 5 Sub-PR 5.6 (wave completion) per `audit-to-gap-pipeline.md`.

---

## Approval checklist — ✅ APPROVED 2026-04-24

Reviewer (user nguyenvankiet) approved all 6 defaults:
- [x] Câu 1: OpenHTMLtoPDF + PDFBox approved
- [x] Câu 2: Inline trong `kiteclass-core` approved
- [x] Câu 3: 4 stubs cho Wave 5, file GAP-208 cho Wave 7 launch expansion
- [x] Câu 4: PDF preview inline; Excel/Word download-only approved
- [x] Câu 5: Sync-first approved; queue deferred to GAP-210
- [x] Câu 6: Defer PPT → Wave 6 (GAP-212) approved
- [x] **ADR-019** (renumbered from 016 — conflict with fe-be-contract-strategy) will document these 6 decisions in Sub-PR 5.0

Wave 5 scope is LOCKED. Change = new user decision + log entry in `wave-05-document-generation.md` §11.

---

## Log

- **2026-04-24 (APPROVED):** User nguyenvankiet approved all 6 defaults in single sign-off. Wave 5 scope LOCKED. ADR number bumped 016 → 019 (conflict with existing ADR-016 fe-be-contract-strategy). Follow-up gap IDs renumbered 104-108 → 208-212 (original range already consumed by Part A audit 2026-04-19). Next step: Wave 5 Sub-PR 5.0 foundation + ADR-019 starts in follow-up session.
- **2026-04-19:** Guide drafted sau khi user request best-practice defaults cho 6 open questions. Dựa trên: CLAUDE.md rules (YAGNI, brainstorm), existing dependencies (Thymeleaf, RabbitMQ, MinIO), SaaS giáo dục use case patterns.
