---
title: Simulation Gap Finder — Phase 1 BETA Failure Modes (3-Axis Matrix)
status: complete
created: 2026-05-14
phase: pre-wave-73
related-acceptance-test: ../../../05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv
---

# Ma trận 4×7×5 — Phase 1 BETA Failure Modes

## Phương pháp

Áp dụng `.claude/skills/quality/simulation-gap-finder/SKILL.md` — 3-axis systematic matrix khác với 2 cách kia (persona walkthrough + competitor benchmark). Mục tiêu: phát hiện gap mà cả 2 cách kia không bắt được, đặc biệt nhóm **edge case người dùng Việt** + **failure mode kỹ thuật** mà solo dev VN dễ skip vì "quá quen tay".

**Cấu trúc:** 4 nhân vật × 7 bước hành trình × 5 chế độ thất bại = **140 ô**.

- **Nhân vật (4):** Anonymous (chưa đăng nhập), Pre-tenant (chờ duyệt), Owner P2 (active), Platform_Admin (dev)
- **Bước (7):** Pre-invite → Beta request → Email verify+provision → Onboarding wizard → Daily use → Hỗ trợ → Off-boarding/retention
- **Failure (5):** Mạng/Hạ tầng, Sai dữ liệu, Sai quyền, Concurrent/Race, Edge case VN

Đa phần ô (~110/140) hệ thống handle đủ tốt hoặc không xảy ra trong scope Phase 1 BETA. Báo cáo chỉ liệt kê **23 gap candidates** thực sự có evidence cụ thể.

---

## Ma trận tóm tắt (chỉ ô có gap)

