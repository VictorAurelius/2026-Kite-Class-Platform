/**
 * IncidentBanner — unit tests (Wave 19 Bucket A — GAP-322c Phase 1C v1).
 *
 * @author KiteClass Team
 * @since 5.x
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import {
    IncidentBanner,
    shouldTriggerBanner,
    type IncidentCategory,
    type IncidentSeverity,
} from './IncidentBanner';

describe('IncidentBanner — trigger predicate', () => {
    it.each<[IncidentSeverity, IncidentCategory, boolean]>([
        ['CRITICAL', 'ABUSE', true],
        ['CRITICAL', 'GROOMING', true],
        ['CRITICAL', 'CSAM', true],
        ['CRITICAL', 'BULLYING', false],
        ['CRITICAL', 'OTHER', false],
        ['HIGH', 'ABUSE', false],
        ['MEDIUM', 'CSAM', false],
        ['LOW', 'GROOMING', false],
    ])(
        'severity=%s + category=%s → triggered=%s',
        (severity, category, expected) => {
            expect(shouldTriggerBanner(severity, category)).toBe(expected);
        },
    );
});

describe('IncidentBanner — render', () => {
    it('returns null when (severity, category) does not trigger', () => {
        const { container } = render(
            <IncidentBanner
                incidentId={1}
                severity="MEDIUM"
                category="BULLYING"
            />,
        );
        expect(container.firstChild).toBeNull();
    });

    it('renders the warning variant when CRITICAL+ABUSE and not yet acked', () => {
        render(
            <IncidentBanner
                incidentId={42}
                severity="CRITICAL"
                category="ABUSE"
                onAck={vi.fn()}
            />,
        );
        expect(
            screen.getByTestId('incident-banner-warning'),
        ).toBeInTheDocument();
        // Cites Đ.51 + 24h obligation
        expect(
            screen.getByText(/Điều 51/i),
        ).toBeInTheDocument();
        // "≤24h" appears in BOTH heading + body — assert ≥2 occurrences instead of getByText (which throws on multi-match)
        expect(screen.getAllByText(/24h/).length).toBeGreaterThanOrEqual(2);
        // Surfaces the incident id
        expect(screen.getByText(/#42/)).toBeInTheDocument();
    });

    it('renders the calmer "acked" strip when alreadyAcked=true', () => {
        render(
            <IncidentBanner
                incidentId={7}
                severity="CRITICAL"
                category="GROOMING"
                alreadyAcked
            />,
        );
        expect(
            screen.getByTestId('incident-banner-acked'),
        ).toBeInTheDocument();
        expect(
            screen.queryByTestId('incident-banner-warning'),
        ).not.toBeInTheDocument();
    });

    it('does not render the ack CTA when onAck is not provided', () => {
        render(
            <IncidentBanner
                incidentId={5}
                severity="CRITICAL"
                category="CSAM"
            />,
        );
        expect(
            screen.queryByTestId('incident-banner-ack-cta'),
        ).not.toBeInTheDocument();
    });

    it('invokes onAck with the incident id when CTA clicked', async () => {
        const user = userEvent.setup();
        const onAck = vi.fn();
        render(
            <IncidentBanner
                incidentId={99}
                severity="CRITICAL"
                category="CSAM"
                onAck={onAck}
            />,
        );

        await user.click(screen.getByTestId('incident-banner-ack-cta'));

        expect(onAck).toHaveBeenCalledTimes(1);
        expect(onAck).toHaveBeenCalledWith(99);
    });
});
