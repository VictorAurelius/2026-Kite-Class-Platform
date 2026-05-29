---
title: Session Handoff — Wave thesis-4 (landing data-driven + banner + citation fix) → chờ Claude Design
audience: dev
last-updated: 2026-05-30
status: complete
branch: wave/thesis-3-content-fixes
---

# Session Handoff — 2026-05-30 (Wave thesis-4)

## ⏭️ SESSION SAU: CHỜ OUTPUT CLAUDE DESIGN

User đang setup **Claude Design System** (claude.ai web — KHÔNG có trong CLI) để redesign landing KiteClass cho tất cả persona + **nhiều banner**. Form đã soạn sẵn: `documents/08-thesis/claude-design-setup.md` (6 field + §Tích hợp output).

**Khi user đưa output Claude Design (code/design):** tích hợp vào `kiteclass-frontend/src/components/sections/*` — giữ data-driven (slots+fallback) + theme RGB vars + per-tenant template_type (V77) + build verify (`pnpm --filter kiteclass-frontend build`). Nếu multi-banner → mở rộng `landing_pages` schema (cột `banners JSONB` array) + `compose-teacher-banner.mjs`. Quy trình chi tiết: `claude-design-setup.md` §Tích hợp.

## Trạng thái (94 commits ahead origin, CHƯA push — giữ local per feedback_no_push_without_explicit_ask)

Branch `wave/thesis-3-content-fixes`. Stack local đang chạy (14 container healthy). Dev FE port 4700 (sẽ tắt khi end session — restart: `cd kiteclass/kiteclass-frontend && INTERNAL_API_URL=http://localhost:9000 NEXT_PUBLIC_API_URL=http://localhost:9000 NEXT_PUBLIC_TENANT_ID=126eaa8c-1f63-4c30-81b5-a5921b384b3b pnpm dev`).

### ✅ Shipped session này
1. **Landing data-driven** (V76 + 7 JSONB: aboutText/teachers/programs/pricingTiers/testimonials/faqs/stats) — fix Hibernate `List<Map>`→`JsonNode` (đọc JSONB null), Spring `@Cacheable` Redis stale (evict bằng `redis-cli --scan --pattern landingPages* | xargs DEL`), SSR base URL `INTERNAL_API_URL`.
2. **template_type per-tenant** (V77) — 3 GV → `personal` (không còn section trung tâm). page.tsx đọc `landingData.templateType`.
3. **Template chuyên nghiệp** — full-width gradient hero + CTA cam (`--theme-cta #F97316`) + counter animate + card shadow + trust badge + zebra dividers + testimonial carousel. Per `banner-prompts-and-design-spec.md` (phân tích mshoajunior/anhngumshoa).
4. **Banner 3 GV (ChatGPT)** — `portrait/{khanh,ha,nhi}/` (portrait-goc + banner variants). Deploy: Khánh dùng thẳng `banner-quyet-tam.png` (full text); Hà/Nhì = scene + HTML text overlay qua `scripts/compose-teacher-banner.mjs`. Banner = full-width hero (HeroSection: banner clickable + trust ribbon dưới).
5. **Ch.1 BeeClass fix** — URL beeclass.com→beeclass.net + re-capture + viết lại đúng (gamification điểm thi đua, KHÔNG phải QL trung tâm) + bảng so sánh.
6. **Citation targeted fix** — +[39] BeeClass +[40] Mike Cohn +[41] AWS SLA (ch2 [25]→[41]) −[33] SDXL stale. 0 broken cite, 40 entries (≥30).
7. **Meta** — memory `feedback_thesis_banner_html_compose` thêm "banner 3 lớp + icon chủ đề". GAP-815 (landing content editor UI). `claude-design-setup.md`.

### 🔴 OUTSTANDING (sau Claude Design hoặc song song)
1. **Capture chính thức Ch.3/§4.2** — 5 ảnh demo-trio + §4.2. ⚠️ Stats counter count-up = 0 trong headless fullPage → cần scroll-trigger hoặc disable animation lúc capture.
2. **Reconcile narrative Ch.3** — seed thật: cô Khánh 30 HV/1 lớp (Pháp luật 12A1), Hà 18, Nhì 22. Ch.3 narrative còn ghi "78 HV · 5 khóa" (số Sky cũ) → sửa khớp seed thật.
3. **GAP-815** loose end — warning `updateEntity` unmapped (UpdateLandingPageRequest thiếu 7 field → PUT chưa set sections). Cho landing editor UI.
4. **C3 bibliography full reconciliation** (deferred) — sửa `renumber_citations.py` (bỏ ai-techniques chapter DROPPED, scan script-side Mở đầu/Kết luận, dynamic notes) + cite orphan hợp lệ (Spring Security/GPT-4/methodology) giữ ≥30 trước khi renumber-by-appearance. Header bibliography đã ghi note defer.
5. **RabbitMQ queue IaC** (từ handoff trước) — `class.rescheduled.queue` + `.email.queue` đã thêm RabbitConfig (B5 wave trước).
6. **Cleanup:** 5 worktree `wt-t4-*` (b1-fr/b2-ch3/b3-s42/ld-be/ld-fe/tpl) — `git worktree remove` sau khi chắc đã merge.

## Seed demo (3 GV độc lập — reproducible)
```
GATEWAY_URL=http://localhost:9000 bash kitehub/scripts/seed-demo-independent-teachers.sh
docker exec -i kite-postgres psql -U kitehub -d kiteclass_shared < kitehub/scripts/seed-landing-content.sql
docker exec kite-redis redis-cli --scan --pattern "landingPages*" | xargs -r -I{} docker exec kite-redis redis-cli DEL "{}"
```
instance_id: Khánh `126eaa8c-1f63-4c30-81b5-a5921b384b3b` · Hà `ad0fa96e-af24-49cb-b3e5-19d44f182d85` · Nhì `0abe093c-4c66-4c99-abab-a756582dc60b`

## Rules áp dụng
`fe-build-local-verify` (KHÔNG chạy `pnpm build` khi dev đang chạy → corrupt `.next`) · `agent-model-opus-default` · `release-fix-retry-budget §3.5` (investigation-first — tránh chạy renumber_citations.py mù) · `feedback_thesis_banner_html_compose` · `feedback_no_push_without_explicit_ask` · `always-commit-action-scratchpad`.

## ⚠️ Gotcha học được
- Spring `@Cacheable("landingPages")` Redis sống qua restart → MỌI seed/update landing PHẢI evict cache.
- `pnpm build` (production) ghi đè `.next` của `pnpm dev` đang chạy → CSS/JS text/plain MIME → trang vỡ. Kill dev trước khi build.
- ChatGPT Plus render dấu tiếng Việt OK (memory cũ lo garble không còn đúng cho banner full-text).
