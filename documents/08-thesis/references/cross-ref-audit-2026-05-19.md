---
title: Bibliography Cross-Reference Audit — 2026-05-19 (Wave 100.7 Phase 3a)
audience: mixed
status: complete
created: 2026-05-19
wave: 100.7-phase-3a
agent: 3a
parent_gap: GAP-647
---

# Báo cáo kiểm chéo bibliography ↔ chapter citations

> 🔄 **Round 2 update — Wave 100.7 Phase 4 Foundation 2026-05-19:**
> - **Renumber applied:** `[40]` (was PDPL duplicate) merged vào `[21]` canonical (PDPL Số 49/2023/QH15 + "có hiệu lực 2026-07-01" note appended). `[41]`→`[40]` Phuong+Anh, `[42]`→`[41]` Forsgren, `[43]`→`[42]` Sato et al. Total bibliography refs: **43 → 42**.
> - **Original Phase 3a findings preserved below** — body audit content (orphan list, collision map, per-chapter counts) reflects pre-renumber snapshot. Use this Round 2 banner để map old→new ref numbers khi đọc body audit content. Phase 4 Bucket B (Ch.2 LOCAL migration) + Buckets A/C (orphan retro-cite) sẽ ship Round 3 audit khi closure PR ready.

Kiểm tra **bibliography.md** (43 IEEE refs pre-renumber → 42 post-renumber, chapter-grouped) đối chiếu với các trích dẫn `[N]` thực tế xuất hiện trong các file chương khóa luận. Mục tiêu: phát hiện (1) refs mồ côi (orphan — không chương nào cite), (2) `[N]` trong chương không resolve được sang bibliography (missing-ref — examiner sẽ catch), (3) bất nhất numbering scheme giữa các chương.

## TL;DR (đọc 30 giây)

- **43 refs** trong bibliography, **17 refs unique** được cite ở các chương 1+2 (39%) — phần còn lại đang ngủ (24 orphan refs, 56% bibliography chưa dùng).
- **Chương 2** dùng **local numbering [1]-[8]** riêng — KHÔNG map vào bibliography global. Đây là rủi ro examiner P0: 2 hệ thống số ([1] tại chương 2 = AWS SaaS Lens, [1] tại bibliography = EasyEdu).
- **Chương 3 + Chương 4** chưa có trích dẫn nào (placeholder).
- **Chương 1 (3 parts) trích dẫn nhất quán** theo global numbering.
- **Không có missing-ref** giữa chương 1 và bibliography (tất cả `[N]` chương 1 đều resolve).

---

## 1. Phương pháp

```bash
# Bước 1 — Extract citations từ mỗi chương
for f in documents/08-thesis/chapter-*.md; do
  grep -oE "\[[0-9]+\]" "$f" | sort -u
done

# Bước 2 — So sánh với 43 entries trong bibliography.md
# Bước 3 — Phân nhóm: cited / orphan / missing-ref / cross-numbering-collision
```

Scope: 6 chapter files dưới `documents/08-thesis/`:

1. `chapter-1-competitor-analysis.md` (Wave 100 D, 211 dòng)
2. `chapter-1-ai-techniques.md` (Wave 100 D, 260 dòng)
3. `chapter-1-vn-law-methodology.md` (Wave 100.7 Phase 2-2a)
4. `chapter-2-system-architecture.md` (Wave 100.7 Phase 2-2b, 707 dòng)
5. `chapter-3-implementation.md` (Wave 100.7 Phase 2-2c, 472 dòng)
6. `chapter-4-deployment-results.md` (Wave 100.7 Phase 2-2c, 644 dòng)

Source bibliography: `documents/08-thesis/references/bibliography.md` (43 refs, last-updated 2026-05-19).

---

## 2. Trích dẫn thực tế trong từng chương

| Chương | Refs unique được cite | Số lượng | Có local references section? |
|---|---|---|---|
| `chapter-1-competitor-analysis.md` | [1], [2], [3], [4], [21], [23], [24] | 7 | ❌ Không — dùng bibliography global |
| `chapter-1-ai-techniques.md` | [4], [14], [15], [16], [17], [18], [19], [21], [22] | 9 | ❌ Không — dùng bibliography global |
| `chapter-1-vn-law-methodology.md` | [18], [21], [22], [23], [24], [25], [28], [39] | 8 | ❌ Không — dùng bibliography global |
| `chapter-2-system-architecture.md` | [1], [2], [3], [4], [5], [6], [7], [8] | 8 | ⚠️ **CÓ — LOCAL numbering tách rời bibliography** |
| `chapter-3-implementation.md` | (chưa có) | 0 | ❌ Placeholder, chưa cite |
| `chapter-4-deployment-results.md` | (chưa có) | 0 | ❌ Placeholder, chưa cite |

