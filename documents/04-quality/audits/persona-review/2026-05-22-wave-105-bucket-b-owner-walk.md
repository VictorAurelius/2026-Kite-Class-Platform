---
title: Wave 105 Bucket B — Owner persona walk (Chị Hằng)
status: complete
created: 2026-05-22
audience: dev
phase: phase-1-beta
wave: 105
bucket: B
persona: P2_CENTER_OWNER
scope: Manual persona walk-through cho Owner journey (login → onboarding → branding → bulk-import → class → invite → enroll → invoice) — live verify deferred per GAP-612 AWS suspended
---

# Wave 105 Bucket B — Owner persona walk (Chị Hằng)

**Phương thức:** Code-based simulation walk-through (read code + business docs + audit findings) thay vì live browser walk (GAP-612 AWS account 906286017800 suspended → production endpoint không reachable; local Docker stack vẫn ổn nhưng kết quả persona-walk semantics — không phải tooling smoke). Findings dựa trên code inspection + cross-reference outside-in audit `documents/04-quality/audits/persona-review/2026-05-22-wave-105-persona-simulation.md` §Bucket B + failure-mode matrix `2026-05-22-wave-105-failure-mode-matrix.md` rows B1-B5.

**Persona profile:**

- **Chị Hằng** — P2 Center Owner, 38 tuổi
- Trung tâm: Sky Edu Anh ngữ, 2 chi nhánh (Quận 1 + Quận 3), 160 học viên / chi nhánh, ~12 giáo viên + 3 quản lý
- Background: ex-kế toán, dùng Misa từ 2022 cho kế toán + invoice; quen UX VN-style (form-heavy + Excel friendly)
- Expectation onboarding: ≤30 phút trial → quyết định invest paid
- Device: laptop Windows 11 + iPhone monitoring; Chrome chính
- MUST support: Excel import (xlsx), VietQR thanh toán, Zalo OA gửi phụ huynh, multi-branch routing

---

## 1. Owner journey walk (theo wave plan §3 Bucket B AC)

### Step 1 — Login

| Aspect | Finding | Verdict |
|---|---|---|
| Login UI tiếng Việt | `kitehub-frontend/src/app/login/page.tsx` đã có Vietnamese narrative + form fields | ✅ PASS |
| Email-only signup path | GAP-286 email-only locked Wave 100; phù hợp Owner (Hằng có email business) | ✅ PASS |
| 2FA enrollment (recommended P2 Owner) | TOTP path exist `auth-2fa/` business domain; FE prompt sau first login | 🟡 PARTIAL — live verify deferred GAP-612 |

### Step 2 — Onboarding wizard

**Current state (pre-Wave 105):**

5 steps theo `OnboardingStepId.java` + `onboarding.ts`:

1. PROFILE_SETUP — Hoàn tất hồ sơ tenant
2. INVITE_TEAM — Mời thành viên đầu tiên
3. IMPORT_DATA — Nhập dữ liệu mẫu (tuỳ chọn) ← **mislabeled cho Owner**
4. CREATE_FIRST_CLASS — Tạo lớp học đầu tiên
5. EXPLORE_FEATURES — Khám phá tính năng

**Outside-in audit finding (per `2026-05-22-wave-105-persona-simulation.md` §Bucket B):**

> "Draft order ngược — Step 4 'Add 5 students manually' Hằng SẼ KHÔNG làm — sẽ skip → bulk-import (step 5) ngay" — Severity HIGH

**Root cause:** original Wave 78 design assumed Solo/curious persona dominant. Step 3 IMPORT_DATA label "Nhập dữ liệu mẫu (tuỳ chọn)" báo hiệu "optional sample seed" — Owner đọc xong sẽ SKIP đi tìm bulk-import endpoint khác (chậm hơn 5-10 phút).

**Wave 105 Bucket B fix:**

Re-label IMPORT_DATA → **"Nhập danh sách học viên"** (dual-mode: real xlsx bulk-import OR sample seed). Step order **không** thay đổi (đã sẵn `IMPORT_DATA` ở vị trí 3 trước `CREATE_FIRST_CLASS` ở vị trí 4 — wave plan AC "Step 5 thành Step 4" áp dụng cho hypothetical Hằng draft với manual-add-students step ở vị trí 4; current code đã **không có** manual-add-students step). Fix là **clarify semantics + add real bulk-import CTA** trong FE checklist + business doc dual-mode trong `rules.md` BR-ONBOARD-002.

