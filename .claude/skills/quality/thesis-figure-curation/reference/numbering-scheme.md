# Numbering Scheme — `<chapter>.<sequential>` restart per chapter

## 1. Format

```
Hình N.M    (figure / diagram / screenshot / Mermaid / PlantUML)
Bảng N.M    (table)
Listing N.M (code snippet >20 lines, optional)
```

- `N` = chapter number (1, 2, 3, 4 — Vietnamese academic convention dùng decimal số nguyên)
- `M` = sequential trong chapter (1, 2, 3, ..., bắt đầu lại từ 1 cho mỗi chapter)
- Separator = dấu chấm `.` (KHÔNG `-`, `_`, hay `:`)

## 2. Restart per chapter

```
Chapter 1:
  Hình 1.1: Tổng quan thị trường edu SaaS VN
  Hình 1.2: Sơ đồ kiến trúc tổng quan KiteHub
  Bảng 1.1: So sánh competitor matrix
  Hình 1.3: AI techniques trong edu use case
  Bảng 1.2: VN edu law requirements

Chapter 2:
  Hình 2.1: Multi-tenant architecture        ← restart từ 2.1, KHÔNG tiếp 1.6
  Hình 2.2: Service catalog overview
  Bảng 2.1: Service responsibilities matrix  ← Bảng cũng restart
  Hình 2.3: Sequence JWT authentication flow

Chapter 3:
  Hình 3.1: Implementation timeline
  Listing 3.1: Spring Boot config example   ← Listing cũng restart per chapter

Chapter 4:
  Hình 4.1: Deployment topology AWS EC2
  Hình 4.2: CloudWatch dashboard production
  Bảng 4.1: Performance metrics post-deploy
```

## 3. Hybrid figure + table + listing — tách taxonomy

Numbering CHO MỖI taxonomy độc lập (Hình ≠ Bảng ≠ Listing):

| Order trong chapter | Element | Numbering |
|---|---|---|
| 1st visual | Mermaid diagram | `Hình 2.1` |
| 2nd | comparison table | `Bảng 2.1` (KHÔNG `Hình 2.2`) |
| 3rd | sequence diagram | `Hình 2.2` |
| 4th | code listing >20 lines | `Listing 2.1` |
| 5th | parameter table | `Bảng 2.2` |
| 6th | screenshot | `Hình 2.3` |

→ Reader scan TOC thấy 3 list riêng: "Danh mục hình ảnh" (Hình 2.1, 2.2, 2.3) + "Danh mục bảng" (Bảng 2.1, 2.2) + "Danh mục code listing" (Listing 2.1).

## 4. Cross-chapter citation

OK reference figure from earlier chapter:

```markdown
Như đã trình bày trong Hình 2.3, kiến trúc multi-tenant của KiteHub sử dụng
gateway routing theo subdomain. Trong Chapter 4 sẽ chi tiết hoá deployment topology.
```

Citation phrase chuẩn dùng full prefix `Hình 2.3` / `Bảng 2.3` — KHÔNG short form `H.2.3`.

## 5. Integrity rules

- **Không skip number** — nếu Hình 2.3 đã có thì Hình 2.4 phải có trước Hình 2.5
- **Không duplicate** — không có 2 Hình 2.3 trong cùng chapter
- **Sequential trong document order** — figure xuất hiện đầu chapter = `N.1`, figure cuối = `N.M`
- **Restart từ 1** mỗi khi sang chapter mới — KHÔNG dùng continuous numbering

## 6. Audit script verification

`scripts/audit-figures.sh` check 4 dimensions cho mỗi chapter:

1. **Count** — tổng số figure / table / listing trong chapter
2. **Caption coverage** — % figure có caption đầy đủ (per `caption-format-vietnamese.md`)
3. **Numbering integrity** — không skip / không duplicate / restart đúng per chapter
4. **Citation in body** — heuristic check mỗi `Hình N.M` có corresponding citation trong ±3 đoạn

Output format JSON:

```json
{
  "chapter": 2,
  "figures": {
    "count": 5,
    "items": [
      {"id": "2.1", "type": "mermaid", "caption_present": true, "cited_in_body": true},
      {"id": "2.2", "type": "mermaid", "caption_present": true, "cited_in_body": true},
      {"id": "2.3", "type": "image", "caption_present": false, "cited_in_body": true},
      {"id": "2.4", "type": "mermaid", "caption_present": true, "cited_in_body": false},
      {"id": "2.5", "type": "mermaid", "caption_present": true, "cited_in_body": true}
    ],
    "caption_coverage_pct": 80,
    "citation_coverage_pct": 80,
    "numbering_gaps": []
  },
  "tables": {
    "count": 2,
    "items": [
      {"id": "2.1", "caption_present": true},
      {"id": "2.2", "caption_present": true}
    ]
  }
}
```

## 7. Numbering edge cases

| Case | Resolution |
|---|---|
| Chapter có 0 figure | OK — không có `Hình N.*` trong INDEX |
| Figure trong appendix (Phụ lục A) | Đánh số `Hình A.1, A.2, ...` (chapter letter thay vì number) |
| Figure trong introduction (lời mở đầu, không chapter number) | Hiếm — nếu cần thì `Hình 0.1, 0.2` (chapter 0) HOẶC inline không đánh số |
| Sub-figure (vd `2.3a`, `2.3b`) | Avoid — split thành Hình 2.3 + Hình 2.4 riêng |
| Figure shared across 2 chapters | Pick 1 chapter primary, cite từ chapter khác bằng full reference |
