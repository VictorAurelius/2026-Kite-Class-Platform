---
persona: p3-center-manager
topic: daily-operations
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 6
---

# Vận hành hàng ngày — Lớp · Chấm công · Học sinh

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **6 phút**

## TL;DR

Anh Tâm dùng 3 màn hình chính mỗi ngày: lịch lớp, chấm công, danh sách học sinh. Mobile-friendly để dùng trên tablet/điện thoại khi đi qua các phòng học.

- 📅 **Lịch:** Kéo-thả slot giữa GV/phòng, auto cảnh báo trùng
- ✅ **Chấm công:** Tap-check trên mobile, ≤30 giây/lớp
- 👨‍🎓 **Học sinh:** Search, sort, filter theo lớp/tình trạng thanh toán
- 📱 **Mobile:** Có app dành riêng cho Manager + Teacher

---

## 1. Quản lý lịch lớp

### 1.1 Xem lịch tuần

<!-- Screenshot placeholder pending B+C merge: capture daily-operations-step-1.png — 1440×900 vi-VN — show /schedule weekly view với 5 cột (Thứ 2-6) × 12 row giờ (8-20h) + cells hiển thị "Lớp 5A1 / Cô Mai / Phòng 101" + nút "Thêm lớp" mũi tên đỏ -->

URL: `/schedule`

Grid view 7 cột (Thứ 2-CN) × 12 row giờ (8h-20h, mỗi row 1 giờ).

Cell hiển thị:
- Tên lớp: `Anh ngữ 5A1`
- Giáo viên: `Cô Phạm Thị Mai`
- Phòng: `Phòng 101`
- Số học sinh: `12/15`

Click cell → mở detail view với link "Sửa lớp" + "Xoá lớp" + "Đổi GV".

### 1.2 Tạo lớp mới

<!-- Screenshot placeholder pending B+C merge: capture daily-operations-step-2.png — 1440×900 vi-VN — show /classes/new form (Tên lớp / GV dropdown / Phòng dropdown / Slot picker) + dropdown trùng lịch warning + nút Lưu mũi tên đỏ -->

Click **+ Thêm lớp** trong grid → form:

| Field | Bắt buộc | Mô tả |
|---|:---:|---|
| Tên lớp | ✅ | VD `Anh ngữ 5A1`, `Toán nâng cao 9B` |
| Môn học | ✅ | Dropdown: Anh ngữ / Toán / Khoa học / STEM / Năng khiếu |
| Cấp lớp | ✅ | Tiểu học / THCS / THPT / Người lớn |
| Giáo viên | ✅ | Dropdown autocomplete (Mai, Lan, ...) |
| Phòng học | ✅ | Dropdown từ `/settings/rooms` |
| Slot | ✅ | Pick từ grid hoặc gõ "Thứ 2,4,6 - 18h-19h30" |
| Số lượng tối đa | ✅ | Mặc định 15 (theo `kitehub.class.max-students`) |
| Học phí | ✅ | Mặc định 1.500.000đ/khoá theo bảng giá |

Hệ thống verify:
- ✅ GV có slot trống → tạo
- ⚠️ GV đã có lớp khác trong slot → confirm dialog
- ❌ Phòng đã đặt → reject, suggest phòng khác

### 1.3 Kéo-thả đổi slot

Drag cell tới slot mới → tự động:
1. Kiểm tra trùng lịch GV/phòng
2. Nếu OK → cập nhật + gửi notification GV qua email
3. Nếu trùng → modal cảnh báo "Lớp 5A1 đã được xếp tại Thứ Hai 18h. Bạn vẫn muốn đổi?"

---

## 2. Chấm công học sinh

### 2.1 Mở chấm công lớp

<!-- Screenshot placeholder pending B+C merge: capture daily-operations-step-3.png — 1440×900 vi-VN — show /attendance/{class-id} mobile view tablet 768×1024 với 12 student rows mỗi row có 3 button (Có mặt xanh / Vắng đỏ / Phép vàng) + status "Đã điểm danh 8/12" + mũi tên đỏ chỉ vào nút Lưu -->

URL: `/attendance` → chọn lớp đang diễn ra (gợi ý từ lịch hiện tại)

Hoặc QR: GV scan QR phòng → tự mở chấm công lớp đó.

Mobile-first layout — mỗi học sinh 1 row có 3 button lớn:

| Học sinh | Có mặt | Vắng có phép | Vắng không phép |
|---|:---:|:---:|:---:|
| Lê Thị Hoa | 🟢 Tap | 🟡 Tap | 🔴 Tap |
| Nguyễn Văn Hùng | 🟢 Tap | 🟡 Tap | 🔴 Tap |
| ... | | | |

