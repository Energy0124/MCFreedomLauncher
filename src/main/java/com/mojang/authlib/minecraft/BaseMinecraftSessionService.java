package com.mojang.authlib.minecraft;

import com.mojang.authlib.AuthenticationService;

abstract class BaseMinecraftSessionService implements MinecraftSessionService {
    private final AuthenticationService authenticationService;

    BaseMinecraftSessionService(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }
}
