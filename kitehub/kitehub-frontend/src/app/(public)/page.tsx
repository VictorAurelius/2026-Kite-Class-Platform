'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import {
  Users,
  GraduationCap,
  BookOpen,
  CheckCircle,
  CreditCard,
  Palette,
  ArrowRight,
  Star,
  Sparkles,
  BarChart3,
  Building2,
  Clock,
  Shield,
  ChevronDown,
  Phone,
  Mail,
  MapPin,
} from 'lucide-react';
import { useState } from 'react';
import { GradientButton } from '@/components/ui/gradient-button';
import { SectionTitle } from '@/components/ui/section-title';

// ============================================================
// DATA
// ============================================================

const features = [
  {
    icon: Users,
    title: 'Quản lý học viên',
    desc: 'Hồ sơ chi tiết, theo dõi tiến độ, bảng điểm tự động.',
    color: 'text-blue-500 bg-blue-50',
  },
  {
    icon: Clock,
    title: 'Lịch học & Điểm danh',
    desc: 'Tạo lịch, điểm danh tự động, thông báo cho phụ huynh.',
    color: 'text-green-500 bg-green-50',
  },
  {
    icon: CreditCard,
    title: 'Thanh toán & Hóa đơn',
    desc: 'Quản lý học phí, tạo hóa đơn, nhắc thanh toán tự động.',
    color: 'text-orange-500 bg-orange-50',
  },
  {
    icon: Palette,
    title: 'AI Branding',
    desc: 'Tạo website & thương hiệu tự động bằng trí tuệ nhân tạo.',
    color: 'text-purple-500 bg-purple-50',
  },
  {
    icon: BarChart3,
    title: 'Báo cáo & Thống kê',
    desc: 'Dashboard trực quan, báo cáo doanh thu, phân tích học viên.',
    color: 'text-pink-500 bg-pink-50',
  },
  {
    icon: Building2,
    title: 'Đa chi nhánh',
    desc: 'Quản lý nhiều cơ sở từ 1 tài khoản, phân quyền nhân viên.',
    color: 'text-teal-500 bg-teal-50',
  },
];

const steps = [
  { num: '01', title: 'Đăng ký miễn phí', desc: 'Tạo tài khoản trong 30 giây, không cần thẻ tín dụng.' },
  { num: '02', title: 'Cấu hình trung tâm', desc: 'Thêm khóa học, giáo viên, lịch học. AI hỗ trợ tạo website.' },
  { num: '03', title: 'Bắt đầu sử dụng', desc: 'Quản lý học viên, điểm danh, thu học phí ngay lập tức.' },
];

const testimonials = [
  {
    name: 'Nguyễn Thị Minh Anh',
    role: 'Giám đốc Trung tâm Anh ngữ SkyLight',
    text: 'Trước đây tôi quản lý bằng Excel, mất cả ngày. KiteClass giúp tôi tiết kiệm 3 tiếng mỗi ngày và không bao giờ quên ghi điểm danh nữa.',
    rating: 5,
  },
  {
    name: 'Trần Văn Đức',
    role: 'Chủ lớp Toán tư duy MathGenius',
    text: 'Phụ huynh rất hài lòng khi nhận thông báo tự động về tiến độ học của con. Tỷ lệ giữ chân học viên tăng 40% sau 3 tháng dùng KiteClass.',
    rating: 5,
  },
  {
    name: 'Lê Hoàng Phương',
    role: 'Trưởng phòng đào tạo, Trung tâm IT Academy',
    text: 'Chức năng AI Branding tạo website cho trung tâm chỉ trong 5 phút. Trông rất chuyên nghiệp, phụ huynh ấn tượng ngay từ lần đầu truy cập.',
    rating: 5,
  },
];

