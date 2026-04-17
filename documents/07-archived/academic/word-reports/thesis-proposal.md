# Skill: Thesis Proposal Generator (Đề cương ĐATN)

Generate Word document (.docx) for Graduation Thesis Proposal (Đề cương Đồ án Tốt nghiệp) following UTC template.

## Folder Structure
All thesis proposal files are organized in: `documents/02-academic/word-reports/de-cuong-datn/`

## Trigger phrases
- "tạo đề cương DATN"
- "create thesis proposal"
- "generate đề cương đồ án"
- "xuất file đề cương"
- "update thesis proposal"

## Files

| File | Path |
|------|------|
| Script chính | `de-cuong-datn/create_de_cuong_datn_v4.py` |
| Output | `de-cuong-datn/DE_CUONG_DATN.docx` |
| Logo UTC | `de-cuong-datn/logo_utc.png` |
| Template guide | `templates/Mau-Decuong DATN-Cử nhân.pdf` |
| Student info | `.claude/skills/student-info.md` |

## When to Use
- Generate thesis proposal document
- Update proposal content
- Submit proposal for advisor review
- Prepare for thesis registration

## Document Structure (4 Sections)

### Header & Student Info
1. University header (2-column table: School/Department | National motto)
2. Date line (right-aligned, italic)
3. Title: "ĐỀ CƯƠNG ĐỒ ÁN TỐT NGHIỆP **CỬ NHÂN**" (yellow highlight on CỬ NHÂN)
4. Student information (name, ID, class, course, phone, email, major, system)
5. Advisor information (name, department, phone, email)
6. Thesis title

### Content Sections

#### 1. Nội dung, phạm vi của đề tài
- **Nội dung của đề tài** (bold subsection)
  - Brief intro paragraph (2-3 lines)
  - Bullet points (4 concise items)

- **Phạm vi của đề tài** (bold subsection)
  - Brief intro paragraph (1 line)
  - Bullet points (3 items: chức năng, công nghệ, giới hạn)

#### 2. Công nghệ, công cụ và ngôn ngữ lập trình
- Brief intro paragraph
- Bullet points (5 items: Backend, Frontend, AI Services, DevOps, Tools)

#### 3. Các kết quả chính dự kiến đạt được
- Brief intro: "Qua quá trình nghiên cứu và thực hiện đề tài, các kết quả dự kiến đạt được bao gồm:"
- **Unified bullet list** (10 items covering both personal and product results)
  - Personal outcomes (kiến thức, kỹ năng, kinh nghiệm)
  - Product outcomes (báo cáo, chương trình, kết quả kỹ thuật, mã nguồn)

#### 4. Kế hoạch thực hiện đề tài
- Intro paragraph: "Mỗi công việc kéo dài khoảng 1 tuần, các công việc được thực hiện đồng thời để tối ưu thời gian"
- **Table format** (4 columns):
  - STT (sequential number)
  - Nội dung công việc (task description)
  - Thời gian dự kiến (date range)
  - Ghi chú (Vietnamese notes only)

### Signatures & Logo
- Signature table (4 columns: Trưởng Khoa, Trưởng Bộ môn, GVHD, Sinh viên)
- UTC logo (bottom left, 3cm width)

## Format Specifications

### Margins
```python
MARGIN_TOP = Cm(2.0)
MARGIN_BOTTOM = Cm(2.0)
MARGIN_LEFT = Cm(2.5)
MARGIN_RIGHT = Cm(2.0)
```

### Fonts
```python
FONT_NAME = 'Times New Roman'
FONT_SIZE_NORMAL = Pt(13)
FONT_SIZE_TITLE = Pt(14)
FONT_SIZE_TABLE = Pt(12)
```

### Section Spacing
- Section title: `space_before = Pt(12)`, `space_after = Pt(6)`
- Subsection title: `space_before = Pt(6)`, `space_after = Pt(3)`
- Bullet items: `left_indent = Cm(1.0)`

## Best Practices

### ✅ DO: Use Goal-Oriented Language
**Đề cương chỉ là bản để xác định bài toán rõ ràng, không cam kết số liệu cụ thể**

Good examples:
- "thời gian phản hồi nhanh" (not "< 200ms")
- "hỗ trợ nhiều người dùng đồng thời" (not "1000+ concurrent users")
- "độ sẵn sàng cao" (not "99.9% uptime")
- "chi phí thấp và thời gian nhanh" (not "$0.19/instance, 5 phút")
- "tự động setup infrastructure" (not "trong 20 phút")
- "các trung tâm giáo dục nhỏ" (not "~10,000+ trung tâm")

### ✅ DO: Structure Results Section
- Use unified bullet list (no separate category headers)
- Cover both personal outcomes and product outcomes
- Keep concise (10 bullet points total)

### ✅ DO: Implementation Plan Structure
- **Each task ~1 week** (not longer than 1 week)
- **Tasks >1 week must have parallel tasks**
- **Develop KiteClass FIRST, then KiteHub**
- Backend + Frontend development in parallel
- Unit testing integrated with development
- Integration testing separate phase
- Vietnamese notes (avoid English technical terms when possible)
- Clear platform labels: "KiteClass:" or "KiteHub:"

