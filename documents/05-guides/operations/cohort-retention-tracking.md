# Cohort Retention Tracking — D7/D14/D30 framework Phase 1 BETA (Wave 86 Bucket H H-AC8)

**Owner:** Solo dev / Product
**Created:** 2026-05-16 (Wave 86 Bucket H H-AC8)
**Last Updated:** 2026-05-16
**Related gap:** GAP-591 P1
**Phase scope:** Phase 1 BETA (5 cohort tenant) — spreadsheet manual; dashboard automation = Phase 1.5+

---

## 1. Bối cảnh

Per Wave 86 Bucket A benchmark-vn-saas-edu Q2: **70% churn xảy ra trong 90 ngày đầu** với VN edu SaaS. Phase 1 BETA cần baseline retention metrics để:

- Phát hiện sớm activation cliff (tenant signup nhưng không dùng)
- Trigger outreach kịp thời (Zalo OA contact tenant ở risk)
- Validate product-market-fit assumption Phase 1 BETA → Phase 1.5 paid

**Framework chuẩn industry:** Mixpanel/Amplitude/Heap dùng **D7 / D14 / D30** milestone retention. Phase 1 BETA tracking manual qua spreadsheet (5 cohort tenant); Phase 1.5+ tự động via dashboard (Metabase / Grafana / custom).

---

## 2. Định nghĩa metric

### 2.1 D7 Activation Rate

**Cohort definition:** tenant signup tuần X (Monday-Sunday window).

**D7 active:** trong vòng 7 ngày sau signup, tenant đã:
- Tạo ≥ 1 lớp học (`POST /api/v1/classes`)
- VÀ mời ≥ 1 student/parent (`POST /api/v1/invitations`)

**Formula:**

```
D7_activation = (# cohort tenant đạt D7 active) / (# cohort tenant signup)
```

**Phase 1 BETA target:** ≥ 60% D7 activation (3/5 cohort tenant).

**Trigger nếu D7 < 50%:**
- Zalo OA outreach (per `support-sla-phase-1-beta.md`)
- 1-on-1 onboarding call để identify blocker
- Update user manual với pain point detected

### 2.2 D14 Active

**D14 active:** trong tuần 2 (ngày 8-14 sau signup), tenant có ≥ 1 login session (verify via `last_login_at` timestamp).

**Formula:**

```
D14_active = (# cohort tenant có login tuần 2) / (# cohort tenant đạt D7 active)
```

**Phase 1 BETA target:** ≥ 80% D14 active (4/5 tenant đã activate stay active).

**Trigger nếu D14 < 70%:**
- Email follow-up "Bạn còn cần hỗ trợ gì không?"
- Check support ticket lịch sử cho tenant (per SLA tracking)

### 2.3 D30 Active

**D30 active:** trong tuần 4 (ngày 22-30 sau signup), tenant có:
- ≥ 1 login session, HOẶC
- Churn reason captured (offboarding survey, support email "không phù hợp")

**Formula:**

```
D30_active = (# cohort tenant active tuần 4 + # tenant có churn reason captured)
            / (# cohort tenant đạt D14 active)
```

**Phase 1 BETA target:** ≥ 70% D30 active (paid conversion proxy).

**Trigger nếu D30 < 60%:**
- Re-evaluate Phase 1.5 pricing model
- Persona review (per `quality/persona-based-business-review`)
- Wave plan persona-specific gap files

---

## 3. Phase 1 BETA tracking — spreadsheet manual

5 cohort tenant. Spreadsheet columns:

| Column | Type | Example |
|---|---|---|
| `cohort_week` | YYYY-Www | 2026-W21 |
| `tenant_id` | UUID | abc123-... |
| `tenant_name` | string | "Trung tâm Anh ngữ Sky" |
| `signup_date` | YYYY-MM-DD | 2026-05-19 |
| `d7_active` | bool | true |
| `d7_classes_created` | int | 2 |
| `d7_invitations_sent` | int | 8 |
| `d14_active` | bool | true |
| `d14_login_count` | int | 4 |
| `d30_active` | bool | false |
| `d30_churn_reason` | string | "Chuyển sang Misa" / "" |
| `notes` | string | Outreach log |

**Location:** `documents/04-quality/metrics/phase-1-beta-cohort-retention.csv` (UTF-8 BOM per `test-artifact-format-standard.md` §3.2). XLSX render via `scripts/render-cohort-retention-xlsx.sh` (deferred Phase 1.5+).

---

## 4. Data extraction queries

### 4.1 D7 activation check

```sql
-- For each cohort tenant signed up tuần W, check D7 active
WITH cohort AS (
  SELECT id, name, created_at
  FROM tenants
  WHERE created_at BETWEEN :week_start AND :week_end
),
classes_count AS (
  SELECT tenant_id, COUNT(*) AS n_classes
  FROM classes
  WHERE created_at BETWEEN :week_start AND :week_start + INTERVAL '7 days'
  GROUP BY tenant_id
),
invitations_count AS (
  SELECT tenant_id, COUNT(*) AS n_invitations
  FROM invitations
  WHERE created_at BETWEEN :week_start AND :week_start + INTERVAL '7 days'
  GROUP BY tenant_id
)
SELECT
  c.id,
  c.name,
  c.created_at,
  COALESCE(cc.n_classes, 0) AS d7_classes,
  COALESCE(ic.n_invitations, 0) AS d7_invitations,
  CASE
    WHEN COALESCE(cc.n_classes, 0) >= 1 AND COALESCE(ic.n_invitations, 0) >= 1
    THEN true ELSE false
  END AS d7_active
FROM cohort c
LEFT JOIN classes_count cc ON cc.tenant_id = c.id
LEFT JOIN invitations_count ic ON ic.tenant_id = c.id;
```

