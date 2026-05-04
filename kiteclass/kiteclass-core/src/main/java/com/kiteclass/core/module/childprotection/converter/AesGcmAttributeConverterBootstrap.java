package com.kiteclass.core.module.childprotection.converter;

/**
 * Static bridge between the Spring-managed {@link AesGcmAttributeConverter}
 * singleton (which holds the master key) and the JPA-instantiated converter
 * instances (which Hibernate creates via reflection no-arg constructor).
 *
 * <p>JPA's {@code AttributeConverter} is instantiated by the JPA provider
 * outside Spring's container, so {@code @Autowired} cannot inject the master
 * key. This bootstrap registers the Spring-built singleton at application
 * start and lets JPA-instantiated copies delegate to it.
 *
 * <p>Thread-safety: registration is idempotent under Spring's single-threaded
 * application context refresh; reads happen during JPA query/persist on the
 * application's transaction threads after the application is started.
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
final class AesGcmAttributeConverterBootstrap {

    private static volatile AesGcmAttributeConverter INSTANCE;

    private AesGcmAttributeConverterBootstrap() {
        // utility — no instances
    }

    static synchronized void register(AesGcmAttributeConverter instance) {
        INSTANCE = instance;
    }

    /**
     * Look up the registered Spring-managed singleton. Throws if the
     * application context has not yet wired the converter — fail-loud rather
     * than silently fall back to an ephemeral key.
     */
    static AesGcmAttributeConverter required() {
        AesGcmAttributeConverter ref = INSTANCE;
        if (ref == null) {
            throw new IllegalStateException(
                    "AesGcmAttributeConverter Spring singleton not registered yet — " +
                            "ensure ApplicationContext has refreshed before any encrypted " +
                            "Incident column is read or written.");
        }
        return ref;
    }

    /**
     * Test-only reset hook to clear the static singleton between test classes
     * that exercise different keys.
     */
    static synchronized void reset() {
        INSTANCE = null;
    }
}
