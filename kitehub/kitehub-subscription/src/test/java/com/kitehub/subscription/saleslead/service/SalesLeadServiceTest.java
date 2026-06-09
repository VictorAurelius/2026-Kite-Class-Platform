package com.kitehub.subscription.saleslead.service;

import com.kitehub.subscription.saleslead.dto.CreateSalesLeadRequest;
import com.kitehub.subscription.saleslead.entity.SalesLead;
import com.kitehub.subscription.saleslead.repository.SalesLeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SalesLeadService} (GAP-1101).
 *
 * Coverage:
 *  - Persistence happy path with all fields (VN diacritics preserved)
 *  - Default planInterest = ENTERPRISE when null/blank
 *  - Trim whitespace on inputs
 *  - Blank message normalized to null
 *  - status defaults to NEW
 *
 * @since GAP-1101
 */
@ExtendWith(MockitoExtension.class)
class SalesLeadServiceTest {

    @Mock
    private SalesLeadRepository repository;

    private SalesLeadService service;

    @BeforeEach
    void setUp() {
        service = new SalesLeadService(repository);
    }

    @Test
    void shouldPersistLeadWithAllFields() {
        CreateSalesLeadRequest request = new CreateSalesLeadRequest(
                "Nguyễn Văn An",
                "an.nguyen@skyedu.vn",
                "0901 234 567",
                "Trung tâm Anh ngữ Sky Education",
                "Cần tư vấn gói Enterprise cho 3 chi nhánh",
                "ENTERPRISE",
                "");
        when(repository.save(any(SalesLead.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesLead result = service.submit(request, "203.0.113.7");

        ArgumentCaptor<SalesLead> captor = ArgumentCaptor.forClass(SalesLead.class);
        verify(repository).save(captor.capture());
        SalesLead saved = captor.getValue();

        assertThat(saved.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(saved.getEmail()).isEqualTo("an.nguyen@skyedu.vn");
        assertThat(saved.getPhone()).isEqualTo("0901 234 567");
        assertThat(saved.getOrganizationName()).isEqualTo("Trung tâm Anh ngữ Sky Education");
        assertThat(saved.getMessage()).isEqualTo("Cần tư vấn gói Enterprise cho 3 chi nhánh");
        assertThat(saved.getPlanInterest()).isEqualTo("ENTERPRISE");
        assertThat(saved.getStatus()).isEqualTo("NEW");
        assertThat(saved.getClientIp()).isEqualTo("203.0.113.7");
        assertThat(saved.getPublicId()).isNotNull();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void shouldDefaultPlanInterestToEnterpriseWhenNull() {
        CreateSalesLeadRequest request = new CreateSalesLeadRequest(
                "Trần Thị Hồng",
                "hong@quangminh.edu.vn",
                "0987654321",
                "Trung tâm Toán Quang Minh",
                null,
                null,
                "");
        when(repository.save(any(SalesLead.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submit(request, null);

        ArgumentCaptor<SalesLead> captor = ArgumentCaptor.forClass(SalesLead.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPlanInterest()).isEqualTo("ENTERPRISE");
        assertThat(captor.getValue().getMessage()).isNull();
        assertThat(captor.getValue().getClientIp()).isNull();
    }

    @Test
    void shouldTrimInputsAndNormalizeBlankMessage() {
        CreateSalesLeadRequest request = new CreateSalesLeadRequest(
                "  Lê Văn Quang  ",
                "  quang@center.vn  ",
                "  0912345678  ",
                "  Trung tâm Tin học Bách Khoa  ",
                "   ",
                "  PREMIUM  ",
                "");
        when(repository.save(any(SalesLead.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submit(request, null);

        ArgumentCaptor<SalesLead> captor = ArgumentCaptor.forClass(SalesLead.class);
        verify(repository).save(captor.capture());
        SalesLead saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Lê Văn Quang");
        assertThat(saved.getEmail()).isEqualTo("quang@center.vn");
        assertThat(saved.getPhone()).isEqualTo("0912345678");
        assertThat(saved.getOrganizationName()).isEqualTo("Trung tâm Tin học Bách Khoa");
        assertThat(saved.getMessage()).isNull();
        assertThat(saved.getPlanInterest()).isEqualTo("PREMIUM");
    }
}
