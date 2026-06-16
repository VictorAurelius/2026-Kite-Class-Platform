# -*- coding: utf-8 -*-
"""
build_defense_pptx.py (v2) — Dựng slide bảo vệ khóa luận bám THESIS BẢN FINAL
(chapter-*.md + thesis-v1.docx) trên theme template UTC, NHÚNG ẢNH THẬT.

Nguồn:
  - Cấu trúc + số liệu: chapter-1..4 (final), audit scores canonical.
  - Ảnh: defense/assets/*.png (trích từ thesis-v1.docx — 31 figure final).
  - Theme: Presentation - File baocaodatn 16-9.pptx (layout placeholder + footer
    + số trang được áp dụng đúng).

Chạy:  ../.venv/bin/python defense/build_defense_pptx.py   (từ thư mục 08-thesis)
Output: defense/KiteHub-baove-khoaluan-20slide.pptx
"""
import os
import struct
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

HERE = os.path.dirname(os.path.abspath(__file__))
THESIS = os.path.dirname(HERE)
TEMPLATE = os.path.join(THESIS, "Presentation - File baocaodatn 16-9.pptx")
ASSETS = os.path.join(HERE, "assets")
OUT = os.path.join(HERE, "KiteHub-baove-khoaluan-20slide.pptx")

NAVY = RGBColor(0x1F, 0x49, 0x7D)
BLUE = RGBColor(0x4F, 0x81, 0xBD)
GREEN = RGBColor(0x1E, 0x7E, 0x34)
RED = RGBColor(0xB0, 0x30, 0x30)
GREY = RGBColor(0x5A, 0x5A, 0x5A)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
LIGHT = RGBColor(0xEE, 0xF3, 0xFA)
DARK = RGBColor(0x22, 0x22, 0x22)

FOOTER = "Khóa luận tốt nghiệp · Nguyễn Văn Kiệt · CNTT1-K63 · UTC 2026"

prs = Presentation(TEMPLATE)

sldIdLst = prs.slides._sldIdLst
for sldId in list(sldIdLst):
    prs.part.drop_rel(sldId.get(qn('r:id')))
    sldIdLst.remove(sldId)

L_TITLE = prs.slide_layouts[0]
L_CONTENT = prs.slide_layouts[1]
L_SECTION = prs.slide_layouts[2]
L_TWO = prs.slide_layouts[3]
L_TITLEONLY = prs.slide_layouts[5]


def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(26)
    if head[:8] == b'\x89PNG\r\n\x1a\n':
        return struct.unpack('>II', head[16:24])
    return 1280, 720


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


def footer_and_number(slide, idx):
    tb = slide.shapes.add_textbox(Inches(0.4), Inches(7.06), Inches(10.5), Inches(0.35))
    r = tb.text_frame.paragraphs[0].add_run()
    r.text = FOOTER
    r.font.size = Pt(9); r.font.color.rgb = RGBColor(0x9A, 0x9A, 0x9A)
    nb = slide.shapes.add_textbox(Inches(12.4), Inches(7.06), Inches(0.7), Inches(0.35))
    rn = nb.text_frame.paragraphs[0].add_run()
    rn.text = str(idx)
    rn.font.size = Pt(10); rn.font.color.rgb = NAVY; rn.font.bold = True
    nb.text_frame.paragraphs[0].alignment = PP_ALIGN.RIGHT


def accent_bar(slide):
    bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.55), Inches(1.32),
                                 Inches(3.4), Pt(3))
    bar.fill.solid(); bar.fill.fore_color.rgb = BLUE
    bar.line.fill.background(); bar.shadow.inherit = False


_idx = [0]


def new_slide(layout, title=None, tsize=28, bar=True):
    s = prs.slides.add_slide(layout)
    _idx[0] += 1
    if title is not None and s.shapes.title is not None:
        s.shapes.title.text = title
        for p in s.shapes.title.text_frame.paragraphs:
            for r in p.runs:
                r.font.size = Pt(tsize); r.font.bold = True; r.font.color.rgb = NAVY
    footer_and_number(s, _idx[0])
    if bar and layout not in (L_TITLE, L_SECTION):
        accent_bar(s)
    return s


def notes(slide, text):
    slide.notes_slide.notes_text_frame.text = text


