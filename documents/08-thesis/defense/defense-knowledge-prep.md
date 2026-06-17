---
audience: dev
title: Tài liệu ôn tập kiến thức bảo vệ khóa luận — KiteHub/KiteClass
status: active
created: 2026-06-18
---

# Tài liệu ôn tập kiến thức bảo vệ — KiteHub/KiteClass

> Mục đích: cung cấp đầy đủ kiến thức nền cho **mọi câu nói trong văn nói bảo vệ** (`defense-speaker-script-20slide.md`), để khi hội đồng vặn thì trả lời vững. Mỗi mục gồm: **(1) khái niệm là gì → (2) hệ thống áp dụng thế nào → (3) câu hỏi hội đồng hay vặn + cách trả lời**. Các facts đã được kiểm chứng trực tiếp trên code 2026-06-18; chỗ **thesis nói khác code thật** được đánh dấu 🔴 — phần này quan trọng nhất, học thuộc để không nói sai trước hội đồng.

---

## Phần 0 — 3 nguyên tắc trả lời + cheat-sheet chênh lệch

### 3 nguyên tắc

1. **Phân biệt "kiến trúc hỗ trợ" vs "đã chạy thật".** Nhiều phần thiết kế đúng nhưng Phase 1 chạy ở dạng tối thiểu (mock/template/stub). Khi hỏi, trả lời theo công thức: *"Kiến trúc thiết kế để làm X; hiện tại em đã triển khai tới mức Y; phần Z là hướng phát triển sau."* — không khẳng định cái chưa chạy.
2. **Trung thực về ranh giới.** Báo cáo chỉ có 2 mốc thời gian: **hiện tại đạt được** và **phát triển sau**. Không nói "đã có" cho cái còn mock. Hội đồng đánh giá cao sinh viên biết rõ giới hạn sản phẩm của mình.
3. **Không bịa con số / nguồn.** Nếu không nhớ chính xác (version, ngày hiệu lực luật, số liệu), nói *"em xin phép kiểm tra lại con số chính xác"* thay vì đoán. Đoán sai = mất điểm nặng hơn nói "em chưa nhớ chính xác".

### Cheat-sheet: thesis/slide nói KHÁC code thật (HỌC THUỘC)

| # | Chủ đề | Văn nói/thesis | Sự thật code | Trả lời khi bị hỏi |
|---|---|---|---|---|
| 1 | JWT | Hình 2.4a ghi HS256 | Access token = **HS512** (HS256 chỉ cho 2FA challenge) | "Access token ký HS512; HS256 chỉ dùng cho token thử thách 2FA" |
| 2 | AI sinh logo | "tự động sinh logo/banner 30-60s" | Wizard "Triển khai" = **mock**; pipeline mặc định chạy **mock key**; chỉ sinh thật khi cấu hình key Gemini/OpenAI | Xem Phần 3 — KHÔNG hứa "demo AI thật" |
| 3 | Image model | thesis: Stable Diffusion XL + Replicate | Code: **Gemini** (text) + **OpenAI gpt-image-1** (banner trả phí) | "Stack đã chốt lại theo ADR-037: Gemini cho copy, gpt-image-1 cho ảnh" |
| 4 | Java | thesis ghi Java 21 | Code = **Java 17** | "Java 17" |
| 5 | RDS PostgreSQL | thesis ghi PG 16 | RDS engine = **15**; container kitehub 15, kiteclass 16 | "RDS chạy PostgreSQL 15" |
| 6 | EC2 | "2 máy t3.micro" | **3 instances**: t3.large + t3.medium + t3.small | "Hiện có 3 EC2; backend đã nâng t3.large do nhu cầu bộ nhớ" |
| 7 | Load balancer | "Cloudflare → ALB → EC2" | **ALB đã gỡ** (tiết kiệm chi phí); đường thật CF → EIP → nginx → backend | "Phase BETA dùng nginx reverse-proxy thay ALB để tối ưu chi phí Free Tier" |
| 8 | Máy trạng thái tenant | 5 trạng thái (có CANCELLED) | **6 trạng thái**: PENDING/TRIAL/ACTIVE/SUSPENDED/DELETED/PURGED (không có CANCELLED) | Xem Phần 4 |
| 9 | Cột tenant | "cột tenant_id" | Cột thật = **`instance_id`**; biến RLS = `app.current_tenant_id` | "Khái niệm tenant_id, hiện thực bằng cột instance_id" |
| 10 | RLS coverage | (ngụ ý mọi bảng) | **51/91 bảng (56%)** — bảng dùng chung/audit không cần | "RLS bật trên các bảng thuộc phạm vi tenant, 51/91 bảng" |
| 11 | Custom domain | (thiết kế đầy đủ) | Routing có, **DNS verify + SSL chưa wire**; chỉ subdomain chạy thật | "Subdomain đã chạy; tên miền riêng là hướng phát triển sau" |
| 12 | WCAG AA | "bắt buộc đạt chuẩn" | Enforce thật ở FE (`contrast.ts`); quality-gate backend = **placeholder** | Xem Phần 3 mục WCAG |
| 13 | Content safety | "bộ phân loại tự động" | **Stub keyword**, chưa phải ML classifier | "Hiện là tiền kiểm từ khóa; bộ phân loại ML là hướng phát triển sau" |

