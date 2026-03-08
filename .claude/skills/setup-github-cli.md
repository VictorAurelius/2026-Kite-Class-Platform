# Skill: Setup GitHub CLI

Hướng dẫn cài đặt và cấu hình GitHub CLI (`gh`) để tương tác với GitHub Actions, repositories, và CI/CD.

## Prerequisites

- GitHub account
- Terminal access (Linux, macOS, or WSL2)
- Internet connection

## Installation

### Ubuntu/Debian/WSL2

```bash
# 1. Add GitHub CLI repository
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg

# 2. Add apt source
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null

# 3. Update and install
sudo apt update
sudo apt install gh -y
```

### macOS

```bash
brew install gh
```

### Windows

```powershell
# Using winget
winget install --id GitHub.cli

# Or using Chocolatey
choco install gh
```

## Verification

```bash
gh --version
# Expected: gh version X.X.X (YYYY-MM-DD)
```

## Authentication

### Method 1: Browser-based Login (Recommended)

```bash
gh auth login
```

Interactive prompts:
1. **What account do you want to log into?** → `GitHub.com`
2. **What is your preferred protocol for Git operations?** → `HTTPS` (hoặc `SSH` nếu đã có SSH key)
3. **Authenticate Git with your GitHub credentials?** → `Yes`
4. **How would you like to authenticate GitHub CLI?** → `Login with a web browser`
5. Copy the one-time code
6. Press Enter to open browser
7. Paste code vào browser và authorize

### Method 2: Token-based Login

```bash
gh auth login
```

Interactive prompts:
1. Chọn `GitHub.com`
2. Chọn `HTTPS`
3. Chọn `Yes` for Git credentials
4. Chọn `Paste an authentication token`
5. Paste Personal Access Token (tạo tại https://github.com/settings/tokens)
   - Scopes cần: `repo`, `workflow`, `read:org`

### Verify Authentication

```bash
gh auth status
```

Expected output:
```
github.com
  ✓ Logged in to github.com as <username> (~/.config/gh/hosts.yml)
  ✓ Git operations for github.com configured to use https protocol.
  ✓ Token: *******************
```

## Common Commands

### CI/CD & Actions

```bash
# List recent workflow runs
gh run list --limit 10

# View specific run details
gh run view <run_id>

# View run logs
gh run view <run_id> --log

# Download run logs and artifacts
gh run download <run_id>

# Watch a running workflow
gh run watch

# Re-run a failed workflow
gh run rerun <run_id>
```

### Repository Operations

```bash
# View current repo info
gh repo view

# Clone a repo
gh repo clone <owner>/<repo>

# Create a new repo
gh repo create <name> --public/--private

# Fork a repo
gh repo fork <owner>/<repo>
```

### Pull Requests

```bash
# List PRs
gh pr list

# Create PR
gh pr create --title "Title" --body "Description"

# View PR
gh pr view <pr_number>

# Checkout PR locally
gh pr checkout <pr_number>

# Merge PR
gh pr merge <pr_number>
```

### Issues

```bash
# List issues
gh issue list

# Create issue
gh issue create --title "Title" --body "Description"

# View issue
gh issue view <issue_number>

# Close issue
gh issue close <issue_number>
```

## Configuration

### Default Repository

```bash
# Set default repo for current directory
gh repo set-default <owner>/<repo>
```

### Default Editor

```bash
gh config set editor "vim"
# Or: code, nano, emacs, etc.
```

### Git Protocol

```bash
# Use HTTPS (default)
gh config set git_protocol https

# Use SSH
gh config set git_protocol ssh
```

## Troubleshooting

### Issue: "gh: command not found"

**Solution**: Thêm gh vào PATH hoặc restart terminal sau khi install

### Issue: Authentication failed

**Solution**:
```bash
# Logout and login again
gh auth logout
gh auth login
```

### Issue: "HTTP 403: Resource not accessible by integration"

**Solution**: Token thiếu permissions. Tạo lại token với đủ scopes:
- `repo` - Full control of private repositories
- `workflow` - Update GitHub Action workflows
- `read:org` - Read org and team membership

### Issue: Rate limit exceeded

**Solution**:
```bash
# Check rate limit status
gh api rate_limit

# Authenticated requests have higher limits (5000/hour vs 60/hour)
gh auth status
```

## Advanced Usage

### Custom API Calls

```bash
# GET request
gh api /repos/{owner}/{repo}/actions/runs

# POST request with data
gh api --method POST /repos/{owner}/{repo}/issues \
  -f title="Bug report" \
  -f body="Description"
```

### Scripting with gh

```bash
#!/bin/bash
# Example: Auto-download latest CI logs

REPO="owner/repo"
RUN_ID=$(gh run list --repo $REPO --limit 1 --json databaseId --jq '.[0].databaseId')

gh run download $RUN_ID --repo $REPO
echo "Downloaded logs from run #$RUN_ID"
```

### JSON Output for Parsing

```bash
# Get JSON output
gh run list --json databaseId,status,conclusion,createdAt,headBranch

# Parse with jq
gh run list --json status,conclusion | jq '.[] | select(.conclusion=="failure")'
```

## Integration with Git

After `gh auth login` with Git credentials enabled:

```bash
# Git operations will use gh credentials automatically
git clone https://github.com/owner/repo.git
git push origin main
# No need to enter username/password
```

## Security Best Practices

1. **Use Token with Minimal Scopes**: Chỉ cấp quyền cần thiết
2. **Set Token Expiration**: Nên dùng token có thời hạn (7-90 days)
3. **Revoke Unused Tokens**: Xóa tokens không dùng tại https://github.com/settings/tokens
4. **Use Environment-Specific Tokens**: Dùng token khác nhau cho dev/prod
5. **Never Commit Tokens**: Không commit token vào git repository

## Uninstallation

### Ubuntu/Debian/WSL2

```bash
sudo apt remove gh -y
sudo rm /etc/apt/sources.list.d/github-cli.list
sudo rm /usr/share/keyrings/githubcli-archive-keyring.gpg
```

### macOS

```bash
brew uninstall gh
```

### Windows

```powershell
winget uninstall GitHub.cli
```

## References

- Official Docs: https://cli.github.com/manual/
- GitHub: https://github.com/cli/cli
- Auth Guide: https://cli.github.com/manual/gh_auth_login

---

**Created**: 2026-02-09
**Last Updated**: 2026-02-09
**Author**: KiteClass Team
