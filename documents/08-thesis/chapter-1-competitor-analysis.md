---
title: Chương 1 — Tổng quan đề tài (Phần 1 Khảo sát thị trường)
chapter: 1
section: introduction-and-competitor-analysis
audience: mixed
last-updated: 2026-05-20
status: draft
---

# Chương 1 — Tổng quan đề tài

## 1.1 Giới thiệu chung về đề tài

### 1.1.1 Tên đề tài

Đồ án tốt nghiệp với tên đầy đủ: *"Xây dựng nền tảng SaaS đa khách thuê (multi-tenant) hỗ trợ quản lý trung tâm giáo dục với khả năng tự sinh tài nguyên thương hiệu bằng trí tuệ nhân tạo — Áp dụng cho thị trường giáo dục Việt Nam"*. Đề tài ngắn gọn được đặt tên là *KiteHub Platform*, ký hiệu trong các tài liệu kỹ thuật là `kitehub`.

### 1.1.2 Đối tượng nghiên cứu

Đối tượng nghiên cứu là kiến trúc và quy trình xây dựng nền tảng phần mềm dịch vụ (Software-as-a-Service, SaaS) phục vụ phân khúc trung tâm giáo dục tư thục quy mô nhỏ và vừa tại Việt Nam (1-10 chi nhánh, 100-2000 học viên mỗi cơ sở). Trọng tâm đối tượng gồm bốn nhánh chính: (1) kiến trúc đa khách thuê (multi-tenant) với cô lập dữ liệu ở mức cơ sở dữ liệu (schema-per-tenant + Row-Level Security); (2) tích hợp trí tuệ nhân tạo tạo sinh để sinh tự động các tài nguyên thương hiệu (logo, banner, hero image) phục vụ giai đoạn khai trương trung tâm; (3) tuân thủ khung pháp lý Việt Nam đối với SaaS xử lý dữ liệu cá nhân (Luật Bảo vệ Dữ liệu Cá nhân 2023, Luật An ninh mạng 2018, các nghị định và thông tư liên quan); (4) phương pháp luận phát triển phần mềm dưới điều kiện một nhà phát triển độc lập với hạn chế tài nguyên và thời hạn pháp lý cứng.

### 1.1.3 Phạm vi đề tài

Phạm vi đồ án được giới hạn ở giai đoạn thử nghiệm (beta) nội bộ và giai đoạn phát hành chính thức (production) ban đầu của nền tảng KiteHub. Cụ thể, đề tài chia làm bốn giai đoạn triển khai có lộ trình tăng tiến: *giai đoạn beta tenant* gồm khoảng năm trung tâm thử nghiệm mời theo invite-only, kiểm thử tính năng cốt lõi (đăng ký người dùng, tạo tenant, AI Branding tự động, quản lý lớp học, điểm danh và học phí cơ bản); *giai đoạn paid beta* mở rộng lên 30-50 trung tâm trả phí thử nghiệm với mô hình product-led growth (PLG); *giai đoạn production launch* phát hành chính thức ra thị trường với ràng buộc tuân thủ pháp luật Việt Nam đầy đủ (Luật Bảo vệ Dữ liệu Cá nhân hiệu lực từ ngày 1 tháng 7 năm 2026); *giai đoạn K-12 expansion* mở rộng sang phân khúc trường công lập và tư thục K-12 với các yêu cầu compliance bổ sung dành cho dữ liệu trẻ em vị thành niên (Điều 17 Luật Bảo vệ Dữ liệu Cá nhân). Phạm vi nghiên cứu trong đồ án chủ yếu tập trung vào *giai đoạn beta tenant readiness* — tức là hoàn thiện toàn bộ hạ tầng kỹ thuật, quy trình phát triển và bộ tài liệu pháp lý cần thiết để khởi động giai đoạn beta tenant một cách bền vững.

### 1.1.4 Mục tiêu đề tài

