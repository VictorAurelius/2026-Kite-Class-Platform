# PR Dependency Graph (v2.0 - With PR 0 Foundation)

**Version:** 2.0
**Created:** 2026-02-27
**Purpose:** Visualize PR dependencies với PR 0 Database Foundation làm prerequisite

---

## 📊 Critical Path

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PR DEPENDENCY GRAPH V2.0                                 │
│                  (With Database Foundation Prerequisite)                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────┐     │
│  │                    PR 0: DATABASE FOUNDATION                       │     │
│  │                         (PREREQUISITE)                             │     │
│  ├────────────────────────────────────────────────────────────────────┤     │
│  │  • PR 0.1: Gateway V1 (6 tables, 5 roles, 30+ permissions)         │     │
│  │  • PR 0.2: Core V1 (40+ business tables, seed data)                │     │
│  │  • Dependencies: NONE                                              │     │
│  │  • Blocks: ALL feature PRs (1.1+, 2.1+, 3.1+)                      │     │
│  └────────────────────────────────────────────────────────────────────┘     │
│                                 │                                            │
│                                 │ Blocks ALL                                 │
│                                 ▼                                            │
│  ┌───────────────────┬──────────────────────┬─────────────────────┐         │
│  │   GATEWAY PRs     │     CORE PRs         │    FRONTEND PRs     │         │
│  │   (1.1 - 1.8)     │   (2.1 - 2.15)       │    (3.1 - 3.14)     │         │
│  └───────────────────┴──────────────────────┴─────────────────────┘         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔗 Dependency Matrix

### Tier 0: Foundation (BLOCKING ALL)

| PR | Status | Depends On | Blocks | Risk Level |
|----|--------|------------|--------|------------|
| **PR 0** | ⏳ NOT STARTED | NONE | ALL (1.1+, 2.1+, 3.1+) | **CRITICAL** |

**Rationale:**
- V1 migrations create ALL core tables upfront
- Feature PRs chỉ cần ALTER TABLE (add columns) → simpler
- Parallel development (no migration conflicts)
- Complete schema reference ngay từ đầu

---

### Tier 1: Gateway Infrastructure (Depends on PR 0)

| PR | Status | Depends On | Blocks | Risk Level |
|----|--------|------------|--------|------------|
| PR 1.1 | ✅ DONE | PR 0 | PR 1.2+ | Low |
| PR 1.2 | ✅ DONE | PR 0, PR 1.1 | PR 1.3+ | Low |
| PR 1.3 | ✅ DONE | PR 0, PR 1.2 | PR 1.4+ | Medium |
| PR 1.4 | ✅ DONE | PR 0, PR 1.3 | PR 1.5+ | Medium |
| PR 1.5 | ✅ DONE | PR 0, PR 1.4 | PR 1.6+ | Low |
| PR 1.6 | ✅ DONE | PR 0, PR 1.5 | PR 1.7+ | Low |
| PR 1.7 | ✅ DONE | PR 0, PR 1.6 | PR 1.8 | Medium |
| PR 1.8 | ⏳ TODO | PR 0, PR 1.7, PR 2.11 | PR 2.6+, PR 3.1+ | **HIGH** |

**Critical PR: 1.8 (Cross-Service Integration)**
- Implements UserType + ReferenceId pattern
- Blocks Core modules that need Gateway integration
- Blocks Frontend (requires user linking)

---

### Tier 2: Core Infrastructure (Depends on PR 0)

| PR | Status | Depends On | Blocks | Risk Level |
|----|--------|------------|--------|------------|
| PR 2.1 | ✅ DONE | PR 0 | PR 2.2+ | Low |
| PR 2.2 | ✅ DONE | PR 0, PR 2.1 | PR 2.3+ | Low |
| PR 2.3 | ✅ DONE | PR 0, PR 2.2 | PR 2.6+, PR 2.7+ | Medium |
| PR 2.3.1 | ✅ DONE | PR 0, PR 2.2 | PR 2.4+, PR 2.7.1 | Medium |
| PR 2.4 | ✅ DONE | PR 0, PR 2.3.1 | PR 2.5+ | Medium |
| PR 2.5 | ✅ DONE | PR 0, PR 2.4 | PR 2.6+, PR 2.7 | Medium |
| PR 2.11 | ✅ DONE | PR 0, PR 2.3 | PR 1.8 | Medium |
| PR 2.12 | ✅ DONE | PR 0 | - | Low |

---

### Tier 3: Core Business Logic (High Risk)

