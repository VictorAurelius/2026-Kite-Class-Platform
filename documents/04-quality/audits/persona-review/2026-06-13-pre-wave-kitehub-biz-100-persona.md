---
audience: mixed
---

# Outside-In Persona Audit — KiteHub Subscription Lifecycle (biz-100)

**Ngày:** 2026-06-13
**Loại:** Persona simulation (outside-in) per `.claude/skills/quality/persona-based-business-review/SKILL.md`
**Phạm vi:** Full subscription lifecycle KiteHub (signup → trial → trial→paid → ACTIVE → upgrade/downgrade → grace → renew → involuntary lapse → cancel → reactivate/win-back)
**Investigation order:** DESIGN trước (rules.md TR/SUB/T2P/OFF/RET + ADR), GAPS dedup, KHÔNG đọc code (đây là expectation gap audit, không phải code-correctness)
**Mục tiêu:** tìm gap tâm lý / kỳ vọng / communication / UX mà inside-out brainstorm (6 cụm A-F) MISSED

---

## 1. Persona profile

**Chị Hằng — chủ trung tâm dạy thêm nhỏ (P2_CENTER_OWNER)**
- 38 tuổi, chủ 1 trung tâm luyện thi ~160 học viên, 6 giáo viên, 2 cơ sở (cơ sở chính + chi nhánh mới mở).
- Không rành kỹ thuật. Dùng điện thoại nhiều hơn laptop. Giao tiếp công việc chủ yếu qua **Zalo**, ít check email.
- Quản lý tài chính chặt: mọi khoản chi cần "nhìn thấy hóa đơn rõ ràng" và thường chuyển khoản qua app ngân hàng (Vietcombank/MB) — quen với MoMo "chuyển là xong ngay".
- Tâm lý: lo mất dữ liệu học viên/điểm số (tài sản lớn nhất của trung tâm); ngại commit tiền khi chưa chắc; dễ hoảng nếu "đã trả tiền mà không thấy gì".
- Mùa cao điểm: đầu năm học + trước kỳ thi → cần nâng cấp/mở rộng GẤP, không chờ được.

**Phụ persona — anh Tuấn (Solo teacher, P1):** 1 giáo viên, 25 học viên, thử nghiệm rồi mới quyết. Đại diện cho nhóm "thử trial xong bỏ rồi quay lại sau".

---

## 2. Journey table — friction từng bước

