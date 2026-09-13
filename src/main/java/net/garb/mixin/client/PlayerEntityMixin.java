package net.garb.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.character.GarbActiveCharacterCache;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

//    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
//    private void garb$getNameMixin(CallbackInfoReturnable<Text> info) {
//        System.out.println("GET NAME ");
//        if ((Object) this instanceof ServerPlayerEntity serverPlayer) {
//            GarbActiveCharacterCache.get(serverPlayer.getUuid()).ifPresent(active -> {
//                if (!active.name().isEmpty()) {
//                    info.setReturnValue(Text.literal(active.name()));
//                }
//            });
//        }
//    }

    // Maybe works in ServerPlayerEntityMixin
//    @Inject(method = "getDisplayName", at = @At(value = "HEAD"))
//    private void getDisplayNameMixin(CallbackInfoReturnable<Text> info) {
//    }

}
