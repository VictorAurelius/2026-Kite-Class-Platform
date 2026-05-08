package com.kitehub.email.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AWS SES production integration smoke test (GAP-370 — Wave 45 Bucket C).
 *
 * <p><b>Profile-gated.</b> Default {@code mvn verify} skips this test (no
 * {@code aws-ses-real} system property set). To run:</p>
 *
 * <pre>
 * cd kitehub
 * ./mvnw -pl kitehub-email verify \
 *     -Daws-ses-real=true \
 *     -Dses.smoke.recipient=verified@example.com \
 *     -Dses.smoke.region=ap-southeast-1 \
 *     -Dses.smoke.from=noreply@kitehub.vn
 * </pre>
 *
 * <p>Requires production AWS SES credentials supplied via the standard
 * default credential chain (env vars / instance profile / SSO profile).
 * Recipient MUST be a verified address while account is in sandbox mode;
 * once production access is granted, any recipient works.</p>
 *
 * <p>This test sends ONE templated-shape email and asserts a non-blank
 * {@code MessageId} is returned. It exists to prove (a) credentials are
 * wired correctly, (b) the FROM domain/identity is verified in the target
 * region, and (c) production access is in effect for the recipient. It
 * does NOT exercise Thymeleaf rendering or branding enrichment — those
 * are covered by the in-process unit tests in {@code service/}.</p>
 *
 * <p>See {@code documents/05-guides/deploy/email-ses-setup-runbook.md}
 * §7 for the surrounding setup runbook this test verifies.</p>
 *
 * @since Wave 45 Bucket C (GAP-370 closure)
 */
@EnabledIfSystemProperty(named = "aws-ses-real", matches = "true")
class SesIntegrationSmokeTest {

    private static final String DEFAULT_REGION = "ap-southeast-1";
    private static final String DEFAULT_SUBJECT = "[SMOKE TEST] KiteHub SES integration — Wave 45 GAP-370";
    private static final String DEFAULT_HTML_BODY =
            "<html><body><h1>KiteHub SES smoke test</h1>"
                    + "<p>This email was sent by <code>SesIntegrationSmokeTest</code> to verify "
                    + "AWS SES production access. Safe to ignore.</p></body></html>";

    @Test
    void sendOneEmailViaRealSesAndAssertMessageId() {
        String recipient = requireSysProp(
                "ses.smoke.recipient",
                "set -Dses.smoke.recipient=<verified-or-prod-recipient>");
        String fromEmail = requireSysProp(
                "ses.smoke.from",
                "set -Dses.smoke.from=<verified-FROM-address, e.g. noreply@kitehub.vn>");
        String region = System.getProperty("ses.smoke.region", DEFAULT_REGION);

        try (SesClient ses = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build()) {

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder().toAddresses(recipient).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(DEFAULT_SUBJECT).build())
                            .body(Body.builder()
                                    .html(Content.builder().data(DEFAULT_HTML_BODY).build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = ses.sendEmail(request);

            assertThat(response).isNotNull();
            assertThat(response.messageId())
                    .as("SES SendEmail must return a non-blank MessageId")
                    .isNotBlank();

            // Surface MessageId so reviewers can correlate with the SES Console
            // sending-history view and recipient inbox headers.
            System.out.println("[SES smoke] MessageId=" + response.messageId()
                    + " region=" + region
                    + " from=" + fromEmail
                    + " to=" + recipient);
        }
    }

    private static String requireSysProp(String key, String hint) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required system property '" + key + "'. Hint: " + hint);
        }
        return value;
    }
}
