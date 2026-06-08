# Session Handoff 2026-06-08 — Zalo OA setup + GAP-819/820 flow maps

**Ngày:** 2026-06-08
**Next-session focus:** Build GAP-819 (ZNS push code-now phần) HOẶC GAP-820 (Zalo Group link) trên main sạch + production env wiring OA ID.

---

## 1. Session này đã ship (merged main)

| PR | Nội dung |
|---|---|
| #2269 | SePay webhook hardening (GAP-1058/1061/1062/1063 DONE; 975/976 PARTIAL 90%; GAP-1064 OPEN — H2 IT RLS boot fail) |
| #2271 | GAP-1065 DONE — Zalo OA env-var consistency: LandingShellSSR + waitlist đọc env var (was hardcode `zalo.me/kitehub`) + `.env.example` entry |
| #2272 | Zalo OA brand assets (`assets/zalo-oa/`: avatar 500×500 + cover 1280×720 16:9 + SVG nguồn) + runbook fix (3-loại OA Zalo 2026, bỏ "Cá nhân", Option A skip-verification, gỡ Vercel) |

## 2. Zalo OA — trạng thái thực

- **OA Doanh nghiệp tạo rồi:** OA ID `1851148412966286224`, **status "Đang chờ duyệt"** (skip verification → review cơ bản, không cần giấy phép). Check oa.zalo.me sau vài giờ; nếu kẹt >24-48h → tạo loại "Nội dung" fallback.
- **OA ID đã set:** `kitehub/kitehub-frontend/.env.local` (gitignored) `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID=1851148412966286224`.
- **Zalo 2026 KHÔNG có loại "Cá nhân"** — 3 loại: Doanh nghiệp / Nội dung / Cơ quan. Solo dev dùng Doanh nghiệp (chính sách bao gồm cá nhân xây thương hiệu). Runbook đã sửa.
- **Passive CTA (GAP-660) đủ cho Phase 1 BETA** — chỉ cần OA duyệt + deep-link. Code đã sẵn (4 site đọc env var).

## 3. TODO còn lại (next session)

### 3.1 Production env wiring OA ID (per local-fix-production-parity-check.md) — 3 chỗ
Dockerfile chỉ pass 2 ARG; thiếu `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID`:
1. `kitehub/kitehub-frontend/Dockerfile`: thêm `ARG` + `ENV NEXT_PUBLIC_KITEHUB_ZALO_OA_ID`
2. `kitehub/docker-compose.kitehub.yml` build.args: `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID`
3. `.github/workflows/docker-build-push.yml`: pass `--build-arg`
(Chưa gấp: OA chờ duyệt + AWS stopped.)

### 3.2 GAP-820 — Zalo Group link (Phase 1.5, flow-check DONE)
**Flow đầy đủ, không blocker.** Build extend-on-existing:
- Migration **V95** (mẫu V68 reschedule-audit): `zalo_group_link` VARCHAR(500) + `zalo_group_qr_code_url` + `zalo_group_updated_at` TIMESTAMPTZ vào `classes`
- Entity `Class.java` (mẫu reschedule cols line 226+) + `ClassMapper` `@Mapping` (triad drift §3.12!) + `ClassResponse` thêm field
- Endpoint: extend `ClassController` (đã có PATCH); authz = `@PreAuthorize("@authz.hasAccessToClass(#classId)")` (KHÔNG có literal 'OWNER'/'TEACHER'); regex `@Pattern ^https://zalo\.me/g/[a-zA-Z0-9]+$`; audit via `AuditLogWriter.record()` (MANDATORY txn, old+new vào payload)
- FE: extend `class-form.tsx` (1 field) + Student card `student/my-classes/page.tsx` CTA + `push-notification-card.tsx` inline
- Docs: extend `documents/01-business/kiteclass/course-class/` (KHÔNG tạo dir `class/`)
- Per `feature-ship-runtime-walk-mandate` + `pre-walk-persona-simulation-mandate`: spawn Opus pre-walk sim trước RST walk

### 3.3 GAP-819 — ZNS push (PARTIAL 10%, flow-check + architecture reconcile DONE this session)
**Architecture chốt 2026-06-08** (`zalo-integration-design.md` §3.0/§3.3): build **kiteclass-core outbox-drain** (KHÔNG kitehub-email NotificationChannel); **platform-level single OA** Phase 1.5 (per-tenant defer Phase 2).
- **Code-now (~70%, mock):** `ZaloZnsClient` (mock|live) + `ZaloOutboxDispatcher` (@Scheduled drain `zalo_oa_notification_outbox` V61) + fix `resolveTenantId()` nil-UUID bug + ALTER `chk_zalo_oa_event_type` (+GRADE_PUBLISHED) + grade hook + wire invite/attendance callers (hiện 0 caller) + `zalo-zns-templates.yml` scaffold + IT mock
- **Blocked (cần Zalo App của user):** live OAuth token + ZNS template_id thật (Zalo duyệt) + end-to-end verify
- **User parallel TODO:** tạo OA Test RIÊNG + Zalo App (developers.zalo.me) → gửi App ID + Secret Key
- Reuse `zalo.*` config (kitehub-email application.yml pattern) + fetch-secrets.sh `kitehub/production/zalo-oa-credentials` (đã scaffold, mock mode)

## 4. Sync state

- ✅ gap-status.csv (1058/1061/1062/1063/1065 DONE; 819 PARTIAL 10; 1064 OPEN)
- ✅ Gap files trong closed/ (1058/1061/1062/1063/1065)
- ⏳ This handoff PR (docs-only): GAP-819 PARTIAL flip + design-doc §3 reconcile + this file
- Local stack: 14 container (chưa rebuild với env var mới — rebuild khi OA duyệt). AWS stopped.
