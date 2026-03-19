'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import {
  Users,
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
  Zap,
  GraduationCap,
  BookOpen,
  Play,
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
    color: 'from-blue-500 to-blue-600',
    bg: 'bg-blue-50 dark:bg-blue-950/30',
    img: '👨‍🎓',
    longDesc: 'Quản lý toàn bộ thông tin học viên từ hồ sơ cá nhân, lịch sử học tập, điểm số đến ghi chú riêng. Hệ thống tự động cập nhật bảng điểm và gửi báo cáo cho phụ huynh.',
    benefits: ['Hồ sơ học viên đầy đủ (ảnh, liên hệ, ghi chú)', 'Theo dõi tiến độ qua từng khóa học', 'Bảng điểm tự động tính GPA', 'Tìm kiếm & lọc nhanh theo tên, lớp, trạng thái'],
    mockupRows: [
      { name: 'Nguyễn Văn An', class: 'IELTS 7.0', status: 'active' },
      { name: 'Trần Thị Bình', class: 'TOEIC 800', status: 'active' },
      { name: 'Lê Hoàng Cường', class: 'Giao tiếp B2', status: 'trial' },
    ],
  },
  {
    icon: Clock,
    title: 'Lịch học & Điểm danh',
    desc: 'Tạo lịch, điểm danh tự động, thông báo cho phụ huynh.',
    color: 'from-green-500 to-green-600',
    bg: 'bg-green-50 dark:bg-green-950/30',
    img: '📅',
    longDesc: 'Tạo lịch học theo tuần/tháng, điểm danh bằng 1 chạm. Hệ thống tự động gửi thông báo vắng mặt cho phụ huynh qua email hoặc Zalo.',
    benefits: ['Lịch kéo-thả trực quan', 'Điểm danh 1 chạm cho cả lớp', 'Thông báo vắng tự động cho phụ huynh', 'Thống kê chuyên cần theo học viên/lớp'],
    mockupRows: [
      { name: 'Thứ 2 - IELTS 7.0', class: '18:00 - 20:00', status: 'active' },
      { name: 'Thứ 4 - TOEIC 800', class: '19:00 - 21:00', status: 'active' },
      { name: 'Thứ 7 - Kids English', class: '09:00 - 11:00', status: 'trial' },
    ],
  },
  {
    icon: CreditCard,
    title: 'Thanh toán & Hóa đơn',
    desc: 'Quản lý học phí, tạo hóa đơn, nhắc thanh toán tự động.',
    color: 'from-orange-500 to-orange-600',
    bg: 'bg-orange-50 dark:bg-orange-950/30',
    img: '💳',
    longDesc: 'Tạo hóa đơn tự động theo khóa học, theo dõi công nợ, nhắc thanh toán. Hỗ trợ QR code VietQR, Momo, ZaloPay. Xuất hóa đơn VAT cho doanh nghiệp.',
    benefits: ['Tạo hóa đơn tự động theo khóa học', 'Thanh toán QR code (VietQR, Momo)', 'Nhắc thanh toán tự động qua email', 'Báo cáo doanh thu theo tháng/quý/năm'],
    mockupRows: [
      { name: 'Nguyễn Văn An - IELTS', class: '2.500.000đ', status: 'active' },
      { name: 'Trần Thị Bình - TOEIC', class: '1.800.000đ', status: 'pending' },
      { name: 'Lê Hoàng Cường - B2', class: '2.000.000đ', status: 'active' },
    ],
  },
  {
    icon: Palette,
    title: 'AI Branding',
    desc: 'Tạo website & thương hiệu tự động bằng trí tuệ nhân tạo.',
    color: 'from-purple-500 to-purple-600',
    bg: 'bg-purple-50 dark:bg-purple-950/30',
    img: '🎨',
    longDesc: 'Upload logo → AI phân tích màu sắc, phong cách → tự động tạo website landing page, banner, profile image. Chỉ mất 5 phút để có website chuyên nghiệp.',
    benefits: ['AI phân tích logo & tạo bộ nhận diện', 'Website landing page tự động', 'Banner & hình ảnh marketing', 'Nhiều theme để lựa chọn'],
    mockupRows: [
      { name: 'Logo Analysis', class: 'Blue, Modern', status: 'active' },
      { name: 'Hero Banner', class: '1792x1024', status: 'active' },
      { name: 'Marketing Copy', class: 'Vietnamese', status: 'active' },
    ],
  },
  {
    icon: BarChart3,
    title: 'Báo cáo & Thống kê',
    desc: 'Dashboard trực quan, báo cáo doanh thu, phân tích học viên.',
    color: 'from-pink-500 to-pink-600',
    bg: 'bg-pink-50 dark:bg-pink-950/30',
    img: '📊',
    longDesc: 'Dashboard tổng quan với biểu đồ doanh thu, xu hướng tuyển sinh, tỷ lệ chuyên cần. Xuất báo cáo PDF/Excel cho quản lý và nhà đầu tư.',
    benefits: ['Dashboard tổng quan real-time', 'Biểu đồ doanh thu theo tháng/quý', 'Phân tích xu hướng tuyển sinh', 'Xuất báo cáo PDF/Excel'],
    mockupRows: [
      { name: 'Doanh thu T3/2026', class: '₫45.600.000', status: 'active' },
      { name: 'Học viên mới', class: '+12 (↑15%)', status: 'active' },
      { name: 'Tỷ lệ chuyên cần', class: '94.5%', status: 'active' },
    ],
  },
  {
    icon: Building2,
    title: 'Đa chi nhánh',
    desc: 'Quản lý nhiều cơ sở từ 1 tài khoản, phân quyền nhân viên.',
    color: 'from-teal-500 to-teal-600',
    bg: 'bg-teal-50 dark:bg-teal-950/30',
    img: '🏢',
    longDesc: 'Quản lý nhiều cơ sở/chi nhánh từ 1 tài khoản duy nhất. Phân quyền nhân viên theo vai trò (quản lý, giáo viên, kế toán). Báo cáo tổng hợp toàn hệ thống.',
    benefits: ['Quản lý nhiều chi nhánh từ 1 tài khoản', 'Phân quyền theo vai trò (quản lý, GV, kế toán)', 'Báo cáo tổng hợp toàn hệ thống', 'Chuyển học viên giữa các chi nhánh'],
    mockupRows: [
      { name: 'Chi nhánh Quận 1', class: '120 học viên', status: 'active' },
      { name: 'Chi nhánh Quận 7', class: '85 học viên', status: 'active' },
      { name: 'Chi nhánh Thủ Đức', class: '45 học viên', status: 'trial' },
    ],
  },
];

