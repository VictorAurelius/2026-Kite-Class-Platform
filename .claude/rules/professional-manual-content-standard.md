---
paths:
  - "documents/05-guides/professional-manual/**"
  - "documents/05-guides/dev/**"
  - "documents/05-guides/integration/**"
  - "documents/05-guides/operations/**/*-runbook.md"
---

# Professional Manual Content Standard — 15-item checklist + audience discoverability matrix (sister rule to `user-manual-content-standard.md`)

**Priority:** 🟠 MANDATORY — internal/professional doc content governance
**Version:** 1.0.0
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-18
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; sister rule cho `user-manual-content-standard.md` v1.0.0 — codify split rule cho Manual split queue item 2026-05-17 (inside-out-queue.md). Built-in enforcement (15-item checklist adapted professional audience + audience discoverability matrix + reviewer-checklist + worked self-test on existing professional guide samples) per §6.5 Enforcement Parity Mandate; no constraint loosening — closes coverage gap surfaced by Wave 87 planning user direct "manual hiện tại text-only không đủ cho 2 audience".)
**Applies to:** Mọi trang professional/technical manual cho audience nội bộ + integrators + technical operators. Scope = `documents/05-guides/professional-manual/**` (canonical professional manual), `documents/05-guides/dev/**` (dev integration guides), `documents/05-guides/integration/**` (3rd-party integrators), `documents/05-guides/operations/**/*-runbook.md` (ops runbooks). Tenant-facing end-user help content (P1/P2/P3 personas) thuộc scope `user-manual-content-standard.md` — KHÔNG cover ở đây.

---

## 1. The Rule

> **Mọi trang professional manual (technical user, dev integrator, admin operator, founder/tester audience) PHẢI đáp ứng đủ 15-item checklist §2 trước khi merge.** Reviewer enforces per-page tại pre-merge; CI grep detector deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày.

Professional manual = technical/architecture/integration content audience nội bộ đọc khi cần hiểu hệ thống ở mức kỹ thuật. KHÁC user manual ở chỗ:
- **Audience:** technical (dev / ops / integrator / founder) thay vì end-user (chị Hằng / anh Tâm / em Vy)
- **Format:** text-heavy + diagram + code snippet thay vì screenshot-heavy + annotated UI
- **Language tone:** technical lexicon broader (architecture / data flow / API / SQL) — English technical token acceptable trong code examples; narrative vẫn Vietnamese per `dev-readable-doc-language.md`
- **Cognitive load:** giả định audience có background technical — không cần TL;DR ≤80 từ; có thể wall-of-text với phân đoạn logic tốt
- **Discoverability:** discoverable qua GitHub navigation + repo search, không cần in-app entry points

Sister rule `user-manual-content-standard.md` cover end-user scope. Force-multiplier: 1 chuẩn chung cho mọi professional manual → eliminate retroactive rework cost khi audience nội bộ phát hiện gaps.

---

## 2. The 15-item checklist (mandatory per page)

Mỗi `.md` page trong scope §"Applies to" PHẢI satisfy:

### Foundation (5 items)

1. **Frontmatter mandatory fields:**
   ```yaml
   ---
   audience: dev | ops | integrator | founder | tester | architect
   topic: <short-slug>
   last-updated: YYYY-MM-DD
   version: <doc-version-or-app-version>
   effort_minutes: <estimated read time>
   ---
   ```
   `last-updated` PHẢI match session date per `session-currentdate-check.md` §1 (không forward-date).

2. **Scope/Purpose box** đầu page (within first 300 words):
   - 1 đoạn mô tả purpose: "Trang này mô tả ..."
   - 3-5 bullet liệt kê audience + use case
   - Liệt kê prerequisites (knowledge / tooling / access required)
   - Có thể ≤ 200 từ — audience kỹ thuật chịu được text dense

3. **Audience-specific organization** — section structure theo audience:
   - **Dev integration guide:** Quick Start → API Reference → Examples → Troubleshooting → FAQ
   - **Architecture doc:** Context → Diagrams → Components → Data Flow → Trade-offs → ADR refs
   - **Ops runbook:** When to run → Pre-flight checks → Steps → Verification → Rollback → Troubleshooting
   - **Integration guide:** Setup → Auth → Endpoints → Webhook handling → Rate limits → Error codes

