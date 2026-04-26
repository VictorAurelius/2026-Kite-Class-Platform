# GAP-018: Content Safety & Compliance for AI Branding

**Status:** 🟢 DONE (Wave 4 Sub-PR 4.1, merged 2026-04-14; ModerationStatus state machine + ContentModerationService 3-stage pipeline + AuditLog integration. Real ML classifier + admin review queue deferred.)
**Priority:** 🔴 P0 (legal/compliance risk)
**Domain:** Security / Compliance / AI
**Detected:** 2026-04-14 (simulation)

## Problem

Hiện tại **KHÔNG có** content safety hay compliance mechanism cho AI branding:

- ❌ Logo upload: không content moderation (NSFW, violence, copyright)
- ❌ AI-generated images: không safety filter (có thể output inappropriate)
- ❌ Không audit log của AI generations (who requested what, when)
- ❌ GDPR: user uploaded logo = personal data → không có deletion workflow
- ❌ Không terms of service / acceptable use policy

**Risk:** Legal liability nghiêm trọng cho SaaS platform.

## Evidence

- Grep "moderation", "nsfw", "safety", "filter" trong kitehub-branding/src → không có
- `LogoAnalysis.java` chỉ extract colors, không check content
- Không có `ContentSafetyService` hay tương đương
- Không có audit log table cho AI requests

## Proposed Fix

### 1. Upload Content Moderation

```java
@Service
public class UploadModerationService {
  public ModerationResult moderate(MultipartFile file) {
    // Option A: Local ML (NSFWJS or similar)
    // Option B: AWS Rekognition Content Moderation API
    // Option C: Google Vision Safe Search

    ModerationResult result = nsfwDetector.detect(file);
    if (result.nsfwScore > 0.7) return REJECT;
    if (result.violenceScore > 0.7) return REJECT;
    return APPROVE;
  }
}
```

### 2. AI Output Safety Filter

```java
@Component
public class AIOutputSafetyChecker {
  public SafetyResult check(GeneratedImage img) {
    // Run generated images through same moderation
    // If fail: regenerate with safer prompt, or mark FAILED
  }
}
```

### 3. Audit Log

```java
@Entity
public class AIGenerationAuditLog {
  Long id;
  String tenantId;
  String userId;
  AIFeatureType type;
  String inputSummary;   // hash or redacted summary
  String outputUrl;
  Integer moderationScore;
  Timestamp createdAt;
  String ipAddress;
}
```

- Retention: 2 years (legal compliance)
- Searchable by admin for moderation/compliance queries

### 4. GDPR Deletion Workflow

```java
public void deleteTenantAIData(String tenantId) {
  // 1. Delete all uploaded logos
  // 2. Delete all AI-generated assets
  // 3. Delete audit logs (or anonymize per GDPR)
  // 4. Log deletion with timestamp
}
```

### 5. Terms & Conditions

- Tenant phải accept ToS khi sign up
- Acceptable Use Policy cho AI features
- Disclaimer: AI output is user's responsibility

## Acceptance Criteria

- [ ] Upload moderation service integrated
- [ ] AI output safety checker implemented
- [ ] AIGenerationAuditLog entity + retention policy
- [ ] GDPR deletion workflow
- [ ] Terms of Service + AUP published
- [ ] Admin moderation UI (link with GAP-023)
- [ ] Penetration test: upload inappropriate images → rejected
- [ ] Legal review sign-off

## Dependencies

- Integrates with GAP-023 (admin moderation tools)
- Legal counsel consultation needed

## Log

- **2026-04-26** — **Governance closure tracked: [GAP-225](GAP-225-scaffolded-as-done-governance-closure-umbrella.md)** (Scaffolded-as-DONE Governance Closure Umbrella) + [GAP-223](GAP-223-ai-branding-migration-verification-governance.md) Sub-PR 223.1 scope. Scaffold debt: "Real ML classifier + admin review queue deferred". Status preserved 🟢 DONE for audit trail; this gap is part of Cluster C3 (AI Branding Quality Gates, paired with GAP-012) — covered by GAP-223 Option C plan (skill `quality/ai-branding-quality-gate/` + audit-gate rule + matrix line 75 sync). `output-review-mandate.md` line 75 synced this PR from "PLANNED" → "PARTIAL" reflecting actual scaffold state. No code change this PR — docs truth-up only.
- 2026-04-14 — P0 legal risk identified qua simulation
