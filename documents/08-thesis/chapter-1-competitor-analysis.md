---
title: Chương 1 §1.1 Hiện trạng và §1.2 Bài toán — Tổng quan đề tài và khảo sát thị trường
chapter: 1
section: hien-trang-bai-toan
audience: mixed
last-updated: 2026-05-26
status: draft
---

# Chương 1 — Tổng quan về bài toán và các công nghệ, công cụ

## 1.1 Hiện trạng

Thị trường phần mềm quản lý trung tâm giáo dục Việt Nam tăng trưởng mạnh giai đoạn 2020-2025, được thúc đẩy bởi ba yếu tố chính. Thứ nhất, ngành dạy thêm (trung tâm ngoại ngữ, tin học, năng khiếu) bùng nổ sau Thông tư 29/2024/TT-BGDĐT chính thức hóa hoạt động dạy thêm có thu phí [1], ước tính hơn 50.000 trung tâm hoạt động trên toàn quốc theo báo cáo công khai của Magenest 2024 [2] (truy cập ngày 20/05/2026). Thứ hai, phụ huynh Việt Nam có thói quen đầu tư mạnh cho giáo dục con cái, với mức chi trung bình 15-20% thu nhập hộ gia đình cho học thêm con theo công bố trên website của 6Wresearch [3] (truy cập ngày 20/05/2026) — chỉ số này phù hợp với báo cáo Kinh tế Số Việt Nam 2024 của VECITA về tăng trưởng chi tiêu EdTech trong cấu phần kinh tế số [4] (truy cập ngày 20/05/2026). Thứ ba, sau đại dịch COVID-19, các trung tâm buộc phải số hóa quy trình quản lý (điểm danh, học phí, lịch học, kênh liên lạc với phụ huynh) để duy trì hoạt động khi liên tục chuyển đổi giữa chế độ trực tuyến và trực tiếp.

Tuy nhiên, đa số trung tâm nhỏ và vừa (1-3 chi nhánh, dưới 500 học viên) vẫn dùng Excel kết hợp Zalo nhóm, thậm chí sổ ghi tay để quản lý. Lý do chính: phần mềm hiện có hoặc quá phức tạp (Cyber School, MISA EMIS [5] hướng đến trường công lập K-12), hoặc thiếu UX tiếng Việt (LMS quốc tế như Moodle, Canvas), hoặc chi phí cao không phù hợp với phân khúc trung tâm tự phát (Speed Manager, EduCom với mức 50-100 USD/tháng/cơ sở).

### 1.1.1 Bối cảnh thị trường và mô hình học hỗn hợp

Trung tâm dạy thêm tại Việt Nam vận hành theo mô hình học hỗn hợp (B-learning, blended learning) — kết hợp lớp học sau giờ chính khóa và cuối tuần, phân biệt với trường công lập chính khóa. Thị trường mục tiêu của đồ án là các trung tâm vừa và nhỏ với 50-500 học sinh; phạm vi K-12 (giáo dục phổ thông từ mầm non đến lớp 12) trường công có lớp tuân thủ pháp lý riêng (cán bộ bảo vệ dữ liệu DPO, đánh giá tác động bảo vệ dữ liệu DPIA, kiểm tra an ninh) được lùi sang lộ trình phát triển sau.

Đặc điểm mô hình học hỗn hợp tại Việt Nam theo Báo cáo Kinh tế Số Việt Nam 2024 [4, tr.42]: *"Hơn 90% phụ huynh đô thị sử dụng Zalo group chat làm kênh chính trao đổi với trung tâm; email phục vụ tài liệu chính thức như hóa đơn và báo cáo."* Buổi học buổi tối và cuối tuần chiếm ưu thế (thứ 2 đến thứ 7 từ 17:00 đến 21:00; thứ 7 và Chủ nhật từ 8:00 đến 17:00). Niên khóa tháng 9 đến tháng 5 (năm học `2025-2026` ứng tháng 9/2025 đến tháng 5/2026) gồm học kỳ 1 (tháng 9 đến tháng 12), học kỳ 2 (tháng 1 đến tháng 5) và học kỳ hè (tháng 6 đến tháng 8). Mẹ là đầu mối liên lạc chính cho việc học của con (~60%), bố dự phòng (35%), ông bà (5%). Khung Tết Nguyên Đán nghỉ 7-10 ngày cuối tháng 1, đầu tháng 2 yêu cầu lịch chạy tự động tính phí bỏ qua. Hệ thống đã hỗ trợ email cho tài liệu chính thức và đã kết nối kênh Zalo OA cho liên lạc với phụ huynh.

