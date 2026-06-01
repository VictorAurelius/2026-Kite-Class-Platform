# Acceptance Tests — Per-Release Manual Walkthrough Matrices

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md) + [`.claude/rules/test-artifact-format-standard.md`](../../../../.claude/rules/test-artifact-format-standard.md) + [`.claude/rules/dev-readable-doc-language.md`](../../../../.claude/rules/dev-readable-doc-language.md)

**Last Updated:** 2026-05-17

Folder này chứa các **acceptance test matrices** cho mỗi release tag. Mỗi matrix là file CSV canonical với hàng = bước test, cột = thuộc tính (persona, hành động, kết quả mong đợi, verify, status). User mở CSV trong spreadsheet, walk qua từng row, tick status — không cần soạn dữ liệu (mọi `input_data` đã pre-fill).

---

## Phase 1 BETA workaround — Vercel direct URL trong `verify_via`

Trong Wave 87 Bucket C, cột `verify_via` đã được patch để dùng `https://kitehub.vercel.app/<path>` thay cho `https://kitehub.me/<path>` vì CF→origin proxy chain trên apex `kitehub.me` đang TIMEOUT (xem audit Wave 86 `documents/04-quality/audits/acceptance-tests/2026-05-16-wave-86-pretag-self-test-results.md` §3.2 endpoint baseline). API endpoints `api.kitehub.me` vẫn dùng CF bình thường — chỉ FE TLD bị broken.

