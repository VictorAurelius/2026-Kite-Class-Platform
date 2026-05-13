# RabbitMQ Credential Sync Runbook — RC1 fix for GAP-502

**Audience:** SRE / DevOps thực hiện đồng bộ credential giữa `/etc/kite/.env` và RabbitMQ user definitions trong `kite-rabbitmq` container trên production EC2.
**Sister runbook:** ongoing rotation cadence — `documents/05-guides/operations/secrets-rotation-runbook.md`; first-time secrets seeding — `documents/05-guides/deploy/secrets-seeding-runbook.md`.
**Closes (PARTIAL):** Phase 1 (RC1) của [GAP-502](../../04-quality/gaps/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md) — RabbitMQ `AmqpAuthenticationException ACCESS_REFUSED` loop khiến container `kitehub-email` thrash trên production.
**Naming:** Per `.claude/rules/deployment-naming-convention.md` §2 — one-time pre-deploy / hotfix artifact, sống ở `deploy/`.
**Rules applied:** `agent-aws-access.md` (Tier 1 reads / Tier 3 mutations), `pre-mutation-state-check.md` §3 (audit artifact mandate), `concurrent-production-mutation-ops.md` (serialize với terraform / deploy workflows), `terraform-apply-retry-reconfirm.md` (per-mutation user re-confirm pattern).

---

## 1. Purpose + when to run

Đây là **one-time hotfix runbook** áp dụng khi production EC2 quan sát thấy `AmqpAuthenticationException: ACCESS_REFUSED` loop trong logs `kitehub-*` Spring Boot containers — containers restart liên tục, Spring context init fail tại RabbitMQ connection factory.

Triggered Wave 70 RC1 (2026-05-13): EC2 `i-05d7af46d01436b96` (account 906286017800, region `ap-southeast-1`) thrashing — `kitehub-email` log spam `com.rabbitmq.client.AuthenticationFailureException ACCESS_REFUSED`. Nghi ngờ drift giữa credential trong `/etc/kite/.env` (Spring qua `EnvironmentFile`) và RabbitMQ user definitions trong `kite-rabbitmq` (Mnesia). Drift phát sinh nếu Wave 67 seeding mismatch, rotation skip parity check (per `secrets-rotation-runbook.md` §5), hoặc PR đẩy `.env` một chiều.

Đây là **Option A — cred sync path** (per GAP-502 §"Proposed Fix → Phase 1"). Option B — `spring.rabbitmq.listener.autoStartup=false` defer — KHÔNG dùng vì user chọn fix gốc.

**Skip runbook nếu:** `ACCESS_REFUSED` chỉ trên ONE container (race condition, check rabbit ready trước); RabbitMQ broker down (fix broker trước, runbook giả định broker healthy); staging/dev (dùng dev secrets-management).

---

## 2. Pre-flight — đọc state hiện tại (Tier 1 read-only)

Tất cả lệnh dưới đây là Tier 1 read-only per `.claude/rules/agent-aws-access.md` §2 (`describe-*`, `list-*`, log reads). Agent được phép DRAFT lệnh; **user execute qua SSM SendCommand** vì `docker exec` trong production = Tier 3 mutation context (running shell ops on prod EC2). Output dùng để fill `## Commands run` section của audit artifact (§6).

### 2.1 Verify broker healthy + list rabbit users

```bash
# Send-command target: i-05d7af46d01436b96 (kh_backend EC2)
# Document name: AWS-RunShellScript
# Comment: "GAP-502 RC1 pre-flight rabbit users"

aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 pre-flight rabbit users" \
  --parameters 'commands=[
    "docker ps --filter name=kite-rabbitmq --format \"table {{.Names}}\\t{{.Status}}\"",
    "docker exec kite-rabbitmq rabbitmqctl list_users",
    "docker exec kite-rabbitmq rabbitmqctl status | grep -E \"^(Status|Listeners|Plugins|RabbitMQ version)\" | head -20"
  ]' \
  --region ap-southeast-1
```

Expected output shape:

| Phần | Expected |
|------|----------|
| `docker ps` cho rabbit | `kite-rabbitmq    Up X minutes (healthy)` — nếu KHÔNG healthy → STOP, fix broker trước |
| `list_users` | Table 2 cột: `user` + `tags` (administrator / management / monitoring / `[]`). Ghi lại tên user nào đang tồn tại. |
| `rabbitmqctl status` snippet | Confirm RabbitMQ version (3.13+ expected) + AMQP listener trên port 5672 |

