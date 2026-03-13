# Brainstorming Question Templates

**Quick Reference** - Socratic Design Refinement

---

## Step 1: Question Assumptions (10 min) 🤔

### Problem Definition
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

### User Context
```
Q: Who is the primary user?
   - Student? Teacher? Admin? Parent?

Q: What is their workflow?
   - When do they need this?
   - How often? (daily, weekly, monthly)
   - What's their current workaround?
```

### Success Criteria
```
Q: How do we know we succeeded?
   - Specific metrics? (e.g., "<2 sec load time")
   - User satisfaction? (survey score)
   - Adoption rate? (% users using feature)

Q: What does "done" look like?
   - MVP vs full feature
   - Acceptance criteria list
```

### Constraints
```
Q: What are the constraints?
   - Performance? (latency, throughput)
   - Data volume? (10 students vs 1000)
   - Budget? (infrastructure cost)
   - Timeline? (must ship by X date)
```

---

## Step 2: Explore Trade-offs (15 min) ⚖️

### Trade-off Matrix Template

| Criterion | Weight | Option A | Option B | Option C |
|-----------|--------|----------|----------|----------|
| Performance | 20% | 5 (100) | 4 (80) | 3 (60) |
| Durability | 30% | 3 (90) | 5 (150) | 5 (150) |
| Complexity | 25% | 5 (125) | 3 (75) | 1 (25) |
| Cost (dev) | 15% | 5 (75) | 4 (60) | 2 (30) |
| Extensibility | 10% | 2 (20) | 4 (40) | 5 (50) |
| **TOTAL** | 100% | **410** | **405** | **315** |

**Decision:** Option A (highest score)

### For Each Option, Ask:

**Pros:**
- ✅ [Benefit 1]
- ✅ [Benefit 2]
- ✅ [Benefit 3]

**Cons:**
- ❌ [Drawback 1]
- ❌ [Drawback 2]
- ❌ [Drawback 3]

**Use Case:**
- Best for: [scenario]
- Not suitable for: [scenario]

---

## Step 3: Document Decision (10 min) 📝

### Decision Doc Template

```markdown
## Design Decision: [Feature Name]

**Date:** YYYY-MM-DD
**Complexity:** Medium/High
**Participants:** [who decided]

---

### Chosen Approach: [Option Name]

**Summary:** [1-2 sentence description]

**Rationale:**
- [Reason 1 why this approach]
- [Reason 2 why this approach]
- [Key trade-off accepted]

---

### Rejected Alternatives

**1. [Alternative A]**
- Why considered: [benefit]
- Why rejected: [critical flaw]

**2. [Alternative B]**
- Why considered: [benefit]
- Why rejected: [critical flaw]

---

### Trade-offs Accepted

**What we're giving up:**
- [Trade-off 1] - why acceptable
- [Trade-off 2] - why acceptable

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

### Review Date

**When to revisit:** [Date or trigger]
**Why:** [What would change our decision]
```

---

## Common Mistakes ❌

- ❌ Jumping to solution without questioning assumptions
- ❌ Only considering 1 option (no alternatives explored)
- ❌ Not documenting the "why" (only "what")
- ❌ Forgetting to define success criteria
- ❌ No review date (decision set in stone forever)

## Success Criteria ✅

- ✅ ≥2 alternatives explored
- ✅ Trade-offs documented with scoring
- ✅ Decision rationale clear (team can understand 6 months later)
- ✅ Success criteria measurable
- ✅ Review date set

---

## When to Use

**Mandatory for:**
- Medium+ complexity features
- Architectural decisions
- Cross-service integrations
- Unclear requirements

**Skip for:**
- Simple bug fixes
- Typo corrections
- Config changes

---

**Reference:** `.claude/skills/brainstorming-methodology.md`
**Target Time:** 20-40 minutes (saves hours of rework later)
