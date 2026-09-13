package net.garb.mixin.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.util.ClientCharacterCache;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

    @Shadow
    @Final
    private GameProfile profile;

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void getSkinTexturesMixin(CallbackInfoReturnable<SkinTextures> info) {
        ClientCharacterCache.getSkin(this.profile.getId()).ifPresent(info::setReturnValue);
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void getNameMixin(CallbackInfoReturnable<Text> info) {
        ClientCharacterCache.getName(profile.getId()).ifPresent(name -> info.setReturnValue(Text.literal(name)));
    }
}