# 04-quality/audits — Audit Reports & Verification Artifacts

**Rules:** [`docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) · [`output-review-mandate.md`](../../../.claude/rules/output-review-mandate.md) §3 · [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) · [`post-wave-audit-mandate.md`](../../../.claude/rules/post-wave-audit-mandate.md)

Nơi lưu **mọi báo cáo audit** (đánh giá có chấm điểm / có verdict) + **verification artifact** (bằng chứng đã kiểm tra production / mutation). Mỗi file là một **snapshot tại một thời điểm** — immutable sau khi ship (per `output-review-mandate.md` §3 audit-evidence convention). Index canonical: [`audits-index.csv`](audits-index.csv).

---

## Khái niệm: Audit là gì trong dự án này

Audit = **đánh giá một output theo standard có sẵn → ra điểm/verdict → feed vào gap pipeline**. KHÔNG fix trực tiếp trong audit (per `audit-to-gap-pipeline.md` §1): audit → gap file → fix PR.

**7 audit skill chuẩn** (chấm điểm, định kỳ post-wave per `post-wave-audit-mandate.md`):

| Audit | Thang | Skill |
|---|---|---|
| UI review | /128 per-screen | `quality/ui-review` |
| Quality | /110→/100 (11 category) | `quality-audit` |
| Security | /100 (v2 evidence block) | `quality/security-audit` |
| Performance | /100 | `quality/performance-audit` |
| API contract | /100 | `quality/api-contract-audit` |
| Ops readiness | /100 | `quality/ops-readiness-audit` |
| Business logic | /100 | `quality/business-logic-audit` |

Ngoài 7 audit chuẩn còn có audit **ad-hoc / outside-in** (persona-review, external-benchmark, simulation-gap-finder, failure-mode matrix) + **verification artifact** (aws-verification, cloudflare-verification — bằng chứng read-only per `agent-aws-access.md` §5 + `pre-mutation-state-check.md` §3).

---

## Directory Map

Mỗi subfolder = 1 category audit. File đặt tên `YYYY-MM-DD-<topic>.md` (Tier 2 time-bound per `docs-filename-prefix-convention.md`) HOẶC `AUDIT-NNN-...` nếu đã indexed.

| Subfolder | Mục đích | Loại |
|---|---|---|
| `quality/` · `quality-audit/` | Quality /100 reports | 7-audit chuẩn |
| `security/` | Security /100 reports | 7-audit chuẩn |
| `performance/` | Performance /100 | 7-audit chuẩn |
| `api/` · `api-contract/` | API contract /100 | 7-audit chuẩn |
| `ops/` · `ops-readiness/` | Ops readiness /100 | 7-audit chuẩn |
| `business/` · `business-logic/` | Business logic /100 | 7-audit chuẩn |
| `ui/` · `ui-review/` | UI /128 per-screen | 7-audit chuẩn |
| `persona-review/` | Persona simulation + pre-walk (outside-in) | ad-hoc |
| `external-benchmark/` · `outside-in-benchmark/` | So sánh ngành | ad-hoc |
| `simulation/` · `simulation-gap-finder/` | Ma trận failure-mode | ad-hoc |
| `aws-verification/` · `cloudflare-verification/` | Bằng chứng infra read-only / pre-mutation | verification |
| `credential-rotation/` | Bằng chứng rotate secret | verification |
| `local-stack/` · `rst-html/` · `pre-self-test/` | RST walk + local stack self-test | verification |
| `meta/` | Audit về meta-governance (rules/skills/process) | meta |
| `retro/` · `incidents/` · `gap-closure/` | Retro + incident RCA + gap-closure audit | ad-hoc |
| `acceptance-tests/` · `i18n/` · `email/` · `email-template/` · `design-patterns/` · `ai-branding/` · `architecture/` · `helm-k8s/` · `starter-kit/` · `waves/` | Audit chuyên đề | theo chủ đề |

---

## File Placement Rules

- ✅ **Belongs here:** Báo cáo audit có điểm/verdict; verification artifact (commands + findings); persona/benchmark/simulation outside-in audit.
- ❌ **Does NOT belong here:**
  - Gap files (issues audit tìm ra) → [`documents/04-quality/gaps/`](../gaps/) per `audit-to-gap-pipeline.md`
  - Skill rubric (cách chấm) → `.claude/skills/quality/*/SKILL.md`
  - Plan / roadmap → [`documents/03-planning/`](../../03-planning/)
- Naming: `YYYY-MM-DD-<topic>.md` (ISO 8601 date lead) hoặc `AUDIT-NNN-...` nếu indexed.
- Mọi audit MỚI phải thêm row vào `audits-index.csv` (per `meta-csv-index-pattern.md` 100% coverage).

---

## Archive Policy

Per [`docs-archival-cadence.md`](../../../.claude/rules/docs-archival-cadence.md) §2: audit reports archive khi **≥90 ngày tuổi** → `documents/04-quality/audits/{category}/closed/{year}-Q{N}/`. Landmark/compliance audit có thể giữ in-place (override trailer). Audit là **immutable evidence** — không sửa nội dung sau ship, chỉ annotate.

---

## Key Documents

- [`audits-index.csv`](audits-index.csv) — index canonical mọi audit (status + score + date + gap links)
- [`output-review-mandate.md`](../../../.claude/rules/output-review-mandate.md) §3 — bảng review-standard cho mọi output type (volatile audit state ở CSV)
- [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) — quy trình Issue → Gap → Fix
