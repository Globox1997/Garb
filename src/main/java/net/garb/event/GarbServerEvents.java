package net.garb.event;

import net.garb.character.CharacterClassRegistry;
import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbActiveCharacterCache;
import net.garb.character.GarbPlayerManager;
import net.garb.util.ClassApplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class GarbServerEvents {

    public static void onPlayerDisconnect(ServerPlayNetworkHandler context, MinecraftServer server) {
        if (server.isOnThread()) {
            saveCurrentCharacter(context.getPlayer(), server);
        } else {
            server.execute(() -> saveCurrentCharacter(context.getPlayer(), server));
        }
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        int slot = GarbPlayerManager.getSelection(player.getUuid());
        if (slot == -1) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CharacterData data = CharacterSaveManager.load(server, player.getUuid(), slot);
        data.setHealth(player.getMaxHealth());
        CharacterSaveManager.save(server, player.getUuid(), slot, data);
    }

    public static void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        int slot = GarbPlayerManager.getSelection(newPlayer.getUuid());
        if (slot == -1) {
            return;
        }
        MinecraftServer server = newPlayer.getServer();
        if (server == null) {
            return;
        }
        CharacterData data = CharacterSaveManager.load(server, newPlayer.getUuid(), slot);
        Identifier classId = Identifier.tryParse(data.getClassId());
        if (classId == null) {
            return;
        }
        CharacterClassRegistry.get(classId).ifPresent(characterClass -> {
            ClassApplier.applyOnLogin(newPlayer, characterClass);
            newPlayer.setHealth(alive ? oldPlayer.getHealth() : newPlayer.getMaxHealth());
        });
    }

    public static void saveCurrentCharacter(ServerPlayerEntity player, MinecraftServer server) {
        int slot = GarbPlayerManager.getAndRemoveSelection(player.getUuid());
        if (slot == -1) {
            return;
        }

        CharacterData data = CharacterSaveManager.load(server, player.getUuid(), slot);

        data.captureFromPlayer(player);
        CharacterSaveManager.save(server, player.getUuid(), slot, data);

        GarbActiveCharacterCache.clear(player.getUuid());
    }

}
