---
title: G2 Human Test Recipe — KH-6 AI Branding wizard (browser-walk)
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff for KH-6 AI Branding wizard — browser-walk FE :3001 (tenant test-8 PREMIUM) + curl supplement cho BE-only job/outbox
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh6-ai-branding-wizard.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1090-instance-tier-not-synced-applypendingupgrade.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1091-ai-branding-trial-locked-mock-quota-wrong-instanceid.md
---

# G2 Human Test Recipe — KH-6 AI Branding wizard (browser-walk)

> **Sản phẩm:** KH-6 = AI Branding wizard = **KiteHub** (SaaS platform). FE = `kitehub-frontend`, port **`http://localhost:3001`** (per `kitehub-kiteclass-boundary.md` §2 — KH = `:3001`, KHÔNG phải `:3000` của KiteClass). Backend = `kitehub-branding` + `kitehub-subscription` + `kite-gateway` (`:9000`).

> **Loại walk (per `g1-browser-walk-before-flip.md` §3 + `flow-verification-campaign.md` §1 G2):** Affordance FE-wired (login / branding hub / wizard generate-apply / advanced settings) walk qua **browser thật `:3001`** để FE tự inject token + tenant header + route. Curl chỉ là **supplement** cho phần BE-only không có FE surface (job async poll, outbox row, RabbitMQ queue). KHÔNG dùng curl gắn header tay làm bằng chứng G2 cho affordance FE.

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (dev) tự xác nhận trên browser thật `:3001` rằng flow AI Branding wizard chạy đúng với tenant **PREMIUM** (`test-8`): đăng nhập → xem dashboard/branding hub hiển thị **đúng tier PREMIUM** (không trial/locked) → đi wizard generate theme/text + apply template. Walk này đồng thời **verify cluster fix GAP-1090 / GAP-1091** ("đã nâng PREMIUM nhưng UI vẫn hiển thị trial/PRO/locked").

**⚠️ PREREQ BẮT BUỘC trước khi walk** (nếu chưa đủ → các bước tier-display §3 + §4 sẽ FAIL, KHÔNG phải lỗi mới):
- **GAP-1090 fix land** — `SubscriptionService.applyPendingUpgrade` gọi `instance.setTier(targetTier)` ở cả 3 path; **backfill migration đã chạy** (resync `instances.tier` lệch → PREMIUM). Verify: tenant `test-8` instance row có `tier=PREMIUM` (xem §2 bước 4).
- **GAP-1091 fix land** — `branding/page.tsx` thay `MOCK_QUOTA` bằng `useBrandingTier(instanceId)` thật + `settings/branding/advanced/page.tsx` truyền `instanceId` đúng (không `user.id`).
- **Rebuild + restart** sau khi fix land: `bash kitehub/scripts/rebuild.sh kitehub-subscription` + `kitehub-branding` + `kitehub-frontend` rồi chờ tất cả `healthy`.
- Local Docker stack UP. Check: `docker ps --format '{{.Names}} {{.Status}}' | grep -E "branding|subscription|gateway|postgres|rabbitmq|frontend"` → tất cả `healthy`/`Up`.
- AI provider = mock mặc định (`OPENAI_API_KEY=sk-mock-key-for-local-testing` → `OpenAIClient.isMockMode()=true`). Generate trả mock, KHÔNG gọi API thật, KHÔNG cần internet (xem §6 nếu preview ảnh placehold vỡ).
- **Inline-fixes Wave flow-kh6 (Bug A + Bug B)** đã ship trên branch — verify cùng walk này:
  - **Bug A** — `branding_outbox.instance_id NOT NULL` V58 drift → `BrandingEventEmitter` giờ set `instanceId` cho outbox row.
  - **Bug B** — `XUserRolesHeaderFilter` re-auth trên async/error dispatch → 4 Mono AI endpoint không còn bị mask **401**.

**Tài khoản walk (test-8 PREMIUM — chủ đích verify gói khác trial):**
- Login: `g2test-an-8@example.com` / `WalkKh3@2026`
- `instance_id` = `7862ab7e-a960-41db-b6d7-706ac9a544fa`
- `owner_id` = `2bf182d5-72c8-4b25-90dd-529b36bf4fbe`
- subscription = **PREMIUM / ACTIVE**

**Thời lượng:** ~15-20 phút (browser-walk FE + ~3 phút curl supplement BE-only).