Mục tiêu tổng quát: xây dựng được một nền tảng SaaS đa khách thuê đầy đủ chức năng cốt lõi, sẵn sàng triển khai cho năm trung tâm giáo dục thực tế ở giai đoạn beta tenant, với điểm chất lượng tổng thể đạt ngưỡng tối thiểu 80 trên thang 100 theo bộ tiêu chí kiểm thử chất lượng phần mềm tự xây dựng. Bốn mục tiêu cụ thể bao gồm: (1) thiết kế và triển khai kiến trúc đa khách thuê với cô lập dữ liệu theo schema và Row-Level Security đáp ứng Điều 11 Luật Bảo vệ Dữ liệu Cá nhân về tamper-proof; (2) tích hợp tính năng AI Branding tự động sinh logo, hero image và social banner cho mỗi trung tâm mới với chi phí dưới 300 đồng mỗi trung tâm; (3) hoàn thiện bộ tài liệu pháp lý gồm Privacy Policy, Terms of Service, Data Processing Agreement, runbook ứng phó sự cố dữ liệu trong 72 giờ; (4) chứng minh phương pháp luận phát triển Quality-Driven Development áp dụng được cho mô hình nhà phát triển độc lập (solo-developer) với deadline pháp lý cứng.

### 1.1.5 Bối cảnh chuyên ngành Công nghệ thông tin và giáo dục

Đề tài kết hợp hai lĩnh vực chuyên ngành: *Công nghệ phần mềm* (kiến trúc hệ thống phân tán, microservice, multi-tenant SaaS, AI integration, DevOps) và *Công nghệ ứng dụng trong giáo dục* (EdTech, học tập số, quản lý cơ sở giáo dục). Trong giai đoạn 2022-2026, công nghệ AI tạo sinh, đặc biệt là mô hình ngôn ngữ lớn (Large Language Models) và mô hình khuếch tán (diffusion models) sinh ảnh, đã tạo ra cuộc cách mạng trong nhiều ngành công nghiệp, trong đó có giáo dục. Đồng thời, các quy định pháp luật về dữ liệu cá nhân tại Việt Nam được ban hành dày đặc giai đoạn 2018-2024 đặt ra thách thức tuân thủ mới cho các nền tảng SaaS xử lý dữ liệu người dùng cuối — đặc biệt là dữ liệu trẻ em vị thành niên trong môi trường giáo dục. Sự giao thoa giữa hai xu hướng (AI tạo sinh + tuân thủ pháp luật chặt chẽ) tạo ra một không gian thiết kế phong phú cho đề tài đồ án tốt nghiệp ngành Công nghệ phần mềm.

## 1.2 Cơ sở chuyên ngành

### 1.2.1 Kiến trúc Multi-tenant SaaS

Multi-tenant SaaS là một mô hình triển khai phần mềm dịch vụ trong đó một instance phần mềm duy nhất phục vụ nhiều khách hàng (tenant) cùng lúc, với mỗi tenant có dữ liệu và cấu hình riêng nhưng chia sẻ chung tài nguyên hạ tầng [40]. So với mô hình single-tenant truyền thống (mỗi khách hàng có một instance độc lập), multi-tenant SaaS giúp giảm chi phí hạ tầng đáng kể khi mở rộng quy mô, đồng thời cho phép cập nhật phiên bản đồng loạt cho tất cả khách hàng. Ba mức độ cô lập dữ liệu phổ biến trong multi-tenant SaaS gồm: cô lập ở mức cơ sở dữ liệu (database-per-tenant) với cách ly mạnh nhất nhưng tốn kém nhất; cô lập ở mức schema (schema-per-tenant) với cách ly trung bình và chi phí hợp lý; cô lập ở mức row (shared schema + tenant_id discriminator) với cách ly thấp nhất nhưng linh hoạt nhất. Đồ án KiteHub chọn cách tiếp cận lai: cô lập ở mức cơ sở dữ liệu cho dữ liệu nhạy cảm (dữ liệu học viên, hóa đơn) kết hợp với Row-Level Security của PostgreSQL để đảm bảo cách ly thậm chí khi lập trình viên viết nhầm query không có điều kiện tenant — chi tiết kiến trúc trình bày trong Chương 2.

### 1.2.2 Giáo dục công nghệ (EdTech) và phân khúc tại Việt Nam

