# Pre-walk persona simulation — Flow KC-1 (Tenant provisioning + lifecycle + settings)

**Created:** 2026-06-04
**Flow:** KC-1 (auto-trigger từ KH-2b sau owner signup; spans tenant DB provisioning saga → first KC login → onboarding wizard → settings edit → lifecycle transitions)
**Scope:** Outside-in audit BEFORE G2 walk, per `pre-walk-persona-simulation-mandate.md` v1.0.0
**Source artifacts:**
- Business: `documents/01-business/kiteclass/{tenant-provisioning,tenant-settings,instance-lifecycle}/{rules,use-cases}.md`
- BE saga: `kiteclass/kiteclass-core/.../module/provisioning/TenantProvisioningSaga.java`, `module/instance/service/InstanceLifecycleService`
- BE infra: `kitehub/kitehub-subscription/.../service/DatabaseProvisioningService.java` (DB provisioning + Flyway)
- FE: chưa tồn tại tenant settings page trong `kiteclass-frontend` (xác minh khi walk)
- Campaign: `documents/03-planning/roadmap/flow-verification-campaign.md` row KC-1

---

## Personas walked

1. **P1 — Owner mới signup (Wave flow-kh1 outcome)** — Tuấn, vừa nhận provisioned tenant `kc-an-8.kitehub.me`, lần đầu vào `/admin`
2. **P2 — Owner có sẵn 1 tenant trial** — Linh, provision tenant thứ 2 (cross-tenant scope)
3. **P3 — Owner non-tech 50+, smartphone-Zalo first** — bác Hùng (chủ trung tâm tiếng Anh trẻ em, Mon-Sat lunar-aware), kỹ năng đọc tiếng Anh hạn chế
4. **P4 — Admin platform** — observer Tenant provisioning lifecycle, intervene khi job FAILED

---

## Persona 1: Owner mới signup (Tuấn)

### Finding 1.1: Provisioning saga là placeholder — KC tenant DB không thực sự được tạo