Cột `action` + `input_data` giữ nguyên URL `kitehub.me` vì đó là URL mà user **sẽ visit** sau khi CF cutover (PR #1466 5-gate workflow); cột `verify_via` chỉ rõ Vercel direct URL = endpoint actually-working hôm nay.

**Revert plan:** sau khi PR #1466 (EIP + Cloudflare apex cutover) execute thành công ở Wave 88+, revert `verify_via` về `kitehub.me` để align cả 3 cột (`action` / `input_data` / `verify_via`).

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | Index folder này | 1 |
| `phase-1-beta-acceptance-self-test.csv` | Matrix cho Phase 1 BETA acceptance (126 rows) | 1 per release |
| `phase-1-beta-acceptance-self-test.md` | README companion giải thích cách dùng + scope coverage | 1 per matrix |
| `*.xlsx` | XLSX render từ CSV (gitignored — generate on-demand) | 0 (gitignored) |

---

## Canonical Format — CSV with Companion XLSX Render

Theo [`.claude/rules/test-artifact-format-standard.md`](../../../../.claude/rules/test-artifact-format-standard.md):

- **CSV = canonical** (git-tracked, LLM-readable, diff-able, UTF-8 với BOM)
- **XLSX = generated on-demand** (gitignored, render bằng script bên dưới — dùng cho Excel/Sheets UX)
- **Cột name English** (technical identifiers — `flow_id`, `persona`, `step_num`, etc.) cho cross-locale stability
- **Cột value Vietnamese** (`step_title`, `action`, `expected_result`, `verify_via`, `notes`) cho dev đọc tự nhiên theo [`.claude/rules/dev-readable-doc-language.md`](../../../../.claude/rules/dev-readable-doc-language.md)
- **Codes/identifiers giữ nguyên** (`flow_id`, `persona` enum, `status` enum, `blocker_gap`, sample `input_data` đã VN-friendly)

### Render XLSX

```bash
bash scripts/render-acceptance-test-xlsx.sh \
  documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv
```

Output: `phase-1-beta-acceptance-self-test.xlsx` cùng folder. Features XLSX:

- Header row in đậm + frozen pane (cuộn không mất header)
- Auto-fit column widths
- Tab name = filename không đuôi
- UTF-8 friendly (Vietnamese diacritics render đúng trong Excel/Sheets/LibreOffice)

XLSX gitignored — nếu cần share file, generate locally rồi attach vào ticket/email.

### UTF-8 BOM trên CSV

CSV ship với UTF-8 BOM (`EF BB BF` ở 3 byte đầu) để Excel detect encoding đúng. Kiểm tra:

```bash
head -c 3 documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv | od -c | head -1
# Expected: 357 273 277 (BOM = EF BB BF in octal)
```

Nếu mở CSV trong Excel mà chữ Việt thành lỗi → file thiếu BOM, prepend lại:

```bash
F=documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv
printf '\xef\xbb\xbf' > /tmp/x && cat "$F" >> /tmp/x && mv /tmp/x "$F"
```

---

## File Placement Rules

- ✅ **Belongs here:** acceptance test matrices per release (Phase 1 BETA, Phase 1.5 PAID, etc.) — exhaustive, tick-trackable, pre-filled
- ❌ **Does NOT belong here:**
  - Smoke test scripts (executable) → `scripts/smoke-*.sh`
  - Per-alert runbooks → `../runbooks/`
  - Pre-deploy setup runbooks → `documents/05-guides/deploy/` per [`.claude/rules/deployment-naming-convention.md`](../../../../.claude/rules/deployment-naming-convention.md)
  - QA report after-run → `documents/04-quality/audits/` (per audit category)
- Naming: `<release-or-phase>-<scope>.csv` + companion `<same-stem>.md` (vd `phase-1-beta-acceptance-self-test.csv` + `phase-1-beta-acceptance-self-test.md`)

---

## Archive Policy

Move sang `documents/07-archived/acceptance-tests-YYYY/` khi:
- Release đã ship + quality audit milestone đã capture trạng thái pass/fail per row
- Matrix superseded bởi matrix mới (vd Phase 1.5 PAID kế thừa Phase 1 BETA scope)
- Doc > 180 ngày kể từ release tương ứng

---

## Key Documents

- [Phase 1 BETA Acceptance Self-Test Matrix](phase-1-beta-acceptance-self-test.csv) — 126 rows, cover P1+P2 owner + admin + email-driven flows. Companion README: [`phase-1-beta-acceptance-self-test.md`](phase-1-beta-acceptance-self-test.md)
- Render script: [`scripts/render-acceptance-test-xlsx.sh`](../../../../scripts/render-acceptance-test-xlsx.sh)

---

## ⚠️ Concurrent browser session — multi-actor walkthrough

**Trạng thái (post Wave 92 Bucket B PR #1515 + Wave email-finalize-1):** JWT 2-tab collision đã được fix code-level — JWT lưu trong `sessionStorage` (per-tab native isolation) qua facade `kitehub-frontend/src/lib/auth/jwt-storage.ts`. Architecture chi tiết: [`../../../02-architecture/frontend/auth-storage.md`](../../../02-architecture/frontend/auth-storage.md). Test evidence: 17 unit tests + 3 jsdom two-tab simulation PASS local + CI.

### Live verify cho multi-actor walkthrough (mandatory pre-DONE per [GAP-599](../../../04-quality/gaps/GAP-599-jwt-tab-collide-storage-isolation.md))

Per [`.claude/rules/pre-handoff-self-test-completeness.md`](../../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.7 multi-tenant tenant-switch checklist — JWT isolation cần verify trên real browser pre-DONE flip:

1. **Test setup:** Mở **2 tab trên cùng browser** (cùng Chrome window, cùng profile) cho `https://kitehub.me`:
   - Tab A: login admin
   - Tab B: login tenant owner
2. **Verify isolation:** DevTools → Application → Storage → `sessionStorage` cho mỗi tab — `accessToken` PHẢI khác nhau (Wave 92 Bucket B sessionStorage per-tab guarantee)
3. **Cross-action test:** Switch tab A → submit admin form → DevTools Network tab → `Authorization` header dùng admin JWT (KHÔNG phải tenant JWT của tab B)
4. **Logout isolation:** Logout tab A → tab B vẫn logged in (independent session)
5. **Cross-tenant leak test:** Tab A xem admin dashboard, tab B xem tenant owner dashboard — KHÔNG có request nào trả data sai tenant context

Live verify này gated GAP-612 AWS restore (per GAP-599 Status PARTIAL). Khi AWS up, walk full 5-step matrix.

### Legacy workaround (pre Wave 92 Bucket B — DEPRECATED)

Trước Wave 92 fix dùng `localStorage` shared cross-tab — workaround buộc phải tách profile / browser. Sau Wave 92 fix, **2 tab cùng profile là test scenario hợp lệ và recommend** để verify sessionStorage isolation hoạt động. Profile-split chỉ còn cần khi muốn isolate cookies / persistent state ngoài JWT scope.

### Cleanup giữa các session walkthrough

Nếu abort flow giữa chừng và gặp `409 Conflict` khi re-submit cùng email → chạy `bash scripts/dev/self-test-reset.sh` (Wave 87 Bucket B) HOẶC chờ scheduled cleanup landing (per [GAP-600](../../../04-quality/gaps/GAP-600-beta-request-abort-cleanup.md) P1, Wave 88+).

---

## Relationship to Other Folders

| Folder | Relationship |
|--------|--------------|
| [`../`](../) (operations/) | Folder này là child của operations/; xem README cha cho ngữ cảnh ops chung |
| [`../runbooks/`](../runbooks/) | Per-alert runbooks (post-deploy recurring) — khác scope với acceptance test (per-release one-time) |
| [`../../deploy/`](../deploy/) | Pre-deploy setup runbooks — chạy trước acceptance test |
| [`../../../04-quality/audits/`](../../../04-quality/audits/) | Sau khi walk CSV, finding gop vào audit report tương ứng |
| [`../../../04-quality/gaps/`](../../../04-quality/gaps/) | Mỗi row `fail` → file gap theo [`.claude/rules/audit-to-gap-pipeline.md`](../../../../.claude/rules/audit-to-gap-pipeline.md) §3 |
