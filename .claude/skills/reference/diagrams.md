# Diagrams — Technical Documentation

## Tại sao cần diagrams?

- Architecture overview cho developer mới (onboarding)
- Business flows cho stakeholder review
- ERD cho database design
- Sequence diagram cho API integration

**Không có diagrams → mọi kiến trúc chỉ tồn tại trong đầu developer.**

## Cấu trúc folder

```
documents/06-diagrams/
├── README.md               # Index: danh sách diagrams + mục đích
├── source/                  # Source files (.puml, .mmd, .drawio)
├── rendered/                # Output files (.png, .svg) — committed
└── tools/                   # Render tools (plantuml.jar, scripts)
```

**Rule:** Commit CẢ source + rendered. Người đọc không cần install tool để xem diagram.

## Tool Options

| Tool | Format | Ưu điểm | Nhược điểm |
|------|--------|---------|------------|
| **PlantUML** | `.puml` | Text-based, git-friendly, nhiều diagram types | Cần Java runtime |
| **Mermaid** | `.mmd` | Native trong GitHub/GitLab markdown | Ít diagram types hơn PlantUML |
| **Draw.io** | `.drawio` | Visual editor, export PNG/SVG | Binary file, khó diff |

**Recommend:** PlantUML cho projects có Java. Mermaid cho projects nhẹ.

## PlantUML Setup

### Prerequisites

**MUST install CẢ HAI:**

1. **Java 8+** (runtime cho PlantUML)
2. **Graphviz** (render engine cho hầu hết diagram types)

```bash
# Ubuntu/WSL2:
sudo apt-get install -y default-jre graphviz

# Verify:
java -version
dot -V
```

**Không có Graphviz → PNG render ra lỗi đỏ "Cannot find Graphviz"** thay vì diagram thật.

### Install PlantUML

```bash
# Download jar vào tools/ (gitignored)
mkdir -p documents/06-diagrams/tools
curl -L -o documents/06-diagrams/tools/plantuml.jar \
  "https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar"

# Verify:
java -jar documents/06-diagrams/tools/plantuml.jar -testdot
```

**VS Code extension** (preview only): `jebbs.plantuml`

### Render

```bash
# Dùng script (recommend)
scripts/render-diagrams.sh

# Hoặc manual
java -jar documents/06-diagrams/tools/plantuml.jar \
  -tpng -o "$(pwd)/documents/06-diagrams/rendered" \
  documents/06-diagrams/source/*.puml
```

### Syntax Gotchas

| Vấn đề | Ví dụ | Fix |
|--------|-------|-----|
| `()` trong activity label | `:Call method();` | Bỏ `()`: `:Call method;` |
| `{}` trong label | `:Return {};` | Bỏ `{}`: `:Return object;` |
| `/` trong label | `:true/false;` | Dùng text: `:true or false;` |
| File quá dài | >200 lines | Chia thành sub-diagrams |

## Mermaid Setup (alternative)

Không cần install — render trực tiếp trong GitHub markdown:

````markdown
```mermaid
graph TD
    A[Client] --> B[Gateway]
    B --> C[Core Service]
    C --> D[(PostgreSQL)]
```
````

Nếu cần render PNG:

```bash
# Install mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# Render
mmdc -i documents/06-diagrams/source/architecture.mmd \
     -o documents/06-diagrams/rendered/architecture.png
```

## Diagrams nên có (minimum)

| Diagram | Mục đích | Khi nào tạo |
|---------|----------|------------|
| Architecture overview | Hệ thống tổng quan | Bắt đầu dự án |
| ERD (database schema) | Quan hệ giữa entities | Sau khi thiết kế DB |
| Business flow | Luồng nghiệp vụ chính | Mỗi domain có flow phức tạp |
| Deployment | Infra topology | Khi setup production |
| Sequence (API) | Tương tác giữa services | Cho cross-service flows |

## Render Tool

**MUST** dùng `scripts/render-diagrams.sh` để render — KHÔNG dùng `java -jar` trực tiếp hay tool khác.

**Lý do:**
- Script xử lý paths, output directory, error handling tự động
- Hỗ trợ cả PlantUML + Mermaid
- `--check` mode để verify tools available
- Consistent output location (`documents/06-diagrams/rendered/`)

```bash
scripts/render-diagrams.sh              # Render tất cả
scripts/render-diagrams.sh --check      # Kiểm tra tools
scripts/render-diagrams.sh file.puml    # Render 1 file
```

## Verification (BẮT BUỘC)

Sau khi render, **PHẢI mở từng PNG kiểm tra**:

1. **Mở file PNG** trong image viewer hoặc browser
2. **Kiểm tra lỗi:**
   - ❌ Hình có khung đỏ "Syntax Error" → Fix source `.puml`
   - ❌ Hình trắng trống → File source rỗng hoặc sai encoding
   - ❌ Text bị cắt / overlap → Điều chỉnh layout
   - ❌ Arrow thiếu / sai hướng → Check connections
3. **Nếu lỗi:** Fix source → Re-render → Verify lại
4. **Nếu OK:** Proceed to commit

**Anti-pattern:** Render xong commit ngay mà KHÔNG xem PNG → deploy diagram lỗi, mất credibility.

## .gitignore

**MUST** thêm vào `.gitignore`:
```
*.jar
documents/06-diagrams/tools/
```

Jar files quá lớn (28MB+), KHÔNG commit vào git. Mỗi developer tự download khi cần.

## Workflow

1. Tạo source file trong `documents/06-diagrams/source/`
2. Render: `scripts/render-diagrams.sh`
3. **Verify: Mở từng PNG kiểm tra lỗi** ← BẮT BUỘC
4. Commit CẢ source + rendered: `git add documents/06-diagrams/`
5. Update `documents/06-diagrams/README.md` index
