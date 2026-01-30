# Skill: Thesis Proposal Generator (Đề cương ĐATN)

Generate Word document (.docx) for Graduation Thesis Proposal (Đề cương Đồ án Tốt nghiệp) following UTC template.

## Folder Structure
All thesis proposal files are organized in: `documents/word-reports/de-cuong-datn/`

## Trigger phrases
- "tạo đề cương DATN"
- "create thesis proposal"
- "generate đề cương đồ án"
- "xuất file đề cương"

## Files

| File | Path |
|------|------|
| Script chính | `de-cuong-datn/create_de_cuong_datn.py` |
| Output | `de-cuong-datn/DE_CUONG_DATN.docx` |
| Logo UTC | `templates/logo_utc.png` |
| Template guide | `templates/Mau-Decuong DATN-Cử nhân.pdf` |
| Template guide 2 | `templates/CUONG_THAMKHAO_Decuong DATN-DuThao.pdf` |
| Student info | `.claude/skills/student-info.md` |

## When to Use
- Generate thesis proposal document
- Update proposal content
- Submit proposal for advisor review
- Prepare for thesis registration

## Document Structure

### Cover Page
1. University header (logo, name, department)
2. Document title: "ĐỀ CƯƠNG ĐỒ ÁN TỐT NGHIỆP"
3. Thesis title (Vietnamese + English)
4. Student information table
5. Advisor information
6. Academic year

### Content Sections

#### PHẦN 1: THÔNG TIN CHUNG
1. **Tên đề tài** (Vietnamese and English)
2. **Sinh viên thực hiện** (Student info)
3. **Giảng viên hướng dẫn** (Advisor info)
4. **Đơn vị thực hiện** (Department)
5. **Thời gian thực hiện** (Timeline)

#### PHẦN 2: MỤC TIÊU VÀ NỘI DUNG
1. **Mục tiêu đề tài**
   - Specific objectives
   - Expected outcomes

2. **Nội dung nghiên cứu**
   - Research scope
   - Key features
   - Technology stack

3. **Phương pháp nghiên cứu**
   - Research methodology
   - Development approach
   - Testing strategy

#### PHẦN 3: DỰ KIẾN KẾT QUẢ
1. **Sản phẩm**
   - Software deliverables
   - Documentation deliverables

2. **Tính khả thi**
   - Technical feasibility
   - Resource feasibility
   - Timeline feasibility

#### PHẦN 4: KẾ HOẠCH THỰC HIỆN
1. **Lịch trình chi tiết**
   - Phase breakdown
   - Milestones
   - Deliverable dates

2. **Phân công công việc** (if team project)

#### PHẦN 5: TÀI LIỆU THAM KHẢO
- IEEE format references
- Technical documentation
- Academic papers

## Format Specifications

### Margins
- Top: 2.5cm, Bottom: 2.5cm
- Left: 3cm, Right: 2cm
- Page number: Center, header

### Font (Times New Roman)
- Title: 18pt Bold, center, uppercase
- Section heading: 14pt Bold, left
- Subsection: 13pt Bold, left
- Normal: 13pt, justify, spacing 1.5

### Numbering
- Section: Phần 1, Phần 2, etc.
- Subsection: 1., 2., 3., etc.
- Sub-subsection: a), b), c), etc.

## Actions

### 1. Generate proposal
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-reports/de-cuong-datn
python3 create_de_cuong_datn.py
```

### 2. Update proposal content
Edit `create_de_cuong_datn.py` functions:
- `add_part1()` - General information
- `add_part2()` - Objectives and content
- `add_part3()` - Expected results
- `add_part4()` - Implementation plan
- `add_part5()` - References

### 3. Customize thesis information
```python
# Update thesis details
THESIS_INFO = {
    "title_vi": "Xây dựng Hệ thống Quản lý Lớp học Trực tuyến theo Kiến trúc Microservices",
    "title_en": "Building Online Class Management System using Microservices Architecture",
    "advisor": "TS. Nguyễn Đức Dư",
    "start_date": "01/01/2026",
    "end_date": "30/06/2026"
}
```

## Dependencies
```bash
pip install python-docx --user
```

## Integration with Other Reports

### Related Documents
- Survey report: `../bao-cao-khao-sat/` (market research)
- Internship report: `../bao-cao-thuc-tap/` (technical implementation)
- Architecture diagrams: `../../diagrams/` (system design)
- Technical reports: `../../reports/` (detailed analysis)

### Workflow
1. **Market Research** → Create survey report
2. **Technical Planning** → Create thesis proposal (this document)
3. **Implementation** → Document in internship report
4. **Final Defense** → Compile all documents

## Checklist

Before submitting proposal:
- [ ] Student information complete and accurate
- [ ] Thesis title clear and specific (VN + EN)
- [ ] Objectives SMART (Specific, Measurable, Achievable, Relevant, Time-bound)
- [ ] Technology stack justified
- [ ] Timeline realistic (6 months breakdown)
- [ ] References in IEEE format
- [ ] All fields updated (no placeholder text)
- [ ] Advisor reviewed and approved
- [ ] Document formatted correctly
- [ ] Page numbers present
- [ ] Logo and university info correct

## Tips

### Writing Good Objectives
✅ **Good**: "Xây dựng hệ thống multi-tenant với 6 microservices, hỗ trợ 100+ concurrent users, deployment trên AWS EKS"

❌ **Bad**: "Xây dựng hệ thống quản lý lớp học tốt"

### Timeline Best Practices
- **Month 1-2**: Research, design, database schema
- **Month 3-4**: Core features implementation, testing
- **Month 5**: Advanced features, integration testing
- **Month 6**: Deployment, documentation, thesis writing

### Common Mistakes to Avoid
- Too broad scope (reduce features if needed)
- Unrealistic timeline (6 months for 10 microservices?)
- Missing technical details (which database? which cloud?)
- Vague objectives (what exactly will you build?)
- No testing plan (how will you verify it works?)

## Templates Available

Located in `templates/`:
- `Mau-Decuong DATN-Cử nhân.pdf` - Official template
- `CUONG_THAMKHAO_Decuong DATN-DuThao.pdf` - Reference example
- `Quy dinh trinh bay do an tot nghiep.pdf` - Formatting guidelines

## Support

For questions about:
- **Format**: Check `templates/Quy dinh trinh bay do an tot nghiep.pdf`
- **Content**: Consult with thesis advisor
- **Technical details**: Review `../../reports/system-architecture-v3-final.md`
- **Student info**: See `.claude/skills/student-info.md`
