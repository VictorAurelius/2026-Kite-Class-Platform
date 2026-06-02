package com.kitehub.email.zalo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for the Zalo OA Phase 1 scaffold (Wave local-doable-11 Bucket B).
 *
 * <p>Verifies — without standing up the full Spring Boot context — three contract
 * surfaces:
 * <ol>
 *   <li>{@link ZaloOAConfig.ZaloProperties} binds the {@code zalo.*} property
 *       block (defaults + explicit overrides) — mirrors {@code SESConfigTest}
 *       pattern.</li>
 *   <li>{@link ZaloOAMockClient} activates as the default {@link ZaloOAClient}
 *       bean when {@code zalo.provider} is absent OR equal to {@code "mock"},
 *       and stays inactive when {@code zalo.provider=live} (no live impl yet
 *       in Phase 1, so the context exposes zero {@link ZaloOAClient} beans).</li>
 *   <li>The mock's determinism contract holds —
 *       {@link ZaloOAMockClient#sendMessage} issues {@code mock-zalo-1},
 *       {@code mock-zalo-2}, ... in order;
 *       {@link ZaloOAMockClient#getDeliveryStatus} returns {@code DELIVERED}
 *       for ids the same client issued and {@code UNKNOWN} otherwise.</li>
 * </ol>
 *
 * <p>Live integration against a real Zalo OA business account is deferred to
 * Wave 12+ (requires verified OA business account — see Bucket C runbook).</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@DisplayName("Zalo OA Phase 1 scaffold — config + mock client IT")
class ZaloOAScaffoldIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ZaloOAConfig.class, ZaloOAMockClient.class);

    // ---------------------------------------------------------------
    // (1) Config binding
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ZaloProperties binding")
    class PropertyBinding {

        @Test
        @DisplayName("Defaults apply when no zalo.* properties set")
        void defaultsApply() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ZaloOAConfig.ZaloProperties.class);
                ZaloOAConfig.ZaloProperties props =
                    context.getBean(ZaloOAConfig.ZaloProperties.class);

                assertThat(props.getProvider()).isEqualTo("mock");
                assertThat(props.getOaId()).isEmpty();
                assertThat(props.getAccessToken()).isEmpty();
                assertThat(props.getApiBaseUrl()).isEqualTo("https://openapi.zalo.me");
                assertThat(props.getTimeoutSeconds()).isEqualTo(5);
            });
        }

        @Test
        @DisplayName("Explicit overrides bind correctly")
        void overridesBind() {
            contextRunner
                .withPropertyValues(
                    "zalo.provider=live",
                    "zalo.oa-id=1234567890",
                    "zalo.access-token=stub-token-abcdef",
                    "zalo.api-base-url=https://openapi-stub.example.com",
                    "zalo.timeout-seconds=12"
                )
                .run(context -> {
                    ZaloOAConfig.ZaloProperties props =
                        context.getBean(ZaloOAConfig.ZaloProperties.class);
                    assertThat(props.getProvider()).isEqualTo("live");
                    assertThat(props.getOaId()).isEqualTo("1234567890");
                    assertThat(props.getAccessToken()).isEqualTo("stub-token-abcdef");
                    assertThat(props.getApiBaseUrl())
                        .isEqualTo("https://openapi-stub.example.com");
                    assertThat(props.getTimeoutSeconds()).isEqualTo(12);
                });
        }
    }

    // ---------------------------------------------------------------
    // (2) Mock client conditional activation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ZaloOAMockClient conditional activation")
    class MockActivation {

        @Test
        @DisplayName("Mock client present when zalo.provider absent (matchIfMissing)")
        void mockActivatesWhenProviderAbsent() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ZaloOAClient.class);
                assertThat(context.getBean(ZaloOAClient.class))
                    .isInstanceOf(ZaloOAMockClient.class);
            });
        }

        @Test
        @DisplayName("Mock client present when zalo.provider=mock")
        void mockActivatesWhenProviderMock() {
            contextRunner
                .withPropertyValues("zalo.provider=mock")
                .run(context -> {
                    assertThat(context).hasSingleBean(ZaloOAClient.class);
                    assertThat(context.getBean(ZaloOAClient.class))
                        .isInstanceOf(ZaloOAMockClient.class);
                });
        }

        @Test
        @DisplayName("Mock client NOT present when zalo.provider=live (no live impl yet)")
        void mockInactiveWhenProviderLive() {
            contextRunner
                .withPropertyValues("zalo.provider=live")
                .run(context -> {
                    // Phase 1: only mock impl exists. With provider=live the mock is
                    // disabled by @ConditionalOnProperty and no replacement bean is
                    // wired yet — verifies the conditional is wired correctly so the
                    // future live impl can plug in without rule churn.
                    assertThat(context).doesNotHaveBean(ZaloOAClient.class);
                });
        }
    }

    // ---------------------------------------------------------------
    // (3) Mock determinism contract
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ZaloOAMockClient determinism contract")
    class Determinism {

        @Test
        @DisplayName("sendMessage issues monotonic mock-zalo-N ids")
        void sendMessageMonotonic() {
            contextRunner.run(context -> {
                ZaloOAClient client = context.getBean(ZaloOAClient.class);
                ZaloMessage msg = ZaloMessage.builder()
                    .body("Hello {{name}}")
                    .locale("vi")
                    .build();

                ZaloSendResult r1 = client.sendMessage("zalo-user-1", msg);
                ZaloSendResult r2 = client.sendMessage("zalo-user-1", msg);
                ZaloSendResult r3 = client.sendMessage("zalo-user-2", msg);

                assertThat(r1.getProviderMessageId()).isEqualTo("mock-zalo-1");
                assertThat(r2.getProviderMessageId()).isEqualTo("mock-zalo-2");
                assertThat(r3.getProviderMessageId()).isEqualTo("mock-zalo-3");

                assertThat(r1.getStatus()).isEqualTo(ZaloSendResult.Status.MOCK);
                assertThat(r2.getStatus()).isEqualTo(ZaloSendResult.Status.MOCK);
                assertThat(r3.getStatus()).isEqualTo(ZaloSendResult.Status.MOCK);

                assertThat(r1.getSentAt()).isNotNull();
                assertThat(r1.getErrorMessage()).isNull();
            });
        }

        @Test
        @DisplayName("getDeliveryStatus returns DELIVERED for previously-sent ids")
        void deliveryStatusForKnownIds() {
            contextRunner.run(context -> {
                ZaloOAClient client = context.getBean(ZaloOAClient.class);
                ZaloSendResult sent = client.sendMessage(
                    "zalo-user-1",
                    ZaloMessage.builder().body("ping").build()
                );

                assertThat(client.getDeliveryStatus(sent.getProviderMessageId()))
                    .isEqualTo(ZaloOAClient.DeliveryStatus.DELIVERED);
            });
        }

        @Test
        @DisplayName("getDeliveryStatus returns UNKNOWN for never-issued ids")
        void deliveryStatusForUnknownIds() {
            contextRunner.run(context -> {
                ZaloOAClient client = context.getBean(ZaloOAClient.class);

                assertThat(client.getDeliveryStatus("mock-zalo-9999"))
                    .isEqualTo(ZaloOAClient.DeliveryStatus.UNKNOWN);
                assertThat(client.getDeliveryStatus("not-a-mock-id"))
                    .isEqualTo(ZaloOAClient.DeliveryStatus.UNKNOWN);
                assertThat(client.getDeliveryStatus("mock-zalo-notnumeric"))
                    .isEqualTo(ZaloOAClient.DeliveryStatus.UNKNOWN);
            });
        }

        @Test
        @DisplayName("verifyAccount returns true (mock is always verified)")
        void verifyAccountAlwaysTrue() {
            contextRunner.run(context -> {
                ZaloOAClient client = context.getBean(ZaloOAClient.class);
                assertThat(client.verifyAccount()).isTrue();
            });
        }

        @Test
        @DisplayName("sendMessage rejects null/blank recipient")
        void sendMessageRejectsBlankRecipient() {
            contextRunner.run(context -> {
                ZaloOAClient client = context.getBean(ZaloOAClient.class);
                ZaloMessage msg = ZaloMessage.builder().body("ping").build();

                assertThatThrownBy(() -> client.sendMessage(null, msg))
                    .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> client.sendMessage("", msg))
                    .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> client.sendMessage("   ", msg))
                    .isInstanceOf(IllegalArgumentException.class);
            });
        }

        @Test
        @DisplayName("sendMessage rejects null message")
        void sendMessageRejectsNullMessage() {
            contextRunner.run(context -> {
                ZaloOAClient client = context.getBean(ZaloOAClient.class);
                assertThatThrownBy(() -> client.sendMessage("zalo-user-1", null))
                    .isInstanceOf(IllegalArgumentException.class);
            });
        }
    }
}
