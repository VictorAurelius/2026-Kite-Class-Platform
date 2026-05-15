---
title: Wave 86 — v1.0.0-rc.1 Tag Preflight + Final Audit Suite + Beta Cohort Invite
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 86
waves: [86]
risk_profile: HIGH (release candidate tag → production traffic; downstream cohort invite irreversible)
trigger: Wave 85 multi-tenant + performance baseline CLOSED; pre-launch hardening checklists ALL satisfied
estimated_wall_clock: 14-20h
---

# Wave 86 — v1.0.0-rc.1 Tag Preflight + Beta Cohort Invite

## 1. Brainstorm

**Q1 (goal):** Final pre-launch checklist execution + tag `v1.0.0-rc.1` + invite first 5 beta tenants. v1.0.0-rc gate = pre-launch hardening checklists all satisfy (`pre-launch-{auth,secrets,owasp-rest,infra,dependency}-hardening-checklist.md`) + post-wave audit suite ≥80 across all categories + dev self-test full 126 rows complete.

**Q2 (decision context):** Wave 81-85 đã ship backend production + FE rebuild + RLS + performance + ops baseline. Wave 86 = consolidation wave — không feature mới, chỉ audit + tag + invite. Spring Boot dep bump GAP-440 ship cùng (last opportunity before tag). User manual P2/P3/Admin GAP-537c ship cùng (beta tenants cần). AWS Activate $1k credit resubmit GAP-412 (denied 2026-05-10 due to kitehub.vn stale ref Wave 81 fixed).

**Q3 (risks):**
- Tag prematurely → production traffic on un-hardened backend
- Beta cohort invite < ready → first-impression damage
- Dep bump introduce regression → revert + re-tag overhead
- AWS Activate denial #2 → financial cushion gone
- Outside-in audit per rule §3 — wave touch beta user (P2/P3 manager) UX path → SHOULD trigger outside-in coverage

## 2. Task Breakdown

| Bucket | Item | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A** | Outside-in audit (persona-based-business-review + benchmark + simulation) | parallel agents | 1h | First |
| **B** | GAP-440 Spring Boot 3.5.14 → latest dep bump + smoke test | coordinator | 2-3h | After A |
| **C** | GAP-537c P2 Owner + P3 Manager screenshots capture + Tier 2 annotation (defer from Wave 79/80) | coordinator + Playwright | 2-3h | Parallel B |
| **D** | GAP-412 AWS Activate $1k credit resubmit (kitehub.me URL post-Wave 81 fix) | user-action AWS Console | 30min | Parallel |
| **E** | Final pre-launch hardening 5 checklist verification — each script + manual sweep | coordinator | 3-4h | After A-D |
| **F** | Tag `v1.0.0-rc.1` + automated release CI workflow trigger | user-action + coordinator | 1h | After E |
| **G** | First 5 beta cohort invite (manual = Solo teacher P1 ×2 + Center Owner P2 ×3 from invite waiting list) | user-action + coordinator verify | 1-2h | After F (DNS warm) |
| **H** | Post-cohort-invite monitoring + first-incident response plan ready | coordinator | 2h | After G |

## 3. Scope — Bucket detail

### Bucket A — Outside-in audit

- `persona-based-business-review`: 5 personas × 5 questions focusing on first-30-min experience
  - Anonymous prospect → "What stops me converting?"
  - P1 Solo Teacher invited → "First class setup confusion?"
  - P2 Center Owner invited → "Onboarding wizard feels overwhelming?"
  - P3 Manager invited by Owner → "Permissions feel right?"
  - Platform Admin → "Audit log coverage feels safe?"
- WebSearch benchmark: VN SaaS edu industry signup conversion + churn studies (Zalo OA, Misa, Edupia)
- `simulation-gap-finder`: failure modes during first-100-user load (DB connection pool exhaust, email queue backlog, etc.)

### Bucket B — GAP-440 Spring Boot bump

- Check latest Spring Boot 3.5.x patch release (security fixes only, no 3.6.x jump pre-launch)
- Pin `pom.xml` parent + dependencies; run mvn dependency:tree để verify transitive consistency
- Full smoke test post-bump per Wave 81 Bucket F pattern
- Dependabot vulnerability sweep: target 0 HIGH CVE per `pre-launch-dependency-hardening-checklist.md`

### Bucket C — GAP-537c P2/P3 screenshots

- Playwright capture 8 screens cho P2 Owner (signup → onboarding wizard → first class create)
- Playwright capture 6 screens cho P3 Manager (login → dashboard → invite accept)
- Tier 2 annotation overlay (Wave 79 standard) — arrows + numbered steps
- VN narrative captions per `dev-readable-doc-language.md`
- PDF render via `scripts/render-user-manual-pdf.sh p2-owner` + `p3-manager`

### Bucket D — GAP-412 AWS Activate resubmit

- USER ACTION: submit Activate Founders Pack application với new kitehub.me URL
- Application body cite: "Wave 81 deploy CLOSED; production live; review again"
- Expected: $1k credit + technical support tier