Giáo dục công nghệ (Educational Technology, viết tắt EdTech) là một lĩnh vực ứng dụng công nghệ thông tin vào quá trình dạy và học, gồm các sản phẩm như hệ thống quản lý học tập (Learning Management System, LMS), nền tảng học trực tuyến (MOOC), phần mềm quản lý trung tâm giáo dục, các công cụ hỗ trợ giảng dạy và đánh giá. Tại Việt Nam, thị trường EdTech tăng trưởng mạnh giai đoạn 2020-2026 dưới tác động của ba yếu tố chính: chuyển đổi số sau đại dịch COVID-19, chính thức hóa hoạt động dạy thêm có thu phí (Thông tư 29/2024/TT-BGDĐT [1]), và mức chi đầu tư cao của phụ huynh Việt Nam cho giáo dục con cái (15-20 phần trăm thu nhập hộ gia đình theo báo cáo 6Wresearch [3]). Theo báo cáo Kinh tế Số Việt Nam của VECITA năm 2024 [4], cấu phần EdTech trong kinh tế số Việt Nam có tốc độ tăng trưởng kép hằng năm khoảng 12-15 phần trăm. Đề tài KiteHub khai thác phân khúc trung tâm giáo dục tư thục quy mô nhỏ và vừa — phân khúc có nhu cầu cao nhưng ít được phục vụ bởi các nền tảng hiện hữu (do giá thành cao hoặc thiếu tính năng phù hợp văn hóa địa phương).

### 1.2.3 Lý do chọn ngành Công nghệ phần mềm

Ngành Công nghệ phần mềm tại Đại học Giao thông Vận tải cung cấp khối kiến thức nền tảng cần thiết để giải quyết các bài toán đặt ra trong đề tài: thiết kế và phân tích hệ thống (yêu cầu kiến trúc multi-tenant SaaS), lập trình hướng đối tượng và microservice (yêu cầu xây dựng các dịch vụ độc lập có khả năng mở rộng), cơ sở dữ liệu và Row-Level Security (yêu cầu cô lập dữ liệu đa khách thuê), an toàn thông tin và mật mã (yêu cầu tuân thủ Luật An ninh mạng), kỹ thuật phần mềm và đảm bảo chất lượng (yêu cầu phương pháp luận Quality-Driven Development). Đồ án vừa là sản phẩm tổng kết quá trình học tập, vừa là một thực nghiệm áp dụng các kiến thức lý thuyết của chuyên ngành vào một bài toán thực tế có tính thương mại và xã hội cao.

## 1.3 Khảo sát thị trường giáo dục SaaS Việt Nam

### 1.3.1 Bối cảnh thị trường

Thị trường phần mềm quản lý trung tâm giáo dục Việt Nam tăng trưởng mạnh giai đoạn 2020-2025, được thúc đẩy bởi 3 yếu tố chính. Thứ nhất, ngành dạy thêm (trung tâm ngoại ngữ + tin học + năng khiếu) bùng nổ sau Thông tư 29/2024/TT-BGDĐT chính thức hóa hoạt động dạy thêm có thu phí [1], ước tính hơn 50.000 trung tâm hoạt động trên toàn quốc theo báo cáo Magenest 2024 [2]. Thứ hai, phụ huynh Việt Nam có thói quen đầu tư mạnh cho giáo dục con cái, với mức chi trung bình 15-20 phần trăm thu nhập hộ gia đình cho học thêm con (theo 6Wresearch [3]) — chỉ số này phù hợp với báo cáo Kinh tế Số Việt Nam 2024 của VECITA về tăng trưởng chi tiêu EdTech trong cấu phần kinh tế số [4]. Thứ ba, sau đại dịch COVID-19, các trung tâm buộc phải số hóa quy trình quản lý (điểm danh, học phí, lịch học, kênh liên lạc với phụ huynh) để duy trì hoạt động khi liên tục chuyển đổi giữa chế độ trực tuyến và trực tiếp.

Tuy nhiên, đa số trung tâm nhỏ và vừa (1-3 chi nhánh, dưới 500 học viên) vẫn dùng Excel kết hợp Zalo group chat, thậm chí sổ ghi tay để quản lý. Lý do chính: phần mềm hiện có hoặc quá phức tạp (Cyber School, MISA EMIS hướng đến trường công lập K-12), hoặc thiếu UX tiếng Việt (LMS quốc tế như Moodle, Canvas), hoặc chi phí cao không phù hợp với phân khúc trung tâm tự phát (Speed Manager, EduCom với mức 50-100 USD/tháng/cơ sở).

