/* eslint-disable */
/**
 * ai-branding-wizard-v2 — React/JSX skeleton sketch (NOT runnable in repo)
 *
 * Wave UI Kits Round 2 · 1.7 add-on · Direction C 6-step refactor
 * Per ai-branding-guidelines.md §4.1 wizard pattern + §4.2 preview + §4.3 regen counter
 *                              + §2.4 ENTERPRISE Advanced Mode + §5 quality gate
 *
 * This file documents the component breakdown a future Track 2 production port
 * (Next.js 15 / React 19 / Tailwind / shadcn / Radix / Framer Motion) would land.
 * Do not import this file from the actual production frontend — HTML prototypes in
 * screens/ are the source of truth for vibe-check review.
 *
 * Component tree:
 *
 *   <BrandingWizard tenantId, tier>
 *     ├─ <WizardTopbar tier, timeEstimate />
 *     ├─ <WizardStepper steps, currentStep />
 *     ├─ <AnimatePresence>                            // Framer Motion KH-only
 *     │    └─ {currentStep === 1 && <WelcomeStep />}
 *     │    {currentStep === 2 && <LogoStep />}
 *     │    {currentStep === 3 && <AudienceStep />}
 *     │    {currentStep === 4 && <ToneStep />}
 *     │    {currentStep === 5 && <TemplateStep mode="standard|advanced" />}
 *     │    {currentStep === 6 && <PreviewStep />}
 *     ├─ <WizardFooter onBack onNext disabled />
 *     └─ <Toaster />                                  // Sonner
 *
 * State machine (XState Wizard FE pattern per ai-branding-guidelines.md §10):
 *   IDLE → STEP1 → STEP2 → STEP3 → STEP4 → STEP5 → STEP6 → DEPLOYING → DONE
 *                        ↘ STEP_BACK on each step
 *
 * Lifecycle G9 hook:
 *   useInstanceLifecycle(instanceId) → SSE subscribe to `instance.lifecycle.{instanceId}`
 *     events: NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING / FAILED
 *
 * AnalyzerService → PlannerService → PlanExecutor — backend triple from
 * ai-branding-guidelines.md §3.1 (NOT direct AIClient call from FE).
 */

import { motion, AnimatePresence } from "framer-motion";
import { Toaster, toast } from "sonner";
import { useReducer, useEffect } from "react";
import { useInstanceLifecycle } from "@/hooks/useInstanceLifecycle";
import { useRegenQuota } from "@/hooks/useRegenQuota";

const TIER_REGEN_LIMITS = {
  FREE: 3,
  PRO: 10,
  PREMIUM: 30,
  ENTERPRISE: -1, // -1 = unlimited per §4.3
};

// Per §2.5 — input cap by tier
const TIER_INPUT_TOKEN_CAP = {
  FREE: 2000,
  BASIC: 4000,
  PRO: 4000,
  PREMIUM: 8000,
  ENTERPRISE: 16000,
};

// 6 steps as defined in dossier/08 Decision 4
const STEPS = [
  { id: 1, key: "welcome",    title: "Chào mừng",     component: WelcomeStep   },
  { id: 2, key: "logo",       title: "Logo",          component: LogoStep      },
  { id: 3, key: "audience",   title: "Đối tượng",     component: AudienceStep  },
  { id: 4, key: "tone",       title: "Phong cách",    component: ToneStep      },
  { id: 5, key: "template",   title: "Mẫu thiết kế",  component: TemplateStep  },
  { id: 6, key: "preview",    title: "Phê duyệt",     component: PreviewStep   },
];

// 4 audience cards (constrained presets per §2.1 — no free-form)
const AUDIENCES = [
  { id: "preschool",  emoji: "🏫", title: "Trường mầm non",        desc: "Trẻ 2-6 tuổi. Phụ huynh ra quyết định." },
  { id: "k12",        emoji: "📚", title: "Trường THCS / THPT",    desc: "Học sinh 11-18 tuổi. Tone học thuật." },
  { id: "english",    emoji: "🌐", title: "Trung tâm tiếng Anh",   desc: "Đa độ tuổi. Tone hiện đại quốc tế." },
  { id: "examprep",   emoji: "🎓", title: "Lớp luyện thi",         desc: "Lớp 9, 12. Tone tập trung kết quả." },
];