### 2.2 List permissions cho mỗi user

Sau khi có danh sách user từ §2.1, query permissions per user:

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 pre-flight rabbit permissions" \
  --parameters 'commands=[
    "for u in $(docker exec kite-rabbitmq rabbitmqctl list_users -q | awk \"{print \\$1}\"); do echo \"=== $u ===\"; docker exec kite-rabbitmq rabbitmqctl list_user_permissions $u; done"
  ]' \
  --region ap-southeast-1
```

Expected output: per-user block listing `vhost / configure / write / read` (regex patterns). Cho mỗi production user cần consume, expected là `vhost=/ configure=.* write=.* read=.*` (full perms trên default vhost).

### 2.3 Read `.env` rabbit credentials (PII handling MANDATORY)

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 pre-flight env rabbit creds (REDACT password trước khi paste audit)" \
  --parameters 'commands=[
    "sudo grep -E \"^(RABBITMQ_|SPRING_RABBITMQ_)\" /etc/kite/.env | sed -E \"s/(PASSWORD|PASS|SECRET)=.*/\\1=<REDACTED>/\""
  ]' \
  --region ap-southeast-1
```

**PII handling rule:** lệnh trên ĐÃ redact password trong stdout — KHÔNG echo password sang audit artifact. Khi paste vào audit log, double-check không có dòng nào lộ password. Nếu cần raw value để compare, đọc local trong session terminal (không lưu repo).

Expected fields:
- `RABBITMQ_USERNAME` hoặc `SPRING_RABBITMQ_USERNAME` — tên user Spring sẽ auth
- `RABBITMQ_PASSWORD` (redacted) — mật khẩu Spring sẽ gửi
- `RABBITMQ_HOST` — thường `kite-rabbitmq` (container name) hoặc `localhost`
- `RABBITMQ_PORT` — thường `5672`
- `RABBITMQ_VHOST` — thường `/` (default)

Nếu Spring config dùng prefix `SPRING_RABBITMQ_*` (Spring Boot env-var binding) ưu tiên đọc các key đó; legacy `RABBITMQ_*` có thể chỉ là alias hoặc deprecated.

### 2.4 Confirm fail vẫn active

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 pre-flight confirm auth fail still active" \
  --parameters 'commands=[
    "docker logs --tail 100 kitehub-email 2>&1 | grep -iE \"AuthenticationFailure|ACCESS_REFUSED|AmqpAuthen\" | tail -20",
    "docker events --since 30m --until now --filter event=die --filter \"name=kitehub-\" 2>&1 | head -20"
  ]' \
  --region ap-southeast-1
