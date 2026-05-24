package com.kitehub.email.template;

import com.kitehub.email.template.PersonaToneResolver.Persona;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link PersonaToneResolver} (Wave beta-readiness-4 Bucket E — GAP-NEW-email-tone-matrix).
 *
 * <p>Verifies 5-persona greeting matrix per vn-localization-audit-checklist.md §2 Section 2:
 * <ul>
 *   <li>{@link Persona#P1_SOLO_TEACHER} → "Chào em," (casual, friendly)</li>
 *   <li>{@link Persona#P2_CENTER_OWNER} → "Em chào chị," (formal-respectful Owner)</li>
 *   <li>{@link Persona#P3_CENTER_MANAGER} → "Em chào chị/anh," (formal, neutral)</li>
 *   <li>{@link Persona#PARENT} → "Kính gửi quý phụ huynh," (very formal)</li>
 *   <li>{@link Persona#STUDENT} → "Chào em," (friendly)</li>
 *   <li>{@link Persona#PLATFORM_ADMIN} → "Em chào anh/chị," (formal — default)</li>
 * </ul>
 *
 * @author KiteHub Email Team
 * @since Wave beta-readiness-4 Bucket E (GAP-NEW-email-tone-matrix)
 */
class PersonaToneResolverTest {

    private final PersonaToneResolver resolver = new PersonaToneResolver();

    @Test
    void resolveGreeting_P1SoloTeacher_returnsCasualEm() {
        assertThat(resolver.resolveGreeting(Persona.P1_SOLO_TEACHER))
                .isEqualTo("Chào em,");
    }

    @Test
    void resolveGreeting_P2CenterOwner_returnsFormalChi() {
        assertThat(resolver.resolveGreeting(Persona.P2_CENTER_OWNER))
                .isEqualTo("Em chào chị,");
    }

    @Test
    void resolveGreeting_P3CenterManager_returnsFormalChiAnh() {
        assertThat(resolver.resolveGreeting(Persona.P3_CENTER_MANAGER))
                .isEqualTo("Em chào chị/anh,");
    }

    @Test
    void resolveGreeting_Parent_returnsVeryFormalKinhGui() {
        assertThat(resolver.resolveGreeting(Persona.PARENT))
                .isEqualTo("Kính gửi quý phụ huynh,");
    }

    @Test
    void resolveGreeting_Student_returnsFriendlyEm() {
        assertThat(resolver.resolveGreeting(Persona.STUDENT))
                .isEqualTo("Chào em,");
    }

    @Test
    void resolveGreeting_PlatformAdmin_returnsFormalNeutral() {
        assertThat(resolver.resolveGreeting(Persona.PLATFORM_ADMIN))
                .isEqualTo("Em chào anh/chị,");
    }

    @Test
    void resolveGreeting_nullPersona_returnsDefaultFormalNeutral() {
        // Safe-default: null persona = formal-neutral (avoid casual leak to unknown recipient)
        assertThat(resolver.resolveGreeting(null))
                .isEqualTo(PersonaToneResolver.DEFAULT_GREETING)
                .isEqualTo("Em chào anh/chị,");
    }

    @Test
    void resolveGreetingByName_validUppercaseName_returnsExpectedGreeting() {
        assertThat(resolver.resolveGreetingByName("PARENT"))
                .isEqualTo("Kính gửi quý phụ huynh,");
        assertThat(resolver.resolveGreetingByName("P2_CENTER_OWNER"))
                .isEqualTo("Em chào chị,");
    }

    @Test
    void resolveGreetingByName_lowercaseName_caseInsensitiveMatch() {
        // Common case: persona name from RabbitMQ payload may be lowercase
        assertThat(resolver.resolveGreetingByName("parent"))
                .isEqualTo("Kính gửi quý phụ huynh,");
        assertThat(resolver.resolveGreetingByName("p1_solo_teacher"))
                .isEqualTo("Chào em,");
    }

    @Test
    void resolveGreetingByName_unknownName_returnsDefault() {
        // Unknown enum name → safe-default; never throw
        assertThat(resolver.resolveGreetingByName("SUPER_ADMIN"))
                .isEqualTo(PersonaToneResolver.DEFAULT_GREETING);
        assertThat(resolver.resolveGreetingByName("typo"))
                .isEqualTo(PersonaToneResolver.DEFAULT_GREETING);
    }

    @Test
    void resolveGreetingByName_nullOrBlank_returnsDefault() {
        assertThat(resolver.resolveGreetingByName(null))
                .isEqualTo(PersonaToneResolver.DEFAULT_GREETING);
        assertThat(resolver.resolveGreetingByName(""))
                .isEqualTo(PersonaToneResolver.DEFAULT_GREETING);
        assertThat(resolver.resolveGreetingByName("   "))
                .isEqualTo(PersonaToneResolver.DEFAULT_GREETING);
    }

    @Test
    void allGreetings_areNonEmptyVietnamese_noEnglishPlaceholder() {
        // VN-localization audit guard — verify every greeting is non-empty Vietnamese, no English fallback
        for (Persona persona : Persona.values()) {
            String greeting = resolver.resolveGreeting(persona);
            assertThat(greeting)
                    .as("Greeting for persona %s must be non-empty Vietnamese", persona)
                    .isNotNull()
                    .isNotBlank()
                    .doesNotContain("Hi")
                    .doesNotContain("Hello")
                    .doesNotContain("Dear")
                    .endsWith(",");
        }
    }
}
