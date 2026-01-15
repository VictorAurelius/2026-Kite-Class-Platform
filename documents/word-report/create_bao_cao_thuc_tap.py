#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script tạo báo cáo thực tập dạng Word (.docx)
Format theo quy định trình bày đồ án tốt nghiệp - ĐH GTVT

Quy chuẩn áp dụng:
- Căn lề: trên 2.5cm, dưới 2.5cm, trái 3cm, phải 2cm
- Số trang: giữa, phía trên đầu trang
- Chương: Times New Roman 18pt, Bold, căn giữa, Before: 0pt, After: 12pt
- Mục (1.1): Times New Roman 16pt, Bold, căn trái, Before: 6pt, After: 6pt
- Tiểu mục (1.1.1): Times New Roman 14pt, Bold, căn trái, Before: 6pt, After: 6pt
- Đoạn văn: Times New Roman 13pt, Justify, thụt đầu dòng 1cm, giãn dòng 1.2
- Tên bảng: phía trên bảng
- Tên hình: phía dưới hình
"""

from docx import Document
from docx.shared import Pt, Cm, Inches, RGBColor, Twips
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.section import WD_ORIENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

# ============== CONSTANTS theo quy định ==============
FONT_NAME = 'Times New Roman'
FONT_SIZE_NORMAL = Pt(13)      # Nội dung đoạn văn
FONT_SIZE_CHAPTER = Pt(18)     # Tiêu đề chương
FONT_SIZE_SECTION = Pt(16)     # Mục 1.1, 1.2
FONT_SIZE_SUBSECTION = Pt(14)  # Tiểu mục 1.1.1, 1.1.2
FONT_SIZE_TABLE = Pt(12)       # Nội dung bảng
FONT_SIZE_CAPTION = Pt(13)     # Caption bảng/hình

LINE_SPACING = 1.2             # Giãn dòng 1.2
FIRST_LINE_INDENT = Cm(1.0)    # Thụt đầu dòng 1cm

# Căn lề theo quy chuẩn
MARGIN_LEFT = Cm(3.0)
MARGIN_RIGHT = Cm(2.0)
MARGIN_TOP = Cm(2.5)
MARGIN_BOTTOM = Cm(2.5)


def set_document_margins(doc):
    """Thiết lập căn lề cho toàn bộ document"""
    for section in doc.sections:
        section.top_margin = MARGIN_TOP
        section.bottom_margin = MARGIN_BOTTOM
        section.left_margin = MARGIN_LEFT
        section.right_margin = MARGIN_RIGHT


def set_cell_shading(cell, color):
    """Set cell background color"""
    shading = OxmlElement('w:shd')
    shading.set(qn('w:fill'), color)
    cell._tc.get_or_add_tcPr().append(shading)


def add_page_number_header(doc):
    """Thêm số trang ở giữa phía TRÊN đầu trang (header)"""
    for section in doc.sections:
        header = section.header
        header.is_linked_to_previous = False

        p = header.paragraphs[0] if header.paragraphs else header.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER

        # Tạo field cho số trang
        run = p.add_run()
        fldChar1 = OxmlElement('w:fldChar')
        fldChar1.set(qn('w:fldCharType'), 'begin')

        instrText = OxmlElement('w:instrText')
        instrText.text = "PAGE"

        fldChar2 = OxmlElement('w:fldChar')
        fldChar2.set(qn('w:fldCharType'), 'end')

        run._r.append(fldChar1)
        run._r.append(instrText)
        run._r.append(fldChar2)

        for r in p.runs:
            r.font.name = FONT_NAME
            r.font.size = FONT_SIZE_NORMAL


def setup_styles(doc):
    """Thiết lập các style chuẩn cho document"""
    style = doc.styles['Normal']
    font = style.font
    font.name = FONT_NAME
    font.size = FONT_SIZE_NORMAL

    pf = style.paragraph_format
    pf.line_spacing = LINE_SPACING
    pf.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    # Đảm bảo font tiếng Việt
    style._element.rPr.rFonts.set(qn('w:eastAsia'), FONT_NAME)


def set_font(run, size=FONT_SIZE_NORMAL, bold=False, italic=False):
    """Helper to set font properties"""
    run.font.name = FONT_NAME
    run.font.size = size
    run.bold = bold
    run.italic = italic
    run._element.rPr.rFonts.set(qn('w:eastAsia'), FONT_NAME)


def add_chapter_title(doc, text, add_page_break=True):
    """
    Tiêu đề chương: 18pt, Bold, căn giữa
    Before: 0pt, After: 12pt
    Bắt đầu từ trang mới
    """
    if add_page_break:
        doc.add_page_break()

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(12)

    run = p.add_run(text.upper())
    set_font(run, FONT_SIZE_CHAPTER, bold=True)

    return p


def add_section_title(doc, text):
    """
    Tiêu đề mục (1.1, 1.2): 16pt, Bold, căn trái
    Before: 6pt, After: 6pt, không thụt đầu dòng
    """
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.first_line_indent = Pt(0)

    run = p.add_run(text)
    set_font(run, FONT_SIZE_SECTION, bold=True)

    return p


def add_subsection_title(doc, text):
    """
    Tiêu đề tiểu mục (1.1.1, 1.1.2): 14pt, Bold, căn trái
    Before: 6pt, After: 6pt, không thụt đầu dòng
    """
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.first_line_indent = Pt(0)

    run = p.add_run(text)
    set_font(run, FONT_SIZE_SUBSECTION, bold=True)

    return p


def add_sub_subsection_title(doc, text):
    """
    Tiêu đề cấp nhỏ hơn (a), b),...): 13pt, Bold + Italic, căn trái
    """
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.first_line_indent = Pt(0)

    run = p.add_run(text)
    set_font(run, FONT_SIZE_NORMAL, bold=True, italic=True)

    return p


def add_paragraph_text(doc, text, first_line_indent=True):
    """
    Đoạn văn: 13pt, Justify, thụt đầu dòng 1cm, giãn dòng 1.2
    """
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.line_spacing = LINE_SPACING

    if first_line_indent:
        p.paragraph_format.first_line_indent = FIRST_LINE_INDENT

    run = p.add_run(text)
    set_font(run, FONT_SIZE_NORMAL)

    return p


def add_bullet_list(doc, items):
    """Thêm danh sách bullet"""
    for item in items:
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.left_indent = Cm(1.0)
        p.paragraph_format.line_spacing = LINE_SPACING

        run = p.add_run(item)
        set_font(run, FONT_SIZE_NORMAL)


def add_numbered_list(doc, items, start=1):
    """Thêm danh sách đánh số"""
    for i, item in enumerate(items, start):
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(1.0)
        p.paragraph_format.line_spacing = LINE_SPACING

        run = p.add_run(f"{i}. {item}")
        set_font(run, FONT_SIZE_NORMAL)


def add_table_with_caption(doc, caption, headers, rows, col_widths=None):
    """
    Thêm bảng với tiêu đề (caption) ở PHÍA TRÊN bảng
    Caption: đậm, căn giữa

    Args:
        doc: Document object
        caption: Tiêu đề bảng
        headers: List các header cột
        rows: List các hàng dữ liệu
        col_widths: List độ rộng cột (cm), ví dụ: [1.5, 6.0, 8.5]
    """
    # Caption phía trên
    p_caption = doc.add_paragraph()
    p_caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_caption.paragraph_format.space_before = Pt(12)
    p_caption.paragraph_format.space_after = Pt(6)

    run = p_caption.add_run(caption)
    set_font(run, FONT_SIZE_CAPTION, bold=True)

    # Tạo bảng
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER

    # Header row
    header_cells = table.rows[0].cells
    for i, header in enumerate(headers):
        header_cells[i].text = header
        # Thiết lập độ rộng cho header cell
        if col_widths and i < len(col_widths):
            header_cells[i].width = Cm(col_widths[i])
        for paragraph in header_cells[i].paragraphs:
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in paragraph.runs:
                set_font(run, FONT_SIZE_TABLE, bold=True)
        set_cell_shading(header_cells[i], 'D9E2F3')

    # Data rows
    for row_data in rows:
        row = table.add_row()
        for i, cell_text in enumerate(row_data):
            row.cells[i].text = str(cell_text)
            # Thiết lập độ rộng cho data cell
            if col_widths and i < len(col_widths):
                row.cells[i].width = Cm(col_widths[i])
            for paragraph in row.cells[i].paragraphs:
                for run in paragraph.runs:
                    set_font(run, FONT_SIZE_TABLE)

    # Space after table
    doc.add_paragraph()

    return table


def add_figure_placeholder(doc, caption):
    """
    Thêm placeholder cho hình vẽ với caption ở PHÍA DƯỚI hình
    """
    # Placeholder
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(12)

    run = p.add_run("[Chèn hình vẽ tại đây]")
    run.italic = True
    run.font.name = FONT_NAME
    run.font.size = FONT_SIZE_NORMAL
    run.font.color.rgb = RGBColor(128, 128, 128)

    # Caption phía dưới
    p_caption = doc.add_paragraph()
    p_caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_caption.paragraph_format.space_after = Pt(12)

    run = p_caption.add_run(caption)
    set_font(run, FONT_SIZE_CAPTION, bold=True)


def add_border_to_paragraph(paragraph):
    """Add border around paragraph (for cover page)"""
    pPr = paragraph._p.get_or_add_pPr()
    pBdr = OxmlElement('w:pBdr')

    for border_name in ['top', 'left', 'bottom', 'right']:
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), 'single')
        border.set(qn('w:sz'), '24')  # Border width
        border.set(qn('w:space'), '1')
        border.set(qn('w:color'), '000000')
        pBdr.append(border)

    pPr.append(pBdr)


def add_title_page(doc):
    """
    Tạo trang bìa theo mẫu quy định ĐH GTVT (trang 6 PDF)
    Không có khung viền, bố cục theo đúng mẫu
    """
    import os

    # === 1. TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI (không đậm) ===
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("TRƯỜNG ĐẠI HỌC GIAO THÔNG VẬN TẢI")
    set_font(run, Pt(14), bold=False)

    # === 2. KHOA CÔNG NGHỆ THÔNG TIN (đậm) ===
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("KHOA CÔNG NGHỆ THÔNG TIN")
    set_font(run, Pt(14), bold=True)
    # Thêm underline
    run.font.underline = True

    # === 3. LOGO ===
    logo_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'logo_utc.png')
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(24)
    p.paragraph_format.space_after = Pt(24)

    if os.path.exists(logo_path):
        run = p.add_run()
        run.add_picture(logo_path, width=Cm(3.5))  # Logo 3.5cm width
    else:
        run = p.add_run("[LOGO TRƯỜNG]")
        run.font.color.rgb = RGBColor(128, 128, 128)
        set_font(run, Pt(12))

    # === 4. BÁO CÁO THỰC TẬP TỐT NGHIỆP (chữ lớn, đậm) ===
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(24)
    p.paragraph_format.space_after = Pt(24)
    run = p.add_run("BÁO CÁO THỰC TẬP TỐT NGHIỆP")
    set_font(run, Pt(26), bold=True)

    # === 5. ĐỀ TÀI ===
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(6)
    run = p.add_run("ĐỀ TÀI")
    set_font(run, Pt(14), bold=False)

    # === 6. TÊN ĐỀ TÀI (đậm) ===
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(36)
    run = p.add_run("THIẾT KẾ HỆ THỐNG PHẦN MỀM\nTẠI DOANH NGHIỆP PHÁT TRIỂN PHẦN MỀM")
    set_font(run, Pt(16), bold=True)

    # === 7. Thông tin sinh viên (căn trái, có dấu : thẳng hàng) ===
    info = [
        ("Giảng viên hướng dẫn", "ThS. Nguyễn Đức Dư"),
        ("Sinh viên thực hiện", "[Họ và tên]"),
        ("Lớp", "[Tên lớp]"),
        ("Mã sinh viên", "[MSSV]"),
    ]

    for label, value in info:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.left_indent = Cm(3)
        p.paragraph_format.space_before = Pt(6)
        p.paragraph_format.space_after = Pt(6)

        run1 = p.add_run(f"{label}")
        set_font(run1, Pt(14), bold=False)

        run2 = p.add_run(f"\t: {value}")
        set_font(run2, Pt(14), bold=False)

    # === 8. Hà Nội – 2026 (đậm, ở cuối trang) ===
    # Thêm khoảng trống để đẩy xuống cuối trang
    for _ in range(4):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run("Hà Nội – 2026")
    set_font(run, Pt(14), bold=True)


def add_toc_page(doc):
    """Thêm trang Mục lục"""
    add_chapter_title(doc, "MỤC LỤC")

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("[Tạo mục lục tự động: References → Table of Contents]")
    run.italic = True
    run.font.name = FONT_NAME
    run.font.size = Pt(12)
    run.font.color.rgb = RGBColor(128, 128, 128)


def add_list_of_tables(doc):
    """Thêm Danh mục bảng biểu"""
    add_chapter_title(doc, "DANH MỤC BẢNG BIỂU")

    tables = [
        "Bảng 1.1. Lĩnh vực hoạt động của SY Partners",
        "Bảng 1.2. Các loại thiết kế được giao",
        "Bảng 2.1. Các tài liệu tham chiếu khi thiết kế màn hình",
        "Bảng 2.2. Các tài liệu tham chiếu khi thiết kế API",
        "Bảng 2.3. HTTP Methods trong RESTful API",
        "Bảng 2.4. Cấu trúc thiết kế Batch",
        "Bảng 2.5. Hệ thống AI Checker theo cấp độ",
        "Bảng 2.6. Tiêu chí kiểm tra Level 1 (API-DB)",
        "Bảng 3.1. Kỹ năng trước và sau thực tập",
    ]

    for item in tables:
        p = doc.add_paragraph()
        run = p.add_run(item)
        set_font(run, FONT_SIZE_NORMAL)


def add_list_of_figures(doc):
    """Thêm Danh mục hình vẽ"""
    add_chapter_title(doc, "DANH MỤC HÌNH VẼ")

    figures = [
        "Hình 2.1. Quy trình thiết kế hệ thống (có AI Check)",
        "Hình 2.2. Kiến trúc xử lý Chunk trong Spring Batch",
        "Hình 2.3. Quy trình sử dụng AI trong kiểm tra chất lượng",
        "Hình 3.1. Kiến trúc hệ thống nhiều lớp",
    ]

    for item in figures:
        p = doc.add_paragraph()
        run = p.add_run(item)
        set_font(run, FONT_SIZE_NORMAL)


def add_abbreviations(doc):
    """Thêm Danh mục từ viết tắt"""
    add_chapter_title(doc, "DANH MỤC TỪ VIẾT TẮT")

    abbreviations = [
        ("AI", "Artificial Intelligence - Trí tuệ nhân tạo"),
        ("API", "Application Programming Interface - Giao diện lập trình ứng dụng"),
        ("BrSE", "Bridge Software Engineer - Kỹ sư cầu nối"),
        ("CRUD", "Create, Read, Update, Delete - Các thao tác cơ bản với dữ liệu"),
        ("DB", "Database - Cơ sở dữ liệu"),
        ("FK", "Foreign Key - Khóa ngoại"),
        ("IPO", "Input-Process-Output - Đầu vào-Xử lý-Đầu ra"),
        ("JSON", "JavaScript Object Notation - Định dạng dữ liệu"),
        ("PK", "Primary Key - Khóa chính"),
        ("QA", "Quality Assurance / Question & Answer"),
        ("REST", "Representational State Transfer - Kiến trúc API"),
        ("SQL", "Structured Query Language - Ngôn ngữ truy vấn"),
    ]

    # Độ rộng cột theo thiết kế chuẩn
    col_widths = [3.0, 13.0]

    # Tạo bảng không có caption
    table = doc.add_table(rows=1, cols=2)
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER

    # Header
    headers = ["Từ viết tắt", "Giải thích"]
    header_cells = table.rows[0].cells
    for i, header in enumerate(headers):
        header_cells[i].text = header
        header_cells[i].width = Cm(col_widths[i])
        for paragraph in header_cells[i].paragraphs:
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in paragraph.runs:
                set_font(run, FONT_SIZE_TABLE, bold=True)
        set_cell_shading(header_cells[i], 'D9E2F3')

    # Data
    for abbr, meaning in abbreviations:
        row = table.add_row()
        row.cells[0].text = abbr
        row.cells[0].width = Cm(col_widths[0])
        row.cells[1].text = meaning
        row.cells[1].width = Cm(col_widths[1])
        for cell in row.cells:
            for paragraph in cell.paragraphs:
                for run in paragraph.runs:
                    set_font(run, FONT_SIZE_TABLE)

    doc.add_paragraph()


def add_introduction(doc):
    """MỞ ĐẦU theo mẫu báo cáo thực tập"""
    add_chapter_title(doc, "MỞ ĐẦU")

    # 1. Giới thiệu về nội dung thực tập
    add_section_title(doc, "1. Giới thiệu về nội dung thực tập")

    add_paragraph_text(doc,
        "Báo cáo này trình bày quá trình thực tập tốt nghiệp tại công ty SY PARTNERS., JSC "
        "với vị trí Kỹ sư lập trình (Software Engineer). Trong suốt thời gian thực tập, "
        "sinh viên được tham gia vào các dự án phát triển phần mềm thực tế, cụ thể là "
        "công việc thiết kế hệ thống bao gồm: thiết kế cơ sở dữ liệu, thiết kế màn hình, "
        "thiết kế API và thiết kế batch processing.")

    add_paragraph_text(doc,
        "Ngoài ra, sinh viên còn được tham gia vào việc xây dựng và sử dụng hệ thống "
        "kiểm tra chất lượng thiết kế tự động bằng AI (Artificial Intelligence), giúp "
        "nâng cao hiệu quả công việc và giảm thiểu lỗi trước khi gửi review.")

    # 2. Yêu cầu (Mục đích)
    add_section_title(doc, "2. Yêu cầu (Mục đích) của đợt thực tập")

    add_paragraph_text(doc, "Mục đích của đợt thực tập:", first_line_indent=True)

    add_bullet_list(doc, [
        "Tiếp cận môi trường doanh nghiệp và quy trình phát triển phần mềm chuyên nghiệp",
        "Áp dụng kiến thức đã học vào thực tế công việc thiết kế hệ thống",
        "Rèn luyện kỹ năng thiết kế cơ sở dữ liệu, màn hình, API và batch processing",
        "Học cách sử dụng AI trong quy trình kiểm tra chất lượng",
        "Chuẩn bị nền tảng kiến thức cho đồ án tốt nghiệp"
    ])

    add_paragraph_text(doc, "Mục đích của báo cáo:", first_line_indent=True)

    add_bullet_list(doc, [
        "Tổng hợp và trình bày các công việc đã thực hiện trong quá trình thực tập",
        "Chia sẻ kiến thức và kinh nghiệm thu được",
        "Đánh giá kết quả và rút ra bài học cho công việc sau này"
    ])

    # 3. Phương pháp thực hiện
    add_section_title(doc, "3. Phương pháp thực hiện")

    add_sub_subsection_title(doc, "a) Phương pháp nghiên cứu tài liệu")
    add_bullet_list(doc, [
        "Đọc và phân tích tài liệu thiết kế hệ thống cũ",
        "Nghiên cứu tài liệu quan điểm thiết kế (design policy)",
        "Tìm hiểu tài liệu nghiệp vụ cụ thể (business requirements)"
    ])

    add_sub_subsection_title(doc, "b) Phương pháp học qua thực hành")
    add_bullet_list(doc, [
        "Trực tiếp thực hiện các bản thiết kế",
        "Nhận phản hồi (shiteki) và sửa lỗi để cải thiện"
    ])

    add_sub_subsection_title(doc, "c) Phương pháp ứng dụng AI")
    add_bullet_list(doc, [
        "Sử dụng hệ thống AI Checker để tự kiểm tra thiết kế",
        "Tham gia xây dựng và cải tiến quy trình kiểm tra tự động"
    ])

    # 4. Cấu trúc báo cáo
    add_section_title(doc, "4. Cấu trúc báo cáo")

    add_paragraph_text(doc,
        "Ngoài phần Mở đầu và Kết luận, báo cáo được tổ chức thành 3 nội dung chính:")

    add_paragraph_text(doc,
        "Nội dung 1: Giới thiệu về doanh nghiệp và công việc - Trình bày tổng quan về "
        "SY PARTNERS., JSC và các công việc được giao trong quá trình thực tập.")

    add_paragraph_text(doc,
        "Nội dung 2: Quy trình và phương pháp thiết kế hệ thống - Mô tả chi tiết quy trình "
        "thiết kế cơ sở dữ liệu, màn hình, API, batch và ứng dụng AI trong kiểm tra chất lượng.")

    add_paragraph_text(doc,
        "Nội dung 3: Kết quả và kiến thức thu được - Tổng kết kết quả công việc và các "
        "kiến thức chuyên môn, kỹ năng mềm thu được trong quá trình thực tập.")


def add_content1(doc):
    """NỘI DUNG 1: Giới thiệu về doanh nghiệp và công việc"""
    add_chapter_title(doc, "NỘI DUNG 1\nGIỚI THIỆU VỀ DOANH NGHIỆP VÀ CÔNG VIỆC")

    # 1.1 Giới thiệu về SY Partners
    add_section_title(doc, "1.1. Giới thiệu về SY PARTNERS., JSC")

    add_subsection_title(doc, "1.1.1. Thông tin chung")

    add_paragraph_text(doc,
        "SY PARTNERS., JSC (viết tắt: SYP) là công ty công nghệ thông tin chuyên về "
        "phát triển phần mềm gia công (offshore development). Công ty được thành lập "
        "tại Hà Nội, Việt Nam vào năm 2022, với đội ngũ lãnh đạo có hơn 20 năm kinh nghiệm "
        "trong lĩnh vực phát triển phần mềm offshore trên toàn cầu.")

    add_paragraph_text(doc,
        "Tên \"SY\" trong \"SY Partners\" xuất phát từ từ tiếng Nhật \"信用\" (Shinyō), "
        "có nghĩa là \"niềm tin\" - nguyên tắc cốt lõi trong mọi mối quan hệ hợp tác của công ty.")

    add_bullet_list(doc, [
        "Tên công ty: SY PARTNERS., JSC",
        "Địa chỉ: Tầng 3, Tòa nhà Luxury, Số 99 Võ Chí Công, Quận Tây Hồ, Hà Nội",
        "Website: https://syp.vn",
        "Quy mô: Hơn 95 nhân viên (tính đến tháng 6/2024)"
    ])

    add_subsection_title(doc, "1.1.2. Lĩnh vực hoạt động")

    add_table_with_caption(doc,
        "Bảng 1.1. Lĩnh vực hoạt động của SY Partners",
        ["STT", "Lĩnh vực", "Mô tả"],
        [
            ("1", "Digital Transformation", "Tư vấn và triển khai chuyển đổi số"),
            ("2", "Legacy Modernization", "Hiện đại hóa hệ thống cũ"),
            ("3", "AI Integration", "Tích hợp các dịch vụ AI"),
            ("4", "E-Commerce", "Phát triển hệ thống thương mại điện tử"),
            ("5", "Salesforce CRM", "Tư vấn và phát triển hệ thống CRM"),
            ("6", "Data Engineering", "Xử lý và phân tích dữ liệu lớn"),
        ],
        col_widths=[1.5, 5.5, 9.0]
    )

    # 1.2 Các công việc được giao
    add_section_title(doc, "1.2. Các công việc được giao")

    add_subsection_title(doc, "1.2.1. Tổng quan công việc")

    add_paragraph_text(doc,
        "Trong quá trình thực tập với vai trò Kỹ sư lập trình (Software Engineer), "
        "sinh viên được giao các công việc thiết kế hệ thống dựa trên tài liệu đầu vào "
        "từ hệ thống cũ và yêu cầu nghiệp vụ.")

    add_table_with_caption(doc,
        "Bảng 1.2. Các loại thiết kế được giao",
        ["STT", "Loại thiết kế", "Mô tả"],
        [
            ("1", "Thiết kế cơ sở dữ liệu", "Thiết kế cấu trúc bảng, quan hệ, index"),
            ("2", "Thiết kế màn hình", "Thiết kế giao diện, bố cục, luồng điều hướng"),
            ("3", "Thiết kế API", "Thiết kế các endpoint RESTful API"),
            ("4", "Thiết kế Batch", "Thiết kế các chương trình xử lý hàng loạt"),
        ],
        col_widths=[1.5, 5.5, 9.0]
    )

    add_subsection_title(doc, "1.2.2. Quy trình công việc")

    add_paragraph_text(doc, "Công việc được thực hiện theo quy trình 10 bước:")

    add_numbered_list(doc, [
        "Tiếp nhận tài liệu đầu vào (tài liệu hệ thống cũ, nghiệp vụ)",
        "Phân tích và hiểu yêu cầu",
        "Tạo QA cho những vấn đề cần xác nhận",
        "Thực hiện thiết kế theo template chuẩn",
        "Kiểm tra bằng AI Checker trước khi gửi review",
        "Gửi review cho Leader",
        "Nhận và xử lý phản hồi (shiteki)",
        "Kiểm tra lại bằng AI Checker sau khi sửa shiteki",
        "Gửi review cho khách hàng và end-user",
        "Hoàn thiện và delivery"
    ])

    add_subsection_title(doc, "1.2.3. Quy trình trao đổi với khách hàng (QA Process)")

    add_paragraph_text(doc,
        "Đặc trưng của phát triển phần mềm offshore là cần trao đổi nhiều với khách hàng "
        "để làm rõ yêu cầu. Khi đọc tài liệu đầu vào, nếu gặp những vấn đề không rõ hoặc "
        "cần xác nhận, cần tạo QA (Question & Answer).")

    add_sub_subsection_title(doc, "a) Đối tượng QA")
    add_bullet_list(doc, [
        "Leader: Những vấn đề về quy trình, cách làm, template",
        "Khách hàng: Những vấn đề về nghiệp vụ, yêu cầu chức năng"
    ])

    add_sub_subsection_title(doc, "b) Quy trình tạo QA")
    add_numbered_list(doc, [
        "Viết QA bằng tiếng Việt trên ticket Redmine",
        "BrSE (Bridge Software Engineer) convert sang tiếng Nhật",
        "Gửi cho khách hàng duyệt",
        "Nhận câu trả lời và áp dụng vào thiết kế"
    ])


def add_content2(doc):
    """NỘI DUNG 2: Quy trình và phương pháp thiết kế hệ thống"""
    add_chapter_title(doc, "NỘI DUNG 2\nQUY TRÌNH VÀ PHƯƠNG PHÁP THIẾT KẾ HỆ THỐNG")

    # 2.1 Quy trình thiết kế tổng quan
    add_section_title(doc, "2.1. Quy trình thiết kế tổng quan")

    add_paragraph_text(doc,
        "Quy trình thiết kế hệ thống tại công ty tuân theo mô hình chuyên nghiệp với "
        "nhiều vòng review và kiểm tra chất lượng. Đặc biệt, việc ứng dụng AI Checker "
        "giúp tự động phát hiện lỗi trước khi gửi review cho leader.")

    add_figure_placeholder(doc, "Hình 2.1. Quy trình thiết kế hệ thống (có AI Check)")

    # 2.2 Thiết kế cơ sở dữ liệu
    add_section_title(doc, "2.2. Thiết kế cơ sở dữ liệu")

    add_subsection_title(doc, "2.2.1. Kiến thức về Oracle Database")

    add_paragraph_text(doc,
        "Trong quá trình thiết kế database, sinh viên làm việc với Oracle Database "
        "và học được các kiến thức quan trọng sau:")

    add_bullet_list(doc, [
        "Kiểu dữ liệu: CHAR, VARCHAR2, NUMBER, DATE, CLOB, BLOB, JSON",
        "Constraints: PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK",
        "Index: B-tree index, Bitmap index, Function-based index",
        "Naming convention: Quy tắc đặt tên chuẩn cho table, column, index"
    ])

    add_subsection_title(doc, "2.2.2. Cấu trúc định nghĩa bảng")

    add_paragraph_text(doc, "Một định nghĩa bảng tiêu chuẩn bao gồm:")

    add_bullet_list(doc, [
        "Entity Info: Tên logic, tên vật lý, hệ thống",
        "Column Info: Chi tiết các cột (tên, kiểu dữ liệu, ràng buộc)",
        "Index Info: Primary Key, Foreign Key, các index"
    ])

    # 2.3 Thiết kế màn hình
    add_section_title(doc, "2.3. Thiết kế màn hình")

    add_subsection_title(doc, "2.3.1. Tổng quan")

    add_paragraph_text(doc,
        "Thiết kế màn hình là quá trình xác định giao diện người dùng, bao gồm bố cục, "
        "các thành phần tương tác và luồng xử lý. Đây là loại thiết kế phức tạp nhất "
        "vì cần tham chiếu đến nhiều tài liệu liên quan.")

    add_subsection_title(doc, "2.3.2. Các tài liệu tham chiếu")

    add_table_with_caption(doc,
        "Bảng 2.1. Các tài liệu tham chiếu khi thiết kế màn hình",
        ["STT", "Tài liệu", "Mục đích sử dụng"],
        [
            ("1", "File di chuyển màn hình", "Xác định điểm đến khi click button"),
            ("2", "File message", "Hiển thị thông báo lỗi, cảnh báo"),
            ("3", "File validate đơn lẻ", "Validate format, độ dài, kiểu dữ liệu"),
            ("4", "File validate tương quan", "Validate logic phụ thuộc lẫn nhau"),
            ("5", "Thiết kế API", "Mapping dữ liệu màn hình với API"),
            ("6", "Thiết kế DB", "Hiểu cấu trúc dữ liệu"),
        ],
        col_widths=[1.5, 5.5, 9.0]
    )

    add_subsection_title(doc, "2.3.3. Validation trong thiết kế màn hình")

    add_sub_subsection_title(doc, "a) Validate đơn lẻ (Single field validation)")
    add_bullet_list(doc, [
        "Bắt buộc nhập (required)",
        "Độ dài tối đa/tối thiểu",
        "Format (email, phone, date)",
        "Kiểu dữ liệu (số, chữ, alphanumeric)"
    ])

    add_sub_subsection_title(doc, "b) Validate tương quan (Cross-field validation)")
    add_bullet_list(doc, [
        "Ngày kết thúc phải sau ngày bắt đầu",
        "Mật khẩu xác nhận phải khớp với mật khẩu",
        "Tổng các giá trị không vượt quá 100%"
    ])

    # 2.4 Thiết kế API
    add_section_title(doc, "2.4. Thiết kế API RESTful")

    add_subsection_title(doc, "2.4.1. Tổng quan")

    add_paragraph_text(doc,
        "Thiết kế API là quá trình định nghĩa các endpoint để frontend và các hệ thống "
        "khác giao tiếp với backend. API trong dự án tuân theo kiến trúc RESTful.")

    add_subsection_title(doc, "2.4.2. Các tài liệu tham chiếu")

    add_table_with_caption(doc,
        "Bảng 2.2. Các tài liệu tham chiếu khi thiết kế API",
        ["STT", "Tài liệu", "Mục đích sử dụng"],
        [
            ("1", "Thiết kế DB", "Viết SQL, mapping dữ liệu"),
            ("2", "File message", "Định nghĩa error response"),
            ("3", "File HTTP Status Code", "Xác định response status"),
            ("4", "File validate", "Validate request parameters"),
        ],
        col_widths=[1.5, 5.5, 9.0]
    )

    add_subsection_title(doc, "2.4.3. HTTP Methods")

    add_table_with_caption(doc,
        "Bảng 2.3. HTTP Methods trong RESTful API",
        ["Method", "CRUD", "Mô tả"],
        [
            ("GET", "Read", "Lấy dữ liệu"),
            ("POST", "Create", "Tạo mới"),
            ("PUT", "Update", "Cập nhật toàn bộ"),
            ("PATCH", "Update", "Cập nhật một phần"),
            ("DELETE", "Delete", "Xóa"),
        ],
        col_widths=[4.0, 4.0, 8.0]
    )

    # 2.5 Thiết kế Batch
    add_section_title(doc, "2.5. Thiết kế Batch Processing")

    add_paragraph_text(doc,
        "Batch processing là phương pháp xử lý khối lượng lớn dữ liệu theo lịch trình "
        "định sẵn, không cần tương tác người dùng.")

    add_table_with_caption(doc,
        "Bảng 2.4. Cấu trúc thiết kế Batch",
        ["STT", "Thành phần", "Mô tả"],
        [
            ("1", "Tổng quan chức năng", "Mục đích, đối tượng batch"),
            ("2", "Shell Script", "Tham số đầu vào, mã kết thúc"),
            ("3", "Xử lý Java", "Logic xử lý chính"),
            ("4", "Yêu cầu tìm kiếm", "Các câu SQL SELECT"),
            ("5", "Yêu cầu cập nhật", "SQL INSERT/UPDATE/DELETE"),
        ],
        col_widths=[1.5, 5.5, 9.0]
    )

    add_figure_placeholder(doc, "Hình 2.2. Kiến trúc xử lý Chunk trong Spring Batch")

    # 2.6 AI Checker
    add_section_title(doc, "2.6. Ứng dụng AI trong kiểm tra chất lượng")

    add_subsection_title(doc, "2.6.1. Giới thiệu hệ thống AI Checker")

    add_paragraph_text(doc,
        "Trong quá trình thực tập, sinh viên được tham gia xây dựng và sử dụng hệ thống "
        "kiểm tra chất lượng thiết kế tự động bằng AI (Claude AI). Hệ thống này được "
        "thiết kế theo kiến trúc multi-level.")

    add_table_with_caption(doc,
        "Bảng 2.5. Hệ thống AI Checker theo cấp độ",
        ["Level", "Tên", "Mục đích kiểm tra"],
        [
            ("Level 1", "API-DB Checker", "Tính nhất quán giữa API và DB"),
            ("Level 2", "Screen-API Checker", "Tính nhất quán giữa Screen và API"),
            ("Level 3", "Text Quality", "Chất lượng văn bản, ngôn ngữ"),
            ("Level ALL", "Master Orchestrator", "Chạy tất cả Level song song"),
        ],
        col_widths=[3.0, 5.0, 8.0]
    )

    add_subsection_title(doc, "2.6.2. Tiêu chí kiểm tra Level 1 (API-DB)")

    add_table_with_caption(doc,
        "Bảng 2.6. Tiêu chí kiểm tra Level 1 (API-DB)",
        ["#", "Tiêu chí", "Mô tả"],
        [
            ("1", "Kiểu dữ liệu & Độ dài", "So sánh type, length của API và DB"),
            ("2", "result.count", "API danh sách phải có result.count"),
            ("3", "Chất lượng văn bản", "Ngữ pháp đúng, văn phong"),
            ("4", "Không có tiếng Việt", "Loại bỏ tiếng Việt còn sót"),
            ("5", "Common columns", "Các cột chung phải có đủ"),
            ("6", "Foreign Key rules", "Quy tắc biểu thị FK đúng chuẩn"),
        ],
        col_widths=[1.5, 5.5, 9.0]
    )

    add_figure_placeholder(doc, "Hình 2.3. Quy trình sử dụng AI trong kiểm tra chất lượng")


def add_content3(doc):
    """NỘI DUNG 3: Kết quả và kiến thức thu được"""
    add_chapter_title(doc, "NỘI DUNG 3\nKẾT QUẢ VÀ KIẾN THỨC THU ĐƯỢC")

    # 3.1 Kết quả công việc
    add_section_title(doc, "3.1. Kết quả công việc")

    add_subsection_title(doc, "3.1.1. Sản phẩm đầu ra")

    add_paragraph_text(doc,
        "Các thiết kế được hoàn thành đạt tiêu chuẩn của doanh nghiệp và khách hàng, "
        "bao gồm thiết kế cơ sở dữ liệu, thiết kế màn hình, thiết kế API và thiết kế Batch.")

    add_subsection_title(doc, "3.1.2. Cải thiện nhờ AI Checker")

    add_paragraph_text(doc,
        "Việc sử dụng AI Checker đã giúp cải thiện đáng kể chất lượng thiết kế:")

    add_bullet_list(doc, [
        "Phát hiện 60-70% lỗi trước khi gửi leader",
        "Giảm số vòng review từ 3-4 xuống còn 1-2",
        "Tiết kiệm thời gian tổng thể cho cả team"
    ])

    # 3.2 Kiến thức chuyên môn
    add_section_title(doc, "3.2. Kiến thức chuyên môn thu được")

    add_table_with_caption(doc,
        "Bảng 3.1. Kỹ năng trước và sau thực tập",
        ["Kỹ năng", "Trước thực tập", "Sau thực tập"],
        [
            ("Thiết kế DB", "Cơ bản", "Có thể thiết kế phức tạp"),
            ("SQL", "SELECT đơn giản", "JOIN, Subquery"),
            ("API Design", "Chưa có kinh nghiệm", "Hiểu RESTful principles"),
            ("Batch Design", "Không biết", "Nắm được quy trình"),
            ("Viết QA", "Không biết", "Xác định và viết QA rõ ràng"),
            ("AI Tools", "Không biết", "Sử dụng thành thạo"),
        ],
        col_widths=[4.0, 5.5, 6.5]
    )

    add_subsection_title(doc, "3.2.1. Thiết kế cơ sở dữ liệu")
    add_bullet_list(doc, [
        "Nắm vững cách thiết kế schema database với Oracle",
        "Hiểu các kiểu dữ liệu và cách sử dụng phù hợp",
        "Biết cách định nghĩa constraints và indexes"
    ])

    add_subsection_title(doc, "3.2.2. Thiết kế màn hình")
    add_bullet_list(doc, [
        "Hiểu cách thiết kế giao diện người dùng theo chuẩn",
        "Nắm được quy trình tham chiếu các tài liệu liên quan",
        "Biết cách định nghĩa validation đơn lẻ và tương quan"
    ])

    add_subsection_title(doc, "3.2.3. Thiết kế API")
    add_bullet_list(doc, [
        "Hiểu quy trình thiết kế API RESTful",
        "Biết cách sử dụng HTTP methods và status codes",
        "Hiểu về validation và error handling"
    ])

    add_subsection_title(doc, "3.2.4. Ứng dụng AI trong công việc")
    add_bullet_list(doc, [
        "Hiểu cách tích hợp AI vào quy trình làm việc",
        "Biết cách viết prompt hiệu quả cho AI",
        "Nắm được khả năng và giới hạn của AI trong QA"
    ])

    # 3.3 Kiến thức mềm
    add_section_title(doc, "3.3. Kiến thức mềm thu được")

    add_bullet_list(doc, [
        "Kỹ năng đọc hiểu tài liệu kỹ thuật",
        "Làm việc độc lập và có trách nhiệm",
        "Quản lý thời gian và ưu tiên công việc",
        "Hiểu mô hình offshore development",
        "Biết quy trình làm việc với khách hàng nước ngoài"
    ])

    add_figure_placeholder(doc, "Hình 3.1. Kiến trúc hệ thống nhiều lớp")


def add_conclusion(doc):
    """KẾT LUẬN"""
    add_chapter_title(doc, "KẾT LUẬN")

    add_section_title(doc, "1. Tổng kết quá trình thực tập")

    add_paragraph_text(doc,
        "Qua quá trình thực tập tại SY PARTNERS., JSC, sinh viên đã hoàn thành các "
        "mục tiêu đề ra và thu được nhiều kiến thức, kinh nghiệm quý báu.")

    add_bullet_list(doc, [
        "Hoàn thành các công việc thiết kế hệ thống theo yêu cầu",
        "Nắm vững quy trình thiết kế hệ thống chuyên nghiệp",
        "Tham gia xây dựng và sử dụng hệ thống AI Checker",
        "Tích lũy kiến thức và kinh nghiệm cho đồ án tốt nghiệp"
    ])

    add_section_title(doc, "2. Những đóng góp của đề tài")

    add_bullet_list(doc, [
        "Kinh nghiệm thiết kế hệ thống theo chuẩn công nghiệp",
        "Hiểu biết về quy trình phát triển phần mềm offshore",
        "Kinh nghiệm ứng dụng AI trong quy trình QA",
        "Nền tảng vững chắc cho đồ án tốt nghiệp"
    ])


def add_references(doc):
    """TÀI LIỆU THAM KHẢO"""
    add_chapter_title(doc, "TÀI LIỆU THAM KHẢO")

    references = [
        "[1] Oracle (2024). Oracle Database Documentation, <https://docs.oracle.com/en/database/>, truy cập 01/2026.",
        "[2] VMware (2024). Spring Batch Reference Documentation, <https://docs.spring.io/spring-batch/>, truy cập 01/2026.",
        "[3] RESTfulAPI.net (2024). RESTful API Design Guidelines, <https://restfulapi.net/>, truy cập 01/2026.",
        "[4] Anthropic (2024). Claude Documentation, <https://docs.anthropic.com/>, truy cập 01/2026.",
        "[5] Tài liệu nội bộ công ty SY PARTNERS., JSC (không công khai)."
    ]

    for ref in references:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Cm(0.5)
        p.paragraph_format.first_line_indent = Cm(-0.5)
        run = p.add_run(ref)
        set_font(run, FONT_SIZE_NORMAL)


def add_appendix(doc):
    """PHỤ LỤC"""
    add_chapter_title(doc, "PHỤ LỤC")

    add_section_title(doc, "Phụ lục A: Giấy xác nhận kết quả thực tập")

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("[Đính kèm giấy xác nhận từ doanh nghiệp]")
    run.italic = True
    run.font.name = FONT_NAME
    run.font.size = FONT_SIZE_NORMAL
    run.font.color.rgb = RGBColor(128, 128, 128)

    add_section_title(doc, "Phụ lục B: Nhật ký thực tập")

    diary = [
        ("Tuần 1", "Làm quen môi trường, tìm hiểu dự án SORA"),
        ("Tuần 2", "Training thiết kế cơ sở dữ liệu"),
        ("Tuần 3-4", "Thực hành thiết kế DB, nhận shiteki đầu tiên"),
        ("Tuần 5-6", "Training thiết kế màn hình"),
        ("Tuần 7-8", "Training thiết kế API RESTful"),
        ("Tuần 9-10", "Giới thiệu AI Checker, training thiết kế Batch"),
        ("Tuần 11-12", "Đóng góp cải tiến AI Checker, thiết kế độc lập"),
        ("Tuần cuối", "Hoàn thành thiết kế, tổng kết, viết báo cáo"),
    ]

    # Tạo bảng
    table = doc.add_table(rows=1, cols=2)
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER

    headers = ["Thời gian", "Nội dung chính"]
    header_cells = table.rows[0].cells
    for i, header in enumerate(headers):
        header_cells[i].text = header
        for paragraph in header_cells[i].paragraphs:
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in paragraph.runs:
                set_font(run, FONT_SIZE_TABLE, bold=True)
        set_cell_shading(header_cells[i], 'D9E2F3')

    for time, content in diary:
        row = table.add_row()
        row.cells[0].text = time
        row.cells[1].text = content
        for cell in row.cells:
            for paragraph in cell.paragraphs:
                for run in paragraph.runs:
                    set_font(run, FONT_SIZE_TABLE)

    # Chữ ký
    doc.add_paragraph()
    doc.add_paragraph()

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = p.add_run("Hà Nội, ngày ... tháng 01 năm 2026")
    run.italic = True
    set_font(run, FONT_SIZE_NORMAL, italic=True)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = p.add_run("Sinh viên thực tập")
    set_font(run, FONT_SIZE_NORMAL, bold=True)

    doc.add_paragraph()
    doc.add_paragraph()

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = p.add_run("[Ký tên]")
    set_font(run, FONT_SIZE_NORMAL)

    doc.add_paragraph()

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = p.add_run("[Họ và tên]")
    set_font(run, FONT_SIZE_NORMAL)


def create_report():
    """Hàm chính tạo báo cáo"""
    print("Đang tạo báo cáo thực tập theo quy định ĐH GTVT...")

    doc = Document()

    # Thiết lập document
    set_document_margins(doc)
    setup_styles(doc)

    # Trang bìa (không có số trang)
    add_title_page(doc)

    # Các phần còn lại
    add_toc_page(doc)
    add_list_of_tables(doc)
    add_list_of_figures(doc)
    add_abbreviations(doc)

    # Nội dung chính
    add_introduction(doc)
    add_content1(doc)
    add_content2(doc)
    add_content3(doc)
    add_conclusion(doc)
    add_references(doc)
    add_appendix(doc)

    # Thêm số trang ở header (phía trên)
    add_page_number_header(doc)

    # Lưu file
    output_path = "BAO_CAO_THUC_TAP_SORA.docx"
    doc.save(output_path)

    print(f"✓ Đã tạo file: {output_path}")
    print(f"✓ Căn lề: Trái 3cm, Phải 2cm, Trên 2.5cm, Dưới 2.5cm")
    print(f"✓ Số trang: Giữa, phía trên đầu trang")
    print(f"✓ Chương: 18pt Bold, Mục: 16pt Bold, Tiểu mục: 14pt Bold")
    print(f"✓ Nội dung: 13pt, giãn dòng 1.2, thụt đầu dòng 1cm")

    return output_path


if __name__ == "__main__":
    create_report()
