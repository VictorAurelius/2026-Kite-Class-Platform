---
title: Pre-walk persona simulation — KC-8 Parent portal (browser-walk readiness)
audience: dev
created: 2026-06-16
scope: Flow Verification Campaign KC-8 — predict failure modes BEFORE G2/G1-browser walk of parent portal (child grade/attendance/fees/conduct facets)
persona: Phụ huynh (PARENT) đăng nhập :3000 → xem danh sách con → đọc học bạ/điểm danh/học phí/hạnh kiểm
references:
  - documents/03-planning/waves/wave-2026-06-05-flow-kc8-parent-portal.md
  - documents/05-guides/operations/2026-06-05-g2-recipe-kc8-parent-portal.md
  - documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc8-parent-portal.md
  - .claude/rules/pre-walk-persona-simulation-mandate.md
  - .claude/rules/g1-browser-walk-before-flip.md
---

# Pre-walk persona simulation — KC-8 Parent portal (2026-06-16)

**Persona walked:** Phụ huynh mới (chị Hằng) — đăng nhập KiteClass `:3000` qua subdomain tenant → mở dashboard `/parent` → bấm vào con → xem học bạ / điểm danh / học phí / hạnh kiểm. Test consent gate + IDOR.

## Bối cảnh quan trọng — kiến trúc đã DỊCH CHUYỂN so với wave plan 2026-06-05

Wave plan + G2 recipe (2026-06-05) ghi: **parent login = Phase 2 by design** (GAP-725) — `TokenService` không issue tenantId/referenceId cho PARENT, gateway không inject `X-User-Reference-Id` → G3 gateway-parity bị defer, G2 test bằng **direct-core curl gắn header tay**.

**State-check 2026-06-16 cho thấy điều này KHÔNG còn đúng** — Wave auth-1 (GAP-1122/GAP-725) đã land:
- KC-native tenant-auth login cho PARENT/TEACHER/STUDENT tồn tại: `POST /api/v1/tenant-auth/login` (`AuthTokenService` issue HS512 token với claim `role` + `tenantId` + `referenceId`; `auth_credentials.entity_type` CHECK gồm `PARENT`).
- Gateway `kite-gateway` (build từ `kitehub/kitehub-gateway`) `JwtAuthenticationGatewayFilter:233-237` đã inject `X-User-Reference-Id` từ claim `referenceId` (strip-then-reinject per GAP-1308).
- FE `auth.ts` probe tenant-auth trước, normalize role (GAP-1122), Host-preservation cho subdomain (GAP-1207).

