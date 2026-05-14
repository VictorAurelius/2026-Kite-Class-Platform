# Agent Action Bias — do it yourself, prefer command over UI

**Priority:** 🟠 MANDATORY — agent behavior governance
**Version:** 1.0.0
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every Claude turn that involves environment setup, configuration, file editing, system administration, repo state inspection, or any action where Claude has tool access AND the user could reasonably expect Claude to perform it

---

## 1. The Rule (two parts, both mandatory)

> **Part A — Do it yourself.** If Claude has the tools to perform an action, Claude performs it. Do NOT instruct the user to do it manually unless one of the §3 exceptions applies.
>
> **Part B — Command over UI.** If an action can be accomplished via command line, file edit, or API call, Claude MUST take that path. Do NOT propose UI/GUI workflows when a command path exists.

This rule closes a recurring offload pattern surfaced 2026-05-07 in the WSL shared-dev migration session: Claude told the user "Docker Desktop → Settings → Resources → WSL Integration → toggle for distro" instead of editing `settings-store.json` directly via `jq` + restarting via `taskkill.exe` + `start "" "Docker Desktop.exe"`. The user got stuck in a UI loop, asked "có cách nào bằng lệnh không?", and Claude immediately produced a working command path. The command path existed all along — it should have been the first proposal.

---

## 2. Concrete examples (banned vs preferred)

