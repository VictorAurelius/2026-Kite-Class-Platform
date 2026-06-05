---
title: Session handoff 2026-06-05 — Flow Verification Campaign KC-1/KC-2 walks + KC-3 prep
audience: dev
created: 2026-06-05
scope: Bàn giao phiên walk KC-1 + KC-2 (G1 PASS + fixes) + prep KC-3
---

# Session handoff 2026-06-05 — KC-1/KC-2 walks + KC-3 prep

## Đã ship (5 PR merged → main e7400748)

| PR | Nội dung |
|---|---|
| **#2169** | KC-1 G1 walk (re-scoped branding+prefs) + GAP-979 prefs-tab fix + GAP-978/979/980 |
| **#2170** | KC-2 wave plan stub + decouple (KC-2 platform-side, độc lập KC-1) |
| **#2171** | KC-2 prep deliverable (pre-walk persona sim artifact + blocker verdict) |
| **#2172** | KC-2 G1 walk PASS — **GAP-981 STAFF tenant resolution fix** + GAP-784 DONE |
| **#2173** | KC-3 wave plan stub |

## Campaign status (§4)

| Flow | Status |
|---|---|
| KH-1 / KH-2c / KH-4 | ✅ THÔNG |
| KH-3 (SePay) | 🔄 G1 — chờ Bucket E real-transfer + G2 + G3 |
| **KC-1** (tenant settings) | 🔄 G1 PASS (branding+prefs re-scoped) — chờ G2 + G3 |
| **KC-2** (staff invite) | 🔄 G1 PASS (FM-1 fixed) — chờ G2 + G3 |
| KC-3 (academic) | ⬜ plan stub ready |

## Pending (cần con người / gated)

1. **G2 (bạn test local)** — 3 recipe sẵn sàng:
   - KC-1: `documents/05-guides/operations/2026-06-05-g2-recipe-kc1-tenant-settings.md`
   - KC-2: `documents/05-guides/operations/2026-06-05-g2-recipe-kc2-staff-invitation.md`
   - KH-3: `documents/05-guides/operations/2026-06-04-g2-recipe-kh3-subscription.md`
2. **G3** (production parity) — gated **GAP-612** (AWS account 906286017800 suspended).
3. **KC-3 walk** — next-in-chain. ⚠️ State-check: academicyear+course có controller nhưng **class/schedule controller chưa thấy** → partial-impl risk. Session-start: hardened state-check + pre-walk persona sim.

## Follow-up gaps

- **GAP-978** P1 DevOps OPEN — `build-all.sh`/`--rebuild` bỏ sót kiteclass-core/frontend → stale-image. **Fix:** thêm kiteclass vào build-all.sh.
- **GAP-886** → fold vào GAP-877 (actor-sweep UUID, không fix standalone).
- **GAP-980** P3 — tenant-config (locale/currency) defer Phase 1.5.
- **2 minor KC-2** (P3 defer): email link prod-domain `kitehub.me` (local friction) + subject "Trung tâm KiteHub" thay tên tenant.

## Bài học phiên

- **Pre-walk persona sim** dự đoán đúng bug thật (KC-1 FM-7 prefs / KC-2 FM-1 tenant) → walk hiệu quả (catalog-then-batch, 1 fix mỗi flow).
- **Stale-image trap (GAP-978):** image local cũ suýt cho false-PASS 2 lần (KC-1 V77 + KC-2). Luôn verify image date vs repo HEAD trước walk.
- **id-model UUID-vs-Long boundary:** GAP-979 (KC-1, missing ref-id Owner) + GAP-981 (KC-2, missing STAFF tenant resolution) cùng family nhưng KHÁC fix surface — KHÔNG gộp meta-fix.

## Lưu ý môi trường

- **Self-hosted CI runner OFFLINE** (máy `nguyenvankiet`) → self-hosted Quality jobs queue. 5 PR merged qua local-parity + `--admin` + `ADMIN_MERGE_OVERRIDE` trailer (GitHub-hosted gates xanh, gồm Test KiteHub Subscription Service cho #2172 Java change). Bật lại: `sudo systemctl restart actions.runner.VictorAurelius-2026-Kite-Class-Platform.kite-dev-wsl-runner.service` trên máy nguyenvankiet.
- Stack đang UP (kitehub services + kiteclass rebuilt 2026-06-05, production-equivalent).