### Bucket E — Pre-launch hardening verification

- `pre-launch-auth-hardening-checklist.md` Cat 4 — all 9 items PASS
- `pre-launch-secrets-hardening-checklist.md` Cat 2 — all 8 items PASS (KMS CMK PARTIAL acceptable v1)
- `pre-launch-owasp-rest-hardening-checklist.md` Cat 3 — all REST best practices PASS
- `pre-launch-infra-hardening-checklist.md` Cat 5 — all 9 items PASS (CSP report-only acceptable v1)
- `pre-launch-dependency-hardening-checklist.md` Cat 1 — 0 HIGH CVE PASS
- Each checklist: script run + grep evidence + manual sweep result trong audit report

### Bucket F — Tag v1.0.0-rc.1

- `git tag -s v1.0.0-rc.1 -m "Release candidate 1 — Phase 1 BETA — first 5 cohort invite"`
- Per `versioning-policy.md` §3
- Per `release-deploy-standard.md` §3.4 first PRODUCTION = MAJOR scope; 18-item checklist verified
- Automated release CI workflow `release.yml` triggered → ECR images tagged `v1.0.0-rc.1` + Helm chart bumped
- GitHub Release với changelog + DOMAIN_MILESTONE_AUDIT trailer per `post-wave-audit-mandate.md` §2.4.2

### Bucket G — Beta cohort invite (5 tenants)

- 5 hand-picked tenants từ invite waitlist (VN edu trainers):
  - 2 Solo Teacher P1 (English center freelance)
  - 3 Center Owner P2 (small center 50-100 students)
- Send invite via Resend production (verified Wave 83 Bucket F)
- Welcome page: TOS + Privacy + Beta disclaimer + "feedback channel" link
- Coordinator monitor: first-hour signup completion rate + first-day churn rate

### Bucket H — Monitoring + incident response

- Grafana dashboard `KiteHub-Production` (from Wave 84) → on-call rotation set
- Incident response runbook `documents/05-guides/operations/incident-response-runbook.md` updated với Wave 86 deploy specific paths
- First-incident SLA: <30 min MTTD (mean time to detect) + <2h MTTR (mean time to recover)

## 4. State-Check Evidence

| Symbol | Verification | Verdict |
|---|---|---|
| 5 pre-launch checklist files | `ls .claude/rules/pre-launch-*-checklist.md` | ✅ exists (5 files) |
| `release.yml` workflow | `ls .github/workflows/release.yml` | 🆕 to-be-created (or verify exists) |
| AWS Activate ticket history | `documents/05-guides/account-prep/03-aws-activate-history.md` | ✅ exists (Wave 81 denial) |
| P2/P3 screenshot scripts | `ls scripts/capture-user-manual-screenshots.sh` | ✅ exists Wave 80 |
| Beta cohort invite list | `documents/01-business/kitehub/beta-cohort/p1-cohort-list.md` | 🆕 to-be-created |

## 5. Acceptance Gate

| Criterion | Met when |
|---|---|
| Outside-in audit findings integrated | 4 personas reports + benchmark + simulation findings → AC additions |
| GAP-440 Spring Boot bump | dep tree clean + 0 HIGH CVE + smoke test pass |
| GAP-537c screenshots | 14 screens captured + annotated + PDF rendered |
| GAP-412 AWS Activate | application submitted (outcome separate) |
| 5 pre-launch checklists | all categories PASS or PARTIAL với follow-up gap |
| `v1.0.0-rc.1` tag | tag signed + release CI workflow green + ECR images tagged |
| 5 beta cohort invited | invite email delivered + Resend dashboard shows opens |
| Incident response ready | runbook updated + dashboard live + SLA targets defined |
| Post-wave audit suite | all categories ≥80 v2 format |

## 6. Cross-link

- Wave 85 closure: `wave-2026-05-15-85-multi-tenant-security-perf.md`
- `release-deploy-standard.md` §3.4 (MAJOR + first PROD)
- `versioning-policy.md` §3
- `outside-in-coverage-trigger.md` §3
- 5 pre-launch hardening checklists (Cat 1-5)
- `release-1-deploy-plan.md`
- Incident response runbook

## 5. Verification Gates

See §5 Acceptance Gate table above — bucket-level criteria. Post-wave audit per `post-wave-audit-mandate.md` §2.1 (Backend/FE/Security/Performance categories) per bucket scope.

## 6. Agent Spawn Pattern

Sequential coordinator execution where buckets share files (deploy state, gateway config). Parallel background agents for isolated FE work (cookie consent banner, screenshots capture) per `agent-background-spawn-default.md` §1. Outside-in audit agents (per `outside-in-coverage-trigger.md` §3) spawn parallel background when wave triggers (Wave 85/86 mark §1 Q4).

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md`:
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- ROADMAP §🎯 Snapshot prepend
- gap-status.csv sync per bucket DONE flips
- `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- Session handoff `2026-05-XX-post-wave-NN-handoff.md` NEW
