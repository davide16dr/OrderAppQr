package com.orderapp.ordering.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderapp.ordering.dto.BusinessSignupRequest;
import com.orderapp.ordering.dto.BusinessSignupResponse;
import com.orderapp.ordering.entity.BusinessRegistrationRequest;
import com.orderapp.ordering.entity.StaffRole;
import com.orderapp.ordering.entity.StaffUser;
import com.orderapp.ordering.entity.StaffUserRole;
import com.orderapp.ordering.entity.SubscriptionPlan;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.BusinessRegistrationRequestRepository;
import com.orderapp.ordering.repository.StaffRoleRepository;
import com.orderapp.ordering.repository.StaffUserRepository;
import com.orderapp.ordering.repository.StaffUserRoleRepository;
import com.orderapp.ordering.repository.SubscriptionPlanRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessRegistrationService Unit Tests")
class BusinessRegistrationServiceTest {

    @Mock
    private BusinessRegistrationRequestRepository registrationRequestRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private StaffUserRepository staffUserRepository;

    @Mock
    private StaffUserRoleRepository staffUserRoleRepository;

    @Mock
    private StaffRoleRepository staffRoleRepository;

    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Mock
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BusinessRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new BusinessRegistrationService(
                registrationRequestRepository,
                tenantRepository,
                staffUserRepository,
                staffUserRoleRepository,
                staffRoleRepository,
                tenantSubscriptionRepository,
                subscriptionPlanRepository,
                passwordEncoder,
                temporaryPasswordGenerator,
                emailService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should create tenant, staff user and subscription during business signup")
    void testSubmitBusinessRegistrationCreatesTenant() {
        BusinessSignupRequest request = BusinessSignupRequest.builder()
                .tenantName("Acme Bistro")
                .legalName("Acme Bistro S.r.l.")
                .companyLogoDataUrl(null)
                .businessType("RESTAURANT")
                .vatNumber("IT12345678901")
                .businessEmail("info@acme-bistro.it")
                .businessPhone("+39 0123 456789")
                .addressLine1("Via Roma 1")
                .addressLine2("Piano Terra")
                .city("Roma")
                .province("RM")
                .postalCode("00100")
                .country("IT")
                .requestedSlug("acme-bistro")
                .requestedPlanCode("basic-plan")
                .contactFirstName("Mario")
                .contactLastName("Rossi")
                .contactEmail("owner@acme-bistro.it")
                .contactPhone("+39 333 1234567")
                .billingCycle("MONTHLY")
                .paymentMethod("BANK_TRANSFER")
                .build();

        SubscriptionPlan subscriptionPlan = SubscriptionPlan.builder()
                .code("basic-plan")
                .name("Basic")
                .priceMonthly(BigDecimal.valueOf(29.90))
                .isActive(true)
                .build();

        StaffRole managerRole = StaffRole.builder()
                .id(1L)
                .code("MANAGER")
                .description("Manager")
                .build();

        when(tenantRepository.findBySlugIgnoreCase("acme-bistro")).thenReturn(Optional.empty());
        when(tenantRepository.findBySubdomainIgnoreCase("acme-bistro")).thenReturn(Optional.empty());
        when(registrationRequestRepository.findByRequestedSlugIgnoreCase("acme-bistro")).thenReturn(Optional.empty());
        when(staffUserRepository.findByEmailIgnoreCase("owner@acme-bistro.it")).thenReturn(Optional.empty());
        when(temporaryPasswordGenerator.generateDefault()).thenReturn("temporary-pass");
        when(passwordEncoder.encode("temporary-pass")).thenReturn("hashed-pass");
        when(staffRoleRepository.findByCode("MANAGER")).thenReturn(Optional.of(managerRole));
        when(subscriptionPlanRepository.findById("basic-plan")).thenReturn(Optional.of(subscriptionPlan));
        when(emailService.sendTemporaryPasswordEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(42L);
            return tenant;
        });

        when(staffUserRepository.save(any(StaffUser.class))).thenAnswer(invocation -> {
            StaffUser staffUser = invocation.getArgument(0);
            staffUser.setId(77L);
            return staffUser;
        });

        when(staffUserRoleRepository.save(any(StaffUserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantSubscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRequestRepository.save(any(BusinessRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessSignupResponse response = service.submitBusinessRegistration(request);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        org.mockito.Mockito.verify(tenantRepository).save(tenantCaptor.capture());

        Tenant savedTenant = tenantCaptor.getValue();
        assertNotNull(savedTenant);
        assertEquals("acme-bistro", savedTenant.getSlug());
        assertEquals("acme-bistro", savedTenant.getSubdomain());
        assertEquals("Acme Bistro", savedTenant.getName());
        assertEquals("ACTIVE", savedTenant.getStatus());
        assertEquals("SELF_SIGNUP", savedTenant.getRegistrationSource());
        assertEquals("IT", savedTenant.getCountry());

        assertNotNull(response);
        assertEquals(42L, response.getTenantId());
        assertEquals("acme-bistro", response.getTenantSlug());
        assertEquals("ACTIVE", response.getTenantStatus());
        assertTrue(response.getMessage().startsWith("Registrazione completata"));
    }
}