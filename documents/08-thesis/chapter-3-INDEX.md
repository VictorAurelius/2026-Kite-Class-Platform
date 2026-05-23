---
chapter: 3
generated: 2026-05-23
audit_source: .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-3.json
---

# Chapter 3 — Index hình ảnh + bảng

File: `chapter-3-implementation.md` (145 dòng, 8 PNG screenshots + 1 Mermaid)

## Hình ảnh (figure / diagram / screenshot)

| # | Loại | File / Block | Caption | Vị trí |
|:-:|------|--------------|---------|--------|
| 3.1 | PNG screenshot | line 70 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.1: Trang chủ marketing KiteHub (landing page)**` | §3.2.1 Luồng khám phá và kích hoạt tenant |
| 3.2 | PNG screenshot | line 71 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.2: Wizard đăng ký chủ trung tâm — bước 1 thông tin cơ bản**` | §3.2.1 |
| 3.3 | PNG screenshot | line 72 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.3: Tenant provisioning success screen**` | §3.2.1 |
| 3.4 | PNG screenshot | line 73 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.4: Dashboard chủ trung tâm sau first login**` | §3.2.1 |
| 3.5 | PNG screenshot | line 82 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.5: Màn hình quản lý lớp học của chủ trung tâm**` | §3.2.2 Luồng vận hành nghiệp vụ thường nhật |
| 3.6 | PNG screenshot | line 83 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.6: Trình tạo hoá đơn cho học viên**` | §3.2.2 |
| 3.7 | PNG screenshot | line 92 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.7: Email chào mừng gửi từ KiteHub**` | §3.2.3 Luồng điều hành Admin nền tảng |
| 3.8 | PNG screenshot | line 93 | ⚠️ chỉ có alt text — đề xuất `**Hình 3.8: Trang nhật ký audit cho Platform Admin**` | §3.2.3 |
| 3.9 | Mermaid (test pyramid) | line 114 | ⚠️ thiếu bold caption — đề xuất `**Hình 3.9: Kim tự tháp kiểm thử KiteHub theo mô hình Mike Cohn (unit / integration / E2E)**` | §3.3 Kiểm thử và đánh giá chất lượng |

## Bảng

| # | File / Block | Caption | Vị trí |
|:-:|--------------|---------|--------|
| — | (chưa có bảng được đánh số `**Bảng 3.M**` chính thức) | — | — |

> **Ghi chú:** §3.1 Công nghệ sử dụng có markdown table (tech stack list) nhưng chưa wrap caption `**Bảng 3.M: ...**`. Đề xuất bổ sung khi refresh.

## Tóm tắt audit

- **Tổng visual block:** 9 (8 PNG screenshots + 1 Mermaid)
- **Caption coverage:** 0/9 (0%) — ⚠️ tất cả figure chỉ có alt text trong cú pháp `![alt](path)`, chưa có bold caption underneath
- **Numbering integrity:** N/A (chưa có figure được đánh số chính thức)
- **Citation coverage:** N/A — chưa có caption nên không thể audit citation
- **Action chính:** bổ sung 9 dòng `**Hình N.M: ...**` ngay dưới mỗi `![...](...)` line OR sau Mermaid fence ``` đóng

## Trạng thái figure cần bổ sung

- [ ] §3.2.1: thêm 4 captions `**Hình 3.1**` → `**Hình 3.4**` sau mỗi screenshot line 70-73
- [ ] §3.2.2: thêm 2 captions `**Hình 3.5**` → `**Hình 3.6**` sau mỗi screenshot line 82-83
- [ ] §3.2.3: thêm 2 captions `**Hình 3.7**` → `**Hình 3.8**` sau mỗi screenshot line 92-93
- [ ] §3.3: thêm caption `**Hình 3.9: Kim tự tháp kiểm thử KiteHub theo mô hình Mike Cohn**` sau Mermaid block line 114
- [ ] Bổ sung citation trong body text cho mỗi Hình 3.N (heuristic ±3 đoạn)
- [ ] §3.1: bổ sung `**Bảng 3.1: Tech stack Phase 1 BETA**` cho markdown table tech stack

## Last audit

```bash
bash .claude/skills/quality/thesis-figure-curation/scripts/audit-figures.sh --json \
  documents/08-thesis/chapter-3-implementation.md \
  > .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-3.json
```

Audit timestamp: 2026-05-23
