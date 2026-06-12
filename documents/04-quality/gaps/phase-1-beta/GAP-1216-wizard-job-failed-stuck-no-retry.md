# GAP-1216: Job FAILED → user kẹt DeployingStep — không Retry/Back

**Status:** 🟡 PARTIAL (code-level DONE Wave branding-100 Đợt 3; G2 browser-walk pending)
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (branding-100 failure-mode audit #3 + persona F7)
**Affects:** KH wizard DeployingStep

## Problem

Job FAILED chỉ append 1 dòng log đỏ — không nút Retry, không Back, không hướng dẫn. User kẹt màn deploy vô hạn (dead-end per pre-handoff §2.9 — retry/DLQ UX absent).

## Proposed Fix

FAILED state UI: lý do ngắn + Retry (re-enqueue) + Back-to-edit + liên hệ hỗ trợ. Bucket E wave branding-100.

## Acceptance Criteria

- [ ] FAILED → retry thành công được không mất input
- [ ] Back về bước trước giữ state

## Related

- Failure-mode #3; GAP-1021 (SSE), persona F7


## Cập nhật Đợt 3 (2026-06-12) — code-level DONE

Wave branding-100 Đợt 3 ship FE wizard reorder **output-first 7→5 bước** (GAP-1216):
1. Welcome + Mode (GenerationModeSelector vào Welcome; escape-ramp → bước 4 TEMPLATE / bước 5 FULL_AI)
2. Brand personality (gộp Audience + Tone — `BrandPersonalityStep`)
3. Assets (gộp Logo + Portrait, optional/skip; Portrait chỉ khi mode=FULL_AI — `AssetsStep` embed `LogoStep`/`PortraitStep`)
4. Template (TEMPLATE-only; FULL_AI skip qua reducer mode-aware NEXT/PREV)
5. Preview/Generate (`Step6Preview` — đọc `wizardState.mode`)

State machine `wizard-shared.tsx`: `WizardStep` 1-5, thêm `mode` + `SET_MODE`, reducer skip step 4 cho FULL_AI. `StepIndicator` 5 bước mode-aware. FAILED recovery (`DeployingStep` retry/back/errorCode) + 422 quality-gate handling + `DoneStep` landing link đã wire.

Evidence: 149 vitest PASS (KH wizard) + 3 PASS (KC) + `pnpm build` KH+KC PASS + tsc clean. **Còn:** G2 browser-walk (coordinator) + retire orphan FSM component files (GAP-1214).
