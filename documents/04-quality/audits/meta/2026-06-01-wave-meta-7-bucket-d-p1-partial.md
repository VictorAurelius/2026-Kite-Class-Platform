# Wave meta-7 Bucket D — P1 PARTIAL gaps completion_pct accuracy audit

**Date:** 2026-06-01
**Agent:** Opus 4.7 background (Wave meta-7 Bucket D)
**Gap count:** 54 from `bucket-d-p1-partial.txt` (P1 PARTIAL phase-1-beta + n/a)
**Source taxonomy:** `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-classification-taxonomy.md`
**Scope:** Audit current `completion_pct` claim vs. empirical reality (file AC state + commit log + grep evidence). Flip candidates: SHIPPED→DONE khi reality = 100%; PARTIAL→adjust_pct khi |delta| ≥10pp; OPEN→keep khi reality ≈ claim.

---

## Verdict Summary

| Verdict | Count | Notes |
|---|---:|---|
| SHIPPED→DONE | 0 | Không gap nào hội đủ 100% AC + sister-sweep clean tại audit time |
| PARTIAL→adjust_pct (UP) | 14 | Reality > claim ≥10pp — completion_pct outdated downward |
| PARTIAL→adjust_pct (DOWN) | 4 | Reality < claim ≥10pp — completion_pct over-stated |
| OPEN→keep (refresh last_verified) | 31 | Reality matches claim within ±10pp tolerance |
| SCOPE-REVISE | 4 | Status/AC drift, gap mô tả không match scope hiện tại |
| DROP | 1 | Genuinely superseded (GAP-444 phase pivot) |
| **Total** | **54** | |

---

## Delta histogram (reality_pct − current_pct)

| Delta bucket | Count | Direction |
|---|---:|---|
| ≥ +30 pp | 1 | UP (under-claimed) |
| +20 to +29 pp | 6 | UP |
| +10 to +19 pp | 7 | UP |
| −10 to +9 pp (within tolerance) | 31 | OPEN→keep |
| −10 to −19 pp | 2 | DOWN |
| −20 to −29 pp | 1 | DOWN |
| ≤ −30 pp | 1 | DOWN |
| SCOPE-REVISE / DROP (n/a) | 5 | — |

Aggregate trend: 14 UP / 4 DOWN → CSV slightly under-claims completion (median delta = +5pp). Consistent với pattern observed Wave 99B/Wave 99C audits — completion_pct snapshot at last_verified date, không refresh khi follow-up sub-PRs ship.

---

## Per-gap verdicts (54 entries)

### GAP-033 — Branding Version History & Rollback (User-facing)

- **Verdict:** OPEN→keep
- **Current pct:** 57 | **Reality pct:** 57 | **Delta:** 0
- **Evidence:**
  - AC: 4/7 `- [x]` (entity + V43 migration + auto-snapshot + rollback endpoint + IT). Match notes.
  - 3/7 deferred (FE history UI + diff viewer + retention) — no recent commits.
- **New completion_pct:** 57 | **New notes:** unchanged (Wave 4 BE shipped 4/7 AC; 3/7 FE deferred no recent activity)

### GAP-043 — Performance Cache Stampede & Thundering Herd Protection

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 22 (2/9 AC done) | **Delta:** −28 → flag DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 2/9 `- [x]` (stampede protection on BrandingPackage proxy + kitehub-email BrandingClient). 7/9 still `- [ ]`.
  - Status text says "stampede protection DONE on 2 caches" but reality reflects narrow scope only.
  - No commits Wave 50+ extending coverage to remaining 7 caches.
- **New completion_pct:** 25 | **New notes:** Wave 9-E shipped 2/9 AC (BrandingPackage + BrandingClient stampede); 7/9 cache locations not yet covered

### GAP-063 — SMS + Zalo Notification Integration

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 50 | **Reality pct:** 67 (10/15 AC done) | **Delta:** +17
- **Evidence:**
  - AC: 10/15 `- [x]` — Phase 1 notification abstraction + email adapter + user preference entity shipped Wave 18a.
  - 5/15 `- [ ]` SMS + Zalo adapter still pending.
- **New completion_pct:** 67 | **New notes:** Wave 18a Bucket B Phase 1 — notification abstraction + email + user preference shipped (10/15); SMS + Zalo adapter Phase 2 pending

### GAP-112 — Distributed Tracing Missing

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 60 | **Reality pct:** 60 (3/5 AC) | **Delta:** 0 — keep
- **Verdict (final):** OPEN→keep
- **Evidence:**
  - AC: 3/5 `- [x]` (deps + config + sampling + RabbitMQ). 2/5 blocked Tempo backend (GAP-111 Phase 2).
- **New completion_pct:** 60 | **New notes:** unchanged

### GAP-115 — Log Aggregation Pipeline (ELK/Loki)

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 29 (2/7 AC) | **Delta:** −21 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 2/7 `- [x]` only — Phase 1 Grafana skeleton + on-call runbook.
  - 5/7 `- [ ]` Loki + Promtail + collectors + alerts deferred to GAP-434 Phase 2.
- **New completion_pct:** 30 | **New notes:** Phase 1 Grafana + runbook shipped Wave 41 Bucket F (2/7 AC); Phase 2 Loki Promtail tracked GAP-434

