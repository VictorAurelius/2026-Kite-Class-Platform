# GAP-045: Template Marketplace (Community Contributions)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (future)
**Domain:** Product / Community
**Detected:** 2026-04-14 (simulation: cross-cutting × C9)

## Problem

Template curation (GAP-011) hiện tại chỉ là **in-house**:
- 30 templates initial + 10/quarter by 1 designer
- Không leverage community (designers, power users)
- Slow growth vs user demand
- Không có revenue model cho template creators

Reference: Figma Community, Canva Creator Program, Webflow Showcase.

## Proposed Fix (Long-term)

### 1. Contribution Platform

```
/marketplace
├── Browse templates (by category, style, rating)
├── Submit template
│   ├── Upload SVG + metadata
│   ├── Preview rendering
│   ├── Submit for review
├── My templates (dashboard)
│   ├── Draft / Submitted / Approved / Rejected
│   ├── Usage stats
│   └── Earnings
└── Reviewer dashboard (admin)
```

### 2. Contributor Roles

| Role | Privileges |
|------|-----------|
| Public | Browse, use free templates |
| Contributor | Submit templates |
| Verified Contributor | Priority review, higher revenue share |
| Official Partner | Featured, exclusive templates |

### 3. Review Process

Tái sử dụng GAP-011 5 review criteria:
1. Brand-agnostic
2. Accessibility
3. Responsive
4. Text safety
5. Brand family consistency

+ additional:
6. Originality (not copy of existing)
7. Quality (aesthetic judgment by reviewer)
8. IP check (no copyrighted elements)

Review SLA: 5-10 business days.

### 4. Revenue Model

**Option A: Revenue share per usage**
- Template used → contributor earns X% of AI generation fee
- KiteClass platform takes 70%, contributor 30%

**Option B: Free templates + premium**
- Free templates: no revenue
- Premium templates: contributor earns
- Tenant subscribes tier to unlock premium

**Option C: One-time purchase**
- Marketplace sells templates individually
- Contributor earns from each sale

**Recommend: Hybrid A+B** for ecosystem growth.

### 5. Quality Gatekeeping

- Unverified contributors: 1 template per month
- Verified: 5 per month
- Auto-deprecate templates với low rating (< 3 stars)
- Anti-spam: detect template similarity (plagiarism)

### 6. Legal Framework

- CLA (Contributor License Agreement) required
- Contributor grants KiteClass license to use + sublicense
- Contributor indemnifies against IP claims
- Clear termination rights

### 7. Incentives

- Badge system (verified, top contributor)
- Featured templates on landing page
- Revenue transparency dashboard
- Annual awards

## Launch Strategy

**Phase 0** (Year 1 after GA):
- Keep in-house only
- Learn usage patterns
- Build analytics (GAP-022)

**Phase 1** (Invite-only beta):
- Invite 5-10 known designers
- Stress-test review process
- Refine revenue model

**Phase 2** (Public beta):
- Open submissions
- Community forum
- Featured creator program

**Phase 3** (GA):
- Full marketplace
- Discovery algorithms
- Integration với Figma, Canva?

## Acceptance Criteria

Since P2 + long-term:
- [ ] Business case validated (sufficient interest)
- [ ] Legal framework drafted (CLA, ToS)
- [ ] MVP marketplace UI (submit, review, browse)
- [ ] Review process (reuse GAP-011 + new criteria)
- [ ] Revenue infrastructure (tracking, payouts)
- [ ] 10+ community contributors onboarded (Phase 1)
- [ ] Quality metrics tracked

## Dependencies

- GAP-011 (in-house templates) — foundation
- GAP-022 (template analytics) — discovery insights
- GAP-023 (admin moderation) — review infrastructure
- GAP-042 (legal/IP) — CLA framework

## Log

- 2026-04-14 — Future ecosystem opportunity