Khoảng trống thị trường mà đồ án hướng đến là *trung tâm nhỏ và vừa (1-10 chi nhánh, 100-2000 học viên/cơ sở)* với mức giá 500.000-1.500.000đ/tháng, giao diện tiếng Việt, kiến trúc multi-tenant gốc cho phép mở rộng nhanh khi thành lập chi nhánh mới, và khả năng tự sinh tài nguyên branding bằng AI thay vì phải thuê thiết kế viên.

### 1.3.2 Khảo sát năm hệ thống tham khảo trên thị trường

Đồ án khảo sát năm hệ thống tham khảo nổi bật trong phân khúc quản lý trung tâm giáo dục Việt Nam: BeeClass, MISA AMIS Trường Học, Mona eLMS, Easy Edu và DotB. Mỗi hệ thống có lịch sử thị trường, phân khúc khách hàng và điểm mạnh khác nhau, cùng nhau phủ phần lớn nhu cầu hiện hữu của các trung tâm vừa và lớn.

#### 1.3.2.1 BeeClass

*BeeClass* [41] là một trong những phần mềm quản lý trung tâm giáo dục dành cho các trung tâm ngoại ngữ và đào tạo tư nhân quy mô nhỏ tại Việt Nam, hoạt động từ khoảng năm 2018-2019. Sản phẩm định vị mạnh ở phân khúc trung tâm có 50-500 học viên với gói giá khởi điểm khá thấp (báo giá từ khoảng 500.000-1.000.000 đồng mỗi tháng tùy quy mô), tập trung vào các tính năng quản lý cơ bản: danh sách học viên, đăng ký lớp học, lịch học, điểm danh, học phí và xuất hóa đơn đơn giản. Thế mạnh nổi bật của BeeClass là giao diện tiếng Việt thân thiện được thiết kế cho người không chuyên về công nghệ thông tin (chủ trung tâm thường không có nền tảng kỹ thuật), kết hợp tích hợp Zalo Notification Service cho thông báo tự động đến phụ huynh. Điểm yếu chính: kiến trúc single-tenant theo từng trung tâm (mỗi khách hàng tương ứng một instance cơ sở dữ liệu riêng) gây khó khăn khi trung tâm mở rộng nhiều chi nhánh; thiếu khả năng tự sinh tài nguyên thương hiệu bằng trí tuệ nhân tạo; quy trình onboarding yêu cầu liên hệ bộ phận kinh doanh để được hỗ trợ thiết lập tài khoản thay vì tự phục vụ (self-service). Đây cũng là phân khúc cạnh tranh trực tiếp với hệ thống đề xuất, do đó BeeClass được khảo sát chi tiết hơn các hệ thống khác.

<!-- screenshot placeholder: beeclass-homepage-2026-05-20.png — 1440x900 vi-VN — show landing page hero + 'Đăng ký dùng thử' CTA + customer logos -->

**Hình 1.3.1.** Giao diện trang chủ BeeClass với chiến lược tập trung vào phân khúc trung tâm ngoại ngữ và đào tạo quy mô nhỏ tại Việt Nam. *Nguồn: https://beeclass.com, truy cập ngày 20/05/2026.*

#### 1.3.2.2 MISA AMIS Trường Học

*MISA AMIS Trường Học* [5] là sản phẩm giáo dục của công ty phần mềm MISA — một trong những công ty phần mềm Việt Nam lâu đời nhất với hơn 25 năm hoạt động trong lĩnh vực kế toán và thuế. Sản phẩm MISA EMIS đã triển khai tại hơn 30.000 trường tiểu học và trung học cơ sở công lập toàn quốc, phục vụ hơn 12 triệu học sinh. Thế mạnh chính của MISA AMIS Trường Học là tích hợp sâu với MISA MeInvoice (hóa đơn điện tử theo Thông tư 78/2021/TT-BTC) và mạng lưới phân phối qua hệ thống đại lý tại 63 tỉnh thành. Điểm yếu: định vị B2B trường công lập với chu kỳ bán hàng 3-6 tháng, phí thiết lập 50-200 triệu đồng và mức giá 2-5 triệu/tháng/trường, không phù hợp với trung tâm tự phát quy mô nhỏ cần triển khai nhanh.

<!-- screenshot placeholder: misa-amis-truong-hoc-homepage-2026-05-20.png — 1440x900 vi-VN — show product landing page với enterprise messaging + B2B trường công lập positioning -->

