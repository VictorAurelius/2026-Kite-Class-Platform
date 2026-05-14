# Approve tenant email — content audit

**Template:** ❌ KHÔNG TỒN TẠI tại `kitehub/kitehub-email/src/main/resources/templates/emails/`
**Audited:** 2026-05-14
**Auditor:** Wave 78 Bucket E (GAP-543)
**Verdict:** ❌ TEMPLATE MISSING — file follow-up gap

---

## State check

Audit thực hiện trên template hiện có:

```bash
ls kitehub/kitehub-email/src/main/resources/templates/emails/ | grep -iE "approve|tenant"
# → 0 results
```

Email gần nhất phục vụ flow tenant approval = `beta-invite.html` (gửi sau khi admin approve beta-access request). Tuy nhiên đây là flow **beta-access → signup**, KHÔNG phải flow **tenant provisioning → notify Owner** đầy đủ.

GAP-531 (tenant init handoff) yêu cầu:
1. Admin click "Approve" trên `/admin/beta-requests` → triggers tenant provision (V35+ migrations).
2. Backend gửi **invite email** với signup token (= `beta-invite.html`).
3. User completes signup → tenant `beta=true`.
4. Tenant dashboard loads.

Hiện tại **bước 2 dùng `beta-invite.html`** thay vì dedicated `approve-tenant.html` cho Owner-side notification. Đây có thể là intentional consolidation (1 email = invite + approve confirmation) — nhưng audit cần verify intent.

---

## Recommendation

**Option A — Consolidate (keep current state):**
- Document rõ trong runbook rằng `beta-invite.html` cover cả approval notification (đã có disclaimer + 3-step instruction).
- File NO follow-up gap; close khâu này trong scope GAP-543.

**Option B — Separate template (Wave 79+ work):**
- Tạo `approve-tenant.html` riêng cho Owner-side notification sau khi tenant đã provision xong (post-signup).
- Content: "Tenant của bạn đã sẵn sàng — đăng nhập tại `${dashboardUrl}` để bắt đầu thiết lập."
- Audit dimensions tương tự welcome/beta-invite.

**Decision (Wave 78):** Option A — `beta-invite.html` đã cover scope đủ cho Phase 1 BETA. Owner nhận chỉ 1 email (beta-invite) → click → signup → dashboard load. Không cần tách thêm template trong Phase 1 BETA tightly-controlled handful invite.

---

## 7-dimension scoring (deferred)

N/A — template không tồn tại. Audit dimension chỉ áp dụng khi Option B chọn → file GAP-543.1 follow-up.

---

## Follow-up gap (deferred)

**GAP-543.1 (post-Wave 78):** Đánh giá xem có nên tách `approve-tenant.html` riêng khỏi `beta-invite.html` sau khi Phase 1 BETA persona feedback (10 tenants live) cho biết Owner-side có cần dedicated post-signup notification không. Wave 80+ candidate.

---

## Related

- Parent: GAP-543
- Sibling: GAP-531 (tenant init handoff end-to-end)
- Current template covering scope: `beta-invite.html` (audit ở `beta-invite-audit.md`)
- Rule: `dev-readable-doc-language.md` §4
