package com.mojang.authlib.exceptions;

public class UserMigratedException extends InvalidCredentialsException {
    public UserMigratedException(final String message) {
        super(message);
    }
}