**Tổng refs unique được cite (deduplicate)** ở chương 1 (3 parts): **{1, 2, 3, 4, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 28, 39}** = **17 refs**.

---

## 3. Refs được cite (resolve OK với bibliography)

Bảng dưới đây chỉ tính scope **chương 1** (3 parts) vì chương 2 dùng numbering local riêng (phân tích §5):

| Ref# | Bibliography entry | Chương cite | Trạng thái |
|---|---|---|---|
| [1] | EasyEdu — Tính năng EasyEdu | Ch1-competitor | ✅ Resolve |
| [2] | MISA EMIS — K-12 hệ thống | Ch1-competitor | ✅ Resolve |
| [3] | Magenest — Top 15 phần mềm quản lý trung tâm | Ch1-competitor | ✅ Resolve |
| [4] | 6Wresearch — VN LMS Market Report | Ch1-competitor + Ch1-ai-techniques | ✅ Resolve (cited 2 chapters) |
| [14] | T. Brown — Few-Shot Learners (GPT-3) | Ch1-ai-techniques | ✅ Resolve |
| [15] | P. Lewis — RAG paper | Ch1-ai-techniques | ✅ Resolve |
| [16] | R. Rombach — Latent Diffusion (Stable Diffusion) | Ch1-ai-techniques | ✅ Resolve |
| [17] | H. Liu — Visual Instruction Tuning (LLaVA) | Ch1-ai-techniques | ✅ Resolve |
| [18] | K. Beck — TDD By Example | Ch1-ai-techniques + Ch1-vn-law | ✅ Resolve (cited 2 chapters) |
| [19] | E. Evans — DDD | Ch1-ai-techniques | ✅ Resolve |
| [21] | Luật BVDLCN (PDPL 2023) Số 49/2023/QH15 | Ch1-competitor + Ch1-ai-techniques + Ch1-vn-law | ✅ Resolve (cited 3 chapters — "đinh" của Chương 1) |
| [22] | Nghị định 13/2023/NĐ-CP | Ch1-ai-techniques + Ch1-vn-law | ✅ Resolve |
| [23] | Luật An ninh mạng 2018 | Ch1-competitor + Ch1-vn-law | ✅ Resolve |
| [24] | Nghị định 53/2022/NĐ-CP | Ch1-competitor + Ch1-vn-law | ✅ Resolve |
| [25] | Thông tư 78/2021/TT-BTC eInvoice | Ch1-vn-law | ✅ Resolve |
| [28] | OWASP Top 10 2021 | Ch1-vn-law | ✅ Resolve |
| [39] | Nghị định 147/2024/NĐ-CP — Giao dịch Điện tử | Ch1-vn-law | ✅ Resolve |

**Tổng: 17/17 in-text `[N]` của chương 1 đều resolve đúng sang bibliography.md. KHÔNG có missing-ref.**

---

## 4. Refs mồ côi (orphan — không chương nào cite)

24 refs trong bibliography KHÔNG được trích dẫn ở chương nào (tính cả chương 2 local numbering — không xét vì collision):

