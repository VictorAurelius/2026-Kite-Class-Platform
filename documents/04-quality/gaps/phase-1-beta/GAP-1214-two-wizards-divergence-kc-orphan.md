# GAP-1214: 2 wizard branding lệch nhau — KH 7-bước ADR-037 (canonical) vs KC 6-bước orphan + preview about:blank

**Status:** 🟡 PARTIAL (code-level DONE Wave branding-100 Đợt 3; G2 browser-walk pending)
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (branding-100 persona + failure-mode audits)
**Affects:** `kiteclass-frontend/components/branding/wizard/` (orphan) + `kitehub-frontend (customer)/branding/wizard` (canonical)

## Problem

Hai wizard tồn tại song song: KH 7 bước theo ADR-037 (mode/portrait/banner/SSE) vs KC 6 bước input-collector cũ — KC `PreviewStep.tsx:51` iframe `src="about:blank"` (deploy mù, P0 UX). Mọi fix phải làm 2 lần hoặc trôi (đúng class GAP-1208/1212).

## Proposed Fix

Chốt canonical = KH wizard (per failure-mode audit); KC route → embed/redirect sang canonical hoặc port; retire FSM orphan. Bucket B wave branding-100; kit GAP-1212 design cho bộ bước unified.

## Acceptance Criteria

- [ ] 1 wizard canonical duy nhất phục vụ cả 2 entry
- [ ] Không còn preview about:blank
- [ ] Bộ bước theo kit redesign (output-first)

## Related

- Audits persona F1/F2 + failure-mode; GAP-1212 (kit), GAP-1147/1134


## Cập nhật Đợt 3 (2026-06-12) — code-level DONE

Wave branding-100 Đợt 3 ship FE wizard reorder **output-first 7→5 bước** (GAP-1216):
1. Welcome + Mode (GenerationModeSelector vào Welcome; escape-ramp → bước 4 TEMPLATE / bước 5 FULL_AI)
2. Brand personality (gộp Audience + Tone — `BrandPersonalityStep`)
3. Assets (gộp Logo + Portrait, optional/skip; Portrait chỉ khi mode=FULL_AI — `AssetsStep` embed `LogoStep`/`PortraitStep`)
4. Template (TEMPLATE-only; FULL_AI skip qua reducer mode-aware NEXT/PREV)
5. Preview/Generate (`Step6Preview` — đọc `wizardState.mode`)

State machine `wizard-shared.tsx`: `WizardStep` 1-5, thêm `mode` + `SET_MODE`, reducer skip step 4 cho FULL_AI. `StepIndicator` 5 bước mode-aware. FAILED recovery (`DeployingStep` retry/back/errorCode) + 422 quality-gate handling + `DoneStep` landing link đã wire.

Evidence: 149 vitest PASS (KH wizard) + 3 PASS (KC) + `pnpm build` KH+KC PASS + tsc clean. **Còn:** G2 browser-walk (coordinator) + retire orphan FSM component files (GAP-1214).