- **Where:** `TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core, line 83-86) chỉ log "infrastructure provisioning stub"; `DatabaseProvisioningService.lifecycleEnabled=false` mặc định (kitehub-subscription) → simulate, không tạo DB
- **Symptom:** Owner login vào `kc-an-8.kitehub.me/admin` → BE service dùng default datasource (master `kitehub`), KHÔNG có dedicated tenant DB → KC core queries dựa RLS + tenant_id column, NHƯNG nếu admin/middleware giả định "instance.databaseUrl exists" sẽ NPE / 500 trong gateway routing
- **Pre-walk check:**
  ```bash
  grep -rn "lifecycleEnabled\|database.lifecycle.enabled" kitehub/kitehub-subscription/src/main/resources/ kitehub/docker-compose.kitehub.yml
  curl -s http://localhost:8082/actuator/env | jq '.propertySources[].properties | with_entries(select(.key | test("lifecycle")))'
  # Expect: lifecycle-enabled=true cho real DB; ACTUAL likely false (stub mode)
  grep -rn "instance.databaseUrl\|getDatabaseUrl" kiteclass/kiteclass-core/src/main/java --include='*.java'
  ```
- **Severity:** P0 BLOCKING
- **Pattern class:** state-machine / provisioning incomplete

### Finding 1.2: Saga dùng TenantCreatedEvent direct method call — KH-2b auto-trigger chưa wire

- **Where:** `tenant-provisioning/rules.md` BR-PROV-011: "Saga invoked via direct method call in this Sub-PR; Spring @EventListener / RabbitMQ consumer wiring lands alongside the outbox RabbitMQ dispatcher in follow-up". Wave flow-kh1 G2 walk surfaced GAP-930 (EmailConsumer @Async mismatch) → same outbox dispatcher chưa fully wired
- **Symptom:** KH-2b signup hoàn tất → owner + Instance row tạo trong kitehub-platform DB → BUT KC saga KHÔNG được trigger (no event listener wired) → instance status `INITIALIZING` mãi mãi → Owner thấy `/admin` 404 hoặc spinner forever
- **Pre-walk check:**
  ```bash
  grep -rn "@RabbitListener.*tenant\|@EventListener.*TenantCreated" kiteclass/kiteclass-core/src/main/java --include='*.java'
  # Expect: ≥1 listener consuming tenant.created topic; ACTUAL: likely 0 per BR-PROV-011 "follow-up"
  # Check Instance status after KH-2b signup:
  docker exec kite-postgres psql -U kite -d kitehub -c "SELECT id, slug, status, database_url, created_at FROM instances ORDER BY created_at DESC LIMIT 3;"
  ```
- **Severity:** P0 BLOCKING (depends on KH-2b → KC-1 chain critical path per campaign §4)
- **Pattern class:** event-driven / saga wiring

### Finding 1.3: Không có "tenant đang setup, chờ N giây" UI feedback — Owner thấy spinner vô tận

- **Where:** Provisioning saga chạy ~3-30s (DB create + Flyway migrate + branding plan execute). UC-PROV-01 không define UI feedback contract. FE chưa có `/onboarding/provisioning` polling page
- **Symptom:** Owner click "Vào trung tâm của tôi" trong KH-2c → redirect `kc-<slug>.kitehub.me/admin` → BE saga vẫn GENERATING → admin page 503/redirect-loop → Owner refresh 5 lần → bỏ cuộc
- **Pre-walk check:**
  ```bash
  find kiteclass/kiteclass-frontend/src -name 'provisioning*' -o -name 'setup-progress*' 2>/dev/null
  # Expect: polling page với instance status; ACTUAL: likely missing
  grep -rn "instance.status\|GENERATING\|INITIALIZING" kiteclass/kiteclass-frontend/src --include='*.tsx' 2>/dev/null | head -5
  ```
- **Severity:** P1
- **Pattern class:** trust-signal / UX / state-machine UI surface

### Finding 1.4: Default branding hardcoded English "KiteClass" — vi phạm VN-localization audit checklist

- **Where:** `tenant-settings/rules.md` BR-SET-03: default display_name = `"KiteClass"`. BR-SET-04: tagline = `"Nen tang quan ly trung tam dao tao"` (no diacritics — UTF-8 mojibake risk hoặc cố tình ASCII)
- **Symptom:** Owner Tuấn vào admin lần đầu thấy header "KiteClass — Nen tang quan ly..." → cảm giác "đây là sản phẩm KiteClass, không phải trung tâm của tôi" → trust drop + confusion. Phải tự edit settings ngay turn 1 (high friction)
- **Pre-walk check:**
  ```bash
  grep -n "Nen tang\|KiteClass.*default\|display-name" kiteclass/kiteclass-core/src/main/java --include='*.java' -r | head
  # Verify diacritic-aware default OR pre-fill từ KH-2b signup data (tenant_name owner provided)
  ```
- **Severity:** P1
- **Pattern class:** i18n / VN-localization / trust-signal — should pre-fill từ owner signup (`tenantName` field) thay vì hardcoded "KiteClass"

### Finding 1.5: Slug conflict (UC-PROV-02) không có user-facing recovery path

- **Where:** UC-PROV-02 "Initiate Fails (Slug Conflict)" → saga rethrow → "Caller sees 400 from API layer". NHƯNG caller là KH-2b auto-trigger (no human UI). Email + DB row đã commit ở KH-1 chain. Now KC provisioning fails → Owner đã có tài khoản nhưng KHÔNG có tenant
- **Symptom:** Owner Tuấn login vào `kc-an-8.kitehub.me` → 404 (slug đã bị Linh xài 2 phút trước) → không có "slug conflict, vui lòng chọn slug khác" recovery flow → support ticket
- **Pre-walk check:**
  ```bash
  grep -rn "slug.*conflict\|SlugAlreadyExists\|duplicate.*slug" kiteclass/kiteclass-core/src/main/java --include='*.java'
  # Expect: explicit exception + retry mechanism; ACTUAL: BR-PROV-004 "initiate failure does NOT trigger compensation" → orphan owner
  # Check unique constraint at instance level:
  docker exec kite-postgres psql -U kite -d kitehub -c "\d instances" | grep -i unique
  ```
- **Severity:** P1
- **Pattern class:** state-machine edge / data-orphan / recovery

---

## Persona 2: Owner có sẵn 1 tenant trial (Linh)

### Finding 2.1: Không có tenant-switcher UI — Owner phải logout/login để switch

- **Where:** Per §2.7 `pre-handoff-self-test-completeness.md` Multi-tenant tenant-switch flow gap. Linh login vào KH `/dashboard` → thấy 1 tenant `english-center-a` → click "Tạo trung tâm mới" → KH-2 flow tạo `english-center-b` → KC saga provision → BUT redirect logic dùng JWT có `tenantId` claim của tenant cũ
- **Symptom:** Linh sau khi tạo tenant 2 → click "Vào trung tâm B" → gateway resolve tenant từ subdomain `kc-english-center-b.kitehub.me` → BUT JWT có tenantId của A → BE return 403 hoặc data leak A vào view B context
- **Pre-walk check:**
  ```bash
  grep -rn "tenantId.*claim\|JWT.*tenant" kitehub/kitehub-gateway/src/main/java --include='*.java' | head
  grep -rn "X-Tenant-Id\|tenant-switcher\|switchTenant" kitehub/kitehub-frontend/src/lib kitehub/kitehub-frontend/src/components 2>/dev/null | head
  # Expect: tenant picker post-login OR re-issue JWT khi switch; ACTUAL: likely subdomain-only without JWT swap
  ```
- **Severity:** P0 (cross-tenant data leak risk)
- **Pattern class:** auth / multi-tenant / JWT scope

### Finding 2.2: Branding cache contamination across tenants

- **Where:** BR-SET-13 `theme_config_json`, BR-INST-004 `brandingVersion` increments on DEPLOY. Nếu FE cache (Next.js ISR / SWR) keyed bằng `branding-key` mà KHÔNG include tenant slug → tenant B mượn theme tenant A
- **Symptom:** Linh tạo tenant B với primary-color `#FF0000` (đỏ — tone tiếng Anh trẻ em). Refresh tenant A → A bị thay theme đỏ vì cache hit. Owner mất uy tín với staff
- **Pre-walk check:**
  ```bash
  grep -rn "branding.*cache\|themeConfig.*ISR\|revalidate.*branding" kiteclass/kiteclass-frontend/src 2>/dev/null
  grep -rn "@Cacheable.*branding\|CacheKeyGenerator" kiteclass/kiteclass-core/src/main/java --include='*.java' | head
  # Expect: cache key includes tenantSlug/instanceId; ACTUAL: verify
  ```