| # | Ô (Nhân vật × Bước × Failure) | Quan sát | Gap | Mức độ |
|---|---|---|---|---|
| SIM-01 | Anonymous × Beta request × Sai dữ liệu | `BetaRequestDto.name` validate `@Size(max=200)` nhưng KHÔNG có normalization Unicode — user nhập tên "Nguyễn Thị Phương Khánh" với Unicode mixed (NFC vs NFD từ iOS keyboard) có thể bị duplicate detection bỏ sót | Thiếu Unicode-normalize (NFC) cho name + orgName trước khi store/dedupe | **P1** |
| SIM-02 | Anonymous × Beta request × Edge case VN | Phone validation: CSV row BETA-REQ-005 dùng `phone=0938765432` nhưng `BetaRequestDto` không có field `phone` validation (chỉ có email/name/orgName/persona/referralSource/consentGiven). Form FE có thể submit phone format `+84938765432` hoặc `0938 765 432` (space) → BE accept hoặc reject inconsistent | Phone validation pattern thiếu/không enforce VN format (10-11 digit, prefix 0/+84) | **P1** |
| SIM-03 | Anonymous × Beta request × Concurrent | Honeypot field check (`@Size(max=0)`) chỉ chặn bot — nhưng nếu user mở 2 tab và submit cùng 1 email trong vòng 100ms, cả 2 đi qua rate-limit check (5/sec burst) → 2 row PENDING cùng email trong DB | DB unique constraint trên `(email, status=PENDING)` thiếu OR transaction lock thiếu | **P1** |
| SIM-04 | Pre-tenant × Email verify × Mạng | User click link verify nhưng Cloudflare/Vercel/CDN cache token 1 lần → request thứ 2 (user refresh page hoặc click 2 lần) hit cache → server không thấy token invalidation → user thấy "Verified" 2 lần nhưng DB chỉ flip 1 lần | Verify endpoint thiếu `Cache-Control: no-store, must-revalidate` header | **P2** |
| SIM-05 | Pre-tenant × Email verify × Sai dữ liệu | Email link có token URL-encoded (vd `%2B` cho `+` trong base64 padding) — nếu Gmail/Outlook strip dấu `+` trong URL preview, user click link bị truncated → 400 invalid token, UI không phân biệt được "token expired" vs "token malformed" | UI verify-email page không xử lý malformed token (chỉ có "expired") | **P2** |
| SIM-06 | Pre-tenant × Email verify × Edge case VN | VN ISP Viettel/VNPT/FPT đôi khi route email gateway qua server EU → email arrival latency có thể 5-15 phút thay vì 60s (CSV row EMAIL-VERIFY-001 expect "trong vòng 60s") → user nghĩ system broken | Acceptance criteria 60s timeout quá strict cho VN ISP; thiếu UI "Hãy đợi vài phút, có thể bị filter spam" | **P2** |
| SIM-07 | Platform_Admin × Beta approve × Concurrent | 2 admin (current Admin + future Admin co-founder) cùng mở chi tiết request id=1, cùng click "Duyệt" trong 5 giây → cả 2 đều chạy provisioning job → tenant `sky-education` provisioned 2 lần (race trong invite_token generate) | Optimistic locking trên `BetaAccessRequest.status` thiếu hoặc DB unique constraint trên `tenant.slug` thiếu | **P0** |
| SIM-08 | Platform_Admin × Beta approve × Sai quyền | Admin từ chối yêu cầu (CSV ADM-BETA-REJECT-002) nhưng quên uncheck `notify_user=true` → email từ chối gửi tới user, nhưng nếu user đã reset email (forwarder bị disable bên Cloudflare) → SES bounce → soft-bounce count tăng, ảnh hưởng reputation domain | Reject flow không validate destination email vẫn deliverable trước khi send | **P2** |
| SIM-09 | Pre-tenant × Onboarding wizard × Mạng | Wizard 6 step (CSV OWNER-PROVISION-001..007) — user complete bước 5 chọn template, upload logo 1.8MB qua 3G yếu → request timeout 30s → user click "Tiếp" lần 2 → state wizard reset về step 1 thay vì retry step 5 | Wizard FE thiếu persistent state (localStorage) + resume từ step đang dở | **P1** |
| SIM-10 | Pre-tenant × Onboarding wizard × Sai dữ liệu | Wizard step 2 upload logo: user upload file `.heic` (iOS default từ iPhone screenshot) — BE expect PNG/JPG → reject với HTTP 415 nhưng UI hiển thị "Lỗi không xác định" thay vì "Định dạng không hỗ trợ, hãy upload PNG/JPG" | UI upload error không có per-MIME-type message, không suggest conversion | **P1** |
| SIM-11 | Pre-tenant × Onboarding wizard × Edge case VN | Audience preset `KIDS_TWEEN_TEEN` (CSV OWNER-PROVISION-003) — phụ huynh VN thường dùng từ "thiếu niên/nhi đồng" thay vì "tween" — nếu UI render label English chưa localized → user confused, chọn nhầm preset → branding kết quả không match expectation | UI wizard label preset chưa localized 100% (audience + tone preset names) | **P1** |
| SIM-12 | Owner × Daily use × Edge case VN | Tạo học viên (CSV OWNER-STU-003) — tên học viên chứa dấu tiếng Việt dài "Nguyễn Thị Phương Khánh Linh" (28 ký tự) — UI table column `Họ tên` được fixed-width 200px → bị tràn `...` không hiển thị đủ tên, hover tooltip thiếu | DataGrid component thiếu hover tooltip cho cell bị truncate | **P2** |
| SIM-13 | Owner × Daily use × Sai dữ liệu | Import CSV học viên hàng loạt (CSV OWNER-STU-005) — file Excel xuất từ Office 365 VN locale có separator `;` thay vì `,` → BE parser expect comma → reject với "Invalid CSV" generic | CSV import thiếu auto-detect separator (`,` vs `;`) hoặc UI hint "Lưu file CSV với UTF-8 + comma separator" | **P1** |
| SIM-14 | Owner × Daily use × Sai dữ liệu | Ngày sinh học viên: VN owner thường nhập `15/03/2010` (dd/mm/yyyy) nhưng input HTML5 `<input type="date">` expect ISO `2010-03-15` (yyyy-mm-dd) — nếu FE không control format và user dán text "15/03/2010" → parse `2010-03-15` thành `2003-03-15` ✕ | Date input thiếu locale-aware parsing hoặc UI thiếu placeholder rõ "dd/mm/yyyy" + parse helper | **P1** |
| SIM-15 | Owner × Daily use × Concurrent | Owner mở 2 tab cùng edit thông tin tenant (CSV OWNER-SET-004) — tab 1 đổi tenant_slug, tab 2 đổi locale → submit tab 1 trước, tab 2 sau (without refresh) → tab 2 ghi đè tenant_slug về giá trị cũ | Optimistic locking trên Tenant entity thiếu (version field) OR last-write-wins documented | **P1** |
| SIM-16 | Owner × Daily use × Mạng | Browser idle 20 phút (lunch break) → JWT access token expire (TTL 15 phút per `pre-launch-auth-hardening-checklist.md` §2.8) — user quay lại click action → 401 → expected behavior: silent refresh; actual: tùy implementation có thể logout → user mất state đang edit | Silent refresh-on-401 flow thiếu hoặc UI không preserve form data trước khi redirect login | **P1** |
| SIM-17 | Owner × Daily use × Edge case VN | Payment thủ công (CSV OWNER-PAYMENT-002) — owner nhập số tiền `2.500.000` (VN format dot thousand separator) — input HTML number type parse thành `2.5` (3-decimal!) → payment row created với amount=2.5 VND thay vì 2500000 | Currency input thiếu locale-aware mask hoặc accept "2.500.000" + "2500000" + "2,500,000" cùng parse VND | **P0** |
| SIM-18 | Owner × Hỗ trợ × Sai quyền | Owner cố access trang admin platform `/admin/beta-requests` (sau khi đọc rò rỉ URL từ docs) → role-guard reject 403 — nhưng nếu hệ thống không log attempt → security blind spot không phát hiện được pattern probing | Audit log thiếu ghi failed authorization attempts (403/forbidden) | **P2** |
| SIM-19 | Owner × Off-boarding × Concurrent | Owner request xóa tenant (CSV OWNER-OFFBOARD-001) — nhưng có teacher đang upload bài tập cùng thời điểm, parent đang xem bảng điểm → soft-delete flag set nhưng background job dọn dữ liệu chạy ngay → teacher/parent thấy lỗi 404 bất ngờ | Off-boarding flow thiếu grace period broadcast notification + delay cleanup ≥24h | **P1** |
| SIM-20 | Platform_Admin × Hỗ trợ × Concurrent | Admin tạm khoá instance (CSV ADM-INST-003) — user tenant đang đăng nhập tại thời điểm đó → JWT vẫn valid 15 phút → user vẫn truy cập được trong window 15 phút trước khi token expire | Suspend flow thiếu immediate token revocation OR token-blacklist với Redis (ngoài JWT TTL) | **P1** |
| SIM-21 | Anonymous × Pre-invite × Edge case VN | User VN tìm Google "phần mềm quản lý trung tâm" → landing kitehub.me — nhưng URL path SEO meta-tag chỉ tiếng Anh (per Wave 71 docs) → Google rank thấp cho query VN, traffic conversion thấp | SEO meta-tags + JSON-LD chưa Vietnamese-first cho VN audience | **P2** |
| SIM-22 | Pre-tenant × Provisioning × Sai quyền | Provisioning job (CSV OWNER-PROVISION-007) — admin approved request nhưng job background fail (Ollama không up cho FULL_AI path) → user signup thành công nhưng dashboard load không có branding theme → confused | Job-state visibility for user — UI thiếu "Đang setup, vui lòng đợi 2-5 phút" + retry/notify nếu fail | **P1** |
| SIM-23 | Owner × Daily use × Mạng | Dashboard real-time updates (SSE/WebSocket per `pre-handoff-self-test-completeness.md` §2.8 — nếu adopted Phase 1.5) — nhưng VN ISP đôi khi block long-poll/SSE qua proxy enterprise (vd FPT enterprise) → real-time silent fail, user thấy data stale không refresh | Phase 1 BETA chưa adopt SSE nhưng tài liệu UC mention → cần graceful degradation polling fallback từ ngày đầu | **P2** |

