# 08-thesis/defense — Thesis Defense Materials

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Folder chứa tài liệu chuẩn bị defense (bảo vệ) khóa luận tốt nghiệp KiteHub Platform. Defense window dự kiến: 15/08/2026 → 15/10/2026 (per `thesis-info.md`). Audience: GVHD + GVPB + Defense committee + sinh viên prepping.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `defense-deck.html` | Reveal.js slide deck 40 slide tiếng Việt + Mermaid diagrams + speaker notes | 1 |
| `defense-qa-response-sheet.md` | 20 câu Q&A chuẩn bị theo 4 archetype người chấm | 1 |
| `defense-demo-script.md` | 15-phút live demo walkthrough — 6 phase + fallback per step | 1 |
| `multi-tenant-demo-script.md` | 5-phút secondary demo chứng minh multi-tenant isolation (UI + API + DB layer); per GAP-652 Bucket F | 1 |
| `practice-schedule.md` | 2 buổi dry-run (T-3 + T-2 tuần) + pre-defense checklist + contingency plans | 1 |
| `screenshots/` | Pre-recorded backup evidence (placeholder Wave thesis-1; capture defer Wave thesis-2) | folder |

**Backup recording (gitignored):**
- `backup-demo.mp4` — quay 15-phút demo backup (chỉ lưu local + Google Drive, không commit vào repo do > 100MB)

---

## File Placement Rules

- ✅ **Belongs here:** mọi artifact phục vụ trực tiếp buổi bảo vệ — slide deck, Q&A prep, demo scripts (primary + secondary multi-tenant), practice schedule
- ❌ **Does NOT belong here:**
  - Báo cáo chính + chapter source files → `documents/08-thesis/chapter-*.md`
  - Audit reports → `documents/04-quality/audits/`
  - Wave plans + gap files → `documents/03-planning/` + `documents/04-quality/gaps/`
- Naming: `defense-{topic}.{ext}` cho primary materials; descriptive name cho specialized scripts (vd `multi-tenant-demo-script.md`)

---

## Defense flow

Tổng thời gian phiên bảo vệ tiêu chuẩn UTC ≈ 40-60 phút.

1. **Slide deck** (~30 phút) — `defense-deck.html` 40 slide đi qua bối cảnh, AI techniques, architecture, deployment, KPI, lộ trình tenant thực tế, hạn chế thừa nhận, bài học
2. **Primary live demo** (~10-15 phút) — `defense-demo-script.md` 6 phase: anonymous prospect → admin onboarding → tenant wizard → multi-tenant proof → audit → wrap
3. **Secondary multi-tenant demo** (~5 phút) — `multi-tenant-demo-script.md` chứng minh data isolation thực tế (Bucket F GAP-652)
4. **Q&A** (~15-20 phút) — áp dụng `defense-qa-response-sheet.md` 20 câu × 4 archetype (Architecture / NFR-DB-DevOps / Business-Compliance / Process-Methodology)

---

## How to use

### Khi chuẩn bị defense:

1. Đọc `practice-schedule.md` để biết lộ trình 2 buổi dry-run T-3 + T-2 tuần
2. Open `defense-deck.html` trong browser local — Reveal.js render với Mermaid diagrams
3. Drill 20 câu Q&A trong `defense-qa-response-sheet.md` cho đến khi confident tất cả 4 archetype
4. Bám sát `defense-demo-script.md` 6 phase × 15 phút khi practice live demo
5. Kết hợp `multi-tenant-demo-script.md` secondary demo nếu hội đồng quan tâm sâu

### Khi bảo vệ thực tế:

1. Mở `defense-deck.html` fullscreen Reveal.js
2. Theo flow 6 phase trong `defense-demo-script.md`
3. Khi Q&A, áp dụng template trong `defense-qa-response-sheet.md` §Quy trình ứng phó

### Khi export PDF từ slide deck:

```bash
# Method 1: decktape (recommended)
npm install -g decktape
decktape reveal documents/08-thesis/defense/defense-deck.html defense-deck.pdf

# Method 2: chrome headless
google-chrome --headless --disable-gpu --print-to-pdf=defense-deck.pdf \
  --no-pdf-header-footer "file://$(pwd)/documents/08-thesis/defense/defense-deck.html?print-pdf"
```

---

## Archive Policy

Move to `documents/07-archived/thesis-defense-{year}/` khi:
- Defense đã pass — sau 1 tháng kể từ ngày defense thành công
- Defense fail và tag stable release mới — files cần redo từ đầu
- Doc > 180 ngày old AND không reference từ wave plan active

Backup recording (`backup-demo.mp4`) lưu Google Drive + local USB sau defense — không commit vào repo do file size.

---

## Key Documents

- [defense-deck.html](defense-deck.html) — 40 slide Reveal.js + 4 Mermaid diagrams + speaker notes (Bucket C)
- [defense-qa-response-sheet.md](defense-qa-response-sheet.md) — 20 câu Q&A × 4 archetype (5 câu/archetype) (Bucket C)
- [defense-demo-script.md](defense-demo-script.md) — 6 phase × 15 phút primary demo walkthrough (Bucket C)
- [multi-tenant-demo-script.md](multi-tenant-demo-script.md) — 5 phase × 5 phút secondary demo multi-tenant proof (Bucket F GAP-652)
- [practice-schedule.md](practice-schedule.md) — 2 buổi dry-run + pre-defense checklist + contingency plans (Bucket C)

## Related

- `documents/08-thesis/thesis-info.md` — canonical metadata sinh viên + đề tài + GVHD
- `documents/08-thesis/chapter-2-system-architecture.md` — multi-tenant section
- `documents/08-thesis/chapter-4-deployment-results.md` — deployment + tenant evidence
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-652-thesis-multi-tenant-isolation-demo.md` — Bucket F closure
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-653-thesis-defense-prep-deck.md` — Bucket C closure
- `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md` — nguồn 20 câu Q&A
- `scripts/seed-thesis-demo-tenants.sh` — seed 2 demo tenants pre-demo
