# Acceptance Criteria — P&lt;N&gt; &lt;Persona Name&gt;

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** P&lt;N&gt;
**Persona name (VN):** &lt;Tên persona tiếng Việt&gt;
**Persona name (EN):** &lt;English name&gt;
**Last-Updated:** YYYY-MM-DD
**Reviewer (Phase 1 — author):** &lt;handle, role&gt;
**Reviewer (Phase 2 — domain expert):** &lt;TBD — Product Owner / Education domain expert / Real persona representative&gt;
**Tier:** &lt;1 Primary / 2 Secondary / 3 Future&gt;
**Tracking:** GAP-151 (Phase 1 — AC framework) → GAP-152 (Round 1 review execution) → GAP-153 (secondary persona AC)

---

## 0. Context

### Scale assumption (from `personas-catalog.md`)
- **Users:** &lt;e.g. 500 students, 30 teachers, 10 staff&gt;
- **Data volume:** &lt;e.g. 30 classes, 1000 enrollments, 5000 attendance records/week&gt;
- **Usage pattern:** &lt;daily / weekly / seasonal — peak moments&gt;

### Organization archetype
- **Type:** &lt;e.g. Public K-12 School / Solo gia sư / Trung tâm dạy thêm&gt;
- **Hierarchy:** &lt;reporting structure relevant to AC: Principal → VP → Department Head → Teacher → Student&gt;
- **Decision-making:** &lt;who signs up, who manages billing, who handles operations&gt;

### Revenue tier mapping
- **Expected tier:** &lt;FREE / BASIC / PREMIUM / ENTERPRISE&gt;
- **Reason:** &lt;why this tier — Solo Teacher = FREE/BASIC; K-12 School = PREMIUM/ENTERPRISE&gt;

### Real-world reviewer profile
- **Acting role:** &lt;e.g. "Hiệu trưởng trường THCS công lập 800 học sinh ở quận trung tâm Hà Nội"&gt;
- **Critical concerns:** &lt;list 3-5 top concerns this persona has when evaluating platform&gt;

---

## AC Categories (6 standardized)

Each AC has format:
- **AC-&lt;CATEGORY&gt;-&lt;NUM&gt;** (3-digit zero-padded ID — e.g. AC-ONBOARD-001)
- **Statement** (1 sentence — what must be verifiable)
- **Test** (concrete scenario — reviewer can simulate)
- **Fail signal** (what reviewer observes if system gaps)
- **Status** (PASS / PARTIAL / FAIL — filled at review time, not at AC creation time)
- **Linked gap** (if FAIL → existing GAP-XXX or NEW gap to file)

---

## 1. Onboarding AC

Initial signup → tenant provisioning → first usable state.

- [ ] **AC-ONBOARD-001:** &lt;persona can complete signup + initial setup in &lt;X&gt; minutes/hours&gt;
  - **Test:** &lt;step-by-step scenario&gt;
  - **Fail signal:** &lt;observable gap — UI missing, error, friction&gt;
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** &lt;GAP-XXX or "—"&gt;

- [ ] **AC-ONBOARD-002:** &lt;...&gt;

(Recommended count: 3-5 ACs)

---

## 2. Daily Operations AC

Recurring workflows after onboarding (attendance, grading, communication, scheduling).

- [ ] **AC-OPS-001:** &lt;e.g. bulk import 500 students via xlsx in &lt;5 min&gt;
  - **Test:** Upload valid xlsx với 500 rows, assert accounts created + email/SMS credentials sent
  - **Fail signal:** No xlsx upload UI OR upload fails OR credentials not distributed
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (xlsx import)

- [ ] **AC-OPS-002:** &lt;...&gt;

(Recommended count: 5-10 ACs — most populated category)

---

## 3. Financial / Admin AC

Billing, invoicing, payroll, financial reporting, tenant administration.

- [ ] **AC-FIN-001:** &lt;...&gt;
  - **Test:** &lt;...&gt;
  - **Fail signal:** &lt;...&gt;
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** &lt;...&gt;

(Recommended count: 3-5 ACs)

---

## 4. Communication AC (stakeholders)

