---
title: Đợt 106 trước RST — Khảo sát trạng thái 80 gap PARTIAL Pha 1 BETA
status: complete
created: 2026-05-23
phase: phase-1-beta
wave: 106
gaps: [GAP-612, GAP-379, GAP-502, GAP-534, GAP-535, GAP-536, GAP-658]
audience: dev
---

# Đợt 106 trước RST — Khảo sát trạng thái 80 gap PARTIAL Pha 1 BETA

**Ngày:** 2026-05-23
**Quy tắc áp dụng:** `audit-to-gap-pipeline.md` §2.8 Fix-Time State-Check
**Phương pháp:** 5 Explore agent song song theo miền, đọc CHỈ phần Problem + AC + Log gần đây của mỗi file gap, kiểm tra empirical qua grep/find/ls trên mã hiện tại.

## 1. Phạm vi

Tổng 80 gap PARTIAL phân theo miền:

| Miền | Số gap | Đường dẫn danh sách |
|---|:---:|---|
| Backend | 17 | `/tmp/wave-106-partial-backend.txt` |
| DevOps | 28 | `/tmp/wave-106-partial-devops.txt` |
| Frontend | 8 | `/tmp/wave-106-partial-frontend.txt` |
| Meta | 5 | `/tmp/wave-106-partial-meta.txt` |
| Mixed | 22 | `/tmp/wave-106-partial-mixed.txt` |

## 2. Tổng hợp phán quyết theo agent

| Miền | LIKELY_DONE | STILL_PARTIAL | AWS_BLOCKED | Cần xem lại |
|---|:---:|:---:|:---:|:---:|
| Backend | 7 | 10 | 0 | 0 |
| Frontend | 5 | 3 | 0 | 0 |
| DevOps | 11 | 15 | 2 | 0 |
| Meta | 1 | 3 | 0 | 1 |
| Mixed | 8 | 13 | 1 | 0 |
| **Tổng** | **32 (40%)** | **44 (55%)** | **3 (4%)** | **1 (1%)** |

## 3. Quyết định — KHÔNG flip gap nào lên DONE

Sau khi xem chi tiết 6 P0 LIKELY_DONE (GAP-379 / 502 / 534 / 535 / 536 / 658), **cả 6 đều KHÔNG đáp ứng `gap-done-discipline.md` §2 tiêu chí 1** ("mọi `- [ ]` checkbox trong Acceptance Criteria PHẢI là `- [x]`"):

| Gap | AC đã tick | AC chưa tick | Phần thiếu |
|---|:---:|:---:|---|
| GAP-379 (Secrets rotation) | 9/9 | 0 | Log nêu rõ "awaits user-triggered terraform apply + AWS console RDS bootstrap" — AWS bị tạm dừng chặn |
| GAP-502 (RabbitMQ thrashing) | 0/7 | 7 | Log chốt rõ "DONE flip phải defer 14-day observation post-Wave-85-deploy" |
| GAP-534 (Invite token single-use) | 7/8 | 1 | Live verify post-deploy chưa chạy — AWS chặn |
| GAP-535 (Tenant slug VN) | 6/8 | 2 | Wiring vào `InstanceService.createInstance` chưa làm |
| GAP-536 (Tenant idempotency) | 6/9 | 3 | HandlerInterceptor + FE debounce + live verify chưa làm |
| GAP-658 (VN sample seed) | 0/7 | 7 | Foundation 80% shipped; copywriter pass + service wiring chưa làm |

**Phân tích sâu:** Phán quyết "LIKELY_DONE" của agent dựa trên hiện diện file/class/migration trong mã. Nhưng AC của 6 gap này có dòng cuối cùng = "live verify post-deploy" — đây chính là cảnh báo của `pre-handoff-self-test-completeness.md` §1: **"Curl trả 201" ≠ "user đi được luồng".**

→ 32 LIKELY_DONE thực chất là **32 gap có code-shape ready, chờ AWS khôi phục + walk thực** — KHÔNG phải 32 gap đóng được.

## 4. Nguyên nhân gốc

3 yếu tố cộng hưởng:

1. **GAP-612 (AWS bị tạm dừng) chặn live verify** — 134+ giờ kể từ khi báo lỗi 2026-05-17, chưa có phản hồi từ AWS Support. Mọi gap có AC dạng "live verify post-deploy on production endpoint" đều không đóng được cho đến khi tài khoản được phục hồi.

