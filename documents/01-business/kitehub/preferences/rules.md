# User Preferences — Business Rules

**Domain:** User preference state — dismissible banners + onboarding phase tracking (Wave 98 GAP-656 UI Coordinator)
**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/preferences/controller/PreferencesController.java`
**Sister layers:** [`use-cases.md`](use-cases.md) (Layer 2) · [`api-contract.md`](api-contract.md) (Layer 3)
**Last verified:** 2026-06-21 (GAP-664 backfill — grounded in actual `PreferencesController` code)

> **Phạm vi Wave 98 (Phase 1):** chỉ "dismiss banner state" qua cookie marker. Persist vào bảng `user_preferences` + opt-out vĩnh viễn là Phase 2 (Wave 99+) — đánh dấu TODO trong code, KHÔNG thuộc rules dưới đây trừ khi ghi rõ "deferred".

---

## Rules

| ID | Rule | Value | Config / Code ref |
|----|------|-------|-------------------|
| BR-PREF-001 | **Cookie marker cho dismissed banner** | Server set cookie `kite-banner-dismissed-{bannerKey}` value `"1"` (dismissed) hoặc `"0"` (reset). `bannerKey` được sanitize (lowercase + strip ngoài `[a-z0-9-]`, ≤100 chars) trước khi ghép tên cookie. | `PreferencesController.COOKIE_PREFIX = "kite-banner-dismissed-"` + `sanitizeBannerKey()` |
| BR-PREF-002 | **TTL dismissal = 30 ngày** | Cookie `Max-Age = 30 ngày` (2.592.000 giây). Hết hạn → banner hiển thị lại. Giá trị hiện là **hằng số code** `COOKIE_MAX_AGE = Duration.ofDays(30)`, CHƯA tách thành config key. | `PreferencesController.COOKIE_MAX_AGE` (TODO Wave 99: tách `kitehub.preferences.banner-dismiss-ttl-days`) |
| BR-PREF-003 | **bannerKey format constraint** | `bannerKey` PHẢI non-blank, dài 3–100 ký tự, kebab-case `^[a-z0-9-]+$`. Vi phạm → 400 `PREF_INVALID_BANNER_KEY`. | `DismissBannerStateRequest` `@NotBlank @Size(min=3,max=100) @Pattern(regexp="^[a-z0-9-]+$")` |
| BR-PREF-004 | **dismissed bắt buộc** | Field `dismissed` (Boolean) là `@NotNull`; thiếu → 400 (validation). | `DismissBannerStateRequest.dismissed @NotNull` |
| BR-PREF-005 | **Cookie FE-readable (không httpOnly)** | Cookie set `HttpOnly=false` có chủ đích để FE hook `useOnboardingPhase` đọc qua `document.cookie` (cross-tab sync). Bù lại bằng `SameSite=Lax` (chống CSRF) + `Secure=true` (HTTPS-only qua reverse proxy). | `ResponseCookie ... .httpOnly(false).sameSite("Lax").secure(true)` |
| BR-PREF-006 | **Endpoint public (anonymous OK)** | Không yêu cầu JWT — khách ẩn danh vẫn dismiss được vì state gắn theo browser cookie. User đã đăng nhập: JWT context có sẵn nhưng Phase 1 CHƯA dùng để partition (map key hardcode `"anonymous:"`). | `PreferencesController.dismissBannerState()` — `mapKey = "anonymous:" + sanitizedKey` (TODO Wave 99: derive userId) |
| BR-PREF-007 | **Persistence Phase 1 = in-memory, per-server** | Trạng thái lưu trong `ConcurrentHashMap` mỗi instance, MẤT khi restart. Cookie phía client là nguồn dismissal bền vững thực sự ở Phase 1. KHÔNG ghi DB ở Phase 1. | `PreferencesController.dismissedState` (TODO Wave 99: `user_preferences` table) |
| BR-PREF-008 | **Không phát outbox event** | Preference state là per-user transient, không cross-service → KHÔNG emit broker/outbox event. | api-contract.md §Side effects |
| BR-PREF-009 | **Reset dismissal** | Gửi `dismissed=false` reset banner (kịch bản hiếm, admin / QA). Cookie value flip về `"0"` cùng TTL 30 ngày. | `dismissBannerState` — `request.dismissed() ? "1" : "0"` |

> **Lưu ý drift doc↔code (GAP-664):** api-contract.md mô tả response cookie có `HttpOnly` + gọi field `PREF_MISSING_DISMISSED`; code thực tế set `HttpOnly=false` (BR-PREF-005) và `dismissed` thiếu bị bắt bởi `@NotNull` validation chung (Bean Validation 400) chứ không có error code riêng `PREF_MISSING_DISMISSED`. Rules ở trên ground theo **code thực tế**; reconcile api-contract.md về sau (theo dõi GAP-666 / GAP-733 contract-sync).

---

## Verification chain

`BR-PREF-001..009` → `UC-PREF-001..003` ([use-cases.md](use-cases.md)) → `POST /api/v1/preferences/dismiss-banner-state` ([api-contract.md](api-contract.md)) → `PreferencesController#dismissBannerState` → `PreferencesControllerIT`.