def bullets(slide, items, left=0.6, top=1.55, width=12.1, height=5.2, base=18):
    tb = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(height))
    tf = tb.text_frame; tf.word_wrap = True
    first = True
    for it in items:
        text, level, bold, color = (list(it) + [None, None])[:4]
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.level = level; p.space_after = Pt(5)
        run = p.add_run()
        run.text = ("• " if level == 0 else "– ") + text
        run.font.size = Pt(base - level * 2)
        run.font.bold = bool(bold)
        run.font.color.rgb = color if color else DARK
    return tb


def table(slide, rows, top=1.6, left=0.55, width=12.2, col_widths=None, fs=12):
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
            cell.fill.fore_color.rgb = NAVY if r == 0 else (LIGHT if r % 2 == 0 else WHITE)
    return t


def caption(slide, text, top=6.55, size=11, left=0.6, width=12.1):
    tb = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(0.45))
    p = tb.text_frame.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = text
    r.font.size = Pt(size); r.font.italic = True; r.font.color.rgb = GREY


A = lambda n: os.path.join(ASSETS, n)

# 1. Bìa
s = prs.slides.add_slide(L_TITLE); _idx[0] += 1
s.shapes.title.text = "Xây dựng hệ thống SaaS cung cấp dịch vụ đào tạo"
for p in s.shapes.title.text_frame.paragraphs:
    for r in p.runs:
        r.font.size = Pt(32); r.font.bold = True; r.font.color.rgb = NAVY
s.placeholders[1].text = (
    "Nền tảng KiteHub & KiteClass — kiến trúc đa-tenant tích hợp AI\n\n"
    "Sinh viên: Nguyễn Văn Kiệt — MSSV 221230890 — Lớp CNTT1-K63\n"
    "Giảng viên hướng dẫn: TS. Nguyễn Đức Dư\n"
    "Bộ môn Công nghệ phần mềm — Khoa Công nghệ thông tin\n"
    "Trường Đại học Giao thông Vận tải — Hà Nội, 2026")
for p in s.placeholders[1].text_frame.paragraphs:
    for r in p.runs:
        r.font.size = Pt(16); r.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
notes(s, "Chào hội đồng. Em là Nguyễn Văn Kiệt, lớp CNTT1-K63, dưới hướng dẫn "
         "của thầy Nguyễn Đức Dư. Em xin trình bày khóa luận: Xây dựng hệ thống "
         "SaaS cung cấp dịch vụ đào tạo — nền tảng KiteHub. (~30 giây)")

# 2. Nội dung
s = new_slide(L_CONTENT, "Nội dung trình bày")
bullets(s, [
    ("Phần 1 — Tổng quan: bối cảnh, khảo sát thị trường, mục tiêu (Chương 1)", 0, True, NAVY),
    ("Phần 2 — Kỹ thuật AI, khung pháp lý, phương pháp luận (Chương 1)", 0, True, NAVY),
    ("Phần 3 — Phân tích và thiết kế kiến trúc đa-tenant (Chương 2)", 0, True, NAVY),
    ("Phần 4 — Cài đặt, triển khai AWS và kết quả (Chương 3 & 4)", 0, True, NAVY),
    ("Phần 5 — Demo trực tiếp, hạn chế và kết luận", 0, True, NAVY),
], top=1.7, base=22)
notes(s, "Bài trình bày bám bốn chương: tổng quan, thiết kế kiến trúc, cài đặt "
         "và triển khai, đánh giá kết quả; khép lại bằng demo và kết luận. (~20 giây)")

# 3. Bối cảnh
s = new_slide(L_CONTENT, "Bối cảnh và vấn đề (§1.1)")
bullets(s, [
    ("Thông tư 29/2024/TT-BGDĐT chính thức hóa hoạt động dạy thêm có thu phí", 0, False, None),
    ("Hơn 50.000 trung tâm dạy thêm tư nhân đang hoạt động tại Việt Nam", 0, False, None),
    ("Phần lớn trung tâm vừa và nhỏ vẫn quản lý bằng Excel và nhóm Zalo", 0, False, None),
    ("Ba quan sát thúc đẩy đề tài:", 0, True, NAVY),
    ("Khoảng trống thị trường: trung tâm 100–2000 học viên cần phần mềm 0,5–1,5 triệu đồng/tháng, đa-tenant gốc", 1, False, None),
    ("Mốc pháp lý: Luật Bảo vệ dữ liệu cá nhân 2023 hiệu lực 07/2026 — tích hợp tuân thủ ngay từ thiết kế", 1, False, None),
    ("Công nghệ AI trưởng thành: API thương mại cho phép tự động hóa nhận diện thương hiệu chi phí thấp", 1, False, None),
], top=1.55, base=16)
caption(s, "Nguồn: Magenest EdTech 2024; 6Wresearch 2024–2030; VECITA 2024.", top=6.55)
notes(s, "Bối cảnh: thị trường lớn, pháp luật hợp pháp hóa dạy thêm, đa số "
         "trung tâm nhỏ quản lý thủ công. Ba quan sát hội tụ: nhu cầu, mốc pháp "
         "lý cứng, AI vừa đủ chín. (~60 giây)")

