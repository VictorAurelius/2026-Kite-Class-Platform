---
title: Outside-In Persona Audit — GAP-564 scope expansion decision (Cat 2 vs all 5)
status: complete
created: 2026-05-14
phase: Phase 1 BETA pre-launch (post Wave 78 closure + pre v1.0.0-rc gate)
auditor: Background agent (Opus 4.7, GAP-564 outside-in audit)
gaps: [GAP-564, GAP-522]
audit_target: documents/04-quality/audits/security/2026-05-14-post-wave-78.md (89/100 B+)
methodology: persona-based-business-review (3 personas × 5 categories)
related_rules:
  - .claude/rules/pre-launch-dependency-hardening-checklist.md
  - .claude/rules/pre-launch-secrets-hardening-checklist.md
  - .claude/rules/pre-launch-owasp-rest-hardening-checklist.md
  - .claude/rules/pre-launch-auth-hardening-checklist.md
  - .claude/rules/pre-launch-infra-hardening-checklist.md
---

# Outside-In Persona Audit — GAP-564 Scope Expansion Decision

## Scope

GAP-564 hiện được filed với scope chỉ Cat 2 Secrets — fix grep evidence enforcement sau khi audit Wave 78 PASS 17/20 nhưng MISSED 11 hardcoded passwords trong `kiteclass/docker-compose*.yml`.

**Câu hỏi outside-in:** GAP-564 inside (Cat 2 only), where is outside?

Audit này simulate 3 personas đánh giá Wave 78 audit report (`2026-05-14-post-wave-78.md` 89/100 B+) như một bằng chứng về security posture, để xác định:

1. Liệu Cat 2 fix có đủ trust không, HAY tất cả 5 categories đều có cùng "narrative-only PASS" pattern?
2. So sánh với industry benchmark (SOC2/ISO27001 evidence format) — Wave 78 audit thiếu gì?
3. GAP-564 scope nên giữ Cat 2 only HAY expand ALL 5?

**Method:** Mỗi persona đọc audit report + 5 sister rules (`pre-launch-{deps,secrets,owasp,auth,infra}-hardening-checklist.md`) — đặt câu hỏi từ góc nhìn ngành để surface trust gaps systemic.

---

## Persona 1 — Legal Counsel (Phase 3 K-12 Trigger)

**Background:** VN/international counsel engaged pre-K-12 launch per `release-deploy-standard.md` §3.4. Sẽ review audit reports như bằng chứng security posture trước khi clear K-12 minor-data compliance (LGPD-K12 + Luật Trẻ em + PDPL 2023).

**Counsel mindset:** Paranoia, evidence-based, sẽ REJECT report nếu narrative-only without grep/scan output. Counsel quen với SOC2 Type II audit reports — mỗi control PHẢI có `Test Procedure` + `Test Result` + `Evidence`.

### Walkthrough Wave 78 audit (89/100 B+)

**Câu hỏi 1 — Trust signal level:** "Báo cáo này tự-audit hay third-party audit?"
- Audit report ghi: `auditor: Background agent (Opus 4.7, Wave 78 closure audit)` → self-audit bằng AI agent.
- Counsel verdict: ⚠️ self-audit ACCEPTABLE cho internal QA gate, KHÔNG đủ cho K-12 legal evidence pack. Cần third-party pentest report pre-K-12.

**Câu hỏi 2 — Per-category evidence:** "Mỗi PASS verdict có grep output / scan output / matrix verification không?"

| Cat | Score | Counsel verdict | Evidence missing |
|---|---|---|---|
| 1 Deps 18/20 | 🟢 PASS | ⚠️ TRUST GAP | "Spring Boot BOM unchanged" — KHÔNG có `mvn dependency-check:check` output evidence; "pnpm 0 CVE" — KHÔNG có `pnpm audit --json` output |
| 2 Secrets 17/20 | 🟢 PASS | 🔴 **VIOLATION** | Đã confirmed — 11 hardcoded passwords MISSED (caught by GitHub Secret Scanning). Audit ghi "AES-256-GCM impl đúng" nhưng KHÔNG có code-pattern grep evidence covering `docker-compose*.yml` scope |
| 3 OWASP 17/20 | 🟡 PARTIAL | 🔴 **VIOLATION** | "A01: FeedbackController authz boundary mơ hồ" narrative-only — KHÔNG có `@PreAuthorize` coverage matrix across all admin endpoints. A03 Injection PASS no grep evidence for `String.format` SQL patterns |
| 4 Auth 18/20 | 🟢 PASS | ⚠️ TRUST GAP | "4 endpoints rate-limited" — KHÔNG có endpoint × rate-limit verification matrix (gateway YAML scan); 2.3 password complexity = UNCHECKED carry-over (admitted gap) |
| 5 Infra 19/20 | 🟢 PASS | ⚠️ TRUST GAP | "Env config registry" — KHÔNG có TLS listener policy output (`aws elbv2 describe-listeners`), KHÔNG có Docker non-root sweep, KHÔNG có IAM `*:*` grep evidence, KHÔNG có CORS production config snapshot |

