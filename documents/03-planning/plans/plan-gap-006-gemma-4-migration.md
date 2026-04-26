---
title: GAP-006 Gemma 4 9B Migration — Session Prep + Brainstorm
status: draft
created: 2026-04-26
updated: 2026-04-26
gaps: [GAP-006]
parent_gap: GAP-223
session_id: 20260426-164325
---

# GAP-006 Gemma 4 9B Migration — Session Prep

**Status:** Brainstorm + pre-flight done; execution deferred (infra blocker — local Ollama not installed, dev Docker stack down).

Ngữ cảnh: GAP-006 unblocked 2026-04-26 sau khi Sub-PR 223.1 ship governance scaffold (skill `quality/ai-branding-quality-gate` + audit-gate rule + `ai-branding-guidelines.md` §11.4 + baseline 62/100). Session 2026-04-26 picked GAP-006 từ Wave 7 priority queue, brainstorm + task breakdown xong nhưng phát hiện infra blocker → user chuyển sang gap khác.

---

## 1. Pre-flight Findings (2026-04-26 16:45 UTC)

| Check | Result | Implication |
|-------|--------|-------------|
| `which ollama` | empty | Ollama không cài local |
| `curl localhost:11434/api/tags` | empty (timeout) | Ollama không chạy |
| `docker ps` | 1 unrelated container (`laughing_bhabha`) | Kite Docker stack down |
| Disk space | 946GB free | OK cho ~14GB models |
| WSL2 GPU | none | Inference CPU-only — slow (60-120s/sample/9B) |

**Blocker:** Cannot run pre-migration A/B test (AC #1) without Ollama up + 2 models pulled.

---

## 2. Brainstorm

### Scope (per GAP-006 line 153-163, 11 AC)

3 phases:
- **Phase A (research, ~1-2h):** Ollama up → pull `gemma4:9b` + `mixsura` → A/B 10 VN samples → tool-calling test → quyết định primary model
- **Phase B (migration, ~30 min):** Update 8 files (2 docker-compose + 6 trong `kitehub-branding`) + Java tests adjust
- **Phase C (verify, ~1-2h):** Benchmarks (latency, RAM, concurrent) → `/ai-branding-quality-gate` skill chạy → score ≥70 → docs (`rules.md`, `ai-local-implementation-plan.md`) → memory feedback

### Risks

1. **Inference slow trên WSL2 CPU** — không có GPU → 9B model có thể 30-120s/sample. 10 samples × 2 models = 10-40 min wall-clock chỉ generation.
2. **MixSura RAM unknown** — không document trong gap, có thể >8GB.
3. **Tool-calling Ollama support** — Gemma 4 9B claim built-in, nhưng Ollama wrap phải verify (có thể vẫn manual prompt).
4. **Quality-gate skill (62/100 baseline)** — mới ship sáng nay (Sub-PR 223.1), score thực sau migration có thể tệ hơn baseline → BLOCK migration.
5. **Manual VN rubric scoring** — bias 1-người, cần rubric chặt; nếu Gemma 4 9B vs MixSura sát nhau, decision khó.

### Edge cases

- Gemma 4 9B fail tool-calling → fallback manual prompts (giống llama3.1) → bỏ tool-calling AC nhưng vẫn migrate vì RAM win
- MixSura output quality không vượt Gemma 4 9B ≥10% → primary = Gemma 4 9B (confirmed)
- Quality-gate <70 → block + filed regression gap
- WSL2 inference quá chậm → cân nhắc deploy lên Oracle ARM trước A/B (added complexity)

---

## 3. Task Breakdown

| # | Task | Effort | Output |
|---|---|---|---|
| 1 | Feature branch + scratch dir setup | 5min | `feature/GAP-006-gemma-4-migration` |
| 2 | Ollama Docker compose + pull `gemma4:9b` | 20min | `kite-ollama` running, model loaded |
| 3 | A/B test script (10 VN prompts + rubric) | 30min | `scripts/ai-branding-ab-test.sh` + `documents/04-quality/audits/ai-branding/2026-04-26-gap006-ab-prompts.md` |
| 4 | Run Gemma 4 9B 10 samples | 15-40min | output JSON saved |
| 5 | Pull MixSura + run 10 samples | 30-60min | output JSON saved |
| 6 | Score manual rubric + decision | 20min | A/B report + primary model decision |
| 7 | Tool-calling integration test | 20min | verify + decision (built-in vs manual) |
| 8 | Update 8 files + Java tests | 30min | code change |
| 9 | Benchmarks (latency, RAM, concurrent) | 20min | benchmark report |
| 10 | `/ai-branding-quality-gate` skill run | 30min | gate report ≥70 |
| 11 | Update `rules.md` + `ai-local-implementation-plan.md` + memory | 20min | docs synced |
| 12 | Living docs gate + PR | 15min | PR created |

**Total estimate:** 4-6h realistic.

---

## 4. Wave-eligibility Verdict (per `/continue` Step 0)

| Q | Answer |
|---|--------|
| ≥3 sub-tasks? | ✅ YES (12 tasks) |
| Disjoint files? | ❌ NO — A/B kết quả gate code change; benchmark phải chạy SAU migration; quality-gate report SAU A/B+tool-calling |
| Self-contained TDD/build cycle? | ❌ NO — sequential dependency chain |

→ **NOT wave-eligible** (1/3). Phải chạy serial qua /continue khi infra ready.

---

## 5. Resume Conditions

Trước khi pick lại GAP-006:

1. ✅ Local Ollama installed (`curl -fsSL https://ollama.com/install.sh | sh`) HOẶC dev Docker stack up (`./scripts/up.sh` từ `kitehub/`) với `kite-ollama` service
2. ✅ Disk ≥20GB free (cho gemma4:9b ~6GB + mixsura ~8GB + buffer)
3. ✅ WSL2 RAM ≥12GB available HOẶC alternative inference target (Oracle ARM, AWS g4dn)
4. ✅ Manual VN rubric finalized (cultural fit, grammar, tone, brand-safety) — chưa làm session này
5. ✅ 10 representative VN sample prompts drafted — chưa làm session này

**Recommended next step khi resume:** Bắt đầu task #1-2 (branch + Ollama setup + pull `gemma4:9b` only — defer MixSura pull tới sau khi Gemma working).

---

## 6. Alternative Path (nếu defer A/B test)

Nếu user accept rủi ro skip A/B test:
- Migrate code ngay với `gemma4:9b` (không A/B)
- Document deferred MixSura comparison là follow-up gap
- Score migration với existing `/ai-branding-quality-gate` (manual checklist)
- Risk: VN content quality regression không phát hiện sớm

**Not recommended** — vi phạm `ai-branding-guidelines.md` §11.4.1 Output behavior consistency (5 sample outputs minimum + A/B vs baseline).

---

## 7. References

- Gap: `documents/04-quality/gaps/GAP-006-upgrade-to-gemma-4.md`
- Parent: `documents/04-quality/gaps/GAP-223-ai-branding-migration-verification-governance.md`
- Skill: `.claude/skills/quality/ai-branding-quality-gate/SKILL.md`
- Rule: `.claude/rules/ai-branding-guidelines.md` §11.4 Migration test checklist
- Baseline: `documents/04-quality/audits/ai-branding/2026-04-26-baseline.md` (62/100)
- Memory: `feedback_ai_branding_governance_gap.md`

---

## 8. Log

- **2026-04-26 16:45 UTC** — Brainstorm + task breakdown done. Pre-flight blocker discovered (local Ollama not installed + Docker stack down). Session deferred to khi infra ready. Logged for future resume.
