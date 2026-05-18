# GAP-006: Upgrade AI models to Gemma 4

**Status:** 🔵 OPEN — ⏸ **DEFERRED 2026-04-28** until local Ollama + Docker stack ready (see Log entry 2026-04-28)
**Priority:** 🟠 P1 (cost + quality improvement)
**Domain:** AI / Backend
**Detected:** 2026-04-14
**Blocked-on:** Local Ollama daemon + full kitehub Docker stack running (WSL2 CPU-only too slow for Gemma 4 9B A/B test per `feedback_gap006_infra_blocker.md`). GAP-244 dev-profile schema fix landed 2026-04-28 ✅; Ollama + stack remain.
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

| Model | Params | RAM | Context | Modality | Tool Calling | Use Case |
|-------|--------|-----|---------|----------|:------------:|----------|
| **E2B** | Effective 2B | ~4GB | 128K | Text + Image + Video + Audio | — | Smartphone, Raspberry Pi |
| **E4B** | Effective 4B | ~8GB | 128K | Text + Image + Video + Audio | — | Edge/consumer with audio needs |
| **9B** *(released 2026-04-02)* | 9B dense | **~6GB** | 128K | Text + Image | ✅ **Built-in** | **Edge/consumer — BEST FIT for Kite** |
| **26B MoE** | 26B (sparse) | ~26GB | 256K | Text + Image | ✅ | Consumer GPU / Premium tier |
| **31B Dense** | 31B | ~62GB | 256K | Text + Image | ✅ | Data center |

**Đặc điểm Gemma 4:**
- ✅ Multimodal **native** (1 model thay cho 2)
- ✅ 140+ ngôn ngữ với cultural context (tiếng Việt tốt hơn)
- ✅ Apache 2.0 license (thoải mái commercial)
- ✅ Configurable "thinking modes" cho reasoning tasks
- ✅ Day-one Ollama support
- ✅ **Gemma 4 9B built-in tool calling** — match `ai-branding-guidelines.md` §3 agent orchestration (Analyzer → Planner → PlanExecutor) without extra prompt engineering
- ✅ 31B beats Llama 4 400B trên benchmarks (AIME 89.2%, LiveCodeBench 80.0%, GPQA 84.3%)
- ✅ Vision benchmark leader: Gemma 4 31B 76.9% MMMU Pro

### Vietnamese-Specialized Alternative: MixSura

State-of-the-art MoE Vietnamese LLM (Ho Chi Minh University of Technology + Stanford collaboration), available on Ollama: `nqduc/mixsura:mixsura-q6_K`. Worth A/B testing for Vietnamese content quality vs Gemma 4 9B before final commit.

### Future Watch: Qwen 3.6-27B (released 2026-04-22)

Newest Qwen flagship. Strong on agentic coding (SWE-bench 77.2 beats Qwen 3.5 397B-A17B), Apache 2.0, native multimodal text+image+video, 256K context, 201 languages.

**BLOCKED for Kite project — 3 reasons:**

1. **Ollama incompatibility** — *"Ollama does not yet support the separate mmproj vision files Qwen 3.6 uses. Use llama.cpp / LM Studio / vLLM / SGLang"*. Kite entire AI layer (`OllamaClient.java`, `OllamaResponse`, `AIProviderConfig`, ADR-020 `/client/` adapter convention) is Ollama-bound. Switching backend = major rewrite.
2. **RAM marginal** — Q4_K_M = 16.8GB weights + KV cache + OS = "just fits" Oracle Cloud 24GB ARM. Multi-tenant concurrent workers cạn RAM. Gemma 4 9B (6GB) cho phép 3-4 concurrent; Qwen 3.6 chỉ 1.
3. **Use-case mismatch** — Qwen 3.6 strength = code generation. Kite use case = logo analysis + banner generation + Vietnamese marketing copy. Coding benchmarks impressive but irrelevant.

**Re-evaluate when:** (a) Ollama adds mmproj support, (b) project tier upgrades to 48GB+ Oracle ARM, (c) project scope expands to code-gen.

## Comparison Matrix (revised 2026-04-26 after candidate research)

