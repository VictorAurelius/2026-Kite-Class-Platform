---
title: Browser-walk KH-7 — Quản lý custom domain (CustomDomainTab)
audience: dev
created: 2026-06-16
flow: KH-7 (KiteHub custom domain / domain management)
product: KiteHub (KH) — FE kitehub-frontend :3001, gateway :9000, kitehub-subscription
walk_type: headless browser (Playwright chromium, real FE :3001)
verdict: ✅ FULL PASS (happy path + sad paths) — 3 cosmetic gap cataloged, 1 positive finding (GAP-1023 đã fix)
references:
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh7-domain-management.md
  - documents/05-guides/operations/2026-06-06-g2-recipe-kh7-domain-management.md
  - documents/03-planning/roadmap/flow-verification-campaign.md
---

# Browser-walk KH-7 — Quản lý custom domain

> **Verdict tổng:** ✅ **FULL PASS** — happy path (add → verify → delete) + 2 sad path (reserved, 3-label VN) chạy end-to-end qua browser thật `:3001`. 0 lỗi BLOCKING. 3 cosmetic/localization gap NHỎ cataloged + 1 **positive finding** (GAP-1023 cross-tenant IDOR nay trả 403, recipe ghi còn-mở 200 → cần re-verify status).

## 0. Bối cảnh + đính chính recipe

- **Recipe (2026-06-06) ghi "KH-7 chưa có FE page hoàn chỉnh, test backend qua API".** **SAI** — `CustomDomainTab` FE TỒN TẠI và wired đầy đủ (per GAP-1051). Đường dẫn:
  - `kitehub/kitehub-frontend/src/app/(customer)/settings/page.tsx` — tab "Tên miền" (`value="domain"`).
  - `kitehub/kitehub-frontend/src/app/(customer)/settings/components/CustomDomainTab.tsx` — render đủ 4 state (locked / NONE-form / PENDING_VERIFY / VERIFIED).
  - `kitehub/kitehub-frontend/src/hooks/use-domain.ts` — `useDomainStatus/useInitiateDomain/useVerifyDomain/useRemoveDomain`.
- **Access-mode:** KH customer portal `:3001` resolve tenant qua **JWT `tenantId` claim** (apiClient auto-inject `X-Tenant-Id` từ JWT — `client.ts:30`), KHÔNG phải subdomain. Nên `localhost:3001` LÀ production-accurate access-mode cho KH apex portal (per `g1-browser-walk-before-flip.md` §3.2 — không cần nip.io như KC subdomain flow).
- **Credential dùng:** `owner@skyedu.vn / SkyEdu@2026` (PREMIUM, sở hữu instance `sky-education`, id `e8ff87e1-69fc-4842-a263-7385c68b4ffb`). **Lưu ý:** credential recipe `owner.test@test.vn / Test@1234` sở hữu instance **FREE** → settings page (resolve `instances[0]` của user login) sẽ hiển thị **locked state**, KHÔNG tới được domain form. Browser-walk BẮT BUỘC login bằng owner của instance PREMIUM (khác curl-walk cấp instanceId tay).
- **Tool:** Playwright chromium headless (`/tmp/walk-kh7.mjs`). Stack full healthy; `kitehub-subscription` Up; reset DB `domain_status=NONE` trước + sau walk.

## 1. Walk evidence per bước (per g1-browser-walk-before-flip.md §3)

