package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class ValidateRequest {

    public ValidateRequest(final YggdrasilUserAuthentication authenticationService) {
        String clientToken = authenticationService.getAuthenticationService().getClientToken();
        String accessToken = authenticationService.getAuthenticatedToken();
    }
}
