# AI Local Implementation Plan

**Ngày tạo**: 2026-03-18
**Mục tiêu**: Triển khai AI theo đúng design - hybrid local + cloud

---

## 1. Design vs Reality Gap

### Design gốc (từ architecture-qa.md, graduation-thesis-outline-v3.1.md)

```
Hybrid Architecture:
  - Self-hosted (local): Image generation, background removal, bulk quiz
  - Cloud API (OpenAI): Logo analysis, marketing copy (complex NLP)

Lý do chọn self-hosted:
  ✅ Privacy: Logo/ảnh không gửi ra bên ngoài
  ✅ No API limits: Không bị rate limit
  ✅ No vendor lock-in: Không phụ thuộc OpenAI
  ✅ Fine-tune: Tùy chỉnh model cho education domain
  ✅ Cost: $0 cho bulk tasks (chỉ trả infra)
  ✅ Latency: 20-30s local vs 60s+ cloud
```

### Hiện tại (implementation)

```
Cloud-only (OpenAI):
  - Tất cả AI features gọi OpenAI API
  - Local dev: mock mode (trả sample data)
  - Không có self-hosted model nào
```

### Gap chi tiết

| Feature | Design | Reality | Gap |
|---------|--------|---------|-----|
| Banner/Hero generation | Stable Diffusion XL (self-hosted) | DALL-E 3 (OpenAI cloud) | Cần chuyển sang local |
| Background removal | U2-Net (self-hosted) | Chưa implement | Cần implement |
| Logo analysis | GPT-4 Vision (cloud) | GPT-4 Vision (cloud) | ✅ Đúng |
| Marketing copy | GPT-4o-mini (cloud) | GPT-4 Turbo (cloud) | Đúng, cần downgrade model |
| Quiz generation | Hybrid: OpenAI + Llama 3.1 | Chưa implement | Cần implement |

---

## 2. Giải pháp: Ollama cho Local Dev

### Ollama là gì?
- Chạy AI models trên máy local (như Docker cho AI)
- 1 command install, 1 command pull model
- API tương thích OpenAI format → dễ switch
- Chạy trên CPU (chậm hơn) hoặc GPU (nhanh)

### Models cần cho KiteHub

| Feature | Ollama Model | Size | RAM cần | Thay thế |
|---------|-------------|------|---------|----------|
| Logo analysis | `llava:13b` | ~8GB | 16GB | GPT-4 Vision |
| Marketing copy | `llama3.1:8b` | ~4.7GB | 8GB | GPT-4o-mini |
| Image generation | `stable-diffusion` * | ~4GB | 8GB | DALL-E 3 |
| Quiz generation | `llama3.1:8b` | ~4.7GB | 8GB | GPT-4o-mini |

\* Stable Diffusion qua Ollama chưa mature. Alternative: ComfyUI hoặc Automatic1111 container.

### RAM Requirements

| Profile | Models loaded | RAM cần | Phù hợp |
|---------|-------------|---------|---------|
| Minimal (text only) | llama3.1:8b | ~8GB | Laptop 16GB |
| Standard (text + vision) | llama3.1 + llava | ~16GB | Desktop 32GB |
| Full (text + vision + image) | All models | ~24GB | Desktop 32GB + GPU |

---

## 3. Architecture: Local vs Production

### Local Development

```
KiteHub Branding Service
    ↓
AIBrandingService
    ↓ (check profile)
    ├── dev profile → OllamaClient (localhost:11434)
    │   ├── llava:13b (logo analysis)
    │   ├── llama3.1:8b (marketing copy)
    │   └── ComfyUI (image generation) *optional
    │
    └── prod profile → OpenAIClient (api.openai.com)
        ├── GPT-4 Vision (logo analysis)
        ├── GPT-4o-mini (marketing copy)
        └── [Stable Diffusion XL on GPU server] (image generation)
```

### Docker Compose (local)

```yaml
# Thêm vào docker-compose.kitehub.yml
ollama:
  image: ollama/ollama:latest
  container_name: kitehub-ollama
  ports:
    - "11434:11434"
  volumes:
    - ollama-models:/root/.ollama
  # GPU support (nếu có):
  # deploy:
  #   resources:
  #     reservations:
  #       devices:
  #         - capabilities: [gpu]

# Init container: pull models
ollama-setup:
  image: ollama/ollama:latest
  depends_on:
    - ollama
  entrypoint: >
    /bin/sh -c "
    sleep 10;
    ollama pull llama3.1:8b;
    ollama pull llava:13b;
    echo 'Models ready';
    "
```

### Production (AWS)

```
Option A: Self-hosted trên GPU instance
  - EC2 g4dn.xlarge (NVIDIA T4, ~$0.526/hr)
  - Stable Diffusion XL cho image generation
  - Cost: ~$380/month (on-demand) hoặc ~$150/month (spot)

Option B: AWS Bedrock
  - Managed AI service
  - Llama 3.1, Stable Diffusion via API
  - Pay-per-use, không cần quản lý GPU
  - Cost: ~$0.001-0.01 per request

Option C: Hybrid (Recommended)
  - Text (copy, quiz): AWS Bedrock Llama 3.1 ($0.001/request)
  - Image generation: Self-hosted Stable Diffusion XL ($150/month spot)
  - Logo analysis: OpenAI GPT-4 Vision ($0.01/request) - khó thay thế
```

