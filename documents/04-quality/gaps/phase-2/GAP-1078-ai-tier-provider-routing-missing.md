---
id: GAP-1078
title: AI provider global (no tier→model routing) — FREE tier hits paid OpenAI thay vì free Ollama
status: OPEN
priority: P2
phase: phase-2
domain: Backend
created: 2026-06-09
last_verified: 2026-06-09
affects: kitehub-branding (ResilientAIClient / AIProviderConfig)
---

# GAP-1078 — AI tier→provider routing thiếu (cost leak khi bật AI thật)

## Problem

`kitehub-branding` AI generation dùng **1 provider GLOBAL** (`ai.provider`), KHÔNG route theo tier. Phát hiện khi điều tra AI-branding cho wave landing-100 (2026-06-09).

- `ResilientAIClient` (`@Primary`) → mọi call AI (text `generateText` + image `generateImage` + `analyzeLogo`) đi qua 1 bean, delegate tới provider duy nhất set bởi `ai.provider` (`AIProviderConfig.aiClient()`).
- Tier CHỈ ảnh hưởng: input token cap (`AIInputCapConfig` GAP-258) + regenerate limit (FREE 3 / PRO 10...). **KHÔNG ảnh hưởng model/provider.**
- Verify (stack local 2026-06-09): `AI_PROVIDER=openai` + `OPENAI_API_KEY` set → mọi tier (kể cả FREE) hit **OpenAI paid** (`gpt-4-turbo` + `gpt-4-vision-preview`). Free route Ollama (`llama3.1:8b` + `llava:13b`, $0) tồn tại config nhưng container `kite-ollama` không chạy (GPU defer Phase 2 per ADR-026).

**Mismatch design↔impl:** ADR-037 lý luận cost theo "free-tier LLM cho text (FREE) + GPT cho banner" — hàm ý tier-differentiated. Nhưng impl là single global provider → khi bật AI thật, FREE tenant trigger AI text-gen sẽ tốn OpenAI paid thay vì Ollama free.

**Chưa cắn ở Phase 1 BETA** vì: template-first (ADR-026) + wizard FULL_AI = phase-2 PARTIAL (GAP-272) → FREE tenant Phase 1 không chạy AI. Sẽ cắn khi AI wizard go-live.

## Proposed Fix

Tier→provider/model routing trong `AIProviderConfig` / `ResilientAIClient` (hoặc 1 `AIProviderRouter`):
- FREE / TRIAL → Ollama local (`llama3.1:8b`) — $0
- PREMIUM / ENTERPRISE → OpenAI (`gpt-4-turbo`) hoặc Ollama tuỳ cost policy
- Fallback chain giữ nguyên (circuit breaker → template defaults).
- Cân nhắc: banner = GPT (ADR-037) chỉ cho paid; FREE = template-composer (GAP-810).

## Acceptance Criteria

- [ ] AI call route provider/model theo tier (FREE→free model, PAID→OpenAI) — không global cứng
- [ ] FREE tenant KHÔNG hit OpenAI paid cho text-gen
- [ ] Cost policy documented (which tier → which provider/model) trong ADR hoặc rules
- [ ] Test: mock FREE tier → Ollama path; PREMIUM → OpenAI path

## Related

- Discovered in: điều tra AI-branding cho wave landing-100 (session 2026-06-09)
- ADR-037 (AI generation stack — free-tier LLM intent) · ADR-026 (Ollama defer Phase 2)
- [[GAP-272]] (AI wizard v2 → production, phase-2) · [[GAP-1021]] (AI assets persist → active theme)
- `ai-branding-guidelines.md` §1 (STATIC/TEMPLATE/FULL_AI) §2.5 (input cap GAP-258 — tier-aware nhưng chỉ cap, không provider)
- NGOÀI scope wave landing-100 (landing đọc Branding entity, decoupled khỏi AI provider)
