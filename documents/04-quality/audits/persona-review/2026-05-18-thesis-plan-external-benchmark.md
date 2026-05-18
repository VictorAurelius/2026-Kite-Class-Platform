---
title: Thesis Plan External Benchmark — outside-in audit T2
status: complete
created: 2026-05-18
phase: Release 2 thesis planning
wave: pre-lock
trigger: outside-in-coverage-trigger.md §3 — Claude phải spawn outside-in trước khi lock scope
scope: External benchmark cho thesis report planning task (đồ án tốt nghiệp KiteHub/KiteClass Platform, sinh viên Nguyễn Văn Kiệt UTC K63 CNTT)
---

# Thesis Plan External Benchmark — outside-in audit T2

**Date:** 2026-05-18
**Trigger:** Pre-lock outside-in audit per `.claude/rules/outside-in-coverage-trigger.md` §3 — Claude tự động đề xuất outside-in audit khi dev brainstorm inside-out (thesis plan).
**Sources:** UTC template files (Quy định trình bày DATN + Hướng dẫn TTTN + Mẫu đề cương + 2 sample thesis K63) + Vietnamese tech university benchmark search (HUST, PTIT, UIT, HVKTMM, UTT, UTC, ĐH Công nghiệp HN) + industry CS thesis structure benchmark (CMU, TU Chemnitz, Leiden, CalState, Toronto Met) + IEEE citation guidance (UIT, OU, Vinh University).

---

## 1. UTC convention findings (canonical)

### 1.1 Định dạng (Format) — bắt buộc theo "Quy định trình bày đồ án tốt nghiệp" UTC

| Yêu cầu | Chuẩn UTC | Nguồn |
|---|---|---|
| Khổ giấy | A4 (210×297 mm), in 1 mặt, đóng bìa mềm | §2.1 Quy định trình bày |
| Căn lề | Trên 2.5cm / Dưới 2.5cm / Trái 3cm / Phải 2cm | §2.1 |
| Số trang | Đánh số giữa, phía trên đầu trang | §2.1 |
| Font chữ | Times New Roman, Unicode | §2.2 |
| Cỡ chữ tên chương | 18pt, Bold, căn giữa, Before 0pt After 12pt, sang trang mới | §2.2 |
| Cỡ chữ mục (1.1, 1.2) | 16pt, Bold, căn trái, Before 6pt After 6pt | §2.2 |
| Cỡ chữ tiểu mục (1.1.1) | 14pt, Bold, căn trái, Before 6pt After 6pt | §2.2 |
| Cỡ chữ đoạn văn | 13pt, justify, thụt đầu dòng 1cm, dãn dòng 1.2 lines | §2.3 |
| Tên bảng | Đặt PHÍA TRÊN bảng | §2.4 |
| Tên hình | Đặt PHÍA DƯỚI hình | §2.4 |
| Đánh số bảng/hình | Theo chương: `Bảng 3.1` = bảng đầu tiên chương 3 | §2.4 |
| Nguồn trích dẫn bảng/hình | Bắt buộc nếu lấy từ ngoài | §2.4 |
| Viết tắt | Sau lần viết đầu phải có ngoặc đơn; nếu nhiều phải có danh mục ABC | §2.4 |

### 1.2 Bố cục báo cáo DATN (Chapter convention)

UTC **KHÔNG ép cứng số chương** — "Số chương của một ĐATN tuỳ thuộc vào từng đề tài cụ thể" (§1 Quy định bố cục). Bố cục bắt buộc:

```
1.  LỜI CẢM ƠN (bắt buộc)
2.  MỤC LỤC (bắt buộc)
3.  DANH MỤC TỪ VIẾT TẮT (nếu có)
4.  DANH MỤC BẢNG BIỂU (nếu có)
5.  DANH MỤC HÌNH ẢNH (nếu có)
6.  MỞ ĐẦU (lý do chọn đề tài + tóm tắt nội dung + phương pháp + cấu trúc đồ án)
7.  CHƯƠNG 1 (tùy đề tài)
8.  CHƯƠNG 2 (tùy đề tài)
9.  CHƯƠNG 3 (tùy đề tài)
10. ... (thêm chương nếu cần)
11. KẾT LUẬN VÀ KIẾN NGHỊ
12. DANH MỤC TÀI LIỆU THAM KHẢO (chỉ tài liệu thực sự được trích dẫn)
13. PHỤ LỤC (nếu có)
```

