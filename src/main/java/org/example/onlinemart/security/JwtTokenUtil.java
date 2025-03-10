package org.example.onlinemart.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expirationMillis}")
    private long expirationMillis;

    /**
     * Generates a JWT token that includes:
     * - subject (username)
     * - role (ROLE_USER or ROLE_ADMIN)
     * - userId (the numeric ID from the database)
     */
    public String generateToken(String username, String role, int userId) {
        return Jwts.builder()
                .setSubject(username)
                // additional claims
                .claim("role", role)
                .claim("userId", userId)
                // standard JWT fields
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    /**
     * Returns the "sub" (username) from the token
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Returns the "role" claim from the token
     */
    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    /**
     * Returns the "userId" claim from the token
     */
    public Integer getUserIdFromToken(String token) {
        Object userIdObj = getClaims(token).get("userId");
        // The cast might be Integer or Long depending on how it's serialized;
        // usually Jackson in Spring will handle int as Integer
        if (userIdObj instanceof Integer) {
            return (Integer) userIdObj;
        } else if (userIdObj instanceof Long) {
            return ((Long) userIdObj).intValue();
        } else {
            return null; // or throw an exception
        }
    }

    /**
     * Internally returns the Claims object from a token
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the token (checks signature, expiration, etc.)
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException |
                 UnsupportedJwtException |
                 MalformedJwtException |
                 SignatureException |
                 IllegalArgumentException e) {
            return false;
        }
    }
}