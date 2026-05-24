// Fixture: ACCEPTABLE-ENGLISH — English technical tokens natural per dev-readable-doc-language §4.
// Expected: audit script reports PASS — these are NOT placeholders, they are technical/brand terms.
// Acceptable per vn-localization-audit-checklist §2 row 2:
//   - JWT, HTTP, JSON, URL, OAuth — technical tokens
//   - KiteHub, KiteClass, Sky Education — brand names (proper nouns)
//   - P2_CENTER_OWNER, PLATFORM_ADMIN — code-shaped persona slugs
package com.kitehub.email.fixtures;

public class SampleAcceptable {
    private String jwtSecret;            // JWT acceptable English technical token
    private String httpEndpoint;         // HTTP acceptable
    private String oauthProvider;        // OAuth acceptable
    private String tenantName = "Trung tâm Anh ngữ Sky Education";  // brand + VN
    private String personaSlug = "P2_CENTER_OWNER";                  // code-shaped enum
    private String role = "PLATFORM_ADMIN";                          // role enum
    private String studentName = "Trần Thị Hồng";                    // VN sample data
    // All fields above use VN sample per vn-localization-audit-checklist §2 row 7.
    // English tokens above (JWT/HTTP/OAuth) are acceptable per §2 row 2 technical-token exception.
}