Tap → màu fill ngay → progress bar trên đầu "Đã điểm danh 8/12".

Khi điểm danh đủ 12/12 → tự động lưu + gửi notification phụ huynh "Bé Hoa đã có mặt lúc 14:00 Thứ Hai 14/05/2026".

### 2.2 Sửa điểm danh sau

Trong vòng 24h, GV/Manager có thể sửa:
- Click lớp → tab **Lịch sử điểm danh**
- Click ngày cần sửa → form re-tap
- Save → gửi notification correct cho phụ huynh

Sau 24h, sửa cần escalate chị Hằng duyệt (audit log).

---

## 3. Quản lý học sinh

<!-- Screenshot placeholder pending B+C merge: capture daily-operations-step-4.png — 1440×900 vi-VN — show /students list 120 hs với search + filter (Lớp / Tình trạng thanh toán PAID/PENDING/OVERDUE) + 5 row mock (Lê Thị Hoa 5A1 PAID, Phạm Tuấn 9B PENDING ...) + nút "Thêm học sinh" mũi tên đỏ -->

URL: `/students`

| Cột | Filter |
|---|---|
| Họ tên | Search box |
| Ngày sinh | Range picker |
| Lớp đang học | Dropdown |
| Phụ huynh | Search |
| SĐT phụ huynh | — |
| Tình trạng thanh toán | PAID / PENDING / OVERDUE |
| Status | ACTIVE / WITHDRAWN |

Click row → detail full info + history class + payment + attendance.

### 3.1 Thêm học sinh mới

Click **+ Thêm học sinh** → form 3 step:

1. **Thông tin cá nhân:** Họ tên, DOB, giới tính, ảnh
2. **Thông tin phụ huynh:** Họ tên cha/mẹ, SĐT, email, Zalo
3. **Xếp lớp:** Chọn lớp đang dạy hoặc tạo lớp mới → tự tính học phí

Sau khi save → hệ thống gửi welcome email phụ huynh + tạo invoice tháng đầu.

### 3.2 Học sinh xin nghỉ

Phụ huynh báo qua Zalo/phone → anh Tâm vào `/students/{id}` → click **Đổi tình trạng** → chọn `WITHDRAWN` + nhập lý do + ngày nghỉ.

Action này KHÔNG cần chị Hằng duyệt. Học phí tháng còn lại sẽ hoàn lại theo policy.

---

## 4. Đơn xin nghỉ giáo viên

URL: `/leave-requests`

<!-- Screenshot placeholder pending B+C merge: capture daily-operations-step-5.png — 1440×900 vi-VN — show leave request list 2 row "Cô Mai nghỉ ốm 16-17/05 / Cô Lan họp PH 18/05" + nút "Phê duyệt" mũi tên đỏ + escalate chị Hồng nếu >3 ngày -->

GV submit đơn → anh Tâm duyệt:

| Loại nghỉ | Anh Tâm tự duyệt | Cần chị Hồng |
|---|:---:|:---:|
| ≤1 ngày (ốm, việc gia đình) | ✅ | — |
| 2-3 ngày | ✅ | — |
| >3 ngày | — | ✅ |
| Nghỉ phép năm | — | ✅ |
| Không lương | — | ✅ |

Click **Phê duyệt** → tự động:
1. Update lịch GV nghỉ
2. Reassign lớp cho GV thay (gợi ý tự động)
3. Gửi notification phụ huynh các lớp ảnh hưởng

Nếu cần escalate → click **Chuyển lên Chủ trung tâm** → đơn về `/owner/leave-requests` của chị Hồng.

---

## 5. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Kéo-thả lịch không update | Network drop | F5, retry |
| Điểm danh mobile lag | 3G yếu | Wait WiFi hoặc cache offline (Phase 2) |
| Học sinh không hiện trong list | Filter ẩn (Withdrawn) | Toggle filter |
| Đơn xin nghỉ "Cần Owner duyệt" nhưng nghỉ ≤2 ngày | Bug RBAC | File P1 gap + escalate chị Hồng |

---

## 6. Liên kết

- [Tổng quan Manager](index.md)
- [Báo cáo](reports.md)
- [Quyền hạn STAFF](permissions.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 👥 Hỏi chị Hồng trực tiếp khi escalate
- 📞 Hotline: 1900-xxxx
- 📊 Trạng thái beta: [/beta-status](/beta-status)
