---
title: Thảo luận vector DB cho project knowledge base — note để bàn lại sau
date: 2026-05-23
status: deferred-for-discussion
audience: dev
tags: [meta, infrastructure, knowledge-base, vector-db, embedding, rag]
---

# Vector DB cho project knowledge base — bàn lại sau

## Bối cảnh

User đặt câu hỏi 2026-05-23 (post Wave thesis-1 closure): "Project có nhiều code + docs. Đưa hết vào 1 vector DB để Claude dùng tối ưu workflow — nghĩ sao?"

Thảo luận khởi xướng nhưng **chưa chốt** — note này archive để bàn lại khi:
- Wave thesis-2 ship (cohort feedback aggregation surface dùng)
- Team mở rộng (thêm co-dev)
- ROI thực tế của hệ thống hiện tại (CSV + path-scoped + Glob/Grep) suy giảm
- Có session dài chỉ dành cho meta-infrastructure (~1-2 ngày)

## Tóm lược thảo luận

### Phía Claude dè dặt ban đầu

Hệ thống truy vấn project đã được tinh chỉnh tốt:
- `query-gaps.sh` / `query-rules.sh` / `query-adrs.sh` / `query-audits.sh` CSV-canonical (~50× rẻ hơn grep)
- Path-scoped rules auto-load (Wave 73 tiết kiệm ~75% base context: 347k → 88k)
- Glob/Grep dedicated tools cho code
- Skill progressive disclosure (3 lớp metadata / body / reference)

Vector DB → lợi ích biên thấp; chi phí hạ tầng cao; nguy cơ lạc hậu re-index churn; phá vỡ CSV-canonical convention.

### User push back → 7 lập luận ngược (steelman)

1. **Quy mô đã hợp lý hóa** — ~2200+ artifact (466 gap + 257 audit + 56 rule + 100+ wave plan + ~50 skill + ~120 chapter MD + code base ~50k+ LOC). Mainstream IDE (Cursor / Sourcegraph Cody / GitHub Copilot Workspace) tích hợp vector cho project nhỏ hơn nhiều.

2. **Truy vấn cross-domain đang tăng** — Wave thesis-1 Bucket B quét 27 hình × 4 chapter thủ công; Bucket C dựng 20 Q&A đọc toàn audit. Vector query "tìm mọi chỗ thảo luận PDPL audit log retention" → 1 lượt thay vì 3-5 vòng grep biến thể keyword. Field `description` của Anthropic skill dùng matching = embedding-based → khái niệm đã chứng minh trong project.

3. **CSV + Vector bổ sung, không thay thế** — CSV chính danh cho structural query (status/priority/phase); vector cho semantic query (find pattern / similar gap / cover scenario). Hai lớp song song.

4. **Ngân sách context per-session có thể giảm tiếp** — Wave 73 đã đẩy 347k → 88k. Vector tiếp → chỉ load khung tối thiểu + retrieve theo nhu cầu → ~30-40k baseline (giảm thêm 50% trên nền Wave 73).

5. **Phòng tránh nộp gap trùng lặp** — Hiện `audit-to-gap-pipeline.md` §2 grep keyword bỏ sót biến thể. Vector retrieve "gap tương tự" trên CSV `title_short` + `notes` → surface trùng theo ngữ nghĩa thực. Wave 99C có mẫu lặp GAP-259 ≈ GAP-581 nộp cách 5 tuần.

6. **Chi phí khởi tạo cho co-dev / agent** — Agent (Wave Obs 4-5 song song) chưa hấp thụ 56 rule + 466 gap + 257 audit. Vector = giao diện "hỏi project" → agent tự retrieve context liên quan → ngân sách context per-agent giảm.

7. **Chuẩn bị Q&A defense + bằng chứng khóa luận** — Bucket C quét thủ công tốn ~4-5h. Wave thesis-2 cần tổng hợp NFR + beta data + tự sự Ch.5-7 cite — vector tăng tốc tìm nguồn bằng chứng 2-3x.

8. **Mẫu tăng trưởng memory entry** — `MEMORY.md` cap 200 dòng. Số file feedback (~12+ và tăng). Vector trên `~/.claude/projects/.../memory/**/*.md` → surface "feedback liên quan task hiện tại" tự động.