---

## Phần 1 — Khái niệm nền tảng

**SaaS (Software as a Service):** mô hình cung cấp phần mềm dạng dịch vụ qua internet — khách hàng dùng qua trình duyệt, không cài đặt/bảo trì hạ tầng. Đối lập với on-premise (khách tự cài trên máy chủ của họ).

**Multi-tenant (đa người thuê):** một bản phần mềm + hạ tầng phục vụ nhiều khách hàng (tenant), mỗi tenant thấy dữ liệu/giao diện như của riêng mình. Đối lập single-tenant (mỗi khách 1 bản riêng). Trong đề tài: **mỗi tenant = 1 trung tâm dạy thêm = 1 trường**.

**Tenant:** một đơn vị khách hàng độc lập (ở đây là 1 trung tâm). Mọi dữ liệu (học sinh, lớp, điểm) gắn với 1 tenant và cô lập với tenant khác.

**Microservices:** kiến trúc chia hệ thống thành nhiều dịch vụ nhỏ, độc lập triển khai/mở rộng, giao tiếp qua API/message. Đối lập monolith (1 khối lớn).

**API Gateway:** điểm vào duy nhất cho mọi request, đứng trước các microservice — lo xác thực, định tuyến, rate-limit. Trong đề tài là `kite-gateway` (Spring Cloud Gateway, cổng 9000) — cũng là **biên tin cậy duy nhất** (chỉ gateway xác thực JWT, service phía sau tin header gateway gắn).

**Mô hình C4 (Simon Brown):** chuẩn vẽ kiến trúc phần mềm theo 4 mức phóng to dần — **Context** (L1: hệ thống + actor + hệ thống ngoài), **Container** (L2: các ứng dụng/dịch vụ/CSDL chạy độc lập), **Component** (L3: bên trong 1 container), **Code** (L4: lớp/class). Thesis dùng L1 (Hình 2.1) + L2 (Hình 2.2).

**Adapter pattern:** mẫu thiết kế bọc hệ thống ngoài sau 1 interface, để thay nhà cung cấp mà không sửa lõi. Đề tài có `NotificationChannel` (email) + `PaymentProcessor` (thanh toán) — đổi nhà cung cấp email/thanh toán chỉ cần thêm 1 implementation.

> **Q: Tại sao chọn microservices mà không monolith cho 1 đồ án?**
> A: Để cô lập trách nhiệm (lifecycle SaaS tách khỏi nghiệp vụ giáo dục), cho phép mở rộng/triển khai độc lập từng dịch vụ, và thể hiện được thiết kế kiến trúc — vốn là trọng tâm học thuật của đề tài. Đánh đổi là độ phức tạp vận hành cao hơn, được kiểm soát bằng Docker + gateway tập trung.

---

## Phần 2 — Cô lập multi-tenant (cụm Q&A nặng nhất)

### 2.1 Sáu mô hình cô lập + lý do chọn

Đã đánh giá **6 pattern** trên các trục (độ cô lập / chi phí vận hành / truy vấn liên-tenant / phù hợp Phase / tuân thủ / chi phí migration):

| Ký hiệu | Mô hình | Quyết định |
|---|---|---|
| P1 | Mỗi tenant 1 database riêng (1 RDS/tenant) | Loại — quá đắt |
| P2 | Mỗi tenant 1 schema riêng, chung DB | Hoãn/loại |
| P3 | Chung DB + cột `tenant_id`, **không** RLS | Loại — rò rỉ ngầm khi quên điều kiện lọc |
| **P4** | **Chung DB + cột định danh tenant + RLS PostgreSQL** | **✅ CHỌN** |
| P5 | Lai (Pool mặc định + Silo DB riêng cho enterprise) | Hoãn (phát triển sau) |
| P6 | Serverless (Aurora Serverless v2 / DynamoDB) | Loại |

**2 lý do chọn P4:**
- **Chi phí:** ~15 USD/tháng (db.t3.micro Free Tier) so với ~295 USD/tháng cho mô hình mỗi tenant 1 DB ở quy mô 10 tenant — chênh **~20 lần**.
- **An toàn (quan trọng hơn):** RLS enforce ở tầng database — một lỗi lập trình quên điều kiện lọc `WHERE` ở tầng ứng dụng vẫn bị database chặn, không gây rò rỉ chéo.

