# GAP-006: Upgrade AI models to Gemma 4

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (cost + quality improvement)
**Domain:** AI / Backend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/03-planning/implementation/ai-local-implementation-plan.md`

## Problem

Dự án hiện dùng `llama3.1:8b` (text) + `llava:13b` (vision) — 2 models riêng biệt, tổng ~24GB RAM. Gemma 4 (Google, ra mắt 2026-04-02) có multimodal native, performance cao hơn, RAM thấp hơn, license Apache 2.0. Nên cân nhắc migrate.

## Scan: AI Config hiện tại trong dự án

**Phạm vi ảnh hưởng — các file reference models:**

| File | Reference |
|------|-----------|
| `kitehub/docker-compose.kitehub.yml` | `OLLAMA_TEXT_MODEL:-llama3.1:8b`, `OLLAMA_VISION_MODEL:-llava:13b` |
| `kitehub/docker-compose.oracle-backend.yml` | Same defaults |
| `kitehub/kitehub-branding/src/main/resources/application.yml` | `text-model: llama3.1:8b`, `vision-model: llava:13b` |
| `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/config/AIProviderConfig.java` | Java defaults `llama3.1:8b`, `llava:13b` |
| `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/client/OllamaClient.java` | Javadoc examples |
| `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/dto/LogoAnalysis.java` | Javadoc |
| `kitehub/kitehub-branding/src/test/java/com/kitehub/branding/client/OllamaClientTest.java` | Test fixtures |
| `kitehub/kitehub-branding/src/test/java/com/kitehub/branding/config/AIProviderConfigTest.java` | Test fixtures |

**Tổng: 8 files** — tất cả trong `kitehub-branding` service + 2 docker-compose files.

**Scope KHÔNG bị ảnh hưởng:**
- KiteClass core, gateway, frontend — không dùng AI
- Các kitehub services khác (subscription, admin, email) — không dùng AI
- Infrastructure (postgres, redis, minio, rabbitmq) — không liên quan

## Comparison: Current Stack vs Gemma 4

### Current: Llama 3.1 + LLaVA

| Aspect | llama3.1:8b | llava:13b |
|--------|-------------|-----------|
| Params | 8B | 13B |
| Size | ~4.7GB | ~8GB |
| RAM | 8GB | 16GB |
| Context | 128K | 32K |
| Modality | Text only | Text + Image |
| Languages | Primary EN, limited VI | Primary EN |
| License | Llama Community License (có restriction) | Apache 2.0 |

**Tổng RAM:** ~24GB (load cả 2)

### Gemma 4 (April 2, 2026)

| Model | Params | RAM | Context | Modality | Use Case |
|-------|--------|-----|---------|----------|----------|
| **E2B** | Effective 2B | ~4GB | 128K | Text + Image + Video + Audio | Smartphone, Raspberry Pi |
| **E4B** | Effective 4B | ~8GB | 128K | Text + Image + Video + Audio | **Edge/consumer — BEST FIT** |
| **26B MoE** | 26B (sparse) | ~26GB | 256K | Text + Image | Consumer GPU |
| **31B Dense** | 31B | ~62GB | 256K | Text + Image | Data center |

**Đặc điểm Gemma 4:**
- ✅ Multimodal **native** (1 model thay cho 2)
- ✅ 140+ ngôn ngữ với cultural context (tiếng Việt tốt hơn)
- ✅ Apache 2.0 license (thoải mái commercial)
- ✅ Configurable "thinking modes" cho reasoning tasks
- ✅ Day-one Ollama support
- ✅ 31B beats Llama 4 400B trên benchmarks:
  - AIME 2026 Math: 89.2% vs 88.3%
  - LiveCodeBench v6: 80.0% vs 77.1%
  - GPQA Diamond: 84.3% vs 82.3%

## Comparison Matrix

| Criterion | llama3.1:8b + llava:13b | Gemma 4 E4B | Gemma 4 26B MoE |
|-----------|--------------------------|-------------|-----------------|
| RAM cần | ~24GB (cả 2 models) | ~8GB | ~26GB |
| Multimodal | 2 models separate | 1 model native | 1 model native |
| Cold start | Load 2 models | Load 1 model | Load 1 model |
| Latency text | Baseline | ~Same (smaller) | Slower but smarter |
| Latency vision | llava slow | Native faster | Native + smartest |
| Vietnamese | Limited | Native 140+ langs | Native 140+ langs |
| Context | 128K / 32K | 128K | 256K |
| License | Llama restricted | Apache 2.0 ✅ | Apache 2.0 ✅ |
| Oracle Cloud (24GB ARM) | Marginal fit | Plenty of headroom | Just fits |

## Proposed Fix

### Recommended: **Gemma 4 E4B** (single model replacement)

**Lý do:**
- Thay thế CẢ llama3.1:8b VÀ llava:13b bằng 1 model
- RAM: 24GB → 8GB (save 16GB → scale được 3x concurrent workers)
- Native multimodal → đơn giản hóa code (bỏ dual-client)
- Cultural VI context → kết quả tốt hơn cho tenant Vietnam
- Apache 2.0 → không license risk

**Migration scope:**

```diff
# docker-compose.kitehub.yml, docker-compose.oracle-backend.yml
- OLLAMA_TEXT_MODEL: ${OLLAMA_TEXT_MODEL:-llama3.1:8b}
- OLLAMA_VISION_MODEL: ${OLLAMA_VISION_MODEL:-llava:13b}
+ OLLAMA_TEXT_MODEL: ${OLLAMA_TEXT_MODEL:-gemma4:e4b}
+ OLLAMA_VISION_MODEL: ${OLLAMA_VISION_MODEL:-gemma4:e4b}
+ # Gemma 4 E4B is multimodal native — same model for text + vision
```

```diff
# kitehub-branding/src/main/resources/application.yml
  ollama:
-   text-model: ${OLLAMA_TEXT_MODEL:llama3.1:8b}
-   vision-model: ${OLLAMA_VISION_MODEL:llava:13b}
+   text-model: ${OLLAMA_TEXT_MODEL:gemma4:e4b}
+   vision-model: ${OLLAMA_VISION_MODEL:gemma4:e4b}
```

### Optional: Gemma 4 26B MoE for Production

Nếu có GPU Oracle Cloud (paid tier) hoặc AWS g4dn:
- Better quality cho premium/enterprise users (GAP-005 dedicated workers)
- 256K context cho long documents
- Route via tier: Free/Pro → E4B, Premium/Enterprise → 26B MoE

## Acceptance Criteria

- [ ] Update 8 files reference models
- [ ] Test: Gemma 4 E4B handle logo analysis + marketing copy
- [ ] Benchmark: latency comparison với current stack
- [ ] Benchmark: Vietnamese content quality comparison
- [ ] RAM usage measurement: confirm save ~16GB
- [ ] Update docs `ai-local-implementation-plan.md` với model table mới
- [ ] Update `rules.md` config keys (không đổi) nhưng update default values

## Risks & Mitigation

| Risk | Mitigation |
|------|------------|
| Gemma 4 chưa stable trên Ollama | Test kỹ với sample requests trước khi production |
| Output quality khác llama3.1 (prompt tuning) | Regression test trên existing use cases, adjust prompts nếu cần |
| Ollama pull time | `ollama-setup` init container pulls trước khi service start |
| Rollback | Giữ env var `OLLAMA_TEXT_MODEL` — override về llama3.1:8b nếu cần |

## Dependencies

- Không blocked bởi gap nào
- Có thể làm parallel với GAP-002, GAP-005
- Khuyến nghị: làm cùng GAP-005 để config queue với model mới

## References

- [Gemma 4 blog (Google)](https://blog.google/innovation-and-ai/technology/developers-tools/gemma-4/)
- [Gemma 4 model card](https://ai.google.dev/gemma/docs/core/model_card_4)
- [Gemma 4 on Ollama](https://ollama.com/library/gemma4) (pull command: `ollama pull gemma4:e4b`)

## Log

- 2026-04-14 — Phát hiện Gemma 4 ra mắt 2026-04-02; scan config + đề xuất migrate
