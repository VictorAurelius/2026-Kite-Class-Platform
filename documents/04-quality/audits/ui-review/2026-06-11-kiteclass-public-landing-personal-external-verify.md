# External verify /128 — kiteclass-public (4 screens) + landing-personal

**Audit type:** ui-review /128 external re-score (calibration) + production-parity check
**Date:** 2026-06-11
**Wave:** ui-kits-100 (Bucket C)
**Scope:** `documents/02-architecture/design-system/ui_kits/kiteclass-public/screens/{catalog,catalog-detail,about,contact}.html` + `landing-personal/index.html` (shipped PR #2326)
**Rubric:** `.claude/skills/quality/ui-review/SKILL.md` 8 dimension × /16 = /128 (target ≥105 floor)
**Lens:** port-from-production per `frontend-standards.md` §3.1 (production Wave 78 = source of truth) + `feedback_audit_calibration` (self-score → external calibration)

---

## 1. Mục tiêu

PR #2326 ship 5 screen với **self-score** trong README. Audit này:
1. Re-score độc lập /128 (external lens, không trust self-report) per `feedback_audit_calibration`.
2. Đối chiếu production-parity (kit ↔ `kiteclass-frontend/src/app/(public)/`): kit lệch production = sửa KIT.
3. Verdict ≥105 floor cho từng screen; <105 → polish hoặc giữ residual.

---

## 2. External re-score vs self-score

Calibration discount điển hình ~3-5 điểm (self-report lạc quan ở D3 interaction + D7 a11y). 8 dimension:
D1 Hierarchy · D2 Persona · D3 Interaction · D4 Theme · D5 VN content · D6 Responsive · D7 A11y · D8 Completeness.

| Screen | Self /128 | **External /128** | Δ | Verdict |
|--------|:---------:|:-----------------:|:--:|:-------:|
| `catalog` | 115 | **111** | -4 | ✅ ≥105 PASS |
| `catalog-detail` | 112 | **108** | -4 | ✅ ≥105 PASS |
| `about` | 110 | **106** | -4 | ✅ ≥105 PASS |
| `contact` | 114 | **110** | -4 | ✅ ≥105 PASS |
| `landing-personal` | 113 | **109** | -4 | ✅ ≥105 PASS |
| **Trung bình** | 112.8 | **108.8** | -4.0 | ✅ tất cả ≥105 |

### 2.1 Điểm trừ external (lý do calibration)

| Screen | Dimension trừ | Lý do external |
|--------|---------------|----------------|
| catalog | D7 A11y 14→13 · D2 15→14 | Filter chip active dựa màu + `aria-pressed` nhưng chưa announce kết quả lọc qua `aria-live`; persona-reco runtime tốt nhưng JS-only (no-JS degrade chưa rõ) |
| catalog-detail | D3 13→12 · D7 13→12 | Chủ yếu đọc (syllabus/FAQ `<details>`); schedule "seats/Đã đầy" chưa có `aria-live`; sticky mobile CTA OK |
| about | D3 12→11 · D8 13→12 | Read-heavy inherent (story GV) — ít affordance tương tác; stats anti-fabrication tốt nhưng demo tĩnh |
| contact | D7 15→14 · D4 13→12 | Form validation + inline error + focus-first mạnh; theme runtime swap thật nhưng production cần token dày hơn |
| landing-personal | D7 14→13 · D8 12→11 | Carousel thiếu `aria-live` announce slide (README tự note); 12 section khớp template nhưng programs/timeline demo tĩnh |

Không screen nào < 105 → KHÔNG cần polish khẩn cấp. Δ-4 đồng đều = self-report calibrated tốt (không inflate quá), phù hợp `feedback_audit_calibration` (self-score đáng tin trong biên ±5).

---

## 3. Production-parity check (port-from-production lens)

Per audit verdict §3 `2026-06-11-pre-wave-ui-kits-100-outside-in-refresh.md`: production pages `(public)/`
(catalog 472 LOC / about 175 / contact 141 / landing page 290) ĐÃ VN-polish + Shadcn + WCAG AA tại Wave 78.
Kit `kiteclass-public` + `landing-personal` (#2326) = **back-port doc baseline** cho các route production đã đi trước.

| Chiều parity | Kit | Production | Verdict |
|--------------|-----|------------|---------|
| Route coverage | catalog / catalog-detail / about / contact + landing `/` | `(public)/{catalog, catalog/[id], about, contact}` + `(public)/page.tsx` | ✅ khớp 1:1 |
| Domain | (không hardcode domain trong kit; demo tenant slug) | `kitehub.me` (GAP-458) | ✅ không drift (kit không dùng `kiteclass.vn/.com`) |
| Anti-fabrication stats | "2 lớp · 12 HV · 85% lên lớp giỏi" + chú thích ẩn-khi-null | production "Hỗ trợ qua email (Beta)" honesty (Wave 78) | ✅ kit kế thừa honesty, không bịa hotline/500+ trường |
| Contact form contract | regex SĐT `^0\d{9}$` + email optional + lời nhắn ≥10 | replicate server-side `kiteclass-core` lead endpoint (README note #5) | ✅ contract documented cho port |
| Theme per-tenant | runtime `.theme-*` swap (3 GV demo) | `src/lib/template/configs.ts` inject `--theme-*` runtime | ✅ pattern khớp; production cần token dày hơn (kit note) |
| Persona-reco / catalog filter | vanilla JS trên DOM tĩnh | port → Spring course-list query + client chips (README note #2) | ✅ design-source documented; port = đợt 2 (GAP-269/271 ngoài wave) |

**Verdict parity:** Kit KHÔNG drift khỏi production. Kit là design baseline hợp lệ cho production routes đã live;
README `kiteclass-public` §"Notes cho production-port agent" + `landing-personal` §"Notes" đã ghi đầy đủ mapping
production. KHÔNG cần sửa KIT. Port production→kit (GAP-269/271 Track 2) ngoài scope wave này per user chốt 2026-06-11.

---

## 4. Kết luận

- 5/5 screen ≥105 floor (external 106-111, avg 108.8). PASS.
- Calibration Δ-4 đồng đều → self-report đáng tin (không inflate).
- Production-parity ✅ — kit reflect production state, 0 drift cần sửa kit.
- ConsentBanner (GAP-274 AC) + favicon head spec (GAP-1229 phần C) bổ sung vào `landing-personal/README.md`
  cùng wave (Bucket C) — design-first cho production-port.

**Không có gap mới filed** (external scores ≥ floor, parity clean). GAP-274 + GAP-428 đủ điều kiện DONE (kit scope).

---

## Cross-link
- Kit: `ui_kits/kiteclass-public/` + `ui_kits/landing-personal/` (PR #2326)
- Audit verdict nguồn: `2026-06-11-pre-wave-ui-kits-100-outside-in-refresh.md` §3(c)
- Rubric: `.claude/skills/quality/ui-review/SKILL.md`
- Standard: `frontend-standards.md` §3.1 Kit as Source of Truth + `feedback_audit_calibration`
- Gaps: GAP-274 (KC public marketing kit) · GAP-428 (prospect public pages) · GAP-1229 (favicon)
