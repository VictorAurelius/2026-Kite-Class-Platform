// Synthetic fixture (PASS) — BE FE-path builders that all resolve to real
// Next.js App Router routes, including a route-group-stripped path and a
// dynamic-segment path. Per GAP-802 cơ chế #2 test.
package com.x;

class BetaAccessService {

    private String betaSignupBaseUrl = "https://kitehub.me";
    private String inviteBaseUrl = "https://kitehub.me";
    private String dashboardBaseUrl = "https://kitehub.me";

    void buildLinks(String code, String token, String id) {
        // Shape (b) String.format — route lives under (auth) route group:
        //   src/app/(auth)/beta-signup/code/page.tsx → URL /beta-signup/code
        // Verifies route-group "(auth)" is stripped from the matched URL.
        String signupUrl = String.format("%s/beta-signup/code?code=%s",
                betaSignupBaseUrl, code);

        // Shape (a) concatenation — route under (public) route group:
        //   src/app/(public)/staff/accept-invite/page.tsx → URL /staff/accept-invite
        // Verifies a different route-group "(public)" is also stripped.
        String inviteUrl = inviteBaseUrl + "/staff/accept-invite?token=" + token;

    }
}
