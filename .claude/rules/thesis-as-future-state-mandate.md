---
paths:
  - "documents/08-thesis/**"
  - "documents/03-planning/waves/**"
  - "documents/04-quality/gaps/**"
---

# Thesis-as-Future-State Mandate — thesis = goal state Phase 1.5+ must deliver

**Priority:** 🟠 MANDATORY — thesis-reality drift governance
**Version:** 1.0.0
**Created:** 2026-06-01
**Last-Reviewed:** 2026-06-01
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test trên 2026-06-01 Zalo audit incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codify treatment of thesis content as forward commitment thay vì backward-fact)
**Applies to:** Mọi thesis claim trong `documents/08-thesis/chapter-*.md` về features ("đã có / đã tích hợp / đã kết nối / hỗ trợ"). Mọi wave plan Phase 1.5+ scope. Mọi gap filing tham chiếu thesis section.

---

## 1. The Rule

> **Thesis content (`documents/08-thesis/`) describes the GOAL STATE của hệ thống — KHÔNG phải fact ở thời điểm hiện tại. Claims trong thesis chapter 1-2 về features "đã có / đã tích hợp / đã kết nối / hỗ trợ" là forward commitments — Phase 1 BETA shipping minimum interpretation (vd passive CTA = "kết nối"); Phase 1.5+ (paid tier) MUST deliver full claim interpretation trước khi thesis ship academic submission.**

Thesis là academic deliverable mô tả vision system; KHÔNG sửa wording khi reality drift. Thay vào đó, Phase 1.5 work đóng vai trò delivery — make claims TRUE.

Pattern thesis-reality drift xảy ra 2026-06-01 với Zalo OA: thesis Ch1 + Ch2 claim "đã kết nối Zalo OA" trong khi Phase 1 BETA chỉ ship passive CTA. Per rule này: claim = goal state; passive CTA = minimum interpretation Phase 1 BETA acceptable; Phase 1.5 commits to deliver active push (full interpretation).

---

## 2. Why this rule exists

### 2.1 Anti-pattern alternative (thesis edit)

Common reaction khi phát hiện thesis drift = sửa thesis wording xuống match reality. Problems:
- Thesis = academic deliverable; mỗi edit cần GVHD re-review + version bump
- Wording-down dilutes thesis ambition (downgrade "đã tích hợp" → "đang phát triển")
- Doesn't drive Phase 1.5 work — drift accumulates over thời gian
- Misses opportunity: thesis claims = product roadmap for free

### 2.2 Better: thesis = forward commitment

Treat thesis as **roadmap reference + acceptance criteria source**:
- Phase 1 BETA ships **minimum interpretation** of each thesis claim (passive CTA satisfies "kết nối")
- Phase 1.5+ ships **full interpretation** of claims that need richer scope (active push, group integration, deep features)
- Wave plans Phase 1.5+ reference thesis sections as AC source
- Audit pipeline auto-files gaps for thesis claims không có matching delivery commitment

### 2.3 Wave 102 thesis ship context

Thesis V1 ship academic submission Wave 102 + thesis-N follow-ups. Thesis-N waves CAN edit thesis wording cho academic-rubric reasons (grammar / clarity / IEEE format) per `thesis-content-standard.md` §6.1, NHƯNG KHÔNG được edit để match degraded reality. Reality phải catch up to claim qua Phase 1.5 delivery.

---

## 3. Mapping thesis claim → Phase delivery

### 3.1 Minimum vs full interpretation

| Thesis verb | Minimum interpretation (Phase 1 BETA OK) | Full interpretation (Phase 1.5+ required) |
|---|---|---|
| "đã kết nối" | Deep-link CTA / config field exists | Active 2-way integration |
| "đã tích hợp" | Adapter scaffold / passive surface | Active dispatch + audit log + fallback |
| "hỗ trợ" | Enum value / config flag | Production-ready feature |
| "có thể" | Architecture allows | Implemented + tested + documented |
| "tự động" | Manual workflow exists | Scheduled job / event-driven trigger |
| "đa nền tảng" | 1 platform + roadmap | ≥2 platforms shipped |

