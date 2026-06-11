package com.kiteclass.core.dev.seeder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotently seeds the AI Branding demo dataset when the {@code dev} profile
 * is active: 1 DEPLOYED {@link FrontendInstance}, 3 {@link BrandingResource}
 * rows (one per {@link ResourceCategory}), 1 {@link QualityReport}, and
 * 1 outbox event. The dataset matches the wave plan §7.4 and lets the
 * frontend wizard demo run end-to-end without hitting Ollama.
 *
 * <p>Skips silently if the dev tenant slug already has an instance — safe to
 * re-run on every boot.
 *
 * <p>Tracking: GAP-235 Sub-PR F.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class BrandingDataSeeder {

    static final UUID DEV_TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final String DEV_TENANT_SLUG = "thanglong";
    static final String DEV_TENANT_REF = "dev-tenant-thanglong";
    static final String DEV_FRONTEND_URL = "https://thanglong.kite.local";

    // GAP-805 Bucket A — demo tenant "Sky Education" branding seed.
    // Instance UUID matches the kitehub gateway `instances` row for subdomain
    // `sky-education` (id e8ff87e1…) so browser→gateway→core resolves the demo data
    // (shared-DB + RLS canonical model per ADR-023). Previously a made-up a5e00000…
    // that the gateway never resolved — see 2026-05-29 demo-trio walk align-seed-to-gateway fix.
    // Slug + display name mirror scripts/seed-thesis-demo-tenants.sh tenant_a (Sky Education),
    // proving live UI theme customisation on the multi-tenant demo.
    static final UUID SKY_TENANT_ID = UUID.fromString("e8ff87e1-69fc-4842-a263-7385c68b4ffb");
    static final String SKY_TENANT_SLUG = "sky-education";
    static final String SKY_TENANT_REF = "dev-tenant-sky-education";
    static final String SKY_FRONTEND_URL = "https://sky-education.kite.local";
    static final String SKY_DISPLAY_NAME = "Trung tâm Anh ngữ Sky Education";
    static final String SKY_TAGLINE = "Chắp cánh tương lai Anh ngữ";
    // Warm education palette (orange/amber) — deliberately distinct from the default
    // shadcn blue so the live theme swap is visually obvious during the demo.
    static final String SKY_PRIMARY_COLOR = "#E8590C";   // cam đậm — primary actions
    static final String SKY_SECONDARY_COLOR = "#1B4965";  // xanh navy — headers/footer
    static final String SKY_ACCENT_COLOR = "#FFB703";    // vàng hổ phách — highlights

    // ── Wave landing-100 Bucket G — demo-trio Branding settings seed (GAP-805) ──
    // Three independent instructors proving plan-tier + branding variety per thesis §4.1-4.2
    // (Hình 4.3 / 4.4). Each seeds a `Branding` settings entity (color + tagline + theme JSON)
    // + FrontendInstance + LandingPage hero — directly, NOT via the AI wizard (FULL_AI landing
    // persist breaks per GAP-1021). Banner assets are HTML-composed AI scenes (GAP-810) living in
    // kiteclass-frontend/public/demo-banners/; the seed stores only the URL string.
    // Khánh (§4.1 walkthrough tenant) reuses the existing Sky instance; Hà + Nhì are fresh tenants.

    // Cô Nguyễn Thị Hà — gói Miễn phí, Toán tiểu học, tông xanh dương (template, no AI).
    static final UUID HA_TENANT_ID = UUID.fromString("a1100000-0000-4000-a000-000000000001");
    static final String HA_TENANT_SLUG = "co-ha-toan";
    static final String HA_TENANT_REF = "dev-tenant-co-ha-toan";
    static final String HA_FRONTEND_URL = "https://co-ha-toan.kite.local";
    static final String HA_DISPLAY_NAME = "Lớp Toán cô Nguyễn Thị Hà";
    static final String HA_TAGLINE = "Toán tiểu học vững nền tảng";
    static final String HA_PRIMARY_COLOR = "#2563EB";    // xanh dương
    static final String HA_SECONDARY_COLOR = "#1E40AF";
    static final String HA_ACCENT_COLOR = "#60A5FA";
    static final String HA_BANNER_URL = "/demo-banners/co-ha-toan.webp";
    static final String HA_LOGO_URL = "/demo-banners/co-ha-toan-logo.webp";

    // Thầy Nguyễn Đình Nhì — gói Trả phí, Hóa THCS, tông xanh lá (AI Branding).
    static final UUID NHI_TENANT_ID = UUID.fromString("b1100000-0000-4000-a000-000000000002");
    static final String NHI_TENANT_SLUG = "thay-nhi-hoa";
    static final String NHI_TENANT_REF = "dev-tenant-thay-nhi-hoa";
    static final String NHI_FRONTEND_URL = "https://thay-nhi-hoa.kite.local";
    static final String NHI_DISPLAY_NAME = "Hóa học THCS thầy Nguyễn Đình Nhì";
    static final String NHI_TAGLINE = "Hóa học THCS — học là hiểu";
    static final String NHI_PRIMARY_COLOR = "#16A34A";   // xanh lá
    static final String NHI_SECONDARY_COLOR = "#14532D";
    static final String NHI_ACCENT_COLOR = "#4ADE80";
    static final String NHI_BANNER_URL = "/demo-banners/thay-nhi-hoa.webp";
    static final String NHI_LOGO_URL = "/demo-banners/thay-nhi-hoa-logo.webp";

    // ── GAP-1194 — data-driven landing sections (teachers / pricing / stats) ──
    // JSONB seeded into LandingPage.{teachers,pricingTiers,stats}. Shapes match the FE
    // consumer contract in kiteclass-frontend (public)/page.tsx (the canonical de-facto
    // contract): teachers={name,subject,credentials[]}; pricingTiers={name,price,period,
    // features[],highlighted}; stats={value,label}. stats values are derived from the real
    // academic core seeded by DemoAcademicSeeder (Hà = 2 lớp / 12 HV; Nhì = 4 lớp / 35 HV;
    // chuyên cần = present%+late% per TenantSpec). Teacher avatar omitted on purpose →
    // TeachersSection falls back to name-initials (no remote 404 per GAP-958 anti-fabrication).

    // Hà — FREE tenant: 1 teacher, 1 free pricing tier, stats from 2-class/12-student core.
    static final String HA_TEACHERS_JSON = """
            [{"name":"Nguyễn Thị Hà","subject":"Toán tiểu học",
              "credentials":["Cử nhân Sư phạm Toán — ĐH Sư phạm Hà Nội",
                             "Hơn 6 năm kèm Toán tiểu học","Lớp nhỏ, bám sát từng học viên"]}]""";
    static final String HA_PRICING_JSON = """
            [{"name":"Lớp Toán cô Hà","price":"Miễn phí","period":"học thử + lộ trình",
              "features":["Lớp nhỏ 6 học viên","2 buổi/tuần (tối 18:00–19:30)",
                          "Bám sát từng học viên","Báo cáo tiến độ qua Zalo"],"highlighted":true}]""";
    static final String HA_STATS_JSON = """
            [{"value":"2","label":"Lớp đang mở"},{"value":"12","label":"Học viên"},
             {"value":"85%","label":"Tỷ lệ chuyên cần"}]""";

    // Nhì — PAID tenant: 1 teacher, 3 paid pricing tiers (thesis §4.4 bảng giá nhiều mức),
    // stats from 4-class/35-student core.
    static final String NHI_TEACHERS_JSON = """
            [{"name":"Nguyễn Đình Nhì","subject":"Hóa học THCS",
              "credentials":["Thạc sĩ Hóa học — ĐH Khoa học Tự nhiên",
                             "Luyện Hóa THCS lộ trình bài bản","Chuyên cần cao, báo cáo chi tiết"]}]""";
    static final String NHI_PRICING_JSON = """
            [{"name":"Lớp Hóa 8","price":"1.200.000đ","period":"/tháng",
              "features":["3 buổi/tuần","Lớp 8A & 8B","Tài liệu chuyên đề","Báo cáo chuyên cần"],
              "highlighted":false},
             {"name":"Lớp Hóa 9","price":"1.500.000đ","period":"/tháng",
              "features":["3 buổi/tuần","Luyện thi vào 10","Thi thử định kỳ","Chữa đề chi tiết"],
              "highlighted":true},
             {"name":"Luyện thi vào 10","price":"1.800.000đ","period":"/tháng",
              "features":["Lộ trình chuyên sâu","Cường độ cao (lớp 9B)","Cam kết đầu ra","Ôn sát kỳ thi"],
              "highlighted":false}]""";
    static final String NHI_STATS_JSON = """
            [{"value":"4","label":"Lớp đang mở"},{"value":"35","label":"Học viên"},
             {"value":"94%","label":"Tỷ lệ chuyên cần"}]""";

    // Cô Đỗ Lan Khánh — §4.1 walkthrough tenant; reuses the Sky instance, adds a Branding row.
    static final String KHANH_DISPLAY_NAME = "Trung tâm cô Đỗ Lan Khánh";
    static final String KHANH_BANNER_URL = "/demo-banners/co-khanh-phapluat.webp";
    static final String KHANH_LOGO_URL = "/demo-banners/co-khanh-phapluat-logo.webp";
    // GAP-826 — 2nd carousel slide for the §4.1 walkthrough tenant so the demo shows the
    // banner rotator (≥2 slides → FE renders dots/arrows + auto-rotate). Lives in
    // public/demo/sky/ — a gitignored AI-composed scene per GAP-810 (the dir already named
    // canonical in seedSkyLanding's javadoc). Present on the dev box, NOT committed: verify
    // it exists locally before the runtime walk, else the 2nd slide 404s (1st slide +
    // carousel chrome still render). All other trio tenants ship a single committed banner.
    static final String KHANH_BANNER_2_URL = "/demo/sky/teacher-do-lan-khanh.webp";

    // ── GAP-1083 / GAP-1205 / GAP-1206 — short center name + landing-100 F-section content ──
    // centerName = the center's own short display name (nav/footer/JsonLd prefer this over the
    // heroTitle marketing slogan, per GAP-1083). Distinct from Branding.displayName (longer form).
    static final String HA_CENTER_NAME = "Cô Hà Toán";
    static final String NHI_CENTER_NAME = "Thầy Nhì Hóa";
    static final String SKY_CENTER_NAME = "Sky Education";

    // F-section JSONB shapes mirror the FE consumer contract in kiteclass-frontend
    // (public)/page.tsx: problemSolution=[{title (pain),description (problem),items[] (fixes)}];
    // howItWorks=[{title (step),description}]; trustStrip=[{icon,title,description}] where icon ∈
    // {"shield","lock","support","vn","spark"}. Audience = phụ huynh/học viên (NOT platform-pitch),
    // tiếng Việt, khớp persona từng giáo viên. Non-empty → FE renders per-tenant; null → VN default.

    static final String HA_PROBLEM_SOLUTION_JSON = """
            [{"title":"Con mất gốc, sợ môn Toán",
              "description":"Nhiều bé tiểu học hổng kiến thức nền nên càng học càng nản.",
              "items":["Kiểm tra đầu vào miễn phí để biết con đang hổng phần nào",
                       "Lộ trình kèm sát, lấp lỗ hổng từ gốc"]},
             {"title":"Học mãi chưa tiến bộ",
              "description":"Lớp đông khiến con ngại hỏi, cô khó theo sát từng bạn.",
              "items":["Lớp nhỏ 6 học viên, cô kèm sát từng con",
                       "Báo cáo tiến độ gửi phụ huynh hằng tuần"]}]""";
    static final String HA_HOW_IT_WORKS_JSON = """
            [{"title":"Kiểm tra đầu vào miễn phí",
              "description":"Đánh giá đúng trình độ, xác định lỗ hổng kiến thức của con."},
             {"title":"Học theo lộ trình riêng",
              "description":"Lộ trình cá nhân hóa, lớp nhỏ, cô bám sát từng buổi học."},
             {"title":"Phụ huynh nhận báo cáo hằng tuần",
              "description":"Tiến độ và nhận xét của con được gửi qua Zalo mỗi tuần."}]""";
    static final String HA_TRUST_STRIP_JSON = """
            [{"icon":"spark","title":"6+ năm kinh nghiệm",
              "description":"Cô Hà kèm Toán tiểu học, bám sát từng học viên."},
             {"icon":"support","title":"Lớp nhỏ 6 học viên",
              "description":"Sĩ số nhỏ để cô theo sát từng con."},
             {"icon":"shield","title":"Báo cáo hằng tuần",
              "description":"Phụ huynh nắm tiến độ con qua báo cáo Zalo mỗi tuần."}]""";

    static final String NHI_PROBLEM_SOLUTION_JSON = """
            [{"title":"Hóa học khó, dễ mất căn bản",
              "description":"Hóa THCS nhiều khái niệm trừu tượng nên học sinh dễ hổng kiến thức.",
              "items":["Hệ thống lại kiến thức nền theo từng chuyên đề",
                       "Luyện đề bài bản từ cơ bản đến nâng cao"]},
             {"title":"Lo lắng kỳ thi vào 10",
              "description":"Áp lực thi chuyển cấp khiến nhiều em mất phương hướng ôn tập.",
              "items":["Lộ trình luyện thi vào 10 rõ ràng theo tuần",
                       "Thi thử định kỳ kèm chữa đề chi tiết"]}]""";
    static final String NHI_HOW_IT_WORKS_JSON = """
            [{"title":"Kiểm tra đầu vào miễn phí",
              "description":"Xác định trình độ Hóa hiện tại để xếp lộ trình phù hợp."},
             {"title":"Học theo lộ trình chuyên đề",
              "description":"Học bài bản theo chuyên đề, lớp tách theo trình độ 8 và 9."},
             {"title":"Báo cáo chuyên cần & tiến độ",
              "description":"Phụ huynh nhận báo cáo chuyên cần và kết quả thi thử định kỳ."}]""";
    static final String NHI_TRUST_STRIP_JSON = """
            [{"icon":"spark","title":"Thạc sĩ Hóa học",
              "description":"Thầy Nhì — Thạc sĩ Hóa, ĐH Khoa học Tự nhiên."},
             {"icon":"shield","title":"Cam kết đầu ra",
              "description":"Lộ trình luyện thi vào 10 cam kết tiến bộ rõ rệt."},
             {"icon":"support","title":"Chuyên cần 94%",
              "description":"Theo sát chuyên cần, báo cáo chi tiết cho phụ huynh."}]""";

    // Sky / cô Khánh — Anh ngữ (§4.1 walkthrough tenant on the Sky instance).
    static final String SKY_PROBLEM_SOLUTION_JSON = """
            [{"title":"Con ngại nói tiếng Anh",
              "description":"Nhiều bé mất gốc, ngại giao tiếp vì thiếu môi trường luyện tập.",
              "items":["Lộ trình lấy lại căn bản từ đầu",
                       "Luyện phản xạ nghe – nói ngay mỗi buổi"]},
             {"title":"Học nhiều nhưng chưa hiệu quả",
              "description":"Học thêm tràn lan mà con vẫn chưa tự tin dùng tiếng Anh.",
              "items":["Lớp nhỏ, giáo viên kèm sát phát âm",
                       "Báo cáo tiến bộ thường xuyên cho phụ huynh"]}]""";
    static final String SKY_HOW_IT_WORKS_JSON = """
            [{"title":"Kiểm tra trình độ miễn phí",
              "description":"Đánh giá nghe – nói – đọc – viết để xếp lớp phù hợp."},
             {"title":"Học theo lộ trình cá nhân",
              "description":"Lộ trình bám sát mục tiêu, lớp nhỏ, giáo viên tận tâm."},
             {"title":"Phụ huynh theo dõi tiến bộ",
              "description":"Nhận báo cáo kết quả học tập của con định kỳ qua Zalo."}]""";
    static final String SKY_TRUST_STRIP_JSON = """
            [{"icon":"spark","title":"Giáo viên tận tâm",
              "description":"Đội ngũ giàu kinh nghiệm luyện Anh ngữ cho mọi lứa tuổi."},
             {"icon":"support","title":"Lớp nhỏ kèm sát",
              "description":"Sĩ số nhỏ giúp con được luyện nói nhiều hơn mỗi buổi."},
             {"icon":"vn","title":"Lộ trình rõ ràng",
              "description":"Cam kết tiến bộ với lộ trình minh bạch theo từng giai đoạn."}]""";

    // ── GAP-1224 — FAQ + Testimonials JSONB seed (landing-100 re-score delta #1) ──
    // Component FaqSection / TestimonialsSection đã port + wire (hide-when-empty per
    // GAP-958) nhưng demo-trio không có data → section ẩn → landing mỏng hơn kit (−4).
    // Shapes mirror the FE consumer contract in kiteclass-frontend (public)/page.tsx:
    // faqs=[{"question","answer"}]; testimonials=[{"author","role","content","rating":int}].
    // Audience = phụ huynh/học viên, tiếng Việt thật, KHÔNG bịa số liệu thống kê (GAP-958):
    // testimonials giữ định tính (con tự tin/tiến bộ), không nêu con số/tỷ lệ bịa.

    static final String HA_FAQS_JSON = """
            [{"question":"Con đang mất gốc Toán thì có theo kịp lớp không ạ?",
              "answer":"Cô kiểm tra đầu vào miễn phí để biết con hổng phần nào rồi kèm sát từ gốc. Lớp nhỏ nên cô theo sát từng bé."},
             {"question":"Học phí đóng theo tháng hay theo khóa ạ?",
              "answer":"Phụ huynh đóng theo tháng. Cô trao đổi rõ lộ trình và học phí từng giai đoạn trước khi con bắt đầu, không phí ẩn."},
             {"question":"Mỗi lớp có khoảng bao nhiêu bé ạ?",
              "answer":"Lớp giữ sĩ số nhỏ (khoảng 6 bé) để cô kèm sát từng con, bé nào cũng được hỏi bài và chữa bài mỗi buổi."},
             {"question":"Con nghỉ ốm thì có được học bù không ạ?",
              "answer":"Có ạ. Khi con nghỉ có phép, cô ôn lại phần đã học để con không bị hổng và báo nội dung buổi học qua Zalo."},
             {"question":"Cô có báo tình hình học của con cho phụ huynh không ạ?",
              "answer":"Cô gửi báo cáo tiến độ và nhận xét của con qua Zalo hằng tuần để phụ huynh nắm con cần hỗ trợ thêm gì."}]""";
    static final String HA_TESTIMONIALS_JSON = """
            [{"author":"Chị Nguyễn Thị Lan","role":"Phụ huynh bé Minh Khôi","rating":5,
              "content":"Con trước sợ môn Toán, học với cô Hà ít lâu thì tự tin hẳn, về nhà chủ động làm bài. Mình rất yên tâm."},
             {"author":"Anh Trần Văn Hùng","role":"Phụ huynh bé Bảo An","rating":5,
              "content":"Lớp ít bạn nên cô kèm rất sát, chỗ nào con chưa hiểu cô giảng lại ngay. Cô nhiệt tình, hay trao đổi."},
             {"author":"Chị Lê Thu Hà","role":"Phụ huynh bé Thảo Vy","rating":5,
              "content":"Con đi học về vui vẻ, không còn áp lực như trước. Lộ trình rõ ràng, mình thấy con tiến bộ từng tuần."}]""";

    static final String NHI_FAQS_JSON = """
            [{"question":"Con chuẩn bị thi vào 10 thì nên học lớp nào ạ?",
              "answer":"Thầy có lớp luyện thi vào 10 lộ trình chuyên sâu, thi thử định kỳ. Thầy kiểm tra đầu vào để xếp lớp."},
             {"question":"Lớp Hóa 8 và Hóa 9 khác nhau thế nào ạ?",
              "answer":"Lớp 8 xây nền tảng theo chuyên đề, lớp 9 nâng cao bám sát thi chuyển cấp. Con học đúng trình độ, không quá sức."},
             {"question":"Học phí từng lớp là bao nhiêu ạ?",
              "answer":"Mỗi lớp có mức học phí riêng theo thời lượng và cường độ, thầy báo minh bạch. Xem chi tiết ở mục Học phí."},
             {"question":"Lớp có thi thử và chữa đề không ạ?",
              "answer":"Có ạ. Thầy tổ chức thi thử định kỳ và chữa đề chi tiết để con quen áp lực phòng thi và trình bày bài đạt điểm."},
             {"question":"Con mất căn bản Hóa có học được không ạ?",
              "answer":"Được ạ. Thầy hệ thống kiến thức nền theo chuyên đề trước rồi mới nâng cao, nên con mất gốc vẫn theo được."}]""";
    static final String NHI_TESTIMONIALS_JSON = """
            [{"author":"Chị Phạm Thu Hà","role":"Phụ huynh em lớp 9","rating":5,
              "content":"Con ôn thi vào 10 với thầy Nhì, được chữa đề kỹ và thi thử thường xuyên nên vào phòng thi rất bình tĩnh."},
             {"author":"Anh Hoàng Anh Tuấn","role":"Phụ huynh em lớp 8","rating":5,
              "content":"Trước con học Hóa khá đuối, sau khi học theo chuyên đề của thầy thì hiểu bài hẳn. Thầy tận tâm và nghiêm túc."},
             {"author":"Chị Lê Diệu Linh","role":"Phụ huynh em lớp 9","rating":5,
              "content":"Lộ trình luyện thi rõ ràng, thầy nhắc bài và theo sát từng em. Mình rất yên tâm khi con học cùng thầy."}]""";

    // Sky / cô Khánh — Anh ngữ. Sky KHÔNG có academic core (DemoAcademicSeeder chỉ seed
    // Hà + Nhì) nên CỐ TÌNH không seed stats (số liệu phải thật, không bịa per GAP-958);
    // thay vào đó lấp các section nội dung (teacher card / pricing / faq / testimonials)
    // để nâng lowest-tenant bar mà vẫn trung thực (re-score §4.1 sky sparse).
    static final String SKY_TEACHERS_JSON = """
            [{"name":"Đỗ Lan Khánh","subject":"Tiếng Anh giao tiếp & thiếu nhi",
              "credentials":["Cử nhân Ngôn ngữ Anh","Nhiều năm luyện phát âm và phản xạ giao tiếp",
                             "Lớp nhỏ, kèm sát phát âm từng học viên"]}]""";
    static final String SKY_PRICING_JSON = """
            [{"name":"Tiếng Anh thiếu nhi","price":"900.000đ","period":"/tháng",
              "features":["2 buổi/tuần","Lớp nhỏ 8 học viên","Luyện nghe – nói qua trò chơi","Báo cáo tiến bộ qua Zalo"],
              "highlighted":false},
             {"name":"Giao tiếp cơ bản","price":"1.200.000đ","period":"/tháng",
              "features":["3 buổi/tuần","Luyện phản xạ nghe – nói","Lộ trình cá nhân hóa","Kiểm tra trình độ định kỳ"],
              "highlighted":true},
             {"name":"Luyện thi & nâng cao","price":"1.500.000đ","period":"/tháng",
              "features":["Lộ trình chuyên sâu","Giáo viên kèm sát","Chữa bài chi tiết","Cam kết tiến bộ"],
              "highlighted":false}]""";
    static final String SKY_FAQS_JSON = """
            [{"question":"Con ngại nói tiếng Anh thì bắt đầu thế nào ạ?",
              "answer":"Trung tâm kiểm tra trình độ miễn phí rồi xếp con vào lớp phù hợp. Lớp nhỏ, con luyện nói mỗi buổi nên tự tin."},
             {"question":"Mỗi lớp có bao nhiêu học viên ạ?",
              "answer":"Lớp giữ sĩ số nhỏ để mỗi bé được luyện nói nhiều hơn và giáo viên sửa phát âm sát từng bạn."},
             {"question":"Học phí đóng theo tháng hay theo khóa ạ?",
              "answer":"Phụ huynh đóng theo tháng theo từng lộ trình. Mức học phí được thông báo minh bạch trước khi con vào học."},
             {"question":"Có kiểm tra trình độ đầu vào không ạ?",
              "answer":"Có ạ. Trung tâm đánh giá nghe – nói – đọc – viết để xếp lớp đúng trình độ, giúp con học hiệu quả hơn."}]""";
    static final String SKY_TESTIMONIALS_JSON = """
            [{"author":"Chị Trần Thị Hồng","role":"Phụ huynh bé Gia Bảo","rating":5,
              "content":"Con trước rất ngại nói tiếng Anh, học ở đây một thời gian thì dám nói, dám hỏi. Giáo viên kiên nhẫn, vui tính."},
             {"author":"Anh Nguyễn Văn Thành","role":"Phụ huynh bé Khánh Vy","rating":5,
              "content":"Lớp nhỏ nên cô kèm sát phát âm cho con. Mình thấy con thích đi học và tiến bộ rõ ở phần nghe – nói."},
             {"author":"Chị Đỗ Thanh Mai","role":"Phụ huynh bé Minh Anh","rating":5,
              "content":"Cô Khánh tận tâm, thường xuyên báo tình hình học của con. Con tự tin giao tiếp hơn hẳn so với trước."}]""";

    private final FrontendInstanceRepository instanceRepo;
    private final BrandingResourceRepository resourceRepo;
    private final QualityReportRepository qualityRepo;
    private final LandingPageRepository landingPageRepository;
    private final BrandingRepository brandingRepository;
    private final OutboxEventWriter outbox;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    // GAP-1203: evict the public landing cache after an upsert so the next anonymous
    // visit re-reads the reconciled row instead of a stale cached response. Nullable
    // (no caching configured / unit tests) — guarded in {@link #evictLandingCache}.
    private final CacheManager cacheManager;

    /** Triggered after the Spring context is fully initialized. */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Seed each demo tenant under its own TenantContext so the
        // EntityPersistenceListener stamps the correct instance_id per resource.
        seedTenant(DEV_TENANT_ID, DEV_TENANT_SLUG);
        seedTenant(SKY_TENANT_ID, SKY_TENANT_SLUG);
        // Wave landing-100 Bucket G — demo-trio Branding settings (GAP-805).
        seedDemoTrio();
    }

    /**
     * Seeds the demo-trio (Khánh / Hà / Nhì) so each tenant's public homepage renders
     * a distinct branded theme + banner per thesis §4.1-4.2. Idempotent.
     */
    private void seedDemoTrio() {
        seedTrioTenant(new TrioSpec(HA_TENANT_ID, HA_TENANT_SLUG, HA_TENANT_REF, HA_FRONTEND_URL,
                HA_DISPLAY_NAME, HA_CENTER_NAME, HA_TAGLINE, HA_PRIMARY_COLOR, HA_SECONDARY_COLOR, HA_ACCENT_COLOR,
                HA_BANNER_URL, HA_LOGO_URL,
                // GAP-826 — single committed banner (1 slide → FE renders static, no carousel chrome).
                List.of(HA_BANNER_URL),
                "Lấy lại căn bản môn Toán cùng cô Hà",
                "Lộ trình Toán tiểu học bài bản, lớp nhỏ, kèm sát từng học viên.",
                "https://zalo.me/co-ha-toan", "https://facebook.com/cohatoan",
                HA_TEACHERS_JSON, HA_PRICING_JSON, HA_STATS_JSON,
                HA_PROBLEM_SOLUTION_JSON, HA_HOW_IT_WORKS_JSON, HA_TRUST_STRIP_JSON,
                // GAP-1224 — FAQ + testimonials so FaqSection/TestimonialsSection render.
                HA_FAQS_JSON, HA_TESTIMONIALS_JSON));
        seedTrioTenant(new TrioSpec(NHI_TENANT_ID, NHI_TENANT_SLUG, NHI_TENANT_REF, NHI_FRONTEND_URL,
                NHI_DISPLAY_NAME, NHI_CENTER_NAME, NHI_TAGLINE, NHI_PRIMARY_COLOR, NHI_SECONDARY_COLOR, NHI_ACCENT_COLOR,
                NHI_BANNER_URL, NHI_LOGO_URL,
                // GAP-826 — single committed banner (1 slide → static render).
                List.of(NHI_BANNER_URL),
                "Hóa học THCS — học là hiểu cùng thầy Nhì",
                "Khóa Hóa học THCS đầy đủ, bộ nhận diện sinh tự động bằng AI Branding.",
                "https://zalo.me/thay-nhi-hoa", "https://facebook.com/thaynhihoa",
                NHI_TEACHERS_JSON, NHI_PRICING_JSON, NHI_STATS_JSON,
                NHI_PROBLEM_SOLUTION_JSON, NHI_HOW_IT_WORKS_JSON, NHI_TRUST_STRIP_JSON,
                // GAP-1224 — FAQ + testimonials so FaqSection/TestimonialsSection render.
                NHI_FAQS_JSON, NHI_TESTIMONIALS_JSON));
        seedKhanhBranding();
    }

    /** Immutable spec for one demo-trio tenant seed (keeps {@link #seedTrioTenant} param count sane). */
    private record TrioSpec(UUID tenantId, String slug, String tenantRef, String frontendUrl,
                            String displayName, String centerName, String tagline,
                            String primary, String secondary, String accent,
                            String bannerUrl, String logoUrl,
                            // GAP-826 — ordered hero banner carousel slides (slide order = list order).
                            List<String> heroImages,
                            String heroTitle, String heroSubtitle, String zaloUrl, String facebookUrl,
                            // GAP-1194 — JSONB landing sections (FE-contract shapes; see constants above).
                            String teachersJson, String pricingJson, String statsJson,
                            // GAP-1083/1205 — landing-100 F-section JSONB (problem/how/trust).
                            String problemSolutionJson, String howItWorksJson, String trustStripJson,
                            // GAP-1224 — FAQ + testimonials JSONB (FaqSection / TestimonialsSection).
                            String faqsJson, String testimonialsJson) {
    }

    /**
     * Seeds one demo-trio tenant: DEPLOYED FrontendInstance + Branding settings row
     * + LandingPage hero. Each step is independently idempotent (skip when present /
     * upsert the landing) so re-running on every boot does not duplicate rows.
     *
     * <p>The Branding {@code instanceId} is stamped by {@code EntityPersistenceListener}
     * from the {@link TenantContext} set by the caller — consistent with how the
     * {@link BrandingResource} rows are seeded.
     */
    private void seedTrioTenant(TrioSpec spec) {
        try {
            TenantContext.setCurrentTenant(spec.tenantId());
            transactionTemplate.executeWithoutResult(status -> {
                // 1. FrontendInstance (DEPLOYED) — idempotent by slug.
                if (!instanceRepo.existsBySlugAndDeletedFalse(spec.slug())) {
                    FrontendInstance instance = FrontendInstance.builder()
                            .tenantSlug(spec.tenantRef())
                            .slug(spec.slug())
                            .frontendUrl(spec.frontendUrl())
                            .build();
                    instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
                    instance.transitionTo(FrontendInstanceStatus.GENERATING);
                    instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
                    instanceRepo.save(instance);
                }
                // 2. Branding settings row — UPSERT (GAP-1203): reconcile content to the current
                //    seed constants even when the row already exists, so a row seeded by an older
                //    constant set (e.g. stale .png logo) is refreshed instead of skipped. Fixed
                //    demo UUID → only the demo row is touched, never user-created rows.
                Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(spec.tenantId())
                        .orElseGet(Branding::new);
                branding.setDisplayName(spec.displayName());
                branding.setTagline(spec.tagline());
                branding.setPrimaryColor(spec.primary());
                branding.setSecondaryColor(spec.secondary());
                branding.setAccentColor(spec.accent());
                branding.setThemeConfigJson(
                        buildTrioThemeConfigJson(spec.displayName(), spec.tagline(),
                                spec.primary(), spec.secondary(), spec.accent(),
                                spec.zaloUrl(), spec.facebookUrl(), spec.frontendUrl()));
                branding.setLogoUrl(spec.logoUrl());
                branding.setZaloUrl(spec.zaloUrl());
                branding.setFacebookUrl(spec.facebookUrl());
                branding.setWebsiteUrl(spec.frontendUrl());
                brandingRepository.save(branding);

                // 3. LandingPage — UPSERT (lazily created on first GET otherwise, BR-MKT-001).
                LandingPage lp = landingPageRepository.findByInstanceIdAndDeletedFalse(spec.tenantId())
                        .orElseGet(() -> {
                            LandingPage created = new LandingPage();
                            created.setInstanceId(spec.tenantId());
                            return created;
                        });
                lp.setCenterName(spec.centerName());
                lp.setHeroTitle(spec.heroTitle());
                lp.setHeroSubtitle(spec.heroSubtitle());
                lp.setHeroImageUrl(spec.bannerUrl());
                // GAP-826: carousel slides (≥2 → FE rotator; 1 → static single banner).
                lp.setHeroImages(spec.heroImages());
                // GAP-1203/1204: store the stable static logo path (NOT a presigned MinIO URL) so
                // the public landing header logo never expires. Reconciled each boot from constants.
                lp.setLogoUrl(spec.logoUrl());
                lp.setTagline(spec.tagline());
                lp.setPrimaryColor(spec.primary());
                lp.setSecondaryColor(spec.secondary());
                // GAP-1083: surface the tenant's Zalo OA in the FloatingCTA.
                lp.setZaloUrl(spec.zaloUrl());
                // template_type NOT NULL (DB constraint) — trio đều là GV cá nhân → "personal"
                // ("personal" GV độc lập | "organization" trung tâm, per LandingPage entity).
                lp.setTemplateType("personal");
                // GAP-1194 — data-driven sections (teachers / pricing / stats). Re-set each boot:
                // idempotent (columns on the single landing row; overwrite, never duplicates).
                // FE (public)/page.tsx maps these JSONB shapes → section slots; null/empty → section
                // hides (anti-fabrication). teachers renders only on a template that lists the
                // 'teachers' section (PERSONAL_TEMPLATE updated in this PR to enable it).
                lp.setTeachers(landingJson(spec.teachersJson()));
                lp.setPricingTiers(landingJson(spec.pricingJson()));
                lp.setStats(landingJson(spec.statsJson()));
                // GAP-1083/1205 — landing-100 F-sections (problem→solution / how-it-works / trust).
                // Per-tenant phụ huynh/học viên copy (not platform-pitch); FE falls back to generic
                // VN default when null.
                lp.setProblemSolution(landingJson(spec.problemSolutionJson()));
                lp.setHowItWorks(landingJson(spec.howItWorksJson()));
                lp.setTrustStrip(landingJson(spec.trustStripJson()));
                // GAP-1224 — FAQ + testimonials (FaqSection / TestimonialsSection hide-when-empty).
                lp.setFaqs(landingJson(spec.faqsJson()));
                lp.setTestimonials(landingJson(spec.testimonialsJson()));
                landingPageRepository.save(lp);
            });
            // GAP-1203: evict AFTER the transaction commits so the next read repopulates
            // the cache from the reconciled row (not a stale pre-upsert cached response).
            evictLandingCache(spec.tenantId());
            log.info("Seeded demo-trio tenant (slug={}, primary={})", spec.slug(), spec.primary());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Evicts the public landing-page cache entry for {@code instanceId} so the next
     * anonymous visit re-reads the freshly upserted row (GAP-1203). The cache name +
     * key mirror {@code LandingPageServiceImpl} ({@code @Cacheable("landingPages",
     * key="#tenantId")}). No-op when caching is not configured (unit tests / nooop
     * cache) — never fails the boot-time seed.
     */
    private void evictLandingCache(UUID instanceId) {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache("landingPages");
        if (cache != null) {
            cache.evict(instanceId);
        }
    }

    /**
     * Seeds cô Đỗ Lan Khánh's Branding settings row on the existing Sky instance
     * (§4.1 walkthrough tenant). The Sky FrontendInstance + LandingPage are seeded
     * elsewhere; here we only add the missing {@link Branding} settings row.
     */
    private void seedKhanhBranding() {
        try {
            TenantContext.setCurrentTenant(SKY_TENANT_ID);
            transactionTemplate.executeWithoutResult(status -> {
                // UPSERT (GAP-1203): reconcile to current constants — refresh a stale row
                // (e.g. presigned logo) instead of skipping. Fixed Sky UUID → demo row only.
                Branding branding = brandingRepository.findByInstanceIdAndDeletedFalse(SKY_TENANT_ID)
                        .orElseGet(Branding::new);
                branding.setDisplayName(KHANH_DISPLAY_NAME);
                branding.setTagline(SKY_TAGLINE);
                branding.setPrimaryColor(SKY_PRIMARY_COLOR);
                branding.setSecondaryColor(SKY_SECONDARY_COLOR);
                branding.setAccentColor(SKY_ACCENT_COLOR);
                branding.setThemeConfigJson(buildSkyThemeConfigJson());
                // GAP-1204: stable static logo path (NOT a presigned MinIO URL that expires).
                branding.setLogoUrl(KHANH_LOGO_URL);
                brandingRepository.save(branding);
            });
            log.info("Seeded Khánh (Sky) Branding settings row (instance={})", SKY_TENANT_ID);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Parses a JSON literal into a {@link JsonNode} for a JSONB landing-section column
     * ({@code teachers} / {@code pricing_tiers} / {@code stats}). Fail-loud on malformed
     * seed JSON — a broken literal is a dev-time author error, not a runtime condition.
     */
    private JsonNode landingJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Malformed demo landing-section JSON (GAP-1194)", e);
        }
    }

    /** Builds the theme-config JSON stored in {@link Branding#getThemeConfigJson()} for a trio tenant. */
    private String buildTrioThemeConfigJson(String displayName, String tagline,
                                            String primary, String secondary, String accent,
                                            String zaloUrl, String facebookUrl, String website) {
        Map<String, Object> theme = new LinkedHashMap<>();
        theme.put("displayName", displayName);
        theme.put("tagline", tagline);

        Map<String, String> cssVars = new LinkedHashMap<>();
        cssVars.put("--brand-primary", primary);
        cssVars.put("--brand-secondary", secondary);
        cssVars.put("--brand-accent", accent);
        theme.put("cssVars", cssVars);

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("zalo", zaloUrl);
        contact.put("facebook", facebookUrl);
        contact.put("website", website);
        theme.put("contact", contact);

        try {
            return objectMapper.writeValueAsString(theme);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize demo-trio theme config", e);
        }
    }

    private void seedTenant(UUID tenantId, String slug) {
        try {
            TenantContext.setCurrentTenant(tenantId);
            transactionTemplate.executeWithoutResult(status -> {
                seed(slug);
                if (SKY_TENANT_SLUG.equals(slug)) {
                    seedSkyLanding(tenantId);
                }
            });
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Seeds the Sky Education landing page hero so the public homepage renders the
     * promo banner (slogan + teacher portrait + CTA). Idempotent: refreshes the demo
     * hero fields on each boot. {@code heroImageUrl} points at a static asset served
     * by the frontend ({@code public/demo/sky/}, local-only/gitignored per GAP-810);
     * the seed stores only the URL string. The landing row is otherwise lazily
     * created on first GET (BR-MKT-001), so we upsert here.
     */
    private void seedSkyLanding(UUID tenantId) {
        LandingPage lp = landingPageRepository.findByInstanceIdAndDeletedFalse(tenantId)
                .orElseGet(() -> {
                    LandingPage created = new LandingPage();
                    created.setInstanceId(tenantId);
                    return created;
                });
        // GAP-1083: short center name preferred over the hero slogan in nav/footer/JsonLd.
        lp.setCenterName(SKY_CENTER_NAME);
        lp.setHeroTitle("Mất gốc tiếng Anh? Đã có cô Khánh");
        lp.setHeroSubtitle("Lộ trình lấy lại căn bản tiếng Anh, học cùng giáo viên tận tâm.");
        lp.setHeroImageUrl(KHANH_BANNER_URL);
        // GAP-826: 2-slide carousel for the §4.1 walkthrough tenant → FE renders the rotator
        // (dots + arrows + auto-rotate). 2nd slide is the gitignored GAP-810 asset (see
        // KHANH_BANNER_2_URL note) — verify present locally before the runtime walk.
        lp.setHeroImages(List.of(KHANH_BANNER_URL, KHANH_BANNER_2_URL));
        // GAP-1204: overwrite any stale presigned logo with the stable static demo logo so
        // the Sky landing header never renders a broken (403) image after the 7-day TTL.
        lp.setLogoUrl(KHANH_LOGO_URL);
        lp.setTagline(SKY_TAGLINE);
        lp.setPrimaryColor(SKY_PRIMARY_COLOR);
        lp.setSecondaryColor(SKY_SECONDARY_COLOR);
        if (lp.getTemplateType() == null) {
            lp.setTemplateType("personal");
        }
        // GAP-1083/1205 — landing-100 F-sections (Anh ngữ, cô Khánh). Per-tenant VN copy.
        lp.setProblemSolution(landingJson(SKY_PROBLEM_SOLUTION_JSON));
        lp.setHowItWorks(landingJson(SKY_HOW_IT_WORKS_JSON));
        lp.setTrustStrip(landingJson(SKY_TRUST_STRIP_JSON));
        // GAP-1224 — lấp các section sky còn thiếu (re-score §4.1 sparse → lowest bar 72):
        // teacher card + bảng giá + FAQ + testimonials. KHÔNG seed stats (sky không có
        // academic core → số liệu phải thật, không bịa per GAP-958).
        lp.setTeachers(landingJson(SKY_TEACHERS_JSON));
        lp.setPricingTiers(landingJson(SKY_PRICING_JSON));
        lp.setFaqs(landingJson(SKY_FAQS_JSON));
        lp.setTestimonials(landingJson(SKY_TESTIMONIALS_JSON));
        landingPageRepository.save(lp);
        // GAP-1203: evict so the next read repopulates from the reconciled row. seedSkyLanding
        // runs inside the seedTenant transaction; eviction here (pre-commit) is still correct
        // because @Cacheable repopulates lazily on the next anonymous GET after commit.
        evictLandingCache(tenantId);
        log.info("Seeded Sky landing hero (instance={})", tenantId);
    }

    /**
     * Seeds the thanglong demo tenant. Kept for backward compatibility with
     * existing tests that call {@code seed()} with no argument.
     *
     * <p>Public for direct invocation in tests. Caller must have a transaction
     * open (or rely on {@link #onApplicationReady()} which wraps via
     * {@link TransactionTemplate}) AND must set {@link TenantContext} to
     * {@link #DEV_TENANT_ID} first.
     */
    @Transactional
    public void seed() {
        seed(DEV_TENANT_SLUG);
    }

    /**
     * Seeds branding for the tenant identified by {@code slug}. Idempotent —
     * skips silently when an instance with the slug already exists.
     *
     * <p>Caller MUST have already set {@link TenantContext} to the matching tenant
     * id so the {@code EntityPersistenceListener} stamps the right instance_id.
     */
    @Transactional
    public void seed(String slug) {
        if (instanceRepo.existsBySlugAndDeletedFalse(slug)) {
            log.info("Dev branding seed already present (slug={}). Skipping.", slug);
            return;
        }

        FrontendInstance instance = instanceRepo.save(buildInstance(slug));
        resourceRepo.saveAll(buildResources(slug));
        qualityRepo.save(buildQualityReport(instance));
        emitOutboxEvent(instance);

        log.info("Seeded dev branding: instance id={}, slug={}, brandingVersion={}",
                instance.getId(), instance.getSlug(), instance.getBrandingVersion());
    }

    private FrontendInstance buildInstance(String slug) {
        boolean isSky = SKY_TENANT_SLUG.equals(slug);
        FrontendInstance instance = FrontendInstance.builder()
                .tenantSlug(isSky ? SKY_TENANT_REF : DEV_TENANT_REF)
                .slug(slug)
                .frontendUrl(isSky ? SKY_FRONTEND_URL : DEV_FRONTEND_URL)
                .build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
        return instance;
    }

    private List<BrandingResource> buildResources(String slug) {
        if (SKY_TENANT_SLUG.equals(slug)) {
            return buildSkyResources();
        }
        return buildThangLongResources();
    }

    private List<BrandingResource> buildThangLongResources() {
        BrandingResource logo = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .storageUrl("/mocks/assets/logo-thanglong.png")
                .build();
        logo.validateInvariants();

        BrandingResource banner = BrandingResource.builder()
                .type(ResourceType.BANNER)
                .category(ResourceCategory.TEMPLATE)
                .storageUrl("/mocks/assets/banner-thanglong.svg")
                .templateId(1L)
                .build();
        banner.validateInvariants();

        BrandingResource hero = BrandingResource.builder()
                .type(ResourceType.HERO)
                .category(ResourceCategory.FULL_AI)
                .storageUrl("/mocks/assets/hero-thanglong.png")
                .aiJobId(UUID.randomUUID())
                .build();
        hero.validateInvariants();

        return List.of(logo, banner, hero);
    }

    /**
     * Sky Education resources. The LOGO resource carries the theme config
     * (display name, tagline, custom CSS palette) in its {@code metadata} jsonb
     * column — the same slot the AI branding pipeline writes theme vars into —
     * so the FE renders Sky's warm orange/amber palette instead of the default
     * shadcn blue. Logo asset path is seeded directly (no upload controller round-trip
     * per GAP-804 / GAP-798b).
     */
    private List<BrandingResource> buildSkyResources() {
        BrandingResource logo = BrandingResource.builder()
                .type(ResourceType.LOGO)
                .category(ResourceCategory.STATIC)
                .storageUrl("/mocks/assets/logo-sky-education.png")
                .metadata(buildSkyThemeConfigJson())
                .build();
        logo.validateInvariants();

        BrandingResource banner = BrandingResource.builder()
                .type(ResourceType.BANNER)
                .category(ResourceCategory.TEMPLATE)
                .storageUrl("/mocks/assets/banner-sky-education.svg")
                .templateId(1L)
                .build();
        banner.validateInvariants();

        BrandingResource hero = BrandingResource.builder()
                .type(ResourceType.HERO)
                .category(ResourceCategory.FULL_AI)
                .storageUrl("/mocks/assets/hero-sky-education.png")
                .aiJobId(UUID.randomUUID())
                .build();
        hero.validateInvariants();

        return List.of(logo, banner, hero);
    }

    /**
     * Builds the Sky Education theme config JSON stored in the LOGO resource
     * metadata: display name, tagline, CSS palette + VN contact links.
     */
    private String buildSkyThemeConfigJson() {
        Map<String, Object> theme = new LinkedHashMap<>();
        theme.put("displayName", SKY_DISPLAY_NAME);
        theme.put("tagline", SKY_TAGLINE);

        Map<String, String> cssVars = new LinkedHashMap<>();
        cssVars.put("--brand-primary", SKY_PRIMARY_COLOR);
        cssVars.put("--brand-secondary", SKY_SECONDARY_COLOR);
        cssVars.put("--brand-accent", SKY_ACCENT_COLOR);
        theme.put("cssVars", cssVars);

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("zalo", "https://zalo.me/sky-education");
        contact.put("facebook", "https://facebook.com/skyedu.vn");
        contact.put("website", "https://sky-education.kite.local");
        theme.put("contact", contact);

        try {
            return objectMapper.writeValueAsString(theme);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Sky theme config", e);
        }
    }

    private QualityReport buildQualityReport(FrontendInstance instance) {
        return QualityReport.builder()
                .targetInstanceId(instance.getId())
                .brandingVersion(instance.getBrandingVersion())
                .score(85)
                .passed(true)
                .contrastScore(85)
                .cssVarsScore(90)
                .assetUrlsScore(80)
                .visualRegressionScore(82)
                .logoPlacementScore(88)
                .build();
    }

    private void emitOutboxEvent(FrontendInstance instance) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instanceId", instance.getId());
        payload.put("slug", instance.getSlug());
        payload.put("brandingVersion", instance.getBrandingVersion());
        payload.put("deployedAt", instance.getDeployedAt() == null ? null : instance.getDeployedAt().toString());
        try {
            outbox.enqueue(
                    "branding.updated",
                    "FrontendInstance",
                    String.valueOf(instance.getId()),
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize dev seed outbox payload", e);
        }
    }
}
