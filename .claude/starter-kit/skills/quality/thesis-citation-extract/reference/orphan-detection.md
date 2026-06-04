# Orphan Detection — định nghĩa + decision tree fix

Skill `thesis-citation-extract` phân loại mọi cite/entry thành 3 nhóm. Tài liệu này giải thích semantic + cách fix khi gặp orphan.

## 3 bucket classification

Cho tập hợp:

- `B` = body cites — tất cả `[N]` xuất hiện trong chapter markdown files (sau khi expand range + tách list)
- `R` = bibliography entries — tất cả `[N]` đầu line trong `bibliography.md`

Skill output:

| Bucket | Định nghĩa (set theory) | Ý nghĩa |
|---|---|---|
| ✅ **matched** | `B ∩ R` | Cite trong body + entry trong bib → OK |
| ⚠️ **orphan-body** | `B \ R` (cite trong body NHƯNG không có entry bib) | **Broken reference** — cite chỉ đến entry không tồn tại |
| ⚠️ **orphan-bib** | `R \ B` (entry trong bib NHƯNG không được cite ở body) | **Dead weight** — entry lãng phí, không có claim ground |

## Decision tree fix

### Khi gặp orphan-body (cite không có entry)

```
Orphan-body: cite [N] xuất hiện ở body nhưng bibliography.md không có [N]
│
├─ Trường hợp 1: cite typo (vd định viết [12] viết nhầm [21])
│  → Fix body — sửa số [N] về cite đúng
│
├─ Trường hợp 2: entry bị xóa accident từ bibliography
│  → Fix bib — restore entry [N] từ git history
│
├─ Trường hợp 3: cite reference đến source chưa thêm vào bib
│  → Fix bib — append entry mới [N] với metadata IEEE-formatted
│
└─ Trường hợp 4: cite reference đến chapter khác đang draft chưa merge
   → Document inline trong PR: "[N] sẽ resolve khi <chapter X> merge"
   → Track follow-up nếu blocker
```

### Khi gặp orphan-bib (entry không được cite)

```
Orphan-bib: entry [N] trong bibliography nhưng body chapters không cite [N]
│
├─ Trường hợp 1: claim relevant đã có nhưng forget cite
│  → Fix body — thêm `[N]` trong câu/đoạn relevant trong body
│
├─ Trường hợp 2: entry obsolete (claim đã remove khỏi body trong refactor)
│  → Fix bib — xóa entry [N] + renumber các entry sau (decrement)
│  → Sau renumber, chạy lại verify để confirm cite trong body sync với numbering mới
│
├─ Trường hợp 3: entry dự kiến cho future chapter chưa viết
│  → Trì hoãn: KHÔNG fix; chấp nhận orphan-bib tới khi chapter ship
│  → Document inline trong PR closure: "Entry [N] reserved for <chapter Y>"
│
└─ Trường hợp 4: entry ship sai (vd duplicate)
   → Fix bib — xóa duplicate + renumber
```

## Renumber cascade warning

Khi xóa entry trong bibliography (orphan-bib case 2/4), TẤT CẢ entry sau bị shift number. Body cite cần update đồng bộ. Quy trình:

1. Xóa entry `[N]`
2. Renumber `[N+1] → [N]`, `[N+2] → [N+1]`, ... cho tới cuối bib
3. Sweep body chapters: `[N+1]` → `[N]`, `[N+2]` → `[N+1]`, ... (regex-based hoặc script)
4. Chạy lại `bash scripts/verify-citations.sh` để confirm 0 orphan

## Threshold guidance

- **0 orphan** — production-ready cho thesis defense
- **1-3 orphan-bib** — chấp nhận được nếu entry reserved cho future chapter (document inline)
- **≥1 orphan-body** — BLOCKING — broken cite không được phép trong V1+ ship; PHẢI fix trước defense
- **≥5 orphan-bib không justified** — dead weight cleanup mandatory (giảm bibliography noise + giúp examiner đọc)

## Related

- `documents/<thesis-dir>/references/CITATION-STYLE.md` — IEEE format spec (companion doc)
- `documents/<thesis-dir>/references/bibliography.md` — canonical bibliography
- Cross-ref audit reports trong `documents/<thesis-dir>/references/` (project-specific archive)
