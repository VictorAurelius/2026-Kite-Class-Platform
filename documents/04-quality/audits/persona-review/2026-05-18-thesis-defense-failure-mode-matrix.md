---
title: Outside-in Audit — Thesis Defense Failure-Mode Matrix (4 Examiner Archetypes)
status: complete
created: 2026-05-18
audit_type: failure-mode-simulation
agent_model: sonnet-4-6
trigger: Release 1.5 thesis scope decision per outside-in-coverage-trigger.md
scope: Role-play 4 examiner archetypes × 5 questions = 20 challenges Kite Platform thesis defense
---

# Outside-in Audit — Thesis Defense Failure-Mode Matrix

**Mục tiêu:** Mô phỏng hội đồng chấm luận văn — 4 persona examiners challenge Kite Platform với failure modes, evidence audit, scope additions cần thiết trước defense window 2026-08-15 → 2026-10-15.

## Examiner A — Architecture Hawk (Kiến trúc)

| # | Câu hỏi | Evidence | Scope bổ sung | Severity |
|---|---------|----------|---------------|----------|
| A1 | "Multi-tenant isolation strategy là gì? Database-per-tenant hay shared schema? Tại sao chọn cách này?" | ✅ PostgreSQL RLS documented, database-per-tenant KiteClass documented trong deployment-strategy.md | Bổ sung Chapter 2 §Kiến trúc multi-tenant: bảng so sánh 3 chiến lược (db-per-tenant / shared-schema / hybrid) trade-off chi phí + isolation level | P1 |
| A2 | "Service Registry -95% overhead — em đo bằng cách nào, số liệu từ đâu?" | ⚠️ Con số đề cập nhưng thiếu benchmark code + profiling artifacts | Thêm Appendix: micro-benchmark JMH hoặc Gatling load test trước/sau Service Registry removal, export CSV + chart | P0 |
| A3 | "API Gateway xử lý rate limit theo tenant_id thế nào? Circuit breaker?" | ⚠️ Gateway exists, GAP-637 @PreAuthorize missing chưa fix | Fix GAP-637 trước defense; bổ sung sequence diagram gateway → service auth flow Chapter 2 | P0 |
| A4 | "Em chọn AWS Singapore lý do gì? Latency benchmark so với Oracle Cloud có chưa?" | ⚠️ ADR-025 exists nhưng không có latency data thực tế | Thêm AWS CloudWatch p50/p95 latency dashboard screenshot (≥30 ngày data) Chapter 4 | P1 |
| A5 | "Modular monolith + microservices hybrid — ranh giới module theo tiêu chí nào?" | ✅ Deployment-strategy.md có 5 nguyên tắc | Bổ sung Chapter 2 §Domain Boundary: bounded context diagram (Mermaid) 6 service → bounded domain | P2 |

## Examiner B — NFR/Quality Auditor (Chất lượng phi chức năng)

| # | Câu hỏi | Evidence | Scope bổ sung | Severity |
|---|---------|----------|---------------|----------|
| B1 | "Performance 86/100 — 4 điểm thiếu là gì? Plan fix trước GA?" | ✅ Performance audit 86/100 B+ documented chi tiết delta | Thêm Chapter 4: audit score evolution (Wave 54 → 85 → target GA), gap còn lại + action plan | P1 |
| B2 | "Security 93/100 — 7 điểm thiếu, P2 findings GAP-642/643/644 xử lý thế nào?" | ✅ Security audit 93/100 A với 27/27 evidence blocks v2 format | Thêm Chapter 4 §Bảo mật: OWASP Top 10 mapping table, 3 P2 + disposition (accepted risk / backlog) | P1 |
| B3 | "Business Logic 70/100 C — FAIL. Hội đồng đánh giá thế nào?" | ❌ Điểm yếu nghiêm trọng nhất; GAP-639/640 chưa đóng | Fix GAP-639 ABORTED enum + GAP-640 admin-audit 3-layer TRƯỚC defense; Chapter 4 framing "tiến trình cải tiến" với trajectory chart | P0 |
| B4 | "API Contract 79/100 FAIL — GAP-637 @PreAuthorize missing, OWASP A01 broken access control. Giải thích?" | ❌ FAIL audit level với 3 P0 sub-checks | GAP-637 fix xong trước tag release defense; không thể demo broken access control trước hội đồng | P0 |
| B5 | "65+ rules, 200+ audits — workflow áp dụng nhất quán không? Prove?" | ✅ CI pipeline documented, script-quality.yml, audit artifacts committed | Thêm Chapter 3 §Quy trình đảm bảo chất lượng: flowchart audit pipeline, sample CI output screenshot | P2 |

