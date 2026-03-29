package net.garb.event;

import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbPlayerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class GarbServerEvents {

    public static void onPlayerDisconnect(ServerPlayNetworkHandler context, MinecraftServer server) {
        server.execute(() -> {
            saveCurrentCharacter(context.getPlayer(), server);
        });
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

//        var spawnPos = server.getOverworld().getSpawnPos();
//        data.setLastPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, "minecraft:overworld");
        data.setHealth(player.getMaxHealth());
        CharacterSaveManager.save(server, player.getUuid(), slot, data);
    }

    public static void saveCurrentCharacter(ServerPlayerEntity player, MinecraftServer server) {
        int slot = GarbPlayerManager.getAndRemoveSelection(player.getUuid());
        if (slot == -1) {
            return;
        }

        CharacterData data = CharacterSaveManager.load(server, player.getUuid(), slot);

        data.captureFromPlayer(player);
        CharacterSaveManager.save(server, player.getUuid(), slot, data);
    }

}
