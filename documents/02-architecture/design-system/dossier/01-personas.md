# 01 — Personas

**Source:** `documents/00-brd/personas-catalog.md` v1 DRAFT (2026-04-14, GAP-150) + use-case synthesis from `documents/01-business/kiteclass/*/use-cases.md`. Secondary personas Student + Parent synthesized where BRD skeleton was empty.

**Use this when:** designing a screen — pick the persona whose JTBD the screen serves, then optimize for that persona's device + tech literacy + time-of-day. Don't design "generic dashboard" — design Owner-Dashboard, Teacher-Dashboard, Parent-Dashboard separately.

---

## Tier 1 — Tenant operators (use KiteHub or KiteClass admin/owner views)

### P1. Solo Teacher (Gia sư tự do)

| Attribute | Value |
|-----------|-------|
| Role | Part-time educator running 1-on-1 or small-group tutoring on their own |
| Scale | 5–50 students, 0 staff |
| JTBD | Schedule lessons · Track student progress · Collect payment per student · Send parent updates |
| Top 3 pains | Manual admin overhead steals teaching time · Payment tracking scattered across Zalo/spreadsheet/cash · No central place for parent communication |
| Devices | Desktop (admin work) + Mobile (notifications, quick check) |
| Tech literacy | Moderate — comfortable with Zalo, Google Forms, Facebook; wary of "complicated software" |
| Usage time | Evening/weekend (tutoring hours 6pm–9pm; admin Sunday morning) |
| Subscription tier | FREE or BASIC |
| Design implications | Mobile-friendly admin (one-handed Zalo replacement vibe). Plain Vietnamese. No jargon. Sparse-density layouts. |

### P2. Center Owner (Chủ trung tâm nhỏ)

| Attribute | Value |
|-----------|-------|
| Role | Owner-operator of a small education center; teaches some classes themselves |
| Scale | 1–3 teachers, 20–100 students, 1 location |
| JTBD | Manage multiple classes · Roster + attendance · Enroll students · Collect tuition · Manage 1-3 teaching staff · Marketing |
| Top 3 pains | Wears many hats with no admin support · Juggling Excel sheets for finance · Hard to track which student paid this month |
| Devices | Desktop primary (PC at center), tablet for evening admin |
| Tech literacy | High — has used spreadsheets for years, may have used 1-2 prior SaaS tools |
| Usage time | Business hours 9am–5pm + evening admin 8pm–10pm |
| Subscription tier | FREE or BASIC |
| Design implications | Density OK on desktop dashboards. Show financial KPIs prominently. Multi-class views. Need quick "today's classes" view. **This is the primary KiteHub customer persona.** |

### P3. Medium Center Admin (Quản lý vận hành trung tâm cỡ vừa)

| Attribute | Value |
|-----------|-------|
| Role | Dedicated operations manager (separate from owner); handles day-to-day admin |
| Scale | 5–20 teachers, 100–500 students, 1–3 locations |
| JTBD | Role-based team management · Multi-course catalog · Financial reporting · Teacher payroll · Marketing website · Parent escalation handling |
| Top 3 pains | Complex role hierarchy (owner / managers / teachers / TAs) · Consolidating reports across teachers manually · Payroll calc with commission % is painful |
| Devices | Desktop + Tablet (presentations to owner) |
| Tech literacy | High — likely has bookkeeping or HR background |
| Usage time | Business hours + evenings for monthly reports + year-end pushes |
| Subscription tier | BASIC or PREMIUM |
| Design implications | Bulk operations critical. Filterable tables. Export-to-Excel everywhere. Audit trail visible. Permissions UI clear. |

### P5. K–12 School Principal/Admin (Hiệu trưởng / Quản lý trường tư thục)

