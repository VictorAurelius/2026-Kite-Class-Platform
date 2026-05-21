---
title: Bìa pipeline personal data verify — diff create_thesis_v1.py STUDENT_INFO ↔ student-info.md
status: complete
created: 2026-05-21
phase: Wave 102.7.5 Bucket C
wave: 102.7.5
gaps: [GAP-696]
audience: dev
---

# Bìa pipeline personal data verify — Wave 102.7.5 Bucket C Task C2

## Scope

Verify-only audit (không sửa pipeline) để xác nhận `documents/08-thesis/create_thesis_v1.py` STUDENT_INFO dict (lines 51-63) đọc đúng personal data từ source-of-truth `documents/07-archived/academic/word-reports/student-info.md` (Wave 102.5 baseline). Audit này phục vụ closure protocol Wave 102.7.5 Bucket C — phát hiện sớm drift giữa hard-coded dict vs source-of-truth canonical, đề xuất follow-up nếu cần sync (không tự ý sửa pipeline — phạm vi Bucket B).

## Commands run (Tier 1 read-only — dedicated tools per `mcp-first-with-fallback.md`)

```bash
# Source-of-truth read (Wave 102.5 baseline canonical)
Read documents/07-archived/academic/word-reports/student-info.md lines 7-17

# Pipeline STUDENT_INFO dict read (Wave 102.7.5 current — DO NOT modify)
Read documents/08-thesis/create_thesis_v1.py lines 50-63

# Pipeline THESIS_INFO advisor cross-ref (GVHD personal-data adjacent)
Read documents/08-thesis/create_thesis_v1.py lines 65-78

# Working tree clean verification (no modification this PR)
git status documents/08-thesis/create_thesis_v1.py
# → On branch worktree-agent-a6b10dfcb98cabe5a; nothing to commit
```

## Findings

### Diff table — STUDENT_INFO dict vs student-info.md personal-data section

| # | Field (student-info.md) | Value (source-of-truth) | Key (create_thesis_v1.py) | Value (pipeline) | Match? |
|:-:|---|---|---|---|:-:|
| 1 | Họ và tên | `Nguyễn Văn Kiệt` | `name` | `Nguyễn Văn Kiệt` | ✅ |
| 2 | Mã sinh viên | `221230890` | `student_id` | `221230890` | ✅ |
| 3 | Lớp | `CNTT1-K63` | `class` | `CNTT1-K63` | ✅ |
| 4 | Khóa | `63` | `course` | `63` | ✅ |
| 5 | Chuyên ngành | `Công nghệ thông tin` | `major` | `Công nghệ thông tin` | ✅ |
| 6 | Khoa | `Công nghệ thông tin` | `department` | `Công nghệ thông tin` | ✅ |
| 7 | Hệ đào tạo | `Cử nhân` | `degree` | `Cử nhân` | ✅ |
| 8 | Trường | `Đại học Giao thông Vận tải` | `university` | `Đại học Giao thông Vận tải` | ✅ |

**8/8 fields aligned ✅** — không phát hiện drift giữa pipeline STUDENT_INFO dict và source-of-truth student-info.md personal-data table.

### Pipeline-only extension fields (không có trong student-info.md)

| Key (create_thesis_v1.py) | Value (pipeline) | Nguồn / Lý do |
|---|---|---|
| `specialization` | `Công nghệ phần mềm` | Pipeline thêm để diễn đạt chi tiết hơn cho bìa thesis (Chuyên ngành cấp khoa = CNTT; Chuyên ngành cấp chương trình = CNPM) |
| `training_mode` | `Chính quy` | Pipeline thêm để khớp UTC convention bìa thesis (Hệ đào tạo cử nhân thường có thêm "Chính quy" cho cử nhân chính quy 4 năm) |
| `university_short` | `UTC GTVT` | Pipeline thêm để dùng cho header chân trang / chữ ký gói gọn |

Các field extension này là **enhancement hợp lý** cho bìa thesis (richer than baseline internship-report student-info.md scope). Không phải drift — đây là pipeline mở rộng dữ liệu thay vì sai lệch.

