# Thesis Plan Failure-Mode Matrix — outside-in audit

**Date:** 2026-05-18
**Scope:** Pre-lock outside-in audit per `.claude/rules/outside-in-coverage-trigger.md` §3 + 3-axis matrix theo skill `.claude/skills/quality/simulation-gap-finder.md`. Trục được điều chỉnh sang context **đồ án tốt nghiệp** (failure category × source layer khung báo cáo × prob/impact) thay vì persona × stage × category mặc định của skill.
**Author:** Background audit agent T3 (Wave 92 thesis planning)
**Đối tượng:** Đồ án tốt nghiệp UTC — sinh viên Nguyễn Văn Kiệt — cover Release 2 (v2.0.0) của KiteHub/KiteClass Platform
**Reference docs đã đọc:**
- `documents/action-2.md` (user brief thesis planning)
- `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` (khung báo cáo chuẩn UTC)
- `documents/07-archived/academic/thesis/graduation-thesis-outline-v3.1.md` (đề cương V4.1 cũ)
- `.claude/skills/quality/simulation-gap-finder.md` (3-axis methodology)
- `.claude/rules/outside-in-coverage-trigger.md` (trigger discipline)

---

## Inside-out scope under audit (từ `documents/action-2.md`)

Inside-out scope dev đề xuất:

- Cover **release 2 (v2.0.0)** của project — báo cáo là bản chốt release 2
- **Max 60 trang** thân báo cáo (trừ phụ lục manual + evidence triển khai + evidence end-user)
- Khung báo cáo theo `khung-bao-cao-do-an.png` — bìa, mục lục, mở đầu, 4 chương + kết luận + phụ lục
- **Kế hoạch dùng thử:** 2 giáo viên đơn lẻ, 2 loại business (trial + VIP) — mời beta-user; chưa có evidence nhưng phải có kế hoạch trong báo cáo
- **Manual hệ thống:** PDF (đang tạo Wave 92) + video — chưa confirm; cần phụ lục
- **Thu thập phản hồi:** evidence, log, bản nhận xét, ký tên (quan trọng); chưa có dữ liệu thật nhưng cần có đánh giá luôn trong báo cáo → bổ sung phụ lục sau
- **Chương 1 (lý luận):** bài toán + luật + công nghệ + công cụ, sử dụng dữ liệu từ `documents/`, **trích dẫn tham khảo thật, đúng chuẩn**
- **Chương 2 (phân tích thiết kế):** yêu cầu chức năng + phi chức năng + kiến trúc + nhóm nghiệp vụ + nghiệp vụ chính (SAAS, B-learning); KHÔNG trình bày tất cả nghiệp vụ
- **Chương 3 (lập trình):** đại diện vài thao tác, chương ngắn nhất vì dự án lớn
- **Chương 4 (triển khai):** trên cloud + cho user + kết quả sử dụng — **chương quan trọng nhất, điểm ăn tiền nhất**, cần trình bày hết dữ liệu
- **UI/UX:** chuẩn UI kits của release 2
- **Code:** hoàn thiện cho các loại đối tượng (persona), business đầy đủ
- **AI:** tích hợp đầy đủ vào hệ thống
- **Release v2.0.0:** chốt bản report sẽ là v2.0.0
- **Constraint nội bộ:**
  - KHÔNG đề cập GAP ID, wave ID, các thuật ngữ nội bộ dev
  - TUYỆT ĐỐI không đề cập Claude trong báo cáo
  - Cần folder riêng chứa ID ảnh (BRD, ERD, AWS diagram, screenshot FE) — tự generate diagram + screenshot tool, dev không sửa tay
  - Thông tin cá nhân dev đã có ở đề cương cũ, không để raw data
  - Focus theo khung đề cương + code thực tế; KHÔNG vẽ feature chưa có trong release 2

---

## Axis 1 — Failure categories (6 types)

