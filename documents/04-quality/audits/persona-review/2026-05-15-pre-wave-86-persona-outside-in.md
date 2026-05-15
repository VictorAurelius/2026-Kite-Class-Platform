---
title: Pre-Wave 86 Persona Outside-In Audit — v1.0.0-rc.1 Tag Preflight + Beta Cohort Invite
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 86
gaps: [GAP-440, GAP-537c, GAP-412, GAP-372, GAP-373, GAP-377, GAP-378]
---

# Pre-Wave 86 Persona Outside-In Audit

## 1. Scope

Audit outside-in per `outside-in-coverage-trigger.md` §3 Bước 2 (Bucket A của Wave 86 plan). Wave 86 = 8 buckets:
- A Outside-in audit (artifact này — 1 trong 3 song song: persona / benchmark / simulation)
- B GAP-440 Spring Boot dep bump
- C GAP-537c P2/P3 screenshots + Tier 2 annotation
- D GAP-412 AWS Activate $1k resubmit
- E Pre-launch hardening 5 checklist verification
- F Tag `v1.0.0-rc.1` + release CI
- G Beta cohort invite 5 tenants (P1×2 + P2×3)
- H Post-cohort monitoring + incident response

Audit scope = 5 personas × 5 question dimensions = **25 cells**, focus first-30-minute experience (landing → signup → onboarding → first daily ops). Mục đích surface gaps mà inside-out Wave 86 plan miss khi cohort invite irreversible.

## 2. Methodology

Skill: `.claude/skills/quality/persona-based-business-review/SKILL.md` (role-play 5 persona types → tìm gaps).

**Personas:**
1. **Anonymous Prospect (Vy)** — sinh viên sư phạm/giáo viên freelance đến từ Google search "phần mềm quản lý lớp học" hoặc Facebook ad → landing → cân nhắc convert
2. **P1 Solo Teacher (chị Hồng)** — giáo viên freelance Anh ngữ, invited qua email từ waitlist, first login + setup lớp đầu tiên ≤30 học sinh
3. **P2 Center Owner (chị Hằng)** — chủ trung tâm Anh ngữ Sky Education (~100 học sinh, 3 staff), invited qua manual approval, onboarding wizard + invite first P3 manager
4. **P3 Manager (anh Tâm)** — quản lý vận hành cho chị Hằng, invited bởi P2, first daily ops (điểm danh + nhập điểm) + permissions feel
5. **Platform Admin (Mai)** — internal KiteHub admin, first beta-cohort approval review + audit log inspection cho 5 tenants

**5 question dimensions** focus first-30-min:
1. **First impression** — Loading time, hero clarity, CTA visibility — block trong 30 giây?
2. **Confidence signals** — Trust gates (HTTPS, branding, social proof, legal links, beta disclaimer) — cảm thấy an toàn?
3. **Cognitive load** — Overwhelm vì terms / wizard steps / unfamiliar concepts?
4. **First success path** — Complete được core task (signup confirm / first class / first approval) trong 30 phút?
5. **Failure recovery** — Nếu gặp error (typo email / 2FA fail / network glitch), path recovery rõ?

Per `dev-readable-doc-language.md` narrative Vietnamese; technical token English.

---

## 3. Matrix 5×5

### 3.1 Anonymous Prospect (Vy)

| # | Dimension | (a) Mong gì | (b) Wave 86 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 1.1 | First impression | Landing load <3s mobile 3G; hero hiểu được "đây là gì cho ai" trong 5 giây; CTA "Yêu cầu truy cập Beta" rõ | **Partial** — Wave 84 Bucket H deploy live nhưng KHÔNG có target latency P95 mobile-3G cho landing | ⚠️ Bucket E add AC: landing P95 <3s mobile-3G (Lighthouse mobile profile) |
| 1.2 | Confidence signals | HTTPS lock + brand consistent + có "Đang trong giai đoạn Beta" disclaimer rõ + legal links visible footer | **Match** — Wave 83 stacktrace fix + PDPL opt-in shipped; Wave 84 deploy live | ✅ existing — verify Bucket G welcome page có beta disclaimer explicit |
| 1.3 | Cognitive load | KHÔNG bị bắt đăng ký ngay; có "Xem demo" hoặc "Tìm hiểu giá" trước commit; signup form ≤5 fields | **Miss** — Wave 86 không cover signup form audit / demo entry / pricing transparency | 🚨 **NEW gap proposal:** GAP-XXX P1 "Landing CTA hierarchy + demo entry path before signup commit" (gate Phase 1 beta conversion rate) |
| 1.4 | First success path | Submit beta request form → kỳ vọng confirmation email <2 min + "chờ duyệt 2-3 ngày" expectation set | **Partial** — Bucket G manual invite flow exists; KHÔNG có auto-acknowledgement email cho waitlist signup | ⚠️ Bucket G add AC: waitlist signup auto-ACK email <2min + SLA expectation 2-3 ngày trong email body |
| 1.5 | Failure recovery | Email typo → reset; form validation inline; nếu submit fail thì giữ data | **Miss** — Wave 86 không cover landing form UX hardening | ⚠️ Defer Wave 87 — **NEW gap proposal:** GAP-XXX P2 "Landing form inline validation + draft preservation" |

