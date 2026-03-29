package net.garb.mixin.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.DynamicRegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(ClientConfigurationNetworkHandler.class)
public interface ClientConfigurationNetworkHandlerAccessor {

    @Accessor("registryManager")
    DynamicRegistryManager.Immutable garb$getRegistryManager();

    @Accessor("profile")
    GameProfile garb$getProfile();

}
