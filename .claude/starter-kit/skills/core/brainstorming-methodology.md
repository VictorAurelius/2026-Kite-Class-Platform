# Portable Skill — adapt {project} placeholders to your project

# Skill: Socratic Brainstorming

**Purpose:** Refine ideas through questions before coding, explore alternatives, document decisions.

---

## When to Use

**Mandatory:** New features (Medium+ complexity), architectural decisions, cross-service integrations, unclear requirements.

**Skip:** Simple bug fixes, typos, doc updates, config changes.

**When in doubt:** Spend 10 minutes brainstorming. Better to over-clarify than rework.

---

## 3-Step Process (20-40 min)

### Step 1: Question Assumptions (10 min)

Ask these before proposing solutions:

**Problem Definition:**
- What problem are we solving? (user pain, business need, tech debt?)
- Why is this important NOW?

**User Context:**
- Who is the primary user?
- What is their workflow? How often do they need this?
- What's their current workaround?

**Success Criteria:**
- How do we know we succeeded? (specific metrics)
- What does "done" look like? (MVP vs full feature)

**Constraints:**
- Performance? (latency, throughput)
- Data volume? Scale?
- Budget? Timeline?

### Step 2: Explore Trade-offs (15 min)

Compare multiple approaches systematically:

```markdown
| Criterion       | Option A | Option B | Option C |
|-----------------|----------|----------|----------|
| Performance     |          |          |          |
| Scalability     |          |          |          |
| Complexity      |          |          |          |
| Maintainability |          |          |          |
| Dev Cost        |          |          |          |
| Infra Cost      |          |          |          |
```

**Optional scoring:** Assign weights (e.g., Performance 30%), score 1-5, calculate weighted total.

### Step 3: Document Decisions (10 min)

Record WHY you chose this approach:

```markdown
## Design Decision: [Feature Name]
**Date:** YYYY-MM-DD

### Chosen Approach: [Option Name]
**Summary:** [1-2 sentences]
**Rationale:** [Key reasons]

### Rejected Alternatives
- [Alternative]: Why rejected

### Trade-offs Accepted
- What we're giving up (and why acceptable)
- What we're gaining

### Success Criteria
- [ ] [Criterion with metric]

### Risks & Mitigation
- Risk: [What could go wrong] -> Mitigation: [How to handle]

### Review Date
**When to revisit:** [Date or trigger condition]
```

---

## Quick Reference Checklist

Before coding, verify:

- [ ] Did I question assumptions? (what/why/who/success criteria)
- [ ] Did I explore >=2 alternatives? (trade-off matrix)
- [ ] Did I document the decision with rationale?
- [ ] Did I record WHY alternatives were rejected?
- [ ] Did I define success criteria?

**If rushed:** 10 min questioning + document chosen approach with 1-line rationale.

---

## Success Metrics

- Time spent per session: 20-40 min (not >1 hour)
- Alternatives explored: >=2 options
- Design decision documented: 100%
- Requirements changes mid-PR: <20%
