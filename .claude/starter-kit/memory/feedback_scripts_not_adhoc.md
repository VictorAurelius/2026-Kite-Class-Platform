---
name: Scripts not ad-hoc commands
description: PHẢI dùng scripts cho test, CI, docker — KHÔNG lệnh tự do. Đã vi phạm nhiều lần ở dự án trước.
type: feedback
---

Mọi operation có script thì PHẢI dùng script, KHÔNG chạy lệnh ad-hoc.

**Why:** Dự án trước vi phạm 4+ lần: dùng `gh run watch` thay vì `scripts/check-ci.sh`, dùng `mvnw test` thay vì `scripts/test-local.sh`, dùng `docker-compose` thay vì `scripts/dev-up.sh`. Mỗi lần mất thời gian sửa skill + memory.

**How to apply:**
| Operation | Script | KHÔNG dùng |
|-----------|--------|-----------|
| Test local | `scripts/test-local.sh` | `mvnw test`, `vitest run`, `pytest` |
| CI monitor | `scripts/check-ci.sh` | `gh run watch`, `sleep + poll` |
| CI status | `scripts/check-ci.sh --status` | `gh run list` |
| Docker | `scripts/dev-up.sh` etc. | `docker-compose` trực tiếp |
