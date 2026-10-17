package com.pm.auth_service.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;


@Component
public class JwtUtil {

    private final Key secretkey;


    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        this.secretkey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, String role) {
        // Implement token generation logic here using the secret key
        // You can use libraries like jjwt to create the JWT token
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10 )) // Token valid for 10 hour
                .signWith(secretkey)
                .compact();
    }

    public void validateToken(String token){
        try{
            Jwts.parser().verifyWith((SecretKey) secretkey)
                    .build()
                    .parseSignedClaims(token);
        }catch(SignatureException e){
            throw new SignatureException("Invalid JWT signature");
        } catch (JwtException e) {
            throw new JwtException("Invalid JWT token");
        }
    }
}


