package net.garb.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getSpawnPos()Lnet/minecraft/util/math/BlockPos;"))
    private static BlockPos initMixin(ClientWorld instance, Operation<BlockPos> original) {
        if (instance == null) {
            return BlockPos.ORIGIN;
        }
        return original.call(instance);
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getSpawnAngle()F"))
    private static float init2Mixin(ClientWorld instance, Operation<Float> original) {
        if (instance == null) {
            return 0f;
        }
        return original.call(instance);
    }

    @Inject(method = "getPlayerListEntry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getNetworkHandler()Lnet/minecraft/client/network/ClientPlayNetworkHandler;"), cancellable = true)
    private void getPlayerListEntryMixin(CallbackInfoReturnable<PlayerListEntry> info) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            info.setReturnValue(null);
        }
    }
}
