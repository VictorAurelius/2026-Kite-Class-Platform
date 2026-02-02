# Word Reports - Academic Documents

This directory contains all Word documents, scripts, and templates for academic submissions (Internship Report, Thesis Proposal, Survey Report).

## 📁 Folder Structure

```
word-reports/
├── bao-cao-thuc-tap/           # Internship Report (Báo cáo Thực tập Tốt nghiệp)
│   ├── BAO_CAO_THUC_TAP.docx
│   ├── BAO_CAO_THUC_TAP_SORA.docx
│   ├── CONTEXT_BAO_CAO_THUC_TAP.md
│   └── create_bao_cao_thuc_tap.py
│
├── de-cuong-datn/              # Thesis Proposal (Đề cương Đồ án Tốt nghiệp)
│   ├── DE_CUONG_DATN.docx
│   └── create_de_cuong_datn.py
│
├── bao-cao-khao-sat/           # Survey Report (Báo cáo Khảo sát)
│   ├── BAO_CAO_KHAO_SAT_KITECLASS.docx
│   ├── CONTEXT_BAO_CAO_KHAO_SAT.md
│   └── create_bao_cao_khao_sat.py
│
├── templates/                   # Templates & References
│   ├── logo_utc.png
│   ├── maubaocaothuctap.png
│   ├── CUONG_THAMKHAO_BaoCaoTTTN-DuThao.pdf
│   ├── CUONG_THAMKHAO_Decuong DATN-DuThao.pdf
│   ├── DUC_THAMKHAO_Báo cáo thực tập tốt nghiệp.pdf
│   ├── Huong dan trinh bay bao cao TTTN.pdf
│   ├── Mau-Decuong DATN-Cử nhân.pdf
│   ├── Quy dinh trinh bay do an tot nghiep.docx
│   └── Quy dinh trinh bay do an tot nghiệp.pdf
│
└── .claude/skills/             # Claude Skills for automation
    ├── word-report.md          # Internship report generator
    ├── survey-report.md        # Survey report generator
    ├── thesis-proposal.md      # Thesis proposal generator
    └── student-info.md         # Student information
```

## 📝 Documents Overview

### 1. Báo cáo Thực tập (Internship Report)

**Purpose**: Document internship experience and technical work

**Files**:
- `BAO_CAO_THUC_TAP.docx` - Main report
- `BAO_CAO_THUC_TAP_SORA.docx` - Alternative version
- `create_bao_cao_thuc_tap.py` - Python generator script
- `CONTEXT_BAO_CAO_THUC_TAP.md` - Content context

**Generate**:
```bash
cd bao-cao-thuc-tap
python3 create_bao_cao_thuc_tap.py
```

**Claude Skill**: `.claude/skills/word-report.md`

**Reference**: `templates/Huong dan trinh bay bao cao TTTN.pdf`

---

### 2. Đề cương ĐATN (Thesis Proposal)

**Purpose**: Propose graduation thesis topic and plan

**Files**:
- `DE_CUONG_DATN.docx` - Thesis proposal document
- `create_de_cuong_datn.py` - Python generator script

**Generate**:
```bash
cd de-cuong-datn
python3 create_de_cuong_datn.py
```

**Claude Skill**: `.claude/skills/thesis-proposal.md`

**References**:
- `templates/Mau-Decuong DATN-Cử nhân.pdf`
- `templates/CUONG_THAMKHAO_Decuong DATN-DuThao.pdf`

---

### 3. Báo cáo Khảo sát (Survey Report)

**Purpose**: Document market research and user survey results

**Files**:
- `BAO_CAO_KHAO_SAT_KITECLASS.docx` - Survey report
- `create_bao_cao_khao_sat.py` - Python generator script
- `CONTEXT_BAO_CAO_KHAO_SAT.md` - Survey context and data

**Generate**:
```bash
cd bao-cao-khao-sat
python3 create_bao_cao_khao_sat.py
```

**Claude Skill**: `.claude/skills/survey-report.md`

**Reference**: Survey plan in `../../plans/survey-interview-plan.md`

---

### 4. Templates

