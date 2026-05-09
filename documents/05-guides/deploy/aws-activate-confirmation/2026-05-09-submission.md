---
title: AWS Activate Founder Application — Submission Log
date: 2026-05-09
time: 17:19 ICT
status: submitted, pending approval
gap: GAP-412
---

# AWS Activate Founder — Submission 2026-05-09

## Submission details

| Field | Value |
|---|---|
| **Tier** | Activate Founder ($1,000 USD credits) |
| **URL used** | https://aws.amazon.com/startups/credits/ (rebrand from /activate/founders-pack/) |
| **AWS account ID** | 906286017800 |
| **Submission timestamp** | 2026-05-09 17:19 ICT (UTC+7) |
| **Approval lead time** | 1-2 weeks typical |
| **Calendar reminder** | 2026-05-23 10:00 ICT (D+14, set via Google Calendar MCP) |

## Form fields submitted

| Field | Value |
|---|---|
| Company name | KiteClass |
| City | Ha Noi |
| Country | VN |
| Industry | Education |
| Founded date | 2026-01-01 |
| Number of employees | 1 - 10 |
| Founder | Yes |
| Job title | Software Engineer |
| Customer type | Education (Ed Tech / EdTech) |
| Primary target customers | Businesses (B2B) |
| Product stage | Ideating/building initial product |
| Customer interaction | Web browser |
| AI/ML usage | Yes |
| Funding round | No Funding |
| Valuation | No Valuation |
| % AWS spend | 75 - 99% |
| Annual marketing spend | < $250,000 |
| Planned product launch | 2026-07-01 |
| Most recent funding date | (blank — bootstrapped) |
| AWS years | 0 - 2 yrs |
| AWS top-3 ways helps | Building scalable architecture / Reduce infrastructure costs / Validating product concept |
| Website | https://kitehub.me |
| LinkedIn | https://www.linkedin.com/in/victoraurelius/ |

## Description text submitted (238 chars, ≤250 budget)

> Vietnamese-first multi-tenant SaaS for ~30,000 education centers managing students, attendance, grades, parents, payments. PDPL-compliant, MoET-aware. Replaces Excel/Zalo. Solo dev, pre-revenue, AWS Singapore Phase 1 BETA launch May 2026.

## Pitch deck attached

- Source: `documents/00-brd/kite-pitch-deck.md`
- Rendered PDF: `kite-pitch-deck.pdf` (95KB, 3 pages, A4)
- Render command: `npx -y md-to-pdf <source.md> --pdf-options '{"format":"A4","margin":{"top":"20mm","bottom":"20mm","left":"20mm","right":"20mm"}}' --launch-options '{"args":["--no-sandbox"]}'`

## Next actions

1. **D+14 (2026-05-23)** — Calendar reminder fires; check approval status
2. **If approved:**
   - Screenshot https://console.aws.amazon.com/billing/home#/credits
   - Save as `2026-MM-DD-approval.png` cùng folder này
   - Flip GAP-412 status PARTIAL → 🟢 DONE
   - Resume EC2 + RDS từ cost-save
3. **If denied:**
   - Read denial reason
   - Re-submit với enhanced pitch deck nếu cần
   - Pivot Activate Builder ($300 tier) nếu Founders denied repeatedly
4. **If no response D+21 (2026-05-30):**
   - Escalate qua AWS Support contact form

## Compliance check

- ✅ `aws-activate-credit-policy.md` §2 Application Checklist met (account active + pitch deck + AWS account ID)
- ✅ `agent-action-bias.md` §3 row 2 — agent helped với drafting/PDF rendering, user submitted (form cần GitHub OAuth + identity verify, agent không làm thay được)
- ✅ `gap-done-discipline.md` §3 PARTIAL exit ramp — submission shipped, approval pending → status stays PARTIAL với explicit pending criterion