---

## 4. PRs cần implement

### PR-AI-1: Ollama container + OllamaClient
**Priority**: P1
**Scope**:
- [ ] Thêm Ollama + ollama-setup vào docker-compose
- [ ] Tạo `OllamaClient.java` (tương tự OpenAIClient, gọi Ollama API)
- [ ] Ollama API tương thích OpenAI format: `POST /api/chat`, `POST /api/generate`
- [ ] AIBrandingService switch giữa OpenAI/Ollama dựa trên profile
- [ ] application-dev.yml: `ai.provider: ollama`
- [ ] application-prod.yml: `ai.provider: openai`
**Estimate**: 2 ngày

### PR-AI-2: Image generation (ComfyUI hoặc Stable Diffusion)
**Priority**: P2
**Scope**:
- [ ] Thêm ComfyUI container (stable diffusion web UI)
- [ ] API wrapper cho image generation
- [ ] Thay thế DALL-E 3 calls bằng local Stable Diffusion
- [ ] Quality comparison: local vs DALL-E 3
**Estimate**: 2-3 ngày
**Lưu ý**: Cần GPU hoặc chấp nhận chậm (~2-5 phút/ảnh trên CPU)

### PR-AI-3: Background removal (U2-Net)
**Priority**: P2
**Scope**:
- [ ] Thêm U2-Net container (hoặc rembg Python service)
- [ ] API endpoint: POST image → return image without background
- [ ] Integrate vào branding wizard step 1
**Estimate**: 1 ngày

### PR-AI-4: Quiz Generator
**Priority**: P3 (sau khi LMS module hoàn thành)
**Scope**:
- [ ] Quiz generation service
- [ ] Multi-tier: Ollama (local) cho simple questions, OpenAI cho complex
- [ ] Template-based question generation
- [ ] Support Vietnamese
**Estimate**: 3-5 ngày

---

## 5. Execution Order

```
PR-AI-1 (Ollama + text AI) ──→ PR-AI-2 (Image generation)
                               PR-AI-3 (Background removal)
                                        ↓
                               PR-AI-4 (Quiz generator) ←── sau LMS module
```

### Immediate (có thể làm ngay):
- **PR-AI-1**: Ollama cho text generation (logo analysis + marketing copy)

### Sau khi có GPU hoặc chấp nhận CPU slow:
- **PR-AI-2**: Stable Diffusion cho image generation
- **PR-AI-3**: U2-Net cho background removal

### Sau khi LMS module có content:
- **PR-AI-4**: Quiz generator

---

## 6. Cost Comparison

### Local Dev (tất cả free)

| Component | Cost |
|-----------|------|
| Ollama (llama3.1 + llava) | $0 (chạy local) |
| ComfyUI/Stable Diffusion | $0 (chạy local) |
| U2-Net | $0 (chạy local) |
| **Total** | **$0/month** |

### Production (hybrid)

| Component | Provider | Cost/month |
|-----------|----------|------------|
| Logo analysis | OpenAI GPT-4 Vision | ~$5 (500 analyses) |
| Marketing copy | AWS Bedrock Llama 3.1 | ~$2 (2000 requests) |
| Image generation | Self-hosted SD XL (spot) | ~$150 |
| Background removal | Self-hosted U2-Net | $0 (cùng server SD) |
| Quiz generation | AWS Bedrock Llama 3.1 | ~$10 (10000 questions) |
| **Total** | | **~$167/month** |

### So với Cloud-only (OpenAI)

| Component | OpenAI Cost/month |
|-----------|------------------|
| GPT-4 Vision (500 analyses) | ~$15 |
| GPT-4o-mini (2000 copies) | ~$3 |
| DALL-E 3 (1500 images) | ~$60 |
| GPT-4o-mini (10000 questions) | ~$15 |
| **Total** | **~$93/month** |

**Kết luận cost**: Cloud-only rẻ hơn cho scale nhỏ. Self-hosted có lợi khi scale lớn + privacy requirements.

---

## 7. Decision Matrix

| Yếu tố | Cloud (OpenAI) | Local (Ollama/SD) | Recommendation |
|--------|----------------|-------------------|----------------|
| **Dev setup** | Cần API key + tiền | Free, 1 command | **Local cho dev** |
| **Privacy** | Data gửi ra ngoài | Data ở local | **Local cho production** |
| **Quality** | Tốt nhất | 70-90% so với cloud | **Cloud cho NLP, Local cho images** |
| **Cost (small)** | ~$93/month | ~$167/month (GPU) | **Cloud rẻ hơn** |
| **Cost (large)** | Scales linearly | Fixed GPU cost | **Local rẻ hơn** |
| **Vendor lock-in** | Phụ thuộc OpenAI | Không phụ thuộc | **Local** |
| **Maintenance** | Không cần | Cần quản lý GPU | **Cloud dễ hơn** |
