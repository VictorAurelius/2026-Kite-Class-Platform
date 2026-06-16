# -*- coding: utf-8 -*-
"""
build_defense_pptx.py (v3) — Slide bảo vệ khóa luận bám THESIS FINAL, NHÚNG ẢNH
THẬT, ÁP ĐÚNG THIẾT KẾ TEMPLATE UTC (3 ảnh nền: bìa / nội dung / kết thúc).

- Nền template: defense/assets/tpl-bg-{cover,content,section}.jpg (trích từ
  template gốc — band xanh + logo UTC + sóng). Tiêu đề đặt DƯỚI band.
- Ảnh thesis: defense/assets/fig-*.png (31 figure final từ thesis-v1.docx).
- KHÔNG dùng phụ lục — nội dung trình bày hết ở slide chính.

Chạy:  ../.venv/bin/python defense/build_defense_pptx.py   (từ 08-thesis)
Output: defense/KiteHub-baove-khoaluan-20slide.pptx
"""
import os
import struct
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.oxml.ns import qn, nsdecls
from pptx.oxml import parse_xml

HERE = os.path.dirname(os.path.abspath(__file__))
THESIS = os.path.dirname(HERE)
TEMPLATE = os.path.join(THESIS, "Presentation - File baocaodatn 16-9.pptx")
ASSETS = os.path.join(HERE, "assets")
OUT = os.path.join(HERE, "KiteHub-baove-khoaluan-20slide.pptx")

NAVY = RGBColor(0x1F, 0x3A, 0x6E)
BLUE = RGBColor(0x2E, 0x2C, 0x7E)   # band navy-tím của template
GREEN = RGBColor(0x1E, 0x7E, 0x34)
RED = RGBColor(0xB0, 0x30, 0x30)
GREY = RGBColor(0x55, 0x55, 0x55)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
LIGHT = RGBColor(0xEC, 0xEF, 0xF6)
DARK = RGBColor(0x22, 0x22, 0x22)

# Font typeface tường minh — tránh font-substitution trên máy phòng bảo vệ.
# Arial: phổ cập mọi OS + hỗ trợ dấu tiếng Việt tốt + rõ khi chiếu. Đổi 1 dòng nếu cần.
FONT_NAME = "Arial"

BG_COVER = os.path.join(ASSETS, "tpl-bg-cover.jpg")
BG_CONTENT = os.path.join(ASSETS, "tpl-bg-content.jpg")
BG_SECTION = os.path.join(ASSETS, "tpl-bg-section.jpg")

prs = Presentation(TEMPLATE)
sldIdLst = prs.slides._sldIdLst
for sldId in list(sldIdLst):
    prs.part.drop_rel(sldId.get(qn('r:id')))
    sldIdLst.remove(sldId)

L_BLANK = prs.slide_layouts[6]

_idx = [0]


def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(26)
    if head[:8] == b'\x89PNG\r\n\x1a\n':
        return struct.unpack('>II', head[16:24])
    return 1280, 720


def set_bg(slide, jpg):
    """Đặt ảnh nền full-bleed cho slide (áp thiết kế template)."""
    _img_part, rId = slide.part.get_or_add_image_part(jpg)
    bg = parse_xml(
        '<p:bg %s><p:bgPr><a:blipFill><a:blip r:embed="%s"/>'
        '<a:stretch><a:fillRect/></a:stretch></a:blipFill>'
        '<a:effectLst/></p:bgPr></p:bg>' % (nsdecls('p', 'a', 'r'), rId))
    cSld = slide._element.find(qn('p:cSld'))
    cSld.insert(0, bg)


def add_pic_fit(slide, path, box_l, box_t, box_w, box_h, border=True):
    w, h = png_size(path)
    bw, bh = Inches(box_w), Inches(box_h)
    scale = min(bw / w, bh / h)
    pw, ph = int(w * scale), int(h * scale)
    left = Inches(box_l) + (bw - pw) // 2
    top = Inches(box_t) + (bh - ph) // 2
    pic = slide.shapes.add_picture(path, left, top, pw, ph)
    if border:
        pic.line.color.rgb = RGBColor(0xC8, 0xD2, 0xE0)
        pic.line.width = Pt(0.75)
    return pic


def title_box(slide, text, size=26):
    """Tiêu đề đặt DƯỚI band xanh template, chữ navy.
    Band trong tpl-bg-content.jpg kết thúc ở y=1.146in → title top=1.28in
    để đỉnh chữ + dấu tiếng Việt KHÔNG bị band che (trước đây 1.04in → chui vào band)."""
    tb = slide.shapes.add_textbox(Inches(0.55), Inches(1.28), Inches(12.2), Inches(0.6))
    tb.text_frame.word_wrap = True
    p = tb.text_frame.paragraphs[0]
    r = p.add_run(); r.text = text
    r.font.size = Pt(size); r.font.bold = True; r.font.color.rgb = NAVY


def page_num(slide, idx):
    tb = slide.shapes.add_textbox(Inches(0.45), Inches(7.04), Inches(1.2), Inches(0.32))
    r = tb.text_frame.paragraphs[0].add_run(); r.text = str(idx)
    r.font.size = Pt(10); r.font.color.rgb = GREY


