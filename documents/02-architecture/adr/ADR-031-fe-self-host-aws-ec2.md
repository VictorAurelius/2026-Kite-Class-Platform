---
title: FE Self-Host trên AWS EC2 (t3.small) — Phase 1 BETA
status: PROPOSED
date: 2026-05-15
deciders: "@nguyenvankiet (solo-dev, acting CTO)"
consulted: 2 outside-in background audit agents (external benchmark + failure-mode matrix)
technical-story: documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md
---

# ADR-031: FE Self-Host trên AWS EC2 (t3.small) — Phase 1 BETA

**Status:** PROPOSED
**Date:** 2026-05-15
**Deciders:** @nguyenvankiet (solo-dev, acting CTO)
**Consulted:** 2 outside-in background audit agents — external benchmark (WebSearch VN SaaS 2026 pricing + Next.js compat) + failure-mode matrix (16 failure modes simulation per `simulation-gap-finder` methodology)
**Related Gap(s):** Wave 82 Bucket A decision lock (FE rebuild architecture)
**Related ADR(s):** ADR-025 (AWS Singapore Free Tier — vendor consistency anchor); ADR-029 (JVM-in-container memory budget — context cho RAM-sensitive co-host decision)

---

## Context

Phase 1 BETA frontend (`kitehub-frontend` + `kiteclass-frontend`, cả hai dùng Next.js standalone build) được serve qua **Vercel Free Tier** từ Wave 1. Khoảng 2026-05-13, Vercel Free Tier daily build limit (100 builds/day) hit cap → FE deploy bị block. Wave 81 backend deploy CLOSED 2026-05-15 với contract changes (Beta Status / Onboarding wizard / Staff Invitation / 2FA) NHƯNG FE stale ~38h không thể rebuild → full 126-row dev walk-through hit FE-BE drift thay vì code bug thực sự.

Wave 82 Bucket A cần lock kiến trúc FE serving moving forward. 3 phương án chính được đưa ra:

1. **Cloudflare Pages (free tier)** — DNS đã ở Cloudflare; CF Pages Next.js adapter (`next-on-pages`) hỗ trợ static + ISR
2. **AWS EC2 t3.small self-host** — ~$15/mo (ngoài Free Tier ECS budget); chạy `next start` standalone + nginx + PM2
3. **Vercel Pro** — $20/mo upgrade; bỏ build cap + giữ nguyên zero-config Next.js workflow

Để tránh blind spot inside-out (per `.claude/rules/outside-in-coverage-trigger.md`), 2 background audit agents chạy parallel 2026-05-15:

### Audit agent A — External benchmark (WebSearch VN SaaS 2026)

Tham chiếu pricing + adoption signal + Next.js compat:

- **Khuyến nghị: Vercel Pro $20/mo**
- Lập luận chính: **solo-dev time là tài nguyên scarce nhất**, $20/mo amortize over migration risk (DNS cutover + nginx config + PM2 + certbot + CORS sweep + SG audit = ~3-4h work + ongoing maintenance burden)
- Cloudflare Pages `next-on-pages` adapter struggle với app shape thực tế của project: standalone output + pnpm workspace + ISR enabled + Next.js Image Optimization → high migration friction + đụng edge cases không có upstream support
- AWS EC2 path "viable nhưng đắt hơn về dev time, không đắt hơn về tiền"

### Audit agent B — Failure-mode matrix (simulation-gap-finder)

Áp dụng `simulation-gap-finder` methodology lên repo state thực tế, surface 16 failure modes. Cũng converged **Vercel Pro recommendation** dựa trên total cost ownership (TCO) bao gồm operational burden.

Cho AWS EC2 path cụ thể, matrix flag 4 P0 mitigations bắt buộc:

| ID | Failure mode | Mitigation cost |
|---|---|---|
| **F6** | SG description audit — risk port 4701/22 internet-exposed nếu SG rule thiếu description per `aws-sg-description-ascii.md` (audit có thể bypass khi unable to parse rule intent) | SG audit + description bắt buộc per `.claude/rules/aws-sg-description-ascii.md` |
| **F7** | t3.small 2GB RAM tight cho 2 Next.js standalone + nginx + PM2; ISR regen có thể OOM → restart loop | PM2 with cluster mode + swapfile 1GB + CloudWatch memory alarm @ 80% |
| **F10** | certbot renewal silent failure → 100% outage khi cert expire (90d) → HTTPS error blanket | certbot DNS-01 challenge (avoid HTTP-01 race) + monitor cert expiry 30d ahead |
| **F11** | BE CORS allowlist không có new FE EC2 origin → cross-host requests rejected silently sau DNS flip | Sweep BE `allowed-origins` config TRƯỚC khi flip DNS; verify via curl pre-flip |