**Hình 1.3.2.** Giao diện trang chủ MISA AMIS Trường Học định vị doanh nghiệp B2B với chu kỳ bán hàng 3-6 tháng. *Nguồn: https://amis.misa.vn/truong-hoc/, truy cập ngày 20/05/2026.*

#### 1.3.2.3 Mona eLMS

*Mona eLMS* [6] là sản phẩm của công ty Mona Software thành lập năm 2017, tập trung chuyên biệt vào phân khúc trung tâm ngoại ngữ và tin học. Mona eLMS có khoảng 800 khách hàng trung tâm tại Việt Nam tính đến năm 2024. Thế mạnh nổi bật của Mona là ứng dụng di động native iOS/Android cho học viên và phụ huynh, tích hợp Zalo Notification Service (ZNS) cho thông báo điểm danh, kết quả thi và lịch học — điểm chạm rất quan trọng với phụ huynh Việt Nam khi Zalo chiếm hơn 90 phần trăm thị phần ứng dụng nhắn tin smartphone. Điểm yếu: hệ sinh thái đóng (không có API công khai), single-tenant theo từng trung tâm, mức giá 1,5-5 triệu đồng/tháng tùy số học viên với cơ chế báo giá tùy chỉnh không minh bạch.

<!-- screenshot placeholder: mona-elms-homepage-2026-05-20.png — 1440x900 vi-VN — show mobile app + Zalo ZNS marketing emphasis -->

**Hình 1.3.3.** Giao diện trang chủ Mona eLMS với điểm mạnh ứng dụng di động native và tích hợp Zalo ZNS. *Nguồn: https://mona.solutions/elms, truy cập ngày 20/05/2026.*

#### 1.3.2.4 Easy Edu

*Easy Edu* [7] là hệ thống lớn nhất trên thị trường trong phân khúc trung tâm ngoại ngữ vừa và nhỏ với hơn 1.400 trung tâm khách hàng. Sản phẩm ra mắt năm 2018, có hệ thống phân phối mạnh tại miền Bắc và miền Trung thông qua các sự kiện ngành giáo dục và hợp tác với hiệp hội trung tâm ngoại ngữ. Tập tính năng đầy đủ gồm quản lý học viên, lớp học, học phí, điểm danh, báo cáo, tích hợp Zalo OA, ứng dụng di động cho phụ huynh, với mức giá phải chăng từ 800.000 đồng/tháng cho gói cơ bản phục vụ 200 học viên. Điểm yếu chính: kiến trúc single-tenant theo từng trung tâm (khó mở rộng nhượng quyền), không có khả năng tự sinh tài nguyên branding bằng AI, onboarding yêu cầu liên hệ bộ phận kinh doanh chứ chưa tự phục vụ.

<!-- screenshot placeholder: easy-edu-homepage-2026-05-20.png — 1440x900 vi-VN — show landing với 1400+ customer logos + pricing table -->

**Hình 1.3.4.** Giao diện trang chủ Easy Edu với hơn 1.400 khách hàng trung tâm ngoại ngữ vừa và nhỏ. *Nguồn: https://easyedu.vn, truy cập ngày 20/05/2026.*

#### 1.3.2.5 DotB

*DotB* [8] là sản phẩm phần mềm quản lý giáo dục đa năng của công ty DotB Vietnam ra mắt năm 2019, hướng đến phân khúc trung tâm tầm trung và trường tư thục với giá trị thương vụ trung bình 3-8 triệu đồng/tháng. Thế mạnh đặc thù của DotB là module CRM tích hợp phục vụ quản lý khách hàng tiềm năng (theo dõi học viên triển vọng từ biểu mẫu hỏi thông tin, qua lớp học thử, đến đăng ký chính thức) và tích hợp sẵn ba cổng thanh toán VNPay, MoMo, ZaloPay — giải quyết điểm đau của khoảng 80 phần trăm trung tâm vẫn dùng chuyển khoản ngân hàng kèm đối soát thủ công. Điểm yếu: mức giá cao gấp 3-5 lần Easy Edu, định vị tầm trung không phục vụ phân khúc trung tâm tự phát quy mô nhỏ — không cạnh tranh trực tiếp với khoảng trống thị trường mà đồ án hướng đến.

<!-- screenshot placeholder: dotb-homepage-2026-05-20.png — 1440x900 vi-VN — show CRM module + 3 payment gateway integrations -->