---

## Code references

- Controller: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/preferences/controller/PreferencesController.java`
- IT test: `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/preferences/PreferencesControllerIT.java`
- FE consumer: `useOnboardingPhase` hook + `OnboardingCoordinator` component (per api-contract.md §Consumed bởi)

---

## Five-attribute review per `business-logic-review.md` §2

Các giá trị ở đây phần lớn là **quyết định UX/engineering** (TTL banner, tên cookie, format key) — không phải pricing/legal. Tuy nhiên cookie storage chạm vùng cookie-consent (PDPL) nên đánh giá rõ.

- **Source:** GAP-656 (Wave 98 Bucket B0 UI Coordinator) — informed gut + UX standard. TTL 30 ngày là first-best estimate dựa heuristic "banner re-show theo chu kỳ ~1 tháng để không gây phiền"; chưa có A/B test. Cookie `HttpOnly=false` là yêu cầu kỹ thuật để FE hook `useOnboardingPhase` đọc marker (GAP-656 §Proposed Fix Step 5).
- **Rationale:** 30 ngày — đủ dài để user không bị banner làm phiền lặp lại trong một tháng, đủ ngắn để banner quan trọng (vd disclaimer beta) tái xuất hiện sau khi context thay đổi. `SameSite=Lax + Secure` bù cho việc bỏ `HttpOnly` (cookie không chứa secret, chỉ là marker `"0"/"1"`). In-memory Phase 1 chấp nhận được ở quy mô beta (mất khi restart không ảnh hưởng vì cookie client là nguồn bền vững).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-06-21). Legal N/A đối với UX value (TTL, tên cookie); chiều cookie-consent self-assessed bên dưới, formal counsel review queued — GAP-156 AC-D.
- **Compliance check:** **Considered (self-assessed, counsel pending GAP-156 AC-D)** — cookie `kite-banner-dismissed-*` là **strictly-necessary / functional cookie** (chỉ lưu trạng thái UI đã-đóng-banner, KHÔNG tracking/profiling/quảng cáo) → theo `documents/00-brd/compliance-checklist.md` + Nghị định 13/2023/NĐ-CP (PDPL) thuộc nhóm essential, không cần consent (đồng nhất với BR-PDPL-CONSENT-001 "essential cookies locked-on" trong `../marketing/rules.md`). Không thu thập PII mới. Chưa có counsel verification.
- **Review cadence:** Quarterly. **Next review:** 2026-09-21 (next audit checkpoint). Event triggers: tách config key TTL (BR-PREF-002), Phase 2 persist `user_preferences` table (BR-PREF-007), thêm tracking cookie nào khác (đổi phân loại compliance).

---

## Related

- GAP-656 — domain origin (Wave 98 Bucket B0 UI Coordinator)
- GAP-664 — 3-layer backfill (this rules.md)
- GAP-666 — business README index sync (depends on this file)
- Rule `pre-launch-auth-hardening-checklist.md` — cookie flag policy (SameSite/Secure)
- Rule `business-logic-review.md` §2 — 5-attribute mandate

## Log

- **2026-06-21** — rules.md created (GAP-664 — 3-layer completeness backfill; preferences đã có api-contract.md, thiếu rules.md + use-cases.md). 9 BR-PREF-* grounded trong `PreferencesController` thực tế (cookie marker / TTL 30d hằng số / format / httpOnly=false / in-memory Phase 1 / no outbox). Ghi nhận drift doc↔code (HttpOnly + `PREF_MISSING_DISMISSED`) để GAP-666/733 reconcile. 5-attribute review per `business-logic-review.md` §2.
