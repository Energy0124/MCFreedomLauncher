package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

class InvalidateRequest {

    public InvalidateRequest(final YggdrasilUserAuthentication authenticationService) {
        String accessToken = authenticationService.getAuthenticatedToken();
        String clientToken = authenticationService.getAuthenticationService().getClientToken();
    }
}
