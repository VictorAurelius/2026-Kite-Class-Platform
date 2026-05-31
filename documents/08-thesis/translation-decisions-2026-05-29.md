---
title: Translation decisions — Wave thesis-3 sweep 2026-05-29
audience: dev
last-updated: 2026-05-29
status: applied-best-practice
---

# Bảng quyết định bản dịch — Wave thesis-3 sweep

**Mục đích:** review từng cặp dịch ở 6 file chapter MD. Per `thesis-content-standard.md` v1.1.0 §10 S3, English giữ nguyên CHỈ khi: (1) không có Vietnamese equivalent đúng nghĩa, (2) vendor/product proper noun, (3) code identifier inline.

**User cần đánh dấu:**
- ✅ KEEP: dịch hợp lý, giữ Vietnamese
- ⏪ REVERT: technical term, nên giữ English
- 🟡 DEBATABLE: review cùng GVHD trước khi quyết định

---

## A. Technical terms — likely REVERT (rule S3 #1: không có VN equivalent đúng)

| # | English source | Đã dịch thành | File | Recommendation |
|---|---|---|---|---|
| A1 | `endpoint` | điểm cuối API | ch3 §3.2.4, ch4 §4.2 | ⏪ REVERT — "endpoint" common VN dev vocab |
| A2 | `end-user` | người dùng cuối | ch1-competitor §1.2.5/1.2.6, ch4 §4.2 | ⏪ REVERT? — "end-user" UX/HCI standard term; HOẶC keep "người dùng cuối" trong narrative |
| A3 | `pipeline` (CI/CD) | đường ống triển khai / đường ống CI/CD | ch4 §4.1.4 | 🟡 DEBATABLE — "CI/CD pipeline" thường giữ nguyên; "đường ống" có thể lạ tai |
| A4 | `workflow` (GitHub Actions) | workflow (giữ) / lần chạy workflow | ch4 §4.1.4 | ✅ KEEP — giữ "workflow" cho GitHub Actions context |
| A5 | `deploy` (verb) | triển khai | ch4 §4.1.4 | ✅ KEEP — "triển khai" chuẩn |
| A6 | `instance` (EC2/RDS) | thực thể | ch1-competitor, ch4 §4.1.7 | ⏪ REVERT — "instance" technical AWS term; "thực thể" gây hiểu lầm sang "entity" trong DB |
| A7 | `multi-tenant` / `single-tenant` | đa tenant / đơn tenant | nhiều chỗ | ✅ KEEP — "đa tenant" / "đơn tenant" đã thành chuẩn VN SaaS |
| A8 | `schema-per-tenant` | mỗi tenant một schema riêng | ch1-competitor §1.3.1 | ✅ KEEP — natural translation |
| A9 | `Bounded context` (DDD) | Ngữ cảnh giới hạn (bounded context) | ch1-ai §1.3.5 | ✅ KEEP — đã định nghĩa inline |
| A10 | `audit log` | nhật ký kiểm toán | nhiều chỗ | 🟡 DEBATABLE — "nhật ký kiểm toán" formal nhưng "audit log" common tech VN |
| A11 | `cutover` (DNS) | chuyển đổi | ch4 §4.1.7 | ⏪ REVERT — "cutover" specific DevOps term |
| A12 | `assume role` (IAM) | đảm nhận vai trò | ch4 §4.1.4 | 🟡 DEBATABLE — "IAM assume role" specific AWS term |
| A13 | `narrow IAM scope` | phạm vi IAM thu hẹp | ch4 §4.1.4 | ✅ KEEP |
| A14 | `confirm-input gate` | cổng xác nhận đầu vào | ch4 §4.1.4 | ✅ KEEP |
| A15 | `ephemeral OIDC role` | vai trò OIDC tạm thời | ch4 §4.1.4 | ✅ KEEP |
| A16 | `hardcode` (AWS keys) | nhúng cứng | ch4 §4.1.4 | ✅ KEEP — "nhúng cứng" natural |
| A17 | `smoke admin-login post-deploy` | kiểm thử nhanh đăng nhập quản trị sau triển khai | ch4 §4.1.4 | ✅ KEEP |
| A18 | `class binding Postgres-specific` | lỗi liên kết kiểu đặc thù PostgreSQL | ch4 §4.1.4 | 🟡 DEBATABLE — "class binding" specific Java/JDBC term |
| A19 | `unit test` | kiểm thử đơn vị | ch4 §4.1.4 | ✅ KEEP |
| A20 | `integration test` | kiểm thử tích hợp | ch1-ai §1.3.5 | ✅ KEEP |
| A21 | `mock response` | phản hồi giả lập | ch1-ai §1.3.5 | ✅ KEEP |
| A22 | `edge case` | tình huống biên | ch1-ai §1.3.5 | ✅ KEEP |
| A23 | `rate limit` | giới hạn tần suất | ch1-ai §1.3.5 | ✅ KEEP |
| A24 | `LLM` | mô hình ngôn ngữ lớn (LLM — Large Language Model) | ch1-ai §1.3.1 | ✅ KEEP — defined inline first use |
| A25 | `CAGR` | tăng trưởng kép hàng năm (CAGR — Compound Annual Growth Rate) | ch1-ai §1.3.1 | ✅ KEEP — defined inline |
| A26 | `diffusion (model)` | khuếch tán | ch1-ai §1.3.1 | 🟡 DEBATABLE — math/ML term "diffusion model" common |
| A27 | `SOTA` (state-of-the-art) | tiên tiến nhất | ch1-ai §1.3.1 | ✅ KEEP — "tiên tiến nhất" tự nhiên |
| A28 | `self-host` | tự vận hành | ch1-ai §1.3.1 | ✅ KEEP |
| A29 | `tech debt` | gánh nợ kỹ thuật | ch1-ai §1.3.1 | ✅ KEEP |
| A30 | `inference` (AI) | suy luận | ch1-ai §1.3.6 | ✅ KEEP |
| A31 | `training` (AI) | huấn luyện | ch1-ai §1.3.6 | ✅ KEEP |

