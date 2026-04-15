# GAP-073: GDPR / Data Deletion Policy for AI-Generated + AI-Trained Assets

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (legal exposure)
**Domain:** Compliance / AI Branding / Legal
**Detected:** 2026-04-14 (simulation-gap-finder on Wave 3 scope)
**Matrix cell:** Owner × Termination × C6 Compliance

## Problem

Khi tenant delete account:

1. **AI-generated assets** (logos, banners, hero images) — lưu đâu, xóa khi nào, có backup tồn tại sau delete không? GAP-024 (asset lifecycle cleanup) scope chung, không cover compliance angle.
2. **Uploaded logos + brand inputs** — tenant uploaded logo có thể là trademarked corporate logo. Delete account → phải remove completely.
3. **AI prompts + derived embeddings** — AI provider có cache prompts / outputs? Fine-tuned models có derived tenant data?
4. **Moderation decisions referencing tenant content** — phải retain cho audit hay delete?

Pháp lý Việt Nam + GDPR (tenant EU users):
- Right to erasure
- Data residency (VN Decree 53/2022 yêu cầu data trong biên giới VN)
- Audit retention (invoices 10 năm per tax law)

Mâu thuẫn: delete vs retain. Cần explicit policy.

## Evidence

- GAP-024 Asset Lifecycle & Storage Cleanup — scope ops hygiene, không phải legal compliance
- GAP-018 Content Safety — focus moderation, không phải deletion
- Không có `DataDeletionRequest` workflow / DPO contact / retention schedule
- Settings → DangerZone chỉ "Delete account" — không explain data policy

## Proposed Fix

### 1. Data Retention Classification

| Category | Retain | Delete on account termination | Pseudonymize |
|----------|:------:|:-----------------------------:|:------------:|
| AI-generated assets (logos, banners) | 30d soft-delete, then purge | ✅ | — |
| Uploaded originals (logo, brand guide PDFs) | 30d soft-delete, then purge | ✅ | — |
| Prompts sent to AI provider | Do NOT send PII; provider must honor no-retention flag | N/A | — |
| Branding history / versions | 90d soft-delete, then purge | ✅ | — |
| Financial invoices | 10 years (VN tax law) | ❌ (retain) | ✅ PII pseudonymized |
| Moderation decisions | 5 years (legal evidence) | ❌ (retain) | ✅ tenant ID → hash |
| Admin audit logs | 2 years | ❌ (retain) | ✅ |
| Quality gate reports | 1 year | soft-delete after | ✅ |

### 2. DeletionRequest workflow

```
User clicks "Delete account" → DeletionRequest(PENDING)
  → 7-day grace period (reversible)
  → Day 7: DeletionRequest(PROCESSING)
    → Disable login
    → Soft-delete assets (mark for purge)
    → Pseudonymize retained records (invoice, audit)
    → Notify AI providers to purge prompts (OpenAI data-deletion API / Ollama is local OK)
  → Day 37: hard-delete soft-deleted assets (purge MinIO, Redis)
  → DeletionRequest(COMPLETED) + tombstone record (for future restore ban)
```

### 3. Data export before delete (GDPR Art. 20)

Before PROCESSING, offer "Download my data" → ZIP:
- All branding assets
- Branding history JSON
- Invoice history CSV
- User list CSV
- Ships to user's email as signed download link (24h)

Ties to GAP-034 (Branding Export Pack) — extend scope.

### 4. DPO + Privacy Policy page

- Public route `/privacy-policy` (updated)
- DPO contact email on DangerZone
- Explicit mention: data residency VN, what goes to AI providers, retention schedule

### 5. AI provider contract clauses

- Ollama (local) — no data leaves instance ✅ by default
- OpenAI — turn on `x-openai-no-training-data: true`, SOC 2 confirmation
- Anthropic / Bedrock — enterprise data agreement

Document these in `.claude/rules/ai-branding-guidelines.md` §9 extension.

## Acceptance Criteria

- [ ] Retention classification table published in `01-business/kiteclass/data-retention/`
- [ ] `DeletionRequest` entity + 7-day grace workflow
- [ ] Data export endpoint (ZIP download)
- [ ] Pseudonymization script for retained records
- [ ] AI provider no-retention flags configured
- [ ] DPO contact on DangerZone + Privacy Policy page
- [ ] Runbook: how to handle GDPR erasure request
- [ ] E2E: delete flow + grace period + purge + export

## Dependencies

- GAP-024 (asset lifecycle) — deletion trigger
- GAP-034 (export pack) — ZIP format
- GAP-042 (legal / IP) — related scope
- GAP-018 (content safety) — moderation retention policy

## Target Wave

**Wave 4 Security & Compliance** (Sprint 2-3, parallel với Wave 3).

Legal exposure → cannot defer past GA launch.

## Log

- 2026-04-14 — Detected via simulation-gap-finder (termination stage, GDPR + VN data residency gap)
