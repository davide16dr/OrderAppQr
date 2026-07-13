package com.orderapp.ordering.controller;
//test
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderapp.ordering.dto.ChangePasswordRequestDTO;
import com.orderapp.ordering.dto.LoginResponseDTO;
import com.orderapp.ordering.entity.StaffUser;
import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.StaffUserRepository;
import com.orderapp.ordering.repository.StaffUserRoleRepository;
import com.orderapp.ordering.repository.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/account")
@RequiredArgsConstructor
public class AccountController {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final StaffUserRoleRepository staffUserRoleRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Non autenticato"));
        }

        Long userId;
        try {
            userId = Long.valueOf(auth.getPrincipal().toString());
        } catch (NumberFormatException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Token non valido"));
        }

        StaffUser user = staffUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Utente non trovato"));
        }

        Tenant tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Tenant associato non trovato"));
        }

        var roles = staffUserRoleRepository.findAllByStaffUser_Id(user.getId()).stream()
                .map(assignment -> assignment.getRole() != null ? assignment.getRole().getCode() : null)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();

        LoginResponseDTO.UserDTO payload = new LoginResponseDTO.UserDTO(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getTenantId().toString(),
                tenant.getName(),
                readBrandingLogoDataUrl(tenant),
                roles,
                tenant.isDemo()
        );

        return ResponseEntity.ok(payload);
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
            // ignore malformed branding json and return no logo
        }

        return null;
    }

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Le password non coincidono"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Non autenticato"));
        }

        Long userId;
        try {
            userId = Long.valueOf(auth.getPrincipal().toString());
        } catch (NumberFormatException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Token non valido"));
        }

        StaffUser user = staffUserRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Utente non trovato"));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Password attuale non corretta"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        staffUserRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    private record ErrorResponse(String message) {}
}