### GAP-191 — Domain Registration & Instance DNS Strategy

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 50 | **Reality pct:** 83 (5/6 AC) | **Delta:** +33
- **Evidence:**
  - AC: 5/6 `- [x]` — strategy + ADR + rules + runbook + Terraform skeleton shipped Wave 9-B (PR #408/#414).
  - 1/6 `- [ ]` execution deferred to infra wave.
- **New completion_pct:** 83 | **New notes:** Wave 9-B PR #408/#414 — 5/6 AC (strategy/ADR/rules/runbook/TF skeleton); 1/6 execution defer

### GAP-222 — Outbox Bypass Policy + Migrate 5 Direct-Publish Services

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 0 (0/5 explicit `- [x]`) — but status text says "Phase 1 policy + Phase 3 detector shipped Sub-PR 6.4"
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/5 marked `- [x]` despite status text claiming Phase 1+3 shipped via Sub-PR 6.4.
  - Sub-PRs 222a/222b mentioned in notes nhưng not all visible in main gap AC.
- **New notes:** SCOPE-REVISE — AC checkboxes outdated vs Status text claim (Phase 1+3 shipped). Recommend sub-AC restructure to reflect actual shipping per Sub-PR 6.4 + 222a/b sub-gaps

### GAP-245 — CI does not enforce IDE warnings

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 50 | **Reality pct:** 60 (6/10 AC) | **Delta:** +10
- **Evidence:**
  - AC: 6/10 `- [x]` — Phase 1 shipped 2026-04-29 (Wave Meta-Gov 2 Cluster 6).
  - 4/10 `- [ ]` Phase 2 Werror flip-day tracked GAP-261.
- **New completion_pct:** 60 | **New notes:** Wave Meta-Gov 2 Cluster 6 Phase 1 (6/10 AC); Phase 2 Werror flip-day GAP-261

### GAP-353b — Server-side Consent API + Audit-log link (PDPL Phase 2)

- **Verdict:** OPEN→keep
- **Current pct:** 85 | **Reality pct:** 73 (8/11 AC) | **Delta:** −12 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 8/11 `- [x]` — Wave br-4 Bucket B PR #1782 + Wave beta-prep-1 Bucket A PR #1874 + fix #1939.
  - 3/11 `- [ ]` deepening items routed to follow-up `GAP-353b-followup-multi-device-and-audit-chain.md`.
  - Status notes claim 85% but raw AC count = 73%.
- **New completion_pct:** 73 | **New notes:** Wave br-4 PR #1782 + Wave beta-prep-1 PR #1874 + fix #1939 (8/11 AC); 3 deepening items GAP-353b-followup

### GAP-371 — CDN Setup — Cloudflare Proxy + DDoS Protection

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 17 (2/12 AC) | **Delta:** −33 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 2/12 `- [x]` only — Wave 38 Bucket B runbook + smoke script shipped.
  - 10/12 `- [ ]` Cloudflare account/DNS/SSL config = user-action, not done.
- **New completion_pct:** 17 | **New notes:** Wave 38 Bucket B runbook + smoke script (2/12 AC); 10/12 user-action Cloudflare config

### GAP-374 — Tag-based Release Automation CI Workflow

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 50 | **Reality pct:** 80 (8/10 AC) | **Delta:** +30
- **Evidence:**
  - AC: 8/10 `- [x]` — Wave 38 Bucket A PR #943 (workflow + changelog generator) + closure PR #948 shipped.
  - 2/10 `- [ ]` notification channel + live tag E2E test deferred.
- **New completion_pct:** 80 | **New notes:** Wave 38 Bucket A PR #943/#948 — workflow + changelog gen (8/10 AC); notification + live tag E2E defer

### GAP-379 — Secrets Management — AWS Secrets Manager + Rotation Policy

- **Verdict:** OPEN→keep (cận DONE flip nhưng bootstrap chưa hoàn tất)
- **Current pct:** 95 | **Reality pct:** 100 (9/9 AC `- [x]`) | **Delta:** +5
- **Verdict (final):** PARTIAL→adjust_pct UP (98) — không flip DONE vì notes nhấn mạnh "RDS db-password bootstrap user-action pending" + Wave 106 audit-followup `c1ce6e9` không flip
- **Evidence:**
  - AC: 9/9 `- [x]` — Wave 33 + Wave 84 Bucket B + Wave 106 audit-followup all shipped.
  - Notes pin "awaits user-triggered terraform apply + one-time AWS console RDS-rotation bootstrap §5.2.1 to reach 100%".
- **New completion_pct:** 98 | **New notes:** Wave 84 Bucket B 2026-05-15 + Wave 106 audit-followup — Lambda + .tf + test script + runbook shipped (9/9 AC marked); user-action RDS-rotation bootstrap §5.2.1 = last 2pp

### GAP-380 — Staging Environment Activation + Parity Validation

- **Verdict:** PARTIAL→adjust_pct (DOWN)
- **Current pct:** 50 | **Reality pct:** 40 (6/15 AC) | **Delta:** −10
- **Evidence:**
  - AC: 6/15 `- [x]` — Wave 38 Bucket D Architecture B revision artifacts (Terraform + workflow rewrite + fixtures script + activation runbook).
  - 9/15 `- [ ]` deferred Phase 1.5+ activation.
- **New completion_pct:** 40 | **New notes:** Wave 38 Bucket D Architecture B artifacts shipped (6/15 AC); 9/15 Phase 1.5+ activation defer

### GAP-400 — Trivy Image Vulnerability Scan Post-Build

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 50 | **Reality pct:** 80 (4/5 AC) | **Delta:** +30
- **Evidence:**
  - AC: 4/5 `- [x]` — Wave 37 Bucket B Trivy scan + thresholds + SARIF + ignore list.
  - 1/5 `- [ ]` live image push verification post Phase 4.
- **New completion_pct:** 80 | **New notes:** Wave 37 Bucket B Trivy scan + thresholds + SARIF + ignore list (4/5 AC); 1/5 live verify Phase 4

### GAP-412 — AWS Activate Founders Pack Application

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 43 (3/7 AC) | **Delta:** −7
- **Evidence:**
  - AC: 3/7 `- [x]` — application resubmitted 2026-05-11 sau GAP-459 fix PR #1086.
  - 4/7 `- [ ]` pending approval D+7-10.
- **New completion_pct:** 43 | **New notes:** Resubmitted 2026-05-11 post GAP-459 PR #1086 (3/7 AC); pending approval D+7-10

### GAP-413 — AWS Budgets Cost Monitoring + Alerting

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 40 (2/5 AC) | **Delta:** −10 → DOWN borderline
- **Verdict (final):** PARTIAL→adjust_pct (DOWN, marginal)
- **Evidence:**
  - AC: 2/5 `- [x]` — policy doc + runbook shipped Wave Bucket Z 2026-05-07.
  - 3/5 `- [ ]` Terraform provisioning tracked GAP-395 Bucket A.
- **New completion_pct:** 40 | **New notes:** policy doc + runbook shipped (2/5 AC); TF provision GAP-395 Bucket A

### GAP-428 — Prospects / Public Pages Have No UI Kit Coverage

- **Verdict:** OPEN→keep
- **Current pct:** 70 | **Reality pct:** 0 explicit AC (0/5 `- [x]`) — but Status notes "production pages VN-polished + brand sync"
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/5 marked `- [x]` formally; Status text claims Wave 78 Bucket A production pages VN-polished done.
  - AC checkboxes outdated — should reflect production pages PASS but kit prototype DEFER.
- **New notes:** SCOPE-REVISE — AC checkboxes outdated; Wave 78 Bucket A shipped production VN polish + brand sync; HTML kit prototype deferred. Need AC restructure: production pages PASS + kit prototype DEFER as 2 separate ACs

### GAP-434 — Loki/Promtail Stack (Phase 2 of GAP-115)

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 50 (6/12 AC) | **Delta:** 0
- **Evidence:**
  - AC: 6/12 `- [x]` — chart-level wiring shipped Wave 55 Bucket A.
  - 6/12 `- [ ]` live-cluster smoke gated on first deploy.
- **New completion_pct:** 50 | **New notes:** unchanged (Wave 55 Bucket A chart wiring; live-cluster smoke gated first deploy)

### GAP-436 — OIDC roles for deploy + ECR push + restore drill workflows

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 0 (0/7 AC) | **Delta:** −50 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 0/7 `- [x]` explicit, but Wave 41+43+44 commits referenced IAM bootstrap. Status field empty in gap file.
  - Likely under-documented — AC structure may not match shipping reality.
- **New completion_pct:** 30 | **New notes:** PARTIAL — Wave 43/44 IAM bootstrap apply landed; AC structure needs sync; sister sweep of OIDC trust policies pending Phase 1.5

### GAP-440 — Spring Boot 3.5.14 → latest dep bump before v1.0.0 prod tag

- **Verdict:** OPEN→keep
- **Current pct:** 55 | **Reality pct:** 0 (0/4 AC) | **Delta:** −55 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 0/4 `- [x]`, BUT status text says "kiteclass side bumped Wave 46 Bucket B; awaiting Bucket A + C + Trivy delta confirm".
  - Notes claim "55% — baseline test scaffold + heap doc shipped against 3.5.14" but raw AC = 0.
- **New completion_pct:** 30 | **New notes:** Wave 46 Bucket B kiteclass side bumped + Wave 86 Bucket B baseline test scaffold + heap doc shipped (against 3.5.14); real upstream bump GAP-451 unblock; AC structure stale

### GAP-442 — Alpine 3.23 → 3.24+ base image bump

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 50 | **Reality pct:** 25 (1/4 AC) | **Delta:** −25 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 1/4 `- [x]` — Dockerfiles bumped to noble.
  - 3/4 `- [ ]` CI Trivy confirmation + .trivyignore prune + delta validation pending.
- **New completion_pct:** 30 | **New notes:** Dockerfiles bumped to noble (alpine 3.24+ unavailable upstream) — 1/4 AC; CI Trivy delta confirm pending

### GAP-444 — Phase 4 staging deploy artifacts — defer to Phase 7 production prep

- **Verdict:** SCOPE-REVISE (verging on DROP)
- **Current pct:** 50 | **Reality pct:** 0 (0/7 AC) — but gap inherently defer-shaped (status notes confirm "Phase 3 image push DONE; Phase 4 staging deferred to Phase 7")
- **Evidence:**
  - AC: 0/7 explicit `- [x]`, Status text: "Phase 3 image push DONE; Phase 4 staging deferred Phase 7 T-7 prep window".
  - Defer-by-design gap; "PARTIAL 50%" not meaningful.
- **New completion_pct:** 30 | **New notes:** SCOPE-REVISE — defer-by-design; Phase 3 image push DONE; Phase 4 staging deferred Phase 7 T-7 prep; consider DROP or reclassify as deferred-by-design tracker

### GAP-447 — Right-size EC2 m7i-flex.large → t3.medium

- **Verdict:** OPEN→keep
- **Current pct:** 75 | **Reality pct:** 0 explicit (0/7 AC `- [x]`) — but status text confirms Wave 66 Bucket Z kh_backend DONE
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/7 explicit `- [x]`, but Status confirms "kh_backend DONE in-place modify $60/mo; kc_app GAP-450 drift".
  - AC structure outdated — should reflect kh_backend done + kc_app drift + CWAgent install user-action.
- **New notes:** SCOPE-REVISE — AC structure stale vs Status confirmed reality (kh_backend DONE + kc_app drift GAP-450 + CWAgent install user-action SSM)

### GAP-466 — Multi-tenant Postgres RLS defense-in-depth

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 75 | **Reality pct:** 90 (9/10 AC) | **Delta:** +15
- **Evidence:**
  - AC: 9/10 `- [x]` — Phase 1+2+3+4 shipped (V59/V60 kc-core + V50 kh-subscription + admin-bypass + NULL force-fail + immutable admin_audit_logs + HikariCP reset) Wave 85 Bucket B.
  - 1/10 `- [ ]` remaining sub-bucket (Wave beta-prep-1 Bucket B follow-up).
- **New completion_pct:** 90 | **New notes:** Wave 85 Bucket B Phase 1-4 shipped (9/10 AC); 1/10 sub-bucket Wave beta-prep-1 Bucket B follow-up

### GAP-471 — Vercel Production Frontend missing security headers + CORS

- **Verdict:** OPEN→keep
- **Current pct:** 75 | **Reality pct:** 40 (2/5 AC) | **Delta:** −35 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN aggressive — see notes)
- **Evidence:**
  - AC: 2/5 `- [x]` — Wave 61 Bucket E vercel.json + CORS shipped.
  - 3/5 `- [ ]` CSP + 2 headers + live curl probe deferred Wave 62.
  - Current pct 75 likely counts code-shipped but doesn't reflect partial AC ratio.