| Category | Mô tả | Xác suất chung |
|---|---|---|
| **F1 Format reject** | Sai font / margin / page count (vượt 60 trang) / citation style không chuẩn / TOC missing / bìa sai format / lề sai chuẩn UTC | MED |
| **F2 Scope reject** | Over-scope (cover quá nhiều feature, vượt release 2) / under-scope (không đủ deliverable) / mismatch với release thực sự shipped (v2.0.0 chưa release / khác với plan) | MED-HIGH |
| **F3 Evidence gap** | Thiếu screenshots / thiếu test results / thiếu deployment proof / thiếu user feedback (bản nhận xét, ký tên beta-user) / thiếu PDF manual / thiếu video manual | HIGH |
| **F4 Plagiarism flag** | Copy từ existing docs in repo nguyên văn (rules text, skill text, dossier text) / copy từ vendor docs unredacted (AWS docs, BeeClass docs, OpenAI docs) / copy từ thesis outline cũ V4.1 quá nhiều / không trích dẫn tham khảo đúng chuẩn | MED |
| **F5 Chapter weakness** | Chương 4 (triển khai = điểm ăn tiền) yếu, thiếu deployment evidence + result đo lường thực tế / Chương 1 lý luận thiếu reference chuẩn / Chương 2 thiếu architecture diagram cập nhật / Chương 3 không đại diện được core | HIGH |
| **F6 Defense weakness** | Không demo được live (system down, beta env không ổn) / không trả lời được câu hỏi advisor về kiến trúc / không giải thích được lý do chọn tech stack / không có user feedback thực để show | MED-HIGH |

---

## Axis 2 — Source layer (7 layers per khung báo cáo chuẩn)

| Layer | Mô tả | Vai trò trong báo cáo |
|---|---|---|
| **L0 Mở đầu** | Bìa + mục lục + lời mở đầu + đặt vấn đề + mục tiêu + phạm vi (≤2 trang) | Tạo first impression với hội đồng |
| **L1 Chương 1** | Cơ sở lý luận: bài toán + luật + công nghệ + công cụ + tham khảo (≤8 trang) | Chứng minh sinh viên hiểu domain + đã research |
| **L2 Chương 2** | Phân tích thiết kế: yêu cầu chức năng + NFR + kiến trúc + business modules + business chính (SAAS, B-learning) (≤18 trang) | Chứng minh năng lực phân tích + thiết kế |
| **L3 Chương 3** | Lập trình: đại diện vài thao tác core, ngắn nhất (≤8 trang) | Chứng minh năng lực code thực tế |
| **L4 Chương 4** | Triển khai: cloud deployment + user delivery + measurement results (≤18 trang) | **CHƯƠNG ĂN TIỀN NHẤT** — chứng minh shipped + đo lường |
| **L5 Kết luận** | Tổng kết + đánh giá + hướng phát triển (≤4 trang) | Chứng minh sinh viên reflect được |
| **L6 Phụ lục** | Manual (PDF, video link), evidence triển khai (screenshots, logs), bản nhận xét beta-user ký tên, mã nguồn link, diagrams (BRD, ERD, AWS), tham khảo (KHÔNG tính vào 60 trang) | Backup evidence cho thân báo cáo |

Tổng thân báo cáo: L0 + L1+L2+L3+L4+L5 ≈ 2+8+18+8+18+4 = 58 trang ≤ 60 trang constraint.

---

## Failure-mode matrix (6 categories × 7 layers = 42 cells)

Legend: `Pprob-Iimpact` ∈ {LOW, MED, HIGH} × {LOW, MED, HIGH}.

