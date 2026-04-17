package com.kitehub.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for EncryptionService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Generate a test master key
        String masterKey = EncryptionService.generateMasterKey();
        encryptionService = new EncryptionService(masterKey, "test");
    }

    @Test
    void shouldEncryptAndDecryptPassword() {
        // Given
        String plainPassword = "MySecurePassword123!@#";

        // When
        String encrypted = encryptionService.encrypt(plainPassword);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(encrypted).isNotEqualTo(plainPassword);
        assertThat(encrypted).isNotEmpty();
        assertThat(decrypted).isEqualTo(plainPassword);
    }

    @Test
    void shouldProduceDifferentCiphertextForSameInput() {
        // Given
        String plainPassword = "TestPassword";

        // When - encrypt same password twice
        String encrypted1 = encryptionService.encrypt(plainPassword);
        String encrypted2 = encryptionService.encrypt(plainPassword);

        // Then - ciphertexts should differ due to random IV
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both should decrypt to same plaintext
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plainPassword);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plainPassword);
    }

    @Test
    void shouldThrowExceptionForNullPlaintext() {
        // When/Then
        assertThatThrownBy(() -> encryptionService.encrypt(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plain text cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForEmptyPlaintext() {
        // When/Then
        assertThatThrownBy(() -> encryptionService.encrypt(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plain text cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForNullCiphertext() {
        // When/Then
        assertThatThrownBy(() -> encryptionService.decrypt(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cipher text cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForEmptyCiphertext() {
        // When/Then
        assertThatThrownBy(() -> encryptionService.decrypt(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cipher text cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForInvalidCiphertext() {
        // When/Then
        assertThatThrownBy(() -> encryptionService.decrypt("InvalidBase64!@#"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to decrypt data");
    }

    @Test
    void shouldThrowExceptionForTamperedCiphertext() {
        // Given
        String plainPassword = "TestPassword";
        String encrypted = encryptionService.encrypt(plainPassword);

        // Tamper with the ciphertext
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";

        // When/Then - decryption should fail due to auth tag mismatch
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to decrypt data");
    }

    @Test
    void shouldHandleLongPasswords() {
        // Given - 256 character password
        String longPassword = "a".repeat(256);

        // When
        String encrypted = encryptionService.encrypt(longPassword);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(longPassword);
    }

    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String passwordWithSpecialChars = "P@ssw0rd!#$%^&*(){}[]|\\:;\"'<>,.?/~`+-=_";

        // When
        String encrypted = encryptionService.encrypt(passwordWithSpecialChars);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(passwordWithSpecialChars);
    }

    @Test
    void shouldHandleUnicodeCharacters() {
        // Given
        String unicodePassword = "Mật khẩu 密码 パスワード";

        // When
        String encrypted = encryptionService.encrypt(unicodePassword);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(unicodePassword);
    }

    @Test
    void shouldGenerateValidMasterKey() {
        // When
        String masterKey = EncryptionService.generateMasterKey();

        // Then
        assertThat(masterKey).isNotNull();
        assertThat(masterKey).isNotEmpty();

        // Should be valid Base64
        assertThatCode(() -> java.util.Base64.getDecoder().decode(masterKey))
            .doesNotThrowAnyException();

        // Decoded key should be 32 bytes (256 bits)
        byte[] decodedKey = java.util.Base64.getDecoder().decode(masterKey);
        assertThat(decodedKey).hasSize(32);
    }

    @Test
    void shouldCreateServiceWithoutMasterKeyForDevelopment() {
        // When - create service without master key
        EncryptionService devService = new EncryptionService(null, "dev");

        // Then - should still work with temporary key
        String plainPassword = "TestPassword";
        String encrypted = devService.encrypt(plainPassword);
        String decrypted = devService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plainPassword);
    }

    @Test
    void shouldRejectInvalidMasterKeyLength() {
        // Given - 16 byte key (128 bits) instead of 32 bytes (256 bits)
        String invalidKey = java.util.Base64.getEncoder().encodeToString(new byte[16]);

        // When/Then
        assertThatThrownBy(() -> new EncryptionService(invalidKey, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Master key must be 32 bytes");
    }
}