| Attribute | Value |
|-----------|-------|
| Role | Hierarchical institutional user at a private K–12 school |
| Scale | 50+ teachers, 500–3000 students, 1 main campus |
| JTBD | Bulk import students each academic year · Manage academic calendar (semester/term) · Issue official MoET-compliant grade reports · Monitor parent communication · Process annual fees · Conduct/behavior tracking |
| Top 3 pains | Data entry at scale (500 students/day during enrollment week) · Generating MoET-compliant report cards (legal requirement) · Handling parent escalations + conduct issues |
| Devices | Desktop + Admin Portal (large displays for school-wide views) |
| Tech literacy | High — institutional IT support available |
| Usage time | School hours continuous + year-end reporting pushes |
| Subscription tier | PREMIUM or ENTERPRISE |
| Design implications | Bulk-import flow MUST work for 10k rows. Official report card format mandatory (GAP-055). Hierarchical permissions (principal / vice / homeroom teacher / subject teacher). Class structure: grade-level + section (e.g., "Lớp 10A2"). |

---

## Tier 2 — End users (use KiteClass tenant app)

### S. Student (Học viên)

| Attribute | Value |
|-----------|-------|
| Role | Learner enrolled in a class (K–12, vocational, language center, etc.) |
| Age range | 6–22 (varies by tenant type) |
| JTBD | View today's schedule · Submit assignments · Check grades · Track attendance · Pay tuition (older students) · Receive teacher feedback |
| Top 3 pains | Limited visibility into progress · Unclear fee balance · Waiting for grade feedback |
| Devices | **Mobile primary** (~85% sessions) + Tablet (homework) |
| Tech literacy | Moderate (digital native but lacks admin software experience) |
| Usage time | Anytime — peaks at homework hours (7pm–10pm) and during breaks at school |
| Subscription | N/A (covered by tenant) |
| Design implications | **Mobile-first** (320–414px). Touch-friendly. Push notification for grade updates. Empty states friendly ("Chưa có bài tập mới — nghỉ ngơi tí nhé 🎈"). Avoid admin density. |

### Pa. Parent (Phụ huynh)

| Attribute | Value |
|-----------|-------|
| Role | Guardian of a student (linked via parent invite token) |
| Age range | 25–55 |
| JTBD | Monitor child's attendance + grades · Pay school fees · Communicate with homeroom teacher · View official report card |
| Top 3 pains | Fragmented communication (app + email + Zalo + SMS) · Hard to see child's standing at a glance · Late fee surprises |
| Devices | **Mobile primary** (~95% sessions). Some prefer Zalo OA notification over app push. |
| Tech literacy | Low–Moderate. Comfortable with Zalo, banking apps, Facebook. Wary of "yet another app." |
| Usage time | Evening/weekend after work (7pm–10pm). Quick check-ins during workday. |
| Subscription | N/A |
| Design implications | **Mobile-first, very simple.** One screen = one task. Big tap targets. Vietnamese only (no English fallback). Zalo notification preferred over native push. **Direction D core persona.** |

---

## Persona × Direction matrix

Use this when picking which UI kit to design for which persona.

| UI kit / Direction | Primary persona | Secondary persona |
|--------------------|----------------|-------------------|
| `kitehub` (existing recreation) | P2 Center Owner | P3 Medium Center Admin |
| `kitehub-story` (Direction A — marketing) | Prospects (pre-tenant) — likely P2 evaluating | P1 Solo Teacher evaluating BASIC tier |
| `kiteclass-pro` (Direction B — owner dashboard) | P2 Center Owner | P3 Medium Center Admin |
| `kiteclass-teacher` (Round 2 new) | Homeroom teacher (GVCN) | Subject teacher |
| `kiteclass-parent` (Round 2 new — was Direction D, pivoted) | Pa. Parent | — |
| `kiteclass-student` (Round 2 — Phase 2) | S. Student | — |
| `ai-branding-wizard-v2` (Direction C — integrated) | P2 Center Owner (first-time setup) | P3 Medium Center Admin (rebrand) |
| `kitehub-admin` (existing) | P5 K–12 School Principal | P3 Medium Center Admin |

**Anti-pattern:** "generic dashboard" that tries to serve P2 + Teacher + Parent simultaneously. Each role gets its own home.

---

## What this dossier file is NOT

- Not market research — that's `documents/00-brd/market-segmentation.md` (filed under GAP-150).
- Not user testing transcripts — none exist yet (planned Wave 5+).
- Not buyer personas for marketing campaigns — that's the marketing team's separate doc.

If Round 2 needs deeper persona context (e.g., "what does P5 do during enrollment week"), ask the user to fill in BRD blanks rather than synthesizing.
