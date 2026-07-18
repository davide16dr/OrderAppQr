package com.orderapp.ordering.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class LoginResponseDTO {
    
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
    private String redirectUrl;
    
    public LoginResponseDTO() {}
    
    public LoginResponseDTO(String accessToken, String refreshToken, UserDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public LoginResponseDTO(String accessToken, String refreshToken, UserDTO user, String redirectUrl) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.redirectUrl = redirectUrl;
    }
    
    // Getter e Setter
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public UserDTO getUser() {
        return user;
    }
    
    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    // Inner UserDTO
    public static class UserDTO {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String tenantId;
        private String tenantName;
        private String tenantLogoDataUrl;
        private List<String> roles;
        @JsonProperty("isDemo")
        private boolean isDemo;

        public UserDTO() {}

        public UserDTO(String id, String email, String firstName, String lastName, String tenantId, String tenantName, String tenantLogoDataUrl, List<String> roles, boolean isDemo) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.tenantId = tenantId;
            this.tenantName = tenantName;
            this.tenantLogoDataUrl = tenantLogoDataUrl;
            this.roles = roles;
            this.isDemo = isDemo;
        }
        
        // Getter e Setter
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getTenantId() {
            return tenantId;
        }
        
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
        }

        public String getTenantLogoDataUrl() {
            return tenantLogoDataUrl;
        }

        public void setTenantLogoDataUrl(String tenantLogoDataUrl) {
            this.tenantLogoDataUrl = tenantLogoDataUrl;
        }
        
        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public boolean isDemo() {
            return isDemo;
        }

        public void setDemo(boolean isDemo) {
            this.isDemo = isDemo;
        }
    }
}