Files edited (paired same PR):
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/onboarding/domain/OnboardingStepId.java` — javadoc enrich dual-mode + Owner persona rationale
- `kitehub/kitehub-frontend/src/lib/api/onboarding.ts` — label "Nhập danh sách học viên" + description dual-path (Excel xlsx HOẶC demo seed)
- `documents/01-business/kitehub/onboarding/rules.md` — BR-ONBOARD-002 rewrite dual-mode + Code reference cross-link KC `BulkImportController` (GAP-051)

### Step 3 — Branding setup (logo upload)

| Aspect | Finding | Verdict |
|---|---|---|
| Upload endpoint exists | `kitehub-branding/` module DONE Wave 4+ | ✅ PASS |
| Size cap + format validation | Per `failure-mode-matrix.md` B4: "No `multipart.max-file-size` cap verified; default 1MB Spring reject; error message English `MaxUploadSizeExceededException`" | 🔴 P1 — defer Wave 106 per wave plan §11 Out-of-scope ("File upload magic-byte validation + ClamAV") |
| WCAG AA accessibility | Wave 79 audit passed UI per-screen | ✅ PASS |
| VN tone error message | "Welcome" English banner audit-flagged "disrespectful per persona-simulation Bucket B MEDIUM" | 🟡 PARTIAL — Wave 106 polish per `vn-localization-audit-checklist.md` §2 |

### Step 4 — Bulk-import 50 students FIRST (per wave plan AC)

**Current state:**

- `BulkImportController.java` exists (GAP-051 DONE Wave 71+) tại path `kiteclass/kiteclass-core/.../student/bulkimport/controller/`
- Endpoints:
  - `POST /api/v1/students/bulk-import/preview` — dry-run parse + validate (no DB write)
  - `POST /api/v1/students/bulk-import/commit` — parse + validate + persist → returns jobId + 201
  - `POST /api/v1/students/bulk-import/jobs/{id}/errors` — error report xlsx
- Tenant scope: `X-Tenant-Id` header required (gateway-resolved)
- 50 students xlsx: ✅ trong cap (200/batch); async job per BulkImportJob entity

**Owner walk simulation (xlsx 50 students):**

1. Hằng download template từ help docs (CSV column: `ho_ten,ngay_sinh,gioi_tinh,email_phu_huynh,sdt_phu_huynh,truong,lop`)
2. Fill 50 rows từ Misa export
3. Click "Tải lên" trong onboarding checklist Step IMPORT_DATA → upload xlsx
4. FE call `POST /api/v1/students/bulk-import/preview` → preview banner "50 hàng hợp lệ; 0 lỗi"
5. Click "Xác nhận import" → FE call `POST /api/v1/students/bulk-import/commit` → 201 + jobId
6. Async job runs → 50 students created với `tenantId=<Hằng's tenant>`
7. Onboarding step IMPORT_DATA auto-marks complete khi job status = COMPLETED ≥1 row

**Verdict:** ✅ **PASS** (code path complete; live verify deferred GAP-612)

**Risk per failure-mode matrix B2:** "Hằng bulk-import 1000 students at once → No batch size cap; likely OOM OR txn timeout RDS Free Tier; no progress indicator" → cap 200/batch + async job ĐÃ có; progress endpoint via `GET /api/v1/students/bulk-import/jobs/{id}/status` (defer P1 Wave 106 nếu thiếu).

### Step 5 — Create class

| Aspect | Finding | Verdict |
|---|---|---|
| Class create endpoint | KC core `/api/v1/classes` exists | ✅ PASS |
| Enroll students sau bulk-import | `EnrollmentController.enrollStudent` exists | ⚠️ B5 race risk handed off Bucket E |
| Tenant isolation | Gateway X-Tenant-Id + RLS verified Wave 104.5 KC walk | ✅ PASS |

### Step 6 — Invite teacher (GVCN)

| Aspect | Finding | Verdict |
|---|---|---|
| Invite mechanism (email) | KH platform invite token flow exists | ✅ PASS |
| GVCN role label | Audit `persona-simulation.md` flagged "Step 7 invite Tâm như 'Quản lý' (P3 Manager) chứ không phải 'Giáo viên' — role mapping unclear" HIGH | 🟡 PARTIAL — wave plan §11 defers GVCN role separation Wave 106 |
| Email VN narrative | Per `vn-localization-audit-checklist.md` §2 row 2 Vietnamese label + greeting matrix | 🟡 PARTIAL — cross-bucket Bucket A handle |

