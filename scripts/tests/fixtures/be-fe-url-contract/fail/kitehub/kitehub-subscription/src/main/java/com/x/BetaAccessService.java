// Synthetic fixture (FAIL) — reproduces the GAP-801 class-of-bug: the backend
// builds a FE signup link "/signup/beta" but the only real FE route is
// "/beta-signup/code". The invitee clicks → 404. Per GAP-802 cơ chế #2 test.
package com.x;

class BetaAccessService {

    private String betaSignupBaseUrl = "https://kitehub.me";

    void buildInviteLink(String code) {
        // GAP-801 drift: this path does NOT exist as a Next.js route.
        // The real route is src/app/(auth)/beta-signup/code/page.tsx (/beta-signup/code).
        String signupUrl = String.format("%s/signup/beta?code=%s",
                betaSignupBaseUrl, code);
    }
}
