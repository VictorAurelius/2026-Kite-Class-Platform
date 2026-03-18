package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 * Used for encrypting database passwords before storage.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits)
    private static final int AES_KEY_SIZE = 256; // bits

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    /**
     * Constructor that initializes the encryption service.
     *
     * @param masterKeyBase64 Base64-encoded master encryption key (32 bytes for AES-256)
     */
    public EncryptionService(
            @Value("${encryption.master-key:#{null}}") String masterKeyBase64,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.secureRandom = new SecureRandom();

        if (masterKeyBase64 == null || masterKeyBase64.isEmpty()) {
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                throw new IllegalStateException(
                    "ENCRYPTION_MASTER_KEY is required in production! " +
                    "Generate with: openssl rand -base64 32");
            }
            log.warn("No master encryption key provided. Generating temporary key (NOT FOR PRODUCTION)");
            this.masterKey = generateTemporaryKey();
        } else {
            byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException("Master key must be 32 bytes (256 bits) for AES-256");
            }
            this.masterKey = new SecretKeySpec(decodedKey, "AES");
            log.info("Encryption service initialized with provided master key");
        }
    }

    /**
     * Encrypt plaintext using AES-256-GCM.
     * Format: [IV (12 bytes)][Encrypted Data][Auth Tag (16 bytes)]
     *
     * @param plainText Text to encrypt
     * @return Base64-encoded encrypted data with IV and auth tag
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Plain text cannot be null or empty");
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

            // Encrypt
            byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] cipherText = cipher.doFinal(plainTextBytes);

            // Combine IV + ciphertext (which includes auth tag)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            // Encode to Base64 for storage
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt ciphertext using AES-256-GCM.
     *
     * @param cipherTextBase64 Base64-encoded encrypted data (IV + ciphertext + auth tag)
     * @return Decrypted plaintext
     * @throws RuntimeException if decryption fails
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isEmpty()) {
            throw new IllegalArgumentException("Cipher text cannot be null or empty");
        }

        try {
            // Decode from Base64
            byte[] cipherMessage = Base64.getDecoder().decode(cipherTextBase64);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

            // Decrypt
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Generate temporary AES-256 key for development/testing.
     * WARNING: Do not use in production.
     *
     * @return Generated secret key
     */
    private SecretKey generateTemporaryKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate temporary key", e);
        }
    }

    /**
     * Generate a new random master key.
     * Use this to generate a production master key, then store it securely.
     *
     * @return Base64-encoded 256-bit AES key
     */
    public static String generateMasterKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate master key", e);
        }
    }
}
