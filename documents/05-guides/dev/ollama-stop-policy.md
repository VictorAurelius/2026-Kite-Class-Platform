# Ollama Stop Policy — Local Dev Workflow (GAP-409)

**Wave:** 37 (Layer 4 Local dev resource)
**Status:** Active 2026-05-07
**Related:** GAP-407 (Compose profiles), GAP-416 (Ollama defer Phase 2 ADR), `ai-branding-guidelines.md` §1

## Bối cảnh

Ollama 9B model (Gemma 4 9B / Llama 3.1 8B) chiếm 6-12 GB RAM khi chạy. Trên máy 27 GB local, để Ollama bật mặc định = mất ~25-45 % RAM trước khi mở bất kỳ service nào.

Phase 1 BETA quyết định **defer Ollama self-host sang Phase 2** (xem ADR-026 — Bucket E GAP-416). Phase 1 chạy template-only path; agent chỉ cần Ollama khi **iterate AI Branding workflow** trong dev.

## Khi nào chạy Ollama

| Use case | Bật Ollama? | Profile |
|---|---|---|
| Test luồng signup / billing / admin | ❌ | `beta-funnel` |
| Chỉnh template SVG branding (no AI) | ❌ | `branding-only-no-ai` |
| Iterate AI Branding generation (analyzer/planner/executor) | ✅ | `branding-only` (kèm `ai-local`) |
| KC class/grade/attendance work | ❌ | `kc-only` |
| Demo full stack | Tùy demo | `full` (default no Ollama) hoặc `full,ai-local` |

## Bật / tắt thủ công

```bash
# Bật Ollama (kèm download model lần đầu)
cd kitehub && ./scripts/up.sh --profile branding-only

# Tắt khi xong session
docker compose -f kitehub/docker-compose.kitehub.yml stop kite-ollama kite-ollama-setup

# Tắt + xóa container (vẫn giữ model trên volume kite-ollama-models)
docker compose -f kitehub/docker-compose.kitehub.yml rm -fs kite-ollama kite-ollama-setup
```

Volume `kite-ollama-models` được giữ giữa các lần restart — model chỉ tải lại nếu xóa volume thủ công.

## Cloud API fallback (Phase 1 mặc định)

Khi không chạy Ollama local, Phase 1 dùng cloud LLM cho AI Branding qua biến môi trường:

```bash
# .env (kitehub/) hoặc shell
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini

# hoặc Anthropic:
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
ANTHROPIC_MODEL=claude-haiku-4-5
```

Code đã ship Strategy pattern (`AIClient`) — swap provider qua config, không cần đổi code. Tham khảo `kitehub-branding/src/main/resources/application-dev.yml` cho key listing.

## Cost ceiling cho dev OpenAI key

- Hard cap **$5 / tháng** trên dev account
- Set qua AWS / OpenAI dashboard usage limit
- Dev nào đụng cap → swap sang `AI_PROVIDER=anthropic` (Anthropic miễn phí $5 monthly credit cho free tier dev)
- Production cost monitoring tracked riêng — xem `documents/05-guides/deploy/aws-cost-monitoring.md` (Bucket E GAP-413)

## Auto-stop after idle (optional)

Để tránh quên tắt sau session, có thể chạy supervisor script:

```bash
# Ví dụ supervisor (chưa ship — placeholder cho follow-up gap)
# Stop Ollama nếu không có request 30 phút
while true; do
  LAST=$(docker logs --tail 1 --timestamps kite-ollama 2>&1 | awk '{print $1}')
  IDLE_MIN=$(( ($(date +%s) - $(date -d "$LAST" +%s)) / 60 ))
  if [ "$IDLE_MIN" -gt 30 ]; then
    docker stop kite-ollama && break
  fi
  sleep 300
done
```

Hiện chưa wire vào dev workflow — manual stop là chuẩn Phase 1.

## Trade-off ngắn

| | Ollama local | Cloud API |
|---|---|---|
| RAM | 6-12 GB | 0 (chỉ network) |
| Latency p50 | ~3-8s (CPU only WSL2) | ~500ms-2s |
| Cost | $0 (electricity) | ~$0.001-0.01 / request |
| Internet required | No | Yes |
| Repro test | Stable | Tùy provider rate limit |

Phase 1 default = cloud (faster iteration, không tốn RAM). Phase 2 evaluate self-host khi traffic justify (xem ADR-026).
