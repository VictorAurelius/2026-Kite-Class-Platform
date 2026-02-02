# KẾ HOẠCH KHẢO SÁT VÀ PHỎNG VẤN ACTORS
## KiteClass Platform V3.1

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Dự án** | KiteClass Platform V3.1 |
| **Loại tài liệu** | Survey & Interview Plan |
| **Ngày tạo** | 23/12/2025 |
| **Mục đích** | Thu thập yêu cầu từ các stakeholders |

---

# MỤC LỤC

1. [Tổng quan các Actors](#1-tổng-quan-các-actors)
2. [Kế hoạch khảo sát (Survey)](#2-kế-hoạch-khảo-sát-survey)
3. [Kế hoạch phỏng vấn (Interview)](#3-kế-hoạch-phỏng-vấn-interview)
4. [Bảng câu hỏi chi tiết theo Actor](#4-bảng-câu-hỏi-chi-tiết-theo-actor)
5. [Mẫu phiếu khảo sát](#5-mẫu-phiếu-khảo-sát)
6. [Kịch bản phỏng vấn](#6-kịch-bản-phỏng-vấn)
7. [Phương pháp phân tích kết quả](#7-phương-pháp-phân-tích-kết-quả)

---

# 1. TỔNG QUAN CÁC ACTORS

## 1.1. Ma trận Actors

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ACTORS MAPPING                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         KITEHUB ACTORS                                   │   │
│  │                                                                          │   │
│  │   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                 │   │
│  │   │  CUSTOMER   │    │   ADMIN     │    │   AGENT     │                 │   │
│  │   │ (Chủ TT)    │    │ (KiteHub)   │    │ (Hỗ trợ)   │                 │   │
│  │   │             │    │             │    │             │                 │   │
│  │   │ Mua gói     │    │ Quản lý     │    │ Chat hỗ trợ │                 │   │
│  │   │ Quản lý TT  │    │ Hệ thống    │    │ Tư vấn      │                 │   │
│  │   └─────────────┘    └─────────────┘    └─────────────┘                 │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                      KITECLASS INSTANCE ACTORS                           │   │
│  │                                                                          │   │
│  │   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                 │   │
│  │   │CENTER_OWNER │    │CENTER_ADMIN │    │  TEACHER    │                 │   │
│  │   │ (Chủ TT)    │    │ (Quản trị)  │    │ (Giáo viên) │                 │   │
│  │   │             │    │             │    │             │                 │   │
│  │   │ Toàn quyền  │    │ Vận hành    │    │ Giảng dạy   │                 │   │
│  │   │ Tài chính   │    │ Nhân sự     │    │ Điểm danh   │                 │   │
│  │   └─────────────┘    └─────────────┘    └─────────────┘                 │   │
│  │                                                                          │   │
│  │   ┌─────────────┐    ┌─────────────┐                                    │   │
│  │   │  STUDENT    │    │   PARENT    │                                    │   │
│  │   │ (Học viên)  │    │ (Phụ huynh) │                                    │   │
│  │   │             │    │             │                                    │   │
│  │   │ Học tập     │    │ Theo dõi    │                                    │   │
│  │   │ Bài tập     │    │ Thanh toán  │                                    │   │
│  │   └─────────────┘    └─────────────┘                                    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. Bảng thông tin Actors

| Actor | Mô tả | Số lượng ước tính | Độ ưu tiên khảo sát |
|-------|-------|-------------------|---------------------|
| **CENTER_OWNER** | Chủ trung tâm, người ra quyết định mua | 50-100 | P0 - Cao nhất |
| **CENTER_ADMIN** | Quản trị viên vận hành hàng ngày | 100-200 | P0 - Cao nhất |
| **TEACHER** | Giáo viên trực tiếp giảng dạy | 500-1000 | P1 - Cao |
| **STUDENT** | Học viên sử dụng hệ thống | 5000-10000 | P1 - Cao |
| **PARENT** | Phụ huynh theo dõi con em | 3000-6000 | P1 - Cao |
| **KITEHUB_ADMIN** | Admin hệ thống KiteHub | 5-10 | P2 - Trung bình |
| **AGENT** | Nhân viên hỗ trợ khách hàng | 10-20 | P2 - Trung bình |

---

# 2. KẾ HOẠCH KHẢO SÁT (SURVEY)

## 2.1. Mục tiêu khảo sát

| STT | Mục tiêu | Đo lường bằng |
|-----|----------|---------------|
| 1 | Xác định pain points hiện tại | Số lượng vấn đề được nêu |
| 2 | Đánh giá mức độ quan trọng của tính năng | Điểm 1-5 cho mỗi feature |
| 3 | Hiểu workflow làm việc hàng ngày | Mô tả quy trình |
| 4 | Thu thập yêu cầu mới | Danh sách features mong muốn |
| 5 | Đánh giá sẵn sàng chi trả | Mức giá chấp nhận được |

## 2.2. Phương pháp khảo sát

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PHƯƠNG PHÁP KHẢO SÁT                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  PHASE 1: KHẢO SÁT ONLINE (2 tuần)                                               │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  • Công cụ: Google Forms / Typeform                                              │
│  • Đối tượng: Tất cả actors                                                      │
│  • Mục tiêu: 200+ responses                                                      │
│  • Nội dung: Câu hỏi đóng + mở                                                   │
│                                                                                  │
│  PHASE 2: KHẢO SÁT CHUYÊN SÂU (1 tuần)                                           │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  • Công cụ: Google Forms với logic branching                                     │
│  • Đối tượng: Respondents từ Phase 1 đồng ý tham gia                            │
│  • Mục tiêu: 50+ responses                                                       │
│  • Nội dung: Câu hỏi chi tiết về workflow                                        │
│                                                                                  │
│  PHASE 3: PHỎNG VẤN TRỰC TIẾP (2 tuần)                                           │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  • Hình thức: Video call / Gặp trực tiếp                                         │
│  • Đối tượng: Key stakeholders (CENTER_OWNER, CENTER_ADMIN)                      │
│  • Mục tiêu: 15-20 interviews                                                    │
│  • Thời lượng: 30-45 phút/interview                                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.3. Timeline khảo sát

| Tuần | Hoạt động | Output |
|------|-----------|--------|
| **Tuần 1** | Thiết kế bảng khảo sát, pilot test | Bảng câu hỏi hoàn chỉnh |
| **Tuần 2-3** | Phát khảo sát online Phase 1 | 200+ responses |
| **Tuần 4** | Phân tích sơ bộ, thiết kế Phase 2 | Báo cáo sơ bộ |
| **Tuần 5** | Khảo sát chuyên sâu Phase 2 | 50+ responses chi tiết |
| **Tuần 6-7** | Phỏng vấn trực tiếp | 15-20 interview transcripts |
| **Tuần 8** | Tổng hợp và báo cáo | Báo cáo khảo sát hoàn chỉnh |

## 2.4. Kênh tiếp cận

| Actor | Kênh chính | Kênh phụ | Incentive |
|-------|-----------|----------|-----------|
| CENTER_OWNER | Email trực tiếp | LinkedIn, Zalo | 1 tháng dùng thử miễn phí |
| CENTER_ADMIN | Email qua Owner | Zalo Group | Voucher 200k |
| TEACHER | Email từ Admin | Facebook Group | Voucher 100k |
| STUDENT | In-app survey | Zalo, Email | Điểm thưởng gamification |
| PARENT | Zalo notification | Email | Voucher 100k |

---

# 3. KẾ HOẠCH PHỎNG VẤN (INTERVIEW)

## 3.1. Phân loại phỏng vấn

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         CÁC LOẠI PHỎNG VẤN                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. DISCOVERY INTERVIEW (Khám phá)                                               │
│     ────────────────────────────────                                             │
│     • Mục đích: Hiểu context, workflow hiện tại                                  │
│     • Đối tượng: Tất cả actors                                                   │
│     • Thời lượng: 30 phút                                                        │
│     • Câu hỏi: Mở, không định hướng                                              │
│                                                                                  │
│  2. VALIDATION INTERVIEW (Xác nhận)                                              │
│     ────────────────────────────────                                             │
│     • Mục đích: Xác nhận assumptions, test prototype                             │
│     • Đối tượng: Key users từ Discovery                                          │
│     • Thời lượng: 45 phút                                                        │
│     • Câu hỏi: Cụ thể về features                                                │
│                                                                                  │
│  3. USABILITY INTERVIEW (Khả dụng)                                               │
│     ────────────────────────────────                                             │
│     • Mục đích: Test UI/UX với mockup/prototype                                  │
│     • Đối tượng: Representative users                                            │
│     • Thời lượng: 60 phút                                                        │
│     • Phương pháp: Think-aloud, task completion                                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 3.2. Ma trận phỏng vấn theo Actor

| Actor | Discovery | Validation | Usability | Tổng |
|-------|:---------:|:----------:|:---------:|:----:|
| CENTER_OWNER | 5 | 3 | 2 | **10** |
| CENTER_ADMIN | 5 | 3 | 2 | **10** |
| TEACHER | 3 | 2 | 2 | **7** |
| STUDENT | 3 | 2 | 2 | **7** |
| PARENT | 3 | 2 | 2 | **7** |
| **Tổng** | **19** | **12** | **10** | **41** |

## 3.3. Tiêu chí chọn người phỏng vấn

### CENTER_OWNER / CENTER_ADMIN

| Tiêu chí | Yêu cầu |
|----------|---------|
| Quy mô trung tâm | Đa dạng: Nhỏ (<50 HV), Vừa (50-200), Lớn (>200) |
| Loại hình | Ngoại ngữ, Toán/Lý/Hóa, Kỹ năng mềm, Âm nhạc |
| Kinh nghiệm CNTT | Từ thấp đến cao |
| Vị trí địa lý | TP.HCM, Hà Nội, các tỉnh |
| Đang dùng phần mềm | Có/Không có kinh nghiệm với LMS |

### TEACHER

| Tiêu chí | Yêu cầu |
|----------|---------|
| Môn giảng dạy | Đa dạng chuyên môn |
| Kinh nghiệm | Junior (1-3 năm), Senior (>5 năm) |
| Thế hệ | Gen Z, Millennial, Gen X |
| Thiết bị | Chủ yếu mobile / Desktop |

### STUDENT

| Tiêu chí | Yêu cầu |
|----------|---------|
| Độ tuổi | Tiểu học, THCS, THPT, Đại học, Người đi làm |
| Thiết bị | Mobile-first / Desktop-first |
| Mức độ tự học | Chủ động / Cần nhắc nhở |

### PARENT

| Tiêu chí | Yêu cầu |
|----------|---------|
| Số con đang học | 1 / 2+ |
| Mức độ quan tâm | Cao / Trung bình |
| Khả năng CNTT | Từ thấp đến cao |

---

# 4. BẢNG CÂU HỎI CHI TIẾT THEO ACTOR

## 4.1. CENTER_OWNER - Chủ trung tâm

### A. Thông tin chung

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| A1 | Trung tâm của bạn hoạt động bao lâu rồi? | Single choice | Phân loại |
| A2 | Quy mô trung tâm (số học viên)? | Single choice | Phân loại |
| A3 | Lĩnh vực giảng dạy chính? | Multiple choice | Phân loại |
| A4 | Số lượng nhân viên/giáo viên? | Number | Context |

### B. Pain Points hiện tại

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| B1 | Bạn đang quản lý trung tâm bằng công cụ gì? | Multiple choice | Hiểu hiện trạng |
| B2 | 3 vấn đề lớn nhất trong quản lý hiện tại? | Open text | Pain points |
| B3 | Thời gian trung bình dành cho công việc hành chính/ngày? | Single choice | Pain points |
| B4 | Đánh giá mức độ khó khăn các công việc (1-5): | Matrix | Ưu tiên features |
| | - Quản lý học phí, công nợ | | |
| | - Điểm danh, theo dõi học viên | | |
| | - Trao đổi với phụ huynh | | |
| | - Quản lý lịch học, phòng học | | |
| | - Báo cáo, thống kê | | |

### C. Yêu cầu tính năng

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| C1 | Đánh giá mức độ quan trọng các tính năng (1-5): | Matrix | Ưu tiên |
| | - Quản lý học phí tự động | | |
| | - Thanh toán online (QR, ví điện tử) | | |
| | - Thông báo tự động cho phụ huynh | | |
| | - Hệ thống điểm thưởng (Gamification) | | |
| | - Cổng thông tin phụ huynh | | |
| | - Video bài giảng online | | |
| | - Báo cáo doanh thu, tài chính | | |
| C2 | Tính năng nào bạn mong muốn nhưng chưa được liệt kê? | Open text | Features mới |
| C3 | Bạn sẵn sàng chi trả bao nhiêu/tháng cho phần mềm quản lý? | Single choice | Pricing |

### D. Kỳ vọng

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| D1 | Nếu có 1 phần mềm lý tưởng, bạn kỳ vọng tiết kiệm bao nhiêu % thời gian? | Single choice | Expectation |
| D2 | Yếu tố quan trọng nhất khi chọn phần mềm? | Ranking | Decision factors |
| | - Giá cả | | |
| | - Tính năng đầy đủ | | |
| | - Dễ sử dụng | | |
| | - Hỗ trợ kỹ thuật | | |
| | - Bảo mật dữ liệu | | |

---

## 4.2. CENTER_ADMIN - Quản trị viên

### A. Công việc hàng ngày

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| A1 | Mô tả một ngày làm việc điển hình của bạn? | Open text | Workflow |
| A2 | Bạn dùng bao nhiêu công cụ/phần mềm mỗi ngày? | Number | Context |
| A3 | Công việc nào tốn thời gian nhất? | Ranking | Pain points |

### B. Quản lý học viên

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| B1 | Quy trình nhập học viên mới như thế nào? | Open text | Workflow |
| B2 | Thời gian trung bình để nhập 1 học viên? | Single choice | Efficiency |
| B3 | Khó khăn lớn nhất khi quản lý hồ sơ học viên? | Open text | Pain points |
| B4 | Bạn theo dõi công nợ học phí bằng cách nào? | Multiple choice | Current solution |

### C. Lịch học và điểm danh

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| C1 | Làm thế nào để sắp xếp lịch học tránh trùng? | Open text | Workflow |
| C2 | Tần suất thay đổi lịch học/tuần? | Single choice | Frequency |
| C3 | Điểm danh được thực hiện như thế nào? | Single choice | Current solution |
| C4 | Thông tin điểm danh được gửi cho phụ huynh như thế nào? | Multiple choice | Communication |

### D. Báo cáo

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| D1 | Bạn cần làm báo cáo gì định kỳ? | Multiple choice | Requirements |
| D2 | Thời gian làm báo cáo hàng tháng? | Single choice | Pain points |
| D3 | Báo cáo nào quan trọng nhất với ban lãnh đạo? | Ranking | Priority |

---

## 4.3. TEACHER - Giáo viên

### A. Giảng dạy

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| A1 | Bạn dạy bao nhiêu lớp/tuần? | Number | Context |
| A2 | Số học viên trung bình/lớp? | Single choice | Context |
| A3 | Bạn chuẩn bị bài giảng như thế nào? | Open text | Workflow |
| A4 | Có sử dụng công nghệ trong giảng dạy không? | Multiple choice | Tech adoption |

### B. Điểm danh và theo dõi

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| B1 | Bạn điểm danh bằng cách nào hiện tại? | Single choice | Current solution |
| B2 | Thời gian dành cho điểm danh mỗi buổi? | Single choice | Efficiency |
| B3 | Bạn theo dõi tiến độ học viên như thế nào? | Open text | Workflow |
| B4 | Khó khăn lớn nhất trong việc theo dõi học viên? | Open text | Pain points |

### C. Giao bài tập và chấm điểm

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| C1 | Bạn giao bài tập bằng cách nào? | Multiple choice | Current solution |
| C2 | Học viên nộp bài tập như thế nào? | Multiple choice | Current solution |
| C3 | Thời gian chấm bài trung bình/lớp? | Single choice | Efficiency |
| C4 | Bạn mong muốn công cụ hỗ trợ gì trong chấm bài? | Open text | Requirements |

### D. Tương tác với phụ huynh

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| D1 | Tần suất liên lạc với phụ huynh? | Single choice | Frequency |
| D2 | Kênh liên lạc chính với phụ huynh? | Multiple choice | Channel |
| D3 | Nội dung thường trao đổi với phụ huynh? | Multiple choice | Content |

---

## 4.4. STUDENT - Học viên

### A. Thói quen học tập

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| A1 | Bạn học bao nhiêu lớp/tuần? | Number | Context |
| A2 | Thiết bị bạn dùng để học? | Multiple choice | Tech |
| A3 | Thời gian tự học/ngày? | Single choice | Context |
| A4 | Bạn thích học theo hình thức nào? | Ranking | Learning style |

### B. Bài tập và kiểm tra

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| B1 | Bạn nhận bài tập qua đâu? | Multiple choice | Current solution |
| B2 | Khó khăn lớn nhất khi làm bài tập? | Open text | Pain points |
| B3 | Bạn muốn xem điểm số ở đâu? | Single choice | Requirements |
| B4 | Bạn có muốn so sánh điểm với bạn bè không? | Yes/No | Gamification |

### C. Gamification

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| C1 | Bạn có thích được thưởng điểm khi học tốt không? | Scale 1-5 | Validation |
| C2 | Loại phần thưởng nào hấp dẫn bạn nhất? | Ranking | Requirements |
| | - Điểm tích lũy đổi quà | | |
| | - Huy hiệu thành tích | | |
| | - Xếp hạng trong lớp | | |
| | - Giảm học phí | | |
| C3 | Bạn có muốn có bảng xếp hạng lớp học không? | Yes/No | Validation |

### D. Giao diện và trải nghiệm

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| D1 | Bạn thích giao diện app học tập như thế nào? | Open text | UI/UX |
| D2 | Tính năng quan trọng nhất trên app? | Ranking | Priority |
| D3 | Bạn có dùng app học tập nào khác không? Thích gì ở app đó? | Open text | Benchmark |

---

## 4.5. PARENT - Phụ huynh

### A. Theo dõi con

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| A1 | Bạn có bao nhiêu con đang đi học thêm? | Number | Context |
| A2 | Bạn theo dõi việc học của con bằng cách nào? | Multiple choice | Current solution |
| A3 | Tần suất bạn liên lạc với trung tâm? | Single choice | Frequency |
| A4 | Thông tin nào bạn muốn biết nhất về con? | Ranking | Requirements |

### B. Thanh toán học phí

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| B1 | Bạn thanh toán học phí bằng cách nào? | Multiple choice | Current solution |
| B2 | Bạn có gặp khó khăn gì khi thanh toán không? | Open text | Pain points |
| B3 | Bạn có muốn thanh toán online (QR, ví điện tử) không? | Yes/No | Validation |
| B4 | Bạn có muốn xem lịch sử thanh toán, công nợ không? | Yes/No | Validation |

### C. Thông báo

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| C1 | Bạn muốn nhận thông báo qua kênh nào? | Ranking | Channel preference |
| | - Zalo | | |
| | - SMS | | |
| | - Email | | |
| | - App | | |
| C2 | Loại thông báo nào quan trọng với bạn? | Multiple choice | Content |
| | - Con vắng học | | |
| | - Điểm kiểm tra | | |
| | - Nhắc đóng học phí | | |
| | - Lịch học thay đổi | | |
| C3 | Tần suất nhận thông báo mong muốn? | Single choice | Frequency |

### D. Cổng thông tin phụ huynh

| # | Câu hỏi | Loại | Mục đích |
|---|---------|------|----------|
| D1 | Bạn có muốn có app/website riêng để theo dõi con không? | Yes/No | Validation |
| D2 | Tính năng quan trọng nhất trên cổng phụ huynh? | Ranking | Priority |
| D3 | Bạn sẵn sàng cài app mới để theo dõi con không? | Yes/No | Adoption |

---

# 5. MẪU PHIẾU KHẢO SÁT

## 5.1. Phiếu khảo sát CENTER_OWNER (Google Forms)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    KHẢO SÁT NHU CẦU QUẢN LÝ TRUNG TÂM GIÁO DỤC                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Xin chào Quý Anh/Chị!                                                           │
│                                                                                  │
│  Chúng tôi đang phát triển phần mềm quản lý trung tâm giáo dục KiteClass.       │
│  Xin Anh/Chị dành 5-7 phút để hoàn thành khảo sát này.                          │
│                                                                                  │
│  Mọi thông tin sẽ được bảo mật và chỉ sử dụng cho mục đích nghiên cứu.          │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 1: THÔNG TIN TRUNG TÂM                                                     │
│                                                                                  │
│  1. Trung tâm của Anh/Chị hoạt động được bao lâu? *                              │
│     ○ Dưới 1 năm                                                                 │
│     ○ 1-3 năm                                                                    │
│     ○ 3-5 năm                                                                    │
│     ○ Trên 5 năm                                                                 │
│                                                                                  │
│  2. Số lượng học viên hiện tại? *                                                │
│     ○ Dưới 50                                                                    │
│     ○ 50 - 100                                                                   │
│     ○ 100 - 200                                                                  │
│     ○ 200 - 500                                                                  │
│     ○ Trên 500                                                                   │
│                                                                                  │
│  3. Lĩnh vực giảng dạy chính? (Chọn nhiều) *                                     │
│     ☐ Ngoại ngữ (Anh, Trung, Hàn, Nhật...)                                      │
│     ☐ Toán - Lý - Hóa                                                            │
│     ☐ Văn - Sử - Địa                                                             │
│     ☐ Tin học / Lập trình                                                        │
│     ☐ Âm nhạc / Mỹ thuật                                                         │
│     ☐ Kỹ năng mềm                                                                │
│     ☐ Khác: _____________                                                        │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 2: TÌNH HÌNH HIỆN TẠI                                                      │
│                                                                                  │
│  4. Anh/Chị đang quản lý trung tâm bằng công cụ gì? (Chọn nhiều) *               │
│     ☐ Excel / Google Sheets                                                      │
│     ☐ Sổ sách giấy                                                               │
│     ☐ Phần mềm quản lý (ghi rõ tên): _____________                               │
│     ☐ Zalo / Facebook Group                                                      │
│     ☐ Khác: _____________                                                        │
│                                                                                  │
│  5. 3 vấn đề lớn nhất trong quản lý hiện tại của Anh/Chị? *                      │
│     ___________________________________________________________________          │
│     ___________________________________________________________________          │
│     ___________________________________________________________________          │
│                                                                                  │
│  6. Đánh giá mức độ khó khăn của các công việc (1 = Dễ, 5 = Rất khó) *           │
│                                                                                  │
│     Quản lý học phí, công nợ:          ○1  ○2  ○3  ○4  ○5                        │
│     Điểm danh, theo dõi học viên:      ○1  ○2  ○3  ○4  ○5                        │
│     Trao đổi với phụ huynh:            ○1  ○2  ○3  ○4  ○5                        │
│     Quản lý lịch học, phòng học:       ○1  ○2  ○3  ○4  ○5                        │
│     Báo cáo, thống kê:                 ○1  ○2  ○3  ○4  ○5                        │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 3: NHU CẦU TÍNH NĂNG                                                       │
│                                                                                  │
│  7. Đánh giá mức độ quan trọng các tính năng (1 = Không cần, 5 = Rất cần) *      │
│                                                                                  │
│     Quản lý học phí tự động:           ○1  ○2  ○3  ○4  ○5                        │
│     Thanh toán online (QR, MoMo):      ○1  ○2  ○3  ○4  ○5                        │
│     Thông báo tự động cho phụ huynh:   ○1  ○2  ○3  ○4  ○5                        │
│     Hệ thống điểm thưởng học viên:     ○1  ○2  ○3  ○4  ○5                        │
│     Cổng thông tin phụ huynh:          ○1  ○2  ○3  ○4  ○5                        │
│     Video bài giảng online:            ○1  ○2  ○3  ○4  ○5                        │
│     Báo cáo doanh thu tự động:         ○1  ○2  ○3  ○4  ○5                        │
│                                                                                  │
│  8. Tính năng nào Anh/Chị mong muốn nhưng chưa được liệt kê?                     │
│     ___________________________________________________________________          │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 4: GIÁ CẢ VÀ QUYẾT ĐỊNH                                                    │
│                                                                                  │
│  9. Anh/Chị sẵn sàng chi trả bao nhiêu/tháng cho phần mềm quản lý? *             │
│     ○ Dưới 500.000 VND                                                           │
│     ○ 500.000 - 1.000.000 VND                                                    │
│     ○ 1.000.000 - 2.000.000 VND                                                  │
│     ○ Trên 2.000.000 VND                                                         │
│                                                                                  │
│  10. Yếu tố quan trọng nhất khi chọn phần mềm? (Xếp hạng 1-5) *                  │
│      ___ Giá cả hợp lý                                                           │
│      ___ Tính năng đầy đủ                                                        │
│      ___ Dễ sử dụng                                                              │
│      ___ Hỗ trợ kỹ thuật tốt                                                     │
│      ___ Bảo mật dữ liệu                                                         │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  11. Anh/Chị có muốn tham gia phỏng vấn sâu hơn không? (30 phút, online)         │
│      ○ Có, xin liên hệ qua: _____________                                        │
│      ○ Không                                                                     │
│                                                                                  │
│                           [ GỬI KHẢO SÁT ]                                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# 6. KỊCH BẢN PHỎNG VẤN

## 6.1. Discovery Interview - CENTER_OWNER (30 phút)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│              KỊCH BẢN PHỎNG VẤN - CHỦ TRUNG TÂM (DISCOVERY)                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  CHUẨN BỊ TRƯỚC PHỎNG VẤN:                                                       │
│  • Đọc lại responses từ survey                                                   │
│  • Chuẩn bị note-taking template                                                 │
│  • Test recording (nếu được phép)                                                │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  MỞ ĐẦU (3 phút)                                                                 │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  "Xin chào Anh/Chị [Tên], cảm ơn Anh/Chị đã dành thời gian cho buổi phỏng vấn   │
│  hôm nay. Tôi là [Tên], đang phát triển phần mềm quản lý trung tâm giáo dục.    │
│                                                                                  │
│  Buổi nói chuyện này khoảng 30 phút. Mục đích là để tôi hiểu hơn về công việc   │
│  hàng ngày của Anh/Chị và những khó khăn đang gặp phải.                         │
│                                                                                  │
│  Không có câu trả lời đúng hay sai. Tôi muốn nghe ý kiến thật của Anh/Chị.      │
│  Anh/Chị có cho phép tôi ghi âm để tiện ghi chép không?"                        │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 1: CONTEXT (5 phút)                                                        │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  Q1: "Anh/Chị có thể giới thiệu sơ về trung tâm được không ạ?                   │
│       - Dạy những môn gì?                                                        │
│       - Quy mô như thế nào?"                                                     │
│                                                                                  │
│  Q2: "Anh/Chị bắt đầu mở trung tâm như thế nào? Động lực ban đầu là gì?"        │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 2: WORKFLOW HIỆN TẠI (10 phút)                                             │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  Q3: "Anh/Chị có thể mô tả một ngày làm việc điển hình được không?              │
│       Từ sáng đến tối thường làm những gì?"                                      │
│                                                                                  │
│  Q4: "Khi có học viên mới đăng ký, quy trình tiếp nhận như thế nào?             │
│       Ai làm việc này? Mất bao lâu?"                                             │
│                                                                                  │
│  Q5: "Việc thu học phí được thực hiện như thế nào?                               │
│       Có gặp trường hợp học viên nợ học phí không? Xử lý ra sao?"               │
│                                                                                  │
│  Q6: "Anh/Chị liên lạc với phụ huynh bằng cách nào?                              │
│       Tần suất như thế nào? Thường trao đổi về vấn đề gì?"                       │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 3: PAIN POINTS (10 phút)                                                   │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  Q7: "Trong khảo sát, Anh/Chị có đề cập đến [vấn đề từ survey].                 │
│       Anh/Chị có thể kể cụ thể hơn về lần gần đây nhất gặp vấn đề này không?"   │
│                                                                                  │
│  Q8: "Vấn đề này ảnh hưởng đến công việc như thế nào?                            │
│       Mất bao nhiêu thời gian/tiền bạc?"                                         │
│                                                                                  │
│  Q9: "Anh/Chị đã thử cách nào để giải quyết chưa? Kết quả thế nào?"             │
│                                                                                  │
│  Q10: "Nếu có một phép màu giải quyết được 1 vấn đề lớn nhất,                    │
│        Anh/Chị muốn giải quyết vấn đề gì?"                                       │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  KẾT THÚC (2 phút)                                                               │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  "Cảm ơn Anh/Chị rất nhiều. Những chia sẻ này rất giá trị với chúng tôi.        │
│                                                                                  │
│  Trong thời gian tới, khi chúng tôi có prototype, Anh/Chị có sẵn sàng           │
│  dành thêm 30-45 phút để thử và cho feedback không?                              │
│                                                                                  │
│  Anh/Chị còn điều gì muốn chia sẻ thêm không ạ?"                                 │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  SAU PHỎNG VẤN:                                                                  │
│  • Gửi email cảm ơn trong vòng 24h                                               │
│  • Transcribe recording (nếu có)                                                 │
│  • Highlight key insights                                                        │
│  • Update interview summary sheet                                                │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 6.2. Validation Interview - PARENT (45 phút)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│              KỊCH BẢN PHỎNG VẤN - PHỤ HUYNH (VALIDATION)                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  MỞ ĐẦU (3 phút)                                                                 │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  "Xin chào Anh/Chị, cảm ơn Anh/Chị đã tham gia. Hôm nay tôi muốn cho Anh/Chị   │
│  xem một số ý tưởng về cổng thông tin phụ huynh và lấy ý kiến của Anh/Chị."    │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 1: CONTEXT RECALL (5 phút)                                                 │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  Q1: "Hiện tại Anh/Chị theo dõi việc học của con như thế nào?"                  │
│                                                                                  │
│  Q2: "Điều gì làm Anh/Chị lo lắng nhất về việc học của con?"                    │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 2: SHOW MOCKUPS (25 phút)                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  [Hiển thị mockup Dashboard]                                                     │
│                                                                                  │
│  Q3: "Đây là màn hình chính. Anh/Chị thấy những thông tin gì?                   │
│       Thông tin nào quan trọng nhất với Anh/Chị?"                                │
│                                                                                  │
│  Q4: "Nếu con vắng học, Anh/Chị muốn biết ngay không?                           │
│       Muốn nhận thông báo qua đâu?"                                              │
│                                                                                  │
│  [Hiển thị mockup Thanh toán QR]                                                 │
│                                                                                  │
│  Q5: "Đây là tính năng thanh toán học phí bằng QR.                               │
│       Anh/Chị có thấy dễ dùng không? Có lo ngại gì không?"                       │
│                                                                                  │
│  [Hiển thị mockup Điểm số & Tiến độ]                                             │
│                                                                                  │
│  Q6: "Đây là nơi xem điểm số của con.                                            │
│       Anh/Chị muốn xem thêm thông tin gì?"                                       │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  PHẦN 3: FEATURE VALIDATION (10 phút)                                            │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  Q7: "Trong các tính năng vừa xem, tính năng nào Anh/Chị sẽ dùng nhiều nhất?"   │
│                                                                                  │
│  Q8: "Có tính năng nào Anh/Chị thấy không cần thiết không?"                     │
│                                                                                  │
│  Q9: "Anh/Chị có muốn thêm tính năng gì khác không?"                            │
│                                                                                  │
│  Q10: "Nếu có app này, Anh/Chị có sẵn sàng cài và sử dụng không?"               │
│                                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  KẾT THÚC (2 phút)                                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# 7. PHƯƠNG PHÁP PHÂN TÍCH KẾT QUẢ

## 7.1. Quy trình phân tích

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         QUY TRÌNH PHÂN TÍCH                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  BƯỚC 1: THU THẬP & CHUẨN HÓA DỮ LIỆU                                            │
│  ─────────────────────────────────────                                           │
│  • Export survey responses → Excel/Google Sheets                                 │
│  • Transcribe interviews → Text documents                                        │
│  • Mã hóa câu trả lời mở (open coding)                                           │
│                                                                                  │
│  BƯỚC 2: PHÂN TÍCH ĐỊNH LƯỢNG (Survey)                                           │
│  ─────────────────────────────────────                                           │
│  • Tính % cho câu hỏi đóng                                                       │
│  • Tính mean, median cho thang điểm                                              │
│  • Cross-tabulation theo segment (quy mô, lĩnh vực)                              │
│  • Tạo biểu đồ visualization                                                     │
│                                                                                  │
│  BƯỚC 3: PHÂN TÍCH ĐỊNH TÍNH (Interview)                                         │
│  ─────────────────────────────────────                                           │
│  • Affinity mapping: Gom nhóm insights theo theme                                │
│  • Quote extraction: Trích dẫn quan trọng                                        │
│  • Pain point ranking: Xếp hạng vấn đề theo frequency + severity                 │
│                                                                                  │
│  BƯỚC 4: TỔNG HỢP & BÁO CÁO                                                      │
│  ─────────────────────────────────────                                           │
│  • Key findings summary                                                          │
│  • Feature prioritization matrix                                                 │
│  • Persona profiles                                                              │
│  • Recommendations                                                               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 7.2. Template báo cáo kết quả

| Section | Nội dung |
|---------|----------|
| **Executive Summary** | Tóm tắt 5-7 key findings |
| **Methodology** | Số respondents, timeline, phương pháp |
| **Demographics** | Phân bố theo quy mô, lĩnh vực, vị trí |
| **Pain Points** | Top 10 vấn đề, xếp hạng theo tần suất & severity |
| **Feature Priorities** | Ma trận quan trọng vs khó thực hiện |
| **Personas** | 3-5 user personas chi tiết |
| **Quotes** | 10-15 trích dẫn tiêu biểu |
| **Recommendations** | Hành động đề xuất |

## 7.3. Feature Prioritization Matrix

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    FEATURE PRIORITIZATION MATRIX                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  QUAN TRỌNG                                                                      │
│  (User Rating)                                                                   │
│       ▲                                                                          │
│   5   │   ┌─────────────────────────────────────────────────────────┐           │
│       │   │                  DO FIRST (P0)                          │           │
│       │   │  • Quản lý học phí                                      │           │
│       │   │  • Điểm danh tự động                                    │           │
│       │   │  • Thông báo phụ huynh                                  │           │
│   4   │   └─────────────────────────────────────────────────────────┘           │
│       │                                                                          │
│       │   ┌─────────────────────────────────────────────────────────┐           │
│   3   │   │               DO NEXT (P1)                              │           │
│       │   │  • Thanh toán QR                                        │           │
│       │   │  • Cổng phụ huynh                                       │           │
│       │   │  • Báo cáo doanh thu                                    │           │
│   2   │   └─────────────────────────────────────────────────────────┘           │
│       │                                                                          │
│       │   ┌─────────────────────────────────────────────────────────┐           │
│   1   │   │              DO LATER (P2)                              │           │
│       │   │  • Gamification                                         │           │
│       │   │  • Video bài giảng                                      │           │
│       │   │  • Forum                                                │           │
│       │   └─────────────────────────────────────────────────────────┘           │
│       │                                                                          │
│       └─────────────────────────────────────────────────────────────────────►   │
│           1         2         3         4         5                              │
│                         KHẢ THI (Implementation Effort)                          │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

*Tài liệu được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