const stats = [
  { value: 500, suffix: '+', label: 'Trung tâm tin dùng', icon: Building2 },
  { value: 50000, suffix: '+', label: 'Học viên quản lý', icon: GraduationCap },
  { value: 99, suffix: '.9%', label: 'Uptime cam kết', icon: Shield },
  { value: 4, suffix: '.9/5', label: 'Đánh giá trung bình', icon: Star },
];

const steps = [
  {
    num: '01',
    title: 'Đăng ký miễn phí',
    desc: 'Tạo tài khoản trong 30 giây, không cần thẻ tín dụng.',
    icon: Zap,
    color: 'from-blue-500 to-cyan-500',
  },
  {
    num: '02',
    title: 'Cấu hình trung tâm',
    desc: 'Thêm khóa học, giáo viên, lịch học. AI hỗ trợ tạo website.',
    icon: BookOpen,
    color: 'from-purple-500 to-pink-500',
  },
  {
    num: '03',
    title: 'Bắt đầu sử dụng',
    desc: 'Quản lý học viên, điểm danh, thu học phí ngay lập tức.',
    icon: Play,
    color: 'from-orange-500 to-red-500',
  },
];

const testimonials = [
  {
    name: 'Nguyễn Thị Minh Anh',
    role: 'Giám đốc TT Anh ngữ SkyLight',
    text: 'Trước đây tôi quản lý bằng Excel, mất cả ngày. KiteClass giúp tôi tiết kiệm 3 tiếng mỗi ngày và không bao giờ quên ghi điểm danh nữa.',
    rating: 5,
    avatar: '👩‍💼',
  },
  {
    name: 'Trần Văn Đức',
    role: 'Chủ lớp Toán tư duy MathGenius',
    text: 'Phụ huynh rất hài lòng khi nhận thông báo tự động về tiến độ học của con. Tỷ lệ giữ chân học viên tăng 40% sau 3 tháng dùng.',
    rating: 5,
    avatar: '👨‍🏫',
  },
  {
    name: 'Lê Hoàng Phương',
    role: 'Trưởng phòng đào tạo, IT Academy',
    text: 'Chức năng AI Branding tạo website cho trung tâm chỉ trong 5 phút. Trông rất chuyên nghiệp, phụ huynh ấn tượng ngay từ lần đầu.',
    rating: 5,
    avatar: '👩‍💻',
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
  {
    q: 'Dùng thử 14 ngày thì cần chuẩn bị gì?',
    a: 'Không cần chuẩn bị gì cả! Chỉ cần email và 30 giây để tạo tài khoản. Bạn sẽ được dùng toàn bộ tính năng như gói cao cấp nhất — quản lý học viên, lịch học, điểm danh, thanh toán, và cả AI tạo website. Sau 14 ngày, bạn tự quyết định có tiếp tục hay không, không bị tính phí tự động.',
    icon: Sparkles,
    category: 'Bắt đầu',
  },
  {
    q: 'Tôi đang dùng Excel/sổ tay, chuyển sang KiteClass có mất dữ liệu không?',
    a: 'Không mất gì cả. Đội ngũ KiteClass sẽ hỗ trợ bạn nhập dữ liệu học viên, lớp học từ file Excel vào hệ thống. Thường chỉ mất 1-2 ngày là toàn bộ thông tin được chuyển sang. Bạn cũng có thể nhập tay dần dần — hệ thống hoạt động ngay cả khi chưa có đầy đủ dữ liệu.',
    icon: Users,
    category: 'Bắt đầu',
  },
  {
    q: 'Thanh toán bằng cách nào? Có xuất hóa đơn không?',
    a: 'Rất đơn giản — bạn quét mã QR trên ứng dụng ngân hàng (Vietcombank, Techcombank, MB Bank,... tất cả đều được), hoặc thanh toán qua Momo, ZaloPay. Hệ thống tự nhận diện khi bạn chuyển khoản thành công. Nếu trung tâm cần hóa đơn VAT (hóa đơn đỏ), chúng tôi xuất và gửi qua email trong vòng 24 giờ.',
    icon: CreditCard,
    category: 'Thanh toán',
  },
  {
    q: 'Nếu muốn nâng cấp hoặc hạ gói thì sao?',
    a: 'Bạn vào phần "Thanh toán" trong menu, chọn gói mới và xác nhận. Nếu nâng cấp, bạn chỉ trả thêm phần chênh lệch cho những ngày còn lại. Nếu hạ gói, số tiền dư sẽ được chuyển sang tháng sau. Không có phí phạt, không có ràng buộc hợp đồng. Bạn cũng có thể hủy bất kỳ lúc nào.',
    icon: ArrowRight,
    category: 'Thanh toán',
  },
  {
    q: 'Thông tin học viên có bị lộ không? Tôi rất lo về bảo mật.',
    a: 'Chúng tôi hiểu sự lo lắng của bạn. Mỗi trung tâm có một "két sắt" dữ liệu riêng — không ai (kể cả trung tâm khác dùng KiteClass) có thể xem được thông tin của bạn. Dữ liệu được "khóa" bằng công nghệ bảo mật ngân hàng (mã hóa AES-256) và lưu tại trung tâm dữ liệu Singapore với sao lưu tự động mỗi ngày. Nói đơn giản: an toàn như gửi tiền ngân hàng.',
    icon: Shield,
    category: 'Bảo mật',
  },
  {
    q: 'Tôi không rành công nghệ lắm, có dùng được không?',
    a: 'Hoàn toàn được! KiteClass được thiết kế đặc biệt cho người không chuyên IT. Giao diện giống như dùng Facebook hay Zalo — bấm nút, chọn, nhập thông tin, vậy thôi. Toàn bộ bằng tiếng Việt, không có thuật ngữ khó hiểu. Khi mới bắt đầu, hệ thống sẽ hướng dẫn bạn từng bước. Và nếu bị kẹt ở đâu, bạn nhắn tin hoặc gọi cho đội ngũ hỗ trợ — chúng tôi sẵn sàng hướng dẫn qua điện thoại hoặc video call.',
    icon: GraduationCap,
    category: 'Hỗ trợ',
  },
  {
    q: 'Tôi có thể dùng trên điện thoại được không?',
    a: 'Được luôn! Bạn mở trình duyệt trên điện thoại (Chrome, Safari) và truy cập trang quản lý — không cần cài thêm ứng dụng nào. Giao diện tự động điều chỉnh cho màn hình nhỏ nên rất dễ dùng. Nhiều chủ trung tâm dùng điện thoại để điểm danh ngay tại lớp, kiểm tra báo cáo khi đang di chuyển, hoặc nhận thông báo khi có học viên mới đăng ký.',
    icon: Phone,
    category: 'Sản phẩm',
  },
  {
    q: 'Chi phí hàng tháng có cao không?',
    a: 'Gói cơ bản chỉ từ 199.000đ/tháng (bằng 2 ly cà phê mỗi ngày). So với thuê nhân viên quản lý thủ công hoặc mất học viên vì thiếu chuyên nghiệp, đây là khoản đầu tư rất hợp lý. Nhiều trung tâm cho biết tiết kiệm được 3-5 triệu/tháng chi phí vận hành sau khi dùng KiteClass. Và bạn được dùng thử miễn phí 14 ngày trước khi quyết định.',
    icon: BarChart3,
    category: 'Thanh toán',
  },
];

// ============================================================
// ANIMATED COUNTER
// ============================================================

function AnimatedCounter({ value, suffix }: { value: number; suffix: string }) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      whileInView={{ opacity: 1 }}
      viewport={{ once: true }}
    >
      <span className="tabular-nums">{value.toLocaleString()}</span>
      <span>{suffix}</span>
    </motion.div>
  );
}