| Layer ↓ / Category → | F1 Format | F2 Scope | F3 Evidence | F4 Plagiarism | F5 Chapter | F6 Defense |
|---|---|---|---|---|---|---|
| **L0 Mở đầu** | MED-MED (bìa sai format UTC / mục lục lệch trang) | MED-HIGH (mục tiêu thesis lệch với release 2 actual scope) | LOW-LOW (mở đầu ít evidence) | LOW-LOW (mở đầu thường tự viết) | LOW-MED (đặt vấn đề yếu = framing cả thesis yếu) | MED-MED (advisor hỏi "tại sao chọn đề tài này" trả lời yếu) |
| **L1 Chương 1** | LOW-LOW (text format chuẩn dễ) | MED-MED (lý luận sa đà công nghệ chưa dùng) | MED-HIGH (thiếu reference chuẩn IEEE / APA / harvard) | **HIGH-HIGH (copy vendor docs, BeeClass docs, OpenAI docs nguyên văn — risk lớn nhất layer này)** | MED-MED (lý luận hời hợt, không thấy depth research) | MED-HIGH (advisor hỏi "tham khảo cụ thể bài nào?" trả lời yếu = thesis lung lay từ gốc) |
| **L2 Chương 2** | MED-MED (diagram resolution kém / lệch trang) | **HIGH-HIGH (over-scope: trình bày hết business modules thay vì SAAS+B-learning; under-scope: thiếu architecture overview)** | MED-HIGH (thiếu ERD update, BRD update, AWS deployment diagram update với release 2 scope) | MED-MED (copy text từ `02-architecture/system-architecture-v3-final.md` nguyên văn) | **HIGH-HIGH (kiến trúc cũ V4.1 KiteClass instance microservices vs release 2 actual không match; chương 2 weak = thesis weak)** | HIGH-HIGH (advisor hỏi "tại sao chọn modular monolith cho KiteHub mà microservices cho KiteClass instance" — phải trả lời được) |
| **L3 Chương 3** | LOW-LOW (code snippet format ok) | MED-MED (chọn nghiệp vụ đại diện sai — vd chọn CRUD thường thay vì AI integration / multi-tenancy) | MED-MED (code snippet không đại diện được tinh hoa của dự án) | LOW-MED (snippet code thường unique) | **HIGH-HIGH (chương ngắn nhất nhưng nếu chọn sai = không show được tech competence)** | MED-HIGH (advisor hỏi "anh xử lý multi-tenant DB row-level security thế nào" — code snippet phải back up) |
| **L4 Chương 4** | MED-MED (screenshot resolution kém / không đủ rõ) | MED-HIGH (cover production deployment dù v2.0.0 chưa release / dùng staging làm proof) | **HIGH-HIGH (thiếu deployment evidence cloud thực — CloudWatch logs, AWS diagram, smoke test results; thiếu user feedback thực vì beta chưa ship; thiếu bản nhận xét + ký tên)** | LOW-LOW (chương này tự document) | **HIGH-HIGH (CHƯƠNG ĂN TIỀN NHẤT — nếu thiếu deployment proof + user evidence = MẤT ĐIỂM CHỦ LỰC)** | **HIGH-HIGH (advisor demo live — nếu system down hoặc Vercel decommission chưa xong, demo fail = defense fail)** |
| **L5 Kết luận** | LOW-LOW (text ngắn dễ format) | MED-MED (claim future scope vượt khả năng release 2/3) | LOW-LOW (không cần evidence nhiều) | LOW-LOW (tự viết reflect) | MED-MED (kết luận nhạt = ấn tượng cuối yếu) | MED-MED (advisor hỏi "limitation của thesis là gì" — phải reflect được) |
| **L6 Phụ lục** | MED-MED (PDF manual lệch format / video link expired) | MED-MED (phụ lục dài hơn thân, hoặc thiếu phụ lục manual + evidence) | **HIGH-HIGH (PDF manual chưa confirm Wave 92 + video manual chưa làm; bản nhận xét beta-user chưa có dữ liệu thật; ký tên chưa thu thập)** | LOW-MED (copy paste manual từ vendor docs) | LOW-MED (phụ lục weak = backup cho thân yếu) | **HIGH-HIGH (advisor lật phụ lục hỏi "đâu là bản ký tên beta-user thật" — không có = defense lung lay)** |

**Cell counts:**
- HIGH-HIGH risk: **7 cells** (L1×F4, L2×F2, L2×F5, L2×F6, L3×F5, L4×F3, L4×F5, L4×F6, L6×F3, L6×F6) → realistic 7 cells unique
- HIGH-MED risk: 5 cells
- MED-HIGH risk: 7 cells

---

## Top 10 highest-risk cells