**Hình 1.3.5.** Giao diện trang chủ DotB nhấn mạnh module CRM và tích hợp ba cổng thanh toán VNPay, MoMo, ZaloPay. *Nguồn: https://dotb.com.vn, truy cập ngày 20/05/2026.*

### 1.3.3 Khảo sát nhu cầu sử dụng từ phía người dùng cuối

Bên cạnh khảo sát các hệ thống đang có trên thị trường, đồ án tổng hợp nhu cầu sử dụng từ năm nhóm người dùng cuối (end-user) dựa trên các báo cáo ngành công khai. Phân tích này giúp xác định những tính năng cốt lõi cần ưu tiên trong giai đoạn đầu của hệ thống đề xuất.

*Nhóm 1 — Chủ trung tâm (Owner):* theo báo cáo Magenest 2024 [2] và 6Wresearch 2024 [3], chủ trung tâm vừa và nhỏ (1-10 chi nhánh, 100-2000 học viên) ưu tiên ba nhu cầu chính: (i) quản lý học phí và đối chiếu thanh toán tự động (80 phần trăm trung tâm vẫn dùng chuyển khoản ngân hàng kèm đối soát thủ công, chiếm 4-6 giờ mỗi tuần làm việc văn phòng); (ii) báo cáo doanh thu — tỷ lệ giữ chân học viên — chi phí vận hành theo thời gian thực để ra quyết định mở hoặc đóng lớp; (iii) chi phí phần mềm thấp dưới 1,5 triệu đồng mỗi tháng phù hợp biên lợi nhuận hiện hành 25-30 phần trăm.

*Nhóm 2 — Quản lý trung tâm (Manager):* nhu cầu chính tập trung vào quy trình vận hành hàng ngày — điểm danh tự động, lịch giảng dạy linh hoạt khi giáo viên thay ca, thông báo tự động cho phụ huynh khi học viên vắng mặt hoặc nghỉ học liên tiếp. Báo cáo VECITA 2024 [4] về kinh tế số trong giáo dục cho thấy 65 phần trăm trung tâm gặp khó khăn vận hành khi vượt qua mốc 300 học viên do thiếu công cụ phân quyền và quy trình chuẩn hóa giữa các chi nhánh.

*Nhóm 3 — Giáo viên độc lập (Solo Teacher):* nhóm giáo viên dạy thêm tự do (1-50 học viên) cần công cụ nhẹ, chi phí thấp dưới 500.000 đồng mỗi tháng, ưu tiên: lịch học cá nhân, biểu mẫu thu học phí qua chuyển khoản kèm xác nhận tự động, gửi tài liệu học tập qua kênh quen thuộc (Zalo, email). Thông tư 29/2024/TT-BGDĐT [1] công nhận hợp pháp dạy thêm có thu phí mở ra phân khúc này, ước tính 50.000-100.000 giáo viên độc lập trên toàn quốc — tuy nhiên các hệ thống đang có trên thị trường đều định vị cho trung tâm tổ chức, chưa phục vụ nhóm cá nhân.

*Nhóm 4 — Phụ huynh:* phụ huynh đầu tư 15-20 phần trăm thu nhập hộ gia đình cho học thêm con [3], do đó nhu cầu minh bạch về tiến độ học tập và tài chính. Các nhu cầu cụ thể bao gồm: thông báo điểm danh hàng ngày, báo cáo kết quả học tập định kỳ hai tuần, hóa đơn điện tử có thể tra cứu lại, kênh liên lạc trực tiếp với giáo viên qua Zalo (90 phần trăm phụ huynh dùng Zalo). Báo cáo Magenest 2024 [2] nhận định Zalo là kênh giao tiếp dominant giữa trung tâm và phụ huynh, vượt SMS và email.

*Nhóm 5 — Học viên:* nhóm học viên (đa số là thanh thiếu niên 10-18 tuổi) cần truy cập tài liệu học tập, lịch học cá nhân, lịch bài kiểm tra và thông báo điểm danh trên thiết bị di động. Báo cáo VECITA 2024 [4] cho biết 92 phần trăm học viên Việt Nam có smartphone từ tuổi 12; tuy nhiên các hệ thống đang có trên thị trường chủ yếu phục vụ phía quản trị, chưa thiết kế giao diện riêng cho học viên độc lập với phụ huynh.

