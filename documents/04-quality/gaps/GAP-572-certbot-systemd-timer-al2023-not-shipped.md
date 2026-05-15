---
title: "GAP-572: Certbot systemd timer setup fails on AL2023 (units không ship)"
status: OPEN
priority: P2
domain: DevOps
phase: phase-1-beta
wave: 82-bucket-b-followup
created: 2026-05-15
---

# GAP-572: Certbot auto-renewal systemd timer fails on Amazon Linux 2023

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (cert manual renew works; 90-day window before expiry)
**Domain:** DevOps / Server bootstrap
**Found:** 2026-05-15 Wave 82 Bucket B server bootstrap SSM SendCommand
**Affects:** Auto-renewal cho wildcard `*.kitehub.me` cert (currently exp 2026-08-13)

## Problem

`infrastructure/fe-host/certbot-dns-01-setup.sh` Step 4 đã fail trên EC2 i-05cfda7c6c60b683f (AL2023) khi `systemctl enable certbot-renew.timer`:

```
[2026-05-15T11:53:05Z] Step 4: Setup certbot-renew.timer + nginx deploy-hook
[2026-05-15T11:53:05Z] ERROR: Cannot enable certbot-renew.timer
```

Script aborted at Step 4 do `set -euo pipefail` → Step 5 (CloudWatch metric publisher) + Step 6 (nginx reload) cũng không chạy.

Investigation via SSM SendCommand 2026-05-15 11:55 UTC:

```bash
$ systemctl list-unit-files | grep -i certbot   # → NO_CERTBOT_UNITS
$ ls /usr/lib/systemd/system/ | grep -i certbot  # → NO_CERTBOT_SERVICE
$ cat /etc/cron.d/certbot                        # → NO_CRON_FILE
$ certbot --version                              # → certbot 4.2.0
```

**Discovery:** Amazon Linux 2023 `python3-certbot-dns-cloudflare` package ships `certbot` binary nhưng KHÔNG ship systemd timer/service unit files (khác với Ubuntu/Debian package). Script `certbot-dns-01-setup.sh` assumed Ubuntu-like behavior.

## Root Cause

Script Step 4 chỉ chạy `systemctl enable certbot-renew.timer` — không check unit exist trước hoặc create unit inline nếu missing. Khác giữa Linux distros:
- **Ubuntu/Debian**: `certbot` package ships `/lib/systemd/system/certbot.timer` + `certbot.service`
- **Amazon Linux 2023**: package ships binary only

## Proposed Fix

Update `infrastructure/fe-host/certbot-dns-01-setup.sh` Step 4 để self-create unit files khi missing:

```bash
setup_renewal_timer() {
  log "Step 4: Setup certbot-renew.timer + nginx deploy-hook"

  # Check if package shipped systemd units (Ubuntu/Debian path)
  if systemctl list-unit-files certbot.timer 2>/dev/null | grep -q certbot.timer; then
    log "  Using package-provided certbot.timer"
    sudo systemctl enable --now certbot.timer
    return 0
  fi

  # AL2023 path — create units inline
  log "  Package không ship systemd units (likely AL2023) — creating inline"

  sudo tee /etc/systemd/system/certbot-renew.service > /dev/null <<EOF
[Unit]
Description=Certbot automatic cert renewal
After=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/bin/certbot renew --quiet --deploy-hook "systemctl reload nginx"
EOF

  sudo tee /etc/systemd/system/certbot-renew.timer > /dev/null <<EOF
[Unit]
Description=Run certbot-renew weekly
Persistent=true

[Timer]
OnCalendar=weekly
RandomizedDelaySec=12h

[Install]
WantedBy=timers.target
EOF

  sudo systemctl daemon-reload
  sudo systemctl enable --now certbot-renew.timer

  log "  certbot-renew.timer created + enabled"
}
```

## Acceptance Criteria

- [ ] Script handles both Ubuntu/Debian (package units) + AL2023 (inline units) paths
- [ ] `systemctl list-timers --all | grep certbot` returns active timer on EC2
- [ ] Manual run `sudo systemctl start certbot-renew.service` succeeds (cert hiện chưa near expiry → no-op)
- [ ] Update repo `infrastructure/fe-host/certbot-dns-01-setup.sh` + commit
- [ ] Re-run via SSM SendCommand trên `i-05cfda7c6c60b683f` để fix existing instance
- [ ] Verify next scheduled renewal will fire correctly (sysmctl list-timers shows NEXT)

## Workaround (immediate)

Cron-based fallback nếu không muốn fix script ngay:

```bash
echo "0 0,12 * * * root certbot renew --quiet --deploy-hook 'systemctl reload nginx'" | sudo tee /etc/cron.d/certbot-renew
```

Hoặc set CloudWatch reminder 60 ngày trước expiry (2026-06-13) để manual `certbot renew`.

## Related

- Wave 82 Bucket B post-apply audit: `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-post-apply.md`
- SSM bootstrap command ID: `c6399e78-b3ff-413c-a4d8-6676ac0c48f9` (2026-05-15 11:51 UTC)
- Sister gap: GAP-573 (CloudWatch metric publisher cũng không chạy do script abort)
- Cert expires: 2026-08-13 (~90d from acquire)
- Rule: `pre-launch-secrets-hardening-checklist.md` §2.5 rotation cadence

## Log

- **2026-05-15:** Gap filed post Wave 82 Bucket B server bootstrap. Cert acquired thành công nhưng auto-renewal timer setup fail. Investigate qua SSM tier-1 confirmed AL2023 certbot package không ship systemd units. Non-blocking — manual `certbot renew` works + cert valid 90 ngày tới 2026-08-13. Fix script khi convenient (ideally Wave 83 Phase 1).