**Lưu ý FE auto-inject header (per `g1-browser-walk-before-flip.md` §3 evidence d):** FE `apiClient` (`kitehub-frontend/src/lib/api/client.ts`) **tự gắn** `Authorization: Bearer <token>` + `X-Tenant-Id` (từ JWT claim) cho mọi request — bạn KHÔNG gắn tay. `instanceId` được FE resolve từ owner's instances rồi truyền vào **body/path** (KHÔNG phải `X-Instance-Id` header). Đây là khác biệt với phần curl supplement (§3 BE-only) — curl dùng `X-Instance-Id` header tay vì gọi trực tiếp gateway, không qua FE.

## 2. Setup

### 2.1 Browser + DevTools
- Mở Chrome/Edge → DevTools (F12) → tab **Network** (filter `Fetch/XHR`) + tab **Console**.
- Để ý: request chính đi tới gateway `http://localhost:9000`; FE chạy ở `http://localhost:3001`.

### 2.2 Verify instance.tier đã backfill PREMIUM (BE-only, 1 lệnh — confirm prereq GAP-1090)
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT i.id, i.tier AS instance_tier, s.tier AS sub_tier, s.status
   FROM instances i
   LEFT JOIN subscriptions s ON s.instance_id = i.id AND s.status = 'ACTIVE'
   WHERE i.id = '7862ab7e-a960-41db-b6d7-706ac9a544fa';"
```
**✅ Kỳ vọng (post GAP-1090 fix + backfill):** `instance_tier = PREMIUM` **VÀ** `sub_tier = PREMIUM`, `status = ACTIVE`. Nếu `instance_tier = FREE` mà `sub_tier = PREMIUM` → split-brain GAP-1090 chưa fix/backfill → dashboard sẽ hiện trial (xem §3 Bước 2 sad path).

(Nếu `docker exec ... -U kitehub` báo lỗi auth, thử `docker exec kite-postgres psql -U kitehub -h localhost -p 5432 -d kitehub -c "..."` hoặc dùng password `PGPASSWORD=kitehub_dev_password`.)

### 2.3 Tools curl supplement (BE-only)
Terminal + `curl` + `jq`. Optional: RabbitMQ admin UI `http://localhost:15672` xem queue `BRANDING_QUEUE`.

## 3. Các bước

> Mỗi bước FE-wired có 4 sub-section: **Hành động UI** / **✅ Kỳ vọng PASS** (Network 2xx + FE auto-inject header) / **⚠️ Sad path** / **🔍 Verify**. Bước BE-only đánh dấu rõ "BE-only verify, no FE surface".

### Bước 1 — Đăng nhập browser `:3001` (FE-wired)
**Hành động UI:** Mở `http://localhost:3001/login` → nhập `g2test-an-8@example.com` / `WalkKh3@2026` → click **Đăng nhập**.

**✅ Kỳ vọng PASS:**
- Redirect tới `/dashboard`.
- DevTools Network: `POST http://localhost:9000/api/auth/login` → **200** + body chứa `accessToken`.
- DevTools Application → Session Storage `http://localhost:3001` → có key chứa `accessToken` (per-tab isolation, GAP-599).
- Từ request kế tiếp trở đi, Network tab cho thấy FE **tự gắn** `Authorization: Bearer ...` + `X-Tenant-Id: ...` (KHÔNG do bạn gắn tay).

**⚠️ Sad path:** Sai mật khẩu → `POST /api/auth/login` **401** + FE **hiển thị error message rõ ràng** (KHÔNG silent fail — GAP-924 fix: `/api/auth/login` nằm trong `AUTH_FLOW_401_PASSTHROUGH`, không bị refresh-redirect che).

**🔍 Verify:** Console không có uncaught error trên happy path.

### Bước 2 — Dashboard tier-display (FE-wired · verify GAP-1090c)
**Hành động UI:** Ở `/dashboard`, xem **KPI tier card** + khu vực **thanh trial-countdown** ("còn N ngày dùng thử").

**✅ Kỳ vọng PASS (post GAP-1090 fix + backfill):**
- KPI tier card hiển thị **"PREMIUM"** (tone positive, không cảnh báo trial).
- **KHÔNG có thanh trial-countdown** "còn N ngày dùng thử" (PREMIUM active, không trial).
- Network: `GET .../instances...` **200**, response `instance.tier = PREMIUM`, `trialDaysLeft = null`.

