# Data Retention + Deletion Policy — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — section structure + retention matrix với TODO placeholder values; Phase 2 legal counsel review + config externalization via GAP-154 + GAP-108)
**Owner:** Legal + Engineering Lead
**Reviewer:** Legal counsel + DPO + Tax advisor (financial retention) + Operations (offboarding runbook)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-184 (Phase 1, Wave Legal-BRD 2026-04-29) → GAP-154 (Phase 2 content + legal sign-off) → GAP-108 (StorageCleanupScheduler hardcoded → externalize per matrix)
**Legal basis:** **Decree 13/2023/NĐ-CP Art 6** (data minimization, retention only as long as necessary), Art 11 (right to erasure); MOET Education Law 2019 (5y educational records); VN Tax Law 2019 + ND 123/2020/NĐ-CP (10y financial records); Cybersecurity Law 2018 (audit logs); Consumer Protection Law 2023 (support tickets); GDPR Art 5(1)(e) (storage limitation principle)
**Cross-cuts:** [privacy-policy.md](privacy-policy.md) (GAP-182 sibling skeleton in this wave — retention disclosures), GAP-186 (Child Protection — stricter retention for minors, planned), GAP-117 (restore drill — tests deletion completeness, planned), GAP-108 (config externalization, planned — see [`StorageCleanupScheduler`](../../kitehub/kitehub-subscription/src/main/java) hardcoded constants), GAP-093 (backup policy alignment, planned)

---

## 1. Phạm vi & nguyên tắc

Tài liệu này định nghĩa chu kỳ lưu trữ và quy trình xóa dữ liệu cho toàn bộ nền tảng KiteHub (SaaS quản lý) + KiteClass (multi-tenant giáo dục). **Skeleton Phase 1** chỉ thiết lập khung 8 sections + retention matrix với TODO placeholder values; nội dung pháp lý đầy đủ, sign-off từ legal counsel, và externalize config thuộc Phase 2 (GAP-154).

Nguyên tắc cốt lõi (theo **Decree 13/2023/NĐ-CP Art 6** — Personal Data Protection Law / PDPL):
- **Tối thiểu hóa dữ liệu (minimization):** chỉ thu thập + lưu giữ những gì cần thiết cho mục đích xử lý đã khai báo
- **Giới hạn thời gian (storage limitation):** xóa hoặc anonymize ngay khi mục đích kết thúc
- **Quyền xóa (right to erasure, Art 11):** chủ thể dữ liệu có quyền yêu cầu xóa, KiteHub phải đáp ứng trong thời hạn luật định
- **Tách biệt analytics khỏi PII:** dữ liệu thống kê đã anonymize có thể giữ vô thời hạn (xem §5)

Phạm vi áp dụng: tất cả dữ liệu khách hàng (tenant), người dùng cuối (giáo viên, phụ huynh, học sinh, nhân viên), backup, logs, AI generation outputs, support tickets. Không bao gồm: source code, infrastructure secrets, internal team chats (governed bởi nội bộ).

---

## 2. Retention Categories + Periods (matrix)

Bảng dưới là **placeholder Phase 1**. Mọi giá trị TODO sẽ được legal counsel + tax advisor xác nhận trong Phase 2 theo `business-logic-review.md` 5-attribute "informed gut" pattern (xem cột Phase 2 review).

| Data Category | Active retention | Post-termination | Legal basis | Config key | Phase 2 review |
|---|:-:|:-:|---|---|:-:|
| User accounts (active) | While tenant active | TODO 30d soft delete | Contract + PDPL Art 6 | `retention.user-account.days` | informed gut Q3 2026 |
| Educational records (grades, attendance, transcripts) | While tenant active | TODO 5y | MOET Education Law 2019 (record retention) | `retention.edu-records.years` | informed gut Q3 2026 |
| Financial records (invoices, payments, receipts) | While tenant active | TODO 10y | VN Tax Law (Luật Quản lý Thuế 2019, ND 123/2020/NĐ-CP e-invoice) | `retention.financial.years` | informed gut Q3 2026 |
| Audit logs (auth, admin actions, data access) | TODO 1y | TODO 1y post-termination | Cybersecurity Law 2018 + ND 53/2022/NĐ-CP | `retention.audit-log.days` | informed gut Q3 2026 |
| Marketing consent records | While consent active | TODO 3y | PDPL (consent traceability) | `retention.marketing-consent.years` | informed gut Q3 2026 |
| AI generation outputs (banners, hero, generated copy) | While instance active | TODO 30d | Service contract | `retention.ai-output.days` | informed gut Q3 2026 |
| Support tickets + chats | TODO 2y | TODO 2y | Consumer Protection Law 2023 (dispute window) | `retention.support.years` | informed gut Q3 2026 |
| Parent communication (SMS / Zalo / email logs) | TODO 2y | TODO 1y | PDPL + Consumer Protection | `retention.comm-logs.years` | informed gut Q3 2026 |
| Student sensitive (health absences, conduct, behavioral notes) | While enrolled | **TODO ≤6 months max** post-termination | PDPL minor data + Art 6 minimization (stricter cho trẻ em) | `retention.sensitive-minor.months` | informed gut Q3 2026 |

