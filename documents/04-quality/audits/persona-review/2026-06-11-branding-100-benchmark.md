# External-Benchmark Audit — AI Branding Onboarding Wizard (wave `branding-100`)

**Ngày:** 2026-06-11
**Loại:** External-benchmark audit (outside-in) — đối chiếu wizard AI branding KiteClass với best-in-class AI-branding / AI site-builder onboarding 2025-2026
**Phương pháp:** WebSearch/WebFetch 7 sản phẩm + 1 vòng best-practices UX onboarding; READ-ONLY code KiteClass
**Scope nội bộ tham chiếu:** `kiteclass/kiteclass-frontend/src/components/branding/wizard/` (6 bước), ADR-037, GAP-1134/1135/1136/1147/1212
**Thị trường mục tiêu:** Trung tâm/gia sư VN — low design literacy, Zalo-first, thường KHÔNG có logo/asset chuyên nghiệp

---

## 1. Baseline nội bộ — wizard KiteClass hiện tại

| Đặc điểm | Hiện trạng (đọc từ code) |
|---|---|
| Số bước | **6 bước** tuần tự: `welcome` (chọn segment) → `logo` (upload, skippable) → `audience` → `tone` → `template` → `preview`; rồi `submitting` → `done`/`error` |
| AI generate ở đâu | **Bước cuối (`preview`/step 6)** — sau khi đã đi hết 5 bước input. Regenerate có quota theo tier (FREE 3 / PRO 10 / PREMIUM 30 / ENT ∞) |
| Input tối thiểu | segment + audiences + tone + templateId (4 trường, FREE tier). Logo optional |
| Mode đơn-giản vs nâng-cao | Tier-gated fields: FREE 4 trường → ENTERPRISE 16 trường (`VISIBLE_FIELDS_BY_TIER`). KHÔNG có "simple vs advanced toggle" rõ ràng trong cùng tier |
| Escape ramp | ✅ `USE_DEFAULTS` ("Sử dụng mặc định") — fill defaults an toàn (`DEFAULT_BRAND_INPUTS`) rồi submit ngay (GAP-287). Điểm tốt. |
| Xử lý không có asset | Logo step skippable → defaults; nhưng KHÔNG tự generate/placeholder logo, KHÔNG extract từ URL/Zalo |
| FULL_AI path | ADR-037 amendment 2026-06-10 chốt 2-mode (Gemini free text/HTML + GPT 5.5 banner image), nhưng **GAP-1147: Step 7 mode selector chỉ set state, action generate FULL_AI CHƯA wired** |
| Preview UX | 1 preview + regenerate counter (`RegenerateCounter.tsx`) — KHÔNG show nhiều biến thể song song |

---

## 2. Bảng so sánh per-product (benchmark 2025-2026)

| Sản phẩm | Số bước + thứ tự | Input tối thiểu | AI generate ở đâu + preview UX | Simple vs Advanced | Không có asset (logo) | Mobile / conversion |
|---|---|---|---|---|---|---|
| **Looka** (logo+brand kit) | Style quiz → tên brand → industry + màu + symbol thích → **generate** → customize | Chỉ **tên công ty** | Generate sớm; show **hàng trăm logo** để chọn, click vào để đổi màu/font/layout | Quiz đơn giản trước; chỉnh chi tiết sau khi chọn | **AI tự tạo logo** — user không cần có sẵn | Beginner-friendly, guided từng bước |
| **Canva Brand Kit** | Builder **extract logo+màu+font từ URL/PDF** (AI) HOẶC setup tay → Brand Kit → AI gen on-brand | URL website HOẶC upload tay | AI reference brand templates để gen design on-brand (300+ template) | "Upload guidelines + để AI điền" vs "setup tay" | Extract từ website có sẵn, không bắt upload logo riêng | Tích hợp dashboard, mass-template |
| **Squarespace Blueprint AI** | **5 bước guided**: tên → **brand personality** (professional/trustworthy/approachable) → chọn section homepage → cấu trúc trang → màu+font palette | Tên business | Gen nội dung+ảnh+layout xuyên suốt; **~4 phút tới draft đầu**; collaborative (chọn ở mỗi bước, không phải bấm 1 nút) | Guided (không phải prompt thuần) = đã là "simple"; edit sâu sau | — | Free trên mọi plan; aha nhanh |
| **Durable** | **3 câu hỏi, 30 giây**: niche cụ thể → generate full site (copy + layout + testimonial mẫu) | Niche/loại hình ("residential cleaning service"...) | Generate **ngay tức thì**; edit drag-drop sau | 1 path siêu đơn giản | Tự gen toàn bộ content+layout | **Output-before-signup** (xem site rồi mới đăng ký); mobile-perfect |
| **Wix ADI** | Chatbot hỏi vài câu: business facts → audience → services → brand prefs (màu/logo/tone) → upload ảnh/social (optional) → follow-up hội thoại | Vài câu cơ bản | AI phân tích → dựng site; follow-up "đổi tone / thêm CTA / bỏ section" | Conversational, điều chỉnh bằng chat | Upload optional, connect social để lấy context | Conversational, low-friction |
| **10Web** | **1 prompt** → full WordPress site → chat chỉnh real-time | 1 dòng prompt | Generate từ prompt; chat fine-tune layout/content | 1 prompt = simplest; WP editor cho advanced | Tự gen | All-in-one, hosting kèm |
| **Framer AI** | **1 prompt** (có example prompts gợi ý) → hero/CTA/pricing minimalist | 1 prompt (chất lượng phụ thuộc prompt) | GPT-4o gen layout+copy; visual drag-drop refine | Design-first, cần chút kỹ năng design cho advanced | Tự gen | Design-quality cao |

