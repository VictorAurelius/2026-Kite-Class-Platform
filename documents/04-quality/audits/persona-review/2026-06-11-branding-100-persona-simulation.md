---
title: "Persona Simulation — AI Branding Wizard (wave candidate branding-100)"
date: 2026-06-11
audience: mixed
type: persona-simulation
wave: branding-100
scope: "kiteclass-frontend + kitehub-frontend branding wizard"
method: "3-persona step-by-step walkthrough trên code thật (read-only), đối chiếu ADR-037 + cụm gap"
status: draft
related_adr: ADR-037
related_gaps: [GAP-1021, GAP-1108, GAP-1134, GAP-1135, GAP-1147, GAP-1160, GAP-1212]
---

# Persona Simulation — AI Branding Wizard (branding-100)

> Read-only audit. Không sửa code, không commit. Cite `file:line` cho mọi nhận định.
> Thứ tự investigate: DESIGN (ADR-037 + kit) → GAPS → CODE (per `design-first-investigation-order.md`).

## 0. Bối cảnh — HAI wizard song song (finding nền)

Scope branding-100 chứa **2 surface khác hẳn nhau**, đây là vấn đề kiến trúc lớn nhất:

| Trục | KC wizard (`kiteclass-frontend`) | KH wizard (`kitehub-frontend`) |
|---|---|---|
| File gốc | `components/branding/wizard/BrandingWizard.tsx` + FSM `wizard-machine.ts` | `app/(customer)/branding/wizard/page.tsx` orchestrator + `wizard-shared.tsx` reducer |
| Bước | 6: Welcome(segment)→Logo→Audience→Tone→Template→Preview (`types.ts:129 ORDERED_STEPS`) | 7: Welcome(name+slug+org)→Logo→**Portrait**→Audience→Tone→Template→Preview (`page.tsx:6-14`) |
| Mode selector (STATIC/TEMPLATE/FULL_AI) | ❌ KHÔNG có | ✅ `GenerationModeSelector.tsx` (ở Step 7) |
| Portrait upload (GAP-1134) | ❌ KHÔNG có | ✅ `PortraitStep.tsx` |
| Banner live preview | ❌ iframe `about:blank` (`PreviewStep.tsx:51`) | ✅ `BannerLivePreview.tsx` + `Step6Preview.tsx` |
| Quality gate /100 | ❌ KHÔNG có | ✅ `QualityGateWidget.tsx` |
| Per-resource approve (§4.2) | ❌ submit = deploy thẳng | ✅ `ResourceToggle.tsx` + `approvedResources[]` |
| SSE deploy stream | ❌ "Đang gửi tới pipeline AI…" tĩnh (`BrandingWizard.tsx:35-40`) | ✅ `useDeployStream.ts` + `DeployingStep.tsx` |

→ KH wizard đã tiến hoá theo ADR-037 (Direction C); **KC wizard đứng yên ở bản input-collector cũ**. `GAP-1212` xác nhận kit `ai-branding-wizard-v2` outdated + thiếu design cho mọi bước ADR-037. Engineering đang build KH-side bằng judgment, không có 基本設計.

**Câu hỏi chiến lược (chưa có gap):** giữ 2 wizard hay hợp nhất 1? KC per-tenant rebrand vs KH provisioning chồng lấn ~80% bước. Đề xuất hợp nhất (xem §5).

---

## 1. P1 — Cô Hà (Solo Teacher, mobile, "landing đẹp trong 5 phút")

Persona: ít thời gian, không rành design, FREE tier, vào từ điện thoại. Giả định đi **KC wizard** (per-tenant, FREE).