| Ref# | Entry | Chương dự định | Phân loại orphan |
|---|---|---|---|
| [5] | UIT — Khóa luận chia sẻ video microservices | Ch1 (reference VN thesis) | 🟡 Seed reference Wave 97 — chưa được Ch1 trích dẫn (có thể move vào Ch1 trong revision) |
| [6] | UIT — Danh sách khóa luận tốt nghiệp HTTT 2020-2025 | Ch1 | 🟡 Tương tự [5] |
| [7] | S. Newman — *Building Microservices* | Ch2 (theoretical) | 🟡 Phải dùng cho Ch2 microservices background |
| [8] | M. Fowler — *Patterns of Enterprise Application Architecture* | Ch2 | 🟡 Phải dùng cho Ch2 |
| [9] | AWS — SaaS Lens Whitepaper | Ch2 (multi-tenant SaaS) | 🟠 **Chương 2 đã cite "AWS SaaS Lens" tại [1] LOCAL — đây là conflict** |
| [10] | J. Krishnan — Microsoft Multi-Tenant Data Architecture | Ch2 | 🟠 **Tương tự — Ch2 [2] LOCAL trùng nội dung** |
| [11] | VMware Tanzu — Spring Boot Reference v3.5 | Ch2 (Spring Boot section) | 🟡 Phải dùng Ch2/Ch3 |
| [12] | PostgreSQL 16 Documentation | Ch2 (DB section) | 🟡 Phải dùng Ch2/Ch3 |
| [13] | Vercel — Next.js Documentation v15 | Ch2 (Frontend section) | 🟡 Phải dùng Ch2/Ch3 |
| [20] | R. C. Martin — *Clean Architecture* | Ch2 (development methodology) | 🟡 Phải dùng Ch2 |
| [26] | J. Tyree, A. Akerman — Architecture Decisions IEEE Software | Ch4 (ADR methodology) | 🟡 Phải dùng Ch4 deployment |
| [27] | Microsoft — ADR Template | Ch4 | 🟡 Tương tự |
| [29] | M. B. Jones — RFC 7519 JWT | Ch2 (security section) | 🟡 Phải dùng Ch2/Ch3 |
| [30] | VMware Tanzu — Spring Security Reference v6.4 | Ch2 | 🟡 Tương tự |
| [31] | Mona Software — Mona eLMS | Ch1 (competitor analysis) | 🟠 Wave 100 D thêm nhưng Ch1-competitor KHÔNG cite đến |
| [32] | DotB Vietnam — DotB EduSoft | Ch1 | 🟠 Tương tự |
| [33] | Bộ GD&ĐT — Thông tư 29/2024/TT-BGDĐT Dạy thêm | Ch1 | 🟠 Chương 1 đề cập 29/2024 narrative nhưng KHÔNG cite `[33]` |
| [34] | VECITA — Báo cáo Kinh tế Số VN 2024 | Ch1 | 🟠 Tương tự |
| [35] | Anthropic — Claude API Prompt Engineering | Ch1-ai-techniques | 🟠 Wave 100 D thêm nhưng Ch1-ai KHÔNG cite |
| [36] | OpenAI — GPT-4 Technical Report | Ch1-ai-techniques | 🟠 Tương tự |
| [37] | Hugging Face — NSFW Image Classifier Model Card | Ch1-ai-techniques | 🟠 Tương tự — moderation pipeline reference |
| [38] | Replicate — Stable Diffusion XL API | Ch1-ai-techniques | 🟠 Tương tự |
| [40] | Quốc hội VN — PDPL 2023 (variant Số 49/2023/QH15, effective 2026-07-01) | Ch1-vn-law | 🔴 **DUPLICATE [21]** — cùng văn bản, format khác |
| [41] | N. T. Phuong, L. H. Anh — VN PDPL Compliance Frameworks SE Asia | Ch1-vn-law | 🟡 Wave 100.7 thêm nhưng Ch1-vn-law KHÔNG cite |
| [42] | Forsgren, Humble, Kim — *Accelerate* DevOps | Ch4 (deployment metrics DORA) | 🟡 Phải dùng Ch4 |
| [43] | D. Sato et al. — Continuous Delivery ICSE 2020 | Ch4 (CI/CD) | 🟡 Phải dùng Ch4 |

**Phân loại tổng:**

| Trạng thái | Số lượng | % tổng bibliography |
|---|---|---|
| **Resolve OK (được cite)** | 17 | 39% |
| **Orphan — chờ Ch2-4 cite** | 17 | 40% (chap 2-4 chưa hoàn thành) |
| **Orphan — phải fix retroactively chương 1** | 6 | 14% (refs [31]-[37] Wave 100 D thêm nhưng chưa cite) |
| **DUPLICATE entry** | 1 | 2% ([40] ≈ [21] cùng văn bản pháp lý) |
| **Wave 97 seed orphan** | 2 | 5% ([5], [6] UIT thesis refs — Ch1 reference VN thesis section hiện không cite) |

---

## 5. Bất nhất numbering scheme — Chương 2 dùng LOCAL numbering [1]-[8]

**Đây là rủi ro P0 cho thesis defense.** Examiner sẽ phát hiện:

| Vị trí | [1] resolve thành gì |
|---|---|
| Bibliography global [1] | EasyEdu — Tính năng EasyEdu phần mềm quản lý trung tâm |
| Chapter 2 local [1] (line 685) | AWS Architecture Center, "SaaS Lens — AWS Well-Architected Framework" |
| Chapter 2 local [2] | Microsoft Azure — Multi-tenant SaaS database tenancy patterns |
| Chapter 2 local [5] | Quốc hội VN — Luật BVDLCN số **91/2025/QH15** (PDPL 2025?!) |