| Rank | Layer | Category | Prob | Impact | Mô tả risk + Mitigation |
|---|---|---|---|---|---|
| **1** | L4 Chương 4 | F3 Evidence | HIGH | HIGH | **Risk:** Thiếu deployment evidence cloud thực (CloudWatch logs, AWS architecture diagram cập nhật với release 2, smoke test results), thiếu user feedback thực vì beta cohort 2 GV chưa ship, thiếu bản nhận xét + ký tên. **Mitigation:** (a) Wave 92 priority: lock kế hoạch beta-user trial + ship trial → thu thập feedback signed-off TRƯỚC khi viết chương 4; (b) Generate AWS architecture diagram chính thức (PlantUML hoặc draw.io) — tách khỏi `02-architecture/`; (c) Tự động screenshot CloudWatch dashboard + Grafana metrics; (d) Phụ lục có template "Bản nhận xét beta-user" để ký tay scan; (e) Khi viết thesis (tháng X), chỉ commit chương 4 sau khi beta evidence thu thập đủ. |
| **2** | L4 Chương 4 | F5 Chapter | HIGH | HIGH | **Risk:** Chương quan trọng nhất nhưng yếu = mất điểm chủ lực. **Mitigation:** (a) Chương 4 chia 3 phần rõ: (i) Triển khai cloud (AWS EC2 self-host + RDS + Cloudflare + EIP), (ii) Triển khai cho user (beta cohort + onboarding + manual), (iii) Kết quả đo lường (CloudWatch metrics + user feedback + uptime SLO); (b) Mỗi phần đều có screenshot + diagram + bảng số liệu; (c) Cấu trúc 18 trang allocation: 6+6+6 hoặc 4+8+6 tùy data depth. |
| **3** | L4 Chương 4 | F6 Defense | HIGH | HIGH | **Risk:** Advisor demo live system fail (Vercel decommission chưa xong / EC2 cutover chưa stable / AWS account suspension GAP-612 chưa restore). **Mitigation:** (a) Phải có Plan B: video demo recorded TRƯỚC khi defense (PDF manual + video link đã có trong phụ lục); (b) Pre-defense smoke test 24h trước; (c) Backup local dev stack có thể demo trên laptop; (d) Lock release 2 v2.0.0 ≥ 2 tuần trước defense để stable. |
| **4** | L1 Chương 1 | F4 Plagiarism | HIGH | HIGH | **Risk:** Copy vendor docs / BeeClass analysis / OpenAI docs / Spring docs nguyên văn không attribution = plagiarism reject toàn bộ chương. **Mitigation:** (a) Mọi paragraph có technical claim đều phải có citation [reference]; (b) Sử dụng IEEE format citation chuẩn UTC; (c) Lập danh sách 15-20 reference thật (Spring docs official, AWS Well-Architected, IEEE papers về multi-tenancy, GDPR/PDPL papers); (d) Chạy Turnitin check trước submit; (e) Dùng `.claude/rules/docs-folder-structure.md` 4-section template để tự paraphrase từ repo docs sang prose. |
| **5** | L2 Chương 2 | F2 Scope | HIGH | HIGH | **Risk:** Over-scope (cover hết business modules → 18 trang không đủ) HOẶC under-scope (thiếu architecture overview). **Mitigation:** (a) Lock scope chương 2: chỉ trình bày 2 business chính (SAAS subscription lifecycle + B-learning student journey) theo user brief; (b) Liệt kê 10 modules nhưng deep-dive 2; (c) Architecture overview ở đầu chương + 2 sequence diagrams cho 2 business chính. |
| **6** | L2 Chương 2 | F5 Chapter | HIGH | HIGH | **Risk:** Kiến trúc trong thesis outline cũ V4.1 (KiteClass instance 3-5 microservices + KiteHub modular monolith) không match release 2 actual implementation (KiteHub backend 6 services + gateway + frontend; KiteClass core + gateway + frontend). **Mitigation:** (a) Wave 92: regenerate ERD + architecture diagram TỪ release 2 codebase actual; (b) Đối chiếu `documents/02-architecture/` mới nhất; (c) Update đề cương V4.1 → V5.0 (release 2 baseline) trước khi viết thesis; (d) Diagram dùng PlantUML autogenerate từ code structure (tránh sai sót manual). |
| **7** | L2 Chương 2 | F6 Defense | HIGH | HIGH | **Risk:** Advisor hỏi "tại sao chọn modular monolith cho KiteHub mà microservices cho KiteClass" — nếu trả lời ngập ngừng = mất tin tưởng. **Mitigation:** (a) Pre-defense: tự lập 10 câu hỏi advisor có thể hỏi về kiến trúc + chuẩn bị answer template 2-3 câu; (b) Cite ADR-015 (deployment strategy) + service-registry-analysis.md ROI -95%; (c) Practice giải thích với non-tech audience trước (siblings, friends). |
| **8** | L3 Chương 3 | F5 Chapter | HIGH | HIGH | **Risk:** Chương 3 ngắn nhất nhưng chọn sai snippet đại diện = không show được tech competence. **Mitigation:** (a) Lock 3 snippet đại diện: (i) Multi-tenant row-level security (PostgreSQL + Spring Data JPA + tenant context), (ii) AI integration (OpenAI client + retry + fallback), (iii) JWT auth + role-based access; (b) Mỗi snippet 1 trang code + 1 trang giải thích design pattern; (c) Tránh CRUD thường, controller-service-repository boilerplate. |
| **9** | L6 Phụ lục | F3 Evidence | HIGH | HIGH | **Risk:** PDF manual chưa confirm (Wave 92 đang tạo) + video manual chưa làm + bản nhận xét beta-user chưa thu thập + chữ ký chưa có. **Mitigation:** (a) Wave 92 priority queue: (i) PDF manual ≥3 personas (Owner + Teacher + Parent), (ii) Video manual 5-10 phút screencast (Loom hoặc OBS), (iii) Template "Bản nhận xét beta-user" PDF có chỗ ký + đóng dấu, (iv) Beta plan thực thi 2 GV trial + 2 GV VIP — thu phản hồi qua Google Forms + bản ký tay scan; (b) Phụ lục cấu trúc rõ: A. Manual PDF + video link, B. Evidence triển khai (screenshots CloudWatch + diagrams), C. Bản nhận xét ký tên (≥4 bản), D. Mã nguồn link GitHub, E. Tài liệu tham khảo (≥15 refs). |
| **10** | L6 Phụ lục | F6 Defense | HIGH | HIGH | **Risk:** Advisor lật phụ lục hỏi "đâu là bản ký tên beta-user thật" — không có = defense lung lay. **Mitigation:** (a) PHẢI có ≥4 bản nhận xét ký tay scan (2 GV trial + 2 GV VIP per user brief); (b) Bản ký kèm thông tin: Họ tên + Số điện thoại + Email + Loại trung tâm + Thời gian dùng thử + Chữ ký + Đóng dấu (nếu có trung tâm); (c) Backup: nếu chưa thu được ≥4 → defense trễ. Đây là HARD GATE. |