### 1.1.2 Phân tích nhóm người dùng đại diện hiện tại

Đồ án tập trung bốn nhóm người dùng đại diện. **P1 — Giáo viên độc lập** (28 tuổi, 5-50 học sinh, dạy IELTS/Toán) thay thế sổ tay giấy, Excel và Zalo thủ công bằng nền tảng tập trung, gói `FREE` đủ dùng. **P2 — Chủ sở hữu trung tâm** (35 tuổi, 20-100 học sinh, 2-5 giáo viên) thay ba công cụ rời rạc bằng nền tảng tích hợp, gói `STARTER` `500.000đ/tháng`. **P3 — Quản lý trung tâm** (24 tuổi, 100-500 học sinh, 5-15 giáo viên) cần nhập hàng loạt từ tệp CSV khoảng 300 dòng, phân quyền theo vai trò (vai trò quản lý không thấy mục thanh toán), nhật ký kiểm toán đầy đủ, gói `PRO` `1.500.000đ/tháng`. **Phụ huynh và học sinh** — phụ huynh nhận thông báo email cho tài liệu chính thức và nhận cập nhật thường xuyên qua nhóm Zalo OA đã được kết nối; học sinh truy cập qua thiết bị di động chiếm 85% phiên.

## 1.2 Khảo sát

Để xác định cơ hội và định vị sản phẩm, đề tài tiến hành khảo sát hai khía cạnh bổ trợ: các sản phẩm phần mềm hiện có trên thị trường, và nhu cầu thực tế từ các nhóm người dùng cuối. Phần khảo sát các hệ thống tham khảo dưới đây phân tích bốn sản phẩm tiêu biểu — BeeClass (ứng dụng gamification quản lý điểm thi đua), cùng ba phần mềm quản lý trung tâm Mona eLMS, Easy Edu và DotB — theo các tiêu chí phân khúc mục tiêu, mức giá, kiến trúc, khả năng tích hợp AI và mức tuân thủ pháp luật Việt Nam. BeeClass được đưa vào như đại diện nhóm công cụ nhẹ — miễn phí phục vụ tương tác lớp học, tương phản với nhóm phần mềm quản lý nghiệp vụ trung tâm.

### 1.2.1 BeeClass — ứng dụng gamification quản lý điểm thi đua

*BeeClass* [39] là ứng dụng quản lý điểm thi đua của học sinh phổ biến tại Việt Nam, do nhóm phát triển trong nước vận hành với khẩu hiệu "Lớp học Hạnh phúc". Khác với nhóm phần mềm quản lý trung tâm, BeeClass định vị là công cụ gamification (game hóa) nhẹ cho giáo viên: thầy cô quy ước các tiêu chí thưởng — phạt (hăng hái phát biểu, làm bài đầy đủ, đi học trễ, nói chuyện riêng…) rồi tích/trừ điểm cho học sinh, kèm hình đại diện sinh động nhằm tạo động lực học tập. Theo thông tin công bố trên website chính thức [39] (truy cập ngày 30/05/2026), sản phẩm hướng tới giáo viên chủ nhiệm, lớp tiếng Anh, lớp ngoại khóa và nhóm học trẻ em.

![Giao diện trang chủ BeeClass](screenshots/competitors/beeclass-homepage.png)

**Hình 1.1.** Giao diện trang chủ BeeClass — ứng dụng gamification quản lý điểm thi đua học sinh.
*Nguồn: https://beeclass.net, truy cập ngày 30/05/2026.*

Tập tính năng chính của BeeClass gồm: điểm danh từng buổi học (kèm gọi tên ngẫu nhiên), tích điểm thưởng — phạt theo tiêu chí, thống kê thành tích theo tuần/tháng, ghi nhận xét mỗi buổi và cho phụ huynh xem qua liên kết chia sẻ. Thế mạnh là giao diện tiếng Việt đơn giản, đăng nhập bằng tài khoản Gmail dùng ngay trên mọi thiết bị, miễn phí ở mức cơ bản và dễ tương tác với phụ huynh. Tuy nhiên, BeeClass thuộc một phân khúc khác với đề tài: sản phẩm tập trung vào game hóa lớp học, **không cung cấp các nghiệp vụ quản lý kinh doanh** mà trung tâm và giáo viên dạy thêm cần — không có quản lý học phí, đối soát thanh toán, hóa đơn, báo cáo doanh thu, xếp lịch lớp định kỳ hay quản lý đa chi nhánh. Việc BeeClass được giáo viên đón nhận cho thấy nhu cầu công cụ nhẹ, miễn phí, tự phục vụ; nhưng khoảng trống nghiệp vụ quản lý dạy-thêm vẫn bỏ ngỏ — đây chính là phần đề tài hướng tới.

