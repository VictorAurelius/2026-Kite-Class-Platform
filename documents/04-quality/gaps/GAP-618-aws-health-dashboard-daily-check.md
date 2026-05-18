# GAP-618 — AWS Service Health Dashboard daily check (automated scrape + alert)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-05-18 (Wave 92 Bucket E meta backlog filing — surfaced từ GAP-612 sự cố cần proactive AWS-level monitoring)
**Affects:** Long-term observability posture; phát hiện AWS regional outage + account-level event sớm; không phải Phase 1 BETA blocker, Phase 1.5+ candidate

## Problem

Hiện tại không có cơ chế proactive theo dõi AWS Service Health Dashboard (<https://health.aws.amazon.com/health/status>) hoặc AWS Personal Health Dashboard. Sự cố GAP-612 (account suspension 2026-05-17) đã expose blind-spot:
- Account suspension notification chỉ tới qua email `vannkite@outlook.com` (user phải check inbox)
- Không có CLI/script tự động pull AWS Health event API
- Khi `ap-southeast-1` region có issue (vd RDS maintenance, EC2 spot reclaim, DNS issue), dev phải truy cập console manually
- Không có alert channel khi AWS publishes regional event

Outside-in audit: dev sleep / OOO khi AWS push notification (vd 2 AM local time) → late discovery → late response. Beta tenant Phase 1.5+ sẽ expect dev response trong 4h windows; AWS Health daily check = entry point.

Cụ thể context: GAP-612 surfaced 2026-05-17 16:50 UTC mid-Wave-90 walkthrough — nếu có daily automated check, có thể detect sớm hơn (vd 04:09 UTC khi Free Tier alert email arrived — 12h lead time).

## Root Cause

Solo-dev mode + Phase 1 BETA scope chưa wire AWS Health monitoring vì:
- Không có tenant production data hiện tại → AWS event blast radius = solo dev annoyance only
- AWS Health Event API rate-limit + auth requirement (require enabled AWS Health API service trong account, $0 cost)
- Cognitive load: thêm 1 daily cron job + alert routing logic
- Trigger event GAP-612 chỉ vừa surface 2026-05-17 → lesson-learned đang được rút

Phase 1 BETA-acceptable strategy: rely on AWS email notification + user inbox check. Acceptable rủi ro cho 0-5 tenant scale.

## Proposed Fix

### Phase 1 — Enable AWS Health API + IAM policy (~15 phút)

AWS Health API là free service available cho Business/Enterprise Support plan accounts. Solo-dev account có thể không có Business plan — nếu vậy, fallback dùng public RSS feed `https://status.aws.amazon.com/rss/all.rss`.

**Option A: AWS Health API (require Business Support plan)**
- Endpoint: `aws health describe-events --filter ...`
- Account-specific events + regional events
- Cost: $0 (included với Business Support plan; chưa enable)

**Option B: Public RSS feed (free, region-only)**
- Endpoint: `https://status.aws.amazon.com/rss/ap-southeast-1.rss`
- Regional events only (no account-specific)
- Cost: $0
- Recommend cho Phase 1 BETA (chưa có Business Support plan)

### Phase 2 — Cron job script (~1h)

Create `scripts/aws-health-daily-check.sh`:

```bash
#!/bin/bash
# Daily AWS Health Dashboard check — alert via email + log artifact
# Cron: 08:00 UTC daily (15:00 ICT)
# Run on EC2 cron OR GitHub Actions scheduled workflow

set -euo pipefail

# Phase 1 BETA: use public RSS feed for ap-southeast-1
RSS_URL="https://status.aws.amazon.com/rss/ap-southeast-1.rss"
DATE=$(date -u +%Y-%m-%d)
ARTIFACT_DIR="documents/04-quality/audits/aws-health-checks"
mkdir -p "$ARTIFACT_DIR"

# Fetch RSS + extract events last 24h
curl -s "$RSS_URL" > "/tmp/aws-health-${DATE}.rss"

# Parse + filter events last 24h (use xmllint or simple grep)
EVENTS_COUNT=$(grep -c "<item>" "/tmp/aws-health-${DATE}.rss" || echo 0)

# Write artifact
cat > "$ARTIFACT_DIR/${DATE}-aws-health.md" <<EOF
# AWS Health Daily Check — ${DATE}

**Region:** ap-southeast-1
**Events count (last 24h):** ${EVENTS_COUNT}
**Source:** ${RSS_URL}

## Events

$(grep -A3 "<item>" "/tmp/aws-health-${DATE}.rss" | head -40)

## Verdict

$( [ "$EVENTS_COUNT" -gt 0 ] && echo "⚠️ Events detected — review needed" || echo "✅ No new events" )
EOF

# Email alert nếu có events
if [ "$EVENTS_COUNT" -gt 0 ]; then
  # Use AWS SES OR Resend OR mailto fallback
  echo "AWS Health events detected ${DATE}: ${EVENTS_COUNT} events. See $ARTIFACT_DIR/${DATE}-aws-health.md" \
    | mail -s "[KiteHub] AWS Health Alert ${DATE}" vannkite@outlook.com || true
fi
```

### Phase 3 — Schedule cron (~30 phút)

**Option A: GitHub Actions scheduled workflow (recommend — không cần EC2 running)**

Create `.github/workflows/aws-health-daily-check.yml`:

```yaml
name: AWS Health Daily Check
on:
  schedule:
    - cron: '0 8 * * *'  # 08:00 UTC = 15:00 ICT daily
  workflow_dispatch:
permissions:
  contents: write
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: bash scripts/aws-health-daily-check.sh
      - name: Commit artifact
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add documents/04-quality/audits/aws-health-checks/
          git commit -m "chore(aws-health): daily check $(date -u +%Y-%m-%d)" || echo "No changes"
          git push
```

**Option B: EC2 cron (require EC2 running 24/7)**

```bash
# crontab -e on kh-backend EC2
0 8 * * * /home/ec2-user/scripts/aws-health-daily-check.sh
```

Recommend Option A vì:
- Không phụ thuộc EC2 uptime (Phase 1 BETA EC2 hay stop khi idle)
- GitHub Actions free tier sufficient (5-10s/day × 30 ngày = ~5 phút/tháng)
- Artifact tự động commit vào repo (audit trail)

### Phase 4 — Document runbook (~30 phút)

Create `documents/05-guides/operations/aws-health-monitoring.md`:
- Daily check script invocation + cron schedule
- Artifact location + retention policy
- Triage flow khi event detected (RSS event → severity classify → AWS console verify → response action)
- Cross-reference với `incident-response-runbook.md` + GAP-616 (uptime monitoring)

## Acceptance Criteria

- [ ] AWS Health source chosen (Option B: public RSS feed cho Phase 1 BETA)
- [ ] Script `scripts/aws-health-daily-check.sh` shipped + executable + shellcheck clean
- [ ] GitHub Actions workflow `.github/workflows/aws-health-daily-check.yml` shipped
- [ ] Cron schedule active = 08:00 UTC daily (15:00 ICT — sau giờ làm việc dev)
- [ ] Artifact directory `documents/04-quality/audits/aws-health-checks/` created với README
- [ ] Email alert delivery verified (test: trigger workflow manually → email arrive)
- [ ] Runbook `documents/05-guides/operations/aws-health-monitoring.md` shipped
- [ ] Self-test: simulate event detect (manual sửa RSS fixture → script báo alert) → workflow pass
- [ ] First 7 ngày daily artifact accumulated (validate cadence)

## Related

- **Wave 92 plan:** [`documents/03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md`](../../03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md) §3 Bucket E
- **Sister gaps:** GAP-616 (uptime monitoring external) + GAP-617 (DR plan) — long-term observability cluster
- **Trigger event:** GAP-612 AWS account suspension 2026-05-17 — surfaced gap "không có proactive AWS-level event detection"
- **Cross-link gap:** GAP-613 (CloudWatch Free Tier reduce) — sister cost-monitoring; AWS Health alerts cũng có Free Tier exhaust warning
- **Rule:** `audit-to-gap-pipeline.md` §3 (gap template); `output-review-mandate.md` §3 (ops-readiness audit standard); `script-review-checklist` skill (shell script quality)
- **AWS docs:** <https://docs.aws.amazon.com/health/latest/ug/getting-started-api.html> (Health API); <https://status.aws.amazon.com/rss/all.rss> (RSS feed fallback)

## Log

- **2026-05-18:** Gap filed by Wave 92 Bucket E meta backlog filing. Surfaced từ GAP-612 incident lesson-learned: account suspension chỉ tới qua email manual check; cần proactive automated AWS event scrape. RSS feed (Option B) recommend cho Phase 1 BETA vì free + không require Business Support plan. Defer implementation Wave 93+ hoặc Phase 1.5 — không phải Phase 1 BETA blocker (solo-dev mode acceptable email check manual).