**Ánh xạ AWS SaaS Lens:** P4 = mô hình **"Pool"**; **"Silo"** = mỗi tenant tài nguyên riêng (P1); **"Bridge"** = schema riêng (P2). (AWS SaaS Lens là tài liệu hướng dẫn kiến trúc SaaS chính thức của AWS — trích trong thesis.)

### 2.2 RLS (Row-Level Security) hoạt động thế nào

**RLS là gì:** cơ chế của PostgreSQL cho phép định nghĩa **policy** quyết định mỗi dòng (row) có hiển thị với phiên truy vấn hiện tại hay không — lọc tự động ngay tại database engine, không phụ thuộc câu lệnh ứng dụng.

**Cơ chế thật trong đề tài:**
- Cột định danh tenant tên thật là **`instance_id`** (UUID), không phải `tenant_id` (tenant_id chỉ là tên khái niệm).
- Biến ngữ cảnh (GUC — Grand Unified Configuration variable) tên **`app.current_tenant_id`**, đặt bằng `set_config('app.current_tenant_id', :id, true)` — tham số `true` nghĩa là **SET LOCAL**, chỉ có hiệu lực trong **phạm vi 1 giao dịch** rồi tự xóa. Đặt tại mỗi ranh giới `@Transactional` qua AOP interceptor.
- Policy (đơn giản hóa, migration `V59`):
  ```sql
  CREATE POLICY tenant_isolation ON <bảng>
    USING (
      COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
      OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );
  ```
  Áp cho ~53 bảng thuộc phạm vi tenant, kèm `FORCE ROW LEVEL SECURITY` (để cả tài khoản owner của bảng cũng bị lọc).
- **Fail-closed on NULL (buộc thất bại khi rỗng — migration V59):** nếu biến `app.current_tenant_id` chưa được đặt → `current_setting(..., true)` trả `NULL` → so sánh `instance_id = NULL` cho kết quả NULL (không phải TRUE) → trả về **0 dòng** thay vì vô tình trả tất cả. Nhờ vậy lỗi quên-đặt-tenant lộ ngay trong kiểm thử thay vì âm thầm rò rỉ.
- **Defense bổ sung (nên biết):** ngoài RLS, code còn bật thêm **Hibernate `@Filter`** ở tầng ORM — cô lập 2 tầng (database + ORM).

> **Q: RLS có làm chậm truy vấn không?**
> A: Policy chỉ thêm một điều kiện so sánh trên cột đã đánh index (`instance_id`), chi phí không đáng kể so với lợi ích an toàn. Đây là cơ chế gốc của PostgreSQL, được tối ưu sẵn.
> **Q: Nếu lập trình viên quên đặt ngữ cảnh tenant?**
> A: Chính sách fail-closed sẽ trả 0 dòng (không rò rỉ), và lỗi lộ ngay khi test. Đây là lý do chọn fail-closed thay vì fail-open.
> **Q: Có nhánh admin-bypass — vậy admin xem được mọi tenant, có rủi ro không?**
> A: Nhánh `app.is_platform_admin` chỉ bật cho thao tác quản trị nền tảng hợp lệ, và mọi truy cập admin đều ghi audit log bất biến. Đây là ngoại lệ có kiểm soát, không phải lỗ hổng.

### 2.3 Phòng thủ chiều sâu (Defense-in-depth) — 5 lớp

Nguyên tắc: không dựa vào 1 cơ chế; kẻ tấn công phải xuyên thủng cả 5 lớp độc lập mới rò rỉ được dữ liệu chéo.

| Lớp | Vị trí | Cơ chế | Hỏng thì |
|---|---|---|---|
| 1 | Gateway | Xác thực chữ ký JWT + rút claim định danh tenant | JWT sai → 401 |
| 2 | Service | Spring Security `@PreAuthorize` kiểm vai trò | Thiếu quyền → 403 |
| 3 | Kết nối DB | `SET LOCAL app.current_tenant_id` mỗi giao dịch | (nền cho lớp 4) |
| 4 | PostgreSQL | RLS policy `USING` + `WITH CHECK` mỗi bảng | Sai/NULL tenant → 0 dòng |
| 5 | Schema | Ràng buộc cột `instance_id` UUID `NOT NULL` | Chặn dữ liệu không gắn tenant |

(Code còn 1 tầng phụ là Hibernate `@Filter` — nêu nếu muốn ghi điểm chiều sâu.)

### 2.4 JWT (JSON Web Token)

**JWT là gì:** chuỗi token đã ký số, gồm 3 phần (header.payload.signature), mang thông tin định danh người dùng. Server xác minh chữ ký để tin payload mà không cần truy DB mỗi request.