---

## B. Narrative jargon — clearly KEEP (rule S3 narrative discipline)

| # | English source | Đã dịch thành | File | Recommendation |
|---|---|---|---|---|
| B1 | `persona` (lower-case) | nhóm người dùng đại diện | ch1-competitor §1.2.5/1.3, ch3 §3.2.4 | ✅ KEEP — academic VN convention |
| B2 | `platform` | nền tảng | nhiều chỗ | ✅ KEEP |
| B3 | `dashboard` | bảng tổng quan | nhiều chỗ | ✅ KEEP |
| B4 | `manager` (role) | vai trò quản lý | ch1-competitor §1.1.2 | ✅ KEEP |
| B5 | `mobile` (device) | thiết bị di động | ch1-competitor §1.1.2 | ✅ KEEP |
| B6 | `Zalo group` (chat) | nhóm Zalo | ch1-competitor §1.1.2 | ✅ KEEP |
| B7 | `bulk import` | nhập hàng loạt | ch1-competitor §1.1.2 | ✅ KEEP |
| B8 | `native` (mobile app) | gốc | ch1-competitor §1.3.4 | ✅ KEEP |
| B9 | `onboarding (wizard)` | trình hướng dẫn khởi tạo / quy trình khởi tạo | nhiều chỗ | ✅ KEEP |
| B10 | `baseline` (compliance) | ngưỡng / đường nền | ch1-vn-law §1.3.3 | ✅ KEEP |
| B11 | `cohort` (tenant) | nhóm thử nghiệm | ch1-vn-law §1.3.3 | ✅ KEEP |
| B12 | `roadmap` | lộ trình | nhiều chỗ | ✅ KEEP |
| B13 | `MVP (Minimum Viable Product)` | sản phẩm tối giản khả thi (MVP) | ch1-vn-law §1.3.3 | ✅ KEEP — defined inline |
| B14 | `K-12 expansion` | mở rộng sang khối K-12 | ch1-vn-law §1.3.3 | ✅ KEEP |
| B15 | `EdTech` | công nghệ giáo dục | ch1-ai §1.3.1 | ✅ KEEP |
| B16 | `enterprise` (segment) | hướng doanh nghiệp | ch1-competitor §1.2.1 | ✅ KEEP |
| B17 | `PLG (Product-Led Growth)` | tăng trưởng dẫn dắt bởi sản phẩm (PLG) | ch1-competitor §1.3.2 | ✅ KEEP — defined inline |
| B18 | `marketing` | tiếp thị | ch1-competitor §1.3.4 | ✅ KEEP |
| B19 | `beta tenants` | tenant thử nghiệm | ch4 §4.2 | ✅ KEEP |
| B20 | `partnership` | hợp tác | ch1-vn-law §1.3.3 | ✅ KEEP |
| B21 | `SLA` (commitment) | thỏa thuận mức dịch vụ (SLA) | ch1-vn-law §1.3.3 | ✅ KEEP — defined inline |
| B22 | `uptime` | thời gian hoạt động | ch1-vn-law §1.3.3 | ✅ KEEP |
| B23 | `live chat` | trò chuyện trực tuyến | ch1-vn-law §1.3.3 | ✅ KEEP |
| B24 | `ship (verb)` | phát hành | ch1-vn-law §1.3.3 | ✅ KEEP |
| B25 | `launch` | ra mắt | ch1-ai §1.3.6 | ✅ KEEP |
| B26 | `DPO` / `DPIA` | định nghĩa Việt + acronym | ch1-vn-law §1.3.3 | ✅ KEEP — per S7 first-use mandate |
| B27 | `infrastructure` | hạ tầng | ch1-ai §1.3.1 | ✅ KEEP |
| B28 | `feedback` | phản hồi | ch4 §4.2 | ✅ KEEP |
| B29 | `screenshot` | ảnh chụp | ch4 §4.2 | ✅ KEEP |
| B30 | `monitoring` | giám sát | ch1-ai §1.3.5/§1.3.1 | ✅ KEEP |
| B31 | `feature` (product) | tính năng | nhiều chỗ | ✅ KEEP |
| B32 | `disclosure` | công bố | ch1-ai §1.3.6 | ✅ KEEP |
| B33 | `bias mitigation` | giảm thiểu thiên kiến | ch1-ai §1.3.6 | ✅ KEEP |
| B34 | `Human-in-the-loop review` | đánh giá có sự tham gia của con người | ch1-ai §1.3.6 | ✅ KEEP |
| B35 | `web responsive` | giao diện web đáp ứng | ch1-competitor §1.3.4 | ✅ KEEP |
| B36 | `tone brand` | phong cách thương hiệu | ch1-ai §1.3.2 | ✅ KEEP |
| B37 | `landing page` | trang chủ | ch1-ai §1.3.2, ch4 §4.1.7 | ✅ KEEP |
| B38 | `hero image` | ảnh nền | ch1-ai §1.3.2 | ✅ KEEP |

