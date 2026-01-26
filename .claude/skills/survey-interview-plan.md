# Skill: Survey & Interview Plan

Kế hoạch khảo sát và phỏng vấn các actors cho dự án KiteClass Platform V3.1.

## Mô tả

Tài liệu hướng dẫn thu thập yêu cầu từ stakeholders:
- Actor mapping và phân loại
- Kế hoạch khảo sát online (Google Forms)
- Kế hoạch phỏng vấn sâu (Discovery, Validation, Usability)
- Mẫu câu hỏi chi tiết theo từng actor
- Phương pháp phân tích kết quả

## Trigger phrases

- "khảo sát người dùng"
- "phỏng vấn"
- "survey"
- "interview"
- "thu thập yêu cầu"

## Files

| File | Path |
|------|------|
| Tài liệu chính | `documents/plans/survey-interview-plan.md` |

## Actors

### KiteHub Actors
| Actor | Mô tả |
|-------|-------|
| CUSTOMER | Chủ trung tâm - người mua gói |
| ADMIN | Quản lý hệ thống KiteHub |
| AGENT | Nhân viên hỗ trợ khách hàng |

### KiteClass Instance Actors
| Actor | Mô tả | Ưu tiên khảo sát |
|-------|-------|------------------|
| CENTER_OWNER | Chủ trung tâm, toàn quyền | P0 - Cao nhất |
| CENTER_ADMIN | Quản trị viên vận hành | P0 - Cao nhất |
| TEACHER | Giáo viên giảng dạy | P1 - Cao |
| STUDENT | Học viên | P1 - Cao |
| PARENT | Phụ huynh theo dõi | P1 - Cao |

## Survey Phases

### Phase 1: Khảo sát online (2 tuần)
- Công cụ: Google Forms / Typeform
- Đối tượng: Tất cả actors
- Mục tiêu: 200+ responses

### Phase 2: Khảo sát chuyên sâu (1 tuần)
- Đối tượng: Respondents đồng ý từ Phase 1
- Mục tiêu: 50+ responses

### Phase 3: Phỏng vấn trực tiếp (2 tuần)
- Hình thức: Video call / Gặp trực tiếp
- Đối tượng: Key stakeholders
- Mục tiêu: 15-20 interviews (30-45 phút)

## Interview Types

| Loại | Mục đích | Thời lượng |
|------|----------|------------|
| Discovery | Hiểu context, workflow hiện tại | 30 phút |
| Validation | Xác nhận assumptions, test prototype | 45 phút |
| Usability | Test UI/UX với mockup | 60 phút |

## Key Questions by Actor

### CENTER_OWNER
- Quy mô trung tâm, lĩnh vực giảng dạy
- Công cụ quản lý hiện tại
- 3 vấn đề lớn nhất
- Mức độ quan trọng các tính năng
- Sẵn sàng chi trả bao nhiêu/tháng

### TEACHER
- Số lớp/tuần, số học viên/lớp
- Cách điểm danh, theo dõi tiến độ
- Giao bài tập, chấm điểm
- Tương tác với phụ huynh

### PARENT
- Cách theo dõi con
- Phương thức thanh toán
- Kênh nhận thông báo ưu tiên
- Nhu cầu cổng thông tin phụ huynh

## Incentives

| Actor | Kênh tiếp cận | Incentive |
|-------|---------------|-----------|
| CENTER_OWNER | Email trực tiếp | 1 tháng dùng thử miễn phí |
| CENTER_ADMIN | Email qua Owner | Voucher 200k |
| TEACHER | Email từ Admin | Voucher 100k |
| STUDENT | In-app survey | Điểm thưởng gamification |
| PARENT | Zalo notification | Voucher 100k |

## Actions

### Xem mẫu phiếu khảo sát
Đọc phần "5. Mẫu phiếu khảo sát" trong tài liệu.

### Xem kịch bản phỏng vấn
Đọc phần "6. Kịch bản phỏng vấn" cho scripts chi tiết.

### Phương pháp phân tích
Đọc phần "7. Phương pháp phân tích kết quả" cho quy trình và templates.