2. **AC viết theo chuẩn `pre-handoff-self-test-completeness.md`** — quy tắc này là ĐÚNG; không cho phép flip DONE chỉ vì có file/class. Đảm bảo tester beta nhận sản phẩm thực sự đi được, không chỉ "có cấu trúc".

3. **Agent Explore khảo sát theo file presence** — phương pháp này hữu ích cho khảo sát nhanh quy mô lớn, nhưng KHÔNG thay thế được live walk thực tế. Phương pháp đúng: agent chỉ ra "code-ready, chờ live walk" thay vì "LIKELY_DONE".

## 5. Tác động lên backlog Pha 1 BETA

| Trạng thái | Trước Đợt 106 | Sau khảo sát (KHÔNG flip) | Δ |
|---|:---:|:---:|:---:|
| OPEN | 100 | 100 | — |
| PARTIAL | 81 | 81 | — |
| DONE | 128 | 128 | — |
| **Tỷ lệ hoàn thành** | **41%** | **41%** | **0** |

**Backlog không giảm** — nhưng **kiến thức về backlog tăng đáng kể**:
- 32 gap **đã sẵn code, chờ AWS restore** (40% backlog PARTIAL có thể đóng nhanh khi AWS phục hồi)
- 44 gap **vẫn cần làm thật** (55%) — đây là phạm vi Đợt 107 sửa lỗi
- 3 gap **bị chặn AWS** không phụ thuộc code (4%) — cần defer chính thức
- 1 gap **cần xem lại** ngữ cảnh (1%)

## 6. Phán quyết per-gap (cho Đợt 107 ưu tiên)

### 6.1 32 gap LIKELY_DONE (code-ready, chờ live walk hậu-AWS)

| Mã | Ưu tiên | Miền | Tóm tắt code-shape |
|---|:---:|---|---|
| GAP-127 | P0 | Frontend | Bundle analyzer + dynamic() đã wired |
| GAP-197 | P0 | Frontend | enhanced-attendance-calendar.tsx 315 LOC + tests |
| GAP-269b | P1 | Frontend | StudentPortalController + 5 endpoint + IT tests |
| GAP-471 | P1 | Frontend | Security headers (CSP + X-Frame) wired cả 2 app |
| GAP-599 | P1 | Frontend | sessionStorage migration 72 hits + two-tab test |
| GAP-033 | P1 | Backend | BrandingVersion entity + Service + Controller + V41 |
| GAP-405 | P2 | Backend | critical-screens.spec.ts với 8 visual regression tests |
| GAP-520 | P1 | Backend | JwtKeyService dual-key + runbook |
| GAP-534 | P0 | Backend | Invite token consumeInviteToken @Modifying query + 6 tests |
| GAP-535 | P0 | Backend | TenantSlugNormalizer + 16 unit tests + V40 |
| GAP-536 | P0 | Backend | IdempotencyKey entity + Service + Job + V41 + 7 tests |
| GAP-582 | P1 | Backend | V51 oauth_attempts table + UNIQUE state_token |
| GAP-658 | P0 | Backend | 6 VN CSV + VietnamSampleDataGenerator shape |
| GAP-112 | P1 | DevOps | Spring Boot 3.5 MDC bridge + otel/micrometer wired |
| GAP-371 | P1 | DevOps | Cloudflare runbook 13 sections |
| GAP-374 | P1 | DevOps | release-tag.yml workflow + changelog generator |
| GAP-379 | P1 | DevOps | Lambda + 3 secret rotations + runbook §5.2 |
| GAP-408 | P1 | DevOps | 6 services heap cap configured 512m-768m |
| GAP-436 | P1 | DevOps | OIDC Phase 1+2+3 DONE PR #993 |
| GAP-447 | P1 | DevOps | kh-backend right-size DONE + CWAgent spec |
| GAP-466 | P1 | DevOps | 51 tables RLS + 1398+452 tests PASS |
| GAP-473 | P1 | DevOps | Stack on-demand start/stop scripts |
| GAP-475 | P1 | DevOps | Smoke test 5/6 sub-tests functional |
| GAP-477 | P1 | DevOps | Rollback workflow PR #1188-1190 Wave 63 |
| GAP-502 | P0 | DevOps | RC1+RC2 Wave 70 resolved; defer 14-day observation |
| GAP-514 | P1 | DevOps | Password reset rate limit defense-in-depth |
| GAP-527 | P1 | DevOps | Email actuator + smoke script |
| GAP-544 | P1 | DevOps | Subscription Testcontainers migrated Wave 79 |
| GAP-566 | P0 | DevOps | t3.small 2GB swap + memory alarm 2026-05-15 |
| GAP-692 | P1 | DevOps | env-reference.yaml Phase 1 tooling Wave 102.8 |
| GAP-508 | P1 | Meta | env-config-registry rule v1.1.0 + audit-env-coverage.sh |
| GAP-135 | P1 | Mixed | 11 @Timed + SLO rubric + Prometheus rules |
| GAP-191 | P1 | Mixed | ADR-018 + terraform DNS + CNAME state machine |
| GAP-656 | P1 | Mixed | Wave 98 B0 coordinator + 6 files; mobile test pending |

