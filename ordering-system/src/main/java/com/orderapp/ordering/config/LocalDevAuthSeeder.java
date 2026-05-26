package com.orderapp.ordering.config;

import com.orderapp.ordering.entity.StaffUser;
import com.orderapp.ordering.entity.StaffUserRole;
import com.orderapp.ordering.entity.StaffUserRoleId;
import com.orderapp.ordering.entity.StaffRole;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.StaffRoleRepository;
import com.orderapp.ordering.repository.StaffUserRepository;
import com.orderapp.ordering.repository.StaffUserRoleRepository;
import com.orderapp.ordering.repository.TenantRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Profile("local")
public class LocalDevAuthSeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final StaffUserRepository staffUserRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final StaffUserRoleRepository staffUserRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDevAuthSeeder(
        TenantRepository tenantRepository,
        StaffUserRepository staffUserRepository,
        StaffRoleRepository staffRoleRepository,
        StaffUserRoleRepository staffUserRoleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.tenantRepository = tenantRepository;
        this.staffUserRepository = staffUserRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.staffUserRoleRepository = staffUserRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Prefer the demo tenant from db/local/data.sql so dashboard has sample data.
        Long tenantId = tenantRepository.findBySlugIgnoreCase("lido-azzurro")
            .map(Tenant::getId)
            .orElseGet(() -> seedTenantIfMissing(
                "azienda-test",
                "test",
                "Azienda Test",
                "Azienda Test S.r.l.",
                "RESTAURANT"
            ));

        // Seed staff users
        seedStaffUserIfMissing(
                "test@test.it",
                "test1234",
                "Test",
                "User",
                tenantId,
                true
        );

        seedStaffUserIfMissing(
            "admin@azienda.it",
            "password123",
            "Admin",
            "Azienda",
            tenantId,
            true
        );

        seedSuperAdminIfMissing(tenantId);
    }

    private void seedSuperAdminIfMissing(Long tenantId) {
        final String email = "superadmin@orderapp.local";
        seedStaffUserIfMissing(
            email,
            "superadmin",
            "Super",
            "Admin",
            tenantId,
            true
        );

        StaffUser superAdmin = staffUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Super admin user not found after seeding"));

        StaffRole role = staffRoleRepository.findByCode("SUPER_ADMIN")
            .orElseGet(() -> staffRoleRepository.save(
                StaffRole.builder()
                    .code("SUPER_ADMIN")
                    .description("Super Admin")
                    .build()
            ));

        StaffUserRoleId id = new StaffUserRoleId(superAdmin.getId(), role.getId());
        if (!staffUserRoleRepository.existsById(id)) {
            staffUserRoleRepository.save(
                StaffUserRole.builder()
                    .staffUser(superAdmin)
                    .role(role)
                    .build()
            );
        }
    }

    private Long seedTenantIfMissing(String slug, String subdomain, String name, String legalName, String businessType) {
        return tenantRepository.findBySlugIgnoreCase(slug)
                .map(Tenant::getId)
                .orElseGet(() -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    
                    Tenant tenant = Tenant.builder()
                            .slug(slug)
                            .subdomain(subdomain)
                            .name(name)
                            .legalName(legalName)
                            .businessType(businessType)
                            .status("ACTIVE")
                            .enabled(true)
                            .timezone("Europe/Rome")
                            .currencyCode("EUR")
                            .country("IT")
                            .registrationSource("SEEDER")
                            .activationDate(now)
                            .approvedAt(now)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    
                    return tenantRepository.save(tenant).getId();
                });
    }

    private void seedStaffUserIfMissing(
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            Long tenantId,
            boolean isPrimaryContact
    ) {
        var existing = staffUserRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            StaffUser current = existing.get();

            // Keep local login deterministic across restarts and schema resets.
            current.setTenantId(tenantId);
            current.setPasswordHash(passwordEncoder.encode(rawPassword));
            current.setStatus("ACTIVE");
            current.setIsPrimaryContact(isPrimaryContact);
            current.setUpdatedAt(OffsetDateTime.now());
            staffUserRepository.save(current);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        StaffUser user = StaffUser.builder()
                .tenantId(tenantId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .phone(null)
                .isPrimaryContact(isPrimaryContact)
                .invitedAt(null)
                .activatedAt(now)
                .lastLoginAt(null)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        staffUserRepository.save(user);
    }
}