const TONES = [
  { id: "pro",    emoji: "💼", title: "Chuyên nghiệp", className: "tone-pro"    },
  { id: "friend", emoji: "😊", title: "Thân thiện",    className: "tone-friend" },
  { id: "energy", emoji: "⚡", title: "Năng động",     className: "tone-energy" },
  { id: "luxe",   emoji: "✨", title: "Sang trọng",    className: "tone-luxe"   },
];

// 6 templates per §2.2 ("always provide 6+ template previews")
const TEMPLATES = [
  { code: "T1", name: "Navy Focus",        tags: ["pro", "examprep", "k12"] },
  { code: "T2", name: "Score Board",       tags: ["pro", "examprep"] },
  { code: "T3", name: "Coach Card",        tags: ["pro", "luxe"] },
  { code: "T4", name: "Result Stripes",    tags: ["pro", "energy"] },
  { code: "T5", name: "Schedule Grid",     tags: ["pro", "energy", "k12"] },
  { code: "T6", name: "Roadmap Vertical",  tags: ["pro", "examprep"] },
];

// Reducer for wizard state (XState equivalent)
function wizardReducer(state, action) {
  switch (action.type) {
    case "GO_NEXT":   return { ...state, step: Math.min(state.step + 1, 6) };
    case "GO_BACK":   return { ...state, step: Math.max(state.step - 1, 1) };
    case "SET_FIELD": return { ...state, [action.field]: action.value };
    case "TOGGLE_RESOURCE":
      return { ...state, approve: { ...state.approve, [action.resource]: !state.approve[action.resource] } };
    case "RESET":     return initialWizardState;
    default:          return state;
  }
}

const initialWizardState = {
  step: 1,
  tenantName: "",
  slug: "",
  slugStatus: "idle", // idle | checking | ok | bad
  logoMode: null,     // upload | ai-generate
  logoFile: null,
  audience: null,     // preschool | k12 | english | examprep
  tone: null,         // pro | friend | energy | luxe
  templateCode: null, // T1..T6
  customPrompt: "",   // ENTERPRISE only
  approve: { logo: true, palette: true, banner: true, hero: true },
};

// ============================================================
// Component sketches
// ============================================================

export function BrandingWizard({ tenantId, tier = "FREE", advancedModeEnabled = false }) {
  const [state, dispatch] = useReducer(wizardReducer, initialWizardState);
  const { regenLeft, regenLimit, useRegen } = useRegenQuota(tier);
  const { lifecycle, progress, log } = useInstanceLifecycle(tenantId);

  // Slide transition per Wave 1 brief (300ms ease-out)
  const stepVariants = {
    enter:  { opacity: 0, x:  40 },
    center: { opacity: 1, x:   0 },
    exit:   { opacity: 0, x: -40 },
  };

  const StepComponent = STEPS.find(s => s.id === state.step).component;

  return (
    <main className="wiz-shell">
      <WizardTopbar tier={tier} timeEstimate={`Còn ~${6 - state.step} phút`} />
      <WizardStepper steps={STEPS} currentStep={state.step} />

      <section className="wiz-main">
        <AnimatePresence mode="wait">
          <motion.div
            key={state.step}
            variants={stepVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.3, ease: "easeOut" }}
          >
            <StepComponent
              state={state}
              dispatch={dispatch}
              tier={tier}
              advancedModeEnabled={advancedModeEnabled}
              regenLeft={regenLeft}
              regenLimit={regenLimit}
              lifecycle={lifecycle}
              progress={progress}
              log={log}
            />
          </motion.div>
        </AnimatePresence>
      </section>

      <WizardFooter
        canGoBack={state.step > 1}
        canGoNext={validateStep(state)}
        onBack={() => dispatch({ type: "GO_BACK" })}
        onNext={() => dispatch({ type: "GO_NEXT" })}
        progress={`${state.step} / 6`}
      />
      <Toaster richColors position="bottom-right" />
    </main>
  );
}

