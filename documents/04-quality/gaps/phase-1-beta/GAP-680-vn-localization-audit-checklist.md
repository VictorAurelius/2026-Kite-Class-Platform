# GAP-680: VN-localization audit checklist pre-merge (Wave 100 cross-bucket META)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta
**Detected:** 2026-05-19
**Related PRs:** (none yet)
**Related Docs:** `documents/03-planning/roadmap/release-1.5-thesis-scope.md`, `.claude/rules/dev-readable-doc-language.md`, `.claude/rules/user-manual-content-standard.md` §2 row 7-8

## Current State (verified 2026-05-19)

> **Step 2.5 state-check per `audit-to-gap-pipeline.md`** — grep paths checklist sẽ touch.

| Piece | File / Path | Status |
|---|---|---|
| VN-localization audit checklist (cross-bucket) | KHÔNG tồn tại — verified | ❌ missing |
| Vietnamese narrative rule (scope: dev-readable docs) | `.claude/rules/dev-readable-doc-language.md` v1.0.2 | ✅ shipped |
| User-manual VND/date format mandate (scope: user manual only) | `.claude/rules/user-manual-content-standard.md` §2 row 7-8 | ✅ shipped |
| SES + Statuspage Vietnamese overlay (scope: 2 specific runbooks) | `closed/GAP-423-ses-runbook-vietnamese-overlay.md` + `closed/GAP-424-statuspage-runbook-vietnamese-overlay.md` | ✅ DONE (narrow scope) |
| Cross-bucket pre-merge checklist covering VND / Vietnamese label / VN sample / Zalo culture | KHÔNG tồn tại trong `documents/04-quality/audits/` hoặc `.claude/skills/` | ❌ missing |

**Grep commands run:**
```bash
grep -rliE "vn[ -]?localization|vietnamese[- ]?(audit|review|checklist)|vnd[- ]?format[- ]?(check|audit)|zalo[ -]?culture" documents/04-quality/gaps/
# → 2 hits: GAP-423 + GAP-424 (closed, narrow runbook overlay scope)
grep -E "Dropped:.*GAP-" documents/04-quality/gaps/ROADMAP.md
# → 1 hit: GAP-196 (irrelevant)
awk -F',' 'NR>1 {print $1}' gap-status.csv | grep -E '^GAP-[0-9]+$' | sort -t- -k2 -n | tail -5
# → Max GAP-679; GAP-680 next available
```

Per Step 0 canonical-status lookup (rule vừa ship cùng session): genuinely new gap, không duplicate.

## Problem

Wave 100 plan có 4 buckets (A batch invoice / B income dashboard / C email-only signup / D thesis Chapter 1 Part 2). 3 outside-in audits 2026-05-19 (persona + failure-mode + benchmark) **đồng thuận** rằng mỗi bucket có VN-localization concern riêng:

- **Bucket A:** VND format hoá đơn (`1.500.000đ`, KHÔNG `$60.00`); email template Vietnamese (chủ đề + body)
- **Bucket B:** VND label dashboard KPI cards; Vietnamese MoM/YoY delta; date `Thứ Hai, 14/05/2026` không phải `Mon May 14, 2026`
- **Bucket C:** Zalo culture conflict (VN edu user signup habit dùng phone — email-only outlier); migration UX cho existing SMS users tone tiếng Việt
- **Bucket D:** Vietnamese narrative + VN law citation format; PDPL Art numbers cụ thể

Hiện tại KHÔNG có cross-bucket checklist enforce VN-localization. `dev-readable-doc-language.md` chỉ cover narrative language; `user-manual-content-standard.md` §2 row 7-8 cover VND format + date format CHỈ cho user manual scope. Wave 100 4 buckets touch FE component / email template / dashboard chart / thesis docs — **scope rộng hơn user manual** → cần checklist riêng applied pre-merge per bucket.

Nếu KHÔNG có checklist:
- Risk pattern recurrence: bucket ship english label + USD format → user retro round-trip (precedent Wave 72a Bucket F CSV English narrative violation 2026-05-14)
- Inconsistent VN context giữa 4 bucket cùng wave
- Future wave subsequent (Wave 101+) lặp lại same friction

## Context

Triggered bởi 3 outside-in audits Wave 100 thesis push (2026-05-19) — cả 3 agents đồng thuận về VN-localization gap cross-bucket. Per `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 5, findings consolidated thành scope adjustments cho Wave 100 plan + 1 META gap force-multiplier file riêng (rule này).

META P1 force-multiplier per `meta-gap-priority.md` §3 — fix checklist 1 lần → mọi bucket subsequent (Wave 100 + Wave 101+) auto-comply.

## Evidence

3 outside-in audit findings (background agent outputs 2026-05-19):
- Persona simulation Wave 100: "VN context gap đa bucket: Bucket A (VND format + email VN template), Bucket B (VND + Vietnamese label), Bucket C (Zalo culture conflict), Bucket D (VN law citation depth) — tất cả buckets cần VN-localization audit pre-merge"
- Failure-mode matrix Wave 100: A1 invoice number vi phạm thông tư 78/2021 + D6 VN law citation accuracy
- VN edu SaaS benchmark Wave 100: Bucket C "Email-only signup = outlier pattern trong VN edu market" + Bucket A "Per-class generation + auto-reconciliation industry standard"

Sister rule `dev-readable-doc-language.md` v1.0.2 §2 cover narrative language only; KHÔNG cover format/cultural/sample-data discipline cross-bucket.

## Proposed Fix

Tạo new rule `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 với 4 sections:

