'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Copy, Check } from 'lucide-react';
import { useState } from 'react';
import type { Payment } from '@/types/payment';

interface PaymentInfoProps {
  payment: Payment;
}

export function PaymentInfo({ payment }: PaymentInfoProps) {
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  const infoItems = [
    {
      label: 'Số tiền',
      value: formatAmount(payment.amountVnd),
      copyValue: payment.amountVnd.toString(),
      field: 'amount',
    },
    {
      label: 'Mã giao dịch',
      value: payment.id.substring(0, 8).toUpperCase(),
      copyValue: payment.id,
      field: 'id',
    },
    {
      label: 'Phương thức',
      value: payment.paymentMethod === 'VIETQR' ? 'VietQR' : payment.paymentMethod,
      copyValue: null,
      field: 'method',
    },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Thông tin thanh toán</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {infoItems.map((item) => (
          <div key={item.field} className="flex items-center justify-between py-2 border-b last:border-b-0">
            <div>
              <p className="text-sm text-muted-foreground">{item.label}</p>
              <p className="font-medium">{item.value}</p>
            </div>
            {item.copyValue && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => copyToClipboard(item.copyValue!, item.field)}
                className="ml-2"
              >
                {copiedField === item.field ? (
                  <Check className="h-4 w-4 text-green-600" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
              </Button>
            )}
          </div>
        ))}

        <div className="pt-4 border-t">
          <p className="text-sm text-muted-foreground mb-2">Nội dung chuyển khoản:</p>
          <div className="flex items-center justify-between bg-muted p-3 rounded">
            <code className="text-sm font-mono">{payment.id.substring(0, 8).toUpperCase()}</code>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => copyToClipboard(payment.id.substring(0, 8).toUpperCase(), 'description')}
            >
              {copiedField === 'description' ? (
                <Check className="h-4 w-4 text-green-600" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
