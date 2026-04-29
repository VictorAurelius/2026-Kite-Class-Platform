/**
 * kitehub-pro v2 — React port shape (Wave 1.5 add-on)
 *
 * This file documents the production port target. The actual screens ship as static
 * HTML in `screens/` for human review. Production port will land via separate gap
 * (Track 2 — see wave plan; will be filed as GAP-2XX after user accepts Round 2 quality).
 *
 * Stack constraints (per dossier 09-tech-constraints.md):
 *   Next.js 15 App Router · React 19 · TypeScript 5.7 · Tailwind 3.4 · shadcn/ui
 *   Radix primitives · lucide-react · Inter + JetBrains Mono · recharts (charts)
 *   Framer Motion (KH-only — KC has none)
 *   No Bootstrap/MUI/Chakra · No Redux/MobX · No moment/dayjs (use date-fns)
 *
 * KH custom shadcn extensions (4): gradient-button · gradient-text · page-header · section-title
 */

// =============================================================================
// 1. Tokens (mirror styles.css — for static type-safety in TS port)
// =============================================================================
export const KH_THEME = {
  primary: 'hsl(199 89% 48%)',   // sky blue
  accent:  'hsl(25 95% 53%)',    // orange
  success: 'hsl(160 84% 39%)',
  warning: 'hsl(38 92% 50%)',
  danger:  'hsl(0 84.2% 60.2%)',
};

// =============================================================================
// 2. Lifecycle state machine (per ai-branding-guidelines.md §6 — STRICT)
// =============================================================================
export type InstanceStatus =
  | 'NOT_STARTED'
  | 'INITIALIZING'
  | 'GENERATING'
  | 'DEPLOYED'
  | 'REGENERATING'
  | 'FAILED';

// State transitions enforced via InstanceLifecycleService — never set directly.
const ALLOWED_TRANSITIONS: Record<InstanceStatus, InstanceStatus[]> = {
  NOT_STARTED:  ['INITIALIZING'],
  INITIALIZING: ['GENERATING', 'FAILED'],
  GENERATING:   ['DEPLOYED', 'FAILED'],
  DEPLOYED:     ['REGENERATING'],
  REGENERATING: ['DEPLOYED', 'FAILED'],
  FAILED:       ['INITIALIZING'], // retry path
};

// =============================================================================
// 3. Resource categorization (per ai-branding-guidelines.md §1)
// =============================================================================
export type ResourceCategory = 'STATIC' | 'TEMPLATE' | 'FULL_AI';

// =============================================================================
// 4. Screen registry — maps each HTML screen to a production route
// =============================================================================
export const KH_SCREEN_REGISTRY = {
  // Dashboard hub (kh `/dashboard`)
  'dashboard-default':       { route: '/dashboard',                state: 'default'  },
  'dashboard-loading':       { route: '/dashboard',                state: 'loading'  },
  'dashboard-empty':         { route: '/dashboard',                state: 'empty'    },
  'dashboard-error':         { route: '/dashboard',                state: 'error'    },
  'dashboard-success':       { route: '/dashboard',                state: 'success'  },
  'dashboard-dark':          { route: '/dashboard',                state: 'dark'     },

  // Billing (kh `/billing/**`)
  'billing-default':         { route: '/billing',                  state: 'default'  },
  'billing-loading':         { route: '/billing',                  state: 'loading'  },
  'billing-empty':           { route: '/billing',                  state: 'empty'    },
  'billing-payment':         { route: '/billing/payment/[id]',     state: 'default'  },
  'billing-dark':            { route: '/billing',                  state: 'dark'     },

  // Branding hub (kh `/branding`)
  'branding-hub-default':    { route: '/branding',                 state: 'default'  },
  'branding-hub-loading':    { route: '/branding',                 state: 'loading'  },
  'branding-hub-quota-empty':{ route: '/branding',                 state: 'quota_empty' },
  'branding-hub-dark':       { route: '/branding',                 state: 'dark'     },

  // Branding wizard preview (kh `/branding/wizard`)
  // Full 6-step wizard is Wave 2 Direction C `ai-branding-wizard-v2` kit
  'branding-wizard-step1-welcome':         { route: '/branding/wizard', state: 'step-1' },
  'branding-wizard-step3-audience':        { route: '/branding/wizard', state: 'step-3' },
  'branding-wizard-step5-template':        { route: '/branding/wizard', state: 'step-5' },
  'branding-wizard-step6-preview-approve': { route: '/branding/wizard', state: 'step-6' },

  // Instance lifecycle (kh `/instances/[id]`) — 5 states
  'instance-NOT_STARTED':    { route: '/instances/[id]',           state: 'NOT_STARTED'  },
  'instance-GENERATING':     { route: '/instances/[id]',           state: 'GENERATING'   },
  'instance-DEPLOYED':       { route: '/instances/[id]',           state: 'DEPLOYED'     },
  'instance-FAILED':         { route: '/instances/[id]',           state: 'FAILED'       },
  'instance-REGENERATING':   { route: '/instances/[id]',           state: 'REGENERATING' },
} as const;