**⚠️ Sad path (= triệu chứng GAP-1090 nếu prereq chưa đủ):** Card vẫn hiện trial / "còn N ngày" → 1 trong 3: (1) GAP-1090 fix chưa land, (2) backfill migration chưa chạy (`instance.tier` kẹt FREE — xem §2.2), (3) FE cache stale → **reload trang** (subscription mutation hooks invalidate `['instances']` là phần fix GAP-1090).

**🔍 Verify:** Network request tới `:9000` trả 2xx; `Authorization` + `X-Tenant-Id` auto-injected (KHÔNG gắn tay).

### Bước 3 — Branding hub quota + tier (FE-wired · verify GAP-1091a)
**Hành động UI:** Vào `/branding` (sidebar **Thương hiệu AI** hoặc gõ URL `http://localhost:3001/branding`). Xem **widget "Quota làm lại brand"** trên cùng.

**✅ Kỳ vọng PASS (post GAP-1091a fix):**
- Widget hiển thị **"Quota làm lại brand · gói PREMIUM"** + **"30 / 30 lượt còn lại trong tháng này"** (tier + limit lấy từ `useBrandingTier(instanceId)` thật — PREMIUM quota = 30).
- **KHÔNG có CTA "Nâng cấp PREMIUM"** (vì `canUpgradeTier` chỉ true cho FREE/BASIC; PREMIUM đã ≥ PREMIUM).
- Network: `GET .../subscriptions/instance/7862ab7e.../active` **200**, `tier = PREMIUM`.

**⚠️ Sad path (= triệu chứng GAP-1091a nếu chưa fix):** Widget hiện **"gói PRO"** + "3/10 lượt" + CTA "Nâng cấp PREMIUM" vĩnh viễn → `MOCK_QUOTA = {tier:'PRO', limit:10}` hardcode chưa thay bằng `useBrandingTier`.

**🔍 Verify:** Network request branding → `Authorization: Bearer` auto-injected; `instanceId` nằm trong **path/body** (KHÔNG phải `X-Instance-Id` header gắn tay).

### Bước 4 — Wizard generate theme/text (FE-wired)
**Hành động UI:** Ở `/branding` click **"Tạo lại brand"** (hoặc **"Bắt đầu wizard"** ở empty state) → vào `/branding/wizard` (6 bước). Đi tuần tự:
1. **Welcome** — nhập tên trung tâm (vd `Trung tâm Anh ngữ Sky Education`) + slug → Tiếp.
2. **Logo** — upload logo HOẶC chọn AI-generate → Tiếp.
3. **Audience** — chọn 1 trong 4 thẻ audience VN → Tiếp.
4. **Tone** — chọn 1 trong 4 thẻ tone → Tiếp.
5. **Template** — chọn 1 template trong grid (đây là nơi generate theme) → Tiếp.
6. **Preview** — xem preview palette + marketing copy VN (generate text/assets) + approve per-resource.

**✅ Kỳ vọng PASS:**
- `StepIndicator` advance qua từng bước; mỗi step render đúng (không blank / không crash).
- Network các call generate: `POST .../branding/ai/generate-theme` + `.../ai/generate-text` (hoặc qua job) → **200** (mock deterministic). Preview hiển thị palette màu + đoạn marketing copy **tiếng Việt**.
- FE auto-inject `Authorization` + `X-Tenant-Id`; `instanceId` resolve từ owner instances → truyền body/path.

**⚠️ Sad path:** Input AI quá dài → **400** `AI_INPUT_TOO_LONG` + `{estimatedTokens, maxTokens, tier}` (PREMIUM cap cao hơn FREE nên khó chạm). Quota cạn → nút generate **disabled**.

**🔍 Verify:** Không gọi OpenAI thật — `docker logs kitehub-branding 2>&1 | tail -20 | grep -i mock` thấy mock path.

### Bước 5 — Apply / deploy template (FE-wired)
**Hành động UI:** Ở **Step 6 Preview**, approve → click **Apply / Triển khai** (deploy).

**✅ Kỳ vọng PASS:**
- Network `POST .../branding/templates/{id}/apply` **200** (hoặc job deploy) → redirect `/branding?success=true` → toast **"Branding đã được xuất bản thành công!"**.

**⚠️ Sad path / 🔍 Verify (KNOWN-ISSUE — đừng coi là blocker):**
- **GAP-1021 P1** — apply trả `status:applied` NHƯNG theme **chưa persist** thành active theme của instance (job-apply persistence gap). KHÔNG block G2.
- **GAP-1021 P1 (SSE)** — nếu deploy dùng SSE `deploy-stream` qua browser `EventSource` → **401** (EventSource không gửi custom auth header). KHÔNG block G2.

