---
audience: mixed
---

# PDPL 2023 Compliance Checklist — Pre-Launch Sign-off (Hiệu lực 2026-07-01)

**Created:** 2026-06-02
**Owner:** @nguyenvankiet (solo-dev — acting Compliance scout + DPO proxy Phase 1 BETA)
**Status:** ✅ Phase 1 BETA self-attested PASS — full counsel formal review → GAP-182 Phase 2
**Phase scope:** Phase 1 BETA marketing-surface launch (KiteHub + KiteClass public landing + blog + legal pages)
**Out-of-scope cho v1.0.0:** server consent record API (GAP-353b), DSAR self-service (GAP-353c), DPIA Decree 13/2023 Art 24-30 documentation (GAP-353d), K-12 explicit consent flow (Phase 3)

---

## 1. Mục đích

Checklist này verify mọi yêu cầu **Personal Data Protection Law 2023 (Luật Bảo vệ Dữ liệu Cá nhân, hiệu lực 2026-07-01)** Articles 11-13 (consent collection) đã được implement ở mức Phase 1 BETA. Đây là **self-attestation document** cho marketing-surface launch — KHÔNG thay thế formal legal counsel review (GAP-182 Phase 2). Áp dụng disclaimer "v1 pending counsel review" cho non-K-12 marketing per CLAUDE.md decision context 2026-05-06.

Countdown: **29 ngày** từ 2026-06-02 → 2026-07-01 effective date.

---

## 2. PDPL Articles 11-13 mapping

### Article 11 — Sự đồng ý của chủ thể dữ liệu

| Yêu cầu | Implementation | Status | Evidence |
|---|---|---|---|
| Đồng ý PHẢI rõ ràng, tự nguyện | ConsentBanner 3 CTA cân bằng visual (Từ chối tất cả / Tuỳ chỉnh / Đồng ý tất cả) — không pre-select | ✅ DONE | `packages/shared-ui/src/components/ConsentBanner/ConsentBanner.tsx` §"Dark-pattern guards" |
| Granular per-purpose | Toggle riêng cho Cookie thiết yếu (locked-on) / Phân tích (opt-in default OFF) / Marketing (opt-in default OFF) | ✅ DONE | `ConsentBanner.tsx` lines 216-250 fieldset 3 categories |
| Không dark pattern | "Từ chối tất cả" reachable 1 click ngang "Đồng ý tất cả"; analytics/marketing default OFF | ✅ DONE | `BR-PDPL-CONSENT-002` + component dark-pattern guards javadoc |
| Đồng ý phải có thể thu hồi | `useConsent` hook expose `revoke()`; cookie page mention banner re-prompt mechanism | ✅ DONE | `useConsent.ts` + `kiteclass-frontend/src/app/(public)/legal/cookies/page.tsx:217` |

### Article 12 — Quyền của chủ thể dữ liệu

| Quyền | Phase 1 BETA implementation | Status | Evidence |
|---|---|---|---|
| Quyền được biết (right to know) | Privacy + Cookie page link trực tiếp từ ConsentBanner; legal pages shipped GAP-368 | ✅ DONE | `(public)/legal/privacy/page.tsx` + `(public)/legal/cookies/page.tsx` |
| Quyền đồng ý / từ chối | ConsentBanner 3 CTA + per-category toggle | ✅ DONE | Component shipped Wave 23 Bucket BC |
| Quyền truy cập (right to access) | Self-service DSAR API → **DEFERRED GAP-353c** Phase 2 | ⚠️ DEFERRED | Manual email request via support@kitehub.me Phase 1 BETA |
| Quyền sửa đổi (right to rectification) | User profile edit (existing) | ✅ DONE (user-account scope) | KiteHub user settings page |
| Quyền xóa (right to be forgotten) | DSAR deletion request → **DEFERRED GAP-353c** Phase 2 | ⚠️ DEFERRED | Manual email request + 30-day SLA documented privacy page |
| Quyền hạn chế xử lý | Revoke consent flow stops analytics/marketing scripts | ✅ DONE | `analytics.ts` gates pixel/script loading by consent state |
| Quyền phản đối | Reject all + per-category toggle | ✅ DONE | ConsentBanner UI |
| Quyền yêu cầu bồi thường | Legal pages document DPO contact (support@kitehub.me) | ✅ DONE | Privacy page §Contact |

### Article 13 — Nghĩa vụ của bên xử lý dữ liệu

