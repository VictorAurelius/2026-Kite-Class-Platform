# FE Route Bundle Measurement — closes GAP-1364 (audit 2026-06-14 sub-check 3.1/3.2)

**Ngày đo:** 2026-06-15
**Người đo:** audit-fixG-quality wave (Jacoco/bundle/triage)
**Closes:** GAP-1364 (Performance full audit 2026-06-14, F-008 — route bundle ❓ UNCHECKED)
**Build command:** `pnpm --filter kitehub-frontend build` + `pnpm --filter kiteclass-frontend build` (Next.js 15.5.18, production `next build`)
**Build kết quả:** cả 2 app `BUILD EXIT 0` (KH 68 route, KC 98 route).

---

## 0. Lưu ý đo lường — RAW vs GZIPPED

Next.js báo cáo **"First Load JS" ở dạng RAW (parsed/uncompressed)**, KHÔNG phải gzipped. Sub-check audit dùng ngưỡng gzipped:
- **3.1** route bundle ≤ **250KB gzipped**
- **3.2** First Load JS shared ≤ **200KB** (audit không nói rõ gzipped; dùng raw làm worst-case)

Quy đổi thực nghiệm: gzip cho JS bundle ≈ **30–35%** của raw. Bảng dưới ghi số RAW của Next.js + cột gzipped ước lượng (raw × 0.34) để so ngưỡng.

---

## 1. KiteHub-frontend (68 route)

**First Load JS shared by all: 103 kB raw (~35 kB gzipped)** → ✅ PASS 3.2 (kể cả raw 103 < 200).

Top route theo First Load JS (raw):

| Route | Page size | First Load JS (raw) | ~Gzipped | Verdict |
|---|---|---|---|---|
| `/billing` | 9.19 kB | **212 kB** | ~72 kB | ✅ < 250KB gz |
| `/admin/instances` | 17.4 kB | 211 kB | ~72 kB | ✅ |
| `/branding` | 5.83 kB | 207 kB | ~70 kB | ✅ |
| `/instances` | 3.69 kB | 195 kB | ~66 kB | ✅ |
| `/instances/[id]` | 5.72 kB | 195 kB | ~66 kB | ✅ |

Max route raw = **212 kB** (`/billing`) → ~72 kB gzipped → **PASS 3.1** (< 250KB gz, biên rộng).

---

## 2. KiteClass-frontend (98 route)

**First Load JS shared by all: 103 kB raw (~35 kB gzipped)** → ✅ PASS 3.2.

Top route theo First Load JS (raw):

| Route | Page size | First Load JS (raw) | ~Gzipped | Verdict |
|---|---|---|---|---|
| `/settings` | 33.9 kB | **276 kB** | ~94 kB | ⚠️ raw > 250 nhưng gz < 250 → PASS, **watch** |
| `/students` | 7.33 kB | 264 kB | ~90 kB | ✅ < 250KB gz |
| `/billing` | 6.86 kB | 249 kB | ~85 kB | ✅ |
| `/classes/[id]/attendance` | 3.08 kB | 237 kB | ~81 kB | ✅ |
| `/courses/[id]` | 10.3 kB | 237 kB | ~81 kB | ✅ |
| `/classes/[id]` | 9.03 kB | 235 kB | ~80 kB | ✅ |
| `/students/[id]/attendance` | 9.37 kB | 233 kB | ~79 kB | ✅ |
| `/courses/[id]/classes/new` | 3.75 kB | 233 kB | ~79 kB | ✅ |

Max route raw = **276 kB** (`/settings`, page-level 33.9 kB — màn lớn nhất) → ~94 kB gzipped → **PASS 3.1** (< 250KB gz).

---

## 3. Verdict

| Sub-check | Ngưỡng | KH | KC | Kết luận |
|---|---|---|---|---|
| 3.1 route bundle ≤ 250KB gzipped | gzipped | max ~72 kB | max ~94 kB | ✅ PASS cả 2 |
| 3.2 First Load JS shared ≤ 200KB | — | 103 kB raw | 103 kB raw | ✅ PASS cả 2 |

**Không có route vượt 250KB gzipped.** GAP-1364 sub-check 3.1/3.2 chuyển từ ❓ UNCHECKED → ✅ PASS bằng số liệu thật.

## 4. Watch item (không phải finding chặn)

- **KC `/settings` 276 kB raw / ~94 kB gzipped** — route nặng nhất, page-level 33.9 kB. Vẫn dưới ngưỡng gzipped nhưng là ứng viên đầu tiên nếu cần code-split khi màn settings phình thêm. Theo dõi ở lần performance audit kế; nếu raw > ~370 kB (≈250KB gz) thì file gap code-split. Hiện KHÔNG cần action.
- Khớp với GAP-354 (phase-2, per-kit bundle budget — scope khác: production-port kit).
