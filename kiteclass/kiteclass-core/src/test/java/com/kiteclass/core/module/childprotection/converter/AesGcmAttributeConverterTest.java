package com.kiteclass.core.module.childprotection.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AesGcmAttributeConverter} — field-level encryption for
 * {@code Incident} sensitive columns (description, evidence_paths).
 *
 * <p>Coverage:
 * <ul>
 *   <li>Encrypt/decrypt roundtrip preserves plaintext</li>
 *   <li>Per-field random IV — same plaintext encrypts to different ciphertexts</li>
 *   <li>Tampered ciphertext detected via GCM auth tag → throws</li>
 *   <li>Null + empty handled (DB null preserves null)</li>
 *   <li>Output format: BYTEA-compatible byte[] [IV(12)|cipher|tag(16)]</li>
 *   <li>Unicode + special chars roundtrip (Vietnamese diacritics)</li>
 *   <li>Construction with invalid key length rejected</li>
 * </ul>
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@DisplayName("AesGcmAttributeConverter — AES-256-GCM field-level encryption")
class AesGcmAttributeConverterTest {

    private AesGcmAttributeConverter converter;
    private String testMasterKeyBase64;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a test 32-byte AES-256 key (Base64-encoded)
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey key = kg.generateKey();
        testMasterKeyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());

        converter = new AesGcmAttributeConverter(testMasterKeyBase64, "test");
    }

    @Test
    @DisplayName("encrypt/decrypt roundtrip preserves plaintext")
    void shouldRoundtripPlaintext() {
        String plaintext = "Suspected bullying incident — student reported by PH HS D 7A";

        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotNull();
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("per-field random IV — same plaintext encrypts to different ciphertexts")
    void shouldUseRandomIvPerField() {
        String plaintext = "Same incident description";

        byte[] cipher1 = converter.convertToDatabaseColumn(plaintext);
        byte[] cipher2 = converter.convertToDatabaseColumn(plaintext);

        // Different ciphertexts (random IV) — defends against pattern analysis
        assertThat(cipher1).isNotEqualTo(cipher2);

        // But both decrypt to the same plaintext
        assertThat(converter.convertToEntityAttribute(cipher1)).isEqualTo(plaintext);
        assertThat(converter.convertToEntityAttribute(cipher2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("tampered ciphertext detected — GCM auth tag verification throws")
    void shouldDetectTamperedCiphertext() {
        String plaintext = "Highly sensitive evidence";
        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);

        // Tamper with last byte (auth tag region)
        byte[] tampered = encrypted.clone();
        tampered[tampered.length - 1] ^= (byte) 0xFF;

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("decrypt");
    }

    @Test
    @DisplayName("tampered IV region detected — GCM auth tag verification throws")
    void shouldDetectTamperedIv() {
        String plaintext = "Sensitive evidence";
        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);

        // Tamper with IV region (first 12 bytes)
        byte[] tampered = encrypted.clone();
        tampered[0] ^= (byte) 0xFF;

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("decrypt");
    }

    @Test
    @DisplayName("null plaintext → null DB column (preserves null semantics)")
    void shouldHandleNullPlaintext() {
        byte[] encrypted = converter.convertToDatabaseColumn(null);
        assertThat(encrypted).isNull();
    }

    @Test
    @DisplayName("null DB column → null entity attribute (preserves null semantics)")
    void shouldHandleNullDbColumn() {
        String decrypted = converter.convertToEntityAttribute(null);
        assertThat(decrypted).isNull();
    }

    @Test
    @DisplayName("empty plaintext encrypts and decrypts to empty string")
    void shouldHandleEmptyPlaintext() {
        byte[] encrypted = converter.convertToDatabaseColumn("");
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertThat(decrypted).isEmpty();
    }

    @Test
    @DisplayName("Vietnamese diacritics + emoji roundtrip preserved (UTF-8)")
    void shouldHandleVietnameseAndEmoji() {
        String plaintext = "Nghi ngờ xâm hại trẻ em — học sinh lớp 7A — nghiêm trọng 🚨";

        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("long plaintext (10KB description) roundtrips")
    void shouldHandleLongPlaintext() {
        String plaintext = "evidence-line\n".repeat(700); // ~10KB

        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("ciphertext format: [IV(12) | cipher | tag(16)] — minimum 28 bytes overhead")
    void shouldUseExpectedCiphertextLayout() {
        String plaintext = "x"; // 1-byte plaintext
        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);

        // Expected: 12 (IV) + 1 (plaintext) + 16 (auth tag) = 29 bytes
        assertThat(encrypted).hasSize(12 + 1 + 16);
    }

    @Test
    @DisplayName("invalid master key length (16 bytes) rejected at construction")
    void shouldRejectInvalidKeyLength() {
        String invalidKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesGcmAttributeConverter(invalidKey, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("missing master key in prod profile fails fast")
    void shouldFailFastInProdWithoutMasterKey() {
        assertThatThrownBy(() -> new AesGcmAttributeConverter(null, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("encryption.master-key");
    }

    @Test
    @DisplayName("missing master key in dev profile auto-generates ephemeral key (with warning)")
    void shouldGenerateEphemeralKeyInDev() {
        // Should not throw — dev mode tolerates absent key
        AesGcmAttributeConverter devConverter = new AesGcmAttributeConverter(null, "dev");

        String plaintext = "Dev test";
        byte[] encrypted = devConverter.convertToDatabaseColumn(plaintext);
        String decrypted = devConverter.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("ciphertext from one converter instance not decryptable by another (different key)")
    void shouldNotDecryptWithDifferentKey() {
        String plaintext = "Cross-key isolation";
        byte[] encrypted = converter.convertToDatabaseColumn(plaintext);

        // Build a SECOND converter with a different generated key
        AesGcmAttributeConverter otherConverter = new AesGcmAttributeConverter(null, "test");

        assertThatThrownBy(() -> otherConverter.convertToEntityAttribute(encrypted))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("converter is reusable for multiple encrypt/decrypt operations without state leak")
    void shouldBeReusable() {
        for (int i = 0; i < 10; i++) {
            String plaintext = "iteration-" + i;
            byte[] encrypted = converter.convertToDatabaseColumn(plaintext);
            assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo(plaintext);
        }
    }

    @Test
    @DisplayName("ciphertext too short to contain IV+tag rejected")
    void shouldRejectShortCiphertext() {
        byte[] tooShort = new byte[10]; // shorter than IV (12) + tag (16)

        assertThatCode(() -> converter.convertToEntityAttribute(tooShort))
                .isInstanceOf(RuntimeException.class);
    }
}