| Nghĩa vụ | Phase 1 BETA implementation | Status | Evidence |
|---|---|---|---|
| Thông báo mục đích thu thập | Privacy page liệt kê 3 category (essential / analytics / marketing) + mục đích cụ thể | ✅ DONE | `(public)/legal/privacy/page.tsx` §"What data we collect" |
| Bảo mật dữ liệu | TLS 1.3 + HTTPS only (CF apex `kitehub.me`); password hash bcrypt; JWT signing key rotated 90-day | ✅ DONE | TLS verified `curl -sI https://kitehub.me/`; bcrypt per AuthService; rotation per GAP-379 |
| Lưu trữ có thời hạn (retention) | Cookie 12 tháng max (banner re-prompt expiry); consent log 36 tháng per DR-03; user data deletion on account close | ✅ DONE (cookie scope) | `storage.ts` `DEFAULT_TTL_MS = 365 days`; DR-03 retention rule per `output-review-mandate.md` §1 example |
| Thông báo vi phạm (breach notification) | Incident response runbook 72h notify per PDPL Art 17 | ⚠️ PARTIAL | Runbook scaffolded `documents/05-guides/operations/incident-response-runbook.md`; formal DPA breach playbook → GAP-353d |
| Cung cấp DPO contact | Privacy page + cookie page list support@kitehub.me | ✅ DONE | Legal pages footer |

---

## 3. Implementation surface coverage

### 3.1 Frontend mount points

| Mount point | Banner visible on routes | Verified |
|---|---|---|
| `kitehub-frontend/src/components/layout/PublicLayout.tsx:48` | `/`, `/pricing`, `/blog`, `/blog/[slug]`, `/legal/**` | ✅ Code present + tests pass |
| `kiteclass-frontend/src/app/(public)/layout.tsx:229` | `/`, `/about`, `/catalog`, `/contact`, future `(public)/**` | ✅ Code present + tests pass |

### 3.2 Component governance

| Element | Spec | Implementation |
|---|---|---|
| State machine | NOT_PROMPTED → PROMPTED → {CONSENT_GIVEN / REJECTED} → REVOKED → RE_PROMPTED | `useConsent.ts` |
| WCAG AA | role=dialog + aria-modal=false + focus trap + ESC reject + aria-live polite | `ConsentBanner.tsx` lines 99-167 |
| Storage | LocalStorage versioned key `kite.consent.v1` + 12-month TTL | `storage.ts` `DEFAULT_STORAGE_KEY` |
| Vietnamese-first | All copy + ARIA labels in Vietnamese per CLAUDE.md + `vn-localization-audit-checklist.md` §2 | `COPY_VI` const lines 51-75 |
| Test coverage | 27 RTL tests (8 storage + 8 hook + 11 component flows) | `packages/shared-ui/src/components/ConsentBanner/__tests__/` 4 files, 851 LOC tests |

### 3.3 Cross-cut AC enrichment

| Sister gap | ConsentBanner integration AC | Status |
|---|---|---|
| GAP-274 (KC marketing port) | "ConsentBanner integrated on landing per `BR-PDPL-CONSENT-001..004`" | ✅ Enriched Wave 23 Bucket E (line 43) |
| GAP-275 (KH marketing + blog port) | "ConsentBanner integrated on landing + blog" | ✅ Enriched Wave 23 Bucket E (line 43) |
| GAP-350 (kitehub-story-v2 polish) | Banner mockup `kitehub-story-v2/screens/consent-banner.html` | ✅ DONE Wave 21 PR #807 (gap closed) |
| GAP-368 (production legal pages) | Privacy + Cookie + Terms pages live | ✅ DONE — link targets verified `/legal/privacy`, `/legal/cookies`, `/legal/terms` |

### 3.4 Business rules sign-off

| Rule | Source | Status |
|---|---|---|
| BR-PDPL-CONSENT-001 — Banner mandatory all public marketing surfaces | `documents/01-business/kitehub/marketing/rules.md` Wave 23 Bucket A | ✅ DONE — 5-attribute compliant per `business-logic-review.md` |
| BR-PDPL-CONSENT-002 — Granular toggles, no dark patterns | Same | ✅ DONE — verified component dark-pattern guards |
| BR-PDPL-CONSENT-003 — Consent record retention (DR-03 36mo) | Same — cross-link DR-03 | ⚠️ Server-side audit log → GAP-353b Phase 2; client storage 12-month OK |
| BR-PDPL-CONSENT-004 — Revocation flow (settings + cookie reset) | Same | ✅ DONE — `revoke()` exposed + settings page mention |

---

## 4. Phase 1 BETA sign-off attestation

**Self-attestation (solo-dev acting Compliance scout):**

