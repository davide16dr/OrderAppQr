package com.orderapp.ordering.service;

import java.time.OffsetDateTime;
import java.util.Arrays;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderapp.ordering.dto.BusinessSignupRequest;
import com.orderapp.ordering.dto.BusinessSignupResponse;
import com.orderapp.ordering.entity.BusinessRegistrationRequest;
import com.orderapp.ordering.entity.StaffRole;
import com.orderapp.ordering.entity.StaffUser;
import com.orderapp.ordering.entity.StaffUserRole;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.entity.SubscriptionPlan;
import com.orderapp.ordering.repository.BusinessRegistrationRequestRepository;
import com.orderapp.ordering.repository.StaffRoleRepository;
import com.orderapp.ordering.repository.StaffUserRepository;
import com.orderapp.ordering.repository.StaffUserRoleRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.orderapp.ordering.repository.SubscriptionPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessRegistrationService {

    private final BusinessRegistrationRequestRepository registrationRequestRepository;
    private final TenantRepository tenantRepository;
    private final StaffUserRepository staffUserRepository;
    private final StaffUserRoleRepository staffUserRoleRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    /**
     * Registra una nuova azienda nel flusso di self-signup
     * Crea: tenant (PENDING), staff_user (contatto primario), subscription, business_registration_request
     * 
     * @param request la richiesta di registrazione
     * @return la risposta della registrazione
     * @throws IllegalArgumentException se i dati non sono validi
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    @CacheEvict(value = "allTenants", allEntries = true)
    public BusinessSignupResponse submitBusinessRegistration(BusinessSignupRequest request) {
        log.info("Processing business registration for slug: {}", request.getRequestedSlug());

        // 1. Validazioni preliminari
        validateRegistrationRequest(request);

        // 2. Verificare che lo slug e il subdomain non siano già in uso
        if (tenantRepository.findBySlugIgnoreCase(request.getRequestedSlug()).isPresent()) {
            throw new IllegalArgumentException("Lo slug richiesto è già in uso. Scegliere uno slug diverso.");
        }

        if (tenantRepository.findBySubdomainIgnoreCase(request.getRequestedSlug()).isPresent()) {
            throw new IllegalArgumentException("Il subdomain è già in uso.");
        }

        // 3. Verificare se esiste già una richiesta per questo slug
        if (registrationRequestRepository.findByRequestedSlugIgnoreCase(request.getRequestedSlug()).isPresent()) {
            throw new IllegalArgumentException("Esiste già una richiesta di registrazione per questo slug.");
        }

        // 4. Verificare se l'email aziendale è già in uso da un altro tenant
        if (tenantRepository.findByBusinessEmailIgnoreCase(request.getBusinessEmail()).isPresent()) {
            throw new IllegalArgumentException("L'email aziendale è già associata a un'altra attività registrata.");
        }

        // 5. Verificare se l'email del contatto è già registrata come staff user
        if (staffUserRepository.findByEmailIgnoreCase(request.getContactEmail()).isPresent()) {
            throw new IllegalArgumentException("L'email del contatto è già associata a un account.");
        }

        // 5. Genera password temporanea e hash
        String temporaryPassword = temporaryPasswordGenerator.generateDefault();
        String passwordHash = passwordEncoder.encode(temporaryPassword);

        // 6. Creare il TENANT — sempre ACTIVE con prova gratuita 14 giorni.
        // paymentMethod (CARD/BANK_TRANSFER) determina solo come verrà gestito il rinnovo:
        // CARD → l'utente inserirà la carta dalla dashboard → pagamenti automatici
        // BANK_TRANSFER → rinnovo gestito manualmente dall'admin
        Tenant tenant = Tenant.builder()
                .slug(request.getRequestedSlug())
                .subdomain(request.getRequestedSlug())
                .name(request.getTenantName())
                .legalName(request.getLegalName())
                .businessType(request.getBusinessType())
                .status("ACTIVE")
                .enabled(true)
                .activationDate(OffsetDateTime.now())
                .timezone("Europe/Rome")
                .currencyCode("EUR")
                .vatNumber(request.getVatNumber())
                .sid(request.getSid())
                .businessEmail(request.getBusinessEmail())
                .businessPhone(request.getBusinessPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .registrationSource("SELF_SIGNUP")
                .brandingJson(buildBrandingJson(request))
                .openingConfigJson("{}")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Tenant created with ID: {} status={} paymentMethod={}", savedTenant.getId(), savedTenant.getStatus(), request.getPaymentMethod());
        log.info("Tenant branding info: logoPresent={}", hasLogo(request.getCompanyLogoDataUrl()));

        // 7. Creare lo STAFF_USER principale (contatto primario)
        StaffUser staffUser = StaffUser.builder()
                .tenantId(savedTenant.getId())
                .firstName(request.getContactFirstName())
                .lastName(request.getContactLastName())
                .email(request.getContactEmail())
                .passwordHash(passwordHash)
                .phone(request.getContactPhone())
                .isPrimaryContact(true)
                .activatedAt(OffsetDateTime.now())
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        StaffUser savedStaffUser = staffUserRepository.save(staffUser);
        log.info("Primary contact staff user created with ID: {} for tenant: {}", savedStaffUser.getId(), savedTenant.getId());

        // 8. Assegnare il ruolo MANAGER allo staff user
        assignManagerRole(savedStaffUser);

        // 9. Creare la TENANT_SUBSCRIPTION — sempre TRIAL 14 giorni
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(request.getRequestedPlanCode())
                .orElseThrow(() -> new IllegalArgumentException("Piano di sottoscrizione non trovato: " + request.getRequestedPlanCode()));

        OffsetDateTime trialEnd = OffsetDateTime.now().plusDays(14);
        TenantSubscription subscription = TenantSubscription.builder()
                .tenant(savedTenant)
                .subscriptionPlan(subscriptionPlan)
                .status("TRIAL")
                .billingCycle("MONTHLY")
                .paymentStatus("NONE")
                .trialEndsAt(trialEnd)
                .currentPeriodStart(OffsetDateTime.now())
                .currentPeriodEnd(trialEnd)
                .activatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        TenantSubscription savedSubscription = tenantSubscriptionRepository.save(subscription);
        log.info("Trial subscription created for tenant: {} paymentMethod={} trialEnd={}",
                 savedTenant.getId(), request.getPaymentMethod(), trialEnd);

        // 10. Creare la BUSINESS_REGISTRATION_REQUEST con status CONVERTED
        BusinessRegistrationRequest registrationRequest = BusinessRegistrationRequest.builder()
                .requestedSlug(request.getRequestedSlug())
                .tenantName(request.getTenantName())
                .legalName(request.getLegalName())
                .businessType(request.getBusinessType())
                .vatNumber(request.getVatNumber())
                .businessEmail(request.getBusinessEmail())
                .businessPhone(request.getBusinessPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .contactFirstName(request.getContactFirstName())
                .contactLastName(request.getContactLastName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .passwordHash(passwordHash)
                .requestedPlanCode(request.getRequestedPlanCode())
                .status("CONVERTED")
                .submittedAt(OffsetDateTime.now())
                .tenantId(savedTenant.getId())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        registrationRequestRepository.save(registrationRequest);
        log.info("Business registration request created and marked as CONVERTED");

        // 11. Email (non blocca la registrazione se SMTP non è configurato)
        boolean emailSent = emailService.sendTemporaryPasswordEmail(
            request.getContactEmail(),
            request.getTenantName(),
            temporaryPassword,
            request.getCompanyLogoDataUrl());

        emailService.sendAdminRegistrationNotification(
            request.getTenantName(),
            request.getBusinessType(),
            request.getContactFirstName(),
            request.getContactLastName(),
            request.getContactEmail(),
            request.getContactPhone(),
            request.getRequestedPlanCode(),
            "TRIAL_" + (request.getPaymentMethod() != null ? request.getPaymentMethod() : "UNKNOWN"),
            null);

        String responseMessage = emailSent
            ? "Registrazione completata! Hai 14 giorni di prova gratuita. Potrai aggiungere il metodo di pagamento dalla dashboard."
            : "Registrazione completata, ma non è stato possibile inviare l'email con la password temporanea (SMTP non disponibile).";

        return BusinessSignupResponse.builder()
                .tenantId(savedTenant.getId())
                .tenantSlug(savedTenant.getSlug())
                .tenantStatus(savedTenant.getStatus())
                .message(responseMessage)
                .subscriptionId(savedSubscription.getId())
                .checkoutUrl(null)
                .paymentMethod(request.getPaymentMethod())
                .build();
    }

    /**
     * Approva una registrazione pendente e attiva il tenant
     * (Utilizzato dai backend administrators)
     * 
     * @param tenantId l'ID del tenant
     * @param approvedByStaffUserId l'ID dello staff che approva
     * @return la risposta della registrazione approvata
     */
    @Transactional
    @CacheEvict(value = "allTenants", allEntries = true)
    public BusinessSignupResponse approveTenant(Long tenantId, Long approvedByStaffUserId) {
        log.info("Approving tenant ID: {}", tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trovato"));

        if (!"PENDING".equals(tenant.getStatus())) {
            throw new IllegalArgumentException("Il tenant non è nello stato PENDING per l'approvazione");
        }

        tenant.setStatus("ACTIVE");
        tenant.setEnabled(true);
        tenant.setApprovedAt(OffsetDateTime.now());
        tenant.setApprovedByStaffUserId(approvedByStaffUserId);
        tenant.setActivationDate(OffsetDateTime.now());
        tenant.setUpdatedAt(OffsetDateTime.now());

        Tenant updatedTenant = tenantRepository.save(tenant);
        log.info("Tenant {} activated", tenantId);

        return BusinessSignupResponse.builder()
                .tenantId(updatedTenant.getId())
                .tenantSlug(updatedTenant.getSlug())
                .tenantStatus(updatedTenant.getStatus())
                .message("Tenant approvato e attivato con successo!")
                .build();
    }

    /**
     * Assegna il ruolo MANAGER allo staff user
     * 
     * @param staffUser lo staff user a cui assegnare il ruolo
     */
    private void assignManagerRole(StaffUser staffUser) {
        StaffRole managerRole = staffRoleRepository.findByCode("MANAGER")
                .orElseThrow(() -> new IllegalArgumentException("Ruolo MANAGER non trovato nel sistema"));

        StaffUserRole staffUserRole = StaffUserRole.builder()
                .staffUser(staffUser)
                .role(managerRole)
                .build();

        staffUserRoleRepository.save(staffUserRole);
        log.info("MANAGER role assigned to staff user: {}", staffUser.getId());
    }

    /**
     * Valida i dati della richiesta di registrazione
     * 
     * @param request la richiesta di registrazione
     * @throws IllegalArgumentException se i dati non sono validi
     */
    private void validateRegistrationRequest(BusinessSignupRequest request) {
        // Validare che lo slug sia valido
        if (!request.getRequestedSlug().matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Lo slug contiene caratteri non validi");
        }

        // Validare il tipo di attività
        if (!Arrays.asList("LIDO", "BAR", "RESTAURANT", "NIGHTCLUB", "OTHER")
                .contains(request.getBusinessType())) {
            throw new IllegalArgumentException("Tipo di attività non valido");
        }

        log.debug("Business registration request validation passed");
    }

    private String buildBrandingJson(BusinessSignupRequest request) {
        try {
            ObjectNode branding = objectMapper.createObjectNode();
            if (request.getCompanyLogoDataUrl() != null && !request.getCompanyLogoDataUrl().isBlank()) {
                branding.put("logoDataUrl", request.getCompanyLogoDataUrl());
            }
            if (request.getCompanyBannerDataUrl() != null && !request.getCompanyBannerDataUrl().isBlank()) {
                branding.put("bannerDataUrl", request.getCompanyBannerDataUrl());
            }
            return objectMapper.writeValueAsString(branding);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private boolean hasLogo(String logoDataUrl) {
        return logoDataUrl != null && !logoDataUrl.isBlank();
    }
}