def content_slide(title):
    s = prs.slides.add_slide(L_BLANK)
    _idx[0] += 1
    set_bg(s, BG_CONTENT)
    if title:
        title_box(s, title)
    page_num(s, _idx[0])
    return s


def section_slide(title, subtitle=None):
    s = prs.slides.add_slide(L_BLANK)
    _idx[0] += 1
    set_bg(s, BG_SECTION)
    tb = s.shapes.add_textbox(Inches(0.8), Inches(2.9), Inches(11.7), Inches(1.2))
    p = tb.text_frame.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = title
    r.font.size = Pt(40); r.font.bold = True; r.font.color.rgb = NAVY
    if subtitle:
        tb2 = s.shapes.add_textbox(Inches(1.2), Inches(4.2), Inches(10.9), Inches(1.0))
        p2 = tb2.text_frame.paragraphs[0]; p2.alignment = PP_ALIGN.CENTER
        p2.text = subtitle
        for rr in p2.runs:
            rr.font.size = Pt(16); rr.font.color.rgb = GREY
    return s


def notes(slide, text):
    slide.notes_slide.notes_text_frame.text = text


def bullets(slide, items, left=0.75, top=1.95, width=11.9, height=4.6, base=19):
    tb = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(height))
    tf = tb.text_frame; tf.word_wrap = True
    first = True
    for it in items:
        text, level, bold, color = (list(it) + [None, None])[:4]
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.level = level
        p.space_after = Pt(10 if level == 0 else 6)
        run = p.add_run()
        run.text = ("▸  " if level == 0 else "•  ") + text
        run.font.size = Pt(base - level * 3)
        run.font.bold = bool(bold)
        run.font.color.rgb = color if color else DARK
    return tb


def table(slide, rows, top=1.9, left=0.55, width=12.2, col_widths=None, fs=12):
    nr, nc = len(rows), len(rows[0])
    t = slide.shapes.add_table(nr, nc, Inches(left), Inches(top), Inches(width),
                               Inches(0.4 * nr)).table
    if col_widths:
        for i, w in enumerate(col_widths):
            t.columns[i].width = Inches(w)
    for r in range(nr):
        for c in range(nc):
            cell = t.cell(r, c)
            cell.text = rows[r][c]
            cell.margin_left = Inches(0.07); cell.margin_right = Inches(0.07)
            cell.margin_top = Inches(0.02); cell.margin_bottom = Inches(0.02)
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            for para in cell.text_frame.paragraphs:
                for run in para.runs:
                    run.font.size = Pt(fs)
                    if r == 0:
                        run.font.bold = True; run.font.color.rgb = WHITE
            cell.fill.solid()
            cell.fill.fore_color.rgb = BLUE if r == 0 else (LIGHT if r % 2 == 0 else WHITE)
    return t


def caption(slide, text, top=6.5, size=11, left=0.6, width=12.1):
    tb = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(0.5))
    p = tb.text_frame.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    p.word_wrap = True
    r = p.add_run(); r.text = text
    r.font.size = Pt(size); r.font.italic = True; r.font.color.rgb = GREY


PANEL_FILL = RGBColor(0xFB, 0xFC, 0xFE)
PANEL_LINE = RGBColor(0xC5, 0xD0, 0xE0)


def panel(slide, l, t, w, h, fill=PANEL_FILL, line=PANEL_LINE):
    from pptx.enum.shapes import MSO_SHAPE
    sp = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(l), Inches(t),
                                Inches(w), Inches(h))
    sp.adjustments[0] = 0.025
    sp.fill.solid(); sp.fill.fore_color.rgb = fill
    sp.line.color.rgb = line; sp.line.width = Pt(1)
    sp.shadow.inherit = False
    return sp


def diagram(slide, img, l=0.5, t=1.74, w=12.33, h=4.78, pad=0.22):
    """Khung panel trắng + sơ đồ phóng to căn giữa (tách sơ đồ tối khỏi nền sóng)."""
    panel(slide, l, t, w, h)
    add_pic_fit(slide, img, l + pad, t + pad, w - 2 * pad, h - 2 * pad, border=False)


def kpi_cards(slide, cards, top=2.05, h=2.3):
    """cards: list of (number, label, sub). Thẻ số lớn."""
    from pptx.enum.shapes import MSO_SHAPE
    n = len(cards); gap = 0.4
    total_w = 12.33; cw = (total_w - gap * (n - 1)) / n
    x = 0.5
    for num, label, sub in cards:
        c = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x), Inches(top),
                                   Inches(cw), Inches(h))
        c.adjustments[0] = 0.06
        c.fill.solid(); c.fill.fore_color.rgb = RGBColor(0xF2, 0xF6, 0xFC)
        c.line.color.rgb = BLUE; c.line.width = Pt(1.25)
        c.shadow.inherit = False
        tf = c.text_frame; tf.word_wrap = True
        tf.margin_top = Inches(0.18); tf.margin_left = Inches(0.1); tf.margin_right = Inches(0.1)
        p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
        r = p.add_run(); r.text = num
        r.font.size = Pt(40); r.font.bold = True; r.font.color.rgb = NAVY
        p2 = tf.add_paragraph(); p2.alignment = PP_ALIGN.CENTER
        r2 = p2.add_run(); r2.text = label
        r2.font.size = Pt(15); r2.font.bold = True; r2.font.color.rgb = DARK
        p3 = tf.add_paragraph(); p3.alignment = PP_ALIGN.CENTER
        r3 = p3.add_run(); r3.text = sub
        r3.font.size = Pt(11); r3.font.color.rgb = GREY
        x += cw + gap


