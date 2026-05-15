# GAP-555: Wave 78 config keys documented nhưng không wired vào code (4 domain)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (kitehub-subscription)
**Found:** 2026-05-14 (Business Logic audit post-Wave-78 — Cat 2.1 P0 FAIL)
**Affects:** kitehub/feedback + kitehub/onboarding + kitehub/beta-status + kitehub/support (rules.md ↔ application.yml drift)

## Problem

Wave 78 ship 4 business domain mới với rules.md liệt kê **15+ config keys** prefix `kitehub.{feedback,onboarding,beta-status,support}.*`. Audit phát hiện chỉ **1 key thật sự wired** vào code (`kitehub.feedback.survey-cron` trong `FeedbackSurveyScheduler.java:65`); còn lại đều hardcoded qua Bean Validation hoặc Java constants.

Vi phạm CLAUDE.md §"Business Logic Documents 3-Layer": "KHÔNG hardcode business rules — luôn dùng config key từ rules.md".

**Evidence (per `business-logic-audit/SKILL.md` Cat 2.1):**

```bash
$ grep -rnE "kitehub\.(onboarding|feedback|beta-status|support)" kitehub/kitehub-subscription/src --include="*.java" --include="*.yml"
# Returns only 3 hits — all for `kitehub.feedback.survey-*` (Bucket F scheduler).
```

Specific drifts:

| BR-xxx | Documented config key (rules.md) | Actual code | File:line |
|--------|----------------------------------|-------------|-----------|
| BR-FEEDBACK-001 | `kitehub.feedback.rating-range-min/max=1/5` | Hardcoded `@Min(1) @Max(5)` | `FeedbackSubmissionRequest.java:25-26` |
| BR-FEEDBACK-003 | `kitehub.feedback.comment-min-chars/max-chars=5/2000` | Hardcoded `@Size(min=5, max=2000)` | `FeedbackSubmissionRequest.java:30` |
| BR-FEEDBACK-001 | `kitehub.feedback.categories=BUG,USABILITY,FEATURE_REQUEST,GENERAL` | Không tìm thấy enum binding | — |
| BR-ONBOARD-001 | `kitehub.onboarding.step-ids=PROFILE_SETUP,...` | Hardcoded enum `OnboardingStepId` | `OnboardingStepId.java` |
| BR-BETA-STATUS-002 | `kitehub.beta-status.cache-ttl-seconds=300` | Không có `@CacheControl` hay @Value binding | `BetaStatusService.java` |
| BR-BETA-STATUS-001 | `kitehub.beta-status.content-source=classpath:beta-status/beta-status.md` | Path hardcoded inline | `BetaStatusService.java:35` |
| BR-SUPPORT-001/002 | 10 `kitehub.support.*` keys | **0 BE code** (xem GAP-556) | — |

## Root Cause

Wave 78 Bucket 0 ship rules.md trước (foundation), buckets B/F sau ship code. Khi implement code, agent dùng Bean Validation hardcoded values thay vì wire `@Value("${kitehub.feedback.comment-max-chars:2000}")`. Pattern này tiết kiệm thời gian short-term nhưng tạo silent drift risk: nếu Product Owner update rules.md (vd: tăng comment max lên 5000), code không reflect → user vẫn bị reject 2000.

## Proposed Fix

Per domain:

1. **feedback** (Bucket F):
   - Define `@ConfigurationProperties("kitehub.feedback")` class với fields: ratingMin/Max, commentMinChars/MaxChars, categories
   - Refactor `FeedbackSubmissionRequest` validation từ hardcoded annotations sang dynamic (custom validator dùng config)
   - Add `application.yml` block với defaults match rules.md

2. **onboarding** (Bucket B):
   - Keep `OnboardingStepId` enum (whitelist không nên configurable runtime — security)
   - Update rules.md BR-ONBOARD-001 Code reference + add note: "Step IDs là enum compile-time, không phải config"
   - Add `kitehub.onboarding.put-rate-limit-per-min` wiring nếu cần expose (hoặc remove khỏi rules.md)

3. **beta-status** (Bucket B):
   - `@Value("${kitehub.beta-status.content-source:classpath:beta-status/beta-status.md}")` ở `BetaStatusService`
   - `@Value("${kitehub.beta-status.cache-ttl-seconds:300}")` + apply `Cache-Control` header trong Controller
   - Wire `kitehub.beta-status.rate-limit-per-min-per-ip` qua gateway route config

4. **support**: covered by GAP-556 (BE not implemented yet)

## Acceptance Criteria

- [ ] Mỗi documented config key trong 4 rules.md (Wave 78 domain) có matching `@Value` hoặc `@ConfigurationProperties` binding trong code
- [ ] `grep -rnE "kitehub\.(feedback|onboarding|beta-status)" kitehub/kitehub-subscription/src --include="*.java"` returns ≥10 hits (vs 3 hiện tại)
- [ ] `application.yml` có block defaults cho mỗi prefix; values match rules.md
- [ ] Unit test cho 1 boundary case mỗi domain (vd: feedback comment 2001 chars → 400; beta-status cache header present)
- [ ] Business logic audit Cat 2.1 P0 sub-check PASS

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-14-post-wave-78.md`
- Skill: `.claude/skills/quality/business-logic-audit/SKILL.md`
- Rubric: `.claude/rules/audit-skill-rubric-business-logic-audit.md` §2.2 Cat 2 Config Accuracy
- Rules referenced: BR-FEEDBACK-001/003, BR-ONBOARD-001, BR-BETA-STATUS-001/002
- Sister gap: GAP-556 (support BE absent), GAP-557 (UC ↔ BR traceability)

## Log

- **2026-05-14:** DONE — Wave 79 Bucket A closure. 22 @Value annotations wiring 15+ config keys across 4 domains (auth + onboarding + feedback + beta-status). Cat 2.1 P0 cleared (PR #1365).
