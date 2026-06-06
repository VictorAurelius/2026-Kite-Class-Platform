---
title: G2 Human Test Recipe — KH-6 AI Branding wizard
audience: dev
created: 2026-06-06
scope: Flow Verification Campaign G2 handoff for KH-6 AI Branding wizard chain (generate → job async → assets → apply)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-06-flow-kh6-ai-branding-wizard.md
  - documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md
---

# G2 Human Test Recipe — KH-6 AI Branding wizard

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (dev) tự xác nhận flow AI Branding wizard chạy thật trên local Docker stack — Owner: generate theme/text (AI mock) → tạo branding job (async qua RabbitMQ + outbox) → poll job tới `COMPLETED` → đọc assets → apply template. Đây chính là logic vừa walk G1 (KH-6 G1 ✅ PASS sau 2 inline fix).

**Prereq:**
- Local Docker stack UP, services healthy. Check: `docker ps --format '{{.Names}} {{.Status}}' | grep -E "branding|gateway|subscription|postgres|rabbitmq"` → tất cả `healthy`/`Up`.
- AI provider = mock mặc định (`OPENAI_API_KEY=sk-mock-key-for-local-testing` → `OpenAIClient.isMockMode()=true`). Generate trả mock data, KHÔNG gọi API thật, KHÔNG cần internet (xem §6 nếu preview ảnh vỡ).
- Credential Owner: `owner.test@test.vn / Test@1234`. Instance/tenant của owner này: `22003e3c-...` (lấy chính xác qua §2 Setup — decode JWT hoặc query DB).
- Fixes Wave flow-kh6 đã ship trên branch: **Bug A** (`branding_outbox.instance_id` NOT NULL V58 drift → emitter giờ set instanceId) + **Bug B** (`XUserRolesHeaderFilter` re-auth trên async/error dispatch → 4 Mono AI endpoint không còn bị mask 401).

**Thời lượng:** ~12-18 phút (API walk qua gateway). Generate + job async + apply là core; SSE preview/deploy-stream là optional (xem KNOWN-ISSUE GAP-1021).

**Lưu ý quan trọng:** Gateway inject `X-User-Id` / `X-User-Roles` từ JWT, NHƯNG **KHÔNG inject `X-Instance-Id`** — bạn phải tự gắn header này. Đây vừa là walk-requirement vừa là root của GAP-1019 (IDOR, xem §4) — Phase 1 BETA chấp nhận, sẽ fix wave bảo mật gateway-tenant-bind chung với GAP-1015.

## 2. Setup

```bash
GW=http://localhost:9000

# (1) Đăng nhập Owner → lấy accessToken
TOKEN=$(curl -s -X POST $GW/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner.test@test.vn","password":"Test@1234"}' | jq -r '.accessToken')
echo "TOKEN len: ${#TOKEN}"   # > 0 nghĩa login OK

# (2) Lấy instanceId của owner (cách A: decode JWT; cách B: query DB)
#   Cách A — nếu JWT có claim instanceId:
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq '.' 2>/dev/null
#   Cách B — query DB nếu JWT không chứa (instance gắn theo owner email):
PGPASSWORD=kitehub_dev_password psql -h localhost -p 5433 -U kitehub -d kitehub -c \
  "SELECT id FROM instances WHERE owner_email='owner.test@test.vn' LIMIT 1;" 2>/dev/null

INSTANCE=22003e3c-0000-0000-0000-000000000000   # ← thay bằng giá trị thật từ bước (2)

# (3) Headers tái sử dụng
AUTH=(-H "Authorization: Bearer $TOKEN")
INST=(-H "X-Instance-Id: $INSTANCE")
JSON=(-H "Content-Type: application/json")
```

Tools: terminal + `curl` + `jq`. (Optional: RabbitMQ admin UI `http://localhost:15672` để xem queue `BRANDING_QUEUE`; browser DevTools nếu test SSE.)

## 3. Các bước