| Bước | Trải nghiệm (code) | Phán xét |
|---|---|---|
| 1. Welcome | `WelcomeStep.tsx:16` "6 bước nhanh… chọn loại tổ chức". Phải chọn segment (`SegmentPicker`) mới `NEXT` enabled (`:12 canProceed`). **KHÔNG có nút "Dùng mặc định" ở bước này** — `UseDefaultsButton` chỉ xuất hiện từ LogoStep trở đi (`LogoStep.tsx:45`). | ⚠️ Cô Hà muốn "5 phút" nhưng bị buộc qua ≥2 bước trước khi thấy escape ramp. Escape ramp nên ở Welcome. |
| 2. Logo | `LogoStep.tsx:27` "bỏ qua bước này — **AI sẽ tạo logo mới**". Cô bỏ qua, kỳ vọng AI vẽ logo. | ❌ Over-promise: ADR-037 Phase 1 generation = MOCK (`AIBrandingProcessor` trả `logoUrl`, GAP-1135). "AI tạo logo" không có thật ở Phase 1 → kỳ vọng sai. |
| 3-5. Audience/Tone/Template | 3 bước chọn-thẻ liên tiếp. Mobile: lưới thẻ `max-w-3xl` (`BrandingWizard.tsx:24`). | 🟡 4 quyết định design cho người "không rành design" → mệt mỏi quyết định. `USE_DEFAULTS` có cứu (skip thẳng tới submit) nhưng nằm rải rác. |
| 6. Preview | `PreviewStep.tsx:45-53` "Xem trước trực tiếp" = **iframe `src="about:blank"` — TRỐNG**. Cô bấm "Triển khai" (`:74`) mà **chưa từng thấy landing trông thế nào**. | 🔴 ĐIỂM CHẾT: preview giả. Deploy mù. Vi phạm trực tiếp kỳ vọng "ra landing đẹp". |
| Sau deploy | `BrandingWizard.tsx:41-48` "✅ Đã gửi… nhận email khi DEPLOYED". Redirect `/branding`. | ❌ `GAP-1108`: `/branding` rỗng, không link landing, assets=0. Cô không biết landing ở đâu → dead-end niềm tin. |

**Kết cho cô Hà:** đi hết wizard, deploy mù, post-deploy không thấy thành phẩm → bỏ cuộc. "5 phút" KHẢ THI về số bước nhưng GÃY ở preview trống + post-deploy rỗng.

---

## 2. P2 — Chị Hằng (Center Owner, 200 HS, "duyệt trước khi áp")

Persona: cần brand chuyên nghiệp, muốn **review/approve trước khi apply**, có thể PRO/PREMIUM. Đi **KH wizard** (provisioning, có org-type/deploy).

| Bước | Trải nghiệm (code) | Phán xét |
|---|---|---|
| 1. Welcome | `WelcomeStep` KH: tên + slug + org-type cùng 1 bước (`page.tsx:8`, `wizard-shared.tsx:107-119`). Slug validate async (`useSlugAvailability.ts`). | 🟡 Bước 1 gánh 3 việc + chờ validate slug → nặng. Org-type quan trọng (drive portrait count `portraitCountHint :86`) nhưng trộn chung dễ rối. |
| 2. Logo | upload OR AI-generate fork (`page.tsx:9`). | ✅ OK. |
| 3. Portrait | `PortraitStep.tsx` upload 1..N (hint theo org-type, `portraitHint :54`). | ✅ Có (KH-only). Nhưng chị chưa biết vì sao cần chân dung (chưa thấy banner mẫu) → thứ tự "upload trước, hiểu lý do sau". |
| 4-6. Audience/Tone/Template | chọn thẻ + template grid. | ✅ OK. |
| 7. Preview | `Step6Preview.tsx`: banner live preview + `QualityGateWidget` + per-resource approve (`ResourceToggle` + `approvedResources[]`) + `GenerationModeSelector` + deploy. | ✅ Đúng nhu cầu "duyệt trước". NHƯNG: (a) mode selector TEMPLATE/FULL_AI xuất hiện **lần đầu ở bước cuối** (`GAP-1147`) — chị đã chọn template ở bước 6 rồi mới phát hiện có "AI cao cấp" → đổi ý phải lùi. (b) banner có thể **thiếu ảnh chân dung** vừa upload (`GAP-1160` — Playwright in-container không fetch presigned URL). |
| Deploy | `useDeployStream.ts` SSE "Tiến trình". | 🔴 `GAP-1021`: EventSource không gửi `Authorization` header → gateway 401 → stream không kết nối; `GAP-1105` ref: panel kẹt. Deploy có vẻ treo → chị mất tin. |
| Sau deploy | redirect `/branding` (`page.tsx:153`). | ❌ `GAP-1108` rỗng (dù PARTIAL 80% đã ship deploy-status card, runtime-walk pending). |