const pricingTiers = [
  {
    name: 'FREE',
    price: 'Miễn phí',
    period: '',
    desc: 'Dùng thử đầy đủ tính năng trong 14 ngày',
    features: ['Tối đa 30 học viên', '1 giáo viên', '3 khóa học', 'Điểm danh cơ bản', 'Hỗ trợ email'],
    cta: 'Bắt đầu miễn phí',
    popular: false,
  },
  {
    name: 'BASIC',
    price: '199.000đ',
    period: '/tháng',
    desc: 'Cho giáo viên độc lập và lớp nhỏ',
    features: ['Tối đa 100 học viên', '5 giáo viên', '10 khóa học', 'Thanh toán & hóa đơn', 'Báo cáo cơ bản', 'Hỗ trợ chat'],
    cta: 'Dùng thử 14 ngày',
    popular: false,
  },
  {
    name: 'PREMIUM',
    price: '399.000đ',
    period: '/tháng',
    desc: 'Cho trung tâm vừa và lớn',
    features: ['Không giới hạn học viên', 'Không giới hạn giáo viên', 'AI Branding', 'Đa chi nhánh', 'Báo cáo nâng cao', 'API tích hợp', 'Hỗ trợ ưu tiên'],
    cta: 'Dùng thử 14 ngày',
    popular: true,
  },
  {
    name: 'ENTERPRISE',
    price: 'Liên hệ',
    period: '',
    desc: 'Giải pháp tùy chỉnh cho trường học lớn',
    features: ['Tất cả tính năng Premium', 'SLA 99.9%', 'Server riêng', 'Tùy chỉnh giao diện', 'Đào tạo nhân viên', 'Quản lý chuyên biệt'],
    cta: 'Liên hệ tư vấn',
    popular: false,
  },
];

const faqs = [
  { q: 'Trial 14 ngày có giới hạn gì không?', a: 'Không. Bạn được dùng tất cả tính năng của gói PREMIUM trong 14 ngày. Không cần thẻ tín dụng.' },
  { q: 'Thanh toán bằng hình thức nào?', a: 'Chuyển khoản ngân hàng (QR code VietQR), Momo, ZaloPay. Hóa đơn VAT cho doanh nghiệp.' },
  { q: 'Có thể nâng cấp hoặc hạ gói giữa chừng không?', a: 'Có. Bạn có thể thay đổi gói bất kỳ lúc nào. Chi phí được tính theo ngày sử dụng.' },
  { q: 'Dữ liệu có an toàn không?', a: 'Dữ liệu được mã hóa AES-256, lưu trên AWS Singapore. Sao lưu tự động hàng ngày. Tuân thủ quy định bảo vệ dữ liệu.' },
];

// ============================================================
// ANIMATION VARIANTS
// ============================================================

const fadeInUp = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6 } },
};

const stagger = {
  visible: { transition: { staggerChildren: 0.1 } },
};

// ============================================================
// PAGE
// ============================================================

