/**
 * Contact page with form submission.
 *
 * @author KiteClass Team
 * @since 3.12.0
 */

'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { publicApi } from '@/lib/api/public';
import { Mail, Phone, MapPin, CheckCircle } from 'lucide-react';

const schema = z.object({
  name: z.string().min(2, 'Tên phải có ít nhất 2 ký tự'),
  email: z.string().email('Email không hợp lệ'),
  phone: z.string().optional(),
  message: z.string().min(10, 'Tin nhắn phải có ít nhất 10 ký tự'),
});

type FormData = z.infer<typeof schema>;

export default function ContactPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    setIsSubmitting(true);
    try {
      await publicApi.submitContactForm(data);
      setIsSuccess(true);
      toast({
        title: 'Gửi thành công!',
        description: 'Chúng tôi sẽ liên hệ lại với bạn trong thời gian sớm nhất.',
      });
      reset();

      // Reset success state after 5 seconds
      setTimeout(() => setIsSuccess(false), 5000);
    } catch {
      toast({
        title: 'Lỗi',
        description: 'Không thể gửi tin nhắn. Vui lòng thử lại sau.',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-4xl font-bold mb-4 text-center">Liên hệ</h1>
        <p className="text-muted-foreground mb-12 text-center">
          Liên hệ với chúng tôi để được tư vấn và hỗ trợ
        </p>

        <div className="grid md:grid-cols-2 gap-8">
          {/* Contact Form */}
          <Card>
            <CardHeader>
              <CardTitle>Gửi tin nhắn</CardTitle>
            </CardHeader>
            <CardContent>
              {isSuccess ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                  <CheckCircle className="h-16 w-16 text-green-500 mb-4" />
                  <h3 className="text-lg font-semibold mb-2">
                    Đã gửi tin nhắn thành công!
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Chúng tôi sẽ liên hệ lại với bạn sớm nhất có thể.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                  <div>
                    <Label htmlFor="name">Họ và tên *</Label>
                    <Input id="name" {...register('name')} />
                    {errors.name && (
                      <p className="text-sm text-destructive mt-1">
                        {errors.name.message}
                      </p>
                    )}
                  </div>

                  <div>
                    <Label htmlFor="email">Email *</Label>
                    <Input id="email" type="email" {...register('email')} />
                    {errors.email && (
                      <p className="text-sm text-destructive mt-1">
                        {errors.email.message}
                      </p>
                    )}
                  </div>

                  <div>
                    <Label htmlFor="phone">Số điện thoại</Label>
                    <Input id="phone" type="tel" {...register('phone')} />
                  </div>

                  <div>
                    <Label htmlFor="message">Nội dung tin nhắn *</Label>
                    <Textarea
                      id="message"
                      rows={5}
                      {...register('message')}
                    />
                    {errors.message && (
                      <p className="text-sm text-destructive mt-1">
                        {errors.message.message}
                      </p>
                    )}
                  </div>

                  <Button
                    type="submit"
                    className="w-full"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? 'Đang gửi...' : 'Gửi tin nhắn'}
                  </Button>
                </form>
              )}
            </CardContent>
          </Card>

          {/* Contact Info */}
          <div className="space-y-6">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <Mail className="h-6 w-6 text-primary mt-1" />
                  <div>
                    <h3 className="font-semibold mb-1">Email</h3>
                    <p className="text-sm text-muted-foreground">
                      {process.env.NEXT_PUBLIC_CONTACT_EMAIL || 'support@kiteclass.com'}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <Phone className="h-6 w-6 text-primary mt-1" />
                  <div>
                    <h3 className="font-semibold mb-1">Hotline</h3>
                    <p className="text-sm text-muted-foreground">
                      {process.env.NEXT_PUBLIC_CONTACT_PHONE || '1900 xxxx'} (8:00 - 18:00)
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-start gap-4">
                  <MapPin className="h-6 w-6 text-primary mt-1" />
                  <div>
                    <h3 className="font-semibold mb-1">Địa chỉ</h3>
                    <p className="text-sm text-muted-foreground">
                      Hà Nội, Việt Nam
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
