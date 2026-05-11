# GAP-105: Parent-Portal Domain Missing 3-Layer Business Docs

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (meta — 3-layer structure contract broken)
**Domain:** KiteClass / Parent Portal / Business Docs
**Found:** 2026-04-19 (business-logic audit)
**Resolved:** 2026-04-19 (PR pending — see Resolution section)
**Affects:** kiteclass-core `parent/` module, Wave 5 parent dashboard readiness, audit traceability

## Problem

Wave 2 PR #337 (GAP-052a) đã ship `ParentPortalProperties.java` với javadoc rõ ràng:

```java
// ParentPortalProperties.java:16-18
// @param invitationTtlHours   token lifetime — default 24 h per BR-PARENT-003.
```

Rule ID `BR-PARENT-003` được REFERENCE trong code, nhưng:

```
$ ls documents/01-business/kiteclass/ | grep -i parent
# (nothing)
$ grep -r "BR-PARENT" documents/01-business/
# 0 hits
```

3-layer structure của project (`CLAUDE.md §Business Logic Documents — 3-Layer Structure`) yêu cầu: mỗi domain = 1 folder với `rules.md` + `use-cases.md` + `api-contract.md`. Parent-portal domain không có folder → contract broken.

Scope code đã ship (PR #337) mà không có rules doc:
- `Parent`, `ParentInvitation`, `ParentStudentLink` entities
- Rules implicit trong code: invitation TTL 24h, tenant isolation, feature flag `kiteclass.parent-portal.enabled`, expire sweep 3600000ms, max children per parent, redemption flow
- Endpoints: `POST /api/v1/parent-invitations`, `POST /api/v1/parent-invitations/redeem/{token}`, `GET /api/v1/parent/me`, `GET /api/v1/parent/me/children`, `GET /internal/parents/{id}`

## Root Cause

GAP-052 meta-plan note: "Wave 2 ships identity + invitation; Wave 5 completes it". Assumption: docs shipping với Wave 5 khi feature "complete". Nhưng GAP-052 comment Wave 2 PR #337 đã ship production code — giả định này vi phạm Living Docs rule (doc và code PHẢI cùng PR).

Meta impact: BR-PARENT-003 là "ghost rule" — không ai biết chính xác rule này nói gì. Reviewer tương lai không thể verify invitation TTL=24h đúng với quy định, test coverage không map lại rule ID, Wave 5 khi landing sẽ phải reverse-engineer.

## Proposed Fix

Tạo `documents/01-business/kiteclass/parent-portal/` với 3 files:

### rules.md (draft BR-PARENT-* list — cần stakeholder validation)
- BR-PARENT-001: Parent entity unique per tenant (instanceId + phoneOrEmail)
- BR-PARENT-002: 1 parent có thể link N children (ParentStudentLink)
- BR-PARENT-003: Invitation token TTL 24 hours (`kiteclass.parent-portal.invitation-ttl-hours`)
- BR-PARENT-004: Feature flag `kiteclass.parent-portal.enabled` mặc định false (Wave 5 flip true)
- BR-PARENT-005: Redeem base URL config `kiteclass.parent-portal.redeem-base-url`
- BR-PARENT-006: Expire sweep hourly (`kiteclass.parent-portal.expire-sweep-ms=3600000`)
- BR-PARENT-007: Redemption idempotent (same token → same result)
- BR-PARENT-008: Tenant isolation — parent chỉ thấy children trong cùng tenant
- BR-PARENT-009: Invitation status: PENDING → REDEEMED / EXPIRED / REVOKED

### use-cases.md
- UC-PARENT-01: Teacher invite parent cho student
- UC-PARENT-02: Parent redeem invitation → create Parent account
- UC-PARENT-03: Parent xem list children
- UC-PARENT-04: Hourly sweep expire pending invitations
- UC-PARENT-05: Internal gateway JWT enrichment

### api-contract.md
- Endpoints đã liệt kê ở trên + request/response DTOs (ParentResponse, ChildSummaryResponse, etc.)

## Acceptance Criteria
- [ ] Folder `documents/01-business/kiteclass/parent-portal/` tồn tại với 3 files
- [ ] `BR-PARENT-003` trong rules.md match javadoc reference trong `ParentPortalProperties.java`
- [ ] 9 rules cover scope đã ship PR #337 (không forward-looking Wave 5)
- [ ] Use-cases match controller endpoints (5 UC ↔ 5 endpoints)
- [ ] api-contract.md liệt kê tất cả endpoints với request/response samples
- [ ] Pre-commit hook `scripts/verify-business-docs.sh` pass
- [ ] Wave 5 planning doc reference BR-PARENT-* cho feature expansion

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Original feature PR: #337 (Wave 2 parent portal identity + invitation MVP)
- Original gap: GAP-052 (still IN_PROGRESS per ROADMAP — Wave 2 ship identity, Wave 5 complete)
- CLAUDE.md §Business Logic Documents — 3-Layer Structure (contract)
- Rules: `.claude/rules/meta-gap-priority.md` (meta-boost), `.claude/rules/audit-to-gap-pipeline.md`

## Resolution

**2026-04-19** — Branch: `feature/partb-gap105-parent-portal-docs` (PR pending push)

Created `documents/01-business/kiteclass/parent-portal/` với 3 files:
- `rules.md` — 9 BR-PARENT + 4 BR-PARENT-LINK + 10 BR-PARENT-INV + 3 BR-PARENT-PWD + 4 BR-PARENT-AUTH + config keys + DB schema (extracted from `ParentPortalProperties`, `ParentInvitationServiceImpl`, entities, enums, `application.yml:222-228`)
- `use-cases.md` — 6 UCs (UC-PARENT-01 → UC-PARENT-06) covering invite, redeem, /me, /me/children, sweep, internal lookup
- `api-contract.md` — 5 endpoints (4 public + 1 internal) với request/response schemas + 10 error codes

**BR-PARENT-003 verified:** rule giờ explicit nói "Token TTL mặc định 24h, configurable qua `kiteclass.parent-portal.invitation-ttl-hours`"; matches javadoc reference ở `ParentPortalProperties.java:15`.

Index `documents/01-business/README.md` updated: KiteClass count 12 → 13 domains (39 files).

**Out-of-scope (per task brief):** không modify ROADMAP, không implement features mới, không touch code behavior.