| Bước | Hành động (browser :3001) | API observed (gateway :9000) | Kết quả | Verdict |
|---|---|---|---|---|
| 0 LOGIN | `/login` fill owner@skyedu.vn → submit | `POST /api/auth/login` 200 | redirect `/dashboard`, JWT tier PREMIUM | ✅ PASS |
| 1 Baseline | `/settings` → click tab "Tên miền" | `GET .../domain` → 200 (status NONE) | render `domain-form`, badge "Chưa cài đặt", backupUrl `https://sky-education.kitehub.me` | ✅ PASS |
| 2 Add | nhập `school.com` → "Cài đặt tên miền" | `POST .../domain` → 200 | render `domain-pending`, badge "Đang chờ xác minh", TXT token `kitehub-verify=8f3e1d89-…`, hướng dẫn DNS đầy đủ | ✅ PASS |
| 3 Verify | click "Kiểm tra lại" | `POST .../domain/verify` → 200 | **giữ `PENDING_VERIFY`** (trần local DNS — đúng kỳ vọng), **KHÔNG 500** | ✅ PASS |
| 4 Delete | click "Hủy" | `DELETE .../domain` → 204 + `GET` 200 | về `domain-form` NONE | ✅ PASS |
| 5a Sad: reserved | nhập `kitehub.me` → submit | `POST .../domain` → **400** | FE hiển thị error alert (FM-5 denylist BE sống) | ✅ PASS (BE) / ⚠️ cosmetic FE (Bug #1) |
| 5b Sad: 3-label VN | nhập `truong.edu.vn` → submit | `POST .../domain` → **200** | render `domain-pending` (FM-2 multi-label fix sống) | ✅ PASS |

**Console/Network:** 0 uncaught JS error trên happy path. 2 console "Failed to load resource" = (a) expected 400 sad-path, (b) `/docs/data-reset-policy` 404 (banner link — Bug #3). 1 network ERR_ABORTED = RSC prefetch của cùng link 404.

**Screenshots:** `/tmp/kh7-step-{0-login,1-baseline,2-pending,3-verify,4-deleted,5a-reserved,5b-vn-3label}.png` (PENDING + reserved-error đã review trực quan).

## 2. Verify pre-walk failure modes (12 mục)

| FM pre-walk | Kỳ vọng | Thực tế walk | Trạng thái |
|---|---|---|---|
| FM-1 role gate (`@PreAuthorize`) | non-OWNER 403 | (không walk non-OWNER trong session này; FM-1 đã fix G1) | ⏭ skip |
| FM-2 regex 3-label VN | `truong.edu.vn` → 200 | ✅ 200 PENDING (bước 5b) | ✅ fix sống |
| FM-3 VERIFIED ceiling local | verify giữ PENDING | ✅ giữ PENDING, no 500 (bước 3) | ✅ đúng (trần) |
| FM-4 state machine (CERT_PROVISIONING/cert) | chưa đủ | không reach VERIFIED local — không quan sát được | ➡ **GAP-1024** còn mở (P1) |
| FM-5 reserved denylist | `kitehub.me` → 400 | ✅ 400 "is reserved by the platform" (bước 5a) | ✅ fix sống |
| FM-6 tier gate | instance PREMIUM mới add được | ✅ PREMIUM tới form; FREE = locked (xác nhận qua owner.test) | ✅ đúng |
| FM-8 backupUrl null | subdomain non-null → url đẹp | ✅ `https://sky-education.kitehub.me` (kitehub.me, không `null`/`kiteclass.com`) | ✅ đúng |
| FM-1/IDOR (GAP-1023) cross-tenant | recipe ghi (SAI) 200 | **403 "Access denied"** (owner.test → sky-education instance) | 🟢 **đã FIX** (xem §3) |

## 3. Catalog bug + finding

### 🟢 Positive finding — GAP-1023 cross-tenant IDOR nay trả 403 (đã fix)
- **Bằng chứng:** `owner.test@test.vn` JWT → `GET /api/instances/e8ff87e1…(của owner@skyedu)/domain` → **HTTP 403** `{"title":"Forbidden","detail":"Access denied"}`. Pre-walk + recipe (2026-06-06) ghi GAP-1023 PARTIAL / "(SAI) 200" (FM-1 chỉ chặn role chưa chặn ownership). Empirically nay đã chặn cross-tenant.
- **Hành động đề xuất:** coordinator **re-verify GAP-1023** + GAP-1015/GAP-1019 sister (sau khi đã ship ownership binding) → ứng viên flip DONE (per `audit-to-gap-pipeline.md` §2.8 fix-time state-check: symptom 200 không còn reproduce).

### ⚠️ Bug #1 [P2, cosmetic/UX] FE nuốt backend reason — hiện "Request failed with status code 400"
- **Where:** `CustomDomainTab.tsx:320` render `{(initiate.error as Error)?.message ?? 'Có lỗi xảy ra...'}`; `use-domain.ts` mutation không transform axios error.
- **Symptom:** Submit `kitehub.me` (reserved) → FE alert hiển thị **"Request failed with status code 400"** (raw axios message tiếng Anh), KHÔNG phải backend `detail` ("Domain 'kitehub.me' is reserved by the platform…") và cũng KHÔNG rơi về fallback tiếng Việt "Có lỗi xảy ra. Vui lòng thử lại." Vì `axios error.message` ≠ rỗng nên fallback không kích hoạt.
- **Root:** FE đọc `error.message` thay vì `error.response.data.detail` (RFC 7807 ProblemDetail). Cùng lớp với **GAP-926** (FE generic catch swallow backend reason). Ảnh hưởng mọi sad path domain: reserved / tier-gate / invalid-format → đều ra 1 message generic.
- **Fix gợi ý:** map `error.response.data.detail` (hoặc interceptor chung chuẩn hoá ProblemDetail → message tiếng Việt).

### ⚠️ Bug #2 [P2, localization] Backend domain error `detail` bằng tiếng Anh (tenant-facing)
- **Where:** kitehub-subscription `DomainService` (reserved message) + `DomainSetupRequest` validation message.
- **Symptom:** ProblemDetail `detail` = "Domain 'kitehub.me' is reserved by the platform and cannot be used as a custom domain" + "Invalid domain format. Use a fully-qualified domain (e.g., school.example.com or truong.edu.vn);" — tenant-facing nhưng tiếng Anh. Vi phạm `vn-localization-audit-checklist.md` §2 (error message tenant-facing PHẢI tiếng Việt).
- **Ghép Bug #1:** kể cả khi FE surface đúng `detail`, message vẫn tiếng Anh → cần fix cả 2 để UX đúng (VN-friendly).

### ⚠️ Bug #3 [P3, cosmetic, ngoài scope KH-7 core] Banner BETA link 404
- **Where:** BETA banner toàn cục `(customer)/layout` — link "Chính sách reset dữ liệu Beta" → `/docs/data-reset-policy`.
- **Symptom:** Console 404 + RSC prefetch `GET /docs/data-reset-policy?_rsc=… ERR_ABORTED`. Route không tồn tại → broken link. Không phải lỗi domain flow nhưng quan sát thấy mọi customer page.

### ℹ️ Quan sát nhỏ (không file): sau DELETE, input domain field vẫn giữ text "school.com" cũ (React `inputDomain` state không reset). Form đã đúng về NONE; chỉ là leftover value cosmetic — fix inline 1 dòng nếu muốn.

## 4. Còn mở (không phải lỗi mới)
- **GAP-1024 [P1]** state machine chưa đủ (`CERT_PROVISIONING` không set, không cert side-effect, không reach VERIFIED) — trần G2 local, cần G3 production parity (domain thật + TXT record + ACM/LE cert). Không block walk.

## 5. Tổng kết cho coordinator
- **Verdict KH-7:** ✅ **FULL PASS** (happy + sad qua browser thật). Đề xuất flip campaign §4 KH-7 → `✅ G1+G2 chờ G3 production parity`.
- **Batch-fix candidate (nhỏ, cùng PR):** Bug #1 (FE surface ProblemDetail.detail) + Bug #2 (BE message VN) — cùng lớp, fix chung 1 wave nhỏ. Bug #3 banner link riêng.
- **Re-verify gap:** GAP-1023 (cross-tenant 403 nay) → state-check → ứng viên DONE. GAP-1024 giữ P1 cho G3.
- 0 BLOCKING. KHÔNG sửa code sản phẩm trong walk này (chỉ catalog).
