import { Mail, Phone, MapPin } from 'lucide-react';
import type { SlotData } from '@/lib/template/slots';

interface ContactSectionProps {
  slots?: SlotData;
  email?: string;
  phone?: string;
  address?: string;
}

export function ContactSection({ slots, email, phone, address }: ContactSectionProps) {
  const contactEmail = (slots?.email as string) || email;
  const contactPhone = (slots?.phone as string) || phone;
  const contactAddress = (slots?.address as string) || address;

  const hasAny = contactEmail || contactPhone || contactAddress;
  if (!hasAny) return null;

  return (
    <section className="py-16 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-3xl font-bold text-center mb-12">Liên hệ</h2>
        <div className="grid md:grid-cols-3 gap-8 max-w-3xl mx-auto">
          {contactEmail && (
            <div className="text-center">
              <div className="rounded-full bg-theme-primary/10 p-4 w-fit mx-auto mb-3">
                <Mail className="h-6 w-6 text-theme-primary" />
              </div>
              <p className="font-semibold">Email</p>
              <a href={`mailto:${contactEmail}`} className="text-sm text-muted-foreground hover:text-theme-primary">
                {contactEmail}
              </a>
            </div>
          )}
          {contactPhone && (
            <div className="text-center">
              <div className="rounded-full bg-theme-primary/10 p-4 w-fit mx-auto mb-3">
                <Phone className="h-6 w-6 text-theme-primary" />
              </div>
              <p className="font-semibold">Điện thoại</p>
              <a href={`tel:${contactPhone}`} className="text-sm text-muted-foreground hover:text-theme-primary">
                {contactPhone}
              </a>
            </div>
          )}
          {contactAddress && (
            <div className="text-center">
              <div className="rounded-full bg-theme-primary/10 p-4 w-fit mx-auto mb-3">
                <MapPin className="h-6 w-6 text-theme-primary" />
              </div>
              <p className="font-semibold">Địa chỉ</p>
              <p className="text-sm text-muted-foreground">{contactAddress}</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
