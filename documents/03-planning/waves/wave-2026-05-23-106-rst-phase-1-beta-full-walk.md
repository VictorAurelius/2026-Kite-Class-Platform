---
title: Đợt 106 — RST đầy đủ Pha 1 BETA (23 luồng × 4 vai trò)
status: draft
created: 2026-05-23
updated: 2026-05-27
waves: [106]
gaps: [GAP-724, GAP-725, GAP-761]
audience: dev
---

# Đợt 106 — RST đầy đủ Pha 1 BETA

**Mục tiêu:** Đi xuyên suốt 23 luồng (4 vai trò × phạm vi Pha 1 BETA) trên trình duyệt thật — ra danh sách lỗi thật làm cơ sở ưu tiên cho Đợt 107.
**Khởi sự:** Phiên Đợt 105 chứng minh — RST chuỗi đăng nhập KiteClass bắt 5 lỗi mà danh sách gap không có. Trước khi mời tester beta, cần đi hết 23 luồng để biết luồng nào thông, luồng nào vỡ.
**Thời gian ước tính:** ~3-5 giờ làm việc agent, chia 4 phiên nhỏ.

---

## 1. Brainstorm (5-10 phút)

**Q1 (đối tượng phục vụ):** 4 vai trò Pha 1 BETA — Khách ẩn danh / Chủ trung tâm / Nhân viên / Quản trị nền tảng. Phụ huynh + Giáo viên đã đẩy Pha 2 (GAP-725); Học sinh không nằm trong Pha 1.

**Q1-bis (inside-out queue cross-check per `inside-out-completeness-trigger.md` §3 — patched 2026-05-27):**

| Source | Item | Phase relevance | Action trong Wave 106 |
|---|---|---|---|
| `inside-out-queue.md` 2026-05-14 | Premium plan / pricing surface — disclaimer Phase 1 BETA + lifetime discount post-convert + TOS checkbox + "Free during beta" | phase-1-beta (disclaimer) | **Acknowledge khi walk A1 (trang chủ) + A2 (yêu cầu beta)** — flag presence/absence of beta disclaimer + pricing copy; file gap nếu missing |
| Audit ROADMAP §🎯 2026-05-27 | GAP-761 P1 OPEN (Zustand persist rehydrate route-guard sentinel ~4-5h) — KH route-guard layouts 5 sites | phase-1-beta | **Acknowledge — Mảng D (KH Platform Admin) walk có thể hit residual race 5/20 PASS post-GAP-760 PARTIAL 40%; nếu walk fail → confirm GAP-761 scope chưa fix → defer fix Wave 107+** |
| ROADMAP §🎯 GAP-756 P0 | Wave production deploy + RST verify (blocked GAP-612 RST + ECR repo provisioning) | phase-1-beta | **Out-of-scope Wave 106** (local walk only) — flag selective production RST trigger sau Wave 106 fix queue |

**Q2 (giải pháp đã xét và loại):**
- ❌ Sửa hết gap đang mở trước rồi mới RST → nhiều gap có thể đã tự sửa qua các đợt trước; tốn công vào việc không còn relevant. RST đầu sẽ ra danh sách thực tế đáng tin hơn.
- ❌ Cắt phạm vi còn 12 hoặc 8 luồng cốt lõi → tester beta đụng vào toàn bộ; cắt phạm vi = chấp nhận tester báo lỗi ở luồng chưa walk = tốn vòng đối thoại sau.
- ❌ Chỉ ghi gap, không sửa tại chỗ → nhiều luồng sẽ bị chặn giữa chừng, không đi tiếp được; hiệu suất walk giảm.
- ✅ **RST đầy đủ 23 luồng + sửa tại chỗ với lỗi chặn luồng** — cân bằng giữa khảo sát đầy đủ và tiến độ hoàn thành.

