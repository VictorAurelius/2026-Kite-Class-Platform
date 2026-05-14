---
title: Session Handoff — 2026-05-14 (Wave 72a/72b shipped + outside-in audits)
status: active
created: 2026-05-14
session_scope: "Wave 72a + 72b closure; starter-kit v2.4.0 + v2.4.1 + v2.5.0; outside-in audits (3); outside-in-coverage-trigger rule"
---

# Session Handoff — 2026-05-14

## TL;DR for next session

Session này shipped Wave 72a (6 buckets) + Wave 72b (7 buckets ngoại trừ Bucket A pending rebase) + starter-kit v2.4.0/v2.4.1/v2.5.0 + outside-in-coverage-trigger rule v1.0.0 + 3 outside-in audits (persona/benchmark/simulation).

**Next session pick-up order:**
1. Rebase + merge Bucket A #1301 (2FA BE — DIRTY do branched từ Foundation pre-merge)
2. Wave 72b closure PR (ROADMAP §🚀 + wave-history.jsonl + Release Plan Progress)
3. **Wave 73 plan revision** dựa trên 3 outside-in audit reports (dev scope chỉ cover ~20% gaps; cần mở rộng ~21h work)
4. File ~30 new gap candidates từ 3 audits

## State of waves

| Wave | Status | Note |
|---|---|---|
| 72a | ✅ SHIPPED 2026-05-14 | 6 buckets, 1 DONE + 7 PARTIAL, closure PR #1291 merged |
| 72b | 🟡 7/7 buckets coded; 6/7 merged; Bucket A #1301 pending rebase | Foundation #1294 ✅; B #1297 ✅; C #1298 ✅; D #1296 ✅; E #1299 ✅; G #1300 ✅; **A #1301 DIRTY (rebase needed)** |
| 73 | ⚠️ REVISION REQUIRED | Plan stub exists; outside-in audits surface 30 gaps; dev scope ~20% coverage; cần mở rộng buckets |
| 74 | Deferred | Stripe sandbox; ~+12h scope từ benchmark recommendations |

## Open PRs to handle next session

| PR | Status | Action needed |
|---|---|---|
| #1301 | DIRTY (Bucket A 2FA BE) | Rebase wave/72b-bucket-a-2fa-be onto main; resolve gap-status.csv conflicts (GAP-516 row); local `mvn verify -P strict-warnings`; merge |

## Outside-in audit findings — INPUT FOR WAVE 73 PLAN REVISION

### Persona review (PR #1303, audit `documents/04-quality/audits/persona-review/2026-05-14-phase-1-beta-persona-walkthrough.md`)
- 28 ô × 4 nhân vật (Lan/Hùng/Thuý/Bảo) × 7 bước
- **30 gaps surfaced**: 18 P0 + 9 P1 + 3 P2
- **Wave 73 dev coverage: ~20%** (5 dev buckets cover ~6/30 thực tế)
- **8 P0 dev miss** đề xuất thêm vào Wave 73/74:
  - Wave 73 (+4): onboarding auto-trigger / self-service export (PDPL!) / delete confirmation (PDPL!) / persona landing variant cho P1
  - Wave 74 (+4): personalized banner preview / cash invoice template / diacritic VN font / test-sandbox mode flag
- **9 strategic P0 cho Wave 75+**: 1-on-1 schedule, phone identity, notification engine, RBAC, public class landing, bulk import P3, VN invoice compliance
- Meta findings: persona scope bias (UI default P2 → P1 Solo Teacher lạc lõng); VN cultural blindspots; PDPL compliance gaps ở Bước 7

### External benchmark (PR #1305, audit `documents/04-quality/audits/external-benchmark/2026-05-14-beta-cohort-saas-comparison.md`)
- 5 sản phẩm: Hotmart / Teachable / Kajabi / ELSA Speak / Marathon Education
- 15 khía cạnh × 5 sản phẩm
- **Top 5 thiếu sót (P0/P1)**:
  1. **P0** Office hours / Zalo / FB Messenger live support
  2. **P1** Onboarding video tiếng Việt 5-7 phút
  3. **P1** Founder member / Pioneer badge cam kết beta
  4. **P1** Funnel tracking (PostHog/Mixpanel) — solo-dev không thể iterate khi miss
  5. **P0** Beta termination plan (30 ngày sau → ?)