### Advisor (GVHD) cross-reference

| Source | Location | Giá trị |
|---|---|---|
| `student-info.md` line 25 | INTERNSHIP_INFO scope `GVHD tại trường` | `TS. Nguyễn Đức Dư` |
| `create_thesis_v1.py` line 73 | THESIS_INFO `advisor` field | `TS. Nguyễn Đức Dư` |
| `create_thesis_v1.py` line 74 | THESIS_INFO `advisor_dept` | `Khoa Công nghệ thông tin` |

**GVHD aligned ✅** — TS. Nguyễn Đức Dư match giữa internship-report context và thesis context. Khoa Công nghệ thông tin của GVHD nhất quán với Khoa của sinh viên.

### Verdict

**KHÔNG có drift.** STUDENT_INFO dict trong `create_thesis_v1.py` (lines 51-63) phản ánh chính xác 8/8 trường personal data từ source-of-truth `student-info.md`. 3 field extension (`specialization`, `training_mode`, `university_short`) là enrichment phù hợp cho bìa thesis convention UTC, không phải drift.

Pipeline render bìa thesis sẽ hiển thị personal data đúng với canonical baseline. KHÔNG cần follow-up sync work cho Bucket B scope.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — tránh duplicate work)

| Action | When | Where verified |
|---|---|---|
| `student-info.md` Wave 102.5 baseline ship | Trước 2026-05-21 | `documents/07-archived/academic/word-reports/student-info.md` — file tồn tại, table personal-data 8 rows complete |
| `create_thesis_v1.py` STUDENT_INFO dict scaffold | Wave 102.5 era | Lines 50-63; comment line 50 `# THÔNG TIN SINH VIÊN (từ thesis-info.md §4)` chỉ rõ pipeline đọc từ thesis-info reference (chứa cùng dữ liệu student-info baseline) |
| Bucket B Wave 102.7.5 — Phụ lục cleanup + ABC sort | Đang chạy parallel | Touches `create_thesis_v1.py` nhưng KHÔNG động vào STUDENT_INFO dict lines 51-63 (Bucket B scope = Phụ lục function + terms/abbrevs lists) |

## Pending (this audit)

| Action | Owner | Notes |
|---|---|---|
| Audit verdict aligned ✅ | Bucket C (verify-only) | Không cần follow-up sync — pipeline đúng baseline |
| Pipeline edit nếu cần sync drift | Bucket B (out of scope this audit) | KHÔNG triggered vì 0 drift phát hiện |
| Source-of-truth student-info.md update nếu cần | Future wave (out of scope) | KHÔNG triggered |

## Recommendations

1. **No-op** — pipeline STUDENT_INFO dict đã chính xác. Không cần fix.
2. **Future verify cadence** — re-run audit này khi:
   - student-info.md được update (rare — personal data ổn định)
   - create_thesis_v1.py STUDENT_INFO dict được edit (rare — scaffold đã stable)
   - Thesis V2+ ship đòi hỏi thêm field mới (vd MSCB, năm sinh) cần sync 2 source
3. **Consider** — nếu Wave 102.7.6+ refactor pipeline để đọc STUDENT_INFO trực tiếp từ student-info.md (single source-of-truth), file follow-up gap (không scope wave này).

## References

- Source-of-truth: `documents/07-archived/academic/word-reports/student-info.md` (Wave 102.5 baseline)
- Pipeline file: `documents/08-thesis/create_thesis_v1.py` lines 51-63 STUDENT_INFO dict
- Wave plan: `documents/03-planning/waves/wave-2026-05-21-102.7.5-thesis-v1-deferred-cleanup.md` §3 Bucket C Task C2
- Tracking gap: `documents/04-quality/gaps/GAP-696-*` (Wave 102.7.5 thesis V1 deferred cleanup umbrella)
- Rule applied: `audit-to-gap-pipeline.md` §3 template + §2.8 prior-actions verify
- Sister rule: `pre-mutation-state-check.md` §3 — audit pattern (verify-only mode here, không phải pre-mutation)