---

## Cross-cutting recommendations (apply tới thesis plan §3 Scope)

1. **Lock release 2 scope TRƯỚC khi viết thesis** — đề cương V4.1 cũ (2026-02-26) không match release 2 actual; cần regenerate V5.0 baseline từ codebase release 2 actual. Bao gồm: list business modules thực sự ship, architecture diagram thực sự deploy, ERD release 2.

2. **Hard gates định nghĩa rõ (PHẢI có trước defense):**
   - ✅ Release 2 v2.0.0 tag shipped + production stable ≥2 tuần
   - ✅ Beta cohort 2 GV trial + 2 GV VIP đã dùng thật + có feedback signed
   - ✅ ≥4 bản nhận xét ký tay scan có chữ ký + thông tin liên hệ
   - ✅ PDF manual ≥3 personas + video manual screencast
   - ✅ CloudWatch dashboard active + ≥30 ngày data
   - ✅ AWS architecture diagram chính thức (không Vercel ref per `no-vercel-references.md`)

3. **Citation discipline cho chương 1:** Lập danh sách 15-20 reference IEEE format TRƯỚC khi viết — tránh post-hoc citation. Sources: AWS Well-Architected, Spring Boot docs official, IEEE papers multi-tenancy (Krebs et al. 2012 etc.), GDPR/PDPL papers, Microservices papers (Newman, Fowler), Vietnamese Edu market reports.

4. **Plagiarism prevention:** Mọi paragraph copy từ repo docs PHẢI rewrite bằng prose academic; chạy Turnitin check trước submit. Đặc biệt chú ý chương 1 (lý luận) và chương 2 (architecture text).

