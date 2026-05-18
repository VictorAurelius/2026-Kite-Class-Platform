---
id: GAP-565
title: EC2 security group description audit + port 4701 nginx-only restriction (Wave 82 Bucket B prerequisite)
status: OPEN
priority: P0
domain: DevOps
phase: phase-1-beta
percent_complete: 0
created: 2026-05-15
updated: 2026-05-15
wave_target: 82
---

# GAP-565 — Wave 82 EC2 security group description + port 4701 internal-only

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — BLOCKING Bucket B EC2 provisioning
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-15 (Wave 82 Bucket A outside-in failure-mode matrix audit, finding F6)
**Affects:** Wave 82 FE self-host migration — new EC2 kc-app instance security posture

---

## Problem

Wave 82 Bucket A failure-mode matrix audit (finding F6) flag rủi ro: nếu provision EC2 kc-app instance mới với security group misconfig, port 4701 (Next.js standalone server, kitehub-frontend hoặc kiteclass-frontend port) có thể bị expose ra Internet thay vì giới hạn nội bộ qua nginx reverse proxy. Đây là attack surface không cần thiết — public traffic phải chỉ đi qua nginx (port 80/443) để được TLS termination + rate limiting + header sanitization.

Thêm vào đó, theo `.claude/rules/aws-sg-description-ascii.md`, MỌI rule trong security group PHẢI có description ASCII rõ ràng để audit trail. SG rules thiếu description → khó forensics khi incident, vi phạm SOC2/ISO27001 baseline + `output-review-mandate.md` Security baseline.

Nếu provision EC2 mà không enforce F6 mitigation từ đầu, sau khi public DNS flip qua kc-app, attacker có thể bypass nginx middleware bằng cách connect thẳng `http://<ec2-public-ip>:4701/` → expose Next.js standalone không có production hardening (rate limit, CSRF defense, IP allowlist).

---

## Root Cause

EC2 security group default templates trong terraform (`infrastructure/terraform-aws/ec2-kh-backend.tf`) tạo SG cho kh-backend với rules public 22/80/443 + backend service ports. Khi copy-paste để tạo `ec2-kc-app.tf` mới cho FE self-host, dễ:
- (a) Quên restrict port 4701 → default 0.0.0.0/0
- (b) Quên thêm `description = "..."` cho từng rule
- (c) Không dùng security group self-reference cho internal port

Failure matrix F6 surface rủi ro này TRƯỚC khi terraform apply chạy, do đó cần codify mitigation thành gap để Bucket B agent (hoặc user manual) verify từng line SG trước khi apply.

---

## Proposed Fix

### Bước 1: Định nghĩa SG rules trong `infrastructure/terraform-aws/ec2-kc-app.tf`

```hcl
resource "aws_security_group" "kc_app" {
  name        = "kitehub-kc-app-sg"
  description = "FE self-host EC2 (kc-app) — nginx public + Next standalone internal"
  vpc_id      = var.vpc_id

  # Public: SSH (admin only, restrict to user IP nếu có thể)
  ingress {
    description = "SSH admin access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # narrow nếu có static IP
  }

  # Public: nginx HTTP (redirect to HTTPS)
  ingress {
    description = "nginx HTTP — redirects to 443"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Public: nginx HTTPS (TLS termination)
  ingress {
    description = "nginx HTTPS — TLS termination + reverse proxy to Next 4701"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Internal: Next.js standalone — SG self-reference, NOT public
  ingress {
    description     = "Next.js standalone — nginx loopback only"
    from_port       = 4701
    to_port         = 4701
    protocol        = "tcp"
    self            = true   # only same SG (nginx on same host)
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.default_tags, {
    Name = "kitehub-kc-app-sg"
  })
}
```

### Bước 2: Verify post-apply qua Tier 1 read-only commands

```bash
aws ec2 describe-security-groups --filters Name=group-name,Values=kitehub-kc-app-sg \
  --query 'SecurityGroups[].IpPermissions[].{Port:FromPort,Source:IpRanges[].CidrIp,Self:UserIdGroupPairs[].GroupId,Desc:Description}' \
  --output table
```

Expected output:
- 22/80/443 → CidrIp `0.0.0.0/0` + Description không rỗng
- 4701 → UserIdGroupPairs (self-reference) + Description không rỗng
- Mọi rule có `Desc` non-empty

### Bước 3: AWS verification artifact

Theo `.claude/rules/agent-aws-access.md` §5 + `.claude/rules/pre-mutation-state-check.md` §3, ship audit doc tại `documents/04-quality/audits/aws-verification/2026-MM-DD-wave-82-kc-app-sg-verification.md` với commands run + findings + verdict trước khi user trigger terraform apply.

---

## Acceptance Criteria

- [ ] File `infrastructure/terraform-aws/ec2-kc-app.tf` định nghĩa `aws_security_group.kc_app` với description trên mọi ingress/egress rule (ASCII only per `aws-sg-description-ascii.md`)
- [ ] Rule cho port 4701 dùng `self = true` (SG self-reference) — KHÔNG `cidr_blocks = ["0.0.0.0/0"]`
- [ ] Rule cho 22/80/443 public với description giải thích purpose
- [ ] Post-apply: `aws ec2 describe-security-groups --filters Name=group-name,Values=kitehub-kc-app-sg --query 'SecurityGroups[].IpPermissions[].Description'` trả về 4 description non-empty
- [ ] Post-apply: connect trực tiếp `curl -m 5 http://<ec2-public-ip>:4701/` từ máy ngoài timeout/refuse (không 200)
- [ ] Connect qua nginx `curl https://<fe-domain>/` trả về 200 + Next.js HTML
- [ ] AWS verification artifact ship trước terraform apply per `pre-mutation-state-check.md` §3

## Related

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §1 Brainstorm Q3 + §3 Bucket B (4 P0 mitigations identified)
- Failure-matrix finding: **F6** Wave 82 Bucket A outside-in audit 2026-05-15 (conversation artifact)
- Sister gaps: GAP-566 (F7 RAM tuning) · GAP-567 (F10 cert renewal) · GAP-568 (F11 BE CORS sweep)
- Rules: `.claude/rules/aws-sg-description-ascii.md` · `.claude/rules/agent-aws-access.md` §5 · `.claude/rules/pre-mutation-state-check.md` §3
- AWS verification artifacts pattern: `documents/04-quality/audits/aws-verification/` (existing examples Wave 64 Step E)

## Log

- **2026-05-15:** Gap filed via Wave 82 Bucket A outside-in failure-mode matrix audit (finding F6). P0 BLOCKING — phải address trước khi user trigger terraform apply tạo EC2 kc-app instance Bucket B. Mitigation = SG self-reference cho port 4701 + description ASCII cho mọi rule + AWS verification artifact pre-apply.
