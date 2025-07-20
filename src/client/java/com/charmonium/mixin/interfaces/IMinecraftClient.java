package com.charmonium.mixin.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

@Mixin(MinecraftClient.class)
public interface IMinecraftClient {
    @Accessor("itemUseCooldown")
    int getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int value);

    @Invoker("doItemUse")
    void useItem();

    @Mutable
    @Accessor("session")
    void setSession(Session session);

    @Accessor("authenticationService")
    YggdrasilAuthenticationService getAuthenticationService();

    @Mutable
    @Accessor("socialInteractionsManager")
    void setSocialInteractionsManager(SocialInteractionsManager socialInteractionsManager);

    @Mutable
    @Accessor("abuseReportContext")
    void setAbuseReportContext(AbuseReportContext abuseReportContext);

    @Mutable
    @Accessor("gameProfileFuture")
    void setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("profileKeys")
    void setProfileKeys(ProfileKeys keys);

    @Mutable
    @Accessor
    void setUserApiService(UserApiService apiService);
}
