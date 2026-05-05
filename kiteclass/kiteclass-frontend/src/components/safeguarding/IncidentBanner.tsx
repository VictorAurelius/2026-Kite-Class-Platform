/**
 * IncidentBanner — mandatory-reporting banner for child-protection
 * incidents (GAP-322c Phase 1C v1, Wave 19 Bucket A).
 *
 * Displays a persistent warning when an incident has
 * `severity=CRITICAL` AND `category ∈ {ABUSE, GROOMING, CSAM}`. The
 * banner cites Luật Trẻ em 2016 Đ.51 mandatory-reporting obligation
 * (≤24h to Tổng đài 111 + công an địa phương) and exposes an "Đã báo
 * cáo" CTA that the safeguarding officer clicks once they have actually
 * filed the external report.
 *
 * Tied to BR-CHILD-PROTECT-006 (mandatory reporting). Out of scope for
 * v1 (deferred to Phase 1C remainder follow-up gap):
 *   - Inline reference-number form (current v1 expects parent page to
 *     wire the modal/dialog to the API call)
 *   - Localized i18n (vi only for v1; en/zh-CN tracked separately)
 *   - Live timer countdown (≤24h)
 *
 * @author KiteClass Team
 * @since 5.x (Wave 19 Bucket A — GAP-322c Phase 1C v1)
 */

'use client';

import { AlertTriangle } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

/** Severity values that participate in the banner trigger condition. */
export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Category values that participate in the banner trigger condition. */
export type IncidentCategory =
    | 'BULLYING'
    | 'ABUSE'
    | 'GROOMING'
    | 'CSAM'
    | 'OTHER';

/**
 * Categories that combined with `severity=CRITICAL` trigger the banner —
 * mirrors `IncidentService.MANDATORY_REPORT_CATEGORIES` on the backend.
 */
const MANDATORY_REPORT_CATEGORIES: ReadonlyArray<IncidentCategory> = [
    'ABUSE',
    'GROOMING',
    'CSAM',
];

export interface IncidentBannerProps {
    /** Surrogate id of the incident (echoed in the ack request). */
    incidentId: number;
    /** Current severity. */
    severity: IncidentSeverity;
    /** Current category. */
    category: IncidentCategory;
    /**
     * Whether the safeguarding officer has already acked the mandatory
     * report — when `true` the banner switches to a calmer
     * "đã báo cáo" confirmation strip; when `false` the warning is shown.
     */
    alreadyAcked?: boolean;
    /**
     * Click handler for the "Đã báo cáo" CTA. Parent page wires this to
     * a dialog that POSTs to
     * `/api/v1/incidents/{id}/mandatory-report-ack`.
     */
    onAck?: (incidentId: number) => void;
}

/**
 * Decide whether the warning banner should render for the given
 * (severity, category) pair. Exposed so callers can also use it to
 * decide non-banner UI cues (e.g., row highlight in tables).
 */
export function shouldTriggerBanner(
    severity: IncidentSeverity,
    category: IncidentCategory,
): boolean {
    return (
        severity === 'CRITICAL' &&
        MANDATORY_REPORT_CATEGORIES.includes(category)
    );
}

export function IncidentBanner({
    incidentId,
    severity,
    category,
    alreadyAcked = false,
    onAck,
}: IncidentBannerProps): JSX.Element | null {
    if (!shouldTriggerBanner(severity, category)) {
        return null;
    }

    if (alreadyAcked) {
        return (
            <Alert
                role="status"
                aria-live="polite"
                className="border-emerald-500/40 bg-emerald-50 text-emerald-900"
                data-testid="incident-banner-acked"
            >
                <AlertTitle className="font-semibold">
                    Đã báo cáo theo Đ.51
                </AlertTitle>
                <AlertDescription>
                    Sự cố #{incidentId} đã được báo cáo cho Tổng đài 111 +
                    công an địa phương. Audit log đã ghi nhận thời điểm và
                    số tham chiếu.
                </AlertDescription>
            </Alert>
        );
    }

    return (
        <Alert
            variant="destructive"
            role="alert"
            aria-live="assertive"
            className="border-red-600 bg-red-50"
            data-testid="incident-banner-warning"
        >
            <AlertTriangle className="h-4 w-4" aria-hidden="true" />
            <AlertTitle className="font-semibold">
                Luật Trẻ em 2016 Điều 51 — Báo cáo bắt buộc ≤24h
            </AlertTitle>
            <AlertDescription className="space-y-2">
                <p>
                    Trường có nghĩa vụ báo cáo nghi ngờ xâm hại trẻ em ≤24h
                    cho Tổng đài 111 + công an địa phương. Sự cố
                    #{incidentId} ({severity} / {category}) phải được xử lý
                    ngay.
                </p>
                {onAck && (
                    <Button
                        type="button"
                        size="sm"
                        variant="destructive"
                        onClick={() => onAck(incidentId)}
                        data-testid="incident-banner-ack-cta"
                    >
                        Đã báo cáo — nhập số tham chiếu
                    </Button>
                )}
            </AlertDescription>
        </Alert>
    );
}

export default IncidentBanner;
