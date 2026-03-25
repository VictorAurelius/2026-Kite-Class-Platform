# IDE Setup — VS Code Configuration

## 1. Claude Code Permissions

File: `.claude/settings.local.json`

```json
{
  "permissions": {
    "allow": [
      "Bash",
      "Read",
      "Write",
      "Edit",
      "WebFetch",
      "WebSearch",
      "Skill(update-config)"
    ]
  },
  "defaultMode": "bypassPermissions"
}
```

**Mục đích:** Claude Code không hỏi quyền mỗi lần dùng tool. File này KHÔNG commit (local only).

**Cách tạo:**
```bash
mkdir -p .claude
cp .claude/starter-kit/templates/settings.local.json.template .claude/settings.local.json
```

## 2. VS Code Settings (shared, committed)

File: `.vscode/settings.json`

Copy template và uncomment sections phù hợp với tech stack:
```bash
mkdir -p .vscode
cp .claude/starter-kit/templates/vscode-settings.json.template .vscode/settings.json
```

## 3. Tắt Markdown Warnings

**Vấn đề:** markdownlint + VS Code markdown validation tạo hàng trăm warnings cho business docs, plans, etc. Đây là noise, không phải lỗi.

**Fix (bắt buộc cho mọi dự án có docs):**

```json
// .vscode/settings.json
{
  "markdownlint.config": { "default": false },
  "[markdown]": {
    "editor.formatOnSave": false,
    "editor.codeActionsOnSave": {},
    "cSpell.enabled": false
  },
  "markdown.validate.enabled": false,
  "markdownlint.run": "onSave"
}
```

**Extensions nên cài:** `davidanson.vscode-markdownlint` (rồi tắt rules như trên)

## 4. Test Runner per Language

### Java (JUnit + Maven)

**Extension:** `vscjava.vscode-java-test`

```json
// .vscode/settings.json
{
  "java.test.defaultConfig": "",
  "java.test.config": [
    {
      "name": "Unit Tests",
      "workingDirectory": "${workspaceFolder}",
      "vmArgs": ["-Dspring.profiles.active=test"]
    }
  ]
}
```

**Run tests:**
- Click ▶️ trên test method/class trong editor
- Test Explorer sidebar (flask icon)
- Terminal: `scripts/test-local.sh {project} core`

**Troubleshoot:**
- "Test not discovered" → Rebuild: `Ctrl+Shift+P` → "Java: Clean Workspace"
- "ClassNotFoundException" → Check `application-test.yml` exists in `src/test/resources/`

### TypeScript (Vitest)

**Extension:** `vitest.explorer`

```json
// .vscode/settings.json
{
  "vitest.commandLine": "npx vitest"
}
```

**Run tests:**
- Click ▶️ trên `describe`/`it` blocks
- Test Explorer sidebar
- Terminal: `scripts/test-local.sh {project} frontend`

**Troubleshoot:**
- "Module not found" → `npm install` trong frontend directory
- "Cannot find module" → Check `vitest.config.ts` aliases match `tsconfig.json` paths

### Python (pytest)

**Extension:** `ms-python.python`

```json
// .vscode/settings.json
{
  "python.testing.pytestEnabled": true,
  "python.testing.pytestArgs": ["tests"],
  "python.testing.autoTestDiscoverOnSaveEnabled": true
}
```

**Run tests:**
- Click ▶️ trên test functions
- Test Explorer sidebar
- Terminal: `scripts/test-local.sh {project} backend`

### Go

**Extension:** `golang.go`

```json
// .vscode/settings.json
{
  "go.testOnSave": true,
  "go.coverOnSave": true,
  "go.lintTool": "golangci-lint"
}
```

## 5. Extensions Recommend

File: `.vscode/extensions.json`

```json
{
  "recommendations": [
    // Core
    "esbenp.prettier-vscode",
    "streetsidesoftware.code-spell-checker",
    "davidanson.vscode-markdownlint",

    // Java
    "vscjava.vscode-java-pack",
    "vscjava.vscode-java-test",
    "vmware.vscode-spring-boot",

    // TypeScript/React
    "dbaeumer.vscode-eslint",
    "vitest.explorer",

    // Python
    "ms-python.python",
    "ms-python.black-formatter",

    // DevOps
    "ms-azuretools.vscode-docker",
    "hashicorp.terraform"
  ]
}
```

## 6. Common IDE Warnings — Khi nào fix vs ignore

| Warning | Severity | Action |
|---------|----------|--------|
| markdownlint rules | Noise | **Tắt** (settings trên) |
| cSpell unknown word | Noise | **Thêm vào** `cSpell.words` |
| Java resource leak (Testcontainers) | False positive | **Ignore** — `@Container` manages lifecycle |
| TypeScript strict null | Real issue | **Fix** — thêm null check |
| ESLint error | Real issue | **Fix** — hoặc `// eslint-disable-next-line` với comment lý do |
| Checkstyle ConstantName | Real issue | **Fix** — dùng UPPER_CASE cho static final |
| "Cannot find module" | Missing dependency | **Fix** — `npm install` |
| Java "not analysed due to compiler option" | Eclipse compiler noise | **Ignore** — CI dùng javac, không ảnh hưởng |
