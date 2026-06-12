/* =============================================================
   KiteClass — Marketing landing components (beta-signup site)
   Targets P2 (chủ trung tâm). Reuses Icon set from primitives.jsx.
   100% tiếng Việt · VND format · VN sample data · no overclaim.
   ============================================================= */
const { useState: useS, useEffect: useE } = React;

/* tiny brand kite mark (default blue theme) */
function Kite({ size = 28 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <path d="M24 4L40 24L24 44L8 24L24 4Z" fill="rgb(var(--primary-strong))"></path>
      <path d="M24 4V44M8 24H40" stroke="white" strokeWidth="2" strokeOpacity="0.3"></path>
      <circle cx="24" cy="24" r="4" fill="rgb(var(--cta-strong))"></circle>
    </svg>
  );
}

/* ---------- Top beta announcement strip ---------- */
function BetaStrip() {
  return (
    <div className="beta-strip" role="region" aria-label="Thông báo Beta">
      <span className="tag">Beta</span>
      <span>KiteClass đang trong giai đoạn Beta — miễn phí cho trung tâm tham gia sớm.</span>
      <a href="#dang-ky">Đăng ký ngay →</a>
    </div>
  );
}

/* ---------- Nav ---------- */
function MsNav() {
  const links = [
    { label: "Tính năng", href: "#tinh-nang" },
    { label: "Cách hoạt động", href: "#cach-hoat-dong" },
    { label: "Cổng phụ huynh", href: "#phu-huynh" },
    { label: "An toàn dữ liệu", href: "#an-toan" },
  ];
  return (
    <header className="nav">
      <div className="container nav-inner">
        <a href="#" className="nav-logo" onClick={(e) => e.preventDefault()} aria-label="KiteClass trang chủ">
          <Kite size={34} />
          <span className="name"><span className="accent" style={{ color: "rgb(var(--primary-strong))" }}>Kite</span>Class</span>
        </a>
        <nav className="nav-links" aria-label="Điều hướng chính">
          {links.map((l) => <a key={l.label} href={l.href}>{l.label}</a>)}
        </nav>
        <div className="nav-cta">
          <span className="nav-beta"><span style={{ width: 7, height: 7, borderRadius: "50%", background: "rgb(var(--cta-strong))", display: "inline-block" }}></span> Beta</span>
          <a href="#dang-ky" className="btn btn-cta btn-sm">Đăng ký Beta</a>
        </div>
      </div>
    </header>
  );
}

