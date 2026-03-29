package net.garb.init;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.event.GarbServerEvents;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.server.network.ServerPlayerEntity;

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
            ServerConfigurationNetworking.send(handler, packet);
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
