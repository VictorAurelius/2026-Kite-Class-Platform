# GAP-002: Async pipeline cho heavy AI tasks

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** AI / Backend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/03-planning/implementation/ai-local-implementation-plan.md`
- `documents/01-business/kitehub/ai-branding/rules.md`

## Problem

AI calls hiện tại là **synchronous HTTP** tới Ollama API. Image generation với Stable Diffusion mất 2-5 phút/ảnh trên CPU (hoặc 30s trên GPU). Nếu user submit request → FE blocking 2-5 phút → timeout hoặc UX kém.

## Context

- RabbitMQ đã có sẵn trong shared infra (`kite-rabbitmq`)
- Hiện RabbitMQ được dùng cho: email events, tenant provisioning
- **Chưa được dùng cho AI async processing**
- `OllamaClient.java` call sync, block thread

## Evidence

- `ai-local-implementation-plan.md` line 177-179: "Cần GPU hoặc chấp nhận chậm (~2-5 phút/ảnh trên CPU)"
- Không có mention async/queue trong AI design docs
- PR-AI-2 (Image generation) pending, chưa giải quyết UX latency

## Proposed Fix

**PR-AI-5: Async AI Pipeline**

```
User submits request
    ↓
AIController.generateImage() → validate + rate limit
    ↓
Publish message vào queue `ai.image.generation`
    ↓ (returns immediately with jobId)
FE receives jobId → show "Processing..." + progress bar
    ↓
AIWorker (listener) processes job:
    - Call Ollama/SD API (blocking OK)
    - Upload result tới MinIO
    - Update AIJob status = COMPLETED
    - Publish event `ai.image.completed`
    ↓
FE nhận notification qua:
    Option A: Polling GET /api/v1/ai/jobs/{jobId}
    Option B: SSE (Server-Sent Events)
    Option C: WebSocket
```

**Components cần tạo:**
- `AIJob` entity (id, userId, type, status, inputParams, resultUrl, error, createdAt, completedAt)
- `AIJobController` — POST /generate (returns jobId), GET /jobs/{id}
- `AIJobListener` (RabbitMQ consumer) — process jobs từ queue
- `ai.image.generation` queue config
- Status tracking: PENDING → PROCESSING → COMPLETED/FAILED

## Acceptance Criteria

- [ ] FE submit AI request → nhận jobId trong <200ms
- [ ] Heavy AI task chạy async, không block FE
- [ ] FE poll/subscribe được status updates
- [ ] Retry logic cho failed jobs
- [ ] Rate limit vẫn hoạt động (tier-based)
- [ ] Job history stored trong DB để audit

## Log

- 2026-04-14 — Phát hiện khi review AI design; blocker cho image generation UX
