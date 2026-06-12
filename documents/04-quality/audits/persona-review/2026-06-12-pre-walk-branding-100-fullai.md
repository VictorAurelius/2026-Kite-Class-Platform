---
audience: dev
created: 2026-06-12
type: pre-walk-persona-simulation
wave: branding-100
flow: AI Branding wizard (KH :3001) — TEMPLATE + FULL_AI → deploy → KC landing (:3000)
persona: Owner KC tenant (Cô Hà BASIC) + biến thể PREMIUM (FULL_AI)
---

# Pre-Walk Persona Simulation — Wave branding-100 FULL_AI walk

> **TL;DR:** Mô phỏng walk G1 trên local Docker stack cho wizard branding KH (`:3001`) cả 2 nhánh (TEMPLATE persona BASIC + FULL_AI persona PREMIUM) → approve → quality gate → outbox `branding.deployed` → KC-core áp theme → landing `:3000` đổi thật. Đọc theo `design-first-investigation-order.md` (design `ai-branding-generation-flow.md` + ADR-037 → gaps → docs → code). **1 P0 blocker xác định gần chắc chắn: iframe WYSIWYG `/preview` sẽ BỊ CSP `frame-ancestors` CHẶN** vì KC container chạy `NODE_ENV=production` → `isDev=false` → `localhost:3001` bị loại khỏi allowlist (config #2363 chưa từng chạy thật trong Docker). Fix-before-walk HIGH item này TRƯỚC khi mở browser.

Per `pre-walk-persona-simulation-mandate.md` §1 + `feature-ship-runtime-walk-mandate.md` §2. Đây là pre-walk simulation, KHÔNG sửa code — chỉ liệt kê failure modes + pre-walk check.

---

## 1. Flow tóm tắt (trạng thái sau merge #2356–#2364, Bucket D `feature/branding-100-bucket-d-wysiwyg-preview` ở HEAD worktree)

```
KH owner login (:3001, /api/auth/login subscription)
  → wizard /branding/wizard (7 bước: Welcome→Logo→Portrait→Audience→Tone→Template→Step6Preview)
  → Step6Preview (mount): createJob V1 (POST /api/v1/branding/jobs) → jobId + instanceId (= JWT tenant claim)
  → live preview iframe → kiteclass /preview (:3000) themed bằng query draft (GAP-1215)
  → useBannerPreview: POST /api/v1/branding/jobs/preview-banner (TEMPLATE mặc định; FULL_AI nếu PREMIUM)
  → approve 4 tài nguyên → Deploy → POST /jobs/{id}/approve
        → QualityScoreAggregator.aggregate ≥70? (GAP-1217)
        → mockProvisioningService.provisionAsync → COMPLETED → publishDeployed(job.instanceId, ...)
  → SSE deploy-stream (useDeployStream: GET /jobs/{id}/deploy-stream?token=<JWT>)
  → RabbitMQ branding.events / branding.deployed → KC-core BrandingDeployedEventConsumer
        → applyDeployedBranding(tenantUuid, primary, secondary, logo, version) + CacheEvict + V98 version
  → landing :3000 (tenant e8ff87e1 Sky Education) đổi THẬT
```

Hai nhánh resolve **server-side** (`BrandingJobV1Controller.previewBanner`): gate tier → image-gen khả dụng (flag + !mock-key) → quota → generate. FE-supplied `X-Subscription-Tier` **bị gateway strip** (anti-spoof, `default-filters: RemoveRequestHeader=X-Subscription-Tier`) + re-inject từ JWT `tier` claim → **tier eligibility = JWT claim, KHÔNG phải header FE gửi**.

---

## 2. Failure modes (xếp theo confidence)

| # | Conf | Where (bước + file:line) | Symptom dự kiến | Loại |
|---|------|---|---|---|
| 1 | 🔴 HIGH | **CSP iframe** — `kiteclass-frontend/next.config.js:80,105-106` + Dockerfile:33 `NODE_ENV=production` | iframe `/preview` BỊ CHẶN ("refused to frame" / blank). `isDev=false` → `khFrameAncestors` = `'self' kitehub.me *.kitehub.me` **KHÔNG có** `localhost:3001`. Wizard `:3001` nhúng iframe `:3000/preview` → browser CSP `frame-ancestors` violation. WYSIWYG preview = trống. | P0 blocker |
| 2 | 🔴 HIGH | **FULL_AI tier gate** — gateway `JwtAuthenticationGatewayFilter:236` + `BrandingJobV1Controller:204` | Persona PREMIUM login nhưng JWT `tier` claim ≠ PREMIUM → `GenerationMode.forTier(tier)!=FULL_AI` → `fallbackReason=TIER_NOT_ELIGIBLE` → FULL_AI KHÔNG bao giờ chạy dù key thật + flag on. FE `X-Subscription-Tier` header bị strip vô dụng. | P0 cho nhánh FULL_AI |
| 3 | 🔴 HIGH | **Cross-product identity** — `MockProvisioningService:150` `publishDeployed(job.getInstanceId(),...)` + KC `BrandingDeployedEventConsumer:76` | Deploy "thành công" nhưng landing KHÔNG đổi. Event `tenantId = job.instanceId` (= KH JWT tenant claim). Nếu ≠ KC `landing_pages.instance_id` (demo Sky = `e8ff87e1-69fc-4842-a263-7385c68b4ffb`) → consumer áp lên instance khác / không có landing → `changed=false`, landing :3000 không thay đổi. | P0 cho deploy verify |
| 4 | 🔴 HIGH | **FULL_AI mock-key** — `BrandingJobV1Controller:206,267` `isAiMockMode()` + `OpenAIClient:38` | `OPENAI_API_KEY` mặc định `sk-mock-key` → provider name chứa "mock" → `fallbackReason=NOT_AVAILABLE` (không trừ quota, toast "đang hoàn thiện"). FULL_AI nhánh không chạy nếu key thật chưa inject từ AWS SM. | P0 cho nhánh FULL_AI |
| 5 | 🟠 MED | **SSE auth contract drift** — FE `useDeployStream.ts:78-83` dùng `?token=<JWT>` vs arch doc/`SseQueryTokenAuthFilter.ts:42` `?access_token=` + mint `/sse-token` | SSE hoạt động qua đường gateway JWT-in-query (`JwtAuthenticationGatewayFilter:150` đọc `?token=`, whitelist deploy-stream `:303`). NHƯNG mint `/sse-token` + `SseQueryTokenAuthFilter` (`?access_token=`) là **dead path** — arch doc §2 mô tả đường KHÔNG dùng. Nếu JWT hết hạn giữa walk → SSE 401 → `STREAM_DISCONNECTED`, deploy-stream không complete dù deploy đã xong. | MED |
| 6 | 🟠 MED | **RabbitMQ boot-order** — chỉ KC `BrandingEventsConfig:50-57` declare queue+binding; branding chỉ declare exchange | Nếu branding publish `branding.deployed` TRƯỚC khi kiteclass-core boot xong (queue `branding.deployed.kiteclass.queue` chưa declare) → topic exchange không có queue bound → message **rớt im lặng** (no mandatory flag) → landing không đổi, không lỗi. Risk khi rebuild chỉ 1 service hoặc KC restart. | MED |
| 7 | 🟠 MED | **NEXT_PUBLIC_KITECLASS_URL absent** — `useLandingPreviewUrl.ts:22` + compose `kitehub-frontend` (không set) | Compose set `NEXT_PUBLIC_KITECLASS_URL_PATTERN` build-arg nhưng KHÔNG set `NEXT_PUBLIC_KITECLASS_URL` (hook đọc cái này). May mắn fallback `http://localhost:3000` đúng cho local → iframe src OK. NHƯNG nếu build kit kit lỡ inline giá trị khác → iframe trỏ sai origin → trống. Verify build bundle. | MED (fallback che) |
| 8 | 🟡 LOW | **FULL_AI timeout/expiry** — `BrandingJobV1Controller:286` `generateImage(...).block(Duration.ofSeconds(60))` + `OpenAIClient:117` `/images/generations` | Key thật: DALL-E call có thể >60s (slow/quota) → exception → `GENERATION_FAILED` → fallback TEMPLATE, không trừ quota. Hoặc image URL OpenAI hết hạn ~1-2h → preview banner 403 sau đó. CircuitBreaker ResilientAIClient fallback placeholder. | LOW |
| 9 | 🟡 LOW | **Quality gate "always-pass"** — `QualityScoreAggregator:70-103` placeholder | Composite tối thiểu = **70** (offset=0, logo null) → luôn `≥70 PASS`. Gate KHÔNG bao giờ chặn deploy (theater v0, GAP-226/227/228 chưa land). KHÔNG phải blocker — nhưng walk "quality gate" không test được nhánh fail trừ khi set `QUALITY_GATE_PASS_THRESHOLD` cao tay. | LOW (không chặn walk) |
| 10 | 🟡 LOW | **MinIO presigned host** — `BrandingJobV1Controller:239` `inlineImageDataUri` + banner heroImage query | Portrait/logo inline data-URI cho Playwright render (host-agnostic, GAP-1146b). NHƯNG banner WebP + heroImage truyền vào iframe qua `?heroImage=<presigned localhost:9100>` → browser load OK (host port), nhưng KC `/preview` CSP `img-src` (report-only, `devImg` thêm `:9100` chỉ khi isDev) — production-build KC `isDev=false` → `img-src` KHÔNG có `:9100`. Report-only → KHÔNG chặn, chỉ warn. Nếu flip enforce sau → ảnh banner trong preview mất. | LOW (report-only) |
| 11 | 🟡 LOW | **Regenerate mid-wizard 400/409** — `Step6Preview.tsx:581,593` | "Tạo lại" mid-wizard: thiếu `instanceId` → toast.info (handled); job QUEUED → 409 INVALID_JOB_STATE → toast.info (handled). KHÔNG crash. Chỉ gây hiểu nhầm "tạo lại không chạy". | LOW (đã handle) |
| 12 | 🟡 LOW | **Stale Docker image** — kiteclass-frontend / kitehub-branding | #2363 (Bucket D WYSIWYG) + #2362 (FULL_AI wire) vừa merge. Nếu container chưa rebuild → chạy code cũ (iframe srcDoc cũ / FULL_AI chưa wire) → walk test sai code. | LOW (process) |

---

## 3. Pre-walk checklist (chạy THEO THỨ TỰ trước khi mở browser)

```bash
# === A. Stale-image guard (per pre-walk-static-audit-bundle.md) — rebuild nếu stale ===
bash scripts/check-stale-images.sh 2>/dev/null || \
  docker images --format '{{.Repository}} {{.CreatedSince}}' | grep -E 'kiteclass-frontend|kitehub-branding|kiteclass-core'
# Nếu stale → rebuild: bash kitehub/scripts/rebuild.sh kiteclass-frontend kitehub-branding kiteclass-core

# === B. #1 CSP frame-ancestors (P0 BLOCKER) — kiểm tra container TRẢ localhost:3001 chưa ===
curl -sI 'http://localhost:3000/preview' | grep -i 'content-security-policy'
#   EXPECT (để walk PASS): frame-ancestors ... http://localhost:3001
#   THỰC TẾ dự kiến (FAIL):  frame-ancestors 'self' https://kitehub.me https://*.kitehub.me   ← THIẾU :3001
#   → vì NODE_ENV=production trong container → isDev=false (next.config.js:80)
#   FIX-BEFORE-WALK: thêm env-driven origin (vd ALLOW_PREVIEW_FRAME_ORIGIN=http://localhost:3001)
#                    vào khFrameAncestors thay vì gate bằng isDev, rồi rebuild kiteclass-frontend.

# === C. #4 FULL_AI key thật (P0 nhánh FULL_AI) — provider KHÔNG mock ===
docker exec kitehub-branding printenv OPENAI_API_KEY | head -c 8
#   EXPECT FULL_AI: sk-proj / sk-... (key thật từ AWS SM), KHÔNG phải sk-mock
docker exec kitehub-branding printenv BRANDING_FULL_AI_IMAGE_GEN_ENABLED
#   EXPECT: true

# === D. #2 JWT tier claim (P0 nhánh FULL_AI) — persona PREMIUM phải có tier=PREMIUM ===
#   Login owner rồi decode JWT payload (claim `tier`):
TOKEN=$(curl -s -XPOST http://localhost:9000/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner@sky-education.test","password":"DevOwner#2026"}' | python3 -c 'import sys,json;print(json.load(sys.stdin).get("accessToken",""))')
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | python3 -c 'import sys,json;d=json.load(sys.stdin);print("tier=",d.get("tier"),"tenantId=",d.get("tenantId") or d.get("instanceId"))'
#   EXPECT FULL_AI: tier= PREMIUM   ← nếu FREE/BASIC/None → FULL_AI luôn TIER_NOT_ELIGIBLE
#   → nếu cần PREMIUM: seed/upgrade subscription owner instance sang PREMIUM (kitehub-subscription)

# === E. #3 Cross-product identity (P0 deploy verify) — JWT tenantId == KC landing_pages.instance_id ===
#   Lấy tenantId từ bước D, so với KC landing tenant (compose NEXT_PUBLIC_TENANT_ID=e8ff87e1-...):
docker exec kite-postgres psql -U kite -d kiteclass -c \
  "SELECT instance_id, center_name, branding_version FROM landing_pages WHERE instance_id='<tenantId-từ-bước-D>';"
#   EXPECT: 1 row (instance khớp). Nếu 0 row → deploy sẽ áp lên instance không có landing → landing KHÔNG đổi.
#   Sky demo tenant = e8ff87e1-69fc-4842-a263-7385c68b4ffb (compose). owner@sky-education phải map về instance này.

# === F. #6 RabbitMQ topology — queue declared + bound ===
docker exec kite-rabbitmq rabbitmqctl list_queues name messages 2>/dev/null | grep branding.deployed
#   EXPECT: branding.deployed.kiteclass.queue  0   ← queue tồn tại (kiteclass-core đã boot + declare)
docker exec kite-rabbitmq rabbitmqctl list_bindings 2>/dev/null | grep 'branding.events.*branding.deployed'
#   EXPECT: 1 binding branding.events → queue (routing key branding.deployed)
#   → nếu thiếu: restart kiteclass-core TRƯỚC, rồi mới deploy (tránh message rớt boot-order #6)

# === G. #5 SSE path — gateway whitelist + JWT-in-query ===
grep -n 'deploy-stream' kitehub/kitehub-gateway/src/main/resources/application.yml
#   EXPECT: route kitehub-branding-deploy-stream PRECEDES kitehub-branding-v1 (more-specific wins) — confirmed code

# === H. #7 iframe origin trong bundle FE ===
curl -s http://localhost:3001/branding/wizard 2>/dev/null | grep -oE 'localhost:3000/preview' | head -1 || \
  echo "check bundle: NEXT_PUBLIC_KITECLASS_URL fallback localhost:3000 (useLandingPreviewUrl.ts:22)"
```

---

## 4. Khuyến nghị fix-before-walk (HIGH items)

1. **#1 CSP (P0):** `kiteclass-frontend/next.config.js` đang gate `localhost:3001` trong `khFrameAncestors` bằng `isDev` (`NODE_ENV !== 'production'`). Container chạy production → loại `:3001` → iframe chặn. **Fix:** dùng env-driven allowlist (vd `ALLOW_PREVIEW_FRAME_ORIGINS`) inject `http://localhost:3001` cho local Docker bất kể NODE_ENV, rồi rebuild `kiteclass-frontend`. Đây là root-fix (config #2363 chưa từng chạy thật trong Docker). Verify bước B PASS trước khi walk.
2. **#2 + #4 (P0 FULL_AI):** Trước nhánh FULL_AI walk: (a) inject `OPENAI_API_KEY` thật vào `kitehub-branding` (bước C), (b) đảm bảo persona PREMIUM JWT carry `tier=PREMIUM` (bước D). Thiếu một trong hai → FULL_AI luôn fallback TEMPLATE (NOT_AVAILABLE / TIER_NOT_ELIGIBLE), nhánh FULL_AI không test được.
3. **#3 (P0 deploy):** Confirm `owner@sky-education.test` instanceId == KC `landing_pages.instance_id` (Sky `e8ff87e1-...`, bước E). Nếu lệch → deploy chain "xanh" nhưng landing không đổi → false-pass.
4. **#6 (MED):** Restart `kiteclass-core` TRƯỚC deploy (đảm bảo queue declared) để tránh boot-order message-drop.

**Nhánh TEMPLATE persona BASIC** (Cô Hà): không cần OPENAI key / PREMIUM tier. Blocker còn lại = **#1 CSP iframe** (chung cả 2 nhánh) + **#3 cross-product identity** (deploy verify). Walk TEMPLATE được sau khi fix #1.

---

## 5. Tham chiếu

- Design: `documents/02-architecture/ai-branding-generation-flow.md` §1 gate chain + §2 sequence + §4 config keys
- ADR-037 (generation stack 2-mode), ai-branding-deploy-flow.md (SSE deploy)
- Rule: `ai-branding-guidelines.md` §2.4 (FULL_AI tier-gate) + §2.5 (input cap) + §5 (quality gate)
- Code đối chiếu: `BrandingJobV1Controller` (gate chain) · `Step6Preview.tsx` (iframe + approve + SSE) · `useLandingPreviewUrl.ts` (preview URL) · `useDeployStream.ts` (SSE `?token=`) · `MockProvisioningService` (publish) · `BrandingDeployedEventConsumer` (KC apply) · `next.config.js` (CSP frame both sides) · `JwtAuthenticationGatewayFilter` (tier inject + strip)
- Gaps liên quan: GAP-1213 (deploy thật) · GAP-1215 (WYSIWYG iframe) · GAP-1217 (quality gate) · GAP-1218 (quota trust) · GAP-1135/1147 (FULL_AI wire) · GAP-1021 (SSE auth) · GAP-1108 (done+link) · GAP-1137 (PREMIUM FULL_AI tier)
