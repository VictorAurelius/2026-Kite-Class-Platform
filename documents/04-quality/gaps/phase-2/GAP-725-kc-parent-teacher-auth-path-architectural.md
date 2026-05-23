---
id: GAP-725
title: KC Parent/Teacher persona auth path — architectural gap
status: 🟦 DEFERRED Phase 2
priority: P1
phase: phase-1-beta
type: feature
created: 2026-05-23
deferred_at: 2026-05-23
deferred_to: phase-2
decision: B+C ghép theo vai (B = KC tự cấp đăng nhập cho Giáo viên; C = lời mời + OTP Zalo/SMS cho Phụ huynh + Học sinh)
discovered_via: Wave 105 RST UI walk 2026-05-23 — KC Parent/Teacher persona walks
related: [GAP-724, Wave 105]
---

## Problem

Wave 105 RST UI walk attempted Parent + Teacher persona browser walks on kc-frontend
(routes `/parent`, `/teacher`). Both redirected to `/login` because:

1. KH `PlatformRole` enum exposes only **OWNER / STAFF / PLATFORM_ADMIN** (per
   `kitehub-subscription/.../auth/role/PlatformRole.java`). PARENT and TEACHER are
   not valid platform roles.
2. KC frontend `types/auth.ts UserType` declares ADMIN/STAFF/TEACHER/PARENT/STUDENT
   (intended as KC-internal multi-tenant ops roles), but the FE login flow
   currently calls `POST /api/auth/login` which lands on **KH subscription**
   (not KC). KH issues OWNER/STAFF JWT only.
3. Net effect: there is no production login path that produces a JWT with
   `role: PARENT` or `role: TEACHER` for the KC route-guard to accept.

## Impact

- KC Parent portal (`/parent/*` route family) cannot be reached in production
  even though backend endpoints exist (per V61/V64 migrations + Bucket D
  Wave 105 Parent persona walk).
- KC Teacher dashboard (`/teacher/*`) likewise unreachable.
- Wave 105 RST UI walk goal "RST full UI" partial — Owner walk PASS only.

## Proposed Fix (3 options)

### Option A — Extend KH PlatformRole (architectural change)

Add PARENT + TEACHER to `PlatformRole` enum + DB CHECK constraint + JWT claim.
KH issues unified JWT for all roles. KC route-guard already accepts.

Pros: single auth path, simpler mental model
Cons: changes KH from "tenant management" to "multi-role identity provider"

### Option B — KC-native login endpoint

KC core exposes `/api/v1/auth/login` for PARENT/TEACHER/STUDENT. Each tenant
has its own auth scope. KC issues its own JWT signed with same secret.

Pros: keeps KH narrow scope; matches multi-tenant SaaS pattern (tenant-local users)
Cons: 2 login paths to maintain; user model split across KH (owner) + KC (parents)

### Option C — Federated identity (Phase 2+)

KH stays OWNER-only. Parents/teachers sign in via tenant's invite link + OTP
(no password). KC issues short-lived session token per device.

Pros: best UX for non-tech parents; aligns with `dev-readable-doc-language.md`
VN edu market (`vn-localization-audit-checklist.md` §4 phone OTP culture)
Cons: largest scope; Phase 2 work

## Quyết định 2026-05-23 — đẩy sang Đợt 2, ghép B+C theo vai

Đợt 1 BETA giữ phạm vi hẹp (Chủ trung tâm + Nhân viên — đã thông qua PR #1737).
Phụ huynh / Giáo viên / Học sinh chuyển sang Đợt 2.

| Vai | Hướng sửa | Lý do chọn |
|---|---|---|
| **Giáo viên** | Hướng B — KiteClass tự cấp endpoint `/api/v1/auth/login` (thư điện tử + mật khẩu, cùng khóa ký HS512) | Giáo viên rành công nghệ, đăng nhập hàng ngày → cần mật khẩu để ghi nhớ phiên dài |
| **Phụ huynh** | Hướng C — Lời mời từ Chủ trung tâm + OTP qua Zalo / SMS → phiên ngắn hạn 24-48 giờ | Văn hóa Việt: phụ huynh dùng Zalo + SĐT nhiều hơn thư điện tử (per `vn-localization-audit-checklist.md` §4 phong tục thanh toán + nhóm chat) |
| **Học sinh** | Hướng C (giống Phụ huynh) — hoặc đăng nhập kế thừa qua tài khoản cha mẹ | Nghiên cứu Đợt 2 thêm — tùy mô hình khóa học (K-12 vs trung tâm tiếng Anh) |

**Cột mốc khởi động Đợt 2:**
- Đạt ≥80 điểm chất lượng + 5 tenant sống + 0 sự cố P0 hai tuần liên tiếp (theo `release-1-plan-2026.md` Pha 1 → Pha 2 chuyển dịch).
- Chọn nhà gửi SMS (Twilio / Vonage / nhà cung cấp Việt) + duyệt tài khoản Zalo Official Account TRƯỚC khi mở mã của Hướng C.

**Loại bỏ:**
- Hướng A — phá tách miền KH=quản trị thuê bao, không phù hợp đường dài.

## References

- PR #1737 — GAP-724 Owner login chain fix
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/role/PlatformRole.java`
- `kiteclass/kiteclass-frontend/src/types/auth.ts` (UserType enum)
- Wave 105 wave plan §Bucket D Parent persona walk
- `vn-localization-audit-checklist.md` §4 phong tục Zalo + OTP điện thoại
- `release-1-plan-2026.md` Pha 2 cột mốc — chốt 2026-05-23 chọn B+C