### ❌ DON'T: Avoid Specific Metrics
Bad examples (too specific for proposal):
- ❌ "10+ assets từ 1 ảnh upload"
- ❌ "API < 200ms cho 95% requests"
- ❌ "1000+ concurrent users/instance"
- ❌ "uptime 99.9%"
- ❌ "214 use cases"
- ❌ "80% test coverage"

### ❌ DON'T: Avoid Tool-Specific Names (in notes)
Remove specific tool names from notes when not critical:
- ❌ "JMeter, profiling" → ✅ "Kiểm thử tải, tối ưu"
- ❌ "Triển khai K8s" → ✅ "Triển khai production"
- ❌ "Thanh toán VietQR" → ✅ "Thanh toán điện tử"
- ❌ "Next.js, React" → ✅ "Cơ sở giao diện"

Keep tool names in main task description where relevant:
- ✅ "KiteClass Frontend: Base setup + Auth pages + Unit tests"
- ✅ "AWS EKS production deployment"

## Implementation Plan Template

### Phase Breakdown (22 tasks, 4 months)

**Phase 1: Research & Analysis** (3 weeks, 2 tasks with overlap)
- Task 1-2: Research + Requirements analysis

**Phase 2: System Design** (2 weeks, 2 tasks)
- Task 3-4: Architecture + Database design

**Phase 3: KiteClass Development** (7 weeks, 9 tasks - backend + frontend parallel)
- Week 1: Gateway + Frontend base (parallel)
- Weeks 2-3: Student/Teacher modules (parallel backend + frontend)
- Weeks 4-5: Class/Attendance modules (parallel backend + frontend)
- Week 6: Assignment modules (parallel backend + frontend)
- Week 7: Integration testing

**Phase 4: KiteHub Platform** (4 weeks, 6 tasks)
- Week 1: Auth/Tenant + Billing (parallel)
- Week 2: Dashboard + Auto-provisioning (parallel)
- Week 3: AI Agent + Integration testing (parallel)

**Phase 5: System Testing & Deployment** (1.5 weeks, 2 tasks parallel)
- Load testing + Deployment (parallel)

**Phase 6: Documentation** (1 week, 1 task)
- Final documentation

## Actions

### 1. Generate proposal
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/02-academic/word-reports/de-cuong-datn
python3 create_de_cuong_datn_v4.py
```

### 2. Update thesis information
```python
THESIS_INFO = {
    "title": "XÂY DỰNG HỆ THỐNG SAAS CUNG CẤP DỊCH VỤ ĐÀO TẠO",
}

STUDENT_INFO = {
    "name": "Nguyễn Văn Kiệt",
    "student_id": "221230890",
    "class": "CNTT1-K63",
    "course": "63",
    "phone": "...",
    "email": "...",
    "major": "Công nghệ thông tin",
    "system": "Chính quy",
}

ADVISOR_INFO = {
    "name": "TS. Nguyễn Đức Dư",
    "department": "Khoa Công nghệ thông tin - Trường ĐH GTVT",
    "phone": "...",
    "email": "...",
}
```

### 3. Update content sections
Edit functions in `create_de_cuong_datn_v4.py`:
- `add_content_sections()` - All 4 main sections
- Section-specific helpers:
  - `add_section_title()`, `add_section_content()`
  - `add_bullet_item()`, `add_subsection_title()`

## Dependencies
```bash
pip install python-docx --user
```

## Checklist

Before submitting proposal:
- [ ] Title clear and concise (Vietnamese)
- [ ] Student information complete
- [ ] Advisor information correct
- [ ] Section 1: Content & scope concise (7 bullets total)
- [ ] Section 2: Technology stack concise (5 bullets)
- [ ] Section 3: Unified results list (10 bullets, no category headers)
- [ ] Section 4: Implementation plan 22 tasks (~1 week each)
- [ ] **NO specific metrics** (no numbers like "200ms", "99.9%", "$0.19")
- [ ] **Goal-oriented language** (hướng tới kết quả, chung chung)
- [ ] **Vietnamese notes** (no "JMeter", "K8s" in notes column)
- [ ] KiteClass developed before KiteHub
- [ ] Parallel tasks for multi-week work
- [ ] Document fits in ~2 pages
- [ ] Logo present
- [ ] All sections formatted correctly

## Common Updates

### Change title
```python
THESIS_INFO = {
    "title": "NEW TITLE HERE",
}
```

### Adjust timeline
Update `plan_data` array in section 4:
- Each task should be ~1 week
- Tasks >1 week need parallel tasks
- Format: `(STT, task_description, date_range, vietnamese_note)`

### Update results section
Edit bullet points in section 3:
- Keep unified list (no category headers)
- Use goal-oriented language (no specific numbers)
- Balance personal and product outcomes

## Tips

### Target Length
- Aim for **2 pages** total
- If longer, condense sections 1-2
- Section 4 table should fit on one page

### Language Style
- **Proposal = Direction, not commitment**
- Use "dự kiến", "hướng tới", "mục tiêu" instead of absolutes
- "nhanh, cao, tốt, hiệu quả" instead of "< 200ms, 99.9%, 1000+"

### Section Balance
- Section 1: Brief overview (not detailed)
- Section 2: Essential tech only
- Section 3: Mix of personal + product results
- Section 4: Detailed implementation plan (most important)

## Version History
- v4.0: Current version (22 tasks, KiteClass-first, goal-oriented language)
- v3.0: 14-step implementation plan
- v2.0: Expanded details
- v1.0: Initial version
