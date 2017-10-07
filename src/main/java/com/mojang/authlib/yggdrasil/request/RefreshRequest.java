package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class RefreshRequest {

    public RefreshRequest(final YggdrasilUserAuthentication authenticationService) {
        this(authenticationService, null);
    }

    public RefreshRequest(final YggdrasilUserAuthentication authenticationService, final GameProfile profile) {
        boolean requestUser = true;
        String clientToken = authenticationService.getAuthenticationService().getClientToken();
        String accessToken = authenticationService.getAuthenticatedToken();
    }
}