| Criterion | llama3.1+llava (current) | Gemma 4 E4B | **Gemma 4 9B** ⭐ | Gemma 4 26B MoE | MixSura (VN-spec) | Qwen 3.6-27B |
|-----------|--------------------------|-------------|:-----------------:|-----------------|-------------------|--------------|
| RAM | ~24GB (cả 2) | ~8GB | **~6GB** | ~26GB | varies | 16.8-28.6GB (quant-dep) |
| Multimodal | 2 models separate | 1 native (+audio) | 1 native | 1 native | text-only | 1 native |
| Tool calling | manual prompt | manual | **✅ built-in** | ✅ | manual | ✅ |
| Vietnamese | Limited | Native 140+ langs | Native 140+ langs | Native 140+ langs | **VN-specialized SOTA** | 201 langs (no VN benchmark) |
| Context | 128K / 32K | 128K | 128K | 256K | varies | 256K |
| License | Llama restricted | Apache 2.0 ✅ | Apache 2.0 ✅ | Apache 2.0 ✅ | Apache 2.0 ✅ | Apache 2.0 ✅ |
| Ollama support | ✅ | ✅ day-1 | ✅ day-1 | ✅ day-1 | ✅ | ❌ **mmproj not supported** |
| Oracle 24GB fit | Marginal | Plenty | **Plenty** (3-4 workers) | Just fits (1 worker) | TBD | Tight (1 worker) |
| Use case match (AI Branding) | Baseline | Good | **Best** (tool-calling) | Better quality | VN content only | Coding-focused, mismatch |

## Proposed Fix (revised 2026-04-26)

### Primary recommendation: **Gemma 4 9B** ⭐ (changed from E4B)

**Lý do đổi từ E4B → 9B:**
- **Tool calling built-in** — `ai-branding-guidelines.md` §3 require Analyzer → Planner → PlanExecutor agent orchestration. Native tool-calling = bỏ 30-40% prompt engineering boilerplate.
- **RAM thấp hơn** — 6GB vs 8GB (-2GB → 4 concurrent workers thay vì 3 trên Oracle 24GB ARM)
- **Audio support không cần** — E4B's audio capability không phục vụ AI Branding use case (không có voice input)
- **Same multimodal core** — text+image native cho logo analysis + banner generation

**Migration scope (8 files — same):**

```diff
# docker-compose.kitehub.yml, docker-compose.oracle-backend.yml
- OLLAMA_TEXT_MODEL: ${OLLAMA_TEXT_MODEL:-llama3.1:8b}
- OLLAMA_VISION_MODEL: ${OLLAMA_VISION_MODEL:-llava:13b}
+ OLLAMA_TEXT_MODEL: ${OLLAMA_TEXT_MODEL:-gemma4:9b}
+ OLLAMA_VISION_MODEL: ${OLLAMA_VISION_MODEL:-gemma4:9b}
+ # Gemma 4 9B is multimodal native + tool-calling — 1 model for text + vision + agent orchestration
```

```diff
# kitehub-branding/src/main/resources/application.yml
  ollama:
-   text-model: ${OLLAMA_TEXT_MODEL:llama3.1:8b}
-   vision-model: ${OLLAMA_VISION_MODEL:llava:13b}
+   text-model: ${OLLAMA_TEXT_MODEL:gemma4:9b}
+   vision-model: ${OLLAMA_VISION_MODEL:gemma4:9b}
```

### Pre-commit A/B test: Vietnamese content quality

**BEFORE merging migration**, run quality A/B between:
- (a) Gemma 4 9B
- (b) MixSura (VN-specialized MoE)

on representative AI Branding tasks: logo analysis, banner copy, school-name marketing line. If MixSura beats Gemma 4 9B by ≥10% on VN content quality (manual rubric: cultural fit, grammar, tone), consider MixSura as primary for Vietnamese tenants + Gemma 4 9B for non-VN.

### Tier route (Premium tenant): Gemma 4 26B MoE

Nếu paid Oracle GPU tier hoặc AWS g4dn:
- Better quality cho premium/enterprise users (per GAP-005 dedicated workers)
- 256K context cho long documents
- Route via tier: Free/Pro → 9B, Premium/Enterprise → 26B MoE

### Future watch: Qwen 3.6-27B

Re-evaluate khi: (a) Ollama add mmproj support, (b) RAM tier upgrade 48GB+, (c) project mở rộng sang code-gen use case. Otherwise stay on Gemma 4 family — proven Ollama integration + RAM headroom + use-case fit.

## Acceptance Criteria (revised 2026-04-26)

