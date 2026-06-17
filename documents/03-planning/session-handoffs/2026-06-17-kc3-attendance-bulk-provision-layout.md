# Session Handoff — 2026-06-17 — KC-3 attendance enhancements + bulk-provision credential + layout fix

**Scope:** KiteClass attendance report (KC-3) enhancements, account-provisioning investigation + feature, g2walk layout/localization fix. Tất cả qua worktree → squash-merge.

## Đã ship (7 PR, all merged to main)

| PR | Gap | Nội dung |
|---|---|---|
| #2476 | GAP-1477 | `attendance.ts` envelope drift — 7 fn còn lại `.data.data` → `response.data` (BE AttendanceController trả unwrapped) + fix tsc enum trong test |
| #2477 | GAP-1479 | `kitehub/scripts/seed-attendance-demo.sh` — committed idempotent seed "Lớp Demo Báo Cáo" (12 hs + ~9 buổi) |
| #2481 | GAP-1479 | Seed dùng **owner token** mark attendance (lớp teacher-tạo có `teacherId=None` → teacher token 403 `TEACHER_NOT_IN_CLASS`) |
| #2478 | GAP-1478 | Báo cáo điểm danh: export **XLSX** (SheetJS lazy-load) + 4 tiêu chí (chi tiết/theo buổi/theo học sinh/tổng hợp); fix lockfile xlsx integrity + bundle-budget |
| #2482 | GAP-1124 / GAP-1277 | **Bulk-provision credential**: field opt-in `initialPassword` cho create-teacher / create-student / bulk-import → auto-gọi provisioning (reuse `AuthCredentialProvisioningService`). 54 test PASS |
| #2483 | (layout/l10n, no gap) | `PaymentStatusTimeline` prop `embedded` (bỏ page-chrome `max-w-3xl/mx-auto/bg` khi nhúng dashboard) + FormSelect default `'Select an option'`→`'Chọn...'` + gender "Chọn giới tính". Sweep 2 consumer (KC billing + KH fees) |
| #2464 | GAP-1469 | KH-3 QR `<Image>` unoptimize + host allowlist (**session song song kh3qr** — merged per user xác nhận) |

## Open PRs
Không có. Tất cả merged khi CI xanh.

## Pickup (việc đầu tiên next session)
- **Không có việc pending từ session này** — đều merged + verified live.
- Nếu tiếp KC-3 walk: **g2walk tenant đã có demo data** — class 28 "Lớp Demo Báo Cáo" (12 hs, ~9 buổi, ~90% chuyên cần). Human G2 walk `http://g2walk.127.0.0.1.nip.io:3000` (owner `g2walk@kite.local`/`G2walk@2026`) → Báo cáo điểm danh + xuất XLSX 4 tiêu chí + verify layout billing/students-new đã fix.
- Bulk-provision live-verified: tạo teacher/student kèm `initialPassword` → `/api/v1/tenant-auth/login` ra token.

## Background services / state (survive /clear)
- **Docker stack UP** (kite-* + kitehub-* + kiteclass-*). **kiteclass-core + kiteclass-frontend đã rebuild** session này (merged code live).
- ⚠️ **kitehub-frontend CHƯA rebuild** → #2483 fix `school-admin/fees` embedded + #2464 KH-3 QR chưa live tới khi rebuild kitehub-frontend (`bash kitehub/scripts/rebuild.sh kitehub-frontend`). Đó là KH/kh3qr surface, không phải KC task này.
- Không có background `run_in_background` task nào còn chạy.

## Known issues / deferred
- **students/new "Ngày sinh"** vẫn `mm/dd/yyyy` (native `<input type="date">` locale-driven) — defer, cần date-picker VN tùy biến.
- **GAP-1124** (teacher email-invite self-serve full-flow) + **GAP-1277** (FE student-shell: dashboard/lesson player) — vẫn PARTIAL, Phase 2. Bulk-provision BE đã ship; posture pháp lý K-12/minor-data "v1 pending counsel" không đổi.
- **2 untracked file trong main tree** (`pr-logs/PR-2398.json` + thesis PDF `K63_...CNTT1.pdf`) — pre-existing/session khác, KHÔNG phải session này; để nguyên.
- Worktree session khác active: `kite-wt-biz0` (wave-kitehub-biz-100), `kite-wt-g1walk`, `kite-wt-kh3qr` + 1 orphan `agent-a29bb0c2` (detached, husk) — không đụng (multi-session).

## Docs-sync (per session-end-context-check §4.5)
- gap-status.csv: ✅ synced (GAP-1477/1478/1479/1124/1277 cập nhật qua các PR đã merge).
- ROADMAP §Snapshot: gap-fix work, status canonical ở gap-status.csv — không thêm dòng ROADMAP riêng.
- wave-history.jsonl: N/A (không có wave).
- MEMORY.md: không entry mới.
- Session handoff: file này.
