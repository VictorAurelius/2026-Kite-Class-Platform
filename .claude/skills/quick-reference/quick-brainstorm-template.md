# Quick Brainstorm Template

**Quick Reference** - 5-Minute Decision Process for Simple Features

---

## When to Use This Template

**Use Quick Brainstorm (5 min) for:**
- ✅ Features estimated <30 minutes
- ✅ Low complexity (adding field, simple validation, minor refactor)
- ✅ Clear requirements (no ambiguity)
- ✅ 2 obvious options to compare

**Use Full Socratic Brainstorming (20-40 min) for:**
- ⚠️ Features >30 minutes
- ⚠️ Medium+ complexity
- ⚠️ Unclear requirements (need exploration)
- ⚠️ Multiple options with complex trade-offs

---

## 5-Minute Template

### Problem Statement (1 min)

**What:** [One sentence - what are we building?]

**Why:** [One sentence - why now?]

**Success:** [One sentence - how do we know we succeeded?]

---

### Option Comparison (2 min)

**Option A: [Approach 1]**
- **Pros:** [Top 2 benefits]
- **Cons:** [Top 2 drawbacks]
- **Time:** [Estimated implementation time]

**Option B: [Approach 2]**
- **Pros:** [Top 2 benefits]
- **Cons:** [Top 2 drawbacks]
- **Time:** [Estimated implementation time]

---

### Decision (2 min)

**Chosen:** Option [A/B]

**Rationale:** [1-2 sentences explaining why]

**Trade-off Accepted:** [What we're giving up and why it's OK]

**Review Date:** [When to revisit - e.g., "If X happens" or "After 3 months"]

---

## Example 1: Add Phone Field to Student

```markdown
### Problem Statement

**What:** Add phone_number field to Student entity
**Why:** Need contact info for emergency communication
**Success:** Phone stored, validated (E.164 format), API returns phone

---

### Option Comparison

**Option A: String field with @Pattern**
- **Pros:** Simple (1 field, 1 annotation), Standard validation
- **Cons:** No structure (just string)
- **Time:** 30 min

**Option B: Embedded PhoneNumber object**
- **Pros:** Clean domain design, Reusable value object
- **Cons:** More complex (UserType, more code)
- **Time:** 2 hours

---

### Decision

**Chosen:** Option A (String with @Pattern)

**Rationale:** Simple contact field doesn't need complex object structure.
Standard @Pattern validation sufficient for MVP.

**Trade-off Accepted:** No structured data (country code separate) -
acceptable for simple contact storage. Can refactor to value object later if needed.

**Review Date:** When SMS integration needed (requires country code parsing)
```

**Time Spent:** 5 minutes ✅

---

## Example 2: Add Pagination to List Endpoint

```markdown
### Problem Statement

**What:** Add pagination to GET /api/students endpoint
**Why:** Performance degrades with >100 students
**Success:** Endpoint returns paginated results, controllable via query params

---

### Option Comparison

**Option A: Spring Data Pageable (default)**
- **Pros:** Built-in support, Standard pattern, No custom code
- **Cons:** Exposes internal structure (page/size/sort)
- **Time:** 15 min

**Option B: Custom PaginationRequest DTO**
- **Pros:** API contract flexibility, Can customize parameter names
- **Cons:** Duplicate code (reinvent Pageable), More maintenance
- **Time:** 45 min

---

### Decision

**Chosen:** Option A (Spring Data Pageable)

**Rationale:** Standard Spring pagination pattern widely understood.
No need to customize for this use case.

**Trade-off Accepted:** Exposes "page/size/sort" parameter names -
acceptable as industry standard. Most APIs use these names.

**Review Date:** If API contract needs to differ from backend pattern
```

**Time Spent:** 4 minutes ✅

---

## Example 3: Error Handling Strategy

```markdown
### Problem Statement

**What:** Standardize error responses across all endpoints
**Why:** Inconsistent error formats confuse frontend developers
**Success:** All errors return consistent JSON structure with error codes

---

### Option Comparison

**Option A: @ControllerAdvice with custom ErrorResponse**
- **Pros:** Centralized handling, Consistent format, Easy to maintain
- **Cons:** Global behavior (harder to customize per endpoint)
- **Time:** 45 min

**Option B: Manual try-catch in each controller**
- **Pros:** Full control per endpoint, Easy to customize
- **Cons:** Duplicate code, Inconsistent (developer forgets), Hard to maintain
- **Time:** 2 hours (across all controllers)

---

### Decision

**Chosen:** Option A (@ControllerAdvice)

**Rationale:** Consistency across all endpoints more important than per-endpoint customization.
Centralized approach reduces code duplication.

**Trade-off Accepted:** Less flexibility for custom error handling per endpoint -
acceptable as 95% of errors follow same pattern.

**Review Date:** If specific endpoint needs completely different error format
```

**Time Spent:** 5 minutes ✅

---

## Checklist

Quick decision is valid if:
- [ ] Problem clearly stated (1 sentence)
- [ ] 2 options compared (not just 1)
- [ ] Decision has rationale (not just "I prefer A")
- [ ] Trade-off explicitly acknowledged
- [ ] Review date specified (know when to revisit)

---

## When Quick Brainstorm is NOT Enough

**Red flags that need full Socratic process:**
- ❌ "I'm not sure which approach is better" → Need trade-off matrix scoring
- ❌ "This affects multiple services" → Need architecture review
- ❌ "Requirements are unclear" → Need problem exploration (Step 1 of full process)
- ❌ "We have 3+ viable options" → Need systematic comparison
- ❌ "Decision is hard to reverse" → Need thorough analysis

**Escalate to full brainstorming if:**
- Complexity is Medium or High
- Multiple stakeholders involved
- Significant performance/security implications
- Cross-service integration needed

---

## Time Comparison

| Feature Complexity | Quick Brainstorm | Full Socratic | Saved Time |
|-------------------|------------------|---------------|------------|
| Simple (<30 min) | 5 min | 20 min | **-75%** |
| Medium (30-120 min) | Not recommended | 30 min | N/A |
| High (>120 min) | Not recommended | 40 min | N/A |

**ROI:** For simple features, Quick Brainstorm saves 15 min while still preventing wrong decisions

---

**Reference:** `.claude/skills/brainstorming-methodology.md` (full version)
**When to Use:** Features <30 min, Low complexity, 2 clear options
**Time Target:** 5 minutes or less