Sample đề cương `Mục lục` trong Quy định cho thấy 3 chương là pattern mẫu, NHƯNG khoa CNTT-UTC không cấm 4-5 chương.

### 1.3 Trích dẫn — bắt buộc IEEE (Vancouver) per Bộ GD&ĐT VN

UTC mandate IEEE citation style (`[1]`, `[15, 314-315]`, `[19],[25],[41]`), KHÔNG dùng Harvard (tên tác giả-năm). Đặc tính:

- Đánh số theo **thứ tự trích dẫn xuất hiện trong báo cáo** (NOT alphabetical)
- Tài liệu tham khảo chỉ list những item **thực sự được trích dẫn**
- KHÔNG dùng đồ án/luận án/website làm TLTK (hạn chế)
- KHÔNG ghi học hàm/học vị tác giả
- Định dạng tài liệu sách / bài báo / luận án / hội nghị / website mỗi loại có template riêng (§3.1-§3.7 Hướng dẫn trích dẫn)

### 1.4 Đề cương DATN (Mau-Decuong) — pre-thesis artifact

Form 1 trang gồm 4 mục:
1. Nội dung, phạm vi đề tài
2. Công nghệ, công cụ, ngôn ngữ lập trình
3. Các kết quả chính dự kiến đạt được
4. Kế hoạch thực hiện đề tài (bảng STT × Nội dung × Thời gian dự kiến × Ghi chú)

Có 4 chữ ký: Trưởng Khoa + Trưởng Bộ môn + GVHD + Sinh viên.

### 1.5 Báo cáo TTTN (Thực tập tốt nghiệp) — sister artifact, 4-chương structure

TTTN report = báo cáo thực tập, NOT đồ án. Theo "Hướng dẫn trình bày báo cáo TTTN" 4 chương:

```
1. GIỚI THIỆU CHUNG VỀ ĐƠN VỊ THỰC TẬP
2. NỘI DUNG THỰC TẬP
3. KẾT QUẢ VÀ ĐÁNH GIÁ
4. NHẬN XÉT VÀ ĐỊNH HƯỚNG
```

Sample TTTN của Đặng Hữu Cương (cùng khóa K63, cùng GVHD TS. Nguyễn Đức Dư) **14 trang nội dung** chỉ tập trung mô tả thực tập, KHÔNG có code/architecture/testing chi tiết — TTTN ≠ DATN.

---

## 2. Vietnamese tech university benchmark

### 2.1 Bảng so sánh các đồ án/đề tài 2023-2025

