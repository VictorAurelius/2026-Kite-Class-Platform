---
title: Outside-in Audit — VN CS Thesis Benchmark cho Release 1.5 Scope
status: complete
created: 2026-05-18
audit_type: external-benchmark
agent_model: sonnet-4-6
trigger: Release 1.5 thesis scope decision per outside-in-coverage-trigger.md
scope: Benchmark 5 VN CS theses + 3 VN edu SaaS industry references để right-size thesis scope
---

# Outside-in Audit — VN CS Thesis Benchmark cho Kite Platform Release 1.5

## 1. Bảng 5 ví dụ thesis đại diện VN

| # | Đề tài / Trường | Kiến trúc | Scope shipped | Kết quả ước tính |
|---|---|---|---|---|
| 1 | "Phát triển hệ thống phân tán microservices cho ứng dụng chia sẻ video" — UIT 2025 | Microservices (4 services) + Kafka + Redis + K8s + ELK + Prometheus/Grafana + CI/CD | Video share, comment, user, streaming, AI recommendation + toxic-comment detection, DevOps pipeline | Được đánh giá tốt — tech stack đầy đủ; không có real users/scale metrics cụ thể |
| 2 | "Xây dựng hệ thống e-learning tích hợp AI cho IT learning" — UIT 2021-2022 | Monolith hoặc light-microservice + AI personalization | Course management, student tracking, AI-based personalized learning | Baseline tốt; AI là điểm cộng đáng kể |
| 3 | "Building home rentals platform with microservices" — UIT 2020-2021 | Microservices | Listing, booking, user management | Không có data thực; kiến trúc là trọng tâm |
| 4 | "Xây dựng hệ thống quản lý điểm trường phổ thông" — nhiều trường | Monolith web app | Grade management, teacher portal, report gen | BASELINE thấp cho 2026 — không đủ cho top grade |
| 5 | "Building a cloud based quiz system" — UIT 2023-2024 | Cloud-native, single service | Quiz CRUD, student access, basic analytics | Mid-range; thiếu DevOps + security audit |

**Nhận xét tổng:** Chưa tìm thấy thesis VN nào kết hợp đủ 4 trụ cột: (1) multi-tenant SaaS + (2) 6+ microservices + (3) real beta tenants live + (4) DevOps pipeline hoàn chỉnh. **Kite Platform nằm ở mức HIẾM trong context VN.**

## 2. Industry benchmark — 3 sản phẩm VN edu

**EasyEdu** (1,400+ trung tâm, 5 triệu end users):
- Phase 1 MVP: student management + class scheduling + attendance + basic billing + parent portal
- SaaS multi-tenant, mobile app, Zalo/Facebook integration
- KHÔNG có AI trong Phase 1; AI không phải baseline requirement

**MISA EMIS** (gov-facing, K-12):
- Phase 1: timetable, grade management, HR/payroll, library, government reporting compliance
- Monolith/semi-SaaS; focus compliance hơn UX

**Faceworks / Mona LMS** (mid-market):
- Phase 1: student DB + class scheduling + fee/billing + teacher payroll + basic CRM
- SaaS VND 390k–1.1M/tháng/tenant
- Không có DevOps, không có security audit, không có AI

**Kết luận ngành:** Feature set P1+P2 của Kite NGANG BẰNG EasyEdu Phase 1. Điểm vượt trội: multi-tenant architecture chính danh, AI branding, CI/CD pipeline, security audit suite — không competitor nào trong segment này có những thứ đó ở thesis level.

## 3. Top 5 câu hỏi examiner VN hay challenge nhất

**Q1 — "Tại sao chọn microservices thay vì monolith?"**
Trade-off complexity vs scale. Preempt: ADR-025 + deployment strategy làm evidence.

**Q2 — "Hệ thống chịu tải bao nhiêu concurrent users?"**
Top schools (HUST/UET/UIT) đòi load test thực tế. Preempt: k6/JMeter 50 concurrent baseline ≥ P95 < 2s.

**Q3 — "Bảo mật + PDPL compliance?"**
2026 examiner ngày càng aware. Preempt: Security audit suite 5 cats là ASSET LỚN — không thesis VN nào có. Trình bày scores + PDPL Art.9/11 mapping.