| # | Bước | Owner kỳ vọng gì | Hệ thống làm gì (theo design) | Gap (tâm lý / UX / communication) |
|---|------|------------------|-------------------------------|-----------------------------------|
| 1 | Signup → trial 14 ngày | Tạo tài khoản, dùng thử miễn phí; mỗi cơ sở thử riêng | TR-01 trial 14d; TR-02 max 1 trial/owner; TR-07 chặn re-trial vĩnh viễn | Owner 2 cơ sở thấy chi nhánh 2 "đã đếm ngược sẵn" (extends GAP-959); re-trial bị chặn vĩnh viễn không lối liên hệ |
| 2 | Warning email day 11/13 | Được nhắc ĐỦ SỚM để kịp xoay tiền + duyệt ngân sách | TR-03 warning day 7 (midpoint) + day 11 + day 13 | 3+1 ngày QUÁ NGẮN cho chu kỳ VietQR thủ công + chờ admin confirm; email không nói "cần chuyển khoản trước ngày Y vì admin xác nhận mất thời gian" |
| 3 | Trial sắp hết (chần chừ) | Nếu lỡ quên vẫn có cách cứu, không mất data ngay | T2P-05 rescue window 24h; TR-05 giữ data 7 ngày sau suspend | Email suspend KHÔNG trấn an "data giữ 7 ngày" + KHÔNG báo "còn 24h nâng gói không mất gì" → owner hoảng/bỏ cuộc |
| 4 | Quyết định nâng gói (chọn tier) | Thấy giá rõ, biết gói nào hợp với mình | SUB-22 entitlement matrix (10/50/200/∞ học viên); ENTERPRISE = giá "custom" | Không có gợi ý chọn tier theo số học viên thực tế → owner đoán sai tier; ENTERPRISE "custom" không có đường "liên hệ tư vấn" |
| 5 | Trả VietQR + chờ admin confirm | Chuyển khoản xong → kích hoạt NGAY (quen MoMo) | SUB-11/18/19 chuyển khoản thủ công, admin đối soát rồi confirm; tier flip chỉ sau confirm | **KHÔNG có trạng thái "đang chờ xác nhận" / ETA / thông báo khi confirm** → owner đã trả tiền mà "im lặng" → khủng hoảng niềm tin ("có bị lừa không?") |
| 6 | Nâng tier cao hơn (đang dùng) | Trả thêm tiền → dùng tính năng mới NGAY (mùa cao điểm) | SUB-07 set pendingTier, tier chỉ apply SAU admin confirm; SUB-10 prorated | Tính năng bị khóa tới khi admin confirm (mất thời điểm cần gấp); số tiền prorated lạ, không giải thích "vì sao chỉ trả 600k" |
| 7 | Muốn hạ tier | Hạ gói tiết kiệm, VẪN giữ học viên/data | SUB-08/09 downgrade cuối chu kỳ (pendingTier) | **Không cảnh báo:** 160 học viên > cap BASIC 50, 8GB > 2GB, custom domain mất → owner bị "úp sọt" mất quyền truy cập/khóa học viên |
| 8 | Gần hết hạn (grace) | Được nhắc, có thời gian gia hạn; biết còn dùng được gì | SUB-04 grace 3 ngày; SUB-05 warning 7/3/1 | Grace 3 ngày quá ngắn cho chu kỳ trả thủ công; design không nói grace = full access hay read-only → owner không rõ |
| 9 | Auto-renew | Bật auto-renew = không lo mất dịch vụ | SUB-03 auto-renew mặc định `true`; NHƯNG SUB-11 không lưu thẻ/không auto-capture | **Mâu thuẫn:** "auto-renew ON" nhưng KHÔNG thể tự trừ tiền → owner tưởng an toàn → vẫn bị suspend khi không chủ động chuyển khoản |
| 10 | Quên trả tiền (involuntary) | Có cơ hội cứu vãn, không mất data ngay | grace 3d → suspend → RET-01..05 giữ data theo tier (FREE/BASIC 7/30d, PREMIUM 60d) | Thiếu chuỗi dunning "đã lỡ hạn — bấm đây để khôi phục"; owner không biết data còn an toàn bao lâu |
| 11 | Muốn hủy | Hủy dễ, LẤY ĐƯỢC data học viên/điểm ra trước | OFF-01 self-service cancel; OFF-05 export bundle 24h; OFF-07 undo 30d | Luồng hủy có ÉP export trước không? Có quảng bá "hoàn tác trong 30 ngày" không? Nếu không → owner mất data hoặc không biết có thể undo (extends GAP-1017) |
| 12 | Sau hủy, data còn không | Data còn 1 thời gian, dự đoán được khi nào xóa | Trial 7d (TR-05) / lapse tier 7-90d (RET) / cancel 90d (OFF-04) — **3 chế độ khác nhau** | Owner không đoán được "data giữ bao lâu" vì phụ thuộc CÁCH rời đi; cancel (90d) hào phóng hơn lapse FREE/BASIC (7/30d) — incentive ngược |
| 13 | Muốn quay lại (reactivate/win-back) | Đăng ký lại bằng email/SĐT cũ, data còn nguyên | OFF-15 tombstone chặn re-signup cùng identifier (chống fraud); TR-07 chặn re-trial | **Khách hàng quay lại chính đáng bị chặn**: email/SĐT đã tombstone → không đăng ký lại được; không có đường win-back |

---

## 3. Findings (chi tiết)

### F1 — Chờ admin confirm thanh toán KHÔNG có trạng thái / SLA / thông báo cho owner (P1, bước 5)
**NET-NEW.** Design SUB-19 mô tả CƠ CHẾ (admin đối soát + confirm) nhưng im lặng hoàn toàn về owner-facing UX. Owner chuyển khoản xong rơi vào khoảng đen: không thấy "đang chờ xác nhận", không có ETA ("admin xác nhận trong vòng X giờ làm việc"), không nhận thông báo khi đã confirm. Với người quen MoMo (trả là xong ngay), sự im lặng này = khủng hoảng niềm tin ("tôi đã chuyển tiền mà sao chưa thấy gì?"). Solo-dev admin → confirm có thể qua đêm, làm trầm trọng thêm.
*Fix:* màn hình "Đang chờ xác nhận thanh toán" + hiển thị SLA confirm + email/Zalo + in-app khi confirmed (GAP-974 lo email-sau-confirm; F1 lo khoảng-CHỜ-trước-confirm).

