---
title: Chương 1 Phần 3 — Khung pháp lý Việt Nam và phương pháp luận Quality-Driven Development
chapter: 1
section: vn-law-methodology
audience: mixed
last-updated: 2026-05-19
status: draft
---

# Chương 1 — Phần 3: Khung pháp lý Việt Nam và phương pháp luận phát triển cho nền tảng KiteHub

Phần này trình bày hai chủ đề bổ sung cho Chương 1: khung pháp lý Việt Nam tác động đến kiến trúc KiteHub, và phương pháp luận Quality-Driven Development áp dụng trong quá trình phát triển nền tảng dưới điều kiện solo-developer với deadline pháp lý cứng.

---

## Phần A — Khung pháp lý Việt Nam tác động đến nền tảng KiteHub

### 1. Bối cảnh tuân thủ pháp luật trong SaaS giáo dục Việt Nam

Khác với SaaS thông thường ở các thị trường phương Tây nơi GDPR (Liên minh Châu Âu) hoặc CCPA (California, Hoa Kỳ) là chuẩn tham chiếu chính, SaaS hoạt động tại Việt Nam phải tuân thủ một tập hợp văn bản pháp luật riêng được ban hành giai đoạn 2018-2024 nhằm thiết lập khung pháp lý cho không gian mạng, dữ liệu cá nhân và giao dịch điện tử. Đối với một nền tảng giáo dục B2B SaaS, các văn bản này ảnh hưởng đến lựa chọn vendor hạ tầng, cấu trúc database (data localization), thiết kế consent flow, pipeline xuất hóa đơn và format hợp đồng tenant.

Khóa luận này định hướng nền tảng KiteHub tiếp cận theo nguyên tắc **compliance built-in chứ không phải addon** — tức là mọi quyết định kiến trúc đều xem xét tác động pháp lý trước khi cố định phạm vi. Cách tiếp cận này khác với mô hình "ship trước, compliance retrofit sau" phổ biến ở các startup giai đoạn 2018-2022, đặc biệt khi Luật Bảo vệ Dữ liệu Cá nhân 2023 và các nghị định hướng dẫn đã đặt mức phạt hành chính lên đến 5% doanh thu hàng năm cho vi phạm nghiêm trọng theo Nghị định 13/2023/NĐ-CP [22].

### 2. Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023)

Luật Bảo vệ Dữ liệu Cá nhân Số 49/2023/QH15 [21] được Quốc hội thông qua tháng 11/2023, là văn bản pháp lý đầu tiên ở cấp độ Luật quy định riêng về bảo vệ dữ liệu cá nhân tại Việt Nam. Luật có hiệu lực từ **2026-07-01**, đặt deadline cứng cho mọi SaaS hoạt động tại Việt Nam phải hoàn thiện compliance trước thời điểm này.

#### 2.1 Phạm vi áp dụng

Luật áp dụng cho mọi tổ chức xử lý dữ liệu cá nhân của công dân Việt Nam, không phụ thuộc trụ sở của tổ chức đó (extraterritorial scope tương tự GDPR). Nền tảng KiteHub xử lý dữ liệu cá nhân thuộc ba nhóm chính: (1) Owner trung tâm + Manager + Giáo viên (B2B contact data, nền tảng là Data Controller); (2) Học viên + Phụ huynh (B2C indirect via tenant, tenant là Controller, nền tảng là Processor theo Data Processing Agreement); (3) Anonymous prospect (signup form pre-conversion, nền tảng là Controller cho dữ liệu marketing).

#### 2.2 Nghĩa vụ chính của Controller

Luật Bảo vệ Dữ liệu Cá nhân Điều 11 và Điều 17 quy định Controller phải có consent cụ thể-riêng lẻ-có thể rút lại bất cứ lúc nào cho mỗi mục đích xử lý (consent ghép chung bị cấm); bổ nhiệm Data Protection Officer (DPO) khi xử lý dữ liệu nhạy cảm hoặc dữ liệu của ≥10.000 chủ thể; thực hiện Data Protection Impact Assessment (DPIA) trước mỗi tính năng tác động cao đến quyền riêng tư; thông báo vi phạm dữ liệu cho cơ quan có thẩm quyền trong vòng 72 giờ; và đảm bảo các quyền của data subject (truy cập, chỉnh sửa, xóa, hạn chế xử lý, phản đối quyết định tự động hóa).