### 3.2 Acceptance criteria sourcing

Wave plan Phase 1.5+ scope items SHOULD reference thesis section:

```markdown
### Bucket A — Zalo OA active push (GAP-819)

- **Thesis source:** Ch1 §1.1.2 + Ch1 §1.4 "đã kết nối Zalo OA"
- **Interpretation:** Phase 1 BETA shipped passive CTA (minimum); Phase 1.5 ships active ZNS push (full)
- **Acceptance:** ZaloZnsAdapter implements NotificationChannel + 3 templates approved + IT verified
```

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Sửa thesis "đã kết nối Zalo OA" → "đang phát triển Zalo OA" để match reality | Giữ thesis claim; file Phase 1.5 gap delivering full interpretation |
| Treat thesis Ch1 + Ch2 facts as outdated rotten content | Treat as forward roadmap reference for Phase 1.5+ |
| Skip checking thesis claims khi plan Phase 1.5 wave | Grep thesis chapter for verb patterns ("đã" / "hỗ trợ") → check Phase 1 BETA minimum interpretation shipped → file Phase 1.5 gap nếu full interpretation chưa |
| File gap blocking thesis submission vì claim chưa true | Phase 1 BETA minimum interpretation đủ cho thesis submission; full interpretation = Phase 1.5+ trajectory |
| Lose thesis claim because no matching code | Claim = commitment; file gap để track delivery |
| Refuse to ship thesis vì Phase 1.5 work chưa xong | Thesis ships at Phase 1 BETA minimum; Phase 1.5 ships post-thesis academic submission |

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active now)

Pre-merge review cho:
- (a) PR touching `documents/08-thesis/chapter-*.md` (thesis content edit)
- (b) PR creating wave plan Phase 1.5+ scope
- (c) PR filing gap với phase=phase-1.5-paid OR phase-2 OR phase-3

Reviewer hỏi:

- [ ] (Thesis edit) Edit này có downgrade claim để match degraded reality không? Nếu CÓ → REJECT; file Phase 1.5 gap thay vì edit
- [ ] (Wave plan Phase 1.5+) Bucket scope reference thesis section nào? Bucket §Scope có cite `documents/08-thesis/chapter-X.md` không?
- [ ] (Gap filing Phase 1.5+) Gap problem statement reference thesis claim source? Nếu YES → ghi trong gap §References

### 5.2 PR template extension (deferred per `incident-to-rule-pipeline.md` §3.1)

Future enhancement: `.github/PULL_REQUEST_TEMPLATE.md` add row:
> - [ ] **Thesis claim coverage** — nếu PR ship Phase 1.5+ feature, gap problem cite thesis section (Ch1 §X.Y) source

Defer until 2nd recurrence verify reviewer-checklist sufficient.

### 5.3 Audit pipeline integration (recommend, deferred)

Audit-to-gap-pipeline.md §2.X (new step): khi audit phase planning, grep thesis chapter for verb patterns ("đã" + "hỗ trợ" + "tích hợp" + "kết nối"):
- For each match → check Phase 1 BETA minimum shipped (gap-status.csv query)
- Nếu NOT shipped → flag P1 minimum interpretation gap
- Nếu shipped minimum but no Phase 1.5+ full interpretation gap → flag P2 full interpretation candidate

Defer detector wiring ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard. v1.0.0 enforcement = reviewer-checklist + worked self-test sufficient.

### 5.4 Override mechanism

Genuine exception khi thesis claim genuinely outdated/incorrect (vd: feature deprecated / superseded):

