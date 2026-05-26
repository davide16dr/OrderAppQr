package com.orderapp.ordering.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {
    
    @NotBlank(message = "Email è obbligatorio")
    @Email(message = "Email non valida")
    private String email;
    
    @NotBlank(message = "Password è obbligatorio")
    @Size(min = 6, message = "Password deve essere almeno 6 caratteri")
    private String password;
    
    public LoginRequestDTO() {}
    
    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    // Getter e Setter
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