#### 2.3 Thiết kế nền tảng đáp ứng PDPL

Bảng 1.3.1 dưới đây liệt kê các thành phần kỹ thuật + tổ chức được triển khai để tuân thủ Luật Bảo vệ Dữ liệu Cá nhân trong giai đoạn beta nội bộ:

| Yêu cầu Luật Bảo vệ Dữ liệu Cá nhân | Thiết kế nền tảng |
|---|---|
| Consent cụ thể, riêng lẻ | Form đăng ký có 3 checkbox riêng (Terms / Privacy / Marketing optional) — không tích sẵn |
| Quyền truy cập (Điều 7) | API `GET /api/v1/me/personal-data-export` trả về JSON đầy đủ |
| Quyền xóa (Điều 9) | API `DELETE /api/v1/me/account` kích hoạt soft-delete 30 ngày + hard-delete sau retention |
| DPIA template | Tài liệu kiến trúc DPIA template chuẩn hóa cho mọi tính năng mới |
| Audit log immutable | Bảng `admin_audit_logs` với database trigger immutable (Luật Bảo vệ Dữ liệu Cá nhân Điều 11 tamper-proof) |
| Data breach notification SOP | Runbook ứng phó sự cố data breach với quy trình thông báo 72 giờ |

#### 2.4 Roadmap DPO và DPIA

Giai đoạn beta nội bộ ≤10 tenant chưa kích hoạt ngưỡng PDPL Điều 28 (10.000 data subject) yêu cầu bổ nhiệm DPO bắt buộc. Roadmap dự kiến bổ nhiệm DPO part-time + thực hiện DPIA cho mọi tính năng nhạy cảm trước giai đoạn General Availability (Q3 2026) khi nền tảng mở rộng sang quy mô ≥50 trung tâm. Mở rộng K-12 (giai đoạn tiếp theo) sẽ kích hoạt yêu cầu DPO ngay lập tức do dữ liệu trẻ em (K-12 minor data) thuộc loại nhạy cảm cao theo Điều 17 — quy trình DPO + DPIA + đào tạo compliance đầy đủ phải hoàn tất trước thời điểm kích hoạt.

### 3. Luật An ninh mạng 2018 và Nghị định 53/2022/NĐ-CP — yêu cầu data localization

Luật An ninh mạng Số 24/2018/QH14 [23] có hiệu lực từ 2019-01-01 đặt nền tảng pháp lý cho việc đảm bảo an ninh không gian mạng Việt Nam. Tác động trực tiếp nhất đến SaaS là yêu cầu localize dữ liệu quy định tại Điều 26. Nghị định 53/2022/NĐ-CP [24] (hiệu lực 2022-10-01) hướng dẫn chi tiết Điều 26, đặc biệt khoản về lưu trữ dữ liệu người dùng Việt Nam tại Việt Nam trong tối thiểu 24 tháng cho các nhà cung cấp dịch vụ có (1) Người dùng Việt Nam ≥1 triệu, HOẶC (2) Hoạt động trong lĩnh vực truyền thông / mạng xã hội / thương mại điện tử / thanh toán / vận tải / viễn thông / lưu trữ dữ liệu trực tuyến.

#### 3.1 Quyết định kiến trúc — AWS Singapore (ap-southeast-1)

Nền tảng giáo dục B2B SaaS hoạt động trong lĩnh vực lưu trữ dữ liệu trực tuyến — thuộc danh mục Điều 26 — nên cần chuẩn bị data localization từ trước. Khóa luận chọn AWS Singapore region (ap-southeast-1) cho giai đoạn beta nội bộ thay vì AWS Hà Nội (chưa available cho Free Tier 2026) hoặc AWS US (không phù hợp Nghị định 53/2022 dài hạn).

Phân kỳ kiến trúc theo từng giai đoạn:

- **Giai đoạn beta nội bộ (≤50 tenant):** AWS Singapore chấp nhận được do (a) Singapore là quốc gia ASEAN với điều ước hợp tác data sharing thuận lợi với Việt Nam, (b) chi phí Free Tier giúp khả thi cho mô hình solo-developer, (c) giai đoạn beta invite-only chưa kích hoạt ngưỡng Decree 53 §26 (1M user) hay PDPL Article 28 (10.000 subject).
- **Giai đoạn General Availability (50-500 tenant):** Roadmap migrate sang AWS Asia Pacific (Hanoi) Local Zone HOẶC VN cloud provider (Viettel IDC / VNG Cloud) khi đạt ngưỡng kích hoạt Điều 26 hoặc đạt 200 tenant — tùy điều kiện nào đến trước.
- **Giai đoạn K-12 mở rộng:** Bắt buộc hosting tại Việt Nam do dữ liệu trẻ em + quy mô ≥1 triệu user sẽ vượt mọi ngưỡng kích hoạt Điều 26.

#### 3.2 Cybersecurity baseline

Ngoài data localization, Luật An ninh mạng 2018 yêu cầu các tổ chức cung cấp dịch vụ trên không gian mạng phải áp dụng biện pháp kỹ thuật bảo vệ thông tin (mã hóa, kiểm soát truy cập, audit log), thiết lập phương án ứng phó sự cố, và phối hợp với Cục An ninh mạng và Phòng chống tội phạm công nghệ cao (A05) khi có yêu cầu. Nền tảng đáp ứng các yêu cầu trên qua HTTPS mandatory với TLS 1.3, JWT signed, password Argon2id hashing theo OWASP Top 10 baseline [28]; runbook ứng phó sự cố chuẩn hóa; và audit log immutable với 90 ngày retention cho mọi action quản trị.

### 4. Thông tư 78/2021/TT-BTC — Hóa đơn điện tử

Thông tư 78/2021/TT-BTC [25] của Bộ Tài chính (hiệu lực 2022-07-01) bắt buộc mọi doanh nghiệp hoạt động tại Việt Nam phải xuất hóa đơn điện tử (eInvoice) theo định dạng XML chuẩn của Tổng cục Thuế thay vì hóa đơn giấy truyền thống. Định dạng eInvoice phải có chữ ký số và được nộp về Tổng cục Thuế thông qua nhà cung cấp dịch vụ trung gian được Tổng cục Thuế cấp phép.

#### 4.1 Quyết định kiến trúc — Partnership thay vì self-build

Nền tảng giáo dục B2B SaaS hỗ trợ tenant xuất hóa đơn cho học viên và doanh nghiệp (corporate sponsorship). Mỗi giao dịch thanh toán học phí cần kèm eInvoice hợp lệ theo Thông tư 78. Khóa luận đánh giá hai phương án: self-build eInvoice engine vs. partnership với nhà cung cấp được cấp phép (MISA MeInvoice).

Phương pháp **external benchmark** (so sánh ngành — chi tiết trong Phần B §3) cho thấy partnership là lựa chọn industry-norm cho VN edu SaaS thay vì self-build. Lý do chính: (1) MISA là nhà cung cấp được Tổng cục Thuế cấp phép với network distribution thiết lập sẵn, tránh quy trình apply giấy phép trung gian phức tạp; (2) API hỗ trợ XML format chuẩn Tổng cục Thuế, chữ ký số, retry on failure — tiết kiệm 3-4 tháng engineering effort; (3) Compliance maintenance ongoing — khi Tổng cục Thuế cập nhật format, nhà cung cấp tự động update; (4) Cost-benefit: phí ~2.000-5.000đ/invoice tùy volume, so với chi phí self-build + maintain ~50-100 triệu đồng/năm engineering — partnership rẻ hơn đáng kể ở quy mô beta + early GA.

### 5. Nghị định 13/2023/NĐ-CP và 147/2024/NĐ-CP

#### 5.1 Nghị định 13/2023/NĐ-CP — hướng dẫn PDPL chi tiết

Nghị định 13/2023/NĐ-CP [22] ban hành 2023-04-17 hướng dẫn thi hành Luật Bảo vệ Dữ liệu Cá nhân với các điểm quan trọng: phân loại dữ liệu cá nhân thành hai nhóm (cơ bản và nhạy cảm); yêu cầu DPIA cho mọi xử lý dữ liệu cá nhân nhạy cảm không phụ thuộc quy mô; mức phạt hành chính từ 50 triệu đồng đến 100 triệu đồng cho vi phạm hành chính, lên đến 5% doanh thu năm trước cho vi phạm nghiêm trọng; cross-border data transfer phải thông báo Bộ Công An ≥60 ngày trước (nền tảng hosting AWS Singapore là cross-border transfer cần thông báo trước thời điểm hiệu lực Luật Bảo vệ Dữ liệu Cá nhân 2026-07-01).