### 1.2.2 Mona eLMS — chuyên ngoại ngữ và tin học

*Mona eLMS* [6] là sản phẩm của công ty Mona Software thành lập năm 2017, tập trung chuyên biệt vào phân khúc trung tâm ngoại ngữ và tin học. Theo công bố trên website chính thức [6] (truy cập ngày 20/05/2026), Mona eLMS có khoảng 800 khách hàng trung tâm tại Việt Nam tính đến năm 2024.

![Giao diện trang chủ Mona eLMS](screenshots/competitors/mona-elms.png)

**Hình 1.2.** Giao diện trang chủ Mona eLMS — phần mềm quản lý trung tâm ngoại ngữ và tin học.
*Nguồn: https://mona.solutions, truy cập ngày 20/05/2026.*

Thế mạnh nổi bật của Mona là ứng dụng di động native iOS và Android cho học viên và phụ huynh, tích hợp Zalo Notification Service (ZNS) cho thông báo điểm danh, kết quả thi và lịch học — điểm chạm rất quan trọng với phụ huynh Việt Nam khi Zalo chiếm hơn 90% thị phần ứng dụng nhắn tin smartphone. Điểm yếu: hệ sinh thái đóng (không có API công khai), single-tenant theo từng trung tâm, mức giá 1,5-5 triệu đồng/tháng tùy số học viên với cơ chế báo giá tùy chỉnh không minh bạch.

### 1.2.3 Easy Edu — phân khúc trung tâm ngoại ngữ vừa và nhỏ

*Easy Edu* [7] là hệ thống lớn nhất trên thị trường trong phân khúc trung tâm ngoại ngữ vừa và nhỏ với hơn 1.400 trung tâm khách hàng theo công bố trên website chính thức [7] (truy cập ngày 20/05/2026). Sản phẩm ra mắt năm 2018, có hệ thống phân phối mạnh tại miền Bắc và miền Trung thông qua các sự kiện ngành giáo dục và hợp tác với hiệp hội trung tâm ngoại ngữ.

![Giao diện trang chủ Easy Edu](screenshots/competitors/easy-edu.png)

**Hình 1.3.** Giao diện trang chủ Easy Edu — phần mềm quản lý trung tâm ngoại ngữ phổ biến phân khúc vừa và nhỏ.
*Nguồn: https://easyedu.vn, truy cập ngày 20/05/2026.*

Tập tính năng đầy đủ gồm quản lý học viên, lớp học, học phí, điểm danh, báo cáo, tích hợp Zalo OA, ứng dụng di động cho phụ huynh, với mức giá phải chăng từ 800.000 đồng/tháng cho gói cơ bản phục vụ 200 học viên. Điểm yếu chính: kiến trúc đơn tenant theo từng trung tâm (khó mở rộng nhượng quyền), không có khả năng tự sinh tài nguyên thương hiệu bằng AI, quy trình khởi tạo yêu cầu liên hệ bộ phận kinh doanh chứ chưa tự phục vụ.

### 1.2.4 DotB — phân khúc tầm trung và trường tư thục

*DotB* [8] là sản phẩm phần mềm quản lý giáo dục đa năng của công ty DotB Vietnam ra mắt năm 2019, hướng đến phân khúc trung tâm tầm trung và trường tư thục với giá trị thương vụ trung bình 3-8 triệu đồng/tháng theo bảng giá công bố trên website chính thức [8] (truy cập ngày 20/05/2026).

![Giao diện trang chủ DotB](screenshots/competitors/dotb.png)

**Hình 1.4.** Giao diện trang chủ DotB — phần mềm quản lý giáo dục đa năng phân khúc tầm trung.
*Nguồn: https://dotb.vn, truy cập ngày 20/05/2026.*

Thế mạnh đặc thù của DotB là module CRM tích hợp phục vụ quản lý khách hàng tiềm năng (theo dõi học viên triển vọng từ biểu mẫu hỏi thông tin, qua lớp học thử, đến đăng ký chính thức) và tích hợp sẵn ba cổng thanh toán VNPay, MoMo, ZaloPay — giải quyết điểm đau của khoảng 80% trung tâm vẫn dùng chuyển khoản ngân hàng kèm đối soát thủ công. Điểm yếu: mức giá cao gấp 3-5 lần Easy Edu, định vị tầm trung không phục vụ phân khúc trung tâm tự phát quy mô nhỏ — không cạnh tranh trực tiếp với khoảng trống thị trường mà đề tài hướng đến.