- **Severity:** P1
- **Pattern class:** multi-tenant / cache / data-leak

### Finding 2.3: Tenant lifecycle DELETE không cascade verify trên tenant 2

- **Where:** Per `tenant-provisioning/rules.md` BR-PROV-005 "Compensation failure logged but never rethrown (best-effort)". `instance-lifecycle/rules.md` không cover SUSPEND/DELETE state-machine — chỉ NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING → FAILED loop
- **Symptom:** Linh muốn xoá tenant trial cũ → click "Đóng trung tâm A" → BE call DELETE → orphan data (DB row, MinIO bucket, DNS record, S3 logo) → 6 tháng sau bị PDPL audit hỏi "retain dữ liệu sau xoá account?"
- **Pre-walk check:**
  ```bash
  grep -rn "SUSPENDED\|DELETED\|TENANT_DELETE\|tenant.*deletion" kiteclass/kiteclass-core/src/main/java --include='*.java' | head
  grep -rn "kitehub/instance-lifecycle\|off-boarding/rules" documents/01-business/
  # Expect: full lifecycle FSM; ACTUAL: BR-INST-002 state machine missing SUSPEND/DELETE transitions per §1 rules.md
  ```
- **Severity:** P0 (PDPL Art 23 compliance — data retention)
- **Pattern class:** state-machine / compliance / cascade

### Finding 2.4: "Trial expires" countdown chỉ tính từ tenant đầu, không reset cho tenant 2

- **Where:** Trial subscription thường tied to Owner-level (not Instance-level) per `kitehub/trial-lifecycle/rules.md`. Owner có 2 tenants share 1 trial expiry → tenant 2 trial chỉ còn 3 ngày khi vừa tạo
- **Symptom:** Linh tạo tenant B day 11/14 trial → tenant B chỉ còn 3 ngày → cảm giác bị "cheated"
- **Pre-walk check:**
  ```bash
  grep -rn "trialExpiresAt\|trial_expires" kitehub/kitehub-subscription/src/main/java --include='*.java' | head
  # Check schema: trial scope = per-owner or per-instance?
  docker exec kite-postgres psql -U kite -d kitehub -c "\d subscriptions" 2>/dev/null
  docker exec kite-postgres psql -U kite -d kitehub -c "\d instances" 2>/dev/null | grep -i trial
  ```
- **Severity:** P1
- **Pattern class:** time-sensitive / business-rule clarity

### Finding 2.5: Concurrent provisioning từ same owner → race condition

