package com.grupo6.subastar.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // Clave secreta (en producción debería ir en application.properties o variables de entorno)
    // Debe ser de al menos 256 bits (32 caracteres)
    private final String jwtSecret = "SubastarAppSecretKeyParaFirmaDeTokens2026";
    
    // Tiempo de expiración del token (ej: 24 horas)
    private final int jwtExpirationMs = 86400000;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generarTokenJwt(Authentication authentication) {
        UserDetailsImpl usuarioPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(usuarioPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String obtenerEmailDelToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validarTokenJwt(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // El token es inválido, expiró, está mal formado, etc.
            System.err.println("Token JWT inválido: " + e.getMessage());
        }
        return false;
    }
}