---
audience: mixed
---

# Pre-walk persona simulation — Wave flow-fix-1 Phase-3 (14 product-bug fixes)

**Created:** 2026-06-16
**Scope:** 14 fix gaps (GAP-1435/1436/1437/1438/1439/1440/1441/1442/1443/1444/1445/1446/1447/1448) + 1 prod-only follow-up (GAP-1455)
**Per:** `.claude/rules/pre-walk-persona-simulation-mandate.md` §3
**Worktree code read:** `/home/nguyenvankiet/projects/kite-wt-phase3-inline` (branch `wave-flow-fix-1-phase3`)
**Personas:** KH owner (`:3001`), KC owner (`:3000`), anonymous (DSAR public form), platform admin (KH-9 `/admin`)

> **Mục đích:** surface failure modes TRƯỚC khi coordinator/user walk 14 fix trên local Docker stack. Phần lớn các fix đã PARTIAL "pending re-walk" — rủi ro lớn nhất KHÔNG phải logic code sai mà là **stack chưa rebuild / env mới chưa apply / seed thiếu** → walk thấy "fix không có hiệu lực" và misdiagnose là code bug.

---

## 0. TL;DR rebuild + env matrix (đọc TRƯỚC khi walk)

Mỗi fix chỉ có hiệu lực sau khi service tương ứng được rebuild + container recreate. Đây là nguồn lỗi #1 cho walk PARTIAL gaps.

| Service cần rebuild | Fix gaps phụ thuộc | Lý do |
|---|---|---|
| **kitehub-frontend** (`:3001`) | GAP-1438, 1440, 1441, 1442, 1443, 1444, 1445, 1435, 1436 | FE source (DataRightsForm, admin dashboard/revenue, DangerZone, TierSelector, DashboardLayout mount, beta-status.ts, onboarding-checklist, next.config CSP). Next.js production build → MUST `pnpm build` + image rebuild, không hot-reload. |
| **kitehub-subscription** (BE) | GAP-1439 | `SecurityConfig.java` dsar permitAll — Spring Security chain chỉ load lúc startup. |
| **kitehub-branding** (BE) | GAP-1437 | `LogoAnalysis.java` `@NotBlank` — validation annotation compiled-in. |
| **kiteclass-frontend** (`:3000`) | GAP-1446, 1447, 1448 | FE source (branding-version-history, wizard page, payroll empty-state). |
| **compose recreate (env)** | GAP-1444 | `INTERNAL_API_URL=http://kite-gateway:9000` env MỚI cho `kitehub-frontend` → cần `docker compose up -d --force-recreate kitehub-frontend` (env mới không apply nếu chỉ rebuild image mà không recreate container). |

**GAP-1455 = prod-only (AWS-restore-blocked) → SKIP local walk.** Chỉ verify `pm2-ecosystem.config.js` chứa `INTERNAL_API_URL` cho cả 2 FE (static check), không walk runtime.

---

## Failure modes (12)

### 1. FE fix không có hiệu lực vì container chưa rebuild (toàn bộ FE gaps)
- **(a) Where:** kitehub-frontend `:3001` + kiteclass-frontend `:3000` — mọi FE fix gap (9 KH + 3 KC). Persona: KH/KC owner.
- **(b) Symptom:** Walk thấy UI cũ (revenue vẫn "0đ", không có support button, TierSelector vẫn cho chọn FREE, branding không có version-history tab) → kết luận sai "fix không work". Thực ra image đang chạy là build cũ. Next.js production = static build, KHÔNG hot-reload từ source mount.
- **(c) Pre-walk check:**
  ```bash
  # Xác định image age vs source mtime
  docker images | grep -E "kitehub-frontend|kiteclass-frontend"
  # Rebuild đúng cách (per CLAUDE.md — dùng scripts, không docker trực tiếp)
  bash kitehub/scripts/rebuild.sh kitehub-frontend
  bash kitehub/scripts/rebuild.sh kiteclass-frontend
  # Verify mới: vào container check 1 string đã sửa, vd CSP
  docker exec kitehub-frontend sh -c "grep -rl 'upgrade-insecure-requests' .next 2>/dev/null | head" # KỲ VỌNG: rỗng (đã bỏ)
  ```