**Trong đề tài:**
- **Thuật toán: HS512** (HMAC-SHA512) cho access token — gateway bắt buộc secret ≥64 byte (512 bit). 🔴 *Thesis Hình 2.4a ghi HS256 là chưa chính xác; HS256 chỉ dùng cho token thử thách 2FA.*
- **Claims:** `sub` (userId), `email`, `role`, `type=access`, `tenantId` (chỉ set cho vai trò gắn tenant), `tier` (FREE/BASIC/PREMIUM/ENTERPRISE).
- **Verify tại gateway** (`JwtAuthenticationGatewayFilter`) — service phía sau **không tự parse JWT**, chỉ tin các header gateway gắn (`X-User-Id`, `X-User-Roles`, `X-Subscription-Tier`…).
- **Quản lý secret:** lưu AWS Secrets Manager, hỗ trợ **dual-key rotation** (current key để ký + previous key để verify trong cửa sổ xoay vòng), xoay định kỳ hàng quý.

> **Q: JWT bị lộ thì sao?** A: Token có thời hạn ngắn (access token); có refresh-token rotation (token cũ bị thu hồi khi dùng lại). Bí mật ký lưu Secrets Manager + xoay định kỳ.

### 2.5 Chuỗi định tuyến tenant + chống giả mạo (slide 9)

**4 bước phân giải tenant tại gateway** (`TenantResolverGatewayFilterFactory`), theo thứ tự ưu tiên:
1. Header nội bộ `X-Instance-Subdomain` (môi trường phát triển).
2. **So khớp hậu tố subdomain** với tên miền gốc (`.kitehub.me`) → `findBySubdomain(slug)`.
3. Tra cứu **tên miền riêng** → `findByCustomDomain(host)`.
4. **Claim `tenantId` trong JWT** (dự phòng).

Sau khi xác định: kiểm trạng thái phải `ACTIVE`/`TRIAL` (khác → **503**), rồi gắn `X-Tenant-Id` (UUID do gateway phát hành).

**Chống giả mạo (đã triển khai — KHÔNG còn là gap):** gateway **luôn loại bỏ** header `X-Tenant-Id` (và `X-User-Id`, `X-Subscription-Tier`…) do client gửi lên (`RemoveRequestHeader`), rồi gắn lại từ JWT đã xác minh (`TenantHeaderGuardFilter`). Nhờ vậy không ai giả mạo định danh tenant để truy cập dữ liệu trung tâm khác.

> 🔴 **Lưu ý:** 2 tài liệu kiến trúc (`multi-tenant-architecture.md`, `tenant-domain-landing-architecture.md`) còn ghi chống-giả-mạo này là "OPEN/chưa làm" (GAP-814) — đó là **doc cũ chưa cập nhật**; code đã đóng. Thesis §2.2.6 thì đúng. Nếu hội đồng đọc doc cũ và hỏi, trả lời: "phần đó đã được triển khai, tài liệu kiến trúc nội bộ chưa kịp cập nhật".

> **Q: Demo cô lập dữ liệu thế nào?** A: Đăng nhập 2 tài khoản thuộc 2 tenant khác nhau; mỗi tài khoản chỉ thấy dữ liệu của tenant mình. Hoặc dùng token tenant A gọi tài nguyên tenant B → trả 403/0 dòng (đã kiểm thử cross-tenant).
> **Q: Custom domain demo được không?** A: Subdomain `{slug}.kitehub.me` đã chạy thật; tên miền riêng (vd `skyedu.vn`) đã có phần định tuyến nhưng cấp SSL/xác minh DNS là **hướng phát triển sau** — chưa demo được.

---

## Phần 3 — AI Branding (cụm dễ bị vặn nhất)

### 3.1 AI Branding làm gì

Owner điền form ngắn (tên trung tâm / lĩnh vực / phong cách / màu) → hệ thống dựng **theme + logo + ảnh nền (hero) + banner mạng xã hội**. UI là **wizard 6 bước** (chào → upload logo → chọn đối tượng → chọn tông → chọn template trong 6 mẫu → xem trước + duyệt từng tài nguyên). Bắt buộc **xem trước trước khi triển khai**; đếm số lần tạo lại theo gói (FREE 3 / BASIC 10 / PREMIUM 30 / ENTERPRISE không giới hạn).

### 3.2 Ba chế độ tạo (3 modes)

| Chế độ | Nghĩa | Chi phí |
|---|---|---|
| **STATIC** | Dùng ảnh user upload / mặc định hệ thống, không tính toán | 0 |
| **TEMPLATE** | Sinh từ template SVG/HTML + tham số thương hiệu (gần như mọi request) | ~0 |
| **FULL_AI** | Gọi mô hình AI sinh thật (nặng, bất đồng bộ, có phí) | Cao |

Theo **ADR-037**: TEMPLATE = mặc định **mọi gói**; **FULL_AI = chỉ PREMIUM/ENTERPRISE** (gói trả phí); FREE/BASIC chỉ TEMPLATE. Gói (tier) được truyền xuyên service qua claim trong JWT → gateway gắn header `X-Subscription-Tier` (ADR-039), chống client tự khai gói.

### 3.3 🔴 TRẠNG THÁI THẬT Phase 1 — học kỹ phần này

Có **2 luồng tách biệt**:

1. **Bước "Triển khai" của wizard (Step 6 duyệt)** = **mock hoàn toàn** (GAP-1021). Code gọi `MockProvisioningService` — chỉ giả lập tiến trình (delay), **không dựng hạ tầng riêng cho tenant**; URL trả về chỉ là placeholder.
2. **Pipeline sinh nội dung** (qua RabbitMQ → `AIBrandingProcessor`) = **đã wire AI thật**, nhưng:
   - Có **fallback mock** khi không có API key.
   - **Cấu hình mặc định = mock** (key mặc định là `sk-mock-key-for-local-testing`, key Gemini rỗng) → out-of-the-box chạy mock.
   - Banner TEMPLATE hiện rasterise qua `StubBannerRenderer` (placeholder) — Playwright chưa wire.
   - **Chỉ sinh thật khi cấu hình key thật.**

**Mô hình AI thật (khi có key):** **Gemini** (`gemini-flash-latest`) cho văn bản/copy; **OpenAI `gpt-image-1`** cho ảnh banner FULL_AI (gói trả phí). 🔴 *Thesis ghi Stable Diffusion XL + Replicate là phần khảo sát cũ — ADR-037 đã thay; code không có SDXL/Replicate/MiniMax.*

> **Q (BẪY LỚN): Demo cho hội đồng xem AI sinh logo thật được không?**
> A AN TOÀN: *"Kiến trúc hỗ trợ 3 chế độ STATIC/TEMPLATE/FULL_AI; Phase BETA hiện chạy chế độ TEMPLATE (chi phí gần bằng 0) là chủ đạo, FULL_AI bằng gọi mô hình thật dành cho gói trả phí. Trong môi trường demo em đang chạy chế độ template/mock để kiểm soát chi phí; phần sinh ảnh bằng mô hình thật cần cấu hình API key và là hướng hoàn thiện tiếp."*
> ❌ **TUYỆT ĐỐI KHÔNG** nói "đây là logo do AI sinh real-time" nếu đang chạy mock/template — sẽ bị bắt ngay.
> **Q: Vì sao template-first?** A: ~80% nhu cầu xử lý được bằng template chất lượng cao với chi phí gần 0; chỉ gọi AI thật khi thực sự cần (gói cao) → kiểm soát chi phí, đúng tinh thần SaaS Free Tier.

### 3.4 WCAG AA

**WCAG (Web Content Accessibility Guidelines):** bộ tiêu chuẩn quốc tế về khả năng tiếp cận nội dung web, 3 mức **A** (tối thiểu) < **AA** (phổ biến, thường là yêu cầu pháp lý/doanh nghiệp) < **AAA** (cao nhất). Tiêu chí tương phản (contrast) của **AA**: chữ thường ≥ **4.5:1**, chữ lớn/thành phần UI ≥ **3:1** (AAA yêu cầu 7:1).

**Trong đề tài:** enforce thật ở frontend KiteClass — `contrast.ts` cài đặt đầy đủ thuật toán WCAG (tính relative luminance theo sRGB, tỉ lệ tương phản, hàm `ensureContrast` tự điều chỉnh độ sáng để đạt 4.5:1, `buildThemeStyleCss` chèn biến CSS an toàn SSR). Bảng màu backend cũng được thiết kế đạt tương phản cao.

> 🔴 **Lưu ý:** quality-gate **backend** (`QualityScoreAggregator`) tính điểm "contrast" bằng giá trị **placeholder** (không đo thật) — đo WCAG thật là hướng phát triển sau (GAP-226). Nếu hỏi sâu: *"Việc đảm bảo tương phản được thực thi ở tầng giao diện qua thuật toán WCAG; cổng chấm điểm tự động phía backend hiện ở mức khung, đo lường WCAG đầy đủ là phần hoàn thiện tiếp."*

### 3.5 Bộ phân loại an toàn nội dung

**Hiện trạng:** có service `ContentModerationService` (pipeline 3 stage), nhưng **Stage 1 là tiền-kiểm bằng danh sách từ khóa (stub)**, chưa phải mô hình ML. Mọi lần kiểm đều ghi 1 dòng audit log thật. Bộ phân loại ML là hướng phát triển sau (GAP-228).

> Trả lời an toàn: *"Hiện tại là tiền kiểm bằng từ khóa và ghi nhật ký kiểm duyệt; bộ phân loại học máy là hướng phát triển sau."*

---

## Phần 4 — Vòng đời tenant (máy trạng thái)

**6 trạng thái thật** (`InstanceStatus` enum): `PENDING` → `TRIAL` → `ACTIVE` → `SUSPENDED` → `DELETED` → `PURGED`.
🔴 *Thesis Hình 2.8 vẽ 5 trạng thái và có `CANCELLED`; code KHÔNG có `CANCELLED` mà dùng `DELETED` (xóa mềm) → `PURGED` (xóa vĩnh viễn).*