**Câu hỏi 3 — K-12 clearance verdict:** "Tôi có dám đưa audit này lên bàn DPO + LGPD-K12 reviewer không?"

- 🔴 **REJECT.** 5/5 categories đều có narrative-only PASS thiếu Test Procedure + Test Result evidence cells. K-12 compliance reviewer sẽ hỏi cùng câu hỏi với Counsel — và self-audit AI agent KHÔNG phải acceptable evidence trail.
- **Required gap to close:** mỗi category PHẢI có "Command run" + "Output" + "Verdict" blocks per Cat 2 fix proposal. Cat 2 fix alone KHÔNG đủ — phải systemic.

### Persona 1 verdict

**GAP-564 scope = Cat 2 only → 🔴 INSUFFICIENT cho K-12 launch evidence trail.**

Recommendation: expand GAP-564 → ALL 5 categories với mandatory evidence cell format per `pre-launch-*-hardening-checklist.md` sister rules (đã tồn tại từ 2026-05-14 mandate). Skill rubric của 5 categories đã được sharpened (Wave 71c PR #1278 + Wave 72b), nhưng skill agent KHÔNG enforce evidence output — gap là **skill → audit report template binding**.

---

## Persona 2 — Insurance/Compliance Auditor (Cyber Insurance Pre-Bind)

**Background:** Cyber insurance carrier auditing KiteHub pre-binding policy. Reviews 5 audit categories per ISO27001 Annex A + SOC2 Trust Services Criteria (TSC) baseline. Carrier pricing premium dựa trên measurable security control maturity.

**Auditor mindset:** Industry-standard format expectations. SOC2 Type II Section 4 — Description of Test of Controls — yêu cầu mỗi control có:
- Control description (what)
- Test procedure (how tested)
- Test result (what found)
- Evidence reference (artifact ID)

ISO27001 control 8.x cũng yêu cầu evidence cell. Narrative scores → bị reject hoặc giảm coverage limit.

### Per-category SOC2-baseline review

**Cat 1 — Dependency Vulnerabilities (CC7.1 Threat Detection):**
- Wave 78 audit ghi "18/20 ✅ PASS — totp 1.7.1 mature, RFC-6238". 
- SOC2 evidence required: `mvn dependency-check` Maven SBOM CSV + `pnpm audit --json` snapshot + SBOM CycloneDX artifact attached to release.
- **Found in audit:** ❌ Zero of 3 evidence artifacts. Narrative-only attestation.
- **`pre-launch-dependency-hardening-checklist.md` §2.8 SBOM hook:** rule mandates SBOM generation hook BUT marked acceptable v1 manual — audit không document manual run.
- **Verdict:** 🟡 ELEVATED RISK — insurance carrier không price control as ACTIVE without evidence artifact.

**Cat 2 — Secrets & Credentials (CC6.1 Logical Access):**
- Wave 78 audit ghi "17/20 ✅ PASS — TOTP encryption key có dev-default (P1)".
- SOC2 evidence required: secrets scan tool output (gitleaks/trufflehog) + Secrets Manager rotation policy log + KMS CMK arn snapshot.
- **Found in audit:** ❌ Zero of 3 evidence artifacts. Same class as Cat 1 — narrative attestation.
- **CRITICAL:** 11 missed hardcoded passwords là PROOF carrier sẽ classify CC6.1 control = INEFFECTIVE pre-binding policy. Insurance premium impact: HIGH.

**Cat 3 — OWASP A01-A06/A08-A10 (CC6.6 Logical Access + CC7.2 Detection):**
- Wave 78 audit ghi "17/20 🟡 PARTIAL — A01 mơ hồ, A05 default-allow".
- SOC2 evidence required: `@PreAuthorize` coverage matrix per endpoint + threat model docs (`documents/02-architecture/threat-models/`) + production profile config snapshot showing actuator scope.
- **Found in audit:** ❌ Zero matrices. Threat model docs likely don't exist (verified per `pre-launch-owasp-rest-hardening-checklist.md` §4 self-test — A04 FAIL).
- **Verdict:** 🔴 BLOCKING — A04 threat model absence + A05 misconfig + A09 admin audit log gap = three CC6.x controls UNTESTABLE.

**Cat 4 — Auth & Access Control (CC6.1):**
- Wave 78 audit ghi "18/20 ✅ PASS — 4 endpoints rate-limited + TOTP + lockout".
- SOC2 evidence required: endpoint × rate-limit matrix (gateway YAML grep) + lockout test log (failed-login curl sequence) + 2FA enrollment screenshot/log.
- **Found in audit:** ❌ Narrative only. AuthServiceLockoutTest mentioned but test output NOT attached.
- **Verdict:** ⚠️ MODERATE — Wave 71c rule extension exists (`pre-launch-auth-hardening-checklist.md`); audit didn't use it as evidence template.

**Cat 5 — Infrastructure (CC6.6 + CC7.1):**
- Wave 78 audit ghi "19/20 ✅ PASS — env registry + 3 audit scripts".
- SOC2 evidence required: ALB TLS policy + RDS encryption snapshot + IAM least-priv matrix + CORS production config + CloudTrail multi-region status + GuardDuty detector status.
- **Found in audit:** ❌ Zero AWS CLI describe-* outputs. Pre-launch-infra rule §4 self-test estimates 2-3 P1 follow-ups (CSP, Docker non-root, GuardDuty) — audit didn't surface these.
- **Verdict:** 🟡 ELEVATED RISK — Cat 5 highest score (19/20) but zero infra evidence cells = score-vs-evidence drift maximal.

### Persona 2 verdict

**Industry-standard format gap:** Wave 78 audit format ≈ "internal security review note". To pass SOC2 Type II readiness assessment OR cyber insurance pre-bind, EVERY category PHẢI có evidence cell — không phải chỉ Cat 2.

**GAP-564 scope = Cat 2 only → 🔴 INSUFFICIENT for industry compliance baseline.** Insurance carrier sẽ vẫn classify other 4 controls = INEFFECTIVE pending evidence.

Recommendation: GAP-564 expand ALL 5; bind skill agent output template tới sister rules `pre-launch-*-hardening-checklist.md` §2 mandatory checks — mỗi check produces evidence cell trong audit report.

---

## Persona 3 — Beta Tenant Security Officer (P2 Center Owner IT Contact)

**Background:** Chị Hằng (P2 Center Owner Trung tâm Anh ngữ Sky Education) hỏi anh Tâm — IT contractor của trung tâm — review xem KiteHub có safe cho tenant data trước khi accept invite-trial.

**Tâm mindset:** Pragmatic IT (không phải deep security expert). Đọc abbreviated summary OR hỏi "có audit không?". Trust signal level:
- ✅ Third-party pentest report = highest trust
- ⚠️ Self-audit với methodology + evidence trail = medium trust  
- ❌ Self-audit narrative score only = low trust

### Walkthrough abbreviated summary

Anh Tâm hỏi: "Anh có audit pre-launch không?"

KiteHub reply (hypothetical, dựa trên Wave 78 audit hiện có): "Có, security audit 89/100 B+, PASS Phase 1 BETA threshold ≥80."

**Tâm follow-up #1:** "Audit do ai làm? Có report tôi xem được không?"
- Reply: "Self-audit bằng AI agent theo skill `.claude/skills/quality/security-audit/SKILL.md`."
- Tâm verdict: ⚠️ Self-audit OK cho Phase 1 BETA invite-trial nếu có methodology trail + evidence — chấp nhận được cho tenant trial. KHÔNG đủ cho production paid contract.

**Tâm follow-up #2:** "11 hardcoded passwords mà GitHub Secret Scanning bắt được — audit có bắt không?"
- Reply: "Audit Cat 2 17/20 PASS — không bắt được."
- Tâm verdict: 🔴 **TRUST BREAK** — nếu audit miss obvious thing (hardcoded password) thì còn miss gì nữa không kiểm tra được? Đây là deal-breaker pragmatic — không cần deep tech để hiểu.

**Tâm follow-up #3:** "Có monitoring runtime không, hay chỉ audit pre-launch?"
- Reply: phụ thuộc Cat 5 19/20 PASS có evidence không. Audit narrative-only.
- Tâm verdict: ⚠️ Không phân biệt được "có monitoring" vs "không có monitoring" từ audit report.

### Persona 3 verdict

**Tenant officer trust threshold:** Phase 1 BETA acceptable level = methodology + evidence trail. Wave 78 audit fails methodology gate (audit miss 11 secrets là proof methodology weak).

**GAP-564 scope = Cat 2 only → 🟡 PARTIAL TRUST RESTORE.** Cat 2 fix giúp recover from specific miss, nhưng Tâm vẫn skeptical về Cat 1/3/4/5 — "what else did the same methodology miss?"

Recommendation: GAP-564 expand ALL 5 để rebuild end-to-end methodology trust. Specifically:
- Mỗi Cat report grep/scan output → tenant officer có thể verify ad-hoc
- Audit report public path (per `output-review-mandate.md` §3) → discoverable
- Optional: third-party pentest report acceptance trial-2 onwards

---

## Per-Category Trust Verdict Matrix

| Cat | Wave 78 score | P1 Counsel verdict | P2 Insurance verdict | P3 Tenant officer verdict | Aggregate trust gap |
|---|---|---|---|---|---|
| 1 Deps | 18/20 PASS | ⚠️ TRUST GAP (no mvn/pnpm audit output) | 🟡 ELEVATED RISK (no SBOM artifact) | ⚠️ skeptical (chained from Cat 2 miss) | **EXPAND mandatory** |
| 2 Secrets | 17/20 PASS | 🔴 VIOLATION (11 missed) | 🔴 INEFFECTIVE control | 🔴 TRUST BREAK | **EXPAND mandatory** (in-scope) |
| 3 OWASP A01-A06/A08-A10 | 17/20 PARTIAL | 🔴 VIOLATION (no @PreAuthorize matrix) | 🔴 BLOCKING (3 CC6.x untestable) | ⚠️ skeptical | **EXPAND mandatory** |
| 4 Auth A07 | 18/20 PASS | ⚠️ TRUST GAP (no endpoint matrix) | ⚠️ MODERATE | ⚠️ skeptical | **EXPAND mandatory** |
| 5 Infra | 19/20 PASS | ⚠️ TRUST GAP (no AWS CLI output) | 🟡 ELEVATED RISK (zero infra evidence) | ⚠️ skeptical | **EXPAND mandatory** |

**Pattern surfaced:** All 5 categories có cùng "narrative-only PASS" pattern — KHÔNG phải Cat 2 isolated. Cat 5 highest score (19/20) có evidence gap lớn nhất (zero AWS CLI outputs).

---

## Industry Benchmark Comparison

### SOC2 Type II Section 4 — Description of Test of Controls

SOC2 Type II audit report format mandate per AICPA TSP 100:

```
Control: CC6.1 — Logical Access
Test Procedure: Auditor reviewed [X] sample identity records and 
  observed authentication enforcement via [tool/method].
Test Result: No exceptions noted. [N] of [N] sample records 
  showed compliant authentication state.
Evidence: Artifact ID AUTH-001 — screenshot/log snapshot dated YYYY-MM-DD
```

**Mapping to Wave 78 audit:**
- ✅ Control description: present (`Cat N — Category name`)
- ❌ Test procedure: MISSING (audit ghi "Wave 78 ship X" — không có "auditor reviewed X via Y")
- ⚠️ Test result: partial (score nhưng không có "N of N sample")
- ❌ Evidence reference: MISSING (no artifact ID, no grep output, no screenshot)

### ISO27001 Annex A 8.x Controls

ISO27001 evidence baseline expect:
- Control objective (matches Wave 78 category header ✅)
- Control activity description (matches Cat 4 sub-bullets ✅)
- **Evidence of control operating effectively** (Wave 78 narrative ❌)
- **Test of control sample size + outcome** (Wave 78 ❌)

### Mintlify / GitHub Security Audit Format

Modern dev-focused security audit reports (Trail of Bits, NCC Group, Cure53) all include:
- **Methodology section** — tools used, scope (file paths), time-bounded
- **Findings table** — ID, severity, category, status (Wave 78 has this ✅)
- **Per-finding evidence** — code snippets, line numbers, reproduction steps (Wave 78 partial — bug list has file:line but no full grep output ⚠️)
- **Tool output appendix** — raw scan results (Wave 78 ❌)

### Verdict: Wave 78 audit format scores ~40% industry baseline

Strengths: Per-OWASP-item rubric (Cat 3) + per-check carry-forward tracking (Cat 4 §2 table) + delta vs baseline (per-Cat). Weaknesses: ALL 5 categories lack evidence appendix.

---

## Recommended GAP-564 Scope Expansion: ALL 5 Categories

### Decision

🔴 **EXPAND GAP-564 from Cat 2 only → ALL 5 categories.**

**Rationale:**

1. **Same root cause pattern** — All 5 categories use narrative attestation without evidence cell. Cat 2 fix alone leaves Cat 1/3/4/5 vulnerable to same class of miss.

2. **Industry benchmark gap is systemic** — SOC2/ISO27001/modern audit reports ALL require per-category evidence. Self-audit báo cáo without evidence trail fails ALL 3 persona tests.

3. **Force-multiplier per `meta-gap-priority.md` §3** — Meta P1 force-multiplier: fix skill template once → every future security audit benefits. Splitting into 5 GAPs = 5x duplicate work + drift risk.

4. **Sister rules already exist + aligned** — 5 `pre-launch-*-hardening-checklist.md` rules đã ship 2026-05-14 với mandatory checks. Gap is **skill → rule binding** not new rule work. Closing systemic.

5. **Wave 78 audit self-test failure** — Wave 78 audit ran AFTER all 5 sister rules existed (rules dated 2026-05-14 v1.0.0). Audit agent had access to rules but didn't enforce evidence output. Proof: rule alone không đủ — skill template enforcement required.

### Renamed scope

**Old:** GAP-564 META — security-audit skill Cat 2 must mandate grep evidence

**New:** GAP-564 META — security-audit skill ALL 5 categories must mandate per-check evidence cell (audit-of-trust-pass systemic fix)

### Updated Acceptance Criteria (expand from 5 → 9 items)

1. [ ] `.claude/skills/quality/security-audit/SKILL.md` Cat 1 section explicitly mandates `pnpm audit --json` + `mvn dependency-check` + SBOM artifact evidence cells (per `pre-launch-dependency-hardening-checklist.md` §2 8-check)
2. [ ] Cat 2 section mandates grep + gitleaks + Secrets Manager describe-secret evidence cells (per `pre-launch-secrets-hardening-checklist.md` §2 8-check) — **in-scope original**
3. [ ] Cat 3 section mandates `@PreAuthorize` coverage matrix + crypto grep + injection grep + threat model existence + production profile snapshot + SSRF allowlist grep evidence cells (per `pre-launch-owasp-rest-hardening-checklist.md` §2 9-check)
4. [ ] Cat 4 section mandates gateway YAML rate-limit matrix + lockout test log + 2FA enrollment log + password validator grep + admin audit log entity check evidence cells (per `pre-launch-auth-hardening-checklist.md` §2 8-check)
5. [ ] Cat 5 section mandates `aws elbv2/rds/cloudtrail/ec2/iam describe-*` outputs + Docker non-root sweep + CSP header check + CORS config snapshot evidence cells (per `pre-launch-infra-hardening-checklist.md` §2 9-check)
6. [ ] Audit report template (paired same PR) standardize per-Cat structure: `Command run` + `Output` + `Verdict` + `Evidence artifact ID` blocks (matching SOC2 Type II Section 4 format)
7. [ ] Sister rules §5.1 cross-link GAP-564 (all 5: pre-launch-deps, secrets, owasp, auth, infra)
8. [ ] Next security audit (post-fix) produces evidence cells for ALL 5 categories; self-test: run on current main HEAD should surface specific findings per category §4 worked self-test sections (e.g., Cat 1 SBOM PARTIAL, Cat 4 password complexity FAIL, Cat 5 CSP/Docker non-root PARTIAL)
9. [ ] Wave 78 audit retrospectively annotated as "v1 format - evidence appendix retrofit deferred" OR re-run with new template (decision: annotate, don't re-run — audit cost-benefit)

### Concrete Enforcement Changes Per Category

**Cat 1 (Deps):** Skill instructs agent to RUN `pnpm audit --audit-level=high --json` + `mvn dependency-check:check -DfailBuildOnCVSS=7` + paste output cells. SBOM acceptable v1 manual generation but evidence cell required.

**Cat 2 (Secrets):** Original GAP-564 scope — mandatory grep evidence including `docker-compose*.yml` + `kiteclass/` scope. Add gitleaks/trufflehog baseline check evidence cell.

**Cat 3 (OWASP):** Skill instructs agent per OWASP item:
- A01: grep `@PreAuthorize` coverage on Controllers — matrix output cell
- A02: grep MD5/SHA1/DES — output cell (expect 0 hits)
- A03: grep `String.format.*WHERE.*%` — output cell
- A04: list `documents/02-architecture/threat-models/*.md` — output cell
- A05: paste `application-production.yml` actuator + error config — output cell
- A08/A09/A10: per §2.7-2.9 rule checks

**Cat 4 (Auth):** Skill instructs agent grep gateway YAML for `RequestRateLimiter` × auth endpoints — matrix cell. Run `AuthServiceLockoutTest` + paste assert output. List 2FA enrollment endpoints from controller.

**Cat 5 (Infra):** Skill instructs agent run `aws elbv2 describe-listeners --query` + `aws rds describe-db-instances` + `aws cloudtrail get-trail-status` + grep Dockerfile `USER` directives + grep IAM `*:*` patterns + verify CORS + check CSP header. ALL outputs paste into evidence cells.

### Pre-handoff self-test mandate (per `pre-handoff-self-test-completeness.md`)

GAP-564 closure PR MUST include retrofit run on current main HEAD producing per-Cat evidence cells. Self-test: counterfactual — would v2 audit have caught 11 hardcoded passwords? Verify YES via Cat 2 grep evidence cell scope expansion.

---

## Concrete Skill Update Pattern (Cat 2 reference applied to all 5)

```markdown
### Cat N.X — [Check name]

**Command run:**
```bash
<exact command from sister rule §2.X>
```

**Output:**
```
<paste full output OR explicit "0 hits / clean">
```

**Verdict:** PASS / FAIL / PARTIAL with hit count + cited line numbers + risk tier.

**Cross-reference:** `pre-launch-<cat>-hardening-checklist.md` §2.X
```

This template applied to ALL 5 categories closes the systemic narrative-only gap.

---

## Pending (post-audit actions)

| Action | Owner | Notes |
|---|---|---|
| Update GAP-564 scope from Cat 2 only → ALL 5 categories | Coordinator | Per recommendation §6 above |
| Re-prioritize GAP-564: 🟠 P1 META → consider 🔴 P0 META (force-multiplier for v1.0.0-rc gate) | Coordinator | Depends on Wave 79 plan timing |
| Add row to `output-review-mandate.md` §3: "Security audit per-Cat evidence cell" tracking standard | Coordinator | Cross-link GAP-564 expanded scope |
| Sister rules update §5.1 cross-link GAP-564 (5 rules) | Coordinator | Mass-edit during GAP-564 fix PR |
| Wave 78 audit annotation entry "v1 format - evidence appendix retrofit deferred to GAP-564" | Coordinator | Avoid re-run cost; mark v2 forward |
| Update `gap-status.csv` row for GAP-564 with expanded scope + priority adjustment | Coordinator | Per `gap-architecture-v2.md` canonical CSV |
| Notify next session: GAP-564 expansion is pre-`v1.0.0-rc` gate work | Coordinator | Block v1.0.0-rc promotion until evidence cells shipped |

---

## References

- **Audit report under review:** `documents/04-quality/audits/security/2026-05-14-post-wave-78.md` (89/100 B+)
- **GAP-564 (in-scope expansion target):** `documents/04-quality/gaps/GAP-564-security-audit-cat2-grep-evidence-enforcement.md`
- **GAP-522 (parent meta-rule extension):** filed 2026-05-13 sister concern (security-audit averaging hides per-item gaps)
- **5 Sister rules already shipped 2026-05-14 v1.0.0:**
  - `.claude/rules/pre-launch-dependency-hardening-checklist.md` — Cat 1 8 checks
  - `.claude/rules/pre-launch-secrets-hardening-checklist.md` — Cat 2 8 checks
  - `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` — Cat 3 9 checks
  - `.claude/rules/pre-launch-auth-hardening-checklist.md` — Cat 4 8 checks
  - `.claude/rules/pre-launch-infra-hardening-checklist.md` — Cat 5 9 checks
- **External safety net (proof of methodology weakness):** GitHub Secret Scanning caught 11 hardcoded passwords Wave 78 audit missed
- **Sister anti-pattern memory:** `feedback_audit_of_trust_pass.md`
- **Governance rules applied:**
  - `outside-in-coverage-trigger.md` — this audit is the outside-in response to inside-out GAP-564 scope
  - `meta-gap-priority.md` §3 META P1 force-multiplier
  - `incident-to-rule-pipeline.md` 5-stage
  - `pre-handoff-self-test-completeness.md` — self-test mandate for GAP-564 closure

---

## Final Summary (≤500 words)

### Per-Persona Verdict

**Persona 1 (Legal Counsel Phase 3 K-12):** 🔴 REJECT audit cho K-12 evidence pack. ALL 5 categories thiếu Test Procedure + Test Result + Evidence cells. Self-audit AI agent KHÔNG đủ — cần third-party pentest pre-K-12 + per-category evidence trail.

**Persona 2 (Insurance/Compliance Auditor):** 🟡-🔴 ELEVATED RISK 5/5 categories. SOC2 Type II Section 4 + ISO27001 Annex A đều require per-control evidence reference. Wave 78 format scores ~40% industry baseline. Cat 2 11-secret miss = PROOF carrier classify CC6.1 control INEFFECTIVE pre-binding.

**Persona 3 (Beta Tenant Security Officer P2 IT contact):** 🔴 TRUST BREAK trên Cat 2; ⚠️ skeptical 4 categories khác do "what else did same methodology miss?". Phase 1 BETA acceptable level = methodology + evidence trail; Wave 78 fails methodology gate.

### Per-Category Gap

ALL 5 categories có cùng pattern "narrative-only PASS" — không phải Cat 2 isolated:
- Cat 1 Deps: thiếu `pnpm audit`/`mvn dep-check`/SBOM evidence
- Cat 2 Secrets: thiếu grep + gitleaks + Secrets Manager evidence (in-scope GAP-564 hiện tại)
- Cat 3 OWASP: thiếu `@PreAuthorize` matrix + threat model docs + crypto/injection grep
- Cat 4 Auth: thiếu endpoint × rate-limit matrix + lockout test log + 2FA log
- Cat 5 Infra: thiếu AWS CLI `describe-*` outputs + Docker non-root sweep + CSP/CORS snapshot

Cat 5 highest score (19/20) có evidence gap LỚN NHẤT — score-vs-evidence drift maximal — chứng minh narrative-score không correlate với actual control effectiveness.

### Scope Expansion Recommendation

🔴 **EXPAND GAP-564 → ALL 5 categories** (không phải Cat 2 only).

**5 reasons:**

1. **Root cause systemic** — same narrative-attestation pattern across all 5
2. **Industry baseline systemic** — SOC2/ISO27001/modern audits all require per-category evidence
3. **Force-multiplier per `meta-gap-priority.md` §3** — fix skill template 1 lần, all future audits benefit
4. **Sister rules already exist** — 5 `pre-launch-*-hardening-checklist.md` rules shipped 2026-05-14 với mandatory checks; gap = **skill → rule binding** not new rule work
5. **Wave 78 self-test proof** — audit ran AFTER 5 sister rules existed but didn't enforce evidence; rule alone không đủ, skill template enforcement required

**Updated AC:** 9 items expanding from 5 (per §6 Decision). Each Cat gets `Command run` + `Output` + `Verdict` + `Evidence artifact ID` template matching SOC2 Type II Section 4 format. Pre-handoff self-test counterfactual: would v2 audit catch 11 hardcoded passwords? — YES via Cat 2 expanded grep scope.

**Priority recommendation:** consider 🔴 P0 META (force-multiplier pre-`v1.0.0-rc` gate) thay vì 🟠 P1 META hiện tại — block v1.0.0-rc promotion until evidence cells shipped.

**Wave 78 retrospective:** annotate as "v1 format" — don't re-run (cost-benefit) — but mark forward audits use v2 template per GAP-564 expanded scope.

---

**Outside-in audit complete.** Recommendation: GAP-564 expand to ALL 5 categories with mandatory evidence cells, retrofit `security-audit/SKILL.md` rubric to bind sister rules' §2 mandatory checks → audit report template.