### Quyết định người dùng (locked 2026-05-15)

User chọn **AWS EC2 self-host** — đi ngược lại khuyến nghị của cả 2 audit agents. Lý do chính:

1. **Cost-priority** — solo-dev mode, no recurring $20/mo cost nếu tránh được. AWS EC2 t3.small ~$15/mo ngoài Free Tier nhưng vẫn rẻ hơn Vercel Pro + đã trong budget Phase 1 BETA tier
2. **Vendor consistency** — BE đã deploy hoàn toàn trên AWS Singapore Free Tier per ADR-025 (kh-backend EC2 + kiteclass-core RDS + ECR + Route53). Thêm 1 EC2 nữa cho FE giữ stack đồng nhất, không split vendor (Vercel vs AWS) → dễ ops, audit logs tập trung CloudTrail, IAM một chỗ
3. **Control over deploy infrastructure** — Vercel Pro vẫn là vendor-locked black box (build behavior, ISR cache, image optimization pipeline); tự host = quyền debug từng layer (nginx access log, PM2 stdout, EC2 system metrics)
4. **Acceptance of 4 P0 mitigation cost** — user chấp nhận trade-off "trả bằng dev time để tiết kiệm $20/mo" sau khi đọc kỹ failure matrix output

---

## Decision

**Sử dụng AWS EC2 t3.small (new instance, tên `kc-app`) làm FE serving host trong Phase 1 BETA**, thay thế Vercel Free Tier. Stack cụ thể:

- 1× EC2 t3.small (Singapore region `ap-southeast-1`, cùng region với BE)
- Ubuntu 22.04 LTS + Node.js 22 LTS + nginx (reverse proxy + TLS termination)
- PM2 cluster mode để chạy 2 Next.js standalone processes (kitehub-frontend port `3000` + kiteclass-frontend port `3001`)
- certbot DNS-01 challenge với Cloudflare DNS API token cho TLS cert auto-renewal
- 1GB swapfile + CloudWatch memory alarm @ 80%
- Security Group có description đầy đủ per `.claude/rules/aws-sg-description-ascii.md`

DNS cutover (`kitehub.me` + `*.kitehub.me`) flip từ Vercel sang new EC2 IP qua Cloudflare DNS edit, với TTL drop 60s 24h pre-flip để rollback nhanh nếu cần.

Quyết định này **đi ngược 2 audit agents recommendation** (cả hai đề xuất Vercel Pro). Tài liệu hoá ở đây để:
- Future-team thấy rationale + trade-off explicit, không phải re-litigate
- Re-evaluation trigger rõ ràng (xem §Implementation Notes)
- Transparency về audit findings được tôn trọng dù decision đi khác hướng

---

## Decision Drivers

| Driver | Trọng số | Notes |
|---|:---:|---|
| **Cost (recurring $/month)** | HIGH | Solo-dev mode, mọi recurring cost phải justify |
| **Vendor consistency** | HIGH | BE đã 100% AWS per ADR-025; FE trên Vercel = split vendor |
| **Control / debug surface** | MEDIUM | Self-host = full visibility; Vercel = black box |
| **Dev time (migration + ops)** | MEDIUM | Audit agents flag đây là cost cao nhất của EC2 path |
| **Latency / performance** | LOW-MEDIUM | Vercel edge CDN tốt hơn single-region EC2; mitigation = Cloudflare proxy trước EC2 |
| **Time-to-recover khi outage** | MEDIUM | Vercel auto-heals; EC2 self-host = user phải debug |
| **Phase 1 BETA scope** | HIGH | Decision có thể revisit khi sang Phase 2 (PAID tier hoặc K8s) |

---

## Considered Options

### Option 1 — Cloudflare Pages (free tier)

**Stack:** CF Pages + `next-on-pages` adapter; DNS đã ở CF; build qua Wrangler hoặc CF Pages git integration.

