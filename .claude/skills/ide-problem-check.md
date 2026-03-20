# Skill: IDE Problem Check trong WSL

**Version:** 1.0
**Last Updated:** 2026-02-21
**Purpose:** Hướng dẫn cài đặt và chạy IDE problem check (Java compile + Checkstyle + TypeScript + ESLint) trong WSL

---

## Tổng Quan

IDE Problems panel của VSCode được populate bởi Language Servers chạy trên **Windows**, không accessible trực tiếp từ WSL shell. Thay vào đó, ta sử dụng các tools CLI để replicate kết quả tương đương:

| Layer | Tool | Tương đương IDE |
|-------|------|----------------|
| Java compile + Checkstyle | `mvn compile` | Java Problems tab |
| TypeScript type check | `tsc --noEmit` | TS errors |
| ESLint | `next lint` | ESLint warnings |
| Maven pom.xml version | Hook check | "Newer version available" warning |

---

## Cài Đặt Java 21 (Temurin) — Không Cần `sudo`

```bash
# Tạo thư mục
mkdir -p ~/.local/java

# Download JDK 21 LTS
curl -L "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz" \
  -o ~/.local/java/jdk21.tar.gz

# Giải nén
tar xzf ~/.local/java/jdk21.tar.gz -C ~/.local/java/

# Cấu hình trong ~/.bashrc
echo 'export JAVA_HOME="$HOME/.local/java/jdk-21.0.5+11"' >> ~/.bashrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc

# Verify
java -version
# Expected: openjdk version "21.0.5" 2024-10-15 LTS
```

---

## Vấn Đề Thường Gặp — Deprecated Constructor Ambiguity

### Nguyên nhân
Java ưu tiên **exact match** hơn **varargs**. Khi một class có cả 2 constructors:
```java
@Deprecated
public SomeException(String message) { ... }          // exact match

public SomeException(String code, Object... args) { } // varargs
```

Gọi `new SomeException("CODE")` → Java chọn **deprecated** constructor (exact match)!

### Pattern đúng — force varargs
```java
// ❌ SAI — Java chọn deprecated SomeException(String)
throw new ValidationException("CLASS_NOT_FOUND");
throw new DuplicateResourceException("EMAIL_EXISTS", email);  // String arg!

// ✅ ĐÚNG — force varargs với new Object[0] hoặc (Object) cast
throw new ValidationException("CLASS_NOT_FOUND", new Object[0]);
throw new DuplicateResourceException("EMAIL_EXISTS", (Object) email);
```

### Áp dụng cho tất cả exceptions trong project
| Exception | Deprecated ctor | Fix |
|-----------|----------------|-----|
| `ValidationException("CODE")` | `(String message)` | `("CODE", new Object[0])` |
| `EntityNotFoundException("CODE")` | `(String message)` | `("CODE", (Object) id)` |
| `DuplicateResourceException("CODE", str)` | `(String field, String value)` | `("CODE", (Object) str)` |

### Phát hiện bằng Maven
Thêm vào `pom.xml` (đã có trong core/gateway):
```xml
<configuration>
    <compilerArgs>
        <arg>-Xlint:deprecation</arg>
        <arg>-Xlint:unchecked</arg>
    </compilerArgs>
    <showWarnings>true</showWarnings>
    <showDeprecation>true</showDeprecation>
</configuration>
```

**Chạy không có `-q`** để thấy warnings:
```bash
mvn compile 2>&1 | grep "deprecated\|warning:"
```

---

## Chạy IDE Problem Check

### Backend (Java)

```bash
# Đường dẫn Maven (đã có sẵn từ mvnw)
MVN="$HOME/.m2/wrapper/dists/apache-maven-3.9.6/bin/mvn"

# Core Service — Compile + Checkstyle
JAVA_HOME=~/.local/java/jdk-21.0.5+11 bash $MVN \
  -f /mnt/e/2026-Kite-Class-Platform/kiteclass/kiteclass-core/pom.xml \
  compile -q

# Gateway Service — Compile + Checkstyle
JAVA_HOME=~/.local/java/jdk-21.0.5+11 bash $MVN \
  -f /mnt/e/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/pom.xml \
  compile -q

# Kết quả mong đợi:
# [INFO] You have 0 Checkstyle violations.
# [INFO] BUILD SUCCESS
```

### Frontend (TypeScript + ESLint)

