# Failure-Mode Matrix — Wave candidate `branding-100` (AI Branding Wizard)

**Ngày:** 2026-06-11
**Phương pháp:** Ma trận 3 trục — STATE × TIER × MODE (read-only code audit, design-first per `design-first-investigation-order.md`)
**Scope:** chuỗi end-to-end wizard → generate (job) → preview → approve/deploy → landing render
**Auditor:** failure-mode-matrix agent (read-only)
**Design source:** ADR-037 (+ Amendment 2026-06-10), `ai-branding-guidelines.md` §2.4/§4.2/§5/§6
**Wizard canonical:** `kitehub-frontend` `(customer)/branding/wizard` (7-bước) — KHÔNG phải KC FSM 6-bước

---

## 0. Bối cảnh kiến trúc (design vs reality)

Tồn tại **HAI wizard** — đây là nguồn nhầm lẫn lớn:

| Wizard | Vị trí | Submit tới | Trạng thái |
|---|---|---|---|
| **KH wizard (canonical)** | `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` (7 bước: Welcome→Logo→Portrait→Audience→Tone→Template→Step6Preview) | `POST /api/v1/branding/jobs` + `/approve` + SSE `deploy-stream` | LIVE — branding-100 scope |
| **KC FSM wizard (legacy)** | `kiteclass-frontend/.../branding/wizard/BrandingWizard.tsx` (6 bước FSM, `wizard-machine.ts`) | `POST /api/v1/instances` (`branding-wizard-api.ts:33`) | ORPHAN — submit endpoint khác hẳn, không qua job/SSE/banner-preview |

**Reality chốt (đọc code):** deploy của KH wizard là **MOCK toàn bộ** (`MockProvisioningService`). Nó:
- Drive `BrandingJob.status` QUEUED→PROCESSING→COMPLETED + lifecycle GENERATING→DEPLOYED;
- Ghi `BrandingAsset[]` JSON **lên chính BrandingJob entity** (mock "active branding") + lifecycle marker `deploy-completed`;
- KHÔNG ghi gì vào nguồn-sự-thật landing của KC-core (`BrandingResourceRepository` / `FrontendInstance`), KHÔNG publish event cross-service (grep `convertAndSend|publishEvent` trong `wizard/` + `BrandingJobService` = **rỗng**);
- `frontendUrl = https://{slug}.kiteclass.vn` là placeholder, không DNS/render thật (GAP-1055 / GAP-811 / GAP-1077).

→ **Hệ quả nền tảng: "apply/deploy xong → landing KHÔNG đổi".** Branding mới chỉ xuất hiện ở **preview iframe client-side** (`buildLandingPreviewHtml`), không bao giờ ở landing thật.

---

## 1. Ma trận STATE × TIER × MODE

Cột: **F-T** = FREE/BASIC + TEMPLATE · **F-AI** = FREE/BASIC + FULL_AI(yêu cầu) · **P-T** = PREMIUM/ENTERPRISE + TEMPLATE · **P-AI** = PREMIUM/ENTERPRISE + FULL_AI.
Ô: ✅ OK · ⚠️ degrade-có-chủ-đích/mislead · ❌ vỡ (chức năng không đạt design intent).

| STATE | F-T | F-AI | P-T | P-AI |
|---|:--:|:--:|:--:|:--:|
| **1. Wizard step 1-6 (nav, input)** | ✅ | n/a | ✅ | n/a |
| **2. Job-create (vào Step7, `POST /jobs`)** | ✅ | ✅ | ✅ | ✅ |
| **3. Live preview banner (`preview-banner`)** | ✅ | ⚠️ | ✅ | ⚠️ |
| **4. GENERATING / SSE deploy-stream** | ✅ | ✅ | ✅ | ✅ |
| **5. FULL_AI on-demand ("tạo bằng AI cao cấp")** | ⚠️ khoá | ⚠️ fallback | ⚠️ mock-pixels | ❌ mislead |
| **6. Regenerate ("Tạo lại") mid-wizard** | ❌ | ❌ | ❌ | ❌ |
| **7. Quota regenerate FREE cạn giữa chừng** | ⚠️ | ⚠️ | n/a | n/a |
| **8. DEPLOYED → landing render thật** | ❌ | ❌ | ❌ | ❌ |
| **9. Preview vs landing (WYSIWYG)** | ❌ | ❌ | ❌ | ❌ |
| **10. Job FAILED → user recovery** | ❌ | ❌ | ❌ | ❌ |
| **11. Quality gate §5 (≥70) trước DEPLOYED** | ❌ | ❌ | ❌ | ❌ |
| **12. Post-deploy /branding link landing** | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

