# GAP-042: Legal / IP Protection (Trademark, DMCA, Copyright)

**Status:** 🟢 DONE (Wave 4 Sub-PR 4.3, merged 2026-04-14; DmcaStatus state machine + DmcaService workflow + TrademarkCheckService scaffold + PublicDmcaController + legal/dmca page. Asset revert + counter-notice email deferred.)
**Priority:** 🔴 P0 (legal exposure)
**Domain:** Legal / Compliance
**Detected:** 2026-04-14 (simulation: cross-cutting × C6)

## Problem

AI branding có legal/IP risks **không được handle**:

- ❌ Trademark conflict: tenant đặt tên "McDonald's Tutoring" → legal issue
- ❌ AI image có thể output nội dung copyright (trained on copyrighted data)
- ❌ Logo upload có thể vi phạm copyright người khác
- ❌ Không có DMCA takedown process
- ❌ Không có IP claim response workflow
- ❌ Tenant tải logo của công ty khác → claim?

## Proposed Fix

### 1. Trademark Screening

Khi tenant đặt tên/tagline:
```java
public void screenTrademark(String name) {
  // Option A: Manual review queue (safer, slower)
  // Option B: Automated fuzzy match với trademark DB (USPTO API, WIPO)
  // Option C: Warning + user affirms ownership

  var conflicts = trademarkDb.fuzzyMatch(name, threshold: 0.8);
  if (!conflicts.isEmpty()) {
    prompt("Tên này tương tự với trademark đã đăng ký: X.
            Bạn có chắc có quyền sử dụng?");
  }
}
```

### 2. AI Output Copyright Check

```java
public class AIOutputIPChecker {
  public boolean check(GeneratedImage img) {
    // Reverse image search — Google Images API
    var matches = reverseImageSearch(img);
    if (matches.any(m -> m.similarity > 0.9 && m.source == COPYRIGHTED)) {
      regenerate(img, different_seed);
      logPotentialIPIssue(img);
    }
  }
}
```

### 3. Logo Upload Copyright Declaration

```tsx
<UploadStep>
  <FileInput label="Upload logo" />
  <Checkbox required>
    Tôi xác nhận:
    - Tôi sở hữu quyền sử dụng logo này
    - Logo không vi phạm trademark/copyright bên thứ 3
    - Tôi chịu trách nhiệm với hậu quả pháp lý nếu sai
  </Checkbox>
</UploadStep>
```

### 4. DMCA Takedown Process

Endpoint: `POST /legal/dmca-notice`

```json
{
  "claimant": { "name", "email", "address" },
  "copyrightedWork": "description",
  "infringingContent": { "tenantId", "assetUrl" },
  "goodFaithStatement": "...",
  "perjuryStatement": "...",
  "signature": "..."
}
```

Workflow:
1. Receive notice → create ticket
2. Verify format valid
3. Notify tenant, provide 10 days response
4. If no counter-notice → take down
5. If counter-notice → may republish after 10-14 business days
6. Audit trail maintained

Team: designated DMCA agent (required by law).

### 5. Terms of Service Requirements

Tenant must accept:
```
User Agreement
- I own rights to all uploaded content
- I indemnify KiteClass against IP claims
- I agree to respond to takedown notices within 10 days
- Repeat infringers may be terminated (per DMCA §512(i))
```

### 6. Repeat Infringer Policy

Track tenants với IP claims:
- 1st claim: warning
- 2nd claim within 12 months: suspension
- 3rd claim: termination (DMCA requirement)

### 7. AI Model IP Compliance

- Document which models used (license check)
- Gemma 4: Apache 2.0 ✓
- Stable Diffusion: CreativeML Open RAIL-M (derivative works OK)
- OpenAI: commercial use allowed but outputs not copyrightable

### 8. Tenant-sourced Template Review

Nếu tenant contribute template (GAP-045):
- Review cho IP compliance
- CLA (Contributor License Agreement) required

## Acceptance Criteria

- [ ] Trademark fuzzy matching (warning, not blocking)
- [ ] Logo upload copyright declaration
- [ ] DMCA notice endpoint + workflow
- [ ] DMCA agent designated + public contact
- [ ] Terms of Service updated với IP clauses
- [ ] Repeat infringer tracking + enforcement
- [ ] AI model license compliance documented
- [ ] Legal counsel sign-off

## Dependencies

- Legal counsel (external)
- GAP-018 (content safety) — share infrastructure
- GAP-023 (admin moderation) — enforcement UI

## Log

- 2026-04-14 — Legal exposure gap