**Luồng + mốc thời gian (đã xác minh):**
- Người dùng tiềm năng gửi yêu cầu beta → `PENDING` (chờ quản trị duyệt).
- Quản trị duyệt + gửi **liên kết kích hoạt (magic-link)** → `TRIAL` (**14 ngày**).
- Thanh toán thành công → `ACTIVE`. Thanh toán thất bại quá **grace 3 ngày** → `SUSPENDED` (giữ data, retention theo gói; cron purge dọn sau ~30 ngày).
- Trial hết hạn (cron 8h sáng hàng ngày) → tự chuyển `SUSPENDED`.

**TTL token (phân biệt khi bị hỏi):** token mời nhân viên = **7 ngày**; token mời beta-access = **24 giờ**.

**Sự kiện async dựng branding mặc định:** khi duyệt, hệ thống phát sự kiện để `kitehub-branding` dựng template mặc định **song song** với gửi email → rút ngắn chờ khi user đăng nhập lần đầu. 🔴 *Hiện cơ chế chạy bằng poller (quét mỗi ~2 giây); chuyển sang exchange fanout đầy đủ là phần hoàn thiện tiếp — nói "song song bất đồng bộ" là đúng tinh thần.*

> **Q: Vì sao giữ data 7 ngày khi suspend?** A: Cho khách hàng cơ hội gia hạn mà không mất dữ liệu; sau thời hạn mới xóa — cân bằng giữa trải nghiệm và chi phí lưu trữ.

---

## Phần 5 — Triển khai + CI/CD + giám sát

### 5.1 C4 Level 2 — 4 cụm, 17 thành phần (slide 12)

- **Cụm giao diện (2):** `kitehub-frontend` (:3001, marketing + quản trị), `kiteclass-frontend` (:3000, nghiệp vụ giáo dục) — đều Next.js 15.
- **Cụm gateway (1):** `kite-gateway` (:9000, Spring Cloud Gateway).
- **Cụm dịch vụ (7):** 6 dịch vụ KiteHub (`kitehub-admin`, `kitehub-branding`, `kitehub-email`, `kitehub-gateway`, `kitehub-platform`, `kitehub-subscription`) + `kiteclass-core`. Lưu ý: `kitehub-platform` là **thư viện JAR dùng chung**, không deploy riêng.
- **Cụm hạ tầng (đếm 8 để ra 17):** PostgreSQL, Redis, RabbitMQ, MinIO… (prefix `kite-`).

> 🔴 **Mâu thuẫn nội bộ thesis:** narrative §2.4 đếm ra 17 (gồm 8 hạ tầng), nhưng Hình 2.2 (sơ đồ C4 L2) chỉ vẽ **4 container hạ tầng** + 4 service KiteHub → nếu hội đồng đếm theo hình sẽ ra ~12-14, lệch số 17. Nếu bị hỏi: *"Sơ đồ minh họa các thành phần tiêu biểu; tổng cộng đếm đầy đủ là 17 thành phần tách biệt."* (Cân nhắc thống nhất số liệu trước buổi bảo vệ.)

### 5.2 AWS deployment (slide 13)

- **Region:** `ap-southeast-1` (Singapore) — pin cứng.
- **VPC 2 tầng:** public subnet (×2 AZ) chứa máy chủ ứng dụng; private subnet (×2 AZ) chứa RDS (`publicly_accessible = false`, không có IP public).
- **RDS:** `db.t3.micro`, **PostgreSQL 15**, **single-AZ** (Phase BETA tiết kiệm chi phí). 🔴 *Thesis ghi PG 16 — sự thật là 15.*
- 🔴 **EC2:** thực tế **3 instances** — `kh_backend` (t3.large, đã nâng từ t3.micro do thiếu bộ nhớ), `kc_app` (t3.medium), `kc_app_fe` (t3.small, chạy nginx + frontend). *Thesis nói "2 máy t3.micro".*
- 🔴 **ALB (load balancer):** Terraform **có** khai báo nhưng đã **tắt** (`enable_alb=false`, gỡ để tiết kiệm ~27 USD/tháng). Đường đi thật: **Cloudflare → Elastic IP (kc_app_fe) → nginx reverse-proxy → backend**. *Thesis vẽ ALB là điểm vào.* Trả lời: *"Phase BETA dùng nginx reverse-proxy thay ALB để tối ưu chi phí Free Tier; kiến trúc sẵn sàng bật ALB khi mở rộng."*

> **Q (BẪY): Tại sao đặt máy chủ ở Singapore mà không phải Việt Nam — có vi phạm Nghị định 53/2022 về lưu trữ dữ liệu trong nước không?**
> A: *"Phase BETA mời giới hạn (invite-only) chưa kích hoạt ngưỡng bắt buộc lưu trữ trong nước theo Nghị định 53 (áp dụng khi đạt quy mô lớn / có yêu cầu từ cơ quan chức năng). Hướng phát triển sau là chuyển sang AWS vùng Việt Nam (hoặc cloud nội địa như Viettel/VNG) trước khi vận hành chính thức quy mô lớn."* — KHÔNG nói "chấp nhận vi phạm".