**Đếm:** tổng ô đáng kể = **42** (12 state × 3.5 cột trung bình, loại n/a). Ô ❌ vỡ = **23**; ⚠️ degrade/mislead = **11**; ✅ = **8**.

---

## 2. Per-cell verdict (cells đáng kể)

### STATE 8 — DEPLOYED → landing render thật ❌❌❌❌ (4/4 vỡ — TRỌNG TÂM)
- **Vỡ gì:** `MockProvisioningService.provisionAsync` chỉ ghi `BrandingAsset[]` lên `BrandingJob` + marker; KHÔNG propagate sang `kiteclass-core` `BrandingResourceRepository`/`FrontendInstance` (nguồn KC landing đọc qua `BrandingPackageServiceImpl`). Không event cross-service.
- **User thấy:** deploy-stream 100% "Đã triển khai", redirect `/branding`, nhưng truy cập landing → branding cũ/seed (hoặc 404 subdomain). Mọi tier/mode giống nhau.
- **Recover:** không — không có đường nào khiến landing đổi từ wizard ở Phase 1.
- **Gap cover:** GAP-1055 (real infra) + GAP-811/1077 (subdomain render) lân cận, nhưng **"approve→persist active theme KC-core + propagate"** chính là GAP-1021 pt1 (vẫn 🔵 OPEN, AC1 chưa đạt: "Job COMPLETED → approve → instance active theme = job assets"). Mock hiện ghi lên job, KHÔNG lên instance theme.

### STATE 9 — Preview ≠ landing thật (WYSIWYG drift) ❌×4
- **Vỡ gì:** preview iframe = `buildLandingPreviewHtml` (client-side, standard wave-landing-100) + banner TEMPLATE WebP; deploy mock không render landing đó. "What you see" (preview) ≠ "what you get" (landing không đổi). 2 nguồn render độc lập → drift cố hữu.
- **User thấy:** xác nhận một preview đẹp, deploy, landing thật khác hẳn.
- **Gap cover:** **CHƯA** có gap riêng cho "preview-source ≠ deploy-source contract".

### STATE 10 — Job FAILED → recovery ❌×4
- **Vỡ gì:** `MockProvisioning` catch → `markJobFailed` → SSE emit `error`(JOB_FAILED). FE `Step6Preview` chỉ map error event thành 1 dòng log đỏ trong `DeployingStep` (`eventsToLogEntries`); **không có nút Retry/Back, không thoát khỏi `isDeploying`**. `deployStream` đóng. → user kẹt màn "đang triển khai" với 1 log lỗi, phải reload trang.
- **User thấy:** màn deploy treo + dòng "Lỗi triển khai (JOB_FAILED)", không lối ra.
- **Gap cover:** **CHƯA** (GAP-1105 chỉ xử spurious STREAM_DISCONNECTED, không xử FAILED-state UX).

### STATE 11 — Quality gate §5 trước DEPLOYED ❌×4
- **Vỡ gì:** `MockProvisioningService` đi thẳng PROCESSING→COMPLETED, **không gọi quality gate** (grep `quality/score/§5` = rỗng). Vi phạm `ai-branding-guidelines.md` §5 + GAP-1021 AC3 ("Quality gate §5 chạy trước DEPLOYED, score ≥70").
- **Gap cover:** GAP-1021 AC3 (OPEN) — chưa thực thi; không tracked riêng.