### Bước 6 — Templates gallery (FE-wired)
**Hành động UI:** Vào `http://localhost:3001/branding/templates` (hoặc nút **Template Gallery** ở hub).

**✅ Kỳ vọng PASS:** Network `GET .../branding/templates` **200** + grid template render (vd `Classic Academy` + others).

**⚠️ Sad path:** Grid rỗng / 404 → kiểm tra branding service healthy + Network status.

### Bước 7 — Advanced branding settings (FE-wired · verify GAP-1091b)
**Hành động UI:** Vào `http://localhost:3001/settings/branding/advanced`.

**✅ Kỳ vọng PASS (post GAP-1091b fix):**
- Notice hiển thị đúng tier: **"Tính năng chỉ dành cho ENTERPRISE — Gói hiện tại (PREMIUM)"** — tức hiện **`(PREMIUM)`**, **KHÔNG phải `(FREE)`**.
- Network `GET .../subscriptions/instance/7862ab7e.../active` **200** (KHÔNG 404).

> **LƯU Ý quan trọng (design-accurate):** Advanced Mode là **ENTERPRISE-only** per `ai-branding-guidelines.md` §2.4 (`useBrandingTier` đặt `advancedModeEnabled = tier === 'ENTERPRISE'`). Vì test-8 là **PREMIUM** (< ENTERPRISE), toggle Advanced **vẫn ẩn** sau fix — đây là ĐÚNG behavior, KHÔNG phải bug. Fix GAP-1091b chỉ sửa **lỗi hiển thị sai tier**: trước fix gọi `useBrandingTier(user.id)` → `/subscriptions/instance/{owner_id}/active` **404** → fallback `'FREE'` → notice hiện sai `(FREE)`. Sau fix truyền `instanceId` đúng → notice hiện đúng `(PREMIUM)`. Muốn verify toggle Advanced **thật sự unlock** (hiện ô custom-prompt 200 ký tự) phải dùng tenant **ENTERPRISE** — ngoài scope walk PREMIUM này.

**⚠️ Sad path (= triệu chứng GAP-1091b nếu chưa fix):** Notice hiện **`(FREE)`** + Network `GET .../subscriptions/instance/{owner_id}/active` **404** → call-site vẫn truyền `user.id` thay vì `instanceId`.

### Bước S1 — Outbox row có instance_id (BE-only verify, no FE surface · Bug A)
> BE-only: kiểm tra cơ chế outbox không có FE surface trực tiếp.
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT event_type, instance_id, status FROM branding_outbox ORDER BY created_at DESC LIMIT 3;"
```
**✅ Kỳ vọng PASS:** Cột `instance_id` **có giá trị** (không NULL) cho row mới tạo từ wizard generate (Bước 4). Đây là bằng chứng **Bug A đã fix** (`BrandingEventEmitter` set instanceId — trước đây `branding_outbox.instance_id NOT NULL` V58 drift → INSERT 500 bị mask 401).

### Bước S2 — Job async + RabbitMQ (BE-only verify, no FE surface · Bug B + outbox relay)
> BE-only: poll trực tiếp job status endpoint qua gateway (FE wizard poll internally; đây verify cơ chế async độc lập). Curl dùng `X-Instance-Id` header tay vì gọi thẳng gateway, KHÔNG qua FE.
```bash
GW=http://localhost:9000
# Login lấy token cho curl supplement (token này tách biệt session browser)
TOKEN=$(curl -s -X POST $GW/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"g2test-an-8@example.com","password":"WalkKh3@2026"}' | jq -r '.accessToken')
INSTANCE=7862ab7e-a960-41db-b6d7-706ac9a544fa
AUTH=(-H "Authorization: Bearer $TOKEN"); INST=(-H "X-Instance-Id: $INSTANCE")