### 1.2.5 Khảo sát nhu cầu sử dụng từ phía người dùng cuối

Bên cạnh khảo sát các hệ thống đang có trên thị trường, đề tài tổng hợp nhu cầu sử dụng từ năm nhóm end-user dựa trên các báo cáo ngành công khai. Phân tích này giúp xác định những tính năng cốt lõi cần ưu tiên trong định hướng triển khai ưu tiên.

*Chủ sở hữu trung tâm (Center Owner):* theo các báo cáo Magenest 2024 [2] và 6Wresearch 2024 [3] (cả hai truy cập ngày 20/05/2026), chủ sở hữu trung tâm vừa và nhỏ (1-10 chi nhánh, 100-2000 học viên) ưu tiên ba nhu cầu chính: (i) quản lý học phí và đối chiếu thanh toán tự động (khoảng 80% trung tâm vẫn dùng chuyển khoản ngân hàng kèm đối soát thủ công, chiếm 4-6 giờ/tuần làm việc văn phòng — theo báo cáo VECITA 2024 [4] truy cập ngày 20/05/2026); (ii) báo cáo doanh thu, tỷ lệ giữ chân học viên, chi phí vận hành theo thời gian thực để ra quyết định mở hoặc đóng lớp; (iii) chi phí phần mềm thấp dưới 1,5 triệu đồng/tháng phù hợp biên lợi nhuận hiện hành 25-30%.

*Quản lý trung tâm (Center Manager):* nhu cầu chính tập trung vào quy trình vận hành hàng ngày — điểm danh tự động, lịch giảng dạy linh hoạt khi giáo viên thay ca, thông báo tự động cho phụ huynh khi học viên vắng mặt hoặc nghỉ học liên tiếp. Báo cáo VECITA 2024 [4] (truy cập ngày 20/05/2026) về kinh tế số trong giáo dục cho thấy khoảng 65% trung tâm gặp khó khăn vận hành khi vượt qua mốc 300 học viên do thiếu công cụ phân quyền và quy trình chuẩn hóa giữa các chi nhánh.

*Giáo viên độc lập (Solo Teacher):* nhóm giáo viên dạy thêm tự do (1-50 học viên) cần công cụ nhẹ, chi phí thấp dưới 500.000 đồng/tháng, ưu tiên: lịch học cá nhân, biểu mẫu thu học phí qua chuyển khoản kèm xác nhận tự động, gửi tài liệu học tập qua kênh quen thuộc (Zalo, email). Thông tư 29/2024/TT-BGDĐT [1] công nhận hợp pháp dạy thêm có thu phí mở ra phân khúc này, ước tính 50.000-100.000 giáo viên độc lập trên toàn quốc — tuy nhiên các hệ thống đang có trên thị trường đều định vị cho trung tâm tổ chức, chưa phục vụ nhóm cá nhân.

*Phụ huynh:* phụ huynh đầu tư 15-20% thu nhập hộ gia đình cho học thêm con [3], do đó nhu cầu minh bạch về tiến độ học tập và tài chính. Các nhu cầu cụ thể bao gồm: thông báo điểm danh hàng ngày, báo cáo kết quả học tập định kỳ hai tuần, hóa đơn điện tử có thể tra cứu lại, kênh liên lạc trực tiếp với giáo viên qua Zalo (90% phụ huynh dùng Zalo). Báo cáo Magenest 2024 [2] nhận định Zalo là kênh giao tiếp dominant giữa trung tâm và phụ huynh, vượt SMS và email.

*Học viên:* nhóm học viên (đa số là thanh thiếu niên 10-18 tuổi) cần truy cập tài liệu học tập, lịch học cá nhân, lịch bài kiểm tra và thông báo điểm danh trên thiết bị di động. Báo cáo VECITA 2024 [4] (truy cập ngày 20/05/2026) cho biết khoảng 92% học viên Việt Nam có smartphone từ độ tuổi 12; tuy nhiên các hệ thống đang có trên thị trường chủ yếu phục vụ phía quản trị, chưa thiết kế giao diện riêng cho học viên độc lập với phụ huynh.