### 2. GAP-1444 beta-status SSR vẫn fallback vì env INTERNAL_API_URL chưa recreate container
- **(a) Where:** `kitehub-frontend/src/lib/api/beta-status.ts` `resolveBaseUrl()` server-side → `INTERNAL_API_URL` env. Page `(public)/beta-status` SSR. Persona: anonymous/owner xem `:3001/beta-status`.
- **(b) Symptom:** Trang vẫn hiện "Không tải được nội dung trạng thái BE" (fallback changelog cũ) dù code đã split server/browser URL. Nguyên nhân: env `INTERNAL_API_URL` mới thêm vào compose nhưng container chạy bằng env cũ (chỉ rebuild image không đủ — env inject lúc container create).
- **(c) Pre-walk check:**
  ```bash
  docker exec kitehub-frontend printenv INTERNAL_API_URL   # KỲ VỌNG: http://kite-gateway:9000 ; nếu rỗng → force-recreate
  docker compose -f kitehub/docker-compose.kitehub.yml up -d --force-recreate kitehub-frontend
  # Verify SSR reach gateway từ trong FE container:
  docker exec kitehub-frontend sh -c "wget -qO- http://kite-gateway:9000/api/v1/beta-status | head -c 200"  # KỲ VỌNG: JSON 200
  ```

### 3. GAP-1439 DSAR anonymous vẫn 401 vì kitehub-subscription chưa rebuild (SecurityConfig)
- **(a) Where:** `kitehub-subscription SecurityConfig.java:172` permitAll `POST /api/v1/dsar/request` + `GET /api/v1/dsar/*`. Persona: anonymous DSAR form.
- **(b) Symptom:** Submit DSAR form → 401 (vẫn default-deny). Spring Security filter chain build lúc startup; chạy jar cũ = vẫn `.authenticated()`.
- **(c) Pre-walk check:**
  ```bash
  # curl trực tiếp gateway (anonymous, no token)
  curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:9000/api/v1/dsar/request \
    -H "Content-Type: application/json" -d '{"requestType":"ACCESS","fullName":"Test","email":"t@t.vn","nationalIdLast4":"1234"}'
  # KỲ VỌNG: 201 (hoặc 400 validation) — KHÔNG 401. Nếu 401 → rebuild kitehub-subscription:
  bash kitehub/scripts/rebuild.sh kitehub-subscription
  ```

### 4. GAP-1438 DSAR browser fetch lỗi vì NEXT_PUBLIC_API_URL build-time sai host
- **(a) Where:** `DataRightsForm.tsx:127` `process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000'` — browser-side fetch. `.env.docker.example` set `NEXT_PUBLIC_API_URL=http://gateway:9000` (Docker DNS, KHÔNG browser-reachable). Persona: anonymous trên browser thật.
- **(b) Symptom:** Browser submit DSAR → `net::ERR_NAME_NOT_RESOLVED` (browser không resolve được `gateway`/`kite-gateway` DNS — đó là tên Docker network, không phải host). `NEXT_PUBLIC_*` inline lúc build → nếu build với `gateway:9000` thì browser hỏng. localhost fallback chỉ dùng khi env rỗng.
- **(c) Pre-walk check:**
  ```bash
  # NEXT_PUBLIC_API_URL phải là host-reachable (localhost:9000), KHÔNG phải docker DNS
  docker exec kitehub-frontend sh -c "grep -rho 'http://[a-z-]*:9000' .next 2>/dev/null | sort -u"
  # KỲ VỌNG browser bundle chứa http://localhost:9000 (không phải http://gateway:9000 / kite-gateway:9000)
  # Nếu sai → set build-arg NEXT_PUBLIC_API_URL=http://localhost:9000 khi rebuild
  ```

### 5. GAP-1440 admin dashboard vẫn NaN vì BE kitehub-admin trả shape khác fixture
- **(a) Where:** `use-admin.ts` `mapDashboardStats()` map BE nested `instancesByStatus.{ACTIVE,TRIAL,SUSPENDED}` + `mrr/arr` → FE flat. BE = `kitehub-admin AnalyticsService.getDashboardStats()`. Persona: platform admin `:3001/admin`.
- **(b) Symptom:** Dashboard vẫn NaN/blank ở vài KPI nếu BE thật trả field name khác fixture test (test mirror DTO nhưng BE runtime có thể có field optional null, hoặc `newSignupsLast30Days` không tồn tại → `newInstancesThisMonth` undefined). Mapper có `?? 0` nhưng chỉ cho field map đúng key.
- **(c) Pre-walk check:**
  ```bash
  # Login admin → lấy token → curl dashboard, so shape thật vs mapper expect
  curl -s http://localhost:9000/api/platform/admin/dashboard -H "Authorization: Bearer $ADMIN_JWT" | python3 -m json.tool
  # Verify keys: instancesByStatus.ACTIVE/TRIAL/SUSPENDED, mrr, arr, newSignupsLast30Days, totalInstances
  grep -n "instancesByStatus\|newSignupsLast30Days\|mrr\|arr" kitehub/kitehub-frontend/src/hooks/use-admin.ts
  ```

