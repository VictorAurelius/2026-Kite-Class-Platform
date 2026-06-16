---
title: G2 Recipe ĐƠN GIẢN — KC-1 (kiteclass :3000) → flip 5 gap PARTIAL
audience: dev
date: 2026-06-09
flow: KC-1 G2 (dashboard :3000)
gaps: [GAP-1067, GAP-1071, GAP-1072, GAP-1073, GAP-1074]
stack_state_required: full (local Docker healthy)
supersedes: 2026-06-08-g2-recipe-kc1-remaining-browser-walks.md (bản gọn, gộp 1067+1071)
---

# G2 walk KC-1 — bản đơn giản (2026-06-09)

> Mục đích: bạn click thật trên browser `:3000`, mỗi bước có **làm gì → đúng thì thấy gì**. Xong báo 4-outcome → tôi flip gap DONE.
> **KHÔNG đụng landing/nip.io** (đó là GAP-811, walk riêng sau khi rebuild middleware).

## Setup (30 giây)
```bash
docker ps --format '{{.Names}}\t{{.Status}}' | grep kite   # tất cả phải healthy
```
- Login: http://localhost:3000/login
- Tenant **A**: `owner@skyedu.vn` / `SkyEdu@2026` · Tenant **B**: `owner@skyedu.vn` / `SkyEdu@2026`
- Mở DevTools (F12) → tab **Network** (để xem status) + **Application → Storage**.

---

## Walk 1 — Trang load + shell (GAP-1067 + GAP-1071)
1. Mở http://localhost:3000/login → **đúng:** trang login hiện ra (KHÔNG trắng/`ERR_EMPTY_RESPONSE`). → *pass = GAP-1067 OK.*
2. Login tenant A → vào `/dashboard` → **đúng:** có **header + sidebar trái + footer** đầy đủ, tên "Sky Education". → *pass = GAP-1071 OK.*
3. Bấm vài menu sidebar (Lớp / Học viên / Cài đặt) → **đúng:** mỗi trang vẫn còn shell (không mất header/sidebar). → *củng cố GAP-1071.*

## Walk 2 — Session cross-tab (GAP-1074)
1. Giữ tab 1 đang login tenant A. Mở **tab mới** (Ctrl+T) → gõ http://localhost:3000/dashboard.
2. **đúng:** tab 2 vào thẳng dashboard tenant A, **KHÔNG bắt login lại**. → *pass = GAP-1074 OK.*
3. (kiểm chứng) Application → Local Storage `localhost:3000` → có key `kc:<tenantId>:accessToken`.

## Walk 3 — Upload + render logo (GAP-1073 + GAP-1072)
1. Vào http://localhost:3000/settings → khu vực logo/thương hiệu.
2. Chọn 1 ảnh PNG/JPG < 2MB → Upload → **đúng:** toast "thành công", KHÔNG lỗi đỏ. Network: request upload **200/201**. → *pass = GAP-1073 OK.*
3. F5 reload trang → **đúng:** logo vừa upload **hiện ra** (không vỡ ảnh, không 403). → *pass = GAP-1072 OK.*

---

## Báo kết quả (chọn 1)
Báo gọn dạng: `W1 ✅ | W2 ✅ | W3 ⚠️ (bước 2 upload 415)`

| Outcome | Nghĩa | Tôi làm gì |
|---|---|---|
| ✅ **ALL PASS** | Cả 3 walk pass | Flip **GAP-1067/1071/1072/1073/1074 → DONE** |
| ⚠️ **PARTIAL** | Walk nào pass/fail (nêu rõ bước) | Giữ gap fail ở PARTIAL + fix |
| ❌ **BLOCKED** | Không walk được (stack/login lỗi) | Nêu blocker, tôi xử |
| 🔄 **BUG MỚI** | Lỗi ngoài 5 gap | Tôi file gap mới |

## Lỗi thường gặp
| Triệu chứng | Xử nhanh |
|---|---|
| `:3000` trắng / ERR_EMPTY_RESPONSE | `cd kitehub && bash scripts/up.sh --force-recreate` (stale proxy → GAP-1067 chính là cái này) |
| Login 400 | Dùng đúng email bảng trên (owner@skyedu.vn / owner@skyedu.vn) |
| Upload 415 "part not present" | Network → Request Headers phải có `Content-Type: multipart/form-data; boundary=...` |
| Logo 403 "expired" | F5 reload (presigned regen-on-read); vẫn 403 → fail GAP-1072 |

**Liên kết:** gaps `documents/04-quality/gaps/phase-1-beta/GAP-{1067,1071,1072,1073,1074}*.md` · rule `g1-browser-walk-before-flip.md`, `g2-handoff-md-mandate.md`
