package net.garb.mixin;

import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbPlayerManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow
    @Mutable
    @Final
    private MinecraftServer server;

    @Inject(method = "loadPlayerData", at = @At("HEAD"), cancellable = true)
    private void loadPlayerDataMixin(ServerPlayerEntity player, CallbackInfoReturnable<Optional<NbtCompound>> info) {
        UUID uuid = player.getUuid();

        if (GarbPlayerManager.hasSelection(uuid)) {
            CharacterData characterData = CharacterSaveManager.load(server, uuid, GarbPlayerManager.getSelection(uuid));
            if (characterData.exists()) {
                player.readNbt(characterData.getPlayerNbt());
                info.setReturnValue(Optional.of(characterData.getPlayerNbt()));
            } else {
                info.setReturnValue(Optional.empty());
            }
        }
    }
}