**Pros:**
- Free tier rộng (500 builds/month — gấp 5× Vercel Free)
- CF edge CDN built-in, không phải config thêm
- DNS đã ở CF → setup nhanh
- Zero recurring cost

**Cons (per audit agent A):**
- `next-on-pages` adapter chưa support hoàn toàn Next.js feature set của project: ISR + standalone output + pnpm workspace + Image Optimization có edge cases
- Adoption signal yếu cho production-grade Next.js trong VN SaaS 2026 (đa số dùng Vercel hoặc tự host)
- Migration friction cao: phải rewrite next.config.js + test ISR behavior + verify image optimization
- Vendor lock-in tương đương Vercel nhưng documentation + community support kém hơn cho Next.js cụ thể

**Verdict:** Rejected — migration risk cao, ROI thấp (vẫn vendor-locked, vẫn cần ops khi adapter break).

### Option 2 — AWS EC2 t3.small self-host (CHOSEN)

**Stack:** Đã mô tả ở §Decision.

**Pros:**
- Vendor consistency với BE per ADR-025 (audit/IAM/billing tập trung AWS)
- Full control: nginx config, PM2 process management, system logs, CloudWatch metrics
- Cost $15/mo < Vercel Pro $20/mo (marginal nhưng có)
- Skill build-up cho team: tự host Next.js → biết debug toàn stack
- Reusable infrastructure: nginx + PM2 pattern dùng được cho future FE services
- Cloudflare proxy ở phía trước EC2 → vẫn có edge CDN + DDoS protection

**Cons (per audit agent B — 4 P0 mitigations là acknowledged cost):**
- **F6:** SG audit + description discipline bắt buộc; lỡ tay có thể expose port 22/4701 internet-wide → security incident
- **F7:** t3.small 2GB RAM tight cho 2 Next standalone process + nginx → OOM risk khi ISR regen → PM2 + swapfile + memory alarm mandatory
- **F10:** certbot renewal silent failure = 100% outage khi cert expire (90d) — mitigation = DNS-01 + cert expiry monitor
- **F11:** BE CORS allowlist sweep pre-DNS-flip bắt buộc, nếu miss → cross-host requests reject silent
- Dev time setup ~3-4h + ongoing maintenance burden (cert renewal monitor, OS patching, PM2 health, log rotation)
- Single-region latency cao hơn Vercel edge cho user xa Singapore (mitigation: Cloudflare proxy)
- No auto-rollback khi deploy fail (Vercel có); user phải debug SSM + redeploy thủ công

**Verdict:** CHOSEN — cost-priority + vendor consistency thắng dev-time cost, với explicit acceptance của 4 P0 mitigations.

### Option 3 — Vercel Pro ($20/mo) — recommended by BOTH audit agents

**Stack:** Upgrade Vercel Free → Pro; unchanged deploy pipeline.

**Pros (per audit agent A):**
- Zero migration risk — keep existing workflow
- Solo-dev time saved (no nginx/PM2/certbot/SG config)
- Vercel edge CDN built-in, image optimization native
- $20/mo amortize over migration time saved trong 3-6 tháng đầu
- Auto-rollback khi deploy fail
- Build cap removed (unlimited builds/day)

**Cons:**
- Recurring $20/mo cost (vs $15/mo EC2)
- Vendor split: BE AWS + FE Vercel → audit logs split, billing split
- Black box deploy behavior (less debug surface)
- Pricing có thể tăng future (Vercel pricing history shows increases)

**Verdict:** Rejected — user chọn cost-priority + vendor consistency over migration risk. Audit agents recommendation noted + respectfully overridden.

---

## Decision Outcome

**CHOSEN: Option 2 — AWS EC2 t3.small self-host.**

**Honest disclosure:** 2 outside-in audit agents (external benchmark + failure-mode matrix) đều khuyến nghị Option 3 (Vercel Pro). User locked Option 2 với lý do cost-priority + AWS vendor consistency + control over deploy infrastructure. Quyết định này:

- KHÔNG bỏ qua audit findings — 4 P0 mitigations từ failure matrix được accepted as explicit cost (Wave 82 Bucket B pre-flight requirements)
- ĐÃ tôn trọng outside-in audit value — audit chạy đúng quy trình, findings ship đầy đủ, user đọc kỹ trước khi quyết định ngược
- Re-evaluation trigger documented (§Implementation Notes Phase 2)