### 6. GAP-1441 revenue page lỗi/empty vì admin chưa có seed revenue data
- **(a) Where:** `(admin)/admin/revenue/page.tsx` `useAdminRevenue('MONTHLY', startDate, endDate)` → BE `/api/platform/admin/revenue`. Persona: platform admin.
- **(b) Symptom:** Page render empty-state ("không có dữ liệu") thay vì chart — nếu DB không có invoice/payment row trong current-month range. Walk kết luận sai "wire không work" khi thực ra là seed thiếu. Cũng có thể range startDate/endDate current-month không trùng seed data date.
- **(c) Pre-walk check:**
  ```bash
  curl -s "http://localhost:9000/api/platform/admin/revenue?period=MONTHLY&startDate=2026-06-01&endDate=2026-06-30" \
    -H "Authorization: Bearer $ADMIN_JWT" | python3 -m json.tool
  # KỲ VỌNG: totalRevenue>0, dailyRevenue[] non-empty. Nếu empty → seed payment/invoice rows hoặc test với range chứa seed.
  ```

### 7. GAP-1443 SupportMenu mount nhưng owner platform tenantless không thấy / hoặc lỗi cùng GAP-1445
- **(a) Where:** `DashboardLayout.tsx` mount `<OnboardingCoordinator>` (render banner + SupportMenu floating). Persona: KH owner đăng nhập `:3001`.
- **(b) Symptom:** (i) Nút support floating không xuất hiện nếu owner login không route qua `DashboardLayout` (vài route dùng layout khác). (ii) OnboardingCoordinator có thể trigger onboarding fetch → 403/400 cho owner tenantless (liên quan GAP-1445) → console error noise dù SupportMenu vẫn render. Xác nhận `support-menu-trigger` thật sự visible chứ không chỉ mounted.
- **(c) Pre-walk check:**
  ```bash
  grep -n "OnboardingCoordinator\|SupportMenu\|BetaDisclaimerBanner" kitehub/kitehub-frontend/src/components/layout/DashboardLayout.tsx
  # Walk: login owner → mọi authenticated page → tìm nút floating góc phải dưới.
  # Confirm POST /api/v1/feedback 201 khi submit (BE đã permitAll line 128-129 — verify subscription rebuild không cần vì đã có sẵn).
  ```

### 8. GAP-1445 onboarding 403/400 vẫn xuất hiện vì FE guard dựa getTenantIdFromToken nhưng token seed thiếu tenantId claim
- **(a) Where:** `OnboardingChecklist.tsx` + `OnboardingDashboardCTA.tsx` guard `getTenantIdFromToken()` null → skip fetch. BE `OnboardingProgressController:159-164` trả 403 `TENANT_CONTEXT_MISSING`. Persona: KH owner tenantless (`owner.test`).
- **(b) Symptom:** Nếu seed JWT cho owner CÓ tenantId claim (khác với "tenantless") → guard không skip → fetch chạy → có thể vẫn ổn. Nhưng nếu test persona là owner-WITH-tenant thì không reproduce được scenario gap. Ngược lại owner tenantless mà token có tenantId rác → 403 vẫn fire. Cần seed đúng persona tenantless để verify guard skip fetch (network tab KHÔNG có call onboarding-progress).
- **(c) Pre-walk check:**
  ```bash
  # Decode JWT của owner.test xem có tenantId claim không
  # (lấy token sau login, decode payload base64)
  grep -n "getTenantIdFromToken\|tenantId" kitehub/kitehub-frontend/src/components/onboarding/onboarding-checklist/OnboardingChecklist.tsx
  # Walk: login owner tenantless → DevTools Network → KỲ VỌNG: KHÔNG có request GET /api/v1/onboarding-progress (guard skip), KHÔNG có 403/400 đỏ.
  ```

