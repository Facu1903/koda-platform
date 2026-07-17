package com.koda.platform.platform.security.infrastructure;

import com.koda.platform.platform.security.application.RefreshTokenPair;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenPair generate(Instant expiresAt) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new RefreshTokenPair(UUID.randomUUID(), value, hash(value), expiresAt);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
