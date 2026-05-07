# i18n Strategy — KiteHub / KiteClass Platform

**Status:** ✅ ACTIVE — Phase 1 BETA decision
**Owner:** @nguyenvankiet (acting Product Owner, solo-dev)
**Created:** 2026-05-07
**Closes:** GAP-391-B (i18n migration deferral artifact)
**Cross-refs:** `release-1-plan-2026.md` Phase 1 scope; `personas-catalog.md` Tier 1 personas; `business-logic-review.md` §2 (5-attribute rule)

---

## 1. Quyết định

> **Phase 1 BETA: Vietnamese-only.** Toàn bộ wizard, dashboard, marketing copy, email transactional, legal docs ship 100% tiếng Việt. KHÔNG có `useTranslation` hook, KHÔNG có message catalogue, KHÔNG có locale switcher.

Quyết định này có cân nhắc tradeoff cost/benefit và phù hợp với:
- Personas Tier 1 (P2 Center Owner / Pa Parent / Teacher / Student) đều là người Việt
- Pháp luật Việt Nam (Decree 53/2022/NĐ-CP) cho phép legal docs Vietnamese-only đối với khách hàng nội địa
- Solo-dev mode + 9-12 tuần Phase 1 timeline — i18n migration estimate ~20h là cost không cần thiết

---

## 2. Phase progression

| Phase | Scope | Locale support | Trigger để promote |
|-------|-------|----------------|---------------------|
| **Phase 1 BETA** (active, ~9-12 tuần) | P1 + P2 soft launch — VN education centers | 🇻🇳 VN-only | Quality audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần |
| **Phase 2 PAID** (+4-6 tuần) | + P3 medium-center | 🇻🇳 VN primary; 🇬🇧 EN optional nếu trigger | Counsel engaged + 4 sub-conditions (theo `release-1-plan-2026.md`) |
| **Phase 3 K-12** (+8-12 tuần post-counsel) | + K-12 P5 tenants + MoET expansion | 🇻🇳 VN + 🇬🇧 EN; 🇯🇵 JA / 🇰🇷 KO out of scope | Phase 3 completion + market validation |

### 2.1 Phase 2 EN trigger gates (cần thoả ≥1 để bật EN)

- **Conversion gate:** ≥10% non-VN signup attempts (Google Analytics IP + Accept-Language)
- **Partnership gate:** External partnership ký được với international education group (vd: AEAS, IB schools)
- **Marketing gate:** Strategic decision expand beyond VN domestic market
- **Compliance gate:** Bất kỳ quy định nào yêu cầu English copy (vd: GDPR cho EU tenant)

Nếu không trigger gate nào trong vòng 6 tháng sau Phase 2 mở, EN giữ trạng thái "deferred indefinite" cho đến khi market signal thay đổi.

### 2.2 Phase 3 JA/KO — out of scope hiện tại

Japanese + Korean được mention trong context ban đầu (CLAUDE.md MoET expansion) nhưng KHÔNG nằm trong roadmap có thời hạn. Sẽ được file gap riêng nếu Phase 3 K-12 mở rộng đến TPHCM/Hanoi international schools.

---

## 3. Implementation tradeoff (Phase 1 lý do chi tiết)

### 3.1 Cost của full i18n migration ngay từ Phase 1

| Hạng mục | Estimate |
|---|---|
| Wholesale `t()` wrap toàn bộ wizard 6 steps + dashboard | ~8h |
| Extract 200+ message keys vào `messages/vi.ts` + `messages/en.ts` | ~3h |
| 77 wizard tests cập nhật để query bằng key thay vì hardcoded VN | ~6h |
| Setup `next-intl` / `react-i18next` infra + locale routing | ~3h |
| **Total** | **~20h** |

### 3.2 Benefit Phase 1 (Vietnamese-only)

- Personas Tier 1 100% người Việt → translation chỉ tạo overhead, không có user
- Legal docs (TOS/Privacy/Refund) đã được luật sư prepare bằng tiếng Việt — translate sang EN tốn legal review fee thêm
- Marketing copy hiện đang tối ưu cho VN SEO + brand voice — duplicate sang EN không có data để optimize

### 3.3 Tại sao không "build i18n từ đầu cho tương lai"

- YAGNI principle — `t()` wrap không có translator/copywriter sẵn sàng = stale EN strings ngay từ ngày 1
- Migration cost gần như không đổi nếu defer (~20h dù làm bây giờ hay sau 6 tháng)
- Phase 2 trigger có thể KHÔNG xảy ra → cost sunk

---

## 4. Tracking

| Item | Status | Where |
|------|--------|-------|
| Phase 1 VN-only acceptable | ✅ Documented (this file) | — |
| GAP-391-B closed | ✅ Cross-ref this strategy | `documents/04-quality/gaps/GAP-391-*.md` |
| Phase 2 EN migration gap | ⏳ File khi trigger | (chưa tạo) |
| `useTranslation` infra setup gap | ⏳ File khi Phase 2 trigger | (chưa tạo) |

---

## 5. Anti-patterns (KHÔNG làm)

| ❌ Don't | ✅ Do |
|---|---|
| Wrap mọi string trong `t()` "for future" mà không có EN translator | Hardcode VN strings cho Phase 1 |
| Ship EN strings auto-translate qua Google Translate | Wait for native English copywriter (Phase 2 trigger) |
| Add locale switcher khi chỉ có VN content | Single-locale ship — không show toggle |
| Bypass strategy này cho 1 component "vì code đẹp hơn" | Tuân theo phase triggers; nếu cần exception, file gap với rationale |

---

## 6. Review cadence

- **Quarterly review:** check Phase 2 trigger gates (signup analytics, partnership pipeline)
- **Event-driven:** ≥10% non-VN signup → re-review immediately
- **Next review:** 2026-08-07 (3 tháng) HOẶC khi Phase 1 → Phase 2 transition

---

## 7. Log

- **2026-05-07:** Strategy documented as Phase 1 of GAP-391-B closure. Reviewer: @nguyenvankiet (solo-dev, acting Product Owner). Phase 2 EN migration deferred until trigger gates fire (§2.1). 77 wizard tests + components stay 100% hardcoded VN cho Phase 1.