**Q3 (rủi ro):**
- Walk có thể lộ lỗi kiến trúc lớn (ví dụ thiếu cấu hình cổng thanh toán cục bộ) → đẩy thành gap, không sửa trong đợt.
- Luồng vận hành (B9 điểm danh / B10 thu chi) phụ thuộc dữ liệu nền (lớp + học viên + lịch học) → cần chạy theo thứ tự đúng (B7 → B8 → B9 → B10), không random.
- Phụ thuộc nhau giữa mảng: Mảng C (Nhân viên) cần B13 (mời nhân viên) đã chạy → đi C sau B.

---

## 2. Task Breakdown

| Mảng | Vai trò | Số luồng | Người chạy | Thời gian | Phụ thuộc |
|---|---|:---:|---|:---:|---|
| A | Khách ẩn danh | 3 | agent + claude | ~20 phút | (không) |
| B-onboard | Chủ trung tâm (vào hệ thống) | 4 | claude (đăng nhập có sẵn) | ~30 phút | (không) |
| B-CRUD | Chủ trung tâm (quản lý dữ liệu) | 4 | claude | ~40 phút | B-onboard |
| B-vận-hành | Chủ trung tâm (nghiệp vụ ngày) | 5 | claude | ~50 phút | B-CRUD (cần dữ liệu nền) |
| C | Nhân viên | 3 | claude | ~25 phút | B-vận-hành (cần B13 mời) |
| D | Quản trị nền tảng | 4 | claude | ~20 phút | (không — Đợt 105 đã có 2/4) |

Kiểm tra rời rạc: 6 mảng đụng vào miền khác nhau nhưng đều trên cùng cụm Docker → KHÔNG chạy song song agent worktree; chạy tuần tự trong 1-2 phiên.

---

## 3. Scope (lược đồ rút gọn)

**Bậc rủi ro:** TRUNG BÌNH → mô hình Opus medium. Phạm vi user-facing walk + sửa lỗi chặn luồng, không động kiến trúc.
**Có đụng xuyên tầng?** KHÔNG → không cần Mảng 0 Nền tảng (mỗi mảng đụng FE / BE riêng khi cần sửa).

> **Quy ước tham chiếu gap** (theo `.claude/rules/gap-architecture-v2.md`): mỗi lỗi phát hiện → mở GAP-NNN mới trong `documents/04-quality/gaps/phase-1-beta/` + đồng bộ `gap-status.csv` cùng PR sửa.