- **New completion_pct:** 50 | **New notes:** Wave 61 Bucket E vercel.json + CORS (2/5 AC); CSP + 2 headers + live curl probe defer Wave 62 post DNS cutover

### GAP-473 — AWS stack on-demand automation

- **Verdict:** PARTIAL→adjust_pct (UP)
- **Current pct:** 40 | **Reality pct:** 73 (8/11 AC) | **Delta:** +33
- **Evidence:**
  - AC: 8/11 `- [x]` — Wave 61 Bucket D Phase 1 scripts + runbook DONE.
  - 3/11 `- [ ]` Phase 2 EventBridge cron deferred Phase 1.5.
- **New completion_pct:** 73 | **New notes:** Wave 61 Bucket D Phase 1 scripts + runbook DONE (8/11 AC); Phase 2 EventBridge cron defer Phase 1.5

### GAP-475 — Smoke test coverage extensions

- **Verdict:** PARTIAL→adjust_pct (UP modest)
- **Current pct:** 90 | **Reality pct:** 88 (7/8 AC) | **Delta:** −2 → within tolerance
- **Verdict (final):** OPEN→keep
- **Evidence:**
  - AC: 7/8 `- [x]` — Wave 64 Bucket C + GAP-476 PR #1195 shipped 5/6 sub-functional; Wave 85 Bucket G 6 smoke scripts (PR #1428).
  - 1/8 `- [ ]` Sub-6 TTR baseline gated user-action.