| PR | Status | Depends On | Blocks | Risk Level |
|----|--------|------------|--------|------------|
| **PR 2.10.1** | ⏳ TODO | **PR 0** | PR 2.7.1, PR 3.10, PR 3.12 | **CRITICAL** |
| PR 2.6 | ⏳ TODO | PR 0, PR 2.3, PR 2.5 | PR 2.7, PR 2.8 | Medium |
| PR 2.7 | ⏳ TODO | PR 0, PR 2.5, PR 2.6 | PR 2.7.2 | Medium |
| **PR 2.7.1** | ⏳ TODO | PR 0, PR 2.5, **PR 2.10.1** | PR 2.7.2 | **HIGH** |
| PR 2.7.2 | ⏳ TODO | PR 0, PR 2.7, PR 2.7.1 | - | Medium |
| **PR 2.8** | ⏳ TODO | PR 0, PR 2.6 | PR 2.8.1 | **HIGH** |
| **PR 2.8.1** | ⏳ TODO | PR 0, PR 2.8 | - | **HIGH** |
| PR 2.9 | ⏳ TODO | PR 0, PR 2.10.1 | - | Medium |
| PR 2.10 | ⏳ TODO | PR 0, ALL Core PRs | - | Low |

**Critical PRs:**
- **PR 2.10.1 (File Storage):** MUST implement EARLY, blocks Assignment (2.7.1), Profile upload (3.10), Guest Pages (3.12)
- **PR 2.7.1 (Assignment):** Depends on File Storage, complex late penalty logic
- **PR 2.8 (Invoice):** Financial calculations, VietQR integration
- **PR 2.8.1 (Payment):** Payment gateway integration, transaction idempotency

---

### Tier 4: Frontend (Depends on Gateway + Core)

| PR | Status | Depends On | Blocks | Risk Level |
|----|--------|------------|--------|------------|
| PR 3.1 | ✅ DONE | PR 0, PR 1.8 | PR 3.2+ | Low |
| PR 3.2 | ⏳ TODO | PR 0, PR 3.1, PR 2.3 | PR 3.3+ | Low |
| PR 3.3 | ⏳ TODO | PR 0, PR 3.2, PR 2.3.1 | PR 3.4+ | Low |
| PR 3.4 | ⏳ TODO | PR 0, PR 3.3, PR 2.4 | PR 3.5+ | Low |
| PR 3.5 | ⏳ TODO | PR 0, PR 3.4, PR 2.5 | PR 3.6+ | Low |
| PR 3.6 | ⏳ TODO | PR 0, PR 3.5, PR 2.6 | PR 3.7+ | Medium |
| PR 3.7 | ⏳ TODO | PR 0, PR 3.6, PR 2.7 | PR 3.8+ | Medium |
| PR 3.8 | ⏳ TODO | PR 0, PR 3.7, PR 2.7.1 | - | Medium |
| PR 3.9 | ⏳ TODO | PR 0, PR 3.8, PR 2.8 | - | Medium |
| **PR 3.10** | ⏳ TODO | PR 0, PR 3.9, **PR 2.10.1** | - | **HIGH** |
| PR 3.11 | ⏳ TODO | PR 0, PR 3.10, PR 2.9 | - | Low |
| **PR 3.12** | ⏳ TODO | PR 0, PR 3.11, **PR 2.10.1** | - | **HIGH** |
| PR 3.13 | ⏳ TODO | PR 0, PR 3.12, PR 2.13 | - | Medium |
| PR 3.14 | ⏳ TODO | PR 0, PR 3.13, PR 2.14 | - | Medium |

**High Risk PRs:**
- **PR 3.10 (Profile Upload):** Direct S3 upload, CORS configuration
- **PR 3.12 (Guest Pages Upload):** Hero images, teacher photos, presigned URLs

---

## 🚀 Execution Recommendation

### Phase 0: Foundation (Week 1)
**Goal:** Tạo complete database schema upfront

```
PR 0: Database Foundation
├─ PR 0.1: Gateway V1 (6 tables, seed data)
└─ PR 0.2: Core V1 (40+ tables, seed data)

Estimated: 3-5 days
Risk: HIGH (blocks everything)
Mitigation: Extensive local testing, staging deployment first
```

---

### Phase 1: Infrastructure (Week 2-4)
**Goal:** Gateway + Core setup + Early File Storage

```
PARALLEL:
├─ Gateway: PR 1.1 → 1.7 (✅ Done)
├─ Core: PR 2.1 → 2.5 (✅ Done)
└─ CRITICAL: PR 2.10.1 (File Storage) ⚠️ MUST DO EARLY

Why PR 2.10.1 Early?
- Blocks Assignment Module (PR 2.7.1)
- Blocks Profile Upload (PR 3.10)
- Blocks Guest Pages (PR 3.12)
- S3 setup takes time (MinIO Docker, AWS config)
- Complex testing (Testcontainers, CORS, presigned URLs)

Estimated: 3 weeks
```

