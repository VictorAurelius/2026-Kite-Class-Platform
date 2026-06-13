---
chapter: 4
generated: 2026-05-23
audit_source: .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-4.json
---

# Chapter 4: Index hình ảnh + bảng

File: `chapter-4-deployment-results.md` (310 dòng, 4 Mermaid diagrams)

## Hình ảnh (figure / diagram)

| # | Loại | File / Block | Caption | Vị trí |
|:-:|------|--------------|---------|--------|
| 4.1 | Mermaid | line 24 | ⚠️ thiếu bold caption, đề xuất `**Hình 4.1: Sơ đồ hạ tầng AWS Singapore Phase 1 BETA**` | §4.1.2 Sơ đồ hạ tầng |
| 4.2 | Mermaid | line 82 | ⚠️ thiếu bold caption, đề xuất `**Hình 4.2: Sơ đồ CI/CD pipeline GitHub Actions với OIDC + workflow_dispatch**` | §4.1.4 CI/CD Pipeline |
| 4.3 | Mermaid | line 177 | ⚠️ thiếu bold caption, đề xuất `**Hình 4.3: Measurement plan: luồng thu thập KPI metrics**` | §4.3.2 Measurement Plan |
| 4.4 | Mermaid | line 285 | ⚠️ thiếu bold caption, đề xuất `**Hình 4.4: Định hướng phát triển hệ thống giai đoạn sau Phase 1 BETA**` | §4.4 cuối (gần §4.4.5 Định hướng tương lai) |

## Bảng

| # | File / Block | Caption | Vị trí |
|:-:|--------------|---------|--------|
| không có | (chưa có bảng được đánh số `**Bảng 4.M**` chính thức) | không có |, |

> **Ghi chú:** §4.3.1 KPI Metrics table inline (đã trích đoạn `Support Ticket Rate / Crash-Free Rate` trong audit context grep) chưa wrap caption `**Bảng 4.M: ...**`. Đề xuất bổ sung khi refresh.

## Tóm tắt audit

- **Tổng visual block:** 4 (toàn Mermaid)
- **Caption coverage:** 0/4 (0%), ⚠️ tất cả Mermaid blocks thiếu bold caption underneath
- **Numbering integrity:** N/A (chưa có figure được đánh số chính thức)
- **Citation coverage:** N/A, chưa có caption nên không thể audit citation
- **Action chính:** bổ sung 4 dòng `**Hình N.M: ...**` ngay dưới mỗi Mermaid fence ``` đóng

## Trạng thái figure cần bổ sung

- [ ] §4.1.2: thêm caption `**Hình 4.1: Sơ đồ hạ tầng AWS Singapore Phase 1 BETA**` sau Mermaid block line ~24-80
- [ ] §4.1.4: thêm caption `**Hình 4.2: Sơ đồ CI/CD pipeline GitHub Actions với OIDC + workflow_dispatch**` sau Mermaid block line ~82-175
- [ ] §4.3.2: thêm caption `**Hình 4.3: Measurement plan: luồng thu thập KPI metrics**` sau Mermaid block line ~177-283
- [ ] §4.4: thêm caption `**Hình 4.4: Định hướng phát triển hệ thống giai đoạn sau Phase 1 BETA**` sau Mermaid block line ~285
- [ ] §4.3.1 KPI metrics: bổ sung `**Bảng 4.1: KPI metrics Phase 1 BETA (uptime / latency / crash-free rate / support ticket rate)**`
- [ ] Verify citation trong body cho mỗi `Hình 4.N` (heuristic ±3 đoạn) sau khi thêm caption

## Last audit

```bash
bash .claude/skills/quality/thesis-figure-curation/scripts/audit-figures.sh --json \
  documents/08-thesis/chapter-4-deployment-results.md \
  > .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-4.json
```

Audit timestamp: 2026-05-23