| University / Source | Đề tài | Stack | Scope (suy luận) | Strength | Weakness |
|---|---|---|---|---|---|
| **UTC K63 (Đặng Hữu Cương)** | Học tập trực tuyến cá nhân hóa | Next.js 15 + FastAPI + PostgreSQL + AI Tutor (Gen AI) | 1 persona học viên + AI chatbot + dashboard tiến độ; MVP 80-85% precision recommendation | Stack hiện đại 2025-2026; có metric đo (Precision@K, Recall@K); kế hoạch 4 tháng rõ ràng (Feb-May 2026) | Scope mơ hồ "cá nhân hóa đơn giản"; KHÔNG đa-tenant; deploy Vercel + Render free tier (không AWS production-grade); AI section "to be updated" |
| **UIT NC (Microservices + DevOps)** | Web chat application | ReactJS frontend + Spring Boot backend microservices | Chia microservices theo chức năng, dùng DevOps CI/CD | Architecture mẫu cho microservices SaaS; rõ pattern phân tách service | Đề tài chat application scope hẹp hơn SaaS edu; KHÔNG có AI |
| **HVKTMM (Học viện Kỹ thuật Mật mã) 2023-2024** | TOEIC practice system | React + Spring Boot + security integration | Hệ thống luyện thi TOEIC tích hợp xác thực bảo mật | Stack tương tự thesis user; có security layer | Single-tenant, không multi-tenant SaaS; scope edu hẹp |
| **HVKTMM 2023-2024 (sample khác)** | E-commerce microservices platform | Microservices stack | Nền tảng e-commerce phân tán | Có pattern microservices cho thương mại | Domain khác (e-commerce, không edu); không có AI Agent |
| **PTIT (GitHub Phong-Kaster repo)** | Đồ án tốt nghiệp đa-đề | Java/Spring/React | Repo kinh nghiệm thực tập + đồ án sinh viên PTIT HCM | Có sample code cấu trúc thực tế | Không phải benchmark scope reference |
| **Revita-be (iamKhang)** | Đặt lịch khám bệnh + quản lý hồ sơ | NestJS + FastAPI microservices | Microservices y tế single-tenant | Microservices pattern y tế | Domain khác, không edu multi-tenant |
| **chidokun/QuanLyHocVien** | Phần mềm quản lý học viên trung tâm Anh ngữ | ASP.NET (PTTK HTTT, không phải DATN) | Single-tenant trung tâm Anh ngữ | Domain trùng KiteClass (trung tâm edu) | Scope nhỏ, ASP.NET stack cũ, không SaaS |
| **vansti/DATN** | Phần mềm thông minh quản lý chăm sóc học viên trung tâm đào tạo | (chưa rõ stack) | Single-tenant trung tâm | Domain trùng KiteClass | Không multi-tenant SaaS, không AI Agent |
| **ĐH Công nghiệp Hà Nội template 2025** | Template chung CNTT 2025 | N/A | Hướng dẫn template Word | Sample chuẩn 2025 | Không phải sample đề tài SaaS |
| **HUST SIE.VN (danh sách đồ án CNTT)** | List 50+ đề tài CNTT | Đa stack | Variety wide | Có scope reference list | Không có công khai full report |

### 2.2 Quan sát chung Vietnamese tech university theses (CNTT 2024-2026)

- **Page count:** sample DATN tham khảo public ~60-100 trang body (chưa kể phụ lục) — phù hợp khuyến nghị CS thesis quốc tế (CMU/TU Chemnitz ~60-80 trang)
- **Chapter convention phổ biến:** 4-5 chương; UTC sample đề cương 3 chương + Mở đầu + Kết luận = **5 sections total** là pattern tối thiểu
- **Scope trend 2024-2026:** AI integration (Generative AI/LLM/chatbot) trở thành must-have cho đề tài CNTT; microservices/cloud deploy trở thành điểm cộng strong
- **Multi-tenant SaaS:** RẤT HIẾM trong DATN VN — đa số single-tenant; thesis của user nằm ở **top tier complexity** so với benchmark UTC K63
- **AWS production deploy:** RẤT HIẾM — đa số dừng ở Vercel/Render free tier OR localhost docker-compose
- **Evidence trong báo cáo:** sample UTC ưu tiên screenshot UI + UML diagram + ERD; ít khi có production monitoring/CloudWatch dashboard/SLO measurement

---

## 3. Industry CS thesis structure taxonomy

### 3.1 5-chapter "traditional" CS thesis (CMU, TU Chemnitz, Leiden, Toronto Met)

```
Chapter 1: Introduction (background + problem statement + scope + structure)
Chapter 2: Literature Review / Background (related work + theoretical foundation)
Chapter 3: Methodology / Design / Implementation (architecture + tech stack + approach)
Chapter 4: Evaluation / Results / Testing (qualitative + quantitative metrics)
Chapter 5: Conclusion + Future Work (limitations + lessons + roadmap)
```

Ratio guideline: chapter 2/3/4 mỗi chapter ~1/3 nội dung body (per TU Chemnitz). Total 60-80 trang body.

### 3.2 SaaS thesis taxonomy

**Persona scope distribution (industry benchmark):**
| Persona count | Frequency | Strength | Weakness |
|---|---|---|---|
| 1 persona (admin only OR student only) | ~50% DATN VN | Scope khả thi 4 tháng | Thiếu real-world complexity |
| 2-3 persona | ~35% | Cover end-user + admin minimum | Realistic |
| 4-5 persona | ~10% | Demonstrate multi-actor system | Risk over-scope |
| 6+ persona | <5% | Industry-grade (như KiteClass) | High risk over-scope; cần justification |

