---
title: Chương 1 §1.2.11 — Phạm vi đề tài và lộ trình triển khai (sub của §1.2 Bài toán)
chapter: 1
section: scope-roadmap
audience: mixed
last-updated: 2026-05-26
status: draft
---

# Chương 1 — Phạm vi đề tài và lộ trình triển khai

### 1.3.3 Phạm vi đề tài và lộ trình triển khai

Đề tài chia thành các giai đoạn triển khai để cân bằng giữa độ phức tạp kỹ thuật, ngưỡng tuân thủ pháp lý và quy mô thị trường mục tiêu. Giai đoạn thử nghiệm tenant gồm vận hành thử với hai giáo viên độc lập (một dùng gói miễn phí, một dùng gói trả phí), tập trung kiểm chứng trình hướng dẫn khởi tạo tenant, mô-đun AI Branding, kiến trúc đa tenant và ngưỡng tuân thủ pháp lý cơ bản; phạm vi triển khai trong giai đoạn này không thu phí và chấp nhận chính sách giảm thiểu chi phí hạ tầng. Giai đoạn thanh toán thử nghiệm mở rộng nhóm tenant thử nghiệm lên 30-50 trung tâm với chính sách thu phí giới thiệu thấp hơn giá niêm yết, mục đích chính là kiểm chứng mô hình giá, luồng khởi tạo chính thức và hệ thống thanh toán chuyển khoản kèm đối soát tự động; giai đoạn này tích hợp hóa đơn điện tử (eInvoice) qua hợp tác với nhà cung cấp được Tổng cục Thuế cấp phép. Giai đoạn vận hành chính thức mở cho mọi trung tâm tự đăng ký không qua quy trình mời, đi kèm thỏa thuận mức dịch vụ (SLA — Service Level Agreement) cam kết thời gian hoạt động ≥99,5% và bổ sung các kênh hỗ trợ (trò chuyện trực tuyến, kênh hỗ trợ Zalo OA), ứng dụng di động gốc cho học viên và phụ huynh, đa cổng thanh toán tích hợp. Giai đoạn mở rộng sang khối phổ thông K-12 phục vụ trường công lập và tư thục cấp 1-2 với yêu cầu bổ nhiệm cán bộ bảo vệ dữ liệu (DPO — Data Protection Officer) chính thức, đánh giá tác động bảo vệ dữ liệu (DPIA — Data Protection Impact Assessment) cho dữ liệu trẻ em theo Luật Bảo vệ Dữ liệu Cá nhân Điều 17 và Điều 26, hợp tác với Bộ Giáo dục và Đào tạo cùng các sở phòng giáo dục địa phương cho phân phối.

Phạm vi triển khai của đề tài chủ yếu tập trung vào giai đoạn thử nghiệm tenant — bao gồm trình hướng dẫn khởi tạo cốt lõi, mô-đun AI Branding tự động, quản lý vòng đời tenant đầy đủ và tuân thủ pháp luật Việt Nam (PDPL, Luật An ninh mạng, ngưỡng hóa đơn điện tử). Các giai đoạn thanh toán thử nghiệm, vận hành chính thức và mở rộng sang khối K-12 được trình bày như lộ trình kỹ thuật nhưng không nằm trong phạm vi triển khai thực tế của đề tài. Việc tách giai đoạn này phù hợp với phương pháp luận sản phẩm tối giản khả thi (MVP — Minimum Viable Product) trong khởi nghiệp phần mềm — phát hành sản phẩm tối giản đáp ứng nhu cầu cốt lõi của nhóm người dùng đại diện ưu tiên trước, sau đó mở rộng dần các tính năng nâng cao theo phản hồi thị trường.
