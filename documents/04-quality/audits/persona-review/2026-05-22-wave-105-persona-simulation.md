---
title: Wave 105 Persona Walk — Outside-In Persona Simulation Audit
status: complete
audience: dev
created: 2026-05-22
phase: phase-1-beta
wave: 105
gaps: []
scope: Outside-in persona simulation cho 4 personas (Anonymous / Owner / Teacher / Parent) trên Wave 105 draft plan trước khi lock scope
methodology: persona-based-business-review skill — role-play 4 personas độc lập, walk Bucket A/B/C/D, identify gaps mà inside-out scope miss
---

# Wave 105 Persona Walk — Persona Simulation Audit

## Methodology

Per `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 3 — spawned background agent role-playing 4 personas Vietnam edu SaaS với background + expectation cụ thể, walk qua draft Wave 105 Persona Walk plan trước khi lock scope.

**Personas:**

1. **Em Vy (Anonymous Prospect, 24t, mobile-first)** — scout SaaS cho em họ, iPhone Safari + Zalo + ≤2min signup expectation
2. **Chị Hằng (P2 Owner, 38t, 2 chi nhánh)** — Sky Edu 160 students/chi nhánh, dùng Misa kế toán → quen UX VN, expectation onboarding ≤30min, MUST support Excel import
3. **Anh Tâm (P3 Teacher kiêm Manager, 30t)** — Hằng hire, ex-dev, 5 class, điểm danh in-class qua phone, expectation offline-resilient
4. **Chị Linh (P4 Parent, 35t)** — mẹ HS lớp 5, mobile-only, Zalo dominant, VietQR scan, NO credit card

## Per-persona findings

### Bucket A — Em Vy (Anonymous Prospect)

| Aspect | Finding | Severity |
|---|---|---|
| Mobile expectation | iPhone Safari 375×812 — FE port quirk WSL2 BLOCK full test | CRITICAL |
| Subdomain field UX | "subdomain" confusing cho non-tech user — cần inline label + ví dụ `skyedu.kitehub.me` | HIGH |
| PDPL consent | Step 6 chưa explicit consent checkbox; Vy tick-all-skip-read = consent invalid PDPL Art 11 | HIGH |
| Missing steps | Pricing page → CTA path; ToS/PDPL pre-consent landing checkbox; Zalo OA scout flow | HIGH |
| Email vs Zalo wait | Step 4 "wait for approval email" — Vy mobile-only, check email <30%, ưu tiên Zalo/SMS | MEDIUM |
| VN tone | "Số điện thoại" mandatory + auto-format 0901 234 567; error msg VN | MEDIUM |

**Verdict Bucket A: PARTIAL** — landing/form OK conceptually, nhưng mobile + subdomain UX + PDPL consent UX = 3 friction critical.

### Bucket B — Chị Hằng (Owner)

| Aspect | Finding | Severity |
|---|---|---|
| Missing steps | Excel import TRƯỚC khi tạo class (160 students sẵn có); multi-branch setup; VietQR billing account setup; GVCN role assignment; Misa integration check | CRITICAL |
| Draft order ngược | Step 4 "Add 5 students manually" Hằng SẼ KHÔNG làm — sẽ skip → bulk-import (step 5) ngay | HIGH |
| VN tone | "Welcome" English banner = disrespectful; cần "Em chào chị Hằng" formal | MEDIUM |
| Multipart upload | Step 3 logo upload — FE port quirk untested = BLOCK; size cap + format validation untested | HIGH |
| Role mapping | Step 7 invite Tâm như "Quản lý" (P3 Manager) chứ không phải "Giáo viên" — role mapping unclear trong draft | HIGH |
| Mobile expectation | Hằng dual-device (laptop + iPhone monitoring) — dashboard step 10 mobile responsive untested | HIGH |
| Invoice delivery | Step 9 chỉ test generate, không test deliver (PDF + Zalo gửi phụ huynh) | HIGH |
| PDPL consent | Step 2 onboarding wizard không có explicit Owner consent + DPO contact info | MEDIUM |

**Verdict Bucket B: FAIL** — missing bulk-import-first + multi-branch + GVCN + VietQR setup + invoice delivery = 5 critical gaps khiến Hằng bounce trong 30min trial.

### Bucket C — Anh Tâm (Teacher kiêm Manager)

| Aspect | Finding | Severity |
|---|---|---|
| Mobile expectation | Phone primary cho attendance + grade entry — FE port quirk untested = BLOCK | CRITICAL |
| Attendance UX | Step 5 phone-in-class — UI cần 1-tap-per-student (20 hs, không thể multi-step mỗi học sinh) | HIGH |
| Dual-role test | Tâm vừa Teacher vừa Manager — RBAC scope chuyển đổi giữa 2 role untested | HIGH |
| Offline resilient | Điểm danh trong giờ học, có thể mất wifi — draft chỉ assume online | HIGH |
| Zalo notification | Sau khi điểm danh + chấm điểm → cần auto notify Zalo phụ huynh (draft missing) | MEDIUM |
| Batch grade entry | Tâm nhập 20 grades cùng lúc cho class, không từng học sinh — UX batch missing | MEDIUM |
| VN tone | "Teacher" → "Giáo viên"; "Manager" → "Quản lý"; "Assignment" → "Bài tập" | LOW |

**Verdict Bucket C: PARTIAL** — auth/2FA path verified Wave 104.5, nhưng attendance batch UX + dual-role + offline + Zalo notification = 4 gaps.

### Bucket D — Chị Linh (Parent)

| Aspect | Finding | Severity |
|---|---|---|
| Zalo OA channel | Linh NOT check email — Zalo dominant VN parent ↔ center culture; email-only path = dead-end | CRITICAL |
| Mobile-only | iPhone/Android mobile-only — FE port quirk BLOCK | CRITICAL |
| Parent-PDPL | Step 2 parent-on-behalf-of-child consent specific (data trẻ <16t khác data bản thân) — PDPL Art 11 legal risk | CRITICAL |
| Multi-child UX | Linh có 2 con — draft Step 4 single-child only; multi-child select UX missing | CRITICAL |
| VietQR vs credit card | Step 9 expectation: scan QR Zalo banking app + SMS xác nhận; NOT Stripe credit card; cash option absent | CRITICAL |
| Payment idempotency | Mạng yếu → Linh pay 2 lần; webhook race undefined | CRITICAL |
| VN tone | Greeting "Kính gửi quý phụ huynh" (very formal); step 6 "view child transcript" → "Xem điểm con em" | MEDIUM |
| Consent UX | Step 2 PDPL UX — Linh skeptical → đọc skim/gọi Hằng hỏi → drop-off cao | HIGH |

**Verdict Bucket D: FAIL** — Zalo culture conflict + email-only delivery + mobile-only + multi-child + parent-on-behalf-of-child PDPL + VietQR-not-credit-card = 6 critical gaps. Linh sẽ NOT complete flow as drafted.

## Cross-persona summary

### Top 5 critical gaps (3+ personas affected)

1. **FE port quirk WSL2 blocks mobile browser walk** — Vy/Linh mobile-only + Hằng/Tâm phone secondary → 4/4 personas mobile path BLOCK
2. **PDPL consent UX inadequate** — Vy/Hằng/Linh đụng consent step; Linh parent-on-behalf-of-child = highest legal risk PDPL Art 11
3. **Zalo OA notification path absent** — Linh sẽ NOT use email; Tâm Zalo file-share; Hằng Zalo cho phụ huynh; cross-persona Zalo dominant per `vn-localization-audit-checklist.md` §4
4. **VN tone/role label** — "Welcome / Teacher / Manager / Class" English; cross-persona disrespect risk (Hằng owner-formal expectation)
5. **VietQR + cash + idempotency payment flow** — Linh primary + Hằng test; draft mocked locally; production VietQR/MoMo + bank SMS + cash receipt option = blind spot

### Top 3 must-fix Wave 105 (trước beta)

1. **Cross-persona mobile fallback** — fix Docker host port quirk WSL2 OR explicit "mobile walk via real device + ngrok tunnel" override per `release-deploy-standard.md` §3.1; document deferral với follow-up gap
2. **PDPL consent UX redesign** — explicit checkbox + VN summary + parent-on-behalf-of-child variant Bucket D (Linh). Affects A/B/D
3. **Zalo OA notification stub** — minimum endpoint stub + log "would send Zalo" pre Wave 106 full integration; ensures Linh flow không dead-end ở email step

### Top 3 defer Wave 106+ (nice-to-have)

1. Bulk-import Excel UX (Hằng B step 5) Misa-style polish
2. Offline-resilient attendance (Tâm C step 5) service-worker queue
3. Multi-branch routing (Hằng B branch setup) full UX

## Final verdict

Draft Wave 105 scope endpoint-coverage tốt cho backend self-test (~5 controllers verified), nhưng outside-in persona walk surfaces 4 FAIL/PARTIAL verdicts trên 4 buckets với 5 cross-cutting critical gaps. Bucket D (Linh) FAIL nặng nhất do **Zalo culture + email-only + mobile-only + parent-PDPL conflict cộng dồn**. Bucket B (Hằng) FAIL do **draft order ngược business reality** (manual-then-bulk thay vì bulk-first). Buckets A/C PARTIAL còn cứu được.

**Recommendation:** bổ sung 3 must-fix vào Bucket A/B/D scope trước lock plan; explicit defer 3 nice-to-have Wave 106+ với follow-up gaps. AWS suspended + FE port quirk = blocker đôi cần override trailer per `release-deploy-standard.md` §5 + GAP-612 followup.

Per `outside-in-coverage-trigger.md` v1.1.0 §3, findings được integrate vào Wave 105 plan §1 Brainstorm Q1.