**Lưu ý đặc biệt vấn đề Chapter 2 [5]:** local section [5] cite "Luật BVDLCN số 91/2025/QH15 (PDPL 2025)" trong khi bibliography global [21] cite "Luật BVDLCN số 49/2023/QH15 (PDPL 2023)". Cùng văn bản gốc nhưng số hiệu KHÔNG khớp — examiner sẽ catch. Cần thống nhất chính xác số hiệu PDPL.

### 5.1 Khuyến nghị giải pháp Chương 2 collision

**Option A (recommend):** Re-map Chapter 2 LOCAL [1]-[8] sang bibliography global. Mapping dự kiến (cần thẩm tra số chính xác PDPL):

| Chapter 2 LOCAL | Bibliography global tương đương | Action |
|---|---|---|
| [1] AWS SaaS Lens | [9] AWS SaaS Lens Whitepaper | Map [1] → [9] |
| [2] Microsoft Multi-tenant patterns | [10] J. Krishnan — Microsoft Multi-Tenant | Map [2] → [10] |
| [3] F. Pothon — Architecting Multi-Tenant SaaS | **THÊM mới [44]** vào bibliography | Append new entry |
| [4] PostgreSQL Row Security Policies | [12] PostgreSQL 16 Documentation (cần thêm URL `/ddl-rowsecurity` cụ thể) | Map [4] → [12] hoặc thêm sub-entry |
| [5] PDPL Số 91/2025/QH15 | [21] hoặc [40] (verify số chính xác) | Map sau khi xác nhận số hiệu pháp lý đúng |
| [6] Luật An ninh mạng 24/2018 | [23] Luật An ninh mạng 2018 | Map [6] → [23] |
| [7] S. Brown — C4 model | **THÊM mới [45]** | Append new entry |
| [8] OWASP Top 10 2021 | [28] OWASP Top 10 2021 | Map [8] → [28] |

**Option B (rủi ro cao, không khuyên):** Giữ local numbering Chapter 2 và viết rõ trong frontmatter mỗi chapter "Local references — local numbering". Đa số tài liệu IEEE thesis KHÔNG dùng cách này → defense risk.

**Option C (hybrid):** Đặt 1 phần "Tài liệu tham khảo" chung cho toàn luận văn ở cuối — bỏ section local trong từng chapter, dùng numbering global. Đây là cách phổ biến nhất cho thesis Việt Nam.

→ **Khuyến nghị Option C** — chuẩn bị Chapter 5-7 + thesis closing có 1 bibliography section thống nhất (đã tồn tại trong `references/bibliography.md`).

---

## 6. Missing-ref `[N]` trong chương không resolve được

**KHÔNG có missing-ref** ở chương 1 (3 parts). Mọi `[N]` chương 1 đều có entry tương ứng trong bibliography.

Tuy nhiên, **chapter 2 [3] (F. Pothon book) + [7] (S. Brown C4 model) chỉ tồn tại trong local references của chapter 2, chưa có trong global bibliography**. Nếu Option A/C áp dụng → 2 entries này phải được append vào bibliography (suggested as [44], [45] hoặc số kế tiếp).

---

## 7. Per-chapter ref count

| Chương | Số ref unique được cite | Density (refs / 100 dòng) |
|---|---|---|
| chapter-1-competitor-analysis.md (211 dòng) | 7 | 3.3 |
| chapter-1-ai-techniques.md (260 dòng) | 9 | 3.5 |
| chapter-1-vn-law-methodology.md (~?) | 8 | TBD |
| chapter-2-system-architecture.md (707 dòng) | 8 (local) | 1.1 (thấp — cần dày hơn cho chapter dài) |
| chapter-3-implementation.md (472 dòng) | 0 | 0 (placeholder) |
| chapter-4-deployment-results.md (644 dòng) | 0 | 0 (placeholder) |

Examiner expectation chuẩn UIT/HUST/UET: **density 3-5 refs / 100 dòng** cho chapter có argument cần evidence. Chương 2 hiện đang thấp (1.1) — cần thêm citations khi finalize.

---

## 8. Summary stats