### Positive Consequences

- **Vendor consistency:** 100% stack trên AWS Singapore per ADR-025 → audit/IAM/billing tập trung, không split vendor
- **Cost saving:** ~$5/mo so với Vercel Pro (marginal nhưng accumulate over Phase 1 BETA timeline 6-12 tháng)
- **Skill build-up:** Solo-dev có hands-on debug Next.js self-host → future-proof khi mở rộng FE services
- **Full debug surface:** nginx access log + PM2 stdout + EC2 system metrics + CloudWatch → đủ surface để root-cause production issue
- **Reusable infrastructure pattern:** nginx + PM2 + certbot setup có thể replicate cho future FE/edge services

### Negative Consequences

- **4 P0 mitigations cost (explicit acknowledged):**
  1. F6 SG audit + description discipline — bắt buộc per `aws-sg-description-ascii.md`, dev time setup + ongoing review
  2. F7 PM2 cluster mode + 1GB swapfile + CloudWatch memory alarm @ 80% — setup + alarm wiring + tune if false-positive
  3. F10 certbot DNS-01 challenge + Cloudflare DNS API token + cert expiry monitor 30d ahead — setup + monitor wiring
  4. F11 BE allowed-origins config sweep + curl verify pre-flip — manual checklist Bucket B; rủi ro miss endpoint
- **Dev time burden:** ~3-4h setup + ~30min/month ongoing maintenance (cert renewal verify, OS patching, log rotation, PM2 health check)
- **Single-region latency:** t3.small Singapore, user xa region (vd EU/US) latency cao hơn Vercel edge — mitigation Cloudflare proxy giảm phần nào nhưng không bằng Vercel edge
- **No auto-rollback:** Deploy fail → user phải SSM + debug + redeploy thủ công; Vercel có instant rollback
- **t3.small RAM ceiling:** Nếu sustained pressure (>80% memory long-term), phải upgrade t3.medium ($30/mo, mất phần lớn cost advantage so với Vercel Pro)

### Neutral Consequences

- Cloudflare DNS giữ nguyên — chỉ flip A record (kitehub.me) sang new EC2 IP; CDN/DDoS protection vẫn qua CF proxy
- BE deploy pipeline không đổi — chỉ FE thay đổi
- CI workflow GitHub Actions vẫn dùng được cho FE build → push artifact qua SSM hoặc S3 sync

---

## Pros / Cons Table — quick comparison

| Tiêu chí | Option 1 (CF Pages) | Option 2 (AWS EC2) ✅ | Option 3 (Vercel Pro) |
|---|:---:|:---:|:---:|
| **Cost ($/mo)** | $0 ✅ | ~$15 | $20 |
| **Migration risk** | HIGH (adapter compat) | MEDIUM (4 P0 mitigations) | LOW (no migration) |
| **Vendor consistency với BE** | ❌ split CF+AWS | ✅ 100% AWS | ❌ split Vercel+AWS |
| **Dev time setup** | ~4h | ~3-4h | <1h |
| **Ongoing ops burden** | LOW (CF managed) | MEDIUM (4 mitigations + cert + RAM monitor) | LOW (Vercel managed) |
| **Control / debug surface** | LOW | HIGH ✅ | LOW-MEDIUM |
| **Auto-rollback** | YES | NO | YES |
| **Edge CDN performance** | EXCELLENT | MEDIUM (CF proxy fronts) | EXCELLENT |
| **Next.js feature compat** | MEDIUM (adapter limit) | FULL ✅ | FULL ✅ |
| **Audit agent recommendation** | ❌ rejected | ❌ rejected | ✅ recommended by both |
| **User-chosen** | | ✅ | |

---

## Implementation Notes

### Phase 1 — Setup (Wave 82 Bucket B)

Sequence (per `concurrent-production-mutation-ops.md` — mọi mutation serial, không parallel):

