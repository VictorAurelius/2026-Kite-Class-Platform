package com.kiteclass.core.module.childprotection.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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
 * AES-256-GCM JPA AttributeConverter for field-level encryption of sensitive
 * Incident columns (description, evidence_paths) — GAP-322 Phase 1A.
 *
 * <p><b>Encryption design</b> (per `ai-branding-guidelines.md` §1 + ADR pattern
 * shared with {@code kitehub-subscription EncryptionService}):
 * <ul>
 *   <li>Algorithm: {@code AES/GCM/NoPadding}</li>
 *   <li>Key size: 256 bits (32 bytes), Base64-encoded in
 *       {@code encryption.master-key} property</li>
 *   <li>IV size: 96 bits (12 bytes) per NIST SP 800-38D recommendation</li>
 *   <li>Auth tag: 128 bits (16 bytes), appended by JCA</li>
 *   <li>Ciphertext layout: {@code [IV(12) | cipher | auth_tag(16)]} — stored as
 *       BYTEA in PostgreSQL</li>
 *   <li>Per-field random IV from {@link SecureRandom} — defends against
 *       chosen-plaintext + pattern analysis (BR-CHILD-PROT-002)</li>
 *   <li>Tamper detection: GCM auth tag verification on decrypt — corrupted
 *       ciphertext or IV throws (BR-CHILD-PROT-003)</li>
 * </ul>
 *
 * <p><b>Lifecycle:</b> Hibernate instantiates AttributeConverter via no-arg
 * reflection — but a Spring-managed singleton must hold the master key. The
 * {@code @Configuration}-qualified default constructor reads
 * {@code encryption.master-key} from environment via the static bootstrap hook
 * (set by {@link com.kiteclass.core.module.childprotection.converter.AesGcmAttributeConverterBootstrap}).
 * For unit tests, pass key + profile directly to the explicit constructor.
 *
 * <p><b>Null handling:</b> null plaintext maps to null DB column, and null DB
 * column maps to null entity attribute — preserves Java null semantics for
 * optional encrypted fields.
 *
 * <p><b>Compliance:</b> implements PDPL Decree 13/2023 Art 16 special-protection
 * encryption-at-rest requirement for child personal data.
 *
 * @see com.kiteclass.core.module.childprotection.entity.Incident
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@Slf4j
@Converter(autoApply = false)
@Configuration
public class AesGcmAttributeConverter implements AttributeConverter<String, byte[]> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int AES_KEY_SIZE_BITS = 256;
    private static final int AES_KEY_SIZE_BYTES = 32;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    /**
     * Spring-managed constructor — reads {@code encryption.master-key} property
     * + active profile. Used as the singleton converter bean and (transparently
     * via {@link AesGcmAttributeConverterBootstrap}) by Hibernate's converter
     * registry.
     *
     * @param masterKeyBase64 32-byte AES-256 key, Base64-encoded; null/empty
     *                        triggers ephemeral-key path in dev/test only
     * @param activeProfile   Spring active profile — "prod"/"production" require
     *                        explicit master key (fail-fast)
     */
    public AesGcmAttributeConverter(
            @Value("${encryption.master-key:#{null}}") String masterKeyBase64,
            @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.secureRandom = new SecureRandom();

        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                throw new IllegalStateException(
                        "encryption.master-key is REQUIRED in production for child-protection " +
                                "Incident encryption (PDPL Decree 13/2023 Art 16). " +
                                "Generate with: openssl rand -base64 32"
                );
            }
            log.warn("No encryption.master-key provided (profile={}). Generating EPHEMERAL key — " +
                    "encrypted Incident rows will NOT be readable across application restarts.",
                    activeProfile);
            this.masterKey = generateEphemeralKey();
        } else {
            byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
            if (decodedKey.length != AES_KEY_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        "encryption.master-key must decode to 32 bytes (256-bit AES key); got "
                                + decodedKey.length + " bytes"
                );
            }
            this.masterKey = new SecretKeySpec(decodedKey, "AES");
            log.info("AesGcmAttributeConverter initialized with provided master key (profile={})",
                    activeProfile);
        }

        // Register this as the singleton instance for Hibernate-instantiated copies
        // to delegate to (see static bootstrap hook below).
        AesGcmAttributeConverterBootstrap.register(this);
    }

    /**
     * No-arg constructor required by JPA reflection. Delegates to the
     * Spring-bootstrapped singleton — production runtime path. Throws if
     * Spring context has not yet initialized the converter (prevents silent
     * fallback to ephemeral key during JPA bootstrap).
     */
    public AesGcmAttributeConverter() {
        AesGcmAttributeConverter delegate = AesGcmAttributeConverterBootstrap.required();
        this.masterKey = delegate.masterKey;
        this.secureRandom = delegate.secureRandom;
    }

    /**
     * Encrypt entity-attribute plaintext to DB BYTEA column.
     * Layout: [IV(12) | cipher | auth_tag(16)].
     *
     * @param attribute plaintext (may be null)
     * @return ciphertext bytes, or null if attribute was null
     */
    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            byte[] plainBytes = attribute.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(plainBytes); // includes auth tag

            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buf.put(iv);
            buf.put(cipherBytes);
            return buf.array();
        } catch (Exception e) {
            log.error("Failed to encrypt Incident field", e);
            throw new RuntimeException("Failed to encrypt sensitive field", e);
        }
    }

    /**
     * Decrypt DB BYTEA column back to entity-attribute plaintext.
     * GCM auth tag verification rejects tampered ciphertext or IV.
     *
     * @param dbData ciphertext bytes (may be null)
     * @return plaintext, or null if dbData was null
     */
    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw new RuntimeException(
                    "Failed to decrypt sensitive field: ciphertext too short (" + dbData.length + " bytes)");
        }
        try {
            ByteBuffer buf = ByteBuffer.wrap(dbData);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buf.get(iv);
            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Don't log dbData contents — even ciphertext leaks via logs are
            // analytically informative to attackers (size/IV patterns).
            log.error("Failed to decrypt Incident field (tamper or wrong key)");
            throw new RuntimeException("Failed to decrypt sensitive field", e);
        }
    }

    private static SecretKey generateEphemeralKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(AES_KEY_SIZE_BITS);
            return kg.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ephemeral AES key", e);
        }
    }
}