<!-- Phase 2: legal counsel + tax advisor to confirm — informed gut Q3 2026, GAP-154 -->

**Anti-pattern:** KHÔNG hardcode bất kỳ giá trị retention nào trong code. Hiện trạng `StorageCleanupScheduler.SOFT_DELETE_GRACE_PERIOD_DAYS = 30` là vi phạm — GAP-108 theo dõi externalize sang `application.yml` qua các config keys ở cột "Config key" trên.

---

## 3. Deletion Triggers

Bốn trigger chính khởi động pipeline xóa. Mỗi trigger phải tạo audit-trail entry (xem §8).

### 3.1 Subject erasure request (PDPL Art 11)
Chủ thể dữ liệu (user, parent, student) gửi yêu cầu xóa qua kênh chính thức (in-app, email DPO). Phải xử lý trong thời hạn luật định <!-- Phase 2: confirm exact PDPL deadline (likely 30 days) -->. Quy trình chi tiết — xem §7 Subject Erasure Request Runbook.

### 3.2 Retention period expiry
Scheduled job (`StorageCleanupScheduler` + sibling jobs) quét theo từng config key trong matrix §2; record vượt quá kỳ hạn được xếp vào hàng đợi xóa. Job phải idempotent + có dry-run mode để verify trước production.

### 3.3 Tenant termination
Khi tenant churn / chấm dứt subscription, chu trình post-termination retention bắt đầu (cột "Post-termination" trong §2). Quy trình chi tiết — xem §6 Tenant Offboarding Runbook.

### 3.4 Legal hold release
Khi legal hold (xem §4) được giải tỏa, dữ liệu thuộc phạm vi hold trở lại pipeline xóa thông thường — không tự động xóa ngay, mà tính lại thời điểm trigger gần nhất.

---

## 4. Deletion Process

### 4.1 Soft delete → hard delete timeline
Soft delete đặt `deleted_at` + index exclusion ngay; hard delete (DELETE row + cascade) sau **TODO 30d grace period** <!-- Phase 2: legal counsel confirm grace window per category — sensitive-minor có thể ≤7d, financial có thể giữ longer cho audit -->. Hiện trạng: `SOFT_DELETE_GRACE_PERIOD_DAYS = 30` hardcoded trong `StorageCleanupScheduler` — GAP-108 externalize.

Pipeline flow (skeleton — Phase 2 sẽ render thành sequence diagram):
1. Trigger fires (request / expiry / termination / hold release)
2. Mark soft-delete: set `deleted_at`, exclude khỏi search index + L1/Redis cache
3. Grace period (configurable per category): cho phép undelete nếu yêu cầu nhầm
4. Hard delete: DELETE row + cascade FK; emit `data.deleted` event vào audit log
5. Backup purge: next backup rotation cycle propagates deletion (xem §4.4)

### 4.2 Anonymization vs deletion (when to choose)
- **Delete (hard delete row):** dữ liệu PII không còn cần thiết, không có giá trị analytics đã anonymize riêng → xóa hoàn toàn
- **Anonymize (replace PII bằng hash / tombstone, giữ row):** khi cần giữ aggregated analytics (vd. số lượng học sinh dùng, conversion funnel) nhưng không còn quyền xử lý PII → thay email/phone/name bằng tombstone, giữ FK hợp lệ
- **Quy tắc:** ưu tiên anonymize nếu có giá trị business analytics; delete nếu không. PDPL Art 6 minimization yêu cầu chứng minh tính cần thiết của row tồn đọng.