# 4. Khảo sát (ảnh grid)
s = new_slide(L_TITLEONLY, "Khảo sát hệ thống tương tự (§1.3)")
add_pic_fit(s, A("fig-1.1-beeclass.png"), 0.6, 1.55, 5.9, 2.45)
add_pic_fit(s, A("fig-1.2-mona.png"), 6.8, 1.55, 5.9, 2.45)
add_pic_fit(s, A("fig-1.3-easyedu.png"), 0.6, 4.15, 5.9, 2.3)
add_pic_fit(s, A("fig-1.4-dotb.png"), 6.8, 4.15, 5.9, 2.3)
caption(s, "Hình 1.1–1.4: BeeClass, Mona eLMS, Easy Edu, DotB — đều đơn-tenant, không AI. "
           "KiteHub khác biệt: đa-tenant RLS gốc + AI Branding.", top=6.62)
notes(s, "Khảo sát 5 hệ thống tham khảo (thêm MISA AMIS). Hầu hết đơn-tenant, "
         "không AI. KiteHub khai thác khoảng trống đa-tenant gốc + AI Branding "
         "phân khúc giá thấp. (~70 giây)")

# 5. Mục tiêu
s = new_slide(L_CONTENT, "Mục tiêu và phạm vi đề tài (§1.7)")
bullets(s, [
    ("Bốn nhóm mục tiêu:", 0, True, NAVY),
    ("Chức năng: onboarding wizard, AI Branding, vòng đời tenant, tuân thủ tích hợp sẵn", 1, False, None),
    ("Phi chức năng: p95 ≤ 500ms (API đọc), cô lập cấp database, OWASP Top 10, ≥100 tenant/instance", 1, False, None),
    ("Pháp lý: PDPL 2023 + Luật An ninh mạng 2018 + Thông tư 78/2021/TT-BTC", 1, False, None),
    ("Phương pháp luận: phát triển hướng chất lượng — kiểm thử trước, vòng lặp ngắn 1–3 ngày", 1, False, None),
    ("Phạm vi thực hiện: wizard tự phục vụ, AI Branding, cô lập RLS, tuân thủ pháp lý, triển khai AWS", 0, True, NAVY),
    ("Phát triển sau: thanh toán đa cổng, hóa đơn điện tử, ứng dụng di động, mở rộng khối K-12", 0, True, GREY),
], top=1.55, base=16)
notes(s, "Bốn nhóm mục tiêu: chức năng, phi chức năng đo được, pháp lý, phương "
         "pháp luận. Phạm vi tập trung mức sẵn sàng cho tenant thực tế. (~60 giây)")

# 6. AI Branding (ảnh wizard)
s = new_slide(L_TITLEONLY, "Đóng góp 1 — Kỹ thuật AI Branding (§1.4)")
bullets(s, [
    ("Sinh logo + ảnh bìa + banner tự động khi tenant đăng ký (Stable Diffusion XL qua Replicate)", 0, False, None),
    ("Pipeline bất đồng bộ: Form → Gateway → Orchestrator → RabbitMQ → Worker → AI → Quality Gate → S3", 0, False, None),
    ("Quality Gate lọc NSFW + brand-fit; dự phòng Hugging Face; retry tối đa 3 lần", 0, False, None),
    ("Chi phí ~0,0036 USD/3 ảnh; thời gian 30–60 giây", 0, True, GREEN),
], left=0.55, top=1.5, width=6.7, base=15)
add_pic_fit(s, A("fig-3.4-ai-wizard.png"), 7.3, 1.5, 5.6, 5.0)
caption(s, "Hình 3.4 — Trình hướng dẫn AI Branding cho chủ trung tâm.", top=6.6, left=7.0, width=6.0)
notes(s, "Đóng góp thứ nhất: tự động hóa nhận diện thương hiệu. Dùng API "
         "thương mại thay vì tự host GPU. Pipeline bất đồng bộ không chặn giao "
         "diện, có quality gate và dự phòng provider. Bên phải là wizard thực "
         "tế. (~80 giây)")

