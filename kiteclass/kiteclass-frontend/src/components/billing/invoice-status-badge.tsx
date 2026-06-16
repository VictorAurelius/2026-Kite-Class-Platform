import { Badge } from '@/components/ui/badge';
import { InvoiceStatus } from '@/types/invoice';

interface InvoiceStatusBadgeProps {
  status: InvoiceStatus;
}

export function InvoiceStatusBadge({ status }: InvoiceStatusBadgeProps) {
  const variants = {
    [InvoiceStatus.DRAFT]: 'secondary',
    [InvoiceStatus.SENT]: 'default',
    [InvoiceStatus.PENDING]: 'default',
    [InvoiceStatus.PARTIAL]: 'default',
    [InvoiceStatus.PAID]: 'success',
    [InvoiceStatus.OVERDUE]: 'destructive',
    [InvoiceStatus.CANCELLED]: 'outline',
  } as const;

  const labels = {
    [InvoiceStatus.DRAFT]: 'Nháp',
    [InvoiceStatus.SENT]: 'Đã gửi',
    [InvoiceStatus.PENDING]: 'Chờ thanh toán',
    [InvoiceStatus.PARTIAL]: 'Thanh toán một phần',
    [InvoiceStatus.PAID]: 'Đã thanh toán',
    [InvoiceStatus.OVERDUE]: 'Quá hạn',
    [InvoiceStatus.CANCELLED]: 'Đã hủy',
  };

  return <Badge variant={variants[status]}>{labels[status]}</Badge>;
}