---

## C. Restructure decisions (Wave thesis-3 hôm nay)

| # | Change | Recommendation |
|---|---|---|
| C1 | Drop §2.1.3 misplaced (B-learning + persona + Bảng 2.3/2.4) | ✅ KEEP — content đúng thuộc Ch.1 §1.1 |
| C2 | Move B-learning → Ch.1 §1.1.1; persona → Ch.1 §1.1.2 | ✅ KEEP |
| C3 | Merge Bảng 2.3 (Đặc trưng VN → NFR) vào §2.1.2 NFR | ✅ KEEP — design rationale gần NFR cho committee trace |
| C4 | Drop Bảng 2.4 (Định vị cạnh tranh) | ✅ KEEP — redundant với Ch.1 §1.2.6 |
| C5 | Renumber Bảng 2.5→2.4, 2.6→2.5, 2.9-subdomain→2.6 | ✅ KEEP — fixes pre-existing duplicate 2.9 bug |

---

## D. Files modified (final state)

| File | LOC delta | Translation count (approx) |
|---|---|---|
| `chapter-1-vn-law-methodology.md` | +0/-0 (substitution) | ~18 jargon replaced |
| `chapter-1-competitor-analysis.md` | +5/-3 | ~15 jargon + restructure §1.1.1/1.1.2 add |
| `chapter-1-ai-techniques.md` | +29/-29 | ~30 jargon |
| `chapter-2-system-architecture.md` | +18/-39 | ~10 jargon + §2.1.3 drop + renumber |
| `chapter-3-implementation.md` | +1/-1 | 3 jargon (persona/endpoint/dashboard) |
| `chapter-4-deployment-results.md` | +8/-8 | ~15 jargon |
| `thesis-v1.docx` | rebake (3.17→2.43MB, 84→80 pages) | — |

**Total recommendation summary:**
- ✅ KEEP: ~50 translations (clearly Vietnamese-appropriate)
- ⏪ REVERT candidates: 3-4 (endpoint, instance, cutover, possibly end-user)
- 🟡 DEBATABLE: 5-6 (pipeline, audit log, assume role, class binding, diffusion, multimodal)

---

## E. Applied (best practice 2026-05-29)

**Đã REVERT theo best practice:**
- A1 `endpoint` — restored (6 instances ch3 + ch4)
- A2 `end-user` — restored (5 instances ch1-competitor + ch4)
- A6 `instance` (EC2/RDS) — restored (19 instances ch1-competitor + ch1-ai + ch4)
- A11 `cutover` (DNS) — restored (1 instance ch4)
- A3 `pipeline` (CI/CD) — restored (6 instances ch4 §4.1.4/4.1.7)
- A10 `audit log` — restored (5 instances ch1-ai + ch2 + ch4)
- A12 `assume role` (IAM) — restored ch4 §4.1.4
- A18 `class binding` (Java/JDBC) — restored ch4 §4.1.4
- A26 `diffusion (model)` — restored ch1-ai §1.3.1
- Bonus: `unit test` / `smoke test` / `production mode` / `apply` (Terraform) — restored ch4 §4.1.4/4.1.7

**Giữ Vietnamese (§B 38 cặp):** persona / platform / dashboard / manager / mobile / bulk import / onboarding / baseline / cohort / roadmap / MVP / K-12 / EdTech / SLA / DPO / DPIA / launch / bias mitigation + 28 cặp narrative khác — đã verify natural Vietnamese reading flow.

## F. Future scope (next wave)

Sweep deeper technical sections chưa touch (ch1-ai §1.3.2-1.3.5 AI Branding kỹ thuật, ch2 §2.2-2.3 architecture detail, ch3 §3.1/3.3 implementation detail) — defer next session vì cần careful technical fidelity preservation.

---

## F. Sister memory + rule references

- Memory: `feedback_thesis_mixed_lang_copy_propagation.md` (recurrence #2, 2026-05-29)
- Rule: `.claude/rules/thesis-content-standard.md` v1.1.0 §10 S3 VN-narrative-strict
- Rule: `.claude/rules/dev-readable-doc-language.md` §3 acceptable English class (#1 no equivalent, #2 proper noun, #3 code identifier)
- Rule: `.claude/rules/vn-localization-audit-checklist.md` §2 Section 2 (Vietnamese label)
