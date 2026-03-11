package com.kiteclass.core.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Utility class for creating test data fixtures in integration tests.
 *
 * <p>Provides helper methods to create common test entities (teachers, courses, students)
 * with reasonable defaults, reducing boilerplate in integration tests.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * @Autowired
 * private TestDataBuilder testDataBuilder;
 *
 * @BeforeEach
 * void setUp() {
 *     tenantId = UUID.randomUUID();
 *     teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
 * }
 * }</pre>
 *
 * @author KiteClass Team
 * @since 2.13
 */
@Component
public class TestDataBuilder {

    /**
     * Creates a test teacher with default values.
     *
     * <p>Default teacher profile:
     * <ul>
     *     <li>Name: "Test Teacher"</li>
     *     <li>Email: "test.teacher@kiteclass.test" (unique with timestamp)</li>
     *     <li>Phone: "+84900000000"</li>
     *     <li>Status: ACTIVE</li>
     *     <li>Specialization: "Computer Science"</li>
     * </ul>
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return the created teacher's ID
     * @throws Exception if teacher creation fails
     */
    public Long createTestTeacher(MockMvc mockMvc, ObjectMapper objectMapper, UUID tenantId) throws Exception {
        return createTestTeacher(mockMvc, objectMapper, tenantId, "Test Teacher", "Computer Science");
    }

    /**
     * Creates a test teacher with custom name and specialization.
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param name Teacher's full name
     * @param specialization Teacher's specialization/subject area
     * @return the created teacher's ID
     * @throws Exception if teacher creation fails
     */
    public Long createTestTeacher(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            String name,
            String specialization
    ) throws Exception {
        // Generate unique email with timestamp to avoid conflicts in parallel tests
        String email = "teacher." + System.currentTimeMillis() + "@kiteclass.test";

        CreateTeacherRequest teacherRequest = new CreateTeacherRequest(
                name,                           // name
                email,                          // email
                "+84900000000",                 // phoneNumber
                specialization,                 // specialization
                "Experienced educator",         // bio
                "Bachelor of Education",        // qualification
                5                               // experienceYears
        );

        MvcResult result = mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(teacherRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    /**
     * Creates multiple test teachers for bulk testing scenarios.
     *
     * <p>Each teacher will have:
     * <ul>
     *     <li>Unique email (teacher1@test.com, teacher2@test.com, ...)</li>
     *     <li>Sequential names (Teacher 1, Teacher 2, ...)</li>
     *     <li>Different specializations if count {@literal >} 3</li>
     * </ul>
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param count Number of teachers to create
     * @return array of created teacher IDs
     * @throws Exception if teacher creation fails
     */
    public Long[] createMultipleTeachers(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            int count
    ) throws Exception {
        Long[] teacherIds = new Long[count];
        String[] specializations = {"Computer Science", "Mathematics", "Physics", "Chemistry", "Biology"};

        for (int i = 0; i < count; i++) {
            String specialization = specializations[i % specializations.length];
            teacherIds[i] = createTestTeacher(
                    mockMvc,
                    objectMapper,
                    tenantId,
                    "Teacher " + (i + 1),
                    specialization
            );
        }

        return teacherIds;
    }
}