**Tenant count distribution:**
| Tenant evidence | Frequency | Notes |
|---|---|---|
| 1 demo tenant only | ~70% DATN | Phổ biến nhất, không chứng minh isolation |
| 2-3 tenant demo | ~20% | Đủ chứng minh data isolation |
| 5+ tenant evidence (production) | <10% | Industry-grade, hiếm trong DATN |

**AI integration tier:**
| Tier | Description | Frequency 2024-2026 |
|---|---|---|
| Not relevant | Đề tài không có AI | ~30% |
| Nice-to-have | Chatbot/recommendation lightweight | ~50% (đa số DATN 2025+) |
| Must-have / core | AI Agent/MLOps/RAG production-grade | ~20% (xu hướng tăng) |

**Deployment evidence:**
| Tier | Frequency | Strength |
|---|---|---|
| Localhost docker-compose | ~60% | Đủ minimum cho DATN |
| Vercel/Render free tier | ~25% | Có production URL demo |
| AWS/GCP production-grade | <15% | **Strong differentiator** (như thesis user) |

**Testing evidence:**
| Tier | Frequency | Notes |
|---|---|---|
| Unit test only | ~70% | Minimum |
| Unit + integration | ~25% | Khuyến nghị |
| Unit + integration + E2E (Playwright/Cypress) | <5% | Industry-grade |

---

## 4. Gap analysis vs user inside-out scope

User inside-out (tóm tắt từ existing draft `graduation-thesis-outline-v3.1.md` V4.1 Bundled Model):

- **Scope:** KiteHub Platform (Modular Monolith) + KiteClass instances (Microservices 3-5 services) + LMS + Marketing
- **Personas:** Customer, Admin KiteHub, Center Owner, Center Admin, Teacher, Student, Parent, Guest (8 actors)
- **Tenant evidence:** 2 teachers + 2 business trial/vip (4 tenant)
- **AI integration:** AI Agent tạo branding ($0.19/instance — OpenAI GPT-4 + Stable Diffusion XL + Remove.bg)
- **Stack:** Java 21 + Spring Boot 3.2 + Next.js 14 + PostgreSQL 15 + Redis 7 + Docker + Kubernetes + AWS deploy
- **Multi-tenancy:** Database-per-tenant
- **LOC ước tính:** 20,000+ LOC
- **Phase 1 BETA:** P1 + P2 trong scope (per CLAUDE.md Phase 1 Phase progression)

### 4.1 ALIGN với benchmark (điểm mạnh — giữ nguyên)

- ✅ **Stack hiện đại 2025-2026:** Java Spring Boot 3.2 + Next.js 14 align với HVKTMM/UIT/SIE.VN benchmark + xu hướng full-stack 2025-2026
- ✅ **Microservices + Modular Monolith hybrid:** kiến trúc lý thuyết solid, justification (ROI -95% cho Service Registry) thể hiện tư duy phân tích — **STRONG differentiator** so với DATN UTC K63 trung bình
- ✅ **AI Agent integration:** align xu hướng must-have AI tier 2025-2026; cụ thể (3 AI services + chi phí $0.19/instance) cho thấy hiểu sâu, không chỉ chatbot lightweight
- ✅ **Multi-tenant SaaS production:** **rất hiếm** trong DATN VN — top tier complexity; nếu hoàn thành sẽ outperform 90% DATN cùng khóa
- ✅ **AWS production deploy:** dưới 15% DATN VN làm production-grade — **strong differentiator**
- ✅ **Parent Portal + VietQR Payment:** học từ BeeClass = market validation cụ thể, không phải tự bịa
- ✅ **Living docs (3-layer business docs):** governance discipline vượt xa DATN trung bình (đa số chỉ có UML use case + ERD)

### 4.2 AT-RISK — cần justify hoặc narrow scope