// ----- Step 1: Welcome -----
function WelcomeStep({ state, dispatch }) {
  // Slug live availability check (debounced 400ms)
  useEffect(() => {
    if (!state.slug) return;
    dispatch({ type: "SET_FIELD", field: "slugStatus", value: "checking" });
    const t = setTimeout(async () => {
      // FE → /api/v1/branding/slug-check?slug=X — returns { available: bool, suggestions: [] }
      // Mock for prototype:
      const taken = state.slug === "toan-master";
      dispatch({ type: "SET_FIELD", field: "slugStatus", value: taken ? "bad" : "ok" });
    }, 400);
    return () => clearTimeout(t);
  }, [state.slug]);

  return (
    <>
      <div className="wiz-eyebrow">Bước 1 / 6</div>
      <h1 className="wiz-title">Chào mừng đến với Kite Branding Studio</h1>
      <p className="wiz-subtitle">
        Hệ thống AI sẽ tạo trang web cho trung tâm của bạn dựa trên 4 lựa chọn nhỏ.
      </p>
      {/* tenant name input + slug-row + suggestions list */}
      {/* ... */}
    </>
  );
}

// ----- Step 5: Template (with optional ENTERPRISE custom-prompt) -----
function TemplateStep({ state, dispatch, tier, advancedModeEnabled }) {
  const showCustomPrompt = tier === "ENTERPRISE" && advancedModeEnabled;
  const filtered = TEMPLATES.filter(t => t.tags.includes(state.tone));
  // §2.2 — always show 6+ — pad if filtered < 6
  const display = filtered.length >= 6 ? filtered : TEMPLATES;

  return (
    <>
      {showCustomPrompt && (
        <div className="advanced-banner">
          <strong>Advanced Mode đang bật</strong> — bạn có thể nhập custom-prompt 200 ký tự.
        </div>
      )}
      <div className="wiz-eyebrow">Bước 5 / 6</div>
      <h1 className="wiz-title">Chọn mẫu thiết kế</h1>

      {showCustomPrompt && (
        <textarea
          className="wiz-input"
          maxLength={200}
          value={state.customPrompt}
          onChange={(e) => dispatch({ type: "SET_FIELD", field: "customPrompt", value: e.target.value })}
          placeholder="VD: Giọng văn ấm áp như mầm non nhưng tone luyện thi..."
        />
      )}

      <div className="tpl-grid">
        {display.map((t) => (
          <motion.article
            key={t.code}
            className={`tpl-card ${state.templateCode === t.code ? "is-selected" : ""}`}
            whileHover={{ scale: 1.015, y: -4 }}
            onClick={() => dispatch({ type: "SET_FIELD", field: "templateCode", value: t.code })}
          >
            <div className="tpl-thumb">
              <TemplatePreviewSvg code={t.code} />
            </div>
            <div className="tpl-meta">
              <div className="tpl-name">{t.name}</div>
              <div className="tpl-tag">{t.code}</div>
            </div>
          </motion.article>
        ))}
      </div>
    </>
  );
}

