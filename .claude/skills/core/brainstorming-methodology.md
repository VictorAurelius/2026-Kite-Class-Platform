# Skill: Socratic Brainstorming

**Version:** 1.0 (Superpowers-inspired)
**Last Updated:** 2026-03-13
**Purpose:** Refine ideas through questions before coding, explore alternatives, document design decisions

---

## 📋 Overview

Replace "jump straight to coding" with **Socratic-method design refinement**:
- Ask questions to clarify requirements
- Explore alternative approaches
- Evaluate trade-offs systematically
- Document decisions with rationale

**Target:** +40% planning accuracy, fewer requirements changes mid-PR

---

## 🎯 When to Use This Skill

**Mandatory for:**
- ✅ New features (Medium+ complexity)
- ✅ Architectural decisions (service boundaries, data models)
- ✅ Cross-service integrations
- ✅ Unclear or ambiguous requirements
- ✅ PRs marked "Complexity: Medium/High" in implementation plan

**Skip for:**
- ⏭️ Simple bug fixes (well-defined problem)
- ⏭️ Typo corrections
- ⏭️ Documentation updates
- ⏭️ Configuration changes (application.yml)

**When in doubt:** Spend 10 minutes brainstorming. Better to over-clarify than to rework later.

---

## 🔄 3-Step Process (20-40 minutes)

### Step 1: Question Assumptions (10 min)

**Goal:** Understand the REAL problem before proposing solutions

#### Template Questions:

**1. Problem Definition**
```
Q: What problem are we solving?
   - User pain point?
   - Business requirement?
   - Technical debt?

Q: Why is this important NOW?
   - User feedback?
   - Blocker for other features?
   - Compliance requirement?
```

**2. User Context**
```
Q: Who is the primary user?
   - Student?
   - Teacher?
   - Admin?
   - Parent? (future)

Q: What is their workflow?
   - When do they need this?
   - How often? (daily, weekly, monthly)
   - What's their current workaround?
```

**3. Success Criteria**
```
Q: How do we know we succeeded?
   - Specific metrics? (e.g., "<2 sec to mark 30 students")
   - User satisfaction? (survey score)
   - Adoption rate? (% teachers using feature)

Q: What does "done" look like?
   - MVP vs full feature
   - Acceptance criteria list
```

**4. Constraints**
```
Q: What are the constraints?
   - Performance? (latency, throughput)
   - Data volume? (10 students vs 1000)
   - Budget? (infrastructure cost)
   - Timeline? (must ship by X date)
```

---

### Example: Student Attendance Feature

**User Request:** "Teachers need to track student attendance"

**Question Assumptions:**

```markdown
## Brainstorming Session: Student Attendance

Q: What problem are we solving?
A: Teachers manually track attendance on paper, parents don't know if child attended class

Q: Why is this important NOW?
A: School policy requires daily attendance reports to parents, manual process error-prone

Q: Who is the primary user?
A: Teacher (marks attendance), Student/Parent (view attendance history)

Q: What is their workflow?
A: Teacher takes attendance at start of class (5 min), marks present/absent/late
   Parents check attendance weekly via mobile app

Q: How do we know we succeeded?
A: - Teacher can mark 30 students in <2 minutes
   - 99.9% accuracy (no missed/duplicate marks)
   - Parents receive real-time notification if absent

Q: What are the constraints?
A: - Must work on slow mobile networks (3G)
   - Classes have 10-50 students
   - Attendance data retained for 1 year (reports)
   - Must integrate with existing Class/Student models
```

---

### Step 2: Explore Trade-offs (15 min)

**Goal:** Compare multiple approaches systematically before choosing

#### Trade-off Matrix Template:

```markdown
## Option Analysis

| Criterion | Option A | Option B | Option C |
|-----------|----------|----------|----------|
| **Performance** | Fast | Medium | Slow |
| **Scalability** | Limited | High | Very High |
| **Complexity** | Low | Medium | High |
| **Maintainability** | Easy | Medium | Hard |
| **Cost** (dev time) | Low | Medium | High |
| **Cost** (infra) | Low | Medium | High |
| **Extensibility** | Limited | Good | Excellent |
```

**Scoring (optional):**
- Assign weights to criteria (e.g., Performance 30%, Complexity 20%)
- Score each option 1-5
- Calculate weighted total
- Highest score = recommended option

---

### Example: Attendance Storage Options

