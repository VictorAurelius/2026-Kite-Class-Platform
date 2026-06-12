# GAP-1234: Kit wizard v3 thiếu bước nhập thông tin landing — text trên trang không biết nhập ở đâu

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Design System
**Found:** 2026-06-12 (user-flagged: "các thông tin text đang hiển thị trên landing sẽ được nhập ở bước nào?")
**Affects:** `documents/02-architecture/design-system/ui_kits/ai-branding-wizard-v2/v3/`

## Problem

User review landing-personal (hero "Gia sư Toán · 12 năm KN", pain points, bio + bằng cấp, testimonials, FAQ + học phí 800.000đ/900.000đ) và hỏi: các text này nhập ở bước nào của wizard? Investigation (design-first):

1. **Wizard v3 (9 màn cũ) chỉ cover visual identity** — không bước nào thu thập facts (môn, năm KN, học phí, bằng cấp, khác biệt). AI Gemini draft hero/about per ADR-037 nhưng **CẤM bịa số liệu/testimonial** (Luật Quảng cáo VN) → không có nguồn facts thật thì AI không draft đúng được.
2. **GAP-815 (P3, OPEN)** đã file editor `settings/landing` 7 section với hybrid model — nhưng **chưa có màn hình design nào trong ui_kits** (thiếu 基本設計 layer).
3. Nội dung hiện chỉ tạo được qua `seed-landing-content.sql` / `PUT /landing` thô → blank-page friction khi GV onboard (GAP-815 đã ghi nhận).

User chốt (AskUserQuestion 2026-06-12): **"Cả hai"** — wizard thêm bước facts + design editor screens.

## Fix shipped (design layer, cùng PR)

**A. Wizard v3 → 9 bước:** màn mới `step2-info.html` "Thông tin trung tâm" — môn/chương trình (chips), năm KN, sĩ số, bằng cấp, bảng học phí (row-list VND), điểm khác biệt (textarea), integrity note (AI chỉ dùng facts thật; testimonials nhập ở editor). Renumber toàn bộ: `step2-logo→step3-logo`, `step3-mode→step4-mode`, `step4-audience→step5-audience`, `step5-tone→step6-tone`, `step6-portrait→step7a-portrait`; stepper 8→9 vị trí (thêm "Thông tin" tại 2); mọi href/`Bước N / 8`→`/ 9` đồng bộ 11 file.

**B. Editor GAP-815 design source (3 màn):** `editor-overview.html` (7 section card + trạng thái + nguồn nhập AI-draft/manual/auto/prefill), `editor-section-ai-draft.html` (facts panel → AI soạn nháp → GV duyệt + sanitize note GAP-827), `editor-testimonials.html` (manual-only + checkbox xác nhận thật + integrity note "không bao giờ có nút AI" + lộ trình import Zalo/Google).

**Content-entry mapping chốt:** hero/about/pain/FAQ = AI draft từ facts; học phí/bằng cấp/năm KN = GV nhập bước 2; teachers = prefill hồ sơ; stats = auto data thật; testimonials = manual-only.

## Acceptance Criteria

- [x] Wizard v3 có bước nhập facts thật (step2-info) — mọi text user hỏi đều trace được về một bước nhập / nguồn auto
- [x] 3 màn editor GAP-815 design (基本設計) với hybrid integrity boundary đúng GAP-815 §Proposed Fix
- [x] Renumber 9 bước nhất quán (stepper/title/eyebrow/footer/href) — link integrity 100%, residual `/ 8` = 0
- [x] README + v3 index cập nhật mapping content-entry + bảng screen
- [x] Click-through GAP-1233 giữ nguyên: wizard ↔ editor link 2 chiều

## Scope note

Gap này = **design layer** (kit). Implementation production: editor FE = GAP-815 (OPEN, Phase 1.5+); wizard state machine 'info' + AI-chain = cụm GAP-1215. Design source này là 基本設計 cho cả hai.

## Related

- GAP-815 — landing content editor (design source shipped bởi gap này; FE implementation vẫn OPEN)
- GAP-1233 — click-through navigation (PR #2347, nền cho flow walk)
- GAP-1212 — v3 refresh gốc · ADR-037 — no-fabrication constraint · GAP-827 — sanitize-on-write
