interface AboutSectionProps {
  content?: string;
}

export function AboutSection({ content }: AboutSectionProps) {
  return (
    <section className="py-16">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-8">Giới thiệu</h2>
        <div className="max-w-3xl mx-auto text-muted-foreground leading-relaxed">
          {content ? (
            <p>{content}</p>
          ) : (
            <p>
              Chúng tôi là trung tâm giáo dục uy tín với sứ mệnh mang đến chất lượng
              đào tạo tốt nhất cho học viên. Đội ngũ giảng viên giàu kinh nghiệm và
              chương trình học hiện đại giúp học viên đạt kết quả xuất sắc.
            </p>
          )}
        </div>
      </div>
    </section>
  );
}
