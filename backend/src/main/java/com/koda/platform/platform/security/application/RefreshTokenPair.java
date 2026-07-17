package com.koda.platform.platform.security.application;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenPair(UUID id, String value, String hash, Instant expiresAt) {
}
