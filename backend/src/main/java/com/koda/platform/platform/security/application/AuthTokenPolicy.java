package com.koda.platform.platform.security.application;

import java.time.Duration;

public interface AuthTokenPolicy {

    Duration refreshTokenTtl();
}