### STATE 5 — FULL_AI on-demand (GAP-1147) ⚠️/❌
- **F-T/F-AI:** FREE chọn FULL_AI → BE gate `forTier`→TEMPLATE + `fallbackReason=TIER_NOT_ELIGIBLE`, toast "gói chưa hỗ trợ". ✅ không bypass (gate server-side) nhưng nút FE chỉ hiện khi `fullAiEligible` → FREE không thấy nút (⚠️ chấp nhận được).
- **P-AI:** PREMIUM/ENTERPRISE bấm "Tạo bằng AI cao cấp (tốn 1 lượt)" → `FullAiQuotaService.recordFullAiUsage` **trừ quota thật** + toast "Đã tạo banner bằng AI cao cấp — đã trừ 1 lượt", NHƯNG render = **TEMPLATE pixels y hệt** (GAP-1147 PARTIAL, real GPT = GAP-1135). → **user trả 1 lượt nhận banner giống TEMPLATE** = ❌ mislead, rủi ro Luật Quảng cáo (tính phí cho thứ không khác biệt).
- **Gap cover:** GAP-1147 (PARTIAL) biết "render=mock", nhưng **khía cạnh consumer-trust "trừ lượt + toast khẳng định mà output y hệt free"** CHƯA flag riêng. PREMIUM quota cạn → `QUOTA_EXHAUSTED` fallback TEMPLATE ✅.

### STATE 6 — Regenerate mid-wizard ❌×4
- **Vỡ gì:** job mid-wizard là mock QUEUED/INITIALIZING (chưa DEPLOYED) → `regenerate` trả 409 INVALID_JOB_STATE (hoặc 400 MISSING_INSTANCE_ID khi `wizardState.instanceId` null). FE nuốt lỗi → toast "Bản xem trước tự cập nhật... Tạo lại khả dụng sau khi triển khai".
- **User thấy:** nút "Tạo lại" + counter `used/limit` hiển thị như dùng được, nhưng **luôn no-op** mid-wizard. Counter gây hiểu nhầm đã/đang tiêu lượt.
- **Gap cover:** GAP-1145 (regenerate 400/409) biết; nhưng **"counter hiển thị quota dùng-được trong khi regenerate luôn no-op"** = UX-mislead CHƯA flag.

### STATE 7 — FREE quota regenerate cạn ⚠️
- FREE limit=3 (server). Cạn → `quotaExceeded` → upsell modal (`RegenerateCounter`). ✅ có chặn + upsell. NHƯNG vì STATE 6 regenerate mid-wizard luôn no-op, quota gần như không bao giờ thực-tiêu mid-wizard → modal chủ yếu kích hoạt bởi quota seed/khác. ⚠️ logic đúng nhưng path thực-thi mờ.

### STATE 3 — Live preview banner ⚠️ (F-AI/P-AI)
- TEMPLATE preview luôn chạy on-mount (`generateBannerPreview(previewBannerReq)`), không trừ quota ✅. Preview KHÔNG bao giờ render FULL_AI (cố ý). → user chọn FULL_AI mode ở selector nhưng preview vẫn TEMPLATE cho tới khi bấm nút riêng → ⚠️ nhập nhằng mode hiển thị.
- GAP-1160 (portrait inline data-URI) PARTIAL: banner trong-container fetch presigned `localhost:9100` fail → đã fix bằng `inlineImageDataUri`, pending G2 visual confirm.

### STATE 4 — SSE deploy-stream ✅ (GAP-1021 pt2 + GAP-1105 đã ship)
- Token-in-query `?token=` cho EventSource (`useDeployStream:78-84`) — gateway accept token-in-query ✅. Reconnect: terminal-event-then-native-error nuốt đúng (GAP-1105) ✅. Heartbeat 30s + backpressure cap 20 emitters/job (`DeployStreamController`) ✅. Per pre-handoff §2.8: protocol upgrade OK, keepalive OK, **auth-on-reconnect dùng lại cùng `?token=` — token hết hạn giữa stream sẽ 401 reconnect (chưa refresh)** ⚠️ edge nhỏ.