# 7. Pháp luật
s = new_slide(L_CONTENT, "Tuân thủ pháp luật Việt Nam (§1.5)")
table(s, [
    ["Văn bản", "Yêu cầu chính", "Cách hiện thực"],
    ["PDPL 2023", "Consent cụ thể; quyền truy cập/xóa; DPO khi >10k chủ thể; báo vi phạm 72h", "consent_record; quy trình DSAR; audit log bất biến V60"],
    ["Luật An ninh mạng 2018\n+ Nghị định 53/2022", "Lưu trữ dữ liệu tại VN khi vượt 1 triệu người dùng", "Lộ trình chuyển AWS Hà Nội Local Zone / Viettel / VNG"],
    ["Thông tư 78/2021/TT-BTC", "Hóa đơn điện tử, kết nối cơ quan thuế", "Tích hợp đối tác MISA MeInvoice"],
], top=1.7, fs=12, col_widths=[3.1, 4.7, 4.4])
caption(s, "Cách tiếp cận: tuân thủ ngay từ thiết kế (compliance-by-design).", top=6.2)
notes(s, "Ba trụ cột pháp luật tích hợp từ thiết kế. PDPL mốc cứng 07/2026. "
         "Luật An ninh mạng có ngưỡng, chưa chạm, có lộ trình. Thông tư 78 dùng "
         "đối tác. (~60 giây)")

# 8. Phương pháp luận (ảnh pyramid)
s = new_slide(L_TITLEONLY, "Phương pháp luận hướng chất lượng (§1.6)")
bullets(s, [
    ("Vòng lặp ngắn 1–3 ngày, phạm vi + tiêu chí nghiệm thu rõ ràng", 0, False, None),
    ("Kiểm thử trước (test-first), coverage ≥ 70%", 0, False, None),
    ("Đánh giá theo kỳ (audit): bảo mật / hiệu năng / nghiệp vụ / giao diện / vận hành", 0, False, None),
    ("Chuẩn hóa quy tắc: mỗi sai sót → quy tắc + kiểm tra tự động", 0, False, None),
    ("Cơ sở: Deming PDCA, Beck TDD, Poppendieck Lean, IEEE 730", 0, True, GREY),
], left=0.55, top=1.5, width=6.9, base=16)
add_pic_fit(s, A("fig-3.6-test-pyramid.png"), 7.5, 1.6, 5.4, 4.6)
caption(s, "Hình 3.6 — Kim tự tháp kiểm thử áp dụng cho KiteHub.", top=6.5, left=7.2, width=5.6)
notes(s, "Bốn trụ cột có cơ sở lý thuyết. Trụ cột bốn: mỗi sai sót thành quy "
         "tắc có kiểm tra. Kim tự tháp kiểm thử minh họa phân bố ba tầng test. "
         "(~50 giây)")