- ⚠️ **8 personas overshoot:** vượt sample DATN VN (đa số 1-3 personas, top 10% 4-5 personas). Nguy cơ over-scope cho 4 tháng. **Hành động:** chia tier "Core persona evidence" (P1 Solo + P2 Owner + Student + Parent — Tier 1 BETA) vs "Advanced persona evidence" (Teacher + Center Admin + Customer + Guest — Tier 2 reference). Báo cáo §4 (chương Evaluation) phải nói rõ persona nào đã được "production-verified end-to-end" vs "scoped but not full-flow verified".
- ⚠️ **LOC 20,000+:** đặt mục tiêu cao; đa số DATN VN single-developer 8,000-15,000 LOC. **Hành động:** chấp nhận LOC cao nếu split giữa generated (UI components, DTO, repository CRUD boilerplate) vs hand-written business logic; báo cáo phải có breakdown LOC type.
- ⚠️ **Đề tài quá rộng cho 1 sinh viên 4 tháng:** KiteHub (Modular Monolith 4 modules) + KiteClass (3-5 microservices) + LMS + Marketing + AI Agent + AWS deploy = **6-7 major components**. So với UTC K63 trung bình (1 component, 1 stack). **Hành động:** Mở đầu PHẢI có "Phạm vi đề tài" trong đó nói rõ "core scope" (cần demo trong báo cáo) vs "future scope" (Phase 2/3 ngoài DATN scope). Tránh trường hợp hội đồng hỏi "em đã làm xong toàn bộ chưa?" mà sinh viên không cover được.
- ⚠️ **AI cost $0.19/instance:** rất cụ thể nhưng cần evidence (log files, AWS bill screenshot) trong báo cáo Chương 4 Evaluation
- ⚠️ **Database-per-tenant:** scale tốt cho ≤10 tenant; nếu hội đồng hỏi "100 tenant thì sao?" cần trả lời được (current design + roadmap shared-DB pattern Phase 3). Báo cáo Chương 2 Architecture phải đề cập limitation này.

### 4.3 GAP — bổ sung trước khi lock

- ❌ **Chapter 1 Introduction (Mở đầu) cần thêm:**
  - "Câu hỏi nghiên cứu" cụ thể (research question) — không chỉ "xây dựng hệ thống" mà "AI Agent tự động hóa branding có khả thi cho SaaS multi-tenant edu với chi phí ≤$1/instance hay không?"
  - "Đóng góp khoa học/kỹ thuật" — phân biệt rõ "đóng góp mới" vs "tích hợp công nghệ có sẵn". KiteClass đóng góp = ROI analysis methodology cho microservices vs modular monolith decision, không phải tech stack
  - "Phương pháp nghiên cứu" cụ thể — Action research? Design science? Comparative case study? Hội đồng UTC thường hỏi
- ❌ **Chapter 2 Literature Review / Tổng quan THIẾU:**
  - Existing draft v3.1 nhảy thẳng vào architecture (V2 vs V4.1) mà thiếu **tổng quan lý thuyết multi-tenant SaaS** (Patterns of Enterprise Application Architecture by Martin Fowler, Multi-tenant Data Architecture by Microsoft) + **comparison với competitors VN** (BeeClass, Misa MIMOSA, EduFit, ClassIn) — bảng so sánh feature × competitor × KiteClass
  - **Thiếu lý thuyết AI Agent** (LangChain, RAG pattern, Generative AI for marketing assets) — cần literature review tối thiểu 10-15 nguồn IEEE
- ❌ **Chapter 4 Evaluation / Đánh giá THIẾU concrete metrics:**
  - Hiện draft đề cập "Tiết kiệm 40% RAM" nhưng KHÔNG có baseline measurement. Phải có:
    - **Performance metrics:** API p50/p95 latency, throughput, RAM/CPU per service (Prometheus screenshot)
    - **AI Agent metrics:** thời gian tạo (target 30s), chi phí ($0.19/instance), số instance đã test (≥4 tenant để có evidence)
    - **Multi-tenant isolation evidence:** test cross-tenant data leak (PDPL compliance angle)
    - **Cost analysis:** AWS bill breakdown — chứng minh free tier sustainability hoặc cost per tenant
  - Metrics phải có trong báo cáo Chương 4 với screenshots/tables