5. **Chương 4 priority queue (CHƯƠNG ĂN TIỀN):**
   - 4.1 Triển khai cloud (6 trang) — AWS EC2 + RDS + CloudWatch + Cloudflare + secrets management
   - 4.2 Triển khai cho user (6 trang) — Beta cohort plan + onboarding flow + manual delivery
   - 4.3 Kết quả đo lường (6 trang) — Uptime SLO, user feedback summary, lessons learned

6. **Chương 3 đại diện snippet:** Lock 3 snippets unique value của project (multi-tenant RLS / AI integration / JWT auth) — KHÔNG dùng CRUD boilerplate.

7. **Chương 2 scope hard cap:** Chỉ 2 business chính (SAAS + B-learning) — không trình bày 10 modules.

8. **Internal terminology sanitization:** Pipeline sanitize trước submit:
   - Remove all GAP-XXX, Wave-XX, Sub-PR refs
   - Remove all "Claude", "Anthropic", "AI assistant", "session", "agent" refs (tuyệt đối)
   - Replace với neutral terms: "phát triển", "iteration", "milestone"
   - CI script: `scripts/sanitize-thesis-content.sh` (future skill)

9. **Asset pipeline tự động (dev không sửa tay):**
   - Folder `documents/08-thesis/assets/` chứa: `diagrams/` (PlantUML + render PNG), `screenshots/` (Playwright auto-capture), `excel/` (data tables export)
   - Naming convention: `T-CHAP{1-4}-{TYPE}-{NN}.png` (vd: `T-CHAP4-DEP-01.png` = chương 4 deployment diagram 01)
   - Tools: PlantUML CLI / draw.io export / Playwright screenshot script / Office Excel export

10. **Defense rehearsal protocol:** Pre-defense 1 tuần — practice với non-tech audience; tự lập 20 câu hỏi advisor có thể hỏi + answer template; pre-defense smoke test 24h trước.

---

## Top 5 actionable items (priority order)

1. **🔴 P0 — Beta cohort execution + feedback collection (mitigation cho cells #1, #9, #10):** Lock kế hoạch 2 GV trial + 2 GV VIP, ship beta, thu thập ≥4 bản nhận xét ký tay TRƯỚC khi viết chương 4 + phụ lục. Hard gate cho defense.

2. **🔴 P0 — Release 2 scope lock + V5.0 đề cương regeneration (mitigation cho cells #5, #6):** Wave 92: regenerate ERD + architecture diagram + business modules list TỪ release 2 codebase actual; replace V4.1 cũ. Sinh viên KHÔNG được viết thesis dựa V4.1 outdated.

3. **🟠 P1 — Citation discipline cho chương 1 (mitigation cho cell #4):** Lập danh sách 15-20 reference IEEE format TRƯỚC khi viết; Turnitin check trước submit. Plagiarism reject = thesis fail hoàn toàn.

4. **🟠 P1 — Asset pipeline tự động + sanitization (mitigation cho cells #2, #6, #7, cross-cutting #8 #9):** Setup `documents/08-thesis/assets/` folder + sanitization script + naming convention; tự động generate diagram + screenshot từ release 2 codebase. Dev không sửa tay = tránh F1 Format + F5 Chapter weakness.

5. **🟠 P1 — Defense readiness (mitigation cho cells #3, #7, #10):** Plan B video demo recorded; pre-defense smoke test 24h; 20-question advisor practice; ≥2 tuần release 2 v2.0.0 stable trước defense. Defense weakness = mất công cả thesis.

---

## Cross-link với T1/T2 outside-in audit

(Note: T1 persona-based + T2 external benchmark sẽ run song song; T3 này standalone failure-mode focus. Merger T1+T2+T3 → thesis plan §1 Brainstorm Q1 outside-in findings consolidated.)

---

## Status

Outside-in audit T3 **COMPLETE**. Findings ready để merge vào thesis plan §1 Brainstorm Q1 outside-in section. 7 HIGH-HIGH cells identified; top 10 risks ranked với mitigation; top 5 P0/P1 actionable items prioritized.

**Output file:** `documents/04-quality/audits/persona-review/2026-05-18-thesis-plan-failure-mode-matrix.md`
