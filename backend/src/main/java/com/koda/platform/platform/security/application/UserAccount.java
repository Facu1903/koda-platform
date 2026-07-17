package com.koda.platform.platform.security.application;

import java.util.UUID;

public record UserAccount(
    UUID id,
    String email,
    String displayName,
    String passwordHash,
    String status
) {
}