Tổng hợp năm nhóm end-user cho thấy nhu cầu chung là một nền tảng đa-persona (multi-persona) với phân quyền rõ ràng theo vai trò, giá thấp phù hợp phân khúc nhỏ và vừa, tích hợp Zalo cho kênh giao tiếp với phụ huynh — tất cả các tiêu chí này đều được xem xét trong định hướng kiến trúc hệ thống đề xuất.

### 1.3.4 Bảng so sánh tổng hợp

Bảng tổng hợp so sánh năm hệ thống tham khảo với hệ thống đề xuất trong đồ án theo các tiêu chí quan trọng:

| Tiêu chí | BeeClass | MISA AMIS | Mona eLMS | Easy Edu | DotB | Hệ thống đề xuất |
|---|---|---|---|---|---|---|
| Persona mục tiêu | Trung tâm ngoại ngữ nhỏ | Trường K-12 công lập | Trung tâm ngoại ngữ | Trung tâm ngoại ngữ nhỏ | Trung tâm tầm trung và trường tư | Trung tâm nhỏ và vừa |
| Khoảng giá (đ/tháng) | 500.000-1.000.000 | 2-5 triệu + 50-200 triệu thiết lập | 1,5-5 triệu | 800.000-3 triệu | 3-8 triệu | 500.000-1.500.000 |
| Kiến trúc multi-tenant gốc | Không | Không | Không | Không | Không | Có |
| AI Branding tự sinh | Không | Không | Không | Không | Không | Có |
| Onboarding tự phục vụ | Không (liên hệ) | Không (đại lý) | Không (báo giá) | Không (liên hệ) | Không (liên hệ) | Có (1-2 ngày) |
| Ứng dụng di động native | Có | Có | Có | Có | Có | Phiên bản sau |
| Tích hợp Zalo ZNS | Có | Có | Có (native) | Có | Có | Phiên bản sau |
| Cổng thanh toán built-in | Không | Có (MISA Wallet) | Không | Hạn chế | Có (3 cổng) | Phiên bản sau |
| Tuân thủ Luật Bảo vệ Dữ liệu Cá nhân built-in | Đang triển khai | Có | Đang triển khai | Đang triển khai | Có | Có (từ ngày đầu) |
| Hệ sinh thái OpenAPI/webhook | Không | Hạn chế | Không | Không | Có (hạn chế) | Có |
| Khác biệt cốt lõi | Giá thấp phân khúc nhỏ | Tích hợp kế toán MISA | App di động + Zalo | Phân phối rộng | CRM + thanh toán | Multi-tenant + AI Branding |

### 1.3.5 Định vị hệ thống đề xuất và yếu tố khác biệt

Dựa trên phân tích năm hệ thống tham khảo, hệ thống đề xuất trong đồ án có bốn yếu tố khác biệt chính. *Thứ nhất, kiến trúc multi-tenant gốc:* cả năm hệ thống tham khảo đều dùng triển khai single-tenant với mỗi khách hàng tương ứng một instance cơ sở dữ liệu riêng. Khi trung tâm mở rộng lên năm chi nhánh trở lên, chủ trung tâm phải cấp phát thêm instance, đồng bộ dữ liệu thủ công và sao chép cấu hình. Hệ thống đề xuất sử dụng kiến trúc multi-tenant gốc với cô lập ở mức cơ sở dữ liệu (schema-per-tenant), cho phép một trung tâm có 100 chi nhánh trên cùng một instance, tiết kiệm khoảng 80 phần trăm chi phí hạ tầng khi mở rộng.

*Thứ hai, AI Branding tự động:* chủ trung tâm mới khai trương thường tốn 2-5 triệu đồng thuê thiết kế viên cho logo, banner và hero image marketing. AI Branding của hệ thống đề xuất tự động sinh các tài nguyên này từ prompt văn bản và màu thương hiệu, giảm thời gian sẵn sàng vận hành từ 1-2 tuần xuống còn vài giờ. Cả năm hệ thống tham khảo đều không có tính năng này.