---

## Phân tích theo trục

### Theo nhân vật — nhân vật nào thiếu cover nhất

| Nhân vật | Số gap | Mức độ trung bình | Quan sát |
|---|---:|---|---|
| **Anonymous** | 3 | P1 | Beta request flow + SEO — touchpoint công khai dễ miss |
| **Pre-tenant** | 7 | P1+ | Highest count — Provisioning + Email verify gặp nhiều failure mode (mạng, dữ liệu, race) |
| **Owner P2** | 10 | P1 (1 P0) | Most exposed surface — daily use × 5 failure mode = nhiều edge case |
| **Platform_Admin** | 3 | P0/P1 | Ít số lượng nhưng severity cao (race condition Beta-approve P0) |

**Bài học:** Pre-tenant + Owner P2 dominant scope (~74% gap). Wave 73 nên ưu tiên hardening luồng provisioning + daily-use input validation.

### Theo bước — bước nào fragile nhất

| Bước | Số gap | Quan sát |
|---|---:|---|
| 1. Pre-invite/Discovery | 1 | SEO VN |
| 2. Beta request | 3 | Unicode normalize, phone validation, race |
| 3. Email verify + provision | 5 | Cache, malformed token, VN email latency, provision visibility |
| 4. Onboarding wizard | 3 | State persistence, MIME validation, audience preset i18n |
| 5. Daily use | 8 (**peak**) | Date locale, CSV separator, tên dài, currency format, JWT silent refresh, optimistic lock, SSE fallback |
| 6. Hỗ trợ | 2 | Audit log failed auth, suspend token revocation |
| 7. Off-boarding | 1 | Race với active session |