### Phản biện lại (mức kỹ hơn)

- **Chi phí hạ tầng thực:** chroma/qdrant/lancedb local hosting + embedding service (OpenAI/Cohere/local sentence-transformer) — setup tốn 4-6h + xáo trộn re-index mỗi wave
- **Bẫy lạc hậu:** rule update v1 → v2 mà embedding chưa re-index → vector retrieve trả về v1 cũ. Cần CI hook re-embed on docs change
- **Chất lượng retrieval bị thiên lệch bởi chiến lược chunk:** chunk by paragraph vs by section vs by file — chunk size sai = retrieval sai. Cần lặp tinh chỉnh

### Kết luận tái cân bằng

Quan điểm dè dặt ban đầu đánh giá thấp các yếu tố quy mô + cross-domain growth + chi phí khởi tạo. ROI dương đặc biệt nếu Wave thesis-2 ship hoặc team mở rộng.

## 3 phương án để chọn sau

| Phương án | Phạm vi | Thời gian | Khi nào pick |
|---|---|---|---|
| **A — POC phạm vi hẹp** | Chỉ `documents/04-quality/audits/**` (257 file) | 2-3 ngày | Muốn đo ROI thực trước khi commit full; an toàn nhất |
| **B — Quy mô đầy đủ** | Toàn bộ docs + code + memory | 1-2 tuần | Đã có data ROI dương từ POC; hoặc team mở rộng buộc onboarding nhanh |
| **C — Bỏ ý tưởng** | — | 0 | Wave thesis-2 không ship; vẫn solo-dev; CSV + path-scoped đủ |

## Tech stack candidates (nếu pursue)

| Layer | Option | Tradeoff |
|---|---|---|
| Vector DB | chromadb (local SQLite) / qdrant (Docker) / lancedb (file-based) | chromadb đơn giản nhất cho POC; qdrant scale production tốt hơn |
| Embedding | OpenAI `text-embedding-3-small` / Cohere embed-v3 / local `sentence-transformers/all-MiniLM-L6-v2` | OpenAI API có chi phí; local sentence-transformer free + offline + đủ cho doc tiếng Việt nếu chọn model multilingual |
| Re-index trigger | Pre-commit hook / CI workflow / cron nightly | Pre-commit reliable nhất; CI workflow giảm dev local overhead; cron nightly đơn giản nhưng có staleness gap |
| Chunk strategy | By paragraph / by markdown heading / fixed-token-window | Heading-based cho docs structured; paragraph-based cho narrative; cần lặp tinh chỉnh |
| Query interface | MCP server / CLI script / skill | MCP server natural cho Claude (như github MCP); skill simpler nhưng require manual invoke |

## Triggers để mở lại thảo luận

- [ ] Wave thesis-2 ship hoàn tất (cohort feedback tổng hợp surface use case rõ)
- [ ] Team mở rộng (thêm co-dev hoặc nâng số agent song song >5)
- [ ] User-flagged miss tương tự "duplicate gap filed" với grep keyword bỏ sót (Wave 99C precedent)
- [ ] ROI vòng grep manual >3 vòng/tuần trên cross-domain query
- [ ] Anthropic / Claude Code phát hành native vector retrieval feature (eliminate self-host need)

## Cross-references

- `.claude/rules/context-budget-mandate.md` v1.0.1 (Wave 73 context optimization foundation)
- `.claude/rules/mcp-first-with-fallback.md` v1.1.1 (MCP-first nếu chọn MCP server interface)
- `.claude/rules/meta-csv-index-pattern.md` v1.0.2 (CSV-canonical pattern hiện tại — vector sẽ bổ sung không thay thế)
- `.claude/rules/audit-to-gap-pipeline.md` §2 Duplicate Check (vector improvement candidate)
- Wave 73 plan `documents/03-planning/waves/wave-2026-05-14-73-meta-context-optimization.md`

## Note

File này **archive deferred** — không phải gap chính danh. Khi mở lại thảo luận, có thể:
- File gap META P1 cụ thể nếu chốt phương án A hoặc B → wave plan dedicated
- Update file này với "Decision logged: <option> on <date>" + reference gap link
- Hoặc xóa nếu chốt phương án C (bỏ ý tưởng)
