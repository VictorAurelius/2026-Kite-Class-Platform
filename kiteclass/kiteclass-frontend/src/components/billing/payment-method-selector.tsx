'use client';

import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { PaymentMethod } from '@/types/payment';

interface PaymentMethodSelectorProps {
  value: PaymentMethod;
  onChange: (value: PaymentMethod) => void;
}

export function PaymentMethodSelector({
  value,
  onChange,
}: PaymentMethodSelectorProps) {
  const methods = [
    {
      value: PaymentMethod.CASH,
      label: 'Tiền mặt',
      description: 'Thanh toán trực tiếp',
    },
    {
      value: PaymentMethod.BANK_TRANSFER,
      label: 'Chuyển khoản',
      description: 'Chuyển khoản ngân hàng',
    },
    {
      value: PaymentMethod.MOMO,
      label: 'Ví MoMo',
      description: 'Thanh toán qua ví MoMo',
    },
    {
      value: PaymentMethod.VNPAY,
      label: 'VNPay',
      description: 'Cổng thanh toán VNPay',
    },
    {
      value: PaymentMethod.ZALOPAY,
      label: 'ZaloPay',
      description: 'Ví ZaloPay',
    },
    {
      value: PaymentMethod.CREDIT_CARD,
      label: 'Thẻ tín dụng',
      description: 'Thanh toán bằng thẻ',
    },
  ];

  return (
    <RadioGroup value={value} onValueChange={onChange}>
      <div className="space-y-3">
        {methods.map((method) => (
          <div
            key={method.value}
            className="flex items-center space-x-3 rounded-lg border p-4"
          >
            <RadioGroupItem value={method.value} id={method.value} />
            <Label htmlFor={method.value} className="flex-1 cursor-pointer">
              <div className="font-medium">{method.label}</div>
              <div className="text-sm text-muted-foreground">
                {method.description}
              </div>
            </Label>
          </div>
        ))}
      </div>
    </RadioGroup>
  );
}