### 9. GAP-1437 generate-theme vẫn 500 vì kitehub-branding chưa rebuild
- **(a) Where:** `kitehub-branding AIBrandingController generateTheme(@Valid LogoAnalysis)` — `@NotBlank` mới trên 4 field. Persona: KH owner KH-6 AI branding wizard (low-reach, malformed body).
- **(b) Symptom:** POST generate-theme với null body → vẫn 500 (NPE) thay vì 400 nếu branding service chạy jar cũ chưa có annotation. Đây là low-reach (FE luôn gửi đúng) → walk có thể bỏ qua, nhưng nếu test sad-path curl thì phải rebuild trước.
- **(c) Pre-walk check:**
  ```bash
  curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:9000/api/v1/branding/ai/generate-theme \
    -H "Authorization: Bearer $OWNER_JWT" -H "Content-Type: application/json" -d '{}'
  # KỲ VỌNG: 400 (validation). Nếu 500 → rebuild kitehub-branding.
  # Đường gateway route tới branding cần verify (KH-6 endpoint path chính xác).
  ```

### 10. GAP-1446 KC branding version-history empty/lỗi vì instanceId resolve sai hoặc chưa có version seed
- **(a) Where:** `kiteclass-frontend branding-version-history.tsx` lấy `instanceId` từ `auth-store.tenantId`; `useBrandingVersions(instanceId)` → BE `GET /api/v1/branding/{instanceId}/versions` (raw Spring Page, KHÔNG ApiResponse-wrapped). Persona: KC owner `:3000/settings` branding.
- **(b) Symptom:** (i) Card "Lịch sử phiên bản" render rỗng nếu instance chưa từng đổi branding (0 version row) → walk tưởng lỗi. (ii) Nếu `auth-store.tenantId` null (KC session shape khác) → query `enabled:!!instanceId` skip → card không hiện. (iii) Parsing lỗi nếu BE thật wrap ApiResponse khác với raw-Page giả định.
- **(c) Pre-walk check:**
  ```bash
  curl -s "http://localhost:9000/api/v1/branding/$INSTANCE_ID/versions?page=0&size=10" \
    -H "Authorization: Bearer $KC_OWNER_JWT" | python3 -m json.tool
  # KỲ VỌNG: {content:[...], totalElements:N} raw Page. Confirm có ≥1 version (đổi branding 1 lần để seed).
  grep -n "tenantId\|instanceId\|auth-store" kiteclass/kiteclass-frontend/src/components/settings/branding-version-history.tsx
  ```

### 11. GAP-1435/1436 TierSelector + DangerZone phụ thuộc instance subscription state seed
- **(a) Where:** `TierSelector.tsx` `isDowngradeToFreeForPaidOwner` (cần owner BASIC để FREE bị disabled+guidance); `DangerZone.tsx` `hasActiveSubscription = Boolean(instance?.subscriptionId)`. Persona: KH owner KH-5 billing/settings.
- **(b) Symptom:** Không reproduce được fix nếu seed sai tier: GAP-1435 cần owner ở tier **BASIC (paid)** để thấy FREE-disabled guidance; nếu seed owner FREE thì paid-tier vẫn selectable (đúng nhưng không test được fix path). GAP-1436 cần owner **KHÔNG có subscriptionId** (TRIAL) để thấy disabled "chưa có gói để hủy"; nếu seed có subscription thì thấy cancel card interactive (cũng đúng nhưng test path khác).
- **(c) Pre-walk check:**
  ```bash
  # Verify instance state của persona walk
  curl -s http://localhost:9000/api/v1/instances/me -H "Authorization: Bearer $OWNER_JWT" | python3 -m json.tool | grep -iE "tier|subscriptionId|status"
  # GAP-1435 walk persona: owner tier=BASIC ; GAP-1436 walk persona: owner subscriptionId=null (TRIAL)
  # Cần 2 persona/instance khác nhau HOẶC seed update để cover cả 2.
  ```