### 4.3 Backup purge alignment
Backup retention phải align với retention chính (xem GAP-093). Khi hard delete fires, backup rotation cycle tiếp theo (full backup + incremental) phải KHÔNG chứa row đã xóa <!-- Phase 2: define cycle period — likely 30d full + 7d incremental -->. Restore drill (GAP-117) verify deletion completeness — restore từ backup cũ và confirm row đã xóa không reappear.

### 4.4 Search index invalidation
Mọi soft delete + hard delete phải emit invalidation event tới search indices (Elasticsearch / OpenSearch nếu có), removing record từ inverted index trong vòng <!-- Phase 2: SLA --> 5 phút. Pattern: outbox event → search index consumer.

### 4.5 Cache invalidation (Redis + L1)
- **Redis:** key TTL + explicit DEL cho mọi key chứa user/tenant data đã xóa
- **L1 (in-memory Caffeine / per-service):** broadcast invalidation event qua RabbitMQ; consumers evict local cache
- **CDN / static assets (AI outputs, uploaded logos):** purge từ MinIO + invalidate CloudFront/CDN edge caches

---

## 5. Legal Hold

Legal hold **override** retention clock — dữ liệu trong scope hold KHÔNG bị xóa cho đến khi hold release. Triggers:

- **Disputes:** tranh chấp với tenant / user mà KiteHub là một bên (vd. khiếu nại refund, breach of contract)
- **Investigations:** điều tra nội bộ (security incident, fraud) hoặc external (cảnh sát điều tra, regulator)
- **Regulatory inquiry:** yêu cầu chính thức từ MoET, Cục An toàn Thông tin, cơ quan thuế, etc.

### 5.1 Approval chain
Mọi legal hold phải có sign-off bằng văn bản:
1. **CEO + Legal counsel** đồng ký
2. Document hold scope: tenant ID, user ID, date range, data categories, reason
3. Lưu trong `documents/05-guides/legal-holds/` (Phase 2 — runbook + template)
4. Ghi audit log entry với reference tới hold document

### 5.2 Implementation
Hold flag set trên record / tenant / user. Scheduled deletion jobs phải skip mọi record có `legal_hold_id IS NOT NULL`. Hold release phải re-evaluate retention clock — nếu đã quá hạn, xóa ngay; nếu chưa, đợi đến trigger tiếp theo.

### 5.3 Exception: child data overrides
Theo PDPL minor data provisions, thậm chí dưới legal hold, dữ liệu nhạy cảm về trẻ em (health absences, conduct) phải tuân thủ §2 row "Student sensitive" — **TODO ≤6 months max** post-termination — trừ khi hold reason là điều tra trực tiếp về trẻ đó. Cross-link GAP-186.

---

## 6. Tenant Offboarding Runbook (skeleton stub)

Phase 1 stub — full SOP sẽ ship trong Phase 2 vào `documents/05-guides/operations/runbooks/tenant-offboarding.md` (GAP-154 + GAP-201 cross-cut).

Skeleton steps (Phase 2 sẽ điền chi tiết command + script paths):

1. **Confirm termination:** verify churn signal (subscription cancelled, contract end, manual termination); ghi termination date vào `kitehub.tenants` table.
2. **Notify tenant:** email thông báo lịch trình xóa dữ liệu theo matrix §2, đính kèm export option (data portability per PDPL Art 11).
3. **Data export (optional, on request):** generate ZIP archive theo format chuẩn — TODO định nghĩa schema export trong Phase 2.
4. **Soft-delete tenant resources:** mark tất cả tenant data soft-deleted; disable login; preserve trong grace window per matrix.
5. **Trigger retention countdown:** set `tenant.terminated_at`; scheduled jobs bắt đầu đếm post-termination kỳ hạn theo từng category.
6. **Hard delete + audit:** sau khi mọi category quá hạn (longest = financial 10y), hard delete tenant row + cascade; emit audit event `tenant.fully_purged`.
7. **Verify completeness:** chạy GAP-117 restore drill subset để confirm tenant data không xuất hiện trong backup chain mới.

Phase 2 deliverable: GAP-154 + GAP-201 sẽ produce file runbook chi tiết với CLI commands, rollback procedure, edge cases (tenant với active legal hold, tenant với pending dispute).

---

## 7. Subject Erasure Request Runbook (skeleton stub)

Phase 1 stub — full SOP sẽ ship trong Phase 2 vào `documents/05-guides/operations/runbooks/subject-erasure-request.md` (GAP-154 cross-cut).

Skeleton steps:

1. **Receive request:** intake qua kênh chính thức (in-app form / email DPO / hotline). Validate identity của requester (PDPL Art 11 yêu cầu xác minh).
2. **Classify scope:** xác định request áp dụng cho scope nào — toàn bộ user data, một loại data category, hoặc field cụ thể (vd. chỉ marketing consent).
3. **Check legal hold conflicts:** nếu user / scope đang trong legal hold (§5), từ chối hợp pháp + giải thích lý do; ghi audit; thông báo requester thời điểm dự kiến hold release.
4. **Execute deletion:** áp dụng pipeline §4 (soft → grace → hard delete + cache + index + backup); với scope toàn bộ user, cascade qua mọi tenant relationship.
5. **Anonymize residual analytics:** với row có giá trị aggregated analytics, replace PII bằng tombstone thay vì delete (xem §4.2).
6. **Notify requester:** confirm hoàn tất trong thời hạn luật định <!-- Phase 2: confirm SLA --> 30 days; cung cấp report tóm tắt actions đã thực hiện.
7. **Audit log entry:** ghi đầy đủ §8 fields (what / when / why / by whom + request reference ID).

Phase 2 deliverable: GAP-154 sẽ produce file runbook với template form intake, identity verification checklist, rejection grounds, escalation path khi conflict với legal hold.

---

## 8. Audit Trail of Deletions

Mọi deletion (soft + hard) phải ghi audit log entry không thể xóa hoặc sửa (immutable / append-only). Required fields:

| Field | Mô tả | Ví dụ |
|-------|-------|-------|
| `what` | Resource đã xóa: type + ID + scope | `user_id=12345, scope=full_account` |
| `when` | ISO-8601 UTC timestamp với millisecond | `2026-04-29T14:32:17.481Z` |
| `why` | Trigger reason theo §3 | `subject_erasure_request` / `retention_expiry` / `tenant_termination` / `legal_hold_release` |
| `by_whom` | Actor: system job, admin user, or DPO on behalf of subject | `actor=system:storage-cleanup-scheduler` / `actor=admin:dpo@kitehub` |
| `request_ref` | Optional: reference ID khi từ erasure request | `erasure_req=ER-2026-0429-001` |
| `legal_hold_check` | Boolean: hold check đã chạy + kết quả | `hold_check=passed` / `hold_check=blocked, hold_id=LH-001` |

### 8.1 Audit log retention
Audit trail của deletions tự nó tuân thủ retention matrix §2 row "Audit logs" — **TODO 1y active + 1y post-termination**. Audit log lifecycle phải đặc biệt CHẶT vì nó là evidence cho compliance — không được xóa sớm.

### 8.2 Storage + tamper-resistance
- Lưu trong dedicated audit log table với write-only permission cho service accounts
- Cross-link với `logs-format-standard.md` cho schema (timestamp / level / service / message / contextual fields)
- Phase 2: append-only S3 bucket với object lock (compliance mode) cho long-term archive

### 8.3 Reporting
Quarterly report tổng hợp số lượng deletion theo trigger type — đầu vào cho compliance audit (GAP-156) + privacy regulator inquiry. Report format: TODO Phase 2.

---

## 9. Phase 2 mở rộng (tham chiếu)

Phase 1 (file này) shipped trong Wave Legal-BRD 2026-04-29 với 8 sections + retention matrix skeleton. Phase 2 mở rộng (theo dõi qua GAP-154 + GAP-108) bao gồm:

- Legal counsel + tax advisor + DPO sign-off cho mọi giá trị TODO trong matrix
- Externalize hardcoded retention constants — GAP-108 (`StorageCleanupScheduler` + sibling jobs)
- Backup rotation cycle alignment — GAP-093
- Restore drill verifying deletion completeness — GAP-117
- Stricter retention cho minor data — GAP-186 (PDPL minor provisions + KidsSafety / Child Protection cross-cut)
- Privacy Policy retention disclosures phải mirror matrix này — GAP-182 (sibling skeleton trong wave này)
- Full Tenant Offboarding Runbook + Subject Erasure Request Runbook — `documents/05-guides/operations/runbooks/` (GAP-154 + GAP-201)
- First quarterly audit — Q3 2026 (GAP-156 cadence)

**Quy tắc Phase 2 sign-off:** mọi giá trị TODO trong matrix §2 phải có evidence theo `business-logic-review.md` 5-attribute (Source / Rationale / Reviewer / Compliance check / Review cadence) trước khi xóa marker `<!-- Phase 2: ... -->`.
