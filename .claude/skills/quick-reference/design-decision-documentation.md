# Design Decision Documentation — Template + Example

## Template

```markdown
## Design Decision: [Feature Name]

**Date:** YYYY-MM-DD  **Complexity:** Medium/High  **Participants:** [who]

### Chosen Approach: [Option Name]

**Summary:** [1-2 sentences]

**Rationale:**
- [Reason 1]
- [Reason 2]
- [Key trade-off accepted]

### Rejected Alternatives

**1. [Alternative A]**
- Why considered: [benefit]
- Why rejected: [critical flaw or constraint]

**2. [Alternative B]**
- Why considered: [benefit]
- Why rejected: [critical flaw or constraint]

### Trade-offs Accepted
- **Giving up:** [trade-off] — acceptable because [reason]
- **Gaining:** [benefit]

### Success Criteria
- [ ] [Must-have criterion with metric]
- [ ] [Must-have criterion]

### Implementation Notes
- **Services affected:** [Gateway/Core/Frontend]
- **DB changes:** [New tables/columns]
- **API endpoints:** [New endpoints]

### Risks & Mitigation
- **Risk:** [what could go wrong] → **Mitigation:** [how to handle]

### Review Date
**When to revisit:** [date or trigger]  **Why:** [what would change decision]
```

---

## Example: Student Attendance Storage

```markdown
## Design Decision: Student Attendance Storage

**Date:** 2026-03-13  **Complexity:** Medium  **Participants:** Dev Team

### Chosen Approach: PostgreSQL Table in Core Service

**Summary:** Store attendance in `class_attendance` table with columns: class_id, student_id, date, status (PRESENT/ABSENT/LATE), marked_by_teacher_id, instance_id.

**Rationale:**
- Need durable storage for 1-year history (compliance requirement)
- SQL queries essential for reports ("students absent >3 days this month")
- Current scale (<10k students) doesn't warrant separate service
- Fits bounded context (attendance belongs to Class aggregate)

### Rejected Alternatives

**1. Redis Cache Only**
- Why considered: Ultra-fast writes for real-time marking
- Why rejected: No durability, limited queries, 1-year retention impractical

**2. Separate Attendance Microservice**
- Why considered: Clean domain boundary, independent scaling
- Why rejected: Over-engineering for current scale, unnecessary complexity

### Trade-offs Accepted
- **Giving up:** ~40ms slower writes vs Redis — acceptable for daily attendance marking
- **Gaining:** ACID guarantees, SQL reporting, 1-year retention without memory pressure

### Success Criteria
- [x] Mark 30 students in <2 minutes (<4 sec per student)
- [x] Query attendance history in <500ms (indexed on class_id, date)

### Implementation Notes
- **Services affected:** Core (new table + endpoints)
- **DB changes:**
  ```sql
  CREATE TABLE class_attendance (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES classes(id),
    student_id UUID NOT NULL REFERENCES students(id),
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE')),
    marked_by_teacher_id UUID NOT NULL REFERENCES teachers(id),
    instance_id UUID NOT NULL,
    UNIQUE(class_id, student_id, date, instance_id)
  );
  ```
- **API endpoints:** POST /api/classes/{id}/attendance, GET /api/classes/{id}/attendance?date=...

### Risks & Mitigation
- **Risk:** DB write latency on slow networks → **Mitigation:** Optimistic UI, retry logic
- **Risk:** Duplicate marks if teacher submits twice → **Mitigation:** UNIQUE constraint

### Review Date
**When:** Q3 2026 (after 6 months usage data)  **Why:** Evaluate if separate service needed
```