- **Where:** Linh click "Tạo trung tâm B" → BR-PROV-001 saga starts. Trước khi saga complete, Linh click "Tạo trung tâm C" trong tab khác → 2 sagas chạy đồng thời cho cùng owner → DB connection pool exhaust HOẶC duplicate slug allocation race
- **Symptom:** 1 trong 2 saga fails với deadlock / unique constraint violation; orphan instance row INITIALIZING không transition được
- **Pre-walk check:**
  ```bash
  grep -rn "@Transactional\|isolation\|SERIALIZABLE" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/ 2>/dev/null
  grep -rn "rate.*limit\|throttle.*tenant.*creation" kitehub/kitehub-gateway/src/main/java 2>/dev/null | head
  ```
- **Severity:** P2
- **Pattern class:** concurrency / state-machine

---

## Persona 3: Owner non-tech 50+ (bác Hùng)

### Finding 3.1: Onboarding wizard không tồn tại — bác Hùng nhìn "blank admin" panic

- **Where:** Per Flow Verification Campaign KC-1 expected step 5 "Onboarding wizard (chọn school type / academic year / etc.)" — KHÔNG có evidence trong codebase (`grep -rn "OnboardingWizard\|onboarding-wizard" kiteclass-frontend` likely empty)
- **Symptom:** Bác Hùng login `kc-trung-tam-anh-ngu-be-yeu.kitehub.me/admin` → thấy empty dashboard "0 students, 0 classes, 0 teachers" → không có guided path → đóng tab → email support
- **Pre-walk check:**
  ```bash
  find kiteclass/kiteclass-frontend/src -name '*onboarding*' -o -name '*wizard*' -o -name '*setup*' 2>/dev/null | head
  # Compare với kitehub onboarding KH-2c which DOES exist
  ```
- **Severity:** P0 (P3 persona conversion blocker)
- **Pattern class:** UX / onboarding / persona-coverage

### Finding 3.2: VN academic year (tháng 9 → tháng 6) vs fiscal year (tháng 1 → 12) gây confusion

- **Where:** Default tenant settings không có "Niên khóa hiện tại" picker. `documents/01-business/kiteclass/academic-year/` exists nhưng tenant provisioning saga chưa wire default
- **Symptom:** Bác Hùng tạo lớp Tiếng Anh tháng 6/2026 → hệ thống auto-assign niên khóa 2025-2026 hay 2026-2027? Logic ambiguous → grade rollup sai khi xuất báo cáo cuối năm cho phụ huynh
- **Pre-walk check:**
  ```bash
  grep -rn "academic.*year.*default\|niên khóa\|nien khoa" kiteclass/kiteclass-core/src/main/java --include='*.java' | head
  cat documents/01-business/kiteclass/academic-year/rules.md 2>/dev/null | head -30
  ```
- **Severity:** P1
- **Pattern class:** cultural / VN-specific / business-rule

### Finding 3.3: Mobile-first admin missing — bác Hùng dùng phone không ngồi máy tính

- **Where:** 50%+ VN edu admin dùng phone (per persona simulation 2026-05-14 Wave 79). `kiteclass-frontend` admin layout chưa có evidence breakpoint <768px tested
- **Symptom:** Bác Hùng mở `kc-...kitehub.me/admin` trên Samsung A05 → sidebar nav cover 80% screen → settings form input đè nhau → bỏ cuộc
- **Pre-walk check:**
  ```bash
  grep -rn "md:\|lg:\|sm:" kiteclass/kiteclass-frontend/src/app --include='*.tsx' 2>/dev/null | wc -l
  # Compare với mobile-specific viewports trong Playwright config
  find kiteclass/kiteclass-frontend -name 'playwright.config*' -exec grep -l "viewport" {} \;
  ```
- **Severity:** P0
- **Pattern class:** mobile / accessibility / persona-coverage

### Finding 3.4: Logo upload (BR-SET-11) thiếu Zalo OA share preview

- **Where:** BR-SET-11 logo upload S3. NHƯNG bác Hùng marketing chính qua Zalo OA — cần preview "logo sẽ trông thế nào trên Zalo card share"
- **Symptom:** Upload logo 4MB PNG → S3 OK nhưng khi share Zalo → logo blur (Zalo cần 1200x630 OG image). Bác Hùng đổ lỗi "phần mềm KiteClass tệ"
- **Pre-walk check:**
  ```bash
  grep -rn "og:image\|opengraph\|zalo.*preview" kiteclass/kiteclass-frontend/src 2>/dev/null | head
  grep -rn "image.*resize\|thumbnail" kiteclass/kiteclass-core/src/main/java --include='*.java' | head
  # Expect: auto-generate 1200x630 variant; ACTUAL: likely raw upload only
  ```