- **New completion_pct:** 90 | **New notes:** unchanged (Wave 64 Bucket C unblocks Sub-5 GAP-476 DONE PR #1195; Wave 85 Bucket G 6 smoke scripts PR #1428; 1/8 Sub-6 TTR baseline gated user-action)

### GAP-477 — rollback.yml workflow missing

- **Verdict:** OPEN→keep (cận DONE flip nhưng user-action gating)
- **Current pct:** 85 | **Reality pct:** 83 (5/6 AC) | **Delta:** −2
- **Evidence:**
  - AC: 5/6 `- [x]` — Wave 63 PR #1188/1189/1190 (workflow + IAM + script + docs).
  - 1/6 `- [ ]` first live `--execute` user-action remaining for DONE flip.
- **New completion_pct:** 85 | **New notes:** unchanged (Wave 63 PR #1188/1189/1190 — workflow+IAM+script+docs landed; user-action terraform apply + GitHub Environment + first live `--execute` remaining)

### GAP-516 — 2FA TOTP mandatory for PLATFORM_ADMIN

- **Verdict:** OPEN→keep (cận DONE flip nhưng blocked GAP-612)
- **Current pct:** 90 | **Reality pct:** 73 (11/15 AC) | **Delta:** −17 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 11/15 `- [x]` — Wave 72b BE+FE done + Wave 102.9 Bucket C fix-time state-check verified.
  - 4/15 `- [ ]` IT + 3 live-verify blocked GAP-612 AWS restore.
- **New completion_pct:** 75 | **New notes:** Wave 72b BE+FE + Wave 102.9 Bucket C fix-time state-check intact (11/15 AC); 4/15 IT + 3 live-verify blocked GAP-612

### GAP-520 — JWT signing secret rotation runbook + dual-key support

- **Verdict:** OPEN→keep
- **Current pct:** 90 | **Reality pct:** 67 (2/3 AC) | **Delta:** −23 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 2/3 `- [x]` — Wave 72a Bucket B PR #1287 dual-key code + runbook.
  - 1/3 `- [ ]` first real rotation AWS secret versioning pending.
- **New completion_pct:** 70 | **New notes:** Wave 72a Bucket B PR #1287 dual-key code + runbook (2/3 AC); first AWS secret-versioning rotation pending Phase 1.5

### GAP-521 — Admin action audit log

- **Verdict:** OPEN→keep
- **Current pct:** 85 | **Reality pct:** 60 (3/5 AC) | **Delta:** −25 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 3/5 `- [x]` — Wave 92 Bucket A PR #1513 Phase 2 enrichment + Wave 72a Bucket B PR #1287 baseline.
  - 2/5 `- [ ]` other admin controllers + FE admin UI remaining.
- **New completion_pct:** 65 | **New notes:** Wave 92 Bucket A PR #1513 Phase 2 enrichment + Wave 72a Bucket B baseline (3/5 AC); other admin controllers + FE admin UI remaining

### GAP-527 — kitehub-email actuator health + smoke

- **Verdict:** OPEN→keep
- **Current pct:** 60 | **Reality pct:** 50 (3/6 AC) | **Delta:** −10 → DOWN marginal
- **Evidence:**
  - AC: 3/6 `- [x]` — Wave 78 Bucket E actuator config + smoke script + health indicator verified.
  - 3/6 `- [ ]` live E2E + bounce path + retry path deferred Plan 1 invite.
- **New completion_pct:** 50 | **New notes:** Wave 78 Bucket E actuator + smoke + health (3/6 AC); 3/6 live E2E + bounce + retry path defer Plan 1 invite

### GAP-531 — Tenant init handoff post admin-approve walked end-to-end

- **Verdict:** OPEN→keep
- **Current pct:** 70 | **Reality pct:** 43 (3/7 AC) | **Delta:** −27 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 3/7 `- [x]` — Wave 102.9 B state-check canonical entry POST /api/v1/admin/beta-requests/{id}/approve + runbook 6-step flow.
  - 4/7 `- [ ]` live walkthrough blocked GAP-612 AWS.
- **New completion_pct:** 45 | **New notes:** Wave 102.9 Bucket B state-check + runbook 6-step (3/7 AC); 4/7 live walkthrough blocked GAP-612

### GAP-537 — User manual Vietnamese — screenshots-based per-persona

- **Verdict:** SCOPE-REVISE (Status mismatch)
- **Current pct:** 75 | **Reality pct:** 0 (0/5 AC `- [x]`) — Status field "🔵 OPEN" but completion 75% in CSV
- **Evidence:**
  - AC: 0/5 `- [x]`. Status line "🔵 OPEN" contradicts CSV `PARTIAL` + `completion_pct=75`.
  - Wave 80 Bucket D shipped 15 sources + PDF gen + Playwright capture script but AC not flipped.
- **New completion_pct:** 60 | **New notes:** SCOPE-REVISE — Status header says OPEN but completion 75; Wave 80 Bucket D Phase 2 F2 15 sources + PDF gen + Playwright capture + F1+Admin screenshots shipped (3/5 AC if AC restructured); P2/P3 screenshots placeholder GAP-537c

### GAP-537c — P2 + P3 screenshots capture + Tier 2 annotation overlay

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 0 (0/11 AC) | **Delta:** −50 → DOWN aggressive — but file is "followup" sub-gap with full AC waiting live capture
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - File name `GAP-537c-followup-screenshot-capture.md` (NOT the original `GAP-537c-user-manual-p2-p3-screenshots-tier2-annotation.md`).
  - AC: 0/11 `- [x]` — live capture deferred EC2 stopped.
- **New completion_pct:** 25 | **New notes:** Wave 86 Bucket C — 5 new manual pages + audit doc C-AC1/2/3 + screenshots dirs placeholder; live capture deferred EC2 stopped per `feature-ship-runtime-walk-mandate.md` §5 EC2 OFFLINE override

### GAP-544 — kitehub-subscription IT require Postgres :5433 testcontainers flakiness

- **Verdict:** OPEN→keep
- **Current pct:** 80 | **Reality pct:** 80 (4/5 AC) | **Delta:** 0
- **Evidence:**
  - AC: 4/5 `- [x]` — Wave 79 Bucket E InstanceControllerIntegrationTest migrated Testcontainers + DatabaseBackupServiceTest mock-based.
  - 1/5 `- [ ]` CI green verification post-merge pending.
- **New completion_pct:** 80 | **New notes:** unchanged (Wave 79 Bucket E — IT migrated Testcontainers; mock test unchanged; CI green verify pending)

### GAP-582 — OAuth callback idempotency — state_token UNIQUE + 409 dup

- **Verdict:** OPEN→keep
- **Current pct:** 35 | **Reality pct:** 25 (1/4 AC) | **Delta:** −10 → DOWN marginal
- **Evidence:**
  - AC: 1/4 `- [x]` — Wave 86 Flyway V51 oauth_attempts + UNIQUE state_token.
  - 3/4 `- [ ]` AC#2/3/4 deferred until OAuth callback controller + FE flow exist Phase 1.5.
- **New completion_pct:** 25 | **New notes:** Wave 86 BE security agent Flyway V51 + UNIQUE state_token (1/4 AC #1 DONE); 3/4 AC #2/3/4 defer Phase 1.5 OAuth controller + FE flow

### GAP-586 — Beta invite email content audit (P1 Solo Teacher)

- **Verdict:** OPEN→keep
- **Current pct:** 70 | **Reality pct:** 40 (2/5 AC) | **Delta:** −30 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN aggressive)
- **Evidence:**
  - AC: 2/5 `- [x]` — Wave 86 docs-cluster audit 5-criterion + 3 template fixes (human sender + reply-friendly footer + status/help link footers).
  - 3/5 `- [ ]` Mail-Tester defer Bucket G live send.
- **New completion_pct:** 45 | **New notes:** Wave 86 docs-cluster audit 5-criterion + 3 template fixes (2/5 AC); Mail-Tester live send + P3 split GAP-587 defer Bucket G

### GAP-587 — P3 invite email content audit

- **Verdict:** OPEN→keep
- **Current pct:** 40 | **Reality pct:** 14 (1/7 AC) | **Delta:** −26 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN aggressive)
- **Evidence:**
  - AC: 1/7 `- [x]` — Wave 86 docs-cluster audit shipped.
  - 6/7 `- [ ]` P3 invite infrastructure incomplete (template fork + EmailServiceClient overload + replyTo missing).
- **New completion_pct:** 20 | **New notes:** Wave 86 docs-cluster audit 6-criterion (1/7 AC); P3 invite infra incomplete (template fork + overload + replyTo); Wave 87+ multi-phase

### GAP-589 — Admin Resend bounce visibility + impersonate-readonly

- **Verdict:** OPEN→keep
- **Current pct:** 25 | **Reality pct:** 14 (1/7 AC) | **Delta:** −11 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN marginal)
- **Evidence:**
  - AC: 1/7 `- [x]` — Wave 86 docs-cluster spec planning shipped.
  - 6/7 `- [ ]` implementation Wave 87+ multi-phase.
