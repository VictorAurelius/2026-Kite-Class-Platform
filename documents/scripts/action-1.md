tôi có ý tưởng đồ án tốt nghiệp trong file idea
hãy tạo 1 báo cáo công nghiệp chuyên nghiệp mô tả lại use case của toàn bộ hệ thống, use case của từng service, công nghệ service sử dụng, công nghệ sẽ deploy service đó, biết rằng giao tiếp giữa các service là restful
tạo báo cáo nêu lợi ích của kiến trúc mircoservice cho KiteClass
KiteHub có thực sự cần mircoservice không?

hãy tạo báo cáo viết rõ quy trình mở 1 node KiteClass

hãy tạo lại báo cáo kiến trúc V2: KietHub sẽ là dạng Modular Monolith
quy trình mở 1 node KiteClass sẽ có tích hợp AI Agent, cụ thể:
khi khách hàng đăng ký gói tạo node => ngoài các cấu hình technical, khách hàng gửi ảnh cá nhân => tự động render các ảnh khác có banner, khẩu hiệu, marketing và FE sẽ hiển thị những ảnh này lên vị trí phù hợp 

bây giờ hãy tạo 1 báo cáo công nghệ sử dụng cho từng service:
1. ưu tiên BE là JAVA Spring Boot, nhưng nếu service đó cần 1 BE mạnh hơn hẳn (ví dụ streaming service) thì vẫn nên đề xuất BE đó
2. ưu tiên FE là NextJS, cần phân tích xem những service nào dùng chung FE (cho 1 node), có cần tách riêng FE không
3. cơ sở dữ liệu => tối ưu cho từng service, (không ưu tiên No SQL)
4. công nghệ deploy trên AWS rõ ràng

bây giờ hãy tạo req-1 là plan task cho claude để thực hiện tác vụ sau:
1. code plantUML để vẽ các sơ đồ sau:
+ sơ đồ kiến trúc platform
+ flow mở node kiteclass
Sơ đồ đơn giản, dễ hiểu, nhưng chứa đầy đủ thông tin của đề tài
=> chạy code để tạo hình ảnh luôn
2. file báo cáo dạng doc => tạo file md nhưng không có syntax của md, để dạng text thường để copy vào doc, bao gồm các nội dung sau:
+ 1 nêu ý tưởng
+ 2 nêu kiến trúc, 1 số lý do tại sao dùng microservice cho 1 node KiteClass
+ mô tả sơ bộ flow mở 1 node kiteclass
+ điểm mạnh của đề tài
+ thử thách của đề tài: cần kiến thức AWS mạnh, khối lượng code lớn, khảo sát nhiều đối tượng, ...

ước tính file dọc tầm 4 trang cả ảnh sơ đồ thôi nhé

thực hiện req-1-plantuml-and-report

1. làm luôn cho tôi cách 2 để tạo ảnh
2. graduation-thesis-outline đang là tiếng việt không dấu, hãy sửa lại
3. architecture-diagram các đường nối đang hơi dối và hơi nhiều note => sửa lại cho cả provisioning-flow
4. provisioning-flow.puml đang lỗi code