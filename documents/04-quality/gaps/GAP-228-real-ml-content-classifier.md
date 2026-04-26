# GAP-228: Real ML Content Classifier (replace scaffold)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 Feature (Quality / Content safety) — Wave 8+ scope
**Domain:** Backend / Content Safety / ML inference
**Found:** 2026-04-26 (Sub-PR 223.1 baseline audit + GAP-018 explicit deferred "Real ML classifier + admin review queue deferred")
**Affects:** Every AI Branding output (logo, banner, hero copy) — current ContentModerationService 3-stage pipeline runs scaffolded checks (regex blocklist + length); real ML classifier (toxicity, NSFW, brand-safety) deferred

## Problem

`ContentModerationService` (GAP-018 Wave 4 Sub-PR 4.1) implements 3-stage pipeline + state machine + AuditLog integration but stages 1-3 are placeholder logic:
- **Stage 1 (text safety):** regex blocklist only (catches profanity but misses contextual issues — sarcasm, dog whistles, brand-conflict wording)
- **Stage 2 (image safety):** filename heuristic only (no NSFW/violence/copyright detection)
- **Stage 3 (brand-safety):** length + sentiment-keyword scoring (misses cultural-fit, off-brand tone)

Risk: AI generates marketing copy với phrasing không phù hợp văn hoá VN (vd. "lớn nhất", "duy nhất" vi phạm Quảng Cáo law Art 8) → tenant publishes → legal exposure.

## Current State (verified 2026-04-26)

- `kitehub-branding/src/main/java/.../moderation/ContentModerationService.java` — pipeline + state machine + AuditLog integration intact
- 3 stages return scaffold scores; no ML model integration
- `ModerationStatus` state machine (PENDING → REVIEWING → APPROVED/REJECTED/MANUAL_REVIEW) works correctly
- Admin review queue UI not implemented — manual review path goes to AuditLog only

## Proposed Fix

### Stage 1 — Real text safety
- Integrate Vietnamese-tuned toxicity classifier (candidate: PhoBERT fine-tuned on `vi_civil_comments` dataset, or HuggingFace `unitary/multilingual-toxic-xlm-roberta`)
- Run via Ollama local inference or dedicated Python sidecar
- Threshold: toxicity score >0.7 = REJECT, 0.4-0.7 = MANUAL_REVIEW, <0.4 = APPROVE

### Stage 2 — Real image safety
- NSFW/violence: HuggingFace `Falconsai/nsfw_image_detection` or AWS Rekognition (per provider tier)
- Brand-safety: copyright/logo detection — defer to Phase 2 (out of scope this gap)
- Threshold: NSFW prob >0.3 = REJECT, 0.1-0.3 = MANUAL_REVIEW

### Stage 3 — Brand-safety + VN cultural-fit
- Custom LLM check via prompt engineering on existing AI provider:
  ```
  "Đánh giá đoạn marketing copy sau theo 4 tiêu chí (0-10): cultural-fit VN, professional tone, brand-safe wording, advertising-law compliance (Art 8 Quảng Cáo - không superlative tuyệt đối). Trả về JSON {scores: {...}, overall_pass: bool}."
  ```
- Threshold: overall_pass=false hoặc bất kỳ score <6 → MANUAL_REVIEW

### Admin review queue UI
- KiteHub admin dashboard route `/admin/moderation/queue` listing MANUAL_REVIEW items
- Approve/reject buttons → updates `ModerationStatus`
- Notification (Slack/email) on new MANUAL_REVIEW

## Acceptance Criteria

- [ ] Stage 1 toxicity classifier integrated; 100 sample VN texts (50 toxic + 50 clean) → ≥85% accuracy
- [ ] Stage 2 NSFW detection integrated; 50 sample images (25 safe + 25 NSFW) → ≥90% accuracy
- [ ] Stage 3 LLM brand-safety check returns parseable JSON 100% of time across 50 samples
- [ ] Admin review queue UI lists MANUAL_REVIEW items with approve/reject + sends notification
- [ ] Baseline audit re-run after merge → §3 score moves from 8/20 toward ≥16/20

## Dependencies

- **Tracked under:** GAP-225 (umbrella) cluster C3, GAP-223 (governance scaffolding done Sub-PR 223.1)
- **Blocked by:** Ollama capacity for additional model OR Python sidecar setup; admin dashboard auth + routing infra
- **Aligned with:** GAP-018 (deferred items per gap header explicit)
- **Legal alignment:** Luật Quảng Cáo VN Art 8 (no superlative without proof) — checklist source

## References

- `ai-branding-guidelines.md` §5 (Quality Gate 5 checks), §9 (Security & Privacy)
- `ai-branding-quality-gate` skill §11.4.3
- GAP-018 (Content Safety scaffold landed Wave 4)
- HuggingFace classifiers: `unitary/multilingual-toxic-xlm-roberta`, `Falconsai/nsfw_image_detection`
- Luật Quảng Cáo Vietnam Art 8 (false advertising prohibition)

## Log

- **2026-04-26** — Filed as Sub-PR 223.1 follow-up. Real implementation deferred to Wave 8+ when ML inference infra (Ollama capacity or Python sidecar) + admin dashboard auth available.
