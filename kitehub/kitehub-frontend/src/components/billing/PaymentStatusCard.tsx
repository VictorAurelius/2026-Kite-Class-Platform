'use client';

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { CheckCircle, XCircle, Clock, AlertTriangle } from 'lucide-react';
import type { PaymentStatus } from '@/types/payment';

interface PaymentStatusCardProps {
  status: PaymentStatus;
  createdAt: string;
}

export function PaymentStatusCard({ status, createdAt }: PaymentStatusCardProps) {
  const statusConfig = {
    PENDING: {
      icon: Clock,
      title: 'Đang chờ thanh toán',
      description: 'Vui lòng quét mã QR hoặc chuyển khoản theo thông tin bên dưới. Trang sẽ tự động cập nhật khi thanh toán thành công.',
      variant: 'default' as const,
      color: 'text-blue-600 dark:text-blue-400',
    },
    COMPLETED: {
      icon: CheckCircle,
      title: 'Thanh toán thành công',
      description: 'Gói đăng ký của bạn đã được nâng cấp. Cảm ơn bạn đã sử dụng dịch vụ!',
      variant: 'default' as const,
      color: 'text-green-600 dark:text-green-400',
    },
    FAILED: {
      icon: XCircle,
      title: 'Thanh toán thất bại',
      description: 'Giao dịch không thành công. Vui lòng thử lại hoặc liên hệ hỗ trợ nếu vấn đề vẫn tiếp diễn.',
      variant: 'destructive' as const,
      color: 'text-red-600 dark:text-red-400',
    },
    EXPIRED: {
      icon: AlertTriangle,
      title: 'Mã QR đã hết hạn',
      description: 'Mã QR thanh toán đã hết hạn. Vui lòng quay lại trang thanh toán và tạo mã mới.',
      variant: 'destructive' as const,
      color: 'text-orange-600 dark:text-orange-400',
    },
  };

  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <Alert variant={config.variant} className="mb-6">
      <Icon className={`h-5 w-5 ${config.color}`} />
      <AlertTitle className="ml-2 text-lg">{config.title}</AlertTitle>
      <AlertDescription className="ml-2 mt-2">
        {config.description}
      </AlertDescription>

      <div className="ml-2 mt-3 text-sm text-muted-foreground">
        Tạo lúc: {new Date(createdAt).toLocaleString('vi-VN')}
      </div>
    </Alert>
  );
}
