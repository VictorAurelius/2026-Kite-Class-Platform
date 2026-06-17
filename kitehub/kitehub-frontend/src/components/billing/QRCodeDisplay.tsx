'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Clock } from 'lucide-react';
import { useEffect, useState } from 'react';
import Image from 'next/image';

interface QRCodeDisplayProps {
  qrCodeUrl: string;
  expiresAt: string | null;
}

export function QRCodeDisplay({ qrCodeUrl, expiresAt }: QRCodeDisplayProps) {
  const [timeRemaining, setTimeRemaining] = useState<number | null>(null);

  useEffect(() => {
    if (!expiresAt) {
      setTimeRemaining(null);
      return;
    }

    const updateTimer = () => {
      const now = new Date().getTime();
      const expiry = new Date(expiresAt).getTime();
      const diff = expiry - now;

      if (diff <= 0) {
        setTimeRemaining(0);
      } else {
        setTimeRemaining(Math.floor(diff / 1000)); // seconds
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);

    return () => clearInterval(interval);
  }, [expiresAt]);

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-center">Quét mã QR để thanh toán</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col items-center space-y-4">
        {/* QR Code Image */}
        <div className="relative w-64 h-64 bg-white dark:bg-white p-4 rounded-lg border">
          {/* GAP-1469 — unoptimized: bypass the Next.js image optimizer for the QR.
              The dev mock returns an SVG (placehold.co) which the optimizer blocks
              by default ("image type is not allowed"); the prod path returns a JPG
              (img.vietqr.io). A QR gains nothing from optimization (and resizing can
              hurt scannability), so render the source URL directly. */}
          <Image
            src={qrCodeUrl}
            alt="VietQR Payment Code"
            fill
            className="object-contain"
            priority
            unoptimized
          />
        </div>

        {/* Expiry Timer */}
        {timeRemaining !== null && (
          <Alert variant={timeRemaining < 60 ? 'destructive' : 'default'}>
            <Clock className="h-4 w-4" />
            <AlertDescription className="ml-2">
              {timeRemaining > 0 ? (
                <>
                  Mã QR hết hạn sau: <strong>{formatTime(timeRemaining)}</strong>
                </>
              ) : (
                <span className="text-destructive font-medium">Mã QR đã hết hạn. Vui lòng tạo thanh toán mới.</span>
              )}
            </AlertDescription>
          </Alert>
        )}

        <p className="text-sm text-muted-foreground text-center">
          Mở ứng dụng ngân hàng và quét mã QR này để thanh toán
        </p>
      </CardContent>
    </Card>
  );
}