```

Expected output:
- Multiple lines kiểu `AmqpAuthenticationException: ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN. For details see the broker logfile.` — confirm RC1 symptom active.
- Recent `die` events cho `kitehub-*` containers — confirm thrash pattern.

Nếu `kitehub-email` log đã clean (không còn auth fail trong 30 phút gần nhất) → có khả năng issue đã self-correct hoặc PR khác đã fix → STOP, document trong audit + flip GAP-502 RC1 sang DONE without sync per `audit-to-gap-pipeline.md` §2.8 fix-time state-check decision matrix.

---

## 3. Diagnose — apply decision tree

So sánh output §2.1 (rabbit users) với §2.3 (`.env` username), output §2.2 (permissions) với expected `vhost=/ configure=.* write=.* read=.*`, và confirm symptom §2.4.

### Case 1: User trong `.env` KHÔNG tồn tại trong rabbit `list_users`

Ví dụ: `.env` có `RABBITMQ_USERNAME=kitehub_prod` nhưng `list_users` chỉ có `guest` và `admin`.

→ **Path B (create user)** trong §4.

### Case 2: User tồn tại nhưng password mismatch (default assumption)

Ví dụ: `.env` có `RABBITMQ_USERNAME=kitehub_prod`, `list_users` có row `kitehub_prod`, nhưng auth vẫn fail → password trong rabbit ≠ password trong `.env`.

→ **Path A (change password)** trong §4. Đây là case mặc định nghi ngờ post-Wave 67 production seeding nếu rotation chạy không đầy đủ. **Preserve `.env` as source of truth** (less invasive — chỉ rabbit cần re-import password, không phải restart toàn bộ stack với env mới).

### Case 3: User exists + password match nhưng listener vẫn fail

Output §2.2 cho user đó có thể `vhost=/ configure="" write="" read=""` — permissions trống → auth thành công nhưng không có quyền publish/consume.

→ **Path C (grant permissions)** trong §4.

### Case 4: None of the above (ambiguous)

Ví dụ: log §2.4 hiển thị `ACCESS_REFUSED - Login was refused for vhost "/"` (vhost-level reject, không phải credential-level) → có thể user thiếu permission trên vhost `/` nhưng tồn tại trên vhost khác → Path C variant, hoặc rabbit definitions có conflict → escalate, KHÔNG mutate blindly.

→ STOP. Document findings vào audit, request second eyes (sister rule `feedback_sonnet_parallel_agent_crash.md` không cho phép guess production state).

---

## 4. Sync execution — Tier 3 mutation (user-trigger only)

Tất cả lệnh trong §4 đều là Tier 3 production mutations per `agent-aws-access.md` §4 (modify state on production resource). **Agent chỉ draft; user execute qua SSM SendCommand.** Per `terraform-apply-retry-reconfirm.md` §3, user-trigger MỖI lệnh — không bundle nhiều `rabbitmqctl` mutate vào 1 SendCommand mà không re-confirm.

**Concurrency check trước khi chạy bất kỳ command §4 nào** (per `concurrent-production-mutation-ops.md` §4):

```bash
gh run list --status in_progress --limit 20 --json name,databaseId,startedAt
```

Nếu có `terraform-apply.yml` hoặc `deploy-production.yml` đang chạy → **STOP**, đợi hoàn tất + verify EC2 `running` trước khi sync. RabbitMQ container có thể bị restart bởi cả 2 workflow đó (user_data change hoặc compose redeploy), conflict với `rabbitmqctl change_password` mid-execution.

### Path A — Change password (default — `.env` as source of truth)

Mục đích: re-import password từ `.env` vào rabbit user definition.

**Lưu ý:** đặt password trong SSM command sẽ log password ra CloudTrail SSM SendCommand history. Để tránh leak:
- Đọc password qua `sudo cat /etc/kite/.env | grep RABBITMQ_PASSWORD` trong cùng SSM session (password chỉ tồn tại trong shell environment).
- KHÔNG pass password literal trong `--parameters` JSON (đó là CloudTrail-logged).

Pattern an toàn:

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 Path A change_password kitehub_prod" \
  --parameters 'commands=[
    "set -e",
    "USER=$(sudo grep -E \"^RABBITMQ_USERNAME=\" /etc/kite/.env | cut -d= -f2- | tr -d \"\\\"\")",
    "PASS=$(sudo grep -E \"^RABBITMQ_PASSWORD=\" /etc/kite/.env | cut -d= -f2- | tr -d \"\\\"\")",
    "test -n \"$USER\" -a -n \"$PASS\" || { echo \"FATAL: empty user/password in .env\"; exit 1; }",
    "echo \"Changing rabbit password for user: $USER\"",
    "docker exec kite-rabbitmq rabbitmqctl change_password \"$USER\" \"$PASS\"",
    "echo \"Verify: rabbitmqctl authenticate_user (no-op if exists)\"",
    "docker exec kite-rabbitmq rabbitmqctl authenticate_user \"$USER\" \"$PASS\" && echo \"AUTH OK\" || echo \"AUTH FAIL\""
  ]' \
  --region ap-southeast-1
```

**Expected stdout (cleaned, password not echoed):**

```
Changing rabbit password for user: kitehub_prod
Verify: rabbitmqctl authenticate_user (no-op if exists)
AUTH OK
```

Nếu `AUTH FAIL` → password chứa special character bị shell mangle (vd `$`, `!`) → escape lại trong `.env` (quote `RABBITMQ_PASSWORD="..."` strictly) hoặc rotate password thành ASCII-safe per `aws-sg-description-ascii.md` analog principle.

