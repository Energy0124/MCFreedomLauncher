package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

import java.util.Map;

public interface MinecraftSessionService {
    void joinServer(GameProfile p0, String p1, String p2) throws AuthenticationException;

    GameProfile hasJoinedServer(GameProfile p0, String p1) throws AuthenticationUnavailableException;

    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile p0, boolean p1);

    GameProfile fillProfileProperties(GameProfile p0);
}