### Bước 1 — Sanity: Owner auth qua branding endpoint (verify Bug B fix)
**Hành động:**
```bash
curl -s -w " [%{http_code}]" "$GW/api/v1/branding/regenerate-quota" "${AUTH[@]}" "${INST[@]}"
```
**✅ Kỳ vọng (PASS):** HTTP **200** + JSON quota (vd `{"tier":"FREE","remaining":3,...}`). Đây xác nhận `XUserRolesHeaderFilter` set auth đúng + role `OWNER` qua `@PreAuthorize` (Bug B đã fix — trước đây Mono endpoint bị mask **401** trên async dispatch).
**⚠️ Sad path:** Bỏ `Authorization` → **401** `AUTH_REQUIRED`. Role không phải Owner → **403**.
**🔍 Verify:** Nếu vẫn 401 dù token hợp lệ → Bug B chưa nạp: `bash kitehub/scripts/rebuild.sh kitehub-branding` rồi chờ `healthy`.

### Bước 2 — Generate theme (AI mock, deterministic)
**Hành động:**
```bash
curl -s -w "\n[%{http_code}]" -X POST "$GW/api/platform/branding/ai/generate-theme" \
  "${AUTH[@]}" "${INST[@]}" "${JSON[@]}" \
  -H "X-Subscription-Tier: FREE" \
  -d '{"primaryColor":"#2563eb","secondaryColor":"#7c3aed","accentColor":"#f59e0b","theme":"professional","typography":"modern","targetAudience":"phụ huynh và học sinh THCS"}'
```
**✅ Kỳ vọng (PASS):** HTTP **200** + `ThemeConfig` JSON (colors + typography + spacing + layout). Deterministic — chạy 2 lần ra cùng palette (mock mode).
**⚠️ Sad path:** Input quá dài (vượt cap tier FREE 2000 token) → **400** `AI_INPUT_TOO_LONG` + `{estimatedTokens, maxTokens, tier}`.
**🔍 Verify:** Không có call OpenAI thật — `docker logs kitehub-branding 2>&1 | tail -20 | grep -i mock` thấy mock path.

### Bước 3 — Generate marketing text (AI mock VN copy)
**Hành động:**
```bash
curl -s -w "\n[%{http_code}]" -X POST "$GW/api/platform/branding/ai/generate-text" \
  "${AUTH[@]}" "${INST[@]}" "${JSON[@]}" \
  -H "X-Subscription-Tier: FREE" \
  -d '{"organizationName":"Trung tâm Anh ngữ Sao Mai","theme":"professional","targetAudience":"học sinh THPT"}'
```
**✅ Kỳ vọng (PASS):** HTTP **200** + `{"text":"..."}` — đoạn marketing copy tiếng Việt (mock).
**⚠️ Sad path:** Thiếu `Authorization` → **401**. Body sai schema (thiếu `organizationName`) → **400** validation.

### Bước 4 — Xem templates gallery
**Hành động:**
```bash
curl -s -w "\n[%{http_code}]" "$GW/api/platform/branding/templates" "${AUTH[@]}" "${INST[@]}" | jq '.[] | {id, name}'
```
**✅ Kỳ vọng (PASS):** HTTP **200** + danh sách template (vd `Classic Academy` + others). Ghi lại 1 `id` để dùng Bước 7.

### Bước 5 — Tạo branding job (async, verify Bug A fix)
**Hành động:** (lưu ý: `createJob` nhận **query params**, KHÔNG phải JSON body)
```bash
JOB=$(curl -s -X POST "$GW/api/platform/branding/jobs?organizationName=Sao%20Mai&language=vi&logoUrl=https://placehold.co/200x200/png" \
  "${AUTH[@]}" "${INST[@]}")
echo "$JOB" | jq '{id, status, progress}'
JOB_ID=$(echo "$JOB" | jq -r '.id')
```
**✅ Kỳ vọng (PASS):** HTTP **201** + job `{"status":"QUEUED","progress":0,...}`. Đây xác nhận **Bug A fix**: `BrandingEventEmitter` giờ set `instance_id` cho outbox row (trước đây `branding_outbox.instance_id NOT NULL` V58 drift → INSERT 500 bị mask thành 401 → chặn TOÀN BỘ job creation).
**⚠️ Sad path (verify Bug A path + missing-header guard):** Bỏ `X-Instance-Id` →
```bash
curl -s -w " [%{http_code}]" -X POST "$GW/api/platform/branding/jobs?organizationName=X&logoUrl=https://placehold.co/200x200/png" "${AUTH[@]}"
```
→ HTTP **400** (missing required header `X-Instance-Id`). Đúng — trước Bug B fix lỗi này bị mask thành **401**.
**🔍 Verify (outbox row có instance_id):**
```bash
PGPASSWORD=kitehub_dev_password psql -h localhost -p 5433 -U kitehub -d kitehub -c \
  "SELECT event_type, instance_id, status FROM branding_outbox ORDER BY created_at DESC LIMIT 3;"
```
→ Cột `instance_id` PHẢI có giá trị (không NULL). Đây là bằng chứng Bug A đã fix.