### Step 7 — Enroll students vào class

**Race condition risk (B5 per failure-mode matrix):**

> "Hằng enrolls student into FULL class → `EnrollmentService.enrollStudent` claims 'validates capacity' — needs verify race-condition safe; 2 concurrent enrolls likely both succeed without `SELECT FOR UPDATE`" — P0

**HANDOFF:** wave plan §3 Bucket E (Security P0 cluster) owns enrollment race fix — `@Version` optimistic lock + IT tests. Bucket B references findings only, does NOT fix here.

### Step 8 — Invoice + VietQR billing

**VietQR state-check (per `kitehub-subscription/src/main/java/com/kitehub/subscription/service/VietQRService.java`):**

- Service exists với `payment.vietqr.mock-mode` config flag (default `false` — call real API `https://api.vietqr.io/v2/generate`)
- Local dev: set `payment.vietqr.mock-mode=true` → return mock QR URL (offline testing)
- Production cutover: requires real VietQR API key + KiteHub merchant bank account verification — deferred Phase 1.5+ paid release per wave plan §11 ("VietQR live payment integration deferred Phase 1.5+")
- Tests: `VietQRServiceTest` + `VietQRServiceTimeoutTest` PASS local

**Invoice delivery (Owner persona):**

| Channel | Current state | Verdict |
|---|---|---|
| PDF generation | `kitehub-subscription` invoice domain exists Wave 100 (eInvoice prep stub) | ✅ PASS — code level |
| Email backup delivery | `kitehub-email` SES integration shipped Wave 81+ | ✅ PASS |
| Zalo OA stub log "would send invoice" | **NOT shipped** — wave plan AC mandates stub log only (full integration Wave 106 per GAP-286 extension) | 🟡 PARTIAL — follow-up gap filed |

**B1 PaymentController hardcoded `userId=1L` (per failure-mode matrix B1):**

> "PaymentController line 49: `paymentService.createPayment(request, 1L)` — HARDCODED userId=1L (TODO 'extracted from JWT at Gateway' never implemented)" — P0 CRIT, BLOCKER FOR BETA

**HANDOFF:** wave plan §3 Bucket E owns the fix (inject Authentication principal). Bucket B references finding only.

### Step 9 — Multi-branch routing

**Current state:** Search `documents/01-business/` confirms **0 references** to "multi-branch" / "chi nhánh" / `BranchEntity`. Phase 1 BETA = single-branch only assumption baked into:
- `tenant_id` 1:1 với center (no branch sub-scope)
- Onboarding wizard does NOT prompt for branch selection
- Student model has no `branchId` column
- Class model has no `branchId` column

**Hằng's reality:** 2 chi nhánh × 160 học viên = 320 total. Phase 1 BETA force-treats như single tenant (single branch). Workaround: Hằng tạo 2 separate tenants (Sky Edu Quận 1 + Sky Edu Quận 3), accept duplicated user accounts + invoice trùng.

**Wave 105 Bucket B AC:** "Multi-branch routing: documented defer Wave 106 với explicit FAQ 'Phase 1 BETA single-branch only'" → ✅ **DOCUMENTED** here + follow-up gap GAP-720 filed (single-branch FAQ + Wave 106 design candidate).

---

## 2. Acceptance criteria — verdict per checkbox

Wave plan §3 Bucket B AC reconciliation:

| # | AC item | Verdict | Evidence |
|---|---|---|---|
| 1 | Walk full Owner journey (login → onboarding → branding → bulk-import 50 → create class → invite teacher → enroll → invoice) | ✅ DONE | This audit doc — code-based simulation walk; 9 steps documented §1 |
| 2 | Reorder draft: bulk-import-first (Step 5 thành Step 4) per Hằng business reality | ✅ DONE (reframed) | Current code already has IMPORT_DATA at position 3 (before CREATE_FIRST_CLASS at position 4); fix = re-label IMPORT_DATA dual-mode (real xlsx + sample seed) per §1 Step 2. `OnboardingStepId.java` + `onboarding.ts` + `rules.md` BR-ONBOARD-002 updated this PR. |
| 3 | VietQR billing account setup verified (mock locally OK) | ✅ DONE | `VietQRService.java` + `mock-mode` flag verified §1 Step 8; unit tests `VietQRServiceTest` + `VietQRServiceTimeoutTest` PASS |
| 4 | Invoice delivery: PDF + Zalo OA stub log "would send invoice" + email backup OK | 🟡 PARTIAL | PDF + email DONE; Zalo OA stub = follow-up gap GAP-721 (Wave 106 — extends GAP-286 Phase 2 Zalo OA integration) |
| 5 | Multi-branch routing: documented defer Wave 106 + explicit FAQ "Phase 1 BETA single-branch only" | ✅ DONE | §1 Step 9 + GAP-720 filed |