- [ ] **Pre-migration: Vietnamese content quality A/B** — Gemma 4 9B vs MixSura on 10 representative branding tasks (logo, banner, marketing line for VN school). Manual rubric per `ai-branding-guidelines.md`. Decide primary based on results.
- [ ] **Tool-calling test** — Gemma 4 9B native tool-calling integrates with `ai-branding-guidelines.md` §3 Analyzer/Planner/PlanExecutor pipeline (verify with 1 representative provisioning step).
- [ ] Update 8 files reference models (model name `gemma4:9b` — not `e4b`)
- [ ] Test: Gemma 4 9B handle logo analysis + marketing copy with quality on par or better than current llama3.1+llava baseline
- [ ] Benchmark: latency comparison với current stack (target: equal or better)
- [ ] Benchmark: Vietnamese content quality vs current + vs MixSura (3-way)
- [ ] RAM usage measurement: confirm save ~18GB (24GB → 6GB)
- [ ] Concurrent workers: verify 3-4 workers fit Oracle 24GB ARM
- [ ] Update docs `ai-local-implementation-plan.md` với model table mới
- [ ] Update `rules.md` config keys (không đổi) nhưng update default values
- [ ] Document Qwen 3.6-27B re-evaluation triggers in monitoring backlog

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
- [Gemma 4 on Ollama](https://ollama.com/library/gemma4) (pull commands: `ollama pull gemma4:9b` for primary, `ollama pull gemma4:e4b` for audio variant)
- [Gemma 4 family guide — 2B to 27B (aimadetools)](https://www.aimadetools.com/blog/gemma-4-family-guide/)
- [Gemma 4 vs Qwen 3.5 vs Llama 4 benchmarks (ai.rs)](https://ai.rs/ai-developer/gemma-4-vs-qwen-3-5-vs-llama-4-compared)
- [Best Ollama Models 2026 by use case (ML Journey)](https://mljourney.com/best-ollama-models-in-2026-a-practical-guide-by-use-case/)
- [MixSura VN-specialized MoE on Ollama](https://ollama.com/nqduc/mixsura:mixsura-q6_K)
- [Qwen3.6-27B announcement (Qwen blog)](https://qwen.ai/blog?id=qwen3.6-27b) — future watch
- [Qwen3.6-27B VRAM requirements (Will It Run AI)](https://willitrunai.com/blog/qwen-3-6-27b-vram-requirements) — future watch
- [Best Open-Source LLMs April 2026 (Lushbinary)](https://lushbinary.com/blog/best-open-source-llms-april-2026-comparison-guide/)

## Log

- **2026-04-28 (DEFERRED)** — Pickup attempted at session start, Docker stack down + Ollama not reachable on `localhost:11434`. Per `feedback_gap006_infra_blocker.md` 4-6h estimate breaks at pre-flight on WSL2 CPU-only — Gemma 4 9B A/B test against MixSura is the long-pole AC and infeasible without GPU acceleration / running daemon. GAP-244 dev-profile schema mismatch (`created_by`/`updated_by` BIGINT alignment) shipped today via V46 migration ✅, removing one blocker; Ollama + stack remain. Status stays 🔵 OPEN with explicit Blocked-on header. Resume conditions: (1) Ollama daemon running with `gemma4:9b` + `nqduc/mixsura:mixsura-q6_K` pulled, (2) `./kitehub/scripts/up.sh` green, (3) sufficient compute for 9B inference latency target. Next-recommended-wave queue updated to skip AI Branding cluster (GAP-006 + GAP-223 Sub-PR 223.2) until conditions met.
- **2026-04-26** — Re-evaluation after user concern about gap freshness (12 days since creation). Researched landscape: Qwen 3.6-27B (released 2026-04-22), Gemma 4 9B variant (released 2026-04-02 alongside E4B), MixSura VN-specialized. **Recommendation changed**: E4B → **9B** (lower RAM 6GB vs 8GB + built-in tool-calling matches AI Branding agent orchestration requirement; audio support of E4B not needed). **Added Vietnamese A/B test against MixSura** as pre-migration AC. **Qwen 3.6-27B documented as future watch** — currently blocked on Ollama mmproj support + RAM marginal on Oracle 24GB + use-case mismatch (coding-focused). Comparison matrix expanded from 3-column to 6-column.
- 2026-04-14 — Phát hiện Gemma 4 ra mắt 2026-04-02; scan config + đề xuất migrate (initial recommendation: Gemma 4 E4B)