// ============================================================
// FLOATING UI MOCKUP (CSS-only illustration)
// ============================================================

function HeroMockup() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 40, rotateY: -10 }}
      animate={{ opacity: 1, y: 0, rotateY: 0 }}
      transition={{ duration: 0.8, delay: 0.3 }}
      className="relative mx-auto mt-16 max-w-4xl"
      style={{ perspective: '1000px' }}
    >
      {/* Main dashboard mockup */}
      <div className="rounded-2xl border bg-card shadow-soft-xl overflow-hidden">
        {/* Title bar */}
        <div className="flex items-center gap-2 border-b bg-muted/50 px-4 py-3">
          <div className="flex gap-1.5">
            <div className="h-3 w-3 rounded-full bg-red-400" />
            <div className="h-3 w-3 rounded-full bg-yellow-400" />
            <div className="h-3 w-3 rounded-full bg-green-400" />
          </div>
          <div className="ml-4 h-5 w-48 rounded bg-muted" />
        </div>
        {/* Content */}
        <div className="p-6">
          <div className="grid grid-cols-4 gap-4">
            {/* Stat cards */}
            {['120 Học viên', '8 Giáo viên', '12 Khóa học', '₫45.6M Doanh thu'].map((label, i) => (
              <div key={i} className="rounded-xl border bg-gradient-to-br from-background to-muted/50 p-4">
                <div className={`h-8 w-8 rounded-lg bg-gradient-to-br ${['from-blue-500 to-blue-600', 'from-green-500 to-green-600', 'from-purple-500 to-purple-600', 'from-orange-500 to-orange-600'][i]} mb-2`} />
                <div className="text-xs text-muted-foreground">{label.split(' ').slice(-1)}</div>
                <div className="text-lg font-bold">{label.split(' ')[0]}</div>
              </div>
            ))}
          </div>
          {/* Chart area */}
          <div className="mt-4 rounded-xl border bg-muted/20 p-4 h-32 flex items-end gap-1">
            {[40, 65, 45, 80, 55, 90, 70, 85, 60, 95, 75, 88].map((h, i) => (
              <motion.div
                key={i}
                initial={{ height: 0 }}
                animate={{ height: `${h}%` }}
                transition={{ duration: 0.5, delay: 0.5 + i * 0.05 }}
                className="flex-1 rounded-t bg-gradient-to-t from-primary to-primary/60"
              />
            ))}
          </div>
          {/* Table preview */}
          <div className="mt-4 space-y-2">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex items-center gap-3 rounded-lg bg-muted/30 p-3">
                <div className="h-8 w-8 rounded-full bg-gradient-to-br from-primary/20 to-accent/20" />
                <div className="flex-1">
                  <div className="h-3 w-32 rounded bg-muted" />
                  <div className="mt-1 h-2 w-20 rounded bg-muted/60" />
                </div>
                <div className="h-6 w-16 rounded-full bg-green-100 dark:bg-green-900/30" />
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Floating notification */}
      <motion.div
        animate={{ y: [0, -8, 0] }}
        transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
        className="absolute -right-4 top-20 rounded-xl border bg-card p-3 shadow-soft-lg"
      >
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-full bg-green-100 flex items-center justify-center">
            <CheckCircle className="h-4 w-4 text-green-600" />
          </div>
          <div>
            <p className="text-xs font-medium">Điểm danh thành công</p>
            <p className="text-[10px] text-muted-foreground">Lớp IELTS 7.0 - 25/25 học viên</p>
          </div>
        </div>
      </motion.div>

      {/* Floating AI badge */}
      <motion.div
        animate={{ y: [0, 8, 0] }}
        transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
        className="absolute -left-4 bottom-24 rounded-xl border bg-card p-3 shadow-soft-lg"
      >
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-full bg-purple-100 flex items-center justify-center">
            <Sparkles className="h-4 w-4 text-purple-600" />
          </div>
          <div>
            <p className="text-xs font-medium">AI đã tạo website</p>
            <p className="text-[10px] text-muted-foreground">skylight.kiteclass.com</p>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}

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