def agenda_bars(slide, items, top=2.1):
    from pptx.enum.shapes import MSO_SHAPE
    y = top; bh = 0.78; gap = 0.18
    for i, txt in enumerate(items, 1):
        # số tròn
        num = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(1.1), Inches(y),
                                     Inches(0.62), Inches(0.62))
        num.fill.solid(); num.fill.fore_color.rgb = BLUE; num.line.fill.background()
        num.shadow.inherit = False
        np_ = num.text_frame.paragraphs[0]; np_.alignment = PP_ALIGN.CENTER
        rn = np_.add_run(); rn.text = str(i)
        rn.font.size = Pt(22); rn.font.bold = True; rn.font.color.rgb = WHITE
        # thanh
        bar = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1.95), Inches(y),
                                     Inches(10.4), Inches(0.62))
        bar.adjustments[0] = 0.3
        bar.fill.solid(); bar.fill.fore_color.rgb = RGBColor(0xF2, 0xF6, 0xFC)
        bar.line.color.rgb = PANEL_LINE; bar.line.width = Pt(0.75)
        bar.shadow.inherit = False
        bar.text_frame.word_wrap = True
        bar.text_frame.margin_left = Inches(0.2)
        bp = bar.text_frame.paragraphs[0]
        rb = bp.add_run(); rb.text = txt
        rb.font.size = Pt(16); rb.font.bold = True; rb.font.color.rgb = NAVY
        y += bh + gap


A = lambda n: os.path.join(ASSETS, n)

# ═══ 1. Bìa (nền image1, text ở vùng trắng dưới) ═══
s = prs.slides.add_slide(L_BLANK); _idx[0] += 1
set_bg(s, BG_COVER)
tb = s.shapes.add_textbox(Inches(0.7), Inches(4.86), Inches(11.9), Inches(1.0))
p = tb.text_frame.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
r = p.add_run(); r.text = "XÂY DỰNG HỆ THỐNG SAAS CUNG CẤP DỊCH VỤ ĐÀO TẠO"
r.font.size = Pt(23); r.font.bold = True; r.font.color.rgb = NAVY
p2 = tb.text_frame.add_paragraph(); p2.alignment = PP_ALIGN.CENTER
r2 = p2.add_run(); r2.text = "Nền tảng KiteHub & KiteClass — kiến trúc đa-tenant tích hợp AI"
r2.font.size = Pt(13); r2.font.italic = True; r2.font.color.rgb = GREY
info = s.shapes.add_textbox(Inches(0.7), Inches(6.0), Inches(11.9), Inches(1.3))
for line, bold in [
    ("Sinh viên: Nguyễn Văn Kiệt — MSSV 221230890 — Lớp CNTT1-K63", False),
    ("Giảng viên hướng dẫn: TS. Nguyễn Đức Dư", True),
    ("Bộ môn Công nghệ phần mềm — Khoa Công nghệ thông tin — UTC, 2026", False)]:
    pp = info.text_frame.add_paragraph(); pp.alignment = PP_ALIGN.CENTER
    rr = pp.add_run(); rr.text = line
    rr.font.size = Pt(13); rr.font.bold = bold; rr.font.color.rgb = DARK
notes(s, "Chào hội đồng. Em là Nguyễn Văn Kiệt, lớp CNTT1-K63, dưới hướng dẫn "
         "của thầy Nguyễn Đức Dư. Em xin trình bày khóa luận: Xây dựng hệ thống "
         "SaaS cung cấp dịch vụ đào tạo — nền tảng KiteHub. (~30 giây)")

# ═══ 2. Nội dung ═══
s = content_slide("Nội dung trình bày")
agenda_bars(s, [
    "Tổng quan: bối cảnh, khảo sát thị trường, mục tiêu  (Chương 1)",
    "Kỹ thuật AI, khung pháp lý, phương pháp luận  (Chương 1)",
    "Phân tích và thiết kế kiến trúc đa-tenant  (Chương 2)",
    "Cài đặt, triển khai AWS và kết quả  (Chương 3 & 4)",
    "Demo trực tiếp, hạn chế và kết luận",
])
notes(s, "Bài trình bày bám bốn chương: tổng quan, thiết kế kiến trúc, cài đặt "
         "và triển khai, đánh giá kết quả; khép lại bằng demo và kết luận. (~20 giây)")

