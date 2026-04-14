# Resource Classification — Use Cases

### UC-RES-01: Route a Resource Request
- **Actor:** Branding pipeline (system)
- **Input:** ResourceRequest(type, customRequested), ClassificationContext(hasStaticAsset, hasMatchingTemplate, hasAIQuota)
- **Steps:**
  1. Populate context from upstream lookups
  2. `ResourceRoutingService.classify()` walks ordered classifier chain
  3. First classifier returning a category wins
  4. DefaultTemplateClassifier terminal guarantees resolution
- **Result:** ResourceCategory (STATIC | TEMPLATE | FULL_AI)

### UC-RES-02: Static Asset Present
- **Precondition:** tenant previously uploaded asset for type
- **Result:** STATIC (even if customRequested=true)

### UC-RES-03: Custom AI Request with Quota
- **Precondition:** customRequested=true AND hasAIQuota
- **Result:** FULL_AI (skips template, as user explicitly wants custom)

### UC-RES-04: Template-First Default
- **Precondition:** customRequested=false AND hasMatchingTemplate
- **Result:** TEMPLATE (cheap, fast)

### UC-RES-05: No Template + AI Quota
- **Precondition:** no template AND hasAIQuota
- **Result:** FULL_AI (AIFallbackClassifier)

### UC-RES-06: Nothing Available
- **Precondition:** no static, no template, no quota
- **Result:** TEMPLATE (DefaultTemplateClassifier — generic template library)

### UC-RES-07: Persist Resolved Resource
- **Actor:** Branding pipeline (after generation)
- **Steps:**
  1. Construct BrandingResource(type, category, storageUrl, ...)
  2. `validateInvariants()` — throws IllegalStateException if category/FK mismatched
  3. Repository.save
- **Errors:** BR-RES-002..004 violations → IllegalStateException

## Log
- 2026-04-14 — Initial UCs
