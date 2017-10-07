package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

import java.util.Map;

public interface MinecraftSessionService {
    void joinServer(final GameProfile p0, final String p1, final String p2) throws AuthenticationException;

    GameProfile hasJoinedServer(final GameProfile p0, final String p1) throws AuthenticationUnavailableException;

    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(final GameProfile p0, final boolean p1);

    GameProfile fillProfileProperties(final GameProfile p0, final boolean p1);
}
