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
claude remote-control --name "Kite Class Platform"
```

→ QR code + URL hiện ra. Mobile scan/mở → done.

---

## Recommended Setup (parallel-agent workflow)

**Cách 1 — Single line (an toàn nhất, copy nguyên dòng):**

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --capacity 5 --permission-mode default
```

**Cách 2 — Multi-line (chỉ paste từ raw markdown, KHÔNG paste từ formatted):**

```bash
claude remote-control \
  --name "Kite Class Platform" \
  --spawn worktree \
  --capacity 5 \
  --permission-mode default
```

⚠️ **Cảnh báo về backslash continuation:** Khi copy từ HTML/formatted output sang terminal, ký tự sau `\` đôi khi bị space ẩn → terminal hiểu sai. Nếu lỗi `Unknown argument`, dùng Cách 1 (1 dòng).

---

## Flags giải thích

| Flag | Tác dụng |
|------|---------|
| `--name "..."` | Tên hiển thị trên `claude.ai/code` (phân biệt project) |
| `--spawn worktree` | Mỗi session mobile = worktree + branch riêng (khuyến nghị cho parallel work) |
| `--spawn same-dir` | Mọi session share repo dir (default — đơn giản, 1 task tại 1 lúc) |
| `--spawn session` | Session ephemeral (Q&A nhanh, không commit) |
| `--capacity 5` | Max concurrent sessions (5 = khớp với rule parallel-agent của project) |
| `--permission-mode default` | Prompt từng tool — phải approve trên mobile (an toàn nhất) |
| `--permission-mode acceptEdits` | Auto Read/Edit/Write, vẫn prompt Bash (cân bằng) |
| `--permission-mode auto` | Auto allow tất cả tools đã từng approve |
| `--permission-mode bypassPermissions` | Full auto (DANGER — chỉ khi đi xa và biết task an toàn) |
| `--permission-mode plan` | Plan mode — không execute |

---

## Variants — Copy theo nhu cầu

### Default (an toàn — prompt mọi tool):

```bash
claude remote-control --name "Kite Class Platform"
```

### Với worktree spawning (parallel agents):

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --capacity 5
```

### Hands-off khi đi xa (auto edits, vẫn confirm Bash):

```bash
claude remote-control --name "Kite Class Platform" --spawn worktree --permission-mode acceptEdits
```

### Full auto (CẨN TRỌNG — chỉ task không destructive):

```bash
claude remote-control --name "Kite Class Platform" --permission-mode bypassPermissions
```

### Verbose debug (khi troubleshoot):

```bash
claude remote-control --name "Kite Class Platform" --verbose --debug-file /tmp/claude-rc.log
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
claude remote-control --name "Kite Class Platform" --spawn worktree --capacity 5
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
- Settings: `~/.claude/settings.json` (`permissions.allow` để giảm prompts)
