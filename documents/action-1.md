Java Language Support chạy một language server riêng luôn phân tích code ngầm. Còn Next.js/TypeScript cần được cấu hình đúng mới hoạt động tương tự.

=> cấu hình để tôi xem được trong dự án này

MẶC ĐỊNH: mỗi lần thực hiện PR phải dùng superpowers

merge PR redesign chưa

ECE có luồng test chưa, nếu có rồi thì test pass ece tôi mới test manually
tôi vừa vào home thì thấy css lại không có margin?

tạo PR mới theo chuẩn superpowers:
viết đủ test ECE cho kitehub, coverage đạt 100%, test pass

commit cả các file đang changing

tạo test ECE coverage 100% cho BE kitehub, test pass
đặt mục tiêu đỏ, không có bug nào cho kitehub ở môi trường local

fix thì phải làm gì cho đúng quy trình?

push and check CI

theo design dùng công cụ nào để khởi tạo instance

tôi lo lắng việc không phân biệt rõ giữa môi trường local và production trong các vấn đề:
1. code
2. tài liệu
3. dữ liệu mẫu: mock, tài khoản, dữ liệu về demo, dữ liệu về kiteteam, ...
4. các cấu hình bảo mật

hãy tạo PR để đánh giá và hoàn thiện vấn đề này, hãy hỏi tôi để xác nhận rõ yêu cầu

tách thành PR plan riêng
nhưng bạn cần lưu ý, mục tiêu của tôi ở đây là tách rõ môi trường local và production

local: ưu tiên, setup nhanh, test chuẩn, thuận tiện cho dev
production: chuyên nghiệp, bảo mật, không bug, không điểm yếu
các PR fix đã đáp ứng được chưa?

tôi muốn refactor lại UI của kitehub, có nhiều template rất đẹp cho nền tảng SaaS liệu có sử dụng tài nguyên đó để refactor lại UI kitehub đẹp hơn nhưng vẫn giữ được đặc điểm của kitehub không?
ví dụ tài nguyên:
https://www.figma.com/community/website-templates/saas?resource_type=files

https://www.figma.com/community/website-templates/e-learning?resource_type=files

giới thiệu về các công nghệ dùng ở track C để cho tôi hiểu qua

tôi có thắc mắc về file terraform bạn tạo, bởi vì tôi đang ở Việt Nam nên để tối ưu chí phi, region AWS nên chọn là gì, có phải ap-southeast-1 như hiện tại không? best practice là gì?

check lại status plan xem đã cập nhật hết chưa? cleanup CI theo policy chưa? action tiếp theo là gì? còn PR nào?

kitehub đã chạy local end-to-end rồi đúng không? các AI được dùng ở local đúng như design đúng không?

check lại design, AI sẽ là AI local thay vì AI qua API key mà nhỉ?

tạo tài liệu thôi

bạn chọn template nào theo plan?

cập nhật plan theo template này
mục tiêu đỏ: landing pages phải đẹp nhất có thể, còn lại UI phải phù hợp, tương thích với tệp người dùng kiteclass

tương tự, tôi muốn cải thiện cả UI của kiteclass theo templates nữa
kitehub chỉ cần 1 UI nhưng tôi muốn kiteclass có nhiều UI để cho người dùng chọn lựa, có được không?

việc định nghĩa "nhiều UI" cần quay lại câu chuyện branding, từ dữ liệu input của khách hàng, AI branding sẽ render được nhiều ảnh khác nhau, vậy để xử lý lượng output này lên UI thì best practice cho định nghĩa "nhiều UI" và cho kiteclass FE là gì?

Sau khi AI branding thì sẽ đưa output lên các UI để cho người dùng chọn, vậy cần render kiteclass FE ra raw link để khách hàng xem đúng không? liệu đây có phải best practice?

tức là sẽ khiến FE instance nặng lên đúng không?

nhưng mà chỉ đổi CSS mà không đổi layout của landing pages thì có hợp lý để tiếp cận khách hàng không?

ngoài ra còn 1 vấn đề nữa, lượng output (ảnh) được AI branding tạo ra sẽ được đẩy lên UI theo quy chuẩn, quy tắc gì, liệu có nên cho phép người dùng chọn vị trí cho output branding lên UI không?

1 vấn đề nữa, thiết kế hiện tại yêu cầu mọi input đều phải qua AI branding, nhưng có những AI như chứng chỉ học viên đạt được, không cần qua branding, chỉ cần đầy lên UI, vậy có cần thiết kế lại quy trình khởi tạo instance không?

vậy đối với mỗi loại khách hàng: giáo viên độc lập, trung tâm, trường học, ... sẽ cần ít nhất 1 template + mỗi template sẽ có nhiều theme để tránh trùng lặp giữa các instance? liệu có hợp lý?

hãy quét toàn bộ documents để tìm tài liệu liên quan, bắt đầu PR để update toàn bộ tài liệu theo design mới

hãy tạo 1 skill continue, skill này yêu cầu thực hiện action ưu tiên nhất, theo chuẩn của action đó

chưa ăn skill, có vẽ chưa đúng cấu trúc? hay cần restart?

vẫn chưa được, bổ sung vào PR, thêm hình ảnh và hiệu ứng để landing pages phong phú hơn

tiếp tục bổ sung vào PR:
1. Tất cả tính năng bạn cần => có nút tìm hiểu thêm => bấm vào thì ra panel chia đôi hợp lý mô tả về tính năng + hình ảnh
2. Câu hỏi thường gặp => hơi đơn giản => cải thiện như trên hoặc hợp lý hơn

đối với 2 section trên, tôi muốn nó mở rộng ở trên chính page thay vì mở dialog, mở rộng ra visual tốt hơn

đối với cả 2 section, tôi muốn khi 1 tab mở rộng thì các tab nhỏ còn lại đầy hết về bên phải trên 1 hàng dọc được không, đẹp mắt hơn so với hiện tại

Features - khi expand: lần đầu bấm sẽ tab expand sẽ bị dịch xuống so với cột feature vài px, sau khi đổi sang feature khác thì mới về ngang lại

FAQ - khi expand:
1. vẫn hơi bé
2. nội dung giải thích còn tòm tắt, cho người hiểu về tech, chưa phù hợp với tệp khách hàng của kiteclass, cần bổ sung content kỹ hơn

FAG cần chia 2 hàng dọc để khớp height của panel được mở rộng, panel mở rộng cần thêm width cho đẹp hơn, height thì phải khớp với hàng dọc

các panel ở 2 cột đạng bị thu gọn, check lại margin và mở lại ra với height hàng hợp lý