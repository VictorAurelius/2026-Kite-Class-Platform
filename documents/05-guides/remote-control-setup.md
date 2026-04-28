# Claude Code Remote Control — Setup Guide

Điều khiển Claude Code session từ mobile (iOS/Android) hoặc browser khi đi ra ngoài. Built-in feature, không cần SSH/tmux.

**Yêu cầu:**
- Claude Code v2.1.51+ (`claude --version`)
- Pro/Max/Team/Enterprise subscription (API key NOT supported)
- Đăng nhập qua `claude auth login` (không dùng `ANTHROPIC_API_KEY`)
- Mobile: Claude app (iOS/Android) hoặc browser truy cập `claude.ai/code`

---

## Quick Start (1 dòng — copy nguyên dòng)

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode bypassPermissions
```

→ QR code + URL hiện ra. Mobile scan/mở → done.

**Defaults sử dụng trong project này:**
- `--permission-mode bypassPermissions` — full auto **không hỏi gì cả** (Read/Edit/Write **và** Bash). Solo-dev workflow đã được audit-gate hook + `gap-done-discipline.md` + CI gate bảo vệ → không cần per-tool prompt làm chậm mobile flow.
- KHÔNG set `--capacity` → unlimited concurrent sessions. Bỏ giới hạn 5-session cũ vì wave-pack cluster có khi spawn 6-8 agents song song.
- `--spawn worktree` — mỗi mobile session = worktree branch riêng (tránh contamination per `feedback_worktree_absolute_path_contamination.md`).

> ⚠️ **Cẩn trọng `bypassPermissions`:** mode này cho phép Claude chạy mọi Bash command (kể cả `rm -rf`, `git push --force`, `gh pr merge`) **không hỏi**. An toàn vì:
> 1. `audit-gate.py` hook block destructive patterns trước khi execute
> 2. Branch protection (khi enable) chặn force-push to main
> 3. Wave plan + commit history reversible
>
> Nếu chạy task không quen / repo lạ → fallback `acceptEdits` hoặc `default` (xem §Variants).

---

## Recommended Setup (parallel-agent workflow)

**Cách 1 — Single line (an toàn nhất, copy nguyên dòng):**

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode bypassPermissions
```

**Cách 2 — Multi-line (chỉ paste từ raw markdown, KHÔNG paste từ formatted):**

```bash
claude remote-control \
  --name "Kite Class Platform" \
  --spawn worktree \
  --permission-mode bypassPermissions
```

⚠️ **Cảnh báo về backslash continuation:** Khi copy từ HTML/formatted output sang terminal, ký tự sau `\` đôi khi bị space ẩn → terminal hiểu sai. Nếu lỗi `Unknown argument`, dùng Cách 1 (1 dòng).

> **Project policy:** mặc định **`bypassPermissions`** (full auto, không hỏi gì) + **unlimited capacity** (bỏ `--capacity`). Override sang `acceptEdits` / `default` chỉ khi review code lạ — xem §Variants bên dưới.

---

## Flags giải thích

| Flag | Tác dụng |
|------|---------|
| `--name "..."` | Tên hiển thị trên `claude.ai/code` (phân biệt project) |
| `--spawn worktree` | Mỗi session mobile = worktree + branch riêng (khuyến nghị cho parallel work) |
| `--spawn same-dir` | Mọi session share repo dir (default — đơn giản, 1 task tại 1 lúc) |
| `--spawn session` | Session ephemeral (Q&A nhanh, không commit) |
| `--capacity N` | Max concurrent sessions. **Không set → unlimited (project default)**. Set N nếu cần cap để bảo vệ máy yếu. |
| `--permission-mode default` | Prompt từng tool — phải approve trên mobile (chậm, fallback khi review code lạ) |
| `--permission-mode acceptEdits` | Auto Read/Edit/Write, vẫn prompt Bash — fallback khi không tin tưởng full bypass |
| `--permission-mode auto` | Auto allow tools đã từng approve (kế thừa từ session trước) |
| `--permission-mode bypassPermissions` | **Project default** — full auto kể cả Bash. An toàn nhờ audit-gate hook chặn destructive patterns |
| `--permission-mode plan` | Plan mode — không execute |

---

## Variants — Copy theo nhu cầu

### Project default (full auto, unlimited capacity):

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode bypassPermissions
```

### Cần capacity cap (máy yếu hoặc tránh resource exhaustion):

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --capacity 5 --permission-mode bypassPermissions
```

### Auto edit only (vẫn prompt Bash — khi không tin tưởng full bypass):

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode acceptEdits
```

### Strict (prompt mọi tool — chỉ khi review code lạ):

```bash
claude remote-control --name "Kite Class Platform" --permission-mode default
```