### 5.3 CI/CD

- **Ảnh Docker bất biến** gắn theo **mã commit (SHA)** — mỗi lần deploy dùng đúng 1 ảnh, truy vết được.
- **OIDC** (`id-token: write` → AWS STS AssumeRoleWithWebIdentity): sinh thông tin xác thực **tạm thời theo từng lần chạy**, thay khóa AWS tĩnh nhúng cứng → giảm rủi ro lộ khóa.
- **Cổng xác nhận thủ công** (chống deploy nhầm): triển khai hạ tầng phải gõ nguyên văn **`APPLY`** (`terraform-apply.yml`); deploy production phải gõ **`DEPLOY`** (`deploy-production.yml`) — nhập sai → workflow báo lỗi, dừng.
- **IAM thu hẹp:** role deploy chỉ có quyền cần thiết (push ECR + gửi lệnh SSM), không full-access.

> **Q: Làm sao tránh deploy nhầm lên production?** A: workflow chỉ chạy thủ công (workflow_dispatch), bắt gõ đúng từ khóa xác nhận (`APPLY`/`DEPLOY`), và dùng credential tạm OIDC có thời hạn ngắn.

### 5.4 Giám sát 3 lớp (slide 14)

1. **CloudTrail** — ghi mọi lệnh gọi API AWS (terraform, console, SDK). **Bật TRƯỚC khi tạo resource** → có đường cơ sở kiểm toán đầy đủ ngay từ đầu, không có khoảng mù. (Free tier: management events bản đầu = 0 USD.)
2. **CloudWatch** — log ứng dụng dạng **JSON có cấu trúc** + custom metric + cảnh báo (CPU >80%, RDS connections cao, EC2 status-check fail…).
3. **Prometheus + Grafana** (self-host) — scrape `/actuator/prometheus` (độ trễ outbox, request rate, JVM memory…), trực quan hóa Grafana.

> 🔴 Lưu ý: cảnh báo "ALB 5xx" hiện vô nghĩa vì ALB đã gỡ — nếu hội đồng soi alarm config, thừa nhận đây là cấu hình còn lại từ thiết kế ALB.

> **Q: Vì sao bật CloudTrail trước khi tạo hạ tầng quan trọng?** A: Để mọi thao tác tạo/sửa tài nguyên đều được ghi lại từ thời điểm đầu tiên — phục vụ điều tra sự cố bảo mật + sẵn sàng tuân thủ (PDPL/ISO 27001/SOC 2). Nếu bật sau, các thao tác khởi tạo ban đầu sẽ không có log.

---

## Phần 6 — Pháp lý + thị trường

### 6.1 Văn bản pháp luật (slide 5)

| Văn bản | Nội dung | Lưu ý khi trả lời |
|---|---|---|
| **Thông tư 29/2024/TT-BGDĐT** | Quản lý dạy thêm-học thêm; chính thức hóa dạy thêm có thu phí (hiệu lực ~14/02/2025) | Nguồn vững, là động lực thị trường + mở phân khúc giáo viên độc lập |
| **Nghị định 13/2023/NĐ-CP** | Bảo vệ dữ liệu cá nhân (PDPD, hiệu lực 01/07/2023) | 🔴 chương 1 dùng văn bản này, chương 2 lại trích **Luật 49/2023/QH15** (PDPL Luật, hiệu lực 01/07/2026) — **2 văn bản khác nhau**; thống nhất nói "Nghị định 13/2023" cho hiện tại |
| **Luật An ninh mạng 2018 (24/2018/QH14)** + Nghị định 53/2022 | An ninh mạng + lưu trữ dữ liệu trong nước | Lý do chốt vùng Singapore + hướng chuyển VN — xem Q bẫy Phần 5.2 |
| **Thông tư 78/2021/TT-BTC** | Hóa đơn điện tử | 🔴 chương 1 nói "đã có", chương 2 nói "lộ trình phát triển sau" (qua đối tác MISA MeInvoice) — **thống nhất: phát triển sau** |
| **Luật Trẻ em 2016** | Bảo vệ trẻ em (gồm môi trường mạng) | 🔴 K-12 ngoài phạm vi thesis hiện tại — nếu hỏi, nói "phần bảo vệ dữ liệu trẻ em là thiết kế cho hướng mở rộng K-12 sau này" |

### 6.2 Số liệu thị trường (slide 3,4)

| Số liệu | Nguồn | Độ vững |
|---|---|---|
| "Hơn 50.000 trung tâm" | Magenest 2024 | 🟡 yếu (báo cáo thương mại EdTech) |
| "15-20% thu nhập hộ gia đình" | 6Wresearch | 🟡 yếu (market-research thương mại) |
| "90% phụ huynh đô thị dùng Zalo" | VECITA, Báo cáo Kinh tế Số VN 2024 [tr.42] | 🟢 vững nhất (có trích dẫn trang) |

