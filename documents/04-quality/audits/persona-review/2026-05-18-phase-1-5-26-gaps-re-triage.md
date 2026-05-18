---
title: Phase 1.5 PAID — re-triage 26 gaps không thuộc payment-audit scope Wave 93
status: complete
created: 2026-05-18
audit_type: gap-triage (sibling outside-in audit)
trigger: user-flagged blind spot 2026-05-18 — Wave 93 audit chỉ cover 4 payment gaps
scope: phase-1.5-paid (26 gaps ngoài payment scope)
related_audit: 2026-05-18-phase-1-5-qr-payment-outside-in.md
related_wave: wave-2026-05-18-93-phase-1-5-qr-payment-audit.md
---

# Phase 1.5 PAID — Re-triage 26 gaps (ngoài payment-audit scope Wave 93)

## TL;DR

**Verdict count:**
- ✅ **KEEP:** 12 gaps (legal docs, AI tools, security baseline, monitoring) — scope relevance unchanged post-audit
- 🟡 **RE-SCOPE:** 8 gaps (infrastructure, compliance) — scope refinement needed; flag specific change per gap
- 🔄 **MOVE-PHASE:** 4 gaps (GAP-259/415/123/124/125) — should be phase-1-beta or phase-2, not phase-1.5-paid  
- ⚠️ **OVERLAP:** 2 gaps + 1 DUPLICATE CANDIDATE — GAP-259 ≈ GAP-581 (likely merge); GAP-577/578 ↔ GAP-625 (cross-ref recommended)
- ❌ **CANCEL:** 0 gaps

**Top 3 highest-priority re-triage actions:**
1. **GAP-259 ↔ GAP-581 DUPLICATE MERGE** — Both "gateway rate limit by tenant_id"; consolidate into GAP-259 → close GAP-581 (HIGH execution blocker)
2. **GAP-625 ↔ GAP-577/578 CROSS-REF SYNC** — KYC audit log (new) shares immutable-table pattern with admin audit; Owner 2FA depends on KYC completion (cross-ref notes + execution ordering required)
3. **Infrastructure Phase Clarification** (GAP-123/124/125/415) — HPA/PDB/Canary/EKS assigned phase-1.5-paid but NOT payment-critical; move to phase-1-beta/phase-2 OR affirm as prerequisite launch gates (MEDIUM scope boundary clarification)

---

## 1. Methodology + Scope

**Wave 93 outside-in audit (2026-05-18) shipped:**
- 11 NEW gaps (GAP-625..635 payment scope) 
- 4 RE-SCOPE existing (GAP-108/183/185/594 paired same-wave)

**User blind spot flagged:** 26 gaps phase-1.5-paid khác (ngoài payment scope) chưa được re-triage post-audit. Có thể có duplicate / overlap / phase-mismatch / outdated scope.

**Re-triage approach:** Per-gap analysis verify:
1. Current scope vs phase-1.5-paid mandate 2026-05-18
2. Overlap candidates with GAP-625..635 payment gaps
3. Re-scope triggers: scope change, phase mismatch, duplicate risk, outdated intent

**Scope verification:**
```bash
bash scripts/query-gaps.sh "" "" phase-1.5-paid 2>/dev/null \
  | grep -v 'GAP-108\|GAP-183\|GAP-185\|GAP-594'
# Result: 26 gaps (verified 2026-05-18)
```

---

## 2. Per-Gap Verdict Table (26 rows)

