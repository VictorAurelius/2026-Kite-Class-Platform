---
chapter: 2
generated: 2026-05-23
audit_source: .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-2.json
---

# Chapter 2 — Index hình ảnh + bảng

File: `chapter-2-system-architecture.md` (778 dòng, 8 Mermaid diagrams)

## Hình ảnh (figure / diagram)

| # | Loại | File / Block | Caption | Vị trí |
|:-:|------|--------------|---------|--------|
| 2.1 | Mermaid `flowchart TB` | line 195 | ⚠️ thiếu bold caption — body text đã cite `Hình 2.1`; đề xuất `**Hình 2.1: C4 Level 1 — Sơ đồ ngữ cảnh hệ thống Kite Platform**` | §2.2.1 (heading inferred) — phần "Mô hình C4 (Context / Container / Component / Code)" |
| 2.2 | Mermaid `flowchart TB` | line 246 | ⚠️ thiếu bold caption — body đã cite `Hình 2.2`; đề xuất `**Hình 2.2: C4 Level 2 — Bố cục container Kite Platform**` | §2.2.2 Sơ đồ container — C4 Level 2 |
| 2.3 | Mermaid `flowchart TD` | line 382 | ⚠️ thiếu bold caption — body đã cite `Hình 2.3`; đề xuất `**Hình 2.3: Phòng thủ chiều sâu — 5 lớp cô lập cơ sở dữ liệu cho tenantId**` | §2.2.4 Phòng thủ chiều sâu |
| 2.4 | Mermaid `sequenceDiagram` | line 443 | ⚠️ thiếu bold caption — body đã cite `Hình 2.4`; đề xuất `**Hình 2.4: Sequence diagram cho luồng JWT authentication + role-guard + truyền tenantId**` | §2.2.5 Quy trình xác thực |
| 2.5 | Mermaid `classDiagram` | line 498 | ⚠️ thiếu bold caption — body đã cite `Hình 2.5`; đề xuất `**Hình 2.5: Class diagram — Core Domain entity giáo dục đa tenant**` | §2.3.1 Class Diagram |
| 2.6 | Mermaid `erDiagram` | line 582 | ⚠️ thiếu bold caption — body đề cập gián tiếp; đề xuất `**Hình 2.6: ERD — Sơ đồ quan hệ thực thể tầng lưu trữ**` | §2.3.2 ERD |
| 2.7 | Mermaid `sequenceDiagram` | line 604 | ⚠️ thiếu bold caption — body đã cite `Hình 2.7`; đề xuất `**Hình 2.7: Sequence diagram cho luồng cấp phát tenant từ beta request đến first login**` | §2.3.3 Sequence Diagram |
| 2.8 | Mermaid `stateDiagram-v2` | line 637 | ⚠️ thiếu bold caption — body đã cite `Hình 2.8`; đề xuất `**Hình 2.8: Máy trạng thái 5-state vòng đời tenant**` | §2.3.4 Máy trạng thái vòng đời tenant |

## Bảng

| # | File / Block | Caption | Vị trí |
|:-:|--------------|---------|--------|
| — | (chưa có bảng được đánh số `**Bảng 2.M**` chính thức) | — | — |

> **Ghi chú:** chapter chứa nhiều markdown table inline (vd matrix actor, container responsibility) nhưng chưa wrap caption `**Bảng 2.M: ...**`. Đề xuất rà soát bổ sung khi refresh.

## Tóm tắt audit

- **Tổng visual block:** 8 (toàn Mermaid)
- **Caption coverage:** 0/8 (0%) — ⚠️ toàn bộ thiếu bold caption underneath; body text đã cite đúng số `Hình 2.1` → `2.8`
- **Numbering integrity:** ✅ implicit OK (citations rải đều `2.1` → `2.8` theo thứ tự appearance)
- **Citation coverage:** ✅ implicit ~100% (mỗi block có citation trong ±3 đoạn dựa trên context grep)
- **Action chính:** bổ sung 8 dòng `**Hình N.M: ...**` ngay dưới fence ``` đóng của mỗi Mermaid block

## Trạng thái figure cần bổ sung

- [ ] Thêm caption `**Hình 2.1: C4 Level 1 — Sơ đồ ngữ cảnh hệ thống Kite Platform**` sau Mermaid block line ~196-235 trong `chapter-2-system-architecture.md`
- [ ] Thêm caption `**Hình 2.2: C4 Level 2 — Bố cục container Kite Platform**` sau Mermaid line ~246
- [ ] Thêm caption `**Hình 2.3: Phòng thủ chiều sâu — 5 lớp cô lập cơ sở dữ liệu cho tenantId**` sau Mermaid line ~382
- [ ] Thêm caption `**Hình 2.4: Sequence diagram cho luồng JWT authentication + role-guard + truyền tenantId**` sau Mermaid line ~443
- [ ] Thêm caption `**Hình 2.5: Class diagram — Core Domain entity giáo dục đa tenant**` sau Mermaid line ~498
- [ ] Thêm caption `**Hình 2.6: ERD — Sơ đồ quan hệ thực thể tầng lưu trữ**` sau Mermaid line ~582
- [ ] Thêm caption `**Hình 2.7: Sequence diagram cho luồng cấp phát tenant từ beta request đến first login**` sau Mermaid line ~604
- [ ] Thêm caption `**Hình 2.8: Máy trạng thái 5-state vòng đời tenant**` sau Mermaid line ~637
- [ ] Rà soát markdown table inline; bổ sung `**Bảng 2.M: ...**` cho ≥2 bảng chính (actor matrix, container responsibility)

## Last audit

```bash
bash .claude/skills/quality/thesis-figure-curation/scripts/audit-figures.sh --json \
  documents/08-thesis/chapter-2-system-architecture.md \
  > .claude/skills/quality/thesis-figure-curation/data/last-run-chapter-2.json
```

Audit timestamp: 2026-05-23