### Path B — Create user (user không tồn tại)

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 Path B add_user kitehub_prod" \
  --parameters 'commands=[
    "set -e",
    "USER=$(sudo grep -E \"^RABBITMQ_USERNAME=\" /etc/kite/.env | cut -d= -f2- | tr -d \"\\\"\")",
    "PASS=$(sudo grep -E \"^RABBITMQ_PASSWORD=\" /etc/kite/.env | cut -d= -f2- | tr -d \"\\\"\")",
    "test -n \"$USER\" -a -n \"$PASS\" || { echo \"FATAL: empty user/password in .env\"; exit 1; }",
    "docker exec kite-rabbitmq rabbitmqctl add_user \"$USER\" \"$PASS\"",
    "docker exec kite-rabbitmq rabbitmqctl set_permissions -p / \"$USER\" \".*\" \".*\" \".*\"",
    "docker exec kite-rabbitmq rabbitmqctl set_user_tags \"$USER\" administrator",
    "docker exec kite-rabbitmq rabbitmqctl list_users | grep \"$USER\""
  ]' \
  --region ap-southeast-1
```

**Lưu ý:** `administrator` tag là full-rights — tổng thể quá rộng cho service user. Nếu muốn least-privilege, dùng `management` tag thay vì `administrator`. Per `release-deploy-standard.md` §3.1 Security, prefer least-privilege; nhưng nếu Wave 67 seed dùng `administrator` thì giữ nhất quán cho RC1, đề xuất tightening sang Phase 1.5 PAID hardening wave.

### Path C — Grant permissions only

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 Path C set_permissions kitehub_prod" \
  --parameters 'commands=[
    "set -e",
    "USER=$(sudo grep -E \"^RABBITMQ_USERNAME=\" /etc/kite/.env | cut -d= -f2- | tr -d \"\\\"\")",
    "test -n \"$USER\" || { echo \"FATAL: empty user in .env\"; exit 1; }",
    "docker exec kite-rabbitmq rabbitmqctl set_permissions -p / \"$USER\" \".*\" \".*\" \".*\"",
    "docker exec kite-rabbitmq rabbitmqctl list_user_permissions \"$USER\""
  ]' \
  --region ap-southeast-1
```

Expected stdout last line:

```
vhost  configure  write  read
/      .*         .*     .*
```

Tất cả 3 path đều chạy trong `kite-rabbitmq` container — không touch host filesystem ngoài việc đọc `/etc/kite/.env` (which is `kh_backend` host file mounted hoặc adjacent).

---

## 5. Restart Spring services + live verification gate

Sau khi sync xong (1 trong 3 path §4), restart kitehub services để Spring reconnect với credentials đã đồng bộ. RabbitMQ side đã có cred đúng; Spring side chỉ cần reset connection để re-auth.

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 restart kitehub services post cred sync" \
  --parameters 'commands=[
    "cd /opt/kite-prod",
    "docker compose -f docker-compose.production.yml restart kitehub-email kitehub-subscription kitehub-admin kitehub-branding kitehub-gateway",
    "sleep 60",
    "docker ps --filter name=kitehub- --format \"table {{.Names}}\\t{{.Status}}\""
  ]' \
  --region ap-southeast-1
```

**Expected after 60s sleep:** all 5 services show `Up X seconds (healthy)`. Nếu vẫn có service trong `Restarting` → đọc log của service đó:

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 verify Spring boot success" \
  --parameters 'commands=[
    "for s in kitehub-email kitehub-subscription kitehub-admin kitehub-branding kitehub-gateway; do echo \"=== $s ===\"; docker logs --tail 30 $s 2>&1 | grep -E \"Started [A-Za-z]+Application|Application run failed|AuthenticationFailure\" | tail -5; done"
  ]' \
  --region ap-southeast-1
```

Expected: mỗi service có dòng `Started <App>Application in N.NNN seconds` — KHÔNG có `Application run failed` hoặc `AuthenticationFailure`.

**10-minute hold gate:** wait 10 phút (per `release-deploy-standard.md` §4.3 post-deploy stabilization window), re-run `docker ps --filter name=kitehub-` — uptime phải tăng (không reset), zero restarts:

```bash
aws ssm send-command \
  --instance-ids i-05d7af46d01436b96 \
  --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 10min hold verification" \
  --parameters 'commands=[
    "docker ps --filter name=kitehub- --format \"table {{.Names}}\\t{{.Status}}\"",
    "docker events --since 10m --until now --filter event=die --filter \"name=kitehub-\" 2>&1 | wc -l"
  ]' \
  --region ap-southeast-1
```

