---
title: Đợt 107 hybrid — RST Mảng A + B-onboard (cục bộ) + fix cụm thư
status: draft
created: 2026-05-23
updated: 2026-05-23
waves: [107]
gaps: [GAP-543, GAP-657, GAP-659]
audience: dev
---

# Đợt 107 hybrid — RST Mảng A + B-onboard (cục bộ) + fix cụm thư

**Mục tiêu:** Đẩy 2 luồng kết quả song song trong khi GAP-612 (AWS bị tạm dừng) còn chặn — không chờ AWS phục hồi.
**Khởi sự:** Phiên 2026-05-23: 4 PR đã mở (#1737 sửa đăng nhập KC + #1738 GAP-725 + #1739 Đợt 106 plan + #1740 audit 80 PARTIAL). Backlog Pha 1 BETA 181 PARTIAL+OPEN, 32 chờ AWS. Đợt 106 đầy đủ (23 luồng) phụ thuộc AWS → defer; Đợt 107 nhỏ hơn chạy cục bộ được.
**Thời gian ước tính:** ~2.5-3 giờ — RST walk ~50 phút + 3 agent fix song song ~60-90 phút (longest agent) + tổng hợp ~30 phút.

---

## 1. Brainstorm (5-10 phút)

**Q1 (đối tượng phục vụ):**
- Vai Khách ẩn danh + Chủ trung tâm (quá trình vào hệ thống) — phạm vi đầu Pha 1 BETA
- Cụm thư: cải thiện trải nghiệm thư cho mọi vai trò Pha 1 BETA

**Q2 (giải pháp đã xét và loại):**
- ❌ Thuần fix gap → bỏ qua bug walk; Đợt 105 chứng minh RST phát hiện 5 bug mà gap không có
- ❌ Thuần RST → ngắn ~50 phút, dư phiên; lãng phí context
- ❌ Chờ AWS phục hồi rồi bắt đầu Đợt 106 đầy đủ → AWS đã 134+ giờ chưa phản hồi; không biết bao giờ
- ✅ **Hybrid: RST cục bộ + fix cụm thư cục bộ** — lấp đầy 1 phiên, ra 2 luồng kết quả

**Q3 (rủi ro):**
- RST walk có thể phát hiện bug B1-B4 (đăng ký dùng thử / wizard) → mở rộng phạm vi nếu lỗi chặn luồng (theo `gap-done-discipline.md` §3 — sửa tại chỗ, không drift scope)
- 3 agent fix có thể đụng cùng file `EmailTemplateRenderer` → kiểm tra rời rạc trước spawn
- Cụm thư có rủi ro rơi vào "code-ready chờ live verify hậu-AWS" như audit Đợt 106 đã cảnh báo — phải reframe AC cho gap thuộc đợt này

---

## 2. Task Breakdown

| Mảng | Loại | Người chạy | Phụ thuộc | Thời gian |
|---|---|---|:---:|:---:|
| **RST-A** Mảng A — Khách ẩn danh (A1+A2+A3) | RST walk | Main session | Docker stack lên | ~20 phút |
| **RST-B** Mảng B-onboard (B1+B2+B3+B4) | RST walk | Main session | RST-A xong | ~30 phút |
| **FIX-543** Audit nội dung 5 thư + sửa | Code fix | Agent A worktree | Không | ~30-45 phút |
| **FIX-657** Thêm plain-text + List-Unsubscribe + Reply-To | Code fix | Agent B worktree | Không | ~45-60 phút |
| **FIX-659** Staff-invite template + persona-tone split | Code fix | Agent C worktree | Không | ~45-60 phút |
| Tổng hợp + ship | Main session | — | Tất cả xong | ~30 phút |

**Kiểm tra rời rạc:**
- RST-A + RST-B: cùng main session, tuần tự A → B
- FIX-543 đụng `documents/01-business/kitehub/email/*` + audit artifact
- FIX-657 đụng `kitehub/kitehub-email/src/main/resources/templates/email/*.html` + Java template renderer
- FIX-659 đụng `EmailTemplateRenderer.Tone` enum + staff-invite specific template

**Kiểm tra trùng file:**
```bash
# 3 agent đụng folder con khác nhau trong kitehub-email; OK
# Risk: FIX-657 + FIX-659 cùng đụng EmailTemplateRenderer.java
```
→ Phải xác nhận trước spawn: tách rõ FIX-657 = framework (plain-text generator + List-Unsubscribe header) vs FIX-659 = template content (staff-invite + tone enum extend). Nếu vẫn xung đột → ship tuần tự (657 trước, 659 sau).

---

## 3. Scope (lược đồ rút gọn)

**Bậc rủi ro:** TRUNG BÌNH (RST có thể phát hiện bug chặn luồng → mở rộng scope; fix có thể chạm framework)
**Mô hình:** Opus 4.7 — bậc trung
**Có đụng xuyên tầng?** KHÔNG — RST chỉ đọc; 3 fix đụng kitehub-email module riêng. KHÔNG cần Mảng 0 Nền tảng.

> **Quy ước tham chiếu gap:** verify CSV trước spawn agent — `bash scripts/query-gaps.sh GAP-543` / `GAP-657` / `GAP-659`. Cập nhật CSV row sau khi fix xong (canonical theo `gap-architecture-v2.md` §3).

| # | Mảng | Phạm vi | Đầu vào | Đầu ra |
|:-:|---|---|---|---|
| 1 | RST-A | Walk 3 luồng anonymous KH frontend tại localhost:3001 | Docker stack healthy | Playwright spec + ảnh chụp + 0-3 GAP mới (nếu lỗi) |
| 2 | RST-B | Walk 4 luồng owner-onboard KC frontend tại localhost:3000 | RST-A xong + owner.test seed | Playwright spec + ảnh + 0-3 GAP mới |
| 3 | FIX-543 | Audit nội dung 5 thư hiện có (welcome / approval / invite / verify / reset) + sửa tone tiếng Việt | Template tệp `.html` hiện tại | 5 template chỉnh + audit log |
| 4 | FIX-657 | Thêm plain-text part + header `List-Unsubscribe` + `Reply-To` setting | EmailTemplateRenderer + 5 template | Java + template thay đổi + integration test |
| 5 | FIX-659 | Mở rộng `Tone` enum + tạo staff-invite template với 2 biến thể tone (formal owner vs informal teacher) | Tone enum + EmailTemplateRenderer | Enum + 2 template mới + unit test |

### Bằng chứng kiểm tra trạng thái (per `audit-to-gap-pipeline.md` §2.6)

| Tham chiếu | Lệnh xác minh | Phán quyết |
|---|---|---|
| KH frontend `/` ẩn danh | `curl -sI http://localhost:3001/` | ✅ Đã verify trong audit Đợt 106 |
| KC frontend `/login` + `/dashboard` | Đợt 105 PR #1737 đã walk | ✅ tồn tại |
| owner.test@test.vn / Test@1234 đã seed | `bash scripts/local-test-fixtures/seed-test-users.sh` Đợt 105 | ✅ |
| `EmailTemplateRenderer.java` | `find kitehub/kitehub-email/src -name "EmailTemplateRenderer.java"` | ⚠️ verify-at-spawn-time mỗi agent |
| `Tone` enum | `grep -rn "enum Tone" kitehub/kitehub-email/src/main/java/` | ⚠️ verify-at-spawn-time |
| 5 template tệp hiện có | `ls kitehub/kitehub-email/src/main/resources/templates/email/*.html` | ⚠️ verify-at-spawn-time |
| `staff-invite.html` template hiện có | `ls kitehub/kitehub-email/src/main/resources/templates/email/staff-invite.html` | ⚠️ verify-at-spawn-time — nếu chưa có thì FIX-659 = greenfield |

---

## 4. Đầu ra mong đợi

### 4.1 RST-A + RST-B (1 PR — `feat/wave-107-rst-a-b-onboard-walk`)

- File Playwright spec: `kitehub/kitehub-frontend/e2e/_rst-wave-107-anonymous.spec.ts` + `_rst-wave-107-owner-onboard.spec.ts`
- Ảnh chụp: `/tmp/rst-screenshots/wave-107/<mảng>-<luồng>-<bước>.png`
- File báo cáo `documents/04-quality/audits/persona-review/2026-05-23-wave-107-rst-a-b-onboard.md` — bảng thông/vỡ + ảnh tham chiếu + danh sách GAP mới phát hiện
- Mỗi lỗi chặn luồng → sửa tại chỗ (như Đợt 105 PR #1737)
- Mỗi lỗi không chặn → mở GAP mới ưu tiên cho Đợt 108 phân loại

### 4.2 FIX-543/657/659 (3 PR riêng — mỗi agent 1 worktree)

- **FIX-543** `fix/gap-543-email-content-audit-vietnamese-tone` — 5 template chỉnh + audit log
- **FIX-657** `fix/gap-657-email-plain-text-list-unsubscribe-replyto` — Java renderer + integration test
- **FIX-659** `fix/gap-659-staff-invite-template-tone-split` — Tone enum + 2 template + unit test

**Reframe AC quan trọng:** mỗi gap fix phải reframe AC "live verify post-deploy" theo `gap-done-discipline.md` §3 Option B:
- Drop AC live verify → `## Out-of-scope` section
- File follow-up gap `GAP-XXX-post-aws-live-verify-email-cluster` Đợt hậu-AWS
- Sau khi reframe + ship code + test → có thể flip DONE

### 4.3 Tiêu chí kết Đợt 107

- [ ] RST-A: 3 luồng anonymous đã được walk + đánh giá xanh/vàng/đỏ
- [ ] RST-B: 4 luồng owner-onboard đã được walk + đánh giá xanh/vàng/đỏ
- [ ] FIX-543: 5 template chỉnh + audit log + PR gộp + GAP-543 flip DONE (sau reframe AC)
- [ ] FIX-657: plain-text + List-Unsubscribe shipped + PR gộp + GAP-657 flip DONE (sau reframe AC)
- [ ] FIX-659: tone split + staff-invite template shipped + PR gộp + GAP-659 flip DONE (sau reframe AC)
- [ ] CSV `gap-status.csv` đồng bộ — 3 dòng GAP-543/657/659 status DONE
- [ ] File báo cáo Đợt 107 đóng + bảng tổng hợp Đợt 108 nháp

---

## 5. Tối ưu agent (chi tiết)

### 5.1 Cấu hình 3 agent fix song song

| Agent | Loại | Worktree | Phạm vi đụng | Mô hình |
|---|---|---|---|---|
| Agent A | `general-purpose` | `/tmp/wt-543-email-content` | `documents/01-business/kitehub/email/` + 5 template `.html` content | Opus medium |
| Agent B | `general-purpose` | `/tmp/wt-657-email-framework` | `EmailTemplateRenderer.java` (plain-text gen + List-Unsubscribe header) + `EmailServiceClient` | Opus medium |
| Agent C | `general-purpose` | `/tmp/wt-659-email-tone` | `Tone` enum + `staff-invite.html` template (greenfield nếu chưa có) | Opus medium |

**Spawn pattern:**
```bash
# Tạo 3 worktree trước
git worktree add /tmp/wt-543-email-content -b fix/gap-543-email-content-audit
git worktree add /tmp/wt-657-email-framework -b fix/gap-657-email-plain-text-list-unsub
git worktree add /tmp/wt-659-email-tone -b fix/gap-659-staff-invite-tone-split

# Spawn 3 agent trong 1 message Agent[] với run_in_background=true
```

### 5.2 Mỗi agent prompt skeleton

```
Bạn là agent fix GAP-XXX trong worktree /tmp/wt-XXX-...
1. cd /tmp/wt-XXX-... (KHÔNG được cd ra ngoài)
2. Đọc gap file documents/04-quality/gaps/phase-1-beta/GAP-XXX-...md (full)
3. Reframe AC "live verify post-deploy" theo gap-done-discipline.md §3 Option B
4. Implement code thay đổi + tests
5. Run `cd kitehub && ./mvnw -pl kitehub-email test -P strict-warnings` PASS
6. Cập nhật gap-status.csv (qua sed an toàn không phá hàng khác)
7. Commit + push branch
8. Tạo PR docs+code
9. Báo cáo ngắn (<500 từ): URL PR + tóm tắt thay đổi + AC reframe + test status
```

### 5.3 Coordinator (main session) sau spawn

- Main session: chạy RST-A + RST-B tuần tự trong khi agent fix chạy nền
- Khi nhận thông báo 1 agent xong → check PR + verify
- Khi cả 3 agent xong → tổng hợp + sửa nếu conflict + ship Đợt 107 closure PR

### 5.4 Banned

- ❌ KHÔNG spawn 2 agent walk Playwright song song (Docker port collision)
- ❌ KHÔNG dùng worktree absolute path trong commit messages (per `feedback_worktree_absolute_path_contamination.md`)
- ❌ KHÔNG flip DONE khi chưa reframe AC live-verify (gap rơi vào pattern "code-ready chờ live walk hậu-AWS")

---

## 6. Tiền điều kiện trước khi mở Đợt 107

| Điều kiện | Trạng thái | Hành động |
|---|:---:|---|
| Docker stack 13 dịch vụ healthy | ✅ verified 2026-05-23 | (không) |
| owner.test@test.vn / Test@1234 đã seed | ✅ Đợt 105 | (không) |
| MailHog tại localhost:8025 chạy | ✅ verified | dùng làm SMTP giả lập cho fix cụm thư |
| 3 agent worktree fix không đụng nhau | ⚠️ verify-at-spawn | check `EmailTemplateRenderer.java` có conflict giữa FIX-657 + FIX-659 không trước spawn |
| PR #1737/#1738/#1739/#1740 gộp main | ⏳ đang mở | không chặn — Đợt 107 trên nhánh riêng |

---

## 7. Phạm vi ngoài đợt này

| Mục | Lý do |
|---|---|
| RST Mảng B-CRUD (B5-B8) | Cần seed dữ liệu nền (lớp + học viên + giáo viên); đẩy Đợt 108 |
| RST Mảng B-vận-hành (B9-B13) | Tương tự, cần dữ liệu nền |
| RST Mảng C (Nhân viên) | Cần B13 (mời nhân viên) chạy trước; đẩy Đợt 108+ |
| RST Mảng D3+D4 (Quản trị) | Đẩy Đợt 108+ |
| Cụm thư còn lại (GAP-530/531/537/538/586/587) | Phụ thuộc Resend production + AWS — chờ phục hồi |
| Cụm bảo mật (GAP-440/043/638) | Đẩy Đợt 108 sau khi Đợt 107 ship |
| Live verify hậu-AWS cho 3 gap đợt này | File follow-up `GAP-XXX-post-aws-live-verify-email-cluster-wave-107` khi reframe AC |

---

## 8. Lịch sử

- **2026-05-23 (nháp):** Soạn nháp sau khi user chốt hướng hybrid qua AskUserQuestion. Phiên 2026-05-23 đã ship 4 PR + 1 audit; Đợt 107 là phiên kế tiếp khi PR plan này gộp.

---

## 4. State-Check Evidence

Xem §3 "Bằng chứng kiểm tra trạng thái (per `audit-to-gap-pipeline.md` §2.6)" — table đã verify Docker stack + KH/KC frontend routes + owner.test credential + EmailTemplateRenderer + Tone enum + 5 template tệp + staff-invite template.

## 5. Verification Gates

| Nhánh | Lệnh kiểm thử cục bộ | CI gate |
|---|---|---|
| RST-A (anonymous KH) | `PLAYWRIGHT_BASE_URL=http://localhost:3001 pnpm exec playwright test e2e/_rst-wave-107-anonymous.spec.ts` | frontend-ci |
| RST-B (owner-onboard KC) | `PLAYWRIGHT_BASE_URL=http://localhost:3000 pnpm exec playwright test e2e/_rst-wave-107-owner-onboard.spec.ts` | frontend-ci |
| FIX-543 (email content) | `cd kitehub && ./mvnw -pl kitehub-email test -P strict-warnings` | core-ci |
| FIX-657 (email framework) | `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` | core-ci |
| FIX-659 (email tone) | `cd kitehub && ./mvnw -pl kitehub-email test -P strict-warnings` | core-ci |

## 6. Agent Spawn Pattern

Xem §5 "Tối ưu agent (chi tiết)" — 3 agent worktree song song (Agent A/B/C) cho cụm thư + main session tuần tự cho RST walk. Per `agent-background-spawn-default.md`: `run_in_background: true` mặc định. Per `feedback_parallel_agent_strategy.md`: 3 agent + worktree isolation + relative paths trong prompt.
