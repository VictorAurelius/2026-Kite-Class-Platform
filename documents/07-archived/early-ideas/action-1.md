vẫn chưa fix được
chỉ còn 4 problems này

hãy fix CI về build docker

như vậy có test được build all kiteclass không? hay cần đổi quan điểm build lên CI?

fix Docker Build/Push Workflow

check lại policy clean up CI và clean history

bật mode plan:
1. CI của build Docker dùng pass vẫn bị đánh status là fail, update lại workflow
2. CI của các service pass nhưng còn tồn tại nhiều annotations warning, thực hiện fix hết
3. còn nhiều file md nằm ở root, root của các folder, chưa được phân loại đúng cách, phân loại và move vào đúng thư mục
4. update readme của dự án

hãy đọc documents\06-logs\week-5-completion-report.md để hiểu trạng thái project hiện tại, thực hiện check ci status và fix