- **Severity:** P2
- **Pattern class:** VN-specific / Zalo culture / file-upload

### Finding 3.5: Contact phone format validation (BR-SET-16 max 20 chars) cho phép format không chuẩn VN

- **Where:** BR-SET-16 "Contact phone — max 20 chars" — không validate Vietnamese phone format (`0xxxxxxxxx` 10 digits OR `+84xxx`)
- **Symptom:** Bác Hùng gõ `0974.567.890` (định dạng với dấu chấm cũ) → save OK → khi SMS notification (future) fail vì gateway expect `+84974567890`
- **Pre-walk check:**
  ```bash
  grep -rn "phone.*pattern\|vietnamesePhone\|VN.*regex" kiteclass/kiteclass-core/src/main/java --include='*.java' 2>/dev/null
  ```
- **Severity:** P3
- **Pattern class:** i18n / VN-cultural

---

## Persona 4: Admin platform observe + intervene

### Finding 4.1: FAILED instance không có admin "force retry" UI

- **Where:** UC-PROV-05 "Retry After Failure" — actor: "Admin / scheduled retry (future)". `lifecycle.retry()` exists nhưng không có HTTP endpoint exposed (`grep retry.*PostMapping` chưa thấy)
- **Symptom:** Admin platform thấy 5 instances FAILED 24h cuối → muốn manual retry → không có button trong `/admin/tenants` → phải SSH vào RDS UPDATE status='INITIALIZING' (nguy hiểm + audit invisible)
- **Pre-walk check:**
  ```bash
  grep -rn "/admin/.*retry\|@PostMapping.*retry" kitehub/kitehub-platform/src/main/java --include='*.java'
  grep -rn "lifecycle.retry\|retryProvisioning" kiteclass/kiteclass-core/src/main/java --include='*.java' | head
  ```
- **Severity:** P0
- **Pattern class:** admin-action / ops-readiness

### Finding 4.2: Provisioning saga log chỉ ở log files — không có structured event trong DB audit_log

- **Where:** `TenantProvisioningSaga.java` line 53-86 dùng `log.info(...)` toàn bộ. Không có `auditLog.recordProvisioningStep(...)` với REQUIRES_NEW propagation per `audit-service-isolation.md`
- **Symptom:** Admin platform debug "tại sao instance #42 fail tuần trước" → grep CloudWatch logs hết quota retention (Free Tier 5GB) → mất context. PDPL audit hỏi "ai approve tenant này lúc nào" → không có DB audit row
- **Pre-walk check:**
  ```bash
  grep -rn "auditLog\|AdminAudit\|provisioning_audit" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/ 2>/dev/null
  docker exec kite-postgres psql -U kite -d kitehub -c "\dt" 2>/dev/null | grep -i audit
  ```
- **Severity:** P1
- **Pattern class:** observability / compliance / audit-trail

### Finding 4.3: Compensation failure (BR-PROV-005) chỉ log warn — admin không biết để clean orphan

- **Where:** BR-PROV-005 "Compensation failure is logged but never rethrown (best-effort)". `compensate()` catch RuntimeException log error. KHÔNG emit alert / metric / dead-letter
- **Symptom:** Saga fail → markFailed compensation cũng fail (vd DB connection lost) → instance row stuck status `GENERATING` forever. Admin không nhận alert → 3 tháng sau audit "0 healthy instance metric drop" thì mới phát hiện
- **Pre-walk check:**
  ```bash
  grep -rn "compensation.*alert\|metrics.*provisioning_fail" kiteclass/kiteclass-core/src/main/java --include='*.java'
  # Verify CloudWatch alarm: tenant_provisioning_stuck_count > 0
  grep -rn "tenant_provisioning\|saga.*compensate" infrastructure/terraform-aws/ 2>/dev/null | head
  ```
- **Severity:** P0
- **Pattern class:** observability / state-machine / alerting

### Finding 4.4: Admin cannot inspect tenant DB state without prod credentials