- **Top 5 steal**:
  1. ELSA "Thầy cô Việt" sponsor model (free premium + Pioneer badge cho 5-10 hiệu trưởng)
  2. Kajabi 28-day Hero Challenge (email cadence day 1/3/7/14)
  3. ELSA staggered cohort (5 → 10 → 30+, không big-bang)
  4. Marathon FB+Zalo single-channel (1 Zalo OA + 1 FB page)
  5. Core/Reach/Backup recruitment model
- **Khuyến nghị Wave 73**: +21h scope (Zalo OA + FB page + founder welcome + Pioneer badge + PostHog + video + status page)
- **Khuyến nghị Wave 74**: +12h scope (NPS day-14 + data export + reset button)

### Simulation gap finder (PR #1304, audit `documents/04-quality/audits/simulation-gap-finder/2026-05-14-phase-1-beta-failure-modes.md`)
- 140 ô = 4 nhân vật × 7 bước × 5 chế độ thất bại
- **23 gaps surfaced**: 2 P0 + 21 P1/P2
- **8 gaps MỚI (35% dev miss class)** — intersection patterns:
  - SIM-17 P0: VN currency parse error `2.500.000 → 2.5 VND` (severity 20)
  - SIM-07 P0: admin double-approve race (severity 15)
  - Unicode NFC, cache-control verify endpoint, VN ISP email latency, date dd/mm vs mm/dd, JWT-suspend window, SSE block bởi VN enterprise proxy
- **Failure mode yếu nhất**: Sai dữ liệu (6/23 = 26%) — input parsing không locale-aware
- **Bước fragile nhất**: Daily Use (8/23 = 35%)
- **Phân nhóm**: Wave 73 (7 P0/P1 must-ship), Wave 74 (7 P1 medium), Hậu beta (9 P2 backlog)

## ACTION FOR NEXT SESSION — convert audits → gaps → Wave 73 plan

### Phase 1: File gaps (60-90 min)
Cross-reference 3 audit reports:
- 30 persona gaps
- 23 simulation gaps  
- 5+5 benchmark recommendations
= ~58 distinct gap candidates (some overlap)

Action: file each as new GAP-NNN trong `documents/04-quality/gaps/` per `audit-to-gap-pipeline.md` Step 3 template. Update `gap-status.csv` per `meta-csv-index-pattern.md`. ROADMAP entry.

Dedupe: ~58 → ~40-45 unique gaps expected.

### Phase 2: Wave 73 plan REVISION (30 min)
Edit `documents/03-planning/waves/wave-2026-05-14-72b-2fa-audit-rubric-review.md` (stub) → expand to full Wave 73 with:
- Original 4 buckets (email audit / user manual / Tally / UI smoke)
- +4 buckets từ persona P0 (onboarding trigger / export / delete confirm / persona variant)
- +Zalo OA + FB page setup (benchmark)
- +Onboarding video tiếng Việt 5-7 phút (benchmark — Stage 1)
- +Pioneer badge + cam kết founder member (benchmark)
- +PostHog funnel tracking (benchmark)
- +Beta termination plan doc (benchmark)
- +VN locale fixes (simulation P0 SIM-17 + Unicode NFC)

**Stake tier escalated**: MEDIUM-HIGH → HIGH (Wave 73 now blocks beta invite không chỉ là "nice polish")

Apply `outside-in-coverage-trigger.md` rule retroactively — Wave 73 plan §1 Brainstorm Q1 NOW has outside-in coverage from 3 audits.

### Phase 3: Wave 72b closure (15 min)
After Bucket A merged:
- Wave plan frontmatter `status: complete`
- `wave-history.jsonl` append
- ROADMAP §🎯 Status Snapshot + §🚀 Next Action update
- gap-status.csv re-sync if needed
- Release Plan Progress section trong closure PR body

## Docs state check