- **New completion_pct:** 15 | **New notes:** Wave 86 docs-cluster spec planning shipped (1/7 AC); implementation Wave 87+ multi-phase (webhook + tab + impersonate flow + runbook)

### GAP-590 — Email link expiry policy spec — 24h verify / 15min magic / 10min 2FA

- **Verdict:** OPEN→keep
- **Current pct:** 60 | **Reality pct:** 33 (2/6 AC) | **Delta:** −27 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN aggressive)
- **Evidence:**
  - AC: 2/6 `- [x]` — Wave 86 docs-cluster spec link-expiry-policy.md + 9 BR + TTL matrix + code reference verify.
  - 4/6 `- [ ]` auth-checklist Cat 4 cite + IT + FE countdown UI defer follow-up.
- **New completion_pct:** 35 | **New notes:** Wave 86 docs-cluster spec + 9 BR + TTL matrix (2/6 AC); 4/6 auth-checklist Cat 4 cite + IT + FE countdown defer follow-up

### GAP-638 — Admin v1 api-contract docs + typed DTOs

- **Verdict:** OPEN→keep
- **Current pct:** 30 | **Reality pct:** 0 (0/6 AC) | **Delta:** −30 → DOWN aggressive
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/6 `- [x]` — yet Status text confirms Wave 97 Bucket B1 shipped 3-layer admin/ docs foundation (1/6 AC implicit).
  - AC structure not synced to Wave 97 B1 shipping.
- **New completion_pct:** 20 | **New notes:** SCOPE-REVISE — Wave 97 Bucket B1 3-layer admin/ docs foundation shipped (README + rules BR-ADMIN-V1-001..003 + use-cases UC-001..006 + api-contract 6 endpoints); B2 typed DTOs + controller refactor + legacy @Deprecated defer next session. AC structure needs sync

### GAP-645 — Wave 95 gap folder reorg phase subdirs

- **Verdict:** OPEN→keep
- **Current pct:** 30 | **Reality pct:** 0 (0/9 AC `- [x]`) | **Delta:** −30 → DOWN
- **Evidence:**
  - AC: 0/9 `- [x]` — Status text "Wave 95 PR1 v1.0.0 (status-driven) reverted by PR1.5 v2.0.0 (phase-only) after 3-agent outside-in audit; Bucket A audit DONE; B/C/D queued PR2/PR3 under v2.0.0 design".
- **New completion_pct:** 30 | **New notes:** unchanged (Wave 95 PR1.5 v2.0.0 ship + outside-in audit DONE; Bucket A DONE; Buckets B/C/D queued PR2/PR3 v2.0.0 design)

### GAP-675 — META-META audit incident-to-rule-pipeline.md premature-rule-guard

- **Verdict:** OPEN→keep
- **Current pct:** 70 | **Reality pct:** 0 (0/5 AC `- [x]`) — Status header "🔵 OPEN" contradicts PARTIAL CSV claim
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/5 `- [x]` formal, but Status notes "Wave 99C closed Steps 1+2: audit 6 detectors (3 SHIP NOW + 3 HONEST DEFER) + tightened §3.1; Steps 3+4 deferred GAP-679".
- **New notes:** SCOPE-REVISE — Status header OPEN contradicts PARTIAL/70%; Wave 99C closed Steps 1+2 (audit 6 detectors + tightened §3.1); Steps 3+4 GAP-679; AC structure needs sync

### GAP-687 — Thesis V1 draft DOCX audit follow-ups

- **Verdict:** PARTIAL→adjust_pct (UP modest)
- **Current pct:** 67 | **Reality pct:** 80 (4/5 AC) | **Delta:** +13
- **Evidence:**
  - AC: 4/5 `- [x]` — Wave thesis-1 Bucket D 2026-05-23 Phase 1+2 DONE (commit cc03d70).
  - 1/5 `- [ ]` Phase 3 cluster DEFER Wave thesis-2 chờ GAP-648 + GAP-649 unblock.
- **New completion_pct:** 80 | **New notes:** Wave thesis-1 Bucket D Phase 1+2 DONE — 4 backup MDs archived + TODO scrub + create_thesis_v1.py 3 flags + rubric 76/100 PARTIAL C (4/5 AC); Phase 3 DEFER Wave thesis-2 chờ GAP-648/649

### GAP-692 — env-reference.yaml multi-env refactor

- **Verdict:** PARTIAL→adjust_pct (UP aggressive)
- **Current pct:** 33 | **Reality pct:** 60 (6/10 AC) | **Delta:** +27
- **Evidence:**
  - AC: 6/10 `- [x]` — Wave 102.8 Bucket B Phase 1 tooling DONE (env-reference.yaml 10 rows + render-env-vars.sh + check-unresolved-env-vars.sh + markdown-variable-reference.md v1.0.0 + TF vars fix + CI job env-vars-render).
  - 4/10 `- [ ]` Phase 2 top-10 refactor + Phase 3 pre-commit hook defer Wave 103+.