---

## 3. Pattern chung — "industry norm" vs wizard KiteClass hiện tại

| Khía cạnh | Industry norm (đa số benchmark) | Wizard KiteClass hiện tại | Khoảng cách |
|---|---|---|---|
| **Số bước trước khi thấy kết quả** | 1 prompt (Durable/10Web/Framer) → tối đa 5 bước guided (Squarespace/Looka) | **6 bước** rồi mới generate | 🔴 Nhiều hơn norm; generate đặt ở cuối |
| **Thứ tự generate** | **Output-first**: gen sớm từ input tối thiểu, refine sau | Gen ở bước CUỐI sau khi thu hết input | 🔴 Ngược pattern thống trị |
| **Input tối thiểu** | **Tên + ngành/niche** là đủ để generate | 4 trường (segment+audience+tone+template) | 🟠 Nặng hơn norm |
| **Logo/asset** | KHÔNG BAO GIỜ block: tự generate (Looka) hoặc extract từ URL/social (Canva/Wix) | Skip được nhưng không generate/extract thay | 🟠 OK skip, nhưng thiếu "AI bù asset" |
| **Mode đơn giản** | 1 prompt / guided ngắn là DEFAULT; advanced edit SAU generation | Tier-gated fields, không có simple-mode toggle rõ; FULL_AI prompt path chưa wired (GAP-1147) | 🔴 Pattern dominant (prompt→full) đang hỏng |
| **Preview** | Show **nhiều biến thể** để pick (Looka 100s logo) | 1 preview + regenerate counter | 🟠 Thiếu gallery lựa chọn |
| **Defaults / skip** | "Smart defaults", luôn có Skip/X; "users bail on 5 questions" | ✅ Có `USE_DEFAULTS` | 🟢 Đã đúng — giữ |
| **Conversion** | Output-before-signup, free, generate-then-edit-inline | Submit-then-done (không edit inline) | 🟠 Thiếu edit-inline sau gen |
| **Mobile/aha** | aha < 5 phút (benchmark 60s); progressive disclosure | 6 bước trên mobile/Zalo = friction cho low-literacy | 🟠 Rủi ro drop-off |

---

## 4. Khuyến nghị cụ thể cho wizard KiteClass (mỗi mục cite nguồn benchmark)

> Ưu tiên giảm friction cho persona trung tâm/gia sư VN low-design-literacy, Zalo-first.

1. **Đảo sang "output-first": generate draft NGAY từ input tối thiểu (tên trung tâm + segment), refine sau** — thay vì đi hết 6 bước mới generate. Cite **Durable** (3 câu/30s), **Squarespace Blueprint** (~4 phút tới draft đầu), best-practice "aha < 5 phút / benchmark 60s value".

