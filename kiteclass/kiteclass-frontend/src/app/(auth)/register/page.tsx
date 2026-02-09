/**
 * Register page (placeholder for future self-registration).
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import Link from 'next/link';
import { AuthLayout } from '@/components/layout';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Info } from 'lucide-react';

export default function RegisterPage() {
  return (
    <AuthLayout>
      <div className="space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-bold">Create an account</h1>
          <p className="text-muted-foreground">
            Get started with KiteClass today
          </p>
        </div>

        <Alert>
          <Info className="h-4 w-4" />
          <AlertTitle>Self-registration coming soon</AlertTitle>
          <AlertDescription>
            We&apos;re working on self-registration for education centers.
            For now, please contact support to create an account.
          </AlertDescription>
        </Alert>

        <div className="space-y-3">
          <Button className="w-full" onClick={() => window.open('mailto:support@kiteclass.com')}>
            Contact Support
          </Button>

          <div className="text-center text-sm">
            <span className="text-muted-foreground">Already have an account? </span>
            <Link href="/login" className="font-medium text-primary hover:underline">
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </AuthLayout>
  );
}