1. Terraform create `infrastructure/terraform-aws/ec2-kc-app.tf` (new EC2 t3.small + SG với description đầy đủ)
2. SSH/SSM setup: Node.js 22 LTS + nginx + PM2 + certbot
3. Configure 1GB swapfile + CloudWatch memory alarm @ 80% (F7 mitigation)
4. certbot DNS-01 challenge với Cloudflare DNS API token (F10 mitigation)
5. BE CORS allowlist sweep — add new EC2 origin vào `allowed-origins` config của kitehub-gateway + kiteclass-gateway (F11 mitigation)
6. Build + deploy FE qua CI workflow OR SSM SendCommand
7. Smoke test pre-DNS-flip qua direct IP

### Phase 2 — DNS cutover (Wave 82 Bucket D)

1. TTL drop CF DNS A record `kitehub.me` xuống 60s, wait 24h
2. Flip A record → new EC2 IP (proxied through CF)
3. `dig kitehub.me` verify trong 5 phút post-flip
4. Smoke test full 126-row dev walk-through (Wave 82 Bucket H)

### Rollback path

Nếu post-flip issues phát hiện:
1. CF DNS flip lại Vercel IP gốc (TTL 60s → propagation ≤5 phút)
2. Investigate EC2 issue offline
3. Re-flip khi fix

### Re-evaluation triggers (cho future ADR-supersede)

ADR này phải được re-evaluate khi:

- **t3.small RAM sustained pressure >80%** trong 2 tuần liên tiếp → upgrade t3.medium ($30/mo) hoặc revisit Vercel Pro
- **Cert renewal failure** (silent outage incident) → consider managed cert solution (AWS ACM with CloudFront fronting EC2)
- **Phase 2 PAID launch** → tenant cohort lớn, edge CDN latency matters → revisit Vercel Pro / Cloudflare Pages
- **Dev time burden >2h/month** sustained → cost advantage so với Vercel Pro bay hết → revisit
- **K8s migration** (per ADR-028 deferral) → FE serving có thể move vào EKS/ECS Fargate cluster

### Success criteria (Wave 82 closure gate)

- [ ] `dig kitehub.me` returns new EC2 IP
- [ ] FE serves Wave 78+79+80+81 contract changes (Beta Status / Onboarding / Staff Invitation / 2FA UI)
- [ ] No downtime >5 min during DNS cutover
- [ ] 4 P0 mitigations (F6/F7/F10/F11) verified addressed via audit artifact
- [ ] Full 126-row dev walk-through completes post-flip

---

## References

- Wave 82 plan: [`documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md`](../../03-planning/waves/wave-2026-05-15-82-fe-self-host.md) §1 Brainstorm Q1 (audit agent findings) + §3 Bucket A
- ADR-025: [`ADR-025-aws-only-deploy-phase-1-free-tier.md`](ADR-025-aws-only-deploy-phase-1-free-tier.md) — AWS Singapore vendor anchor
- ADR-029: [`ADR-029-jvm-container-memory-budget.md`](ADR-029-jvm-container-memory-budget.md) — RAM-sensitivity context cho co-host decision (FE chọn new EC2 thay vì co-host kh-backend vì BE đã tight RAM)
- ADR-028: [`ADR-028-ecs-fargate-vs-eks-phase-1-beta.md`](ADR-028-ecs-fargate-vs-eks-phase-1-beta.md) — K8s deferral context (re-evaluation trigger)
- Rule: [`.claude/rules/outside-in-coverage-trigger.md`](../../../.claude/rules/outside-in-coverage-trigger.md) — mandate 2-agent outside-in audit
- Rule: [`.claude/rules/aws-sg-description-ascii.md`](../../../.claude/rules/aws-sg-description-ascii.md) — F6 mitigation source
- Rule: [`.claude/rules/concurrent-production-mutation-ops.md`](../../../.claude/rules/concurrent-production-mutation-ops.md) — Phase 1 setup serialization mandate
- Audit agent reports: Wave 82 plan §1 Brainstorm Q1 (background agents results consolidated in plan body 2026-05-15)

---

## Log

- **2026-05-15:** ADR created (PROPOSED). Wave 82 Bucket A decision lock — AWS EC2 t3.small self-host chosen over 2 audit agents' Vercel Pro recommendation. Cost-priority + AWS vendor consistency + control over deploy infrastructure justify decision; 4 P0 mitigations (F6/F7/F10/F11) accepted as explicit cost. Re-evaluation triggers documented (§Implementation Notes). Pending review + ACCEPTED flip when Wave 82 Bucket B-D shipped successfully + 126-row walk-through verifies FE-BE integration.