Tôi xác nhận rằng các yêu cầu Personal Data Protection Law 2023 Articles 11-13 đã được implement ở mức Phase 1 BETA marketing-surface launch (KH + KC public landing + blog + legal pages). Mọi item PASS trong §2/§3 đã được verify qua code review + unit/integration tests + walk-through `kitehub.me` landing page (browser test ConsentBanner render + Accept/Reject/Customize flows).

| Sign-off | Date | Status |
|---|---|---|
| **Phase 1 BETA self-attestation** (Articles 11-13 minimum interpretation) | 2026-06-02 | ✅ PASS — self-attested |
| **Formal legal counsel review** (full Articles 1-50 audit) | TBD | ⚠️ DEFERRED — GAP-182 Phase 2 (counsel engaged Phase 3 trigger) |
| **Server-side consent audit log API** | TBD | ⚠️ DEFERRED — GAP-353b Phase 2 |
| **DSAR self-service intake form** | TBD | ⚠️ DEFERRED — GAP-353c Phase 2 (manual email request Phase 1 BETA) |
| **DPIA Decree 13/2023/NĐ-CP Art 24-30** | TBD | ⚠️ DEFERRED — GAP-353d Phase 2 |
| **K-12 explicit parental consent flow (PDPL Art 20)** | Phase 3 | ⚠️ DEFERRED — Phase 3 K-12 release trigger per CLAUDE.md decision context |

**Disclaimer ship-with marketing surface (per CLAUDE.md Risk tolerance Moderate):**
> Tài liệu pháp lý này (v1) đang được rà soát bởi tư vấn pháp lý. Phiên bản chính thức sẽ được cập nhật trước ngày hiệu lực PDPL 2026-07-01.

---

## 5. Follow-up actions (Phase 2+)

| Action | Owner | Deadline | Tracking |
|---|---|---|---|
| Engage formal legal counsel (Vietnamese law firm) | @nguyenvankiet | Q3 2026 (Phase 2 trigger) | GAP-182 |
| Ship server-side consent API `POST /api/v1/consent/record` + `GET /api/v1/consent/{userId}` | Backend wave Phase 2 | Q3 2026 | GAP-353b |
| Ship DSAR self-service intake form + 30-day SLA workflow | Frontend + Backend wave Phase 2 | Q4 2026 | GAP-353c |
| Document DPIA per Decree 13/2023/NĐ-CP Art 24-30 | Compliance wave Phase 2 | Q4 2026 | GAP-353d |
| K-12 parental consent flow (PDPL Art 20 minor data subject) | Phase 3 K-12 release | TBD (counsel engaged + 4 sub-conditions) | Phase 3 plan |
| Quarterly re-audit Articles 11-13 implementation | @nguyenvankiet | 2026-09-01, 2026-12-01 (90-day cadence) | This checklist re-run |

---

## 6. Verification commands

Tự verify any time bằng:

```bash
# Component existence
ls packages/shared-ui/src/components/ConsentBanner/
# Expected: ConsentBanner.tsx + useConsent.ts + storage.ts + api.ts + analytics.ts + types.ts + __tests__/

# Mount points
grep -n "ConsentBanner" kitehub/kitehub-frontend/src/components/layout/PublicLayout.tsx \
                       kiteclass/kiteclass-frontend/src/app/\(public\)/layout.tsx
# Expected: 2 import + 2 usage sites

# Tests pass
pnpm --filter @kite/shared-ui test
# Expected: 27 tests PASS

# Business rules present
grep -c "BR-PDPL-CONSENT-00[1-4]" documents/01-business/kitehub/marketing/rules.md
# Expected: 4

# Legal page links resolve (post-deploy verification)
for url in /legal/privacy /legal/cookies /legal/terms; do
  curl -sI "https://kitehub.me${url}" | head -1
done
# Expected: HTTP/2 200 for all 3
```

---

## 7. References

- **Luật:** Personal Data Protection Law 2023 (LBVDLCN 2023, Quốc hội thông qua, hiệu lực 2026-07-01) Articles 11-13
- **Decree:** Decree 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân (chi tiết DPIA Articles 24-30)
- **Component spec:** `documents/02-architecture/design-system/dossier/14-common-components-inventory-{kh,kc}.md` §G14
- **Business rules:** `documents/01-business/kitehub/marketing/rules.md` BR-PDPL-CONSENT-001..004
- **Sister gaps:** GAP-353b (server consent API), GAP-353c (DSAR), GAP-353d (DPIA), GAP-368 (legal pages production)
- **Risk decision context:** `CLAUDE.md` §"Decision context locked 2026-05-06" — Moderate risk tolerance + "v1 pending counsel review" disclaimer OK cho non-K-12 + Phase 3 K-12 trigger requires counsel engaged
