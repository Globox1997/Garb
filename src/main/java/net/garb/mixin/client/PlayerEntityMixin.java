package net.garb.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.util.ClientCharacterCache;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void garb$getClientNameMixin(CallbackInfoReturnable<Text> info) {
        if ((Object) this instanceof AbstractClientPlayerEntity clientPlayer) {
            ClientCharacterCache.getName(clientPlayer.getUuid()).ifPresent(name -> info.setReturnValue(Text.literal(name)));
        }
    }
}
