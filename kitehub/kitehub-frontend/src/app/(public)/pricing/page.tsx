import type { Metadata } from 'next';
import { JsonLd } from '@/components/seo/JsonLd';
import { faqPageSchema } from '@/components/seo/schemas';
import { PRICING_FAQS, PricingContent } from './PricingContent';

export const metadata: Metadata = {
  title: 'Bảng giá',
  description:
    'KiteHub cung cấp 4 gói dịch vụ: FREE (miễn phí), BASIC (500.000₫/tháng), PREMIUM (1.500.000₫/tháng) và ENTERPRISE. Dùng thử 14 ngày, không cần thẻ tín dụng.',
  alternates: {
    canonical: 'https://kitehub.vn/pricing',
  },
  openGraph: {
    title: 'Bảng giá KiteHub - Dùng thử miễn phí 14 ngày',
    description:
      'So sánh 4 gói dịch vụ KiteHub. Giá từ 500.000₫/tháng cho trung tâm giáo dục Việt Nam. Thanh toán qua VietQR.',
    url: 'https://kitehub.vn/pricing',
    type: 'website',
  },
};

export default function PricingPage() {
  return (
    <>
      <JsonLd data={faqPageSchema(PRICING_FAQS)} />
      <PricingContent />
    </>
  );
}