### 3.2 P1 Solo Teacher (chị Hồng)

| # | Dimension | (a) Mong gì | (b) Wave 86 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 2.1 | First impression | Invite email landing inbox <1h sau approval; click link → login page sạch + ngắn | **Partial** — Bucket G Resend production verified; KHÔNG có target email delivery time SLA explicit | ⚠️ Bucket H add AC: invite email delivery P95 <5 phút post-trigger (Resend dashboard metric) |
| 2.2 | Confidence signals | Email từ `support@kitehub.me` không phải `noreply@`; có tên thật người duyệt; có "Beta — phản hồi qua ..." | **Miss** — Wave 86 không cover invite email template content review (tone, sender identity, feedback CTA) | 🚨 **NEW gap proposal:** GAP-XXX P1 "Beta invite email template content audit — tone + sender + feedback CTA" (paired với Bucket G) |
| 2.3 | Cognitive load | First login → KHÔNG bắt setup hết lớp + học sinh + lịch học cùng lúc; có "Tạo lớp đầu tiên" step-by-step <5 phút | **Match** — GAP-537c P1 screenshots scope includes first-class onboarding | ✅ existing — Bucket C verify P1 flow ≤5 phút first-class |
| 2.4 | First success path | Tạo 1 lớp + thêm 5 học sinh + điểm danh buổi đầu trong 30 phút | **Partial** — Wave 85 multi-tenant + RLS shipped; KHÔNG có explicit 30-min benchmark cho P1 cold-start | ⚠️ Bucket H add AC: P1 cold-start first-class + first-attendance <30min (validate qua 1 cohort tenant) |
| 2.5 | Failure recovery | Nếu typo tên học sinh / xóa nhầm lớp → undo path; password forgot → reset email <5min | **Miss** — Wave 86 không cover soft-delete restore (was Wave 85 audit NEW gap proposal — chưa filed) | 🚨 **NEW gap proposal:** GAP-XXX P1 "Soft-delete + 7-day restore window for classes/students/attendance" (Wave 85 audit carry-forward, blocks Phase 2 GA) |

### 3.3 P2 Center Owner (chị Hằng)

| # | Dimension | (a) Mong gì | (b) Wave 86 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 3.1 | First impression | Manual approval call/email từ admin trước invite — "chị Mai từ KiteHub đã gọi xác nhận"; sau invite → landing dashboard có "Xin chào chị Hằng" personalized | **Partial** — Bucket G manual approval flow; KHÔNG có personalized dashboard greeting check | ⚠️ Bucket G add AC: P2 first-login dashboard greeting personalized + admin contact visible |
| 3.2 | Confidence signals | Thấy "Đang trong Beta + 5 trung tâm đầu tiên" social proof; PDPL opt-in visible; có "Hỗ trợ Zalo" / hotline cụ thể | **Partial** — Wave 83 PDPL opt-in shipped; KHÔNG có "5 beta tenants" social proof + Zalo OA chưa active (per `user-manual-content-standard.md` §2 row 5 defer Phase 1.5+) | ⚠️ Bucket G add AC: dashboard banner cite "Bạn là 1 trong 5 trung tâm đầu tiên" + support email visible header |
| 3.3 | Cognitive load | Onboarding wizard ≤7 bước (thông tin trung tâm → branding → invite manager → ...); skip-and-resume option | **Miss** — Wave 86 không cover onboarding wizard audit cho P2; GAP-537c chỉ capture screenshots, không validate cognitive load | 🚨 **NEW gap proposal:** GAP-XXX P1 "P2 onboarding wizard step-count audit + skip-and-resume UX" (paired Bucket C scope expand) |
| 3.4 | First success path | Hoàn tất onboarding + invite P3 manager + tạo 1 lớp demo trong 30 phút; nhận confirmation summary email | **Partial** — Bucket G invite mechanism; KHÔNG có "summary email" sau onboarding + 30-min benchmark | ⚠️ Bucket G add AC: P2 onboarding-complete trigger summary email + 30-min cold-start benchmark validate |
| 3.5 | Failure recovery | Invite manager email typo → resend with edit; nếu Manager không xác nhận trong 24h → P2 nhận reminder; revoke invite path | **Miss** — Wave 86 không cover invite management UX (resend / revoke / reminder) | ⚠️ Defer Wave 87 — **NEW gap proposal:** GAP-XXX P2 "P2 invite management — resend / revoke / 24h reminder for unaccepted invites" |

