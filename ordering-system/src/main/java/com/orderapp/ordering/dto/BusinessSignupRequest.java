package com.orderapp.ordering.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSignupRequest {

    // Business data
    @NotBlank(message = "Il nome dell'azienda è obbligatorio")
    @Size(min = 2, max = 255, message = "Il nome deve avere tra 2 e 255 caratteri")
    private String tenantName;

    @NotBlank(message = "La ragione sociale è obbligatoria")
    @Size(min = 2, max = 255, message = "La ragione sociale deve avere tra 2 e 255 caratteri")
    private String legalName;

    @Size(max = 2000000, message = "Il logo è troppo grande")
    private String companyLogoDataUrl;

    @NotBlank(message = "Il tipo di attività è obbligatorio")
    @Pattern(regexp = "^(LIDO|BAR|RESTAURANT|NIGHTCLUB|OTHER)$", message = "Tipo di attività non valido")
    private String businessType;

    @NotBlank(message = "La partita IVA è obbligatoria")
    @Size(max = 50, message = "La partita IVA non può superare 50 caratteri")
    private String vatNumber;

    @NotBlank(message = "L'email aziendale è obbligatoria")
    @Email(message = "Inserire un'email aziendale valida")
    private String businessEmail;

    @NotBlank(message = "Il telefono aziendale è obbligatorio")
    @Size(max = 50, message = "Il telefono non può superare 50 caratteri")
    private String businessPhone;

    @NotBlank(message = "L'indirizzo è obbligatorio")
    @Size(min = 5, max = 255, message = "L'indirizzo deve avere tra 5 e 255 caratteri")
    private String addressLine1;

    @NotBlank(message = "Il complemento indirizzo è obbligatorio")
    @Size(max = 255, message = "L'indirizzo aggiuntivo non può superare 255 caratteri")
    private String addressLine2;

    @NotBlank(message = "La città è obbligatoria")
    @Size(min = 2, max = 100, message = "La città deve avere tra 2 e 100 caratteri")
    private String city;

    @NotBlank(message = "La provincia è obbligatoria")
    @Size(min = 2, max = 100, message = "La provincia deve avere tra 2 e 100 caratteri")
    private String province;

    @NotBlank(message = "Il CAP è obbligatorio")
    @Size(min = 5, max = 20, message = "Il CAP deve avere tra 5 e 20 caratteri")
    private String postalCode;

    @NotBlank(message = "Il paese è obbligatorio")
    @Size(min = 2, max = 100, message = "Il paese deve avere tra 2 e 100 caratteri")
    private String country;

    @NotBlank(message = "Lo slug del tenant è obbligatorio")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Lo slug deve contenere solo lettere minuscole, numeri e trattini")
    @Size(min = 3, max = 100, message = "Lo slug deve avere tra 3 e 100 caratteri")
    private String requestedSlug;

    @NotBlank(message = "Il piano è obbligatorio")
    @Size(max = 50, message = "Il codice del piano non può superare 50 caratteri")
    private String requestedPlanCode;

    // Primary contact data
    @NotBlank(message = "Il nome del contatto è obbligatorio")
    @Size(min = 2, max = 100, message = "Il nome deve avere tra 2 e 100 caratteri")
    private String contactFirstName;

    @NotBlank(message = "Il cognome del contatto è obbligatorio")
    @Size(min = 2, max = 100, message = "Il cognome deve avere tra 2 e 100 caratteri")
    private String contactLastName;

    @NotBlank(message = "L'email del contatto è obbligatoria")
    @Email(message = "Inserire un'email del contatto valida")
    private String contactEmail;

    @NotBlank(message = "Il telefono del contatto è obbligatorio")
    @Size(max = 50, message = "Il telefono del contatto non può superare 50 caratteri")
    private String contactPhone;

    // Password: non richiesta in self-signup.
    // Il backend genera e invia una password temporanea via email.
    private String password;

    private String confirmPassword;

    @NotBlank(message = "Il ciclo di fatturazione è obbligatorio")
    @Pattern(regexp = "^(MONTHLY|YEARLY)$", message = "Ciclo di fatturazione non valido")
    private String billingCycle;
}
