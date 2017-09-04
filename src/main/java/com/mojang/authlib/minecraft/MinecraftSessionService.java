package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

import java.util.Map;

public abstract interface MinecraftSessionService {
    public abstract void joinServer(GameProfile paramGameProfile, String paramString1, String paramString2)
            throws AuthenticationException;

    public abstract GameProfile hasJoinedServer(GameProfile paramGameProfile, String paramString)
            throws AuthenticationUnavailableException;

    public abstract Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile paramGameProfile, boolean paramBoolean);

    public abstract GameProfile fillProfileProperties(GameProfile paramGameProfile);
}