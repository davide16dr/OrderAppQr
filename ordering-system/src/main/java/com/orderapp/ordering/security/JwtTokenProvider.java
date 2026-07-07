package com.orderapp.ordering.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400000}")  // 24 hours
    private long jwtExpirationMs;
    
    @Value("${app.jwt.refreshExpiration:604800000}")  // 7 days
    private long refreshTokenExpirationMs;
    
    /**
     * Genera JWT token
     */
    public String generateAccessToken(String userId, String email, String firstName, String lastName, 
                                     String tenantId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("firstName", firstName);
        claims.put("lastName", lastName);
        claims.put("tenantId", tenantId);
        claims.put("roles", roles);
        
        return createToken(claims, userId, jwtExpirationMs);
    }
    
    /**
     * Genera refresh token
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return createToken(claims, userId, refreshTokenExpirationMs);
    }
    
    /**
     * Crea il token con claims e scadenza
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Estrai userId dal token
     */
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    
    /**
     * Estrai email dal token
     */
    public String getEmailFromToken(String token) {
        return (String) getClaimsFromToken(token).get("email");
    }

    /**
     * Estrai il tenantId dal token.
     */
    public String getTenantIdFromToken(String token) {
        Object tenantId = getClaimsFromToken(token).get("tenantId");
        return tenantId != null ? String.valueOf(tenantId) : null;
    }

    /**
     * Estrai i ruoli dal token (claim "roles").
     */
    public List<String> getRolesFromToken(String token) {
        Object raw = getClaimsFromToken(token).get("roles");
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of(String.valueOf(raw));
    }
    
    /**
     * Verifica se token è valido
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Ottieni claims dal token
     */
    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