### 3.4 P3 Manager (anh Tâm)

| # | Dimension | (a) Mong gì | (b) Wave 86 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 4.1 | First impression | Invite email từ chị Hằng (KHÔNG phải `support@kitehub.me`) → click → biết "chị Hằng mời tôi làm Manager Trung tâm Sky Education"; signup ngắn | **Miss** — Wave 86 không verify invite email từ-P2-name personalization + role context trong email body | 🚨 **NEW gap proposal:** GAP-XXX P1 "P3 invite email content audit — P2 owner name + center context + role explicit" (paired Bucket G) |
| 4.2 | Confidence signals | Biết rõ quyền hạn ngay từ đầu — "Bạn có quyền: nhập điểm, điểm danh; KHÔNG có quyền: xóa lớp, sửa giá" | **Miss** — Wave 86 không cover P3 first-login permission disclosure | ⚠️ Bucket C add AC: P3 first-login screen show permission matrix explicit |
| 4.3 | Cognitive load | Dashboard KHÔNG show admin-level menu (billing / subscription) — chỉ show daily ops view; navigation ≤5 mục | **Partial** — Wave 85 RLS shipped (data layer); KHÔNG có FE permission-guard audit cho P3 menu | ⚠️ Bucket E add AC: P3 dashboard FE menu hide admin/billing items (verify per checklist) |
| 4.4 | First success path | Điểm danh 1 lớp + nhập điểm 1 buổi trong 15 phút first-day | **Partial** — GAP-537c P3 6-screen scope; KHÔNG có 15-min benchmark | ⚠️ Bucket H add AC: P3 first-day daily-ops <15 phút benchmark validate |
| 4.5 | Failure recovery | Nếu nhập điểm sai → sửa được trong 24h; nếu lock-out → P2 owner unlock được | **Miss** — Wave 86 không cover edit-window / lock-out recovery path | ⚠️ Defer Wave 87 — **NEW gap proposal:** GAP-XXX P2 "P3 grade edit-window (24h) + P2 unlock-P3 recovery path" |

### 3.5 Platform Admin (Mai)

| # | Dimension | (a) Mong gì | (b) Wave 86 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 5.1 | First impression | Beta cohort approval queue dashboard load <2s; thấy 5 pending request rõ ràng | **Partial** — Wave 71b admin dashboard exists; KHÔNG có specific cohort approval queue UX check Wave 86 | ⚠️ Bucket H add AC: admin cohort queue load <2s + 5-row visibility verify |
| 5.2 | Confidence signals | Audit log mọi action (approve / invite / impersonate); CloudTrail visibility cho infra ops | **Match** — Wave 84 CloudTrail baseline + V52 audit_logs table shipped | ✅ existing — verify Bucket H runbook cite audit log path |
| 5.3 | Cognitive load | Approval form ngắn ≤3 fields (notes / tier / send invite?); bulk-approve N tenants 1 click | **Miss** — Wave 86 không cover admin approval form UX audit | ⚠️ Defer Wave 87 — **NEW gap proposal:** GAP-XXX P2 "Admin cohort bulk-approve UX" (small scale 5 tenants OK manual; gate khi >20 tenants Phase 2) |
| 5.4 | First success path | Approve 5 tenants + trigger invites + verify Resend delivery + monitor first-hour signup — tất cả trong 30 phút | **Partial** — Bucket G + H cover mechanism; KHÔNG có explicit admin runbook step-by-step 30-min flow | ⚠️ Bucket H add AC: admin runbook section "5-tenant first-cohort approval workflow 30-min" |
| 5.5 | Failure recovery | Nếu invite bounce → admin thấy email bounce trong dashboard + resend với corrected email; nếu tenant report bug → 1-click impersonate (read-only) cho debug | **Miss** — Wave 86 không cover bounce visibility + impersonate-read-only | 🚨 **NEW gap proposal:** GAP-XXX P1 "Admin Resend bounce visibility + impersonate-read-only debugging path" (incident response prereq) |

