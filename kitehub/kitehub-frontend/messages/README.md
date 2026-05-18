# messages — Vietnamese i18n strings catalog

**Status:** Canonical reference catalog (v1.0.0)
**Last Updated:** 2026-05-18
**Wave:** Wave 98 Bucket B4 GAP-541 (Customer-facing Vietnamese i18n close)

## Mục đích

Thư mục này chứa canonical Vietnamese strings cho KiteHub frontend — phục vụ:

1. **Audit reference** — đối chiếu narrative Vietnamese trong hardcoded `.tsx` với canonical catalog để phát hiện drift hoặc English narrative leak (per `dev-readable-doc-language.md`).
2. **Future i18n library integration** — khi GAP-182 Phase 2 ship next-intl/i18next, các catalog ở đây sẽ trở thành source-of-truth thay vì hardcoded inline.
3. **In-app preview** — `email-preview.json` map nội dung Thymeleaf email templates Vietnamese cho UI admin xem trước (pair với Wave 98 Bucket B1 GAP-657/659 nếu cần).

## Cấu trúc

```
messages/
├── README.md                # File này
└── vi/                      # Vietnamese (default locale Phase 1 BETA)
    ├── common.json          # Shared UI primitives: nav, buttons, errors, loading, forms, weekday, time
    ├── legal.json           # TOS + Privacy + Cookie + DPA headings + key blocks
    ├── beta.json            # Beta banner, /beta-status page, feedback widget, NPS
    └── email-preview.json   # In-app preview của 20 email templates Vietnamese
```

## Quy ước nội dung

- **Vietnamese narrative** mọi nội dung user-facing (per `dev-readable-doc-language.md` §2 + CLAUDE.md §"CRITICAL: Communication Language").
- **English technical tokens** giữ nguyên trong câu Vietnamese (per `dev-readable-doc-language.md` §4): `HTTP 200`, `JWT`, `CORS`, `OIDC`, `TLS`, `OTP`, `2FA`, `CSV`, `API`, `SDK`, `OAuth`.
- **Brand names** giữ English: `KiteHub`, `KiteClass`, `AWS`, `Cloudflare`, `MISA`, `MoMo`, `VNPay`, `Zalo`.
- **Currency** format VND theo `Intl.NumberFormat('vi-VN')`: `1.500.000đ` hoặc `1.500.000 ₫`.
- **Date** format Vietnamese: short `18/05/2026`, long `Thứ Hai, 18/05/2026`, time `14:30`.
- **Sample data** dùng tên Việt thật: "Trần Thị Hồng", "Trung tâm Sky Education", "Lớp Anh ngữ 5A1" (per `user-manual-content-standard.md` §2 row 7).

## Tình trạng implementation

- **Phase 1 BETA (hiện tại):** Vietnamese content hardcoded inline trong `.tsx` (per GAP-541 state-check 2026-05-14). Catalog này là canonical reference cho audit + future migration.
- **Phase 2 (counsel-review + next-intl integration):** GAP-182 sẽ migrate hardcoded strings sang `t('common.button.submit')` consume từ JSON.
- **English (`en/`) locale:** Defer Phase 2 per CLAUDE.md decision context Moderate risk — Phase 1 BETA = Vietnamese audience only (P1 Solo Teacher + P2 Center Owner).

## Mối quan hệ với code

| Catalog | File code đối chiếu |
|---|---|
| `vi/common.json` | `src/app/**/page.tsx`, `src/components/**` (nav + buttons + forms hardcoded) |
| `vi/legal.json` | `src/app/(public)/legal/{terms,privacy,cookies,dmca,data-rights}/page.tsx` |
| `vi/beta.json` | Beta banner component, `/beta-status` page (Bucket B Wave 98), feedback widget |
| `vi/email-preview.json` | `kitehub-email/src/main/resources/templates/emails/*.html` (Thymeleaf templates) |

## Maintenance

- Khi đổi narrative Vietnamese trong `.tsx`, cập nhật key tương ứng trong JSON (per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — code và doc sync cùng PR).
- Khi thêm UI primitive mới (button, error, status), thêm key vào `common.json` để tránh duplicate string.
- Khi update email template Thymeleaf, mirror vào `email-preview.json` cho in-app preview consistency.

## References

- Gap: `documents/04-quality/gaps/phase-1-beta/GAP-541-customer-facing-vi-i18n-audit.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md`
- Rules: `.claude/rules/dev-readable-doc-language.md`, `.claude/rules/user-manual-content-standard.md`
- Audit: `documents/04-quality/audits/i18n/2026-05-14-customer-facing-vi-audit.md`
- Future scope: GAP-182 Phase 2 (counsel-reviewed) + next-intl integration