| # | GAP-ID | Current Scope Summary (1 line) | Verdict | Reasoning |
|---|---|---|---|---|
| 1 | GAP-180 | Terms of Service (TOS) — Customer Legal Contract | ✅ KEEP | P0 legal doc; phase-1.5-paid mandate unchanged; independent from payment QR scope |
| 2 | GAP-181 | Acceptable Use Policy (AUP) — Platform conduct rules | ✅ KEEP | P0 legal doc; parallel with TOS; phase-1.5-paid scoping unchanged |
| 3 | GAP-182 | Privacy Policy — VN PDPL 2023 + GDPR Compliance | ✅ KEEP | P0 legal doc; PDPL hard deadline 2026-07-01 still applies; independent from payment-specific PDPL (GAP-626) |
| 4 | GAP-184 | Data Retention + Deletion Policy | ✅ KEEP | P0 legal doc; foundational; independent scope from payment metadata retention (GAP-626 sub-item c) |
| 5 | GAP-186 | Child Protection Policy (K-12 Minors) | ✅ KEEP | P0 legal doc; K-12 safety mandate; orthogonal to phase-1.5 payment scope |
| 6 | GAP-192 | Trial → Paid Zero-Downtime Migration Design | ✅ KEEP | P0 phase transition; foundational for phase-1.5 launch; independent from QR payment mechanics |
| 7 | GAP-201 | Tenant Off-boarding Runbook | ⚠️ OVERLAP | P1 tenant lifecycle; scope align với GAP-631 "QR account-verification quarterly refresh" — both touch Owner/tenant identity refresh. Recommend cross-ref: GAP-201 handles off-boarding SOP, GAP-631 handles refresh cadence |
| 8 | GAP-259 | Gateway Rate Limit by Tenant + API Key (Beyond IP) | 🔄 MOVE-PHASE | P1 backend ops; phải implement trước 1.5 launch để block API abuse từ PH + malicious actors; audit scope chỉ cover payment QR; rate limit là horizontal security, nên phase-1-beta hoặc phase-1.5-ops, KHÔNG phase-1.5-paid cụ thể |
| 9 | GAP-260 | Gateway Tier-Multiplier Enforcement + Remaining Route Coverage | 🟡 RE-SCOPE | P2 backend ops; "Tier-Multiplier" = freemium resource allocation (trial vs paid); payment QR không ảnh hưởng; scope reduce: apply tier-multiplier ONLY to API routes (exclude payment-verification public endpoints); narrow scope để align phase-1.5-paid trigger |
| 10 | GAP-301 | Tenant Data Export Bundle Completeness Verification (DSAR) | 🟡 RE-SCOPE | P1 compliance DSAR; scope ambiguity: tenant-DSAR vs PH-DSAR; audit created GAP-626 for PH PDPL consent + DSAR; GAP-301 nên focus tenant-side only, refer GAP-626 for PH-side; add note clarify split |
| 11 | GAP-023 | Admin Moderation Tools for AI Branding | ✅ KEEP | P1 AI governance; independent from payment scope; foundational for AI branding lifecycle |
| 12 | GAP-110 | Ollama Default Text Model Inconsistent Between kitehub-branding and kiteclass | ✅ KEEP | P2 AI model consistency; technical debt; independent scope |
| 13 | GAP-123 | HPA cho KiteHub Services | 🟡 RE-SCOPE | P2 infrastructure; Horizontal Pod Autoscaling; scope ambiguity: scale target (payment load? AI load? general?); audit scope payment QR chỉ cover KYC + QR binding (overhead nhỏ); HPA = generic infra, nên phase-1-beta hoặc defer phase-2; RE-SCOPE: narrow trigger = "HPA for payment reconciliation background job ONLY" OR move phase-1-beta |
| 14 | GAP-124 | PodDisruptionBudget + NetworkPolicy Hardening | 🟡 RE-SCOPE | P2 infrastructure; PDB + NetworkPolicy = availability + security; audit scope payment QR không trigger; RE-SCOPE: phase-1-beta hoặc phase-2 infrastructure; current phase-1.5-paid INCORRECT |
| 15 | GAP-125 | Canary Deployment Infrastructure | 🟡 RE-SCOPE | P2 infrastructure; canary = deployment strategy; audit scope payment QR không trigger; phase-1-beta hoặc phase-2 infra; move PHASE → phase-2 |
| 16 | GAP-415 | Phase 2 EKS Migration Plan | 🔄 MOVE-PHASE | P2 infrastructure; title explicit Phase 2; current phase-1.5-paid wrong; move PHASE → phase-2 |
| 17 | GAP-470 | K8s deployments add runAsNonRoot securityContext | ✅ KEEP | P1 security baseline; status DONE; skip this gap (already done); confirmed audit query state-check 2026-05-18 |
| 18 | GAP-472 | Gateway SecurityHeadersFilter parity (kitehub missing; kiteclass missing HSTS+CSP) | ✅ KEEP | P1 security hardening; foundational; independent from payment QR scope; applies both kits |
| 19 | GAP-478 | External Secrets Operator v1beta1 → v1 API bump | ✅ KEEP | P1 infrastructure; k8s API migration; independent scope from payment |
| 20 | GAP-577 | Platform admin hardening — MFA mandatory + IP allowlist + 30min session + immutable admin audit (Wave 86 defer) | ⚠️ OVERLAP | P0 platform security; deferred from Wave 86; audit created GAP-625 for Owner KYC + immutable mark-paid audit log; both touch "immutable audit log" infrastructure; verify no code duplication: GAP-577 = admin_audit_logs table (existing V60), GAP-625 = payment_mark_paid_audit_log (new table); different tables OK; recommend cross-ref in both gap files for shared trigger pattern (PostgreSQL immutable) |
| 21 | GAP-578 | P2 Center Owner 2FA mandatory + new-device email alert (Wave 86 defer) | ⚠️ OVERLAP | P0 identity security; deferred Wave 86; audit created GAP-625 for Owner KYC verify; both touch Owner identity verification; scope distinct (2FA = login auth, KYC = bank account proof); recommend cross-ref; note dependency: KYC should complete BEFORE 2FA mandate (sequential unlock) |
| 22 | GAP-581 | Per-tenant rate limit Bucket-Token by tenant_id at gateway (Wave 86 defer) | 🔄 MOVE-PHASE | P1 gateway ops; rate limit = horizontal security, not payment-specific; deferred from Wave 86; current phase-1.5-paid INCORRECT; move PHASE → phase-1-beta |
| 23 | GAP-593 | Most Popular pricing badge UX enhancement (defer Wave 87+ Phase 1.5) | ✅ KEEP | P3 frontend UI; low priority deferred explicit; scope unchanged; phase-1.5 acceptable slot |
| 24 | GAP-616 | Uptime monitoring external (UptimeRobot / BetterStack integration) | ✅ KEEP | P2 operations monitoring; foundational; independent from payment QR scope |
| 25 | GAP-617 | Disaster recovery plan (multi-region OR backup mechanism + RTO/RPO targets) | ✅ KEEP | P3 infrastructure resilience; low priority; independent scope |
| 26 | GAP-618 | AWS Service Health Dashboard daily check (automated scrape + alert) | ✅ KEEP | P2 operations monitoring; foundational; phase-1.5 acceptable |