const scaleIn = {
  hidden: { opacity: 0, scale: 0.8 },
  visible: { opacity: 1, scale: 1, transition: { duration: 0.5 } },
};

// ============================================================
// PAGE
// ============================================================

// ============================================================
// PAGE
// ============================================================

export default function HomePage() {
  const [openFaq, setOpenFaq] = useState<number | null>(null);
  const [expandedFeature, setExpandedFeature] = useState<number | null>(null);
  return (
    <div className="overflow-hidden">
      {/* ========== HERO ========== */}
      <section className="relative py-20 sm:py-28 lg:py-32">
        {/* Animated gradient background */}
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-primary/5 via-background to-accent/5" />
        <motion.div
          animate={{ scale: [1, 1.2, 1], opacity: [0.3, 0.5, 0.3] }}
          transition={{ duration: 8, repeat: Infinity }}
          className="absolute top-20 left-1/4 -z-10 h-96 w-96 rounded-full bg-primary/10 blur-3xl"
        />
        <motion.div
          animate={{ scale: [1.2, 1, 1.2], opacity: [0.2, 0.4, 0.2] }}
          transition={{ duration: 10, repeat: Infinity, delay: 2 }}
          className="absolute bottom-10 right-1/4 -z-10 h-80 w-80 rounded-full bg-accent/10 blur-3xl"
        />
        <motion.div
          animate={{ scale: [1, 1.3, 1], opacity: [0.1, 0.3, 0.1] }}
          transition={{ duration: 12, repeat: Infinity, delay: 4 }}
          className="absolute top-1/2 right-10 -z-10 h-64 w-64 rounded-full bg-purple-500/10 blur-3xl"
        />

        <div className="container">
          <motion.div
            initial="hidden"
            animate="visible"
            variants={stagger}
            className="mx-auto max-w-4xl text-center"
          >
            <motion.div variants={fadeInUp}>
              <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary border border-primary/20">
                <Sparkles className="h-4 w-4" />
                Nền tảng quản lý giáo dục #1 Việt Nam
              </span>
            </motion.div>

            <motion.h1
              variants={fadeInUp}
              className="mt-8 text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl"
            >
              Quản lý trung tâm{' '}
              <br className="hidden sm:block" />
              giáo dục{' '}
              <span className="relative">
                <span className="bg-gradient-to-r from-primary via-purple-500 to-accent bg-clip-text text-transparent">
                  thông minh hơn
                </span>
                <motion.span
                  className="absolute -bottom-2 left-0 h-1 w-full bg-gradient-to-r from-primary to-accent rounded-full"
                  initial={{ scaleX: 0 }}
                  animate={{ scaleX: 1 }}
                  transition={{ duration: 0.8, delay: 0.5 }}
                />
              </span>
            </motion.h1>

            <motion.p
              variants={fadeInUp}
              className="mt-8 text-lg text-muted-foreground sm:text-xl max-w-2xl mx-auto leading-relaxed"
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
                className="group inline-flex items-center gap-2 rounded-xl border-2 px-6 py-3.5 text-sm font-semibold hover:border-primary hover:text-primary transition-all"
              >
                Xem bảng giá
                <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform" />
              </Link>
            </motion.div>

            <motion.p variants={fadeInUp} className="mt-4 text-sm text-muted-foreground">
              ✓ Không cần thẻ tín dụng &nbsp; ✓ Hủy bất kỳ lúc nào &nbsp; ✓ Hỗ trợ tiếng Việt
            </motion.p>
          </motion.div>

          {/* Hero Dashboard Mockup */}
          <HeroMockup />
        </div>
      </section>

      {/* ========== STATS ========== */}
      <section className="relative border-y bg-gradient-to-r from-primary/5 via-background to-accent/5 py-16">
        <div className="container">
          <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
            {stats.map((stat) => (
              <motion.div
                key={stat.label}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={scaleIn}
                className="text-center"
              >
                <stat.icon className="mx-auto h-6 w-6 text-primary mb-2" />
                <div className="text-3xl font-bold sm:text-4xl">
                  <AnimatedCounter value={stat.value} suffix={stat.suffix} />
                </div>
                <p className="mt-1 text-sm text-muted-foreground">{stat.label}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ========== FEATURES ========== */}
      <section className="py-20 sm:py-28">
        <div className="container">
          <SectionTitle
            title="Tất cả tính năng bạn cần"
            subtitle="Từ quản lý học viên đến AI tạo website — KiteClass giúp bạn vận hành trung tâm chuyên nghiệp hơn."
          />

          {expandedFeature === null ? (
            /* ---- All collapsed: 3-column grid ---- */
            <motion.div
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: '-100px' }}
              variants={stagger}
              className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"
            >
              {features.map((f, i) => {
                const Icon = f.icon;
                return (
                  <motion.div
                    key={f.title}
                    variants={fadeInUp}
                    whileHover={{ y: -8, transition: { duration: 0.2 } }}
                    className={`group relative rounded-2xl border p-6 shadow-soft hover:shadow-soft-xl transition-all duration-300 overflow-hidden cursor-pointer ${f.bg}`}
                    onClick={() => setExpandedFeature(i)}
                  >
                    <div className="absolute -right-6 -top-6 h-24 w-24 rounded-full bg-gradient-to-br opacity-10 group-hover:opacity-20 transition-opacity" />
                    <div className="absolute right-4 top-4 text-4xl opacity-20 group-hover:opacity-40 group-hover:scale-110 transition-all">{f.img}</div>
                    <div className={`inline-flex rounded-xl bg-gradient-to-br ${f.color} p-3 text-white shadow-sm`}>
                      <Icon className="h-6 w-6" />
                    </div>
                    <h3 className="mt-4 text-lg font-semibold">{f.title}</h3>
                    <p className="mt-2 text-sm text-muted-foreground leading-relaxed">{f.desc}</p>
                    <span className="mt-4 flex items-center gap-1 text-xs font-medium text-primary">
                      Tìm hiểu thêm <ArrowRight className="h-3 w-3 group-hover:translate-x-1 transition-transform" />
                    </span>
                  </motion.div>
                );
              })}
            </motion.div>
          ) : (
            /* ---- One expanded: panel left + mini cards right ---- */
            <div className="flex gap-6 flex-col lg:flex-row items-start">
              {/* Expanded panel (left, ~2/3) */}
              {(() => {
                const f = features[expandedFeature!];
                if (!f) return null;
                const Icon = f.icon;
                return (
                  <motion.div
                    key={f.title}
                    initial={false}
                    animate={{ opacity: 1 }}
                    className={`flex-1 lg:flex-[2] rounded-2xl border shadow-soft-xl ring-2 ring-primary/20 overflow-hidden ${f.bg}`}
                  >
                    <div className="flex items-center justify-between p-5 border-b">
                      <div className="flex items-center gap-3">
                        <div className={`inline-flex rounded-xl bg-gradient-to-br ${f.color} p-3 text-white shadow-sm`}>
                          <Icon className="h-6 w-6" />
                        </div>
                        <div>
                          <h3 className="text-xl font-bold">{f.title}</h3>
                          <p className="text-sm text-muted-foreground">{f.desc}</p>
                        </div>
                      </div>
                      <button
                        onClick={() => setExpandedFeature(null)}
                        className="rounded-lg px-3 py-1.5 text-xs font-medium hover:bg-muted transition-colors text-muted-foreground border"
                      >
                        Thu gọn
                      </button>
                    </div>
                    <div className="grid md:grid-cols-2">
                      <div className="p-6 space-y-5">
                        <div>
                          <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">Mô tả chi tiết</h4>
                          <p className="text-sm leading-relaxed">{f.longDesc}</p>
                        </div>
                        <div>
                          <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">Tính năng nổi bật</h4>
                          <ul className="space-y-2.5">
                            {f.benefits.map((b, bi) => (
                              <motion.li key={b} initial={{ opacity: 0, x: -15 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: bi * 0.1 }} className="flex items-start gap-2.5 text-sm">
                                <CheckCircle className={`h-4 w-4 shrink-0 mt-0.5 bg-gradient-to-br ${f.color} text-white rounded-full p-0.5`} />
                                {b}
                              </motion.li>
                            ))}
                          </ul>
                        </div>
                        <Link href="/register" className={`inline-flex items-center gap-2 rounded-xl bg-gradient-to-r ${f.color} px-5 py-2.5 text-sm font-semibold text-white hover:shadow-soft-lg transition-all`}>
                          Dùng thử miễn phí <ArrowRight className="h-4 w-4" />
                        </Link>
                      </div>
                      <div className="p-6 bg-muted/20 border-t md:border-t-0 md:border-l">
                        <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">Giao diện minh họa</h4>
                        <div className="rounded-xl border bg-card shadow-soft overflow-hidden">
                          <div className="flex items-center gap-2 border-b bg-muted/50 px-3 py-2">
                            <div className="flex gap-1"><div className="h-2.5 w-2.5 rounded-full bg-red-400" /><div className="h-2.5 w-2.5 rounded-full bg-yellow-400" /><div className="h-2.5 w-2.5 rounded-full bg-green-400" /></div>
                            <span className="ml-2 text-[10px] text-muted-foreground">{f.title}</span>
                          </div>
                          <div className="grid grid-cols-3 gap-2 p-3">
                            {['Tổng', 'Hoạt động', 'Mới'].map((label, si) => (
                              <div key={label} className="rounded-lg bg-muted/40 p-2 text-center">
                                <div className={`text-lg font-bold bg-gradient-to-r ${f.color} bg-clip-text text-transparent`}>{[f.mockupRows.length * 42, f.mockupRows.length * 38, f.mockupRows.length * 3][si]}</div>
                                <div className="text-[10px] text-muted-foreground">{label}</div>
                              </div>
                            ))}
                          </div>
                          <div className="px-3 pb-3 space-y-2">
                            {f.mockupRows.map((row, ri) => (
                              <motion.div key={row.name} initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: ri * 0.12 }} className="flex items-center justify-between rounded-lg bg-muted/30 p-2.5">
                                <div className="flex items-center gap-2">
                                  <div className={`h-8 w-8 rounded-full bg-gradient-to-br ${f.color} flex items-center justify-center text-white text-xs font-bold`}>{row.name.charAt(0)}</div>
                                  <div><div className="text-xs font-medium">{row.name}</div><div className="text-[10px] text-muted-foreground">{row.class}</div></div>
                                </div>
                                <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full ${row.status === 'active' ? 'bg-green-100 text-green-700 dark:bg-green-900/30' : row.status === 'pending' ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30' : 'bg-blue-100 text-blue-700 dark:bg-blue-900/30'}`}>
                                  {row.status === 'active' ? 'Hoạt động' : row.status === 'pending' ? 'Chờ TT' : 'Dùng thử'}
                                </span>
                              </motion.div>
                            ))}
                          </div>
                        </div>
                      </div>
                    </div>
                  </motion.div>
                );
              })()}

              {/* Mini cards column (right, ~1/3) */}
              <div className="lg:flex-1 flex flex-row lg:flex-col gap-3 overflow-x-auto lg:overflow-visible">
                {features.map((f, i) => {
                  if (i === expandedFeature) return null;
                  const Icon = f.icon;
                  return (
                    <motion.button
                      key={f.title}
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.05 }}
                      onClick={() => setExpandedFeature(i)}
                      className={`flex items-center gap-3 rounded-xl border p-3 shadow-soft hover:shadow-soft-lg transition-all text-left min-w-[200px] lg:min-w-0 ${f.bg}`}
                    >
                      <div className={`shrink-0 inline-flex rounded-lg bg-gradient-to-br ${f.color} p-2 text-white`}>
                        <Icon className="h-4 w-4" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold truncate">{f.title}</p>
                        <p className="text-[11px] text-muted-foreground truncate">{f.desc}</p>
                      </div>
                    </motion.button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* ========== GRADIENT DIVIDER ========== */}
      <div className="h-px bg-gradient-to-r from-transparent via-primary/30 to-transparent" />

      {/* ========== HOW IT WORKS ========== */}
      <section className="py-20 sm:py-28 bg-muted/20">
        <div className="container">
          <SectionTitle
            title="Bắt đầu trong 3 bước"
            subtitle="Không cần kiến thức kỹ thuật. Thiết lập trung tâm của bạn chỉ trong vài phút."
          />

          <div className="grid gap-8 sm:grid-cols-3 max-w-4xl mx-auto">
            {steps.map((s, i) => (
              <motion.div
                key={s.num}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                variants={fadeInUp}
                className="relative text-center"
              >
                {/* Connector line */}
                {i < steps.length - 1 && (
                  <div className="hidden sm:block absolute top-8 left-[60%] w-[80%] h-0.5 bg-gradient-to-r from-primary/30 to-transparent" />
                )}
                <motion.div
                  whileHover={{ scale: 1.1, rotate: 5 }}
                  className={`mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br ${s.color} text-white shadow-soft-lg`}
                >
                  <s.icon className="h-7 w-7" />
                </motion.div>
                <div className="mt-1 text-xs font-bold text-primary">{s.num}</div>
                <h3 className="mt-2 text-lg font-semibold">{s.title}</h3>
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
                whileHover={{ y: -4 }}
                className="relative rounded-2xl border bg-card p-6 shadow-soft hover:shadow-soft-lg transition-all"
              >
                {/* Quote decoration */}
                <div className="absolute -top-3 -left-2 text-5xl text-primary/10 font-serif">&ldquo;</div>

                <div className="flex gap-1">
                  {Array.from({ length: t.rating }).map((_, j) => (
                    <Star key={j} className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                  ))}
                </div>
                <p className="mt-4 text-sm leading-relaxed text-muted-foreground">{t.text}</p>
                <div className="mt-4 flex items-center gap-3 border-t pt-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-primary/20 to-accent/20 text-lg">
                    {t.avatar}
                  </div>
                  <div>
                    <p className="text-sm font-semibold">{t.name}</p>
                    <p className="text-xs text-muted-foreground">{t.role}</p>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ========== GRADIENT DIVIDER ========== */}
      <div className="h-px bg-gradient-to-r from-transparent via-accent/30 to-transparent" />

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
                whileHover={{ y: -8 }}
                className={`relative rounded-2xl border bg-card p-6 shadow-soft transition-all ${
                  tier.popular ? 'border-primary shadow-soft-lg ring-2 ring-primary/20 scale-[1.02]' : 'hover:shadow-soft-lg'
                }`}
              >
                {tier.popular && (
                  <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-gradient-to-r from-primary to-accent px-4 py-1 text-xs font-semibold text-white shadow-sm">
                    Phổ biến nhất
                  </span>
                )}
                <h3 className="text-lg font-bold">{tier.name}</h3>
                <div className="mt-4">
                  <span className="text-3xl font-bold">{tier.price}</span>
                  {tier.period && <span className="text-sm text-muted-foreground">{tier.period}</span>}
                </div>
                <p className="mt-2 text-sm text-muted-foreground">{tier.desc}</p>
                <ul className="mt-6 space-y-2.5">
                  {tier.features.map((f) => (
                    <li key={f} className="flex items-start gap-2 text-sm">
                      <CheckCircle className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                      {f}
                    </li>
                  ))}
                </ul>
                <Link
                  href={tier.name === 'ENTERPRISE' ? '#contact' : '/register'}
                  className={`mt-6 block w-full rounded-xl py-2.5 text-center text-sm font-semibold transition-all ${
                    tier.popular
                      ? 'bg-gradient-to-r from-primary to-accent text-white hover:shadow-soft-lg hover:scale-[1.02]'
                      : 'border-2 hover:border-primary hover:text-primary'
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
        <div className="container max-w-4xl">
          <SectionTitle
            title="Câu hỏi thường gặp"
            subtitle="Không tìm thấy câu trả lời? Liên hệ đội ngũ hỗ trợ qua chat hoặc email."
          />

          {openFaq === null ? (
            /* ---- All collapsed: 2-column grid ---- */
            <div className="grid gap-4 sm:grid-cols-2">
              {faqs.map((faq, i) => {
                const FaqIcon = faq.icon;
                return (
                  <motion.button
                    key={i}
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true }}
                    variants={fadeInUp}
                    onClick={() => setOpenFaq(i)}
                    className="rounded-2xl border bg-card p-5 shadow-soft hover:shadow-soft-lg transition-all text-left group"
                  >
                    <div className="flex items-center gap-3">
                      <div className="shrink-0 rounded-xl bg-muted p-2 text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                        <FaqIcon className="h-4 w-4" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <span className="text-[10px] font-medium text-primary uppercase tracking-wider">{faq.category}</span>
                        <p className="text-sm font-medium mt-0.5">{faq.q}</p>
                      </div>
                      <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground" />
                    </div>
                  </motion.button>
                );
              })}
            </div>
          ) : (
            /* ---- One expanded: answer left + other questions right ---- */
            <div className="flex gap-6 flex-col lg:flex-row items-start">
              {/* Expanded answer (left, ~2/3) */}
              {(() => {
                const faq = faqs[openFaq!];
                if (!faq) return null;
                const FaqIcon = faq.icon;
                return (
                  <motion.div
                    key={openFaq}
                    initial={false}
                    animate={{ opacity: 1 }}
                    className="flex-1 lg:flex-[2] rounded-2xl border bg-card shadow-soft-xl ring-2 ring-primary/20 overflow-hidden"
                  >
                    <div className="flex items-center justify-between p-6 border-b bg-primary/5">
                      <div className="flex items-center gap-4">
                        <div className="rounded-xl bg-primary/10 p-3 text-primary">
                          <FaqIcon className="h-6 w-6" />
                        </div>
                        <div>
                          <span className="text-xs font-medium text-primary uppercase tracking-wider">{faq.category}</span>
                          <h3 className="text-lg font-bold mt-0.5">{faq.q}</h3>
                        </div>
                      </div>
                      <button onClick={() => setOpenFaq(null)} className="rounded-lg px-3 py-1.5 text-xs font-medium hover:bg-muted transition-colors text-muted-foreground border">
                        Thu gọn
                      </button>
                    </div>
                    <div className="p-8">
                      <p className="text-base leading-relaxed text-foreground/80">{faq.a}</p>
                      <div className="mt-8 pt-6 border-t flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <span className="text-sm text-muted-foreground">Câu trả lời có hữu ích không?</span>
                          <button className="rounded-lg border px-4 py-1.5 text-sm hover:bg-primary/10 hover:text-primary hover:border-primary transition-colors">👍 Có</button>
                          <button className="rounded-lg border px-4 py-1.5 text-sm hover:bg-muted transition-colors">Chưa rõ</button>
                        </div>
                        <Link href="/register" className="text-sm font-medium text-primary hover:underline flex items-center gap-1">
                          Dùng thử ngay <ArrowRight className="h-3.5 w-3.5" />
                        </Link>
                      </div>
                    </div>
                  </motion.div>
                );
              })()}

              {/* Other questions (right, ~1/3) */}
              <div className="lg:flex-1 flex flex-row lg:flex-col gap-3 overflow-x-auto lg:overflow-visible lg:sticky lg:top-24">
                {faqs.map((faq, i) => {
                  if (i === openFaq) return null;
                  const FaqIcon = faq.icon;
                  return (
                    <button
                      key={i}
                      onClick={() => setOpenFaq(i)}
                      className="flex items-center gap-3 rounded-xl border bg-card p-4 shadow-soft hover:shadow-soft-lg hover:border-primary/30 transition-all text-left min-w-[240px] lg:min-w-0"
                    >
                      <div className="shrink-0 rounded-lg bg-muted p-2 text-muted-foreground">
                        <FaqIcon className="h-4 w-4" />
                      </div>
                      <div className="min-w-0">
                        <span className="text-[10px] font-medium text-primary uppercase tracking-wider">{faq.category}</span>
                        <p className="text-sm font-medium mt-0.5 line-clamp-2">{faq.q}</p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* ========== CTA BOTTOM ========== */}
      <section className="relative py-20 sm:py-28 overflow-hidden">
        {/* Background */}
        <div className="absolute inset-0 bg-gradient-to-br from-primary/10 via-background to-accent/10" />
        <motion.div
          animate={{ scale: [1, 1.3, 1] }}
          transition={{ duration: 10, repeat: Infinity }}
          className="absolute top-0 left-1/3 h-64 w-64 rounded-full bg-primary/10 blur-3xl"
        />
        <motion.div
          animate={{ scale: [1.3, 1, 1.3] }}
          transition={{ duration: 12, repeat: Infinity }}
          className="absolute bottom-0 right-1/3 h-64 w-64 rounded-full bg-accent/10 blur-3xl"
        />

        <div className="container relative text-center">
          <motion.div
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={stagger}
          >
            <motion.h2 variants={fadeInUp} className="text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">
              Bắt đầu miễn phí{' '}
              <span className="bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                ngay hôm nay
              </span>
            </motion.h2>
            <motion.p variants={fadeInUp} className="mt-4 text-lg text-muted-foreground max-w-xl mx-auto">
              Hơn 500 trung tâm đã chọn KiteClass. Tạo tài khoản trong 30 giây và trải nghiệm sự khác biệt.
            </motion.p>
            <motion.div variants={fadeInUp} className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link href="/register">
                <GradientButton size="lg">
                  Dùng thử miễn phí 14 ngày
                  <ArrowRight className="ml-2 h-5 w-5" />
                </GradientButton>
              </Link>
            </motion.div>
            <motion.div variants={fadeInUp} className="mt-8 flex flex-wrap items-center justify-center gap-6 text-sm text-muted-foreground">
              <span className="flex items-center gap-2">
                <Phone className="h-4 w-4 text-primary" />
                Hotline: 1900-xxxx
              </span>
              <span className="flex items-center gap-2">
                <Mail className="h-4 w-4 text-primary" />
                support@kiteclass.com
              </span>
            </motion.div>
          </motion.div>
        </div>
      </section>
    </div>
  );
}