## Examiner C — Business/Product (Kinh doanh/Sản phẩm)

| # | Câu hỏi | Evidence | Scope bổ sung | Severity |
|---|---------|----------|---------------|----------|
| C1 | "5 beta tenants — họ là ai? Feedback cụ thể? Họ tiếp tục dùng sau beta?" | ❌ Chưa có ≥4 signed beta-user reviews | Beta cohort execution: mời ≥2 GV trial + ≥2 GV VIP, thu signed feedback (tên + trung tâm + ngày ký) → Phụ lục | P0 |
| C2 | "So với BeeClass, Misa, EduFit — KiteClass superior ở điểm nào? Pricing?" | ❌ Chapter 1 thiếu competitor analysis | Thêm Chapter 1 §Phân tích thị trường: bảng so sánh 5 đối thủ (tính năng / multi-tenant / AI / pricing), positioning | P0 |
| C3 | "PDPL 2023 — xử lý data subject rights (Art.9, Art.11) thế nào trong product?" | ⚠️ PDPL đề cập nhưng implementation evidence thin | Thêm Chapter 2 §Tuân thủ pháp lý: mapping PDPL Art.9/11 → feature (consent flow, audit log immutable V54) | P1 |
| C4 | "AI branding $0.19/instance — ROI? Tenant có sẵn sàng trả thêm tiền?" | ⚠️ Cost evidence có nhưng willingness-to-pay chưa validated | Bổ sung Chapter 4 §Đánh giá kinh tế: cost breakdown $0.19 component-level, beta survey "Would pay extra?" | P1 |
| C5 | "Personas P1/P2/P3 — acceptance criteria validated chưa?" | ⚠️ Personas defined nhưng formal AC validation chưa documented | Thêm Appendix: AC matrix per persona (CSV format), kết quả từ beta walkthrough session | P2 |

## Examiner D — AI/Modern Tech (Công nghệ AI)

| # | Câu hỏi | Evidence | Scope bổ sung | Severity |
|---|---------|----------|---------------|----------|
| D1 | "Stable Diffusion XL + GPT-4 pipeline — prompt engineering thế nào? Quality control?" | ⚠️ Pipeline documented nhưng prompt templates + quality gate criteria chưa publish | Thêm Appendix: sample prompts (3 styles), output quality rubric, reject rate statistics production | P1 |
| D2 | "AI-generated assets có WCAG AA compliance không? Test thế nào?" | ⚠️ AI branding quality gate 62/100 baseline (Wave 4 scaffold) | Thêm Chapter 4 §Đánh giá AI: contrast ratio test 3 sample generated assets, WCAG checker screenshot | P1 |
| D3 | "So sánh approach với RAG, fine-tuning, multi-modal LLaVA không?" | ❌ Không có literature review AI technique comparison | Bổ sung Chapter 1 §Công nghệ AI: bảng so sánh 4 approaches (prompt-eng / fine-tuning / RAG / multi-modal) rationale chọn GPT-4 + SD-XL | P0 |
| D4 | "Remove.bg API dependency — vendor tăng giá hoặc outage, system thế nào?" | ⚠️ Dependency documented nhưng fallback strategy không rõ | Thêm Chapter 2 §Thiết kế AI Agent: fallback diagram (Remove.bg down → transparent fallback → notify tenant) | P2 |
| D5 | "2026, vì sao không dùng GPT-4o Vision phân tích branding hiện có suggest themes?" | ❌ Câu hỏi về future scope — cần defensive answer | Chuẩn bị slide: "Scope Phase 1 = generation from text prompts. Vision-based = Phase 2 roadmap" | P3 |