Tổng hợp năm nhóm end-user cho thấy nhu cầu chung là một nền tảng phục vụ nhiều nhóm vai trò khác nhau (đa nhóm người dùng đại diện) với phân quyền rõ ràng theo vai trò, giá thấp phù hợp phân khúc nhỏ và vừa, tích hợp Zalo cho kênh giao tiếp với phụ huynh — tất cả các tiêu chí này đều được xem xét trong định hướng kiến trúc hệ thống đề xuất.

### 1.2.6 Bảng so sánh tổng hợp

Bảng 1.1 đối chiếu trực tiếp bốn hệ thống tham khảo với hệ thống đề xuất trên các tiêu chí cạnh tranh cốt lõi: phân khúc mục tiêu, khoảng giá, kiến trúc đa tenant, mức độ tự động hóa và khả năng tuân thủ pháp luật Việt Nam.

**Bảng 1.1.** So sánh bốn hệ thống tham khảo với hệ thống đề xuất theo các tiêu chí quan trọng.

| Tiêu chí | BeeClass | Mona eLMS | Easy Edu | DotB | Hệ thống đề xuất |
|---|---|---|---|---|---|
| Persona mục tiêu | GV cá nhân (gamification) | Trung tâm ngoại ngữ | Trung tâm ngoại ngữ nhỏ | Trung tâm tầm trung | Trung tâm nhỏ và vừa |
| Quản lý nghiệp vụ (học phí/doanh thu/lịch) | Không | Có | Có | Có | Có |
| Khoảng giá (đ/tháng) | Miễn phí cơ bản | 1,5-5 triệu | 800.000-3 triệu | 3-8 triệu | 500.000-1.500.000 |
| Kiến trúc multi-tenant gốc | Không | Không | Không | Không | Có |
| AI Branding tự sinh | Không | Không | Không | Không | Có |
| Onboarding tự phục vụ | Có (Gmail) | Không (báo giá) | Không (liên hệ) | Không (liên hệ) | Có (1-2 ngày) |
| Ứng dụng di động native | Phát triển sau | Có | Có | Có | Phát triển sau |
| Tích hợp Zalo (OA + nhóm) | Có | Có (native) | Có | Có | Có |
| Thanh toán tích hợp | Hạn chế | Không | Hạn chế | Có (3 cổng) | Có (VietQR + chuyển khoản) |
| Tuân thủ PDPL 2023 theo thiết kế | Đang triển khai | Đang triển khai | Đang triển khai | Có | Theo thiết kế (lộ trình bản địa hóa) |
| Hệ sinh thái OpenAPI / webhook | Hạn chế | Không | Không | Có (hạn chế) | Có |
| Khác biệt cốt lõi | UX trung tâm tiếng Anh | App di động + Zalo | Phân phối rộng | CRM + thanh toán | Multi-tenant + AI Branding |

## 1.3 Bài toán

### 1.3.1 Khoảng trống thị trường và định vị hệ thống đề xuất

Khoảng trống thị trường mà đề tài hướng đến là *trung tâm nhỏ và vừa (1-10 chi nhánh, 100-2000 học viên/cơ sở)* với mức giá 500.000-1.500.000đ/tháng, giao diện tiếng Việt, kiến trúc multi-tenant gốc cho phép mở rộng nhanh khi thành lập chi nhánh mới, và khả năng tự sinh tài nguyên branding bằng AI thay vì phải thuê thiết kế viên.

Dựa trên phân tích bốn hệ thống tham khảo, hệ thống đề xuất trong đề tài có bốn yếu tố khác biệt chính.

*Thứ nhất, kiến trúc đa tenant nguyên bản:* cả năm hệ thống tham khảo đều triển khai theo mô hình đơn tenant — mỗi khách hàng tương ứng một instance cơ sở dữ liệu riêng. Khi trung tâm mở rộng lên năm chi nhánh trở lên, chủ sở hữu trung tâm phải cấp phát thêm instance, đồng bộ dữ liệu thủ công và sao chép cấu hình. Hệ thống đề xuất sử dụng kiến trúc đa tenant nguyên bản theo mô hình Pool (mô hình hồ chứa dùng chung) của khung tham chiếu AWS Well-Architected SaaS Lens: một cơ sở dữ liệu dùng chung, một schema dùng chung, mỗi bảng nghiệp vụ mang cột định danh tenant (`tenant_id`) và cơ chế Row-Level Security (RLS, bảo mật ở mức dòng) của PostgreSQL tự động lọc dữ liệu theo từng tenant ngay tại tầng cơ sở dữ liệu. Nhờ chia sẻ một instance cơ sở dữ liệu duy nhất, một trung tâm có thể vận hành nhiều chi nhánh mà không phải cấp phát thêm hạ tầng cho mỗi chi nhánh, qua đó tiết kiệm đáng kể chi phí hạ tầng khi mở rộng so với mô hình đơn tenant của các hệ thống tham khảo.