---

## 4. Findings — top 10 missed/partial cells priority-ranked

| Rank | Cell | Persona | Severity | Why critical | Action |
|---|---|---|---|---|---|
| 1 | 2.5 Soft-delete restore | P1 | 🚨 P0 | Carry-forward Wave 85 audit; user lost data = trust collapse + churn; blocks Phase 2 GA | NEW gap GAP-XXX P0 — defer Wave 87 nhưng FILE NGAY trong Wave 86 |
| 2 | 2.2 Invite email template content | P1 | 🚨 P1 | Email là first touchpoint; tone English/template-y = trust loss; impacts beta conversion | NEW gap + paired Bucket G AC |
| 3 | 4.1 P3 invite email content | P3 | 🚨 P1 | P2-to-P3 invite trust gate; missing context = P3 confusion / spam-folder rate | NEW gap + paired Bucket G AC |
| 4 | 3.3 P2 onboarding wizard step-count | P2 | 🚨 P1 | Wave 86 capture screenshots nhưng KHÔNG validate cognitive load; overwhelm = churn first-week | NEW gap — expand Bucket C scope (audit not just capture) |
| 5 | 5.5 Admin bounce + impersonate | Admin | 🚨 P1 | Incident response prereq — Bucket H scope; nếu invite bounce silent → first-cohort tenant lost | NEW gap + Bucket H AC |
| 6 | 1.3 Landing demo entry | Anonymous | ⚠️ P1 | Conversion gate — forcing signup-before-demo = bounce; impacts cohort pipeline post-5 | NEW gap — Wave 87 priority |
| 7 | 4.2 P3 permission disclosure | P3 | ⚠️ P1 | Trust gate — P3 unclear permissions = anxiety / over-cautious / over-bold actions | Bucket C AC add |
| 8 | 1.4 Waitlist auto-ACK email | Anonymous | ⚠️ P2 | UX hygiene — silent submit = user re-submits / bounces; small effort high signal | Bucket G AC add |
| 9 | 2.4 P1 30-min cold-start benchmark | P1 | ⚠️ P2 | Validation gate — Wave 86 needs measurable acceptance not just screenshots | Bucket H AC add |
| 10 | 1.1 Landing P95 mobile-3G <3s | Anonymous | ⚠️ P2 | Performance baseline gate — Wave 81 81/100 perf audit không có mobile-3G target explicit | Bucket E AC add |

## 5. AC additions suggested per bucket B-H

### Bucket C (GAP-537c P2/P3 screenshots) — expand scope

- [ ] **AC**: P2 onboarding wizard step-count ≤7 + skip-and-resume verified (rank 4)
- [ ] **AC**: P3 first-login show permission matrix explicit (rank 7)
- [ ] **AC**: P1 first-class onboarding flow ≤5 phút verified (rank 3.2 cell 2.3)

### Bucket E (Pre-launch hardening verification) — add 2 ACs

- [ ] **AC**: Landing P95 mobile-3G <3s (Lighthouse mobile profile evidence) (rank 10)
- [ ] **AC**: P3 dashboard FE menu hide admin/billing items (verify role-based UI guard) (rank 7 follow-up)

### Bucket G (Beta cohort invite) — add 4 ACs

- [ ] **AC**: Waitlist signup auto-ACK email <2min + SLA "2-3 ngày" trong email body (rank 8)
- [ ] **AC**: Invite email sender `support@kitehub.me` + tone Vietnamese + feedback CTA visible (rank 2)
- [ ] **AC**: P2 invite email body: P2 owner name + center context + role explicit (rank 3)
- [ ] **AC**: P2 first-login dashboard greeting personalized + admin contact + "1 trong 5 beta tenants" social proof banner (rank 3.1 + 3.2)
- [ ] **AC**: P2 onboarding-complete trigger summary email + 30-min benchmark validate (rank 3.4)

### Bucket H (Post-cohort monitoring + incident response) — add 4 ACs

- [ ] **AC**: Invite email delivery P95 <5min (Resend dashboard metric) (rank 2.1)
- [ ] **AC**: P1 cold-start first-class + first-attendance <30min (validate via 1 cohort tenant) (rank 9)
- [ ] **AC**: P3 first-day daily-ops <15 phút benchmark (rank 4.4)
- [ ] **AC**: Admin cohort queue load <2s + runbook section "5-tenant first-cohort 30-min workflow" (rank 5.1 + 5.4)