Expected:
- 5/5 services Up với uptime ≥10 phút.
- `docker events ... | wc -l` returns `0` (zero die events trong 10 phút window).

Nếu một service crash trong 10 phút → KHÔNG flip RC1 DONE, file follow-up gap với log evidence và keep RC1 PARTIAL.

---

## 6. Pre-mutation audit artifact template

Per `.claude/rules/pre-mutation-state-check.md` §3, **TRƯỚC khi chạy §4 commands**, user PHẢI file artifact tại:

```
documents/04-quality/audits/aws-verification/YYYY-MM-DD-gap-502-rc1-cred-sync.md
```

Trong đó `YYYY-MM-DD` = ngày user thực sự execute (verify per `session-currentdate-check.md`).

Template tuân theo cấu trúc chuẩn từ `.claude/rules/pre-mutation-state-check.md` §3 (Scope + Commands run + Findings + Prior actions + Pending + Recommendations + References). GAP-502-specific fields cần fill:

- **Frontmatter:** `phase: wave-70`, `wave: 70`, `gaps: [GAP-502]`
- **Scope:** Cite EC2 `i-05d7af46d01436b96` + Path A/B/C chosen per §3 decision tree
- **Commands run:** Paste output của §2.1 / §2.2 / §2.3 (PASSWORD REDACTED) / §2.4 — bốn SSM SendCommand blocks
- **Findings → Diagnose verdict:** Rabbit users present, `.env` USERNAME, match Yes/No, auth fail symptom active Yes/No (log evidence), final Decision: Path A / B / C / Ambiguous
- **Findings → Real changes table:** 1 row mô tả mutation kế hoạch (resource = `kite-rabbitmq` user `<USER>`, action = change_password/add_user/set_permissions, root cause = drift giữa `.env` và rabbit definitions, risk = 60-120s Spring reconnect window)
- **Findings → Phantom updates:** N/A (cred sync = pure mutation, không có phantom drift)
- **Findings → Verdict:** Mutation intentional, scope hẹp, không touch terraform state / EC2 config; rabbit Mnesia persistence intact (queued messages preserved); kitehub-email DLQ có thể nhận messages trong window, flush sau Spring up
- **Prior actions:** Wave 67 seeding artifact (nếu exists) + GAP-502 filing + last secrets rotation
- **Pending:** §4 chosen-path command, §5 restart, §5 10-min hold, concurrent op check (per `concurrent-production-mutation-ops.md` §4 — `gh run list --status in_progress` showed ZERO `terraform-apply.yml` / `deploy-production.yml` active)
- **Recommendations:** Path A default; post-mutation curl health endpoint; watch DLQ growth 24h; review `secrets-rotation-runbook.md` §5 cho parity check meta-gap nếu Wave 67 là root cause
- **References:** Wave 70 plan, GAP-502, sister runbooks (`secrets-seeding-runbook.md` + `secrets-rotation-runbook.md`), 4 rules applied

**Mandate:** Artifact PHẢI tồn tại trên disk + commit trước khi user trigger §4. Skip → vi phạm `pre-mutation-state-check.md` §3 → backfill post-hoc + log violation trong `## Findings`.

---

## 7. Rollback

Tất cả rollback PHẢI re-run §5 verify gate sau khi áp dụng. Nếu Spring vẫn fail post-rollback → escalate Phase 2, keep RC1 PARTIAL.

### Path A rollback (change_password)

Nếu Path A change_password mà Spring vẫn fail auth (password trong `.env` cũng sai):

1. **Revert `.env` từ backup** (nếu có): `sudo cp /etc/kite/.env.bak.YYYY-MM-DD /etc/kite/.env && sudo chmod 600 /etc/kite/.env`, rồi re-run §4 Path A với .env đã rollback.
2. **Re-import password gốc từ AWS Secrets Manager** (nếu Wave 67 seed dùng): `aws secretsmanager get-secret-value --secret-id kitehub/production/rabbitmq-password --query SecretString --output text` → pipe vào `rabbitmqctl change_password` qua SSM (CloudTrail logs password — accept risk vì emergency).

### Path B rollback (delete newly-created user)

