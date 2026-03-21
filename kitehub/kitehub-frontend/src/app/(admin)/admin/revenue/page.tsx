'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { DollarSign, TrendingUp, Calendar } from 'lucide-react';

export default function RevenuePage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Doanh thu</h1>

      <div className="grid gap-4 md:grid-cols-3">
        <Card className="shadow-soft">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Tổng doanh thu</p>
                <p className="mt-1 text-3xl font-bold">0đ</p>
              </div>
              <div className="rounded-xl bg-primary/10 p-3 text-primary">
                <DollarSign className="h-5 w-5" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-soft">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Tháng này</p>
                <p className="mt-1 text-3xl font-bold">0đ</p>
              </div>
              <div className="rounded-xl bg-green-50 dark:bg-green-950/30 p-3 text-green-600">
                <TrendingUp className="h-5 w-5" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-soft">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Kỳ thanh toán</p>
                <p className="mt-1 text-3xl font-bold">Tháng</p>
              </div>
              <div className="rounded-xl bg-blue-50 dark:bg-blue-950/30 p-3 text-blue-600">
                <Calendar className="h-5 w-5" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card className="shadow-soft">
        <CardHeader>
          <CardTitle>Biểu đồ doanh thu</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center h-64 rounded-xl bg-muted/30 border border-dashed">
            <p className="text-muted-foreground text-sm">Biểu đồ doanh thu sẽ hiển thị khi có dữ liệu thanh toán</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
