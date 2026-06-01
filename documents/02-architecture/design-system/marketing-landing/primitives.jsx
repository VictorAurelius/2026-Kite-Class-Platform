/* =============================================================
   KiteClass Landing — Primitives
   Logo, icon set (Lucide-style, 2px stroke), theme switcher,
   count-up hook, section header. Shared by both landing kits.
   Exposed on window for cross-file Babel scope.
   ============================================================= */
const { useState, useEffect, useRef } = React;

/* ---------- Icons (mirror lucide-react stroke style: 24 grid, 2px, round) ---------- */
function Ico({ d, paths, size = 20, stroke = 2, fill = "none", ...rest }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={fill}
      stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
      aria-hidden="true" {...rest}>
      {d ? <path d={d}></path> : paths}
    </svg>
  );
}
const Icon = {
  check:   (p) => <Ico {...p} d="M20 6 9 17l-5-5" />,
  arrow:   (p) => <Ico {...p} paths={<><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></>} />,
  phone:   (p) => <Ico {...p} d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z" />,
  mail:    (p) => <Ico {...p} paths={<><rect x="2" y="4" width="20" height="16" rx="2"></rect><path d="m22 7-10 5L2 7"></path></>} />,
  pin:     (p) => <Ico {...p} paths={<><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"></path><circle cx="12" cy="10" r="3"></circle></>} />,
  clock:   (p) => <Ico {...p} paths={<><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></>} />,
  calendar:(p) => <Ico {...p} paths={<><rect x="3" y="4" width="18" height="18" rx="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></>} />,
  users:   (p) => <Ico {...p} paths={<><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></>} />,
  chevron: (p) => <Ico {...p} d="m6 9 6 6 6-6" />,
  star:    (p) => <Ico {...p} fill="currentColor" stroke="none" d="M12 2 15.09 8.26 22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />,
  shield:  (p) => <Ico {...p} paths={<><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><path d="m9 12 2 2 4-4"></path></>} />,
  award:   (p) => <Ico {...p} paths={<><circle cx="12" cy="8" r="6"></circle><path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"></path></>} />,
  book:    (p) => <Ico {...p} d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V2H6.5A2.5 2.5 0 0 0 4 4.5v15zM4 19.5V22h14" />,
  zap:     (p) => <Ico {...p} fill="currentColor" stroke="none" d="M13 2 3 14h7l-1 8 10-12h-7l1-8z" />,
  play:    (p) => <Ico {...p} fill="currentColor" stroke="none" d="M8 5v14l11-7z" />,
  target:  (p) => <Ico {...p} paths={<><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle></>} />,
  graduation:(p) => <Ico {...p} paths={<><path d="M22 10v6M2 10l10-5 10 5-10 5z"></path><path d="M6 12v5c3 3 9 3 12 0v-5"></path></>} />,
  message: (p) => <Ico {...p} d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />,
  heart:   (p) => <Ico {...p} fill="currentColor" stroke="none" d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 1 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z" />,
};
function Stars({ n = 5 }) {
  return <div className="testi-stars" aria-label={n + " sao"}>{"★".repeat(n)}{"☆".repeat(5 - n)}</div>;
}

/* ---------- Kite logo (inline, picks up theme-primary) ---------- */
function KiteLogo({ name = "KiteClass", accentWord }) {
  // split name → first word coloured, rest dark
  const parts = accentWord ? [accentWord, name.replace(accentWord, "")] : [name, ""];
  return (
    <a href="#" className="nav-logo" onClick={(e) => e.preventDefault()}>
      <svg className="mark" viewBox="0 0 48 48" fill="none" aria-hidden="true">
        <path d="M24 4L40 24L24 44L8 24L24 4Z" fill="rgb(var(--theme-primary))"></path>
        <path d="M24 4V44M8 24H40" stroke="white" strokeWidth="2" strokeOpacity="0.3"></path>
        <circle cx="24" cy="24" r="4" fill="rgb(var(--theme-cta))"></circle>
      </svg>
      <span className="name"><span className="accent">{parts[0]}</span>{parts[1]}</span>
    </a>
  );
}

/* ---------- count-up (animates when scrolled into view) ---------- */
function useCountUp(target, { duration = 1400, decimals = 0 } = {}) {
  const ref = useRef(null);
  const [val, setVal] = useState(0);
  useEffect(() => {
    const el = ref.current; if (!el) return;
    let raf, started = false;
    const run = () => {
      const t0 = performance.now();
      const tick = (now) => {
        const p = Math.min(1, (now - t0) / duration);
        const eased = 1 - Math.pow(1 - p, 3);
        setVal(target * eased);
        if (p < 1) raf = requestAnimationFrame(tick);
      };
      raf = requestAnimationFrame(tick);
    };
    const io = new IntersectionObserver((ents) => {
      ents.forEach((e) => { if (e.isIntersecting && !started) { started = true; run(); } });
    }, { threshold: 0.4 });
    io.observe(el);
    return () => { io.disconnect(); cancelAnimationFrame(raf); };
  }, [target, duration]);
  const display = decimals ? val.toFixed(decimals) : Math.round(val).toLocaleString("vi-VN");
  return [ref, display];
}
function Counter({ value, suffix, label, icon, decimals }) {
  const [ref, display] = useCountUp(value, { decimals });
  return (
    <div className="counter-cell">
      {icon && <span className="ico">{icon}</span>}
      <div className="counter-num" ref={ref}>{display}<span className="suffix">{suffix}</span></div>
      <div className="label">{label}</div>
    </div>
  );
}

/* ---------- Section header ---------- */
function SectionHead({ eyebrow, title, sub, align = "center" }) {
  return (
    <div className={"sec-head" + (align === "left" ? " left" : "")}>
      {eyebrow && <span className="eyebrow">{eyebrow}</span>}
      <h2 className="h2">{title}</h2>
      {sub && <p>{sub}</p>}
    </div>
  );
}

/* ---------- Theme switcher (demo only) ---------- */
const THEMES = [
  { id: "default", label: "KiteClass (mặc định)", cls: "", sw: ["#3B82F6", "#8B5CF6", "#F59E0B", "#F97316"] },
  { id: "ha",      label: "Cô Hà — Xanh dương",   cls: "theme-ha",    sw: ["#2563EB", "#3B82F6", "#0EA5E9", "#F97316"] },
  { id: "khanh",   label: "Cô Khánh — Navy + Gold", cls: "theme-khanh", sw: ["#1E3A5F", "#2D4E78", "#C9A227", "#C9A227"] },
  { id: "nhi",     label: "Thầy Nhì — Xanh lá",   cls: "theme-nhi",   sw: ["#16A34A", "#059669", "#FACC15", "#F97316"] },
];
function ThemeSwitcher({ value, onChange }) {
  return (
    <div className="theme-switch">
      <div className="ts-title">Chủ đề theo giáo viên</div>
      {THEMES.map((t) => (
        <button key={t.id} className={"ts-opt" + (value === t.id ? " on" : "")} onClick={() => onChange(t.id)}>
          <span className="sw">{t.sw.map((c, i) => <i key={i} style={{ background: c }}></i>)}</span>
          {t.label}
        </button>
      ))}
    </div>
  );
}
function themeClass(id) { return (THEMES.find((t) => t.id === id) || THEMES[0]).cls; }

Object.assign(window, { Icon, Stars, KiteLogo, useCountUp, Counter, SectionHead, ThemeSwitcher, THEMES, themeClass });
