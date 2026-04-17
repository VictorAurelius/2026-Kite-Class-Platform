/**
 * Parent invitation redemption page (Wave 2 MVP, GAP-052a).
 *
 * <p>Public page — the token in the URL path is the only secret needed to
 * reach it. The parent fills in a password, their name, optional phone, and
 * relationship, submits, and is redirected to login.
 *
 * <p>This page intentionally does NOT log the parent in automatically on
 * success — password hashing + User creation happen on the Gateway via the
 * registerParent flow (wired in Wave 5); MVP just calls Core's public
 * redemption endpoint, shows a success card, and sends the user to
 * {@code /login}.
 *
 * @author KiteClass Team
 * @since 3.14.0 (Wave 2 — GAP-052a)
 */

'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { AuthLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

type Relationship = 'FATHER' | 'MOTHER' | 'GUARDIAN';

interface FormState {
  password: string;
  confirmPassword: string;
  fullName: string;
  phoneNumber: string;
  relationship: Relationship;
}

const PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{8,}$/;

export default function ParentInviteRedeemPage() {
  const params = useParams<{ token: string }>();
  const router = useRouter();
  const token = params?.token ?? '';

  const [form, setForm] = useState<FormState>({
    password: '',
    confirmPassword: '',
    fullName: '',
    phoneNumber: '',
    relationship: 'GUARDIAN',
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setError(null);
  }

  function validate(): string | null {
    if (!form.fullName.trim() || form.fullName.trim().length < 2) {
      return 'Vui lòng nhập họ tên (tối thiểu 2 ký tự).';
    }
    if (!PASSWORD_REGEX.test(form.password)) {
      return 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.';
    }
    if (form.password !== form.confirmPassword) {
      return 'Mật khẩu xác nhận không khớp.';
    }
    if (form.phoneNumber && !/^0\d{9}$/.test(form.phoneNumber)) {
      return 'Số điện thoại không hợp lệ (10 số bắt đầu bằng 0).';
    }
    return null;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const invalid = validate();
    if (invalid) {
      setError(invalid);
      return;
    }
    setLoading(true);
    try {
      await apiClient.post(`/api/v1/parent-invitations/redeem/${token}`, {
        password: form.password,
        fullName: form.fullName.trim(),
        phoneNumber: form.phoneNumber || null,
        relationship: form.relationship,
      });
      setSuccess(true);
      // Send the parent to the login page after a brief confirmation.
      window.setTimeout(() => router.push('/login'), 2500);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Không thể kích hoạt tài khoản phụ huynh.';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <AuthLayout>
        <div className="space-y-4 text-center">
          <CheckCircle2 className="mx-auto h-10 w-10 text-green-500" />
          <h1 className="text-2xl font-bold">Kích hoạt thành công</h1>
          <p className="text-muted-foreground">
            Tài khoản phụ huynh của bạn đã sẵn sàng. Đang chuyển tới trang đăng nhập…
          </p>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <div className="space-y-6">
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-bold">Kích hoạt tài khoản phụ huynh</h1>
          <p className="text-sm text-muted-foreground">
            Vui lòng nhập thông tin để hoàn tất liên kết với con bạn.
          </p>
        </div>

        {error ? (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        ) : null}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="fullName">Họ và tên</Label>
            <Input
              id="fullName"
              required
              value={form.fullName}
              onChange={(e) => update('fullName', e.target.value)}
              autoComplete="name"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="phoneNumber">Số điện thoại (tuỳ chọn)</Label>
            <Input
              id="phoneNumber"
              value={form.phoneNumber}
              onChange={(e) => update('phoneNumber', e.target.value)}
              placeholder="0912345678"
              autoComplete="tel"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="relationship">Quan hệ với học sinh</Label>
            <select
              id="relationship"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={form.relationship}
              onChange={(e) => update('relationship', e.target.value as Relationship)}
            >
              <option value="FATHER">Bố</option>
              <option value="MOTHER">Mẹ</option>
              <option value="GUARDIAN">Người giám hộ</option>
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Mật khẩu</Label>
            <Input
              id="password"
              type="password"
              required
              value={form.password}
              onChange={(e) => update('password', e.target.value)}
              autoComplete="new-password"
            />
            <p className="text-xs text-muted-foreground">
              Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt.
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
            <Input
              id="confirmPassword"
              type="password"
              required
              value={form.confirmPassword}
              onChange={(e) => update('confirmPassword', e.target.value)}
              autoComplete="new-password"
            />
          </div>

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Đang xử lý…' : 'Kích hoạt tài khoản'}
          </Button>
        </form>
      </div>
    </AuthLayout>
  );
}
