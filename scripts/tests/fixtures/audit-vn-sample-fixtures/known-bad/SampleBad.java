// Fixture: KNOWN-BAD — English placeholders + USD currency.
// Expected: audit script reports WARN with ≥4 findings:
//   - ENGLISH_NAME: John Doe, Jane Doe
//   - ENGLISH_PLACE: Example Center
//   - ENGLISH_CLASS: Class A1
//   - USD_CURRENCY: $60.00
package com.kitehub.email.fixtures;

public class SampleBad {
    private String studentName = "John Doe";
    private String parentName = "Jane Doe";
    private String tenantName = "Example Center";
    private String className = "Class A1";
    private String monthlyFee = "$60.00";
    private String yearlyFee = "720 USD";
}