// ----- Step 6: Preview + per-resource approve + quality gate + regen counter -----
function PreviewStep({ state, dispatch, tier, regenLeft, regenLimit, lifecycle, progress, log }) {
  const [qReport, setQReport] = useState({ score: 95, checks: [...] });

  return (
    <>
      <div className="wiz-eyebrow">Bước 6 / 6 — Cuối cùng!</div>
      <h1 className="wiz-title">Xem trước trang web của bạn</h1>

      <div className="preview-grid">
        <div>
          <PreviewFrame
            url={`https://${state.slug}.kitehub.me`}
            theme={buildThemeFromState(state)}
            assets={state.approve}
          />
        </div>

        <div className="space-y-4">
          {/* Quality gate widget — §5 */}
          <QualityGateWidget report={qReport} />

          {/* Per-resource approve — §4.2 */}
          <ApproveStack
            approve={state.approve}
            onToggle={(resource) => dispatch({ type: "TOGGLE_RESOURCE", resource })}
          />

          {/* Regenerate counter — §4.3 */}
          <RegenBar
            tier={tier}
            left={regenLeft}
            limit={regenLimit}
            onRegen={() => {
              if (regenLeft <= 0) toast.warning("Đã hết quota — nâng cấp PRO?");
              else useRegen();
            }}
          />

          {/* Lifecycle progress when DEPLOYING */}
          {lifecycle !== "NOT_STARTED" && (
            <LifecycleStages stages={lifecycle} progress={progress} log={log} />
          )}
        </div>
      </div>
    </>
  );
}

// ----- Quality gate widget (§5 mandate) -----
function QualityGateWidget({ report }) {
  const passed = report.score >= 70;
  return (
    <div className={`qgate ${passed ? "pass" : "fail"}`}>
      <div className="qgate-head">
        <div>
          <p className="text-xs uppercase tracking-wider muted font-semibold">Điểm chất lượng</p>
          <p className="qgate-score">{report.score}<span className="qgate-score-out">/100</span></p>
        </div>
        <span className={passed ? "px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold"
                                 : "px-2.5 py-1 rounded-full bg-red-100 text-red-800 text-xs font-bold"}>
          {passed ? "PASS" : "FAIL"}
        </span>
      </div>
      <div className="qgate-bar"><span style={{ width: `${report.score}%` }} /></div>
      <div className="qgate-checks">
        {report.checks.map((c) => (
          <div key={c.id} className={`qgate-check ${c.ok ? "ok" : "fail"}`}>{c.label}</div>
        ))}
      </div>
    </div>
  );
}

// ============================================================
// Hooks (sketches — backed by services in production)
// ============================================================

// useInstanceLifecycle — SSE subscribe per §3.3 / §6
//   GET /api/v1/branding/{instanceId}/events  (text/event-stream)
//   yields: { state: "GENERATING", progress: 47, log: [...] }
//
// useRegenQuota — per-tier counter from §4.3
//   { regenLeft: 2, regenLimit: 3, useRegen: fn, isUnlimited: tier === "ENTERPRISE" }

// ============================================================
// Validation per step (gates Next button)
// ============================================================
function validateStep(state) {
  switch (state.step) {
    case 1: return state.tenantName.length >= 2 && state.slugStatus === "ok";
    case 2: return state.logoMode !== null;        // either upload OR ai-generate
    case 3: return state.audience !== null;
    case 4: return state.tone !== null;
    case 5: return state.templateCode !== null;
    case 6: return Object.values(state.approve).some(Boolean);  // ≥1 resource approved
    default: return true;
  }
}

// ============================================================
// Backend integration contract (production port)
// ============================================================
//
// POST /api/v1/branding/start            → { instanceId }
// POST /api/v1/branding/{id}/wizard      body: { step, payload } — saves draft
// POST /api/v1/branding/{id}/generate    body: full state → enqueues ai.generate.{tier}
// GET  /api/v1/branding/{id}/package     → composite per §7.1 — theme + assets + ETag
// GET  /api/v1/branding/{id}/events      SSE stream — lifecycle transitions
// POST /api/v1/branding/{id}/regenerate  body: { resource } — partial regen
// POST /api/v1/branding/{id}/approve     body: approve={} — final commit → DEPLOYED
//
// All AI input goes through AIInputCapService#checkInputSize(tier, ...inputs) per §2.5.
// No FE calls AIClient directly — must go via backend AnalyzerService → PlannerService → PlanExecutor (§3.1).
</content>
</invoke>