### Verbose debug (khi troubleshoot):

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode bypassPermissions --verbose --debug-file /tmp/claude-rc.log
```

---

## Bật từ trong session đang chạy

Nếu đang trong Claude Code session, không cần thoát:

```
/remote-control "Kite Class Platform"
```

Slash command sẽ expose session hiện tại ra mobile.

---

## Troubleshooting

### `Unknown argument:`

→ Backslash continuation bị hỏng khi paste. Dùng single-line variant.

### `Remote control not available`

→ Version cũ. Update:
```bash
claude --version    # Phải ≥ 2.1.51
```

### `Authentication required`

→ Dùng API key thay vì subscription auth. Fix:
```bash
unset ANTHROPIC_API_KEY
claude auth login
```

### Mobile không thấy session

→ Verify subscription tier (Pro/Max/Team/Enterprise). Free tier KHÔNG support remote control.
→ Team/Enterprise: admin phải bật toggle ở `claude.ai/admin-settings/claude-code`.

### WSL2 ngủ → mobile mất connection

→ WSL2 reconnect tự động khi máy thức. Để giảm sleep:
```bash
# Windows PowerShell (admin):
powercfg /change standby-timeout-ac 0
```
Hoặc giữ Windows terminal mở suốt session.

---

## Push Notification (v2.1.110+)

Trong session, hỏi Claude:

```
Khi CI PR #N xong, gửi notification cho mobile
```

```
Notify me when /quality-audit finishes — push to mobile
```

Claude sẽ dùng built-in notification → mobile push qua Claude app.

---

## Stop Notification — báo khi chat dừng / chờ input

Project default: **mỗi khi Claude dừng để chờ user reply (Stop event), trigger notification**. Cấu hình qua `Stop` hook trong `~/.claude/settings.json`.

### Setup (one-time)

Script đã có sẵn trong repo: `.claude/hooks/notify-stop.sh` (commit chung dự án — mọi máy clone về đều có). Chỉ cần wire qua `~/.claude/settings.json`:

```json
{
  "hooks": {
    "Stop": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/notify-stop.sh"
          }
        ]
      }
    ]
  }
}
```

Nếu `settings.json` đã có `hooks` block khác (PreToolUse, PostToolUse, …), thêm key `Stop` cùng level — KHÔNG ghi đè.

`${CLAUDE_PROJECT_DIR}` là biến Claude Code tự inject — script work cho mọi project chứa `.claude/hooks/notify-stop.sh`.

### Cài libnotify (WSL2 + WSLg)

```bash
sudo apt install -y libnotify-bin
notify-send "Test" "Hello"   # phải hiện toast WSLg
```

Nếu chạy ngoài WSLg (chỉ headless WSL2), script tự fallback sang Windows toast qua `powershell.exe`.

### Verify

```bash
# Trigger thủ công (giả lập Stop event)
.claude/hooks/notify-stop.sh
```

Kết quả mong đợi:
- WSLg desktop notification (góc phải Windows) + terminal bell
- Hoặc Windows toast (nếu chạy ngoài WSLg)
- Không có lỗi stderr

### Mobile push (qua remote-control)

Khi remote-control session đang active, Stop event tự gửi push tới Claude app trên mobile **mà không cần config thêm**. Hook trên chỉ bổ sung notification cho desktop khi đang ngồi máy.

### Disable tạm thời

Edit `~/.claude/settings.json` → đổi `Stop` thành `Stop_disabled` (hoặc xóa block). Restart Claude Code session để áp dụng.

---

## Workflow khuyến nghị cho project Kite

**1. Trước khi chuyển sang remote control:**

```bash
# Clear stale session locks (nếu có)
rm -f .claude/session-locks/session-*.lock

# Verify state
./.claude/skills/workflow/start-session/scripts/collect-state.sh
```

**2. Bắt đầu remote control:**

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode acceptEdits
```

**3. Mobile flow:**

- Mở Claude app (iOS/Android) hoặc `claude.ai/code` browser
- Scan QR HOẶC enter pairing code từ terminal
- Session "Kite Class Platform" hiện trong list
- Tap → bắt đầu chat

**4. Mobile commands phổ biến:**

```
/start-session
/repo-status
/continue
```

Mobile chạy được tất cả slash commands như desktop.

**5. Wrap session từ mobile:**

```
/clear
```

Hoặc đóng app — session vẫn chạy ngầm trên WSL2 cho lần sau resume.

---

## Khi nào KHÔNG dùng Remote Control

- Cần full shell access (inspect logs, run arbitrary commands ngoài Claude tools) → SSH/tmux
- Free tier subscription
- Sensitive task không muốn expose qua Anthropic (ví dụ: code mã nguồn private chưa public hợp đồng)

---

## Tham khảo

- Docs: [code.claude.com/docs/en/remote-control](https://code.claude.com/docs/en/remote-control)
- Mobile apps: Claude iOS / Android (App Store / Play Store)
- Settings: `~/.claude/settings.json` (`permissions.allow` để giảm prompts; `hooks.Stop` cho stop notification)
- Project hook script: `.claude/hooks/notify-stop.sh` (committed)

---

## Log

- **2026-04-28 (later):** Default upgraded từ `acceptEdits` → `bypassPermissions` (full auto kể cả Bash). Audit-gate hook + commit-history reversibility là safety net. Solo-dev workflow, không có team review per-tool, prompt Bash mỗi command làm chậm mobile flow + tốn battery. `acceptEdits` giữ là fallback variant cho repo lạ.
- **2026-04-28:** Project default đổi sang `--permission-mode acceptEdits` (auto edit không hỏi) + bỏ `--capacity 5` (unlimited concurrent sessions). Bổ sung §Stop Notification — wire `Stop` hook qua `~/.claude/settings.json` invoke `${CLAUDE_PROJECT_DIR}/.claude/hooks/notify-stop.sh` (committed) → desktop notification (WSLg/libnotify) + terminal bell + Windows toast (powershell.exe interop). Reason: 5-session cap không match wave-pack cluster có khi 6-8 agents song song; mỗi edit prompt làm chậm mobile flow.
