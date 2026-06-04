# Figure Selection Criteria — when to use figure vs table vs prose

## 1. Decision matrix

| Khi nào dùng | Format | Lý do |
|---|---|---|
| **Visual relationship**, architecture, data flow, sequence | **Figure** (Mermaid / PlantUML / PNG) | Đọc cấu trúc bằng mắt nhanh hơn đọc text |
| **Structured comparison** ≥3 cột × ≥3 hàng (vd: matrix tính năng vs vendor) | **Table** | Eye scan column header + row label |
| **Parameter list** (config keys, env vars) | **Table** | Reader cần locate value nhanh |
| **Decision matrix** (vd Phase 1 vs Phase 2 vs Phase 3) | **Table** | Decision pivots cần đặt cạnh nhau |
| **State machine** / FSM | **Figure** (Mermaid `stateDiagram-v2`) | State + transition rendering rõ ràng |
| **Sequence interaction over time** | **Figure** (Mermaid `sequenceDiagram`) | Order matters — visual time axis |
| **ER diagram** | **Figure** (Mermaid `erDiagram`) | Crow's foot notation chuẩn |
| **C4 model** (Context / Container / Component) | **Figure** (PlantUML C4-PlantUML) | C4 quality cần PlantUML library |
| **Architecture box + arrow** | **Figure** (Mermaid `flowchart TB/LR`) | GitHub native render |
| **Screenshot UI / dashboard** | **Figure** (PNG) | Visual fidelity của UI |
| **Code snippet ≤20 dòng** | **Inline code block** (NOT figure, không cần caption) | Reader copy-paste; không cần đánh số |
| **Code snippet >20 dòng** | **Listing** với caption `Listing N.M:` (treat as figure trong INDEX) | Long code cần locate by number |
| **1-2 paragraph giải thích đủ** | **Prose** | Đừng vẽ hình khi text 30 chữ đủ |
| **Quick inline reference** ≤5 node | **ASCII / Unicode box** | Inline reference grep-able |

## 2. Diagram format selection (per `.claude/rules/diagram-format-selection.md`)

Default = **Mermaid** vì GitHub native render + diff-friendly.

| Diagram type | Recommended | Khi nào exception |
|---|---|---|
| Flowchart / architecture | Mermaid `flowchart TD/LR` | C4 quality → PlantUML |
| Sequence | Mermaid `sequenceDiagram` | Complex group/loop/alt → PlantUML |
| State | Mermaid `stateDiagram-v2` | — |
| ER | Mermaid `erDiagram` | — |
| Class | Mermaid `classDiagram` | — |
| Gantt | Mermaid `gantt` | — |
| C4 | **PlantUML** C4-PlantUML | Mermaid `C4Context` limited |
| CI/CD pipeline | PlantUML | — |
| Quick inline ≤5 node | ASCII | Single use case for ASCII |

**Anti-pattern:** ASCII box-drawing >5 nodes (30+ lines monospace text art). GitHub render = text → reader phải decode. Dùng Mermaid thay vào.

## 3. Quality bar (mỗi figure phải có)

- [ ] **Caption** đầy đủ (per `caption-format-vietnamese.md` template)
- [ ] **Numbering** đúng scheme (per `numbering-scheme.md`) — `<chapter>.<sequential>`
- [ ] **Cited trong body text** trong vòng ±3 đoạn (vd: "như trình bày trong Hình 2.3, ...")
- [ ] **Resolution** ≥ 1440×900 desktop hoặc 375×812 mobile (cho PNG screenshot)
- [ ] **Vietnamese UI locale** vi-VN cho screenshot
- [ ] **VN-friendly sample data** (per `vn-localization-audit-checklist.md` §3) — `Trần Thị Hồng`, `Trung tâm <fictional name>`, KHÔNG `John Doe`
- [ ] **No sensitive data leak** (real tenant names, PII, secrets)
- [ ] **File size** < 500KB cho PNG optimized (ImageMagick `mogrify -resize 1440x900 -quality 85`)
- [ ] **Annotation style consistent** khi cần highlight: mũi tên đỏ `#dc2626`, viền vàng `#facc15`, numbered steps (per `ui-review` skill convention)

## 4. Citation window heuristic

Mỗi figure cần được reference trong body text trong khoảng ±3 đoạn văn từ vị trí figure:

```
[paragraph -3] ... preceding context ...
[paragraph -2] ... lead-up text ...
[paragraph -1] "Hình 2.3 minh hoạ kiến trúc đa tenant của hệ thống."  ← citation here
[FIGURE 2.3 Mermaid block]
[CAPTION: **Hình 2.3: Kiến trúc đa tenant**]
[paragraph +1] "Như Hình 2.3 cho thấy, gateway routes ..."  ← OR citation here
[paragraph +2] ... follow-up analysis ...
```

Citation phrases gợi ý:
- `như trình bày trong Hình N.M`
- `Hình N.M minh hoạ`
- `xem Hình N.M`
- `(xem Hình N.M)`
- `Hình N.M cho thấy`
- `Bảng N.M liệt kê`

Cross-chapter citation OK (vd Chapter 4 cite "Hình 2.3 đã trình bày kiến trúc...") nhưng nên minimize.

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Vẽ figure khi 2 đoạn prose đủ | Prose first; figure khi reader cần visual scan |
| ASCII box >5 node | Mermaid flowchart |
| Caption above figure | Caption below figure (italic, font 11pt) |
| Bỏ qua đánh số ("Hình kiến trúc") | Đánh số strict `Hình N.M` |
| Đánh số liên tục cross-chapter (Hình 1, 2, 3, ... 50) | Restart per chapter (Hình 1.1, 1.2, ..., 2.1, 2.2, ...) |
| Figure không cite trong body | Mỗi figure phải có ít nhất 1 citation trong ±3 đoạn |
| Screenshot blurry low-res | ≥1440×900 desktop |
| Sample data `John Doe / Test Center` | `Trần Thị Hồng / Trung tâm <fictional name>` |
| Mix Hình + Bảng cùng đánh số (Hình 2.3 = Bảng 2.3) | Tách taxonomy: Hình independent từ Bảng |
| Diagram quá phức tạp (>20 nodes) trong 1 figure | Split thành multiple figures với caption chỉ rõ scope từng cái |