```markdown
## Design Decision: Where to Store Attendance?

### Option A: Redis Cache Only

**Pros:**
- ✅ Very fast writes/reads (<10ms)
- ✅ Simple implementation (key-value store)
- ✅ Low infra cost (already have Redis)

**Cons:**
- ❌ No durability (data loss if Redis crashes)
- ❌ Limited query capability (can't do complex reports)
- ❌ Hard to retain 1-year history (memory constraint)

**Use Case:** Real-time presence indicators, not primary storage

---

### Option B: PostgreSQL Table (Recommended)

**Pros:**
- ✅ Durable (ACID guarantees)
- ✅ Queryable (SQL for reports: "students absent >3 days")
- ✅ 1-year history no problem (disk-based)
- ✅ Fits existing schema (attendance belongs to class/student)

**Cons:**
- ⚠️ Slower than Redis (~50ms write)
- ⚠️ Need indexes for fast queries
- ⚠️ Slightly more complex (migrations)

**Use Case:** Primary storage for all attendance records

---

### Option C: Separate Attendance Microservice

**Pros:**
- ✅ Highly scalable (independent scaling)
- ✅ Isolated (attendance issues don't affect core)
- ✅ Domain-driven design (attendance is bounded context)

**Cons:**
- ❌ Over-engineering for current scale (<10k students)
- ❌ Network latency between services
- ❌ Complex deployment (another service to manage)
- ❌ Higher dev time (need API, auth, etc.)

**Use Case:** >100k students, complex attendance workflows

---

## Trade-off Matrix

| Criterion | Weight | Redis (A) | PostgreSQL (B) | Microservice (C) |
|-----------|--------|-----------|----------------|------------------|
| **Performance** | 20% | 5 (100) | 4 (80) | 3 (60) |
| **Durability** | 30% | 1 (30) | 5 (150) | 5 (150) |
| **Query Capability** | 25% | 2 (50) | 5 (125) | 5 (125) |
| **Simplicity** | 15% | 5 (75) | 4 (60) | 1 (15) |
| **Cost** (dev) | 10% | 5 (50) | 4 (40) | 1 (10) |
| **Total** | 100% | **305** | **455** ⭐ | **360** |

**Decision:** Option B (PostgreSQL) - Best balance for current needs
```

---

### Step 3: Document Decisions (10 min)

**Goal:** Record WHY we chose this approach for future reference

#### Documentation Template:

```markdown
## Design Decision: [Feature Name]

**Date:** YYYY-MM-DD
**Complexity:** Medium/High
**Participants:** [who was involved in decision]

---

### Chosen Approach: [Option Name]

**Summary:**
[1-2 sentence description of what we're building]

**Rationale:**
- [Reason 1 why this approach]
- [Reason 2 why this approach]
- [Key trade-off accepted]

---

### Rejected Alternatives

**1. [Alternative A]**
- **Why considered:** [benefit]
- **Why rejected:** [critical flaw or constraint]

**2. [Alternative B]**
- **Why considered:** [benefit]
- **Why rejected:** [critical flaw or constraint]

---

### Trade-offs Accepted

**What we're giving up:**
- [Trade-off 1 - why acceptable]
- [Trade-off 2 - why acceptable]

**What we're gaining:**
- [Benefit 1]
- [Benefit 2]

---

### Success Criteria

**Must have:**
- [ ] [Criterion 1 with metric]
- [ ] [Criterion 2 with metric]

**Nice to have:**
- [ ] [Optional criterion]

---

### Implementation Notes

**Services affected:** [Gateway/Core/Frontend]
**Database changes:** [New tables/columns]
**API endpoints:** [New endpoints]
**Dependencies:** [New libraries if any]

---

### Risks & Mitigation

**Risk 1:** [What could go wrong]
- **Mitigation:** [How we'll handle it]

**Risk 2:** [What could go wrong]
- **Mitigation:** [How we'll handle it]

---

### Review Date

**When to revisit:** [Date or trigger - e.g., "if >10k students"]
**Why:** [What would change our decision]
```

---

### Example Documentation:

```markdown
## Design Decision: Student Attendance Storage

**Date:** 2026-03-13
**Complexity:** Medium
**Participants:** Development Team

---

### Chosen Approach: PostgreSQL Table in Core Service

**Summary:**
Store attendance records in `class_attendance` table in Core service database, with columns: class_id, student_id, date, status (PRESENT/ABSENT/LATE), marked_by_teacher_id.

**Rationale:**
- Need durable storage for 1-year history (compliance requirement)
- SQL queries essential for reports ("students absent >3 days this month")
- Current scale (<10k students) doesn't warrant separate service
- Fits bounded context (attendance belongs to Class aggregate)

---

### Rejected Alternatives

**1. Redis Cache Only**
- **Why considered:** Ultra-fast writes for real-time marking
- **Why rejected:** No durability, limited query capability, 1-year retention impractical

**2. Separate Attendance Microservice**
- **Why considered:** Clean domain boundary, independent scaling
- **Why rejected:** Over-engineering for current scale, unnecessary complexity

---

### Trade-offs Accepted

**What we're giving up:**
- ~40ms slower writes vs Redis (50ms vs 10ms) - acceptable for daily attendance marking
- Core service grows slightly (~1GB/year data) - well within capacity

**What we're gaining:**
- ACID guarantees (no data loss)
- SQL reporting (complex queries easy)
- 1-year retention with no memory pressure

---

### Success Criteria

**Must have:**
- [x] Mark 30 students in <2 minutes (<4 sec per student)
- [x] 99.9% accuracy (unique constraint on date+student prevents duplicates)
- [x] Query attendance history in <500ms (indexed on class_id, date)

**Nice to have:**
- [ ] Real-time parent notifications (future: webhook to notification service)
- [ ] Attendance analytics dashboard (future: after 3 months data)

---

### Implementation Notes

**Services affected:** Core (new table + endpoints)
**Database changes:**
```sql
CREATE TABLE class_attendance (
  id UUID PRIMARY KEY,
  class_id UUID NOT NULL REFERENCES classes(id),
  student_id UUID NOT NULL REFERENCES students(id),
  date DATE NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE')),
  marked_by_teacher_id UUID NOT NULL REFERENCES teachers(id),
  marked_at TIMESTAMP NOT NULL,
  instance_id UUID NOT NULL, -- Multi-tenant
  UNIQUE(class_id, student_id, date, instance_id)
);
CREATE INDEX idx_attendance_class_date ON class_attendance(class_id, date);
```

**API endpoints:**
- POST /api/classes/{classId}/attendance (mark attendance)
- GET /api/classes/{classId}/attendance?date={date} (view daily)
- GET /api/students/{studentId}/attendance?from={date}&to={date} (history)

**Dependencies:** None (uses existing models)

---

### Risks & Mitigation

**Risk 1:** Database write latency on slow networks
- **Mitigation:** Optimistic UI (mark locally, sync async), retry logic

**Risk 2:** Duplicate marks if teacher submits twice
- **Mitigation:** UNIQUE constraint prevents duplicates, UI shows confirmation

**Risk 3:** Scale issues if >10k students
- **Mitigation:** Monitor query performance, add database read replica if needed

---

### Review Date

**When to revisit:** Q3 2026 (after 6 months of usage data)
**Why:** Evaluate if separate service needed based on:
- Query performance metrics
- Data volume growth
- Feature requests (complex workflows)
```

---

## 🔗 Integration with Existing Skills

**Before Brainstorming:**
- Review `architecture-overview.md` for service boundaries
- Check `api-design.md` for endpoint design patterns
- Scan `implementation-plan.md` for related features

**During Brainstorming:**
- Reference `database-design.md` for schema patterns
- Use `cross-service-data-strategy.md` if integration needed
- Consider `performance-testing-standards.md` constraints

**After Brainstorming:**
- Update `implementation-plan.md` with design decision reference
- Link to specific PR section
- Add to `documents/07-archived/research/` if significant architectural change

---

## 📏 Success Metrics

**Track for each brainstorming session:**
- Time spent (target: 20-40 min, not >1 hour)
- Alternatives explored (target: ≥2 options)
- Design decision documented (target: 100%)

**Measure overall:**
- % of Medium+ PRs with brainstorming (target: 100%)
- Requirements changes mid-PR (target: <20%, down from 40%)
- Planning accuracy (estimated vs actual time) (target: 80%+)

---

## 🎯 Trigger Phrases

Auto-activate this skill when detecting:
- "plan this feature"
- "design review needed"
- "brainstorm approach"
- "evaluate options"
- "which approach should we use"
- "PR [X.X]" + "Complexity: Medium/High"

---

## ✅ Quick Reference Checklist

Before coding, verify:

- [ ] **Step 1:** Did I question assumptions? (what/why/who/success criteria)
- [ ] **Step 2:** Did I explore ≥2 alternatives? (trade-off matrix)
- [ ] **Step 3:** Did I document the decision? (in implementation-plan.md)
- [ ] **Step 3:** Did I record WHY alternatives rejected?
- [ ] **Step 3:** Did I define success criteria?

**If rushed:** Minimum viable brainstorming = 10 min questioning + document chosen approach with 1-line rationale

---

**Last Updated:** 2026-03-13
**Author:** Claude Code (Superpowers-inspired)
**Status:** ✅ Active - Mandatory for Medium+ complexity PRs