# ═══ 3. Bối cảnh ═══
s = content_slide("Bối cảnh và vấn đề (§1.1)")
bullets(s, [
    ("Thông tư 29/2024/TT-BGDĐT chính thức hóa hoạt động dạy thêm có thu phí", 0, False, None),
    ("Hơn 50.000 trung tâm dạy thêm tư nhân đang hoạt động tại Việt Nam", 0, False, None),
    ("Phần lớn trung tâm vừa và nhỏ vẫn quản lý bằng Excel và nhóm Zalo", 0, False, None),
    ("Ba quan sát thúc đẩy đề tài:", 0, True, NAVY),
    ("Khoảng trống thị trường: trung tâm 100–2000 học viên cần phần mềm 0,5–1,5 triệu đồng/tháng, đa-tenant gốc", 1, False, None),
    ("Mốc pháp lý: Luật Bảo vệ dữ liệu cá nhân 2023 hiệu lực 07/2026 — tích hợp tuân thủ ngay từ thiết kế", 1, False, None),
    ("Công nghệ AI trưởng thành: API thương mại cho phép tự động hóa nhận diện thương hiệu chi phí thấp", 1, False, None),
], top=1.85, base=16)
caption(s, "Nguồn: Magenest EdTech 2024; 6Wresearch 2024–2030; VECITA 2024.", top=6.45)
notes(s, "Bối cảnh: thị trường lớn, pháp luật hợp pháp hóa dạy thêm, đa số trung "
         "tâm nhỏ quản lý thủ công. Ba quan sát hội tụ: nhu cầu, mốc pháp lý cứng, "
         "AI vừa đủ chín. (~60 giây)")

# ═══ 4. Khảo sát (ảnh grid) ═══
s = content_slide("Khảo sát hệ thống tương tự (§1.2)")
add_pic_fit(s, A("fig-1.1-beeclass.png"), 0.6, 1.85, 5.9, 2.25)
add_pic_fit(s, A("fig-1.2-mona.png"), 6.8, 1.85, 5.9, 2.25)
add_pic_fit(s, A("fig-1.3-easyedu.png"), 0.6, 4.2, 5.9, 2.15)
add_pic_fit(s, A("fig-1.4-dotb.png"), 6.8, 4.2, 5.9, 2.15)
caption(s, "Hình 1.1–1.4: BeeClass, Mona eLMS, Easy Edu, DotB — đều đơn-tenant, không AI. "
           "KiteHub khác biệt: đa-tenant RLS gốc + AI Branding.", top=6.55)
notes(s, "Khảo sát bốn hệ thống tham khảo: BeeClass (gamification nhẹ) + Mona eLMS, "
         "Easy Edu, DotB (quản lý trung tâm). Đều đơn-tenant, không AI. KiteHub khai "
         "thác khoảng trống đa-tenant gốc + AI Branding phân khúc giá thấp. (~70 giây)")

# ═══ 5. Mục tiêu + Phạm vi (Mở đầu §2–§3) ═══
s = content_slide("Mục tiêu và phạm vi nghiên cứu (Mở đầu §2–§3)")
bullets(s, [
    ("Bốn mục tiêu nghiên cứu:", 0, True, NAVY),
    ("Nền tảng SaaS đa-tenant — mở rộng 1 → hàng trăm chi nhánh không thiết kế lại kiến trúc", 1, False, None),
    ("AI Branding tự sinh logo/banner/ảnh chủ đạo — rút ngắn khởi tạo từ vài tuần xuống vài ngày", 1, False, None),
    ("Tuân thủ pháp luật VN: PDPL 2023, Luật An ninh mạng 2018, Thông tư 78/2021/TT-BTC", 1, False, None),
    ("Phương pháp luận phát triển hướng chất lượng — TDD, DDD, PDCA xuyên suốt", 1, False, None),
    ("Phạm vi: AWS Singapore Free Tier; 2 giáo viên độc lập thật (Miễn phí + Trả phí)", 0, True, NAVY),
    ("3 nhóm người dùng: GV độc lập, Chủ trung tâm, Quản lý. Phụ huynh/Học sinh K-12 — phát triển sau", 0, True, GREY),
], top=1.85, base=16)
notes(s, "Bốn mục tiêu (Mở đầu §2): SaaS đa-tenant, AI Branding, tuân thủ pháp luật, "
         "phương pháp luận chất lượng. Phạm vi (§3): AWS Singapore, hai giáo viên thật, "
         "ba nhóm người dùng; nhóm K-12 lùi sau do cần DPO/DPIA. (~60 giây)")