```
Bibliography size:           43 refs
Unique refs cited (Ch1):     17 (39% of bibliography)
Refs orphan total:           24 (56%)
  - Wave 100 D late-add (chưa cite): 8 ([31]-[38])
  - Wave 100.7 Phase 2 late-add (chưa cite): 4 ([40]-[43])
  - Wave 97 seed orphan (Ch2-4 chờ): 11
  - DUPLICATE (cùng văn bản): 1 ([40] ≈ [21])
  - Wave 97 thesis refs: 1 ([5] UIT chia sẻ video)

Missing-ref [N] in chapters: 0 (Chapter 1 perfect)
Cross-numbering collision:    8 instances (Chapter 2 [1]-[8] local — P0 risk)

Chapters using global bibliography: 3 (Ch1 three parts)
Chapters using local numbering:     1 (Ch2 — needs migration)
Chapters without citations:         2 (Ch3, Ch4 — placeholder)
```

---

## 9. Khuyến nghị hành động (P0 → P3)

### P0 — Trước thesis defense (BẮT BUỘC)

1. **Resolve Chapter 2 local numbering collision** — Option C khuyên: bỏ local "Tài liệu tham khảo" của Chapter 2, dùng global bibliography. Re-cite Chapter 2 `[1]-[8]` → bibliography numbers ([9], [10], [12], [21] hoặc [40], [23], [28], + 2 entries mới).
2. **Thẩm tra số hiệu pháp lý PDPL** — bibliography [21]/[40] đang dùng `49/2023/QH15`; Chapter 2 [5] dùng `91/2025/QH15`. Verify với `thuvienphapluat.vn` và sync về 1 con số.
3. **Append 2 refs mới** ([44] F. Pothon book + [45] S. Brown C4 model) vào bibliography để Chapter 2 [3] và [7] có entry resolve. Đây là điều kiện cần để Option C khả thi.

### P1 — Trước revision V2

4. **Dedupe [40] hoặc clarify role of [40] vs [21]** — hiện 2 entries reference cùng văn bản. Hoặc xóa [40], hoặc chuyển nội dung [40] thành reference riêng (vd: implementation guide / commentary trên PDPL).
5. **Retroactive cite Wave 100 D + Phase 2 refs** — refs [31]-[38] (Wave 100 D thêm cho Ch1) + refs [40]-[43] (Wave 100.7 thêm cho Ch1+Ch4) cần được cite inline trong các chương tương ứng để KHÔNG là orphan.

### P2 — Khi Ch3-4 hoàn thành

6. **Chapter 3 cite implementation refs** — Spring Boot [11], PostgreSQL [12], JWT RFC 7519 [29], Spring Security [30].
7. **Chapter 4 cite deployment refs** — ADR methodology [26]-[27], Accelerate DORA [42], CD ICSE [43].
8. **Chapter 5-7 (Conclusion, Testing, etc.)** chưa scope — defer Wave 101+.

### P3 — Optional polish

9. **Chapter 2 density tăng** — hiện 1.1 refs/100 dòng, target 3-5. Thêm citations cho mỗi mô hình isolation pattern, mỗi NFR claim.
10. **UIT thesis refs [5], [6]** — quyết định giữ hoặc gỡ. Nếu Ch1 không reference VN thesis section thì gỡ; nếu giữ thì viết 1 đoạn ngắn "Reference VN thesis projects" trong Ch1 cite chúng.

---

## 10. Note for future audit cycles

- **Re-run audit này khi:** Chapter 3 hoặc Chapter 4 finalize content (sẽ thêm nhiều refs); khi append entry mới vào bibliography (re-verify orphan list).
- **Tool tiềm năng:** `scripts/check-thesis-citations.sh` (mentioned in CITATION-STYLE.md §Tooling, hiện deferred Phase 2 per `incident-to-rule-pipeline.md` premature-rule guard) — sẽ replace audit thủ công này khi shipped.
- **Citation-extract skill** (GAP-655, defer Wave 98+): khi shipped sẽ auto-format citations từ WebFetch output → giảm risk format inconsistency từ phía source.

---

## Related

- [bibliography.md](./bibliography.md) — Source IEEE bibliography (43 refs)
- [CITATION-STYLE.md](./CITATION-STYLE.md) — IEEE format rules
- [GAP-647](../../04-quality/gaps/phase-1-beta/GAP-647-thesis-bibliography-ieee.md) — Parent gap (PARTIAL 50%, Step 3 deferred GAP-655)
- [GAP-655] — Wave 98+ citation-extract skill (future automation)
- `documents/08-thesis/chapter-mapping.md` — Chapter-to-source map
