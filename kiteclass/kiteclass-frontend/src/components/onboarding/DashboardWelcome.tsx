/**
 * Welcome banner for first-time users with onboarding quick actions.
 * Persists dismissal state via localStorage.
 *
 * @author KiteClass Team
 * @since 3.15.0
 */

'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import Link from 'next/link';

const STORAGE_KEY = 'kiteclass-welcome-dismissed';

export function DashboardWelcome() {
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    const wasDismissed = localStorage.getItem(STORAGE_KEY);
    if (wasDismissed) setDismissed(true);
  }, []);

  const handleDismiss = () => {
    localStorage.setItem(STORAGE_KEY, 'true');
    setDismissed(true);
  };

  if (dismissed) return null;

  return (
    <Card className="mb-6 border-primary/20 bg-primary/5">
      <CardHeader>
        <CardTitle>Chào mừng đến với KiteClass!</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="mb-4 text-muted-foreground">
          Bắt đầu với các bước sau:
        </p>
        <div className="flex flex-wrap gap-2">
          <Link href="/students">
            <Button variant="outline" size="sm">
              Thêm học sinh
            </Button>
          </Link>
          <Link href="/teachers">
            <Button variant="outline" size="sm">
              Thêm giáo viên
            </Button>
          </Link>
          <Link href="/courses">
            <Button variant="outline" size="sm">
              Tạo khóa học
            </Button>
          </Link>
        </div>
        <Button
          variant="ghost"
          size="sm"
          className="mt-4"
          onClick={handleDismiss}
        >
          Ẩn hướng dẫn
        </Button>
      </CardContent>
    </Card>
  );
}