```
git commit -m "...
THESIS_CLAIM_EDIT: <thesis section> — <reason — e.g. feature superseded by ADR-NNN; original claim genuinely obsolete>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review (likely thesis written with too-aggressive claims).

---

## 6. Worked self-test — 2026-06-01 Zalo audit incident

### 6.1 Scenario

User asks 2026-06-01: "audit nội dung thesis có đề cập đến zalo OA, group zalo cho phụ huynh; check xem đã design chưa; trình bày design thành báo cáo architecture; tạo gaps phase 1.5".

Audit kết quả:
- Thesis Ch1 + Ch2 claim "đã kết nối / đã tích hợp Zalo OA"
- Reality: Passive CTA only (GAP-660 DONE Wave 98); active push defer Phase 2 (GAP-063b)

Initial agent reaction (BEFORE rule): file gap để fix thesis wording match reality.

User correction: "Thesis wording cần fix => không cần, thêm meta để hiểu thesis là goal mà phase 1.5 cần đạt được".

### 6.2 Apply rule retroactively

Per §3.1 Mapping table:
- "đã kết nối Zalo OA" minimum interpretation = deep-link CTA exists ✅ Phase 1 BETA GAP-660 shipped → MINIMUM MET
- Full interpretation = active push notification → Phase 1.5 commitment via GAP-819 (NEW filed này PR)

Per §4 Anti-pattern: "Don't sửa thesis để match reality. Do file Phase 1.5 gap delivering full interpretation" — matches user direction.

Per §5.1 reviewer-checklist: this PR filing Phase 1.5 gap (GAP-819 + GAP-820) đúng pattern; gap §References cite Ch1 §1.1.2 + Ch1 §1.2.5 + Ch1 §1.4 + Ch2 source ✅.

**Verdict:** Rule fires correctly trên originating incident. Self-test PASS ✅.

**Counterfactual without rule:** Future thesis-reality drifts → ad-hoc decisions edit-thesis vs file-gap → inconsistent treatment + thesis ambition drift. With rule: standard treatment (thesis = goal, Phase 1.5 = delivery), eliminate drift class permanently.

---

## 7. Relationship to other rules

- **`thesis-content-standard.md`** — thesis content quality rubric (academic standard); rule này extends với phase-delivery commitment dimension
- **`outside-in-coverage-trigger.md`** §2.1 architecture-decision keywords — thesis claims = architecture decision proxies; this rule directs proper handling
- **`audit-to-gap-pipeline.md`** §2.5 state-check — rule này extends state-check to thesis-claim coverage check
- **`meta-gap-priority.md`** §3 — thesis-driven gaps Phase 1.5+ get business-logic-tier (rooted in declared product narrative)
- **`gap-architecture-v2.md`** §3 — gap-status.csv canonical; gaps filed per thesis claim get phase=phase-1.5-paid+
- **`output-review-mandate.md`** §3 — paired same-PR matrix row "Thesis-as-future-state alignment" tracking standard
- **`incident-to-rule-pipeline.md`** — rule này = direct output 2026-06-01 user-flagged direction "thesis = goal Phase 1.5 cần đạt" applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 + paired GAP-819 + GAP-820 + zalo-integration-design.md all ship same PR

---

## 8. Log

- **2026-06-01 (v1.0.0):** Rule created in response to user direction 2026-06-01 "Thesis wording cần fix => không cần, thêm meta để hiểu thesis là goal mà phase 1.5 cần đạt được" — sau audit Zalo OA thesis drift discovery. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged scope direction) → Classify ✓ (no existing rule treats thesis as forward commitment; `thesis-content-standard.md` covers academic rubric quality only; nothing covers thesis-reality drift class) → Rule+Enforce ✓ (this file + reviewer-checklist §5.1 + worked self-test §6 + paired same-PR GAP-819 + GAP-820 + zalo-integration-design.md per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên Zalo originating incident — rule fires correctly + counterfactual eliminate ad-hoc edit-vs-file decisions) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-implicit thesis-as-goal treatment; no constraint loosening; existing thesis content grandfathered with current claims serving as Phase 1.5+ roadmap; rule applies prospectively từ this PR forward 2026-06-01). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: thesis = future state) + ✅ unique (no overlap với thesis-content-standard quality rubric) + ✅ widely applicable (every thesis claim + Phase 1.5+ wave/gap) + ✅ body discipline §1 ≤2 "and" conjunctions. CI grep detector (§5.3) + memory auto-load + PR template (§5.2) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + worked self-test §6 sufficient cho v1.0.0.