> **Q: Nguồn "50.000 trung tâm" / "15-20%" lấy từ đâu?** A: *"Đây là ước lượng từ các báo cáo thị trường công khai (Magenest, 6Wresearch); con số có thể dao động và em dùng để minh họa quy mô, không phải số liệu kiểm toán nhà nước. Số liệu vững nhất là tỉ lệ dùng Zalo từ báo cáo VECITA."* — trung thực về độ tin cậy.

---

## Phần 7 — Câu hỏi khó dự kiến (cross-cutting) + cách trả lời

1. **"Đóng góp mới / điểm khác biệt của đề tài là gì?"**
   → Ba điểm: (a) kiến trúc multi-tenant cô lập bằng RLS fail-closed (an toàn ở tầng DB, không phụ thuộc lập trình viên); (b) AI Branding tự động hóa nhận diện thương hiệu cho phân khúc trung tâm nhỏ chưa có thương hiệu số; (c) trải nghiệm + tuân thủ pháp luật bản địa hóa cho thị trường Việt. Không claim "phát minh", mà là **tích hợp + bản địa hóa** đúng bài toán.

2. **"AI thật hay mock?"** → Xem Phần 3.3 — kiến trúc 3 chế độ, Phase BETA chạy template chủ đạo, AI thật (Gemini + gpt-image-1) cho gói trả phí khi cấu hình key.

3. **"Đã kiểm thử bảo mật / pen-test chưa?"** → Có kiểm thử cô lập cross-tenant (token tenant A → tài nguyên tenant B trả 403/0 dòng); audit nội bộ bảo mật đạt mức cao; **pen-test chuyên nghiệp là hướng làm trước khi vận hành chính thức**. Không claim "đã pen-test đầy đủ".

4. **"Hệ thống chịu được bao nhiêu tenant?"** → Mô hình Pool (chung DB + RLS) chia sẻ tài nguyên hiệu quả; có thể nâng cấp instance (đã nâng backend lên t3.large) và bật ALB + multi-AZ + đọc-ghi tách biệt khi cần; mô hình lai (Silo cho enterprise) là hướng mở rộng.

5. **"Tự làm hết hay dùng công cụ AI hỗ trợ code?"** → Trung thực: có dùng công cụ AI hỗ trợ lập trình như công cụ năng suất, nhưng **kiến trúc, quyết định thiết kế, kiểm thử và hiểu biết hệ thống là của em** — sẵn sàng giải thích bất kỳ dòng nào. (Không giấu, không phóng đại.)

6. **"Vì sao 6 microservice cho 1 đồ án — có over-engineer không?"** → Tách theo 2 mặt phẳng: control-plane (lifecycle SaaS) và domain-plane (nghiệp vụ giáo dục); mỗi service một trách nhiệm rõ. Đánh đổi phức tạp vận hành được kiểm soát bằng Docker + gateway + thư viện dùng chung `kitehub-platform`.

7. **"Tại sao không dùng schema-per-tenant hay DB-per-tenant?"** → Chi phí (chênh ~20 lần ở 10 tenant) + vận hành (migration ×N schema). Pool + RLS cho cô lập đủ mạnh ở tầng DB với chi phí thấp; mô hình lai để dành cho enterprise (phát triển sau).

8. **"Dữ liệu sao lưu / khôi phục thế nào (DR)?"** → RDS có snapshot tự động; (nói thật mức hiện tại) khôi phục/diễn tập restore là phần đang hoàn thiện. Không phóng đại.

---

## Phần 8 — Tự kiểm tra trước buổi bảo vệ (checklist)

- [ ] Thuộc cheat-sheet Phần 0 (13 chênh lệch thesis-vs-code).
- [ ] Giải thích được RLS + fail-closed + tại sao chọn Pool (Phần 2).
- [ ] Có sẵn câu trả lời an toàn cho "demo AI thật?" (Phần 3.3) và "Singapore vs Nghị định 53" (Phần 5.2).
- [ ] Nhớ đúng version: Java 17, Spring Boot 3.5.x, Next.js 15, PostgreSQL 15 (RDS).
- [ ] Nhớ đúng con số: 6 trạng thái tenant, trial 14 ngày, grace 3 ngày, RLS 51/91 bảng, 3 EC2, JWT HS512.
- [ ] Phân biệt được "đã chạy thật" vs "hướng phát triển sau" cho: custom domain, FULL_AI, content-safety ML, WCAG backend gate, eInvoice, K-12/trẻ em.

---

*Tài liệu nội bộ phục vụ ôn tập của tác giả; không thuộc bản nộp khóa luận. Facts kiểm chứng trên code 2026-06-18.*