4. **Vietnamese narrative** per `dev-readable-doc-language.md` §2-§4 (dev-readable doc scope). Technical token (HTTP / API / JWT / SQL / commit-shaped) giữ English; code blocks giữ English naturally; mixed-language code-switching trong câu acceptable.

5. **Reference + cross-link footer** mỗi page (cuối content):
   ```markdown
   ---
   ## 📚 Tham khảo

   - **Source code:** [<link-to-source>](path)
   - **API contract:** [<link-to-api-contract>](path)
   - **ADRs liên quan:** [ADR-NNN](path) — <topic>
   - **Sister docs:** [<related-doc>](path)
   - **Báo lỗi / cải tiến:** [GitHub Issue](link) hoặc Slack #dev-platform
   ```

### Visual + Technical Media (3 items)

6. **Diagrams thay vì screenshots** (NOT annotated UI):
   - Architecture: component diagram, sequence diagram, deployment diagram (PlantUML preferred, generate to `06-diagrams/`)
   - Data flow: ER diagram, state machine, message bus topology
   - Code/config: code blocks với syntax highlighting
   - File path: `documents/06-diagrams/professional-manual/<topic>-<diagram-type>.{puml,png}`
   - Tooling: PlantUML / Mermaid / Excalidraw OK; KHÔNG dùng screenshot UI cho professional manual (đó là user manual scope)
   - **Phase 1 BETA allowance:** placeholder reference (`<!-- Diagram: <topic>-flow.puml — TODO Phase 1 BETA -->`) acceptable khi PlantUML chưa render, follow-up tracked

7. **Code examples runnable + tested**:
   - Code snippet PHẢI có ngôn ngữ tag: ```bash, ```java, ```typescript, ```sql, ```yaml
   - Code phải syntactically valid (qua linter / parser)
   - Khi có thể, code phải runnable (real commands user copy-paste được)
   - Sample data: realistic technical values (UUID, ISO date, JWT, status enums) — KHÔNG dùng "foo/bar/baz" lazy placeholder
   - ❌ BANNED: pseudocode khi real code available, "// implementation here" không có chi tiết

8. **Technical accuracy + version sync**:
   - Reference version cụ thể: `Spring Boot 3.5.0`, `PostgreSQL 16`, `Node 22.x`
   - API endpoint match `api-contract.md` exactly (không drift)
   - Config keys match `application.yml` exactly
   - Deprecated patterns rõ ràng marked: `> ⚠️ DEPRECATED — see <new-pattern>`
   - Date format: ISO 8601 `2026-05-18` trong technical content (vs date long format cho user manual)

### Trust + Discoverability (4 items)

9. **Last-updated badge + version metadata** visible top of page (after frontmatter):
   ```markdown
   > 📅 Cập nhật: **{YYYY-MM-DD}** · Áp dụng cho: **{app-version}** · Audience: **{audience}** · Đọc khoảng **{effort_minutes} phút**
   ```

10. **Discoverability ≥3 entry points** (per §3 audience matrix):
    - Professional audience tìm docs qua: repo navigation / GitHub search / cross-link từ source code
    - 3 entry points: 1 từ `documents/05-guides/README.md` index + 1 từ source-code javadoc/comment link + 1 từ related sister doc

11. **Technical accuracy verification**:
    - Code examples tested OR marked "untested example" + reason
    - Diagrams generated từ source (PlantUML/Mermaid commit + rendered output)
    - References tới source code valid (link không 404)
    - API endpoint exists trong `api-contract.md` OR Controller class
    - Cross-checked với related ADRs (no contradiction)

12. **Search functional**:
    - GitHub built-in search (default fallback, free)
    - Phase 1.5+: dedicated docs search (Algolia DocSearch hoặc similar) nếu manual size scale up
    - Headings có anchor links cho deep-link sharing
    - Min 3 ký tự trigger search; debounce 300ms (nếu custom search)

### Format Discipline (3 items)

13. **GitHub-friendly Markdown rendering**:
    - Heading hierarchy đúng (H1 = page title, H2 = sections, H3 = subsections)
    - Tables render properly với `|` syntax
    - Code blocks có language tag
    - Links relative paths trong repo (NOT absolute GitHub URLs that break on rename)
    - Emoji minimal: cho UX cues only (📚 reference, ⚠️ warning, ✅ checked)

14. **Long-form structure**:
    - Page có TOC nếu >300 lines (auto-gen qua GitHub `[[_TOC_]]` hoặc manual ToC)
    - Section anchors qua heading `## Section Name {#section-name}` syntax (GitHub auto-anchors normally)
    - Footnotes cho references nếu quote external source heavy
    - Code blocks ≤ 80 lines một block (split nếu dài hơn)