### F2 — "Auto-renew: ON" mâu thuẫn ngữ nghĩa với VietQR thủ công (P1, bước 9)
**NET-NEW.** SUB-03 đặt `autoRenew=true` mặc định, nhưng SUB-11 Phase 1 BETA không lưu thẻ / không auto-capture. "Auto-renew" ngụ ý tự động trừ tiền — bất khả thi trong luồng thủ công. Owner thấy toggle "ON" → tin rằng dịch vụ tự gia hạn → KHÔNG chủ động chuyển khoản → bị suspend bất ngờ. Lời hứa sai do design.
*Fix:* đổi nhãn thành "Tự động nhắc gia hạn (cần chuyển khoản thủ công)" + reminder rõ trước cuối chu kỳ; HOẶC tắt auto-renew mặc định trong Phase 1 BETA để không hứa hão.

### F3 — Hạ tier không cảnh báo mất học viên / dung lượng / custom domain vượt cap (P1, bước 7)
**NET-NEW (extends-GAP-1018 về luồng kỹ thuật).** PREMIUM→BASIC: 160 học viên > cap 50, 8GB > cap 2GB, custom domain (✅PREMIUM → ❌BASIC) ngừng hoạt động → URL thương hiệu của trung tâm chết, phụ huynh/học viên không vào được. SUB-08/09 chỉ nói "áp dụng cuối chu kỳ" — im lặng hoàn toàn về xử lý data vượt cap + cảnh báo owner. Prompt yêu cầu soi đúng điểm này. GAP-1018 cover downgrade-FREE kỹ thuật, GAP-071 (phase-2) cover branding; KHÔNG cái nào cover cảnh báo data-loss owner-facing.
*Fix:* màn hình preview tác động trước khi hạ ("Bạn có 160 học viên, gói BASIC chỉ cho 50 — N học viên sẽ bị khóa; custom domain sẽ ngừng") + bắt confirm chủ động.

### F4 — Tombstone (OFF-15) + chặn re-trial (TR-07) chặn cả khách hàng quay lại chính đáng (P1, bước 13)
**NET-NEW.** OFF-15 lưu tombstone hash email/SĐT để chống fraud re-signup; TR-07 chặn re-trial vĩnh viễn. Hệ quả: khách hàng cũ thực sự (đã hủy/để purge) muốn quay lại sau 6 tháng bị chặn đăng ký lại bằng chính email/SĐT của họ + không được trial lại. Cơ chế chống-fraud va chạm với win-back hợp pháp. Không doc nào có đường "khách hàng quay lại".
*Fix:* phân biệt fraud-block với voluntary-cancel; cho phép reactivate/re-signup bằng identifier cũ cho khách rời đi tự nguyện; thêm win-back offer (giảm giá quay lại).

### F5 — Ba chế độ retention khác nhau làm owner không đoán được "data giữ bao lâu" (P1, bước 12)
**NET-NEW.** Trial = 7 ngày (TR-05); involuntary lapse = theo tier 7-90 ngày (RET-01..05); voluntary cancel = 90 ngày (OFF-04). Owner không thể dự đoán data giữ bao lâu vì phụ thuộc CÁCH rời đi. Nghịch lý incentive: hủy tự nguyện (90d) hào phóng hơn lapse FREE/BASIC (7d/30d) → người quên trả (vẫn muốn ở lại) bị phạt nặng hơn người chủ động bỏ.
*Fix:* hợp nhất messaging + hiển thị NGÀY CỤ THỂ ("data của bạn giữ đến 28/07/2026") theo từng đường thoát; cân nhắc nâng retention cho involuntary-lapse paid tiers.

### F6 — Lead-time cảnh báo (trial + grace) quá ngắn so với chu kỳ trả-thủ-công + chờ-confirm (P1, bước 2+8)
**NET-NEW.** Trial warning day 11/13 (3+1d), grace 3 ngày — nhưng chu kỳ thực tế = duyệt ngân sách + chuyển khoản + admin đối soát/confirm có thể 1-2 ngày làm việc. Owner hết runway giữa chừng → instance suspend khi đang quyết. GAP-959 lo trial-clock-shared, KHÔNG lo lead-time vs manual cycle.
*Fix:* cảnh báo sớm hơn (day 7 nói rõ "bắt đầu thanh toán ngay") + nói cho owner biết lead-time confirm; HOẶC nới grace để bao trọn chu kỳ thủ công.

