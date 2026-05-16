# Load Test K6 — Cohort 50 Signup Baseline

**Audience:** Solo dev chuẩn bị Phase 1 BETA invite ≥20 tenants → cần verify hệ thống chịu được 50 concurrent users signup flow trước khi tag v1.0.0-rc.
**Standards:** AWS Well-Architected (Performance Efficiency + Reliability) · `release-deploy-standard.md` §3.4 · `pre-handoff-self-test-completeness.md` §2.1 (auth-gated flow verify).
**Cross-link upstream:** Yêu cầu EC2 stack đang chạy + Vercel landing live + Resend API key active.
**Cross-link downstream:** Wave 86 Bucket H H-AC1 AC — K6 baseline doc shipped; actual run = Wave 87+ post EC2 restart.
**Estimated time:** ~30 min runbook đọc; ~15 min chạy K6 (2 min ramp + 5 min hold + 2 min ramp-down + 5 min report).
**Last-Updated:** 2026-05-16

---

## TL;DR

> K6 script `infrastructure/load-test/k6-cohort-50-signup.js` mô phỏng 50 concurrent virtual users (VUs) submit signup flow → đo P95 latency < 3s + 0 error rate >1% → baseline cho v1.0.0-rc Phase 1.5 invite cohort.

Quick path 4 bước:

1. Cài K6: `brew install k6` (macOS) / `apt install k6` (Ubuntu) / `docker run grafana/k6` (containerized)
2. Verify pre-conditions: EC2 stack `running` + Vercel landing reachable + Resend test mode active
3. Chạy: `k6 run infrastructure/load-test/k6-cohort-50-signup.js` với env vars (xem §3)
4. Review output: P95 < 3s + error rate < 1% = PASS baseline

---

## 1. Pre-conditions

| Item | Verify | Why |
|------|--------|-----|
| EC2 stack chạy | `bash scripts/aws/start-stack.sh` → wait 5 min healthcheck | K6 cần production endpoint thật |
| Vercel landing live | `curl -sI https://kitehub.vercel.app/ → 200` | Signup flow entry point |
| Resend test mode | Verify Resend dashboard "Domains" tab → `kitehub.me` status `Verified` | Email verification step trong flow |
| RDS available | `aws rds describe-db-instances --query 'DBInstances[?DBInstanceIdentifier==\`kitehub-prod\`].DBInstanceStatus' --output text` → `available` | DB write target signup data |
| Beta Access invite-only mode | Check `kitehub-subscription` config — Phase 1 BETA cần whitelist hoặc disable signup if invite-only | Avoid spam signup creating real beta accounts |
| Tenant isolation seed data | Optional: seed test tenant để K6 không pollute production data | RLS không leak; soft-deleted post-test |

⚠️ **Critical:** K6 cohort 50 = 50 real signups + 50 real emails sent via Resend (test mode hoặc real). Tự suspend Resend 100/day limit if running multiple times. Recommend Resend Pro $20/month before Phase 1.5 invite (xem `resend-paid-upgrade-runbook.md`).

---

## 2. K6 script

Script lưu tại `infrastructure/load-test/k6-cohort-50-signup.js` (paired same PR). Specification:

| Setting | Value | Why |
|---|---|---|
| Scenario type | `ramping-vus` (graceful ramp) | Realistic invite-blast pattern |
| Stages | 2 min ramp 0→50, 5 min hold 50, 2 min ramp 50→0 | Total 9 min — covers SLO window |
| Target URL | `https://app.kitehub.me/api/v1/auth/request-beta-access` (or landing form endpoint) | Primary signup flow |
| Method | POST | Per `api-contract.md` |
| Payload | `{email, name, organization, source}` JSON | Per beta-access request shape |
| Rate limit awareness | 2/sec/IP burst 5 per `pre-launch-auth-hardening-checklist.md` §2.1 → spread VUs across IP pool OR test single-IP throttling behavior | Gateway will rate-limit single IP |
| Think time | `sleep(1)` between iterations (simulate user reading form) | Realistic user behavior |
| Thresholds | `http_req_duration{expected_response:true}` p(95) < 3000ms; `http_req_failed` rate < 0.01 | Phase 1 BETA SLO target |
| Output | JSON to `infrastructure/load-test/results/cohort-50-YYYY-MM-DD.json` + summary stdout | Baseline artifact |

---

## 3. Run procedure

