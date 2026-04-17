# Pilot PR 1: Add Phone Number to Student - Design Decision

**Date:** 2026-03-13
**Complexity:** Low
**Skill Applied:** Socratic Brainstorming

---

## Step 1: Question Assumptions

### Problem Definition

**Q: What problem are we solving?**
A: Students need a contact phone number field for emergency contact and class communication

**Q: Why is this important NOW?**
A: Testing Superpowers Socratic Brainstorming skill on a simple, well-defined feature

**Q: Who is the primary user?**
A: Admin (creates students with phone), Teacher (views student contact info)

### Success Criteria

**Q: How do we know we succeeded?**
- Phone number stored in database ✓
- Validation enforces correct format ✓
- API returns phone in StudentResponse ✓
- Tests verify all scenarios ✓

**Q: What does "done" look like?**
- MVP: Add nullable phone_number field with optional validation
- Full feature: Not needed for pilot

### Constraints

**Q: What are the constraints?**
- Time: Target 30-45 min total (pilot test)
- Format: International phone format support (E.164)
- Validation: Optional field (can be null)

---

## Step 2: Explore Trade-offs

### Option A: String field with @Pattern validation ⭐ RECOMMENDED

**Pros:**
- ✅ Simple implementation (1 field, 1 annotation)
- ✅ Flexible format (supports international)
- ✅ Standard JPA/Jakarta validation
- ✅ Easy to test

**Cons:**
- ⚠️ No structure (just string storage)
- ⚠️ Manual format validation regex

**Use Case:** Best for MVP, simple contact storage

**Implementation:**
```java
@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "STUDENT_PHONE_INVALID")
@Column(name = "phone_number")
private String phoneNumber;
```

---

### Option B: Separate country code + number fields

**Pros:**
- ✅ Structured data (code separate from number)
- ✅ Better for international formatting
- ✅ Easier to query by country

**Cons:**
- ❌ Over-engineering for simple field
- ❌ More complex (2 fields, 2 validations)
- ❌ More migration columns

**Use Case:** Large-scale international system with country-specific logic

---

### Option C: Use Embedded PhoneNumber value object

**Pros:**
- ✅ Domain-driven design (proper value object)
- ✅ Encapsulation of validation logic
- ✅ Reusable across entities

**Cons:**
- ❌ Too complex for pilot test
- ❌ Requires custom Hibernate UserType
- ❌ More boilerplate code

**Use Case:** Enterprise system with complex phone logic

---

## Trade-off Matrix

| Criterion | Weight | Option A (String) | Option B (Split) | Option C (Value Object) |
|-----------|--------|-------------------|------------------|-------------------------|
| **Simplicity** | 40% | 5 (200) | 3 (120) | 2 (80) |
| **Development Time** | 30% | 5 (150) | 3 (90) | 2 (60) |
| **Testability** | 20% | 5 (100) | 4 (80) | 3 (60) |
| **Extensibility** | 10% | 3 (30) | 4 (40) | 5 (50) |
| **TOTAL** | 100% | **480** ⭐ | **330** | **250** |

**Decision:** Option A (String with @Pattern)

---

## Step 3: Document Decision

### Chosen Approach: String field with @Pattern validation

**Summary:**
Add `phone_number VARCHAR(20)` column to students table with optional `@Pattern` validation for E.164 format

**Rationale:**
- Simplest implementation for pilot test
- Supports international formats via E.164 regex
- Standard Jakarta Bean Validation (no custom code)
- Easy to implement and test in 30-45 min

---

### Rejected Alternatives

**1. Separate country code + number fields**
- Why considered: Better structure for international
- Why rejected: Over-engineering for simple contact field

**2. Embedded PhoneNumber value object**
- Why considered: Clean domain design
- Why rejected: Too complex for 30-min pilot test

---

### Trade-offs Accepted

**What we're giving up:**
- Structured data (country code separate) - acceptable for MVP
- Advanced phone formatting features - not needed now

**What we're gaining:**
- Fast implementation (30-45 min)
- Simple validation with standard annotations
- Easy to test and maintain

---

### Success Criteria

**Must have:**
- [x] Phone number field added to Student entity
- [x] Nullable (optional field)
- [x] Validation regex for E.164 format
- [x] Database migration (add column)
- [x] DTO updated (CreateStudentRequest, StudentResponse)
- [x] Tests cover valid/invalid/null scenarios

**Nice to have:**
- [ ] Phone number formatting helper (future)
- [ ] SMS integration (future)

---

### Implementation Notes

**Affected Files:**
- Entity: `Student.java` (+1 field with @Pattern)
- DTOs: `CreateStudentRequest.java`, `UpdateStudentRequest.java`, `StudentResponse.java`
- Migration: `V15__add_student_phone_number.sql`
- Tests: `StudentServiceTest.java` (+3 tests for phone validation)

**Database Change:**
```sql
ALTER TABLE students ADD COLUMN phone_number VARCHAR(20);
```

**Validation Pattern:**
```
^\\+?[1-9]\\d{1,14}$
- Optional + prefix
- Starts with 1-9
- Followed by 1-14 digits
- Total max 15 digits (E.164 standard)
```

---

### Review Date

**When to revisit:** When SMS/WhatsApp integration needed (future)
**Why:** May need structured country code for SMS gateway

---

**Decision Status:** ✅ Approved - Ready for TDD implementation
**Time Spent on Brainstorming:** 15 minutes
**Skills Applied:** Socratic Brainstorming ✅