15. **PDF generation optional (vs user manual mandatory)**:
    - Script `scripts/render-professional-manual-pdf.sh <audience>` optional cho heavy docs (architecture book)
    - Default: GitHub markdown rendering đủ cho dev/ops audience (đã quen đọc on-screen)
    - PDF chỉ generate khi có specific use case (offline architecture review, presentation handouts)
    - PDF gitignored nếu generated; regen on-demand pattern (per `test-artifact-format-standard.md` §4.2)

---

## 3. Audience discoverability matrix per audience

Mỗi audience PHẢI có ≥3 entry points cụ thể:

| Audience | Entry point 1 (repo) | Entry point 2 (source link) | Entry point 3 (cross-link) |
|---|---|---|---|
| **Dev / Integrator (Backend dev)** | `documents/05-guides/README.md` "Dev guides" section | Source javadoc `@see <doc-path>` annotation | Cross-link từ `api-contract.md` of related domain |
| **Ops / SRE** | `documents/05-guides/operations/README.md` "Runbooks" index | Cross-link từ `release-deploy-standard.md` related row | Cross-link từ `monitoring-dashboards.md` |
| **Architect / Tech Lead** | `documents/02-architecture/README.md` "Architecture docs" section | Cross-link từ ADR `Related artifacts` section | Cross-link từ `02-architecture/architecture.md` overview |
| **Founder / Tester** | `documents/05-guides/professional-manual/README.md` landing | Cross-link từ `README.md` repo root "For founders" section | Cross-link từ `documents/00-brd/README.md` |
| **3rd-party Integrator (Phase 1.5+)** | `documents/05-guides/integration/README.md` portal | Cross-link từ public `api-contract.md` published version | Public portal entry (nếu có) |

Bucket D Wave 92 scope: rule shipped với 15-item checklist + matrix; concrete professional manual content (Phase 1 BETA professional manual) defer Wave 88+ sister scope (Manual split queue item).

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| English narrative "View the API documentation" trong content | "Xem API documentation" Vietnamese narrative + technical token English |
| Annotated screenshots cho professional audience | Diagram (PlantUML/Mermaid) — dev không cần thấy UI button |
| Pseudocode "// query database, return result" | Real code: `return jdbcTemplate.query(SQL, mapper)` |
| Foo/bar/baz lazy placeholder | UUID format `a1b2c3d4-...`, ISO date `2026-05-18T10:30:00Z`, JWT prefix `Bearer eyJ...` |
| Wall-of-text 5 paragraph intro không có section | Scope box top + section headings logic |
| TL;DR ≤80 từ cho architecture doc | Architecture audience chịu được scope box ≤200 từ + technical depth |
| Manual cho mọi audience | Audience-specific organization per §2 row 3 |
| Manual không có cross-link tới source code | Reference footer §2 row 5 mandatory |
| Forward-date `last-updated` | Match session date per `session-currentdate-check.md` |
| Ship architecture doc không có diagram | Diagram per §2 row 6 mandatory (PlantUML/Mermaid OR placeholder + follow-up) |
| Code examples không có language tag | Markdown code block PHẢI có ` ```bash / ```java / ```sql` etc |
| Skip cross-reference ADR | ADR refs §2 row 5 mandatory khi doc touches architectural decision |
| Mix user-manual screenshot + professional content trong cùng page | Split: tenant-facing scope → `user-manual-content-standard.md`; technical scope → here |

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/05-guides/professional-manual/**` hoặc `documents/05-guides/dev/**` hoặc `documents/05-guides/integration/**` hoặc `documents/05-guides/operations/**/*-runbook.md`:

- [ ] §2 Foundation (5 items): frontmatter (audience field) + Scope/Purpose box + audience-specific organization + Vietnamese narrative + reference footer
- [ ] §2 Visual (3 items): diagrams (NOT screenshots) + runnable+tested code examples + technical accuracy + version sync
- [ ] §2 Trust (4 items): last-updated badge + audience metadata + ≥3 discoverability entry points + technical accuracy verification + search functional
- [ ] §2 Format (3 items): GitHub-friendly markdown + long-form structure (TOC if >300 lines) + PDF optional
- [ ] §3 audience matrix: entry points wired per audience scope
- [ ] Vietnamese narrative per `dev-readable-doc-language.md`
- [ ] `last-updated` ≤ session date per `session-currentdate-check.md`
- [ ] Sister rule `user-manual-content-standard.md` KHÔNG mis-apply (audience là technical, not tenant-facing)

### 5.2 Cross-reference `output-review-mandate.md` §3

This rule paired same-PR với new matrix row "Professional manual content" — review standard tracking.

### 5.3 Memory auto-load (optional, deferred)

Memory entry `feedback_professional_manual_content_standard.md` có thể remind session start before professional manual editing. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test đủ cho v1.0.0.

### 5.4 CI grep detector (deferred)

Future enhancement — heuristic regex tìm common anti-patterns:

```bash
# Detect placeholder pseudocode in professional manual scope
grep -rnE "// implementation here|// TODO: implement|foo\.bar\(|baz\(\)" \
  documents/05-guides/professional-manual/ \
  documents/05-guides/dev/ \
  documents/05-guides/integration/ \
  2>/dev/null \
  && { echo "WARN: pseudocode/placeholder detected — use real runnable code per professional-manual-content-standard.md §2 row 7"; exit 0; }

# Detect screenshot reference (should be diagram)
grep -rnE "!\[.*\]\(.*screenshot.*\.png\)|!\[.*\]\(.*ui-.*\.png\)" \
  documents/05-guides/professional-manual/ \
  2>/dev/null \
  && { echo "WARN: screenshot in professional manual — use diagram per §2 row 6"; exit 0; }
```

WARN-only (false positives expected). Track follow-up gap khi rule stabilize.

### 5.5 Override mechanism

Genuine exception (e.g., legacy doc partial migration, external doc copy):