### 4.2 D14/D30 active check

```sql
-- Login activity in weeks 2-4
SELECT
  t.id,
  t.name,
  t.created_at AS signup,
  MAX(CASE
    WHEN s.created_at BETWEEN t.created_at + INTERVAL '8 days'
                          AND t.created_at + INTERVAL '14 days'
    THEN s.created_at END) AS d14_last_login,
  MAX(CASE
    WHEN s.created_at BETWEEN t.created_at + INTERVAL '22 days'
                          AND t.created_at + INTERVAL '30 days'
    THEN s.created_at END) AS d30_last_login,
  COUNT(DISTINCT CASE
    WHEN s.created_at BETWEEN t.created_at + INTERVAL '8 days'
                          AND t.created_at + INTERVAL '14 days'
    THEN DATE(s.created_at) END) AS d14_login_days,
  COUNT(DISTINCT CASE
    WHEN s.created_at BETWEEN t.created_at + INTERVAL '22 days'
                          AND t.created_at + INTERVAL '30 days'
    THEN DATE(s.created_at) END) AS d30_login_days
FROM tenants t
LEFT JOIN user_sessions s ON s.tenant_id = t.id
WHERE t.created_at BETWEEN :cohort_start AND :cohort_end
GROUP BY t.id, t.name, t.created_at;
```

**SSM tunnel:** per `stack-on-demand-runbook.md` — SSH tunnel to RDS, run psql.

---

## 5. Cadence & ownership

### 5.1 Weekly review (every Monday)

- **Owner:** Solo dev
- **Effort:** 30 phút
- **Steps:**
  1. SSM tunnel → psql `kite-postgres`
  2. Run §4.1 D7 query for cohort signup last Mon-Sun
  3. Run §4.2 D14/D30 for older cohort
  4. Update spreadsheet `phase-1-beta-cohort-retention.csv`
  5. Check trigger thresholds (§2.1/§2.2/§2.3)
  6. File outreach actions trong Wave plan §🚀 Next Action

### 5.2 Monthly summary (1st of month)

- **Owner:** Solo dev
- **Effort:** 1h
- **Steps:**
  1. Aggregate 4 weekly snapshots
  2. Compute monthly D7/D14/D30 averages
  3. Identify trends (cohort N vs N-1 vs N-2)
  4. Update ROADMAP §🎯 với insights
  5. Adjust Phase 1.5 pricing/scope nếu cần

---

## 6. Outreach playbook

### 6.1 D7 not active (after day 7)

**Trigger:** spreadsheet row có `d7_active=false`

**Action template:**

```
Subject: KiteHub - Cần hỗ trợ setup không?

Chào anh/chị {tenant_owner_name},

Em thấy mình đã đăng ký KiteHub được 1 tuần, nhưng chưa tạo lớp học
hay mời học sinh. Có vướng mắc gì em hỗ trợ được không?

Em có thể:
- Setup mẫu 1 lớp giúp anh/chị (15 phút)
- Hướng dẫn import danh sách HS qua Excel
- Zalo call 1-on-1 nếu tiện

Reply email này hoặc Zalo: {zalo_link}

Cảm ơn anh/chị!
- Đội KiteHub
```

### 6.2 D14 not active

**Trigger:** `d7_active=true AND d14_active=false`

**Action template:**

```
Subject: KiteHub - Mình muốn nghe phản hồi từ anh/chị

Chào anh/chị {tenant_owner_name},

KiteHub đang trong giai đoạn beta và phản hồi của anh/chị rất quý giá.
Anh/chị có gặp khó khăn gì khi sử dụng không?

Nếu KiteHub chưa đáp ứng được nhu cầu, em xin phép hỏi:
- Tính năng nào còn thiếu?
- So với giải pháp khác (Misa, sổ tay) thì điểm gì chưa ổn?

Phản hồi của anh/chị giúp em cải thiện sản phẩm cho cộng đồng giáo dục.

Cảm ơn anh/chị!
```

### 6.3 D30 churn captured

**Trigger:** `d30_active=false AND notes có churn signal`

**Action template:**

```
Subject: KiteHub - Cảm ơn anh/chị đã thử

Chào anh/chị,

Em rất tiếc khi biết KiteHub chưa phù hợp với trung tâm hiện tại.
Em xin phép hỏi 1 câu cuối để học hỏi:

Lý do chính khiến anh/chị không tiếp tục dùng KiteHub là gì?
(a) Tính năng thiếu
(b) Giá / mô hình kinh doanh
(c) Khó dùng / chậm
(d) Khác

Reply 1 chữ cái đủ, em không làm phiền lâu.

Chúc anh/chị thuận lợi với giải pháp mới!
```

---

## 7. Phase 1.5+ automation roadmap

Khi tenant count > 20 hoặc cohort > 4 tuần/tháng → manual spreadsheet không scale.

**Phase 1.5 target:**

- **Metabase dashboard** (open-source BI) connect RDS read replica
- Auto-compute D7/D14/D30 charts
- Email digest weekly (gửi spreadsheet snapshot)
- Slack/email alert khi D7 < threshold

**Phase 2 target:**

- Amplitude / Mixpanel integration (event tracking)
- Cohort funnels (signup → first class → first invitation → first attendance taken)
- Predictive churn ML model (Phase 3 scope)

---

## 8. Related

- Gap: GAP-591 P1 (Wave 86 Bucket H H-AC8)
- Wave 86 Bucket A audit: benchmark-vn-saas-edu Q2 (70% 90-day churn baseline)
- Sister docs: `support-sla-phase-1-beta.md`, `beta-invite-flow.md`
- Rules: `dev-readable-doc-language.md`, `test-artifact-format-standard.md` §3.2 (CSV format)
- Future: Metabase deploy runbook (Phase 1.5+ scope)
