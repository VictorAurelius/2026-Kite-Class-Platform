giải thích cho tôi các 12 gap candidates mới + 11 gaps unscheduled + 4 plan-alignment issues.

sử dụng prompt mới như thế nào, sẽ làm được những gì? commit hết các file changing để bắt đầu session mới

1. các gaps này có bị trùng với gaps đã có không, kiểm tra lại 1 lượt
2.  Meta-P2    │ GAP-196 │ 9router tool ADR  => gaps này bỏ đi, không có hiệu quả với dự án nhỉ

GAP-190 │ KiteHub SEO + marketing site
=> gaps phải dựa trên tình trạng của hệ thống hiện tại, đã check status thực sự của gaps này chưa

lưu memory và update skills để tránh sai lầm khi tạo gaps, tăng effort

mô phỏng để check gaps trong workflow tương tự và fix luôn

sync action-2 lên remote:

Bây giờ có 1 task cực kỳ quan trọng, làm báo cáo đồ án tốt nghiệp cho dự án này => cover release 2, cần draft plan trước:

1. đổi với skill tạo file docx đồ án, đọc lại các thư mục liên quan đến báo cáo như: documents/07-archived/academic/word-reports, các skills, rules về tạo báo cáo

2. Đối với cấu trúc của file đồ án, đọc ảnh: documents/07-archived/khung-bc-do-an.png (move vào folder đúng), thiết định rule: đối với toàn bộ docx đồ án, nhưng ngoại trừ phụ lục như manual, evidence triển khai, evidence end-user thì không quá 60 trang. Ghi nhớ: max 60 trang

3. Riêng đề tài này có các lưu ý về đồ án như sau:
- xác định kế hoạch dùng thử: 2 giáo viên đơn lẻ, 2 loại business: trial, vip => đây là mời beta-user => chưa có evidence nhưng phải có kế hoạch
- cần tạo tài liệu manual hệ thống: pdf, video => pdf đang tạo ở wave 92 => chưa confirm
- thu thập dữ liệu phản hồi: evidence, log, bản nhận xét, ký tên(quan trọng) => chưa có dữ liệu thật, nhưng cần có đánh giá luôn trong báo cáo => bổ sung phụ lục sau
- chương 1: xác định bài toàn(luật) công nghệ, công cụ => sử dụng toàn bộ dữ liệu từ documents => trích dẫn tài liệu tham khảo thật, đúng chuẩn
- chương 2: yêu cầu chức năng, phi chức năng => từ dữ liệu documents cần viết lại đúng theo chuẩn báo cáo đồ án
- chương 2: kiến trúc, nhóm nghiệp vụ, nghiệp vụ chính => đối với kiến trúc và nhóm nghiệp vụ thì cần trình bày đủ trong báo cáo, nhưng đối với cụ thể thì cần chọn các nghiệp vụ chính để trình bày như SAAS, B-learning => không trình bày tất cả nghiệp vụ
- chương 3: lập trình - Đại diện 1 vài thao tác => chương này cũng chỉ nên ý chính vì dự án lớn, không nêu hết được => chương ngắn nhất
- chương 4: Triển khai: cách triển khai trên cloud, cho user, kết quả sử dụng => đây là chương quan trọng nhất trong đồ án và là điểm ăn tiền nhất của dự án => cần trình bày hết dữ liệu
- thiết kế hoàn thiện giao diện theo tiêu chuẩn => chốt báo cáo sẽ là bản release 2 nên sẽ có UI /UX theo chuẩn UI kits
- hoàn thiện code cho các loại đối tượng khác => business đầy đủ, code đầy đủ có các loại đối tượng P
- hoàn thiện tích hợp AI vào hệ thống => có tích hợp AI
- release thành công bản v2.0.0 => chốt bản sẽ báo cáo

4. Đối với các inside của dev:
- trong báo cáo không được đề cập đến các dữ liệu không chuẩn báo cáo cửa dự án như GAP ID, wave, ... => giảng viên không nhận các dạng dữ liệu kiểu này
- cần có folder riêng chưa ID các ảnh sẽ có trong báo cáo => tự tạo các diagram phục vụ chương 1,2,3,4 như BRD, ERD, AWS diagram, ... => các ảnh chụp FE, dùng tools chụp, ...
=> mục tiêu là dev không phải sửa bằng tay
- tuyệt đối không đề cập đến claude trong đồ án
- thông tin cá nhân của dev đã được lưu ở các folder cũ, cụ thể là đề cương, cần tìm hiểu đầy đủ, không để các dữ liệu raw
- cần focus theo khung đề cương mà code thực tế, không vẽ feature chưa có trong kế hoạch của release 2

Vậy cần bổ sung thêm outside

À, đáng ra phải chốt plan của release 2 trước rồi mới chốt được plan thesis nhỉ?