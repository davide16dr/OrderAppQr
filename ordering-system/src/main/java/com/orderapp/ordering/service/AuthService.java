package com.orderapp.ordering.service;

import com.orderapp.ordering.dto.LoginRequestDTO;
import com.orderapp.ordering.dto.LoginResponseDTO;
import com.orderapp.ordering.dto.RefreshTokenRequestDTO;
import com.orderapp.ordering.entity.StaffUser;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.entity.TenantSubscription;
import com.orderapp.ordering.repository.StaffUserRepository;
import com.orderapp.ordering.repository.StaffUserRoleRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.orderapp.ordering.repository.TenantSubscriptionRepository;
import com.orderapp.ordering.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private StaffUserRepository staffUserRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StaffUserRoleRepository staffUserRoleRepository;

    @Autowired
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private String[] resolveSubscriptionInfo(Long tenantId) {
        Optional<TenantSubscription> subOpt = tenantSubscriptionRepository.findCurrentSubscriptionByTenantId(tenantId);
        if (subOpt.isEmpty()) return new String[]{"NONE", null};
        TenantSubscription sub = subOpt.get();
        String status = sub.getStatus();
        String trialEndsAt = null;
        if ("TRIAL".equalsIgnoreCase(status)) {
            if (sub.getTrialEndsAt() != null) {
                trialEndsAt = sub.getTrialEndsAt().format(ISO_FMT);
                if (sub.getTrialEndsAt().isBefore(OffsetDateTime.now())) {
                    status = "EXPIRED";
                }
            }
        }
        return new String[]{status, trialEndsAt};
    }

    private List<String> buildRolesFor(StaffUser user) {
        List<String> roles = new ArrayList<>();
        roles.add("STAFF");

        if (Boolean.TRUE.equals(user.getIsPrimaryContact())) {
            roles.add("ADMIN");
        }

        // Check for elevated roles: SUPER_ADMIN, MANAGER, BAR, KITCHEN, etc.
        var userRoles = staffUserRoleRepository.findAllByStaffUser_Id(user.getId());
        for (var userRole : userRoles) {
            if (userRole.getRole() == null) continue;
            String roleCode = userRole.getRole().getCode();
            if (roleCode != null) {
                String upperRoleCode = roleCode.toUpperCase();
                // Add elevated roles to JWT if not already present
                if (("SUPER_ADMIN".equals(upperRoleCode) || "MANAGER".equals(upperRoleCode) || 
                     "BAR".equals(upperRoleCode) || "KITCHEN".equals(upperRoleCode)) 
                    && !roles.contains(upperRoleCode)) {
                    roles.add(upperRoleCode);
                }
            }
        }

        return roles;
    }
    
    /**
     * Effettua il login dell'utente staff
     */
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // Verifica che l'utente esista
        Optional<StaffUser> userOptional = staffUserRepository.findByEmailIgnoreCase(loginRequest.getEmail());
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Email o password non corretti");
        }
        
        StaffUser user = userOptional.get();

        // Verifica che il tenant associato esista e non sia esplicitamente sospeso/disabilitato
        Tenant tenant = tenantRepository.findById(user.getTenantId())
            .orElseThrow(() -> new RuntimeException("Tenant associato non trovato"));
        if ("SUSPENDED".equalsIgnoreCase(tenant.getStatus()) || "DISABLED".equalsIgnoreCase(tenant.getStatus())) {
            throw new RuntimeException("Account sospeso. Contatta il supporto.");
        }
        if (!tenant.isEnabled()) {
            throw new RuntimeException("Account disabilitato.");
        }

        // Verifica la password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Email o password non corretti");
        }

        // Verifica che l'account sia attivo
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account disabilitato o sospeso");
        }

        // Aggiorna l'ultimo login
        user.setLastLoginAt(OffsetDateTime.now());
        staffUserRepository.save(user);

        // Genera i token
        List<String> roles = buildRolesFor(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getTenantId().toString(),
            roles
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        // Risolvi lo stato dell'abbonamento
        String[] subInfo = resolveSubscriptionInfo(user.getTenantId());

        // Costruisci la risposta
        LoginResponseDTO.UserDTO userDTO = new LoginResponseDTO.UserDTO(
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getTenantId().toString(),
            tenant.getName(),
            readBrandingLogoDataUrl(tenant),
            roles,
            tenant.isDemo(),
            subInfo[0],
            subInfo[1]
        );
        
        String redirect = null;
        if (roles.stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r))) {
            redirect = "/admin/tenants";
        }

        return new LoginResponseDTO(accessToken, refreshToken, userDTO, redirect);
    }
    
    /**
     * Effettua il refresh del token
     */
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();
        
        // Valida il refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token non valido o scaduto");
        }
        
        // Estrai l'userId dal token
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        
        // Recupera l'utente
        Optional<StaffUser> userOptional = staffUserRepository.findById(Long.valueOf(userId));
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Utente non trovato");
        }
        
        StaffUser user = userOptional.get();

        // Verifica che il tenant associato esista e non sia esplicitamente sospeso/disabilitato
        Tenant tenant = tenantRepository.findById(user.getTenantId())
            .orElseThrow(() -> new RuntimeException("Tenant associato non trovato"));
        if ("SUSPENDED".equalsIgnoreCase(tenant.getStatus()) || "DISABLED".equalsIgnoreCase(tenant.getStatus())) {
            throw new RuntimeException("Account sospeso.");
        }
        if (!tenant.isEnabled()) {
            throw new RuntimeException("Account disabilitato.");
        }

        // Verifica che l'account sia ancora attivo
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account disabilitato o sospeso");
        }

        // Genera nuovo access token
        List<String> roles = buildRolesFor(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getTenantId().toString(),
            roles
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        String[] subInfo = resolveSubscriptionInfo(user.getTenantId());

        LoginResponseDTO.UserDTO userDTO = new LoginResponseDTO.UserDTO(
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getTenantId().toString(),
            tenant.getName(),
            readBrandingLogoDataUrl(tenant),
            roles,
            tenant.isDemo(),
            subInfo[0],
            subInfo[1]
        );

        String redirectRefresh = null;
        if (roles.stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r))) {
            redirectRefresh = "/admin/tenants";
        }

        return new LoginResponseDTO(newAccessToken, newRefreshToken, userDTO, redirectRefresh);
    }

    private String readBrandingLogoDataUrl(Tenant tenant) {
        String brandingJson = tenant.getBrandingJson();
        if (brandingJson == null || brandingJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(brandingJson);
            JsonNode logoNode = root.path("logoDataUrl");
            if (logoNode.isTextual() && !logoNode.asText().isBlank()) {
                return logoNode.asText();
            }
        } catch (Exception ignored) {
            // fall back to no logo
        }

        return null;
    }
}