### Bước 6 — Poll job tới COMPLETED (async qua RabbitMQ consumer)
**Hành động:**
```bash
for i in $(seq 1 10); do
  ST=$(curl -s "$GW/api/platform/branding/jobs/$JOB_ID" "${AUTH[@]}" "${INST[@]}" | jq -r '.status')
  echo "poll $i: $ST"
  [ "$ST" = "COMPLETED" ] && break
  sleep 2
done
```
**✅ Kỳ vọng (PASS):** trong ~5-10s status chuyển `QUEUED → PROCESSING → COMPLETED` (`BrandingJobConsumer` xử lý qua `BRANDING_QUEUE`).
**⚠️ Sad path:** Job kẹt `QUEUED` mãi → outbox relay/dispatcher hoặc RabbitMQ chưa chạy. Check:
```bash
docker ps | grep rabbitmq                      # phải Up
# RabbitMQ admin http://localhost:15672 → queue BRANDING_QUEUE có message + consumer?
```
**🔍 Verify:** Xem §4 KNOWN-ISSUE GAP-1022 — outbox **relay** (poll-then-dispatch) hiện chưa hoạt động đầy đủ; G1 đi qua nhờ fast-path publish. Nếu kẹt QUEUED là biểu hiện GAP-1022, KHÔNG phải lỗi mới.

### Bước 7 — Đọc assets + apply template
**Hành động:**
```bash
# Assets của job đã COMPLETED
curl -s -w "\n[%{http_code}]" "$GW/api/platform/branding/jobs/$JOB_ID/assets" "${AUTH[@]}" "${INST[@]}" | jq 'keys'

# Apply 1 template (TEMPLATE_ID từ Bước 4)
TEMPLATE_ID=<paste-id-từ-Bước-4>
curl -s -w "\n[%{http_code}]" -X POST "$GW/api/platform/branding/templates/$TEMPLATE_ID/apply" \
  "${AUTH[@]}" "${INST[@]}" "${JSON[@]}" -d '{}'
```
**✅ Kỳ vọng (PASS):**
- assets → **200** + full asset map (copy + logos + hero + og + profile).
- apply → **200** + `{"status":"applied", "themeConfig":{...}}`.
**⚠️ Sad path:** assets khi job chưa COMPLETED → trả `null`/rỗng (đúng — chưa generate xong).
**🔍 Verify (KNOWN-ISSUE — đừng coi là blocker):** Xem §4 GAP-1021 — apply trả `status:applied` NHƯNG theme **chưa persist** thành active theme của instance (job-apply persistence gap). Đây là gap chức năng thật của bước "approve", track Phase 1.5+, KHÔNG block G2.

## 4. Sad path quick checks