(Tổng 34 dòng trong table = 32 gap LIKELY_DONE + 2 dòng bonus từ tổng hợp Backend/Mixed)

### 6.2 44 gap STILL_PARTIAL (cần làm thật — đầu vào Đợt 107)

3 cụm chính:
- **Cụm thư điện tử + onboarding** (~10): GAP-543, 530, 531, 537, 538, 657, 659, 586, 587
- **Cụm quan trắc + audit log** (~5): GAP-115, 434, 521, 517, 589, 590
- **Cụm CI/CD hardening** (~5): GAP-400 (Trivy), 401 (multi-arch), 402 (cosign/SBOM), 442 (Alpine 3.24), 444 (staging defer)
- **Cụm bảo mật + tài liệu** (~5): GAP-043 (cache stampede), 063 (SMS/Zalo), 222 (outbox phase-2), 516 (2FA IT), 638 (admin DTO drift)
- **Cụm thesis + meta** (~5): GAP-647, 689, 695, 675 (premature-rule guard), 204 (npm CVE)
- **Cụm hạ tầng còn lại** (~14): GAP-117, 370, 380, 412, 413, 440, 469, 533, 567, 572, 573, 223, 428, 537c

### 6.3 3 gap AWS_BLOCKED (đợi GAP-612 restore)

- GAP-612 (chính nó — bản gốc)
- GAP-412 (AWS Activate Founder application)
- GAP-693 (AWS rebuild SOP — phụ thuộc GAP-612)

→ Defer chính thức cho đến khi GAP-612 phục hồi.

### 6.4 1 gap cần xem lại

- GAP-675 (premature-rule-guard META audit) — không chặn execution, chờ user quyết định scope

## 7. Khuyến nghị cho Đợt 107

1. **Mở đợt sửa cụm thư điện tử + onboarding (~10 gap STILL_PARTIAL)** — phụ thuộc Resend nhưng không chặn AWS local; có thể sửa nhiều phần
2. **Mở đợt sửa cụm bảo mật + tài liệu (~5 gap)** — Spring Boot bump (GAP-440), cache stampede (GAP-043), DTO drift (GAP-638) — không chặn AWS
3. **Defer formally 3 gap AWS_BLOCKED + 32 gap chờ-AWS-walk** — qua trường `notes` trong `gap-status.csv` để tester beta nhìn ra rõ ràng
4. **KHÔNG bắt đầu Đợt 106 RST cho đến khi GAP-612 phục hồi** — luồng RST cần đăng nhập + cập nhật DB + gửi thư qua AWS resources

## 8. Bài học rút ra cho Đợt 107+

1. **Agent Explore state-check KHÔNG đủ để đề xuất DONE flip** — cần thêm bước "verify từng AC checkbox" trước khi gắn nhãn LIKELY_DONE
2. **AC dạng "live verify post-deploy" là bóng đèn báo** — báo hiệu gap KHÔNG đóng được nếu không có deploy + walk thực
3. **AWS restore là nút thắt cổ chai** — 32+3 = 35 gap (44%) phụ thuộc trực tiếp; cần ưu tiên đẩy AWS Support trả lời

## 9. Tham chiếu

- `audit-to-gap-pipeline.md` §2.8 Fix-Time State-Check — quy tắc áp dụng
- `gap-done-discipline.md` §2 — tiêu chí DONE flip
- `pre-handoff-self-test-completeness.md` §1 — cảnh báo "curl 201 ≠ user đi được"
- `gap-status.csv` — backlog canonical
- PR #1739 — Đợt 106 plan (RST 23 luồng)
- 5 báo cáo agent state-check chi tiết: lưu trong session transcript

## 10. Lịch sử

- **2026-05-23:** Khảo sát hoàn tất; quyết định KHÔNG flip 32 gap LIKELY_DONE; chuyển thành "code-ready, chờ AWS restore + live walk hậu-AWS". Backlog Pha 1 BETA không giảm số nhưng kiến thức về backlog tăng đáng kể.