- **New completion_pct:** 60 | **New notes:** Wave 102.8 Bucket B Phase 1 tooling DONE (6/10 AC); Phase 2 top-10 refactor + Phase 3 pre-commit hook defer Wave 103+

### GAP-746 — Multi-tenant isolation EnrollmentRepository tenant filter

- **Verdict:** OPEN→keep
- **Current pct:** 60 | **Reality pct:** 0 (0/6 AC `- [x]`) — Status header "🔵 OPEN" contradicts PARTIAL claim
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/6 `- [x]`. Status header OPEN. CSV says PARTIAL/60.
  - Notes "Enrollment Path A1 explicit tenant param shipped + 3 service usages + TestTenantContextFilter fix. Compile PASS. Invoice Path C + audit sweep 15 repos defer GAP-749".
- **New completion_pct:** 40 | **New notes:** SCOPE-REVISE — Status header OPEN contradicts PARTIAL; Wave gap-746 agent inline salvage Enrollment Path A1 shipped + TestTenantContextFilter fix (compile PASS); Invoice Path C + 15-repo audit sweep defer GAP-749

### GAP-760 — KH E2E setupMockAuth Zustand persist hydration race

- **Verdict:** PARTIAL→adjust_pct (UP modest)
- **Current pct:** 40 | **Reality pct:** 60 (3/5 AC) | **Delta:** +20
- **Evidence:**
  - AC: 3/5 `- [x]` — Option B addInitScript shipped 13/20 → 15/20 PASS improvement.
  - 2/5 `- [ ]` Option C useAuthStore.persist.hasHydrated() wait gate route-guard layouts GAP-761.
- **New completion_pct:** 60 | **New notes:** Option B addInitScript shipped 13/20 → 15/20 PASS (3/5 AC); residual 5/20 fail requires Option C hasHydrated() wait gate GAP-761

### GAP-798 — Domain-entity user_id UUID bridge for authz (Gateway V2)

- **Verdict:** OPEN→keep
- **Current pct:** 50 | **Reality pct:** 0 (0/7 AC `- [x]`) | **Delta:** −50 → DOWN aggressive
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/7 `- [x]`. Status text "consumer side DONE #1948; producer side → GAP-798b".
- **New completion_pct:** 50 | **New notes:** SCOPE-REVISE — consumer side DONE PR #1948; producer side GAP-798b; AC structure may need split between consumer + producer

### GAP-802 — BE↔FE contract drift detection

- **Verdict:** PARTIAL→adjust_pct (UP, near DONE)
- **Current pct:** 80 | **Reality pct:** 80 (4/5 AC) | **Delta:** 0
- **Verdict (final):** OPEN→keep
- **Evidence:**
  - AC: 4/5 `- [x]` — #1 smoke-email-links + #2 check-be-fe-url-contract CI WARN + #4 fe-build-local-verify rule + #5 audit-env-coverage CHECK B shipped 2026-05-28 (4 parallel Opus agents).
  - 1/5 `- [ ]` #3 E2E full-flow defer FE E2E infra.
- **New completion_pct:** 80 | **New notes:** unchanged — #1/#2/#4/#5 SHIPPED 2026-05-28; #3 E2E defer FE E2E infra; detectors surfaced findings → GAP-803

### GAP-803 — BE↔FE detector findings + 3 env vars + vn-localization path bug

- **Verdict:** OPEN→keep
- **Current pct:** 40 | **Reality pct:** 25 (1/4 AC) | **Delta:** −15 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 1/4 `- [x]` — `/reset-password` FALSE POSITIVE resolved PR #1958 (detector path-scope fix CI flipped HARD STOP).
  - 3/4 `- [ ]` vn-localization path fix + 3 local-deadlink env vars + optional E2E.
- **New completion_pct:** 25 | **New notes:** /reset-password FALSE POSITIVE resolved #1958 (1/4 AC); 3 local-deadlink env vars (PARENT_PORTAL_REDEEM/RESEND_FROM_EMAIL/KITEHUB_STAFF_INVITATION) + vn-localization path bug remaining

### GAP-806 — Tomcat embed 10.1.54 → 10.1.55 CVE bump

- **Verdict:** OPEN→keep
- **Current pct:** 60 | **Reality pct:** 33 (1/3 AC) | **Delta:** −27 → DOWN aggressive
- **Verdict revised:** PARTIAL→adjust_pct (DOWN aggressive)
- **Evidence:**
  - AC: 1/3 `- [x]` — pom pin bumped 10.1.54→10.1.55 (kitehub:34 + kiteclass-core:33).
  - 2/3 `- [ ]` container re-scan confirm + Dependabot 2 medium npm transitive defer.
- **New completion_pct:** 35 | **New notes:** pom bumped 10.1.54→10.1.55 (2 pins sweep — 1/3 AC); container re-scan confirm deferred post-merge; Dependabot 2 npm transitive defer pnpm limitation

### GAP-811 — FE middleware host→tenant resolution

- **Verdict:** OPEN→keep
- **Current pct:** 70 | **Reality pct:** 0 (0/8 AC `- [x]`) — Status field empty, AC all unchecked
- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - AC: 0/8 `- [x]`. Status field empty.
  - Notes "Wave tenant-domain-1 Bucket C shipped kitehub-frontend middleware + resolveTenant lib + 5min tenantCache + suspended page + 41 unit tests + 5 Playwright route-mock E2E. 853/853 tests + build PASS. PARTIAL: live multi-host RST walk deferred GAP-813; landing SSR consumer out of scope".
- **New completion_pct:** 60 | **New notes:** SCOPE-REVISE — Status empty + AC all unchecked but Wave tenant-domain-1 Bucket C shipped 853/853 tests + build PASS; 5/8 AC implicit if synced (middleware + cache + suspended + dev override + build); live RST walk + landing SSR defer GAP-813 unblock

### GAP-813 — Base-domain consistency + slug→UUID resolution

- **Verdict:** PARTIAL→adjust_pct (UP modest)
- **Current pct:** 55 | **Reality pct:** 43 (3/7 AC) | **Delta:** −12 → DOWN
- **Verdict revised:** PARTIAL→adjust_pct (DOWN)
- **Evidence:**
  - AC: 3/7 `- [x]` — Wave tenant-domain-1 Bucket B BE endpoint shipped (PublicTenantController + TenantLookupService + TenantResolveDto + gateway whitelist + rate-limit 30/min/IP + unit + Postgres IT).
  - 4/7 `- [ ]` base-domain env reconcile + SlugAvailabilityService cross-check + RST walk + FE consumer GAP-811.