```bash
aws ssm send-command --instance-ids i-05d7af46d01436b96 --document-name AWS-RunShellScript \
  --comment "GAP-502 RC1 Path B rollback" \
  --parameters 'commands=["docker exec kite-rabbitmq rabbitmqctl delete_user kitehub_prod"]' \
  --region ap-southeast-1
```

**Cẩn trọng:** nếu Spring đã consume qua user đó, delete sẽ kill connection. Chỉ rollback Path B nếu Spring chưa connect (<2 phút post-create).

### Path C rollback (revoke permissions)

`docker exec kite-rabbitmq rabbitmqctl set_permissions -p / kitehub_prod "" "" ""` — clear permissions sẽ instantly kill consumer connections; chỉ dùng để quay về deny-all baseline để re-grant fine-grained.

---

## 8. Acceptance criteria (mirror GAP-502 §AC line for RC1)

- [ ] `docker logs kitehub-email --tail 100` trong 10 phút sau §5 KHÔNG có dòng `AmqpAuthenticationException` hoặc `ACCESS_REFUSED`.
- [ ] `docker logs <each kitehub-* service> --tail 50` cho thấy `Started <App>Application in N.NNN seconds` post-restart; KHÔNG có `Application run failed`.
- [ ] `docker ps --filter name=kitehub-` cho thấy cả 5 service (`kitehub-email`, `kitehub-subscription`, `kitehub-admin`, `kitehub-branding`, `kitehub-gateway`) trong status `Up X seconds (healthy)` với uptime ≥ 10 phút.
- [ ] `docker events --since 30m --until now --filter event=die --filter "name=kitehub-"` trả về ZERO entries trong 30-phút window post §5 restart.
- [ ] Audit artifact `documents/04-quality/audits/aws-verification/YYYY-MM-DD-gap-502-rc1-cred-sync.md` exists với đầy đủ sections (Scope + Commands run + Findings + Prior actions + Pending + Recommendations).
- [ ] GAP-502 file `Status` field flipped phù hợp: `🟢 DONE` nếu mọi AC trên satisfied, `🟡 PARTIAL` nếu một item failed hoặc tracked rollback (per `gap-done-discipline.md` §2 — không banned-phrase trong Log).
- [ ] CSV row `gap-status.csv` cho GAP-502 đồng bộ với status flip (per `post-merge-sync-completeness.md` §2 target 1).

---

## 9. Related

- **Parent gap:** [`documents/04-quality/gaps/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md`](../../04-quality/gaps/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md) — RC1 scope
- **Sister runbooks:**
  - [`documents/05-guides/deploy/secrets-seeding-runbook.md`](secrets-seeding-runbook.md) — initial cred provisioning (Wave 67 reference)
  - [`documents/05-guides/operations/secrets-rotation-runbook.md`](../operations/secrets-rotation-runbook.md) — ongoing rotation cadence (root cause if drift originated here)
- **Rules:**
  - `.claude/rules/agent-aws-access.md` — Tier 1 reads (§2) vs Tier 3 mutations (§4) — user-only execute
  - `.claude/rules/pre-mutation-state-check.md` §3 — audit artifact mandate (§6)
  - `.claude/rules/concurrent-production-mutation-ops.md` §4 — overlap check before §4 commands
  - `.claude/rules/terraform-apply-retry-reconfirm.md` — per-command user re-confirm pattern
  - `.claude/rules/deployment-naming-convention.md` §2 — file location justification
  - `.claude/rules/gap-done-discipline.md` §2 — AC checking trước flip
  - `.claude/rules/post-merge-sync-completeness.md` §2 — CSV sync target
- **Standards:** AWS Well-Architected Operational Excellence + Reliability pillars (per `release-deploy-standard.md` §2)

---

## 10. Log

- **2026-05-13** Runbook created as Wave 70 Bucket A deliverable for GAP-502 RC1 hotfix. Path A (change_password preserving `.env` as source of truth) là default; Path B (add_user) + Path C (set_permissions) covered cho edge cases. PII handling rules cho password reads + SSM CloudTrail logging caveat documented. Pre-mutation audit artifact template (§6) included to satisfy `pre-mutation-state-check.md` §3. AC mirrors GAP-502 RC1 line + adds CSV sync per `post-merge-sync-completeness.md`.