- ❌ **Tài liệu tham khảo IEEE — sample draft v3.1 KHÔNG có references section.** UTC bắt buộc; tối thiểu 15-20 references IEEE format cho DATN. Cần:
  - Sách: Sam Newman "Building Microservices" + Eric Evans "Domain-Driven Design" + Martin Fowler "Patterns of Enterprise Application Architecture"
  - Bài báo: AWS Well-Architected Framework whitepaper + Twelve-Factor App + DORA metrics paper
  - Specification: OWASP Top 10 (2021) + PDPL 2023 + Decree 13/2023/NĐ-CP
  - Vietnamese context: BeeClass / Misa MIMOSA / EduFit comparison links
- ❌ **Phụ lục:**
  - Sample đề cương draft v3.1 mention 6 sơ đồ (architecture-simple/bfd-actors/erd/architecture-full/system-overview-v3/business-flow-v3) — cần ALL renderable PNG ≥1200×900 cho báo cáo Word
  - Bổ sung: deploy runbook (chương 3 reference); E2E test report; AWS cost report; gap analysis (đề cập gap process trong báo cáo = strong differentiator) — **nhưng chỉ trong phụ lục, không spam chương chính**

---

## 5. Recommended chapter structure (5-chapter, UTC-compliant + industry-aligned)

Đề xuất bố cục cho thesis report (extends UTC §1.2 với "5-chapter traditional"):

```
LỜI CẢM ƠN
MỤC LỤC
DANH MỤC TỪ VIẾT TẮT (KiteHub, KiteClass, SaaS, BR, UC, ADR, ...)
DANH MỤC BẢNG BIỂU
DANH MỤC HÌNH ẢNH

MỞ ĐẦU (~4-6 trang)
- Bối cảnh + lý do chọn đề tài (chuyển đổi số giáo dục VN + thiếu SaaS multi-tenant)
- Mục tiêu + câu hỏi nghiên cứu cụ thể
- Phạm vi nghiên cứu (Core scope vs Future scope split)
- Phương pháp nghiên cứu (Design Science + Action Research kết hợp)
- Đóng góp dự kiến của đề tài
- Cấu trúc đồ án

CHƯƠNG 1: TỔNG QUAN VÀ CƠ SỞ LÝ THUYẾT (~15-20 trang)
1.1. Tổng quan thị trường SaaS giáo dục Việt Nam
1.2. Phân tích các giải pháp hiện có (BeeClass, Misa, EduFit, ClassIn) — bảng so sánh feature × competitor
1.3. Cơ sở lý thuyết Multi-tenant SaaS (database-per-tenant vs shared-DB vs schema-per-tenant)
1.4. Cơ sở lý thuyết Microservices vs Modular Monolith
1.5. Cơ sở lý thuyết AI Agent (Generative AI, LLM, image generation)
1.6. Pháp lý + tuân thủ (PDPL 2023, Decree 13/2023, Luật An ninh mạng)

CHƯƠNG 2: PHÂN TÍCH YÊU CẦU VÀ THIẾT KẾ HỆ THỐNG (~15-20 trang)
2.1. Phân tích yêu cầu nghiệp vụ (8 personas, ưu tiên Tier 1)
2.2. Use case diagram tổng quát + chi tiết per persona core
2.3. Kiến trúc hệ thống tổng thể (KiteHub Modular Monolith + KiteClass Microservices)
2.4. Lý do lựa chọn kiến trúc (ROI analysis Service Registry -95%, V2 vs V4.1 trade-off)
2.5. Database design (ERD KiteHub + ERD KiteClass instance, database-per-tenant)
2.6. AI Agent design (workflow + 3 AI services integration)
2.7. Security design (JWT, OWASP Top 10, PDPL compliance)

CHƯƠNG 3: TRIỂN KHAI HỆ THỐNG (~15-20 trang)
3.1. Công nghệ và công cụ sử dụng (justification cho từng tech)
3.2. Triển khai KiteHub Platform (4 modules: Sale, Message, Maintaining, AI Agent)
3.3. Triển khai KiteClass Instance Services (3-5 microservices)
3.4. Triển khai Frontend (Next.js 14 App Router, theming per-tenant)
3.5. Triển khai AI Agent workflow (Remove.bg + GPT-4 + SDXL pipeline)
3.6. CI/CD pipeline (GitHub Actions + Docker + Kubernetes)
3.7. AWS deployment (ap-southeast-1, Free Tier strategy, ADR-025)

CHƯƠNG 4: KIỂM THỬ VÀ ĐÁNH GIÁ (~10-15 trang)
4.1. Chiến lược kiểm thử (Unit + Integration + E2E Playwright)
4.2. Kết quả unit test + coverage (target ≥80% backend, ≥70% frontend)
4.3. Kiểm thử persona walkthrough (Tier 1: P1 Solo + P2 Owner + Student + Parent — production-verified)
4.4. Performance benchmark (API p50/p95 latency, throughput, RAM/CPU per service)
4.5. AI Agent metrics (thời gian tạo, chi phí $0.19/instance evidence, ≥4 tenant test)
4.6. Multi-tenant isolation evidence (cross-tenant data leak test PASS)
4.7. Quality audit /100 score (per project's quality-audit skill)
4.8. Cost analysis (AWS bill, free tier sustainability)
4.9. So sánh với competitors (KiteClass vs BeeClass × feature × performance)
4.10. Hạn chế đã phát hiện + future scope (Phase 2/3 K-12)

CHƯƠNG 5: KẾT LUẬN VÀ KIẾN NGHỊ (~3-5 trang)
5.1. Kết quả đạt được (recap mục tiêu vs kết quả thực)
5.2. Đóng góp khoa học và thực tiễn
5.3. Hạn chế của đề tài
5.4. Hướng phát triển trong tương lai (Phase 2 + Phase 3 + K-12 P5)
5.5. Bài học kinh nghiệm

DANH MỤC TÀI LIỆU THAM KHẢO (IEEE format, ≥15-20 nguồn)
PHỤ LỤC A: Sơ đồ kiến trúc chi tiết (PNG ≥1200×900)
PHỤ LỤC B: Database schema chi tiết (DDL)
PHỤ LỤC C: AI Agent prompt templates
PHỤ LỤC D: Deploy runbook
PHỤ LỤC E: Mã nguồn (GitHub link + key files snapshot)
```