```
git commit -m "...
PROFESSIONAL_MANUAL_STANDARD_OVERRIDE: <page-path> — <reason — e.g., legacy doc Phase 1 import, refactor Wave NN>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 6. Self-test (worked example — existing professional guide samples)

Apply 15-item checklist retroactively to existing professional manual samples:

| # | Checklist item | `02-architecture/architecture.md` | `05-guides/deploy/secrets-seeding-runbook.md` | `05-guides/operations/incident-response-runbook.md` |
|---|---|:---:|:---:|:---:|
| 1 | Frontmatter (audience + topic + last-updated + version + effort_minutes) | 🟡 partial (no `audience:`) | 🟡 partial | 🟡 partial |
| 2 | Scope/Purpose box ≤200 từ | ✅ | ✅ | ✅ |
| 3 | Audience-specific organization | ✅ architecture | ✅ runbook | ✅ runbook |
| 4 | Vietnamese narrative | ✅ | ✅ | ✅ |
| 5 | Reference footer | 🟡 partial (ADR refs có, source link incomplete) | ✅ | ✅ |
| 6 | Diagrams (NOT screenshots) | ✅ PlantUML | N/A (text-only) | N/A (text-only) |
| 7 | Code examples runnable + tested | ✅ | ✅ | ✅ |
| 8 | Technical accuracy + version sync | ✅ | ✅ | ✅ |
| 9 | Last-updated badge | 🟡 partial (no badge format) | 🟡 partial | 🟡 partial |
| 10 | ≥3 discoverability entry points | ✅ | ✅ | ✅ |
| 11 | Technical accuracy verification | ✅ | ✅ | ✅ |
| 12 | Search functional | ✅ GitHub default | ✅ GitHub default | ✅ GitHub default |
| 13 | GitHub-friendly markdown | ✅ | ✅ | ✅ |
| 14 | Long-form structure (TOC if >300 lines) | 🟡 partial (no TOC) | N/A (<300 lines) | ✅ |
| 15 | PDF generation optional | ✅ N/A | ✅ N/A | ✅ N/A |

**Verdict:** 3 sample existing docs satisfy 11/15 fully + 4/15 partial (audience frontmatter field + last-updated badge format + ToC syntax — minor format gaps, content quality solid). Self-test PASS ✅ — rule fires correctly trên originating scope + identifies retroactive polish items (track follow-up khi refresh docs).

**Counterfactual without rule:** Professional manual evolution Phase 1 BETA → Phase 1.5+ → Phase 2 sẽ drift theo từng author preference (Vietnamese vs English narrative, screenshot vs diagram, runnable vs pseudocode) → eventual user complaint "không đọc fluidly được" như Wave 72a Bucket F user-manual incident. Rule này codify standard 1 lần → force-multiplier mọi professional manual subsequent.

---

## 7. Relationship to other rules

- **`user-manual-content-standard.md`** v1.0.0 — SISTER rule. End-user scope (tenant-facing P1/P2/P3 personas) vs professional scope (technical audience nội bộ). Two non-overlapping audiences với two adapted checklists.
- **`dev-readable-doc-language.md`** §2 row "Runbooks" + "Audit reports" + "Planning doc body" — this rule extends Vietnamese narrative mandate cho professional scope specifically với English technical token broader acceptance trong code examples
- **`output-review-mandate.md`** §3 — paired same-PR với new matrix row "Professional manual content" tracking this rule's review standard
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (this rule precedes future professional manual gaps)
- **`session-currentdate-check.md`** §4.2 — `last-updated` field MUST match session date, banned forward-date
- **`docs-folder-structure.md`** §3 — `documents/05-guides/professional-manual/` follows folder README template
- **`test-artifact-format-standard.md`** §4.2 — PDF gitignored + regen-on-demand pattern reused here (optional vs user manual mandatory)
- **`gap-done-discipline.md`** §3 — professional manual sections defer = PARTIAL exit ramp acceptable per audience scope
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test all paired same PR
- **`incident-to-rule-pipeline.md`** — applied 5-stage: Detect ✓ (Manual split queue item 2026-05-17 in-chat) → Classify ✓ (no existing rule codifies professional manual content discipline; closest = `user-manual-content-standard.md` covers end-user scope only) → Rule+Enforce ✓ (this file + matrix row + rules-index row + 3 retroactive self-test samples) → Self-Test ✓ (§6 above) → Retro Log ✓ (§8 below)
- **`contract-first-for-cross-layer.md`** — when professional manual touches API contract, cross-link mandatory; this rule §2 row 8 enforces version sync

---

## 8. Log

- **2026-05-18 (v1.0.0):** Rule created in response to Manual split queue item 2026-05-17 (inside-out-queue.md) — user direct: "manual hiện tại text-only không đủ cho 2 audience. Cần tách 2 track: (1) Professional system manual ... (2) End-user manual ... Reference: `user-manual-content-standard.md` đã codify end-user scope; cần (a) extend rule với professional sister-scope HOẶC (b) create sister rule `professional-manual-content-standard.md`". Sister rule path chosen — single responsibility per `rule-change-process.md` §5.1 atomic-unique bar. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user direct queue item) → Classify ✓ (no existing rule codifies professional manual content discipline; closest = `user-manual-content-standard.md` covers end-user scope only — different audience, different format, different cognitive load assumptions) → Rule+Enforce ✓ (this file + 15-item checklist + audience discoverability matrix + paired same-PR: `output-review-mandate.md` §3 row "Professional manual content" + `rules-index.csv` row + queue file `status: consumed (Wave 92 Bucket D)` per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on 3 existing professional samples — 11/15 PASS + 4/15 partial polish items) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix standard 1 lần → force-multiplier mọi professional manual subsequent (Phase 1 BETA dev/ops/architect/integrator content). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered class (professional/internal manual scope); no constraint loosening; existing professional docs grandfathered, rule applies prospectively từ Wave 92 forward). Detector wiring (§5.4 CI grep + §5.3 memory auto-load) deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test sufficient cho v1.0.0.