#### 5.2 Nghị định 147/2024/NĐ-CP — giao dịch điện tử

Nghị định 147/2024/NĐ-CP ban hành cuối 2024 [39] cập nhật khung pháp lý cho giao dịch điện tử giai đoạn 2024-2030: hợp đồng điện tử giữa tenant và nhà cung cấp dịch vụ phải có chữ ký số hợp lệ hoặc xác thực 2 yếu tố (2FA); hồ sơ điện tử lưu trữ tối thiểu 5 năm cho mục đích kiểm toán + tranh chấp; identity verification (KYC) — tenant Owner phải xác thực danh tính qua CCCD/CMND hoặc tài khoản định danh điện tử Việt Nam (VNeID) khi đăng ký B2B SaaS. Nền tảng triển khai 2FA cho Owner login, audit log immutable 5 năm retention, và roadmap tích hợp VNeID khi API công khai được Bộ Công An cung cấp.

---

## Phần B — Phương pháp luận Quality-Driven Development cho nền tảng KiteHub

### 1. Bối cảnh phương pháp luận

Phát triển một nền tảng SaaS multi-tenant trong môi trường solo-developer với deadline pháp lý cứng (Luật Bảo vệ Dữ liệu Cá nhân hiệu lực 2026-07-01) đặt ra thách thức quản lý chất lượng khác biệt so với team SaaS truyền thống. Không có code reviewer thứ hai, không có đội QA riêng, không có architecture review board — mọi quyết định kiến trúc, nghiệp vụ và chất lượng đều dồn lên một người. Trong bối cảnh này, khóa luận áp dụng phương pháp luận **Quality-Driven Development** kết hợp bốn nguyên tắc nền tảng đã được nghiên cứu trong văn liệu khoa học phần mềm.

Cách tiếp cận này kế thừa và mở rộng các phương pháp đã có:

- **Plan-Do-Check-Act (PDCA) cycle** của Deming [45] — vòng lặp liên tục phát hiện sai lệch và cải tiến quy trình
- **Test-Driven Development (TDD)** của Beck [18] — viết test trước implementation cho từng chức năng
- **Lean Software Development** của Poppendieck [46] — eliminate waste, build quality in, amplify learning
- **IEEE 730-2014 Software Quality Assurance Standard** [47] — chuẩn quy trình đảm bảo chất lượng phần mềm

Tuy nhiên TDD truyền thống tập trung unit test trước implementation cho từng chức năng cụ thể, còn phương pháp luận khóa luận đề xuất tập trung vào **Quality Management Process** ở cấp meta-governance — làm thế nào để đảm bảo bản thân quy trình phát triển không bị drift theo thời gian, không bỏ sót các lớp lỗi nghiệp vụ, và mọi sai lệch đều convert thành quy trình + enforcement vĩnh viễn thay vì chỉ ghi chú tạm thời. Quy trình này bù trừ điểm yếu cố hữu của mô hình solo-developer — thiếu code reviewer thứ hai — bằng cách codify mọi miss thành rule có enforcement tự động.

Bốn trụ cột chính của phương pháp luận, mỗi trụ cột địa chỉ một lớp lỗi đặc trưng, được trình bày sau đây.

### 2. Trụ cột 1 — Quy trình Incident-to-Rule

Trụ cột này áp dụng PDCA cycle của Deming [45] ở mức process governance: mọi sai lệch (incident) mà nhà phát triển hoặc người review phát hiện phải được chuyển hóa thành **rule + enforcement vĩnh viễn trong cùng phiên làm việc phát hiện**, không để dồn vào backlog.

Quy trình gồm năm giai đoạn tuần tự: (1) DETECT — phát hiện sai lệch; (2) CLASSIFY — phân loại (rule thiếu, rule có nhưng thiếu enforcement, hay rule conflict); (3) RULE + ENFORCE — viết rule mới kèm cơ chế phát hiện tự động (Git hook, CI check, reviewer checklist) trong cùng pull request; (4) SELF-TEST — chạy detection mới trên sai lệch gốc bằng synthetic fixture để xác nhận rule fire đúng; (5) RETRO LOG — ghi nhận incident-to-rule chain vào lịch sử dự án.

