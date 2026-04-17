import { Badge } from '@/components/ui/badge';
import { PaymentStatus } from '@/types/payment';

interface PaymentStatusBadgeProps {
  status: PaymentStatus;
}

export function PaymentStatusBadge({ status }: PaymentStatusBadgeProps) {
  const variants = {
    [PaymentStatus.PENDING]: 'default',
    [PaymentStatus.PROCESSING]: 'secondary',
    [PaymentStatus.COMPLETED]: 'success',
    [PaymentStatus.FAILED]: 'destructive',
    [PaymentStatus.REFUNDED]: 'outline',
  } as const;

  const labels = {
    [PaymentStatus.PENDING]: 'Chờ xử lý',
    [PaymentStatus.PROCESSING]: 'Đang xử lý',
    [PaymentStatus.COMPLETED]: 'Hoàn thành',
    [PaymentStatus.FAILED]: 'Thất bại',
    [PaymentStatus.REFUNDED]: 'Đã hoàn tiền',
  };

  return <Badge variant={variants[status]}>{labels[status]}</Badge>;
}
