# GAP-409: Ollama Stop Policy + Cloud API Fallback (Dev)

**Status:** 🟢 DONE 2026-05-07 (Wave 37 Bucket D PR pending)
**Priority:** 🟡 P2
**Domain:** DevOps / AI / Local dev
**Found:** 2026-05-07 (Wave 37 — Layer 4)
**Affects:** Local 27GB RAM — Ollama 9B chiếm 6-12 GB

## Problem

Ollama 9B model (Gemma 4 / Llama 3.1 8B) tốn 6-12 GB RAM. Khi không test AI Branding, Ollama vẫn chạy (Docker auto-restart hoặc systemd service) → lãng phí RAM.

## Proposed Fix

1. Document policy `documents/05-guides/dev/ollama-stop-policy.md`:
   - WHEN start: chỉ khi test AI Branding feature
   - WHEN stop: mọi case khác (`docker stop ollama` hoặc `ollama stop`)
   - Auto-stop after idle 30 min (Docker healthcheck + supervisor script)
2. Cloud API fallback config: `application-dev.yml` `ai.provider=openai` option (env var swap)
3. Cost ceiling: dev OpenAI key cap $5/tháng ngân sách (alarming)

**LƯU Ý:** Per quyết định 2026-05-07 user-confirmed, Ollama deferred Phase 2 — Phase 1 BETA template-only. Gap này covers DEV WORKFLOW khi developer test AI iteration locally.

## Acceptance Criteria

- [x] `documents/05-guides/dev/ollama-stop-policy.md` shipped — bối cảnh + when-to-run matrix + manual start/stop + cloud fallback config + $5/mo cap + auto-stop placeholder + trade-off
- [x] Cloud fallback documented — `AI_PROVIDER=openai|anthropic` env var swap path (Strategy via `AIClient`); reference `application-dev.yml`
- [x] OpenAI dev key cost cap $5/mo documented + Anthropic $5 monthly credit fallback
- [x] Auto-stop idle-30min — placeholder bash script in doc (manual stop là Phase 1 standard, auto-stop opt-in)

## Log

- **2026-05-07 (Wave 37 Bucket D):** Shipped. Cross-link với Bucket E ADR-026 (Ollama defer Phase 2) — Phase 1 default = cloud, Ollama opt-in qua `--profile branding-only` (GAP-407). Auto-stop script là placeholder; full implementation tùy nhu cầu.

## Related

- GAP-407 (compose profile `branding-only-no-ai`)
- GAP-416 Ollama defer Phase 2 ADR
- `ai-branding-guidelines.md` §1 STATIC/TEMPLATE/FULL_AI taxonomy
