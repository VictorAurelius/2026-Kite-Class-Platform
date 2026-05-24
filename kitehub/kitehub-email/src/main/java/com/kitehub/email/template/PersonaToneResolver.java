package com.kitehub.email.template;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves persona-specific greeting strings for email templates (Wave beta-readiness-4 Bucket E).
 *
 * <p>Per {@code vn-localization-audit-checklist.md} §2 Section 2 — Vietnamese persona tone matrix
 * mandates 5 greeting variants for tenant-facing emails. Centralizing the mapping here ensures
 * cross-bucket consistency (Bucket D class-reschedule + Bucket B PDPL consent + Bucket A welcome
 * sequence all consume same source-of-truth instead of inline hardcoding per template).
 *
 * <h2>Greeting matrix</h2>
 * <table>
 *   <tr><th>Persona</th><th>Greeting</th><th>Tone</th></tr>
 *   <tr><td>{@link Persona#P1_SOLO_TEACHER}</td><td>Chào em,</td><td>Casual, friendly</td></tr>
 *   <tr><td>{@link Persona#P2_CENTER_OWNER}</td><td>Em chào chị,</td><td>Formal-respectful</td></tr>
 *   <tr><td>{@link Persona#P3_CENTER_MANAGER}</td><td>Em chào chị/anh,</td><td>Formal-respectful, neutral gender</td></tr>
 *   <tr><td>{@link Persona#PARENT}</td><td>Kính gửi quý phụ huynh,</td><td>Very formal</td></tr>
 *   <tr><td>{@link Persona#STUDENT}</td><td>Chào em,</td><td>Friendly</td></tr>
 *   <tr><td>{@link Persona#PLATFORM_ADMIN}</td><td>Em chào anh/chị,</td><td>Formal — internal</td></tr>
 * </table>
 *
 * <h2>Usage</h2>
 * Inject in EmailService classes when building Thymeleaf template context:
 * <pre>{@code
 *   Map<String, Object> ctx = new HashMap<>();
 *   ctx.put("personaGreeting", personaToneResolver.resolveGreeting(Persona.PARENT));
 *   // ... other vars ...
 *   templateRenderer.render("class-rescheduled", ctx, Tone.FORMAL_AUTHORITY);
 * }</pre>
 *
 * <p>Templates consume via Thymeleaf fragment:
 * <pre>{@code
 *   <th:block th:replace="~{_shared/persona-tone :: greeting}" />
 * }</pre>
 *
 * <p>Unknown persona OR null → defaults to {@link Persona#PLATFORM_ADMIN} formal-neutral greeting
 * (safe-default — avoids accidentally casual greeting to unknown recipient).
 *
 * @author KiteHub Email Team
 * @since Wave beta-readiness-4 Bucket E (GAP-NEW-email-tone-matrix)
 */
@Component
public class PersonaToneResolver {

    /**
     * Default greeting when persona unknown or null — formal-neutral, internal tone.
     * Choosing PLATFORM_ADMIN formal as safe-default avoids casual greeting leak to
     * unknown recipient (vd: spam pipeline, mis-routed email).
     */
    public static final String DEFAULT_GREETING = "Em chào anh/chị,";

    /**
     * Immutable persona → greeting mapping per vn-localization-audit-checklist.md §2 Section 2.
     * EnumMap chosen vì faster lookup + null-key rejection (safety).
     */
    private static final Map<Persona, String> PERSONA_GREETING;

    static {
        Map<Persona, String> m = new EnumMap<>(Persona.class);
        m.put(Persona.P1_SOLO_TEACHER, "Chào em,");
        m.put(Persona.P2_CENTER_OWNER, "Em chào chị,");
        m.put(Persona.P3_CENTER_MANAGER, "Em chào chị/anh,");
        m.put(Persona.PARENT, "Kính gửi quý phụ huynh,");
        m.put(Persona.STUDENT, "Chào em,");
        m.put(Persona.PLATFORM_ADMIN, DEFAULT_GREETING);
        PERSONA_GREETING = Map.copyOf(m);
    }

    /**
     * Resolve persona-specific Vietnamese greeting string.
     *
     * @param persona recipient persona; null → returns {@link #DEFAULT_GREETING}
     * @return greeting string (vd: "Kính gửi quý phụ huynh,"); never null/empty
     */
    public String resolveGreeting(Persona persona) {
        if (persona == null) {
            return DEFAULT_GREETING;
        }
        return PERSONA_GREETING.getOrDefault(persona, DEFAULT_GREETING);
    }

    /**
     * Resolve persona-specific greeting from string-based persona role identifier
     * (vd: parsed from JWT claim or RabbitMQ payload).
     *
     * <p>Case-insensitive matching against enum constant names. Unknown / null → returns
     * {@link #DEFAULT_GREETING}.
     *
     * @param personaName persona enum name string (vd: "PARENT", "P2_CENTER_OWNER"); null OK
     * @return greeting string; never null/empty
     */
    public String resolveGreetingByName(String personaName) {
        if (personaName == null || personaName.isBlank()) {
            return DEFAULT_GREETING;
        }
        try {
            Persona persona = Persona.valueOf(personaName.toUpperCase());
            return resolveGreeting(persona);
        } catch (IllegalArgumentException ex) {
            return DEFAULT_GREETING;
        }
    }

    /**
     * Persona enumeration covering all tenant-facing email recipient types in Phase 1 BETA.
     *
     * <p>Naming aligns với BRD persona slugs (P1/P2/P3) + role-string conventions used elsewhere
     * (vd: kitehub-platform role seeding, kiteclass-core enrollment metadata).
     */
    public enum Persona {
        /** Solo Teacher tenant — individual freelance instructor (P1 BRD). */
        P1_SOLO_TEACHER,
        /** Center Owner tenant — trung tâm dạy thêm proprietor (P2 BRD). */
        P2_CENTER_OWNER,
        /** Center Manager — trung tâm operational manager (P3 BRD). */
        P3_CENTER_MANAGER,
        /** Parent recipient — phụ huynh học sinh (operational notification scope). */
        PARENT,
        /** Student recipient — học sinh (limited operational scope Phase 1 BETA). */
        STUDENT,
        /** Platform Admin — internal KiteHub team (audit notifications, system alerts). */
        PLATFORM_ADMIN
    }
}
