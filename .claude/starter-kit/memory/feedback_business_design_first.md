---
name: Business design first
description: PHẢI thiết kế business docs TRƯỚC code. Audit sớm, audit thường xuyên.
type: feedback
---

PHẢI thiết kế nghiệp vụ TRƯỚC code. Không nhảy vào implementation mà chưa có business documents.

**Why:** Dự án trước merge 188 PRs → phát hiện 22 business gaps. Root cause: code trước design sau. Quality score 91/100 nhưng business gap chỉ 45%.

**How to apply:**
1. Module mới → tạo business docs TRƯỚC (rules.md, use-cases.md, api-contract.md)
2. Mỗi PR → check business doc cùng commit
3. Mỗi wave → quality audit + business gap check
4. KHÔNG hardcode business constants — dùng config
5. Plans/gap reports PHẢI update sau mỗi wave