### 12. GAP-1447 KC wizard hand-off card + GAP-1448 payroll copy — cosmetic, verify rebuild KC FE only
- **(a) Where:** `kiteclass-frontend (dashboard)/branding/wizard/page.tsx` (bỏ auto-bounce, thêm hand-off card target=_blank); `(dashboard)/admin/payroll/page.tsx:221-226` (bỏ leak `PayrollService.calculate`). Persona: KC owner `:3000`.
- **(b) Symptom:** (i) Wizard: nếu KC FE chưa rebuild → vẫn auto-redirect bounce sang `:3001/login` dead-end. Sau fix phải thấy card + amber notice + link target=_blank (KHÔNG auto-jump). (ii) Payroll empty-state vẫn hiện "PayrollService.calculate" nếu chưa rebuild. Cả 2 cosmetic, không cần BE.
- **(c) Pre-walk check:**
  ```bash
  grep -n "window.location.assign\|target=\"_blank\"\|chưa có đăng nhập dùng chung" kiteclass/kiteclass-frontend/src/app/\(dashboard\)/branding/wizard/page.tsx
  grep -rn "PayrollService.calculate" kiteclass/kiteclass-frontend/src/  # KỲ VỌNG: rỗng (đã bỏ)
  # Walk: KC owner → /branding/wizard → KỲ VỌNG card amber, KHÔNG bounce. /admin/payroll empty → copy user-facing.
  ```

---

## Recommended pre-walk batch (sort by confidence × impact)

**HIGH confidence + HIGH impact — fix/verify TRƯỚC walk (làm sạch stack):**
- **#1 + #2 (rebuild + env recreate):** rebuild cả 4 service (kitehub-frontend, kitehub-subscription, kitehub-branding, kiteclass-frontend) + `--force-recreate kitehub-frontend` cho env INTERNAL_API_URL. Đây là điều kiện tiên quyết cho MỌI gap khác — không làm thì 80% walk sẽ "fix không hiệu lực" giả.
- **#4 (NEXT_PUBLIC_API_URL browser host):** verify browser bundle chứa `localhost:9000` không phải docker DNS — nếu sai, DSAR (GAP-1438) + mọi browser-side fetch hỏng `ERR_NAME_NOT_RESOLVED`. Đây là risk thật cao nhất ngoài rebuild.
- **#3 (DSAR 401 curl probe):** curl gateway xác nhận subscription rebuild đúng trước khi walk browser.

**MEDIUM — seed/persona check TRƯỚC walk (tránh false-negative):**
- **#6 (revenue seed):** seed payment/invoice trong current-month range, hoặc walk với range chứa seed.
- **#10 (KC branding version seed):** đổi branding 1 lần để có ≥1 version row trước khi walk rollback.
- **#11 (tier/subscription state):** chuẩn bị 2 persona — owner BASIC (GAP-1435) + owner TRIAL no-subscription (GAP-1436).
- **#8 (onboarding tenantless persona):** seed JWT đúng owner tenantless để verify guard skip fetch.

**LOW — defer to walk catch (verify in-walk):**
- **#5 (admin dashboard shape):** curl 1 lần check key, còn lại walk catch.
- **#7 (SupportMenu visible):** walk-time visual confirm.
- **#9 (generate-theme 500):** low-reach sad-path, curl sau rebuild branding.
- **#12 (KC wizard/payroll cosmetic):** grep + walk visual, KC FE rebuild đã cover.

**SKIP local walk:** GAP-1455 (prod-only, AWS-restore-blocked) — chỉ static-check `pm2-ecosystem.config.js` chứa `INTERNAL_API_URL` cho cả kitehub-frontend + kiteclass-frontend.

---

## Cross-cutting notes

- **KH vs KC boundary** (`kitehub-kiteclass-boundary.md`): KH gaps (1435-1445) walk trên `:3001`; KC gaps (1446-1448) walk trên `:3000`. DSAR (1438) là KH public `:3001`. Đừng walk KH gap trên `:3000`.
- **Design-first đã resolve 2 gap:** GAP-1445 (BE 403 đúng design, fix=FE guard) + GAP-1435 (BE reject downgrade→FREE đúng design, fix=FE guidance). Walk verify FE behavior, KHÔNG kỳ vọng BE đổi.
- **Gateway pass-through anonymous:** DSAR + consent + feedback đều dựa `JwtAuthenticationGatewayFilter` pass-through request không-Bearer (line 159-160). Walk anonymous KHÔNG gắn token; nếu lỡ gắn token invalid → 401 từ gateway (khác với 401 từ subscription).