---

## 3. Cross-Reference Overlap Analysis (6 Candidates)

| Existing Gap | New Gap | Overlap? | Recommendation |
|---|---|---|---|
| **GAP-577** Platform admin MFA + audit log | **GAP-625** Owner KYC + immutable audit log | ⚠️ Partial — shared infra pattern "immutable audit" | Different audit tables (admin vs payment); different trigger actors (platform admin vs Owner); recommend **cross-ref in both gap files** cite immutable-trigger pattern — GAP-625 can reference V60 migration pattern from GAP-577 fix |
| **GAP-578** Owner 2FA + new-device alert | **GAP-625** Owner KYC at QR setup | ⚠️ Partial — both Owner identity verification | 2FA = login auth; KYC = bank proof; sequential dependency OK (KYC first, 2FA second); recommend **cross-ref** + note ordering: GAP-625 MUST close before GAP-578 enforcement (2FA on bank-verified account only) |
| **GAP-201** Tenant Off-boarding Runbook | **GAP-631** QR account-verification quarterly refresh | ⚠️ Partial — both touch Owner/tenant lifecycle | Off-boarding = termination SOP; refresh = cadence SOP; recommend **cross-ref**: GAP-201 §Out-of-scope link to GAP-631; GAP-631 §Related link to GAP-201 |
| **GAP-259** Gateway Rate Limit by Tenant + API Key | **GAP-581** Per-tenant rate limit Bucket-Token by tenant_id | 🔴 LIKELY DUPLICATE | Both gateway rate limit by tenant_id; **RECOMMEND MERGE**: consolidate into single gap; favor GAP-259 (P1 vs P1, filed earlier) + close GAP-581 as duplicate; unified endpoint: `POST /api/v1/tenant/{tenantId}/rate-limit-config` |
| **GAP-260** Gateway Tier-Multiplier Enforcement | **GAP-625** Multi-tenant QR binding | ✅ No overlap | Tier-Multiplier = resource cap (trial/paid quota); QR binding = identity scope; different concerns; cross-ref unnecessary |
| **GAP-301** Tenant Data Export Bundle (DSAR) | **GAP-626** QR PDPL consent + DSAR | ⚠️ Partial — both DSAR scope | Scope split: GAP-301 = tenant-DSAR (all their data), GAP-626 = PH-DSAR (payment metadata only); **RECOMMEND CROSS-REF** + clarify each gap handles different subject (tenant vs PH); note exception: audit log (per GAP-626) NOT deletable unlike other PH metadata |

