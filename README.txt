================================================================
KITECLASS PLATFORM - HUONG DAN SU DUNG TAI LIEU DO AN TOT NGHIEP
================================================================

Chao mung ban den voi tai lieu do an KiteClass Platform!
File nay huong dan cach su dung cac tai lieu da duoc tao tu dong.


================================================================
1. CAU TRUC THU MUC
================================================================

KiteClass Platform/
│
├── diagrams/                           # Cac so do PlantUML
│   ├── architecture-diagram.puml       # Code PlantUML - So do kien truc
│   ├── architecture-diagram.png        # Hinh anh so do kien truc
│   ├── provisioning-flow.puml          # Code PlantUML - Flow provision
│   └── provisioning-flow.png           # Hinh anh flow provision
│
├── documents/
│   ├── reports/
│   │   ├── graduation-thesis-outline.txt        # De cuong do an (plain text)
│   │   ├── technology-stack-report.md           # Bao cao cong nghe chi tiet
│   │   └── system-architecture-v2-with-ai.md    # Bao cao kien truc V2
│   │
│   └── scripts/
│       ├── idea.md                     # Y tuong ban dau
│       └── action-1.md                 # Yeu cau tao tai lieu
│
└── README.txt                          # File nay


================================================================
2. CAC FILE CHINH VA CACH SU DUNG
================================================================

2.1. SƠ ĐỒ KIẾN TRÚC (architecture-diagram.png)
------------------------------------------------------------
Nội dung:
- Sơ đồ tổng quan kiến trúc KiteClass Platform
- Thể hiện KiteHub (Modular Monolith) và KiteClass Instances (Microservices)
- Các services, database, AWS infrastructure

Cách sử dụng:
1. Mở file architecture-diagram.png bằng trình xem ảnh
2. Chèn vào Word/PowerPoint để trình bày
3. Sử dụng trong báo cáo đề tài minh họa kiến trúc
4. Khuyến nghị: Đặt ở phần "Kiến trúc hệ thống"

Lưu ý:
- Sơ đồ đã được tạo từ file .puml (PlantUML) và đã có sẵn PNG
- Nếu cần chỉnh sửa, edit file architecture-diagram.puml
- Tạo lại hình bằng lệnh (đã cài sẵn Java & PlantUML):
  cd diagrams
  ~/jdk/jdk-21/bin/java -jar plantuml.jar architecture-diagram.puml


2.2. SƠ ĐỒ FLOW PROVISION (provisioning-flow.png)
------------------------------------------------------------
Nội dung:
- Quy trình đầy đủ để mở một KiteClass node mới
- Bao gồm các bước: Order -> AI Generation -> Infrastructure Provisioning
- Thể hiện rõ ràng các actor, services tham gia và thời gian xử lý

Cách sử dụng:
1. Mở file provisioning-flow.png
2. Chèn vào báo cáo ở phần "Quy trình mở node KiteClass"
3. Kết hợp với phần giải thích chi tiết trong graduation-thesis-outline.txt
4. Sử dụng khi trình bày demo flow hoạt động

Lưu ý:
- Sơ đồ sequence diagram dễ hiểu flow theo thời gian
- Chứa thông tin về cost và time estimate
- Đã được sửa lỗi và tạo PNG thành công


2.3. ĐỀ CƯƠNG ĐỒ ÁN (graduation-thesis-outline.txt)
------------------------------------------------------------
Nội dung:
- Báo cáo hoàn chỉnh về đề tài (ước tính 4 trang khi in)
- 5 phần chính:
  + Phần 1: Ý tưởng đề tài
  + Phần 2: Kiến trúc hệ thống và lý do lựa chọn
  + Phần 3: Quy trình mở node KiteClass với AI Agent
  + Phần 4: Điểm mạnh của đề tài
  + Phần 5: Thử thách của đề tài

Cách sử dụng:
1. Mở file bằng Notepad hoặc text editor bất kỳ
2. COPY TOÀN BỘ nội dung
3. PASTE vào Microsoft Word
4. Format lại:
   - Chọn font chữ phù hợp (VD: Times New Roman 13pt)
   - Căn lề justify
   - Tạo tiêu đề (Heading 1, 2, 3)
   - Chèn 2 sơ đồ .png vào vị trí phù hợp
   - Thêm số trang, header, footer
5. Lưu thành file .docx

Lưu ý:
- File được viết KHÔNG CÓ MARKDOWN SYNTAX để dễ copy vào Word
- ĐÃ CÓ DẤU TIẾNG VIỆT - Sẵn sàng sử dụng!
- Cần format lại trong Word để đẹp hơn
- Kích thước ước tính: 4 trang (bao gồm 2 hình sơ đồ)


2.4. BAO CAO CONG NGHE (technology-stack-report.md)
------------------------------------------------------------
Noi dung:
- Chi tiet cong nghe su dung cho tung service
- Ly do lua chon cong nghe
- AWS services va cach deploy

Cach su dung:
- Tham khao khi can biet chi tiet cong nghe
- Dung khi trinh bay technical details
- Copy cac phan can thiet vao bao cao chinh


2.5. BAO CAO KIEN TRUC V2 (system-architecture-v2-with-ai.md)
------------------------------------------------------------
Noi dung:
- Bao cao chi tiet ve kien truc V2 co AI Agent
- Giai thich ly do thay doi tu microservices sang modular monolith cho KiteHub
- Mo ta cac module va services

Cach su dung:
- Tham khao khi can ly giai kien truc
- Dung trong phan Literature Review
- Trich dan khi viet bao cao chi tiet


================================================================
3. HUONG DAN TAO BAO CAO HOAN CHINH
================================================================

