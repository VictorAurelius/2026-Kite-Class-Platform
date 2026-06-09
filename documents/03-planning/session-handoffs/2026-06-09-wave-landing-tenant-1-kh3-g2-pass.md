---
audience: dev
---

# Session Handoff — 2026-06-09 — wave/landing-tenant-1 + KH-3 G2 PASS

**Branch:** `wave/landing-tenant-1` (5 commits, clean, **CHƯA push, CHƯA PR**)
**Base:** `main` @ `cda3a40b` (PR #2274 đã merge session này: landing-100 + KC-1 G2)

---

## 1. Đã làm xong session này

### 1a. CI-ops (DONE)
- Cancel **52 stale queued runs**; mở **2 self-hosted runner** → systemd service (`~/actions-runner` + `~/actions-runner-2`, enabled+active, **persist qua reboot**). Queue 54→0.
- PR #2274 (KC-1 G2 + landing-100) → **ALL GREEN → MERGED** (`cda3a40b`). Bao gồm GAP-1084 fix (e2e auth helper tenant-scoped storage — class-lifecycle gate green).

### 1b. wave/landing-tenant-1 — 5 commit (CHƯA push)
```
4991da67 fix(kitehub): KH-3 G2 SePay — qr_code_url TEXT + QR memo==txnRef (bugs C+D)
075de5e1 fix(kitehub): KH-3 G2 SePay — beta amount override + VietQR bank env passthrough
83d64781 fix(kitehub): wire beta-payment env into compose for KH-3 G2 SePay walk
0632859d docs(kiteclass): GAP-811/1077/813 tenant-resolution state-check + AC
3e57f1c3 feat(kiteclass): GAP-1083 per-tenant centerName + Zalo CTA + F-section landing data
```
- **GAP-1083** (Agent A, Opus worktree): kiteclass-core triad (entity + Flyway **V95** + DTO + mapper + sanitizer + service inherit centerName/zaloUrl từ branding) + FE `(public)/page.tsx` wire 3 F-section slots. Verified BE compile + landing unit test + FE build. **AC3 (G1 browser walk demo-trio render) CHƯA verify** — cần rebuild kiteclass + walk.
- **GAP-811/1077/813** (Agent B): state-check — cluster đã ship đủ wave trước (middleware ở kiteclass-frontend, BE resolve endpoint `PublicTenantController` ở kitehub-subscription). FE 41/41 test PASS. Docs-only tick AC. GAP-813 còn item base-domain/SlugAvailability defer.

### 1c. KH-3 G2 SePay walk → **PASS** (chuyển khoản THẬT)
- Owner Tuấn `g2test-an-8@example.com` / `WalkKh3@2026` (tenant **test-8**, không phải `g2test-an-8`). Password reset session này.
- Walk thật: nâng cấp PREMIUM → QR 10k MB `0988269432` → chuyển khoản thật → SePay webhook (txn `62542238`) → **PREMIUM ACTIVE** + payment COMPLETED.
- **4 bug fixed** (SePay create-path chưa từng test e2e): A acqId-BIN, B beta-amount-10k, C qr_code_url→TEXT (V67), D QR memo==txnRef.
- Verified full chain local: POST 201 → webhook 200 → ACTIVE/BASIC (test) + PREMIUM (walk thật).

---

## 2. Việc CHƯA làm — pickup session sau

| # | Task | Note |
|---|---|---|
| 1 | **Push + mở PR** `wave/landing-tenant-1` → main | gom landing-100 BE + tenant-res + KH-3 SePay 4 fixes + compose/tunnel config |
| 2 | **File gaps KH-3** A/B/C/D (DONE, discovery) + **E/F (OPEN)** | per `discovery-to-gap-inline-filing.md` — chưa file gap nào cho A-F |
| 3 | **Bug E (OPEN)** — duplicate activation email | `EmailServiceClient.publishToQueue` outbox + fast-path convertAndSend → consumer nhận 2 lần (class GAP-930). Cần idempotency-key consumer hoặc bỏ 1 path |
| 4 | **Bug F (OPEN)** — email content sai | (a) subject `"Subscription đã kích hoạt"` cho type `subscription-created` (sai wording + "Subscription" tiếng Anh vi phạm VN-localization); (b) template `subscription-created.txt` THIẾU → no text part; (c) `dashboardUrl="https://kitehub.vn/dashboard"` sai domain (phải kitehub.me) — `EmailServiceClient:585-592` |
| 5 | **Follow-up Bug D** — `PaymentService.createPayment` (upgrade path) cùng class memo!=txnRef | overload UUID cũ giữ nguyên → upgrade path vẫn broken; file gap |
| 6 | **GAP-1083 AC3** G1 browser walk demo-trio | rebuild kiteclass-core+frontend (V95) + walk co-ha-toan/thay-nhi-hoa render per-tenant F-section |
| 7 | **Flip gaps CSV** + **campaign §4 KH-3** → G2 PASS | GAP-1083/811/1077 + KH-3 row |

---

## 3. State ephemeral (lưu ý)
- **cloudflared tunnel** `https://mechanisms-casio-bizarre-organisms.trycloudflare.com` (→ :9000) chạy qua background task của session — **CHẾT khi session kết thúc**. Walk KH-3 lại cần dựng lại: `~/.local/bin/cloudflared tunnel --url http://localhost:9000` (cloudflared đã cài).
- **tenant test-8** giờ **PREMIUM/ACTIVE** (kết quả walk thật) — reset về FREE/TRIAL nếu cần walk KH-3 lại: `DELETE payments/subscriptions WHERE instance_id='7862ab7e-...'; UPDATE instances SET tier='FREE',status='TRIAL',subscription_id=NULL`.
- **email-sent-log** + MailHog có email test (BASIC) của coordinator → email walk PREMIUM thật của user bị dedup-skip (`alreadySentToday`). Clear nếu cần verify email sạch.
- **VietQR bank env** (MB 970422 / acct user) chỉ ở shell env lúc rebuild — KHÔNG commit (compose giữ generic default). Rebuild lại cần re-export.
- Local Docker stack: 13 container healthy (3h+); kitehub-subscription/frontend rebuilt với SePay env.
