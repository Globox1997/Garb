package net.garb.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(World.class)
public class WorldMixin {

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/registry/DynamicRegistryManager;)Lnet/minecraft/entity/damage/DamageSources;"))
    private DamageSources initMixin(DynamicRegistryManager registryManager, Operation<DamageSources> original) {
        if (registryManager.getOptional(RegistryKeys.DAMAGE_TYPE).isEmpty()) {
            return null;
        } else {
          return  original.call(registryManager);
        }
//        return null;
    }

//    @WrapWithCondition(method = "<init>",at = @At(value = "INVOKE",target = ""))
}