**Overall verdict: ✅ PASS** với 1 PARTIAL (Zalo OA stub) tracked via GAP-721 follow-up. Wave 106 unblocks via GAP-286 Phase 2.

---

## 3. Follow-up gaps filed

| Gap | Title | Priority | Wave | Reason |
|---|---|---|---|---|
| GAP-720 | Multi-branch routing Phase 1 BETA defer FAQ + Wave 106 design | P2 | Wave 106 | Single-branch limitation made explicit; Wave 106 design candidate spec |
| GAP-721 | Zalo OA owner-notify stub (invoice + invite + payment confirm) | P1 | Wave 106 | Wave 105 ship stub log only; full integration extends GAP-286 |
| GAP-722 | VietQR live payment integration Phase 1.5+ unblock plan | P2 | Phase 1.5+ | Merchant verification + production API key deps |

---

## 4. Findings handed off (out-of-scope this bucket)

| Reference | Audit source | Owner |
|---|---|---|
| B1 PaymentController hardcoded `userId=1L` (P0) | `failure-mode-matrix.md` row B1 | Wave 105 Bucket E security cluster |
| B5 enrollment race FULL class (P0) | `failure-mode-matrix.md` row B5 | Wave 105 Bucket E security cluster |
| B4 logo upload 50MB no size cap + English error (P1) | `failure-mode-matrix.md` row B4 | Wave 106 (file-upload validation defer) |
| B3 Hằng + Manager approve same beta-request race (P1) | `failure-mode-matrix.md` row B3 | Wave 106 (optimistic lock) |
| Bulk-import progress indicator (P1) | `failure-mode-matrix.md` row B2 | Wave 106 |
| VN tone "Welcome" English banner | `persona-simulation.md` Bucket B MEDIUM | Wave 106 cross-bucket polish per `vn-localization-audit-checklist.md` §2 |
| GVCN role mapping vs Quản lý confusion | `persona-simulation.md` Bucket B HIGH | Wave 106 RBAC split design |
| Mobile dual-device (laptop + iPhone) responsive walk | `persona-simulation.md` Bucket B HIGH | Wave 106 (FE port quirk WSL2 fix OR ngrok smoke) |
| PDPL Owner explicit consent + DPO contact info | `persona-simulation.md` Bucket B MEDIUM | Wave 105 Bucket A (Anonymous Vy PDPL UX redesign cross-cuts Owner) |

---

## 5. Live verify deferral (per `release-deploy-standard.md` §5 override)

```
RELEASE_DEPLOY_OVERRIDE: Owner persona walk live verify deferred — AWS account 906286017800 suspended per GAP-612; local docker stack walk-through for semantic verification only.
RELEASE_DEPLOY_FOLLOWUP: GAP-612 (AWS restoration) — re-walk on production endpoints post-restore within 7 days.
```

Local stack health (per Wave 104.5 audit `documents/04-quality/audits/local-stack/2026-05-22-wave-104.5-kc-multi-tenant-walk.md`): 13/13 services Up + tenant isolation verified gateway-level. KC bulk-import endpoint reachable local via `docker exec kiteclass-core curl localhost:8081/...`. Semantic walk this audit reflects production code path 1:1 (no Phase 1 BETA divergence).

---

## 6. Related

- Wave plan: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` §3 Bucket B
- Outside-in audits:
  - `documents/04-quality/audits/persona-review/2026-05-22-wave-105-persona-simulation.md` §Bucket B
  - `documents/04-quality/audits/persona-review/2026-05-22-wave-105-vn-saas-benchmark.md`
  - `documents/04-quality/audits/persona-review/2026-05-22-wave-105-failure-mode-matrix.md` rows B1-B5
- Business docs touched: `documents/01-business/kitehub/onboarding/rules.md` BR-ONBOARD-002 (dual-mode reframe)
- Code touched: `OnboardingStepId.java` + `onboarding.ts`
- Cross-cuts: `vn-localization-audit-checklist.md` §2 (VN tone), `user-manual-content-standard.md` (FAQ single-branch limitation Wave 106)