2. **Giảm bước BẮT BUỘC xuống ≤3-4 trước khi thấy kết quả; gom `audience` + `tone` thành 1 bước "brand personality"** (giống Squarespace: professional/trustworthy/approachable). Cite **Squarespace** 5-step guided + best-practice "one JTBD question beats five-question wizard — users bail on 5".

3. **Ưu tiên wire FULL_AI prompt path (GAP-1147) — đây là pattern THỐNG TRỊ ngành.** Cho path "1 prompt → full banner + copy" làm **default cho low-literacy**; wizard 6-bước thành "advanced/guided mode" tùy chọn. Cite **Durable / 10Web / Framer** (single-prompt → full output) — 3/7 benchmark dùng prompt-thuần làm entry chính.

4. **Logo KHÔNG bao giờ block — AI tự bù asset.** KiteClass nên auto-generate logo/placeholder HOẶC extract màu/logo từ URL/Zalo OA của trung tâm, thay vì chỉ skip→default. Cite **Looka** (AI tự tạo logo, user không cần có sẵn) + **Canva Brand Kit** (extract logo/màu/font từ URL/PDF). Liên quan GAP-1134 (portrait upload) — đặt mọi upload thành optional-sau-preview.

5. **Preview show NHIỀU biến thể song song để pick, không chỉ 1 + regenerate counter.** Cite **Looka** (hàng trăm logo để chọn) + best-practice giảm decision fatigue (chọn nhanh hơn là regenerate mù). Cân nhắc dùng quota regenerate (FREE 3...) để gen 2-3 biến thể/lần thay vì 1.

6. **Giữ `USE_DEFAULTS` (đã khớp best-practice "smart defaults") nhưng đổi từ submit-and-done sang generate-then-edit-inline.** Cite best-practice "smart defaults" + **Durable/Wix** (generate rồi drag-drop/chat chỉnh ngay). Cho sửa heroTitle/màu inline trên trang preview.

7. **Mobile/Zalo-first: simple-mode là default; progressive-disclose các trường advanced (PRO/PREMIUM/ENT 6-16 trường) SAU generation.** Cite best-practice progressive disclosure ("surface essentials first, layer complexity when ready") + mobile 60s-to-value. ENTERPRISE 16 trường không bao giờ nên hiện trước khi user thấy draft đầu.

8. **Output-before-signup / output-before-commit để tăng conversion.** Cho trung tâm thấy banner preview TRƯỚC khi yêu cầu commit bước tiếp. Cite **Durable** (xem site xong mới đăng ký) + **Squarespace** (free, không AI-surcharge → giảm rào tâm lý).

---

## 5. Nguồn (Sources)

- Looka — [how-it-works](https://looka.com/logo-maker/how-it-works/), [brand-kit](https://looka.com/brand-kit/), [beginner guide](https://mattrics.com/blog/looka/)
- Canva Brand Kit — [brand-kit-builder](https://www.canva.com/help/brand-kit-builder/), [create on-brand designs](https://www.canva.com/help/create-on-brand-designs/)
- Squarespace Blueprint AI — [AI website builder](https://www.squarespace.com/websites/ai-website-builder), [walkthrough](https://www.sparkplugin.com/blog/what-is-squarespace-blueprint-ai), [feisworld review](https://www.feisworld.com/blog/squarespace-blueprint-ai-builder-review)
- Durable — [ai-website-builder](https://durable.com/ai-website-builder), [how-to](https://www.makingthatwebsite.com/how-to-build-a-website-with-durable-ai-website-builder/)
- Wix ADI — [how to use Wix AI](https://www.websitebuilderexpert.com/website-builders/how-use-wix-ai/), [openai/wix](https://openai.com/index/wix/)
- 10Web — [ai-website-builder](https://10web.io/ai-website-builder/); Framer AI — [framer review 2025](https://skywork.ai/skypage/en/Framer-AI-Review-(2025)-From-Prompt-to-Published-Website-in-Minutes/1973800094116409344), [10web vs framer](https://www.fahimai.com/10web-vs-framer)
- UX best practices — [Userpilot onboarding UX](https://userpilot.com/blog/onboarding-ux-examples/), [UXPin progressive disclosure](https://www.uxpin.com/studio/blog/what-is-progressive-disclosure/), [Userpilot PLG 2026](https://userpilot.com/blog/user-onboarding/)