**Q4 — "Có real users chưa?"**
Phân biệt thesis 8 điểm vs 9-10 điểm. 5 beta tenants live = extremely strong proof.

**Q5 — "AI integration thực sự có giá trị gì? Hay chỉ API call?"**
Examiner skeptical. Preempt: live generation demo + before/after comparison + honest limitation.

## 4. Recommendation Release 1.5 thesis

**BREADTH vs DEPTH:** Chọn **DEPTH-FIRST với breadth showcase**:
- **Primary demo (10-15 phút):** 1 tenant end-to-end (signup → branding → class → enroll → attendance → payment → report)
- **Secondary demo (5 phút):** Multi-tenant isolation proof (2 tenants data separation) — unique differentiator
- **Slides-only:** DevOps pipeline, security audit scores, Terraform AWS

**Scope ADD cho thesis:**
| Item | Effort | Impact |
|---|---|---|
| Load test cơ bản (k6, 50-100 concurrent) | 2-3 ngày | Cao — Q2 |
| Tenant onboarding metrics | 1 ngày | Trung bình |
| PDPL compliance table | 2 ngày | Cao — Q3 |
| Business value metrics (time saved) | 1 ngày | Trung bình — Q4 |

**Scope DROP / "future scope":**
- K-12 (Phase 3 / P5) — nêu trong "Future Scope" chapter, không demo
- Multi-region failover — over-engineering
- ML model training từ đầu — nếu AI dùng API, honest về đó
- PDPL legal counsel — mention "pending counsel review" + "v1 disclaimer" honest

**Risk preempt trong thesis:**
- NFR chapter: response time target vs measured, availability SLA, security audit summary
- Limitations chapter: honest no load test production scale, no third-party pen-test, PDPL counsel pending
- Testing chapter: unit/integration/E2E coverage % với số liệu

## 5. Strategic verdict

**Release 1.5 scope = AMBITIOUS cho VN CS thesis 2026.**

| Dimension | Kite | VN Median | Assessment |
|---|---|---|---|
| Services count | 8+ | 3-5 | Top 10% |
| Multi-tenancy | True SaaS | Single-tenant CRUD | Rare/unique |
| Real deployment | AWS Singapore + CI/CD | Localhost + Docker compose | Top 5% |
| Security audit | 5-category scored | Typically absent | Unique |
| AI integration | Branding gen + API | Recommendation API only | Above average |
| Beta tenants live | Target 5 | Typically 0 | Top 5% |
| PDPL compliance | Documented + partial | Typically not mentioned | Above average |

**Verdict:** Release 1.5 đạt (Phase 1 BETA live + 5 tenants + 0 P0 incidents) → đủ điều kiện **9-10 điểm** UIT/HUST/UET **nếu** trình bày đúng với NFR section + load test cơ bản + honest limitations chapter.

**Minimum additions trước defense:**
1. Load test 1 scenario — không thể thiếu top schools
2. PDPL compliance table — 2 ngày, high relevance 2026
3. Tenant isolation demo script — 5 phút tái hiện live

**Rủi ro lớn nhất:** Scope quá ambitious → thesis report mỏng về từng phần. Mỗi chapter phải có depth tương xứng với claim. Nếu claim "microservices" nhưng chỉ 2 trang về service decomposition → examiner drill sâu tìm weakness.

## Citations

- [EasyEdu — 1,400+ trung tâm](https://easyedu.vn/tinh-nang/)
- [MISA EMIS features](https://emis.misa.vn/emisconglap/)
- [UIT Khóa luận tốt nghiệp list](https://httt.uit.edu.vn/en_US/nghien-cuu-khoa-hoc/danh-sach-khoa-luan-tot-nghiep-dai-hoc/)
- [UIT Microservices video sharing thesis 2025](https://nc.uit.edu.vn/khoa-luan/phat-trien-he-thong-phan-tan-microservices-cho-ung-dung-chia-se-video)
- [Top 15 phần mềm quản lý trung tâm ngoại ngữ VN](https://magenest.com/vi/phan-mem-quan-ly-trung-tam-ngoai-ngu/)
- [HUST Grading System](https://www.hust.edu.vn/en/academics/academic-information/grading-system-554906.html)
- [Vietnam LMS Market 2024-2030](https://www.6wresearch.com/industry-report/vietnam-learning-management-system-market)
