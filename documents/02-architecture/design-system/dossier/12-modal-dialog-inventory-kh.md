# 12 — Modal / Dialog / Sheet / Drawer Inventory — KiteHub FE

**Source:** `grep -rE "<Dialog|<AlertDialog|<Sheet|<Drawer" kitehub/kitehub-frontend/src --include="*.tsx"` (2026-04-29, Wave UI Coverage Audit Agent B).

**Scope:** every Dialog / AlertDialog / Sheet / Drawer **usage site** (excludes shadcn primitives `components/ui/dialog.tsx` + `components/ui/alert-dialog.tsx` which are the underlying primitives, not consumer modals).

**Use this when:** auditing modal coverage, planning a "modals & dialogs" UI kit, or checking whether a destructive flow has a confirmation step.

---

## Coverage marker legend

- ✅ **explicit** — a kit ships a screen/state that depicts this modal directly
- ⚠️ **implicit** — a kit covers the parent flow but the modal itself isn't rendered as a separate screen (e.g., wizard step kit shows steps but not the "advanced disclaimer" modal split)
- ❌ **missing** — no kit screen depicts this modal

---

## Inventory — 5 usage sites · 8 distinct modal instances

### Customer-side (P2 Center Owner)

| # | File | Type | Triggered from | Persona | Use case | Kit-covered? | Note |
|:-:|------|:----:|----------------|---------|----------|:------------:|------|
| M1 | `app/(customer)/settings/components/DangerZone.tsx` (line 95) | Dialog | `/settings` → "Hủy đăng ký" button | P2 Center Owner | Cancel subscription confirmation (free-text reason field) | ❌ missing | No kit shows cancel-subscription modal; `kitehub-pro-v2` settings screens do not exist (preview-only wizard only) |
| M2 | `app/(customer)/settings/components/DangerZone.tsx` (line 147) | Dialog | `/settings` → "Xóa tài khoản" button (destructive) | P2 Center Owner | Delete account confirmation (type-to-confirm pattern) | ❌ missing | High-stakes destructive — needs explicit kit coverage |
| M3 | `components/onboarding/OnboardingWizard.tsx` (line 204) | Dialog (wizard shell) | First login post-register | P2 Center Owner | 6-step onboarding wizard (welcome → instance config → branding → done) | ⚠️ implicit | `kitehub-pro-v2/screens/branding-wizard-step1..step6-*.html` covers 4 wizard step variants; onboarding shell wrapper not depicted as discrete modal screen |

### Customer-side branding wizard (preview)

The `branding/wizard` flow is a **page**, not a modal — it lives at `/branding/wizard` route. AI Branding Wizard step files (`AnalyzeStep.tsx`, `GenerateStep.tsx`, `ReviewStep.tsx`, `UploadStep.tsx`, `ThemePreviewCard.tsx`) compose into the page; no Dialog usage. Kit `ai-branding-wizard-v2/screens/` covers all 6 steps × multiple states (28 HTML files) — coverage status ✅ for the wizard page itself (see `03-screen-inventory.md`), N/A for modal inventory.

### Platform admin — KH ops

| # | File | Type | Triggered from | Persona | Use case | Kit-covered? | Note |
|:-:|------|:----:|----------------|---------|----------|:------------:|------|
| M4 | `components/admin/AdminInstancesTable.tsx` (line 273) | AlertDialog | `/admin/instances` → row action button | KH platform ops admin | Confirm SUSPEND / RESUME / DELETE on tenant instance row | ❌ missing | No kit covers KH platform admin (kitehub-admin kit targets P5 K-12 School Principal — different persona) |
| M5 | `app/(admin)/admin/instances/[id]/InstanceActionDialogs.tsx` (line 77) | AlertDialog | Instance detail page → "Tạm ngưng" button | KH platform ops admin | Confirm suspend single instance (with optional reason) | ❌ missing | Same — platform admin has no dedicated kit |
| M6 | `app/(admin)/admin/instances/[id]/InstanceActionDialogs.tsx` (line 104) | AlertDialog | Instance detail page → "Kích hoạt" button | KH platform ops admin | Confirm activate suspended instance | ❌ missing | Same |
| M7 | `components/admin/AdminPaymentsTable.tsx` (line 278) | Dialog | `/admin/payments` → "Xác nhận" row action | KH platform ops admin | Confirm a manual payment receipt (admin marks paid) | ❌ missing | Same |
| M8 | `components/admin/AdminPaymentsTable.tsx` (line 324) | Dialog | `/admin/payments` → "Từ chối" row action | KH platform ops admin | Reject payment with reason (free text + reject reason enum) | ❌ missing | Same |
| M9 | `components/admin/AdminPaymentsTable.tsx` (line 364) | Dialog (preview) | `/admin/payments` → row "Xem QR" | KH platform ops admin | Preview QR code attached to incoming bank-transfer payment | ❌ missing | Same |

---

## Coverage breakdown

| Bucket | Total instances | ✅ explicit | ⚠️ implicit | ❌ missing |
|--------|:---------------:|:----------:|:-----------:|:---------:|
| Customer settings (Danger Zone) | 2 | 0 | 0 | **2** |
| Onboarding wizard shell | 1 | 0 | 1 | 0 |
| Platform admin — instances | 3 (1 in table + 2 in detail page) | 0 | 0 | **3** |
| Platform admin — payments | 3 (confirm + reject + QR preview) | 0 | 0 | **3** |
| **Total** | **9** | **0** | **1** | **8** |

**Key finding:** ZERO modals have explicit kit coverage. KH side has 8 missing modal screens — informs **GAP-279 (Common modals + dialogs catalog)** scope. Particularly notable: 6 platform-admin modals (M4–M9) are completely uncovered because no kit targets the KH ops viewpoint (kitehub-admin kit targets P5 K-12 School Principal, a tenant persona — see `03-screen-inventory.md` "(admin) nuance" note).

---

## Out of scope

- `components/ui/dialog.tsx` and `components/ui/alert-dialog.tsx` — shadcn primitives. They define `<Dialog>`, `<AlertDialog>` building blocks; the consumer sites above are the actual modal instances.
- Toast / sonner / inline alerts — those are not Dialog/Sheet/Drawer; tracked separately under common-components inventory.
- Test files (`*.test.tsx`) inside `__tests__/` — flagged by grep but excluded as they instantiate dialogs for testing only.

---

## Cross-references

- Production routes: `dossier/03-screen-inventory.md` § KiteHub
- Personas: `dossier/01-personas.md`
- Component gaps (G1..G12): `dossier/04-component-gaps.md`
- Wave plan: `documents/03-planning/waves/wave-2026-04-29-ui-coverage-audit.md` § Bucket B
- Sister inventory (KC): `dossier/12-modal-dialog-inventory-kc.md`
