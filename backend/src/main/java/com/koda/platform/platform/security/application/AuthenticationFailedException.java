package com.koda.platform.platform.security.application;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Invalid credentials");
    }
}