**Shared Resources**:
- **Logos**: `logo_utc.png`, `maubaocaothuctap.png`
- **Official templates** (PDF): University-provided templates
- **Formatting guidelines** (PDF): Rules and examples

## 🔧 Setup

### Prerequisites
```bash
# Install Python dependencies
pip install python-docx --user
```

### Student Information
Update student details in: `.claude/skills/student-info.md`

## 📋 Academic Workflow

```
1. Market Research
   └─> Create Survey Report (bao-cao-khao-sat/)

2. Thesis Planning
   └─> Create Thesis Proposal (de-cuong-datn/)

3. Implementation & Documentation
   └─> Create Internship Report (bao-cao-thuc-tap/)

4. Final Submission
   └─> Compile all documents for defense
```

## 🎯 Quick Actions

### Generate All Reports
```bash
cd /mnt/e/person/2026-Kite-Class-Platform/documents/word-reports

# Survey report
cd bao-cao-khao-sat && python3 create_bao_cao_khao_sat.py && cd ..

# Thesis proposal
cd de-cuong-datn && python3 create_de_cuong_datn.py && cd ..

# Internship report
cd bao-cao-thuc-tap && python3 create_bao_cao_thuc_tap.py && cd ..
```

### Update Fields in Word
After generating .docx files:
1. Open document in Microsoft Word
2. Press `Ctrl+A` to select all
3. Press `F9` to update all fields
4. Insert Table of Contents: References → Table of Contents

## 📐 Formatting Standards (ĐH GTVT)

### Common Requirements
- **Font**: Times New Roman
- **Margins**: Top 2-2.5cm, Bottom 2-2.5cm, Left 3cm, Right 2cm
- **Line spacing**: 1.2-1.5
- **Paragraph indent**: 1-1.27cm
- **Page numbers**: Center, header

### Heading Levels
| Level | Size | Style |
|-------|------|-------|
| Chapter (Chương) | 14-18pt | Bold, Center/Left |
| Section (1.1) | 13-16pt | Bold, Left |
| Subsection (1.1.1) | 13-14pt | Bold/Italic, Left |
| Normal text | 13pt | Regular, Justify |

## 🔗 Related Directories

- **Technical Reports**: `../reports/` - Architecture, analysis, technical docs
- **Diagrams**: `../diagrams/` - PlantUML architecture diagrams
- **Plans**: `../plans/` - Project plans and schedules
- **Implementation Docs**: `../implementation/` - PR summaries, guides

## 📚 References

### Official Guidelines
All PDF templates in `templates/` directory follow ĐH GTVT (University of Transport) standards.

### Technical Content Sources
- System architecture: `../reports/system-architecture-v3-final.md`
- Technology stack: `../reports/technology-stack-report.md`
- Database design: `../plans/database-design.md`
- Project schedule: `../plans/project-schedule.md`

## 💡 Tips

### For Internship Report
- Focus on technical implementation details
- Include diagrams from `../diagrams/`
- Document challenges and solutions
- Add code samples in appendix

### For Thesis Proposal
- Be specific about objectives (SMART goals)
- Realistic 6-month timeline
- Justify technology choices
- Clear scope boundaries

### For Survey Report
- Include raw survey data in appendix
- Use charts/graphs for statistics
- Compare competitor features
- Link findings to feature priorities

## 🆘 Troubleshooting

### Logo not found
```bash
cd templates
curl -L -o logo_utc.png "https://cdn.haitrieu.com/wp-content/uploads/2022/03/Logo-Dai-Hoc-Giao-Thong-Van-Tai-UTC.png"
```

### Python module error
```bash
pip install python-docx --user
```

### Fields not updating in Word
- Select All (Ctrl+A)
- Update Fields (F9)
- If still not working, check field codes (Alt+F9)

## 📞 Support

- **Formatting questions**: Check PDFs in `templates/`
- **Content questions**: Consult thesis advisor
- **Technical help**: Review Claude skills in `.claude/skills/`
- **Student info**: `.claude/skills/student-info.md`

---

**Last Updated**: 2026-01-30
**Maintained by**: Nguyễn Văn Kiệt (221230890)