# ═══ 6. AI Branding (ảnh) ═══
s = content_slide("Khác biệt — AI Branding tự động (§1.3 · §3.1.2)")
bullets(s, [
    ("Tự sinh logo + ảnh bìa từ prompt văn bản + màu thương hiệu khi tenant khởi tạo", 0, False, None),
    ("OpenAI GPT-4 Vision + DALL-E 3 (vận hành) · Ollama tự host (phát triển)", 0, False, None),
    ("Ưu tiên mẫu (template-first): chỉ gọi AI khi cần; xem trước bắt buộc đạt WCAG AA", 0, False, None),
    ("Bộ phân loại an toàn nội dung trước khi triển khai; gói FREE tạo lại tối đa 3 lần/ngày", 0, False, None),
    ("Chi phí ~0,04 USD/ảnh DALL-E 3 — không hệ tham khảo nào có tính năng này", 0, True, GREEN),
], left=0.55, top=1.85, width=6.7, base=15)
diagram(s, A("fig-3.4-ai-wizard.png"), 7.2, 1.8, 5.85, 4.7)
caption(s, "Hình 3.4 — Trình hướng dẫn AI Branding cho chủ trung tâm.", top=6.55, left=7.0, width=6.0)
notes(s, "Yếu tố khác biệt thứ hai (§1.3): tự động hóa nhận diện thương hiệu. "
         "Vận hành dùng OpenAI GPT-4 Vision + DALL-E 3, phát triển dùng Ollama tự "
         "host. Ưu tiên mẫu, chỉ gọi AI khi cần; xem trước bắt buộc đạt WCAG AA + "
         "qua bộ phân loại an toàn nội dung. Bên phải là wizard thực tế. (~80 giây)")

# ═══ 7. Pháp luật VN ═══
s = content_slide("Khác biệt — Tuân thủ pháp luật VN theo thiết kế (§1.3 · §2.1.2)")
table(s, [
    ["Văn bản", "Yêu cầu chính", "Cách hiện thực"],
    ["PDPL 2023", "Consent cụ thể; quyền truy cập/xóa; DPO khi >10k chủ thể; báo vi phạm 72h", "consent_record; quy trình DSAR; audit log bất biến V60 (Điều 11)"],
    ["Luật An ninh mạng 2018\n+ Nghị định 53/2022", "Lưu trữ dữ liệu tại VN khi vượt 1 triệu người dùng", "Lộ trình chuyển AWS Hà Nội Local Zone / Viettel / VNG"],
    ["Thông tư 78/2021/TT-BTC", "Hóa đơn điện tử, kết nối cơ quan thuế", "Tích hợp đối tác MISA MeInvoice"],
], top=2.0, fs=12, col_widths=[3.1, 4.7, 4.4])
caption(s, "Tuân thủ ngay từ thiết kế (compliance-by-design): consent, DSAR, audit log bất biến tích hợp từ schema.", top=6.2)
notes(s, "Ba trụ cột pháp luật tích hợp từ thiết kế. PDPL mốc cứng 07/2026, với "
         "consent_record, quy trình DSAR và audit log bất biến cho Điều 11. Luật "
         "An ninh mạng có ngưỡng chưa chạm, có lộ trình. Thông tư 78 dùng đối "
         "tác MISA. (~70 giây)")

# ═══ 8. Phương pháp luận (ảnh) ═══
s = content_slide("Phương pháp luận hướng chất lượng (Mở đầu §4 · §3.2)")
bullets(s, [
    ("Kết hợp nghiên cứu lý thuyết (so sánh 4 hệ thống) + thực nghiệm (thiết kế + hiện thực)", 0, False, None),
    ("Phát triển hướng kiểm thử (TDD) — coverage ≥ 75% line trên module nghiệp vụ", 0, False, None),
    ("Thiết kế hướng miền (DDD) tách control-plane / domain-plane", 0, False, None),
    ("Chu trình cải tiến liên tục Plan-Do-Check-Act (PDCA) duy trì chất lượng mã + tài liệu", 0, False, None),
    ("Cơ sở: Beck TDD (2002), Evans DDD (2003), Deming PDCA (1986), IEEE 730 SQA", 0, True, GREY),
], left=0.55, top=1.85, width=6.9, base=16)
diagram(s, A("fig-3.6-test-pyramid.png"), 7.4, 1.85, 5.6, 4.6)
caption(s, "Hình 3.6 — Kim tự tháp kiểm thử áp dụng cho KiteHub.", top=6.5, left=7.2, width=5.6)
notes(s, "Phương pháp luận kết hợp lý thuyết (so sánh hệ tham khảo) và thực "
         "nghiệm. Ba trụ cột có cơ sở: TDD (Beck), DDD (Evans), PDCA (Deming), "
         "theo chuẩn SQA IEEE 730. Kim tự tháp kiểm thử minh họa phân bố ba tầng "
         "test (~985 test). (~50 giây)")

# ═══ 9. C4 context ═══
s = content_slide("Kiến trúc tổng thể — C4 Level 1 (Hình 2.1)")
diagram(s, A("fig-2.1-c4-context.png"))
caption(s, "Hình 2.1 — Ngữ cảnh C4 L1: 8 nhóm actor + 6 hệ thống ngoài; mọi truy cập qua HTTPS, không actor nào chạm DB trực tiếp.", top=6.5)
notes(s, "Sơ đồ ngữ cảnh C4 Level 1 (Brown): Kite Platform tương tác 8 nhóm actor "
         "và 6 hệ thống bên ngoài. Mọi actor truy cập qua HTTPS (TLS 1.2+); hệ "
         "thống ngoài cô lập qua adapter pattern (NotificationChannel cho email, "
         "PaymentProcessor cho VietQR). Không actor nào chạm DB trực tiếp — đều "
         "qua biên trust gateway. (~70 giây)")