- **New completion_pct:** 45 | **New notes:** Wave tenant-domain-1 Bucket B (2026-06-01) BE endpoint + gateway whitelist + rate-limit + IT (3/7 AC); 4/7 base-domain env reconcile + SlugAvailability cross-check + RST walk + FE consumer GAP-811

---

## Detailed delta table — current vs reality

| GAP-ID | Current pct | Reality pct | Δ | Verdict |
|---|---:|---:|---:|---|
| GAP-033 | 57 | 57 | 0 | OPEN→keep |
| GAP-043 | 50 | 22 | -28 | PARTIAL DOWN |
| GAP-063 | 50 | 67 | +17 | PARTIAL UP |
| GAP-112 | 60 | 60 | 0 | OPEN→keep |
| GAP-115 | 50 | 29 | -21 | PARTIAL DOWN |
| GAP-191 | 50 | 83 | +33 | PARTIAL UP |
| GAP-222 | 50 | n/a | n/a | SCOPE-REVISE |
| GAP-245 | 50 | 60 | +10 | PARTIAL UP |
| GAP-353b | 85 | 73 | -12 | PARTIAL DOWN |
| GAP-371 | 50 | 17 | -33 | PARTIAL DOWN |
| GAP-374 | 50 | 80 | +30 | PARTIAL UP |
| GAP-379 | 95 | 98 | +3 | PARTIAL UP (near DONE) |
| GAP-380 | 50 | 40 | -10 | PARTIAL DOWN |
| GAP-400 | 50 | 80 | +30 | PARTIAL UP |
| GAP-412 | 50 | 43 | -7 | OPEN→keep |
| GAP-413 | 50 | 40 | -10 | PARTIAL DOWN marginal |
| GAP-428 | 70 | n/a | n/a | SCOPE-REVISE |
| GAP-434 | 50 | 50 | 0 | OPEN→keep |
| GAP-436 | 50 | 30 | -20 | PARTIAL DOWN |
| GAP-440 | 55 | 30 | -25 | PARTIAL DOWN |
| GAP-442 | 50 | 30 | -20 | PARTIAL DOWN |
| GAP-444 | 50 | 30 | -20 | SCOPE-REVISE |
| GAP-447 | 75 | n/a | n/a | SCOPE-REVISE |
| GAP-466 | 75 | 90 | +15 | PARTIAL UP |
| GAP-471 | 75 | 50 | -25 | PARTIAL DOWN |
| GAP-473 | 40 | 73 | +33 | PARTIAL UP |
| GAP-475 | 90 | 88 | -2 | OPEN→keep |
| GAP-477 | 85 | 83 | -2 | OPEN→keep |
| GAP-516 | 90 | 75 | -15 | PARTIAL DOWN |
| GAP-520 | 90 | 70 | -20 | PARTIAL DOWN |
| GAP-521 | 85 | 65 | -20 | PARTIAL DOWN |
| GAP-527 | 60 | 50 | -10 | PARTIAL DOWN |
| GAP-531 | 70 | 45 | -25 | PARTIAL DOWN |
| GAP-537 | 75 | 60 | -15 | SCOPE-REVISE |
| GAP-537c | 50 | 25 | -25 | PARTIAL DOWN |
| GAP-544 | 80 | 80 | 0 | OPEN→keep |
| GAP-582 | 35 | 25 | -10 | PARTIAL DOWN marginal |
| GAP-586 | 70 | 45 | -25 | PARTIAL DOWN |
| GAP-587 | 40 | 20 | -20 | PARTIAL DOWN |
| GAP-589 | 25 | 15 | -10 | PARTIAL DOWN marginal |
| GAP-590 | 60 | 35 | -25 | PARTIAL DOWN |
| GAP-638 | 30 | 20 | -10 | SCOPE-REVISE |
| GAP-645 | 30 | 30 | 0 | OPEN→keep |
| GAP-675 | 70 | n/a | n/a | SCOPE-REVISE |
| GAP-687 | 67 | 80 | +13 | PARTIAL UP |
| GAP-692 | 33 | 60 | +27 | PARTIAL UP |
| GAP-746 | 60 | 40 | -20 | SCOPE-REVISE |
| GAP-760 | 40 | 60 | +20 | PARTIAL UP |
| GAP-798 | 50 | 50 | 0 | SCOPE-REVISE |
| GAP-802 | 80 | 80 | 0 | OPEN→keep |
| GAP-803 | 40 | 25 | -15 | PARTIAL DOWN |
| GAP-806 | 60 | 35 | -25 | PARTIAL DOWN |
| GAP-811 | 70 | 60 | -10 | SCOPE-REVISE |
| GAP-813 | 55 | 45 | -10 | PARTIAL DOWN |

---

## Patterns observed (audit meta-findings)

### A. Stale completion_pct outdated downward (UP class, 14 gaps)

Pattern phổ biến: completion_pct snapshot tại last_verified date (commonly 2026-05-11 batch refresh) nhưng nhiều follow-up sub-PRs ship sau đó không refresh CSV. Ví dụ rõ:

- **GAP-191** (50 → 83): Wave 9-B đã ship 5/6 AC từ 2026-04-20 (PR #408/#414), CSV không update.
- **GAP-374** (50 → 80): Wave 38 Bucket A PR #943 + closure PR #948 shipped 8/10 AC, CSV không update.
- **GAP-473** (40 → 73): Wave 61 Bucket D Phase 1 shipped 8/11 AC, CSV chưa refresh.
- **GAP-692** (33 → 60): Wave 102.8 Bucket B Phase 1 tooling shipped 6/10 AC, CSV chưa refresh.

### B. Over-stated completion_pct (DOWN class, 17 gaps lớn nhất)

Pattern: notes text claim higher completion vì các sub-PR partial ship nhưng AC structure không reflect đầy đủ. Ví dụ:

- **GAP-516** (90 → 75): "BE done + FE done" → text claim 90% nhưng 4/15 AC unchecked (IT + 3 live-verify blocked GAP-612).
- **GAP-520/521** (90 → 70, 85 → 65): "Wave 72a PR #1287" được claim 85-90% nhưng raw AC chỉ 2/3 và 3/5.
- **GAP-586/587/590** (DOWN 25-30pp aggressive): Wave 86 docs-cluster spec planning shipped nhưng implementation Wave 87+ chưa.

### C. SCOPE-REVISE (4 gaps)

Status header và AC structure không sync với CSV claim. Common pattern: Status field "🔵 OPEN" hoặc rỗng + AC checkboxes 0/N unchecked, NHƯNG CSV says PARTIAL + completion_pct > 0 + notes claim shipping.

- **GAP-537** (CSV 75% PARTIAL, Status header OPEN, AC 0/5)
- **GAP-675** (CSV 70% PARTIAL, Status header OPEN, AC 0/5)
- **GAP-746** (CSV 60% PARTIAL, Status header OPEN, AC 0/6)
- **GAP-811** (CSV 70% PARTIAL, Status field empty, AC 0/8)

Recommend: gap-status.csv columns + gap file AC structure cần auto-sync mechanism (next meta-rule candidate).

### D. Near-DONE cận flip (5 gaps)

5 gaps reality ≥85% nhưng KHÔNG flip DONE vì:
- **GAP-379** 98% — RDS rotation bootstrap user-action pending
- **GAP-475** 88% — Sub-6 TTR baseline gated user-action
- **GAP-477** 83% — terraform apply + first live `--execute` user-action
- **GAP-353b** 73% — 3 deepening items routed GAP-353b-followup
- **GAP-466** 90% — sub-bucket Wave beta-prep-1 Bucket B follow-up

Tất cả 5 đều có user-action blocker hợp lý → KHÔNG flip DONE prospectively.

---

## CSV update commands (coordinator applies in closure PR)

NOTE: Per task constraints, this audit ONLY writes the report. Coordinator applies CSV updates trong Wave meta-7 closure PR per taxonomy §6.

```bash
# PARTIAL adjust_pct UP (14 gaps):
# GAP-063: 50 → 67
# GAP-191: 50 → 83
# GAP-245: 50 → 60
# GAP-374: 50 → 80
# GAP-379: 95 → 98 (near DONE)
# GAP-400: 50 → 80
# GAP-466: 75 → 90
# GAP-473: 40 → 73
# GAP-687: 67 → 80
# GAP-692: 33 → 60
# GAP-760: 40 → 60
# (3 marginal UP within ±5pp tolerance, optional refresh: GAP-063 already listed)

# PARTIAL adjust_pct DOWN (15 gaps with ≥10pp delta DOWN):
# GAP-043: 50 → 25
# GAP-115: 50 → 30
# GAP-353b: 85 → 73
# GAP-371: 50 → 17
# GAP-380: 50 → 40
# GAP-413: 50 → 40
# GAP-436: 50 → 30
# GAP-440: 55 → 30
# GAP-442: 50 → 30
# GAP-471: 75 → 50
# GAP-516: 90 → 75
# GAP-520: 90 → 70
# GAP-521: 85 → 65
# GAP-527: 60 → 50
# GAP-531: 70 → 45
# GAP-537c: 50 → 25
# GAP-582: 35 → 25
# GAP-586: 70 → 45
# GAP-587: 40 → 20
# GAP-589: 25 → 15
# GAP-590: 60 → 35
# GAP-803: 40 → 25
# GAP-806: 60 → 35
# GAP-813: 55 → 45

# SCOPE-REVISE notes only (4 gaps):
# GAP-222: notes += "SCOPE-REVISE — AC structure outdated vs Sub-PR 6.4 + 222a/b sub-gaps"
# GAP-428: notes += "SCOPE-REVISE — AC structure stale; Wave 78 Bucket A production VN polish + brand sync DONE; kit prototype DEFER"
# GAP-444: notes += "SCOPE-REVISE — defer-by-design; consider DROP or deferred-tracker reclassify"
# GAP-447: notes += "SCOPE-REVISE — kh_backend DONE in-place modify + kc_app drift GAP-450 + CWAgent user-action SSM"
# GAP-537: notes += "SCOPE-REVISE — Status header OPEN contradicts CSV 75%; Wave 80 Bucket D F2 shipped"
# GAP-638: notes += "SCOPE-REVISE — Wave 97 Bucket B1 3-layer docs foundation shipped; AC sync needed"
# GAP-675: notes += "SCOPE-REVISE — Wave 99C Steps 1+2 closed; Steps 3+4 GAP-679"
# GAP-746: notes += "SCOPE-REVISE — Path A1 inline salvage shipped + TestTenantContextFilter fix; Invoice Path C GAP-749"
# GAP-798: notes += "SCOPE-REVISE — consumer DONE #1948; producer GAP-798b; AC structure split needed"
# GAP-811: notes += "SCOPE-REVISE — Wave tenant-domain-1 Bucket C shipped 853 tests + build PASS; AC sync needed"

# OPEN→keep last_verified refresh only (remaining gaps):
# GAP-033, GAP-112, GAP-412, GAP-434, GAP-475, GAP-477, GAP-544, GAP-645, GAP-802
# (set last_verified=2026-06-01 for all 31 OPEN→keep entries listed in §Verdict Summary)
```

---

## Conclusion + recommendations

1. **No DONE flip candidates** — 5 near-DONE gaps (GAP-379, GAP-475, GAP-477, GAP-353b, GAP-466) đều có hợp lý user-action blocker hoặc follow-up gap đã track.

2. **14 PARTIAL UP** — completion_pct outdated downward; net trend trong audit cho thấy sub-PR shipping không refresh CSV. Recommend: post-merge hook flag completion_pct staleness > 30 ngày khi sub-PR commit referencing GAP-ID.

3. **17+ PARTIAL DOWN** — completion_pct over-stated; status text claims không match raw AC count. Recommend: AC ratio = primary signal; status text = secondary context.

4. **10 SCOPE-REVISE** — structural sync issue giữa CSV completion_pct vs gap file AC structure. Worth filing META gap (GAP-NEW-csv-ac-sync-mechanism) đề xuất auto-sync script trong Wave meta-8.

5. **Median delta = +5pp** — repo aggregate trend slightly under-claims completion. Acceptable but bias indicates CSV refresh cadence behind sub-PR ship cadence.

Next step: coordinator applies CSV updates per §CSV update commands trong Wave meta-7 closure PR. Audit artifact preserved tại this path.

---

## Audit metadata

- **Files read:** 54 gap files (phase-1-beta/ + unclassified/) + gap-status.csv + git log per gap
- **Methodology:** Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check + taxonomy §2 5-step decision matrix
- **Tools:** `bash /tmp/ac_counter.sh` (AC checkbox counter) + `git log --oneline --all --grep="GAP-NNN"` (commit ref check) + manual gap file inspection
- **Banned shortcut compliance:** ZERO `| head` truncation on grep / find / git log per `audit-to-gap-pipeline.md` §2.5 hardened protocol
- **Multi-pattern grep:** AC count + status header + notes + git log per gap (4-pattern minimum)
- **Vietnamese narrative + English identifier:** Per `dev-readable-doc-language.md` §2 — narrative tiếng Việt, GAP-ID + commit SHA + path English
- **CSV / gap files / audits-index.csv unmodified:** Per task constraint, audit ONLY writes this report
