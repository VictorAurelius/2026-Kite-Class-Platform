---
name: Self-test before push
description: PHẢI chạy scripts/test-local.sh trước push. CI là safety net, không phải nơi phát hiện lỗi.
type: feedback
---

PHẢI test local trước push. CI là safety net cuối cùng.

**Why:** Dự án trước push code có Checkstyle violation → CI fail 3 lần → mất thời gian. Nếu chạy test local trước, phát hiện ngay trong 5 giây.

**How to apply:**
1. Sau commit, TRƯỚC push → `scripts/test-local.sh`
2. Quick mode: `scripts/test-local.sh --quick`
3. Nếu fail → fix rồi commit mới, KHÔNG push broken code
