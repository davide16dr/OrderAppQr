package com.orderapp.ordering.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);
            
            // Se il token esiste e è valido, configura l'autenticazione
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                
                // Estrai i ruoli dal token (se presenti)
                List<GrantedAuthority> authorities = extractAuthoritiesFromToken(token);
                
                // Crea l'autenticazione
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId, 
                        null, 
                        authorities
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("JWT authentication failed", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Estrai il token JWT dall'header Authorization
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * Estrai le autorità (ruoli) dal token JWT
     */
    private List<GrantedAuthority> extractAuthoritiesFromToken(String token) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        if (roles == null || roles.isEmpty()) {
            return authorities;
        }

        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }

            String normalized = role.trim();
            if (normalized.startsWith("ROLE_")) {
                authorities.add(new SimpleGrantedAuthority(normalized));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
            }
        }
        
        return authorities;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Non filtrare i percorsi pubblici
        return path.startsWith("/api/public/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/");
    }
}