Notifications, parent engagement, teacher-student messaging, announcements, reporting to authorities.

- [ ] **AC-COMM-001:** &lt;...&gt;
  - **Test:** &lt;...&gt;
  - **Fail signal:** &lt;...&gt;
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** &lt;...&gt;

(Recommended count: 2-5 ACs)

---

## 5. Edge Cases AC

Failure scenarios, partial failures, peak loads, data corruption recovery, unusual user behavior.

- [ ] **AC-EDGE-001:** &lt;...&gt;
  - **Test:** &lt;...&gt;
  - **Fail signal:** &lt;...&gt;
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** &lt;...&gt;

(Recommended count: 2-5 ACs)

---

## 6. Exit / Termination AC

Tenant offboarding, data export, account deletion, contract termination, MOET reporting (if K-12).

- [ ] **AC-EXIT-001:** &lt;...&gt;
  - **Test:** &lt;...&gt;
  - **Fail signal:** &lt;...&gt;
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** &lt;...&gt;

(Recommended count: 2-3 ACs)

---

## Scoring

**Total ACs:** &lt;N&gt; (sum across 6 categories)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported (production-ready for this persona) |
| 60-84% | ⚠️ Persona partially supported (usable but with gaps; defer GA for this persona) |
| 30-59% | 🔴 Persona NOT supported (major gaps; not production-ready) |
| &lt;30% | ❌ Persona NOT viable (fundamental misfit; consider deferring to Tier 2/3 or out-of-scope) |

---

## Gap Linkage Summary

Gather all FAIL/PARTIAL ACs with linked gaps into one table for review report digest:

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-OPS-001 | FAIL | GAP-051 | 🔵 OPEN | P0 |
| AC-COMM-002 | PARTIAL | GAP-063 | 🟡 PARTIAL | P1 |
| ... | ... | ... | ... | ... |

**New gaps to file** (FAIL ACs without existing gap — go through `audit-to-gap-pipeline.md` Step 2.5 state-check before filing):
- &lt;list of NEW candidate gaps surfaced by this persona review&gt;

---

## Cross-References

- **Persona source:** [`../personas-catalog.md`](../personas-catalog.md) §P&lt;N&gt;
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **Review reports:** [`../persona-reviews/`](../persona-reviews/) (output of GAP-152 quarterly reviews)
- **AC framework gap:** [GAP-151](../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (this template)
- **Review execution gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check

---

## How to Use This Template

1. **Copy this file** to `P<N>-<persona-slug>.md` (e.g. `P5-k12-school.md`)
2. **Fill §0 Context** from `personas-catalog.md` + research / interviews
3. **Derive ACs** from persona's "Key needs" + "Pain points" + real-world workflow walkthrough
4. **Each AC must be:**
   - **Specific** (not "good UX" — quantify: "≤3 clicks", "&lt;5 min", "500 rows")
   - **Verifiable** (concrete Test scenario reviewer can simulate)
   - **Falsifiable** (clear Fail signal — observable gap if system doesn't meet AC)
5. **Cross-link existing gaps** (GAP-051..064 + others) — don't duplicate gap creation work
6. **Status starts blank** — filled at review time (GAP-152 Round 1)
7. **Recommended size:** 15-30 ACs per persona (more for complex personas like P5 K-12)

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Vague ACs ("system is fast") | Specific ACs ("p95 latency &lt;500ms") |
| AC without Test scenario | Every AC has reproducible Test |
| AC without Fail signal | Every AC has observable failure mode |
| Mark Status="PASS" without evidence | Status filled at review time với evidence |
| 100+ ACs per persona | 15-30 ACs (curated, high-impact) |
| Cross-persona ACs in single doc | Each persona doc = standalone; cross-cuts in review report |
| Skip §0 Context | Context drives AC selection — without it, ACs become generic |

---

## Log

- &lt;YYYY-MM-DD&gt; — Initial AC set v1 (author: &lt;handle&gt;)
- &lt;YYYY-MM-DD&gt; — Domain expert reviewer &lt;name&gt; sign-off, status updates
- &lt;YYYY-MM-DD&gt; — GAP-152 Round 1 review completed, score &lt;X%&gt;