- **Where:** Multi-tenant DB isolation (per `DatabaseProvisioningService`) creates per-instance DB with random password encrypted AES-256-GCM. Admin platform không có "view-as-tenant" UI
- **Symptom:** Owner Tuấn report "không thấy student tôi nhập tuần trước" → admin platform cần inspect tenant DB → phải call SRE → unsealed via KMS → 30 min response time → Owner đã giận
- **Pre-walk check:**
  ```bash
  grep -rn "/admin/.*tenant.*view-as\|impersonate" kitehub/kitehub-platform/src/main/java --include='*.java'
  grep -rn "EncryptionService.decrypt\|decryptPassword" kitehub/kitehub-subscription/src/main/java --include='*.java' | head
  ```
- **Severity:** P2
- **Pattern class:** admin-action / support-readiness

### Finding 4.5: Provisioning latency p99 không có SLO + dashboard

- **Where:** `provisioning.infrastructure.timeout-seconds: 120` (BR-PROV config). Nhưng không có CloudWatch metric `tenant_provisioning_duration_seconds` ship qua outbox
- **Symptom:** Admin platform không thể trả lời stakeholder "Owner đợi bao lâu trung bình để tenant ready?" → quyết định BizDev (free tier promotion) không có data
- **Pre-walk check:**
  ```bash
  grep -rn "tenant_provisioning_duration\|@Timed.*provision" kiteclass/kiteclass-core/src/main/java --include='*.java'
  # Check Micrometer / Prometheus integration
  curl -s http://localhost:8081/actuator/prometheus 2>/dev/null | grep -i provision
  ```
- **Severity:** P2
- **Pattern class:** observability / SLO / business-metric

---

## Aggregate (20 findings) — priority distribution

| Priority | Count | Concrete examples |
|---|---|---|
| 🔴 P0 BLOCKING | 8 | 1.1, 1.2, 2.1, 2.3, 3.1, 3.3, 4.1, 4.3 |
| 🟠 P1 | 8 | 1.3, 1.4, 1.5, 2.2, 2.4, 3.2, 4.2 (+ wave-deferred slug check) |
| 🟡 P2 | 3 | 2.5, 3.4, 4.4, 4.5 |
| 🟢 P3 | 1 | 3.5 |

## Pattern class breakdown

| Class | Findings |
|---|---|
| state-machine | 1.1, 1.5, 2.3, 4.3 |
| auth / multi-tenant | 2.1, 2.2 |
| i18n / VN-localization / cultural | 1.4, 3.2, 3.4, 3.5 |
| onboarding / UX / trust-signal | 1.3, 3.1, 3.3 |
| event-driven / saga wiring | 1.2 |
| observability / audit / SLO | 4.2, 4.3, 4.5 |
| admin-action / ops-readiness | 4.1, 4.4 |
| compliance (PDPL) | 2.3, 4.2 |
| time-sensitive / business-rule | 2.4 |
| concurrency / race | 2.5 |
| file-upload / Zalo culture | 3.4 |

## Top 3 BLOCKING priorities cho G2 walk

1. **Finding 1.2 (KH-2b → KC-1 saga wire)** — nếu listener chưa wire, walk dừng ở bước 1; cần verify trước khi spend stack-up time. Pre-walk command: check Spring container `@EventListener` registered cho `TenantCreatedEvent`
2. **Finding 1.1 (provisioning stub)** — verify `database.lifecycle.enabled` flag; nếu false, document expected behavior trong walk recipe (real DB not created, but app behavior continues via shared master DB)
3. **Finding 3.1 (onboarding wizard missing)** — primary persona (non-tech Owner) blocked at step "first login". Khả năng cao walk surface empty admin → confirm scope: KC-1 chỉ verify provisioning hay bao gồm onboarding wizard?

## Recommendation cho walk recipe

- Spawn walk với persona P1 (Tuấn) PRIMARY — clearest happy path scope
- P3 (bác Hùng) walk = SECONDARY (high signal cho conversion blocker)
- P2 + P4 sad-path scenarios = follow-up sessions (cross-tenant + admin-intervene scope rộng hơn KC-1)
- Pre-walk MUST run 4-script bundle per `pre-walk-static-audit-bundle.md` §2 + add: state-check `lifecycleEnabled` + `@EventListener` wiring per Finding 1.1 + 1.2

---

## Cross-link

- Sister pre-walk: `2026-06-04-pre-walk-flow-kh3-subscription.md`
- Campaign matrix: `documents/03-planning/roadmap/flow-verification-campaign.md` row KC-1
- Rule: `.claude/rules/pre-walk-persona-simulation-mandate.md` v1.0.0
- Business: `documents/01-business/kiteclass/tenant-provisioning/{rules,use-cases}.md`