export default function HomePage() {
  const [openFaq, setOpenFaq] = useState<number | null>(null);

  return (
    <div className="overflow-hidden">
      {/* ========== HERO ========== */}
      <section className="relative py-20 sm:py-28 lg:py-36">
        {/* Gradient background */}
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-primary/5 via-background to-accent/5" />
        <div className="absolute top-20 left-1/4 -z-10 h-72 w-72 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute bottom-10 right-1/4 -z-10 h-56 w-56 rounded-full bg-accent/10 blur-3xl" />

        <div className="container">
          <motion.div
            initial="hidden"
            animate="visible"
            variants={stagger}
            className="mx-auto max-w-4xl text-center"
          >
            <motion.div variants={fadeInUp}>
              <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary">
                <Sparkles className="h-4 w-4" />
                Nền tảng quản lý giáo dục #1 Việt Nam
              </span>
            </motion.div>

            <motion.h1
              variants={fadeInUp}
              className="mt-8 text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl"
            >
              Quản lý trung tâm giáo dục{' '}
              <span className="bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                thông minh hơn
              </span>
            </motion.h1>

            <motion.p
              variants={fadeInUp}
              className="mt-6 text-lg text-muted-foreground sm:text-xl max-w-2xl mx-auto"
            >
              Dành thời gian cho việc giảng dạy, để KiteClass lo phần còn lại.
              Quản lý học viên, lịch học, thanh toán — tất cả trong một nền tảng.
            </motion.p>

            <motion.div variants={fadeInUp} className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link href="/register">
                <GradientButton size="lg">
                  Dùng thử miễn phí 14 ngày
                  <ArrowRight className="ml-2 h-5 w-5" />
                </GradientButton>
              </Link>
              <Link
                href="/pricing"
                className="inline-flex items-center gap-2 rounded-xl border px-6 py-3.5 text-sm font-medium hover:bg-muted transition-colors"
              >
                Xem bảng giá
              </Link>
            </motion.div>

            <motion.p variants={fadeInUp} className="mt-4 text-sm text-muted-foreground">
              Không cần thẻ tín dụng • Hủy bất kỳ lúc nào
            </motion.p>
          </motion.div>
        </div>
      </section>

      {/* ========== SOCIAL PROOF ========== */}
      <section className="border-y bg-muted/30 py-6">
        <div className="container">
          <div className="flex flex-wrap items-center justify-center gap-8 text-sm text-muted-foreground">
            <div className="flex items-center gap-2">
              <Shield className="h-5 w-5 text-primary" />
              <span><strong className="text-foreground">500+</strong> trung tâm tin dùng</span>
            </div>
            <div className="flex items-center gap-2">
              <Users className="h-5 w-5 text-primary" />
              <span><strong className="text-foreground">50,000+</strong> học viên</span>
            </div>
            <div className="flex items-center gap-2">
              <Star className="h-5 w-5 text-yellow-500" />
              <span><strong className="text-foreground">4.9/5</strong> đánh giá</span>
            </div>
          </div>
        </div>
      </section>

      {/* ========== FEATURES BENTO GRID ========== */}
      <section className="py-20 sm:py-28">
        <div className="container">
          <SectionTitle
            title="Tất cả tính năng bạn cần"
            subtitle="Từ quản lý học viên đến AI tạo website — KiteClass giúp bạn vận hành trung tâm chuyên nghiệp hơn."
          />

          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-100px' }}
            variants={stagger}
            className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
          >
            {features.map((f) => (
              <motion.div
                key={f.title}
                variants={fadeInUp}
                className="group rounded-2xl border bg-card p-6 shadow-soft hover:shadow-soft-lg transition-all duration-300"
              >
                <div className={`inline-flex rounded-xl p-3 ${f.color}`}>
                  <f.icon className="h-6 w-6" />
                </div>
                <h3 className="mt-4 text-lg font-semibold">{f.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground leading-relaxed">{f.desc}</p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* ========== HOW IT WORKS ========== */}
      <section className="py-20 sm:py-28 bg-muted/20">
        <div className="container">
          <SectionTitle
            title="Bắt đầu trong 3 bước"
            subtitle="Không cần kiến thức kỹ thuật. Thiết lập trung tâm của bạn chỉ trong vài phút."
          />

          <div className="grid gap-8 sm:grid-cols-3 max-w-4xl mx-auto">
            {steps.map((s) => (
              <motion.div
                key={s.num}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeInUp}
                className="text-center"
              >
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-accent text-2xl font-bold text-white shadow-soft">
                  {s.num}
                </div>
                <h3 className="mt-4 text-lg font-semibold">{s.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{s.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ========== TESTIMONIALS ========== */}
      <section className="py-20 sm:py-28">
        <div className="container">
          <SectionTitle
            title="Khách hàng nói gì về KiteClass"
            subtitle="Hơn 500 trung tâm đã tin dùng KiteClass để quản lý hoạt động giáo dục."
          />

          <div className="grid gap-6 sm:grid-cols-3">
            {testimonials.map((t) => (
              <motion.div
                key={t.name}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeInUp}
                className="rounded-2xl border bg-card p-6 shadow-soft"
              >
                <div className="flex gap-1">
                  {Array.from({ length: t.rating }).map((_, i) => (
                    <Star key={i} className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                  ))}
                </div>
                <p className="mt-4 text-sm leading-relaxed text-muted-foreground">"{t.text}"</p>
                <div className="mt-4 border-t pt-4">
                  <p className="text-sm font-semibold">{t.name}</p>
                  <p className="text-xs text-muted-foreground">{t.role}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ========== PRICING ========== */}
      <section className="py-20 sm:py-28 bg-muted/20" id="pricing">
        <div className="container">
          <SectionTitle
            title="Bảng giá"
            subtitle="Chọn gói phù hợp với quy mô trung tâm của bạn. Dùng thử miễn phí 14 ngày."
          />

          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 max-w-6xl mx-auto">
            {pricingTiers.map((tier) => (
              <motion.div
                key={tier.name}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeInUp}
                className={`relative rounded-2xl border bg-card p-6 shadow-soft ${
                  tier.popular ? 'border-primary shadow-soft-lg ring-2 ring-primary/20' : ''
                }`}
              >
                {tier.popular && (
                  <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-white">
                    Phổ biến nhất
                  </span>
                )}
                <h3 className="text-lg font-bold">{tier.name}</h3>
                <div className="mt-4">
                  <span className="text-3xl font-bold">{tier.price}</span>
                  {tier.period && <span className="text-sm text-muted-foreground">{tier.period}</span>}
                </div>
                <p className="mt-2 text-sm text-muted-foreground">{tier.desc}</p>
                <ul className="mt-6 space-y-2">
                  {tier.features.map((f) => (
                    <li key={f} className="flex items-start gap-2 text-sm">
                      <CheckCircle className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                      {f}
                    </li>
                  ))}
                </ul>
                <Link
                  href={tier.name === 'ENTERPRISE' ? '#contact' : '/register'}
                  className={`mt-6 block w-full rounded-xl py-2.5 text-center text-sm font-semibold transition-colors ${
                    tier.popular
                      ? 'bg-primary text-white hover:bg-primary/90'
                      : 'border hover:bg-muted'
                  }`}
                >
                  {tier.cta}
                </Link>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ========== FAQ ========== */}
      <section className="py-20 sm:py-28">
        <div className="container max-w-3xl">
          <SectionTitle title="Câu hỏi thường gặp" />

          <div className="space-y-3">
            {faqs.map((faq, i) => (
              <div key={i} className="rounded-xl border bg-card">
                <button
                  onClick={() => setOpenFaq(openFaq === i ? null : i)}
                  className="flex w-full items-center justify-between p-4 text-left text-sm font-medium hover:bg-muted/50 transition-colors rounded-xl"
                >
                  {faq.q}
                  <ChevronDown className={`h-4 w-4 shrink-0 transition-transform ${openFaq === i ? 'rotate-180' : ''}`} />
                </button>
                {openFaq === i && (
                  <div className="px-4 pb-4 text-sm text-muted-foreground leading-relaxed">
                    {faq.a}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ========== CTA BOTTOM ========== */}
      <section className="py-20 sm:py-28 bg-gradient-to-br from-primary/10 via-background to-accent/10">
        <div className="container text-center">
          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={fadeInUp}
          >
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Bắt đầu miễn phí ngay hôm nay
            </h2>
            <p className="mt-4 text-lg text-muted-foreground max-w-xl mx-auto">
              Hơn 500 trung tâm đã chọn KiteClass. Tạo tài khoản trong 30 giây và trải nghiệm sự khác biệt.
            </p>
            <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link href="/register">
                <GradientButton size="lg">
                  Dùng thử miễn phí 14 ngày
                  <ArrowRight className="ml-2 h-5 w-5" />
                </GradientButton>
              </Link>
            </div>
            <div className="mt-6 flex items-center justify-center gap-6 text-sm text-muted-foreground">
              <span className="flex items-center gap-2">
                <Phone className="h-4 w-4" />
                Hotline: 1900-xxxx
              </span>
              <span className="flex items-center gap-2">
                <Mail className="h-4 w-4" />
                support@kiteclass.com
              </span>
            </div>
          </motion.div>
        </div>
      </section>
    </div>
  );
}