# ═══ 10. C4 container ═══
s = content_slide("Phân rã container — C4 Level 2 (Hình 2.2)")
diagram(s, A("fig-2.2-c4-container.png"))
caption(s, "Hình 2.2 — Container C4 L2: 4 cụm — Frontend (2 Next.js) · Gateway · 6 service KiteHub + KiteClass core · 4 hạ tầng kite-.", top=6.5)
notes(s, "Sơ đồ container C4 Level 2: bốn cụm. Frontend gồm hai Next.js (kitehub "
         "cổng 3001 marketing/quản trị, kiteclass cổng 3000 giáo dục, ~85% mobile). "
         "Gateway (Spring Cloud Gateway cổng 9000) là điểm vào duy nhất. Cụm dịch "
         "vụ: 6 service KiteHub + kiteclass-core. Hạ tầng dùng chung 4 container "
         "kite- (Postgres/Redis/RabbitMQ/MinIO). Tổng 17 thành phần. (~60 giây)")

# ═══ 11. RLS ═══
s = content_slide("Khác biệt — Cô lập đa-tenant bằng RLS (§2.2.3)")
table(s, [
    ["Mô hình", "Chi phí", "Cô lập", "Quy mô", "Quyết định"],
    ["Instance mỗi tenant", "Rất cao", "Tuyệt đối", "≤10", "Loại"],
    ["Database mỗi tenant", "Cao", "Mạnh", "10–100", "Loại"],
    ["Schema mỗi tenant", "Trung bình", "Khá", "100–1000", "Cân nhắc sau"],
    ["Row-Level Security", "Thấp", "DB engine ép buộc", "≥1000", "Áp dụng"],
], top=1.95, fs=13, col_widths=[2.9, 1.9, 2.7, 2.3, 2.4])
bullets(s, [
    ("RLS policy + biến phiên app.current_tenant_id — Postgres ép lọc ở MỖI truy vấn", 0, True, NAVY),
    ("Mô hình Pool theo AWS Well-Architected SaaS Lens; NULL force-fail + HikariCP GUC reset", 0, False, None),
], top=4.75, height=1.7, base=15)
notes(s, "Đồ án đánh giá 6 pattern multi-tenant, chọn Shared Database + tenant_id "
         "+ PostgreSQL RLS (mô hình Pool theo AWS SaaS Lens). Lý do: chi phí vận "
         "hành thấp nhất + database engine ép lọc, không phụ thuộc lập trình viên "
         "nhớ điều kiện. Bảng rút gọn 4 mô hình tiêu biểu. (~70 giây)")

# ═══ 12. Defense 5 layer ═══
s = content_slide("Bảo mật nhiều lớp — Defense-in-depth (Hình 2.3)")
diagram(s, A("fig-2.3-defense-5layer.png"))
caption(s, "Hình 2.3 — JWT → @PreAuthorize → tenant interceptor → bộ lọc repository → PostgreSQL RLS (phòng tuyến cuối).", top=6.5)
notes(s, "Nhiều lớp độc lập. RLS là phòng tuyến cuối: quên kiểm tra tầng ứng "
         "dụng, database vẫn ép buộc. Phải thủng cả 5 lớp mới rò dữ liệu chéo "
         "tenant. (~60 giây)")

# ═══ 13. ERD ═══
s = content_slide("Mô hình dữ liệu — ERD KiteClass (Hình 2.6b)")
diagram(s, A("fig-2.6b-erd-kiteclass.png"))
caption(s, "Hình 2.6b — ERD domain giáo dục: ENROLLMENTS phân giải quan hệ nhiều-nhiều STUDENTS ↔ CLASSES.", top=6.5)
notes(s, "Mô hình dữ liệu domain giáo dục. ENROLLMENTS phân giải nhiều-nhiều "
         "học viên–lớp; điểm danh, điểm, thanh toán gắn quanh đăng ký. Mọi bảng "
         "mang tenant_id phục vụ RLS. (~50 giây)")

# ═══ 14. Tenant state ═══
s = content_slide("Vòng đời tenant — máy trạng thái (Hình 2.8)")
diagram(s, A("fig-2.8-tenant-state.png"))
caption(s, "Hình 2.8 — PENDING → TRIAL → ACTIVE → SUSPEND; cấp phát tenant + magic-link kích hoạt.", top=6.5)
notes(s, "Vòng đời tenant: từ chờ duyệt, sang dùng thử khi cấp magic-link, đến "
         "hoạt động chính thức. Sự kiện branding.deploy phát song song dựng "
         "template mặc định. (~50 giây)")