→ **Hệ quả:** browser-walk parent end-to-end qua gateway giờ KHẢ THI về mặt code (G3 deferral note trong recipe đã STALE). NHƯNG hiện chặn bởi **thiếu seed** (FM#1) + **FE pages render mock** (FM#2). Đây chính là loại bug curl-walk-G1 (gắn header tay, direct-core) bỏ lọt mà browser-walk bắt.

---

## Failure modes (10) — ưu tiên browser-bắt-curl-bỏ-lọt

| # | Failure mode | (a) Where | (b) Symptom user thấy | (c) Pre-walk check (đã chạy) | (d) Confidence + Fix |
|---|---|---|---|---|---|
| **1** | **Seed parent THIẾU HẲN — không login/walk được** | DB `kiteclass_shared`: `parents`, `parent_student_links`, `auth_credentials(entity_type=PARENT)` | Không có credential PARENT → tenant-auth login 401 "Email hoặc mật khẩu không đúng"; kể cả login OK thì 0 con → dashboard rỗng | `psql`: `parents=0`, `parent_student_links=0`, `parental_consent rows=0`; `auth_credentials` chỉ 2 rows (1 TEACHER + 1 STUDENT, instance `5b3ef1ae` subdomain `sky-education-074901`); 143 students tổng nhưng 0 parent. Wave-plan seed `parent1@test.com` / instance `aaaabbbb…0001` KHÔNG tồn tại | **HIGH (blocker).** Fix: re-seed trong instance TRIAL `5b3ef1ae` (sky-education-074901): 1 `parents` row + `parent_student_links` (link 1 trong 4 students có sẵn) + `auth_credentials(PARENT, email, bcrypt pwd, instance_id, entity_id=parents.id)` + consent JSONB. Recipe PHẢI thêm bước seed + verify trước walk |
| **2** | **FE facet pages render MOCK, KHÔNG gọi BE facet (attendance/grades/billing)** | `(dashboard)/parent/attendance/page.tsx` (`buildMockAttendance`), `billing/page.tsx` (`MOCK_INVOICES`), `page.tsx` (hero 92%, GPA 8.4, `MOCK_ACTIVITIES`); `lib/api/parent.ts` CHỈ wire `getMe`/`getMyChildren`/`getChildTranscript` | Phụ huynh thấy lịch điểm danh + hóa đơn + điểm TB = **dữ liệu giả**, không qua consent/IDOR/BE. Trông như thật → hiểu nhầm | grep: attendance/billing/home pages import mock; `parent.ts` 3 endpoint; `use-parent.ts` 3 hook — KHÔNG có attendance/fees/conduct hook | **HIGH.** Curl-walk-G1 test BE facet trực tiếp → mock này **vô hình** với curl; chỉ browser thấy data giả present-as-real. Fix: defer FE-wiring Phase 1.5 (đã planned) NHƯNG recipe G2 PHẢI cảnh báo rõ "attendance/grades/billing = mock"; chỉ transcript + danh-sách-con là thật |
| **3** | **Transcript (facet thật duy nhất) gộp consent-required + not-linked thành 1 lỗi generic, KHÔNG có CTA cấp consent** | `transcript/[childId]/page.tsx:87-99` `ErrorAlert` message cứng "Bạn có thể không có quyền xem học bạ này (chỉ phụ huynh đã liên kết)"; BE: transcript throw `PARENT_NOT_LINKED` (KHÔNG có consent gate), facet throw `PARENT_CONSENT_REQUIRED`/`RECONSENT_REQUIRED` | Khi consent rỗng (first-login), facet 403 → FE không có nhánh "cấp quyền" → phụ huynh kẹt, không biết phải làm gì. Toàn parent portal KHÔNG có UI consent | grep error codes: `ParentConductFacetServiceImpl` 3 mã (FORBIDDEN/CONSENT_REQUIRED/RECONSENT); `ParentTranscriptService` chỉ `PARENT_NOT_LINKED`; FE 0 component consent | **HIGH.** Browser thấy lỗi mơ hồ; curl thấy errorCode chính xác → curl bỏ lọt UX gap. Fix: FE branch theo errorCode → render CTA "Cấp quyền xem dữ liệu con" cho `PARENT_CONSENT_REQUIRED`; phân biệt với `PARENT_NOT_LINKED` |
| **4** | **Browser PHẢI vào qua subdomain Host (nip.io), localhost:3000 thuần → login fail im lặng** | `lib/api/auth.ts:33-47` `loginBaseUrl()`: Host không subdomain → dùng `NEXT_PUBLIC_API_URL=:9000` Host=localhost → gateway không resolve tenant (GAP-814 strip client `X-Tenant-Id`, GAP-1207) → tenant-auth 401 | Mở `localhost:3000` → đăng nhập parent → "Email/mật khẩu không đúng" dù đúng. Hoặc :3000 `ERR_EMPTY_RESPONSE` sau rebuild (stale docker-proxy GAP-1067) | đọc `auth.ts` loginBaseUrl logic; g1-browser-walk §3.3 KC recipe; instance subdomain = `sky-education-074901` | **MEDIUM-HIGH.** Curl-with-manual-header che hẳn cơ chế này. Fix: recipe dùng `http://sky-education-074901.127.0.0.1.nip.io:3000`; verify by-subdomain resolve trước; sau rebuild `docker restart kiteclass-frontend` |
| **5** | **IDOR cross-parent KHÔNG browser-test được nếu chỉ 1 parent seed + FE chỉ link con của mình** | FE `page.tsx` chỉ render `<ChildCard>` cho con đã link; không có ô nhập childId tùy ý; IDOR guard `@authz.hasAccessToChild` ở BE | Browser-walk tự nhiên không probe được parent1→child2 (phải sửa URL tay `/parent/transcript/2`). Cần 2 parent + 2 con seed | đọc page.tsx (chỉ map own children); wave plan §1 IDOR 2-lớp | **MEDIUM.** Đây là chỗ curl-walk MẠNH HƠN browser (swap childId+header). Fix: seed 2 parent (parent1→child1, parent2→child2); browser verify own-child happy + manual URL-tamper `/parent/transcript/<child2>` → kỳ vọng 403/ErrorAlert; ghi rõ IDOR-core do curl G1 cover |
| **6** | **Children rỗng → dashboard hiện "Chưa có con" nhưng hero 92% + GPA 8.4 + "Đã đóng" vẫn render mock → mâu thuẫn** | `page.tsx:128-151` empty branch chỉ cho phần children list; hero/GPA/billing/activity vẫn render từ mock bất kể | Phụ huynh chưa có con nào nhưng thấy "Tỷ lệ đi học 92%", "Điểm TB 8.4", "Học phí Đã đóng" → vô lý, mất trust | đọc page.tsx: empty-state cục bộ, mock blocks unconditional | **MEDIUM.** Browser-only (curl không render page). Fix: empty-children → ẩn/disable hero+stat+activity, hoặc thay bằng mock-gated khi list rỗng |
| **7** | **userType PARENT phải set đúng store cho RoleGuard; tenant-auth trả role string "PARENT" cần normalize** | `(dashboard)/parent/layout.tsx` `RoleGuard allow=[UserType.PARENT]`; `auth.ts` login map `roles:[data.role]`; `use-auth onSuccess` normalize (GAP-1122) | Nếu normalize "PARENT" → `UserType.PARENT` sai → RoleGuard bounce parent về role-home khác → không vào được `/parent/*` | đọc layout (RoleGuard PARENT-only); auth.ts roles[0]; entity_type CHECK gồm PARENT | **MEDIUM.** Browser-only (curl bỏ qua FE guard hoàn toàn). Fix: walk verify post-login landing = `/parent` (không bounce); kiểm normalize map entity_type PARENT đúng UserType |
| **8** | **Pagination from/to sai → 400 PARAM_MISSING (BE fix G1) nhưng FE attendance không gửi from/to thật (mock)** | BE `ParentAttendanceFacetController` `?from&to` required (G1 fix #1 → 400 PARAM_MISSING); FE attendance page = mock, không bao giờ gọi endpoint | Khi FE-wiring Phase 1.5, nếu quên gửi from/to → 400; hiện mock che | grep facet controller `@GetMapping /children/{childId}/attendance`; FE mock | **LOW-MED.** Phase-1.5 risk, không block G2 hiện tại. Fix: khi wire FE, default range = niên khóa hiện tại; ghi vào parity checklist |
| **9** | **Logout tenant-auth: access-token-only (no refresh) → `/api/auth/logout` với refreshToken rỗng** | `auth.ts:88` `refreshToken: ''`; `authApi.logout` post refreshToken rỗng (fail-open) | Đăng xuất có thể không revoke server-side (token stateless tự hết hạn); minor | đọc auth.ts logout + comment "tenant-auth issues access token only" | **LOW.** Browser-only nhưng fail-open. Fix: verify logout clear local token + redirect login; chấp nhận no server revoke cho tenant-auth |
| **10** | **VND format ĐÚNG (không phải finding) — ghi nhận để khỏi false-flag** | `parent-mock-data.ts:122-124` `formatVN` dùng `toLocaleString('vi-VN')` → `4.500.000đ` | Hóa đơn hiển thị đúng định dạng VND VN | grep formatVN | **N/A (PASS).** Mock amounts (4.5tr/350k) là giả, sẽ thay bằng BE fees facet Phase 1.5 (thuộc FM#2) |

---

## Recommended pre-walk batch fix (trước browser-walk)

Sắp theo confidence × impact:

- **BLOCKER (làm trước tiên) — FM#1:** seed parent trong instance `sky-education-074901` (`5b3ef1ae`):
  - `parents` row + `parent_student_links` → 1 trong 4 students sẵn có
  - `auth_credentials(PARENT, parent1@skyedu.vn, bcrypt, instance_id=5b3ef1ae…, entity_id=parents.id)`
  - consent JSONB (1 link consent rỗng để test gate, 1 grant để test happy — nếu seed 2 parent cho FM#5)
- **HIGH — FM#2 + FM#3 + FM#6:** KHÔNG fix code trước walk (Phase 1.5 scope) NHƯNG:
  - Cập nhật G2 recipe: (a) đổi từ curl-direct-core sang **browser-walk qua nip.io subdomain** (kiến trúc đã unblock), (b) cảnh báo rõ attendance/grades/billing = MOCK, chỉ transcript + danh-sách-con thật, (c) note consent gate hiện chỉ áp facet (transcript không gate) + FE chưa có CTA consent
- **MEDIUM — FM#4 + FM#7:** verify browser path: `sky-education-074901.127.0.0.1.nip.io:3000`, `docker restart kiteclass-frontend` sau rebuild, confirm post-login landing `/parent` (RoleGuard pass)
- **MEDIUM — FM#5:** seed 2 parent nếu muốn browser-probe IDOR; nếu không, ghi rõ IDOR-core verified bởi curl G1
- **LOW — FM#8/#9/#10:** defer; ghi vào parity checklist Phase 1.5

## Lưu ý meta cho coordinator

Wave plan §11 + G2 recipe §6 nói G3 deferred Phase 2 vì parent login chưa land — **state-check 2026-06-16 cho thấy Wave auth-1 ĐÃ land** (tenant-auth PARENT login + gateway `X-User-Reference-Id` inject + referenceId claim). Nên xem xét: G2/G3 cho KC-8 giờ có thể chạy browser-real qua gateway local (per `g1-browser-walk-before-flip.md` §3.2 access-mode parity), thay vì direct-core curl. Cần re-verify campaign §4 KC-8 row + cân nhắc unblock G3-functional qua G2★ (per campaign §1 G2★-absorbs-G3-functional). Đây là discovery gap-worthy → nên file gap "KC-8 G2/G3 stale Phase-2-deferral, auth-1 đã unblock".
