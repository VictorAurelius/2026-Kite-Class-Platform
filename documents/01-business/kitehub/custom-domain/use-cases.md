# Custom Domain — Use Cases

**Last verified:** 2026-06-01
**Created:** Wave tenant-domain-1 Bucket D (GAP-812)
**Rules:** [`rules.md`](rules.md)
**API:** [`api-contract.md`](api-contract.md)

## UC-DOMAIN-001 — Owner đăng ký custom domain

**Actor:** Center Owner (PREMIUM/ENTERPRISE tier)
**Trigger:** Owner muốn dùng domain riêng `lop.skyedu.vn` thay vì `skyedu.kitehub.me`.

**Preconditions:**
- Instance ở tier PREMIUM hoặc ENTERPRISE (BR-DOMAIN-011)
- Owner đã đăng ký domain ở vendor riêng (Mat Bao / PA Vietnam / Namecheap...) và có quyền sửa DNS

**Main flow:**
1. Owner mở **Settings → Custom Domain** trong KiteHub admin UI
2. Owner nhập domain `lop.skyedu.vn` + click "Initiate verify"
3. System gọi `POST /api/instances/{id}/domain` với body `{ customDomain: "lop.skyedu.vn" }`
4. System sinh token `kitehub-verify={uuid}` + lưu vào Instance (`domainStatus=PENDING_VERIFY`) — BR-DOMAIN-001/002
5. System trả về `verifyRecord` instruction: "Add TXT record at `_kitehub-verify.lop.skyedu.vn` value `kitehub-verify=abc...`"
6. UI hiển thị instruction + nút **"Tôi đã thêm TXT, verify ngay"**
7. Owner login vào DNS provider, thêm TXT record theo instruction
8. Owner click "Verify ngay" → system gọi `POST /api/instances/{id}/domain/verify`
9. System DNS TXT lookup qua JNDI (BR-DOMAIN-005):
   - Lookup `_kitehub-verify.lop.skyedu.vn` TXT
   - Fallback: lookup apex `lop.skyedu.vn` TXT
10. Match → flip `domainStatus=VERIFIED` (v1.0) hoặc `CERT_PROVISIONING` (v1.1+) + record `domainVerifiedAt`
11. UI hiển thị status badge "Active" / "Pending cert provision"

**Postconditions:**
- Owner truy cập `https://lop.skyedu.vn` được route đúng tenant
- Backup URL `https://skyedu.kitehub.me` vẫn hoạt động (BR-DOMAIN-007)

**Errors:**
- Tier không hỗ trợ → 403 + thông báo upgrade plan (BR-DOMAIN-011)
- Domain đã được instance khác chiếm → 400 "already in use" (BR-DOMAIN-006)
- DNS chưa propagate → status giữ PENDING_VERIFY; UI gợi ý "chờ 5-15 phút rồi verify lại"
- Quá 48h chưa verify → status `FAILED` (timeout job); Owner re-initiate (BR-DOMAIN-003/004)

**FE behavior:**
- Trạng thái real-time: badge màu (Gray=NONE, Yellow=PENDING, Blue=CERT_PROVISIONING, Green=VERIFIED, Red=FAILED)
- Copy-to-clipboard cho TXT record value
- Per-vendor DNS guide modal (Mat Bao / PA Vietnam / Namecheap / Cloudflare) — link tới runbook §3
- SSL-pending fallback page: trong CERT_PROVISIONING, click vào URL custom domain → serve qua backup subdomain với banner "Cert đang được cấp..."

---

## UC-DOMAIN-002 — Owner gỡ custom domain

**Actor:** Center Owner

**Preconditions:**
- Instance đang có `customDomain` set ở bất kỳ trạng thái (PENDING/VERIFIED/FAILED)

**Main flow:**
1. Owner mở Settings → Custom Domain → click "Remove"
2. UI confirmation modal: "Bạn chắc chắn? Backup URL `{subdomain}.kitehub.me` vẫn hoạt động."
3. Owner confirm → system gọi `DELETE /api/instances/{id}/domain`
4. System reset: `customDomain=null`, `domainVerifyToken=null`, `domainVerifiedAt=null`, `domainStatus=NONE` (BR-DOMAIN-012)
5. UI redirect về Custom Domain page với state NONE
6. (Future) Background job: cleanup ACM cert + Cloudflare Custom Hostname

---

## UC-DOMAIN-003 — Re-verify sau khi FAILED

**Actor:** Center Owner

**Preconditions:**
- `domainStatus=FAILED` (vd quá 48h timeout)

**Main flow:**
1. Owner mở Custom Domain page → thấy status FAILED + message "Verify timeout sau 48h. Click để thử lại."
2. Owner click "Re-verify" → system gọi `POST /api/instances/{id}/domain` (same domain)
3. System regenerate token mới + reset `domainStatus=PENDING_VERIFY` (BR-DOMAIN-004)
4. Continue như UC-DOMAIN-001 từ bước 5

**Note:** Token cũ trong DNS TXT của tenant không match → phải update TXT record với token mới.

---

## UC-DOMAIN-004 — Scheduled DNS polling (future v1.1)

**Actor:** Background job (`DomainVerificationScheduler`)

**Preconditions:**
- Có ≥1 instance ở status `PENDING_VERIFY`

**Main flow:**
1. Job chạy mỗi 10 phút (`@Scheduled(fixedDelay = 600000)`)
2. Query instances `WHERE domainStatus=PENDING_VERIFY`
3. Per instance: gọi `DnsTxtLookupService.verifyTxtRecord()`
4. Match → flip VERIFIED + `domainVerifiedAt`; Outbox event `domain.verified`
5. Quá `timeoutHours` chưa match → flip FAILED + log audit

**Status:** Deferred to v1.1 (scheduler job not yet implemented in this PR — manual verify endpoint sufficient cho Phase 1 BETA).

---

## Related

- **Rules:** [`rules.md`](rules.md)
- **API:** [`api-contract.md`](api-contract.md)
- **Runbook:** [`../../../05-guides/operations/custom-domain-verify-runbook.md`](../../../05-guides/operations/custom-domain-verify-runbook.md)
- **Gap:** [GAP-812](../../../04-quality/gaps/phase-1-beta/GAP-812-custom-domain-dns-ssl-completion.md)

## Log

- **2026-06-01:** Doc created — Wave tenant-domain-1 Bucket D (GAP-812). 4 use cases cover Owner-facing flow + future scheduled polling.