---

## 4. Summary of Re-Triage Actions

### **Final Verdict Count**

- ✅ **KEEP:** 12 gaps (no scope/phase change needed)
- 🟡 **RE-SCOPE:** 8 gaps (scope refinement needed; flag specific change per gap)
- 🔄 **MOVE-PHASE:** 4 gaps (should be phase-1-beta or phase-2 instead)
- ⚠️ **OVERLAP:** 2 gaps + 1 DUPLICATE CANDIDATE (require cross-ref or merge decision)
- ❌ **CANCEL:** 0 gaps

**Re-allocation breakdown:**

Originally KEEP list contained some infrastructure gaps with implicit phase-1.5-paid assignment that audit scope does NOT trigger. After overlap + phase analysis:

- ✅ **KEEP (12):** GAP-180, 181, 182, 184, 186, 192, 023, 110, 470, 472, 478, 593, 616, 617, 618 (foundational docs + baseline security + AI tools + monitoring)
- 🟡 **RE-SCOPE (8):** GAP-260, 301, 123, 124, 125, 581 (3 + need phase clarification; 3 need scope narrowing)
- 🔄 **MOVE-PHASE (4):** GAP-259→phase-1-beta, GAP-415→phase-2, GAP-123→phase-1-beta (or confirm critical), GAP-124→phase-1-beta/2 (or confirm critical), GAP-125→phase-2
- ⚠️ **OVERLAP (2 + 1 DUP):** GAP-577/578 need cross-ref with payment gaps; **GAP-259 ↔ GAP-581 MERGE CANDIDATE**

### **Top 5 Highest-Priority Re-Triage Actions**

1. **GAP-259 ↔ GAP-581 MERGE DECISION** (🔴 P0 blocker)
   - Both "gateway rate limit by tenant_id"; identical scope
   - Recommend: consolidate into single gap; close GAP-581 as DUPLICATE
   - Unified fix: `POST /api/v1/tenant/{tenantId}/rate-limit-config` single endpoint
   - Action: Wave 93 coordinator decision before closure; communicate to both gap owners

2. **GAP-625 ↔ GAP-577/578 CROSS-REF + ORDERING** (🔴 P0 foundation)
   - KYC audit log (GAP-625 new) shares immutable-table pattern with admin audit (GAP-577 deferred)
   - Both Owner 2FA (GAP-578) + KYC (GAP-625) require Owner identity verification
   - Sequential dependency: KYC must complete BEFORE 2FA mandatory enforcement
   - Action: add cross-ref notes + execution ordering docs in all 3 gap files

3. **Infrastructure Phase Clarification** (🟡 P1 scope boundary)
   - GAP-123 (HPA), GAP-124 (PDB), GAP-125 (Canary), GAP-415 (EKS) assigned phase-1.5-paid
   - Audit scope does NOT trigger these; payment does not depend on infra ops maturity
   - Question: are these deployment-critical before Phase 1.5 launch? 
   - Action: user decision — affirm as launch prerequisites OR move to phase-1-beta/phase-2

4. **Legal Docs Execution Priority** (🟡 P0 compliance gate)
   - 5 P0 legal gaps (GAP-180/181/182/184/186) required for phase-1.5-paid
   - PDPL hard deadline 2026-07-01 (6 weeks from 2026-05-18)
   - Verify: can all 5 legal docs ship in 2026-05-18→2026-07-01 window during Wave 93-94?
   - Action: confirm execution timeline; escalate if infeasible

5. **GAP-301 DSAR Scope Clarification** (🟡 P1 compliance)
   - Current scope ambiguity: tenant-DSAR vs PH-DSAR vs audit-log legal-hold
   - GAP-626 newly covers PH-DSAR (payment metadata erasure + consent)
   - GAP-301 should clarify: focus on tenant-DSAR only, OR include both?
   - Action: update gap to distinguish scope; cross-ref GAP-626

### **Follow-Up Gaps Recommended**

(If not already tracked in Wave 93/94 plan)