| # | Mảng | Luồng | Trạng thái khởi đầu |
|:-:|---|---|:---:|
| 1 | A | A1 Trang chủ ẩn danh — bộ ba (Tính năng / Bảng giá / Liên hệ) hiển thị đúng | ❌ chưa walk |
| 2 | A | A2 Biểu mẫu yêu cầu beta — gửi thành công + nhận thư xác nhận | ❌ chưa walk |
| 3 | A | A3 Trang chính sách — Điều khoản / Quyền riêng tư / Câu hỏi tiếp cận từ chân trang | ❌ chưa walk |
| 4 | B-onboard | B1 Đăng ký dùng thử sau khi được duyệt beta (đường liên kết mời) | ❌ chưa walk |
| 5 | B-onboard | B2 Trợ lý cài đặt ban đầu (tên trung tâm + lĩnh vực + thương hiệu cơ bản) | ❌ chưa walk |
| 6 | B-onboard | B3 Đăng nhập lại + chọn trung tâm (nếu có N trung tâm) | ⚠️ một phần (PR #1737) |
| 7 | B-onboard | B4 Bảng điều khiển + bấm vào mỗi mục thanh điều hướng — không lỗi 404 / không trang trắng | ⚠️ chỉ thấy thanh |
| 8 | B-CRUD | B5 Quản lý học viên — Tạo / sửa / xoá / tìm kiếm / phân trang | ❌ chưa walk |
| 9 | B-CRUD | B6 Quản lý giáo viên — Tạo / sửa / xoá / xem lịch dạy | ❌ chưa walk |
| 10 | B-CRUD | B7 Quản lý khoá học — Tạo / sửa / xoá / xếp giáo viên cho khoá | ❌ chưa walk |
| 11 | B-CRUD | B8 Quản lý lớp — Tạo lớp + gán giáo viên + thêm học viên + lập lịch buổi học | ❌ chưa walk |
| 12 | B-vận-hành | B9 Điểm danh — Mở buổi + đánh điểm danh + ghi chú nghỉ phép | ❌ chưa walk |
| 13 | B-vận-hành | B10 Thu chi — Tạo hoá đơn từ lớp + ghi nhận thanh toán (tiền mặt / chuyển khoản) | ❌ chưa walk |
| 14 | B-vận-hành | B11 Báo cáo — Doanh thu tháng + tỷ lệ điểm danh | ❌ chưa walk |
| 15 | B-vận-hành | B12 Cài đặt trung tâm — Đổi tên / đổi logo / đổi mật khẩu | ❌ chưa walk |
| 16 | B-vận-hành | B13 Mời Nhân viên qua thư + xem trạng thái lời mời | ❌ chưa walk |
| 17 | C | C1 Nhân viên nhận thư mời → đăng ký tài khoản | ❌ chưa walk |
| 18 | C | C2 Nhân viên đăng nhập → bảng điều khiển nhân viên | ❌ chưa walk |
| 19 | C | C3 Kiểm chứng giới hạn quyền — không thấy thanh toán + không thấy cấu hình trung tâm | ❌ chưa walk |
| 20 | D | D1 Đăng nhập trang quản trị | ✅ Đợt 105 Mảng E |
| 21 | D | D2 Duyệt yêu cầu beta | ✅ Đợt 105 Mảng E |
| 22 | D | D3 Xem danh sách trung tâm + chi tiết tenant | ❌ chưa walk |
| 23 | D | D4 Xem nhật ký audit — đăng nhập + hành động nhạy cảm (V62/V63 đã ship) | ❌ chưa walk |

**Tóm tắt:** 23 luồng tổng; 2 đã thông (D1+D2); 2 một phần (B3+B4); **19 cần walk**.

### Bằng chứng kiểm tra trạng thái (per `audit-to-gap-pipeline.md` §2.6)

Mọi tên đường dẫn / endpoint / trang được tham chiếu trong §3 đã verify tồn tại trong mã hiện tại:

| Tham chiếu | Lệnh xác minh | Phán quyết |
|---|---|---|
| `/login` KH + KC | `find kitehub/kitehub-frontend/src/app -name "login" -type d` + `find kiteclass/kiteclass-frontend/src/app -name "login" -type d` | ✅ tồn tại |
| `/admin/beta-requests` | grep `beta-requests` trong `kitehub-frontend/src/app/admin/` | ✅ tồn tại (Đợt 105 đã walk) |
| `/dashboard` KC | `find kiteclass/kiteclass-frontend/src/app -name "dashboard" -type d` | ✅ tồn tại (PR #1737 đã đến đây) |
| `/students` `/teachers` `/courses` `/classes` `/attendance` `/finance` KC | xem thanh điều hướng trong ảnh chụp Đợt 105 — đầy đủ | ✅ tồn tại |
| Trang chủ ẩn danh KH `/` | `head -20 kitehub-frontend/src/app/page.tsx` | ✅ tồn tại |
| Biểu mẫu yêu cầu beta | grep `beta-request` trong `kitehub-frontend/src/app/` | ⚠️ cần verify tên đường dẫn lúc walk |

(Toàn bộ luồng còn lại — wizard onboarding + mời nhân viên + báo cáo — sẽ verify trạng thái lúc bắt đầu mảng tương ứng, KHÔNG mở danh sách lỗi giả định trước.)

---

## 4. State-Check Evidence

Xem §3 "Bằng chứng kiểm tra trạng thái (per `audit-to-gap-pipeline.md` §2.6)" — table đã verify mọi tên đường dẫn tham chiếu (KH + KC frontend routes + admin pages + auth credentials) đã tồn tại trong mã hiện tại.

## 5. Verification Gates

| Mảng | Lệnh kiểm thử cục bộ | CI gate |
|---|---|---|
| A (anonymous KH) | `PLAYWRIGHT_BASE_URL=http://localhost:3001 pnpm exec playwright test e2e/_rst-wave-106-a.spec.ts` | frontend-ci |
| B (owner KC) | `PLAYWRIGHT_BASE_URL=http://localhost:3000 pnpm exec playwright test e2e/_rst-wave-106-b.spec.ts` | frontend-ci |
| C (staff KC) | Tương tự, dùng staff seed | frontend-ci |
| D (admin KH) | `PLAYWRIGHT_BASE_URL=http://localhost:3001 pnpm exec playwright test e2e/_rst-wave-106-d.spec.ts` | frontend-ci |

## 6. Agent Spawn Pattern

Đợt này KHÔNG dùng parallel agent — single coordinator (main session) walk tuần tự 23 luồng. Lý do: 4 mảng đều dùng Docker port độc quyền (3000/3001/9000) → không thể chạy song song Playwright agent worktree.

Đợt 107 hybrid SẼ dùng agent spawn pattern cho fix cụm thư (3 agent worktree song song) — xem `wave-2026-05-23-107-hybrid-rst-anonymous-onboard-plus-email-fix.md` §5 Tối ưu agent.

## 7. Closure Protocol

1. **File này (`wave-2026-05-23-106-...md`)** trạng thái → `status: complete` cuối đợt, có bảng tổng hợp:
   - Số luồng đi xuyên suốt / số luồng vỡ giữa chừng
   - Danh sách GAP mới được mở (mã + ưu tiên)
   - Số lỗi sửa tại chỗ (PR đính kèm)
   - **Scope-Completeness Reconciliation table** (per `wave-closure-scope-completeness.md` v1.0.0 — mandatory từ 2026-05-18) — mọi item §3 Scope categorize ✅DONE / 🟡PARTIAL / ❌NOT-IMPL với follow-up gap link

2. **Mỗi lỗi phát hiện** → một file `documents/04-quality/gaps/phase-1-beta/GAP-NNN-...md` (theo `audit-to-gap-pipeline.md` §3) + một dòng trong `gap-status.csv`.

3. **Lỗi chặn luồng** → sửa cùng đợt + ship PR theo từng nhóm logic (vd PR 1 cho Mảng A + Mảng B-onboard; PR 2 cho B-CRUD; v.v.). Mỗi PR không quá 5 luồng để giữ quy mô review hợp lý.

4. **Lỗi không chặn** (hiển thị xấu / sót dịch sang tiếng Việt / nội dung mẫu là tên tiếng Anh) → ghi gap, đẩy Đợt 107 phân loại, không sửa trong đợt này.

5. **RST → E2E promotion mandate** (per `e2e-rst-test-layer-boundary.md` v1.0.0 — mandatory từ 2026-05-25):

   Mỗi bug fix PR Wave 106 PHẢI paired NEW E2E spec same PR (regression-guard), TRỪ KHI bug class thuần subjective UX không có testable invariant.

   | Bug class | E2E spec required? | Spec location |
   |---|:---:|---|
   | Auth redirect / role-guard / form validation | ✅ MANDATORY | `kiteclass-frontend/e2e/` hoặc `kitehub-frontend/e2e/` |
   | API contract mismatch / HTTP status | ✅ MANDATORY | E2E (contract test) |
   | CRUD flow (B-CRUD / B-vận-hành) | ✅ MANDATORY | E2E full-flow spec |
   | Copy English-in-VN context / cultural feedback | ❌ EXEMPT — trailer `RST_E2E_PROMOTION_EXEMPT: <bug-id> — cultural feedback only` | (cultural review track) |
   | Layout 360px mobile / visual regression | ⚠️ visual regression tool (deferred Phase 1.5+) | Document but exempt v1 |

   Fix PR body PHẢI có `## RST→E2E promotion` section ghi rõ spec(s) added hoặc exemption rationale.

6. **Ảnh chụp** mỗi luồng trong `/tmp/rst-screenshots/wave-106/<mảng>-<luồng>-<bước>.png` để đối chiếu trước/sau khi tester beta đến.

### Tiêu chí kết đợt

- [ ] 23/23 luồng đã được walk + đánh giá xanh / vàng / đỏ
- [ ] Mọi lỗi chặn luồng (P0) đã sửa hoặc có gap follow-up với người chịu trách nhiệm rõ ràng
- [ ] Mọi lỗi không chặn đã có gap mở trong `phase-1-beta/`
- [ ] **Mỗi bug fix PR có E2E spec paired** (per §7.5) HOẶC explicit exemption trailer
- [ ] **Scope-Completeness Reconciliation table** trong closure PR body — mọi §3 item reconcile ✅/🟡/❌
- [ ] Bảng tổng hợp cuối đợt + danh sách Đợt 107 nháp gửi cho người dùng đầu phiên kế

---

## Tiền điều kiện trước khi mở Đợt 106

**Cập nhật 2026-05-27 (sau 12 wave shipped + 4 PR pending merge từ Wave rst-cleanup session):**

| Điều kiện | Trạng thái | Hành động |
|---|:---:|---|
| ~~PR #1737 (chuỗi đăng nhập KC) gộp main~~ | ✅ DONE | Wave 105 shipped + GAP-724 DONE; auth flat shape contract synced |
| ~~PR #1738 (GAP-725 đẩy Pha 2) gộp main~~ | ✅ DONE | Wave 105 shipped |
| **Wave rst-cleanup 4 PRs merge (#1890 + #1891 + #1892 + #1893)** | ⏳ pending CI | **Đợi merge trước khi pivot** — clean main HEAD baseline. Per option B picked 2026-05-27. |
| Wave 105 contract sync impact verify (`User.userType` singular shape) | ✅ verified | PR #1891 fix 6 TS test type drift; `useAuthStore.setState({user: {userType: UserType.TEACHER}})` updated; KC FE test fixtures synced |
| GAP-761 P1 OPEN (Zustand sentinel ~4-5h) — KH route-guard race 5/20 PASS | ⚠️ unfixed | **Wave 106 Mảng D có thể hit** — nếu KH admin walk fail at route-guard, file note → defer fix Wave 107+ (KHÔNG fix Wave 106 tại chỗ vì scope ~4-5h vượt RST budget) |
| Dữ liệu test: ≥1 trung tâm + ≥3 lớp + ≥5 học viên + ≥1 giáo viên | ❌ chưa | **Seed-as-you-go strategy chốt:** build through UI Mảng B-CRUD (B5-B8) — accept risk +30-60 phút retro nếu B-vận-hành thiếu data. Pre-seed script alternative tốn ~20-30 phút prep nhưng eliminate retro risk. Single coordinator default = seed-as-you-go. |
| Dữ liệu test: tài khoản Nhân viên + lời mời đang chờ | ❌ chưa | Sẽ tạo qua B13 (mời thật từ Chủ trung tâm) — sequential dependency C ← B-vận-hành đã capture §2 |
| Tài khoản Khách ẩn danh = không cần (anonymous) | ✅ | (không) |
| Tài khoản Chủ trung tâm `owner.test@test.vn / Test@1234` | ✅ | `scripts/local-test-fixtures/seed-test-users.sh` Đợt 105 seed (3 users: owner + admin + staff) |
| Tài khoản Quản trị nền tảng `admin.test@test.vn / Test@1234` | ✅ | Cùng script Đợt 105 |
| Tài khoản Nhân viên `staff.test@test.vn / Test@1234` | ✅ | Cùng script — sẵn cho Mảng C2/C3 walk (sau khi B13 mời tạo invite link) |
| Cụm Docker LOCAL đang chạy (13 dịch vụ healthy) | ✅ verified 2026-05-27 | kite-postgres / kite-redis / kite-rabbitmq / kite-minio / kitehub-* / kiteclass-core / kite-gateway — all up 5h healthy |
| AWS production stack | ⚪ N/A | Wave 106 = **LOCAL walk only** (port 3000 KC FE / 3001 KH FE / 9000 MinIO); AWS production deploy = separate GAP-756 P0 sau Wave 106 fix queue |

---

## Phạm vi ngoài đợt này

| Mục | Lý do |
|---|---|
| Vai Phụ huynh / Giáo viên / Học sinh | Đẩy Đợt 2 qua GAP-725 (đã chốt) |
| Cổng thanh toán VNPAY thật | Tích hợp Pha 1.5+ (GAP-722 đã ghi) |
| Cổng VietQR thật | Tương tự, Pha 1.5+ |
| Gửi thư qua Resend production | Pha 1.5+ — Đợt 106 dùng giả lập cục bộ |
| Tải lên tệp (giáo trình / ảnh đại diện) | Nếu B5/B6 có nhập ảnh → walk; nếu không → bỏ qua đợt này |
| Mã đa ngôn ngữ tiếng Anh (i18n) | Pha 2+ — Đợt 106 chỉ kiểm tra phiên bản tiếng Việt |
| Đợt sửa gap đã mở trước Đợt 106 | Dồn vào Đợt 107 sau khi có danh sách lỗi thật từ RST |

---

## 8. Log

- **2026-05-27 (PATCH — pre-execution review):** Plan PATCH PR ship 4 nhóm cập nhật trước khi pivot execution:
  - **§1 Brainstorm Q1-bis** (NEW row block) — inside-out queue cross-check per `inside-out-completeness-trigger.md` §3: Premium plan disclaimer (queued 2026-05-14) sẽ chạm Mảng A1+A2; GAP-761 P1 OPEN ack Mảng D race risk; GAP-756 P0 production deploy explicit out-of-scope
  - **§Tiền điều kiện** state sync: PR #1737 + PR #1738 đã DONE (Wave 105 shipped); Wave 105 contract sync (`User.userType` singular) verify đã sync qua PR #1891 FE test fix; GAP-761 unfixed flagged; Wave rst-cleanup 4 PRs pending merge gate; AWS production explicit N/A row (LOCAL Docker only walk)
  - **§7 Closure** thêm 2 mandate mới: (5) RST→E2E promotion per `e2e-rst-test-layer-boundary.md` v1.0.0 + bug-class table (auth/contract/CRUD MANDATORY; cultural EXEMPT); (Scope-Completeness Reconciliation table) per `wave-closure-scope-completeness.md` v1.0.0 mandatory mỗi closure PR
  - **Tiêu chí kết đợt** thêm 2 checkbox: E2E spec paired per fix PR + Reconciliation table trong closure PR
  - **Test data seed strategy** chốt: seed-as-you-go through UI Mảng B-CRUD (single coordinator default; accept retro risk +30-60 phút nếu B-vận-hành thiếu data); `scripts/local-test-fixtures/seed-test-users.sh` đã cover 3 credentials (owner/admin/staff)
  - **Frontmatter** updated: 2026-05-23 → 2026-05-27 + gaps list += GAP-761
  - Patch lý do: trong 4 ngày (2026-05-23 → 2026-05-27) shipped 12 waves + 5 META rules mới landed (`e2e-rst-test-layer-boundary.md` + `wave-closure-scope-completeness.md` + `inside-out-completeness-trigger.md` + `agent-model-opus-default.md` + `pre-flight-aws-lifecycle-check.md`) — 2 rules áp dụng trực tiếp Wave 106 closure (E2E promotion + reconciliation table); 1 rule áp dụng plan-time (inside-out queue check)
- **2026-05-23 (nháp):** Soạn nháp sau khi user chốt phương án (RST trước / 23 luồng giữ nguyên / sửa lỗi chặn luồng tại chỗ) qua AskUserQuestion. Đợt 105 vừa khép. Bắt đầu thực thi sau khi PR kế hoạch này gộp + tiền điều kiện thoả mãn.