| ❌ BANNED (offload + UI) | ✅ REQUIRED (do it + command) |
|---|---|
| "Open Docker Desktop → Settings → toggle WSL Integration" | `jq` edit of `settings-store.json` + `taskkill.exe` + restart via `cmd.exe /c start` |
| "Open the file in your editor and add field X" | `Edit` tool with old_string/new_string |
| "Run this command yourself: `git push`" (when git push is in user's permission allowlist) | Claude runs `git push` via Bash tool |
| "Visit github.com/.../settings/secrets to add VAR" | `gh secret set VAR -b "..."` (or `gh api`) |
| "Open your shell rc and append `export X=Y`" | `Edit` tool on `~/.bashrc` |
| "In the GitHub UI, click Merge button" | `gh pr merge <n> --squash` (when merge is sanctioned) |
| "Configure DNS records via your registrar's web panel" (if registrar has API) | API call via curl/CLI; only fall back to UI if no API exists |
| "Restart your IDE manually" (if IDE has CLI restart) | CLI command |
| "Open browser and authenticate" (when token already in env) | Use the env token |
| "Edit the YAML file to set foo: bar" | `Edit` tool, exact diff |

The general shape: Claude says "tôi sẽ làm X" and does X, instead of "bạn làm X" or "hãy click Y".

---

## 3. Allowed exceptions (use sparingly, document inline)

UI proposal or user-execute is allowed ONLY when:

| Case | Why exempt | Example |
|------|------------|---------|
| **No command path exists** | Vendor truly has UI-only flow | OAuth consent screens; some bank/regulator portals; Cloudflare account creation; AWS root account signup |
| **Authentication interactive by design** | Browser-based 2FA / SSO / OAuth | `gh auth login --web`, `gcloud auth login`, browser MFA prompt |
| **Permission denied — Claude lacks the tool** | Tool not granted in this session's allowlist; OR sandbox blocks it | User-only allowed `git push --force`; sudo without NOPASSWD |
| **Action requires physical-world step** | Hardware, hand verification, payment | Plug in YubiKey, call support hotline, ship paperwork |
| **Risk gate: action requires user confirmation per CLAUDE.md "Executing actions with care"** | Destructive/shared-state action without prior authorization | `rm -rf`, force-push, posting to public channels |
| **User explicitly asked for instructions, not execution** | "Hướng dẫn tôi cách làm X" / "show me the steps" | Don't execute; provide steps |

When invoking an exception, **state which row applies** in one phrase: "no API exists for X" / "this is OAuth, browser required" / "destructive — confirm trước". Don't silently default to UI.

---

## 4. Decision flow

Before writing "open ... and click ..." or "run this yourself":

1. **Can I do it with a tool I have right now?** (Bash / Edit / Write / gh CLI / docker / API call)
   - YES → do it. State what you're doing in one sentence, then act.
   - NO → continue.
2. **Is there a command/CLI/API path that achieves the same result?**
   - YES → propose that path (with the exact command), don't propose UI.
   - NO → continue.
3. **Does an exception in §3 apply?**
   - YES → state the row + ask user to perform the minimal step.
   - NO → revisit step 1; you probably missed a tool.

Most "I'll have you do it" instincts fail step 1 — the tool was available, the agent just didn't think of it.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Default to UI walkthroughs because they "feel safer" | Default to command — fewer steps, less ambiguous, reproducible |
| Tell user to edit a file when `Edit` tool is available | Use `Edit` with exact old/new strings |
| Suggest a 5-click GUI flow when a 1-line CLI does it | Run the 1-line CLI |
| Tell user "you can do X yourself faster" — paternalism | Do X. Time-cost belongs to the agent, not the user |
| Hide behind "I don't want to break anything" without trying | Try the safe path (read, dry-run, backup-then-edit). Confirm only for irreversible actions per CLAUDE.md "Executing actions with care" |
| Loop the user through a UI that's known to glitch | Edit the underlying config file; restart the process via CLI |
| Use `Bash` to `echo "edit this file"` instructions | Use `Edit` to actually edit the file |

---

## 6. Self-test (worked example — 2026-05-07 Docker WSL Integration)

**Scenario:** User said "tôi bật bằng UI thì bị quay vòng, có cách nào bằng lệnh không?"

**Apply §4 decision flow at the original moment Claude proposed UI:**

1. **Can I do it with a tool I have?**
   - Docker Desktop config = JSON file at `/mnt/c/Users/ADMIN/AppData/Roaming/Docker/settings-store.json` → ✅ accessible from WSL via `/mnt/c`
   - Tool to edit it = `Edit` or `jq` via Bash → ✅ available
   - Tool to restart Docker Desktop = `taskkill.exe` + `cmd.exe /c start` (Windows binaries reachable via `/mnt/c/Windows/System32/`) → ✅ available
   - Verdict: **YES, all tools available — should have done it directly the first time.**

2. **What I should have said the first time:**
   > "Tôi sửa `settings-store.json` để add `shared-dev` vào `IntegratedWslDistros`, rồi restart Docker Desktop qua `taskkill.exe` + `cmd.exe`. Verify bằng `docker ps`."

3. **What I actually said the first time:**
   > "Cần làm trên Windows: Docker Desktop → Settings → Resources → WSL Integration → bật toggle..."

   → Violates §1 Part B (UI proposal when command path exists) AND §1 Part A (offloads work Claude could perform).

4. **Cost of the miss:** user got stuck in UI loop, had to escalate, agent's correction round-trip = 2 extra turns. With this rule: zero loop, single turn.

→ **Rule fires correctly on the original incident.** ✅

---

## 7. Enforcement

### 7.1 Memory auto-load (per-session)

Memory entry `feedback_agent_action_bias.md` (paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) loads at session start. 4-bullet checklist before any "user, please do X" or "open the UI" phrasing.

### 7.2 Reviewer / user manual

When Claude responds with offload-to-user wording or UI walkthroughs, user is encouraged to flag with:
> "Bạn làm được không?" / "Có cách nào bằng lệnh không?"

Pattern repeated → file follow-up gap referencing this rule for retro analysis.

### 7.3 Self-detection (in-turn)

Before sending a response that contains phrases like "open ...", "click ...", "navigate to ...", "you can do this yourself", "in your editor", "via the UI", "in Settings → ...", Claude must run the §4 decision flow mentally. If §4 step 1 or step 2 returns YES, rewrite the response to do/command instead of offload/UI.

### 7.4 Override mechanism

For genuine §3 exceptions, document inline:

```
AGENT_ACTION_OVERRIDE: <reason — e.g. "OAuth browser-only, no API path">
```

Or in narrative form: "Bước này cần browser auth (no CLI path); bạn click ... để approve."

Pattern frequency >5% of agent actions per session triggers meta-review of §3 exceptions list.

---

## 8. Relationship to other rules

- **`mcp-first-with-fallback.md`** — same family of rule (tool-selection priority); this rule is broader (action delegation), that rule is narrower (which tool flavor). Both apply: prefer MCP/dedicated tools over Bash AND prefer command over UI.
- **CLAUDE.md "Executing actions with care"** — risk gate prevents destructive/shared-state actions without confirmation. This rule does NOT override that gate; §3 row 5 explicitly defers.
- **`agent-background-spawn-default.md`** — companion rule on agent invocation pattern (foreground vs background). Both target reducing user wait + parent context cost.
- **`incident-to-rule-pipeline.md`** — this rule is direct output of 2026-05-07 Docker WSL UI-loop incident applied through 5-stage pipeline.
- **`rule-change-process.md`** §6.5 — Enforcement Parity Mandate; this rule + memory + self-test all land same PR.
- **`output-review-mandate.md`** — every output requires evidence; agent-action choice is part of the output review surface.

---

## 9. Log

- **2026-05-07 (v1.0.0):** Rule created at user request "thêm rules, cái gì bạn làm được thì cấm không bắt user tự làm; cái gì làm được bằng lệnh thì cấm đề xuất làm bằng UI" — direct response to Docker Desktop WSL Integration UI-loop incident in the same session. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged miss when UI walkthrough looped) → Classify ✓ (no existing rule covers offload-to-user OR UI-vs-command tool selection bias; `mcp-first-with-fallback.md` is closest but covers tool flavor not action delegation) → Rule+Enforce ✓ (this file + memory `feedback_agent_action_bias.md` paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 worked example on the originating incident) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint, no constraint loosening; existing patterns grandfathered, rule applies prospectively from this PR forward).
