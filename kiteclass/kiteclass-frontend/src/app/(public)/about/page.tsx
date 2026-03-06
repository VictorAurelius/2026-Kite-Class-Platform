import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Giới thiệu',
  description: 'Giới thiệu về KiteClass - Nền tảng quản lý trung tâm tiếng Anh',
};

export default function AboutPage() {
  return (
    <div className="container mx-auto px-4 py-12">
      <h1 className="text-4xl font-bold mb-4">Giới thiệu về KiteClass</h1>

      <div className="prose max-w-none">
        <p className="text-lg text-muted-foreground mb-6">
          KiteClass là nền tảng quản lý trung tâm tiếng Anh toàn diện, được
          thiết kế để giúp các trung tâm tối ưu hóa vận hành và nâng cao chất
          lượng giảng dạy.
        </p>

        <h2 className="text-2xl font-semibold mb-4 mt-8">Sứ mệnh</h2>
        <p className="text-muted-foreground mb-6">
          Chúng tôi tin rằng công nghệ có thể giúp các trung tâm tiếng Anh vận
          hành hiệu quả hơn, để giáo viên có thể tập trung vào việc giảng dạy
          và học viên có trải nghiệm học tập tốt nhất.
        </p>

        <h2 className="text-2xl font-semibold mb-4 mt-8">Tầm nhìn</h2>
        <p className="text-muted-foreground">
          Trở thành nền tảng quản lý trung tâm tiếng Anh hàng đầu tại Việt Nam,
          giúp hàng nghìn trung tâm nâng cao chất lượng giảng dạy và trải
          nghiệm học viên.
        </p>
      </div>
    </div>
  );
}
