package com.koda.platform.platform.security.application;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