Nguyên tắc cốt lõi: **không cho phép advisory-only rules**. Rule không có cơ chế enforcement tự động sẽ drift, bị quên, và re-trigger sai lệch tương tự sau vài tuần. Yêu cầu rule + enforcement same-PR là biến thể của "build quality in" của Poppendieck [46] — chất lượng phải được nhúng vào quy trình ngay tại thời điểm xây dựng, không phải retrofit sau.

### 3. Trụ cột 2 — Meta-Index Governance Pattern

Trụ cột này áp dụng nguyên tắc Single Source of Truth từ IEEE 730 [47] §6 (Quality Management documentation): mọi tập hợp artifact có ID tuần tự (gap, Architecture Decision Record, rule, audit report) PHẢI có một CSV index canonical kèm query helper, CI validator và 100% coverage parity giữa file và index.

Vấn đề được giải quyết: khi codebase scale ≥100 gap, ≥30 ADR, ≥60 rule, mọi truy vấn dạng "artifact nào còn open" hoặc "rule nào touching scope X" trở nên expensive nếu scan toàn bộ file system mỗi lần. Tệ hơn, status thường drift giữa file body và tài liệu kế hoạch — không có single source of truth.

Pattern giải quyết: mỗi loại artifact có một CSV index lưu metadata cốt lõi (id, title, status, phase, owner, last-updated). CI validator quét cả hai hướng (file ↔ CSV row) và fail nếu drift, tránh được lớp lỗi "ghost artifact" (file tồn tại nhưng không trong index) hoặc "phantom row" (row CSV trỏ về file đã xóa).

### 4. Trụ cột 3 — Outside-In Coverage Trigger

Trụ cột này áp dụng nguyên tắc "amplify learning" của Lean Software Development [46]: khi nhà phát triển đề xuất phạm vi mới theo lối inside-out brainstorm ("liệt kê tính năng có sẵn" hoặc "build engine X"), quy trình PHẢI proactively yêu cầu bổ sung outside-in audit trước khi cố định phạm vi.

Inside-out là "dev liệt kê tính năng dev có hoặc dev nghĩ tới" — góc nhìn từ trong hệ thống ra ngoài. Outside-in là "user thực sự cần gì, kỳ vọng gì, bị cản ở đâu" — góc nhìn từ ngoài (user, ngành) vào hệ thống. Hai góc nhìn này bù trừ nhau, không thay thế. Dev giỏi inside-out (biết hệ thống) nhưng yếu outside-in (đã quá quen, có blind spot tâm lý user).

Quy trình đề xuất ba phương pháp outside-in chạy song song:

| Phương pháp | Phù hợp khi |
|---|---|
| Mô phỏng nhân vật (Persona Simulation) | Phạm vi user-facing (signup, onboarding, daily use) |
| So sánh ngành (External Benchmark) | Pre-launch, beta cohort, business model decisions |
| Ma trận tìm sai lệch (Failure-Mode Matrix) | Quy trình phức tạp với nhiều failure modes |

Một ví dụ minh họa trụ cột này: gap ban đầu được đề xuất theo phong cách inside-out "self-build VAT engine theo Thông tư 78". External benchmark cho thấy partnership với nhà cung cấp được Tổng cục Thuế cấp phép (MISA MeInvoice) là industry-norm cho VN edu SaaS, tiết kiệm 3-4 tháng engineering effort và tránh quy trình xin giấy phép trung gian phức tạp. Decision re-scope từ self-build → partnership được áp dụng nhờ outside-in trigger fire tại gap-filing time, tránh 3-4 tháng wasted implementation.

### 5. Trụ cột 4 — Audit-to-Gap Pipeline

Trụ cột này áp dụng nguyên tắc traceability của IEEE 730 [47] §7 (Quality Assurance Records): mỗi audit finding phải có một gap file tương ứng với template chuẩn, được index trong CSV, có memory pointer (nếu là lớp lỗi recurring), và được close bằng fix PR theo lifecycle OPEN → PARTIAL → DONE.

Quy trình cụ thể: (1) Run audit — skill emit findings vào audit category folder với date prefix chuẩn; (2) Triage findings — phân loại P0 BLOCKING / P1 / P2 / informational; (3) File gap per finding (chỉ new finding, không duplicate) — mỗi gap file follow template canonical với các trường Problem / Root Cause / Proposed Fix / Acceptance Criteria / Status / Log / Related; (4) Add row to gap status CSV với status OPEN; (5) Memory pointer optional nếu finding là lớp lỗi recurring cần cross-session awareness; (6) Fix PR — gap file Status flip OPEN → PARTIAL → DONE qua các PR lifecycle.

