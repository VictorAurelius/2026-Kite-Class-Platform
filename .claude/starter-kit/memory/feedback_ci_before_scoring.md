---
name: CI must complete before scoring
description: KHÔNG kết luận CI pass/fail khi CI còn in_progress.
type: feedback
---

Khi đánh giá chất lượng, KHÔNG giả định CI pass khi còn đang chạy.

**Why:** Dự án trước đã kết luận CI score = 9/10 trong khi CI còn in_progress. User phát hiện → mất credibility.

**How to apply:**
1. Dùng `scripts/check-ci.sh --status` để kiểm tra
2. Nếu in_progress → dùng `scripts/check-ci.sh` (wait mode) hoặc báo user
3. Ghi "PENDING" nếu chưa có kết quả, KHÔNG đoán
