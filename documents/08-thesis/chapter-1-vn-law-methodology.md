---
title: Chương 1 §1.2.11 — Phạm vi đề tài và lộ trình triển khai (sub của §1.2 Bài toán)
chapter: 1
section: scope-roadmap
audience: mixed
last-updated: 2026-05-26
status: draft
---

# Chương 1 — Phạm vi đề tài và lộ trình triển khai

### 1.2.11 Phạm vi đề tài và lộ trình triển khai

Đề tài chia thành các giai đoạn triển khai để cân bằng giữa độ phức tạp kỹ thuật, ngưỡng tuân thủ pháp lý và quy mô thị trường mục tiêu. Giai đoạn thử nghiệm tenant gồm vận hành thử với hai giáo viên độc lập (một dùng gói miễn phí, một dùng gói premium), tập trung kiểm chứng onboarding wizard, AI Branding, kiến trúc multi-tenant và compliance baseline; phạm vi triển khai trong giai đoạn này không thu phí và chấp nhận chính sách giảm thiểu chi phí hạ tầng. Giai đoạn thanh toán thử nghiệm mở rộng tenant cohort lên 30-50 trung tâm với chính sách thu phí giới thiệu thấp hơn giá niêm yết, mục đích chính là validation pricing model, onboarding flow chính thức và hệ thống thanh toán chuyển khoản kèm đối soát tự động; giai đoạn này tích hợp eInvoice qua partnership với nhà cung cấp được Tổng cục Thuế cấp phép. Giai đoạn vận hành chính thức mở cho mọi trung tâm tự đăng ký không qua quy trình mời, đi kèm SLA cam kết uptime ≥99,5% và bổ sung các kênh hỗ trợ (live chat, Zalo OA support), ứng dụng di động native cho học viên và phụ huynh, đa cổng thanh toán tích hợp. Giai đoạn K-12 expansion mở rộng sang phân khúc trường công lập và tư thục cấp 1-2 với yêu cầu bổ nhiệm DPO chính thức, DPIA cho dữ liệu trẻ em theo Luật Bảo vệ Dữ liệu Cá nhân Điều 17 và Điều 26, hợp tác với Bộ Giáo dục và Đào tạo cùng các sở/phòng giáo dục địa phương cho phân phối.

Phạm vi triển khai của đề tài chủ yếu tập trung vào giai đoạn thử nghiệm tenant readiness — bao gồm core onboarding wizard, AI Branding tự động, quản lý lifecycle tenant đầy đủ, và compliance pháp luật Việt Nam (PDPL, Cybersecurity, eInvoice baseline). Các giai đoạn thanh toán thử nghiệm, vận hành chính thức và K-12 expansion được trình bày làm roadmap kỹ thuật nhưng không nằm trong phạm vi triển khai thực tế của đề tài. Việc tách giai đoạn này phù hợp với phương pháp luận MVP (Minimum Viable Product) trong khởi nghiệp phần mềm — ship sản phẩm tối giản đáp ứng nhu cầu cốt lõi của persona ưu tiên trước, sau đó mở rộng dần các tính năng nâng cao theo phản hồi thị trường.
