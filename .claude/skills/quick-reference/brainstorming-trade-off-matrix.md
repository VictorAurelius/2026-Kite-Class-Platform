# Trade-off Matrix — Brainstorming Reference

## Template

```markdown
## Option Analysis

| Criterion | Weight | Option A | Option B | Option C |
|-----------|--------|----------|----------|----------|
| Performance | X% | score (weighted) | ... | ... |
| Scalability | X% | ... | ... | ... |
| Complexity | X% | ... | ... | ... |
| Maintainability | X% | ... | ... | ... |
| Cost (dev time) | X% | ... | ... | ... |
| Cost (infra) | X% | ... | ... | ... |
| **Total** | 100% | **X** | **X** | **X** |
```

**Scoring:** 1-5 per criterion. Multiply by weight. Highest total = recommended.

---

## Example: Attendance Storage Options

```markdown
## Design Decision: Where to Store Attendance?

### Option A: Redis Cache Only
**Pros:** Very fast writes (<10ms), simple, already have Redis
**Cons:** No durability (crash = data loss), no complex queries, 1-year retention impractical
**Use Case:** Real-time indicators only, not primary storage

### Option B: PostgreSQL Table (Recommended)
**Pros:** Durable (ACID), queryable (SQL reports), 1-year history, fits existing schema
**Cons:** Slower than Redis (~50ms write), needs indexes, migration overhead
**Use Case:** Primary storage for all attendance records

### Option C: Separate Attendance Microservice
**Pros:** Highly scalable, isolated, domain-driven
**Cons:** Over-engineering for <10k students, network latency, complex deployment
**Use Case:** >100k students, complex workflows

### Trade-off Matrix

| Criterion | Weight | Redis (A) | PostgreSQL (B) | Microservice (C) |
|-----------|--------|-----------|----------------|------------------|
| Performance | 20% | 5 (100) | 4 (80) | 3 (60) |
| Durability | 30% | 1 (30) | 5 (150) | 5 (150) |
| Query Capability | 25% | 2 (50) | 5 (125) | 5 (125) |
| Simplicity | 15% | 5 (75) | 4 (60) | 1 (15) |
| Dev Cost | 10% | 5 (50) | 4 (40) | 1 (10) |
| **Total** | 100% | **305** | **455 ⭐** | **360** |

**Decision:** Option B (PostgreSQL) — best balance for current scale
```