| Case | Expected | Ghi chú |
|---|---|---|
| Bỏ `Authorization` trên branding endpoint | 401 `AUTH_REQUIRED` | default-deny SecurityConfig |
| Token Owner, bỏ `X-Instance-Id` khi create job | 400 (missing header) | Bug B fix: trước bị mask 401 |
| Role không phải Owner trên generate/create | 403 | `@PreAuthorize` |
| Input AI vượt token cap tier FREE | 400 `AI_INPUT_TOO_LONG` | §2.5 ai-branding-guidelines |
| `regenerate` thiếu `Idempotency-Key` | 400 `MISSING_IDEMPOTENCY_KEY` | FM-11 pre-walk |
| ⚠️ **KNOWN-ISSUE** set `X-Instance-Id=<instance của owner khác>` → đọc/tạo job tenant khác | **200 (IDOR — bug)** | **GAP-1019 P0** — header client-controlled không bind từ JWT; Phase 1 BETA chấp nhận, fix chung GAP-1015 |
| ⚠️ **KNOWN-ISSUE** set `X-Subscription-Tier: ENTERPRISE` → bypass rate-limit/quota | unlimited (bug) | **GAP-1020 P1** — tier header client-controlled + RLS GUC chưa set |
| ⚠️ **KNOWN-ISSUE** apply template → theme không persist active | `status:applied` nhưng instance theme không đổi | **GAP-1021 P1** — job-apply persistence gap |
| ⚠️ **KNOWN-ISSUE** SSE `preview` / `deploy-stream` qua browser EventSource | 401 (EventSource không gửi custom header) | **GAP-1021 P1** — SSE auth gap |
| ⚠️ **KNOWN-ISSUE** job kẹt QUEUED nếu chỉ dựa outbox relay | relay không dispatch | **GAP-1022 P2** — outbox relay chưa hoạt động; fast-path che |

## 5. Báo kết quả

Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** (Bước 1-7 happy path đạt, KNOWN-ISSUE không tính) → Claude flip campaign KH-6 → ✅ G1+G2 chờ G3.
- ⚠️ **MOSTLY PASS** (core chạy nhưng cosmetic mới, vd preview ảnh placehold vỡ do egress) → catalog gap polish.
- 🔴 **BLOCKING** (Bước nào trong happy path fail KHÔNG nằm trong KNOWN-ISSUE list) → catalog blocker + fix loop + re-walk per `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch.
- ❓ **UNCLEAR** → ping kèm output curl + `[%{http_code}]` + log `docker logs kitehub-branding`.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| HTTP 000 | Container restarting — `docker inspect kitehub-branding --format '{{.State.Health.Status}}'` chờ `healthy` |
| Bước 1 vẫn 401 dù token đúng | Bug B chưa nạp — `bash kitehub/scripts/rebuild.sh kitehub-branding` |
| Bước 5 create job 500/401 | Bug A chưa nạp (outbox instance_id) — rebuild branding; verify §Bước 5 outbox query có instance_id |
| Job kẹt QUEUED | RabbitMQ down hoặc relay (GAP-1022) — check `docker ps | grep rabbitmq` + admin UI queue; nếu rabbit up mà vẫn kẹt = GAP-1022 (không block) |
| `generate-image` preview ảnh vỡ | Mock trả `placehold.co` URL — cần egress internet; cosmetic (FM-9), không chặn flow |
| branding fail startup | Schema coupling (FM-8): branding `ddl-auto=validate` cần bảng do kitehub-subscription Flyway tạo — đảm bảo subscription healthy TRƯỚC; `psql ... -c "\dt branding_*"` thấy bảng |
| `INSTANCE` không đúng | Lấy lại qua §2 Setup bước (2); sai instance → list/assets rỗng hoặc 400 |

**G3 production-parity — DEFERRED Phase 2:** Walk qua gateway production thật cần JWT-issued tenant binding + gateway inject `X-Instance-Id` từ JWT claim (hiện client-controlled = GAP-1019). G3 unblock khi gateway-tenant-bind hardening (GAP-1015 + GAP-1019) land. G2 hiện test qua gateway local với header tự gắn — đúng cách cho Phase 1 BETA, đó là logic production-ready của BE branding wizard.

**Known defers (không block G2):** GAP-1019 (X-Instance-Id IDOR P0), GAP-1020 (RLS GUC + tier header trust P1), GAP-1021 (job-apply persistence + SSE auth P1), GAP-1022 (outbox relay P2). Inline-fixed wave này: Bug A (outbox instance_id V58 drift) + Bug B (filter async/error dispatch 401) — đã verify PASS trong Bước 1 + Bước 5.
