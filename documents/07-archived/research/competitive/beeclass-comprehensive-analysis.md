# BÁO CÁO PHÂN TÍCH TOÀN DIỆN: BEECLASS v2.0

## Nền tảng quản lý lớp học và trung tâm hàng đầu Việt Nam

| Thuộc tính | Giá trị |
|------------|---------|
| **Ngày** | 23/12/2025 |
| **Phiên bản báo cáo** | 2.0 (Cập nhật từ tài liệu chính thức) |
| **Loại tài liệu** | Phân tích đối thủ cạnh tranh |
| **Nguồn chính** | Hướng dẫn sử dụng BeeClass v2.0 (Official PDF) |

---

## MỤC LỤC

1. [Tổng quan](#phần-1-tổng-quan)
2. [Tính năng mới v2.0](#phần-2-tính-năng-mới-v20)
3. [Quản lý Trung tâm](#phần-3-quản-lý-trung-tâm)
4. [Quản lý Lớp học & Học sinh](#phần-4-quản-lý-lớp-học--học-sinh)
5. [Quản lý Buổi học & Điểm danh](#phần-5-quản-lý-buổi-học--điểm-danh)
6. [Hệ thống Thu học phí](#phần-6-hệ-thống-thu-học-phí)
7. [Gamification & BeeClass Points](#phần-7-gamification--beeclass-points)
8. [Quản lý Điểm số](#phần-8-quản-lý-điểm-số)
9. [Cổng Phụ huynh](#phần-9-cổng-phụ-huynh)
10. [So sánh và Bài học cho KiteClass](#phần-10-so-sánh-và-bài-học-cho-kiteclass)

---

## PHẦN 1: TỔNG QUAN

### 1.1. Giới thiệu BeeClass v2.0

| Thông tin | Chi tiết |
|-----------|----------|
| **Website** | [beeclass.net](https://beeclass.net) |
| **Slogan** | "Lớp học Hạnh phúc - Tích sao, thưởng điểm, nhận xét online" |
| **Version hiện tại** | **v2.0** |
| **Đối tượng** | Giáo viên K-12, **Trung tâm dạy thêm**, Gia sư |
| **Giá** | **Miễn phí hoàn toàn** |
| **Ngôn ngữ** | Tiếng Việt |
| **Platform** | Web (Desktop ưu tiên, Mobile đang phát triển) |

### 1.2. Thay đổi quan trọng từ 05/10/2025

| Thay đổi | Chi tiết |
|----------|----------|
| **Đăng ký mới** | Chỉ qua **Số điện thoại + Zalo OTP** |
| **Tài khoản cũ** | Vẫn dùng Gmail được |
| **Chuyển đổi** | Có chức năng chuyển data từ Gmail sang SĐT |
| **Khôi phục MK** | Qua email đã đăng ký |

### 1.3. Định vị thị trường (Cập nhật)

```
┌─────────────────────────────────────────────────────────────────────────┐
│              BEECLASS v2.0 - ĐỊNH VỊ THỊ TRƯỜNG MỚI                      │
└─────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────┐
                    │   CLASSROOM & CENTER MANAGEMENT  │
                    │   (Quản lý lớp học + Trung tâm)  │
                    └─────────────────────────────────┘
                                    │
    ┌───────────────┬───────────────┼───────────────┬───────────────┐
    │               │               │               │               │
    ▼               ▼               ▼               ▼               ▼
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│GAMIFICA-│   │  TUITION│   │ GRADE   │   │ PARENT  │   │SCHEDULE │
│  TION   │   │ BILLING │   │MANAGEMT │   │ PORTAL  │   │ & TKB   │
│         │   │         │   │         │   │         │   │         │
│Tích điểm│   │Thu học  │   │Bảng điểm│   │Cổng PH  │   │Thời khóa│
│Đổi quà  │   │phí, QR  │   │Hệ số    │   │tự đăng  │   │biểu     │
└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘

                    ┌─────────────────────────────────┐
                    │         TARGET USERS            │
                    └─────────────────────────────────┘
                                    │
    ┌───────────────┬───────────────┼───────────────┬───────────────┐
    │               │               │               │               │
    ▼               ▼               ▼               ▼               ▼
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│  K-12   │   │ TRUNG   │   │  GIA    │   │  PHỤ    │   │  NHÂN   │
│ TEACHERS│   │  TÂM    │   │  SƯ     │   │ HUYNH   │   │  VIÊN   │
└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘
```

---

## PHẦN 2: TÍNH NĂNG MỚI v2.0

### 2.1. Tổng quan tính năng

> **Quan trọng:** BeeClass v2.0 đã mở rộng đáng kể từ một công cụ quản lý lớp học đơn giản thành một **hệ thống quản lý trung tâm toàn diện**.

```
BEECLASS v2.0 FEATURE MAP (OFFICIAL)
│
├── 🏢 QUẢN LÝ TRUNG TÂM/ĐƠN VỊ
│   ├── Tạo Đơn vị/Trung tâm
│   ├── Multi-teacher support
│   ├── Phân quyền (Quản lý/Nhân viên/Giáo viên)
│   └── Thống kê buổi dạy của GV
│
├── 📋 QUẢN LÝ LỚP HỌC
│   ├── Danh sách lớp
│   ├── Thời lượng buổi học
│   ├── Khóa học & Chương trình
│   ├── Ngày bắt đầu/kết thúc
│   └── Cách tính học phí (cố định/theo buổi)
│
├── 👨‍🎓 QUẢN LÝ HỌC SINH
│   ├── Danh sách học viên toàn trung tâm
│   ├── Thông tin chi tiết (avatar, SĐT, email)
│   ├── Giảm giá riêng cho từng HS
│   ├── Import từ Excel
│   └── 1 HS có thể học nhiều lớp
│
├── 📅 QUẢN LÝ BUỔI HỌC
│   ├── Tạo buổi học (kể cả quá khứ)
│   ├── Điểm danh (Có mặt/Vắng/Đi trễ/Vắng phép)
│   ├── Nhận xét từng buổi
│   ├── Nội dung buổi học
│   └── Hoàn tác điểm trong 10 giây
│
├── 💰 THU HỌC PHÍ ⭐ (MỚI)
│   ├── Phiếu thu học phí hàng tháng
│   ├── Phiếu thu khoản khác (đồng phục, tài liệu...)
│   ├── Tính học phí: Cố định/Theo buổi
│   ├── Giảm giá linh hoạt
│   ├── QR Code chuyển khoản
│   ├── In/Download PDF phiếu thu
│   ├── Theo dõi công nợ
│   └── Thống kê thu chi
│
├── 📊 QUẢN LÝ ĐIỂM SỐ ⭐ (MỚI)
│   ├── Tạo nhiều bài kiểm tra
│   ├── Hệ số điểm khác nhau
│   ├── Thang điểm tùy chỉnh
│   ├── Import/Export CSV
│   └── Tính điểm trung bình tự động
│
├── ⭐ BEECLASS (GAMIFICATION)
│   ├── Tiêu chí thưởng/phạt tùy chỉnh
│   ├── Tích điểm từng buổi
│   ├── Thống kê tuần/tháng
│   ├── Vinh danh học sinh
│   └── Đổi quà từ điểm tích lũy ⭐ (MỚI)
│
├── 📅 THỜI KHÓA BIỂU ⭐ (MỚI)
│   ├── Xem theo tuần
│   ├── Sáng/Chiều/Tối
│   ├── Hiển thị tên GV
│   └── Export CSV
│
└── 👨‍👩‍👧 CỔNG PHỤ HUYNH ⭐ (CẢI TIẾN)
    ├── PH tự tạo tài khoản (không cần GV gửi link)
    ├── Đăng nhập bằng SĐT + Zalo OTP
    ├── Xem: Nhận xét, Điểm danh, BeeClass, Học phí, Điểm
    ├── Cấu hình quyền xem của PH
    └── 1 PH có thể xem nhiều HS
```

### 2.2. So sánh v1.0 vs v2.0

| Tính năng | v1.0 | v2.0 |
|-----------|:----:|:----:|
| **Gamification (tích điểm)** | ✅ | ✅ |
| **Điểm danh** | ✅ | ✅✅ (theo buổi) |
| **Nhận xét** | ✅ | ✅✅ (theo buổi) |
| **Quản lý Trung tâm** | ❌ | ✅ |
| **Multi-teacher** | ❌ | ✅ |
| **Phân quyền** | ❌ | ✅ |
| **Thu học phí** | ❌ | ✅✅ |
| **Phiếu thu QR Code** | ❌ | ✅ |
| **Công nợ** | ❌ | ✅ |
| **Quản lý điểm số** | ❌ | ✅ |
| **Hệ số điểm** | ❌ | ✅ |
| **Đổi quà từ điểm** | ❌ | ✅ |
| **Thời khóa biểu** | ❌ | ✅ |
| **PH tự đăng ký** | ❌ | ✅ |
| **Buổi học quá khứ** | ❌ | ✅ |
| **Hoàn tác điểm** | ❌ | ✅ |

---

## PHẦN 3: QUẢN LÝ TRUNG TÂM

### 3.1. Khái niệm "Đơn vị/Trung tâm"

> **Định nghĩa:** Là một đơn vị, hoặc lớp dạy thêm tại nhà của thầy cô.
>
> **Ví dụ:** "Trung tâm Anh ngữ BeeClass", "Lớp Tiếng Anh cô Thảo"

### 3.2. Cấu trúc phân quyền

```
ROLE-BASED ACCESS CONTROL
│
├── 👑 QUẢN LÝ (Admin)
│   └── Toàn quyền thao tác
│       ├── Thêm/xóa Giáo viên
│       ├── Thiết lập quyền
│       ├── Xem tất cả lớp
│       └── Quản lý tài chính
│
├── 👔 NHÂN VIÊN (Staff)
│   └── Đầy đủ quyền, TRỪ:
│       ├── ❌ Thêm/xóa Giáo viên
│       └── ❌ Thiết lập quyền
│
└── 👨‍🏫 GIÁO VIÊN (Teacher)
    └── Chỉ thao tác với lớp được phân công
        ├── Tạo buổi học
        ├── Điểm danh
        ├── Nhận xét
        └── Ghi điểm
```

### 3.3. Multi-tenant Architecture

```
MULTI-CENTER SUPPORT
│
├── 1 Giáo viên có thể:
│   ├── Tham gia NHIỀU Trung tâm
│   └── Có vai trò KHÁC NHAU ở mỗi Trung tâm
│
├── 1 Trung tâm có thể:
│   ├── Có NHIỀU Giáo viên
│   ├── Có NHIỀU Nhân viên
│   └── Có NHIỀU Quản lý
│
└── 1 Lớp có thể:
    └── Có NHIỀU Giáo viên cùng dạy
```

**Use Case:** Thầy cô dạy thêm cho nhiều trung tâm, chỉ cần đưa SĐT cho bên Trung tâm để họ phân công lớp tương ứng.

---

## PHẦN 4: QUẢN LÝ LỚP HỌC & HỌC SINH

### 4.1. Tạo lớp học

```
TẠO LỚP - THÔNG TIN CẦN THIẾT
│
├── BẮT BUỘC
│   └── Tên lớp
│
└── TÙY CHỌN
    ├── Trạng thái (Đang học/Hủy lớp)
    ├── Thời lượng buổi học (phút)
    ├── Khóa học
    ├── Chương trình
    ├── Ngày bắt đầu / Ngày kết thúc
    ├── Cách tính học phí:
    │   ├── Cố định theo tháng (VD: 800.000đ)
    │   └── Theo số buổi (VD: 80.000đ/buổi)
    └── Học phí theo tháng / theo buổi
```

**Ví dụ từ tài liệu:**
- Lớp: "English Kids 1"
- Khóa học: "Tiếng Anh Trẻ em"
- Thời lượng: 90 phút
- Học phí: 800.000đ/tháng (cố định)

### 4.2. Quản lý học sinh

```
HỌC SINH vs HỌC VIÊN
│
├── HỌC SINH (trong 1 lớp cụ thể)
│   └── Khi thêm HS vào lớp → Tự động thành Học viên
│
└── HỌC VIÊN (toàn Trung tâm)
    ├── Danh sách tất cả HS đã/đang học
    ├── Kể cả HS đã học xong
    ├── Kể cả lớp đã bị xóa
    └── 1 HV có thể học NHIỀU lớp cùng lúc
```

### 4.3. Thông tin học sinh

| Field | Bắt buộc | Mô tả |
|-------|:--------:|-------|
| **Tên học sinh** | ✅ | Họ và tên |
| **Giới tính** | ✅ | Nam/Nữ (ảnh hưởng avatar) |
| **Nick** | ❌ | Biệt danh |
| **Ngày sinh** | ❌ | DD/MM/YYYY |
| **Điện thoại** | ❌ | SĐT PH (để PH xem được HS) |
| **Email** | ❌ | Email liên hệ |
| **Giảm giá hàng tháng** | ❌ | VNĐ (áp dụng cho lớp đang học) |
| **Ghi chú** | ❌ | Ghi chú riêng |
| **Avatar** | ❌ | Tùy chỉnh ảnh đại diện |

### 4.4. Import học sinh từ Excel

**Cấu trúc file Excel:**
```
| Họ và tên | Giới tính | Ngày sinh | Số điện thoại | Email |
|-----------|-----------|-----------|---------------|-------|
| Nguyễn A  | Nam       | 01/01/2015| 0901234567    | ...   |
```

- Dòng đầu tiên: Tên cột
- "Họ và tên" và "Giới tính": **Bắt buộc**
- Các cột khác: Tùy chọn

---

## PHẦN 5: QUẢN LÝ BUỔI HỌC & ĐIỂM DANH

### 5.1. Quản lý buổi học

```
SESSION MANAGEMENT FLOW
│
├── 1. TẠO BUỔI HỌC
│   ├── Tiêu đề (VD: "Unit 5: From the countryside...")
│   ├── Ngày/Giờ
│   └── ⚡ Có thể tạo buổi học TRONG QUÁ KHỨ
│
├── 2. ĐIỂM DANH
│   ├── Mặc định: Tất cả HS = "Có mặt"
│   ├── Trạng thái: Có mặt | Vắng | Đi trễ | Vắng phép
│   └── ⚡ Có thể điểm danh lại buổi cũ
│
├── 3. NHẬN XÉT
│   └── Thêm nhận xét cho từng HS mỗi buổi
│
└── 4. LƯU Ý
    ├── Số liệu điểm danh → Tính học phí (nếu theo buổi)
    ├── "Có mặt" + "Đi trễ" = Tính học phí
    ├── "Vắng" + "Vắng phép" = Không tính
    └── 🚩 Nếu buổi có tích điểm BeeClass → KHÔNG XÓA ĐƯỢC
```

### 5.2. Thống kê điểm danh

**Ví dụ từ tài liệu:**

| STT | Họ tên | Tổng có mặt | 01/09 | 03/09 | 05/09 | 08/09 | 10/09 |
|-----|--------|-------------|:-----:|:-----:|:-----:|:-----:|:-----:|
| 1 | Khánh Linh | 5 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 2 | Phương Mai | 3 | ✅ | ❌ | ✅ | ❌ | ✅ |
| 3 | Quốc Khánh | 5 | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## PHẦN 6: HỆ THỐNG THU HỌC PHÍ ⭐

> **QUAN TRỌNG:** Đây là tính năng mới của BeeClass v2.0, hoàn toàn **MIỄN PHÍ**.

### 6.1. Tổng quan tính năng học phí

```
TUITION BILLING SYSTEM
│
├── 💰 PHIẾU THU HỌC PHÍ HÀNG THÁNG
│   ├── Tạo kỳ hóa đơn cho cả lớp
│   ├── Tính tự động theo cài đặt lớp
│   └── Áp dụng giảm giá riêng từng HS
│
├── 📝 PHIẾU THU CÁC KHOẢN KHÁC
│   ├── Học phí toàn khóa
│   ├── Đồng phục
│   ├── Tài liệu
│   └── Các khoản phí khác
│
├── 💳 PHƯƠNG THỨC THANH TOÁN
│   ├── Tiền mặt (Cash)
│   ├── Chuyển khoản (Bank Transfer)
│   ├── Momo
│   └── Khác
│
├── 🧾 PHIẾU THU
│   ├── Số phiếu tự động (VD: 202509.2.7)
│   ├── QR Code chuyển khoản (đúng STK, số tiền, mã phiếu)
│   ├── Download PDF
│   ├── In phiếu thu
│   └── Trạng thái: OPEN | PAID
│
└── 📊 BÁO CÁO
    ├── Sổ thu theo ngày
    ├── Học phí theo lớp
    ├── Công nợ
    ├── Tổng quan
    ├── Bảng kê
    └── Export CSV
```

### 6.2. Cách tính học phí

| Loại | Công thức | Ví dụ |
|------|-----------|-------|
| **Cố định theo tháng** | Học phí = Phí tháng - Giảm giá | 800.000 - 100.000 = 700.000đ |
| **Theo số buổi** | Học phí = Số buổi có mặt × Phí/buổi - Giảm giá | 10 buổi × 80.000 = 800.000đ |

### 6.3. Chi tiết Phiếu thu

**Ví dụ phiếu thu từ tài liệu:**

```
┌─────────────────────────────────────────────────────────────┐
│                    Tiếng Anh Cô Thảo                        │
│     123 Nguyễn Tất Thành, Đà Nẵng • 0905123456             │
│     MST: 0123456789 • [email protected]                  │
├─────────────────────────────────────────────────────────────┤
│  PHIẾU THU HỌC PHÍ – English Kids 1 (09/2025)              │
│  Ngày lập: 10/09/2025 19:45           Trạng thái: [PAID]   │
├─────────────────────────────────────────────────────────────┤
│  Học sinh:      Phương Mai          Số phiếu:   202509.2.7 │
│  Lớp:           English Kids 1      Tháng/Năm:  09/2025    │
│  Cách tính:     Cố định/tháng                              │
├─────────────────────────────────────────────────────────────┤
│  Phí gốc:       800.000 đ                                  │
│  Giảm:          100.000 đ                                  │
│  Phải thu:      700.000 đ                                  │
├─────────────────────────────────────────────────────────────┤
│  Thông tin chuyển khoản:                                   │
│  Ngân hàng:     VietcomBank                                │
│  Số tài khoản:  99999999                                   │
│  Tên tài khoản: Cô Thảo                                    │
│  Số tiền:       700.000 đ                                  │
│  Nội dung:      HocPhi 202509.2.7                          │
│                                                             │
│  [QR CODE - Quét để chuyển khoản]                          │
│                                                             │
│  Tình trạng: Đã thanh toán (CASH) vào 10/09/2025 19:45    │
└─────────────────────────────────────────────────────────────┘
```

### 6.4. Thống kê học phí

**Ví dụ từ tài liệu - Kỳ học phí tháng 09/2025:**

| STT | Học sinh | Học phí | Giảm | Phải thu | Trạng thái |
|-----|----------|---------|------|----------|------------|
| 1 | Khánh Linh | 800.000 | 0 | 800.000 | OPEN |
| 2 | Phương Mai | 800.000 | 100.000 | 700.000 | **PAID** |
| 3 | Quốc Khánh | 800.000 | 0 | 800.000 | **PAID** |
| 4 | Thảo Yên | 800.000 | 400.000 | 400.000 | **PAID** |

**Tổng kết:**
- Tổng phải thu: 5.100.000đ
- Đã thu: 1.900.000đ
- Còn nợ: 3.200.000đ

### 6.5. Sổ thu theo ngày

```
SỔ THU NGÀY 10/09/2025
│
├── Tổng đã thu: 1.900.000 đ
│   ├── Tiền mặt: 1.900.000 đ
│   ├── Chuyển khoản: 0 đ
│   ├── Momo: 0 đ
│   └── Khác: 0 đ
│
└── Chi tiết:
    ├── Phương Mai | English Kids 1 | 700.000đ | Tiền mặt
    ├── Quốc Khánh | English Kids 1 | 800.000đ | Tiền mặt
    └── Thảo Yên | English Kids 1 | 400.000đ | Tiền mặt
```

---

## PHẦN 7: GAMIFICATION & BEECLASS POINTS

### 7.1. Hệ thống tiêu chí

```
CRITERIA SYSTEM
│
├── TIÊU CHÍ THƯỞNG (Điểm dương)
│   ├── Icon + Tên + Số điểm
│   ├── VD: 🌟 Làm bài tập đầy đủ (+2)
│   └── VD: 💪 Hăng hái phát biểu (+1)
│
├── TIÊU CHÍ TRỪ (Điểm âm)
│   ├── Icon + Tên + Số điểm trừ
│   ├── VD: 💬 Nói chuyện riêng (-1)
│   └── VD: 📝 Không làm bài tập (-2)
│
└── ÁP DỤNG
    └── Bộ tiêu chí dùng CHUNG cho tất cả các lớp
```

### 7.2. Quy trình tích điểm

```
POINT AWARDING FLOW
│
├── 1. Vào BeeClass → Chọn Lớp → Chọn Buổi
│
├── 2. Bấm vào Học sinh muốn cộng/trừ điểm
│
├── 3. Chọn Tiêu chí → Điểm được cộng/trừ
│
├── 4. ⚡ Hoàn tác trong 10 giây (nếu bấm nhầm)
│
└── 5. Điểm tích lũy → Thống kê tuần/tháng/tổng
```

### 7.3. Đổi quà từ điểm tích lũy ⭐ (MỚI)

```
REWARD EXCHANGE SYSTEM
│
├── 1. TẠO DANH SÁCH QUÀ
│   ├── Tên quà
│   ├── Số điểm cần đổi
│   └── VD: "Bút bi" - 10 điểm
│
├── 2. ĐỔI QUÀ
│   ├── Chọn học sinh
│   ├── Chọn món quà
│   └── Trừ điểm tương ứng từ tổng điểm tích lũy
│
└── 3. THEO DÕI
    └── Lịch sử đổi quà
```

### 7.4. Giao diện BeeClass

**Từ ảnh trong tài liệu:**
- Hiển thị avatar + tên tất cả HS trong lớp
- 2 số bên cạnh mỗi HS: Điểm thưởng (xanh) | Điểm trừ (đỏ)
- Click vào HS để mở popup tích điểm

---

## PHẦN 8: QUẢN LÝ ĐIỂM SỐ

### 8.1. Tạo bài kiểm tra

```
TEST/EXAM MANAGEMENT
│
├── THÔNG TIN BÀI KIỂM TRA
│   ├── Tiêu đề (VD: "Test 1", "Test 2")
│   ├── Ngày kiểm tra
│   ├── Thang điểm (VD: 10)
│   ├── Hệ số (VD: 1, 2)
│   └── Tính vào TB (Có/Không)
│
├── NHẬP ĐIỂM
│   ├── Nhập từng HS
│   ├── Đánh dấu "Vắng" nếu không có điểm
│   ├── Import CSV
│   └── Export CSV
│
└── TÍNH TOÁN
    └── Điểm TB = Σ(Điểm × Hệ số) / Σ(Hệ số)
```

### 8.2. Ví dụ bảng điểm

| STT | Học sinh | Test 1 (03/09) | Test 2 (05/09) | Test 3 (05/09) | Điểm TB |
|-----|----------|:--------------:|:--------------:|:--------------:|:-------:|
| 1 | Khánh Linh | 9.0 | 9.0 | 9.0 | **9.0** |
| 2 | Phương Mai | 8.0 | 9.0 | 8.0 | **8.3** |
| 3 | Quốc Khánh | 7.0 | 9.0 | 9.0 | **8.5** |
| 4 | Thành Long | 10.0 | 9.0 | 9.0 | **9.3** |
| 5 | Thanh Thảo | Vắng | 9.0 | 8.0 | **8.3** |

---

## PHẦN 9: CỔNG PHỤ HUYNH

### 9.1. Đăng ký và Đăng nhập (Cải tiến v2.0)

```
PARENT PORTAL v2.0
│
├── ĐĂNG NHẬP
│   ├── Vào trang chủ → "Đăng nhập dành cho Phụ huynh"
│   ├── Nhập SĐT
│   ├── Nhận OTP qua Zalo
│   └── Đăng nhập thành công
│
├── YÊU CẦU
│   └── SĐT của PH phải được GV nhập vào thông tin HS
│
├── 1 PHỤ HUYNH CÓ THỂ
│   ├── Xem NHIỀU học sinh
│   ├── Ở 1 Trung tâm hoặc NHIỀU Trung tâm
│   └── Miễn là HS có gắn SĐT của PH
│
└── GV KHÔNG THỂ
    └── Can thiệp vào tài khoản Phụ huynh
```

### 9.2. Thông tin PH có thể xem

| Tab | Nội dung |
|-----|----------|
| **Nhận xét** | Nhận xét từng buổi học |
| **Điểm danh** | Lịch sử có mặt/vắng theo tháng |
| **BeeClass** | Điểm thưởng/phạt tích lũy |
| **Học phí** | Danh sách phiếu thu, trạng thái thanh toán |
| **Điểm** | Bảng điểm các bài kiểm tra, điểm TB |

### 9.3. Cấu hình quyền xem

> GV vào "Cấu hình" của Trung tâm và **bật/tắt** cho PH xem những thông tin nào.

### 9.4. Demo Account

```
DEMO PHỤHUYNH
├── Tên đăng nhập: 0123456789
└── Mật khẩu: 123456
```

---

## PHẦN 10: SO SÁNH VÀ BÀI HỌC CHO KITECLASS

### 10.1. So sánh BeeClass v2.0 với đối thủ

| Tính năng | BeeClass v2.0 | EasyEdu | EDUSPACE | KiteClass |
|-----------|:-------------:|:-------:|:--------:|:---------:|
| **Gamification** | ✅✅ | ❌ | ❌ | ❌ |
| **Đổi quà từ điểm** | ✅ | ❌ | ❌ | ❌ |
| **Thu học phí** | ✅ | ✅✅ | ✅✅ | ❌ |
| **Phiếu thu QR Code** | ✅ | ✅ | ✅ | ❌ |
| **Công nợ** | ✅ | ✅ | ✅ | ❌ |
| **Quản lý điểm số** | ✅ | ✅ | ✅ | ❌ |
| **Multi-teacher** | ✅ | ✅ | ✅ | ✅ |
| **Cổng Phụ huynh** | ✅ | ✅ | ✅ | ❌ |
| **Video Learning** | ❌ | ❌ | ❌ | ✅✅ |
| **Live Streaming** | ❌ | ❌ | ❌ | ✅✅ |
| **AI Features** | ❌ | ❌ | ❌ | ✅✅ |
| **Bán khóa học** | ❌ | ❌ | ❌ | ✅ |
| **Giá** | **Miễn phí** | Trả phí | Trả phí | Trả phí |

### 10.2. Điểm mạnh của BeeClass v2.0

```
STRENGTHS (Cập nhật)
│
├── 1. MIỄN PHÍ + ĐẦY ĐỦ TÍNH NĂNG
│   └── Cạnh tranh trực tiếp với EasyEdu, EDUSPACE
│
├── 2. GAMIFICATION UNIQUE
│   ├── Tích điểm + Đổi quà
│   └── Không đối thủ nào có
│
├── 3. THU HỌC PHÍ HOÀN CHỈNH
│   ├── Phiếu thu chuyên nghiệp
│   ├── QR Code thanh toán
│   └── Theo dõi công nợ
│
├── 4. PARENT PORTAL TỰ ĐĂNG KÝ
│   └── Không cần GV gửi link
│
├── 5. MULTI-CENTER SUPPORT
│   └── Phù hợp GV dạy nhiều nơi
│
└── 6. TIẾNG VIỆT NATIVE
    └── UX tối ưu cho thị trường VN
```

### 10.3. Hạn chế của BeeClass v2.0

```
WEAKNESSES (Cập nhật)
│
├── 1. KHÔNG CÓ VIDEO/LIVE LEARNING
│   └── Chỉ quản lý lớp offline
│
├── 2. KHÔNG CÓ MOBILE APP
│   └── Chỉ có web (đang phát triển mobile)
│
├── 3. KHÔNG CÓ AI FEATURES
│   └── Không có quiz generator, chatbot
│
├── 4. KHÔNG CÓ BÁN KHÓA HỌC
│   └── Không hỗ trợ e-commerce
│
└── 5. UI DESKTOP-FIRST
    └── Hiện chưa tối ưu cho điện thoại
```

### 10.4. Bài học cho KiteClass

#### Nên học từ BeeClass v2.0:

| Tính năng | Lý do | Priority |
|-----------|-------|:--------:|
| **Gamification + Đổi quà** | Tạo động lực học, unique | CAO |
| **Cổng Phụ huynh** | Thị trường VN cần | TRUNG BÌNH |
| **Phiếu thu QR Code** | Tiện lợi, chuyên nghiệp | THẤP |
| **Quản lý buổi học** | Cho live streaming | TRUNG BÌNH |

#### KHÔNG cần học:

| Tính năng | Lý do |
|-----------|-------|
| **Thu học phí chi tiết** | KiteClass dùng Stripe/VNPay tích hợp |
| **Multi-teacher phức tạp** | Đã có trong thiết kế |
| **K-12 focus** | Target khác (course creators) |

### 10.5. Tổng kết

| Aspect | BeeClass v2.0 | KiteClass |
|--------|---------------|-----------|
| **Target** | Trung tâm dạy thêm, Gia sư, K-12 | Course creators, Online education |
| **Model** | Miễn phí, quản lý lớp offline | Trả phí SaaS, online learning |
| **Core value** | Gamification + Billing miễn phí | Video + AI + Selling |
| **Cạnh tranh** | Gián tiếp | Có thể học hỏi Gamification |

---

## PHỤ LỤC: QUY TRÌNH SỬ DỤNG

### A. Quy trình chung

```
1️⃣ Tạo Tài khoản (SĐT + Zalo OTP)
        ↓
2️⃣ Tạo Đơn vị/Trung tâm
        ↓
3️⃣ Thêm lớp
        ↓
4️⃣ Thêm học sinh vào lớp
        ↓
5️⃣ Mỗi buổi học:
    └── Tạo buổi học → Điểm danh → Tích điểm BeeClass
        ↓
6️⃣ Thu học phí:
    └── Tạo kỳ thu học phí → Thu từng HS → Thống kê
```

### B. Tip: Tạo lớp Demo

> ⚡ Để không ảnh hưởng đến dữ liệu thực tế, thầy cô có thể tạo 1 lớp "Demo" và thao tác thử trên lớp đó.

---

## NGUỒN THAM KHẢO

1. **[CHÍNH]** Hướng dẫn sử dụng BeeClass v2.0 (PDF Official)
2. [BeeClass Official](https://beeclass.net/) - Website chính thức
3. [YourHomework](https://yourhomework.net/) - Hệ sinh thái
4. [Hướng dẫn BeeClass - AiDayHoc](https://aidayhoc.vn/quan-li-lop-hoc-bang-beeclass-626.html)

---

*Báo cáo được tạo bởi: KiteClass Development Team*
*Ngày: 23/12/2025*
*Phiên bản: 2.0 (Cập nhật từ tài liệu chính thức BeeClass v2.0)*