*Thứ ba, tuân thủ pháp luật Việt Nam built-in:* Luật Bảo vệ Dữ liệu Cá nhân năm 2023 (thời hạn áp dụng 2026-07-01) [9], Luật An ninh mạng 2018 [10] và Nghị định 53/2022/NĐ-CP về bản địa hóa dữ liệu [11] là ba yêu cầu tuân thủ quan trọng cho mọi SaaS xử lý dữ liệu cá nhân tại Việt Nam. Hệ thống đề xuất tích hợp tuân thủ ngay từ thiết kế ban đầu thay vì bổ sung sau. Trong khi đó, BeeClass, Mona eLMS và Easy Edu đang trong quá trình triển khai; DotB và MISA AMIS có sẵn nhưng mức giá cao.

*Thứ tư, UX Vietnamese-first:* hệ thống đề xuất áp dụng nhất quán bốn tiêu chí địa hóa cho mọi tài nguyên người dùng cuối — định dạng tiền tệ đồng Việt Nam (`1.500.000đ` thay vì `$60.00`), nhãn tiếng Việt, dữ liệu mẫu phù hợp văn hóa Việt Nam, và nhận thức văn hóa địa phương. Cụ thể ngày tháng hiển thị dạng "Thứ Hai, 14/05/2026" thay vì "Mon May 14, 2026"; lời chào email "Em chào chị Hằng" trang trọng-kính trọng thay vì "Hi Hằng"; lịch học theo niên khóa tháng 9 đến tháng 5 và làm việc Thứ Hai đến Thứ Bảy phù hợp với quy ước giáo dục Việt Nam.

### 1.3.6 Cơ hội và rủi ro chiến lược

*Cơ hội thị trường:* phân khúc trung tâm nhỏ (1-3 chi nhánh, 100-500 học viên) chiếm khoảng 60 phần trăm thị trường nhưng đa số hệ thống hiện hữu nhắm vào phân khúc trung và lớn — mô hình product-led growth (PLG) có thể bắt đầu từ phân khúc này và mở rộng lên cao hơn khi sản phẩm trưởng thành. Thời hạn Luật Bảo vệ Dữ liệu Cá nhân 2026-07-01 yêu cầu hơn 50.000 trung tâm phải tuân thủ trong khoảng bảy tuần (đến cuối tháng 6/2026), tạo lợi thế cạnh tranh tại thời điểm vàng cho hệ thống đề xuất với tuân thủ built-in. Bên cạnh đó, mô hình nhượng quyền (Apollo English, ILA, Wall Street English) đang phát triển — kiến trúc multi-tenant phù hợp với lộ trình mở rộng nhượng quyền tốt hơn so với kiến trúc single-tenant.

*Rủi ro chính:* việc lùi ứng dụng di động native và tích hợp Zalo ZNS sang các phiên bản sau có rủi ro mất khách so với Mona eLMS và Easy Edu vốn đã có ứng dụng native — giải pháp giảm thiểu là web responsive kết hợp Zalo group chat trong phiên bản đầu, ưu tiên ứng dụng native ở phiên bản kế tiếp. Cổng thanh toán cũng là điểm yếu khi DotB có sẵn ba cổng tích hợp — phiên bản đầu của hệ thống đề xuất sử dụng chuyển khoản ngân hàng kết hợp VietQR (không cần cổng), phiên bản kế tiếp hợp tác với một cổng chính (VNPay). Cuối cùng, nhận diện thương hiệu là rào cản khi MISA và Easy Edu có hơn 5 năm hiện diện thị trường trong khi hệ thống đề xuất mới ra mắt — giải pháp giảm thiểu là tiếp cận PLG, marketing nội dung tiếng Việt và chương trình người dùng tiên phong với các trung tâm beta.

## 1.4 Kết luận chương 1 phần khảo sát

Thị trường giáo dục SaaS Việt Nam có năm hệ thống tương tự chính (BeeClass, MISA AMIS, Mona eLMS, Easy Edu, DotB) phủ các phân khúc khác nhau từ trung tâm nhỏ phân tán đến trường công lập, đều thiếu đồng thời bốn yếu tố quan trọng (multi-tenant gốc, AI Branding tự sinh, Vietnamese-first UX, compliance built-in) mà hệ thống đề xuất sẽ tích hợp ngay từ thiết kế đầu — chi tiết kiến trúc kỹ thuật và phương pháp luận trình bày trong các phần tiếp theo của Chương 1 cùng với Chương 2 về Kiến trúc Hệ thống.
