package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class AuthenticationRequest {

    public AuthenticationRequest(final YggdrasilUserAuthentication authenticationService, final String username, final String password) {
        boolean requestUser = true;
        Agent agent = authenticationService.getAgent();
        String clientToken = authenticationService.getAuthenticationService().getClientToken();
    }
}