Trước khi filing gap mới, quy trình mandate state-check trên bốn trục: (a) đọc prior audit reports cùng category để tránh duplicate finding; (b) verify gap pending từ wave trước đã được file đầy đủ; (c) verify Architecture Decision Record liên quan đã được approved; (d) verify state environment match expectation trước khi đề xuất mutation. State-check eliminate hai lớp lỗi phổ biến: duplicate gap filing và phantom gap referencing non-existent state.

### 6. Tổng hợp lưới an toàn meta-governance

Bốn trụ cột không hoạt động độc lập — chúng tạo thành lưới an toàn nhiều lớp, mỗi trụ cột địa chỉ một lớp lỗi cụ thể:

| Lớp lỗi | Trụ cột địa chỉ |
|---|---|
| Sai lệch được catch nhưng không codify thành quy trình | Trụ cột 1 — Incident-to-Rule pipeline |
| Drift giữa file body và index/CSV | Trụ cột 2 — Meta-Index governance |
| Phạm vi cố định bỏ qua nhu cầu user thực | Trụ cột 3 — Outside-In coverage trigger |
| Audit finding bị quên không track | Trụ cột 4 — Audit-to-Gap pipeline |

Phương pháp luận này được áp dụng nhất quán qua nhiều iteration phát triển nền tảng. Kết quả định lượng (chi tiết Chương 6 — Testing và Evaluation): Quality audit cải thiện từ baseline 65/100 lên 90/100; Security audit từ 85/100 lên 93/100; trung bình 3-5 gap closure mỗi iteration với 0 P0 incident production trong hai tuần liên tiếp pre-launch giai đoạn beta nội bộ.

---

## Kết luận Phần 3 Chương 1

Phần A đã trình bày năm văn bản pháp luật Việt Nam quan trọng nhất ảnh hưởng đến nền tảng SaaS giáo dục: Luật Bảo vệ Dữ liệu Cá nhân 2023 (hiệu lực 2026-07-01), Luật An ninh mạng 2018 và Nghị định 53/2022/NĐ-CP (data localization), Thông tư 78/2021/TT-BTC (hóa đơn điện tử partnership), Nghị định 13/2023/NĐ-CP (DPIA và breach notification), Nghị định 147/2024/NĐ-CP (electronic contract và 5-year retention). Các quyết định kiến trúc chính rút ra: AWS Singapore cho giai đoạn beta nội bộ với roadmap migrate VN cloud trước General Availability; partnership với nhà cung cấp hóa đơn điện tử được Tổng cục Thuế cấp phép thay vì self-build; DPIA template chuẩn hóa cho mọi tính năng nhạy cảm; 2FA bắt buộc cho Owner login; và audit log immutable với 5 năm retention.

Phần B đã codify bốn trụ cột phương pháp luận Quality-Driven Development kế thừa từ Plan-Do-Check-Act của Deming [45], Test-Driven Development của Beck [18], Lean Software Development của Poppendieck [46] và IEEE 730-2014 [47]: quy trình Incident-to-Rule, Meta-Index Governance Pattern, Outside-In Coverage Trigger, và Audit-to-Gap Pipeline. Bốn trụ cột này hoạt động bù trừ tạo lưới an toàn meta-governance, cho phép một solo-developer maintain chất lượng consistent qua nhiều iteration phát triển mà không drift theo thời gian. Cách tiếp cận này khác biệt với TDD truyền thống ở chỗ tập trung vào meta-governance ở mức quy trình thay vì chỉ unit-level testing.

Các chương sau sẽ tham chiếu chi tiết: Chương 2 (Kiến trúc Hệ thống) giải thích cách multi-tenant single-bucket isolation đáp ứng Luật Bảo vệ Dữ liệu Cá nhân Điều 11 (tamper-proof) và Row-Level Security với NULL force-fail; Chương 4 (Triển khai) trình bày JWT authentication, Outbox pattern và immutable audit logs migration; Chương 6 (Testing và Evaluation) định lượng kết quả audit và persona review findings qua các iteration phát triển.