// =============================================================================
// 5. Mock data shape (VN-only — per dossier 02-vietnamese-ux-musts.md)
// =============================================================================
export const MOCK_TENANTS = [
  {
    id: 'tenant-mathmaster',
    name: 'Trung tâm Toán Master',
    slug: 'mathmaster',
    domain: 'mathmaster.kiteclass.app',
    owner: 'Nguyễn Văn An',
    phone: '0901 234 567',
    address: '123 Nguyễn Văn Cừ, P. Phước Long B, Q. 9, TP. HCM',
    students: 156,
    teachers: 12,
    classes: 18,
    status: 'DEPLOYED' as InstanceStatus,
    tier: 'PRO',
    deployedAt: '2026-03-31',
    template: 'sky-wave',
    qualityGateScore: 92,
  },
  {
    id: 'tenant-eduplus',
    name: 'Trường THCS-THPT EduPlus',
    slug: 'eduplus',
    domain: 'eduplus.kiteclass.app',
    owner: 'Trần Thị Hương',
    students: 72,
    teachers: 4,
    classes: 6,
    status: 'DEPLOYED' as InstanceStatus,
    tier: 'PRO',
  },
  {
    id: 'tenant-vietanh',
    name: 'Trung tâm Anh ngữ Việt-Anh',
    slug: 'vietanh',
    domain: 'vietanh.kiteclass.app',
    owner: 'Lê Minh Tuấn',
    status: 'REGENERATING' as InstanceStatus,
    tier: 'PRO',
  },
];

// =============================================================================
// 6. Sample component shape — Stat card with sparkline
// =============================================================================
//
// Production port uses recharts (KH-only). For HTML mocks we used inline SVG.
//
// import { Card } from '@/components/ui/card';
// import { TrendingUp } from 'lucide-react';
// import { Sparklines, SparklinesLine } from 'react-sparklines';
//
// export function StatCard({ label, value, delta, sparkData, color }: StatCardProps) {
//   return (
//     <Card className="p-5 rounded-2xl shadow-soft hover:shadow-soft-lg transition-shadow">
//       <div className="text-sm text-muted-foreground flex items-center gap-2">
//         <TrendingUp className="w-4 h-4" />
//         {label}
//       </div>
//       <div className="flex items-baseline gap-2 mt-2">
//         <div className="text-3xl font-bold tracking-tight">{value}</div>
//         <DeltaBadge delta={delta} />
//       </div>
//       <Sparklines data={sparkData} height={32}>
//         <SparklinesLine color={color} style={{ fillOpacity: 0.12 }} />
//       </Sparklines>
//     </Card>
//   );
// }

// =============================================================================
// 7. Sample component shape — Lifecycle progress (state machine visualisation)
// =============================================================================
//
// import { motion } from 'framer-motion'; // KH-only
// import { Check, ArrowRight } from 'lucide-react';
//
// export function LifecycleTrack({ current }: { current: InstanceStatus }) {
//   const steps = ['NOT_STARTED', 'INITIALIZING', 'GENERATING', 'DEPLOYED'] as const;
//   const currentIdx = steps.indexOf(current);
//   return (
//     <div className="flex items-center gap-2 p-4 rounded-2xl bg-muted/50 overflow-x-auto">
//       {steps.map((step, i) => (
//         <Fragment key={step}>
//           <motion.span
//             className={cn(
//               'lifecycle-step',
//               i < currentIdx && 'lifecycle-step--done',
//               i === currentIdx && 'lifecycle-step--active',
//               i > currentIdx && 'lifecycle-step--idle',
//             )}
//             initial={{ opacity: 0, x: -10 }}
//             animate={{ opacity: 1, x: 0 }}
//             transition={{ delay: i * 0.05 }}
//           >
//             {step}
//           </motion.span>
//           {i < steps.length - 1 && <ArrowRight className="w-4 h-4 text-muted-foreground" />}
//         </Fragment>
//       ))}
//     </div>
//   );
// }

// =============================================================================
// 8. Sample component shape — Quality gate widget (5 checks /100)
// =============================================================================
//
// per ai-branding-guidelines.md §5 — must show before DEPLOY
// Check 1: contrast WCAG AA · 2: CSS vars · 3: 404 URLs · 4: visual diff · 5: logo
//
// export function QualityGateWidget({ checks }: { checks: QualityCheck[] }) {
//   const score = computeScore(checks);
//   const passed = score >= 70;
//   return (
//     <Card className="p-5 stack-3">
//       <div className="flex items-center justify-between">
//         <h3 className="font-semibold">Quality Gate</h3>
//         <Badge variant={passed ? 'success' : 'destructive'}>
//           {score} / 100 · {passed ? 'PASS' : 'FAIL'}
//         </Badge>
//       </div>
//       <Progress value={score} />
//       {checks.map(c => <GateCheckRow key={c.id} {...c} />)}
//     </Card>
//   );
// }

// =============================================================================
// 9. Anti-patterns to avoid in production port
// =============================================================================
//
// ❌ Free-form prompt entry (per ai-branding-guidelines.md §2.1)
// ❌ Direct AIClient.generate() in controller (must go through Analyzer→Planner→Executor)
// ❌ Sync HTTP for AI generation (must use RabbitMQ ai.generate.{tier} queue)
// ❌ Auto-deploy after generate (must show preview + per-resource approve)
// ❌ Bypass InstanceLifecycleService for status changes
// ❌ Color hex literals in components (use HSL CSS vars)
// ❌ Lorem ipsum / John Doe / $ currency / MM/dd/yyyy date format
//
// ✅ Always: VN names, đ currency, dd/MM/yyyy, AA contrast measurement, dark mode parity