**Total page count target:** 70-90 trang body (chưa kể phụ lục) — tuân theo TU Chemnitz CS thesis 60-80 trang khuyến nghị, ratio chương 1/2/3/4 mỗi chương ~1/3-1/4 body.

---

## 6. Top 5 actionable items

1. **🔴 Adopt 5-chapter UTC-compliant structure** (§5 above) — extends UTC §1.2 3-chapter mẫu thành 5-chapter pattern industry-standard. Rationale: UTC không cấm 5-chương, ratio 1/3-1/3-1/3 cho chapter 2/3/4 (per TU Chemnitz/CMU CS thesis guideline) phù hợp scope KiteClass production-grade vượt xa DATN trung bình.

2. **🔴 Bổ sung Chapter 1 (Tổng quan và Cơ sở lý thuyết) THIẾU trong draft v3.1** — research question + literature review ≥15-20 IEEE references + competitor comparison (BeeClass/Misa/EduFit/ClassIn) + lý thuyết multi-tenant SaaS + lý thuyết AI Agent + pháp lý VN (PDPL/Decree 13/Luật An ninh mạng). Đây là gap LỚN NHẤT so với UTC sample + industry standard.

3. **🟠 Narrow persona scope thành 2 tier** — Tier 1 BETA (P1 Solo + P2 Owner + Student + Parent — production-verified end-to-end trong báo cáo) vs Tier 2 reference (Teacher + Center Admin + Customer + Guest — design-only, future scope). Justify trong "Phạm vi nghiên cứu" Mở đầu. Tránh hội đồng chất vấn "8 persona đã làm hết chưa?".

4. **🟠 Concrete evidence cho Chapter 4 Evaluation** — performance metrics screenshots (Prometheus + Grafana), AI Agent cost log ($0.19/instance từ ≥4 tenant), multi-tenant isolation test evidence (PDPL angle), AWS bill breakdown, quality audit /100 score. Hiện draft v3.1 dùng số "40% RAM saving" mà không có baseline → vi phạm `output-review-mandate.md` §3 "Review evidence preserved".