# Sanity Bug B fix: Mono endpoint không còn bị mask 401 trên async dispatch
curl -s -w " [%{http_code}]" "$GW/api/v1/branding/regenerate-quota" "${AUTH[@]}" "${INST[@]}"
```
**✅ Kỳ vọng PASS:** `regenerate-quota` → **200** + JSON quota (tier PREMIUM). Xác nhận `XUserRolesHeaderFilter` set auth đúng (**Bug B fixed** — trước đây Mono bị mask **401**).

**⚠️ Sad path:** Vẫn 401 dù token hợp lệ → Bug B chưa nạp: `bash kitehub/scripts/rebuild.sh kitehub-branding` rồi chờ `healthy`.

**🔍 Verify (KNOWN-ISSUE — đừng coi là blocker):**
- RabbitMQ: `docker ps | grep rabbitmq` phải Up; admin `http://localhost:15672` → queue `BRANDING_QUEUE`.
- **GAP-1022 P2** — outbox **relay** (poll-then-dispatch) hiện chưa hoạt động đầy đủ; G1 đi qua nhờ fast-path publish. Nếu job kẹt `QUEUED` là biểu hiện GAP-1022, KHÔNG phải lỗi mới.

## 4. Tier-display verification summary (sau GAP-1090/1091 fix land)

Bảng tổng hợp 3 surface tier-display cho tenant **PREMIUM** — đây là trọng tâm walk này (verify cluster "PREMIUM shows trial/PRO/locked UI"):

| # | Surface (browser `:3001`) | ✅ Kỳ vọng PASS (post-fix) | ⚠️ Triệu chứng nếu chưa fix | Gap |
|---|---|---|---|---|
| (a) | `/branding` quota widget (Bước 3) | "gói **PREMIUM**" + "30/30 lượt" + **KHÔNG** CTA "Nâng cấp PREMIUM" | "gói **PRO**" + CTA upgrade vĩnh viễn (MOCK_QUOTA hardcode) | **GAP-1091a** |
| (b) | `/settings/branding/advanced` (Bước 7) | Notice "Gói hiện tại **(PREMIUM)**" (toggle vẫn ẩn — ENTERPRISE-only, đúng) | Notice "Gói hiện tại **(FREE)**" + `/instance/{owner_id}/active` **404** | **GAP-1091b** |
| (c) | `/dashboard` tier card + trial bar (Bước 2) | Tier card "**PREMIUM**" + **KHÔNG** thanh trial-countdown | Card hiện trial / "còn N ngày dùng thử" (instance.tier kẹt FREE) | **GAP-1090** (sau backfill) |

## 5. Sad path quick checks

| Case | Expected | Ghi chú |
|---|---|---|
| Sai mật khẩu ở `/login` | 401 + FE hiển thị error rõ | GAP-924 fix: không silent fail |
| Bỏ token (gọi BE trực tiếp không Authorization) | 401 `AUTH_REQUIRED` | default-deny SecurityConfig |
| Input AI vượt token cap tier | 400 `AI_INPUT_TOO_LONG` | PREMIUM cap cao hơn FREE |
| `regenerate` thiếu `Idempotency-Key` | 400 `MISSING_IDEMPOTENCY_KEY` | FM-11 pre-walk |
| ⚠️ **KNOWN-ISSUE** curl `X-Instance-Id=<instance owner khác>` → đọc/tạo job tenant khác | **200 (IDOR — bug)** | **GAP-1019 P0** — header client-controlled không bind từ JWT; Phase 1 BETA chấp nhận, fix chung GAP-1015 |
| ⚠️ **KNOWN-ISSUE** curl `X-Subscription-Tier: ENTERPRISE` → bypass quota | unlimited (bug) | **GAP-1020 P1** — tier header client-controlled + RLS GUC chưa set |
| ⚠️ **KNOWN-ISSUE** apply template → theme không persist active | `status:applied` nhưng instance theme không đổi | **GAP-1021 P1** — job-apply persistence gap |
| ⚠️ **KNOWN-ISSUE** SSE `deploy-stream` qua browser EventSource | 401 | **GAP-1021 P1** — EventSource không gửi custom header |
| ⚠️ **KNOWN-ISSUE** job kẹt QUEUED nếu chỉ dựa outbox relay | relay không dispatch | **GAP-1022 P2** — fast-path che |