*Thứ hai, AI Branding tự động:* chủ sở hữu trung tâm mới khai trương thường phải thuê thiết kế viên cho logo, banner và hero image marketing — một khoản chi phí và khoảng thời gian chờ đáng kể trước khi sẵn sàng vận hành. AI Branding của hệ thống đề xuất tự động sinh các tài nguyên này từ prompt văn bản và màu thương hiệu, qua đó rút ngắn đáng kể thời gian lẫn chi phí chuẩn bị nhận diện thương hiệu so với quy trình thuê ngoài. Cả năm hệ thống tham khảo đều không có tính năng này.

*Thứ ba, tuân thủ pháp luật Việt Nam theo thiết kế:* Luật Bảo vệ Dữ liệu Cá nhân năm 2023 (PDPL, thời hạn áp dụng 2026-07-01) [9], Luật An ninh mạng 2018 [10] và Nghị định 53/2022/NĐ-CP về bản địa hóa dữ liệu [11] là ba yêu cầu tuân thủ quan trọng cho mọi SaaS xử lý dữ liệu cá nhân tại Việt Nam. Hệ thống đề xuất tiếp cận theo nguyên tắc bảo mật và quyền riêng tư theo thiết kế (privacy-by-design), tích hợp các biện pháp tuân thủ ngay từ kiến trúc ban đầu: cô lập dữ liệu giữa các tenant bằng Row-Level Security, nhật ký kiểm toán bất biến phục vụ truy vết dữ liệu cá nhân, và thu thập sự đồng ý của người dùng ngay tại luồng đăng ký. Ở giai đoạn hiện tại, hệ thống vận hành trên hạ tầng AWS khu vực Singapore do ràng buộc chi phí; lộ trình phát triển sau bao gồm bản địa hóa dữ liệu sang vùng AWS Hà Nội hoặc nhà cung cấp đám mây trong nước nhằm đáp ứng đầy đủ Nghị định 53/2022/NĐ-CP trước khi mở rộng tới quy mô kích hoạt ngưỡng bản địa hóa dữ liệu. Trong khi đó, Mona eLMS, Easy Edu và BeeClass đang trong quá trình triển khai PDPL; DotB có sẵn nhưng mức giá cao.

*Thứ tư, UX Vietnamese-first:* hệ thống đề xuất áp dụng nhất quán bốn tiêu chí địa phương hóa cho mọi tài nguyên người dùng cuối — định dạng tiền tệ VND (`1.500.000đ` thay vì `$60.00`), nhãn tiếng Việt, dữ liệu mẫu phù hợp văn hóa Việt Nam, và nhận thức văn hóa địa phương. Cụ thể ngày tháng hiển thị dạng "Thứ Hai, 14/05/2026" thay vì "Mon May 14, 2026"; lời chào email "Em chào chị Hằng" trang trọng-kính trọng thay vì "Hi Hằng"; lịch học theo niên khóa tháng 9 đến tháng 5 và làm việc Thứ Hai đến Thứ Bảy phù hợp với quy ước giáo dục Việt Nam.

### 1.3.2 Cơ hội chiến lược

*Cơ hội thị trường:* phân khúc trung tâm nhỏ (1-3 chi nhánh, 100-500 học viên) chiếm khoảng 60% thị trường nhưng đa số hệ thống hiện hữu nhắm vào phân khúc trung và lớn — mô hình tăng trưởng dẫn dắt bởi sản phẩm (PLG — Product-Led Growth) có thể bắt đầu từ phân khúc này và mở rộng lên cao hơn khi sản phẩm trưởng thành. Thời hạn PDPL ngày 01/07/2026 yêu cầu hơn 50.000 trung tâm phải tuân thủ trong khoảng bảy tuần (đến cuối tháng 6/2026), tạo lợi thế cạnh tranh tại thời điểm vàng cho hệ thống đề xuất với khả năng tuân thủ tích hợp sẵn. Bên cạnh đó, mô hình nhượng quyền (Apollo English, ILA, Wall Street English) đang phát triển — kiến trúc đa tenant phù hợp với lộ trình mở rộng nhượng quyền tốt hơn so với kiến trúc đơn tenant.