# 9. C4 context (ảnh 2.1)
s = new_slide(L_TITLEONLY, "Kiến trúc tổng thể — C4 Level 1 (Hình 2.1)")
add_pic_fit(s, A("fig-2.1-c4-context.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.1 — Sơ đồ ngữ cảnh: KiteHub (control-plane) + KiteClass (data-plane) chia sẻ PostgreSQL RLS.", top=6.5)
notes(s, "Kiến trúc tổng thể chia hai mặt phẳng: KiteHub control-plane quản lý "
         "vòng đời tenant; KiteClass data-plane phục vụ giáo dục. Chia sẻ một "
         "PostgreSQL cô lập bằng RLS. (~70 giây)")

# 10. C4 container (ảnh 2.2)
s = new_slide(L_TITLEONLY, "Phân rã container — C4 Level 2 (Hình 2.2)")
add_pic_fit(s, A("fig-2.2-c4-container.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.2 — KiteHub: 6 microservice; KiteClass: modular monolith.", top=6.5)
notes(s, "KiteHub 6 microservice vì vòng đời khác nhau; KiteClass modular "
         "monolith vì domain giáo dục gắn kết chặt. (~60 giây)")

# 11. multi-tenant RLS
s = new_slide(L_CONTENT, "Đóng góp 2 — Cô lập đa-tenant bằng RLS")
table(s, [
    ["Mô hình", "Chi phí", "Cô lập", "Quy mô", "Quyết định"],
    ["Instance mỗi tenant", "Rất cao", "Tuyệt đối", "≤10", "Loại"],
    ["Database mỗi tenant", "Cao", "Mạnh", "10–100", "Loại"],
    ["Schema mỗi tenant", "Trung bình", "Khá", "100–1000", "Cân nhắc sau"],
    ["Row-Level Security", "Thấp", "DB engine ép buộc", "≥1000", "Áp dụng"],
], top=1.65, fs=13, col_widths=[2.9, 1.9, 2.7, 2.3, 2.4])
bullets(s, [
    ("RLS policy + biến phiên app.current_tenant_id (HikariCP) — Postgres ép lọc ở MỖI truy vấn", 0, True, NAVY),
    ("Quên điều kiện lọc vẫn không rò chéo tenant — kiểm chứng tại Salesforce, Shopify, HubSpot", 0, False, None),
], top=4.55, height=1.9, base=15)
notes(s, "So sánh bốn mô hình. Chọn RLS vì chi phí thấp nhất và database engine "
         "ép buộc. Đóng góp thứ hai. (~70 giây)")

# 12. Defense 5 layer (ảnh 2.3)
s = new_slide(L_TITLEONLY, "Bảo mật nhiều lớp — Defense-in-depth (Hình 2.3)")
add_pic_fit(s, A("fig-2.3-defense-5layer.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.3 — JWT → @PreAuthorize → tenant interceptor → bộ lọc repository → PostgreSQL RLS (phòng tuyến cuối).", top=6.5)
notes(s, "Nhiều lớp độc lập. RLS là phòng tuyến cuối: quên kiểm tra tầng ứng "
         "dụng, database vẫn ép buộc. Phải thủng cả 5 lớp mới rò. (~60 giây)")

# 13. ERD (ảnh 2.6b)
s = new_slide(L_TITLEONLY, "Mô hình dữ liệu — ERD KiteClass (Hình 2.6b)")
add_pic_fit(s, A("fig-2.6b-erd-kiteclass.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.6b — ERD domain giáo dục: ENROLLMENTS phân giải quan hệ nhiều-nhiều STUDENTS ↔ CLASSES.", top=6.5)
notes(s, "Mô hình dữ liệu domain giáo dục. ENROLLMENTS phân giải nhiều-nhiều "
         "học viên–lớp; điểm danh, điểm, thanh toán gắn quanh đăng ký. Mọi bảng "
         "mang tenant_id. (~50 giây)")

# 14. Tenant state (ảnh 2.8)
s = new_slide(L_TITLEONLY, "Vòng đời tenant — máy trạng thái (Hình 2.8)")
add_pic_fit(s, A("fig-2.8-tenant-state.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.8 — PENDING → TRIAL → ACTIVE → SUSPEND; cấp phát tenant + magic-link kích hoạt.", top=6.5)
notes(s, "Vòng đời tenant: từ chờ duyệt, sang dùng thử khi cấp magic-link, đến "
         "hoạt động chính thức. Sự kiện branding.deploy phát song song dựng "
         "template. (~50 giây)")

# 15. AWS (ảnh 4.1a)
s = new_slide(L_TITLEONLY, "Triển khai thực tế — AWS Singapore (Hình 4.1a)")
add_pic_fit(s, A("fig-4.1a-vpc-topology.png"), 0.6, 1.5, 7.6, 5.0)
bullets(s, [
    ("Vùng ap-southeast-1", 0, True, NAVY),
    ("Public subnet: ALB + 2× EC2 t3.micro", 1, False, None),
    ("Private subnet: RDS PostgreSQL cô lập", 1, False, None),
    ("Phụ trợ: S3, SES, Secrets Manager, ECR, CloudWatch", 1, False, None),
    ("Free Tier 12 tháng → chi phí hạ tầng ~0", 0, True, GREEN),
    ("Chưa chạm ngưỡng PDPL Đ28 / NĐ53; có lộ trình chuyển vùng trong nước", 0, False, GREY),
], left=8.4, top=1.6, width=4.6, base=14)
caption(s, "Hình 4.1a — Topology VPC (10.0.0.0/16).", top=6.6, left=0.6, width=7.6)
notes(s, "Triển khai thực tế AWS Singapore, VPC tách public/private subnet, RDS "
         "trong private subnet. Free Tier cho chi phí ~0. Chưa vượt ngưỡng pháp "
         "lý, có lộ trình chuyển vùng. (~60 giây)")

# 16. CI/CD (ảnh 4.2a)
s = new_slide(L_TITLEONLY, "CI/CD và giám sát vận hành (Hình 4.2a)")
add_pic_fit(s, A("fig-4.2a-ci-build.png"), 0.6, 1.5, 7.5, 5.0)
bullets(s, [
    ("GitHub Actions + OIDC — không lưu access key tĩnh", 0, False, None),
    ("Docker tag bất biến theo SHA; Terraform plan-trước-apply + xác nhận thủ công", 0, False, None),
    ("Giám sát 3 lớp:", 0, True, NAVY),
    ("CloudTrail — audit mọi AWS API call", 1, False, None),
    ("CloudWatch — log JSON + cảnh báo", 1, False, None),
    ("Prometheus + Grafana — metric ứng dụng", 1, False, None),
], left=8.3, top=1.6, width=4.7, base=14)
caption(s, "Hình 4.2a — Pha build: CI verify → OIDC → push ECR.", top=6.6, left=0.6, width=7.5)
notes(s, "CI/CD chuẩn hiện đại: artifact bất biến, OIDC thay key tĩnh, xác nhận "
         "thủ công. Giám sát ba lớp; CloudTrail bật trước khi tạo tài nguyên. "
         "(~50 giây)")

# 17. Giao diện thật (3 ảnh)
s = new_slide(L_TITLEONLY, "Sản phẩm thực tế — giao diện (Chương 3)")
add_pic_fit(s, A("fig-3.1-tenant-landing.png"), 0.5, 1.5, 4.1, 4.6)
add_pic_fit(s, A("fig-3.3-dashboard.png"), 4.7, 1.5, 4.1, 4.6)
add_pic_fit(s, A("fig-3.5-student-mgmt.png"), 8.9, 1.5, 4.1, 4.6)
caption(s, "Hình 3.1 / 3.3 / 3.5 — Trang chủ thương hiệu riêng theo tenant · Dashboard tổng quan · Quản lý học viên.", top=6.4)
notes(s, "Ba giao diện thực tế: trang chủ thương hiệu riêng (minh chứng phân "
         "giải Tenant→Domain→Landing), dashboard, quản lý học viên. Sản phẩm "
         "chạy thật. (~60 giây)")

# 18. AI Branding free vs paid (2 ảnh)
s = new_slide(L_TITLEONLY, "Kết quả AI Branding — gói Miễn phí vs Trả phí")
add_pic_fit(s, A("fig-4.3-landing-free.png"), 0.6, 1.55, 6.0, 4.7)
add_pic_fit(s, A("fig-4.4-landing-paid.png"), 6.8, 1.55, 6.0, 4.7)
caption(s, "Hình 4.3 (Miễn phí — mẫu dựng sẵn, tông xanh dương) vs Hình 4.4 (Trả phí — AI sinh tự động, tông xanh lá).", top=6.45)
notes(s, "Minh chứng giá trị AI Branding: bên trái gói Miễn phí mẫu dựng sẵn; "
         "bên phải gói Trả phí bộ nhận diện sinh tự động qua AI cho môn Hóa, "
         "tông màu khác hẳn. Hai tenant thật, hai thương hiệu riêng. (~70 giây)")

# 19. KPI
s = new_slide(L_CONTENT, "Kết quả đánh giá — các chỉ số chính (Chương 4)")
table(s, [
    ["Hạng mục", "Baseline", "Hiện tại", "Tăng", "Động lực cải tiến"],
    ["Hiệu năng", "81/100 B", "86/100 B+", "+5", "RLS NULL force-fail, reset GUC, phân trang con trỏ"],
    ["Bảo mật", "76/100 C+", "93/100 A", "+17", "Audit OWASP, audit_logs bất biến V60, chặn bypass admin"],
    ["Chất lượng", "78/110 C+", "90/110 B+", "+12", "Coverage test, giám sát, sẵn sàng vận hành"],
    ["Tải (RPS)", "—", "≥100k", "—", "Kiểm thử tải Locust — dư địa cho cohort mời thử"],
], top=1.7, fs=12.5, col_widths=[1.9, 1.7, 1.7, 1.0, 5.9])
caption(s, "Mỗi điểm số gắn audit report có evidence block trong Chương 4 — không phải tự nhận định.", top=6.0)
notes(s, "Bốn chỉ số: hiệu năng 86, bảo mật 93, chất lượng 90/110, đều vượt "
         "ngưỡng đạt. Mỗi điểm số có audit report evidence block; trajectory "
         "cho thấy cải tiến đo được. (~80 giây)")

# 20. DEMO
s = prs.slides.add_slide(L_SECTION); _idx[0] += 1
s.shapes.title.text = "Demo trực tiếp"
for p in s.shapes.title.text_frame.paragraphs:
    for r in p.runs:
        r.font.size = Pt(36); r.font.bold = True; r.font.color.rgb = NAVY
try:
    s.placeholders[1].text = ("Khách tham quan → đăng ký onboarding → wizard tạo "
                              "tenant → chứng minh cô lập đa-tenant\nDự phòng: video backup")
    for p in s.placeholders[1].text_frame.paragraphs:
        for r in p.runs:
            r.font.size = Pt(16)
except Exception:
    pass
footer_and_number(s, _idx[0])
notes(s, "Chuyển sang demo trực tiếp theo kịch bản 6 phase (defense-demo-script.md): "
         "khách tham quan, đăng ký onboarding, wizard tạo tenant, chứng minh cô "
         "lập bằng 2 tài khoản khác tenant, xem audit log. Dự phòng video. (~30 giây + demo)")

# 21. Hạn chế
s = new_slide(L_CONTENT, "Hạn chế thừa nhận và hướng phát triển")
bullets(s, [
    ("Hạn chế hiện tại:", 0, True, RED),
    ("Hạ tầng 2× t3.micro bộ nhớ hạn chế; chưa có failover đa vùng (multi-AZ)", 1, False, None),
    ("Chưa lưu trữ dữ liệu trong nước — kích hoạt chuyển vùng khi vượt ngưỡng", 1, False, None),
    ("Quality gate AI dùng bộ lọc provider; chưa tự huấn luyện bộ phân loại brand-fit", 1, False, None),
    ("Chưa có ứng dụng di động native — người dùng dùng web responsive", 1, False, None),
    ("Hướng phát triển sau:", 0, True, GREEN),
    ("Mở đăng ký công khai, nâng cấp hạ tầng, kích hoạt cổng thanh toán MoMo/VNPay", 1, False, None),
    ("Tích hợp hóa đơn điện tử (MISA MeInvoice), kênh Zalo OA, mở rộng K-12 sau khi có legal counsel", 1, False, None),
], top=1.55, base=16)
notes(s, "Thừa nhận hạn chế kèm lộ trình tốt hơn che giấu. Mỗi hạn chế có "
         "hướng phát triển: nâng cấp hạ tầng, chuyển vùng, mở thanh toán, tích "
         "hợp Zalo và hóa đơn điện tử qua đối tác. (~70 giây)")

# 22. Kết luận
s = new_slide(L_CONTENT, "Kết luận")
bullets(s, [
    ("Ba đóng góp chính của đề tài:", 0, True, NAVY),
    ("Kỹ thuật AI Branding tự động — sinh bộ nhận diện thương hiệu chi phí thấp, bất đồng bộ", 1, False, None),
    ("Kiến trúc đa-tenant cô lập bằng PostgreSQL RLS — bảo mật nhiều lớp, mở rộng tốt", 1, False, None),
    ("Phương pháp luận phát triển hướng chất lượng — audit có evidence, chuẩn hóa quy tắc", 1, False, None),
    ("Sản phẩm đã triển khai thực tế trên AWS, đạt các ngưỡng hiệu năng/bảo mật/chất lượng", 0, False, None),
    ("Em xin chân thành cảm ơn thầy Nguyễn Đức Dư và quý hội đồng — sẵn sàng trả lời câu hỏi", 0, True, NAVY),
], top=1.7, base=18)
notes(s, "Tóm ba đóng góp: AI Branding, đa-tenant RLS, phương pháp luận hướng "
         "chất lượng. Sản phẩm triển khai thực tế, đạt ngưỡng đánh giá. Cảm ơn "
         "GVHD và hội đồng. (~40 giây)")

# Phụ lục divider
s = prs.slides.add_slide(L_SECTION); _idx[0] += 1
s.shapes.title.text = "Phụ lục — slide dự phòng hỏi đáp"
for p in s.shapes.title.text_frame.paragraphs:
    for r in p.runs:
        r.font.size = Pt(28); r.font.bold = True; r.font.color.rgb = NAVY
footer_and_number(s, _idx[0])
notes(s, "Slide phụ lục bật khi hội đồng hỏi sâu.")

# A1 tenant routing (ảnh 2.4c)
s = new_slide(L_TITLEONLY, "Phụ lục A1 — Định tuyến Tenant → Domain → Landing (Hình 2.4c)", tsize=24)
add_pic_fit(s, A("fig-2.4c-tenant-routing.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.4c — Trình duyệt → Cloudflare DNS → gateway phân giải tenant theo Host → lớp dữ liệu RLS.", top=6.5)
notes(s, "Chuỗi định tuyến tenant theo Host: gateway ánh xạ tên miền thành "
         "định danh tenant rồi truyền ngữ cảnh xuống lớp dữ liệu cô lập RLS.")

# A2 ERD kitehub (ảnh 2.6a)
s = new_slide(L_TITLEONLY, "Phụ lục A2 — ERD KiteHub control-plane (Hình 2.6a)", tsize=24)
add_pic_fit(s, A("fig-2.6a-erd-kitehub.png"), 0.6, 1.5, 12.1, 4.9)
caption(s, "Hình 2.6a — INSTANCES là bảng gốc (PK id UUID), 1-1 với SUBSCRIPTIONS, 1-N tới các bảng còn lại.", top=6.5)
notes(s, "Mô hình control-plane: INSTANCES quản lý vòng đời tenant, liên kết "
         "subscription và các bảng cấu hình.")

# A3 PDPL
s = new_slide(L_CONTENT, "Phụ lục A3 — PDPL 2023: điều luật → tính năng")
table(s, [
    ["Điều", "Yêu cầu", "Cách hiện thực"],
    ["Điều 9", "Quyền truy cập dữ liệu của chủ thể", "Endpoint DSAR + dashboard phụ huynh"],
    ["Điều 11", "Audit log bất biến, chống sửa", "Migration V60 — admin_audit_logs không cho UPDATE/DELETE"],
    ["Điều 16", "Consent cụ thể theo từng mục đích", "Bảng consent_record + luồng thu hồi"],
    ["Điều 28", "DPO khi >10k chủ thể", "Hiện <1000 chủ thể — chưa kích hoạt; có lộ trình"],
    ["Điều 30", "Báo vi phạm trong 72h", "Incident response + cảnh báo CloudWatch SNS"],
], top=1.7, fs=12.5, col_widths=[1.4, 4.6, 6.2])
notes(s, "Năm điều luật mapping 1-1 sang tính năng. Điều 11 audit log bất biến "
         "giải bằng bảng không cho sửa/xóa ở cấp database.")

# A4 cost
s = new_slide(L_CONTENT, "Phụ lục A4 — Phân tích chi phí")
table(s, [
    ["Hạng mục", "Chi phí hiện tại", "Ghi chú"],
    ["AWS Free Tier (12 tháng)", "0 USD/tháng", "2× t3.micro + RDS db.t3.micro + 5GB S3"],
    ["AWS SES email", "0 USD/tháng", "62k email/tháng miễn phí khi gửi từ EC2"],
    ["AI Branding (Replicate)", "~0,0036 USD/tenant", "3 ảnh × 0,0012 USD"],
    ["Cloudflare DNS + CDN", "0 USD/tháng", "Gói Free đủ dùng"],
    ["Tên miền kitehub.me", "~9 USD/năm", "Gia hạn hàng năm"],
], top=1.7, fs=13, col_widths=[3.4, 3.0, 5.8])
notes(s, "Chi phí hạ tầng gần như 0 nhờ Free Tier; chi phí chính AI Branding "
         "~0,19 USD mỗi lần onboard tenant.")

prs.save(OUT)
print("OK saved:", OUT)
print("Tổng slide:", len(prs.slides._sldIdLst))
