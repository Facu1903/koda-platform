package com.koda.platform.platform.security.application;

import java.time.Instant;

public interface RefreshTokenService {

    RefreshTokenPair generate(Instant expiresAt);

    String hash(String token);
}