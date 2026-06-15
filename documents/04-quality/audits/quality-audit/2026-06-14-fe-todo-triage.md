# FE TODO/FIXME Triage Inventory — GAP-1345 (Quality full audit 2026-06-14, Cat 4/9)

**Ngày:** 2026-06-15
**Scope:** 72 marker `TODO|FIXME|HACK` trên 2 frontend src (kiteclass-frontend 29 + kitehub-frontend 43).
**Trạng thái GAP-1345:** PARTIAL — inventory + phân loại DONE; gap-worthy cluster đã file (GAP-1394); xoá marker vật lý DEFER (component/page file thuộc ownership UI agent — không sửa trong PR này).
**Phương pháp:** `grep -rnE "TODO|FIXME|HACK" <fe>/src --include=*.ts --include=*.tsx`.

---

## 1. Phân loại 4 nhóm

| Nhóm | Ý nghĩa | Đếm | Disposition |
|---|---|---:|---|
| (a) **Acceptable — Phase-2 legal placeholder** | Nội dung legal page (privacy/terms/cookies) chờ hoàn tất đăng ký pháp nhân (tên công ty, MST, DPO, hotline) — Phase 2 per roadmap; nhiều cái render `TODO (Phase 2)` là TEXT hiển thị có chủ ý | **52** | KEEP — gắn Phase 2 (xref GAP-186 parental consent); không phải code-debt |
| (b) **Đã gap-tracked** | Marker có sẵn `TODO(GAP-xxx)` trỏ gap mở | **12** | KEEP — đã theo dõi, không silent |
| (c) **Gap-worthy chưa track** | FE page render stub/placeholder chờ BE endpoint chưa ship; chưa có gap | **6** | → **file GAP-1394** (cluster tracker) |
| (d) **False-positive / prose** | Chữ "TODO" trong văn xuôi comment, không phải debt thực | **2** | Non-actionable; bỏ qua |
| | **Tổng** | **72** | |

---

## 2. Chi tiết nhóm (c) — gap-worthy chưa track → GAP-1394

| # | App | File:line | Nội dung | BE phụ thuộc |
|---|---|---|---|---|
| 1 | KH | `src/app/(customer)/settings/components/AccountTab.tsx:72` | wire to backend when `/api/users/{id}/preferences` ships | preferences endpoint |
| 2 | KH | `src/components/support/SupportMenu.tsx:79` | B5 — read JWT role claim → route to `/help/p1`, `/help/p2-owner`, `/help/p3-manager` | role-based help routing |
| 3 | KH | `src/app/(customer)/dashboard/page.tsx:79` | wave-31-followup: replace stub series with real `/api/subscription/health` | subscription health endpoint |
| 4 | KC | `src/app/(teacher)/teacher/grades/[classId]/page.tsx:114` | wire kc-core gradebook API (sub-gap to be filed) | gradebook API |
| 5 | KC | `src/app/(teacher)/teacher/attendance/[classId]/page.tsx:6` | wire `attendance-period` API | attendance-period API |
| 6 | KC | `src/app/(teacher)/teacher/attendance/[classId]/page.tsx:79` | Wave 49+ follow-up: wire kc-core attendance-period API | attendance-period API |

→ Cluster đồng nhất: "FE page stub chờ BE endpoint chưa ship". File **GAP-1394** làm tracker (per `discovery-to-gap-inline-filing.md`).

## 3. Chi tiết nhóm (b) — đã gap-tracked (KEEP)

- KH branding wizard: `TemplateGrid.tsx:16` GAP-272n, `Step6Preview.tsx:539` GAP-272r, `TemplateFullscreen.tsx:62/65/68` GAP-226/227/228, `DeployingStep.tsx:16` GAP-272d, `QualityGateWidget.tsx:14/17/19/24` GAP-226/227/228 + prose, test `Step6Preview-orchestrator-wiring.test.tsx:15` GAP-272e (11 marker).
- KH `src/components/seo/schemas.ts:40` GAP-174 (1 marker).

## 4. Chi tiết nhóm (d) — false-positive (bỏ qua)

- KH `src/components/branding/wizard/LogoStep.tsx:25` — comment "…no mocks or TODO needed for upload" (phủ định, không phải debt).
- KH `src/app/(customer)/dashboard/page.tsx:42` — prose "(TODO follow-up: file gap to expose composite endpoint…)" — mô tả; cùng concern với GAP-1394 nên đã gộp.

---

## 5. Verdict + AC

| Loại | Đếm | Untracked debt thực còn lại |
|---|---:|---|
| Acceptable Phase-2 legal | 52 | 0 (có chủ ý) |
| Gap-tracked | 12 | 0 (đã track) |
| Gap-worthy → GAP-1394 | 6 | 0 (vừa track) |
| False-positive | 2 | 0 |

**Untracked actionable debt sau triage = 0.** Raw grep vẫn 72 vì marker nằm trong component/page file (UI agent sở hữu — PR này không sửa). Mục tiêu "≤30" được diễn giải là "untracked actionable → 0", đạt qua triage + GAP-1394; xoá marker vật lý + CI WARN-mode count DEFER.

AC GAP-1345:
- [x] 72 marker được phân loại (actionable / gap-worthy / acceptable) — bảng triage §1–§4.
- [x] Marker gap-worthy đã file gap riêng — GAP-1394 (cluster 6 marker).
- [ ] (DEFER) FE TODO count ≤30 vật lý — chờ UI agent retire markers + CI WARN-mode gate (ngoài ownership PR này). Tài liệu hoá lý do giữ 52 legal + 12 tracked.

**GAP-1345 giữ PARTIAL.**