**Kết cho chị Hằng:** KH wizard ĐÁP ỨNG nhu cầu duyệt (preview + approve + quality gate) nhưng (1) mode selector đặt sai vị trí, (2) banner thiếu chân dung, (3) SSE deploy có thể treo. Review-before-apply OK về cấu trúc, gãy ở chi tiết render + auth.

---

## 3. P3 — User FULL_AI (không có asset gì, "AI làm hết kể cả banner + chân dung")

Persona: PREMIUM/ENTERPRISE, không logo/không ảnh, kỳ vọng AI sinh toàn bộ.

| Điểm kỳ vọng | Thực tế (code/gap) | Phán xét |
|---|---|---|
| Chọn FULL_AI ngay từ đầu | KC: **không có mode selector** (`types.ts` không có `GenerationMode`). KH: chỉ ở Step 7 (`GenerationModeSelector.tsx`). | 🔴 Không thể "khai báo ý định FULL_AI" sớm để wizard ẩn bước upload. Đi qua Logo/Portrait upload dù định để AI làm hết → thừa bước. |
| AI sinh banner | `GAP-1147` PARTIAL: action "Tạo bằng AI cao cấp (tốn 1 lượt)" đã ship (PR #2289) nhưng **render = TEMPLATE mock**; GPT-5.5 image-gen thật = `GAP-1135` (Phase 2). | 🔴 Bấm "AI cao cấp", trừ quota, nhưng ra banner template — kỳ vọng GÃY. |
| AI sinh chân dung | Không có path AI-gen portrait — `PortraitStep` chỉ **upload**; `GAP-1134` không đề cập AI sinh chân dung. ADR-037 banner = text+chân dung+icon, chân dung là asset chính. | 🔴 "AI làm hết kể cả chân dung" KHÔNG khả thi — không upload thì banner thiếu lớp giữa (fallback icon 📚, `GAP-1160` AC). |
| Hết quota FULL_AI | `GenerationModeSelector.tsx:60` `premiumQuotaExhausted` → disable + fallback TEMPLATE (`GAP-1147` AC3). | ✅ Có xử lý fallback + thông báo. Tốt. |
| FREE/BASIC chọn FULL_AI | gate server-side `GenerationMode.forTier` (`GAP-1147` Fix) — không bypass. | ✅ Đúng. |

**Kết cho FULL_AI user:** kỳ vọng "AI làm hết" KHÔNG được đáp ứng ở Phase 1 — banner FULL_AI = mock, không có AI-gen chân dung. Cần (a) đặt mode selector trước, (b) quản trị kỳ vọng rõ ("Phase 1: AI viết copy + render template; ảnh AI = sắp ra"), (c) wire GAP-1135.

---

## 4. Bảng tổng findings

| # | Bước | Vấn đề | Đề xuất | Sev | Gap cover? |
|---|---|---|---|---|---|
| F1 | KC Preview | iframe `about:blank` (`PreviewStep.tsx:51`) — preview TRỐNG, deploy mù | Render banner/landing thật (WebP) hoặc nhúng buildLandingPreviewHtml giống KH | **P0** | ❌ chưa (GAP-1135 lo render BE, không lo KC preview UI) → FILE MỚI |
| F2 | Toàn KC | KC wizard thiếu mode selector + portrait + banner preview + quality gate + per-resource approve + SSE (lệch ADR-037 vs KH) | Hợp nhất KC↔KH hoặc port các bước ADR-037 sang KC | **P1** | 🟡 GAP-1212 (design) — không cover code port |
| F3 | Welcome (cả 2) | Không có escape "Dùng mặc định" ở bước đầu; KH Welcome gánh 3 việc | Thêm "Tạo nhanh với mặc định" ở Welcome; tách org-type/slug | P2 | ❌ (GAP-287 chỉ defaults từ Logo) |
| F4 | Logo | Copy "AI sẽ tạo logo mới" (`LogoStep.tsx:27`) over-promise vs mock Phase 1 | Đổi copy "AI chọn logo mẫu phù hợp (ảnh AI: sắp ra)" | P2 | ❌ FILE MỚI |
| F5 | Mode selector | TEMPLATE/FULL_AI ở Step 7 (cuối) → chọn template xong mới biết có FULL_AI | Chuyển mode selector lên ĐẦU (sau Welcome) — drive branching | **P1** | 🟡 GAP-1147 (action) — không cover thứ tự |
| F6 | Preview/Banner | Banner thiếu ảnh chân dung (Playwright in-container không fetch presigned) | inline data-URI (đã ship PR #2289, pending G2 visual) | P2 | ✅ GAP-1160 PARTIAL |
| F7 | Deploy | SSE EventSource 401 (không gửi auth header) → stream treo | token-in-query/cookie auth cho SSE + gateway whitelist | **P1** | ✅ GAP-1021 OPEN |
| F8 | Post-deploy | `/branding` rỗng, không link landing, assets=0 | deploy-status card + frontendUrl link + success toast | **P1** | ✅ GAP-1108 PARTIAL 80% |
| F9 | FULL_AI | Bấm "AI cao cấp" → render TEMPLATE mock, không AI banner/chân dung thật | wire Gemini(copy)+Playwright(template) & GPT-5.5(FULL_AI) | **P1** | ✅ GAP-1135 OPEN + GAP-1147 PARTIAL |
| F10 | KC Preview | Nút "Tạo lại" (`PreviewStep.tsx:69 REGENERATE`) → `submitting` dù CHƯA generate gì (regenerate-before-generate vô nghĩa) | Tách "Generate (xem trước)" khỏi "Deploy"; regenerate chỉ sau khi đã có preview | P2 | ❌ FILE MỚI |
| F11 | Portrait | Không có path AI-gen chân dung; user FULL_AI không asset → banner thiếu lớp giữa | Quản trị kỳ vọng + (Phase 2) AI portrait gen hoặc stock-illustration fallback | P2 | 🟡 GAP-1134 (upload only) |
| F12 | Kit/design | Kit `ai-branding-wizard-v2` outdated, 0 screen cho bước ADR-037 | Refresh kit TRƯỚC khi build cụm AI-chain | P2 | ✅ GAP-1212 OPEN |

**Đếm theo severity:** P0 = 1 (F1) · P1 = 5 (F2,F5,F7,F8,F9) · P2 = 6 (F3,F4,F6,F10,F11,F12). Tổng 12.

---

## 5. Kết luận — danh sách bước ĐỀ XUẤT cho wizard redesign

**Hiện tại (KC, 6 bước):** Welcome(segment) → Logo → Audience → Tone → Template → Preview(trống).

**Đề xuất (unified, 7 bước + branching theo mode):**

| # | Bước đề xuất | Đổi gì so hiện tại |
|---|---|---|
| 1 | **Welcome + Mode** — org-type/segment + chọn STATIC/TEMPLATE/FULL_AI NGAY + nút "Tạo nhanh mặc định" | Gộp mode selector lên đầu (F5); thêm escape ramp (F3) |
| 2 | **Đối tượng & Tông** (gộp Audience+Tone) | Giảm số bước chọn-thẻ cho solo teacher; cả 2 đều phân loại nhanh |
| 3 | **Assets** — Logo + Portrait upload *(ẩn/giảm nếu FULL_AI)*; copy không over-promise | KC chưa có Portrait (F2); branch theo mode (F11); sửa copy (F4) |
| 4 | **Template** *(auto-suggest, có thể skip ở FULL_AI)* | giữ, nhưng đặt sau mode để filter đúng |
| 5 | **Generate & Live Preview** — banner WebP thật + quality gate /100 + per-resource approve + nút "Tạo lại" (chỉ sau khi đã có preview) | thay iframe `about:blank` (F1); tách generate khỏi deploy (F10); port quality gate + approve sang KC (F2) |
| 6 | **Deploy (SSE tiến trình thật)** — auth qua query-token | sửa SSE 401 (F7) |
| 7 | **Hoàn tất** — summary + link landing + "Xem trang của bạn" | sửa post-deploy rỗng (F8) |

Nguyên tắc xuyên suốt: **mode quyết định branching sớm**; **preview phải thật trước khi deploy**; **hợp nhất KC↔KH** để không maintain 2 FSM lệch nhau (per GAP-1212). Refresh kit (GAP-1212) làm design source TRƯỚC khi code cụm GAP-1021/1134/1135/1147/1160.
