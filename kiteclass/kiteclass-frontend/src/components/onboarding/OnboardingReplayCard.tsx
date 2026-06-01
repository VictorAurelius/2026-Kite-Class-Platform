/**
 * Settings card to replay the first-login onboarding tour.
 *
 * Resets the wizard progress in localStorage + records a replay telemetry
 * event, then navigates to the dashboard where the wizard re-fires from step 1.
 * Satisfies GAP-288 AC "Replay-from-settings works".
 *
 * @author KiteClass Team
 * @since 4.0.0 — GAP-288
 */

'use client';

import { useRouter } from 'next/navigation';
import { RotateCcw } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { resetOnboardingProgress } from './OnboardingWizard';
import { trackOnboarding } from '@/lib/onboarding-telemetry';

export function OnboardingReplayCard() {
  const router = useRouter();

  const handleReplay = () => {
    resetOnboardingProgress();
    trackOnboarding('onboarding_replay');
    router.push('/dashboard');
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <RotateCcw className="h-5 w-5" />
          Hướng dẫn sử dụng
        </CardTitle>
        <CardDescription>
          Xem lại hướng dẫn 5 bước thiết lập ban đầu bất cứ lúc nào.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button
          variant="outline"
          onClick={handleReplay}
          aria-label="Xem lại hướng dẫn"
        >
          <RotateCcw className="mr-2 h-4 w-4" aria-hidden />
          Xem lại hướng dẫn
        </Button>
      </CardContent>
    </Card>
  );
}