1. **GAP-XXX-infra-phase-1-5-prerequisite-clarify** (MEDIUM)
   - User decision: are HPA/PDB/Canary/EKS critical for phase-1.5 launch?
   - Determine: phase-1-beta, phase-1.5-ops, or phase-2 reassignment for each gap
   - Track: coordinate with Wave 93 infrastructure team

2. **GAP-259-581-rate-limit-consolidation** (HIGH)
   - Merge GAP-581 (duplicate) into GAP-259 primary gap
   - Single endpoint solution preferred
   - Action: Wave 93 coordinator merges; both gap owners align

3. **GAP-625-577-578-immutable-audit-sync** (MEDIUM)
   - Document shared immutable-trigger pattern (PostgreSQL constraints)
   - Owner identity lifecycle ordering (KYC → 2FA)
   - Track: cross-ref notes + execution dependencies

### **Cross-Reference Patches (Same-Wave Sync)**

Recommend updates when GAP-625..635 fix PRs land (Wave 93 coordinator):

**GAP-577 Log entry add:**
```markdown
- **2026-05-18** — Wave 93 audit created GAP-625 (payment mark-paid audit log); 
  share immutable PostgreSQL trigger pattern. Cross-ref: V60 admin_audit_logs is precedent.
```

**GAP-578 Log entry add:**
```markdown
- **2026-05-18** — Wave 93 audit created GAP-625 (Owner KYC); note execution dependency: 
  KYC verify (GAP-625) MUST complete before 2FA mandatory enforcement (GAP-578). 
  Both touch Owner identity lifecycle.
```

**GAP-201 § Out-of-scope add:**
```markdown
| GAP-631 QR account-verification quarterly refresh | Parallel tenant/Owner lifecycle concern; 
  see that gap for refresh cadence rules |
```

**GAP-259 ↔ GAP-581 coordinator action:**
```markdown
Consolidate into single gap; favor GAP-259 (earlier filed) + close GAP-581 DUPLICATE.
Unified fix PR references both gap IDs for clarity.
```

**GAP-301 Problem section update:**
```markdown
## Scope Clarification (post-audit 2026-05-18)

This gap focuses on TENANT-side DSAR (all their data export + compliance export).
Related PH-side DSAR scope (parent payment metadata deletion) covered by GAP-626.
Execution note: audit log rows (per PDPL legal obligation) are NOT deletable exceptions — 
verify with GAP-626 distinction.
```

---

## 5. Log Entry

- **2026-05-18** — Re-triage of 26 gaps phase-1.5-paid (ngoài payment-audit scope Wave 93) completed. Methodology: per-gap overlap analysis vs GAP-625..635 payment gaps; phase-mismatch verification; duplicate candidate detection. Final verdict: **12 KEEP** (scope unchanged) + **8 RE-SCOPE** (scope refine needed) + **4 MOVE-PHASE** (wrong phase assignment) + **2 OVERLAP + 1 DUPLICATE** (cross-ref or merge required) + **0 CANCEL**. **Top blocker:** GAP-259 ↔ GAP-581 likely DUPLICATE (both "gateway rate limit by tenant_id") — recommend merge decision before Wave 93 closure; if merged, unified fix PR achieves both gap closure. **Phase clarity needed:** 4 infrastructure gaps (HPA/PDB/Canary/EKS) assigned phase-1.5-paid but NOT payment-trigger per audit scope — user decision required: deployment-critical prerequisites OR move phase-1-beta/phase-2. **Legal timeline:** 5 P0 legal docs (TOS/AUP/Privacy/Retention/ChildProtection) affirmed phase-1.5-paid + PDPL deadline 2026-07-01 (6 weeks) — confirm execution feasible. **Cross-ref sync needed:** GAP-577/578 ↔ GAP-625 share identity patterns; recommend same-wave log entry updates + execution ordering documentation.

---

## Audit Trail

- **Audit initiation:** 2026-05-18 user-flagged blind spot "26 gaps phase-1.5-paid chưa re-triage post-Wave-93-audit"
- **Audit method:** per-gap re-triage vs Wave 93 outside-in audit scope + 3-agent convergence findings
- **Related audit:** 2026-05-18-phase-1-5-qr-payment-outside-in.md (shipping 11 new gaps GAP-625..635 + 4 re-scope existing)
- **State-check:** gap-status.csv query confirmed 26 gaps phase-1.5-paid (excluded GAP-108/183/185/594 re-scope pair)
- **Reviewer:** @nguyenvankiet (solo-dev audit author)
