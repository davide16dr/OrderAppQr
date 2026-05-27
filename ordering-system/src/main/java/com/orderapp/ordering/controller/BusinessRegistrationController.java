package com.orderapp.ordering.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orderapp.ordering.dto.BusinessSignupRequest;
import com.orderapp.ordering.dto.BusinessSignupResponse;
import com.orderapp.ordering.service.BusinessRegistrationService;

@Slf4j
@RestController
@RequestMapping("/api/public/business-registration")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://*.vercel.app"})
@RequiredArgsConstructor
public class BusinessRegistrationController {

    private final BusinessRegistrationService businessRegistrationService;

    /**
     * Endpoint pubblico per la registrazione aziendale (self-signup)
     * POST /api/public/business-registration/signup
     * 
     * @param request i dati della registrazione
     * @return la risposta della registrazione
     */
    @PostMapping("/signup")
    public ResponseEntity<BusinessSignupResponse> submitBusinessRegistration(
            @Valid @RequestBody BusinessSignupRequest request) {
        log.info("Received business registration request for slug: {}", request.getRequestedSlug());
        
        try {
            BusinessSignupResponse response = businessRegistrationService.submitBusinessRegistration(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Business registration validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(BusinessSignupResponse.builder()
                            .message("Errore nella registrazione: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error during business registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BusinessSignupResponse.builder()
                            .message("Errore durante la registrazione. Per favore riprova più tardi.")
                            .build());
        }
    }

    /**
     * Endpoint protetto per approvare una registrazione (solo staff admin)
     * POST /api/public/business-registration/{tenantId}/approve
     * 
     * @param tenantId l'ID del tenant da approvare
     * @param approvedByStaffUserId l'ID dello staff che approva
     * @return la risposta della registrazione approvata
     */
    @PostMapping("/{tenantId}/approve")
    public ResponseEntity<BusinessSignupResponse> approveTenant(
            @PathVariable Long tenantId,
            @RequestParam Long approvedByStaffUserId) {
        log.info("Approving tenant ID: {} by staff user: {}", tenantId, approvedByStaffUserId);
        
        try {
            BusinessSignupResponse response = businessRegistrationService.approveTenant(
                    tenantId, approvedByStaffUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Tenant approval error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(BusinessSignupResponse.builder()
                            .message("Errore nell'approvazione: " + e.getMessage())
                            .build());
        }
    }
}