```bash
# Cài dependencies (lần đầu)
cd /mnt/e/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend
pnpm install --frozen-lockfile

# TypeScript type check
node_modules/.bin/tsc --noEmit

# ESLint
node_modules/.bin/next lint

# Full build (production)
node_modules/.bin/next build
```

---

## Cài Đặt pnpm (Không Cần `sudo`)

```bash
npm install -g pnpm
# Hoặc
curl -fsSL https://get.pnpm.io/install.sh | sh -
```

---

## Git Hooks Tự Động Check

Hooks đã được install tại `.git/hooks/`:
- `pre-commit` → `.claude/scripts/pre-commit-check.sh`
- `commit-msg` → `.claude/scripts/commit-msg-check.sh`

**Pre-commit hook tự động:**
1. ✅ Check Checkstyle (Java wildcard imports, NeedBraces, etc.)
2. ✅ Compile Java khi có file `.java` thay đổi
3. ✅ Check `pom.xml` Spring Boot version === approved version
4. ✅ Check Git author name không chứa "claude"
5. ✅ Check hardcoded messages trong exceptions
6. ✅ Check `@since` annotations trên new Java files
7. ✅ Check wildcard imports
8. ✅ Check sensitive data

**Commit-msg hook:**
1. ✅ Message length 25-50 chars
2. ✅ Format: `type(scope): description`
3. ✅ No "Claude" trong `Co-Authored-By`

### Install lại hooks nếu clone repo mới

```bash
# Từ root project
ln -sf ../../.claude/scripts/pre-commit-check.sh .git/hooks/pre-commit
ln -sf ../../.claude/scripts/commit-msg-check.sh .git/hooks/commit-msg
chmod +x .git/hooks/pre-commit .git/hooks/commit-msg
```

---

## Checkstyle Rules (Core Service)

File: `kiteclass/kiteclass-core/checkstyle.xml`

| Rule | Mô tả |
|------|-------|
| `AvoidStarImport` | Không dùng `import *`, NGOẠI TRỪ: `jakarta.persistence`, `org.springframework.web.bind.annotation`, `jakarta.validation.constraints`, `org.mapstruct` |
| `NeedBraces` | Mọi `if/else/for/while` phải có `{}` |
| `LineLength` | Tối đa 140 chars |
| `ConstantName` | Constants phải là `UPPER_SNAKE_CASE` |
| `UnusedImports` | Không import thứ không dùng |

**Allowed star imports (do checkstyle.xml exclude):**
```java
import jakarta.persistence.*;           // ✅ OK
import org.springframework.web.bind.annotation.*;  // ✅ OK
import jakarta.validation.constraints.*;  // ✅ OK
import org.mapstruct.*;                 // ✅ OK

import lombok.*;                        // ❌ BLOCKED
import com.kiteclass.core.module.*.dto.*;  // ❌ BLOCKED
```

---

## Troubleshooting

### Maven không tìm thấy `dirname`

```bash
# Dùng bash shebang thay vì dùng mvnw
JAVA_HOME=~/.local/java/jdk-21.0.5+11 bash ~/.m2/wrapper/dists/apache-maven-3.9.6/bin/mvn ...
# Không dùng: ./mvnw (cần dirname trong PATH)
```

### Java không chạy từ Windows JDK path

```
/mnt/c/Program Files/Java/jdk-17/bin/ chứa Windows DLL, không executable trong WSL
```
→ Phải cài JDK Linux binary riêng (xem hướng dẫn trên).

### SDKMAN cần `unzip`

```bash
# SDKMAN cần unzip, nếu không có sudo thì dùng cách download trực tiếp như trên
```

---

## Version Approved

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21.0.5 (Temurin) | LTS, no sudo needed |
| Spring Boot | **3.5.12** | Luôn dùng version mới nhất trong skill |
| Maven | 3.9.6 | Đã có sẵn qua mvnw |
| Node.js | 18.x | Pre-installed trong WSL |
| pnpm | 10.x | Install via npm |

---

## Quy Tắc Quan Trọng

1. **LUÔN chạy `mvn compile`** trước khi commit khi có Java changes
2. **LUÔN chạy `tsc --noEmit + next lint`** trước khi commit khi có TS changes
3. **0 violations** = IDE Problems panel sạch
4. **Hook tự động** sẽ block commit nếu có violations
5. **Cập nhật version** trong skill này khi upgrade JDK hoặc Spring Boot

---

**Author:** KiteClass Team
**Related Skills:** `maven-dependencies.md`, `spring-boot-testing-quality.md`, `skills-compliance-checklist.md`