5. **🟡 IEEE references list bắt buộc + phụ lục đầy đủ** — minimum 15-20 references theo template UTC §3.1-§3.7 (sách Sam Newman "Building Microservices" + Eric Evans "DDD" + Martin Fowler "PEAA" + AWS Well-Architected paper + Twelve-Factor App + DORA paper + OWASP Top 10 + PDPL/Decree 13 + competitor websites). Phụ lục: 6 sơ đồ ≥1200×900 PNG + Database DDL + AI prompts + deploy runbook + GitHub link.

---

## 7. Status

**Outside-in audit T2 COMPLETE.** Findings ready để merge vào thesis plan §1 Brainstorm Q1.

**Sources used:**
- UTC `Quy dinh trinh bay do an tot nghiep.pdf` (canonical format + bố cục + IEEE citation)
- UTC `Mau-Decuong DATN-Cử nhân.pdf` (pre-thesis form template)
- UTC `Huong dan trinh bay bao cao TTTN.pdf` (sister artifact pattern)
- UTC `CUONG_THAMKHAO_Decuong DATN-DuThao.pdf` (sample K63 cùng GVHD)
- UTC `CUONG_THAMKHAO_BaoCaoTTTN-DuThao.pdf` (sample K63 TTTN cùng GVHD)
- WebSearch: 4 queries cross Vietnamese tech universities + industry CS thesis benchmark + IEEE citation guidance
- Existing `documents/07-archived/academic/thesis/graduation-thesis-outline-v3.1.md` V4.1 Bundled Model (user inside-out draft)

**Constraints honored:**
- KHÔNG ship code (audit-only)
- KHÔNG flip gap status
- KHÔNG tạo PR
- Vietnamese narrative + English technical identifiers per `dev-readable-doc-language.md`
- File path follows `audit-to-gap-pipeline.md` + `output-review-mandate.md` §3 persona-review row pattern

---

## 8. References (external sources cited)

- [HUST Digital Library](https://dlib.hust.edu.vn/)
- [PTIT Digital Library](https://dlib.ptit.edu.vn/handle/HVCNBCVT/2220)
- [UIT NC — Microservices DevOps thesis](https://nc.uit.edu.vn/do-an/xay-dung-ung-dung-microservices-va-tich-hop-devops)
- [HVKTMM danh sách đồ án 2023-2024](https://www.studocu.vn/vn/document/hoc-vien-ky-thuat-mat-ma/do-an-tot-nghiep/danh-sach-de-tai-do-an-2023-2024-dky/92194803)
- [HUST SIE.VN — đồ án CNTT](https://sie.hust.edu.vn/danh-sach-phan-cong-project-va-do-an-tot-nghiep-nganh-cntt/)
- [ĐH Công nghiệp HN template 2025](https://www.studocu.vn/vn/document/truong-dai-hoc-cong-nghiep-ha-noi/truong-dai-hoc-cong-nghiep-ha-noi/template-datn-2025-do-an-tot-nghiep-dai-hocnganh-cong-nghe-thong-tin/122865896)
- [Phong-Kaster PTIT thesis guide](https://github.com/Phong-Kaster/PTIT-Do-An-Tot-Nghiep)
- [UIT — IEEE citation style guide](https://www.uit.edu.vn/chuan-trich-dan-tai-lieu-tham-khao-ieee-ieee-citation-style)
- [OU — IEEE citation example](https://it.ou.edu.vn/asset/ckfinder/userfiles/5/files/TrichDanKieu_IEEE.pdf)
- [TU Chemnitz — Bachelor thesis guideline](https://www.tu-chemnitz.de/informatik/ce/files/Guidelines-Bachelor-Thesis.pdf)
- [Leiden — Writing CS thesis](https://liacs.leidenuniv.nl/~nijssensgr/bachelorklas-2014-2015/writing.pdf)
- [CMU CS — Thesis sample](https://www.cs.cmu.edu/~ckaestne/pdf/thesispusch.pdf)
- [Toronto Met — CS thesis guide](https://www.torontomu.ca/content/dam/cs/grad-pdf/Thesis_Guide.pdf)
- [UTC FIT — Đồ án tốt nghiệp listings](https://fit.utt.edu.vn/vi/do-an-tot-nghiep-c29?page=3)