Buoc 1: Tao file Word moi
- Mo Microsoft Word
- Tao document moi
- Thiet lap page layout: A4, margin Normal

Buoc 2: Copy noi dung tu graduation-thesis-outline.txt
- Mo graduation-thesis-outline.txt
- Copy toan bo (Ctrl+A, Ctrl+C)
- Paste vao Word (Ctrl+V)

Buoc 3: Format co ban
- Chon tat ca text (Ctrl+A)
- Doi font: Times New Roman, size 13
- Can le: Justify
- Line spacing: 1.5

Buoc 4: Tao tieu de
- Chon dong "DE CUONG Y TUONG DO AN TOT NGHIEP" -> Heading 1, center, bold, size 16
- Chon dong "TEN DE TAI: ..." -> Heading 1, center, bold, size 14
- Chon cac dong "PHAN 1:", "PHAN 2:", ... -> Heading 1, bold, size 14
- Chon cac dong "1.1.", "2.1.", ... -> Heading 2, bold, size 13

Buoc 5: Chen so do
- Tai dong "[Xem so do kien truc chi tiet trong file architecture-diagram.png]"
  -> Delete dong nay
  -> Insert -> Picture -> Chon architecture-diagram.png
  -> Resize cho phu hop (width: 16cm)
  -> Center alignment
  -> Them caption: "Hinh 1: So do kien truc KiteClass Platform"

- Tai dong "[Xem so do flow chi tiet trong file provisioning-flow.png]"
  -> Delete dong nay
  -> Insert -> Picture -> Chon provisioning-flow.png
  -> Resize cho phu hop (width: 16cm)
  -> Center alignment
  -> Them caption: "Hinh 2: Quy trinh provision KiteClass node"

Buoc 6: Dinh dang nang cao
- Them header: Ten truong, ten khoa
- Them footer: So trang
- Table of Contents (neu can): References -> Table of Contents
- Kiem tra chinh ta va ngu phap

Buoc 7: Luu file
- File -> Save As
- Dat ten: "De_Cuong_KiteClass_Platform.docx"


================================================================
4. MOT SO LUU Y QUAN TRONG
================================================================

4.1. Ve tieng Viet
- File .txt khong co dau de tranh loi encoding
- Sau khi paste vao Word, BAN CAN GO LAI DAU cho chinh xac
- Hoac su dung tool "Unikey" de tu dong go dau

4.2. Ve hinh anh
- 2 file PNG da duoc tao san, chat luong cao
- Nen resize lai cho phu hop voi trang Word (16cm width)
- Luon them caption cho hinh anh

4.3. Ve noi dung
- Noi dung da day du, uoc tinh 4 trang
- Ban co the them/bot noi dung tuy theo yeu cau cua giao vien huong dan
- Phan "Thu thach" rat quan trong, the hien tinh thuc te

4.4. Ve chinh sua
- Neu can chinh sua so do, edit file .puml
- Tao lai PNG bang cong cu PlantUML
- Hoac su dung online: http://www.plantuml.com/plantuml/uml/


================================================================
5. CÁC FILE PLANTUML (.puml)
================================================================

File .puml là PlantUML source code, dùng để tạo sơ đồ.

QUAN TRỌNG: Hệ thống đã được cài đặt sẵn Java và PlantUML!
- Java OpenJDK 21 đã được cài tại: ~/jdk/jdk-21/
- PlantUML jar đã được tải tại: diagrams/plantuml.jar
- Các file PNG đã được tạo sẵn và sẵn sàng sử dụng!

Cách chỉnh sửa và tạo lại hình:

Option 1: Sử dụng Command Line (Đã cài sẵn - Khuyến nghị)
1. Mở terminal/command prompt
2. Chuyển đến thư mục diagrams:
   cd /mnt/e/person/2026-Kite-Class-Platform/diagrams
3. Chạy lệnh:
   ~/jdk/jdk-21/bin/java -jar plantuml.jar architecture-diagram.puml
   ~/jdk/jdk-21/bin/java -jar plantuml.jar provisioning-flow.puml
4. File PNG sẽ được tạo tự động (có thể cần đổi tên)

Option 2: Sử dụng online
1. Truy cập: http://www.plantuml.com/plantuml/uml/
2. Copy nội dung từ file .puml
3. Paste vào editor
4. Click "Submit" để xem preview
5. Download PNG

Option 3: Sử dụng VS Code
1. Cài đặt extension "PlantUML"
2. Mở file .puml trong VS Code
3. Nhấn Alt+D để preview
4. Click chuột phải -> Export -> PNG


================================================================
6. HO TRO VA LIEN HE
================================================================

Neu gap van de khi su dung tai lieu:

1. Kiem tra lai cau truc thu muc
2. Dam bao tat ca cac file deu co mat
3. Su dung text editor ho tro UTF-8 de mo file .txt
4. Su dung Word 2016 tro len de tranh loi format

Chuc ban thanh cong voi do an tot nghiep!


================================================================
PHU LUC: CHECKLIST HOAN THANH BAO CAO
================================================================

[ ] Da copy noi dung tu graduation-thesis-outline.txt vao Word
[ ] Da format font chu, can le, line spacing
[ ] Da tao cac heading cho tieu de
[ ] Da chen 2 hinh so do vao dung vi tri
[ ] Da them caption cho cac hinh
[ ] Da them header/footer
[ ] Da them so trang
[ ] Da kiem tra chinh ta
[ ] Da go dau tieng Viet day du
[ ] Da luu file voi ten phu hop
[ ] Da in thu 1 ban de kiem tra layout


---
KiteClass Platform Documentation
Version: 2.0
Last updated: 2025-12-18
Generated by: Claude Code