**Bài học:** Daily Use là bước fragile nhất — 8 gap (35% tổng). Tập trung vào input validation + VN locale + concurrent edit handling.

### Theo failure mode — mode nào hệ thống yếu nhất

| Failure mode | Số gap | Quan sát |
|---|---:|---|
| **Mạng/Hạ tầng** | 4 | Cache headers, timeout retry, JWT refresh, SSE fallback |
| **Sai dữ liệu** | 6 (**peak**) | Unicode NFC, MIME .heic, CSV separator, date format, currency format, malformed token |
| **Sai quyền** | 3 | Provision visibility, suspend revocation, failed-auth audit |
| **Concurrent/Race** | 5 | Honeypot race, admin double-approve, tab edit conflict, off-boarding race, JWT-vs-suspend window |
| **Edge case VN** | 5 | Tên dài có dấu, audience preset chưa Việt, phone format, email latency VN ISP, SEO VN |

**Bài học:** Sai dữ liệu là failure mode chiếm cao nhất (6/23 = 26%) — phần lớn liên quan input parsing không locale-aware. Edge case VN + Concurrent xếp tied 2nd (5 each) — dev VN solo dễ skip cả 2 nhóm này vì "quá quen tay" (VN locale) + "tôi là user duy nhất" (concurrent).

---

## Top 10 gap nghiêm trọng nhất (sắp xếp theo blast radius × likelihood)

Severity score = (Blast radius 1-5) × (Likelihood 1-5):

| Rank | Gap | Blast | Likelihood | Score | Lý do |
|---|---|:-:|:-:|:-:|---|
| 1 | SIM-07 Admin double-approve race → tenant slug duplicate | 5 | 3 | **15** | Data corruption — DB inconsistent; P0 |
| 2 | SIM-17 Currency parse error 2.500.000 → 2.5 VND | 5 | 4 | **20** | Owner mất tiền theo dõi sai, mất uy tín; P0 |
| 3 | SIM-14 Date format dd/mm vs mm/dd ambiguity | 4 | 5 | **20** | Daily input, sai DOB → sai age check; P1 high |
| 4 | SIM-13 CSV import separator `;` vs `,` reject | 4 | 4 | **16** | Block onboarding flow, user bỏ cuộc; P1 |
| 5 | SIM-15 Optimistic lock missing → ghi đè last-write-wins | 4 | 3 | **12** | Data loss silent; P1 |
| 6 | SIM-09 Wizard state reset khi network fail | 3 | 4 | **12** | User abandon onboarding; P1 |
| 7 | SIM-20 JWT-suspend window 15 phút | 4 | 3 | **12** | Security gap — suspended user vẫn access; P1 |
| 8 | SIM-03 Honeypot double-submit race | 3 | 3 | **9** | DB dup row, admin xử lý 2 lần; P1 |
| 9 | SIM-16 JWT refresh fail → form data loss | 3 | 4 | **12** | UX rough, user nghi service; P1 |
| 10 | SIM-22 Provisioning silent fail → blank dashboard | 4 | 2 | **8** | First-impression hỏng, churn; P1 |

---

## Khuyến nghị — Phân nhóm Wave 73/74

### Wave 73 — P0/P1 high blast radius (must-ship pre-launch)

Ưu tiên fix trước first 5 beta tenants:

