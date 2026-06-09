# Session Handoff 2026-06-08 — KC-1 G2 walk: 9 bugs + 5 meta-rules

**Ngày:** 2026-06-08
**Branch:** `fix/v87-attendance-status-normalize-kc5` (CHƯA mở PR — next session mở PR → main, nhiều commit)
**Context khi end:** 91% (1M Opus) — end session đúng lúc per `session-end-context-check.md`

---

## 1. Bối cảnh
Bắt đầu Flow Verification Campaign **G2 (human browser walk) cho KC-1** (tenant settings). User test thật trên browser → lòi hàng loạt bug mà G1/G3 curl-walk bỏ sót (đúng lỗ hổng curl≠browser). Owner test: **`owner@skyedu.vn` / `SkyEdu@2026`** (tenant sky-education, instance `e8ff87e1`).

## 2. Bug đã xử lý (GAP-1066 → 1074)

| GAP | Mô tả | Status |
|---|---|---|
| 1066 | V87 migration crash-loop kiteclass-core (attendance status lowercase chưa normalize) | ✅ Fixed (V87 + UPPER normalize; core healthy V87→V94) |
| 1067 | ERR_EMPTY_RESPONSE :3000 = stale docker-proxy sau compose-up (KHÔNG phải SSR) | 🟡 Workaround (restart frontend); root-fix ops defer |
| 1068 | OWNER browser 400 = wrong-credential (owner@skyedu.vn theo instances.owner_id, KHÔNG owner.sky@test.vn) | 🟢→P3 reframed (code Wave 104 đúng; residual seed link drift) |
| 1069 | dashboard classes/invoices 404 (BE thiếu flat-list) | ✅ Fixed (flat GET endpoint tenant-scoped + IT isolation PASS) |
| 1071 | (dashboard) page thiếu wrap shell → mất header/sidebar/footer | 🟡 PARTIAL: settings + 7 page FIX, 3 EXEMPT; **root-fix move-shell→layout SPLIT-defer** |
| 1072 | logo presigned URL hết hạn (BE lưu URL thay vì regen) | 🟡 BE regen-on-read shipped + verified fresh; browser preview chờ confirm |
| 1073 | upload logo fail browser (apiClient default Content-Type json phá multipart) | 🟡 Fixed + **BROWSER-CONFIRMED PASS** (user); residual sweep kitehub-frontend |
| 1074 | session per-tab (mở tab mới = login lại; GAP-830 sessionStorage) | 🔄 **IN-FLIGHT** — xem §4 |

## 3. Meta-rules thêm (incident-to-rule từ chính G2 này)
- **`g1-browser-walk-before-flip.md`** v1.0.0 — browser-real walk BẮT BUỘC trước G1 PASS cho FE flow (curl≠browser che bug)
- **`small-gap-inline-fix.md`** v1.0.0 — gap NHỎ (≤30p, in-scope, low-risk, verify-now) → fix inline + DONE cùng session; lớn → defer; cả 2 → SPLIT
- **`cross-flow-bug-class-sweep.md`** v1.1.0 §4.1 — statically-detectable class → MUST build persistent CI detector (không grep 1 lần)
- **`flow-verification-campaign.md`** §MODE refine + **§4.5 G2-bug feedback loop** (blast-radius → re-run matrix) + Bước 6 browser-confirm clause (visual bug re-walk PHẢI browser, không curl)
- **2 CI detector wired** (`quality-code.yml` WARN-mode): `fe-be-api-contract` (GAP-1070) + `dashboard-shell-wrapper` (GAP-1071) → giờ check mọi flow tự động

## 4. ⚠️ IN-FLIGHT — Option B session (GAP-1074)
User chốt **Option B** (localStorage scoped theo tenant — cross-tab + isolated). Opus agent đang chạy trong worktree:
- **Worktree:** `.claude/worktrees/agent-af80300f278ac070e` (branch `worktree-agent-af80300f278ac070e`)
- **Files đang sửa (uncommitted):** `jwt-storage.ts`, `api-client.ts`, `useAuth.ts`, `student-register-form.tsx`, `branding/wizard/page.tsx`
- **⚠️ Lưu ý:** agent base có upload-fix (commit 00293e48) + cũng sửa api-client.ts → next session integrate CẨN THẬN (merge, đừng clobber upload fix).
- **Next session:** (a) check agent đã xong chưa (`git -C <worktree> status`); nếu xong → cherry-pick/copy + **verify 2-tenant isolation KHÔNG leak** (OWASP A01) + rebuild FE; (b) nếu chưa/lỗi → re-run GAP-1074 Option B.

## 5. Stack state
- kiteclass-core: rebuilt với V87(1066) + GAP-1069 flat-list + GAP-1072 regen. Healthy.
- kiteclass-frontend: rebuilt với settings shell(1071) + upload fix(1073). **7 shell pages (reports/overview/branding/payroll/...) CHƯA rebuild** (trong tree, batch với Option B rebuild).
- Redis: đã bust `branding-by-tenant::e8ff87e1` cache 1 lần (logo fresh).

## 6. Next session TODO
1. Integrate Option B (GAP-1074) từ worktree + verify isolation + rebuild FE
2. Rebuild FE gồm 7 shell pages → re-walk (browser) các flow KH/KC khác xác nhận shell
3. Mở PR `fix/v87-attendance-status-normalize-kc5` → main (CI: 2 detector mới WARN-mode)
4. KC-1 G2: login✅ + layout✅ + field-update✅ + upload✅ confirmed; còn session-UX (1074) → flip KC-1 G2 khi 1074 xong
5. Residual sweeps: GAP-1073 kitehub-frontend api-client; GAP-1071 root-fix move-shell; GAP-1068 seed link