### Section 1 — VND currency + date format
- VND format `1.500.000đ` HOẶC `1.500.000 ₫` (BANNED: `$60.00`, `60 USD`)
- Date long: `Thứ Hai, 14/05/2026` (BANNED narrative: `Mon May 14, 2026`; ISO `2026-05-14` OK trong code/frontmatter)
- Date short: `14/05/2026` narrative
- Time: `09:30` 24h preferred

### Section 2 — Vietnamese label (UI + email)
- Button/menu/error message tiếng Việt
- English label cho persona slug / role enum acceptable per `dev-readable-doc-language.md` §3
- Email template subject + body tiếng Việt + tone phù hợp đối tượng (Owner formal / Solo casual)

### Section 3 — VN sample data
- Tên: "Trần Thị Hồng", "Nguyễn Văn An" — BANNED "John Doe"
- Trung tâm: "Trung tâm Anh ngữ Sky Education" — BANNED "Example Center"
- Lớp: "Lớp 5A1" / "Lớp Anh ngữ Sky 5A1" — BANNED "Class A1"
- Địa chỉ: "123 Lê Lợi, Q.1, TP.HCM"

### Section 4 — VN cultural awareness
- Phone signup habit (Zalo culture) — nếu remove SMS/Zalo path, MUST document rationale + migration path + FAQ
- Phong tục giao tiếp: chào "Em chào chị/anh" trong email Owner, KHÔNG "Hi Hằng"
- VN edu conventions: niên khóa 9-5 (NOT calendar year), GVCN/Hiệu trưởng terminology
- Working day: tuần 6 ngày Mon-Sat (NOT Mon-Fri) cho center

Plus enforcement:
- Reviewer-checklist active immediately (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)
- CI grep detector deferred ≥7 ngày per `incident-to-rule-pipeline.md` §3 premature-rule guard
- Self-test §6 worked example: Wave 100 4 buckets retroactive check

## Acceptance Criteria

- [ ] Rule `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 created với 4 sections + enforcement section + worked self-test trên 4 Wave 100 buckets
- [ ] `output-review-mandate.md` §3 matrix thêm row "VN-localization audit checklist (cross-bucket)" reference new rule
- [ ] `.claude/rules/_meta/rules-index.csv` thêm row mới với correct lifecycle_status=active
- [ ] Reviewer-checklist active cho Wave 100 4 buckets pre-merge (checklist embedded vào Wave 100 plan PR §1 Brainstorm Q2 + §3 per-bucket AC section)
- [ ] Memory entry `feedback_vn_localization_audit_checklist.md` (optional, defer per premature-rule guard nếu reviewer-checklist + self-test đủ)

## Related

- 3 outside-in audits Wave 100 thesis push 2026-05-19 (output background agents)
- `.claude/rules/dev-readable-doc-language.md` v1.0.2 — sister rule cover narrative language only
- `.claude/rules/user-manual-content-standard.md` §2 row 7-8 — VND/date format mandate cho user manual scope (narrower)
- `.claude/rules/professional-manual-content-standard.md` — sister rule mixed audience
- `.claude/rules/outside-in-coverage-trigger.md` v1.1.0 §3 — triggered this META gap filing
- `.claude/rules/meta-gap-priority.md` §3 — META P1 force-multiplier
- `.claude/rules/audit-to-gap-pipeline.md` §2.5 + §2.7 — state-check protocol + decision-doc code-sync (parallel pattern)
- `.claude/rules/rule-change-process.md` §6.5 Enforcement Parity Mandate
- `.claude/rules/incident-to-rule-pipeline.md` — 5-stage applied (Detect Wave 100 3-audit consensus → Classify no existing checklist cross-bucket → Rule+Enforce paired same Wave 100 plan PR → Self-Test 4 buckets retroactive → Retro Log)
- Wave 72a Bucket F precedent — CSV English narrative violation 2026-05-14, retro round-trip eliminated bởi `dev-readable-doc-language.md` v1.0.0; this gap extends pattern cross-bucket

## Log

- **2026-05-19** — Initial write-up. Step 0 canonical-status lookup completed (Max GAP-679 → GAP-680; ROADMAP §Dropped no match; duplicate scan 2 hits = GAP-423/424 closed narrow scope). State-check §2.5 completed (4 paths grep'd, checklist genuinely new). Filed per 3-audit consensus Wave 100 (persona + failure-mode + benchmark 2026-05-19). Defer rule body write to Wave 100 plan PR draft session để paired same-PR with reviewer-checklist embed.