### 3.1 Install K6

```bash
# macOS
brew install k6

# Linux (Debian/Ubuntu)
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update && sudo apt install k6

# Docker (no install)
docker run --rm -i grafana/k6:latest run - <infrastructure/load-test/k6-cohort-50-signup.js
```

### 3.2 Set env vars

```bash
export K6_BASE_URL=https://app.kitehub.me
export K6_TEST_EMAIL_DOMAIN=test.kitehub.me  # safe domain to avoid spam real inbox
export K6_OUTPUT_DIR=infrastructure/load-test/results
mkdir -p "$K6_OUTPUT_DIR"
```

### 3.3 Run

```bash
# Foreground (watch progress)
k6 run --out json=$K6_OUTPUT_DIR/cohort-50-$(date +%Y-%m-%d).json \
  infrastructure/load-test/k6-cohort-50-signup.js

# Background (long-run)
nohup k6 run --out json=$K6_OUTPUT_DIR/cohort-50-$(date +%Y-%m-%d).json \
  infrastructure/load-test/k6-cohort-50-signup.js > /tmp/k6.log 2>&1 &
```

### 3.4 Verify pass criteria

K6 stdout summary tail:
```
✓ http_req_duration p(95): 2.8s   ← MUST < 3s
✓ http_req_failed: 0.4%           ← MUST < 1%
✓ checks: 99.7%                   ← MUST > 99%
```

If FAIL → review:
- P95 > 3s: investigate slow endpoint via CloudWatch metrics or grafana dashboard
- Error rate > 1%: check gateway rate-limit logs (`/api/auth/request-beta-access` should allow 2/sec burst 5)
- 5xx errors: check `kitehub-subscription` logs via SSM

---

## 4. Acceptance criteria (Phase 1 BETA target)

| Metric | Target | Source |
|---|---|---|
| P95 latency | < 3000ms | `release-deploy-standard.md` §3.4 SLO |
| Error rate | < 1% | Phase 1 BETA acceptable |
| Throughput | ≥ 50 successful signups in 5 min hold | 50 VUs × 0.2 RPS effective |
| Email delivery | 100% via Resend dashboard `Delivered` status | Confirms downstream pipeline |
| RDS connection pool not saturated | HikariCP `active < max` per CloudWatch alarm | Wave 85 Bucket E alarm in place |
| No 5xx from gateway | Gateway `error_rate` metric < 0.01 | Avoid cascading failure |

---

## 5. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| K6 reports 100% rate-limit 429 | Single-IP rate-limit hit (gateway 2/sec/IP) | (a) Reduce VUs to 10 cho retry; (b) Spread VUs via proxy pool; (c) Document expected behavior + adjust target |
| Slow P95 > 5s at hold | Spring Boot cold-start OR RDS connection pool exhausted | (a) Pre-warm với 5 VU × 1 min first; (b) Increase RDS max connections; (c) Increase HikariCP max-pool-size |
| Resend `429 Too Many Requests` | Free tier 100/day limit hit | Upgrade to Resend Pro $20/month per `resend-paid-upgrade-runbook.md` |
| Email không tới inbox | DKIM/SPF/DMARC propagation OR Resend domain `Unverified` | Re-verify domain Resend dashboard + check Cloudflare DNS records |
| K6 segfault Mac M1 | Stale K6 binary | `brew upgrade k6` → re-run |

---

## 6. Scheduled cadence

| Trigger | Cadence | Action |
|---|---|---|
| Pre-tag v1.0.0-rc.1 | One-time | Run baseline + verify all 6 §4 criteria |
| Phase 1.5 invite blast ≥20 tenants | Pre-invite | Re-run; expect tighter P95 < 2s |
| Quarterly maintenance window | Every 90d | Re-baseline against drift |
| Post-major-deploy (Spring Boot bump, RDS upgrade) | Post-deploy | Regression check |

---

## 7. References

- K6 docs: https://k6.io/docs/
- Wave 86 plan §3 Bucket H H-AC1
- `documents/04-quality/audits/performance/2026-05-15-wave-85-post-apply.md` (86/100 baseline)
- `pre-handoff-self-test-completeness.md` §2.1 — auth-gated flow verify
- `release-deploy-standard.md` §3.4 SLO targets
- `infrastructure/load-test/k6-cohort-50-signup.js` (paired same PR)
- `resend-paid-upgrade-runbook.md` (sister runbook)
