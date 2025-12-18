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

2.1. SO DO KIEN TRUC (architecture-diagram.png)
------------------------------------------------------------
Noi dung:
- So do tong quan kien truc KiteClass Platform
- The hien KiteHub (Modular Monolith) va KiteClass Instances (Microservices)
- Cac services, database, AWS infrastructure

Cach su dung:
1. Mo file architecture-diagram.png bang trinh xem anh
2. Chen vao Word/PowerPoint de trinh bay
3. Su dung trong bao cao de tai minh hoa kien truc
4. Khuyen nghi: Dat o phan "Kien truc he thong"

Luu y:
- So do da duoc tao tu file .puml (PlantUML)
- Neu can chinh sua, edit file architecture-diagram.puml
- Tao lai hinh bang lenh: plantuml -tpng architecture-diagram.puml
  (hoac su dung online: http://www.plantuml.com/plantuml)


2.2. SO DO FLOW PROVISION (provisioning-flow.png)
------------------------------------------------------------
Noi dung:
- Quy trinh day du de mo mot KiteClass node moi
- Bao gom cac buoc: Order -> AI Generation -> Infrastructure Provisioning
- The hien ro rang cac actor, services tham gia va thoi gian xu ly

Cach su dung:
1. Mo file provisioning-flow.png
2. Chen vao bao cao o phan "Quy trinh mo node KiteClass"
3. Ket hop voi phan giai thich chi tiet trong graduation-thesis-outline.txt
4. Su dung khi trinh bay demo flow hoat dong

Luu y:
- So do sequence diagram de hieu flow theo thoi gian
- Chua thong tin ve cost va time estimate
- Co the chinh sua file provisioning-flow.puml neu can


2.3. DE CUONG DO AN (graduation-thesis-outline.txt)
------------------------------------------------------------
Noi dung:
- Bao cao hoan chinh ve de tai (uoc tinh 4 trang khi in)
- 5 phan chinh:
  + Phan 1: Y tuong de tai
  + Phan 2: Kien truc he thong va ly do lua chon
  + Phan 3: Quy trinh mo node KiteClass voi AI Agent
  + Phan 4: Diem manh cua de tai
  + Phan 5: Thu thach cua de tai

Cach su dung:
1. Mo file bang Notepad hoac text editor bat ky
2. COPY TOAN BO noi dung
3. PASTE vao Microsoft Word
4. Format lai:
   - Chon font chu phu hop (VD: Times New Roman 13pt)
   - Can le justify
   - Tao tieu de (Heading 1, 2, 3)
   - Chen 2 so do .png vao vi tri phu hop
   - Them so trang, header, footer
5. Luu thanh file .docx

Luu y:
- File duoc viet KHONG CO MARKDOWN SYNTAX de de copy vao Word
- Tieng Viet KHONG DAU de tranh loi font
- Can format lai trong Word de dep hon
- Kich thuoc uoc tinh: 4 trang (bao gom 2 hinh so do)


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
5. CAC FILE PLANTUMA (.puml)
================================================================

File .puml la PlantUML source code, dung de tao so do.

Cach chinh sua va tao lai hinh:

Option 1: Su dung online (De nhat)
1. Truy cap: http://www.plantuml.com/plantuml/uml/
2. Copy noi dung tu file .puml
3. Paste vao editor
4. Click "Submit" de xem preview
5. Download PNG

Option 2: Su dung VS Code
1. Cai dat extension "PlantUML"
2. Mo file .puml trong VS Code
3. Nhan Alt+D de preview
4. Click chuot phai -> Export -> PNG

Option 3: Command line (Can cai Java va PlantUML)
1. Cai dat Java JDK
2. Download plantuml.jar
3. Chay: java -jar plantuml.jar file.puml
4. Se tao ra file.png


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