### STATE 12 — Post-deploy /branding link landing ⚠️
- GAP-1108 (80% PARTIAL): deploy-status card + "Xem landing" (`frontendUrl`) + success toast **DONE-in-code**, runtime-walk pending. Nhưng `frontendUrl` = placeholder `{slug}.kiteclass.vn` không resolve local/chưa-DNS → link 404 (cùng lớp STATE 8). ⚠️ link hiện ra nhưng dẫn tới landing không-tồn-tại/không-đổi.

---

## 3. Finding CHƯA có gap (đề xuất gap mới — coordinator file)

1. **[P0] Deploy không propagate theme → KC-core landing source-of-truth.** `MockProvisioningService` ghi assets lên `BrandingJob`, không vào `kiteclass-core BrandingResourceRepository`/`FrontendInstance`; không event cross-service → landing thật không bao giờ phản ánh wizard. (GAP-1021 pt1 OPEN chạm phần "approve persist", nhưng cross-service propagation KH-branding→KC-core là mảnh thiếu rõ ràng; tách rõ khỏi GAP-1055 infra.)
2. **[P1] Preview-source ≠ deploy-source (WYSIWYG broken).** Preview = `buildLandingPreviewHtml` client-side; deploy = mock không render landing đó. Cần contract "preview render = deploy render" hoặc disclaimer rõ "bản xem trước, landing thật cập nhật sau".
3. **[P1] Job FAILED → user kẹt màn deploy, không Retry/Back.** `Step6Preview` không thoát `isDeploying` khi SSE `error`; cần nút thử lại / quay về Step7.
4. **[P1] Quality gate §5 (≥70) không chạy trước DEPLOYED.** Mock bỏ qua gate; vi phạm guidelines §5 + GAP-1021 AC3. Cần wire gate (hoặc tracked-defer rõ ràng).
5. **[P2] FULL_AI trừ quota + toast "đã tạo AI cao cấp" nhưng render TEMPLATE y hệt** (consumer-trust / Luật Quảng cáo). Tách khỏi GAP-1147 (vốn chỉ flag "render=mock"): khi chưa có real GPT, **không nên trừ lượt + không nên toast khẳng định** — nên gate nút FULL_AI sau key/real-render sẵn sàng.
6. **[P2, bonus] Regenerate counter hiển thị quota dùng-được trong khi regenerate mid-wizard luôn no-op** → ẩn nút/đổi label cho tới sau deploy.
7. **[P2, bonus] Two-wizard divergence** — KC FSM wizard (`/branding/wizard` → `POST /api/v1/instances`) orphan, endpoint khác hẳn KH wizard. Quyết định archive/redirect để tránh nhầm + route chết.

---

## 4. Thứ tự fix đề xuất cho wave branding-100

1. **Finding #1 (P0)** — propagate approve→KC-core active theme (đường để landing thật đổi). Đây là điều kiện tiên quyết: không có nó, mọi cell STATE 8/9/12 vẫn vỡ. Nếu Phase 1 chốt mock-only thì PHẢI re-label deploy "(mô phỏng)" + disclaimer, không gọi "Đã triển khai".
2. **Finding #4 (P1)** — wire quality gate §5 trước DEPLOYED (rẻ, đạt GAP-1021 AC3, chặn deploy theme rác).
3. **Finding #3 (P1)** — FAILED recovery UX (Retry/Back) — chặn user kẹt, độc lập, nhỏ.
4. **Finding #2 (P1)** — preview/deploy WYSIWYG contract HOẶC disclaimer (đi cùng #1).
5. **Finding #5 (P2)** — gate nút FULL_AI sau real-render/GAP-1135; tạm thời ngừng trừ quota + đổi toast (tránh mislead).
6. **Finding #6 + #7 (P2)** — regenerate counter label + archive KC FSM wizard (dọn nợ).

**Kết luận:** chuỗi wizard→generate→preview UI khá hoàn chỉnh (SSE/token/preview/quota-gate đều ship), nhưng **"apply→landing" về cơ bản chưa thông** (mock, không propagate, không gate, không recovery). 23/42 ô vỡ tập trung ở nửa sau chuỗi (deploy→landing). branding-100 nên ưu tiên đóng mảnh propagation + relabel/disclaimer trước khi tô điểm thêm bước preview.
