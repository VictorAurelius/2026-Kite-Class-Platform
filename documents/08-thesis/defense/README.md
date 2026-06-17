# 08-thesis/defense — Tài liệu bảo vệ khóa luận

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Folder chứa tài liệu chuẩn bị defense (bảo vệ) khóa luận tốt nghiệp KiteHub Platform. Thời gian bảo vệ dự kiến: 15/08/2026 → 15/10/2026 (theo `thesis-info.md`). Đối tượng: GVHD + GVPB + hội đồng bảo vệ + sinh viên đang chuẩn bị.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `defense-deck.html` | Bộ slide Reveal.js 40 slide tiếng Việt + sơ đồ Mermaid + ghi chú thuyết trình | 1 |
| `defense-qa-response-sheet.md` | 20 câu Q&A chuẩn bị theo 4 archetype người chấm | 1 |
| `defense-demo-script.md` | demo trực tiếp 15 phút — 6 phase + dự phòng mỗi bước | 1 |
| `multi-tenant-demo-script.md` | demo phụ 5 phút chứng minh multi-tenant isolation (UI + API + DB layer); theo GAP-652 Bucket F | 1 |
| `practice-schedule.md` | 2 buổi dry-run (T-3 + T-2 tuần) + danh sách kiểm tra trước bảo vệ + kế hoạch dự phòng | 1 |
| `screenshots/` | Bằng chứng dự phòng quay sẵn (placeholder Wave thesis-1; hoãn quay Wave thesis-2) | folder |

**Bản ghi dự phòng (gitignored):**
- `backup-demo.mp4` — quay demo dự phòng 15 phút (chỉ lưu local + Google Drive, không commit vào repo do > 100MB)

---

## File Placement Rules

- ✅ **Belongs here:** mọi artifact phục vụ trực tiếp buổi bảo vệ — bộ slide, chuẩn bị Q&A, kịch bản demo (chính + phụ multi-tenant), lịch luyện tập
- ❌ **Does NOT belong here:**
  - Báo cáo chính + tệp nguồn chương → `documents/08-thesis/chapter-*.md`
  - Báo cáo audit → `documents/04-quality/audits/`
  - Wave plan + gap file → `documents/03-planning/` + `documents/04-quality/gaps/`
- Naming: `defense-{topic}.{ext}` cho tài liệu chính; tên mô tả cho kịch bản chuyên biệt (vd `multi-tenant-demo-script.md`)

---

## Quy trình bảo vệ

Tổng thời gian phiên bảo vệ tiêu chuẩn UTC ≈ 40-60 phút.

1. **Bộ slide** (~30 phút) — `defense-deck.html` 40 slide đi qua bối cảnh, kỹ thuật AI, kiến trúc, triển khai, KPI, lộ trình tenant thực tế, hạn chế thừa nhận, bài học
2. **Demo trực tiếp chính** (~10-15 phút) — `defense-demo-script.md` 6 phase: khách ẩn danh → quản trị onboarding → wizard tạo tenant → chứng minh multi-tenant → audit → kết thúc
3. **Demo multi-tenant phụ** (~5 phút) — `multi-tenant-demo-script.md` chứng minh data isolation thực tế (Bucket F GAP-652)
4. **Q&A** (~15-20 phút) — áp dụng `defense-qa-response-sheet.md` 20 câu × 4 archetype (Architecture / NFR-DB-DevOps / Business-Compliance / Process-Methodology)

---

## Cách sử dụng

### Khi chuẩn bị bảo vệ:

1. Đọc `practice-schedule.md` để biết lộ trình 2 buổi dry-run T-3 + T-2 tuần
2. Mở `defense-deck.html` trong browser local — Reveal.js render với sơ đồ Mermaid
3. Luyện 20 câu Q&A trong `defense-qa-response-sheet.md` cho đến khi tự tin tất cả 4 archetype
4. Bám sát `defense-demo-script.md` 6 phase × 15 phút khi luyện demo trực tiếp
5. Kết hợp `multi-tenant-demo-script.md` demo phụ nếu hội đồng quan tâm sâu

### Khi bảo vệ thực tế:

1. Mở `defense-deck.html` toàn màn hình Reveal.js
2. Theo quy trình 6 phase trong `defense-demo-script.md`
3. Khi Q&A, áp dụng mẫu trong `defense-qa-response-sheet.md` §Quy trình ứng phó

### Khi xuất PDF từ bộ slide:

```bash
# Cách 1: decktape (khuyến nghị)
npm install -g decktape
decktape reveal documents/08-thesis/defense/defense-deck.html defense-deck.pdf

# Cách 2: chrome headless
google-chrome --headless --disable-gpu --print-to-pdf=defense-deck.pdf \
  --no-pdf-header-footer "file://$(pwd)/documents/08-thesis/defense/defense-deck.html?print-pdf"
```

---

## Archive Policy

Move to `documents/07-archived/thesis-defense-{year}/` khi:
- Bảo vệ đã đạt — sau 1 tháng kể từ ngày bảo vệ thành công
- Bảo vệ trượt và tag stable release mới — file cần làm lại từ đầu
- Tài liệu > 180 ngày tuổi VÀ không được tham chiếu từ wave plan đang hoạt động

Bản ghi dự phòng (`backup-demo.mp4`) lưu Google Drive + local USB sau bảo vệ — không commit vào repo do kích thước file.

---

## Key Documents

- [defense-deck.html](defense-deck.html) — 40 slide Reveal.js + 4 sơ đồ Mermaid + ghi chú thuyết trình (Bucket C)
- [defense-qa-response-sheet.md](defense-qa-response-sheet.md) — 20 câu Q&A × 4 archetype (5 câu/archetype) (Bucket C)
- [defense-demo-script.md](defense-demo-script.md) — 6 phase × 15 phút demo chính (Bucket C)
- [multi-tenant-demo-script.md](multi-tenant-demo-script.md) — 5 phase × 5 phút demo phụ chứng minh multi-tenant (Bucket F GAP-652)
- [practice-schedule.md](practice-schedule.md) — 2 buổi dry-run + danh sách kiểm tra trước bảo vệ + kế hoạch dự phòng (Bucket C)

## Related

- `documents/08-thesis/thesis-info.md` — metadata chuẩn sinh viên + đề tài + GVHD
- `documents/08-thesis/chapter-2-system-architecture.md` — phần multi-tenant
- `documents/08-thesis/chapter-4-deployment-results.md` — triển khai + bằng chứng tenant
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-652-thesis-multi-tenant-isolation-demo.md` — đóng Bucket F
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-653-thesis-defense-prep-deck.md` — đóng Bucket C
- `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md` — nguồn 20 câu Q&A
- `scripts/seed-thesis-demo-tenants.sh` — seed 2 demo tenant trước demo
