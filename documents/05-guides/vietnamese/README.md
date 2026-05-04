# vietnamese — VN-language guide translations

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Vietnamese-language versions của các guide mà target audience yêu cầu tiếng Việt (vd client-facing onboarding, deploy guides cho Oracle Cloud customer). Không phải mọi guide cần bản VN — chỉ guide có audience non-engineer hoặc external customer.

---

## Directory Map

| File | EN counterpart | Purpose |
|------|----------------|---------|
| `LOCAL-DEV.md` | [`../local-dev/`](../local-dev/) (multiple) | VN dev setup overview |
| `ENVIRONMENT-MATRIX.md` | (no direct EN — VN-original) | Tổng quan environment dev/staging/prod |
| `PRODUCTION-DEPLOY.md` | [`../deploy/cicd-release-procedure.md`](../deploy/cicd-release-procedure.md) | VN production deploy procedure |
| `SECRET-MANAGEMENT.md` | [`../infrastructure/SECRET-MANAGEMENT.md`](../infrastructure/SECRET-MANAGEMENT.md) | VN secret management |
| `huong-dan-deploy-oracle-cloud.md` | (no EN counterpart) | Oracle Cloud deploy cho VN customer |
| `huong-dan-testing-100-percent.md` | (no EN counterpart) | VN testing methodology guide |
| `huong-dan-trien-khai-production.md` | (no EN counterpart) | VN production rollout guide |

---

## File Placement Rules

- ✅ **Belongs here:** VN translations của EN guide có audience VN-only, hoặc VN-original guide cho client-facing scenario
- ❌ **Does NOT belong here:** internal dev docs (audience đa số English-OK — không cần dịch), business rules (xem `documents/01-business/`)
- **Naming:** EN-style for direct translations (`SECRET-MANAGEMENT.md`), VN-style for VN-original (`huong-dan-*.md`)

---

## Translation sync policy

Khi EN counterpart update major:
- Reviewer flag VN file as stale
- Update VN file trong vòng 7 ngày HOẶC mark `[OUTDATED — see EN counterpart YYYY-MM-DD]` ở đầu file
- Long-stale files (>30 ngày) → archive

---

## Archive Policy

Move sang `documents/07-archived/vietnamese-YYYY/` khi:
- EN counterpart bị archive
- Customer audience không còn (vd ngừng support Oracle Cloud)
- VN file >180 ngày không update + EN counterpart đã update major