## 6. NEW gap proposals

| # | Title | P-level | Bucket | Trigger persona |
|---|---|---|---|---|
| 1 | Soft-delete + 7-day restore window for classes/students/attendance/grades | P0 | Wave 87 (file Wave 86) | P1 (cell 2.5) |
| 2 | Beta invite email template content audit — tone + sender + feedback CTA | P1 | Wave 86 Bucket G paired | P1 (cell 2.2) |
| 3 | P3 invite email content — P2 owner name + center context + role explicit | P1 | Wave 86 Bucket G paired | P3 (cell 4.1) |
| 4 | P2 onboarding wizard step-count audit + skip-and-resume UX | P1 | Wave 86 Bucket C paired | P2 (cell 3.3) |
| 5 | Admin Resend bounce visibility + impersonate-read-only debugging path | P1 | Wave 86 Bucket H paired | Admin (cell 5.5) |
| 6 | Landing CTA hierarchy + demo entry path before signup commit | P1 | Wave 87 | Anonymous (cell 1.3) |
| 7 | Landing form inline validation + draft preservation | P2 | Wave 87 | Anonymous (cell 1.5) |
| 8 | P2 invite management — resend / revoke / 24h reminder | P2 | Wave 87 | P2 (cell 3.5) |
| 9 | P3 grade edit-window (24h) + P2 unlock-P3 recovery | P2 | Wave 87 | P3 (cell 4.5) |
| 10 | Admin cohort bulk-approve UX (>20 tenants scale) | P2 | Wave 88+ | Admin (cell 5.3) |

## 7. Verdict — Wave 86 scope completeness % + critical adds

**Wave 86 inside-out scope completeness:** **~65%** (16/25 cells Match/Partial covered acceptably; 9/25 Miss or critical Partial).

**Breakdown:**
- ✅ Match: 4 cells (1.2, 2.3, 5.2 + 1 partial elevated)
- ⚠️ Partial acceptable: 12 cells (most addressable via AC additions §5)
- 🚨 Miss critical: 9 cells (need NEW gaps §6 + AC additions)

**Critical adds blocking Wave 86 closure (P0/P1, must address before tag):**
1. **NEW gap #1 (P0 soft-delete restore)** — FILE trong Wave 86 even nếu defer execution Wave 87; carry-forward Wave 85 audit + Phase 2 GA blocker
2. **NEW gap #2 + #3 (P1 invite email content audits)** — Bucket G paired execution; email là first beta touchpoint, tone/sender critical
3. **NEW gap #4 (P1 P2 onboarding wizard audit)** — Bucket C scope expand từ "capture" → "audit + capture"
4. **NEW gap #5 (P1 admin bounce + impersonate)** — Bucket H incident response prereq
5. **15 AC additions §5** — distribute across Bucket C/E/G/H trong Wave 86 plan revision

**Recommended Wave 86 plan revision (Bucket A → coordinator):**
- Expand Bucket C scope: "screenshots + Tier 2 annotation" → "+ wizard step-count audit + permission disclosure audit"
- Expand Bucket G scope: "invite send mechanism" → "+ email content audit + dashboard greeting + summary email"
- Expand Bucket H scope: "monitoring" → "+ bounce visibility + impersonate path + 30-min/15-min benchmarks"
- Add Bucket I (optional, sequential after F): file 4 NEW gaps + ROADMAP sync trước tag (per `inside-out-completeness-trigger.md` + `gap-architecture-v2.md`)

**Counterfactual without this audit:** Wave 86 ships rc.1 tag + invites 5 cohort với silent gaps (English email tone, no bounce visibility, no soft-delete safety net) → first-cohort tenant data-loss incident OR trust collapse → 2-4 week churn recovery + re-tag cycle. Audit eliminates 5 P0/P1 blockers BEFORE irreversible cohort invite.

**Pair with parallel audit outputs:**
- Benchmark agent (Wave 86 Bucket A peer): VN SaaS edu signup conversion + churn studies — converge findings rank 1.3 (demo entry) + rank 1.4 (auto-ACK)
- Simulation agent (Wave 86 Bucket A peer): failure modes first-100-user load — converge findings rank 2.1 (email delivery P95) + rank 5.5 (bounce visibility)

3-agent convergence target: ≥3 overlapping findings = high-confidence critical gaps for Wave 86 scope lock.