/* ---------- App mockup (hero product preview) ---------- */
function AppMock() {
  const students = [
    { nm: "Trần Thị Hồng", in: "TH", s: "present" },
    { nm: "Nguyễn Văn An", in: "NA", s: "present" },
    { nm: "Phạm Thị Mai", in: "PM", s: "present" },
    { nm: "Lê Văn Quang", in: "LQ", s: "absent" },
  ];
  return (
    <div className="app-mock" role="img" aria-label="Giao diện KiteClass: màn hình điểm danh lớp Anh ngữ 5A1 và tổng quan học phí của Trung tâm Anh ngữ Sky Education">
      <div className="app-bar">
        <div className="dots"><i style={{ background: "#FF5F57" }}></i><i style={{ background: "#FEBC2E" }}></i><i style={{ background: "#28C840" }}></i></div>
        <span className="url">sky-education.kitehub.me</span>
      </div>
      <div className="app-body">
        <aside className="app-rail">
          <div className="brand"><Kite size={20} /> KiteClass</div>
          <div className="ri"><Icon.target /><span>Tổng quan</span></div>
          <div className="ri"><Icon.users /><span>Học viên</span></div>
          <div className="ri"><Icon.book /><span>Lớp &amp; khóa học</span></div>
          <div className="ri on"><Icon.check /><span>Điểm danh</span></div>
          <div className="ri"><Icon.award /><span>Điểm số</span></div>
          <div className="ri"><Icon.zap /><span>Học phí</span></div>
        </aside>
        <div className="app-content">
          <div className="app-h">
            <span className="t">Điểm danh hôm nay</span>
            <span className="d">Thứ Hai, 01/06/2026</span>
          </div>
          <div className="app-kpis">
            <div className="app-kpi"><div className="lab">Học viên</div><div className="val">214</div></div>
            <div className="app-kpi"><div className="lab">Có mặt hôm nay</div><div className="val green">96%</div></div>
            <div className="app-kpi"><div className="lab">Học phí T6 đã thu</div><div className="val blue">128.500.000đ</div></div>
          </div>
          <div className="app-card">
            <div className="ch">Lớp Anh ngữ 5A1 <span className="pill">18:00 · GVCN: cô Mai</span></div>
            {students.map((st) => (
              <div className="att-row" key={st.nm}>
                <span className="av">{st.in}</span>
                <span className="nm">{st.nm}</span>
                <span className={"st " + st.s}>
                  {st.s === "present" ? <><Icon.check size={13} /> Có mặt</> : <>Vắng (đã báo PH)</>}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
      <div className="float-badge fb1">
        <span className="ic" style={{ background: "rgb(22 163 74 / 0.14)", color: "rgb(22 122 56)" }}><Icon.zap size={16} /></span>
        <div>Học phí tự động<div className="sub">Hóa đơn gửi phụ huynh</div></div>
      </div>
      <div className="float-badge fb2">
        <span className="ic" style={{ background: "rgb(var(--primary-strong) / 0.12)", color: "rgb(var(--primary-strong))" }}><Icon.message size={16} /></span>
        <div>Phụ huynh xem ngay<div className="sub">Điểm danh &amp; điểm số</div></div>
      </div>
    </div>
  );
}

/* ---------- Hero ---------- */
function MsHero() {
  return (
    <section className="ms-hero">
      <div className="container">
        <div className="ms-hero-grid">
          <div>
            <span className="ms-hero-badge"><span className="dot"></span> Phiên bản Beta · Miễn phí cho trung tâm tham gia sớm</span>
            <h1>Quản lý trung tâm, <span className="hl">gọn trong một nền tảng</span></h1>
            <p className="lede">
              Thay Excel, Zalo và sổ giấy bằng một chỗ duy nhất. KiteClass giúp anh/chị
              điểm danh, tính học phí, vào điểm và cập nhật cho phụ huynh — nhanh, chính xác,
              chuyên nghiệp.
            </p>
            <div className="ms-hero-actions">
              <a href="#dang-ky" className="btn btn-cta btn-lg">Đăng ký Beta miễn phí <Icon.arrow size={20} /></a>
              <a href="#cach-hoat-dong" className="btn btn-on-dark btn-lg"><Icon.play size={18} /> Xem cách hoạt động</a>
            </div>
            <div className="ms-hero-trust">
              <span className="ti"><Icon.check size={18} /> Miễn phí trong giai đoạn Beta</span>
              <span className="ti"><Icon.check size={18} /> Không cần thẻ tín dụng</span>
              <span className="ti"><Icon.check size={18} /> Hỗ trợ tiếng Việt</span>
            </div>
          </div>
          <div style={{ position: "relative" }}>
            <AppMock />
          </div>
        </div>
      </div>
    </section>
  );
}

/* ---------- Problem → Solution ---------- */
function ProblemSolution() {
  const items = [
    { pain: "Điểm danh thủ công", h: "Mỗi buổi lại dò sổ, gọi tên", p: "Điểm danh bằng sổ giấy mất thời gian, dễ sót, cuối tháng khó tổng hợp số buổi học của từng em.", fix: "Điểm danh 1 chạm, tự tổng hợp theo lớp và theo tháng." },
    { pain: "Học phí dễ tính nhầm", h: "Tính tay trên Excel, sai là mất uy tín", p: "Học phí theo buổi, theo khóa, có nghỉ — tính tay dễ sai, vừa mất tiền vừa mất lòng tin của phụ huynh.", fix: "Tự tính học phí từ điểm danh, xuất hóa đơn rõ ràng." },
    { pain: "Phụ huynh hỏi liên tục", h: "Zalo nổ tin nhắn hỏi điểm, điểm danh", p: "Phụ huynh nhắn Zalo hỏi con đi học chưa, điểm thế nào, đóng tiền đến đâu — trả lời cả ngày không xuể.", fix: "Phụ huynh tự xem điểm danh, điểm số, học phí của con." },
  ];
  return (
    <section className="ms-sec soft">
      <div className="container">
        <div className="ms-head">
          <span className="eyebrow">Vấn đề quen thuộc</span>
          <h2>Vận hành trung tâm không nên vất vả đến vậy</h2>
          <p>Nếu anh/chị đang xoay xở giữa Excel, Zalo và sổ giấy, đây là 3 việc KiteClass gỡ rối ngay.</p>
        </div>
        <div className="prob-grid">
          {items.map((it) => (
            <div className="prob-card" key={it.pain}>
              <div className="pain"><span className="x"><Icon.message size={16} /></span> {it.pain}</div>
              <h3>{it.h}</h3>
              <p>{it.p}</p>
              <div className="fix"><Icon.check size={18} /> {it.fix}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ---------- Features (Phase 1 scope only — no overclaim) ---------- */
function Features() {
  const feats = [
    { ic: <Icon.check size={24} />, bg: "linear-gradient(135deg,#1D4ED8,#3B82F6)", h: "Điểm danh", p: "Điểm danh từng lớp chỉ với một chạm, tự tổng hợp số buổi học theo tháng cho từng học viên.", b: "Tiết kiệm thời gian mỗi buổi" },
    { ic: <Icon.zap size={24} />, bg: "linear-gradient(135deg,#C2410C,#F97316)", h: "Học phí & hóa đơn", p: "Tự tính học phí dựa trên điểm danh và khóa học, xuất hóa đơn điện tử rõ ràng gửi phụ huynh.", b: "Không còn tính nhầm" },
    { ic: <Icon.award size={24} />, bg: "linear-gradient(135deg,#7C3AED,#A855F7)", h: "Điểm số", p: "Nhập và lưu điểm theo lớp, theo kỳ. Phụ huynh và học viên xem được kết quả học tập minh bạch.", b: "Minh bạch với phụ huynh" },
    { ic: <Icon.book size={24} />, bg: "linear-gradient(135deg,#0E7490,#06B6D4)", h: "Lớp & khóa học", p: "Tạo lớp, xếp lịch, gán giáo viên và quản lý danh sách học viên của từng khóa ở một nơi.", b: "Mọi lớp trong tầm tay" },
    { ic: <Icon.users size={24} />, bg: "linear-gradient(135deg,#16A34A,#22C55E)", h: "Cổng phụ huynh", p: "Phụ huynh tự xem điểm danh, điểm số và học phí của con — giảm hẳn tin nhắn hỏi đáp mỗi ngày.", b: "Bớt trả lời Zalo cả ngày" },
    { ic: <Icon.graduation size={24} />, bg: "linear-gradient(135deg,#B45309,#F59E0B)", h: "Quản lý giáo viên", p: "Phân quyền cho giáo viên và quản lý, theo dõi ai dạy lớp nào, ai phụ trách điểm danh và vào điểm.", b: "Rõ ai làm việc gì" },
  ];
  return (
    <section className="ms-sec" id="tinh-nang">
      <div className="container">
        <div className="ms-head">
          <span className="eyebrow">Tính năng cốt lõi</span>
          <h2>Tất cả việc vận hành, trong một nền tảng</h2>
          <p>Sáu nhóm tính năng có sẵn trong giai đoạn Beta, thiết kế cho cách trung tâm Việt Nam thật sự làm việc.</p>
        </div>
        <div className="feat-grid">
          {feats.map((f) => (
            <article className="feat-card" key={f.h}>
              <div className="fi" style={{ background: f.bg }}>{f.ic}</div>
              <h3>{f.h}</h3>
              <p>{f.p}</p>
              <span className="benefit"><Icon.check size={15} /> {f.b}</span>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ---------- How it works ---------- */
function HowItWorks() {
  const steps = [
    { h: "Tạo lớp & nhập học viên", p: "Tạo lớp, xếp lịch và thêm danh sách học viên. Nhập nhanh từ file Excel sẵn có của trung tâm." },
    { h: "Mời giáo viên & phụ huynh", p: "Gửi lời mời cho giáo viên và phụ huynh qua email. Mỗi người có tài khoản với đúng quyền của mình." },
    { h: "Vận hành tự động", p: "Điểm danh, học phí và điểm số liên thông với nhau. Phụ huynh nhận cập nhật, anh/chị xem báo cáo tổng quan." },
  ];
  return (
    <section className="ms-sec soft" id="cach-hoat-dong">
      <div className="container">
        <div className="ms-head">
          <span className="eyebrow">Cách hoạt động</span>
          <h2>Bắt đầu trong ba bước</h2>
          <p>Không cần cài đặt phức tạp. Anh/chị có thể đưa trung tâm lên KiteClass ngay trong ngày đầu.</p>
        </div>
        <div className="steps-grid">
          {steps.map((s, i) => (
            <div className="step" key={i}>
              <div className="n">{i + 1}</div>
              <h3>{s.h}</h3>
              <p>{s.p}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ---------- Parent portal highlight ---------- */
function ParentBand() {
  return (
    <section className="ms-sec" id="phu-huynh">
      <div className="container">
        <div className="portal-band">
          <div>
            <span className="eyebrow" style={{ color: "#FFB570" }}>Cổng phụ huynh</span>
            <h2 style={{ marginTop: 10 }}>Phụ huynh yên tâm, trung tâm chuyên nghiệp hơn</h2>
            <p>Khi phụ huynh tự theo dõi được việc học của con, trung tâm vừa giảm tải tin nhắn, vừa tạo được niềm tin minh bạch.</p>
            <ul className="plist">
              <li><Icon.check size={20} /> Xem điểm danh và buổi học của con theo thời gian thực</li>
              <li><Icon.check size={20} /> Theo dõi điểm số và tiến độ học tập</li>
              <li><Icon.check size={20} /> Nhận hóa đơn học phí rõ ràng, không nhầm lẫn</li>
            </ul>
          </div>
          <div className="zalo-card" aria-label="Ví dụ thông báo gửi phụ huynh">
            <div className="zalo-head">
              <span className="av">SE</span>
              <div><div className="nm">Trung tâm Anh ngữ Sky Education</div><div className="sub">Thông báo cho phụ huynh</div></div>
            </div>
            <div className="zalo-msg">
              <span className="lab">Điểm danh · 01/06/2026</span>
              Em <strong>Trần Thị Hồng</strong> đã có mặt buổi học lớp <strong>Anh ngữ 5A1</strong> lúc 18:00.
            </div>
            <div className="zalo-msg">
              <span className="lab">Học phí tháng 6/2026</span>
              Học phí của em Hồng: <strong>1.500.000đ</strong>. Hạn đóng: <strong>10/06/2026</strong>.
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ---------- Trust / security ---------- */
function TrustSection() {
  const cards = [
    { ic: <Icon.shield size={22} />, h: "An toàn dữ liệu", p: "Dữ liệu học viên, phụ huynh và học phí được bảo mật, tuân thủ Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân." },
    { ic: <Icon.users size={22} />, h: "Đồng hành cùng Beta", p: "KiteClass đang mời các trung tâm tham gia Beta. Anh/chị được hỗ trợ trực tiếp và góp ý định hình sản phẩm." },
    { ic: <Icon.message size={22} />, h: "Hỗ trợ tận nơi", p: "Đội ngũ người Việt hỗ trợ qua điện thoại, email và Zalo trong suốt quá trình đưa trung tâm lên nền tảng." },
  ];
  return (
    <section className="ms-sec soft" id="an-toan">
      <div className="container">
        <div className="ms-head">
          <span className="eyebrow">Tin cậy &amp; minh bạch</span>
          <h2>Xây dựng cho trung tâm Việt Nam, bảo mật theo luật Việt Nam</h2>
        </div>
        <div className="trust-grid">
          {cards.map((c) => (
            <div className="trust-c" key={c.h}>
              <div className="ti">{c.ic}</div>
              <h3>{c.h}</h3>
              <p>{c.p}</p>
            </div>
          ))}
        </div>
        <div className="beta-note" role="note">
          <span className="ic"><Icon.shield size={20} /></span>
          <p><strong>Lưu ý về phiên bản Beta:</strong> KiteClass hiện đang trong giai đoạn Beta (thử nghiệm). Một số tính năng vẫn đang được hoàn thiện và có thể thay đổi. Chúng tôi mời anh/chị đồng hành sớm và rất mong nhận được góp ý để sản phẩm phù hợp hơn với trung tâm của anh/chị.</p>
        </div>
      </div>
    </section>
  );
}

/* ---------- Final CTA + beta form ---------- */
function FinalCTA() {
  const [persona, setPersona] = useS("P2_CENTER_OWNER");
  const [done, setDone] = useS(false);
  const personas = [
    { id: "P2_CENTER_OWNER", label: "Chủ trung tâm" },
    { id: "P1_SOLO_TEACHER", label: "Giáo viên tự do" },
    { id: "P3_MANAGER", label: "Quản lý" },
  ];
  return (
    <section className="cta-final" id="dang-ky">
      <div className="container">
        <div className="cta-grid">
          <div>
            <h2>Đưa trung tâm của anh/chị lên KiteClass</h2>
            <p className="lede">Đăng ký tham gia Beta miễn phí. Đội ngũ KiteClass sẽ liên hệ trong vòng 24 giờ để hỗ trợ anh/chị bắt đầu.</p>
            <ul className="checks">
              <li><Icon.check size={20} /> Miễn phí trong suốt giai đoạn Beta</li>
              <li><Icon.check size={20} /> Không cần thẻ tín dụng, không ràng buộc</li>
              <li><Icon.check size={20} /> Hỗ trợ chuyển dữ liệu từ Excel</li>
            </ul>
          </div>
          <div className="beta-form">
            {done ? (
              <div className="ok" role="status">
                <div className="big"><Icon.check size={28} /></div>
                <h3>Đã nhận đăng ký của anh/chị!</h3>
                <p>Cảm ơn anh/chị đã quan tâm KiteClass. Đội ngũ sẽ liên hệ trong vòng 24 giờ qua điện thoại hoặc email.</p>
              </div>
            ) : (
              <form onSubmit={(e) => { e.preventDefault(); setDone(true); }}>
                <h3>Đăng ký Beta miễn phí</h3>
                <p className="sub">Điền thông tin, chúng tôi sẽ liên hệ trong 24 giờ.</p>
                <div className="seg" role="group" aria-label="Anh/chị là">
                  {personas.map((p) => (
                    <button type="button" key={p.id} className={persona === p.id ? "on" : ""} onClick={() => setPersona(p.id)}>{p.label}</button>
                  ))}
                </div>
                <div className="field"><label htmlFor="bf-name">Họ và tên</label><input id="bf-name" placeholder="Trần Thị Hồng" required /></div>
                <div className="field"><label htmlFor="bf-org">Tên trung tâm</label><input id="bf-org" placeholder="Trung tâm Anh ngữ Sky Education" /></div>
                <div className="field"><label htmlFor="bf-phone">Số điện thoại</label><input id="bf-phone" type="tel" inputMode="tel" placeholder="0901 234 567" required /></div>
                <div className="field"><label htmlFor="bf-email">Email</label><input id="bf-email" type="email" placeholder="hong.tran@skyedu.vn" required /></div>
                <button type="submit" className="btn btn-cta btn-block btn-lg">Đăng ký Beta <Icon.arrow size={20} /></button>
                <p className="form-note">Bằng việc đăng ký, anh/chị đồng ý với <a href="#">Điều khoản sử dụng</a> và <a href="#">Chính sách bảo mật</a> của KiteClass.</p>
              </form>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

/* ---------- Footer ---------- */
function MsFooter() {
  return (
    <footer className="ms-footer">
      <div className="container ms-footer-grid">
        <div>
          <div className="brand"><Kite size={30} /> KiteClass</div>
          <p className="blurb">Nền tảng quản lý trung tâm và trường học tại Việt Nam: học viên, lớp học, điểm danh, điểm số và học phí — tất cả ở một nơi.</p>
          <p className="pub">Đơn vị phát hành: Công ty TNHH Công nghệ Giáo dục KiteClass<br />Hà Nội, Việt Nam · info@kitehub.me · 1900 6868</p>
        </div>
        <div>
          <h5>Sản phẩm</h5>
          <ul>
            <li><a href="#tinh-nang">Tính năng</a></li>
            <li><a href="#cach-hoat-dong">Cách hoạt động</a></li>
            <li><a href="#phu-huynh">Cổng phụ huynh</a></li>
            <li><a href="#dang-ky">Đăng ký Beta</a></li>
          </ul>
        </div>
        <div>
          <h5>Hỗ trợ</h5>
          <ul>
            <li><a href="#an-toan">An toàn dữ liệu</a></li>
            <li><a href="#dang-ky">Liên hệ tư vấn</a></li>
            <li><a href="#">Câu hỏi thường gặp</a></li>
            <li><a href="#">Hướng dẫn sử dụng</a></li>
          </ul>
        </div>
        <div>
          <h5>Pháp lý</h5>
          <ul>
            <li><a href="#">Điều khoản sử dụng</a></li>
            <li><a href="#">Chính sách bảo mật</a></li>
            <li><a href="#">Chính sách cookie</a></li>
            <li><a href="#">Bảo vệ dữ liệu (NĐ 13)</a></li>
          </ul>
        </div>
      </div>
      <div className="container ms-footer-bar">
        <span>© 2026 KiteClass. Mọi quyền được bảo lưu.</span>
        <span className="beta-mini"><span className="tag">Beta</span> Sản phẩm đang trong giai đoạn thử nghiệm</span>
      </div>
    </footer>
  );
}

/* ---------- Cookie consent (Nghị định 13 — hard gate) ---------- */
function CookieConsent() {
  const [show, setShow] = useS(false);
  useE(() => {
    try { if (!localStorage.getItem("kc_cookie_consent")) setShow(true); }
    catch (e) { setShow(true); }
  }, []);
  const decide = (v) => { try { localStorage.setItem("kc_cookie_consent", v); } catch (e) {} setShow(false); };
  if (!show) return null;
  return (
    <div className="cookie" role="dialog" aria-label="Thông báo cookie" aria-live="polite">
      <span className="ic"><Icon.shield size={20} /></span>
      <div className="txt">
        <strong>Chúng tôi sử dụng cookie.</strong> KiteClass dùng cookie cần thiết và cookie phân tích để cải thiện trải nghiệm,
        tuân thủ Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân. Xem <a href="#">Chính sách cookie</a>.
      </div>
      <div className="acts">
        <button className="btn btn-ghost btn-sm" onClick={() => decide("necessary")}>Chỉ cookie cần thiết</button>
        <button className="btn btn-cta btn-sm" onClick={() => decide("all")}>Đồng ý tất cả</button>
      </div>
    </div>
  );
}

Object.assign(window, {
  Kite, BetaStrip, MsNav, AppMock, MsHero, ProblemSolution, Features,
  HowItWorks, ParentBand, TrustSection, FinalCTA, MsFooter, CookieConsent,
});