## 6. Báo kết quả

Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** — Bước 1-7 happy path đạt **VÀ** §4 tier-display (a)(b)(c) đúng PREMIUM (KNOWN-ISSUE GAP-1019/1020/1021/1022 không tính) → Claude flip campaign KH-6 → ✅ G1+G2 chờ G3.
- ⚠️ **MOSTLY PASS** — core chạy nhưng cosmetic mới (vd preview ảnh placehold vỡ do egress, layout shift nhỏ) → catalog gap polish; nếu nhỏ-rẻ-bounded thì fix inline per `small-gap-inline-fix.md`.
- 🔴 **BLOCKING** — bước happy path HOẶC §4 tier-display fail KHÔNG nằm trong KNOWN-ISSUE list (vd vẫn hiện trial/PRO/FREE → GAP-1090/1091 fix chưa đủ) → catalog blocker + fix loop + re-walk per `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch.
- ❓ **UNCLEAR** → ping kèm screenshot DevTools (Network status + Console) + output `docker logs kitehub-branding`.

## 7. Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| `:3001` mở không ra / ERR_EMPTY_RESPONSE | FE container/proxy stale sau compose-up — `docker inspect kitehub-frontend --format '{{.State.Health.Status}}'` chờ `healthy`; nếu vẫn lỗi `bash kitehub/scripts/rebuild.sh kitehub-frontend` |
| Login OK nhưng tier vẫn trial/PRO/FREE | Prereq GAP-1090/1091 chưa đủ — verify §2.2 (instance.tier), rebuild subscription+branding+frontend, chạy backfill migration; reload trang (FE cache) |
| `/settings/branding/advanced` hiện `(FREE)` + 404 | GAP-1091b chưa fix (truyền `user.id` thay `instanceId`) — rebuild frontend sau khi fix land |
| Bước S2 `regenerate-quota` vẫn 401 dù token đúng | Bug B chưa nạp — `bash kitehub/scripts/rebuild.sh kitehub-branding` |
| Outbox row `instance_id` NULL (Bước S1) | Bug A chưa nạp — rebuild branding; re-run wizard generate (Bước 4) tạo row mới |
| Job kẹt QUEUED | RabbitMQ down hoặc relay (GAP-1022) — check `docker ps | grep rabbitmq` + admin UI queue; rabbit up mà vẫn kẹt = GAP-1022 (không block) |
| `generate-image` preview ảnh vỡ | Mock trả `placehold.co` URL — cần egress internet; cosmetic (FM-9), không chặn flow |
| branding fail startup | Schema coupling (FM-8): branding `ddl-auto=validate` cần bảng do kitehub-subscription Flyway tạo — đảm bảo subscription healthy TRƯỚC |

**G3 production-parity — DEFERRED Phase 2:** Walk qua gateway production thật cần JWT-issued tenant binding + gateway inject `X-Instance-Id` từ JWT claim (hiện client-controlled = GAP-1019). G3 unblock khi gateway-tenant-bind hardening (GAP-1015 + GAP-1019) land. G2 này test qua browser `:3001` local — FE tự inject token/tenant header (production-ready FE path), curl supplement chỉ cho cơ chế BE-only.

**Known defers (không block G2):** GAP-1019 (X-Instance-Id IDOR P0), GAP-1020 (RLS GUC + tier header trust P1), GAP-1021 (job-apply persistence + SSE auth P1), GAP-1022 (outbox relay P2). Inline-fixed wave này: Bug A (outbox instance_id V58 drift) + Bug B (filter async/error dispatch 401) — verify ở Bước S1 + S2.

---

## G1/G2 browser-walk evidence (per `g1-browser-walk-before-flip.md` §3)

Điền khi walk (browser thật `:3001`, KHÔNG curl gắn header tay):

| # | Evidence | Tiêu chí PASS | Kết quả |
|---|---|---|---|
| (a) | FE entry point thật | Mở `http://localhost:3001/login` + `/branding` + `/branding/wizard` + `/settings/branding/advanced` trên browser (KHÔNG curl) | ⬜ |
| (b) | Console clean | DevTools Console không uncaught error / ERR_EMPTY_RESPONSE / failed-to-fetch trên happy path | ⬜ |
| (c) | Network status | Request chính tới gateway `:9000` trả **2xx** (login 200, subscription/active 200, branding/templates 200, generate 200) | ⬜ |
| (d) | FE-injected header observed | Network → Request Headers cho thấy FE **tự gắn** `Authorization: Bearer` + `X-Tenant-Id` (từ JWT, KHÔNG do tay); `instanceId` trong body/path | ⬜ |
| (e) | FE route resolves | Next.js routes `/dashboard`, `/branding`, `/branding/wizard`, `/branding/templates`, `/settings/branding/advanced` render đúng (KHÔNG 404 / redirect loop / blank) | ⬜ |
| (f) | ≥1 sad path qua browser | Sai mật khẩu → FE hiển thị error rõ (KHÔNG silent) **HOẶC** advanced page hiển thị đúng tier `(PREMIUM)` không `(FREE)` | ⬜ |