# ═══ 15. AWS ═══
s = content_slide("Triển khai thực tế — AWS Singapore (Hình 4.1a)")
diagram(s, A("fig-4.1a-vpc-topology.png"), 0.5, 1.8, 7.7, 4.7)
bullets(s, [
    ("Vùng ap-southeast-1", 0, True, NAVY),
    ("Public subnet: ALB + 2× EC2 t3.micro", 1, False, None),
    ("Private subnet: RDS PostgreSQL cô lập", 1, False, None),
    ("Phụ trợ: S3, SES, Secrets Manager, ECR, CloudWatch", 1, False, None),
    ("Free Tier 12 tháng → tổng chi phí ~15–30 USD/tháng (≈360–720 nghìn đồng)", 0, True, GREEN),
    ("Chưa chạm ngưỡng PDPL Đ28 (10k) / NĐ53 (1 triệu user); có lộ trình chuyển vùng trong nước", 0, False, GREY),
], left=8.35, top=1.95, width=4.7, base=14)
caption(s, "Hình 4.1a — Topology VPC (10.0.0.0/16).", top=6.55, left=0.55, width=7.6)
notes(s, "Triển khai thực tế AWS Singapore (ap-southeast-1), VPC tách public/"
         "private subnet, RDS trong private subnet. Free Tier giữ tổng chi phí "
         "~15–30 USD/tháng (EC2 vượt 750h tính ~7,38 USD; AI DALL-E 3 ~0,04 USD/"
         "ảnh). Chưa vượt ngưỡng pháp lý, có lộ trình chuyển vùng. (~60 giây)")

# ═══ 16. CI/CD ═══
s = content_slide("CI/CD và giám sát vận hành (Hình 4.2a)")
diagram(s, A("fig-4.2a-ci-build.png"), 0.5, 1.8, 7.6, 4.7)
bullets(s, [
    ("GitHub Actions + OIDC — không lưu access key tĩnh", 0, False, None),
    ("Docker tag bất biến theo SHA; Terraform plan-trước-apply + xác nhận thủ công", 0, False, None),
    ("Giám sát 3 lớp:", 0, True, NAVY),
    ("CloudTrail — audit mọi AWS API call", 1, False, None),
    ("CloudWatch — log JSON + cảnh báo", 1, False, None),
    ("Prometheus + Grafana — metric ứng dụng", 1, False, None),
], left=8.25, top=1.95, width=4.8, base=14)
caption(s, "Hình 4.2a — Pha build: CI verify → OIDC → push ECR.", top=6.55, left=0.55, width=7.5)
notes(s, "CI/CD chuẩn hiện đại: artifact bất biến, OIDC thay key tĩnh, xác nhận "
         "thủ công. Giám sát ba lớp; CloudTrail bật trước khi tạo tài nguyên để "
         "có audit baseline. (~50 giây)")

# ═══ 17. Giao diện thật ═══
s = content_slide("Sản phẩm thực tế — giao diện (Chương 3)")
add_pic_fit(s, A("fig-3.1-tenant-landing.png"), 0.5, 1.85, 4.1, 4.4)
add_pic_fit(s, A("fig-3.3-dashboard.png"), 4.7, 1.85, 4.1, 4.4)
add_pic_fit(s, A("fig-3.5-student-mgmt.png"), 8.9, 1.85, 4.1, 4.4)
caption(s, "Hình 3.1 / 3.3 / 3.5 — Trang chủ thương hiệu riêng theo tenant · Dashboard tổng quan · Quản lý học viên.", top=6.45)
notes(s, "Ba giao diện thực tế: trang chủ thương hiệu riêng (minh chứng phân "
         "giải Tenant→Domain→Landing), dashboard, quản lý học viên. Sản phẩm "
         "chạy thật, không phải mô hình. (~60 giây)")

# ═══ 18. AI free vs paid ═══
s = content_slide("Kết quả AI Branding — gói Miễn phí vs Trả phí")
add_pic_fit(s, A("fig-4.3-landing-free.png"), 0.6, 1.85, 6.0, 4.5)
add_pic_fit(s, A("fig-4.4-landing-paid.png"), 6.8, 1.85, 6.0, 4.5)
caption(s, "Hình 4.3 (Miễn phí — mẫu dựng sẵn, tông xanh dương) vs Hình 4.4 (Trả phí — AI sinh tự động, tông xanh lá).", top=6.5)
notes(s, "Minh chứng giá trị AI Branding: bên trái gói Miễn phí mẫu dựng sẵn; "
         "bên phải gói Trả phí bộ nhận diện sinh tự động qua AI cho môn Hóa, "
         "tông màu khác hẳn. Hai tenant thật, hai thương hiệu riêng. (~70 giây)")

# ═══ 19. KPI ═══
s = content_slide("Kết quả kiểm thử và đánh giá chất lượng (§3.2)")
kpi_cards(s, [
    ("~985", "Test case", "850 unit·120 IT·15-25 E2E"),
    ("≥99,5%", "Tỷ lệ pass", "trên nhánh main"),
    ("≥75%", "Coverage", "module nghiệp vụ"),
    ("4 chiều", "Audit định kỳ", "Quality·Security·Perf·API"),
], top=2.1, h=2.3)
bullets(s, [
    ("Kim tự tháp test pyramid (Cohn): đáy unit rộng → integration (Testcontainers Postgres+RabbitMQ) → đỉnh E2E Playwright", 0, False, None),
    ("Audit định kỳ theo SQA IEEE 730 — findings mỗi kỳ track riêng + schedule fix vòng kế tiếp (continuous quality loop)", 0, True, NAVY),
], top=4.75, height=1.7, base=16)
notes(s, "Tổng ~985 test (850 unit + 120 integration + 15-25 E2E), pass ≥99,5% "
         "trên main, coverage ≥75% line module nghiệp vụ. Integration dùng "
         "Testcontainers Postgres thật (không H2) cho RLS/GUC. Audit định kỳ 4 "
         "chiều theo IEEE 730, vòng cải tiến liên tục. (~80 giây)")