---

### Phase 2: Core Modules (Week 5-10)
**Goal:** Complete business logic modules

```
SEQUENTIAL (dependency-driven):
1. PR 2.6: Enrollment (depends on Student, Class)
2. PR 2.7: Attendance (depends on Enrollment)
3. PR 2.7.1: Assignment (depends on File Storage ✅, Class)
4. PR 2.7.2: Grade (depends on Assignment, Attendance)
5. PR 2.8: Invoice (depends on Enrollment)
6. PR 2.8.1: Payment (depends on Invoice)
7. PR 2.9: Settings (depends on File Storage ✅)
8. PR 2.10: Docker & Integration (final)

Estimated: 6 weeks
High Risk PRs: 2.7.1, 2.8, 2.8.1 (need extra testing)
```

---

### Phase 3: Learning Modules (Week 11-14)
**Goal:** LMS, Marketing, Trial features

```
SEQUENTIAL:
1. PR 2.13: LMS Module (Modules, Lessons, Progress)
2. PR 2.14: Marketing Module (Landing Pages, Leads)
3. PR 2.15: Trial User Support (Magic Links, Quotas)

Estimated: 4 weeks
```

---

### Phase 4: Frontend (Week 15-20)
**Goal:** User-facing UI

```
SEQUENTIAL (paired with backend):
1. PR 3.1: Infrastructure (✅ Done)
2. PR 3.2-3.9: Core UI (Students, Teachers, Classes, Attendance, Assignments, Invoices)
3. PR 3.10: Profile Upload (depends on PR 2.10.1 ✅)
4. PR 3.11: Settings UI
5. PR 3.12: Guest Pages (depends on PR 2.10.1 ✅)
6. PR 3.13-3.14: Trial Learning UI

Estimated: 6 weeks
High Risk PRs: 3.10, 3.12 (file uploads, CORS, presigned URLs)
```

---

## ⚠️ Risk Mitigation Strategy

### Critical Path Items

**PR 0 (Database Foundation):**
- **Risk:** Migration timeout, schema mismatch
- **Mitigation:** Split into 2 files (Gateway, Core), benchmark locally, test on staging first
- **Rollback:** Keep migration scripts reversible, test rollback procedure

**PR 2.10.1 (File Storage):**
- **Risk:** S3 timeout, CORS issues, quota race conditions
- **Mitigation:** Implement EARLY, comprehensive Testcontainers tests, MinIO local testing
- **Rollback:** Soft delete với 30-day grace period, manual recovery endpoint

**PR 1.8 (Cross-Service Integration):**
- **Risk:** Gateway-Core communication failure, event loss
- **Mitigation:** Retry logic, circuit breaker, compensation transactions
- **Rollback:** Feature flag để disable cross-service calls

**PR 2.8 + 2.8.1 (Invoice + Payment):**
- **Risk:** Financial calculation errors, payment reconciliation mismatch
- **Mitigation:** Double-entry validation, automated reconciliation, extensive unit tests
- **Rollback:** Manual reversal procedures, transaction audit logs

---

## 📈 Progress Tracking

### Completed (✅)
- **Gateway:** 8/8 PRs (100%) - PR 1.1 to 1.7, PR 1.12
- **Core:** 8/15 PRs (53%) - PR 2.1 to 2.5, PR 2.11, PR 2.12
- **Frontend:** 1/14 PRs (7%) - PR 3.1

### In Progress (⏳)
- **PR 0:** Database Foundation (prerequisite)

### Blocked (🚫)
- **ALL feature PRs** blocked by PR 0
- **PR 2.7.1** additionally blocked by PR 2.10.1
- **PR 3.10, 3.12** additionally blocked by PR 2.10.1

### Total Progress
- **17/40 PRs completed (42.5%)**
- **23/40 PRs remaining (57.5%)**
- **Estimated completion:** 20 weeks from PR 0 completion

---

## 🎯 Next Steps (Recommended Order)

1. **Week 1:** PR 0 (Database Foundation) - CRITICAL
2. **Week 2:** PR 2.10.1 (File Storage) - EARLY IMPLEMENTATION
3. **Week 3-4:** PR 1.8 (Cross-Service Integration) - UNBLOCKS FRONTEND
4. **Week 5-10:** Core Business Logic (PR 2.6 → 2.9) - HIGH RISK MODULES
5. **Week 11-14:** Learning Modules (PR 2.13 → 2.15) - V4.1 FEATURES
6. **Week 15-20:** Frontend UI (PR 3.2 → 3.14) - USER EXPERIENCE

---

**Document Version:** 2.0
**Last Updated:** 2026-02-27
**Next Review:** After PR 0 completion
