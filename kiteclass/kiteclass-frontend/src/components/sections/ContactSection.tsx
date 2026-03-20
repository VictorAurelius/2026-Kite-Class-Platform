import { Mail, Phone, MapPin } from 'lucide-react';

interface ContactSectionProps {
  email?: string;
  phone?: string;
  address?: string;
}

export function ContactSection({ email, phone, address }: ContactSectionProps) {
  return (
    <section className="py-16 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Liên hệ</h2>
        <div className="grid md:grid-cols-3 gap-8 max-w-3xl mx-auto">
          {email && (
            <div className="text-center">
              <div className="rounded-full bg-theme-primary/10 p-4 w-fit mx-auto mb-3">
                <Mail className="h-6 w-6 text-theme-primary" />
              </div>
              <p className="font-semibold">Email</p>
              <a href={`mailto:${email}`} className="text-sm text-muted-foreground hover:text-theme-primary">
                {email}
              </a>
            </div>
          )}
          {phone && (
            <div className="text-center">
              <div className="rounded-full bg-theme-primary/10 p-4 w-fit mx-auto mb-3">
                <Phone className="h-6 w-6 text-theme-primary" />
              </div>
              <p className="font-semibold">Điện thoại</p>
              <a href={`tel:${phone}`} className="text-sm text-muted-foreground hover:text-theme-primary">
                {phone}
              </a>
            </div>
          )}
          {address && (
            <div className="text-center">
              <div className="rounded-full bg-theme-primary/10 p-4 w-fit mx-auto mb-3">
                <MapPin className="h-6 w-6 text-theme-primary" />
              </div>
              <p className="font-semibold">Địa chỉ</p>
              <p className="text-sm text-muted-foreground">{address}</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