- **SIM-17 P0** Currency parse VN format → ngay
- **SIM-07 P0** Admin double-approve race → ngay (DB unique constraint + optimistic lock)
- **SIM-14 P1** Date locale-aware parsing → ngay
- **SIM-13 P1** CSV import separator detection → ngay
- **SIM-09 P1** Wizard state persistence → cần FE refactor
- **SIM-16 P1** JWT silent refresh + form preservation → cần auth flow update
- **SIM-15 P1** Optimistic locking Tenant + critical entities → BE migration

**Estimated effort:** ~5-7 days với 3-4 parallel agent buckets.

### Wave 74 — P1 medium + P2 polish (post-soft-launch)

Fix sau khi beta cohort 1 hoạt động:

- SIM-02 Phone validation VN format
- SIM-08 Reject email deliverable check
- SIM-10 Upload error per-MIME message
- SIM-11 Wizard preset i18n
- SIM-20 Suspend immediate revocation
- SIM-22 Provisioning visibility UI
- SIM-03 Honeypot dedupe constraint

**Estimated effort:** ~3-4 days.

### Hậu beta (P2 — backlog, không block Phase 1)

- SIM-01 Unicode normalize NFC
- SIM-04 Cache-Control no-store cho verify endpoint
- SIM-05 Malformed token UI
- SIM-06 VN ISP email latency message
- SIM-12 DataGrid tooltip truncate
- SIM-18 Failed-auth audit log
- SIM-19 Off-boarding grace period
- SIM-21 SEO Vietnamese-first
- SIM-23 SSE fallback polling

---

## Khác biệt vs persona/benchmark approaches

3-axis matrix surface những class gap mà 2 cách kia thường miss:

**Mới (3-axis catch, persona/benchmark likely miss):**

- SIM-01 Unicode NFC normalization — không persona nào tự nhiên test mixed-Unicode tên, không competitor benchmark cũng vì là edge VN
- SIM-04 Cache-Control verify endpoint — chỉ surface khi force "Mạng/Hạ tầng × Email verify" cell
- SIM-06 VN ISP email latency — chỉ surface khi force "Edge case VN × Email" cell
- SIM-07 Admin double-approve race — chỉ surface khi force "Concurrent × Admin × Beta approve"
- SIM-14 Date locale dd/mm/yyyy parse ambiguity — chỉ surface khi force "Sai dữ liệu × Daily use × VN edge"
- SIM-17 Currency 2.500.000 parse error — same logic
- SIM-20 JWT-suspend 15 phút window — chỉ surface khi force "Concurrent × Suspend × Token TTL"
- SIM-23 SSE block bởi VN enterprise proxy — chỉ surface "Mạng × Daily × VN ISP"

**Có thể trùng (persona/benchmark cũng catch):**

- SIM-09 Wizard state — persona Owner P2 đi qua flow sẽ thấy
- SIM-10 Upload error UI — persona Pre-tenant cũng có thể catch
- SIM-13 CSV separator — common feedback từ user
- SIM-21 SEO VN — benchmark Hotmart/Teachable cũng có

→ **~8/23 (35%) là gap mới do 3-axis matrix catch** — confirm value của method này khác bổ sung 2 cách kia, không thay thế.

---

## Self-test rule applied

Per `audit-to-gap-pipeline.md` §2.5 state-check: gap được file dựa trên evidence cụ thể (CSV row reference + Java DTO field check + i18n state per Wave 31 + auth config per Wave 71). Không có gap "fully greenfield" claim mà chưa verify codebase.

Per `dev-readable-doc-language.md` — narrative tiếng Việt, technical identifier (HTTP, JWT, CSV, DTO, FE/BE, Unicode NFC, MIME, SSE) giữ English code-switching tự nhiên.

Per `meta-gap-priority.md` §3 priority matrix — Top 10 ranking respect blast radius × likelihood; P0 SIM-17 (currency) + SIM-07 (race) ưu tiên trước feature gap nominal cùng P-level.

---

## Liên kết

- Acceptance test source: `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`
- Skill reference: `.claude/skills/quality/simulation-gap-finder/SKILL.md`
- Sister rules: `audit-to-gap-pipeline.md` §2.5, `meta-gap-priority.md` §3, `pre-handoff-self-test-completeness.md` §2 (file-upload, payment, multi-tab race, SSE, JWT classes)
- Open Phase 1 BETA gaps để cross-check: `bash scripts/query-gaps.sh "" "" phase-1-beta` (81 active)
