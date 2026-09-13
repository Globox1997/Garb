package net.garb.mixin;

import net.garb.character.GarbActiveCharacterCache;
import net.garb.util.ClientCharacterCache;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void garb$getNameMixin(CallbackInfoReturnable<Text> info) {
        if ((Object) this instanceof ServerPlayerEntity serverPlayer) {
            GarbActiveCharacterCache.get(serverPlayer.getUuid()).ifPresent(active -> {
                if (!active.name().isEmpty()) {
                    info.setReturnValue(Text.literal(active.name()));
                }
            });
        } else if ((Object) this instanceof AbstractClientPlayerEntity clientPlayer) {
            ClientCharacterCache.getName(clientPlayer.getUuid()).ifPresent(name -> info.setReturnValue(Text.literal(name)));
        }
    }
}