# ═══ 20. DEMO ═══
s = section_slide("Demo trực tiếp",
                  "Khách tham quan → đăng ký onboarding → wizard tạo tenant → cô lập đa-tenant · dự phòng video")
notes(s, "Chuyển sang demo trực tiếp theo kịch bản 6 phase (defense-demo-script.md): "
         "khách tham quan, đăng ký onboarding, wizard tạo tenant, chứng minh cô "
         "lập bằng 2 tài khoản khác tenant, xem audit log. Dự phòng video. "
         "(~30 giây + demo)")

# ═══ 21. Hạn chế ═══
s = content_slide("Hạn chế thừa nhận và hướng phát triển")
bullets(s, [
    ("Hạn chế hiện tại:", 0, True, RED),
    ("Hạ tầng 2× t3.micro bộ nhớ hạn chế; chưa có failover đa vùng (multi-AZ)", 1, False, None),
    ("Chưa lưu trữ dữ liệu trong nước — kích hoạt chuyển vùng khi vượt ngưỡng", 1, False, None),
    ("Quality gate AI dùng bộ lọc provider; chưa tự huấn luyện bộ phân loại brand-fit", 1, False, None),
    ("Chưa có ứng dụng di động native — người dùng dùng web responsive", 1, False, None),
    ("Hướng phát triển sau:", 0, True, GREEN),
    ("Mở đăng ký công khai, nâng cấp hạ tầng, kích hoạt cổng thanh toán MoMo/VNPay", 1, False, None),
    ("Tích hợp hóa đơn điện tử (MISA MeInvoice), kênh Zalo OA, mở rộng K-12 sau khi có legal counsel", 1, False, None),
], top=1.85, base=16)
notes(s, "Thừa nhận hạn chế kèm lộ trình tốt hơn che giấu. Mỗi hạn chế có hướng "
         "phát triển: nâng cấp hạ tầng, chuyển vùng, mở thanh toán, tích hợp "
         "Zalo và hóa đơn điện tử qua đối tác. (~70 giây)")

# ═══ 22. Kết luận (nền section) ═══
s = prs.slides.add_slide(L_BLANK); _idx[0] += 1
set_bg(s, BG_SECTION)
title_box(s, "Kết luận")
bullets(s, [
    ("Đồ án hoàn thành các mục tiêu đặt ra trong phạm vi triển khai hiện tại:", 0, True, NAVY),
    ("Nền tảng KiteHub trên AWS Singapore: 7 microservice + 2 ứng dụng giao diện, cô lập đa-tenant bằng RLS", 1, False, None),
    ("Bốn khác biệt: đa-tenant RLS gốc · AI Branding tự động · tuân thủ pháp luật theo thiết kế · UX Vietnamese-first", 1, False, None),
    ("Đáp ứng PDPL 2023, Luật An ninh mạng 2018, Thông tư 78/2021/TT-BTC; kiểm chứng qua bộ kiểm thử nhiều tầng", 1, False, None),
    ("Mã nguồn công khai trên GitHub để hội đồng đối chiếu trực tiếp", 0, False, None),
    ("Em xin chân thành cảm ơn thầy Nguyễn Đức Dư và quý hội đồng — sẵn sàng trả lời câu hỏi", 0, True, NAVY),
], top=1.95, base=18)
page_num(s, _idx[0])
notes(s, "Tóm kết quả: nền tảng 7 microservice + 2 FE trên AWS Singapore, cô lập "
         "đa-tenant RLS, bốn yếu tố khác biệt, đáp ứng PDPL/An ninh mạng/TT78, "
         "kiểm chứng qua kiểm thử nhiều tầng, mã nguồn công khai. Cảm ơn GVHD và "
         "hội đồng, em sẵn sàng nhận câu hỏi. (~40 giây)")

# --- Áp font tường minh cho MỌI run (title/bullet/table/caption) trước khi lưu ---
def _apply_font(prs, name):
    n = 0
    for slide in prs.slides:
        for shape in slide.shapes:
            if shape.has_text_frame:
                for para in shape.text_frame.paragraphs:
                    for run in para.runs:
                        run.font.name = name
                        n += 1
            if shape.has_table:
                for row in shape.table.rows:
                    for cell in row.cells:
                        for para in cell.text_frame.paragraphs:
                            for run in para.runs:
                                run.font.name = name
                                n += 1
    return n


_n = _apply_font(prs, FONT_NAME)
print(f"Applied font '{FONT_NAME}' to {_n} runs")

prs.save(OUT)
print("OK saved:", OUT)
print("Tổng slide:", len(prs.slides._sldIdLst))
