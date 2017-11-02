package com.mojang.authlib.exceptions;

public class AuthenticationException extends Exception {
    AuthenticationException() {
    }

    public AuthenticationException(final String message) {
        super(message);
    }

    public AuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    AuthenticationException(final Throwable cause) {
        super(cause);
    }
}