## Top 10 P0 Thesis-Blockers (Aggregate)

| Rank | Vấn đề | Action ngay |
|------|--------|-------------|
| 1 | API Contract 79/100 FAIL — GAP-637 @PreAuthorize missing | Fix code trước release tag defense |
| 2 | Business Logic 70/100 FAIL — GAP-639/640 chưa đóng | Fix 2 findings → +8 pts → ≥78 |
| 3 | Beta cohort ≥4 signed reviews — chưa có | Mời + thu signed feedback NGAY |
| 4 | Chapter 1 literature review hoàn toàn thiếu | Viết competitor table + AI theory + VN law section |
| 5 | IEEE citations hoàn toàn vắng trong draft v3.1 | Retroactive cite toàn bộ theo [1][2][3] format |
| 6 | Service Registry -95% overhead không có benchmark artifact | Chạy micro-benchmark + export data Appendix |
| 7 | Chapter 4 không có concrete metrics (p50/p95, AWS bill) | Export CloudWatch screenshots + AWS Cost Explorer |
| 8 | Competitor analysis BeeClass/Misa/EduFit không có | Bảng so sánh 5 đối thủ Chapter 1 |
| 9 | AI technique comparison RAG/fine-tuning/multi-modal thiếu | Bảng so sánh 4 approaches Chapter 1 §AI |
| 10 | v2.0.0 stable ≥2 tuần trước defense — chưa tag | Lock feature freeze sớm, tag stable release |

## Top 5 P1 Score-Impacting

1. AWS CloudWatch p50/p95 dashboard screenshot ≥30 ngày → Chapter 4
2. PDPL Art.9/11 → feature mapping table → Chapter 2
3. AI branding ROI breakdown + beta willingness-to-pay survey → Chapter 4
4. Security OWASP Top 10 mapping + 3 P2 GAP disposition → Chapter 4
5. Bounded context diagram 6 service → Chapter 2

## Top 5 Nên Xóa/Thu Gọn Khỏi Luận Văn

1. **Wave/GAP/bucket internal terminology** — hội đồng không hiểu, thay bằng "sprint", "yêu cầu", "module"
2. **65+ rules detail** — chỉ cần đề cập methodology, không list từng rule
3. **Chi tiết CI cleanup policy** (50-run cap) — quá micro
4. **Raw audit JSON/CSV artifacts** — chỉ summary table + chart
5. **Internal tooling comparisons** (MCP vs CLI, Bash tier hierarchy) — dev process internals không thuộc thesis

## Strategic Preempt — 3 Narrative Moves

**Move 1 — "Evidence-first defense":** Mở Chapter 4 bằng audit score dashboard (6 dimensions, Wave progression chart). 93/100 Security + 86/100 Performance → tone tích cực. Business Logic 70/100 framed "phát hiện sớm qua audit pipeline, action plan rõ ràng" — chủ động thừa nhận > giấu.

**Move 2 — "Complexity moat":** Chapter 1 §Phân tích thị trường bảng benchmark 5 đối thủ. KiteClass duy nhất trong cohort K63 có: AWS production deploy + multi-tenant PostgreSQL RLS + AI Agent pipeline + modular monolith hybrid + audit-driven development. Differentiator: "quyết định thiết kế có căn cứ đo lường".

**Move 3 — "Beta validates market fit":** ≥4 signed beta reviews (tên thật + trung tâm + nhận xét cụ thể) đầu Chapter 4. Framing: "Hệ thống không chỉ chạy được — người dùng thực đã dùng và có ý kiến." Phân biệt với thesis "demo chỉ chạy trên máy thầy".
