package net.garb.init;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.garb.character.CharacterData;
import net.garb.character.*;
import net.garb.event.GarbServerEvents;
import net.garb.network.GarbServerPacket;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.garb.network.packet.S2CClassDataPacket;
import net.garb.util.ClassApplier;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.function.Consumer;

public class EventInit {

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register(GarbServerEvents::onPlayerDisconnect);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
                GarbServerEvents.onPlayerDeath(serverPlayerEntity);
            }
        });

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            handler.addTask(new CharacterSelectionTask());

            CharacterData[] slots = CharacterSaveManager.loadAll(server, handler.getDebugProfile().getId());
            S2CCharacterDataPacket packet = S2CCharacterDataPacket.from(slots);

            ServerConfigurationNetworking.send(handler, S2CClassDataPacket.fromRegistry());
            ServerConfigurationNetworking.send(handler, packet);
        });

        ServerPlayerEvents.JOIN.register((player) -> {
            if (GarbPlayerManager.hasSelection(player.getUuid())) {

                int slot = GarbPlayerManager.getSelection(player.getUuid());
                CharacterData characterData = CharacterSaveManager.load(player.getServer(), player.getUuid(), slot);
                Optional<CharacterClass> optional = CharacterClassRegistry.get(Identifier.of(characterData.getClassId()));

                GarbActiveCharacterCache.set(player.getUuid(), characterData.getName(), characterData.getSkinUsername());
                GarbServerPacket.syncOnJoin(player, characterData.getName(), characterData.getSkinUsername());

                boolean newCharacter = !characterData.exists();
                optional.ifPresent(characterClass -> {
                    if (newCharacter) {
                        ClassApplier.applyToNewCharacter(player, characterClass);
                    } else {
                        ClassApplier.applyOnLogin(player, characterClass);
                    }
                });

                if (newCharacter) {
                    characterData.captureFromPlayer(player);
                    CharacterSaveManager.save(player.getServer(), player.getUuid(), slot, characterData);
                }
            }

        });
    }

    public static class CharacterSelectionTask implements ServerPlayerConfigurationTask {

        public static final ServerPlayerConfigurationTask.Key KEY = new ServerPlayerConfigurationTask.Key("character_selection");

        @Override
        public void sendPacket(Consumer<Packet<?>> sender) {
        }

        @Override
        public ServerPlayerConfigurationTask.Key getKey() {
            return KEY;
        }
    }

}