### F7 — Email suspend/expiry không trấn an data-an-toàn + không nêu rescue window (P1, bước 3+10)
**NET-NEW.** T2P-05 rescue 24h + RET retention theo tier đều tồn tại nhưng email suspend không nói "data giữ X ngày" + không nói "còn 24h nâng gói không mất gì" + không có CTA khôi phục 1-bấm. Owner đọc email suspend → tưởng mất hết → hoảng hoặc bỏ.
*Fix:* email suspend/expiry BẮT BUỘC nêu thời hạn retention + rescue/reactivate CTA 1-bấm.

### F8 — Luồng hủy không ép export data + không quảng bá undo 30 ngày (P1/P2, bước 11)
**NET-NEW (mặt UX của GAP-1017).** OFF-05 export bundle + OFF-07 undo-30d có trong design nhưng owner-facing cancel UX có ép "tải dữ liệu học viên/điểm về" trước khi hủy không? có quảng bá "hoàn tác trong 30 ngày" không? Nếu không → owner mất data học viên/điểm (tài sản lớn nhất) hoặc hủy nhầm mà không biết có thể undo → churn không đáng có. GAP-1017 lo deprovision kỹ thuật.
*Fix:* cancel wizard bắt buộc bước "Tải dữ liệu về" + hiển thị nổi bật "Có thể hoàn tác trong 30 ngày".

### F9 — Thiếu gợi ý chọn tier — owner đoán sai gói + ENTERPRISE "custom" không có đường liên hệ (P2, bước 4)
**NET-NEW.** SUB-22 matrix tồn tại nhưng UI nâng gói không gợi ý theo số học viên thực tế → owner 160 học viên có thể chọn nhầm BASIC (cap 50) rồi vỡ cap bất ngờ. ENTERPRISE giá "custom" (priceVND=0) → owner cần ∞ học viên gặp ngõ cụt, không có "liên hệ tư vấn".
*Fix:* tier recommender theo số học viên hiện tại ("Bạn có 160 học viên → gói PREMIUM phù hợp") + nút "Liên hệ tư vấn" cho ENTERPRISE.

### F10 — Thông báo chỉ qua email bỏ lọt owner dùng Zalo → involuntary churn (P2, mọi bước notification)
**NET-NEW (VN-localization channel).** Toàn bộ cảnh báo (trial, expiry, grace, retention) qua email. Chủ trung tâm VN chủ yếu dùng **Zalo**, ít check email → bỏ lọt cảnh báo gia hạn = mất dịch vụ ngoài ý muốn (churn họ không hề chọn). GAP-701 lo email-service-impl, không lo channel-choice.
*Fix:* fallback Zalo OA / in-app persistent banner (không chỉ email); tối thiểu banner cảnh báo nổi bật trong dashboard.

### F11 — Số tiền prorated khi nâng tier không giải thích cho owner không-kỹ-thuật (P2/P3, bước 6)
**NET-NEW.** SUB-10 công thức prorated; owner thấy con số lạ ("sao chỉ 600.000đ?") không có breakdown → nghi ngờ tính sai.
*Fix:* hiển thị breakdown "Còn 12 ngày gói cũ → chỉ trả phần chênh lệch 600.000đ".

---

## 4. Tổng kết dedup

| Finding | Severity | Dedup |
|---------|----------|-------|
| F1 chờ-confirm không status/SLA | P1 | NET-NEW (extends-GAP-974 ở email-sau-confirm) |
| F2 auto-renew mâu thuẫn manual | P1 | NET-NEW |
| F3 downgrade không cảnh báo over-cap | P1 | NET-NEW (extends-GAP-1018) |
| F4 tombstone/re-trial chặn win-back | P1 | NET-NEW |
| F5 3 chế độ retention không nhất quán | P1 | NET-NEW |
| F6 lead-time cảnh báo ngắn vs manual cycle | P1 | NET-NEW |
| F7 email suspend không trấn an + rescue | P1 | NET-NEW |
| F8 cancel không ép export + undo | P1/P2 | NET-NEW (extends-GAP-1017) |
| F9 thiếu gợi ý chọn tier + ENTERPRISE dead-end | P2 | NET-NEW |
| F10 email-only bỏ lọt Zalo | P2 | NET-NEW |
| F11 prorated không giải thích | P2/P3 | NET-NEW |

**11 findings, 11 NET-NEW** (8 extends/overlap nhẹ với 6 cụm inside-out nhưng đều bổ sung GÓC NHÌN OWNER mà cụm kỹ thuật miss). Inside-out 6 cụm (A-F) là correctness/IDOR/test-infra; outside-in audit này phơi ra lớp **communication + expectation + win-back + data-loss-warning** — lớp owner thật sự cảm nhận và là nguyên nhân churn/bounce chính.