| Doc | Updated? | Notes |
|---|---|---|
| `documents/04-quality/gaps/ROADMAP.md` §🎯 Snapshot | ⚠️ Stale — vẫn nhắc Wave 72a SHIPPED, chưa nhắc 72b | Update ở Wave 72b closure PR |
| `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action | ⚠️ Đang nhắc Wave 72b queue (đã done) | Update ở Wave 72b closure → Wave 73 |
| `documents/04-quality/gaps/gap-status.csv` | ✅ Current cho GAP-516..526 (Wave 72b range) | New gaps từ audits chưa file |
| `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` | ✅ Wave 72a entry; ❌ Wave 72b chưa append | Append ở closure |
| `.claude/rules/rules-index.csv` | ✅ Current (54 rows; outside-in rule added) | — |
| `documents/03-planning/waves/wave-2026-05-14-72b-2fa-audit-rubric-review.md` | ✅ Plan PR #1292 merged; closure pending | Frontmatter status flip ở closure |
| `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` | ✅ Wave 72a Bucket F + 72b Bucket G refined (VN translation + BOM + relocated) | — |

## PRs merged this session (chronological)

Wave 72a (10 PRs): #1282 plan, #1283 A gateway, #1284 D credential, #1285 C FE role, #1286 E meta, #1287 B BE security, #1288 F self-test CSV, #1289 statusline sync, #1290 starter-kit v2.4.0, #1291 closure

Wave 72b (10 PRs): #1292 plan, #1293 unused imports, #1294 Foundation auth, #1295 starter-kit v2.5.0 project, #1296 D admin verify, #1297 B 2FA FE, #1298 C login alert, #1299 E audit rubric, #1300 G self-test rework

Side (5 PRs): #1302 outside-in rule, #1303 persona audit, #1304 simulation gap, #1305 benchmark audit, **+ remote starter-kit #11 v2.4.0 + #12 v2.4.1 + #13 v2.5.0**

**Total: 25 PRs merged**

## Pending PRs at session end

| PR | Status | Action |
|---|---|---|
| #1301 Bucket A 2FA BE | DIRTY | Rebase + merge next session |

## Active background work at session end

None — all 3 outside-in agents completed. No worktree husks (will prune ở closure PR).

## Critical context for next session

1. **outside-in-coverage-trigger.md** rule shipped — Claude PHẢI tự động hỏi outside-in khi dev inside-out brainstorm. Auto-loads via memory.
2. **Wave 73 plan stub là cũ** — đề xuất 4 buckets dựa trên dev inside-out. Audit reports cho thấy under-scoped 80%. Cần REVISION trước khi spawn agents.
3. **AWS stack đang UP** từ session này — nếu next session pause lâu → `bash scripts/aws/stop-stack.sh --force` để save Free Tier hours.
4. **Bucket A 2FA BE PR base** đã được thay đổi main (từ wave/72b-bucket-0-foundation). Rebase sẽ resolve gap-status.csv GAP-516 row (Bucket B đã update với PARTIAL 50%; Bucket A sẽ bump tới ~80%).
5. **Audit-of-trust mandate** — Wave 72b BE changes (V37/V38 migrations, 2FA, login audit, admin audit) cần audit suite chạy ≤3 ngày per `post-wave-audit-mandate.md`. Security audit + business-logic audit pending.

## Memory entries to copy to user-memory dir

Per `post-merge-sync-completeness.md` §7.5 — new memory entries from this session that next session should have:

### `feedback_outside_in_coverage_trigger.md` (already in PR #1302 description)

Inside-out (dev liệt kê features) phải pair với outside-in (persona/benchmark/matrix). Claude auto-suggest outside-in khi user message có pattern brainstorm scope. Cost của question 1 message; cost của miss = wave retroactive fix.

Trigger patterns:
- "Đã có X, Y, Z — đủ chưa?"
- "Wave NN sẽ có buckets A/B/C"
- "Cần làm gì nữa cho launch?"
- Pre-release readiness check

Reference: `.claude/rules/outside-in-coverage-trigger.md` v1.0.0.

### `feedback_audit_scope_under_inside_out.md` (NEW — write to user-memory)

Khi dev brainstorm scope inside-out, kết quả thường cover 15-25% gap thực tế (Wave 73 example: 20%). Cần outside-in audit để mở rộng. 3 phương pháp bù trừ nhau: persona walkthrough (tâm lý + flow gaps), external benchmark (industry standards), simulation matrix (failure modes intersection). Spawn 3 parallel; tổng hợp findings → wave plan revision.

Source: Wave 73 outside-in audit findings 2026-05-14 — 3 reports surfaced 30+23+10 gaps; ~70% mới dev không nghĩ tới.
